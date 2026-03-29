package com.graphdbplugin.language.parser

import com.graphdbplugin.language.CypherLanguage
import com.graphdbplugin.language.CypherTokenTypes
import com.graphdbplugin.language.lexer.CypherLexer
import com.graphdbplugin.language.psi.CypherFile
import com.graphdbplugin.language.psi.CypherPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.lang.PsiBuilder

/**
 * Parser definition for the Cypher language.
 *
 * This class is the central integration point between the IntelliJ Platform's
 * language infrastructure and the Cypher language support. It supplies:
 * - A [CypherLexer] for tokenisation
 * - A flat [PsiParser] that wraps all tokens under a single [FILE_ELEMENT_TYPE] node
 * - Token sets identifying whitespace, comments, and string literals
 * - Factory methods for creating PSI elements and the root [CypherFile]
 *
 * ### Why a flat parser?
 * A full hierarchical Cypher grammar parser is planned for Phase 5. For Phase 2,
 * a flat parser (all tokens as direct children of the file node) is sufficient to
 * enable syntax highlighting, code completion, and commenting — none of which
 * require a structured tree. The flat approach also avoids parse errors appearing
 * in the editor before the full grammar is implemented.
 *
 * ### Registration
 * Registered in `plugin.xml`:
 * ```xml
 * <lang.parserDefinition language="Cypher"
 *     implementationClass="com.graphdbplugin.language.parser.CypherParserDefinition"/>
 * ```
 */
class CypherParserDefinition : ParserDefinition {

    companion object {

        /**
         * The root element type for the Cypher file PSI tree.
         *
         * All other PSI nodes are descendants of a node of this type.
         * Declared as a companion constant so it can be referenced from
         * [CypherFile] and other places without creating a [CypherParserDefinition] instance.
         */
        @JvmField
        val FILE_ELEMENT_TYPE: IFileElementType = IFileElementType(CypherLanguage)

        /** Token set for whitespace tokens — used by the platform to skip insignificant gaps. */
        @JvmField
        val WHITESPACE_TOKENS: TokenSet = TokenSet.create(CypherTokenTypes.WHITESPACE)

        /** Token set for comment tokens — used by the platform for comment-folding, etc. */
        @JvmField
        val COMMENT_TOKENS: TokenSet = TokenSet.create(
            CypherTokenTypes.LINE_COMMENT,
            CypherTokenTypes.BLOCK_COMMENT
        )

        /** Token set for string-literal tokens — used by the platform's string-escape utilities. */
        @JvmField
        val STRING_TOKENS: TokenSet = TokenSet.create(CypherTokenTypes.STRING_LITERAL)
    }

    /**
     * Creates a new [CypherLexer] instance for tokenising Cypher source text.
     *
     * The platform may create multiple lexer instances concurrently (e.g. for
     * different editors). Each call returns a fresh, independent instance.
     *
     * @param project The current [Project], or `null` during index build. Unused by
     *                the stateless [CypherLexer].
     * @return A new [CypherLexer] ready to [com.intellij.lexer.Lexer.start].
     */
    override fun createLexer(project: Project?): Lexer = CypherLexer()

    /**
     * Returns the root file element type for the Cypher PSI tree.
     *
     * @return [FILE_ELEMENT_TYPE] — a language-scoped [IFileElementType] for Cypher.
     */
    override fun getFileNodeType(): IFileElementType = FILE_ELEMENT_TYPE

    /**
     * Returns the token set that the platform should treat as insignificant whitespace.
     *
     * The platform uses this set to skip whitespace when traversing the PSI tree and
     * during formatting and code analysis.
     *
     * @return A [TokenSet] containing [CypherTokenTypes.WHITESPACE].
     */
    override fun getWhitespaceTokens(): TokenSet = WHITESPACE_TOKENS

    /**
     * Returns the token set that the platform should treat as comments.
     *
     * Used for comment folding, the "Comment with Line Comment" / "Comment with
     * Block Comment" actions (which are handled by [com.graphdbplugin.language.CypherCommenter]),
     * and various inspections.
     *
     * @return A [TokenSet] containing [CypherTokenTypes.LINE_COMMENT] and
     *         [CypherTokenTypes.BLOCK_COMMENT].
     */
    override fun getCommentTokens(): TokenSet = COMMENT_TOKENS

    /**
     * Returns the token set that the platform should treat as string literals.
     *
     * Used by string-injection support, string escape inspections, and other
     * platform features that operate on string content.
     *
     * @return A [TokenSet] containing [CypherTokenTypes.STRING_LITERAL].
     */
    override fun getStringLiteralElements(): TokenSet = STRING_TOKENS

    /**
     * Creates the [PsiParser] that builds the PSI tree from the token stream.
     *
     * For Phase 2 this returns a minimal flat parser: it marks the entire content
     * as a single [FILE_ELEMENT_TYPE] node and advances past every token without
     * creating any nested structure. This is sufficient for syntax highlighting,
     * code folding of comments, and completion — all of which operate on the raw
     * token stream rather than the structured tree.
     *
     * The returned parser is a lambda-based implementation of [PsiParser]; a
     * full named class will replace it in Phase 5.
     *
     * @param project The current [Project]. Unused by the flat parser.
     * @return A [PsiParser] that produces a flat single-level Cypher PSI tree.
     */
    override fun createParser(project: Project?): PsiParser {
        return PsiParser { root: IElementType, builder: PsiBuilder ->
            val fileMarker = builder.mark()
            // Consume every token, leaving them all as direct children of the file node.
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            fileMarker.done(FILE_ELEMENT_TYPE)
            builder.treeBuilt
        }
    }

    /**
     * Creates the appropriate [PsiElement] implementation for a composite AST node.
     *
     * For Phase 2, all composite nodes (if any were created) would be wrapped in
     * [CypherPsiElement]. The flat parser does not create any composite nodes below
     * the file level, so in practice this method is only called for the file node
     * itself — which is handled separately by [createFile].
     *
     * @param node The [ASTNode] for which to create a PSI wrapper.
     * @return A [CypherPsiElement] wrapping [node].
     */
    override fun createElement(node: ASTNode): PsiElement = CypherPsiElement(node)

    /**
     * Creates the root [CypherFile] PSI element for a new virtual file.
     *
     * The platform calls this once per file open to create the root of the PSI tree.
     *
     * @param viewProvider The [FileViewProvider] backed by the file's virtual file.
     * @return A new [CypherFile] rooted at [viewProvider].
     */
    override fun createFile(viewProvider: FileViewProvider): PsiFile = CypherFile(viewProvider)
}
