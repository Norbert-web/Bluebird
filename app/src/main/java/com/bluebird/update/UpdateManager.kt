package com.bluebird.update

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bluebird.update.UpdateManager.UPDATE_JSON_URL
import com.bluebird.update.UpdateManager.shouldCheckNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// UPDATE MANAGER
// ─────────────────────────────────────────────────────────────────────────────

/**

 */
object UpdateManager {

    // ── Configuration ─────────────────────────────────────────────────────────
    /** Raw URL of your update.json on GitHub. */
    const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/Norbert-web/bluebird-releases/main/assets/bluebird/update.json"

    private const val PREFS_NAME            = "bluebird_update_prefs"
    private const val KEY_LAST_CHECK_MS     = "last_check_ms"
    private const val KEY_CHECK_FREQUENCY   = "check_frequency"
    private const val KEY_DELIVERY_MODE     = "delivery_mode"
    private const val KEY_AUTO_UPDATE       = "auto_update"
    private const val KEY_UPDATE_CHANNEL    = "update_channel"
    private const val KEY_CACHED_VERSION    = "cached_version_code"
    private const val KEY_CACHED_MANIFEST   = "cached_manifest_json"
    private const val KEY_LAST_NOTIFIED_VER = "last_notified_version_code"

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches update.json and compares against the installed version.
     * Must be called from a coroutine (uses IO dispatcher internally).
     */
    suspend fun checkForUpdate(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson(UPDATE_JSON_URL)
            val manifest = parseManifest(json)
            val currentCode = getInstalledVersionCode(context)

            prefs(context).edit {
                putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                putString(KEY_CACHED_MANIFEST, json)
                putInt(KEY_CACHED_VERSION, currentCode)
            }

            if (manifest.versionCode > currentCode) {
                UpdateResult.UpdateAvailable(manifest, currentCode)
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Returns true if enough time has passed since the last check,
     * based on the user's selected frequency.  Always returns true
     * for EVERY_LAUNCH.
     */
    fun shouldCheckNow(context: Context): Boolean {
        val p = prefs(context)
        val freq = UpdateCheckFrequency.fromLabel(
            p.getString(KEY_CHECK_FREQUENCY, UpdateCheckFrequency.EVERY_LAUNCH.label)!!
        )
        if (freq == UpdateCheckFrequency.EVERY_LAUNCH) return true
        if (freq == UpdateCheckFrequency.MANUAL) return false

        val lastMs = p.getLong(KEY_LAST_CHECK_MS, 0L)
        val elapsed = System.currentTimeMillis() - lastMs
        val threshold = when (freq) {
            UpdateCheckFrequency.DAILY      -> TimeUnit.DAYS.toMillis(1)
            UpdateCheckFrequency.WEEKLY     -> TimeUnit.DAYS.toMillis(7)
            UpdateCheckFrequency.BIWEEKLY   -> TimeUnit.DAYS.toMillis(14)
            UpdateCheckFrequency.SEVEN_WEEKS-> TimeUnit.DAYS.toMillis(49)
            UpdateCheckFrequency.MONTHLY    -> TimeUnit.DAYS.toMillis(30)
            else                            -> 0L
        }
        return elapsed >= threshold
    }

    /** Human-readable timestamp of when updates were last checked. */
    fun lastCheckedLabel(context: Context): String {
        val ms = prefs(context).getLong(KEY_LAST_CHECK_MS, 0L)
        if (ms == 0L) return "Never"
        val diff = System.currentTimeMillis() - ms
        return when {
            diff < 60_000                      -> "Just now"
            diff < 3_600_000                   -> "${diff / 60_000}m ago"
            diff < 86_400_000                  -> "${diff / 3_600_000}h ago"
            else                               -> "${diff / 86_400_000}d ago"
        }
    }

    /** Returns the cached manifest from the last successful check, if any. */
    fun getCachedManifest(context: Context): UpdateManifest? {
        val json = prefs(context).getString(KEY_CACHED_MANIFEST, null) ?: return null
        return try { parseManifest(json) } catch (_: Exception) { null }
    }

    /** Fires an update notification only if we haven't already notified for this version. */
    fun notifyIfNewVersion(
        context: Context,
        result: UpdateResult,
        deliveryMode: UpdateDelivery
    ) {
        if (result !is UpdateResult.UpdateAvailable) return
        val lastNotified = prefs(context).getInt(KEY_LAST_NOTIFIED_VER, -1)
        if (lastNotified == result.manifest.versionCode) return // already notified

        UpdateNotificationHelper.notifyUpdateAvailable(context, result.manifest, deliveryMode)
        prefs(context).edit {
            putInt(KEY_LAST_NOTIFIED_VER, result.manifest.versionCode)
        }
    }

    // ── Preference accessors ──────────────────────────────────────────────────

    fun getCheckFrequency(context: Context): UpdateCheckFrequency =
        UpdateCheckFrequency.fromLabel(
            prefs(context).getString(KEY_CHECK_FREQUENCY, UpdateCheckFrequency.EVERY_LAUNCH.label)!!
        )

    fun setCheckFrequency(context: Context, freq: UpdateCheckFrequency) =
        prefs(context).edit { putString(KEY_CHECK_FREQUENCY, freq.label) }

    fun getDeliveryMode(context: Context): UpdateDelivery =
        UpdateDelivery.fromLabel(
            prefs(context).getString(KEY_DELIVERY_MODE, UpdateDelivery.EXTERNAL.label)!!
        )

    fun setDeliveryMode(context: Context, mode: UpdateDelivery) =
        prefs(context).edit { putString(KEY_DELIVERY_MODE, mode.label) }

    fun getAutoUpdate(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_UPDATE, true)

    fun setAutoUpdate(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_AUTO_UPDATE, enabled) }

    fun getUpdateChannel(context: Context): String =
        prefs(context).getString(KEY_UPDATE_CHANNEL, "Stable") ?: "Stable"

    fun setUpdateChannel(context: Context, channel: String) =
        prefs(context).edit { putString(KEY_UPDATE_CHANNEL, channel) }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            conn.requestMethod  = "GET"
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${conn.responseCode}")
            }
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun parseManifest(json: String): UpdateManifest {
        val obj = JSONObject(json)
        val changelog = mutableListOf<String>()
        if (obj.has("changelog")) {
            val arr = obj.getJSONArray("changelog")
            for (i in 0 until arr.length()) changelog.add(arr.getString(i))
        }
        return UpdateManifest(
            versionCode  = obj.optInt("versionCode", 0),
            versionName  = obj.optString("versionName", ""),
            apkUrl       = obj.optString("apkUrl", ""),
            changelog    = changelog,
            forceUpdate  = obj.optBoolean("forceUpdate", false),
            apkSize      = obj.optString("apkSize", "")
        )
    }

    private fun getInstalledVersionCode(context: Context): Int = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pi.versionCode
        }
    } catch (_: Exception) { 0 }
}

// Needed for Build.VERSION_CODES reference without extra import clutter
private object Build {
    object VERSION_CODES { const val P = 28 }
    object VERSION { val SDK_INT get() = android.os.Build.VERSION.SDK_INT }
}
