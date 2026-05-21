package com.bluebird

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Forces a desktop/tablet density that actually works with Jetpack Compose.
 *
 * ROOT CAUSE OF ALL PREVIOUS FAILURES:
 * Compose does NOT read density from resources.displayMetrics.
 * It reads from the window's Display object via LocalDensity.
 * Mutating DisplayMetrics or wrapping Context changes nothing Compose sees.
 *
 * THE REAL FIX:
 * Override LocalDensity via CompositionLocalProvider at the top of the
 * composition tree. This is the ONLY way to change how Compose measures dp.
 *
 * We still also wrap the Context (for Views, XML layouts, dp→px conversions
 * outside Compose), but the Compose override is what actually makes the
 * UI look like a tablet.
 */
object DesktopModeHelper {

    /**
     * Target: smallest screen axis must be at least this many dp.
     * 600dp = Android's official "tablet" threshold (sw600dp).
     * We target 700dp to give a comfortable desktop layout.
     * Increase if you want everything even more spread out.
     */
    private const val TARGET_SMALLEST_WIDTH_DP = 700f

    /**
     * Computes the density multiplier that makes the smallest screen axis
     * equal to TARGET_SMALLEST_WIDTH_DP logical dp.
     *
     * density = smallestAxisPixels / TARGET_SMALLEST_WIDTH_DP
     *
     * Example — 1080×1920 phone at density 2.625 (420dpi):
     *   smallestAxis = 1080px
     *   targetDensity = 1080 / 700 = 1.543
     *   logical smallest width = 1080 / 1.543 = 700dp ✓
     *   logical largest width  = 1920 / 1.543 = 1244dp ✓  (huge desktop space)
     *
     * A real 10" tablet at density 2.0 (320dpi), 1600×2560:
     *   smallestAxis = 1600px
     *   targetDensity = 1600 / 700 = 2.286 — but native is 2.0
     *   → returns native 2.0 (we never increase density)
     */
    fun computeTargetDensity(context: Context): Float {
        val dm = context.resources.displayMetrics
        val nativeDensity = dm.density
        val smallestAxisPx = minOf(dm.widthPixels, dm.heightPixels).toFloat()
        val targetDensity = smallestAxisPx / TARGET_SMALLEST_WIDTH_DP
        // Never go denser than native (would make things smaller, not larger)
        return targetDensity.coerceAtMost(nativeDensity)
    }

    /**
     * Wraps a Context with the desktop density baked into Configuration.
     * Use in Application.attachBaseContext and Activity.attachBaseContext.
     * This affects Views, XML inflation, and dp→px conversions outside Compose.
     */
    fun buildDesktopContext(base: Context): Context {
        val dm = base.resources.displayMetrics
        val nativeDensity = dm.density
        val targetDensity = computeTargetDensity(base)

        if (targetDensity >= nativeDensity) return base // already large enough

        val targetDpi = (targetDensity * DisplayMetrics.DENSITY_DEFAULT).toInt()
        val config = Configuration(base.resources.configuration)
        config.densityDpi = targetDpi
        return base.createConfigurationContext(config)
    }
}

/**
 * THE KEY COMPOSABLE — wrap your entire app content in this.
 *
 * Overrides LocalDensity so every dp measurement in Compose uses the
 * desktop density instead of the window's native density.
 *
 * Usage in MainActivity.setContent:
 *
 *   setContent {
 *       DesktopDensityOverride {
 *           Win11Theme { ... }
 *       }
 *   }
 */
@Composable
fun DesktopDensityOverride(context: Context, content: @Composable () -> Unit) {
    val targetDensity = DesktopModeHelper.computeTargetDensity(context)
    val fontScale = context.resources.configuration.fontScale

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = targetDensity,
            fontScale = fontScale
        ),
        content = content
    )
}
