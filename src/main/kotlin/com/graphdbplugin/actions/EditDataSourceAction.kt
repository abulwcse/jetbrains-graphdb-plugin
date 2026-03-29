package com.graphdbplugin.actions

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.graphdbplugin.dialog.AddEditDataSourceDialog
import com.graphdbplugin.toolwindow.DataSourceTreePanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Action that opens the [AddEditDataSourceDialog] in "edit" mode for the
 * currently selected [BoltDataSource] and, upon confirmation, persists the
 * updated entry to [DataSourceManager] and refreshes the tool-window list.
 *
 * This action is registered in `plugin.xml` under the id `GraphDB.EditDataSource`
 * and is also wired directly into the [DataSourceTreePanel]'s toolbar.
 *
 * ### Selection resolution
 * The action first attempts to resolve the selected data source from the
 * [DataSourceTreePanel] component. If none is selected, the action is presented
 * as disabled (via [update]) so the user cannot trigger it without a valid
 * selection.
 *
 * ### Null-safety
 * If the action is invoked outside a project context, it is a no-op.
 */
class EditDataSourceAction : AnAction() {

    /**
     * Invoked when the user triggers the "Edit Data Source" action.
     *
     * Resolves the currently selected [BoltDataSource] from the tool-window panel.
     * If a selection exists, opens [AddEditDataSourceDialog] in edit mode. On OK:
     * 1. [DataSourceManager.updateDataSource] replaces the existing entry.
     * 2. The tool-window panel's list is refreshed.
     *
     * @param e The [AnActionEvent] providing project and component context.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = findPanel(e) ?: return
        val selected: BoltDataSource = panel.getSelectedDataSource() ?: return

        val dialog = AddEditDataSourceDialog(project, existingDataSource = selected)
        if (dialog.showAndGet()) {
            val updated = dialog.getResult()
            DataSourceManager.getInstance().updateDataSource(updated)
            panel.refreshList()
        }
    }

    /**
     * Disables the action when no data source is selected in the tool window
     * or when there is no active project.
     *
     * @param e The [AnActionEvent] providing context.
     */
    override fun update(e: AnActionEvent) {
        val panel = findPanel(e)
        e.presentation.isEnabled = panel?.hasSelection() == true
    }

    /**
     * Locates the [DataSourceTreePanel] within the "GraphDB" tool window for the
     * project associated with [e].
     *
     * @param e The action event whose project is used for the lookup.
     * @return The [DataSourceTreePanel], or `null` if the tool window or panel
     *         cannot be found.
     */
    private fun findPanel(e: AnActionEvent): DataSourceTreePanel? {
        val project = e.project ?: return null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GraphDB") ?: return null
        return toolWindow.contentManager.contents
            .mapNotNull { it.component as? DataSourceTreePanel }
            .firstOrNull()
    }
}
