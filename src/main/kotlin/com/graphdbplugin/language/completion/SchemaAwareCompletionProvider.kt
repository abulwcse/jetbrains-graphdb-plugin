package com.graphdbplugin.language.completion

import com.graphdbplugin.services.SchemaIntrospectionService
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Completion provider that contributes schema-aware lookup elements for
 * node labels, relationship types, and property keys.
 *
 * Schema data is fetched asynchronously from the connected Neo4j instance by
 * [SchemaIntrospectionService] and stored in an in-memory cache. This provider
 * reads from that cache synchronously at completion time, so it returns whatever
 * data was available from the last successful schema refresh. If no schema has
 * been loaded yet (e.g. no connection has been made), no schema completions are
 * contributed — the completion popup still shows keyword completions from
 * [KeywordCompletionProvider].
 *
 * ### Priority
 * Schema completions are added at priority `1.0`, placing them below keywords
 * (priority `2.0`) but above the platform's default completions.
 *
 * ### Type text
 * Each completion element displays a type text label (`"label"`, `"relationship type"`,
 * or `"property key"`) in the completion popup's right column, so users can quickly
 * distinguish schema elements from language keywords.
 *
 * ### Thread safety
 * This provider runs on the IDE's completion thread. [SchemaIntrospectionService]
 * guards its caches with `@Volatile` / `Collections.synchronizedList`, so reading
 * from the cache here is safe.
 *
 * @see SchemaIntrospectionService for the background schema-refresh logic.
 * @see KeywordCompletionProvider for keyword and function-name completions.
 */
class SchemaAwareCompletionProvider : CompletionProvider<CompletionParameters>() {

    /**
     * Adds schema-aware lookup elements (labels, relationship types, property keys)
     * to [result] by reading from the [SchemaIntrospectionService] cache.
     *
     * If the service cache is empty (no schema loaded) or the project has no active
     * connection, this method simply returns without adding anything.
     *
     * @param parameters Completion invocation parameters. Used to obtain the project.
     * @param context    Processing context (unused by this provider).
     * @param result     The [CompletionResultSet] to which lookup elements are added.
     */
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val service = SchemaIntrospectionService.getInstance(project)

        val prefix = CompletionUtil.findReferenceOrAlphanumericPrefix(parameters)
        val resultSet = result.withPrefixMatcher(prefix)

        // Node label completions, e.g. "Person", "Movie"
        for (label in service.getCachedLabels()) {
            val element = LookupElementBuilder.create(label)
                .withTypeText("label")
                .withBoldness(false)
            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 1.0))
        }

        // Relationship type completions, e.g. "KNOWS", "ACTED_IN"
        for (relType in service.getCachedRelationshipTypes()) {
            val element = LookupElementBuilder.create(relType)
                .withTypeText("relationship type")
                .withBoldness(false)
            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 1.0))
        }

        // Property key completions, e.g. "name", "age", "email"
        for (propKey in service.getCachedPropertyKeys()) {
            val element = LookupElementBuilder.create(propKey)
                .withTypeText("property key")
                .withBoldness(false)
            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 1.0))
        }
    }
}
