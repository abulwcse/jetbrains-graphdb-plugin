package com.graphdbplugin.language

/**
 * Centralised registry of Cypher reserved keywords and built-in function names.
 *
 * The sets in this object are consulted by the [com.graphdbplugin.language.lexer.CypherLexer]
 * when classifying identifier-like tokens: a token whose uppercased value is present in
 * [KEYWORDS] is emitted as [CypherTokenTypes.KEYWORD]; one whose lowercased value is
 * present in [FUNCTIONS] is emitted as [CypherTokenTypes.FUNCTION_NAME]; all others
 * become plain [CypherTokenTypes.IDENTIFIER] tokens.
 *
 * They are also used by [com.graphdbplugin.language.completion.KeywordCompletionProvider]
 * to populate the completion popup with ranked entries.
 *
 * ### Case sensitivity
 * Cypher is case-insensitive for keywords and function names. The [KEYWORDS] set
 * stores entries in UPPER_CASE and the lexer compares after calling `uppercase()`.
 * The [FUNCTIONS] set stores entries in lower_case and the lexer compares after
 * calling `lowercase()`.
 *
 * ### Coverage
 * Covers Cypher as defined in the openCypher specification and Neo4j 5.x extensions,
 * including administrative commands (`SHOW`, `DATABASES`, `ROLES`, etc.) and
 * temporal / spatial functions.
 */
object CypherKeywords {

    /**
     * The complete set of Cypher reserved keywords.
     *
     * Stored in UPPER_CASE. The lexer compares candidate identifiers against this
     * set using `candidateText.uppercase()`. Matching tokens are emitted as
     * [CypherTokenTypes.KEYWORD].
     *
     * Note: `TRUE`, `FALSE`, and `NULL` are included here and produce dedicated
     * literal token types ([CypherTokenTypes.TRUE_LITERAL], [CypherTokenTypes.FALSE_LITERAL],
     * [CypherTokenTypes.NULL_LITERAL]) rather than the generic [CypherTokenTypes.KEYWORD].
     * The lexer checks for those specific values before returning [CypherTokenTypes.KEYWORD].
     */
    val KEYWORDS: Set<String> = setOf(
        "MATCH", "OPTIONAL", "RETURN", "WHERE", "WITH", "UNWIND",
        "CREATE", "MERGE", "DELETE", "DETACH", "SET", "REMOVE",
        "FOREACH", "CALL", "YIELD", "UNION", "ALL", "DISTINCT",
        "AS", "AND", "OR", "NOT", "XOR", "IN", "IS", "STARTS",
        "ENDS", "CONTAINS", "ORDER", "BY", "SKIP", "LIMIT",
        "ASC", "ASCENDING", "DESC", "DESCENDING",
        "CASE", "WHEN", "THEN", "ELSE", "END",
        "TRUE", "FALSE", "NULL", "COUNT",
        "ON", "UNIQUE", "CONSTRAINT", "INDEX", "DROP",
        "LOAD", "CSV", "FROM", "HEADERS", "FIELDTERMINATOR",
        "PERIODIC", "COMMIT", "USING", "SCAN", "JOIN", "START",
        "NODE", "RELATIONSHIP", "REL", "MAP", "LIST", "PATH",
        "PROFILE", "EXPLAIN", "SHOW", "DATABASES", "DATABASE",
        "GRAPHS", "ROLES", "USERS", "PRIVILEGES"
    )

    /**
     * The complete set of Cypher built-in function names.
     *
     * Stored in lower_case. The lexer compares candidate identifiers against this
     * set using `candidateText.lowercase()`. Matching tokens are emitted as
     * [CypherTokenTypes.FUNCTION_NAME] instead of [CypherTokenTypes.IDENTIFIER].
     *
     * Categories included:
     * - **Aggregate**: `count`, `sum`, `avg`, `min`, `max`, `collect`, `stdev`, etc.
     * - **Mathematical**: `abs`, `ceil`, `floor`, `round`, trigonometric functions, etc.
     * - **String**: `toString`, `toLower`, `toUpper`, `trim`, `split`, `replace`, etc.
     * - **Type conversion**: `toInteger`, `toFloat`, `toBoolean`, and nullable variants
     * - **Node/Relationship**: `id`, `labels`, `keys`, `type`, `startNode`, `endNode`, etc.
     * - **Collection**: `range`, `reduce`, `filter`, `extract`, `coalesce`, `exists`, etc.
     * - **Path**: `shortestPath`, `allShortestPaths`
     * - **Date/Time**: `date`, `time`, `localTime`, `dateTime`, `localDateTime`, `duration`
     * - **Miscellaneous**: `randomUUID`, `point`, `distance`
     */
    val FUNCTIONS: Set<String> = setOf(
        // Aggregate functions
        "count", "sum", "avg", "min", "max", "collect", "stdev", "stdevp",
        "percentilecont", "percentiledisc",
        // Mathematical functions
        "abs", "ceil", "floor", "round", "sign", "rand", "exp", "log",
        "log10", "sqrt", "pi", "e", "sin", "cos", "tan", "asin", "acos",
        "atan", "atan2", "degrees", "radians",
        // String functions
        "tostring", "tolower", "toupper", "trim", "ltrim", "rtrim",
        "left", "right", "split", "replace", "reverse", "substring",
        "size", "length", "head", "last", "tail",
        // Type conversion
        "tointeger", "tofloat", "toboolean", "tostringorNull",
        "tointegerornull", "tofloatornull",
        // Node / relationship functions
        "id", "labels", "keys", "type", "startnode", "endnode",
        "nodes", "relationships", "properties", "elementid",
        // Collection functions
        "range", "reduce", "filter", "extract", "all", "any", "none",
        "single", "coalesce", "exists", "isempty",
        // Path functions
        "shortestpath", "allshortestpaths",
        // Date / time functions
        "date", "time", "localtime", "datetime", "localdatetime", "duration",
        // Miscellaneous
        "randomuuid", "point", "distance"
    )
}
