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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AppsOutage
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.runtime.collectAsState
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
// Design Tokens
// ─────────────────────────────────────────────────────────
private object DS {
    // Glass / surface
    val glassDark   = Color(0xCC1A1A2E)
    val glassLight  = Color(0xE6F5F5FA)
    val borderDark  = Color(0x25FFFFFF)
    val borderLight = Color(0x18000000)

    // Accent palette — vibrant indigo-to-cyan gradient
    val accentStart = Color(0xFF6C63FF)
    val accentMid   = Color(0xFF48CAE4)
    val accentEnd   = Color(0xFF00B4D8)

    // Hover states
    val hoverDark  = Color(0x14FFFFFF)
    val hoverLight = Color(0x0A000000)

    // Pill badge
    val badgeRed   = Color(0xFFFF4E6A)

    // Sizing
    val menuWidthCompact  = 560.dp
    val menuWidthExpanded = 780.dp
    val menuHeightCompact = 640.dp
    val menuHeightExpanded = 820.dp
    val cornerRadius = 16.dp
    val sectionCorner = 12.dp

    // Gradients
    fun accentBrush() = Brush.linearGradient(
        colors = listOf(accentStart, accentMid, accentEnd),
        start = Offset(0f, 0f), end = Offset(500f, 500f)
    )
}

// ─────────────────────────────────────────────────────────
// Tab enum
// ─────────────────────────────────────────────────────────
private enum class StartMenuTab { PINNED, ALL_APPS, SEARCH }

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

    // Animate menu size
    val menuWidth by animateDpAsState(
        targetValue = if (isExpanded) DS.menuWidthExpanded else DS.menuWidthCompact,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f), label = "width"
    )
    val menuHeight by animateDpAsState(
        targetValue = if (isExpanded) DS.menuHeightExpanded else DS.menuHeightCompact,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f), label = "height"
    )

    val isDark = uiState.isDarkTheme
    val bgColor = if (isDark) DS.glassDark else DS.glassLight
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val textSecondary = textPrimary.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .width(menuWidth)
            .height(menuHeight)
            .clip(RoundedCornerShape(DS.cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(DS.cornerRadius))
            .shadow(elevation = 48.dp, shape = RoundedCornerShape(DS.cornerRadius), clip = false)
    ) {
        // Subtle inner shimmer line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            Spacer(Modifier.height(18.dp))

            // ── Top Bar: Search + Expand ──
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

                // Expand/Collapse toggle
                IconToggleButton(
                    checked = isExpanded,
                    onCheckedChange = { isExpanded = it },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) DS.hoverDark else DS.hoverLight)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
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
                    StartMenuTab.PINNED -> PinnedView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        isExpanded = isExpanded,
                        editMode = editMode,
                        onEditModeToggle = { editMode = !editMode },
                        onSwitchAllApps = { activeTab = StartMenuTab.ALL_APPS },
                        context = context
                    )
                    StartMenuTab.ALL_APPS -> AllAppsView(
                        uiState = uiState,
                        viewModel = viewModel,
                        isDark = isDark,
                        isExpanded = isExpanded,
                        context = context
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
                        context = context
                    )
                }
            }

            // ── Bottom User Bar ──
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = if (isDark) Color(0x18FFFFFF) else Color(0x12000000))
            Spacer(Modifier.height(10.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0x0DFFFFFF) else Color(0x08000000))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(
            StartMenuTab.PINNED to "Pinned",
            StartMenuTab.ALL_APPS to "All Apps"
        ).forEach { (tab, label) ->
            val isActive = activeTab == tab
            val bgAlpha by animateFloatAsState(if (isActive) 1f else 0f, label = "tabBg")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive)
                            if (isDark) Color(0xFF2A2A3E) else Color.White
                        else Color.Transparent
                    )
                    .clickable { onTabChange(tab) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(DS.accentBrush(), CircleShape)
                        )
                    }
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) textPrimary else textPrimary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Pinned View
// ─────────────────────────────────────────────────────────
@Composable
private fun PinnedView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    isExpanded: Boolean,
    editMode: Boolean,
    onEditModeToggle: () -> Unit,
    onSwitchAllApps: () -> Unit,
    context: Context
) {
    val columns = if (isExpanded) 8 else 6
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Pinned header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pinned",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Edit/Done toggle
                    CompactActionChip(
                        label = if (editMode) "Done" else "Edit",
                        icon = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                        isDark = isDark,
                        onClick = onEditModeToggle
                    )
                    CompactActionChip(
                        label = "All apps",
                        icon = Icons.Default.AppsOutage,
                        isDark = isDark,
                        onClick = onSwitchAllApps
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Pinned Apps Grid ──
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                items(uiState.pinnedTaskbarApps) { app ->
                    AnimatedPinnedIcon(
                        app = app,
                        isDark = isDark,
                        editMode = editMode,
                        onClick = { viewModel.openApp(context, app) },
                        onUnpin = { viewModel.unpinAppFromTaskbar(app) },
                        onPinToTaskbar = { viewModel.unpinAppFromTaskbar(app) },
                        isPinnedToTaskbar = true
                    )
                }
                items(builtInApps) { (name, icon, screen) ->
                    AnimatedBuiltInIcon(
                        name = name,
                        icon = icon,
                        isDark = isDark,
                        editMode = editMode,
                        onClick = { viewModel.openWindow(screen) }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        // ── Recommended Section ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recommended",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                CompactActionChip(
                    label = "More",
                    icon = Icons.Default.ChevronRight,
                    isDark = isDark,
                    onClick = {}
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            RecommendedSection(
                isDark = isDark,
                viewModel = viewModel,
                isExpanded = isExpanded
            )
        }

        // ── Quick Actions Strip ──
        item {
            Spacer(Modifier.height(16.dp))
            QuickActionsStrip(isDark = isDark)
        }
    }
}

// ─────────────────────────────────────────────────────────
// All Apps View — grouped alphabetically with jump index
// ─────────────────────────────────────────────────────────
@Composable
private fun AllAppsView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    isExpanded: Boolean,
    context: Context
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val sortedApps = uiState.installedApps.sortedBy { it.name.lowercase() }
    val grouped = sortedApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Build flat index list for jump navigation
    val jumpItems = grouped.keys.toList()
    var showJumpIndex by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // App list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            grouped.forEach { (letter, apps) ->
                // Letter header
                item(key = "header_$letter") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(DS.accentBrush(), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                // App rows
                items(apps, key = { it.packageName }) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = { viewModel.openApp(context, app) },
                        onPinToTaskbar = { viewModel.pinAppToTaskbar(app) }
                    )
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
                jumpItems.forEach { letter ->
                    Text(
                        letter.toString(),
                        fontSize = 9.sp,
                        color = DS.accentStart,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            // Scroll to letter group
                            val keys = grouped.keys.toList()
                            val idx = keys.indexOf(letter)
                            if (idx >= 0) scope.launch { listState.animateScrollToItem(idx * 2) }
                        }
                    )
                }
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
    context: Context
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val results = uiState.installedApps.filter {
        it.name.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Results for \"$query\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            TextButton(onClick = onClearSearch) {
                Text("Clear", color = DS.accentStart, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        null,
                        tint = textPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No results found",
                        color = textPrimary.copy(alpha = 0.35f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(results) { app ->
                    AllAppsRow(
                        app = app,
                        isDark = isDark,
                        onClick = { viewModel.openApp(context, app) },
                        onPinToTaskbar = { viewModel.pinAppToTaskbar(app) }
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
    val bgColor = if (isDark) Color(0xFF252535) else Color(0xFFEEEEF5)
    val borderColor = if (isDark) Color(0x30FFFFFF) else Color(0x20000000)
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    // Glow effect when focused
    var isFocused by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(if (isFocused) 1f else 0f, label = "glow")

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = if (isFocused)
                    Brush.linearGradient(listOf(DS.accentStart, DS.accentEnd))
                else
                    Brush.linearGradient(listOf(borderColor, borderColor)),
                shape = RoundedCornerShape(12.dp)
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
                tint = if (isFocused) DS.accentStart else Color(0xFF888899),
                modifier = Modifier.size(16.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = 13.sp
                ),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search apps, files, settings...",
                            color = Color(0xFF888899),
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
                    tint = Color(0xFF888899),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Animated Pinned App Icon (grid style)
// ─────────────────────────────────────────────────────────
@Composable
fun AnimatedPinnedIcon(
    app: AppInfo,
    isDark: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    onPinToTaskbar: () -> Unit,
    isPinnedToTaskbar: Boolean
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    // Wobble in edit mode
    val wobbleAngle by animateFloatAsState(
        targetValue = if (editMode) 3f else 0f,
        animationSpec = if (editMode) infiniteRepeatable(
            tween(300), RepeatMode.Reverse
        ) else tween(200),
        label = "wobble"
    )

    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (pressed) hoverBg else Color.Transparent)
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
            // Icon container with gradient background tile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF2A2A40) else Color(0xFFF0F0F8)),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon!!.toBitmap().asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DS.accentBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apps, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                app.name,
                fontSize = 10.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Edit mode — unpin badge
        if (editMode) {
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
                .background(
                    if (isDark) Color(0xFF1E1E30) else Color(0xFFF8F8FC),
                    RoundedCornerShape(10.dp)
                )
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(10.dp))
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            StyledMenuItem(
                if (isPinnedToTaskbar) "Unpin from taskbar" else "Pin to taskbar",
                Icons.Default.PushPin, isDark
            ) { showMenu = false; onPinToTaskbar() }
            StyledMenuItem("Unpin from Start", Icons.Default.PushPin, isDark, tintAccent = true) {
                showMenu = false; onUnpin()
            }
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
    onClick: () -> Unit
) {
    val wobble by animateFloatAsState(
        targetValue = if (editMode) -3f else 0f,
        animationSpec = if (editMode) infiniteRepeatable(tween(280), RepeatMode.Reverse) else tween(200),
        label = "wobble2"
    )
    var pressed by remember { mutableStateOf(false) }
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) hoverBg else Color.Transparent)
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
                .size(44.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(DS.accentBrush()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, name, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            name,
            fontSize = 10.sp,
            color = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────
// All Apps List Row
// ─────────────────────────────────────────────────────────
@Composable
private fun AllAppsRow(
    app: AppInfo,
    isDark: Boolean,
    onClick: () -> Unit,
    onPinToTaskbar: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (pressed) hoverBg else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isDark) Color(0xFF2A2A40) else Color(0xFFF0F0F8)),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon!!.toBitmap().asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(DS.accentBrush())) {
                        Icon(Icons.Default.Apps, null, tint = Color.White, modifier = Modifier.size(20.dp).align(Alignment.Center))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontSize = 13.sp,
                    color = textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    fontSize = 10.sp,
                    color = textPrimary.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = textPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(
                if (isDark) Color(0xFF1E1E30) else Color(0xFFF8F8FC),
                RoundedCornerShape(10.dp)
            )
        ) {
            StyledMenuItem("Open", Icons.Default.OpenInNew, isDark) { showMenu = false; onClick() }
            StyledMenuItem("Pin to Start", Icons.Default.PushPin, isDark) { showMenu = false }
            StyledMenuItem("Pin to taskbar", Icons.Default.PushPin, isDark) { showMenu = false; onPinToTaskbar() }
            StyledMenuItem("App info", Icons.Default.Info, isDark) { showMenu = false }
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
    isExpanded: Boolean
) {
    val context = LocalContext.current
    val recentFiles = remember { getRecentFiles(context) }
    val recentApps = viewModel.uiState.collectAsState().value.installedApps.take(if (isExpanded) 8 else 5)
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    if (recentFiles.isEmpty() && recentApps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) DS.hoverDark else DS.hoverLight),
            contentAlignment = Alignment.Center
        ) {
            Text("No recent items", color = textPrimary.copy(alpha = 0.3f), fontSize = 12.sp)
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
        items(recentApps) { app ->
            RecentCard(
                title = app.name,
                subtitle = "Recently used",
                iconDrawable = app.icon,
                isDark = isDark,
                onClick = { viewModel.openApp(context, app) }
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
    val cardBg = if (isDark) Color(0xFF1E1E30) else Color(0xFFF0F0FA)

    Row(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(
                1.dp,
                if (isDark) DS.borderDark else DS.borderLight,
                RoundedCornerShape(10.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF2A2A40) else Color(0xFFE4E4F0)),
            contentAlignment = Alignment.Center
        ) {
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap().asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.size(28.dp)
                )
            } else if (icon != null) {
                Icon(icon, null, tint = DS.accentStart, modifier = Modifier.size(20.dp))
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
                color = textPrimary.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Quick Actions Strip (new feature!)
// ─────────────────────────────────────────────────────────
@Composable
private fun QuickActionsStrip(isDark: Boolean) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val context = LocalContext.current

    val actions = listOf(
        Pair(Icons.Default.Wifi, "Wi-Fi"),
        Pair(Icons.Default.Bluetooth, "Bluetooth"),
        Pair(Icons.Default.AirplanemodeActive, "Airplane"),
        Pair(Icons.Default.DoNotDisturb, "Focus"),
        Pair(Icons.Default.Brightness6, "Brightness"),
        Pair(Icons.Default.VolumeUp, "Sound")
    )

    Column {
        Text(
            "Quick Actions",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions.forEach { (icon, label) ->
                var active by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) DS.accentBrush()
                            else if (isDark) Brush.linearGradient(listOf(Color(0xFF1E1E30), Color(0xFF1E1E30)))
                            else Brush.linearGradient(listOf(Color(0xFFF0F0FA), Color(0xFFF0F0FA)))
                        )
                        .border(
                            1.dp,
                            if (active) Color.Transparent else if (isDark) DS.borderDark else DS.borderLight,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { active = !active },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            icon, label,
                            tint = if (active) Color.White else textPrimary.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            fontSize = 8.sp,
                            color = if (active) Color.White else textPrimary.copy(alpha = 0.5f),
                            maxLines = 1,
                            textAlign = TextAlign.Center
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) DS.hoverDark else DS.hoverLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        Text(label, fontSize = 11.sp, color = textColor.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────
// Bottom User Bar
// ─────────────────────────────────────────────────────────
@Composable
private fun BottomUserBar(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User profile pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { viewModel.openWindow(LauncherScreen.SETTINGS) }
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(32.dp)) {
                if (uiState.userProfile.profilePicturePath.isNotEmpty()) {
                    AsyncImage(
                        model = Uri.parse(uiState.userProfile.profilePicturePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(1.5.dp, DS.accentStart, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DS.accentBrush(), CircleShape)
                            .border(1.5.dp, DS.accentStart.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.userProfile.userName.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Online indicator dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4CAF50), CircleShape)
                        .border(1.5.dp, if (isDark) DS.glassDark else DS.glassLight, CircleShape)
                )
            }
            Column {
                Text(
                    uiState.userProfile.userName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Account settings",
                    fontSize = 10.sp,
                    color = DS.accentStart,
                    maxLines = 1
                )
            }
        }

        // Right action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Settings shortcut
            BottomBarIconBtn(Icons.Default.Settings, "Settings", isDark) {
                viewModel.openWindow(LauncherScreen.SETTINGS)
            }
            // Power button
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
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(hoverBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint ?: defaultTint, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────────────────
// Power Menu
// ─────────────────────────────────────────────────────────
@Composable
fun PowerMenu(isDark: Boolean, onAction: (PowerAction) -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF5F5FA)
    val borderColor = if (isDark) DS.borderDark else DS.borderLight
    val textPrimary = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Box(
        modifier = modifier
            .width(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .shadow(32.dp, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(DS.accentBrush(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Text(
                    "Power options",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
            }
            HorizontalDivider(
                color = if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                modifier = Modifier.padding(vertical = 4.dp)
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
    val hoverBg = if (isDark) DS.hoverDark else DS.hoverLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) hoverBg else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, label, tint = textColor, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Normal)
    }
}

// ─────────────────────────────────────────────────────────
// Legacy-compat: original simple icon composables kept for other call sites
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

// ─────────────────────────────────────────────────────────
// StartMenuSearch (public alias)
// ─────────────────────────────────────────────────────────
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
    Triple("Image Viewer",       Icons.Default.Photo,     LauncherScreen.IMAGE_VIEWER),
    Triple("Text Editor",       Icons.Default.TextFields,     LauncherScreen.TextEditorScreen),
)

private val powerOptions = listOf(
    Triple("Sleep",     Icons.Default.BedtimeOff,       PowerAction.SLEEP),
    Triple("Lock",      Icons.Default.Lock,             PowerAction.LOCK),
    Triple("Restart",   Icons.Default.RestartAlt,       PowerAction.RESTART),
    Triple("Shut down", Icons.Default.PowerSettingsNew, PowerAction.SHUTDOWN),
)

// ─────────────────────────────────────────────────────────
// Utility helpers
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
