package com.graphdbplugin.toolwindow

import com.graphdbplugin.services.SchemaIntrospectionService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action that forces a schema re-introspection for the currently selected data source.
 *
 * Appears as a "Refresh" button in the GraphDB tool window toolbar. Useful when the
 * Neo4j database schema changes (new labels, relationship types, or property keys are
 * added) and the editor's auto-completion cache needs to be updated.
 *
 * The refresh is performed asynchronously by [SchemaIntrospectionService.refreshSchema];
 * the action returns immediately and completion results update in the background.
 */
class SchemaRefreshAction : AnAction(
    "Refresh Schema",
    "Re-introspect the schema from the selected data source",
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val panel = e.getData(DataSourceTreePanel.DATA_KEY) ?: return
        val ds = panel.getSelectedDataSource() ?: return
        SchemaIntrospectionService.getInstance(project).refreshSchema(ds, project)
    }

    override fun update(e: AnActionEvent) {
        val panel = e.getData(DataSourceTreePanel.DATA_KEY)
        e.presentation.isEnabled = panel?.hasSelection() == true
    }
}
