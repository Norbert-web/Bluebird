package io.github.norbertweb.bluebird.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import io.github.norbertweb.bluebird.LauncherViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.text.selection.SelectionContainer
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
    val extension: String = file.extension.lowercase()
)



fun getFileIcon(item: RealFileItem): ImageVector = when {
    item.isDirectory -> Icons.Default.Folder
    item.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
    item.extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> Icons.Default.OndemandVideo
    item.extension in listOf("mp3", "wav", "ogg", "flac", "aac", "m4a") -> Icons.Default.AudioFile
    item.extension in listOf("pdf") -> Icons.Default.PictureAsPdf
    item.extension in listOf("txt", "log", "md", "xml", "json", "csv") -> Icons.Default.Description
    item.extension in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.Archive
    item.extension in listOf("apk") -> Icons.Default.Android
    item.extension in listOf("doc", "docx") -> Icons.Default.Article
    item.extension in listOf("xls", "xlsx") -> Icons.Default.TableChart
    item.extension in listOf("ppt", "pptx") -> Icons.Default.Slideshow
    item.extension in listOf("html", "htm") -> Icons.Default.Code
    item.extension == "webapp" -> Icons.Default.Public
    item.extension == "io.github.norbertweb.io.github.norbertweb.bluebird" -> Icons.Default.Apps
    else -> Icons.Default.InsertDriveFile
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
    var files by mutableStateOf(listOf<RealFileItem>())
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
    var showRenameDialog by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)
    var renameTarget by mutableStateOf<RealFileItem?>(null)
    var showNewFolderDialog by mutableStateOf(false)
    // Clipboard now lives in the ViewModel (shared with Desktop) — see vmUiState.clipboardFiles
    var previewFile by mutableStateOf<RealFileItem?>(null)

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
    return remember { FileExplorerState(initialDir) }
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

    val startDir = remember(startPath) {
        val f = startPath?.let { File(it) }
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
                Icons.Default.FolderOff,
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
                fontSize = 13.sp,
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
    val navBg = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF5F5F5)

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
    val loadFiles: (File) -> Unit = { dir ->
        scope.launch {
            state.isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    if (!dir.exists() || !dir.canRead()) {
                        state.files = emptyList()
                        return@withContext
                    }
                    val rawFiles = dir.listFiles()?.toList() ?: emptyList()
                    val filtered = rawFiles
                        .filter { state.showHidden || !it.name.startsWith(".") }
                        .filter { state.searchQuery.isEmpty() || it.name.contains(state.searchQuery, ignoreCase = true) }
                    val sorted = when (state.sortBy) {
                        "name" -> filtered.sortedBy { it.name.lowercase() }
                        "size" -> filtered.sortedBy { it.length() }
                        "date" -> filtered.sortedBy { it.lastModified() }
                        "type" -> filtered.sortedBy { it.extension }
                        else -> filtered.sortedBy { it.name.lowercase() }
                    }
                    val dirs = sorted.filter { it.isDirectory }
                    val fils = sorted.filter { it.isFile }
                    val ordered = if (state.sortAscending) dirs + fils else (fils + dirs).reversed()
                    state.files = ordered.map { RealFileItem(it) }
                } catch (e: SecurityException) {
                    state.files = emptyList()
                } finally {
                    withContext(Dispatchers.Main) { state.isLoading = false }
                }
            }
        }
    }

    LaunchedEffect(state.currentDir, state.searchQuery, state.showHidden, state.sortBy, state.sortAscending) {
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
    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {

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
                viewModel = viewModel
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
        onReload = { loadFiles(state.currentDir) }
    )
}

// ────────────────────────────────────────────────────────
// File List Area — extracted composable
// ────────────────────────────────────────────────────────

@Composable
private fun FileListArea(
    state: FileExplorerState,
    bgColor: Color,
    surfaceBg: Color,
    textColor: Color,
    isDark: Boolean,
    context: android.content.Context,
    viewModel: LauncherViewModel?
) {
    Box(modifier = Modifier.fillMaxHeight().background(bgColor)) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = bluebirdColors.AccentBlue)
                }
            }
            state.files.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                        Text("This folder is empty", color = textColor.copy(alpha = 0.4f), fontSize = 13.sp)
                    }
                }
            }
            state.isGridView -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(90.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.files, key = { it.file.absolutePath }) { fileItem ->
                        GridFileItem(
                            item = fileItem,
                            isSelected = fileItem.file.absolutePath in state.selectedFiles,
                            textColor = textColor,
                            isDark = isDark,
                            onClick = {
                                if (fileItem.isDirectory) state.navigateTo(fileItem.file)
                                else previewOrOpen(fileItem, context, viewModel) { state.previewFile = it }
                            },
                            onLongPress = { state.contextMenuFile = fileItem },
                            onSelect = {
                                val path = fileItem.file.absolutePath
                                state.selectedFiles = if (path in state.selectedFiles)
                                    state.selectedFiles - path else state.selectedFiles + path
                            }
                        )
                    }
                }
            }
            else -> {
                Column {
                    FileListHeader(surfaceBg = surfaceBg, textColor = textColor)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.files, key = { it.file.absolutePath }) { fileItem ->
                            ListFileItem(
                                item = fileItem,
                                isSelected = fileItem.file.absolutePath in state.selectedFiles,
                                textColor = textColor,
                                isDark = isDark,
                                onClick = {
                                    if (fileItem.isDirectory) state.navigateTo(fileItem.file)
                                    else previewOrOpen(fileItem, context, viewModel) { state.previewFile = it }
                                },
                                onLongPress = { state.contextMenuFile = fileItem },
                                onSelect = {
                                    val path = fileItem.file.absolutePath
                                    state.selectedFiles = if (path in state.selectedFiles)
                                        state.selectedFiles - path else state.selectedFiles + path
                                }
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
        modifier = Modifier.fillMaxWidth().height(26.dp)
            .background(surfaceBg).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Name", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("Date modified", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(130.dp))
        Text("Type", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Text("Size", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(60.dp))
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
    onReload: () -> Unit
) {
    // Context Menu
    state.contextMenuFile?.let { fileItem ->
        FileContextMenu(
            fileItem = fileItem,
            isDark = isDark,
            onDismiss = { state.contextMenuFile = null },
            onOpen = {
                state.contextMenuFile = null
                previewOrOpen(fileItem, context, viewModel) { state.previewFile = it }
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
            onRename = { state.renameTarget = fileItem; state.showRenameDialog = true; state.contextMenuFile = null },
            onDelete = { state.showDeleteDialog = true },
            onCreateShortcut = {
                viewModel?.addDesktopShortcutFromFile(fileItem.file.absolutePath, fileItem.name)
                state.contextMenuFile = null
            },
            onPreview = { state.contextMenuFile = null; state.previewFile = fileItem },
            onProperties = { state.contextMenuFile = null }
        )
    }

    // Rename Dialog
    if (state.showRenameDialog && state.renameTarget != null) {
        RenameDialog(
            target = state.renameTarget!!,
            onConfirm = { newName ->
                val dest = File(state.renameTarget!!.file.parent, newName)
                state.renameTarget!!.file.renameTo(dest)
                state.showRenameDialog = false
                state.renameTarget = null
                onReload()
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
            onConfirm = { folderName ->
                File(state.currentDir, folderName).mkdir()
                state.showNewFolderDialog = false
                onReload()
            },
            onDismiss = { state.showNewFolderDialog = false }
        )
    }

    // Preview
    state.previewFile?.let { file ->
        FilePreviewDialog(
            file = file,
            isDark = isDark,
            onDismiss = { state.previewFile = null }
        )
    }
}

// ────────────────────────────────────────────────────────
// Individual Dialog Composables
// ────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(
    target: RealFileItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(target.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteDialog(
    targets: List<File>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete") },
        text = {
            Text(
                if (targets.size == 1) "Move '${targets[0].name}' to Recycle Bin?"
                else "Move ${targets.size} items to Recycle Bin?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = bluebirdColors.DangerRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("New Folder") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(folderName) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ────────────────────────────────────────────────────────
// File Opening Logic
// ────────────────────────────────────────────────────────

private fun previewOrOpen(
    item: RealFileItem,
    context: android.content.Context,
    viewModel: LauncherViewModel?,
    onPreview: (RealFileItem) -> Unit
) {
    if (item.isDirectory) return
    val previewExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "txt", "log", "md", "xml", "json", "csv",
        "html", "htm"
    )
    if (item.extension in previewExtensions) {
        onPreview(item)
        return
    }
    // Shortcut files (native app shortcuts + installed web apps) open internally through
    // the same shared path Desktop uses, instead of falling through to the system's
    // "Open with" chooser — this is what makes them behave like real apps from File Explorer.
    if (viewModel != null && item.extension.lowercase() in setOf("io.github.norbertweb.io.github.norbertweb.bluebird", "webapp")) {
        val info = io.github.norbertweb.bluebird.ui.components.loadDesktopFileInfo(item.file, context)
        if (info != null) {
            io.github.norbertweb.bluebird.ui.components.openDesktopItem(info, context, viewModel)
            return
        }
    }
    viewModel?.openFileWithSystem(context, item.file.absolutePath) ?: run {
        try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", item.file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(item.file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // Graceful fallback
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
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(surfaceBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowBack, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onUp, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowUpward, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Refresh, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        BreadcrumbBar(
            parts = pathParts,
            onNavigate = onNavigate,
            textColor = textColor,
            surfaceBg = surfaceBg,
            modifier = Modifier.weight(1f).height(28.dp)
        )
        Box(
            modifier = Modifier.width(160.dp).height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0E0E0))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchQuery.isEmpty()) {
                Text("Search", color = textColor.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                textStyle = TextStyle(color = textColor, fontSize = 12.sp),
                cursorBrush = SolidColor(bluebirdColors.AccentBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(onClick = onToggleView, modifier = Modifier.size(28.dp)) {
            Icon(
                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                null, tint = textColor, modifier = Modifier.size(16.dp)
            )
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
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(surfaceBg.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { index, (label, dir) ->
            if (index > 0) {
                Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
            }
            Text(
                label,
                color = if (index == parts.size - 1) textColor else bluebirdColors.AccentBlueLight,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { onNavigate(dir) }
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp)
            .background(if (isDark) Color(0xFF2D2D2D) else Color(0xFFE8E8E8))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onNewFolder, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.CreateNewFolder, null, tint = textColor, modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = onCut, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.ContentCut, null, tint = textColor, modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.ContentCopy, null, tint = textColor, modifier = Modifier.size(14.dp))
        }
        if (clipboardActive) {
            IconButton(onClick = onPaste, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentPaste, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(14.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = textColor, modifier = Modifier.size(14.dp))
        }
        Divider(Modifier.width(1.dp).height(20.dp).background(textColor.copy(alpha = 0.1f)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sort:", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
            listOf("name", "date", "size", "type").forEach { sort ->
                TextButton(
                    onClick = {
                        if (sortBy == sort) onSortChange(sort, !sortAscending)
                        else onSortChange(sort, true)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        sort.replaceFirstChar { it.uppercase() },
                        color = if (sortBy == sort) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    if (sortBy == sort) {
                        Icon(
                            if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleHidden, modifier = Modifier.size(24.dp)) {
            Icon(
                if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                null, tint = textColor, modifier = Modifier.size(14.dp)
            )
        }
        Text("$itemCount items", color = textColor.copy(alpha = 0.4f), fontSize = 11.sp)
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
        modifier = Modifier.width(200.dp).fillMaxHeight()
            .background(navBg)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Quick Access",
            color = textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        quickAccess.forEach { (label, dir) ->
            NavItem(
                label = label,
                icon = when (label) {
                    "Downloads" -> Icons.Default.CloudDownload
                    "Pictures" -> Icons.Default.Image
                    "Music" -> Icons.Default.MusicNote
                    "Movies" -> Icons.Default.Movie
                    "Desktop" -> Icons.Default.DesktopWindows
                    "DCIM" -> Icons.Default.PhotoCamera
                    "Documents" -> Icons.Default.Description
                    else -> Icons.Default.Storage
                },
                isSelected = currentDir == dir,
                textColor = textColor,
                onClick = { onNavigate(dir) }
            )
        }

        Divider(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = textColor.copy(alpha = 0.1f))

        val stat = remember { StatFs(Environment.getExternalStorageDirectory().path) }
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("Internal Storage (C:)", color = textColor.copy(alpha = 0.5f), fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = bluebirdColors.AccentBlue,
                trackColor = textColor.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatFileSize(usedBytes)} / ${formatFileSize(totalBytes)}",
                color = textColor.copy(alpha = 0.5f), fontSize = 10.sp
            )
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
        modifier = Modifier.fillMaxWidth()
            .background(if (isSelected) bluebirdColors.AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) bluebirdColors.AccentBlue else textColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Text(label, color = if (isSelected) bluebirdColors.AccentBlue else textColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusBar(textColor: Color, itemCount: Int, selectedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp)
            .background(textColor.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$itemCount items", color = textColor.copy(alpha = 0.4f), fontSize = 10.sp)
        if (selectedCount > 0) {
            Text("$selectedCount selected", color = bluebirdColors.AccentBlue, fontSize = 10.sp)
        }
    }
}

// ────────────────────────────────────────────────────────
// List & Grid Items
// ────────────────────────────────────────────────────────

@Composable
private fun ListFileItem(
    item: RealFileItem,
    isSelected: Boolean,
    textColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isSelected) bluebirdColors.AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(getFileIcon(item), null, tint = getFileIconColor(item), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(item.name, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatDate(item.lastModified), color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(130.dp))
        Text(if (item.isDirectory) "Folder" else item.extension.uppercase().ifEmpty { "File" }, color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Text(if (item.isDirectory) "" else formatFileSize(item.size), color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(60.dp))
    }
}

@Composable
private fun GridFileItem(
    item: RealFileItem,
    isSelected: Boolean,
    textColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) bluebirdColors.AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .padding(8.dp)
    ) {
        Icon(getFileIcon(item), null, tint = getFileIconColor(item), modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(4.dp))
        Text(item.name, color = textColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (!item.isDirectory) {
            Text(formatFileSize(item.size), color = textColor.copy(alpha = 0.4f), fontSize = 10.sp)
        }
    }
}

// ────────────────────────────────────────────────────────
// Context Menu
// ────────────────────────────────────────────────────────

@Composable
private fun FileContextMenu(
    fileItem: RealFileItem,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit,
    onPreview: () -> Unit,
    onProperties: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(fileItem.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                ContextMenuItem("Open", Icons.Default.OpenInNew, onOpen)
                ContextMenuItem("Preview", Icons.Default.Visibility, onPreview)
                Divider(Modifier.padding(vertical = 4.dp))
                ContextMenuItem("Cut", Icons.Default.ContentCut, onCut)
                ContextMenuItem("Copy", Icons.Default.ContentCopy, onCopy)
                ContextMenuItem("Rename", Icons.Default.DriveFileRenameOutline, onRename)
                ContextMenuItem("Create shortcut", Icons.Default.Link, onCreateShortcut)
                ContextMenuItem("Delete", Icons.Default.Delete, onDelete, tint = bluebirdColors.DangerRed)
                Divider(Modifier.padding(vertical = 4.dp))
                ContextMenuItem("Properties", Icons.Default.Info, onProperties)
            }
        },
        confirmButton = {}
    )
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
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 13.sp)
    }
}

// ────────────────────────────────────────────────────────
// File Preview Dialog
// ────────────────────────────────────────────────────────

@Composable
private fun FilePreviewDialog(
    file: RealFileItem,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name, color = textColor) },
        text = {
            when {
                file.extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> {
                    AsyncImage(
                        model = Uri.fromFile(file.file),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    )
                }
                file.extension in listOf("txt", "log", "md", "xml", "json", "csv") -> {
                    val content = remember {
                        try { file.file.readText() } catch (e: Exception) { "Could not read file." }
                    }
                    SelectionContainer {
                        Text(
                            text = content,
                            color = textColor,
                            fontSize = 12.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
                        )
                    }
                }
                file.extension in listOf("html", "htm") -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                webViewClient = WebViewClient()
                                loadUrl(Uri.fromFile(file.file).toString())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(400.dp)
                    )
                }
                else -> {
                    Text("Preview not available", color = textColor.copy(alpha = 0.5f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = bg
    )
}
