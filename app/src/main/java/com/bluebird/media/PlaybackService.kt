package com.bluebird.media

// ─────────────────────────────────────────────────────────────────
// PlaybackService.kt  —  Bluebird Films & TV playback engine
//
// This replaces the raw android.media.MediaPlayer / android.widget.VideoView
// that used to live inside MediaPlayerScreen.kt. Rationale:
//
//  - MediaSessionService gives lock-screen + notification transport controls
//    (play/pause/next/prev/seek) FOR FREE via Media3's default notification
//    provider — this was entirely missing before.
//  - It's a foreground service, so playback survives navigating away from
//    the screen (the old DisposableEffect(currentPath) released the
//    MediaPlayer the instant the composable left composition).
//  - ExoPlayer handles seamless/gapless queue transitions natively, so the
//    old fake `nextMediaPlayer` field (declared, never used) is gone —
//    replaced with an actual working feature.
//  - Playback speed uses Player.setPlaybackSpeed() — no more reflecting
//    into VideoView's private mMediaPlayer field, which was liable to be
//    blocked by hidden-API restrictions on modern Android anyway.
//  - Equalizer/BassBoost/Virtualizer attach to the player's real
//    audioSessionId, which now applies to VIDEO too, not just audio
//    (previously EQ silently did nothing for video playback).
//
// State (queue index / position) is persisted here, throttled to once
// every 3s + on pause/stop/destroy — NOT on every position tick like the
// old saveQueue() LaunchedEffect, which was writing to SharedPreferences
// ~4x/second continuously during playback.
// ─────────────────────────────────────────────────────────────────

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

@UnstableApi
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Crossfade — implemented as volume automation across ExoPlayer's native
    // gapless transition, rather than a real dual-player mix. That's a
    // deliberate scope call: it delivers an audible crossfade with a single
    // player and no extra decoder overhead, at the cost of a brief volume
    // dip rather than true overlapping playback. Good enough for a "Duration"
    // slider that previously did nothing at all.
    @Volatile var crossfadeSec: Int = 0
    private var fadeJob: Job? = null
    private var baseVolume: Float = 1f

    private var saveJob: Job? = null

    companion object {
        const val CMD_SET_EQ_PRESET   = "bluebird.SET_EQ_PRESET"   // args: IntArray "value" (band gains, mB)
        const val CMD_SET_BASS_BOOST  = "bluebird.SET_BASS_BOOST"  // args: Boolean "value"
        const val CMD_SET_VIRTUALIZER = "bluebird.SET_VIRTUALIZER" // args: Boolean "value"
        const val CMD_SET_CROSSFADE   = "bluebird.SET_CROSSFADE"   // args: Int "value" (0-10 sec)
        const val ARG_VALUE = "value"
        const val PREFS_NAME = "bluebird_player"
        private const val SAVE_DEBOUNCE_MS = 3000L
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true) // pause on headphone unplug — table stakes, was absent before
            .build()
        player.addListener(playerListener)

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivity = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        session = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .also { b -> sessionActivity?.let { b.setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onDestroy() {
        saveJob?.cancel()
        saveState()
        releaseEffects()
        fadeJob?.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    // If the user swipes the app away from recents while paused (or nothing
    // is queued), let the foreground service actually stop instead of
    // lingering — otherwise it'll sit in the notification shade forever.
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            player.stop()
            stopSelf()
        }
    }

    // ── Player listener ────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEffects(audioSessionId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            scheduleSave()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            scheduleSave()
            if (crossfadeSec > 0 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                fadeIn()
            }
        }

        override fun onEvents(p: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                maybeStartCrossfadeOut()
            }
        }
    }

    // ── Custom session commands (EQ / bass / virtualizer / crossfade) ─
    // MediaController on the UI side calls these; effects must live in this
    // process because android.media.audiofx attaches to a real AudioTrack,
    // which only exists here, inside the player.

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(CMD_SET_EQ_PRESET, Bundle.EMPTY))
                .add(SessionCommand(CMD_SET_BASS_BOOST, Bundle.EMPTY))
                .add(SessionCommand(CMD_SET_VIRTUALIZER, Bundle.EMPTY))
                .add(SessionCommand(CMD_SET_CROSSFADE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onCustomCommand(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SET_EQ_PRESET -> {
                    val gains = args.getIntArray(ARG_VALUE)
                        ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    applyEqGains(gains)
                }
                CMD_SET_BASS_BOOST  -> bassBoost?.enabled = args.getBoolean(ARG_VALUE)
                CMD_SET_VIRTUALIZER -> virtualizer?.enabled = args.getBoolean(ARG_VALUE)
                CMD_SET_CROSSFADE   -> crossfadeSec = args.getInt(ARG_VALUE).coerceIn(0, 10)
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    // ── Effects ─────────────────────────────────────────────────────

    private fun attachEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        releaseEffects()
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false; setStrength(500) }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = false; setStrength(500) }
        } catch (_: Exception) {
            // Effect not supported for this session on this device — degrade silently,
            // UI toggle simply won't do anything audible, same as before.
        }
    }

    private fun applyEqGains(gains: IntArray) {
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            gains.take(bands).forEachIndexed { i, g -> eq.setBandLevel(i.toShort(), g.toShort()) }
        } catch (_: Exception) {}
    }

    private fun releaseEffects() {
        try { equalizer?.release(); bassBoost?.release(); virtualizer?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null
    }

    // ── Crossfade (see class-level note above) ─────────────────────

    private fun maybeStartCrossfadeOut() {
        if (crossfadeSec <= 0) return
        val dur = player.duration
        if (dur == C.TIME_UNSET || dur <= 0) return
        val remaining = dur - player.currentPosition
        val fadeMs = crossfadeSec * 1000L
        if (remaining in 1..fadeMs && fadeJob?.isActive != true && player.hasNextMediaItem()) {
            fadeJob = scope.launch {
                val steps = 16
                val stepDelay = (remaining / steps).coerceAtLeast(15)
                for (i in steps downTo 0) {
                    player.volume = baseVolume * (i / steps.toFloat())
                    delay(stepDelay)
                }
            }
        }
    }

    private fun fadeIn() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 16
            val stepDelay = (crossfadeSec * 1000L / steps).coerceAtLeast(15)
            for (i in 0..steps) {
                player.volume = baseVolume * (i / steps.toFloat())
                delay(stepDelay)
            }
            player.volume = baseVolume
        }
    }

    // ── Persistence — throttled ─────────────────────────────────────

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch { delay(SAVE_DEBOUNCE_MS); saveState() }
    }

    private fun saveState() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            for (i in 0 until player.mediaItemCount) arr.put(player.getMediaItemAt(i).mediaId)
            prefs.edit()
                .putString("queue_ids", arr.toString())
                .putInt("queue_index", player.currentMediaItemIndex)
                .putLong("queue_pos", player.currentPosition.coerceAtLeast(0))
                .apply()
        } catch (_: Exception) {}
    }
}
