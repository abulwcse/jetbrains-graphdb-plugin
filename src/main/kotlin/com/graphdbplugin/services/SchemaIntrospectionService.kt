package com.graphdbplugin.services

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Config
import org.neo4j.driver.GraphDatabase
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Project-level service that caches graph schema metadata fetched from a connected
 * Neo4j instance.
 *
 * The service maintains three in-memory caches backed by [AtomicReference]:
 * - **Labels** — node labels returned by `CALL db.labels()`
 * - **Relationship types** — returned by `CALL db.relationshipTypes()`
 * - **Property keys** — returned by `CALL db.propertyKeys()`
 *
 * These caches are populated asynchronously by [refreshSchema] so that the EDT is
 * never blocked by network I/O. The [SchemaAwareCompletionProvider][com.graphdbplugin.language.completion.SchemaAwareCompletionProvider]
 * reads from these caches synchronously at completion time and silently provides no
 * schema suggestions if they are empty.
 *
 * ### Lifecycle
 * A single instance of this service is created per [Project]. The caches start empty
 * and are populated when [refreshSchema] is called (typically when the user double-clicks
 * a data source to open a Cypher editor tab). The caches can be invalidated by calling
 * [clearCache] (e.g. when the active data source is changed).
 *
 * ### Thread safety
 * All cache fields use [AtomicReference] so that background worker threads can write
 * new snapshots while EDT completion reads are in progress, without explicit
 * `synchronized` blocks or `@Volatile` annotations on the list references.
 * The [isLoading] and [lastDataSourceId] flags use `@Volatile` for lightweight
 * single-write semantics.
 *
 * ### Service registration
 * Declared in `plugin.xml`:
 * ```xml
 * <projectService
 *     serviceImplementation="com.graphdbplugin.services.SchemaIntrospectionService"/>
 * ```
 *
 * @param project The [Project] that owns this service instance.
 */
@Service(Service.Level.PROJECT)
class SchemaIntrospectionService(private val project: Project) {

    private val log = logger<SchemaIntrospectionService>()

    /**
     * Cached list of node label strings from the last successful [refreshSchema] call.
     * Replaced atomically via [AtomicReference.set] on each refresh; never mutated
     * in place after the reference is published.
     */
    private val cachedLabels = AtomicReference<List<String>>(emptyList())

    /**
     * Cached list of relationship type strings from the last successful [refreshSchema].
     * Replaced atomically via [AtomicReference.set].
     */
    private val cachedRelationshipTypes = AtomicReference<List<String>>(emptyList())

    /**
     * Cached list of property key strings from the last successful [refreshSchema].
     * Replaced atomically via [AtomicReference.set].
     */
    private val cachedPropertyKeys = AtomicReference<List<String>>(emptyList())

    /**
     * `true` while a background schema-refresh task is in progress.
     * Prevents overlapping refresh calls when the user rapidly opens multiple editor tabs.
     */
    @Volatile
    private var isLoading: Boolean = false

    /**
     * The [BoltDataSource.id] of the data source whose schema is currently cached,
     * or `null` if the cache is empty or was cleared.
     *
     * Used to avoid redundant refreshes when the user opens a second tab for the
     * same data source.
     */
    @Volatile
    private var lastDataSourceId: String? = null

    // -------------------------------------------------------------------------
    // Public read API (safe to call from any thread)
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the currently cached node labels.
     *
     * The list is an immutable snapshot obtained from the [AtomicReference]; it will
     * not reflect subsequent [refreshSchema] completions until the next call.
     *
     * @return An immutable [List] of node label strings, possibly empty.
     */
    fun getCachedLabels(): List<String> = cachedLabels.get()

    /**
     * Returns a snapshot of the currently cached relationship type names.
     *
     * @return An immutable [List] of relationship type strings, possibly empty.
     */
    fun getCachedRelationshipTypes(): List<String> = cachedRelationshipTypes.get()

    /**
     * Returns a snapshot of the currently cached property key names.
     *
     * @return An immutable [List] of property key strings, possibly empty.
     */
    fun getCachedPropertyKeys(): List<String> = cachedPropertyKeys.get()

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    /**
     * Launches a background task to fetch schema metadata from the given [dataSource].
     *
     * The method is non-blocking — it submits work to the IDE's shared application
     * executor and returns immediately. On completion, the cached [AtomicReference]
     * fields are updated via [AtomicReference.set].
     *
     * If [isLoading] is already `true` (a previous refresh is still running), the
     * call is silently ignored to prevent duplicate driver connections.
     *
     * ### Error handling
     * Any exception during driver creation or query execution is caught and logged at
     * `WARN` level. The caches are left in their previous state so that stale
     * completions remain available rather than disappearing on a transient network error.
     *
     * @param dataSource The [BoltDataSource] to introspect. Its [BoltDataSource.url]
     *                   and [BoltDataSource.username] are used to create the driver;
     *                   the password is retrieved from [DataSourceManager.getPassword].
     * @param project    The current [Project], used to invoke EDT callbacks.
     */
    fun refreshSchema(dataSource: BoltDataSource, project: Project) {
        if (isLoading) {
            log.debug("Schema refresh already in progress for project '${project.name}'; skipping.")
            return
        }
        isLoading = true

        AppExecutorUtil.getAppExecutorService().submit {
            val newLabels = mutableListOf<String>()
            val newRelTypes = mutableListOf<String>()
            val newPropKeys = mutableListOf<String>()

            try {
                val password = DataSourceManager.getInstance().getPassword(dataSource.id) ?: ""
                val config = Config.builder()
                    .withConnectionTimeout(dataSource.connectionTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                    .withMaxConnectionPoolSize(1)
                    .build()

                GraphDatabase.driver(
                    dataSource.url,
                    AuthTokens.basic(dataSource.username, password),
                    config
                ).use { driver ->
                    driver.session().use { session ->
                        // Fetch node labels
                        session.run("CALL db.labels()").list().forEach { record ->
                            newLabels.add(record["label"].asString())
                        }
                        // Fetch relationship types
                        session.run("CALL db.relationshipTypes()").list().forEach { record ->
                            newRelTypes.add(record["relationshipType"].asString())
                        }
                        // Fetch property keys
                        session.run("CALL db.propertyKeys()").list().forEach { record ->
                            newPropKeys.add(record["propertyKey"].asString())
                        }
                    }
                }

                log.info(
                    "Schema refresh complete for '${dataSource.name}': " +
                    "${newLabels.size} labels, ${newRelTypes.size} rel types, ${newPropKeys.size} property keys."
                )
            } catch (ex: Exception) {
                log.warn("Schema refresh failed for data source '${dataSource.name}': ${ex.message}", ex)
                // Leave caches as-is so stale completions remain available
            } finally {
                // Update atomic caches and reset the loading flag on the EDT so that
                // subsequent reads from completion providers see a consistent snapshot.
                ApplicationManager.getApplication().invokeLater {
                    if (newLabels.isNotEmpty() || newRelTypes.isNotEmpty() || newPropKeys.isNotEmpty()) {
                        cachedLabels.set(newLabels.toList())
                        cachedRelationshipTypes.set(newRelTypes.toList())
                        cachedPropertyKeys.set(newPropKeys.toList())
                        lastDataSourceId = dataSource.id
                    }
                    isLoading = false
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cache invalidation
    // -------------------------------------------------------------------------

    /**
     * Clears all cached schema data and resets the [lastDataSourceId].
     *
     * Call this when the user switches to a different data source or disconnects,
     * so that stale schema from the previous connection does not pollute completions
     * for the new one.
     *
     * This method is safe to call from any thread.
     */
    fun clearCache() {
        cachedLabels.set(emptyList())
        cachedRelationshipTypes.set(emptyList())
        cachedPropertyKeys.set(emptyList())
        lastDataSourceId = null
        log.debug("Schema cache cleared.")
    }

    // -------------------------------------------------------------------------
    // Companion object
    // -------------------------------------------------------------------------

    companion object {

        /**
         * Returns the [SchemaIntrospectionService] instance for the given [project].
         *
         * This is the canonical entry point for obtaining the service. Example:
         * ```kotlin
         * val svc = SchemaIntrospectionService.getInstance(project)
         * svc.refreshSchema(dataSource, project)
         * val labels = svc.getCachedLabels()
         * ```
         *
         * @param project The [Project] whose service instance to return.
         * @return The [SchemaIntrospectionService] for [project].
         */
        fun getInstance(project: Project): SchemaIntrospectionService = project.service()
    }
}
