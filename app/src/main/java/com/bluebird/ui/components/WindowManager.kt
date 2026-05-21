package com.bluebird.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
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
import com.bluebird.ui.screens.TextEditorScreen
import com.bluebird.ui.theme.Win11Colors
import com.win11launcher.ui.screens.BrowserScreen

// ─────────────────────────────────────────────────────────────────────────────
// Window size constraints (dp)
// ─────────────────────────────────────────────────────────────────────────────
private const val MIN_WINDOW_W = 320f
private const val MIN_WINDOW_H = 260f
private const val MAX_WINDOW_W = 1600f
private const val MAX_WINDOW_H = 1000f

// How many dp from an edge counts as the resize handle zone
private const val RESIZE_HANDLE_DP = 10f

// ─────────────────────────────────────────────────────────────────────────────
// Resize edge / corner enum
// ─────────────────────────────────────────────────────────────────────────────
private enum class ResizeEdge {
    NONE,
    LEFT, RIGHT, TOP, BOTTOM,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon helper
// ─────────────────────────────────────────────────────────────────────────────
private fun iconForKey(key: String): ImageVector = when (key) {
    WindowIconKey.TEXTEDITORSCREEN      -> Icons.Default.TextFields
    WindowIconKey.SETTINGS      -> Icons.Default.Settings
    WindowIconKey.FILE_EXPLORER -> Icons.Default.Folder
    WindowIconKey.BROWSER       -> Icons.Default.Public
    WindowIconKey.CALCULATOR    -> Icons.Default.Calculate
    WindowIconKey.CALENDAR      -> Icons.Default.CalendarToday
    WindowIconKey.PHOTOS        -> Icons.Default.PhotoLibrary
    WindowIconKey.TASK_MANAGER  -> Icons.Default.Monitor
    WindowIconKey.MEDIA_PLAYER  -> Icons.Default.MusicNote
    WindowIconKey.IMAGE_VIEWER  -> Icons.Default.Image
    WindowIconKey.PHONE         -> Icons.Default.Phone
    WindowIconKey.MESSAGES      -> Icons.Default.Chat
    WindowIconKey.RECYCLE_BIN   -> Icons.Default.Delete
    else                        -> Icons.Default.Window
}

// ─────────────────────────────────────────────────────────────────────────────
// Default window sizes per screen type
// ─────────────────────────────────────────────────────────────────────────────
private fun defaultSizeFor(screen: LauncherScreen): Pair<Float, Float> = when (screen) {
    LauncherScreen.CALCULATOR  -> 420f to 540f
    LauncherScreen.PHONE       -> 420f to 600f
    LauncherScreen.MESSAGES    -> 500f to 560f
    LauncherScreen.CALENDAR    -> 560f to 480f
    else                       -> 750f to 520f
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowSize — passed into each screen so it can respond to user resizing
// ─────────────────────────────────────────────────────────────────────────────
data class WindowSize(
    val widthDp: Dp,
    val heightDp: Dp,
    val isCompact: Boolean  = false, // width < 480dp
    val isMedium: Boolean   = false, // 480–720dp
    val isExpanded: Boolean = false  // > 720dp
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
                //
                // Fix: when minimized, collapse the outer wrapper to 0×0 so the
                // layout system gives it zero hit-test area. The inner content is
                // still fully composed (MediaPlayer keeps playing, Calculator keeps
                // state) and rendered via graphicsLayer with clip=false — but since
                // alpha=0 it is invisible. Desktop receives all touches normally.
                // ─────────────────────────────────────────────────────────────────
                Box(
                    modifier = if (isMinimized)
                        Modifier.size(0.dp)   // zero layout footprint = no hit-testing
                    else
                        Modifier              // normal: FloatingWindow sizes itself
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha           = animatedAlpha
                            scaleY          = animatedScaleY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            // clip=false: window renders outside the 0×0 bounds
                            // when minimized, but alpha=0 keeps it invisible
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

    // ── Position state (survives recomposition / resize) ──────────────────────
    var offsetX by remember { mutableStateOf(80f) }
    var offsetY by remember { mutableStateOf(40f) }

    // ── Size state — initialised once from defaults, then owned here ──────────
    val (defaultW, defaultH) = remember(windowState.screen) { defaultSizeFor(windowState.screen) }
    var windowWidthDp  by remember { mutableStateOf(defaultW) }
    var windowHeightDp by remember { mutableStateOf(defaultH) }

    // ── Snap-zone highlight (shows accent border while dragging near an edge) ─
    var isSnapping by remember { mutableStateOf(false) }

    // ── Parent canvas size — measured so we can clamp drag within bounds ──────
    var canvasWidthPx  by remember { mutableStateOf(0) }
    var canvasHeightPx by remember { mutableStateOf(0) }

    val elevation by animateDpAsState(
        targetValue   = if (isActive) 24.dp else 8.dp,
        label         = "elevation"
    )
    val windowBg    = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val borderColor = if (isActive) Win11Colors.AccentBlue.copy(alpha = if (isSnapping) 0.9f else 0.45f)
    else Color.White.copy(alpha = 0.1f)
    val borderWidth = if (isActive) (if (isSnapping) 2.dp else 1.dp) else 0.5.dp

    // ── CRITICAL: WindowContent is ALWAYS in the tree, never inside an if/else ──
    // Putting WindowContent inside `if (isMaximized) { ... } else { ... }` makes
    // Compose treat them as two DIFFERENT composables. When the branch switches,
    // Compose destroys one subtree and creates the other — wiping all remember{}
    // state in every screen (MediaPlayer stops, Calculator clears, etc.).
    //
    // Solution: ONE Box, always composed. Maximize/restore and resize only change
    // animated Modifier values (offset, width, height, corner radius). The resize
    // pointerInput uses PointerEventPass.Initial to peek at touches before
    // children see them — edge touches are claimed for resize, interior touches
    // are NOT consumed and fall through normally to buttons/scrollables/drag.
    // ─────────────────────────────────────────────────────────────────────────

    // Animated geometry so transitions between maximized↔floating are smooth
    val animOffsetX by animateFloatAsState(
        targetValue   = if (windowState.isMaximized) 0f else offsetX,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winOffsetX_${windowState.id}"
    )
    val animOffsetY by animateFloatAsState(
        targetValue   = if (windowState.isMaximized) 0f else offsetY,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winOffsetY_${windowState.id}"
    )

    // Live size: when maximized use canvas size, otherwise use window size state
    val targetW = if (windowState.isMaximized) with(density) { canvasWidthPx.toDp().value }
    else windowWidthDp
    val targetH = if (windowState.isMaximized) with(density) { canvasHeightPx.toDp().value }
    else windowHeightDp

    val animW by animateFloatAsState(
        targetValue   = targetW,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winW_${windowState.id}"
    )
    val animH by animateFloatAsState(
        targetValue   = targetH,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "winH_${windowState.id}"
    )

    val cornerRadius = if (windowState.isMaximized) 0.dp else 10.dp
    val animCorner by animateDpAsState(
        targetValue   = cornerRadius,
        animationSpec = tween(200),
        label         = "winCorner_${windowState.id}"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                canvasWidthPx  = coords.size.width
                canvasHeightPx = coords.size.height
            }
    ) {
        // ── Single always-composed window Box ─────────────────────────────────
        // The resize gesture uses PointerEventPass.Initial so we can PEEK at the
        // touch position before children see it. If the touch is on an edge we
        // claim it for resizing. If it's in the interior we do NOT consume it, so
        // it falls through normally to title-bar drag, buttons, scrollables, etc.
        // This means NO overlay Box is needed — one Box, one pointerInput(Unit).
        Box(
            modifier = Modifier
                .offset { IntOffset(animOffsetX.toInt(), animOffsetY.toInt()) }
                .width(animW.dp)
                .height(animH.dp)
                .shadow(elevation, RoundedCornerShape(animCorner))
                .clip(RoundedCornerShape(animCorner))
                .background(windowBg, RoundedCornerShape(animCorner))
                .border(borderWidth, borderColor, RoundedCornerShape(animCorner))
                // ── Focus on tap anywhere (pass = Main so children still get it) ─
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { onFocus() })
                }
                // ── Resize via Initial pass — peek before children, claim only
                //    edge touches, leave interior touches completely alone ──────────
                .pointerInput(Unit) {
                    val handlePx = with(density) { RESIZE_HANDLE_DP.dp.toPx() }
                    awaitPointerEventScope {
                        while (true) {
                            // Initial pass: we see the event before any child does
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down  = event.changes.firstOrNull() ?: continue
                            if (!down.changedToDown()) continue

                            val edge = detectEdge(
                                pos      = down.position,
                                w        = size.width.toFloat(),
                                h        = size.height.toFloat(),
                                handlePx = handlePx
                            )

                            // Interior touch — do NOT consume, let it pass through
                            if (edge == ResizeEdge.NONE || windowState.isMaximized) continue

                            // Edge touch — claim it and run the resize drag loop
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

            // WindowContent identity is stable — same composable, always, forever
            WindowContent(
                windowState = windowState,
                windowSize  = winSize,
                isDark      = isDark,
                viewModel   = viewModel,
                onClose     = onClose,
                onMinimize  = onMinimize,
                onMaximize  = onMaximize,
                onDrag      = if (windowState.isMaximized) null else { dx, dy ->
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

            // Resize handle affordances (only shown when not maximized)
            if (!windowState.isMaximized) {
                ResizeHandles(isDark = isDark)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers for resize math
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Determines which edge/corner the pointer is touching.
 * Returns ResizeEdge.NONE if not near any edge (interior tap → focus/drag).
 */
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

/**
 * Applies a resize delta to position + size depending on which edge is active.
 * Left/top edges must also shift the window origin so it looks anchored to the
 * opposite side.
 */
private fun applyResize(
    edge: ResizeEdge,
    dx: Float, dy: Float,
    offsetXRef: (Float) -> Unit,
    offsetYRef: (Float) -> Unit,
    widthRef:   (Float) -> Unit,
    heightRef:  (Float) -> Unit
) {
    when (edge) {
        ResizeEdge.RIGHT        -> widthRef(dx)
        ResizeEdge.BOTTOM       -> heightRef(dy)
        ResizeEdge.LEFT         -> { offsetXRef(dx); widthRef(-dx) }
        ResizeEdge.TOP          -> { offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.TOP_LEFT     -> { offsetXRef(dx); widthRef(-dx); offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.TOP_RIGHT    -> { widthRef(dx);   offsetYRef(dy); heightRef(-dy) }
        ResizeEdge.BOTTOM_LEFT  -> { offsetXRef(dx); widthRef(-dx); heightRef(dy) }
        ResizeEdge.BOTTOM_RIGHT -> { widthRef(dx);   heightRef(dy) }
        ResizeEdge.NONE         -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resize handle affordances — small triangles in corners
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.ResizeHandles(isDark: Boolean) {
    val handleColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
    val corners = listOf(
        Alignment.BottomEnd,
        Alignment.BottomStart,
        Alignment.TopEnd
    )
    corners.forEach { alignment ->
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
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowContent — title bar + screen content
// WindowSize is now passed all the way through to every screen so they can
// adapt their layout (sidebar vs bottom nav, grid columns, font scale, etc.)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowContent(
    windowState: WindowState,
    windowSize: WindowSize,
    isDark: Boolean,
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
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
        ) {
            WindowTitleBar(
                title       = windowState.title,
                iconKey     = windowState.iconKey,
                isDark      = isDark,
                windowSize  = windowSize,
                onMinimize  = onMinimize,
                onMaximize  = onMaximize,
                onClose     = onClose
            )
        }

        // ── Screen content — receives WindowSize for responsive layouts ────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val extras = windowState.extras
            when (windowState.screen) {
                LauncherScreen.TextEditorScreen -> TextEditorScreen(isDark)
                LauncherScreen.SETTINGS      -> SettingsScreen(isDark, viewModel)
                LauncherScreen.FILE_EXPLORER -> FileExplorerScreen(isDark, viewModel)
                LauncherScreen.BROWSER       -> BrowserScreen(isDark)
                LauncherScreen.CALCULATOR    -> CalculatorScreen(isDark)
                LauncherScreen.CALENDAR      -> CalendarScreen(isDark)
                LauncherScreen.PHOTOS        -> PhotosScreen(isDark)
                LauncherScreen.TASK_MANAGER  -> TaskManagerScreen(isDark)
                LauncherScreen.MEDIA_PLAYER  -> {
                    // KEY FIX: filePath comes from extras, is remembered so
                    // MediaPlayer never sees a new value on resize recomposition.
                    val filePath = remember(windowState.id) {
                        extras["filePath"] ?: ""
                    }
                    MediaPlayerScreen(isDark, filePath)
                }
                LauncherScreen.IMAGE_VIEWER  -> {
                    val filePath = remember(windowState.id) {
                        extras["filePath"] ?: ""
                    }
                    ImageViewerScreen(isDark, filePath, viewModel)
                }
                LauncherScreen.PHONE         -> PhoneScreen(isDark)
                LauncherScreen.MESSAGES      -> MessagesScreen(isDark)
                LauncherScreen.RECYCLE_BIN   -> RecycleBinScreen(isDark, viewModel)
                else -> {}
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WindowTitleBar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WindowTitleBar(
    title: String,
    iconKey: String = "",
    isDark: Boolean,
    windowSize: WindowSize = WindowSize(750.dp, 520.dp),
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val barBg   = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)
    val textCol = if (isDark) Color.White else Color(0xFF1C1C1C)
    val icon    = remember(iconKey) { iconForKey(iconKey) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(barBg)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (isDark) Color.White.copy(0.75f) else Color(0xFF444444),
            modifier           = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))

        // Show window dimensions when compact (helpful UX during resize)
        val sizeLabel = if (windowSize.isCompact) " — ${windowSize.widthDp.value.toInt()}×${windowSize.heightDp.value.toInt()}" else ""

        Text(
            text       = title + sizeLabel,
            color      = textCol,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)
        )

        // macOS-style traffic-light buttons
        TitleBarButton(color = Color(0xFFFF5F57), onClick = onClose)
        Spacer(Modifier.width(6.dp))
        TitleBarButton(color = Color(0xFFFFBD2E), onClick = onMinimize)
        Spacer(Modifier.width(6.dp))
        TitleBarButton(color = Color(0xFF28C840), onClick = onMaximize)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TitleBarButton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TitleBarButton(color: Color, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (hovered) color.copy(alpha = 0.7f) else color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { hovered = true; tryAwaitRelease(); hovered = false },
                    onTap   = { onClick() }
                )
            }
    )
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
//           // Two-column layout: sidebar + file list
//           Row {
//               FolderSidebar(modifier = Modifier.width(180.dp))
//               FileGrid(columns = 4, modifier = Modifier.weight(1f))
//           }
//       } else if (windowSize.isMedium) {
//           // Compact sidebar + smaller grid
//           Row {
//               FolderSidebar(modifier = Modifier.width(120.dp))
//               FileGrid(columns = 3, modifier = Modifier.weight(1f))
//           }
//       } else {
//           // Phone-like: full width list, no sidebar
//           FileGrid(columns = 2, modifier = Modifier.fillMaxWidth())
//       }
//   }
//
//   // Settings: sidebar nav when wide, tab nav when compact
//   @Composable
//   fun SettingsScreen(isDark: Boolean, viewModel: LauncherViewModel, windowSize: WindowSize) {
//       if (windowSize.isExpanded) {
//           Row {
//               SettingsNav(modifier = Modifier.width(200.dp))   // sidebar
//               SettingsDetail(modifier = Modifier.weight(1f))
//           }
//       } else {
//           Column { SettingsTabBar(); SettingsDetail() }
//       }
//   }
//
// The WindowSize fields available:
//   windowSize.widthDp     — exact current width as Dp
//   windowSize.heightDp    — exact current height as Dp
//   windowSize.isCompact   — width < 480dp (phone-like)
//   windowSize.isMedium    — width 480–720dp
//   windowSize.isExpanded  — width > 720dp (desktop-like)
//
// ─────────────────────────────────────────────────────────────────────────────
