package com.bluebird.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// ─────────────────────────────────────────────────────────────────────────────
// NOTIFICATION HELPER
// ─────────────────────────────────────────────────────────────────────────────

object UpdateNotificationHelper {

    private const val CHANNEL_ID_UPDATES   = "bluebird_updates"
    private const val CHANNEL_ID_INSTALLED = "bluebird_installed"
    private const val NOTIF_ID_UPDATE      = 1001
    private const val NOTIF_ID_INSTALLED   = 1002

    /** Call once on app start (safe to call repeatedly). */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_UPDATES,
                    "Launcher Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies when a new launcher version is available"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_INSTALLED,
                    "Update Installed",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Confirms a launcher update was applied"
                }
            )
        }
    }

    /**
     * Fires a system notification telling the user a new version is ready.
     * Tapping it opens the release URL in a browser (external flow).
     */
    fun notifyUpdateAvailable(
        context: Context,
        manifest: UpdateManifest,
        deliveryMode: UpdateDelivery
    ) {
        if (!areNotificationsEnabled(context)) return

        val tapIntent: PendingIntent = if (deliveryMode == UpdateDelivery.EXTERNAL
            && manifest.apkUrl.isNotEmpty()
        ) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(manifest.apkUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(
                context, 0, browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // For internal delivery, open the launcher's main activity
            val mainIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: Intent()
            PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val changelogSummary = manifest.changelog.take(3).joinToString("\n• ", prefix = "• ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Bluebird ${manifest.versionName} is available")
            .setContentText("Tap to update${if (manifest.apkSize.isNotEmpty()) " · ${manifest.apkSize}" else ""}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("What's new:\n$changelogSummary")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_UPDATE, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }

    /**
     * Fires a low-priority notification confirming the update was installed.
     */
    fun notifyUpdateInstalled(context: Context, versionName: String) {
        if (!areNotificationsEnabled(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INSTALLED)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Bluebird updated to $versionName")
            .setContentText("The launcher was successfully updated. Enjoy the new features!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_INSTALLED, notification)
        } catch (_: SecurityException) { /* no permission */ }
    }

    /** Cancel any pending update notification (e.g. after user dismisses the update). */
    fun cancelUpdateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_UPDATE)
    }

    private fun areNotificationsEnabled(context: Context) =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
