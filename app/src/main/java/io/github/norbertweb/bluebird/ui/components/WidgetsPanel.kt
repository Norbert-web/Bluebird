package io.github.norbertweb.bluebird.ui.components

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
    val default = listOf("clock","weather","music","steps","stocks","news","calendar","photos","sports","traffic","todo","alarm","network","screentime")
    val saved = context.widgetPrefs().getString("widget_order", null) ?: return default
    val savedList = saved.split(",").filter { it.isNotBlank() }
    // merge: keep saved order, append any new ones not yet in list
    return (savedList + default.filter { it !in savedList })
}

private fun saveHiddenWidgets(context: Context, hidden: Set<String>) {
    context.widgetPrefs().edit().putStringSet("hidden_widgets", hidden).apply()
}

private fun loadHiddenWidgets(context: Context): Set<String> =
    context.widgetPrefs().getStringSet("hidden_widgets", emptySet()) ?: emptySet()

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
private data class MatchScore(val team1: String, val score1: String, val team2: String, val score2: String)

// ─── Widget IDs ───────────────────────────────────────────────────────────────

private val ALL_WIDGET_IDS = listOf(
    "clock", "weather", "music", "steps", "stocks",
    "news", "calendar", "photos", "sports", "traffic",
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
    "sports"     to "Sports",
    "traffic"    to "Traffic",
    "todo"       to "To Do",
    "alarm"      to "Next Alarm",
    "network"    to "Network Speed",
    "screentime" to "Screen Time"
)

// ─────────────────────────────────────────────────────────
// Main Widget Panel
// ─────────────────────────────────────────────────────────
@Composable
fun WidgetsPanel(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context    = LocalContext.current
    val isDark     = uiState.isDarkTheme
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val scope      = rememberCoroutineScope()
    val textScale  = LocalTextScale.current

    var isRefreshing  by remember { mutableStateOf(false) }
    var editMode      by remember { mutableStateOf(false) }
    var widgetOrder   by remember { mutableStateOf(loadWidgetOrder(context)) }
    var hiddenWidgets by remember { mutableStateOf(loadHiddenWidgets(context)) }

    // Refresh trigger — widgets observe this to re-fetch
    var refreshTick by remember { mutableStateOf(0) }

    AcrylicSurface(
        modifier = modifier.width(380.dp).fillMaxHeight(),
        isDark = isDark, alpha = 0.96f, cornerRadius = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    // Refresh
                    val rotation by animateFloatAsState(
                        targetValue   = if (isRefreshing) 360f else 0f,
                        animationSpec = if (isRefreshing)
                            infiniteRepeatable(tween(900, easing = LinearEasing))
                        else tween(0),
                        label = "refresh"
                    )
                    IconButton(onClick = {
                        if (!isRefreshing) {
                            isRefreshing = true
                            refreshTick++
                            scope.launch { delay(1500); isRefreshing = false }
                        }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp).rotate(rotation))
                    }
                    // Edit / Done
                    IconButton(onClick = { editMode = !editMode },
                        modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (editMode) Icons.Default.Check else Icons.Default.Edit,
                            null, tint = if (editMode) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Edit mode: visibility toggles ─────────────────────────────────
            AnimatedVisibility(visible = editMode) {
                WidgetEditPanel(
                    order       = widgetOrder,
                    hidden      = hiddenWidgets,
                    isDark      = isDark,
                    textColor   = textColor,
                    textScale   = textScale,
                    onToggle    = { id ->
                        hiddenWidgets = if (id in hiddenWidgets)
                            hiddenWidgets - id else hiddenWidgets + id
                        saveHiddenWidgets(context, hiddenWidgets)
                    },
                    onMoveUp    = { id ->
                        val idx = widgetOrder.indexOf(id)
                        if (idx > 0) {
                            val newOrder = widgetOrder.toMutableList().also {
                                val tmp = it[idx]; it[idx] = it[idx - 1]; it[idx - 1] = tmp
                            }
                            widgetOrder = newOrder
                            saveWidgetOrder(context, newOrder)
                        }
                    },
                    onMoveDown  = { id ->
                        val idx = widgetOrder.indexOf(id)
                        if (idx < widgetOrder.size - 1) {
                            val newOrder = widgetOrder.toMutableList().also {
                                val tmp = it[idx]; it[idx] = it[idx + 1]; it[idx + 1] = tmp
                            }
                            widgetOrder = newOrder
                            saveWidgetOrder(context, newOrder)
                        }
                    }
                )
            }

            // ── Render widgets in saved order ─────────────────────────────────
            widgetOrder.filter { it !in hiddenWidgets }.forEach { id ->
                key(id) {
                    when (id) {
                        "clock"      -> ClockWidget(isDark, context, textScale)
                        "weather"    -> WeatherWidget(isDark, context, textScale, refreshTick)
                        "music"      -> NowPlayingWidget(isDark, context, textScale)
                        "steps"      -> StepsWidget(isDark, context, textScale)
                        "stocks"     -> StockWidget(isDark, textScale, refreshTick)
                        "news"       -> NewsWidget(isDark, textScale, refreshTick)
                        "calendar"   -> CalendarWidget(isDark, context, textScale)
                        "photos"     -> PhotosWidget(isDark, context, textScale)
                        "sports"     -> SportsWidget(isDark, textScale, refreshTick)
                        "traffic"    -> TrafficWidget(isDark, textScale)
                        "todo"       -> TodoWidget(isDark, context, textScale)
                        "alarm"      -> AlarmWidget(isDark, context, textScale)
                        "network"    -> NetworkSpeedWidget(isDark, textScale)
                        "screentime" -> ScreenTimeWidget(isDark, context, textScale)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Edit Panel ───────────────────────────────────────────────────────────────

@Composable
private fun WidgetEditPanel(
    order: List<String>,
    hidden: Set<String>,
    isDark: Boolean,
    textColor: Color,
    textScale: Float,
    onToggle: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF0F0F0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Edit Widgets",
            color      = bluebirdColors.AccentBlue,
            fontSize   = (12 * textScale).sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 4.dp)
        )
        order.forEach { id ->
            val label   = WIDGET_LABELS[id] ?: id
            val visible = id !in hidden
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Switch(
                    checked  = visible,
                    onCheckedChange = { onToggle(id) },
                    colors   = SwitchDefaults.colors(checkedThumbColor = bluebirdColors.AccentBlue),
                    modifier = Modifier.size(36.dp, 20.dp)
                )
                Text(
                    label, color = textColor,
                    fontSize = (12 * textScale).sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onMoveUp(id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onMoveDown(id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Shared card wrapper ──────────────────────────────────────────────────────

@Composable
private fun WidgetCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content   = { Column(modifier = Modifier.padding(14.dp), content = content) }
    )
}

@Composable
private fun WidgetHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, textColor: Color, textScale: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = textColor, fontSize = (14 * textScale).sp)
        Icon(icon, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────────────────
// 1. Clock & World Time Widget (NEW)
// ─────────────────────────────────────────────────────────
@Composable
private fun ClockWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val worldClocks by remember { mutableStateOf(loadWorldClocks(context)) }

    // Tick every second
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); now = System.currentTimeMillis() }
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
            HorizontalDivider(color = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDEDEDE), thickness = 0.5.dp)
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
                        Icon(Icons.Default.Language, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(14.dp))
                        Text(wc.label, color = textColor.copy(alpha = 0.7f), fontSize = (12 * textScale).sp)
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
                        Icon(Icons.Default.LocationOff, null,
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
                        Icon(Icons.Default.CloudOff, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Could not load weather", color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                    }
                }
                weatherData != null -> {
                    val data = weatherData!!
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text(data.city, style = MaterialTheme.typography.titleMedium, color = Color.White, fontSize = (14 * textScale).sp)
                                Text(data.condition, color = Color.White.copy(alpha = 0.8f), fontSize = (12 * textScale).sp)
                                Spacer(Modifier.height(8.dp))
                                Text("${data.temp}°", fontSize = (48 * textScale).sp, color = Color.White, fontWeight = FontWeight.Light)
                                Text("H:${data.high}°  L:${data.low}°", color = Color.White.copy(alpha = 0.7f), fontSize = (11 * textScale).sp)
                            }
                            Icon(
                                when {
                                    data.condition.contains("Clear") || data.condition.contains("Sunny") -> Icons.Default.WbSunny
                                    data.condition.contains("Cloud") -> Icons.Default.WbCloudy
                                    data.condition.contains("Rain")  -> Icons.Default.Umbrella
                                    else -> Icons.Default.WbSunny
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

    // Check notification listener permission (needed for MediaSessionManager)
    val hasPermission = remember {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        enabled.contains(context.packageName)
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return@LaunchedEffect
            val sessions = msm.getActiveSessions(
                android.content.ComponentName(context, io.github.norbertweb.bluebird.data.NotificationListener::class.java)
            )
            val active = sessions.firstOrNull()
            controller = active
            val meta = active?.metadata
            title    = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            artist   = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
            albumArt = meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            isPlaying = active?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        } catch (e: Exception) { /* permission not granted or no active session */ }
    }

    WidgetCard(isDark) {
        WidgetHeader("Now Playing", Icons.Default.MusicNote, textColor, textScale)
        Spacer(Modifier.height(10.dp))

        if (!hasPermission) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Grant notification access", fontSize = (12 * textScale).sp) }
        } else if (title == null) {
            Text("Nothing playing", color = textColor.copy(alpha = 0.5f), fontSize = (13 * textScale).sp)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album art
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
                            .background(bluebirdColors.AccentBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.MusicNote, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(24.dp)) }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(title ?: "", color = textColor, fontSize = (13 * textScale).sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist ?: "", color = textColor.copy(alpha = 0.6f), fontSize = (11 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Default.SkipPrevious, null, tint = textColor, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(bluebirdColors.AccentBlue)
                        .clickable {
                            if (isPlaying) ctrl?.transportControls?.pause()
                            else ctrl?.transportControls?.play()
                            isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { ctrl?.transportControls?.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, null, tint = textColor, modifier = Modifier.size(28.dp))
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
        WidgetHeader("Steps", Icons.Default.DirectionsWalk, textColor, textScale)
        Spacer(Modifier.height(12.dp))

        if (!hasSensor) {
            Text("Step counter not available on this device", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Ring progress
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sweep = (steps.toFloat() / goal).coerceIn(0f, 1f) * 360f
                        drawArc(color = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD),
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(color = bluebirdColors.AccentBlue,
                            startAngle = -90f, sweepAngle = sweep, useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$steps", color = textColor, fontSize = (14 * textScale).sp, fontWeight = FontWeight.Bold)
                        Text("steps", color = textColor.copy(alpha = 0.5f), fontSize = (9 * textScale).sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Goal: $goal steps", color = textColor.copy(alpha = 0.6f), fontSize = (11 * textScale).sp)
                    Spacer(Modifier.height(4.dp))
                    val pct = (steps.toFloat() / goal * 100).toInt().coerceIn(0, 100)
                    LinearProgressIndicator(
                        progress = { steps.toFloat() / goal },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color      = bluebirdColors.AccentBlue,
                        trackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$pct% of daily goal", color = textColor.copy(alpha = 0.5f), fontSize = (10 * textScale).sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 5. Stock / Markets Widget
// ─────────────────────────────────────────────────────────
@Composable
private fun StockWidget(isDark: Boolean, textScale: Float, refreshTick: Int) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var stocks  by remember { mutableStateOf(stableStockData()) }
    var loading by remember { mutableStateOf(false) }

    // In production, replace with real API (Yahoo Finance, Alpha Vantage, etc.)
    // For now uses stable mock data — not random on every recomposition
    LaunchedEffect(refreshTick) {
        if (refreshTick == 0) return@LaunchedEffect
        loading = true; delay(800); loading = false
        stocks = stableStockData() // swap with real API call
    }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Markets", style = MaterialTheme.typography.titleMedium, color = textColor, fontSize = (14 * textScale).sp)
            if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = bluebirdColors.AccentBlue)
            else Icon(Icons.Default.TrendingUp, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        stocks.forEach { stock ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.25f)) {
                    Text(stock.symbol, color = textColor, fontSize = (13 * textScale).sp, fontWeight = FontWeight.SemiBold)
                    Text(stock.price, color = textColor.copy(alpha = 0.5f), fontSize = (10 * textScale).sp)
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
    }
}

private fun stableStockData() = listOf(
    StockTicker("MSFT",  "+2.4%", "$415.32", true,  listOf(0.4f,0.45f,0.5f,0.48f,0.55f,0.6f,0.65f)),
    StockTicker("AAPL",  "-0.8%", "$189.45", false, listOf(0.7f,0.65f,0.6f,0.62f,0.58f,0.55f,0.5f)),
    StockTicker("GOOGL", "+1.2%", "$172.18", true,  listOf(0.3f,0.35f,0.38f,0.4f,0.42f,0.45f,0.5f)),
    StockTicker("AMZN",  "+3.1%", "$192.74", true,  listOf(0.2f,0.3f,0.4f,0.45f,0.5f,0.55f,0.6f)),
    StockTicker("TSLA",  "-2.1%", "$174.60", false, listOf(0.8f,0.7f,0.65f,0.6f,0.55f,0.5f,0.45f)),
)

// ─────────────────────────────────────────────────────────
// 6. News Widget
// ─────────────────────────────────────────────────────────
@Composable
private fun NewsWidget(isDark: Boolean, textScale: Float, refreshTick: Int) {
    val context   = LocalContext.current
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var articles  by remember { mutableStateOf(fallbackNews()) }
    var loading   by remember { mutableStateOf(false) }

    // Replace RSS_URL with a real RSS feed if desired (e.g. BBC, Reuters)
    LaunchedEffect(refreshTick) {
        loading = true; delay(600); loading = false
        // Real API: fetch from https://newsapi.org or parse an RSS feed here
    }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("News", style = MaterialTheme.typography.titleMedium, color = textColor, fontSize = (14 * textScale).sp)
            if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = bluebirdColors.AccentBlue)
            else Icon(Icons.Default.Article, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        articles.take(4).forEach { article ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(bluebirdColors.AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Article, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.title, style = MaterialTheme.typography.labelLarge, color = textColor,
                        fontSize = (12 * textScale).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(article.source, fontSize = (10 * textScale).sp, color = textColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

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
        WidgetHeader("Calendar", Icons.Default.CalendarToday, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        when {
            !hasPermission -> TextButton(onClick = { permLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                Text("Grant calendar access", fontSize = (12 * textScale).sp)
            }
            events.isEmpty() -> Text("No upcoming events", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp)
            else -> events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(28.dp).background(event.color, RoundedCornerShape(2.dp)))
                    Column {
                        Text(event.title, color = textColor, fontSize = (12 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(event.time, color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp)
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
        WidgetHeader("Photos", Icons.Default.Photo, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        when {
            !hasPermission -> TextButton(onClick = { permLauncher.launch(permission) }) {
                Text("Grant photo access", fontSize = (12 * textScale).sp)
            }
            photos.isEmpty() -> Text("No recent photos", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp)
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

// ─────────────────────────────────────────────────────────
// 9. Sports Widget
// ─────────────────────────────────────────────────────────
@Composable
private fun SportsWidget(isDark: Boolean, textScale: Float, refreshTick: Int) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val scores = remember {
        listOf(
            MatchScore("Warriors",   "98",  "Celtics",    "102"),
            MatchScore("Lakers",     "114", "Bucks",      "109"),
            MatchScore("Real Madrid","3",   "Barcelona",  "1"),
        )
    }

    WidgetCard(isDark) {
        WidgetHeader("Sports", Icons.Default.SportsBasketball, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        scores.forEach { (team1, score1, team2, score2) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(team1, color = textColor, fontSize = (12 * textScale).sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bluebirdColors.AccentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("$score1 – $score2", color = textColor, fontSize = (12 * textScale).sp, fontWeight = FontWeight.Bold)
                }
                Text(team2, color = textColor, fontSize = (12 * textScale).sp,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 10. Traffic Widget
// ─────────────────────────────────────────────────────────
@Composable
private fun TrafficWidget(isDark: Boolean, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    WidgetCard(isDark) {
        WidgetHeader("Traffic", Icons.Default.DirectionsCar, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB900), modifier = Modifier.size(16.dp))
            Text("Moderate traffic on your route", color = textColor, fontSize = (12 * textScale).sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("~25 min to home", color = textColor.copy(alpha = 0.55f), fontSize = (11 * textScale).sp)
    }
}

// ─────────────────────────────────────────────────────────
// 11. To‑Do Widget (persistent, swipe-to-delete)
// ─────────────────────────────────────────────────────────
@Composable
private fun TodoWidget(isDark: Boolean, context: Context, textScale: Float) {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var tasks        by remember { mutableStateOf(loadTodos(context)) }
    var newTaskText  by remember { mutableStateOf("") }
    var showAddField by remember { mutableStateOf(false) }

    fun save(updated: List<TodoTask>) { tasks = updated; saveTodos(context, updated) }

    WidgetCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("To Do", style = MaterialTheme.typography.titleMedium, color = textColor, fontSize = (14 * textScale).sp)
            Row {
                val done = tasks.count { it.done }
                Text("$done/${tasks.size}", color = textColor.copy(alpha = 0.45f), fontSize = (11 * textScale).sp,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 6.dp))
                IconButton(onClick = { showAddField = !showAddField }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Add, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        tasks.forEachIndexed { idx, task ->
            var offsetX    by remember { mutableFloatStateOf(0f) }
            var dismissed  by remember { mutableStateOf(false) }
            if (dismissed) {
                LaunchedEffect(Unit) { save(tasks.toMutableList().also { it.removeAt(idx) }) }
                return@forEachIndexed
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                // Delete bg
                if (offsetX < -30f) {
                    Box(
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(8.dp))
                            .background(bluebirdColors.Error.copy(alpha = (-offsetX / 200f).coerceIn(0f, 1f))),
                        contentAlignment = Alignment.CenterEnd
                    ) { Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 12.dp).size(16.dp)) }
                }
                Row(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.toInt(), 0) }
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { if (offsetX < -160f) dismissed = true else offsetX = 0f },
                                onHorizontalDrag = { _, delta -> offsetX = (offsetX + delta).coerceAtMost(0f) }
                            )
                        }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { checked -> save(tasks.toMutableList().also { it[idx] = it[idx].copy(done = checked) }) },
                        colors   = CheckboxDefaults.colors(checkedColor = bluebirdColors.AccentBlue, checkmarkColor = Color.White),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        task.text,
                        color          = if (task.done) textColor.copy(alpha = 0.35f) else textColor,
                        fontSize       = (12 * textScale).sp,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        modifier       = Modifier.weight(1f)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showAddField) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = newTaskText, onValueChange = { newTaskText = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("New task", fontSize = (12 * textScale).sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = (13 * textScale).sp)
                )
                IconButton(onClick = {
                    if (newTaskText.isNotBlank()) {
                        save(tasks + TodoTask(newTaskText.trim(), false))
                        newTaskText = ""; showAddField = false
                    }
                }) { Icon(Icons.Default.Check, null, tint = bluebirdColors.SuccessGreen) }
            }
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
        WidgetHeader("Next Alarm", Icons.Default.Alarm, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        if (nextAlarm == null) {
            Text("No alarm set", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Alarm, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(20.dp))
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
    var rxSpeed by remember { mutableStateOf("— KB/s") }
    var txSpeed by remember { mutableStateOf("— KB/s") }

    LaunchedEffect(Unit) {
        var prevRx = TrafficStats.getTotalRxBytes()
        var prevTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(1000)
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
        WidgetHeader("Network Speed", Icons.Default.Speed, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.ArrowDownward, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(14.dp))
                    Text("Download", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp)
                }
                Text(rxSpeed, color = textColor, fontSize = (15 * textScale).sp, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(if (isDark) Color(0xFF3A3A3A) else Color(0xFFCCCCCC)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, tint = bluebirdColors.Success, modifier = Modifier.size(14.dp))
                    Text("Upload", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp)
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
        WidgetHeader("Screen Time", Icons.Default.PhoneAndroid, textColor, textScale)
        Spacer(Modifier.height(10.dp))
        if (!hasPermission) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Grant usage access", fontSize = (12 * textScale).sp) }
        } else if (topApps.isEmpty()) {
            Text("No data yet", color = textColor.copy(alpha = 0.5f), fontSize = (12 * textScale).sp)
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
                        color      = bluebirdColors.AccentBlue,
                        trackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD)
                    )
                    Text("${app.minutes}m", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textScale).sp,
                        modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}
