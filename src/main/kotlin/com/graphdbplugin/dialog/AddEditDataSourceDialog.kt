package com.graphdbplugin.dialog

import com.graphdbplugin.datasource.BoltDataSource
import com.graphdbplugin.datasource.DataSourceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Modal dialog for adding a new [BoltDataSource] or editing an existing one.
 *
 * ### Modes
 * - **Add mode** (`existingDataSource == null`): The title reads "Add Data Source"
 *   and all fields are initialised with sensible defaults.
 * - **Edit mode** (`existingDataSource != null`): The title reads "Edit Data Source"
 *   and all fields are pre-populated from the existing data source's values. The
 *   password field is left blank — the user only needs to enter a new password if
 *   they want to change it.
 *
 * ### Password handling
 * The password is **not** stored in [BoltDataSource]. After the user clicks OK,
 * the caller should invoke [getResult] to obtain the data source, then separately
 * call [DataSourceManager.savePassword] if the password field is non-empty. This
 * dialog handles the save internally in [doOKAction] for convenience.
 *
 * ### Validation
 * [doOKAction] validates all required fields before closing. Each validation
 * failure is returned as a [ValidationInfo] which the platform displays as an
 * inline error balloon next to the offending field.
 *
 * @param project            The [Project] that owns this dialog (used as the parent
 *                           for the dialog window and for notifications).
 * @param existingDataSource Existing [BoltDataSource] to edit, or `null` to create
 *                           a new one.
 */
class AddEditDataSourceDialog(
    private val project: Project,
    private val existingDataSource: BoltDataSource?
) : DialogWrapper(project) {

    // =========================================================================
    // Form fields
    // =========================================================================

    /** Text field for the user-facing display name of the data source. */
    private val nameField = JBTextField().apply {
        emptyText.text = "e.g. Local Dev Neo4j"
        existingDataSource?.let { text = it.name }
    }

    /** Text field for the Bolt connection URL. */
    private val urlField = JBTextField().apply {
        emptyText.text = "bolt://localhost:7687"
        text = existingDataSource?.url ?: "bolt://localhost:7687"
    }

    /** Text field for the Neo4j username. */
    private val usernameField = JBTextField().apply {
        emptyText.text = "neo4j"
        text = existingDataSource?.username ?: "neo4j"
    }

    /**
     * Password field. Intentionally left blank in edit mode — the user enters a
     * new password only if they wish to change the stored one.
     */
    private val passwordField = JBPasswordField()

    /** Text field for the target database name. */
    private val databaseField = JBTextField().apply {
        emptyText.text = "neo4j"
        text = existingDataSource?.database ?: "neo4j"
    }

    /** Checkbox to enable TLS/SSL encryption on the Bolt connection. */
    private val sslCheckBox = JBCheckBox("Enable SSL/TLS").apply {
        isSelected = existingDataSource?.sslEnabled ?: false
    }

    /**
     * Text field for the TCP connection timeout in seconds. Accepts integer input.
     */
    private val timeoutField = JBTextField().apply {
        emptyText.text = "30"
        text = (existingDataSource?.connectionTimeoutSeconds ?: 30).toString()
        preferredSize = Dimension(60, preferredSize.height)
    }

    // =========================================================================
    // Colour picker state
    // =========================================================================

    /** Currently selected colour hex string. Initialised from existing or default. */
    private var selectedColor: String = existingDataSource?.color ?: BoltDataSource.COLOR_PALETTE[0]

    /** The set of colour swatch panels keyed by their hex colour string. */
    private val colorSwatches: MutableMap<String, ColorSwatchPanel> = mutableMapOf()

    // =========================================================================
    // Test connection widgets
    // =========================================================================

    /** "Test Connection" button in the lower section of the form. */
    private val testConnectionButton = JButton("Test Connection")

    /**
     * Status label updated after a test connection attempt. Shows a green tick
     * on success or a red cross with the error message on failure.
     */
    private val statusLabel = JBLabel("").apply {
        border = JBUI.Borders.emptyTop(4)
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    init {
        title = if (existingDataSource == null) "Add Data Source" else "Edit Data Source"
        init()
        testConnectionButton.addActionListener { performTestConnection() }
    }

    // =========================================================================
    // DialogWrapper overrides
    // =========================================================================

    /**
     * Builds and returns the main content panel displayed inside the dialog.
     *
     * Uses IntelliJ's [FormBuilder] to create a labelled two-column grid layout
     * that matches the platform's standard form styling.
     *
     * @return The root [JComponent] for the dialog's content area.
     */
    override fun createCenterPanel(): JComponent {
        val colorPickerPanel = buildColorPickerPanel()

        val testConnectionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(testConnectionButton)
            add(statusLabel)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Name:"), nameField, /* topGap = */ 1, /* labelOnTop = */ false)
            .addLabeledComponent(JBLabel("URL:"), urlField, 1, false)
            .addLabeledComponent(JBLabel("Username:"), usernameField, 1, false)
            .addLabeledComponent(JBLabel("Password:"), passwordField, 1, false)
            .addLabeledComponent(JBLabel("Database:"), databaseField, 1, false)
            .addComponent(sslCheckBox, 4)
            .addLabeledComponent(JBLabel("Connection Timeout (s):"), timeoutField, 1, false)
            .addLabeledComponent(JBLabel("Color:"), colorPickerPanel, 4, false)
            .addComponent(testConnectionPanel, 8)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        form.border = JBUI.Borders.empty(8)
        return form
    }

    /**
     * Validates all form fields before the dialog is allowed to close.
     *
     * Returns a non-null [ValidationInfo] for the first field that fails
     * validation, which the platform renders as an inline error balloon.
     *
     * Validation rules:
     * - Name must not be blank.
     * - URL must not be blank and must begin with `bolt://` or `neo4j://`.
     * - Username must not be blank.
     * - Connection timeout must be a positive integer.
     *
     * @return `null` if all fields are valid; a [ValidationInfo] describing
     *         the first error otherwise.
     */
    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("Name is required.", nameField)
        }
        val url = urlField.text.trim()
        if (url.isBlank()) {
            return ValidationInfo("URL is required.", urlField)
        }
        if (!url.startsWith("bolt://") && !url.startsWith("neo4j://") &&
            !BoltDataSource.hasTlsScheme(url)) {
            return ValidationInfo(
                "URL must start with bolt://, bolt+s://, bolt+ssc://, neo4j://, neo4j+s://, or neo4j+ssc://.",
                urlField
            )
        }
        if (usernameField.text.isBlank()) {
            return ValidationInfo("Username is required.", usernameField)
        }
        val timeoutText = timeoutField.text.trim()
        if (timeoutText.isBlank() || timeoutText.toIntOrNull()?.let { it <= 0 } != false) {
            return ValidationInfo("Connection timeout must be a positive integer.", timeoutField)
        }
        return null
    }

    /**
     * Persists the form data when the user clicks OK.
     *
     * After passing validation, calls [DataSourceManager.savePassword] to store
     * the entered password in the OS keychain (if the password field is non-empty).
     * Then calls the superclass implementation which closes the dialog.
     */
    override fun doOKAction() {
        // doValidate is called automatically by the platform; we only reach here
        // if validation passed.
        // Build and cache the result so getResult() always returns the same UUID.
        cachedResult = BoltDataSource(
            id = existingDataSource?.id ?: java.util.UUID.randomUUID().toString(),
            name = nameField.text.trim(),
            url = urlField.text.trim(),
            username = usernameField.text.trim(),
            database = databaseField.text.trim().ifBlank { "neo4j" },
            sslEnabled = sslCheckBox.isSelected,
            connectionTimeoutSeconds = timeoutField.text.trim().toIntOrNull() ?: 30,
            color = selectedColor
        )
        val password = String(passwordField.password)
        if (password.isNotBlank()) {
            DataSourceManager.getInstance().savePassword(cachedResult!!.id, password)
        }
        super.doOKAction()
    }

    // =========================================================================
    // Public API
    // =========================================================================

    // Cached result — built once in doOKAction so the UUID is stable across calls.
    private var cachedResult: BoltDataSource? = null

    /**
     * Returns the [BoltDataSource] built from the form values when the user clicked OK.
     *
     * The result is created once (in [doOKAction]) so that the UUID is stable — calling
     * this method multiple times always returns the same object. This ensures the
     * password saved under the UUID in [doOKAction] matches the UUID the caller stores.
     *
     * This method should only be called **after** [showAndGet] returns `true`.
     *
     * @return A [BoltDataSource] reflecting the confirmed form values.
     */
    fun getResult(): BoltDataSource = cachedResult
        ?: BoltDataSource(
            id = existingDataSource?.id ?: java.util.UUID.randomUUID().toString(),
            name = nameField.text.trim(),
            url = urlField.text.trim(),
            username = usernameField.text.trim(),
            database = databaseField.text.trim().ifBlank { "neo4j" },
            sslEnabled = sslCheckBox.isSelected,
            connectionTimeoutSeconds = timeoutField.text.trim().toIntOrNull() ?: 30,
            color = selectedColor
        )

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Builds the colour-picker row consisting of 8 clickable coloured square panels.
     *
     * Clicking a swatch updates [selectedColor] and repaints all swatches to show
     * the selection border.
     *
     * @return A [JPanel] containing the colour swatches in a horizontal flow layout.
     */
    private fun buildColorPickerPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))

        for (hex in BoltDataSource.COLOR_PALETTE) {
            val swatch = ColorSwatchPanel(hex, isSelected = (hex == selectedColor))
            swatch.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            swatch.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    selectedColor = hex
                    colorSwatches.values.forEach { it.setSelected(it.colorHex == hex) }
                }
            })
            colorSwatches[hex] = swatch
            panel.add(swatch)
        }

        return panel
    }

    /**
     * Performs a test connection attempt against the URL/credentials currently
     * entered in the form, then updates [statusLabel] with the result.
     *
     * The connection attempt is run on a background thread to avoid blocking the
     * EDT. The status label is updated back on the EDT via [SwingUtilities.invokeLater].
     */
    private fun performTestConnection() {
        statusLabel.text = "Connecting\u2026"
        testConnectionButton.isEnabled = false

        val url = urlField.text.trim()
        val username = usernameField.text.trim()
        val password = String(passwordField.password).let { pw ->
            if (pw.isNotBlank()) pw
            else {
                // Fall back to the stored password for the existing data source (edit mode).
                existingDataSource?.let { DataSourceManager.getInstance().getPassword(it.id) } ?: ""
            }
        }
        val database = databaseField.text.trim().ifBlank { "neo4j" }
        val ssl = sslCheckBox.isSelected
        val timeoutSecs = timeoutField.text.trim().toIntOrNull() ?: 30

        Thread {
            val result = testBoltConnection(url, username, password, database, ssl, timeoutSecs)
            SwingUtilities.invokeLater {
                if (result == null) {
                    statusLabel.text = "<html><font color='#27AE60'>\u2713 Connected successfully.</font></html>"
                } else {
                    statusLabel.text = "<html><font color='#E74C3C'>\u2717 Connection failed: $result</font></html>"
                }
                testConnectionButton.isEnabled = true
            }
        }.also { it.isDaemon = true }.start()
    }

    /**
     * Attempts to open a Bolt driver session to the given [url] with the given
     * credentials and immediately runs a `RETURN 1` ping query.
     *
     * @param url        Bolt/Neo4j URL.
     * @param username   Neo4j username.
     * @param password   Neo4j password.
     * @param database   Neo4j database name.
     * @param ssl        Whether to require TLS.
     * @param timeoutSec TCP connection timeout in seconds.
     * @return `null` if the connection succeeded, or an error message string if it failed.
     */
    private fun testBoltConnection(
        url: String,
        username: String,
        password: String,
        database: String,
        ssl: Boolean,
        timeoutSec: Int
    ): String? {
        return try {
            val config = org.neo4j.driver.Config.builder()
                .withConnectionTimeout(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                .apply {
                    if (!BoltDataSource.hasTlsScheme(url)) {
                        if (ssl) withEncryption() else withoutEncryption()
                    }
                }
                .build()
            val authToken = org.neo4j.driver.AuthTokens.basic(username, password)
            org.neo4j.driver.GraphDatabase.driver(url, authToken, config).use { driver ->
                driver.session(
                    org.neo4j.driver.SessionConfig.builder()
                        .withDatabase(database)
                        .build()
                ).use { session ->
                    session.run("RETURN 1").consume()
                }
            }
            null // success
        } catch (ex: Exception) {
            ex.message ?: ex.javaClass.simpleName
        }
    }

    // =========================================================================
    // Inner class: colour swatch panel
    // =========================================================================

    /**
     * A fixed-size [JPanel] that paints itself as a filled rounded rectangle in
     * the given [colorHex] colour. When [isSelected] is `true` a white selection
     * ring is painted around the swatch.
     *
     * @property colorHex  Hex colour string for this swatch (e.g. `"#4A90D9"`).
     * @param    isSelected Whether this swatch is initially in the selected state.
     */
    private inner class ColorSwatchPanel(
        val colorHex: String,
        isSelected: Boolean
    ) : JPanel() {

        private var selected: Boolean = isSelected
        private val swatchColor: Color = try {
            Color.decode(colorHex)
        } catch (_: NumberFormatException) {
            Color(74, 144, 217)
        }

        init {
            preferredSize = Dimension(20, 20)
            minimumSize = Dimension(20, 20)
            maximumSize = Dimension(20, 20)
            isOpaque = false
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        }

        /**
         * Updates the selection state and repaints this swatch.
         *
         * @param selected `true` to mark this swatch as the active colour choice.
         */
        fun setSelected(selected: Boolean) {
            this.selected = selected
            repaint()
        }

        /**
         * Paints the coloured rounded rectangle and, when selected, an outer ring.
         *
         * @param g The [Graphics] context provided by Swing.
         */
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val inset = if (selected) 2 else 1
                g2.color = swatchColor
                g2.fillRoundRect(inset, inset, width - inset * 2, height - inset * 2, 4, 4)
                if (selected) {
                    g2.color = Color.WHITE
                    g2.drawRoundRect(1, 1, width - 3, height - 3, 4, 4)
                    g2.color = swatchColor.darker()
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 4, 4)
                } else {
                    g2.color = swatchColor.darker()
                    g2.drawRoundRect(inset, inset, width - inset * 2 - 1, height - inset * 2 - 1, 4, 4)
                }
            } finally {
                g2.dispose()
            }
        }
    }
}
