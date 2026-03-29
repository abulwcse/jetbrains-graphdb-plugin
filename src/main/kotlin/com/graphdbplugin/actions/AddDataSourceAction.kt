package com.graphdbplugin.actions

import com.graphdbplugin.datasource.DataSourceManager
import com.graphdbplugin.dialog.AddEditDataSourceDialog
import com.graphdbplugin.toolwindow.DataSourceTreePanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Action that opens the [AddEditDataSourceDialog] in "add" mode and, upon
 * confirmation, persists the new [com.graphdbplugin.datasource.BoltDataSource]
 * to [DataSourceManager] and refreshes the tool-window list.
 *
 * This action is registered in `plugin.xml` under the id `GraphDB.AddDataSource`
 * and is also wired directly into the [DataSourceTreePanel]'s toolbar. It can
 * therefore be triggered from:
 * - The "+" button in the GraphDB tool window toolbar.
 * - The "Data Source Actions" action group (future: main menu / context menu).
 *
 * ### Null-safety
 * If the action is invoked outside a project context (e.g. from the Welcome
 * screen), `project` will be `null` and the action is a no-op.
 */
class AddDataSourceAction : AnAction() {

    /**
     * Invoked when the user triggers the "Add Data Source" action.
     *
     * Opens [AddEditDataSourceDialog] with `existingDataSource = null` (add mode).
     * If the user clicks OK:
     * 1. [DataSourceManager.addDataSource] is called to persist the new entry.
     * 2. The [DataSourceTreePanel] in the GraphDB tool window is located and its
     *    [DataSourceTreePanel.refreshList] method is called to update the UI.
     *
     * @param e The [AnActionEvent] providing context (project, component, etc.).
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dialog = AddEditDataSourceDialog(project, existingDataSource = null)
        if (dialog.showAndGet()) {
            val newSource = dialog.getResult()
            DataSourceManager.getInstance().addDataSource(newSource)
            refreshToolWindowPanel(e)
        }
    }

    /**
     * Controls when the action is visible and enabled.
     *
     * Disables the action on the Welcome screen (no project) so the button
     * appears grayed out rather than triggering a NullPointerException.
     *
     * @param e The [AnActionEvent] providing context.
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    /**
     * Locates the [DataSourceTreePanel] inside the "GraphDB" tool window and
     * triggers a list refresh.
     *
     * Uses [ToolWindowManager] to find the running tool window instance rather
     * than keeping a direct reference, which avoids memory leaks across project
     * open/close cycles.
     *
     * @param e The [AnActionEvent] whose project context is used for the lookup.
     */
    private fun refreshToolWindowPanel(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GraphDB") ?: return
        toolWindow.contentManager.contents.forEach { content ->
            (content.component as? DataSourceTreePanel)?.refreshList()
        }
    }
}
