package com.graphdbplugin.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for [QueryLogEntry].
 *
 * Verifies summary text generation for all three [QueryLogEntry.Status] values,
 * default field values (UUID format, recent timestamp), and data-class semantics
 * (copy and structural equality).
 */
class QueryLogEntryTest {

    // -------------------------------------------------------------------------
    // summaryText tests
    // -------------------------------------------------------------------------

    /**
     * A SUCCESS entry with rowCount=10 and durationMs=50 should produce a
     * summary containing "10 row(s)" and "50 ms".
     */
    @Test
    fun testSuccessSummaryText() {
        val entry = QueryLogEntry(
            dataSourceName = "local-neo4j",
            queryText      = "MATCH (n) RETURN n",
            status         = QueryLogEntry.Status.SUCCESS,
            durationMs     = 50L,
            rowCount       = 10
        )
        val summary = entry.summaryText()
        assertTrue(summary.contains("10 row(s)"),
            "Expected summary to contain '10 row(s)' but was: $summary")
        assertTrue(summary.contains("50 ms"),
            "Expected summary to contain '50 ms' but was: $summary")
    }

    /**
     * A FAILURE entry with errorMessage="Connection refused" should produce a
     * summary containing that error message.
     */
    @Test
    fun testFailureSummaryText() {
        val entry = QueryLogEntry(
            dataSourceName = "remote-neo4j",
            queryText      = "MATCH (n) RETURN n",
            status         = QueryLogEntry.Status.FAILURE,
            errorMessage   = "Connection refused"
        )
        val summary = entry.summaryText()
        assertTrue(summary.contains("Connection refused"),
            "Expected summary to contain 'Connection refused' but was: $summary")
    }

    /**
     * A RUNNING entry should return exactly "Running…".
     */
    @Test
    fun testRunningSummaryText() {
        val entry = QueryLogEntry(
            dataSourceName = "local-neo4j",
            queryText      = "MATCH (n) RETURN n",
            status         = QueryLogEntry.Status.RUNNING
        )
        assertEquals("Running…", entry.summaryText())
    }

    // -------------------------------------------------------------------------
    // Default field value tests
    // -------------------------------------------------------------------------

    /**
     * The default [QueryLogEntry.id] must be a non-blank string that matches a
     * UUID pattern (8-4-4-4-12 hex groups).
     */
    @Test
    fun testDefaultId() {
        val entry = QueryLogEntry(
            dataSourceName = "test-ds",
            queryText      = "RETURN 1",
            status         = QueryLogEntry.Status.SUCCESS
        )
        val uuidPattern = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            RegexOption.IGNORE_CASE
        )
        assertTrue(entry.id.isNotBlank(), "id should not be blank")
        assertTrue(uuidPattern.matches(entry.id),
            "id '${entry.id}' does not match UUID format")
    }

    /**
     * The default [QueryLogEntry.timestamp] must be within the last 5 seconds of now,
     * confirming it is set at construction time and not to some fixed epoch value.
     */
    @Test
    fun testTimestampIsRecent() {
        val before = Instant.now().minusSeconds(5)
        val entry = QueryLogEntry(
            dataSourceName = "test-ds",
            queryText      = "RETURN 1",
            status         = QueryLogEntry.Status.SUCCESS
        )
        val after = Instant.now()
        assertTrue(entry.timestamp.isAfter(before),
            "timestamp should be after (now - 5s)")
        assertTrue(!entry.timestamp.isAfter(after),
            "timestamp should not be in the future")
    }

    // -------------------------------------------------------------------------
    // Data class semantics
    // -------------------------------------------------------------------------

    /**
     * As a data class, [QueryLogEntry] must support [copy] and structural equality.
     * Two entries created with identical arguments (except auto-generated id/timestamp
     * overridden with fixed values) must be equal.
     */
    @Test
    fun testDataClassName() {
        val fixedInstant = Instant.ofEpochSecond(1_700_000_000L)
        val original = QueryLogEntry(
            id             = "fixed-id",
            timestamp      = fixedInstant,
            dataSourceName = "ds",
            queryText      = "RETURN 1",
            status         = QueryLogEntry.Status.SUCCESS,
            durationMs     = 5L,
            rowCount       = 1
        )
        // copy() must produce an equal instance
        val copy = original.copy()
        assertEquals(original, copy, "copy() should produce an equal entry")

        // copy() with changed field must differ
        val modified = original.copy(rowCount = 99)
        assertNotEquals(original, modified,
            "copy with different rowCount should not be equal to original")
        assertEquals(99, modified.rowCount)
        // Other fields should be preserved
        assertEquals(original.id, modified.id)
        assertEquals(original.queryText, modified.queryText)
    }
}
