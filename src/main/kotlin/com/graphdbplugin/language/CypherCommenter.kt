package com.graphdbplugin.language

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

/**
 * Provides line and block comment delimiters for the Cypher language.
 *
 * Registered via the `lang.commenter` extension point in `plugin.xml`. Enables
 * Ctrl+/ (line comment toggle) and Ctrl+Shift+/ (block comment wrap) in Cypher editor
 * buffers. Documentation-comment methods return null because Cypher has no doc-comment
 * convention.
 */
class CypherCommenter : CodeDocumentationAwareCommenter {

    /** Returns the line-comment prefix: double forward-slash. */
    override fun getLineCommentPrefix(): String = "//"

    /** Returns the block-comment opening delimiter: slash followed by star. */
    override fun getBlockCommentPrefix(): String = "/*"

    /** Returns the block-comment closing delimiter: star followed by slash. */
    override fun getBlockCommentSuffix(): String = "*/"

    /** Returns the prefix for a commented-out block-comment opening delimiter. */
    override fun getCommentedBlockCommentPrefix(): String = "/*"

    /** Returns the suffix for a commented-out block-comment closing delimiter. */
    override fun getCommentedBlockCommentSuffix(): String = "*/"

    /** Returns null — Cypher has no documentation-comment prefix. */
    override fun getDocumentationCommentPrefix(): String? = null

    /** Returns null — Cypher has no per-line documentation-comment prefix. */
    override fun getDocumentationCommentLinePrefix(): String? = null

    /** Returns null — Cypher has no documentation-comment closing delimiter. */
    override fun getDocumentationCommentSuffix(): String? = null

    /** Returns false — Cypher has no documentation comments. */
    override fun isDocumentationComment(element: PsiComment?): Boolean = false

    /** Returns null — no distinct doc-comment token type. */
    override fun getDocumentationCommentTokenType(): IElementType? = null

    /**
     * Returns null — block-comment token detection is handled by
     * CypherParserDefinition.getCommentTokens() which includes BLOCK_COMMENT.
     */
    override fun getBlockCommentTokenType(): IElementType? = null

    /**
     * Returns null — line-comment token detection is handled by
     * CypherParserDefinition.getCommentTokens() which includes LINE_COMMENT.
     */
    override fun getLineCommentTokenType(): IElementType? = null
}
