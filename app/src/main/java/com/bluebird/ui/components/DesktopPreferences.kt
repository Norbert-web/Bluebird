package com.bluebird.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────
// DesktopPreferences
// Persists all desktop settings + icon positions across app restarts.
// Uses a single SharedPreferences file ("desktop_prefs").
// ─────────────────────────────────────────────────────────────────
class DesktopPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("desktop_prefs", Context.MODE_PRIVATE)

    // ── Icon size ──────────────────────────────────────────────────
    var iconSize: DesktopIconSize
        get() = DesktopIconSize.valueOf(
            prefs.getString("icon_size", DesktopIconSize.MEDIUM.name) ?: DesktopIconSize.MEDIUM.name
        )
        set(v) = prefs.edit().putString("icon_size", v.name).apply()

    // ── Sort ───────────────────────────────────────────────────────
    var sortMode: DesktopSortMode
        get() = DesktopSortMode.valueOf(
            prefs.getString("sort_mode", DesktopSortMode.NAME.name) ?: DesktopSortMode.NAME.name
        )
        set(v) = prefs.edit().putString("sort_mode", v.name).apply()

    var sortAscending: Boolean
        get() = prefs.getBoolean("sort_ascending", true)
        set(v) = prefs.edit().putBoolean("sort_ascending", v).apply()

    // ── Layout ─────────────────────────────────────────────────────
    var autoArrange: Boolean
        get() = prefs.getBoolean("auto_arrange", true)
        set(v) = prefs.edit().putBoolean("auto_arrange", v).apply()

    var showIconsOnDesktop: Boolean
        get() = prefs.getBoolean("show_icons", true)
        set(v) = prefs.edit().putBoolean("show_icons", v).apply()

    // ── Wallpaper ──────────────────────────────────────────────────
    var wallpaperMode: DesktopWallpaperMode
        get() = DesktopWallpaperMode.valueOf(
            prefs.getString("wallpaper_mode", DesktopWallpaperMode.APPARENT.name)
                ?: DesktopWallpaperMode.APPARENT.name
        )
        set(v) = prefs.edit().putString("wallpaper_mode", v.name).apply()

    var wallpaperGradientIndex: Int
        get() = prefs.getInt("wallpaper_gradient_index", 0)
        set(v) = prefs.edit().putInt("wallpaper_gradient_index", v).apply()

    var wallpaperImageIndex: Int
        get() = prefs.getInt("wallpaper_image_index", 0)
        set(v) = prefs.edit().putInt("wallpaper_image_index", v).apply()

    // custom wallpaper URI (empty = none)
    var customWallpaperUri: String
        get() = prefs.getString("custom_wallpaper_uri", "") ?: ""
        set(v) = prefs.edit().putString("custom_wallpaper_uri", v).apply()

    // ── Custom icon positions ──────────────────────────────────────
    // Stored as a JSON array: [{"id":"...","x":0.0,"y":0.0}, ...]
    fun saveCustomPositions(positions: Map<String, Offset>) {
        val arr = JSONArray()
        positions.forEach { (id, off) ->
            arr.put(JSONObject().apply {
                put("id", id)
                put("x", off.x.toDouble())
                put("y", off.y.toDouble())
            })
        }
        prefs.edit().putString("custom_positions", arr.toString()).apply()
    }

    fun loadCustomPositions(): Map<String, Offset> {
        val json = prefs.getString("custom_positions", null) ?: return emptyMap()
        return try {
            val arr = JSONArray(json)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    put(o.getString("id"), Offset(o.getDouble("x").toFloat(), o.getDouble("y").toFloat()))
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    fun clearCustomPositions() {
        prefs.edit().remove("custom_positions").apply()
    }
}
