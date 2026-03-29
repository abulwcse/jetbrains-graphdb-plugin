package com.graphdbplugin.language.highlighting

import com.graphdbplugin.language.CypherTokenTypes
import com.graphdbplugin.language.lexer.CypherLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * Syntax highlighter for the Cypher query language.
 *
 * Extends [SyntaxHighlighterBase], which handles the boilerplate of the
 * [com.intellij.openapi.fileTypes.SyntaxHighlighter] interface, leaving only
 * [getHighlightingLexer] and [getTokenHighlights] to implement.
 *
 * ### How highlighting works
 * 1. The IDE calls [getHighlightingLexer] to obtain a lexer.
 * 2. The lexer tokenises the file content.
 * 3. For each token, the IDE calls [getTokenHighlights] with the token's
 *    [IElementType] to obtain the array of [TextAttributesKey]s to apply.
 * 4. Multiple keys can be combined (the first key in the array takes priority
 *    for most attributes, but the platform merges them).
 *
 * ### Token-to-colour mapping
 * | Token type(s) | Attribute key |
 * |---|---|
 * | KEYWORD, TRUE_LITERAL, FALSE_LITERAL, NULL_LITERAL | KEYWORD |
 * | FUNCTION_NAME | FUNCTION |
 * | STRING_LITERAL | STRING |
 * | INTEGER_LITERAL, FLOAT_LITERAL | NUMBER |
 * | LINE_COMMENT | COMMENT |
 * | BLOCK_COMMENT | BLOCK_COMMENT |
 * | EQ, NEQ, LT, GT, LTE, GTE, PLUS, MINUS, SLASH, PERCENT, CARET, TILDE_EQ, ARROW_RIGHT, ARROW_LEFT, DASH, ASTERISK, PIPE | OPERATOR |
 * | LPAREN, RPAREN | PARENTHESES |
 * | LBRACKET, RBRACKET | BRACKETS |
 * | LBRACE, RBRACE | BRACES |
 * | PARAM | PARAM |
 * | IDENTIFIER | IDENTIFIER |
 * | DOT | DOT |
 * | COMMA | COMMA |
 * | COLON | COLON |
 * | BAD_CHARACTER | BAD_CHAR |
 * | all other | empty array |
 *
 * @see CypherHighlighterColors for the [TextAttributesKey] constants.
 * @see CypherTokenTypes for the full token-type registry.
 */
class CypherSyntaxHighlighter : SyntaxHighlighterBase() {

    /**
     * Returns a new [CypherLexer] instance that the platform will use to tokenise
     * the file content for highlighting purposes.
     *
     * A fresh lexer is returned on each call; the platform manages its lifecycle.
     *
     * @return A new [CypherLexer].
     */
    override fun getHighlightingLexer(): Lexer = CypherLexer()

    /**
     * Returns the array of [TextAttributesKey]s that should be applied to a token
     * of the given [tokenType].
     *
     * An empty array means "no highlighting" (the token inherits the editor's default
     * text colour). A single-element array is the common case. Multiple elements are
     * merged by the platform.
     *
     * @param tokenType The [IElementType] of the token being highlighted.
     * @return An array of [TextAttributesKey]s, possibly empty.
     */
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            // Keywords and boolean/null literals all share the keyword style
            CypherTokenTypes.KEYWORD,
            CypherTokenTypes.TRUE_LITERAL,
            CypherTokenTypes.FALSE_LITERAL,
            CypherTokenTypes.NULL_LITERAL ->
                pack(CypherHighlighterColors.KEYWORD)

            // Built-in function names
            CypherTokenTypes.FUNCTION_NAME ->
                pack(CypherHighlighterColors.FUNCTION)

            // String literals
            CypherTokenTypes.STRING_LITERAL ->
                pack(CypherHighlighterColors.STRING)

            // Numeric literals
            CypherTokenTypes.INTEGER_LITERAL,
            CypherTokenTypes.FLOAT_LITERAL ->
                pack(CypherHighlighterColors.NUMBER)

            // Single-line comments
            CypherTokenTypes.LINE_COMMENT ->
                pack(CypherHighlighterColors.COMMENT)

            // Block comments
            CypherTokenTypes.BLOCK_COMMENT ->
                pack(CypherHighlighterColors.BLOCK_COMMENT)

            // Operators: comparison, arithmetic, arrows, regex
            CypherTokenTypes.EQ,
            CypherTokenTypes.NEQ,
            CypherTokenTypes.LT,
            CypherTokenTypes.GT,
            CypherTokenTypes.LTE,
            CypherTokenTypes.GTE,
            CypherTokenTypes.PLUS,
            CypherTokenTypes.MINUS,
            CypherTokenTypes.SLASH,
            CypherTokenTypes.PERCENT,
            CypherTokenTypes.CARET,
            CypherTokenTypes.TILDE_EQ,
            CypherTokenTypes.ARROW_RIGHT,
            CypherTokenTypes.ARROW_LEFT,
            CypherTokenTypes.DASH,
            CypherTokenTypes.ASTERISK,
            CypherTokenTypes.PIPE ->
                pack(CypherHighlighterColors.OPERATOR)

            // Parentheses
            CypherTokenTypes.LPAREN,
            CypherTokenTypes.RPAREN ->
                pack(CypherHighlighterColors.PARENTHESES)

            // Brackets
            CypherTokenTypes.LBRACKET,
            CypherTokenTypes.RBRACKET ->
                pack(CypherHighlighterColors.BRACKETS)

            // Braces
            CypherTokenTypes.LBRACE,
            CypherTokenTypes.RBRACE ->
                pack(CypherHighlighterColors.BRACES)

            // Parameters ($name)
            CypherTokenTypes.PARAM ->
                pack(CypherHighlighterColors.PARAM)

            // Plain identifiers
            CypherTokenTypes.IDENTIFIER ->
                pack(CypherHighlighterColors.IDENTIFIER)

            // Property-access dot
            CypherTokenTypes.DOT ->
                pack(CypherHighlighterColors.DOT)

            // Comma
            CypherTokenTypes.COMMA ->
                pack(CypherHighlighterColors.COMMA)

            // Colon
            CypherTokenTypes.COLON ->
                pack(CypherHighlighterColors.COLON)

            // Unrecognised characters
            CypherTokenTypes.BAD_CHARACTER ->
                pack(CypherHighlighterColors.BAD_CHAR)

            // Whitespace and any other token type — no highlighting
            else -> TextAttributesKey.EMPTY_ARRAY
        }
    }
}
