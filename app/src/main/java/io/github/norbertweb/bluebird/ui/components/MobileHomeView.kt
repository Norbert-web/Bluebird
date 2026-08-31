package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.AppInfo
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

// ─────────────────────────────────────────────────────────
// MOBILE HOME VIEW — v2
//
// Previously this was a bare 4-col grid + dock with none of the desktop
// Start Menu's visual language (no glass, no accent) — it read like an
// unfinished placeholder screen bolted onto a Windows-11-styled app. It
// also had a real bug: the existing top search bar (PremiumSearchBar,
// rendered above this view in StartMenu.kt) would set activeTab = SEARCH
// when typed into, but the caller never checked activeTab while in mobile
// mode — so search silently did nothing here. Fixed at the call site in
// StartMenu.kt (falls through to the existing SearchResultsView when
// activeTab == SEARCH, instead of duplicating a second search box here).
//
// This version:
//   • uses shared DS colors + the user's opacity setting for the glass bg,
//     so it matches desktop Start Menu instead of looking like a separate app
//   • groups apps under alphabetical section headers (Win11 "All apps"
//     jump-list style) instead of one flat unlabeled grid
//   • pulls icon bitmaps from the shared cache (rememberAppIconBitmap)
//     instead of converting on the UI thread per composition — this was
//     the main cause of stutter when swiping between pages
// ─────────────────────────────────────────────────────────
private const val MOBILE_HOME_APPS_PER_PAGE = 20 // 4 columns × 5 rows

@Composable
fun MobileHomeView(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    isDark: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    val (opacity, _) = rememberOpacity(context)
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val sortedApps = remember(uiState.installedApps) {
        uiState.installedApps.sortedBy { it.name.lowercase() }
    }
    val pages = remember(sortedApps) {
        val chunks = sortedApps.chunked(MOBILE_HOME_APPS_PER_PAGE)
        if (chunks.isEmpty()) listOf(emptyList()) else chunks
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockApps = uiState.pinnedTaskbarApps.take(5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DS.glass(isDark, opacity))
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Paginated, alphabetically-sectioned app grid ──
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { pageIndex ->
            val pageApps = pages[pageIndex]
            val grouped = remember(pageApps) { pageApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' } }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (letter, apps) ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                        Text(
                            letter.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DS.accentStart,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(apps, key = { it.packageName }) { app ->
                        MobileAppIcon(
                            app = app,
                            isDark = isDark,
                            onClick = {
                                incrementMobileHomeOpenCount(context, app.packageName)
                                viewModel.openApp(context, app)
                            }
                        )
                    }
                }
            }
        }

        // Page indicator dots (only shown when apps span more than one page)
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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

        // Bottom dock — now uses the same glass surface treatment as the
        // rest of the app instead of a plain flat-alpha background.
        if (dockApps.isNotEmpty()) {
            HorizontalDivider(color = if (isDark) DS.borderDark else DS.borderLight, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DS.surfaceGlass(isDark, opacity))
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
                            incrementMobileHomeOpenCount(context, app.packageName)
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
    val bitmap = rememberAppIconBitmap(app) // off-main-thread cached decode — fixes stutter

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
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = app.name, modifier = Modifier.size((iconSize - 12).dp))
            } else {
                Icon(FluentIcon.Apps, null, tint = DS.accentStart, modifier = Modifier.size((iconSize / 2.5).dp))
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

private fun incrementMobileHomeOpenCount(context: Context, packageName: String) {
    val prefs = context.getSharedPreferences("start_menu_usage_prefs", Context.MODE_PRIVATE)
    val current = prefs.getInt("open_cnt_$packageName", 0)
    prefs.edit().putInt("open_cnt_$packageName", current + 1).apply()
}
