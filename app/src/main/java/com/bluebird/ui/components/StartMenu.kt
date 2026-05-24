package com.bluebird.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bluebird.AppInfo
import com.bluebird.LauncherScreen
import com.bluebird.LauncherUiState
import com.bluebird.LauncherViewModel
import com.bluebird.PowerAction
import com.bluebird.ui.theme.Win11Colors
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

// ─────────────────────────────────────────────────────────
// Helper: Drawable → Bitmap
// ─────────────────────────────────────────────────────────
fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return bitmap
    val w = if (intrinsicWidth > 0) intrinsicWidth else 1
    val h = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp); setBounds(0, 0, c.width, c.height); draw(c)
    return bmp
}

// ─────────────────────────────────────────────────────────
// Layout Mode & Preferences
// ─────────────────────────────────────────────────────────
enum class LayoutMode {
    COMPACT_GRID,
    LARGE_GRID,
    LIST_VIEW,
    HORIZONTAL_SCROLL,
    FAVORITES_BAR
}

enum class AppCategory {
    PINNED, RECENT, SYSTEM, FREQUENT, ALL
}

data class LayoutPreferences(
    val mode: LayoutMode = LayoutMode.COMPACT_GRID,
    val columns: Int = 6,
    val showRecentApps: Boolean = true,
    val showRecommended: Boolean = true,
    val showQuickActions: Boolean = true,
    val iconSize: Int = 38,
    val showLabels: Boolean = true,
    val compactSpacing: Boolean = false,
    val collapsibleGroups: Boolean = true,
    val enableCollapsibleGroups: Boolean = true
)

// ─────────────────────────────────────────────────────────
// App usage tracker for frequency sorting
// ─────────────────────────────────────────────────────────
private val appOpenCounts = mutableMapOf<String, Int>()

// ─────────────────────────────────────────────────────────
// Design Tokens — Professional Blue / Enterprise
// ─────────────────────────────────────────────────────────
private object DS {
    // Surfaces
    val glassDark   = Color(0xCC1C2128)   // 80% opacity — lets wallpaper show through
    val glassLight  = Color(0xCCF0F2F5)

    // Elevated surface (cards, inputs)
    val surfaceDark  = Color(0xFF252B32)
    val surfaceLight = Color(0xFFE8ECF0)

    // Borders — structural, slightly visible
    val borderDark  = Color(0xFF373E47)
    val borderLight = Color(0xFFCDD5DF)

    // Accent — professional blue (Microsoft/GitHub-grade)
    val accentStart = Color(0xFF0078D4)
    val accentMid   = Color(0xFF0078D4)
    val accentEnd   = Color(0xFF005A9E)

    // Hover — minimal
    val hoverDark  = Color(0x14FFFFFF)
    val hoverLight = Color(0x0C000000)

    // Pressed
    val pressedDark  = Color(0x22FFFFFF)
    val pressedLight = Color(0x14000000)

    // Destructive — muted red
    val badgeRed   = Color(0xFFCB4335)

    // Success green (online indicator)
    val successGreen = Color(0xFF3FB950)

    // Sizing
    val menuWidthCompact   = 560.dp
    val menuWidthExpanded  = 780.dp
    val menuHeightCompact  = 660.dp
    val menuHeightExpanded = 840.dp

    // Corner radii — modern, rounded
    val cornerRadius  = 12.dp
    val sectionCorner = 8.dp
    val chipCorner    = 6.dp

    val accentBrushValue: Brush = Brush.linearGradient(
        colors = listOf(accentStart, accentEnd),
        start = Offset(0f, 0f), end = Offset(160f, 160f)
    )
    fun accentBrush() = accentBrushValue
}

// ─────────────────────────────────────────────────────────
// Tab enum  (SEARCH is internal — not shown in tab bar)
// ─────────────────────────────────────────────────────────
private enum class StartMenuTab { PINNED, ALL_APPS, RECENT, SEARCH }

// ─────────────────────────────────────────────────────────
// Search result category
// ─────────────────────────────────────────────────────────
private enum class SearchCategory { APPS, SYSTEM, SETTINGS, FILES }

// ─────────────────────────────────────────────────────────
// Greeting helper
// ─────────────────────────────────────────────────────────
private fun timeGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }
}

// ─────────────────────────────────────────────────────────
// MAIN START MENU
// ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StartMenu(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(StartMenuTab.PINNED) }
    var isExpanded by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var layoutPrefs by remember { mutableStateOf(LayoutPreferences()) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var currentLayoutMode by remember { mutableStateOf(LayoutMode.COMPACT_GRID) }

    val menuWidth  = if (isExpanded) DS.menuWidthExpanded  else DS.menuWidthCompact
    val menuHeight = if (isExpanded) DS.menuHeightExpanded else DS.menuHeightCompact

    val isDark       = uiState.isDarkTheme
    val bgColor      = if (isDark) DS.glassDark else DS.glassLight
    val borderColor  = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary  = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = modifier
            .width(menuWidth)
            .height(menuHeight)
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(DS.cornerRadius), clip = false)
            .clip(RoundedCornerShape(DS.cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(DS.cornerRadius))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // ── Top Bar: Search + Layout + Expand ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = {
                        viewModel.updateSearchQuery(it)
                        activeTab = if (it.isNotEmpty()) StartMenuTab.SEARCH else StartMenuTab.PINNED
                    },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Layout mode picker
                Box {
                    IconToggleButton(
                        checked = showLayoutMenu,
                        onCheckedChange = { showLayoutMenu = it },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(DS.chipCorner))
                            .background(
                                if (showLayoutMenu) DS.accentStart.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = when (currentLayoutMode) {
                                LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> Icons.Default.GridView
                                LayoutMode.LIST_VIEW -> Icons.Default.FormatListBulleted
                                else -> Icons.Default.ViewComfy
                            },
                            contentDescription = "Layout: ${currentLayoutMode.name}",
                            tint = if (showLayoutMenu) DS.accentStart else textPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        modifier = Modifier
                            .background(
                                if (isDark) DS.surfaceDark else DS.glassLight,
                                RoundedCornerShape(DS.sectionCorner)
                            )
                            .border(1.dp, borderColor, RoundedCornerShape(DS.sectionCorner))
                    ) {
                        LayoutMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentLayoutMode = mode
                                        layoutPrefs = layoutPrefs.copy(
                                            mode = mode,
                                            columns = when (mode) {
                                                LayoutMode.COMPACT_GRID -> 6
                                                LayoutMode.LARGE_GRID -> 4
                                                else -> 6
                                            }
                                        )
                                        showLayoutMenu = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentLayoutMode == mode) {
                                    Icon(Icons.Default.Check, null, tint = DS.accentStart, modifier = Modifier.size(14.dp))
                                } else {
                                    Spacer(Modifier.size(14.dp))
                                }
                                Text(
                                    when (mode) {
                                        LayoutMode.COMPACT_GRID     -> "Compact Grid (6 cols)"
                                        LayoutMode.LARGE_GRID       -> "Large Grid (4 cols)"
                                        LayoutMode.LIST_VIEW        -> "List View"
                                        LayoutMode.HORIZONTAL_SCROLL -> "Horizontal Strip"
                                        LayoutMode.FAVORITES_BAR    -> "Favorites Bar"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                // Expand/Collapse
                IconToggleButton(
                    checked = isExpanded,
                    onCheckedChange = { isExpanded = it },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(DS.chipCorner))
                        .background(
                            if (isExpanded) DS.accentStart.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isExpanded) DS.accentStart else textPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Tab Navigation (hidden during search) ──
            if (activeTab != StartMenuTab.SEARCH) {
                PremiumTabRow(
                    activeTab = activeTab,
                    onTabChange = { activeTab = it },
                    isDark = isDark
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Content ──
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    StartMenuTab.PINNED -> PinnedView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        isExpanded = isExpanded,
                        editMode = editMode,
                        onEditModeToggle = { editMode = !editMode },
                        context = context,
                        layoutPrefs = layoutPrefs
                    )
                    StartMenuTab.ALL_APPS -> AllAppsView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        isExpanded = isExpanded,
                        context = context,
                        layoutPrefs = layoutPrefs
                    )
                    StartMenuTab.RECENT -> RecentAppsView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        context = context,
                        layoutPrefs = layoutPrefs
                    )
                    StartMenuTab.SEARCH -> SearchResultsView(
                        query = uiState.searchQuery,
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        onClearSearch = {
                            viewModel.updateSearchQuery("")
                            activeTab = StartMenuTab.PINNED
                        },
                        context = context,
                        layoutPrefs = layoutPrefs
                    )
                }
            }

            // ── Quick Actions Strip ──
            if (layoutPrefs.showQuickActions && activeTab != StartMenuTab.SEARCH) {
                Spacer(Modifier.height(10.dp))
                QuickActionsStrip(isDark = isDark)
            }

            // ── Bottom User Bar ──
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = borderColor, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            BottomUserBar(
                uiState = uiState,
                viewModel = viewModel,
                isDark = isDark
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// Tab Row
// ─────────────────────────────────────────────────────────
@Composable
private fun PremiumTabRow(
    activeTab: StartMenuTab,
    onTabChange: (StartMenuTab) -> Unit,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        listOf(
            StartMenuTab.PINNED   to "Pinned",
            StartMenuTab.ALL_APPS to "All Apps",
            StartMenuTab.RECENT   to "Recent"
        ).forEach { (tab, label) ->
            val isActive = activeTab == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabChange(tab) }
                    .padding(end = 24.dp, bottom = 0.dp)
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = if (isActive) DS.accentStart else textPrimary.copy(alpha = 0.45f),
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (isActive) DS.accentStart else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
    HorizontalDivider(
        color = if (isDark) DS.borderDark else DS.borderLight,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    title: String,
    isDark: Boolean,
    rightContent: (@Composable () -> Unit)? = null
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
        rightContent?.invoke()
    }
}

// ─────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────
@Composable
private fun EmptyStateView(message: String, isDark: Boolean) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Apps, null,
                tint = textPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = textPrimary.copy(alpha = 0.28f), fontSize = 13.sp, letterSpacing = 0.3.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────
// PINNED VIEW  — pinned user apps + system apps (no duplicates)
// ─────────────────────────────────────────────────────────
@Composable
private fun PinnedView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    isExpanded: Boolean,
    editMode: Boolean,
    onEditModeToggle: () -> Unit,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    val pinnedApps = uiState.pinnedTaskbarApps

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Pinned apps section ──
        item {
            SectionHeader(
                title = "Pinned",
                isDark = isDark,
                rightContent = {
                    CompactActionChip(
                        label = if (editMode) "Done" else "Edit",
                        icon  = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                        isDark = isDark,
                        onClick = onEditModeToggle
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            if (pinnedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(DS.sectionCorner))
                        .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No pinned apps — long-press any app to pin",
                        color = (if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight).copy(alpha = 0.25f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AppGridLayout(
                    apps = pinnedApps,
                    isDark = isDark,
                    editMode = editMode,
                    layoutPrefs = layoutPrefs,
                    onAppClick = { app ->
                        appOpenCounts[app.packageName] = (appOpenCounts[app.packageName] ?: 0) + 1
                        viewModel.openApp(context, app)
                    },
                    onAppUnpin   = { app -> viewModel.unpinAppFromTaskbar(app) },
                    onAppPin     = { app -> viewModel.pinAppToTaskbar(app) },
                    isBuiltIn    = false,
                    category     = AppCategory.PINNED
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        // ── System apps section ──
        if (builtInApps.isNotEmpty()) {
            item {
                SectionHeader(title = "System", isDark = isDark)
                Spacer(Modifier.height(10.dp))
            }
            item {
                BuiltInAppGridLayout(
                    apps       = builtInApps,
                    isDark     = isDark,
                    editMode   = editMode,
                    layoutPrefs = layoutPrefs,
                    onAppClick = { screen -> viewModel.openWindow(screen) },
                    category   = AppCategory.SYSTEM
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        // ── Recommended section ──
        if (layoutPrefs.showRecommended) {
            item {
                SectionHeader(title = "Recommended", isDark = isDark)
                Spacer(Modifier.height(10.dp))
            }
            item {
                RecommendedSection(
                    isDark = isDark,
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                    context = context
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ALL APPS VIEW — alphabetical with collapsible groups
// ─────────────────────────────────────────────────────────
@Composable
private fun AllAppsView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    isExpanded: Boolean,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    val sortedApps  = uiState.installedApps.sortedBy { it.name.lowercase() }
    val grouped     = sortedApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()
    val jumpLetters = grouped.keys.sorted()
    var expandedGroups by remember { mutableStateOf(grouped.keys.toSet()) }

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // System apps at the top of All Apps too
            item(key = "sys_header") {
                CollapsibleGroupHeader(
                    letter = "⚙",
                    label = "System",
                    appCount = builtInApps.size,
                    isDark = isDark,
                    isExpanded = '⚙' in expandedGroups,
                    onToggle = {
                        expandedGroups = if ('⚙' in expandedGroups)
                            expandedGroups - '⚙' else expandedGroups + '⚙'
                    }
                )
            }
            if ('⚙' in expandedGroups) {
                items(builtInApps, key = { it.second.name }) { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { viewModel.openWindow(screen) })
                }
            }
            item { Spacer(Modifier.height(4.dp)) }

            // User apps A-Z
            grouped.forEach { (letter, apps) ->
                item(key = "header_$letter") {
                    CollapsibleGroupHeader(
                        letter = letter.toString(),
                        label = null,
                        appCount = apps.size,
                        isDark = isDark,
                        isExpanded = letter in expandedGroups,
                        onToggle = {
                            expandedGroups = if (letter in expandedGroups)
                                expandedGroups - letter else expandedGroups + letter
                        }
                    )
                }
                if (letter in expandedGroups) {
                    items(apps, key = { it.packageName }) { app ->
                        AllAppsRow(
                            app = app,
                            isDark = isDark,
                            onClick = {
                                appOpenCounts[app.packageName] = (appOpenCounts[app.packageName] ?: 0) + 1
                                viewModel.openApp(context, app)
                            },
                            onPinToStart = { viewModel.pinAppToTaskbar(app) },
                            category = AppCategory.ALL
                        )
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        // Alphabetical jump sidebar
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                jumpLetters.forEach { letter ->
                    Text(
                        letter.toString(),
                        fontSize = 8.sp,
                        color = DS.accentStart,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val keys = grouped.keys.sorted()
                            val idx  = keys.indexOf(letter)
                            // +1 to account for system header at top
                            if (idx >= 0) scope.launch { listState.animateScrollToItem(idx * 2 + 3) }
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// RECENT APPS VIEW
// ─────────────────────────────────────────────────────────
@Composable
private fun RecentAppsView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    // Sort by open count desc (most frequent first), fallback to name
    val frequentApps = uiState.installedApps
        .sortedByDescending { appOpenCounts[it.packageName] ?: 0 }
        .take(12)
    val recentApps = uiState.installedApps.take(12)

    if (recentApps.isEmpty()) {
        EmptyStateView("No recent apps", isDark)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Most used
        if (frequentApps.any { (appOpenCounts[it.packageName] ?: 0) > 0 }) {
            item {
                SectionHeader(title = "Most used", isDark = isDark)
                Spacer(Modifier.height(10.dp))
            }
            item {
                AppGridLayout(
                    apps = frequentApps.filter { (appOpenCounts[it.packageName] ?: 0) > 0 }.take(6),
                    isDark = isDark,
                    editMode = false,
                    layoutPrefs = layoutPrefs,
                    onAppClick = { app ->
                        appOpenCounts[app.packageName] = (appOpenCounts[app.packageName] ?: 0) + 1
                        viewModel.openApp(context, app)
                    },
                    onAppUnpin = {},
                    onAppPin   = { app -> viewModel.pinAppToTaskbar(app) },
                    isBuiltIn  = false,
                    category   = AppCategory.FREQUENT
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            SectionHeader(title = "Recently installed", isDark = isDark)
            Spacer(Modifier.height(10.dp))
        }
        item {
            AppGridLayout(
                apps = recentApps,
                isDark = isDark,
                editMode = false,
                layoutPrefs = layoutPrefs,
                onAppClick = { app ->
                    appOpenCounts[app.packageName] = (appOpenCounts[app.packageName] ?: 0) + 1
                    viewModel.openApp(context, app)
                },
                onAppUnpin = {},
                onAppPin   = { app -> viewModel.pinAppToTaskbar(app) },
                isBuiltIn  = false,
                category   = AppCategory.RECENT
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// SEARCH RESULTS VIEW — categorized (Apps / System / Files)
// ─────────────────────────────────────────────────────────
@Composable
private fun SearchResultsView(
    query: String,
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    onClearSearch: () -> Unit,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    val appResults    = uiState.installedApps.filter { it.name.contains(query, ignoreCase = true) }
    val systemResults = builtInApps.filter { it.first.contains(query, ignoreCase = true) }
    val settingsKw    = listOf("wifi", "bluetooth", "brightness", "sound", "display", "theme", "language", "notification", "battery", "storage", "account", "password", "lock", "airplane")
    val settingResults = settingsKw.filter { it.contains(query, ignoreCase = true) }

    val totalCount = appResults.size + systemResults.size + settingResults.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (totalCount > 0) "Results for \"$query\" ($totalCount)" else "No results for \"$query\"",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary.copy(alpha = 0.5f),
                letterSpacing = 0.3.sp
            )
            TextButton(onClick = onClearSearch) {
                Text("Clear", color = DS.accentStart, fontSize = 11.sp)
            }
        }

        if (totalCount == 0) {
            EmptyStateView("Try a different search term", isDark)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Apps
            if (appResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "Apps", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(appResults, key = { it.packageName }) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = {
                            appOpenCounts[app.packageName] = (appOpenCounts[app.packageName] ?: 0) + 1
                            viewModel.openApp(context, app)
                        },
                        onPinToStart = { viewModel.pinAppToTaskbar(app) },
                        category = AppCategory.ALL
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // System
            if (systemResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "System", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(systemResults, key = { it.first }) { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { viewModel.openWindow(screen) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Settings keywords
            if (settingResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "Settings", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(settingResults, key = { it }) { kw ->
                    SettingsSearchRow(keyword = kw, isDark = isDark, onClick = { viewModel.openWindow(LauncherScreen.SETTINGS) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Search category chip label
// ─────────────────────────────────────────────────────────
@Composable
private fun SearchCategoryChip(label: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(DS.chipCorner))
            .background(DS.accentStart.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = DS.accentStart,
            letterSpacing = 0.3.sp
        )
    }
}

// ─────────────────────────────────────────────────────────
// Settings search row
// ─────────────────────────────────────────────────────────
@Composable
private fun SettingsSearchRow(keyword: String, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(keyword.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Normal, maxLines = 1)
            Text("Settings", fontSize = 10.sp, color = textPrimary.copy(alpha = 0.35f), maxLines = 1)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Collapsible Group Header
// ─────────────────────────────────────────────────────────
@Composable
private fun CollapsibleGroupHeader(
    letter: String,
    label: String?,       // null = use letter as label
    appCount: Int,
    isDark: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            null,
            tint = DS.accentStart,
            modifier = Modifier.size(15.dp)
        )
        Text(
            label ?: letter,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DS.accentStart,
            letterSpacing = 0.3.sp
        )
        Text(
            "($appCount)",
            fontSize = 9.sp,
            color = textPrimary.copy(alpha = 0.35f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = if (isDark) DS.borderDark else DS.borderLight,
            thickness = 0.5.dp
        )
    }
}

// ─────────────────────────────────────────────────────────
// App Grid Layout (adaptive)
// ─────────────────────────────────────────────────────────
@Composable
private fun AppGridLayout(
    apps: List<AppInfo>,
    isDark: Boolean,
    editMode: Boolean,
    layoutPrefs: LayoutPreferences,
    onAppClick: (AppInfo) -> Unit,
    onAppUnpin: (AppInfo) -> Unit,
    onAppPin: (AppInfo) -> Unit,
    isBuiltIn: Boolean = false,
    category: AppCategory = AppCategory.ALL
) {
    if (apps.isEmpty()) return

    when (layoutPrefs.mode) {
        LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layoutPrefs.columns),
                modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = true
            ) {
                items(apps) { app ->
                    AnimatedPinnedIcon(
                        app = app,
                        isDark = isDark,
                        editMode = editMode,
                        onClick = { onAppClick(app) },
                        onUnpin = { onAppUnpin(app) },
                        onPinToTaskbar = { onAppPin(app) },
                        isPinnedToTaskbar = category == AppCategory.PINNED,
                        iconSize = layoutPrefs.iconSize,
                        showLabel = layoutPrefs.showLabels,
                        category = category,
                        badgeCount = appOpenCounts[app.packageName]?.takeIf { it > 0 }
                    )
                }
            }
        }
        LayoutMode.LIST_VIEW -> {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(apps) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = { onAppClick(app) },
                        onPinToStart = { onAppPin(app) },
                        category = category
                    )
                }
            }
        }
        LayoutMode.HORIZONTAL_SCROLL -> {
            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(apps) { app ->
                    HorizontalAppCard(app = app, isDark = isDark, onClick = { onAppClick(app) }, onPin = { onAppPin(app) })
                }
            }
        }
        LayoutMode.FAVORITES_BAR -> {
            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(apps) { app ->
                    CompactAppIcon(app = app, isDark = isDark, onClick = { onAppClick(app) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Built-in App Grid Layout
// ─────────────────────────────────────────────────────────
@Composable
private fun BuiltInAppGridLayout(
    apps: List<Triple<String, ImageVector, LauncherScreen>>,
    isDark: Boolean,
    editMode: Boolean,
    layoutPrefs: LayoutPreferences,
    onAppClick: (LauncherScreen) -> Unit,
    category: AppCategory = AppCategory.SYSTEM
) {
    if (apps.isEmpty()) return

    when (layoutPrefs.mode) {
        LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layoutPrefs.columns),
                modifier = Modifier.heightIn(max = 250.dp),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = true
            ) {
                items(apps) { (name, icon, screen) ->
                    AnimatedBuiltInIcon(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        editMode = editMode,
                        onClick = { onAppClick(screen) },
                        iconSize = layoutPrefs.iconSize,
                        showLabel = layoutPrefs.showLabels
                    )
                }
            }
        }
        LayoutMode.LIST_VIEW -> {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(apps) { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { onAppClick(screen) })
                }
            }
        }
        LayoutMode.HORIZONTAL_SCROLL -> {
            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(apps) { (name, icon, screen) ->
                    HorizontalBuiltInCard(name = name, icon = icon, isDark = isDark, onClick = { onAppClick(screen) })
                }
            }
        }
        LayoutMode.FAVORITES_BAR -> {
            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(apps) { (name, icon, screen) ->
                    CompactBuiltInIcon(name = name, icon = icon, isDark = isDark, onClick = { onAppClick(screen) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Premium Search Bar
// ─────────────────────────────────────────────────────────
@Composable
fun PremiumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor     = if (isDark) DS.surfaceDark else DS.surfaceLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textColor   = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isFocused) DS.accentStart else borderColor,
                shape = RoundedCornerShape(DS.sectionCorner)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search, null,
                tint = if (isFocused) DS.accentStart else textColor.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).onFocusEvent { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontSize = 13.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search apps, files, settings…", color = textColor.copy(alpha = 0.3f), fontSize = 13.sp)
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close, null,
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(13.dp).clickable { onQueryChange("") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Animated Pinned Icon — with badge count + folder support
// ─────────────────────────────────────────────────────────
@Composable
fun AnimatedPinnedIcon(
    app: AppInfo,
    isDark: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    onPinToTaskbar: () -> Unit,
    isPinnedToTaskbar: Boolean,
    iconSize: Int = 38,
    showLabel: Boolean = true,
    category: AppCategory = AppCategory.ALL,
    badgeCount: Int? = null
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    val wobbleAngle by animateFloatAsState(
        targetValue = if (editMode) 2f else 0f,
        animationSpec = if (editMode) infiniteRepeatable(tween(350), RepeatMode.Reverse) else tween(150),
        label = "wobble"
    )

    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(72.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
                .rotate(wobbleAngle)
                .pointerInput(editMode) {
                    detectTapGestures(
                        onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap       = { if (!editMode) onClick() },
                        onLongPress = { if (!editMode) showMenu = true }
                    )
                }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(modifier = Modifier.size(iconSize.dp), contentAlignment = Alignment.TopEnd) {
                // App icon
                Box(
                    modifier = Modifier.size(iconSize.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.icon != null) {
                        val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                        Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size((iconSize - 4).dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size((iconSize - 4).dp)
                                .clip(RoundedCornerShape(DS.sectionCorner))
                                .background(if (isDark) DS.surfaceDark else DS.surfaceLight)
                                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Badge count
                if (badgeCount != null && badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .widthIn(min = 16.dp)
                            .height(16.dp)
                            .background(DS.badgeRed, CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    app.name,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Edit mode unpin button
        if (editMode && category == AppCategory.PINNED) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(DS.badgeRed, CircleShape)
                    .clickable { onUnpin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(if (isDark) DS.surfaceDark else DS.glassLight, RoundedCornerShape(DS.sectionCorner))
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            if (category == AppCategory.PINNED) {
                StyledMenuItem("Unpin from Start", Icons.Default.PushPin, isDark, tintAccent = true) { showMenu = false; onUnpin() }
            } else {
                StyledMenuItem("Pin to Start", Icons.Default.PushPin, isDark) { showMenu = false; onPinToTaskbar() }
            }
            StyledMenuItem(
                if (isPinnedToTaskbar) "Unpin from taskbar" else "Pin to taskbar",
                Icons.Default.PushPin, isDark
            ) { showMenu = false; onPinToTaskbar() }
            StyledMenuItem("App info", Icons.Default.Info, isDark) { showMenu = false }
        }
    }
}

@Composable
private fun StyledMenuItem(
    label: String,
    icon: ImageVector,
    isDark: Boolean,
    tintAccent: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDark) Color.White else Color.Black
    val iconTint  = if (tintAccent) DS.badgeRed else textColor.copy(alpha = 0.7f)

    DropdownMenuItem(
        text = { Text(label, fontSize = 12.sp, color = textColor) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = iconTint, modifier = Modifier.size(15.dp)) },
        modifier = Modifier.height(36.dp)
    )
}

// ─────────────────────────────────────────────────────────
// Animated Built-in App Icon
// ─────────────────────────────────────────────────────────
@Composable
fun AnimatedBuiltInIcon(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    iconSize: Int = 38,
    showLabel: Boolean = true
) {
    val wobble by animateFloatAsState(
        targetValue = if (editMode) -2f else 0f,
        animationSpec = if (editMode) infiniteRepeatable(tween(350), RepeatMode.Reverse) else tween(150),
        label = "wobble2"
    )
    var pressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .rotate(wobble)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, name, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                fontSize = 10.sp,
                color = (if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Compact App Icon (Favorites Bar)
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactAppIcon(app: AppInfo, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (app.icon != null) {
            val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(32.dp))
        } else {
            Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// Compact Built-in Icon (Favorites Bar)
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactBuiltInIcon(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(DS.accentStart.copy(alpha = if (pressed) 1f else 0.8f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, name, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────────────────
// Horizontal App Card
// ─────────────────────────────────────────────────────────
@Composable
private fun HorizontalAppCard(app: AppInfo, isDark: Boolean, onClick: () -> Unit, onPin: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg    = if (isDark) DS.surfaceDark else DS.surfaceLight

    Box {
        Row(
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(cardBg)
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap       = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    if (app.icon != null) {
                        val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                        Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(app.name, fontSize = 9.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(if (isDark) DS.surfaceDark else DS.glassLight, RoundedCornerShape(DS.sectionCorner))
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            StyledMenuItem("Pin to taskbar", Icons.Default.PushPin, isDark) { showMenu = false; onPin() }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Horizontal Built-in Card
// ─────────────────────────────────────────────────────────
@Composable
private fun HorizontalBuiltInCard(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(cardBg)
            .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(DS.sectionCorner)).background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, name, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name, fontSize = 9.sp,
            color = (if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────
// All Apps Row (user app)
// ─────────────────────────────────────────────────────────
@Composable
private fun AllAppsRow(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit,
    onPinToStart: () -> Unit,
    category: AppCategory = AppCategory.ALL
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap       = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                if (app.icon != null) {
                    val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                    Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, fontSize = 10.sp, color = textPrimary.copy(alpha = 0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Frequency badge
            val count = appOpenCounts[app.packageName] ?: 0
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DS.chipCorner))
                        .background(DS.accentStart.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${count}×", fontSize = 9.sp, color = DS.accentStart, fontWeight = FontWeight.Medium)
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(if (isDark) DS.surfaceDark else DS.glassLight, RoundedCornerShape(DS.sectionCorner))
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            if (category != AppCategory.PINNED) {
                StyledMenuItem("Pin to Start", Icons.Default.PushPin, isDark) { showMenu = false; onPinToStart() }
            }
            StyledMenuItem("Pin to taskbar", Icons.Default.PushPin, isDark) { showMenu = false; onPinToStart() }
            StyledMenuItem("App info", Icons.Default.Info, isDark) { showMenu = false }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Built-in App List Row
// ─────────────────────────────────────────────────────────
@Composable
private fun BuiltInAppListRow(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(DS.sectionCorner)).background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("System app", fontSize = 10.sp, color = textPrimary.copy(alpha = 0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Recommended Section — recent files
// ─────────────────────────────────────────────────────────
@Composable
private fun RecommendedSection(isDark: Boolean, viewModel: LauncherViewModel, isExpanded: Boolean, context: Context) {
    var recentFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        recentFiles = getRecentFiles(context)
    }

    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    if (recentFiles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
            contentAlignment = Alignment.Center
        ) {
            Text("No recent items", color = textPrimary.copy(alpha = 0.25f), fontSize = 11.sp, letterSpacing = 0.3.sp)
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recentFiles) { file ->
            RecentCard(
                title    = file.name,
                subtitle = file.readableSize(),
                icon     = getFileIcon(file.extension),
                isDark   = isDark,
                onClick  = { viewModel.openFileWithSystem(context, file.absolutePath) }
            )
        }
    }
}

@Composable
private fun RecentCard(
    title: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    iconDrawable: Drawable? = null,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg      = if (isDark) DS.surfaceDark else DS.surfaceLight

    Row(
        modifier = Modifier
            .width(155.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(cardBg)
            .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (iconDrawable != null) {
                val bmp = remember(title) { iconDrawable.toBitmap().asImageBitmap() }
                Image(bitmap = bmp, contentDescription = title, modifier = Modifier.size(26.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 9.sp, color = textPrimary.copy(alpha = 0.38f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Quick Actions Strip
// ─────────────────────────────────────────────────────────
@Composable
private fun QuickActionsStrip(isDark: Boolean) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val actions = listOf(
        Pair(Icons.Default.Wifi,              "Wi-Fi"),
        Pair(Icons.Default.Bluetooth,         "Bluetooth"),
        Pair(Icons.Default.AirplanemodeActive,"Airplane"),
        Pair(Icons.Default.DoNotDisturb,      "Focus"),
        Pair(Icons.Default.Brightness6,       "Brightness"),
        Pair(Icons.Default.VolumeUp,          "Sound")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        actions.forEach { (icon, label) ->
            var active by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DS.chipCorner))
                    .background(if (active) DS.accentStart else if (isDark) DS.surfaceDark else DS.surfaceLight)
                    .border(0.5.dp, if (active) DS.accentEnd else if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.chipCorner))
                    .clickable { active = !active }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, label, tint = if (active) Color.White else textPrimary.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 8.sp, color = if (active) Color.White else textPrimary.copy(alpha = 0.45f), maxLines = 1, textAlign = TextAlign.Center, letterSpacing = 0.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Compact Action Chip
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactActionChip(label: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DS.chipCorner))
            .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.chipCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = DS.accentStart.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
        Text(label, fontSize = 10.sp, color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.Normal, letterSpacing = 0.3.sp)
    }
}

// ─────────────────────────────────────────────────────────
// Bottom User Bar — with greeting
// ─────────────────────────────────────────────────────────
@Composable
private fun BottomUserBar(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val greeting    = remember { timeGreeting() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User tile
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(DS.sectionCorner))
                .clickable { viewModel.openWindow(LauncherScreen.SETTINGS) }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(32.dp)) {
                if (uiState.userProfile.profilePicturePath.isNotEmpty()) {
                    AsyncImage(
                        model = Uri.parse(uiState.userProfile.profilePicturePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(DS.accentStart, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.userProfile.userName.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Online indicator
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .background(DS.successGreen, CircleShape)
                        .border(1.5.dp, if (isDark) DS.glassDark else DS.glassLight, CircleShape)
                )
            }
            Column {
                Text(
                    "$greeting, ${uiState.userProfile.userName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Account settings",
                    fontSize = 9.sp,
                    color = DS.accentStart,
                    maxLines = 1,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BottomBarIconBtn(Icons.Default.NotificationsNone, "Notifications", isDark) { /* notifications */ }
            BottomBarIconBtn(Icons.Default.Settings, "Settings", isDark) { viewModel.openWindow(LauncherScreen.SETTINGS) }
            BottomBarIconBtn(Icons.Default.PowerSettingsNew, "Power", isDark, tint = DS.badgeRed) { viewModel.togglePowerMenu() }
        }
    }
}

@Composable
private fun BottomBarIconBtn(icon: ImageVector, label: String, isDark: Boolean, tint: Color? = null, onClick: () -> Unit) {
    val defaultTint = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(DS.chipCorner))
            .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.chipCorner))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint ?: defaultTint.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
    }
}

// ─────────────────────────────────────────────────────────
// Power Menu
// ─────────────────────────────────────────────────────────
@Composable
fun PowerMenu(isDark: Boolean, onAction: (PowerAction) -> Unit, modifier: Modifier = Modifier) {
    val bgColor     = if (isDark) DS.surfaceDark else DS.glassLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(DS.sectionCorner))
            .shadow(12.dp, RoundedCornerShape(DS.sectionCorner))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Power options",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary.copy(alpha = 0.5f),
                    letterSpacing = 0.3.sp
                )
            }
            HorizontalDivider(color = borderColor, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 4.dp))
            powerOptions.forEach { (label, icon, action) ->
                PremiumPowerMenuItem(label, icon, isDark, action == PowerAction.SHUTDOWN) { onAction(action) }
            }
        }
    }
}

@Composable
private fun PremiumPowerMenuItem(
    label: String,
    icon: ImageVector,
    isDark: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val textColor = if (isDestructive) DS.badgeRed else if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val iconTint  = if (isDestructive) DS.badgeRed else DS.accentStart

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.chipCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Normal)
    }
}

// ─────────────────────────────────────────────────────────
// Legacy Compatibility Functions
// ─────────────────────────────────────────────────────────
@Composable
fun StartMenuAppIcon(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit,
    onAddToDesktop: () -> Unit = {},
    onPinToTaskbar: () -> Unit = {},
    isPinnedToTaskbar: Boolean = false
) {
    AnimatedPinnedIcon(
        app = app,
        isDark = isDark,
        editMode = false,
        onClick = onClick,
        onUnpin = {},
        onPinToTaskbar = onPinToTaskbar,
        isPinnedToTaskbar = isPinnedToTaskbar
    )
}

@Composable
fun BuiltInAppIcon(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    AnimatedBuiltInIcon(name = name, icon = icon, isDark = isDark, editMode = false, onClick = onClick)
}

@Composable
fun StartMenuSearch(query: String, onQueryChange: (String) -> Unit, isDark: Boolean, modifier: Modifier = Modifier) =
    PremiumSearchBar(query, onQueryChange, isDark, modifier)

// ─────────────────────────────────────────────────────────
// Built-in apps registry
// ─────────────────────────────────────────────────────────
internal val builtInApps = listOf(
    Triple("Settings",     Icons.Default.Settings,          LauncherScreen.SETTINGS),
    Triple("Files",        Icons.Default.Folder,            LauncherScreen.FILE_EXPLORER),
    Triple("Browser",      Icons.Default.Language,          LauncherScreen.BROWSER),
    Triple("Calculator",   Icons.Default.Calculate,         LauncherScreen.CALCULATOR),
    Triple("Calendar",     Icons.Default.CalendarMonth,     LauncherScreen.CALENDAR),
    Triple("Photos",       Icons.Default.PhotoLibrary,      LauncherScreen.PHOTOS),
    Triple("Tasks",        Icons.Default.Assignment,        LauncherScreen.TASK_MANAGER),
    Triple("Phone",        Icons.Default.Phone,             LauncherScreen.PHONE),
    Triple("Messages",     Icons.Default.Message,           LauncherScreen.MESSAGES),
    Triple("Media Player", Icons.Default.PlayCircleOutline, LauncherScreen.MEDIA_PLAYER),
    Triple("Recycle Bin",  Icons.Default.Delete,            LauncherScreen.RECYCLE_BIN),
    Triple("Image Viewer", Icons.Default.Photo,             LauncherScreen.IMAGE_VIEWER),
    Triple("Text Editor",  Icons.Default.TextFields,        LauncherScreen.TextEditorScreen),
)

private val powerOptions = listOf(
    Triple("Sleep",     Icons.Default.BedtimeOff,       PowerAction.SLEEP),
    Triple("Lock",      Icons.Default.Lock,             PowerAction.LOCK),
    Triple("Restart",   Icons.Default.RestartAlt,       PowerAction.RESTART),
    Triple("Shut down", Icons.Default.PowerSettingsNew, PowerAction.SHUTDOWN),
)

// ─────────────────────────────────────────────────────────
// Utility Helpers
// ─────────────────────────────────────────────────────────
private fun File.readableSize(): String {
    val size = this.length()
    return when {
        size < 1024            -> "$size B"
        size < 1024 * 1024     -> "%.1f KB".format(size / 1024.0)
        else                   -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}

private fun getFileIcon(extension: String): ImageVector = when (extension.lowercase()) {
    "pdf"                        -> Icons.Default.PictureAsPdf
    "doc", "docx"                -> Icons.Default.Article
    "xls", "xlsx"                -> Icons.Default.TableChart
    "jpg", "jpeg", "png", "gif"  -> Icons.Default.Image
    else                         -> Icons.Default.InsertDriveFile
}

private fun getRecentFiles(context: Context): List<File> {
    val files = mutableListOf<File>()
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else
        MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
    val sortOrder  = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
    try {
        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            var count = 0
            while (cursor.moveToNext() && count < 6) {
                val path = cursor.getString(dataIdx)
                val file = File(path)
                if (file.exists()) { files.add(file); count++ }
            }
        }
    } catch (_: SecurityException) {}
    return files
}
