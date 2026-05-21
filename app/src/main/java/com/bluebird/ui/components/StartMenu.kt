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
// Layout Mode & Preferences Data Class
// ─────────────────────────────────────────────────────────
enum class LayoutMode {
    COMPACT_GRID,      // 6-8 columns, vertical scroll
    LARGE_GRID,        // 4-5 columns, larger icons
    LIST_VIEW,         // Single column with details
    HORIZONTAL_SCROLL, // Card-based horizontal strip
    FAVORITES_BAR      // Top horizontal favorites
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
// Design Tokens — Enterprise Slate / Teal
// ─────────────────────────────────────────────────────────
private object DS {
    // Surfaces — deep slate, no purple or blue tint
    val glassDark   = Color(0xF51C2128)
    val glassLight  = Color(0xFFF0F2F5)

    // Elevated surface (cards, inputs) — one step lighter than bg
    val surfaceDark  = Color(0xFF252B32)
    val surfaceLight = Color(0xFFE8ECF0)

    // Borders — structural, slightly visible
    val borderDark  = Color(0xFF373E47)
    val borderLight = Color(0xFFCDD5DF)

    // Accent — deep teal / cyan — professional, not vibrant
    val accentStart = Color(0xFF2A9D8F)
    val accentMid   = Color(0xFF2A9D8F)
    val accentEnd   = Color(0xFF1D7A6E)

    // Hover — minimal
    val hoverDark  = Color(0x14FFFFFF)
    val hoverLight = Color(0x0C000000)

    // Pressed
    val pressedDark  = Color(0x22FFFFFF)
    val pressedLight = Color(0x14000000)

    // Destructive — muted terracotta
    val badgeRed   = Color(0xFFCB4335)

    // Section rule — accent left-border color
    val ruleColor = Color(0xFF2A9D8F)

    // Sizing
    val menuWidthCompact   = 560.dp
    val menuWidthExpanded  = 780.dp
    val menuHeightCompact  = 640.dp
    val menuHeightExpanded = 820.dp
    val cornerRadius = 4.dp
    val sectionCorner = 3.dp

    // Stable brush
    val accentBrushValue: Brush = Brush.linearGradient(
        colors = listOf(accentStart, accentEnd),
        start = Offset(0f, 0f), end = Offset(160f, 160f)
    )
    fun accentBrush() = accentBrushValue
}

// ─────────────────────────────────────────────────────────
// Tab enum
// ─────────────────────────────────────────────────────────
private enum class StartMenuTab { PINNED, ALL_APPS, SEARCH, RECENT }

// ─────────────────────────────────────────────────────────
// MAIN START MENU - COMPLETELY REFACTORED
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

    // Dynamic sizing based on layout mode
    val menuWidth = if (isExpanded) DS.menuWidthExpanded else DS.menuWidthCompact
    val menuHeight = if (isExpanded) DS.menuHeightExpanded else DS.menuHeightCompact

    val isDark = uiState.isDarkTheme
    val bgColor = if (isDark) DS.glassDark else DS.glassLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val textSecondary = textPrimary.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .width(menuWidth)
            .height(menuHeight)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(DS.cornerRadius), clip = false)
            .clip(RoundedCornerShape(DS.cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(DS.cornerRadius))
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(DS.accentStart)
        )

        Column(modifier = Modifier.fillMaxSize().padding(start = 23.dp, end = 20.dp)) {
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
                        if (it.isNotEmpty()) activeTab = StartMenuTab.SEARCH
                        else activeTab = StartMenuTab.PINNED
                    },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Layout Mode Button with Current Indicator
                Box {
                    IconToggleButton(
                        checked = showLayoutMenu,
                        onCheckedChange = { showLayoutMenu = it },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(DS.cornerRadius))
                            .background(
                                if (showLayoutMenu) DS.accentStart.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (currentLayoutMode) {
                                    LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> Icons.Default.GridView
                                    LayoutMode.LIST_VIEW -> Icons.Default.FormatListBulleted
                                    LayoutMode.HORIZONTAL_SCROLL, LayoutMode.FAVORITES_BAR -> Icons.Default.ViewComfy
                                },
                                contentDescription = "Layout mode: ${currentLayoutMode.name}",
                                tint = if (showLayoutMenu) DS.accentStart else textPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            // Active indicator dot
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(DS.accentStart, CircleShape)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        modifier = Modifier
                            .background(
                                if (isDark) DS.surfaceDark else DS.glassLight,
                                RoundedCornerShape(DS.cornerRadius)
                            )
                            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.cornerRadius))
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
                                                LayoutMode.LIST_VIEW -> 1
                                                else -> 6
                                            }
                                        )
                                        showLayoutMenu = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Checkmark for active mode
                                if (currentLayoutMode == mode) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = DS.accentStart,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Spacer(Modifier.size(14.dp))
                                }
                                Text(
                                    when (mode) {
                                        LayoutMode.COMPACT_GRID -> "Compact Grid (6 cols)"
                                        LayoutMode.LARGE_GRID -> "Large Grid (4 cols)"
                                        LayoutMode.LIST_VIEW -> "List View"
                                        LayoutMode.HORIZONTAL_SCROLL -> "Horizontal Strip"
                                        LayoutMode.FAVORITES_BAR -> "Favorites Bar"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                // Expand/Collapse toggle
                IconToggleButton(
                    checked = isExpanded,
                    onCheckedChange = { isExpanded = it },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(DS.cornerRadius))
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

            Spacer(Modifier.height(16.dp))

            // ── Tab Navigation ──
            if (activeTab != StartMenuTab.SEARCH) {
                PremiumTabRow(
                    activeTab = activeTab,
                    onTabChange = { activeTab = it },
                    isDark = isDark
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Content ──
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    StartMenuTab.PINNED -> {
                        PinnedAndRecentView(
                            uiState = uiState,
                            viewModel = viewModel,
                            isDark = isDark,
                            isExpanded = isExpanded,
                            editMode = editMode,
                            onEditModeToggle = { editMode = !editMode },
                            context = context,
                            layoutPrefs = layoutPrefs
                        )
                    }
                    StartMenuTab.ALL_APPS -> AllAppsView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        isExpanded = isExpanded,
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
                    StartMenuTab.RECENT -> RecentAppsView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        context = context,
                        layoutPrefs = layoutPrefs
                    )
                }
            }

            // ── Bottom User Bar ──
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                color = if (isDark) DS.borderDark else DS.borderLight,
                thickness = 1.dp
            )
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
// Empty State View
// ─────────────────────────────────────────────────────────
@Composable
private fun EmptyStateView(message: String, isDark: Boolean) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Apps,
                null,
                tint = textPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = textPrimary.copy(alpha = 0.3f),
                fontSize = 14.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Premium Tab Row
// ─────────────────────────────────────────────────────────
@Composable
private fun PremiumTabRow(
    activeTab: StartMenuTab,
    onTabChange: (StartMenuTab) -> Unit,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        listOf(
            StartMenuTab.PINNED to "Pinned",
            StartMenuTab.RECENT to "Recent",
            StartMenuTab.ALL_APPS to "All Apps"
        ).forEach { (tab, label) ->
            val isActive = activeTab == tab

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabChange(tab) }
                    .padding(end = 24.dp, bottom = 0.dp)
            ) {
                Text(
                    label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) DS.accentStart else textPrimary.copy(alpha = 0.38f),
                    letterSpacing = 1.2.sp
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
        thickness = 1.dp
    )
}

// ─────────────────────────────────────────────────────────
// REFACTORED: Pinned & Recent Apps View (NO DUPLICATE)
// ─────────────────────────────────────────────────────────
@Composable
private fun PinnedAndRecentView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    isExpanded: Boolean,
    editMode: Boolean,
    onEditModeToggle: () -> Unit,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val pinnedApps = uiState.pinnedTaskbarApps
    val hasSystemApps = builtInApps.isNotEmpty()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── ALL PINNED APPS (Pinned + System) ──
        item {
            if (pinnedApps.isNotEmpty() || hasSystemApps) {
                SectionHeader(
                    title = "PINNED & SYSTEM APPS",
                    isDark = isDark,
                    rightContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CompactActionChip(
                                label = if (editMode) "Done" else "Edit",
                                icon = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                                isDark = isDark,
                                onClick = onEditModeToggle
                            )
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        // Combined Pinned + System Apps
        if (pinnedApps.isNotEmpty() || hasSystemApps) {
            item {
                Column {
                    // Pinned Apps
                    if (pinnedApps.isNotEmpty()) {
                        AppGridLayout(
                            apps = pinnedApps,
                            isDark = isDark,
                            editMode = editMode,
                            layoutPrefs = layoutPrefs,
                            onAppClick = { app -> viewModel.openApp(context, app) },
                            onAppUnpin = { app -> viewModel.unpinAppFromTaskbar(app) },
                            onAppPin = { app -> viewModel.pinAppToTaskbar(app) },
                            isBuiltIn = false,
                            category = AppCategory.PINNED
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // System Apps
                    if (hasSystemApps) {
                        BuiltInAppGridLayout(
                            apps = builtInApps,
                            isDark = isDark,
                            editMode = editMode,
                            layoutPrefs = layoutPrefs,
                            onAppClick = { screen -> viewModel.openWindow(screen) },
                            category = AppCategory.SYSTEM
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }

        // ── RECENT APPS SECTION ──
        if (layoutPrefs.showRecentApps) {
            item {
                val recentApps = uiState.installedApps.take(8)
                if (recentApps.isNotEmpty()) {
                    SectionHeader(
                        title = "RECENT",
                        isDark = isDark
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                val recentApps = uiState.installedApps.take(8)
                if (recentApps.isNotEmpty()) {
                    AppGridLayout(
                        apps = recentApps,
                        isDark = isDark,
                        editMode = false,
                        layoutPrefs = layoutPrefs,
                        onAppClick = { app -> viewModel.openApp(context, app) },
                        onAppUnpin = {},
                        onAppPin = { app -> viewModel.pinAppToTaskbar(app) },
                        isBuiltIn = false,
                        category = AppCategory.RECENT
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        // ── RECOMMENDED SECTION ──
        if (layoutPrefs.showRecommended) {
            item {
                SectionHeader(
                    title = "RECOMMENDED",
                    isDark = isDark
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                RecommendedSection(
                    isDark = isDark,
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                    context = context
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── QUICK ACTIONS STRIP ──
        if (layoutPrefs.showQuickActions) {
            item {
                QuickActionsStrip(isDark = isDark)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Section Header Component
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(DS.accentStart, RoundedCornerShape(2.dp))
            )
            Text(
                title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary.copy(alpha = 0.55f),
                letterSpacing = 1.2.sp
            )
        }
        if (rightContent != null) {
            rightContent()
        }
    }
}

// ─────────────────────────────────────────────────────────
// App Grid Layout (Adaptive) - ENHANCED
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
                        category = category
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(apps) { app ->
                    HorizontalAppCard(
                        app = app,
                        isDark = isDark,
                        onClick = { onAppClick(app) },
                        onPin = { onAppPin(app) }
                    )
                }
            }
        }

        LayoutMode.FAVORITES_BAR -> {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp, max = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(apps) { app ->
                    CompactAppIcon(
                        app = app,
                        isDark = isDark,
                        onClick = { onAppClick(app) }
                    )
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
                    BuiltInAppListRow(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        onClick = { onAppClick(screen) }
                    )
                }
            }
        }

        LayoutMode.HORIZONTAL_SCROLL -> {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(apps) { (name, icon, screen) ->
                    HorizontalBuiltInCard(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        onClick = { onAppClick(screen) }
                    )
                }
            }
        }

        LayoutMode.FAVORITES_BAR -> {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp, max = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(apps) { (name, icon, screen) ->
                    CompactBuiltInIcon(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        onClick = { onAppClick(screen) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// All Apps View — grouped & categorized
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
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val sortedApps = uiState.installedApps.sortedBy { it.name.lowercase() }
    val grouped = sortedApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val jumpItems = grouped.keys.sorted()
    var expandedGroups by remember { mutableStateOf(grouped.keys.toSet()) }

    Row(modifier = Modifier.fillMaxSize()) {
        // App list with collapsible groups
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            grouped.forEach { (letter, apps) ->
                item(key = "header_$letter") {
                    CollapsibleGroupHeader(
                        letter = letter.toString(),
                        appCount = apps.size,
                        isDark = isDark,
                        isExpanded = letter in expandedGroups,
                        onToggle = {
                            expandedGroups = if (letter in expandedGroups) {
                                expandedGroups - letter
                            } else {
                                expandedGroups + letter
                            }
                        }
                    )
                }

                if (letter in expandedGroups) {
                    items(apps, key = { it.packageName }) { app ->
                        AllAppsRow(
                            app = app,
                            isDark = isDark,
                            onClick = { viewModel.openApp(context, app) },
                            onPinToStart = { viewModel.pinAppToTaskbar(app) },
                            category = AppCategory.ALL
                        )
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        // Alphabetical jump sidebar
        if (layoutPrefs.showLabels) {
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
                    jumpItems.forEach { letter ->
                        Text(
                            letter.toString(),
                            fontSize = 8.sp,
                            color = DS.accentStart,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                val keys = grouped.keys.sorted()
                                val idx = keys.indexOf(letter)
                                if (idx >= 0) scope.launch { listState.animateScrollToItem(idx * 2) }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Collapsible Group Header with Toggle
// ─────────────────────────────────────────────────────────
@Composable
private fun CollapsibleGroupHeader(
    letter: String,
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
            modifier = Modifier.size(16.dp)
        )
        Text(
            letter,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = DS.accentStart,
            letterSpacing = 0.5.sp
        )
        Text(
            "($appCount)",
            fontSize = 9.sp,
            color = textPrimary.copy(alpha = 0.35f),
            letterSpacing = 0.5.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = if (isDark) DS.borderDark else DS.borderLight,
            thickness = 1.dp
        )
    }
}

// ─────────────────────────────────────────────────────────
// Recent Apps View
// ─────────────────────────────────────────────────────────
@Composable
private fun RecentAppsView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    context: Context,
    layoutPrefs: LayoutPreferences
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val recentApps = uiState.installedApps.take(12)

    if (recentApps.isEmpty()) {
        EmptyStateView("No recent apps", isDark)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SectionHeader(title = "RECENTLY USED", isDark = isDark)
                Spacer(Modifier.height(10.dp))
            }

            item {
                AppGridLayout(
                    apps = recentApps,
                    isDark = isDark,
                    editMode = false,
                    layoutPrefs = layoutPrefs,
                    onAppClick = { app -> viewModel.openApp(context, app) },
                    onAppUnpin = {},
                    onAppPin = { app -> viewModel.pinAppToTaskbar(app) },
                    isBuiltIn = false,
                    category = AppCategory.RECENT
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Search Results View
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
    val results = uiState.installedApps.filter {
        it.name.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(12.dp)
                        .background(DS.accentStart, RoundedCornerShape(2.dp))
                )
                Text(
                    "RESULTS FOR \"${query.uppercase()}\" (${results.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary.copy(alpha = 0.55f),
                    letterSpacing = 1.2.sp
                )
            }
            TextButton(onClick = onClearSearch) {
                Text("Clear", color = DS.accentStart, fontSize = 11.sp, letterSpacing = 0.3.sp)
            }
        }
        Spacer(Modifier.height(6.dp))

        if (results.isEmpty()) {
            EmptyStateView("No results found for \"$query\"", isDark)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(results) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = { viewModel.openApp(context, app) },
                        onPinToStart = { viewModel.pinAppToTaskbar(app) },
                        category = AppCategory.ALL
                    )
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
    val bgColor = if (isDark) DS.surfaceDark else DS.surfaceLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(DS.cornerRadius))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isFocused) DS.accentStart else borderColor,
                shape = RoundedCornerShape(DS.cornerRadius)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search,
                null,
                tint = if (isFocused) DS.accentStart else textColor.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusEvent { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = 13.sp
                ),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search apps, files, settings…",
                            color = textColor.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(13.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Animated Pinned Icon - ENHANCED (Pin/Unpin independent)
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
    category: AppCategory = AppCategory.ALL
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
                .background(
                    if (pressed)
                        if (isDark) DS.pressedDark else DS.pressedLight
                    else Color.Transparent
                )
                .rotate(wobbleAngle)
                .pointerInput(editMode) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { if (!editMode) onClick() },
                        onLongPress = { if (!editMode) showMenu = true }
                    )
                }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier.size(iconSize.dp),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                    Image(
                        bitmap = bmp,
                        contentDescription = app.name,
                        modifier = Modifier.size((iconSize - 4).dp)
                    )
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (isDark) DS.surfaceDark else DS.glassLight,
                    RoundedCornerShape(DS.cornerRadius)
                )
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.cornerRadius))
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            if (category == AppCategory.PINNED) {
                StyledMenuItem("Unpin from Start", Icons.Default.PushPin, isDark, tintAccent = true) {
                    showMenu = false; onUnpin()
                }
            } else {
                StyledMenuItem("Pin to Start", Icons.Default.PushPin, isDark) {
                    showMenu = false; onPinToTaskbar()
                }
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
    val iconTint = if (tintAccent) DS.badgeRed else textColor.copy(alpha = 0.7f)

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
            .background(
                if (pressed)
                    if (isDark) DS.pressedDark else DS.pressedLight
                else Color.Transparent
            )
            .rotate(wobble)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
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
private fun CompactAppIcon(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(
                if (pressed)
                    if (isDark) DS.pressedDark else DS.pressedLight
                else Color.Transparent
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (app.icon != null) {
            val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
            Image(
                bitmap = bmp,
                contentDescription = app.name,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// Compact Built-in Icon (Favorites Bar)
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactBuiltInIcon(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(DS.accentStart.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
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
private fun HorizontalAppCard(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight

    Box {
        Row(
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(cardBg)
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { onClick() },
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
                Text(
                    app.name,
                    fontSize = 9.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (isDark) DS.surfaceDark else DS.glassLight,
                    RoundedCornerShape(DS.cornerRadius)
                )
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.cornerRadius))
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
private fun HorizontalBuiltInCard(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight

    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(cardBg)
            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(DS.sectionCorner))
                    .background(DS.accentStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, name, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                fontSize = 9.sp,
                color = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// All Apps List Row - ENHANCED
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
                .background(
                    if (pressed)
                        if (isDark) DS.pressedDark else DS.pressedLight
                    else Color.Transparent
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                    Image(
                        bitmap = bmp,
                        contentDescription = app.name,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontSize = 13.sp,
                    color = textPrimary,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    fontSize = 10.sp,
                    color = textPrimary.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (isDark) DS.surfaceDark else DS.glassLight,
                    RoundedCornerShape(DS.cornerRadius)
                )
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.cornerRadius))
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
private fun BuiltInAppListRow(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(
                if (pressed)
                    if (isDark) DS.pressedDark else DS.pressedLight
                else Color.Transparent
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
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
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 13.sp,
                color = textPrimary,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "System App",
                fontSize = 10.sp,
                color = textPrimary.copy(alpha = 0.35f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Recommended Section
// ─────────────────────────────────────────────────────────
@Composable
private fun RecommendedSection(
    isDark: Boolean,
    viewModel: LauncherViewModel,
    isExpanded: Boolean,
    context: Context
) {
    var recentFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        scope.launch {
            recentFiles = getRecentFiles(context)
        }
    }

    val recentApps = emptyList<AppInfo>()
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    if (recentFiles.isEmpty() && recentApps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No recent items",
                color = textPrimary.copy(alpha = 0.25f),
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recentFiles) { file ->
            RecentCard(
                title = file.name,
                subtitle = file.readableSize(),
                icon = getFileIcon(file.extension),
                isDark = isDark,
                onClick = { viewModel.openFileWithSystem(context, file.absolutePath) }
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
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight

    Row(
        modifier = Modifier
            .width(155.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(cardBg)
            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconDrawable != null) {
                val bmp = remember(title) { iconDrawable.toBitmap().asImageBitmap() }
                Image(bitmap = bmp, contentDescription = title, modifier = Modifier.size(26.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 11.sp,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                fontSize = 9.sp,
                color = textPrimary.copy(alpha = 0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        Pair(Icons.Default.Wifi, "Wi-Fi"),
        Pair(Icons.Default.Bluetooth, "Bluetooth"),
        Pair(Icons.Default.AirplanemodeActive, "Airplane"),
        Pair(Icons.Default.DoNotDisturb, "Focus"),
        Pair(Icons.Default.Brightness6, "Brightness"),
        Pair(Icons.Default.VolumeUp, "Sound")
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(DS.accentStart, RoundedCornerShape(2.dp))
            )
            Text(
                "QUICK ACTIONS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary.copy(alpha = 0.5f),
                letterSpacing = 1.2.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            actions.forEach { (icon, label) ->
                var active by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(DS.sectionCorner))
                        .background(
                            if (active) DS.accentStart
                            else if (isDark) DS.surfaceDark else DS.surfaceLight
                        )
                        .border(
                            1.dp,
                            if (active) DS.accentEnd
                            else if (isDark) DS.borderDark else DS.borderLight,
                            RoundedCornerShape(DS.sectionCorner)
                        )
                        .clickable { active = !active }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            icon, label,
                            tint = if (active) Color.White else textPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            fontSize = 8.sp,
                            color = if (active) Color.White else textPrimary.copy(alpha = 0.45f),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Compact Action Chip
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactActionChip(
    label: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DS.sectionCorner))
            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = DS.accentStart.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.7f),
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp
        )
    }
}

// ─────────────────────────────────────────────────────────
// Bottom User Bar
// ─────────────────────────────���───────────────────────────
@Composable
private fun BottomUserBar(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(DS.sectionCorner))
                .clickable { viewModel.openWindow(LauncherScreen.SETTINGS) }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(30.dp)) {
                if (uiState.userProfile.profilePicturePath.isNotEmpty()) {
                    AsyncImage(
                        model = Uri.parse(uiState.userProfile.profilePicturePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DS.accentStart, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.userProfile.userName.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF3FB950), CircleShape)
                        .border(1.5.dp, if (isDark) DS.glassDark else DS.glassLight, CircleShape)
                )
            }
            Column {
                Text(
                    uiState.userProfile.userName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
            BottomBarIconBtn(Icons.Default.Settings, "Settings", isDark) {
                viewModel.openWindow(LauncherScreen.SETTINGS)
            }
            BottomBarIconBtn(Icons.Default.PowerSettingsNew, "Power", isDark, tint = DS.badgeRed) {
                viewModel.togglePowerMenu()
            }
        }
    }
}

@Composable
private fun BottomBarIconBtn(
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val defaultTint = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint ?: defaultTint.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
    }
}

// ─────────────────────────────────────────────────────────
// Power Menu - FIXED (No Taskbar Interference)
// ─────────────────────────────────────────────────────────
@Composable
fun PowerMenu(isDark: Boolean, onAction: (PowerAction) -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (isDark) DS.surfaceDark else DS.glassLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(DS.cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(DS.cornerRadius))
            .shadow(12.dp, RoundedCornerShape(DS.cornerRadius))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(12.dp)
                        .background(DS.accentStart, RoundedCornerShape(2.dp))
                )
                Text(
                    "POWER OPTIONS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )
            }
            HorizontalDivider(
                color = if (isDark) DS.borderDark else DS.borderLight,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            powerOptions.forEach { (label, icon, action) ->
                PremiumPowerMenuItem(label, icon, isDark, action == PowerAction.SHUTDOWN) {
                    onAction(action)
                }
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
    val textColor = if (isDestructive) DS.badgeRed
    else if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val iconTint = if (isDestructive) DS.badgeRed else DS.accentStart

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(
                if (pressed)
                    if (isDark) DS.pressedDark else DS.pressedLight
                else Color.Transparent
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
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
fun BuiltInAppIcon(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    AnimatedBuiltInIcon(name = name, icon = icon, isDark = isDark, editMode = false, onClick = onClick)
}

@Composable
fun StartMenuSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) = PremiumSearchBar(query, onQueryChange, isDark, modifier)

// ─────────────────────────────────────────────────────────
// Built-in apps registry
// ─────────────────────────────────────────────────────────
internal val builtInApps = listOf(
    Triple("Settings",     Icons.Default.Settings,         LauncherScreen.SETTINGS),
    Triple("Files",        Icons.Default.Folder,           LauncherScreen.FILE_EXPLORER),
    Triple("Browser",      Icons.Default.Language,         LauncherScreen.BROWSER),
    Triple("Calculator",   Icons.Default.Calculate,        LauncherScreen.CALCULATOR),
    Triple("Calendar",     Icons.Default.CalendarMonth,    LauncherScreen.CALENDAR),
    Triple("Photos",       Icons.Default.PhotoLibrary,     LauncherScreen.PHOTOS),
    Triple("Tasks",        Icons.Default.Assignment,       LauncherScreen.TASK_MANAGER),
    Triple("Phone",        Icons.Default.Phone,            LauncherScreen.PHONE),
    Triple("Messages",     Icons.Default.Message,          LauncherScreen.MESSAGES),
    Triple("Media Player", Icons.Default.PlayCircleOutline,LauncherScreen.MEDIA_PLAYER),
    Triple("Recycle Bin",  Icons.Default.Delete,           LauncherScreen.RECYCLE_BIN),
    Triple("Image Viewer", Icons.Default.Photo,            LauncherScreen.IMAGE_VIEWER),
    Triple("Text Editor",  Icons.Default.TextFields,       LauncherScreen.TextEditorScreen),
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
private fun java.io.File.readableSize(): String {
    val size = this.length()
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}

private fun getFileIcon(extension: String): ImageVector = when (extension.lowercase()) {
    "pdf"                    -> Icons.Default.PictureAsPdf
    "doc", "docx"            -> Icons.Default.Article
    "xls", "xlsx"            -> Icons.Default.TableChart
    "jpg", "jpeg", "png","gif"-> Icons.Default.Image
    else                     -> Icons.Default.InsertDriveFile
}

private fun getRecentFiles(context: Context): List<File> {
    val files = mutableListOf<File>()
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
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
