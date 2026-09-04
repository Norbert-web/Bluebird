package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

/**
 * Bluebird-native visual tokens for the Word Processor.
 *
 * The document engine remains independent, but the chrome consumes the same
 * Bluebird surface/accent system as the other first-party apps. Theme selection
 * is intentionally delegated to Android's current system light/dark setting.
 */
internal data class WordFluentPalette(
    val appBackground: Color,
    val pageBackground: Color,
    val titleBar: Color,
    val ribbonBackground: Color,
    val ribbonSurface: Color,
    val border: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val selection: Color,
)

@Composable
internal fun rememberWordFluentPalette(): WordFluentPalette {
    val dark = isSystemInDarkTheme()
    return WordFluentPalette(
        appBackground = if (dark) bluebirdColors.Surface else bluebirdColors.SurfaceLight,
        pageBackground = if (dark) bluebirdColors.SurfaceContainer else Color.White,
        titleBar = bluebirdColors.AccentBlue,
        ribbonBackground = if (dark) bluebirdColors.Surface else bluebirdColors.SurfaceLight,
        ribbonSurface = if (dark) bluebirdColors.SurfaceContainer else bluebirdColors.SurfaceContainerLight,
        border = if (dark) bluebirdColors.GlassBorderDark else bluebirdColors.GlassBorderLight,
        text = if (dark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight,
        secondaryText = if (dark) bluebirdColors.TextSecondary else bluebirdColors.TextSecondaryLight,
        accent = bluebirdColors.AccentBlue,
        selection = bluebirdColors.AccentBlue.copy(alpha = if (dark) 0.30f else 0.16f),
    )
}

@Composable
internal fun wordRibbonStripBackground(): Color = rememberWordFluentPalette().ribbonSurface
