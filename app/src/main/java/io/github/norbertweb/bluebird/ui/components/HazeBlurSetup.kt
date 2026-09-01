package io.github.norbertweb.bluebird.ui.components

// ─────────────────────────────────────────────────────────
// REAL BACKDROP BLUR — not active yet, read this first
//
// Everything else in the "soft UI" pass (Motion, softShadow, DS.glass
// opacity) works with zero new dependencies. This one piece — actually
// blurring the content *behind* a glass panel, which is what makes
// ChatGPT/Claude/SwiftKey-style panels feel like frosted glass instead of
// "faded color over whatever's underneath" — needs a library, because
// Jetpack Compose has no first-party cross-version blur-behind-content API
// (RenderEffect blur only exists on API 31+, and only blurs a composable's
// own content, not what's drawn behind it in a different layer).
//
// The standard solution is Haze (Chris Banes): https://github.com/chrisbanes/haze
//
// SETUP (do this before uncommenting anything below):
//   1. In your version catalog / build.gradle.kts (app module), add:
//        implementation("dev.chrisbanes.haze:haze:1.6.11")       // check for a newer version
//        implementation("dev.chrisbanes.haze:haze-materials:1.6.11")
//      (Haze needs no minSdk bump — it has its own fallback under API 31.)
//   2. Sync Gradle.
//   3. Uncomment the code below and delete this comment block.
//
// USAGE ONCE ENABLED:
//   - Wrap whatever sits BEHIND your glass panels (the Desktop content,
//     wallpaper, open windows) with `Modifier.hazeSource(hazeState)`.
//   - On the glass panel itself (Start Menu / Search / Taskbar background),
//     replace `.background(DS.glass(isDark, opacity))` with
//     `.hazeEffect(hazeState) { backgroundColor = DS.glass(isDark, opacity) }`
//     — this both blurs what's behind AND tints it, in one call.
//   - `hazeState` needs to be created once near the root of your screen
//     (e.g. in whatever composable hosts Desktop + Taskbar + StartMenu
//     together) and passed down to each of them.
//
// ─────────────────────────────────────────────────────────

/*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun rememberBluebirdHazeState(): HazeState = remember { HazeState() }

// Drop-in equivalent of `.background(DS.glass(isDark, opacity))` that also
// blurs whatever is behind the panel, instead of just tinting it.
fun Modifier.softGlass(hazeState: HazeState, isDark: Boolean, opacity: Float): Modifier =
    this.hazeEffect(hazeState) {
        // Haze's own blurRadius/tint API — tune to taste once you can see it live.
        backgroundColor = DS.glass(isDark, opacity)
    }
*/
