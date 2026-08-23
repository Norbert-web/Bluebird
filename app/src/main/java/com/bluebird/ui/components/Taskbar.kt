package com.bluebird.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bluebird.AppInfo
import com.bluebird.LauncherUiState
import com.bluebird.LauncherViewModel
import com.bluebird.WindowIconKey
import com.bluebird.WindowState
import com.bluebird.ui.theme.Win11Colors

// ─────────────────────────────────────────────────────────────────────────────
// Taskbar Settings State
// ─────────────────────────────────────────────────────────────────────────────
data class TaskbarSettings(
    val showSearchPill: Boolean = true,
    val showTaskView: Boolean = true,
    val showWidgets: Boolean = true,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showVolume: Boolean = true,
    val showNetwork: Boolean = true,
    val centerIcons: Boolean = true,
    val autoHide: Boolean = false,
    val showLabels: Boolean = false,
    val roundedTaskbar: Boolean = false,
    val separatedParts: Boolean = false,
    val taskbarOpacity: Float = 1.0f,
    val taskbarHeight: TaskbarHeight = TaskbarHeight.NORMAL,
    val iconOverflowMode: IconOverflowMode = IconOverflowMode.SCROLL,
    val maxVisibleIcons: Int = 10
)

enum class TaskbarHeight(val dp: Dp, val label: String) {
    COMPACT(32.dp, "Compact"),
    NORMAL(40.dp, "Normal"),
    LARGE(52.dp, "Large")
}

enum class IconOverflowMode(val label: String) {
    SCROLL("Scroll"),
    OVERFLOW_MENU("Menu"),
    GROUPED("Grouped"),
}

// ─────────────────────────────────────────────────────────────────────────────
// Persistence — SharedPreferences  (no extra dependencies required)
// Every field in TaskbarSettings has a matching key constant.
// load() is called once on first composition; save() is called via
// LaunchedEffect whenever settings change.
// ─────────────────────────────────────────────────────────────────────────────
private object TaskbarPrefs {
    private const val FILE = "taskbar_settings"

    // key constants
    private const val K_SEARCH_PILL      = "showSearchPill"
    private const val K_TASK_VIEW        = "showTaskView"
    private const val K_WIDGETS          = "showWidgets"
    private const val K_CLOCK            = "showClock"
    private const val K_BATTERY          = "showBattery"
    private const val K_VOLUME           = "showVolume"
    private const val K_NETWORK          = "showNetwork"
    private const val K_CENTER_ICONS     = "centerIcons"
    private const val K_AUTO_HIDE        = "autoHide"
    private const val K_SHOW_LABELS      = "showLabels"
    private const val K_ROUNDED          = "roundedTaskbar"
    private const val K_SEPARATED        = "separatedParts"
    private const val K_OPACITY          = "taskbarOpacity"
    private const val K_HEIGHT           = "taskbarHeight"
    private const val K_OVERFLOW_MODE    = "iconOverflowMode"
    private const val K_MAX_ICONS        = "maxVisibleIcons"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context): TaskbarSettings {
        val p = prefs(context)
        return TaskbarSettings(
            showSearchPill   = p.getBoolean(K_SEARCH_PILL,   true),
            showTaskView     = p.getBoolean(K_TASK_VIEW,     true),
            showWidgets      = p.getBoolean(K_WIDGETS,       true),
            showClock        = p.getBoolean(K_CLOCK,         true),
            showBattery      = p.getBoolean(K_BATTERY,       true),
            showVolume       = p.getBoolean(K_VOLUME,        true),
            showNetwork      = p.getBoolean(K_NETWORK,       true),
            centerIcons      = p.getBoolean(K_CENTER_ICONS,  true),
            autoHide         = p.getBoolean(K_AUTO_HIDE,     false),
            showLabels       = p.getBoolean(K_SHOW_LABELS,   false),
            roundedTaskbar   = p.getBoolean(K_ROUNDED,       false),
            separatedParts   = p.getBoolean(K_SEPARATED,     false),
            taskbarOpacity   = p.getFloat(K_OPACITY,         1.0f),
            taskbarHeight    = TaskbarHeight.valueOf(
                p.getString(K_HEIGHT, TaskbarHeight.NORMAL.name)
                    ?: TaskbarHeight.NORMAL.name),
            iconOverflowMode = IconOverflowMode.valueOf(
                p.getString(K_OVERFLOW_MODE, IconOverflowMode.SCROLL.name)
                    ?: IconOverflowMode.SCROLL.name),
            maxVisibleIcons  = p.getInt(K_MAX_ICONS, 10)
        )
    }

    fun save(context: Context, s: TaskbarSettings) {
        prefs(context).edit().apply {
            putBoolean(K_SEARCH_PILL,   s.showSearchPill)
            putBoolean(K_TASK_VIEW,     s.showTaskView)
            putBoolean(K_WIDGETS,       s.showWidgets)
            putBoolean(K_CLOCK,         s.showClock)
            putBoolean(K_BATTERY,       s.showBattery)
            putBoolean(K_VOLUME,        s.showVolume)
            putBoolean(K_NETWORK,       s.showNetwork)
            putBoolean(K_CENTER_ICONS,  s.centerIcons)
            putBoolean(K_AUTO_HIDE,     s.autoHide)
            putBoolean(K_SHOW_LABELS,   s.showLabels)
            putBoolean(K_ROUNDED,       s.roundedTaskbar)
            putBoolean(K_SEPARATED,     s.separatedParts)
            putFloat(K_OPACITY,         s.taskbarOpacity)
            putString(K_HEIGHT,         s.taskbarHeight.name)
            putString(K_OVERFLOW_MODE,  s.iconOverflowMode.name)
            putInt(K_MAX_ICONS,         s.maxVisibleIcons)
            apply()   // async — safe on main thread, no ANR risk
        }
    }
}


private fun iconForKey(key: String): ImageVector = when (key) {
    WindowIconKey.SETTINGS      -> Icons.Default.Settings
    WindowIconKey.FILE_EXPLORER -> Icons.Default.Folder
    WindowIconKey.BROWSER       -> Icons.Default.Public
    WindowIconKey.CALCULATOR    -> Icons.Default.Calculate
    WindowIconKey.CALENDAR      -> Icons.Default.CalendarToday
    WindowIconKey.PHOTOS        -> Icons.Default.PhotoLibrary
    WindowIconKey.TASK_MANAGER  -> Icons.Default.Monitor
    WindowIconKey.MEDIA_PLAYER  -> Icons.Default.MusicNote
    WindowIconKey.IMAGE_VIEWER  -> Icons.Default.Image
    WindowIconKey.WORD_IMPRESS         -> Icons.Default.Phone
    WindowIconKey.BLUEBIRD_STORE      -> Icons.Default.Chat
    WindowIconKey.RECYCLE_BIN   -> Icons.Default.Delete
    WindowIconKey.WEB_APP       -> Icons.Default.Public
    WindowIconKey.COPY_PROGRESS -> Icons.Default.ContentCopy
    else                        -> Icons.Default.Window
}

/**
 * Resolves a window's real bitmap icon (e.g. a fetched favicon) off the main thread.
 * Returns null while loading or on any failure/absence, so callers should keep the
 * Material-icon fallback visible until this resolves.
 */
@Composable
fun rememberWindowBitmapIcon(customIconPath: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, customIconPath) {
        value = if (!customIconPath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val f = java.io.File(context.filesDir, customIconPath)
                    if (f.exists()) android.graphics.BitmapFactory.decodeFile(f.absolutePath) else null
                } catch (_: Exception) { null }
            }
        } else null
    }
    return bitmap?.asImageBitmap()
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Taskbar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Win11Taskbar(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Load persisted settings once on first composition.
    // TaskbarPrefs.load() reads from SharedPreferences synchronously — it is
    // fast (< 1 ms) because the file is tiny, so running it on the main thread
    // inside remember {} is fine.
    var settings by remember { mutableStateOf(TaskbarPrefs.load(context)) }

    // Auto-save whenever settings change. apply() is async so it never blocks
    // the UI thread. The key is `settings` itself — effect re-runs on every change.
    LaunchedEffect(settings) {
        TaskbarPrefs.save(context, settings)
    }
    var hiddenTrayOpen by remember { mutableStateOf(false) }
    var settingsPanelOpen by remember { mutableStateOf(false) }
    var isTaskbarHidden by remember { mutableStateOf(false) }
    var overflowMenuOpen by remember { mutableStateOf(false) }

    var dragStartX by remember { mutableStateOf(0f) }
    var dragDeltaX by remember { mutableStateOf(0f) }
    val SLIDE_THRESHOLD_PX = 200f

    val taskbarHeight = settings.taskbarHeight.dp
    val iconSize: Dp = when (settings.taskbarHeight) {
        TaskbarHeight.COMPACT -> 13.dp
        TaskbarHeight.NORMAL  -> 15.dp
        TaskbarHeight.LARGE   -> 19.dp
    }

    // Popup offset: how many px above the taskbar the popup should appear
    // Popup(alignment=BottomEnd) anchors to the bottom of the *screen*, so
    // we shift up by taskbarHeight + gap.
    val popupOffsetY = with(density) { -(taskbarHeight + 6.dp).roundToPx() }
    val popupOffsetXEnd = with(density) { (-8).dp.roundToPx() }

    val taskbarShape = if (settings.roundedTaskbar) RoundedCornerShape(14.dp) else RectangleShape
    val taskbarBg = Win11Colors.TaskbarBg.copy(alpha = settings.taskbarOpacity)

    // Outer box is ONLY the taskbar strip itself — popups escape via Popup()
    Box(modifier = modifier.fillMaxWidth()) {

        // ── Peek strip ───────────────────────────────────────────────────────
        if (isTaskbarHidden) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    //  .background(Win11Colors.AccentBlue.copy(alpha = 0.6f))
                    // I  think invisible peek strip will be much better LAMN-NOBERT
                    .pointerInput(Unit) { detectTapGestures(onTap = { isTaskbarHidden = false }) }
            )
        }

        // ── Main Taskbar body ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !isTaskbarHidden,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val outerPadding = if (settings.roundedTaskbar)
                PaddingValues(horizontal = 8.dp, vertical = 4.dp) else PaddingValues(0.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(taskbarHeight + if (settings.roundedTaskbar) 8.dp else 0.dp)
                    .padding(outerPadding)
                    .shadow(if (settings.roundedTaskbar) 16.dp else 0.dp, taskbarShape)
                    .clip(taskbarShape)
                    .background(taskbarBg)
                    .border(0.5.dp, Color(0x18FFFFFF), taskbarShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragDeltaX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDeltaX += dragAmount.x
                                if (dragDeltaX > SLIDE_THRESHOLD_PX && dragStartX < 200f) {
                                    viewModel.toggleWidgets()
                                    dragDeltaX = -SLIDE_THRESHOLD_PX
                                }
                            },
                            onDragEnd = { dragDeltaX = 0f }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                isTaskbarHidden = true
                                hiddenTrayOpen = false
                                settingsPanelOpen = false
                            }
                        )
                    }
            ) {
                if (settings.separatedParts) {
                    SeparatedTaskbarLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        settings = settings,
                        iconSize = iconSize,
                        overflowMenuOpen = overflowMenuOpen,
                        onToggleOverflow = { overflowMenuOpen = !overflowMenuOpen },
                        onToggleHiddenTray = {
                            hiddenTrayOpen = !hiddenTrayOpen
                            settingsPanelOpen = false
                        },
                        hiddenTrayOpen = hiddenTrayOpen,
                        context = context
                    )
                } else {
                    ClassicTaskbarLayout(
                        uiState = uiState,
                        viewModel = viewModel,
                        settings = settings,
                        iconSize = iconSize,
                        overflowMenuOpen = overflowMenuOpen,
                        onToggleOverflow = { overflowMenuOpen = !overflowMenuOpen },
                        onToggleHiddenTray = {
                            hiddenTrayOpen = !hiddenTrayOpen
                            settingsPanelOpen = false
                        },
                        hiddenTrayOpen = hiddenTrayOpen,
                        context = context
                    )
                }
            }
        }

        // ── Hidden icons tray ─────────────────────────────────────────────────
        // ROOT FIX: Popup() renders into its own Android window layer — it is
        // never clipped by the taskbar Box's height constraints. This is why
        // AnimatedVisibility inside a fixed-height Box always showed nothing:
        // the composable measured to zero because its parent had no remaining
        // vertical space above it (taskbar is pinned to the bottom edge).
        if (hiddenTrayOpen) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset(x = popupOffsetXEnd, y = popupOffsetY),
                onDismissRequest = { hiddenTrayOpen = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() +
                            scaleIn(initialScale = 0.92f, transformOrigin = TransformOrigin(0.85f, 1f))
                ) {
                    HiddenIconsTray(
                        onOpenSettings = {
                            hiddenTrayOpen = false
                            settingsPanelOpen = true
                        }
                    )
                }
            }
        }

        // ── Settings panel ────────────────────────────────────────────────────
        if (settingsPanelOpen) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset(x = popupOffsetXEnd, y = popupOffsetY),
                onDismissRequest = { settingsPanelOpen = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() +
                            scaleIn(initialScale = 0.93f, transformOrigin = TransformOrigin(0.9f, 1f))
                ) {
                    TaskbarSettingsPanel(
                        settings = settings,
                        onSettingsChange = { settings = it },
                        onDismiss = { settingsPanelOpen = false },
                        onHideTaskbar = { isTaskbarHidden = true; settingsPanelOpen = false }
                    )
                }
            }
        }

        // ── Overflow window list ──────────────────────────────────────────────
        if (overflowMenuOpen && settings.iconOverflowMode == IconOverflowMode.OVERFLOW_MENU) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(x = 0, y = popupOffsetY),
                onDismissRequest = { overflowMenuOpen = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    )
                ) {
                    OverflowIconsPopup(
                        windows = uiState.openWindows.drop(settings.maxVisibleIcons),
                        activeWindowId = uiState.activeWindowId,
                        onWindowClick = { window ->
                            if (window.isMinimized) viewModel.restoreWindow(window.id)
                            else if (window.id == uiState.activeWindowId) viewModel.minimizeWindow(window.id)
                            else viewModel.setActiveWindow(window.id)
                            overflowMenuOpen = false
                        },
                        onDismiss = { overflowMenuOpen = false }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Classic single-bar layout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ClassicTaskbarLayout(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    settings: TaskbarSettings,
    iconSize: Dp,
    overflowMenuOpen: Boolean,
    onToggleOverflow: () -> Unit,
    onToggleHiddenTray: () -> Unit,
    hiddenTrayOpen: Boolean,
    context: android.content.Context
) {
    Box(Modifier.fillMaxSize()) {
        if (settings.showWidgets) {
            Row(Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) {
                TaskbarIconButton(
                    icon = Icons.Default.Dashboard,
                    contentDescription = "Widgets (or swipe right)",
                    isActive = uiState.isWidgetsOpen,
                    onClick = { viewModel.toggleWidgets() },
                    iconSize = iconSize
                )
            }
        }

        // Calculate how wide the right-side tray is so the center cluster never overlaps it.
        val trayEndPadding = 30.dp +  // Show Desktop button
                26.dp +                   // Hidden icons chevron
                (if (settings.showNetwork) 18.dp else 0.dp) +
                (if (settings.showVolume)  18.dp else 0.dp) +
                (if (settings.showBattery) 18.dp else 0.dp) +
                (if (settings.showClock)   54.dp else 0.dp) +
                12.dp                     // padding buffer

        TaskbarCenterCluster(
            modifier = if (settings.centerIcons)
                Modifier.align(Alignment.Center).padding(end = trayEndPadding)
            else
                Modifier.align(Alignment.CenterStart).padding(
                    start = if (settings.showWidgets) 42.dp else 6.dp,
                    end   = trayEndPadding
                ),
            uiState = uiState,
            viewModel = viewModel,
            settings = settings,
            iconSize = iconSize,
            overflowMenuOpen = overflowMenuOpen,
            onToggleOverflow = onToggleOverflow,
            context = context
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TaskbarIconButton(
                icon = Icons.Default.DesktopWindows,
                contentDescription = "Show Desktop",
                onClick = { viewModel.dismissAllOverlays() },
                iconSize = iconSize
            )
            HiddenIconsChevron(isOpen = hiddenTrayOpen, onClick = onToggleHiddenTray)
            SystemTray(
                uiState = uiState,
                settings = settings,
                onClickActionCenter = { viewModel.toggleActionCenter() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Separated pill layout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SeparatedTaskbarLayout(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    settings: TaskbarSettings,
    iconSize: Dp,
    overflowMenuOpen: Boolean,
    onToggleOverflow: () -> Unit,
    onToggleHiddenTray: () -> Unit,
    hiddenTrayOpen: Boolean,
    context: android.content.Context
) {
    val pillShape = RoundedCornerShape(10.dp)
    val pillBg = Win11Colors.TaskbarBg.copy(alpha = (settings.taskbarOpacity + 0.05f).coerceAtMost(1f))
    val pillBorder = BorderStroke(0.5.dp, Color(0x22FFFFFF))

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (settings.showWidgets) {
            Surface(shape = pillShape, color = pillBg, border = pillBorder,
                modifier = Modifier.shadow(8.dp, pillShape)) {
                TaskbarIconButton(
                    icon = Icons.Default.Dashboard,
                    contentDescription = "Widgets",
                    isActive = uiState.isWidgetsOpen,
                    onClick = { viewModel.toggleWidgets() },
                    iconSize = iconSize,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Surface(shape = pillShape, color = pillBg, border = pillBorder,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).shadow(8.dp, pillShape)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TaskbarCenterCluster(
                    modifier = Modifier.wrapContentWidth(),
                    uiState = uiState,
                    viewModel = viewModel,
                    settings = settings,
                    iconSize = iconSize,
                    overflowMenuOpen = overflowMenuOpen,
                    onToggleOverflow = onToggleOverflow,
                    context = context
                )
            }
        }

        Surface(shape = pillShape, color = pillBg, border = pillBorder,
            modifier = Modifier.shadow(8.dp, pillShape)) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TaskbarIconButton(
                    icon = Icons.Default.DesktopWindows,
                    contentDescription = "Show Desktop",
                    onClick = { viewModel.dismissAllOverlays() },
                    iconSize = iconSize
                )
                HiddenIconsChevron(isOpen = hiddenTrayOpen, onClick = onToggleHiddenTray)
                SystemTray(
                    uiState = uiState,
                    settings = settings,
                    onClickActionCenter = { viewModel.toggleActionCenter() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Center cluster
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskbarCenterCluster(
    modifier: Modifier,
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    settings: TaskbarSettings,
    iconSize: Dp,
    overflowMenuOpen: Boolean,
    onToggleOverflow: () -> Unit,
    context: android.content.Context
) {
    val groupedWindows: Map<String, List<WindowState>> = remember(uiState.openWindows) {
        uiState.openWindows.groupBy { it.iconKey.ifBlank { it.title } }
    }
    val pinnedApps = uiState.pinnedTaskbarApps
    val unpinnedWindows = uiState.openWindows.filter { w ->
        pinnedApps.none { it.packageName == w.id }
    }
    val (visibleUnpinned, overflowWindows) = when (settings.iconOverflowMode) {
        IconOverflowMode.OVERFLOW_MENU -> {
            unpinnedWindows.take(settings.maxVisibleIcons) to
                    unpinnedWindows.drop(settings.maxVisibleIcons)
        }
        else -> unpinnedWindows to emptyList()
    }
    val scrollState = rememberScrollState()

    val rowContent: @Composable RowScope.() -> Unit = {
        StartButton(isActive = uiState.isStartMenuOpen, onClick = { viewModel.toggleStartMenu() })
        Spacer(Modifier.width(4.dp))

        if (settings.showSearchPill) {
            SearchPill(onClick = { viewModel.toggleSearch() })
            Spacer(Modifier.width(4.dp))
        }

        if (settings.showTaskView) {
            TaskbarIconButton(
                icon = Icons.Default.ViewDay,
                contentDescription = "Task View",
                onClick = {},
                iconSize = iconSize
            )
        }

        if (pinnedApps.isNotEmpty() || uiState.openWindows.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.width(1.dp).height(18.dp).background(Color(0x22FFFFFF)))
            Spacer(Modifier.width(4.dp))
        }

        pinnedApps.forEach { app ->
            val runningWindow = uiState.openWindows.firstOrNull {
                it.title.contains(app.name, true) && !it.isMinimized
            }
            val isRunning = uiState.openWindows.any { it.title.contains(app.name, true) }
            val isActive  = runningWindow?.id == uiState.activeWindowId
            TaskbarAppIcon(
                appInfo = app,
                showLabel = settings.showLabels,
                isRunning = isRunning,
                isActive = isActive,
                onClick = {
                    val existing = uiState.openWindows.firstOrNull { it.title.contains(app.name, true) }
                    if (existing != null) {
                        if (existing.isMinimized) viewModel.restoreWindow(existing.id)
                        else if (existing.id == uiState.activeWindowId) viewModel.minimizeWindow(existing.id)
                        else viewModel.setActiveWindow(existing.id)
                    } else {
                        viewModel.openApp(context, app)
                    }
                },
                onLongClick = { viewModel.unpinAppFromTaskbar(app) }
            )
        }

        when (settings.iconOverflowMode) {
            IconOverflowMode.GROUPED -> {
                groupedWindows.forEach { (_, windows) ->
                    if (pinnedApps.none { it.packageName == windows.first().id }) {
                        val primary = windows.first()
                        TaskbarWindowIcon(
                            windowState = primary,
                            showLabel = settings.showLabels,
                            isActive = windows.any { it.id == uiState.activeWindowId },
                            windowCount = windows.size,
                            onClick = {
                                val w = if (windows.size == 1) windows[0] else primary
                                if (w.isMinimized) viewModel.restoreWindow(w.id)
                                else if (w.id == uiState.activeWindowId) viewModel.minimizeWindow(w.id)
                                else viewModel.setActiveWindow(w.id)
                            }
                        )
                    }
                }
            }
            IconOverflowMode.OVERFLOW_MENU -> {
                visibleUnpinned.forEach { window ->
                    TaskbarWindowIcon(
                        windowState = window,
                        showLabel = settings.showLabels,
                        isActive = window.id == uiState.activeWindowId,
                        onClick = {
                            if (window.isMinimized) viewModel.restoreWindow(window.id)
                            else if (window.id == uiState.activeWindowId) viewModel.minimizeWindow(window.id)
                            else viewModel.setActiveWindow(window.id)
                        }
                    )
                }
                if (overflowWindows.isNotEmpty()) {
                    OverflowBadgeButton(
                        count = overflowWindows.size,
                        isOpen = overflowMenuOpen,
                        onClick = onToggleOverflow
                    )
                }
            }
            IconOverflowMode.SCROLL -> {
                unpinnedWindows.forEach { window ->
                    TaskbarWindowIcon(
                        windowState = window,
                        showLabel = settings.showLabels,
                        isActive = window.id == uiState.activeWindowId,
                        onClick = {
                            if (window.isMinimized) viewModel.restoreWindow(window.id)
                            else if (window.id == uiState.activeWindowId) viewModel.minimizeWindow(window.id)
                            else viewModel.setActiveWindow(window.id)
                        }
                    )
                }
            }
        }
    }

    if (settings.iconOverflowMode == IconOverflowMode.SCROLL) {
        Row(
            modifier = modifier.horizontalScroll(scrollState).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            content = rowContent
        )
    } else {
        Row(
            modifier = modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            content = rowContent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overflow badge button (+N)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverflowBadgeButton(count: Int, isOpen: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isOpen) Win11Colors.AccentBlue.copy(0.25f) else Color(0x14FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("+$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overflow windows popup content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverflowIconsPopup(
    windows: List<WindowState>,
    activeWindowId: String?,
    onWindowClick: (WindowState) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 320.dp).shadow(20.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Win11Colors.TaskbarBg,
        border = BorderStroke(0.5.dp, Color(0x25FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text("More windows", color = Color.White.copy(0.6f), fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            windows.forEach { window ->
                val isActive = window.id == activeWindowId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) Win11Colors.AccentBlue.copy(0.18f) else Color.Transparent)
                        .clickable { onWindowClick(window) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = remember(window.iconKey) { iconForKey(window.iconKey) }
                    val bmpIcon = rememberWindowBitmapIcon(window.customIconPath)
                    if (bmpIcon != null) {
                        Image(bmpIcon, null, modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)))
                    } else {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(window.title, color = Color.White, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    if (window.isMinimized) {
                        Icon(Icons.Default.Minimize, null,
                            tint = Color.White.copy(0.4f), modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hidden Icons Chevron
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HiddenIconsChevron(isOpen: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = tween(200),
        label = "chevronRotation"
    )
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isOpen) Win11Colors.HoverBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.ExpandLess,
            contentDescription = "Show hidden icons",
            tint = Color.White,
            modifier = Modifier.size(13.dp).graphicsLayer { rotationZ = rotation }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hidden Icons Tray Panel
// Rendered via Popup() — never clipped by taskbar constraints.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HiddenIconsTray(onOpenSettings: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .wrapContentHeight()
            .shadow(20.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Win11Colors.TaskbarBg,
        border = BorderStroke(0.5.dp, Color(0x25FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notification area", color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(5.dp))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, "Taskbar Settings",
                        tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            val hiddenIcons = listOf(
                Icons.Default.Bluetooth     to "Bluetooth",
                Icons.Default.LocationOn    to "Location",
                Icons.Default.Mic           to "Microphone",
                Icons.Default.Notifications to "Notifications",
                Icons.Default.Security      to "Security",
                Icons.Default.SyncAlt       to "Sync",
                Icons.Default.CloudDone     to "Cloud Backup",
                Icons.Default.Print         to "Print Queue"
            )

            hiddenIcons.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { (icon, label) ->
                        var showTooltip by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (showTooltip) Win11Colors.HoverBg else Color.Transparent)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showTooltip = true },
                                        onTap = { showTooltip = false }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, label, tint = Color.White, modifier = Modifier.size(18.dp))
                            if (showTooltip) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-22).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF333333))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(label, color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.size(44.dp)) }
                }
                Spacer(Modifier.height(2.dp))
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Color(0x18FFFFFF))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, null, tint = Win11Colors.AccentBlue,
                    modifier = Modifier.size(14.dp))
                Text("Taskbar settings", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Taskbar Settings Panel
// Rendered via Popup() — never clipped by taskbar constraints.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskbarSettingsPanel(
    settings: TaskbarSettings,
    onSettingsChange: (TaskbarSettings) -> Unit,
    onDismiss: () -> Unit,
    onHideTaskbar: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight()
            .shadow(24.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E2E),
        border = BorderStroke(0.5.dp, Color(0x28FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Taskbar settings", color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(0.6f),
                        modifier = Modifier.size(13.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Items ────────────────────────────────────────────────────────
            SettingsSectionHeader("Items")
            SettingsToggle("Search pill",      settings.showSearchPill) { onSettingsChange(settings.copy(showSearchPill = it)) }
            SettingsToggle("Task View button", settings.showTaskView)   { onSettingsChange(settings.copy(showTaskView = it)) }
            SettingsToggle("Widgets button",   settings.showWidgets)    { onSettingsChange(settings.copy(showWidgets = it)) }
            SettingsToggle("Center icons",     settings.centerIcons)    { onSettingsChange(settings.copy(centerIcons = it)) }
            SettingsToggle("Show app labels",  settings.showLabels)     { onSettingsChange(settings.copy(showLabels = it)) }

            SettingsDivider()

            // ── Appearance ───────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")
            SettingsToggle("Rounded taskbar",  settings.roundedTaskbar) { onSettingsChange(settings.copy(roundedTaskbar = it)) }
            SettingsToggle("Separated parts",  settings.separatedParts) { onSettingsChange(settings.copy(separatedParts = it)) }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Opacity", color = Color.White.copy(0.85f), fontSize = 12.sp)
                Text("${(settings.taskbarOpacity * 100).toInt()}%",
                    color = Color.White.copy(0.5f), fontSize = 11.sp)
            }
            Slider(
                value = settings.taskbarOpacity,
                onValueChange = { onSettingsChange(settings.copy(taskbarOpacity = it)) },
                valueRange = 0.4f..1.0f,
                colors = SliderDefaults.colors(thumbColor = Win11Colors.AccentBlue,
                    activeTrackColor = Win11Colors.AccentBlue),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(4.dp))
            Text("Height", color = Color.White.copy(0.85f), fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskbarHeight.values().forEach { h ->
                    val sel = settings.taskbarHeight == h
                    Box(
                        modifier = Modifier
                            .weight(1f).height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) Win11Colors.AccentBlue else Color(0x18FFFFFF))
                            .clickable { onSettingsChange(settings.copy(taskbarHeight = h)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(h.label, color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            SettingsDivider()

            // ── Icon Overflow ────────────────────────────────────────────────
            SettingsSectionHeader("Icon Overflow")
            Text("When too many apps are open:", color = Color.White.copy(0.5f), fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(6.dp))

            IconOverflowMode.values().forEach { mode ->
                val sel = settings.iconOverflowMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) Win11Colors.AccentBlue.copy(0.15f) else Color.Transparent)
                        .clickable { onSettingsChange(settings.copy(iconOverflowMode = mode)) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = when (mode) {
                        IconOverflowMode.SCROLL        -> Icons.Default.SwipeRight
                        IconOverflowMode.OVERFLOW_MENU -> Icons.Default.MoreHoriz
                        IconOverflowMode.GROUPED       -> Icons.Default.LayersClear
                    }
                    val desc = when (mode) {
                        IconOverflowMode.SCROLL        -> "Scroll the icon row"
                        IconOverflowMode.OVERFLOW_MENU -> "Show +N overflow button"
                        IconOverflowMode.GROUPED       -> "Collapse same-app windows"
                    }
                    Icon(icon, null,
                        tint = if (sel) Win11Colors.AccentBlue else Color.White.copy(0.6f),
                        modifier = Modifier.size(15.dp))
                    Column {
                        Text(mode.label, color = Color.White, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal)
                        Text(desc, color = Color.White.copy(0.45f), fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (sel) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = Win11Colors.AccentBlue, modifier = Modifier.size(14.dp))
                    }
                }
            }

            AnimatedVisibility(settings.iconOverflowMode == IconOverflowMode.OVERFLOW_MENU) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max visible icons", color = Color.White.copy(0.85f), fontSize = 12.sp)
                        Text("${settings.maxVisibleIcons}", color = Win11Colors.AccentBlue,
                            fontSize = 12.sp)
                    }
                    Slider(
                        value = settings.maxVisibleIcons.toFloat(),
                        onValueChange = { onSettingsChange(settings.copy(maxVisibleIcons = it.toInt())) },
                        valueRange = 4f..20f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = Win11Colors.AccentBlue,
                            activeTrackColor = Win11Colors.AccentBlue),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }

            SettingsDivider()

            // ── System Tray ──────────────────────────────────────────────────
            SettingsSectionHeader("System Tray")
            SettingsToggle("Clock & date", settings.showClock)   { onSettingsChange(settings.copy(showClock = it)) }
            SettingsToggle("Battery",      settings.showBattery) { onSettingsChange(settings.copy(showBattery = it)) }
            SettingsToggle("Volume",       settings.showVolume)  { onSettingsChange(settings.copy(showVolume = it)) }
            SettingsToggle("Network",      settings.showNetwork) { onSettingsChange(settings.copy(showNetwork = it)) }

            SettingsDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x14FFFFFF))
                    .clickable(onClick = onHideTaskbar)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VisibilityOff, null, tint = Color(0xFFFF7070),
                    modifier = Modifier.size(14.dp))
                Text("Hide taskbar", color = Color(0xFFFF7070), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("2×tap", color = Color.White.copy(0.35f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, color = Win11Colors.AccentBlue.copy(alpha = 0.85f),
        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0x18FFFFFF))
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle(!value) }
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.85f), fontSize = 12.sp)
        val thumbOffset by animateDpAsState(
            targetValue = if (value) 16.dp else 2.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "toggleThumb"
        )
        Box(
            modifier = Modifier
                .width(34.dp).height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (value) Win11Colors.AccentBlue else Color(0xFF444444))
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset, y = 2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Start Button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StartButton(isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "startScale"
    )
    Box(
        modifier = Modifier
            .size(34.dp).scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Win11Colors.HoverBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        WindowsLogo(modifier = Modifier.size(15.dp), tint = Color.White)
    }
}

@Composable
private fun WindowsLogo(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w    = size.width
        val h    = size.height
        val gap  = w * 0.09f
        val half = (w - gap) / 2
        drawRect(tint, Offset(0f, 0f),                 Size(half, half))
        drawRect(tint, Offset(half + gap, 0f),         Size(half, half))
        drawRect(tint, Offset(0f, half + gap),         Size(half, half))
        drawRect(tint, Offset(half + gap, half + gap), Size(half, half))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search Pill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchPill(onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (hovered) Color(0xFF454545) else Color(0xFF3A3A3A),
        animationSpec = tween(150), label = "searchBg"
    )
    Row(
        modifier = Modifier
            .width(160.dp).height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Search, "Search", tint = Color(0xFFAAAAAA), modifier = Modifier.size(13.dp))
        Text("Search", color = Color(0xFF999999), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.AutoAwesome, null, tint = Win11Colors.AccentBlue.copy(alpha = 0.7f),
            modifier = Modifier.size(11.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic Taskbar Icon Button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun TaskbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    iconSize: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "iconScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(32.dp).scale(scale)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) Win11Colors.HoverBg else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap   = { onClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(iconSize))
        }
        if (isActive) {
            Box(Modifier.width(12.dp).height(2.dp)
                .clip(RoundedCornerShape(1.dp)).background(Win11Colors.AccentBlue))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pinned App Icon
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskbarAppIcon(
    appInfo: AppInfo,
    showLabel: Boolean,
    isRunning: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var pressed  by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "appIconScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .then(if (showLabel) Modifier.width(56.dp) else Modifier.size(32.dp))
                .height(32.dp).scale(scale)
                .clip(RoundedCornerShape(6.dp))
                .background(when {
                    isActive  -> Win11Colors.AccentBlue.copy(alpha = 0.18f)
                    isRunning -> Color(0x12FFFFFF)
                    else      -> Color.Transparent
                })
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap       = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (showLabel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    AppIconSmall(drawable = appInfo.icon, contentDescription = appInfo.name,
                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)))
                    Text(appInfo.name, color = Color.White, fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                AppIconSmall(drawable = appInfo.icon, contentDescription = appInfo.name,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Win11Colors.ContextMenuBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Open", color = Color.White, fontSize = 12.sp) },
                    onClick = { showMenu = false; onClick() },
                    leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = Color.White,
                        modifier = Modifier.size(13.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Unpin from taskbar", color = Color.White, fontSize = 12.sp) },
                    onClick = { showMenu = false; onLongClick() },
                    leadingIcon = { Icon(Icons.Default.PushPin, null, tint = Color.White,
                        modifier = Modifier.size(13.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Close window", color = Color(0xFFFF6B6B), fontSize = 12.sp) },
                    onClick = { showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(13.dp)) }
                )
            }
        }

        if (isRunning || isActive) {
            Box(
                modifier = Modifier
                    .width(if (isActive) 14.dp else 4.dp).height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) Win11Colors.AccentBlue else Color.White.copy(alpha = 0.45f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Running Window Icon (supports group badge)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskbarWindowIcon(
    windowState: WindowState,
    showLabel: Boolean,
    isActive: Boolean,
    windowCount: Int = 1,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Win11Colors.AccentBlue.copy(alpha = 0.22f)
        else Win11Colors.HoverBg.copy(alpha = 0.5f),
        animationSpec = tween(200), label = "windowBg"
    )
    val icon = remember(windowState.iconKey) { iconForKey(windowState.iconKey) }
    val bmpIcon = rememberWindowBitmapIcon(windowState.customIconPath)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .wrapContentWidth().height(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box {
                    if (bmpIcon != null) {
                        Image(bmpIcon, contentDescription = windowState.title,
                            modifier = Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)))
                    } else {
                        Icon(imageVector = icon, contentDescription = windowState.title,
                            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp))
                    }
                    if (windowCount > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Win11Colors.AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$windowCount", color = Color.White, fontSize = 7.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (showLabel) {
                    Text(text = windowState.title,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Box(
            modifier = Modifier
                .width(if (isActive) 16.dp else if (windowCount > 1) 10.dp else 6.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) Win11Colors.AccentBlue else Color.White.copy(alpha = 0.35f))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// System Tray
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SystemTray(
    uiState: LauncherUiState,
    settings: TaskbarSettings,
    onClickActionCenter: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClickActionCenter)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (settings.showNetwork) {
            Icon(
                imageVector = if (uiState.isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = "Wi-Fi",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
        if (settings.showVolume) {
            Icon(
                imageVector = when {
                    uiState.volume < 0.01f -> Icons.Default.VolumeOff
                    uiState.volume < 0.5f  -> Icons.Default.VolumeDown
                    else                   -> Icons.Default.VolumeUp
                },
                contentDescription = "Volume",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
        if (settings.showBattery) {
            Icon(Icons.Default.BatteryFull, "Battery", tint = Color.White,
                modifier = Modifier.size(13.dp))
        }
        if (settings.showClock) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(uiState.currentTime, color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.Medium, lineHeight = 12.sp)
                Text(uiState.currentDate, color = Color.White.copy(alpha = 0.55f),
                    fontSize = 8.5.sp, lineHeight = 10.sp)
            }
        }
    }
}
