package com.graphdbplugin.language

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Centralised registry of all lexer token element types for the Cypher language.
 *
 * Each constant is an [IElementType] whose debug name matches the constant name for
 * easy identification in PSI viewer, parser error messages, and unit-test output.
 * All types except [WHITESPACE] and [BAD_CHARACTER] are language-scoped to
 * [CypherLanguage] so the platform can distinguish Cypher tokens from those of
 * other languages in mixed-language files.
 *
 * ### Categories
 * - **Structural** — parentheses, brackets, braces, punctuation
 * - **Arrows** — relationship arrow tokens used in pattern matching
 * - **Operators** — comparison, arithmetic, and regex-match operators
 * - **Literals** — string, integer, float, boolean, and null constants
 * - **Identifiers and Parameters** — plain identifiers and `$`-prefixed params
 * - **Keywords** — Cypher reserved words (case-insensitive during lexing)
 * - **Function Names** — built-in function names highlighted differently from identifiers
 * - **Comments** — line comments (`//`) and block comments (`/* ... */`)
 * - **Whitespace** — delegates to the platform's shared [TokenType.WHITE_SPACE]
 * - **Bad Character** — unrecognised input, delegates to [TokenType.BAD_CHARACTER]
 */
object CypherTokenTypes {

    // -------------------------------------------------------------------------
    // Structural tokens
    // -------------------------------------------------------------------------

    /** Opening round parenthesis `(`. Used in node patterns and expressions. */
    val LPAREN = IElementType("LPAREN", CypherLanguage)

    /** Closing round parenthesis `)`. */
    val RPAREN = IElementType("RPAREN", CypherLanguage)

    /** Opening square bracket `[`. Used in relationship patterns and list literals. */
    val LBRACKET = IElementType("LBRACKET", CypherLanguage)

    /** Closing square bracket `]`. */
    val RBRACKET = IElementType("RBRACKET", CypherLanguage)

    /** Opening curly brace `{`. Used in map literals and property constraints. */
    val LBRACE = IElementType("LBRACE", CypherLanguage)

    /** Closing curly brace `}`. */
    val RBRACE = IElementType("RBRACE", CypherLanguage)

    /** Comma `,`. Separates items in lists, return expressions, etc. */
    val COMMA = IElementType("COMMA", CypherLanguage)

    /** Colon `:`. Used in label declarations (`(n:Person)`) and map literals. */
    val COLON = IElementType("COLON", CypherLanguage)

    /** Full stop `.`. Used in property access (`n.name`). */
    val DOT = IElementType("DOT", CypherLanguage)

    /** Semicolon `;`. Optional statement terminator in multi-statement scripts. */
    val SEMICOLON = IElementType("SEMICOLON", CypherLanguage)

    /** Pipe `|`. Used in list comprehensions and `CASE` alternatives. */
    val PIPE = IElementType("PIPE", CypherLanguage)

    /** Asterisk `*`. Wildcard in `RETURN *` and hop-count ranges `[*1..5]`. */
    val ASTERISK = IElementType("ASTERISK", CypherLanguage)

    // -------------------------------------------------------------------------
    // Arrow tokens
    // -------------------------------------------------------------------------

    /** Right-directed relationship arrow `->`. Indicates outgoing direction. */
    val ARROW_RIGHT = IElementType("ARROW_RIGHT", CypherLanguage)

    /** Left-directed relationship arrow `<-`. Indicates incoming direction. */
    val ARROW_LEFT = IElementType("ARROW_LEFT", CypherLanguage)

    /** Single dash `-`. Used as the undirected relationship connector. */
    val DASH = IElementType("DASH", CypherLanguage)

    // -------------------------------------------------------------------------
    // Operator tokens
    // -------------------------------------------------------------------------

    /** Equality operator `=`. */
    val EQ = IElementType("EQ", CypherLanguage)

    /** Inequality operator `<>`. */
    val NEQ = IElementType("NEQ", CypherLanguage)

    /** Less-than operator `<`. */
    val LT = IElementType("LT", CypherLanguage)

    /** Greater-than operator `>`. */
    val GT = IElementType("GT", CypherLanguage)

    /** Less-than-or-equal operator `<=`. */
    val LTE = IElementType("LTE", CypherLanguage)

    /** Greater-than-or-equal operator `>=`. */
    val GTE = IElementType("GTE", CypherLanguage)

    /** Addition operator `+`. Also used for string concatenation. */
    val PLUS = IElementType("PLUS", CypherLanguage)

    /** Subtraction operator `-`. Shared with [DASH] context — resolved at parse time. */
    val MINUS = IElementType("MINUS", CypherLanguage)

    /** Division operator `/`. */
    val SLASH = IElementType("SLASH", CypherLanguage)

    /** Modulus operator `%`. */
    val PERCENT = IElementType("PERCENT", CypherLanguage)

    /** Exponentiation operator `^`. */
    val CARET = IElementType("CARET", CypherLanguage)

    /** Regular expression match operator `=~`. */
    val TILDE_EQ = IElementType("TILDE_EQ", CypherLanguage)

    // -------------------------------------------------------------------------
    // Literal tokens
    // -------------------------------------------------------------------------

    /** Single-quoted or double-quoted string literal, e.g. `'Alice'` or `"Bob"`. */
    val STRING_LITERAL = IElementType("STRING_LITERAL", CypherLanguage)

    /** Integer literal, e.g. `42` or `0`. */
    val INTEGER_LITERAL = IElementType("INTEGER_LITERAL", CypherLanguage)

    /** Floating-point literal, e.g. `3.14` or `1.5e10`. */
    val FLOAT_LITERAL = IElementType("FLOAT_LITERAL", CypherLanguage)

    /** The literal `true` (case-insensitive). */
    val TRUE_LITERAL = IElementType("TRUE_LITERAL", CypherLanguage)

    /** The literal `false` (case-insensitive). */
    val FALSE_LITERAL = IElementType("FALSE_LITERAL", CypherLanguage)

    /** The literal `null` (case-insensitive). */
    val NULL_LITERAL = IElementType("NULL_LITERAL", CypherLanguage)

    // -------------------------------------------------------------------------
    // Identifiers and parameters
    // -------------------------------------------------------------------------

    /**
     * Plain identifier, e.g. `n`, `myNode`, or a backtick-quoted name `` `My Node` ``.
     * Identifiers that match a keyword are emitted as [KEYWORD] instead.
     */
    val IDENTIFIER = IElementType("IDENTIFIER", CypherLanguage)

    /**
     * Query parameter, e.g. `$userId` or `$0`.
     * Includes the leading `$` character in the token text.
     */
    val PARAM = IElementType("PARAM", CypherLanguage)

    // -------------------------------------------------------------------------
    // Keywords and function names
    // -------------------------------------------------------------------------

    /**
     * A Cypher reserved keyword such as `MATCH`, `RETURN`, `WHERE`, etc.
     * Case-insensitive: `match` and `MATCH` both produce a [KEYWORD] token.
     * The full set of keywords is defined in [CypherKeywords.KEYWORDS].
     */
    val KEYWORD = IElementType("KEYWORD", CypherLanguage)

    /**
     * A built-in function name such as `count`, `collect`, `toLower`, etc.
     * Emitted instead of [IDENTIFIER] when the lexer recognises the name
     * (case-insensitively) in [CypherKeywords.FUNCTIONS].
     */
    val FUNCTION_NAME = IElementType("FUNCTION_NAME", CypherLanguage)

    // -------------------------------------------------------------------------
    // Comments
    // -------------------------------------------------------------------------

    /** A line comment starting with `//` and running to the end of the line. */
    val LINE_COMMENT = IElementType("LINE_COMMENT", CypherLanguage)

    /** A block comment delimited by `/*` and `*/`. May span multiple lines. */
    val BLOCK_COMMENT = IElementType("BLOCK_COMMENT", CypherLanguage)

    // -------------------------------------------------------------------------
    // Platform-shared token types
    // -------------------------------------------------------------------------

    /**
     * Whitespace (spaces, tabs, newlines).
     * Delegates to the platform's shared [TokenType.WHITE_SPACE] constant so that
     * the formatter and other platform services recognise whitespace correctly.
     */
    val WHITESPACE = TokenType.WHITE_SPACE

    /**
     * An unrecognised character sequence that could not be matched by any other rule.
     * Delegates to [TokenType.BAD_CHARACTER] so the platform can mark it as an error.
     */
    val BAD_CHARACTER = TokenType.BAD_CHARACTER
}
