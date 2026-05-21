package com.bluebird.ui.screens

import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────
// Design tokens — Windows 11 Films & TV aesthetic
// ─────────────────────────────────────────────────────────────────

private object FTV {
    val Bg          = Color(0xFF0D0D0D)
    val BgMid       = Color(0xFF141414)
    val Surface     = Color(0xFF1C1C1C)
    val SurfaceHigh = Color(0xFF242424)
    val Border      = Color(0xFF2E2E2E)
    val SelectedBg  = Color(0x250078D4)
    val Text        = Color(0xFFFFFFFF)
    val TextSec     = Color(0xFFAAAAAA)
    val TextMuted   = Color(0xFF666666)
    val Accent      = Color(0xFF0078D4)
    val AccentGlow  = Color(0xFF429CE3)
    val AccentDim   = Color(0xFF005A9E)
    val Gold        = Color(0xFFFFB900)
    val VideoGreen  = Color(0xFF16C60C)
    val AudioPurple = Color(0xFF9B59B6)

    val LBg          = Color(0xFFF3F3F3)
    val LSurface     = Color(0xFFFFFFFF)
    val LSurfaceHigh = Color(0xFFEBEBEB)
    val LBorder      = Color(0xFFDDDDDD)
    val LText        = Color(0xFF1A1A1A)
    val LTextSec     = Color(0xFF555555)
    val LTextMuted   = Color(0xFF999999)
}

// ─────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────

data class MediaTrack(
    val file: File,
    val title: String      = file.nameWithoutExtension,
    val artist: String     = "Unknown Artist",
    val album: String      = "Unknown Album",
    val durationMs: Long   = 0L,
    val albumArtBytes: ByteArray? = null,
    val isVideo: Boolean   = file.extension.lowercase() in VIDEO_EXTS
)

enum class RepeatMode  { OFF, REPEAT_ALL, REPEAT_ONE }
enum class MediaTab    { VIDEOS, MUSIC, PLAYLIST }

val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "ts", "m4v")
val AUDIO_EXTS = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma", "ape")

// Playback speed steps
private val SPEED_STEPS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private fun speedLabel(speed: Float) = when (speed) {
    0.5f  -> "0.5×"; 0.75f -> "0.75×"; 1.0f  -> "1×"
    1.25f -> "1.25×"; 1.5f -> "1.5×"; 2.0f   -> "2×"; else -> "1×"
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatTimer(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ─────────────────────────────────────────────────────────────────
// State Holder
// ─────────────────────────────────────────────────────────────────

private class PlayerState {
    // Library
    var allTracks   by mutableStateOf(listOf<MediaTrack>())
    var videoTracks by mutableStateOf(listOf<MediaTrack>())
    var audioTracks by mutableStateOf(listOf<MediaTrack>())
    var isLoading   by mutableStateOf(false)
    var showHidden  by mutableStateOf(false)          // FIX: hidden-file setting

    // Playlist / navigation
    var playlist     by mutableStateOf(listOf<MediaTrack>())
    var currentIndex by mutableStateOf(-1)
    var isPlaying    by mutableStateOf(false)
    var isShuffle    by mutableStateOf(false)
    var repeatMode   by mutableStateOf(RepeatMode.OFF)

    // Position
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)

    // Volume
    var volume  by mutableStateOf(1f)
    var isMuted by mutableStateOf(false)

    // UI state
    var activeTab    by mutableStateOf(MediaTab.MUSIC)
    var showQueue    by mutableStateOf(true)
    var isFullscreen by mutableStateOf(false)
    var showControls by mutableStateOf(true)
    var isBuffering  by mutableStateOf(false)
    var searchQuery  by mutableStateOf("")

    // Playback engine refs
    var mediaPlayer by mutableStateOf<MediaPlayer?>(null)
    var videoView   by mutableStateOf<VideoView?>(null)

    // ── NEW: Playback speed ──
    var playbackSpeed by mutableStateOf(1.0f)

    // ── NEW: Sleep timer ──
    var sleepTimerSeconds by mutableStateOf(0L)   // 0 = off
    var sleepTimerActive  by mutableStateOf(false)

    // ── NEW: Folder/album grouping ──
    // Groups audio tracks by album; videos by parent folder name
    val audioGroups: Map<String, List<MediaTrack>> get() =
        audioTracks.groupBy { it.album.ifBlank { "Unknown Album" } }.toSortedMap()
    val videoGroups: Map<String, List<MediaTrack>> get() =
        videoTracks.groupBy { it.file.parentFile?.name ?: "Unknown Folder" }.toSortedMap()

    val currentTrack get() = playlist.getOrNull(currentIndex)

    val filteredTracks get() = when (activeTab) {
        MediaTab.VIDEOS   -> videoTracks.filter { searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
        MediaTab.MUSIC    -> audioTracks.filter { searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
        MediaTab.PLAYLIST -> playlist.filter    { searchQuery.isEmpty() || it.title.contains(searchQuery, true) }
    }

    fun playTrack(index: Int) { currentIndex = index; isPlaying = true }

    fun skipNext() {
        if (playlist.isEmpty()) return
        when {
            isShuffle -> playlist.indices.filter { it != currentIndex }.randomOrNull()?.let { currentIndex = it }
            repeatMode == RepeatMode.REPEAT_ALL -> currentIndex = (currentIndex + 1) % playlist.size
            currentIndex < playlist.size - 1   -> currentIndex++
        }
    }

    fun skipPrev() {
        if (positionMs > 3000) { positionMs = 0; return }
        when {
            isShuffle -> playlist.indices.filter { it != currentIndex }.randomOrNull()?.let { currentIndex = it }
            repeatMode == RepeatMode.REPEAT_ALL -> currentIndex = (currentIndex - 1 + playlist.size) % playlist.size
            currentIndex > 0 -> currentIndex--
        }
    }

    fun addToPlaylist(track: MediaTrack) {
        if (playlist.none { it.file.absolutePath == track.file.absolutePath })
            playlist = playlist + track
    }

    fun removeFromPlaylist(index: Int) {
        playlist = playlist.toMutableList().also { it.removeAt(index) }
        if (currentIndex >= playlist.size) currentIndex = (playlist.size - 1).coerceAtLeast(0)
    }

    // Apply current speed to whichever engine is live
    fun applySpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                    }
                }
                // VideoView speed via MediaPlayer reference via reflection — set after play
            } catch (_: Exception) {}
        }
    }
}

@Composable
private fun rememberPlayerState() = remember { PlayerState() }

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@Composable
fun MediaPlayerScreen(
    isDark: Boolean,
    initialPath: String = ""
) {
    val ctx   = LocalContext.current
    val state = rememberPlayerState()
    val scope = rememberCoroutineScope()

    // Colour scheme
    val bg      = if (isDark) FTV.Bg          else FTV.LBg
    val surface = if (isDark) FTV.Surface      else FTV.LSurface
    val surfaceH= if (isDark) FTV.SurfaceHigh  else FTV.LSurfaceHigh
    val border  = if (isDark) FTV.Border       else FTV.LBorder
    val tc      = if (isDark) FTV.Text         else FTV.LText
    val tcs     = if (isDark) FTV.TextSec      else FTV.LTextSec
    val tcm     = if (isDark) FTV.TextMuted    else FTV.LTextMuted

    // ─────────────────────────────────────────────────────────────
    // FIX 1 — Two-pass library loading on IO dispatcher (fixes slow
    //          start and getImage logcat errors).
    // ─────────────────────────────────────────────────────────────
    LaunchedEffect(state.showHidden) {
        state.isLoading = true
        val allExts = VIDEO_EXTS + AUDIO_EXTS
        val roots = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ).filterNotNull()

        withContext(Dispatchers.IO) {
            // Collect file list
            val found = mutableListOf<File>()
            roots.forEach { root ->
                if (root.exists()) {
                    root.walkTopDown()
                        .maxDepth(6)
                        // FIX 3: skip hidden directories unless setting enabled
                        .onEnter { dir -> state.showHidden || !dir.isHidden }
                        .filter { f ->
                            f.isFile
                                    && (state.showHidden || !f.isHidden)   // FIX 3: skip hidden files
                                    && f.extension.lowercase() in allExts
                        }
                        .forEach { found.add(it) }
                }
            }

            val distinct = found.distinctBy { it.absolutePath }.sortedBy { it.nameWithoutExtension }

            // Pass 1 — fast: populate list immediately with filename-only stubs
            val quickTracks = distinct.map { MediaTrack(it) }
            withContext(Dispatchers.Main) {
                state.allTracks   = quickTracks
                state.videoTracks = quickTracks.filter { it.isVideo }
                state.audioTracks = quickTracks.filter { !it.isVideo }
                state.isLoading   = false

                if (initialPath.isNotEmpty()) {
                    val startFile = File(initialPath)
                    val idx = quickTracks.indexOfFirst { it.file.absolutePath == startFile.absolutePath }
                    if (idx >= 0) {
                        state.playlist     = quickTracks
                        state.currentIndex = idx
                        state.isPlaying    = true
                        state.activeTab    = if (quickTracks[idx].isVideo) MediaTab.VIDEOS else MediaTab.MUSIC
                    }
                }
            }

            // Pass 2 — enrich with metadata in background; album art loaded lazily
            val enriched = distinct.map { f ->
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(f.absolutePath)
                    MediaTrack(
                        file          = f,
                        title         = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)    ?: f.nameWithoutExtension,
                        artist        = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)   ?: "Unknown Artist",
                        album         = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)    ?: "Unknown Album",
                        durationMs    = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                        albumArtBytes = r.embeddedPicture,  // returns null cleanly — no exception
                        isVideo       = f.extension.lowercase() in VIDEO_EXTS
                    )
                } catch (_: Exception) { MediaTrack(f) }
                finally { r.release() }
            }

            withContext(Dispatchers.Main) {
                state.allTracks   = enriched
                state.videoTracks = enriched.filter { it.isVideo }
                state.audioTracks = enriched.filter { !it.isVideo }

                // Update playlist entries with enriched metadata
                if (state.playlist.isNotEmpty()) {
                    val enrichedMap = enriched.associateBy { it.file.absolutePath }
                    state.playlist = state.playlist.map { enrichedMap[it.file.absolutePath] ?: it }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Audio MediaPlayer lifecycle
    // ─────────────────────────────────────────────────────────────
    val track = state.currentTrack
    DisposableEffect(state.currentIndex, state.playlist.size) {
        state.mediaPlayer?.release()
        state.mediaPlayer = null
        if (track != null && !track.isVideo) {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(ctx, Uri.fromFile(track.file))
                mp.prepareAsync()
                mp.setOnPreparedListener { prepared ->
                    state.durationMs  = prepared.duration.toLong()
                    state.isBuffering = false
                    // Apply speed immediately after prepare
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && state.playbackSpeed != 1.0f) {
                        try { prepared.playbackParams = prepared.playbackParams.setSpeed(state.playbackSpeed) } catch (_: Exception) {}
                    }
                    if (state.isPlaying) prepared.start()
                }
                mp.setOnCompletionListener {
                    when (state.repeatMode) {
                        RepeatMode.REPEAT_ONE -> mp.start()
                        else -> state.skipNext()
                    }
                }
                state.isBuffering = true
            } catch (_: Exception) {}
            state.mediaPlayer = mp
        }
        onDispose { state.mediaPlayer?.release(); state.mediaPlayer = null }
    }

    // Volume sync
    LaunchedEffect(state.volume, state.isMuted) {
        val v = if (state.isMuted) 0f else state.volume
        state.mediaPlayer?.setVolume(v, v)
    }

    // Play/pause sync
    LaunchedEffect(state.isPlaying) {
        val mp = state.mediaPlayer ?: return@LaunchedEffect
        try {
            if (state.isPlaying && !mp.isPlaying) mp.start()
            else if (!state.isPlaying && mp.isPlaying) mp.pause()
        } catch (_: Exception) {}
    }

    // Speed sync (audio)
    LaunchedEffect(state.playbackSpeed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                state.mediaPlayer?.let { mp ->
                    val pp = PlaybackParams().setSpeed(state.playbackSpeed)
                    mp.playbackParams = pp
                }
            } catch (_: Exception) {}
        }
    }

    // Position polling
    LaunchedEffect(state.currentIndex, state.isPlaying) {
        while (true) {
            delay(250)
            try {
                val mp = state.mediaPlayer
                val vv = state.videoView
                state.positionMs = when {
                    mp != null && mp.isPlaying -> mp.currentPosition.toLong()
                    vv != null && vv.isPlaying -> vv.currentPosition.toLong()
                    else -> state.positionMs
                }
            } catch (_: Exception) {}
        }
    }

    // Auto-hide controls in fullscreen
    LaunchedEffect(state.showControls, state.isFullscreen) {
        if (state.isFullscreen && state.showControls) {
            delay(4000); state.showControls = false
        }
    }

    // ── NEW: Sleep timer countdown ──
    LaunchedEffect(state.sleepTimerActive) {
        if (!state.sleepTimerActive) return@LaunchedEffect
        while (state.sleepTimerSeconds > 0 && state.sleepTimerActive) {
            delay(1000)
            state.sleepTimerSeconds--
        }
        if (state.sleepTimerActive && state.sleepTimerSeconds == 0L) {
            // Timer expired — pause playback
            state.isPlaying       = false
            state.sleepTimerActive = false
            state.mediaPlayer?.pause()
            state.videoView?.pause()
        }
    }

    // ─── ROOT LAYOUT ───
    Box(Modifier.fillMaxSize().background(bg)) {
        if (state.isFullscreen && track?.isVideo == true) {
            FullscreenVideoView(state, tc, tcm, isDark)
        } else {
            if (state.showQueue) {
                Row(Modifier.fillMaxSize()) {
                    LibraryPane(state, isDark, surface, surfaceH, border, tc, tcs, tcm)
                    Divider(Modifier.fillMaxHeight().width(1.dp), color = border)
                    PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
                }
            } else {
                PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Fullscreen Video
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FullscreenVideoView(state: PlayerState, tc: Color, tcm: Color, isDark: Boolean) {
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            // FIX 4: gesture seek + volume in fullscreen
            .pointerInput(Unit) {
                var dragTotalX = 0f
                var dragTotalY = 0f
                detectDragGestures(
                    onDragStart = { dragTotalX = 0f; dragTotalY = 0f },
                    onDrag = { _, delta ->
                        dragTotalX += delta.x
                        dragTotalY += delta.y
                    },
                    onDragEnd = {
                        // Horizontal: seek
                        if (abs(dragTotalX) > abs(dragTotalY) && abs(dragTotalX) > 30f) {
                            val seekDelta = (dragTotalX / 10f * 1000).toLong()
                            val newPos = (state.positionMs + seekDelta).coerceIn(0L, state.durationMs)
                            state.mediaPlayer?.seekTo(newPos.toInt())
                            state.videoView?.seekTo(newPos.toInt())
                            state.positionMs = newPos
                        } else if (abs(dragTotalY) > 30f) {
                            // Vertical right-half: volume; left-half: brightness (handled via audio manager here)
                            val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val delta = (-dragTotalY / 200f).coerceIn(-1f, 1f)
                            state.volume = (state.volume + delta).coerceIn(0f, 1f)
                            am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (state.volume * maxVol).toInt().coerceIn(0, maxVol),
                                0
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { state.showControls = !state.showControls })
            }
    ) {
        // FIX 2: Video surface — re-sets URI in update block when track changes
        AndroidView(
            factory = { cx ->
                VideoView(cx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    state.currentTrack?.let { t ->
                        tag = t.file.absolutePath
                        setVideoURI(Uri.fromFile(t.file))
                    }
                    state.videoView = this
                    setOnPreparedListener { mp ->
                        state.durationMs = mp.duration.toLong()
                        if (state.isPlaying) start()
                    }
                    setOnCompletionListener { state.skipNext() }
                }
            },
            update = { vv ->
                val newPath = state.currentTrack?.file?.absolutePath
                if (newPath != null && vv.tag != newPath) {
                    vv.tag = newPath
                    vv.setVideoURI(Uri.fromFile(state.currentTrack!!.file))
                    vv.setOnPreparedListener { mp ->
                        state.durationMs = mp.duration.toLong()
                        if (state.isPlaying) vv.start()
                    }
                } else {
                    if (state.isPlaying && !vv.isPlaying) vv.start()
                    else if (!state.isPlaying && vv.isPlaying) vv.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay
        AnimatedVisibility(state.showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.85f), Color.Transparent)))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { state.isFullscreen = false }) {
                        Icon(Icons.Default.FullscreenExit, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.currentTrack?.title ?: "",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    // Speed chip in fullscreen
                    SpeedChip(state, Color.White.copy(0.7f), Color.White)
                    Spacer(Modifier.width(8.dp))
                    SleepTimerChip(state, Color.White.copy(0.7f), Color.White)
                }
                // Bottom controls
                Column(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    FsVideoProgress(state, Color.White, Color.White.copy(0.5f))
                    Spacer(Modifier.height(8.dp))
                    FsMainControls(state, Color.White, isFullscreen = true)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Library Pane (left sidebar)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LibraryPane(
    state: PlayerState,
    isDark: Boolean,
    surface: Color, surfaceH: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color
) {
    Column(Modifier.width(300.dp).fillMaxHeight().background(surface)) {
        // App header
        Row(
            Modifier.fillMaxWidth()
                .background(if (isDark) FTV.BgMid else FTV.LSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(32.dp).background(FTV.Accent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Movie, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Films & TV", color = tc, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Media Player", color = tcm, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            // Hidden files toggle
            IconButton(
                onClick = { state.showHidden = !state.showHidden },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (state.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (state.showHidden) "Hide hidden files" else "Show hidden files",
                    tint = if (state.showHidden) FTV.Accent else tcm,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Divider(color = border)

        // Tab bar
        Row(
            Modifier.fillMaxWidth().background(surfaceH).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                MediaTab.VIDEOS   to Icons.Default.Movie,
                MediaTab.MUSIC    to Icons.Default.MusicNote,
                MediaTab.PLAYLIST to Icons.Default.QueueMusic
            ).forEach { (tab, icon) ->
                val active = state.activeTab == tab
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                        .background(if (active) FTV.Accent else Color.Transparent)
                        .clickable { state.activeTab = tab }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (active) Color.White else tcm, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (tab) { MediaTab.VIDEOS -> "Videos"; MediaTab.MUSIC -> "Music"; MediaTab.PLAYLIST -> "Queue" },
                        color = if (active) Color.White else tcm,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Search
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = tcm, modifier = Modifier.size(14.dp))
            Box(Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) Text("Search…", color = tcm, fontSize = 12.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { state.searchQuery = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 12.sp),
                    cursorBrush = SolidColor(FTV.Accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.searchQuery.isNotEmpty()) {
                Icon(Icons.Default.Close, null, tint = tcm,
                    modifier = Modifier.size(13.dp).clickable { state.searchQuery = "" })
            }
        }

        // Stats / add-all row
        val count = state.filteredTracks.size
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (state.isLoading) {
                Text("Scanning…", color = tcm, fontSize = 10.sp)
            } else {
                Text(
                    "$count ${if (state.activeTab == MediaTab.VIDEOS) "videos" else if (state.activeTab == MediaTab.MUSIC) "tracks" else "in queue"}",
                    color = tcm, fontSize = 10.sp
                )
            }
            if (state.activeTab == MediaTab.MUSIC || state.activeTab == MediaTab.VIDEOS) {
                Text("Add all to queue", color = FTV.Accent, fontSize = 10.sp,
                    modifier = Modifier.clickable { state.filteredTracks.forEach { state.addToPlaylist(it) } })
            }
        }
        Divider(color = border.copy(alpha = 0.5f))

        // ── NEW: Track list with folder/album grouping ──
        val listState = rememberLazyListState()
        val tracks = state.filteredTracks
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            if (state.searchQuery.isNotEmpty()) {
                // Flat search results (no grouping)
                itemsIndexed(tracks) { idx, t ->
                    val isCurrentInPlaylist = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                    LibraryRow(t, isCurrentInPlaylist, state.isPlaying, isDark, tc, tcs, tcm, border,
                        onClick = {
                            if (state.activeTab == MediaTab.PLAYLIST) {
                                val pIdx = state.playlist.indexOf(t)
                                if (pIdx >= 0) { state.currentIndex = pIdx; state.isPlaying = true }
                            } else {
                                state.playlist = tracks; state.currentIndex = idx; state.isPlaying = true
                            }
                        },
                        onAddToQueue = { state.addToPlaylist(t) },
                        onRemoveFromQueue = { if (state.activeTab == MediaTab.PLAYLIST) state.removeFromPlaylist(idx) }
                    )
                }
            } else {
                // Grouped view
                when (state.activeTab) {
                    MediaTab.MUSIC -> {
                        state.audioGroups.forEach { (albumName, groupTracks) ->
                            stickyHeader(key = "album_$albumName") {
                                GroupHeader(albumName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    groupTracks.forEach { state.addToPlaylist(it) }
                                    if (state.playlist.isNotEmpty() && state.currentIndex < 0) {
                                        state.currentIndex = 0; state.isPlaying = true
                                    }
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.file.absolutePath }) { idx, t ->
                                val isCurrentInPlaylist = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                                LibraryRow(t, isCurrentInPlaylist, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = {
                                        state.playlist = groupTracks; state.currentIndex = idx; state.isPlaying = true
                                    },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onRemoveFromQueue = {}
                                )
                            }
                        }
                    }
                    MediaTab.VIDEOS -> {
                        state.videoGroups.forEach { (folderName, groupTracks) ->
                            stickyHeader(key = "folder_$folderName") {
                                GroupHeader(folderName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    groupTracks.forEach { state.addToPlaylist(it) }
                                    if (state.playlist.isNotEmpty() && state.currentIndex < 0) {
                                        state.currentIndex = 0; state.isPlaying = true
                                    }
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.file.absolutePath }) { idx, t ->
                                val isCurrentInPlaylist = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                                LibraryRow(t, isCurrentInPlaylist, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = {
                                        state.playlist = groupTracks; state.currentIndex = idx; state.isPlaying = true
                                    },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onRemoveFromQueue = {}
                                )
                            }
                        }
                    }
                    MediaTab.PLAYLIST -> {
                        itemsIndexed(state.playlist, key = { _, t -> t.file.absolutePath }) { idx, t ->
                            val isCurrentInPlaylist = state.currentIndex == idx
                            LibraryRow(t, isCurrentInPlaylist, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.currentIndex = idx; state.isPlaying = true },
                                onAddToQueue = {},
                                onRemoveFromQueue = { state.removeFromPlaylist(idx) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        // Mini now-playing at bottom of sidebar
        if (state.currentTrack != null) {
            Divider(color = border)
            MiniNowPlaying(state, isDark, tc, tcs, tcm)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Group header (albums / folders)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun GroupHeader(
    name: String,
    count: Int,
    isDark: Boolean,
    tc: Color, tcm: Color,
    surface: Color, surfaceH: Color,
    onPlayAll: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = tc, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count track${if (count != 1) "s" else ""}", color = tcm, fontSize = 10.sp)
        }
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(FTV.Accent.copy(0.15f)).clickable { onPlayAll() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, "Play all", tint = FTV.Accent, modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Library row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LibraryRow(
    track: MediaTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color, border: Color,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromQueue: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val rowBg by animateColorAsState(if (isActive) FTV.SelectedBg else Color.Transparent, label = "rowbg")

    Row(
        Modifier.fillMaxWidth().background(rowBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showActions = true }) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (track.albumArtBytes != null) {
                AsyncImage(
                    model = track.albumArtBytes, contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(5.dp))
                        .background(if (track.isVideo) FTV.VideoGreen.copy(0.15f) else FTV.AudioPurple.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && isPlaying) AnimatedEqualizer(FTV.Accent)
                    else Icon(
                        if (track.isVideo) Icons.Default.PlayCircle else Icons.Default.MusicNote,
                        null,
                        tint = if (isActive) FTV.Accent else (if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple).copy(0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(Modifier.weight(1f)) {
            Text(track.title,  color = if (isActive) FTV.Accent else tc,  fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
            Text(track.artist, color = tcs, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!track.isVideo && track.album != "Unknown Album") {
                Text(track.album, color = tcm, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatDuration(track.durationMs), color = tcm, fontSize = 10.sp)
            if (track.isVideo) {
                Box(Modifier.background(FTV.VideoGreen.copy(0.2f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text("VIDEO", color = FTV.VideoGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            containerColor = if (isDark) FTV.Surface else FTV.LSurface,
            shape = RoundedCornerShape(10.dp),
            title = { Text(track.title, color = tc, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    listOf(
                        Icons.Default.PlayArrow to "Play now"           to onClick,
                        Icons.Default.AddToQueue to "Add to queue"      to onAddToQueue,
                        Icons.Default.Remove     to "Remove from queue" to onRemoveFromQueue
                    ).forEach { (pair, action) ->
                        val (icon, label) = pair
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .clickable { action(); showActions = false }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(icon, null, tint = tc, modifier = Modifier.size(18.dp))
                            Text(label, color = tc, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun AnimatedEqualizer(color: Color) {
    val inf = rememberInfiniteTransition(label = "eq")
    val b1 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), androidx.compose.animation.core.RepeatMode.Reverse), label = "b1")
    val b2 by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(600), androidx.compose.animation.core.RepeatMode.Reverse), label = "b2")
    val b3 by inf.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(500), androidx.compose.animation.core.RepeatMode.Reverse), label = "b3")
    Row(Modifier.size(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(b1, b2, b3).forEach { h ->
            Box(Modifier.width(4.dp).fillMaxHeight(h).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun MiniNowPlaying(state: PlayerState, isDark: Boolean, tc: Color, tcs: Color, tcm: Color) {
    val track = state.currentTrack ?: return
    Row(
        Modifier.fillMaxWidth()
            .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                .background(if (track.isVideo) FTV.VideoGreen.copy(0.2f) else FTV.AudioPurple.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.albumArtBytes != null) AsyncImage(track.albumArtBytes, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(if (track.isVideo) Icons.Default.Movie else Icons.Default.MusicNote, null,
                tint = if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(track.title,  color = tc,  fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(track.artist, color = tcm, fontSize = 9.sp,  maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(
            onClick = {
                val mp = state.mediaPlayer; val vv = state.videoView
                when {
                    mp != null -> { if (state.isPlaying) { mp.pause(); state.isPlaying = false } else { mp.start(); state.isPlaying = true } }
                    vv != null -> { if (state.isPlaying) { vv.pause(); state.isPlaying = false } else { vv.start(); state.isPlaying = true } }
                }
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = FTV.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Player Pane
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PlayerPane(
    state: PlayerState,
    isDark: Boolean,
    bg: Color, surface: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color,
    ctx: android.content.Context
) {
    val track = state.currentTrack

    Column(Modifier.fillMaxSize().background(bg)) {
        // Top toolbar
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(surface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarBtn(Icons.Default.Menu, tc) { state.showQueue = !state.showQueue }
            Spacer(Modifier.width(4.dp))
            if (track != null) {
                Icon(if (track.isVideo) Icons.Default.Movie else Icons.Default.MusicNote, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
                Text(track.title, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                if (track.artist != "Unknown Artist") {
                    Text("—", color = tcm, fontSize = 11.sp)
                    Text(track.artist, color = tcs, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                }
            } else {
                Text("Films & TV", color = tcs, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            // Speed chip
            SpeedChip(state, tcm, tc)
            Spacer(Modifier.width(4.dp))
            // Sleep timer chip
            SleepTimerChip(state, tcm, tc)
            Spacer(Modifier.width(4.dp))
            VolumeControl(state, tc, tcm)
            Spacer(Modifier.width(4.dp))
            if (track?.isVideo == true) ToolbarBtn(Icons.Default.Fullscreen, tc) { state.isFullscreen = true }
        }
        Divider(color = border)

        // Main area
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(if (isDark) FTV.BgMid else FTV.LBg),
            contentAlignment = Alignment.Center
        ) {
            when {
                track == null    -> EmptyPlayerState(tc, tcm)
                track.isVideo    -> VideoPlayerArea(state, track, tc, tcm, ctx)
                else             -> AudioPlayerArea(state, track, isDark, tc, tcs, tcm)
            }
        }

        // Bottom controls
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(if (isDark) FTV.Bg else FTV.LBg, surface)))
                .padding(horizontal = 20.dp)
        ) {
            FsVideoProgress(state, tc, tcm)
            FsMainControls(state, tc, isFullscreen = false)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EmptyPlayerState(tc: Color, tcm: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(160.dp).background(FTV.Accent.copy(0.05f), CircleShape))
            Box(Modifier.size(120.dp).background(FTV.Accent.copy(0.08f), CircleShape))
            Box(Modifier.size(80.dp).background(FTV.Accent.copy(0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Movie, null, tint = FTV.Accent, modifier = Modifier.size(36.dp))
            }
        }
        Text("No media selected", color = tc, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Pick a video or song from the library", color = tcm, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────
// FIX 2 — Video Player Area (re-sets URI on track change)
// FIX 4 — Gesture seek (horizontal) + volume (vertical)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun VideoPlayerArea(
    state: PlayerState,
    track: MediaTrack,
    tc: Color, tcm: Color,
    ctx: android.content.Context
) {
    // Gesture overlay state
    var gestureLabel by remember { mutableStateOf("") }
    var showGestureLabel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            // Gesture seek + volume
            .pointerInput(Unit) {
                var totalX = 0f; var totalY = 0f
                detectDragGestures(
                    onDragStart = { totalX = 0f; totalY = 0f },
                    onDrag = { _, delta -> totalX += delta.x; totalY += delta.y },
                    onDragEnd = {
                        if (abs(totalX) > abs(totalY) && abs(totalX) > 20f) {
                            // Horizontal = seek
                            val seekMs = (totalX / 8f * 1000).toLong()
                            val newPos = (state.positionMs + seekMs).coerceIn(0L, state.durationMs)
                            state.mediaPlayer?.seekTo(newPos.toInt())
                            state.videoView?.seekTo(newPos.toInt())
                            state.positionMs = newPos
                            gestureLabel = if (seekMs > 0) "+${seekMs / 1000}s" else "${seekMs / 1000}s"
                            showGestureLabel = true
                            scope.launch { delay(1000); showGestureLabel = false }
                        } else if (abs(totalY) > 20f) {
                            // Vertical = volume
                            val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val delta = (-totalY / 300f).coerceIn(-1f, 1f)
                            state.volume = (state.volume + delta).coerceIn(0f, 1f)
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (state.volume * maxVol).toInt().coerceIn(0, maxVol), 0)
                            gestureLabel = "Vol ${(state.volume * 100).toInt()}%"
                            showGestureLabel = true
                            scope.launch { delay(1000); showGestureLabel = false }
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { state.showControls = !state.showControls }) }
    ) {
        // FIX 2: VideoView — re-sets URI when track changes via tag comparison
        AndroidView(
            factory = { cx ->
                VideoView(cx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    tag = track.file.absolutePath
                    setVideoURI(Uri.fromFile(track.file))
                    state.videoView = this
                    setOnPreparedListener { mp ->
                        state.durationMs = mp.duration.toLong()
                        state.isBuffering = false
                        if (state.isPlaying) start()
                    }
                    setOnCompletionListener { state.skipNext() }
                }
            },
            update = { vv ->
                val newPath = state.currentTrack?.file?.absolutePath
                if (newPath != null && vv.tag != newPath) {
                    // Track changed — reload URI
                    vv.tag = newPath
                    vv.setVideoURI(Uri.fromFile(state.currentTrack!!.file))
                    vv.setOnPreparedListener { mp ->
                        state.durationMs = mp.duration.toLong()
                        state.isBuffering = false
                        if (state.isPlaying) vv.start()
                    }
                    state.isBuffering = true
                } else {
                    if (state.isPlaying && !vv.isPlaying) vv.start()
                    else if (!state.isPlaying && vv.isPlaying) vv.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.isBuffering) {
            CircularProgressIndicator(color = FTV.Accent, modifier = Modifier.align(Alignment.Center))
        }

        // Gesture feedback label
        AnimatedVisibility(showGestureLabel, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
            Box(
                Modifier.background(Color.Black.copy(0.65f), RoundedCornerShape(8.dp)).padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(gestureLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Audio Player Area
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AudioPlayerArea(state: PlayerState, track: MediaTrack, isDark: Boolean, tc: Color, tcs: Color, tcm: Color) {
    Box(Modifier.fillMaxSize()) {
        if (track.albumArtBytes != null) {
            AsyncImage(
                model = track.albumArtBytes, contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.2f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                colors = listOf(FTV.AudioPurple.copy(0.3f), FTV.Accent.copy(0.15f), Color.Transparent),
                radius = 600f
            )))
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val artRotation by rememberInfiniteTransition(label = "rotate").animateFloat(
                0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "rot"
            )
            Box(
                Modifier.size(220.dp)
                    .rotate(if (state.isPlaying) artRotation else 0f)
                    .shadow(24.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = if (track.albumArtBytes != null)
                            SolidColor(Color.Transparent)
                        else
                            Brush.sweepGradient(listOf(FTV.AudioPurple, FTV.Accent, FTV.AccentGlow, FTV.AudioPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArtBytes != null) {
                    AsyncImage(track.albumArtBytes, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(90.dp))
                }
                Box(Modifier.size(24.dp).background(if (isDark) FTV.BgMid else FTV.LBg, CircleShape))
            }

            Spacer(Modifier.height(32.dp))

            Text(track.title,  color = tc,  fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(track.artist, color = tcs, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(track.album,  color = tcm, fontSize = 12.sp)

            Spacer(Modifier.height(24.dp))

            if (state.isPlaying) WaveformVisualizer() else Box(Modifier.height(32.dp))

            Spacer(Modifier.height(8.dp))

            val nextTrack = state.playlist.getOrNull(state.currentIndex + 1)
            if (nextTrack != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) FTV.Surface.copy(0.6f) else FTV.LSurface.copy(0.8f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("UP NEXT", color = FTV.Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(nextTrack.title,  color = tc,  fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(nextTrack.artist, color = tcm, fontSize = 11.sp, maxLines = 1)
                    Text(formatDuration(nextTrack.durationMs), color = tcm, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualizer() {
    val inf = rememberInfiniteTransition(label = "wave")
    val h0  by inf.animateFloat(0.15f, 0.70f, infiniteRepeatable(tween(380), androidx.compose.animation.core.RepeatMode.Reverse), label = "w0")
    val h1  by inf.animateFloat(0.40f, 0.90f, infiniteRepeatable(tween(440), androidx.compose.animation.core.RepeatMode.Reverse), label = "w1")
    val h2  by inf.animateFloat(0.25f, 0.80f, infiniteRepeatable(tween(510), androidx.compose.animation.core.RepeatMode.Reverse), label = "w2")
    val h3  by inf.animateFloat(0.55f, 1.00f, infiniteRepeatable(tween(470), androidx.compose.animation.core.RepeatMode.Reverse), label = "w3")
    val h4  by inf.animateFloat(0.20f, 0.65f, infiniteRepeatable(tween(390), androidx.compose.animation.core.RepeatMode.Reverse), label = "w4")
    val h5  by inf.animateFloat(0.45f, 0.85f, infiniteRepeatable(tween(530), androidx.compose.animation.core.RepeatMode.Reverse), label = "w5")
    val h6  by inf.animateFloat(0.30f, 0.75f, infiniteRepeatable(tween(420), androidx.compose.animation.core.RepeatMode.Reverse), label = "w6")
    val h7  by inf.animateFloat(0.60f, 0.95f, infiniteRepeatable(tween(490), androidx.compose.animation.core.RepeatMode.Reverse), label = "w7")
    val h8  by inf.animateFloat(0.15f, 0.60f, infiniteRepeatable(tween(360), androidx.compose.animation.core.RepeatMode.Reverse), label = "w8")
    val h9  by inf.animateFloat(0.35f, 0.88f, infiniteRepeatable(tween(540), androidx.compose.animation.core.RepeatMode.Reverse), label = "w9")
    val h10 by inf.animateFloat(0.50f, 1.00f, infiniteRepeatable(tween(410), androidx.compose.animation.core.RepeatMode.Reverse), label = "w10")
    val h11 by inf.animateFloat(0.25f, 0.72f, infiniteRepeatable(tween(460), androidx.compose.animation.core.RepeatMode.Reverse), label = "w11")
    val h12 by inf.animateFloat(0.40f, 0.82f, infiniteRepeatable(tween(520), androidx.compose.animation.core.RepeatMode.Reverse), label = "w12")
    val h13 by inf.animateFloat(0.18f, 0.68f, infiniteRepeatable(tween(430), androidx.compose.animation.core.RepeatMode.Reverse), label = "w13")
    val h14 by inf.animateFloat(0.55f, 0.92f, infiniteRepeatable(tween(370), androidx.compose.animation.core.RepeatMode.Reverse), label = "w14")
    val h15 by inf.animateFloat(0.30f, 0.78f, infiniteRepeatable(tween(500), androidx.compose.animation.core.RepeatMode.Reverse), label = "w15")
    val heights = listOf(h0,h1,h2,h3,h4,h5,h6,h7,h8,h9,h10,h11,h12,h13,h14,h15,
        h15,h14,h13,h12,h11,h10,h9,h8,h7,h6,h5,h4,h3,h2,h1,h0)

    val waveGradient = Brush.verticalGradient(listOf(FTV.AccentGlow, FTV.Accent))
    Row(
        Modifier.fillMaxWidth().height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(Modifier.weight(1f).fillMaxHeight(h).background(brush = waveGradient, shape = RoundedCornerShape(2.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Progress Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FsVideoProgress(state: PlayerState, tc: Color, tcm: Color) {
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.positionMs), color = tcm, fontSize = 11.sp)
            Text("-${formatDuration((state.durationMs - state.positionMs).coerceAtLeast(0))}", color = tcm, fontSize = 11.sp)
            Text(formatDuration(state.durationMs), color = tcm, fontSize = 11.sp)
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(tc.copy(0.15f))) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(
                    Brush.horizontalGradient(listOf(FTV.AccentDim, FTV.Accent, FTV.AccentGlow)),
                    RoundedCornerShape(2.dp)
                ))
            }
            Slider(
                value = progress,
                onValueChange = { frac ->
                    val newPos = (frac * state.durationMs).toLong()
                    state.mediaPlayer?.seekTo(newPos.toInt())
                    state.videoView?.seekTo(newPos.toInt())
                    state.positionMs = newPos
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main Controls Row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FsMainControls(state: PlayerState, tc: Color, isFullscreen: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlBtn(Icons.Default.Shuffle, if (state.isShuffle) FTV.Accent else tc.copy(0.5f), 22.dp) { state.isShuffle = !state.isShuffle }

        ControlBtn(Icons.Default.SkipPrevious, tc, 32.dp) { state.skipPrev() }

        ControlBtn(Icons.Default.Replay10, tc, 26.dp) {
            val newPos = (state.positionMs - 10000).coerceAtLeast(0)
            state.mediaPlayer?.seekTo(newPos.toInt()); state.videoView?.seekTo(newPos.toInt()); state.positionMs = newPos
        }

        // Play / Pause
        Box(
            Modifier.size(56.dp).shadow(8.dp, CircleShape).background(FTV.Accent, CircleShape)
                .clickable {
                    val mp = state.mediaPlayer; val vv = state.videoView
                    when {
                        mp != null -> { if (state.isPlaying) { mp.pause(); state.isPlaying = false } else { mp.start(); state.isPlaying = true } }
                        vv != null -> { if (state.isPlaying) { vv.pause(); state.isPlaying = false } else { vv.start(); state.isPlaying = true } }
                        else -> state.isPlaying = !state.isPlaying
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }

        ControlBtn(Icons.Default.Forward10, tc, 26.dp) {
            val newPos = (state.positionMs + 10000).coerceAtMost(state.durationMs)
            state.mediaPlayer?.seekTo(newPos.toInt()); state.videoView?.seekTo(newPos.toInt()); state.positionMs = newPos
        }

        ControlBtn(Icons.Default.SkipNext, tc, 32.dp) { state.skipNext() }

        ControlBtn(
            when (state.repeatMode) { RepeatMode.REPEAT_ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
            if (state.repeatMode != RepeatMode.OFF) FTV.Accent else tc.copy(0.5f), 22.dp
        ) { state.repeatMode = RepeatMode.values()[(state.repeatMode.ordinal + 1) % 3] }
    }
}

// ─────────────────────────────────────────────────────────────────
// Volume Control
// ─────────────────────────────────────────────────────────────────

@Composable
private fun VolumeControl(state: PlayerState, tc: Color, tcm: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { state.isMuted = !state.isMuted }, modifier = Modifier.size(32.dp)) {
            Icon(
                when { state.isMuted -> Icons.Default.VolumeOff; state.volume < 0.3f -> Icons.Default.VolumeMute; state.volume < 0.7f -> Icons.Default.VolumeDown; else -> Icons.Default.VolumeUp },
                null, tint = tc, modifier = Modifier.size(18.dp)
            )
        }
        Slider(
            value = if (state.isMuted) 0f else state.volume,
            onValueChange = { state.volume = it; state.isMuted = false },
            modifier = Modifier.width(80.dp).height(24.dp),
            colors = SliderDefaults.colors(thumbColor = FTV.Accent, activeTrackColor = FTV.Accent, inactiveTrackColor = tc.copy(0.2f))
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// NEW — Playback speed chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SpeedChip(state: PlayerState, bgTint: Color, tc: Color) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (state.playbackSpeed != 1.0f) FTV.Accent.copy(0.15f) else Color.Transparent)
                .clickable { showMenu = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                speedLabel(state.playbackSpeed),
                color = if (state.playbackSpeed != 1.0f) FTV.Accent else bgTint,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(if (bgTint == Color.White.copy(0.7f)) FTV.Surface else FTV.Surface)
        ) {
            SPEED_STEPS.forEach { speed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            speedLabel(speed),
                            color = if (state.playbackSpeed == speed) FTV.Accent else FTV.Text,
                            fontWeight = if (state.playbackSpeed == speed) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        state.playbackSpeed = speed
                        showMenu = false
                        // Apply immediately if audio is playing
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                state.mediaPlayer?.let { mp ->
                                    val pp = PlaybackParams().setSpeed(speed)
                                    if (mp.isPlaying) mp.playbackParams = pp
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    leadingIcon = {
                        if (state.playbackSpeed == speed) Icon(Icons.Default.Check, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
                        else Spacer(Modifier.size(14.dp))
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// NEW — Sleep timer chip
// ─────────────────────────────────────────────────────────────────

private val SLEEP_OPTIONS = listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hour")

@Composable
private fun SleepTimerChip(state: PlayerState, bgTint: Color, tc: Color) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (state.sleepTimerActive) FTV.Gold.copy(0.15f) else Color.Transparent)
                .clickable { showMenu = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Default.Bedtime,
                    null,
                    tint = if (state.sleepTimerActive) FTV.Gold else bgTint,
                    modifier = Modifier.size(14.dp)
                )
                if (state.sleepTimerActive) {
                    Text(
                        formatTimer(state.sleepTimerSeconds),
                        color = FTV.Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(FTV.Surface)
        ) {
            if (state.sleepTimerActive) {
                DropdownMenuItem(
                    text = { Text("Cancel timer", color = FTV.DangerRed, fontSize = 13.sp) },
                    onClick = {
                        state.sleepTimerActive  = false
                        state.sleepTimerSeconds = 0L
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = FTV.DangerRed, modifier = Modifier.size(14.dp)) }
                )
                Divider(color = FTV.Border)
            }
            SLEEP_OPTIONS.forEach { (minutes, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = FTV.Text, fontSize = 13.sp) },
                    onClick = {
                        state.sleepTimerSeconds = minutes * 60L
                        state.sleepTimerActive  = true
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Bedtime, null, tint = FTV.Gold, modifier = Modifier.size(14.dp)) }
                )
            }
        }
    }
}

// A local constant to avoid a "unresolved reference" if FTV.DangerRed isn't defined on the object
private val FTV.DangerRed get() = Color(0xFFD83B01)

// ─────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ControlBtn(icon: ImageVector, tint: Color, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun ToolbarBtn(icon: ImageVector, tc: Color, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(5.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tc, modifier = Modifier.size(18.dp))
    }
}
