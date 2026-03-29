package com.graphdbplugin

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Central registry of all SVG icons used by the GraphDB Plugin.
 *
 * Icons are loaded lazily via [IconLoader.getIcon], which handles HiDPI scaling,
 * dark-theme variants (`_dark` suffix convention), and caching automatically.
 *
 * Usage example:
 * ```kotlin
 * myLabel.icon = GraphDbPluginIcons.GRAPH_DB
 * ```
 *
 * All icon paths are relative to the plugin's resource root (i.e. the path
 * that would appear in `src/main/resources/`).
 */
object GraphDbPluginIcons {

    /**
     * Main plugin icon — used as the tool-window stripe icon and in
     * Marketplace listings. Depicts a simple graph/network topology.
     *
     * Source: `/icons/graphdb.svg`
     */
    @JvmField
    val GRAPH_DB: Icon = IconLoader.getIcon("/icons/graphdb.svg", GraphDbPluginIcons::class.java)

    /**
     * Data-source entry icon — used in the [com.graphdbplugin.toolwindow.DataSourceListCellRenderer]
     * next to each connection entry in the tool-window list. Depicts a
     * classic database cylinder.
     *
     * Source: `/icons/datasource.svg`
     */
    @JvmField
    val DATA_SOURCE: Icon = IconLoader.getIcon("/icons/datasource.svg", GraphDbPluginIcons::class.java)
}
