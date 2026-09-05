package io.github.norbertweb.bluebird.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.ui.components.BluebirdExecutable
import io.github.norbertweb.bluebird.ui.components.BpkPackageIcon
import io.github.norbertweb.bluebird.LauncherScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.ArrowClockwise
import fluent.ui.system.icons.regular.ArrowDownload
import fluent.ui.system.icons.regular.ArrowLeft
import fluent.ui.system.icons.regular.ArrowUp
import fluent.ui.system.icons.regular.Camera
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.ChevronUp
import fluent.ui.system.icons.regular.ClipboardPaste
import fluent.ui.system.icons.regular.Code
import fluent.ui.system.icons.regular.Copy
import fluent.ui.system.icons.regular.Cut
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Desktop
import fluent.ui.system.icons.regular.DiamondDismiss
import fluent.ui.system.icons.regular.Document
import fluent.ui.system.icons.regular.DocumentOnePage
import fluent.ui.system.icons.regular.DocumentPdf
import fluent.ui.system.icons.regular.Eye
import fluent.ui.system.icons.regular.EyeOff
import fluent.ui.system.icons.regular.Folder
import fluent.ui.system.icons.regular.FolderAdd
//import fluent.ui.system.icons.regular.FolderOff
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.FolderZip
import fluent.ui.system.icons.regular.Globe
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.HardDrive
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Link

import fluent.ui.system.icons.regular.MusicNote2

import fluent.ui.system.icons.regular.Phone
import fluent.ui.system.icons.regular.Rename
import fluent.ui.system.icons.regular.Search

import fluent.ui.system.icons.regular.SelectAllOn
import fluent.ui.system.icons.regular.SlideLayout
import fluent.ui.system.icons.regular.Table
import fluent.ui.system.icons.regular.VideoClip
import fluent.ui.system.icons.filled.Play
import fluent.ui.system.icons.regular.FolderProhibited
import fluent.ui.system.icons.regular.List
import fluent.ui.system.icons.regular.Open
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import io.github.norbertweb.bluebird.wordprocessor.formatDate
import io.github.norbertweb.bluebird.wordprocessor.formatFileSize

// ────────────────────────────────────────────────────────
// Data & Helpers
// ────────────────────────────────────────────────────────

data class RealFileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val iconBitmap: Bitmap? = null
)



fun getFileIcon(item: RealFileItem): ImageVector = when {
    item.isDirectory -> FluentIcons.Regular.Folder
    item.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> FluentIcons.Regular.Image
    item.extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> FluentIcons.Regular.VideoClip
    item.extension in listOf("mp3", "wav", "ogg", "flac", "aac", "m4a") -> FluentIcons.Regular.MusicNote2
    item.extension in listOf("pdf") -> FluentIcons.Regular.DocumentPdf
    item.extension in listOf("txt", "log", "md", "xml", "json", "csv") -> FluentIcons.Regular.Document
    item.extension in listOf("zip", "rar", "7z", "tar", "gz") -> FluentIcons.Regular.FolderZip
    item.extension in listOf("apk") -> FluentIcons.Regular.Phone
    item.extension in listOf("doc", "docx") -> FluentIcons.Regular.DocumentOnePage
    item.extension in listOf("xls", "xlsx") -> FluentIcons.Regular.Table
    item.extension in listOf("ppt", "pptx") -> FluentIcons.Regular.SlideLayout
    item.extension in listOf("html", "htm") -> FluentIcons.Regular.Code
    item.extension == "webapp" -> FluentIcons.Regular.Globe
    item.extension == "bpk" -> FluentIcons.Regular.FolderZip
    item.extension == "exe" -> FluentIcons.Regular.Apps
    item.extension == "io.github.norbertweb.io.github.norbertweb.bluebird" -> FluentIcons.Regular.Apps
    else -> FluentIcons.Regular.Document
}

fun getFileIconColor(item: RealFileItem): Color = when {
    item.isDirectory -> Color(0xFFFFC107)
    item.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Color(0xFF4CAF50)
    item.extension in listOf("mp4", "mkv", "avi", "mov", "webm") -> Color(0xFF9C27B0)
    item.extension in listOf("mp3", "wav", "ogg", "flac", "aac") -> Color(0xFFFF5722)
    item.extension == "pdf" -> Color(0xFFF44336)
    item.extension == "apk" -> Color(0xFF4CAF50)
    item.extension in listOf("doc", "docx") -> Color(0xFF2196F3)
    item.extension in listOf("xls", "xlsx") -> Color(0xFF4CAF50)
    item.extension == "webapp" -> Color(0xFF0078D4)
    item.extension == "bpk" -> Color(0xFF0078D4)
    item.extension == "exe" -> Color(0xFF0078D4)
    item.extension == "io.github.norbertweb.io.github.norbertweb.bluebird" -> Color(0xFF0078D4)
    else -> Color(0xFF9E9E9E)
}

fun getMimeType(file: File): String {
    val ext = file.extension.lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "pdf" -> "application/pdf"
        "txt", "log", "md" -> "text/plain"
        "html", "htm" -> "text/html"
        "xml" -> "text/xml"
        "json" -> "application/json"
        "csv" -> "text/csv"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "apk" -> "application/vnd.android.package-archive"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "*/*"
    }
}

// ────────────────────────────────────────────────────────
// Permission Helper
// ────────────────────────────────────────────────────────

private fun hasStorageAccess(context: android.content.Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            Environment.isExternalStorageManager() ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        else ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

// ────────────────────────────────────────────────────────
// State Holder  ← KEY FIX: moves all mutableStateOf out of
// the root composable so the compiler emits far fewer
// registers in FileExplorerScreen itself.
// ────────────────────────────────────────────────────────

private class FileExplorerState(initialDir: File) {
    var currentDir by mutableStateOf(initialDir)
    var pathHistory by mutableStateOf(listOf<File>())
    var allFiles by mutableStateOf(listOf<RealFileItem>())

    /** Derived listing: search/sort never touches the filesystem. */
    val files: List<RealFileItem>
        get() {
            val query = searchQuery.trim()
            val filtered = if (query.isEmpty()) allFiles else allFiles.filter {
                it.name.contains(query, ignoreCase = true)
            }
            val sorted = when (sortBy) {
                "name" -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                "size" -> filtered.sortedBy { it.size }
                "date" -> filtered.sortedBy { it.lastModified }
                "type" -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.extension })
                else -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
            val dirs = sorted.filter { it.isDirectory }
            val filesOnly = sorted.filter { it.file.isFile }
            return if (sortAscending) dirs + filesOnly else (filesOnly + dirs).reversed()
        }
    var isLoading by mutableStateOf(true)

    // UI toggles
    var isGridView by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var selectedFiles by mutableStateOf(setOf<String>())
    var sortBy by mutableStateOf("name")
    var sortAscending by mutableStateOf(true)
    var showHidden by mutableStateOf(false)

    // Dialogs
    var contextMenuFile by mutableStateOf<RealFileItem?>(null)
    var contextMenuOffset by mutableStateOf(Offset.Zero)
    var showBackgroundContextMenu by mutableStateOf(false)
    var showRenameDialog by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)
    var renameTarget by mutableStateOf<RealFileItem?>(null)
    var showNewFolderDialog by mutableStateOf(false)
    var propertiesTarget by mutableStateOf<RealFileItem?>(null)
    // Clipboard now lives in the ViewModel (shared with Desktop) — see vmUiState.clipboardFiles

    fun navigateTo(dir: File) {
        if (dir != currentDir) {
            pathHistory = pathHistory + currentDir
            currentDir = dir
        }
    }

    fun goBack() {
        if (pathHistory.isNotEmpty()) {
            currentDir = pathHistory.last()
            pathHistory = pathHistory.dropLast(1)
        }
    }

    fun goUp() {
        val parent = currentDir.parentFile
        if (parent != null && parent != currentDir) {
            pathHistory = pathHistory + currentDir
            currentDir = parent
        }
    }
}

@Composable
private fun rememberFileExplorerState(initialDir: File = Environment.getExternalStorageDirectory()): FileExplorerState {
    return remember(initialDir.absolutePath) { FileExplorerState(initialDir) }
}

// ────────────────────────────────────────────────────────
// Main Entry Point — now tiny, delegates everything
// ────────────────────────────────────────────────────────

@Composable
fun FileExplorerScreen(
    isDark: Boolean,
    viewModel: LauncherViewModel? = null,
    // Optional starting folder — e.g. when opened by double-clicking a folder on the
    // Desktop, this is that folder's path instead of always defaulting to the storage root.
    startPath: String? = null
) {
    val context = LocalContext.current
    var hasStoragePermission by remember { mutableStateOf(hasStorageAccess(context)) }

    if (!hasStoragePermission) {
        PermissionGate(
            isDark = isDark,
            onPermissionGranted = { hasStoragePermission = true }
        )
        return
    }

    val startDir = remember(startPath, viewModel) {
        val resolved = startPath?.let { raw ->
            if (raw.startsWith("content://")) {
                viewModel?.resolveSafUriToFilePath(context, Uri.parse(raw))
            } else raw
        }
        val f = resolved?.let { File(it) }
        if (f != null && f.isDirectory) f else Environment.getExternalStorageDirectory()
    }
    FileExplorerContent(isDark = isDark, viewModel = viewModel, startDir = startDir)
}

// ────────────────────────────────────────────────────────
// Permission Gate — extracted composable
// ────────────────────────────────────────────────────────

@Composable
private fun PermissionGate(
    isDark: Boolean,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFFAFAFA)

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || hasStorageAccess(context)) onPermissionGranted()
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it } || hasStorageAccess(context)) onPermissionGranted()
    }

    fun requestPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                mediaPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                )
            }
            else -> legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) { requestPermission() }

    Box(Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                FluentIcons.Regular.FolderProhibited,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text("Storage access required", color = textColor, style = MaterialTheme.typography.titleMedium)
            Text(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    "Tap below to grant 'All files access' in Settings"
                else
                    "Tap below to grant storage permission",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(onClick = { requestPermission() }) {
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Open Settings"
                    else "Grant Storage Access"
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                OutlinedButton(onClick = {
                    if (hasStorageAccess(context)) onPermissionGranted()
                }) {
                    Text("I've granted access — retry")
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────
// Main Content — extracted composable
// ────────────────────────────────────────────────────────

@Composable
private fun FileExplorerContent(
    isDark: Boolean,
    viewModel: LauncherViewModel?,
    startDir: File = Environment.getExternalStorageDirectory()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberFileExplorerState(startDir)
    // Shared clipboard state (same one Desktop reads/writes) — collected here so the
    // Ribbon's paste button reacts live to a cut/copy made on Desktop or in another window.
    val vmUiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value

    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFFAFAFA)
    val surfaceBg = if (isDark) Color(0xFF252525) else Color(0xFFF0F0F0)
    val navBg = if (isDark) Color(0xFF202020) else Color(0xFFF3F5F7)
    val contentBorder = textColor.copy(alpha = if (isDark) 0.10f else 0.08f)
    val explorerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var ctrlMouseSelection by remember { mutableStateOf(false) }
    var shiftMouseSelection by remember { mutableStateOf(false) }
    var selectionAnchor by remember { mutableStateOf<String?>(null) }

    // Desktop keyboard commands. The explorer owns these only while its root has focus;
    // text fields such as Search keep their normal editing shortcuts.
    fun selectedItems(): List<RealFileItem> = state.files.filter { it.file.absolutePath in state.selectedFiles }
    fun primarySelection(): RealFileItem? = selectedItems().firstOrNull()
    fun openSelection() {
        primarySelection()?.let {
            if (it.isDirectory) state.navigateTo(it.file) else openFileFromExplorer(it, context, viewModel)
        }
    }
    fun beginRename() {
        primarySelection()?.let {
            state.renameTarget = it
            state.showRenameDialog = true
            state.contextMenuFile = null
        }
    }
    fun beginDelete() {
        if (state.selectedFiles.isNotEmpty()) {
            state.contextMenuFile = null
            state.showDeleteDialog = true
        }
    }

    fun moveSelection(delta: Int, extend: Boolean = false) {
        val items = state.files
        if (items.isEmpty()) return
        val current = state.selectedFiles.firstOrNull()?.let { path ->
            items.indexOfFirst { it.file.absolutePath == path }
        } ?: -1
        val next = when {
            current < 0 -> if (delta >= 0) 0 else items.lastIndex
            else -> (current + delta).coerceIn(0, items.lastIndex)
        }
        val nextPath = items[next].file.absolutePath
        if (extend) {
            val anchor = selectionAnchor ?: state.selectedFiles.firstOrNull() ?: nextPath
            val anchorIndex = items.indexOfFirst { it.file.absolutePath == anchor }.takeIf { it >= 0 } ?: next
            val range = if (anchorIndex <= next) anchorIndex..next else next..anchorIndex
            state.selectedFiles = range.mapTo(linkedSetOf()) { items[it].file.absolutePath }
            selectionAnchor = anchor
        } else {
            state.selectedFiles = setOf(nextPath)
            selectionAnchor = nextPath
        }
    }

    // FIX: the explorer root requests focus so hardware-keyboard shortcuts (Ctrl+C,
    // Delete, arrow-key navigation) work without needing a click first — but focusing
    // a plain non-text-editable node like this can still make Android pop the on-screen
    // keyboard on some devices, since focus and IME visibility aren't strictly coupled
    // to "is this actually a text field". Explicitly hiding it right after keeps the
    // shortcut-handling focus while stopping the keyboard from appearing uninvited.
    // The Search field is unaffected — it only shows the keyboard when the user
    // actually taps into it, same as Media Player's search box already does.
    LaunchedEffect(Unit) {
        explorerFocusRequester.requestFocus()
        keyboardController?.hide()
    }

    val quickAccess = remember {
        listOf(
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "DCIM" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Internal Storage" to Environment.getExternalStorageDirectory()
        ).filter { (_, dir) -> dir.exists() }
    }

    // ── File Loading ──
    var loadGeneration by remember { mutableIntStateOf(0) }
    val loadFiles: (File) -> Unit = { dir ->
        val generation = loadGeneration + 1
        loadGeneration = generation
        scope.launch {
            state.isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    if (!dir.exists() || !dir.canRead()) {
                        withContext(Dispatchers.Main.immediate) { state.allFiles = emptyList() }
                        return@withContext
                    }
                    val rawFiles = dir.listFiles()?.asSequence() ?: emptySequence()
                    val loaded = ArrayList<RealFileItem>()
                    rawFiles
                        .filter { state.showHidden || !it.name.startsWith(".") }
                        .forEach { file ->
                            val icon = when {
                                file.extension.equals("exe", true) -> {
                                    BluebirdExecutable.read(file)?.let { descriptor ->
                                        runCatching {
                                            BluebirdExecutable.resolveIcon(file, descriptor)
                                                .takeIf { it.isFile }
                                                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                                        }.getOrNull()
                                    }
                                }
                                file.extension.equals("bpk", true) -> {
                                    BpkPackageIcon.decode(file)
                                }
                                else -> null
                            }
                            loaded += RealFileItem(file, iconBitmap = icon)
                        }
                    withContext(Dispatchers.Main.immediate) {
                        if (generation == loadGeneration && dir == state.currentDir) {
                            // Publish one immutable snapshot. Search and sorting are derived locally.
                            state.allFiles = loaded
                            val loadedPaths = loaded.asSequence().map { it.file.absolutePath }.toHashSet()
                            state.selectedFiles = state.selectedFiles.filterTo(linkedSetOf()) { it in loadedPaths }
                        }
                    }
                } catch (e: SecurityException) {
                    withContext(Dispatchers.Main.immediate) { state.allFiles = emptyList() }
                } finally {
                    withContext(Dispatchers.Main.immediate) {
                        if (generation == loadGeneration && dir == state.currentDir) {
                            state.isLoading = false
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.currentDir, state.showHidden) {
        loadFiles(state.currentDir)
    }

    // Auto-refresh this folder's listing once a copy/move job that targets it finishes —
    // paste/copy now runs asynchronously in the shared engine instead of the old
    // synchronous copyTo() that could refresh inline right after.
    LaunchedEffect(vmUiState?.copyJobs, state.currentDir) {
        val justFinished = vmUiState?.copyJobs?.any {
            it.destDir == state.currentDir.absolutePath &&
                it.status != io.github.norbertweb.bluebird.CopyJobStatus.RUNNING &&
                it.status != io.github.norbertweb.bluebird.CopyJobStatus.SCANNING
        } ?: false
        if (justFinished) loadFiles(state.currentDir)
    }

    // ── Layout ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .focusRequester(explorerFocusRequester)
            .onKeyEvent { event ->
                // Keep modifier state available to pointer clicks so Ctrl/Shift-click
                // behaves like a desktop file manager. Key events are delivered while
                // this root owns focus, including when the pointer is over a file tile.
                ctrlMouseSelection = event.nativeKeyEvent.isCtrlPressed
                shiftMouseSelection = event.nativeKeyEvent.isShiftPressed
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val ctrl = event.nativeKeyEvent.isCtrlPressed
                val shift = event.nativeKeyEvent.isShiftPressed
                val alt = event.nativeKeyEvent.isAltPressed
                when {
                    ctrl && event.key == Key.A -> {
                        state.selectedFiles = state.files.mapTo(linkedSetOf()) { it.file.absolutePath }
                        selectionAnchor = state.files.firstOrNull()?.file?.absolutePath
                        true
                    }
                    ctrl && event.key == Key.C -> {
                        if (state.selectedFiles.isNotEmpty()) viewModel?.setClipboard(state.selectedFiles.map { File(it) }, cut = false)
                        true
                    }
                    ctrl && event.key == Key.X -> {
                        if (state.selectedFiles.isNotEmpty()) viewModel?.setClipboard(state.selectedFiles.map { File(it) }, cut = true)
                        true
                    }
                    ctrl && event.key == Key.V -> {
                        viewModel?.pasteClipboard(state.currentDir)
                        true
                    }
                    event.key == Key.F2 -> { beginRename(); true }
                    event.key == Key.Delete -> { beginDelete(); true }
                    event.key == Key.Enter -> { openSelection(); true }
                    event.key == Key.DirectionDown || event.key == Key.DirectionRight -> {
                        moveSelection(1, extend = shift); true
                    }
                    event.key == Key.DirectionUp || event.key == Key.DirectionLeft -> {
                        moveSelection(-1, extend = shift); true
                    }
                    event.key == Key.MoveHome -> {
                        state.files.firstOrNull()?.let { state.selectedFiles = setOf(it.file.absolutePath); selectionAnchor = it.file.absolutePath }; true
                    }
                    event.key == Key.MoveEnd -> {
                        state.files.lastOrNull()?.let { state.selectedFiles = setOf(it.file.absolutePath); selectionAnchor = it.file.absolutePath }; true
                    }
                    event.key == Key.Escape -> {
                        state.contextMenuFile = null
                        state.selectedFiles = emptySet()
                        state.showRenameDialog = false
                        state.showDeleteDialog = false
                        state.propertiesTarget = null
                        true
                    }
                    alt && event.key == Key.DirectionUp -> { state.goUp(); true }
                    event.key == Key.Backspace && !ctrl && !shift -> { state.goBack(); true }
                    else -> false
                }
            }
    ) {

        CommandBar(
            onBack = { state.goBack() },
            onUp = { state.goUp() },
            onRefresh = { loadFiles(state.currentDir) },
            pathParts = remember(state.currentDir) {
                val externalRoot = Environment.getExternalStorageDirectory().absolutePath
                val trimmed = state.currentDir.absolutePath.removePrefix(externalRoot)
                val parts = mutableListOf("This PC" to Environment.getExternalStorageDirectory())
                var curr = Environment.getExternalStorageDirectory()
                trimmed.split("/").filter { it.isNotEmpty() }.forEach { part ->
                    curr = File(curr, part)
                    parts.add(part to curr)
                }
                parts
            },
            onNavigate = { state.navigateTo(it) },
            isGridView = state.isGridView,
            onToggleView = { state.isGridView = !state.isGridView },
            searchQuery = state.searchQuery,
            onSearchChange = { state.searchQuery = it },
            textColor = textColor,
            surfaceBg = surfaceBg,
            isDark = isDark
        )

        Ribbon(
            onNewFolder = { state.showNewFolderDialog = true },
            onCut = {
                if (state.selectedFiles.isNotEmpty() && viewModel != null) {
                    viewModel.setClipboard(state.selectedFiles.map { File(it) }, cut = true)
                }
            },
            onCopy = {
                if (state.selectedFiles.isNotEmpty() && viewModel != null) {
                    viewModel.setClipboard(state.selectedFiles.map { File(it) }, cut = false)
                }
            },
            onPaste = {
                viewModel?.pasteClipboard(state.currentDir)
            },
            onDelete = {
                if (state.selectedFiles.isNotEmpty()) {
                    state.showDeleteDialog = true
                }
            },
            onSelectAll = {
                state.selectedFiles = if (state.selectedFiles.size == state.files.size) emptySet()
                else state.files.mapTo(linkedSetOf()) { it.file.absolutePath }
            },
            sortBy = state.sortBy,
            sortAscending = state.sortAscending,
            onSortChange = { by, asc -> state.sortBy = by; state.sortAscending = asc },
            showHidden = state.showHidden,
            onToggleHidden = { state.showHidden = !state.showHidden },
            clipboardActive = vmUiState?.clipboardFiles?.isNotEmpty() ?: false,
            textColor = textColor,
            surfaceBg = surfaceBg,
            isDark = isDark,
            itemCount = state.files.size
        )

        Row(modifier = Modifier
            .fillMaxWidth()
            .weight(1f) ) {
            NavigationPane(
                quickAccess = quickAccess,
                currentDir = state.currentDir,
                onNavigate = { state.navigateTo(it) },
                textColor = textColor,
                navBg = navBg,
                surfaceBg = surfaceBg,
                isDark = isDark
            )

            FileListArea(
                state = state,
                bgColor = bgColor,
                surfaceBg = surfaceBg,
                textColor = textColor,
                isDark = isDark,
                context = context,
                viewModel = viewModel,
                onBackgroundContextMenu = { offset ->
                    state.contextMenuOffset = offset
                    state.showBackgroundContextMenu = true
                    state.contextMenuFile = null
                }
            )
        }

        StatusBar(
            textColor = textColor,
            itemCount = state.files.size,
            selectedCount = state.selectedFiles.size
        )
    }

    // ── Overlays (dialogs, menus) — extracted ──
    FileExplorerDialogs(
        state = state,
        viewModel = viewModel,
        isDark = isDark,
        context = context,
        clipboardActive = vmUiState?.clipboardFiles?.isNotEmpty() == true,
        onReload = { loadFiles(state.currentDir) }
    )
}

// ────────────────────────────────────────────────────────
// File List Area — extracted composable
// ────────────────────────────────────────────────────────

private fun LayoutCoordinates.trueScreenPosition(view: android.view.View): Offset {
    val loc = IntArray(2)
    view.getLocationOnScreen(loc)
    val inWindow = positionInWindow()
    return Offset(loc[0] + inWindow.x, loc[1] + inWindow.y)
}

@Composable
private fun FileListArea(
    state: FileExplorerState,
    bgColor: Color,
    surfaceBg: Color,
    textColor: Color,
    isDark: Boolean,
    context: android.content.Context,
    viewModel: LauncherViewModel?,
    onBackgroundContextMenu: (Offset) -> Unit
) {
    val localView = LocalView.current
    var listOrigin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .background(bgColor)
            .onGloballyPositioned { coords ->
                listOrigin = coords.trueScreenPosition(localView)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onBackgroundContextMenu(listOrigin + it) }
                )
            }
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            FluentIcons.Regular.FolderOpen,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.28f),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Loading", color = textColor.copy(alpha = 0.5f), fontSize = 12.5.sp)
                    }
                }
            }
            state.files.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = surfaceBg.copy(alpha = 0.55f)
                        ) {
                            Icon(
                                if (state.searchQuery.isNotBlank()) FluentIcons.Regular.Search else FluentIcons.Regular.FolderOpen,
                                null,
                                tint = textColor.copy(alpha = 0.25f),
                                modifier = Modifier.padding(14.dp).size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (state.searchQuery.isNotBlank()) "No matching files" else "This folder is empty",
                            color = textColor.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.searchQuery.isNotBlank()) {
                            Text(
                                "Try a different search",
                                color = textColor.copy(alpha = 0.35f),
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
            state.isGridView -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.files, key = { it.file.absolutePath }) { fileItem ->
                        GridFileItem(
                            item = fileItem,
                            isSelected = fileItem.file.absolutePath in state.selectedFiles,
                            textColor = textColor,
                            isDark = isDark,
                            onClick = {
                                val path = fileItem.file.absolutePath
                                val index = state.files.indexOfFirst { it.file.absolutePath == path }
                                // GridFileItem is an extracted composable and does not own
                                // the Explorer keyboard-modifier state. Keep pointer selection
                                // deterministic here; Ctrl/Shift range selection remains available
                                // through the Explorer keyboard navigation path.
                                state.selectedFiles = setOf(path)
                            },
                            onDoubleClick = {
                                if (fileItem.isDirectory) state.navigateTo(fileItem.file)
                                else openFileFromExplorer(fileItem, context, viewModel)
                            },
                            onLongPress = { offset -> state.contextMenuOffset = listOrigin + offset; state.contextMenuFile = fileItem }
                        )
                    }
                }
            }
            else -> {
                Column {
                    FileListHeader(surfaceBg = surfaceBg, textColor = textColor)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.files, key = { it.file.absolutePath }) { fileItem ->
                            ListFileItem(
                                item = fileItem,
                                isSelected = fileItem.file.absolutePath in state.selectedFiles,
                                textColor = textColor,
                                isDark = isDark,
                                onClick = {
                                    val path = fileItem.file.absolutePath
                                    state.selectedFiles = setOf(path)
                                },
                                onDoubleClick = {
                                    if (fileItem.isDirectory) state.navigateTo(fileItem.file)
                                    else openFileFromExplorer(fileItem, context, viewModel)
                                },
                                onLongPress = { offset -> state.contextMenuOffset = listOrigin + offset; state.contextMenuFile = fileItem }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListHeader(surfaceBg: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(surfaceBg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Name", color = textColor.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text("Date modified", color = textColor.copy(alpha = 0.58f), fontSize = 11.sp, modifier = Modifier.width(128.dp))
        Text("Type", color = textColor.copy(alpha = 0.58f), fontSize = 11.sp, modifier = Modifier.width(68.dp))
        Text("Size", color = textColor.copy(alpha = 0.58f), fontSize = 11.sp, modifier = Modifier.width(62.dp))
    }
}

// ────────────────────────────────────────────────────────
// All Dialogs — extracted composable
// ────────────────────────────────────────────────────────

@Composable
private fun FileExplorerDialogs(
    state: FileExplorerState,
    viewModel: LauncherViewModel?,
    isDark: Boolean,
    context: android.content.Context,
    clipboardActive: Boolean,
    onReload: () -> Unit
) {
    // Context Menu
    state.contextMenuFile?.let { fileItem ->
        FileContextMenu(
            fileItem = fileItem,
            offset = state.contextMenuOffset,
            isDark = isDark,
            onDismiss = { state.contextMenuFile = null },
            onOpen = {
                state.contextMenuFile = null
                openFileFromExplorer(fileItem, context, viewModel)
            },
            onCopy = {
                val targets = if (fileItem.file.absolutePath in state.selectedFiles && state.selectedFiles.size > 1)
                    state.selectedFiles.map { File(it) } else listOf(fileItem.file)
                viewModel?.setClipboard(targets, cut = false)
                state.contextMenuFile = null
            },
            onCut = {
                val targets = if (fileItem.file.absolutePath in state.selectedFiles && state.selectedFiles.size > 1)
                    state.selectedFiles.map { File(it) } else listOf(fileItem.file)
                viewModel?.setClipboard(targets, cut = true)
                state.contextMenuFile = null
            },
            onPaste = if (fileItem.isDirectory && clipboardActive) ({
                viewModel?.pasteClipboard(fileItem.file)
                state.contextMenuFile = null
            }) else null,
            onRename = { state.renameTarget = fileItem; state.showRenameDialog = true; state.contextMenuFile = null },
            onDelete = { state.showDeleteDialog = true },
            onCreateShortcut = {
                viewModel?.addDesktopShortcutFromFile(fileItem.file.absolutePath, fileItem.name)
                state.contextMenuFile = null
            },
            onProperties = {
                state.propertiesTarget = fileItem
                state.contextMenuFile = null
            }
        )
    }

    if (state.showBackgroundContextMenu) {
        ExplorerBackgroundContextMenu(
            offset = state.contextMenuOffset,
            isDark = isDark,
            hasPaste = clipboardActive,
            onDismiss = { state.showBackgroundContextMenu = false },
            onRefresh = { onReload() },
            onPaste = { viewModel?.pasteClipboard(state.currentDir) },
            onNewFolder = { state.showNewFolderDialog = true },
            onNewTextFile = {
                var index = 0
                var name: String
                do {
                    name = if (index == 0) "New Text Document.txt" else "New Text Document ($index).txt"
                    index++
                } while (File(state.currentDir, name).exists())
                runCatching { File(state.currentDir, name).createNewFile() }
                onReload()
            },
            onSelectAll = { state.selectedFiles = state.files.mapTo(linkedSetOf()) { it.file.absolutePath } },
            onToggleHidden = { state.showHidden = !state.showHidden }
        )
    }

    // Rename Dialog
    if (state.showRenameDialog && state.renameTarget != null) {
        RenameDialog(
            target = state.renameTarget!!,
            isDark = isDark,
            onConfirm = { newName ->
                val target = state.renameTarget?.file
                val cleanName = newName.trim()
                if (target == null) {
                    state.showRenameDialog = false
                } else if (cleanName.isBlank() || cleanName == "." || cleanName == ".." || cleanName.contains(File.separatorChar)) {
                    android.widget.Toast.makeText(context, "Invalid file name", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val dest = File(target.parentFile, cleanName)
                    if (dest.exists() && !dest.absolutePath.equals(target.absolutePath, ignoreCase = true)) {
                        android.widget.Toast.makeText(context, "A file with that name already exists", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (!target.renameTo(dest)) {
                        android.widget.Toast.makeText(context, "Rename failed", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        state.showRenameDialog = false
                        state.renameTarget = null
                        state.selectedFiles = state.selectedFiles.mapTo(linkedSetOf()) {
                            if (it == target.absolutePath) dest.absolutePath else it
                        }
                        onReload()
                    }
                }
            },
            onDismiss = { state.showRenameDialog = false }
        )
    }

    // Delete Dialog — deletes the right-clicked file (or the whole multi-selection, if
    // the right-clicked file is part of it), matching real Explorer's selection semantics.
    val deleteTargets: List<File> = if (state.showDeleteDialog) {
        val ctx = state.contextMenuFile
        if (ctx != null) {
            if (ctx.file.absolutePath in state.selectedFiles && state.selectedFiles.size > 1)
                state.selectedFiles.map { File(it) } else listOf(ctx.file)
        } else {
            state.selectedFiles.map { File(it) }
        }
    } else emptyList()

    if (state.showDeleteDialog && deleteTargets.isNotEmpty()) {
        DeleteDialog(
            targets = deleteTargets,
            isDark = isDark,
            onConfirm = {
                deleteTargets.forEach { viewModel?.deleteToRecycleBin(it.absolutePath) }
                val label = if (deleteTargets.size == 1) "Deleted \"${deleteTargets[0].name}\"" else "Deleted ${deleteTargets.size} items"
                viewModel?.showUndoAction(label) {
                    deleteTargets.forEach { viewModel.restoreFromRecycleBinByOriginalPath(it.absolutePath) }
                }
                state.showDeleteDialog = false
                state.contextMenuFile = null
                onReload()
            },
            onDismiss = { state.showDeleteDialog = false; state.contextMenuFile = null }
        )
    }

    // New Folder Dialog
    if (state.showNewFolderDialog) {
        NewFolderDialog(
            isDark = isDark,
            onConfirm = { folderName ->
                val cleanName = folderName.trim()
                if (cleanName.isBlank() || cleanName == "." || cleanName == ".." || cleanName.contains(File.separatorChar)) {
                    android.widget.Toast.makeText(context, "Invalid folder name", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val folder = File(state.currentDir, cleanName)
                    if (folder.exists()) {
                        android.widget.Toast.makeText(context, "A file or folder with that name already exists", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (!folder.mkdir()) {
                        android.widget.Toast.makeText(context, "Could not create folder", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        state.showNewFolderDialog = false
                        onReload()
                    }
                }
            },
            onDismiss = { state.showNewFolderDialog = false }
        )
    }

    state.propertiesTarget?.let { target ->
        FilePropertiesDialog(
            target = target,
            isDark = isDark,
            onDismiss = { state.propertiesTarget = null }
        )
    }

}

// ────────────────────────────────────────────────────────
// Individual Dialog Composables
// ────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(
    target: RealFileItem,
    isDark: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Real Windows Explorer pre-selects the base name (not the extension) when you hit
    // Rename, so typing immediately overwrites just the name. TextFieldValue lets us set
    // that initial selection instead of leaving the caret at the end like a plain String.
    val dotIndex = target.name.lastIndexOf('.')
    val baseNameEnd = if (!target.isDirectory && dotIndex > 0) dotIndex else target.name.length
    var newName by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = target.name,
                selection = androidx.compose.ui.text.TextRange(0, baseNameEnd)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Win11Dialog(
        isDark = isDark,
        title = "Rename",
        onDismissRequest = onDismiss,
        confirmLabel = "Rename",
        onConfirm = { onConfirm(newName.text) },
        onCancel = onDismiss
    ) { colors ->
        Win11TextField(
            value = newName,
            onValueChange = { newName = it },
            colors = colors,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun DeleteDialog(
    targets: List<File>,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberWin11DialogColors(isDark)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        shape = RoundedCornerShape(8.dp),
        title = { Text("Delete", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
        text = {
            Text(
                if (targets.size == 1) "Move '${targets[0].name}' to Recycle Bin?"
                else "Move ${targets.size} items to Recycle Bin?",
                color = colors.text.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = bluebirdColors.DangerRed)
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(4.dp)) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewFolderDialog(
    isDark: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Same "pre-selected so you can just start typing" behavior as Explorer's real
    // New Folder dialog — the whole default name is selected, not just cursor-at-end.
    var folderName by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = "New Folder",
                selection = androidx.compose.ui.text.TextRange(0, "New Folder".length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Win11Dialog(
        isDark = isDark,
        title = "New Folder",
        onDismissRequest = onDismiss,
        confirmLabel = "Create",
        onConfirm = { onConfirm(folderName.text) },
        onCancel = onDismiss
    ) { colors ->
        Win11TextField(
            value = folderName,
            onValueChange = { folderName = it },
            colors = colors,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

// ────────────────────────────────────────────────────────
// Shared Win11-style dialog chrome — rounded corners, accent-colored focus
// ring/buttons, and a consistent surface color instead of Material3's defaults.
// ────────────────────────────────────────────────────────

private data class Win11DialogColors(val bg: Color, val text: Color, val accent: Color, val fieldBg: Color, val border: Color)

@Composable
private fun rememberWin11DialogColors(isDark: Boolean): Win11DialogColors {
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val bg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFBFBFB)
    val fieldBg = if (isDark) Color(0xFF3A3A3A) else Color.White
    val border = textColor.copy(alpha = if (isDark) 0.16f else 0.18f)
    return Win11DialogColors(bg, textColor, bluebirdColors.AccentBlue, fieldBg, border)
}

@Composable
private fun Win11Dialog(
    isDark: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable (Win11DialogColors) -> Unit
) {
    val colors = rememberWin11DialogColors(isDark)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colors.bg,
        shape = RoundedCornerShape(8.dp),
        title = { Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
        text = { content(colors) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(4.dp)) { Text("Cancel") }
        }
    )
}

@Composable
private fun Win11TextField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    colors: Win11DialogColors,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = colors.accent,
            focusedContainerColor = colors.fieldBg,
            unfocusedContainerColor = colors.fieldBg
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FilePropertiesDialog(
    target: RealFileItem,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val file = target.file
    val type = when {
        target.isDirectory -> "Folder"
        target.extension.isBlank() -> "File"
        else -> "${target.extension.uppercase()} file"
    }
    val modified = remember(target.lastModified) {
        if (target.lastModified > 0L) formatDate(target.lastModified) else "Unknown"
    }
    val location = file.parentFile?.absolutePath ?: file.absolutePath

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(getFileIcon(target), null, tint = getFileIconColor(target), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(target.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PropertyRow("Location", location)
                PropertyRow("Size", if (target.isDirectory) "Folder" else formatFileSize(target.size))
                PropertyRow("Modified", modified)
                PropertyRow("Path", file.absolutePath)
                PropertyRow("Readable", if (file.canRead()) "Yes" else "No")
                PropertyRow("Writable", if (file.canWrite()) "Yes" else "No")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ────────────────────────────────────────────────────────
// File Opening Logic
// ────────────────────────────────────────────────────────

private fun openFileFromExplorer(
    item: RealFileItem,
    context: android.content.Context,
    viewModel: LauncherViewModel?
) {
    if (item.isDirectory) return

    if (item.file.extension.equals("exe", ignoreCase = true) && viewModel != null) {
        val descriptor = BluebirdExecutable.read(item.file)
        if (descriptor != null) {
            val root = runCatching { BluebirdExecutable.resolveSourceRoot(item.file, descriptor) }.getOrNull()
            val entry = runCatching { BluebirdExecutable.resolveEntry(item.file, descriptor) }.getOrNull()
            if (root != null && entry != null &&
                (entry.path == root.path || entry.path.startsWith(root.path + File.separator)) &&
                entry.isFile
            ) {
                val iconFile = BluebirdExecutable.resolveIcon(item.file, descriptor)
                viewModel.openWindow(
                    screen = LauncherScreen.WEB_APP_VIEWER,
                    extras = mapOf(
                        "bpkAppId" to descriptor.appId,
                        "bpkAppName" to descriptor.name,
                        "bpkAppLocalDir" to root.absolutePath,
                        "bpkAppEntry" to entry.relativeTo(root).path
                    ),
                    customIconPath = iconFile.takeIf { it.isFile }?.absolutePath
                )
                return
            }
        }
    }

    if (item.file.extension.equals("bpk", ignoreCase = true) && viewModel != null) {
        val packageIcon = BpkPackageIcon.decode(item.file)
        val iconCache = File(context.cacheDir, "bluebird/bpk-icons")
        val packageIconFile = BpkPackageIcon.cache(item.file, iconCache)
        viewModel.openWindow(
            screen = LauncherScreen.PROGRAM_MANAGER,
            extras = buildMap {
                put("bpkPath", item.file.absolutePath)
                put("windowTitle", "Install ${item.file.nameWithoutExtension}")
            },
            customIconPath = packageIconFile?.absolutePath
        )
        return
    }

    if (viewModel?.openFileInternally(context, item.file.absolutePath) == true) return

    // Unsupported formats still get normal Android handling.
    viewModel?.openFileWithSystem(context, item.file.absolutePath) ?: run {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(item.file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "No app can open ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

// ────────────────────────────────────────────────────────
// UI Composables (unchanged from original)
// ────────────────────────────────────────────────────────

@Composable
private fun CommandBar(
    onBack: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    pathParts: List<Pair<String, File>>,
    onNavigate: (File) -> Unit,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    textColor: Color,
    surfaceBg: Color,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onBack,
                enabled = true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(FluentIcons.Regular.ArrowLeft, "Back", tint = textColor, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onUp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(FluentIcons.Regular.ArrowUp, "Up", tint = textColor, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(FluentIcons.Regular.ArrowClockwise, "Refresh", tint = textColor, modifier = Modifier.size(18.dp))
            }

            BreadcrumbBar(
                parts = pathParts,
                onNavigate = onNavigate,
                textColor = textColor,
                surfaceBg = surfaceBg,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            )

            Surface(
                modifier = Modifier.width(220.dp).height(36.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (isDark) Color(0xFF303030) else Color(0xFFE7EAED),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        FluentIcons.Regular.Search,
                        contentDescription = "Search",
                        tint = textColor.copy(alpha = 0.48f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    // FIX: this Box had no contentAlignment, so both the placeholder and the
                    // BasicTextField defaulted to top-start inside a box sized to its tallest
                    // child — that's what made the cursor/caret appear to float above the
                    // visible text line instead of sitting centered on it.
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search this folder",
                                color = textColor.copy(alpha = 0.42f),
                                fontSize = 12.5.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(color = textColor, fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(bluebirdColors.AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                FluentIcons.Regular.DiamondDismiss,
                                contentDescription = "Clear search",
                                tint = textColor.copy(alpha = 0.55f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isDark) Color(0xFF303030) else Color(0xFFE7EAED)
            ) {
                IconButton(onClick = onToggleView, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (isGridView) FluentIcons.Regular.List else FluentIcons.Regular.Grid,
                        if (isGridView) "List view" else "Grid view",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    parts: List<Pair<String, File>>,
    onNavigate: (File) -> Unit,
    textColor: Color,
    surfaceBg: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = surfaceBg.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            parts.forEachIndexed { index, (label, dir) ->
                if (index > 0) {
                    Icon(
                        FluentIcons.Regular.ChevronRight,
                        null,
                        tint = textColor.copy(alpha = 0.28f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    label,
                    color = if (index == parts.lastIndex) textColor else bluebirdColors.AccentBlueLight,
                    fontSize = 12.5.sp,
                    fontWeight = if (index == parts.lastIndex) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable { onNavigate(dir) }
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Ribbon(
    onNewFolder: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    sortBy: String,
    sortAscending: Boolean,
    onSortChange: (String, Boolean) -> Unit,
    showHidden: Boolean,
    onToggleHidden: () -> Unit,
    clipboardActive: Boolean,
    textColor: Color,
    surfaceBg: Color,
    isDark: Boolean,
    itemCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE9ECEF),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExplorerAction("New folder", FluentIcons.Regular.FolderAdd, onNewFolder, textColor)
            ExplorerAction("Cut", FluentIcons.Regular.Cut, onCut, textColor, enabled = true)
            ExplorerAction("Copy", FluentIcons.Regular.Copy, onCopy, textColor, enabled = true)
            ExplorerAction(
                "Paste",
                FluentIcons.Regular.ClipboardPaste,
                onPaste,
                if (clipboardActive) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.28f),
                enabled = clipboardActive
            )
            ExplorerAction("Delete", FluentIcons.Regular.Delete, onDelete, textColor, enabled = true)

            Spacer(Modifier.width(4.dp))
            VerticalDivider(modifier = Modifier.height(22.dp), color = textColor.copy(alpha = 0.10f))
            Spacer(Modifier.width(5.dp))

            Text(
                "Sort",
                color = textColor.copy(alpha = 0.50f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            listOf("name", "date", "size", "type").forEach { sort ->
                TextButton(
                    onClick = {
                        if (sortBy == sort) onSortChange(sort, !sortAscending)
                        else onSortChange(sort, true)
                    },
                    contentPadding = PaddingValues(horizontal = 5.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        sort.replaceFirstChar { it.uppercase() },
                        color = if (sortBy == sort) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                    if (sortBy == sort) {
                        Icon(
                            if (sortAscending) FluentIcons.Regular.ChevronUp else FluentIcons.Regular.ChevronDown,
                            null,
                            tint = bluebirdColors.AccentBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            ExplorerAction(
                if (showHidden) "Hide hidden files" else "Show hidden files",
                if (showHidden) FluentIcons.Regular.EyeOff else FluentIcons.Regular.Eye,
                onToggleHidden,
                textColor
            )
            Text(
                "$itemCount items",
                color = textColor.copy(alpha = 0.42f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ExplorerAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(30.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun NavigationPane(
    quickAccess: List<Pair<String, File>>,
    currentDir: File,
    onNavigate: (File) -> Unit,
    textColor: Color,
    navBg: Color,
    surfaceBg: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .width(196.dp)
            .fillMaxHeight()
            .background(navBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 7.dp, vertical = 9.dp)
    ) {
        Text(
            "Quick access",
            color = textColor.copy(alpha = 0.48f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
        quickAccess.forEach { (label, dir) ->
            NavItem(
                label = label,
                icon = when (label) {
                    "Downloads" -> FluentIcons.Regular.ArrowDownload
                    "Pictures" -> FluentIcons.Regular.Image
                    "Music" -> FluentIcons.Regular.MusicNote2
                    "Movies" -> FluentIcons.Regular.VideoClip
                    "Desktop" -> FluentIcons.Regular.Desktop
                    "DCIM" -> FluentIcons.Regular.Camera
                    "Documents" -> FluentIcons.Regular.Document
                    else -> FluentIcons.Regular.HardDrive
                },
                isSelected = currentDir == dir,
                textColor = textColor,
                onClick = { onNavigate(dir) }
            )
        }

        Spacer(Modifier.height(7.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 5.dp),
            color = textColor.copy(alpha = 0.08f)
        )
        Spacer(Modifier.height(8.dp))

        val stat = remember { StatFs(Environment.getExternalStorageDirectory().path) }
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = surfaceBg.copy(alpha = 0.58f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        FluentIcons.Regular.HardDrive,
                        null,
                        tint = textColor.copy(alpha = 0.60f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Internal storage",
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = bluebirdColors.AccentBlue,
                    trackColor = textColor.copy(alpha = 0.10f)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${formatFileSize(freeBytes)} free of ${formatFileSize(totalBytes)}",
                    color = textColor.copy(alpha = 0.46f),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) bluebirdColors.AccentBlue.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            icon,
            null,
            tint = if (isSelected) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.66f),
            modifier = Modifier.size(17.dp)
        )
        Text(
            label,
            color = if (isSelected) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.88f),
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBar(textColor: Color, itemCount: Int, selectedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(26.dp)
            .background(textColor.copy(alpha = 0.045f))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$itemCount items", color = textColor.copy(alpha = 0.45f), fontSize = 11.sp)
            if (selectedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bluebirdColors.AccentBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        "$selectedCount selected",
                        color = bluebirdColors.AccentBlue,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text("Bluebird Explorer", color = textColor.copy(alpha = 0.25f), fontSize = 10.5.sp)
    }
}

// ────────────────────────────────────────────────────────
// List & Grid Items
// ────────────────────────────────────────────────────────


// Ask Android's media provider for small video thumbnails. Bluebird never persists
// video frames or copies video files into app storage.
private suspend fun loadExplorerVideoThumbnail(context: android.content.Context, file: File): Bitmap? {
    if (!file.isFile || file.length() <= 0L || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return try {
        val uri = context.contentResolver.query(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(android.provider.MediaStore.Video.Media._ID),
            "${android.provider.MediaStore.Video.Media.DATA} = ?",
            arrayOf(file.absolutePath),
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    c.getLong(0)
                )
            } else null
        } ?: return null
        context.contentResolver.loadThumbnail(uri, android.util.Size(160, 90), null)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun FileExplorerThumbnail(
    item: RealFileItem,
    modifier: Modifier,
    iconSize: Dp
) {
    val context = LocalContext.current
    val extension = item.extension
    val isImage = !item.isDirectory && extension in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    val isVideo = !item.isDirectory && extension in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp")
    val videoThumbnail by produceState<Bitmap?>(
        initialValue = null,
        item.file.absolutePath,
        item.lastModified,
        item.size,
        isVideo
    ) {
        if (!isVideo) return@produceState
        value = withContext(Dispatchers.IO) { loadExplorerVideoThumbnail(context, item.file) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isImage -> {
                // Coil handles decoding/downsampling off the UI thread. This is a
                // thumbnail only; Explorer never renders the file itself.
                coil.compose.AsyncImage(
                    model = item.file,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            isVideo && videoThumbnail != null -> {
                androidx.compose.foundation.Image(
                    bitmap = videoThumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(iconSize.coerceAtMost(28.dp))
                ) {
                    Icon(FluentIcons.Filled.Play, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
            item.iconBitmap != null -> {
                Image(
                    bitmap = item.iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(6.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            else -> {
                Icon(getFileIcon(item), null, tint = getFileIconColor(item), modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Composable
private fun ListFileItem(
    item: RealFileItem,
    isSelected: Boolean,
    textColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongPress: (Offset) -> Unit
) {
    val rowColor = if (isSelected) bluebirdColors.AccentBlue.copy(alpha = if (isDark) 0.18f else 0.12f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(rowColor)
            .pointerInput(item.file.absolutePath) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() },
                    onLongPress = { offset -> onLongPress(offset) }
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileExplorerThumbnail(
            item = item,
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)),
            iconSize = 19.dp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            item.name,
            color = textColor,
            fontSize = 13.5.sp,
            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatDate(item.lastModified),
            color = textColor.copy(alpha = 0.48f),
            fontSize = 11.sp,
            modifier = Modifier.width(128.dp),
            maxLines = 1
        )
        Text(
            if (item.isDirectory) "Folder" else item.extension.uppercase().ifEmpty { "File" },
            color = textColor.copy(alpha = 0.48f),
            fontSize = 11.sp,
            modifier = Modifier.width(68.dp),
            maxLines = 1
        )
        Text(
            if (item.isDirectory) "" else formatFileSize(item.size),
            color = textColor.copy(alpha = 0.48f),
            fontSize = 11.sp,
            modifier = Modifier.width(62.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun GridFileItem(
    item: RealFileItem,
    isSelected: Boolean,
    textColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongPress: (Offset) -> Unit
) {
    val cardColor = if (isSelected) bluebirdColors.AccentBlue.copy(alpha = if (isDark) 0.20f else 0.13f) else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardColor)
            .pointerInput(item.file.absolutePath) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() },
                    onLongPress = { offset -> onLongPress(offset) }
                )
            }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) bluebirdColors.AccentBlue.copy(alpha = 0.10f) else textColor.copy(alpha = 0.045f)
        ) {
            FileExplorerThumbnail(
                item = item,
                modifier = Modifier.padding(7.dp).size(56.dp),
                iconSize = 33.dp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.name,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
        if (!item.isDirectory) {
            Text(
                formatFileSize(item.size),
                color = textColor.copy(alpha = 0.38f),
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
    }
}

// ────────────────────────────────────────────────────────
// Context Menu
// ────────────────────────────────────────────────────────

@Composable
private fun FileContextMenu(
    fileItem: RealFileItem,
    offset: Offset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: (() -> Unit)? = null,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit,
    onProperties: () -> Unit
) {
    ExplorerContextPopup(
        offset = offset,
        isDark = isDark,
        onDismiss = onDismiss
    ) {
        if (!fileItem.isDirectory) ContextMenuItem("Open", FluentIcons.Regular.Open, onOpen)
        if (fileItem.isDirectory && onPaste != null) ContextMenuItem("Paste", FluentIcons.Regular.ClipboardPaste, onPaste)
        ContextMenuItem("Cut", FluentIcons.Regular.Cut, onCut)
        ContextMenuItem("Copy", FluentIcons.Regular.Copy, onCopy)
        ContextMenuItem("Rename", FluentIcons.Regular.Rename, onRename)
        ContextMenuItem("Create shortcut", FluentIcons.Regular.Link, onCreateShortcut)
        ContextMenuItem("Delete", FluentIcons.Regular.Delete, onDelete, tint = bluebirdColors.DangerRed)
        Divider(Modifier.padding(vertical = 2.dp))
        ContextMenuItem("Properties", FluentIcons.Regular.Info, onProperties)
    }
}

@Composable
private fun ExplorerContextPopup(
    offset: Offset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val position = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val margin = 8
                return IntOffset(
                    position.x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                    position.y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
                )
            }
        },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        val bg = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
        val border = if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5)
        Surface(
            modifier = Modifier.width(204.dp),
            shape = RoundedCornerShape(6.dp),
            color = bg,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, border)
        ) {
            Column(Modifier.padding(vertical = 3.dp), content = content)
        }
    }
}

@Composable
private fun ExplorerBackgroundContextMenu(
    offset: Offset,
    isDark: Boolean,
    hasPaste: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onPaste: () -> Unit,
    onNewFolder: () -> Unit,
    onNewTextFile: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleHidden: () -> Unit
) {
    ExplorerContextPopup(offset, isDark, onDismiss) {
        ContextMenuItem("Refresh", FluentIcons.Regular.ArrowClockwise, { onRefresh(); onDismiss() })
        if (hasPaste) ContextMenuItem("Paste", FluentIcons.Regular.ClipboardPaste, { onPaste(); onDismiss() })
        Divider(Modifier.padding(vertical = 2.dp))
        ContextMenuItem("New folder", FluentIcons.Regular.FolderAdd, { onNewFolder(); onDismiss() })
        ContextMenuItem("New text document", FluentIcons.Regular.Document, { onNewTextFile(); onDismiss() })
        ContextMenuItem("Select all", FluentIcons.Regular.SelectAllOn, { onSelectAll(); onDismiss() })
        ContextMenuItem("Show hidden files", FluentIcons.Regular.Eye, { onToggleHidden(); onDismiss() })
    }
}

@Composable
private fun ContextMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Text(label, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
