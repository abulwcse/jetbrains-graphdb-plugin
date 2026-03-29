package com.graphdbplugin.language.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * Text attribute keys for Cypher syntax highlighting.
 *
 * Each constant is a [TextAttributesKey] linked to a [DefaultLanguageHighlighterColors]
 * fallback so the editor displays sensible colours out-of-the-box, even before the
 * user customises the Cypher colour scheme in Settings → Editor → Color Scheme → Cypher.
 *
 * ### How attributes work
 * A [TextAttributesKey] is a stable string identifier for a visual style (foreground
 * colour, bold, italic, etc.). The actual colours are defined per colour-scheme and
 * per theme; the `createTextAttributesKey(name, fallback)` overload means the platform
 * inherits the fallback's colours when no scheme-specific override exists.
 *
 * ### Adding a new attribute
 * 1. Add a constant here with a unique `"CYPHER_..."` name and a suitable fallback.
 * 2. Add a corresponding [com.intellij.openapi.options.colors.AttributesDescriptor]
 *    entry in [CypherColorSettingsPage.getAttributeDescriptors].
 * 3. Map the relevant token type(s) to the new key in [CypherSyntaxHighlighter.getTokenHighlights].
 */
object CypherHighlighterColors {

    /**
     * Style applied to Cypher reserved keywords such as `MATCH`, `RETURN`, `WHERE`,
     * and to the boolean/null literals `true`, `false`, `null`.
     * Defaults to the IDE's standard keyword style (typically bold in most themes).
     */
    val KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    /**
     * Style applied to built-in function names such as `count`, `collect`, `toLower`.
     * Defaults to the IDE's function-call style (typically a distinct colour from keywords).
     */
    val FUNCTION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_FUNCTION",
        DefaultLanguageHighlighterColors.FUNCTION_CALL
    )

    /**
     * Style applied to string literals enclosed in single or double quotes.
     * Defaults to the IDE's standard string style (typically green or orange).
     */
    val STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_STRING",
        DefaultLanguageHighlighterColors.STRING
    )

    /**
     * Style applied to numeric literals — both integers (e.g. `42`) and floats
     * (e.g. `3.14`).
     * Defaults to the IDE's standard number style.
     */
    val NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER
    )

    /**
     * Style applied to single-line comments starting with `//`.
     * Defaults to the IDE's line-comment style (typically grey and italic).
     */
    val COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT
    )

    /**
     * Style applied to block comments delimited by `/* ... */`.
     * Defaults to the IDE's block-comment style.
     */
    val BLOCK_COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT
    )

    /**
     * Style applied to operator symbols: `=`, `<>`, `<`, `>`, `<=`, `>=`, `+`,
     * `-`, `*`, `/`, `%`, `^`, `=~`, `->`, `<-`, `-`, `|`.
     * Defaults to the IDE's operation-sign style.
     */
    val OPERATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_OPERATOR",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )

    /**
     * Style applied to opening and closing round parentheses `(` `)`.
     * Defaults to the IDE's parentheses style.
     */
    val PARENTHESES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_PARENTHESES",
        DefaultLanguageHighlighterColors.PARENTHESES
    )

    /**
     * Style applied to opening and closing square brackets `[` `]`.
     * Defaults to the IDE's brackets style.
     */
    val BRACKETS: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_BRACKETS",
        DefaultLanguageHighlighterColors.BRACKETS
    )

    /**
     * Style applied to opening and closing curly braces `{` `}`.
     * Defaults to the IDE's braces style.
     */
    val BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_BRACES",
        DefaultLanguageHighlighterColors.BRACES
    )

    /**
     * Style applied to query parameters prefixed with `$`, e.g. `$userId`.
     * Defaults to the IDE's local-variable style, making parameters visually
     * distinct from plain identifiers and keywords.
     */
    val PARAM: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_PARAM",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )

    /**
     * Style applied to plain identifiers — node aliases (`n`), property keys
     * used directly, and backtick-quoted names.
     * Defaults to the IDE's identifier style.
     */
    val IDENTIFIER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER
    )

    /**
     * Style applied to the property-access dot `.`, e.g. in `n.name`.
     * Defaults to the IDE's dot style.
     */
    val DOT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_DOT",
        DefaultLanguageHighlighterColors.DOT
    )

    /**
     * Style applied to comma separators `,` in return lists, argument lists, etc.
     * Defaults to the IDE's comma style.
     */
    val COMMA: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_COMMA",
        DefaultLanguageHighlighterColors.COMMA
    )

    /**
     * Style applied to the colon `:` used in label declarations and map literals.
     * Defaults to the IDE's operation-sign style to distinguish it from braces.
     */
    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_COLON",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )

    /**
     * Style applied to query variable names bound in node/relationship patterns,
     * e.g. `n` in `(n:Person)` or `r` in `[r:KNOWS]`.
     * Defaults to the IDE's local-variable style (typically italic or a distinct colour).
     */
    val VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )

    /**
     * Style applied to characters that could not be matched by any lexer rule.
     * Shown as a red-squiggle error underline in the default colour scheme.
     * Defaults to the IDE's bad-character style.
     */
    val BAD_CHAR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CYPHER_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER
    )
}
