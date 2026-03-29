package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Project-level service that acts as the bridge between query execution and the result UI.
 *
 * ### Responsibilities
 * 1. Holds a reference to the [ResultPanel] once it is created by [ResultToolWindowFactory].
 * 2. Exposes [displayResult] which is called (on the EDT) after a query completes.
 * 3. Activates (shows) the "GraphDB Results" tool window when a result arrives.
 *
 * ### Usage
 * ```kotlin
 * ApplicationManager.getApplication().invokeLater {
 *     ResultToolWindowManager.getInstance(project).displayResult(result)
 * }
 * ```
 */
@Service(Service.Level.PROJECT)
class ResultToolWindowManager(private val project: Project) {

    /** Reference set by [ResultToolWindowFactory] when the tool window is first shown. */
    private var resultPanel: ResultPanel? = null

    /**
     * Called by [ResultToolWindowFactory] to register the panel after it is created.
     * @param panel The newly created [ResultPanel].
     */
    fun setResultPanel(panel: ResultPanel) {
        this.resultPanel = panel
    }

    /**
     * Forwards the current query text to the parameters panel so it can sync its fields.
     * Called from [com.graphdbplugin.editor.CypherFileEditor]'s document listener.
     */
    fun syncQueryParams(queryText: String) {
        resultPanel?.syncQueryParams(queryText)
    }

    /**
     * Returns the current parameter values from the parameters panel.
     * Falls back to an empty map if the tool window has not been initialised yet.
     */
    fun getParams(): Map<String, Any> = resultPanel?.getParams() ?: emptyMap()

    /**
     * Pushes a [QueryResult] to the UI and activates the result tool window.
     *
     * Must be called on the EDT.
     *
     * @param result The result of a Cypher query execution.
     */
    fun displayResult(result: QueryResult) {
        // Ensure the tool window is visible
        val tw = ToolWindowManager.getInstance(project).getToolWindow("GraphDB Results")
        tw?.show()

        resultPanel?.displayResult(result)
    }

    companion object {
        /** Returns the project-level [ResultToolWindowManager] service instance. */
        fun getInstance(project: Project): ResultToolWindowManager = project.service()
    }
}
