package io.github.norbertweb.bluebird

import android.app.Application
import io.github.norbertweb.bluebird.update.UpdateNotificationHelper

class BluebirdApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Create notification channels once
        UpdateNotificationHelper.createChannels(this)

        // Do not perform network work merely because the application process was
        // created. Update checks are user-triggered from Launcher Update Settings.
        // This keeps a closed Bluebird session from waking the network in the
        // background.
    }
}
