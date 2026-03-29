package com.graphdbplugin.language.psi

import com.graphdbplugin.language.CypherFileType
import com.graphdbplugin.language.CypherLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * Root PSI element for a Cypher source file.
 *
 * The IntelliJ Platform creates one [CypherFile] per `.cypher` / `.cql` file
 * (or per [com.graphdbplugin.editor.CypherVirtualFile] scratch buffer). It acts
 * as the root of the PSI tree; all [CypherPsiElement] nodes and leaf tokens are
 * descendants of this node.
 *
 * [CypherFile] extends [PsiFileBase], which provides the standard PSI file
 * infrastructure (child iterators, element factory access, `findElementAt`, etc.)
 * while binding the file to [CypherLanguage] and [CypherFileType].
 *
 * ### Usage
 * You typically obtain a [CypherFile] via:
 * ```kotlin
 * val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? CypherFile
 * ```
 *
 * @param viewProvider The [FileViewProvider] that maps virtual-file content to PSI.
 */
class CypherFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, CypherLanguage) {

    /**
     * Returns the [CypherFileType] descriptor associated with this file.
     *
     * The platform calls this when it needs to display the file type icon,
     * look up the associated editor, or determine which language support to activate.
     *
     * @return [CypherFileType.INSTANCE].
     */
    override fun getFileType(): FileType = CypherFileType.INSTANCE

    /**
     * Returns a debug-friendly string representation of this node.
     *
     * Shown in the PSI Viewer and debug logs. The format deliberately omits the
     * file name to avoid revealing potentially sensitive path information in logs.
     *
     * @return The string `"CypherFile"`.
     */
    override fun toString(): String = "CypherFile"
}
