package com.graphdbplugin.datasource

import com.graphdbplugin.execution.Neo4jConnectionPool
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level service that manages the lifecycle of all [BoltDataSource]
 * configurations and their associated credentials.
 *
 * ### Persistence
 * Implements [PersistentStateComponent] so that the list of data sources survives
 * IDE restarts. The state is written to `graphdb-datasources.xml` inside the IDE's
 * application-level configuration directory (typically `~/.config/JetBrains/<IDE>/`
 * on Linux, `~/Library/Application Support/JetBrains/<IDE>/` on macOS).
 *
 * **Passwords are never included in the XML file.** They are stored in the OS
 * native keychain via IntelliJ's [PasswordSafe] abstraction, keyed by each data
 * source's UUID.
 *
 * ### Service registration
 * This service is registered at [Service.Level.APP] and declared in `plugin.xml`.
 * Use [DataSourceManager.getInstance] to obtain the singleton instance from anywhere
 * in the plugin.
 *
 * ### Thread safety
 * All public methods should be called on the Event Dispatch Thread (EDT) or wrapped
 * in a write action when mutating state. For Phase 1 this is acceptable because all
 * callers are UI actions running on the EDT. Phase 2 async queries will require
 * additional synchronisation.
 */
@Service(Service.Level.APP)
@State(
    name = "GraphDbDataSources",
    storages = [Storage("graphdb-datasources.xml")]
)
class DataSourceManager : PersistentStateComponent<DataSourceManagerState> {

    /** Internal mutable state; never exposed directly. */
    private var state: DataSourceManagerState = DataSourceManagerState()

    // =========================================================================
    // PersistentStateComponent implementation
    // =========================================================================

    /**
     * Returns a snapshot of the current state for serialisation to XML.
     *
     * The IntelliJ Platform framework calls this method whenever it needs to
     * persist the plugin's state to disk (e.g. when the IDE is closing or after
     * each user-initiated change).
     *
     * @return The current [DataSourceManagerState] instance.
     */
    override fun getState(): DataSourceManagerState = state

    /**
     * Called by the IntelliJ Platform framework when deserialising a previously
     * saved state from XML (typically at IDE startup).
     *
     * Uses [XmlSerializerUtil.copyBean] to merge the deserialised [state] into
     * the existing instance rather than replacing the reference, which avoids
     * stale-reference bugs in classes that hold a direct reference to the state.
     *
     * @param state The deserialised state loaded from `graphdb-datasources.xml`.
     */
    override fun loadState(state: DataSourceManagerState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    // =========================================================================
    // Data-source CRUD
    // =========================================================================

    /**
     * Appends a new [BoltDataSource] to the managed list.
     *
     * The caller is responsible for having already saved the password via
     * [savePassword] before or after calling this method. This method does NOT
     * touch the keychain.
     *
     * @param dataSource The new data source to persist. Its [BoltDataSource.id]
     *                   must be unique among all currently managed sources.
     */
    fun addDataSource(dataSource: BoltDataSource) {
        state.dataSources.add(dataSource)
    }

    /**
     * Replaces an existing [BoltDataSource] identified by [BoltDataSource.id] with
     * an updated copy.
     *
     * If no source with the given [id][BoltDataSource.id] is found, this method is
     * a no-op. This is a safe operation — callers do not need to handle a "not found"
     * case explicitly.
     *
     * @param dataSource Updated data source. The [BoltDataSource.id] is used to
     *                   locate the existing entry; all other fields are replaced.
     */
    fun updateDataSource(dataSource: BoltDataSource) {
        val index = state.dataSources.indexOfFirst { it.id == dataSource.id }
        if (index >= 0) {
            state.dataSources[index] = dataSource
        }
        // Evict the stale pooled driver so the next query uses updated credentials/URL.
        Neo4jConnectionPool.evict(dataSource.id)
    }

    /**
     * Removes the data source with the given [id] from the managed list **and**
     * deletes its stored password from the OS keychain.
     *
     * If no source with the given [id] exists, this method is a no-op.
     *
     * @param id The [BoltDataSource.id] of the data source to delete.
     */
    fun removeDataSource(id: String) {
        state.dataSources.removeIf { it.id == id }
        // Clear the stored password from the OS keychain so we don't leave
        // orphaned credentials behind.
        clearPassword(id)
        // Evict the pooled driver so its connections are released immediately.
        Neo4jConnectionPool.evict(id)
    }

    /**
     * Returns an immutable snapshot of all currently managed data sources.
     *
     * The returned list is a copy; modifications to it do not affect the
     * internal state. Use [addDataSource], [updateDataSource], or
     * [removeDataSource] to mutate the list.
     *
     * @return A new [List] containing all [BoltDataSource] instances in insertion order.
     */
    fun getAllDataSources(): List<BoltDataSource> = state.dataSources.toList()

    /**
     * Looks up a data source by its unique identifier.
     *
     * @param id The [BoltDataSource.id] to search for.
     * @return The matching [BoltDataSource], or `null` if no such source exists.
     */
    fun findById(id: String): BoltDataSource? =
        state.dataSources.firstOrNull { it.id == id }

    // =========================================================================
    // Password / credential management via OS keychain
    // =========================================================================

    /**
     * Persists the given [password] for a data source in the OS native keychain
     * via IntelliJ's [PasswordSafe] abstraction.
     *
     * The keychain entry is keyed by [dataSourceId] so that it can be retrieved
     * or deleted independently of the data-source name (which the user may change).
     *
     * This method is a no-op if [password] is blank — it will not overwrite an
     * existing stored password with an empty string.
     *
     * @param dataSourceId The [BoltDataSource.id] whose password to store.
     * @param password     The plaintext password. Must not be blank.
     */
    open fun savePassword(dataSourceId: String, password: String) {
        if (password.isBlank()) return
        val attrs = credentialAttributesFor(dataSourceId)
        PasswordSafe.instance.set(attrs, Credentials(dataSourceId, password))
    }

    /**
     * Retrieves the stored password for the given data source from the OS keychain.
     *
     * @param dataSourceId The [BoltDataSource.id] whose password to retrieve.
     * @return The stored plaintext password, or `null` if no entry was found
     *         (e.g. the data source was just created or the keychain was cleared).
     */
    open fun getPassword(dataSourceId: String): String? {
        val attrs = credentialAttributesFor(dataSourceId)
        return PasswordSafe.instance.getPassword(attrs)
    }

    /**
     * Removes the stored password for the given data source from the OS keychain.
     *
     * Called automatically by [removeDataSource] to avoid leaving orphaned
     * credentials in the keychain after a data source is deleted.
     *
     * @param dataSourceId The [BoltDataSource.id] whose keychain entry to delete.
     */
    private fun clearPassword(dataSourceId: String) {
        val attrs = credentialAttributesFor(dataSourceId)
        PasswordSafe.instance.set(attrs, null)
    }

    /**
     * Builds the [CredentialAttributes] key used to store/retrieve credentials
     * in the OS keychain for a specific data source.
     *
     * The service name is derived from the plugin's group ID combined with the
     * [dataSourceId], ensuring a unique, stable keychain slot per data source.
     *
     * @param dataSourceId The [BoltDataSource.id] to build attributes for.
     * @return A [CredentialAttributes] instance suitable for use with [PasswordSafe].
     */
    private fun credentialAttributesFor(dataSourceId: String): CredentialAttributes =
        CredentialAttributes(
            generateServiceName("GraphDB Plugin", dataSourceId),
            dataSourceId
        )

    // =========================================================================
    // Companion object
    // =========================================================================

    companion object {

        /**
         * Returns the application-level singleton [DataSourceManager] instance.
         *
         * This is the preferred entry point for all code that needs to interact
         * with the data-source registry. Example:
         *
         * ```kotlin
         * val manager = DataSourceManager.getInstance()
         * val sources = manager.getAllDataSources()
         * ```
         *
         * @return The [DataSourceManager] singleton registered with the IDE.
         */
        fun getInstance(): DataSourceManager = service()
    }
}
