package io.github.norbertweb.bluebird.ui.components

// ─────────────────────────────────────────────────────────
// SELAWIK TYPOGRAPHY — not active yet, read this first
//
// Selawik is Microsoft's official open-source substitute for Segoe UI
// (metrics-compatible, SIL Open Font License — free to bundle/redistribute,
// unlike Segoe UI itself which is Windows-licensed and can't legally be
// shipped in a third-party app).
//
// SETUP (do this before uncommenting anything below):
//   1. Download the font files from https://github.com/Microsoft/Selawik
//      (Regular and Bold at minimum; there's also a variable-weight build).
//   2. Put the .ttf files in res/font/ as:
//        res/font/selawik_regular.ttf
//        res/font/selawik_semibold.ttf   (optional, falls back to regular)
//        res/font/selawik_bold.ttf
//      (Android resource filenames must be lowercase + underscores only.)
//   3. Uncomment the code below and delete this comment block.
//   4. Wire it into your theme: wherever MaterialTheme(...) is called
//      (your Theme.kt / MainActivity.kt — I don't have that file), pass
//      `typography = BluebirdTypography` as a parameter. That's the one
//      change needed to apply this across every screen at once, since
//      every Text() that doesn't set its own fontFamily inherits from
//      MaterialTheme's Typography.
//
// ─────────────────────────────────────────────────────────

/*
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.norbertweb.bluebird.R

val SelawikFontFamily = FontFamily(
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_semibold, FontWeight.SemiBold),
    Font(R.font.selawik_bold, FontWeight.Bold)
)

// Built from Typography()'s defaults so every size/line-height stays
// Material-correct — only the font family changes app-wide.
val BluebirdTypography = Typography().let { base ->
    Typography(
        displayLarge   = base.displayLarge.copy(fontFamily = SelawikFontFamily),
        displayMedium  = base.displayMedium.copy(fontFamily = SelawikFontFamily),
        displaySmall   = base.displaySmall.copy(fontFamily = SelawikFontFamily),
        headlineLarge  = base.headlineLarge.copy(fontFamily = SelawikFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = SelawikFontFamily),
        headlineSmall  = base.headlineSmall.copy(fontFamily = SelawikFontFamily),
        titleLarge     = base.titleLarge.copy(fontFamily = SelawikFontFamily),
        titleMedium    = base.titleMedium.copy(fontFamily = SelawikFontFamily),
        titleSmall     = base.titleSmall.copy(fontFamily = SelawikFontFamily),
        bodyLarge      = base.bodyLarge.copy(fontFamily = SelawikFontFamily),
        bodyMedium     = base.bodyMedium.copy(fontFamily = SelawikFontFamily),
        bodySmall      = base.bodySmall.copy(fontFamily = SelawikFontFamily),
        labelLarge     = base.labelLarge.copy(fontFamily = SelawikFontFamily),
        labelMedium    = base.labelMedium.copy(fontFamily = SelawikFontFamily),
        labelSmall     = base.labelSmall.copy(fontFamily = SelawikFontFamily)
    )
}
*/
