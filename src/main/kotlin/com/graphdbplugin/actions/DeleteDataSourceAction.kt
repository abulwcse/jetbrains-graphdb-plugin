package com.graphdbplugin.actions

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.graphdbplugin.toolwindow.DataSourceTreePanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Action that prompts the user for confirmation and then permanently deletes the
 * currently selected [BoltDataSource] from [DataSourceManager].
 *
 * This action is registered in `plugin.xml` under the id `GraphDB.DeleteDataSource`
 * and is wired into the [DataSourceTreePanel]'s toolbar.
 *
 * ### Deletion effects
 * [DataSourceManager.removeDataSource] is called which:
 * 1. Removes the [BoltDataSource] entry from the persisted XML state.
 * 2. Clears the data source's password from the OS native keychain via
 *    `PasswordSafe`, so no orphaned credentials are left behind.
 *
 * ### Confirmation dialog
 * A [Messages.showYesNoDialog] is shown before deletion. The user must explicitly
 * click "Delete" (the YES button) to proceed. Pressing Cancel or closing the dialog
 * aborts the operation.
 *
 * ### Null-safety
 * If there is no active project or no selected data source, the action is a no-op
 * (and is presented as disabled via [update]).
 */
class DeleteDataSourceAction : AnAction() {

    /**
     * Invoked when the user triggers the "Delete Data Source" action.
     *
     * 1. Resolves the selected [BoltDataSource] from the [DataSourceTreePanel].
     * 2. Shows a confirmation dialog warning that the operation is irreversible.
     * 3. On confirmation, calls [DataSourceManager.removeDataSource] and refreshes
     *    the tool-window list.
     *
     * @param e The [AnActionEvent] providing project and component context.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = findPanel(e) ?: return
        val selected: BoltDataSource = panel.getSelectedDataSource() ?: return

        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete \u2018${selected.name}\u2019?\nThis action cannot be undone.",
            "Delete Data Source",
            "Delete",
            "Cancel",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            DataSourceManager.getInstance().removeDataSource(selected.id)
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
     * @param e The action event whose project is used for the tool-window lookup.
     * @return The [DataSourceTreePanel], or `null` if the tool window is not
     *         currently open or the panel component cannot be found.
     */
    private fun findPanel(e: AnActionEvent): DataSourceTreePanel? {
        val project = e.project ?: return null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GraphDB") ?: return null
        return toolWindow.contentManager.contents
            .mapNotNull { it.component as? DataSourceTreePanel }
            .firstOrNull()
    }
}
