package com.bluebird.media

// ─────────────────────────────────────────────────────────────────
// MediaLibraryRepository.kt
//
// This is the direct fix for "every time it's opened, it has to scan for
// video and music files, which is a long wait."
//
// The old implementation did two expensive things on every launch:
//   1. root.walkTopDown().maxDepth(6) over five storage roots, checking
//      every file's extension by hand.
//   2. For every matched file, opened a fresh MediaMetadataRetriever just
//      to read title/artist/album/duration and pull embedded album art
//      bytes into memory.
//
// Both of those already happen once, continuously, in the background —
// that's what MediaStore is. It's Android's system-maintained index of
// every media file on the device, kept up to date by MediaScanner. A
// single ContentResolver query against it returns title/artist/album/
// duration/path for the entire library, typically in well under 100ms
// regardless of library size, with no filesystem traversal and no
// per-file retriever calls.
//
// Album art is no longer eagerly decoded into a ByteArray per track.
// MediaStore exposes a content:// artwork/thumbnail Uri that Coil's
// AsyncImage can load directly and lazily (only for rows actually on
// screen), which is both faster and dramatically lighter on memory for
// large libraries.
// ─────────────────────────────────────────────────────────────────

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ScannedTrack(
    val contentUri: Uri,
    val file: File?,           // best-effort real path; may be null under strict scoped storage
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val artworkUri: Uri?,
    val dateModified: Long
)

object MediaLibraryRepository {

    /** Fast MediaStore query for both audio and video. Safe to call on Dispatchers.IO. */
    suspend fun scan(context: Context, includeAudio: Boolean = true, includeVideo: Boolean = true): List<ScannedTrack> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<ScannedTrack>(512)
            if (includeAudio) out += queryAudio(context)
            if (includeVideo) out += queryVideo(context)
            out
        }

    private fun queryAudio(context: Context): List<ScannedTrack> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val results = ArrayList<ScannedTrack>()

        context.contentResolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val trackUri = ContentUris.withAppendedId(uri, id)
                    val albumId = c.getLong(albumIdCol)
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    )
                    val path = c.getStringOrNull(dataCol)
                    results += ScannedTrack(
                        contentUri = trackUri,
                        file = path?.let { File(it) },
                        title = c.getStringOrNull(titleCol) ?: (path?.let(::File)?.nameWithoutExtension ?: "Unknown"),
                        artist = c.getStringOrNull(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                        album = c.getStringOrNull(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album",
                        durationMs = c.getLong(durCol),
                        isVideo = false,
                        artworkUri = artUri,
                        dateModified = c.getLong(dateCol)
                    )
                }
            }
        return results
    }

    private fun queryVideo(context: Context): List<ScannedTrack> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val results = ArrayList<ScannedTrack>()

        context.contentResolver.query(uri, projection, null, null, "${MediaStore.Video.Media.TITLE} ASC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val trackUri = ContentUris.withAppendedId(uri, id)
                    // Video thumbnails: MediaStore.Video.Thumbnails is deprecated on API 29+ in
                    // favor of ContentResolver.loadThumbnail(uri, size, signal) — do that lazily
                    // at the call site (e.g. inside a Coil fetcher) rather than here, so scanning
                    // stays a metadata-only query with no bitmap decoding.
                    val path = c.getStringOrNull(dataCol)
                    results += ScannedTrack(
                        contentUri = trackUri,
                        file = path?.let { File(it) },
                        title = c.getStringOrNull(titleCol) ?: (path?.let(::File)?.nameWithoutExtension ?: "Unknown"),
                        artist = "Unknown Artist",
                        album = "Unknown Album",
                        durationMs = c.getLong(durCol),
                        isVideo = true,
                        artworkUri = trackUri, // Coil can resolve a video content Uri to a frame thumbnail
                        dateModified = c.getLong(dateCol)
                    )
                }
            }
        return results
    }

    private fun Cursor.getStringOrNull(col: Int): String? =
        if (isNull(col)) null else getString(col)
}
