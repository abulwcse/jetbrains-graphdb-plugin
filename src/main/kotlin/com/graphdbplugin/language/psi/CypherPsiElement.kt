package com.graphdbplugin.language.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

/**
 * Generic PSI element for non-file Cypher AST nodes.
 *
 * The [com.graphdbplugin.language.parser.CypherParserDefinition.createElement] factory
 * method creates instances of this class for all composite AST nodes whose type is
 * a [CypherElementType]. As the Phase 2 parser builds a flat tree, these are not
 * actually produced yet — but the class is required by the [ParserDefinition] contract
 * and will be used by the full grammar parser in Phase 5.
 *
 * [ASTWrapperPsiElement] provides all standard [com.intellij.psi.PsiElement] methods
 * (navigation, text range, parent/child traversal) by delegating to the underlying
 * [ASTNode], so no additional implementation is needed for the basic case.
 *
 * @param node The underlying [ASTNode] from the syntax tree.
 */
class CypherPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)
