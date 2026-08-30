package io.github.norbertweb.bluebird.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons come from the shared FluentIcon object (FluentIcon.kt), which wraps
// the io.github.niyajali:fluentui-system-icons Compose Multiplatform library.
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.RealNotification
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ─── Unified Toast Model ───────────────────────────────────────────────────────
//
// Both your notification sources (device notifications + Bluebird remote
// announcements) get mapped into this one shape so a single toast host/renderer
// can serve either.

data class ToastNotifData(
    val id: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val accent: Color = bluebirdColors.AccentBlue,
    val actionLabel: String? = null,
    val actionUrl: String? = null,
    val packageName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

fun RealNotification.toToastData(): ToastNotifData = ToastNotifData(
    id          = id,
    appLabel    = appName,
    title       = title,
    body        = body,
    accent      = bluebirdColors.AccentBlue,
    packageName = packageName,
    createdAt   = time
)

fun BluebirdRemoteNotification.toToastData(): ToastNotifData {
    val accent = try {
        Color(android.graphics.Color.parseColor(badgeColor))
    } catch (e: Exception) { bluebirdColors.AccentBlue }

    return ToastNotifData(
        id          = id,
        appLabel    = "Bluebird",
        title       = title,
        body        = body,
        accent      = accent,
        actionLabel = actionLabel,
        actionUrl   = actionUrl
    )
}

// ─── Toast Host ────────────────────────────────────────────────────────────────
//
// Overlay this near the top of your screen/window Z-order (above everything
// else, e.g. as the last child in your root Box) so toasts float above all
// launcher content. Newest toast renders closest to the corner, matching
// Windows 11's stacking behavior.

@Composable
fun NotificationToastHost(
    toasts: List<ToastNotifData>,
    isDark: Boolean,
    textScale: Float,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    autoDismissMillis: Long = 5000L,
    onDismiss: (String) -> Unit,
    onOpen: (ToastNotifData) -> Unit,
    onAction: (String) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .width(340.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            toasts.takeLast(maxVisible).forEach { toast ->
                key(toast.id) {
                    bluebirdToast(
                        toast             = toast,
                        isDark            = isDark,
                        textScale         = textScale,
                        autoDismissMillis = autoDismissMillis,
                        onDismiss         = { onDismiss(toast.id) },
                        onOpen            = { onOpen(toast) },
                        onAction          = onAction
                    )
                }
            }
        }
    }
}

// ─── Single Toast ──────────────────────────────────────────────────────────────

@Composable
private fun bluebirdToast(
    toast: ToastNotifData,
    isDark: Boolean,
    textScale: Float,
    autoDismissMillis: Long,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAction: (String) -> Unit
) {
    var entered           by remember { mutableStateOf(false) }
    var exiting            by remember { mutableStateOf(false) }
    var dragOffsetX        by remember { mutableStateOf(0f) }
    var isPaused           by remember { mutableStateOf(false) }
    var remainingFraction  by remember { mutableStateOf(1f) }
    val dismissThreshold   = 260f

    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight

    LaunchedEffect(Unit) { entered = true }

    fun startExit() {
        if (!exiting) exiting = true
    }

    // Countdown that drives the auto-dismiss + the progress strip. Pauses
    // while the user is dragging the toast.
    LaunchedEffect(toast.id) {
        val tick = 50L
        var elapsedMs = 0L
        while (isActive && elapsedMs < autoDismissMillis && !exiting) {
            delay(tick)
            if (!isPaused) {
                elapsedMs += tick
                remainingFraction = (1f - elapsedMs.toFloat() / autoDismissMillis).coerceIn(0f, 1f)
            }
        }
        startExit()
    }

    // Give the exit animation time to play before removing from the host.
    LaunchedEffect(exiting) {
        if (exiting) {
            delay(200)
            onDismiss()
        }
    }

    val enterOffset by animateFloatAsState(
        targetValue     = if (entered) 0f else 90f,
        animationSpec   = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label           = "toastEnter"
    )
    val exitOffset by animateFloatAsState(
        targetValue   = if (exiting) 420f else 0f,
        animationSpec = tween(200),
        label         = "toastExit"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (entered && !exiting) 1f else 0f,
        animationSpec = tween(if (exiting) 180 else 220),
        label         = "toastAlpha"
    )
    val totalOffset = enterOffset + exitOffset + dragOffsetX

    AcrylicSurface(
        modifier = Modifier
            .alpha(alpha)
            .offset { IntOffset(totalOffset.toInt(), 0) }
            .width(340.dp)
            .pointerInput(toast.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        isPaused = false
                        if (dragOffsetX > dismissThreshold) startExit() else dragOffsetX = 0f
                    },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        isPaused = true
                        dragOffsetX = (dragOffsetX + delta).coerceAtLeast(0f)
                    }
                )
            },
        isDark       = isDark,
        alpha        = 0.97f,
        cornerRadius = 10.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 12.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(toast.accent.copy(alpha = if (isDark) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FluentIcon.Alert,
                        contentDescription = null,
                        tint     = toast.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = toast.appLabel.uppercase(),
                        color      = textColor.copy(alpha = 0.45f),
                        fontSize   = (9 * textScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text       = toast.title,
                        color      = textColor,
                        fontSize   = (12.5f * textScale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = toast.body,
                        color    = textColor.copy(alpha = 0.72f),
                        fontSize = (11.5f * textScale).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val primaryLabel = toast.actionLabel ?: if (toast.packageName != null) "Open" else null
                    if (primaryLabel != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(toast.accent)
                                .clickable {
                                    if (toast.actionUrl != null) onAction(toast.actionUrl) else onOpen()
                                    startExit()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                primaryLabel,
                                color      = Color.White,
                                fontSize   = (11 * textScale).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { startExit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FluentIcon.Dismiss,
                        contentDescription = "Dismiss",
                        tint     = textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Thin countdown strip along the bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(toast.accent.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(remainingFraction)
                        .background(toast.accent.copy(alpha = 0.6f))
                )
            }
        }
    }
}
