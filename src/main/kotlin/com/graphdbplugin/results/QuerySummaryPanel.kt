package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * A compact summary bar shown at the top of the result tool window after every query run.
 *
 * Displays the most recently executed query and its outcome at a glance:
 * - The query text (up to 3 lines, truncated beyond)
 * - A coloured status badge: green **SUCCESS** or red **FAILURE**
 * - Execution duration in milliseconds
 * - On success: row count returned
 * - On failure: the full root error message so the problem is immediately visible
 *
 * This panel is always visible regardless of which result tab is active, giving
 * instant feedback without requiring the user to navigate to the Query Log tab.
 *
 * Updated via [update] each time a new [QueryResult] arrives.
 */
class QuerySummaryPanel : JPanel(BorderLayout()) {

    // ---- Query text display ----
    private val queryArea = JTextArea().apply {
        isEditable  = false
        lineWrap    = false
        rows        = 1
        isOpaque    = false
        border      = JBUI.Borders.empty(4, 8, 2, 8)
        font        = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    // ---- Status / meta row ----
    private val statusLabel   = JBLabel()
    private val durationLabel = JBLabel()
    private val rowsLabel     = JBLabel()

    private val metaPanel = JPanel(FlowLayout(FlowLayout.LEFT, 12, 2)).apply {
        isOpaque = false
        add(statusLabel)
        add(durationLabel)
        add(rowsLabel)
        border = JBUI.Borders.emptyBottom(4)
    }

    init {
        isOpaque = true
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(Color(80, 80, 80), 0, 0, 1, 0),  // bottom divider line
            JBUI.Borders.empty(2)
        )
        add(queryArea,  BorderLayout.CENTER)
        add(metaPanel,  BorderLayout.SOUTH)
        // Initially hidden — shown after the first query
        isVisible = false
    }

    /**
     * Updates the panel with the result of the most recently executed query.
     *
     * Must be called on the EDT. Makes the panel visible if it was hidden.
     *
     * @param result The latest [QueryResult] to display.
     */
    fun update(result: QueryResult) {
        isVisible = true

        when (result) {
            is QueryResult.Success -> {
                queryArea.text = flattenQuery(result.queryText)
                statusLabel.text   = "<html><b><font color='#27AE60'>✓ SUCCESS</font></b></html>"
                durationLabel.text = "<html><font color='gray'>${result.durationMs} ms</font></html>"
                rowsLabel.text     = "<html><font color='gray'>${result.records.size} row(s)</font></html>"
            }
            is QueryResult.Failure -> {
                queryArea.text = flattenQuery(result.queryText)
                val errMsg = buildErrorSummary(result.error)
                statusLabel.text   = "<html><b><font color='#E74C3C'>✗ FAILED</font></b></html>"
                durationLabel.text = ""
                rowsLabel.text     =
                    "<html><font color='#E74C3C'>${escapeHtml(errMsg)}</font></html>"
            }
        }
        revalidate()
        repaint()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Collapses newlines/whitespace runs into single spaces and truncates for single-line display. */
    private fun flattenQuery(query: String): String {
        val flat = query.trim().replace(Regex("\\s*[\\r\\n]+\\s*"), " ")
        return if (flat.length > 120) flat.take(117) + "…" else flat
    }

    /**
     * Produces a concise error string from [error]: the exception's simple class name
     * plus message, followed by the immediate cause if present.
     */
    private fun buildErrorSummary(error: Throwable): String {
        val top = "${error.javaClass.simpleName}: ${error.message ?: "(no message)"}"
        val cause = error.cause
        return if (cause != null)
            "$top  ←  ${cause.javaClass.simpleName}: ${cause.message ?: ""}"
        else
            top
    }

    /** Escapes HTML special characters so error text renders correctly in a JLabel. */
    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .take(200)   // cap label length
}
