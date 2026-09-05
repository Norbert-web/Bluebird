package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * User-visible Bluebird storage root when broad external storage access is
 * available. Falls back to app-private storage so packages can still work on
 * devices where Android storage restrictions prevent a public folder.
 */
object BluebirdStorage {
    fun root(context: Context): File {
        val publicRoot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            File(Environment.getExternalStorageDirectory(), "Bluebird Storage")
        } else {
            File(context.filesDir, "Bluebird Storage")
        }
        publicRoot.mkdirs()
        return publicRoot
    }
}
