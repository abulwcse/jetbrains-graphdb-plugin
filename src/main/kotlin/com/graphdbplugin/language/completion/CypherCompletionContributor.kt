package com.graphdbplugin.language.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns

/**
 * Main completion contributor for the Cypher language.
 *
 * Registered via the `completion.contributor` extension point in `plugin.xml`:
 * ```xml
 * <completion.contributor language="Cypher"
 *     implementationClass="com.graphdbplugin.language.completion.CypherCompletionContributor"/>
 * ```
 *
 * This class acts as an aggregator that registers individual [com.intellij.codeInsight.completion.CompletionProvider]
 * implementations for different completion scenarios:
 *
 * 1. **[KeywordCompletionProvider]** — contributes all Cypher reserved keywords and
 *    built-in function names. These completions are always available and are ranked
 *    above schema-aware completions.
 *
 * 2. **[SchemaAwareCompletionProvider]** — contributes node labels, relationship types,
 *    and property keys based on the schema cached in [com.graphdbplugin.services.SchemaIntrospectionService].
 *    These completions are only populated after a successful schema refresh.
 *
 * Both providers are registered for [CompletionType.BASIC] (triggered by Ctrl+Space or
 * automatically after typing) and apply to all PSI elements in Cypher files
 * (`PlatformPatterns.psiElement()` matches any position).
 *
 * ### Extension points for future phases
 * In Phase 5, additional context-aware providers can be registered here with narrower
 * [com.intellij.patterns.ElementPattern] filters to offer position-sensitive completions
 * (e.g. only suggesting labels after `(n:`, or only keywords after `MATCH`).
 */
class CypherCompletionContributor : CompletionContributor() {

    init {
        // Register keyword and built-in function completions — highest priority
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            KeywordCompletionProvider()
        )

        // Register schema-aware completions (labels, relationship types, property keys)
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            SchemaAwareCompletionProvider()
        )
    }
}
