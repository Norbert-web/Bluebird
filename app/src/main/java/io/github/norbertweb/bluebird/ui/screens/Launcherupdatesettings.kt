package io.github.norbertweb.bluebird.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.update.*
import io.github.norbertweb.bluebird.ui.components.FluentIcon
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// LAUNCHER UPDATE SETTINGS  (replaces the old simple section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun LauncherUpdateSettings(a: ScreenArgs) {
    val scale   = a.uiState?.textScale ?: 1f
    val scope   = rememberCoroutineScope()
    val ctx     = a.ctx

    // ── State ─────────────────────────────────────────────────────────────────
    var updateResult   by remember { mutableStateOf<UpdateResult>(UpdateResult.Loading) }
    var isChecking     by remember { mutableStateOf(false) }
    var lastChecked    by remember { mutableStateOf(UpdateManager.lastCheckedLabel(ctx)) }
    var checkFrequency by remember { mutableStateOf(UpdateManager.getCheckFrequency(ctx)) }
    var deliveryMode   by remember { mutableStateOf(UpdateManager.getDeliveryMode(ctx)) }
    var autoUpdate     by remember { mutableStateOf(UpdateManager.getAutoUpdate(ctx)) }
    var updateChannel  by remember { mutableStateOf(UpdateManager.getUpdateChannel(ctx)) }
    var showChangelog  by remember { mutableStateOf(false) }

    // Kick off a check on first composition
    LaunchedEffect(Unit) {
        isChecking    = true
        updateResult  = UpdateManager.checkForUpdate(ctx)
        lastChecked   = UpdateManager.lastCheckedLabel(ctx)
        isChecking    = false

        // Notify if update found
        if (updateResult is UpdateResult.UpdateAvailable) {
            UpdateManager.notifyIfNewVersion(ctx, updateResult, deliveryMode)
        }
    }

    // Helper to trigger a manual check
    fun manualCheck() {
        scope.launch {
            isChecking   = true
            updateResult = UpdateResult.Loading
            updateResult = UpdateManager.checkForUpdate(ctx)
            lastChecked  = UpdateManager.lastCheckedLabel(ctx)
            isChecking   = false
            if (updateResult is UpdateResult.UpdateAvailable) {
                UpdateManager.notifyIfNewVersion(ctx, updateResult, deliveryMode)
            }
        }
    }

    // ── Current version info ──────────────────────────────────────────────────
    val pm      = ctx.packageManager
    val pkgInfo = try { pm.getPackageInfo(ctx.packageName, 0) } catch (_: Exception) { null }
    val currentVersionName = pkgInfo?.versionName ?: "1.0"

    // ── Update status card ────────────────────────────────────────────────────
    SettingsGroup("Update status", a) {
        AnimatedContent(
            targetState = isChecking to updateResult,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "update_status"
        ) { (checking, result) ->
            when {
                checking -> CheckingIndicator(a, scale)
                result is UpdateResult.Loading -> CheckingIndicator(a, scale)
                result is UpdateResult.UpdateAvailable -> UpdateAvailableCard(
                    a, result, scale, deliveryMode,
                    onShowChangelog = { showChangelog = !showChangelog }
                )
                result is UpdateResult.Error -> ErrorCard(a, result, scale) { manualCheck() }
                else -> UpToDateCard(a, scale, lastChecked) { manualCheck() }
            }
        }

        // Changelog expansion
        AnimatedVisibility(visible = showChangelog && updateResult is UpdateResult.UpdateAvailable) {
            val manifest = (updateResult as? UpdateResult.UpdateAvailable)?.manifest
            if (manifest != null) {
                ChangelogPanel(a, manifest, scale)
            }
        }
    }

    // ── Update delivery method ────────────────────────────────────────────────
    SettingsGroup("Update method", a) {
        UpdateDelivery.entries.forEachIndexed { i, mode ->
            if (i > 0) Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        deliveryMode = mode
                        UpdateManager.setDeliveryMode(ctx, mode)
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, color) = if (mode == UpdateDelivery.EXTERNAL)
                    FluentIcon.OpenInBrowser to a.accent
                else
                    FluentIcon.CloudDownload to Color(0xFF4CAF50)

                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (mode == UpdateDelivery.EXTERNAL) "External — GitHub release"
                        else "In-app — automatic download",
                        color = a.textColor,
                        fontSize = (13 * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (mode == UpdateDelivery.EXTERNAL)
                            "Opens your browser at the GitHub release page to download the APK"
                        else
                            "Downloads and installs the update inside the launcher automatically",
                        color = a.textColor.copy(alpha = 0.5f),
                        fontSize = (11 * scale).sp
                    )
                }
                RadioButton(
                    selected = deliveryMode == mode,
                    onClick = {
                        deliveryMode = mode
                        UpdateManager.setDeliveryMode(ctx, mode)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = a.accent)
                )
            }
        }
    }

    // ── Auto-check preferences ────────────────────────────────────────────────
    SettingsGroup("Automatic checks", a) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    autoUpdate = !autoUpdate
                    UpdateManager.setAutoUpdate(ctx, autoUpdate)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(FluentIcon.Autorenew, null, tint = a.accent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Automatic update checks", color = a.textColor, fontSize = (13 * scale).sp)
                Text("Periodically check for new versions in the background",
                    color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * scale).sp)
            }
            Switch(
                checked = autoUpdate, onCheckedChange = {
                    autoUpdate = it
                    UpdateManager.setAutoUpdate(ctx, it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = a.accent)
            )
        }

        AnimatedVisibility(visible = autoUpdate) {
            Column {
                Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
                // Frequency picker
                UpdateCheckFrequency.entries.forEachIndexed { i, freq ->
                    if (i > 0) Divider(color = divColor(a).copy(alpha = 0.03f), modifier = Modifier.padding(start = 44.dp, end = 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                checkFrequency = freq
                                UpdateManager.setCheckFrequency(ctx, freq)
                            }
                            .padding(start = 44.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val freqIcon = when (freq) {
                            UpdateCheckFrequency.EVERY_LAUNCH -> FluentIcon.Play
                            UpdateCheckFrequency.DAILY        -> FluentIcon.CalendarDay
                            UpdateCheckFrequency.WEEKLY       -> FluentIcon.CalendarWeek
                            UpdateCheckFrequency.BIWEEKLY     -> FluentIcon.CalendarBiweekly
                            UpdateCheckFrequency.SEVEN_WEEKS  -> FluentIcon.CalendarSevenWeeks
                            UpdateCheckFrequency.MONTHLY      -> FluentIcon.CalendarClockIcon
                            UpdateCheckFrequency.MANUAL       -> FluentIcon.TouchApp
                        }
                        Icon(freqIcon, null, tint = if (checkFrequency == freq) a.accent else a.textColor.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                        Text(
                            freq.displayName,
                            color = if (checkFrequency == freq) a.accent else a.textColor,
                            fontSize = (12 * scale).sp,
                            fontWeight = if (checkFrequency == freq) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (checkFrequency == freq) {
                            Icon(FluentIcon.Checkmark, null, tint = a.accent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Channel ───────────────────────────────────────────────────────────────
    SettingsGroup("Update channel", a) {
        listOf(
            Triple("Stable",  "Recommended for daily use",                         FluentIcon.VerifiedUser),
            Triple("Beta",    "Early features, may have occasional rough edges",    FluentIcon.Science),
            Triple("Dev",     "Cutting-edge builds, expect frequent changes",       FluentIcon.BugReport)
        ).forEachIndexed { i, (ch, desc, icon) ->
            if (i > 0) Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        updateChannel = ch
                        UpdateManager.setUpdateChannel(ctx, ch)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val channelColor = when (ch) {
                    "Stable" -> Color(0xFF4CAF50)
                    "Beta"   -> Color(0xFFFF9800)
                    else     -> Color(0xFFE91E63)
                }
                Icon(icon, null, tint = channelColor, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ch, color = a.textColor, fontSize = (13 * scale).sp, fontWeight = FontWeight.Medium)
                    Text(desc, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * scale).sp)
                }
                RadioButton(
                    selected = updateChannel == ch,
                    onClick  = { updateChannel = ch; UpdateManager.setUpdateChannel(ctx, ch) },
                    colors   = RadioButtonDefaults.colors(selectedColor = channelColor)
                )
            }
        }
    }

    // ── Release notes (current version) ──────────────────────────────────────
    SettingsGroup("Release notes — $currentVersionName", a) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("What's in this version", color = a.textColor, fontSize = (13 * scale).sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            listOf(
                "Full theme system: System, For You, Dark, Light, Special",
                "Text size slider with live preview",
                "All settings wired to LauncherViewModel + SharedPreferences",
                "Sound section with per-category volume sliders",
                "Expanded Gaming & Accessibility settings",
                "Full Privacy dashboard with per-app permission controls",
                "Gesture settings, Backup & Restore, Search settings",
                "Integrated update system with GitHub manifest support"
            ).forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✦", color = a.accent, fontSize = (12 * scale).sp)
                    Text(it, color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * scale).sp)
                }
            }
        }
    }

    // ── Schedule ──────────────────────────────────────────────────────────────
    SettingsGroup("Scheduled restart", a) {
        SNav(FluentIcon.Calendar, "Schedule restart", "Pick a maintenance window for updates to apply", a = a)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CheckingIndicator(a: ScreenArgs, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = a.accent
        )
        Text("Checking for updates…", color = a.textColor, fontSize = (13 * scale).sp)
    }
}

@Composable
private fun UpToDateCard(a: ScreenArgs, scale: Float, lastChecked: String, onRecheck: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(FluentIcon.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Your launcher is up to date", color = a.textColor, fontSize = (13 * scale).sp, fontWeight = FontWeight.Medium)
                Text("Last checked: $lastChecked", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * scale).sp)
            }
            OutlinedButton(
                onClick = onRecheck,
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = a.accent),
                border  = BorderStroke(1.dp, a.accent.copy(alpha = 0.5f))
            ) { Text("Check now", fontSize = (12 * scale).sp) }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    a: ScreenArgs,
    result: UpdateResult.UpdateAvailable,
    scale: Float,
    deliveryMode: UpdateDelivery,
    onShowChangelog: () -> Unit
) {
    val ctx      = a.ctx
    val manifest = result.manifest

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header gradient strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(a.accent.copy(alpha = 0.8f), a.accent.copy(alpha = 0.4f))),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FluentIcon.SystemUpdateAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Version ${manifest.versionName} available",
                        color = Color.White,
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (manifest.apkSize.isNotEmpty()) {
                        Text(manifest.apkSize, color = Color.White.copy(alpha = 0.8f), fontSize = (11 * scale).sp)
                    }
                }
                if (manifest.forceUpdate) {
                    Surface(
                        shape  = RoundedCornerShape(4.dp),
                        color  = Color(0xFFE91E63)
                    ) {
                        Text("Required", color = Color.White, fontSize = (10 * scale).sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // Action buttons
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (deliveryMode == UpdateDelivery.EXTERNAL || manifest.apkUrl.isNotEmpty()) {
                Button(
                    onClick = {
                        val url = manifest.apkUrl.ifEmpty { UpdateManager.UPDATE_JSON_URL }
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = a.accent)
                ) {
                    Icon(FluentIcon.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download from GitHub", fontSize = (13 * scale).sp)
                }
            }

            if (deliveryMode == UpdateDelivery.INTERNAL) {
                OutlinedButton(
                    onClick = { /* trigger in-app download — hook into your DownloadManager flow */ },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, a.accent),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = a.accent)
                ) {
                    Icon(FluentIcon.CloudDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download & install now", fontSize = (13 * scale).sp)
                }
            }

            // Changelog toggle
            TextButton(
                onClick  = onShowChangelog,
                colors   = ButtonDefaults.textButtonColors(contentColor = a.accent)
            ) {
                Icon(FluentIcon.List, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("View changelog", fontSize = (12 * scale).sp)
            }
        }
    }
}

@Composable
private fun ErrorCard(a: ScreenArgs, error: UpdateResult.Error, scale: Float, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(FluentIcon.Error, null, tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Check failed", color = a.textColor, fontSize = (13 * scale).sp, fontWeight = FontWeight.Medium)
            Text(error.message, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * scale).sp, maxLines = 2)
        }
        TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = a.accent)) {
            Text("Retry", fontSize = (12 * scale).sp)
        }
    }
}

@Composable
private fun ChangelogPanel(a: ScreenArgs, manifest: UpdateManifest, scale: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(a.accent.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "What's new in ${manifest.versionName}",
            color = a.textColor,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        manifest.changelog.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = a.accent, fontSize = (12 * scale).sp)
                Text(line, color = a.textColor.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
            }
        }
    }
}
