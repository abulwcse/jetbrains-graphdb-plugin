package com.graphdbplugin.execution

import org.neo4j.driver.Record
import org.neo4j.driver.summary.ResultSummary

/**
 * Sealed hierarchy representing the outcome of a Cypher query execution.
 *
 * Returned by [QueryExecutor.execute] and consumed by the result panels.
 *
 * @see Success for a successful result with records and summary.
 * @see Failure for a failed result with the originating exception.
 */
sealed class QueryResult {

    /**
     * A successful query execution.
     *
     * @property records     All result records collected from the Neo4j result cursor.
     *                       May be empty for write-only queries.
     * @property summary     Neo4j driver [ResultSummary] containing counters, query plan,
     *                       notifications, and server info.
     * @property durationMs  Wall-clock elapsed time from query dispatch to full result collection.
     * @property queryText   The Cypher query that produced this result.
     * @property dataSourceName Human-readable name of the data source.
     */
    data class Success(
        val records: List<Record>,
        val summary: ResultSummary,
        val durationMs: Long,
        val queryText: String,
        val dataSourceName: String
    ) : QueryResult() {
        /** Column names in the order they appear in the result. */
        val columns: List<String> get() = if (records.isNotEmpty()) records.first().keys() else emptyList()
    }

    /**
     * A failed query execution.
     *
     * @property error          The exception thrown by the driver or network layer.
     * @property queryText      The Cypher query that was attempted.
     * @property dataSourceName Human-readable name of the data source.
     */
    data class Failure(
        val error: Throwable,
        val queryText: String,
        val dataSourceName: String
    ) : QueryResult()
}
