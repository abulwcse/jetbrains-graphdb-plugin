package com.graphdbplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory responsible for constructing the "GraphDB" tool window's Swing content
 * when the user first opens it (or when the IDE starts and the window was open
 * in the previous session).
 *
 * The factory is registered in `plugin.xml` via the `<toolWindow>` extension:
 * ```xml
 * <toolWindow id="GraphDB" factoryClass="com.graphdbplugin.toolwindow.GraphDbToolWindowFactory" .../>
 * ```
 *
 * IntelliJ Platform instantiates this class lazily — the first time the tool
 * window is shown — so it is safe to perform lightweight UI initialisation here
 * without impacting IDE startup time.
 */
class GraphDbToolWindowFactory : ToolWindowFactory {

    /**
     * Called once by the platform to populate the tool window's content area.
     *
     * Creates a [DataSourceTreePanel] and wraps it in a [com.intellij.ui.content.Content]
     * tab labelled "Data Sources". The tab is added as the sole content pane of the
     * tool window.
     *
     * @param project    The [Project] that owns this tool window instance. Passed
     *                   through to [DataSourceTreePanel] so it can interact with
     *                   project-level services.
     * @param toolWindow The [ToolWindow] whose content area is being initialised.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dataSourcePanel = DataSourceTreePanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            dataSourcePanel,
            "Data Sources",
            /* isLockable = */ false
        )
        toolWindow.contentManager.addContent(content)
    }
}
