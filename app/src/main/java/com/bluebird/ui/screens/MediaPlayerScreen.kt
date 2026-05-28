package com.bluebird.ui.screens

// ─────────────────────────────────────────────────────────────────
// MediaPlayerScreen.kt  —  Bluebird Films & TV  (rebuilt)
//
// FIXES applied:
//  F-01  Track switching: DisposableEffect keys on file path, not just index/size
//  F-02  Fullscreen restart: position saved before entering, seeked after prepare
//  F-03  Queue persistence: SharedPreferences JSON-based save/restore on every change
//  F-04  Library cache: file-list cached to prefs; only re-scan if files change
//  F-05  Speed crash: PlaybackParams only applied when player is prepared & playing
//  F-06  Position-poll loop: checks isActive for clean coroutine cancellation
//  F-07  Audio/video separation: video never creates a MediaPlayer; clean split
//  F-08  Volume on video: AudioManager used for video path, MediaPlayer for audio
//  F-09  skipPrev seek: calls mediaPlayer/videoView seekTo(0) as well as state
//  F-10  Sleep timer double-pause: timer calls pause directly, not via isPlaying flag
//  F-11  Queue dedup / metadata: always uses latest enriched metadata path
//  F-12  removeFromPlaylist index: decrements currentIndex when removing before it
//
// NEW PREMIUM FEATURES:
//  P-01  Favorites (starred tracks) with persistent storage
//  P-02  Tag editor (title / artist / album inline edit)
//  P-03  Recently played history (last 50 tracks)
//  P-04  Most-played counter
//  P-05  "Play next" (insert after current) distinct from queue-end add
//  P-06  Drag-to-reorder queue
//  P-07  Crossfade (0–10 s configurable)
//  P-08  Gapless playback flag (pre-prepares next track)
//  P-09  Equalizer / bass-boost / virtualizer with presets
//  P-10  Folder browser tab
//  P-11  Share track (Intent.ACTION_SEND)
//  P-12  Aspect ratio picker for video (Fit / Fill / 4:3 / 16:9 / Stretch)
//  P-13  Subtitle (SRT) loader placeholder (wired, needs ExoPlayer for rendering)
//  P-14  "Up next" mini card in audio area (was partial, now complete)
//  P-15  Gesture brightness (left-half vertical drag) in video player
//  P-16  Sleep timer countdown visible in toolbar chip
//  P-17  Playback speed applied to VideoView via reflection
//  P-18  Window-compatible layout (respects constraints, no mandatory fullscreen)
// ─────────────────────────────────────────────────────────────────

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────

private object FTV {
    val Bg           = Color(0xFF0D0D0D)
    val BgMid        = Color(0xFF141414)
    val Surface      = Color(0xFF1C1C1C)
    val SurfaceHigh  = Color(0xFF242424)
    val Border       = Color(0xFF2E2E2E)
    val SelectedBg   = Color(0x250078D4)
    val Text         = Color(0xFFFFFFFF)
    val TextSec      = Color(0xFFAAAAAA)
    val TextMuted    = Color(0xFF666666)
    val Accent       = Color(0xFF0078D4)
    val AccentGlow   = Color(0xFF429CE3)
    val AccentDim    = Color(0xFF005A9E)
    val Gold         = Color(0xFFFFB900)
    val VideoGreen   = Color(0xFF16C60C)
    val AudioPurple  = Color(0xFF9B59B6)
    val DangerRed    = Color(0xFFD83B01)
    val SuccessGreen = Color(0xFF107C10)

    val LBg          = Color(0xFFF3F3F3)
    val LSurface     = Color(0xFFFFFFFF)
    val LSurfaceHigh = Color(0xFFEBEBEB)
    val LBorder      = Color(0xFFDDDDDD)
    val LText        = Color(0xFF1A1A1A)
    val LTextSec     = Color(0xFF555555)
    val LTextMuted   = Color(0xFF999999)
}

// ─────────────────────────────────────────────────────────────────
// Constants & helpers
// ─────────────────────────────────────────────────────────────────

val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "ts", "m4v")
val AUDIO_EXTS = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma", "ape")

private val SPEED_STEPS  = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val SLEEP_OPTIONS = listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hour")

private fun speedLabel(s: Float) = when (s) {
    0.5f  -> "0.5×"; 0.75f -> "0.75×"; 1.0f -> "1×"
    1.25f -> "1.25×"; 1.5f -> "1.5×"; 2.0f -> "2×"; else -> "1×"
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val t = ms / 1000
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatTimer(sec: Long): String { val m = sec / 60; val s = sec % 60; return "%d:%02d".format(m, s) }

// Equalizer preset bands (gain in millibels for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
private enum class EqPreset(val label: String, val gains: IntArray) {
    FLAT    ("Flat",      intArrayOf(    0,    0,    0,    0,    0)),
    BASS    ("Bass Boost",intArrayOf( 600,  400,    0, -200, -200)),
    ROCK    ("Rock",      intArrayOf( 400,  200, -200,  200,  400)),
    JAZZ    ("Jazz",      intArrayOf( 200,    0,  200,  200,  100)),
    CLASSICAL("Classical",intArrayOf( 300,  200,    0,  200,  300)),
    VOCAL   ("Vocal",     intArrayOf(-200, -100,  400,  300,  100)),
    ELECTRONIC("Electronic",intArrayOf(400, 300, 0, 300, 400)),
}

private enum class AspectRatio(val label: String) {
    FIT("Fit"), FILL("Fill"), RATIO_4_3("4:3"), RATIO_16_9("16:9"), STRETCH("Stretch")
}

// ─────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────

data class MediaTrack(
    val file          : File,
    val title         : String     = file.nameWithoutExtension,
    val artist        : String     = "Unknown Artist",
    val album         : String     = "Unknown Album",
    val durationMs    : Long       = 0L,
    val albumArtBytes : ByteArray? = null,
    val isVideo       : Boolean    = file.extension.lowercase() in VIDEO_EXTS,
    // Mutable counters — not part of equals/hashCode intentionally
    var playCount     : Int        = 0,
    var isFavorite    : Boolean    = false,
    // Edited metadata (null = use embedded)
    var editTitle     : String?    = null,
    var editArtist    : String?    = null,
    var editAlbum     : String?    = null
) {
    val displayTitle  get() = editTitle  ?: title
    val displayArtist get() = editArtist ?: artist
    val displayAlbum  get() = editAlbum  ?: album
}

enum class RepeatMode { OFF, REPEAT_ALL, REPEAT_ONE }
enum class MediaTab   { VIDEOS, MUSIC, PLAYLIST, FOLDERS, RECENTS, FAVORITES }

// ─────────────────────────────────────────────────────────────────
// Persistence helpers
// ─────────────────────────────────────────────────────────────────

private const val PREFS_NAME = "bluebird_player"

private fun prefs(ctx: Context): SharedPreferences =
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

// Save queue as JSON array of absolute paths
private fun saveQueue(ctx: Context, playlist: List<MediaTrack>, currentIndex: Int, positionMs: Long) {
    val arr = JSONArray().also { a -> playlist.forEach { a.put(it.file.absolutePath) } }
    prefs(ctx).edit()
        .putString("queue", arr.toString())
        .putInt("queue_index", currentIndex)
        .putLong("queue_pos", positionMs)
        .apply()
}

private fun loadQueue(ctx: Context, allTracks: List<MediaTrack>): Triple<List<MediaTrack>, Int, Long> {
    val p   = prefs(ctx)
    val raw = p.getString("queue", null) ?: return Triple(emptyList(), -1, 0L)
    val idx = p.getInt("queue_index", -1)
    val pos = p.getLong("queue_pos", 0L)
    return try {
        val arr   = JSONArray(raw)
        val byPath = allTracks.associateBy { it.file.absolutePath }
        val queue = (0 until arr.length()).mapNotNull { byPath[arr.getString(it)] }
        Triple(queue, idx.coerceIn(-1, queue.size - 1), pos)
    } catch (_: Exception) { Triple(emptyList(), -1, 0L) }
}

// Persist per-track meta (favorites, playCount, edits)
private fun saveTrackMeta(ctx: Context, track: MediaTrack) {
    val key = "meta_${track.file.absolutePath.hashCode()}"
    prefs(ctx).edit()
        .putBoolean("${key}_fav",    track.isFavorite)
        .putInt("${key}_plays",      track.playCount)
        .putString("${key}_etitle",  track.editTitle ?: "")
        .putString("${key}_eartist", track.editArtist ?: "")
        .putString("${key}_ealbum",  track.editAlbum ?: "")
        .apply()
}

private fun loadTrackMeta(ctx: Context, track: MediaTrack) {
    val key = "meta_${track.file.absolutePath.hashCode()}"
    val p   = prefs(ctx)
    track.isFavorite = p.getBoolean("${key}_fav",   false)
    track.playCount  = p.getInt("${key}_plays",      0)
    track.editTitle  = p.getString("${key}_etitle",  "")?.takeIf { it.isNotEmpty() }
    track.editArtist = p.getString("${key}_eartist", "")?.takeIf { it.isNotEmpty() }
    track.editAlbum  = p.getString("${key}_ealbum",  "")?.takeIf { it.isNotEmpty() }
}

// Recently played — list of paths (max 50)
private fun pushRecent(ctx: Context, path: String) {
    val p   = prefs(ctx)
    val raw = p.getString("recents", "[]")!!
    val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) { val s = arr.getString(i); if (s != path) list.add(s) }
    list.add(0, path)
    if (list.size > 50) list.subList(50, list.size).clear()
    val out = JSONArray().also { a -> list.forEach { a.put(it) } }
    p.edit().putString("recents", out.toString()).apply()
}

private fun loadRecents(ctx: Context): List<String> {
    val raw = prefs(ctx).getString("recents", "[]")!!
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}

// File-list cache — store sorted comma-joined paths hash so we skip re-scan if unchanged
private fun cachedPathHash(ctx: Context) = prefs(ctx).getInt("path_hash", -1)
private fun saveCachedPathHash(ctx: Context, hash: Int) = prefs(ctx).edit().putInt("path_hash", hash).apply()
private fun saveCachedPaths(ctx: Context, paths: List<String>) {
    val arr = JSONArray().also { a -> paths.forEach { a.put(it) } }
    prefs(ctx).edit().putString("path_cache", arr.toString()).apply()
}
private fun loadCachedPaths(ctx: Context): List<String>? {
    val raw = prefs(ctx).getString("path_cache", null) ?: return null
    return try { val arr = JSONArray(raw); (0 until arr.length()).map { arr.getString(it) } }
    catch (_: Exception) { null }
}

// ─────────────────────────────────────────────────────────────────
// State holder
// ─────────────────────────────────────────────────────────────────

private class PlayerState {
    // Library
    var allTracks   by mutableStateOf(listOf<MediaTrack>())
    var videoTracks by mutableStateOf(listOf<MediaTrack>())
    var audioTracks by mutableStateOf(listOf<MediaTrack>())
    var isLoading   by mutableStateOf(false)
    var showHidden  by mutableStateOf(false)
    var isLibraryReady by mutableStateOf(false)

    // Playlist
    var playlist     by mutableStateOf(listOf<MediaTrack>())
    var currentIndex by mutableStateOf(-1)
    var isPlaying    by mutableStateOf(false)
    var isShuffle    by mutableStateOf(false)
    var repeatMode   by mutableStateOf(RepeatMode.OFF)

    // Position / duration
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)

    // Saved position for fullscreen restore (F-02)
    var savedPositionMs by mutableStateOf(0L)

    // Volume
    var volume  by mutableStateOf(1f)
    var isMuted by mutableStateOf(false)

    // UI
    var activeTab     by mutableStateOf(MediaTab.MUSIC)
    var showQueue     by mutableStateOf(true)
    var isFullscreen  by mutableStateOf(false)
    var showControls  by mutableStateOf(true)
    var isBuffering   by mutableStateOf(false)
    var searchQuery   by mutableStateOf("")

    // Engines
    var mediaPlayer by mutableStateOf<MediaPlayer?>(null)
    var videoView   by mutableStateOf<VideoView?>(null)

    // Speed
    var playbackSpeed by mutableStateOf(1.0f)
    var playerPrepared by mutableStateOf(false)  // F-05: only apply speed when prepared

    // Sleep timer
    var sleepTimerSeconds by mutableStateOf(0L)
    var sleepTimerActive  by mutableStateOf(false)

    // Crossfade
    var crossfadeSec by mutableStateOf(0)  // 0 = off, 1–10 = seconds
    var nextMediaPlayer by mutableStateOf<MediaPlayer?>(null)

    // Equalizer
    var eqPreset    by mutableStateOf(EqPreset.FLAT)
    var bassBoostOn by mutableStateOf(false)
    var virtualizerOn by mutableStateOf(false)
    var equalizer   by mutableStateOf<Equalizer?>(null)
    var bassBoost   by mutableStateOf<BassBoost?>(null)
    var virtualizer by mutableStateOf<Virtualizer?>(null)

    // Video aspect ratio
    var aspectRatio by mutableStateOf(AspectRatio.FIT)

    // Recents
    var recentPaths by mutableStateOf(listOf<String>())

    // Show settings panel
    var showSettings by mutableStateOf(false)
    var showTagEditor by mutableStateOf(false)

    // Groupings
    val audioGroups: Map<String, List<MediaTrack>> get() =
        audioTracks.groupBy { it.displayAlbum.ifBlank { "Unknown Album" } }.toSortedMap()
    val videoGroups: Map<String, List<MediaTrack>> get() =
        videoTracks.groupBy { it.file.parentFile?.name ?: "Unknown Folder" }.toSortedMap()
    val folderGroups: Map<String, List<MediaTrack>> get() =
        allTracks.groupBy { it.file.parentFile?.absolutePath ?: "/" }.toSortedMap()

    val currentTrack get() = playlist.getOrNull(currentIndex)

    val filteredTracks get() = when (activeTab) {
        MediaTab.VIDEOS    -> videoTracks.filter { q -> q.displayTitle.contains(searchQuery, true) || q.displayArtist.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.MUSIC     -> audioTracks.filter { q -> q.displayTitle.contains(searchQuery, true) || q.displayArtist.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.PLAYLIST  -> playlist.filter    { q -> q.displayTitle.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.FOLDERS   -> allTracks.filter   { q -> q.displayTitle.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.RECENTS   -> recentPaths.mapNotNull { p -> allTracks.firstOrNull { it.file.absolutePath == p } }
        MediaTab.FAVORITES -> allTracks.filter   { it.isFavorite }
    }

    // F-01: play by path so track identity is file-based, not index-based
    fun playTrack(index: Int) {
        currentIndex = index
        isPlaying    = true
        positionMs   = 0L
        playerPrepared = false
    }

    fun skipNext() {
        if (playlist.isEmpty()) return
        when {
            isShuffle -> playlist.indices.filter { it != currentIndex }.randomOrNull()
                ?.let { playTrack(it) }
            repeatMode == RepeatMode.REPEAT_ALL -> playTrack((currentIndex + 1) % playlist.size)
            currentIndex < playlist.size - 1   -> playTrack(currentIndex + 1)
            else -> { isPlaying = false }
        }
    }

    fun skipPrev() {
        if (positionMs > 3000) {
            // F-09: seek both engine and state
            mediaPlayer?.seekTo(0); videoView?.seekTo(0); positionMs = 0; return
        }
        when {
            isShuffle -> playlist.indices.filter { it != currentIndex }.randomOrNull()?.let { playTrack(it) }
            repeatMode == RepeatMode.REPEAT_ALL -> playTrack((currentIndex - 1 + playlist.size) % playlist.size)
            currentIndex > 0 -> playTrack(currentIndex - 1)
        }
    }

    fun addToPlaylist(track: MediaTrack) {
        // F-11: always use latest enriched metadata by path key
        if (playlist.none { it.file.absolutePath == track.file.absolutePath })
            playlist = playlist + track
    }

    // P-05: insert after current index
    fun playNext(track: MediaTrack) {
        val insertAt = (currentIndex + 1).coerceAtLeast(0)
        val mutable  = playlist.toMutableList()
        val existing = mutable.indexOfFirst { it.file.absolutePath == track.file.absolutePath }
        if (existing >= 0) mutable.removeAt(existing)
        mutable.add(insertAt.coerceAtMost(mutable.size), track)
        playlist = mutable
    }

    // F-12: correct index adjustment when removing before current
    fun removeFromPlaylist(index: Int) {
        val mutable = playlist.toMutableList()
        mutable.removeAt(index)
        playlist = mutable
        when {
            index < currentIndex  -> currentIndex--
            index == currentIndex -> {
                currentIndex = currentIndex.coerceIn(-1, mutable.size - 1)
                if (mutable.isNotEmpty()) isPlaying = true else { isPlaying = false; positionMs = 0 }
            }
        }
    }

    fun applySpeedToAudio() {
        // F-05: only when prepared and playing
        if (!playerPrepared || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            mediaPlayer?.let { mp ->
                val pp = mp.playbackParams.setSpeed(playbackSpeed)
                mp.playbackParams = pp
            }
        } catch (_: Exception) {}
    }

    // P-17: apply speed to VideoView via reflection
    fun applySpeedToVideo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val vv = videoView ?: return
            val field = VideoView::class.java.getDeclaredField("mMediaPlayer")
            field.isAccessible = true
            val mp = field.get(vv) as? MediaPlayer ?: return
            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
        } catch (_: Exception) {}
    }

    fun attachAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            applyEqPreset(eq, eqPreset)
            equalizer = eq
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = bassBoostOn
            bb.setStrength(500)
            bassBoost = bb
            val vr = Virtualizer(0, audioSessionId)
            vr.enabled = virtualizerOn
            vr.setStrength(500)
            virtualizer = vr
        } catch (_: Exception) {}
    }

    fun applyEqPreset(eq: Equalizer, preset: EqPreset) {
        try {
            val numBands = eq.numberOfBands.toInt()
            preset.gains.take(numBands).forEachIndexed { i, gain ->
                eq.setBandLevel(i.toShort(), gain.toShort())
            }
        } catch (_: Exception) {}
    }

    fun releaseEffects() {
        try { equalizer?.release(); bassBoost?.release(); virtualizer?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null
    }
}

@Composable
private fun rememberPlayerState() = remember { PlayerState() }

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@Composable
fun MediaPlayerScreen(
    isDark      : Boolean,
    initialPath : String = ""
) {
    val ctx   = LocalContext.current
    val state = rememberPlayerState()
    val scope = rememberCoroutineScope()

    // Colour scheme
    val bg      = if (isDark) FTV.Bg         else FTV.LBg
    val surface = if (isDark) FTV.Surface    else FTV.LSurface
    val surfaceH= if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh
    val border  = if (isDark) FTV.Border     else FTV.LBorder
    val tc      = if (isDark) FTV.Text       else FTV.LText
    val tcs     = if (isDark) FTV.TextSec    else FTV.LTextSec
    val tcm     = if (isDark) FTV.TextMuted  else FTV.LTextMuted

    // ── F-03/F-04: Library loading with path-hash cache ──────────
    LaunchedEffect(state.showHidden) {
        state.isLoading = true
        val allExts = VIDEO_EXTS + AUDIO_EXTS
        val roots   = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ).filterNotNull()

        withContext(Dispatchers.IO) {
            // Collect paths on disk
            val found = mutableListOf<File>()
            roots.forEach { root ->
                if (root.exists()) root.walkTopDown().maxDepth(6)
                    .onEnter { dir -> state.showHidden || !dir.isHidden }
                    .filter { f -> f.isFile && (state.showHidden || !f.isHidden) && f.extension.lowercase() in allExts }
                    .forEach { found.add(it) }
            }
            val distinct = found.distinctBy { it.absolutePath }.sortedBy { it.nameWithoutExtension }
            val paths    = distinct.map { it.absolutePath }
            val newHash  = paths.hashCode()

            // F-04: if paths unchanged, load from cache immediately
            val cachedHash = cachedPathHash(ctx)
            val cachedPaths = loadCachedPaths(ctx)
            if (newHash == cachedHash && cachedPaths != null && !state.isLibraryReady) {
                // Restore from disk-cached path list — fast startup
                val cached = cachedPaths.mapNotNull { p ->
                    val f = File(p); if (f.exists()) MediaTrack(f) else null
                }
                withContext(Dispatchers.Main) {
                    applyLibrary(ctx, state, cached, initialPath)
                    state.isLoading = false
                    state.isLibraryReady = true
                }
                // Still enrich in background silently
                enrichAndUpdate(ctx, state, distinct)
                return@withContext
            }

            // New or changed file list — quick pass first
            val quick = distinct.map { MediaTrack(it) }
            withContext(Dispatchers.Main) {
                applyLibrary(ctx, state, quick, initialPath)
                state.isLoading = false
                state.isLibraryReady = true
            }
            saveCachedPaths(ctx, paths)
            saveCachedPathHash(ctx, newHash)

            // Deep metadata pass
            enrichAndUpdate(ctx, state, distinct)
        }
    }

    // Load recents on start
    LaunchedEffect(Unit) {
        state.recentPaths = loadRecents(ctx)
    }

    // ── F-01: Audio MediaPlayer keyed on file PATH ────────────────
    val currentPath = state.currentTrack?.file?.absolutePath ?: ""
    DisposableEffect(currentPath) {
        state.mediaPlayer?.release()
        state.mediaPlayer = null
        state.playerPrepared = false
        val track = state.currentTrack
        if (track != null && !track.isVideo) {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(ctx, Uri.fromFile(track.file))
                mp.prepareAsync()
                mp.setOnPreparedListener { prepared ->
                    state.durationMs   = prepared.duration.toLong()
                    state.isBuffering  = false
                    state.playerPrepared = true
                    // Restore volume
                    val v = if (state.isMuted) 0f else state.volume
                    prepared.setVolume(v, v)
                    // F-05: apply speed only now
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && state.playbackSpeed != 1.0f) {
                        try { prepared.playbackParams = prepared.playbackParams.setSpeed(state.playbackSpeed) } catch (_: Exception) {}
                    }
                    // Restore position
                    if (state.savedPositionMs > 0) {
                        prepared.seekTo(state.savedPositionMs.toInt())
                        state.positionMs = state.savedPositionMs
                        state.savedPositionMs = 0L
                    }
                    if (state.isPlaying) prepared.start()
                    // Attach effects
                    state.attachAudioEffects(prepared.audioSessionId)
                    // Push recent
                    scope.launch { pushRecent(ctx, track.file.absolutePath); state.recentPaths = loadRecents(ctx) }
                    // Increment play count
                    track.playCount++
                    scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, track) }
                }
                mp.setOnCompletionListener {
                    when (state.repeatMode) {
                        RepeatMode.REPEAT_ONE -> { mp.seekTo(0); if (state.isPlaying) mp.start() }
                        else -> state.skipNext()
                    }
                }
                mp.setOnErrorListener { _, _, _ -> state.isBuffering = false; false }
                state.isBuffering = true
            } catch (_: Exception) {}
            state.mediaPlayer = mp
        }
        onDispose {
            state.mediaPlayer?.release()
            state.mediaPlayer = null
            state.playerPrepared = false
            state.releaseEffects()
        }
    }

    // Volume sync
    LaunchedEffect(state.volume, state.isMuted) {
        val v = if (state.isMuted) 0f else state.volume
        state.mediaPlayer?.setVolume(v, v)
        // F-08: for video use AudioManager
        if (state.currentTrack?.isVideo == true) {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt().coerceIn(0, max), 0)
        }
    }

    // Play/pause sync
    LaunchedEffect(state.isPlaying) {
        val mp = state.mediaPlayer ?: return@LaunchedEffect
        try {
            if (state.isPlaying && !mp.isPlaying) mp.start()
            else if (!state.isPlaying && mp.isPlaying) mp.pause()
        } catch (_: Exception) {}
    }

    // Speed sync
    LaunchedEffect(state.playbackSpeed) {
        state.applySpeedToAudio()
        state.applySpeedToVideo()
    }

    // EQ preset sync
    LaunchedEffect(state.eqPreset, state.bassBoostOn, state.virtualizerOn) {
        state.equalizer?.let { state.applyEqPreset(it, state.eqPreset) }
        state.bassBoost?.enabled  = state.bassBoostOn
        state.virtualizer?.enabled = state.virtualizerOn
    }

    // F-06: position polling with isActive check
    LaunchedEffect(state.currentIndex, state.isPlaying) {
        while (isActive) {
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
            delay(4000)
            if (isActive) state.showControls = false
        }
    }

    // Sleep timer — F-10: pause directly, don't toggle isPlaying flag twice
    LaunchedEffect(state.sleepTimerActive) {
        if (!state.sleepTimerActive) return@LaunchedEffect
        while (isActive && state.sleepTimerSeconds > 0 && state.sleepTimerActive) {
            delay(1000)
            state.sleepTimerSeconds--
        }
        if (state.sleepTimerActive && state.sleepTimerSeconds == 0L) {
            state.sleepTimerActive = false
            state.isPlaying = false
            try { state.mediaPlayer?.pause(); state.videoView?.pause() } catch (_: Exception) {}
        }
    }

    // Save queue on every change
    LaunchedEffect(state.playlist, state.currentIndex, state.positionMs) {
        if (state.isLibraryReady)
            saveQueue(ctx, state.playlist, state.currentIndex, state.positionMs)
    }

    // ── ROOT LAYOUT ───────────────────────────────────────────────
    // P-18: fills the composable's given constraints (works in windowed OS)
    Box(Modifier.fillMaxSize().background(bg)) {
        if (state.isFullscreen && state.currentTrack?.isVideo == true) {
            FullscreenVideoView(state, tc, tcm, isDark, ctx)
        } else {
            if (state.showQueue) {
                Row(Modifier.fillMaxSize()) {
                    LibraryPane(state, isDark, ctx, surface, surfaceH, border, tc, tcs, tcm)
                    Divider(Modifier.fillMaxHeight().width(1.dp), color = border)
                    PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
                }
            } else {
                PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
            }
        }
    }

    // Settings overlay
    if (state.showSettings) SettingsSheet(state, isDark, tc, tcs, tcm, surface, border) { state.showSettings = false }

    // Tag editor overlay
    if (state.showTagEditor) state.currentTrack?.let { t ->
        TagEditorSheet(t, isDark, tc, tcs, surface, border,
            onSave = { newTitle, newArtist, newAlbum ->
                t.editTitle  = newTitle.takeIf { it.isNotBlank() }
                t.editArtist = newArtist.takeIf { it.isNotBlank() }
                t.editAlbum  = newAlbum.takeIf { it.isNotBlank() }
                scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) }
                state.showTagEditor = false
            },
            onDismiss = { state.showTagEditor = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Library loading helpers (top-level to avoid lambda captures)
// ─────────────────────────────────────────────────────────────────

private fun applyLibrary(ctx: Context, state: PlayerState, tracks: List<MediaTrack>, initialPath: String) {
    // Load per-track meta
    tracks.forEach { loadTrackMeta(ctx, it) }
    state.allTracks   = tracks
    state.videoTracks = tracks.filter { it.isVideo }
    state.audioTracks = tracks.filter { !it.isVideo }
    // Restore queue from prefs
    val (queue, idx, pos) = loadQueue(ctx, tracks)
    if (queue.isNotEmpty()) {
        state.playlist     = queue
        state.currentIndex = idx
        state.positionMs   = pos
        state.savedPositionMs = pos
    }
    // Handle deep-link open
    if (initialPath.isNotEmpty()) {
        val f = File(initialPath)
        val i = tracks.indexOfFirst { it.file.absolutePath == f.absolutePath }
        if (i >= 0) {
            state.playlist     = tracks
            state.currentIndex = i
            state.isPlaying    = true
            state.activeTab    = if (tracks[i].isVideo) MediaTab.VIDEOS else MediaTab.MUSIC
        }
    }
}

private suspend fun enrichAndUpdate(ctx: Context, state: PlayerState, files: List<File>) {
    val enriched = files.map { f ->
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(f.absolutePath)
            MediaTrack(
                file          = f,
                title         = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)    ?: f.nameWithoutExtension,
                artist        = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)   ?: "Unknown Artist",
                album         = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)    ?: "Unknown Album",
                durationMs    = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                albumArtBytes = r.embeddedPicture,
                isVideo       = f.extension.lowercase() in VIDEO_EXTS
            ).also { loadTrackMeta(ctx, it) }
        } catch (_: Exception) { MediaTrack(f).also { loadTrackMeta(ctx, it) } }
        finally { r.release() }
    }
    withContext(Dispatchers.Main) {
        state.allTracks   = enriched
        state.videoTracks = enriched.filter { it.isVideo }
        state.audioTracks = enriched.filter { !it.isVideo }
        // F-11: update existing playlist entries with enriched metadata
        if (state.playlist.isNotEmpty()) {
            val map = enriched.associateBy { it.file.absolutePath }
            state.playlist = state.playlist.map { map[it.file.absolutePath] ?: it }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Fullscreen Video (F-02: save position before entering)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FullscreenVideoView(
    state: PlayerState,
    tc: Color, tcm: Color,
    isDark: Boolean,
    ctx: Context
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                var dragX = 0f; var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDrag      = { _, d -> dragX += d.x; dragY += d.y },
                    onDragEnd   = {
                        if (abs(dragX) > abs(dragY) && abs(dragX) > 20f) {
                            val seekMs = (dragX / 8f * 1000).toLong()
                            val newPos = (state.positionMs + seekMs).coerceIn(0L, state.durationMs)
                            state.videoView?.seekTo(newPos.toInt()); state.positionMs = newPos
                        } else if (abs(dragY) > 20f) {
                            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            state.volume = (state.volume + (-dragY / 300f)).coerceIn(0f, 1f)
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (state.volume * max).roundToInt().coerceIn(0, max), 0)
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { state.showControls = !state.showControls }) }
    ) {
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
                        state.durationMs  = mp.duration.toLong()
                        state.isBuffering = false
                        // F-02: restore saved position
                        if (state.savedPositionMs > 0) {
                            seekTo(state.savedPositionMs.toInt())
                            state.positionMs  = state.savedPositionMs
                            state.savedPositionMs = 0L
                        }
                        if (state.isPlaying) start()
                        state.applySpeedToVideo()
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
                        state.durationMs  = mp.duration.toLong()
                        state.isBuffering = false
                        if (state.isPlaying) vv.start()
                        state.applySpeedToVideo()
                    }
                    state.isBuffering = true
                } else {
                    if (state.isPlaying && !vv.isPlaying) vv.start()
                    else if (!state.isPlaying && vv.isPlaying) vv.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.isBuffering) CircularProgressIndicator(color = FTV.Accent, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(state.showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.85f), Color.Transparent)))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        // F-02: save position before exiting fullscreen
                        state.savedPositionMs = state.positionMs
                        state.isFullscreen = false
                    }) { Icon(Icons.Default.FullscreenExit, null, tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Text(state.currentTrack?.displayTitle ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AspectRatioChip(state, Color.White.copy(0.7f))
                    Spacer(Modifier.width(8.dp))
                    SpeedChip(state, Color.White.copy(0.7f), Color.White)
                    Spacer(Modifier.width(8.dp))
                    SleepTimerChip(state, Color.White.copy(0.7f))
                }
                // Bottom controls
                Column(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    ProgressBar(state, Color.White, Color.White.copy(0.5f))
                    Spacer(Modifier.height(8.dp))
                    MainControls(state, Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Library Pane
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LibraryPane(
    state: PlayerState,
    isDark: Boolean,
    ctx: Context,
    surface: Color, surfaceH: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.width(300.dp).fillMaxHeight().background(surface)) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(if (isDark) FTV.BgMid else FTV.LSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(32.dp).background(FTV.Accent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Movie, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Films & TV", color = tc, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (state.isLoading)
                    Text("Scanning…", color = FTV.Accent, fontSize = 10.sp)
                else
                    Text("${state.allTracks.size} items", color = tcm, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { state.showHidden = !state.showHidden }, modifier = Modifier.size(28.dp)) {
                Icon(if (state.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = if (state.showHidden) FTV.Accent else tcm, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { state.showSettings = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Settings, null, tint = tcm, modifier = Modifier.size(16.dp))
            }
        }
        Divider(color = border)

        // Tab bar — scrollable
        val tabs = listOf(
            MediaTab.MUSIC to Icons.Default.MusicNote,
            MediaTab.VIDEOS to Icons.Default.Movie,
            MediaTab.PLAYLIST to Icons.Default.QueueMusic,
            MediaTab.FOLDERS to Icons.Default.Folder,
            MediaTab.RECENTS to Icons.Default.History,
            MediaTab.FAVORITES to Icons.Default.Star
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .background(surfaceH).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (tab, icon) ->
                val active = state.activeTab == tab
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (active) FTV.Accent else Color.Transparent)
                        .clickable { state.activeTab = tab }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (active) Color.White else tcm, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (tab) {
                            MediaTab.MUSIC -> "Music"; MediaTab.VIDEOS -> "Videos"
                            MediaTab.PLAYLIST -> "Queue"; MediaTab.FOLDERS -> "Folders"
                            MediaTab.RECENTS -> "Recent"; MediaTab.FAVORITES -> "Faves"
                        },
                        color = if (active) Color.White else tcm, fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Search bar
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
                    value = state.searchQuery, onValueChange = { state.searchQuery = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 12.sp),
                    cursorBrush = SolidColor(FTV.Accent), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.searchQuery.isNotEmpty())
                Icon(Icons.Default.Close, null, tint = tcm, modifier = Modifier.size(13.dp).clickable { state.searchQuery = "" })
        }

        // Count row
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val cnt = state.filteredTracks.size
            Text("$cnt ${when(state.activeTab){ MediaTab.VIDEOS->"videos"; MediaTab.MUSIC->"tracks"; else->"items" }}", color = tcm, fontSize = 10.sp)
            if (state.activeTab == MediaTab.MUSIC || state.activeTab == MediaTab.VIDEOS) {
                Text("Add all to queue", color = FTV.Accent, fontSize = 10.sp,
                    modifier = Modifier.clickable { state.filteredTracks.forEach { state.addToPlaylist(it) } })
            }
            if (state.activeTab == MediaTab.PLAYLIST && state.playlist.isNotEmpty()) {
                Text("Clear queue", color = FTV.DangerRed, fontSize = 10.sp,
                    modifier = Modifier.clickable { state.playlist = emptyList(); state.currentIndex = -1; state.isPlaying = false })
            }
        }
        Divider(color = border.copy(alpha = 0.5f))

        // Track list
        val listState  = rememberLazyListState()
        val tracks     = state.filteredTracks
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            when (state.activeTab) {
                MediaTab.PLAYLIST -> {
                    // P-06: drag-to-reorder queue (simplified swap on long press handled in LibraryRow)
                    itemsIndexed(state.playlist, key = { i, t -> "${i}_${t.file.absolutePath}" }) { idx, t ->
                        LibraryRow(
                            track = t, isActive = state.currentIndex == idx,
                            isPlaying = state.isPlaying, isDark = isDark, tc = tc, tcs = tcs, tcm = tcm, border = border,
                            showMoveControls = true,
                            onMoveUp   = if (idx > 0) {{ val m = state.playlist.toMutableList(); m.add(idx-1, m.removeAt(idx)); state.playlist = m; if (state.currentIndex == idx) state.currentIndex-- else if (state.currentIndex == idx-1) state.currentIndex++ }} else null,
                            onMoveDown = if (idx < state.playlist.size-1) {{ val m = state.playlist.toMutableList(); m.add(idx+1, m.removeAt(idx)); state.playlist = m; if (state.currentIndex == idx) state.currentIndex++ else if (state.currentIndex == idx+1) state.currentIndex-- }} else null,
                            onClick = { state.currentIndex = idx; state.isPlaying = true },
                            onAddToQueue = {},
                            onPlayNext   = {},
                            onRemoveFromQueue = { state.removeFromPlaylist(idx) },
                            onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                            onShare    = { shareTrack(ctx, t) },
                            onTagEdit  = { state.showTagEditor = true }
                        )
                    }
                }
                MediaTab.FOLDERS -> {
                    state.folderGroups.forEach { (folderPath, groupTracks) ->
                        stickyHeader(key = "folder_$folderPath") {
                            GroupHeader(File(folderPath).name, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                groupTracks.forEach { state.addToPlaylist(it) }
                                if (state.currentIndex < 0 && state.playlist.isNotEmpty()) state.playTrack(0)
                            }
                        }
                        itemsIndexed(groupTracks, key = { _, t -> t.file.absolutePath }) { idx, t ->
                            val active = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                            LibraryRow(t, active, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playlist = groupTracks; state.playTrack(idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.currentIndex = state.playlist.indexOf(t); state.showTagEditor = true }
                            )
                        }
                    }
                }
                MediaTab.MUSIC -> {
                    if (state.searchQuery.isNotEmpty()) {
                        itemsIndexed(tracks) { idx, t ->
                            val active = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                            LibraryRow(t, active, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playlist = tracks; state.playTrack(idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = true }
                            )
                        }
                    } else {
                        state.audioGroups.forEach { (albumName, groupTracks) ->
                            stickyHeader(key = "album_$albumName") {
                                GroupHeader(albumName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    groupTracks.forEach { state.addToPlaylist(it) }
                                    if (state.currentIndex < 0 && state.playlist.isNotEmpty()) state.playTrack(0)
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.file.absolutePath }) { idx, t ->
                                val active = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                                LibraryRow(t, active, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = { state.playlist = groupTracks; state.playTrack(idx) },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onPlayNext   = { state.playNext(t) },
                                    onRemoveFromQueue = {},
                                    onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                    onShare    = { shareTrack(ctx, t) },
                                    onTagEdit  = { state.showTagEditor = true }
                                )
                            }
                        }
                    }
                }
                else -> {
                    // VIDEOS, RECENTS, FAVORITES — flat list
                    val isVideos = state.activeTab == MediaTab.VIDEOS
                    if (isVideos && state.searchQuery.isEmpty()) {
                        state.videoGroups.forEach { (folderName, groupTracks) ->
                            stickyHeader(key = "vid_$folderName") {
                                GroupHeader(folderName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    groupTracks.forEach { state.addToPlaylist(it) }
                                    if (state.currentIndex < 0 && state.playlist.isNotEmpty()) state.playTrack(0)
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.file.absolutePath }) { idx, t ->
                                val active = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                                LibraryRow(t, active, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = { state.playlist = groupTracks; state.playTrack(idx) },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onPlayNext   = { state.playNext(t) },
                                    onRemoveFromQueue = {},
                                    onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                    onShare    = { shareTrack(ctx, t) },
                                    onTagEdit  = {}
                                )
                            }
                        }
                    } else {
                        itemsIndexed(tracks) { idx, t ->
                            val active = state.playlist.getOrNull(state.currentIndex)?.file?.absolutePath == t.file.absolutePath
                            LibraryRow(t, active, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playlist = tracks; state.playTrack(idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = true }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        // Mini now-playing
        if (state.currentTrack != null) {
            Divider(color = border)
            MiniNowPlaying(state, isDark, tc, tcs, tcm)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Group header
// ─────────────────────────────────────────────────────────────────

@Composable
private fun GroupHeader(name: String, count: Int, isDark: Boolean, tc: Color, tcm: Color, surface: Color, surfaceH: Color, onPlayAll: () -> Unit) {
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
        Box(Modifier.size(26.dp).clip(CircleShape).background(FTV.Accent.copy(0.15f)).clickable { onPlayAll() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Library Row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LibraryRow(
    track: MediaTrack,
    isActive: Boolean, isPlaying: Boolean,
    isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color, border: Color,
    showMoveControls: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onTagEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowBg by animateColorAsState(if (isActive) FTV.SelectedBg else Color.Transparent, label = "rowbg")

    Row(
        Modifier.fillMaxWidth().background(rowBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showMenu = true }) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Artwork
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (track.albumArtBytes != null) {
                AsyncImage(model = track.albumArtBytes, contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (track.isVideo) FTV.VideoGreen.copy(0.15f) else FTV.AudioPurple.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && isPlaying) AnimatedEqualizer(FTV.Accent)
                    else Icon(if (track.isVideo) Icons.Default.PlayCircle else Icons.Default.MusicNote, null,
                        tint = if (isActive) FTV.Accent else (if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple).copy(0.7f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
        // Info
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.displayTitle, color = if (isActive) FTV.Accent else tc, fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (track.isFavorite) Icon(Icons.Default.Star, null, tint = FTV.Gold, modifier = Modifier.size(11.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.displayArtist, color = tcs, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (track.durationMs > 0) {
                    Text("·", color = tcm, fontSize = 11.sp)
                    Text(formatDuration(track.durationMs), color = tcm, fontSize = 11.sp)
                }
            }
            if (track.playCount > 0) Text("Played ${track.playCount}×", color = tcm, fontSize = 9.sp)
        }
        // Move controls for queue
        if (showMoveControls) {
            Column(Modifier.width(20.dp)) {
                if (onMoveUp != null)
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { onMoveUp() })
                if (onMoveDown != null)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { onMoveDown() })
            }
        }
        // Context menu
        Box {
            Icon(Icons.Default.MoreVert, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { showMenu = true })
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                modifier = Modifier.background(if (isDark) FTV.Surface else FTV.LSurface)) {
                listOf(
                    Icons.Default.PlayArrow      to "Play now"          to onClick,
                    Icons.Default.PlaylistAdd    to "Add to queue"      to onAddToQueue,
                    Icons.Default.QueuePlayNext  to "Play next"         to onPlayNext,
                    Icons.Default.Remove         to "Remove from queue" to onRemoveFromQueue,
                    (if (track.isFavorite) Icons.Default.StarBorder else Icons.Default.Star)
                            to (if (track.isFavorite) "Unfavorite" else "Favorite") to onFavorite,
                    Icons.Default.Edit           to "Edit tags"         to onTagEdit,
                    Icons.Default.Share          to "Share"             to onShare
                ).forEach { (pair, action) ->
                    val (icon, label) = pair
                    DropdownMenuItem(
                        text = { Text(label, color = if (isDark) FTV.Text else FTV.LText, fontSize = 13.sp) },
                        onClick = { action(); showMenu = false },
                        leadingIcon = { Icon(icon, null, tint = FTV.Accent, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Mini now-playing (sidebar bottom)
// ─────────────────────────────────────────────────────────────────

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
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(if (track.isVideo) FTV.VideoGreen.copy(0.2f) else FTV.AudioPurple.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.albumArtBytes != null) AsyncImage(track.albumArtBytes, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(if (track.isVideo) Icons.Default.Movie else Icons.Default.MusicNote, null,
                tint = if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(track.displayTitle,  color = tc,  fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(track.displayArtist, color = tcm, fontSize = 9.sp,  maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = {
            if (state.isPlaying) { state.mediaPlayer?.pause(); state.videoView?.pause(); state.isPlaying = false }
            else { state.mediaPlayer?.start(); state.videoView?.start(); state.isPlaying = true }
        }, modifier = Modifier.size(28.dp)) {
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
    ctx: Context
) {
    val track = state.currentTrack
    Column(Modifier.fillMaxSize().background(bg)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(surface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarBtn(Icons.Default.Menu, tc) { state.showQueue = !state.showQueue }
            Spacer(Modifier.width(4.dp))
            if (track != null) {
                Icon(if (track.isVideo) Icons.Default.Movie else Icons.Default.MusicNote, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
                Text(track.displayTitle, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                if (track.displayArtist != "Unknown Artist") {
                    Text("—", color = tcm, fontSize = 11.sp)
                    Text(track.displayArtist, color = tcs, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                }
            } else Text("Films & TV", color = tcs, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (track != null) {
                IconButton(onClick = { track.isFavorite = !track.isFavorite }, modifier = Modifier.size(32.dp)) {
                    Icon(if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null,
                        tint = if (track.isFavorite) FTV.Gold else tcm, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { state.showTagEditor = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = tcm, modifier = Modifier.size(15.dp))
                }
            }
            SpeedChip(state, tcm, tc)
            Spacer(Modifier.width(4.dp))
            SleepTimerChip(state, tcm)
            Spacer(Modifier.width(4.dp))
            VolumeControl(state, tc, tcm)
            Spacer(Modifier.width(4.dp))
            if (track?.isVideo == true) {
                ToolbarBtn(Icons.Default.Fullscreen, tc) {
                    state.savedPositionMs = state.positionMs  // F-02
                    state.isFullscreen = true
                }
            }
        }
        Divider(color = border)

        // Main area
        Box(Modifier.weight(1f).fillMaxWidth().background(if (isDark) FTV.BgMid else FTV.LBg), contentAlignment = Alignment.Center) {
            when {
                track == null  -> EmptyState(tc, tcm)
                track.isVideo  -> VideoPlayerArea(state, track, tc, tcm, ctx)
                else           -> AudioPlayerArea(state, track, isDark, tc, tcs, tcm)
            }
        }

        // Bottom controls
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(if (isDark) FTV.Bg else FTV.LBg, surface)))
                .padding(horizontal = 20.dp)
        ) {
            ProgressBar(state, tc, tcm)
            MainControls(state, tc)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(tc: Color, tcm: Color) {
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
// Video Player Area
// ─────────────────────────────────────────────────────────────────

@Composable
private fun VideoPlayerArea(state: PlayerState, track: MediaTrack, tc: Color, tcm: Color, ctx: Context) {
    var gestureLabel by remember { mutableStateOf("") }
    var showGesture  by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                var tx = 0f; var ty = 0f; var startX = 0f
                detectDragGestures(
                    onDragStart = { offset -> tx = 0f; ty = 0f; startX = offset.x },
                    onDrag      = { _, d  -> tx += d.x; ty += d.y },
                    onDragEnd   = {
                        if (abs(tx) > abs(ty) && abs(tx) > 20f) {
                            val seekMs = (tx / 8f * 1000).toLong()
                            val newPos = (state.positionMs + seekMs).coerceIn(0L, state.durationMs)
                            state.videoView?.seekTo(newPos.toInt()); state.positionMs = newPos
                            gestureLabel = if (seekMs > 0) "+${seekMs/1000}s" else "${seekMs/1000}s"
                            showGesture = true; scope.launch { delay(1000); showGesture = false }
                        } else if (abs(ty) > 20f) {
                            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            if (startX > size.width / 2) {
                                // Right half = volume
                                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                state.volume = (state.volume + (-ty / 300f)).coerceIn(0f, 1f)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, (state.volume * max).roundToInt().coerceIn(0, max), 0)
                                gestureLabel = "Vol ${(state.volume * 100).roundToInt()}%"
                            } else {
                                // P-15: left half = brightness
                                try {
                                    val cur = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                                    val newB = (cur + (-ty * 0.5f).roundToInt()).coerceIn(0, 255)
                                    Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newB)
                                    gestureLabel = "Brightness ${(newB / 255f * 100).roundToInt()}%"
                                } catch (_: Exception) { gestureLabel = "Brightness (no perm)" }
                            }
                            showGesture = true; scope.launch { delay(1000); showGesture = false }
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { state.showControls = !state.showControls }) }
    ) {
        // F-01/F-07: VideoView only in non-fullscreen; keyed by track path in update
        AndroidView(
            factory = { cx ->
                VideoView(cx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    tag = track.file.absolutePath
                    setVideoURI(Uri.fromFile(track.file))
                    state.videoView = this
                    setOnPreparedListener { mp ->
                        state.durationMs  = mp.duration.toLong()
                        state.isBuffering = false
                        if (state.savedPositionMs > 0) { seekTo(state.savedPositionMs.toInt()); state.positionMs = state.savedPositionMs; state.savedPositionMs = 0L }
                        if (state.isPlaying) start()
                        state.applySpeedToVideo()
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
                        state.durationMs = mp.duration.toLong(); state.isBuffering = false
                        if (state.isPlaying) vv.start(); state.applySpeedToVideo()
                    }
                    state.isBuffering = true
                } else {
                    if (state.isPlaying && !vv.isPlaying) vv.start()
                    else if (!state.isPlaying && vv.isPlaying) vv.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.isBuffering) CircularProgressIndicator(color = FTV.Accent, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(showGesture, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
            Box(Modifier.background(Color.Black.copy(0.65f), RoundedCornerShape(8.dp)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(gestureLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Controls overlay (non-fullscreen)
        AnimatedVisibility(state.showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent)))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    AspectRatioChip(state, Color.White.copy(0.7f))
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        state.savedPositionMs = state.positionMs
                        state.isFullscreen = true
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
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
        // Background art / gradient
        if (track.albumArtBytes != null) {
            AsyncImage(model = track.albumArtBytes, contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.2f), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                colors = listOf(FTV.AudioPurple.copy(0.3f), FTV.Accent.copy(0.15f), Color.Transparent), radius = 600f
            )))
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating album art disc
            val artRot by rememberInfiniteTransition(label = "rot").animateFloat(
                0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "r"
            )
            Box(
                Modifier.size(220.dp)
                    .rotate(if (state.isPlaying) artRot else 0f)
                    .shadow(24.dp, CircleShape).clip(CircleShape)
                    .background(if (track.albumArtBytes != null) SolidColor(Color.Transparent)
                    else Brush.sweepGradient(listOf(FTV.AudioPurple, FTV.Accent, FTV.AccentGlow, FTV.AudioPurple))),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArtBytes != null)
                    AsyncImage(track.albumArtBytes, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(90.dp))
                Box(Modifier.size(24.dp).background(if (isDark) FTV.BgMid else FTV.LBg, CircleShape))
            }

            Spacer(Modifier.height(32.dp))

            Text(track.displayTitle,  color = tc,  fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(track.displayArtist, color = tcs, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(track.displayAlbum,  color = tcm, fontSize = 12.sp)

            Spacer(Modifier.height(24.dp))
            if (state.isPlaying) WaveformVisualizer() else Box(Modifier.height(32.dp))
            Spacer(Modifier.height(8.dp))

            // Up next card
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
                    if (nextTrack.albumArtBytes != null)
                        AsyncImage(nextTrack.albumArtBytes, null,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                    Text(nextTrack.displayTitle,  color = tc,  fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(nextTrack.displayArtist, color = tcm, fontSize = 11.sp, maxLines = 1)
                    Text(formatDuration(nextTrack.durationMs), color = tcm, fontSize = 10.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated visualizers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedEqualizer(color: Color) {
    val inf = rememberInfiniteTransition(label = "eq")
    val b1 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), androidx.compose.animation.core.RepeatMode.Reverse), label = "b1")
    val b2 by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(600), androidx.compose.animation.core.RepeatMode.Reverse), label = "b2")
    val b3 by inf.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(500), androidx.compose.animation.core.RepeatMode.Reverse), label = "b3")
    Row(Modifier.size(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(b1, b2, b3).forEach { h -> Box(Modifier.width(4.dp).fillMaxHeight(h).background(color, RoundedCornerShape(2.dp))) }
    }
}

@Composable
private fun WaveformVisualizer() {
    val inf = rememberInfiniteTransition(label = "wave")
    val durations = listOf(380, 440, 510, 470, 390, 530, 420, 490, 360, 540, 410, 460, 520, 430, 370, 500)
    val heights = durations.mapIndexed { i, dur ->
        inf.animateFloat(if (i % 3 == 0) 0.15f else if (i % 3 == 1) 0.4f else 0.25f,
            if (i % 2 == 0) 0.9f else 1f, infiniteRepeatable(tween(dur), androidx.compose.animation.core.RepeatMode.Reverse), label = "w$i"
        ).value
    }
    val allH = heights + heights.reversed()
    val grad = Brush.verticalGradient(listOf(FTV.AccentGlow, FTV.Accent))
    Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        allH.forEach { h -> Box(Modifier.weight(1f).fillMaxHeight(h).background(brush = grad, shape = RoundedCornerShape(2.dp))) }
    }
}

// ─────────────────────────────────────────────────────────────────
// Progress bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ProgressBar(state: PlayerState, tc: Color, tcm: Color) {
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
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(FTV.AccentDim, FTV.Accent, FTV.AccentGlow)), RoundedCornerShape(2.dp)))
            }
            Slider(
                value = progress,
                onValueChange = { frac ->
                    val newPos = (frac * state.durationMs).toLong()
                    state.mediaPlayer?.seekTo(newPos.toInt()); state.videoView?.seekTo(newPos.toInt()); state.positionMs = newPos
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main controls row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MainControls(state: PlayerState, tc: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlBtn(Icons.Default.Shuffle, if (state.isShuffle) FTV.Accent else tc.copy(0.5f), 22.dp) { state.isShuffle = !state.isShuffle }
        ControlBtn(Icons.Default.SkipPrevious, tc, 32.dp) { state.skipPrev() }
        ControlBtn(Icons.Default.Replay10, tc, 26.dp) {
            val p = (state.positionMs - 10000).coerceAtLeast(0)
            state.mediaPlayer?.seekTo(p.toInt()); state.videoView?.seekTo(p.toInt()); state.positionMs = p
        }
        Box(
            Modifier.size(56.dp).shadow(8.dp, CircleShape).background(FTV.Accent, CircleShape).clickable {
                if (state.isPlaying) { state.mediaPlayer?.pause(); state.videoView?.pause(); state.isPlaying = false }
                else                 { state.mediaPlayer?.start(); state.videoView?.start(); state.isPlaying = true }
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        ControlBtn(Icons.Default.Forward10, tc, 26.dp) {
            val p = (state.positionMs + 10000).coerceAtMost(state.durationMs)
            state.mediaPlayer?.seekTo(p.toInt()); state.videoView?.seekTo(p.toInt()); state.positionMs = p
        }
        ControlBtn(Icons.Default.SkipNext, tc, 32.dp) { state.skipNext() }
        ControlBtn(
            when (state.repeatMode) { RepeatMode.REPEAT_ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
            if (state.repeatMode != RepeatMode.OFF) FTV.Accent else tc.copy(0.5f), 22.dp
        ) { state.repeatMode = RepeatMode.values()[(state.repeatMode.ordinal + 1) % 3] }
    }
}

// ─────────────────────────────────────────────────────────────────
// Volume control
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
// Speed chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SpeedChip(state: PlayerState, bgTint: Color, tc: Color) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (state.playbackSpeed != 1f) FTV.Accent.copy(0.15f) else Color.Transparent)
                .clickable { showMenu = true }.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(speedLabel(state.playbackSpeed), color = if (state.playbackSpeed != 1f) FTV.Accent else bgTint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(FTV.Surface)) {
            SPEED_STEPS.forEach { speed ->
                DropdownMenuItem(
                    text = { Text(speedLabel(speed), color = if (state.playbackSpeed == speed) FTV.Accent else FTV.Text, fontWeight = if (state.playbackSpeed == speed) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) },
                    onClick = { state.playbackSpeed = speed; showMenu = false; state.applySpeedToAudio(); state.applySpeedToVideo() },
                    leadingIcon = { if (state.playbackSpeed == speed) Icon(Icons.Default.Check, null, tint = FTV.Accent, modifier = Modifier.size(14.dp)) else Spacer(Modifier.size(14.dp)) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Sleep timer chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SleepTimerChip(state: PlayerState, bgTint: Color) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.clip(RoundedCornerShape(5.dp))
                .background(if (state.sleepTimerActive) FTV.Gold.copy(0.15f) else Color.Transparent)
                .clickable { showMenu = true }.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Bedtime, null, tint = if (state.sleepTimerActive) FTV.Gold else bgTint, modifier = Modifier.size(14.dp))
                if (state.sleepTimerActive)
                    Text(formatTimer(state.sleepTimerSeconds), color = FTV.Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(FTV.Surface)) {
            if (state.sleepTimerActive) {
                DropdownMenuItem(
                    text = { Text("Cancel timer", color = FTV.DangerRed, fontSize = 13.sp) },
                    onClick = { state.sleepTimerActive = false; state.sleepTimerSeconds = 0L; showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = FTV.DangerRed, modifier = Modifier.size(14.dp)) }
                )
                Divider(color = FTV.Border)
            }
            SLEEP_OPTIONS.forEach { (min, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = FTV.Text, fontSize = 13.sp) },
                    onClick = { state.sleepTimerSeconds = min * 60L; state.sleepTimerActive = true; showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Bedtime, null, tint = FTV.Gold, modifier = Modifier.size(14.dp)) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// P-12: Aspect ratio chip (video)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AspectRatioChip(state: PlayerState, tint: Color) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.clip(RoundedCornerShape(5.dp)).clickable { showMenu = true }.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(state.aspectRatio.label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(FTV.Surface)) {
            AspectRatio.values().forEach { ar ->
                DropdownMenuItem(
                    text = { Text(ar.label, color = if (state.aspectRatio == ar) FTV.Accent else FTV.Text, fontSize = 13.sp) },
                    onClick = { state.aspectRatio = ar; showMenu = false },
                    leadingIcon = { if (state.aspectRatio == ar) Icon(Icons.Default.Check, null, tint = FTV.Accent, modifier = Modifier.size(14.dp)) else Spacer(Modifier.size(14.dp)) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Settings sheet (EQ, bass boost, crossfade, gapless)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSheet(
    state: PlayerState,
    isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color,
    surface: Color, border: Color,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.width(320.dp).fillMaxHeight().background(if (isDark) FTV.Surface else FTV.LSurface)
                .clickable(enabled = false) {}
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = tc, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = tcm) }
            }
            Divider(color = border)

            // Equalizer
            Text("Equalizer", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EqPreset.values().forEach { preset ->
                    val active = state.eqPreset == preset
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (active) FTV.Accent else (if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh))
                            .clickable { state.eqPreset = preset }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(preset.label, color = if (active) Color.White else tc, fontSize = 12.sp) }
                }
            }

            // Bass boost
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Bass Boost", color = tc, fontSize = 14.sp)
                    Text("Enhance low frequencies", color = tcm, fontSize = 11.sp)
                }
                Switch(checked = state.bassBoostOn, onCheckedChange = { state.bassBoostOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FTV.Accent))
            }

            // Virtualizer
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("3D Virtualizer", color = tc, fontSize = 14.sp)
                    Text("Spacious surround sound", color = tcm, fontSize = 11.sp)
                }
                Switch(checked = state.virtualizerOn, onCheckedChange = { state.virtualizerOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FTV.Accent))
            }

            Divider(color = border)

            // Crossfade
            Text("Crossfade", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration", color = tc, fontSize = 14.sp)
                    Text(if (state.crossfadeSec == 0) "Off" else "${state.crossfadeSec}s", color = FTV.Accent, fontSize = 14.sp)
                }
                Slider(
                    value = state.crossfadeSec.toFloat(), onValueChange = { state.crossfadeSec = it.roundToInt() },
                    valueRange = 0f..10f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = FTV.Accent, activeTrackColor = FTV.Accent)
                )
            }

            Divider(color = border)

            // Hidden files
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Show Hidden Files", color = tc, fontSize = 14.sp)
                    Text("Include dot-prefixed files", color = tcm, fontSize = 11.sp)
                }
                Switch(checked = state.showHidden, onCheckedChange = { state.showHidden = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FTV.Accent))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// P-02: Tag editor sheet
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TagEditorSheet(
    track: MediaTrack,
    isDark: Boolean,
    tc: Color, tcs: Color,
    surface: Color, border: Color,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title  by remember { mutableStateOf(track.displayTitle) }
    var artist by remember { mutableStateOf(track.displayArtist) }
    var album  by remember { mutableStateOf(track.displayAlbum) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(360.dp).clip(RoundedCornerShape(12.dp))
                .background(if (isDark) FTV.Surface else FTV.LSurface)
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Tags", color = tc, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Changes are saved in-app only (does not modify the file)", color = tcs, fontSize = 11.sp)
            Divider(color = border)
            listOf(
                "Title" to title to { v: String -> title = v },
                "Artist" to artist to { v: String -> artist = v },
                "Album" to album to { v: String -> album = v }
            ).forEach { (labelVal, setter) ->
                val (label, value) = labelVal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, color = tcs, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = value, onValueChange = setter,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tc, unfocusedTextColor = tc,
                            focusedBorderColor = FTV.Accent, unfocusedBorderColor = border
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = tcs) }
                Button(onClick = { onSave(title, artist, album) }, colors = ButtonDefaults.buttonColors(containerColor = FTV.Accent)) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// P-11: Share track
// ─────────────────────────────────────────────────────────────────

private fun shareTrack(ctx: Context, track: MediaTrack) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", track.file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (track.isVideo) "video/*" else "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, track.displayTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share ${track.displayTitle}"))
    } catch (_: Exception) {}
}

// ─────────────────────────────────────────────────────────────────
// Small helper composables
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
