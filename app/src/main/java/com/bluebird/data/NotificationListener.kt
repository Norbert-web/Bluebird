package com.bluebird.data

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.bluebird.RealNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        var instance: NotificationListener? = null
        var onNotificationPosted: ((RealNotification) -> Unit)? = null
        var onNotificationRemoved: ((String) -> Unit)? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        try {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: return
            val body = extras.getCharSequence("android.text")?.toString() ?: ""
            val pm = applicationContext.packageManager
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) { sbn.packageName }

            val notification = RealNotification(
                id = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                body = body,
                time = sbn.postTime
            )
            onNotificationPosted?.invoke(notification)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        onNotificationRemoved?.invoke(sbn.key)
    }
}
