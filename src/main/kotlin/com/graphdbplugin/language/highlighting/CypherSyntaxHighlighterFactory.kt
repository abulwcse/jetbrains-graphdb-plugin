package com.graphdbplugin.language.highlighting

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Factory that creates [CypherSyntaxHighlighter] instances on demand.
 *
 * The IntelliJ Platform uses this factory (registered via the
 * `lang.syntaxHighlighterFactory` extension point in `plugin.xml`) to obtain a
 * [SyntaxHighlighter] for any file whose language is `Cypher`. The factory pattern
 * allows the platform to create a fresh highlighter for each editor tab and to
 * supply the [Project] and [VirtualFile] context, which advanced highlighters can
 * use for project-aware colouring.
 *
 * ### Registration
 * ```xml
 * <lang.syntaxHighlighterFactory language="Cypher"
 *     implementationClass="com.graphdbplugin.language.highlighting.CypherSyntaxHighlighterFactory"/>
 * ```
 *
 * @see CypherSyntaxHighlighter for the actual highlighting logic.
 */
class CypherSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    /**
     * Creates and returns a new [CypherSyntaxHighlighter].
     *
     * The [project] and [virtualFile] parameters are available for context-sensitive
     * highlighting (e.g. colouring node labels based on the connected database schema),
     * but are not currently used — the Phase 2 highlighter is purely lexical.
     *
     * @param project     The current [Project], or `null` during IDE startup / indexing.
     * @param virtualFile The file being highlighted, or `null` when called outside
     *                    a file context (e.g. in the colour-settings preview panel).
     * @return A new stateless [CypherSyntaxHighlighter].
     */
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        CypherSyntaxHighlighter()
}
