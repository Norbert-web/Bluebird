package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme

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
    val isDark = isSystemInDarkTheme()
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
