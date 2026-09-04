package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.norbertweb.bluebird.ui.theme.LocalIsDarkTheme

/**
 * First-class Bluebird OS entry point for the native scratch-built Word Processor.
 *
 * VS Code remains a separate application in the Bluebird OS app model. This
 * composable is deliberately thin: all document/ribbon/editing state stays in
 * the Word Processor feature itself while the OS owns app launch and theme.
 */
@Composable
fun BluebirdWordProcessorApp(
    initialPath: String = "",
) {
    // Single source of truth for light/dark (Theme.kt / LocalIsDarkTheme) — was
    // isSystemInDarkTheme() directly, which is how this app's chrome could end up
    // disagreeing with the rest of the launcher about which theme is active.
    val isDark = LocalIsDarkTheme.current
    Surface(modifier = Modifier.fillMaxSize()) {
        PhoneScreen(
            isDark = isDark,
            initialPath = initialPath,
        )
    }
}

/** Stable metadata for the Bluebird OS app launcher/desktop. */
object BluebirdWordProcessorAppInfo {
    const val id = "bluebird.wordprocessor"
    const val displayName = "Word Processor"
    const val category = "Productivity"
}
