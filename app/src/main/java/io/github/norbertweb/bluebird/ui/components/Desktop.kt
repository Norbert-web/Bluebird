package io.github.norbertweb.bluebird.ui.components
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Icons come from the shared FluentIcon object (FluentIcon.kt), which wraps
// the io.github.niyajali:fluentui-system-icons Compose Multiplatform library.
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import io.github.norbertweb.bluebird.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Wallpaper Gradients  (used in APPARENT mode)
// ─────────────────────────────────────────────────────────────────
val wallpaperGradients = listOf(
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
    listOf(Color(0xFF141E30), Color(0xFF243B55)),
    listOf(Color(0xFF000000), Color(0xFF434343)),
    listOf(Color(0xFF1a2a6c), Color(0xFFb21f1f), Color(0xFFfdbb2d)),
    listOf(Color(0xFF00b09b), Color(0xFF96c93d))
)

// ─────────────────────────────────────────────────────────────────
// Background particle animation engine — Kotlin/Compose, single Canvas +
// single frame ticker driving any mix of active BgAnimationType values at
// once (mix mode is just multiple types sharing one particle pool). Sits
// between the wallpaper and the desktop icons; forced off whenever a live
// wallpaper is active (see BackgroundEffectsState's mutual-exclusion rule).
// ─────────────────────────────────────────────────────────────────
private data class BgParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float,
    var rotation: Float,
    var rotSpeed: Float,
    var phase: Float,
    var swayAmp: Float,
    var alpha: Float,
    var glyph: String,
    var color: Color,
    val type: BgAnimationType,
    var twinkleSpeed: Float = 1f
)

private val BG_PARTICLE_BASE_COUNT = mapOf(
    BgAnimationType.SNOW to 50, BgAnimationType.BUBBLES to 20, BgAnimationType.STARS to 100,
    BgAnimationType.RAIN to 100, BgAnimationType.HEARTS to 15, BgAnimationType.CONFETTI to 50,
    BgAnimationType.FIREFLIES to 30, BgAnimationType.LEAVES to 25, BgAnimationType.MATRIX to 15,
    BgAnimationType.SAKURA to 30
)
private val BG_SNOW_GLYPHS     = listOf("❄", "❅", "❆")
private val BG_HEART_GLYPHS    = listOf("❤", "💕", "💖", "💗")
private val BG_LEAF_GLYPHS     = listOf("🍂", "🍁", "🍃")
private val BG_CONFETTI_COLORS = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1), Color(0xFFF7B731),
    Color(0xFF5F27CD), Color(0xFF00D2D3), Color(0xFFFF9FF3)
)
private const val BG_MATRIX_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#\$%^&*()_+-=[]{}|;:,.<>?/~`"

private fun spawnBgParticle(type: BgAnimationType, w: Float, h: Float, rng: kotlin.random.Random): BgParticle {
    val x = rng.nextFloat() * w
    return when (type) {
        BgAnimationType.SNOW -> BgParticle(
            x, rng.nextFloat() * -h, 0f, rng.nextFloat() * 26f + 18f, rng.nextFloat() * 16f + 10f,
            0f, 0f, rng.nextFloat() * 6.28f, rng.nextFloat() * 40f - 20f, 1f,
            BG_SNOW_GLYPHS.random(rng), Color.White, type
        )
        BgAnimationType.BUBBLES -> BgParticle(
            x, h + rng.nextFloat() * h, 0f, -(rng.nextFloat() * 30f + 24f), rng.nextFloat() * 40f + 20f,
            0f, 0f, rng.nextFloat() * 6.28f, 0f, 0.45f,
            "", Color(0xFF80D8FF), type
        )
        BgAnimationType.STARS -> BgParticle(
            rng.nextFloat() * w, rng.nextFloat() * h, 0f, 0f, rng.nextFloat() * 3f + 1f,
            0f, 0f, rng.nextFloat() * 6.28f, 0f, rng.nextFloat(),
            "", Color.White, type, twinkleSpeed = rng.nextFloat() * 2f + 1f
        )
        BgAnimationType.RAIN -> BgParticle(
            x, rng.nextFloat() * -h, -24f, rng.nextFloat() * 340f + 460f, rng.nextFloat() * 14f + 16f,
            0f, 0f, 0f, 0f, 0.55f,
            "", Color(0xFF80C8FF), type
        )
        BgAnimationType.HEARTS -> BgParticle(
            x, h + rng.nextFloat() * h, 0f, -(rng.nextFloat() * 22f + 16f), rng.nextFloat() * 12f + 16f,
            0f, 0f, rng.nextFloat() * 6.28f, rng.nextFloat() * 30f - 15f, 1f,
            BG_HEART_GLYPHS.random(rng), Color.Unspecified, type
        )
        BgAnimationType.CONFETTI -> BgParticle(
            x, rng.nextFloat() * -h, rng.nextFloat() * 40f - 20f, rng.nextFloat() * 70f + 70f, rng.nextFloat() * 7f + 5f,
            rng.nextFloat() * 360f, rng.nextFloat() * 220f - 110f, 0f, 0f, 1f,
            "", BG_CONFETTI_COLORS.random(rng), type
        )
        BgAnimationType.FIREFLIES -> BgParticle(
            x, rng.nextFloat() * h * 0.8f + h * 0.1f, 0f, 0f, rng.nextFloat() * 3f + 2.5f,
            0f, 0f, rng.nextFloat() * 6.28f, 0f, rng.nextFloat() * 0.6f + 0.3f,
            "", Color(0xFFFFEB3B), type, twinkleSpeed = rng.nextFloat() * 1.5f + 0.5f
        )
        BgAnimationType.LEAVES -> BgParticle(
            x, rng.nextFloat() * -h, 0f, rng.nextFloat() * 22f + 16f, rng.nextFloat() * 12f + 16f,
            0f, rng.nextFloat() * 140f - 70f, rng.nextFloat() * 6.28f, rng.nextFloat() * 50f - 25f, 1f,
            BG_LEAF_GLYPHS.random(rng), Color.Unspecified, type
        )
        BgAnimationType.MATRIX -> BgParticle(
            x, rng.nextFloat() * -h, 0f, rng.nextFloat() * 170f + 170f, rng.nextFloat() * 8f + 15f,
            0f, 0f, 0f, 0f, rng.nextFloat() * 0.5f + 0.5f,
            BG_MATRIX_CHARS.random(rng).toString(), Color(0xFF00FF41), type
        )
        BgAnimationType.SAKURA -> BgParticle(
            x, rng.nextFloat() * -h, 0f, rng.nextFloat() * 20f + 14f, rng.nextFloat() * 12f + 16f,
            0f, rng.nextFloat() * 90f - 45f, rng.nextFloat() * 6.28f, rng.nextFloat() * 40f - 20f, 1f,
            "🌸", Color.Unspecified, type
        )
    }
}

private fun stepBgParticle(p: BgParticle, dt: Float, w: Float, h: Float, rng: kotlin.random.Random) {
    p.phase += dt
    when (p.type) {
        BgAnimationType.SNOW, BgAnimationType.HEARTS, BgAnimationType.LEAVES, BgAnimationType.SAKURA -> {
            p.y += p.vy * dt
            p.x += kotlin.math.sin(p.phase) * p.swayAmp * dt
            p.rotation += p.rotSpeed * dt
            if (p.y > h + 40f) { p.y = -30f; p.x = rng.nextFloat() * w }
        }
        BgAnimationType.RAIN, BgAnimationType.MATRIX -> {
            p.y += p.vy * dt
            p.x += p.vx * dt
            if (p.y > h + 40f) { p.y = -30f; p.x = rng.nextFloat() * w }
        }
        BgAnimationType.BUBBLES -> {
            p.y += p.vy * dt
            p.x += kotlin.math.sin(p.phase * 0.7f) * 22f * dt
            if (p.y < -60f) { p.y = h + 40f; p.x = rng.nextFloat() * w }
        }
        BgAnimationType.STARS -> {
            p.alpha = 0.3f + 0.7f * ((kotlin.math.sin(p.phase * p.twinkleSpeed) + 1f) / 2f)
        }
        BgAnimationType.FIREFLIES -> {
            p.x += kotlin.math.sin(p.phase * 0.6f) * 26f * dt
            p.y += kotlin.math.cos(p.phase * 0.5f) * 26f * dt
            p.alpha = 0.2f + 0.8f * ((kotlin.math.sin(p.phase * p.twinkleSpeed) + 1f) / 2f)
        }
        BgAnimationType.CONFETTI -> {
            p.y += p.vy * dt
            p.x += p.vx * dt
            p.rotation += p.rotSpeed * dt
            if (p.y > h + 40f) { p.y = -30f; p.x = rng.nextFloat() * w }
        }
    }
}

private fun drawBgParticle(scope: DrawScope, p: BgParticle, paint: android.graphics.Paint) {
    val canvas = scope.drawContext.canvas.nativeCanvas
    when (p.type) {
        BgAnimationType.SNOW -> {
            paint.textSize = p.size * scope.density
            paint.color = android.graphics.Color.argb((p.alpha * 255).toInt(), 255, 255, 255)
            canvas.drawText(p.glyph, p.x, p.y, paint)
        }
        BgAnimationType.HEARTS -> {
            paint.textSize = p.size * scope.density
            paint.color = android.graphics.Color.argb((p.alpha * 255).toInt(), 255, 90, 120)
            canvas.drawText(p.glyph, p.x, p.y, paint)
        }
        BgAnimationType.LEAVES, BgAnimationType.SAKURA -> {
            paint.textSize = p.size * scope.density
            paint.alpha = (p.alpha * 255).toInt()
            canvas.save(); canvas.rotate(p.rotation, p.x, p.y)
            canvas.drawText(p.glyph, p.x, p.y, paint)
            canvas.restore()
        }
        BgAnimationType.MATRIX -> {
            paint.textSize = p.size * scope.density
            paint.color = android.graphics.Color.argb((p.alpha * 255).toInt(), 0, 255, 65)
            canvas.drawText(p.glyph, p.x, p.y, paint)
        }
        BgAnimationType.BUBBLES -> scope.drawCircle(
            color = Color(0xFF80D8FF).copy(alpha = p.alpha * 0.5f),
            radius = p.size / 2f, center = Offset(p.x, p.y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
        BgAnimationType.STARS -> scope.drawCircle(
            color = Color.White.copy(alpha = p.alpha), radius = p.size / 2f, center = Offset(p.x, p.y)
        )
        BgAnimationType.RAIN -> scope.drawLine(
            color = Color(0xFF80C8FF).copy(alpha = p.alpha),
            start = Offset(p.x, p.y), end = Offset(p.x + p.vx * 0.05f, p.y + p.size),
            strokeWidth = 1.5f
        )
        BgAnimationType.CONFETTI -> {
            canvas.save(); canvas.rotate(p.rotation, p.x, p.y)
            scope.drawRect(
                color = p.color,
                topLeft = Offset(p.x - p.size / 2f, p.y - p.size * 0.3f),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
            )
            canvas.restore()
        }
        BgAnimationType.FIREFLIES -> scope.drawCircle(
            color = Color(0xFFFFEB3B).copy(alpha = p.alpha), radius = p.size, center = Offset(p.x, p.y)
        )
    }
}

@Composable
private fun BackgroundAnimationLayer(
    activeTypes: Set<BgAnimationType>,
    intensity: Int,
    modifier: Modifier = Modifier
) {
    if (activeTypes.isEmpty()) return
    val particles = remember { mutableListOf<BgParticle>() }
    var sizeW by remember { mutableStateOf(0f) }
    var sizeH by remember { mutableStateOf(0f) }
    var tick by remember { mutableStateOf(0L) }
    val rng = remember { kotlin.random.Random(System.nanoTime()) }
    val paint = remember {
        android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }
    }

    LaunchedEffect(activeTypes, intensity, sizeW, sizeH) {
        if (sizeW <= 0f || sizeH <= 0f) return@LaunchedEffect
        particles.clear()
        activeTypes.forEach { type ->
            val base  = BG_PARTICLE_BASE_COUNT[type] ?: 30
            val count = ((base * intensity) / 50f).toInt().coerceAtLeast(1)
            repeat(count) { particles.add(spawnBgParticle(type, sizeW, sizeH, rng)) }
        }
    }

    LaunchedEffect(activeTypes, intensity) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt  = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrame = now
            if (sizeW > 0f && sizeH > 0f) {
                particles.forEach { stepBgParticle(it, dt, sizeW, sizeH, rng) }
            }
            tick++
            // Background particles do not need a 60 Hz simulation. Limiting the
            // animation to ~30 Hz cuts wakeups and snapshot invalidations roughly
            // in half while retaining smooth motion for this decorative layer.
            delay(33)
        }
    }

    Canvas(
        modifier = modifier.onGloballyPositioned { coords ->
            sizeW = coords.size.width.toFloat()
            sizeH = coords.size.height.toFloat()
        }
    ) {
        @Suppress("UNUSED_EXPRESSION") tick  // read to invalidate this draw scope every frame
        particles.forEach { p -> drawBgParticle(this, p, paint) }
    }
}

// ─────────────────────────────────────────────────────────────────
// Live wallpapers — 4 built-in continuously-animated backgrounds, entirely
// procedural (no video/image assets). Mutually exclusive with the particle
// animation layer above (see BackgroundEffectsState).
// ─────────────────────────────────────────────────────────────────
@Composable
// internal (was private): now also called from SettingsScreen.kt's
// PersonalizationSection, which ports this same "Effects" tab content into
// Settings so Personalize no longer needs its own separate dialog surface.
internal fun LiveWallpaperRenderer(
    type: LiveWallpaperType,
    modifier: Modifier = Modifier,
    // The Effects-tab picker previously rendered all 4 wallpapers simultaneously, each
    // running its own continuous animation loop, purely to show small preview thumbnails
    // — real, avoidable CPU/GPU cost for a picker UI that's just showing static previews.
    // Only the actual full-screen active wallpaper needs to animate.
    animated: Boolean = true
) {
    // 10s cycle (was 20s) — the previous speed combined with the soft, low-opacity shapes
    // in Aurora/Bokeh made their motion too subtle to read as animated at a glance.
    //
    // Only create the InfiniteTransition when actually animating — merely not READING its
    // value isn't enough to stop the cost: rememberInfiniteTransition registers a
    // continuously-ticking animation with the composition regardless of whether anything
    // reads its output, so the picker's 4 simultaneous "previews" were still each running
    // a real per-frame animation loop even though they only ever showed a static frame.
    val t = if (animated) {
        val infinite = rememberInfiniteTransition(label = "live_wallpaper")
        val animatedT by infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
            label = "live_wallpaper_t"
        )
        animatedT
    } else {
        0.35f  // fixed mid-cycle frame — a more representative still than t=0 for some designs
    }
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        when (type) {
            LiveWallpaperType.AURORA -> {
                drawRect(Color(0xFF060B1A))
                val bands = listOf(
                    Color(0xFF00FFA3), Color(0xFF00C2FF), Color(0xFF7A5CFF)
                )
                bands.forEachIndexed { i, col ->
                    val phase = t * 6.283f + i * 2.1f
                    // Both vertical AND horizontal drift now (real aurora ribbons undulate
                    // side to side, not just up/down), with a much larger swing so the
                    // motion reads clearly instead of blending into a near-static gradient.
                    val cy = h * (0.22f + 0.16f * i) + kotlin.math.sin(phase) * h * 0.22f
                    val xShift = kotlin.math.cos(phase * 0.8f) * w * 0.18f
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(col.copy(alpha = 0.38f), col.copy(alpha = 0.08f), Color.Transparent),
                            startY = cy - h * 0.18f, endY = cy + h * 0.4f
                        ),
                        topLeft = Offset(xShift, 0f), size = androidx.compose.ui.geometry.Size(w, h)
                    )
                }
            }
            LiveWallpaperType.NEBULA -> {
                drawRect(Color(0xFF05040F))
                val rng = kotlin.random.Random(42)
                repeat(140) {
                    val sx = rng.nextFloat() * w
                    val sy = rng.nextFloat() * h
                    val twinkle = 0.4f + 0.6f * ((kotlin.math.sin(t * 6.283f * 3f + sx) + 1f) / 2f)
                    drawCircle(Color.White.copy(alpha = twinkle * 0.8f), radius = rng.nextFloat() * 1.6f + 0.4f, center = Offset(sx, sy))
                }
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF7A2CFF).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(w * (0.3f + 0.4f * t), h * 0.4f), radius = w * 0.5f
                    )
                )
            }
            LiveWallpaperType.WAVES -> {
                drawRect(Color(0xFF0A1930))
                val colors = listOf(Color(0xFF0078D4), Color(0xFF00C2FF), Color(0xFF7A5CFF))
                colors.forEachIndexed { i, col ->
                    val path = androidx.compose.ui.graphics.Path()
                    val amp = h * 0.05f
                    val baseY = h * (0.5f + i * 0.15f)
                    path.moveTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val y = baseY + kotlin.math.sin((x / w) * 6.283f * 2f + t * 6.283f + i) * amp
                        path.lineTo(x, y)
                        x += w / 60f
                    }
                    path.lineTo(w, h); path.lineTo(0f, h); path.close()
                    drawPath(path, color = col.copy(alpha = 0.22f))
                }
            }
            LiveWallpaperType.BOKEH -> {
                drawRect(Color(0xFF14213D))
                val rng = kotlin.random.Random(7)
                repeat(18) { i ->
                    val baseX = rng.nextFloat() * w
                    val baseY = rng.nextFloat() * h
                    // Bigger, faster drift + higher opacity than before — at drift=30px and
                    // alpha=0.12 the orbs barely read as moving against their own soft edges.
                    val phase = t * 6.283f + i
                    val driftX = kotlin.math.sin(phase) * w * 0.12f
                    val driftY = kotlin.math.cos(phase * 0.7f) * h * 0.08f
                    val r = rng.nextFloat() * 46f + 28f
                    val pulse = 0.85f + 0.15f * kotlin.math.sin(phase * 1.3f)
                    drawCircle(
                        Color(listOf(0xFF64B5F6, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFB74D)[i % 4].toInt())
                            .copy(alpha = 0.22f),
                        radius = r * pulse, center = Offset(baseX + driftX, baseY + driftY)
                    )
                }
            }
            LiveWallpaperType.NONE -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Media Extensions
// ─────────────────────────────────────────────────────────────────
private val MUSIC_EXTS = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma")
private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "wmv", "m4v")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
private val TEXT_EXTS  = setOf("txt", "md", "log", "json", "xml", "csv", "html", "htm", "js", "py", "kt")

// ─────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────
data class DesktopFileInfo(
    val id: String,
    val file: File,
    val name: String,
    val type: DesktopItemType,
    val packageName: String? = null,
    val iconBitmap: Bitmap? = null,
    val position: Offset = Offset.Zero,
    val isSelected: Boolean = false,
    // Only populated for WEB_APP_SHORTCUT items — parsed from the .webapp file
    val webAppId: String? = null,
    val webAppUrl: String? = null,
    val webAppIconPath: String? = null,  // path relative to context.filesDir
    // Non-package Bluebird-native app target (e.g. Text Editor, Terminal).
    val builtInScreen: LauncherScreen? = null,
    // For .desktop shortcuts that point to a real file rather than an app.
    val targetFilePath: String? = null
)

enum class DesktopItemType {
    FOLDER, TEXT_FILE, IMAGE_FILE, MUSIC_FILE, VIDEO_FILE,
    APP_SHORTCUT, WEB_APP_SHORTCUT, OTHER_FILE, THIS_PC, RECYCLE_BIN, SETTINGS_ICON
}

enum class DesktopIconSize { SMALL, MEDIUM, LARGE }
enum class DesktopSortMode { NONE, NAME, DATE_MODIFIED, TYPE, SIZE }

data class InlineRenameState(
    val targetId: String,
    val initialName: String        // FIX: immutable initial — never updated by keystrokes
)

data class AppInfoItem(
    val label: String,
    val packageName: String,
    val iconBitmap: Bitmap
)

// ─────────────────────────────────────────────────────────────────
// Universal Drawable → Bitmap converter
// ─────────────────────────────────────────────────────────────────
fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bm)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bm
}

// ─────────────────────────────────────────────────────────────────
// Icon Helpers
// ─────────────────────────────────────────────────────────────────
fun getFileIcon(file: File): androidx.compose.ui.graphics.vector.ImageVector = when {
    file.isDirectory -> FluentIcon.Folder
    file.extension.lowercase() in MUSIC_EXTS  -> FluentIcon.MusicNote2
    file.extension.lowercase() in VIDEO_EXTS  -> FluentIcon.PlayCircle
    file.extension.lowercase() in IMAGE_EXTS  -> FluentIcon.Image
    file.extension.lowercase() in TEXT_EXTS   -> FluentIcon.DocumentText
    file.extension.lowercase() == "pdf"       -> FluentIcon.DocumentPdf
    file.extension.lowercase() == "apk"       -> FluentIcon.Android
    file.extension.lowercase() in setOf("zip","rar","7z","tar","gz") -> FluentIcon.FolderZip
    file.extension.lowercase() in setOf("doc","docx") -> FluentIcon.DocumentText
    file.extension.lowercase() in setOf("xls","xlsx") -> FluentIcon.Table
    file.extension.lowercase() == "webapp" -> FluentIcon.Globe
    file.extension.lowercase() == "desktop" -> FluentIcon.Apps
    else -> FluentIcon.Document
}

fun getFileIconColor(file: File): Color = when {
    file.isDirectory -> Color(0xFFFFC107)
    file.extension.lowercase() in MUSIC_EXTS  -> Color(0xFFFF8C00)
    file.extension.lowercase() in VIDEO_EXTS  -> Color(0xFF8764B8)
    file.extension.lowercase() in IMAGE_EXTS  -> Color(0xFF16C60C)
    file.extension.lowercase() in TEXT_EXTS   -> Color(0xFF0078D4)
    file.extension.lowercase() == "pdf"       -> Color(0xFFD83B01)
    file.extension.lowercase() == "apk"       -> Color(0xFF107C10)
    file.extension.lowercase() in setOf("doc","docx") -> Color(0xFF0078D4)
    file.extension.lowercase() in setOf("xls","xlsx") -> Color(0xFF217346)
    file.extension.lowercase() in setOf("zip","rar","7z") -> Color(0xFF8B6914)
    file.extension.lowercase() == "webapp" -> Color(0xFF0078D4)
    file.extension.lowercase() == "desktop" -> Color(0xFF0078D4)
    else -> Color(0xFF9E9E9E)
}

// ─────────────────────────────────────────────────────────────────
// Shared shortcut-file parser
//
// Single source of truth for turning a file on the Desktop into a
// DesktopFileInfo. Both the Desktop screen and LauncherViewModel
// (which backs File Explorer's view of the same folder) call this,
// instead of each maintaining their own parsing logic — so a format
// change here (e.g. adding a new shortcut type) only has to happen
// once, and Desktop / File Explorer / Recycle Bin never disagree
// about what a given file on disk actually is.
// ─────────────────────────────────────────────────────────────────
private val MEDIA_THUMB_SAMPLE = BitmapFactory.Options().apply { inSampleSize = 4 }

// ─────────────────────────────────────────────────────────────────
// Unified press/tap/long-press/drag gesture detector.
//
// Previously, icons (and the desktop background) each stacked TWO
// separate, independent pointerInput gesture detectors covering the
// identical hit area — e.g. a parent doing detectDragGesturesAfterLongPress
// while a child did its own detectTapGestures. In Compose, ancestor and
// descendant pointerInput blocks each get their own independent pass over
// the same pointer event stream, so they silently race each other: whichever
// claims the gesture first can starve the other. That's what caused
// unreliable selection, a context menu that wouldn't consistently open, and
// twitchy drag starts.
//
// This single detector replaces both, so there is exactly one state machine
// per interactive surface: tap → onTap, double-tap → onDoubleTap, long-press
// held then released without moving → onLongPressReleased (used for context
// menus), long-press then moved → onDragStart/onDrag/onDragEnd/onDragCancel.
// ─────────────────────────────────────────────────────────────────
// Kotlin's RestrictsSuspension on AwaitPointerEventScope only allows suspend calls made
// directly within it (or within functions declared as extensions ON it) — a LOCAL suspend
// fun declared inside the awaitEachGesture block doesn't count as such an extension, so its
// calls to awaitPointerEvent()/drag() get rejected by the compiler. Declaring these as real
// top-level extensions of AwaitPointerEventScope (instead of local closures) fixes that.
private suspend fun AwaitPointerEventScope.awaitReleaseOrSlop(
    pid: PointerId, downPos: Offset, slop: Float, timeoutMs: Long
): String =
    withTimeoutOrNull(timeoutMs) {
        while (true) {
            val event  = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pid }
                ?: return@withTimeoutOrNull "cancel"
            if (!change.pressed) return@withTimeoutOrNull "released"
            // A child surface (for example a desktop icon) may own this pointer.
            // If it has already consumed the movement, the background must never
            // interpret the same gesture as a lasso drag.
            if (change.isConsumed) return@withTimeoutOrNull "consumed"
            val d = change.position - downPos
            if (kotlin.math.abs(d.x) > slop || kotlin.math.abs(d.y) > slop)
                return@withTimeoutOrNull "moved"
        }
        @Suppress("UNREACHABLE_CODE") "cancel"
    } ?: "timeout"

private suspend fun AwaitPointerEventScope.performDrag(
    pid: PointerId,
    downPos: Offset,
    onDragStart: (Offset) -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    onDragStart(downPos)
    val completed = drag(pid) { change ->
        onDrag(change, change.positionChange())
        change.consume()
    }
    if (completed) onDragEnd() else onDragCancel()
}

suspend fun PointerInputScope.detectPressDragGestures(
    consumeDown: Boolean = false,
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onLongPressReleased: (Offset) -> Unit = {},
    // When true, long-press is committed as soon as the timeout is reached.
    // This makes touch context menus feel immediate instead of requiring a precise
    // stationary release, while movement before the timeout still starts a drag.
    longPressOnTimeout: Boolean = false,
    onSecondaryTap: (Offset) -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val doubleTapTimeout  = viewConfiguration.doubleTapTimeoutMillis
    val slop              = viewConfiguration.touchSlop

    awaitEachGesture {
        val down    = awaitFirstDown(requireUnconsumed = !consumeDown)
        val downPos = down.position
        if (consumeDown) down.consume()
        val pid     = down.id

        // Mouse secondary-click owns the gesture immediately. This prevents a
        // right-click from also becoming a selection/drag gesture on desktop-mode devices.
        //
        // BUGFIX: this used to call awaitPointerEvent(PointerEventPass.Initial) here,
        // which BLOCKS until the next pointer event arrives. That's fine for a mouse
        // (a button-state event follows immediately), but for touch it's broken: a
        // stationary long-press produces NO further events until the finger moves or
        // lifts — Android doesn't synthesize periodic move events for a held-still
        // touch. So this call sat blocked for the entire hold, only unblocking on the
        // UP event. That UP event was then consumed here (buttons check false, falls
        // through), leaving nothing for awaitReleaseOrSlop() below to read: its own
        // awaitPointerEvent() call had nothing left in the queue and just waited out
        // its own timeout, so it reported "timeout" long after the finger had already
        // lifted. That's what made ordinary taps feel ~500ms slow (every tap paid the
        // timeout penalty) and made real long-presses/context-menus never fire during
        // the actual hold.
        //
        // Fix: read currentEvent instead of awaiting a new one. currentEvent reflects
        // the most recently processed event in this gesture — i.e. the down we just
        // got from awaitFirstDown() above — so this check is instant and non-blocking,
        // and awaitReleaseOrSlop() below is left free to see every subsequent event
        // (move/up) as it actually happens.
        if (currentEvent.buttons.isSecondaryPressed) {
            down.consume()
            onSecondaryTap(downPos)
            return@awaitEachGesture
        }

        when (awaitReleaseOrSlop(pid, downPos, slop, longPressTimeout)) {
            "released" -> {
                // Released quickly — check whether a second tap follows within the
                // double-tap window before committing to a single tap.
                val second = withTimeoutOrNull(doubleTapTimeout) {
                    awaitFirstDown(requireUnconsumed = false)
                }
                if (second != null) {
                    withTimeoutOrNull(longPressTimeout) { waitForUpOrCancellation() }
                    onDoubleTap(second.position)
                } else {
                    onTap(downPos)
                }
            }
            "timeout" -> {
                // Long-press threshold reached while still down and unmoved. For background
                // surfaces we can commit immediately, which is much more reliable on touch
                // devices than waiting for the exact release event. Icon drags keep the old
                // release-based behavior by leaving this flag false.
                if (longPressOnTimeout) {
                    onLongPressReleased(downPos)
                    when (awaitReleaseOrSlop(pid, downPos, slop, Long.MAX_VALUE / 2)) {
                        "released", "consumed" -> Unit
                        "moved" -> onDragCancel()
                        else -> onDragCancel()
                    }
                } else {
                    when (awaitReleaseOrSlop(pid, downPos, slop, Long.MAX_VALUE / 2)) {
                        "released" -> onLongPressReleased(downPos)
                        "moved"    -> performDrag(pid, downPos, onDragStart, onDrag, onDragEnd, onDragCancel)
                        else       -> onDragCancel()
                    }
                }
            }
            "moved"  -> {
                // A real desktop drag should not require a long-press first. The old
                // detector silently discarded movement that happened before the long-press
                // timeout, which made icon dragging feel sticky and caused users to move an
                // icon only after an awkward hold. Once touch-slop is crossed, treat it as a
                // drag immediately. The long-press path above is still reserved for the
                // context-menu gesture when the pointer remains stationary.
                performDrag(pid, downPos, onDragStart, onDrag, onDragEnd, onDragCancel)
            }
            "cancel", "consumed" -> onDragCancel()
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Icon bitmap cache — loadDesktopFileInfo runs on every refreshDesktopFiles()
// call (which fires often: after every paste/delete/rename/install, plus the
// FileObserver's own debounced triggers), and was previously re-decoding
// every icon from scratch every single time — including PackageManager app
// icon lookups, which are a real binder-IPC cost, not just a bitmap decode.
// This is very likely the main contributor to "desktop takes long to load,
// especially on other phones". Keyed so a changed/replaced file naturally
// invalidates its own stale entry without needing explicit eviction.
// ─────────────────────────────────────────────────────────────────
private object DesktopIconCache {
    // Capped + LRU-evicted — the previous unbounded ConcurrentHashMap held onto every
    // icon Bitmap it ever decoded for the entire app session (each rename/refresh with a
    // changed file creates a new cache key), which is a real, slow memory leak over a
    // long session — worth fixing on its own, and likely a contributor to the reported
    // "slow on other phones" symptom on lower-RAM devices. LinkedHashMap's access-order
    // mode + removeEldestEntry gives simple, correct LRU behavior; synchronized because
    // this can be hit from concurrent IO-dispatcher refreshes.
    private const val MAX_BYTES = 24L * 1024L * 1024L
    private val cache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {}
    private var cachedBytes = 0L

    @Synchronized fun get(key: String): Bitmap? = cache[key]

    @Synchronized fun put(key: String, bitmap: Bitmap): Bitmap {
        cache.remove(key)?.let { cachedBytes -= it.allocationByteCount.toLong() }
        cache[key] = bitmap
        cachedBytes += bitmap.allocationByteCount.toLong()
        while (cachedBytes > MAX_BYTES && cache.isNotEmpty()) {
            val eldest = cache.entries.iterator().next()
            cachedBytes -= eldest.value.allocationByteCount.toLong()
            cache.remove(eldest.key)
        }
        return bitmap
    }
}

fun loadDesktopFileInfo(file: File, context: android.content.Context): DesktopFileInfo? = try {
    val ext = file.extension.lowercase()
    when {
        file.isDirectory -> DesktopFileInfo(
            id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.FOLDER
        )

        ext == "desktop" -> {
            val lines = file.readLines()
            val pkg   = lines.find { it.startsWith("package=") }?.removePrefix("package=")?.trim() ?: ""
            val builtInName = lines.find { it.startsWith("bluebirdScreen=") }?.removePrefix("bluebirdScreen=")?.trim()
            val builtInScreen = builtInName?.let { name ->
                runCatching { LauncherScreen.valueOf(name) }.getOrNull()
            }
            val shortcutType = lines.find { it.startsWith("type=") }?.removePrefix("type=")?.trim()
            val targetFilePath = lines.find { it.startsWith("path=") }?.removePrefix("path=")?.trim()
                ?.takeIf { shortcutType == "file" && it.isNotBlank() }
            val label = lines.find { it.startsWith("label=") }?.removePrefix("label=")?.trim()
                ?: file.nameWithoutExtension
            val iconBmp: Bitmap? = if (pkg.isNotBlank()) {
                val cacheKey = "app:$pkg"
                DesktopIconCache.get(cacheKey) ?: try {
                    DesktopIconCache.put(cacheKey, drawableToBitmap(context.packageManager.getApplicationIcon(pkg)))
                } catch (_: Exception) { null }
            } else null
            DesktopFileInfo(
                id = file.absolutePath, file = file, name = label,
                type = DesktopItemType.APP_SHORTCUT, packageName = pkg.ifBlank { null },
                iconBitmap = iconBmp, builtInScreen = builtInScreen, targetFilePath = targetFilePath
            )
        }

        ext == "webapp" -> {
            val lines = file.readLines()
            fun field(key: String) = lines.find { it.startsWith("$key=") }?.removePrefix("$key=")?.trim()
            val label   = field("name") ?: file.nameWithoutExtension
            val url     = field("url") ?: ""
            val id      = field("id") ?: file.nameWithoutExtension
            val iconRel = field("icon")
            val iconBmp: Bitmap? = iconRel?.let {
                try {
                    val f = File(context.filesDir, it)
                    if (f.exists()) {
                        val cacheKey = "webapp:$it:${f.lastModified()}"
                        DesktopIconCache.get(cacheKey)
                            ?: BitmapFactory.decodeFile(f.absolutePath)?.let { bmp -> DesktopIconCache.put(cacheKey, bmp) }
                    } else {
                        null
                    }
                } catch (_: Exception) { null }
            }
            DesktopFileInfo(
                id = file.absolutePath, file = file, name = label,
                type = DesktopItemType.WEB_APP_SHORTCUT, iconBitmap = iconBmp,
                webAppId = id, webAppUrl = url, webAppIconPath = iconRel
            )
        }

        ext in MUSIC_EXTS -> DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.MUSIC_FILE)
        ext in VIDEO_EXTS -> DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.VIDEO_FILE)
        ext in IMAGE_EXTS -> {
            val cacheKey = "img:${file.absolutePath}:${file.lastModified()}"
            val thumb = DesktopIconCache.get(cacheKey) ?: try {
                BitmapFactory.decodeFile(file.absolutePath, MEDIA_THUMB_SAMPLE)?.let { DesktopIconCache.put(cacheKey, it) }
            } catch (_: Exception) { null }
            DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.IMAGE_FILE, iconBitmap = thumb)
        }
        ext in TEXT_EXTS -> DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.TEXT_FILE)

        else -> DesktopFileInfo(id = file.absolutePath, file = file, name = file.name, type = DesktopItemType.OTHER_FILE)
    }
} catch (_: Exception) { null }

/**
 * Opens any desktop-backed item the same way regardless of which screen triggered it
 * (Desktop icon double-tap, or File Explorer "Open"). Keeps open-behavior in one place.
 */
fun openDesktopItem(
    item: DesktopFileInfo,
    context: android.content.Context,
    viewModel: LauncherViewModel
) {
    when (item.type) {
        DesktopItemType.FOLDER ->
            viewModel.openWindow(
                LauncherScreen.FILE_EXPLORER,
                extras = mapOf("path" to item.file.absolutePath)
            )
        DesktopItemType.APP_SHORTCUT -> {
            // File shortcuts (.desktop with type=file) must open the TARGET file,
            // not merely launch the shortcut container.
            if (item.targetFilePath != null) {
                if (!viewModel.openFileInternally(context, item.targetFilePath)) {
                    viewModel.openFileWithSystem(context, item.targetFilePath)
                }
            // Bluebird-native shortcuts launch directly inside the desktop shell.
            } else if (item.builtInScreen != null) {
                viewModel.openWindow(item.builtInScreen)
            } else {
                item.packageName?.let { pkg ->
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
        DesktopItemType.WEB_APP_SHORTCUT ->
            viewModel.openWebAppWindow(
                id = item.webAppId ?: item.id,
                name = item.name,
                url = item.webAppUrl ?: "",
                iconPath = item.webAppIconPath
            )
        else -> {
            // Desktop files use the exact same native Bluebird routing as File Explorer.
            // Only unsupported formats fall through to the Android chooser.
            if (!viewModel.openFileInternally(context, item.file.absolutePath)) {
                viewModel.openFileWithSystem(context, item.file.absolutePath)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024          -> "$bytes B"
    bytes < 1_048_576     -> "%.1f KB".format(bytes / 1024f)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576f)
    else                  -> "%.2f GB".format(bytes / 1_073_741_824f)
}

private fun uniqueName(dir: File, baseName: String, ext: String = ""): String {
    val suffix = if (ext.isEmpty()) "" else ".$ext"
    if (!File(dir, "$baseName$suffix").exists()) return "$baseName$suffix"
    var counter = 2
    while (File(dir, "$baseName ($counter)$suffix").exists()) counter++
    return "$baseName ($counter)$suffix"
}

// ─────────────────────────────────────────────────────────────────
// Grid Metrics
// ─────────────────────────────────────────────────────────────────
private fun iconSizeDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 36f
    DesktopIconSize.MEDIUM -> 48f
    DesktopIconSize.LARGE  -> 64f
}

private fun cellWidthDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 74f
    DesktopIconSize.MEDIUM -> 86f
    DesktopIconSize.LARGE  -> 102f
}

private fun cellHeightDp(size: DesktopIconSize): Float = when (size) {
    DesktopIconSize.SMALL  -> 76f
    DesktopIconSize.MEDIUM -> 92f
    DesktopIconSize.LARGE  -> 110f
}

// ─────────────────────────────────────────────────────────────────
// FIX: autoGridPos — clamps to screen bounds so icons never overflow.
// Column-first layout (top→bottom then right), stops at maxCols.
// ─────────────────────────────────────────────────────────────────
private fun autoGridPos(
    idx: Int,
    rows: Int,
    maxCols: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    startPaddingPx: Float = 0f,
    topPaddingPx: Float = 0f,
    taken: MutableSet<Pair<Int, Int>> = mutableSetOf()
): Offset? {
    val safeRows = maxOf(1, rows)
    // When a taken-set is provided (auto-arrange with collision avoidance),
    // scan columns left-to-right and rows top-to-bottom for the first free cell.
    // When taken is empty (simple index-based placement), fall straight through
    // to the fast idx-based formula so existing call sites are unaffected.
    // BUG 11 FIX: idx==0 special-case was redundant; fast path handles it correctly.
    if (taken.isNotEmpty()) {
        // Scan every column up to maxCols for a free slot.
        for (col in 0 until maxCols) {
            for (row in 0 until safeRows) {
                val cell = Pair(col, row)
                if (cell !in taken) {
                    taken.add(cell)
                    return Offset(col * cellWidthPx + startPaddingPx, row * cellHeightPx + topPaddingPx)
                }
            }
        }
        // Grid is completely full.
        return null
    }
    // Fast path: no collision tracking needed — use direct index formula.
    // Keep the fallback layout inside the actual grid. The old implementation
    // allowed columns to grow past the visible desktop, making arrangements
    // become screen-unaware on small/rotated displays. Callers that need collision
    // avoidance use the taken-set path above.
    val totalSlots = (maxCols * safeRows).coerceAtLeast(1)
    if (idx >= totalSlots) return null
    val col = idx / safeRows
    val row = idx % safeRows
    return Offset(col * cellWidthPx + startPaddingPx, row * cellHeightPx + topPaddingPx)
}

// ─────────────────────────────────────────────────────────────────
// FIX: snapToGrid — respects bounds and avoids occupying a position
// already taken by another icon. Searches outward for a free cell.
// ─────────────────────────────────────────────────────────────────
private fun snapToGrid(
    pos: Offset,
    cellWidthPx: Float,
    cellHeightPx: Float,
    startPaddingPx: Float,
    topPaddingPx: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    occupiedPositions: Set<Pair<Int, Int>> = emptySet(),
    bottomSafeAreaPx: Float = 0f
): Offset? {
    // Use the actual usable canvas, not the raw screen height. This keeps grid snapping
    // out of taskbars/navigation overlays and makes portrait/landscape transitions agree
    // with the same bounds used by dragging.
    val usableHeight = (screenHeightPx - bottomSafeAreaPx).coerceAtLeast(topPaddingPx + cellHeightPx)
    val maxCols = ((screenWidthPx - startPaddingPx * 2) / cellWidthPx).toInt().coerceAtLeast(1)
    val maxRows = ((usableHeight - topPaddingPx * 2) / cellHeightPx).toInt().coerceAtLeast(1)

    val preferredCol = ((pos.x - startPaddingPx) / cellWidthPx).roundToInt()
        .coerceIn(0, maxCols - 1)
    val preferredRow = ((pos.y - topPaddingPx) / cellHeightPx).roundToInt()
        .coerceIn(0, maxRows - 1)

    if (Pair(preferredCol, preferredRow) !in occupiedPositions) {
        return Offset(
            preferredCol * cellWidthPx + startPaddingPx,
            preferredRow * cellHeightPx + topPaddingPx
        )
    }

    // Search every valid cell by distance. Unlike the old clamped spiral, this never
    // examines the same edge cell dozens of times, and it cannot accidentally skip a
    // genuinely free cell near an edge after a rotation or resize.
    var best: Pair<Int, Int>? = null
    var bestDistance = Float.POSITIVE_INFINITY
    for (row in 0 until maxRows) {
        for (col in 0 until maxCols) {
            val cell = Pair(col, row)
            if (cell in occupiedPositions) continue
            val dx = (col - preferredCol).toFloat()
            val dy = (row - preferredRow).toFloat()
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = cell
            }
        }
    }
    if (best != null) {
        return Offset(
            best.first * cellWidthPx + startPaddingPx,
            best.second * cellHeightPx + topPaddingPx
        )
    }
    // No free cell exists. Returning null lets the caller keep the icon at its
    // previous position instead of deliberately creating an overlap.
    return null
}

// Convert pixel position → grid cell (col, row)
private fun posToCell(
    pos: Offset,
    cellWidthPx: Float,
    cellHeightPx: Float,
    startPaddingPx: Float,
    topPaddingPx: Float,
    maxCols: Int,
    maxRows: Int
): Pair<Int, Int> {
    val col = ((pos.x - startPaddingPx) / cellWidthPx).roundToInt().coerceIn(0, maxCols - 1)
    val row = ((pos.y - topPaddingPx) / cellHeightPx).roundToInt().coerceIn(0, maxRows - 1)
    return Pair(col, row)
}

// ─────────────────────────────────────────────────────────────────
// animateOffsetAsState — smooth positional animation for snap-back
// ─────────────────────────────────────────────────────────────────
@Composable
private fun animateOffsetAsState(
    targetValue: Offset,
    animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    label: String = "offset"
): State<Offset> {
    val x by animateFloatAsState(
        targetValue   = targetValue.x,
        animationSpec = animationSpec,
        label         = "${label}_x"
    )
    val y by animateFloatAsState(
        targetValue   = targetValue.y,
        animationSpec = animationSpec,
        label         = "${label}_y"
    )
    return derivedStateOf { Offset(x, y) }
}

// ─────────────────────────────────────────────────────────────────
// Default shortcuts created on first launch (only if app is installed)
// Groups: System, Social, Utilities
// ─────────────────────────────────────────────────────────────────
private data class DefaultShortcut(val label: String, val packageName: String? = null, val builtInScreen: LauncherScreen? = null)

// Bluebird-native apps are real desktop shortcuts, not a special protected layer.
// They are created once on first launch, so deleting one is permanent until the user
// explicitly adds it again from Add apps / Start Menu.
private val BLUEBIRD_DEFAULT_APPS = listOf(
    DefaultShortcut("Files", builtInScreen = LauncherScreen.FILE_EXPLORER),
    DefaultShortcut("Settings", builtInScreen = LauncherScreen.SETTINGS),
    DefaultShortcut("Browser", builtInScreen = LauncherScreen.BROWSER),
    DefaultShortcut("Calculator", builtInScreen = LauncherScreen.CALCULATOR),
    DefaultShortcut("Calendar", builtInScreen = LauncherScreen.CALENDAR),
    DefaultShortcut("Photos", builtInScreen = LauncherScreen.PHOTOS),
    DefaultShortcut("Media Player", builtInScreen = LauncherScreen.MEDIA_PLAYER),
    DefaultShortcut("Image Viewer", builtInScreen = LauncherScreen.IMAGE_VIEWER),
    DefaultShortcut("Word Impress", builtInScreen = LauncherScreen.WORD_IMPRESS),
    DefaultShortcut("Text Editor", builtInScreen = LauncherScreen.PremiumTextEditorScreen),
    DefaultShortcut("Terminal", builtInScreen = LauncherScreen.TERMINAL),
    DefaultShortcut("Task Manager", builtInScreen = LauncherScreen.TASK_MANAGER),
    DefaultShortcut("Recycle Bin", builtInScreen = LauncherScreen.RECYCLE_BIN),
    DefaultShortcut("Bluebird Store", builtInScreen = LauncherScreen.BLUEBIRD_STORE),
    DefaultShortcut("Web App Manager", builtInScreen = LauncherScreen.WEB_APP_MANAGER)
)

// Kept as optional Android defaults for devices where these packages actually exist.
private val ANDROID_DEFAULT_SHORTCUTS = listOf(
    DefaultShortcut("Phone", "com.android.dialer"),
    DefaultShortcut("Messages", "com.android.mms"),
    DefaultShortcut("Camera", "com.android.camera2"),
    DefaultShortcut("Chrome", "com.android.chrome"),
    DefaultShortcut("Maps", "com.google.android.apps.maps")
)

/** Creates the initial desktop apps exactly once. Deleting a default shortcut never recreates it. */
private fun createDefaultShortcuts(desktopDir: File, pm: PackageManager, prefs: android.content.SharedPreferences) {
    desktopDir.mkdirs()
    val initializedKey = "desktop_default_apps_initialized_v2"
    if (prefs.getBoolean(initializedKey, false)) return

    (BLUEBIRD_DEFAULT_APPS + ANDROID_DEFAULT_SHORTCUTS).forEach { shortcut ->
        val file = File(desktopDir, "${shortcut.label}.desktop")
        if (file.exists()) return@forEach
        if (shortcut.packageName != null) {
            try { pm.getApplicationInfo(shortcut.packageName, 0) }
            catch (_: PackageManager.NameNotFoundException) { return@forEach }
        }
        val content = buildString {
            append("type=app\n")
            append("label=${shortcut.label}\n")
            shortcut.packageName?.let { append("package=$it\n") }
            shortcut.builtInScreen?.let { append("bluebirdScreen=${it.name}\n") }
        }
        runCatching { file.writeText(content) }
    }
    prefs.edit().putBoolean(initializedKey, true).apply()
}

// ─────────────────────────────────────────────────────────────────
// Wallpaper crossfade helper
// ─────────────────────────────────────────────────────────────────
private const val WALLPAPER_CYCLE_MS = 5 * 60 * 1000L   // 5 minutes

// ─────────────────────────────────────────────────────────────────
// Main Desktop Component
// ─────────────────────────────────────────────────────────────────
@Composable
fun Desktop(
    wallpaper: WallpaperState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
    // Reserved space at the bottom of the desktop's own drag/layout area — e.g. real
    // taskbar height, if the caller lays Desktop() out full-screen with the taskbar
    // drawn as a separate overlay on top rather than reducing Desktop()'s own measured
    // height. Defaults to 0 (no change from previous behavior) if the caller doesn't
    // pass anything. See the maxYBound comment below for why this matters.
    bottomSafeAreaPx: Float = 0f
) {
    // FIX: Use BoxWithConstraints so screenWPxTotal/screenHPxTotal are the
    // *actual* layout pixel dimensions.  LocalConfiguration.screenWidthDp is
    // already in dp units, so doing `.dp.toPx()` on it was a double-conversion
    // that produced a value far too small whenever a high smallest-width is
    // forced in the activity (desktop mode).  constraints.maxWidth/maxHeight
    // are always correct regardless of forced density.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWPxTotal = constraints.maxWidth.toFloat()
        val screenHPxTotal = constraints.maxHeight.toFloat()

        val context  = LocalContext.current
        val scope    = rememberCoroutineScope()
        val density  = LocalDensity.current
        val config   = LocalConfiguration.current
        val screenW  = config.screenWidthDp
        val screenH  = config.screenHeightDp

        val desktopDir = remember { File(Environment.getExternalStorageDirectory(), "Desktop") }

        // ── Persistence ───────────────────────────────────────────────
        val prefs = remember { DesktopPreferences(context) }

        // Live desktop contents now come from the ViewModel's single shared file-state
        // layer (uiState.desktopFiles) — the same list File Explorer and Recycle Bin see.
        // Desktop no longer keeps its own separate FileObserver/scan; it just renders
        // whatever the ViewModel currently has.
        val vmUiState by viewModel.uiState.collectAsStateWithLifecycle()
        val items = vmUiState.desktopFiles

        // ── Core state (FIX: initialised from prefs, not just remember{}) ──
        var selectedIds         by remember { mutableStateOf(setOf<String>()) }
        var selectionAnchorId   by remember { mutableStateOf<String?>(null) }
        var ctrlMouseSelection  by remember { mutableStateOf(false) }
        var shiftMouseSelection by remember { mutableStateOf(false) }
        var iconSize            by remember { mutableStateOf(prefs.iconSize) }

        // Self-contained store for two new settings — kept separate from the existing
        // DesktopPreferences class (not among the files I have access to, so I can't
        // safely change ITS baked-in defaults) rather than risk guessing wrong there.
        //
        // 1. "Has the user ever explicitly chosen a sort mode?" — if not, default to
        //    NONE regardless of whatever DesktopPreferences.sortMode's own raw default
        //    is, matching real Windows: sort isn't "on" until you turn it on, and files
        //    you create/paste stay exactly where you put them until you do.
        // 2. "Align icons to grid" — this toggle previously existed in the UI but did
        //    nothing at all (its onClick was an empty {} lambda). Now wired for real:
        //    ON (the default, matching current/prior always-snap behavior) means drops
        //    snap to the nearest free grid cell; OFF allows free pixel placement.
        val layoutPrefsStore = remember { context.getSharedPreferences("desktop_layout_prefs_v2", Context.MODE_PRIVATE) }
        var alignToGrid by remember { mutableStateOf(layoutPrefsStore.getBoolean("align_to_grid", true)) }
        fun setAlignToGrid(v: Boolean) {
            alignToGrid = v
            layoutPrefsStore.edit().putBoolean("align_to_grid", v).apply()
        }
        var sortMode by remember {
            mutableStateOf(
                if (layoutPrefsStore.getBoolean("user_set_sort", false)) prefs.sortMode else DesktopSortMode.NONE
            )
        }
        var sortAscending       by remember { mutableStateOf(prefs.sortAscending) }
        var autoArrange         by remember { mutableStateOf(prefs.autoArrange) }
        var showIconsOnDesktop  by remember { mutableStateOf(prefs.showIconsOnDesktop) }

        // FIX: customPositions loaded from prefs on first composition
        val customPositions = remember {
            mutableStateMapOf<String, Offset>().also { map ->
                map.putAll(prefs.loadCustomPositions())
            }
        }
        var draggedId           by remember { mutableStateOf<String?>(null) }

        // ── Multi-select drag ──────────────────────────────────────────
        // When dragging starts on a selected icon, all selected icons move together.
        // dragGroupOrigins stores each icon's pixel offset from the drag anchor.
        var isDraggingGroup     by remember { mutableStateOf(false) }
        // Broadcasts the CURRENT absolute target position for each follower (read by each
        // follower's own LaunchedEffect below). Kept separate from groupRelativeOffsets —
        // previously this single map was used for BOTH the original relative offset AND the
        // live absolute broadcast, and once onDrag started overwriting it with absolute
        // positions, the NEXT onDrag call re-read those absolute values as if they were still
        // relative deltas and added them to the anchor again — a compounding feedback loop
        // that could send followers flying off-screen within a few frames of dragging.
        val dragGroupOffsets    = remember { mutableStateMapOf<String, Offset>() }
        // Stable, immutable-during-drag relative offsets (follower - anchor), captured once
        // at drag start and never mutated afterward — the fix for the bug above.
        val groupRelativeOffsets = remember { mutableStateMapOf<String, Offset>() }

        // ── Desktop-full prompt ────────────────────────────────────────
        var showDesktopFullDialog by remember { mutableStateOf(false) }
        var lastDesktopFullPromptKey by remember { mutableStateOf<Any?>(null) }

        // ── File-access toast (shown when storage permission denied) ───
        var showFileAccessToast   by remember { mutableStateOf(false) }

        // ── Wallpaper state (FIX: persisted mode + indices + cycle) ──
        // FIX: On a fresh install prefs.wallpaperMode returns APPARENT (the old
        // enum default).  We want DEFAULT (the bundled wallpaper images) to show
        // first instead, so we remap APPARENT → DEFAULT only on the very first
        // launch (i.e. when no mode has ever been explicitly saved by the user).
        var wallpaperMode         by remember {
            mutableStateOf(
                if (prefs.wallpaperMode == DesktopWallpaperMode.APPARENT && !prefs.wallpaperModeEverSet)
                    DesktopWallpaperMode.DEFAULT
                else
                    prefs.wallpaperMode
            )
        }
        var gradientIndex         by remember { mutableStateOf(prefs.wallpaperGradientIndex) }
        var defaultImageIndex     by remember { mutableStateOf(prefs.wallpaperImageIndex) }
        var customWallpaperUri    by remember { mutableStateOf(prefs.customWallpaperUri) }

        // Override from WallpaperState if a custom URI was just set externally
        LaunchedEffect(wallpaper.homeWallpaperUri) {
            if (wallpaper.homeWallpaperUri.isNotEmpty() && wallpaper.homeWallpaperUri != customWallpaperUri) {
                customWallpaperUri = wallpaper.homeWallpaperUri
                wallpaperMode = DesktopWallpaperMode.CUSTOM
                prefs.customWallpaperUri = wallpaper.homeWallpaperUri
                prefs.wallpaperMode = DesktopWallpaperMode.CUSTOM
            }
        }

        // Auto-cycle wallpaper when no custom wallpaper is set
        LaunchedEffect(wallpaperMode) {
            while (true) {
                delay(WALLPAPER_CYCLE_MS)
                when (wallpaperMode) {
                    DesktopWallpaperMode.APPARENT -> {
                        gradientIndex = (gradientIndex + 1) % wallpaperGradients.size
                        prefs.wallpaperGradientIndex = gradientIndex
                    }
                    DesktopWallpaperMode.DEFAULT -> {
                        defaultImageIndex = (defaultImageIndex + 1) % DEFAULT_WALLPAPERS.size
                        prefs.wallpaperImageIndex = defaultImageIndex
                    }
                    DesktopWallpaperMode.CUSTOM -> return@LaunchedEffect  // BUG 9 FIX: break exits `when`, not `while`; use return instead
                }
            }
        }

        // ── Clipboard ──
        // Clipboard now lives in the ViewModel — shared with File Explorer, so a cut in
        // one and a paste in the other just works (previously each screen had its own,
        // incompatible clipboard: Desktop held a list, File Explorer held a single file).

        // ── Context menus ──
        var showDesktopCtx      by remember { mutableStateOf(false) }
        var desktopCtxOffset    by remember { mutableStateOf(Offset.Zero) }
        var desktopCtxLocalOffset by remember { mutableStateOf(Offset.Zero) }
        // Real screen coordinates of the desktop canvas layer — captured once via
        // onGloballyPositioned below, used to convert local tap positions (measured
        // relative to that layer) into true absolute screen coordinates for the context
        // menus' Popup-based positioning, via the same trueScreenPosition() helper the
        // flyout submenus use (see its doc comment for why plain localToWindow()/
        // positionInWindow() aren't reliable once nested inside a Popup).
        val localView = LocalView.current
        var desktopLayerCoords  by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var iconCtxTarget       by remember { mutableStateOf<DesktopFileInfo?>(null) }
        var iconCtxOffset       by remember { mutableStateOf(Offset.Zero) }

        // ── FIX: InlineRenameState.initialName is now truly immutable ──
        // The live text lives entirely inside DesktopIcon's local state.
        // The parent only holds the rename trigger (targetId + initialName).
        var inlineRename        by remember { mutableStateOf<InlineRenameState?>(null) }
        var pendingRenameId     by remember { mutableStateOf<String?>(null) }

        // ── Dialogs ──
        var showPropsDialog     by remember { mutableStateOf(false) }
        var propsTarget         by remember { mutableStateOf<DesktopFileInfo?>(null) }
        var showShortcutDialog  by remember { mutableStateOf(false) }
        var showAppPickerDialog by remember { mutableStateOf(false) }
        // showWallpaperPanel removed — Personalize now opens the Settings window
        // (Appearance category) instead of this in-place dialog. See onPersonalize
        // above and WallpaperPersonalisePanel's replacement, PersonalizationSection,
        // in SettingsScreen.kt.

        // ── Lasso selection ──
        var selStart            by remember { mutableStateOf(Offset.Zero) }
        var selEnd              by remember { mutableStateOf(Offset.Zero) }
        var isSelecting         by remember { mutableStateOf(false) }
        var lassoActive         by remember { mutableStateOf(false) }

        // PERF FIX: this used to be a second, separate collectAsState() of the
        // *entire* uiState just to read isDarkTheme — a duplicate Flow
        // subscription doing the same job as vmUiState above (which is already
        // collected lifecycle-aware). That meant two live collectors of the same
        // StateFlow driving recomposition off of every single state change,
        // instead of one. Reuse vmUiState instead.
        val isDark = vmUiState.isDarkTheme

        // ── Grid metrics ──
        val cellWDp     = cellWidthDp(iconSize)
        val cellHDp     = cellHeightDp(iconSize)
        val gridPadLeft = 10f
        val gridPadTop  = 10f

        // FIX (landscape): Use px totals from BoxWithConstraints as the remember keys so
        // rows/maxCols recompute on every orientation change, not just when the dp
        // config values happen to change.
        val cellWPx   = with(density) { cellWDp.dp.toPx() }
        val cellHPx   = with(density) { cellHDp.dp.toPx() }
        val padLeftPx = with(density) { gridPadLeft.dp.toPx() }
        val padTopPx  = with(density) { gridPadTop.dp.toPx() }

        val viewportRows = ((screenHPxTotal - padTopPx * 2 - bottomSafeAreaPx) / cellHPx).toInt().coerceAtLeast(1)
        val maxCols = ((screenWPxTotal - padLeftPx * 2) / cellWPx).toInt().coerceAtLeast(1)
        val workspaceRows = viewportRows
        val workspaceCols = maxCols
        val workspaceWidthPx = screenWPxTotal
        val workspaceHeightPx = (screenHPxTotal - bottomSafeAreaPx).coerceAtLeast(cellHPx + padTopPx * 2)
        val maxXBound = (workspaceWidthPx - cellWPx - padLeftPx).coerceAtLeast(padLeftPx)
        val maxYBound = (workspaceHeightPx - cellHPx - padTopPx).coerceAtLeast(padTopPx)
        val desktopCapacity = (workspaceRows * workspaceCols).coerceAtLeast(1)

        // Keep manual grid spacing identical to Auto Arrange. Auto Arrange derives every
        // position from the current cell metrics; manual mode stores absolute offsets, so
        // coordinates saved by an older layout can retain wider spacing. Re-project visible
        // manual-grid positions onto the same current cells without changing the chosen cell.
        fun normalizeManualGridPositions() {
            if (autoArrange || !alignToGrid || customPositions.isEmpty()) return

            val occupied = mutableSetOf<Pair<Int, Int>>()
            val normalized = mutableMapOf<String, Offset>()

            customPositions.entries.forEach { (id, stored) ->
                // Positions outside the fixed viewport are overflow records. Leave them in
                // storage so a later larger viewport can restore them; File Explorer remains
                // the way to access them while they do not fit on this screen.
                if (stored.x < padLeftPx || stored.y < padTopPx ||
                    stored.x > maxXBound || stored.y > maxYBound
                ) {
                    normalized[id] = stored
                    return@forEach
                }

                val preferred = posToCell(
                    stored, cellWPx, cellHPx, padLeftPx, padTopPx,
                    workspaceCols, workspaceRows
                )

                val chosen = if (preferred !in occupied) {
                    preferred
                } else {
                    // Resolve an old collision using the same nearest-cell rule as snapping.
                    var best: Pair<Int, Int>? = null
                    var bestDistance = Float.POSITIVE_INFINITY
                    for (row in 0 until workspaceRows) {
                        for (col in 0 until workspaceCols) {
                            val cell = col to row
                            if (cell in occupied) continue
                            val dx = (col - preferred.first).toFloat()
                            val dy = (row - preferred.second).toFloat()
                            val distance = dx * dx + dy * dy
                            if (distance < bestDistance) {
                                bestDistance = distance
                                best = cell
                            }
                        }
                    }
                    best
                }

                if (chosen != null) {
                    occupied += chosen
                    normalized[id] = Offset(
                        padLeftPx + chosen.first * cellWPx,
                        padTopPx + chosen.second * cellHPx
                    )
                }
            }

            val current = customPositions.toMap()
            if (normalized != current) {
                customPositions.clear()
                customPositions.putAll(normalized)
                prefs.saveCustomPositions(customPositions)
            }
        }

        // Deliberately no resize/rotation repair effect. Positions are user data.
        // The viewport is fixed; overflow is surfaced through the Desktop-full prompt.

        // ── Debounced refresh (only used for explicit user-triggered mutations —
        //    e.g. right after a rename/delete/paste — so the UI feels instant instead
        //    of waiting on the ViewModel's own ~120ms FileObserver debounce) ──
        var refreshPending by remember { mutableStateOf(false) }

        fun scheduleRefresh() {
            if (refreshPending) return
            refreshPending = true
            scope.launch {
                delay(60)
                refreshPending = false
                viewModel.refreshDesktopFiles()
            }
        }

        // ── First-launch default shortcuts + permission check, then hand off to the
        //    ViewModel's shared file-state layer for everything after ──
        LaunchedEffect(desktopDir.absolutePath) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                    android.os.Environment.isExternalStorageManager()

            if (!hasPermission) {
                showFileAccessToast = true
                return@LaunchedEffect
            }
            if (!prefs.defaultShortcutsCreated) {
                withContext(Dispatchers.IO) { createDefaultShortcuts(desktopDir, context.packageManager, context.getSharedPreferences("launcher_prefs_v3", Context.MODE_PRIVATE)) }
                prefs.defaultShortcutsCreated = true
            }
            viewModel.refreshDesktopFiles()
        }

        // Keep transient desktop state aligned with the latest filesystem snapshot.
        // Only persist when something actually changed: the old unconditional save ran
        // on every refresh, including silent FileObserver refreshes, causing unnecessary
        // disk I/O and making rapid create/delete/rename sequences more expensive.
        LaunchedEffect(items) {
            val liveIds = items.asSequence().map { it.id }.toSet()
            var positionsChanged = false
            customPositions.keys.toList().forEach { id ->
                if (id !in liveIds) {
                    customPositions.remove(id)
                    positionsChanged = true
                }
            }
            if (positionsChanged) prefs.saveCustomPositions(customPositions)

            // Selection must never retain ids for files that no longer exist. Apart from
            // stale highlighting, those ids can leak into group-drag calculations.
            val validSelection = selectedIds.intersect(liveIds)
            if (validSelection != selectedIds) selectedIds = validSelection

            val pendingId = pendingRenameId
            if (pendingId != null) {
                val newItem = items.find { it.id == pendingId }
                if (newItem != null) {
                    inlineRename = InlineRenameState(newItem.id, newItem.name)
                    selectedIds = setOf(newItem.id)
                    pendingRenameId = null
                }
            }
        }

        // ── Windows-style refresh effect — icons vanish then reappear across the whole
        //    desktop whenever the shared desktopRefreshTick advances (i.e. a real change
        //    was detected and re-scanned), skipping the very first load so opening the
        //    desktop doesn't flicker. Window is long enough to cover the slowest per-icon
        //    stagger + fade-out + gap + fade-in in DesktopIcon's own animation.
        //
        //    Driven off vmUiState (already collected via collectAsStateWithLifecycle, so
        //    it's real Compose snapshot state) rather than snapshotFlow-over-StateFlow.value
        //    — snapshotFlow only re-fires on snapshot-state reads, and a raw StateFlow.value
        //    read doesn't count, so that version only ever fired once for the whole screen's
        //    lifetime instead of on every refresh. ──
        // ── Windows-style refresh effect — icons vanish then reappear together, but ONLY
        //    for an explicit "Refresh" from the desktop context menu (manualDesktopRefreshTick),
        //    not for silent rescans the FileObserver triggers after a paste/delete/rename.
        //
        //    Driven by ONE shared Animatable owned here, instead of each icon running its
        //    own independent LaunchedEffect + Animatable with a random stagger. The old
        //    per-icon approach caused two real bugs: (1) the random stagger meant icons
        //    visibly disappeared/reappeared at different times instead of together, and
        //    (2) if a slower device's frame timing pushed any single icon's animation past
        //    the parent's fixed window, that icon's LaunchedEffect got cancelled mid-fade
        //    and its alpha froze at whatever value it was interrupted at — sometimes 0,
        //    leaving an icon invisible but still clickable at its real position. With one
        //    shared value, every icon reads the exact same alpha every frame, so they're
        //    perfectly in sync and there's no per-icon coroutine that can get stuck. ──
        val desktopFlickerAlpha = remember { Animatable(1f) }
        var lastManualRefreshTick by remember { mutableStateOf(-1) }
        LaunchedEffect(vmUiState.manualDesktopRefreshTick) {
            val tick = vmUiState.manualDesktopRefreshTick
            if (lastManualRefreshTick != -1 && tick != lastManualRefreshTick) {
                try {
                    desktopFlickerAlpha.animateTo(0f, tween(90))
                    desktopFlickerAlpha.animateTo(1f, tween(180))
                } finally {
                    // Belt-and-braces: even if this coroutine gets cancelled mid-fade (e.g. a
                    // second rapid refresh), never leave icons stuck below full opacity.
                    // NonCancellable because a suspend call in a finally block after
                    // cancellation would otherwise throw immediately.
                    withContext(NonCancellable) {
                        if (desktopFlickerAlpha.value < 1f) desktopFlickerAlpha.snapTo(1f)
                    }
                }
            }
            lastManualRefreshTick = tick
        }

        val sortedItems = remember(items, sortMode, sortAscending) {
            if (sortMode == DesktopSortMode.NONE) {
                // No active sort — items keep whatever order the file system/ViewModel
                // gave them in. Ascending/descending is meaningless here, and since every
                // icon now gets a stable, persisted position (see the earlier fix), this
                // order no longer even affects where anything visually appears — it only
                // matters as a rendering/tab-order detail.
                items
            } else {
                val s = when (sortMode) {
                    DesktopSortMode.NAME          -> items.sortedWith(compareBy<DesktopFileInfo> { it.name.lowercase(java.util.Locale.ROOT) }.thenBy { it.id })
                    DesktopSortMode.DATE_MODIFIED -> items.sortedBy { it.file.lastModified() }
                    DesktopSortMode.TYPE          -> items.sortedWith(compareBy<DesktopFileInfo> { it.file.extension.lowercase(java.util.Locale.ROOT) }.thenBy { it.name.lowercase(java.util.Locale.ROOT) }.thenBy { it.id })
                    DesktopSortMode.SIZE          -> items.sortedBy { it.file.length() }
                    DesktopSortMode.NONE          -> items  // unreachable, handled above
                }
                if (sortAscending) s else s.reversed()
            }
        }

        val indexMap = remember(sortedItems) {
            sortedItems.mapIndexed { idx, item -> item.id to idx }.toMap()
        }

        fun openItem(item: DesktopFileInfo) {
            if (item.type == DesktopItemType.THIS_PC || item.type == DesktopItemType.RECYCLE_BIN ||
                item.type == DesktopItemType.SETTINGS_ICON) {
                // System icons keep their existing handling elsewhere; nothing to do here.
                return
            }
            openDesktopItem(item, context, viewModel)
        }

        // ─────────────────────────────────────────────────────────────
        // FIX: commitRename — reads the live text from the rename state.
        // inlineRename.initialName is only the seed; the DesktopIcon
        // calls this with the final user-typed value directly.
        // ─────────────────────────────────────────────────────────────
        fun commitRename(rename: InlineRenameState, newRawName: String) {
            inlineRename = null
            val target = items.find { it.id == rename.targetId } ?: return
            val base = newRawName.trim()
            if (base.isBlank()) return

            if (target.type == DesktopItemType.APP_SHORTCUT) {
                try {
                    val lines = target.file.readLines().toMutableList()
                    val labelIdx = lines.indexOfFirst { it.startsWith("label=") }
                    if (labelIdx >= 0) lines[labelIdx] = "label=$base"
                    else lines.add("label=$base")
                    target.file.writeText(lines.joinToString("\n") + "\n")
                    scheduleRefresh()
                } catch (_: Exception) {}
                return
            }

            // WEB_APP_SHORTCUT: rewrite only the display name= field — the icon and id
            // stay put, so a rename never orphans the cached favicon.
            if (target.type == DesktopItemType.WEB_APP_SHORTCUT) {
                try {
                    val lines = target.file.readLines().toMutableList()
                    val nameIdx = lines.indexOfFirst { it.startsWith("name=") }
                    if (nameIdx >= 0) lines[nameIdx] = "name=$base"
                    else lines.add("name=$base")
                    target.file.writeText(lines.joinToString("\n") + "\n")
                    scheduleRefresh()
                } catch (_: Exception) {}
                return
            }

            val ext = if (target.file.name.contains("."))
                ".${target.file.name.substringAfterLast(".")}" else ""
            val finalName = if (ext.isNotEmpty() && !base.endsWith(ext, ignoreCase = true))
                "$base$ext" else base

            if (finalName == target.file.name) return
            val dest = File(target.file.parent ?: return, finalName)
            if (dest.exists()) return

            // A regular file's desktop id is its absolute path, so a successful rename
            // necessarily changes its id. Migrate the persisted position/selection to the
            // new id before the FileObserver refresh replaces `items`; otherwise the icon
            // briefly (and often permanently) loses its carefully arranged position.
            val oldId = target.id
            val oldPosition = customPositions[oldId]
            if (!target.file.renameTo(dest)) return
            if (oldPosition != null) {
                customPositions.remove(oldId)
                customPositions[dest.absolutePath] = oldPosition
                prefs.saveCustomPositions(customPositions)
            }
            if (oldId in selectedIds) {
                selectedIds = selectedIds - oldId + dest.absolutePath
            }
            if (inlineRename?.targetId == oldId) {
                inlineRename = InlineRenameState(dest.absolutePath, base)
            }
            scheduleRefresh()
        }

        // ─────────────────────────────────────────────────────────────
        // View hierarchy
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = modifier.fillMaxSize()
                .focusable()
                .onKeyEvent { event ->
                    ctrlMouseSelection = event.nativeKeyEvent.isCtrlPressed
                    shiftMouseSelection = event.nativeKeyEvent.isShiftPressed
                    false
                }
        ) {

            // ── Wallpaper (FIX: mode-aware with crossfade transition) ──
            // Live wallpaper (when active) fully replaces the static wallpaper layer —
            // it's mutually exclusive with both the static picker and particle animations.
            if (vmUiState.backgroundEffects.liveWallpaper != LiveWallpaperType.NONE) {
                LiveWallpaperRenderer(
                    type     = vmUiState.backgroundEffects.liveWallpaper,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
            Crossfade(
                targetState = Triple(wallpaperMode, gradientIndex, defaultImageIndex),
                animationSpec = tween(800),
                label = "wallpaper_crossfade"
            ) { (mode, gIdx, dIdx) ->
                when (mode) {
                    DesktopWallpaperMode.CUSTOM -> {
                        if (customWallpaperUri.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(customWallpaperUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback to APPARENT if URI is empty
                            val gradient = wallpaperGradients[gIdx % wallpaperGradients.size]
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.linearGradient(gradient, start = Offset(0f, 0f), end = Offset(2500f, 1500f))
                                )
                            )
                        }
                    }
                    DesktopWallpaperMode.DEFAULT -> {
                        val resId = DEFAULT_WALLPAPERS.getOrNull(dIdx % DEFAULT_WALLPAPERS.size) ?: 0
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Placeholder until real drawables are added
                            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
                        }
                    }
                    DesktopWallpaperMode.APPARENT -> {
                        val gradient = wallpaperGradients[gIdx % wallpaperGradients.size]
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(gradient, start = Offset(0f, 0f), end = Offset(2500f, 1500f))
                            )
                        )
                    }
                }
            }
            }

            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.02f)))

            // ── Background particle animation — forced off while a live wallpaper is active ──
            if (vmUiState.backgroundEffects.liveWallpaper == LiveWallpaperType.NONE &&
                vmUiState.backgroundEffects.activeAnimations.isNotEmpty()) {
                BackgroundAnimationLayer(
                    activeTypes = vmUiState.backgroundEffects.activeAnimations,
                    intensity   = vmUiState.backgroundEffects.intensity,
                    modifier    = Modifier.fillMaxSize()
                )
            }

            // ── Fixed desktop viewport ───────────────────────────────────
            // Never scroll the desktop. It remains the same size as the screen/taskbar area.
            Box(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onGloballyPositioned { desktopLayerCoords = it }
                    .pointerInput(Unit) {
                        detectPressDragGestures(
                            longPressOnTimeout = true,
                            onTap = {
                                val currentRename = inlineRename
                                if (currentRename != null) {
                                    // The live text is held in DesktopIcon; tapping outside
                                    // with no typed text means keep the initial name
                                    commitRename(currentRename, currentRename.initialName)
                                } else {
                                    selectedIds = emptySet()
                                    selectionAnchorId = null
                                }
                                showDesktopCtx = false
                                iconCtxTarget  = null
                            },
                            onSecondaryTap = { off ->
                                if (draggedId == null) {
                                    desktopCtxOffset = desktopLayerCoords?.let { it.trueScreenPosition(localView) + off } ?: off
                                    desktopCtxLocalOffset = off
                                    showDesktopCtx = true
                                    iconCtxTarget = null
                                }
                            },
                            onLongPressReleased = { off ->
                                if (draggedId == null) {
                                     iconCtxTarget = null
                                    desktopCtxOffset      = desktopLayerCoords?.let { it.trueScreenPosition(localView) + off } ?: off
                                    // Local (grid-space) copy of the same click, kept separately from
                                    // desktopCtxOffset above (which is screen-space, for Popup
                                    // positioning only) — this is what "New > Folder/Text Document"
                                    // uses to place the new item where the menu was actually opened,
                                    // instead of it having no assigned position at all.
                                    desktopCtxLocalOffset = off
                                    showDesktopCtx   = true
                                    iconCtxTarget    = null
                                }
                            },
                            onDragStart = { off ->
                                if (draggedId == null) {
                                    lassoActive = true
                                    isSelecting = true
                                    selStart    = off
                                    selEnd      = off
                                }
                            },
                            onDrag = { _, amt ->
                                if (lassoActive) selEnd += amt
                            },
                            onDragEnd = {
                                if (lassoActive) {
                                    isSelecting = false
                                    lassoActive = false
                                    val rect = Rect(
                                        minOf(selStart.x, selEnd.x), minOf(selStart.y, selEnd.y),
                                        maxOf(selStart.x, selEnd.x), maxOf(selStart.y, selEnd.y)
                                    )
                                    selectedIds = sortedItems.filter { item ->
                                        val idx = indexMap[item.id] ?: return@filter false
                                        val pos = if (autoArrange) {
                                            autoGridPos(idx, workspaceRows, workspaceCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                                                ?: return@filter false
                                        } else {
                                            customPositions[item.id] ?: return@filter false
                                        }
                                        Rect(pos.x, pos.y, pos.x + cellWPx, pos.y + cellHPx).overlaps(rect)
                                    }.map { it.id }.toSet()
                                }
                            },
                            onDragCancel = { isSelecting = false; lassoActive = false }
                        )
                    }
            )

            // FIX: pre-compute the set of occupied grid cells for overlap detection.
            //
            // Previously this used remember(customPositions, ...) — but customPositions
            // is a SnapshotStateMap that gets mutated IN PLACE (same object reference
            // every time), so remember's key-equality check was comparing the map to
            // itself and never saw a change, even after drags wrote new positions into
            // it. That left this occupied-set stale, so every later snap decision (both
            // the dragged icon's own and any group followers') was checking for overlaps
            // against out-of-date positions — which is what let dropped icons land on
            // top of each other and never really line up.
            //
            // derivedStateOf fixes this correctly: it tracks the actual per-entry
            // snapshot reads of customPositions[item.id] made inside the block, so it
            // recomputes whenever any of those entries actually change, not just when
            // some other key happens to change at the same time.
            //
            // Hoisted to this outer scope (not nested inside the icons-layer Box below)
            // since it's also needed by placeNewItemAtClickPosition and the stable-
            // position effect further down, both of which run regardless of whether
            // icons are currently shown.
            val occupiedCells by remember(autoArrange, workspaceRows, workspaceCols, workspaceWidthPx, workspaceHeightPx) {
                derivedStateOf {
                    buildSet {
                        sortedItems.forEachIndexed { idx, item ->
                            val p = if (autoArrange) {
                                autoGridPos(idx, workspaceRows, workspaceCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                            } else {
                                customPositions[item.id]?.takeIf {
                                    it.x >= padLeftPx && it.y >= padTopPx &&
                                        it.x <= maxXBound && it.y <= maxYBound
                                }
                            }
                            if (p != null) add(posToCell(p, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows))
                        }
                    }
                }
            }

            // ── Icons layer ──
            if (showIconsOnDesktop) {
                Box(Modifier.fillMaxSize()) {

                    // FIX: pre-compute the set of occupied grid cells for overlap detection.
                    //
                    // Previously this used remember(customPositions, ...) — but customPositions
                    // is a SnapshotStateMap that gets mutated IN PLACE (same object reference
                    // every time), so remember's key-equality check was comparing the map to
                    // itself and never saw a change, even after drags wrote new positions into
                    // it. That left this occupied-set stale, so every later snap decision (both
                    // the dragged icon's own and any group followers') was checking for overlaps
                    // against out-of-date positions — which is what let dropped icons land on
                    // top of each other and never really line up.
                    //
                    // derivedStateOf fixes this correctly: it tracks the actual per-entry
                    // snapshot reads of customPositions[item.id] made inside the block, so it
                    // recomputes whenever any of those entries actually change, not just when
                    // some other key happens to change at the same time.
                    // ── Performance: O(n) cached auto-arrange positions ──
                    // Build positions once per recomposition key instead of
                    // recomputing from scratch for every icon (was O(n²)).
                    val autoArrangePositions = remember(sortedItems, autoArrange, workspaceRows, workspaceCols, iconSize, workspaceWidthPx, workspaceHeightPx) {
                        if (!autoArrange) return@remember emptyMap<String, Offset>()
                        val taken = mutableSetOf<Pair<Int, Int>>()
                        buildMap {
                            sortedItems.forEachIndexed { i, item ->
                                val p = autoGridPos(i, workspaceRows, workspaceCols, cellWPx, cellHPx,
                                    padLeftPx, padTopPx, taken)
                                if (p != null) {
                                    put(item.id, p)
                                    taken.add(posToCell(p, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows))
                                }
                            }
                        }
                    }

                    sortedItems.forEachIndexed { idx, item ->
                        androidx.compose.runtime.key(item.id) {
                        // O(1) lookup from cached map
                        val storedPos = customPositions[item.id]
                        val basePos: Offset? = if (autoArrange) {
                            autoArrangePositions[item.id]
                        } else {
                            storedPos?.takeIf {
                                it.x >= padLeftPx && it.y >= padTopPx &&
                                    it.x <= maxXBound && it.y <= maxYBound
                            }
                        }
                        if (basePos != null) {
                        val resolvedBasePos = basePos

                        var pos by remember(item.id, workspaceRows, workspaceCols, iconSize, autoArrange, workspaceWidthPx, workspaceHeightPx) {
                            mutableStateOf(resolvedBasePos)
                        }

                        LaunchedEffect(
                            autoArrange,
                            idx,
                            workspaceRows,
                            workspaceCols,
                            iconSize,
                            workspaceWidthPx,
                            workspaceHeightPx,
                            isDraggingGroup,
                            customPositions[item.id]
                        ) {
                            if (autoArrange) {
                                pos = resolvedBasePos
                            } else if (draggedId != item.id && !isDraggingGroup) {
                                pos = customPositions[item.id] ?: resolvedBasePos
                            }
                        }

                        val isDragged    = draggedId == item.id
                        val isInGroup    = isDraggingGroup && item.id in selectedIds && !isDragged

                        // Follower position-apply — keyed purely on the broadcast value changing
                        // (not on isInGroup), so the anchor's FINAL settle position at drag-end
                        // still gets applied even though isInGroup flips false in that same
                        // callback (previously the isInGroup guard meant that last update was
                        // silently dropped, leaving followers un-snapped and unsaved).
                        LaunchedEffect(dragGroupOffsets[item.id]) {
                            val target = dragGroupOffsets[item.id]
                            if (target != null) pos = target
                        }

                        // Snap-back animation: animates position smoothly on grid rejection.
                        // While actively being dragged (as the anchor OR as a following group
                        // member), position tracks the raw target with snap() — instant, no
                        // spring — so it moves 1:1 with the finger instead of visibly lagging
                        // behind it. The spring only kicks back in once the drag ends, which is
                        // exactly when a smooth settle-into-place animation looks good instead
                        // of feeling like drag lag.
                        val animatedPos  by animateOffsetAsState(
                            targetValue   = pos,
                            animationSpec = if (isDragged || isInGroup) snap() else spring(stiffness = Spring.StiffnessMediumLow),
                            label         = "icon_pos_${item.id}"
                        )

                        val dragScale by animateFloatAsState(
                            targetValue   = if (isDragged || isInGroup) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label         = "icon_drag_scale"
                        )

                        var dragMoved by remember { mutableStateOf(false) }
                        // Lifted above the gesture block so onTap (which may need to commit an
                        // in-progress rename on a DIFFERENT icon using its live-typed text) can see it.
                        var liveRenameText by remember { mutableStateOf("") }

                        Box(
                            Modifier
                                .offset { IntOffset(animatedPos.x.roundToInt(), animatedPos.y.roundToInt()) }
                                .scale(dragScale)
                                .zIndex(if (isDragged) 50f else if (isInGroup) 40f else 1f)
                                .pointerInput(item.id) {
                                    detectPressDragGestures(
                                        consumeDown = true,
                                        // Commit touch long-press at the platform threshold.
                                        // Waiting for the exact UP event was unreliable on some Android touch paths.
                                        longPressOnTimeout = true,
                                        onSecondaryTap = { localPoint ->
                                            val r = inlineRename
                                            if (r != null && r.targetId != item.id) commitRename(r, r.initialName)
                                            if (item.id !in selectedIds) selectedIds = setOf(item.id)
                                            iconCtxTarget = item
                                            iconCtxOffset = desktopLayerCoords?.let { it.trueScreenPosition(localView) + localPoint } ?: localPoint
                                            showDesktopCtx = false
                                        },
                                        onTap = {
                                            val r = inlineRename
                                            if (r != null && r.targetId != item.id) commitRename(r, r.initialName)
                                            val currentIndex = indexMap[item.id] ?: 0
                                            when {
                                                shiftMouseSelection -> {
                                                    val anchorIndex = selectionAnchorId?.let { indexMap[it] } ?: currentIndex
                                                    val range = if (anchorIndex <= currentIndex) anchorIndex..currentIndex else currentIndex..anchorIndex
                                                    selectedIds = range.mapNotNull { sortedItems.getOrNull(it)?.id }.toSet()
                                                }
                                                ctrlMouseSelection -> {
                                                    selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                                    selectionAnchorId = item.id
                                                }
                                                else -> {
                                                    selectedIds = setOf(item.id)
                                                    selectionAnchorId = item.id
                                                }
                                            }
                                            showDesktopCtx = false
                                            iconCtxTarget  = null
                                        },
                                        onDoubleTap = { openItem(item) },
                                        onLongPressReleased = {
                                            val r = inlineRename
                                            if (r != null) commitRename(r, r.initialName)
                                            // Right-clicking (long-pressing) an icon that's already part of
                                            // the current multi-selection keeps that selection intact — so
                                            // the context menu's Cut/Copy/Delete act on all of them, matching
                                            // real Explorer. Only replace the selection if this icon wasn't
                                            // already selected (previously this always collapsed to just the
                                            // one icon, silently discarding any multi-selection).
                                            if (item.id !in selectedIds) {
                                                selectedIds = setOf(item.id)
                                            }
                                            iconCtxTarget  = item
                                            val localPoint = Offset(pos.x + cellWPx / 2, pos.y + cellHPx / 2)
                                            iconCtxOffset  = desktopLayerCoords?.let { it.trueScreenPosition(localView) + localPoint } ?: localPoint
                                            showDesktopCtx = false
                                        },
                                        onDragStart = {
                                             // A long-press that turns into a drag must not leave a stale menu.
                                             iconCtxTarget = null
                                             showDesktopCtx = false
                                            val r = inlineRename
                                            if (r != null) commitRename(r, r.initialName)
                                            draggedId = item.id
                                            if (item.id !in selectedIds) {
                                                selectedIds = setOf(item.id)
                                                selectionAnchorId = item.id
                                            }
                                            // The unified detector only calls onDragStart once real
                                            // movement is confirmed, so this is already a real drag.
                                            dragMoved = true
                                            // Multi-select drag: record relative offsets of group members
                                            if (item.id in selectedIds && selectedIds.size > 1) {
                                                isDraggingGroup = true
                                                dragGroupOffsets.clear()
                                                groupRelativeOffsets.clear()
                                                selectedIds.filter { it != item.id }.forEach { otherId ->
                                                    // indexMap gives O(1) lookup — was doing a full O(n) linear
                                                    // scan of sortedItems per follower here, which is wasteful
                                                    // for larger selections since indexMap already exists.
                                                    val otherIdx = indexMap[otherId] ?: 0
                                                    val otherPos = customPositions[otherId]
                                                        ?: autoArrangePositions[otherId]
                                                        ?: autoGridPos(otherIdx.coerceAtLeast(0), workspaceRows, workspaceCols,
                                                            cellWPx, cellHPx, padLeftPx, padTopPx)
                                                        ?: return@forEach
                                                    groupRelativeOffsets[otherId] = otherPos - pos
                                                }
                                            }
                                        },
                                        onDrag = { _, amt ->
                                            val maxX = maxXBound
                                            val maxY = maxYBound
                                            if (isDraggingGroup) {
                                                // Clamp the WHOLE group's movement as one rigid unit, based on
                                                // its combined bounding box — not each icon independently.
                                                //
                                                // Previously every follower's target was clamped on its own
                                                // via .coerceIn(). Near an edge, a follower positioned further
                                                // from the anchor (in the direction of travel) would hit that
                                                // boundary before the anchor did — so it stopped while the
                                                // anchor and other followers kept moving, breaking the rigid
                                                // formation and causing icons to visually separate and pile up
                                                // on top of each other, especially noticeable dragging down
                                                // toward the taskbar/bottom edge.
                                                //
                                                // Fix: find the group's min/max relative offset in each axis
                                                // (including the anchor itself at rel = 0,0), then clamp the
                                                // ANCHOR's proposed position so that even the group's most
                                                // extreme member stays in bounds. Every member then gets the
                                                // exact same (possibly-limited) delta, so relative positions
                                                // — and therefore the whole formation — are always preserved.
                                                var minRelX = 0f; var maxRelX = 0f
                                                var minRelY = 0f; var maxRelY = 0f
                                                groupRelativeOffsets.values.forEach { rel ->
                                                    if (rel.x < minRelX) minRelX = rel.x
                                                    if (rel.x > maxRelX) maxRelX = rel.x
                                                    if (rel.y < minRelY) minRelY = rel.y
                                                    if (rel.y > maxRelY) maxRelY = rel.y
                                                }
                                                val proposedX = pos.x + amt.x
                                                val proposedY = pos.y + amt.y
                                                // Guard against an inverted range (selection wider/taller than
                                                // the available screen space) — coerceIn(min, max) throws if
                                                // min > max, so fall back to unclamped movement in that case
                                                // rather than crash.
                                                val lowX = padLeftPx - minRelX
                                                val highX = maxX - maxRelX
                                                val lowY = padTopPx - minRelY
                                                val highY = maxY - maxRelY
                                                val anchorX = if (lowX <= highX) proposedX.coerceIn(lowX, highX) else proposedX
                                                val anchorY = if (lowY <= highY) proposedY.coerceIn(lowY, highY) else proposedY
                                                pos = Offset(anchorX, anchorY)
                                                groupRelativeOffsets.forEach { (otherId, rel) ->
                                                    dragGroupOffsets[otherId] = Offset(anchorX + rel.x, anchorY + rel.y)
                                                }
                                            } else {
                                                pos = Offset(
                                                    (pos.x + amt.x).coerceIn(padLeftPx, maxX),
                                                    (pos.y + amt.y).coerceIn(padTopPx, maxY)
                                                )
                                            }
                                        },
                                        onDragEnd = {
                                            draggedId = null
                                            val wasGroup = isDraggingGroup
                                            isDraggingGroup = false
                                            val maxX = maxXBound
                                            val maxY = maxYBound

                                            // A dragged item/group dropped directly onto another desktop icon
                                            // is never allowed to remain overlapped. Folders are valid drop
                                            // targets (move the selected files into the folder); all other
                                            // icons reject the drop and return the whole group to its original
                                            // positions. This is the important distinction between a file-manager
                                            // drop and accidental icon piling.
                                            val anchorCenter = Offset(pos.x + cellWPx / 2f, pos.y + cellHPx / 2f)
                                            val actualDropTarget = sortedItems.firstOrNull { candidate ->
                                                if (candidate.id in selectedIds) return@firstOrNull false
                                                val targetPos = if (autoArrange) {
                                                    autoArrangePositions[candidate.id]
                                                } else {
                                                    customPositions[candidate.id]
                                                }
                                                targetPos != null &&
                                                    anchorCenter.x >= targetPos.x && anchorCenter.x <= targetPos.x + cellWPx &&
                                                    anchorCenter.y >= targetPos.y && anchorCenter.y <= targetPos.y + cellHPx
                                            }
                                            if (actualDropTarget != null) {
                                                val sourceIds = if (wasGroup) selectedIds else setOf(item.id)
                                                val sources = sourceIds
                                                    .mapNotNull { id -> sortedItems.find { it.id == id }?.file }
                                                    .filter { it.absolutePath != actualDropTarget.file.absolutePath }
                                                val targetPath = actualDropTarget.file.absolutePath.trimEnd(File.separatorChar)
                                                val validFolderDrop = actualDropTarget.file.isDirectory && sources.none { source ->
                                                    val sourcePath = source.absolutePath.trimEnd(File.separatorChar)
                                                    source.isDirectory && targetPath.startsWith(sourcePath + File.separator)
                                                }
                                                if (validFolderDrop && sources.isNotEmpty()) {
                                                    // Dragging onto a folder is a real move operation, not an icon
                                                    // placement operation. The copy engine handles it asynchronously.
                                                    viewModel.enqueueFileOperation(sources, actualDropTarget.file, isCut = true)
                                                }

                                                // Never leave the icons stacked on the drop target. Restore the
                                                // complete group to its pre-drag positions immediately. Followers
                                                // receive the restoration through the same broadcast mechanism used
                                                // during dragging; keep that broadcast alive for one frame so their
                                                // LaunchedEffect cannot miss it when isDraggingGroup flips false.
                                                pos = resolvedBasePos
                                                if (wasGroup) {
                                                    groupRelativeOffsets.forEach { (otherId, rel) ->
                                                        dragGroupOffsets[otherId] = resolvedBasePos + rel
                                                    }
                                                    val restoreIds = groupRelativeOffsets.keys.toList()
                                                    scope.launch {
                                                        kotlinx.coroutines.yield()
                                                        restoreIds.forEach { dragGroupOffsets.remove(it) }
                                                    }
                                                }
                                                dragMoved = false
                                                groupRelativeOffsets.clear()
                                            } else if (autoArrange) {
                                                pos = resolvedBasePos  // snap-back animation plays automatically
                                            } else if (!alignToGrid) {
                                                // Align-to-grid off: free pixel placement, like real Windows
                                                // with that box unchecked — no snapping, and icons ARE
                                                // allowed to visually overlap if you drop them on top of each
                                                // other, since there's no grid to rearrange them onto.
                                                val finalPos = Offset(pos.x.coerceIn(padLeftPx, maxX), pos.y.coerceIn(padTopPx, maxY))
                                                pos = finalPos
                                                customPositions[item.id] = finalPos
                                                prefs.saveCustomPositions(customPositions)
                                            } else {
                                                val otherCells = occupiedCells - posToCell(
                                                    customPositions[item.id] ?: resolvedBasePos,
                                                    cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows
                                                )
                                                val snapped = snapToGrid(
                                                    pos, cellWPx, cellHPx, padLeftPx, padTopPx,
                                                    workspaceWidthPx, workspaceHeightPx, otherCells, 0f
                                                )
                                                @Suppress("SENSELESS_COMPARISON")
                                                if (snapped == null) {
                                                    // Grid full — animate back to origin
                                                    pos = customPositions[item.id] ?: resolvedBasePos
                                                    showDesktopFullDialog = true
                                                } else {
                                                    val finalPos = Offset(
                                                        snapped.x.coerceIn(padLeftPx, maxX),
                                                        snapped.y.coerceIn(padTopPx, maxY)
                                                    )
                                                    pos = finalPos
                                                    customPositions[item.id] = finalPos
                                                    prefs.saveCustomPositions(customPositions)
                                                }
                                            }
                                            // Settle followers too: snap each to a free grid cell and
                                            // persist it — previously followers just froze at their last
                                            // live-drag pixel position (unsnapped, unsaved), so they'd
                                            // silently revert or look "stuck" off-grid after the drag.
                                            if (wasGroup && !autoArrange && alignToGrid) {
                                                val occupiedNow = occupiedCells.toMutableSet()
                                                customPositions[item.id]?.let {
                                                    occupiedNow.add(posToCell(it, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows))
                                                }
                                                groupRelativeOffsets.keys.forEach { otherId ->
                                                    val lastPos = dragGroupOffsets[otherId] ?: return@forEach
                                                    val freeCells = occupiedNow - posToCell(
                                                        customPositions[otherId] ?: lastPos,
                                                        cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows
                                                    )
                                                    val snapped = snapToGrid(
                                                        lastPos, cellWPx, cellHPx, padLeftPx, padTopPx,
                                                        workspaceWidthPx, workspaceHeightPx, freeCells, 0f
                                                    )
                                                    val finalPos = snapped?.let {
                                                        Offset(
                                                            it.x.coerceIn(padLeftPx, maxX),
                                                            it.y.coerceIn(padTopPx, maxY)
                                                        )
                                                    } ?: lastPos.copy(
                                                        x = lastPos.x.coerceIn(padLeftPx, maxX),
                                                        y = lastPos.y.coerceIn(padTopPx, maxY)
                                                    )
                                                    customPositions[otherId] = finalPos
                                                    dragGroupOffsets[otherId] = finalPos  // last broadcast: followers apply this final settle
                                                    occupiedNow.add(posToCell(finalPos, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows))
                                                }
                                                prefs.saveCustomPositions(customPositions)
                                            } else if (wasGroup && !autoArrange && !alignToGrid) {
                                                // Align-to-grid off: persist each follower's raw last-drag
                                                // position exactly as-is (bounds-clamped only) — no snapping,
                                                // matching the anchor's free-placement behavior above. Without
                                                // this branch, followers' positions were never written to
                                                // customPositions at all in free mode, so they'd silently
                                                // revert to an auto-assigned spot on the next refresh.
                                                groupRelativeOffsets.keys.forEach { otherId ->
                                                    val lastPos = dragGroupOffsets[otherId] ?: return@forEach
                                                    val finalPos = Offset(
                                                        lastPos.x.coerceIn(padLeftPx, maxX),
                                                        lastPos.y.coerceIn(padTopPx, maxY)
                                                    )
                                                    customPositions[otherId] = finalPos
                                                    dragGroupOffsets[otherId] = finalPos
                                                }
                                                prefs.saveCustomPositions(customPositions)
                                            }
                                            dragMoved = false
                                            groupRelativeOffsets.clear()
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            isDraggingGroup = false
                                            dragGroupOffsets.clear()
                                            groupRelativeOffsets.clear()
                                            dragMoved = false
                                            if (autoArrange) pos = resolvedBasePos
                                        }
                                    )
                                }
                        ) {
                            var liveRenameText by remember { mutableStateOf("") }

                            // Group members follow anchor during drag
                            val groupDelta = if (isInGroup && dragMoved) {
                                dragGroupOffsets[item.id] ?: Offset.Zero
                            } else Offset.Zero

                            // Keep the visual/icon subtree in its own restartable + skippable
                            // composition boundary. Desktop-level state (lasso movement, another
                            // icon being dragged, selection changes, etc.) can now recompose the
                            // parent without forcing every icon's expensive drawing/content tree
                            // to execute again when that icon's inputs are unchanged.
                            val iconOnLiveTextChange = remember(item.id) { { text: String -> liveRenameText = text } }
                            val iconOnRenameConfirm = remember(item.id) {
                                {
                                    inlineRename?.let { r -> commitRename(r, liveRenameText) } ?: Unit
                                }
                            }
                            DesktopIconRender(
                                item                  = item,
                                isSelected            = item.id in selectedIds,
                                iconSize              = iconSize,
                                inlineRenaming        = inlineRename?.targetId == item.id,
                                initialRenameText     = inlineRename?.initialName ?: item.name,
                                onLiveTextChange      = iconOnLiveTextChange,
                                onInlineRenameConfirm = iconOnRenameConfirm,
                                refreshFlickerAlpha   = desktopFlickerAlpha.value
                            )
                        }
                        } // basePos != null
                        } // stable key: item.id
                    }

                    // Desktop-full is a deliberate dialog, not a transient toast: the
                    // user needs a clear path to the complete Desktop directory.
                    // ── File-access toast ──────────────────────────────────
                    if (showFileAccessToast) {
                        LaunchedEffect(Unit) {
                            delay(6000)
                            showFileAccessToast = false
                        }
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 72.dp)
                                .background(Color(0xFF1C1C1C).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color(0xFF0078D4).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcon.FolderProhibited, contentDescription = null,
                                tint     = Color(0xFF0078D4),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "File access not granted",
                                color    = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    showFileAccessToast = false
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {}
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Grant", color = Color(0xFF0078D4), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Lasso selection rectangle
                    if (isSelecting) {
                        Canvas(Modifier.fillMaxSize()) {
                            val r = Rect(
                                minOf(selStart.x, selEnd.x), minOf(selStart.y, selEnd.y),
                                maxOf(selStart.x, selEnd.x), maxOf(selStart.y, selEnd.y)
                            )
                            drawRect(Color(0xFF0078D4).copy(alpha = 0.12f), r.topLeft, Size(r.width, r.height))
                            drawRect(Color(0xFF0078D4).copy(alpha = 0.50f), r.topLeft, Size(r.width, r.height),
                                style = Stroke(width = 1.2f.dp.toPx()))
                        }
                    }
                }
            }

        // Places a brand-new item at (near) wherever the "New" context menu was opened
        // from, like real Windows — snapped to the nearest free grid cell so it never
        // lands on top of an existing icon. Previously new items got no position
        // assigned at all, so they fell back to pure index-based auto-placement
        // (wherever their alphabetical/sort position happened to put them).
        fun placeNewItemAtClickPosition(id: String): Boolean {
            if (autoArrange) return false
            val maxX = maxXBound
            val finalPos: Offset? = if (alignToGrid) {
                val snapped = snapToGrid(
                    desktopCtxLocalOffset, cellWPx, cellHPx, padLeftPx, padTopPx,
                    workspaceWidthPx, workspaceHeightPx, occupiedCells, 0f
                )
                snapped?.let {
                    Offset(it.x.coerceIn(padLeftPx, maxX), it.y.coerceIn(padTopPx, maxYBound))
                }
            } else {
                // Free placement still cannot create an icon outside the fixed viewport.
                Offset(
                    desktopCtxLocalOffset.x.coerceIn(padLeftPx, maxX),
                    desktopCtxLocalOffset.y.coerceIn(padTopPx, maxYBound)
                ).takeIf {
                    posToCell(it, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows) !in occupiedCells
                }
            }
            if (finalPos == null) {
                showDesktopFullDialog = true
                return false
            }
            customPositions[id] = finalPos
            prefs.saveCustomPositions(customPositions)
            return true
        }

        // Assign every newly discovered item one persistent position exactly once.
        LaunchedEffect(items, autoArrange, workspaceRows, workspaceCols) {
            if (autoArrange) return@LaunchedEffect
            val occupied = customPositions.values.mapTo(mutableSetOf()) {
                posToCell(it, cellWPx, cellHPx, padLeftPx, padTopPx, workspaceCols, workspaceRows)
            }
            var changed = false
            sortedItems.forEach { item ->
                if (customPositions.containsKey(item.id)) return@forEach
                var chosen: Pair<Int, Int>? = null
                outer@ for (col in 0 until workspaceCols) {
                    for (row in 0 until workspaceRows) {
                        val cell = col to row
                        if (cell !in occupied) { chosen = cell; break@outer }
                    }
                }
                if (chosen != null) {
                    val pos = Offset(padLeftPx + chosen.first * cellWPx, padTopPx + chosen.second * cellHPx)
                    customPositions[item.id] = pos
                    occupied.add(chosen)
                    changed = true
                }
            }
            if (changed) prefs.saveCustomPositions(customPositions)
        }

        // Manual grid mode uses the exact same spacing/cell geometry as Auto Arrange.
        // This runs when entering manual mode and when the viewport/icon geometry changes,
        // but not after a normal drag, so user placement remains under their control.
        LaunchedEffect(
            autoArrange, alignToGrid, items.map { it.id },
            cellWPx, cellHPx, padLeftPx, padTopPx, workspaceRows, workspaceCols
        ) {
            if (!autoArrange && alignToGrid) {
                normalizeManualGridPositions()
            }
        }

            // Prompt when the fixed desktop cannot show the complete Desktop directory.
            // The prompt is keyed to the current item-set/capacity so it does not loop after
            // the user dismisses it, but a new overflow event can surface it again.
            LaunchedEffect(items.map { it.id }.hashCode(), desktopCapacity, iconSize, autoArrange) {
                val visibleManualCount = items.count { item ->
                    val p = customPositions[item.id]
                    p != null && p.x >= padLeftPx && p.y >= padTopPx &&
                        p.x <= maxXBound && p.y <= maxYBound
                }
                val shownCapacity = if (autoArrange) minOf(items.size, desktopCapacity) else visibleManualCount
                val overflow = items.size > desktopCapacity || shownCapacity < items.size
                val key = listOf(items.map { it.id }.hashCode(), desktopCapacity, iconSize, autoArrange).hashCode()
                if (overflow && key != lastDesktopFullPromptKey) {
                    lastDesktopFullPromptKey = key
                    showDesktopFullDialog = true
                }
            }

            // ── Desktop context menu ──
            if (showDesktopCtx) {
                bluebirdDesktopContextMenu(
                    offset              = desktopCtxOffset,
                    isDark              = isDark,
                    screenWidthDp       = screenW,
                    screenHeightDp      = screenH,
                    viewMode            = iconSize,
                    onViewChange        = { iconSize = it; prefs.iconSize = it; showDesktopCtx = false },
                    sortMode            = sortMode,
                    sortAscending       = sortAscending,
                    onSortChange        = { m, a ->
                        sortMode = m; sortAscending = a
                        prefs.sortMode = m; prefs.sortAscending = a
                        // Marks that the user has explicitly chosen a sort mode now — from
                        // here on, launches read the real saved mode instead of defaulting
                        // to NONE. (Explicitly picking "None" itself also sets this flag,
                        // so it correctly stays None on the next launch too, rather than
                        // being indistinguishable from "never touched".)
                        layoutPrefsStore.edit().putBoolean("user_set_sort", true).apply()
                        showDesktopCtx = false
                    },
                    autoArrange         = autoArrange,
                    onAutoArrangeToggle = {
                        autoArrange = it; prefs.autoArrange = it
                        // Keep manual placements intact while Auto Arrange is enabled.
                        showDesktopCtx = false
                    },
                    alignToGrid         = alignToGrid,
                    onAlignToGridToggle = { setAlignToGrid(it) },
                    showIcons           = showIconsOnDesktop,
                    onShowIconsToggle   = { showIconsOnDesktop = it; prefs.showIconsOnDesktop = it; showDesktopCtx = false },
                    onRefresh           = { viewModel.requestDesktopRefresh(); showDesktopCtx = false },
                    onPaste             = {
                        viewModel.pasteClipboard(desktopDir)
                        showDesktopCtx = false
                    },
                    hasPaste            = vmUiState.clipboardFiles.isNotEmpty(),
                    onNewFolder         = {
                        if (!autoArrange && occupiedCells.size >= desktopCapacity) {
                            showDesktopCtx = false
                            showDesktopFullDialog = true
                        } else {
                            val name   = uniqueName(desktopDir, "New folder")
                            val newDir = File(desktopDir, name)
                            newDir.mkdirs()
                            if (placeNewItemAtClickPosition(newDir.absolutePath)) pendingRenameId = newDir.absolutePath
                            showDesktopCtx  = false
                            scheduleRefresh()
                        }
                    },
                    onNewTextFile       = {
                        if (!autoArrange && occupiedCells.size >= desktopCapacity) {
                            showDesktopCtx = false
                            showDesktopFullDialog = true
                        } else {
                            val name    = uniqueName(desktopDir, "New Text Document", "txt")
                            val newFile = File(desktopDir, name)
                            try { newFile.createNewFile() } catch (_: Exception) {}
                            if (placeNewItemAtClickPosition(newFile.absolutePath)) pendingRenameId = newFile.absolutePath
                            showDesktopCtx  = false
                            scheduleRefresh()
                        }
                    },
                    onNewShortcut       = { showShortcutDialog  = true; showDesktopCtx = false },
                    onAddAppShortcut    = { showAppPickerDialog = true; showDesktopCtx = false },
                    // Personalize now opens the real Settings window at the Appearance
                    // category (which hosts the Personalization section — background,
                    // effects, live wallpaper) instead of showing its own separate
                    // WallpaperPersonalisePanel dialog. Matches Windows 11: right-click
                    // → Personalize always lands you in Settings, not a standalone popup.
                    onPersonalize       = {
                        viewModel.openWindow(LauncherScreen.SETTINGS, extras = mapOf("category" to "APPEARANCE"))
                        showDesktopCtx = false
                    },
                    onDisplaySettings   = { viewModel.openWindow(LauncherScreen.SETTINGS); showDesktopCtx = false },
                    onDismiss           = { showDesktopCtx = false }
                )
            }

            if (showDesktopFullDialog) {
                AlertDialog(
                    onDismissRequest = { showDesktopFullDialog = false },
                    icon = { Icon(FluentIcon.Folder, contentDescription = null) },
                    title = { Text("Desktop is full") },
                    text = {
                        Text(
                            "There isn't enough space to show everything on the desktop at this screen size. The files are still in the Desktop folder. Open File Explorer to see the full contents."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDesktopFullDialog = false
                                viewModel.openWindow(LauncherScreen.FILE_EXPLORER)
                            }
                        ) { Text("Open File Explorer") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDesktopFullDialog = false }) { Text("Close") }
                    }
                )
            }

            // ── Icon context menu ──
            iconCtxTarget?.let { target ->
                bluebirdIconContextMenu(
                    item               = target,
                    isDark             = isDark,
                    offset             = iconCtxOffset,
                    screenWidthDp      = screenW,
                    screenHeightDp     = screenH,
                    onDismiss          = { iconCtxTarget = null },
                    onOpen             = { openItem(target); iconCtxTarget = null },
                    onOpenWith         = {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", target.file
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }, "Open with"
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        } catch (_: Exception) {}
                        iconCtxTarget = null
                    },
                    onOpenFileLocation = { viewModel.openWindow(LauncherScreen.FILE_EXPLORER); iconCtxTarget = null },
                    onCut = {
                        val files = selectedIds
                            .mapNotNull { id -> items.find { it.id == id }?.file }
                            .ifEmpty { listOf(target.file) }
                        viewModel.setClipboard(files, cut = true)
                        iconCtxTarget = null
                    },
                    onCopy = {
                        val files = selectedIds
                            .mapNotNull { id -> items.find { it.id == id }?.file }
                            .ifEmpty { listOf(target.file) }
                        viewModel.setClipboard(files, cut = false)
                        iconCtxTarget = null
                    },
                    onPaste = if (target.type == DesktopItemType.FOLDER && vmUiState.clipboardFiles.isNotEmpty()) ({
                        viewModel.pasteClipboard(target.file)
                        iconCtxTarget = null
                        scheduleRefresh()
                    }) else null,
                    onDelete = {
                        val toDelete = selectedIds
                            .mapNotNull { id -> items.find { it.id == id }?.file }
                            .ifEmpty { listOf(target.file) }
                        toDelete.forEach { viewModel.deleteToRecycleBin(it.absolutePath) }
                        val label = if (toDelete.size == 1) "Deleted \"${toDelete[0].name}\"" else "Deleted ${toDelete.size} items"
                        viewModel.showUndoAction(label) {
                            toDelete.forEach { viewModel.restoreFromRecycleBinByOriginalPath(it.absolutePath) }
                        }
                        selectedIds   = emptySet()
                        iconCtxTarget = null
                        scheduleRefresh()
                    },
                    onRename = {
                        inlineRename  = InlineRenameState(targetId = target.id, initialName = target.name)
                        selectedIds   = setOf(target.id)
                        iconCtxTarget = null
                    },
                    onShare = {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", target.file
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }, "Share"
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        } catch (_: Exception) {}
                        iconCtxTarget = null
                    },
                    onSetAsWallpaper = if (target.type == DesktopItemType.IMAGE_FILE) ({
                        val uri = try {
                            androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", target.file
                            ).toString()
                        } catch (_: Exception) { "" }
                        if (uri.isNotEmpty()) {
                            customWallpaperUri = uri
                            wallpaperMode = DesktopWallpaperMode.CUSTOM
                            prefs.customWallpaperUri = uri
                            prefs.wallpaperMode = DesktopWallpaperMode.CUSTOM
                        }
                        iconCtxTarget = null
                    }) else null,
                    onCreateShortcut = {
                        val f = File(desktopDir, uniqueName(desktopDir, target.file.nameWithoutExtension, "desktop"))
                        f.writeText("type=file\npath=${target.file.absolutePath}\nlabel=${target.file.nameWithoutExtension}\n")
                        iconCtxTarget = null
                        scheduleRefresh()
                    },
                    onProperties = { propsTarget = target; showPropsDialog = true; iconCtxTarget = null }
                )
            }

            // ── Modals ──
            if (showShortcutDialog) {
                ShortcutDialog(
                    onConfirm = { pkg, label ->
                        val f = File(desktopDir, uniqueName(desktopDir, label, "desktop"))
                        f.writeText("type=app\npackage=$pkg\nlabel=$label\n")
                        placeNewItemAtClickPosition(f.absolutePath)
                        showShortcutDialog = false
                        scheduleRefresh()
                    },
                    onDismiss = { showShortcutDialog = false }
                )
            }

            if (showAppPickerDialog) {
                AppPickerDialog(
                    isDark       = isDark,
                    onAppSelected = { pkg, label ->
                        val f = File(desktopDir, uniqueName(desktopDir, label, "desktop"))
                        val content = if (pkg.startsWith("bluebird:")) {
                            "type=app\nlabel=$label\nbluebirdScreen=${pkg.removePrefix("bluebird:")}\n"
                        } else {
                            "type=app\npackage=$pkg\nlabel=$label\n"
                        }
                        f.writeText(content)
                        placeNewItemAtClickPosition(f.absolutePath)
                        showAppPickerDialog = false
                        scheduleRefresh()
                    },
                    onDismiss = { showAppPickerDialog = false }
                )
            }

            if (showPropsDialog && propsTarget != null) {
                PropertiesDialog(item = propsTarget!!, isDark = isDark, onDismiss = { showPropsDialog = false })
            }

            // WallpaperPersonalisePanel removed from here — Personalize (see
            // onPersonalize above) now opens the Settings window's Appearance
            // category instead, which hosts the same Background/Effects controls
            // via SettingsScreen.kt's PersonalizationSection. wallpaperMode,
            // gradientIndex, defaultImageIndex, customWallpaperUri and `prefs`
            // above stay as this composable's own live desktop-rendering state;
            // Settings reads/writes the same DesktopPreferences so both views of
            // the wallpaper state stay in sync without any extra plumbing.
        } // end BoxWithConstraints
    } // end BoxWithConstraints lambda — was missing, caused all errors
} // end Desktop

// ─────────────────────────────────────────────────────────────────
// DesktopIcon
// FIX: rename text state lives entirely inside this composable.
// initialRenameText is only read once when renaming begins (keyed
// on item.id). onLiveTextChange reports every keystroke up to the
// parent so commitRename can read the final value.
// ─────────────────────────────────────────────────────────────────
@Composable
private fun DesktopIcon(
    item: DesktopFileInfo,
    isSelected: Boolean,
    iconSize: DesktopIconSize,
    inlineRenaming: Boolean,
    initialRenameText: String,
    onLiveTextChange: (String) -> Unit,
    onInlineRenameConfirm: () -> Unit,
    // Shared, parent-owned refresh-flicker value (0f..1f) — every icon reads the exact
    // same value each frame, so the whole desktop fades out/in in perfect sync, and
    // there's no per-icon coroutine that can get interrupted and freeze an icon invisible.
    refreshFlickerAlpha: Float = 1f
) {
    val iconDp = iconSizeDp(iconSize).dp
    val cellW  = cellWidthDp(iconSize).dp
    val cellH  = cellHeightDp(iconSize).dp
    val focusRequester = remember { FocusRequester() }

    // Performance: cache pure icon conversions. Desktop recomposition can be frequent
    // while dragging/selecting, so avoid rebuilding ImageBitmap/vector wrappers.
    val imageBitmap = remember(item.iconBitmap) { item.iconBitmap?.asImageBitmap() }
    val fallbackIcon = remember(item.file.absolutePath, item.type, item.builtInScreen) {
        when (item.builtInScreen) {
            LauncherScreen.FILE_EXPLORER -> FluentIcon.Folder
            LauncherScreen.SETTINGS -> FluentIcon.Settings
            LauncherScreen.BROWSER -> FluentIcon.Globe
            LauncherScreen.CALCULATOR -> FluentIcon.Calculator
            LauncherScreen.CALENDAR -> FluentIcon.Calendar
            LauncherScreen.PHOTOS -> FluentIcon.ImageMultiple
            LauncherScreen.MEDIA_PLAYER -> FluentIcon.PlayCircle
            LauncherScreen.IMAGE_VIEWER -> FluentIcon.Image
            LauncherScreen.WORD_IMPRESS -> FluentIcon.DocumentText
            LauncherScreen.PremiumTextEditorScreen -> FluentIcon.TextFont
            LauncherScreen.TERMINAL -> FluentIcon.Console
            LauncherScreen.TASK_MANAGER -> FluentIcon.TaskList
            LauncherScreen.RECYCLE_BIN -> FluentIcon.Delete
            LauncherScreen.BLUEBIRD_STORE -> FluentIcon.Moon
            LauncherScreen.WEB_APP_MANAGER -> FluentIcon.Globe
            else -> getFileIcon(item.file)
        }
    }
    val fallbackTint = remember(item.file.absolutePath, item.type, item.builtInScreen) {
        if (item.builtInScreen != null) Color(0xFF0078D4) else getFileIconColor(item.file)
    }

    // FIX: KEY is item.id only — never changes on keystroke.
    // stripping extension for display (Windows UX convention)
    val rawInitial = remember(item.id) {
        val n = initialRenameText
        if (n.contains(".")) n.substringBeforeLast(".") else n
    }

    // FIX: local TextFieldValue — recompositions from parent never reset this
    var textValue by remember(item.id) {
        mutableStateOf(
            TextFieldValue(
                text      = rawInitial,
                selection = TextRange(0, rawInitial.length)   // pre-select all
            )
        )
    }

    LaunchedEffect(inlineRenaming) {
        if (inlineRenaming) {
            delay(80)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .width(cellW)
            .height(cellH)
            .padding(1.dp)
            .graphicsLayer(alpha = refreshFlickerAlpha),
        contentAlignment = Alignment.TopCenter
    ) {
        // bluebird-style selection: subtle blue tint + blue border glow
        val glowColor by animateColorAsState(
            targetValue   = if (isSelected) Color(0xFF0078D4).copy(alpha = 0.28f) else Color.Transparent,
            animationSpec = tween(150),
            label         = "selection_glow"
        )
        val borderColor by animateColorAsState(
            targetValue   = if (isSelected) Color(0xFF0078D4).copy(alpha = 0.80f) else Color.Transparent,
            animationSpec = tween(150),
            label         = "selection_border"
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(glowColor, RoundedCornerShape(5.dp))
                .border(1.dp, borderColor, RoundedCornerShape(5.dp))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp)
        ) {
            Box(
                Modifier.size(iconDp).padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.iconBitmap != null -> {
                        Image(
                            bitmap             = item.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier           = if (item.type == DesktopItemType.IMAGE_FILE)
                                Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp))
                            else Modifier.fillMaxSize(),
                            contentScale       = if (item.type == DesktopItemType.IMAGE_FILE)
                                ContentScale.Crop else ContentScale.Fit
                        )
                    }
                    else -> {
                        // Built-in apps now check for a custom Windows-11-style SVG icon
                        // first (same set Start Menu uses via BuiltInAppIcons.kt), falling
                        // back to the Fluent glyph automatically if one isn't found — so
                        // desktop icons and Start Menu icons for the same app always match.
                        val builtInName = item.builtInScreen?.let { builtInAppDisplayName(it) }
                        if (builtInName != null) {
                            BuiltInAppIcon(
                                appName = builtInName,
                                fallback = fallbackIcon,
                                tint = fallbackTint,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector            = fallbackIcon,
                                contentDescription = null,
                                tint               = fallbackTint,
                                modifier           = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Shortcut arrow badge (native app shortcuts + web app shortcuts)
                if (item.type == DesktopItemType.APP_SHORTCUT || item.type == DesktopItemType.WEB_APP_SHORTCUT) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.White, RoundedCornerShape(2.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = FluentIcon.ArrowReply, contentDescription = null,
                            tint     = Color.Black,
                            modifier = Modifier.size(10.dp).graphicsLayer(scaleX = -1f))
                    }
                }

                // Audio/Video badge
                if (item.type == DesktopItemType.MUSIC_FILE || item.type == DesktopItemType.VIDEO_FILE) {
                    val badgeColor = if (item.type == DesktopItemType.MUSIC_FILE) Color(0xFFFF8C00) else Color(0xFF8764B8)
                    val badgeIcon  = if (item.type == DesktopItemType.MUSIC_FILE) FluentIcon.MusicNote2 else FluentIcon.Play
                    Box(
                        Modifier.size(13.dp).align(Alignment.BottomEnd).background(Color(0xFF1C1C1C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(9.dp))
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // ── bluebird inline rename field ──
            // White background, tight padding, blue 1.5dp border — matches
            // the Windows 11 desktop rename UX exactly.
            if (inlineRenaming) {
                BasicTextField(
                    value         = textValue,
                    onValueChange = { tv ->
                        textValue = tv
                        onLiveTextChange(tv.text)
                    },
                    singleLine    = false,
                    maxLines      = 3,
                    textStyle     = TextStyle(
                        color      = Color(0xFF1A1A1A),
                        fontSize   = 11.5.sp,
                        textAlign  = TextAlign.Center,
                        lineHeight  = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onInlineRenameConfirm() }),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .border(1.5.dp, Color(0xFF0078D4), RoundedCornerShape(2.dp))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                        .focusRequester(focusRequester)
                )
            } else {
                Text(
                    text       = item.name,
                    color      = Color.White,
                    fontSize   = 11.5.sp,
                    lineHeight  = 14.sp,
                    textAlign  = TextAlign.Center,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    modifier   = Modifier.fillMaxWidth(),
                    style      = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color      = Color.Black.copy(alpha = 0.90f),
                            offset     = Offset(0.8f, 1.2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Wallpaper / Personalise Panel
// Lets the user switch between APPARENT (gradients), DEFAULT (images),
// and pick which gradient/image is active.
// ─────────────────────────────────────────────────────────────────
// internal (was private): shared with SettingsScreen.kt's PersonalizationSection
internal fun bgAnimationLabel(type: BgAnimationType): String = when (type) {
    BgAnimationType.SNOW -> "Snow"
    BgAnimationType.BUBBLES -> "Bubbles"
    BgAnimationType.STARS -> "Stars"
    BgAnimationType.RAIN -> "Rain"
    BgAnimationType.HEARTS -> "Hearts"
    BgAnimationType.CONFETTI -> "Confetti"
    BgAnimationType.FIREFLIES -> "Fireflies"
    BgAnimationType.LEAVES -> "Leaves"
    BgAnimationType.MATRIX -> "Matrix Rain"
    BgAnimationType.SAKURA -> "Sakura"
}

// internal (was private): shared with SettingsScreen.kt's PersonalizationSection
internal fun bgAnimationEmoji(type: BgAnimationType): String = when (type) {
    BgAnimationType.SNOW -> "❄"
    BgAnimationType.BUBBLES -> "🫧"
    BgAnimationType.STARS -> "✨"
    BgAnimationType.RAIN -> "🌧"
    BgAnimationType.HEARTS -> "❤"
    BgAnimationType.CONFETTI -> "🎊"
    BgAnimationType.FIREFLIES -> "✨"
    BgAnimationType.LEAVES -> "🍂"
    BgAnimationType.MATRIX -> "🟩"
    BgAnimationType.SAKURA -> "🌸"
}

// WallpaperPersonalisePanel (Background/Effects dialog) used to be defined
// here. It has been removed and its content ported to SettingsScreen.kt's
// PersonalizationSection — see onPersonalize above, which now opens the
// Settings window instead of this in-place dialog.

// ─────────────────────────────────────────────────────────────────
// bluebird style Desktop Context Menu
// ─────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────
// bluebird-style context menu infrastructure
//
// Uses real Compose Popups positioned with the ACTUAL measured menu size and
// the real window bounds (both supplied by Compose itself via
// PopupPositionProvider), instead of the old approach of guessing the menu's
// height and clamping against LocalConfiguration's screen size — that guess
// silently went wrong whenever the real menu height differed from the
// estimate (e.g. once submenus existed), which is what caused the menu to
// sometimes land away from the actual tap point.
//
// Submenus ("View", "Sort by", "New", "Open with") are now real flyout
// Popups anchored beside their parent row's own on-screen bounds — like
// real Windows 11 — instead of an inline accordion that grows the menu.
// ─────────────────────────────────────────────────────────────────
private class ClickAnchoredMenuPosition(
    private val clickWindowPos: IntOffset,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var x = clickWindowPos.x
        var y = clickWindowPos.y
        if (x + popupContentSize.width > windowSize.width - marginPx) {
            x = windowSize.width - popupContentSize.width - marginPx
        }
        if (y + popupContentSize.height > windowSize.height - marginPx) {
            y = windowSize.height - popupContentSize.height - marginPx
        }
        x = x.coerceAtLeast(marginPx)
        y = y.coerceAtLeast(marginPx)
        return IntOffset(x, y)
    }
}

/** Flyout beside a parent row's real bounds — right normally, flipped left if that
 *  would overflow the screen's right edge, matching real Windows 11 submenu behavior. */
private class FlyoutMenuPosition(
    private val parentRowWindowBounds: IntRect,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var x = parentRowWindowBounds.right
        if (x + popupContentSize.width > windowSize.width - marginPx) {
            x = (parentRowWindowBounds.left - popupContentSize.width).coerceAtLeast(marginPx)
        }
        var y = parentRowWindowBounds.top
        if (y + popupContentSize.height > windowSize.height - marginPx) {
            y = windowSize.height - popupContentSize.height - marginPx
        }
        x = x.coerceAtLeast(marginPx)
        y = y.coerceAtLeast(marginPx)
        return IntOffset(x, y)
    }
}

/** True absolute screen position of a composable.
 *
 *  Compose's `LayoutCoordinates.positionInWindow()` is relative to the NEAREST enclosing
 *  window — which, for anything living inside a Popup (Popups are genuinely separate
 *  Android platform windows), is that Popup's OWN window, not the true screen. Compose's
 *  own Popup positioning system, however, expects and returns ABSOLUTE SCREEN coordinates
 *  (via WindowManager). Feeding a Popup-relative position into another Popup's position
 *  provider silently lands it near whatever that inner window's own screen origin happens
 *  to be — which is what caused flyout submenus to always appear pinned near the top
 *  instead of following the (possibly-repositioned) main menu.
 *
 *  Fix: add the enclosing window's own screen origin (via the plain Android View API
 *  `getLocationOnScreen`) to the local in-window position, which works correctly whether
 *  or not the composable is nested inside any number of Popups. */
private fun LayoutCoordinates.trueScreenPosition(view: android.view.View): Offset {
    val loc = IntArray(2)
    view.getLocationOnScreen(loc)
    val local = positionInWindow()
    return Offset(loc[0] + local.x, loc[1] + local.y)
}

@Composable
private fun bluebirdMenuPopup(
    clickWindowPos: Offset,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val posPx = IntOffset(clickWindowPos.x.roundToInt(), clickWindowPos.y.roundToInt())
    Popup(
        popupPositionProvider = remember(posPx) { ClickAnchoredMenuPosition(posPx, marginPx = 12) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        content()
    }
}

/** A menu row that owns a flyout submenu — tapping the row toggles its flyout,
 *  positioned beside the row's own real on-screen bounds instead of expanding inline. */
@Composable
private fun bluebirdFlyoutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tc: Color,
    tcDim: Color,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onCloseFlyout: () -> Unit,
    submenuContent: @Composable () -> Unit
) {
    val view = LocalView.current
    var rowWindowBounds by remember { mutableStateOf(IntRect.Zero) }
    Box(
        Modifier.onGloballyPositioned { coords ->
            val p = coords.trueScreenPosition(view)
            rowWindowBounds = IntRect(
                p.x.roundToInt(), p.y.roundToInt(),
                p.x.roundToInt() + coords.size.width, p.y.roundToInt() + coords.size.height
            )
        }
    ) {
        W11CtxRow(icon, label, tc, tcDim, hasArrow = true) { onToggle() }
    }
    if (isOpen) {
        Popup(
            popupPositionProvider = remember(rowWindowBounds) { FlyoutMenuPosition(rowWindowBounds, marginPx = 12) },
            onDismissRequest = onCloseFlyout,
            properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
        ) {
            submenuContent()
        }
    }
}

@Composable
fun bluebirdDesktopContextMenu(
    offset: Offset,
    isDark: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
    viewMode: DesktopIconSize,
    onViewChange: (DesktopIconSize) -> Unit,
    sortMode: DesktopSortMode,
    sortAscending: Boolean,
    onSortChange: (DesktopSortMode, Boolean) -> Unit,
    autoArrange: Boolean,
    onAutoArrangeToggle: (Boolean) -> Unit,
    alignToGrid: Boolean,
    onAlignToGridToggle: (Boolean) -> Unit,
    showIcons: Boolean,
    onShowIconsToggle: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPaste: () -> Unit,
    hasPaste: Boolean,
    onNewFolder: () -> Unit,
    onNewTextFile: () -> Unit,
    onNewShortcut: () -> Unit,
    onAddAppShortcut: () -> Unit,
    onPersonalize: () -> Unit,
    onDisplaySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val menuW    = 210
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val accent   = Color(0xFF0078D4)

    var openSub by remember { mutableStateOf<String?>(null) }

    bluebirdMenuPopup(clickWindowPos = offset, onDismiss = onDismiss) {
        Surface(
            modifier        = Modifier.width(menuW.dp),
            shape           = RoundedCornerShape(8.dp),
            color           = bg,
            shadowElevation = 16.dp,
            border          = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
        ) {
            Column(Modifier.padding(vertical = 5.dp)) {
                bluebirdFlyoutRow(
                    icon = FluentIcon.Grid, label = "View", tc = tc, tcDim = tcDim,
                    isOpen = openSub == "view",
                    onToggle = { openSub = if (openSub == "view") null else "view" },
                    onCloseFlyout = { openSub = null }
                ) {
                    Surface(
                        modifier = Modifier.width(190.dp), shape = RoundedCornerShape(8.dp), color = bg,
                        shadowElevation = 16.dp, border = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
                    ) {
                        Column(Modifier.padding(vertical = 5.dp)) {
                            W11SubRow("Large icons",  viewMode == DesktopIconSize.LARGE,  tc, accent) { onViewChange(DesktopIconSize.LARGE) }
                            W11SubRow("Medium icons", viewMode == DesktopIconSize.MEDIUM, tc, accent) { onViewChange(DesktopIconSize.MEDIUM) }
                            W11SubRow("Small icons",  viewMode == DesktopIconSize.SMALL,  tc, accent) { onViewChange(DesktopIconSize.SMALL) }
                            W11CtxDivider(divColor)
                            W11SubRow("Auto arrange icons",  autoArrange, tc, accent) { onAutoArrangeToggle(!autoArrange) }
                            W11SubRow("Align icons to grid", alignToGrid, tc, accent) { onAlignToGridToggle(!alignToGrid) }
                            W11CtxDivider(divColor)
                            W11SubRow("Show desktop icons", showIcons, tc, accent) { onShowIconsToggle(!showIcons) }
                        }
                    }
                }

                bluebirdFlyoutRow(
                    icon = FluentIcon.ArrowSort, label = "Sort by", tc = tc, tcDim = tcDim,
                    isOpen = openSub == "sort",
                    onToggle = { openSub = if (openSub == "sort") null else "sort" },
                    onCloseFlyout = { openSub = null }
                ) {
                    Surface(
                        modifier = Modifier.width(190.dp), shape = RoundedCornerShape(8.dp), color = bg,
                        shadowElevation = 16.dp, border = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
                    ) {
                        Column(Modifier.padding(vertical = 5.dp)) {
                            // "None" — the default, matching real Windows (sort isn't on until you
                            // turn it on). Files stay exactly where created/pasted/dropped.
                            W11SubRow("(None)",        sortMode == DesktopSortMode.NONE,          tc, accent) { onSortChange(DesktopSortMode.NONE,          sortAscending) }
                            W11CtxDivider(divColor)
                            W11SubRow("Name",          sortMode == DesktopSortMode.NAME,          tc, accent) { onSortChange(DesktopSortMode.NAME,          sortAscending) }
                            W11SubRow("Size",          sortMode == DesktopSortMode.SIZE,          tc, accent) { onSortChange(DesktopSortMode.SIZE,          sortAscending) }
                            W11SubRow("Item type",     sortMode == DesktopSortMode.TYPE,          tc, accent) { onSortChange(DesktopSortMode.TYPE,          sortAscending) }
                            W11SubRow("Date modified", sortMode == DesktopSortMode.DATE_MODIFIED, tc, accent) { onSortChange(DesktopSortMode.DATE_MODIFIED, sortAscending) }
                            W11CtxDivider(divColor)
                            W11SubRow("Ascending",  sortAscending,  tc, accent) { onSortChange(sortMode, true) }
                            W11SubRow("Descending", !sortAscending, tc, accent) { onSortChange(sortMode, false) }
                        }
                    }
                }

                W11CtxRow(FluentIcon.ArrowSync,      "Refresh",          tc, tcDim) { onRefresh(); onDismiss() }
                W11CtxDivider(divColor)
                W11CtxRow(FluentIcon.ClipboardPaste, "Paste",
                    if (hasPaste) tc else tcDim, tcDim, enabled = hasPaste) { onPaste(); onDismiss() }
                W11CtxDivider(divColor)

                bluebirdFlyoutRow(
                    icon = FluentIcon.Add, label = "New", tc = tc, tcDim = tcDim,
                    isOpen = openSub == "new",
                    onToggle = { openSub = if (openSub == "new") null else "new" },
                    onCloseFlyout = { openSub = null }
                ) {
                    Surface(
                        modifier = Modifier.width(220.dp), shape = RoundedCornerShape(8.dp), color = bg,
                        shadowElevation = 16.dp, border = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
                    ) {
                        Column(Modifier.padding(vertical = 5.dp)) {
                            W11SubRowIcon(FluentIcon.Folder,      "Folder",                     Color(0xFFFFC107), tc) { onNewFolder();       onDismiss() }
                            W11SubRowIcon(FluentIcon.Link,        "Shortcut link",              Color(0xFF0078D4), tc) { onNewShortcut();      onDismiss() }
                            W11SubRowIcon(FluentIcon.Apps,        "Add Installed App Shortcut", Color(0xFF107C10), tc) { onAddAppShortcut();   onDismiss() }
                            W11CtxDivider(divColor)
                            W11SubRowIcon(FluentIcon.DocumentText, "Text Document",              Color(0xFF0078D4), tc) { onNewTextFile();      onDismiss() }
                        }
                    }
                }

                W11CtxDivider(divColor)
                W11CtxRow(FluentIcon.Desktop, "Display settings", tc, tcDim) { onDisplaySettings(); onDismiss() }
                W11CtxRow(FluentIcon.Color, "Personalise",      tc, tcDim) { onPersonalize(); onDismiss() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// bluebird style Icon Context Menu
// FIX: added optional onSetAsWallpaper for image files
// ─────────────────────────────────────────────────────────────────
@Composable
fun bluebirdIconContextMenu(
    item: DesktopFileInfo,
    isDark: Boolean,
    offset: Offset,
    screenWidthDp: Int,
    screenHeightDp: Int,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onOpenFileLocation: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSetAsWallpaper: (() -> Unit)? = null,
    onCreateShortcut: () -> Unit,
    onProperties: () -> Unit
) {
    val menuW    = 220
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val danger   = Color(0xFFE81123)

    var openSub by remember { mutableStateOf<String?>(null) }

    bluebirdMenuPopup(clickWindowPos = offset, onDismiss = onDismiss) {
        Surface(
            modifier        = Modifier.width(menuW.dp),
            shape           = RoundedCornerShape(8.dp),
            color           = bg,
            shadowElevation = 16.dp,
            border          = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
        ) {
            Column(Modifier.padding(vertical = 5.dp)) {
                // Quick action row
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    W11QuickAction(FluentIcon.Cut,             "Cut",    tc)     { onCut();    onDismiss() }
                    W11QuickAction(FluentIcon.Copy,            "Copy",   tc)     { onCopy();   onDismiss() }
                    W11QuickAction(FluentIcon.Rename, "Rename", tc)     { onRename(); onDismiss() }
                    W11QuickAction(FluentIcon.Share,                  "Share",  tc)     { onShare();  onDismiss() }
                    W11QuickAction(FluentIcon.Delete,                 "Delete", danger) { onDelete(); onDismiss() }
                }

                W11CtxDivider(divColor)
                W11CtxRow(FluentIcon.Open, "Open", tc, tcDim, isBold = true) { onOpen(); onDismiss() }

                if (item.type == DesktopItemType.FOLDER && onPaste != null) {
                    W11CtxRow(FluentIcon.ClipboardPaste, "Paste", tc, tcDim) { onPaste(); onDismiss() }
                }

                bluebirdFlyoutRow(
                    icon = FluentIcon.Apps, label = "Open with", tc = tc, tcDim = tcDim,
                    isOpen = openSub == "openwith",
                    onToggle = { openSub = if (openSub == "openwith") null else "openwith" },
                    onCloseFlyout = { openSub = null }
                ) {
                    Surface(
                        modifier = Modifier.width(190.dp), shape = RoundedCornerShape(8.dp), color = bg,
                        shadowElevation = 16.dp, border = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
                    ) {
                        Column(Modifier.padding(vertical = 5.dp)) {
                            W11SubRowIcon(FluentIcon.Open, "Choose app", tc.copy(0.8f), tc) { onOpenWith(); onDismiss() }
                        }
                    }
                }

                if (item.type == DesktopItemType.APP_SHORTCUT) {
                    W11CtxRow(FluentIcon.FolderOpen, "Open file location", tc, tcDim) { onOpenFileLocation(); onDismiss() }
                }

                // "Set as wallpaper" — only for image files
                if (onSetAsWallpaper != null) {
                    W11CtxDivider(divColor)
                    W11CtxRow(FluentIcon.ImageMultiple, "Set as wallpaper", tc, tcDim) { onSetAsWallpaper(); onDismiss() }
                }

                W11CtxDivider(divColor)
                W11CtxRow(FluentIcon.Link,   "Create shortcut", tc,     tcDim) { onCreateShortcut(); onDismiss() }
                W11CtxRow(FluentIcon.Delete, "Delete",          danger, tcDim) { onDelete();         onDismiss() }
                W11CtxDivider(divColor)
                W11CtxRow(FluentIcon.Info,   "Properties",      tc,     tcDim) { onProperties();     onDismiss() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Context menu primitives
// ─────────────────────────────────────────────────────────────────
@Composable
private fun W11CtxRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tc: Color,
    tcDim: Color,
    hasArrow: Boolean = false,
    isBold: Boolean   = false,
    enabled: Boolean  = true,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(horizontal = 12.dp)
            .height(32.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tc.copy(0.8f), modifier = Modifier.size(15.dp))
        Text(label, color = tc, fontSize = 12.5.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f), maxLines = 1)
        if (hasArrow) Icon(imageVector = FluentIcon.ChevronRight, contentDescription = null, tint = tcDim, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun W11SubRow(
    label: String,
    isActive: Boolean,
    tc: Color,
    accent: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(start = 34.dp, end = 12.dp)
            .height(28.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isActive) Icon(imageVector = FluentIcon.Checkmark, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        else          Spacer(Modifier.size(13.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1)
    }
}

@Composable
private fun W11SubRowIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color,
    tc: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHovered) tc.copy(0.06f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(start = 22.dp, end = 12.dp)
            .height(28.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun W11QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHovered) tint.copy(0.10f) else Color.Transparent)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(
                    onPress = { isHovered = true; tryAwaitRelease(); isHovered = false },
                    onTap   = { onClick() }
                )
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = tooltip, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun W11CtxDivider(color: Color) {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        thickness = 1.dp,
        color     = color
    )
}

// ─────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────
@Composable
fun SmartNewItemDialog(
    title: String,
    suggested: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(suggested) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(8.dp),
        title            = { Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        text             = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                singleLine    = true,
                shape         = RoundedCornerShape(4.dp),
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ShortcutDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    var pkg        by remember { mutableStateOf("") }
    var label      by remember { mutableStateOf("") }

    if (showPicker) {
        AppPickerDialog(
            isDark = true,
            onAppSelected = { p, l ->
                pkg        = p
                label      = l
                showPicker = false
                onConfirm(p, l)
            },
            onDismiss = { showPicker = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(8.dp),
        title            = { Text("Create Shortcut", fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pkg, onValueChange = { pkg = it },
                    label = { Text("Package Name (e.g. com.example.app)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = FluentIcon.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse installed apps…")
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (pkg.isNotBlank() && label.isNotBlank()) onConfirm(pkg.trim(), label.trim()) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────
// App Picker Dialog
// ─────────────────────────────────────────────────────────────────
@Composable
fun AppPickerDialog(
    isDark: Boolean,
    onAppSelected: (packageName: String, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context     = LocalContext.current
    var appsList    by remember { mutableStateOf(listOf<AppInfoItem>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(true) }

    val bg = if (isDark) Color(0xFF202020) else Color.White
    val tc = if (isDark) Color.White else Color.Black

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val resolved = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { app ->
                    pm.getLaunchIntentForPackage(app.packageName) ?: return@mapNotNull null
                    val bitmap = try { drawableToBitmap(app.loadIcon(pm)) } catch (_: Exception) { null }
                        ?: return@mapNotNull null
                    AppInfoItem(label = app.loadLabel(pm).toString(), packageName = app.packageName, iconBitmap = bitmap)
                }
                .sortedBy { it.label.lowercase() }
            withContext(Dispatchers.Main) { appsList = resolved; isLoading = false }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(10.dp),
        title = {
            Text("Add App Shortcut", color = tc, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            // BUG 10 FIX: fixed 360.dp height breaks LazyColumn weight; use fillMaxHeight fraction instead
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps…") }, singleLine = true,
                    leadingIcon = { Icon(imageVector = FluentIcon.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0078D4))
                    }
                } else {
                    val builtIns = remember(searchQuery) {
                        listOf(
                            "Files" to LauncherScreen.FILE_EXPLORER,
                            "Settings" to LauncherScreen.SETTINGS,
                            "Browser" to LauncherScreen.BROWSER,
                            "Calculator" to LauncherScreen.CALCULATOR,
                            "Calendar" to LauncherScreen.CALENDAR,
                            "Photos" to LauncherScreen.PHOTOS,
                            "Media Player" to LauncherScreen.MEDIA_PLAYER,
                            "Image Viewer" to LauncherScreen.IMAGE_VIEWER,
                            "Word Impress" to LauncherScreen.WORD_IMPRESS,
                            "Text Editor" to LauncherScreen.PremiumTextEditorScreen,
                            "Terminal" to LauncherScreen.TERMINAL,
                            "Task Manager" to LauncherScreen.TASK_MANAGER,
                            "Recycle Bin" to LauncherScreen.RECYCLE_BIN,
                            "Bluebird Store" to LauncherScreen.BLUEBIRD_STORE,
                            "Web App Manager" to LauncherScreen.WEB_APP_MANAGER
                        ).filter { it.first.contains(searchQuery, true) }
                    }
                    if (builtIns.isNotEmpty()) {
                        Text("Bluebird apps", color = tc.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                        builtIns.forEach { (label, screen) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onAppSelected("bluebird:${screen.name}", label)
                                }.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = FluentIcon.Apps, contentDescription = null, tint = Color(0xFF0078D4), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(label, color = tc, fontSize = 12.sp)
                            }
                        }
                    }
                    val filtered = remember(appsList, searchQuery) {
                        if (searchQuery.isBlank()) appsList
                        else appsList.filter {
                            it.label.contains(searchQuery, ignoreCase = true) ||
                                    it.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No apps found", color = tc.copy(0.5f), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filtered, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { onAppSelected(app.packageName, app.label) }
                                        .padding(vertical = 7.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(bitmap = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.label, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, color = tc, fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(app.packageName, color = tc.copy(alpha = 0.5f), fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF0078D4)) } }
    )
}

// ─────────────────────────────────────────────────────────────────
// Skippable desktop icon rendering boundary
// ─────────────────────────────────────────────────────────────────
// Keep this wrapper deliberately small. Gesture/state orchestration stays in Desktop(),
// while the visual subtree gets its own Compose restart group. This is important during
// lasso selection and dragging: a change to one icon's position should not require the
// visual contents of every other icon to be rebuilt.
@Composable
private fun DesktopIconRender(
    item: DesktopFileInfo,
    isSelected: Boolean,
    iconSize: DesktopIconSize,
    inlineRenaming: Boolean,
    initialRenameText: String,
    onLiveTextChange: (String) -> Unit,
    onInlineRenameConfirm: () -> Unit,
    refreshFlickerAlpha: Float
) {
    DesktopIcon(
        item                   = item,
        isSelected             = isSelected,
        iconSize               = iconSize,
        inlineRenaming         = inlineRenaming,
        initialRenameText      = initialRenameText,
        onLiveTextChange       = onLiveTextChange,
        onInlineRenameConfirm  = onInlineRenameConfirm,
        refreshFlickerAlpha    = refreshFlickerAlpha
    )
}

// ─────────────────────────────────────────────────────────────────
// Properties Dialog
// ─────────────────────────────────────────────────────────────────
@Composable
fun PropertiesDialog(item: DesktopFileInfo, isDark: Boolean, onDismiss: () -> Unit) {
    val bg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val tc  = if (isDark) Color.White else Color(0xFF1A1A1A)
    val tcm = if (isDark) Color(0xFF909090) else Color(0xFF666666)

    var fileSize by remember { mutableStateOf(0L) }
    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            fileSize = if (item.file.isDirectory)
                item.file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            else item.file.length()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(8.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = getFileIcon(item.file), contentDescription = null, tint = getFileIconColor(item.file), modifier = Modifier.size(22.dp))
                Text("Properties", color = tc, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Name"     to item.name,
                    "Type"     to if (item.file.isDirectory) "Folder"
                    else item.file.extension.uppercase().ifBlank { "File" },
                    "Size"     to formatFileSize(fileSize),
                    "Location" to (item.file.parent ?: "/"),
                    "Modified" to SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.getDefault())
                        .format(Date(item.file.lastModified()))
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(k, color = tcm, fontSize = 11.5.sp, modifier = Modifier.width(72.dp))
                        Text(v, color = tc,  fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}
