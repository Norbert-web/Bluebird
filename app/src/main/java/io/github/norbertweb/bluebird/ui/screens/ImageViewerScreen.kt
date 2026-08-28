package io.github.norbertweb.bluebird.ui.screens

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Design Tokens — Windows 11 Photos aesthetic
// ─────────────────────────────────────────────────────────────────

private object Ph {
    // Dark
    val DBg         = Color(0xFF0F0F0F)
    val DSurface    = Color(0xFF1A1A1A)
    val DSurfaceH   = Color(0xFF242424)
    val DBorder     = Color(0xFF2D2D2D)
    val DText       = Color(0xFFFFFFFF)
    val DTextSec    = Color(0xFFAAAAAA)
    val DTextMuted  = Color(0xFF606060)
    val DHover      = Color(0x10FFFFFF)
    val DSel        = Color(0x280078D4)

    // Light
    val LBg         = Color(0xFFF2F2F2)
    val LSurface    = Color(0xFFFFFFFF)
    val LSurfaceH   = Color(0xFFEBEBEB)
    val LBorder     = Color(0xFFDDDDDD)
    val LText       = Color(0xFF1A1A1A)
    val LTextSec    = Color(0xFF555555)
    val LTextMuted  = Color(0xFF999999)
    val LHover      = Color(0x0A000000)
    val LSel        = Color(0x1A0078D4)

    val Accent      = Color(0xFF0078D4)
    val AccentLight = Color(0xFF429CE3)
    val AccentDark  = Color(0xFF005A9E)
    val DangerRed   = Color(0xFFD83B01)
    val Gold        = Color(0xFFFFB900)
    val Green       = Color(0xFF107C10)
}

// ─────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────

data class PhotoItem(
    val uri: Uri,
    val file: File?,
    val name: String,
    val extension: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val relativePath: String = "",
    val isVideo: Boolean = extension in VIDEO_IMG_EXTS
) {
    val key: String get() = file?.absolutePath ?: uri.toString()
    val parentKey: String get() = relativePath.ifBlank { file?.parent ?: "" }
    val displayPath: String get() = file?.parent ?: relativePath.trimEnd('/').ifBlank { "—" }
}

data class PhotoAlbum(
    val name: String,
    val key: String,
    val coverUri: Uri?,
    val count: Int
)

enum class PhotoView { ALBUMS, GRID, VIEWER }
enum class GridSize { SMALL, MEDIUM, LARGE }
enum class PhotoSort { DATE_NEW, DATE_OLD, NAME, SIZE }
enum class PhotoFilter { ALL, PHOTOS, VIDEOS, FAVORITES }

val PHOTO_EXTS     = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "tiff", "raw")
val VIDEO_IMG_EXTS = setOf("mp4", "mkv", "mov", "avi", "3gp", "webm")
val ALL_IMG_EXTS   = PHOTO_EXTS + VIDEO_IMG_EXTS

private fun fmtSize(bytes: Long): String = when {
    bytes < 1024      -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1024f)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576f)
    else              -> "%.2f GB".format(bytes / 1_073_741_824f)
}

private fun fmtDate(ms: Long): String =
    SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()).format(Date(ms))

private fun fmtDateShort(ms: Long): String =
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(ms))

// ─────────────────────────────────────────────────────────────────
// App State
// ─────────────────────────────────────────────────────────────────

private class PhotosState {
    var allPhotos    by mutableStateOf(listOf<PhotoItem>())
    var albums       by mutableStateOf(listOf<PhotoAlbum>())
    var favorites    by mutableStateOf(setOf<String>())
    var selected     by mutableStateOf(setOf<String>())
    var isLoading    by mutableStateOf(false)
    var hasLoaded    by mutableStateOf(false)

    var view         by mutableStateOf(PhotoView.ALBUMS)
    var gridSize     by mutableStateOf(GridSize.MEDIUM)
    var sortMode     by mutableStateOf(PhotoSort.DATE_NEW)
    var filterMode   by mutableStateOf(PhotoFilter.ALL)
    var searchQuery  by mutableStateOf("")
    var activeAlbum  by mutableStateOf<PhotoAlbum?>(null)
    var viewerIndex  by mutableStateOf(0)
    var selectionMode by mutableStateOf(false)

    var showInfo     by mutableStateOf(false)
    var showControls by mutableStateOf(true)
    var showDeleteDialog by mutableStateOf(false)
    var showSortMenu by mutableStateOf(false)
    var toastMsg     by mutableStateOf<String?>(null)

    fun toast(msg: String) { toastMsg = msg }

    fun displayedPhotos(): List<PhotoItem> {
        val albumPath = activeAlbum?.key
        val query = searchQuery.trim()
        val filtered = allPhotos.asSequence()
            .filter { albumPath == null || it.parentKey == albumPath }
            .filter { p -> when (filterMode) {
                PhotoFilter.ALL -> true
                PhotoFilter.PHOTOS -> !p.isVideo
                PhotoFilter.VIDEOS -> p.isVideo
                PhotoFilter.FAVORITES -> p.key in favorites
            }}
            .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
            .toList()

        return when (sortMode) {
            PhotoSort.DATE_NEW -> filtered.sortedByDescending { it.lastModified }
            PhotoSort.DATE_OLD -> filtered.sortedBy { it.lastModified }
            PhotoSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            PhotoSort.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
    }

    fun toggleFavorite(path: String) {
        favorites = if (path in favorites) favorites - path else favorites + path
    }

    fun toggleSelect(path: String) {
        selected = if (path in selected) selected - path else selected + path
        if (selected.isEmpty()) selectionMode = false
    }
}

@Composable
private fun displayedPhotos(state: PhotosState): List<PhotoItem> = remember(
    state.allPhotos, state.activeAlbum?.key, state.filterMode,
    state.favorites, state.searchQuery, state.sortMode
) { state.displayedPhotos() }

@Composable
private fun rememberPhotosState() = remember { PhotosState() }

private fun hasPhotoLibraryAccess(context: android.content.Context): Boolean {
    return when {
        android.os.Build.VERSION.SDK_INT >= 34 -> {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        android.os.Build.VERSION.SDK_INT >= 33 -> {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        else -> androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun PhotoPermissionGate(isDark: Boolean, onGranted: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPhotoLibraryAccess(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = hasPhotoLibraryAccess(context)
        if (granted) onGranted()
    }

    fun requestAccess() {
        when {
            android.os.Build.VERSION.SDK_INT >= 34 -> launcher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                )
            )
            android.os.Build.VERSION.SDK_INT >= 33 -> launcher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                )
            )
            else -> launcher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) requestAccess()
    }

    val textColor = if (isDark) Ph.DText else Ph.LText
    val secondary = if (isDark) Ph.DTextSec else Ph.LTextSec
    Box(Modifier.fillMaxSize().background(if (isDark) Ph.DBg else Ph.LBg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = textColor, modifier = Modifier.size(52.dp))
            Text("Photo access needed", color = textColor, style = MaterialTheme.typography.titleLarge)
            Text(
                "Allow Bluebird to view your photos and videos. On Android 14+, you can choose only the media you want to share.",
                color = secondary,
                textAlign = TextAlign.Center
            )
            Button(onClick = { requestAccess() }) { Text("Choose photos and videos") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@Composable
fun ImageViewerScreen(
    isDark: Boolean,
    initialPath: String = "",
    viewModel: LauncherViewModel? = null
) {
    val ctx   = LocalContext.current
    val state = rememberPhotosState()
    val scope = rememberCoroutineScope()
    var hasLibraryAccess by remember { mutableStateOf(hasPhotoLibraryAccess(ctx)) }

    if (!hasLibraryAccess) {
        PhotoPermissionGate(isDark = isDark) { hasLibraryAccess = true }
        return
    }

    val bg       = if (isDark) Ph.DBg      else Ph.LBg
    val surface  = if (isDark) Ph.DSurface else Ph.LSurface
    val surfaceH = if (isDark) Ph.DSurfaceH else Ph.LSurfaceH
    val border   = if (isDark) Ph.DBorder  else Ph.LBorder
    val tc       = if (isDark) Ph.DText    else Ph.LText
    val tcs      = if (isDark) Ph.DTextSec else Ph.LTextSec
    val tcm      = if (isDark) Ph.DTextMuted else Ph.LTextMuted

    // ── ONE-TIME scan on first launch ──
    suspend fun scanPhotos() {
        state.isLoading = true
        val result = withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.MIME_TYPE
            ).let { base ->
                if (initialPath.isNotEmpty()) base + MediaStore.MediaColumns.DATA else base
            }
            val found = LinkedHashMap<String, PhotoItem>()

            fun addFromCursor(c: android.database.Cursor, collection: Uri, isVideo: Boolean) {
                val idCol = c.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val relCol = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val dataCol = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (idCol < 0) return
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val itemUri = ContentUris.withAppendedId(collection, id)
                    val nameWithExt = if (nameCol >= 0) c.getString(nameCol).orEmpty() else "Untitled"
                    val ext = nameWithExt.substringAfterLast('.', "").lowercase()
                    val size = if (sizeCol >= 0 && !c.isNull(sizeCol)) c.getLong(sizeCol) else 0L
                    val modified = if (dateCol >= 0 && !c.isNull(dateCol)) c.getLong(dateCol) * 1000L else 0L
                    val relative = if (relCol >= 0) c.getString(relCol).orEmpty() else ""
                    val resolvedPath = if (initialPath.isNotEmpty() && dataCol >= 0 && !c.isNull(dataCol)) c.getString(dataCol) else null
                    val photo = PhotoItem(
                        uri = itemUri,
                        file = resolvedPath?.let(::File),
                        name = nameWithExt.substringBeforeLast('.', nameWithExt),
                        extension = ext,
                        sizeBytes = size,
                        lastModified = modified,
                        relativePath = relative,
                        isVideo = isVideo
                    )
                    found.putIfAbsent(itemUri.toString(), photo)
                }
            }

            fun query(collection: Uri, isVideo: Boolean) {
                ctx.contentResolver.query(
                    collection, projection, null, null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { addFromCursor(it, collection, isVideo) }
            }

            query(if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false)
            query(if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)

            val photos = found.values.sortedByDescending { it.lastModified }
            val albumMap = photos.groupBy { it.parentKey }
            val albums = albumMap.entries.map { (key, items) ->
                val displayName = key.trimEnd('/').substringAfterLast('/').ifBlank { "Unknown" }
                PhotoAlbum(displayName, key, items.firstOrNull()?.uri, items.size)
            }.sortedByDescending { it.count }
            photos to albums
        }

        state.allPhotos = result.first
        state.albums = result.second
        state.isLoading = false
        state.hasLoaded = true
        val validKeys = result.first.asSequence().map { it.key }.toSet()
        state.selected = state.selected.filter(validKeys::contains).toSet()
        if (initialPath.isNotEmpty()) {
            val idx = result.first.indexOfFirst { it.file?.absolutePath == initialPath || it.uri.toString() == initialPath }
            if (idx >= 0) { state.viewerIndex = idx; state.view = PhotoView.VIEWER }
        }
    }

    LaunchedEffect(Unit) {
        if (!state.hasLoaded) scanPhotos()
    }

    // Toast auto-dismiss
    LaunchedEffect(state.toastMsg) {
        if (state.toastMsg != null) { delay(2500); state.toastMsg = null }
    }

    // Controls auto-hide in viewer
    LaunchedEffect(state.showControls, state.view) {
        if (state.view == PhotoView.VIEWER && state.showControls) {
            delay(4000); state.showControls = false
        }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        when (state.view) {
            PhotoView.ALBUMS -> AlbumsView(state, isDark, bg, surface, surfaceH, border, tc, tcs, tcm,
                onRescan = { scope.launch { scanPhotos() } })
            PhotoView.GRID   -> GridView(state, isDark, bg, surface, surfaceH, border, tc, tcs, tcm,
                viewModel = viewModel, ctx = ctx,
                onRescan = { scope.launch { scanPhotos() } })
            PhotoView.VIEWER -> ViewerView(state, isDark, viewModel, ctx)
        }

        // Toast
        state.toastMsg?.let { msg ->
            Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF323232), shadowElevation = 8.dp) {
                    Text(msg, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Albums View — landing page
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AlbumsView(
    state: PhotosState, isDark: Boolean,
    bg: Color, surface: Color, surfaceH: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color,
    onRescan: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── Top bar ──
        PhotosTopBar(
            title = "Photos",
            isDark = isDark, surface = surface, border = border, tc = tc, tcm = tcm,
            actions = {
                PIconBtn(Icons.Default.Refresh, "Rescan", tc) { onRescan() }
                PIconBtn(Icons.Default.GridView, "All Photos", tc) {
                    state.activeAlbum = null; state.view = PhotoView.GRID
                }
            }
        )

        if (state.isLoading) {
            LoadingState(tc, tcm)
        } else if (!state.hasLoaded) {
            FirstLaunchPrompt(tc, tcm, onRescan)
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize()) {
                // Hero — All Photos
                item {
                    AllPhotosHero(state, isDark, tc, tcs, tcm, surface)
                }

                // Stats bar
                item {
                    StatsRow(state, tc, tcm, surface, border)
                }

                // Filter chips
                item {
                    FilterRow(state, isDark, bg, tc)
                }

                // Section header
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Albums", color = tc, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.albums.size} albums", color = tcm, fontSize = 12.sp)
                    }
                }

                // Album grid — 2 per row
                val albums = state.albums
                val rows = (albums.size + 1) / 2
                items(rows) { rowIdx ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val a1 = albums.getOrNull(rowIdx * 2)
                        val a2 = albums.getOrNull(rowIdx * 2 + 1)
                        if (a1 != null) AlbumCard(a1, isDark, tc, tcs, tcm, Modifier.weight(1f)) {
                            state.activeAlbum = a1; state.view = PhotoView.GRID
                        }
                        if (a2 != null) AlbumCard(a2, isDark, tc, tcs, tcm, Modifier.weight(1f)) {
                            state.activeAlbum = a2; state.view = PhotoView.GRID
                        } else Spacer(Modifier.weight(1f))
                    }
                }

                // Recent photos section
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent", color = tc, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("See all", color = Ph.Accent, fontSize = 12.sp, modifier = Modifier.clickable { state.activeAlbum = null; state.view = PhotoView.GRID })
                    }
                }

                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(120.dp)) {
                        items(state.allPhotos.take(20)) { photo ->
                            SubcomposeAsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)).clickable {
                                    state.viewerIndex = state.displayedPhotos().indexOf(photo).coerceAtLeast(0)
                                    state.view = PhotoView.VIEWER
                                },
                                contentScale = ContentScale.Crop,
                                loading = { Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH)) },
                                error = { Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH)) { Icon(Icons.Default.BrokenImage, null, tint = tcm, modifier = Modifier.align(Alignment.Center).size(24.dp)) } }
                            )
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun AllPhotosHero(state: PhotosState, isDark: Boolean, tc: Color, tcs: Color, tcm: Color, surface: Color) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).clickable { state.activeAlbum = null; state.view = PhotoView.GRID }
    ) {
        // Collage of last 4 photos
        if (state.allPhotos.size >= 4) {
            Row(Modifier.fillMaxSize()) {
                AsyncImage(state.allPhotos[0].file, null, modifier = Modifier.weight(2f).fillMaxHeight(), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(2.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AsyncImage(state.allPhotos[1].file, null, modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Crop)
                    AsyncImage(state.allPhotos[2].file, null, modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Crop)
                    AsyncImage(state.allPhotos[3].file, null, modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Crop)
                }
            }
        } else if (state.allPhotos.isNotEmpty()) {
            AsyncImage(state.allPhotos[0].file, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = tcm, modifier = Modifier.size(48.dp))
            }
        }
        // Gradient + label
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("All Photos", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${state.allPhotos.size} items", color = Color.White.copy(0.75f), fontSize = 13.sp)
        }
        // Play icon overlay
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).background(Color.Black.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatsRow(state: PhotosState, tc: Color, tcm: Color, surface: Color, border: Color) {
    val photos = state.allPhotos.count { !it.isVideo }
    val videos = state.allPhotos.count { it.isVideo }
    val totalSize = state.allPhotos.sumOf { it.sizeBytes }

    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Photos", "$photos", Icons.Outlined.Photo, Ph.Accent, surface, tc, tcm, Modifier.weight(1f))
        StatCard("Videos", "$videos", Icons.Default.Videocam, Ph.Green, surface, tc, tcm, Modifier.weight(1f))
        StatCard("Storage", fmtSize(totalSize), Icons.Default.Storage, Ph.Gold, surface, tc, tcm, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, iconColor: Color, surface: Color, tc: Color, tcm: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            Text(value, color = tc, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = tcm, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AlbumCard(album: PhotoAlbum, isDark: Boolean, tc: Color, tcs: Color, tcm: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(bottom = 8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            if (album.coverUri != null) {
                AsyncImage(album.coverUri, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoAlbum, null, tint = tcm, modifier = Modifier.size(36.dp))
                }
            }
            // Count badge
            Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(0.65f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("${album.count}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(album.name, color = tc, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.count} items", color = tcm, fontSize = 10.sp)
    }
}

@Composable
private fun FilterRow(state: PhotosState, isDark: Boolean, bg: Color, tc: Color) {
    val chips = listOf(PhotoFilter.ALL to "All", PhotoFilter.PHOTOS to "📷 Photos", PhotoFilter.VIDEOS to "🎬 Videos", PhotoFilter.FAVORITES to "❤️ Favorites")
    LazyRow(Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(chips) { (filter, label) ->
            val active = state.filterMode == filter
            Surface(shape = RoundedCornerShape(20.dp),
                color = if (active) Ph.Accent else (if (isDark) Ph.DSurfaceH else Ph.LSurfaceH),
                border = if (!active) BorderStroke(1.dp, if (isDark) Ph.DBorder else Ph.LBorder) else null,
                modifier = Modifier.clickable { state.filterMode = filter }) {
                Text(label, color = if (active) Color.White else tc, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun LoadingState(tc: Color, tcm: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Ph.Accent, strokeWidth = 2.dp)
            Text("Scanning your photos…", color = tc, fontSize = 14.sp)
            Text("This only happens once", color = tcm, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FirstLaunchPrompt(tc: Color, tcm: Color, onScan: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(80.dp).background(Ph.Accent.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Ph.Accent, modifier = Modifier.size(40.dp))
            }
            Text("Welcome to Photos", color = tc, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Scan your device to find all your photos and videos.", color = tcm, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Button(onClick = onScan, colors = ButtonDefaults.buttonColors(containerColor = Ph.Accent), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Scan Device")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Grid View
// ─────────────────────────────────────────────────────────────────

@Composable
private fun GridView(
    state: PhotosState, isDark: Boolean,
    bg: Color, surface: Color, surfaceH: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color,
    viewModel: LauncherViewModel?,
    ctx: android.content.Context,
    onRescan: () -> Unit
) {
    val photos = displayedPhotos(state)
    val cols = when (state.gridSize) { GridSize.SMALL -> 5; GridSize.MEDIUM -> 3; GridSize.LARGE -> 2 }
    val gridState = rememberLazyGridState()

    // Group by month
    val grouped: Map<String, List<IndexedValue<PhotoItem>>> = photos.withIndex().groupBy { (_, p) -> fmtDateShort(p.lastModified) }

    Column(Modifier.fillMaxSize()) {
        PhotosTopBar(
            title = state.activeAlbum?.name ?: "All Photos",
            isDark = isDark, surface = surface, border = border, tc = tc, tcm = tcm,
            onBack = { state.activeAlbum = null; state.view = PhotoView.ALBUMS },
            actions = {
                // Search
                Row(
                    Modifier.width(160.dp).height(30.dp)
                        .clip(RoundedCornerShape(15.dp)).background(surfaceH)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Search, null, tint = tcm, modifier = Modifier.size(13.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty()) Text("Search", color = tcm, fontSize = 11.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            state.searchQuery, { state.searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 11.sp),
                            cursorBrush = SolidColor(Ph.Accent), singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                // Grid size
                PIconBtn(when (state.gridSize) { GridSize.SMALL -> Icons.Default.GridOn; GridSize.MEDIUM -> Icons.Default.GridView; GridSize.LARGE -> Icons.Default.ViewModule }, "Grid size", tc) {
                    state.gridSize = GridSize.values()[(state.gridSize.ordinal + 1) % 3]
                }
                // Sort
                Box {
                    PIconBtn(Icons.Default.Sort, "Sort", tc) { state.showSortMenu = true }
                    DropdownMenu(expanded = state.showSortMenu, onDismissRequest = { state.showSortMenu = false }) {
                        listOf(PhotoSort.DATE_NEW to "Newest first", PhotoSort.DATE_OLD to "Oldest first", PhotoSort.NAME to "By name", PhotoSort.SIZE to "By size").forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (state.sortMode == sort) Ph.Accent else tc, fontSize = 13.sp) },
                                leadingIcon = { if (state.sortMode == sort) Icon(Icons.Default.Check, null, tint = Ph.Accent, modifier = Modifier.size(16.dp)) },
                                onClick = { state.sortMode = sort; state.showSortMenu = false }
                            )
                        }
                    }
                }
                PIconBtn(Icons.Default.Refresh, "Rescan", tc) { onRescan() }
            }
        )

        // Selection bar
        AnimatedVisibility(state.selectionMode) {
            Row(Modifier.fillMaxWidth().background(Ph.Accent).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.selected.size} selected", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionAction(Icons.Default.Share, "Share") {
                        sharePhotos(ctx, state.selected.mapNotNull { key -> state.allPhotos.firstOrNull { it.key == key }?.uri })
                    }
                    SelectionAction(Icons.Default.Delete, "Delete") {
                        state.showDeleteDialog = true
                    }
                    SelectionAction(Icons.Default.Close, "Cancel") {
                        state.selected = emptySet(); state.selectionMode = false
                    }
                }
            }
        }

        // Filter row
        FilterRow(state, isDark, bg, tc)

        // Stats
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${photos.size} items", color = tcm, fontSize = 11.sp)
            if (state.searchQuery.isNotEmpty()) Text("Searching: \"${state.searchQuery}\"", color = Ph.Accent, fontSize = 11.sp)
        }

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, null, tint = tcm, modifier = Modifier.size(48.dp))
                    Text("No photos found", color = tc, fontSize = 16.sp)
                    if (state.searchQuery.isNotEmpty()) Text("Try a different search term", color = tcm, fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                state = gridState,
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Group header + items
                grouped.forEach { (month, indexedItems) ->
                    item(span = { GridItemSpan(cols) }) {
                        Text(month, color = tcs, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    items(indexedItems, key = { it.value.key }) { (globalIdx, photo) ->
                        GridPhotoCell(
                            photo = photo,
                            isSelected = photo.key in state.selected,
                            isFavorite = photo.key in state.favorites,
                            inSelectionMode = state.selectionMode,
                            cols = cols,
                            isDark = isDark,
                            onClick = {
                                if (state.selectionMode) {
                                    state.toggleSelect(photo.key)
                                } else {
                                    state.viewerIndex = globalIdx
                                    state.view = PhotoView.VIEWER
                                    state.showControls = true
                                }
                            },
                            onLongPress = {
                                state.selectionMode = true
                                state.toggleSelect(photo.key)
                            }
                        )
                    }
                }
                item(span = { GridItemSpan(cols) }) { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Delete dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { state.showDeleteDialog = false },
            containerColor = surface, shape = RoundedCornerShape(12.dp),
            title = { Text("Delete ${state.selected.size} item(s)?", color = tc, fontWeight = FontWeight.SemiBold) },
            text = { Text("This action cannot be undone.", color = tcs, fontSize = 13.sp) },
            confirmButton = { Button(onClick = {
                val deleted = state.selected.toSet()
                deleted.forEach { key ->
                    state.allPhotos.firstOrNull { it.key == key }?.let { item ->
                        if (item.file != null) {
                            viewModel?.deleteToRecycleBin(item.file.absolutePath) ?: item.file.delete()
                        } else {
                            runCatching { ctx.contentResolver.delete(item.uri, null, null) }
                        }
                    }
                }
                state.allPhotos = state.allPhotos.filter { it.key !in deleted }
                state.favorites = state.favorites - deleted
                state.selected = emptySet(); state.selectionMode = false; state.showDeleteDialog = false
                state.toast("Deleted")
            }, colors = ButtonDefaults.buttonColors(containerColor = Ph.DangerRed), shape = RoundedCornerShape(6.dp)) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = { state.showDeleteDialog = false }, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = tc) } }
        )
    }
}

@Composable
private fun GridPhotoCell(
    photo: PhotoItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    inSelectionMode: Boolean,
    cols: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val selectedAnim by animateFloatAsState(if (isSelected) 0.85f else 1f, label = "sel")
    Box(
        Modifier.aspectRatio(1f)
            .scale(selectedAnim)
            .clip(RoundedCornerShape(if (cols >= 4) 4.dp else 6.dp))
            .pointerInput(photo.key) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
    ) {
        SubcomposeAsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH)) },
            error = { Box(Modifier.fillMaxSize().background(if (isDark) Ph.DSurfaceH else Ph.LSurfaceH), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BrokenImage, null, tint = Ph.DTextMuted, modifier = Modifier.size(20.dp))
            }}
        )
        // Video badge
        if (photo.isVideo) {
            Box(Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(0.65f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text("VIDEO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // Favorite
        if (isFavorite) {
            Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.size(14.dp).align(Alignment.TopEnd).padding(3.dp))
        }
        // Selection indicator
        if (inSelectionMode) {
            Box(Modifier.fillMaxSize().background(if (isSelected) Ph.Accent.copy(0.3f) else Color.Transparent))
            Box(Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp)
                .background(if (isSelected) Ph.Accent else Color.Black.copy(0.4f), CircleShape)
                .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Full Viewer
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ViewerView(
    state: PhotosState,
    isDark: Boolean,
    viewModel: LauncherViewModel?,
    ctx: android.content.Context
) {
    val photos = displayedPhotos(state)
    val photo = photos.getOrNull(state.viewerIndex)

    var scale by remember(state.viewerIndex) { mutableStateOf(1f) }
    var offsetX by remember(state.viewerIndex) { mutableStateOf(0f) }
    var offsetY by remember(state.viewerIndex) { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }

    val tc = if (isDark) Ph.DText else Ph.LText
    val tcm = if (isDark) Ph.DTextMuted else Ph.LTextMuted
    val surface = if (isDark) Ph.DSurface else Ph.LSurface

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ── Image ──
        if (photo != null) {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(state.viewerIndex) {
                        detectTapGestures(
                            onDoubleTap = { scale = if (scale > 1.5f) 1f else 2.5f; offsetX = 0f; offsetY = 0f },
                            onTap = { state.showControls = !state.showControls }
                        )
                    }
                    .pointerInput(state.viewerIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 8f)
                            if (scale > 1.02f) { offsetX += pan.x; offsetY += pan.y }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.name,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offsetX; translationY = offsetY
                        rotationZ = rotation
                    },
                    contentScale = if (scale > 1f) ContentScale.None else ContentScale.Fit
                )
            }

            // Swipe arrows (sides)
            AnimatedVisibility(state.showControls && state.viewerIndex > 0, modifier = Modifier.align(Alignment.CenterStart)) {
                Box(Modifier.padding(start = 8.dp).size(40.dp).background(Color.Black.copy(0.5f), CircleShape).clickable { state.viewerIndex--; rotation = 0f }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            AnimatedVisibility(state.showControls && state.viewerIndex < photos.size - 1, modifier = Modifier.align(Alignment.CenterEnd)) {
                Box(Modifier.padding(end = 8.dp).size(40.dp).background(Color.Black.copy(0.5f), CircleShape).clickable { state.viewerIndex++; rotation = 0f }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── Top bar ──
        AnimatedVisibility(state.showControls, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }, modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent))).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).clickable { state.view = PhotoView.GRID; state.showControls = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(photo?.name ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state.viewerIndex + 1} of ${photos.size}", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                // Zoom reset
                if (scale != 1f) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.15f)).clickable { scale = 1f; offsetX = 0f; offsetY = 0f }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${(scale * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                    }
                }
                // Favorite
                photo?.let { p ->
                    val isFav = p.key in state.favorites
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) Color.Red else Color.White, modifier = Modifier.size(22.dp).clickable { state.toggleFavorite(p.key) })
                }
                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { state.showInfo = !state.showInfo })
            }
        }

        // ── Bottom toolbar ──
        AnimatedVisibility(state.showControls, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))) {
                // Thumbnail filmstrip
                if (photos.size > 1) {
                    val stripState = rememberLazyListState()
                    LaunchedEffect(state.viewerIndex) {
                        stripState.animateScrollToItem((state.viewerIndex - 2).coerceAtLeast(0))
                    }
                    LazyRow(
                        state = stripState,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(64.dp).fillMaxWidth()
                    ) {
                        itemsIndexed(photos) { idx, p ->
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(5.dp))
                                    .border(if (idx == state.viewerIndex) BorderStroke(2.dp, Ph.Accent) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(5.dp))
                                    .clickable { state.viewerIndex = idx; rotation = 0f }
                            ) {
                                AsyncImage(p.uri, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (idx == state.viewerIndex) Box(Modifier.fillMaxSize().background(Color.White.copy(0.1f)))
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerAction(Icons.Default.RotateLeft, "Rotate L") { rotation = (rotation - 90f) % 360f }
                    ViewerAction(Icons.Default.RotateRight, "Rotate R") { rotation = (rotation + 90f) % 360f }
                    ViewerAction(Icons.Default.Share, "Share") {
                        photo?.let { p ->
                            try {
                                val uri = p.uri
                                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            } catch (_: Exception) {}
                        }
                    }
                    ViewerAction(Icons.Default.Wallpaper, "Wallpaper") {
                        photo?.let { p -> viewModel?.setCustomWallpaper(p.uri.toString(), WallpaperTarget.HOME, ctx) }
                    }
                    ViewerAction(Icons.Default.OpenWith, "Open With") {
                        photo?.let { p ->
                            try {
                                val uri = p.uri
                                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Open with").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            } catch (_: Exception) {}
                        }
                    }
                    ViewerAction(Icons.Default.Delete, "Delete", Ph.DangerRed) {
                        photo?.let { p ->
                            val deleted = if (p.file != null) {
                                if (viewModel != null) {
                                    viewModel.deleteToRecycleBin(p.file.absolutePath)
                                    true
                                } else {
                                    p.file.delete()
                                }
                            } else {
                                runCatching { ctx.contentResolver.delete(p.uri, null, null) }.getOrDefault(0) > 0
                            }
                            if (deleted) {
                                state.allPhotos = state.allPhotos.filter { it.key != p.key }
                                if (state.viewerIndex >= state.displayedPhotos().size) state.viewerIndex = (state.displayedPhotos().size - 1).coerceAtLeast(0)
                                state.toast("Deleted")
                            }
                        }
                    }
                }
            }
        }

        // ── Info panel ──
        AnimatedVisibility(state.showInfo && photo != null, enter = fadeIn() + slideInHorizontally { it }, exit = fadeOut() + slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd)) {
            Surface(Modifier.padding(end = 10.dp).width(210.dp), shape = RoundedCornerShape(14.dp), color = Color(0xEA1A1A1A), shadowElevation = 12.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, tint = Ph.Accent, modifier = Modifier.size(16.dp))
                        Text("File Info", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Close, null, tint = Ph.DTextMuted, modifier = Modifier.size(16.dp).clickable { state.showInfo = false })
                    }
                    Divider(color = Ph.DBorder)
                    photo?.let { p ->
                        InfoPill("Name", p.name)
                        InfoPill("Extension", p.extension.uppercase())
                        InfoPill("Size", fmtSize(p.sizeBytes))
                        InfoPill("Modified", fmtDate(p.lastModified))
                        InfoPill("Path", p.displayPath)
                        InfoPill("Zoom", "${(scale * 100).roundToInt()}%")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(0.45f), fontSize = 10.sp, letterSpacing = 0.5.sp)
        Text(value, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────────
// Shared Helpers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PhotosTopBar(
    title: String,
    isDark: Boolean,
    surface: Color, border: Color, tc: Color, tcm: Color,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(surface).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onBack != null) {
                Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null, tint = tc, modifier = Modifier.size(18.dp))
                }
            } else {
                Box(Modifier.size(28.dp).background(Ph.Accent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(title, color = tc, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
        }
        Divider(color = border)
    }
}

@Composable
private fun PIconBtn(icon: ImageVector, desc: String, tc: Color, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tc, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint.copy(0.8f), fontSize = 9.sp)
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).background(Color.White.copy(0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}

private fun sharePhotos(ctx: android.content.Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share photos").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}