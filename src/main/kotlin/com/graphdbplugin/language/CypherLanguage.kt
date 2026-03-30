package com.graphdbplugin.language

import com.intellij.lang.Language

/**
 * Singleton language definition for the Cypher query language.
 *
 * Declared as a `class` (not a Kotlin `object`) so that [INSTANCE] is initialised
 * eagerly in the companion-object static initialiser when the class is first loaded
 * by the JVM. This guarantees that the `Language("Cypher")` constructor — which
 * registers the language with the IntelliJ Platform — runs at class-load time rather
 * than on first access from an arbitrary thread, avoiding the race condition
 * introduced in IntelliJ Platform build 261 (GoLand / IDEA 2026.1).
 *
 * [CypherFileType] is declared in `plugin.xml` with `fieldName="INSTANCE"`, which
 * causes the platform to access `CypherFileType.INSTANCE` during its file-type
 * initialisation phase. That access loads [CypherFileType], whose constructor calls
 * `LanguageFileType(CypherLanguage.INSTANCE)`, which in turn loads this class and
 * fires the static initialiser — all on the main thread, well within the safe
 * registration window.
 */
class CypherLanguage private constructor() : Language("Cypher") {

    /**
     * Returns the human-readable display name shown in IDE UI elements such as
     * the color-settings page and the language status bar widget.
     *
     * @return The string `"Cypher"`.
     */
    override fun getDisplayName() = "Cypher"

    companion object {
        /** The single [CypherLanguage] instance. Always use this; never instantiate directly. */
        @JvmField
        val INSTANCE: CypherLanguage = CypherLanguage()
    }
}