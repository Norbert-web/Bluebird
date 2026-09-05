package io.github.norbertweb.bluebird.ui.components

import android.util.Base64
import org.json.JSONObject
import java.io.File

/**
 * A Bluebird .exe is a launch descriptor, not a Windows PE executable.
 * It is the canonical launch file inside an installed Bluebird application.
 *
 * The descriptor carries application identity, the source directory reference,
 * entry point and an embedded icon copy so the file remains self-describing.
 */
data class BluebirdExecutableDescriptor(
    val version: Int = 1,
    val appId: String,
    val name: String,
    val sourceRoot: String,
    val entry: String,
    val iconRelativePath: String,
    val iconBase64Png: String = ""
)

object BluebirdExecutable {
    private const val MAGIC = "BLUEBIRD-EXE"
    private const val DESCRIPTOR_VERSION = 1

    fun create(
        executableFile: File,
        appId: String,
        name: String,
        installDir: File,
        entry: String,
        iconFile: File?
    ) {
        val iconBase64 = runCatching {
            iconFile?.takeIf { it.isFile }?.readBytes()?.let {
                Base64.encodeToString(it, Base64.NO_WRAP)
            } ?: ""
        }.getOrDefault("")

        val json = JSONObject()
            .put("magic", MAGIC)
            .put("version", DESCRIPTOR_VERSION)
            .put("appId", appId)
            .put("name", name)
            .put("sourceRoot", ".")
            .put("entry", entry)
            .put("icon", "icon/icon.png")
            .put("iconBase64Png", iconBase64)

        executableFile.parentFile?.mkdirs()
        executableFile.writeText(json.toString(), Charsets.UTF_8)
    }

    fun read(executableFile: File): BluebirdExecutableDescriptor? {
        if (!executableFile.isFile || !executableFile.name.endsWith(".exe", ignoreCase = true)) return null
        return runCatching {
            val o = JSONObject(executableFile.readText(Charsets.UTF_8))
            if (o.optString("magic") != MAGIC) return null
            BluebirdExecutableDescriptor(
                version = o.optInt("version", 1),
                appId = o.getString("appId"),
                name = o.getString("name"),
                sourceRoot = o.optString("sourceRoot", "."),
                entry = o.getString("entry"),
                iconRelativePath = o.optString("icon", "icon/icon.png"),
                iconBase64Png = o.optString("iconBase64Png", "")
            )
        }.getOrNull()
    }

    fun resolveSourceRoot(exeFile: File, descriptor: BluebirdExecutableDescriptor): File {
        val parent = exeFile.parentFile?.canonicalFile
            ?: throw IllegalArgumentException("Executable has no parent directory")
        val candidate = File(parent, descriptor.sourceRoot).canonicalFile
        require(candidate.path == parent.path || candidate.path.startsWith(parent.path + File.separator)) {
            "Executable source escapes application directory"
        }
        return candidate
    }

    fun resolveEntry(exeFile: File, descriptor: BluebirdExecutableDescriptor): File {
        val root = resolveSourceRoot(exeFile, descriptor)
        val candidate = File(root, descriptor.entry).canonicalFile
        require(candidate.path == root.path || candidate.path.startsWith(root.path + File.separator)) {
            "Executable entry escapes application directory"
        }
        return candidate
    }

    fun resolveIcon(exeFile: File, descriptor: BluebirdExecutableDescriptor): File {
        val root = resolveSourceRoot(exeFile, descriptor)
        val candidate = File(root, descriptor.iconRelativePath).canonicalFile
        return candidate.takeIf { it.isFile } ?: File(root, "icon/icon.png")
    }
}
