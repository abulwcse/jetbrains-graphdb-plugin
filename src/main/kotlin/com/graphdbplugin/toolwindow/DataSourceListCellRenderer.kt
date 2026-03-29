package com.graphdbplugin.toolwindow

import com.graphdbplugin.datasource.BoltDataSource
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JList

/**
 * Custom list cell renderer for [BoltDataSource] entries in the tool-window list.
 *
 * Each row is rendered as:
 * ```
 * [■] <bold name>  <gray url>  [neo4j]
 *  ^         ^          ^          ^
 *  colour   name        url    database badge
 *  swatch
 * ```
 *
 * The coloured square on the left is synthesised as an [Icon] from the data
 * source's [BoltDataSource.color] hex string, allowing users to visually distinguish
 * connections at a glance (e.g. red = production, green = local dev).
 *
 * The database name is rendered as a small gray badge to the right of the URL.
 *
 * Extends [ColoredListCellRenderer] from the IntelliJ Platform, which provides
 * consistent theming (selection colours, focus borders, HiDPI support) and the
 * [append] API for building styled text fragments.
 */
class DataSourceListCellRenderer(
    private val connectionStates: Map<String, ConnectionState> = emptyMap()
) : ColoredListCellRenderer<BoltDataSource>() {

    /**
     * Populates the renderer's styled text fragments and icon for a single
     * [BoltDataSource] list cell.
     *
     * @param list     The owning [JList] (used for selection/focus colour lookups
     *                 by the parent [ColoredListCellRenderer]).
     * @param value    The [BoltDataSource] to render; never `null` for non-empty lists.
     * @param index    Zero-based row index within the list.
     * @param selected Whether this row is currently selected.
     * @param hasFocus Whether this row has keyboard focus.
     */
    override fun customizeCellRenderer(
        list: JList<out BoltDataSource>,
        value: BoltDataSource,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        // ----------------------------------------------------------------
        // Icon — a small coloured square painted from the data source colour
        // ----------------------------------------------------------------
        val ready = connectionStates[value.id] == ConnectionState.READY
        icon = ColorSwatchIcon(parseColor(value.color), dimmed = !ready)

        // Name — greyed out until the connection is verified
        val nameStyle = if (connectionStates[value.id] == ConnectionState.READY)
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
        else
            SimpleTextAttributes.GRAYED_ATTRIBUTES
        append(value.name, nameStyle)

        // Tooltip shows full details + connection status on hover
        toolTipText = buildTooltip(value, connectionStates[value.id])
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Parses a CSS hex colour string (e.g. `"#4A90D9"`) into a [Color].
     *
     * Falls back to a neutral blue if the string cannot be parsed.
     *
     * @param hex Hex colour string, with or without the leading `#`.
     * @return The parsed [Color], or `Color(74, 144, 217)` on parse failure.
     */
    private fun parseColor(hex: String): Color {
        return try {
            Color.decode(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: NumberFormatException) {
            Color(74, 144, 217) // fallback: default blue
        }
    }

    /**
     * Builds an HTML tooltip string summarising all fields of a [BoltDataSource].
     *
     * @param ds The data source to describe.
     * @return An HTML-formatted tooltip string.
     */
    private fun buildTooltip(ds: BoltDataSource, state: ConnectionState?): String {
        val statusLine = when (state) {
            ConnectionState.CHECKING -> "<font color='gray'>⟳ Connecting…</font>"
            ConnectionState.READY    -> "<font color='#27ae60'>✓ Connected</font>"
            ConnectionState.FAILED   -> "<font color='#e74c3c'>✗ Connection failed</font>"
            null                     -> "<font color='gray'>⟳ Connecting…</font>"
        }
        return buildString {
            append("<html><b>${ds.name}</b><br/>")
            append("URL: ${ds.url}<br/>")
            append("User: ${ds.username}<br/>")
            append("Database: ${ds.database}<br/>")
            append("SSL: ${if (ds.sslEnabled) "enabled" else "disabled"}<br/>")
            append("Timeout: ${ds.connectionTimeoutSeconds}s<br/>")
            append("Status: $statusLine")
            append("</html>")
        }
    }

    // =========================================================================
    // Inner class: small coloured square icon
    // =========================================================================

    /**
     * A simple [Icon] implementation that paints a 12x12 filled rounded rectangle
     * in the given [color].
     *
     * Used as the leading icon in each data-source list cell to provide a quick
     * visual identification cue without requiring per-datasource image files.
     *
     * @property color The fill colour for the swatch.
     */
    private class ColorSwatchIcon(private val color: Color, private val dimmed: Boolean = false) : Icon {

        override fun getIconWidth(): Int = 12
        override fun getIconHeight(): Int = 12

        /**
         * Paints the coloured rounded rectangle onto [g].
         *
         * @param c The component being painted (used only for Graphics context).
         * @param g The [Graphics] context to paint on.
         * @param x Horizontal offset of the icon's top-left corner.
         * @param y Vertical offset of the icon's top-left corner.
         */
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = if (dimmed) Color(color.red, color.green, color.blue, 70) else color
                g2.fillRoundRect(x, y, iconWidth, iconHeight, 3, 3)
                g2.color = if (dimmed) Color(128, 128, 128, 70) else color.darker()
                g2.drawRoundRect(x, y, iconWidth - 1, iconHeight - 1, 3, 3)
            } finally {
                g2.dispose()
            }
        }
    }
}
