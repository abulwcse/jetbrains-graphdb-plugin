package com.graphdbplugin.results

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

/**
 * Parameters panel that dynamically mirrors every `$paramName` found in the
 * Cypher query.
 *
 * The panel contains no manual JSON editor. Instead, [syncWithQuery] is called
 * from a [com.intellij.openapi.editor.event.DocumentListener] in [CypherFileEditor]
 * every time the document changes. It scans the query text for `$identifier`
 * tokens and adds or removes labelled text-field rows accordingly, preserving the
 * user's existing input for params that did not change.
 *
 * ### Type inference
 * [getParams] attempts to parse each field value as Long, then Double, then
 * Boolean (case-insensitive). Any other non-blank text is kept as a String.
 * Fields left blank are excluded from the returned map (treated as "no value
 * provided").
 *
 * ### Cross-IDE compatibility
 * This approach does not rely on IntelliJ's built-in JSON language support, so
 * it works identically in IntelliJ IDEA, PhpStorm, GoLand, and any other
 * JetBrains IDE.
 */
class ParametersPanel : JPanel(BorderLayout()) {

    /** Ordered map: param name → input field. Insertion order = query order. */
    private val paramFields = LinkedHashMap<String, JBTextField>()

    /** Grid that holds the label+field rows. Rebuilt by [rebuildForm]. */
    private val formPanel = JPanel(GridBagLayout())

    private val emptyLabel = JBLabel(
        "<html><center><br/><i>No parameters detected.</i><br/>" +
        "<i>Use <b>\$paramName</b> in your query and fields will appear here.</i>" +
        "</center></html>"
    ).apply { border = JBUI.Borders.empty(16) }

    // Wrapper holds the form at NORTH (natural height) and empty label at CENTER.
    private val wrapper = JPanel(BorderLayout()).apply {
        add(formPanel, BorderLayout.NORTH)
        add(emptyLabel, BorderLayout.CENTER)
    }

    init {
        val hintLabel = JBLabel(
            "<html>" +
            "<b>Read-only form \u2014 fields are driven by your query.</b><br/>" +
            "<i>To add or remove a parameter, type <b>\$paramName</b> in (or delete it from) " +
            "the Cypher editor above. You can only fill in values here.</i>" +
            "</html>"
        ).apply {
            border = JBUI.Borders.empty(6, 8)
            toolTipText = "Parameter fields are auto-generated from \$params in your query. " +
                          "You cannot add fields manually — edit the query to change this list."
        }

        add(hintLabel, BorderLayout.NORTH)
        add(JBScrollPane(wrapper), BorderLayout.CENTER)
        rebuildForm()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Scans [queryText] for `$identifier` tokens and updates the form rows.
     *
     * - Params no longer present in the query have their rows removed.
     * - Params newly introduced get a fresh empty text field.
     * - Params already present keep their current field value.
     *
     * Must be called on the EDT.
     *
     * @param queryText The current content of the Cypher editor document.
     */
    fun syncWithQuery(queryText: String) {
        val found = PARAM_REGEX.findAll(queryText)
            .map { it.groupValues[1] }
            .toCollection(LinkedHashSet())   // preserve first-seen order, deduplicate

        // Drop params no longer in the query
        paramFields.keys.toList().forEach { name ->
            if (name !in found) paramFields.remove(name)
        }

        // Add params that are new
        for (name in found) {
            if (name !in paramFields) {
                paramFields[name] = JBTextField().apply {
                    preferredSize = Dimension(220, preferredSize.height)
                    toolTipText   = "Value for \$$name"
                }
            }
        }

        rebuildForm()
    }

    /**
     * Returns the current parameter values as a typed map.
     *
     * Only fields with non-blank content are included. Values are parsed in order:
     * Long → Double → Boolean → String.
     *
     * @return A [Map] of parameter name to typed value, ready to pass directly to
     *         [com.graphdbplugin.execution.QueryExecutor.execute].
     */
    fun getParams(): Map<String, Any> =
        paramFields.entries
            .filter { (_, field) -> field.text.isNotBlank() }
            .associate { (name, field) -> name to parseValue(field.text.trim()) }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun rebuildForm() {
        formPanel.removeAll()
        emptyLabel.isVisible = paramFields.isEmpty()

        if (paramFields.isNotEmpty()) {
            val gbc = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(3, 8)
                gridy  = 0
            }
            for ((name, field) in paramFields) {
                // Label: $paramName
                gbc.gridx    = 0
                gbc.weightx  = 0.0
                gbc.fill     = GridBagConstraints.NONE
                formPanel.add(JBLabel("\$$name"), gbc)

                // Text field
                gbc.gridx    = 1
                gbc.weightx  = 1.0
                gbc.fill     = GridBagConstraints.HORIZONTAL
                formPanel.add(field, gbc)

                gbc.gridy++
            }
        }

        formPanel.revalidate()
        formPanel.repaint()
        wrapper.revalidate()
        wrapper.repaint()
    }

    /** Parses [raw] to Long, Double, Boolean, or String — in that order. */
    private fun parseValue(raw: String): Any =
        raw.toLongOrNull()
            ?: raw.toDoubleOrNull()
            ?: when (raw.lowercase()) {
                "true"  -> true
                "false" -> false
                else    -> raw
            }

    companion object {
        /** Matches `$identifier` — the `$` is a literal, `\w+` is the param name. */
        private val PARAM_REGEX = Regex("""\$(\w+)""")
    }
}
