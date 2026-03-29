package com.graphdbplugin.services

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service

/**
 * Project-level service that tracks per-project GraphDB state.
 *
 * In Phase 1 the primary responsibility of this service is keeping track of
 * the **active data source** — the connection that is currently "in focus" for
 * a given IDE project window. This allows future features (query editor tabs,
 * schema browser) to know which database to target without requiring the user
 * to re-select it each time.
 *
 * ### Service registration
 * This service is declared in `plugin.xml` as a project-level service:
 * ```xml
 * <projectService serviceImplementation="com.graphdbplugin.services.GraphDbProjectService"/>
 * ```
 * Use [GraphDbProjectService.getInstance] to obtain the instance for a specific
 * [Project].
 *
 * ### Lifecycle
 * Because the service is project-scoped, a separate instance is created for
 * each open project window. The active data source is **not** persisted to disk;
 * it resets to `null` each time the project is opened. Persistence (if desired)
 * can be added in a later phase by implementing
 * [com.intellij.openapi.components.PersistentStateComponent].
 *
 * @param project The [Project] that owns this service instance.
 */
@Service(Service.Level.PROJECT)
class GraphDbProjectService(private val project: Project) {

    /**
     * The ID of the data source that is currently active (focused) for this project.
     *
     * `null` means no data source has been selected yet in this project session.
     * Use [setActiveDataSource] to change the active source and [getActiveDataSource]
     * to retrieve the full [BoltDataSource] object.
     */
    private var activeDataSourceId: String? = null

    /**
     * Sets the active (focused) data source for this project by its unique identifier.
     *
     * Subsequent calls to [getActiveDataSource] will return the [BoltDataSource]
     * corresponding to [id].
     *
     * If [id] does not correspond to any known data source in [DataSourceManager],
     * [getActiveDataSource] will return `null` until a matching source is added.
     *
     * @param id The [BoltDataSource.id] of the data source to make active.
     */
    fun setActiveDataSource(id: String) {
        activeDataSourceId = id
    }

    /**
     * Returns the currently active [BoltDataSource] for this project.
     *
     * Resolves the stored [activeDataSourceId] against [DataSourceManager.findById].
     * Returns `null` if:
     * - No data source has been set as active yet ([activeDataSourceId] is `null`).
     * - The previously active data source was deleted from [DataSourceManager].
     *
     * @return The active [BoltDataSource], or `null` if none is set / found.
     */
    fun getActiveDataSource(): BoltDataSource? {
        val id = activeDataSourceId ?: return null
        return DataSourceManager.getInstance().findById(id)
    }

    /**
     * Returns the project that owns this service instance.
     *
     * Useful for code that receives a [GraphDbProjectService] reference and needs
     * the associated [Project] for UI operations.
     *
     * @return The owning [Project].
     */
    fun getProject(): Project = project

    // =========================================================================
    // Companion object
    // =========================================================================

    companion object {

        /**
         * Returns the [GraphDbProjectService] instance for the given [project].
         *
         * This is the canonical entry point for obtaining the service. Example:
         *
         * ```kotlin
         * val svc = GraphDbProjectService.getInstance(project)
         * val active = svc.getActiveDataSource()
         * ```
         *
         * @param project The [Project] whose service instance to return.
         * @return The [GraphDbProjectService] for [project].
         */
        fun getInstance(project: Project): GraphDbProjectService = project.service()
    }
}
