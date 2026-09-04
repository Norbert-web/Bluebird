package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.norbertweb.bluebird.ui.theme.LocalIsDarkTheme
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
    // Single source of truth for light/dark (Theme.kt / LocalIsDarkTheme) — was
    // calling isSystemInDarkTheme() independently here, which is how this screen
    // (and BluebirdWordProcessorApp.kt, and two other spots in this app) could
    // end up disagreeing with the rest of the launcher about which theme is active.
    val dark = LocalIsDarkTheme.current
    return WordFluentPalette(
        appBackground = if (dark) bluebirdColors.Surface else bluebirdColors.SurfaceLight,
        // The page represents actual paper — Word, Google Docs, and Pages all
        // keep it white in dark mode too; only the surrounding chrome (ribbon,
        // title bar, sidebar) follows the theme. This used to switch to
        // bluebirdColors.SurfaceContainer (a dark tone) in dark mode, but every
        // document text color in WdocModel.kt (DocStyle.NORMAL/HEADING/etc.) is a
        // fixed "ink" color — near-black body text, dark-blue headings — chosen
        // assuming a white page, exactly like real printed ink colors don't change
        // with your screen's theme. With a dark page, that near-black default body
        // text became close to invisible: dark text on a dark page. Keeping the
        // page always white/paper-colored is what every other word processor does,
        // and it's what actually printing the document would produce regardless.
        pageBackground = Color.White,
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
