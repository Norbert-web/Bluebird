package io.github.norbertweb.bluebird.browser.ui.newtab

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.browser.data.HomeContentRepository
import io.github.norbertweb.bluebird.browser.model.Bookmark
import io.github.norbertweb.bluebird.browser.model.BrowserSettings
import io.github.norbertweb.bluebird.browser.model.HistoryEntry
import io.github.norbertweb.bluebird.browser.model.NewsArticle
import io.github.norbertweb.bluebird.browser.model.NewsSubscription
import io.github.norbertweb.bluebird.browser.ui.components.FluentIcon
import io.github.norbertweb.bluebird.browser.ui.components.FluentIcons
import io.github.norbertweb.bluebird.ui.components.LocalWindowRuntime
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.grid.items as gridItems

private data class QuickLink(val label: String, val url: String, val color: Long)

private val DEFAULT_QUICK_LINKS = listOf(
    QuickLink("Google", "https://google.com", 0xFF4285F4),
    QuickLink("YouTube", "https://youtube.com", 0xFFFF0000),
    QuickLink("Gmail", "https://gmail.com", 0xFFEA4335),
    QuickLink("Maps", "https://maps.google.com", 0xFF34A853),
    QuickLink("Wikipedia", "https://wikipedia.org", 0xFF888888),
    QuickLink("Reddit", "https://reddit.com", 0xFFFF4500),
    QuickLink("X", "https://x.com", 0xFF000000),
    QuickLink("GitHub", "https://github.com", 0xFF333333),
)

@Composable
fun NewTabPage(
    isDark: Boolean,
    isPrivate: Boolean,
    settings: BrowserSettings,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onSettingsChange: (BrowserSettings) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val homeRepo = remember { HomeContentRepository.get(context) }
    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    var news by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var showCustomize by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Home content is intentionally lazy and throttled. Opening a new tab does
    // not create a continuous network job; wallpaper is one download per 5h
    // slot and RSS refreshes at most once every 30 minutes.
    LaunchedEffect(isPrivate, settings.homeShowWallpaper, settings.homeShowNews) {
        if (isPrivate) return@LaunchedEffect
        wallpaper = if (settings.homeShowWallpaper) homeRepo.loadCurrentWallpaper().first else null
        news = if (settings.homeShowNews) homeRepo.loadNews() else emptyList()
    }

    val bg = when {
        isPrivate -> if (isDark) Color(0xFF12091E) else Color(0xFF1A0A2E)
        isDark -> Color(0xFF171717)
        else -> Color(0xFFF4F6F8)
    }
    val textColor = if (isDark || isPrivate) Color(0xFFF1F1F1) else Color(0xFF171717)
    val subColor = if (isDark || isPrivate) Color(0xFFB5B5B5) else Color(0xFF5F6368)
    val cardBg = if (isDark || isPrivate) Color(0xE6232328) else Color(0xEBFFFFFF)
    val accentBlue = Color(0xFF4F8CFF)
    val privateViolet = Color(0xFFB784E7)
    val accent = if (isPrivate) privateViolet else accentBlue

    Box(Modifier.fillMaxSize().background(bg)) {
        if (!isPrivate && settings.homeShowWallpaper && wallpaper != null) {
            Image(
                bitmap = wallpaper!!.asImageBitmap(),
                contentDescription = "Bluebird home wallpaper",
                modifier = Modifier.fillMaxSize().alpha(if (isDark) 0.48f else 0.64f),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            if (isDark) Color.Black.copy(0.56f) else Color.White.copy(0.18f),
                            if (isDark) Color.Black.copy(0.82f) else Color.White.copy(0.88f)
                        )
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ClockGreeting(isPrivate, textColor, subColor, accent)

            if (!isPrivate) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { showCustomize = true }, modifier = Modifier.size(34.dp)) {
                        FluentIcon(FluentIcons.Settings, "Customize New Tab", tint = subColor, modifier = Modifier.size(17.dp))
                    }
                }
            }

            if (isPrivate) {
                PrivateBanner(privateViolet)
            } else {
                if (settings.homeShowQuickLinks) {
                    QuickLinksSection(cardBg, textColor, subColor, onNavigate)
                }

                val recentSites = recentSites(history)
                if (settings.homeShowRecentSites && recentSites.isNotEmpty()) {
                    RecentSection("Recent sites", FluentIcons.History, accent, textColor, subColor) {
                        recentSites.take(8).forEach { entry ->
                            SiteChip(entry.title, entry.url, Color(entry.faviconColor), textColor, cardBg) { onNavigate(entry.url) }
                        }
                    }
                }

                if (settings.homeShowBookmarks && bookmarks.isNotEmpty()) {
                    RecentSection("Bookmarks", FluentIcons.BookmarkBorder, accent, textColor, subColor) {
                        bookmarks.take(8).forEach { bm ->
                            SiteChip(bm.title, bm.url, Color(bm.faviconColor), textColor, cardBg) { onNavigate(bm.url) }
                        }
                    }
                }

                if (settings.homeShowNews) {
                    NewsSection(news, cardBg, textColor, subColor, accent, onNavigate)
                }

                if (settings.homeShowWallpaper && wallpaper != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Wallpaper · Unsplash · changes every 5 hours",
                            fontSize = 9.sp,
                            color = subColor.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showCustomize) {
        CustomizeHomeDialog(
            isDark = isDark,
            settings = settings,
            subscriptions = homeRepo.loadSubscriptions(),
            onSettingsChange = { updated ->
                onSettingsChange(updated)
                if (!updated.homeShowNews) news = emptyList()
                if (!updated.homeShowWallpaper) wallpaper = null
            },
            onToggleSubscription = { id, enabled ->
                val updated = homeRepo.loadSubscriptions().map { if (it.id == id) it.copy(enabled = enabled) else it }
                homeRepo.saveSubscriptions(updated)
                coroutineScope.launch {
                    news = if (settings.homeShowNews) homeRepo.loadNews(force = true) else emptyList()
                }
            },
            onDismiss = { showCustomize = false }
        )
    }
}

@Composable
private fun CustomizeHomeDialog(
    isDark: Boolean,
    settings: BrowserSettings,
    subscriptions: List<NewsSubscription>,
    onSettingsChange: (BrowserSettings) -> Unit,
    onToggleSubscription: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF1F1F1) else Color(0xFF202020)
    val subColor = if (isDark) Color(0xFFB5B5B5) else Color(0xFF5F6368)
    val surface = if (isDark) Color(0xFF252525) else Color.White
    val accent = Color(0xFF4F8CFF)
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = surface,
        title = { Text("Customize New Tab", color = textColor, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeToggle("Quick links", "Favorite shortcuts", settings.homeShowQuickLinks, textColor, subColor, accent) { onSettingsChange(settings.copy(homeShowQuickLinks = it)) }
                HomeToggle("Recent sites", "Recently visited websites", settings.homeShowRecentSites, textColor, subColor, accent) { onSettingsChange(settings.copy(homeShowRecentSites = it)) }
                HomeToggle("Bookmarks", "Saved bookmarks", settings.homeShowBookmarks, textColor, subColor, accent) { onSettingsChange(settings.copy(homeShowBookmarks = it)) }
                HomeToggle("Subscribed news", "Cached RSS articles", settings.homeShowNews, textColor, subColor, accent) { onSettingsChange(settings.copy(homeShowNews = it)) }
                HomeToggle("Wallpaper", "Cached image; changes every 5 hours", settings.homeShowWallpaper, textColor, subColor, accent) { onSettingsChange(settings.copy(homeShowWallpaper = it)) }
                Spacer(Modifier.height(8.dp))
                Divider(color = textColor.copy(alpha = 0.12f))
                Spacer(Modifier.height(8.dp))
                Text("News subscriptions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Text("Feeds refresh only when their cache expires.", fontSize = 9.sp, color = subColor)
                subscriptions.forEach { sub ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sub.name, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
                            Text(sub.feedUrl, fontSize = 8.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = sub.enabled, onCheckedChange = { onToggleSubscription(sub.id, it) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun HomeToggle(title: String, subtitle: String, checked: Boolean, textColor: Color, subColor: Color, accent: Color, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 9.sp, color = subColor)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private fun recentSites(history: List<HistoryEntry>): List<HistoryEntry> {
    val seen = HashSet<String>()
    return history.asSequence()
        .filter { it.url.isNotBlank() && !it.url.startsWith("io.github.norbertweb.bluebird://") }
        .sortedByDescending { it.visitedAt }
        .filter { entry ->
            val key = runCatching { URI(entry.url).host?.lowercase(Locale.US) ?: entry.url }.getOrDefault(entry.url)
            seen.add(key)
        }
        .take(12)
        .toList()
}

@Composable
private fun ClockGreeting(isPrivate: Boolean, textColor: Color, subColor: Color, accent: Color) {
    val windowRuntime = LocalWindowRuntime.current
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(windowRuntime.isMinimized) {
        while (isActive) {
            if (windowRuntime.isMinimized) {
                delay(30_000)
                continue
            }
            val now = System.currentTimeMillis()
            timeStr = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now))
            dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
            delay(10_000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isPrivate) {
            FluentIcon(FluentIcons.Security, null, tint = accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Private Browsing", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = accent)
            Text("Your activity is not saved to this device", fontSize = 12.sp, color = subColor, textAlign = TextAlign.Center)
        } else {
            Text(timeStr, fontSize = 48.sp, fontWeight = FontWeight.Light, color = textColor, letterSpacing = 2.sp)
            Text(dateStr, fontSize = 13.sp, color = subColor)
        }
    }
}

@Composable
private fun PrivateBanner(violet: Color) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = violet.copy(0.1f)), border = BorderStroke(1.dp, violet.copy(0.2f))) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FluentIcon(FluentIcons.VisibilityOff, null, tint = violet, modifier = Modifier.size(20.dp))
            Column {
                Text("Private browsing active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = violet)
                Spacer(Modifier.height(3.dp))
                Text("Pages you view will not be saved in your history. Cookies and site data are cleared when you close this tab.", fontSize = 11.sp, color = violet.copy(0.75f), lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun QuickLinksSection(cardBg: Color, textColor: Color, subColor: Color, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Quick links", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subColor, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
        ) {
            gridItems(DEFAULT_QUICK_LINKS) { link ->
                val initial = link.label.firstOrNull()?.uppercase() ?: "?"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(cardBg).clickable { onNavigate(link.url) }.padding(vertical = 10.dp, horizontal = 5.dp)
                ) {
                    Box(Modifier.size(34.dp).background(Color(link.color).copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(initial, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(link.color))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(link.label, fontSize = 9.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RecentSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, textColor: Color, subColor: Color, content: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FluentIcon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subColor)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
        }
    }
}

@Composable
private fun SiteChip(title: String, url: String, color: Color, textColor: Color, cardBg: Color, onClick: () -> Unit) {
    val label = title.ifBlank { runCatching { URI(url).host }.getOrNull() ?: "Site" }
    val initial = label.firstOrNull()?.uppercase() ?: "?"
    Column(
        modifier = Modifier.width(78.dp).clip(RoundedCornerShape(12.dp)).background(cardBg).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(36.dp).background(color.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Text(initial, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 9.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NewsSection(items: List<NewsArticle>, cardBg: Color, textColor: Color, subColor: Color, accent: Color, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FluentIcon(FluentIcons.Article, null, tint = accent, modifier = Modifier.size(14.dp))
            Text("Subscribed news", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subColor)
            if (items.isNotEmpty()) {
                Text("· ${items.size}", fontSize = 10.sp, color = subColor.copy(0.7f))
            }
        }
        if (items.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FluentIcon(FluentIcons.Article, null, tint = subColor, modifier = Modifier.size(18.dp))
                    Text("News will appear here when your subscribed feeds are available.", fontSize = 11.sp, color = subColor, lineHeight = 15.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(6).forEach { article ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(article.url) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(article.source.uppercase(Locale.getDefault()), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(article.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                            if (article.summary.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(article.summary, fontSize = 10.sp, color = subColor, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
