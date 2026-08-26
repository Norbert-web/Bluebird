package io.github.norbertweb.bluebird.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.RecycleBinItem
import io.github.norbertweb.bluebird.ui.theme.Win11Colors
import java.io.File

// Helper functions (make sure these exist in your project, otherwise include them here)

private fun getFileIconForName(name: String): ImageVector {
    val ext = File(name).extension.lowercase()
    return when {
        ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
        ext in listOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Default.OndemandVideo
        ext in listOf("mp3", "wav", "ogg", "flac", "aac") -> Icons.Default.AudioFile
        ext == "pdf" -> Icons.Default.PictureAsPdf
        ext in listOf("apk") -> Icons.Default.Android
        ext in listOf("zip", "rar", "7z") -> Icons.Default.Archive
        ext in listOf("txt", "log", "md") -> Icons.Default.Description
        ext in listOf("doc", "docx") -> Icons.Default.Article
        ext in listOf("xls", "xlsx") -> Icons.Default.TableChart
        ext in listOf("ppt", "pptx") -> Icons.Default.Slideshow
        ext in listOf("html", "htm") -> Icons.Default.Code
        name.contains(".") -> Icons.Default.InsertDriveFile
        else -> Icons.Default.Folder
    }
}

private fun getFileIconColorForName(name: String): Color {
    val ext = File(name).extension.lowercase()
    return when {
        ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Color(0xFF4CAF50)
        ext in listOf("mp4", "mkv", "avi", "mov") -> Color(0xFF9C27B0)
        ext in listOf("mp3", "wav", "ogg", "flac", "aac") -> Color(0xFFFF5722)
        ext == "pdf" -> Color(0xFFF44336)
        ext == "apk" -> Color(0xFF4CAF50)
        ext in listOf("zip", "rar", "7z") -> Color(0xFF795548)
        ext in listOf("txt", "log", "md") -> Color(0xFF2196F3)
        name.contains(".") -> Color(0xFF9E9E9E)
        else -> Color(0xFFFFC107)
    }
}

@Composable
fun RecycleBinScreen(isDark: Boolean, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.recycleBinItems

    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFFAFAFA)
    val surfaceBg = if (isDark) Color(0xFF252525) else Color(0xFFEEEEEE)
    val accent = Win11Colors.AccentBlue

    // Permission check – not always required, but good practice for accessing real filesystem
    var hasStoragePerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasStoragePerm = granted }

    // Ensure we have permission when screen opens (if needed by future operations)
    LaunchedEffect(Unit) {
        if (!hasStoragePerm) {
            permLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }

    // Apply sorting
    val sortedItems = remember(items, sortMode, sortAscending) {
        val sorted = when (sortMode) {
            "name" -> items.sortedBy { it.name.lowercase() }
            "date" -> items.sortedBy { it.deletedAt }
            "size" -> items.sortedBy { it.sizeBytes }
            "type" -> items.sortedBy { File(it.name).extension }
            else -> items
        }
        if (!sortAscending) sorted.reversed() else sorted.toList()
    }

    // Clear selection when items change
    LaunchedEffect(items) {
        selectedIds = selectedIds.filter { it in items.map { item -> item.id } }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // ── Command bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Recycle Bin",
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Sort controls
            Box {
                var showSortMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = "Sort", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    listOf("name", "date", "size", "type").forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.replaceFirstChar { it.uppercase() }, color = textColor, fontSize = 13.sp) },
                            onClick = {
                                if (sortMode == sort) sortAscending = !sortAscending
                                else sortMode = sort
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortMode == sort) {
                                    Icon(
                                        if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Restore all button
            if (items.isNotEmpty()) {
                OutlinedButton(
                    onClick = { items.forEach { viewModel.restoreFromRecycleBin(it.id) } },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    border = BorderStroke(1.dp, accent),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.RestoreFromTrash, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore all", fontSize = 12.sp)
                }
            }

            // Empty bin button
            if (items.isNotEmpty()) {
                Button(
                    onClick = { showEmptyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Win11Colors.DangerRed),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Empty Bin", fontSize = 12.sp)
                }
            }
        }

        // ── Contextual selection bar ──
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${selectedIds.size} selected",
                    color = textColor,
                    fontSize = 12.sp
                )
                TextButton(onClick = {
                    selectedIds.forEach { viewModel.restoreFromRecycleBin(it) }
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Default.RestoreFromTrash, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp)
                }
                TextButton(onClick = {
                    selectedIds.forEach { viewModel.permanentlyDelete(it) }
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Default.DeleteForever, null, tint = Win11Colors.DangerRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete permanently", color = Win11Colors.DangerRed, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { selectedIds = emptySet() }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f))
                }
            }
        }

        // ── Column headers (subtle) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF282828) else Color(0xFFE8E8E8))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Name", color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Original Location", color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(180.dp))
            Text("Date Deleted", color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(120.dp))
            Text("Size", color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(60.dp))
        }

        // ── Item list ──
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        tint = textColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        "Recycle Bin is empty",
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                    Text(
                        "Items you delete will appear here",
                        color = textColor.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedItems, key = { it.id }) { item ->
                    RecycleBinItemRow(
                        item = item,
                        isSelected = item.id in selectedIds,
                        textColor = textColor,
                        isDark = isDark,
                        onToggleSelect = {
                            selectedIds = if (item.id in selectedIds)
                                selectedIds - item.id
                            else
                                selectedIds + item.id
                        },
                        onRestore = { viewModel.restoreFromRecycleBin(item.id) },
                        onPermanentDelete = { viewModel.permanentlyDelete(item.id) }
                    )
                }
            }
        }
    }

    // ── Empty bin confirmation dialog ──
    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty Recycle Bin") },
            text = { Text("Permanently delete all ${items.size} items? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyRecycleBin()
                        showEmptyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Win11Colors.DangerRed)
                ) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecycleBinItemRow(
    item: RecycleBinItem,
    isSelected: Boolean,
    textColor: Color,
    isDark: Boolean,
    onToggleSelect: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val icon = getFileIconForName(item.name)
    val iconColor = getFileIconColorForName(item.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Win11Colors.AccentBlue.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onToggleSelect,
                onLongClick = { showContextMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                color = textColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "From: ${item.originalPath.substringBeforeLast("/")}",
                color = textColor.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            item.originalPath.substringBeforeLast("/").takeLast(15),
            color = textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.width(180.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatDate(item.deletedAt),
            color = textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.width(120.dp)
        )
        Text(
            if (item.sizeBytes > 0) formatFileSize(item.sizeBytes) else "--",
            color = textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.width(60.dp)
        )

        // Context menu trigger
        Box {
            IconButton(
                onClick = { showContextMenu = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    null,
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Restore") },
                    onClick = {
                        showContextMenu = false
                        onRestore()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.RestoreFromTrash,
                            null,
                            tint = Win11Colors.AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete permanently", color = Win11Colors.DangerRed) },
                    onClick = {
                        showContextMenu = false
                        onPermanentDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteForever,
                            null,
                            tint = Win11Colors.DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}