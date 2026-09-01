package io.github.norbertweb.bluebird.ui.components

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.AppInfo
import io.github.norbertweb.bluebird.LauncherScreen
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

// ─────────────────────────────────────────────────────────
// MOBILE HOME VIEW — v3
//
// Was a paginated, side-swiped HorizontalPager grid + a separate fixed
// bottom dock. That's gone: this is now one continuous, down-scrolling
// list (LazyColumn), matching what was asked for — no side paging.
//
// Sections, top to bottom:
//   • Pinned    — pinned apps + pinned system apps (same "Add to Start"
//                 pin used on desktop's All Apps / Pinned tab)
//   • System    — Bluebird's built-in apps (Settings, Terminal, etc.),
//                 with custom icons via rememberBuiltInIconResourceId
//   • All Apps  — every installed app, alphabetically grouped
//
// "Recent" and the quick-action shortcuts never existed on this screen
// and still don't. No animations anywhere in this file.
// ─────────────────────────────────────────────────────────
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
    val grouped = remember(sortedApps) {
        sortedApps.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    var pinnedBuiltInNames by remember { mutableStateOf(getPinnedBuiltInAppNames(context)) }
    val pinnedBuiltInApps = remember(pinnedBuiltInNames) {
        builtInApps.filter { it.first in pinnedBuiltInNames }
    }
    val pinnedApps = uiState.pinnedTaskbarApps

    fun openApp(app: AppInfo) {
        incrementMobileHomeOpenCount(context, app.packageName)
        viewModel.openApp(context, app)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(DS.glass(isDark, opacity)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (pinnedApps.isNotEmpty() || pinnedBuiltInApps.isNotEmpty()) {
            item { MobileSectionHeader("Pinned", isDark) }
            items(pinnedApps.chunked(4), key = { "pinned_row_" + it.joinToString { a -> a.packageName } }) { row ->
                MobileAppRow(row, isDark, onClick = { openApp(it) })
            }
            if (pinnedBuiltInApps.isNotEmpty()) {
                items(pinnedBuiltInApps.chunked(4), key = { "pinned_sys_row_" + it.joinToString { s -> s.first } }) { row ->
                    MobileBuiltInAppRow(row, isDark, onClick = { viewModel.openWindow(it) })
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        item { MobileSectionHeader("System", isDark) }
        items(builtInApps.chunked(4), key = { "sys_row_" + it.joinToString { s -> s.first } }) { row ->
            MobileBuiltInAppRow(row, isDark, onClick = { viewModel.openWindow(it) })
        }
        item { Spacer(Modifier.height(10.dp)) }

        item { MobileSectionHeader("All Apps", isDark) }
        grouped.forEach { (letter, apps) ->
            item(key = "letter_$letter") {
                Text(
                    letter.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DS.accentStart,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(apps.chunked(4), key = { "all_row_" + it.joinToString { a -> a.packageName } }) { row ->
                MobileAppRow(row, isDark, onClick = { openApp(it) })
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MobileSectionHeader(title: String, isDark: Boolean) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = textPrimary.copy(alpha = 0.7f),
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun MobileAppRow(apps: List<AppInfo>, isDark: Boolean, onClick: (AppInfo) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        apps.forEach { app ->
            Box(modifier = Modifier.weight(1f)) {
                MobileAppIcon(app = app, isDark = isDark, onClick = { onClick(app) })
            }
        }
        repeat(4 - apps.size) { Spacer(modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun MobileBuiltInAppRow(
    apps: List<Triple<String, ImageVector, LauncherScreen>>,
    isDark: Boolean,
    onClick: (LauncherScreen) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        apps.forEach { (name, icon, screen) ->
            Box(modifier = Modifier.weight(1f)) {
                MobileBuiltInAppIcon(name = name, icon = icon, isDark = isDark, onClick = { onClick(screen) })
            }
        }
        repeat(4 - apps.size) { Spacer(modifier = Modifier.weight(1f)) }
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
    val bitmap = rememberAppIconBitmap(app) // off-main-thread cached decode — fixes stutter

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width((iconSize + 16).dp)
            )
        }
    }
}

@Composable
private fun MobileBuiltInAppIcon(
    name: String,
    icon: ImageVector,
    isDark: Boolean,
    iconSize: Int = 56,
    onClick: () -> Unit
) {
    val textPrimary = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    var pressed by remember { mutableStateOf(false) }
    val customIconResId = rememberBuiltInIconResourceId(name)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(name) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
    ) {
        if (customIconResId != 0) {
            Box(
                modifier = Modifier.size(iconSize.dp).clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                BuiltInAppIcon(
                    appName = name,
                    fallback = icon,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DS.accentStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, name, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size((iconSize / 2.5).dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width((iconSize + 16).dp)
        )
    }
}

private fun incrementMobileHomeOpenCount(context: Context, packageName: String) {
    val prefs = context.getSharedPreferences("start_menu_usage_prefs", Context.MODE_PRIVATE)
    val current = prefs.getInt("open_cnt_$packageName", 0)
    prefs.edit().putInt("open_cnt_$packageName", current + 1).apply()
}
