package io.github.norbertweb.bluebird.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────
// SOFT UI — motion + depth system
//
// This is what actually sells the "floating panel" feel apps like
// SwiftKey/ChatGPT/Claude/HyperDroid have — it isn't one setting, it's a
// small set of tokens applied *consistently* everywhere, the same way `DS`
// unified color/corner-radius. Two pieces:
//
//   1. Motion  — spring specs instead of fixed-duration tween(). Springs
//      respond to velocity/interruption and feel organic; tween() always
//      feels slightly mechanical because it's linear/eased but time-locked.
//   2. softShadow() — a wide, low-opacity blurred shadow drawn manually,
//      instead of Modifier.shadow()'s default system shadow (small blur,
//      fairly dark). This is the actual visual difference between "a card
//      with a dropshadow" and "a panel floating above the page".
//
// What this file deliberately does NOT include: real backdrop blur (the
// content-behind-the-panel blur those apps also use). That needs the Haze
// library as a new Gradle dependency — see HazeBlurSetup.kt for that piece,
// kept separate so this file has zero new dependencies and is safe to drop
// in immediately.
// ─────────────────────────────────────────────────────────
object Motion {
    // The one "personality" every interactive transition in the app should
    // share — this consistency (not any single spring's tuning) is most of
    // why the reference apps feel cohesive rather than each screen having
    // its own animation feel.
    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** For panels/menus opening — a little overshoot, feels alive without being cartoonish. */
    fun <T> panel(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** For small interactive elements (icon press/scale, toggle pips). */
    fun <T> micro(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * A soft, wide, low-opacity shadow — draws several progressively larger,
 * progressively fainter rounded rects behind the content instead of relying
 * on Modifier.shadow()'s system shadow (single hard-edged blur, fairly
 * dark). Layering roundrects at falling alpha approximates a Gaussian blur
 * falloff using only stable DrawScope APIs — no native Canvas/BlurMaskFilter
 * involved, so this has no minSdk requirement and no fragile API surface.
 *
 * Usage: `Modifier.softShadow(cornerRadius = DS.overlayCorner)` in place of
 * `.shadow(...)`.
 */
fun Modifier.softShadow(
    cornerRadius: Dp,
    color: Color = Color.Black,
    alpha: Float = 0.22f,
    blurRadius: Dp = 32.dp,
    offsetY: Dp = 10.dp,
    layers: Int = 6
): Modifier = drawBehind {
    val radiusPx = cornerRadius.toPx()
    val blurPx = blurRadius.toPx()
    val offsetPx = offsetY.toPx()
    for (i in layers downTo 1) {
        val t = i / layers.toFloat()
        val expand = blurPx * t
        // Falls off faster near the edges (closer to t=1) than near the
        // shape itself — rough approximation of a Gaussian blur profile.
        val layerAlpha = (alpha * (1f - t) * (2f / layers)).coerceIn(0f, 1f)
        drawRoundRect(
            color = color.copy(alpha = layerAlpha),
            topLeft = Offset(-expand, offsetPx - expand),
            size = Size(size.width + expand * 2, size.height + expand * 2),
            cornerRadius = CornerRadius(radiusPx + expand, radiusPx + expand)
        )
    }
}
