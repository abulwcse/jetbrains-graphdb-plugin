package com.graphdbplugin.datasource

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DataSourceManager].
 *
 * These tests exercise the in-memory CRUD operations of [DataSourceManager] without
 * any IntelliJ Platform infrastructure or OS keychain access. The approach is:
 *
 * 1. A fresh [DataSourceManagerState] is created in [setup].
 * 2. A [TestableDataSourceManager] subclass is used that overrides the PasswordSafe
 *    methods with no-op stubs, completely avoiding the IDE runtime dependency.
 *
 * This means the tests can run as plain JVM unit tests (JUnit 5 via Gradle's
 * `useJUnitPlatform()`) without requiring an IDE sandbox.
 *
 * ### What is NOT tested here
 * - The actual PasswordSafe / OS keychain integration (that requires a running IDE).
 * - Persistence to disk (XML serialisation) — covered by integration tests in Phase 2.
 * - UI components — covered by UI tests in Phase 2.
 */
@DisplayName("DataSourceManager — unit tests")
class DataSourceManagerTest {

    /**
     * Subclass of [DataSourceManager] that stubs out PasswordSafe interactions.
     *
     * By overriding [savePassword] and [getPassword] to use an in-memory map we
     * avoid any dependency on the IntelliJ Platform runtime during unit testing.
     */
    private class TestableDataSourceManager : DataSourceManager() {

        /** In-memory credential store replacing the OS keychain during tests. */
        private val passwordStore = mutableMapOf<String, String>()

        /**
         * Stores [password] in the in-memory map instead of the OS keychain.
         *
         * @param dataSourceId The data source UUID.
         * @param password     The plaintext password to store.
         */
        override fun savePassword(dataSourceId: String, password: String) {
            if (password.isNotBlank()) passwordStore[dataSourceId] = password
        }

        /**
         * Retrieves a password from the in-memory map.
         *
         * @param dataSourceId The data source UUID.
         * @return The stored password, or `null` if none was stored.
         */
        override fun getPassword(dataSourceId: String): String? = passwordStore[dataSourceId]
    }

    /** Fresh manager instance created before each test. */
    private lateinit var manager: TestableDataSourceManager

    /**
     * Creates a new [TestableDataSourceManager] and injects a clean
     * [DataSourceManagerState] before each test method to ensure test isolation.
     */
    @BeforeEach
    fun setup() {
        manager = TestableDataSourceManager()
        // Inject a fresh empty state to ensure no test bleeds into another.
        manager.loadState(DataSourceManagerState())
    }

    // =========================================================================
    // Tests
    // =========================================================================

    /**
     * Verifies that adding a single data source results in exactly one entry
     * being returned by [DataSourceManager.getAllDataSources].
     */
    @Test
    @DisplayName("addDataSource — single entry is persisted")
    fun testAddDataSource() {
        val ds = BoltDataSource.create("Test DB", "bolt://localhost:7687")
        manager.addDataSource(ds)

        val all = manager.getAllDataSources()
        assertEquals(1, all.size, "Expected exactly one data source after adding one")
        assertEquals(ds, all.first())
    }

    /**
     * Verifies that adding three distinct data sources results in all three
     * being present in [DataSourceManager.getAllDataSources], in insertion order.
     */
    @Test
    @DisplayName("addDataSource — multiple entries are all persisted")
    fun testAddMultipleDataSources() {
        val ds1 = BoltDataSource.create("DB 1", "bolt://localhost:7687")
        val ds2 = BoltDataSource.create("DB 2", "bolt://localhost:7688")
        val ds3 = BoltDataSource.create("DB 3", "bolt://localhost:7689")

        manager.addDataSource(ds1)
        manager.addDataSource(ds2)
        manager.addDataSource(ds3)

        val all = manager.getAllDataSources()
        assertEquals(3, all.size, "Expected exactly three data sources")
        assertEquals(listOf(ds1, ds2, ds3), all)
    }

    /**
     * Verifies that removing a data source by ID correctly reduces the list size
     * and that the remaining entry is the expected one.
     */
    @Test
    @DisplayName("removeDataSource — correct entry is removed, other entry remains")
    fun testRemoveDataSource() {
        val keep = BoltDataSource.create("Keep Me", "bolt://keep:7687")
        val remove = BoltDataSource.create("Delete Me", "bolt://delete:7687")

        manager.addDataSource(keep)
        manager.addDataSource(remove)
        assertEquals(2, manager.getAllDataSources().size)

        manager.removeDataSource(remove.id)

        val remaining = manager.getAllDataSources()
        assertEquals(1, remaining.size, "Expected exactly one data source after removal")
        assertEquals(keep, remaining.first(), "The remaining source should be the one that was kept")
    }

    /**
     * Verifies that [DataSourceManager.updateDataSource] replaces the entry with the
     * same ID and that [DataSourceManager.findById] reflects the updated name.
     */
    @Test
    @DisplayName("updateDataSource — updated entry is returned by findById")
    fun testUpdateDataSource() {
        val original = BoltDataSource.create("Original Name", "bolt://localhost:7687")
        manager.addDataSource(original)

        val updated = original.copy(name = "Updated Name")
        manager.updateDataSource(updated)

        val found = manager.findById(original.id)
        assertNotNull(found, "findById should return a result after update")
        assertEquals("Updated Name", found!!.name, "The name should reflect the update")
    }

    /**
     * Verifies that [DataSourceManager.findById] returns the correct [BoltDataSource]
     * when the requested ID exists in the managed list.
     */
    @Test
    @DisplayName("findById — returns the correct data source when ID exists")
    fun testFindById_existing() {
        val ds = BoltDataSource.create("My DB", "bolt://localhost:7687")
        manager.addDataSource(ds)

        val found = manager.findById(ds.id)
        assertNotNull(found, "findById should not return null for an existing ID")
        assertEquals(ds, found)
    }

    /**
     * Verifies that [DataSourceManager.findById] returns `null` when the requested
     * ID does not match any managed data source.
     */
    @Test
    @DisplayName("findById — returns null when ID does not exist")
    fun testFindById_notExisting() {
        val ds = BoltDataSource.create("Some DB", "bolt://localhost:7687")
        manager.addDataSource(ds)

        val found = manager.findById("non-existent-uuid-1234")
        assertNull(found, "findById should return null for an unknown ID")
    }

    /**
     * Verifies that a [BoltDataSource] created via the factory method has the
     * expected default field values.
     */
    @Test
    @DisplayName("BoltDataSource factory defaults — all default fields have expected values")
    fun testDataSourceDefaultValues() {
        val ds = BoltDataSource.create("Default Test", "bolt://localhost:7687")

        assertEquals("bolt://localhost:7687", ds.url)
        assertEquals("neo4j", ds.username)
        assertEquals("neo4j", ds.database)
        assertEquals(false, ds.sslEnabled)
        assertEquals(30, ds.connectionTimeoutSeconds)
        assertEquals("#4A90D9", ds.color)
        assertTrue(ds.id.isNotBlank(), "ID must not be blank")
    }

    /**
     * Verifies structural equality of [BoltDataSource]: two copies of the same
     * instance (identical field values) must be considered equal.
     */
    @Test
    @DisplayName("BoltDataSource equality — copy() of the same instance is equal")
    fun testBoltDataSourceEquality() {
        val original = BoltDataSource.create("EQ Test", "bolt://localhost:7687")
        val copy = original.copy() // same id, same all fields

        assertEquals(original, copy, "A data class copy with identical fields must be equal")
    }

    /**
     * Verifies that [DataSourceManagerState] can hold multiple sources and they
     * can be iterated correctly — simulating what the XML serialiser does.
     */
    @Test
    @DisplayName("DataSourceManagerState — holds and returns multiple sources correctly")
    fun testDataSourceManagerState_serialization() {
        val state = DataSourceManagerState()
        val ds1 = BoltDataSource.create("State DS 1", "bolt://localhost:7687")
        val ds2 = BoltDataSource.create("State DS 2", "bolt://localhost:7688")
        val ds3 = BoltDataSource.create("State DS 3", "bolt://localhost:7689")

        state.dataSources.add(ds1)
        state.dataSources.add(ds2)
        state.dataSources.add(ds3)

        assertEquals(3, state.dataSources.size, "State should hold all three added sources")
        assertTrue(state.dataSources.contains(ds1))
        assertTrue(state.dataSources.contains(ds2))
        assertTrue(state.dataSources.contains(ds3))

        // Simulate loadState: inject into a fresh manager and verify retrieval.
        val freshManager = TestableDataSourceManager()
        freshManager.loadState(state)

        val retrieved = freshManager.getAllDataSources()
        assertEquals(3, retrieved.size)
        assertEquals(ds1, retrieved[0])
        assertEquals(ds2, retrieved[1])
        assertEquals(ds3, retrieved[2])
    }
}
