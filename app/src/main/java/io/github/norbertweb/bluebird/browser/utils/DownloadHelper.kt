package com.io.github.norbertweb.bluebird.browser.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import com.io.github.norbertweb.bluebird.browser.model.DownloadItem
import com.io.github.norbertweb.bluebird.browser.model.DownloadStatus
import kotlinx.coroutines.*

// ═══════════════════════════════════════════════════════════════════════
// DownloadHelper — enqueues downloads, tracks real progress via
// polling the DownloadManager database, fires callbacks on completion.
// ═══════════════════════════════════════════════════════════════════════

class DownloadHelper(private val context: Context) {

    private val dm: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Maps DownloadManager ID → our DownloadItem ID
    private val activeJobs = mutableMapOf<Long, String>()

    // ── Receiver for completion events ──────────────────────────────

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val dmId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val ourId = activeJobs[dmId] ?: return
            onCompletionCallbacks[ourId]?.invoke(queryFinalStatus(dmId))
            activeJobs.remove(dmId)
            onCompletionCallbacks.remove(ourId)
        }
    }

    private val onCompletionCallbacks = mutableMapOf<String, (DownloadStatus) -> Unit>()

    init {
        context.registerReceiver(
            completionReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    // ── Enqueue ─────────────────────────────────────────────────────

    /**
     * Starts a download. Returns a DownloadItem with the real DM id.
     * [onProgress] fires every ~500ms while downloading.
     * [onComplete] fires once when done.
     */
    fun enqueue(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        onProgress: (Float, Long) -> Unit,
        onComplete: (DownloadStatus) -> Unit
    ): DownloadItem {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request  = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            addRequestHeader("User-Agent", userAgent)
            setTitle(fileName)
            setDescription("Downloading via Bluebird Surfer…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dmId = dm.enqueue(request)
        val item  = DownloadItem(
            downloadManagerId = dmId,
            fileName          = fileName,
            url               = url,
            mimeType          = mimeType,
            fileSize          = contentLength,
            status            = DownloadStatus.DOWNLOADING
        )

        activeJobs[dmId] = item.id
        onCompletionCallbacks[item.id] = onComplete

        // Start progress polling
        scope.launch {
            while (activeJobs.containsKey(dmId)) {
                val (progress, bytes) = queryProgress(dmId)
                withContext(Dispatchers.Main) { onProgress(progress, bytes) }
                delay(500)
            }
        }

        return item
    }

    // ── Query helpers ────────────────────────────────────────────────

    private fun queryProgress(dmId: Long): Pair<Float, Long> {
        val query  = DownloadManager.Query().setFilterById(dmId)
        val cursor: Cursor? = dm.query(query)
        return try {
            if (cursor != null && cursor.moveToFirst()) {
                val total     = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val soFar     = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val progress  = if (total > 0) (soFar.toFloat() / total) else 0f
                Pair(progress, soFar)
            } else Pair(0f, 0L)
        } finally {
            cursor?.close()
        }
    }

    private fun queryFinalStatus(dmId: Long): DownloadStatus {
        val query  = DownloadManager.Query().setFilterById(dmId)
        val cursor: Cursor? = dm.query(query)
        return try {
            if (cursor != null && cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_PAUSED     -> DownloadStatus.PAUSED
                    else                              -> DownloadStatus.FAILED
                }
            } else DownloadStatus.FAILED
        } finally {
            cursor?.close()
        }
    }

    fun destroy() {
        scope.cancel()
        try { context.unregisterReceiver(completionReceiver) } catch (_: Exception) {}
    }
}
