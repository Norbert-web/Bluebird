package io.github.norbertweb.bluebird

import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.norbertweb.bluebird.ui.components.BluebirdRemoteNotification
import io.github.norbertweb.bluebird.ui.components.BluebirdExecutable
import io.github.norbertweb.bluebird.ui.components.DesktopFileInfo
import io.github.norbertweb.bluebird.ui.components.BpkPackageManager
import io.github.norbertweb.bluebird.ui.components.InstalledBpkApp
import io.github.norbertweb.bluebird.ui.components.DesktopItemType
import io.github.norbertweb.bluebird.ui.components.ToastNotifData
import io.github.norbertweb.bluebird.ui.components.toToastData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

// ─── Data Models ─────────────────────────────────────────────────────────────

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isPinned: Boolean = false
)

enum class DesktopItemType2 {
    THIS_PC, RECYCLE_BIN, SETTINGS_ICON
}

data class RecycleBinItem(
    val id: String = UUID.randomUUID().toString(),
    val originalPath: String,
    val name: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)

data class DesktopShortcutOld(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val targetPath: String = "",
    val packageName: String = "",
    val type: String = "app",
    val positionX: Int = 0,
    val positionY: Int = 0
)

enum class PowerAction { SLEEP, LOCK, RESTART, SHUTDOWN }

enum class LauncherScreen {
    DESKTOP, SETTINGS, FILE_EXPLORER, BROWSER, TASK_MANAGER,
    CALCULATOR, CALENDAR, PHOTOS, MEDIA_PLAYER, IMAGE_VIEWER,
    WORD_IMPRESS, BLUEBIRD_STORE, RECYCLE_BIN, PremiumTextEditorScreen,
    TERMINAL, PROGRAM_MANAGER, WEB_APP_VIEWER, COPY_PROGRESS
}

// ─────────────────────────────────────────────────────────────────
// Copy/Move engine — single shared implementation used by both Desktop
// and File Explorer (previously each did its own synchronous, non-
// recursive, no-progress File.copyTo() on the main thread).
// ─────────────────────────────────────────────────────────────────
enum class CopyOpType { COPY, MOVE }
enum class CopyJobStatus { SCANNING, RUNNING, DONE, ERROR, CANCELLED }

data class CopyJob(
    val id: String = UUID.randomUUID().toString(),
    val operation: CopyOpType,
    val sourceNames: List<String>,
    val destDir: String,
    val totalBytes: Long = 0L,
    val copiedBytes: Long = 0L,
    val totalFiles: Int = 0,
    val filesDone: Int = 0,
    val currentFileName: String = "",
    val status: CopyJobStatus = CopyJobStatus.SCANNING,
    val speedBytesPerSec: Long = 0L,
    val error: String? = null,
    val startTime: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────
// Background effects — io.github.norbertweb.io.github.norbertweb.bluebird particle animations + live wallpapers.
// Mutually exclusive: turning on a live wallpaper clears any active
// particle animations (both driving the same background layer at once
// would fight visually and cost battery for no benefit).
// ─────────────────────────────────────────────────────────────────
enum class BgAnimationType {
    SNOW, BUBBLES, STARS, RAIN, HEARTS, CONFETTI, FIREFLIES, LEAVES, MATRIX, SAKURA
}

enum class LiveWallpaperType { NONE, AURORA, NEBULA, WAVES, BOKEH }

data class BackgroundEffectsState(
    val activeAnimations: Set<BgAnimationType> = emptySet(),  // multiple = "mix" mode
    val intensity: Int = 50,                                   // 10-100
    val liveWallpaper: LiveWallpaperType = LiveWallpaperType.NONE
)

data class UndoAction(
    val label: String,
    val perform: suspend () -> Unit
)

// Added: iconKey so the taskbar/title-bar can show the right icon without
// importing Compose material icons into the ViewModel layer.
data class WindowState(
    val screen: LauncherScreen,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val title: String = "",
    val id: String = UUID.randomUUID().toString(),
    val extras: Map<String, String> = emptyMap(),
    val iconKey: String = "",
    /** Stable application identity used by Taskbar/Start instead of title matching. */
    val appPackageName: String = "",
    // Path (relative to context.filesDir) to a real bitmap icon — e.g. a fetched
    // favicon for a web app. When present, taskbar/title-bar render this bitmap
    // instead of looking iconKey up in the fixed Material-icon set.
    val customIconPath: String? = null
)

object WindowIconKey {
    const val SETTINGS      = "settings"
    const val FILE_EXPLORER = "folder"
    const val BROWSER       = "public"
    const val CALCULATOR    = "calculate"
    const val CALENDAR      = "calendar_today"
    const val PHOTOS        = "photo_library"
    const val TASK_MANAGER  = "monitor"
    const val MEDIA_PLAYER  = "music_note"
    const val IMAGE_VIEWER  = "image"
    const val BLUEBIRD_STORE         = "NightsStay"
    const val WORD_IMPRESS      = "TextRotationAngledown"
    const val RECYCLE_BIN   = "delete"
    const val PremiumTextEditorScreen = ""
    const val TERMINAL        = "terminal"
    const val PROGRAM_MANAGER = "language"
    // Fallback glyph for a web app window when it has no customIconPath yet
    // (e.g. favicon fetch failed) — grouping key is "webapp:<id>", this is just the glyph.
    const val WEB_APP          = "web_app"
    const val COPY_PROGRESS    = "copy"
}

data class RealNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val time: Long = System.currentTimeMillis(),
    val icon: Drawable? = null
)

data class UserProfile(
    val userName: String = "User",
    val profilePicturePath: String = ""
)

enum class WallpaperTarget { HOME, LOCK_SCREEN }

data class WallpaperState(
    val homeWallpaperIndex: Int = 0,
    val homeWallpaperUri: String = "",
    val lockWallpaperIndex: Int = 1,
    val lockWallpaperUri: String = ""
)

enum class AppTheme { SYSTEM, FOR_YOU, DARK, LIGHT, SPECIAL }

data class LauncherUiState(

    val hasCompletedSetup: Boolean = false,
    val setupStep: Int = 0,
    val isTaskbarVisible: Boolean = true,
    val isStartMenuOpen: Boolean = false,
    val isActionCenterOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isWidgetsOpen: Boolean = false,
    val isPowerMenuOpen: Boolean = false,
    val isDesktopContextMenuOpen: Boolean = false,
    val desktopContextMenuX: Float = 0f,
    val desktopContextMenuY: Float = 0f,
    val isDarkTheme: Boolean = true,
    val accentColor: Long = 0xFF0078D4,
    val isLocked: Boolean = false,
    // Whether any window currently wants the screen kept awake — set true while a
    // video is actively playing in the browser or the built-in Media Player, false
    // otherwise (including "paused"). MainActivity observes this to decide whether
    // to hold FLAG_KEEP_SCREEN_ON; it is intentionally NOT persisted to prefs, since
    // it must always start false on a fresh process rather than resuming "stuck on".
    // Multiple players can call setMediaPlaying independently (e.g. a background
    // download preview + the main player) via the id-keyed set below.
    val activeMediaPlaybackIds: Set<String> = emptySet(),
    val wallpaper: WallpaperState = WallpaperState(),
    val installedApps: List<AppInfo> = emptyList(),
    val pinnedTaskbarApps: List<AppInfo> = emptyList(),
    val desktopFiles: List<DesktopFileInfo> = emptyList(),
    val desktopRefreshTick: Int = 0,
    // Bumped only by an explicit "Refresh" from the io.github.norbertweb.io.github.norbertweb.bluebird context menu — separate from
    // desktopRefreshTick (which bumps on every rescan, including silent ones triggered by
    // the FileObserver after a paste/delete/rename) so the disappear/reappear flicker only
    // plays when the user actually asked for it.
    val manualDesktopRefreshTick: Int = 0,
    val copyJobs: List<CopyJob> = emptyList(),
    val clipboardFiles: List<String> = emptyList(),   // absolute paths — unified across Desktop + File Explorer
    val clipboardCut: Boolean = false,
    val undoAction: UndoAction? = null,
    val backgroundEffects: BackgroundEffectsState = BackgroundEffectsState(),
    val systemDesktopItems: List<DesktopFileInfo> = emptyList(),
    val openWindows: List<WindowState> = emptyList(),
    val activeWindowId: String? = null,
    val notifications: List<RealNotification> = emptyList(),
    val isNotificationListenerEnabled: Boolean = false,
    // Bluebird team announcements fetched from notify.json — moved here (was
    // local state in ActionCenter) so both the panel and the toast host read
    // the same list, and dismissing one place dismisses it everywhere.
    val remoteNotifications: List<BluebirdRemoteNotification> = emptyList(),
    val dismissedRemoteNotificationIds: Set<String> = emptySet(),
    val userProfile: UserProfile = UserProfile(),
    val searchQuery: String = "",
    val volume: Float = 0.5f,
    val brightness: Float = 0.8f,
    val isWifiOn: Boolean = true,
    val isBluetoothOn: Boolean = false,
    val isAirplaneMode: Boolean = false,
    val batteryLevel: Int = 85,
    val isCharging: Boolean = false,
    val currentTime: String = "",
    val currentDate: String = "",
    val recycleBinItems: List<RecycleBinItem> = emptyList(),
    val isWallpaperPickerOpen: Boolean = false,
    val wallpaperPickerTarget: WallpaperTarget = WallpaperTarget.HOME,
    val recentApps: List<AppInfo> = emptyList(),
    val recentFiles: List<String> = emptyList(),
    val installedBpkApps: List<InstalledBpkApp> = emptyList(),
    val pinnedBpkStartIds: Set<String> = emptySet(),


    // ── Extended Settings ──────────────────────────────────────────────────
    // Appearance / Theme
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val textScale: Float = 1f,              // 0.8 – 1.4 multiplier
    val iconSize: String = "Medium",
    val taskbarPosition: String = "Bottom",
    val startMenuLayout: String = "Balanced",
    val transparencyEffects: Boolean = true,
    val animationSpeed: Float = 1f,         // 0 = off, 1 = normal, 2 = fast

    // System
    val launchOnBoot: Boolean = false,
    val snapLayouts: Boolean = true,
    val clipboardHistory: Boolean = true,
    val notificationBanners: Boolean = true,
    val showNotificationBadges: Boolean = true,
    val dndEnabled: Boolean = false,
    val dndScheduled: Boolean = false,
    val dndStartHour: Int = 22,
    val dndEndHour: Int = 7,
    val focusAssist: Boolean = false,
    val screenTimeoutMinutes: Int = 2,

    // Sound
    val mediaVolume: Float = 0.7f,
    val ringtoneVolume: Float = 0.5f,
    val notifVolume: Float = 0.6f,
    val systemSounds: Boolean = true,
    val hapticFeedback: Boolean = true,
    val notifSound: String = "Default",

    // Network extras
    val dataSaver: Boolean = false,
    val hotspotEnabled: Boolean = false,
    val vpnEnabled: Boolean = false,
    val customDns: Boolean = false,
    val dnsAddress: String = "8.8.8.8 (Google)",

    // Gaming
    val gameModeEnabled: Boolean = false,
    val frameRateCap: String = "Unlimited",
    val performanceOverlay: Boolean = false,
    val dndWhileGaming: Boolean = true,
    val hapticInGames: Boolean = true,

    // Accessibility
    val highContrast: Boolean = false,
    val largerText: Boolean = false,
    val boldText: Boolean = false,
    val reduceMotion: Boolean = false,
    val monoAudio: Boolean = false,
    val buttonShapes: Boolean = false,
    val colorCorrectionMode: String = "None",
    val touchHoldDelay: Float = 0.5f,

    // Privacy & Security
    val screenLock: Boolean = false,
    val locationAccess: Boolean = true,
    val cameraAccess: Boolean = true,
    val micAccess: Boolean = true,
    val usageDiagnostics: Boolean = false,
    val unknownSources: Boolean = false,
    val biometricUnlock: Boolean = false,

    // Time & Language
    val use24HourClock: Boolean = false,
    val autoSetTime: Boolean = true,
    val timeZone: String = "UTC",
    val dateFormat: String = "MM/DD/YYYY",
    val firstDayOfWeek: String = "Sunday",

    // Update
    val autoUpdate: Boolean = true,
    val updateChannel: String = "Stable",

    // ── Appearance (extended) ──────────────────────────────────────────────
    val launcherFont: String = "Default (Roboto)",
    val gridSize: String = "4 × 5",
    val showAppLabels: Boolean = true,
    val cornerRadius: Float = 0.5f,
    val statusBarClockPosition: String = "Right",
    val darkModeSchedule: String = "Manual",
    val wallpaperSlideshow: Boolean = false,
    val wallpaperSlideshowInterval: String = "30 minutes",

    // ── Gestures ──────────────────────────────────────────────────────────
    val gestureSwipeUp: String = "App drawer",
    val gestureSwipeDown: String = "Notification shade",
    val gestureDoubleTap: String = "Lock screen",
    val gesturePinch: String = "Overview",
    val iconSwipeUpEnabled: Boolean = false,
    val navBarGestures: Boolean = true,

    // ── Apps ──────────────────────────────────────────────────────────────
    val hideAppsEnabled: Boolean = false,
    val recentAppsLimit: String = "10",

    // ── System (extended) ─────────────────────────────────────────────────
    val batterySaver: Boolean = false,
    val thermalProtection: Boolean = true,

    // ── Sound (extended) ──────────────────────────────────────────────────
    val alarmVolume: Float = 0.7f,
    val volumeKeysMedia: Boolean = true,

    // ── Gaming (extended) ─────────────────────────────────────────────────
    val blockGameScreenshots: Boolean = false,

    // ── Accessibility (extended) ───────────────────────────────────────────
    val magnificationEnabled: Boolean = false,
    val captionsEnabled: Boolean = false,

    // ── Backup & Restore ──────────────────────────────────────────────────
    val autoBackup: Boolean = false,

    // ── Search ────────────────────────────────────────────────────────────
    val showSearchBar: Boolean = true,
    val searchEngine: String = "Google",
    val searchIncludeApps: Boolean = true,
    val searchIncludeContacts: Boolean = true,
    val searchIncludeSettings: Boolean = true,
    val searchWebSuggestions: Boolean = true
)

// ─── Serializable versions for persistence ───────────────────────────────────
private data class AppInfoSaved(val name: String = "", val packageName: String = "")
private data class RecycleBinItemSaved(
    val id: String = "", val originalPath: String = "",
    val name: String = "", val deletedAt: Long = 0L, val sizeBytes: Long = 0L
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val prefs        = application.getSharedPreferences("launcher_prefs_v3", Context.MODE_PRIVATE)
    private val gson         = Gson()
    private val recycleBinDir= File(Environment.getExternalStorageDirectory(), ".recycle")
    private val desktopDir   = File(Environment.getExternalStorageDirectory(), "Desktop")

    init {
        recycleBinDir.mkdirs()
        desktopDir.mkdirs()
        // Remove legacy shortcut target mirrors created by older builds off the UI thread.
        // Shortcuts are pointers only; they must never duplicate target files.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(application.cacheDir, "shortcut_targets").deleteRecursively() }
        }
        loadPersistedData()
        // Installed apps are loaded lazily after the first desktop frame.
        // This keeps PackageManager/icon decoding out of the critical launch path.
        loadInstalledBpkApps()
        loadBackgroundEffects()
        startBatteryMonitor()
        // Remote announcements are fetched only when explicitly requested.
        initSystemDesktopItems()
        refreshDesktopFiles()
        startDesktopObserver()
        checkNotificationListenerPermission()
    }

    private fun loadInstalledBpkApps() {
        val context = getApplication<Application>()
        val manager = BpkPackageManager(context)
        val apps = manager.apps()
        val pinned = try {
            val raw = prefs.getString("pinned_bpk_start", "[]") ?: "[]"
            gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type)?.toSet() ?: emptySet()
        } catch (_: Exception) { emptySet() }
        _uiState.value = _uiState.value.copy(
            installedBpkApps = apps,
            pinnedBpkStartIds = pinned.intersect(apps.map { it.id }.toSet())
        )
    }


    // ─── Background effects (particle animations + live wallpapers) ───

    private fun loadBackgroundEffects() {
        val json = prefs.getString("bg_effects", null) ?: return
        try {
            val loaded = gson.fromJson(json, BackgroundEffectsState::class.java)
            if (loaded != null) _uiState.value = _uiState.value.copy(backgroundEffects = loaded)
        } catch (_: Exception) { /* keep default */ }
    }

    private fun saveBackgroundEffects() {
        prefs.edit().putString("bg_effects", gson.toJson(_uiState.value.backgroundEffects)).apply()
    }

    fun toggleBgAnimation(type: BgAnimationType) {
        val current = _uiState.value.backgroundEffects
        val updated = if (type in current.activeAnimations) current.activeAnimations - type
                      else current.activeAnimations + type
        _uiState.value = _uiState.value.copy(
            backgroundEffects = current.copy(activeAnimations = updated, liveWallpaper = LiveWallpaperType.NONE)
        )
        saveBackgroundEffects()
    }

    fun clearBgAnimations() {
        val current = _uiState.value.backgroundEffects
        _uiState.value = _uiState.value.copy(backgroundEffects = current.copy(activeAnimations = emptySet()))
        saveBackgroundEffects()
    }

    fun setBgAnimationIntensity(value: Int) {
        val current = _uiState.value.backgroundEffects
        _uiState.value = _uiState.value.copy(backgroundEffects = current.copy(intensity = value.coerceIn(10, 100)))
        saveBackgroundEffects()
    }

    /** Enabling a live wallpaper (anything but NONE) clears particle animations — the two
     *  are mutually exclusive so they never fight for the same background layer. */
    fun setLiveWallpaper(type: LiveWallpaperType) {
        val current = _uiState.value.backgroundEffects
        _uiState.value = _uiState.value.copy(
            backgroundEffects = current.copy(
                liveWallpaper    = type,
                activeAnimations = if (type != LiveWallpaperType.NONE) emptySet() else current.activeAnimations
            )
        )
        saveBackgroundEffects()
    }

    // ─── Persistence ─────────────────────────────────────────────────────────
    private fun loadPersistedData() {
        val setupDone          = prefs.getBoolean("setup_done", false)
        val userName           = prefs.getString("user_name", "User") ?: "User"
        val avatarPath         = prefs.getString("avatar_path", "") ?: ""
        val darkTheme          = prefs.getBoolean("dark_theme", true)
        val accentColor        = prefs.getLong("accent_color", 0xFF0078D4)
        val homeWallpaperIndex = prefs.getInt("home_wallpaper_index", 0)
        val homeWallpaperUri   = prefs.getString("home_wallpaper_uri", "") ?: ""
        val lockWallpaperIndex = prefs.getInt("lock_wallpaper_index", 1)
        val lockWallpaperUri   = prefs.getString("lock_wallpaper_uri", "") ?: ""

        // Extended settings
        val appTheme           = AppTheme.values().getOrElse(prefs.getInt("app_theme", 0)) { AppTheme.SYSTEM }
        val textScale          = prefs.getFloat("text_scale", 1f)
        val iconSize           = prefs.getString("icon_size", "Medium") ?: "Medium"
        val taskbarPosition    = prefs.getString("taskbar_position", "Bottom") ?: "Bottom"
        val startMenuLayout    = prefs.getString("start_menu_layout", "Balanced") ?: "Balanced"
        val transparencyEffects= prefs.getBoolean("transparency_effects", true)
        val animationSpeed     = prefs.getFloat("animation_speed", 1f)
        val launchOnBoot       = prefs.getBoolean("launch_on_boot", false)
        val snapLayouts        = prefs.getBoolean("snap_layouts", true)
        val clipboardHistory   = prefs.getBoolean("clipboard_history", true)
        val notifBanners       = prefs.getBoolean("notif_banners", true)
        val notifBadges        = prefs.getBoolean("notif_badges", true)
        val dndEnabled         = prefs.getBoolean("dnd_enabled", false)
        val dndScheduled       = prefs.getBoolean("dnd_scheduled", false)
        val dndStartHour       = prefs.getInt("dnd_start_hour", 22)
        val dndEndHour         = prefs.getInt("dnd_end_hour", 7)
        val focusAssist        = prefs.getBoolean("focus_assist", false)
        val screenTimeout      = prefs.getInt("screen_timeout", 2)
        val mediaVolume        = prefs.getFloat("media_volume", 0.7f)
        val ringtoneVolume     = prefs.getFloat("ringtone_volume", 0.5f)
        val notifVolume        = prefs.getFloat("notif_volume", 0.6f)
        val systemSounds       = prefs.getBoolean("system_sounds", true)
        val hapticFeedback     = prefs.getBoolean("haptic_feedback", true)
        val notifSound         = prefs.getString("notif_sound", "Default") ?: "Default"
        val dataSaver          = prefs.getBoolean("data_saver", false)
        val hotspotEnabled     = prefs.getBoolean("hotspot_enabled", false)
        val vpnEnabled         = prefs.getBoolean("vpn_enabled", false)
        val customDns          = prefs.getBoolean("custom_dns", false)
        val dnsAddress         = prefs.getString("dns_address", "8.8.8.8 (Google)") ?: "8.8.8.8 (Google)"
        val gameModeEnabled    = prefs.getBoolean("game_mode", false)
        val frameRateCap       = prefs.getString("frame_rate_cap", "Unlimited") ?: "Unlimited"
        val perfOverlay        = prefs.getBoolean("perf_overlay", false)
        val dndWhileGaming     = prefs.getBoolean("dnd_gaming", true)
        val hapticInGames      = prefs.getBoolean("haptic_games", true)
        val highContrast       = prefs.getBoolean("high_contrast", false)
        val largerText         = prefs.getBoolean("larger_text", false)
        val boldText           = prefs.getBoolean("bold_text", false)
        val reduceMotion       = prefs.getBoolean("reduce_motion", false)
        val monoAudio          = prefs.getBoolean("mono_audio", false)
        val buttonShapes       = prefs.getBoolean("button_shapes", false)
        val colorCorrection    = prefs.getString("color_correction", "None") ?: "None"
        val touchHoldDelay     = prefs.getFloat("touch_hold_delay", 0.5f)
        val screenLock         = prefs.getBoolean("screen_lock", false)
        val locationAccess     = prefs.getBoolean("location_access", true)
        val cameraAccess       = prefs.getBoolean("camera_access", true)
        val micAccess          = prefs.getBoolean("mic_access", true)
        val usageDiagnostics   = prefs.getBoolean("usage_diagnostics", false)
        val unknownSources     = prefs.getBoolean("unknown_sources", false)
        val biometricUnlock    = prefs.getBoolean("biometric_unlock", false)
        val use24Hour          = prefs.getBoolean("use_24_hour", false)
        val autoSetTime        = prefs.getBoolean("auto_set_time", true)
        val timeZone           = prefs.getString("time_zone", "UTC") ?: "UTC"
        val dateFormat         = prefs.getString("date_format", "MM/DD/YYYY") ?: "MM/DD/YYYY"
        val firstDayOfWeek     = prefs.getString("first_day_of_week", "Sunday") ?: "Sunday"
        val autoUpdate         = prefs.getBoolean("auto_update", true)
        val updateChannel      = prefs.getString("update_channel", "Stable") ?: "Stable"

        // Extended – appearance
        val launcherFont           = prefs.getString("launcher_font", "Default (Roboto)") ?: "Default (Roboto)"
        val gridSize               = prefs.getString("grid_size", "4 × 5") ?: "4 × 5"
        val showAppLabels          = prefs.getBoolean("show_app_labels", true)
        val cornerRadius           = prefs.getFloat("corner_radius", 0.5f)
        val statusBarClockPosition = prefs.getString("status_bar_clock_position", "Right") ?: "Right"
        val darkModeSchedule       = prefs.getString("dark_mode_schedule", "Manual") ?: "Manual"
        val wallpaperSlideshow     = prefs.getBoolean("wallpaper_slideshow", false)
        val wallpaperSlideshowInterval = prefs.getString("wallpaper_slideshow_interval", "30 minutes") ?: "30 minutes"
        // Extended – gestures
        val gestureSwipeUp      = prefs.getString("gesture_swipe_up", "App drawer") ?: "App drawer"
        val gestureSwipeDown    = prefs.getString("gesture_swipe_down", "Notification shade") ?: "Notification shade"
        val gestureDoubleTap    = prefs.getString("gesture_double_tap", "Lock screen") ?: "Lock screen"
        val gesturePinch        = prefs.getString("gesture_pinch", "Overview") ?: "Overview"
        val iconSwipeUpEnabled  = prefs.getBoolean("icon_swipe_up_enabled", false)
        val navBarGestures      = prefs.getBoolean("nav_bar_gestures", true)
        // Extended – apps
        val hideAppsEnabled   = prefs.getBoolean("hide_apps_enabled", false)
        val recentAppsLimit   = prefs.getString("recent_apps_limit", "10") ?: "10"
        // Extended – system
        val batterySaver        = prefs.getBoolean("battery_saver", false)
        val thermalProtection   = prefs.getBoolean("thermal_protection", true)
        // Extended – sound
        val alarmVolume        = prefs.getFloat("alarm_volume", 0.7f)
        val volumeKeysMedia    = prefs.getBoolean("volume_keys_media", true)
        // Extended – gaming
        val blockGameScreenshots = prefs.getBoolean("block_game_screenshots", false)
        // Extended – accessibility
        val magnificationEnabled = prefs.getBoolean("magnification_enabled", false)
        val captionsEnabled      = prefs.getBoolean("captions_enabled", false)
        // Extended – backup
        val autoBackup = prefs.getBoolean("auto_backup", false)
        // Extended – search
        val showSearchBar          = prefs.getBoolean("show_search_bar", true)
        val searchEngine           = prefs.getString("search_engine", "Google") ?: "Google"
        val searchIncludeApps      = prefs.getBoolean("search_include_apps", true)
        val searchIncludeContacts  = prefs.getBoolean("search_include_contacts", true)
        val searchIncludeSettings  = prefs.getBoolean("search_include_settings", true)
        val searchWebSuggestions   = prefs.getBoolean("search_web_suggestions", true)

        val recycleJson  = prefs.getString("recycle_bin", "[]") ?: "[]"
        val recycleItems = try {
            val type = object : TypeToken<List<RecycleBinItemSaved>>() {}.type
            val saved: List<RecycleBinItemSaved> = gson.fromJson(recycleJson, type)
            saved.map { RecycleBinItem(it.id, it.originalPath, it.name, it.deletedAt, it.sizeBytes) }
        } catch (e: Exception) { emptyList() }

        val validItems = recycleItems.filter { File(recycleBinDir, it.id).exists() }
        if (validItems.size != recycleItems.size) {
            prefs.edit().putString("recycle_bin", gson.toJson(validItems.map {
                RecycleBinItemSaved(it.id, it.originalPath, it.name, it.deletedAt, it.sizeBytes)
            })).apply()
        }

        val recentFilesList = prefs.getStringSet("recent_files", emptySet())?.toList() ?: emptyList()

        _uiState.value = _uiState.value.copy(
            hasCompletedSetup = setupDone,
            userProfile       = UserProfile(userName, avatarPath),
            isDarkTheme       = darkTheme,
            accentColor       = accentColor,
            wallpaper         = WallpaperState(homeWallpaperIndex, homeWallpaperUri, lockWallpaperIndex, lockWallpaperUri),
            recycleBinItems   = validItems,
            recentFiles       = recentFilesList,
            // Extended settings
            appTheme              = appTheme,
            textScale             = textScale,
            iconSize              = iconSize,
            taskbarPosition       = taskbarPosition,
            startMenuLayout       = startMenuLayout,
            transparencyEffects   = transparencyEffects,
            animationSpeed        = animationSpeed,
            launchOnBoot          = launchOnBoot,
            snapLayouts           = snapLayouts,
            clipboardHistory      = clipboardHistory,
            notificationBanners   = notifBanners,
            showNotificationBadges= notifBadges,
            dndEnabled            = dndEnabled,
            dndScheduled          = dndScheduled,
            dndStartHour          = dndStartHour,
            dndEndHour            = dndEndHour,
            focusAssist           = focusAssist,
            screenTimeoutMinutes  = screenTimeout,
            mediaVolume           = mediaVolume,
            ringtoneVolume        = ringtoneVolume,
            notifVolume           = notifVolume,
            systemSounds          = systemSounds,
            hapticFeedback        = hapticFeedback,
            notifSound            = notifSound,
            dataSaver             = dataSaver,
            hotspotEnabled        = hotspotEnabled,
            vpnEnabled            = vpnEnabled,
            customDns             = customDns,
            dnsAddress            = dnsAddress,
            gameModeEnabled       = gameModeEnabled,
            frameRateCap          = frameRateCap,
            performanceOverlay    = perfOverlay,
            dndWhileGaming        = dndWhileGaming,
            hapticInGames         = hapticInGames,
            highContrast          = highContrast,
            largerText            = largerText,
            boldText              = boldText,
            reduceMotion          = reduceMotion,
            monoAudio             = monoAudio,
            buttonShapes          = buttonShapes,
            colorCorrectionMode   = colorCorrection,
            touchHoldDelay        = touchHoldDelay,
            screenLock            = screenLock,
            locationAccess        = locationAccess,
            cameraAccess          = cameraAccess,
            micAccess             = micAccess,
            usageDiagnostics      = usageDiagnostics,
            unknownSources        = unknownSources,
            biometricUnlock       = biometricUnlock,
            use24HourClock        = use24Hour,
            autoSetTime           = autoSetTime,
            timeZone              = timeZone,
            dateFormat            = dateFormat,
            firstDayOfWeek        = firstDayOfWeek,
            autoUpdate            = autoUpdate,
            updateChannel         = updateChannel,
            // Extended – appearance
            launcherFont               = launcherFont,
            gridSize                   = gridSize,
            showAppLabels              = showAppLabels,
            cornerRadius               = cornerRadius,
            statusBarClockPosition     = statusBarClockPosition,
            darkModeSchedule           = darkModeSchedule,
            wallpaperSlideshow         = wallpaperSlideshow,
            wallpaperSlideshowInterval = wallpaperSlideshowInterval,
            // Extended – gestures
            gestureSwipeUp      = gestureSwipeUp,
            gestureSwipeDown    = gestureSwipeDown,
            gestureDoubleTap    = gestureDoubleTap,
            gesturePinch        = gesturePinch,
            iconSwipeUpEnabled  = iconSwipeUpEnabled,
            navBarGestures      = navBarGestures,
            // Extended – apps
            hideAppsEnabled = hideAppsEnabled,
            recentAppsLimit = recentAppsLimit,
            // Extended – system
            batterySaver      = batterySaver,
            thermalProtection = thermalProtection,
            // Extended – sound
            alarmVolume     = alarmVolume,
            volumeKeysMedia = volumeKeysMedia,
            // Extended – gaming
            blockGameScreenshots = blockGameScreenshots,
            // Extended – accessibility
            magnificationEnabled = magnificationEnabled,
            captionsEnabled      = captionsEnabled,
            // Extended – backup
            autoBackup = autoBackup,
            // Extended – search
            showSearchBar         = showSearchBar,
            searchEngine          = searchEngine,
            searchIncludeApps     = searchIncludeApps,
            searchIncludeContacts = searchIncludeContacts,
            searchIncludeSettings = searchIncludeSettings,
            searchWebSuggestions  = searchWebSuggestions
        )
    }

    private fun saveAll() {
        val state = _uiState.value
        prefs.edit().apply {
            putBoolean("setup_done",        state.hasCompletedSetup)
            putString("user_name",          state.userProfile.userName)
            putString("avatar_path",        state.userProfile.profilePicturePath)
            putBoolean("dark_theme",        state.isDarkTheme)
            putLong("accent_color",         state.accentColor)
            putInt("home_wallpaper_index",  state.wallpaper.homeWallpaperIndex)
            putString("home_wallpaper_uri", state.wallpaper.homeWallpaperUri)
            putInt("lock_wallpaper_index",  state.wallpaper.lockWallpaperIndex)
            putString("lock_wallpaper_uri", state.wallpaper.lockWallpaperUri)
            putString("recycle_bin", gson.toJson(state.recycleBinItems.map {
                RecycleBinItemSaved(it.id, it.originalPath, it.name, it.deletedAt, it.sizeBytes)
            }))
            putString("pinned_taskbar_apps", gson.toJson(state.pinnedTaskbarApps.map { it.packageName }))
            putString("pinned_bpk_start", gson.toJson(state.pinnedBpkStartIds.toList()))
            putString("recent_apps",         gson.toJson(state.recentApps.map { AppInfoSaved(it.name, it.packageName) }))
            putStringSet("recent_files",     state.recentFiles.toSet())
            val posMap = mutableMapOf<String, Pair<Float, Float>>()
            state.desktopFiles.forEach { file ->
                if (file.position != Offset.Zero)
                    posMap[file.file.absolutePath] = Pair(file.position.x, file.position.y)
            }
            putString("desktop_positions", gson.toJson(posMap))
            // Extended settings
            putInt("app_theme",             state.appTheme.ordinal)
            putFloat("text_scale",          state.textScale)
            putString("icon_size",          state.iconSize)
            putString("taskbar_position",   state.taskbarPosition)
            putString("start_menu_layout",  state.startMenuLayout)
            putBoolean("transparency_effects", state.transparencyEffects)
            putFloat("animation_speed",     state.animationSpeed)
            putBoolean("launch_on_boot",    state.launchOnBoot)
            putBoolean("snap_layouts",      state.snapLayouts)
            putBoolean("clipboard_history", state.clipboardHistory)
            putBoolean("notif_banners",     state.notificationBanners)
            putBoolean("notif_badges",      state.showNotificationBadges)
            putBoolean("dnd_enabled",       state.dndEnabled)
            putBoolean("dnd_scheduled",     state.dndScheduled)
            putInt("dnd_start_hour",        state.dndStartHour)
            putInt("dnd_end_hour",          state.dndEndHour)
            putBoolean("focus_assist",      state.focusAssist)
            putInt("screen_timeout",        state.screenTimeoutMinutes)
            putFloat("media_volume",        state.mediaVolume)
            putFloat("ringtone_volume",     state.ringtoneVolume)
            putFloat("notif_volume",        state.notifVolume)
            putBoolean("system_sounds",     state.systemSounds)
            putBoolean("haptic_feedback",   state.hapticFeedback)
            putString("notif_sound",        state.notifSound)
            putBoolean("data_saver",        state.dataSaver)
            putBoolean("hotspot_enabled",   state.hotspotEnabled)
            putBoolean("vpn_enabled",       state.vpnEnabled)
            putBoolean("custom_dns",        state.customDns)
            putString("dns_address",        state.dnsAddress)
            putBoolean("game_mode",         state.gameModeEnabled)
            putString("frame_rate_cap",     state.frameRateCap)
            putBoolean("perf_overlay",      state.performanceOverlay)
            putBoolean("dnd_gaming",        state.dndWhileGaming)
            putBoolean("haptic_games",      state.hapticInGames)
            putBoolean("high_contrast",     state.highContrast)
            putBoolean("larger_text",       state.largerText)
            putBoolean("bold_text",         state.boldText)
            putBoolean("reduce_motion",     state.reduceMotion)
            putBoolean("mono_audio",        state.monoAudio)
            putBoolean("button_shapes",     state.buttonShapes)
            putString("color_correction",   state.colorCorrectionMode)
            putFloat("touch_hold_delay",    state.touchHoldDelay)
            putBoolean("screen_lock",       state.screenLock)
            putBoolean("location_access",   state.locationAccess)
            putBoolean("camera_access",     state.cameraAccess)
            putBoolean("mic_access",        state.micAccess)
            putBoolean("usage_diagnostics", state.usageDiagnostics)
            putBoolean("unknown_sources",   state.unknownSources)
            putBoolean("biometric_unlock",  state.biometricUnlock)
            putBoolean("use_24_hour",       state.use24HourClock)
            putBoolean("auto_set_time",     state.autoSetTime)
            putString("time_zone",          state.timeZone)
            putString("date_format",        state.dateFormat)
            putString("first_day_of_week",  state.firstDayOfWeek)
            putBoolean("auto_update",       state.autoUpdate)
            putString("update_channel",     state.updateChannel)
            // Extended – appearance
            putString("launcher_font",                  state.launcherFont)
            putString("grid_size",                      state.gridSize)
            putBoolean("show_app_labels",               state.showAppLabels)
            putFloat("corner_radius",                   state.cornerRadius)
            putString("status_bar_clock_position",      state.statusBarClockPosition)
            putString("dark_mode_schedule",             state.darkModeSchedule)
            putBoolean("wallpaper_slideshow",           state.wallpaperSlideshow)
            putString("wallpaper_slideshow_interval",      state.wallpaperSlideshowInterval)
            // Extended – gestures
            putString("gesture_swipe_up",       state.gestureSwipeUp)
            putString("gesture_swipe_down",     state.gestureSwipeDown)
            putString("gesture_double_tap",     state.gestureDoubleTap)
            putString("gesture_pinch",          state.gesturePinch)
            putBoolean("icon_swipe_up_enabled", state.iconSwipeUpEnabled)
            putBoolean("nav_bar_gestures",      state.navBarGestures)
            // Extended – apps
            putBoolean("hide_apps_enabled", state.hideAppsEnabled)
            putString("recent_apps_limit",     state.recentAppsLimit)
            // Extended – system
            putBoolean("battery_saver",       state.batterySaver)
            putBoolean("thermal_protection",  state.thermalProtection)
            // Extended – sound
            putFloat("alarm_volume",         state.alarmVolume)
            putBoolean("volume_keys_media",  state.volumeKeysMedia)
            // Extended – gaming
            putBoolean("block_game_screenshots", state.blockGameScreenshots)
            // Extended – accessibility
            putBoolean("magnification_enabled", state.magnificationEnabled)
            putBoolean("captions_enabled",      state.captionsEnabled)
            // Extended – backup
            putBoolean("auto_backup", state.autoBackup)
            // Extended – search
            putBoolean("show_search_bar",           state.showSearchBar)
            putString("search_engine",              state.searchEngine)
            putBoolean("search_include_apps",       state.searchIncludeApps)
            putBoolean("search_include_contacts",   state.searchIncludeContacts)
            putBoolean("search_include_settings",   state.searchIncludeSettings)
            putBoolean("search_web_suggestions",    state.searchWebSuggestions)
            apply()
        }
    }

    // ─── OOBE ────────────────────────────────────────────────────────────────
    fun advanceSetupStep() {
        val current = _uiState.value.setupStep
        if (current >= 4) completeSetup()
        else _uiState.value = _uiState.value.copy(setupStep = current + 1)
    }

    fun decrementSetupStep() {
        val current = _uiState.value.setupStep
        if (current > 0) _uiState.value = _uiState.value.copy(setupStep = current - 1)
    }

    fun setUserName(name: String) {
        _uiState.value = _uiState.value.copy(userProfile = _uiState.value.userProfile.copy(userName = name))
        saveAll() // FIX: original never persisted this
    }

    /**
     * FIX: Copies the image into app-private storage (stable across restarts)
     * then saves the absolute path. The original stored raw URI strings which
     * expire after the session ends, causing the picture to disappear on relaunch.
     */
    fun setProfilePicture(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val destFile    = File(context.filesDir, "profile_picture.jpg")
                inputStream.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        userProfile = _uiState.value.userProfile.copy(profilePicturePath = destFile.absolutePath)
                    )
                    saveAll() // FIX: original never persisted this either
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /** Overload for callers that already hold a stable absolute file path. */
    fun setProfilePictureFromPath(context: Context, absolutePath: String) {
        setProfilePicture(context, Uri.fromFile(File(absolutePath)))
    }

    fun completeSetup() {
        _uiState.value = _uiState.value.copy(hasCompletedSetup = true, setupStep = 4)
        saveAll()
    }

    // ─── Apps ────────────────────────────────────────────────────────────────
    @Volatile
    private var installedAppsLoadStarted = false

    /** Start app discovery only when the desktop UI is ready to consume it. */
    fun ensureInstalledAppsLoaded() {
        if (installedAppsLoadStarted || _uiState.value.installedApps.isNotEmpty()) return
        installedAppsLoadStarted = true
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm     = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps   = pm.queryIntentActivities(intent, 0)
                .sortedBy { it.loadLabel(pm).toString() }
                .map { info ->
                    AppInfo(
                        name        = info.loadLabel(pm).toString(),
                        packageName = info.activityInfo.packageName,
                        icon        = info.loadIcon(pm)
                    )
                }
            val bpkApps = BpkPackageManager(getApplication<Application>()).apps()
            val bpkInfos = bpkApps.map { bpkAppInfo(it) }
            val allApps = apps + bpkInfos

            val pinnedPackagesJson = prefs.getString("pinned_taskbar_apps", "[]") ?: "[]"
                val savedPackages      = try {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(pinnedPackagesJson, type)
                } catch (e: Exception) { emptyList() }

                val pinnedAndroid = if (savedPackages.isEmpty()) {
                    val defaults = listOf("com.android.chrome", "com.google.android.gm",
                        "com.android.calculator2", "com.google.android.youtube")
                    allApps.filter { it.packageName in defaults }.take(5)
                } else {
                    savedPackages.asSequence()
                        .distinct()
                        .mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                        .distinctBy { it.packageName }
                        .toList()
                }
                // Start pins are independent from Taskbar pins. A BPK selected for
                // Start must not silently appear on the Taskbar.
                val pinned = pinnedAndroid.distinctBy { it.packageName }
                _uiState.value = _uiState.value.copy(pinnedTaskbarApps = pinned)

                val recentAppsJson = prefs.getString("recent_apps", "[]") ?: "[]"
                val recentAppsSaved: List<AppInfoSaved> = try {
                    val type = object : TypeToken<List<AppInfoSaved>>() {}.type
                    gson.fromJson(recentAppsJson, type)
                } catch (e: Exception) { emptyList() }
            val recent = recentAppsSaved.mapNotNull { saved -> allApps.find { it.packageName == saved.packageName } }

            // Publish the complete app snapshot once.  Previously this method emitted
            // three StateFlow values in succession, causing Start Menu/Taskbar/Search
            // to recompose repeatedly during app discovery.
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    installedApps = allApps,
                    pinnedTaskbarApps = pinned,
                    recentApps = recent
                )
            }
        }
    }

    /**
     * Creates a lightweight Desktop pointer. This MUST NOT copy the target file.
     * The .desktop file stores only the real target path and a display label.
     */
    fun addDesktopShortcutFromFile(filePath: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val source = File(filePath)
            if (!source.exists()) return@launch

            val label = fileName.substringBeforeLast('.', fileName).ifBlank { source.nameWithoutExtension.ifBlank { source.name } }
            var dest = File(desktopDir, "$label.desktop")
            var count = 1
            while (dest.exists()) {
                dest = File(desktopDir, "$label ($count).desktop")
                count++
            }

            val type = if (source.isDirectory) "folder" else "file"
            val content = "type=$type\npath=${source.absolutePath}\nlabel=$label\n"
            runCatching { dest.writeText(content) }
                .onSuccess { withContext(Dispatchers.Main) { refreshDesktopFiles() } }
        }
    }

    fun pinAppToTaskbar(app: AppInfo) {
        val current = _uiState.value.pinnedTaskbarApps
        if (current.any { it.packageName == app.packageName }) return
        _uiState.value = _uiState.value.copy(
            pinnedTaskbarApps = (current + app).distinctBy { it.packageName }
        )
        saveAll()
    }

    fun unpinAppFromTaskbar(app: AppInfo) {
        val updated = _uiState.value.pinnedTaskbarApps.filter { it.packageName != app.packageName }
        _uiState.value = _uiState.value.copy(pinnedTaskbarApps = updated)
        saveAll()
    }

    // ─── Desktop File-Backed Model ────────────────────────────────────────────
    private fun initSystemDesktopItems() {
        val sysItems = listOf(
            DesktopFileInfo(
                id = "this_pc",
                file = File(Environment.getExternalStorageDirectory(), "This PC"),
                name = "This PC", type = DesktopItemType.THIS_PC, iconBitmap = null
            ),
            DesktopFileInfo(
                id = "recycle_bin",
                file = File(Environment.getExternalStorageDirectory(), "Recycle Bin"),
                name = "Recycle Bin", type = DesktopItemType.RECYCLE_BIN, iconBitmap = null
            ),
            DesktopFileInfo(
                id = "settings_icon",
                file = File(Environment.getExternalStorageDirectory(), "Settings"),
                name = "Settings", type = DesktopItemType.SETTINGS_ICON, iconBitmap = null
            )
        )
        _uiState.value = _uiState.value.copy(systemDesktopItems = sysItems)
    }

    /** Explicit user-triggered refresh ("Refresh" on the io.github.norbertweb.io.github.norbertweb.bluebird context menu) — bumps
     *  manualDesktopRefreshTick immediately (so the flicker starts right away) and then
     *  does the same rescan refreshDesktopFiles() always does. Silent rescans triggered
     *  by the FileObserver (after a paste/delete/rename) go through refreshDesktopFiles()
     *  directly and never touch this tick, so they never trigger the flicker. */
    fun requestDesktopRefresh() {
        _uiState.value = _uiState.value.copy(manualDesktopRefreshTick = _uiState.value.manualDesktopRefreshTick + 1)
        scheduleDesktopRefresh(0L)
    }

    // Coalesce bursts of FileObserver events and prevent stale scans from overwriting newer state.
    private var desktopRefreshJob: Job? = null
    private val desktopRefreshGeneration = AtomicLong(0L)

    private data class DesktopInfoCacheEntry(
        val modified: Long,
        val length: Long,
        val isDirectory: Boolean,
        val info: DesktopFileInfo?
    )
    private val desktopInfoCache = mutableMapOf<String, DesktopInfoCacheEntry>()

    private fun scheduleDesktopRefresh(delayMs: Long = 250L) {
        desktopRefreshJob?.cancel()
        desktopRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            if (delayMs > 0) delay(delayMs)
            refreshDesktopFilesInternal()
        }
    }

    fun refreshDesktopFiles() = scheduleDesktopRefresh(250L)

    private suspend fun refreshDesktopFilesInternal() {
        val generation = desktopRefreshGeneration.incrementAndGet()
        val files = desktopDir.listFiles()?.toList() ?: emptyList()
        val livePaths = files.asSequence().map { it.absolutePath }.toSet()
        desktopInfoCache.keys.retainAll(livePaths)

        val loaded = files.mapNotNull { file ->
            val path = file.absolutePath
            val modified = file.lastModified()
            val length = if (file.isFile) file.length() else 0L
            val cached = desktopInfoCache[path]
            if (cached != null && cached.modified == modified &&
                cached.length == length && cached.isDirectory == file.isDirectory) {
                cached.info
            } else {
                val info = io.github.norbertweb.bluebird.ui.components.loadDesktopFileInfo(file, getApplication())
                desktopInfoCache[path] = DesktopInfoCacheEntry(modified, length, file.isDirectory, info)
                info
            }
        }

        val positionsJson = prefs.getString("desktop_positions", "{}") ?: "{}"
        val customPositions: Map<String, Pair<Float, Float>> = try {
            val type = object : TypeToken<Map<String, Pair<Float, Float>>>() {}.type
            gson.fromJson(positionsJson, type)
        } catch (_: Exception) { emptyMap() }

        val positioned = loaded.map { item ->
            val pos = customPositions[item.file.absolutePath]
            if (pos != null) item.copy(position = Offset(pos.first, pos.second)) else item
        }

        withContext(Dispatchers.Main.immediate) {
            if (generation == desktopRefreshGeneration.get()) {
                _uiState.value = _uiState.value.copy(
                    desktopFiles = positioned,
                    desktopRefreshTick = _uiState.value.desktopRefreshTick + 1
                )
            }
        }
    }

    private var desktopObserver: FileObserver? = null

    /**
     * Single, ViewModel-owned watcher for the Desktop folder — the one place file-system
     * changes get detected and turned into a refreshDesktopFiles() call. Previously the
     * Desktop screen kept its own separate FileObserver + full local file list; now it (and
     * anything else — File Explorer, Recycle Bin) all read the same live uiState.desktopFiles.
     */
    private fun startDesktopObserver() {
        desktopObserver?.stopWatching()
        desktopDir.mkdirs()
        desktopObserver = object : FileObserver(
            desktopDir.absolutePath,
            CREATE or DELETE or MOVED_FROM or MOVED_TO or CLOSE_WRITE
        ) {
            override fun onEvent(event: Int, path: String?) {
                // Events are deliberately coalesced. A single paste/rename often emits
                // several filesystem events; one refresh is enough to represent the final state.
                scheduleDesktopRefresh(250L)
            }
        }.also { it.startWatching() }
    }

    override fun onCleared() {
        desktopObserver?.stopWatching()
        runCatching { getApplication<Application>().unregisterReceiver(batteryReceiver) }
        super.onCleared()
    }

    fun createDesktopFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = File(desktopDir, name)
            if (!folder.exists()) folder.mkdir()
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun createDesktopTextFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(desktopDir, "$name.txt")
            try {
                file.createNewFile()
                withContext(Dispatchers.Main) { refreshDesktopFiles() }
            } catch (e: IOException) { e.printStackTrace() }
        }
    }

    fun createDesktopAppShortcut(packageName: String, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val safeLabel = label.replace("/", "_")
            val file      = File(desktopDir, "$safeLabel.io.github.norbertweb.io.github.norbertweb.bluebird")
            if (!file.exists()) {
                file.writeText("type=app\npackage=$packageName\nlabel=$label\n")
            }
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun renameDesktopItem(oldPath: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(oldPath)
            if (file.exists()) {
                val dest = File(file.parent, newName)
                file.renameTo(dest)
            }
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun deleteDesktopItem(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) {
                deleteToRecycleBin(file.absolutePath)
            }
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun moveDesktopItem(path: String, newPosition: Offset) {
        val currentPositions = mutableMapOf<String, Pair<Float, Float>>()
        _uiState.value.desktopFiles.forEach {
            if (it.position != Offset.Zero || it.file.absolutePath == path)
                currentPositions[it.file.absolutePath] = Pair(it.position.x, it.position.y)
        }
        currentPositions[path] = Pair(newPosition.x, newPosition.y)
        prefs.edit().putString("desktop_positions", gson.toJson(currentPositions)).apply()
        _uiState.value = _uiState.value.copy(
            desktopFiles = _uiState.value.desktopFiles.map {
                if (it.file.absolutePath == path) it.copy(position = newPosition) else it
            }
        )
    }

    // ─── Recycle Bin (Real File-Backed) ──────────────────────────────────────
    fun deleteToRecycleBin(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        viewModelScope.launch(Dispatchers.IO) {
            val id        = UUID.randomUUID().toString()
            val trashFile = File(recycleBinDir, id)
            val metaFile  = File(recycleBinDir, "$id.meta")
            try {
                file.renameTo(trashFile)
                metaFile.writeText(
                    gson.toJson(
                        RecycleBinItemSaved(
                            id           = id,
                            originalPath = filePath,
                            name         = file.name,
                            deletedAt    = System.currentTimeMillis(),
                            sizeBytes    = file.length()
                        )
                    )
                )
                withContext(Dispatchers.Main) {
                    val item = RecycleBinItem(
                        id           = id,
                        originalPath = filePath,
                        name         = file.name,
                        sizeBytes    = file.length()
                    )
                    _uiState.value = _uiState.value.copy(
                        recycleBinItems = _uiState.value.recycleBinItems + item
                    )
                    saveAll()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /** Restores by the file's original path — for undo, where the recycle bin's
     *  internal UUID (assigned only after the async delete completes) isn't known
     *  to the caller yet at the time the undo action is captured. */
    fun restoreFromRecycleBinByOriginalPath(originalPath: String) {
        val item = _uiState.value.recycleBinItems.find { it.originalPath == originalPath } ?: return
        restoreFromRecycleBin(item.id)
    }

    fun restoreFromRecycleBin(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashFile = File(recycleBinDir, itemId)
            val metaFile  = File(recycleBinDir, "$itemId.meta")
            if (!trashFile.exists()) return@launch

            val meta = try {
                val json = metaFile.readText()
                val type = object : TypeToken<RecycleBinItemSaved>() {}.type
                gson.fromJson<RecycleBinItemSaved>(json, type)
            } catch (e: Exception) { null }

            val originalPath = meta?.originalPath ?: return@launch
            val destFile     = File(originalPath)
            try {
                destFile.parentFile?.mkdirs()
                trashFile.renameTo(destFile)
                metaFile.delete()
            } catch (e: Exception) { e.printStackTrace() }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    recycleBinItems = _uiState.value.recycleBinItems.filter { it.id != itemId }
                )
                saveAll()
                refreshDesktopFiles()
            }
        }
    }

    fun permanentlyDelete(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashFile = File(recycleBinDir, itemId)
            val metaFile  = File(recycleBinDir, "$itemId.meta")
            trashFile.delete()
            metaFile.delete()
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    recycleBinItems = _uiState.value.recycleBinItems.filter { it.id != itemId }
                )
                saveAll()
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch(Dispatchers.IO) {
            recycleBinDir.listFiles()?.forEach { it.delete() }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(recycleBinItems = emptyList())
                saveAll()
            }
        }
    }

    // ─── Wallpaper ────────────────────────────────────────────────────────────
    fun openWallpaperPicker(target: WallpaperTarget) {
        _uiState.value = _uiState.value.copy(isWallpaperPickerOpen = true, wallpaperPickerTarget = target)
    }

    fun closeWallpaperPicker() {
        _uiState.value = _uiState.value.copy(isWallpaperPickerOpen = false)
    }

    fun setBuiltInWallpaper(index: Int, target: WallpaperTarget) {
        val current = _uiState.value.wallpaper
        val updated = when (target) {
            WallpaperTarget.HOME        -> current.copy(homeWallpaperIndex = index, homeWallpaperUri = "")
            WallpaperTarget.LOCK_SCREEN -> current.copy(lockWallpaperIndex = index, lockWallpaperUri = "")
        }
        _uiState.value = _uiState.value.copy(wallpaper = updated)
        saveAll()
    }

    // Signature kept identical to original so all existing call sites compile unchanged.
    fun setCustomWallpaper(uriString: String, target: WallpaperTarget, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentUri  = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(contentUri)
                val destFile    = File(context.filesDir, "wallpaper_${target.name.lowercase()}.jpg")
                inputStream?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                val persistedUri = Uri.fromFile(destFile).toString()

                withContext(Dispatchers.Main) {
                    val current = _uiState.value.wallpaper
                    val updated = when (target) {
                        WallpaperTarget.HOME        -> current.copy(homeWallpaperUri = persistedUri)
                        WallpaperTarget.LOCK_SCREEN -> current.copy(lockWallpaperUri = persistedUri)
                    }
                    _uiState.value = _uiState.value.copy(wallpaper = updated)
                    saveAll()
                }

                if (target == WallpaperTarget.HOME) {
                    try {
                        val wm     = WallpaperManager.getInstance(context)
                        val bitmap = BitmapFactory.decodeFile(destFile.absolutePath)
                        if (bitmap != null) wm.setBitmap(bitmap)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    // One-shot events for the floating bluebird-style toast popup (NotificationToastHost).
    // Separate from the persistent lists in state — this flow only fires the
    // instant something new happens, which is what a transient toast needs.
    private val _toastEvents = MutableSharedFlow<ToastNotifData>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<ToastNotifData> = _toastEvents.asSharedFlow()

    private fun bannersAllowed(): Boolean {
        val state = _uiState.value
        return state.notificationBanners && !state.dndEnabled && !state.focusAssist
    }

    fun addNotification(notification: RealNotification) {
        val updated = listOf(notification) + _uiState.value.notifications.take(49)
        _uiState.value = _uiState.value.copy(notifications = updated)
        if (bannersAllowed()) {
            _toastEvents.tryEmit(notification.toToastData())
        }
    }

    fun dismissNotification(id: String) {
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.filter { it.id != id }
        )
    }

    fun dismissRemoteNotification(id: String) {
        _uiState.value = _uiState.value.copy(
            dismissedRemoteNotificationIds = _uiState.value.dismissedRemoteNotificationIds + id
        )
    }

    // ─── Generic notification portal ──────────────────────────────────────────
    // Any other part of the codebase (battery monitor, storage checks, update
    // checker, background jobs, etc.) can call this to raise a toast without
    // knowing anything about RealNotification/BluebirdRemoteNotification. Pass
    // an `id` for things that should only ever toast once at a time (e.g.
    // "battery_low") — a second call with the same id while it's still the
    // active toast just gets ignored by the host's de-dupe in most cases, but
    // callers doing repeat conditions (like a monitor loop) should still gate
    // themselves — see startBatteryMonitor() below for the pattern.
    fun postSystemNotification(
        title: String,
        body: String,
        id: String = UUID.randomUUID().toString(),
        appLabel: String = "Bluebird",
        accentColor: String = "#0078D4",
        actionLabel: String? = null,
        actionUrl: String? = null,
        addToHistory: Boolean = false
    ) {
        val accent = try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(accentColor))
        } catch (e: Exception) { androidx.compose.ui.graphics.Color(0xFF0078D4) }

        _toastEvents.tryEmit(
            ToastNotifData(
                id          = id,
                appLabel    = appLabel,
                title       = title,
                body        = body,
                accent      = accent,
                actionLabel = actionLabel,
                actionUrl   = actionUrl
            )
        )

        if (addToHistory) {
            addNotification(
                RealNotification(
                    id          = id,
                    packageName = "",
                    appName     = appLabel,
                    title       = title,
                    body        = body
                )
            )
        }
    }

    // ─── Remote (Bluebird team) announcements ─────────────────────────────────
    // Polls notify.json periodically. Newly-seen ids get pushed as toasts;
    // already-seen ids (persisted across restarts) are shown silently in the
    // Action Center panel only, so reinstalling/relaunching the app doesn't
    // replay every historical announcement as a popup.
    private val seenRemoteIds: MutableSet<String> by lazy {
        (prefs.getStringSet("seen_remote_notif_ids", emptySet()) ?: emptySet()).toMutableSet()
    }
    private var remoteBootstrapped = prefs.getBoolean("remote_notif_bootstrapped", false)

    private fun saveSeenRemoteIds() {
        prefs.edit().putStringSet("seen_remote_notif_ids", seenRemoteIds).apply()
    }

    /**
     * Explicitly refresh Bluebird announcements. There is intentionally no periodic
     * poll: opening/keeping the launcher alive must not create recurring network work.
     */
    fun refreshRemoteNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchRemoteNotificationsOnce()
        }
    }

    private suspend fun fetchRemoteNotificationsOnce() {
        try {
            val raw  = URL("https://raw.githubusercontent.com/Norbert-web/bluebird-releases/main/assets/bluebird/notify.json")
                .readText(Charsets.UTF_8)
            val root = JSONObject(raw)
            val arr  = root.getJSONArray("notifications")
            val list = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val action = if (obj.has("action") && !obj.isNull("action")) {
                    val a = obj.getJSONObject("action")
                    Pair(a.optString("label"), a.optString("url"))
                } else null
                BluebirdRemoteNotification(
                    id          = obj.getString("id"),
                    type        = obj.optString("type", "announcement"),
                    priority    = obj.optString("priority", "normal"),
                    title       = obj.getString("title"),
                    body        = obj.getString("body"),
                    timestamp   = obj.optString("timestamp", ""),
                    expiresAt   = if (obj.has("expires_at") && !obj.isNull("expires_at")) obj.getString("expires_at") else null,
                    actionLabel = action?.first,
                    actionUrl   = action?.second,
                    badgeColor  = obj.optString("badge_color", "#0078D4")
                )
            }

            val freshIds       = list.map { it.id }.filter { it !in seenRemoteIds }
            val isFirstEverRun = !remoteBootstrapped

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(remoteNotifications = list)
                if (bannersAllowed() && !isFirstEverRun) {
                    list.filter { it.id in freshIds }.forEach { notif ->
                        _toastEvents.tryEmit(notif.toToastData())
                    }
                }
            }

            if (freshIds.isNotEmpty()) {
                seenRemoteIds.addAll(freshIds)
                saveSeenRemoteIds()
            }
            if (isFirstEverRun) {
                remoteBootstrapped = true
                prefs.edit().putBoolean("remote_notif_bootstrapped", true).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissAllNotifications() {
        _uiState.value = _uiState.value.copy(notifications = emptyList())
    }

    private fun checkNotificationListenerPermission() {
        val context = getApplication<Application>()
        val enabled = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(isNotificationListenerEnabled = enabled)
    }

    fun requestNotificationListenerPermission(context: Context) {
        try {
            context.startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Overlays ─────────────────────────────────────────────────────────────
    fun toggleStartMenu() {
        _uiState.value = _uiState.value.copy(
            isStartMenuOpen          = !_uiState.value.isStartMenuOpen,
            isActionCenterOpen       = false, isSearchOpen = false,
            isPowerMenuOpen          = false, isDesktopContextMenuOpen = false, isWidgetsOpen = false
        )
    }

    fun toggleActionCenter() {
        _uiState.value = _uiState.value.copy(
            isActionCenterOpen       = !_uiState.value.isActionCenterOpen,
            isStartMenuOpen          = false, isSearchOpen = false,
            isPowerMenuOpen          = false, isDesktopContextMenuOpen = false
        )
    }

    fun toggleSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchOpen             = !_uiState.value.isSearchOpen,
            isStartMenuOpen          = false, isActionCenterOpen = false, isDesktopContextMenuOpen = false
        )
    }

    fun toggleWidgets() {
        _uiState.value = _uiState.value.copy(isWidgetsOpen = !_uiState.value.isWidgetsOpen)
    }

    fun togglePowerMenu() {
        _uiState.value = _uiState.value.copy(
            isPowerMenuOpen = !_uiState.value.isPowerMenuOpen, isStartMenuOpen = false
        )
    }

    fun dismissAllOverlays() {
        _uiState.value = _uiState.value.copy(
            isStartMenuOpen          = false, isActionCenterOpen = false, isSearchOpen = false,
            isWidgetsOpen            = false, isPowerMenuOpen    = false, isDesktopContextMenuOpen = false
        )
    }

    fun toggleTaskbar() {
        _uiState.value = _uiState.value.copy(isTaskbarVisible = !_uiState.value.isTaskbarVisible)
    }

    // ─── Windows ─────────────────────────────────────────────────────────────
    private fun readBpkIdentity(packageFile: File): Triple<String, String, String?> {
        var name = packageFile.nameWithoutExtension.ifBlank { "Bluebird Application" }
        var appId = packageFile.nameWithoutExtension.ifBlank { "unknown" }
        var iconPath: String? = null
        runCatching {
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                if (entry != null) {
                    val json = zip.getInputStream(entry).use { input -> JSONObject(input.bufferedReader(Charsets.UTF_8).readText()) }
                    name = json.optString("name", name).trim().ifBlank { name }
                    appId = json.optString("id", appId).trim().ifBlank { appId }
                }
            }
        }
        val cacheRoot = File(getApplication<Application>().filesDir, "bpk-icon-cache")
        iconPath = runCatching {
            io.github.norbertweb.bluebird.ui.components.BpkPackageIcon.cache(packageFile, cacheRoot)?.absolutePath
        }.getOrNull()
        return Triple(name, appId, iconPath)
    }

    fun openWindow(
        screen: LauncherScreen,
        extras: Map<String, String> = emptyMap(),
        customIconPath: String? = null
    ) {
        dismissAllOverlays()
        var effectiveExtras = extras
        var effectiveCustomIconPath = customIconPath
        var effectiveTitle: String? = null
        var effectivePackageName: String? = extras["appPackageName"]
        if (screen == LauncherScreen.PROGRAM_MANAGER && !extras["bpkPath"].isNullOrBlank()) {
            val packageFile = File(extras["bpkPath"]!!)
            if (packageFile.isFile) {
                val (bpkName, bpkId, cachedIcon) = readBpkIdentity(packageFile)
                effectiveTitle = "Install $bpkName"
                effectiveCustomIconPath = cachedIcon ?: customIconPath
                effectivePackageName = "bpk-installer:$bpkId"
                effectiveExtras = extras + mapOf(
                    "windowTitle" to effectiveTitle,
                    "appPackageName" to effectivePackageName.orEmpty(),
                    "windowIconPath" to (effectiveCustomIconPath ?: "")
                )
            }
        }
        // If a window for this screen already exists, restore + focus it (handles minimized case).
        // Packaged apps match on bpkAppId so the same application reopens/focuses consistently
        // whether launched from Desktop, File Explorer, Start, Taskbar, or Program Manager.
        val existing = _uiState.value.openWindows.find {
            if (screen == LauncherScreen.WEB_APP_VIEWER && it.screen == LauncherScreen.WEB_APP_VIEWER) {
                it.extras["bpkAppId"] == effectiveExtras["bpkAppId"]
            } else {
                it.screen == screen && it.extras == effectiveExtras
            }
        }
        if (existing != null) {
            restoreWindow(existing.id)
            return
        }

        val title = when (screen) {

            LauncherScreen.PremiumTextEditorScreen      -> "Text Editor"
            LauncherScreen.SETTINGS      -> "Settings"
            LauncherScreen.FILE_EXPLORER -> extras["path"]?.let { File(it).name.ifBlank { "File Explorer" } } ?: "File Explorer"
            LauncherScreen.BROWSER       -> "Bluebird Surfer Browser"
            LauncherScreen.TASK_MANAGER  -> "Task Manager"
            LauncherScreen.CALCULATOR    -> "Calculator"
            LauncherScreen.CALENDAR      -> "Calendar"
            LauncherScreen.PHOTOS        -> "Photos"
            LauncherScreen.MEDIA_PLAYER  -> "Media Player"
            LauncherScreen.IMAGE_VIEWER  -> extras["fileName"] ?: "Image Viewer"
            LauncherScreen.WORD_IMPRESS         -> "Word Impress"
            LauncherScreen.BLUEBIRD_STORE      -> "Bluebird Store"
            LauncherScreen.RECYCLE_BIN   -> "Recycle Bin"
            LauncherScreen.TERMINAL        -> "Terminal"
            LauncherScreen.PROGRAM_MANAGER -> "Program Manager"
            LauncherScreen.WEB_APP_VIEWER -> "Web Apps Viewer"

            else                         -> "Window"
        }
        val resolvedTitle = effectiveTitle ?: when (screen) {
            LauncherScreen.PROGRAM_MANAGER -> "Program Manager"
            else -> title
        }

        val iconKey = when (screen) {
            // Unique per web app (not the generic manager icon) so each installed
            // web app groups/stacks independently on the taskbar.
            LauncherScreen.WEB_APP_VIEWER -> "bpk:${extras["bpkAppId"] ?: "unknown"}"
            LauncherScreen.PremiumTextEditorScreen   -> WindowIconKey.PremiumTextEditorScreen
            LauncherScreen.SETTINGS      -> WindowIconKey.SETTINGS
            LauncherScreen.FILE_EXPLORER -> WindowIconKey.FILE_EXPLORER
            LauncherScreen.BROWSER       -> WindowIconKey.BROWSER
            LauncherScreen.CALCULATOR    -> WindowIconKey.CALCULATOR
            LauncherScreen.CALENDAR      -> WindowIconKey.CALENDAR
            LauncherScreen.PHOTOS        -> WindowIconKey.PHOTOS
            LauncherScreen.TASK_MANAGER  -> WindowIconKey.TASK_MANAGER
            LauncherScreen.MEDIA_PLAYER  -> WindowIconKey.MEDIA_PLAYER
            LauncherScreen.IMAGE_VIEWER  -> WindowIconKey.IMAGE_VIEWER
            LauncherScreen.WORD_IMPRESS         -> WindowIconKey.WORD_IMPRESS
            LauncherScreen.BLUEBIRD_STORE      -> WindowIconKey.BLUEBIRD_STORE
            LauncherScreen.RECYCLE_BIN   -> WindowIconKey.RECYCLE_BIN
            LauncherScreen.TERMINAL        -> WindowIconKey.TERMINAL



            else                         -> ""
        }

        val window  = WindowState(
            screen = screen, title = resolvedTitle, extras = effectiveExtras,
            iconKey = iconKey,
            appPackageName = effectivePackageName ?: effectiveExtras["appPackageName"] ?: "",
            customIconPath = effectiveCustomIconPath
        )
        val current = _uiState.value.openWindows.toMutableList()
        current.add(window)
        _uiState.value = _uiState.value.copy(openWindows = current, activeWindowId = window.id)
    }

    fun closeWindow(windowId: String) {
        val state = _uiState.value
        if (state.openWindows.none { it.id == windowId }) return
        val current = state.openWindows.filterNot { it.id == windowId }
        _uiState.value = state.copy(
            openWindows    = current,
            activeWindowId = current.lastOrNull { !it.isMinimized }?.id
        )
    }

    fun setActiveWindow(windowId: String) {
        val state = _uiState.value
        val current = state.openWindows
        val idx = current.indexOfFirst { it.id == windowId }
        if (idx < 0) return

        // Most focus events target the already-front window. Avoid allocating a
        // new list and triggering recomposition in that common case.
        if (idx == current.lastIndex && state.activeWindowId == windowId) return

        val reordered = ArrayList<WindowState>(current.size)
        current.forEachIndexed { i, window -> if (i != idx) reordered.add(window) }
        reordered.add(current[idx])
        _uiState.value = state.copy(openWindows = reordered, activeWindowId = windowId)
    }

    /** Always hides the window. Sets focus to the next visible window. */
    fun minimizeWindow(windowId: String) {
        val state = _uiState.value
        val target = state.openWindows.firstOrNull { it.id == windowId } ?: return
        if (target.isMinimized) return

        val updated = state.openWindows.map {
            if (it.id == windowId) it.copy(isMinimized = true) else it
        }
        val nextActive = updated.lastOrNull { !it.isMinimized && it.id != windowId }?.id
        _uiState.value = state.copy(
            openWindows    = updated,
            activeWindowId = if (state.activeWindowId == windowId) nextActive
            else state.activeWindowId
        )
    }

    /** Always un-hides the window and brings it to front. Safe on already-visible windows. */
    fun restoreWindow(windowId: String) {
        val state = _uiState.value
        val current = state.openWindows
        val idx = current.indexOfFirst { it.id == windowId }
        if (idx < 0) return

        val target = current[idx]
        val reordered = ArrayList<WindowState>(current.size)
        current.forEachIndexed { i, window -> if (i != idx) reordered.add(window) }
        reordered.add(if (target.isMinimized) target.copy(isMinimized = false) else target)
        _uiState.value = state.copy(openWindows = reordered, activeWindowId = windowId)
    }

    fun maximizeWindow(windowId: String) {
        val state = _uiState.value
        val target = state.openWindows.firstOrNull { it.id == windowId } ?: return
        if (target.screen == LauncherScreen.PROGRAM_MANAGER && !target.extras["bpkPath"].isNullOrBlank()) return
        val updated = state.openWindows.map {
            if (it.id == windowId) it.copy(isMaximized = !it.isMaximized) else it
        }
        _uiState.value = state.copy(openWindows = updated, activeWindowId = windowId)
    }

    // ─── App Launching & File Opening ─────────────────────────────────────────
    fun bpkAppInfo(app: InstalledBpkApp): AppInfo {
        val icon = runCatching { BitmapFactory.decodeFile(app.iconPath)?.let { BitmapDrawable(getApplication<Application>().resources, it) } }.getOrNull()
        return AppInfo(
            name = app.name,
            packageName = "bpk:${app.id}",
            icon = icon
        )
    }

    fun launchBpkApp(appId: String) {
        val app = BpkPackageManager(getApplication<Application>()).apps().firstOrNull { it.id == appId } ?: return
        dismissAllOverlays()
        openWindow(
            screen = LauncherScreen.WEB_APP_VIEWER,
            extras = mapOf(
                "bpkAppId" to app.id,
                "bpkAppName" to app.name,
                "bpkAppLocalDir" to app.installDir,
                "bpkAppEntry" to app.entry,
                "bpkAppExecutable" to app.executablePath,
                "appPackageName" to "bpk:${app.id}",
                "windowTitle" to app.name
            ),
            customIconPath = app.iconPath
        )
        val recent = _uiState.value.recentApps.toMutableList()
        val info = bpkAppInfo(app)
        recent.removeAll { it.packageName == info.packageName }
        recent.add(0, info)
        _uiState.value = _uiState.value.copy(recentApps = recent.take(10))
        saveAll()
    }

    fun completeBpkInstallation(
        installed: InstalledBpkApp,
        actions: io.github.norbertweb.bluebird.ui.components.BpkInstallActions
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val currentApps = BpkPackageManager(context).apps()
            val info = withContext(Dispatchers.Main) { bpkAppInfo(installed) }
            withContext(Dispatchers.Main) {
                val pinnedTaskbar = if (actions.pinToTaskbar) {
                    (_uiState.value.pinnedTaskbarApps + info).distinctBy { it.packageName }
                } else {
                    _uiState.value.pinnedTaskbarApps.distinctBy { it.packageName }
                }

                val pinnedStart = if (actions.addToStart) {
                    _uiState.value.pinnedBpkStartIds + installed.id
                } else {
                    _uiState.value.pinnedBpkStartIds - installed.id
                }

                _uiState.value = _uiState.value.copy(
                    installedBpkApps = currentApps,
                    pinnedTaskbarApps = pinnedTaskbar,
                    pinnedBpkStartIds = pinnedStart
                )
                prefs.edit()
                    .putString("pinned_bpk_start", gson.toJson(pinnedStart.toList()))
                    .apply()
                saveAll()

                if (actions.createDesktopShortcut) {
                    addDesktopShortcutFromFile(
                        installed.executablePath,
                        installed.name
                    )
                }
            }
        }
    }

    /** Reinstall using the cached package while preserving shell placement preferences. */
    fun reinstallBpkApplication(appId: String): Job = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        BpkPackageManager(context).reinstall(appId)
        withContext(Dispatchers.Main.immediate) {
            refreshInstalledApplicationSnapshot()
        }
    }

    /** Synchronous-in-coroutine variant used by Program Manager actions. */
    suspend fun reinstallBpkApplicationAndWait(appId: String) {
        val context = getApplication<Application>()
        withContext(Dispatchers.IO) { BpkPackageManager(context).reinstall(appId) }
        withContext(Dispatchers.Main.immediate) { refreshInstalledApplicationSnapshot() }
    }

    /** Synchronous-in-coroutine uninstall used by Program Manager so errors are visible. */
    suspend fun uninstallBpkApplicationAndWait(appId: String) {
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val registryApp = BpkPackageManager(context).apps().firstOrNull { it.id == appId }
                ?: throw IllegalArgumentException("Application is not installed")
            withContext(Dispatchers.Main.immediate) {
                val ids = _uiState.value.openWindows
                    .filter { it.screen == LauncherScreen.WEB_APP_VIEWER && it.extras["bpkAppId"] == appId }
                    .map { it.id }
                ids.forEach(::closeWindow)
                val packageName = "bpk:$appId"
                _uiState.value = _uiState.value.copy(
                    pinnedTaskbarApps = _uiState.value.pinnedTaskbarApps.filterNot { it.packageName == packageName },
                    pinnedBpkStartIds = _uiState.value.pinnedBpkStartIds - appId
                )
            }
            desktopDir.listFiles()?.forEach { shortcut ->
                if (!shortcut.isFile || !shortcut.name.endsWith(".desktop", true)) return@forEach
                val contents = runCatching { shortcut.readText(Charsets.UTF_8) }.getOrDefault("")
                if (contents.lines().any { it.startsWith("path=") && it.removePrefix("path=").trim() == registryApp.executablePath }) {
                    runCatching { shortcut.delete() }
                }
            }
            check(BpkPackageManager(context).uninstall(appId)) { "Could not uninstall application" }
        }
        withContext(Dispatchers.Main.immediate) {
            refreshDesktopFiles()
            refreshInstalledApplicationSnapshot()
            saveAll()
        }
    }

    private fun refreshInstalledApplicationSnapshot() {
        val context = getApplication<Application>()
        val apps = BpkPackageManager(context).apps().distinctBy { it.id }
        val bpkInfos = apps.map { bpkAppInfo(it) }.distinctBy { it.packageName }
        val pinnedPackages = _uiState.value.pinnedTaskbarApps.map { it.packageName }.toSet()
        val pinnedTaskbar = _uiState.value.pinnedTaskbarApps
            .asSequence()
            .filterNot { it.packageName.startsWith("bpk:") }
            .distinctBy { it.packageName }
            .toMutableList()
        pinnedTaskbar.addAll(
            apps.asSequence()
                .filter { it.packageNameLike() in pinnedPackages }
                .map { bpkAppInfo(it) }
                .distinctBy { it.packageName }
        )
        _uiState.value = _uiState.value.copy(
            installedBpkApps = apps,
            installedApps = _uiState.value.installedApps.filterNot { it.packageName.startsWith("bpk:") } + bpkInfos,
            pinnedTaskbarApps = pinnedTaskbar
        )
    }

    private fun InstalledBpkApp.packageNameLike(): String = "bpk:$id"

    fun pinBpkToStart(appId: String) {
        val updated = _uiState.value.pinnedBpkStartIds + appId
        _uiState.value = _uiState.value.copy(pinnedBpkStartIds = updated)
        prefs.edit().putString("pinned_bpk_start", gson.toJson(updated.toList())).apply()
    }

    fun unpinBpkFromStart(appId: String) {
        val updated = _uiState.value.pinnedBpkStartIds - appId
        _uiState.value = _uiState.value.copy(pinnedBpkStartIds = updated)
        prefs.edit().putString("pinned_bpk_start", gson.toJson(updated.toList())).apply()
    }

    fun openApp(context: Context, appInfo: AppInfo) {
        dismissAllOverlays()
        if (appInfo.packageName.startsWith("bpk:")) {
            val appId = appInfo.packageName.removePrefix("bpk:")
            launchBpkApp(appId)
            return
        }
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            intent?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it) }
            val recent = _uiState.value.recentApps.toMutableList()
            recent.removeAll { it.packageName == appInfo.packageName }
            recent.add(0, appInfo)
            _uiState.value = _uiState.value.copy(recentApps = recent.take(10))
            saveAll()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun openFileWithSystem(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            val uri  = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val mimeType = context.contentResolver.getType(uri) ?: getMimeType(filePath)
            val intent   = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            val files = _uiState.value.recentFiles.toMutableList()
            files.remove(filePath)
            files.add(0, filePath)
            _uiState.value = _uiState.value.copy(recentFiles = files.take(10))
            saveAll()
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Opens a file in Bluebird's own applications whenever the format is supported.
     * The system chooser remains the fallback for formats Bluebird cannot handle.
     */
    /**
     * Resolves a SAF URI to the actual filesystem path. Shortcuts intentionally require
     * this real path so the target is never copied into Bluebird's cache.
     */
    fun resolveSafUriToFilePath(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme?.lowercase()) {
                "file" -> uri.path?.let(::File)?.absolutePath?.takeIf { File(it).exists() }
                "content" -> {
                    // External Storage provider: support both file/document URIs and tree URIs.
                    if (uri.authority == "com.android.externalstorage.documents") {
                        val docId = runCatching {
                            android.provider.DocumentsContract.getDocumentId(uri)
                        }.getOrNull()
                            ?: runCatching {
                                android.provider.DocumentsContract.getTreeDocumentId(uri)
                            }.getOrNull()
                        if (!docId.isNullOrBlank()) {
                            val split = docId.split(":", limit = 2)
                            if (split.size == 2) {
                                val root = if (split[0].equals("primary", true)) {
                                    Environment.getExternalStorageDirectory()
                                } else {
                                    File("/storage/${split[0]}")
                                }
                                File(root, split[1]).absolutePath.takeIf { File(it).exists() }?.let { return it }
                            }
                        }
                    }

                    // Some local providers expose the backing path through DATA. If it is
                    // unavailable, there is deliberately no cache/copy fallback.
                    runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(android.provider.MediaStore.MediaColumns.DATA),
                            null, null, null
                        )?.use { c ->
                            if (c.moveToFirst()) {
                                val column = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                if (column >= 0) {
                                    c.getString(column)?.let { path ->
                                        File(path).absolutePath.takeIf { File(it).exists() }
                                    }
                                } else null
                            } else null
                        }
                    }.getOrNull()
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    fun openUriWithSystem(context: Context, uri: Uri) {
        try {
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Opens a persisted SAF shortcut target only when Android can resolve it to a real
     * filesystem path. A shortcut is a pointer, never a cached copy of its target.
     */
    fun openFileInternallyUri(context: Context, uri: Uri, displayName: String? = null): Boolean {
        val resolvedPath = resolveSafUriToFilePath(context, uri) ?: return false
        return openFileInternally(context, resolvedPath)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        } catch (_: Exception) { null }
    }

    fun openFileInternally(context: Context, filePath: String): Boolean {
        val file = File(filePath)
        if (!file.isFile) return false
        val ext = file.extension.lowercase()
        val screenAndExtras = when {
            ext == "exe" -> {
                val descriptor = BluebirdExecutable.read(file) ?: return false
                val root = runCatching { BluebirdExecutable.resolveSourceRoot(file, descriptor) }.getOrNull() ?: return false
                val entry = runCatching { BluebirdExecutable.resolveEntry(file, descriptor) }.getOrNull() ?: return false
                if (!entry.isFile || !(entry.path == root.path || entry.path.startsWith(root.path + File.separator))) return false
                LauncherScreen.WEB_APP_VIEWER to mapOf(
                    "bpkAppId" to descriptor.appId,
                    "bpkAppName" to descriptor.name,
                    "bpkAppLocalDir" to root.absolutePath,
                    "bpkAppEntry" to entry.relativeTo(root).path,
                    "bpkAppExecutable" to file.absolutePath
                )
            }
            ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp") ->
                LauncherScreen.IMAGE_VIEWER to mapOf("filePath" to filePath)
            ext in setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "mp4", "mkv", "avi", "mov", "webm", "3gp") ->
                LauncherScreen.MEDIA_PLAYER to mapOf("filePath" to filePath)
            ext in setOf("txt", "log", "md", "xml", "json", "csv", "ini", "cfg", "conf", "properties",
                "c", "h", "cc", "cpp", "cxx", "hpp", "java", "kt", "kts", "js", "jsx", "ts", "tsx",
                "py", "rb", "go", "rs", "swift", "dart", "php", "sh", "bash", "zsh", "sql", "css",
                "scss", "sass", "less", "html", "htm", "xhtml", "vue", "svelte", "yaml", "yml", "toml",
                "gradle", "gitignore", "env") ->
                LauncherScreen.PremiumTextEditorScreen to mapOf("filePath" to filePath)
            ext in setOf("pdf", "doc", "docx", "wdoc", "rtf", "odt") ->
                LauncherScreen.WORD_IMPRESS to mapOf("filePath" to filePath)
            else -> return false
        }
        openWindow(screenAndExtras.first, screenAndExtras.second)

        // Keep desktop and File Explorer launches consistent: internally opened files
        // participate in the same recent-files list as files opened through the chooser.
        val recent = _uiState.value.recentFiles.toMutableList()
        recent.remove(filePath)
        recent.add(0, filePath)
        _uiState.value = _uiState.value.copy(recentFiles = recent.take(10))
        saveAll()
        return true
    }

    private fun getMimeType(filePath: String): String = when (filePath.substringAfterLast(".").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
        "mp4", "mkv", "avi", "mov", "webm"         -> "video/*"
        "mp3", "wav", "ogg", "flac", "aac"         -> "audio/*"
        "pdf"                                        -> "application/pdf"
        "txt"                                        -> "text/plain"
        "html", "htm"                                -> "text/html"
        "apk"                                        -> "application/vnd.android.package-archive"
        "io.github.norbertweb.io.github.norbertweb.bluebird"                                    -> "application/x-io.github.norbertweb.bluebird-shortcut"
        "webapp"                                     -> "application/x-io.github.norbertweb.bluebird-webapp"
        else                                         -> "*/*"
    }

    // ─── Unified clipboard — replaces Desktop's and File Explorer's separate,
    //     incompatible clipboards (one held a list, the other only a single file) ───

    fun setClipboard(files: List<File>, cut: Boolean) {
        _uiState.value = _uiState.value.copy(
            clipboardFiles = files.map { it.absolutePath },
            clipboardCut   = cut
        )
    }

    fun clearClipboard() {
        _uiState.value = _uiState.value.copy(clipboardFiles = emptyList(), clipboardCut = false)
    }

    fun pasteClipboard(destDir: File) {
        val state = _uiState.value
        if (state.clipboardFiles.isEmpty()) return
        val files = state.clipboardFiles.map { File(it) }.filter { it.exists() }
        val wasCut = state.clipboardCut
        enqueueFileOperation(files, destDir, isCut = wasCut)
        // Single-use paste: clipboard clears after the first paste, whether it was a
        // copy or a cut — pasting again without copying/cutting again does nothing.
        clearClipboard()
    }

    // ─── Copy/Move engine ──────────────────────────────────────────────────────
    // Single implementation for both Desktop and File Explorer: recursive (handles
    // folders), byte-progress reported live, cancelable, and an instant same-volume
    // rename fast path for moves (matches real-OS behavior — no progress bar needed
    // for a move that's really just a rename).

    private val cancelledJobIds = ConcurrentHashMap.newKeySet<String>()
    private var undoDismissJob: kotlinx.coroutines.Job? = null

    fun enqueueFileOperation(sources: List<File>, destDir: File, isCut: Boolean) {
        if (sources.isEmpty()) return
        val job = CopyJob(
            operation   = if (isCut) CopyOpType.MOVE else CopyOpType.COPY,
            sourceNames = sources.map { it.name },
            destDir     = destDir.absolutePath
        )
        _uiState.value = _uiState.value.copy(copyJobs = _uiState.value.copyJobs + job)
        if (_uiState.value.openWindows.none { it.screen == LauncherScreen.COPY_PROGRESS }) {
            openWindow(LauncherScreen.COPY_PROGRESS)
        }
        viewModelScope.launch(Dispatchers.IO) { runCopyJob(job.id, sources, destDir, isCut) }
    }

    fun cancelCopyJob(id: String) { cancelledJobIds.add(id) }

    fun dismissCopyJob(id: String) {
        val remaining = _uiState.value.copyJobs.filter { it.id != id }
        _uiState.value = _uiState.value.copy(copyJobs = remaining)
        if (remaining.isEmpty()) {
            _uiState.value.openWindows.find { it.screen == LauncherScreen.COPY_PROGRESS }
                ?.let { closeWindow(it.id) }
        }
    }

    private fun updateJob(id: String, transform: (CopyJob) -> CopyJob) {
        _uiState.value = _uiState.value.copy(
            copyJobs = _uiState.value.copyJobs.map { if (it.id == id) transform(it) else it }
        )
    }

    private fun uniqueDestName(destDir: File, name: String): String {
        if (!File(destDir, name).exists()) return name
        val dot  = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext  = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (File(destDir, "$base ($i)$ext").exists()) i++
        return "$base ($i)$ext"
    }

    private suspend fun runCopyJob(jobId: String, sources: List<File>, destDir: File, isCut: Boolean) {
        try {
            // Refuse recursive self-copies/moves before touching the filesystem.
            val destCanonical = destDir.canonicalFile
            val invalidSource = sources.firstOrNull { src ->
                val sourceCanonical = src.canonicalFile
                sourceCanonical == destCanonical ||
                    (src.isDirectory && destCanonical.path.startsWith(sourceCanonical.path + File.separator))
            }
            if (invalidSource != null) {
                throw IOException("Cannot copy or move an item into itself")
            }

            // A move on the same filesystem is normally just a rename. Try that
            // BEFORE doing an expensive recursive size scan. This is the common
            // case for File Explorer/Desktop moves and makes large-folder moves
            // effectively instantaneous.
            val undoList = mutableListOf<Pair<File, File>>()  // (dest, originalSource)
            val fallbackSources = mutableListOf<File>()
            var completedItems = 0

            if (isCut) {
                for (src in sources) {
                    if (jobId in cancelledJobIds) break
                    val destTarget = File(destDir, uniqueDestName(destDir, src.name))
                    if (src.renameTo(destTarget)) {
                        undoList.add(destTarget to src)
                        completedItems++
                        updateJob(jobId) {
                            it.copy(
                                status = CopyJobStatus.RUNNING,
                                currentFileName = src.name,
                                filesDone = completedItems
                            )
                        }
                    } else {
                        fallbackSources.add(src)
                    }
                }
            } else {
                fallbackSources.addAll(sources)
            }

            // Only COPY operations, or MOVE operations that could not be renamed,
            // need recursive metadata scanning. Previously every move scanned the
            // entire source tree before even attempting rename.
            var totalBytes = 0L
            var totalFiles = completedItems
            if (fallbackSources.isNotEmpty()) {
                updateJob(jobId) { it.copy(status = CopyJobStatus.SCANNING) }
                for (src in fallbackSources) {
                    if (jobId in cancelledJobIds) break
                    src.walkTopDown().forEach { f ->
                        if (jobId in cancelledJobIds) return@forEach
                        if (f.isFile) {
                            totalBytes += f.length()
                            totalFiles++
                        }
                    }
                }
            }

            // If every MOVE succeeded through rename, finish without any recursive
            // scan. The progress window will close normally after the completed job.
            if (fallbackSources.isEmpty()) {
                val cancelled = jobId in cancelledJobIds
                cancelledJobIds.remove(jobId)
                updateJob(jobId) {
                    it.copy(
                        status = if (cancelled) CopyJobStatus.CANCELLED else CopyJobStatus.DONE,
                        copiedBytes = 0L,
                        filesDone = completedItems,
                        totalFiles = completedItems,
                        totalBytes = 0L
                    )
                }
                if (!cancelled && undoList.isNotEmpty()) {
                    val verb = "Moved"
                    val label = if (sources.size == 1) "$verb \"${sources[0].name}\"" else "$verb ${sources.size} items"
                    showUndoAction(label) {
                        undoList.forEach { (dest, originalSrc) ->
                            if (!dest.renameTo(originalSrc)) {
                                dest.copyRecursively(originalSrc, overwrite = true)
                                dest.deleteRecursively()
                            }
                        }
                        refreshDesktopFiles()
                    }
                }
                withContext(Dispatchers.Main) { refreshDesktopFiles() }
                delay(3000)
                if (_uiState.value.copyJobs.any { it.id == jobId && it.status != CopyJobStatus.RUNNING }) {
                    dismissCopyJob(jobId)
                }
                return
            }

            updateJob(jobId) {
                it.copy(
                    totalBytes = totalBytes,
                    totalFiles = totalFiles,
                    status = CopyJobStatus.RUNNING,
                    filesDone = completedItems
                )
            }

            var copiedBytes = 0L
            var filesDone = completedItems
            var lastUpdate = System.currentTimeMillis()
            var lastBytesForSpeed = 0L

            fun copyFileWithProgress(from: File, to: File) {
                to.parentFile?.mkdirs()
                from.inputStream().use { input ->
                    to.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            if (jobId in cancelledJobIds) throw IOException("cancelled")
                            output.write(buffer, 0, read)
                            copiedBytes += read
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 150) {
                                val elapsed = (now - lastUpdate).coerceAtLeast(1)
                                val speed = (copiedBytes - lastBytesForSpeed) * 1000 / elapsed
                                lastBytesForSpeed = copiedBytes
                                lastUpdate = now
                                updateJob(jobId) {
                                    it.copy(
                                        copiedBytes = copiedBytes,
                                        currentFileName = from.name,
                                        speedBytesPerSec = speed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            for (src in fallbackSources) {
                if (jobId in cancelledJobIds) break
                val destTarget = File(destDir, uniqueDestName(destDir, src.name))
                try {
                    if (src.isDirectory) {
                        src.walkTopDown().forEach { f ->
                            if (jobId in cancelledJobIds) return@forEach
                            val rel = f.relativeTo(src)
                            val out = File(destTarget, rel.path)
                            if (f.isDirectory) {
                                out.mkdirs()
                            } else {
                                copyFileWithProgress(f, out)
                                filesDone++
                            }
                        }
                    } else {
                        copyFileWithProgress(src, destTarget)
                        filesDone++
                    }
                    if (jobId !in cancelledJobIds) {
                        undoList.add(destTarget to src)
                        if (isCut) src.deleteRecursively()
                    }
                } catch (ce: IOException) {
                    if (jobId in cancelledJobIds) {
                        destTarget.deleteRecursively()
                        break
                    } else {
                        throw ce
                    }
                }
                updateJob(jobId) { it.copy(filesDone = filesDone, copiedBytes = copiedBytes) }
            }

            val cancelled = jobId in cancelledJobIds
            cancelledJobIds.remove(jobId)
            updateJob(jobId) {
                it.copy(
                    status = if (cancelled) CopyJobStatus.CANCELLED else CopyJobStatus.DONE,
                    copiedBytes = copiedBytes,
                    filesDone = filesDone
                )
            }

            if (!cancelled && undoList.isNotEmpty()) {
                val verb = if (isCut) "Moved" else "Copied"
                val label = if (sources.size == 1) "$verb \"${sources[0].name}\"" else "$verb ${sources.size} items"
                showUndoAction(label) {
                    undoList.forEach { (dest, originalSrc) ->
                        if (isCut) {
                            if (!dest.renameTo(originalSrc)) {
                                dest.copyRecursively(originalSrc, overwrite = true)
                                dest.deleteRecursively()
                            }
                        } else {
                            dest.deleteRecursively()
                        }
                    }
                    refreshDesktopFiles()
                }
            }

            withContext(Dispatchers.Main) { refreshDesktopFiles() }

            delay(3000)
            if (_uiState.value.copyJobs.any { it.id == jobId && it.status != CopyJobStatus.RUNNING }) {
                dismissCopyJob(jobId)
            }
        } catch (e: Exception) {
            cancelledJobIds.remove(jobId)
            updateJob(jobId) { it.copy(status = CopyJobStatus.ERROR, error = e.message ?: "Copy failed") }
        }
    }

    // ─── Undo toast — a few seconds to reverse the last move/copy/delete ───

    fun showUndoAction(label: String, action: suspend () -> Unit) {
        undoDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(undoAction = UndoAction(label, action))
        undoDismissJob = viewModelScope.launch {
            delay(5000)
            if (_uiState.value.undoAction?.label == label) {
                _uiState.value = _uiState.value.copy(undoAction = null)
            }
        }
    }

    fun performUndo() {
        val action = _uiState.value.undoAction ?: return
        _uiState.value = _uiState.value.copy(undoAction = null)
        undoDismissJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            action.perform()
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun dismissUndo() {
        undoDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(undoAction = null)
    }

    // ─── Screen-awake-during-media control ─────────────────────────────────────
    // Call setMediaPlaying(id, true) when a player (browser video, Media Player app,
    // etc.) starts actual playback, and setMediaPlaying(id, false) on pause/stop/
    // completion/error/screen-leave. `id` should be stable per player instance (e.g.
    // the window id) so two players can't clobber each other's state — the screen
    // stays awake as long as at least one id is actively playing.
    fun setMediaPlaying(id: String, isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(
            activeMediaPlaybackIds = if (isPlaying)
                _uiState.value.activeMediaPlaybackIds + id
            else
                _uiState.value.activeMediaPlaybackIds - id
        )
    }

    // Call when a player's window closes, so a forgotten setMediaPlaying(false)
    // can't leave a stale id keeping the screen on forever.
    fun clearMediaPlaying(id: String) {
        _uiState.value = _uiState.value.copy(
            activeMediaPlaybackIds = _uiState.value.activeMediaPlaybackIds - id
        )
    }

    // ─── Theme & Appearance ───────────────────────────────────────────────────
    fun toggleTheme() {
        _uiState.value = _uiState.value.copy(isDarkTheme = !_uiState.value.isDarkTheme)
        saveAll()
    }

    fun setAccentColor(color: Long) {
        _uiState.value = _uiState.value.copy(accentColor = color)
        saveAll()
    }

    fun setAppTheme(theme: AppTheme) {
        val dark = when (theme) {
            AppTheme.DARK    -> true
            AppTheme.LIGHT   -> false
            AppTheme.SPECIAL -> true
            else             -> _uiState.value.isDarkTheme
        }
        _uiState.value = _uiState.value.copy(appTheme = theme, isDarkTheme = dark)
        saveAll()
    }

    fun setTextScale(scale: Float) {
        _uiState.value = _uiState.value.copy(textScale = scale.coerceIn(0.8f, 1.4f))
        saveAll()
    }

    fun setIconSize(size: String) {
        _uiState.value = _uiState.value.copy(iconSize = size); saveAll()
    }

    fun setTaskbarPosition(position: String) {
        _uiState.value = _uiState.value.copy(taskbarPosition = position); saveAll()
    }

    fun setStartMenuLayout(layout: String) {
        _uiState.value = _uiState.value.copy(startMenuLayout = layout); saveAll()
    }

    fun setTransparencyEffects(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(transparencyEffects = enabled); saveAll()
    }

    fun setAnimationSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(animationSpeed = speed); saveAll()
    }

    // ─── System Settings ──────────────────────────────────────────────────────
    fun setLaunchOnBoot(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(launchOnBoot = enabled); saveAll()
    }

    fun setSnapLayouts(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(snapLayouts = enabled); saveAll()
    }

    fun setClipboardHistory(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(clipboardHistory = enabled); saveAll()
    }

    fun setNotificationBanners(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationBanners = enabled); saveAll()
    }

    fun setShowNotificationBadges(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showNotificationBadges = enabled); saveAll()
    }

    fun setDndEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dndEnabled = enabled); saveAll()
    }

    fun setDndScheduled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dndScheduled = enabled); saveAll()
    }

    fun setFocusAssist(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(focusAssist = enabled); saveAll()
    }

    fun setScreenTimeout(minutes: Int) {
        _uiState.value = _uiState.value.copy(screenTimeoutMinutes = minutes); saveAll()
    }

    // ─── Sound Settings ───────────────────────────────────────────────────────
    fun setMediaVolume(value: Float, context: Context) {
        _uiState.value = _uiState.value.copy(mediaVolume = value)
        try {
            val am     = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (value * maxVol).toInt(), 0)
        } catch (e: Exception) { e.printStackTrace() }
        saveAll()
    }

    fun setRingtoneVolume(value: Float, context: Context) {
        _uiState.value = _uiState.value.copy(ringtoneVolume = value)
        try {
            val am     = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, (value * maxVol).toInt(), 0)
        } catch (e: Exception) { e.printStackTrace() }
        saveAll()
    }

    fun setNotifVolume(value: Float, context: Context) {
        _uiState.value = _uiState.value.copy(notifVolume = value)
        try {
            val am     = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (value * maxVol).toInt(), 0)
        } catch (e: Exception) { e.printStackTrace() }
        saveAll()
    }

    fun setSystemSounds(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(systemSounds = enabled); saveAll()
    }

    fun setHapticFeedback(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hapticFeedback = enabled); saveAll()
    }

    fun setNotifSound(sound: String) {
        _uiState.value = _uiState.value.copy(notifSound = sound); saveAll()
    }

    // ─── Network extras ───────────────────────────────────────────────────────
    fun setDataSaver(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dataSaver = enabled); saveAll()
    }

    fun setHotspot(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hotspotEnabled = enabled); saveAll()
    }

    fun setVpn(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vpnEnabled = enabled); saveAll()
    }

    fun setCustomDns(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(customDns = enabled); saveAll()
    }

    fun setDnsAddress(address: String) {
        _uiState.value = _uiState.value.copy(dnsAddress = address); saveAll()
    }

    // ─── Gaming ───────────────────────────────────────────────────────────────
    fun setGameMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(gameModeEnabled = enabled); saveAll()
    }

    fun setFrameRateCap(cap: String) {
        _uiState.value = _uiState.value.copy(frameRateCap = cap); saveAll()
    }

    fun setPerformanceOverlay(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(performanceOverlay = enabled); saveAll()
    }

    fun setDndWhileGaming(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dndWhileGaming = enabled); saveAll()
    }

    fun setHapticInGames(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hapticInGames = enabled); saveAll()
    }

    // ─── Accessibility ────────────────────────────────────────────────────────
    fun setHighContrast(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(highContrast = enabled); saveAll()
    }

    fun setLargerText(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(largerText = enabled); saveAll()
    }

    fun setBoldText(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(boldText = enabled); saveAll()
    }

    fun setReduceMotion(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reduceMotion = enabled); saveAll()
    }

    fun setMonoAudio(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(monoAudio = enabled); saveAll()
    }

    fun setButtonShapes(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(buttonShapes = enabled); saveAll()
    }

    fun setColorCorrectionMode(mode: String) {
        _uiState.value = _uiState.value.copy(colorCorrectionMode = mode); saveAll()
    }

    fun setTouchHoldDelay(delay: Float) {
        _uiState.value = _uiState.value.copy(touchHoldDelay = delay); saveAll()
    }

    // ─── Privacy & Security ───────────────────────────────────────────────────
    fun setScreenLock(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(screenLock = enabled); saveAll()
    }

    fun setBiometricUnlock(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(biometricUnlock = enabled); saveAll()
    }

    fun setUnknownSources(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(unknownSources = enabled); saveAll()
    }

    fun setLocationAccess(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(locationAccess = enabled); saveAll()
    }

    fun setCameraAccess(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cameraAccess = enabled); saveAll()
    }

    fun setMicAccess(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(micAccess = enabled); saveAll()
    }

    fun setUsageDiagnostics(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(usageDiagnostics = enabled); saveAll()
    }

    // ─── Time & Language ──────────────────────────────────────────────────────
    fun setUse24HourClock(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(use24HourClock = enabled); saveAll()
    }

    fun setAutoSetTime(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSetTime = enabled); saveAll()
    }

    fun setTimeZone(zone: String) {
        _uiState.value = _uiState.value.copy(timeZone = zone); saveAll()
    }

    fun setDateFormat(format: String) {
        _uiState.value = _uiState.value.copy(dateFormat = format); saveAll()
    }

    fun setFirstDayOfWeek(day: String) {
        _uiState.value = _uiState.value.copy(firstDayOfWeek = day); saveAll()
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    fun setAutoUpdate(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoUpdate = enabled); saveAll()
    }

    fun setUpdateChannel(channel: String) {
        _uiState.value = _uiState.value.copy(updateChannel = channel); saveAll()
    }

    // ─── User profile setters ─────────────────────────────────────────────────
    fun setUserNameAndSave(name: String) {
        _uiState.value = _uiState.value.copy(userProfile = _uiState.value.userProfile.copy(userName = name))
        saveAll()
    }

    // ─── Quick settings ───────────────────────────────────────────────────────
    fun setVolume(value: Float, context: Context) {
        _uiState.value = _uiState.value.copy(volume = value)
        try {
            val am     = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (value * maxVol).toInt(), 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setBrightness(value: Float, context: Context) {
        _uiState.value = _uiState.value.copy(brightness = value)
        // Caller should apply via WindowManager.LayoutParams — no WRITE_SETTINGS needed
    }

    fun toggleWifi() {
        _uiState.value = _uiState.value.copy(isWifiOn = !_uiState.value.isWifiOn)
    }

    fun openWifiSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleBluetooth() {
        _uiState.value = _uiState.value.copy(isBluetoothOn = !_uiState.value.isBluetoothOn)
    }

    fun openBluetoothSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleAirplaneMode() {
        _uiState.value = _uiState.value.copy(isAirplaneMode = !_uiState.value.isAirplaneMode)
    }

    // ─── Lock screen ──────────────────────────────────────────────────────────
    fun lockScreen() {
        dismissAllOverlays()
        _uiState.value = _uiState.value.copy(isLocked = true)
    }

    fun unlockScreen() {
        _uiState.value = _uiState.value.copy(isLocked = false)
    }

    // ─── Power ────────────────────────────────────────────────────────────────
    fun performPowerAction(context: Context, action: PowerAction) {
        when (action) {
            PowerAction.LOCK, PowerAction.SLEEP,
            PowerAction.RESTART, PowerAction.SHUTDOWN -> lockScreen()
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    val filteredApps: List<AppInfo>
        get() {
            val query = _uiState.value.searchQuery
            return if (query.isBlank()) _uiState.value.installedApps
            else _uiState.value.installedApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }

    // ─── Clock ────────────────────────────────────────────────────────────────
    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                val cal         = Calendar.getInstance()
                val hour        = cal.get(Calendar.HOUR_OF_DAY)
                val min         = cal.get(Calendar.MINUTE)
                val amPm        = if (hour < 12) "AM" else "PM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                val sdf         = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
                _uiState.value  = _uiState.value.copy(
                    currentTime = "$displayHour:${min.toString().padStart(2, '0')} $amPm",
                    currentDate = sdf.format(cal.time)
                )
                delay(30_000)
            }
        }
    }

    // Tracks which low-battery thresholds have already toasted this charge
    // cycle, so the monitor (which polls every 60s) doesn't re-fire the same
    // warning on every tick while the level sits below the line.
    private val batteryToastsFiredThisCycle = mutableSetOf<Int>()

    // BatteryManager is polled here only once at startup. Ongoing changes come
    // from Android's battery broadcast instead of waking a coroutine every 60s.
    // This avoids periodic work while the launcher is idle and also reacts
    // immediately to plug/unplug and meaningful level changes.
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryFromIntent(intent)
        }
    }

    private fun startBatteryMonitor() {
        val app = getApplication<Application>()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = app.registerReceiver(batteryReceiver, filter)
        if (sticky != null) updateBatteryFromIntent(sticky)
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        try {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            if (level !in 0..scale) return
            val percent = (level * 100 / scale).coerceIn(0, 100)
            val old = _uiState.value
            if (old.batteryLevel == percent && old.isCharging == charging) return
            _uiState.value = old.copy(batteryLevel = percent, isCharging = charging)
            if (charging) {
                batteryToastsFiredThisCycle.clear()
            } else {
                checkBatteryThreshold(percent, 20, "Battery is getting low", "20% remaining. Consider plugging in.")
                checkBatteryThreshold(percent, 10, "Battery low", "10% remaining. Save your work soon.")
                checkBatteryThreshold(percent, 5, "Battery critically low", "5% remaining. Plug in now to avoid losing work.")
            }
        } catch (_: Exception) {
            // Battery state is non-critical; keep the last known value.
        }
    }

    private fun checkBatteryThreshold(level: Int, threshold: Int, title: String, body: String) {
        if (level <= threshold && threshold !in batteryToastsFiredThisCycle) {
            batteryToastsFiredThisCycle.add(threshold)
            postSystemNotification(
                id          = "battery_low_$threshold",
                title       = title,
                body        = body,
                accentColor = if (threshold <= 10) "#E81123" else "#FFB900"
            )
        }
    }

    // ─── Desktop Context Menu ─────────────────────────────────────────────────
    fun openDesktopContextMenu(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(
            isDesktopContextMenuOpen = true,
            desktopContextMenuX = x, desktopContextMenuY = y
        )
    }

    fun closeDesktopContextMenu() {
        _uiState.value = _uiState.value.copy(isDesktopContextMenuOpen = false)
    }

    // ─── Appearance (extended) ────────────────────────────────────────────────
    fun setLauncherFont(font: String) {
        _uiState.value = _uiState.value.copy(launcherFont = font); saveAll()
    }

    fun setGridSize(size: String) {
        _uiState.value = _uiState.value.copy(gridSize = size); saveAll()
    }

    fun setShowAppLabels(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAppLabels = show); saveAll()
    }

    fun setCornerRadius(radius: Float) {
        _uiState.value = _uiState.value.copy(cornerRadius = radius); saveAll()
    }

    fun setStatusBarClockPosition(position: String) {
        _uiState.value = _uiState.value.copy(statusBarClockPosition = position); saveAll()
    }

    fun setDarkModeSchedule(schedule: String) {
        _uiState.value = _uiState.value.copy(darkModeSchedule = schedule); saveAll()
    }

    fun setWallpaperSlideshow(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(wallpaperSlideshow = enabled); saveAll()
    }

    fun setWallpaperSlideshowInterval(interval: String) {
        _uiState.value = _uiState.value.copy(wallpaperSlideshowInterval = interval); saveAll()
    }

    // ─── Gestures ─────────────────────────────────────────────────────────────
    fun setGestureSwipeUp(action: String) {
        _uiState.value = _uiState.value.copy(gestureSwipeUp = action); saveAll()
    }

    fun setGestureSwipeDown(action: String) {
        _uiState.value = _uiState.value.copy(gestureSwipeDown = action); saveAll()
    }

    fun setGestureDoubleTap(action: String) {
        _uiState.value = _uiState.value.copy(gestureDoubleTap = action); saveAll()
    }

    fun setGesturePinch(action: String) {
        _uiState.value = _uiState.value.copy(gesturePinch = action); saveAll()
    }

    fun setIconSwipeUp(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(iconSwipeUpEnabled = enabled); saveAll()
    }

    fun setNavBarGestures(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(navBarGestures = enabled); saveAll()
    }

    // ─── Apps ─────────────────────────────────────────────────────────────────
    fun setHideApps(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hideAppsEnabled = enabled); saveAll()
    }

    fun setRecentAppsLimit(limit: String) {
        _uiState.value = _uiState.value.copy(recentAppsLimit = limit); saveAll()
    }

    // ─── System (extended) ────────────────────────────────────────────────────
    fun setBatterySaver(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(batterySaver = enabled); saveAll()
    }

    fun setThermalProtection(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(thermalProtection = enabled); saveAll()
    }

    // ─── Sound (extended) ─────────────────────────────────────────────────────
    fun setAlarmVolume(value: Float, context: Context = getApplication()) {
        _uiState.value = _uiState.value.copy(alarmVolume = value.coerceIn(0f, 1f))
        try {
            val am     = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (value * maxVol).toInt(), 0)
        } catch (e: Exception) { e.printStackTrace() }
        saveAll()
    }

    fun setVolumeKeysMedia(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(volumeKeysMedia = enabled); saveAll()
    }

    // ─── Gaming (extended) ────────────────────────────────────────────────────
    fun setBlockGameScreenshots(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(blockGameScreenshots = enabled); saveAll()
    }

    // ─── Accessibility (extended) ─────────────────────────────────────────────
    fun setMagnification(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(magnificationEnabled = enabled); saveAll()
    }

    fun setCaptionsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(captionsEnabled = enabled); saveAll()
    }

    // ─── Backup & Restore ─────────────────────────────────────────────────────
    fun setAutoBackup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoBackup = enabled); saveAll()
    }

    fun exportSettings(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state    = _uiState.value
                val snapshot = mapOf(
                    "dark_theme"          to state.isDarkTheme,
                    "accent_color"        to state.accentColor,
                    "app_theme"           to state.appTheme.ordinal,
                    "text_scale"          to state.textScale,
                    "icon_size"           to state.iconSize,
                    "taskbar_position"    to state.taskbarPosition,
                    "launcher_font"       to state.launcherFont,
                    "grid_size"           to state.gridSize,
                    "show_app_labels"     to state.showAppLabels,
                    "corner_radius"       to state.cornerRadius,
                    "transparency_effects" to state.transparencyEffects,
                    "animation_speed"     to state.animationSpeed
                )
                val json      = gson.toJson(snapshot)
                val dest      = File(context.filesDir, "bluebird_settings_export.json")
                dest.writeText(json)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun importSettings(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val src = File(context.filesDir, "bluebird_settings_export.json")
                if (!src.exists()) return@launch
                val type: java.lang.reflect.Type = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any> = gson.fromJson(src.readText(), type)
                withContext(Dispatchers.Main) {
                    val st = _uiState.value
                    _uiState.value = st.copy(
                        isDarkTheme         = (map["dark_theme"] as? Boolean) ?: st.isDarkTheme,
                        textScale           = (map["text_scale"] as? Double)?.toFloat() ?: st.textScale,
                        iconSize            = (map["icon_size"] as? String) ?: st.iconSize,
                        taskbarPosition     = (map["taskbar_position"] as? String) ?: st.taskbarPosition,
                        launcherFont        = (map["launcher_font"] as? String) ?: st.launcherFont,
                        gridSize            = (map["grid_size"] as? String) ?: st.gridSize,
                        showAppLabels       = (map["show_app_labels"] as? Boolean) ?: st.showAppLabels,
                        cornerRadius        = (map["corner_radius"] as? Double)?.toFloat() ?: st.cornerRadius,
                        transparencyEffects = (map["transparency_effects"] as? Boolean) ?: st.transparencyEffects,
                        animationSpeed      = (map["animation_speed"] as? Double)?.toFloat() ?: st.animationSpeed
                    )
                    saveAll()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun resetAllSettings() {
        _uiState.value = _uiState.value.copy(
            appTheme              = AppTheme.SYSTEM,
            isDarkTheme           = true,
            accentColor           = 0xFF0078D4,
            textScale             = 1f,
            iconSize              = "Medium",
            taskbarPosition       = "Bottom",
            startMenuLayout       = "Balanced",
            transparencyEffects   = true,
            animationSpeed        = 1f,
            launcherFont          = "Default (Roboto)",
            gridSize              = "4 × 5",
            showAppLabels         = true,
            cornerRadius          = 0.5f,
            statusBarClockPosition= "Right",
            darkModeSchedule      = "Manual",
            wallpaperSlideshow    = false,
            wallpaperSlideshowInterval = "30 minutes",
            gestureSwipeUp        = "App drawer",
            gestureSwipeDown      = "Notification shade",
            gestureDoubleTap      = "Lock screen",
            gesturePinch          = "Overview",
            iconSwipeUpEnabled    = false,
            navBarGestures        = true,
            hideAppsEnabled       = false,
            recentAppsLimit       = "10",
            batterySaver          = false,
            thermalProtection     = true,
            alarmVolume           = 0.7f,
            volumeKeysMedia       = true,
            blockGameScreenshots  = false,
            magnificationEnabled  = false,
            captionsEnabled       = false,
            autoBackup            = false,
            showSearchBar         = true,
            searchEngine          = "Google",
            searchIncludeApps     = true,
            searchIncludeContacts = true,
            searchIncludeSettings = true,
            searchWebSuggestions  = true,
            dndEnabled            = false,
            focusAssist           = false,
            screenTimeoutMinutes  = 2,
            notificationBanners   = true,
            showNotificationBadges= true,
            hapticFeedback        = true,
            systemSounds          = true
        )
        saveAll()
    }

    // ─── Profile picture picker ───────────────────────────────────────────────
    /** Call this from the Activity/Composable to trigger the image picker.
     *  This emits a one-shot event that the UI layer should listen to. */
    private val _openProfilePicturePicker = MutableStateFlow(false)
    val openProfilePickerEvent: StateFlow<Boolean> = _openProfilePicturePicker.asStateFlow()

    fun openProfilePicturePicker() {
        _openProfilePicturePicker.value = true
    }

    fun consumeProfilePickerEvent() {
        _openProfilePicturePicker.value = false
    }

    // ─── Search (extended) ────────────────────────────────────────────────────
    fun setShowSearchBar(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSearchBar = show); saveAll()
    }

    fun setSearchEngine(engine: String) {
        _uiState.value = _uiState.value.copy(searchEngine = engine); saveAll()
    }

    fun setSearchIncludeApps(include: Boolean) {
        _uiState.value = _uiState.value.copy(searchIncludeApps = include); saveAll()
    }

    fun setSearchIncludeContacts(include: Boolean) {
        _uiState.value = _uiState.value.copy(searchIncludeContacts = include); saveAll()
    }

    fun setSearchIncludeSettings(include: Boolean) {
        _uiState.value = _uiState.value.copy(searchIncludeSettings = include); saveAll()
    }

    fun setSearchWebSuggestions(enable: Boolean) {
        _uiState.value = _uiState.value.copy(searchWebSuggestions = enable); saveAll()
    }

    // ─── Storage helper (exposed for SettingsScreen) ──────────────────────────
    fun getStorageTotalBytes(context: Context): Long {
        return try {
            context.filesDir.totalSpace
        } catch (e: Exception) { 0L }
    }

    // ─── Window geometry persistence ─────────────────────────────────────────
    // Stores each window's last position + size so reopening restores it.
    private val _windowGeometries = mutableMapOf<String, io.github.norbertweb.bluebird.ui.components.WindowGeometry>()

    fun getWindowGeometry(id: String): io.github.norbertweb.bluebird.ui.components.WindowGeometry? =
        _windowGeometries[id]

    fun saveWindowGeometry(id: String, geometry: io.github.norbertweb.bluebird.ui.components.WindowGeometry) {
        _windowGeometries[id] = geometry
    }

}
