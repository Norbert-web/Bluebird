package io.github.norbertweb.bluebird.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.browser.model.Bookmark
import io.github.norbertweb.bluebird.browser.model.BrowserTab

// ═══════════════════════════════════════════════════════════════════════
// Desktop-style controlled text field
// Uses TextFieldValue so native pointer-based caret/selection positioning is preserved.
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BrowserTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onFocusRequest: () -> Unit,
    fontSize: TextUnit = 13.sp,
    onImeGo: (() -> Unit)? = null,
    selectAllOnFocus: Boolean = false,
    focusRequestToken: Int = 0
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value))
    }
    var wasFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Keep the internal selection/cursor in sync when browser navigation
    // changes the address from outside the text field. Do not overwrite
    // the local value on every keystroke, or cursor position would jump.
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length)
            )
        }
    }

    LaunchedEffect(focusRequestToken) {
        if (focusRequestToken > 0) {
            focusRequester.requestFocus()
            if (fieldValue.text.isNotEmpty()) {
                fieldValue = fieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(0, fieldValue.text.length)
                )
            }
        }
    }

    val resolvedLineHeight = when {
        fontSize.value <= 12f -> 17.sp
        else -> 19.sp
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val resolvedBoxHeight = with(density) { resolvedLineHeight.toDp() }

    BasicTextField(
        value = fieldValue,
        onValueChange = { next ->
            fieldValue = next
            onValueChange(next.text)
        },
        readOnly = false,
        modifier = modifier
            .focusRequester(focusRequester)
            .heightIn(min = 20.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onFocusRequest()
                    // Browser-style behavior: the first focus selects the
                    // complete address, while later clicks keep the exact
                    // cursor position chosen by the user.
                    if (selectAllOnFocus && !wasFocused && fieldValue.text.isNotEmpty()) {
                        fieldValue = fieldValue.copy(
                            selection = androidx.compose.ui.text.TextRange(
                                0, fieldValue.text.length
                            )
                        )
                    }
                    wasFocused = true
                } else {
                    wasFocused = false
                }
            },
        textStyle = TextStyle(
            color = textColor,
            fontSize = fontSize,
            lineHeight = resolvedLineHeight,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = if (onImeGo != null) ImeAction.Go else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onGo = { onImeGo?.invoke() },
            onDone = { onImeGo?.invoke() }
        ),
        singleLine = true,
        cursorBrush = SolidColor(Color(0xFF1A73E8)),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth().height(resolvedBoxHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                if (fieldValue.text.isEmpty()) {
                    Text(
                        placeholder,
                        color = textColor.copy(0.4f),
                        fontSize = fontSize,
                        lineHeight = resolvedLineHeight,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// NavBtn — icon button for the navigation bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun NavBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    tint: Color,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val hoverTarget = if (hovered && enabled) tint.copy(alpha = 0.09f) else Color.Transparent
    val hoverBg by animateColorAsState(hoverTarget, label = "navButtonHover")

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(hoverBg)
            .hoverable(interactionSource)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(
            icon,
            contentDescription = contentDescription,
            tint     = if (enabled) tint else tint.copy(alpha = 0.25f),
            modifier = Modifier.size(17.dp)
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
    onAddressClear: () -> Unit,
    addressFocusRequestToken: Int = 0,
    onBookmarkToggle: () -> Unit,
    onMenuOpen: () -> Unit,
    onBookmarksPanel: () -> Unit,
    onHistoryPanel: () -> Unit,
    onDownloadsPanel: () -> Unit,
    onSettingsPanel: () -> Unit,
    onSiteSettings: () -> Unit,
    onFindInPage: () -> Unit,
    onChatGptPanel: () -> Unit,
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
                .height(48.dp)
                .background(navBarBg)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NavBtn(FluentIcons.ArrowBack,    enabled = canGoBack,    tint = iconColor, onClick = onBack)
            NavBtn(FluentIcons.ArrowForward, enabled = canGoForward, tint = iconColor, onClick = onForward)
            NavBtn(
                if (isLoading) FluentIcons.Close else FluentIcons.Refresh,
                tint = iconColor, onClick = onRefresh
            )
            NavBtn(FluentIcons.Home, tint = iconColor, onClick = onHome)

            Spacer(Modifier.width(4.dp))

            // ── Address Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
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
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Security / private indicator
                    when {
                        isPrivateTab -> FluentIcon(FluentIcons.Security, null,
                            tint = Color(0xFF9C6DCA), modifier = Modifier.size(11.dp).clickable { onSiteSettings() })
                        isSecure     -> FluentIcon(FluentIcons.Lock, null,
                            tint = Color(0xFF1A9A1A), modifier = Modifier.size(11.dp).clickable { onSiteSettings() })
                        isHttp       -> FluentIcon(FluentIcons.Warning, null,
                            tint = Color(0xFFD32F2F), modifier = Modifier.size(11.dp))
                        else         -> FluentIcon(FluentIcons.Language, null,
                            tint = Color(0xFF888888), modifier = Modifier.size(11.dp).clickable { onSiteSettings() })
                    }

                    BrowserTextField(
                        value              = addressText,
                        onValueChange      = onAddressChange,
                        placeholder        = if (isPrivateTab) "Private search or address" else "Search or enter address",
                        textColor          = if (isPrivateTab) Color(0xFFCCBBEE) else textColor,
                        modifier           = Modifier.weight(1f).fillMaxHeight(),
                        onFocusRequest     = onAddressFocus,
                        onImeGo            = onAddressGo,
                        selectAllOnFocus   = true,
                        focusRequestToken = addressFocusRequestToken,
                        fontSize           = 12.sp
                    )

                    // Clear address/search text while staying in the field.
                    if (addressBarFocused && addressText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { onAddressClear() },
                            contentAlignment = Alignment.Center
                        ) {
                            FluentIcon(FluentIcons.Close, "Clear", tint = textColor.copy(0.55f), modifier = Modifier.size(12.dp))
                        }
                    }

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

            NavBtn(FluentIcons.Article, tint = iconColor, onClick = onFindInPage)
            NavBtn(
                if (isBookmarked) FluentIcons.Star else FluentIcons.StarBorder,
                tint    = if (isBookmarked) Color(0xFFFFD700) else iconColor,
                onClick = onBookmarkToggle
            )
            NavBtn(FluentIcons.BookmarkBorder, tint = iconColor, onClick = onBookmarksPanel)
            NavBtn(FluentIcons.History,        tint = iconColor, onClick = onHistoryPanel)
            NavBtn(FluentIcons.Download,       tint = iconColor, onClick = onDownloadsPanel)
            NavBtn(FluentIcons.Settings,       tint = iconColor, onClick = onSettingsPanel)
            NavBtn(FluentIcons.Sparkle,         tint = iconColor, onClick = onChatGptPanel)
            NavBtn(FluentIcons.MoreVert,       tint = iconColor, onClick = onMenuOpen)
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
    onTabLongPressed: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onTabOverview: () -> Unit,
    onTabMoved: (fromId: String, toId: String) -> Unit,
    onTabMiddleClicked: (BrowserTab) -> Unit = onTabClosed
) {
    val accentBlue = Color(0xFF1A73E8)
    val iconColor  = if (isDark) Color(0xFF999999) else Color(0xFF555555)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
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
                var dragX by remember(tab.id) { mutableFloatStateOf(0f) }
                EdgeTabItem(
                    tab      = tab,
                    isActive = tab.id == activeTabId,
                    isDark   = isDark,
                    onSelect = { onTabSelected(tab) },
                    onClose  = { onTabClosed(tab) },
                    onLongPress = { onTabLongPressed(tab) },
                    onMiddleClick = { onTabMiddleClicked(tab) },
                    modifier = Modifier.pointerInput(tab.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragX = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragX += amount.x
                                val threshold = 72f
                                if (dragX >= threshold) {
                                    val index = tabs.indexOfFirst { it.id == tab.id }
                                    val next = index + 1
                                    if (index >= 0 && next < tabs.size && tabs[next].isPinned == tab.isPinned) {
                                        onTabMoved(tab.id, tabs[next].id)
                                        dragX = 0f
                                    }
                                } else if (dragX <= -threshold) {
                                    val index = tabs.indexOfFirst { it.id == tab.id }
                                    val next = index - 1
                                    if (index > 0 && tabs[next].isPinned == tab.isPinned) {
                                        onTabMoved(tab.id, tabs[next].id)
                                        dragX = 0f
                                    }
                                }
                            },
                            onDragEnd = { dragX = 0f },
                            onDragCancel = { dragX = 0f }
                        )
                    }
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
        NavBtn(FluentIcons.Add, tint = iconColor) { onNewTab() }

        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun EdgeTabItem(
    tab: BrowserTab,
    isActive: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onLongPress: () -> Unit,
    onMiddleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBg   = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
    val bg         = if (isActive) activeBg else Color.Transparent
    val textColor  = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF1A73E8)
    val privateColor = Color(0xFF9C6DCA)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val hoverTarget = if (hovered && !isActive) textColor.copy(alpha = 0.055f) else Color.Transparent
    val hoverBg by animateColorAsState(hoverTarget, label = "tabHover")

    Box(
        modifier = modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(bg)
            .hoverable(interactionSource)
            .drawBehind {
                if (hoverBg != Color.Transparent) drawRect(hoverBg)
            }
            .pointerInput(tab.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            when {
                                event.buttons.isTertiaryPressed -> {
                                    event.changes.forEach { it.consume() }
                                    onMiddleClick()
                                }
                                event.buttons.isSecondaryPressed -> {
                                    event.changes.forEach { it.consume() }
                                    onLongPress()
                                }
                            }
                        }
                    }
                }
            }
            .then(
                if (isActive) Modifier.drawBehind {
                    drawRect(
                        if (tab.isPrivate) privateColor else accentBlue,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                    )
                } else Modifier
            )
            .combinedClickable(onClick = onSelect, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (tab.isPrivate) {
                FluentIcon(FluentIcons.Security, null, tint = privateColor, modifier = Modifier.size(11.dp))
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

            if (tab.groupId != null) {
                Box(Modifier.size(6.dp).background(accentBlue, CircleShape))
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
                FluentIcon(FluentIcons.PushPin, null, tint = accentBlue, modifier = Modifier.size(8.dp))
            } else if (tab.rendererDiscarded) {
                // The renderer was intentionally released to save RAM. The tab
                // remains fully usable and will recreate its WebView on selection.
                FluentIcon(FluentIcons.Refresh, "Renderer will reload", tint = textColor.copy(alpha = 0.55f), modifier = Modifier.size(9.dp))
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close tab") { onClose() },
                contentAlignment = Alignment.Center
            ) {
                FluentIcon(FluentIcons.Close, null,
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
            .height(30.dp)
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
        FluentIcon(FluentIcons.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))

        BrowserTextField(
            value              = query,
            onValueChange      = onQueryChange,
            placeholder        = "Find in page…",
            textColor          = txtColor,
            modifier           = Modifier.weight(1f),
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

        NavBtn(FluentIcons.KeyboardArrowUp,   enabled = totalMatches > 0, tint = Color(0xFF888888)) { onFindPrev() }
        NavBtn(FluentIcons.KeyboardArrowDown, enabled = totalMatches > 0, tint = Color(0xFF888888)) { onFindNext() }
        NavBtn(FluentIcons.Close,             enabled = true,              tint = Color(0xFF888888)) { onClose() }
    }
}
