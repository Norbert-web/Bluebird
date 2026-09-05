package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Single registry for installed Bluebird applications.
 *
 * Start Menu, desktop shortcuts, search, Store updates and File Explorer
 * can all converge on this registry instead of maintaining separate copies.
 */
class BluebirdAppRegistry(private val context: Context) {
    private val root = BluebirdStorage.root(context)
    private val registryFile = File(root, "ProgramData/installed-apps.json")

    init {
        registryFile.parentFile?.mkdirs()
    }

    @Synchronized
    fun all(): List<InstalledBpkApp> = runCatching {
        if (!registryFile.isFile) return emptyList()
        val arr = JSONArray(registryFile.readText(Charsets.UTF_8))
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    InstalledBpkApp(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        version = o.getString("version"),
                        publisher = o.optString("publisher", ""),
                        installDir = o.getString("installDir"),
                        executablePath = o.getString("executablePath"),
                        entry = o.getString("entry"),
                        iconPath = o.getString("iconPath"),
                        installedAt = o.optLong("installedAt", 0L),
                        packagePath = o.optString("packagePath", ""),
                        canReinstall = o.optBoolean("canReinstall", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun find(id: String): InstalledBpkApp? = all().firstOrNull { it.id == id }

    @Synchronized
    fun upsert(app: InstalledBpkApp) {
        val updated = all().filterNot { it.id == app.id }.toMutableList().apply { add(app) }
        write(updated)
    }

    @Synchronized
    fun remove(id: String) {
        write(all().filterNot { it.id == id })
    }

    private fun write(apps: List<InstalledBpkApp>) {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(
                JSONObject()
                    .put("id", app.id)
                    .put("name", app.name)
                    .put("version", app.version)
                    .put("publisher", app.publisher)
                    .put("installDir", app.installDir)
                    .put("executablePath", app.executablePath)
                    .put("entry", app.entry)
                    .put("iconPath", app.iconPath)
                    .put("installedAt", app.installedAt)
                    .put("packagePath", app.packagePath)
                    .put("canReinstall", app.canReinstall)
            )
        }
        registryFile.writeText(arr.toString(), Charsets.UTF_8)
    }
}
