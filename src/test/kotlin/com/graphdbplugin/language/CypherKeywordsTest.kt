package com.graphdbplugin.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CypherKeywords].
 *
 * Verifies the structural invariants of the [CypherKeywords.KEYWORDS] and
 * [CypherKeywords.FUNCTIONS] sets: non-emptiness, presence of well-known entries,
 * case conventions, and absence of duplicates.
 *
 * These tests are pure in-memory unit tests — they require no IntelliJ Platform
 * runtime, no mock environment, and no Neo4j instance.
 */
class CypherKeywordsTest {

    // -------------------------------------------------------------------------
    // Non-emptiness
    // -------------------------------------------------------------------------

    /**
     * Verifies that [CypherKeywords.KEYWORDS] contains at least one entry.
     * A completely empty keywords set would make the lexer unable to classify
     * any reserved words.
     */
    @Test
    fun testKeywordsNotEmpty() {
        assertTrue("KEYWORDS set must not be empty", CypherKeywords.KEYWORDS.isNotEmpty())
    }

    /**
     * Verifies that [CypherKeywords.FUNCTIONS] contains at least one entry.
     * A completely empty functions set would make the lexer unable to classify
     * any built-in function names.
     */
    @Test
    fun testFunctionsNotEmpty() {
        assertTrue("FUNCTIONS set must not be empty", CypherKeywords.FUNCTIONS.isNotEmpty())
    }

    // -------------------------------------------------------------------------
    // Known-entry presence
    // -------------------------------------------------------------------------

    /**
     * Verifies that the five most fundamental Cypher keywords are present in
     * [CypherKeywords.KEYWORDS].
     */
    @Test
    fun testKnownKeywordsPresent() {
        val required = listOf("MATCH", "RETURN", "WHERE", "CREATE", "DELETE")
        for (kw in required) {
            assertTrue("Expected '$kw' in KEYWORDS", kw in CypherKeywords.KEYWORDS)
        }
    }

    /**
     * Verifies that the five most commonly used built-in functions are present in
     * [CypherKeywords.FUNCTIONS].
     */
    @Test
    fun testKnownFunctionsPresent() {
        val required = listOf("count", "id", "labels", "type", "collect")
        for (fn in required) {
            assertTrue("Expected '$fn' in FUNCTIONS", fn in CypherKeywords.FUNCTIONS)
        }
    }

    // -------------------------------------------------------------------------
    // Case conventions
    // -------------------------------------------------------------------------

    /**
     * Verifies that every keyword stored in [CypherKeywords.KEYWORDS] is in
     * UPPER_CASE.
     *
     * The lexer relies on this invariant when comparing `candidateText.uppercase()`
     * against the set.
     */
    @Test
    fun testKeywordsAreUpperCase() {
        for (kw in CypherKeywords.KEYWORDS) {
            assertEquals(
                "Keyword '$kw' must be stored in UPPER_CASE",
                kw.uppercase(),
                kw
            )
        }
    }

    /**
     * Verifies that every function name stored in [CypherKeywords.FUNCTIONS] is in
     * lower_case.
     *
     * The lexer relies on this invariant when comparing `candidateText.lowercase()`
     * against the set.
     */
    @Test
    fun testFunctionsAreLowerCase() {
        for (fn in CypherKeywords.FUNCTIONS) {
            assertEquals(
                "Function '$fn' must be stored in lower_case",
                fn.lowercase(),
                fn
            )
        }
    }

    // -------------------------------------------------------------------------
    // No duplicates
    // -------------------------------------------------------------------------

    /**
     * Verifies that [CypherKeywords.KEYWORDS] has no duplicate entries.
     *
     * Because the backing type is already a [Set], duplicates would have been
     * silently discarded at construction time; this test guards against future
     * refactors that might convert the set to a list.
     */
    @Test
    fun testNoDuplicates_keywords() {
        val asList = CypherKeywords.KEYWORDS.toList()
        val asSet  = asList.toSet()
        assertEquals(
            "KEYWORDS contains duplicate entries",
            asSet.size,
            asList.size
        )
    }

    /**
     * Verifies that [CypherKeywords.FUNCTIONS] has no duplicate entries.
     */
    @Test
    fun testNoDuplicates_functions() {
        val asList = CypherKeywords.FUNCTIONS.toList()
        val asSet  = asList.toSet()
        assertEquals(
            "FUNCTIONS contains duplicate entries",
            asSet.size,
            asList.size
        )
    }

    // -------------------------------------------------------------------------
    // Specific value checks
    // -------------------------------------------------------------------------

    /**
     * Verifies that the lowercase string `"true"` is NOT in [CypherKeywords.KEYWORDS].
     *
     * Keywords are stored in UPPER_CASE; `"TRUE"` should be present, not `"true"`.
     * This guards against accidental mixed-case entries being added.
     */
    @Test
    fun testTrueNotInKeywords() {
        assertFalse(
            "'true' (lowercase) must not be in KEYWORDS — use 'TRUE'",
            "true" in CypherKeywords.KEYWORDS
        )
        assertTrue(
            "'TRUE' (uppercase) must be in KEYWORDS",
            "TRUE" in CypherKeywords.KEYWORDS
        )
    }

    /**
     * Verifies that the string `"MATCH"` (uppercase) is in [CypherKeywords.KEYWORDS].
     *
     * Redundant with [testKnownKeywordsPresent] but serves as a named entry point
     * for quick triage in CI output.
     */
    @Test
    fun testMatchIsInKeywords() {
        assertTrue("'MATCH' must be in KEYWORDS", "MATCH" in CypherKeywords.KEYWORDS)
    }
}
