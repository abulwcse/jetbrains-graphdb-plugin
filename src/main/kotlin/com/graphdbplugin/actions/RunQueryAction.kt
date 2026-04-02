package com.graphdbplugin.actions

import com.graphdbplugin.editor.CypherFileEditor
import com.graphdbplugin.execution.QueryExecutor
import com.graphdbplugin.execution.QueryResult
import com.graphdbplugin.results.ResultToolWindowManager
import com.graphdbplugin.services.SchemaIntrospectionService
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.openapi.fileEditor.FileEditorManager

/**
 * Action that executes the Cypher query in the currently focused [CypherFileEditor]
 * against the selected data source via the Neo4j Bolt driver.
 *
 * Registered in `plugin.xml` inside the `GraphDB.DataSourceActions` group with
 * keyboard shortcuts Ctrl+F5 (Windows/Linux) and Meta+F5 (macOS):
 * ```xml
 * <action id="GraphDB.RunQuery"
 *         class="com.graphdbplugin.actions.RunQueryAction"
 *         text="Run Query"
 *         description="Execute the Cypher query against the selected data source"
 *         icon="AllIcons.Actions.Execute">
 *     <keyboard-shortcut first-keystroke="control F5" keymap="${'$'}default"/>
 *     <keyboard-shortcut first-keystroke="meta F5"    keymap="Mac OS X 10.5+"/>
 * </action>
 * ```
 *
 * ### Phase 4 behaviour
 * [actionPerformed] performs the following steps:
 * 1. Locates the focused [CypherFileEditor] via [FileEditorManager].
 * 2. Reads the Cypher query text and validates that it is non-blank.
 * 3. Reads the selected [com.graphdbplugin.datasource.BoltDataSource] from the editor toolbar.
 * 4. Retrieves query parameters from the "Parameters" tab of the Results panel as JSON,
 *    then parses the JSON object into a `Map<String, Any>` using [ObjectMapper].
 * 5. Shows an "Executing query…" balloon notification so the user has immediate feedback.
 * 6. Submits query execution to a background thread via [AppExecutorUtil.getAppExecutorService].
 * 7. On completion (success or failure), dispatches back to the EDT and calls
 *    [ResultToolWindowManager.displayResult] to push the result to the bottom panel.
 *
 * ### Availability
 * The action is enabled only when the currently focused editor is a [CypherFileEditor].
 * The [update] method disables the action (and greys out the toolbar button) when any
 * other editor type is focused.
 *
 * ### Thread safety
 * Query execution happens on a pooled background thread. All UI interactions (showing
 * notifications, calling [ResultToolWindowManager.displayResult]) are marshalled back
 * to the EDT via [ApplicationManager.getApplication().invokeLater].
 */
class RunQueryAction : AnAction(
    "Run Query",
    "Execute the Cypher query against the selected data source",
    AllIcons.Actions.Execute
) {

    /**
     * Executes the Cypher query from the currently focused [CypherFileEditor].
     *
     * ### Execution flow
     * 1. Obtains the project from the action event; returns silently if unavailable.
     * 2. Casts the focused editor to [CypherFileEditor]; returns if it is not one.
     * 3. Reads and trims the query text; shows an "empty query" balloon if blank.
     * 4. Reads the selected data source from the editor's combo box.
     * 5. Reads the parameters JSON from the Results panel's Parameters tab; parses it
     *    to a `Map<String, Any>`. Falls back to `emptyMap()` if parsing fails.
     * 6. Posts an "Executing…" balloon notification.
     * 7. Submits a background task that calls [QueryExecutor.execute], then marshals
     *    the [com.graphdbplugin.execution.QueryResult] back to the EDT and forwards it
     *    to [ResultToolWindowManager.displayResult].
     *
     * @param e The [AnActionEvent] carrying project context and presentation state.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. Locate the Cypher editor
        val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
        val cypherEditor = selectedEditor as? CypherFileEditor ?: return

        // 2. Read and validate query text
        val queryText = cypherEditor.getQueryText().trim()
        if (queryText.isBlank()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("GraphDB.Notifications")
                ?.createNotification(
                    "Run Query",
                    "Query is empty. Type a Cypher query and try again.",
                    NotificationType.WARNING
                )
                ?.notify(project)
            return
        }

        // 3. Read the selected data source
        val dataSource = cypherEditor.getSelectedDataSource()

        // 4. Read parameters directly from the dynamic Parameters panel
        val params: Map<String, Any?> = ResultToolWindowManager.getInstance(project).getParams()

        // 5. Notify user that execution has started
        NotificationGroupManager.getInstance()
            .getNotificationGroup("GraphDB.Notifications")
            ?.createNotification(
                "Run Query",
                "Executing query against <b>${dataSource.name}</b>…",
                NotificationType.INFORMATION
            )
            ?.notify(project)

        // 6. Execute on a background thread
        AppExecutorUtil.getAppExecutorService().submit {
            val result = QueryExecutor.execute(dataSource, queryText, params)

            // 7. Push result back to EDT → Results tool window
            ApplicationManager.getApplication().invokeLater {
                ResultToolWindowManager.getInstance(project).displayResult(result)

                // Refresh schema cache on first successful connection so that
                // label/relationship-type completions become available immediately.
                if (result is QueryResult.Success) {
                    SchemaIntrospectionService.getInstance(project)
                        .refreshSchema(dataSource, project)
                }
            }
        }
    }

    /**
     * Updates the action's enabled/disabled state based on the focused editor.
     *
     * The action is enabled only when the currently selected editor in [FileEditorManager]
     * is a [CypherFileEditor]. This prevents the Run Query button from being clickable
     * (or the keyboard shortcut from firing) when a non-Cypher editor is focused.
     *
     * @param e The [AnActionEvent] whose [com.intellij.openapi.actionSystem.Presentation]
     *          is updated.
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }
        val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
        e.presentation.isEnabled = selectedEditor is CypherFileEditor
    }
}
