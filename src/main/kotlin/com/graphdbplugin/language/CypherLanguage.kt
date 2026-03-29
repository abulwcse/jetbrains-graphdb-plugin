package com.graphdbplugin.language

import com.intellij.lang.Language

/**
 * Singleton language definition for the Cypher query language.
 *
 * This object serves as the authoritative identifier for the Cypher language
 * throughout all IntelliJ Platform APIs. It is referenced by:
 * - [CypherFileType] to bind the language to file extensions
 * - [com.graphdbplugin.language.parser.CypherParserDefinition] as the parser's language
 * - [com.graphdbplugin.language.highlighting.CypherSyntaxHighlighterFactory] for token colouring
 * - All PSI element types via [CypherTokenTypes] and [com.graphdbplugin.language.psi.CypherElementType]
 *
 * The string identifier `"Cypher"` passed to the [Language] constructor must match the
 * `language` attribute used in `plugin.xml` extension-point registrations.
 */
object CypherLanguage : Language("Cypher") {

    /**
     * Returns the human-readable display name shown in IDE UI elements such as
     * the color-settings page and the language status bar widget.
     *
     * @return The string `"Cypher"`.
     */
    override fun getDisplayName() = "Cypher"
}
