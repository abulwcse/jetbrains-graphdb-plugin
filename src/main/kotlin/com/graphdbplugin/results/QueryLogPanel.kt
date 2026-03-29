package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * Terminal-style log panel. Each query execution is rendered as a block of
 * coloured monospaced text — no table, no columns.
 *
 * Colour scheme is fixed dark-terminal so it looks consistent regardless of
 * the IDE theme. The public [addEntry] API is unchanged so [ResultPanel]
 * needs no modification.
 */
class QueryLogPanel : JPanel(BorderLayout()) {

    // ── Terminal palette ───────────────────────────────────────────────────────
    private val BG     = Color(0x1E, 0x1E, 0x1E)
    private val FG     = Color(0xCC, 0xCC, 0xCC)
    private val DIM    = Color(0x60, 0x60, 0x60)
    private val CYAN   = Color(0x56, 0xB6, 0xC2)
    private val GREEN  = Color(0x98, 0xC3, 0x79)
    private val RED    = Color(0xE0, 0x6C, 0x75)
    private val RULE   = Color(0x38, 0x38, 0x38)

    private val TERM_FONT = Font(Font.MONOSPACED, Font.PLAIN, 13)
    private val TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    // ── UI ────────────────────────────────────────────────────────────────────
    private val textPane = JTextPane().apply {
        isEditable = false
        background = BG
        foreground = FG
        font = TERM_FONT
        border = BorderFactory.createEmptyBorder(10, 14, 10, 14)
    }
    private val doc = textPane.styledDocument

    init {
        add(JBScrollPane(textPane).apply {
            border = null
            viewport.background = BG
        }, BorderLayout.CENTER)
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Appends a new terminal block for [result] and auto-scrolls to the bottom.
     * Must be called on the EDT.
     */
    fun addEntry(result: QueryResult) {
        val time = TIME_FMT.format(java.time.Instant.now())
        when (result) {
            is QueryResult.Success -> renderSuccess(time, result)
            is QueryResult.Failure -> renderFailure(time, result)
        }
        textPane.caretPosition = doc.length   // scroll to bottom
    }

    // ── Renderers ──────────────────────────────────────────────────────────────

    private fun renderSuccess(time: String, r: QueryResult.Success) {
        appendPromptLine(time, r.queryText, r.dataSourceName)
        rule()
        put("  ✓ SUCCESS", GREEN, bold = true)
        put("    ${r.records.size} row(s)    ${r.durationMs} ms\n", FG)
        rule()
        nl()
    }

    private fun renderFailure(time: String, r: QueryResult.Failure) {
        appendPromptLine(time, r.queryText, r.dataSourceName)
        rule()
        put("  ✗ FAILURE\n", RED, bold = true)
        put(indentError(r.error) + "\n", RED)
        rule()
        nl()
    }

    /** Renders the timestamp + prompt line + optional data-source hint. */
    private fun appendPromptLine(time: String, query: String, source: String) {
        put("[$time] ", DIM)
        put("❯ ", CYAN, bold = true)
        put(query.trim() + "\n", FG, bold = true)
        if (source.isNotBlank()) {
            put("         ↳ $source\n", DIM)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun rule() = put("─".repeat(68) + "\n", RULE)
    private fun nl()   = put("\n", FG)

    private fun indentError(error: Throwable): String = buildString {
        var cur: Throwable? = error
        var depth = 0
        while (cur != null && depth < 5) {
            val prefix = if (depth == 0) "  " else "  Caused by: "
            appendLine("$prefix${cur.javaClass.simpleName}: ${cur.message ?: "(no message)"}")
            cur = cur.cause
            depth++
        }
    }.trimEnd()

    private fun put(text: String, color: Color, bold: Boolean = false) {
        val attr = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, color)
            StyleConstants.setFontFamily(this, TERM_FONT.family)
            StyleConstants.setFontSize(this, TERM_FONT.size)
            if (bold) StyleConstants.setBold(this, true)
        }
        doc.insertString(doc.length, text, attr)
    }
}
