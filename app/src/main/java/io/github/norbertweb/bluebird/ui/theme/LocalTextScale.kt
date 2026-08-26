package io.github.norbertweb.bluebird.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * App-wide text scale multiplier.
 * Provided at the root of the composition tree (in your root Composable / Activity)
 * via CompositionLocalProvider. All text that calls [scaledSp] will respond to it.
 *
 * Usage at root:
 *   CompositionLocalProvider(LocalTextScale provides uiState.textScale) {
 *       Win11Theme(...) { ... }
 *   }
 *
 * Usage in any Composable:
 *   Text(fontSize = 14.scaledSp)
 *   Text(style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.scaledSp))
 */
val LocalTextScale = compositionLocalOf { 1f }

/** Convenience extension — converts an Int sp value scaled by [LocalTextScale]. */
val Int.scaledSp: TextUnit
    @androidx.compose.runtime.Composable
    get() = (this * androidx.compose.runtime.currentCompositionLocalContext
        .let { LocalTextScale.current }).sp

/** Convenience extension for Float sp values. */
val Float.scaledSp: TextUnit
    @androidx.compose.runtime.Composable
    get() = (this * LocalTextScale.current).sp
