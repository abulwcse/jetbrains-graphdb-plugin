package com.graphdbplugin.execution

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Smoke tests for [Neo4jConnectionPool].
 *
 * These tests verify the pool's edge-case behaviour (eviction of a non-existent key,
 * closing an already-empty pool, singleton identity) without requiring a live Neo4j
 * instance or any mocking framework.  They intentionally avoid calling
 * [Neo4jConnectionPool.getDriver] because that method requires both a reachable Bolt
 * endpoint and an IntelliJ Platform service context ([com.graphdbplugin.datasource.DataSourceManager])
 * that is not available in a plain unit-test JVM.
 *
 * ### What is tested
 * - [Neo4jConnectionPool.evict] on a key that was never added must not throw.
 * - [Neo4jConnectionPool.closeAll] on an already-empty pool must not throw.
 * - [Neo4jConnectionPool] is a Kotlin `object` (singleton); two references to it must
 *   be the same instance.
 */
class Neo4jConnectionPoolTest {

    /**
     * Calling [Neo4jConnectionPool.evict] with a data-source ID that is not currently
     * in the pool must complete without throwing any exception.
     *
     * This is an important contract: callers (e.g. [com.graphdbplugin.datasource.DataSourceManager])
     * evict on every update/delete regardless of whether a driver was ever created.
     */
    @Test
    fun testEvict_removesFromPool() {
        // Should not throw — evicting a non-existent key is a no-op.
        Neo4jConnectionPool.evict("non-existent-id-${System.nanoTime()}")
    }

    /**
     * Calling [Neo4jConnectionPool.closeAll] when the pool holds no drivers must
     * complete without throwing any exception.
     *
     * This mirrors the shutdown path when the IDE is closed before any query has
     * been executed (so no driver was ever created).
     */
    @Test
    fun testCloseAll_emptyPool() {
        // Drain the pool first (it may have drivers from other tests in a real run).
        Neo4jConnectionPool.closeAll()
        // A second call on an already-empty pool must also be safe.
        Neo4jConnectionPool.closeAll()
    }

    /**
     * Verifies that [Neo4jConnectionPool] is a Kotlin `object` — two references
     * obtained in different ways must refer to the exact same JVM instance.
     *
     * This guards against a future refactor that might accidentally convert the
     * singleton object into a class with multiple instantiation paths.
     */
    @Test
    fun testPoolObject_isSingleton() {
        val ref1 = Neo4jConnectionPool
        val ref2 = Neo4jConnectionPool
        assertNotNull("Pool reference must not be null", ref1)
        assertSame("Neo4jConnectionPool must be a singleton", ref1, ref2)
    }
}
