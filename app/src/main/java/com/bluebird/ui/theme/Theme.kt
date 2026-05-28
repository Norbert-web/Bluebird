package com.bluebird.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object Win11Colors {
    val GlassLight = Color(0xFF0078D4)


    val AccentBlue = Color(0xFF0078D4)
    val AccentBlueLight = Color(0xFF4DA6FF)
    val AccentBlueDark = Color(0xFF005A9E)
    val Surface = Color(0xFF1C1C1C)
    val SurfaceLight = Color(0xFFF5F5F5)
    val SurfaceContainer = Color(0xFF2C2C2C)
    val SurfaceContainerLight = Color(0xFFEEEEEE)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextPrimaryLight = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFFAAAAAA)
    val TextSecondaryLight = Color(0xFF666666)
    val TaskbarBg = Color(0xCC1A1A2E)
    val TaskbarBgLight = Color(0xCCF0F0F0)
    val GlassBg = Color(0x4D1C1C2E)
    val GlassBgLight = Color(0xB3FFFFFF)
    val ContextMenuBg = Color(0xF0252535)
    val ContextMenuBgLight = Color(0xF5FFFFFF)
    val DangerRed = Color(0xFFE81123)
    val SuccessGreen = Color(0xFF107C10)
    val WarningYellow = Color(0xFFFFB900)
    val Success = Color(0xFF4CAF50)

    //new


    val GlassDark = Color(0xCC2D2D2D)
    val GlassBorderLight = Color(0x40FFFFFF)
    val GlassBorderDark = Color(0x20FFFFFF)



    val StartMenuBg = Color(0xE6282828)
    val StartMenuBgLight = Color(0xE6F5F5F5)

    val HoverBg = Color(0x1AFFFFFF)
    val HoverBgLight = Color(0x1A000000)

    val WidgetBg = Color(0xFF2C2C2C)
    val WidgetBgLight = Color(0xFFF0F0F0)

    val Separator = Color(0x30FFFFFF)
    val SeparatorLight = Color(0x30000000)


    val Warning = Color(0xFFFCE100)
    val Error = Color(0xFFD13438)

    val BlueGradientStart = Color(0xFF0078D4)
    val BlueGradientEnd = Color(0xFF40A9FF)

    val WallpaperOverlay = Color(0x99000000)

}

private val DarkColorScheme = darkColorScheme(
    primary = Win11Colors.AccentBlue,
    secondary = Win11Colors.AccentBlueLight,
    background = Win11Colors.Surface,
    surface = Win11Colors.SurfaceContainer,
    onPrimary = Color.White,
    onBackground = Win11Colors.TextPrimary,
    onSurface = Win11Colors.TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = Win11Colors.AccentBlue,
    secondary = Win11Colors.AccentBlueDark,
    background = Win11Colors.SurfaceLight,
    surface = Win11Colors.SurfaceContainerLight,
    onPrimary = Color.White,
    onBackground = Win11Colors.TextPrimaryLight,
    onSurface = Win11Colors.TextPrimaryLight,
)

// ─── Scaled Typography ────────────────────────────────────────────────────────
// Reads LocalTextScale and returns a full Typography where every style's
// fontSize and lineHeight are multiplied by the scale factor.
// Because Win11Theme wraps the entire app, every screen — browser, settings,
// file manager, everything — automatically respects the user's text size choice
// without any per-screen changes needed.

private fun TextUnit.scale(factor: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else (this.value * factor).sp

private fun TextStyle.scale(factor: Float): TextStyle = copy(
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
fun Win11Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // LocalTextScale is provided by MainActivity's CompositionLocalProvider.
    // Win11Theme reads it here so the scaled Typography flows into every
    // Composable in the tree via MaterialTheme.typography.
    val scale      = LocalTextScale.current
    val typography = scaledTypography(scale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        content     = content
    )
}
