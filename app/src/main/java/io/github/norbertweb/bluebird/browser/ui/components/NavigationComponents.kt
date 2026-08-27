package com.io.github.norbertweb.bluebird.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.io.github.norbertweb.bluebird.browser.model.Bookmark
import com.io.github.norbertweb.bluebird.browser.model.BrowserTab

// ═══════════════════════════════════════════════════════════════════════
// IME-suppressing text field
// readOnly=true when built-in keyboard is active → system IME never opens
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BrowserTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    useBuiltInKeyboard: Boolean,
    onFocusRequest: () -> Unit,
    fontSize: TextUnit = 12.sp
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        readOnly      = useBuiltInKeyboard,
        modifier      = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            if (useBuiltInKeyboard) keyboardController?.hide()
            onFocusRequest()
        },
        textStyle     = TextStyle(color = textColor, fontSize = fontSize),
        singleLine    = true,
        cursorBrush   = SolidColor(Color(0xFF1A73E8)),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = textColor.copy(0.4f), fontSize = fontSize)
            inner()
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// NavBtn — icon button for the navigation bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NavBtn(
    icon: ImageVector,
    enabled: Boolean = true,
    tint: Color,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint     = if (enabled) tint else tint.copy(alpha = 0.25f),
            modifier = Modifier.size(15.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Navigation Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EdgeNavigationBar(
    isDark: Boolean,
    navBarBg: Color,
    borderColor: Color,
    isLoading: Boolean,
    loadingProgress: Float,
    canGoBack: Boolean,
    canGoForward: Boolean,
    addressText: String,
    addressBarFocused: Boolean,
    isBookmarked: Boolean,
    useBuiltInKb: Boolean,
    isPrivateTab: Boolean,
    activeUrl: String,
    zoomLevel: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onAddressChange: (String) -> Unit,
    onAddressFocus: () -> Unit,
    onAddressGo: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onMenuOpen: () -> Unit,
    onBookmarksPanel: () -> Unit,
    onHistoryPanel: () -> Unit,
    onDownloadsPanel: () -> Unit,
    onSettingsPanel: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopModeToggle: () -> Unit
) {
    val textColor  = if (isDark) Color(0xFFE8E8E8) else Color(0xFF202020)
    val iconColor  = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555)
    val addrBg     = if (isDark) Color(0xFF383838) else Color.White
    val accentBlue = Color(0xFF1A73E8)
    val isSecure   = activeUrl.startsWith("https://")
    val isHttp     = activeUrl.startsWith("http://") && !activeUrl.startsWith("https://")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(navBarBg)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NavBtn(Icons.Default.ArrowBack,    enabled = canGoBack,    tint = iconColor, onClick = onBack)
            NavBtn(Icons.Default.ArrowForward, enabled = canGoForward, tint = iconColor, onClick = onForward)
            NavBtn(
                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                tint = iconColor, onClick = onRefresh
            )
            NavBtn(Icons.Default.Home, tint = iconColor, onClick = onHome)

            Spacer(Modifier.width(4.dp))

            // ── Address Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isPrivateTab) Color(0xFF2D1F3D)
                        else addrBg
                    )
                    .border(
                        1.dp,
                        if (addressBarFocused) accentBlue
                        else if (isHttp) Color(0xFFD32F2F).copy(0.5f)
                        else borderColor,
                        RoundedCornerShape(15.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAddressFocus() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Security / private indicator
                    when {
                        isPrivateTab -> Icon(Icons.Default.Security, null,
                            tint = Color(0xFF9C6DCA), modifier = Modifier.size(11.dp))
                        isSecure     -> Icon(Icons.Default.Lock, null,
                            tint = Color(0xFF1A9A1A), modifier = Modifier.size(11.dp))
                        isHttp       -> Icon(Icons.Default.Warning, null,
                            tint = Color(0xFFD32F2F), modifier = Modifier.size(11.dp))
                        else         -> Icon(Icons.Default.Language, null,
                            tint = Color(0xFF888888), modifier = Modifier.size(11.dp))
                    }

                    BrowserTextField(
                        value              = addressText,
                        onValueChange      = onAddressChange,
                        placeholder        = if (isPrivateTab) "Private search or address" else "Search or enter address",
                        textColor          = if (isPrivateTab) Color(0xFFCCBBEE) else textColor,
                        modifier           = Modifier.weight(1f),
                        useBuiltInKeyboard = useBuiltInKb,
                        onFocusRequest     = onAddressFocus,
                        fontSize           = 12.sp
                    )

                    // Zoom indicator
                    if (zoomLevel != 100) {
                        Text("${zoomLevel}%", fontSize = 9.sp, color = accentBlue.copy(0.8f))
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(11.dp),
                            strokeWidth = 1.2.dp,
                            color       = accentBlue
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            NavBtn(Icons.Default.Article, tint = iconColor, onClick = onFindInPage)
            NavBtn(
                if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                tint    = if (isBookmarked) Color(0xFFFFD700) else iconColor,
                onClick = onBookmarkToggle
            )
            NavBtn(Icons.Default.BookmarkBorder, tint = iconColor, onClick = onBookmarksPanel)
            NavBtn(Icons.Default.History,        tint = iconColor, onClick = onHistoryPanel)
            NavBtn(Icons.Default.Download,       tint = iconColor, onClick = onDownloadsPanel)
            NavBtn(Icons.Default.Settings,       tint = iconColor, onClick = onSettingsPanel)
            NavBtn(Icons.Default.MoreVert,       tint = iconColor, onClick = onMenuOpen)
        }

        // ── Loading bar ──────────────────────────────────────────────
        if (isLoading) {
            LinearProgressIndicator(
                progress    = { loadingProgress },
                modifier    = Modifier.fillMaxWidth().height(2.dp),
                color       = if (isPrivateTab) Color(0xFF9C6DCA) else accentBlue,
                trackColor  = Color.Transparent
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Tab Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EdgeTabBar(
    tabs: List<BrowserTab>,
    activeTabId: String,
    isDark: Boolean,
    tabBarBg: Color,
    borderColor: Color,
    onTabSelected: (BrowserTab) -> Unit,
    onTabClosed: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onTabOverview: () -> Unit
) {
    val accentBlue = Color(0xFF1A73E8)
    val iconColor  = if (isDark) Color(0xFF999999) else Color(0xFF555555)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(tabBarBg)
            .border(BorderStroke(0.5.dp, borderColor), shape = androidx.compose.ui.graphics.RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scrollable tab list
        LazyRow(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs, key = { it.id }) { tab ->
                EdgeTabItem(
                    tab      = tab,
                    isActive = tab.id == activeTabId,
                    isDark   = isDark,
                    onSelect = { onTabSelected(tab) },
                    onClose  = { onTabClosed(tab) }
                )
            }
        }

        // Tab count / overview
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.5.dp, iconColor.copy(0.5f), RoundedCornerShape(4.dp))
                .clickable { onTabOverview() },
            contentAlignment = Alignment.Center
        ) {
            Text("${tabs.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444))
        }

        Spacer(Modifier.width(4.dp))

        // New normal tab
        NavBtn(Icons.Default.Add, tint = iconColor) { onNewTab() }

        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun EdgeTabItem(
    tab: BrowserTab,
    isActive: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val activeBg   = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
    val bg         = if (isActive) activeBg else Color.Transparent
    val textColor  = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF1A73E8)
    val privateColor = Color(0xFF9C6DCA)

    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .background(bg)
            .then(
                if (isActive) Modifier.drawBehind {
                    drawRect(
                        if (tab.isPrivate) privateColor else accentBlue,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                    )
                } else Modifier
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (tab.isPrivate) {
                Icon(Icons.Default.Security, null, tint = privateColor, modifier = Modifier.size(11.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(tab.faviconColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab.title.firstOrNull()?.uppercase() ?: "N",
                        fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                tab.title,
                color    = textColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (tab.isPinned) {
                Icon(Icons.Default.PushPin, null, tint = accentBlue, modifier = Modifier.size(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null,
                    tint = textColor.copy(0.4f), modifier = Modifier.size(9.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Bookmarks Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BookmarksBar(
    bookmarks: List<Bookmark>,
    isDark: Boolean,
    navBarBg: Color,
    borderColor: Color,
    onBookmarkClick: (Bookmark) -> Unit
) {
    val textColor = if (isDark) Color(0xFFCCCCCC) else Color(0xFF333333)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(navBarBg)
            .border(BorderStroke(0.5.dp, borderColor), shape = androidx.compose.ui.graphics.RectangleShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        bookmarks.filter { it.folder == "Bookmarks Bar" }.take(14).forEach { bm ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onBookmarkClick(bm) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(11.dp).background(Color(bm.faviconColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(bm.title.firstOrNull()?.uppercase() ?: "?", fontSize = 6.sp, color = Color.White)
                }
                Text(bm.title, fontSize = 10.sp, color = textColor, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Find in Page Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun FindInPageBar(
    query: String,
    activeMatch: Int,
    totalMatches: Int,
    isDark: Boolean,
    useBuiltInKb: Boolean,
    onQueryChange: (String) -> Unit,
    onFocused: () -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onClose: () -> Unit
) {
    val bg         = if (isDark) Color(0xFF2A2A2A) else Color(0xFFEBEBEB)
    val txtColor   = if (isDark) Color.White       else Color.Black
    val accentBlue = Color(0xFF1A73E8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))

        BrowserTextField(
            value              = query,
            onValueChange      = onQueryChange,
            placeholder        = "Find in page…",
            textColor          = txtColor,
            modifier           = Modifier.weight(1f),
            useBuiltInKeyboard = useBuiltInKb,
            onFocusRequest     = onFocused,
            fontSize           = 12.sp
        )

        // Match counter
        if (totalMatches > 0) {
            Text(
                "${activeMatch + 1}/$totalMatches",
                fontSize = 10.sp,
                color = txtColor.copy(0.6f)
            )
        } else if (query.isNotEmpty()) {
            Text("No results", fontSize = 10.sp, color = Color(0xFFD32F2F).copy(0.8f))
        }

        NavBtn(Icons.Default.KeyboardArrowUp,   enabled = totalMatches > 0, tint = Color(0xFF888888)) { onFindPrev() }
        NavBtn(Icons.Default.KeyboardArrowDown, enabled = totalMatches > 0, tint = Color(0xFF888888)) { onFindNext() }
        NavBtn(Icons.Default.Close,             enabled = true,              tint = Color(0xFF888888)) { onClose() }
    }
}
