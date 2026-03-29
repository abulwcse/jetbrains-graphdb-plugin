package com.graphdbplugin.language

import com.graphdbplugin.language.lexer.CypherLexer
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CypherLexer].
 *
 * Each test verifies that a specific input string is tokenised correctly by
 * checking the sequence of `(tokenType, tokenText)` pairs produced by the lexer.
 *
 * ### Helper function
 * [tokenize] drives the lexer over a complete input string and collects all
 * non-null token types and their corresponding text slices into a list of pairs.
 * EOF is represented by [getTokenType] returning `null`, which terminates the loop.
 *
 * ### Arrow-token note
 * The lexer emits `->` as a single [CypherTokenTypes.ARROW_RIGHT] token.
 * A bare `-` outside of `->` is emitted as [CypherTokenTypes.DASH]. This avoids
 * the need for the parser to reconstruct arrow tokens from two separate tokens.
 *
 * ### Keyword case insensitivity
 * Cypher is case-insensitive for keywords, so `"match"`, `"MATCH"`, and `"Match"`
 * all produce a [CypherTokenTypes.KEYWORD] token.
 */
class CypherLexerTest {

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Runs [CypherLexer] over [input] and returns all tokens as a list of
     * `(IElementType, tokenText)` pairs.
     *
     * The lexer is started at offset 0 and runs to the end of [input]. The loop
     * terminates when [CypherLexer.getTokenType] returns `null` (EOF).
     *
     * @param input The Cypher source text to tokenise.
     * @return An ordered list of `(tokenType, tokenText)` pairs for every token
     *         in the input, not including the EOF sentinel.
     */
    private fun tokenize(input: String): List<Pair<IElementType, String>> {
        val lexer = CypherLexer()
        lexer.start(input, 0, input.length, 0)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType!!
            val text = input.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(type to text)
            lexer.advance()
        }
        return tokens
    }

    // =========================================================================
    // Keyword tests
    // =========================================================================

    /**
     * A single uppercase keyword `MATCH` should produce exactly one [CypherTokenTypes.KEYWORD] token.
     */
    @Test
    fun testTokenizeKeyword() {
        val tokens = tokenize("MATCH")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.KEYWORD, tokens[0].first)
        assertEquals("MATCH", tokens[0].second)
    }

    /**
     * Two keywords separated by a space: `MATCH RETURN`.
     * Expected sequence: KEYWORD("MATCH"), WHITESPACE(" "), KEYWORD("RETURN").
     */
    @Test
    fun testTokenizeMultipleKeywords() {
        val tokens = tokenize("MATCH RETURN")
        assertEquals(3, tokens.size)
        assertEquals(CypherTokenTypes.KEYWORD to "MATCH",   tokens[0])
        assertEquals(CypherTokenTypes.WHITESPACE to " ",    tokens[1])
        assertEquals(CypherTokenTypes.KEYWORD to "RETURN",  tokens[2])
    }

    /**
     * Cypher keywords are case-insensitive; `match` (lowercase) must still produce
     * a [CypherTokenTypes.KEYWORD] token.
     */
    @Test
    fun testKeywordCaseInsensitive() {
        val tokens = tokenize("match")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.KEYWORD, tokens[0].first)
        assertEquals("match", tokens[0].second)
    }

    // =========================================================================
    // Identifier tests
    // =========================================================================

    /**
     * A plain identifier `myNode` should produce a single [CypherTokenTypes.IDENTIFIER] token.
     */
    @Test
    fun testTokenizeIdentifier() {
        val tokens = tokenize("myNode")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.IDENTIFIER to "myNode", tokens[0])
    }

    // =========================================================================
    // Literal tests
    // =========================================================================

    /**
     * A single-quoted string `'hello'` should produce a single [CypherTokenTypes.STRING_LITERAL] token.
     */
    @Test
    fun testTokenizeString_singleQuote() {
        val tokens = tokenize("'hello'")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.STRING_LITERAL to "'hello'", tokens[0])
    }

    /**
     * A double-quoted string `"world"` should produce a single [CypherTokenTypes.STRING_LITERAL] token.
     */
    @Test
    fun testTokenizeString_doubleQuote() {
        val tokens = tokenize("\"world\"")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.STRING_LITERAL to "\"world\"", tokens[0])
    }

    /**
     * An integer literal `42` should produce a single [CypherTokenTypes.INTEGER_LITERAL] token.
     */
    @Test
    fun testTokenizeInteger() {
        val tokens = tokenize("42")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.INTEGER_LITERAL to "42", tokens[0])
    }

    /**
     * A floating-point literal `3.14` should produce a single [CypherTokenTypes.FLOAT_LITERAL] token.
     */
    @Test
    fun testTokenizeFloat() {
        val tokens = tokenize("3.14")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.FLOAT_LITERAL to "3.14", tokens[0])
    }

    /**
     * The boolean literal `true` (lowercase) should produce a [CypherTokenTypes.TRUE_LITERAL] token.
     */
    @Test
    fun testTokenizeBooleans() {
        val tokens = tokenize("true false")
        assertEquals(3, tokens.size)
        assertEquals(CypherTokenTypes.TRUE_LITERAL  to "true",  tokens[0])
        assertEquals(CypherTokenTypes.WHITESPACE    to " ",     tokens[1])
        assertEquals(CypherTokenTypes.FALSE_LITERAL to "false", tokens[2])
    }

    /**
     * The null literal `null` (lowercase) should produce a [CypherTokenTypes.NULL_LITERAL] token.
     */
    @Test
    fun testTokenizeNull() {
        val tokens = tokenize("null")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.NULL_LITERAL to "null", tokens[0])
    }

    // =========================================================================
    // Parameter tests
    // =========================================================================

    /**
     * A query parameter `$userId` should produce a single [CypherTokenTypes.PARAM] token
     * that includes the leading `$`.
     */
    @Test
    fun testTokenizeParam() {
        val tokens = tokenize("\$userId")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.PARAM to "\$userId", tokens[0])
    }

    // =========================================================================
    // Comment tests
    // =========================================================================

    /**
     * A line comment `// comment` followed by a newline should produce one
     * [CypherTokenTypes.LINE_COMMENT] token (the comment text up to but not including
     * the newline), then a [CypherTokenTypes.WHITESPACE] token for the newline.
     */
    @Test
    fun testTokenizeLineComment() {
        val tokens = tokenize("// comment\n")
        // Comment token does NOT include the trailing newline
        assertEquals(CypherTokenTypes.LINE_COMMENT, tokens[0].first)
        assertEquals("// comment", tokens[0].second)
        // Newline emitted as whitespace
        assertEquals(CypherTokenTypes.WHITESPACE, tokens[1].first)
        assertEquals("\n", tokens[1].second)
    }

    /**
     * A block comment `/* block */` should produce a single [CypherTokenTypes.BLOCK_COMMENT]
     * token spanning the full comment including delimiters.
     */
    @Test
    fun testTokenizeBlockComment() {
        val tokens = tokenize("/* block */")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.BLOCK_COMMENT to "/* block */", tokens[0])
    }

    // =========================================================================
    // Operator and structural tests
    // =========================================================================

    /**
     * The two-character arrow `->` should be emitted as a single
     * [CypherTokenTypes.ARROW_RIGHT] token, not as two separate tokens.
     */
    @Test
    fun testTokenizeArrowRight() {
        val tokens = tokenize("->")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.ARROW_RIGHT to "->", tokens[0])
    }

    /**
     * Parentheses `()` should each produce their own structural token.
     * Expected: LPAREN("("), RPAREN(")").
     */
    @Test
    fun testTokenizeParens() {
        val tokens = tokenize("()")
        assertEquals(2, tokens.size)
        assertEquals(CypherTokenTypes.LPAREN to "(", tokens[0])
        assertEquals(CypherTokenTypes.RPAREN to ")", tokens[1])
    }

    // =========================================================================
    // Function name tests
    // =========================================================================

    /**
     * The built-in function name `count` should be emitted as [CypherTokenTypes.FUNCTION_NAME]
     * followed by the parenthesised argument.
     * Expected: FUNCTION_NAME("count"), LPAREN("("), IDENTIFIER("n"), RPAREN(")").
     */
    @Test
    fun testTokenizeFunction() {
        val tokens = tokenize("count(n)")
        assertEquals(4, tokens.size)
        assertEquals(CypherTokenTypes.FUNCTION_NAME to "count", tokens[0])
        assertEquals(CypherTokenTypes.LPAREN        to "(",     tokens[1])
        assertEquals(CypherTokenTypes.IDENTIFIER    to "n",     tokens[2])
        assertEquals(CypherTokenTypes.RPAREN        to ")",     tokens[3])
    }

    // =========================================================================
    // Full-query integration test
    // =========================================================================

    /**
     * Tokenises the query `MATCH (n:Person) RETURN n.name` and verifies the
     * complete token sequence in order.
     *
     * Expected sequence:
     * 1. KEYWORD("MATCH")
     * 2. WHITESPACE(" ")
     * 3. LPAREN("(")
     * 4. IDENTIFIER("n")
     * 5. COLON(":")
     * 6. IDENTIFIER("Person")
     * 7. RPAREN(")")
     * 8. WHITESPACE(" ")
     * 9. KEYWORD("RETURN")
     * 10. WHITESPACE(" ")
     * 11. IDENTIFIER("n")
     * 12. DOT(".")
     * 13. IDENTIFIER("name")
     */
    @Test
    fun testTokenizeFullQuery() {
        val tokens = tokenize("MATCH (n:Person) RETURN n.name")
        val expected = listOf(
            CypherTokenTypes.KEYWORD    to "MATCH",
            CypherTokenTypes.WHITESPACE to " ",
            CypherTokenTypes.LPAREN     to "(",
            CypherTokenTypes.IDENTIFIER to "n",
            CypherTokenTypes.COLON      to ":",
            CypherTokenTypes.IDENTIFIER to "Person",
            CypherTokenTypes.RPAREN     to ")",
            CypherTokenTypes.WHITESPACE to " ",
            CypherTokenTypes.KEYWORD    to "RETURN",
            CypherTokenTypes.WHITESPACE to " ",
            CypherTokenTypes.IDENTIFIER to "n",
            CypherTokenTypes.DOT        to ".",
            CypherTokenTypes.IDENTIFIER to "name"
        )
        assertEquals(expected.size, tokens.size, "Token count mismatch")
        expected.forEachIndexed { index, (expectedType, expectedText) ->
            assertEquals(expectedType, tokens[index].first,
                "Token[$index] type mismatch: expected $expectedType for '$expectedText'")
            assertEquals(expectedText, tokens[index].second,
                "Token[$index] text mismatch")
        }
    }

    // =========================================================================
    // EOF and edge-case tests
    // =========================================================================

    /**
     * An empty input string should produce zero tokens.
     * The lexer must handle EOF gracefully — [CypherLexer.getTokenType] must return
     * `null` immediately after [CypherLexer.start] with an empty buffer.
     */
    @Test
    fun testEmptyInput() {
        val tokens = tokenize("")
        assertTrue(tokens.isEmpty(), "Expected no tokens for empty input")
    }

    /**
     * Whitespace-only input should produce a single [CypherTokenTypes.WHITESPACE] token.
     */
    @Test
    fun testWhitespaceOnly() {
        val tokens = tokenize("   ")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.WHITESPACE to "   ", tokens[0])
    }

    /**
     * An unrecognised character `@` should produce a [CypherTokenTypes.BAD_CHARACTER] token.
     */
    @Test
    fun testBadCharacter() {
        val tokens = tokenize("@")
        assertEquals(1, tokens.size)
        assertEquals(CypherTokenTypes.BAD_CHARACTER to "@", tokens[0])
    }
}
