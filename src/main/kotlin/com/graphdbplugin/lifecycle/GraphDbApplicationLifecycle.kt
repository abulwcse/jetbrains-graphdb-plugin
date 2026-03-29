package com.graphdbplugin.lifecycle

import com.graphdbplugin.execution.Neo4jConnectionPool
import com.intellij.ide.AppLifecycleListener

/**
 * Listens for IDE lifecycle events and cleans up plugin resources on shutdown.
 *
 * Specifically, closes all pooled Neo4j [org.neo4j.driver.Driver] instances when
 * the IDE is closing. Without this cleanup the JVM may hang briefly at shutdown
 * waiting for driver background threads (Netty event loops) to terminate.
 *
 * Registered in `plugin.xml` as an application-level listener.
 */
class GraphDbApplicationLifecycle : AppLifecycleListener {

    /**
     * Called by the IntelliJ Platform just before the IDE process exits.
     * Delegates to [Neo4jConnectionPool.closeAll] to release all driver resources.
     */
    override fun appWillBeClosed(isRestart: Boolean) {
        Neo4jConnectionPool.closeAll()
    }
}
