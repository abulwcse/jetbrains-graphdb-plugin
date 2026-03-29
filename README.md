# GraphDB Plugin for JetBrains IDEs

A full-featured Neo4j / Bolt graph-database management plugin for IntelliJ IDEA and
other JetBrains IDEs. Manage connections, write and execute Cypher queries with
syntax highlighting and real-time error annotations, and visualise graph results — all
without leaving your IDE.

**Status:** Phase 1 complete | Phase 2 complete | Phase 3 complete | Phase 4 complete | Phase 5 complete

---

## 1. Overview

The GraphDB Plugin integrates Neo4j graph-database workflows directly into your
JetBrains IDE. It provides a dedicated right-sidebar tool window for managing
connection profiles, a per-connection Cypher query editor with schema-aware
auto-completion, and a multi-tab result panel showing an interactive graph canvas,
a sortable table, a query log, and a JSON parameter editor.

The plugin communicates with Neo4j over the Bolt protocol using the official
Neo4j Java Driver 5.x. Passwords are never stored in plain text; they are delegated
to IntelliJ Platform's `PasswordSafe` abstraction, which uses the OS native keychain
on all supported platforms.

---

## 2. Features

### Data Source Management
- Right-sidebar tool window with a scrollable data source panel
- Add, edit, and delete Neo4j / Bolt data source configurations
- Per-source colour coding for at-a-glance identification
- Passwords stored securely in the OS native keychain (macOS Keychain, Windows DPAPI, Linux libsecret / KWallet)
- Test Connection button in the Add / Edit dialog with live status feedback
- Persistent data sources that survive IDE restarts (serialised to `graphdb-datasources.xml`)
- Schema Refresh button in the toolbar to force re-introspection of labels, relationship types, and property keys

### Cypher Language Support
- Cypher file type for `.cypher` and `.cql` files
- Full token-based syntax highlighting (keywords, functions, strings, comments, parameters, operators)
- 16 customisable colour scheme attributes under Settings → Editor → Color Scheme → Cypher
- Cypher auto-completion (Ctrl+Space / ⌘Space):
  - Reserved keywords
  - Built-in aggregate, mathematical, string, type-conversion, and temporal functions
  - Schema-aware: node labels, relationship types, and property keys fetched live from the connected database
- Real-time error annotations (no save or build required):
  - Unmatched parentheses, brackets, and braces
  - Bare `$` parameter without a following identifier
  - Unterminated string literals
- Line and block comment toggling (Ctrl+/ and Ctrl+Shift+/)

### Query Execution
- Per-data-source Cypher editor tab opened by double-clicking a data source
- Run Query toolbar button and keyboard shortcut: Ctrl+F5 (Windows/Linux) or Cmd+F5 (macOS)
- Queries executed via Neo4j Java Driver 5.x over Bolt
- Connection pool: one pooled `Driver` per data source; reused across queries
- Driver evicted and re-created when credentials or URL change
- All pooled drivers closed cleanly on IDE shutdown

### Result Panel (bottom tool window)
Four tabs, activated automatically after the first query:

| Tab | Description |
| --- | --- |
| **Query Log** | Timestamped log of all executions with status, duration (ms), and row count |
| **Graph** | Interactive JGraphX force-directed canvas; nodes coloured by label; PNG export button |
| **Table** | Sortable grid with type-aware cell rendering for all Neo4j value types |
| **Parameters** | JSON editor for `$param` substitution; values are injected into the next query run |

---

## 3. Prerequisites

| Requirement | Minimum version | Notes |
| --- | --- | --- |
| JDK | 17 | LTS release; JDK 21 also tested |
| IntelliJ IDEA | 2024.3 (build 243) | Community or Ultimate |
| Gradle | 8.x | Gradle wrapper included — no manual install needed |
| Neo4j | 5.x | Community or Enterprise; Aura cloud also supported |
| Kotlin | 2.1.0 | Resolved automatically by the Gradle build |

The plugin declares a dependency on `com.intellij.modules.platform`, which is
satisfied by every JetBrains IDE (Rider, GoLand, WebStorm, PyCharm, etc.) in
addition to IntelliJ IDEA.

---

## 4. Project Structure

```
jetBrains-GraphDb-Plugin/
├── build.gradle.kts                              # Gradle build (IntelliJ Platform Plugin 2.x DSL)
├── gradle.properties                             # Version pins and platform coordinates
├── settings.gradle.kts                           # Root project name
│
├── src/
│   ├── main/
│   │   ├── kotlin/com/graphdbplugin/
│   │   │   ├── GraphDbPluginIcons.kt             # Central icon registry (IconLoader)
│   │   │   │
│   │   │   ├── actions/
│   │   │   │   ├── AddDataSourceAction.kt        # AnAction: opens Add dialog
│   │   │   │   ├── EditDataSourceAction.kt       # AnAction: opens Edit dialog
│   │   │   │   ├── DeleteDataSourceAction.kt     # AnAction: delete with confirmation
│   │   │   │   └── RunQueryAction.kt             # AnAction: execute Cypher (Ctrl+F5)
│   │   │   │
│   │   │   ├── datasource/
│   │   │   │   ├── BoltDataSource.kt             # Data class: connection config (no password)
│   │   │   │   ├── DataSourceManagerState.kt     # XML-serialisable state bean
│   │   │   │   └── DataSourceManager.kt          # App service: CRUD + PasswordSafe + pool eviction
│   │   │   │
│   │   │   ├── dialog/
│   │   │   │   └── AddEditDataSourceDialog.kt    # Add / Edit modal dialog with test-connection
│   │   │   │
│   │   │   ├── editor/
│   │   │   │   ├── CypherEditorProvider.kt       # FileEditorProvider + schema refresh trigger
│   │   │   │   ├── CypherFileEditor.kt           # Custom FileEditor with Run toolbar
│   │   │   │   └── CypherVirtualFile.kt          # In-memory VirtualFile per data source
│   │   │   │
│   │   │   ├── execution/
│   │   │   │   ├── Neo4jConnectionPool.kt        # Thread-safe Driver pool (ConcurrentHashMap)
│   │   │   │   ├── QueryExecutor.kt              # Runs Cypher, maps records to QueryResult
│   │   │   │   ├── QueryLogEntry.kt              # Immutable log entry (timestamp, status, duration)
│   │   │   │   └── QueryResult.kt                # Sealed result type (Success / Error)
│   │   │   │
│   │   │   ├── language/
│   │   │   │   ├── CypherCommenter.kt            # Line (//) and block (/* */) comment support
│   │   │   │   ├── CypherFileType.kt             # FileType descriptor (.cypher, .cql)
│   │   │   │   ├── CypherKeywords.kt             # KEYWORDS and FUNCTIONS sets
│   │   │   │   ├── CypherLanguage.kt             # Language singleton
│   │   │   │   ├── CypherTokenTypes.kt           # All IElementType constants
│   │   │   │   │
│   │   │   │   ├── annotation/
│   │   │   │   │   └── CypherAnnotator.kt        # Real-time bracket / string / param annotations
│   │   │   │   │
│   │   │   │   ├── completion/
│   │   │   │   │   ├── CypherCompletionContributor.kt    # Wires completion providers
│   │   │   │   │   ├── KeywordCompletionProvider.kt      # Keywords + functions
│   │   │   │   │   └── SchemaAwareCompletionProvider.kt  # Labels / rel-types / property keys
│   │   │   │   │
│   │   │   │   ├── highlighting/
│   │   │   │   │   ├── CypherColorSettingsPage.kt        # Settings → Color Scheme → Cypher
│   │   │   │   │   ├── CypherHighlighterColors.kt        # TextAttributesKey definitions
│   │   │   │   │   ├── CypherSyntaxHighlighter.kt        # SyntaxHighlighter implementation
│   │   │   │   │   └── CypherSyntaxHighlighterFactory.kt # Factory registered in plugin.xml
│   │   │   │   │
│   │   │   │   ├── lexer/
│   │   │   │   │   └── CypherLexer.kt           # Hand-written lexer producing CypherTokenTypes
│   │   │   │   │
│   │   │   │   ├── parser/
│   │   │   │   │   └── CypherParserDefinition.kt # ParserDefinition (flat PSI tree)
│   │   │   │   │
│   │   │   │   └── psi/
│   │   │   │       ├── CypherCompositeElementTypes.kt # Composite node types
│   │   │   │       ├── CypherElementType.kt           # Base IElementType
│   │   │   │       ├── CypherFile.kt                  # Root PSI file node
│   │   │   │       └── CypherPsiElement.kt            # Base PSI element
│   │   │   │
│   │   │   ├── lifecycle/
│   │   │   │   └── GraphDbApplicationLifecycle.kt  # AppLifecycleListener: closeAll on shutdown
│   │   │   │
│   │   │   ├── results/
│   │   │   │   ├── GraphVisualizationPanel.kt    # JGraphX interactive graph canvas
│   │   │   │   ├── ParametersPanel.kt            # JSON $param editor
│   │   │   │   ├── QueryLogPanel.kt              # Timestamped execution log
│   │   │   │   ├── ResultPanel.kt                # Four-tab container panel
│   │   │   │   ├── ResultToolWindowFactory.kt    # Creates bottom tool window content
│   │   │   │   ├── ResultToolWindowManager.kt    # Project service bridging results to UI
│   │   │   │   └── TableResultPanel.kt           # Sortable results table
│   │   │   │
│   │   │   ├── services/
│   │   │   │   ├── GraphDbProjectService.kt      # Project service: active data source ref
│   │   │   │   └── SchemaIntrospectionService.kt # Project service: async schema cache (AtomicReference)
│   │   │   │
│   │   │   └── toolwindow/
│   │   │       ├── DataSourceListCellRenderer.kt # Colour-coded list cell renderer
│   │   │       ├── DataSourceTreePanel.kt        # Main panel (list + toolbar, implements DataProvider)
│   │   │       ├── GraphDbToolWindowFactory.kt   # Creates right tool window content
│   │   │       └── SchemaRefreshAction.kt        # Toolbar action: force schema re-introspection
│   │   │
│   │   └── resources/
│   │       ├── META-INF/plugin.xml               # Plugin descriptor (extensions, actions, listeners)
│   │       ├── icons/
│   │       │   ├── graphdb.svg                   # Tool-window stripe icon
│   │       │   └── datasource.svg                # Data-source entry icon
│   │       └── messages/
│   │           └── GraphDbBundle.properties      # UI strings for internationalisation
│   │
│   └── test/
│       └── kotlin/com/graphdbplugin/
│           ├── datasource/
│           │   ├── BoltDataSourceTest.kt         # BoltDataSource creation, defaults, UUID, palette
│           │   └── DataSourceManagerTest.kt      # CRUD, findById, state persistence
│           ├── execution/
│           │   ├── Neo4jConnectionPoolTest.kt    # Pool singleton, evict no-op, closeAll no-op
│           │   ├── QueryLogEntryTest.kt          # Log entry construction and formatting
│           │   └── QueryResultTest.kt            # QueryResult sealed types
│           └── language/
│               ├── CypherKeywordsTest.kt         # KEYWORDS/FUNCTIONS invariants
│               └── CypherLexerTest.kt            # Lexer token classification
│
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

---

## 5. Development Setup

### Clone the repository

```bash
git clone <repo-url>
cd jetBrains-GraphDb-Plugin
```

### Open in IntelliJ IDEA

1. Launch IntelliJ IDEA 2024.3 or later.
2. Choose **File → Open** and select the `jetBrains-GraphDb-Plugin` directory.
3. IDEA detects the Gradle project and prompts you to import it — click **Trust Project**.
4. Wait for Gradle sync to complete (first sync downloads ~200 MB of dependencies).

### Run the plugin in a sandboxed IDE

```bash
./gradlew runIde
```

This downloads the target platform (IntelliJ IDEA Community 2024.3.5) into the
Gradle build cache the first time (~500 MB), starts a sandboxed IDE instance with
the plugin pre-installed, and opens an empty project so you can test the tool window
immediately.

To attach the debugger:
1. In your main IDE, open **Run → Edit Configurations** and add a **Remote JVM Debug**
   configuration pointing at `localhost:5005`.
2. Start the sandbox with `./gradlew runIde --debug-jvm`.
3. Hit **Debug** on your remote configuration — breakpoints now work inside the sandbox.

> The sandbox configuration directory is stored under `build/idea-sandbox/`.
> Delete it to reset all sandbox settings to defaults.

---

## 6. Running Tests

```bash
./gradlew test
```

Expected console output (all tests passing):

```
> Task :test

com.graphdbplugin.datasource.BoltDataSourceTest > create() — sets name and url from arguments PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > create() — default field values are applied correctly PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > create() — id is a non-blank, valid UUID PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > create() — two separate calls produce different UUIDs PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > color — default colour is a non-blank CSS hex string PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > copy() — preserves id and overrides specified field PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > URL validation — bolt:// scheme is accepted PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > URL validation — neo4j:// scheme is accepted PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > COLOR_PALETTE — contains exactly 8 colour entries PASSED
com.graphdbplugin.datasource.BoltDataSourceTest > COLOR_PALETTE — all entries are valid CSS hex colours PASSED

com.graphdbplugin.datasource.DataSourceManagerTest > addDataSource — single entry is persisted PASSED
com.graphdbplugin.datasource.DataSourceManagerTest > addDataSource — multiple entries are all persisted PASSED
com.graphdbplugin.datasource.DataSourceManagerTest > removeDataSource — correct entry is removed, other entry remains PASSED
com.graphdbplugin.datasource.DataSourceManagerTest > updateDataSource — updated entry is returned by findById PASSED
com.graphdbplugin.datasource.DataSourceManagerTest > findById — returns the correct data source when ID exists PASSED
com.graphdbplugin.datasource.DataSourceManagerTest > findById — returns null when ID does not exist PASSED

com.graphdbplugin.execution.Neo4jConnectionPoolTest > testEvict_removesFromPool PASSED
com.graphdbplugin.execution.Neo4jConnectionPoolTest > testCloseAll_emptyPool PASSED
com.graphdbplugin.execution.Neo4jConnectionPoolTest > testPoolObject_isSingleton PASSED

com.graphdbplugin.execution.QueryLogEntryTest > timestamp is set PASSED
com.graphdbplugin.execution.QueryLogEntryTest > status is preserved PASSED

com.graphdbplugin.execution.QueryResultTest > success result wraps records PASSED
com.graphdbplugin.execution.QueryResultTest > error result wraps message PASSED

com.graphdbplugin.language.CypherKeywordsTest > testKeywordsNotEmpty PASSED
com.graphdbplugin.language.CypherKeywordsTest > testFunctionsNotEmpty PASSED
com.graphdbplugin.language.CypherKeywordsTest > testKnownKeywordsPresent PASSED
com.graphdbplugin.language.CypherKeywordsTest > testKnownFunctionsPresent PASSED
com.graphdbplugin.language.CypherKeywordsTest > testKeywordsAreUpperCase PASSED
com.graphdbplugin.language.CypherKeywordsTest > testFunctionsAreLowerCase PASSED
com.graphdbplugin.language.CypherKeywordsTest > testNoDuplicates_keywords PASSED
com.graphdbplugin.language.CypherKeywordsTest > testNoDuplicates_functions PASSED
com.graphdbplugin.language.CypherKeywordsTest > testTrueNotInKeywords PASSED
com.graphdbplugin.language.CypherKeywordsTest > testMatchIsInKeywords PASSED

com.graphdbplugin.language.CypherLexerTest > keywords are tokenised as KEYWORD PASSED
com.graphdbplugin.language.CypherLexerTest > identifiers are tokenised as IDENTIFIER PASSED
com.graphdbplugin.language.CypherLexerTest > string literals are tokenised as STRING_LITERAL PASSED

BUILD SUCCESSFUL in Xs
```

Test reports are written to `build/reports/tests/test/index.html`.

### Test classes and coverage

| Test class | What it covers |
| --- | --- |
| `BoltDataSourceTest` | Data class construction, default values, UUID generation, colour palette |
| `DataSourceManagerTest` | CRUD operations, `findById`, state persistence bean |
| `Neo4jConnectionPoolTest` | Pool singleton identity, `evict` no-op, `closeAll` on empty pool |
| `QueryLogEntryTest` | Log entry construction, timestamp, and status fields |
| `QueryResultTest` | `QueryResult.Success` and `QueryResult.Error` sealed variants |
| `CypherKeywordsTest` | Non-emptiness, known entries, UPPER/lower case conventions, no duplicates |
| `CypherLexerTest` | Lexer token classification for keywords, identifiers, and string literals |

---

## 7. Building

```bash
./gradlew buildPlugin
```

Output:

```
build/distributions/GraphDB Plugin-1.0.0.zip
```

The zip contains the plugin JAR and all runtime dependencies (Neo4j Java Driver 5.x,
JGraphX). It can be installed directly into any compatible JetBrains IDE without an
internet connection.

---

## 8. Deployment

### Manual Install (development / local testing)

1. Build: `./gradlew buildPlugin`
2. In the target IDE: **Settings → Plugins → gear icon (⚙) → Install Plugin from Disk...**
3. Select `build/distributions/GraphDB Plugin-1.0.0.zip`
4. Restart the IDE when prompted.

### JetBrains Marketplace (release)

Before publishing you must sign the plugin. Set these environment variables (typically
in your CI/CD pipeline secrets vault):

| Variable | Where to get it |
| --- | --- |
| `CERTIFICATE_CHAIN` | PEM file path — generated by `./gradlew generateCertificate` or obtained from your organisation's PKI |
| `PRIVATE_KEY` | PEM file path — private key matching the certificate |
| `PRIVATE_KEY_PASSWORD` | Passphrase protecting the private key |
| `PUBLISH_TOKEN` | Permanent token from [plugins.jetbrains.com](https://plugins.jetbrains.com) → Your Account → Tokens |

Then run:

```bash
./gradlew signPlugin
./gradlew publishPlugin
```

For first-time signing setup, follow the
[JetBrains Plugin Signing guide](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).

### CI/CD — GitHub Actions example

Create `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Build plugin
        run: ./gradlew buildPlugin

      - name: Run tests
        run: ./gradlew test

      - name: Sign plugin
        env:
          CERTIFICATE_CHAIN:    ${{ secrets.CERTIFICATE_CHAIN }}
          PRIVATE_KEY:          ${{ secrets.PRIVATE_KEY }}
          PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}
        run: ./gradlew signPlugin

      - name: Publish to Marketplace
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
        run: ./gradlew publishPlugin
```

---

## 9. Using the Plugin

### Step-by-step walkthrough

1. **Open the GraphDB panel** — click the graph icon on the right sidebar, or go to
   **View → Tool Windows → GraphDB**.

2. **Add a data source** — click the **+** button in the toolbar. Fill in:
   - **Name** — a display label (e.g. "Local Dev" or "Production Aura")
   - **URL** — Bolt URL (e.g. `bolt://localhost:7687` or `neo4j+s://xxx.databases.neo4j.io`)
   - **Username** and **Password**
   - **Database** — target database (default: `neo4j`)
   - **SSL** — check if your server requires TLS
   - **Timeout** — TCP connection timeout in seconds
   - **Color** — choose a colour swatch for visual identification

3. **Test the connection** — click **Test Connection** in the dialog. A green checkmark
   confirms reachability; a red message shows the error.

4. **Save** — click **OK**. The data source appears in the panel list. The password is
   stored in your OS native keychain and never written to disk in plain text.

5. **Open a Cypher editor** — double-click the data source. A new editor tab opens and
   schema introspection begins in the background.

6. **Write a query** — type Cypher in the editor. Press **Ctrl+Space** (or **Cmd+Space**
   on Mac) for completions. Watch for red squiggles indicating syntax errors (unmatched
   brackets, unterminated strings, bare `$`).

7. **Run the query** — press **Ctrl+F5** (**Cmd+F5** on Mac) or click the **Run** button
   in the editor toolbar.

8. **View results** — the bottom panel activates automatically:
   - **Graph** tab: interactive force-directed graph; drag nodes, zoom with scroll wheel,
     export as PNG using the toolbar button.
   - **Table** tab: all result rows in a sortable grid; click column headers to sort.
   - **Query Log** tab: history of every execution with timestamp, status, duration, and row count.
   - **Parameters** tab: enter JSON like `{"userId": 42}` to supply `$userId` in the query.

9. **Refresh schema** — if the database schema changes (new labels added, etc.), click the
   **Refresh** (circular arrow) button in the GraphDB tool window toolbar to re-introspect
   and update completion suggestions.

---

## 10. Architecture

| Package | Responsibility | Key IntelliJ Platform APIs |
| --- | --- | --- |
| `com.graphdbplugin` | Plugin root, icon registry | `IconLoader` |
| `com.graphdbplugin.datasource` | `BoltDataSource` data model, `DataSourceManager` CRUD service, XML state bean | `@Service`, `PersistentStateComponent`, `PasswordSafe` |
| `com.graphdbplugin.dialog` | Add / Edit data source modal with test-connection | `DialogWrapper`, `JBTextField` |
| `com.graphdbplugin.editor` | Per-data-source Cypher editor tab, virtual file, editor provider | `FileEditorProvider`, `VirtualFile`, `FileEditorManager` |
| `com.graphdbplugin.execution` | Neo4j Driver pool, query execution, result and log types | Neo4j Java Driver 5.x, `ConcurrentHashMap` |
| `com.graphdbplugin.language` | Cypher language definition: file type, lexer, parser, PSI | `Language`, `Lexer`, `ParserDefinition`, `PsiFile` |
| `com.graphdbplugin.language.annotation` | Real-time syntax error annotations | `Annotator`, `AnnotationHolder`, `HighlightSeverity` |
| `com.graphdbplugin.language.completion` | Keyword, function, and schema-aware completions | `CompletionContributor`, `CompletionProvider` |
| `com.graphdbplugin.language.highlighting` | Token colouring, colour scheme settings page | `SyntaxHighlighter`, `ColorSettingsPage`, `TextAttributesKey` |
| `com.graphdbplugin.lifecycle` | IDE shutdown hook — closes all pooled drivers | `AppLifecycleListener` |
| `com.graphdbplugin.results` | Four-tab result panel, graph canvas, table, log, parameters | `ToolWindowFactory`, JGraphX (`mxGraph`) |
| `com.graphdbplugin.services` | Schema cache (atomic), active data source per project | `@Service(PROJECT)`, `AtomicReference` |
| `com.graphdbplugin.toolwindow` | Main sidebar panel, cell renderer, schema refresh action | `ActionToolbar`, `JBList`, `DataProvider` |

### Key design decisions

- **No password in XML state.** `BoltDataSource` deliberately has no password field.
  Passwords are handled exclusively by `PasswordSafe`, which uses the OS keychain.
- **Application-level data source service.** `DataSourceManager` is `Service.Level.APP`
  so connection profiles are shared across all open project windows, matching the
  IntelliJ Database tool window UX.
- **AtomicReference for schema cache.** `SchemaIntrospectionService` uses
  `AtomicReference<List<String>>` instead of `@Volatile` + `synchronized` so that
  background writer threads and EDT reader threads never need to coordinate via locks.
- **Flat PSI tree.** The Cypher parser produces a deliberately flat tree (all tokens are
  direct children of `CypherFile`). This simplifies the annotator, completion, and
  highlighting code at the cost of no structural analysis — a full grammar parser is not
  required for the current feature set.
- **Driver pool eviction on credential change.** `DataSourceManager.updateDataSource` and
  `removeDataSource` both call `Neo4jConnectionPool.evict`, ensuring that stale drivers
  with old credentials are never reused after an edit.
- **Lifecycle listener for clean shutdown.** `GraphDbApplicationLifecycle` implements
  `AppLifecycleListener.appWillBeClosed` to close all pooled drivers, preventing JVM
  hang at shutdown due to Netty event-loop threads.

---

## 11. Contributing

1. Fork the repository and create a branch from `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
   Branch naming convention: `feature/<short-description>`, `fix/<issue-id>`, `chore/<task>`.

2. Make your changes and add or update tests to cover the new behaviour.

3. Ensure all tests pass:
   ```bash
   ./gradlew test
   ```

4. Verify the plugin works in the sandboxed IDE:
   ```bash
   ./gradlew runIde
   ```

5. Commit with a descriptive message and open a pull request against `main`.

6. All pull requests require:
   - At least one passing CI run (`./gradlew test buildPlugin`)
   - KDoc comments on all public and internal API declarations
   - No new compiler warnings or suppressed inspection violations

---

## 12. License

Copyright 2024 GraphDB Plugin Contributors.

Licensed under the **Apache License, Version 2.0**. You may obtain a copy at:

```
https://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.
