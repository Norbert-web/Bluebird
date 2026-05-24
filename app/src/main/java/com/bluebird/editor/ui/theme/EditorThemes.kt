package com.bluebird.editor.ui.theme

import androidx.compose.ui.graphics.Color
import com.bluebird.editor.core.EditorTheme

// ─────────────────────────────────────────────────────────────────
// Theme Token Set
// ─────────────────────────────────────────────────────────────────

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
    // Syntax
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
    // UI Accent
    val accent: Color,
    val accentGlow: Color,
    val gold: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean,
)

// ─────────────────────────────────────────────────────────────────
// Theme Definitions
// ─────────────────────────────────────────────────────────────────

object EdThemes {

    val VsCodeDark = EditorColors(
        bg = Color(0xFF1E1E1E), surface = Color(0xFF252526), surfaceHover = Color(0xFF2D2D2D),
        lineNumBg = Color(0xFF252526), border = Color(0xFF3C3C3C),
        text = Color(0xFFD4D4D4), textSecondary = Color(0xFF888888), textMuted = Color(0xFF555555),
        tabBg = Color(0xFF2D2D2D), tabActive = Color(0xFF1E1E1E), statusBar = Color(0xFF007ACC),
        currentLineBg = Color(0xFF2A2A2A), selectionBg = Color(0xFF264F78),
        findHighlight = Color(0xFFFFD700).copy(alpha = 0.4f), findCurrentHighlight = Color(0xFFFF8C00).copy(alpha = 0.6f),
        minimapBg = Color(0xFF1E1E1E), scrollThumb = Color(0xFF424242),
        gutterModified = Color(0xFF0078D4), gutterAdded = Color(0xFF107C10), gutterDeleted = Color(0xFFD83B01),
        synString = Color(0xFFCE9178), synKeyword = Color(0xFF569CD6), synComment = Color(0xFF6A9955),
        synNumber = Color(0xFFB5CEA8), synType = Color(0xFF4EC9B0), synFunction = Color(0xFFDCDCAA),
        synOperator = Color(0xFFD4D4D4), synAnnotation = Color(0xFF9CDCFE),
        synPunctuation = Color(0xFFD4D4D4), synVariable = Color(0xFF9CDCFE), synConstant = Color(0xFF4FC1FF),
        accent = Color(0xFF0078D4), accentGlow = Color(0xFF429CE3), gold = Color(0xFFFFB900),
        danger = Color(0xFFD83B01), success = Color(0xFF107C10), warning = Color(0xFFFFB900),
        isDark = true
    )

    val VsCodeLight = EditorColors(
        bg = Color(0xFFFFFFFF), surface = Color(0xFFF3F3F3), surfaceHover = Color(0xFFEBEBEB),
        lineNumBg = Color(0xFFF0F0F0), border = Color(0xFFE0E0E0),
        text = Color(0xFF1A1A1A), textSecondary = Color(0xFF666666), textMuted = Color(0xFFAAAAAA),
        tabBg = Color(0xFFECECEC), tabActive = Color(0xFFFFFFFF), statusBar = Color(0xFF0078D4),
        currentLineBg = Color(0xFFF5F5F5), selectionBg = Color(0xFFADD6FF),
        findHighlight = Color(0xFFFFD700).copy(alpha = 0.5f), findCurrentHighlight = Color(0xFFFF8C00).copy(alpha = 0.7f),
        minimapBg = Color(0xFFF3F3F3), scrollThumb = Color(0xFFCDCDCD),
        gutterModified = Color(0xFF0078D4), gutterAdded = Color(0xFF107C10), gutterDeleted = Color(0xFFD83B01),
        synString = Color(0xFFA31515), synKeyword = Color(0xFF0000FF), synComment = Color(0xFF008000),
        synNumber = Color(0xFF098658), synType = Color(0xFF267F99), synFunction = Color(0xFF795E26),
        synOperator = Color(0xFF1A1A1A), synAnnotation = Color(0xFF001080),
        synPunctuation = Color(0xFF1A1A1A), synVariable = Color(0xFF001080), synConstant = Color(0xFF0070C1),
        accent = Color(0xFF0078D4), accentGlow = Color(0xFF429CE3), gold = Color(0xFFB8860B),
        danger = Color(0xFFD83B01), success = Color(0xFF107C10), warning = Color(0xFFB8860B),
        isDark = false
    )

    val Monokai = EditorColors(
        bg = Color(0xFF272822), surface = Color(0xFF2F302A), surfaceHover = Color(0xFF363730),
        lineNumBg = Color(0xFF2F302A), border = Color(0xFF464741),
        text = Color(0xFFF8F8F2), textSecondary = Color(0xFF908D82), textMuted = Color(0xFF605D55),
        tabBg = Color(0xFF363730), tabActive = Color(0xFF272822), statusBar = Color(0xFF75715E),
        currentLineBg = Color(0xFF3E3D32), selectionBg = Color(0xFF49483E),
        findHighlight = Color(0xFFE6DB74).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFFD971F).copy(alpha = 0.5f),
        minimapBg = Color(0xFF272822), scrollThumb = Color(0xFF464741),
        gutterModified = Color(0xFF66D9E8), gutterAdded = Color(0xFFA6E22E), gutterDeleted = Color(0xFFF92672),
        synString = Color(0xFFE6DB74), synKeyword = Color(0xFFF92672), synComment = Color(0xFF75715E),
        synNumber = Color(0xFFAE81FF), synType = Color(0xFF66D9E8), synFunction = Color(0xFFA6E22E),
        synOperator = Color(0xFFF92672), synAnnotation = Color(0xFFF92672),
        synPunctuation = Color(0xFFF8F8F2), synVariable = Color(0xFFF8F8F2), synConstant = Color(0xFFAE81FF),
        accent = Color(0xFFA6E22E), accentGlow = Color(0xFF66D9E8), gold = Color(0xFFE6DB74),
        danger = Color(0xFFF92672), success = Color(0xFFA6E22E), warning = Color(0xFFFD971F),
        isDark = true
    )

    val SolarizedDark = EditorColors(
        bg = Color(0xFF002B36), surface = Color(0xFF073642), surfaceHover = Color(0xFF0D4554),
        lineNumBg = Color(0xFF073642), border = Color(0xFF124652),
        text = Color(0xFF839496), textSecondary = Color(0xFF657B83), textMuted = Color(0xFF405360),
        tabBg = Color(0xFF073642), tabActive = Color(0xFF002B36), statusBar = Color(0xFF268BD2),
        currentLineBg = Color(0xFF073642), selectionBg = Color(0xFF0D4554),
        findHighlight = Color(0xFFB58900).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFCB4B16).copy(alpha = 0.5f),
        minimapBg = Color(0xFF002B36), scrollThumb = Color(0xFF405360),
        gutterModified = Color(0xFF268BD2), gutterAdded = Color(0xFF859900), gutterDeleted = Color(0xFFDC322F),
        synString = Color(0xFF2AA198), synKeyword = Color(0xFF859900), synComment = Color(0xFF586E75),
        synNumber = Color(0xFFD33682), synType = Color(0xFFB58900), synFunction = Color(0xFF268BD2),
        synOperator = Color(0xFF93A1A1), synAnnotation = Color(0xFFCB4B16),
        synPunctuation = Color(0xFF839496), synVariable = Color(0xFF839496), synConstant = Color(0xFFD33682),
        accent = Color(0xFF268BD2), accentGlow = Color(0xFF2AA198), gold = Color(0xFFB58900),
        danger = Color(0xFFDC322F), success = Color(0xFF859900), warning = Color(0xFFB58900),
        isDark = true
    )

    val SolarizedLight = EditorColors(
        bg = Color(0xFFFDF6E3), surface = Color(0xFFEEE8D5), surfaceHover = Color(0xFFE5DEC8),
        lineNumBg = Color(0xFFEEE8D5), border = Color(0xFFD3CBBA),
        text = Color(0xFF657B83), textSecondary = Color(0xFF839496), textMuted = Color(0xFFA0ADB4),
        tabBg = Color(0xFFEEE8D5), tabActive = Color(0xFFFDF6E3), statusBar = Color(0xFF268BD2),
        currentLineBg = Color(0xFFEEE8D5), selectionBg = Color(0xFFCFD4C6),
        findHighlight = Color(0xFFB58900).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFCB4B16).copy(alpha = 0.5f),
        minimapBg = Color(0xFFFDF6E3), scrollThumb = Color(0xFFA0ADB4),
        gutterModified = Color(0xFF268BD2), gutterAdded = Color(0xFF859900), gutterDeleted = Color(0xFFDC322F),
        synString = Color(0xFF2AA198), synKeyword = Color(0xFF859900), synComment = Color(0xFF93A1A1),
        synNumber = Color(0xFFD33682), synType = Color(0xFFB58900), synFunction = Color(0xFF268BD2),
        synOperator = Color(0xFF657B83), synAnnotation = Color(0xFFCB4B16),
        synPunctuation = Color(0xFF657B83), synVariable = Color(0xFF657B83), synConstant = Color(0xFFD33682),
        accent = Color(0xFF268BD2), accentGlow = Color(0xFF2AA198), gold = Color(0xFFB58900),
        danger = Color(0xFFDC322F), success = Color(0xFF859900), warning = Color(0xFFB58900),
        isDark = false
    )

    val Dracula = EditorColors(
        bg = Color(0xFF282A36), surface = Color(0xFF343746), surfaceHover = Color(0xFF3A3D50),
        lineNumBg = Color(0xFF343746), border = Color(0xFF44475A),
        text = Color(0xFFF8F8F2), textSecondary = Color(0xFF8B8D9C), textMuted = Color(0xFF6272A4),
        tabBg = Color(0xFF343746), tabActive = Color(0xFF282A36), statusBar = Color(0xFF6272A4),
        currentLineBg = Color(0xFF343746), selectionBg = Color(0xFF44475A),
        findHighlight = Color(0xFFF1FA8C).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFFFB86C).copy(alpha = 0.5f),
        minimapBg = Color(0xFF282A36), scrollThumb = Color(0xFF44475A),
        gutterModified = Color(0xFF8BE9FD), gutterAdded = Color(0xFF50FA7B), gutterDeleted = Color(0xFFFF5555),
        synString = Color(0xFFF1FA8C), synKeyword = Color(0xFFFF79C6), synComment = Color(0xFF6272A4),
        synNumber = Color(0xFFBD93F9), synType = Color(0xFF8BE9FD), synFunction = Color(0xFF50FA7B),
        synOperator = Color(0xFFFF79C6), synAnnotation = Color(0xFFFFB86C),
        synPunctuation = Color(0xFFF8F8F2), synVariable = Color(0xFFF8F8F2), synConstant = Color(0xFFBD93F9),
        accent = Color(0xFFBD93F9), accentGlow = Color(0xFF50FA7B), gold = Color(0xFFF1FA8C),
        danger = Color(0xFFFF5555), success = Color(0xFF50FA7B), warning = Color(0xFFFFB86C),
        isDark = true
    )

    val GithubDark = EditorColors(
        bg = Color(0xFF0D1117), surface = Color(0xFF161B22), surfaceHover = Color(0xFF1C2128),
        lineNumBg = Color(0xFF161B22), border = Color(0xFF30363D),
        text = Color(0xFFE6EDF3), textSecondary = Color(0xFF7D8590), textMuted = Color(0xFF484F58),
        tabBg = Color(0xFF161B22), tabActive = Color(0xFF0D1117), statusBar = Color(0xFF238636),
        currentLineBg = Color(0xFF161B22), selectionBg = Color(0xFF264F78),
        findHighlight = Color(0xFFE3B341).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFE3B341).copy(alpha = 0.6f),
        minimapBg = Color(0xFF0D1117), scrollThumb = Color(0xFF30363D),
        gutterModified = Color(0xFF1F6FEB), gutterAdded = Color(0xFF238636), gutterDeleted = Color(0xFFDA3633),
        synString = Color(0xFFA5D6FF), synKeyword = Color(0xFFFF7B72), synComment = Color(0xFF8B949E),
        synNumber = Color(0xFF79C0FF), synType = Color(0xFFFFA657), synFunction = Color(0xFFD2A8FF),
        synOperator = Color(0xFFFF7B72), synAnnotation = Color(0xFFF78166),
        synPunctuation = Color(0xFFE6EDF3), synVariable = Color(0xFFFFA657), synConstant = Color(0xFF79C0FF),
        accent = Color(0xFF1F6FEB), accentGlow = Color(0xFF79C0FF), gold = Color(0xFFE3B341),
        danger = Color(0xFFDA3633), success = Color(0xFF238636), warning = Color(0xFFE3B341),
        isDark = true
    )

    val GithubLight = EditorColors(
        bg = Color(0xFFFFFFFF), surface = Color(0xFFF6F8FA), surfaceHover = Color(0xFFEAEEF2),
        lineNumBg = Color(0xFFF6F8FA), border = Color(0xFFD1D9E0),
        text = Color(0xFF1F2328), textSecondary = Color(0xFF636C76), textMuted = Color(0xFF9198A1),
        tabBg = Color(0xFFF6F8FA), tabActive = Color(0xFFFFFFFF), statusBar = Color(0xFF0969DA),
        currentLineBg = Color(0xFFF6F8FA), selectionBg = Color(0xFFBBD5FB),
        findHighlight = Color(0xFFFFB400).copy(alpha = 0.4f), findCurrentHighlight = Color(0xFFFF8C00).copy(alpha = 0.6f),
        minimapBg = Color(0xFFF6F8FA), scrollThumb = Color(0xFFC5CACE),
        gutterModified = Color(0xFF0969DA), gutterAdded = Color(0xFF1A7F37), gutterDeleted = Color(0xFFCF222E),
        synString = Color(0xFF0A3069), synKeyword = Color(0xFFCF222E), synComment = Color(0xFF57606A),
        synNumber = Color(0xFF0550AE), synType = Color(0xFF953800), synFunction = Color(0xFF8250DF),
        synOperator = Color(0xFFCF222E), synAnnotation = Color(0xFFCF222E),
        synPunctuation = Color(0xFF1F2328), synVariable = Color(0xFF953800), synConstant = Color(0xFF0550AE),
        accent = Color(0xFF0969DA), accentGlow = Color(0xFF79C0FF), gold = Color(0xFFB08800),
        danger = Color(0xFFCF222E), success = Color(0xFF1A7F37), warning = Color(0xFFB08800),
        isDark = false
    )

    val OneDark = EditorColors(
        bg = Color(0xFF282C34), surface = Color(0xFF21252B), surfaceHover = Color(0xFF2C313A),
        lineNumBg = Color(0xFF21252B), border = Color(0xFF3D4148),
        text = Color(0xFFABB2BF), textSecondary = Color(0xFF7F848E), textMuted = Color(0xFF5A5F68),
        tabBg = Color(0xFF21252B), tabActive = Color(0xFF282C34), statusBar = Color(0xFF528BFF),
        currentLineBg = Color(0xFF2C313A), selectionBg = Color(0xFF3E4451),
        findHighlight = Color(0xFFE5C07B).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFD19A66).copy(alpha = 0.5f),
        minimapBg = Color(0xFF282C34), scrollThumb = Color(0xFF3D4148),
        gutterModified = Color(0xFF528BFF), gutterAdded = Color(0xFF98C379), gutterDeleted = Color(0xFFE06C75),
        synString = Color(0xFF98C379), synKeyword = Color(0xFFC678DD), synComment = Color(0xFF5C6370),
        synNumber = Color(0xFFD19A66), synType = Color(0xFFE5C07B), synFunction = Color(0xFF61AFEF),
        synOperator = Color(0xFF56B6C2), synAnnotation = Color(0xFFE06C75),
        synPunctuation = Color(0xFFABB2BF), synVariable = Color(0xFFE06C75), synConstant = Color(0xFFD19A66),
        accent = Color(0xFF528BFF), accentGlow = Color(0xFF61AFEF), gold = Color(0xFFE5C07B),
        danger = Color(0xFFE06C75), success = Color(0xFF98C379), warning = Color(0xFFD19A66),
        isDark = true
    )

    val Nord = EditorColors(
        bg = Color(0xFF2E3440), surface = Color(0xFF3B4252), surfaceHover = Color(0xFF434C5E),
        lineNumBg = Color(0xFF3B4252), border = Color(0xFF4C566A),
        text = Color(0xFFECEFF4), textSecondary = Color(0xFFD8DEE9), textMuted = Color(0xFF4C566A),
        tabBg = Color(0xFF3B4252), tabActive = Color(0xFF2E3440), statusBar = Color(0xFF5E81AC),
        currentLineBg = Color(0xFF3B4252), selectionBg = Color(0xFF434C5E),
        findHighlight = Color(0xFFEBCB8B).copy(alpha = 0.35f), findCurrentHighlight = Color(0xFFD08770).copy(alpha = 0.5f),
        minimapBg = Color(0xFF2E3440), scrollThumb = Color(0xFF4C566A),
        gutterModified = Color(0xFF5E81AC), gutterAdded = Color(0xFFA3BE8C), gutterDeleted = Color(0xFFBF616A),
        synString = Color(0xFFA3BE8C), synKeyword = Color(0xFF81A1C1), synComment = Color(0xFF616E88),
        synNumber = Color(0xFFB48EAD), synType = Color(0xFF8FBCBB), synFunction = Color(0xFF88C0D0),
        synOperator = Color(0xFF81A1C1), synAnnotation = Color(0xFFD08770),
        synPunctuation = Color(0xFFECEFF4), synVariable = Color(0xFFD8DEE9), synConstant = Color(0xFFB48EAD),
        accent = Color(0xFF5E81AC), accentGlow = Color(0xFF88C0D0), gold = Color(0xFFEBCB8B),
        danger = Color(0xFFBF616A), success = Color(0xFFA3BE8C), warning = Color(0xFFEBCB8B),
        isDark = true
    )

    fun get(theme: EditorTheme): EditorColors = when (theme) {
        EditorTheme.VSCODE_DARK -> VsCodeDark
        EditorTheme.VSCODE_LIGHT -> VsCodeLight
        EditorTheme.MONOKAI -> Monokai
        EditorTheme.SOLARIZED_DARK -> SolarizedDark
        EditorTheme.SOLARIZED_LIGHT -> SolarizedLight
        EditorTheme.DRACULA -> Dracula
        EditorTheme.GITHUB_DARK -> GithubDark
        EditorTheme.GITHUB_LIGHT -> GithubLight
        EditorTheme.ONE_DARK -> OneDark
        EditorTheme.NORD -> Nord
    }
}
