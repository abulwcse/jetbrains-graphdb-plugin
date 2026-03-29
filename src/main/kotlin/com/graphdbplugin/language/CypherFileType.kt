package com.graphdbplugin.language

import com.graphdbplugin.GraphDbPluginIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * File type descriptor for Cypher query language files.
 *
 * Registers the `.cypher` and `.cql` file extensions with the IntelliJ Platform so
 * that any file bearing those extensions is automatically associated with the Cypher
 * language, enabling syntax highlighting, code completion, and all other language
 * features provided by this plugin.
 *
 * The file type is declared in `plugin.xml` via the `<fileType>` extension point:
 * ```xml
 * <fileType name="Cypher"
 *           implementationClass="com.graphdbplugin.language.CypherFileType"
 *           fieldName="INSTANCE"
 *           language="Cypher"
 *           extensions="cypher;cql"/>
 * ```
 *
 * The `fieldName="INSTANCE"` attribute tells the platform to use the [INSTANCE]
 * companion-object field rather than reflective instantiation, which is the
 * recommended pattern for singleton file types.
 *
 * This class extends [LanguageFileType] and therefore binds itself to [CypherLanguage]
 * so that the platform can route PSI-related operations to the correct language support.
 */
class CypherFileType private constructor() : LanguageFileType(CypherLanguage) {

    /**
     * Returns the unique internal name of this file type.
     *
     * This name is used as a stable key in the IntelliJ Platform's file type registry
     * and must match the `name` attribute in the `plugin.xml` declaration.
     *
     * @return The string `"Cypher"`.
     */
    override fun getName(): String = "Cypher"

    /**
     * Returns the human-readable description of this file type, shown in
     * Settings → Editor → File Types when the user inspects the Cypher entry.
     *
     * @return A short description string.
     */
    override fun getDescription(): String = "Cypher Query Language file"

    /**
     * Returns the default file extension (without the leading dot) used when the
     * IDE creates a new file of this type, e.g. via File → New.
     *
     * The `.cql` extension is also registered in `plugin.xml` but `.cypher` is
     * preferred as the canonical extension.
     *
     * @return The string `"cypher"`.
     */
    override fun getDefaultExtension(): String = "cypher"

    /**
     * Returns the icon displayed next to Cypher files in the Project tree,
     * editor tabs, and file chooser dialogs.
     *
     * Uses [GraphDbPluginIcons.GRAPH_DB] so Cypher files share the plugin's
     * main branding icon, making them visually distinct from generic text files.
     *
     * @return The plugin's main graph-database icon.
     */
    override fun getIcon(): Icon = GraphDbPluginIcons.GRAPH_DB

    companion object {

        /**
         * Singleton instance used by the platform via `fieldName="INSTANCE"` in `plugin.xml`.
         *
         * Always use this field to refer to the Cypher file type; do not instantiate
         * [CypherFileType] directly.
         *
         * Example:
         * ```kotlin
         * val fileType: LanguageFileType = CypherFileType.INSTANCE
         * ```
         */
        @JvmField
        val INSTANCE: CypherFileType = CypherFileType()
    }
}
