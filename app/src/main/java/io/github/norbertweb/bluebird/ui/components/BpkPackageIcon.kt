package io.github.norbertweb.bluebird.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

/** Reads the application icon directly from a .bpk without extracting the package. */
object BpkPackageIcon {
    private const val DEFAULT_ICON = "icon/icon.png"

    /** Extract only the declared PNG icon into a small cache for window/taskbar identity. */
    fun cache(packageFile: File, cacheRoot: File): File? {
        if (!packageFile.isFile || !packageFile.name.endsWith(".bpk", ignoreCase = true)) return null
        return runCatching {
            cacheRoot.mkdirs()
            val out = File(cacheRoot, "${packageFile.nameWithoutExtension}-${UUID.randomUUID()}.png")
            ZipFile(packageFile).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json")
                val iconPath = manifestEntry?.let { entry ->
                    zip.getInputStream(entry).use { input ->
                        JSONObject(input.bufferedReader(Charsets.UTF_8).readText())
                            .optString("icon", DEFAULT_ICON).trim().ifBlank { DEFAULT_ICON }
                    }
                } ?: DEFAULT_ICON
                val iconEntry = zip.getEntry(iconPath) ?: zip.getEntry(DEFAULT_ICON)
                    ?: return null
                zip.getInputStream(iconEntry).use { input -> input.copyTo(out.outputStream()) }
            }
            out.takeIf { it.isFile }
        }.getOrNull()
    }

    fun decode(packageFile: File): Bitmap? {
        if (!packageFile.isFile || !packageFile.name.endsWith(".bpk", ignoreCase = true)) return null
        return runCatching {
            ZipFile(packageFile).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json")
                val iconPath = manifestEntry?.let { entry ->
                    zip.getInputStream(entry).use { input ->
                        val manifest = JSONObject(input.bufferedReader(Charsets.UTF_8).readText())
                        manifest.optString("icon", DEFAULT_ICON).trim().ifBlank { DEFAULT_ICON }
                    }
                } ?: DEFAULT_ICON

                val iconEntry = zip.getEntry(iconPath) ?: zip.getEntry(DEFAULT_ICON)
                iconEntry?.let { entry ->
                    zip.getInputStream(entry).use(BitmapFactory::decodeStream)
                }
            }
        }.getOrNull()
    }
}
