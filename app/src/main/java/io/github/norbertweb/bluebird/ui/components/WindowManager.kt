package io.github.norbertweb.bluebird.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluebirdlauncher.ui.screens.BrowserScreen
import io.github.norbertweb.bluebird.CopyJobStatus
import io.github.norbertweb.bluebird.CopyOpType
import io.github.norbertweb.bluebird.LauncherScreen
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.WindowIconKey
import io.github.norbertweb.bluebird.WindowState
import io.github.norbertweb.bluebird.editor.ui.screens.PremiumTextEditorScreen
import io.github.norbertweb.bluebird.ui.screens.CalculatorScreen
import io.github.norbertweb.bluebird.ui.screens.CalendarScreen
import io.github.norbertweb.bluebird.ui.screens.FileExplorerScreen
import io.github.norbertweb.bluebird.ui.screens.ImageViewerScreen
import io.github.norbertweb.bluebird.ui.screens.MediaPlayerScreen
import io.github.norbertweb.bluebird.ui.screens.MessagesScreen
import io.github.norbertweb.bluebird.wordprocessor.PhoneScreen
import io.github.norbertweb.bluebird.ui.screens.RecycleBinScreen
import io.github.norbertweb.bluebird.ui.screens.SettingsScreen
import io.github.norbertweb.bluebird.ui.screens.TaskManagerScreen
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Window size constraints (dp)
// ─────────────────────────────────────────────────────────────────────────────
private const val MIN_WINDOW_W = 320f
private const val MIN_WINDOW_H = 260f
private const val MAX_WINDOW_W = 1600f
private const val MAX_WINDOW_H = 1000f

// How many dp from an edge counts as the resize handle zone
private const val RESIZE_HANDLE_DP = 10f

// Long-press threshold for snap layout picker (ms)
private const val SNAP_LONG_PRESS_MS = 500L

// ─────────────────────────────────────────────────────────────────────────────
// Resize edge / corner enum
// ─────────────────────────────────────────────────────────────────────────────
private enum class ResizeEdge {
    NONE,
    LEFT, RIGHT, TOP, BOTTOM,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

// ─────────────────────────────────────────────────────────────────────────────
// Snap layout slots — mirrors Windows 11's snap assist grid
// ─────────────────────────────────────────────────────────────────────────────
enum class SnapLayout {
    LEFT_HALF,
    RIGHT_HALF,
    TOP_HALF,
    BOTTOM_HALF,
    TOP_LEFT_QUARTER,
    TOP_RIGHT_QUARTER,
    BOTTOM_LEFT_QUARTER,
    BOTTOM_RIGHT_QUARTER,
    CENTER_TWO_THIRDS
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon helper
// ─────────────────────────────────────────────────────────────────────────────
// Icon lookups (including `iconForKey`) now come from the shared FluentIcon.kt,
// used by every UI file in this package. This also fixes a real inconsistency:
// this file used to map MEDIA_PLAYER to a music-note icon while Taskbar.kt used
// a play icon for the same window — they now agree (FluentIcon.PlayCircle).

// ─────────────────────────────────────────────────────────────────────────────
// Default window sizes per screen type
// ─────────────────────────────────────────────────────────────────────────────
private fun defaultSizeFor(screen: LauncherScreen): Pair<Float, Float> = when (screen) {
    LauncherScreen.CALCULATOR      -> 420f to 540f
    LauncherScreen.WORD_IMPRESS           -> 420f to 600f
    LauncherScreen.BLUEBIRD_STORE        -> 500f to 560f
    LauncherScreen.CALENDAR        -> 560f to 480f
    LauncherScreen.TERMINAL        -> 700f to 480f
    LauncherScreen.WEB_APP_MANAGER -> 800f to 560f
    LauncherScreen.COPY_PROGRESS   -> 420f to 280f
    else                           -> 750f to 520f
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowSize — passed into each screen so it can respond to user resizing
// ─────────────────────────────────────────────────────────────────────────────
data class WindowSize(
    val widthDp: Dp,
    val heightDp: Dp,
    val isCompact: Boolean  = false,
    val isMedium: Boolean   = false,
    val isExpanded: Boolean = false
) {
    companion object {
        fun from(widthDp: Dp, heightDp: Dp): WindowSize {
            val w = widthDp.value
            return WindowSize(
                widthDp    = widthDp,
                heightDp   = heightDp,
                isCompact  = w < 480f,
                isMedium   = w in 480f..720f,
                isExpanded = w > 720f
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-window persisted geometry — stored in ViewModel so closing and reopening
// a window restores the last position and size.
// ─────────────────────────────────────────────────────────────────────────────
/** Runtime visibility contract for a window's content. Closed windows are removed
 * from WindowManager and therefore disposed. Minimized windows remain alive, but
 * expensive UI-only work should suspend while they are hidden. */
data class WindowRuntimeState(
    val isMinimized: Boolean = false,
    val isActive: Boolean = true
)

val LocalWindowRuntime = compositionLocalOf { WindowRuntimeState() }

data class WindowGeometry(
    val offsetX: Float,
    val offsetY: Float,
    val widthDp: Float,
    val heightDp: Float
)

// ─────────────────────────────────────────────────────────────────────────────
// WindowManager — manages the full z-ordered stack
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowManager(
    windows: List<WindowState>,
    activeWindowId: String?,
    isDark: Boolean,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        windows.forEach { window ->
            key(window.id) {
                val isMinimized = window.isMinimized

                val animatedAlpha by animateFloatAsState(
                    targetValue   = if (isMinimized) 0f else 1f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label         = "windowAlpha_${window.id}"
                )
                val animatedScaleY by animateFloatAsState(
                    targetValue   = if (isMinimized) 0.92f else 1f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label         = "windowScaleY_${window.id}"
                )

                // ── Minimized window touch fix ────────────────────────────────
                // graphicsLayer(alpha=0) hides the window visually but the Box
                // still occupies its full layout area and intercepts all touches.
                // Fix: collapse to 0×0 so the layout system gives it zero
                // hit-test area. The inner content stays fully composed
                // (MediaPlayer keeps playing, Calculator keeps state) and is
                // rendered via graphicsLayer with clip=false — but alpha=0 keeps
                // it invisible. Desktop receives all touches normally.
                // ─────────────────────────────────────────────────────────────
                Box(
                    modifier = if (isMinimized)
                        Modifier.size(0.dp)
                    else
                        Modifier
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha           = animatedAlpha
                            scaleY          = animatedScaleY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            clip            = false
                        }
                    ) {
                        FloatingWindow(
                            windowState = window,
                            isActive    = window.id == activeWindowId,
                            isDark      = isDark,
                            viewModel   = viewModel,
                            onClose     = { viewModel.closeWindow(window.id) },
                            onMinimize  = { viewModel.minimizeWindow(window.id) },
                            onMaximize  = { viewModel.maximizeWindow(window.id) },
                            onFocus     = { viewModel.setActiveWindow(window.id) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FloatingWindow — owns position AND size state so resizing never restarts
// the window's content composables (MediaPlayer stays playing, Calculator
// keeps its expression, etc.)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FloatingWindow(
    windowState: WindowState,
    isActive: Boolean,
    isDark: Boolean,
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onFocus: () -> Unit
) {
    val density = LocalDensity.current

    // ── Restore last geometry if available, else use defaults ─────────────────
    val savedGeometry = remember(windowState.id) {
        viewModel.getWindowGeometry(windowState.id)
    }
    val (defaultW, defaultH) = remember(windowState.screen) { defaultSizeFor(windowState.screen) }

    var offsetX        by remember { mutableStateOf(savedGeometry?.offsetX ?: 80f) }
    var offsetY        by remember { mutableStateOf(savedGeometry?.offsetY ?: 40f) }
    var windowWidthDp  by remember { mutableStateOf(savedGeometry?.widthDp  ?: defaultW) }
    var windowHeightDp by remember { mutableStateOf(savedGeometry?.heightDp ?: defaultH) }

    // ── Persist geometry after the user pauses, rather than once per drag frame.
    // Dragging can produce dozens of state changes per second; the old keyed
    // LaunchedEffect restarted a coroutine and wrote the geometry for every one.
    LaunchedEffect(windowState.id) {
        snapshotFlow {
            WindowGeometry(offsetX, offsetY, windowWidthDp, windowHeightDp)
        }
            .distinctUntilChanged()
            .debounce(150)
            .collect { geometry ->
                viewModel.saveWindowGeometry(windowState.id, geometry)
            }
    }

    // ── Snap layout picker visibility ─────────────────────────────────────────
    var showSnapPicker by remember { mutableStateOf(false) }

    // ── Snap-zone highlight (accent border while dragging near an edge) ────────
    var isSnapping by remember { mutableStateOf(false) }

    // ── Parent canvas size — measured so we can clamp drag within bounds ───────
    var canvasWidthPx  by remember { mutableStateOf(0) }
    var canvasHeightPx by remember { mutableStateOf(0) }

    // ── PiP (Picture-in-Picture) mode ─────────────────────────────────────────
    var isPip by remember { mutableStateOf(false) }
    val pipW = 220f
    val pipH = 130f

    // ── Always-on-top flag ────────────────────────────────────────────────────
    var alwaysOnTop by remember { mutableStateOf(false) }

    // ── Per-window opacity (premium) ──────────────────────────────────────────
    var windowOpacity by remember { mutableStateOf(1f) }

    // ── Context menu ──────────────────────────────────────────────────────────
    var showContextMenu by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue   = if (isActive) 24.dp else 8.dp,
        label         = "elevation"
    )
    val windowBg = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val borderColor = if (isActive)
        bluebirdColors.AccentBlue.copy(alpha = if (isSnapping) 0.9f else 0.45f)
    else
        Color.White.copy(alpha = 0.1f)
    val borderWidth = if (isActive) (if (isSnapping) 2.dp else 1.dp) else 0.5.dp

    // ── Effective size: PiP shrinks window to thumbnail ───────────────────────
    val effectiveW = if (isPip) pipW else if (windowState.isMaximized)
        with(density) { canvasWidthPx.toDp().value } else windowWidthDp
    val effectiveH = if (isPip) pipH else if (windowState.isMaximized)
        with(density) { canvasHeightPx.toDp().value } else windowHeightDp

    val animOffsetX by animateFloatAsState(
        targetValue   = if (windowState.isMaximized && !isPip) 0f else offsetX,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winOffsetX_${windowState.id}"
    )
    val animOffsetY by animateFloatAsState(
        targetValue   = if (windowState.isMaximized && !isPip) 0f else offsetY,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winOffsetY_${windowState.id}"
    )
    val animW by animateFloatAsState(
        targetValue   = effectiveW,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winW_${windowState.id}"
    )
    val animH by animateFloatAsState(
        targetValue   = effectiveH,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winH_${windowState.id}"
    )
    val cornerRadius = if (windowState.isMaximized && !isPip) 0.dp else 10.dp
    val animCorner by animateDpAsState(
        targetValue   = cornerRadius,
        animationSpec = tween(200),
        label         = "winCorner_${windowState.id}"
    )

    // ── Canvas size clamp: re-clamp window position if canvas shrinks ──────────
    LaunchedEffect(canvasWidthPx, canvasHeightPx) {
        if (canvasWidthPx > 0 && canvasHeightPx > 0) {
            val maxX = with(density) { canvasWidthPx.toDp().value } - windowWidthDp
            val maxY = with(density) { canvasHeightPx.toDp().value } - windowHeightDp
            offsetX = offsetX.coerceIn(0f, maxX.coerceAtLeast(0f))
            offsetY = offsetY.coerceIn(0f, maxY.coerceAtLeast(0f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                canvasWidthPx  = coords.size.width
                canvasHeightPx = coords.size.height
            }
    ) {
        // ── Single always-composed window Box ─────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(animOffsetX.toInt(), animOffsetY.toInt()) }
                .width(animW.dp)
                .height(animH.dp)
                .graphicsLayer { alpha = windowOpacity }
                .shadow(elevation, RoundedCornerShape(animCorner))
                .clip(RoundedCornerShape(animCorner))
                .background(windowBg, RoundedCornerShape(animCorner))
                .border(borderWidth, borderColor, RoundedCornerShape(animCorner))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { onFocus() })
                }
                .pointerInput(Unit) {
                    val handlePx = with(density) { RESIZE_HANDLE_DP.dp.toPx() }
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down  = event.changes.firstOrNull() ?: continue
                            if (!down.changedToDown()) continue

                            val edge = detectEdge(
                                pos      = down.position,
                                w        = size.width.toFloat(),
                                h        = size.height.toFloat(),
                                handlePx = handlePx
                            )

                            if (edge == ResizeEdge.NONE || windowState.isMaximized || isPip ||
                                windowState.screen == LauncherScreen.COPY_PROGRESS) continue

                            onFocus()
                            down.consume()

                            var prevPos = down.position
                            while (true) {
                                val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                val change    = dragEvent.changes.firstOrNull() ?: break
                                if (!change.pressed) break

                                val delta = change.position - prevPos
                                prevPos   = change.position

                                val dx = with(density) { delta.x.toDp().value }
                                val dy = with(density) { delta.y.toDp().value }

                                applyResize(
                                    edge       = edge,
                                    dx         = dx, dy = dy,
                                    offsetXRef = { offsetX = (offsetX + it).coerceAtLeast(0f) },
                                    offsetYRef = { offsetY = (offsetY + it).coerceAtLeast(0f) },
                                    widthRef   = { windowWidthDp  = (windowWidthDp  + it).coerceIn(MIN_WINDOW_W, MAX_WINDOW_W) },
                                    heightRef  = { windowHeightDp = (windowHeightDp + it).coerceIn(MIN_WINDOW_H, MAX_WINDOW_H) }
                                )
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val winSize = WindowSize.from(animW.dp, animH.dp)

            if (!isPip) {
                // ── Full window content ───────────────────────────────────────
                CompositionLocalProvider(
                    LocalWindowRuntime provides WindowRuntimeState(
                        isMinimized = windowState.isMinimized,
                        isActive = isActive
                    )
                ) {
                    WindowContent(
                        windowState     = windowState,
                    windowSize      = winSize,
                    isDark          = isDark,
                    viewModel       = viewModel,
                    alwaysOnTop     = alwaysOnTop,
                    windowOpacity   = windowOpacity,
                    isPip           = false,
                    onClose         = onClose,
                    onMinimize      = onMinimize,
                    onMaximize      = {
                        showSnapPicker = false
                        onMaximize()
                    },
                    onSnapPickerToggle = { showSnapPicker = !showSnapPicker },
                    onPip           = { isPip = true },
                    onAlwaysOnTop   = { alwaysOnTop = !alwaysOnTop },
                    onOpacityChange = { windowOpacity = it },
                    onContextMenu   = { showContextMenu = !showContextMenu },
                    onDrag          = if (windowState.isMaximized) null else { dx, dy ->
                        val newX = offsetX + with(density) { dx.toDp().value }
                        val newY = offsetY + with(density) { dy.toDp().value }
                        val maxX = with(density) { canvasWidthPx.toDp().value } - windowWidthDp
                        val maxY = with(density) { canvasHeightPx.toDp().value } - windowHeightDp

                        isSnapping = newX < 20f || newX > maxX - 20f ||
                                newY < 20f || newY > maxY - 20f

                        if (newY < -10f) {
                            onMaximize()
                        } else {
                            offsetX = newX.coerceIn(0f, maxX.coerceAtLeast(0f))
                            offsetY = newY.coerceIn(0f, maxY.coerceAtLeast(40f))
                        }
                    },
                        onDragEnd = { isSnapping = false }
                    )
                }

                if (!windowState.isMaximized) {
                    ResizeHandles(isDark = isDark)
                }
            } else {
                // ── PiP thumbnail mode ────────────────────────────────────────
                PipThumbnail(
                    windowState = windowState,
                    isDark      = isDark,
                    onExpand    = { isPip = false },
                    onDrag      = { dx, dy ->
                        val newX = offsetX + with(density) { dx.toDp().value }
                        val newY = offsetY + with(density) { dy.toDp().value }
                        val maxX = with(density) { canvasWidthPx.toDp().value } - pipW
                        val maxY = with(density) { canvasHeightPx.toDp().value } - pipH
                        offsetX = newX.coerceIn(0f, maxX.coerceAtLeast(0f))
                        offsetY = newY.coerceIn(0f, maxY.coerceAtLeast(0f))
                    }
                )
            }
        }

        // ── Snap Layout Picker overlay (anchored to the window's maximize button) ─
        if (showSnapPicker) {
            SnapLayoutPicker(
                isDark          = isDark,
                canvasW         = with(density) { canvasWidthPx.toDp().value },
                canvasH         = with(density) { canvasHeightPx.toDp().value },
                anchorX         = animOffsetX + animW - 80f, // near maximize btn
                anchorY         = animOffsetY + 34f,
                onDismiss       = { showSnapPicker = false },
                onLayoutSelected = { layout ->
                    showSnapPicker = false
                    // Restore if maximized before applying snap
                    if (windowState.isMaximized) onMaximize()
                    applySnapLayout(
                        layout         = layout,
                        canvasW        = with(density) { canvasWidthPx.toDp().value },
                        canvasH        = with(density) { canvasHeightPx.toDp().value },
                        setOffsetX     = { offsetX = it },
                        setOffsetY     = { offsetY = it },
                        setWindowW     = { windowWidthDp = it },
                        setWindowH     = { windowHeightDp = it }
                    )
                }
            )
        }

        // ── Context menu overlay ───────────────────────────────────────────────
        if (showContextMenu) {
            WindowContextMenu(
                isDark          = isDark,
                alwaysOnTop     = alwaysOnTop,
                windowOpacity   = windowOpacity,
                anchorX         = animOffsetX + 10f,
                anchorY         = animOffsetY + 34f,
                onDismiss       = { showContextMenu = false },
                onMinimize      = { showContextMenu = false; onMinimize() },
                onMaximize      = { showContextMenu = false; onMaximize() },
                onClose         = { showContextMenu = false; onClose() },
                onToggleAlwaysOnTop = { alwaysOnTop = !alwaysOnTop },
                onOpacityChange = { windowOpacity = it },
                onPip           = { showContextMenu = false; isPip = true }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Snap layout math — maps a SnapLayout to concrete position + size
// ─────────────────────────────────────────────────────────────────────────────
private fun applySnapLayout(
    layout: SnapLayout,
    canvasW: Float, canvasH: Float,
    setOffsetX: (Float) -> Unit,
    setOffsetY: (Float) -> Unit,
    setWindowW: (Float) -> Unit,
    setWindowH: (Float) -> Unit
) {
    val halfW = canvasW / 2f
    val halfH = canvasH / 2f
    val twoThirdsW = canvasW * 2f / 3f

    when (layout) {
        SnapLayout.LEFT_HALF           -> { setOffsetX(0f);     setOffsetY(0f);     setWindowW(halfW);       setWindowH(canvasH) }
        SnapLayout.RIGHT_HALF          -> { setOffsetX(halfW);  setOffsetY(0f);     setWindowW(halfW);       setWindowH(canvasH) }
        SnapLayout.TOP_HALF            -> { setOffsetX(0f);     setOffsetY(0f);     setWindowW(canvasW);     setWindowH(halfH) }
        SnapLayout.BOTTOM_HALF         -> { setOffsetX(0f);     setOffsetY(halfH);  setWindowW(canvasW);     setWindowH(halfH) }
        SnapLayout.TOP_LEFT_QUARTER    -> { setOffsetX(0f);     setOffsetY(0f);     setWindowW(halfW);       setWindowH(halfH) }
        SnapLayout.TOP_RIGHT_QUARTER   -> { setOffsetX(halfW);  setOffsetY(0f);     setWindowW(halfW);       setWindowH(halfH) }
        SnapLayout.BOTTOM_LEFT_QUARTER -> { setOffsetX(0f);     setOffsetY(halfH);  setWindowW(halfW);       setWindowH(halfH) }
        SnapLayout.BOTTOM_RIGHT_QUARTER-> { setOffsetX(halfW);  setOffsetY(halfH);  setWindowW(halfW);       setWindowH(halfH) }
        SnapLayout.CENTER_TWO_THIRDS   -> { setOffsetX((canvasW - twoThirdsW) / 2f); setOffsetY(0f); setWindowW(twoThirdsW); setWindowH(canvasH) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SnapLayoutPicker — Windows 11-style snap assist grid popup
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SnapLayoutPicker(
    isDark: Boolean,
    canvasW: Float,
    canvasH: Float,
    anchorX: Float,
    anchorY: Float,
    onDismiss: () -> Unit,
    onLayoutSelected: (SnapLayout) -> Unit
) {
    val bg     = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val accent = bluebirdColors.AccentBlue
    val cell   = if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)
    val hover  = accent.copy(alpha = 0.7f)

    // Dismiss on outside tap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(anchorX.toInt(), anchorY.toInt()) }
            .shadow(12.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .pointerInput(Unit) { detectTapGestures { /* absorb */ } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text       = "Snap Layout",
                color      = if (isDark) Color.White.copy(0.7f) else Color(0xFF333333),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(bottom = 2.dp)
            )

            // Row 1 — halves
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SnapCell(label = "Left ½",   color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.LEFT_HALF) })
                SnapCell(label = "Right ½",  color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.RIGHT_HALF) })
                SnapCell(label = "Top ½",    color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.TOP_HALF) })
                SnapCell(label = "Bottom ½", color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.BOTTOM_HALF) })
            }
            // Row 2 — quarters
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SnapCell(label = "↖",  color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.TOP_LEFT_QUARTER) })
                SnapCell(label = "↗",  color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.TOP_RIGHT_QUARTER) })
                SnapCell(label = "↙",  color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.BOTTOM_LEFT_QUARTER) })
                SnapCell(label = "↘",  color = cell, hover = hover, onClick = { onLayoutSelected(SnapLayout.BOTTOM_RIGHT_QUARTER) })
            }
            // Row 3 — center wide
            Row {
                SnapCell(label = "Center ⅔", color = cell, hover = hover,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onLayoutSelected(SnapLayout.CENTER_TWO_THIRDS) })
            }
        }
    }
}

@Composable
private fun SnapCell(
    label: String,
    color: Color,
    hover: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .width(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) hover else color)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit  -> hovered = false
                        }
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures { onClick() } }
    ) {
        Text(label, fontSize = 10.sp, color = if (hovered) Color.White else Color.Unspecified)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowContextMenu — right-click / long-press on title bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WindowContextMenu(
    isDark: Boolean,
    alwaysOnTop: Boolean,
    windowOpacity: Float,
    anchorX: Float,
    anchorY: Float,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onToggleAlwaysOnTop: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onPip: () -> Unit
) {
    val bg      = if (isDark) Color(0xFF2C2C2C) else Color.White
    val itemCol = if (isDark) Color.White else Color(0xFF1A1A1A)
    val divider = if (isDark) Color(0xFF3F3F3F) else Color(0xFFE0E0E0)

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } })

    Box(
        modifier = Modifier
            .offset { IntOffset(anchorX.toInt(), anchorY.toInt()) }
            .width(220.dp)
            .shadow(16.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
            .padding(6.dp)
            .pointerInput(Unit) { detectTapGestures { /* absorb */ } }
    ) {
        Column {
            ContextMenuItem("Minimize",       itemCol, onClick = onMinimize)
            ContextMenuItem("Maximize / Restore", itemCol, onClick = onMaximize)
            ContextMenuItem("Picture-in-Picture 📌", itemCol, onClick = onPip)
            Box(Modifier.fillMaxWidth().height(1.dp).background(divider).padding(vertical = 2.dp))
            ContextMenuItem(
                label   = if (alwaysOnTop) "✓ Always on Top" else "Always on Top",
                color   = itemCol,
                onClick = onToggleAlwaysOnTop
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(divider).padding(vertical = 2.dp))
            // Opacity slider
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Opacity: ${(windowOpacity * 100).toInt()}%",
                    color = itemCol.copy(alpha = 0.7f), fontSize = 11.sp)
                // Simple tap-based opacity steps (full slider requires Material3 Slider)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf(0.3f, 0.5f, 0.7f, 0.85f, 1f).forEach { v ->
                        val isSelected = windowOpacity == v
                        Box(
                            modifier = Modifier
                                .size(28.dp, 20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) bluebirdColors.AccentBlue else divider)
                                .pointerInput(Unit) { detectTapGestures { onOpacityChange(v) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${(v * 100).toInt()}", fontSize = 9.sp,
                                color = if (isSelected) Color.White else itemCol)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(divider).padding(vertical = 2.dp))
            ContextMenuItem("Close",          Color(0xFFE74C3C), onClick = onClose)
        }
    }
}

@Composable
private fun ContextMenuItem(label: String, color: Color, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) bluebirdColors.AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit  -> hovered = false
                        }
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures { onClick() } }
    ) {
        Text(label, color = color, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PiP thumbnail — a draggable mini preview with an expand button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PipThumbnail(
    windowState: WindowState,
    isDark: Boolean,
    onExpand: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val bg   = if (isDark) Color(0xFF2A2A2A) else Color(0xFFEEEEEE)
    val icon = remember(windowState.iconKey) { iconForKey(windowState.iconKey) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) {
                detectDragGestures { _, delta -> onDrag(delta.x, delta.y) }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onExpand() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null,
                tint = if (isDark) Color.White.copy(0.7f) else Color(0xFF444444),
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(windowState.title, fontSize = 10.sp,
                color = if (isDark) Color.White.copy(0.6f) else Color(0xFF555555),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bluebirdColors.AccentBlue)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .pointerInput(Unit) { detectTapGestures { onExpand() } }
            ) {
                Text("Expand", fontSize = 9.sp, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers for resize math
// ─────────────────────────────────────────────────────────────────────────────
private fun detectEdge(pos: Offset, w: Float, h: Float, handlePx: Float): ResizeEdge {
    val nearLeft   = pos.x < handlePx
    val nearRight  = pos.x > w - handlePx
    val nearTop    = pos.y < handlePx
    val nearBottom = pos.y > h - handlePx

    return when {
        nearTop    && nearLeft  -> ResizeEdge.TOP_LEFT
        nearTop    && nearRight -> ResizeEdge.TOP_RIGHT
        nearBottom && nearLeft  -> ResizeEdge.BOTTOM_LEFT
        nearBottom && nearRight -> ResizeEdge.BOTTOM_RIGHT
        nearLeft                -> ResizeEdge.LEFT
        nearRight               -> ResizeEdge.RIGHT
        nearTop                 -> ResizeEdge.TOP
        nearBottom              -> ResizeEdge.BOTTOM
        else                    -> ResizeEdge.NONE
    }
}

private fun applyResize(
    edge: ResizeEdge,
    dx: Float, dy: Float,
    offsetXRef: (Float) -> Unit,
    offsetYRef: (Float) -> Unit,
    widthRef:   (Float) -> Unit,
    heightRef:  (Float) -> Unit
) {
    when (edge) {
        ResizeEdge.RIGHT         -> widthRef(dx)
        ResizeEdge.BOTTOM        -> heightRef(dy)
        ResizeEdge.LEFT          -> { offsetXRef(dx); widthRef(-dx) }
        ResizeEdge.TOP           -> { offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.TOP_LEFT      -> { offsetXRef(dx); widthRef(-dx); offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.TOP_RIGHT     -> { widthRef(dx);   offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.BOTTOM_LEFT   -> { offsetXRef(dx); widthRef(-dx); heightRef(dy) }
        ResizeEdge.BOTTOM_RIGHT  -> { widthRef(dx);   heightRef(dy) }
        ResizeEdge.NONE          -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resize handle affordances — all four corners + edge grips
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.ResizeHandles(isDark: Boolean) {
    val handleColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)

    // All four corners (TOP_LEFT was previously missing — now included)
    listOf(
        Alignment.BottomEnd,
        Alignment.BottomStart,
        Alignment.TopEnd,
        Alignment.TopStart          // ← was missing in original
    ).forEach { alignment ->
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(alignment)
                .padding(2.dp)
                .background(handleColor, RoundedCornerShape(2.dp))
        )
    }
    // Right edge mid-point grip
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(4.dp)
            .height(24.dp)
            .background(handleColor, RoundedCornerShape(2.dp))
    )
    // Bottom edge mid-point grip
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .width(24.dp)
            .height(4.dp)
            .background(handleColor, RoundedCornerShape(2.dp))
    )
    // Left edge mid-point grip
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .width(4.dp)
            .height(24.dp)
            .background(handleColor, RoundedCornerShape(2.dp))
    )
    // Top edge mid-point grip
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .width(24.dp)
            .height(4.dp)
            .background(handleColor, RoundedCornerShape(2.dp))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowContent — title bar + screen content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowContent(
    windowState: WindowState,
    windowSize: WindowSize,
    isDark: Boolean,
    viewModel: LauncherViewModel,
    alwaysOnTop: Boolean,
    windowOpacity: Float,
    isPip: Boolean,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onSnapPickerToggle: () -> Unit,
    onPip: () -> Unit,
    onAlwaysOnTop: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onContextMenu: () -> Unit,
    onDrag: ((Float, Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Title bar ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onDrag != null) Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onDragEnd?.invoke() },
                            onDrag    = { _, d -> onDrag(d.x, d.y) }
                        )
                    } else Modifier
                )
                // Long-press on title bar opens context menu
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onContextMenu() }
                    )
                }
        ) {
            WindowTitleBar(
                title              = windowState.title,
                iconKey            = windowState.iconKey,
                customIconPath     = windowState.customIconPath,
                isDark             = isDark,
                windowSize         = windowSize,
                isMaximized        = windowState.isMaximized,
                alwaysOnTop        = alwaysOnTop,
                canMaximize        = windowState.screen != LauncherScreen.COPY_PROGRESS,
                onMinimize         = onMinimize,
                onMaximize         = onMaximize,
                onSnapPickerToggle = onSnapPickerToggle,
                onClose            = onClose
            )
        }

        // ── Screen content ────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val extras = windowState.extras
            when (windowState.screen) {
                LauncherScreen.PremiumTextEditorScreen -> PremiumTextEditorScreen(isDark, filePath = extras["filePath"] ?: "")
                LauncherScreen.SETTINGS      -> SettingsScreen(isDark, viewModel)
                LauncherScreen.FILE_EXPLORER -> FileExplorerScreen(isDark, viewModel, startPath = extras["path"])
                LauncherScreen.BROWSER       -> BrowserScreen(isDark)
                LauncherScreen.CALCULATOR    -> CalculatorScreen(isDark)
                LauncherScreen.CALENDAR      -> CalendarScreen(isDark)
                //LauncherScreen.PHOTOS        -> PhotosScreen(isDark)
                LauncherScreen.TASK_MANAGER  -> TaskManagerScreen(isDark)
                LauncherScreen.MEDIA_PLAYER  -> {
                    val filePath = remember(windowState.id) { extras["filePath"] ?: "" }
                    MediaPlayerScreen(isDark, filePath)
                }
                LauncherScreen.IMAGE_VIEWER  -> {
                    val filePath = remember(windowState.id) { extras["filePath"] ?: "" }
                    ImageViewerScreen(isDark, filePath, viewModel)
                }
                LauncherScreen.WORD_IMPRESS      -> PhoneScreen(isDark, initialPath = extras["filePath"] ?: "")
                LauncherScreen.BLUEBIRD_STORE   -> MessagesScreen(isDark)
                LauncherScreen.RECYCLE_BIN -> RecycleBinScreen(isDark, viewModel)
                LauncherScreen.TERMINAL    -> TerminalScreen(isDark)
                LauncherScreen.WEB_APP_MANAGER -> {
                    WebAppManagerScreen(
                        isDark      = isDark,
                        viewModel   = viewModel,
                        onLaunchApp = { app ->
                            // Open a dedicated viewer window for this web app, with its
                            // real favicon (if we have one) as the window/taskbar icon.
                            viewModel.openWindow(
                                screen = LauncherScreen.WEB_APP_VIEWER,
                                extras = mapOf("webAppId" to app.id, "webAppName" to app.name,
                                    "webAppUrl" to app.url, "webAppHtml" to app.htmlContent,
                                    "webAppCustom" to app.isCustom.toString(),
                                    "webAppEmoji" to app.iconEmoji,
                                    "webAppIcon" to app.iconPath,
                                    "webAppAccent" to app.accentColor.toString()),
                                customIconPath = app.iconPath.ifBlank { null }
                            )
                        }
                    )
                }
                LauncherScreen.WEB_APP_VIEWER -> {
                    val app = remember(windowState.id) {
                        io.github.norbertweb.bluebird.ui.components.InstalledWebApp(
                            id          = extras["webAppId"] ?: "",
                            name        = extras["webAppName"] ?: "Web App",
                            url         = extras["webAppUrl"] ?: "https://example.com",
                            iconEmoji   = extras["webAppEmoji"] ?: "🌐",
                            iconPath    = extras["webAppIcon"] ?: "",
                            accentColor = extras["webAppAccent"]?.toLongOrNull() ?: 0xFF0078D4L,
                            isCustom    = extras["webAppCustom"] == "true",
                            htmlContent = extras["webAppHtml"] ?: ""
                        )
                    }
                    WebAppViewerScreen(isDark = isDark, app = app)
                }
                LauncherScreen.COPY_PROGRESS -> CopyProgressScreen(isDark = isDark, viewModel = viewModel)
                else -> {}

            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowTitleBar — Windows 11 style
//   • Icon + title (left-aligned)
//   • Minimize, Maximize/Restore, Close buttons (right-aligned, bluebird look)
//   • Maximize button: tap = maximize/restore, long-press = snap picker
//   • "Always on Top" pin badge shown when active
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowTitleBar(
    title: String,
    iconKey: String = "",
    customIconPath: String? = null,
    isDark: Boolean,
    windowSize: WindowSize = WindowSize(750.dp, 520.dp),
    isMaximized: Boolean = false,
    alwaysOnTop: Boolean = false,
    canMaximize: Boolean = true,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onSnapPickerToggle: () -> Unit,
    onClose: () -> Unit
) {
    val barBg   = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)
    val textCol = if (isDark) Color.White else Color(0xFF1C1C1C)
    val icon    = remember(iconKey) { iconForKey(iconKey) }
    val bmpIcon = rememberWindowBitmapIcon(customIconPath)

    // Show dimensions in compact mode
    val sizeLabel = if (windowSize.isCompact)
        " — ${windowSize.widthDp.value.toInt()}×${windowSize.heightDp.value.toInt()}"
    else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(barBg)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── App icon — real favicon bitmap when available, Material icon otherwise ──
        if (bmpIcon != null) {
            androidx.compose.foundation.Image(
                bitmap             = bmpIcon,
                contentDescription = null,
                modifier           = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
            )
        } else {
            Icon(
                imageVector            = icon,
                contentDescription = null,
                tint               = if (isDark) Color.White.copy(0.75f) else Color(0xFF444444),
                modifier           = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(6.dp))

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text       = title + sizeLabel,
            color      = textCol,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)
        )

        // ── Always-on-top badge ───────────────────────────────────────────────
        if (alwaysOnTop) {
            Text("📌", fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
        }

        // ── Windows 11 style control buttons ─────────────────────────────────
        // Minimize —
        bluebirdTitleButton(
            label   = "—",
            hoverBg = if (isDark) Color(0xFF3A3A3A) else Color(0xFFD0D0D0),
            onClick = onMinimize
        )

        // Maximize / Restore □ (tap) + long-press = snap picker — hidden entirely for
        // windows that don't support it (e.g. the copy/move progress dialog), matching
        // real Windows 11 behavior where non-resizable dialogs simply omit this button.
        if (canMaximize) {
            bluebirdTitleButton(
                label        = if (isMaximized) "❐" else "□",
                hoverBg      = if (isDark) Color(0xFF3A3A3A) else Color(0xFFD0D0D0),
                onClick      = onMaximize,
                onLongPress  = onSnapPickerToggle   // touch-friendly snap picker
            )
        }

        // Close ✕ — red hover
        bluebirdTitleButton(
            label        = "✕",
            hoverBg      = Color(0xFFC42B1C),
            hoverTextCol = Color.White,
            onClick      = onClose
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// bluebirdTitleButton — flat, hover-highlighted, supports long-press
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun bluebirdTitleButton(
    label: String,
    hoverBg: Color,
    hoverTextCol: Color = Color.Unspecified,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    var hovered  by remember { mutableStateOf(false) }
    var pressing by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .background(
                when {
                    pressing -> hoverBg.copy(alpha = 0.85f)
                    hovered  -> hoverBg
                    else     -> Color.Transparent
                }
            )
            // True hover for mouse input
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit  -> { hovered = false; pressing = false }
                        }
                    }
                }
            }
            // Tap + long-press (touch-friendly)
            .pointerInput(onLongPress) {
                detectTapGestures(
                    onPress = {
                        pressing = true
                        tryAwaitRelease()
                        pressing = false
                    },
                    onLongPress = { onLongPress?.invoke() },
                    onTap = { onClick() }
                )
            }
    ) {
        Text(
            text     = label,
            fontSize = 13.sp,
            color    = if ((hovered || pressing) && hoverTextCol != Color.Unspecified)
                hoverTextCol else Color.Unspecified
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CopyProgressScreen — Windows-11-style file operation dialog. Lives inside a
// window that can be dragged and minimized but not maximized or resized (see
// FloatingWindow / WindowTitleBar's canMaximize gating for LauncherScreen.COPY_PROGRESS).
// Shows every active/recent CopyJob from the ViewModel's shared copy engine.
// ─────────────────────────────────────────────────────────────────────────────
private fun formatBytesShort(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L          -> "%.0f KB".format(bytes / 1024.0)
    else                    -> "$bytes B"
}

private fun formatEta(bytesRemaining: Long, bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "Calculating…"
    val secs = bytesRemaining / bytesPerSec
    return when {
        secs < 60   -> "$secs sec remaining"
        secs < 3600 -> "${secs / 60} min remaining"
        else        -> "${secs / 3600} hr ${(secs % 3600) / 60} min remaining"
    }
}

@Composable
fun CopyProgressScreen(isDark: Boolean, viewModel: LauncherViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val jobs = uiState.copyJobs
    val bg   = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val tc   = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val tcDim = if (isDark) Color(0xFF9A9A9A) else Color(0xFF6B6B6B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (jobs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No file operations in progress", color = tcDim, fontSize = 13.sp)
            }
            return@Column
        }
        jobs.forEach { job ->
            key(job.id) {
                val verb = if (job.operation == CopyOpType.MOVE) "Moving" else "Copying"
                val destName = File(job.destDir).name.ifBlank { "Desktop" }
                val progress = if (job.totalBytes > 0) (job.copiedBytes.toFloat() / job.totalBytes).coerceIn(0f, 1f) else 0f

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = FluentIcon.Copy,
                            contentDescription = null,
                            tint = Color(0xFF0078D4),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = when (job.status) {
                                    CopyJobStatus.SCANNING  -> "Preparing to ${verb.lowercase()}…"
                                    CopyJobStatus.RUNNING   -> "$verb ${job.sourceNames.size} item${if (job.sourceNames.size != 1) "s" else ""} to $destName"
                                    CopyJobStatus.DONE      -> "${if (job.operation == CopyOpType.MOVE) "Moved" else "Copied"} to $destName"
                                    CopyJobStatus.CANCELLED -> "Cancelled"
                                    CopyJobStatus.ERROR     -> "Couldn't complete — ${job.error ?: "error"}"
                                },
                                color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (job.status == CopyJobStatus.RUNNING && job.currentFileName.isNotBlank()) {
                                Text(
                                    text = job.currentFileName, color = tcDim, fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (job.status == CopyJobStatus.RUNNING || job.status == CopyJobStatus.SCANNING) {
                            IconButton(onClick = { viewModel.cancelCopyJob(job.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = FluentIcon.Dismiss, contentDescription = "Cancel", tint = tcDim, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            IconButton(onClick = { viewModel.dismissCopyJob(job.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = FluentIcon.Dismiss, contentDescription = "Dismiss", tint = tcDim, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    when (job.status) {
                        CopyJobStatus.SCANNING -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF0078D4)
                        )
                        CopyJobStatus.RUNNING -> {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF0078D4)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${formatBytesShort(job.copiedBytes)} of ${formatBytesShort(job.totalBytes)} " +
                                        "(${job.filesDone}/${job.totalFiles} items)",
                                    color = tcDim, fontSize = 10.sp
                                )
                                Text(
                                    formatEta(job.totalBytes - job.copiedBytes, job.speedBytesPerSec),
                                    color = tcDim, fontSize = 10.sp
                                )
                            }
                        }
                        else -> {}
                    }
                }
                if (job != jobs.last()) {
                    Divider(color = tcDim.copy(alpha = 0.15f))
                }
            }
        }
    }
}


//
// Each screen now receives a `windowSize: WindowSize` parameter.
// Use it like this:
//
//   @Composable
//   fun FileExplorerScreen(isDark: Boolean, viewModel: LauncherViewModel, windowSize: WindowSize) {
//       if (windowSize.isExpanded) {
//           Row {
//               FolderSidebar(modifier = Modifier.width(180.dp))
//               FileGrid(columns = 4, modifier = Modifier.weight(1f))
//           }
//       } else if (windowSize.isMedium) {
//           Row {
//               FolderSidebar(modifier = Modifier.width(120.dp))
//               FileGrid(columns = 3, modifier = Modifier.weight(1f))
//           }
//       } else {
//           FileGrid(columns = 2, modifier = Modifier.fillMaxWidth())
//       }
//   }
//
//   // Settings: sidebar nav when wide, tab nav when compact
//   @Composable
//   fun SettingsScreen(isDark: Boolean, viewModel: LauncherViewModel, windowSize: WindowSize) {
//       if (windowSize.isExpanded) {
//           Row {
//               SettingsNav(modifier = Modifier.width(200.dp))
//               SettingsDetail(modifier = Modifier.weight(1f))
//           }
//       } else {
//           Column { SettingsTabBar(); SettingsDetail() }
//       }
//   }
//
// The WindowSize fields:
//   windowSize.widthDp     — exact current width as Dp
//   windowSize.heightDp    — exact current height as Dp
//   windowSize.isCompact   — width < 480dp (phone-like)
//   windowSize.isMedium    — width 480–720dp
//   windowSize.isExpanded  — width > 720dp (io.github.norbertweb.io.github.norbertweb.bluebird-like)
//
// ─────────────────────────────────────────────────────────────────────────────
//
// HOW TO ADD ViewModel support for persisted geometry + always-on-top ordering
// ─────────────────────────────────────────────────────────────────────────────
//
// In LauncherViewModel add:
//
//   private val _windowGeometries = mutableMapOf<String, WindowGeometry>()
//
//   fun getWindowGeometry(id: String): WindowGeometry? = _windowGeometries[id]
//
//   fun saveWindowGeometry(id: String, geometry: WindowGeometry) {
//       _windowGeometries[id] = geometry
//   }
//
// For always-on-top ordering, sort the windows list before passing to
// WindowManager so always-on-top windows render last (on top):
//
//   val sortedWindows = windows.sortedBy { it.isAlwaysOnTop }
//
// ─────────────────────────────────────────────────────────────────────────────
