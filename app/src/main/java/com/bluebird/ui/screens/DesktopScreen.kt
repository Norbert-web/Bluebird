package com.bluebird.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow // Crucial import for .shadow() modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bluebird.LauncherViewModel
import com.bluebird.WallpaperTarget
import com.bluebird.PowerAction // Imported enum type safely
import com.bluebird.ui.components.*
import com.bluebird.ui.components.wallpaperGradients
import com.bluebird.ui.theme.Win11Colors

@Composable
fun DesktopScreen(viewModel: LauncherViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setCustomWallpaper(it.toString(), uiState.wallpaperPickerTarget, context)
            viewModel.closeWallpaperPicker()
        }
    }

    if (uiState.isLocked) {
        LockScreenOverlay(
            wallpaper = uiState.wallpaper,
            userProfile = uiState.userProfile,
            onUnlock = { viewModel.unlockScreen() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { viewModel.toggleTaskbar() },
                    onTap = {
                        if (uiState.isStartMenuOpen || uiState.isActionCenterOpen ||
                            uiState.isSearchOpen || uiState.isWidgetsOpen || uiState.isPowerMenuOpen ||
                            uiState.isDesktopContextMenuOpen
                        ) {
                            viewModel.dismissAllOverlays()
                        }
                    }
                )
            }
    ) {
        // ── 1. Desktop ──
        Desktop(
            wallpaper = uiState.wallpaper,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Windows ──
        WindowManager(
            windows = uiState.openWindows,
            activeWindowId = uiState.activeWindowId,
            isDark = uiState.isDarkTheme,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (uiState.isTaskbarVisible) 48.dp else 0.dp)
        )

        // ── 3. Scrim ──
        val anyOverlay = uiState.isStartMenuOpen || uiState.isActionCenterOpen ||
                uiState.isSearchOpen || uiState.isWidgetsOpen || uiState.isPowerMenuOpen
        AnimatedVisibility(
            visible = anyOverlay,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000))
                    .pointerInput(Unit) { detectTapGestures(onTap = { viewModel.dismissAllOverlays() }) }
            )
        }

        // ── 4. Widgets ──
        AnimatedVisibility(
            visible = uiState.isWidgetsOpen,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), initialOffsetX = { -it }),
            exit = slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), targetOffsetX = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = if (uiState.isTaskbarVisible) 48.dp else 0.dp)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            ) {
                WidgetsPanel(uiState = uiState)
            }
        }

        // ── 5. Start Menu ──
        AnimatedVisibility(
            visible = uiState.isStartMenuOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), initialOffsetY = { it / 3 }) + fadeIn(tween(200)),
            exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), targetOffsetY = { it / 3 }) + fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = if (uiState.isTaskbarVisible) 54.dp else 8.dp)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            ) {
                StartMenu(uiState = uiState, viewModel = viewModel)
            }
        }

        // ── 6. Power Menu ──
        AnimatedVisibility(
            visible = uiState.isPowerMenuOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(tween(150)),
            exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(100))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = if (uiState.isTaskbarVisible) 58.dp else 12.dp)
                    .padding(end = 160.dp)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            ) {
                PowerMenu(
                    isDark = uiState.isDarkTheme,
                    onAction = { action ->
                        viewModel.dismissAllOverlays()
                        viewModel.performPowerAction(context, action)
                    }
                )
            }
        }

        // ── 7. Action Center ──
        AnimatedVisibility(
            visible = uiState.isActionCenterOpen,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), initialOffsetY = { it / 3 }) + fadeIn(tween(200)),
            exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), targetOffsetY = { it / 3 }) + fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = if (uiState.isTaskbarVisible) 54.dp else 8.dp)
                    .padding(end = 8.dp)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            ) {
                ActionCenter(uiState = uiState, viewModel = viewModel)
            }
        }

        // ── 8. Search ──
        AnimatedVisibility(
            visible = uiState.isSearchOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow), initialOffsetY = { it / 3 }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = if (uiState.isTaskbarVisible) 54.dp else 8.dp)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            ) {
                SearchOverlay(uiState = uiState, viewModel = viewModel)
            }
        }

        // ── 9. Taskbar ──
        AnimatedVisibility(
            visible = uiState.isTaskbarVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Win11Taskbar(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        }

        // ── 10. Wallpaper Picker Dialog ──
        if (uiState.isWallpaperPickerOpen) {
            WallpaperPickerDialog(
                target = uiState.wallpaperPickerTarget,
                currentWallpaper = uiState.wallpaper,
                isDark = uiState.isDarkTheme,
                onSelectBuiltIn = { index -> viewModel.setBuiltInWallpaper(index, uiState.wallpaperPickerTarget) },
                onBrowse = { wallpaperPicker.launch("image/*") },
                onDismiss = { viewModel.closeWallpaperPicker() }
            )
        }
    }
}

// ── 11. Custom Windows 11 Power Menu Layout ──
@Composable
fun PowerMenu(
    isDark: Boolean,
    onAction: (PowerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) Color(0xFF252B32) else Color(0xFFE8ECF0)
    val borderColor = if (isDark) Color(0xFF373E47) else Color(0xFFCDD5DF)
    val textColor = if (isDark) Color.White else Color.Black

    Surface(
        modifier = modifier
            .width(140.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(8.dp)), // Resolved with explicit import
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            PowerMenuItem(
                label = "Sleep",
                icon = Icons.Default.BedtimeOff,
                textColor = textColor,
                onClick = { onAction(PowerAction.SLEEP) }
            )
            PowerMenuItem(
                label = "Shut down",
                icon = Icons.Default.PowerSettingsNew,
                textColor = textColor,
                onClick = { onAction(PowerAction.SHUTDOWN) } // Corrected enum variable mapping name
            )
            PowerMenuItem(
                label = "Restart",
                icon = Icons.Default.RestartAlt,
                textColor = textColor,
                onClick = { onAction(PowerAction.RESTART) }
            )
        }
    }
}

@Composable
private fun PowerMenuItem(
    label: String,
    icon: ImageVector,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun WallpaperPickerDialog(
    target: WallpaperTarget,
    currentWallpaper: com.bluebird.WallpaperState,
    isDark: Boolean,
    onSelectBuiltIn: (Int) -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (target == WallpaperTarget.LOCK_SCREEN) Icons.Default.Lock else Icons.Default.Wallpaper,
                    null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(20.dp)
                )
                Text(if (target == WallpaperTarget.LOCK_SCREEN) "Lock Screen Wallpaper" else "Desktop Wallpaper", fontWeight = FontWeight.Medium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose a gradient theme:", style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.7f))

                val names = listOf("Blue Bloom", "Sunset Purple", "Forest Green", "Deep Space", "Aurora")
                wallpaperGradients.forEachIndexed { index, gradient ->
                    val isSelected = if (target == WallpaperTarget.HOME)
                        currentWallpaper.homeWallpaperIndex == index && currentWallpaper.homeWallpaperUri.isEmpty()
                    else
                        currentWallpaper.lockWallpaperIndex == index && currentWallpaper.lockWallpaperUri.isEmpty()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(if (isSelected) 2.dp else 0.dp, Win11Colors.AccentBlue, RoundedCornerShape(8.dp))
                            .clickable { onSelectBuiltIn(index) }
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp, 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(names[index], color = textColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }

                Divider(color = textColor.copy(alpha = 0.1f))
                Text("Or choose a custom photo:", style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.7f))
                OutlinedButton(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Win11Colors.AccentBlue),
                    border = BorderStroke(1.dp, Win11Colors.AccentBlue)
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Gallery")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun LockScreenOverlay(
    wallpaper: com.bluebird.WallpaperState,
    userProfile: com.bluebird.UserProfile,
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onUnlock() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Wallpaper
        if (wallpaper.lockWallpaperUri.isNotEmpty()) {
            AsyncImage(
                model = Uri.parse(wallpaper.lockWallpaperUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val gradient = wallpaperGradients[wallpaper.lockWallpaperIndex % wallpaperGradients.size]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            gradient,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(2000f, 1200f)
                        )
                    )
            )
        }

        // Overlay
        Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)))

        // Lock screen content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val cal = remember { java.util.Calendar.getInstance() }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val min = cal.get(java.util.Calendar.MINUTE)
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            val sdf = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())

            Text(
                "$displayHour:${min.toString().padStart(2, '0')} $amPm",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Light
            )
            Text(sdf.format(cal.time), color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)

            Spacer(Modifier.height(24.dp))

            if (userProfile.profilePicturePath.isNotEmpty()) {
                AsyncImage(
                    model = Uri.parse(userProfile.profilePicturePath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Win11Colors.AccentBlue.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userProfile.userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(userProfile.userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Tap anywhere to unlock", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)

            Spacer(Modifier.height(8.dp))
            Icon(Icons.Default.LockOpen, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
        }
    }
}