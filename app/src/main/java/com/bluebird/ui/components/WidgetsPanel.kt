package com.bluebird.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.bluebird.LauncherUiState
import com.bluebird.ui.theme.Win11Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

// ─────────────────────────────────────────────────────────
// Main Widget Panel (acrylic, scrollable, full of live widgets)
// ─────────────────────────────────────────────────────────
@Composable
fun WidgetsPanel(
    uiState: LauncherUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = uiState.isDarkTheme
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val scope = rememberCoroutineScope()

    // Simulated “refresh” state
    var isRefreshing by remember { mutableStateOf(false) }

    AcrylicSurface(
        modifier = modifier
            .width(360.dp)  // slightly wider for richer layout
            .fillMaxHeight(),
        isDark = isDark,
        alpha = 0.96f,
        cornerRadius = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header with “Add widgets” and refresh ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Refresh button with animation
                    IconButton(
                        onClick = {
                            if (!isRefreshing) {
                                isRefreshing = true
                                scope.launch {
                                    // Simulate network delay
                                    delay(1200)
                                    isRefreshing = false
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isRefreshing) 360f else 0f,
                            animationSpec = if (isRefreshing) infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing)
                            ) else tween(0)
                        )
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotation)
                        )
                    }

                    // Add widgets button
                    IconButton(
                        onClick = { /* Open widget gallery – placeholder */ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Widgets ──
            WeatherWidget(isDark, context)
            StockWidget(isDark)
            NewsWidget(isDark)
            CalendarWidget(isDark, context)
            PhotosWidget(isDark, context)
            SportsWidget(isDark)
            TrafficWidget(isDark)
            TodoWidget(isDark)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// 1. Weather Widget (location-aware, dynamic)
// ─────────────────────────────────────────────────────────
@Composable
private fun WeatherWidget(isDark: Boolean, context: Context) {
    // In a real app, fetch weather from API. Here we simulate a live update.
    val weatherData = remember {
        mutableStateOf(
            WeatherData(
                city = "Lamn Nobert",
                condition = "Fetching... Data",
                temp = 72,
                high = 78,
                low = 65,
                forecast = listOf(
                    DayForecast("Mon", "☀️", 75, 62),
                    DayForecast("Tue", "⛅", 68, 60),
                    DayForecast("Wed", "🌧", 61, 55),
                    DayForecast("Thu", "⛅", 70, 59),
                    DayForecast("Fri", "☀️", 78, 64),
                )
            )
        )
    }
    val data = weatherData.value

    // Condition‑based gradient background
    val gradientColors = remember(data.condition) {
        when {
            data.condition.contains("Sunny") || data.condition.contains("Clear") ->
                listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
            data.condition.contains("Cloud") ->
                listOf(Color(0xFF546E7A), Color(0xFF90A4AE))
            data.condition.contains("Rain") ->
                listOf(Color(0xFF455A64), Color(0xFF607D8B))
            else -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(data.city, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(data.condition, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(8.dp))
                        Text("${data.temp}°", fontSize = 52.sp, color = Color.White, fontWeight = FontWeight.Light)
                    }
                    // Weather icon (simplified)
                    Icon(
                        when {
                            data.condition.contains("Sunny") -> Icons.Default.WbSunny
                            data.condition.contains("Cloud") -> Icons.Default.WbCloudy
                            data.condition.contains("Rain") -> Icons.Default.Umbrella
                            else -> Icons.Default.WbSunny
                        },
                        null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 5‑day forecast horizontal pager
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(data.forecast) { day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(day.day, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text(day.icon, fontSize = 18.sp)
                            Text("${day.high}°", color = Color.White, fontSize = 12.sp)
                            Text("${day.low}°", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class WeatherData(
    val city: String,
    val condition: String,
    val temp: Int,
    val high: Int,
    val low: Int,
    val forecast: List<DayForecast>
)
private data class DayForecast(val day: String, val icon: String, val high: Int, val low: Int)

// ─────────────────────────────────────────────────────────
// 2. Stock Widget (multiple tickers + sparklines)
// ─────────────────────────────────────────────────────────
@Composable
private fun StockWidget(isDark: Boolean) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    // Simulated real‑time data (could be fetched from an API)
    var stocks by remember {
        mutableStateOf(
            listOf(
                StockTicker("MSFT", "+2.4%", true, mockSparklinePoints(7, true)),
                StockTicker("AAPL", "-0.8%", false, mockSparklinePoints(7, false)),
                StockTicker("GOOGL", "+1.2%", true, mockSparklinePoints(7, true)),
                StockTicker("AMZN", "+3.1%", true, mockSparklinePoints(7, true)),
                StockTicker("TSLA", "-2.1%", false, mockSparklinePoints(7, false)),
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Markets", style = MaterialTheme.typography.titleMedium, color = textColor)
                Icon(
                    Icons.Default.TrendingUp,
                    null,
                    tint = Win11Colors.AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            stocks.forEach { stock ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ticker name
                    Text(stock.symbol, color = textColor, fontSize = 13.sp, modifier = Modifier.weight(0.2f))

                    // Sparkline
                    Canvas(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(20.dp)
                    ) {
                        val pts = stock.points
                        val path = Path()
                        pts.forEachIndexed { i, yRatio ->
                            val x = i * (size.width / (pts.size - 1))
                            val y = size.height * (1 - yRatio)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path,
                            color = if (stock.isUp) Win11Colors.SuccessGreen else Win11Colors.DangerRed,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Change
                    Text(
                        stock.change,
                        color = if (stock.isUp) Win11Colors.SuccessGreen else Win11Colors.DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.2f)
                    )
                }
            }
        }
    }
}

private data class StockTicker(
    val symbol: String,
    val change: String,
    val isUp: Boolean,
    val points: List<Float>
)

private fun mockSparklinePoints(count: Int, isUp: Boolean): List<Float> {
    val rng = Random()
    var value = if (isUp) 0.5f else 0.8f
    return List(count) {
        value += rng.nextFloat() * 0.2f - 0.1f
        value.coerceIn(0.05f, 0.95f)
    }
}

// ─────────────────────────────────────────────────────────
// 3. News Widget (headlines with thumbnails, clickable)
// ─────────────────────────────────────────────────────────
@Composable
private fun NewsWidget(isDark: Boolean) {
    val context = LocalContext.current
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    // Simulated news – replace with API call
    val articles = remember {
        listOf(
            NewsArticle(
                "AI Revolution reshapes global technology landscape",
                "TechCrunch",
                "https://techcrunch.com"
            ),
            NewsArticle(
                "Markets soar on positive economic data, Fed signals patience",
                "Reuters",
                "https://reuters.com"
            ),
            NewsArticle(
                "Space mission captures stunning new images of distant galaxy",
                "NASA",
                "https://nasa.gov"
            ),
            NewsArticle(
                "New Android 15 features you should try today",
                "Android Central",
                "https://androidcentral.com"
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("News", style = MaterialTheme.typography.titleMedium, color = textColor)

            articles.take(4).forEach { article ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Open in browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Thumbnail placeholder – always a blue icon box
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Win11Colors.AccentBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Article,
                            null,
                            tint = Win11Colors.AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            article.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            article.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private data class NewsArticle(
    val title: String,
    val source: String,
    val url: String
)  // removed thumbnailRes field

// ─────────────────────────────────────────────────────────
// 4. Calendar Widget (real device events)
// ─────────────────────────────────────────────────────────
@Composable
private fun CalendarWidget(isDark: Boolean, context: Context) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    // Check calendar permission
    val hasCalendarPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Request permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* after grant we should refresh, but for now we'll just update */ }

    // Fetch upcoming events
    val events = remember(hasCalendarPermission) {
        if (hasCalendarPermission) getUpcomingEvents(context, 5) else emptyList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Calendar", style = MaterialTheme.typography.titleMedium, color = textColor)
                Icon(
                    Icons.Default.CalendarToday,
                    null,
                    tint = Win11Colors.AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            if (!hasCalendarPermission) {
                TextButton(onClick = {
                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }) {
                    Text("Grant calendar access")
                }
            } else if (events.isEmpty()) {
                Text("No upcoming events", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(event.color, RoundedCornerShape(2.dp))
                        )
                        Column {
                            Text(event.title, color = textColor, fontSize = 13.sp, maxLines = 1)
                            Text(
                                event.time,
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CalendarEvent(val title: String, val time: String, val color: Color)

private fun getUpcomingEvents(context: Context, maxResults: Int): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val uri = CalendarContract.Events.CONTENT_URI
    val projection = arrayOf(
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.EVENT_COLOR
    )
    val now = System.currentTimeMillis()
    val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DELETED} = 0)"
    val selectionArgs = arrayOf(now.toString())
    val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT $maxResults"

    try {
        val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Event"
                val startMillis = it.getLong(1)
                val color = try {
                    val colorVal = it.getInt(2)
                    Color(colorVal)
                } catch (e: Exception) {
                    Win11Colors.AccentBlue
                }
                val time = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(startMillis))
                events.add(CalendarEvent(title, time, color))
            }
        }
    } catch (e: SecurityException) {
        // permission denied
    }
    return events
}

// ─────────────────────────────────────────────────────────
// 5. Photos Widget (recent images from gallery)
// ─────────────────────────────────────────────────────────
@Composable
private fun PhotosWidget(isDark: Boolean, context: Context) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    // Request media permission (simplified)
    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* refresh after grant */ }

    val photos = remember(hasStoragePermission) {
        if (hasStoragePermission) getRecentPhotos(context, 10) else emptyList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Photos", style = MaterialTheme.typography.titleMedium, color = textColor)
            Spacer(Modifier.height(10.dp))

            if (!hasStoragePermission) {
                TextButton(onClick = {
                    permissionLauncher.launch(
                        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                        else Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }) {
                    Text("Grant photo access")
                }
            } else if (photos.isEmpty()) {
                Text("No recent photos", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(photos) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

private fun getRecentPhotos(context: Context, maxCount: Int): List<Uri> {
    val uris = mutableListOf<Uri>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    try {
        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var count = 0
            while (cursor.moveToNext() && count < maxCount) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                uris.add(contentUri)
                count++
            }
        }
    } catch (e: SecurityException) {
        // permission missing
    }
    return uris
}

// ─────────────────────────────────────────────────────────
// 6. Sports Widget (live scores placeholder)
// ─────────────────────────────────────────────────────────
@Composable
private fun SportsWidget(isDark: Boolean) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    // Use a custom data class instead of Triple with 4 args
    val scores = remember {
        listOf(
            MatchScore("Warriors", "98", "Celtics", "102"),
            MatchScore("Lakers", "114", "Bucks", "109"),
            MatchScore("Real Madrid", "3", "Barcelona", "1"),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sports", style = MaterialTheme.typography.titleMedium, color = textColor)
                Icon(Icons.Default.SportsBasketball, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))

            scores.forEach { (team1, score1, team2, score2) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(team1, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("$score1 - $score2", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(team2, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MatchScore(
    val team1: String,
    val score1: String,
    val team2: String,
    val score2: String
)

// ─────────────────────────────────────────────────────────
// 7. Traffic Widget (commute info)
// ─────────────────────────────────────────────────────────
@Composable
private fun TrafficWidget(isDark: Boolean) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Traffic", style = MaterialTheme.typography.titleMedium, color = textColor)
                Icon(Icons.Default.DirectionsCar, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB900), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Moderate traffic on your route", color = textColor, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("25 min to home", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────
// 8. To‑Do Widget (persistent, interactive)
// ─────────────────────────────────────────────────────────
@Composable
private fun TodoWidget(isDark: Boolean) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)
    var tasks by remember {
        mutableStateOf(
            mutableListOf(
                Task("Review pull request", true),
                Task("Update documentation", false),
                Task("Send weekly report", false),
                Task("Fix login bug", true),
            )
        )
    }
    var newTaskText by remember { mutableStateOf("") }
    var showAddField by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("To Do", style = MaterialTheme.typography.titleMedium, color = textColor)
                IconButton(
                    onClick = { showAddField = !showAddField },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))

            tasks.forEachIndexed { idx, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { checked ->
                            tasks = tasks.toMutableList().apply {
                                this[idx] = this[idx].copy(done = checked)
                            }.toMutableList()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Win11Colors.AccentBlue,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        task.text,
                        color = if (task.done) textColor.copy(alpha = 0.4f) else textColor,
                        fontSize = 13.sp,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null
                    )
                }
            }

            if (showAddField) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Add a task") }
                    )
                    IconButton(onClick = {
                        if (newTaskText.isNotBlank()) {
                            tasks = tasks.toMutableList().apply { add(Task(newTaskText, false)) }
                            newTaskText = ""
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = Win11Colors.SuccessGreen)
                    }
                }
            }
        }
    }
}

private data class Task(val text: String, val done: Boolean)