package com.graphdbplugin.toolwindow

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.graphdbplugin.dialog.AddEditDataSourceDialog
import com.graphdbplugin.editor.CypherEditorProvider
import com.graphdbplugin.services.SchemaIntrospectionService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.concurrency.AppExecutorUtil
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Config
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.SessionConfig
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent

/** Lifecycle state of a data source connection attempt. */
enum class ConnectionState { CHECKING, READY, FAILED }

/**
 * Main content panel displayed inside the "GraphDB" tool window.
 *
 * The panel is divided into two sections:
 *
 * 1. **Toolbar (NORTH)** — an [ActionToolbar] containing Add, Edit, Delete, and
 *    Schema Refresh buttons. Edit, Delete, and Refresh are enabled only when a data
 *    source is selected.
 *
 * 2. **List (CENTER)** — a [JBList] that shows all configured [BoltDataSource]
 *    instances using [DataSourceListCellRenderer]. The list is backed by a
 *    [DefaultListModel] that is refreshed whenever the user performs a CRUD
 *    operation.
 *
 * ### Interaction model
 * - **Single click** selects a row and updates toolbar button states.
 * - **Double click** triggers [openDataSource], which opens a Cypher editor tab.
 * - **Add / Edit buttons** open [AddEditDataSourceDialog] in the appropriate mode.
 * - **Delete button** shows a confirmation dialog then removes the selected source.
 * - **Refresh button** triggers an async schema re-introspection via [SchemaRefreshAction].
 *
 * Implements [DataProvider] so that [SchemaRefreshAction] (and other actions in the
 * toolbar) can retrieve this panel instance via [DATA_KEY].
 *
 * @param project The active [Project], required for dialog parent windows,
 *                project-service access, and notification display.
 */
class DataSourceTreePanel(private val project: Project) : JPanel(BorderLayout()), DataProvider {

    /** Model backing the list — updated by [refreshList]. */
    private val listModel = DefaultListModel<BoltDataSource>()

    /** Live connection state for every data source, keyed by [BoltDataSource.id]. */
    private val connectionStates = ConcurrentHashMap<String, ConnectionState>()

    /**
     * The visible list component. Exposed as internal so that action classes in
     * the same module can read the current selection without requiring a separate
     * accessor method.
     */
    internal val dataSourceList = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = DataSourceListCellRenderer(connectionStates)
        emptyText.text = "No data sources. Click \u2795 to add one."
    }

    // -------------------------------------------------------------------------
    // Toolbar actions (defined inline so they can reference panel state)
    // -------------------------------------------------------------------------

    /** Opens [AddEditDataSourceDialog] in "add" mode. */
    private val addAction = object : AnAction("Add Data Source", "Add a new Bolt data source", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) = addNewDataSource()
    }

    /**
     * Opens [AddEditDataSourceDialog] in "edit" mode for the currently selected
     * data source. Disabled when no selection is active.
     */
    private val editAction = object : AnAction("Edit Data Source", "Edit the selected data source", AllIcons.Actions.Edit) {
        override fun actionPerformed(e: AnActionEvent) = editSelectedDataSource()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = dataSourceList.selectedValue != null
        }
    }

    /**
     * Prompts for confirmation then deletes the currently selected data source.
     * Disabled when no selection is active.
     */
    private val deleteAction = object : AnAction("Delete Data Source", "Delete the selected data source", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) = deleteSelectedDataSource()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = dataSourceList.selectedValue != null
        }
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    init {
        buildToolbar()
        buildList()
        refreshList()
    }

    /**
     * Constructs the [ActionToolbar] and adds it to the NORTH border of this panel.
     *
     * The toolbar uses the IDE's standard [ActionManager] so that action icons,
     * tooltips and keyboard shortcuts integrate naturally with the platform.
     * The [SchemaRefreshAction] is included after the CRUD actions.
     */
    private fun buildToolbar() {
        // Register the inline actions under unique IDs so ActionManager can look
        // them up if needed (e.g. for keyboard shortcut binding).
        val am = ActionManager.getInstance()
        if (am.getAction("GraphDB.ToolPanel.Add") == null) {
            am.registerAction("GraphDB.ToolPanel.Add", addAction)
        }
        if (am.getAction("GraphDB.ToolPanel.Edit") == null) {
            am.registerAction("GraphDB.ToolPanel.Edit", editAction)
        }
        if (am.getAction("GraphDB.ToolPanel.Delete") == null) {
            am.registerAction("GraphDB.ToolPanel.Delete", deleteAction)
        }

        val group = DefaultActionGroup().apply {
            add(addAction)
            add(editAction)
            add(deleteAction)
            add(SchemaRefreshAction())
        }

        val toolbar: ActionToolbar = am.createActionToolbar(
            /* place = */ "GraphDB.DataSourceToolbar",
            /* group = */ group,
            /* horizontal = */ true
        )
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.NORTH)
    }

    /**
     * Wires up the [dataSourceList] with selection listeners and a mouse adapter,
     * then wraps it in a scroll pane and adds it to the CENTER of this panel.
     */
    private fun buildList() {
        // Enable/disable Edit & Delete based on selection changes.
        dataSourceList.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                dataSourceList.repaint()
            }
        }

        // Double-click opens the data source only when the connection is READY.
        // Non-READY items are already greyed out in the renderer — do nothing on double-click.
        dataSourceList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = dataSourceList.selectedValue ?: return
                    if (connectionStates[selected.id] == ConnectionState.READY) {
                        openDataSource(selected)
                    }
                }
            }
        })

        add(JBScrollPane(dataSourceList), BorderLayout.CENTER)
    }

    // -------------------------------------------------------------------------
    // DataProvider implementation
    // -------------------------------------------------------------------------

    /**
     * Exposes this panel to actions via the [DataKey] mechanism.
     *
     * When [dataId] matches [DATA_KEY], returns `this` so that [SchemaRefreshAction]
     * (and any other action that calls `e.getData(DataSourceTreePanel.DATA_KEY)`) can
     * obtain a reference to this panel without static coupling.
     *
     * @param dataId The data key identifier being requested.
     * @return This panel if [dataId] matches [DATA_KEY]; `null` otherwise.
     */
    override fun getData(dataId: String): Any? {
        return if (DATA_KEY.`is`(dataId)) this else null
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the currently selected [BoltDataSource] in the list, or `null` if
     * nothing is selected.
     *
     * Provides a clean public API for external classes (actions, etc.) to read
     * the current selection without depending on the internal [dataSourceList] field.
     *
     * @return The selected [BoltDataSource], or `null` if the list has no selection.
     */
    fun getSelectedDataSource(): BoltDataSource? = dataSourceList.selectedValue

    /**
     * Returns `true` if a data source is currently selected in the list.
     *
     * Convenience method for use in [AnAction.update] implementations.
     *
     * @return `true` when [getSelectedDataSource] would return a non-null value.
     */
    fun hasSelection(): Boolean = dataSourceList.selectedValue != null

    /**
     * Clears the list model and repopulates it from [DataSourceManager].
     *
     * Should be called after any CRUD operation that changes the set of managed
     * data sources (add, edit, delete).
     */
    fun refreshList() {
        val selected = dataSourceList.selectedValue
        listModel.clear()
        DataSourceManager.getInstance().getAllDataSources().forEach { ds ->
            listModel.addElement(ds)
            // Start a connection test for any source not already verified or in-flight.
            if (connectionStates[ds.id] != ConnectionState.CHECKING &&
                connectionStates[ds.id] != ConnectionState.READY) {
                triggerConnectionTest(ds)
            }
        }
        // Restore the previous selection if the item still exists.
        if (selected != null) {
            val idx = listModel.indexOf(DataSourceManager.getInstance().findById(selected.id))
            if (idx >= 0) dataSourceList.selectedIndex = idx
        }
    }

    /**
     * Handles a double-click "open" event on a data source.
     *
     * Opens a [com.graphdbplugin.editor.CypherFileEditor] tab for [dataSource]
     * via [CypherEditorProvider.openEditor]. If a tab is already open for this
     * data source, the platform brings it to the front rather than creating a
     * duplicate.
     *
     * @param dataSource The [BoltDataSource] that was double-clicked.
     */
    fun openDataSource(dataSource: BoltDataSource) {
        val ds = getSelectedDataSource() ?: return
        CypherEditorProvider.openEditor(project, ds)
    }

    /**
     * Spawns a background thread that opens a throwaway Bolt session to verify the
     * connection. On success the state becomes [ConnectionState.READY] and the schema
     * is loaded into [SchemaIntrospectionService] for auto-complete. On failure the
     * state becomes [ConnectionState.FAILED] and double-click stays blocked.
     */
    private fun triggerConnectionTest(ds: BoltDataSource) {
        connectionStates[ds.id] = ConnectionState.CHECKING
        dataSourceList.repaint()

        AppExecutorUtil.getAppExecutorService().submit {
            val ok = try {
                val password = DataSourceManager.getInstance().getPassword(ds.id) ?: ""
                val config = Config.builder()
                    .withConnectionTimeout(ds.connectionTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                    .withMaxConnectionPoolSize(1)
                    .apply {
                        if (!BoltDataSource.hasTlsScheme(ds.url)) {
                            if (ds.sslEnabled) withEncryption() else withoutEncryption()
                        }
                    }
                    .build()
                GraphDatabase.driver(ds.url, AuthTokens.basic(ds.username, password), config).use { driver ->
                    val sessionConfig = SessionConfig.builder().withDatabase(ds.database).build()
                    driver.session(sessionConfig).use { session -> session.run("RETURN 1").consume() }
                }
                true
            } catch (_: Exception) {
                false
            }

            ApplicationManager.getApplication().invokeLater {
                connectionStates[ds.id] = if (ok) ConnectionState.READY else ConnectionState.FAILED
                dataSourceList.repaint()
                if (ok) {
                    SchemaIntrospectionService.getInstance(project).refreshSchema(ds, project)
                }
            }
        }
    }

    /**
     * Opens [AddEditDataSourceDialog] in "add" mode (no pre-populated values).
     *
     * If the user confirms with OK, the new [BoltDataSource] is added to
     * [DataSourceManager] and the list is refreshed.
     */
    fun addNewDataSource() {
        val dialog = AddEditDataSourceDialog(project, existingDataSource = null)
        if (dialog.showAndGet()) {
            val newSource = dialog.getResult()
            DataSourceManager.getInstance().addDataSource(newSource)
            refreshList()
        }
    }

    /**
     * Opens [AddEditDataSourceDialog] pre-populated with the currently selected
     * data source's values (edit mode).
     *
     * If the user confirms with OK, the updated [BoltDataSource] replaces the
     * existing entry in [DataSourceManager] and the list is refreshed.
     *
     * If no data source is selected, this method is a no-op.
     */
    fun editSelectedDataSource() {
        val selected = dataSourceList.selectedValue ?: return
        val dialog = AddEditDataSourceDialog(project, existingDataSource = selected)
        if (dialog.showAndGet()) {
            val updated = dialog.getResult()
            connectionStates.remove(updated.id)   // config changed — force re-test
            DataSourceManager.getInstance().updateDataSource(updated)
            refreshList()
        }
    }

    /**
     * Prompts the user to confirm deletion of the currently selected data source.
     *
     * If confirmed, calls [DataSourceManager.removeDataSource] (which also clears
     * the stored password from the OS keychain and evicts the pooled driver) and
     * refreshes the list.
     *
     * If no data source is selected, this method is a no-op.
     */
    fun deleteSelectedDataSource() {
        val selected = dataSourceList.selectedValue ?: return
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete \u2018${selected.name}\u2019?\nThis action cannot be undone.",
            "Delete Data Source",
            "Delete",
            "Cancel",
            Messages.getWarningIcon()
        )
        if (result == Messages.YES) {
            connectionStates.remove(selected.id)
            DataSourceManager.getInstance().removeDataSource(selected.id)
            refreshList()
        }
    }

    // -------------------------------------------------------------------------
    // Companion object
    // -------------------------------------------------------------------------

    companion object {
        /**
         * [DataKey] used by [SchemaRefreshAction] (and any other toolbar action)
         * to access this panel from an [AnActionEvent].
         *
         * Usage:
         * ```kotlin
         * val panel = e.getData(DataSourceTreePanel.DATA_KEY) ?: return
         * ```
         */
        val DATA_KEY: DataKey<DataSourceTreePanel> =
            DataKey.create("GraphDB.DataSourceTreePanel")
    }
}
