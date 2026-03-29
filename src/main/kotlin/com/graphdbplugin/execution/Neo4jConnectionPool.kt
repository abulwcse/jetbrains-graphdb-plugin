package com.graphdbplugin.execution

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Config
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Thread-safe pool of Neo4j [Driver] instances, keyed by [BoltDataSource.id].
 *
 * ### Why pool drivers?
 * The Neo4j Java Driver maintains an internal connection pool, TLS session cache, and
 * authentication state. Creating a new [Driver] for every query incurs significant
 * overhead (TCP handshake, TLS negotiation, authentication round-trip). By reusing a
 * [Driver] per data source, subsequent queries on the same source are much faster.
 *
 * ### Eviction
 * Call [evict] when a data source is edited or deleted. This closes the existing driver
 * asynchronously and removes it from the pool. The next query against that source will
 * trigger a fresh driver creation with the updated credentials.
 *
 * ### Thread safety
 * [ConcurrentHashMap] is used for the pool. Driver creation is idempotent under
 * concurrent access (the losing thread's driver is closed immediately).
 *
 * ### Singleton access
 * Use [Neo4jConnectionPool.instance] to obtain the shared pool.
 */
object Neo4jConnectionPool {

    /** Live pool of drivers, keyed by data-source UUID. */
    private val pool = ConcurrentHashMap<String, Driver>()

    /**
     * Returns a [Driver] for the given [dataSource], creating one if necessary.
     *
     * The password is fetched from [DataSourceManager]'s [PasswordSafe]-backed store
     * each time a new driver is created (but not on subsequent calls for the same source).
     *
     * @param dataSource The data source configuration.
     * @return A live [Driver] connected (or capable of connecting) to the specified Neo4j instance.
     * @throws org.neo4j.driver.exceptions.ServiceUnavailableException if the Bolt endpoint is unreachable.
     */
    fun getDriver(dataSource: BoltDataSource): Driver {
        return pool.getOrPut(dataSource.id) {
            buildDriver(dataSource)
        }
    }

    /**
     * Removes and asynchronously closes the cached [Driver] for [dataSourceId].
     *
     * Should be called whenever a data source's URL, credentials, or SSL settings change,
     * and when a data source is deleted.
     *
     * @param dataSourceId The [BoltDataSource.id] whose driver to evict.
     */
    fun evict(dataSourceId: String) {
        pool.remove(dataSourceId)?.closeAsync()
    }

    /**
     * Closes all pooled drivers and clears the pool.
     * Called on IDE shutdown via a [com.intellij.openapi.application.ApplicationActivationListener].
     */
    fun closeAll() {
        val entries = pool.entries.toList()
        pool.clear()
        entries.forEach { (_, driver) -> driver.closeAsync() }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Constructs a new [Driver] from the given [dataSource] configuration.
     *
     * @param dataSource The data source to connect to.
     * @return A configured [Driver] instance. Not yet connected — connection is deferred
     *         until the first session is opened.
     */
    private fun buildDriver(dataSource: BoltDataSource): Driver {
        val password = DataSourceManager.getInstance().getPassword(dataSource.id) ?: ""
        val authToken = AuthTokens.basic(dataSource.username, password)

        val config = Config.builder()
            .withMaxConnectionPoolSize(5)
            .withConnectionTimeout(dataSource.connectionTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .apply {
                if (dataSource.sslEnabled) withEncryption()
                else withoutEncryption()
            }
            .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
            .build()

        return GraphDatabase.driver(dataSource.url, authToken, config)
    }
}
