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
import androidx.compose.animation.ExperimentalAnimationApi
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
// Icons come from the shared FluentIcon object (FluentIcon.kt), which wraps
// the io.github.niyajali:fluentui-system-icons Compose Multiplatform library.
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
// All icons used by this file are centralized in the `FluentIcon` object —
// if a name doesn't exist in the version you pull in, Android Studio's
// FluentIcons.Regular.… autocomplete will show the closest real name.
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
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
import io.github.norbertweb.bluebird.ui.theme.LocalIsDarkTheme
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
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
// Pinned built-in (system) apps — lets "Add to Start" on a system app
// (Settings, Terminal, etc.) actually put it in the Pinned tab, the
// same way pinning a regular installed app does.
// ─────────────────────────────────────────────────────────
private const val PINNED_BUILTIN_KEY = "pinned_builtin_apps"

internal fun getPinnedBuiltInAppNames(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    return prefs.getStringSet(PINNED_BUILTIN_KEY, emptySet()) ?: emptySet()
}

internal fun pinBuiltInApp(context: Context, name: String) {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    val updated = (prefs.getStringSet(PINNED_BUILTIN_KEY, emptySet()) ?: emptySet()).toMutableSet()
    updated.add(name)
    prefs.edit().putStringSet(PINNED_BUILTIN_KEY, updated).apply()
}

internal fun unpinBuiltInApp(context: Context, name: String) {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    val updated = (prefs.getStringSet(PINNED_BUILTIN_KEY, emptySet()) ?: emptySet()).toMutableSet()
    updated.remove(name)
    prefs.edit().putStringSet(PINNED_BUILTIN_KEY, updated).apply()
}

// ─────────────────────────────────────────────────────────
// Persisted Start Menu size + layout preference
// (fixes: size choice previously reset every time the Start Menu closed)
// ─────────────────────────────────────────────────────────
private const val START_MENU_PREFS = "start_menu_layout_prefs"

private fun getSavedSizeMode(context: Context): StartMenuSizeMode {
    val prefs = context.getSharedPreferences(START_MENU_PREFS, Context.MODE_PRIVATE)
    // Default changed from COMPACT to EXPANDED per updated design spec.
    val name = prefs.getString("size_mode", StartMenuSizeMode.EXPANDED.name)
    return runCatching { StartMenuSizeMode.valueOf(name ?: "COMPACT") }
        .getOrDefault(StartMenuSizeMode.EXPANDED)
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
// `DS` (design tokens) now lives in FluentDesignSystem.kt and is shared with
// SearchOverlay.kt — this used to be a private copy that had drifted from
// SearchOverlay's own hardcoded colors/corner-radius, which is why the two
// surfaces looked like different apps. Glass alpha is now driven by the
// user's opacity setting (DS.glass / DS.surfaceGlass) instead of a fixed
// 0xCC baked into the color literal.

// Icon lookups now come from the shared `FluentIcon` object in FluentIcon.kt,
// used by every UI file in this package (Start Menu, Desktop, TaskBar, Settings).

private enum class StartMenuTab { PINNED, ALL_APPS, RECENT, SEARCH }

private val SIZE_MODE_LABELS = listOf(
    StartMenuSizeMode.COMPACT to "Compact",
    StartMenuSizeMode.EXPANDED to "Expanded",
    StartMenuSizeMode.FULLSCREEN to "Full Screen"
)

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
internal val builtInApps = listOf(
    Triple("Settings", FluentIcon.Settings, LauncherScreen.SETTINGS),
    Triple("Calculator", FluentIcon.Calculator, LauncherScreen.CALCULATOR),
    Triple("Calendar", FluentIcon.Calendar, LauncherScreen.CALENDAR),
    Triple("Bluebird Store", FluentIcon.Moon, LauncherScreen.BLUEBIRD_STORE),
    Triple("Word Impress", FluentIcon.DocumentText, LauncherScreen.WORD_IMPRESS),

    Triple("Files",        FluentIcon.Folder,       LauncherScreen.FILE_EXPLORER),
    Triple("Browser",      FluentIcon.Globe,        LauncherScreen.BROWSER),

    Triple("Photos",       FluentIcon.ImageMultiple, LauncherScreen.PHOTOS),
    Triple("Tasks",        FluentIcon.TaskList,      LauncherScreen.TASK_MANAGER),


    Triple("Media Player", FluentIcon.PlayCircle, LauncherScreen.MEDIA_PLAYER),
    Triple("Recycle Bin",  FluentIcon.Delete,        LauncherScreen.RECYCLE_BIN),
    Triple("Image Viewer", FluentIcon.Image,         LauncherScreen.IMAGE_VIEWER),
    Triple("Text Editor",  FluentIcon.TextFont,      LauncherScreen.PremiumTextEditorScreen),

    Triple("Terminal", FluentIcon.Console,          LauncherScreen.TERMINAL),
    Triple("Web App Manager",  FluentIcon.Globe,    LauncherScreen.WEB_APP_MANAGER)


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

    // Installed-app discovery is intentionally lazy: the launcher shell can start
    // without PackageManager/icon enumeration. Start it only when Start Menu is visible.
    LaunchedEffect(Unit) { viewModel.ensureInstalledAppsLoaded() }

    var activeTab by remember { mutableStateOf(StartMenuTab.PINNED) }
    // Persisted (not just `remember`ed) so the size survives the Start Menu being closed/reopened.
    var sizeMode by remember { mutableStateOf(getSavedSizeMode(context)) }
    var editMode by remember { mutableStateOf(false) }
    var layoutPrefs by remember { mutableStateOf(LayoutPreferences(mode = getSavedLayoutMode(context))) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showOpacityMenu by remember { mutableStateOf(false) }
    // User-controllable surface transparency (Settings > Personalization).
    // Previously there was no variable here at all — every glass alpha was
    // a fixed value baked into the DS color literal.
    val (opacity, setOpacity) = rememberOpacity(context)

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

    // Bluebird no longer carries its own light/dark toggle — it follows the
    // device's system theme, read from the single source of truth in
    // Theme.kt (bluebirdTheme() → LocalIsDarkTheme) instead of asking the
    // system independently, so every screen agrees with every other screen.
    val isDark       = LocalIsDarkTheme.current
    val bgColor      = DS.glass(isDark, opacity) // live opacity setting, not a fixed 0xCC alpha
    val borderColor  = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    var localSearchQuery by rememberSaveable { mutableStateOf(uiState.searchQuery) }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != localSearchQuery) localSearchQuery = uiState.searchQuery
    }

    LaunchedEffect(localSearchQuery) {
        delay(120)
        if (localSearchQuery != uiState.searchQuery) {
            viewModel.updateSearchQuery(localSearchQuery)
        }
    }

    Box(
        modifier = modifier
            .then(
                if (isFullscreen) Modifier.fillMaxSize()
                else Modifier.width(menuWidth).height(menuHeight)
            )
            // Was Modifier.shadow(24.dp, ...) — Android's default system shadow
            // (small blur radius, fairly dark). softShadow() draws a much wider,
            // lower-opacity blur instead, which reads as "floating" rather than
            // "card with a dropshadow" — see SoftUI.kt.
            .then(
                if (isFullscreen) Modifier
                else Modifier.softShadow(cornerRadius = cornerRadius, blurRadius = 40.dp, alpha = 0.28f, offsetY = 14.dp)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .background(
                // Windows 11 Mica/Acrylic materials carry a faint light-to-transparent
                // sheen near the top edge — this reproduces that without extra draw calls.
                Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    endY = 220f
                )
            )
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
                    query = localSearchQuery,
                    onQueryChange = {
                        localSearchQuery = it
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
                                    LayoutMode.COMPACT_GRID, LayoutMode.LARGE_GRID -> FluentIcon.Grid
                                    LayoutMode.LIST_VIEW -> FluentIcon.List
                                    else -> FluentIcon.Apps
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
                        LayoutMode.entries.forEach { mode ->
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
                                    Icon(FluentIcon.Checkmark, null, tint = DS.accentStart, modifier = Modifier.size(14.dp))
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
                                        FluentIcon.PhoneAndroid,
                                        null,
                                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Personalization: surface transparency — previously there was no
                // control for this anywhere; every glass alpha was hardcoded.
                Box {
                    IconToggleButton(
                        checked = showOpacityMenu,
                        onCheckedChange = { showOpacityMenu = it },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(DS.chipCorner))
                            .background(
                                if (showOpacityMenu) DS.accentStart.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            FluentIcon.Color,
                            contentDescription = "Transparency",
                            tint = if (showOpacityMenu) DS.accentStart else textPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOpacityMenu,
                        onDismissRequest = { showOpacityMenu = false },
                        modifier = Modifier
                            .width(220.dp)
                            .background(
                                if (isDark) DS.surfaceDark else DS.glassLight,
                                RoundedCornerShape(DS.sectionCorner)
                            )
                            .border(1.dp, borderColor, RoundedCornerShape(DS.sectionCorner))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                "Transparency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.material3.Slider(
                                value = opacity,
                                onValueChange = { setOpacity(it) },
                                valueRange = DS.OPACITY_MIN..DS.OPACITY_MAX,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = DS.accentStart,
                                    activeTrackColor = DS.accentStart
                                )
                            )
                            Text(
                                "${(opacity * 100).toInt()}% opaque",
                                fontSize = 10.sp,
                                color = textPrimary.copy(alpha = 0.5f)
                            )
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
                                        StartMenuSizeMode.COMPACT    -> FluentIcon.FullScreenMax
                                        StartMenuSizeMode.EXPANDED   -> FluentIcon.Resize
                                        StartMenuSizeMode.FULLSCREEN -> FluentIcon.FullScreenMin
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
                            SIZE_MODE_LABELS.forEach { (mode, label) ->
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
                                        Icon(FluentIcon.Checkmark, null, tint = DS.accentStart, modifier = Modifier.size(14.dp))
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

            // Bug fix: this used to be a plain `if (isMobileHome)` — the top
            // search bar could set activeTab = SEARCH while in mobile mode,
            // but nothing here checked activeTab, so search silently did
            // nothing on the phone-style layout. Falling through to the
            // existing tab-content branch (below) when a search is active
            // reuses the same SearchResultsView desktop mode already uses.
            if (isMobileHome && activeTab != StartMenuTab.SEARCH) {
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

                // ── Content — switches instantly, no transition animation ──
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
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
                                query = localSearchQuery,
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = isDark,
                                onClearSearch = {
                                    localSearchQuery = ""
                                    activeTab = StartMenuTab.PINNED
                                },
                                context = context,
                                layoutPrefs = layoutPrefs
                            )
                    }
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
            val indicatorColor = if (isActive) DS.accentStart else Color.Transparent
            val indicatorWidth = if (isActive) 40.dp else 16.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabChange(tab) }
                    .padding(end = 24.dp, bottom = 0.dp)
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isActive) DS.accentStart else textPrimary.copy(alpha = 0.55f),
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.dp)
                        .background(indicatorColor, RoundedCornerShape(1.dp))
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
                FluentIcon.Apps, null,
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
    // Only the "Pinned" tab now exists on this screen — no System tray, no
    // Recommended (recent files) tray. A system app reaches this list the
    // same way a regular app does: "Add to Start" from All Apps.
    var pinnedBuiltInNames by remember { mutableStateOf(getPinnedBuiltInAppNames(context)) }
    val pinnedBuiltInApps = remember(pinnedBuiltInNames) {
        builtInApps.filter { it.first in pinnedBuiltInNames }
    }

    fun addToDesktop(label: String, screen: LauncherScreen) {
        val dir = File(android.os.Environment.getExternalStorageDirectory(), "Desktop").apply { mkdirs() }
        var file = File(dir, "$label.desktop")
        var n = 2
        while (file.exists()) { file = File(dir, "$label ($n).desktop"); n++ }
        runCatching { file.writeText("type=app\nlabel=$label\nbluebirdScreen=${screen.name}\n") }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionHeader(
                title = "Pinned",
                isDark = isDark,
                rightContent = {
                    CompactActionChip(
                        label = if (editMode) "Done" else "Edit",
                        icon  = if (editMode) FluentIcon.Checkmark else FluentIcon.Edit,
                        isDark = isDark,
                        onClick = onEditModeToggle
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            if (pinnedApps.isEmpty() && pinnedBuiltInApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(DS.sectionCorner))
                        .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No pinned apps — long-press any app and choose Add to Start",
                        color = (if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight).copy(alpha = 0.25f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (pinnedApps.isNotEmpty()) {
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
                if (pinnedBuiltInApps.isNotEmpty()) {
                    if (pinnedApps.isNotEmpty()) Spacer(Modifier.height(6.dp))
                    BuiltInAppGridLayout(
                        apps       = pinnedBuiltInApps,
                        isDark     = isDark,
                        editMode   = editMode,
                        layoutPrefs = layoutPrefs,
                        onAppClick = { screen -> viewModel.openWindow(screen) },
                        onAddToDesktop = { label, screen -> addToDesktop(label, screen) },
                        isPinnedToStart = { name -> name in pinnedBuiltInNames },
                        onTogglePinToStart = { name ->
                            if (name in pinnedBuiltInNames) unpinBuiltInApp(context, name) else pinBuiltInApp(context, name)
                            pinnedBuiltInNames = getPinnedBuiltInAppNames(context)
                        },
                        category   = AppCategory.SYSTEM
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
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
    val sortedApps = remember(uiState.installedApps) {
        uiState.installedApps.sortedBy { it.name.lowercase() }
    }
    val grouped = remember(sortedApps) {
        sortedApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()
    val jumpLetters = remember(grouped) { grouped.keys.sorted() }
    var expandedGroups by remember(grouped) { mutableStateOf(grouped.keys.toSet()) }
    var pinnedBuiltInNames by remember { mutableStateOf(getPinnedBuiltInAppNames(context)) }
    fun addBuiltInToDesktop(label: String, screen: LauncherScreen) {
        val dir = File(android.os.Environment.getExternalStorageDirectory(), "Desktop").apply { mkdirs() }
        var file = File(dir, "$label.desktop")
        var n = 2
        while (file.exists()) { file = File(dir, "$label ($n).desktop"); n++ }
        runCatching { file.writeText("type=app\nlabel=$label\nbluebirdScreen=${screen.name}\n") }
    }

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
                    BuiltInAppListRow(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        onClick = { viewModel.openWindow(screen) },
                        onAddToDesktop = { addBuiltInToDesktop(name, screen) },
                        isPinnedToStart = name in pinnedBuiltInNames,
                        onTogglePinToStart = {
                            if (name in pinnedBuiltInNames) unpinBuiltInApp(context, name) else pinBuiltInApp(context, name)
                            pinnedBuiltInNames = getPinnedBuiltInAppNames(context)
                        }
                    )
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
    // PackageManager and preference reads can be surprisingly expensive on some devices.
    // Do not perform them synchronously during Start Menu composition.
    val recentAndFrequent = produceState(
        initialValue = emptyList<AppInfo>() to emptyList<AppInfo>(),
        key1 = uiState.installedApps
    ) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val recent = uiState.installedApps.sortedByDescending { app ->
                try {
                    pm.getPackageInfo(app.packageName, 0).firstInstallTime
                } catch (_: Exception) {
                    0L
                }
            }.take(12)

            val openCounts = uiState.installedApps.associate { app ->
                app.packageName to getAppOpenCount(context, app.packageName)
            }
            val frequent = uiState.installedApps
                .mapNotNull { app ->
                    val count = openCounts[app.packageName] ?: 0
                    if (count > 0) app to count else null
                }
                .sortedByDescending { it.second }
                .take(6)
                .map { it.first }

            recent to frequent
        }
    }
    val recentApps = recentAndFrequent.value.first
    val frequentApps = recentAndFrequent.value.second

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

private val SEARCH_SETTING_KEYWORDS = listOf(
    "wifi", "bluetooth", "brightness", "sound", "display", "theme",
    "language", "notification", "battery", "storage", "account",
    "password", "lock", "airplane"
)

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

    val normalizedQuery = remember(query) { query.trim().lowercase() }
    val appResults = remember(uiState.installedApps, normalizedQuery) {
        if (normalizedQuery.isEmpty()) emptyList()
        else uiState.installedApps.filter {
            it.name.contains(normalizedQuery, ignoreCase = true) ||
                    it.packageName.contains(normalizedQuery, ignoreCase = true)
        }
    }
    val systemResults = remember(normalizedQuery) {
        if (normalizedQuery.isEmpty()) emptyList()
        else builtInApps.filter { it.first.contains(normalizedQuery, ignoreCase = true) }
    }
    val settingResults = remember(normalizedQuery) {
        if (normalizedQuery.isEmpty()) emptyList()
        else SEARCH_SETTING_KEYWORDS.filter { it.contains(normalizedQuery, ignoreCase = true) }
    }

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
                .clip(RoundedCornerShape(DS.tileCorner))
                .background(DS.accentStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(FluentIcon.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
            if (isExpanded) FluentIcon.ChevronUp else FluentIcon.ChevronDown,
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
    apps: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, LauncherScreen>>,
    isDark: Boolean,
    editMode: Boolean,
    layoutPrefs: LayoutPreferences,
    onAppClick: (LauncherScreen) -> Unit,
    onAddToDesktop: (String, LauncherScreen) -> Unit = { _, _ -> },
    isPinnedToStart: (String) -> Boolean = { false },
    onTogglePinToStart: (String) -> Unit = {},
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
                        onAddToDesktop = { onAddToDesktop(name, screen) },
                        isPinnedToStart = isPinnedToStart(name),
                        onTogglePinToStart = { onTogglePinToStart(name) },
                        iconSize = layoutPrefs.iconSize,
                        showLabel = layoutPrefs.showLabels
                    )
                }
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                apps.forEach { (name, icon, screen) ->
                    BuiltInAppListRow(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        onClick = { onAppClick(screen) },
                        onAddToDesktop = { onAddToDesktop(name, screen) },
                        isPinnedToStart = isPinnedToStart(name),
                        onTogglePinToStart = { onTogglePinToStart(name) }
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
                FluentIcon.Search, null,
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
                    FluentIcon.Dismiss, "Clear",
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
                val cachedBmp = rememberAppIconBitmap(app)
                if (cachedBmp != null) {
                    Image(bitmap = cachedBmp, contentDescription = app.name, modifier = Modifier.size((iconSize - 4).dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size((iconSize - 4).dp)
                            .clip(RoundedCornerShape(DS.sectionCorner))
                            .background(if (isDark) DS.surfaceDark else DS.surfaceLight)
                            .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FluentIcon.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    app.name, fontSize = 10.sp, color = textColor.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth()
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
                Icon(FluentIcon.Subtract, null, tint = Color.White, modifier = Modifier.size(10.dp))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    onAddToDesktop: () -> Unit = {},
    isPinnedToStart: Boolean = false,
    onTogglePinToStart: () -> Unit = {},
    iconSize: Int = 38,
    showLabel: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(DS.sectionCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        // Custom SVG-derived icons (see BuiltInAppIcons.kt) already contain
        // their own colored rounded-square background, so they render
        // full-bleed here. Apps without a custom icon yet fall back to the
        // original accent-tile + white Fluent glyph treatment.
        val customIconResId = rememberBuiltInIconResourceId(name)
        if (customIconResId != 0) {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(RoundedCornerShape(DS.sectionCorner)),
                contentAlignment = Alignment.Center
            ) {
                BuiltInAppIcon(
                    appName = name,
                    fallback = icon,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(RoundedCornerShape(DS.sectionCorner))
                    .background(DS.accentStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                name, fontSize = 10.sp, color = (if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight).copy(alpha = 0.9f),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth()
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onClick() })
            DropdownMenuItem(text = { Text("Add to desktop") }, onClick = { showMenu = false; onAddToDesktop() })
            DropdownMenuItem(
                text = { Text(if (isPinnedToStart) "Remove from Start" else "Add to Start") },
                onClick = { showMenu = false; onTogglePinToStart() }
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
                    val cachedBmp = rememberAppIconBitmap(app)
                if (cachedBmp != null) {
                    Image(bitmap = cachedBmp, contentDescription = app.name, modifier = Modifier.size(28.dp))
                } else {
                    Icon(FluentIcon.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
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
        val cachedBmp = rememberAppIconBitmap(app)
                if (cachedBmp != null) {
                    Image(bitmap = cachedBmp, contentDescription = app.name, modifier = Modifier.size(24.dp))
                } else {
                    Icon(FluentIcon.Apps, null, tint = DS.accentStart, modifier = Modifier.size(16.dp))
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
                val cachedBmp = rememberAppIconBitmap(app)
                if (cachedBmp != null) {
                    Image(bitmap = cachedBmp, contentDescription = app.name, modifier = Modifier.size(28.dp))
                } else {
                    Icon(FluentIcon.Apps, null, tint = DS.accentStart, modifier = Modifier.size(18.dp))
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
private fun BuiltInAppListRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    onClick: () -> Unit,
    onAddToDesktop: (() -> Unit)? = null,
    isPinnedToStart: Boolean = false,
    onTogglePinToStart: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val hasMenu = onAddToDesktop != null || onTogglePinToStart != null
    // Custom SVG-derived icons (BuiltInAppIcons.kt) previously weren't
    // consulted here at all — this row always drew the plain Fluent glyph
    // on a flat accent tile, which is why system apps' custom icons never
    // showed up in All Apps. Now it checks for a custom icon the same way
    // AnimatedBuiltInIcon does.
    val customIconResId = rememberBuiltInIconResourceId(name)
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
                        onLongPress = { if (hasMenu) showMenu = true }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customIconResId != 0) {
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(DS.tileCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    BuiltInAppIcon(appName = name, fallback = icon, tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            } else {
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(DS.tileCorner)).background(DS.accentStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("System app", fontSize = 10.sp, color = textPrimary.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (hasMenu) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onClick() })
                onAddToDesktop?.let { addToDesktop ->
                    DropdownMenuItem(text = { Text("Add to desktop") }, onClick = { showMenu = false; addToDesktop() })
                }
                onTogglePinToStart?.let { togglePin ->
                    DropdownMenuItem(
                        text = { Text(if (isPinnedToStart) "Remove from Start" else "Add to Start") },
                        onClick = { showMenu = false; togglePin() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// (Recommended / recent-files tray and the Wi-Fi/Bluetooth/etc. Quick
// Actions strip have been removed entirely — Pinned is the only tray now,
// and the quick-action toggles were non-functional placeholders that
// didn't reflect real system state.)
// ─────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────
// Compact Action Chip
// ─────────────────────────────────────────────────────────
@Composable
private fun CompactActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isDark: Boolean, onClick: () -> Unit) {
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
        Icon(imageVector = icon, contentDescription = null, tint = DS.accentStart.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
        Text(label, fontSize = 10.sp, color = textColor.copy(alpha = 0.85f), fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
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

        // Power button — extracted to PowerMenu.kt (PowerMenuButton) so it can
        // be reused elsewhere without duplicating the sleep/restart/shutdown logic.
        PowerMenuButton(isDark = isDark, textPrimary = textPrimary)
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

// Renamed from `BuiltInAppIcon` to avoid colliding with the new
// BuiltInAppIcon(appName, fallback, tint, modifier) in BuiltInAppIcons.kt.
// If other files in the project still call the old `BuiltInAppIcon(name, icon, isDark, onClick)`
// signature, update those call sites to this name.
@Composable
fun LegacyBuiltInAppIconCompat(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isDark: Boolean, onClick: () -> Unit) {
    AnimatedBuiltInIcon(name = name, icon = icon, isDark = isDark, editMode = false, onClick = onClick)
}

// (formatFileSize / getFileIcon / getRecentFiles removed along with the
// Recommended tray — they had no other caller.)