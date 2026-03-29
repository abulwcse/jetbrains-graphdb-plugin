package com.graphdbplugin.editor

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.language.CypherFileType
import com.intellij.testFramework.LightVirtualFile

/**
 * In-memory virtual file that backs a Cypher query editor tab.
 *
 * [CypherVirtualFile] is a scratch buffer — its content is never persisted to
 * disk. It uses the platform's [LightVirtualFile] (an in-memory [com.intellij.openapi.vfs.VirtualFile]
 * implementation provided by `com.intellij.testFramework`) to integrate with the
 * IDE's file-editor infrastructure without requiring a real file on the filesystem.
 *
 * ### Identity and naming
 * The file name is derived from the [BoltDataSource.name] so that the editor tab
 * shows a meaningful title (e.g. `"Production DB.cypher"`) instead of a generic
 * untitled buffer. The [getPath] override returns a stable synthetic URI of the form
 * `graphdb://<id>/<name>.cypher` that is used by [CypherEditorProvider.accept] to
 * distinguish this file type from ordinary disk-based `.cypher` files.
 *
 * ### Association with a data source
 * Each [CypherVirtualFile] is tied to exactly one [BoltDataSource] via the
 * [dataSource] property. The [CypherFileEditor] uses this to pre-select the correct
 * entry in its data-source combo box and as the default target when the user clicks
 * "Run Query".
 *
 * ### Usage
 * Created by [CypherEditorProvider.openEditor]:
 * ```kotlin
 * val vf = CypherVirtualFile(dataSource)
 * FileEditorManager.getInstance(project).openFile(vf, true)
 * ```
 *
 * @param dataSource The [BoltDataSource] this query editor is associated with.
 *                   The file name and synthetic path are derived from this value.
 *
 * @suppress UnstableApiUsage — [LightVirtualFile] is in the `com.intellij.testFramework`
 *           package but is the documented, supported API for in-memory virtual files
 *           in the IntelliJ Platform (see the platform source and SDK documentation).
 */
@Suppress("UnstableApiUsage")
class CypherVirtualFile(val dataSource: BoltDataSource) :
    LightVirtualFile("${dataSource.name}.cypher", CypherFileType.INSTANCE, "") {

    /**
     * Returns a stable synthetic path string used as the file identity key.
     *
     * The path follows the format `graphdb://<dataSourceId>/<name>.cypher`.
     * This value is:
     * - Used by [CypherEditorProvider.accept] to recognise this as a managed
     *   Cypher editor file (as opposed to an arbitrary `.cypher` file opened
     *   from the Project tree).
     * - Stable across editor tab re-opens for the same data source.
     * - Not a real filesystem path; the file exists only in memory.
     *
     * @return A synthetic URI string uniquely identifying this editor buffer.
     */
    override fun getPath(): String = "graphdb://${dataSource.id}/${dataSource.name}.cypher"
}
