package io.github.norbertweb.bluebird.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons come from the shared FluentIcon object (FluentIcon.kt), which wraps
// the io.github.niyajali:fluentui-system-icons Compose Multiplatform library.
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.RealNotification
import io.github.norbertweb.bluebird.ui.theme.LocalTextScale
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

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

// Reused across every notification card instead of allocating a new
// SimpleDateFormat on every recomposition/drag frame.
private val notifTimeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

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
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val subColor   = textColor.copy(alpha = 0.55f)
    val divColor   = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDEDEDE)

    // Local, self-contained state for toggles that aren't (yet) modeled in
    // LauncherUiState/LauncherViewModel. Wire each of these to real
    // ViewModel state + Android system APIs when available — see the
    // "TODO(wire)" comments below on each tile. Keeping them local means
    // this panel works standalone today and is a drop-in once the
    // corresponding state/actions exist upstream.
    var touchKeyboardOn by rememberSaveable { mutableStateOf(false) }
    var nightLightOn    by rememberSaveable { mutableStateOf(false) }
    var hotspotOn       by rememberSaveable { mutableStateOf(false) }
    var rotationLocked  by rememberSaveable { mutableStateOf(false) }
    var nfcOn           by rememberSaveable { mutableStateOf(false) }
    var energySaverOn   by rememberSaveable { mutableStateOf(false) }
    var accessibilityOn by rememberSaveable { mutableStateOf(false) }
    var expanded         by rememberSaveable { mutableStateOf(false) }

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
                // Subtle top mica sheen — mirrors the soft specular highlight
                // Windows 11 flyouts get along their top edge over acrylic.
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            (if (isDark) Color.White else Color.White).copy(alpha = if (isDark) 0.045f else 0.35f),
                            Color.Transparent
                        ),
                        endY = 140f
                    )
                )
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
                        Icon(imageVector = FluentIcon.Settings, contentDescription = "Settings",
                            tint = textColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Quick Toggle Grid (Win11-style: primary 2×3 grid + an
            //    expandable second page for the long tail of toggles,
            //    exactly like clicking the chevron in the real Quick
            //    Settings flyout) ────────────────────────────────────────
            val primaryToggles = listOf(
                // NOTE: Wi-Fi/Bluetooth still route straight to the system
                // settings page on tap (as in the original), since Android
                // 10+ apps generally can't flip these radios directly without
                // extra permissions. onLongClick is wired the same way so
                // both gestures are safe no-ops-into-settings until/unless
                // viewModel exposes real in-place toggle methods.
                ToggleData(
                    label    = if (uiState.isWifiOn) "Wi-Fi" else "Wi-Fi Off",
                    subLabel = if (uiState.isWifiOn) "Connected" else "Disconnected",
                    icon     = if (uiState.isWifiOn) FluentIcon.Wifi else FluentIcon.WifiOff,
                    active   = uiState.isWifiOn,
                    onClick  = { viewModel.openWifiSettings(context) },
                    onLongClick = { viewModel.openWifiSettings(context) }
                ),
                ToggleData(
                    label    = "Bluetooth",
                    subLabel = if (uiState.isBluetoothOn) "On" else "Off",
                    icon     = FluentIcon.Bluetooth,
                    active   = uiState.isBluetoothOn,
                    onClick  = { viewModel.openBluetoothSettings(context) },
                    onLongClick = { viewModel.openBluetoothSettings(context) }
                ),
                ToggleData(
                    label    = "Airplane",
                    subLabel = if (uiState.isAirplaneMode) "On" else "Off",
                    icon     = FluentIcon.Airplane,
                    active   = uiState.isAirplaneMode,
                    onClick  = { viewModel.toggleAirplaneMode() }
                ),
                ToggleData(
                    label    = if (uiState.isDarkTheme) "Dark Mode" else "Light Mode",
                    subLabel = "Active",
                    icon     = if (uiState.isDarkTheme) FluentIcon.Moon else FluentIcon.WeatherSunny,
                    active   = true,
                    onClick  = { viewModel.toggleTheme() }
                ),
                ToggleData(
                    label    = "Focus Assist",
                    subLabel = if (uiState.focusAssist) "On" else "Off",
                    icon     = FluentIcon.Prohibited,
                    active   = uiState.focusAssist,
                    onClick  = { viewModel.setFocusAssist(!uiState.focusAssist) }
                ),
                ToggleData(
                    label    = "Data Saver",
                    subLabel = if (uiState.dataSaver) "On" else "Off",
                    icon     = FluentIcon.DataUsage,
                    active   = uiState.dataSaver,
                    onClick  = { viewModel.setDataSaver(!uiState.dataSaver) }
                )
            )

            // Second page — mirrors the extra tiles Windows 11 tucks behind
            // the "expand" chevron: Night light, Mobile hotspot, Cast,
            // Rotation lock, Touch keyboard, NFC, Energy saver, Accessibility.
            // TODO(wire): swap each local `var` + no-op action below for real
            // LauncherUiState fields / LauncherViewModel calls once they
            // exist (e.g. uiState.nightLightOn, viewModel.toggleNightLight()).
            val secondaryToggles = listOf(
                ToggleData(
                    label    = "Night Light",
                    subLabel = if (nightLightOn) "On" else "Off",
                    icon     = FluentIcon.NightLight,
                    active   = nightLightOn,
                    onClick  = { nightLightOn = !nightLightOn }
                ),
                ToggleData(
                    label    = "Hotspot",
                    subLabel = if (hotspotOn) "On" else "Off",
                    icon     = FluentIcon.Hotspot,
                    active   = hotspotOn,
                    onClick  = {
                        hotspotOn = !hotspotOn
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_WIRELESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                ),
                ToggleData(
                    label    = "Cast",
                    subLabel = "Connect",
                    icon     = FluentIcon.Cast,
                    active   = false,
                    onClick  = {
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_CAST_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                ),
                ToggleData(
                    label    = "Rotation",
                    subLabel = if (rotationLocked) "Locked" else "Auto",
                    icon     = if (rotationLocked) FluentIcon.RotationLock else FluentIcon.AutoRotate,
                    active   = !rotationLocked,
                    onClick  = { rotationLocked = !rotationLocked }
                ),
                ToggleData(
                    label    = "Touch KB",
                    subLabel = if (touchKeyboardOn) "Shown" else "Hidden",
                    icon     = FluentIcon.Keyboard,
                    active   = touchKeyboardOn,
                    onClick  = { touchKeyboardOn = !touchKeyboardOn }
                ),
                ToggleData(
                    label    = "NFC",
                    subLabel = if (nfcOn) "On" else "Off",
                    icon     = FluentIcon.Nfc,
                    active   = nfcOn,
                    onClick  = {
                        nfcOn = !nfcOn
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NFC_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                ),
                ToggleData(
                    label    = "Energy Saver",
                    subLabel = if (energySaverOn) "On" else "Off",
                    icon     = FluentIcon.EnergySaver,
                    active   = energySaverOn,
                    onClick  = { energySaverOn = !energySaverOn }
                ),
                ToggleData(
                    label    = "Accessibility",
                    subLabel = if (accessibilityOn) "On" else "Off",
                    icon     = FluentIcon.Accessibility,
                    active   = accessibilityOn,
                    onClick  = {
                        accessibilityOn = !accessibilityOn
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                )
            )

            QuickToggleGrid(
                toggles   = primaryToggles,
                isDark    = isDark,
                textScale = textScale
            )

            AnimatedVisibility(
                visible = expanded,
                enter   = fadeIn(tween(180)) + expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                exit    = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickToggleGrid(
                        toggles   = secondaryToggles,
                        isDark    = isDark,
                        textScale = textScale
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expand/collapse chevron — matches the real Quick Settings
            // flyout's "show more" affordance.
            ExpandChevronRow(
                expanded  = expanded,
                isDark    = isDark,
                textScale = textScale,
                onClick   = { expanded = !expanded }
            )

            Spacer(modifier = Modifier.height(12.dp))
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
                                uiState.volume < 0.01f -> FluentIcon.SpeakerMute
                                uiState.volume < 0.5f  -> FluentIcon.Speaker1
                                else                   -> FluentIcon.Speaker2
                            },
                        contentDescription = "Volume",
                        tint     = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight,
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
                        imageVector            = FluentIcon.BrightnessHigh,
                        contentDescription = "Brightness",
                        tint     = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight,
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
                    color      = bluebirdColors.AccentBlue
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
                        onClick = viewModel::dismissAllNotifications,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Clear all",
                            color    = bluebirdColors.AccentBlue,
                            fontSize = (12 * textScale).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.notifications.isEmpty()) {
                EmptyNotificationsPlaceholder(isDark = isDark, textColor = subColor, textScale = textScale)
            } else {
                // Group once per notification-list identity instead of rebuilding the map
                // on unrelated recompositions (theme, text scale, quick toggles, etc.).
                val grouped = remember(uiState.notifications) {
                    uiState.notifications.groupBy { it.appName }
                }
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
    val bg by animateColorAsState(
        targetValue   = if (enabled) bluebirdColors.AccentBlue else
            if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0E0E0),
        animationSpec = tween(160),
        label         = "dndBg"
    )
    val fg = if (enabled) Color.White else
        if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label         = "dndPress"
    )
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                // No ripple indication here — avoids the deprecated
                // rememberRipple API (this project's Material3 version
                // doesn't ship the newer ripple() replacement either).
                // The scale-based press animation above already gives
                // tactile feedback on tap.
                indication        = null,
                onClick           = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = FluentIcon.Prohibited, contentDescription = "DND",
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label         = "iconBtnPress"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFE8E8E8))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ─── Toggle Data ──────────────────────────────────────────────────────────────

private data class ToggleData(
    val label: String,
    val subLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val active: Boolean,
    val onClick: () -> Unit,
    // Windows 11 pattern: tapping the tile flips the toggle; long-pressing
    // (or clicking the label text on desktop) jumps to the relevant system
    // settings page. Optional — tiles that have no deeper settings page
    // just omit it.
    val onLongClick: (() -> Unit)? = null
)

// ─── Quick Toggle Grid (2×3) ──────────────────────────────────────────────────

@Composable
private fun QuickToggleGrid(
    toggles: List<ToggleData>,
    isDark: Boolean,
    textScale: Float
) {
    // 3 columns, N rows — matches Windows 11's Quick Settings tile grid.
    val rows = remember(toggles) { toggles.chunked(3) }
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
    val activeBg   = bluebirdColors.AccentBlue.copy(alpha = 0.18f)
    val inactiveBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEBEBEB)
    val bg         = if (data.active) activeBg else inactiveBg
    val iconTint   = if (data.active) bluebirdColors.AccentBlue else
        if (isDark) bluebirdColors.TextSecondary else bluebirdColors.TextSecondaryLight
    val textColor  = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val subColor   = textColor.copy(alpha = 0.5f)
    val haptics    = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Two-stage motion: a quick, snappy press-down (Win11's fast fluent
    // "compress" feel) then a soft bouncy settle on release/toggle — instead
    // of one generic spring driving everything.
    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label         = "tilePress"
    )
    val settleScale by animateFloatAsState(
        targetValue   = if (data.active) 1f else 0.985f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "tileSettle"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (data.active) 0.4f else 0f,
        animationSpec = tween(180),
        label         = "tileBorder"
    )
    val bgColor by animateColorAsState(
        targetValue   = bg,
        animationSpec = tween(180),
        label         = "tileBg"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale * settleScale
                scaleY = pressScale * settleScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = bluebirdColors.AccentBlue.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    data.onClick()
                },
                onLongClick       = data.onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                }
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector            = data.icon,
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

// ─── Expand/Collapse Chevron Row ──────────────────────────────────────────────
// The little pill-with-chevron Windows 11 shows beneath the quick-toggle
// grid to reveal a second page of tiles.

@Composable
private fun ExpandChevronRow(
    expanded: Boolean,
    isDark: Boolean,
    textScale: Float,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "chevronRotate"
    )
    val tint = if (isDark) bluebirdColors.TextSecondary else bluebirdColors.TextSecondaryLight

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FluentIcon.ChevronDown,
                contentDescription = if (expanded) "Show fewer quick actions" else "Show more quick actions",
                tint     = tint,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
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
                    thumbColor            = bluebirdColors.AccentBlue,
                    activeTrackColor      = bluebirdColors.AccentBlue,
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
                    imageVector = FluentIcon.TextFont,
                    contentDescription = "Text Size",
                    tint     = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight,
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
                color    = bluebirdColors.AccentBlue,
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
                val bg       = if (selected) bluebirdColors.AccentBlue
                else if (isDark) Color(0xFF2C2C2C) else Color(0xFFEBEBEB)
                val fg       = if (selected) Color.White
                else if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

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
        charging  -> bluebirdColors.AccentBlue
        level > 20 -> bluebirdColors.Success
        else       -> bluebirdColors.Error
    }
    val animatedColor by animateColorAsState(
        targetValue   = batteryColor,
        animationSpec = tween(300),
        label         = "batteryColor"
    )
    val animatedLevel by animateFloatAsState(
        targetValue   = level / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label         = "batteryLevel"
    )

    // Gentle charging pulse on the bolt icon — a small nod to the subtle
    // motion Windows 11 uses to signal an active/live state.
    val infinite = rememberInfiniteTransition(label = "chargePulse")
    val boltAlpha by infinite.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "boltAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (charging) FluentIcon.BatteryCharge else
                    when {
                        level > 80 -> FluentIcon.Battery9
                        level > 50 -> FluentIcon.Battery6
                        level > 20 -> FluentIcon.Battery3
                        else       -> FluentIcon.Battery0
                    },
            contentDescription = "Battery",
            tint     = animatedColor,
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
                        imageVector = FluentIcon.Flash,
                        contentDescription = null,
                        tint     = bluebirdColors.AccentBlue.copy(alpha = boltAlpha),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { animatedLevel },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color      = animatedColor,
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
                        .background(bluebirdColors.AccentBlue)
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
                        color = bluebirdColors.AccentBlue,
                        fontSize = (11 * textScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = if (expanded) FluentIcon.ChevronUp else FluentIcon.ChevronDown,
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
    val haptics       = LocalHapticFeedback.current
    var thresholdCrossed by remember { mutableStateOf(false) }

    // Snappier settle spring than a flat tween — matches the fluent
    // "flick and stick" feel Windows 11 lists use.
    val animOffset by animateFloatAsState(
        targetValue   = if (isDismissed) 1000f else offsetX,
        animationSpec = if (isDismissed) tween(180) else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label         = "swipe"
    )

    if (isDismissed) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (offsetX > 4f) 3.dp else 0.dp,
                shape     = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                clip      = false
            )
    ) {
        // Dismiss background
        if (offsetX > 40f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(bluebirdColors.Error.copy(alpha = (offsetX / threshold).coerceIn(0f, 1f))),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = FluentIcon.Delete,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(20.dp)
                        .scale(if (offsetX > threshold) 1.15f else 1f)
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
                            if (offsetX > threshold) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isDismissed = true
                            } else {
                                offsetX = 0f
                            }
                            thresholdCrossed = false
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta).coerceAtLeast(0f)
                            // Tick a light haptic exactly once when the drag
                            // first crosses the dismiss threshold, so the
                            // user feels the "point of no return" — a small
                            // but very Windows-11 tactile detail.
                            if (offsetX > threshold && !thresholdCrossed) {
                                thresholdCrossed = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (offsetX <= threshold) {
                                thresholdCrossed = false
                            }
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
                    .background(bluebirdColors.AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcon.Alert,
                    contentDescription = null,
                    tint     = bluebirdColors.AccentBlue,
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
                    text = remember(notification.time) {
                        notifTimeFormat.format(java.util.Date(notification.time))
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label         = "notifActionPress"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color    = bluebirdColors.AccentBlue,
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
    } catch (e: Exception) { bluebirdColors.AccentBlue }

    val bg     = accent.copy(alpha = if (isDark) 0.12f else 0.1f)
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

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
                imageVector = FluentIcon.Dismiss,
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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(260),
        label         = "emptyFadeIn"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = FluentIcon.AlertOff,
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
