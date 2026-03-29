package com.graphdbplugin.execution

import com.graphdbplugin.datasource.BoltDataSource
import org.neo4j.driver.SessionConfig

/**
 * Executes Cypher queries against a Neo4j data source via the Bolt protocol.
 *
 * All methods in this object are **blocking** and must be called from a background
 * thread — never from the Event Dispatch Thread (EDT).
 *
 * ### Usage
 * ```kotlin
 * AppExecutorUtil.getAppExecutorService().submit {
 *     val result = QueryExecutor.execute(dataSource, "MATCH (n) RETURN n LIMIT 10", emptyMap())
 *     ApplicationManager.getApplication().invokeLater {
 *         resultManager.displayResult(result)
 *     }
 * }
 * ```
 *
 * ### Parameter syntax
 * Parameters are passed as a [Map] where keys match `$paramName` references in the query
 * (without the leading `$`). Values must be types supported by the Neo4j driver:
 * [String], [Long], [Double], [Boolean], [List], or [Map].
 */
object QueryExecutor {

    /**
     * Executes the given [queryText] against [dataSource] with optional [parameters].
     *
     * Opens a new session for each query (the driver manages the underlying connection pool),
     * runs the query, collects all records eagerly into a [List], then closes the session.
     * Eager collection is safe for the typical interactive query sizes targeted by this plugin
     * (< 10,000 rows).
     *
     * @param dataSource  The [BoltDataSource] to execute the query against.
     * @param queryText   The Cypher query string. May contain `$param` references.
     * @param parameters  Map of parameter name → value for parameterised queries. Pass
     *                    [emptyMap] for queries without parameters.
     * @return [QueryResult.Success] if the query completed without error, or
     *         [QueryResult.Failure] if the driver threw an exception.
     */
    fun execute(
        dataSource: BoltDataSource,
        queryText: String,
        parameters: Map<String, Any> = emptyMap()
    ): QueryResult {
        val startMs = System.currentTimeMillis()
        return try {
            val driver = Neo4jConnectionPool.getDriver(dataSource)
            val sessionConfig = SessionConfig.builder()
                .withDatabase(dataSource.database)
                .build()

            driver.session(sessionConfig).use { session ->
                val result = session.run(queryText, parameters)
                val records = result.list()          // collect all records eagerly
                val summary = result.consume()       // fetch query summary / counters
                val durationMs = System.currentTimeMillis() - startMs

                QueryResult.Success(
                    records = records,
                    summary = summary,
                    durationMs = durationMs,
                    queryText = queryText,
                    dataSourceName = dataSource.name
                )
            }
        } catch (ex: Exception) {
            QueryResult.Failure(
                error = ex,
                queryText = queryText,
                dataSourceName = dataSource.name
            )
        }
    }
}
