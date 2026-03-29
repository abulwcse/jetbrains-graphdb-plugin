package com.graphdbplugin.language.completion

import com.graphdbplugin.language.CypherTokenTypes
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

/**
 * Controls when the IDE auto-triggers the completion popup while the user types
 * in a Cypher editor buffer.
 *
 * By default the IntelliJ Platform skips the auto-popup for languages it doesn't
 * know about. Returning [ThreeState.NO] ("do not skip") from [shouldSkipAutopopup]
 * tells the platform to show the completion popup automatically as soon as the
 * user types an identifier character — without needing to press Ctrl+Space.
 *
 * ### Auto-popup rules
 * - **Identifiers** — popup triggers immediately; covers keywords, function names,
 *   node labels, relationship types, and property keys.
 * - **After `:` and `.`** — the platform re-evaluates confidence; returning NO here
 *   means completions for labels (`(n:`) and property access (`n.`) appear instantly.
 * - **Inside strings and comments** — returning [ThreeState.YES] suppresses the
 *   popup so it does not appear mid-string-literal or inside a comment.
 *
 * Registered in `plugin.xml` via the `completion.confidence` extension point.
 */
class CypherCompletionConfidence : CompletionConfidence() {

    /**
     * Decides whether to suppress the auto-popup at the given [offset] in [psiFile].
     *
     * @param contextElement The PSI element at the caret position.
     * @param psiFile        The file being edited.
     * @param offset         The caret offset within the file.
     * @return [ThreeState.YES] to suppress auto-popup (inside strings/comments);
     *         [ThreeState.NO] to allow auto-popup everywhere else.
     */
    override fun shouldSkipAutopopup(
        contextElement: PsiElement,
        psiFile: PsiFile,
        offset: Int
    ): ThreeState {
        val tokenType = contextElement.node?.elementType
        // Suppress inside string literals and comments — completion there is not useful
        if (tokenType == CypherTokenTypes.STRING_LITERAL ||
            tokenType == CypherTokenTypes.LINE_COMMENT  ||
            tokenType == CypherTokenTypes.BLOCK_COMMENT) {
            return ThreeState.YES
        }
        // Allow auto-popup for everything else: identifiers, after ':', after '.', etc.
        return ThreeState.NO
    }
}
