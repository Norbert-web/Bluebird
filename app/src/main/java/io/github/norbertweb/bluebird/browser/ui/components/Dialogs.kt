package com.io.github.norbertweb.bluebird.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.io.github.norbertweb.bluebird.browser.model.JsDialogState
import com.io.github.norbertweb.bluebird.browser.model.JsDialogType
import com.io.github.norbertweb.bluebird.browser.model.SslDialogState

// ═══════════════════════════════════════════════════════════════════════
// JsDialogComposable — alert / confirm / prompt
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun JsDialog(
    state: JsDialogState,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent    = Color(0xFF1A73E8)

    var promptText by remember { mutableStateOf(state.defaultValue) }

    Dialog(
        onDismissRequest = state.onDismiss,
        properties       = DialogProperties(dismissOnClickOutside = state.type == JsDialogType.ALERT)
    ) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        when (state.type) {
                            JsDialogType.ALERT   -> Icons.Default.Info
                            JsDialogType.CONFIRM -> Icons.Default.Warning
                            JsDialogType.PROMPT  -> Icons.Default.Edit
                        },
                        null,
                        tint     = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        when (state.type) {
                            JsDialogType.ALERT   -> "Page says"
                            JsDialogType.CONFIRM -> "Confirm"
                            JsDialogType.PROMPT  -> "Input required"
                        },
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = textColor
                    )
                }

                // Message
                Text(state.message, fontSize = 13.sp, color = textColor.copy(0.8f), lineHeight = 18.sp)

                // Prompt input
                if (state.type == JsDialogType.PROMPT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value         = promptText,
                            onValueChange = { promptText = it },
                            textStyle     = TextStyle(color = textColor, fontSize = 13.sp),
                            cursorBrush   = SolidColor(accent),
                            modifier      = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (state.type != JsDialogType.ALERT) {
                        TextButton(onClick = state.onDismiss) {
                            Text("Cancel", color = textColor.copy(0.6f), fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = {
                            when (state.type) {
                                JsDialogType.ALERT   -> state.onConfirm("ok")
                                JsDialogType.CONFIRM -> state.onConfirm("true")
                                JsDialogType.PROMPT  -> state.onConfirm(promptText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(if (state.type == JsDialogType.ALERT) "OK" else "OK", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SSL Warning Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SslWarningDialog(
    state: SslDialogState,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val red       = Color(0xFFD32F2F)

    Dialog(onDismissRequest = state.onCancel) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Warning, null, tint = red, modifier = Modifier.size(24.dp))
                    Text("Security Warning", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = red)
                }

                Text(
                    "Your connection to ${state.host} is not secure.",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor
                )

                Text(state.errorDescription, fontSize = 12.sp, color = textColor.copy(0.7f), lineHeight = 17.sp)

                Text(
                    "Attackers might be trying to steal your information. " +
                    "It is not recommended to proceed.",
                    fontSize = 12.sp, color = textColor.copy(0.6f), lineHeight = 17.sp
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = state.onProceed) {
                        Text("Proceed anyway", color = red.copy(0.7f), fontSize = 12.sp)
                    }
                    Button(
                        onClick = state.onCancel,
                        colors  = ButtonDefaults.buttonColors(containerColor = red)
                    ) {
                        Text("Go back", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Permission Request Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PermissionRequestDialog(
    request: com.io.github.norbertweb.bluebird.browser.model.PermissionRequest,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent    = Color(0xFF1A73E8)

    Dialog(onDismissRequest = request.deny) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("Permission Request", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }

                Text(
                    "${request.origin} wants to access:",
                    fontSize = 13.sp, color = textColor.copy(0.8f)
                )

                request.resources.forEach { res ->
                    val (icon, label) = when (res) {
                        android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Icons.Default.Settings to "Camera"
                        android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Icons.Default.Settings to "Microphone"
                        else -> Icons.Default.Settings to res
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                        Text(label, fontSize = 12.sp, color = textColor)
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = request.deny) {
                        Text("Deny", color = textColor.copy(0.5f), fontSize = 13.sp)
                    }
                    Button(
                        onClick = request.grant,
                        colors  = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Allow", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Geolocation Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun GeolocationDialog(
    origin: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent    = Color(0xFF1A73E8)

    Dialog(onDismissRequest = onDeny) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("Location Access", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }
                Text("$origin wants to access your location.", fontSize = 13.sp, color = textColor.copy(0.8f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDeny) { Text("Deny", color = textColor.copy(0.5f), fontSize = 13.sp) }
                    Button(onClick = onAllow, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Allow", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
