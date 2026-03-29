package com.graphdbplugin.language.lexer

import com.graphdbplugin.language.CypherKeywords
import com.graphdbplugin.language.CypherTokenTypes
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Hand-written lexer for the Cypher query language.
 *
 * Extends [LexerBase] from the IntelliJ Platform, which provides a convenient skeleton
 * that handles buffer storage and enforces the [com.intellij.lexer.Lexer] contract.
 * By implementing the lexer by hand (rather than using JFlex), we avoid the JFlex
 * toolchain as a compile-time dependency for Phase 2, keeping the build simple.
 *
 * ### How it works
 * Each call to [advance] moves [myStart] forward past one complete token and stores
 * the matching [IElementType] in [myTokenType]. [getTokenType], [getTokenStart], and
 * [getTokenEnd] then return the cached values without re-scanning.
 *
 * The lexer scans the buffer character-by-character. For multi-character tokens (arrows,
 * strings, comments, numbers) it uses look-ahead via [peekChar] to avoid over-consuming.
 *
 * ### Token priority
 * When multiple rules could match at the current position, the order within [advance]
 * determines which rule wins:
 * 1. Whitespace
 * 2. Line comments (`//`)
 * 3. Block comments (slash-star ... star-slash)
 * 4. String literals (`'...'` or `"..."`)
 * 5. Parameter (`$identifier`)
 * 6. Numbers (integer or float)
 * 7. Structural / operator characters (longest-match where needed for `->`, `<-`, `<>`, etc.)
 * 8. Identifiers and keywords
 * 9. Backtick-quoted identifiers
 * 10. Bad characters (catch-all)
 *
 * ### Thread safety
 * Lexer instances are **not** thread-safe and should not be shared between threads.
 * The platform creates a new instance for each file being highlighted/parsed.
 *
 * @see CypherTokenTypes for the full set of token types this lexer may emit.
 * @see CypherKeywords for the keyword and function-name sets used during classification.
 */
class CypherLexer : LexerBase() {

    /** The character buffer being lexed. Set by [start]. */
    private var myBuffer: CharSequence = ""

    /** Inclusive start offset of the current token within [myBuffer]. */
    private var myStart: Int = 0

    /** Exclusive end offset of the current token within [myBuffer]. */
    private var myEnd: Int = 0

    /** Exclusive upper bound of the region being lexed (may be < buffer.length). */
    private var myBufferEnd: Int = 0

    /** The token type of the current token, or `null` at end-of-file. */
    private var myTokenType: IElementType? = null

    // -------------------------------------------------------------------------
    // LexerBase / Lexer implementation
    // -------------------------------------------------------------------------

    /**
     * Initialises the lexer for a new lexing run over [buffer].
     *
     * The platform calls this before the first [advance]; it may also be called
     * mid-stream to restart lexing from a different offset (e.g. during incremental
     * highlighting).
     *
     * @param buffer       The full character buffer (may extend beyond [endOffset]).
     * @param startOffset  The offset at which lexing should begin (inclusive).
     * @param endOffset    The offset at which lexing should stop (exclusive).
     * @param initialState The initial lexer state; always 0 for a stateless lexer.
     */
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        myBuffer = buffer
        myStart = startOffset
        myEnd = startOffset
        myBufferEnd = endOffset
        myTokenType = null
        advance()
    }

    /**
     * Advances the lexer past the current token and scans for the next one.
     *
     * After this call, [getTokenType] returns the type of the newly scanned token
     * (or `null` at EOF), and [getTokenStart] / [getTokenEnd] delimit it.
     *
     * The method moves [myStart] to the position right after the previous token
     * (i.e. to [myEnd]), then scans forward from that position to determine the
     * next token.
     */
    override fun advance() {
        myStart = myEnd
        if (myStart >= myBufferEnd) {
            myTokenType = null
            return
        }

        val c = myBuffer[myStart]

        when {
            // ------------------------------------------------------------------
            // Whitespace
            // ------------------------------------------------------------------
            c.isWhitespace() -> {
                var pos = myStart + 1
                while (pos < myBufferEnd && myBuffer[pos].isWhitespace()) pos++
                myEnd = pos
                myTokenType = CypherTokenTypes.WHITESPACE
            }

            // ------------------------------------------------------------------
            // Comments  (must come before '/' operator check)
            // ------------------------------------------------------------------
            c == '/' && peekChar(myStart + 1) == '/' -> {
                myEnd = readLineComment(myStart)
                myTokenType = CypherTokenTypes.LINE_COMMENT
            }
            c == '/' && peekChar(myStart + 1) == '*' -> {
                myEnd = readBlockComment(myStart)
                myTokenType = CypherTokenTypes.BLOCK_COMMENT
            }

            // ------------------------------------------------------------------
            // String literals
            // ------------------------------------------------------------------
            c == '\'' || c == '"' -> {
                myEnd = readString(myStart, c)
                myTokenType = CypherTokenTypes.STRING_LITERAL
            }

            // ------------------------------------------------------------------
            // Parameters: $name or $0
            // ------------------------------------------------------------------
            c == '$' -> {
                var pos = myStart + 1
                if (pos < myBufferEnd && (myBuffer[pos].isLetterOrDigit() || myBuffer[pos] == '_')) {
                    while (pos < myBufferEnd && (myBuffer[pos].isLetterOrDigit() || myBuffer[pos] == '_')) pos++
                }
                myEnd = pos
                myTokenType = CypherTokenTypes.PARAM
            }

            // ------------------------------------------------------------------
            // Numbers: integer and float
            // ------------------------------------------------------------------
            c.isDigit() -> {
                val (end, type) = readNumber(myStart)
                myEnd = end
                myTokenType = type
            }

            // ------------------------------------------------------------------
            // Backtick-quoted identifiers
            // ------------------------------------------------------------------
            c == '`' -> {
                var pos = myStart + 1
                while (pos < myBufferEnd && myBuffer[pos] != '`') pos++
                if (pos < myBufferEnd) pos++ // consume closing backtick
                myEnd = pos
                myTokenType = CypherTokenTypes.IDENTIFIER
            }

            // ------------------------------------------------------------------
            // Structural tokens — single character
            // ------------------------------------------------------------------
            c == '(' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.LPAREN }
            c == ')' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.RPAREN }
            c == '[' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.LBRACKET }
            c == ']' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.RBRACKET }
            c == '{' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.LBRACE }
            c == '}' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.RBRACE }
            c == ',' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.COMMA }
            c == ':' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.COLON }
            c == '.' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.DOT }
            c == ';' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.SEMICOLON }
            c == '|' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.PIPE }
            c == '*' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.ASTERISK }
            c == '%' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.PERCENT }
            c == '^' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.CARET }
            c == '+' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.PLUS }

            // ------------------------------------------------------------------
            // Slash (already handled line/block comments above, so this is division)
            // ------------------------------------------------------------------
            c == '/' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.SLASH }

            // ------------------------------------------------------------------
            // Arrow right: ->
            // ------------------------------------------------------------------
            c == '-' && peekChar(myStart + 1) == '>' -> {
                myEnd = myStart + 2
                myTokenType = CypherTokenTypes.ARROW_RIGHT
            }

            // ------------------------------------------------------------------
            // Plain dash / minus (after -> check above)
            // ------------------------------------------------------------------
            c == '-' -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.DASH }

            // ------------------------------------------------------------------
            // Less-than family: <-, <>, <=, <
            // ------------------------------------------------------------------
            c == '<' -> {
                when (peekChar(myStart + 1)) {
                    '-' -> { myEnd = myStart + 2; myTokenType = CypherTokenTypes.ARROW_LEFT }
                    '>' -> { myEnd = myStart + 2; myTokenType = CypherTokenTypes.NEQ }
                    '=' -> { myEnd = myStart + 2; myTokenType = CypherTokenTypes.LTE }
                    else -> { myEnd = myStart + 1; myTokenType = CypherTokenTypes.LT }
                }
            }

            // ------------------------------------------------------------------
            // Greater-than family: >=, >
            // ------------------------------------------------------------------
            c == '>' -> {
                if (peekChar(myStart + 1) == '=') {
                    myEnd = myStart + 2; myTokenType = CypherTokenTypes.GTE
                } else {
                    myEnd = myStart + 1; myTokenType = CypherTokenTypes.GT
                }
            }

            // ------------------------------------------------------------------
            // Equals family: =~, =
            // ------------------------------------------------------------------
            c == '=' -> {
                if (peekChar(myStart + 1) == '~') {
                    myEnd = myStart + 2; myTokenType = CypherTokenTypes.TILDE_EQ
                } else {
                    myEnd = myStart + 1; myTokenType = CypherTokenTypes.EQ
                }
            }

            // ------------------------------------------------------------------
            // Identifiers and keywords
            // ------------------------------------------------------------------
            c.isLetter() || c == '_' -> {
                val (end, type) = readIdentifier(myStart)
                myEnd = end
                myTokenType = type
            }

            // ------------------------------------------------------------------
            // Bad / unrecognised character — emit single-char token
            // ------------------------------------------------------------------
            else -> {
                myEnd = myStart + 1
                myTokenType = CypherTokenTypes.BAD_CHARACTER
            }
        }
    }

    /**
     * Returns the [IElementType] of the current token, or `null` at end-of-file.
     *
     * @return The current token type, or `null` if the lexer is past [myBufferEnd].
     */
    override fun getTokenType(): IElementType? = myTokenType

    /**
     * Returns the inclusive start offset of the current token in [myBuffer].
     *
     * @return Zero-based start offset.
     */
    override fun getTokenStart(): Int = myStart

    /**
     * Returns the exclusive end offset of the current token in [myBuffer].
     *
     * @return Zero-based end offset (one past the last character of the token).
     */
    override fun getTokenEnd(): Int = myEnd

    /**
     * Returns the current lexer state.
     *
     * This lexer is stateless (no multi-line state machine beyond block comments,
     * which are handled inline within [advance]), so the state is always `0`.
     *
     * @return Always `0`.
     */
    override fun getState(): Int = 0

    /**
     * Returns the full character buffer that was passed to [start].
     *
     * @return The buffer [CharSequence].
     */
    override fun getBufferSequence(): CharSequence = myBuffer

    /**
     * Returns the exclusive upper bound of the region being lexed.
     *
     * @return The [endOffset] value that was passed to [start].
     */
    override fun getBufferEnd(): Int = myBufferEnd

    // -------------------------------------------------------------------------
    // Private helper methods
    // -------------------------------------------------------------------------

    /**
     * Returns the character at [offset] in [myBuffer], or the null character `\0`
     * if [offset] is at or beyond [myBufferEnd].
     *
     * Used for single-character lookahead without bounds-check boilerplate at
     * every call site.
     *
     * @param offset Absolute offset into [myBuffer].
     * @return The character at [offset], or `\0` if out of bounds.
     */
    private fun peekChar(offset: Int): Char =
        if (offset < myBufferEnd) myBuffer[offset] else '\u0000'

    /**
     * Reads a single-quoted or double-quoted string literal starting at [start].
     *
     * Handles backslash-escaped characters (e.g. `\'`, `\"`, `\\`) so that an
     * escaped quote inside the string does not prematurely terminate the token.
     * If the closing quote is never found (e.g. unterminated string at EOF), the
     * method returns [myBufferEnd] so the remainder of the buffer is consumed as
     * one string token.
     *
     * @param start The offset of the opening quote character.
     * @param quote The opening quote character (`'` or `"`).
     * @return The exclusive end offset of the string token (i.e. one past the
     *         closing quote, or [myBufferEnd] if unterminated).
     */
    private fun readString(start: Int, quote: Char): Int {
        var pos = start + 1 // skip opening quote
        while (pos < myBufferEnd) {
            val ch = myBuffer[pos]
            when {
                ch == '\\' -> pos += 2 // skip escaped character
                ch == quote -> return pos + 1 // found closing quote
                else -> pos++
            }
        }
        return myBufferEnd // unterminated string — consume to end of buffer
    }

    /**
     * Reads an identifier or keyword starting at [start] and classifies it.
     *
     * An identifier begins with a Unicode letter or underscore, followed by any
     * combination of Unicode letters, digits, and underscores.
     *
     * After reading the raw text, the method checks (case-insensitively) whether
     * it matches a keyword or built-in function name:
     * - `"TRUE"` → [CypherTokenTypes.TRUE_LITERAL]
     * - `"FALSE"` → [CypherTokenTypes.FALSE_LITERAL]
     * - `"NULL"` → [CypherTokenTypes.NULL_LITERAL]
     * - Any other [CypherKeywords.KEYWORDS] entry → [CypherTokenTypes.KEYWORD]
     * - Any [CypherKeywords.FUNCTIONS] entry (compared lowercase) → [CypherTokenTypes.FUNCTION_NAME]
     * - Otherwise → [CypherTokenTypes.IDENTIFIER]
     *
     * @param start The offset of the first character of the identifier.
     * @return A [Pair] of (exclusive end offset, token type).
     */
    private fun readIdentifier(start: Int): Pair<Int, IElementType> {
        var pos = start + 1
        while (pos < myBufferEnd && (myBuffer[pos].isLetterOrDigit() || myBuffer[pos] == '_')) pos++
        val text = myBuffer.subSequence(start, pos).toString()
        val upper = text.uppercase()
        val lower = text.lowercase()
        val tokenType = when {
            upper == "TRUE" -> CypherTokenTypes.TRUE_LITERAL
            upper == "FALSE" -> CypherTokenTypes.FALSE_LITERAL
            upper == "NULL" -> CypherTokenTypes.NULL_LITERAL
            CypherKeywords.KEYWORDS.contains(upper) -> CypherTokenTypes.KEYWORD
            CypherKeywords.FUNCTIONS.contains(lower) -> CypherTokenTypes.FUNCTION_NAME
            else -> CypherTokenTypes.IDENTIFIER
        }
        return Pair(pos, tokenType)
    }

    /**
     * Reads a numeric literal (integer or float) starting at [start].
     *
     * A number begins with one or more decimal digits. It becomes a float if it
     * contains a decimal point (`.`) followed by more digits, or an exponent
     * suffix (`e` or `E`, optionally followed by `+` or `-` and more digits).
     *
     * Note: the method does not handle hex (`0x...`) or octal literals, which are
     * not part of the standard Cypher specification.
     *
     * @param start The offset of the first digit.
     * @return A [Pair] of (exclusive end offset, [CypherTokenTypes.INTEGER_LITERAL]
     *         or [CypherTokenTypes.FLOAT_LITERAL]).
     */
    private fun readNumber(start: Int): Pair<Int, IElementType> {
        var pos = start
        while (pos < myBufferEnd && myBuffer[pos].isDigit()) pos++
        var isFloat = false

        // Optional decimal part: '.' followed by digits
        if (pos < myBufferEnd && myBuffer[pos] == '.' &&
            pos + 1 < myBufferEnd && myBuffer[pos + 1].isDigit()
        ) {
            isFloat = true
            pos++ // consume '.'
            while (pos < myBufferEnd && myBuffer[pos].isDigit()) pos++
        }

        // Optional exponent part: 'e' or 'E', optional sign, then digits
        if (pos < myBufferEnd && (myBuffer[pos] == 'e' || myBuffer[pos] == 'E')) {
            val expStart = pos
            pos++ // consume 'e'/'E'
            if (pos < myBufferEnd && (myBuffer[pos] == '+' || myBuffer[pos] == '-')) pos++
            if (pos < myBufferEnd && myBuffer[pos].isDigit()) {
                isFloat = true
                while (pos < myBufferEnd && myBuffer[pos].isDigit()) pos++
            } else {
                // Malformed exponent — roll back to before 'e'/'E'
                pos = expStart
            }
        }

        return Pair(pos, if (isFloat) CypherTokenTypes.FLOAT_LITERAL else CypherTokenTypes.INTEGER_LITERAL)
    }

    /**
     * Reads a line comment starting at [start].
     *
     * A line comment begins with `//` and extends to (but does not include) the
     * terminating newline. The newline itself will be consumed as a [CypherTokenTypes.WHITESPACE]
     * token on the next call to [advance].
     *
     * @param start The offset of the first `/` character.
     * @return The exclusive end offset of the comment (the position of the newline,
     *         or [myBufferEnd] if the file ends without a newline).
     */
    private fun readLineComment(start: Int): Int {
        var pos = start + 2 // skip '//'
        while (pos < myBufferEnd && myBuffer[pos] != '\n' && myBuffer[pos] != '\r') pos++
        return pos
    }

    /**
     * Reads a block comment starting at [start].
     *
     * A block comment begins with slash-star and ends with star-slash. Nested block
     * comments are **not** supported (Cypher does not support them). If the closing
     * delimiter is never found, the entire remainder of the buffer is consumed as
     * one comment token.
     *
     * @param start The offset of the opening slash character.
     * @return The exclusive end offset of the comment (one past the closing slash),
     *         or [myBufferEnd] if unterminated.
     */
    private fun readBlockComment(start: Int): Int {
        var pos = start + 2 // skip '/*'
        while (pos < myBufferEnd - 1) {
            if (myBuffer[pos] == '*' && myBuffer[pos + 1] == '/') {
                return pos + 2 // consume closing '*/'
            }
            pos++
        }
        return myBufferEnd // unterminated block comment
    }
}
