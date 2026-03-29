package com.graphdbplugin.language.annotation

import com.graphdbplugin.language.CypherTokenTypes
import com.graphdbplugin.language.highlighting.CypherHighlighterColors
import com.graphdbplugin.language.psi.CypherFile
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import java.util.ArrayDeque

/**
 * Lightweight Cypher syntax annotator that runs on the PSI token stream to provide
 * real-time error highlights directly in the editor gutter.
 *
 * Unlike inspections (which run on a full parsed PSI tree), an [Annotator] is invoked
 * for every [PsiElement] as the user types, giving sub-second feedback without blocking
 * the EDT.  Because the Cypher PSI tree produced by [com.graphdbplugin.language.parser.CypherParserDefinition]
 * is intentionally flat (all tokens are immediate children of [CypherFile]), the annotator
 * only acts when the visited element is a [CypherFile] and then iterates its child tokens
 * in a single pass.
 *
 * ### Checks performed
 * 1. **Unmatched parentheses / brackets / braces** — a single-pass stack algorithm detects
 *    brackets that have no matching closer, or closers that have no preceding opener.
 *    Each problem token is annotated at [HighlightSeverity.ERROR].
 * 2. **Bare `$` parameter** — a `$` without a following identifier (i.e. a lone `$` token
 *    whose text is exactly `"$"`) is annotated at [HighlightSeverity.ERROR].
 * 3. **Unterminated string literals** — a [CypherTokenTypes.STRING_LITERAL] whose text
 *    starts with `'` or `"` but does not end with the same quote character is annotated
 *    at [HighlightSeverity.ERROR].
 *
 * ### Registration
 * Registered in `plugin.xml` via:
 * ```xml
 * <annotator language="Cypher"
 *     implementationClass="com.graphdbplugin.language.annotation.CypherAnnotator"/>
 * ```
 */
class CypherAnnotator : Annotator {

    /**
     * Entry point called by the IntelliJ Platform for every [PsiElement] in the file.
     *
     * The method exits early for any element that is not a [CypherFile] root node.
     * When a [CypherFile] is encountered it iterates over all direct child AST nodes
     * (tokens) and performs three independent checks:
     *
     * 1. **Bracket matching** — openers `(`, `[`, `{` are pushed onto a [ArrayDeque].
     *    When a closer `)`, `]`, `}` is seen the stack is popped; if the stack is empty
     *    or the top does not match the closer, the closer token is annotated.  After the
     *    full traversal any openers remaining on the stack are also annotated.
     *
     * 2. **Lone `$`** — [CypherTokenTypes.PARAM] tokens whose full text is exactly `"$"`
     *    (i.e. no identifier follows within the same token) are annotated.
     *
     * 3. **Unterminated strings** — [CypherTokenTypes.STRING_LITERAL] tokens are checked:
     *    if the text is shorter than 2 characters, or the last character does not match
     *    the opening quote character, the token is annotated.
     *
     * @param element The PSI element currently being visited.
     * @param holder  The annotation sink to which error annotations are added.
     */
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Only act on the file root; all tokens are direct children of CypherFile.
        if (element !is CypherFile) return

        // Stack for bracket matching: each entry is an AST child node that is an opener.
        val stack = ArrayDeque<PsiElement>()

        for (child in element.children) {
            val nodeType = child.node.elementType
            val text = child.text

            // ------------------------------------------------------------------
            // 1. Bracket matching
            // ------------------------------------------------------------------
            when (nodeType) {
                CypherTokenTypes.LPAREN,
                CypherTokenTypes.LBRACKET,
                CypherTokenTypes.LBRACE -> {
                    // Push opener onto stack
                    stack.push(child)
                }

                CypherTokenTypes.RPAREN,
                CypherTokenTypes.RBRACKET,
                CypherTokenTypes.RBRACE -> {
                    val expectedOpener = when (nodeType) {
                        CypherTokenTypes.RPAREN   -> CypherTokenTypes.LPAREN
                        CypherTokenTypes.RBRACKET -> CypherTokenTypes.LBRACKET
                        else                      -> CypherTokenTypes.LBRACE
                    }

                    val top = stack.peek()
                    if (top != null && top.node.elementType == expectedOpener) {
                        // Matching pair found — consume opener from stack
                        stack.pop()
                    } else {
                        // No matching opener → annotate the stray closer
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            "Unmatched '${text}'"
                        ).range(child.textRange).create()
                    }
                }

                // ------------------------------------------------------------------
                // 2. Bare '$' parameter (token text is exactly "$")
                // ------------------------------------------------------------------
                CypherTokenTypes.PARAM -> {
                    if (text == "$") {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            "Expected parameter name after '\$'"
                        ).range(child.textRange).create()
                    }
                }

                // ------------------------------------------------------------------
                // 3. Unterminated string literal
                // ------------------------------------------------------------------
                CypherTokenTypes.STRING_LITERAL -> {
                    if (text.length >= 1) {
                        val openQuote = text[0]
                        if (openQuote == '\'' || openQuote == '"') {
                            val terminated = text.length >= 2 && text[text.length - 1] == openQuote
                            if (!terminated) {
                                holder.newAnnotation(
                                    HighlightSeverity.ERROR,
                                    "Unterminated string literal"
                                ).range(child.textRange).create()
                            }
                        }
                    }
                }

                else -> { /* no annotation needed */ }
            }
        }

        // Any openers still on the stack were never closed → annotate them
        while (stack.isNotEmpty()) {
            val unclosed = stack.pop()
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Unmatched '${unclosed.text}'"
            ).range(unclosed.textRange).create()
        }

        // ------------------------------------------------------------------
        // Pass 2: Variable highlighting
        // Identifiers immediately after ( or [ (before any other non-WS
        // token) are variable bindings — highlight them distinctly.
        // e.g.  (n:Person)  → n is a variable
        //       [r:KNOWS]   → r is a variable
        //       (:Label)    → no variable (colon comes first)
        // ------------------------------------------------------------------
        val tokens = element.children.toList()
        for (i in tokens.indices) {
            val tokenType = tokens[i].node.elementType
            if (tokenType == CypherTokenTypes.LPAREN || tokenType == CypherTokenTypes.LBRACKET) {
                val next = tokens.getOrNull(i + 1) ?: continue
                if (next.node.elementType == CypherTokenTypes.IDENTIFIER) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .textAttributes(CypherHighlighterColors.VARIABLE)
                        .range(next.textRange)
                        .create()
                }
            }
        }
    }
}
