package io.github.norbertweb.bluebird.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.R

// ─── Selawik — Microsoft's open-source Segoe UI substitute ────────────────
// Metrics-compatible with Segoe UI (SIL Open Font License, free to bundle).
// Drop the 5 .ttf files you already fetched straight into res/font/ as-is —
// their filenames (selawk, selawkl, selawksl, selawksb, selawkb) are
// already valid Android resource names, no renaming needed.
val SelawikFontFamily = FontFamily(
    Font(R.font.selawkl,  FontWeight.Light),      // selawkl.ttf  — Light
    Font(R.font.selawksl, FontWeight(350)),        // selawksl.ttf — Semilight
    Font(R.font.selawk,   FontWeight.Normal),      // selawk.ttf   — Regular
    Font(R.font.selawksb, FontWeight.SemiBold),    // selawksb.ttf — Semibold
    Font(R.font.selawkb,  FontWeight.Bold),        // selawkb.ttf  — Bold
)

object bluebirdColors {
    val GlassLight = Color(0xFF4C63D9)


    val AccentBlue = Color(0xFF4C63D9)
    val AccentBlueLight = Color(0xFF7C8FEA)
    val AccentBlueDark = Color(0xFF33409F)
    val Surface = Color(0xFF14161C)
    val SurfaceLight = Color(0xFFF8F9FB)
    val SurfaceContainer = Color(0xFF1B1E26)
    val SurfaceContainerLight = Color(0xFFF3F5F8)
    val TextPrimary = Color(0xFFF5F6F8)
    val TextPrimaryLight = Color(0xFF171A21)
    val TextSecondary = Color(0xFFA3A9B4)
    val TextSecondaryLight = Color(0xFF5B6270)
    val TaskbarBg = Color(0xCC14161C)
    val TaskbarBgLight = Color(0xCCF3F5F8)
    val GlassBg = Color(0x4D14161C)
    val GlassBgLight = Color(0xB3FFFFFF)
    val ContextMenuBg = Color(0xF01B1E26)
    val ContextMenuBgLight = Color(0xF5FFFFFF)
    val DangerRed = Color(0xFFD6564C)
    val SuccessGreen = Color(0xFF3EA66D)
    val WarningYellow = Color(0xFFE0A82E)
    val Success = Color(0xFF3EA66D)

    //new


    val GlassDark = Color(0xCC1B1E26)
    val GlassBorderLight = Color(0x40171A21)
    val GlassBorderDark = Color(0x30FFFFFF)



    val StartMenuBg = Color(0xE614161C)
    val StartMenuBgLight = Color(0xE6F8F9FB)

    val HoverBg = Color(0x1AFFFFFF)
    val HoverBgLight = Color(0x0A171A21)

    val WidgetBg = Color(0xFF1B1E26)
    val WidgetBgLight = Color(0xFFF3F5F8)

    val Separator = Color(0x33FFFFFF)
    val SeparatorLight = Color(0x26171A21)


    val Warning = Color(0xFFE0A82E)
    val Error = Color(0xFFD6564C)

    val BlueGradientStart = Color(0xFF4C63D9)
    val BlueGradientEnd = Color(0xFF7C8FEA)

    val WallpaperOverlay = Color(0x99000000)

}

private val DarkColorScheme = darkColorScheme(
    primary = bluebirdColors.AccentBlue,
    secondary = bluebirdColors.AccentBlueLight,
    background = bluebirdColors.Surface,
    surface = bluebirdColors.SurfaceContainer,
    onPrimary = Color.White,
    onBackground = bluebirdColors.TextPrimary,
    onSurface = bluebirdColors.TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = bluebirdColors.AccentBlue,
    secondary = bluebirdColors.AccentBlueDark,
    background = bluebirdColors.SurfaceLight,
    surface = bluebirdColors.SurfaceContainerLight,
    onPrimary = Color.White,
    onBackground = bluebirdColors.TextPrimaryLight,
    onSurface = bluebirdColors.TextPrimaryLight,
)

// ─── Scaled Typography ────────────────────────────────────────────────────────
// Reads LocalTextScale and returns a full Typography where every style's
// fontSize and lineHeight are multiplied by the scale factor.
// Because bluebirdTheme wraps the entire app, every screen — browser, settings,
// file manager, everything — automatically respects the user's text size choice
// without any per-screen changes needed.

private fun TextUnit.scale(factor: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else (this.value * factor).sp

private fun TextStyle.scale(factor: Float): TextStyle = copy(
    // This one line applies Selawik across every screen in the app —
    // display/headline/title/body/label all funnel through this same
    // extension already (see scaledTypography below), so there's no need
    // to touch it in more than this one place.
    fontFamily = SelawikFontFamily,
    fontSize   = fontSize.scale(factor),
    lineHeight = lineHeight.scale(factor)
)

@Composable
private fun scaledTypography(scale: Float): Typography {
    val base = Typography()
    return remember(scale) {
        Typography(
            displayLarge   = base.displayLarge.scale(scale),
            displayMedium  = base.displayMedium.scale(scale),
            displaySmall   = base.displaySmall.scale(scale),
            headlineLarge  = base.headlineLarge.scale(scale),
            headlineMedium = base.headlineMedium.scale(scale),
            headlineSmall  = base.headlineSmall.scale(scale),
            titleLarge     = base.titleLarge.scale(scale),
            titleMedium    = base.titleMedium.scale(scale),
            titleSmall     = base.titleSmall.scale(scale),
            bodyLarge      = base.bodyLarge.scale(scale),
            bodyMedium     = base.bodyMedium.scale(scale),
            bodySmall      = base.bodySmall.scale(scale),
            labelLarge     = base.labelLarge.scale(scale),
            labelMedium    = base.labelMedium.scale(scale),
            labelSmall     = base.labelSmall.scale(scale),
        )
    }
}

@Composable
fun bluebirdTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // LocalTextScale is provided by MainActivity's CompositionLocalProvider.
    // bluebirdTheme reads it here so the scaled Typography flows into every
    // Composable in the tree via MaterialTheme.typography.
    val scale      = LocalTextScale.current
    val typography = scaledTypography(scale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        content     = content
    )
}
