package io.github.norbertweb.bluebird.ui.components

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons come from the shared FluentIcon object (FluentIcon.kt), which wraps
// the io.github.niyajali:fluentui-system-icons Compose Multiplatform library.
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.theme.LocalTextScale
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// ─── Persistence helpers ──────────────────────────────────────────────────────

private fun Context.widgetPrefs() = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

private fun saveTodos(context: Context, tasks: List<TodoTask>) {
    val json = org.json.JSONArray().apply {
        tasks.forEach { t ->
            put(JSONObject().apply {
                put("text", t.text)
                put("done", t.done)
            })
        }
    }
    context.widgetPrefs().edit().putString("todos", json.toString()).apply()
}

private fun loadTodos(context: Context): List<TodoTask> {
    val raw = context.widgetPrefs().getString("todos", null) ?: return listOf(
        TodoTask("Review pull request", true),
        TodoTask("Update documentation", false),
        TodoTask("Send weekly report", false),
    )
    return try {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TodoTask(o.getString("text"), o.getBoolean("done"))
        }
    } catch (e: Exception) { emptyList() }
}

private fun saveWidgetOrder(context: Context, order: List<String>) {
    context.widgetPrefs().edit().putString("widget_order", order.joinToString(",")).apply()
}

private fun loadWidgetOrder(context: Context): List<String> {
    val default = listOf("clock","weather","music","steps","stocks","news","calendar","photos","todo","alarm","network","screentime")
    val saved = context.widgetPrefs().getString("widget_order", null) ?: return default
    val savedList = saved.split(",").filter { it.isNotBlank() }
    // merge: keep saved order, append any new ones not yet in list (external
    // "ext:<id>" widget entries are appended to this same list, never to `default`)
    return (savedList + default.filter { it !in savedList })
}

// ─── External (third-party) app widgets ────────────────────────────────────
// Bound via AppWidgetHost. Represented in `widget_order` as "ext:<appWidgetId>"
// entries so they can be freely reordered alongside the built-in widgets.

private fun saveExternalWidgetIds(context: Context, ids: Set<Int>) {
    context.widgetPrefs().edit()
        .putStringSet("external_widget_ids", ids.map { it.toString() }.toSet())
        .apply()
}

private fun loadExternalWidgetIds(context: Context): Set<Int> =
    context.widgetPrefs().getStringSet("external_widget_ids", emptySet())
        ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

private fun saveHiddenWidgets(context: Context, hidden: Set<String>) {
    context.widgetPrefs().edit().putStringSet("hidden_widgets", hidden).apply()
}

private fun loadHiddenWidgets(context: Context): Set<String> =
    context.widgetPrefs().getStringSet("hidden_widgets", emptySet()) ?: emptySet()

// ─── Pinned widgets ─────────────────────────────────────────────────────────
// Pinned widgets always render first, above the rest of the (still freely
// reorderable) list — the same "pin to top" idea as a pinned Slack channel
// or a pinned home-screen app.

private fun savePinnedWidgets(context: Context, pinned: Set<String>) {
    context.widgetPrefs().edit().putStringSet("pinned_widgets", pinned).apply()
}

private fun loadPinnedWidgets(context: Context): Set<String> =
    context.widgetPrefs().getStringSet("pinned_widgets", emptySet()) ?: emptySet()

// ─── Compact density ────────────────────────────────────────────────────────

private fun saveCompactDensity(context: Context, compact: Boolean) {
    context.widgetPrefs().edit().putBoolean("compact_density", compact).apply()
}

private fun loadCompactDensity(context: Context): Boolean =
    context.widgetPrefs().getBoolean("compact_density", false)

// ─── Dynamic (wallpaper-matched) color ──────────────────────────────────────
// Only meaningful on Android 12+ (S), where the system exposes a
// wallpaper-derived Material You palette. Defaults to on when available.

private fun saveDynamicColorEnabled(context: Context, enabled: Boolean) {
    context.widgetPrefs().edit().putBoolean("dynamic_color_enabled", enabled).apply()
}

private fun loadDynamicColorEnabled(context: Context): Boolean =
    context.widgetPrefs().getBoolean("dynamic_color_enabled", true)

private fun saveWorldClocks(context: Context, clocks: List<WorldClock>) {
    val json = org.json.JSONArray().apply {
        clocks.forEach { c -> put(JSONObject().apply { put("label", c.label); put("tz", c.tz) }) }
    }
    context.widgetPrefs().edit().putString("world_clocks", json.toString()).apply()
}

private fun loadWorldClocks(context: Context): List<WorldClock> {
    val raw = context.widgetPrefs().getString("world_clocks", null) ?: return listOf(
        WorldClock("New York", "America/New_York"),
        WorldClock("London",   "Europe/London"),
        WorldClock("Tokyo",    "Asia/Tokyo"),
    )
    return try {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); WorldClock(o.getString("label"), o.getString("tz"))
        }
    } catch (e: Exception) { emptyList() }
}

// ─── Data classes ─────────────────────────────────────────────────────────────

private data class TodoTask(val text: String, val done: Boolean)
private data class WorldClock(val label: String, val tz: String)
private data class WeatherData(
    val city: String, val condition: String, val temp: Int,
    val high: Int, val low: Int, val forecast: List<DayForecast>
)
private data class DayForecast(val day: String, val icon: String, val high: Int, val low: Int)
private data class StockTicker(val symbol: String, val change: String, val price: String, val isUp: Boolean, val points: List<Float>)
private data class NewsArticle(val title: String, val source: String, val url: String)
private data class CalendarEvent(val title: String, val time: String, val color: Color)

// ─── Widget IDs ───────────────────────────────────────────────────────────────

// Built-in widget ids. Third-party app widgets bound via AppWidgetHost are
// represented separately as "ext:<appWidgetId>" entries inside widgetOrder —
// they don't need a fixed slot here since their count is dynamic.
private val ALL_WIDGET_IDS = listOf(
    "clock", "weather", "music", "steps", "stocks",
    "news", "calendar", "photos",
    "todo", "alarm", "network", "screentime"
)

private val WIDGET_LABELS = mapOf(
    "clock"      to "Clock & World Time",
    "weather"    to "Weather",
    "music"      to "Now Playing",
    "steps"      to "Steps",
    "stocks"     to "Markets",
    "news"       to "News",
    "calendar"   to "Calendar",
    "photos"     to "Photos",
    "todo"       to "To Do",
    "alarm"      to "Next Alarm",
    "network"    to "Network Speed",
    "screentime" to "Screen Time"
)

// ─────────────────────────────────────────────────────────
// Main Widget Panel
// ─────────────────────────────────────────────────────────
// A fixed host id for this panel's AppWidgetHost. Any small positive int is
// fine as long as it's unique within the app (only one widget surface hosts
// widgets here, so a constant is fine).
private const val BLUEBIRD_APPWIDGET_HOST_ID = 1042

@Composable
fun WidgetsPanel(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context    = LocalContext.current
    // Follows the system's light/dark setting directly instead of the app's
    // own theme toggle, so this panel always matches the rest of Android.
    val isDark     = isSystemInDarkTheme()
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val scope      = rememberCoroutineScope()
    val textScale  = LocalTextScale.current

    var isRefreshing  by remember { mutableStateOf(false) }
    var editMode      by remember { mutableStateOf(false) }
    var widgetOrder   by remember { mutableStateOf(loadWidgetOrder(context)) }
    var hiddenWidgets by remember { mutableStateOf(loadHiddenWidgets(context)) }
    var externalIds   by remember { mutableStateOf(loadExternalWidgetIds(context)) }
    var pinnedWidgets by remember { mutableStateOf(loadPinnedWidgets(context)) }
    var isCompact     by remember { mutableStateOf(loadCompactDensity(context)) }
    var dynamicColorEnabled by remember { mutableStateOf(loadDynamicColorEnabled(context)) }

    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // The accent every widget renders with. Falls back to the fixed brand
    // blue on API <31 or when the person has turned this off.
    val accentColor = remember(isDark, dynamicColorEnabled, dynamicColorSupported) {
        if (dynamicColorEnabled && dynamicColorSupported) {
            val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            scheme.primary
        } else {
            bluebirdColors.AccentBlue
        }
    }
    val density = if (isCompact) WidgetDensity.COMPACT else WidgetDensity.COMFORTABLE

    // ── AppWidgetHost: lets this panel embed widgets from other installed apps ──
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, BLUEBIRD_APPWIDGET_HOST_ID) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appWidgetHost.startListening()
                Lifecycle.Event.ON_STOP  -> appWidgetHost.stopListening()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        appWidgetHost.startListening()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appWidgetHost.stopListening()
        }
    }

    fun addExternalWidget(id: Int) {
        externalIds = externalIds + id
        saveExternalWidgetIds(context, externalIds)
        val newOrder = widgetOrder + "ext:$id"
        widgetOrder = newOrder
        saveWidgetOrder(context, newOrder)
    }

    fun removeExternalWidget(id: Int) {
        appWidgetHost.deleteAppWidgetId(id)
        externalIds = externalIds - id
        saveExternalWidgetIds(context, externalIds)
        val newOrder = widgetOrder.filter { it != "ext:$id" }
        widgetOrder = newOrder
        saveWidgetOrder(context, newOrder)
    }

    // Step 2 of the bind flow: system asked the user to approve binding —
    // if they said yes, the widget is now safe to host.
    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && id != -1) {
            addExternalWidget(id)
        } else if (id != -1) {
            appWidgetHost.deleteAppWidgetId(id)
        }
    }

    // Step 1: system's own "choose a widget" picker (shows every widget from
    // every installed app, exactly like adding one to the home screen).
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode != android.app.Activity.RESULT_OK || id == -1) {
            if (id != -1) appWidgetHost.deleteAppWidgetId(id)
            return@rememberLauncherForActivityResult
        }
        val info = appWidgetManager.getAppWidgetInfo(id)
        if (info != null) {
            addExternalWidget(id)
        } else {
            // Provider needs explicit bind approval — ask the user.
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
            )
        }
    }

    fun launchWidgetPicker() {
        val newId = appWidgetHost.allocateAppWidgetId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
        }
        pickLauncher.launch(intent)
    }

    // Refresh trigger — only widgets that support an explicit refresh observe it.
    var refreshTick by remember { mutableStateOf(0) }
    val visibleWidgetIds = remember(widgetOrder, hiddenWidgets, pinnedWidgets) {
        val visible = widgetOrder.filter { it !in hiddenWidgets }
        val pinned  = visible.filter { it in pinnedWidgets }
        val rest    = visible.filter { it !in pinnedWidgets }
        pinned + rest
    }
    val effectiveTextScale = textScale * density.textScaleMultiplier

    CompositionLocalProvider(
        LocalWidgetAccent provides accentColor,
        LocalWidgetDensity provides density
    ) {
    AcrylicSurface(
        modifier = modifier.width(380.dp).fillMaxHeight(),
        isDark = isDark, alpha = 0.96f, cornerRadius = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(density.itemSpacing)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Widgets",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Refresh — a single determinate spin, not a looping animation.
                    val rotation = remember { Animatable(0f) }
                    IconButton(onClick = {
                        if (!isRefreshing) {
                            isRefreshing = true
                            refreshTick++
                            scope.launch {
                                rotation.snapTo(0f)
                                rotation.animateTo(360f, tween(450, easing = FastOutSlowInEasing))
                                isRefreshing = false
                            }
                        }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = FluentIcon.ArrowSync, contentDescription = "Refresh widgets",
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp).rotate(rotation.value))
                    }
                    // Edit / Done
                    IconButton(onClick = { editMode = !editMode },
                        modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (editMode) FluentIcon.Checkmark else FluentIcon.Edit,
                            contentDescription = if (editMode) "Done editing" else "Edit widgets",
                            tint = if (editMode) LocalWidgetAccent.current else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Edit mode: visibility toggles + drag-to-reorder + add widget ──
            AnimatedVisibility(visible = editMode) {
                WidgetEditPanel(
                    order       = widgetOrder,
                    hidden      = hiddenWidgets,
                    pinned      = pinnedWidgets,
                    isDark      = isDark,
                    textColor   = textColor,
                    textScale   = textScale,
                    isCompact   = isCompact,
                    dynamicColorEnabled = dynamicColorEnabled,
                    dynamicColorSupported = dynamicColorSupported,
                    onToggle    = { id ->
                        hiddenWidgets = if (id in hiddenWidgets)
                            hiddenWidgets - id else hiddenWidgets + id
                        saveHiddenWidgets(context, hiddenWidgets)
                    },
                    onTogglePin = { id ->
                        pinnedWidgets = if (id in pinnedWidgets)
                            pinnedWidgets - id else pinnedWidgets + id
                        savePinnedWidgets(context, pinnedWidgets)
                    },
                    onReorder   = { newOrder ->
                        widgetOrder = newOrder
                        saveWidgetOrder(context, newOrder)
                    },
                    onRemoveExternal = { id -> removeExternalWidget(id) },
                    onAddWidget = { launchWidgetPicker() },
                    onToggleCompact = { isCompact = it; saveCompactDensity(context, it) },
                    onToggleDynamicColor = { dynamicColorEnabled = it; saveDynamicColorEnabled(context, it) }
                )
            }

            // ── Render widgets in saved order (pinned first) ──────────────────
            visibleWidgetIds.forEachIndexed { index, id ->
                key(id) {
                    if (id in pinnedWidgets && (index == 0)) {
                        Text(
                            "PINNED",
                            color = textColor.copy(alpha = 0.35f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (id !in pinnedWidgets && index > 0 && visibleWidgetIds[index - 1] in pinnedWidgets) {
                        Spacer(Modifier.height(2.dp))
                    }
                    when {
                        id == "clock"      -> ClockWidget(isDark, context, effectiveTextScale)
                        id == "weather"    -> WeatherWidget(isDark, context, effectiveTextScale, refreshTick)
                        id == "music"      -> NowPlayingWidget(isDark, context, effectiveTextScale)
                        id == "steps"      -> StepsWidget(isDark, context, effectiveTextScale)
                        id == "stocks"     -> StockWidget(isDark, effectiveTextScale, refreshTick)
                        id == "news"       -> NewsWidget(isDark, effectiveTextScale, refreshTick)
                        id == "calendar"   -> CalendarWidget(isDark, context, effectiveTextScale)
                        id == "photos"     -> PhotosWidget(isDark, context, effectiveTextScale)
                        id == "todo"       -> TodoWidget(isDark, context, effectiveTextScale)
                        id == "alarm"      -> AlarmWidget(isDark, context, effectiveTextScale)
                        id == "network"    -> NetworkSpeedWidget(isDark, effectiveTextScale)
                        id == "screentime" -> ScreenTimeWidget(isDark, context, effectiveTextScale)
                        id.startsWith("ext:") -> {
                            val widgetId = id.removePrefix("ext:").toIntOrNull()
                            if (widgetId != null) {
                                ExternalAppWidget(widgetId, appWidgetHost, appWidgetManager, isDark)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    }
}

// ─────────────────────────────────────────────────────────
// External app widget host — renders a widget picked from another app,
// wrapped in the same card language as the built-in widgets.
// ─────────────────────────────────────────────────────────
@Composable
private fun ExternalAppWidget(
    widgetId: Int,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    isDark: Boolean
) {
    val info = remember(widgetId) { manager.getAppWidgetInfo(widgetId) } ?: return
    val border = if (isDark) DS.borderDark else DS.borderLight
    val cardBg = if (isDark) bluebirdColors.WidgetBg else bluebirdColors.WidgetBgLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WIDGET_CORNER))
            .background(cardBg)
            .border(1.dp, border, RoundedCornerShape(WIDGET_CORNER))
            .padding(6.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = {
                host.createView(it, widgetId, info).apply {
                    setPadding(0, 0, 0, 0)
                }
            }
        )
    }
}

// ─── Edit Panel ───────────────────────────────────────────────────────────────

@Composable
private fun WidgetEditPanel(
    order: List<String>,
    hidden: Set<String>,
    pinned: Set<String>,
    isDark: Boolean,
    textColor: Color,
    textScale: Float,
    isCompact: Boolean,
    dynamicColorEnabled: Boolean,
    dynamicColorSupported: Boolean,
    onToggle: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRemoveExternal: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onToggleCompact: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    val bg     = if (isDark) bluebirdColors.WidgetBg else bluebirdColors.WidgetBgLight
    val border = if (isDark) DS.borderDark else DS.borderLight
    val haptic = LocalHapticFeedback.current

    // ── Manual drag-to-reorder state ──
    // Rows report their measured height; while dragging we track how far the
    // pointer has moved and swap the dragged row past a neighbor once it
    // crosses that neighbor's midpoint — the same interaction used by most
    // native "rearrange your home screen" pickers. A light haptic tick fires
    // on pickup and again on every swap, mirroring how the system home
    // screen's own rearrange mode feels.
    var localOrder by remember(order) { mutableStateOf(order) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = remember { mutableStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(WIDGET_CORNER), ambientColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.05f), spotColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.05f))
            .clip(RoundedCornerShape(WIDGET_CORNER))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(WIDGET_CORNER))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Edit widgets",
                color      = LocalWidgetAccent.current,
                fontSize   = (13 * textScale).sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Hold  ${'\u2261'}  to drag",
                color      = textColor.copy(alpha = 0.4f),
                fontSize   = (10 * textScale).sp,
                fontWeight = FontWeight.Medium
            )
        }

        localOrder.forEachIndexed { index, id ->
            val isExternal = id.startsWith("ext:")
            val label = if (isExternal) "App widget" else (WIDGET_LABELS[id] ?: id)
            val visible = id !in hidden
            val isPinned = id in pinned
            val isDragged = id == draggingId
            val rowBg by animateColorAsState(
                targetValue = if (isDragged) LocalWidgetAccent.current.copy(alpha = 0.10f)
                else Color.Transparent,
                label = "editRowBg"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { rowHeightPx.value = it.size.height.toFloat() }
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffset else 0f
                        scaleX = if (isDragged) 1.015f else 1f
                        scaleY = if (isDragged) 1.015f else 1f
                    }
                    .clip(RoundedCornerShape(11.dp))
                    .background(rowBg)
                    .then(
                        if (isDragged) Modifier.border(1.dp, LocalWidgetAccent.current.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                        else Modifier
                    )
                    .padding(vertical = 7.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = FluentIcon.ArrowSort,
                    contentDescription = "Drag to reorder",
                    tint = if (isDragged) LocalWidgetAccent.current else textColor.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(22.dp)
                        .pointerInput(localOrder) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = id; dragOffset = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = { draggingId = null; dragOffset = 0f; onReorder(localOrder) },
                                onDragCancel = { draggingId = null; dragOffset = 0f },
                                onDrag = { change, delta ->
                                    change.consume()
                                    dragOffset += delta.y
                                    val rowH = rowHeightPx.value.coerceAtLeast(1f)
                                    val currentIdx = localOrder.indexOf(id)
                                    val targetIdx = (currentIdx + (dragOffset / rowH).roundToInt())
                                        .coerceIn(0, localOrder.lastIndex)
                                    if (targetIdx != currentIdx) {
                                        localOrder = localOrder.toMutableList().apply {
                                            add(targetIdx, removeAt(currentIdx))
                                        }
                                        dragOffset -= (targetIdx - currentIdx) * rowH
                                        // A lighter tick than the pickup, one per swap — gives
                                        // continuous feedback without feeling like a buzzer.
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            )
                        }
                )
                Switch(
                    checked  = visible,
                    onCheckedChange = { onToggle(id) },
                    colors   = SwitchDefaults.colors(checkedThumbColor = LocalWidgetAccent.current),
                    modifier = Modifier.size(36.dp, 21.dp)
                )
                Text(
                    label, color = textColor,
                    fontSize = (13 * textScale).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onTogglePin(id) }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = FluentIcon.Pin,
                        contentDescription = if (isPinned) "Unpin widget" else "Pin widget to top",
                        tint = if (isPinned) LocalWidgetAccent.current else textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (isExternal) {
                    IconButton(
                        onClick = { id.removePrefix("ext:").toIntOrNull()?.let(onRemoveExternal) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(imageVector = FluentIcon.Delete, contentDescription = "Remove widget",
                            tint = bluebirdColors.Error.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = widgetDividerColor(textColor), thickness = 0.5.dp
        )

        // ── Add a widget from any installed app ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(LocalWidgetAccent.current.copy(alpha = 0.08f))
                .clickable { onAddWidget() }
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = FluentIcon.Add, contentDescription = null,
                tint = LocalWidgetAccent.current, modifier = Modifier.size(17.dp))
            Text(
                "Add widget from an app",
                color = LocalWidgetAccent.current,
                fontSize = (13 * textScale).sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = widgetDividerColor(textColor), thickness = 0.5.dp
        )

        // ── Panel-wide settings ──
        SettingsToggleRow(
            icon = FluentIcon.Resize, label = "Compact view", textColor = textColor, textScale = textScale,
            checked = isCompact, onCheckedChange = onToggleCompact
        )
        if (dynamicColorSupported) {
            SettingsToggleRow(
                icon = FluentIcon.Color, label = "Match wallpaper colors", textColor = textColor, textScale = textScale,
                checked = dynamicColorEnabled, onCheckedChange = onToggleDynamicColor
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color,
    textScale: Float,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(17.dp))
        Text(
            label, color = textColor,
            fontSize = (13 * textScale).sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors  = SwitchDefaults.colors(checkedThumbColor = LocalWidgetAccent.current),
            modifier = Modifier.size(36.dp, 21.dp)
        )
    }
}

// Shared corner radius for every widget-panel surface (cards, edit sheet,
// external widget frames) so the whole panel reads as one consistent shape
// language instead of the previous 12–14dp mismatch.
private val WIDGET_CORNER = 20.dp

// ── Dynamic (Material You) accent, threaded via CompositionLocal so every
// widget — most of which only take `isDark` — picks it up without a signature
// change. Falls back to the fixed brand blue when dynamic color is off or
// unavailable (pre-Android 12).
private val LocalWidgetAccent = staticCompositionLocalOf { bluebirdColors.AccentBlue }

// ── Compact/comfortable density, same CompositionLocal approach. Comfortable
// matches the original spacing; compact tightens card padding for people who
// want more widgets visible at once.
private enum class WidgetDensity { COMFORTABLE, COMPACT }
private val LocalWidgetDensity = staticCompositionLocalOf { WidgetDensity.COMFORTABLE }
private val WidgetDensity.cardPadding get() = if (this == WidgetDensity.COMPACT) 12.dp else 18.dp
private val WidgetDensity.itemSpacing get() = if (this == WidgetDensity.COMPACT) 10.dp else 14.dp

/** True density-scale multiplier applied on top of the user's accessibility text scale. */
private val WidgetDensity.textScaleMultiplier get() = if (this == WidgetDensity.COMPACT) 0.92f else 1f

// ─── Shared card wrapper ──────────────────────────────────────────────────────

@Composable
private fun WidgetCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val base   = if (isDark) bluebirdColors.WidgetBg else bluebirdColors.WidgetBgLight
    val border = if (isDark) DS.borderDark else DS.borderLight
    val shape  = RoundedCornerShape(WIDGET_CORNER)
    val density = LocalWidgetDensity.current
    // A very slight top-to-bottom tonal gradient instead of one flat fill —
    // this is what reads as "premium" rather than a plain filled rectangle,
    // without needing any actual elevation/shadow animation.
    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            base.copy(alpha = 1f).let { if (isDark) lighten(it, 0.035f) else lighten(it, 0.5f) },
            base
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.06f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.06f)
            )
            .clip(shape)
            .background(surfaceBrush)
            .border(1.dp, border, shape)
    ) {
        Column(modifier = Modifier.padding(density.cardPadding), content = content)
    }
}

/** Nudges a color slightly toward white — used for the card's subtle top gradient. */
private fun lighten(color: Color, amount: Float): Color = Color(
    red   = (color.red + (1f - color.red) * amount).coerceIn(0f, 1f),
    green = (color.green + (1f - color.green) * amount).coerceIn(0f, 1f),
    blue  = (color.blue + (1f - color.blue) * amount).coerceIn(0f, 1f),
    alpha = color.alpha
)

/** Shared, tone-balanced divider color for widget-internal separators. */
private fun widgetDividerColor(textColor: Color) = textColor.copy(alpha = 0.10f)

@Composable
private fun WidgetHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, textColor: Color, textScale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color      = textColor,
            fontSize   = (15 * textScale).sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(LocalWidgetAccent.current.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(17.dp))
        }
    }
}

/**
 * A small "Show more / Show less" affordance for widgets whose list is
 * capped by default (Markets, News, …). Keeps the panel scannable while
 * still letting the full data be reached without a separate screen.
 */
@Composable
private fun ExpandToggle(expanded: Boolean, textScale: Float, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (expanded) "Show less" else "Show more",
            color = LocalWidgetAccent.current,
            fontSize = (11 * textScale).sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = if (expanded) FluentIcon.ChevronUp else FluentIcon.ChevronDown,
            contentDescription = null,
            tint = LocalWidgetAccent.current,
            modifier = Modifier.size(14.dp).padding(start = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────
// 1. Clock & World Time Widget (NEW)
// ─────────────────────────────────────────────────────────
@Composable
private fun ClockWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val worldClocks by remember { mutableStateOf(loadWorldClocks(context)) }
    val windowRuntime = LocalWindowRuntime.current

    // A hidden widget does not need second-by-second Compose invalidations.
    // Keep the clock correct when restored, but sleep cheaply while minimized.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(windowRuntime.isMinimized) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(if (windowRuntime.isMinimized) 5000 else 1000)
        }
    }

    val localTime = remember(now) {
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(now))
    }
    val localDate = remember(now) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
    }

    WidgetCard(isDark) {
        // Large local clock
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                localTime,
                fontSize   = (38 * textScale).sp,
                fontWeight = FontWeight.Light,
                color      = textColor,
                letterSpacing = 2.sp
            )
            Text(
                localDate,
                fontSize = (12 * textScale).sp,
                color    = textColor.copy(alpha = 0.6f)
            )
        }

        if (worldClocks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = widgetDividerColor(textColor), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            // World clocks
            worldClocks.forEach { wc ->
                val tz   = TimeZone.getTimeZone(wc.tz)
                val sdf  = SimpleDateFormat("h:mm a", Locale.getDefault()).apply { timeZone = tz }
                val time = remember(now) { sdf.format(Date(now)) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = FluentIcon.Globe, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(14.dp))
                        Text(wc.label, color = textColor.copy(alpha = 0.7f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
                    }
                    Text(time, color = textColor, fontSize = (13 * textScale).sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 2. Weather Widget (real API via OpenWeatherMap free tier)
// ─────────────────────────────────────────────────────────
@Composable
private fun WeatherWidget(isDark: Boolean, context: Context, textScale: Float, refreshTick: Int) {
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var loading     by remember { mutableStateOf(true) }
    var error       by remember { mutableStateOf(false) }

    // ── 1. Track whether we have location permission ──────────────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // ── 2. Permission launcher ────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // ── 3. Ask for permission on first composition if not granted ─────────────
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── 4. Fetch weather once we have permission (or on refresh) ──────────────
    LaunchedEffect(refreshTick, hasLocationPermission) {
        if (!hasLocationPermission) { loading = false; return@LaunchedEffect }

        loading = true; error = false

        // Resolve current lat/lon on IO thread
        val location: Pair<Double, Double>? = withContext(Dispatchers.IO) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val provider = when {
                    lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)     -> android.location.LocationManager.GPS_PROVIDER
                    lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) -> android.location.LocationManager.NETWORK_PROVIDER
                    else -> null
                } ?: return@withContext null

                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(provider) ?: return@withContext null
                Pair(loc.latitude, loc.longitude)
            } catch (e: Exception) { null }
        }

        if (location == null) { error = true; loading = false; return@LaunchedEffect }

        val (lat, lon) = location

        withContext(Dispatchers.IO) {
            try {
                val apiKey = "42b87f7284bf8558f040e20a112874b9"   // ← your key
                val url  = "https://api.openweathermap.org/data/2.5/forecast" +
                        "?lat=$lat&lon=$lon&units=imperial&cnt=5&appid=$apiKey"
                val raw  = URL(url).readText()
                val json = JSONObject(raw)
                val city = json.getJSONObject("city").getString("name")
                val list = json.getJSONArray("list")
                val current = list.getJSONObject(0)
                val temp    = current.getJSONObject("main").getDouble("temp").toInt()
                val high    = current.getJSONObject("main").getDouble("temp_max").toInt()
                val low     = current.getJSONObject("main").getDouble("temp_min").toInt()
                val cond    = current.getJSONArray("weather").getJSONObject(0).getString("main")
                val forecast = (0 until minOf(5, list.length())).map { i ->
                    val item  = list.getJSONObject(i)
                    val dt    = item.getLong("dt") * 1000
                    val day   = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dt))
                    val ic    = item.getJSONArray("weather").getJSONObject(0).getString("main")
                    val hi    = item.getJSONObject("main").getDouble("temp_max").toInt()
                    val lo    = item.getJSONObject("main").getDouble("temp_min").toInt()
                    val emoji = when {
                        ic.contains("Clear")   -> "☀️"
                        ic.contains("Cloud")   -> "⛅"
                        ic.contains("Rain")    -> "🌧"
                        ic.contains("Snow")    -> "❄️"
                        ic.contains("Thunder") -> "⛈"
                        else                   -> "🌤"
                    }
                    DayForecast(day, emoji, hi, lo)
                }
                withContext(Dispatchers.Main) {
                    weatherData = WeatherData(city, cond, temp, high, low, forecast)
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = true; loading = false }
            }
        }
    }

    // ── 5. UI — identical to before ──────────────────────────────────────────
    val gradientColors = when {
        weatherData?.condition?.contains("Clear") == true ||
                weatherData?.condition?.contains("Sunny") == true ->
            listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
        weatherData?.condition?.contains("Cloud") == true ->
            listOf(Color(0xFF546E7A), Color(0xFF90A4AE))
        weatherData?.condition?.contains("Rain") == true ->
            listOf(Color(0xFF455A64), Color(0xFF607D8B))
        weatherData?.condition?.contains("Snow") == true ->
            listOf(Color(0xFF78909C), Color(0xFFB0BEC5))
        else -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            when {
                !hasLocationPermission -> Box(
                    Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = FluentIcon.LocationOff, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Location permission needed",
                            color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                    }
                }
                loading -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                error -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = FluentIcon.CloudOff, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Could not load weather", color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                    }
                }
                weatherData != null -> {
                    val data = weatherData!!
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text(data.city, color = Color.White, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
                                Text(data.condition, color = Color.White.copy(alpha = 0.8f), fontSize = (12 * textScale).sp)
                                Spacer(Modifier.height(8.dp))
                                Text("${data.temp}°", fontSize = (48 * textScale).sp, color = Color.White, fontWeight = FontWeight.Light)
                                Text("H:${data.high}°  L:${data.low}°", color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                            }
                            Icon(
                                when {
                                    data.condition.contains("Clear") || data.condition.contains("Sunny") -> FluentIcon.WeatherSunny
                                    data.condition.contains("Cloud") -> FluentIcon.WeatherCloudy
                                    data.condition.contains("Rain")  -> FluentIcon.WeatherRain
                                    else -> FluentIcon.WeatherSunny
                                },
                                null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            items(data.forecast) { day ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                                    Text(day.day, color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                                    Text(day.icon, fontSize = (16 * textScale).sp)
                                    Text("${day.high}°", color = Color.White, fontSize = (11 * textScale).sp)
                                    Text("${day.low}°", color = Color.White.copy(alpha = 0.5f), fontSize = (10 * textScale).sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// ─────────────────────────────────────────────────────────
// 3. Now Playing Widget (MediaSession) — NEW
// ─────────────────────────────────────────────────────────
@Composable
private fun NowPlayingWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    var title    by remember { mutableStateOf<String?>(null) }
    var artist   by remember { mutableStateOf<String?>(null) }
    var albumArt by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<android.media.session.MediaController?>(null) }

    // Check notification listener permission (needed for MediaSessionManager).
    // This widget observes the active-session list instead of taking a one-time snapshot.
    val hasPermission = remember {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        enabled.contains(context.packageName)
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            if (msm == null) {
                onDispose { }
            } else {
                val component = android.content.ComponentName(
                    context,
                    io.github.norbertweb.bluebird.data.NotificationListener::class.java
                )

                var registeredController: android.media.session.MediaController? = null

                val controllerCallback = object : android.media.session.MediaController.Callback() {
                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                        title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                        artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                        albumArt = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    }

                    override fun onPlaybackStateChanged(
                        state: android.media.session.PlaybackState?
                    ) {
                        isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
                    }
                }

                fun attach(active: android.media.session.MediaController?) {
                    if (registeredController === active) return
                    registeredController?.unregisterCallback(controllerCallback)
                    registeredController = active
                    controller = active
                    active?.registerCallback(controllerCallback)

                    val metadata = active?.metadata
                    title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                    artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                    albumArt = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    isPlaying = active?.playbackState?.state ==
                        android.media.session.PlaybackState.STATE_PLAYING
                }

                val listener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
                    attach(sessions?.firstOrNull())
                }

                try {
                    attach(msm.getActiveSessions(component).firstOrNull())
                    msm.addOnActiveSessionsChangedListener(listener, component)
                } catch (_: SecurityException) {
                    // Permission can disappear while the settings screen is open.
                } catch (_: IllegalArgumentException) {
                    // Listener registration can fail if the notification service is unavailable.
                }

                onDispose {
                    try { msm.removeOnActiveSessionsChangedListener(listener) } catch (_: Exception) {}
                    registeredController?.unregisterCallback(controllerCallback)
                    registeredController = null
                    controller = null
                    albumArt = null
                }
            }
        }
    }

    val albumImage = remember(albumArt) { albumArt?.asImageBitmap() }

    WidgetCard(isDark) {
        WidgetHeader("Now Playing", FluentIcon.MusicNote2, textColor, textScale)
        Spacer(Modifier.height(10.dp))

        if (!hasPermission) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Grant notification access", fontSize = (12 * textScale).sp) }
        } else if (title == null) {
            Text("Nothing playing", color = textColor.copy(alpha = 0.5f), fontSize = (13 * textScale).sp, fontWeight = FontWeight.Medium)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album art
                if (albumArt != null) {
                    Image(
                        bitmap = albumImage!!,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
                            .background(LocalWidgetAccent.current.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(imageVector = FluentIcon.MusicNote2, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(24.dp)) }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(title ?: "", color = textColor, fontSize = (13 * textScale).sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist ?: "", color = textColor.copy(alpha = 0.6f), fontSize = (11 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ctrl = controller
                IconButton(onClick = { ctrl?.transportControls?.skipToPrevious() }) {
                    Icon(imageVector = FluentIcon.SkipBack, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(LocalWidgetAccent.current)
                        .clickable {
                            if (isPlaying) ctrl?.transportControls?.pause()
                            else ctrl?.transportControls?.play()
                            isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (isPlaying) FluentIcon.Pause else FluentIcon.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { ctrl?.transportControls?.skipToNext() }) {
                    Icon(imageVector = FluentIcon.SkipForward, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 4. Step Counter Widget — NEW
// ─────────────────────────────────────────────────────────
@Composable
private fun StepsWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var steps by remember { mutableStateOf(0) }
    val goal  = 10000

    val hasSensor = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    DisposableEffect(Unit) {
        if (!hasSensor) return@DisposableEffect onDispose {}
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        var baseline = -1
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val total = e.values[0].toInt()
                if (baseline < 0) baseline = total
                steps = total - baseline
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }

    WidgetCard(isDark) {
        WidgetHeader("Steps", FluentIcon.WalkingIcon, textColor, textScale)
        Spacer(Modifier.height(12.dp))

        if (!hasSensor) {
            Text("Step counter not available on this device", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Ring progress
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    val accentColor = LocalWidgetAccent.current
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sweep = (steps.toFloat() / goal).coerceIn(0f, 1f) * 360f
                        drawArc(color = widgetDividerColor(textColor),
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(color = accentColor,
                            startAngle = -90f, sweepAngle = sweep, useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$steps", color = textColor, fontSize = (14 * textScale).sp, fontWeight = FontWeight.Bold)
                        Text("steps", color = textColor.copy(alpha = 0.5f), fontSize = (9 * textScale).sp, fontWeight = FontWeight.Medium)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Goal: $goal steps", color = textColor.copy(alpha = 0.6f), fontSize = (11 * textScale).sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    val pct = (steps.toFloat() / goal * 100).toInt().coerceIn(0, 100)
                    LinearProgressIndicator(
                        progress = { steps.toFloat() / goal },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color      = LocalWidgetAccent.current,
                        trackColor = widgetDividerColor(textColor)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$pct% of daily goal", color = textColor.copy(alpha = 0.5f), fontSize = (10 * textScale).sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 5. Stock / Markets Widget (real quotes via Stooq's public CSV endpoint —
//    no API key required; delayed data, refreshed on demand)
// ─────────────────────────────────────────────────────────
private val TRACKED_SYMBOLS = listOf("msft.us", "aapl.us", "googl.us", "amzn.us", "tsla.us", "nvda.us", "meta.us", "nflx.us")
private const val STOCKS_COLLAPSED_COUNT = 5

@Composable
private fun StockWidget(isDark: Boolean, textScale: Float, refreshTick: Int) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var stocks   by remember { mutableStateOf<List<StockTicker>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            try {
                val symbols = TRACKED_SYMBOLS.joinToString(",")
                val url = "https://stooq.com/q/l/?s=$symbols&f=sd2t2ohlc&h&e=csv"
                val lines = URL(url).readText().lines().drop(1).filter { it.isNotBlank() }
                lines.mapNotNull { line ->
                    val cols = line.split(",")
                    if (cols.size < 7) return@mapNotNull null
                    val symbol = cols[0].removeSuffix(".US")
                    val open  = cols[3].toDoubleOrNull() ?: return@mapNotNull null
                    val high  = cols[4].toDoubleOrNull() ?: open
                    val low   = cols[5].toDoubleOrNull() ?: open
                    val close = cols[6].toDoubleOrNull() ?: open
                    val changePct = if (open != 0.0) (close - open) / open * 100.0 else 0.0
                    // Real open/low/high/close shape (4 points) rather than a
                    // fabricated multi-day history — honest about what we have.
                    val lo = minOf(open, low, high, close)
                    val hi = maxOf(open, low, high, close).coerceAtLeast(lo + 0.01)
                    val points = listOf(open, low, high, close).map { ((it - lo) / (hi - lo)).toFloat() }
                    StockTicker(
                        symbol   = symbol.uppercase(),
                        change   = "${if (changePct >= 0) "+" else ""}${"%.2f".format(changePct)}%",
                        price    = "$" + "%.2f".format(close),
                        isUp     = changePct >= 0,
                        points   = points
                    )
                }
            } catch (e: Exception) { null }
        }
        if (result.isNullOrEmpty()) { error = stocks.isEmpty() } else { stocks = result; error = false }
        loading = false
    }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Markets", color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalWidgetAccent.current)
            else Icon(imageVector = FluentIcon.ArrowTrendingUp, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        when {
            error && stocks.isEmpty() -> Text("Could not load markets", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
            else -> {
                val shown = if (expanded) stocks else stocks.take(STOCKS_COLLAPSED_COUNT)
                shown.forEach { stock ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.25f)) {
                        Text(stock.symbol, color = textColor, fontSize = (13 * textScale).sp, fontWeight = FontWeight.SemiBold)
                        Text(stock.price, color = textColor.copy(alpha = 0.5f), fontSize = (10 * textScale).sp, fontWeight = FontWeight.Medium)
                    }
                    Canvas(modifier = Modifier.weight(0.4f).height(22.dp)) {
                        val pts  = stock.points
                        val path = Path()
                        pts.forEachIndexed { i, yRatio ->
                            val x = i * (size.width / (pts.size - 1))
                            val y = size.height * (1 - yRatio)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path,
                            color = if (stock.isUp) bluebirdColors.SuccessGreen else bluebirdColors.DangerRed,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text(
                        stock.change,
                        color      = if (stock.isUp) bluebirdColors.SuccessGreen else bluebirdColors.DangerRed,
                        fontSize   = (12 * textScale).sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.weight(0.25f)
                    )
                }
                }
                if (stocks.size > STOCKS_COLLAPSED_COUNT) {
                    ExpandToggle(expanded, textScale) { expanded = !expanded }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 6. News Widget (real headlines via BBC News' public RSS feed)
// ─────────────────────────────────────────────────────────
private const val NEWS_RSS_URL = "https://feeds.bbci.co.uk/news/rss.xml"

private const val NEWS_COLLAPSED_COUNT = 4

@Composable
private fun NewsWidget(isDark: Boolean, textScale: Float, refreshTick: Int) {
    val context   = LocalContext.current
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var articles  by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var loading   by remember { mutableStateOf(true) }
    var usedFallback by remember { mutableStateOf(false) }
    var expanded  by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        loading = true
        val fetched = withContext(Dispatchers.IO) {
            try { parseRssFeed(URL(NEWS_RSS_URL).readText()) } catch (e: Exception) { null }
        }
        if (fetched.isNullOrEmpty()) {
            if (articles.isEmpty()) { articles = fallbackNews(); usedFallback = true }
        } else {
            articles = fetched; usedFallback = false
        }
        loading = false
    }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("News", color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalWidgetAccent.current)
            else Icon(imageVector = FluentIcon.DocumentText, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        val shownArticles = if (expanded) articles.take(8) else articles.take(NEWS_COLLAPSED_COUNT)
        shownArticles.forEach { article ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(LocalWidgetAccent.current.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(imageVector = FluentIcon.DocumentText, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.title, color = textColor, fontWeight = FontWeight.Medium,
                        fontSize = (12 * textScale).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(article.source, fontSize = (10 * textScale).sp, color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                }
            }
        }
        if (articles.size > NEWS_COLLAPSED_COUNT) {
            ExpandToggle(expanded, textScale) { expanded = !expanded }
        }
        if (usedFallback) {
            Spacer(Modifier.height(4.dp))
            Text("Offline — showing saved headlines", color = textColor.copy(alpha = 0.4f), fontSize = (9 * textScale).sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Minimal RSS 2.0 parser — good enough for standard <item><title>/<link> feeds. */
private fun parseRssFeed(xml: String): List<NewsArticle> {
    val items = xml.split("<item>").drop(1)
    return items.mapNotNull { item ->
        val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
            .find(item)?.groupValues?.get(1)
            ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim() ?: return@mapNotNull null
        val link = Regex("<link>(.*?)</link>", RegexOption.DOT_MATCHES_ALL)
            .find(item)?.groupValues?.get(1)?.trim() ?: return@mapNotNull null
        NewsArticle(title = title, source = "BBC News", url = link)
    }
}

// Kept only as an offline fallback if the live RSS fetch fails — no longer
// the primary data path.
private fun fallbackNews() = listOf(
    NewsArticle("AI Revolution reshapes global technology landscape", "TechCrunch", "https://techcrunch.com"),
    NewsArticle("Markets soar on positive economic data", "Reuters", "https://reuters.com"),
    NewsArticle("Space mission captures stunning images of distant galaxy", "NASA", "https://nasa.gov"),
    NewsArticle("New Android features you should try today", "Android Central", "https://androidcentral.com"),
)


// ─────────────────────────────────────────────────────────
// 7. Calendar Widget (real events, fixed permission refresh)
// ─────────────────────────────────────────────────────────
@Composable
private fun CalendarWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        events = withContext(Dispatchers.IO) { getUpcomingEvents(context, 5) }
    }

    WidgetCard(isDark) {
        WidgetHeader("Calendar", FluentIcon.Calendar, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        when {
            !hasPermission -> TextButton(onClick = { permLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                Text("Grant calendar access", fontSize = (12 * textScale).sp)
            }
            events.isEmpty() -> Text("No upcoming events", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
            else -> events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(28.dp).background(event.color, RoundedCornerShape(2.dp)))
                    Column {
                        Text(event.title, color = textColor, fontSize = (12 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(event.time, color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun getUpcomingEvents(context: Context, maxResults: Int): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.EVENT_COLOR)
    val now = System.currentTimeMillis()
    val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DELETED} = 0)"
    try {
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI, projection, selection, arrayOf(now.toString()),
            "${CalendarContract.Events.DTSTART} ASC LIMIT $maxResults"
        )?.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Event"
                val start = it.getLong(1)
                val color = try { Color(it.getInt(2)) } catch (e: Exception) { bluebirdColors.AccentBlue }
                events.add(CalendarEvent(title, SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(start)), color))
            }
        }
    } catch (e: SecurityException) { }
    return events
}

// ─────────────────────────────────────────────────────────
// 8. Photos Widget (fixed permission refresh)
// ─────────────────────────────────────────────────────────
@Composable
private fun PhotosWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        photos = withContext(Dispatchers.IO) { getRecentPhotos(context, 10) }
    }

    WidgetCard(isDark) {
        WidgetHeader("Photos", FluentIcon.Image, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        when {
            !hasPermission -> TextButton(onClick = { permLauncher.launch(permission) }) {
                Text("Grant photo access", fontSize = (12 * textScale).sp)
            }
            photos.isEmpty() -> Text("No recent photos", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(photos) { uri ->
                    AsyncImage(
                        model = uri, contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

private fun getRecentPhotos(context: Context, maxCount: Int): List<Uri> {
    val uris       = mutableListOf<Uri>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    try {
        context.contentResolver.query(
            collection, arrayOf(MediaStore.Images.Media._ID), null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var n = 0
            while (cursor.moveToNext() && n < maxCount) {
                uris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(col)))
                n++
            }
        }
    } catch (e: SecurityException) { }
    return uris
}

// Sports and Traffic widgets were removed: both only ever rendered static,
// hardcoded sample data (fake scores, a fixed "25 min to home"), and neither
// has a real data source available without a paid/keyed API (live sports
// scores, and Google's Directions/Traffic API respectively). Rather than
// ship something that looks live but never changes, they're gone. If you
// get an API key for either later (e.g. TheSportsDB, Google Maps Platform),
// they can follow the same real-fetch pattern now used by Weather/Stocks/News.

// ─────────────────────────────────────────────────────────
// 11. To‑Do Widget (persistent, swipe-to-delete)
// ─────────────────────────────────────────────────────────
@Composable
private fun TodoWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var tasks        by remember { mutableStateOf(loadTodos(context)) }
    var newTaskText  by remember { mutableStateOf("") }
    var showAddField by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun save(updated: List<TodoTask>) { tasks = updated; saveTodos(context, updated) }
    fun submitNewTask() {
        if (newTaskText.isNotBlank()) {
            save(tasks + TodoTask(newTaskText.trim(), false))
            newTaskText = ""
        }
    }

    LaunchedEffect(showAddField) {
        if (showAddField) { delay(80); focusRequester.requestFocus() }
    }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("To Do", color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val done = tasks.count { it.done }
                if (tasks.isNotEmpty()) {
                    Text("$done/${tasks.size}", color = textColor.copy(alpha = 0.45f), fontSize = (11 * textScale).sp,
                        modifier = Modifier.padding(end = 6.dp))
                }
                IconButton(onClick = { showAddField = !showAddField }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (showAddField) FluentIcon.Dismiss else FluentIcon.Add,
                        contentDescription = if (showAddField) "Cancel" else "Add task",
                        tint = LocalWidgetAccent.current, modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (tasks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else tasks.count { it.done }.toFloat() / tasks.size },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = LocalWidgetAccent.current,
                trackColor = widgetDividerColor(textColor)
            )
        }
        Spacer(Modifier.height(10.dp))

        if (tasks.isEmpty() && !showAddField) {
            Text("Nothing on your list — tap + to add a task", color = textColor.copy(alpha = 0.45f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
        }

        tasks.forEachIndexed { idx, task ->
            var offsetX    by remember { mutableFloatStateOf(0f) }
            var dismissed  by remember { mutableStateOf(false) }
            if (dismissed) {
                LaunchedEffect(Unit) { save(tasks.toMutableList().also { it.removeAt(idx) }) }
                return@forEachIndexed
            }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                // Delete bg
                if (offsetX < -30f) {
                    Box(
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(10.dp))
                            .background(bluebirdColors.Error.copy(alpha = (-offsetX / 200f).coerceIn(0f, 1f))),
                        contentAlignment = Alignment.CenterEnd
                    ) { Icon(imageVector = FluentIcon.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 12.dp).size(16.dp)) }
                }
                Row(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.toInt(), 0) }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (task.done) Color.Transparent else (if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { if (offsetX < -160f) dismissed = true else offsetX = 0f },
                                onHorizontalDrag = { _, delta -> offsetX = (offsetX + delta).coerceAtMost(0f) }
                            )
                        }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { checked -> save(tasks.toMutableList().also { it[idx] = it[idx].copy(done = checked) }) },
                        colors   = CheckboxDefaults.colors(checkedColor = LocalWidgetAccent.current, checkmarkColor = Color.White),
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        task.text,
                        color          = if (task.done) textColor.copy(alpha = 0.35f) else textColor,
                        fontSize       = (13 * textScale).sp,
                        fontWeight     = FontWeight.Medium,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        modifier       = Modifier.weight(1f)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showAddField) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = newTaskText, onValueChange = { newTaskText = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester), singleLine = true,
                    placeholder = { Text("New task", fontSize = (12 * textScale).sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = (13 * textScale).sp, color = textColor),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                        submitNewTask(); showAddField = false
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalWidgetAccent.current,
                        unfocusedBorderColor = widgetDividerColor(textColor)
                    )
                )
                IconButton(onClick = { submitNewTask(); showAddField = false }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = FluentIcon.Checkmark, contentDescription = "Save task", tint = bluebirdColors.SuccessGreen)
                }
            }
        }

        if (tasks.any { it.done }) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Clear completed",
                color = textColor.copy(alpha = 0.45f),
                fontSize = (10 * textScale).sp,
                modifier = Modifier.clickable { save(tasks.filterNot { it.done }) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// 12. Upcoming Alarm Widget — NEW
// ─────────────────────────────────────────────────────────
@Composable
private fun AlarmWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val nextAlarm = remember {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.nextAlarmClock?.triggerTime?.let { triggerMs ->
                SimpleDateFormat("h:mm a, EEE MMM d", Locale.getDefault()).format(Date(triggerMs))
            }
        } catch (e: Exception) { null }
    }

    WidgetCard(isDark) {
        WidgetHeader("Next Alarm", FluentIcon.Alarm, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        if (nextAlarm == null) {
            Text("No alarm set", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = FluentIcon.Alarm, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(20.dp))
                Text(nextAlarm, color = textColor, fontSize = (14 * textScale).sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 13. Network Speed Widget — NEW
// ─────────────────────────────────────────────────────────
@Composable
private fun NetworkSpeedWidget(isDark: Boolean, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val windowRuntime = LocalWindowRuntime.current
    var rxSpeed by remember { mutableStateOf("— KB/s") }
    var txSpeed by remember { mutableStateOf("— KB/s") }

    LaunchedEffect(Unit) {
        var prevRx = TrafficStats.getTotalRxBytes()
        var prevTx = TrafficStats.getTotalTxBytes()
        while (isActive) {
            // Hidden widgets should not poll at dashboard cadence.
            delay(if (windowRuntime.isMinimized) 10000 else 2000)
            val curRx = TrafficStats.getTotalRxBytes()
            val curTx = TrafficStats.getTotalTxBytes()
            val diffRx = (curRx - prevRx).coerceAtLeast(0)
            val diffTx = (curTx - prevTx).coerceAtLeast(0)
            rxSpeed = formatSpeed(diffRx)
            txSpeed = formatSpeed(diffTx)
            prevRx = curRx; prevTx = curTx
        }
    }

    WidgetCard(isDark) {
        WidgetHeader("Network Speed", FluentIcon.TopSpeed, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = FluentIcon.ArrowDownload, contentDescription = null, tint = LocalWidgetAccent.current, modifier = Modifier.size(14.dp))
                    Text("Download", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp, fontWeight = FontWeight.Medium)
                }
                Text(rxSpeed, color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(widgetDividerColor(textColor)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = FluentIcon.ArrowUpload, contentDescription = null, tint = bluebirdColors.Success, modifier = Modifier.size(14.dp))
                    Text("Upload", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp, fontWeight = FontWeight.Medium)
                }
                Text(txSpeed, color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024      -> "${bytesPerSec} B/s"
        bytesPerSec < 1024*1024 -> "${"%.1f".format(bytesPerSec/1024f)} KB/s"
        else                    -> "${"%.1f".format(bytesPerSec/1024f/1024f)} MB/s"
    }
}

// ─────────────────────────────────────────────────────────
// 14. Screen Time Widget — NEW
// ─────────────────────────────────────────────────────────
@Composable
private fun ScreenTimeWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val hasPermission = remember {
        try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    data class AppUsage(val name: String, val minutes: Long, val fraction: Float)

    var topApps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val usm   = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                val end   = System.currentTimeMillis()
                val start = end - 24 * 60 * 60 * 1000L
                val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, start, end)
                    .filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(4)
                val total = stats.sumOf { it.totalTimeInForeground }.coerceAtLeast(1)
                val result = stats.map { s ->
                    val name = try {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(s.packageName, 0)
                        ).toString()
                    } catch (e: Exception) { s.packageName.substringAfterLast('.') }
                    AppUsage(name, s.totalTimeInForeground / 60000, s.totalTimeInForeground.toFloat() / total)
                }
                withContext(Dispatchers.Main) { topApps = result }
            } catch (e: Exception) { }
        }
    }

    WidgetCard(isDark) {
        WidgetHeader("Screen Time", FluentIcon.PhoneAndroid, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        if (!hasPermission) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Grant usage access", fontSize = (12 * textScale).sp) }
        } else if (topApps.isEmpty()) {
            Text("No data yet", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
        } else {
            topApps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(app.name, color = textColor, fontSize = (12 * textScale).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(90.dp))
                    LinearProgressIndicator(
                        progress = { app.fraction },
                        modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color      = LocalWidgetAccent.current,
                        trackColor = widgetDividerColor(textColor)
                    )
                    Text("${app.minutes}m", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp,
                        modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}
