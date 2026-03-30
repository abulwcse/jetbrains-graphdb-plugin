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
 * ### Lazy initialisation
 * Every [IElementType] that references [CypherLanguage] is declared with `by lazy`
 * so that the `IElementType` registry entry is not created until the token type is
 * first accessed. [CypherLanguage.INSTANCE] itself is initialised eagerly at
 * class-load time (see [CypherLanguage] KDoc), so the `by lazy` here is purely
 * about deferring [IElementType] registration, not about language-registration timing.
 *
 * [WHITESPACE] and [BAD_CHARACTER] delegate to platform-shared constants and do
 * not reference [CypherLanguage], so they are left as plain properties.
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
    val LPAREN by lazy { IElementType("LPAREN", CypherLanguage.INSTANCE) }

    /** Closing round parenthesis `)`. */
    val RPAREN by lazy { IElementType("RPAREN", CypherLanguage.INSTANCE) }

    /** Opening square bracket `[`. Used in relationship patterns and list literals. */
    val LBRACKET by lazy { IElementType("LBRACKET", CypherLanguage.INSTANCE) }

    /** Closing square bracket `]`. */
    val RBRACKET by lazy { IElementType("RBRACKET", CypherLanguage.INSTANCE) }

    /** Opening curly brace `{`. Used in map literals and property constraints. */
    val LBRACE by lazy { IElementType("LBRACE", CypherLanguage.INSTANCE) }

    /** Closing curly brace `}`. */
    val RBRACE by lazy { IElementType("RBRACE", CypherLanguage.INSTANCE) }

    /** Comma `,`. Separates items in lists, return expressions, etc. */
    val COMMA by lazy { IElementType("COMMA", CypherLanguage.INSTANCE) }

    /** Colon `:`. Used in label declarations (`(n:Person)`) and map literals. */
    val COLON by lazy { IElementType("COLON", CypherLanguage.INSTANCE) }

    /** Full stop `.`. Used in property access (`n.name`). */
    val DOT by lazy { IElementType("DOT", CypherLanguage.INSTANCE) }

    /** Semicolon `;`. Optional statement terminator in multi-statement scripts. */
    val SEMICOLON by lazy { IElementType("SEMICOLON", CypherLanguage.INSTANCE) }

    /** Pipe `|`. Used in list comprehensions and `CASE` alternatives. */
    val PIPE by lazy { IElementType("PIPE", CypherLanguage.INSTANCE) }

    /** Asterisk `*`. Wildcard in `RETURN *` and hop-count ranges `[*1..5]`. */
    val ASTERISK by lazy { IElementType("ASTERISK", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Arrow tokens
    // -------------------------------------------------------------------------

    /** Right-directed relationship arrow `->`. Indicates outgoing direction. */
    val ARROW_RIGHT by lazy { IElementType("ARROW_RIGHT", CypherLanguage.INSTANCE) }

    /** Left-directed relationship arrow `<-`. Indicates incoming direction. */
    val ARROW_LEFT by lazy { IElementType("ARROW_LEFT", CypherLanguage.INSTANCE) }

    /** Single dash `-`. Used as the undirected relationship connector. */
    val DASH by lazy { IElementType("DASH", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Operator tokens
    // -------------------------------------------------------------------------

    /** Equality operator `=`. */
    val EQ by lazy { IElementType("EQ", CypherLanguage.INSTANCE) }

    /** Inequality operator `<>`. */
    val NEQ by lazy { IElementType("NEQ", CypherLanguage.INSTANCE) }

    /** Less-than operator `<`. */
    val LT by lazy { IElementType("LT", CypherLanguage.INSTANCE) }

    /** Greater-than operator `>`. */
    val GT by lazy { IElementType("GT", CypherLanguage.INSTANCE) }

    /** Less-than-or-equal operator `<=`. */
    val LTE by lazy { IElementType("LTE", CypherLanguage.INSTANCE) }

    /** Greater-than-or-equal operator `>=`. */
    val GTE by lazy { IElementType("GTE", CypherLanguage.INSTANCE) }

    /** Addition operator `+`. Also used for string concatenation. */
    val PLUS by lazy { IElementType("PLUS", CypherLanguage.INSTANCE) }

    /** Subtraction operator `-`. Shared with [DASH] context — resolved at parse time. */
    val MINUS by lazy { IElementType("MINUS", CypherLanguage.INSTANCE) }

    /** Division operator `/`. */
    val SLASH by lazy { IElementType("SLASH", CypherLanguage.INSTANCE) }

    /** Modulus operator `%`. */
    val PERCENT by lazy { IElementType("PERCENT", CypherLanguage.INSTANCE) }

    /** Exponentiation operator `^`. */
    val CARET by lazy { IElementType("CARET", CypherLanguage.INSTANCE) }

    /** Regular expression match operator `=~`. */
    val TILDE_EQ by lazy { IElementType("TILDE_EQ", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Literal tokens
    // -------------------------------------------------------------------------

    /** Single-quoted or double-quoted string literal, e.g. `'Alice'` or `"Bob"`. */
    val STRING_LITERAL by lazy { IElementType("STRING_LITERAL", CypherLanguage.INSTANCE) }

    /** Integer literal, e.g. `42` or `0`. */
    val INTEGER_LITERAL by lazy { IElementType("INTEGER_LITERAL", CypherLanguage.INSTANCE) }

    /** Floating-point literal, e.g. `3.14` or `1.5e10`. */
    val FLOAT_LITERAL by lazy { IElementType("FLOAT_LITERAL", CypherLanguage.INSTANCE) }

    /** The literal `true` (case-insensitive). */
    val TRUE_LITERAL by lazy { IElementType("TRUE_LITERAL", CypherLanguage.INSTANCE) }

    /** The literal `false` (case-insensitive). */
    val FALSE_LITERAL by lazy { IElementType("FALSE_LITERAL", CypherLanguage.INSTANCE) }

    /** The literal `null` (case-insensitive). */
    val NULL_LITERAL by lazy { IElementType("NULL_LITERAL", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Identifiers and parameters
    // -------------------------------------------------------------------------

    /**
     * Plain identifier, e.g. `n`, `myNode`, or a backtick-quoted name `` `My Node` ``.
     * Identifiers that match a keyword are emitted as [KEYWORD] instead.
     */
    val IDENTIFIER by lazy { IElementType("IDENTIFIER", CypherLanguage.INSTANCE) }

    /**
     * Query parameter, e.g. `$userId` or `$0`.
     * Includes the leading `$` character in the token text.
     */
    val PARAM by lazy { IElementType("PARAM", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Keywords and function names
    // -------------------------------------------------------------------------

    /**
     * A Cypher reserved keyword such as `MATCH`, `RETURN`, `WHERE`, etc.
     * Case-insensitive: `match` and `MATCH` both produce a [KEYWORD] token.
     * The full set of keywords is defined in [CypherKeywords.KEYWORDS].
     */
    val KEYWORD by lazy { IElementType("KEYWORD", CypherLanguage.INSTANCE) }

    /**
     * A built-in function name such as `count`, `collect`, `toLower`, etc.
     * Emitted instead of [IDENTIFIER] when the lexer recognises the name
     * (case-insensitively) in [CypherKeywords.FUNCTIONS].
     */
    val FUNCTION_NAME by lazy { IElementType("FUNCTION_NAME", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Comments
    // -------------------------------------------------------------------------

    /** A line comment starting with `//` and running to the end of the line. */
    val LINE_COMMENT by lazy { IElementType("LINE_COMMENT", CypherLanguage.INSTANCE) }

    /** A block comment delimited by `/*` and `*/`. May span multiple lines. */
    val BLOCK_COMMENT by lazy { IElementType("BLOCK_COMMENT", CypherLanguage.INSTANCE) }

    // -------------------------------------------------------------------------
    // Platform-shared token types (no CypherLanguage dependency — not lazy)
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
