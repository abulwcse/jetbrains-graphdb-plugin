package com.graphdbplugin.results

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory that creates the "GraphDB Results" tool window content.
 *
 * Registered in `plugin.xml` with `anchor="bottom"`. The tool window hosts a
 * [ResultPanel] which contains four tabs:
 * - Query Log
 * - Graph View
 * - Table View
 * - Parameters
 *
 * The tool window is shown automatically by [ResultToolWindowManager.displayResult]
 * when the first query result arrives.
 */
class ResultToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val resultPanel = ResultPanel()
        val content = ContentFactory.getInstance().createContent(resultPanel, "", false)
        toolWindow.contentManager.addContent(content)
        // Store reference in the project service so other components can push results
        ResultToolWindowManager.getInstance(project).setResultPanel(resultPanel)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
