package com.bluebird

import android.app.Application
import android.content.Context
import android.content.res.Configuration

/**
 * Custom Application class — register in AndroidManifest.xml:
 *   <application android:name=".LauncherApplication" ...>
 *
 * Handles the View/XML side of desktop mode.
 * The Compose side is handled by DesktopDensityOverride in MainActivity.
 */
class LauncherApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(DesktopModeHelper.buildDesktopContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Nothing extra needed here — Compose re-reads LocalDensity on recomposition,
        // and the Context wrapper handles the View system automatically.
    }
}
