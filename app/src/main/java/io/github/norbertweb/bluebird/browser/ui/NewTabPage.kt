package com.io.github.norbertweb.bluebird.browser.ui.newtab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.io.github.norbertweb.bluebird.browser.model.Bookmark
import com.io.github.norbertweb.bluebird.browser.model.BrowserSettings
import com.io.github.norbertweb.bluebird.browser.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.grid.items as gridItems

// ═══════════════════════════════════════════════════════════════════════
// Quick links — default tiles shown on new tab
// ═══════════════════════════════════════════════════════════════════════

private data class QuickLink(val label: String, val url: String, val color: Long)

private val DEFAULT_QUICK_LINKS = listOf(
    QuickLink("Google",    "https://google.com",     0xFF4285F4),
    QuickLink("YouTube",   "https://youtube.com",    0xFFFF0000),
    QuickLink("Gmail",     "https://gmail.com",      0xFFEA4335),
    QuickLink("Maps",      "https://maps.google.com",0xFF34A853),
    QuickLink("Wikipedia", "https://wikipedia.org",  0xFF888888),
    QuickLink("Reddit",    "https://reddit.com",     0xFFFF4500),
    QuickLink("X",         "https://x.com",          0xFF000000),
    QuickLink("GitHub",    "https://github.com",     0xFF333333),
)

// ═══════════════════════════════════════════════════════════════════════
// NewTabPage
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NewTabPage(
    isDark: Boolean,
    isPrivate: Boolean,
    settings: BrowserSettings,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit
) {
    val bg = when {
        isPrivate -> if (isDark) Color(0xFF12091E) else Color(0xFF1A0A2E)
        isDark    -> Color(0xFF1A1A1A)
        else      -> Color(0xFFF6F7F9)
    }
    val textColor     = if (isDark || isPrivate) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val subColor      = if (isDark || isPrivate) Color(0xFF888888) else Color(0xFF666666)
    val cardBg        = if (isDark || isPrivate) Color(0xFF252530) else Color.White
    val accentBlue    = Color(0xFF1A73E8)
    val privateViolet = Color(0xFF9C6DCA)
    val accent        = if (isPrivate) privateViolet else accentBlue

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        // Radial glow for private mode
        if (isPrivate) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF9C6DCA).copy(0.15f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ClockGreeting(isPrivate, textColor, subColor, accent)

            if (isPrivate) {
                PrivateBanner(privateViolet)
            }

            if (!isPrivate) {
                QuickLinksSection(cardBg, textColor, subColor, onNavigate)
            }

            if (bookmarks.isNotEmpty() && !isPrivate) {
                RecentSection(
                    title     = "Bookmarks",
                    icon      = Icons.Default.BookmarkBorder,
                    accent    = accent,
                    textColor = textColor,
                    subColor  = subColor
                ) {
                    bookmarks.take(6).forEach { bm ->
                        SiteChip(
                            title     = bm.title,
                            url       = bm.url,
                            color     = Color(bm.faviconColor),
                            textColor = textColor,
                            cardBg    = cardBg,
                            onClick   = { onNavigate(bm.url) }
                        )
                    }
                }
            }

            if (history.isNotEmpty() && !isPrivate) {
                RecentSection(
                    title     = "Recently visited",
                    icon      = Icons.Default.History,
                    accent    = accent,
                    textColor = textColor,
                    subColor  = subColor
                ) {
                    history.take(8).forEach { entry ->
                        SiteChip(
                            title     = entry.title,
                            url       = entry.url,
                            color     = Color(entry.faviconColor),
                            textColor = textColor,
                            cardBg    = cardBg,
                            onClick   = { onNavigate(entry.url) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Clock & greeting ─────────────────────────────────────────────────

@Composable
private fun ClockGreeting(
    isPrivate: Boolean,
    textColor: Color,
    subColor: Color,
    accent: Color
) {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            timeStr = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now))
            dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
            kotlinx.coroutines.delay(10_000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isPrivate) {
            Icon(Icons.Default.Security, null, tint = accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Private Browsing", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = accent)
            Text(
                "Your activity is not saved to this device",
                fontSize  = 12.sp,
                color     = subColor,
                textAlign = TextAlign.Center
            )
        } else {
            Text(timeStr, fontSize = 48.sp, fontWeight = FontWeight.Light, color = textColor, letterSpacing = 2.sp)
            Text(dateStr, fontSize = 13.sp, color = subColor)
        }
    }
}

// ─── Private banner ───────────────────────────────────────────────────

@Composable
private fun PrivateBanner(violet: Color) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = violet.copy(0.1f)),
        border = BorderStroke(1.dp, violet.copy(0.2f))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.VisibilityOff, null, tint = violet, modifier = Modifier.size(20.dp))
            Column {
                Text("Private browsing active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = violet)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Pages you view will not be saved in your history. " +
                            "Cookies and site data are cleared when you close this tab.",
                    fontSize   = 11.sp,
                    color      = violet.copy(0.75f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// ─── Quick links ──────────────────────────────────────────────────────

@Composable
private fun QuickLinksSection(
    cardBg: Color,
    textColor: Color,
    subColor: Color,
    onNavigate: (String) -> Unit
) {
    Column {
        Text(
            "Quick links",
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = subColor,
            modifier   = Modifier.padding(bottom = 10.dp)
        )
        LazyVerticalGrid(
            columns               = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.fillMaxWidth().heightIn(max = 220.dp)
        ) {
            gridItems(DEFAULT_QUICK_LINKS) { link ->
                // Capture all properties up front to avoid data-class scope issues in lambda
                val linkLabel = link.label
                val linkUrl   = link.url
                val linkColor = Color(link.color)
                val initial   = linkLabel.firstOrNull()?.toString()?.uppercase() ?: "?"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .clickable { onNavigate(linkUrl) }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(linkColor.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = initial,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = linkColor
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text      = linkLabel,
                        fontSize  = 9.sp,
                        color     = textColor,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─── Recent section wrapper ───────────────────────────────────────────

@Composable
private fun RecentSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    textColor: Color,
    subColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier              = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subColor)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }
}

// ─── Site chip ────────────────────────────────────────────────────────

@Composable
private fun SiteChip(
    title: String,
    url: String,
    color: Color,
    textColor: Color,
    cardBg: Color,
    onClick: () -> Unit
) {
    val initial = title.firstOrNull()?.toString()?.uppercase() ?: "?"
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = initial,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text      = title,
            fontSize  = 9.sp,
            color     = textColor,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
