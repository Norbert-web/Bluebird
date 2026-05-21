package com.win11launcher.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import com.win11launcher.LauncherUiState
import com.win11launcher.LauncherViewModel
import com.win11launcher.ui.theme.Win11Colors

@Composable
fun ActionCenter(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AcrylicSurface(
        modifier = modifier
            .width(360.dp)
            .wrapContentHeight(),
        isDark = uiState.isDarkTheme,
        alpha = 0.95f,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Quick Toggles Grid
            Text(
                text = "Quick settings",
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.isDarkTheme) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickToggle(
                    label = if (uiState.isWifiOn) "Wi-Fi On" else "Wi-Fi Off",
                    icon = {
                        Icon(
                            imageVector = if (uiState.isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Wifi",
                            tint = if (uiState.isWifiOn) Win11Colors.AccentBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isActive = uiState.isWifiOn,
                    onClick = { viewModel.openWifiSettings(context) },
                    isDark = uiState.isDarkTheme
                )

                QuickToggle(
                    label = if (uiState.isBluetoothOn) "BT On" else "Bluetooth",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = if (uiState.isBluetoothOn) Win11Colors.AccentBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isActive = uiState.isBluetoothOn,
                    onClick = { viewModel.openBluetoothSettings(context) },
                    isDark = uiState.isDarkTheme
                )

                QuickToggle(
                    label = if (uiState.isAirplaneMode) "Airplane On" else "Airplane",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AirplanemodeActive,
                            contentDescription = "Airplane",
                            tint = if (uiState.isAirplaneMode) Win11Colors.AccentBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isActive = uiState.isAirplaneMode,
                    onClick = { viewModel.toggleAirplaneMode() },
                    isDark = uiState.isDarkTheme
                )

                QuickToggle(
                    label = if (uiState.isDarkTheme) "Dark" else "Light",
                    icon = {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = Win11Colors.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isActive = uiState.isDarkTheme,
                    onClick = { viewModel.toggleTheme() },
                    isDark = uiState.isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Slider
            val textColor = if (uiState.isDarkTheme) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

            Text(
                text = "Volume",
                style = MaterialTheme.typography.labelLarge,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SliderRow(
                label = "Volume",
                value = uiState.volume,
                onValueChange = { viewModel.setVolume(it, context) },
                leadingIcon = {
                    Icon(
                        imageVector = when {
                            uiState.volume < 0.01f -> Icons.Default.VolumeOff
                            uiState.volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                isDark = uiState.isDarkTheme
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brightness Slider
            Text(
                text = "Brightness",
                style = MaterialTheme.typography.labelLarge,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SliderRow(
                label = "Brightness",
                value = uiState.brightness,
                onValueChange = { viewModel.setBrightness(it, context) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Brightness",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                isDark = uiState.isDarkTheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Battery indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.batteryLevel}% — Plugged in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    LinearProgressIndicator(
                        progress = { uiState.batteryLevel / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Win11Colors.Success,
                        trackColor = if (uiState.isDarkTheme) Color(0xFF3C3C3C) else Color(0xFFE0E0E0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                if (uiState.notifications.isNotEmpty()) {
                    TextButton(onClick = {
                        uiState.notifications.forEach { viewModel.dismissNotification(it.id) }
                    }) {
                        Text("Clear all", color = Win11Colors.AccentBlue, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No new notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                uiState.notifications.forEach { notification ->
                    NotificationCard(
                        notification = notification,
                        isDark = uiState.isDarkTheme,
                        onDismiss = { viewModel.dismissNotification(notification.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: com.win11launcher.RealNotification,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFFEEEEEE)
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = Win11Colors.AccentBlue,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = run {
                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(notification.time))
                },
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
