package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Installs and validates Bluebird .bpk packages.
 *
 * The installer deliberately extracts to a private staging directory first.
 * Only after validation succeeds is the final Program Files directory created.
 */
enum class BpkInstallStage {
    COPYING,
    OPTIMIZING,
    CREATING_LAUNCHER,
    CACHING_PACKAGE,
    REGISTERING
}

class BpkPackageManager(private val context: Context) {
    val bluebirdRoot: File = BluebirdStorage.root(context)
    val programFilesDir: File = File(bluebirdRoot, "Program Files")
    private val stagingRoot: File = File(bluebirdRoot, ".staging")
    private val packageCacheDir: File = File(bluebirdRoot, "ProgramData/Packages")

    private val registry = BluebirdAppRegistry(context)

    init {
        programFilesDir.mkdirs()
        stagingRoot.mkdirs()
        packageCacheDir.mkdirs()
    }

    fun stage(packageFile: File): StagedBpkPackage {
        require(packageFile.isFile) { "Package does not exist: ${packageFile.path}" }
        require(packageFile.extension.equals("bpk", true)) { "Not a .bpk package" }

        val stageDir = File(stagingRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        try {
            extractSafely(packageFile, stageDir)
            val manifest = parseManifest(File(stageDir, "manifest.json"))
            validateStructure(stageDir, manifest)
            return StagedBpkPackage(
                sourceFile = packageFile,
                stageDir = stageDir,
                manifest = manifest,
                hasCustomInstaller = File(stageDir, "installer/index.html").isFile
            )
        } catch (t: Throwable) {
            stageDir.deleteRecursively()
            throw t
        }
    }

    fun install(
        staged: StagedBpkPackage,
        installDirOverride: File? = null,
        onProgress: ((BpkInstallStage, Float) -> Unit)? = null
    ): InstalledBpkApp {
        val manifest = staged.manifest
        val finalDir = chooseInstallDirectory(manifest, installDirOverride)
        val parentDir = finalDir.parentFile ?: throw IllegalArgumentException("Invalid installation directory")
        parentDir.mkdirs()
        val tempFinal = File(parentDir, ".${finalDir.name}.${UUID.randomUUID()}.installing")

        try {
            tempFinal.mkdirs()
            onProgress?.invoke(BpkInstallStage.COPYING, 0f)
            copyRecursivelyWithProgress(staged.stageDir, tempFinal, onProgress)
            onProgress?.invoke(BpkInstallStage.COPYING, 1f)
            onProgress?.invoke(BpkInstallStage.OPTIMIZING, 0f)
            optimizeWebAssets(tempFinal)
            onProgress?.invoke(BpkInstallStage.OPTIMIZING, 1f)

            val iconFile = File(tempFinal, manifest.icon)
            require(iconFile.isFile) { "Manifest icon does not exist: ${manifest.icon}" }

            onProgress?.invoke(BpkInstallStage.CREATING_LAUNCHER, 0f)
            val exeFile = File(tempFinal, "${sanitizeFileName(manifest.name)}.exe")
            BluebirdExecutable.create(
                executableFile = exeFile,
                appId = manifest.id,
                name = manifest.name,
                installDir = finalDir,
                entry = manifest.entry,
                iconFile = iconFile
            )

            // The descriptor points at finalDir, so replace the old directory only
            // after the complete staged copy is ready.
            if (finalDir.exists()) finalDir.deleteRecursively()
            check(tempFinal.renameTo(finalDir)) { "Could not finalize application install" }
            onProgress?.invoke(BpkInstallStage.CREATING_LAUNCHER, 1f)

            onProgress?.invoke(BpkInstallStage.CACHING_PACKAGE, 0f)
            val cachedPackage = File(packageCacheDir, sanitizeFileName(manifest.id) + ".bpk")
            runCatching { staged.sourceFile.copyTo(cachedPackage, overwrite = true) }
            onProgress?.invoke(BpkInstallStage.CACHING_PACKAGE, 1f)

            val installed = InstalledBpkApp(
                id = manifest.id,
                name = manifest.name,
                version = manifest.version,
                publisher = manifest.publisher,
                installDir = finalDir.absolutePath,
                executablePath = File(finalDir, exeFile.name).absolutePath,
                entry = manifest.entry,
                iconPath = File(finalDir, manifest.icon).absolutePath,
                installedAt = System.currentTimeMillis(),
                packagePath = cachedPackage.takeIf { it.isFile }?.absolutePath.orEmpty(),
                canReinstall = cachedPackage.isFile
            )
            onProgress?.invoke(BpkInstallStage.REGISTERING, 0f)
            registry.upsert(installed)
            onProgress?.invoke(BpkInstallStage.REGISTERING, 1f)
            return installed
        } catch (t: Throwable) {
            tempFinal.deleteRecursively()
            throw t
        } finally {
            staged.stageDir.deleteRecursively()
        }
    }

    fun install(
        packageFile: File,
        installDirOverride: File? = null,
        onProgress: ((BpkInstallStage, Float) -> Unit)? = null
    ): InstalledBpkApp = install(stage(packageFile), installDirOverride, onProgress)

    fun reinstall(appId: String): InstalledBpkApp? {
        val installed = registry.find(appId) ?: return null
        val packageFile = File(installed.packagePath).takeIf { it.isFile } ?: return null
        return install(packageFile, File(installed.installDir))
    }

    fun uninstall(appId: String): Boolean {
        val installed = registry.find(appId) ?: return false
        val deleted = File(installed.installDir).deleteRecursively()
        if (deleted || !File(installed.installDir).exists()) registry.remove(appId)
        return deleted
    }

    fun apps(): List<InstalledBpkApp> = registry.all()

    fun readManifestFromInstall(installDir: File): BpkManifest? =
        runCatching { parseManifest(File(installDir, "manifest.json")) }.getOrNull()

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun parseManifest(file: File): BpkManifest {
        require(file.isFile) { "Required file missing: manifest.json" }
        val o = JSONObject(file.readText(StandardCharsets.UTF_8))
        val manifest = BpkManifest(
            id = o.optString("id").trim(),
            name = o.optString("name").trim(),
            version = o.optString("version").trim(),
            publisher = o.optString("publisher").trim(),
            entry = o.optString("entry", "app/index.html").trim(),
            icon = o.optString("icon", "icon/icon.png").trim(),
            description = o.optString("description", ""),
            homepage = o.optString("homepage", ""),
            runtime = o.optString("runtime", "web").trim().lowercase()
        )
        require(manifest.id.matches(Regex("[A-Za-z0-9._-]{2,128}"))) { "Invalid manifest id" }
        require(manifest.name.isNotBlank()) { "Manifest name is required" }
        require(manifest.version.isNotBlank()) { "Manifest version is required" }
        require(manifest.publisher.isNotBlank()) { "Manifest publisher is required" }
        require(manifest.runtime == "web") { "Unsupported runtime: ${manifest.runtime}" }
        require(isSafeRelativePath(manifest.entry)) { "Unsafe manifest entry path" }
        require(isSafeRelativePath(manifest.icon)) { "Unsafe manifest icon path" }
        return manifest
    }

    private fun validateStructure(stageDir: File, manifest: BpkManifest) {
        val allowedTopLevel = setOf("manifest.json", "icon", "app", "installer", "metadata")
        stageDir.listFiles()?.forEach { child ->
            require(child.name in allowedTopLevel) {
                "Unsupported top-level entry: ${child.name}"
            }
        }
        require(File(stageDir, "icon/icon.png").isFile) {
            "Required file missing: icon/icon.png"
        }
        require(File(stageDir, "app/index.html").isFile) {
            "Required file missing: app/index.html"
        }
        require(File(stageDir, manifest.entry).isFile) {
            "Manifest entry does not exist: ${manifest.entry}"
        }
        require(File(stageDir, manifest.icon).isFile) {
            "Manifest icon does not exist: ${manifest.icon}"
        }
        val installerDir = File(stageDir, "installer")
        if (installerDir.exists()) {
            require(installerDir.isDirectory) { "installer must be a directory" }
            require(File(installerDir, "index.html").isFile) {
                "Custom installer requires installer/index.html"
            }
            require(File(installerDir, "style.css").isFile) {
                "Custom installer requires installer/style.css"
            }
            require(File(installerDir, "installer.js").isFile) {
                "Custom installer requires installer/installer.js"
            }
        }
    }

    private fun chooseInstallDirectory(manifest: BpkManifest, overrideDir: File? = null): File {
        if (overrideDir != null) {
            val chosen = overrideDir.canonicalFile
            val parent = chosen.parentFile?.canonicalFile ?: throw IllegalArgumentException("Invalid installation directory")
            require(chosen.path.startsWith(bluebirdRoot.canonicalFile.path + File.separator)) {
                "Installation directory must remain inside Bluebird Storage"
            }
            require(chosen.path != bluebirdRoot.canonicalFile.path) { "Cannot install into Bluebird Storage root" }
            if (chosen.exists()) {
                require(chosen.isDirectory) { "Installation path is not a directory" }
                val existingManifest = readManifestFromInstall(chosen)
                require(existingManifest?.id == manifest.id) {
                    "Installation path already belongs to another application"
                }
            }
            return chosen
        }
        val base = sanitizeFileName(manifest.name).ifBlank { manifest.id }
        val existing = File(programFilesDir, base)
        val existingManifest = if (existing.isDirectory) readManifestFromInstall(existing) else null
        if (!existing.exists() || existingManifest?.id == manifest.id) return existing
        val suffixBase = "$base-${manifest.id.take(8)}"
        var candidate = File(programFilesDir, suffixBase)
        var counter = 2
        while (candidate.exists()) {
            candidate = File(programFilesDir, "$suffixBase-$counter")
            counter++
        }
        return candidate
    }

    /**
     * Safe, conservative minification. HTML/CSS are compacted; JavaScript is
     * intentionally left byte-for-byte because arbitrary JS whitespace and
     * strings can make naive minifiers break applications.
     */
    private fun optimizeWebAssets(root: File) {
        val app = File(root, "app")
        if (!app.isDirectory) return
        app.walkTopDown().filter { it.isFile }.forEach { file ->
            when (file.extension.lowercase()) {
                "html", "htm" -> file.writeText(minifyHtml(file.readText(Charsets.UTF_8)), Charsets.UTF_8)
                "css" -> file.writeText(minifyCss(file.readText(Charsets.UTF_8)), Charsets.UTF_8)
            }
        }
    }

    private fun minifyHtml(source: String): String =
        source.replace(Regex("<!--(?!\\[if)[\\s\\S]*?-->"), "")
            .replace(Regex(">\\s+<"), "><")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    private fun minifyCss(source: String): String =
        source.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s*([{}:;,>])\\s*"), "$1")
            .trim()

    private fun extractSafely(zipFile: File, targetDir: File) {
        var entries = 0
        var extractedBytes = 0L
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                entries++
                require(entries <= MAX_ENTRIES) { "Package contains too many files" }
                validateZipEntry(entry)
                val out = File(targetDir, entry.name).canonicalFile
                val root = targetDir.canonicalFile
                require(out.path == root.path || out.path.startsWith(root.path + File.separator)) {
                    "Unsafe package path: ${entry.name}"
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            extractedBytes += read
                            require(extractedBytes <= MAX_EXTRACTED_BYTES) {
                                "Package exceeds the maximum installed size"
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
        }
    }

    private fun validateZipEntry(entry: ZipEntry) {
        require(!entry.name.startsWith("/") && !entry.name.startsWith("\\")) {
            "Absolute paths are not allowed in .bpk"
        }
        require(isSafeRelativePath(entry.name)) { "Unsafe package path: ${entry.name}" }
    }

    private fun isSafeRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\")) return false
        val normalized = path.replace('\\', '/').removeSuffix("/")
        if (normalized.isBlank()) return false
        return normalized.split('/').none { it == ".." || it.isBlank() || it == "." }
    }

    private fun copyRecursively(from: File, to: File) {
        if (from.isDirectory) {
            to.mkdirs()
            from.listFiles()?.forEach { copyRecursively(it, File(to, it.name)) }
        } else {
            to.parentFile?.mkdirs()
            from.copyTo(to, overwrite = true)
        }
    }

    private fun copyRecursivelyWithProgress(
        from: File,
        to: File,
        onProgress: ((BpkInstallStage, Float) -> Unit)?
    ) {
        val files = from.walkTopDown().filter { it.isFile }.toList()
        val total = files.size.coerceAtLeast(1)
        var copied = 0
        fun copy(file: File) {
            val relative = file.relativeTo(from).path
            val dest = File(to, relative)
            dest.parentFile?.mkdirs()
            file.copyTo(dest, overwrite = true)
            copied++
            onProgress?.invoke(BpkInstallStage.COPYING, copied.toFloat() / total)
        }
        files.forEach(::copy)
    }

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("""[<>:"/\\\\|?*]"""), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(80)

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 16 * 1024
        private const val MAX_ENTRIES = 5_000
        private const val MAX_EXTRACTED_BYTES = 100L * 1024L * 1024L
    }
}
