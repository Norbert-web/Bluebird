package io.github.norbertweb.bluebird.ui.components

import java.io.File

/**
 * Bluebird Package Specification (BPS) — version 1.
 *
 * A .bpk is a ZIP container with a fixed top-level structure:
 *
 * manifest.json
 * icon/icon.png
 * app/index.html
 * app/...
 * installer/ (optional; developer-designed HTML/CSS/JS installer)
 */
data class BpkManifest(
    val id: String,
    val name: String,
    val version: String,
    val publisher: String,
    val entry: String,
    val icon: String,
    val description: String = "",
    val homepage: String = "",
    val runtime: String = "web"
)

data class StagedBpkPackage(
    val sourceFile: File,
    val stageDir: File,
    val manifest: BpkManifest,
    val hasCustomInstaller: Boolean
)

data class InstalledBpkApp(
    val id: String,
    val name: String,
    val version: String,
    val publisher: String,
    val installDir: String,
    val executablePath: String,
    val entry: String,
    val iconPath: String,
    val installedAt: Long,
    val packagePath: String = "",
    val canReinstall: Boolean = false
)
