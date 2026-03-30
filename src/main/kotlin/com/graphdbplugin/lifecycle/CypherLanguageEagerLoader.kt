package com.graphdbplugin.lifecycle

import com.graphdbplugin.language.CypherLanguage
import com.intellij.ide.ApplicationInitializedListener

/**
 * Ensures [CypherLanguage] is registered with the IntelliJ Platform during the
 * application initialisation phase, before any file-type resolution, background
 * indexing, or user interaction can trigger lazy class loading.
 *
 * ### Why this is needed
 * IntelliJ Platform 2026.1 (build 261) tightened language-registration timing:
 * a [com.intellij.lang.Language] whose constructor is called after the platform's
 * startup window closes causes an error that permanently poisons the class (the
 * JVM marks it as failed and every subsequent access throws
 * [NoClassDefFoundError]).
 *
 * `CypherLanguage` is a Kotlin `object`, so it is initialised lazily on first
 * access.  Without this loader, the first access can happen from a background
 * indexing thread or from the file-type manager — both outside the safe window.
 *
 * Registered in `plugin.xml` as an [ApplicationInitializedListener]:
 * ```xml
 * <listener class="...CypherLanguageEagerLoader"
 *           topic="com.intellij.ide.ApplicationInitializedListener"/>
 * ```
 */
class CypherLanguageEagerLoader : ApplicationInitializedListener {

    /**
     * Called by the platform once all application components have been initialised.
     * Accessing [CypherLanguage] here forces the Kotlin object to run its
     * `Language("Cypher")` constructor call inside the safe registration window.
     */
    override suspend fun execute() {
        // Touch CypherLanguage to trigger its object initialisation and register
        // the language with the platform before any file-type or indexing code runs.
        @Suppress("UNUSED_EXPRESSION")
        CypherLanguage
    }
}
