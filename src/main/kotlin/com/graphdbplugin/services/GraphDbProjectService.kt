package com.graphdbplugin.services

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Serialisable state for [GraphDbProjectService].
 *
 * Holds the ID of the last active data source so it can be restored when
 * the project is re-opened or the plugin is updated.
 */
class GraphDbProjectServiceState {
    /** ID of the last selected [BoltDataSource], or empty string if none. */
    var activeDataSourceId: String = ""
}

/**
 * Project-level service that tracks per-project GraphDB state.
 *
 * Tracks the **active data source** — the connection that is currently
 * "in focus" for a given IDE project window — and persists it to
 * `graphdb-project.xml` so it survives IDE restarts and plugin updates.
 *
 * ### Service registration
 * Declared in `plugin.xml` as a project-level service. Use
 * [GraphDbProjectService.getInstance] to obtain the instance for a specific [Project].
 */
@Service(Service.Level.PROJECT)
@State(
    name = "GraphDbProjectService",
    storages = [Storage("graphdb-project.xml")]
)
class GraphDbProjectService(private val project: Project)
    : PersistentStateComponent<GraphDbProjectServiceState> {

    private var state: GraphDbProjectServiceState = GraphDbProjectServiceState()

    // =========================================================================
    // PersistentStateComponent
    // =========================================================================

    override fun getState(): GraphDbProjectServiceState = state

    override fun loadState(state: GraphDbProjectServiceState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    // =========================================================================
    // Active data source
    // =========================================================================

    /**
     * Sets the active (focused) data source for this project by its unique identifier.
     *
     * The selection is persisted to disk and restored automatically on the next
     * IDE start or plugin update.
     *
     * @param id The [BoltDataSource.id] of the data source to make active.
     */
    fun setActiveDataSource(id: String) {
        state.activeDataSourceId = id
    }

    /**
     * Returns the currently active [BoltDataSource] for this project.
     *
     * Resolves the stored ID against [DataSourceManager.findById]. Returns `null`
     * if no data source has been selected or if the previously selected source
     * was deleted.
     */
    fun getActiveDataSource(): BoltDataSource? {
        val id = state.activeDataSourceId.takeIf { it.isNotEmpty() } ?: return null
        return DataSourceManager.getInstance().findById(id)
    }

    /**
     * Returns the project that owns this service instance.
     */
    fun getProject(): Project = project

    // =========================================================================
    // Companion object
    // =========================================================================

    companion object {
        fun getInstance(project: Project): GraphDbProjectService = project.service()
    }
}
