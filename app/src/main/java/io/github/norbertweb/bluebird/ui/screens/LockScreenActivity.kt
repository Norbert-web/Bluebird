package io.github.norbertweb.bluebird.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import io.github.norbertweb.bluebird.ui.theme.bluebirdTheme
import io.github.norbertweb.bluebird.ui.components.FluentIcon
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector

class LockScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            bluebirdTheme(darkTheme = true) {
                LockScreenContent(onUnlock = { finish() })
            }
        }
    }
}


@Composable
fun LockScreenOverlay(onUnlock: () -> Unit) {
    LockScreenContent(onUnlock = onUnlock)
}

@Composable
fun LockScreenContent(onUnlock: () -> Unit) {
    var showPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var swipeOffset by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            currentTime = "$displayHour:${min.toString().padStart(2, '0')} $amPm"
            val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            currentDate = sdf.format(cal.time)
            delay(30_000)
        }
    }

    // Wallpaper background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF415A77),
                        Color(0xFF778DA9)
                    )
                )
            )
    ) {
        // Subtle animated overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x40000000))
        )

        if (!showPin) {
            // Lock screen main view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (swipeOffset < -80f) showPin = true
                                swipeOffset = 0f
                            },
                            onVerticalDrag = { _, delta ->
                                swipeOffset += delta
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Time
                Text(
                    text = currentTime.ifEmpty { "12:00 PM" },
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date
                Text(
                    text = currentDate.ifEmpty { "Wednesday, May 6" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(60.dp))

                // Notification badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LockScreenBadge(icon = FluentIcon.Email, count = 3, color = Color(0xFF0078D4))
                    LockScreenBadge(icon = FluentIcon.Alert, count = 5, color = Color(0xFF107C10))
                    LockScreenBadge(icon = FluentIcon.Message, count = 1, color = Color(0xFF7B2FBE))
                }

                Spacer(modifier = Modifier.height(60.dp))

                // Swipe up hint
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = FluentIcon.KeyboardArrowUp,
                        contentDescription = "Swipe up",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Swipe up to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Bottom system info
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(FluentIcon.Wifi, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Icon(FluentIcon.Battery10, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(FluentIcon.Accessibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Icon(FluentIcon.NetworkCell, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Icon(FluentIcon.Power, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // PIN entry screen
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(bluebirdColors.AccentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FluentIcon.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "User",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN dots display
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < pin.length) Color.White
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "PIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Numpad
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("⌫", "0", "→")
                )

                numRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            when (key) {
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                "→" -> {
                                                    // Any PIN works for demo
                                                    onUnlock()
                                                }
                                                else -> {
                                                    if (pin.length < 6) {
                                                        pin += key
                                                        if (pin.length == 6) {
                                                            onUnlock()
                                                        }
                                                    }
                                                }
                                            }
                                        })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showPin = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun LockScreenBadge(
    icon: ImageVector,
    count: Int,
    color: Color
) {
    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .background(color, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
        }
    }
}
