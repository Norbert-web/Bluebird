package io.github.norbertweb.bluebird.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.AppInfo
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.theme.Win11Colors

// ─────────────────────────────────────────────
// Search filter categories (like the one for Windows 11)
// ─────────────────────────────────────────────
private enum class SearchFilter(val label: String, val icon: ImageVector) {
    All("All", Icons.Default.Apps),
    Apps("Apps", Icons.Default.AppsOutage),
    Web("Web", Icons.Default.Language),
    Settings("Settings", Icons.Default.Settings),
    Files("Files", Icons.Default.FolderOpen)
}

// ─────────────────────────────────────────────
// SearchOverlay
// ─────────────────────────────────────────────
@Composable
fun SearchOverlay(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = uiState.isDarkTheme
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val searchQuery = uiState.searchQuery

    // Expand state: compact vs expanded
    var isExpanded by remember { mutableStateOf(false) }

    // Active filter tab
    var activeFilter by remember { mutableStateOf(SearchFilter.All) }

    // Recent searches
    val recentSearches = remember { mutableStateListOf<String>() }

    // Animated width
    val panelWidth by animateDpAsState(
        targetValue = if (isExpanded) 680.dp else 540.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "panelWidth"
    )

    // Focus requester for auto-focus
    val focusRequester = remember { FocusRequester() }

    fun performWebSearch(query: String) {
        if (query.isNotBlank()) {
            if (!recentSearches.contains(query)) recentSearches.add(0, query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
            context.startActivity(intent)
        }
    }

    // Filtered results based on active tab
    val appResults = viewModel.filteredApps
    val hasResults = appResults.isNotEmpty()

    // Background surface (acrylic style)
    val surfaceBg = if (isDark) Color(0xFF1C1C2A) else Color(0xFFF0F0F5)
    val surfaceBorder = if (isDark) Color(0x28FFFFFF) else Color(0x28000000)

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Surface(
        modifier = modifier
            .width(panelWidth)
            .wrapContentHeight()
            .shadow(32.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = surfaceBg,
        border = BorderStroke(0.5.dp, surfaceBorder)
    ) {
        Column {
            // ── Top gradient accent line ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Win11Colors.AccentBlue,
                                Color(0xFF8855FF),
                                Win11Colors.AccentBlue
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {

                // ── Search bar row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Input field
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF2E2E3E) else Color(0xFFE8E8F0))
                            .border(
                                1.dp,
                                if (searchQuery.isNotEmpty()) Win11Colors.AccentBlue.copy(0.5f) else surfaceBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isEmpty()) Color(0xFF888888) else Win11Colors.AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Win11Colors.AccentBlue),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search apps, web, and more...",
                                        color = textColor.copy(alpha = 0.35f),
                                        fontSize = 13.sp
                                    )
                                }
                                inner()
                            }
                        )

                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textColor.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { viewModel.updateSearchQuery("") }
                            )
                        }
                    }

                    // Expand/collapse toggle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF2E2E3E) else Color(0xFFE8E8F0))
                            .border(0.5.dp, surfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = if (isExpanded) "Compact" else "Expand",
                            tint = textColor.copy(0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Filter tabs (Win11 style) ──
                AnimatedVisibility(visible = searchQuery.isNotEmpty() || isExpanded) {
                    Column {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SearchFilter.values().toList()) { filter ->
                                SearchFilterChip(
                                    filter = filter,
                                    isSelected = activeFilter == filter,
                                    isDark = isDark,
                                    onClick = { activeFilter = filter }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── IDLE STATE (no query) ──
                if (searchQuery.isBlank()) {
                    IdleSearchContent(
                        uiState = uiState,
                        isDark = isDark,
                        textColor = textColor,
                        isExpanded = isExpanded,
                        recentSearches = recentSearches,
                        onAppClick = { viewModel.openApp(context, it) },
                        onQuickSearch = { viewModel.updateSearchQuery(it) },
                        onRecentClick = {
                            viewModel.updateSearchQuery(it)
                            performWebSearch(it)
                        },
                        onClearRecent = { recentSearches.clear() }
                    )
                } else {
                    // ── RESULTS STATE ──
                    SearchResultsContent(
                        query = searchQuery,
                        appResults = appResults,
                        activeFilter = activeFilter,
                        isDark = isDark,
                        textColor = textColor,
                        isExpanded = isExpanded,
                        onAppClick = { viewModel.openApp(context, it) },
                        onWebSearch = { performWebSearch(it) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Idle state (no search query)
// ─────────────────────────────────────────────
@Composable
private fun IdleSearchContent(
    uiState: LauncherUiState,
    isDark: Boolean,
    textColor: Color,
    isExpanded: Boolean,
    recentSearches: List<String>,
    onAppClick: (AppInfo) -> Unit,
    onQuickSearch: (String) -> Unit,
    onRecentClick: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    val gridColumns = if (isExpanded) 8 else 6
    val appCount = if (isExpanded) 16 else 12

    // ── Recommended / Top Apps ──
    SectionHeader(
        title = "Recommended",
        icon = Icons.Default.Star,
        textColor = textColor,
        action = null
    )
    Spacer(Modifier.height(8.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.heightIn(max = if (isExpanded) 200.dp else 160.dp),
        contentPadding = PaddingValues(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false
    ) {
        items(uiState.installedApps.take(appCount)) { app ->
            TopAppItem(app = app, isDark = isDark, onClick = { onAppClick(app) })
        }
    }

    Spacer(Modifier.height(14.dp))

    // ── Quick searches ──
    SectionHeader(title = "Quick search", icon = Icons.Default.Bolt, textColor = textColor, action = null)
    Spacer(Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(quickSearchItems) { (label, icon) ->
            QuickSearchChip(
                label = label,
                icon = icon,
                isDark = isDark,
                onClick = { onQuickSearch(label) }
            )
        }
    }

    // ── Recent searches ──
    if (recentSearches.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        SectionHeader(
            title = "Recent searches",
            icon = Icons.Default.History,
            textColor = textColor,
            action = "Clear" to onClearRecent
        )
        Spacer(Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            recentSearches.take(if (isExpanded) 8 else 5).forEach { query ->
                RecentSearchRow(query = query, isDark = isDark, textColor = textColor, onClick = { onRecentClick(query) })
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

// ─────────────────────────────────────────────
// Results state
// ─────────────────────────────────────────────
@Composable
private fun SearchResultsContent(
    query: String,
    appResults: List<AppInfo>,
    activeFilter: SearchFilter,
    isDark: Boolean,
    textColor: Color,
    isExpanded: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onWebSearch: (String) -> Unit
) {
    val showApps = activeFilter == SearchFilter.All || activeFilter == SearchFilter.Apps
    val showWeb = activeFilter == SearchFilter.All || activeFilter == SearchFilter.Web
    val maxResults = if (isExpanded) 10 else 6

    if (appResults.isEmpty() && showApps) {
        // No app results
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SearchOff, null, tint = textColor.copy(0.18f), modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("No apps found for \"$query\"", color = textColor.copy(0.45f), fontSize = 12.sp)
            }
        }
    }

    // App results section
    if (showApps && appResults.isNotEmpty()) {
        SectionHeader(
            title = "Apps",
            icon = Icons.Default.Apps,
            textColor = textColor,
            action = if (appResults.size > maxResults) "See all (${appResults.size})" to {} else null
        )
        Spacer(Modifier.height(6.dp))

        // Best match hero item
        if (appResults.isNotEmpty()) {
            BestMatchItem(
                app = appResults.first(),
                isDark = isDark,
                textColor = textColor,
                onClick = { onAppClick(appResults.first()) }
            )
            Spacer(Modifier.height(6.dp))
        }

        // Rest of results
        if (appResults.size > 1) {
            LazyColumn(
                modifier = Modifier.heightIn(max = if (isExpanded) 260.dp else 180.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(appResults.drop(1).take(maxResults - 1)) { app ->
                    AppResultRow(
                        app = app,
                        query = query,
                        isDark = isDark,
                        textColor = textColor,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    // Web search section
    if (showWeb) {
        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
        Spacer(Modifier.height(4.dp))

        SectionHeader(title = "Web", icon = Icons.Default.Language, textColor = textColor, action = null)
        Spacer(Modifier.height(6.dp))

        // Search the web row
        WebSearchRow(query = query, isDark = isDark, textColor = textColor, onClick = { onWebSearch(query) })

        Spacer(Modifier.height(6.dp))

        // Suggested web queries
        val suggestions = generateWebSuggestions(query)
        suggestions.take(if (isExpanded) 4 else 2).forEach { suggestion ->
            WebSuggestionRow(
                suggestion = suggestion,
                isDark = isDark,
                textColor = textColor,
                onClick = { onWebSearch(suggestion) }
            )
        }
    }
}

// ─────────────────────────────────────────────
// Best match "hero" result
// ─────────────────────────────────────────────
@Composable
private fun BestMatchItem(app: AppInfo, isDark: Boolean, textColor: Color, onClick: () -> Unit) {
    val bg = if (isDark) Color(0xFF2A2A3A) else Color(0xFFEAEAF5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, Win11Colors.AccentBlue.copy(0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App icon with accent ring
        Box {
            AppIconSmall(
                drawable = app.icon,
                contentDescription = app.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            // Best match indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Win11Colors.AccentBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(8.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Best match · App", color = textColor.copy(0.5f), fontSize = 11.sp)
        }

        // Open button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Win11Colors.AccentBlue.copy(0.15f))
                .border(0.5.dp, Win11Colors.AccentBlue.copy(0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("Open", color = Win11Colors.AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────
// App result row
// ─────────────────────────────────────────────
@Composable
private fun AppResultRow(app: AppInfo, query: String, isDark: Boolean, textColor: Color, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val bg = if (hovered) (if (isDark) Color(0xFF2A2A3A) else Color(0xFFEAEAF5)) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppIconSmall(
            drawable = app.icon,
            contentDescription = app.name,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            // Highlight matched text
            val nameAnnotated = buildAnnotatedString {
                val lower = app.name.lowercase()
                val queryLower = query.lowercase()
                val idx = lower.indexOf(queryLower)
                if (idx >= 0) {
                    append(app.name.substring(0, idx))
                    withStyle(SpanStyle(color = Win11Colors.AccentBlue, fontWeight = FontWeight.SemiBold)) {
                        append(app.name.substring(idx, idx + query.length))
                    }
                    append(app.name.substring(idx + query.length))
                } else {
                    append(app.name)
                }
            }
            Text(nameAnnotated, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("App", color = textColor.copy(0.4f), fontSize = 10.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(0.25f), modifier = Modifier.size(14.dp))
    }
}

// ─────────────────────────────────────────────
// Web search row
// ─────────────────────────────────────────────
@Composable
private fun WebSearchRow(query: String, isDark: Boolean, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF25253A) else Color(0xFFE5E5F5))
            .border(0.5.dp, Win11Colors.AccentBlue.copy(0.2f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Language, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(16.dp))
        Text(
            buildAnnotatedString {
                append("Search the web for ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                    append("\"$query\"")
                }
            },
            color = textColor.copy(0.75f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.OpenInNew, null, tint = textColor.copy(0.3f), modifier = Modifier.size(13.dp))
    }
}

// ─────────────────────────────────────────────
// Web suggestion row
// ─────────────────────────────────────────────
@Composable
private fun WebSuggestionRow(suggestion: String, isDark: Boolean, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.TrendingUp, null, tint = textColor.copy(0.3f), modifier = Modifier.size(13.dp))
        Text(suggestion, color = textColor.copy(0.75f), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Default.NorthWest, null, tint = textColor.copy(0.2f), modifier = Modifier.size(12.dp))
    }
}

// ─────────────────────────────────────────────
// Recent search row
// ─────────────────────────────────────────────
@Composable
private fun RecentSearchRow(query: String, isDark: Boolean, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.History, null, tint = textColor.copy(0.4f), modifier = Modifier.size(14.dp))
        Text(
            query,
            color = textColor.copy(0.8f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.NorthWest, null, tint = textColor.copy(0.2f), modifier = Modifier.size(12.dp))
    }
}

// ─────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────
@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    textColor: Color,
    action: Pair<String, () -> Unit>?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor.copy(0.45f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            title,
            color = textColor.copy(0.5f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                action.first,
                color = Win11Colors.AccentBlue,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { action.second() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Search filter chip
// ─────────────────────────────────────────────
@Composable
private fun SearchFilterChip(
    filter: SearchFilter,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> Win11Colors.AccentBlue
            isDark -> Color(0xFF2A2A3A)
            else -> Color(0xFFE0E0EE)
        },
        animationSpec = tween(150),
        label = "chipBg"
    )
    val textColor = if (isSelected) Color.White else if (isDark) Color.White.copy(0.7f) else Color.Black.copy(0.6f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = if (isSelected) 0.dp else 0.5.dp,
                color = if (isDark) Color(0x22FFFFFF) else Color(0x22000000),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(filter.icon, null, tint = textColor, modifier = Modifier.size(12.dp))
        Text(filter.label, color = textColor, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ─────────────────────────────────────────────
// Top App Icon in Grid
// ─────────────────────────────────────────────
@Composable
private fun TopAppItem(app: AppInfo, isDark: Boolean, onClick: () -> Unit) {
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "topAppScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.scale(scale)) {
            AppIconSmall(
                drawable = app.icon,
                contentDescription = app.name,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            app.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(0.75f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 9.sp
        )
    }
}

// ─────────────────────────────────────────────
// Quick search chip
// ─────────────────────────────────────────────
@Composable
private fun QuickSearchChip(label: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    val bgColor = if (isDark) Color(0xFF252535) else Color(0xFFE0E0EE)
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(0.5.dp, if (isDark) Color(0x18FFFFFF) else Color(0x18000000), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(13.dp))
        Text(label, color = textColor.copy(0.85f), fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────
private fun generateWebSuggestions(query: String): List<String> = listOf(
    "$query - Wikipedia",
    "$query tutorial",
    "$query near me",
    "How to $query",
    "$query download"
)

private val quickSearchItems = listOf(
    "Weather" to Icons.Default.WbSunny,
    "News" to Icons.Default.Newspaper,
    "Calculator" to Icons.Default.Calculate,
    "Maps" to Icons.Default.Map,
    "Music" to Icons.Default.MusicNote,
    "Photos" to Icons.Default.PhotoLibrary,
    "Translate" to Icons.Default.Translate,
    "Shopping" to Icons.Default.ShoppingBag
)
