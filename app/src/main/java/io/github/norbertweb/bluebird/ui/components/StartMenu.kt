package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assignment
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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextRotationAngledown
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
import androidx.compose.ui.draw.scale
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
import io.github.norbertweb.bluebird.AppInfo
import io.github.norbertweb.bluebird.LauncherScreen
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
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
    FAVORITES_BAR,
    MOBILE_HOME
}

// Start Menu window size. Persisted across opens (see getSavedSizeMode/saveSizeMode) —
// previously this was a plain `remember { mutableStateOf(false) }` boolean which reset
// to compact every time the Start Menu left composition (i.e. every time it was closed).
enum class StartMenuSizeMode { COMPACT, EXPANDED, FULLSCREEN }

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
    val enableCollapsibleGroups: Boolean = true
)

// ─────────────────────────────────────────────────────────
// App usage persistent tracker functions
// ─────────────────────────────────────────────────────────
private fun getAppOpenCount(context: Context, packageName: String): Int {
    val prefs = context.getSharedPreferences("start_menu_usage_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("open_cnt_$packageName", 0)
}

private fun incrementAppOpenCount(context: Context, packageName: String) {
    val prefs = context.getSharedPreferences("start_menu_usage_prefs", Context.MODE_PRIVATE)
    val current = prefs.getInt("open_cnt_$packageName", 0)
    prefs.edit().putInt("open_cnt_$packageName", current + 1).apply()
}

// ─────────────────────────────────────────────────────────
// Persisted Start Menu size + layout preference
// (fixes: size choice previously reset every time the Start Menu closed)
// ─────────────────────────────────────────────────────────
private const val START_MENU_PREFS = "start_menu_layout_prefs"

private fun getSavedSizeMode(context: Context): StartMenuSizeMode {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    val name = prefs.getString("size_mode", StartMenuSizeMode.COMPACT.name)
    return runCatching { StartMenuSizeMode.valueOf(name ?: "COMPACT") }
        .getOrDefault(StartMenuSizeMode.COMPACT)
}

private fun saveSizeMode(context: Context, mode: StartMenuSizeMode) {
    context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
        .edit().putString("size_mode", mode.name).apply()
}

private fun getSavedLayoutMode(context: Context): LayoutMode {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    val name = prefs.getString("layout_mode", LayoutMode.COMPACT_GRID.name)
    return runCatching { LayoutMode.valueOf(name ?: "COMPACT_GRID") }
        .getOrDefault(LayoutMode.COMPACT_GRID)
}

private fun saveLayoutMode(context: Context, mode: LayoutMode) {
    context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
        .edit().putString("layout_mode", mode.name).apply()
}

// ─────────────────────────────────────────────────────────
// Design Tokens — Professional Blue / Enterprise
// ─────────────────────────────────────────────────────────
private object DS {
    val glassDark   = Color(0xCC1C2128)
    val glassLight  = Color(0xCCF0F2F5)
    val surfaceDark  = Color(0xFF252B32)
    val surfaceLight = Color(0xFFE8ECF0)
    val borderDark  = Color(0xFF373E47)
    val borderLight = Color(0xFFCDD5DF)
    val accentStart = Color(0xFF0078D4)
    val accentEnd   = Color(0xFF005A9E)
    val hoverDark  = Color(0x14FFFFFF)
    val hoverLight = Color(0x0C000000)
    val pressedDark  = Color(0x22FFFFFF)
    val pressedLight = Color(0x14000000)
    val badgeRed   = Color(0xFFCB4335)
    val successGreen = Color(0xFF3FB950)

    val menuWidthCompact   = 560.dp
    val menuWidthExpanded  = 780.dp
    val menuHeightCompact  = 660.dp
    val menuHeightExpanded = 840.dp

    val cornerRadius  = 12.dp
    val sectionCorner = 8.dp
    val chipCorner    = 6.dp

    val accentBrushValue: Brush = Brush.linearGradient(
        colors = listOf(accentStart, accentEnd),
        start = Offset(0f, 0f), end = Offset(160f, 160f)
    )
    fun accentBrush() = accentBrushValue
}

private enum class StartMenuTab { PINNED, ALL_APPS, RECENT, SEARCH }

private fun timeGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }
}

// ─────────────────────────────────────────────────────────
// Built-In System Apps Definition
// ─────────────────────────────────────────────────────────
private val builtInApps = listOf(
    Triple("Settings", Icons.Default.Settings, LauncherScreen.SETTINGS),
    Triple("Calculator", Icons.Default.Calculate, LauncherScreen.CALCULATOR),
    Triple("Calendar", Icons.Default.CalendarMonth, LauncherScreen.CALENDAR),
    Triple("Bluebird Store", Icons.Default.NightsStay, LauncherScreen.BLUEBIRD_STORE),
    Triple("Word Impress", Icons.Default.TextRotationAngledown, LauncherScreen.WORD_IMPRESS),

    Triple("Files",        Icons.Default.Folder,            LauncherScreen.FILE_EXPLORER),
    Triple("Browser",      Icons.Default.Language,          LauncherScreen.BROWSER),

    Triple("Photos",       Icons.Default.PhotoLibrary,      LauncherScreen.PHOTOS),
    Triple("Tasks",        Icons.Default.Assignment,        LauncherScreen.TASK_MANAGER),


    Triple("Media Player", Icons.Default.LiveTv, LauncherScreen.MEDIA_PLAYER),
    Triple("Recycle Bin",  Icons.Default.Delete,            LauncherScreen.RECYCLE_BIN),
    Triple("Image Viewer", Icons.Default.Photo,             LauncherScreen.IMAGE_VIEWER),
    Triple("Text Editor",  Icons.Default.TextFields,        LauncherScreen.PremiumTextEditorScreen),

    Triple("Terminal", Icons.Default.Terminal,             LauncherScreen.TERMINAL),
    Triple("Web App Manager",  Icons.Default.Language,        LauncherScreen.WEB_APP_MANAGER)


)

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
    // Persisted (not just `remember`ed) so the size survives the Start Menu being closed/reopened.
    var sizeMode by remember { mutableStateOf(getSavedSizeMode(context)) }
    var editMode by remember { mutableStateOf(false) }
    var layoutPrefs by remember { mutableStateOf(LayoutPreferences(mode = getSavedLayoutMode(context))) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }

    fun changeSizeMode(mode: StartMenuSizeMode) {
        sizeMode = mode
        saveSizeMode(context, mode)
    }

    fun changeLayoutMode(mode: LayoutMode) {
        layoutPrefs = layoutPrefs.copy(
            mode = mode,
            columns = when (mode) {
                LayoutMode.COMPACT_GRID -> 6
                LayoutMode.LARGE_GRID -> 4
                else -> 6
            }
        )
        saveLayoutMode(context, mode)
        // Mobile home screen needs the whole screen to feel like a phone launcher.
        if (mode == LayoutMode.MOBILE_HOME && sizeMode != StartMenuSizeMode.FULLSCREEN) {
            changeSizeMode(StartMenuSizeMode.FULLSCREEN)
        }
    }

    val isMobileHome = layoutPrefs.mode == LayoutMode.MOBILE_HOME
    val isFullscreen = sizeMode == StartMenuSizeMode.FULLSCREEN

    val menuWidth  = when (sizeMode) {
        StartMenuSizeMode.COMPACT    -> DS.menuWidthCompact
        StartMenuSizeMode.EXPANDED   -> DS.menuWidthExpanded
        StartMenuSizeMode.FULLSCREEN -> DS.menuWidthExpanded // ignored, fillMaxSize used instead
    }
    val menuHeight = when (sizeMode) {
        StartMenuSizeMode.COMPACT    -> DS.menuHeightCompact
        StartMenuSizeMode.EXPANDED   -> DS.menuHeightExpanded
        StartMenuSizeMode.FULLSCREEN -> DS.menuHeightExpanded // ignored, fillMaxSize used instead
    }
    // Fullscreen reads as a real "home screen" rather than a floating panel, so drop the
    // rounded corners/border in that mode.
    val cornerRadius = if (isFullscreen) 0.dp else DS.cornerRadius

    val isDark       = uiState.isDarkTheme
    val bgColor      = if (isDark) DS.glassDark else DS.glassLight
    val borderColor  = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    Box(
        modifier = modifier
            .then(
                if (isFullscreen) Modifier.fillMaxSize()
                else Modifier.width(menuWidth).height(menuHeight)
            )
            .shadow(elevation = if (isFullscreen) 0.dp else 24.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .then(
                if (isFullscreen) Modifier else Modifier.border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            )
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
                            imageVector = when (layoutPrefs.mode) {
                                LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> Icons.Default.GridView
                                LayoutMode.LIST_VIEW -> Icons.Default.FormatListBulleted
                                else -> Icons.Default.ViewComfy
                            },
                            contentDescription = "Layout: ${layoutPrefs.mode.name}",
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
                                        changeLayoutMode(mode)
                                        showLayoutMenu = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (layoutPrefs.mode == mode) {
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
                                        LayoutMode.MOBILE_HOME      -> "Mobile Layout (phone-style)"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                if (mode == LayoutMode.MOBILE_HOME) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        null,
                                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Size picker: Compact / Expanded / Full Screen — replaces the old
                // two-state expand toggle and is now persisted (see changeSizeMode()).
                if (!isMobileHome) {
                    Box {
                        IconToggleButton(
                            checked = showSizeMenu,
                            onCheckedChange = { showSizeMenu = it },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(DS.chipCorner))
                                .background(
                                    if (sizeMode != StartMenuSizeMode.COMPACT) DS.accentStart.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = when (sizeMode) {
                                    StartMenuSizeMode.COMPACT    -> Icons.Default.Fullscreen
                                    StartMenuSizeMode.EXPANDED   -> Icons.Default.AspectRatio
                                    StartMenuSizeMode.FULLSCREEN -> Icons.Default.FullscreenExit
                                },
                                contentDescription = "Window size: ${sizeMode.name}",
                                tint = if (sizeMode != StartMenuSizeMode.COMPACT) DS.accentStart else textPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSizeMenu,
                            onDismissRequest = { showSizeMenu = false },
                            modifier = Modifier
                                .background(
                                    if (isDark) DS.surfaceDark else DS.glassLight,
                                    RoundedCornerShape(DS.sectionCorner)
                                )
                                .border(1.dp, borderColor, RoundedCornerShape(DS.sectionCorner))
                        ) {
                            listOf(
                                StartMenuSizeMode.COMPACT to "Compact",
                                StartMenuSizeMode.EXPANDED to "Expanded",
                                StartMenuSizeMode.FULLSCREEN to "Full Screen"
                            ).forEach { (mode, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            changeSizeMode(mode)
                                            showSizeMenu = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (sizeMode == mode) {
                                        Icon(Icons.Default.Check, null, tint = DS.accentStart, modifier = Modifier.size(14.dp))
                                    } else {
                                        Spacer(Modifier.size(14.dp))
                                    }
                                    Text(label, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isMobileHome) {
                // ── Mobile-style home screen: full-bleed paginated app grid + dock ──
                MobileHomeView(
                    uiState = uiState,
                    viewModel = viewModel,
                    isDark = isDark,
                    context = context,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // `isExpanded` is used by the existing grid views only to pick icon/column
                // sizing, so any non-compact size counts as "expanded" for that purpose.
                val isExpandedLayout = sizeMode != StartMenuSizeMode.COMPACT

                // ── Tab Navigation (hidden during search) ──
                if (activeTab != StartMenuTab.SEARCH) {
                    PremiumTabRow(
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        isDark = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── Content with Windows 11 Transitions ──
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
                        label = "tab_switching"
                    ) { targetTab ->
                        when (targetTab) {
                            StartMenuTab.PINNED -> PinnedView(
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = isDark,
                                isExpanded = isExpandedLayout,
                                editMode = editMode,
                                onEditModeToggle = { editMode = !editMode },
                                context = context,
                                layoutPrefs = layoutPrefs
                            )
                            StartMenuTab.ALL_APPS -> AllAppsView(
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = isDark,
                                isExpanded = isExpandedLayout,
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
                }

                // ── Quick Actions Strip ──
                if (layoutPrefs.showQuickActions && activeTab != StartMenuTab.SEARCH) {
                    Spacer(Modifier.height(10.dp))
                    QuickActionsStrip(isDark = isDark, context = context)
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
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

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
                        .width(40.dp)
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

@Composable
private fun SectionHeader(
    title: String,
    isDark: Boolean,
    rightContent: (@Composable () -> Unit)? = null
) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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

@Composable
private fun EmptyStateView(message: String, isDark: Boolean) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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
// PINNED VIEW
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
                        color = (if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight).copy(alpha = 0.25f),
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
                        incrementAppOpenCount(context, app.packageName)
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
// MOBILE HOME VIEW — phone-launcher-style paginated app grid + dock
// ─────────────────────────────────────────────────────────
private const val MOBILE_HOME_APPS_PER_PAGE = 20 // 4 columns × 5 rows

@Composable
private fun MobileHomeView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    val sortedApps = remember(uiState.installedApps) {
        uiState.installedApps.sortedBy { it.name.lowercase() }
    }
    val pages = remember(sortedApps) {
        val chunks = sortedApps.chunked(MOBILE_HOME_APPS_PER_PAGE)
        if (chunks.isEmpty()) listOf(emptyList()) else chunks
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val dockApps = uiState.pinnedTaskbarApps.take(5)

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pages[pageIndex], key = { it.packageName }) { app ->
                    MobileAppIcon(
                        app = app,
                        isDark = isDark,
                        onClick = {
                            incrementAppOpenCount(context, app.packageName)
                            viewModel.openApp(context, app)
                        }
                    )
                }
            }
        }

        // Page indicator dots (only shown when apps span more than one page)
        if (pages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (active) DS.accentStart else textPrimary.copy(alpha = 0.25f))
                    )
                }
            }
        }

        // Bottom dock — pinned/taskbar apps, phone-style
        if (dockApps.isNotEmpty()) {
            HorizontalDivider(color = if (isDark) DS.borderDark else DS.borderLight, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) DS.surfaceDark.copy(alpha = 0.5f) else DS.surfaceLight.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dockApps.forEach { app ->
                    MobileAppIcon(
                        app = app,
                        isDark = isDark,
                        showLabel = false,
                        iconSize = 48,
                        onClick = {
                            incrementAppOpenCount(context, app.packageName)
                            viewModel.openApp(context, app)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileAppIcon(
    app: AppInfo,
    isDark: Boolean,
    showLabel: Boolean = true,
    iconSize: Int = 56,
    onClick: () -> Unit
) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, label = "mobile_icon_scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) DS.surfaceDark else DS.surfaceLight),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
                Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size((iconSize - 12).dp))
            } else {
                Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size((iconSize / 2.5).dp))
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.name,
                fontSize = 10.sp,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width((iconSize + 16).dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// ALL APPS VIEW
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
            if (layoutPrefs.enableCollapsibleGroups) {
                item(key = "sys_header") {
                    CollapsibleGroupHeader(
                        letter = "⚙",
                        label = "System",
                        appCount = builtInApps.size,
                        isDark = isDark,
                        isExpanded = '⚙' in expandedGroups,
                        onToggle = {
                            expandedGroups = if ('⚙' in expandedGroups) expandedGroups - '⚙' else expandedGroups + '⚙'
                        }
                    )
                }
            }
            if (!layoutPrefs.enableCollapsibleGroups || '⚙' in expandedGroups) {
                items(builtInApps, key = { "built_in_" + it.first }) { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { viewModel.openWindow(screen) })
                }
            }
            item { Spacer(Modifier.height(4.dp)) }

            grouped.forEach { (letter, apps) ->
                if (layoutPrefs.enableCollapsibleGroups) {
                    item(key = "header_$letter") {
                        CollapsibleGroupHeader(
                            letter = letter.toString(),
                            label = null,
                            appCount = apps.size,
                            isDark = isDark,
                            isExpanded = letter in expandedGroups,
                            onToggle = {
                                expandedGroups = if (letter in expandedGroups) expandedGroups - letter else expandedGroups + letter
                            }
                        )
                    }
                }
                if (!layoutPrefs.enableCollapsibleGroups || letter in expandedGroups) {
                    items(apps, key = { app -> "app_" + app.packageName }) { app ->
                        AllAppsRow(
                            app = app,
                            isDark = isDark,
                            onClick = {
                                incrementAppOpenCount(context, app.packageName)
                                viewModel.openApp(context, app)
                            },
                            onPinToStart = { viewModel.pinAppToTaskbar(app) },
                            onPinToTaskbar = { viewModel.pinAppToTaskbar(app) },
                            category = AppCategory.ALL
                        )
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        // Alphabetical Jump Sidebar
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                jumpLetters.forEach { letter ->
                    Text(
                        letter.toString(),
                        fontSize = 9.sp,
                        color = DS.accentStart,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    var itemsIndexBefore = 0
                                    if (layoutPrefs.enableCollapsibleGroups) {
                                        itemsIndexBefore += 1
                                    }
                                    if (!layoutPrefs.enableCollapsibleGroups || '⚙' in expandedGroups) {
                                        itemsIndexBefore += builtInApps.size
                                    }
                                    itemsIndexBefore += 1

                                    for (currentLetter in jumpLetters) {
                                        if (currentLetter == letter) break
                                        if (layoutPrefs.enableCollapsibleGroups) {
                                            itemsIndexBefore += 1
                                        }
                                        if (!layoutPrefs.enableCollapsibleGroups || currentLetter in expandedGroups) {
                                            itemsIndexBefore += grouped[currentLetter]?.size ?: 0
                                        }
                                        itemsIndexBefore += 1
                                    }
                                    listState.animateScrollToItem(itemsIndexBefore)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
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
    val pm = context.packageManager

    val recentApps = remember(uiState.installedApps) {
        uiState.installedApps.sortedByDescending { app ->
            try {
                pm.getPackageInfo(app.packageName, 0).firstInstallTime
            } catch (e: Exception) {
                0L
            }
        }.take(12)
    }

    val frequentApps = remember(uiState.installedApps) {
        uiState.installedApps
            .filter { getAppOpenCount(context, it.packageName) > 0 }
            .sortedByDescending { getAppOpenCount(context, it.packageName) }
            .take(6)
    }

    if (recentApps.isEmpty() && frequentApps.isEmpty()) {
        EmptyStateView("No recent activities detected", isDark)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (frequentApps.isNotEmpty()) {
            item {
                SectionHeader(title = "Most used", isDark = isDark)
                Spacer(Modifier.height(10.dp))
            }
            item {
                AppGridLayout(
                    apps = frequentApps,
                    isDark = isDark,
                    editMode = false,
                    layoutPrefs = layoutPrefs,
                    onAppClick = { app ->
                        incrementAppOpenCount(context, app.packageName)
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
                    incrementAppOpenCount(context, app.packageName)
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
// SEARCH RESULTS VIEW
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
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val appResults    = uiState.installedApps.filter { it.name.contains(query, ignoreCase = true) }
    val systemResults = builtInApps.filter { it.first.contains(query, ignoreCase = true) }
    val settingsKw    = listOf("wifi", "bluetooth", "brightness", "sound", "display", "theme", "language", "notification", "battery", "storage", "account", "password", "lock", "airplane")
    val settingResults = settingsKw.filter { it.contains(query, ignoreCase = true) }

    val totalCount = appResults.size + systemResults.size + settingResults.size

    Column(modifier = Modifier.fillMaxSize()) {
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
            if (appResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "Apps", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(appResults, key = { "search_app_" + it.packageName }) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = {
                            incrementAppOpenCount(context, app.packageName)
                            viewModel.openApp(context, app)
                        },
                        onPinToStart = { viewModel.pinAppToTaskbar(app) },
                        onPinToTaskbar = { viewModel.pinAppToTaskbar(app) },
                        category = AppCategory.ALL
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (systemResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "System Actions", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(systemResults, key = { "search_sys_" + it.first }) { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { viewModel.openWindow(screen) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (settingResults.isNotEmpty()) {
                item {
                    SearchCategoryChip(label = "Settings Links", isDark = isDark)
                    Spacer(Modifier.height(4.dp))
                }
                items(settingResults, key = { "search_sett_" + it }) { kw ->
                    SettingsKeywordRow(keyword = kw, isDark = isDark, onClick = {
                        val actionIntent = when (kw) {
                            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            "airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                            else -> Intent(Settings.ACTION_SETTINGS)
                        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(actionIntent) } catch (e: Exception) {}
                    })
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryChip(label: String, isDark: Boolean) {
    Text(
        label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = DS.accentStart,
        modifier = Modifier
            .background(DS.accentStart.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsKeywordRow(keyword: String, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
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
            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(keyword.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Normal, maxLines = 1)
            Text("System Settings Link", fontSize = 10.sp, color = textPrimary.copy(alpha = 0.35f), maxLines = 1)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Collapsible Group Header
// ─────────────────────────────────────────────────────────
@Composable
private fun CollapsibleGroupHeader(
    letter: String,
    label: String?,
    appCount: Int,
    isDark: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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
            modifier = Modifier.size(14.dp)
        )
        Text(
            label ?: letter,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            "($appCount)",
            fontSize = 10.sp,
            color = textPrimary.copy(alpha = 0.4f)
        )
    }
}

// ─────────────────────────────────────────────────────────
// App Grid Layout
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
    isBuiltIn: Boolean,
    category: AppCategory
) {
    if (apps.isEmpty()) return
    when (layoutPrefs.mode) {
        LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID, LayoutMode.MOBILE_HOME -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layoutPrefs.columns),
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(apps, key = { "grid_" + category.name + "_" + it.packageName }) { app ->
                    AnimatedPinnedIcon(
                        app = app,
                        isDark = isDark,
                        editMode = editMode,
                        onClick = { onAppClick(app) },
                        onUnpin = { onAppUnpin(app) },
                        onPinToTaskbar = { onAppPin(app) },
                        isPinnedToTaskbar = (category == AppCategory.PINNED),
                        iconSize = layoutPrefs.iconSize,
                        showLabel = layoutPrefs.showLabels,
                        category = category
                    )
                }
            }
        }
        LayoutMode.LIST_VIEW -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                apps.forEach { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = { onAppClick(app) },
                        onPinToStart = { onAppPin(app) },
                        onPinToTaskbar = { onAppPin(app) },
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
                items(apps, key = { "horiz_" + category.name + "_" + it.packageName }) { app ->
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
                items(apps, key = { "fav_" + category.name + "_" + it.packageName }) { app ->
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
                modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(apps, key = { "builtin_grid_" + it.first }) { (name, icon, screen) ->
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
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                apps.forEach { (name, icon, screen) ->
                    BuiltInAppListRow(name = name, icon = icon, isDark = isDark, onClick = { onAppClick(screen) })
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
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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
                    Icons.Default.Close, "Clear",
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp).clickable { onQueryChange("") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Animated Pinned Icon
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
    category: AppCategory = AppCategory.PINNED
) {
    val wobble by animateFloatAsState(
        targetValue = if (editMode) -2.5f else 0f,
        animationSpec = if (editMode) infiniteRepeatable(tween(300), RepeatMode.Reverse) else tween(150),
        label = "wobble"
    )
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    Box(contentAlignment = Alignment.TopEnd) {
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
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Box(modifier = Modifier.size(iconSize.dp), contentAlignment = Alignment.Center) {
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
            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    app.name, fontSize = 10.sp, color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (editMode && category == AppCategory.PINNED) {
            Box(
                modifier = Modifier
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(15.dp)
                    .background(DS.badgeRed, CircleShape)
                    .clickable { onUnpin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Open application") },
                onClick = { showMenu = false; onClick() }
            )
            DropdownMenuItem(
                text = { Text(if (isPinnedToTaskbar) "Unpin from Start" else "Pin to Start") },
                onClick = { showMenu = false; if (isPinnedToTaskbar) onUnpin() else onPinToTaskbar() }
            )
        }
    }
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
                name, fontSize = 10.sp, color = (if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight).copy(alpha = 0.8f),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Horizontal App Card
// ─────────────────────────────────────────────────────────
@Composable
private fun HorizontalAppCard(app: AppInfo, isDark: Boolean, onClick: () -> Unit, onPin: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight
    Box {
        Row(
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(cardBg)
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
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
                Text(app.name, fontSize = 9.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Pin to start layer") }, onClick = { showMenu = false; onPin() })
        }
    }
}

@Composable
private fun CompactAppIcon(app: AppInfo, isDark: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(DS.chipCorner))
            .background(if (isDark) DS.surfaceDark else DS.surfaceLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (app.icon != null) {
            val bmp = remember(app.packageName) { app.icon!!.toBitmap().asImageBitmap() }
            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Apps, null, tint = DS.accentStart, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// All Apps Row
// ─────────────────────────────────────────────────────────
@Composable
private fun AllAppsRow(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit,
    onPinToStart: () -> Unit,
    onPinToTaskbar: () -> Unit,
    category: AppCategory = AppCategory.ALL
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
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
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Pin to Start Menu") }, onClick = { showMenu = false; onPinToStart() })
            DropdownMenuItem(text = { Text("Pin to Taskbar") }, onClick = { showMenu = false; onPinToTaskbar() })
        }
    }
}

@Composable
private fun BuiltInAppListRow(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
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
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(DS.sectionCorner)).background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("System Action Component", fontSize = 10.sp, color = textPrimary.copy(alpha = 0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────
// RECOMMENDED SECTION
// ─────────────────────────────────────────────────────────
@Composable
private fun RecommendedSection(isDark: Boolean, viewModel: LauncherViewModel, isExpanded: Boolean, context: Context) {
    var recentFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            recentFiles = getRecentFiles(context)
            permissionDenied = false
        } catch (e: SecurityException) {
            permissionDenied = true
        }
    }

    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    if (permissionDenied) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(DS.badgeRed.copy(alpha = 0.08f), RoundedCornerShape(DS.sectionCorner))
                .border(0.5.dp, DS.badgeRed.copy(alpha = 0.3f), RoundedCornerShape(DS.sectionCorner))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Storage Permission required to see Recommended Items", color = DS.badgeRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        return
    }

    if (recentFiles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
            contentAlignment = Alignment.Center
        ) {
            Text("No recent documents found", color = textPrimary.copy(alpha = 0.25f), fontSize = 11.sp, letterSpacing = 0.3.sp)
        }
        return
    }

    val itemRows = recentFiles.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemRows.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { file ->
                    Box(modifier = Modifier.weight(1f)) {
                        RecentCard(
                            title = file.name,
                            subtitle = formatFileSize(file.length()),
                            extension = file.extension,
                            filePath = file.absolutePath,
                            isDark = isDark,
                            onClick = {
                                try {
                                    val uri = Uri.fromFile(file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "*/*")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Recent Card
// ─────────────────────────────────────────────────────────
@Composable
private fun RecentCard(
    title: String,
    subtitle: String,
    extension: String,
    filePath: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val cardBg = if (isDark) DS.surfaceDark else DS.surfaceLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else cardBg)
            .border(0.5.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val vectorIcon = remember(filePath) { getFileIcon(extension) }
        Box(
            modifier = Modifier.size(28.dp).background(DS.accentStart.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(vectorIcon, null, tint = DS.accentStart, modifier = Modifier.size(16.dp))
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
private fun QuickActionsStrip(isDark: Boolean, context: Context) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val actions = listOf(
        Pair(Icons.Default.Wifi, "Wi-Fi"),
        Pair(Icons.Default.Bluetooth, "Bluetooth"),
        Pair(Icons.Default.AirplanemodeActive,"Airplane"),
        Pair(Icons.Default.DoNotDisturb, "Focus"),
        Pair(Icons.Default.Brightness6, "Brightness"),
        Pair(Icons.Default.VolumeUp, "Sound")
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.forEach { (icon, label) ->
            var active by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DS.chipCorner))
                    .background(if (active) DS.accentStart else if (isDark) DS.surfaceDark else DS.surfaceLight)
                    .border(0.5.dp, if (active) DS.accentEnd else if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.chipCorner))
                    .clickable {
                        active = !active
                        val targetIntent = when (label) {
                            "Wi-Fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                            "Bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            "Airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                            "Brightness", "Sound" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            else -> null
                        }
                        targetIntent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try { context.startActivity(it) } catch (e: Exception) {}
                        }
                    }
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
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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
        Text(label, fontSize = 10.sp, color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp)
    }
}

// ─────────────────────────────────────────────────────────
// BOTTOM USER BAR & POWER SELECTION
// ─────────────────────────────────────────────────────────
@Composable
private fun BottomUserBar(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean
) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
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


    }
}

@Composable
private fun PowerMenuItem(label: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val iconTint = if (label == "Shut Down") DS.badgeRed else DS.accentStart

    Row(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(DS.chipCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
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
        app = app, isDark = isDark, editMode = false, onClick = onClick,
        onUnpin = {}, onPinToTaskbar = onPinToTaskbar, isPinnedToTaskbar = isPinnedToTaskbar
    )
}

@Composable
fun BuiltInAppIcon(name: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    AnimatedBuiltInIcon(name = name, icon = icon, isDark = isDark, editMode = false, onClick = onClick)
}

private fun formatFileSize(size: Long): String {
    return when {
        size <= 0              -> "0 B"
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
                if (file.exists() && file.isFile) {
                    files.add(file)
                    count++
                }
            }
        }
    } catch (e: Exception) {}
    return files
}