/**
 * Build configuration for the GraphDB Plugin for JetBrains IDEs.
 *
 * This build script uses:
 *   - Kotlin JVM plugin 2.1.20 for compiling Kotlin sources (2.1.20 adds Java 25 support)
 *   - IntelliJ Platform Gradle Plugin 2.3.0 (new DSL) for IDE integration
 *   - Neo4j Java Driver 5.26.3 for Bolt protocol connectivity
 *   - JGraphX 4.2.2 for graph visualization in the canvas panel (Phase 2+)
 *   - JUnit 5 + Mockito for unit testing
 *
 * Key tasks:
 *   ./gradlew runIde          – launches a sandboxed IntelliJ IDEA with the plugin installed
 *   ./gradlew test            – runs all unit tests
 *   ./gradlew buildPlugin     – produces a distributable zip under build/distributions/
 *   ./gradlew generateLexer   – regenerates the Cypher lexer from the .flex grammar file
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    // Required repository for the IntelliJ Platform Gradle Plugin 2.x
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // -------------------------------------------------------------------------
    // IntelliJ Platform
    // -------------------------------------------------------------------------
    intellijPlatform {
        // Target platform: IntelliJ IDEA Community 2024.3.5
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())

        // Bundled plugin required for Java-related UI helpers (icons, PSI utils)
        bundledPlugin("com.intellij.java")

        // Instrumentation tools required for internal API access and UI testing
        instrumentationTools()

        // IntelliJ Platform test framework for integration tests
        testFramework(TestFrameworkType.Platform)
    }

    // -------------------------------------------------------------------------
    // Neo4j Java Driver — Bolt protocol connectivity
    // Netty transitive dependency is excluded because IntelliJ already bundles
    // its own Netty version, which would cause classloader conflicts.
    // -------------------------------------------------------------------------
    implementation("org.neo4j.driver:neo4j-java-driver:${providers.gradleProperty("neo4jDriverVersion").get()}") {
        exclude(group = "io.netty")
    }

    // -------------------------------------------------------------------------
    // JGraphX — graph layout and canvas rendering (used in Phase 2+ editor)
    // -------------------------------------------------------------------------
    implementation("com.github.vlsi.mxgraph:jgraphx:4.2.2")

    // -------------------------------------------------------------------------
    // Test dependencies
    // -------------------------------------------------------------------------
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
}

// -----------------------------------------------------------------------------
// IntelliJ Platform configuration
// -----------------------------------------------------------------------------
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        // Signing is required for JetBrains Marketplace publication.
        // Provide these environment variables in CI / publish workflow:
        //   CERTIFICATE_CHAIN  — PEM content of the certificate chain
        //   PRIVATE_KEY        — PEM content of the private key
        //   PRIVATE_KEY_PASSWORD — passphrase (empty string if key is unencrypted)
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN").orElse("")
        privateKey        = providers.environmentVariable("PRIVATE_KEY").orElse("")
        password          = providers.environmentVariable("PRIVATE_KEY_PASSWORD").orElse("")
    }

    publishing {
        // Token for ./gradlew publishPlugin — set PUBLISH_TOKEN env var in CI.
        token = providers.environmentVariable("PUBLISH_TOKEN").orElse("")
    }
}

// -----------------------------------------------------------------------------
// JFlex lexer generation task
// Regenerates src/main/gen/com/graphdbplugin/language/CypherLexer.java from
// src/main/kotlin/com/graphdbplugin/language/Cypher.flex whenever the grammar
// changes. The generated sources are committed to the repository so that
// contributors without JFlex installed can still build the project.
// -----------------------------------------------------------------------------
tasks.register<Exec>("generateLexer") {
    group = "build"
    description = "Regenerates the Cypher JFlex lexer from Cypher.flex grammar file."

    val flexFile = file("src/main/kotlin/com/graphdbplugin/language/Cypher.flex")
    val outputDir = file("src/main/gen/com/graphdbplugin/language")

    inputs.file(flexFile)
    outputs.dir(outputDir)

    doFirst {
        outputDir.mkdirs()
    }

    // JFlex must be on the PATH, or replace with full path to jflex jar invocation.
    commandLine(
        "jflex",
        "--output", outputDir.absolutePath,
        flexFile.absolutePath
    )
}

// Add generated sources to the main source set so the IDE and compiler pick them up.
sourceSets {
    main {
        kotlin {
            srcDir("src/main/gen")
        }
        java {
            srcDir("src/main/gen")
        }
    }
}

// -----------------------------------------------------------------------------
// Test configuration
// -----------------------------------------------------------------------------
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// -----------------------------------------------------------------------------
// runIde task — opens a sandboxed IntelliJ IDEA instance with the plugin loaded
// -----------------------------------------------------------------------------
tasks.runIde {
    // Increase the sandbox IDE heap to avoid OOM during development.
    jvmArgs("-Xmx2g", "-Xms512m")
}
