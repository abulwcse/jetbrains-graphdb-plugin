package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

/**
 * Panel that renders a [QueryResult.Success] as a scrollable table grid.
 *
 * Column names are derived from the first result record's keys. Each subsequent
 * row corresponds to one [org.neo4j.driver.Record]. Cell values are converted to
 * display strings via [valueToDisplayString].
 *
 * The table is cleared and repopulated on each call to [displayResult]; it does not
 * accumulate results across queries.
 *
 * A status label above the table shows the column count and row count.
 */
class TableResultPanel : JPanel(BorderLayout()) {

    private val statusLabel = JLabel(" ")
    private val tableModel  = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val table = JBTable(tableModel)

    init {
        val topPanel = JPanel(BorderLayout()).apply { add(statusLabel, BorderLayout.WEST) }
        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
    }

    /**
     * Populates the table with [result]'s records.
     *
     * Clears any previously displayed data before populating. Must be called on the EDT.
     *
     * @param result A [QueryResult.Success] containing records to display.
     */
    fun displayResult(result: QueryResult.Success) {
        // Rebuild the model
        tableModel.setRowCount(0)
        tableModel.setColumnCount(0)

        val columns = result.columns
        if (columns.isEmpty() && result.records.isEmpty()) {
            statusLabel.text = "  Query completed — no records returned."
            return
        }

        columns.forEach { tableModel.addColumn(it) }

        result.records.forEach { record ->
            val row = columns.map { col ->
                valueToDisplayString(record.get(col))
            }.toTypedArray()
            tableModel.addRow(row)
        }

        statusLabel.text = "  ${columns.size} column(s), ${result.records.size} row(s) — ${result.durationMs} ms"
        // Auto-fit column widths (simple heuristic: min 60, max 300)
        for (i in 0 until table.columnCount) {
            val col = table.columnModel.getColumn(i)
            col.minWidth    = 60
            col.preferredWidth = minOf(300, maxOf(80, col.headerValue.toString().length * 10))
        }
    }

    /**
     * Converts a Neo4j [org.neo4j.driver.Value] to a human-readable display string.
     *
     * - `NULL` → `"null"`
     * - Node → `"(id:<id> :<label1>:<label2> {prop: val, …})"`
     * - Relationship → `"[:<TYPE> {prop: val}]"`
     * - List → `"[a, b, c]"`
     * - Map → `"{k: v, …}"`
     * - Everything else → `value.toString()`
     *
     * @param value The [org.neo4j.driver.Value] to convert.
     * @return A display-friendly string representation.
     */
    private fun valueToDisplayString(value: org.neo4j.driver.Value): String {
        if (value.isNull) return "null"
        return when (value.type().name()) {
            "NODE" -> {
                val node = value.asNode()
                val labels = node.labels().joinToString(":")
                val props = node.asMap().entries.take(3).joinToString(", ") { "${it.key}: ${it.value}" }
                "(id:${node.id()} :$labels {$props})"
            }
            "RELATIONSHIP" -> {
                val rel = value.asRelationship()
                val props = rel.asMap().entries.take(3).joinToString(", ") { "${it.key}: ${it.value}" }
                "[:${rel.type()} {$props}]"
            }
            "PATH" -> {
                val path = value.asPath()
                "Path(${path.length()} hops)"
            }
            "LIST" -> value.asList { it.toString() }.toString()
            "MAP"  -> value.asMap { it.toString() }.toString()
            else   -> value.toString()
        }
    }
}
