package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.intellij.ui.components.JBTabbedPane
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Root panel for the "GraphDB Results" tool window.
 *
 * ### Layout
 * ```
 * ┌──────────────────────────────────────────────────┐
 * │  QuerySummaryPanel  (query text + status banner) │  ← always visible
 * ├──────────────────────────────────────────────────┤
 * │  ┌──────────┬───────┬────────┬───────────────┐  │
 * │  │Query Log │ Graph │ Table  │  Parameters   │  │  ← tabs
 * │  └──────────┴───────┴────────┴───────────────┘  │
 * │  [tab content]                                   │
 * └──────────────────────────────────────────────────┘
 * ```
 *
 * The [QuerySummaryPanel] at the top shows — for every query — the query text,
 * status (SUCCESS / FAILURE), duration, and either the row count or the full
 * error message. It is always visible regardless of which tab is selected.
 *
 * ### Tab selection after execution
 * - **Failure** → Query Log tab (index 0) so the full error detail is one click away.
 * - **Success with graph data** → Graph tab (index 1).
 * - **Success with scalar data** → Table tab (index 2).
 *
 */
class ResultPanel : JPanel(BorderLayout()) {

    private val summaryPanel     = QuerySummaryPanel()
    private val queryLogPanel    = QueryLogPanel()
    private val graphPanel       = GraphVisualizationPanel()
    private val tablePanel       = TableResultPanel()
    val parametersPanel          = ParametersPanel()

    private val tabbedPane = JBTabbedPane().apply {
        addTab("Query Log",  queryLogPanel)
        addTab("Graph",      graphPanel)
        addTab("Table",      tablePanel)
        addTab("Parameters", parametersPanel)
    }

    init {
        add(summaryPanel, BorderLayout.NORTH)
        add(tabbedPane,   BorderLayout.CENTER)
    }

    /**
     * Pushes [result] to the summary banner and all relevant tabs, then selects
     * the most appropriate tab.
     *
     * Must be called on the EDT.
     *
     * @param result The result of a Cypher query execution.
     */
    fun displayResult(result: QueryResult) {
        // Always update the top summary banner
        summaryPanel.update(result)

        // Always append to the query log
        queryLogPanel.addEntry(result)

        when (result) {
            is QueryResult.Success -> {
                tablePanel.displayResult(result)
                graphPanel.displayResult(result)
                val hasGraphValues = result.records.any { record ->
                    record.values().any { v ->
                        val t = v.type().name()
                        t == "NODE" || t == "RELATIONSHIP" || t == "PATH"
                    }
                }
                tabbedPane.selectedIndex = if (hasGraphValues) 1 else 2
            }
            is QueryResult.Failure -> {
                // Switch to Query Log so the user sees the error detail immediately
                tabbedPane.selectedIndex = 0
            }
        }
    }

    /** Forwards query text to [ParametersPanel] so it can sync its fields. */
    fun syncQueryParams(queryText: String) = parametersPanel.syncWithQuery(queryText)

    /** Returns the current parameter values for use in query execution. */
    fun getParams(): Map<String, Any> = parametersPanel.getParams()
}
