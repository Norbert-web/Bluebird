package com.bluebird.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Window
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.bluebird.LauncherScreen
import com.bluebird.LauncherViewModel
import com.bluebird.WindowIconKey
import com.bluebird.WindowState
import com.bluebird.ui.screens.CalculatorScreen
import com.bluebird.ui.screens.CalendarScreen
import com.bluebird.ui.screens.FileExplorerScreen
import com.bluebird.ui.screens.ImageViewerScreen
import com.bluebird.ui.screens.MediaPlayerScreen
import com.bluebird.ui.screens.MessagesScreen
import com.bluebird.ui.screens.PhoneScreen
import com.bluebird.ui.screens.PhotosScreen
import com.bluebird.ui.screens.RecycleBinScreen
import com.bluebird.ui.screens.SettingsScreen
import com.bluebird.ui.screens.TaskManagerScreen
import com.bluebird.editor.ui.screens.PremiumTextEditorScreen
import com.bluebird.ui.theme.Win11Colors
import com.win11launcher.ui.screens.BrowserScreen
import kotlinx.coroutines.delay

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
private fun iconForKey(key: String): ImageVector = when (key) {
    WindowIconKey.PremiumTextEditorScreen -> Icons.Default.TextFields
    WindowIconKey.SETTINGS         -> Icons.Default.Settings
    WindowIconKey.FILE_EXPLORER    -> Icons.Default.Folder
    WindowIconKey.BROWSER          -> Icons.Default.Public
    WindowIconKey.CALCULATOR       -> Icons.Default.Calculate
    WindowIconKey.CALENDAR         -> Icons.Default.CalendarToday
    WindowIconKey.PHOTOS           -> Icons.Default.PhotoLibrary
    WindowIconKey.TASK_MANAGER     -> Icons.Default.Monitor
    WindowIconKey.MEDIA_PLAYER     -> Icons.Default.MusicNote
    WindowIconKey.IMAGE_VIEWER     -> Icons.Default.Image
    WindowIconKey.PHONE            -> Icons.Default.Phone
    WindowIconKey.MESSAGES         -> Icons.Default.Chat
    WindowIconKey.RECYCLE_BIN      -> Icons.Default.Delete
    else                           -> Icons.Default.Window
}

// ─────────────────────────────────────────────────────────────────────────────
// Default window sizes per screen type
// ─────────────────────────────────────────────────────────────────────────────
private fun defaultSizeFor(screen: LauncherScreen): Pair<Float, Float> = when (screen) {
    LauncherScreen.CALCULATOR -> 420f to 540f
    LauncherScreen.PHONE      -> 420f to 600f
    LauncherScreen.MESSAGES   -> 500f to 560f
    LauncherScreen.CALENDAR   -> 560f to 480f
    else                      -> 750f to 520f
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

    // ── Save geometry whenever it changes ─────────────────────────────────────
    LaunchedEffect(offsetX, offsetY, windowWidthDp, windowHeightDp) {
        viewModel.saveWindowGeometry(
            windowState.id,
            WindowGeometry(offsetX, offsetY, windowWidthDp, windowHeightDp)
        )
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
        Win11Colors.AccentBlue.copy(alpha = if (isSnapping) 0.9f else 0.45f)
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

                            if (edge == ResizeEdge.NONE || windowState.isMaximized || isPip) continue

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
                // Hover support for mouse (PointerEventType.Enter/Exit)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // Hover events are informational; child composables handle
                            // their own hover state via the same mechanism.
                            // This block is reserved for future global hover effects.
                        }
                    }
                }
        ) {
            val winSize = WindowSize.from(animW.dp, animH.dp)

            if (!isPip) {
                // ── Full window content ───────────────────────────────────────
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
    val accent = Win11Colors.AccentBlue
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
                                .background(if (isSelected) Win11Colors.AccentBlue else divider)
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
            .background(if (hovered) Win11Colors.AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
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
            Icon(icon, null,
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
                    .background(Win11Colors.AccentBlue)
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
                isDark             = isDark,
                windowSize         = windowSize,
                isMaximized        = windowState.isMaximized,
                alwaysOnTop        = alwaysOnTop,
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
                LauncherScreen.PremiumTextEditorScreen -> PremiumTextEditorScreen(isDark)
                LauncherScreen.SETTINGS      -> SettingsScreen(isDark, viewModel)
                LauncherScreen.FILE_EXPLORER -> FileExplorerScreen(isDark, viewModel)
                LauncherScreen.BROWSER       -> BrowserScreen(isDark)
                LauncherScreen.CALCULATOR    -> CalculatorScreen(isDark)
                LauncherScreen.CALENDAR      -> CalendarScreen(isDark)
                LauncherScreen.PHOTOS        -> PhotosScreen(isDark)
                LauncherScreen.TASK_MANAGER  -> TaskManagerScreen(isDark)
                LauncherScreen.MEDIA_PLAYER  -> {
                    val filePath = remember(windowState.id) { extras["filePath"] ?: "" }
                    MediaPlayerScreen(isDark, filePath)
                }
                LauncherScreen.IMAGE_VIEWER  -> {
                    val filePath = remember(windowState.id) { extras["filePath"] ?: "" }
                    ImageViewerScreen(isDark, filePath, viewModel)
                }
                LauncherScreen.PHONE      -> PhoneScreen(isDark)
                LauncherScreen.MESSAGES   -> MessagesScreen(isDark)
                LauncherScreen.RECYCLE_BIN -> RecycleBinScreen(isDark, viewModel)
                else -> {}
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowTitleBar — Windows 11 style
//   • Icon + title (left-aligned)
//   • Minimize, Maximize/Restore, Close buttons (right-aligned, Win11 look)
//   • Maximize button: tap = maximize/restore, long-press = snap picker
//   • "Always on Top" pin badge shown when active
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowTitleBar(
    title: String,
    iconKey: String = "",
    isDark: Boolean,
    windowSize: WindowSize = WindowSize(750.dp, 520.dp),
    isMaximized: Boolean = false,
    alwaysOnTop: Boolean = false,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onSnapPickerToggle: () -> Unit,
    onClose: () -> Unit
) {
    val barBg   = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)
    val textCol = if (isDark) Color.White else Color(0xFF1C1C1C)
    val icon    = remember(iconKey) { iconForKey(iconKey) }

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
        // ── App icon ──────────────────────────────────────────────────────────
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (isDark) Color.White.copy(0.75f) else Color(0xFF444444),
            modifier           = Modifier.size(14.dp)
        )
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
        Win11TitleButton(
            label   = "—",
            hoverBg = if (isDark) Color(0xFF3A3A3A) else Color(0xFFD0D0D0),
            onClick = onMinimize
        )

        // Maximize / Restore □ (tap) + long-press = snap picker
        Win11TitleButton(
            label        = if (isMaximized) "❐" else "□",
            hoverBg      = if (isDark) Color(0xFF3A3A3A) else Color(0xFFD0D0D0),
            onClick      = onMaximize,
            onLongPress  = onSnapPickerToggle   // touch-friendly snap picker
        )

        // Close ✕ — red hover
        Win11TitleButton(
            label        = "✕",
            hoverBg      = Color(0xFFC42B1C),
            hoverTextCol = Color.White,
            onClick      = onClose
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Win11TitleButton — flat, hover-highlighted, supports long-press
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Win11TitleButton(
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
// HOW TO MAKE YOUR SCREENS RESPONSIVE
// ─────────────────────────────────────────────────────────────────────────────
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
//   windowSize.isExpanded  — width > 720dp (desktop-like)
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
