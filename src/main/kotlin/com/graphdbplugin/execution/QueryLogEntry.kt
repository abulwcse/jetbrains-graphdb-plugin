package com.graphdbplugin.execution

import java.time.Instant

/**
 * Immutable record of a single query execution attempt.
 *
 * Stored in the query log and displayed in [com.graphdbplugin.results.QueryLogPanel].
 *
 * @property id              Unique identifier for this log entry (UUID).
 * @property timestamp       The instant at which the query was dispatched.
 * @property dataSourceName  Human-readable name of the data source it ran against.
 * @property queryText       The full Cypher text that was submitted.
 * @property status          Whether the execution succeeded or failed.
 * @property durationMs      Elapsed wall-clock time in milliseconds, or -1 if unknown.
 * @property rowCount        Number of result records returned (0 for write queries), or -1 if failed.
 * @property errorMessage    The exception message if [status] is [Status.FAILURE]; null otherwise.
 */
data class QueryLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val dataSourceName: String,
    val queryText: String,
    val status: Status,
    val durationMs: Long = -1L,
    val rowCount: Int = -1,
    val errorMessage: String? = null
) {
    /** Execution outcome. */
    enum class Status { SUCCESS, FAILURE, RUNNING }

    /** Returns a short display string, e.g. "42 rows in 18 ms". */
    fun summaryText(): String = when (status) {
        Status.SUCCESS -> "$rowCount row(s) in ${durationMs} ms"
        Status.FAILURE -> "Error: ${errorMessage?.take(80) ?: "unknown"}"
        Status.RUNNING -> "Running…"
    }
}
