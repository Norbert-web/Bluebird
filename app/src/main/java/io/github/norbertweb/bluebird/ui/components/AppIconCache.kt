package io.github.norbertweb.bluebird.ui.components

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.norbertweb.bluebird.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────
// APP ICON CACHE
// Bug fix: every icon tile previously did
//     remember(pkg) { app.icon!!.toBitmap().asImageBitmap() }
// which runs the Drawable→Bitmap conversion synchronously on the UI thread
// the first time each icon composes — this is what caused the jank when
// opening Start Menu, switching Mobile Home pages, or scrolling All Apps,
// since dozens of conversions could land on the same frame.
//
// This cache does the conversion once per app, off the main thread, and
// is shared process-wide so Start Menu, Search, and Mobile Home never
// redo work for the same package.
// ─────────────────────────────────────────────────────────
private val iconBitmapCache = object : LruCache<String, ImageBitmap>(150) {}

/**
 * Returns the cached ImageBitmap for [app] once ready, or null while it's
 * still being decoded on a background dispatcher. Use with a placeholder
 * (e.g. the Fluent "Apps" glyph) for the null case so the tile never blocks
 * layout waiting on icon decode.
 */
@Composable
fun rememberAppIconBitmap(app: AppInfo): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = iconBitmapCache.get(app.packageName), key1 = app.packageName) {
        if (value == null) {
            val drawable = app.icon
            if (drawable != null) {
                value = withContext(Dispatchers.Default) {
                    val bmp = drawable.toBitmap().asImageBitmap()
                    iconBitmapCache.put(app.packageName, bmp)
                    bmp
                }
            }
        }
    }
    return state.value
}

/** Call when an app is uninstalled/updated so a stale icon isn't served. */
fun invalidateAppIcon(packageName: String) {
    iconBitmapCache.remove(packageName)
}
