package com.graphdbplugin.results

import com.graphdbplugin.execution.QueryResult
import com.mxgraph.layout.mxFastOrganicLayout
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxCellRenderer
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

class GraphVisualizationPanel : JPanel(BorderLayout()) {

    // ── Palette ───────────────────────────────────────────────────────────────
    companion object {
        private val LABEL_PALETTE = listOf(
            "#4A90D9", "#E74C3C", "#27AE60", "#F39C12", "#9B59B6",
            "#1ABC9C", "#E67E22", "#2ECC71", "#3498DB", "#E91E63"
        )
        private const val MAX_NODES_WARNING = 500

        private val BG_CANVAS  = Color(28,  28,  35)
        private val BG_TOOLBAR = Color(36,  36,  44)
        private val BG_LEGEND  = Color(32,  32,  40)
        private val FG_DIM     = Color(130, 130, 150)
        private val FG_TEXT    = Color(200, 200, 210)
    }

    // ── JGraphX ───────────────────────────────────────────────────────────────
    private val graph = mxGraph().apply {
        isAutoSizeCells   = true
        isCellsEditable   = false
        isCellsMovable    = true
        isCellsResizable  = false
        isEdgeLabelsMovable = false
        isGridEnabled     = true
        gridSize          = 20
    }

    private val graphComponent = mxGraphComponent(graph).apply {
        isConnectable    = false
        isDragEnabled    = false
        viewport.background = BG_CANVAS
        background          = BG_CANVAS
        graphControl.isDoubleBuffered = true
        // Enable built-in scroll bars so the user can scroll when zoomed in
        horizontalScrollBarPolicy = javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBarPolicy   = javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private val labelColorMap = mutableMapOf<String, String>()
    private var paletteIndex  = 0
    private val nodeDataStore  = mutableMapOf<Long, NodeData>()
    /** Direct cell-object → NodeData map; avoids fragile ID-cast lookups on double-click. */
    private val cellToNodeData = java.util.IdentityHashMap<Any, NodeData>()
    // tracks which label keys are in the *current* graph for legend
    private val currentLabels = mutableListOf<String>()

    // ── UI components ─────────────────────────────────────────────────────────
    private val infoLabel = JLabel(" ").apply {
        foreground = FG_TEXT
        font = font.deriveFont(Font.PLAIN, 12f)
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
    }

    private val legendPanel = JPanel().apply {
        background = BG_LEGEND
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, Color(50, 50, 62)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )
        preferredSize = Dimension(140, 0)
    }
    private val legendScroll = JScrollPane(legendPanel).apply {
        border = null
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        background = BG_LEGEND
        viewport.background = BG_LEGEND
        preferredSize = Dimension(140, 0)
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        background = BG_CANVAS

        val toolbar = buildToolbar()
        add(toolbar,      BorderLayout.NORTH)
        add(graphComponent, BorderLayout.CENTER)
        add(legendScroll, BorderLayout.EAST)

        installMouseHandlers()
    }

    private fun buildToolbar(): JPanel {
        fun styledBtn(text: String, tip: String, action: () -> Unit) = JButton(text).apply {
            toolTipText  = tip
            isFocusable  = false
            isOpaque     = false
            border       = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 70, 85), 1, true),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
            )
            foreground   = FG_TEXT
            background   = BG_TOOLBAR
            font         = font.deriveFont(Font.BOLD, 13f)
            addActionListener {
                action()
                graphComponent.refresh()
            }
        }

        val zoomOut   = styledBtn("−",  "Zoom out (Ctrl+−)")  { graphComponent.zoomOut() }
        val zoomReset = styledBtn("⊡",  "Fit to window")      { graphComponent.zoomAndCenter() }
        val zoomIn    = styledBtn("+",  "Zoom in (Ctrl++)")   { graphComponent.zoomIn() }

        val btnRow = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            isOpaque = false
            add(zoomOut); add(zoomReset); add(zoomIn)
        }

        return JPanel(BorderLayout()).apply {
            background = BG_TOOLBAR
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color(50, 50, 62)),
                BorderFactory.createEmptyBorder(5, 0, 5, 6)
            )
            add(infoLabel, BorderLayout.WEST)
            add(btnRow,    BorderLayout.EAST)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────
    fun displayResult(result: QueryResult.Success) {
        graph.model.beginUpdate()
        try {
            clearGraph()
            nodeDataStore.clear()
            cellToNodeData.clear()
            currentLabels.clear()

            val nodes = mutableListOf<NodeData>()
            val rels  = mutableListOf<RelData>()
            for (record in result.records)
                for (value in record.values())
                    collectGraphValues(value, nodes, rels)

            if (nodes.isEmpty() && rels.isEmpty()) {
                infoLabel.text = "  No graph data — use the Table tab for scalar results."
                rebuildLegend()
                return
            }

            val distinct = nodes.distinctBy { it.id }
            val count    = distinct.size

            infoLabel.text = if (count > MAX_NODES_WARNING)
                "  ⚠ Large graph ($count nodes) — layout may be slow."
            else
                "  $count node(s)   ${rels.distinctBy { it.id }.size} relationship(s)   ${result.durationMs} ms"

            val parent   = graph.defaultParent
            val nodeMap  = mutableMapOf<Long, Any>()
            val rng      = java.util.Random(42)
            val spread   = spreadFor(count)
            val nodeSize = nodeSizeFor(count)
            val fontSize = fontSizeFor(count)

            for (nd in distinct) {
                val labelKey = nd.labels.firstOrNull() ?: ""
                val color    = colorForLabel(labelKey)
                if (labelKey.isNotBlank() && labelKey !in currentLabels) currentLabels += labelKey
                val vertex = graph.insertVertex(
                    parent, nd.id.toString(), buildNodeLabel(nd),
                    rng.nextDouble() * spread, rng.nextDouble() * spread,
                    nodeSize, nodeSize, buildNodeStyle(color, fontSize)
                )
                nodeMap[nd.id]        = vertex
                nodeDataStore[nd.id]  = nd
                cellToNodeData[vertex] = nd
            }

            for (rel in rels.distinctBy { it.id }) {
                val src = nodeMap[rel.startId] ?: continue
                val dst = nodeMap[rel.endId]   ?: continue
                graph.insertEdge(parent, rel.id.toString(), rel.type, src, dst, EDGE_STYLE)
            }

            mxFastOrganicLayout(graph).apply {
                forceConstant      = 200.0
                maxIterations      = 500.0
                isDisableEdgeStyle = false
            }.execute(graph.defaultParent)

        } finally {
            graph.model.endUpdate()
        }

        graphComponent.setToolTips(true)
        rebuildLegend()
        // Defer fit-to-content until after the EDT has finished painting the layout
        SwingUtilities.invokeLater {
            graphComponent.zoomAndCenter()
            graphComponent.refresh()
        }
    }

    // ── Legend ────────────────────────────────────────────────────────────────
    private fun rebuildLegend() {
        legendPanel.removeAll()

        val title = JLabel("Node Types").apply {
            foreground = FG_DIM
            font = font.deriveFont(Font.BOLD, 11f)
            alignmentX = LEFT_ALIGNMENT
        }
        legendPanel.add(title)
        legendPanel.add(Box.createVerticalStrut(8))

        if (currentLabels.isEmpty()) {
            val empty = JLabel("—").apply {
                foreground = FG_DIM
                font = font.deriveFont(11f)
                alignmentX = LEFT_ALIGNMENT
            }
            legendPanel.add(empty)
        } else {
            for (label in currentLabels.sorted()) {
                val hex   = labelColorMap[label] ?: continue
                val color = runCatching { Color.decode(hex) }.getOrNull() ?: continue
                legendPanel.add(LegendRow(label, color))
                legendPanel.add(Box.createVerticalStrut(6))
            }
        }

        legendPanel.add(Box.createVerticalGlue())
        legendPanel.revalidate()
        legendPanel.repaint()
    }

    /** A single row in the legend: coloured dot + label name. */
    private inner class LegendRow(text: String, color: Color) : JPanel() {
        init {
            layout     = FlowLayout(FlowLayout.LEFT, 6, 0)
            isOpaque   = false
            maximumSize = Dimension(Int.MAX_VALUE, 22)
            alignmentX = LEFT_ALIGNMENT

            val dot = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = color
                    g2.fillOval(0, 1, 13, 13)
                    g2.color = color.darker()
                    g2.stroke = BasicStroke(1.2f)
                    g2.drawOval(0, 1, 13, 13)
                }
            }.apply {
                isOpaque = false
                preferredSize = Dimension(14, 15)
            }

            val lbl = JLabel(text).apply {
                foreground = FG_TEXT
                font = font.deriveFont(11f)
            }
            add(dot); add(lbl)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun clearGraph() =
        graph.removeCells(graph.getChildCells(graph.defaultParent, true, true))

    private fun collectGraphValues(
        value: org.neo4j.driver.Value,
        nodes: MutableList<NodeData>,
        rels:  MutableList<RelData>
    ) {
        when (value.type().name()) {
            "NODE" -> {
                val n = value.asNode()
                nodes += NodeData(n.id(), n.labels().toList(), n.asMap { if (it.isNull) "null" else it.asObject().toString() })
            }
            "RELATIONSHIP" -> {
                val r = value.asRelationship()
                rels += RelData(r.id(), r.startNodeId(), r.endNodeId(), r.type(), r.asMap { if (it.isNull) "null" else it.asObject().toString() })
            }
            "PATH" -> {
                value.asPath().nodes().forEach { n ->
                    nodes += NodeData(n.id(), n.labels().toList(), n.asMap { if (it.isNull) "null" else it.asObject().toString() })
                }
                value.asPath().relationships().forEach { r ->
                    rels += RelData(r.id(), r.startNodeId(), r.endNodeId(), r.type(), r.asMap { if (it.isNull) "null" else it.asObject().toString() })
                }
            }
            "LIST" -> value.asList().filterIsInstance<org.neo4j.driver.Value>()
                .forEach { collectGraphValues(it, nodes, rels) }
        }
    }

    private fun colorForLabel(label: String) =
        labelColorMap.getOrPut(label) { LABEL_PALETTE[paletteIndex++ % LABEL_PALETTE.size] }

    private fun buildNodeLabel(nd: NodeData): String {
        val fallback    = nd.labels.joinToString(":").ifBlank { "Node" }
        val displayName = nd.props.entries.firstOrNull { it.key.endsWith("name", ignoreCase = true) }?.value
            ?: nd.props.entries.firstOrNull { it.key.endsWith("id", ignoreCase = true) }?.value
            ?: fallback
        return truncate(displayName, 10)
    }

    private fun truncate(s: String, max: Int) =
        if (s.length > max) s.take(max) + "\u2026" else s

    private fun buildNodeStyle(hexColor: String, fontSize: Int) =
        "${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_ELLIPSE};" +
        "${mxConstants.STYLE_FILLCOLOR}=$hexColor;" +
        "${mxConstants.STYLE_STROKECOLOR}=${darken(hexColor)};" +
        "${mxConstants.STYLE_STROKEWIDTH}=2;" +
        "${mxConstants.STYLE_SHADOW}=1;" +
        "${mxConstants.STYLE_FONTCOLOR}=#FFFFFF;" +
        "${mxConstants.STYLE_FONTSTYLE}=1;" +
        "${mxConstants.STYLE_FONTSIZE}=$fontSize;" +
        "${mxConstants.STYLE_VERTICAL_ALIGN}=${mxConstants.ALIGN_MIDDLE};" +
        "${mxConstants.STYLE_ALIGN}=${mxConstants.ALIGN_CENTER};"

    private fun spreadFor(count: Int)   = (count * 55.0).coerceAtLeast(600.0)
    private fun nodeSizeFor(count: Int) = when {
        count <= 10  -> 100.0
        count <= 30  ->  88.0
        count <= 100 ->  72.0
        count <= 300 ->  56.0
        else         ->  44.0
    }
    private fun fontSizeFor(count: Int) = when {
        count <= 10  -> 14
        count <= 30  -> 12
        count <= 100 -> 11
        count <= 300 ->  9
        else         ->  8
    }

    private fun darken(hex: String) = try {
        val d = Color.decode(hex).darker()
        String.format("#%02x%02x%02x", d.red, d.green, d.blue)
    } catch (_: Exception) { hex }

    // ── Mouse / context menu ──────────────────────────────────────────────────
    private fun installMouseHandlers() {
        val popup      = JPopupMenu()
        val exportItem = JMenuItem("Export as PNG…")
        exportItem.addActionListener { exportAsPng() }
        popup.add(exportItem)

        graphComponent.graphControl.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popup.show(graphComponent, e.x, e.y)
            }

        })

        graphComponent.graphControl.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    val cell = graphComponent.getCellAt(e.x, e.y) ?: return
                    // Look up directly by cell object identity — no ID casting needed
                    val nd = cellToNodeData[cell] ?: return
                    showNodeProperties(nd)
                }
            }
        })

        // Ctrl+scroll zoom — JGraphX handles this natively; we just refresh after each step
        graphComponent.addMouseWheelListener { e ->
            if (e.isControlDown) {
                SwingUtilities.invokeLater { graphComponent.refresh() }
            }
        }
    }

    // ── Properties dialog ─────────────────────────────────────────────────────
    private fun showNodeProperties(nd: NodeData) {
        val typeLabel = nd.labels.joinToString(":").ifBlank { "Node" }

        val columnNames = arrayOf("Property", "Value")
        val rows: Array<Array<String>> = if (nd.props.isEmpty()) {
            arrayOf(arrayOf("(no properties)", ""))
        } else {
            nd.props.entries.sortedBy { it.key }
                .map { (k, v) -> arrayOf(k, v) }
                .toTypedArray()
        }

        val tableModel = object : javax.swing.table.DefaultTableModel(rows, columnNames) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        val table = com.intellij.ui.table.JBTable(tableModel).apply {
            setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN)
            tableHeader.reorderingAllowed = false
            rowHeight = 24
            columnModel.getColumn(0).preferredWidth = 140
            columnModel.getColumn(1).preferredWidth = 260
            // Wrap long values in the Value column
            columnModel.getColumn(1).cellRenderer = object : javax.swing.table.DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    t: javax.swing.JTable, value: Any?, sel: Boolean, foc: Boolean, row: Int, col: Int
                ): java.awt.Component {
                    val lbl = super.getTableCellRendererComponent(t, value, sel, foc, row, col)
                    toolTipText = value?.toString()
                    return lbl
                }
            }
        }

        val scroll = JScrollPane(table).apply {
            preferredSize = Dimension(460, minOf(40 + rows.size * 26, 360))
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        JOptionPane.showMessageDialog(this, scroll, "Node: $typeLabel", JOptionPane.PLAIN_MESSAGE)
    }

    // ── Export ────────────────────────────────────────────────────────────────
    private fun exportAsPng() {
        val chooser = JFileChooser().apply {
            dialogTitle  = "Export Graph as PNG"
            fileFilter   = FileNameExtensionFilter("PNG Images", "png")
            selectedFile = File("graph-export.png")
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val img: BufferedImage = mxCellRenderer.createBufferedImage(
                graph, null, 1.0, Color(28, 28, 35), true, null
            )
            ImageIO.write(img, "PNG", chooser.selectedFile)
        }
    }

    // ── Edge style ────────────────────────────────────────────────────────────
    private val EDGE_STYLE =
        "${mxConstants.STYLE_STARTARROW}=${mxConstants.NONE};" +
        "${mxConstants.STYLE_ENDARROW}=${mxConstants.ARROW_CLASSIC};" +
        "${mxConstants.STYLE_STROKECOLOR}=#7A7A9A;" +
        "${mxConstants.STYLE_STROKEWIDTH}=1.5;" +
        "${mxConstants.STYLE_FONTCOLOR}=#AAAACC;" +
        "${mxConstants.STYLE_FONTSIZE}=10;" +
        "curved=1;"

    // ── Data holders ──────────────────────────────────────────────────────────
    private data class NodeData(val id: Long, val labels: List<String>, val props: Map<String, String>)
    private data class RelData(val id: Long, val startId: Long, val endId: Long, val type: String, val props: Map<String, String>)
}
