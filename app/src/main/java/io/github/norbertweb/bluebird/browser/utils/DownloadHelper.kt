package io.github.norbertweb.bluebird.browser.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.webkit.URLUtil
import io.github.norbertweb.bluebird.browser.model.DownloadItem
import io.github.norbertweb.bluebird.browser.model.DownloadStatus
import kotlinx.coroutines.*
import java.io.File

/**
 * Bluebird download bridge.
 * Android DownloadManager owns the actual transfer; Bluebird only keeps a
 * lightweight, persistent presentation of each download.
 */
class DownloadHelper(private val context: Context) {
    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<Long, String>()
    private val onCompletionCallbacks = mutableMapOf<String, (DownloadStatus) -> Unit>()

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val dmId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val ourId = activeJobs.remove(dmId) ?: return
            val callback = onCompletionCallbacks.remove(ourId) ?: return
            callback(queryFinalStatus(dmId))
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(completionReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(completionReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun uniqueFileName(candidate: String): String {
        val safe = candidate.ifBlank { "Download" }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!File(dir, safe).exists()) return safe
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        for (i in 1..999) {
            val name = "$base ($i)$ext"
            if (!File(dir, name).exists()) return name
        }
        return "$base-${System.currentTimeMillis()}$ext"
    }

    fun enqueue(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        onProgress: (Float, Long) -> Unit,
        onComplete: (DownloadStatus) -> Unit
    ): DownloadItem {
        val fileName = uniqueFileName(URLUtil.guessFileName(url, contentDisposition, mimeType))
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            if (mimeType.isNotBlank()) setMimeType(mimeType)
            if (userAgent.isNotBlank()) addRequestHeader("User-Agent", userAgent)
            setTitle(fileName)
            setDescription("Downloading via Bluebird")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dmId = dm.enqueue(request)
        val item = DownloadItem(
            downloadManagerId = dmId,
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            fileSize = contentLength,
            status = DownloadStatus.DOWNLOADING
        )
        activeJobs[dmId] = item.id
        onCompletionCallbacks[item.id] = onComplete
        scope.launch {
            while (activeJobs.containsKey(dmId)) {
                val (progress, bytes) = queryProgress(dmId)
                withContext(Dispatchers.Main) { onProgress(progress, bytes) }
                delay(700)
            }
        }
        return item
    }

    fun retry(
        item: DownloadItem,
        onProgress: (Float, Long) -> Unit,
        onComplete: (DownloadStatus) -> Unit
    ): DownloadItem {
        return enqueue(
            url = item.url,
            userAgent = android.webkit.WebSettings.getDefaultUserAgent(context),
            contentDisposition = "",
            mimeType = item.mimeType,
            contentLength = item.fileSize,
            onProgress = onProgress,
            onComplete = onComplete
        )
    }

    fun cancel(dmId: Long): Boolean {
        val ourId = activeJobs.remove(dmId)
        if (ourId != null) onCompletionCallbacks.remove(ourId)
        return dm.remove(dmId) > 0
    }

    private fun queryProgress(dmId: Long): Pair<Float, Long> {
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        return try {
            if (cursor != null && cursor.moveToFirst()) {
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                Pair(if (total > 0) soFar.toFloat() / total else 0f, soFar)
            } else Pair(0f, 0L)
        } finally { cursor?.close() }
    }

    private fun queryFinalStatus(dmId: Long): DownloadStatus {
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        return try {
            if (cursor != null && cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                    else -> DownloadStatus.FAILED
                }
            } else DownloadStatus.FAILED
        } finally { cursor?.close() }
    }

    fun reconcile(items: List<DownloadItem>): List<DownloadItem> = items.map { item ->
        if (item.status == DownloadStatus.CANCELLED) return@map item
        val cursor = if (item.downloadManagerId >= 0L) dm.query(DownloadManager.Query().setFilterById(item.downloadManagerId)) else null
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val soFar = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val state = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                    else -> DownloadStatus.FAILED
                }
                return@map item.copy(status = state, fileSize = if (total > 0) total else item.fileSize,
                    bytesDownloaded = soFar, progress = if (total > 0) soFar.toFloat() / total else item.progress)
            }
        }
        item.copy(status = DownloadStatus.FAILED)
    }

    fun open(downloadManagerId: Long): Boolean {
        val uri = dm.getUriForDownloadedFile(downloadManagerId) ?: return false
        val mime = runCatching {
            dm.query(DownloadManager.Query().setFilterById(downloadManagerId))?.use {
                if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)) else null
            }
        }.getOrNull() ?: "*/*"
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrDefault(false)
    }

    fun openDownloadsFolder(): Boolean = runCatching {
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun remove(downloadManagerId: Long): Boolean {
        activeJobs.remove(downloadManagerId)
        return if (downloadManagerId >= 0L) dm.remove(downloadManagerId) > 0 else false
    }

    fun destroy() {
        scope.cancel()
        activeJobs.clear()
        onCompletionCallbacks.clear()
        try { context.unregisterReceiver(completionReceiver) } catch (_: Exception) {}
    }
}
