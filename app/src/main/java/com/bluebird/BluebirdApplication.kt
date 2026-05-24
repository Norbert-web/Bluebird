package com.bluebird

import android.app.Application
import com.bluebird.update.UpdateManager
import com.bluebird.update.UpdateNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluebirdApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Create notification channels once
        UpdateNotificationHelper.createChannels(this)

        // Background update check respecting user's chosen frequency
        if (UpdateManager.shouldCheckNow(this)) {
            val deliveryMode = UpdateManager.getDeliveryMode(this)
            CoroutineScope(Dispatchers.IO).launch {
                val result = UpdateManager.checkForUpdate(this@BluebirdApplication)
                UpdateManager.notifyIfNewVersion(this@BluebirdApplication, result, deliveryMode)
            }
        }
    }
}
