package io.github.norbertweb.bluebird.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

// ─────────────────────────────────────────────────────────
// BUILT-IN APP ICONS — custom SVG-first, Fluent-icon fallback
//
// Drop Windows-11-style vector drawables into res/drawable named
// "ic_builtin_<slug>" (slug = app name, lowercased, spaces→underscores —
// see slugFor() below, e.g. "Word Impress" -> ic_builtin_word_impress).
//
// If that resource doesn't exist in the app's res/drawable directory,
// this silently falls back to the existing FluentIcon vector passed in
// from `builtInApps`, so nothing breaks for apps that don't have a
// custom icon yet — you can add them incrementally.
// ─────────────────────────────────────────────────────────

fun slugFor(appName: String): String =
    "ic_builtin_" + appName.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

// Maps a built-in screen to the same display name StartMenu.kt's
// `builtInApps` list uses, so Desktop.kt (and anywhere else) can resolve
// the exact same custom icon Start Menu shows for that app — one name,
// one icon, everywhere in the launcher.
fun builtInAppDisplayName(screen: io.github.norbertweb.bluebird.LauncherScreen): String? =
    when (screen) {
        io.github.norbertweb.bluebird.LauncherScreen.SETTINGS -> "Settings"
        io.github.norbertweb.bluebird.LauncherScreen.CALCULATOR -> "Calculator"
        io.github.norbertweb.bluebird.LauncherScreen.CALENDAR -> "Calendar"
        io.github.norbertweb.bluebird.LauncherScreen.BLUEBIRD_STORE -> "Bluebird Store"
        io.github.norbertweb.bluebird.LauncherScreen.WORD_IMPRESS -> "Word Impress"
        io.github.norbertweb.bluebird.LauncherScreen.FILE_EXPLORER -> "Files"
        io.github.norbertweb.bluebird.LauncherScreen.BROWSER -> "Browser"
        io.github.norbertweb.bluebird.LauncherScreen.PHOTOS -> "Photos"
        io.github.norbertweb.bluebird.LauncherScreen.TASK_MANAGER -> "Tasks"
        io.github.norbertweb.bluebird.LauncherScreen.MEDIA_PLAYER -> "Media Player"
        io.github.norbertweb.bluebird.LauncherScreen.RECYCLE_BIN -> "Recycle Bin"
        io.github.norbertweb.bluebird.LauncherScreen.IMAGE_VIEWER -> "Image Viewer"
        io.github.norbertweb.bluebird.LauncherScreen.PremiumTextEditorScreen -> "Text Editor"
        io.github.norbertweb.bluebird.LauncherScreen.TERMINAL -> "Terminal"

        else -> null
    }

// Same idea, but for the taskbar/window-manager side, which identifies a
// window by its `iconKey` (WindowIconKey.*, a String) rather than a
// LauncherScreen — see FluentIcon.kt's `iconForKey()`. Mapped to the exact
// same display names so Taskbar.kt and WindowManager.kt resolve the same
// custom icon Start Menu and Desktop.kt use for that app.
fun builtInAppDisplayNameForWindowKey(key: String): String? =
    when (key) {
        io.github.norbertweb.bluebird.WindowIconKey.SETTINGS -> "Settings"
        io.github.norbertweb.bluebird.WindowIconKey.CALCULATOR -> "Calculator"
        io.github.norbertweb.bluebird.WindowIconKey.CALENDAR -> "Calendar"
        io.github.norbertweb.bluebird.WindowIconKey.BLUEBIRD_STORE -> "Bluebird Store"
        io.github.norbertweb.bluebird.WindowIconKey.WORD_IMPRESS -> "Word Impress"
        io.github.norbertweb.bluebird.WindowIconKey.FILE_EXPLORER -> "Files"
        io.github.norbertweb.bluebird.WindowIconKey.BROWSER -> "Browser"
        io.github.norbertweb.bluebird.WindowIconKey.PHOTOS -> "Photos"
        io.github.norbertweb.bluebird.WindowIconKey.TASK_MANAGER -> "Tasks"
        io.github.norbertweb.bluebird.WindowIconKey.MEDIA_PLAYER -> "Media Player"
        io.github.norbertweb.bluebird.WindowIconKey.RECYCLE_BIN -> "Recycle Bin"
        io.github.norbertweb.bluebird.WindowIconKey.IMAGE_VIEWER -> "Image Viewer"
        io.github.norbertweb.bluebird.WindowIconKey.PremiumTextEditorScreen -> "Text Editor"

        else -> null // TERMINAL has no entry in iconForKey() either (falls to FluentIcon.Window) —
                     // pre-existing gap, unrelated to this change; WEB_APP/COPY_PROGRESS are
                     // per-instance windows with no single fixed icon.
    }

/**
 * Drop-in replacement for `Icon(imageVector = iconForKey(key), ...)` in
 * Taskbar.kt / WindowManager.kt — resolves the custom SVG for the window's
 * app first (if one exists), falling back to the existing Fluent glyph
 * otherwise, so a window's taskbar/titlebar icon always matches its
 * Start Menu tile.
 */
@Composable
fun WindowKeyIcon(
    key: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val builtInName = builtInAppDisplayNameForWindowKey(key)
    if (builtInName != null) {
        BuiltInAppIcon(
            appName = builtInName,
            fallback = iconForKey(key),
            tint = tint,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = iconForKey(key),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

/**
 * Looks up a drawable resource by name at runtime (no compile-time R
 * reference needed, so missing icons never crash — resourceId is 0 when
 * not found, and callers should fall back to the vector icon).
 */
@Composable
fun rememberBuiltInIconResourceId(appName: String): Int {
    val context = LocalContext.current
    return remember(appName) {
        context.resources.getIdentifier(slugFor(appName), "drawable", context.packageName)
    }
}

/**
 * Renders a built-in app's icon: custom SVG/vector-drawable if present in
 * res/drawable, otherwise the Fluent fallback icon. Drop-in replacement
 * for a plain `Icon(imageVector = icon, ...)` call.
 */
@Composable
fun BuiltInAppIcon(
    appName: String,
    fallback: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val resId = rememberBuiltInIconResourceId(appName)
    if (resId != 0) {
        // Custom SVG-derived vector drawable found — these are authored as
        // full-color Windows-11-style badge icons, so no tint is applied.
        Image(
            painter = painterResource(id = resId),
            contentDescription = appName,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = fallback,
            contentDescription = appName,
            tint = tint,
            modifier = modifier
        )
    }
}
