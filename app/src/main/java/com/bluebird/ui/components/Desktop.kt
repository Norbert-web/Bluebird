package com.bluebird.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.os.FileObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bluebird.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Wallpaper Gradients
// ─────────────────────────────────────────────────────────────────
val wallpaperGradients = listOf(
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
    listOf(Color(0xFF141E30), Color(0xFF243B55)),
    listOf(Color(0xFF000000), Color(0xFF434343)),
    listOf(Color(0xFF1a2a6c), Color(0xFFb21f1f), Color(0xFFfdbb2d)),
    listOf(Color(0xFF00b09b), Color(0xFF96c93d))
)

// ─────────────────────────────────────────────────────────────────
// Media Extensions
// ─────────────────────────────────────────────────────────────────
private val MUSIC_EXTS = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma")
private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "m4v")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
private val TEXT_EXTS  = setOf("txt", "md", "log", "json", "xml", "csv", "html", "htm", "js", "py", "kt")

// ─────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────
data class DesktopFileInfo(
    val id: String,
    val file: File,
    val name: String,
    val type: DesktopItemType,
    val packageName: String? = null,
    val iconBitmap: Bitmap? = null,
    val position: Offset = Offset.Zero,
    val isSelected: Boolean = false
)

enum class DesktopItemType {
    FOLDER, TEXT_FILE, IMAGE_FILE, MUSIC_FILE, VIDEO_FILE,
    APP_SHORTCUT, OTHER_FILE, THIS_PC, RECYCLE_BIN, SETTINGS_ICON
}

enum class DesktopIconSize { SMALL, MEDIUM, LARGE }
enum class DesktopSortMode { NAME, DATE_MODIFIED, TYPE, SIZE }

data class InlineRenameState(
    val targetId: String,
    val currentName: String
)

data class AppInfoItem(
    val label: String,
    val packageName: String,
    val iconBitmap: Bitmap          // Pre-resolved Bitmap — avoids repeated cast at render time
)

// ─────────────────────────────────────────────────────────────────
// FIX: Universal drawable → Bitmap converter.
// Handles BitmapDrawable, AdaptiveIconDrawable (API 26+), and any
// other Drawable subclass by drawing onto a fresh Canvas.
// ─────────────────────────────────────────────────────────────────
fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bm)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bm
}

// ─────────────────────────────────────────────────────────────────
// Icon Helpers
// ─────────────────────────────────────────────────────────────────
fun getFileIcon(file: File): ImageVector = when {
    file.isDirectory -> Icons.Default.Folder
    file.extension.lowercase() in MUSIC_EXTS  -> Icons.Default.AudioFile
    file.extension.lowercase() in VIDEO_EXTS  -> Icons.Default.PlayCircle
    file.extension.lowercase() in IMAGE_EXTS  -> Icons.Default.Image
    file.extension.lowercase() in TEXT_EXTS   -> Icons.Default.Description
    file.extension.lowercase() == "pdf"       -> Icons.Default.PictureAsPdf
    file.extension.lowercase() == "apk"       -> Icons.Default.Android
    file.extension.lowercase() in setOf("zip","rar","7z","tar","gz") -> Icons.Default.Archive
    file.extension.lowercase() in setOf("doc","docx") -> Icons.Default.Article
    file.extension.lowercase() in setOf("xls","xlsx") -> Icons.Default.TableChart
    else -> Icons.Default.InsertDriveFile
}

fun getFileIconColor(file: File): Color = when {
    file.isDirectory -> Color(0xFFFFC107)
    file.extension.lowercase() in MUSIC_EXTS  -> Color(0xFFFF8C00)
    file.extension.lowercase() in VIDEO_EXTS  -> Color(0xFF8764B8)
    file.extension.lowercase() in IMAGE_EXTS  -> Color(0xFF16C60C)
    file.extension.lowercase() in TEXT_EXTS   -> Color(0xFF0078D4)
    file.extension.lowercase() == "pdf"       -> Color(0xFFD83B01)
    file.extension.lowercase() == "apk"       -> Color(0xFF107C10)
    file.extension.lowercase() in setOf("doc","docx") -> Color(0xFF0078D4)
    file.extension.lowercase() in setOf("xls","xlsx") -> Color(0xFF217346)
    file.extension.lowercase() in setOf("zip","rar","7z") -> Color(0xFF8B6914)
    else -> Color(0xFF9E9E9E)
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024          -> "$bytes B"
    bytes < 1_048_576     -> "%.1f KB".format(bytes / 1024f)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576f)
    else                  -> "%.2f GB".format(bytes / 1_073_741_824f)
}

private fun uniqueName(dir: File, baseName: String, ext: String = ""): String {
    val suffix = if (ext.isEmpty()) "" else ".$ext"
    if (!File(dir, "$baseName$suffix").exists()) return "$baseName$suffix"
    var counter = 2
    while (File(dir, "$baseName ($counter)$suffix").exists()) counter++
    return "$baseName ($counter)$suffix"
}

// ─────────────────────────────────────────────────────────────────
// Grid Metrics & Placement
// Column-first: fills top→bottom before moving right, like Windows 11.
// ─────────────────────────────────────────────────────────────────
private fun iconSizeDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 36f
    DesktopIconSize.MEDIUM -> 48f
    DesktopIconSize.LARGE  -> 64f
}

private fun cellWidthDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 74f
    DesktopIconSize.MEDIUM -> 86f
    DesktopIconSize.LARGE  -> 102f
}

private fun cellHeightDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 76f
    DesktopIconSize.MEDIUM -> 92f
    DesktopIconSize.LARGE  -> 110f
}

private fun autoGridPos(
    idx: Int,
    rows: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    startPaddingPx: Float = 0f,
    topPaddingPx: Float = 0f
): Offset {
    val safeRows = maxOf(1, rows)
    val col = idx / safeRows
    val row = idx % safeRows
    return Offset(col * cellWidthPx + startPaddingPx, row * cellHeightPx + topPaddingPx)
}

private fun snapToGrid(
    pos: Offset,
    cellWidthPx: Float,
    cellHeightPx: Float,
    startPaddingPx: Float,
    topPaddingPx: Float
): Offset {
    val col = ((pos.x - startPaddingPx) / cellWidthPx).roundToInt().coerceAtLeast(0)
    val row = ((pos.y - topPaddingPx) / cellHeightPx).roundToInt().coerceAtLeast(0)
    return Offset(col * cellWidthPx + startPaddingPx, row * cellHeightPx + topPaddingPx)
}

// ─────────────────────────────────────────────────────────────────
// Main Desktop Component
// ─────────────────────────────────────────────────────────────────
@Composable
fun Desktop(
    wallpaper: WallpaperState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val density  = LocalDensity.current
    val config   = LocalConfiguration.current
    val screenW  = config.screenWidthDp
    val screenH  = config.screenHeightDp

    val desktopDir = remember { File(Environment.getExternalStorageDirectory(), "Desktop") }

    // ── Core state ──
    var items               by remember { mutableStateOf(listOf<DesktopFileInfo>()) }
    var selectedIds         by remember { mutableStateOf(setOf<String>()) }
    var iconSize            by remember { mutableStateOf(DesktopIconSize.MEDIUM) }
    var sortMode            by remember { mutableStateOf(DesktopSortMode.NAME) }
    var sortAscending       by remember { mutableStateOf(true) }
    var autoArrange         by remember { mutableStateOf(true) }
    var showIconsOnDesktop  by remember { mutableStateOf(true) }

    val customPositions     = remember { mutableStateMapOf<String, Offset>() }
    var draggedId           by remember { mutableStateOf<String?>(null) }

    // ── Clipboard ──
    var clipboardFiles      by remember { mutableStateOf(listOf<Pair<File, Boolean>>()) }

    // ── Context menus ──
    var showDesktopCtx      by remember { mutableStateOf(false) }
    var desktopCtxOffset    by remember { mutableStateOf(Offset.Zero) }
    var iconCtxTarget       by remember { mutableStateOf<DesktopFileInfo?>(null) }
    var iconCtxOffset       by remember { mutableStateOf(Offset.Zero) }

    // ── Inline rename ──
    var inlineRename        by remember { mutableStateOf<InlineRenameState?>(null) }
    var pendingRenameId     by remember { mutableStateOf<String?>(null) }

    // ── Dialogs ──
    var showPropsDialog     by remember { mutableStateOf(false) }
    var propsTarget         by remember { mutableStateOf<DesktopFileInfo?>(null) }
    var showShortcutDialog  by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

    // ── Lasso selection ──
    var selStart            by remember { mutableStateOf(Offset.Zero) }
    var selEnd              by remember { mutableStateOf(Offset.Zero) }
    var isSelecting         by remember { mutableStateOf(false) }

    // FIX: track whether a drag is really happening on the background
    // so lasso doesn't fire when the user long-presses an icon area.
    var lassoActive         by remember { mutableStateOf(false) }

    val isDark = viewModel.uiState.collectAsState().value.isDarkTheme

    // ── Grid metrics ──
    val cellWDp     = cellWidthDp(iconSize)
    val cellHDp     = cellHeightDp(iconSize)
    val gridPadLeft = 10f
    val gridPadTop  = 10f

    val rows = remember(screenH, iconSize) {
        ((screenH - gridPadTop * 2) / cellHeightDp(iconSize)).toInt().coerceAtLeast(1)
    }

    val cellWPx   = with(density) { cellWDp.dp.toPx() }
    val cellHPx   = with(density) { cellHDp.dp.toPx() }
    val padLeftPx = with(density) { gridPadLeft.dp.toPx() }
    val padTopPx  = with(density) { gridPadTop.dp.toPx() }

    // ── FIX: debounce refresh to prevent double-fire on rename MOVED_FROM+MOVED_TO ──
    var refreshPending by remember { mutableStateOf(false) }

    // ── File loader — runs on IO thread ──
    // FIX: uses drawableToBitmap() so AdaptiveIconDrawable icons show correctly.
    fun loadItem(file: File): DesktopFileInfo? = try {
        val ext  = file.extension.lowercase()
        val type = when {
            file.isDirectory  -> DesktopItemType.FOLDER
            ext in MUSIC_EXTS -> DesktopItemType.MUSIC_FILE
            ext in VIDEO_EXTS -> DesktopItemType.VIDEO_FILE
            ext in IMAGE_EXTS -> DesktopItemType.IMAGE_FILE
            ext in TEXT_EXTS  -> DesktopItemType.TEXT_FILE
            ext == "desktop"  -> {
                val lines = file.readLines()
                val pkg   = lines.find { it.startsWith("package=") }?.removePrefix("package=")?.trim() ?: ""
                val label = lines.find { it.startsWith("label=") }?.removePrefix("label=")?.trim()
                    ?: file.nameWithoutExtension
                // FIX: resolve app icon through drawableToBitmap — covers AdaptiveIconDrawable
                val iconBmp: Bitmap? = if (pkg.isNotBlank()) {
                    try {
                        val drawable = context.packageManager.getApplicationIcon(pkg)
                        drawableToBitmap(drawable)
                    } catch (_: Exception) { null }
                } else null
                return DesktopFileInfo(
                    id = file.absolutePath, file = file, name = label,
                    type = DesktopItemType.APP_SHORTCUT, packageName = pkg,
                    iconBitmap = iconBmp
                )
            }
            else -> DesktopItemType.OTHER_FILE
        }
        val thumb = if (type == DesktopItemType.IMAGE_FILE) {
            try {
                BitmapFactory.decodeFile(
                    file.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = 4 }
                )
            } catch (_: Exception) { null }
        } else null
        DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = type, iconBitmap = thumb)
    } catch (_: Exception) { null }

    fun refreshDesktop() {
        scope.launch(Dispatchers.IO) {
            desktopDir.mkdirs()
            val loaded = (desktopDir.listFiles()?.toList() ?: emptyList())
                .sortedBy { it.name.lowercase() }
                .mapNotNull { loadItem(it) }
            withContext(Dispatchers.Main) {
                items = loaded
                // Remove stale custom positions for items that no longer exist
                customPositions.keys.retainAll(loaded.map { it.id }.toSet())
                // Auto-activate rename for newly created items
                val pendId = pendingRenameId
                if (pendId != null) {
                    val newItem = loaded.find { it.id == pendId }
                    if (newItem != null) {
                        inlineRename = InlineRenameState(newItem.id, newItem.name)
                        selectedIds  = setOf(newItem.id)
                        pendingRenameId = null
                    }
                }
                refreshPending = false
            }
        }
    }

    // FIX: debounced refresh — FileObserver fires MOVED_FROM + MOVED_TO on rename,
    // which caused two rapid refreshes and a flicker. Now we coalesce them.
    fun scheduleRefresh() {
        if (refreshPending) return
        refreshPending = true
        scope.launch {
            delay(120)
            refreshDesktop()
        }
    }

    DisposableEffect(desktopDir.absolutePath) {
        refreshDesktop()
        val observer = object : FileObserver(
            desktopDir.absolutePath,
            CREATE or DELETE or MOVED_FROM or MOVED_TO or CLOSE_WRITE
        ) {
            override fun onEvent(event: Int, path: String?) { scheduleRefresh() }
        }
        observer.startWatching()
        onDispose { observer.stopWatching() }
    }

    val sortedItems = remember(items, sortMode, sortAscending) {
        val s = when (sortMode) {
            DesktopSortMode.NAME          -> items.sortedBy { it.name.lowercase() }
            DesktopSortMode.DATE_MODIFIED -> items.sortedBy { it.file.lastModified() }
            DesktopSortMode.TYPE          -> items.sortedBy { it.file.extension }
            DesktopSortMode.SIZE          -> items.sortedBy { it.file.length() }
        }
        if (sortAscending) s else s.reversed()
    }

    // Pre-compute index map to avoid O(n) indexOf() inside the render loop
    val indexMap = remember(sortedItems) {
        sortedItems.mapIndexed { idx, item -> item.id to idx }.toMap()
    }

    fun openItem(item: DesktopFileInfo) {
        when {
            item.type == DesktopItemType.FOLDER ->
                viewModel.openWindow(LauncherScreen.FILE_EXPLORER)
            item.type == DesktopItemType.APP_SHORTCUT ->
                item.packageName?.let { pkg ->
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            else -> viewModel.openFileWithSystem(context, item.file.absolutePath)
        }
    }

    // FIX: commitRename now appends extension only for non-.desktop files,
    // and guards against blank/unchanged names without crashing.
    fun commitRename(rename: InlineRenameState, newRawName: String) {
        inlineRename = null  // clear immediately to prevent double-commit
        val target = items.find { it.id == rename.targetId } ?: return
        val base = newRawName.trim()
        if (base.isBlank()) return

        // For app shortcuts, the name is the display label stored inside the file —
        // rewrite the label line rather than renaming the file itself.
        if (target.type == DesktopItemType.APP_SHORTCUT) {
            try {
                val lines = target.file.readLines().toMutableList()
                val labelIdx = lines.indexOfFirst { it.startsWith("label=") }
                if (labelIdx >= 0) lines[labelIdx] = "label=$base"
                else lines.add("label=$base")
                target.file.writeText(lines.joinToString("\n") + "\n")
                scheduleRefresh()
            } catch (_: Exception) {}
            return
        }

        // For real files: preserve extension automatically
        val ext = if (target.file.name.contains("."))
            ".${target.file.name.substringAfterLast(".")}" else ""
        val finalName = if (ext.isNotEmpty() && !base.endsWith(ext, ignoreCase = true))
            "$base$ext" else base

        if (finalName == target.file.name) return  // nothing to do

        val dest = File(target.file.parent ?: return, finalName)
        if (dest.exists()) return  // don't clobber existing file silently
        target.file.renameTo(dest)
        scheduleRefresh()
    }

    // ─────────────────────────────────────────────────────────────
    // View hierarchy
    // ─────────────────────────────────────────────────────────────
    Box(modifier = modifier.fillMaxSize()) {

        // ── Wallpaper ──
        if (wallpaper.homeWallpaperUri.isNotEmpty()) {
            AsyncImage(
                model = Uri.parse(wallpaper.homeWallpaperUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val gradient = wallpaperGradients[wallpaper.homeWallpaperIndex % wallpaperGradients.size]
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(gradient, start = Offset(0f, 0f), end = Offset(2500f, 1500f))
                )
            )
        }

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.02f)))

        // ── Background gesture layer (tap to deselect / long-press for desktop ctx / lasso) ──
        // FIX: lasso only activates when draggedId is null, so it can't conflict
        // with an icon drag that's already in progress.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // FIX: commit rename on outside tap before deselecting
                            val currentRename = inlineRename
                            if (currentRename != null) {
                                commitRename(currentRename, currentRename.currentName)
                            } else {
                                selectedIds = emptySet()
                            }
                            showDesktopCtx = false
                            iconCtxTarget  = null
                        },
                        onLongPress = { off ->
                            // Only open desktop context menu if not dragging an icon
                            if (draggedId == null) {
                                desktopCtxOffset = off
                                showDesktopCtx   = true
                                iconCtxTarget    = null
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { off ->
                            // FIX: only start lasso if no icon is being dragged
                            if (draggedId == null) {
                                lassoActive = true
                                isSelecting = true
                                selStart    = off
                                selEnd      = off
                            }
                        },
                        onDrag = { ch, amt ->
                            if (lassoActive) {
                                ch.consume()
                                selEnd += amt
                            }
                        },
                        onDragEnd = {
                            if (lassoActive) {
                                isSelecting = false
                                lassoActive = false
                                val rect = Rect(
                                    minOf(selStart.x, selEnd.x), minOf(selStart.y, selEnd.y),
                                    maxOf(selStart.x, selEnd.x), maxOf(selStart.y, selEnd.y)
                                )
                                // FIX: use pre-computed indexMap instead of indexOf()
                                selectedIds = sortedItems.filter { item ->
                                    val idx = indexMap[item.id] ?: return@filter false
                                    val pos = customPositions[item.id]
                                        ?: autoGridPos(idx, rows, cellWPx, cellHPx, padLeftPx, padTopPx)
                                    Rect(pos.x, pos.y, pos.x + cellWPx, pos.y + cellHPx).overlaps(rect)
                                }.map { it.id }.toSet()
                            }
                        },
                        onDragCancel = { isSelecting = false; lassoActive = false }
                    )
                }
        )

        // ── Icons layer ──
        if (showIconsOnDesktop) {
            Box(Modifier.fillMaxSize()) {
                sortedItems.forEachIndexed { idx, item ->
                    val basePos = autoGridPos(idx, rows, cellWPx, cellHPx, padLeftPx, padTopPx)

                    // FIX: pos is remembered by item.id + layout params; reset cleanly when autoArrange flips
                    var pos by remember(item.id, rows, iconSize, autoArrange) {
                        mutableStateOf(
                            if (autoArrange) basePos else customPositions[item.id] ?: basePos
                        )
                    }

                    LaunchedEffect(autoArrange, idx, rows, iconSize) {
                        if (autoArrange) pos = basePos
                    }

                    val isDragged = draggedId == item.id
                    val dragScale by animateFloatAsState(
                        targetValue    = if (isDragged) 1.08f else 1f,
                        animationSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                        label          = "icon_drag_scale"
                    )

                    // FIX: separate drag-threshold logic — long-press with no movement
                    // triggers context menu; actual movement triggers drag mode.
                    // This replaces the old onLongPress on DesktopIcon for drag.
                    var dragMoved by remember { mutableStateOf(false) }

                    Box(
                        Modifier
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .scale(dragScale)
                            .zIndex(if (isDragged) 50f else 1f)
                            .pointerInput(item.id, autoArrange) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        // Commit any open rename first
                                        val r = inlineRename
                                        if (r != null) commitRename(r, r.currentName)
                                        draggedId = item.id
                                        dragMoved = false
                                    },
                                    onDrag = { ch, amt ->
                                        ch.consume()
                                        // Only count as a real drag after passing a 10px threshold
                                        if (!dragMoved &&
                                            (abs(amt.x) > 10f || abs(amt.y) > 10f)
                                        ) {
                                            dragMoved = true
                                        }
                                        if (dragMoved) {
                                            val maxX = with(density) { (screenW.dp - cellWDp.dp).toPx() }
                                            val maxY = with(density) { (screenH.dp - cellHDp.dp).toPx() }
                                            pos = Offset(
                                                (pos.x + amt.x).coerceIn(padLeftPx, maxX),
                                                (pos.y + amt.y).coerceIn(padTopPx, maxY)
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        if (!dragMoved) {
                                            // FIX: no real drag → treat as long-press → show context menu
                                            selectedIds    = setOf(item.id)
                                            iconCtxTarget  = item
                                            iconCtxOffset  = Offset(
                                                pos.x + cellWPx / 2,
                                                pos.y + cellHPx / 2
                                            )
                                            showDesktopCtx = false
                                        } else {
                                            // Real drag ended — snap to grid and save position
                                            val maxX = with(density) { (screenW.dp - cellWDp.dp).toPx() }
                                            val maxY = with(density) { (screenH.dp - cellHDp.dp).toPx() }
                                            val snapped = snapToGrid(pos, cellWPx, cellHPx, padLeftPx, padTopPx)
                                            val finalPos = Offset(
                                                snapped.x.coerceIn(padLeftPx, maxX),
                                                snapped.y.coerceIn(padTopPx, maxY)
                                            )
                                            if (autoArrange) {
                                                pos = basePos   // bounce back if auto-arrange is on
                                            } else {
                                                pos = finalPos
                                                customPositions[item.id] = finalPos
                                            }
                                        }
                                        dragMoved = false
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        dragMoved = false
                                        if (autoArrange) pos = basePos
                                    }
                                )
                            }
                    ) {
                        DesktopIcon(
                            item              = item,
                            isSelected        = item.id in selectedIds,
                            iconSize          = iconSize,
                            inlineRenaming    = inlineRename?.targetId == item.id,
                            inlineRenameText  = if (inlineRename?.targetId == item.id)
                                inlineRename!!.currentName else "",
                            onInlineRenameChange  = { newVal ->
                                inlineRename = inlineRename?.copy(currentName = newVal)
                            },
                            onInlineRenameConfirm = {
                                inlineRename?.let { r -> commitRename(r, r.currentName) }
                            },
                            onTap = {
                                // FIX: commit rename on tap of a different icon
                                val r = inlineRename
                                if (r != null && r.targetId != item.id) {
                                    commitRename(r, r.currentName)
                                }
                                selectedIds    = if (item.id in selectedIds)
                                    selectedIds - item.id else setOf(item.id)
                                showDesktopCtx = false
                                iconCtxTarget  = null
                            },
                            onDoubleTap = { openItem(item) }
                            // Long press is now handled by detectDragGesturesAfterLongPress above
                            // with the dragMoved threshold check (no movement = context menu)
                        )
                    }
                }

                // Lasso selection rectangle
                if (isSelecting) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = Rect(
                            minOf(selStart.x, selEnd.x), minOf(selStart.y, selEnd.y),
                            maxOf(selStart.x, selEnd.x), maxOf(selStart.y, selEnd.y)
                        )
                        drawRect(
                            color    = Color(0xFF0078D4).copy(alpha = 0.12f),
                            topLeft  = r.topLeft,
                            size     = Size(r.width, r.height)
                        )
                        drawRect(
                            color    = Color(0xFF0078D4).copy(alpha = 0.50f),
                            topLeft  = r.topLeft,
                            size     = Size(r.width, r.height),
                            style    = Stroke(width = 1.2f.dp.toPx())
                        )
                    }
                }
            }
        }

        // ── Desktop context menu ──
        if (showDesktopCtx) {
            Win11DesktopContextMenu(
                offset            = desktopCtxOffset,
                isDark            = isDark,
                screenWidthDp     = screenW,
                screenHeightDp    = screenH,
                viewMode          = iconSize,
                onViewChange      = { iconSize = it; showDesktopCtx = false },
                sortMode          = sortMode,
                sortAscending     = sortAscending,
                onSortChange      = { m, a -> sortMode = m; sortAscending = a; showDesktopCtx = false },
                autoArrange       = autoArrange,
                onAutoArrangeToggle = {
                    autoArrange = it
                    if (it) customPositions.clear()
                    showDesktopCtx = false
                },
                showIcons         = showIconsOnDesktop,
                onShowIconsToggle = { showIconsOnDesktop = it; showDesktopCtx = false },
                onRefresh         = { refreshDesktop(); showDesktopCtx = false },
                onPaste           = {
                    clipboardFiles.forEach { (f, cut) ->
                        val dest = File(desktopDir, uniqueName(desktopDir, f.nameWithoutExtension, f.extension))
                        try {
                            if (cut) f.renameTo(dest) else f.copyTo(dest, overwrite = false)
                        } catch (_: Exception) {}
                    }
                    if (clipboardFiles.any { it.second }) clipboardFiles = emptyList()
                    scheduleRefresh()
                    showDesktopCtx = false
                },
                hasPaste          = clipboardFiles.isNotEmpty(),
                onNewFolder       = {
                    val name   = uniqueName(desktopDir, "New folder")
                    val newDir = File(desktopDir, name)
                    newDir.mkdirs()
                    pendingRenameId = newDir.absolutePath
                    showDesktopCtx  = false
                    scheduleRefresh()
                },
                onNewTextFile     = {
                    val name    = uniqueName(desktopDir, "New Text Document", "txt")
                    val newFile = File(desktopDir, name)
                    try { newFile.createNewFile() } catch (_: Exception) {}
                    pendingRenameId = newFile.absolutePath
                    showDesktopCtx  = false
                    scheduleRefresh()
                },
                onNewShortcut     = { showShortcutDialog  = true; showDesktopCtx = false },
                onAddAppShortcut  = { showAppPickerDialog = true; showDesktopCtx = false },
                onPersonalize     = { viewModel.openWallpaperPicker(WallpaperTarget.HOME); showDesktopCtx = false },
                onDisplaySettings = { viewModel.openWindow(LauncherScreen.SETTINGS); showDesktopCtx = false },
                onDismiss         = { showDesktopCtx = false }
            )
        }

        // ── Icon context menu ──
        iconCtxTarget?.let { target ->
            Win11IconContextMenu(
                item              = target,
                isDark            = isDark,
                offset            = iconCtxOffset,
                screenWidthDp     = screenW,
                screenHeightDp    = screenH,
                onDismiss         = { iconCtxTarget = null },
                onOpen            = { openItem(target); iconCtxTarget = null },
                onOpenWith        = {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", target.file
                        )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Open with"
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    } catch (_: Exception) {}
                    iconCtxTarget = null
                },
                onOpenFileLocation = { viewModel.openWindow(LauncherScreen.FILE_EXPLORER); iconCtxTarget = null },
                onCut = {
                    clipboardFiles = selectedIds
                        .mapNotNull { id -> items.find { it.id == id }?.file }
                        .map { it to true }
                        .ifEmpty { listOf(target.file to true) }
                    iconCtxTarget = null
                },
                onCopy = {
                    clipboardFiles = selectedIds
                        .mapNotNull { id -> items.find { it.id == id }?.file }
                        .map { it to false }
                        .ifEmpty { listOf(target.file to false) }
                    iconCtxTarget = null
                },
                onDelete = {
                    val toDelete = selectedIds
                        .mapNotNull { id -> items.find { it.id == id }?.file }
                        .ifEmpty { listOf(target.file) }
                    toDelete.forEach { viewModel.deleteToRecycleBin(it.absolutePath) }
                    selectedIds   = emptySet()
                    iconCtxTarget = null
                    scheduleRefresh()
                },
                onRename = {
                    inlineRename  = InlineRenameState(targetId = target.id, currentName = target.name)
                    selectedIds   = setOf(target.id)
                    iconCtxTarget = null
                },
                onShare = {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", target.file
                        )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Share"
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    } catch (_: Exception) {}
                    iconCtxTarget = null
                },
                onCreateShortcut = {
                    val f = File(desktopDir, uniqueName(desktopDir, target.file.nameWithoutExtension, "desktop"))
                    f.writeText("type=file\npath=${target.file.absolutePath}\nlabel=${target.file.nameWithoutExtension}\n")
                    iconCtxTarget = null
                    scheduleRefresh()
                },
                onProperties = { propsTarget = target; showPropsDialog = true; iconCtxTarget = null }
            )
        }

        // ── Modals ──
        if (showShortcutDialog) {
            ShortcutDialog(
                onConfirm = { pkg, label ->
                    val f = File(desktopDir, uniqueName(desktopDir, label, "desktop"))
                    f.writeText("type=app\npackage=$pkg\nlabel=$label\n")
                    showShortcutDialog = false
                    scheduleRefresh()
                },
                onDismiss = { showShortcutDialog = false }
            )
        }

        if (showAppPickerDialog) {
            AppPickerDialog(
                isDark      = isDark,
                onAppSelected = { pkg, label ->
                    val f = File(desktopDir, uniqueName(desktopDir, label, "desktop"))
                    f.writeText("type=app\npackage=$pkg\nlabel=$label\n")
                    showAppPickerDialog = false
                    scheduleRefresh()
                },
                onDismiss = { showAppPickerDialog = false }
            )
        }

        if (showPropsDialog && propsTarget != null) {
            PropertiesDialog(item = propsTarget!!, isDark = isDark, onDismiss = { showPropsDialog = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// DesktopIcon
// ─────────────────────────────────────────────────────────────────
@Composable
private fun DesktopIcon(
    item: DesktopFileInfo,
    isSelected: Boolean,
    iconSize: DesktopIconSize,
    inlineRenaming: Boolean,
    inlineRenameText: String,
    onInlineRenameChange: (String) -> Unit,
    onInlineRenameConfirm: () -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
    // Long-press is handled by the parent Box's detectDragGesturesAfterLongPress
) {
    val iconDp  = iconSizeDp(iconSize).dp
    val cellW   = cellWidthDp(iconSize).dp
    val cellH   = cellHeightDp(iconSize).dp
    val focusRequester = remember { FocusRequester() }

    // FIX: auto-focus the rename field after a brief layout delay
    LaunchedEffect(inlineRenaming) {
        if (inlineRenaming) {
            delay(80)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val selectAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.20f else 0f,
        animationSpec = tween(150),
        label         = "selection_alpha"
    )

    Box(
        modifier = Modifier
            .width(cellW)
            .height(cellH)
            .padding(1.dp)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap       = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                    // onLongPress removed — handled by parent drag gesture detector
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Selection highlight
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    color = Color.White.copy(alpha = selectAlpha),
                    shape = RoundedCornerShape(5.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.30f) else Color.Transparent,
                    shape = RoundedCornerShape(5.dp)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp)
        ) {
            Box(
                Modifier
                    .size(iconDp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // FIX: APP_SHORTCUT now always renders if iconBitmap is set (covers AdaptiveIcon).
                // IMAGE_FILE thumbnails also always render when available.
                when {
                    item.iconBitmap != null -> {
                        Image(
                            bitmap       = item.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier     = if (item.type == DesktopItemType.IMAGE_FILE)
                                Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp))
                            else
                                Modifier.fillMaxSize(),
                            contentScale = if (item.type == DesktopItemType.IMAGE_FILE)
                                ContentScale.Crop else ContentScale.Fit
                        )
                    }
                    else -> {
                        Icon(
                            imageVector  = getFileIcon(item.file),
                            contentDescription = null,
                            tint         = getFileIconColor(item.file),
                            modifier     = Modifier.fillMaxSize()
                        )
                    }
                }

                // App shortcut arrow badge
                if (item.type == DesktopItemType.APP_SHORTCUT) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.White, RoundedCornerShape(2.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector  = Icons.Default.Reply,
                            contentDescription = null,
                            tint         = Color.Black,
                            modifier     = Modifier.size(10.dp).graphicsLayer(scaleX = -1f)
                        )
                    }
                }

                // Audio / Video type badge
                if (item.type == DesktopItemType.MUSIC_FILE || item.type == DesktopItemType.VIDEO_FILE) {
                    val badgeColor = if (item.type == DesktopItemType.MUSIC_FILE)
                        Color(0xFFFF8C00) else Color(0xFF8764B8)
                    val badgeIcon  = if (item.type == DesktopItemType.MUSIC_FILE)
                        Icons.Default.MusicNote else Icons.Default.PlayArrow
                    Box(
                        Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF1C1C1C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(badgeIcon, null, tint = badgeColor, modifier = Modifier.size(9.dp))
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // ── Inline rename field ──
            if (inlineRenaming) {
                // FIX: initialise text from the name without extension to match Windows UX
                val rawInitial = remember(inlineRenameText) {
                    if (inlineRenameText.contains("."))
                        inlineRenameText.substringBeforeLast(".")
                    else inlineRenameText
                }
                var textValue by remember(rawInitial) {
                    mutableStateOf(
                        TextFieldValue(
                            text      = rawInitial,
                            selection = TextRange(0, rawInitial.length)
                        )
                    )
                }

                // FIX: wrapped in Surface for elevation so it's always visible on any wallpaper
                Surface(
                    shape          = RoundedCornerShape(3.dp),
                    color          = Color(0xFF0F0F0F).copy(alpha = 0.93f),
                    shadowElevation = 4.dp,
                    border         = BorderStroke(1.5.dp, Color(0xFF0078D4))
                ) {
                    BasicTextField(
                        value         = textValue,
                        onValueChange = { tv ->
                            textValue = tv
                            onInlineRenameChange(tv.text)
                        },
                        singleLine    = false,
                        maxLines      = 3,
                        textStyle     = TextStyle(
                            color      = Color.White,
                            fontSize   = 11.5.sp,
                            textAlign  = TextAlign.Center,
                            lineHeight  = 14.sp
                        ),
                        // FIX: Done action on keyboard saves the rename
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { onInlineRenameConfirm() }
                        ),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .focusRequester(focusRequester)
                    )
                }
            } else {
                // ── Normal label with shadow so it's readable on any wallpaper ──
                Text(
                    text        = item.name,
                    color       = Color.White,
                    fontSize    = 11.5.sp,
                    lineHeight  = 14.sp,
                    textAlign   = TextAlign.Center,
                    maxLines    = 2,
                    overflow    = TextOverflow.Ellipsis,
                    fontWeight  = FontWeight.Normal,
                    modifier    = Modifier.fillMaxWidth(),
                    style       = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color      = Color.Black.copy(alpha = 0.90f),
                            offset     = Offset(0.8f, 1.2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Win11 Desktop Context Menu
// ─────────────────────────────────────────────────────────────────
@Composable
fun Win11DesktopContextMenu(
    offset: Offset,
    isDark: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
    viewMode: DesktopIconSize,
    onViewChange: (DesktopIconSize) -> Unit,
    sortMode: DesktopSortMode,
    sortAscending: Boolean,
    onSortChange: (DesktopSortMode, Boolean) -> Unit,
    autoArrange: Boolean,
    onAutoArrangeToggle: (Boolean) -> Unit,
    showIcons: Boolean,
    onShowIconsToggle: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPaste: () -> Unit,
    hasPaste: Boolean,
    onNewFolder: () -> Unit,
    onNewTextFile: () -> Unit,
    onNewShortcut: () -> Unit,
    onAddAppShortcut: () -> Unit,
    onPersonalize: () -> Unit,
    onDisplaySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val density  = LocalDensity.current
    val menuW    = 210
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val hoverBg  = if (isDark) Color(0x12FFFFFF) else Color(0x0F000000)
    val accent   = Color(0xFF0078D4)

    var openSub by remember { mutableStateOf<String?>(null) }

    val maxX = with(density) { (screenWidthDp - menuW - 6).dp.toPx() }
    val xOff = offset.x.coerceIn(6f, maxX).roundToInt()
    val yOff = offset.y.coerceAtLeast(6f).roundToInt()

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Surface(
            modifier        = Modifier.offset { IntOffset(xOff, yOff) }.width(menuW.dp),
            shape           = RoundedCornerShape(8.dp),
            color           = bg,
            shadowElevation = 16.dp,
            border          = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
        ) {
            Column(Modifier.padding(vertical = 5.dp)) {
                W11CtxRow(Icons.Default.ViewModule, "View", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "view") null else "view"
                }
                AnimatedVisibility(openSub == "view", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRow("Large icons",  viewMode == DesktopIconSize.LARGE,  tc, accent) { onViewChange(DesktopIconSize.LARGE) }
                        W11SubRow("Medium icons", viewMode == DesktopIconSize.MEDIUM, tc, accent) { onViewChange(DesktopIconSize.MEDIUM) }
                        W11SubRow("Small icons",  viewMode == DesktopIconSize.SMALL,  tc, accent) { onViewChange(DesktopIconSize.SMALL) }
                        W11CtxDivider(divColor)
                        W11SubRow("Auto arrange icons",  autoArrange, tc, accent) { onAutoArrangeToggle(!autoArrange) }
                        W11SubRow("Align icons to grid", true,        tc, accent) {}
                        W11CtxDivider(divColor)
                        W11SubRow("Show desktop icons", showIcons, tc, accent) { onShowIconsToggle(!showIcons) }
                    }
                }

                W11CtxRow(Icons.Default.Sort, "Sort by", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "sort") null else "sort"
                }
                AnimatedVisibility(openSub == "sort", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRow("Name",          sortMode == DesktopSortMode.NAME,          tc, accent) { onSortChange(DesktopSortMode.NAME,          sortAscending) }
                        W11SubRow("Size",          sortMode == DesktopSortMode.SIZE,          tc, accent) { onSortChange(DesktopSortMode.SIZE,          sortAscending) }
                        W11SubRow("Item type",     sortMode == DesktopSortMode.TYPE,          tc, accent) { onSortChange(DesktopSortMode.TYPE,          sortAscending) }
                        W11SubRow("Date modified", sortMode == DesktopSortMode.DATE_MODIFIED, tc, accent) { onSortChange(DesktopSortMode.DATE_MODIFIED, sortAscending) }
                        W11CtxDivider(divColor)
                        W11SubRow("Ascending",  sortAscending,  tc, accent) { onSortChange(sortMode, true) }
                        W11SubRow("Descending", !sortAscending, tc, accent) { onSortChange(sortMode, false) }
                    }
                }

                W11CtxRow(Icons.Default.Refresh, "Refresh", tc, tcDim) { onRefresh() }
                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.ContentPaste, "Paste", if (hasPaste) tc else tcDim, tcDim, enabled = hasPaste) { onPaste() }
                W11CtxDivider(divColor)

                W11CtxRow(Icons.Default.Add, "New", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "new") null else "new"
                }
                AnimatedVisibility(openSub == "new", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRowIcon(Icons.Default.Folder,      "Folder",                    Color(0xFFFFC107), tc) { onNewFolder();       onDismiss() }
                        W11SubRowIcon(Icons.Default.Link,        "Shortcut link",             Color(0xFF0078D4), tc) { onNewShortcut();      onDismiss() }
                        W11SubRowIcon(Icons.Default.Apps,        "Add Installed App Shortcut",Color(0xFF107C10), tc) { onAddAppShortcut();   onDismiss() }
                        W11CtxDivider(divColor)
                        W11SubRowIcon(Icons.Default.Description, "Text Document",             Color(0xFF0078D4), tc) { onNewTextFile();      onDismiss() }
                    }
                }

                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Monitor, "Display settings", tc, tcDim) { onDisplaySettings() }
                W11CtxRow(Icons.Default.Palette, "Personalise",      tc, tcDim) { onPersonalize() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Win11 Icon Context Menu
// ─────────────────────────────────────────────────────────────────
@Composable
fun Win11IconContextMenu(
    item: DesktopFileInfo,
    isDark: Boolean,
    offset: Offset,
    screenWidthDp: Int,
    screenHeightDp: Int,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onOpenFileLocation: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onCreateShortcut: () -> Unit,
    onProperties: () -> Unit
) {
    val density  = LocalDensity.current
    val menuW    = 220
    val estH     = 360
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val danger   = Color(0xFFE81123)

    var openSub by remember { mutableStateOf<String?>(null) }

    val maxX = with(density) { (screenWidthDp - menuW - 6).dp.toPx() }
    val maxY = with(density) { (screenHeightDp - estH - 6).dp.toPx() }
    val xOff = offset.x.coerceIn(6f, maxX).roundToInt()
    val yOff = offset.y.coerceIn(6f, maxY.coerceAtLeast(6f)).roundToInt()

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Surface(
            modifier        = Modifier.offset { IntOffset(xOff, yOff) }.width(menuW.dp),
            shape           = RoundedCornerShape(8.dp),
            color           = bg,
            shadowElevation = 16.dp,
            border          = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
        ) {
            Column(Modifier.padding(vertical = 5.dp)) {
                // Quick action row
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    W11QuickAction(Icons.Default.ContentCut,            "Cut",    tc)     { onCut();    onDismiss() }
                    W11QuickAction(Icons.Default.ContentCopy,           "Copy",   tc)     { onCopy();   onDismiss() }
                    W11QuickAction(Icons.Default.DriveFileRenameOutline,"Rename", tc)     { onRename(); onDismiss() }
                    W11QuickAction(Icons.Default.Share,                 "Share",  tc)     { onShare();  onDismiss() }
                    W11QuickAction(Icons.Default.Delete,                "Delete", danger) { onDelete(); onDismiss() }
                }

                W11CtxDivider(divColor)

                W11CtxRow(Icons.Default.OpenInNew, "Open", tc, tcDim, isBold = true) { onOpen(); onDismiss() }

                W11CtxRow(Icons.Default.OpenWith, "Open with", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "openwith") null else "openwith"
                }
                AnimatedVisibility(openSub == "openwith", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(Color.Black.copy(0.02f))) {
                        W11SubRowIcon(Icons.Default.OpenInNew, "Choose app", tc.copy(0.8f), tc) { onOpenWith(); onDismiss() }
                    }
                }

                if (item.type == DesktopItemType.APP_SHORTCUT) {
                    W11CtxRow(Icons.Outlined.FolderOpen, "Open file location", tc, tcDim) { onOpenFileLocation(); onDismiss() }
                }

                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Link,        "Create shortcut", tc,     tcDim) { onCreateShortcut(); onDismiss() }
                W11CtxRow(Icons.Default.Delete,      "Delete",          danger, tcDim) { onDelete();         onDismiss() }
                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Info,        "Properties",      tc,     tcDim) { onProperties();     onDismiss() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Context menu primitives
// ─────────────────────────────────────────────────────────────────
@Composable
private fun W11CtxRow(
    icon: ImageVector,
    label: String,
    tc: Color,
    tcDim: Color,
    hasArrow: Boolean = false,
    isBold: Boolean   = false,
    enabled: Boolean  = true,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 12.dp)
            .height(32.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = tc.copy(0.8f), modifier = Modifier.size(15.dp))
        Text(
            label,
            color      = tc,
            fontSize   = 12.5.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier   = Modifier.weight(1f),
            maxLines   = 1
        )
        if (hasArrow) Icon(Icons.Default.ChevronRight, null, tint = tcDim, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun W11SubRow(
    label: String,
    isActive: Boolean,
    tc: Color,
    accent: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(start = 34.dp, end = 12.dp)
            .height(28.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isActive) Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(13.dp))
        else          Spacer(Modifier.size(13.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1)
    }
}

@Composable
private fun W11SubRowIcon(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    tc: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(start = 22.dp, end = 12.dp)
            .height(28.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun W11QuickAction(
    icon: ImageVector,
    tooltip: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHovered) tint.copy(0.10f) else Color.Transparent)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = tooltip, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun W11CtxDivider(color: Color) {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        thickness = 1.dp,
        color     = color
    )
}

// ─────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────
@Composable
fun SmartNewItemDialog(
    title: String,
    suggested: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(suggested) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(8.dp),
        title            = { Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        text             = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                singleLine    = true,
                shape         = RoundedCornerShape(4.dp),
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton    = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Create") }
        },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// FIX: ShortcutDialog now uses AppPickerDialog internally so the user
// can pick from installed apps rather than typing a raw package name.
@Composable
fun ShortcutDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    var pkg        by remember { mutableStateOf("") }
    var label      by remember { mutableStateOf("") }

    if (showPicker) {
        // Reuse AppPickerDialog — when the user picks an app it fills pkg + label
        // and closes the picker, returning to this dialog for confirmation.
        // For simplicity we confirm directly here.
        AppPickerDialog(
            isDark = true,
            onAppSelected = { p, l ->
                pkg         = p
                label       = l
                showPicker  = false
                // Confirm immediately since the user explicitly chose an app
                onConfirm(p, l)
            },
            onDismiss = { showPicker = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(8.dp),
        title            = { Text("Create Shortcut", fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text("Display Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = pkg,
                    onValueChange = { pkg = it },
                    label         = { Text("Package Name (e.g. com.example.app)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick  = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Apps, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse installed apps…")
                }
            }
        },
        confirmButton    = {
            Button(onClick = {
                if (pkg.isNotBlank() && label.isNotBlank()) onConfirm(pkg.trim(), label.trim())
            }) { Text("Create") }
        },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────
// App Picker Dialog
// FIX: Uses drawableToBitmap() so all app icons render regardless
// of their Drawable type (BitmapDrawable, AdaptiveIconDrawable, etc.)
// ─────────────────────────────────────────────────────────────────
@Composable
fun AppPickerDialog(
    isDark: Boolean,
    onAppSelected: (packageName: String, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context      = LocalContext.current
    var appsList     by remember { mutableStateOf(listOf<AppInfoItem>()) }
    var searchQuery  by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(true) }

    val bg = if (isDark) Color(0xFF202020) else Color.White
    val tc = if (isDark) Color.White else Color.Black

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val resolved = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { app ->
                    val intent = pm.getLaunchIntentForPackage(app.packageName) ?: return@mapNotNull null
                    // FIX: drawableToBitmap handles every Drawable type
                    val bitmap = try { drawableToBitmap(app.loadIcon(pm)) } catch (_: Exception) { null }
                        ?: return@mapNotNull null
                    AppInfoItem(
                        label       = app.loadLabel(pm).toString(),
                        packageName = app.packageName,
                        iconBitmap  = bitmap
                    )
                }
                .sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                appsList  = resolved
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(10.dp),
        title            = {
            Text("Add App Shortcut", color = tc, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        },
        text             = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search apps…") },
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    modifier      = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape         = RoundedCornerShape(6.dp)
                )

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0078D4))
                    }
                } else {
                    val filtered = remember(appsList, searchQuery) {
                        if (searchQuery.isBlank()) appsList
                        else appsList.filter {
                            it.label.contains(searchQuery, ignoreCase = true) ||
                                    it.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No apps found", color = tc.copy(0.5f), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filtered, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAppSelected(app.packageName, app.label) }
                                        .padding(vertical = 7.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // FIX: iconBitmap is always non-null (resolved above)
                                    Image(
                                        bitmap       = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier     = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(app.packageName, color = tc.copy(alpha = 0.5f), fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton    = {},
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF0078D4)) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Properties Dialog
// ─────────────────────────────────────────────────────────────────
@Composable
fun PropertiesDialog(item: DesktopFileInfo, isDark: Boolean, onDismiss: () -> Unit) {
    val bg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val tc  = if (isDark) Color.White else Color(0xFF1A1A1A)
    val tcm = if (isDark) Color(0xFF909090) else Color(0xFF666666)

    var fileSize by remember { mutableStateOf(0L) }
    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            fileSize = if (item.file.isDirectory)
                item.file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            else
                item.file.length()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(8.dp),
        title            = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(getFileIcon(item.file), null,
                    tint     = getFileIconColor(item.file),
                    modifier = Modifier.size(22.dp)
                )
                Text("Properties", color = tc, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Name"     to item.name,
                    "Type"     to if (item.file.isDirectory) "Folder"
                    else item.file.extension.uppercase().ifBlank { "File" },
                    "Size"     to formatFileSize(fileSize),
                    "Location" to (item.file.parent ?: "/"),
                    "Modified" to SimpleDateFormat(
                        "yyyy-MM-dd  HH:mm:ss", Locale.getDefault()
                    ).format(Date(item.file.lastModified()))
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(k, color = tcm, fontSize = 11.5.sp, modifier = Modifier.width(72.dp))
                        Text(v, color = tc,  fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f), maxLines = 3,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton    = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}
