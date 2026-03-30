package com.graphdbplugin.language.psi

import com.graphdbplugin.language.CypherLanguage
import com.intellij.psi.tree.IElementType

/**
 * Base element type for all composite (non-leaf) Cypher PSI nodes.
 *
 * All [CypherCompositeElementTypes] entries extend this class so that
 * [com.graphdbplugin.language.parser.CypherParserDefinition.createElement] can
 * uniformly wrap any composite AST node in a [CypherPsiElement].
 *
 * The [debugName] is the string shown in the PSI Viewer (Tools → PSI Viewer) when
 * inspecting the syntax tree of a `.cypher` file.
 *
 * @param debugName The human-readable name for this element type, used in debug
 *                  output and the PSI viewer. Must be unique among Cypher element types.
 */
open class CypherElementType(debugName: String) : IElementType(debugName, CypherLanguage.INSTANCE)
