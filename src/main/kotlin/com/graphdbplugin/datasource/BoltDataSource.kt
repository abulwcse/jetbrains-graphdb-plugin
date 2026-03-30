package com.graphdbplugin.datasource

import java.util.UUID

/**
 * Immutable value object representing a single Neo4j/Bolt connection configuration.
 *
 * A [BoltDataSource] stores every piece of configuration needed to open a Bolt
 * driver session **except the password**, which is kept separately in the OS
 * keychain via [DataSourceManager.savePassword] / [DataSourceManager.getPassword].
 * This ensures that sensitive credentials are never serialised to the IDE's plain
 * XML state files on disk.
 *
 * Because this is a Kotlin `data class`, structural equality (`==`) compares all
 * fields including [id], so two instances with the same [id] but different [name]
 * values are **not** equal — this is intentional because it allows
 * [DataSourceManager.updateDataSource] to detect stale copies.
 *
 * @property id                    Universally unique identifier for this data source.
 *                                 Generated once at creation time and never changed.
 *                                 Used as the key for PasswordSafe credential lookup.
 * @property name                  Human-readable display name shown in the tool window,
 *                                 e.g. "Production DB" or "Local Dev Neo4j".
 * @property url                   Bolt connection URL, e.g. `bolt://localhost:7687` or
 *                                 `neo4j+s://myinstance.databases.neo4j.io`.
 *                                 Must start with `bolt://` or `neo4j://` (with optional
 *                                 `+s` / `+ssc` TLS variants).
 * @property username              Neo4j username; defaults to `"neo4j"` (the standard
 *                                 out-of-the-box account).
 * @property database              Neo4j database name to connect to; defaults to `"neo4j"`.
 *                                 Pass an empty string to use the server's default database.
 * @property sslEnabled            When `true`, the Bolt driver is configured to require
 *                                 TLS encryption. Note: `neo4j+s://` URLs always enforce
 *                                 TLS regardless of this flag.
 * @property connectionTimeoutSeconds
 *                                 Maximum number of seconds the driver waits when
 *                                 establishing a new TCP connection before throwing a
 *                                 timeout exception. Defaults to `30`.
 * @property color                 CSS hex colour string (e.g. `"#4A90D9"`) used to
 *                                 visually distinguish this data source in the tool window
 *                                 list and future query-editor tabs. Defaults to a calm
 *                                 blue to match the plugin's icon palette.
 */
data class BoltDataSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val url: String = "bolt://localhost:7687",
    val username: String = "neo4j",
    val database: String = "neo4j",
    val sslEnabled: Boolean = false,
    val connectionTimeoutSeconds: Int = 30,
    val color: String = "#4A90D9"
) {

    companion object {

        /**
         * Convenience factory method that creates a [BoltDataSource] with the given
         * [name] and [url], applying all other fields at their default values.
         *
         * A fresh [UUID] is assigned to [BoltDataSource.id] on every call, so two
         * invocations with identical arguments produce distinct instances.
         *
         * Example:
         * ```kotlin
         * val ds = BoltDataSource.create("Local Dev", "bolt://localhost:7687")
         * ```
         *
         * @param name Human-readable display name for the data source.
         * @param url  Bolt/Neo4j connection URL.
         * @return A new [BoltDataSource] with a unique [id] and default settings.
         */
        fun create(name: String, url: String): BoltDataSource =
            BoltDataSource(
                id = UUID.randomUUID().toString(),
                name = name,
                url = url
            )

        /**
         * Palette of predefined colours available in the
         * [com.graphdbplugin.dialog.AddEditDataSourceDialog] colour picker.
         *
         * The list is ordered to give a visually pleasing left-to-right gradient
         * from cool blues through warm reds and finally neutral pink/magenta.
         */
        val COLOR_PALETTE: List<String> = listOf(
            "#4A90D9", // blue (default)
            "#27AE60", // green
            "#E74C3C", // red
            "#E67E22", // orange
            "#8E44AD", // purple
            "#16A085", // teal
            "#F39C12", // yellow/amber
            "#E91E8C"  // pink/magenta
        )
    }
}
