package com.graphdbplugin.language.completion

import com.graphdbplugin.language.CypherKeywords
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Completion provider that contributes Cypher reserved keywords and built-in
 * function names to the completion popup.
 *
 * This provider is registered unconditionally — it fires at any cursor position
 * in a Cypher file. The platform deduplicates suggestions when the prefix already
 * excludes certain entries.
 *
 * ### Keywords
 * Each keyword from [CypherKeywords.KEYWORDS] is added as a bold lookup element
 * with type text `"keyword"` and a priority of `2.0`, placing keywords above
 * schema-aware completions (which have priority `1.0`) and default completions.
 *
 * ### Functions
 * Each function name from [CypherKeywords.FUNCTIONS] is added with type text
 * `"function"`, a priority of `1.0`, and an insert handler that places the cursor
 * inside the parentheses after inserting `functionName()`.
 *
 * ### Case
 * Keywords are inserted in UPPER_CASE to follow Cypher convention. Function names
 * are inserted in their canonical lower_case form. The completion popup performs
 * case-insensitive prefix matching so typing `mat` offers `MATCH`.
 *
 * @see SchemaAwareCompletionProvider for schema-based label/relationship/property completions.
 */
class KeywordCompletionProvider : CompletionProvider<CompletionParameters>() {

    /**
     * Adds keyword and function-name lookup elements to [result].
     *
     * Called by the platform for every basic completion invocation in a Cypher file.
     * The [parameters] provide cursor context; the [context] carries any data attached
     * by the surrounding [com.intellij.codeInsight.completion.CompletionContributor].
     * Neither is needed here because keyword completions are position-independent.
     *
     * @param parameters Completion invocation parameters (unused by this provider).
     * @param context    Processing context (unused by this provider).
     * @param result     The [CompletionResultSet] to which lookup elements are added.
     */
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Explicitly set the prefix so the platform finds word boundaries correctly
        // in the Cypher custom language (handles cases where the dummy completion
        // identifier disrupts the lexer's token recognition).
        val prefix = CompletionUtil.findReferenceOrAlphanumericPrefix(parameters)
        val resultSet = result.withPrefixMatcher(prefix)

        // Add reserved keywords in UPPER_CASE with priority 2.0
        for (keyword in CypherKeywords.KEYWORDS) {
            val element = LookupElementBuilder.create(keyword)
                .withBoldness(true)
                .withTypeText("keyword")
            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 2.0))
        }

        // Add built-in function names with trailing "()" and cursor-positioning handler
        for (function in CypherKeywords.FUNCTIONS) {
            val element = LookupElementBuilder.create(function)
                .withTailText("()")
                .withTypeText("function")
                .withInsertHandler { insertionContext, _ ->
                    // Insert the parentheses and move cursor to between them
                    val editor = insertionContext.editor
                    val offset = insertionContext.tailOffset
                    insertionContext.document.insertString(offset, "()")
                    editor.caretModel.moveToOffset(offset + 1)
                }
            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 1.0))
        }
    }
}
