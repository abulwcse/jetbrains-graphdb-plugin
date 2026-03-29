package com.graphdbplugin.editor

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.services.SchemaIntrospectionService
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * File editor provider that creates [CypherFileEditor] instances for
 * [CypherVirtualFile] scratch buffers.
 *
 * Registered via the `fileEditorProvider` extension point in `plugin.xml`:
 * ```xml
 * <fileEditorProvider
 *     implementationClass="com.graphdbplugin.editor.CypherEditorProvider"/>
 * ```
 *
 * The platform calls [accept] for every file that is opened and uses [createEditor]
 * to instantiate the editor if [accept] returns `true`. The [FileEditorPolicy.HIDE_DEFAULT_EDITOR]
 * policy ensures that only the custom [CypherFileEditor] is shown (not both the
 * custom editor and the default text editor stacked on top of each other).
 *
 * ### Opening an editor programmatically
 * Use the [openEditor] companion function rather than constructing [CypherVirtualFile]
 * and calling [FileEditorManager] directly:
 * ```kotlin
 * CypherEditorProvider.openEditor(project, dataSource)
 * ```
 * This also triggers an asynchronous schema refresh via [SchemaIntrospectionService]
 * so completions are populated shortly after the tab opens.
 *
 * Implements [DumbAware] so the provider is active even while the IDE is indexing.
 */
class CypherEditorProvider : FileEditorProvider, DumbAware {

    /**
     * Returns `true` if [file] is a [CypherVirtualFile], i.e. an in-memory scratch
     * buffer managed by this plugin.
     *
     * Regular `.cypher` files from the filesystem are *not* accepted here — they
     * open in the default text editor with Cypher syntax highlighting applied by
     * the [com.graphdbplugin.language.highlighting.CypherSyntaxHighlighterFactory].
     *
     * @param project The current [Project] (unused — the check is purely type-based).
     * @param file    The [VirtualFile] being considered for this editor.
     * @return `true` if [file] is a [CypherVirtualFile].
     */
    override fun accept(project: Project, file: VirtualFile): Boolean = file is CypherVirtualFile

    /**
     * Creates and returns a new [CypherFileEditor] for the given [file].
     *
     * The platform guarantees that [createEditor] is only called when [accept]
     * has returned `true`, so the cast to [CypherVirtualFile] is safe.
     *
     * @param project The current [Project].
     * @param file    The [CypherVirtualFile] to open.
     * @return A new [CypherFileEditor] bound to [file].
     */
    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        CypherFileEditor(project, file as CypherVirtualFile)

    /**
     * Returns the unique type identifier for editors created by this provider.
     *
     * This string is used as a stable key in the IDE's editor state persistence
     * mechanism and must remain constant across plugin versions.
     *
     * @return The string `"cypher-query-editor"`.
     */
    override fun getEditorTypeId(): String = "cypher-query-editor"

    /**
     * Returns [FileEditorPolicy.HIDE_DEFAULT_EDITOR] so that only the custom
     * [CypherFileEditor] is shown for [CypherVirtualFile]s. Without this policy
     * the platform would show both the custom editor and its default text editor.
     *
     * @return [FileEditorPolicy.HIDE_DEFAULT_EDITOR].
     */
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {

        /**
         * Opens a Cypher query editor tab for the given [dataSource] in the specified
         * [project], or brings an existing tab to the front if one is already open.
         *
         * The method:
         * 1. Creates a [CypherVirtualFile] wrapping [dataSource].
         * 2. Opens it via [FileEditorManager], which triggers [CypherEditorProvider.createEditor].
         * 3. Kicks off an asynchronous schema refresh via [SchemaIntrospectionService]
         *    so that label, relationship-type, and property-key completions are populated
         *    in the background.
         *
         * Must be called on the Event Dispatch Thread.
         *
         * @param project    The [Project] in whose editor area the tab should be opened.
         * @param dataSource The [BoltDataSource] to associate with the new editor tab.
         */
        fun openEditor(project: Project, dataSource: BoltDataSource) {
            val virtualFile = CypherVirtualFile(dataSource)
            FileEditorManager.getInstance(project).openFile(virtualFile, /* focusEditor = */ true)

            // Kick off schema refresh in the background so completion data is ready soon
            SchemaIntrospectionService.getInstance(project).refreshSchema(dataSource, project)
        }
    }
}
