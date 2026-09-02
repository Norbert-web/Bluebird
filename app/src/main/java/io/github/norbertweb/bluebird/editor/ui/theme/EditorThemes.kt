package io.github.norbertweb.bluebird.editor.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.norbertweb.bluebird.ui.components.DS
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

/**
 * Editor colours are deliberately not a second theme system.
 * The editor follows the device/application system light or dark appearance
 * and consumes Bluebird's shared Fluent design tokens.
 */
data class EditorColors(
    val bg: Color,
    val surface: Color,
    val surfaceHover: Color,
    val lineNumBg: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val tabBg: Color,
    val tabActive: Color,
    val statusBar: Color,
    val currentLineBg: Color,
    val selectionBg: Color,
    val findHighlight: Color,
    val findCurrentHighlight: Color,
    val minimapBg: Color,
    val scrollThumb: Color,
    val gutterModified: Color,
    val gutterAdded: Color,
    val gutterDeleted: Color,
    val synString: Color,
    val synKeyword: Color,
    val synComment: Color,
    val synNumber: Color,
    val synType: Color,
    val synFunction: Color,
    val synOperator: Color,
    val synAnnotation: Color,
    val synPunctuation: Color,
    val synVariable: Color,
    val synConstant: Color,
    val accent: Color,
    val accentGlow: Color,
    val gold: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean,
)

object EdThemes {
    /** The single editor visual scheme, driven by system appearance. */
    fun system(isDark: Boolean): EditorColors {
        val bg = if (isDark) bluebirdColors.Surface else bluebirdColors.SurfaceLight
        val surface = if (isDark) DS.surfaceDark else DS.surfaceLight
        val hover = if (isDark) DS.hoverDark else DS.hoverLight
        val border = if (isDark) DS.borderDark else DS.borderLight
        val text = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
        val secondary = if (isDark) bluebirdColors.TextSecondary else bluebirdColors.TextSecondaryLight
        val muted = secondary.copy(alpha = 0.66f)
        val tabBg = if (isDark) bluebirdColors.SurfaceContainer else bluebirdColors.SurfaceContainerLight
        val currentLine = if (isDark) DS.hoverDark else DS.hoverLight
        val selection = if (isDark) bluebirdColors.AccentBlue.copy(alpha = 0.28f) else bluebirdColors.AccentBlue.copy(alpha = 0.20f)
        val status = if (isDark) bluebirdColors.TaskbarBg else bluebirdColors.TaskbarBgLight
        val minimap = if (isDark) bluebirdColors.Surface else bluebirdColors.SurfaceLight
        val scroll = if (isDark) bluebirdColors.GlassBorderDark else bluebirdColors.GlassBorderLight

        // Fluent/Bluebird semantic syntax colours. These are intentionally
        // shared across light/dark mode rather than introducing another theme.
        val keyword = bluebirdColors.AccentBlue
        val type = if (isDark) bluebirdColors.AccentBlueLight else bluebirdColors.AccentBlueDark
        val string = bluebirdColors.SuccessGreen
        val comment = secondary
        val number = bluebirdColors.AccentBlueLight
        val function = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextSecondaryLight
        val variable = if (isDark) bluebirdColors.TextSecondary else bluebirdColors.TextSecondaryLight
        val constant = if (isDark) bluebirdColors.AccentBlueLight else bluebirdColors.AccentBlueDark
        val punctuation = text
        val operator = text
        val annotation = bluebirdColors.AccentBlueLight

        return EditorColors(
            bg = bg,
            surface = surface,
            surfaceHover = surface.copy(alpha = if (isDark) 0.92f else 0.96f),
            lineNumBg = tabBg,
            border = border,
            text = text,
            textSecondary = secondary,
            textMuted = muted,
            tabBg = tabBg,
            tabActive = bg,
            statusBar = status,
            currentLineBg = currentLine,
            selectionBg = selection,
            findHighlight = bluebirdColors.Warning.copy(alpha = 0.32f),
            findCurrentHighlight = bluebirdColors.Warning.copy(alpha = 0.55f),
            minimapBg = minimap,
            scrollThumb = scroll,
            gutterModified = bluebirdColors.AccentBlue,
            gutterAdded = bluebirdColors.SuccessGreen,
            gutterDeleted = bluebirdColors.DangerRed,
            synString = string,
            synKeyword = keyword,
            synComment = comment,
            synNumber = number,
            synType = type,
            synFunction = function,
            synOperator = operator,
            synAnnotation = annotation,
            synPunctuation = punctuation,
            synVariable = variable,
            synConstant = constant,
            accent = bluebirdColors.AccentBlue,
            accentGlow = bluebirdColors.AccentBlueLight,
            gold = bluebirdColors.Warning,
            danger = bluebirdColors.DangerRed,
            success = bluebirdColors.SuccessGreen,
            warning = bluebirdColors.Warning,
            isDark = isDark,
        )
    }
}
