package com.graphdbplugin.language.highlighting

import com.graphdbplugin.GraphDbPluginIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * Color settings page for the Cypher language.
 *
 * Registered via the `colorSettingsPage` extension point in `plugin.xml`, this
 * class adds a **Cypher** node under Settings → Editor → Color Scheme. The user
 * can customise the foreground colour, background colour, bold, italic, and
 * underline attributes for each Cypher token category independently of other
 * languages.
 *
 * ### Demo text
 * The [getDemoText] method returns a representative multi-line Cypher snippet that
 * covers all highlighted token types. When the user selects a colour-scheme entry,
 * the platform highlights the corresponding tokens in the preview panel.
 *
 * ### Attribute descriptors
 * Each [AttributesDescriptor] pairs a human-readable group/name string with a
 * [TextAttributesKey] from [CypherHighlighterColors]. Group prefixes (separated by
 * `//`) create a tree hierarchy in the settings UI.
 *
 * ### No custom tags
 * [getAdditionalHighlightingTagToDescriptorMap] returns `null` (no XML-tag-based
 * custom highlighting in the demo text) — all token highlighting is driven by the
 * standard syntax highlighter.
 */
class CypherColorSettingsPage : ColorSettingsPage {

    companion object {

        /**
         * Static array of all attribute descriptors, built once and reused.
         *
         * The string format for each descriptor name is `"Group//Item"` which
         * creates a two-level tree in the settings UI. Simple names produce
         * flat (top-level) entries.
         */
        private val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("Keywords//Reserved words",          CypherHighlighterColors.KEYWORD),
            AttributesDescriptor("Keywords//Functions",               CypherHighlighterColors.FUNCTION),
            AttributesDescriptor("Literals//Strings",                 CypherHighlighterColors.STRING),
            AttributesDescriptor("Literals//Numbers",                 CypherHighlighterColors.NUMBER),
            AttributesDescriptor("Comments//Line comment (//)",       CypherHighlighterColors.COMMENT),
            AttributesDescriptor("Comments//Block comment (/* */)",   CypherHighlighterColors.BLOCK_COMMENT),
            AttributesDescriptor("Operators//Operator symbols",       CypherHighlighterColors.OPERATOR),
            AttributesDescriptor("Braces and Operators//Parentheses", CypherHighlighterColors.PARENTHESES),
            AttributesDescriptor("Braces and Operators//Brackets",    CypherHighlighterColors.BRACKETS),
            AttributesDescriptor("Braces and Operators//Braces",      CypherHighlighterColors.BRACES),
            AttributesDescriptor("Braces and Operators//Dot",         CypherHighlighterColors.DOT),
            AttributesDescriptor("Braces and Operators//Comma",       CypherHighlighterColors.COMMA),
            AttributesDescriptor("Braces and Operators//Colon",       CypherHighlighterColors.COLON),
            AttributesDescriptor("Identifiers//Identifier",           CypherHighlighterColors.IDENTIFIER),
            AttributesDescriptor("Identifiers//Parameter",            CypherHighlighterColors.PARAM),
            AttributesDescriptor("Bad character",                     CypherHighlighterColors.BAD_CHAR)
        )
    }

    /**
     * Returns the icon displayed next to the "Cypher" entry in the color-scheme
     * language list.
     *
     * @return The plugin's main graph-database icon.
     */
    override fun getIcon(): Icon = GraphDbPluginIcons.GRAPH_DB

    /**
     * Returns the [CypherSyntaxHighlighter] used to colour the demo text in the
     * settings preview panel.
     *
     * @return A new [CypherSyntaxHighlighter] instance.
     */
    override fun getHighlighter(): SyntaxHighlighter = CypherSyntaxHighlighter()

    /**
     * Returns a representative multi-line Cypher snippet that exercises all
     * highlighted token categories.
     *
     * The snippet intentionally includes:
     * - Keywords: `MATCH`, `WHERE`, `RETURN`, `WITH`, `CREATE`, `MERGE`, `SET`
     * - Boolean/null literals: `true`, `null`
     * - Built-in functions: `count`, `toLower`, `shortestPath`
     * - String, integer, and float literals
     * - Parameters: `$minAge`, `$name`
     * - Line and block comments
     * - Operators: `=`, `>`, `<>`, `=~`, `->`, `-`
     * - Parentheses, brackets, braces, dots, commas, colons
     * - Plain identifiers and property access
     *
     * @return The demo Cypher query string.
     */
    override fun getDemoText(): String = """
        // Find people older than a given age
        /* Block comment spanning
           multiple lines */
        MATCH (p:Person)-[:KNOWS]->(friend:Person)
        WHERE p.age > ${'$'}minAge
          AND friend.active = true
          AND p.name =~ '.*Alice.*'
        WITH p, count(friend) AS friendCount
        WHERE friendCount <> 0
        RETURN p.name AS name,
               toLower(p.email) AS email,
               friendCount
        ORDER BY friendCount DESC
        LIMIT 10;

        // Create a new person
        CREATE (n:Person {
            name: ${'$'}name,
            age: 30,
            score: 9.5
        })

        // Shortest path example
        MATCH path = shortestPath((a:Person)-[*]-(b:Person))
        WHERE a.name = 'Alice' AND b.name <> null
        RETURN path
    """.trimIndent()

    /**
     * Returns additional XML-tag-to-descriptor mappings for custom highlighting
     * in the demo text.
     *
     * Not used for Cypher — all token highlighting is provided by the lexer via
     * [getHighlighter]. Returning `null` is valid per the [ColorSettingsPage] contract.
     *
     * @return `null`.
     */
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    /**
     * Returns the array of [AttributesDescriptor]s that populate the attribute tree
     * in the settings panel.
     *
     * Each descriptor maps a human-readable name (with optional `//`-separated group
     * prefix) to a [TextAttributesKey] from [CypherHighlighterColors].
     *
     * @return The static [DESCRIPTORS] array.
     */
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    /**
     * Returns colour descriptors for background/foreground pair customisation.
     *
     * Cypher highlighting does not use named colour pairs, so this returns an empty
     * array.
     *
     * @return An empty [ColorDescriptor] array.
     */
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    /**
     * Returns the display name for this colour-settings entry as shown in the
     * language list under Settings → Editor → Color Scheme.
     *
     * @return The string `"Cypher"`.
     */
    override fun getDisplayName(): String = "Cypher"
}
