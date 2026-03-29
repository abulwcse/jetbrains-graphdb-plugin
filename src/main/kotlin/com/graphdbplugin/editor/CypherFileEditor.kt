package com.graphdbplugin.editor

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.graphdbplugin.language.CypherFileType
import com.graphdbplugin.results.ResultToolWindowManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.beans.PropertyChangeListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

/**
 * Custom file editor for Cypher query scratch buffers.
 *
 * Implements [FileEditor] and [UserDataHolderBase] to integrate cleanly with the
 * IntelliJ Platform's file-editor lifecycle. This editor is created by
 * [CypherEditorProvider.createEditor] whenever a [CypherVirtualFile] is opened.
 *
 * ### Layout
 * The editor's root panel uses [BorderLayout]:
 * - **NORTH**: A toolbar containing the "Run Query" action button and a
 *   data-source selector [JComboBox].
 * - **CENTER**: A full-featured IntelliJ [Editor] component with Cypher syntax
 *   highlighting, code completion, and line numbers enabled.
 *
 * ### Document and Editor lifecycle
 * The [Document] and [Editor] are created in [init] and **must** be released in
 * [dispose] via `EditorFactory.getInstance().releaseEditor(editor)`. Failing to
 * release the editor causes a memory leak because the platform keeps a reference
 * to unreleased editors indefinitely.
 *
 * ### Data source selection
 * The toolbar contains a [JComboBox] pre-populated with all configured
 * [BoltDataSource] instances. The item corresponding to [virtualFile]'s data source
 * is pre-selected. [getSelectedDataSource] returns the currently chosen item,
 * falling back to [CypherVirtualFile.dataSource] if the combo box has no selection.
 *
 * ### Query text
 * Call [getQueryText] to retrieve the current content of the editor document.
 * This is used by [com.graphdbplugin.actions.RunQueryAction] to obtain the
 * query to execute.
 *
 * @param project     The current [Project]. Required for editor creation and
 *                    action-manager access.
 * @param virtualFile The [CypherVirtualFile] that backs this editor tab.
 *
 * @suppress UnstableApiUsage — [com.intellij.testFramework.LightVirtualFile] is in
 *           `testFramework` but is the supported in-memory VirtualFile API.
 */
@Suppress("UnstableApiUsage")
class CypherFileEditor(
    private val project: Project,
    val virtualFile: CypherVirtualFile
) : UserDataHolderBase(), FileEditor {

    /**
     * The document that belongs to [virtualFile].
     *
     * Using the [VirtualFile]'s canonical document (via [FileDocumentManager]) is
     * critical: the PSI infrastructure associates a [com.intellij.psi.PsiFile] with
     * a document through its [VirtualFile]. An orphan document created with
     * `EditorFactory.createDocument("")` has no [VirtualFile], so
     * [com.intellij.psi.PsiDocumentManager] can never create a PSI file for it —
     * which means completion contributors registered for `language="Cypher"` never
     * fire. Using the VirtualFile's document gives the editor a full PSI tree of
     * [com.graphdbplugin.language.CypherLanguage] and enables all completion features.
     */
    private val document: Document =
        FileDocumentManager.getInstance().getDocument(virtualFile)
            ?: EditorFactory.getInstance().createDocument("")

    /**
     * The IntelliJ [Editor] component with Cypher language support.
     * Created with `isViewer = false` so the user can type and edit freely.
     */
    private val editor: Editor = EditorFactory.getInstance()
        .createEditor(document, project, CypherFileType.INSTANCE, false)

    /**
     * Combo box listing all configured [BoltDataSource] instances.
     * Initialized in [buildToolbar] and read by [getSelectedDataSource].
     */
    private lateinit var dataSourceCombo: JComboBox<BoltDataSource>

    /** The root Swing panel returned by [getComponent]. */
    private val rootPanel: JPanel = JPanel(BorderLayout())

    init {
        configureEditorSettings()
        rootPanel.add(buildToolbar(), BorderLayout.NORTH)
        rootPanel.add(editor.component, BorderLayout.CENTER)

        // Whenever the query text changes, sync the Parameters panel so its
        // fields mirror the $params present in the current query.
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                ResultToolWindowManager.getInstance(project).syncQueryParams(document.text)
            }
        })
    }

    // -------------------------------------------------------------------------
    // Private initialisation helpers
    // -------------------------------------------------------------------------

    /**
     * Applies sensible default editor settings to the Cypher editor component.
     *
     * Settings applied:
     * - Line numbers visible
     * - Code folding outline visible (for comment folding)
     * - Auto code folding enabled
     * - Whitespace markers hidden (too noisy for a query editor)
     * - Soft wraps disabled (Cypher queries are typically short single-line statements)
     */
    private fun configureEditorSettings() {
        with(editor.settings) {
            isLineNumbersShown = true
            isAutoCodeFoldingEnabled = true
            isFoldingOutlineShown = true
            isWhitespacesShown = false
            isUseSoftWraps = false
        }
    }

    /**
     * Builds the editor toolbar panel placed at the NORTH border of [rootPanel].
     *
     * The toolbar contains:
     * - **Left**: An [com.intellij.openapi.actionSystem.ActionToolbar] with the
     *   "Run Query" action ([com.graphdbplugin.actions.RunQueryAction]).
     * - **Right**: A [JComboBox] for selecting the target data source. The combo
     *   is pre-populated from [DataSourceManager] and pre-selects the data source
     *   associated with [virtualFile].
     *
     * @return A [JPanel] ready to be added to [rootPanel].
     */
    private fun buildToolbar(): JPanel {
        val toolbarPanel = JPanel(BorderLayout())

        // ---- Left: Action toolbar with Run Query button ----
        val actionGroup = DefaultActionGroup()
        val runQueryAction = ActionManager.getInstance().getAction("GraphDB.RunQuery")
        if (runQueryAction != null) {
            actionGroup.add(runQueryAction)
        }
        val toolbar = ActionManager.getInstance().createActionToolbar(
            /* place = */ "GraphDB.CypherEditor",
            /* group = */ actionGroup,
            /* horizontal = */ true
        )
        toolbar.targetComponent = rootPanel
        toolbarPanel.add(toolbar.component, BorderLayout.WEST)

        // ---- Right: Data source selector combo box ----
        val dataSources = DataSourceManager.getInstance().getAllDataSources()
        val comboModel = DefaultComboBoxModel(dataSources.toTypedArray())
        dataSourceCombo = JComboBox(comboModel)

        // Pre-select the data source associated with this virtual file
        val currentDs = dataSources.firstOrNull { it.id == virtualFile.dataSource.id }
        if (currentDs != null) {
            dataSourceCombo.selectedItem = currentDs
        }

        // Render each item by its display name
        dataSourceCombo.renderer = object : ListCellRenderer<BoltDataSource> {
            override fun getListCellRendererComponent(
                list: JList<out BoltDataSource>?,
                value: BoltDataSource?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component = JLabel(value?.name ?: "(none)")
        }

        toolbarPanel.add(dataSourceCombo, BorderLayout.EAST)
        return toolbarPanel
    }

    // -------------------------------------------------------------------------
    // Public query API
    // -------------------------------------------------------------------------

    /**
     * Returns the current text content of the Cypher editor document.
     *
     * Called by [com.graphdbplugin.actions.RunQueryAction] to obtain the query
     * string before execution. The document text is always up-to-date because
     * the [Document] is modified directly by the [Editor] component.
     *
     * @return The full text of the query currently in the editor.
     */
    fun getQueryText(): String = document.text

    /**
     * Returns the [BoltDataSource] currently selected in the data-source combo box.
     *
     * If the combo box has no selection (e.g. the data source list is empty), falls
     * back to [CypherVirtualFile.dataSource] to ensure a non-null result.
     *
     * @return The selected [BoltDataSource], or the file's original data source as fallback.
     */
    fun getSelectedDataSource(): BoltDataSource =
        (dataSourceCombo.selectedItem as? BoltDataSource) ?: virtualFile.dataSource

    // -------------------------------------------------------------------------
    // FileEditor interface implementation
    // -------------------------------------------------------------------------

    /**
     * Returns the root Swing component to be embedded in the IDE's editor area.
     *
     * @return The [rootPanel] containing the toolbar and the [Editor] component.
     */
    override fun getComponent(): JPanel = rootPanel

    /**
     * Returns the component that should receive keyboard focus when this editor
     * is opened or switched to.
     *
     * The editor's content component is the correct focus target — it is the Swing
     * widget that processes key events and dispatches them to the editor's key map.
     *
     * @return The editor's [Editor.getContentComponent].
     */
    override fun getPreferredFocusedComponent(): javax.swing.JComponent = editor.contentComponent

    /**
     * Returns the display name of this editor type, shown in the editor tab title
     * context menu (not the tab label itself — that is driven by the virtual file name).
     *
     * @return The string `"Cypher Editor"`.
     */
    override fun getName(): String = "Cypher Editor"

    /**
     * Returns the saved editor state for serialisation.
     *
     * The Cypher editor does not persist any state (scroll position, caret offset,
     * etc.) across IDE restarts, so [FileEditorState.INSTANCE] (a no-op singleton)
     * is returned.
     *
     * @param level The granularity of the state request (ignored).
     * @return [FileEditorState.INSTANCE].
     */
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

    /**
     * Restores a previously saved editor state.
     *
     * Because [getState] always returns the no-op [FileEditorState.INSTANCE], this
     * method is effectively a no-op.
     *
     * @param state The state to restore (ignored).
     */
    override fun setState(state: FileEditorState) {
        // No persistent state to restore in Phase 3.
    }

    /**
     * Returns `false` — the Cypher editor's document is a scratch buffer that is
     * never written to disk, so the "modified" concept does not apply.
     *
     * @return Always `false`.
     */
    override fun isModified(): Boolean = false

    /**
     * Returns `true` as long as this editor instance has not been [dispose]d.
     *
     * A more sophisticated implementation would check whether the backing
     * [virtualFile] and [document] are still valid, but for a scratch buffer that
     * lives for the duration of the IDE session this is unnecessary.
     *
     * @return Always `true` (until [dispose] is called).
     */
    override fun isValid(): Boolean = true

    /**
     * Returns the [VirtualFile] that this editor was opened for.
     *
     * Used by the platform to associate this editor with the file in
     * [FileEditorManager] lookups and in the Recent Files list.
     *
     * @return The [CypherVirtualFile] associated with this editor.
     */
    override fun getFile(): VirtualFile = virtualFile

    /**
     * Registers a [PropertyChangeListener] for editor property changes.
     *
     * The Cypher editor does not fire any property-change events, so this
     * method is a no-op.
     *
     * @param listener The listener to register (ignored).
     */
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // No property-change events fired by this editor.
    }

    /**
     * Unregisters a previously registered [PropertyChangeListener].
     *
     * Matches the no-op in [addPropertyChangeListener].
     *
     * @param listener The listener to remove (ignored).
     */
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // No property-change events fired by this editor.
    }

    /**
     * Releases the [Editor] to prevent memory leaks.
     *
     * **CRITICAL**: [EditorFactory.releaseEditor] must be called here. The platform
     * keeps a global registry of all active editors; without an explicit release the
     * editor's document, highlighting state, and all related data structures are
     * never garbage-collected, causing a memory leak proportional to the number of
     * tabs the user opens during a session.
     *
     * The [rootPanel] and [document] are garbage-collected naturally after this
     * editor is released.
     */
    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}
