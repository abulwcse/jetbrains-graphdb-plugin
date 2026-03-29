package com.graphdbplugin.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.neo4j.driver.Record
import org.neo4j.driver.summary.ResultSummary

/**
 * Unit tests for the [QueryResult] sealed hierarchy.
 *
 * Uses Mockito to create stub [Record] and [ResultSummary] instances so that
 * [QueryResult.Success] can be constructed without a live Neo4j connection.
 */
class QueryResultTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a [QueryResult.Success] with the given record list and optional metadata. */
    private fun successWith(
        records: List<Record> = emptyList(),
        durationMs: Long = 100L,
        queryText: String = "MATCH (n) RETURN n",
        dataSourceName: String = "test-ds"
    ): QueryResult.Success {
        val summary: ResultSummary = mock()
        return QueryResult.Success(
            records        = records,
            summary        = summary,
            durationMs     = durationMs,
            queryText      = queryText,
            dataSourceName = dataSourceName
        )
    }

    // -------------------------------------------------------------------------
    // Success tests
    // -------------------------------------------------------------------------

    /**
     * [QueryResult.Success.columns] must return an empty list when [records] is empty.
     */
    @Test
    fun testSuccessColumnsEmpty() {
        val success = successWith(records = emptyList())
        assertTrue(success.columns.isEmpty(),
            "columns should be empty when records list is empty")
    }

    /**
     * [QueryResult.Success.columns] must reflect the keys of the first record when
     * records are present.
     */
    @Test
    fun testSuccessColumnsFromFirstRecord() {
        val record: Record = mock()
        whenever(record.keys()).thenReturn(listOf("name", "age"))
        val success = successWith(records = listOf(record))
        assertEquals(listOf("name", "age"), success.columns)
    }

    /**
     * [QueryResult.Success.durationMs] must equal the value passed at construction.
     */
    @Test
    fun testSuccessDuration() {
        val success = successWith(durationMs = 42L)
        assertEquals(42L, success.durationMs)
    }

    /**
     * [QueryResult.Success.dataSourceName] must equal the value passed at construction.
     */
    @Test
    fun testSuccessDataSourceName() {
        val success = successWith(dataSourceName = "my-neo4j")
        assertEquals("my-neo4j", success.dataSourceName)
    }

    // -------------------------------------------------------------------------
    // Failure tests
    // -------------------------------------------------------------------------

    /**
     * [QueryResult.Failure.error] must be the exact [Throwable] instance passed at
     * construction — i.e. it must be reference-equal, not just message-equal.
     */
    @Test
    fun testFailureHoldsError() {
        val cause = RuntimeException("Connection refused")
        val failure = QueryResult.Failure(
            error          = cause,
            queryText      = "MATCH (n) RETURN n",
            dataSourceName = "test-ds"
        )
        assertSame(cause, failure.error,
            "Failure.error should be the same Throwable instance that was passed in")
    }

    // -------------------------------------------------------------------------
    // Sealed hierarchy / when exhaustiveness
    // -------------------------------------------------------------------------

    /**
     * A `when` expression over [QueryResult] must be able to match both [QueryResult.Success]
     * and [QueryResult.Failure] branches, confirming that the sealed hierarchy is complete
     * and that pattern matching works as expected.
     */
    @Test
    fun testSealedHierarchy() {
        val results: List<QueryResult> = listOf(
            successWith(),
            QueryResult.Failure(
                error          = IllegalStateException("oops"),
                queryText      = "BAD QUERY",
                dataSourceName = "ds"
            )
        )

        var successCount = 0
        var failureCount = 0

        for (result in results) {
            when (result) {
                is QueryResult.Success -> successCount++
                is QueryResult.Failure -> failureCount++
            }
        }

        assertEquals(1, successCount, "Expected exactly 1 Success in the list")
        assertEquals(1, failureCount, "Expected exactly 1 Failure in the list")
    }
}
