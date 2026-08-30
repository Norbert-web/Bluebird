package io.github.norbertweb.bluebird.ui.screens

// ─────────────────────────────────────────────────────────────────
// MediaPlayerScreen.kt  —  Bluebird Films & TV  (Media3 rewrite)
//
// Playback now lives in PlaybackService (a Media3 MediaSessionService),
// reached here through a MediaController. This screen no longer owns a
// MediaPlayer or VideoView. What that buys you, concretely:
//
//  BUG FIXES from the previous version
//  B-01  seekTo crash: all seeks now go through the controller and are
//        no-ops (not exceptions) if the player isn't ready — Media3
//        handles this internally, no more manual playerPrepared flag.
//  B-02  Disk-write storm: PlaybackService persists queue/position on a
//        3s debounce, not on every 250ms position tick. This screen's
//        polling loop only touches Compose state, never SharedPreferences.
//  B-03  Drag-to-seek spam: ProgressBar now seeks in onValueChangeFinished,
//        not onValueChange — one seek per gesture, not one per pixel.
//  B-04  Duplicate/leaking video views: one ExoPlayer instance lives in
//        the service; windowed and fullscreen are just two PlayerView
//        attachments to the same player, released cleanly via onRelease.
//  B-05  Reflection-based video speed: gone. Player.setPlaybackSpeed()
//        is native in ExoPlayer and applies to audio AND video uniformly.
//  B-06  Fake crossfade: crossfadeSec now actually drives a volume-fade
//        across the (real, native) gapless transition — see PlaybackService.
//  B-07  EQ/Bass/Virtualizer silently no-op on video: they now attach to
//        the player's real audioSessionId regardless of media type.
//  B-08  Gesture "brightness" permanently changed the system-wide screen
//        brightness (and needed WRITE_SETTINGS). Now sets this window's
//        LayoutParams.screenBrightness only — no special permission,
//        and it reverts when you leave the screen, like every other app.
//
//  NEW
//  N-01  Library scan uses MediaLibraryRepository (MediaStore query)
//        instead of a recursive filesystem walk + per-file metadata
//        retriever pass — this is the fix for the multi-second scan on
//        every open.
//  N-02  Lock-screen / notification transport controls, via Media3's
//        default media notification (nothing to build here — it's a
//        consequence of playback living in a proper MediaSessionService).
//  N-03  Background playback: navigating away from this screen no longer
//        stops playback, because the player isn't tied to this
//        composable's lifecycle anymore.
//  N-04  Real SRT subtitle loading, wired end to end via
//        MediaItem.SubtitleConfiguration (file picking is done by the
//        hosting Activity — see onPickSubtitle param below).
// ─────────────────────────────────────────────────────────────────

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Microsoft Fluent System UI icons (https://github.com/niyajali/fluentui-system-icons) —
// replaces the previous Material Icons set. FluentIcons.Filled.* is used for active/emphasis
// states (currently-selected tab, active toggle, primary transport button); FluentIcons.Regular.*
// is used everywhere else, matching Fluent's own filled-vs-outline usage convention.
//
// Add to your module's build.gradle.kts:
//   implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
//
// CONFIRMED (the hard way, via Android Studio's own auto-import): every single
// icon is its own top-level symbol and needs its own import line — e.g.
// `FluentIcons.Regular.Image` requires `import fluent.ui.system.icons.regular.Image`.
// `FluentIcons.Filled.X` / `FluentIcons.Regular.X` in code is really sugar over
// these individually-imported properties, not a nested-object lookup. All of
// them are listed together right below, grouped by style, so this is the one
// place to look if a future icon addition needs the same treatment.

import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.norbertweb.bluebird.ui.components.LocalWindowRuntime
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.palette.graphics.Palette
import io.github.norbertweb.bluebird.media.MediaLibraryRepository
import io.github.norbertweb.bluebird.media.PlaybackService
import io.github.norbertweb.bluebird.media.ScannedTrack
import com.google.common.util.concurrent.MoreExecutors
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.AppsList
import fluent.ui.system.icons.filled.AppsListDetail
import fluent.ui.system.icons.filled.CheckboxChecked
import fluent.ui.system.icons.filled.Folder
import fluent.ui.system.icons.filled.History
import fluent.ui.system.icons.filled.MusicNote2
import fluent.ui.system.icons.filled.Next
import fluent.ui.system.icons.filled.Pause
import fluent.ui.system.icons.filled.Play
import fluent.ui.system.icons.filled.PlayCircle
import fluent.ui.system.icons.filled.Previous
import fluent.ui.system.icons.filled.Star
import fluent.ui.system.icons.filled.Video
import fluent.ui.system.icons.regular.Add
import fluent.ui.system.icons.regular.AddSquare
import fluent.ui.system.icons.regular.AppsList
import fluent.ui.system.icons.regular.AppsListDetail
import fluent.ui.system.icons.regular.ArrowClockwise
import fluent.ui.system.icons.regular.ArrowCounterclockwise
import fluent.ui.system.icons.regular.ArrowLeft
import fluent.ui.system.icons.regular.ArrowRepeat1
import fluent.ui.system.icons.regular.ArrowRepeatAll
import fluent.ui.system.icons.regular.ArrowShuffle
import fluent.ui.system.icons.regular.Checkbox1
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ChevronUp
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Dismiss
import fluent.ui.system.icons.regular.Edit
import fluent.ui.system.icons.regular.Folder
import fluent.ui.system.icons.regular.FolderAdd
import fluent.ui.system.icons.regular.FullScreenMaximize
import fluent.ui.system.icons.regular.FullScreenMinimize
import fluent.ui.system.icons.regular.History
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.MoreVertical
import fluent.ui.system.icons.regular.MusicNote2
import fluent.ui.system.icons.regular.Navigation
import fluent.ui.system.icons.regular.Options
import fluent.ui.system.icons.regular.Play
import fluent.ui.system.icons.regular.Search
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Share
import fluent.ui.system.icons.regular.Speaker0
import fluent.ui.system.icons.regular.Speaker2
import fluent.ui.system.icons.regular.SpeakerMute
import fluent.ui.system.icons.regular.Star
import fluent.ui.system.icons.regular.Subtitles
import fluent.ui.system.icons.regular.Timer
import fluent.ui.system.icons.regular.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Fluent icon set — Microsoft Fluent System UI icons
//
// Every icon used by this screen is centralized here as a single object,
// `FI`, instead of scattering `FluentIcons.Filled.Y` / `FluentIcons.Regular.Y`
// references across 40+ call sites. Benefits:
//  - One place to audit which icons the screen depends on.
//  - One place to swap Regular↔Filled emphasis without touching UI code.
//  - Compiles to a direct property reference, so there's no runtime cost
//    versus referencing FluentIcons directly.
//
// A couple of the less common names below (e.g. MusicNote2, AppsListDetail,
// SpeakerZero) are typed from Fluent's published icon catalog naming
// conventions but weren't individually confirmed against your exact library
// version. If Android Studio flags any one of them as unresolved, open the
// interactive catalog (niyajali.github.io/fluentui-system-icons), search for
// the icon by appearance, and swap in the exact name it shows — everything
// else in the file references icons only through this object, so a fix here
// is a one-line change with no ripple effect.
// ─────────────────────────────────────────────────────────────────
private object FI {
    // Navigation / chrome
    val Back            = FluentIcons.Regular.ArrowLeft
    val Menu            = FluentIcons.Regular.Navigation
    val Close           = FluentIcons.Regular.Dismiss
    val Search          = FluentIcons.Regular.Search
    val Settings        = FluentIcons.Regular.Settings
    val MoreVert        = FluentIcons.Regular.MoreVertical
    val Tune            = FluentIcons.Regular.Options
    val Fullscreen      = FluentIcons.Regular.FullScreenMaximize
    val FullscreenExit  = FluentIcons.Regular.FullScreenMinimize

    // Media type / library — Regular/Filled pairs so tab and row "active" states
    // can swap weight consistently, the way Fluent apps do (e.g. Windows' own
    // Media Player / Groove Music nav rail).
    val Movie           = FluentIcons.Filled.Video
    val MovieOutline    = FluentIcons.Regular.Video
    val MusicNoteFilled = FluentIcons.Filled.MusicNote2
    val MusicNote       = FluentIcons.Regular.MusicNote2
    val PlaylistPlayFilled = FluentIcons.Filled.AppsList
    val PlaylistPlay    = FluentIcons.Regular.AppsList
    val QueueMusicFilled = FluentIcons.Filled.AppsListDetail
    val QueueMusic      = FluentIcons.Regular.AppsListDetail
    val FolderFilled    = FluentIcons.Filled.Folder
    val Folder          = FluentIcons.Regular.Folder
    val HistoryFilled   = FluentIcons.Filled.History
    val History         = FluentIcons.Regular.History
    val Star            = FluentIcons.Filled.Star
    val StarBorder      = FluentIcons.Regular.Star
    val PlayCircle      = FluentIcons.Filled.PlayCircle

    // Transport controls
    val Play            = FluentIcons.Filled.Play
    val Pause           = FluentIcons.Filled.Pause
    val SkipNext        = FluentIcons.Filled.Next
    val SkipPrevious    = FluentIcons.Filled.Previous
    val Shuffle         = FluentIcons.Regular.ArrowShuffle
    val Repeat          = FluentIcons.Regular.ArrowRepeatAll
    val RepeatOne       = FluentIcons.Regular.ArrowRepeat1
    val Replay10        = FluentIcons.Regular.ArrowCounterclockwise
    val Forward10       = FluentIcons.Regular.ArrowClockwise

    // Volume
    val VolumeOff       = FluentIcons.Regular.SpeakerMute
    val VolumeDown      = FluentIcons.Regular.Speaker0
    val VolumeUp        = FluentIcons.Regular.Speaker2

    // Queue / playlist row actions
    val Add             = FluentIcons.Regular.Add
    val Check           = FluentIcons.Regular.Checkmark
    val CheckBox        = FluentIcons.Filled.CheckboxChecked
    val CheckBoxOutline = FluentIcons.Regular.Checkbox1
    val Edit            = FluentIcons.Regular.Edit
    val Delete          = FluentIcons.Regular.Delete
    val Share           = FluentIcons.Regular.Share
    val Subtitles       = FluentIcons.Regular.Subtitles
    val LibraryAdd      = FluentIcons.Regular.FolderAdd
    val PlaylistAdd     = FluentIcons.Regular.AddSquare
    val QueuePlayNext   = FluentIcons.Regular.Play
    val Remove          = FluentIcons.Regular.Dismiss
    val KeyboardArrowUp = FluentIcons.Regular.ChevronUp
    val KeyboardArrowDown = FluentIcons.Regular.ChevronDown
    val Timer           = FluentIcons.Regular.Timer
}

// ─────────────────────────────────────────────────────────────────
// Design tokens (unchanged)
// ─────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────
// Theme system — io.github.norbertweb.io.github.norbertweb.bluebird-style accent themes. FTV keeps every existing
// call site (`FTV.Accent`, `FTV.Gold`, …) working unchanged; those
// properties now read from whichever BBTheme is currently selected via
// a plain Compose-observable holder. Structural surface/bg colors stay
// as before — only the "personality" colors are themeable.
// ─────────────────────────────────────────────────────────────────

data class BBPalette(
    val accent: Color, val accentGlow: Color, val accentDim: Color,
    val gold: Color, val videoColor: Color, val audioColor: Color
)

enum class BBTheme(val label: String, val palette: BBPalette) {
    FLUENT("Fluent Blue", BBPalette(
        accent = Color(0xFF0078D4), accentGlow = Color(0xFF429CE3), accentDim = Color(0xFF005A9E),
        gold = Color(0xFFFFB900), videoColor = Color(0xFF16C60C), audioColor = Color(0xFF9B59B6)
    )),
    NORD("Nord", BBPalette(
        accent = Color(0xFF88C0D0), accentGlow = Color(0xFFA3D4E0), accentDim = Color(0xFF5E9AAD),
        gold = Color(0xFFEBCB8B), videoColor = Color(0xFFA3BE8C), audioColor = Color(0xFFB48EAD)
    )),
    DRACULA("Dracula", BBPalette(
        accent = Color(0xFFBD93F9), accentGlow = Color(0xFFD6BCFA), accentDim = Color(0xFF8C6FC4),
        gold = Color(0xFFF1FA8C), videoColor = Color(0xFF50FA7B), audioColor = Color(0xFFFF79C6)
    )),
    WINAMP("Classic Winamp", BBPalette(
        accent = Color(0xFF00FF41), accentGlow = Color(0xFF6BFF8E), accentDim = Color(0xFF00B82E),
        gold = Color(0xFFFFD400), videoColor = Color(0xFF00E5FF), audioColor = Color(0xFF00FF41)
    )),
    SOLAR("Solarized", BBPalette(
        accent = Color(0xFF268BD2), accentGlow = Color(0xFF5AA9DE), accentDim = Color(0xFF1B6796),
        gold = Color(0xFFB58900), videoColor = Color(0xFF859900), audioColor = Color(0xFFD33682)
    )),
    SUNSET("Sunset", BBPalette(
        accent = Color(0xFFFF6B6B), accentGlow = Color(0xFFFF9E9E), accentDim = Color(0xFFCC4F4F),
        gold = Color(0xFFFFD166), videoColor = Color(0xFF4ECDC4), audioColor = Color(0xFFFF6B6B)
    ));
}

private object ThemeHolder {
    var current by mutableStateOf(BBTheme.FLUENT)
}

private const val THEME_PREFS_KEY = "bluebird_theme"

private data class LibraryLoadResult(
    val tracks: List<MediaTrack>,
    val recentPaths: List<String>,
    val playlists: List<BBPlaylist>,
    val theme: String
)

private fun loadThemeName(ctx: Context): String =
    prefs(ctx).getString(THEME_PREFS_KEY, BBTheme.FLUENT.name) ?: BBTheme.FLUENT.name

private fun applyThemeName(name: String) {
    ThemeHolder.current = try { BBTheme.valueOf(name) } catch (_: Exception) { BBTheme.FLUENT }
}

private fun saveTheme(ctx: Context, theme: BBTheme) {
    ThemeHolder.current = theme
    prefs(ctx).edit().putString(THEME_PREFS_KEY, theme.name).apply()
}

private object FTV {
    val Bg           = Color(0xFF0D0D0D)
    val BgMid        = Color(0xFF141414)
    val Surface      = Color(0xFF1C1C1C)
    val SurfaceHigh  = Color(0xFF242424)
    val Border       = Color(0xFF2E2E2E)
    val SelectedBg   get() = Accent.copy(alpha = 0.14f)
    val Text         = Color(0xFFFFFFFF)
    val TextSec      = Color(0xFFAAAAAA)
    val TextMuted    = Color(0xFF666666)
    val Accent       get() = ThemeHolder.current.palette.accent
    val AccentGlow   get() = ThemeHolder.current.palette.accentGlow
    val AccentDim    get() = ThemeHolder.current.palette.accentDim
    val Gold         get() = ThemeHolder.current.palette.gold
    val VideoGreen   get() = ThemeHolder.current.palette.videoColor
    val AudioPurple  get() = ThemeHolder.current.palette.audioColor
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

/**
 * Fluent-style flyout chrome for dropdown/overflow menus: rounded corners and a
 * hairline border instead of Material's sharp-cornered, borderless popup — matches
 * the rounded, bordered "Flyout" surface Fluent apps use for context menus.
 */
@Composable
private fun fluentMenuModifier(isDark: Boolean): Modifier = Modifier
    .clip(RoundedCornerShape(8.dp))
    .background(if (isDark) FTV.Surface else FTV.LSurface)
    .border(1.dp, if (isDark) FTV.Border else FTV.LBorder, RoundedCornerShape(8.dp))

/**
 * Same Fluent flyout chrome, tuned for the always-dark, on-video-scrim chips
 * (Speed / Sleep Timer / Aspect Ratio) that only ever appear over the black
 * fullscreen-video overlay, regardless of the app's light/dark setting.
 */
private fun fluentFlyoutOnScrimModifier(): Modifier = Modifier
    .clip(RoundedCornerShape(8.dp))
    .background(Color(0xFF242424))
    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))

// Equalizer preset bands (gain in millibels for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
private enum class EqPreset(val label: String, val gains: IntArray) {
    FLAT       ("Flat",       intArrayOf(   0,    0,    0,    0,    0)),
    BASS       ("Bass Boost", intArrayOf( 600,  400,    0, -200, -200)),
    ROCK       ("Rock",       intArrayOf( 400,  200, -200,  200,  400)),
    JAZZ       ("Jazz",       intArrayOf( 200,    0,  200,  200,  100)),
    CLASSICAL  ("Classical",  intArrayOf( 300,  200,    0,  200,  300)),
    VOCAL      ("Vocal",      intArrayOf(-200, -100,  400,  300,  100)),
    ELECTRONIC ("Electronic", intArrayOf( 400,  300,    0,  300,  400)),
}

private enum class AspectRatio(val label: String) {
    FIT("Fit"), FILL("Fill"), RATIO_4_3("4:3"), RATIO_16_9("16:9"), STRETCH("Stretch")
}

private fun AspectRatio.toResizeMode(): Int = when (this) {
    AspectRatio.FIT                            -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    AspectRatio.FILL                           -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatio.STRETCH                        -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatio.RATIO_4_3, AspectRatio.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

// ─────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────

data class MediaTrack(
    val contentUri    : Uri,
    val file          : File?,
    val title         : String,
    val artist        : String     = "Unknown Artist",
    val album         : String     = "Unknown Album",
    val durationMs    : Long       = 0L,
    val isVideo       : Boolean    = false,
    val artworkUri    : Uri?       = null,
    // Mutable counters — not part of equals/hashCode intentionally
    var playCount     : Int        = 0,
    var isFavorite    : Boolean    = false,
    // Edited metadata (null = use retrieved)
    var editTitle     : String?    = null,
    var editArtist    : String?    = null,
    var editAlbum     : String?    = null,
    // N-04: real SRT subtitle wiring
    var subtitleUri   : Uri?       = null
) {
    val displayTitle  get() = editTitle  ?: title
    val displayArtist get() = editArtist ?: artist
    val displayAlbum  get() = editAlbum  ?: album

    /** Stable key for prefs/meta storage and queue identity. */
    val metaKey get() = contentUri.toString()

    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(displayTitle)
            .setArtist(displayArtist)
            .setAlbumTitle(displayAlbum)
            .setArtworkUri(artworkUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val builder = MediaItem.Builder()
            .setUri(contentUri)
            .setMediaId(metaKey)
            .setMediaMetadata(metadata)
        if (isVideo && subtitleUri != null) {
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(subtitleUri!!)
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        return builder.build()
    }
}

private fun ScannedTrack.toMediaTrack() = MediaTrack(
    contentUri = contentUri,
    file       = file,
    title      = title,
    artist     = artist,
    album      = album,
    durationMs = durationMs,
    isVideo    = isVideo,
    artworkUri = artworkUri
)

enum class RepeatMode { OFF, REPEAT_ALL, REPEAT_ONE }
enum class MediaTab   { VIDEOS, MUSIC, PLAYLIST, PLAYLISTS, FOLDERS, RECENTS, FAVORITES }

private fun RepeatMode.toPlayerRepeat() = when (this) {
    RepeatMode.OFF        -> Player.REPEAT_MODE_OFF
    RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
}
private fun Int.toRepeatMode() = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.REPEAT_ALL
    Player.REPEAT_MODE_ONE -> RepeatMode.REPEAT_ONE
    else                   -> RepeatMode.OFF
}

// ─────────────────────────────────────────────────────────────────
// Persistence helpers — favorites / tags / recents (unchanged from
// before; these were never the slow or buggy part). Queue/position
// persistence now lives in PlaybackService, on the same prefs file.
// ─────────────────────────────────────────────────────────────────

private fun prefs(ctx: Context): SharedPreferences =
    ctx.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE)

private fun saveTrackMeta(ctx: Context, track: MediaTrack) {
    val key = "meta_${track.metaKey}"
    prefs(ctx).edit()
        .putBoolean("${key}_fav",    track.isFavorite)
        .putInt("${key}_plays",      track.playCount)
        .putString("${key}_etitle",  track.editTitle ?: "")
        .putString("${key}_eartist", track.editArtist ?: "")
        .putString("${key}_ealbum",  track.editAlbum ?: "")
        .apply()
}

private fun loadTrackMeta(ctx: Context, track: MediaTrack) {
    val key = "meta_${track.metaKey}"
    val p   = prefs(ctx)
    track.isFavorite = p.getBoolean("${key}_fav",   false)
    track.playCount  = p.getInt("${key}_plays",      0)
    track.editTitle  = p.getString("${key}_etitle",  "")?.takeIf { it.isNotEmpty() }
    track.editArtist = p.getString("${key}_eartist", "")?.takeIf { it.isNotEmpty() }
    track.editAlbum  = p.getString("${key}_ealbum",  "")?.takeIf { it.isNotEmpty() }
}

private fun pushRecent(ctx: Context, key: String) {
    val p   = prefs(ctx)
    val raw = p.getString("recents", "[]")!!
    val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) { val s = arr.getString(i); if (s != key) list.add(s) }
    list.add(0, key)
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

/** Reads the queue PlaybackService last persisted (mediaId list + index + position). */
private fun loadSavedQueueIds(ctx: Context): Triple<List<String>, Int, Long> {
    val p = prefs(ctx)
    val raw = p.getString("queue_ids", null) ?: return Triple(emptyList(), -1, 0L)
    val idx = p.getInt("queue_index", -1)
    val pos = p.getLong("queue_pos", 0L)
    return try {
        val arr = JSONArray(raw)
        Triple((0 until arr.length()).map { arr.getString(it) }, idx, pos)
    } catch (_: Exception) { Triple(emptyList(), -1, 0L) }
}

// ─────────────────────────────────────────────────────────────────
// User playlists — named, persisted collections distinct from the live
// playback queue (MediaTab.PLAYLIST). Stored as one JSON blob; this is
// a "few dozen playlists, few hundred tracks each" scale feature, so a
// single pref key read/written whole is simpler and fast enough — no
// need for a database for this.
// ─────────────────────────────────────────────────────────────────

data class BBPlaylist(
    val id: String,
    var name: String,
    val trackKeys: MutableList<String> = mutableListOf()
)

private const val PLAYLISTS_PREFS_KEY = "bluebird_playlists"

private fun loadPlaylists(ctx: Context): List<BBPlaylist> {
    val raw = prefs(ctx).getString(PLAYLISTS_PREFS_KEY, "[]")!!
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val keysArr = o.getJSONArray("keys")
            BBPlaylist(
                id = o.getString("id"),
                name = o.getString("name"),
                trackKeys = (0 until keysArr.length()).map { keysArr.getString(it) }.toMutableList()
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun savePlaylists(ctx: Context, playlists: List<BBPlaylist>) {
    val arr = JSONArray()
    playlists.forEach { pl ->
        val o = org.json.JSONObject()
        o.put("id", pl.id)
        o.put("name", pl.name)
        o.put("keys", JSONArray().also { a -> pl.trackKeys.forEach { a.put(it) } })
        arr.put(o)
    }
    prefs(ctx).edit().putString(PLAYLISTS_PREFS_KEY, arr.toString()).apply()
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
    var isLibraryReady by mutableStateOf(false)
    var hasRestoredQueue by mutableStateOf(false)
    var hasHandledInitialPath by mutableStateOf(false)

    // Playlist — mirrors the controller's timeline; controller is the
    // source of truth, this is a display-friendly projection of it.
    var playlist     by mutableStateOf(listOf<MediaTrack>())
    var currentIndex by mutableStateOf(-1)
    var isPlaying    by mutableStateOf(false)
    var isShuffle    by mutableStateOf(false)
    var repeatMode   by mutableStateOf(RepeatMode.OFF)

    // Position / duration — polled from the controller for UI display only.
    // No disk I/O happens as a result of these changing (B-02).
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)

    var volume  by mutableStateOf(1f)
    var isMuted by mutableStateOf(false)

    // UI
    var activeTab     by mutableStateOf(MediaTab.MUSIC)
    var showQueue     by mutableStateOf(true)
    var isFullscreen  by mutableStateOf(false)
    var showControls  by mutableStateOf(true)
    var isBuffering   by mutableStateOf(false)
    var searchQuery   by mutableStateOf("")

    // Engine — a single controller, shared by windowed and fullscreen views
    var controller by mutableStateOf<MediaController?>(null)

    var playbackSpeed by mutableStateOf(1.0f)

    // Sleep timer
    var sleepTimerSeconds by mutableStateOf(0L)
    var sleepTimerActive  by mutableStateOf(false)

    // Crossfade — now a real setting sent to the service (B-06)
    var crossfadeSec by mutableStateOf(0)

    // Equalizer
    var eqPreset       by mutableStateOf(EqPreset.FLAT)
    var bassBoostOn    by mutableStateOf(false)
    var virtualizerOn  by mutableStateOf(false)

    // Video aspect ratio
    var aspectRatio by mutableStateOf(AspectRatio.FIT)

    // Recents
    var recentPaths by mutableStateOf(listOf<String>())

    // Overlays
    var showSettings  by mutableStateOf(false)
    var showTagEditor by mutableStateOf<MediaTrack?>(null)
    var showMoreControls by mutableStateOf(false)

    // User playlists (distinct from the live queue)
    var userPlaylists    by mutableStateOf(listOf<BBPlaylist>())
    var openPlaylistId   by mutableStateOf<String?>(null)
    var showAddToPlaylist by mutableStateOf<MediaTrack?>(null) // non-null = "add this track" sheet is open
    var showNewPlaylistDialog by mutableStateOf(false)
    var editingPlaylistId by mutableStateOf<String?>(null) // rename-in-progress

    // Groupings
    val audioGroups: Map<String, List<MediaTrack>> get() =
        audioTracks.groupBy { it.displayAlbum.ifBlank { "Unknown Album" } }.toSortedMap()
    val videoGroups: Map<String, List<MediaTrack>> get() =
        videoTracks.groupBy { it.file?.parentFile?.name ?: "Videos" }.toSortedMap()
    val folderGroups: Map<String, List<MediaTrack>> get() =
        allTracks.groupBy { it.file?.parentFile?.absolutePath ?: it.displayAlbum }.toSortedMap()

    val currentTrack get() = playlist.getOrNull(currentIndex)

    val openPlaylist get() = userPlaylists.firstOrNull { it.id == openPlaylistId }
    val openPlaylistTracks: List<MediaTrack> get() {
        val pl = openPlaylist ?: return emptyList()
        val byKey = allTracks.associateBy { it.metaKey }
        return pl.trackKeys.mapNotNull { byKey[it] }
    }

    val filteredTracks get() = when (activeTab) {
        MediaTab.VIDEOS    -> videoTracks.filter { q -> q.displayTitle.contains(searchQuery, true) || q.displayArtist.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.MUSIC     -> audioTracks.filter { q -> q.displayTitle.contains(searchQuery, true) || q.displayArtist.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.PLAYLIST  -> playlist.filter    { q -> q.displayTitle.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.PLAYLISTS -> openPlaylistTracks.filter { q -> q.displayTitle.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.FOLDERS   -> allTracks.filter   { q -> q.displayTitle.contains(searchQuery, true) || searchQuery.isEmpty() }
        MediaTab.RECENTS   -> recentPaths.mapNotNull { key -> allTracks.firstOrNull { it.metaKey == key } }
        MediaTab.FAVORITES -> allTracks.filter   { it.isFavorite }
    }

    // ── Controller-backed actions ───────────────────────────────────
    // Every one of these replaces hand-rolled index math from the old
    // version with the controller's own (correct, tested) queue handling.

    fun playQueue(tracks: List<MediaTrack>, startIndex: Int) {
        val ctrl = controller ?: return
        if (tracks.isEmpty()) return
        ctrl.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun playTrackAt(index: Int) {
        controller?.let { if (index in playlist.indices) { it.seekTo(index, 0L); it.play() } }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() { controller?.let { if (it.hasNextMediaItem()) it.seekToNext() } }

    fun skipPrev() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3000) { ctrl.seekTo(0); return }
        if (ctrl.hasPreviousMediaItem()) ctrl.seekToPrevious() else ctrl.seekTo(0)
    }

    fun seekTo(ms: Long) {
        val ctrl = controller ?: return
        if (!ctrl.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) return
        ctrl.seekTo(ms.coerceIn(0, ctrl.duration.takeIf { it != C.TIME_UNSET } ?: ms))
    }

    fun addToPlaylist(track: MediaTrack) {
        val ctrl = controller ?: return
        val exists = (0 until ctrl.mediaItemCount).any { ctrl.getMediaItemAt(it).mediaId == track.metaKey }
        if (!exists) ctrl.addMediaItem(track.toMediaItem())
        if (ctrl.mediaItemCount == 1 && !ctrl.isPlaying) { ctrl.prepare(); ctrl.play() }
    }

    fun playNext(track: MediaTrack) {
        val ctrl = controller ?: return
        val existing = (0 until ctrl.mediaItemCount).firstOrNull { ctrl.getMediaItemAt(it).mediaId == track.metaKey }
        existing?.let { ctrl.removeMediaItem(it) }
        val insertAt = (ctrl.currentMediaItemIndex + 1).coerceIn(0, ctrl.mediaItemCount)
        ctrl.addMediaItem(insertAt, track.toMediaItem())
    }

    fun removeFromPlaylist(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
    }

    fun moveInPlaylist(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        controller?.setPlaybackSpeed(speed) // B-05: native, no reflection, works for audio + video
    }

    fun sendEffectCommand(action: String, bundle: android.os.Bundle) {
        controller?.sendCustomCommand(SessionCommand(action, android.os.Bundle.EMPTY), bundle)
    }

    fun applyEqPreset(preset: EqPreset) {
        eqPreset = preset
        sendEffectCommand(PlaybackService.CMD_SET_EQ_PRESET, bundleOf(PlaybackService.ARG_VALUE to preset.gains))
    }

    fun setBassBoost(on: Boolean) {
        bassBoostOn = on
        sendEffectCommand(PlaybackService.CMD_SET_BASS_BOOST, bundleOf(PlaybackService.ARG_VALUE to on))
    }

    fun setVirtualizer(on: Boolean) {
        virtualizerOn = on
        sendEffectCommand(PlaybackService.CMD_SET_VIRTUALIZER, bundleOf(PlaybackService.ARG_VALUE to on))
    }

    fun setCrossfade(seconds: Int) {
        crossfadeSec = seconds
        sendEffectCommand(PlaybackService.CMD_SET_CROSSFADE, bundleOf(PlaybackService.ARG_VALUE to seconds))
    }

    // ── User playlists ───────────────────────────────────────────────

    fun createPlaylist(ctx: Context, name: String): BBPlaylist {
        val pl = BBPlaylist(id = System.currentTimeMillis().toString(), name = name.ifBlank { "New Playlist" })
        userPlaylists = userPlaylists + pl
        savePlaylists(ctx, userPlaylists)
        return pl
    }

    fun deletePlaylist(ctx: Context, id: String) {
        userPlaylists = userPlaylists.filterNot { it.id == id }
        if (openPlaylistId == id) openPlaylistId = null
        savePlaylists(ctx, userPlaylists)
    }

    fun renamePlaylist(ctx: Context, id: String, newName: String) {
        if (newName.isBlank()) return
        userPlaylists = userPlaylists.map { if (it.id == id) it.copy(name = newName) else it }
        savePlaylists(ctx, userPlaylists)
    }

    fun addTrackToPlaylist(ctx: Context, playlistId: String, track: MediaTrack) {
        userPlaylists = userPlaylists.map { pl ->
            if (pl.id == playlistId && track.metaKey !in pl.trackKeys) {
                BBPlaylist(pl.id, pl.name, (pl.trackKeys + track.metaKey).toMutableList())
            } else pl
        }
        savePlaylists(ctx, userPlaylists)
    }

    fun removeTrackFromPlaylist(ctx: Context, playlistId: String, track: MediaTrack) {
        userPlaylists = userPlaylists.map { pl ->
            if (pl.id == playlistId) BBPlaylist(pl.id, pl.name, pl.trackKeys.filterNot { it == track.metaKey }.toMutableList())
            else pl
        }
        savePlaylists(ctx, userPlaylists)
    }

    fun playPlaylist(id: String, startIndex: Int = 0) {
        val pl = userPlaylists.firstOrNull { it.id == id } ?: return
        val byKey = allTracks.associateBy { it.metaKey }
        val tracks = pl.trackKeys.mapNotNull { byKey[it] }
        playQueue(tracks, startIndex)
    }
}

@Composable
private fun rememberPlayerState() = remember { PlayerState() }

/** Rebuilds the display playlist from the controller's actual timeline (source of truth). */
private fun rebuildPlaylistFromController(ctrl: MediaController, library: List<MediaTrack>): List<MediaTrack> {
    val byId = library.associateBy { it.metaKey }
    return (0 until ctrl.mediaItemCount).mapNotNull { i -> byId[ctrl.getMediaItemAt(i).mediaId] }
}

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    isDark      : Boolean,
    initialPath : String = "",
    /**
     * N-04: subtitle files require the Storage Access Framework, which
     * needs an ActivityResultLauncher registered at the Activity level —
     * that can't be started from inside a plain Composable. The hosting
     * Activity passes a launcher-backed callback in; default is a no-op
     * so this screen still compiles/works if the caller hasn't wired it.
     */
    onPickSubtitle: ((onPicked: (Uri) -> Unit) -> Unit)? = null
) {
    val ctx   = LocalContext.current
    val windowRuntime = LocalWindowRuntime.current
    val state = rememberPlayerState()
    val scope = rememberCoroutineScope()

    val bg      = if (isDark) FTV.Bg         else FTV.LBg
    val surface = if (isDark) FTV.Surface    else FTV.LSurface
    val surfaceH= if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh
    val border  = if (isDark) FTV.Border     else FTV.LBorder
    val tc      = if (isDark) FTV.Text       else FTV.LText
    val tcs     = if (isDark) FTV.TextSec    else FTV.LTextSec
    val tcm     = if (isDark) FTV.TextMuted  else FTV.LTextMuted

    // ── N-01: MediaStore-backed scan — no filesystem walk, no per-file
    // metadata retriever pass. This is the fix for the slow-open complaint.
    LaunchedEffect(windowRuntime.isMinimized) {
        if (windowRuntime.isMinimized || state.isLibraryReady) return@LaunchedEffect
        state.isLoading = true
        val libraryData = withContext(Dispatchers.IO) {
            val scanned = MediaLibraryRepository.scan(ctx)
            val tracks = scanned.map { it.toMediaTrack() }
            tracks.forEach { loadTrackMeta(ctx, it) }
            LibraryLoadResult(
                tracks = tracks,
                recentPaths = loadRecents(ctx),
                playlists = loadPlaylists(ctx),
                theme = loadThemeName(ctx)
            )
        }
        val tracks = libraryData.tracks
        state.allTracks   = tracks
        state.videoTracks = tracks.filter { it.isVideo }
        state.audioTracks = tracks.filter { !it.isVideo }
        state.isLoading   = false
        state.isLibraryReady = true
        state.recentPaths = libraryData.recentPaths
        state.userPlaylists = libraryData.playlists
        applyThemeName(libraryData.theme)

    }

    // ── Connect to PlaybackService via MediaController ───────────────
    DisposableEffect(Unit) {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            state.controller = try { future.get() } catch (_: Exception) { null }
        }, MoreExecutors.directExecutor())
        onDispose {
            state.controller?.let { ctrl -> runCatching { ctrl.release() } }
            MediaController.releaseFuture(future)
            state.controller = null
        }
    }

    // ── Mirror controller state into Compose state ───────────────────
    DisposableEffect(state.controller) {
        val ctrl = state.controller
        if (ctrl == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { state.isPlaying = isPlaying }
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    state.durationMs = ctrl.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                state.currentIndex = ctrl.currentMediaItemIndex
                state.durationMs = ctrl.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                state.positionMs = 0L
                val track = state.playlist.getOrNull(state.currentIndex) ?: return
                track.playCount++
                scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, track) }
                scope.launch { pushRecent(ctx, track.metaKey); state.recentPaths = loadRecents(ctx) }
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                state.playlist = rebuildPlaylistFromController(ctrl, state.allTracks)
                state.currentIndex = ctrl.currentMediaItemIndex
            }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { state.isShuffle = enabled }
            override fun onRepeatModeChanged(mode: Int) { state.repeatMode = mode.toRepeatMode() }
        }
        ctrl.addListener(listener)
        // Prime initial state in case the controller connected with an
        // already-active session (e.g. re-entering the screen).
        state.isPlaying    = ctrl.isPlaying
        state.isShuffle     = ctrl.shuffleModeEnabled
        state.repeatMode    = ctrl.repeatMode.toRepeatMode()
        state.currentIndex  = ctrl.currentMediaItemIndex
        state.durationMs    = ctrl.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        onDispose { ctrl.removeListener(listener) }
    }

    // ── Open a file explicitly requested by Explorer/Desktop. This runs only after
    // both the MediaStore library and PlaybackService controller are ready. The old
    // implementation tried to play during the library scan, before the controller
    // existed, so the app window opened with an empty player.
    LaunchedEffect(state.controller, state.isLibraryReady, initialPath) {
        val ctrl = state.controller ?: return@LaunchedEffect
        if (!state.isLibraryReady || initialPath.isBlank()) return@LaunchedEffect
        if (state.hasHandledInitialPath) return@LaunchedEffect

        val target = state.allTracks.firstOrNull {
            it.file?.absolutePath == initialPath ||
                it.contentUri.toString() == initialPath
        }
        if (target != null) {
            state.hasHandledInitialPath = true
            val index = state.allTracks.indexOf(target)
            state.activeTab = if (target.isVideo) MediaTab.VIDEOS else MediaTab.MUSIC
            state.playQueue(state.allTracks, index)
        } else {
            // MediaStore can occasionally omit a freshly-created or unusual file.
            // Still open it directly through our FileProvider instead of presenting
            // an empty Media Player window.
            val file = File(initialPath)
            if (file.isFile) {
                val uri = runCatching {
                    androidx.core.content.FileProvider.getUriForFile(
                        ctx, "${ctx.packageName}.fileprovider", file
                    )
                }.getOrNull()
                if (uri != null) {
                    val ext = file.extension.lowercase()
                    val direct = MediaTrack(
                        contentUri = uri,
                        file = file,
                        title = file.nameWithoutExtension,
                        isVideo = ext in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "m4v")
                    )
                    state.hasHandledInitialPath = true
                    state.activeTab = if (direct.isVideo) MediaTab.VIDEOS else MediaTab.MUSIC
                    state.playQueue(listOf(direct), 0)
                }
            }
        }
    }

    // ── Restore last queue once both library and controller are ready ─
    LaunchedEffect(state.controller, state.isLibraryReady) {
        val ctrl = state.controller ?: return@LaunchedEffect
        // An explicit Explorer/Desktop file launch always wins over the saved queue.
        if (initialPath.isNotBlank()) return@LaunchedEffect
        if (!state.isLibraryReady || state.hasRestoredQueue) return@LaunchedEffect
        state.hasRestoredQueue = true
        if (ctrl.mediaItemCount > 0) return@LaunchedEffect // service already has an active queue
        val (ids, idx, pos) = loadSavedQueueIds(ctx)
        if (ids.isEmpty()) return@LaunchedEffect
        val byId = state.allTracks.associateBy { it.metaKey }
        val restored = ids.mapNotNull { byId[it] }
        if (restored.isNotEmpty()) {
            ctrl.setMediaItems(
                restored.map { it.toMediaItem() },
                idx.coerceIn(0, restored.size - 1),
                pos.coerceAtLeast(0)
            )
            ctrl.prepare()
        }
    }

    // ── Position polling for the UI progress bar only — no disk I/O (B-02)
    LaunchedEffect(state.controller, state.isPlaying, windowRuntime.isMinimized) {
        val ctrl = state.controller ?: return@LaunchedEffect
        if (windowRuntime.isMinimized) return@LaunchedEffect
        var lastPublished = -1L
        while (isActive) {
            if (state.isPlaying) {
                val position = ctrl.currentPosition.coerceAtLeast(0)
                if (lastPublished < 0L || kotlin.math.abs(position - lastPublished) >= 450L) {
                    state.positionMs = position
                    lastPublished = position
                }
            }
            delay(500)
        }
    }

    // Volume sync — one code path for audio AND video now (was split before)
    LaunchedEffect(state.volume, state.isMuted) {
        state.controller?.volume = if (state.isMuted) 0f else state.volume
    }

    // Shuffle / repeat sync
    LaunchedEffect(state.isShuffle) { state.controller?.shuffleModeEnabled = state.isShuffle }
    LaunchedEffect(state.repeatMode) { state.controller?.repeatMode = state.repeatMode.toPlayerRepeat() }

    // Auto-hide controls in fullscreen
    LaunchedEffect(state.showControls, state.isFullscreen) {
        if (state.isFullscreen && state.showControls) {
            delay(4000)
            if (isActive) state.showControls = false
        }
    }

    // Sleep timer
    LaunchedEffect(state.sleepTimerActive) {
        if (!state.sleepTimerActive) return@LaunchedEffect
        while (isActive && state.sleepTimerSeconds > 0 && state.sleepTimerActive) {
            delay(1000)
            state.sleepTimerSeconds--
        }
        if (state.sleepTimerActive && state.sleepTimerSeconds == 0L) {
            state.sleepTimerActive = false
            state.controller?.pause()
        }
    }

    // ── ROOT LAYOUT ───────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(bg)) {
        if (state.isFullscreen && state.currentTrack?.isVideo == true) {
            FullscreenVideoView(state, tc, tcm, isDark, ctx)
        } else {
            if (state.showQueue) {
                Row(Modifier.fillMaxSize()) {
                    LibraryPane(state, isDark, ctx, surface, surfaceH, border, tc, tcs, tcm, onPickSubtitle)
                    Divider(Modifier.fillMaxHeight().width(1.dp), color = border)
                    PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
                }
            } else {
                PlayerPane(state, isDark, bg, surface, border, tc, tcs, tcm, ctx)
            }
        }
    }

    if (state.showSettings) SettingsSheet(state, isDark, tc, tcs, tcm, surface, border, ctx) { state.showSettings = false }

    state.showTagEditor?.let { t ->
        TagEditorSheet(t, isDark, tc, tcs, surface, border,
            onSave = { newTitle, newArtist, newAlbum ->
                t.editTitle  = newTitle.takeIf { it.isNotBlank() }
                t.editArtist = newArtist.takeIf { it.isNotBlank() }
                t.editAlbum  = newAlbum.takeIf { it.isNotBlank() }
                scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) }
                state.showTagEditor = null
            },
            onDismiss = { state.showTagEditor = null }
        )
    }

    state.showAddToPlaylist?.let { t ->
        AddToPlaylistDialog(
            state = state, track = t, isDark = isDark, tc = tc, tcs = tcs, tcm = tcm, surface = surface, border = border,
            onCreateNew = { name ->
                val pl = state.createPlaylist(ctx, name)
                state.addTrackToPlaylist(ctx, pl.id, t)
            },
            onToggle = { playlistId, alreadyIn ->
                if (alreadyIn) state.removeTrackFromPlaylist(ctx, playlistId, t)
                else state.addTrackToPlaylist(ctx, playlistId, t)
            },
            onDismiss = { state.showAddToPlaylist = null }
        )
    }

    if (state.showNewPlaylistDialog) {
        NamePromptDialog(
            title = "New playlist", initialValue = "", isDark = isDark, tc = tc, surface = surface, border = border,
            onConfirm = { name -> state.createPlaylist(ctx, name); state.showNewPlaylistDialog = false },
            onDismiss = { state.showNewPlaylistDialog = false }
        )
    }

    state.editingPlaylistId?.let { id ->
        val pl = state.userPlaylists.firstOrNull { it.id == id }
        if (pl != null) {
            NamePromptDialog(
                title = "Rename playlist", initialValue = pl.name, isDark = isDark, tc = tc, surface = surface, border = border,
                onConfirm = { name -> state.renamePlaylist(ctx, id, name); state.editingPlaylistId = null },
                onDismiss = { state.editingPlaylistId = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// B-04: shared video surface — one PlayerView factory used by both the
// windowed and fullscreen composables. Both just attach/detach the SAME
// controller; there is only ever one decoder running.
// ─────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(controller: MediaController?, aspectRatio: AspectRatio, modifier: Modifier = Modifier) {
    val boxModifier = when (aspectRatio) {
        AspectRatio.RATIO_4_3  -> modifier.aspectRatio(4f / 3f)
        AspectRatio.RATIO_16_9 -> modifier.aspectRatio(16f / 9f)
        else                   -> modifier.fillMaxSize()
    }
    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // B-09: force a surface reattach on resume. SurfaceView's native surface
    // is destroyed by the OS whenever the window isn't visible (screen off,
    // app backgrounded, another app in front) — that's normal Android
    // behavior, not a leak. The problem is that ExoPlayer keeps decoding
    // against the service's foreground playback the whole time, and simply
    // getting a new Surface back on return doesn't always push a fresh frame
    // promptly: what was showing before is a stale/black frame that can take
    // a while to catch up, or never does until the next seek. Detaching and
    // reattaching the player here forces PlayerView's internal SurfaceHolder
    // callback to rebind from scratch and immediately render the current
    // frame, which is the standard fix for this exact ExoPlayer symptom.
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val view = playerViewRef[0] ?: return@LifecycleEventObserver
                if (controller != null) {
                    view.player = null
                    view.player = controller
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { cx ->
            PlayerView(cx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                useController = false // we draw our own transport controls
                player = controller
                resizeMode = aspectRatio.toResizeMode()
                subtitleView?.visibility = android.view.View.VISIBLE
                playerViewRef[0] = this
            }
        },
        update = { view ->
            if (view.player !== controller) view.player = controller
            view.resizeMode = aspectRatio.toResizeMode()
        },
        onRelease = { view -> view.player = null; playerViewRef[0] = null }, // B-04: explicit cleanup, was entirely absent before
        modifier = boxModifier
    )
}

/** B-08: per-window brightness instead of a permanent system-wide Settings.System write. */
private fun setWindowBrightness(ctx: Context, fraction: Float) {
    val activity = ctx as? Activity ?: return
    val lp: WindowManager.LayoutParams = activity.window.attributes
    lp.screenBrightness = fraction.coerceIn(0.01f, 1f)
    activity.window.attributes = lp
}

// ─────────────────────────────────────────────────────────────────
// Fullscreen Video
// ─────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
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
                var dragX = 0f; var dragY = 0f; var startX = 0f
                detectDragGestures(
                    onDragStart = { offset -> dragX = 0f; dragY = 0f; startX = offset.x },
                    onDrag      = { _, d -> dragX += d.x; dragY += d.y },
                    onDragEnd   = {
                        if (abs(dragX) > abs(dragY) && abs(dragX) > 20f) {
                            val seekMs = (dragX / 8f * 1000).toLong()
                            val newPos = (state.positionMs + seekMs).coerceIn(0L, state.durationMs)
                            state.seekTo(newPos); state.positionMs = newPos
                        } else if (abs(dragY) > 20f) {
                            if (startX > size.width / 2) {
                                state.volume = (state.volume + (-dragY / 300f)).coerceIn(0f, 1f)
                            } else {
                                // B-11: brightness must read the window's *current* brightness
                                // and clamp into a valid range, same as the windowed video
                                // gesture handler below — this previously wrapped an
                                // unrelated `state.volume` read in a throwaway `.let`, which
                                // did nothing but obscure that the base value could be
                                // ScreenBrightness.BRIGHTNESS_OVERRIDE_NONE (-1f) and produce
                                // an invalid, out-of-range brightness.
                                val current = (ctx as? Activity)?.window?.attributes?.screenBrightness
                                    ?.takeIf { it in 0f..1f } ?: 0.5f
                                setWindowBrightness(ctx, current + (-dragY / 600f))
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { state.showControls = !state.showControls }) }
    ) {
        VideoSurface(state.controller, state.aspectRatio, Modifier.fillMaxSize())

        if (state.isBuffering) CircularProgressIndicator(color = FTV.Accent, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(state.showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.85f), Color.Transparent)))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { state.isFullscreen = false }) {
                        Icon(FI.FullscreenExit, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(state.currentTrack?.displayTitle ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AspectRatioChip(state, Color.White.copy(0.7f))
                    Spacer(Modifier.width(8.dp))
                    SpeedChip(state, Color.White.copy(0.7f), Color.White)
                    Spacer(Modifier.width(8.dp))
                    SleepTimerChip(state, Color.White.copy(0.7f))
                }
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
    tc: Color, tcs: Color, tcm: Color,
    onPickSubtitle: ((onPicked: (Uri) -> Unit) -> Unit)?
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.width(300.dp).fillMaxHeight().background(surface)) {
        Row(
            Modifier.fillMaxWidth().background(if (isDark) FTV.BgMid else FTV.LSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(32.dp).background(FTV.Accent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(FI.Movie, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Films & TV", color = tc, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (state.isLoading)
                    Text("Loading…", color = FTV.Accent, fontSize = 10.sp)
                else
                    Text("${state.allTracks.size} items", color = tcm, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { state.showSettings = true }, modifier = Modifier.size(28.dp)) {
                Icon(FI.Settings, null, tint = tcm, modifier = Modifier.size(16.dp))
            }
        }
        Divider(color = border)

        val tabs = listOf(
            MediaTab.MUSIC to (FI.MusicNote to FI.MusicNoteFilled),
            MediaTab.VIDEOS to (FI.MovieOutline to FI.Movie),
            MediaTab.PLAYLISTS to (FI.PlaylistPlay to FI.PlaylistPlayFilled),
            MediaTab.PLAYLIST to (FI.QueueMusic to FI.QueueMusicFilled),
            MediaTab.FOLDERS to (FI.Folder to FI.FolderFilled),
            MediaTab.RECENTS to (FI.History to FI.HistoryFilled),
            MediaTab.FAVORITES to (FI.StarBorder to FI.Star)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .background(surfaceH).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (tab, icons) ->
                val active = state.activeTab == tab
                val (iconRegular, iconFilled) = icons
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (active) FTV.Accent else Color.Transparent)
                        .clickable { state.activeTab = tab; if (tab != MediaTab.PLAYLISTS) state.openPlaylistId = null }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (active) iconFilled else iconRegular, null, tint = if (active) Color.White else tcm, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (tab) {
                            MediaTab.MUSIC -> "Music"; MediaTab.VIDEOS -> "Videos"
                            MediaTab.PLAYLISTS -> "Playlists"
                            MediaTab.PLAYLIST -> "Queue"; MediaTab.FOLDERS -> "Folders"
                            MediaTab.RECENTS -> "Recent"; MediaTab.FAVORITES -> "Faves"
                        },
                        color = if (active) Color.White else tcm, fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        val showsPlaylistFolderList = state.activeTab == MediaTab.PLAYLISTS && state.openPlaylist == null

        if (!showsPlaylistFolderList) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(FI.Search, null, tint = tcm, modifier = Modifier.size(14.dp))
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
                    Icon(FI.Close, null, tint = tcm, modifier = Modifier.size(13.dp).clickable { state.searchQuery = "" })
            }
        }

        if (state.activeTab == MediaTab.PLAYLISTS) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val openPl = state.openPlaylist
                if (openPl != null) {
                    IconButton(onClick = { state.openPlaylistId = null }, modifier = Modifier.size(32.dp)) {
                        Icon(FI.Back, null, tint = tc, modifier = Modifier.size(18.dp))
                    }
                    Text(openPl.name, color = tc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { state.playPlaylist(openPl.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(FI.Play, null, tint = FTV.Accent, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Text("Your playlists", color = tcs, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(FTV.Accent)
                            .clickable { state.showNewPlaylistDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(FI.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("New", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Divider(color = border.copy(alpha = 0.5f))
        }

        if (!showsPlaylistFolderList) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val cnt = state.filteredTracks.size
                Text("$cnt ${when(state.activeTab){ MediaTab.VIDEOS->"videos"; MediaTab.MUSIC->"tracks"; else->"items" }}", color = tcm, fontSize = 10.sp)
                if (state.activeTab == MediaTab.MUSIC || state.activeTab == MediaTab.VIDEOS) {
                    Text("Add all to queue", color = FTV.Accent, fontSize = 10.sp,
                        modifier = Modifier.clickable { state.filteredTracks.forEach { state.addToPlaylist(it) } })
                }
                if (state.activeTab == MediaTab.PLAYLIST && state.playlist.isNotEmpty()) {
                    Text("Clear queue", color = FTV.DangerRed, fontSize = 10.sp,
                        modifier = Modifier.clickable { state.clearQueue() })
                }
                if (state.activeTab == MediaTab.PLAYLISTS && state.openPlaylist != null) {
                    Text("Add tracks…", color = FTV.Accent, fontSize = 10.sp,
                        modifier = Modifier.clickable { state.activeTab = MediaTab.MUSIC })
                }
            }
            Divider(color = border.copy(alpha = 0.5f))
        }

        val listState = rememberLazyListState()
        val tracks    = state.filteredTracks
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            when (state.activeTab) {
                MediaTab.PLAYLIST -> {
                    itemsIndexed(state.playlist, key = { i, t -> "${i}_${t.metaKey}" }) { idx, t ->
                        LibraryRow(
                            track = t, isActive = state.currentIndex == idx,
                            isPlaying = state.isPlaying, isDark = isDark, tc = tc, tcs = tcs, tcm = tcm, border = border,
                            showMoveControls = true,
                            onMoveUp   = if (idx > 0) {{ state.moveInPlaylist(idx, idx - 1) }} else null,
                            onMoveDown = if (idx < state.playlist.size - 1) {{ state.moveInPlaylist(idx, idx + 1) }} else null,
                            onClick = { state.playTrackAt(idx) },
                            // B-12: these were no-ops. A track already in the queue can still
                            // usefully be duplicated to the end, or bumped to play next.
                            onAddToQueue = { state.addToPlaylist(t) },
                            onPlayNext   = { state.playNext(t) },
                            onRemoveFromQueue = { state.removeFromPlaylist(idx) },
                            onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                            onAddToPlaylist = { state.showAddToPlaylist = t },
                            onShare    = { shareTrack(ctx, t) },
                            onTagEdit  = { state.showTagEditor = t },
                            onLoadSubtitle = if (t.isVideo) { { pickSubtitleFor(t, onPickSubtitle, state) } } else null
                        )
                    }
                }
                MediaTab.PLAYLISTS -> {
                    val openPl = state.openPlaylist
                    if (openPl == null) {
                        if (state.userPlaylists.isEmpty()) {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(FI.PlaylistPlay, null, tint = tcm, modifier = Modifier.size(32.dp))
                                    Text("No playlists yet", color = tcs, fontSize = 12.sp)
                                    Text("Tap New to create one", color = tcm, fontSize = 10.sp)
                                }
                            }
                        }
                        items(state.userPlaylists, key = { it.id }) { pl ->
                            var showPlMenu by remember(pl.id) { mutableStateOf(false) }
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { state.openPlaylistId = pl.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(FTV.Accent.copy(0.15f)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(FI.PlaylistPlay, null, tint = FTV.Accent, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(pl.name, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${pl.trackKeys.size} tracks", color = tcm, fontSize = 11.sp)
                                }
                                Box {
                                    Icon(FI.MoreVert, null, tint = tcm,
                                        modifier = Modifier.size(16.dp).clickable { showPlMenu = true })
                                    DropdownMenu(expanded = showPlMenu, onDismissRequest = { showPlMenu = false },
                                        modifier = fluentMenuModifier(isDark)) {
                                        DropdownMenuItem(
                                            text = { Text("Play", color = if (isDark) FTV.Text else FTV.LText, fontSize = 13.sp) },
                                            onClick = { state.playPlaylist(pl.id); showPlMenu = false },
                                            leadingIcon = { Icon(FI.Play, null, tint = FTV.Accent, modifier = Modifier.size(16.dp)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename", color = if (isDark) FTV.Text else FTV.LText, fontSize = 13.sp) },
                                            onClick = { state.editingPlaylistId = pl.id; showPlMenu = false },
                                            leadingIcon = { Icon(FI.Edit, null, tint = FTV.Accent, modifier = Modifier.size(16.dp)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = FTV.DangerRed, fontSize = 13.sp) },
                                            onClick = { state.deletePlaylist(ctx, pl.id); showPlMenu = false },
                                            leadingIcon = { Icon(FI.Delete, null, tint = FTV.DangerRed, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }
                            }
                            Divider(color = border.copy(alpha = 0.4f))
                        }
                    } else {
                        val plTracks = state.openPlaylistTracks
                        if (plTracks.isEmpty()) {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("This playlist is empty", color = tcs, fontSize = 12.sp)
                                    Text("Long-press or use ⋮ on any track to add it here", color = tcm, fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        itemsIndexed(plTracks, key = { _, t -> "${openPl.id}_${t.metaKey}" }) { idx, t ->
                            LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playPlaylist(openPl.id, idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = { state.removeTrackFromPlaylist(ctx, openPl.id, t) },
                                removeLabel = "Remove from playlist",
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onAddToPlaylist = { state.showAddToPlaylist = t },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = t },
                                onLoadSubtitle = if (t.isVideo) { { pickSubtitleFor(t, onPickSubtitle, state) } } else null
                            )
                        }
                    }
                }
                MediaTab.FOLDERS -> {
                    state.folderGroups.forEach { (folderPath, groupTracks) ->
                        stickyHeader(key = "folder_$folderPath") {
                            GroupHeader(File(folderPath).name.ifBlank { folderPath }, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                state.playQueue(groupTracks, 0)
                            }
                        }
                        itemsIndexed(groupTracks, key = { _, t -> t.metaKey }) { idx, t ->
                            LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playQueue(groupTracks, idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onAddToPlaylist = { state.showAddToPlaylist = t },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = t },
                                onLoadSubtitle = if (t.isVideo) { { pickSubtitleFor(t, onPickSubtitle, state) } } else null
                            )
                        }
                    }
                }
                MediaTab.MUSIC -> {
                    if (state.searchQuery.isNotEmpty()) {
                        itemsIndexed(tracks) { idx, t ->
                            LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playQueue(tracks, idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onAddToPlaylist = { state.showAddToPlaylist = t },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = t },
                                onLoadSubtitle = null
                            )
                        }
                    } else {
                        state.audioGroups.forEach { (albumName, groupTracks) ->
                            stickyHeader(key = "album_$albumName") {
                                GroupHeader(albumName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    state.playQueue(groupTracks, 0)
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.metaKey }) { idx, t ->
                                LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = { state.playQueue(groupTracks, idx) },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onPlayNext   = { state.playNext(t) },
                                    onRemoveFromQueue = {},
                                    onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                    onAddToPlaylist = { state.showAddToPlaylist = t },
                                    onShare    = { shareTrack(ctx, t) },
                                    onTagEdit  = { state.showTagEditor = t },
                                    onLoadSubtitle = null
                                )
                            }
                        }
                    }
                }
                else -> {
                    val isVideos = state.activeTab == MediaTab.VIDEOS
                    if (isVideos && state.searchQuery.isEmpty()) {
                        state.videoGroups.forEach { (folderName, groupTracks) ->
                            stickyHeader(key = "vid_$folderName") {
                                GroupHeader(folderName, groupTracks.size, isDark, tc, tcm, surface, surfaceH) {
                                    state.playQueue(groupTracks, 0)
                                }
                            }
                            itemsIndexed(groupTracks, key = { _, t -> t.metaKey }) { idx, t ->
                                LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                    onClick = { state.playQueue(groupTracks, idx) },
                                    onAddToQueue = { state.addToPlaylist(t) },
                                    onPlayNext   = { state.playNext(t) },
                                    onRemoveFromQueue = {},
                                    onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                    onAddToPlaylist = { state.showAddToPlaylist = t },
                                    onShare    = { shareTrack(ctx, t) },
                                    onTagEdit  = {},
                                    onLoadSubtitle = { pickSubtitleFor(t, onPickSubtitle, state) }
                                )
                            }
                        }
                    } else {
                        itemsIndexed(tracks) { idx, t ->
                            LibraryRow(t, state.currentTrack?.metaKey == t.metaKey, state.isPlaying, isDark, tc, tcs, tcm, border,
                                onClick = { state.playQueue(tracks, idx) },
                                onAddToQueue = { state.addToPlaylist(t) },
                                onPlayNext   = { state.playNext(t) },
                                onRemoveFromQueue = {},
                                onFavorite = { t.isFavorite = !t.isFavorite; scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, t) } },
                                onAddToPlaylist = { state.showAddToPlaylist = t },
                                onShare    = { shareTrack(ctx, t) },
                                onTagEdit  = { state.showTagEditor = t },
                                onLoadSubtitle = if (t.isVideo) { { pickSubtitleFor(t, onPickSubtitle, state) } } else null
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (state.currentTrack != null) {
            Divider(color = border)
            MiniNowPlaying(state, isDark, tc, tcs, tcm)
        }
    }
}

private fun pickSubtitleFor(track: MediaTrack, onPickSubtitle: ((onPicked: (Uri) -> Unit) -> Unit)?, state: PlayerState) {
    onPickSubtitle?.invoke { uri ->
        track.subtitleUri = uri
        // Re-queue is required for ExoPlayer to pick up the new subtitle config;
        // this replaces the current item in place at the same position/time.
        val ctrl = state.controller ?: return@invoke
        val idx = state.playlist.indexOfFirst { it.metaKey == track.metaKey }
        if (idx >= 0) {
            val pos = if (idx == state.currentIndex) ctrl.currentPosition else 0L
            ctrl.replaceMediaItem(idx, track.toMediaItem())
            if (idx == state.currentIndex) ctrl.seekTo(idx, pos)
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
            Icon(FI.Play, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
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
    removeLabel: String = "Remove from queue",
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onShare: () -> Unit,
    onTagEdit: () -> Unit,
    onLoadSubtitle: (() -> Unit)? = null
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
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (track.artworkUri != null) {
                AsyncImage(model = track.artworkUri, contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)), contentScale = ContentScale.Crop,
                    error = null)
            } else {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (track.isVideo) FTV.VideoGreen.copy(0.15f) else FTV.AudioPurple.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && isPlaying) AnimatedEqualizer(FTV.Accent)
                    else Icon(if (track.isVideo) FI.PlayCircle else FI.MusicNote, null,
                        tint = if (isActive) FTV.Accent else (if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple).copy(0.7f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.displayTitle, color = if (isActive) FTV.Accent else tc, fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (track.isFavorite) Icon(FI.Star, null, tint = FTV.Gold, modifier = Modifier.size(11.dp))
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
        if (showMoveControls) {
            Column(Modifier.width(20.dp)) {
                if (onMoveUp != null)
                    Icon(FI.KeyboardArrowUp, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { onMoveUp() })
                if (onMoveDown != null)
                    Icon(FI.KeyboardArrowDown, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { onMoveDown() })
            }
        }
        Box {
            Icon(FI.MoreVert, null, tint = tcm, modifier = Modifier.size(16.dp).clickable { showMenu = true })
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                modifier = fluentMenuModifier(isDark)) {
                val items = buildList {
                    add(Triple(FI.Play, "Play now", onClick))
                    add(Triple(FI.PlaylistAdd, "Add to queue", onAddToQueue))
                    add(Triple(FI.QueuePlayNext, "Play next", onPlayNext))
                    add(Triple(FI.Remove, removeLabel, onRemoveFromQueue))
                    add(Triple(FI.LibraryAdd, "Add to playlist…", onAddToPlaylist))
                    add(Triple(if (track.isFavorite) FI.StarBorder else FI.Star,
                        if (track.isFavorite) "Unfavorite" else "Favorite", onFavorite))
                    add(Triple(FI.Edit, "Edit tags", onTagEdit))
                    add(Triple(FI.Share, "Share", onShare))
                    if (onLoadSubtitle != null) add(Triple(FI.Subtitles, "Load subtitles…", onLoadSubtitle))
                }
                items.forEach { (icon, label, action) ->
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
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(5.dp))
            .background(if (track.isVideo) FTV.VideoGreen.copy(0.2f) else FTV.AudioPurple.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUri != null) AsyncImage(track.artworkUri, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(if (track.isVideo) FI.Movie else FI.MusicNoteFilled, null,
                tint = if (track.isVideo) FTV.VideoGreen else FTV.AudioPurple, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(track.displayTitle,  color = tc,  fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(track.displayArtist, color = tcm, fontSize = 9.sp,  maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { state.togglePlayPause() }, modifier = Modifier.size(30.dp)) {
            Icon(if (state.isPlaying) FI.Pause else FI.Play, null, tint = FTV.Accent, modifier = Modifier.size(18.dp))
        }
        IconButton(
            onClick = { state.skipNext() },
            enabled = state.currentIndex < state.playlist.size - 1,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(FI.SkipNext, null,
                tint = if (state.currentIndex < state.playlist.size - 1) tcs else tcm.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp))
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
    // Dynamic accent pulled from the current track's artwork — falls back to the
    // theme accent for video, tracks with no art, or while extraction is running.
    var dynamicAccent by remember(track?.metaKey) { mutableStateOf(FTV.Accent) }

    Column(Modifier.fillMaxSize().background(bg)) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(surface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarBtn(FI.Menu, tc) { state.showQueue = !state.showQueue }
            Spacer(Modifier.width(4.dp))
            if (track != null) {
                Icon(if (track.isVideo) FI.Movie else FI.MusicNoteFilled, null, tint = FTV.Accent, modifier = Modifier.size(14.dp))
                Text(track.displayTitle, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                if (track.displayArtist != "Unknown Artist") {
                    Text("—", color = tcm, fontSize = 11.sp)
                    Text(track.displayArtist, color = tcs, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                }
            } else Text("Films & TV", color = tcs, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (track != null) {
                val scope = rememberCoroutineScope()
                IconButton(onClick = {
                    track.isFavorite = !track.isFavorite
                    // B-10 (perf): this used to write to SharedPreferences synchronously
                    // on the main/UI thread on every tap. Dispatch to IO like every other
                    // favorite toggle in this file so tapping the star never causes a
                    // dropped frame.
                    scope.launch(Dispatchers.IO) { saveTrackMeta(ctx, track) }
                }, modifier = Modifier.size(32.dp)) {
                    Icon(if (track.isFavorite) FI.Star else FI.StarBorder, null,
                        tint = if (track.isFavorite) FTV.Gold else tcm, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { state.showTagEditor = track }, modifier = Modifier.size(32.dp)) {
                    Icon(FI.Edit, null, tint = tcm, modifier = Modifier.size(15.dp))
                }
                ToolbarBtn(FI.Tune, tc) { state.showMoreControls = true }
            }
            if (track?.isVideo == true) {
                ToolbarBtn(FI.Fullscreen, tc) { state.isFullscreen = true }
            }
        }
        Divider(color = border)

        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(
                    if (track != null && !track.isVideo)
                        Brush.verticalGradient(listOf(dynamicAccent.copy(alpha = if (isDark) 0.16f else 0.08f), if (isDark) FTV.BgMid else FTV.LBg))
                    else
                        Brush.verticalGradient(listOf(if (isDark) FTV.BgMid else FTV.LBg, if (isDark) FTV.BgMid else FTV.LBg))
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                track == null  -> EmptyState(tc, tcm)
                track.isVideo  -> VideoPlayerArea(state, track, tc, tcm, ctx)
                else           -> AudioPlayerArea(state, track, isDark, tc, tcs, tcm, ctx) { dynamicAccent = it }
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(if (isDark) FTV.Bg else FTV.LBg, surface)))
                .padding(horizontal = 20.dp)
        ) {
            ProgressBar(state, tc, tcm, dynamicAccent)
            MainControls(state, tc, dynamicAccent)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (state.showMoreControls) {
        MoreControlsSheet(state, isDark, tc, tcs, tcm, surface, border, dynamicAccent) { state.showMoreControls = false }
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
                Icon(FI.MovieOutline, null, tint = FTV.Accent, modifier = Modifier.size(36.dp))
            }
        }
        Text("No media selected", color = tc, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Pick a video or song from the library", color = tcm, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────
// Video Player Area (windowed)
// ─────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
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
                            state.seekTo(newPos); state.positionMs = newPos
                            gestureLabel = if (seekMs > 0) "+${seekMs/1000}s" else "${seekMs/1000}s"
                            showGesture = true; scope.launch { delay(1000); showGesture = false }
                        } else if (abs(ty) > 20f) {
                            if (startX > size.width / 2) {
                                state.volume = (state.volume + (-ty / 300f)).coerceIn(0f, 1f)
                                gestureLabel = "Vol ${(state.volume * 100).roundToInt()}%"
                            } else {
                                // B-08: per-window brightness, no WRITE_SETTINGS permission needed
                                val activity = ctx as? Activity
                                val current = activity?.window?.attributes?.screenBrightness
                                    ?.takeIf { it in 0f..1f } ?: 0.5f
                                val newB = (current + (-ty / 600f)).coerceIn(0.01f, 1f)
                                setWindowBrightness(ctx, newB)
                                gestureLabel = "Brightness ${(newB * 100).roundToInt()}%"
                            }
                            showGesture = true; scope.launch { delay(1000); showGesture = false }
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { state.showControls = !state.showControls }) }
    ) {
        VideoSurface(state.controller, state.aspectRatio, Modifier.fillMaxSize())

        if (state.isBuffering) CircularProgressIndicator(color = FTV.Accent, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(showGesture, modifier = Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.7f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(gestureLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Audio Player Area
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AudioPlayerArea(
    state: PlayerState, track: MediaTrack, isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color,
    ctx: Context,
    onAccentExtracted: (Color) -> Unit
) {
    // Pull a dominant color out of the album art so the whole Now Playing
    // screen feels tied to *this* track instead of one static app-wide blue.
    // Falls back silently to the theme accent if there's no art or extraction
    // fails for any reason — never blocks or breaks playback.
    val minimized = LocalWindowRuntime.current.isMinimized
    LaunchedEffect(track.artworkUri, minimized) {
        if (minimized) return@LaunchedEffect
        val uri = track.artworkUri
        if (uri == null) { onAccentExtracted(FTV.Accent); return@LaunchedEffect }
        try {
            val request = ImageRequest.Builder(ctx).data(uri).allowHardware(false).build()
            val result = ctx.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    // Palette extraction is CPU-heavy. Keep it off the main thread so
                    // changing tracks never competes with playback controls/animation frames.
                    val rgb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        val palette = Palette.from(bitmap).generate()
                        (palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch)?.rgb
                    }
                    if (rgb != null) {
                        onAccentExtracted(Color(rgb))
                        return@LaunchedEffect
                    }
                }
            }
        } catch (_: Exception) { /* degrade to theme accent, same as effects failing elsewhere in this file */ }
        onAccentExtracted(FTV.Accent)
    }

    val breathe = if (!minimized) {
        val transition = rememberInfiniteTransition(label = "artBreathe")
        val value by transition.animateFloat(
            initialValue = 1f, targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "breathe"
        )
        value
    } else 1f

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier
                .size(268.dp)
                .graphicsLayer {
                    val s = if (state.isPlaying) breathe else 1f
                    scaleX = s; scaleY = s
                }
                .shadow(24.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(FTV.AudioPurple.copy(0.25f), FTV.Accent.copy(0.15f)))),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUri != null) {
                AsyncImage(model = track.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(FI.MusicNote, null, tint = FTV.AudioPurple, modifier = Modifier.size(84.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.displayTitle, color = tc, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp))
            Text(track.displayArtist, color = tcs, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (state.playlist.size > 1 && state.currentIndex < state.playlist.size - 1) {
            val next = state.playlist[state.currentIndex + 1]
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(FI.QueueMusic, null, tint = tcm, modifier = Modifier.size(14.dp))
                Text("Up next: ${next.displayTitle}", color = tcs, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated equalizer bars (playing indicator)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedEqualizer(color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.size(16.dp)) {
        repeat(3) { i ->
            val height by transition.animateFloat(
                initialValue = 4f, targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + i * 120, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ), label = "bar$i"
            )
            Box(Modifier.width(3.dp).height(height.dp).background(color, RoundedCornerShape(1.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Progress bar — B-03: seeks on release, not per pixel of drag
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ProgressBar(state: PlayerState, tc: Color, tcm: Color, accent: Color = FTV.Accent) {
    var seekPreview by remember { mutableStateOf<Float?>(null) }
    val displayedProgress = seekPreview
        ?: if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration((displayedProgress * state.durationMs).toLong()), color = tcm, fontSize = 11.sp)
            Text("-${formatDuration((state.durationMs - (displayedProgress * state.durationMs).toLong()).coerceAtLeast(0))}", color = tcm, fontSize = 11.sp)
            Text(formatDuration(state.durationMs), color = tcm, fontSize = 11.sp)
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(tc.copy(0.15f))) {
                Box(Modifier.fillMaxWidth(displayedProgress).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent)), RoundedCornerShape(2.dp)))
            }
            Slider(
                value = displayedProgress,
                onValueChange = { frac -> seekPreview = frac }, // B-03: preview only, no seek yet
                onValueChangeFinished = {
                    val frac = seekPreview ?: return@Slider
                    state.seekTo((frac * state.durationMs).toLong())
                    seekPreview = null
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
private fun MainControls(state: PlayerState, tc: Color, accent: Color = FTV.Accent) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlBtn(FI.Shuffle, if (state.isShuffle) accent else tc.copy(0.5f), 20.dp) { state.isShuffle = !state.isShuffle }
        ControlBtn(FI.SkipPrevious, tc, 28.dp) { state.skipPrev() }
        Box(
            Modifier.size(72.dp)
                .shadow(14.dp, CircleShape, spotColor = accent, ambientColor = accent)
                .background(Brush.radialGradient(listOf(accent, accent.copy(alpha = 0.85f))), CircleShape)
                .clickable { state.togglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (state.isPlaying) FI.Pause else FI.Play, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        ControlBtn(FI.SkipNext, tc, 28.dp) { state.skipNext() }
        ControlBtn(
            when (state.repeatMode) { RepeatMode.REPEAT_ONE -> FI.RepeatOne; else -> FI.Repeat },
            if (state.repeatMode != RepeatMode.OFF) accent else tc.copy(0.5f), 20.dp
        ) { state.repeatMode = RepeatMode.entries[(state.repeatMode.ordinal + 1) % RepeatMode.entries.size] }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlBtn(FI.Replay10, tc.copy(0.7f), 20.dp) { state.seekTo((state.positionMs - 10000).coerceAtLeast(0)) }
        Spacer(Modifier.width(48.dp))
        ControlBtn(FI.Forward10, tc.copy(0.7f), 20.dp) { state.seekTo((state.positionMs + 10000).coerceAtMost(state.durationMs)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// Volume control
// ─────────────────────────────────────────────────────────────────

// NOTE: not currently wired into any toolbar — volume is exposed today via
// MoreControlsSheet's full-width slider instead. Left in place (and kept
// visually consistent with the rest of the Fluent-styled chips/menus) in
// case a compact toolbar affordance is wanted later; safe to delete otherwise.
@Composable
private fun VolumeControl(state: PlayerState, isDark: Boolean, tc: Color, tcm: Color) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ToolbarBtn(
            when { state.isMuted || state.volume == 0f -> FI.VolumeOff
                   state.volume < 0.5f -> FI.VolumeDown
                   else -> FI.VolumeUp },
            tc
        ) { expanded = !expanded }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = fluentMenuModifier(isDark)) {
            Column(Modifier.width(160.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Slider(
                    value = if (state.isMuted) 0f else state.volume,
                    onValueChange = { state.volume = it; state.isMuted = false },
                    colors = SliderDefaults.colors(thumbColor = FTV.Accent, activeTrackColor = FTV.Accent)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Speed chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SpeedChip(state: PlayerState, bgTint: Color, tc: Color) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).background(bgTint.copy(0.12f))
                .clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { Text(speedLabel(state.playbackSpeed), color = tc, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = fluentFlyoutOnScrimModifier()) {
            SPEED_STEPS.forEach { s ->
                val active = state.playbackSpeed == s
                DropdownMenuItem(
                    text = { Text(speedLabel(s), color = Color.White, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { state.setSpeed(s); expanded = false },
                    trailingIcon = if (active) { { Icon(FI.Check, null, tint = FTV.Accent, modifier = Modifier.size(16.dp)) } } else null
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
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).background(bgTint.copy(0.12f))
                .clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(FI.Timer, null, tint = bgTint, modifier = Modifier.size(13.dp))
            if (state.sleepTimerActive) Text(formatTimer(state.sleepTimerSeconds), color = bgTint, fontSize = 11.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = fluentFlyoutOnScrimModifier()) {
            if (state.sleepTimerActive) {
                DropdownMenuItem(
                    text = { Text("Cancel timer", color = FTV.DangerRed) },
                    onClick = { state.sleepTimerActive = false; expanded = false },
                    leadingIcon = { Icon(FI.Close, null, tint = FTV.DangerRed, modifier = Modifier.size(16.dp)) }
                )
            } else {
                SLEEP_OPTIONS.forEach { (min, label) ->
                    DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = {
                        state.sleepTimerSeconds = min * 60L; state.sleepTimerActive = true; expanded = false
                    })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// More controls sheet — houses Speed / Sleep Timer / Volume (and
// Aspect Ratio for video) as full-width rows instead of cramming them
// into the toolbar as tiny chips. Opened from the single "Tune" icon
// in PlayerPane's toolbar.
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MoreControlsSheet(
    state: PlayerState, isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color,
    surface: Color, border: Color,
    accent: Color,
    onDismiss: () -> Unit
) {
    val track = state.currentTrack
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (isDark) FTV.Surface else FTV.LSurface)
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(border))
            }

            // Playback speed
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Playback speed", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(speedLabel(state.playbackSpeed), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPEED_STEPS.forEach { s ->
                        val active = state.playbackSpeed == s
                        Text(
                            speedLabel(s), color = if (active) Color.White else tc, fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (active) accent else (if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh))
                                .clickable { state.setSpeed(s) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Divider(color = border)

            // Sleep timer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep timer", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (state.sleepTimerActive) {
                        Text(formatTimer(state.sleepTimerSeconds), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.sleepTimerActive) {
                        Text(
                            "Cancel", color = FTV.DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(FTV.DangerRed.copy(0.12f))
                                .clickable { state.sleepTimerActive = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        SLEEP_OPTIONS.forEach { (min, label) ->
                            Text(
                                label, color = tc, fontSize = 12.sp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                                    .clickable { state.sleepTimerSeconds = min * 60L; state.sleepTimerActive = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = border)

            // Volume
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Volume", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        when { state.isMuted || state.volume == 0f -> FI.VolumeOff
                               state.volume < 0.5f -> FI.VolumeDown
                               else -> FI.VolumeUp },
                        null, tint = tc, modifier = Modifier.size(20.dp).clickable { state.isMuted = !state.isMuted }
                    )
                    Slider(
                        value = if (state.isMuted) 0f else state.volume,
                        onValueChange = { state.volume = it; state.isMuted = false },
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (track?.isVideo == true) {
                Divider(color = border)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aspect ratio", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AspectRatio.values().forEach { ar ->
                            val active = state.aspectRatio == ar
                            Text(
                                ar.label, color = if (active) Color.White else tc, fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (active) accent else (if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh))
                                    .clickable { state.aspectRatio = ar }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Aspect ratio chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AspectRatioChip(state: PlayerState, tint: Color) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).background(tint.copy(0.12f))
                .clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { Text(state.aspectRatio.label, color = tint, fontSize = 11.sp) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = fluentFlyoutOnScrimModifier()) {
            AspectRatio.entries.forEach { ar ->
                val active = state.aspectRatio == ar
                DropdownMenuItem(
                    text = { Text(ar.label, color = Color.White, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { state.aspectRatio = ar; expanded = false },
                    trailingIcon = if (active) { { Icon(FI.Check, null, tint = FTV.Accent, modifier = Modifier.size(16.dp)) } } else null
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Settings sheet (EQ, bass boost, virtualizer, crossfade)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSheet(
    state: PlayerState, isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color,
    surface: Color, border: Color,
    ctx: Context,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).clickable(onClick = onDismiss), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.width(320.dp).fillMaxHeight()
                .shadow(16.dp, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .background(if (isDark) FTV.Surface else FTV.LSurface)
                .clickable(enabled = false) {}
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = tc, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(FI.Close, null, tint = tcm) }
            }
            Divider(color = border)

            Text("Theme", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BBTheme.values().forEach { theme ->
                    val active = ThemeHolder.current == theme
                    Column(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (active) theme.palette.accent.copy(0.15f) else (if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh))
                            .border(1.dp, if (active) theme.palette.accent else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { saveTheme(ctx, theme) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(theme.palette.accent))
                        Text(theme.label, color = tc, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            Divider(color = border)

            Text("Equalizer", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EqPreset.values().forEach { preset ->
                    val active = state.eqPreset == preset
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (active) FTV.Accent else (if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh))
                            .clickable { state.applyEqPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(preset.label, color = if (active) Color.White else tc, fontSize = 12.sp) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Bass Boost", color = tc, fontSize = 14.sp)
                    Text("Enhance low frequencies — now applies to video too", color = tcm, fontSize = 11.sp)
                }
                Switch(checked = state.bassBoostOn, onCheckedChange = { state.setBassBoost(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FTV.Accent))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("3D Virtualizer", color = tc, fontSize = 14.sp)
                    Text("Spacious surround sound", color = tcm, fontSize = 11.sp)
                }
                Switch(checked = state.virtualizerOn, onCheckedChange = { state.setVirtualizer(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FTV.Accent))
            }

            Divider(color = border)

            Text("Crossfade", color = tcs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration", color = tc, fontSize = 14.sp)
                    Text(if (state.crossfadeSec == 0) "Off" else "${state.crossfadeSec}s", color = FTV.Accent, fontSize = 14.sp)
                }
                Slider(
                    value = state.crossfadeSec.toFloat(), onValueChange = { state.setCrossfade(it.roundToInt()) },
                    valueRange = 0f..10f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = FTV.Accent, activeTrackColor = FTV.Accent)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Tag editor sheet
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
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Edit Tags", color = tc, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Changes are saved in-app only (does not modify the file)", color = tcs, fontSize = 11.sp)
            Divider(color = border)
            listOf(
                Triple("Title", title) { v: String -> title = v },
                Triple("Artist", artist) { v: String -> artist = v },
                Triple("Album", album) { v: String -> album = v }
            ).forEach { (label, value, setter) ->
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
// Share track
// ─────────────────────────────────────────────────────────────────

private fun shareTrack(ctx: Context, track: MediaTrack) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (track.isVideo) "video/*" else "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.contentUri)
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

// ─────────────────────────────────────────────────────────────────
// Add-to-playlist dialog — lists all playlists with a checkbox-style
// toggle for "is this track in it", plus an inline "new playlist" row.
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AddToPlaylistDialog(
    state: PlayerState,
    track: MediaTrack,
    isDark: Boolean,
    tc: Color, tcs: Color, tcm: Color,
    surface: Color, border: Color,
    onCreateNew: (String) -> Unit,
    onToggle: (playlistId: String, alreadyIn: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showNewRow by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(320.dp).heightIn(max = 480.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) FTV.Surface else FTV.LSurface)
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Add to playlist", color = tc, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(track.displayTitle, color = tcs, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(FI.Close, null, tint = tcm, modifier = Modifier.size(16.dp)) }
            }
            Divider(color = border)

            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (state.userPlaylists.isEmpty() && !showNewRow) {
                    Text("No playlists yet — create one below", color = tcm, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
                state.userPlaylists.forEach { pl ->
                    val alreadyIn = track.metaKey in pl.trackKeys
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(pl.id, alreadyIn) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (alreadyIn) FI.CheckBox else FI.CheckBoxOutline, null,
                            tint = if (alreadyIn) FTV.Accent else tcm, modifier = Modifier.size(18.dp)
                        )
                        Text(pl.name, color = tc, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${pl.trackKeys.size}", color = tcm, fontSize = 11.sp)
                    }
                }
            }

            Divider(color = border)

            if (showNewRow) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (newName.isEmpty()) Text("Playlist name…", color = tcm, fontSize = 12.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = newName, onValueChange = { newName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 13.sp),
                            cursorBrush = SolidColor(FTV.Accent), singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = {
                        if (newName.isNotBlank()) { onCreateNew(newName); showNewRow = false; newName = "" }
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(FI.Check, null, tint = FTV.Accent, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clickable { showNewRow = true }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(FI.Add, null, tint = FTV.Accent, modifier = Modifier.size(18.dp))
                    Text("New playlist", color = FTV.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Generic single-text-field prompt — used for both "new playlist" and
// "rename playlist" so we don't duplicate the same little dialog twice.
// ─────────────────────────────────────────────────────────────────

@Composable
private fun NamePromptDialog(
    title: String,
    initialValue: String,
    isDark: Boolean,
    tc: Color,
    surface: Color, border: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(300.dp).clip(RoundedCornerShape(12.dp))
                .background(if (isDark) FTV.Surface else FTV.LSurface)
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, color = tc, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) FTV.SurfaceHigh else FTV.LSurfaceHigh)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = value, onValueChange = { value = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = tc, fontSize = 14.sp),
                    cursorBrush = SolidColor(FTV.Accent), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = FTV.TextMuted, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", color = FTV.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { if (value.isNotBlank()) onConfirm(value) }.padding(8.dp))
            }
        }
    }
}
