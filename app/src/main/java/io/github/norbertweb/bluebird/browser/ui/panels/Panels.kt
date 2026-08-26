package com.win11launcher.browser.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.win11launcher.browser.model.Bookmark
import com.win11launcher.browser.model.BrowserPanel
import com.win11launcher.browser.model.BrowserSettings
import com.win11launcher.browser.model.BrowserTab
import com.win11launcher.browser.model.DownloadItem
import com.win11launcher.browser.model.DownloadStatus
import com.win11launcher.browser.model.HistoryEntry
import com.win11launcher.browser.model.SearchEngine
import com.win11launcher.browser.model.StartPage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.grid.items as gridItems

// ═══════════════════════════════════════════════════════════════════════
// SidePanel — container that routes to the right panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SidePanel(
    panel: BrowserPanel,
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    borderColor: Color,
    bookmarks: MutableList<Bookmark>,
    history: MutableList<HistoryEntry>,
    downloads: MutableList<DownloadItem>,
    settings: BrowserSettings,
    onSettingsChange: (BrowserSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val accentBlue = Color(0xFF1A73E8)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(surfaceColor)
            .border(
                BorderStroke(0.5.dp, borderColor),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Panel header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    when (panel) {
                        BrowserPanel.BOOKMARKS   -> "Bookmarks"
                        BrowserPanel.HISTORY     -> "History"
                        BrowserPanel.DOWNLOADS   -> "Downloads"
                        BrowserPanel.SETTINGS    -> "Settings"
                        BrowserPanel.EXTENSIONS  -> "Extensions"
                        BrowserPanel.COLLECTIONS -> "Collections"
                        else                     -> ""
                    },
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = textColor
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = textColor.copy(0.5f), modifier = Modifier.size(14.dp))
                }
            }

            Divider(color = borderColor, thickness = 0.5.dp)

            when (panel) {
                BrowserPanel.BOOKMARKS  -> BookmarksPanel(bookmarks, isDark, textColor, accentBlue, onNavigate)
                BrowserPanel.HISTORY    -> HistoryPanel(history, isDark, textColor, accentBlue, onNavigate) { history.clear() }
                BrowserPanel.DOWNLOADS  -> DownloadsPanel(downloads, isDark, textColor, accentBlue)
                BrowserPanel.SETTINGS   -> SettingsPanel(settings, isDark, textColor, accentBlue, onSettingsChange)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Coming soon", color = textColor.copy(0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Bookmarks Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BookmarksPanel(
    bookmarks: MutableList<Bookmark>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onNavigate: (String) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyState(Icons.Default.BookmarkBorder, "No bookmarks yet", textColor)
        return
    }

    // Group by folder
    val grouped = bookmarks.groupBy { it.folder }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grouped.forEach { (folder, items) ->
            if (folder != "Bookmarks Bar") {
                item {
                    Text(
                        folder,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBlue,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            items(items) { bm ->
                BookmarkRow(
                    bm        = bm,
                    textColor = textColor,
                    onClick   = { onNavigate(bm.url) },
                    onDelete  = { bookmarks.remove(bm) }
                )
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bm: Bookmark,
    textColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(Color(bm.faviconColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = bm.title.firstOrNull()?.toString()?.uppercase() ?: "?",
                fontSize = 10.sp,
                color    = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(bm.title, fontSize = 11.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bm.url, fontSize = 9.sp, color = textColor.copy(0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, null, tint = textColor.copy(0.3f), modifier = Modifier.size(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// History Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun HistoryPanel(
    history: MutableList<HistoryEntry>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onNavigate: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${history.size} entries", fontSize = 10.sp, color = textColor.copy(0.4f))
                TextButton(onClick = onClear, contentPadding = PaddingValues(4.dp)) {
                    Text("Clear all", fontSize = 10.sp, color = Color(0xFFD32F2F))
                }
            }
        }

        if (history.isEmpty()) {
            EmptyState(Icons.Default.History, "No history", textColor)
        } else {
            val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(history, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigate(entry.url) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(18.dp).background(Color(entry.faviconColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text     = entry.title.firstOrNull()?.toString()?.uppercase() ?: "?",
                                fontSize = 8.sp,
                                color    = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, fontSize = 10.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sdf.format(Date(entry.visitedAt)), fontSize = 8.sp, color = textColor.copy(0.4f))
                        }
                        IconButton(onClick = { /* remove single entry */ }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, null, tint = textColor.copy(0.2f), modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Downloads Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DownloadsPanel(
    downloads: MutableList<DownloadItem>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color
) {
    if (downloads.isEmpty()) {
        EmptyState(Icons.Default.Download, "No downloads", textColor)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(downloads, key = { it.id }) { dl ->
            val cardBg = if (isDark) Color(0xFF333333) else Color(0xFFF5F5F5)
            Card(
                shape  = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            when (dl.status) {
                                DownloadStatus.COMPLETED  -> Icons.Default.CheckCircle
                                DownloadStatus.FAILED     -> Icons.Default.Error
                                DownloadStatus.PAUSED     -> Icons.Default.Pause
                                DownloadStatus.DOWNLOADING-> Icons.Default.Downloading
                            },
                            null,
                            tint = when (dl.status) {
                                DownloadStatus.COMPLETED  -> Color(0xFF107C10)
                                DownloadStatus.FAILED     -> Color(0xFFD13438)
                                else                      -> accentBlue
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            dl.fileName,
                            fontSize = 11.sp,
                            color    = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (dl.status == DownloadStatus.DOWNLOADING) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress    = { dl.progress },
                            modifier    = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color       = accentBlue,
                            trackColor  = if (isDark) Color(0xFF555555) else Color(0xFFDDDDDD)
                        )
                    }

                    Spacer(Modifier.height(3.dp))
                    val sizeLabel = if (dl.fileSize > 0)
                        "${dl.fileSize / 1024 / 1024} MB · "
                    else ""
                    Text(
                        "$sizeLabel${dl.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 9.sp,
                        color    = textColor.copy(0.5f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Settings Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SettingsPanel(
    settings: BrowserSettings,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onChange: (BrowserSettings) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

        item { SectionHeader("General", accentBlue) }
        item { ToggleRow("Show bookmarks bar",    null, settings.showBookmarksBar, textColor, accentBlue) { onChange(settings.copy(showBookmarksBar = it)) } }
        item { ToggleRow("Desktop mode",          "Request full io.github.norbertweb.io.github.norbertweb.bluebird websites", settings.desktopMode, textColor, accentBlue) { onChange(settings.copy(desktopMode = it)) } }
        item { ToggleRow("JavaScript",            null, settings.javaScriptEnabled, textColor, accentBlue) { onChange(settings.copy(javaScriptEnabled = it)) } }
        item { ToggleRow("Load images",           null, settings.showImages, textColor, accentBlue) { onChange(settings.copy(showImages = it)) } }
        item { ToggleRow("Save cookies",          null, settings.saveCookies, textColor, accentBlue) { onChange(settings.copy(saveCookies = it)) } }
        item { ToggleRow("Save form data",        null, settings.saveFormData, textColor, accentBlue) { onChange(settings.copy(saveFormData = it)) } }
        item { ToggleRow("Allow mixed content",   "Load HTTP on HTTPS pages (insecure)", settings.mixedContentAllowed, textColor, accentBlue) { onChange(settings.copy(mixedContentAllowed = it)) } }

        item { SectionHeader("Privacy & Security", accentBlue) }
        item { ToggleRow("Ad blocker",           "Block intrusive advertisements", settings.adBlockEnabled, textColor, accentBlue) { onChange(settings.copy(adBlockEnabled = it)) } }
        item { ToggleRow("Tracking protection",  "Block cross-site trackers", settings.trackingProtection, textColor, accentBlue) { onChange(settings.copy(trackingProtection = it)) } }
        item { ToggleRow("Block pop-ups",        null, settings.popupBlocker, textColor, accentBlue) { onChange(settings.copy(popupBlocker = it)) } }

        item { SectionHeader("Search Engine", accentBlue) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                SearchEngine.values().forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onChange(settings.copy(searchEngine = engine)) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.searchEngine == engine,
                            onClick  = { onChange(settings.copy(searchEngine = engine)) },
                            modifier = Modifier.size(16.dp),
                            colors   = RadioButtonDefaults.colors(selectedColor = accentBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, fontSize = 12.sp, color = textColor)
                    }
                }
            }
        }

        item { SectionHeader("Keyboard", accentBlue) }
        item { ToggleRow("Built-in floating keyboard", "Suppresses Android keyboard", settings.useBuiltInKeyboard, textColor, accentBlue) { onChange(settings.copy(useBuiltInKeyboard = it)) } }

        item { SectionHeader("Permissions", accentBlue) }
        item { ToggleRow("Location access",   "Allow sites to request location",   settings.locationAccess,   textColor, accentBlue) { onChange(settings.copy(locationAccess = it)) } }
        item { ToggleRow("Camera access",     "Allow sites to request camera",     settings.cameraAccess,     textColor, accentBlue) { onChange(settings.copy(cameraAccess = it)) } }
        item { ToggleRow("Microphone access", "Allow sites to request microphone", settings.microphoneAccess, textColor, accentBlue) { onChange(settings.copy(microphoneAccess = it)) } }

        item { SectionHeader("Start Page", accentBlue) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                StartPage.values().forEach { sp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onChange(settings.copy(startPage = sp)) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.startPage == sp,
                            onClick  = { onChange(settings.copy(startPage = sp)) },
                            modifier = Modifier.size(16.dp),
                            colors   = RadioButtonDefaults.colors(selectedColor = accentBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (sp) {
                                StartPage.NEW_TAB                -> "New tab page"
                                StartPage.BLANK                  -> "Blank page"
                                StartPage.CONTINUE_WHERE_LEFT_OFF-> "Continue where left off"
                            },
                            fontSize = 12.sp,
                            color    = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentBlue: Color) {
    Text(
        title,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Bold,
        color      = accentBlue,
        modifier   = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 3.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    textColor: Color,
    accentBlue: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 9.sp, color = textColor.copy(0.45f), lineHeight = 12.sp)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            modifier        = Modifier.scale(0.65f),
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = accentBlue,
                uncheckedTrackColor = Color(0xFF888888)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Context Menu (three-dot)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EdgeContextMenu(
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    isBookmarked: Boolean,
    isPrivateTab: Boolean,
    currentZoom: Int,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onFindInPage: () -> Unit,
    onPrint: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onAddBookmark: () -> Unit,
    onShare: () -> Unit,
    onClearData: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.3f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            modifier  = Modifier.width(220.dp).padding(top = 80.dp, end = 8.dp)
                .clickable(enabled = false) {},
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MenuRow(Icons.Default.Add,       "New tab",          textColor) { onNewTab(); onDismiss() }
                MenuRow(Icons.Default.Security,  "New private tab",  textColor) { onNewPrivateTab(); onDismiss() }

                MenuDivider(isDark)

                MenuRow(
                    if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    if (isBookmarked) "Bookmarked ✓" else "Add bookmark",
                    textColor
                ) { onAddBookmark(); onDismiss() }
                MenuRow(Icons.Default.Share,       "Share page",     textColor) { onShare(); onDismiss() }
                MenuRow(Icons.Default.FindInPage,  "Find in page",   textColor) { onFindInPage(); onDismiss() }
                MenuRow(Icons.Default.Print,       "Print…",         textColor) { onPrint(); onDismiss() }

                MenuDivider(isDark)

                // Zoom row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ZoomIn, null, tint = textColor.copy(0.6f), modifier = Modifier.size(14.dp))
                    Text("Zoom", fontSize = 12.sp, color = textColor, modifier = Modifier.padding(start = 8.dp))
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Remove, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "$currentZoom%",
                        fontSize = 11.sp,
                        color = textColor.copy(0.8f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onZoomReset() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Add, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                }

                MenuDivider(isDark)

                MenuRow(Icons.Default.BookmarkBorder, "Bookmarks", textColor) { onBookmarks(); onDismiss() }
                MenuRow(Icons.Default.History,        "History",   textColor) { onHistory(); onDismiss() }
                MenuRow(Icons.Default.Download,       "Downloads", textColor) { onDownloads(); onDismiss() }
                MenuRow(Icons.Default.Delete,         "Clear browsing data", textColor) { onClearData(); onDismiss() }

                MenuDivider(isDark)

                MenuRow(Icons.Default.Settings, "Settings", textColor) { onSettings(); onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = textColor.copy(0.65f), modifier = Modifier.size(15.dp))
        Text(label, fontSize = 12.sp, color = textColor)
    }
}

@Composable
private fun MenuDivider(isDark: Boolean) {
    Divider(
        color     = if (isDark) Color(0x20FFFFFF) else Color(0x20000000),
        thickness = 0.5.dp,
        modifier  = Modifier.padding(vertical = 3.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Tab Overview Grid
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun TabOverviewGrid(
    tabs: List<BrowserTab>,
    activeTabId: String,
    isDark: Boolean,
    onSelectTab: (BrowserTab) -> Unit,
    onCloseTab: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit
) {
    val bg        = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val cardBg    = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF1A73E8)
    val privateColor = Color(0xFF9C6DCA)

    // Separate normal and private tabs
    val normalTabs  = tabs.filter { !it.isPrivate }
    val privateTabs = tabs.filter { it.isPrivate }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${tabs.size} tabs", fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onNewPrivateTab) {
                        Icon(Icons.Default.Security, null, tint = privateColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Private", fontSize = 11.sp, color = privateColor)
                    }
                    TextButton(onClick = onNewTab) {
                        Icon(Icons.Default.Add, null, tint = accentBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New tab", fontSize = 11.sp, color = accentBlue)
                    }
                }
            }

            LazyVerticalGrid(
                columns               = GridCells.Adaptive(140.dp),
                contentPadding        = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.weight(1f)
            ) {
                gridItems(tabs, key = { tab -> tab.id }) { tab ->
                    // Capture all tab properties before entering nested composables
                    val tabId         = tab.id
                    val tabIsPrivate  = tab.isPrivate
                    val tabTitle      = tab.title
                    val tabFavColor   = tab.faviconColor
                    val isActive      = tabId == activeTabId
                    val accent        = if (tabIsPrivate) privateColor else accentBlue

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable { onSelectTab(tab) }
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = if (isActive) accent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape     = RoundedCornerShape(8.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = if (tabIsPrivate) Color(0xFF1E1430) else cardBg
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        if (tabIsPrivate) Color(0xFF2D1F3D)
                                        else Color(tabFavColor).copy(0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tabIsPrivate) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint     = privateColor.copy(0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint     = Color(tabFavColor).copy(0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (tabIsPrivate) privateColor else Color(tabFavColor),
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text     = tabTitle,
                                    fontSize = 9.sp,
                                    color    = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseTab(tab) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint     = textColor.copy(0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Address Suggestions
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AddressSuggestionsDropdown(
    query: String,
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    isDark: Boolean,
    onSuggestionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bg        = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color.White       else Color.Black
    val accentBlue = Color(0xFF1A73E8)

    val historyMatches  = history.filter {
        it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
    }.take(5)
    val bookmarkMatches = bookmarks.filter {
        it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
    }.take(3)

    if (historyMatches.isEmpty() && bookmarkMatches.isEmpty()) return

    Box(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                if (bookmarkMatches.isNotEmpty()) {
                    Text("Bookmarks", fontSize = 9.sp, color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp))
                }
                bookmarkMatches.forEach { bm ->
                    SuggestionRow(
                        icon     = Icons.Default.BookmarkBorder,
                        title    = bm.title,
                        url      = bm.url,
                        color    = accentBlue,
                        textColor = textColor,
                        onClick  = { onSuggestionClick(bm.url) }
                    )
                }

                if (historyMatches.isNotEmpty()) {
                    Text("History", fontSize = 9.sp, color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp))
                }
                historyMatches.forEach { entry ->
                    SuggestionRow(
                        icon     = Icons.Default.History,
                        title    = entry.title,
                        url      = entry.url,
                        color    = textColor.copy(0.4f),
                        textColor = textColor,
                        onClick  = { onSuggestionClick(entry.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    url: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url,   fontSize = 9.sp,  color = textColor.copy(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Empty state helper
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(icon: ImageVector, label: String, textColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = textColor.copy(0.25f), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, color = textColor.copy(0.4f), fontSize = 12.sp)
        }
    }
}


