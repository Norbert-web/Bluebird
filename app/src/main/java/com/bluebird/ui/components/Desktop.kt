package com.bluebird.ui.components
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.Manifest
import android.content.ActivityNotFoundException
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
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.LocalTextStyle
// I need to review something here,LAMN NOBERT
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bluebird.*
import kotlinx.coroutines.Dispatchers
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
private fun LiveWallpaperRenderer(type: LiveWallpaperType, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "live_wallpaper")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "live_wallpaper_t"
    )
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
                    val cy = h * (0.25f + 0.15f * i) + kotlin.math.sin(phase) * h * 0.08f
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(col.copy(alpha = 0.25f), Color.Transparent),
                            startY = cy - h * 0.2f, endY = cy + h * 0.35f
                        ),
                        topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h)
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
                    val drift = kotlin.math.sin(t * 6.283f + i) * 30f
                    val r = rng.nextFloat() * 50f + 30f
                    drawCircle(
                        Color(listOf(0xFF64B5F6, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFFB74D)[i % 4].toInt())
                            .copy(alpha = 0.12f),
                        radius = r, center = Offset(baseX + drift, baseY + drift * 0.6f)
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
    val webAppIconPath: String? = null   // path relative to context.filesDir
)

enum class DesktopItemType {
    FOLDER, TEXT_FILE, IMAGE_FILE, MUSIC_FILE, VIDEO_FILE,
    APP_SHORTCUT, WEB_APP_SHORTCUT, OTHER_FILE, THIS_PC, RECYCLE_BIN, SETTINGS_ICON
}

enum class DesktopIconSize { SMALL, MEDIUM, LARGE }
enum class DesktopSortMode { NAME, DATE_MODIFIED, TYPE, SIZE }

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
fun getFileIcon(file: File): ImageVector = when {
    file.isDirectory -> Icons.Default.Folder
    file.extension.lowercase() in MUSIC_EXTS  -> Icons.Default.AudioFile
    file.extension.lowercase() in VIDEO_EXTS  -> Icons.Default.PlayCircle
    file.extension.lowercase() in IMAGE_EXTS  -> Icons.Default.Image
    file.extension.lowercase() in TEXT_EXTS   -> Icons.Default.Description
    file.extension.lowercase() == "pdf"       -> Icons.Default.PictureAsPdf
    file.extension.lowercase() == "apk"       -> Icons.Default.Android
    file.extension.lowercase() in setOf("zip","rar","7z","tar","gz") -> Icons.Default.Archive
    file.extension.lowercase() in setOf("doc","docx") -> Icons.Default.Article
    file.extension.lowercase() in setOf("xls","xlsx") -> Icons.Default.TableChart
    file.extension.lowercase() == "webapp" -> Icons.Default.Public
    file.extension.lowercase() == "desktop" -> Icons.Default.Apps
    else -> Icons.Default.InsertDriveFile
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
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onLongPressReleased: (Offset) -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val doubleTapTimeout  = viewConfiguration.doubleTapTimeoutMillis
    val slop              = viewConfiguration.touchSlop

    awaitEachGesture {
        val down    = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        val pid     = down.id

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
                // Long-press threshold reached while still down and unmoved.
                when (awaitReleaseOrSlop(pid, downPos, slop, Long.MAX_VALUE / 2)) {
                    "released" -> onLongPressReleased(downPos)
                    "moved"    -> performDrag(pid, downPos, onDragStart, onDrag, onDragEnd, onDragCancel)
                    else       -> onDragCancel()
                }
            }
            "moved"  -> { /* moved before the long-press threshold — ignored, same as before */ }
            "cancel" -> onDragCancel()
        }
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
            val label = lines.find { it.startsWith("label=") }?.removePrefix("label=")?.trim()
                ?: file.nameWithoutExtension
            val iconBmp: Bitmap? = if (pkg.isNotBlank()) {
                try { drawableToBitmap(context.packageManager.getApplicationIcon(pkg)) }
                catch (_: Exception) { null }
            } else null
            DesktopFileInfo(
                id = file.absolutePath, file = file, name = label,
                type = DesktopItemType.APP_SHORTCUT, packageName = pkg, iconBitmap = iconBmp
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
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
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
            val thumb = try { BitmapFactory.decodeFile(file.absolutePath, MEDIA_THUMB_SAMPLE) } catch (_: Exception) { null }
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
        DesktopItemType.APP_SHORTCUT ->
            item.packageName?.let { pkg ->
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) context.startActivity(intent)
                } catch (_: Exception) {}
            }
        DesktopItemType.WEB_APP_SHORTCUT ->
            viewModel.openWebAppWindow(
                id = item.webAppId ?: item.id,
                name = item.name,
                url = item.webAppUrl ?: "",
                iconPath = item.webAppIconPath
            )
        else -> viewModel.openFileWithSystem(context, item.file.absolutePath)
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
    // FIX: Do NOT clamp idx to totalSlots-1.  The old code forced every icon
    // beyond the grid capacity onto the very last cell, causing them all to
    // stack on top of each other.  Instead let col/row grow naturally — extra
    // columns simply extend to the right, which is harmless (the desktop Box
    // is not scroll-limited and icons remain individually draggable).
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
    occupiedPositions: Set<Pair<Int, Int>> = emptySet()
): Offset {
    val maxCols = ((screenWidthPx - startPaddingPx) / cellWidthPx).toInt().coerceAtLeast(1)
    val maxRows = ((screenHeightPx - topPaddingPx) / cellHeightPx).toInt().coerceAtLeast(1)

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

    // Search for nearest free cell in a spiral-like scan
    for (radius in 1..(maxCols + maxRows)) {
        for (dc in -radius..radius) {
            for (dr in -radius..radius) {
                if (abs(dc) != radius && abs(dr) != radius) continue
                val c = (preferredCol + dc).coerceIn(0, maxCols - 1)
                val r = (preferredRow + dr).coerceIn(0, maxRows - 1)
                if (Pair(c, r) !in occupiedPositions) {
                    return Offset(c * cellWidthPx + startPaddingPx, r * cellHeightPx + topPaddingPx)
                }
            }
        }
    }
    // Absolute fallback: preferred position even if occupied
    return Offset(preferredCol * cellWidthPx + startPaddingPx, preferredRow * cellHeightPx + topPaddingPx)
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
private data class DefaultShortcut(val label: String, val packageName: String)

private val DEFAULT_SHORTCUTS = listOf(
    // System
    DefaultShortcut("Settings",   "com.android.settings"),
    DefaultShortcut("Phone",      "com.android.dialer"),
    DefaultShortcut("Messages",   "com.android.mms"),
    DefaultShortcut("Camera",     "com.android.camera2"),
    // Social / Entertainment
    DefaultShortcut("TikTok",     "com.zhiliaoapp.musically"),
    DefaultShortcut("WhatsApp",   "com.whatsapp"),
    DefaultShortcut("Instagram",  "com.instagram.android"),
    DefaultShortcut("YouTube",    "com.google.android.youtube"),
    // Utilities
    DefaultShortcut("Chrome",     "com.android.chrome"),
    DefaultShortcut("Maps",       "com.google.android.apps.maps"),
)

/** Creates .desktop shortcut files for installed apps on first launch. */
private fun createDefaultShortcuts(desktopDir: File, pm: PackageManager) {
    desktopDir.mkdirs()
    DEFAULT_SHORTCUTS.forEach { shortcut ->
        // Only create if the app is actually installed
        val installed = try {
            pm.getApplicationInfo(shortcut.packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }

        if (!installed) return@forEach

        val file = File(desktopDir, "${shortcut.label}.desktop")
        if (file.exists()) return@forEach          // never overwrite existing

        file.writeText("package=${shortcut.packageName}\nlabel=${shortcut.label}\n")
    }
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
    modifier: Modifier = Modifier
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
        var iconSize            by remember { mutableStateOf(prefs.iconSize) }
        var sortMode            by remember { mutableStateOf(prefs.sortMode) }
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
        val dragGroupOffsets    = remember { mutableStateMapOf<String, Offset>() }

        // ── Desktop-full toast ─────────────────────────────────────────
        var showDesktopFullToast  by remember { mutableStateOf(false) }

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
        var showWallpaperPanel  by remember { mutableStateOf(false) }

        // ── Lasso selection ──
        var selStart            by remember { mutableStateOf(Offset.Zero) }
        var selEnd              by remember { mutableStateOf(Offset.Zero) }
        var isSelecting         by remember { mutableStateOf(false) }
        var lassoActive         by remember { mutableStateOf(false) }

        val isDark = viewModel.uiState.collectAsState().value.isDarkTheme

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

        val rows = remember(screenHPxTotal, iconSize) {
            ((screenHPxTotal - padTopPx * 2) / cellHPx).toInt().coerceAtLeast(1)
        }

        val maxCols = remember(screenWPxTotal, iconSize) {
            ((screenWPxTotal - padLeftPx) / cellWPx).toInt().coerceAtLeast(1)
        }
        val maxRows = remember(screenHPxTotal, iconSize) { rows }

        // FIX: Re-clamp all saved custom positions whenever screen dimensions change
        // (e.g. on orientation flip).  Without this, icons whose saved pixel X/Y
        // are larger than the new screen size stay off-screen after rotation.
        LaunchedEffect(screenWPxTotal, screenHPxTotal) {
            val maxX = screenWPxTotal - cellWPx
            val maxY = screenHPxTotal - cellHPx
            var changed = false
            customPositions.keys.toList().forEach { id ->
                val old = customPositions[id] ?: return@forEach
                val clamped = Offset(
                    old.x.coerceIn(padLeftPx, maxX),
                    old.y.coerceIn(padTopPx, maxY)
                )
                if (clamped != old) {
                    customPositions[id] = clamped
                    changed = true
                }
            }
            if (changed) prefs.saveCustomPositions(customPositions)
        }

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
                withContext(Dispatchers.IO) { createDefaultShortcuts(desktopDir, context.packageManager) }
                prefs.defaultShortcutsCreated = true
            }
            viewModel.refreshDesktopFiles()
        }

        // Keep customPositions in sync with whatever items currently exist, and resolve
        // any pending inline-rename target once its freshly-created file appears.
        LaunchedEffect(items) {
            customPositions.keys.retainAll(items.map { it.id }.toSet())
            prefs.saveCustomPositions(customPositions)
            val pendId = pendingRenameId
            if (pendId != null) {
                val newItem = items.find { it.id == pendId }
                if (newItem != null) {
                    inlineRename = InlineRenameState(newItem.id, newItem.name)
                    selectedIds  = setOf(newItem.id)
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
        var refreshFlicker by remember { mutableStateOf(false) }
        var lastRefreshTick by remember { mutableStateOf(-1) }
        LaunchedEffect(vmUiState.desktopRefreshTick) {
            val tick = vmUiState.desktopRefreshTick
            if (lastRefreshTick != -1 && tick != lastRefreshTick) {
                refreshFlicker = true
                delay(500)
                refreshFlicker = false
            }
            lastRefreshTick = tick
        }

        val sortedItems = remember(items, sortMode, sortAscending) {
            val s = when (sortMode) {
                DesktopSortMode.NAME          -> items.sortedBy { it.name.lowercase() }
                DesktopSortMode.DATE_MODIFIED -> items.sortedBy { it.file.lastModified() }
                DesktopSortMode.TYPE          -> items.sortedBy { it.file.extension }
                DesktopSortMode.SIZE          -> items.sortedBy { it.file.length() }
            }
            if (sortAscending) s else s.reversed()
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
            target.file.renameTo(dest)
            scheduleRefresh()
        }

        // ─────────────────────────────────────────────────────────────
        // View hierarchy
        // ─────────────────────────────────────────────────────────────
        Box(modifier = modifier.fillMaxSize()) {

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
                                painter = painterResource(resId),
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

            // ── Background gesture layer — single unified detector (was two competing ones) ──
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectPressDragGestures(
                            onTap = {
                                val currentRename = inlineRename
                                if (currentRename != null) {
                                    // The live text is held in DesktopIcon; tapping outside
                                    // with no typed text means keep the initial name
                                    commitRename(currentRename, currentRename.initialName)
                                } else {
                                    selectedIds = emptySet()
                                }
                                showDesktopCtx = false
                                iconCtxTarget  = null
                            },
                            onLongPressReleased = { off ->
                                if (draggedId == null) {
                                    desktopCtxOffset = off
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
                                        val pos = customPositions[item.id]
                                            ?: autoGridPos(idx, rows, maxCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                                            ?: return@filter false  // icon has no cell (grid full)
                                        Rect(pos.x, pos.y, pos.x + cellWPx, pos.y + cellHPx).overlaps(rect)
                                    }.map { it.id }.toSet()
                                }
                            },
                            onDragCancel = { isSelecting = false; lassoActive = false }
                        )
                    }
            )

            // ── Icons layer ──
            if (showIconsOnDesktop) {
                Box(Modifier.fillMaxSize()) {

                    // FIX: pre-compute the set of occupied grid cells for overlap detection
                    // Also key on screenWPxTotal/screenHPxTotal so this recomputes on rotation.
                    val occupiedCells = remember(sortedItems, customPositions, autoArrange, rows, maxCols, screenWPxTotal, screenHPxTotal) {
                        buildSet {
                            sortedItems.forEachIndexed { idx, item ->
                                val pos = if (autoArrange) {
                                    autoGridPos(idx, rows, maxCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                                } else {
                                    customPositions[item.id]
                                        ?: autoGridPos(idx, rows, maxCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                                } ?: return@forEachIndexed  // grid full — skip icon
                                add(posToCell(pos, cellWPx, cellHPx, padLeftPx, padTopPx, maxCols, maxRows))
                            }
                        }
                    }

                    // ── Performance: O(n) cached auto-arrange positions ──
                    // Build positions once per recomposition key instead of
                    // recomputing from scratch for every icon (was O(n²)).
                    val autoArrangePositions = remember(sortedItems, autoArrange, rows, maxCols, iconSize, screenWPxTotal, screenHPxTotal) {
                        if (!autoArrange) return@remember emptyMap<String, Offset>()
                        val taken = mutableSetOf<Pair<Int, Int>>()
                        buildMap {
                            sortedItems.forEachIndexed { i, item ->
                                val p = autoGridPos(i, rows, maxCols, cellWPx, cellHPx,
                                    padLeftPx, padTopPx, taken)
                                if (p != null) {
                                    put(item.id, p)
                                    taken.add(posToCell(p, cellWPx, cellHPx, padLeftPx, padTopPx, maxCols, maxRows))
                                }
                            }
                        }
                    }

                    sortedItems.forEachIndexed { idx, item ->
                        // O(1) lookup from cached map
                        val basePos: Offset = if (autoArrange) {
                            autoArrangePositions[item.id] ?: return@forEachIndexed  // grid full → skip
                        } else {
                            customPositions[item.id]
                                ?: autoGridPos(idx, rows, maxCols, cellWPx, cellHPx, padLeftPx, padTopPx)
                                ?: return@forEachIndexed
                        }

                        var pos by remember(item.id, rows, maxCols, iconSize, autoArrange, screenWPxTotal, screenHPxTotal) {
                            mutableStateOf(if (autoArrange) basePos else customPositions[item.id] ?: basePos)
                        }

                        LaunchedEffect(autoArrange, idx, rows, maxCols, iconSize, screenWPxTotal, screenHPxTotal) {
                            if (autoArrange) pos = basePos
                        }

                        val isDragged    = draggedId == item.id
                        val isInGroup    = isDraggingGroup && item.id in selectedIds && !isDragged

                        // BUG 6 FIX: apply absolute positions broadcast by the drag anchor for group members
                        LaunchedEffect(dragGroupOffsets[item.id], isInGroup) {
                            if (isInGroup) {
                                val target = dragGroupOffsets[item.id]
                                if (target != null) pos = target
                            }
                        }

                        // Snap-back animation: animates position smoothly on grid rejection
                        val animatedPos  by animateOffsetAsState(
                            targetValue   = pos,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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
                                .pointerInput(item.id, autoArrange, selectedIds) {
                                    detectPressDragGestures(
                                        onTap = {
                                            val r = inlineRename
                                            if (r != null && r.targetId != item.id) commitRename(r, r.initialName)
                                            selectedIds    = if (item.id in selectedIds)
                                                selectedIds - item.id else setOf(item.id)
                                            showDesktopCtx = false
                                            iconCtxTarget  = null
                                        },
                                        onDoubleTap = { openItem(item) },
                                        onLongPressReleased = {
                                            val r = inlineRename
                                            if (r != null) commitRename(r, r.initialName)
                                            selectedIds    = setOf(item.id)
                                            iconCtxTarget  = item
                                            iconCtxOffset  = Offset(pos.x + cellWPx / 2, pos.y + cellHPx / 2)
                                            showDesktopCtx = false
                                        },
                                        onDragStart = {
                                            val r = inlineRename
                                            if (r != null) commitRename(r, r.initialName)
                                            draggedId = item.id
                                            // The unified detector only calls onDragStart once real
                                            // movement is confirmed, so this is already a real drag.
                                            dragMoved = true
                                            // Multi-select drag: record relative offsets of group members
                                            if (item.id in selectedIds && selectedIds.size > 1) {
                                                isDraggingGroup = true
                                                dragGroupOffsets.clear()
                                                selectedIds.filter { it != item.id }.forEach { otherId ->
                                                    val otherIdx = sortedItems.indexOfFirst { it.id == otherId }
                                                    val otherPos = customPositions[otherId]
                                                        ?: autoArrangePositions[otherId]
                                                        ?: autoGridPos(otherIdx.coerceAtLeast(0), rows, maxCols,
                                                            cellWPx, cellHPx, padLeftPx, padTopPx)
                                                        ?: return@forEach
                                                    dragGroupOffsets[otherId] = otherPos - pos
                                                }
                                            }
                                        },
                                        onDrag = { _, amt ->
                                            val maxX = screenWPxTotal - cellWPx
                                            val maxY = screenHPxTotal - cellHPx
                                            pos = Offset(
                                                (pos.x + amt.x).coerceIn(padLeftPx, maxX),
                                                (pos.y + amt.y).coerceIn(padTopPx, maxY)
                                            )
                                            // Broadcast current anchor position so group members can follow.
                                            // On drag start, dragGroupOffsets held relative offsets (member - anchor).
                                            // During drag we overwrite them with the current ABSOLUTE target
                                            // position (anchorPos + rel) so each member's LaunchedEffect can
                                            // apply it directly to its own pos state.
                                            if (isDraggingGroup) {
                                                val anchorPos = pos
                                                val relOffsets = dragGroupOffsets.toMap()
                                                relOffsets.forEach { (otherId, rel) ->
                                                    val target = Offset(
                                                        (anchorPos.x + rel.x).coerceIn(padLeftPx, screenWPxTotal - cellWPx),
                                                        (anchorPos.y + rel.y).coerceIn(padTopPx, screenHPxTotal - cellHPx)
                                                    )
                                                    dragGroupOffsets[otherId] = target
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedId = null
                                            val wasGroup = isDraggingGroup
                                            isDraggingGroup = false
                                            val maxX = screenWPxTotal - cellWPx
                                            val maxY = screenHPxTotal - cellHPx
                                            if (autoArrange) {
                                                pos = basePos  // snap-back animation plays automatically
                                            } else {
                                                val otherCells = occupiedCells - posToCell(
                                                    customPositions[item.id] ?: basePos,
                                                    cellWPx, cellHPx, padLeftPx, padTopPx, maxCols, maxRows
                                                )
                                                val snapped = snapToGrid(
                                                    pos, cellWPx, cellHPx, padLeftPx, padTopPx,
                                                    screenWPxTotal, screenHPxTotal, otherCells
                                                )
                                                @Suppress("SENSELESS_COMPARISON")
                                                if (snapped == null) {
                                                    // Grid full — animate back to origin
                                                    pos = customPositions[item.id] ?: basePos
                                                    showDesktopFullToast = true
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
                                            dragMoved = false
                                            if (!wasGroup) dragGroupOffsets.clear()
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            isDraggingGroup = false
                                            dragGroupOffsets.clear()
                                            dragMoved = false
                                            if (autoArrange) pos = basePos
                                        }
                                    )
                                }
                        ) {
                            var liveRenameText by remember { mutableStateOf("") }

                            // Group members follow anchor during drag
                            val groupDelta = if (isInGroup && dragMoved) {
                                dragGroupOffsets[item.id] ?: Offset.Zero
                            } else Offset.Zero

                            DesktopIcon(
                                item              = item,
                                isSelected        = item.id in selectedIds,
                                iconSize          = iconSize,
                                inlineRenaming    = inlineRename?.targetId == item.id,
                                initialRenameText = inlineRename?.initialName ?: item.name,
                                onLiveTextChange  = { liveRenameText = it },
                                onInlineRenameConfirm = {
                                    inlineRename?.let { r -> commitRename(r, liveRenameText) }
                                },
                                refreshFlicker    = refreshFlicker
                            )
                        }
                    }

                    // ── Desktop-full toast ────────────────────────────────
                    if (showDesktopFullToast) {
                        LaunchedEffect(Unit) {
                            delay(2500)
                            showDesktopFullToast = false
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 72.dp)
                                .background(Color(0xFF1C1C1C).copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "No space available on desktop",
                                color    = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

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
                                Icons.Default.FolderOff, null,
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

            // ── Desktop context menu ──
            if (showDesktopCtx) {
                Win11DesktopContextMenu(
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
                        showDesktopCtx = false
                    },
                    autoArrange         = autoArrange,
                    onAutoArrangeToggle = {
                        autoArrange = it; prefs.autoArrange = it
                        if (it) { customPositions.clear(); prefs.clearCustomPositions() }
                        showDesktopCtx = false
                    },
                    showIcons           = showIconsOnDesktop,
                    onShowIconsToggle   = { showIconsOnDesktop = it; prefs.showIconsOnDesktop = it; showDesktopCtx = false },
                    onRefresh           = { viewModel.refreshDesktopFiles(); showDesktopCtx = false },
                    onPaste             = {
                        viewModel.pasteClipboard(desktopDir)
                        showDesktopCtx = false
                    },
                    hasPaste            = vmUiState.clipboardFiles.isNotEmpty(),
                    onNewFolder         = {
                        val name   = uniqueName(desktopDir, "New folder")
                        val newDir = File(desktopDir, name)
                        newDir.mkdirs()
                        pendingRenameId = newDir.absolutePath
                        showDesktopCtx  = false
                        scheduleRefresh()
                    },
                    onNewTextFile       = {
                        val name    = uniqueName(desktopDir, "New Text Document", "txt")
                        val newFile = File(desktopDir, name)
                        try { newFile.createNewFile() } catch (_: Exception) {}
                        pendingRenameId = newFile.absolutePath
                        showDesktopCtx  = false
                        scheduleRefresh()
                    },
                    onNewShortcut       = { showShortcutDialog  = true; showDesktopCtx = false },
                    onAddAppShortcut    = { showAppPickerDialog = true; showDesktopCtx = false },
                    onPersonalize       = { showWallpaperPanel  = true; showDesktopCtx = false },
                    onDisplaySettings   = { viewModel.openWindow(LauncherScreen.SETTINGS); showDesktopCtx = false },
                    onDismiss           = { showDesktopCtx = false }
                )
            }

            // ── Icon context menu ──
            iconCtxTarget?.let { target ->
                Win11IconContextMenu(
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
                        f.writeText("type=app\npackage=$pkg\nlabel=$label\n")
                        showAppPickerDialog = false
                        scheduleRefresh()
                    },
                    onDismiss = { showAppPickerDialog = false }
                )
            }

            if (showPropsDialog && propsTarget != null) {
                PropertiesDialog(item = propsTarget!!, isDark = isDark, onDismiss = { showPropsDialog = false })
            }

            // ── Wallpaper / Personalise Panel ──
            if (showWallpaperPanel) {
                WallpaperPersonalisePanel(
                    isDark             = isDark,
                    viewModel          = viewModel,
                    currentMode        = wallpaperMode,
                    currentGradientIdx = gradientIndex,
                    currentImageIdx    = defaultImageIndex,
                    onModeChange       = { mode ->
                        wallpaperMode = mode
                        prefs.wallpaperMode = mode
                        // Clear custom URI if switching away from custom
                        if (mode != DesktopWallpaperMode.CUSTOM) {
                            customWallpaperUri = ""
                            prefs.customWallpaperUri = ""
                        }
                    },
                    onGradientChange   = { idx ->
                        gradientIndex = idx
                        prefs.wallpaperGradientIndex = idx
                    },
                    onImageChange      = { idx ->
                        defaultImageIndex = idx
                        prefs.wallpaperImageIndex = idx
                    },
                    onPickCustom       = { viewModel.openWallpaperPicker(WallpaperTarget.HOME) },
                    onDismiss          = { showWallpaperPanel = false }
                )
            }
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
    refreshFlicker: Boolean = false
) {
    val iconDp = iconSizeDp(iconSize).dp
    val cellW  = cellWidthDp(iconSize).dp
    val cellH  = cellHeightDp(iconSize).dp
    val focusRequester = remember { FocusRequester() }

    // Windows-style refresh effect — icons briefly vanish entirely, then reappear,
    // matching the real explorer.exe F5 behavior (not just a dip in opacity), with a
    // small random per-icon stagger so the whole desktop doesn't blink in perfect unison.
    val flickerAlpha = remember { Animatable(1f) }
    LaunchedEffect(refreshFlicker) {
        if (refreshFlicker) {
            delay((0..150).random().toLong())
            flickerAlpha.animateTo(0f, tween(60))
            delay((30..90).random().toLong())
            flickerAlpha.animateTo(1f, tween(140))
        }
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
            .graphicsLayer(alpha = flickerAlpha.value),
        contentAlignment = Alignment.TopCenter
    ) {
        // Win11-style selection: subtle blue tint + blue border glow
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
                        Icon(
                            imageVector        = getFileIcon(item.file),
                            contentDescription = null,
                            tint               = getFileIconColor(item.file),
                            modifier           = Modifier.fillMaxSize()
                        )
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
                        Icon(Icons.Default.Reply, null,
                            tint     = Color.Black,
                            modifier = Modifier.size(10.dp).graphicsLayer(scaleX = -1f))
                    }
                }

                // Audio/Video badge
                if (item.type == DesktopItemType.MUSIC_FILE || item.type == DesktopItemType.VIDEO_FILE) {
                    val badgeColor = if (item.type == DesktopItemType.MUSIC_FILE) Color(0xFFFF8C00) else Color(0xFF8764B8)
                    val badgeIcon  = if (item.type == DesktopItemType.MUSIC_FILE) Icons.Default.MusicNote else Icons.Default.PlayArrow
                    Box(
                        Modifier.size(13.dp).align(Alignment.BottomEnd).background(Color(0xFF1C1C1C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(badgeIcon, null, tint = badgeColor, modifier = Modifier.size(9.dp))
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // ── Win11 inline rename field ──
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
private fun bgAnimationLabel(type: BgAnimationType): String = when (type) {
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

private fun bgAnimationEmoji(type: BgAnimationType): String = when (type) {
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

@Composable
fun WallpaperPersonalisePanel(
    isDark: Boolean,
    viewModel: LauncherViewModel,
    currentMode: DesktopWallpaperMode,
    currentGradientIdx: Int,
    currentImageIdx: Int,
    onModeChange: (DesktopWallpaperMode) -> Unit,
    onGradientChange: (Int) -> Unit,
    onImageChange: (Int) -> Unit,
    onPickCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    val bg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val tc  = if (isDark) Color.White else Color(0xFF1A1A1A)
    val tcm = if (isDark) Color(0xFF909090) else Color(0xFF666666)
    val acc = Color(0xFF0078D4)

    val gradientNames = listOf(
        "Ocean Depth", "Midnight Blue", "Carbon", "Sunset Tricolor", "Forest Lime"
    )

    var activeTab by remember { mutableStateOf(0) }   // 0 = Background, 1 = Effects
    val effectsState by viewModel.uiState.collectAsStateWithLifecycle()
    val bgEffects = effectsState.backgroundEffects

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(12.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Palette, null, tint = acc, modifier = Modifier.size(20.dp))
                    Text("Personalise Desktop", color = tc, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Background" to 0, "Effects" to 1).forEach { (label, idx) ->
                        val selected = activeTab == idx
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(6.dp),
                            color    = if (selected) acc.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Box(
                                Modifier.clickable { activeTab = idx }.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label, fontSize = 12.sp,
                                    color = if (selected) acc else tcm,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            if (activeTab == 0) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ── Mode selector ──
                Text("Wallpaper type", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DesktopWallpaperMode.APPARENT to "Apparent",
                        DesktopWallpaperMode.DEFAULT  to "Default",
                        DesktopWallpaperMode.CUSTOM   to "Custom"
                    ).forEach { (mode, label) ->
                        val selected = currentMode == mode
                        Surface(
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(6.dp),
                            color         = if (selected) acc else (if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)),
                            border        = if (selected) null else BorderStroke(1.dp, if (isDark) Color(0xFF3A3A3A) else Color(0xFFCCCCCC))
                        ) {
                            Box(
                                Modifier
                                    .clickable { onModeChange(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color      = if (selected) Color.White else tc,
                                    fontSize   = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── Apparent gradient picker ──
                AnimatedVisibility(currentMode == DesktopWallpaperMode.APPARENT) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Colour scheme", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            wallpaperGradients.forEachIndexed { idx, gradient ->
                                val isActive = idx == currentGradientIdx
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isActive) acc.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .clickable { onGradientChange(idx) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Gradient preview swatch
                                    Box(
                                        Modifier
                                            .size(36.dp, 22.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    gradient,
                                                    start = Offset(0f, 0f),
                                                    end   = Offset(200f, 100f)
                                                )
                                            )
                                    )
                                    Text(
                                        gradientNames.getOrElse(idx) { "Preset ${idx + 1}" },
                                        color    = tc,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isActive) {
                                        Icon(Icons.Default.Check, null, tint = acc, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Default image picker ──
                AnimatedVisibility(currentMode == DesktopWallpaperMode.DEFAULT) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Wallpaper image", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        if (DEFAULT_WALLPAPERS.all { it == 0 }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Add 5 wallpapers to res/drawable\nas desktop_wp_1.png … desktop_wp_5.png",
                                    color     = tcm,
                                    fontSize  = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DEFAULT_WALLPAPERS.forEachIndexed { idx, resId ->
                                    val isActive = idx == currentImageIdx
                                    Box(
                                        Modifier
                                            .size(72.dp, 48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                width = if (isActive) 2.dp else 0.dp,
                                                color = if (isActive) acc else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onImageChange(idx) }
                                    ) {
                                        if (resId != 0) {
                                            Image(
                                                painter      = painterResource(resId),
                                                contentDescription = null,
                                                modifier     = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
                                        }
                                        if (isActive) {
                                            Box(
                                                Modifier.fillMaxSize().background(acc.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Custom wallpaper ──
                AnimatedVisibility(currentMode == DesktopWallpaperMode.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pick an image from your device", color = tcm, fontSize = 11.sp)
                        Button(
                            onClick  = { onPickCustom(); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(6.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = acc)
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Gallery…", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            } else {
                // ── Effects tab: particle animations (mix-able) + live wallpapers ──
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Background animation", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Select one or more to mix them together",
                            color = tcm.copy(alpha = 0.7f), fontSize = 10.sp
                        )
                    }

                    val liveActive = bgEffects.liveWallpaper != LiveWallpaperType.NONE
                    BgAnimationType.entries.toList().chunked(2).forEach { rowTypes ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { type ->
                                val active = type in bgEffects.activeAnimations
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(6.dp),
                                    color    = if (active) acc else (if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)),
                                    border   = if (active) null else BorderStroke(1.dp, if (isDark) Color(0xFF3A3A3A) else Color(0xFFCCCCCC))
                                ) {
                                    Row(
                                        Modifier
                                            .clickable(enabled = !liveActive) { viewModel.toggleBgAnimation(type) }
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
                                            .alpha(if (liveActive) 0.4f else 1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(bgAnimationEmoji(type), fontSize = 14.sp)
                                        Text(
                                            bgAnimationLabel(type), fontSize = 11.5.sp,
                                            color = if (active) Color.White else tc,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    if (bgEffects.activeAnimations.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearBgAnimations() }) {
                            Text("Turn off all animations", fontSize = 11.sp, color = acc)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Intensity", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = bgEffects.intensity.toFloat(),
                            onValueChange = { viewModel.setBgAnimationIntensity(it.toInt()) },
                            valueRange = 10f..100f,
                            enabled = !liveActive,
                            colors = SliderDefaults.colors(thumbColor = acc, activeTrackColor = acc)
                        )
                    }

                    Divider(color = tcm.copy(alpha = 0.15f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Live wallpaper", color = tcm, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Replaces the static wallpaper and turns off particle animations",
                            color = tcm.copy(alpha = 0.7f), fontSize = 10.sp
                        )
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "Off" tile
                        val offActive = bgEffects.liveWallpaper == LiveWallpaperType.NONE
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(72.dp, 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFEEEEEE))
                                    .border(
                                        width = if (offActive) 2.dp else 0.dp,
                                        color = if (offActive) acc else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.setLiveWallpaper(LiveWallpaperType.NONE) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = tcm, modifier = Modifier.size(16.dp))
                            }
                            Text("Off", fontSize = 10.sp, color = if (offActive) acc else tcm)
                        }
                        listOf(
                            LiveWallpaperType.AURORA to "Aurora",
                            LiveWallpaperType.NEBULA to "Nebula",
                            LiveWallpaperType.WAVES  to "Waves",
                            LiveWallpaperType.BOKEH  to "Bokeh"
                        ).forEach { (lw, label) ->
                            val isActive = bgEffects.liveWallpaper == lw
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(72.dp, 48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (isActive) 2.dp else 0.dp,
                                            color = if (isActive) acc else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.setLiveWallpaper(lw) }
                                ) {
                                    LiveWallpaperRenderer(type = lw, modifier = Modifier.fillMaxSize())
                                    if (isActive) {
                                        Box(
                                            Modifier.fillMaxSize().background(acc.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Text(label, fontSize = 10.sp, color = if (isActive) acc else tcm)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = acc)) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Win11 style Desktop Context Menu
// ─────────────────────────────────────────────────────────────────
@Composable
fun Win11DesktopContextMenu(
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
    val density  = LocalDensity.current
    val menuW    = 210
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val hoverBg  = if (isDark) Color(0x12FFFFFF) else Color(0x0F000000)
    val accent   = Color(0xFF0078D4)

    var openSub by remember { mutableStateOf<String?>(null) }

    val estMenuH = 360  // approximate height of desktop context menu
    val maxX = with(density) { (screenWidthDp - menuW - 6).dp.toPx() }
    val maxY = with(density) { (screenHeightDp - estMenuH - 6).dp.toPx() }
    val xOff = offset.x.coerceIn(6f, maxX).roundToInt()
    // BUG 7 FIX: clamp y so menu never overflows the bottom edge
    val yOff = offset.y.coerceIn(6f, maxY.coerceAtLeast(6f)).roundToInt()

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Surface(
            modifier        = Modifier.offset { IntOffset(xOff, yOff) }.width(menuW.dp),
            shape           = RoundedCornerShape(8.dp),
            color           = bg,
            shadowElevation = 16.dp,
            border          = BorderStroke(1.dp, if (isDark) Color(0xFF303030) else Color(0xFFE5E5E5))
        ) {
            Column(Modifier.padding(vertical = 5.dp)) {
                W11CtxRow(Icons.Default.ViewModule, "View", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "view") null else "view"
                }
                AnimatedVisibility(openSub == "view", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRow("Large icons",  viewMode == DesktopIconSize.LARGE,  tc, accent) { onViewChange(DesktopIconSize.LARGE) }
                        W11SubRow("Medium icons", viewMode == DesktopIconSize.MEDIUM, tc, accent) { onViewChange(DesktopIconSize.MEDIUM) }
                        W11SubRow("Small icons",  viewMode == DesktopIconSize.SMALL,  tc, accent) { onViewChange(DesktopIconSize.SMALL) }
                        W11CtxDivider(divColor)
                        W11SubRow("Auto arrange icons",  autoArrange, tc, accent) { onAutoArrangeToggle(!autoArrange) }
                        W11SubRow("Align icons to grid", true,        tc, accent) {}
                        W11CtxDivider(divColor)
                        W11SubRow("Show desktop icons", showIcons, tc, accent) { onShowIconsToggle(!showIcons) }
                    }
                }

                W11CtxRow(Icons.Default.Sort, "Sort by", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "sort") null else "sort"
                }
                AnimatedVisibility(openSub == "sort", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRow("Name",          sortMode == DesktopSortMode.NAME,          tc, accent) { onSortChange(DesktopSortMode.NAME,          sortAscending) }
                        W11SubRow("Size",          sortMode == DesktopSortMode.SIZE,          tc, accent) { onSortChange(DesktopSortMode.SIZE,          sortAscending) }
                        W11SubRow("Item type",     sortMode == DesktopSortMode.TYPE,          tc, accent) { onSortChange(DesktopSortMode.TYPE,          sortAscending) }
                        W11SubRow("Date modified", sortMode == DesktopSortMode.DATE_MODIFIED, tc, accent) { onSortChange(DesktopSortMode.DATE_MODIFIED, sortAscending) }
                        W11CtxDivider(divColor)
                        W11SubRow("Ascending",  sortAscending,  tc, accent) { onSortChange(sortMode, true) }
                        W11SubRow("Descending", !sortAscending, tc, accent) { onSortChange(sortMode, false) }
                    }
                }

                W11CtxRow(Icons.Default.Refresh,      "Refresh",          tc, tcDim) { onRefresh() }
                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.ContentPaste, "Paste",
                    if (hasPaste) tc else tcDim, tcDim, enabled = hasPaste) { onPaste() }
                W11CtxDivider(divColor)

                W11CtxRow(Icons.Default.Add, "New", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "new") null else "new"
                }
                AnimatedVisibility(openSub == "new", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(hoverBg.copy(0.04f))) {
                        W11SubRowIcon(Icons.Default.Folder,      "Folder",                     Color(0xFFFFC107), tc) { onNewFolder();       onDismiss() }
                        W11SubRowIcon(Icons.Default.Link,        "Shortcut link",              Color(0xFF0078D4), tc) { onNewShortcut();      onDismiss() }
                        W11SubRowIcon(Icons.Default.Apps,        "Add Installed App Shortcut", Color(0xFF107C10), tc) { onAddAppShortcut();   onDismiss() }
                        W11CtxDivider(divColor)
                        W11SubRowIcon(Icons.Default.Description, "Text Document",              Color(0xFF0078D4), tc) { onNewTextFile();      onDismiss() }
                    }
                }

                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Monitor, "Display settings", tc, tcDim) { onDisplaySettings() }
                W11CtxRow(Icons.Default.Palette, "Personalise",      tc, tcDim) { onPersonalize() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Win11 style Icon Context Menu
// FIX: added optional onSetAsWallpaper for image files
// ─────────────────────────────────────────────────────────────────
@Composable
fun Win11IconContextMenu(
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
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSetAsWallpaper: (() -> Unit)? = null,
    onCreateShortcut: () -> Unit,
    onProperties: () -> Unit
) {
    val density  = LocalDensity.current
    val menuW    = 220
    val estH     = if (onSetAsWallpaper != null) 400 else 360
    val bg       = if (isDark) Color(0xFA1E1E1E) else Color(0xFCEFF4F9)
    val tc       = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF999999) else Color(0xFF666666)
    val divColor = if (isDark) Color(0xFF333333) else Color(0xFFDCDCDC)
    val danger   = Color(0xFFE81123)

    var openSub by remember { mutableStateOf<String?>(null) }

    val maxX = with(density) { (screenWidthDp - menuW - 6).dp.toPx() }
    val maxY = with(density) { (screenHeightDp - estH - 6).dp.toPx() }
    val xOff = offset.x.coerceIn(6f, maxX).roundToInt()
    val yOff = offset.y.coerceIn(6f, maxY.coerceAtLeast(6f)).roundToInt()

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Surface(
            modifier        = Modifier.offset { IntOffset(xOff, yOff) }.width(menuW.dp),
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
                    W11QuickAction(Icons.Default.ContentCut,             "Cut",    tc)     { onCut();    onDismiss() }
                    W11QuickAction(Icons.Default.ContentCopy,            "Copy",   tc)     { onCopy();   onDismiss() }
                    W11QuickAction(Icons.Default.DriveFileRenameOutline, "Rename", tc)     { onRename(); onDismiss() }
                    W11QuickAction(Icons.Default.Share,                  "Share",  tc)     { onShare();  onDismiss() }
                    W11QuickAction(Icons.Default.Delete,                 "Delete", danger) { onDelete(); onDismiss() }
                }

                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.OpenInNew, "Open", tc, tcDim, isBold = true) { onOpen(); onDismiss() }
                W11CtxRow(Icons.Default.OpenWith, "Open with", tc, tcDim, hasArrow = true) {
                    openSub = if (openSub == "openwith") null else "openwith"
                }
                AnimatedVisibility(openSub == "openwith", enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.background(Color.Black.copy(0.02f))) {
                        W11SubRowIcon(Icons.Default.OpenInNew, "Choose app", tc.copy(0.8f), tc) { onOpenWith(); onDismiss() }
                    }
                }

                if (item.type == DesktopItemType.APP_SHORTCUT) {
                    W11CtxRow(Icons.Outlined.FolderOpen, "Open file location", tc, tcDim) { onOpenFileLocation(); onDismiss() }
                }

                // "Set as wallpaper" — only for image files
                if (onSetAsWallpaper != null) {
                    W11CtxDivider(divColor)
                    W11CtxRow(Icons.Default.Wallpaper, "Set as wallpaper", tc, tcDim) { onSetAsWallpaper(); onDismiss() }
                }

                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Link,   "Create shortcut", tc,     tcDim) { onCreateShortcut(); onDismiss() }
                W11CtxRow(Icons.Default.Delete, "Delete",          danger, tcDim) { onDelete();         onDismiss() }
                W11CtxDivider(divColor)
                W11CtxRow(Icons.Default.Info,   "Properties",      tc,     tcDim) { onProperties();     onDismiss() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Context menu primitives
// ─────────────────────────────────────────────────────────────────
@Composable
private fun W11CtxRow(
    icon: ImageVector,
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
        Icon(icon, null, tint = tc.copy(0.8f), modifier = Modifier.size(15.dp))
        Text(label, color = tc, fontSize = 12.5.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f), maxLines = 1)
        if (hasArrow) Icon(Icons.Default.ChevronRight, null, tint = tcDim, modifier = Modifier.size(14.dp))
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
        if (isActive) Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(13.dp))
        else          Spacer(Modifier.size(13.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1)
    }
}

@Composable
private fun W11SubRowIcon(
    icon: ImageVector,
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
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        Text(label, color = tc, fontSize = 12.5.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun W11QuickAction(
    icon: ImageVector,
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
        Icon(icon, contentDescription = tooltip, tint = tint, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.Default.Apps, null, modifier = Modifier.size(16.dp))
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
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0078D4))
                    }
                } else {
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
                Icon(getFileIcon(item.file), null, tint = getFileIconColor(item.file), modifier = Modifier.size(22.dp))
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
