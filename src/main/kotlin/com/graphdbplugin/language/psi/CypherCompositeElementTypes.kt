package com.graphdbplugin.language.psi

/**
 * Registry of all composite (non-leaf) PSI node types for the Cypher language.
 *
 * Each constant represents a named syntactic construct in the Cypher grammar.
 * The [com.graphdbplugin.language.parser.CypherParserDefinition] parser wraps
 * token sequences in nodes of these types to build the PSI tree.
 *
 * ### Current scope (Phase 2)
 * The Phase 2 parser is deliberately minimal — it creates a flat [FILE] node
 * that directly contains all tokens (i.e. no sub-nodes of type [STATEMENT] or
 * [CLAUSE] are actually created yet). The additional type constants below are
 * reserved for use in the full grammar parser that will be added in Phase 5.
 *
 * ### PSI Viewer
 * These names appear in the IDE's built-in PSI Viewer (Tools → PSI Viewer when
 * developer mode is enabled), making it straightforward to inspect the tree
 * structure of any `.cypher` file.
 */
object CypherCompositeElementTypes {

    /** Root file node; wraps the entire content of a `.cypher` file. */
    val FILE by lazy { CypherElementType("FILE") }

    /**
     * A single complete Cypher statement, i.e. a sequence of clauses optionally
     * terminated by a semicolon. Reserved for Phase 5 full-grammar parsing.
     */
    val STATEMENT by lazy { CypherElementType("STATEMENT") }

    /**
     * A generic clause node used as a common supertype for more specific clause
     * types below. Reserved for Phase 5.
     */
    val CLAUSE by lazy { CypherElementType("CLAUSE") }

    /**
     * A `MATCH` clause, e.g. `MATCH (n:Person)`. Reserved for Phase 5.
     */
    val MATCH_CLAUSE by lazy { CypherElementType("MATCH_CLAUSE") }

    /**
     * A `RETURN` clause, e.g. `RETURN n.name`. Reserved for Phase 5.
     */
    val RETURN_CLAUSE by lazy { CypherElementType("RETURN_CLAUSE") }

    /**
     * A `WITH` clause used to pipe results between query parts. Reserved for Phase 5.
     */
    val WITH_CLAUSE by lazy { CypherElementType("WITH_CLAUSE") }

    /**
     * A `CREATE` clause, e.g. `CREATE (n:Person {name: 'Alice'})`. Reserved for Phase 5.
     */
    val CREATE_CLAUSE by lazy { CypherElementType("CREATE_CLAUSE") }

    /**
     * A `WHERE` clause, e.g. `WHERE n.age > 30`. Reserved for Phase 5.
     */
    val WHERE_CLAUSE by lazy { CypherElementType("WHERE_CLAUSE") }

    /**
     * A generic expression node covering comparisons, arithmetic, function calls,
     * literals, and identifiers. Reserved for Phase 5.
     */
    val EXPRESSION by lazy { CypherElementType("EXPRESSION") }
}
