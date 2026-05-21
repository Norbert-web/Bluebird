package com.win11launcher

import android.app.Application
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.win11launcher.ui.components.DesktopFileInfo
import com.win11launcher.ui.components.DesktopItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
    PHONE, MESSAGES, RECYCLE_BIN, TextEditorScreen
}

// Added: iconKey so the taskbar/title-bar can show the right icon without
// importing Compose material icons into the ViewModel layer.
data class WindowState(
    val screen: LauncherScreen,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val title: String = "",
    val id: String = UUID.randomUUID().toString(),
    val extras: Map<String, String> = emptyMap(),
    val iconKey: String = ""
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
    const val PHONE         = "phone"
    const val MESSAGES      = "chat"
    const val RECYCLE_BIN   = "delete"
    const val TEXTEDITORSCREEN = ""
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
    val wallpaper: WallpaperState = WallpaperState(),
    val installedApps: List<AppInfo> = emptyList(),
    val pinnedTaskbarApps: List<AppInfo> = emptyList(),
    val desktopFiles: List<DesktopFileInfo> = emptyList(),
    val systemDesktopItems: List<DesktopFileInfo> = emptyList(),
    val openWindows: List<WindowState> = emptyList(),
    val activeWindowId: String? = null,
    val notifications: List<RealNotification> = emptyList(),
    val isNotificationListenerEnabled: Boolean = false,
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
    val updateChannel: String = "Stable"
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
        loadPersistedData()
        loadInstalledApps()
        startClock()
        startBatteryMonitor()
        initSystemDesktopItems()
        refreshDesktopFiles()
        checkNotificationListenerPermission()
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
            updateChannel         = updateChannel
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
            apply()
        }
    }

    // ─── OOBE ────────────────────────────────────────────────────────────────
    fun advanceSetupStep() {
        val current = _uiState.value.setupStep
        if (current >= 4) completeSetup()
        else _uiState.value = _uiState.value.copy(setupStep = current + 1)
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
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(installedApps = apps)

                val pinnedPackagesJson = prefs.getString("pinned_taskbar_apps", "[]") ?: "[]"
                val savedPackages      = try {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(pinnedPackagesJson, type)
                } catch (e: Exception) { emptyList() }

                val pinned = if (savedPackages.isEmpty()) {
                    val defaults = listOf("com.android.chrome", "com.google.android.gm",
                        "com.android.calculator2", "com.google.android.youtube")
                    apps.filter { it.packageName in defaults }.take(5)
                } else {
                    savedPackages.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                }
                _uiState.value = _uiState.value.copy(pinnedTaskbarApps = pinned)

                val recentAppsJson = prefs.getString("recent_apps", "[]") ?: "[]"
                val recentAppsSaved: List<AppInfoSaved> = try {
                    val type = object : TypeToken<List<AppInfoSaved>>() {}.type
                    gson.fromJson(recentAppsJson, type)
                } catch (e: Exception) { emptyList() }
                val recent = recentAppsSaved.mapNotNull { saved -> apps.find { it.packageName == saved.packageName } }
                _uiState.value = _uiState.value.copy(recentApps = recent)
            }
        }
    }

    fun addDesktopShortcutFromFile(filePath: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val source = File(filePath)
            if (!source.exists()) return@launch
            var dest  = File(desktopDir, fileName)
            var count = 1
            while (dest.exists()) {
                val name = fileName.substringBeforeLast(".")
                val ext  = fileName.substringAfterLast(".", "")
                dest = File(desktopDir, "$name ($count).$ext")
                count++
            }
            source.copyTo(dest, overwrite = false)
            withContext(Dispatchers.Main) { refreshDesktopFiles() }
        }
    }

    fun pinAppToTaskbar(app: AppInfo) {
        val current = _uiState.value.pinnedTaskbarApps
        if (current.any { it.packageName == app.packageName }) return
        _uiState.value = _uiState.value.copy(pinnedTaskbarApps = current + app)
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

    fun refreshDesktopFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files  = desktopDir.listFiles()?.toList() ?: emptyList()
            val loaded = files.mapNotNull { file ->
                try {
                    when {
                        file.isDirectory -> DesktopFileInfo(
                            id = file.absolutePath, file = file,
                            name = file.name, type = DesktopItemType.FOLDER, iconBitmap = null
                        )
                        file.name.endsWith(".desktop") -> {
                            val lines = file.readLines()
                            val pkg   = lines.find { it.startsWith("package=") }?.removePrefix("package=") ?: ""
                            val label = lines.find { it.startsWith("label=") }?.removePrefix("label=")
                                ?: file.nameWithoutExtension
                            val iconBitmap = try {
                                val pm = getApplication<Application>().packageManager
                                (pm.getApplicationIcon(pkg) as? BitmapDrawable)?.bitmap
                            } catch (e: Exception) { null }
                            DesktopFileInfo(
                                id = file.absolutePath, file = file, name = label,
                                type = DesktopItemType.APP_SHORTCUT, packageName = pkg, iconBitmap = iconBitmap
                            )
                        }
                        else -> DesktopFileInfo(
                            id = file.absolutePath, file = file,
                            name = file.name, type = DesktopItemType.OTHER_FILE, iconBitmap = null
                        )
                    }
                } catch (e: Exception) { null }
            }

            val positionsJson = prefs.getString("desktop_positions", "{}") ?: "{}"
            val customPositions: Map<String, Pair<Float, Float>> = try {
                val type = object : TypeToken<Map<String, Pair<Float, Float>>>() {}.type
                gson.fromJson(positionsJson, type)
            } catch (e: Exception) { emptyMap() }

            val positioned = loaded.map { item ->
                val pos = customPositions[item.file.absolutePath]
                if (pos != null) item.copy(position = Offset(pos.first, pos.second)) else item
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(desktopFiles = positioned)
            }
        }
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
            val file      = File(desktopDir, "$safeLabel.desktop")
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
    fun addNotification(notification: RealNotification) {
        val updated = listOf(notification) + _uiState.value.notifications.take(49)
        _uiState.value = _uiState.value.copy(notifications = updated)
    }

    fun dismissNotification(id: String) {
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.filter { it.id != id }
        )
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
    fun openWindow(screen: LauncherScreen, extras: Map<String, String> = emptyMap()) {
        dismissAllOverlays()
        // If a window for this screen already exists, restore + focus it (handles minimized case)
        val existing = _uiState.value.openWindows.find { it.screen == screen && it.extras == extras }
        if (existing != null) {
            restoreWindow(existing.id)
            return
        }

        val title = when (screen) {
            LauncherScreen.TextEditorScreen      -> "Text Editor"
            LauncherScreen.SETTINGS      -> "Settings"
            LauncherScreen.FILE_EXPLORER -> "File Explorer"
            LauncherScreen.BROWSER       -> "Bluebird Surfer Browser"
            LauncherScreen.TASK_MANAGER  -> "Task Manager"
            LauncherScreen.CALCULATOR    -> "Calculator"
            LauncherScreen.CALENDAR      -> "Calendar"
            LauncherScreen.PHOTOS        -> "Photos"
            LauncherScreen.MEDIA_PLAYER  -> "Media Player"
            LauncherScreen.IMAGE_VIEWER  -> extras["fileName"] ?: "Image Viewer"
            LauncherScreen.PHONE         -> "Phone"
            LauncherScreen.MESSAGES      -> "Messages"
            LauncherScreen.RECYCLE_BIN   -> "Recycle Bin"
            else                         -> "Window"
        }

        val iconKey = when (screen) {
            LauncherScreen.TextEditorScreen   -> WindowIconKey.TEXTEDITORSCREEN
            LauncherScreen.SETTINGS      -> WindowIconKey.SETTINGS
            LauncherScreen.FILE_EXPLORER -> WindowIconKey.FILE_EXPLORER
            LauncherScreen.BROWSER       -> WindowIconKey.BROWSER
            LauncherScreen.CALCULATOR    -> WindowIconKey.CALCULATOR
            LauncherScreen.CALENDAR      -> WindowIconKey.CALENDAR
            LauncherScreen.PHOTOS        -> WindowIconKey.PHOTOS
            LauncherScreen.TASK_MANAGER  -> WindowIconKey.TASK_MANAGER
            LauncherScreen.MEDIA_PLAYER  -> WindowIconKey.MEDIA_PLAYER
            LauncherScreen.IMAGE_VIEWER  -> WindowIconKey.IMAGE_VIEWER
            LauncherScreen.PHONE         -> WindowIconKey.PHONE
            LauncherScreen.MESSAGES      -> WindowIconKey.MESSAGES
            LauncherScreen.RECYCLE_BIN   -> WindowIconKey.RECYCLE_BIN


            else                         -> ""
        }

        val window  = WindowState(screen = screen, title = title, extras = extras, iconKey = iconKey)
        val current = _uiState.value.openWindows.toMutableList()
        current.add(window)
        _uiState.value = _uiState.value.copy(openWindows = current, activeWindowId = window.id)
    }

    fun closeWindow(windowId: String) {
        val current = _uiState.value.openWindows.filter { it.id != windowId }
        _uiState.value = _uiState.value.copy(
            openWindows    = current,
            activeWindowId = current.lastOrNull { !it.isMinimized }?.id
        )
    }

    fun setActiveWindow(windowId: String) {
        val current = _uiState.value.openWindows.toMutableList()
        val idx     = current.indexOfFirst { it.id == windowId }
        if (idx >= 0) {
            val w = current.removeAt(idx)
            current.add(w)
        }
        _uiState.value = _uiState.value.copy(openWindows = current, activeWindowId = windowId)
    }

    /** Always hides the window. Sets focus to the next visible window. */
    fun minimizeWindow(windowId: String) {
        val updated    = _uiState.value.openWindows.map {
            if (it.id == windowId) it.copy(isMinimized = true) else it
        }
        val nextActive = updated.lastOrNull { !it.isMinimized && it.id != windowId }?.id
        _uiState.value = _uiState.value.copy(
            openWindows    = updated,
            activeWindowId = if (_uiState.value.activeWindowId == windowId) nextActive
            else _uiState.value.activeWindowId
        )
    }

    /** Always un-hides the window and brings it to front. Safe on already-visible windows. */
    fun restoreWindow(windowId: String) {
        val current = _uiState.value.openWindows.toMutableList()
        val idx     = current.indexOfFirst { it.id == windowId }
        if (idx < 0) return
        val w = current.removeAt(idx)
        current.add(w.copy(isMinimized = false))
        _uiState.value = _uiState.value.copy(openWindows = current, activeWindowId = windowId)
    }

    fun maximizeWindow(windowId: String) {
        val updated = _uiState.value.openWindows.map {
            if (it.id == windowId) it.copy(isMaximized = !it.isMaximized) else it
        }
        _uiState.value = _uiState.value.copy(openWindows = updated)
    }

    // ─── App Launching & File Opening ─────────────────────────────────────────
    fun openApp(context: Context, appInfo: AppInfo) {
        dismissAllOverlays()
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

    private fun getMimeType(filePath: String): String = when (filePath.substringAfterLast(".").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
        "mp4", "mkv", "avi", "mov", "webm"         -> "video/*"
        "mp3", "wav", "ogg", "flac", "aac"         -> "audio/*"
        "pdf"                                        -> "application/pdf"
        "txt"                                        -> "text/plain"
        "html", "htm"                                -> "text/html"
        "apk"                                        -> "application/vnd.android.package-archive"
        else                                         -> "*/*"
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

    private fun startBatteryMonitor() {
        viewModelScope.launch {
            while (true) {
                try {
                    val bm       = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val level    = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val charging = bm.isCharging
                    if (level in 1..100)
                        _uiState.value = _uiState.value.copy(batteryLevel = level, isCharging = charging)
                } catch (e: Exception) { /* ignore */ }
                delay(60_000)
            }
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
}
