package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────
// FLUENT DESIGN SYSTEM
// Single shared token set for every Windows-11-style surface in the app
// (Start Menu, Search Overlay, Taskbar, …). Previously StartMenu.kt and
// SearchOverlay.kt each defined their own colors/corner radii, which is
// why they visually drifted apart. Everything now reads from here.
//
// Also owns the new user-controllable "surface opacity" setting
// (Settings > Personalization > Transparency), persisted the same way
// size mode already was.
// ─────────────────────────────────────────────────────────
object DS {
    // Base surface + accent palette (Windows 11 Mica/Acrylic-inspired)
    val surfaceDark  = Color(0xFF252B32)
    val surfaceLight = Color(0xFFE8ECF0)
    val borderDark   = Color(0xFF373E47)
    val borderLight  = Color(0xFFCDD5DF)
    val accentStart  = Color(0xFF0078D4)
    val accentEnd    = Color(0xFF005A9E)
    val hoverDark    = Color(0x14FFFFFF)
    val hoverLight   = Color(0x0C000000)
    val pressedDark  = Color(0x22FFFFFF)
    val pressedLight = Color(0x14000000)
    val badgeRed     = Color(0xFFCB4335)
    val successGreen = Color(0xFF3FB950)

    // Base (fully-opaque) glass tint — the alpha applied on top of this is
    // driven by `opacity`, so every glass surface in the app tracks the
    // same slider instead of each screen hardcoding its own alpha.
    private val glassDarkBase  = Color(0xFF1C2128)
    private val glassLightBase = Color(0xFFF0F2F5)

    val menuWidthCompact   = 560.dp
    val menuWidthExpanded  = 780.dp
    val menuHeightCompact  = 660.dp
    val menuHeightExpanded = 840.dp

    // Shared corner language — SearchOverlay previously used 16dp while
    // Start Menu used 8dp for the same kind of flyout surface. Unified here.
    val cornerRadius  = 8.dp   // Windows 11 flyout/menu corner radius
    val sectionCorner = 8.dp
    val tileCorner    = 4.dp   // Fluent 2 control-corner-radius, small icon tiles
    val chipCorner    = 6.dp
    val overlayCorner = 12.dp  // Search Overlay's card radius (was 16dp, standardized down)

    val accentBrushValue: Brush = Brush.linearGradient(
        colors = listOf(accentStart, accentEnd),
        start = Offset(0f, 0f), end = Offset(160f, 160f)
    )
    fun accentBrush() = accentBrushValue

    // ── Opacity (Personalization setting) ──────────────────────────
    // Range 0.55–1.0. Below 0.55 text contrast against wallpaper suffers,
    // so the slider UI should clamp to this range.
    const val OPACITY_MIN = 0.55f
    const val OPACITY_MAX = 1.0f
    const val OPACITY_DEFAULT = 0.85f

    /** Glass surface color with the current user opacity applied. */
    fun glass(isDark: Boolean, opacity: Float): Color =
        (if (isDark) glassDarkBase else glassLightBase).copy(alpha = opacity.coerceIn(OPACITY_MIN, OPACITY_MAX))

    /** Secondary/dock surface — slightly lower alpha than the main glass panel. */
    fun surfaceGlass(isDark: Boolean, opacity: Float): Color =
        (if (isDark) surfaceDark else surfaceLight).copy(alpha = (opacity * 0.6f).coerceIn(OPACITY_MIN * 0.5f, OPACITY_MAX))

    // Fixed-opacity convenience accessors for minor chrome (dropdown
    // backgrounds, thin borders) that isn't wired to the live opacity
    // slider — those call sites don't have a Context/opacity value handy
    // and the transparency there is cosmetic, not the user-facing control.
    // The main Start Menu panel and Mobile Home use DS.glass(isDark, opacity)
    // directly with the live value instead.
    val glassDark: Color get() = glass(isDark = true, opacity = OPACITY_DEFAULT)
    val glassLight: Color get() = glass(isDark = false, opacity = OPACITY_DEFAULT)
}

// ─────────────────────────────────────────────────────────
// Persistence for the opacity setting (mirrors getSavedSizeMode/saveSizeMode
// pattern already used for Start Menu size).
// ─────────────────────────────────────────────────────────
private const val PERSONALIZATION_PREFS = "bluebird_personalization_prefs"

fun getSavedOpacity(context: Context): Float {
    val prefs = context.getSharedPreferences(PERSONALIZATION_PREFS, Context.MODE_PRIVATE)
    return prefs.getFloat("surface_opacity", DS.OPACITY_DEFAULT)
        .coerceIn(DS.OPACITY_MIN, DS.OPACITY_MAX)
}

fun saveOpacity(context: Context, opacity: Float) {
    val prefs = context.getSharedPreferences(PERSONALIZATION_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putFloat("surface_opacity", opacity.coerceIn(DS.OPACITY_MIN, DS.OPACITY_MAX)).apply()
}

/**
 * Remembers the current opacity setting, reading the persisted value once.
 * Call `setOpacity` from the Personalization slider to update + persist.
 */
@Composable
fun rememberOpacity(context: Context): Pair<Float, (Float) -> Unit> {
    var opacity by remember { mutableFloatStateOf(getSavedOpacity(context)) }
    val setOpacity: (Float) -> Unit = { newValue ->
        opacity = newValue.coerceIn(DS.OPACITY_MIN, DS.OPACITY_MAX)
        saveOpacity(context, opacity)
    }
    return opacity to setOpacity
}
