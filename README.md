# GraphDB for Neo4j

A JetBrains IDE plugin for working with Neo4j graph databases directly from your editor.
Write Cypher, manage connections, run queries, and explore results as a graph or table — without leaving the IDE.

> **Found a bug or have a suggestion?**
> [Open an issue on GitHub](https://github.com/abulwcse/jetbrains-graphdb-plugin/issues/new) — it's the fastest way to get it resolved.

---
## Features

| | |
|---|---|
| **Cypher editor** | Syntax highlighting, keyword auto-completion, bracket and parameter annotations |
| **Data source management** | Add, edit and delete Bolt connections; passwords stored securely in the OS keychain |
| **Connection health check** | Automatic connection verification on startup; sources are greyed out until ready |
| **Query execution** | Run Cypher from the editor with a single action (`Ctrl+F5` / `Cmd+F5`) |
| **Graph visualisation** | Force-directed graph with per-type node colouring, legend panel, zoom controls and dynamic sizing |
| **Node properties** | Double-click any node to inspect all its properties in a table |
| **Terminal log** | Chronological dark-theme query log with timestamps, row counts, duration and error details |
| **Table results** | Scrollable data grid for scalar and list results |
| **Query parameters** | Bind `$param` values before execution via a JSON editor |
| **Export** | Export the current graph view as a PNG image |

---

## Getting Started

### 1 — Install

**From JetBrains Marketplace**
`Settings / Preferences` → `Plugins` → search **GraphDB for Neo4j** → Install → Restart IDE

**From a downloaded zip**
`Settings / Preferences` → `Plugins` → ⚙ → `Install Plugin from Disk…` → select the zip → Restart IDE

---

### 2 — Add a data source

1. Open the **GraphDB** tool window (right sidebar)
2. Click **+** in the toolbar
3. Fill in your connection details:
   - **Name** — e.g. `Local Dev` or `Production Aura`
   - **Bolt URL** — e.g. `bolt://localhost:7687` or `neo4j+s://xxx.databases.neo4j.io`
   - **Username / Password**
   - **Database** — leave blank to use the default database
4. Click **Test Connection** to verify, then **OK** to save

The data source will appear greyed out while the connection is being verified, and turn bold once ready.

---

### 3 — Write and run a query

1. Double-click a connected data source to open its Cypher editor
2. Write your query, e.g.:
   ```cypher
   MATCH (c:Customer)-[r:HAS_Project]->(p:Project)
   RETURN c, r, p
   ```
3. Press `Ctrl+F5` (Windows/Linux) or `Cmd+F5` (macOS) to run
4. Results appear in the panel at the bottom of the IDE

---

### 4 — Explore results

| Tab | What you see |
|---|---|
| **Graph** | Interactive force-directed graph. Scroll to pan, `Ctrl+scroll` to zoom, drag nodes to reposition, **double-click** a node to view all its properties |
| **Table** | Raw rows and columns for scalar/list results |
| **Log** | Terminal-style history of every query run in this session |
| **Parameters** | Set named `$param` values as JSON before running a query |

The **legend** on the right side of the Graph tab shows which colour corresponds to which node type.

---

## Compatibility

| IDE | Version |
|---|---|
| IntelliJ IDEA Community | 2024.3 – 2025.1.x |
| IntelliJ IDEA Ultimate | 2024.3 – 2025.1.x |

---

## Reporting Issues

If you find a bug or want to request a feature, please open an issue on GitHub.

👉 **[Report an issue](https://github.com/abulwcse/jetbrains-graphdb-plugin/issues/new)**

When reporting a bug, please include:
- IDE name and version (`Help → About`)
- Plugin version
- Neo4j version
- Steps to reproduce the issue
- Any error messages from `Help → Show Log in Finder / Explorer`

---

## License

[MIT](LICENSE) © 2026 abulwcse
