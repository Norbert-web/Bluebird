package io.github.norbertweb.bluebird.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.ui.theme.Win11Colors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    cornerRadius: Dp = 12.dp,
    blur: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isDark) Win11Colors.GlassDark else Win11Colors.GlassLight
    val borderColor = if (isDark) Win11Colors.GlassBorderDark else Win11Colors.GlassBorderLight

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

@Composable
fun AcrylicSurface(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    alpha: Float = 0.85f,
    cornerRadius: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val baseColor = if (isDark)
        Color(0xFF202020).copy(alpha = alpha)
    else
        Color(0xFFF5F5F5).copy(alpha = alpha)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color(0x25FFFFFF) else Color(0x25000000),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

@Composable
fun Win11Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = if (isPressed)
        Win11Colors.AccentBlue.copy(alpha = 0.3f)
    else if (isDark)
        Win11Colors.HoverBg
    else
        Win11Colors.HoverBgLight

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun TaskbarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isHovered: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isActive -> Win11Colors.AccentBlue.copy(alpha = 0.3f)
                    hovered -> Win11Colors.HoverBg
                    else -> Color.Transparent
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
        content = content
    )

    // Active indicator line
    if (isActive) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .background(
                    Win11Colors.AccentBlue,
                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                )
        )
    }
}

@Composable
fun WindowTitleBar(
    title: String,
    isDark: Boolean = true,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val btnHover = if (isDark) Color(0x20FFFFFF) else Color(0x20000000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFEFEFEF)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Window controls
        listOf(
            Triple(Icons.Default.Minimize, "Minimize", onMinimize),
            Triple(Icons.Default.CropSquare, "Maximize", onMaximize),
            Triple(Icons.Default.Close, "Close", onClose)
        ).forEachIndexed { index, (icon, desc, action) ->
            var hovered by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(46.dp, 32.dp)
                    .background(
                        if (hovered && index == 2) Win11Colors.Error
                        else if (hovered) btnHover
                        else Color.Transparent
                    )
                    .pointerInput(Unit) { detectTapGestures(onTap = { action() }) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    tint = if (hovered && index == 2) Color.White else textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickToggle(
    label: String,
    icon: @Composable () -> Unit,
    isActive: Boolean,
    onClick: () -> Unit,
    isDark: Boolean = true
) {
    val bgColor = if (isActive) Win11Colors.AccentBlue.copy(alpha = 0.25f)
    else if (isDark) Color(0xFF3C3C3C) else Color(0xFFE5E5E5)

    val borderColor = if (isActive) Win11Colors.AccentBlue.copy(alpha = 0.6f)
    else if (isDark) Color(0x20FFFFFF) else Color(0x20000000)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(8.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    leadingIcon: @Composable () -> Unit,
    isDark: Boolean = true
) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        leadingIcon()
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Win11Colors.AccentBlue,
                activeTrackColor = Win11Colors.AccentBlue
            )
        )
    }
}

@Composable
fun AppIconSmall(
    drawable: android.graphics.drawable.Drawable?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Win11Colors.AccentBlue, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contentDescription.firstOrNull()?.toString() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
