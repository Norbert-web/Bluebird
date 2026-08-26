package io.github.norbertweb.bluebird.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.RealNotification
import io.github.norbertweb.bluebird.ui.theme.LocalTextScale
import io.github.norbertweb.bluebird.ui.theme.Win11Colors

// ─── Remote Notification Model ────────────────────────────────────────────────

data class BluebirdRemoteNotification(
    val id: String,
    val type: String,           // "announcement" | "update" | "warning"
    val priority: String,       // "normal" | "high"
    val title: String,
    val body: String,
    val timestamp: String,
    val expiresAt: String?,
    val actionLabel: String?,
    val actionUrl: String?,
    val badgeColor: String = "#0078D4"
)

// ─── Text Scale Steps ─────────────────────────────────────────────────────────

private val TEXT_SCALE_STEPS = listOf(
    0.8f  to "Small",
    1.0f  to "Default",
    1.15f to "Large",
    1.3f  to "XL"
)

private fun Float.toTextScaleLabel(): String =
    TEXT_SCALE_STEPS.minByOrNull { kotlin.math.abs(it.first - this) }?.second ?: "Default"

// ─── Main ActionCenter ────────────────────────────────────────────────────────

@Composable
fun ActionCenter(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textScale = LocalTextScale.current

    // Remote (Bluebird team) notifications now live in the ViewModel — it
    // polls notify.json on a timer so both this panel and the toast host
    // (NotificationToastHost) share the exact same fetched list, and
    // dismissing an announcement in either place dismisses it in both.
    val remoteNotifications = uiState.remoteNotifications
    val dismissedRemoteIds  = uiState.dismissedRemoteNotificationIds

    val isDark     = uiState.isDarkTheme
    val textColor  = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val subColor   = textColor.copy(alpha = 0.55f)
    val divColor   = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDEDEDE)

    AcrylicSurface(
        modifier = modifier
            .width(380.dp)
            .wrapContentHeight(),
        isDark       = isDark,
        alpha        = 0.97f,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Quick Settings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = (15 * textScale).sp,
                    color      = textColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // DND toggle (header pill)
                    DndPill(
                        enabled  = uiState.dndEnabled,
                        isDark   = isDark,
                        onClick  = { viewModel.setDndEnabled(!uiState.dndEnabled) },
                        textScale = textScale
                    )
                    // Settings shortcut
                    SmallIconButton(isDark = isDark, onClick = {
                        viewModel.openWindow(io.github.norbertweb.bluebird.LauncherScreen.SETTINGS)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = textColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 2×3 Quick Toggle Grid ─────────────────────────────────────────
            val toggles = listOf(
                ToggleData(
                    label    = if (uiState.isWifiOn) "Wi-Fi" else "Wi-Fi Off",
                    subLabel = if (uiState.isWifiOn) "Connected" else "Disconnected",
                    icon     = if (uiState.isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                    active   = uiState.isWifiOn,
                    onClick  = { viewModel.openWifiSettings(context) }
                ),
                ToggleData(
                    label    = if (uiState.isBluetoothOn) "Bluetooth" else "Bluetooth",
                    subLabel = if (uiState.isBluetoothOn) "On" else "Off",
                    icon     = Icons.Default.Bluetooth,
                    active   = uiState.isBluetoothOn,
                    onClick  = { viewModel.openBluetoothSettings(context) }
                ),
                ToggleData(
                    label    = "Airplane",
                    subLabel = if (uiState.isAirplaneMode) "On" else "Off",
                    icon     = Icons.Default.AirplanemodeActive,
                    active   = uiState.isAirplaneMode,
                    onClick  = { viewModel.toggleAirplaneMode() }
                ),
                ToggleData(
                    label    = if (uiState.isDarkTheme) "Dark Mode" else "Light Mode",
                    subLabel = if (uiState.isDarkTheme) "Active" else "Active",
                    icon     = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    active   = true,
                    onClick  = { viewModel.toggleTheme() }
                ),
                ToggleData(
                    label    = "Focus Assist",
                    subLabel = if (uiState.focusAssist) "On" else "Off",
                    icon     = Icons.Default.DoNotDisturb,
                    active   = uiState.focusAssist,
                    onClick  = { viewModel.setFocusAssist(!uiState.focusAssist) }
                ),
                ToggleData(
                    label    = "Data Saver",
                    subLabel = if (uiState.dataSaver) "On" else "Off",
                    icon     = Icons.Default.DataSaverOn,
                    active   = uiState.dataSaver,
                    onClick  = { viewModel.setDataSaver(!uiState.dataSaver) }
                )
            )

            QuickToggleGrid(
                toggles   = toggles,
                isDark    = isDark,
                textScale = textScale
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = divColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // ── Volume ────────────────────────────────────────────────────────
            SliderSection(
                label     = "Volume",
                value     = uiState.volume,
                percent   = "${(uiState.volume * 100).toInt()}%",
                isDark    = isDark,
                textColor = textColor,
                subColor  = subColor,
                textScale = textScale,
                leadIcon  = {
                    Icon(
                        imageVector = when {
                            uiState.volume < 0.01f -> Icons.Default.VolumeOff
                            uiState.volume < 0.5f  -> Icons.Default.VolumeDown
                            else                   -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint     = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onValueChange = { viewModel.setVolume(it, context) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Brightness ────────────────────────────────────────────────────
            SliderSection(
                label     = "Brightness",
                value     = uiState.brightness,
                percent   = "${(uiState.brightness * 100).toInt()}%",
                isDark    = isDark,
                textColor = textColor,
                subColor  = subColor,
                textScale = textScale,
                leadIcon  = {
                    Icon(
                        imageVector        = Icons.Default.Brightness6,
                        contentDescription = "Brightness",
                        tint     = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onValueChange = { viewModel.setBrightness(it, context) }
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = divColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // ── Text Size ─────────────────────────────────────────────────────
            TextScaleSection(
                currentScale = uiState.textScale,
                isDark       = isDark,
                textColor    = textColor,
                subColor     = subColor,
                textScale    = textScale,
                onScaleChange = { viewModel.setTextScale(it) }
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = divColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // ── Battery ───────────────────────────────────────────────────────
            BatterySection(
                level     = uiState.batteryLevel,
                charging  = uiState.isCharging,
                isDark    = isDark,
                textColor = textColor,
                subColor  = subColor,
                textScale = textScale
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = divColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // ── Bluebird Team Notifications ───────────────────────────────────
            if (remoteNotifications.isNotEmpty()) {
                Text(
                    text       = "From Bluebird",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = (13 * textScale).sp,
                    color      = Win11Colors.AccentBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                remoteNotifications
                    .filter { it.id !in dismissedRemoteIds }
                    .forEach { notif ->
                        BluebirdRemoteNotifCard(
                            notif     = notif,
                            isDark    = isDark,
                            textScale = textScale,
                            onDismiss = { viewModel.dismissRemoteNotification(notif.id) },
                            onAction  = { url ->
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = divColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── System Notifications ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Notifications",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = (15 * textScale).sp,
                    color      = textColor
                )
                if (uiState.notifications.isNotEmpty()) {
                    TextButton(
                        onClick = { uiState.notifications.forEach { viewModel.dismissNotification(it.id) } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Clear all",
                            color    = Win11Colors.AccentBlue,
                            fontSize = (12 * textScale).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.notifications.isEmpty()) {
                EmptyNotificationsPlaceholder(isDark = isDark, textColor = subColor, textScale = textScale)
            } else {
                // Group by appName
                val grouped = uiState.notifications.groupBy { it.appName }
                grouped.forEach { (appName, notifs) ->
                    NotificationGroup(
                        appName   = appName,
                        notifs    = notifs,
                        isDark    = isDark,
                        textColor = textColor,
                        textScale = textScale,
                        onDismiss = { id -> viewModel.dismissNotification(id) },
                        onOpen    = { packageName ->
                            try {
                                val pm     = context.packageManager
                                val intent = pm.getLaunchIntentForPackage(packageName)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (intent != null) context.startActivity(intent)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── DND Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun DndPill(
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    textScale: Float
) {
    val bg = if (enabled) Win11Colors.AccentBlue else
        if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)
    val fg = if (enabled) Color.White else
        if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.DoNotDisturb, contentDescription = "DND",
            tint = fg, modifier = Modifier.size(13.dp)
        )
        Text(
            text     = if (enabled) "DND On" else "DND",
            color    = fg,
            fontSize = (11 * textScale).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Small Icon Button ────────────────────────────────────────────────────────

@Composable
private fun SmallIconButton(
    isDark: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFE8E8E8))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

// ─── Toggle Data ──────────────────────────────────────────────────────────────

private data class ToggleData(
    val label: String,
    val subLabel: String,
    val icon: ImageVector,
    val active: Boolean,
    val onClick: () -> Unit
)

// ─── Quick Toggle Grid (2×3) ──────────────────────────────────────────────────

@Composable
private fun QuickToggleGrid(
    toggles: List<ToggleData>,
    isDark: Boolean,
    textScale: Float
) {
    // 2 rows of 3
    val rows = toggles.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { toggle ->
                    QuickToggleTile(
                        data      = toggle,
                        isDark    = isDark,
                        textScale = textScale,
                        modifier  = Modifier.weight(1f)
                    )
                }
                // fill remainder if row is short
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickToggleTile(
    data: ToggleData,
    isDark: Boolean,
    textScale: Float,
    modifier: Modifier = Modifier
) {
    val activeBg   = Win11Colors.AccentBlue.copy(alpha = 0.18f)
    val inactiveBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEBEBEB)
    val bg         = if (data.active) activeBg else inactiveBg
    val iconTint   = if (data.active) Win11Colors.AccentBlue else
        if (isDark) Win11Colors.TextSecondary else Win11Colors.TextSecondaryLight
    val textColor  = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val subColor   = textColor.copy(alpha = 0.5f)

    val scale by animateFloatAsState(
        targetValue  = if (data.active) 1f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label        = "toggleScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (data.active) 1.dp else 0.dp,
                color = if (data.active) Win11Colors.AccentBlue.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { data.onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector        = data.icon,
            contentDescription = data.label,
            tint               = iconTint,
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text       = data.label,
            color      = textColor,
            fontSize   = (11 * textScale).sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            text     = data.subLabel,
            color    = subColor,
            fontSize = (10 * textScale).sp,
            maxLines = 1
        )
    }
}

// ─── Slider Section ───────────────────────────────────────────────────────────

@Composable
private fun SliderSection(
    label: String,
    value: Float,
    percent: String,
    isDark: Boolean,
    textColor: Color,
    subColor: Color,
    textScale: Float,
    leadIcon: @Composable () -> Unit,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leadIcon()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = textColor, fontSize = (12 * textScale).sp, fontWeight = FontWeight.Medium)
                Text(percent, color = subColor, fontSize = (11 * textScale).sp)
            }
            Slider(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors        = SliderDefaults.colors(
                    thumbColor            = Win11Colors.AccentBlue,
                    activeTrackColor      = Win11Colors.AccentBlue,
                    inactiveTrackColor    = if (isDark) Color(0xFF444444) else Color(0xFFCCCCCC)
                )
            )
        }
    }
}

// ─── Text Scale Section ───────────────────────────────────────────────────────

@Composable
private fun TextScaleSection(
    currentScale: Float,
    isDark: Boolean,
    textColor: Color,
    subColor: Color,
    textScale: Float,
    onScaleChange: (Float) -> Unit
) {
    val steps = TEXT_SCALE_STEPS

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.TextFields,
                    contentDescription = "Text Size",
                    tint     = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Text Size",
                    color      = textColor,
                    fontSize   = (12 * textScale).sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                currentScale.toTextScaleLabel(),
                color    = Win11Colors.AccentBlue,
                fontSize = (12 * textScale).sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Step buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEach { (scale, label) ->
                val selected = kotlin.math.abs(scale - currentScale) < 0.05f
                val bg       = if (selected) Win11Colors.AccentBlue
                else if (isDark) Color(0xFF2C2C2C) else Color(0xFFEBEBEB)
                val fg       = if (selected) Color.White
                else if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(
                            width = if (selected) 0.dp else 0.5.dp,
                            color = if (isDark) Color(0xFF444444) else Color(0xFFCCCCCC),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onScaleChange(scale) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color      = fg,
                        fontSize   = (11 * textScale).sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Preview text
        val previewBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(previewBg)
                .padding(10.dp)
        ) {
            Text(
                text     = "Preview: The quick brown fox jumps over the lazy dog.",
                color    = subColor,
                fontSize = (13 * currentScale).sp
            )
        }
    }
}

// ─── Battery Section ──────────────────────────────────────────────────────────

@Composable
private fun BatterySection(
    level: Int,
    charging: Boolean,
    isDark: Boolean,
    textColor: Color,
    subColor: Color,
    textScale: Float
) {
    val batteryColor = when {
        charging  -> Win11Colors.AccentBlue
        level > 20 -> Win11Colors.Success
        else       -> Win11Colors.Error
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (charging) Icons.Default.BatteryChargingFull else
                when {
                    level > 80 -> Icons.Default.BatteryFull
                    level > 50 -> Icons.Default.Battery5Bar
                    level > 20 -> Icons.Default.Battery3Bar
                    else       -> Icons.Default.Battery1Bar
                },
            contentDescription = "Battery",
            tint     = batteryColor,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$level% — ${if (charging) "Charging" else "On Battery"}",
                    color = textColor,
                    fontSize = (12 * textScale).sp,
                    fontWeight = FontWeight.Medium
                )
                if (charging) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint     = Win11Colors.AccentBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { level / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color      = batteryColor,
                trackColor = if (isDark) Color(0xFF3C3C3C) else Color(0xFFE0E0E0)
            )
        }
    }
}

// ─── Notification Group ───────────────────────────────────────────────────────

@Composable
private fun NotificationGroup(
    appName: String,
    notifs: List<RealNotification>,
    isDark: Boolean,
    textColor: Color,
    textScale: Float,
    onDismiss: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val headerBg = if (isDark) Color(0xFF252525) else Color(0xFFE8E8E8)

    Column {
        // Group header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(headerBg)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Win11Colors.AccentBlue)
                )
                Text(
                    text = appName,
                    color = textColor,
                    fontSize = (12 * textScale).sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (notifs.size > 1) {
                    Text(
                        text = "${notifs.size}",
                        color = Win11Colors.AccentBlue,
                        fontSize = (11 * textScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                notifs.forEach { notif ->
                    SwipeToDismissNotifCard(
                        notification = notif,
                        isDark       = isDark,
                        textColor    = textColor,
                        textScale    = textScale,
                        onDismiss    = { onDismiss(notif.id) },
                        onOpen       = { onOpen(notif.packageName) }
                    )
                }
            }
        }
    }
}

// ─── Swipe-to-Dismiss Notification Card ──────────────────────────────────────

@Composable
private fun SwipeToDismissNotifCard(
    notification: RealNotification,
    isDark: Boolean,
    textColor: Color,
    textScale: Float,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    var offsetX       by remember { mutableStateOf(0f) }
    var isDismissed   by remember { mutableStateOf(false) }
    val threshold     = 300f
    val cardBg        = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
    val animOffset    by animateFloatAsState(if (isDismissed) 1000f else offsetX, label = "swipe")

    if (isDismissed) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Dismiss background
        if (offsetX > 40f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(Win11Colors.Error.copy(alpha = (offsetX / threshold).coerceIn(0f, 1f))),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.padding(end = 16.dp).size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(animOffset.toInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(cardBg)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > threshold) isDismissed = true else offsetX = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta).coerceAtLeast(0f)
                        }
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Win11Colors.AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint     = Win11Colors.AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = notification.title,
                    color      = textColor,
                    fontSize   = (12 * textScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = notification.body,
                    color    = textColor.copy(alpha = 0.7f),
                    fontSize = (11 * textScale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = run {
                        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(notification.time))
                    },
                    color    = textColor.copy(alpha = 0.4f),
                    fontSize = (10 * textScale).sp
                )

                // Action buttons
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotifActionButton(label = "Open", isDark = isDark, textScale = textScale, onClick = onOpen)
                    NotifActionButton(label = "Dismiss", isDark = isDark, textScale = textScale, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun NotifActionButton(
    label: String,
    isDark: Boolean,
    textScale: Float,
    onClick: () -> Unit
) {
    val bg = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color    = Win11Colors.AccentBlue,
            fontSize = (11 * textScale).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Bluebird Remote Notif Card ───────────────────────────────────────────────

@Composable
private fun BluebirdRemoteNotifCard(
    notif: BluebirdRemoteNotification,
    isDark: Boolean,
    textScale: Float,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val accent = try {
        Color(android.graphics.Color.parseColor(notif.badgeColor))
    } catch (e: Exception) { Win11Colors.AccentBlue }

    val bg     = accent.copy(alpha = if (isDark) 0.12f else 0.1f)
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Bluebird brand dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
                .padding(top = 4.dp)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text       = notif.title,
                color      = textColor,
                fontSize   = (12 * textScale).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = notif.body,
                color    = textColor.copy(alpha = 0.75f),
                fontSize = (11 * textScale).sp,
                maxLines = 3
            )
            if (!notif.actionLabel.isNullOrBlank() && !notif.actionUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent)
                        .clickable { onAction(notif.actionUrl!!) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        notif.actionLabel!!,
                        color    = Color.White,
                        fontSize = (11 * textScale).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint     = textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─── Empty Placeholder ────────────────────────────────────────────────────────

@Composable
private fun EmptyNotificationsPlaceholder(
    isDark: Boolean,
    textColor: Color,
    textScale: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint     = textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                "No new notifications",
                color    = textColor.copy(alpha = 0.4f),
                fontSize = (12 * textScale).sp
            )
        }
    }
}
