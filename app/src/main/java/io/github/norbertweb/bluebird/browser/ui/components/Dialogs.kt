package io.github.norbertweb.bluebird.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import io.github.norbertweb.bluebird.browser.model.JsDialogState
import io.github.norbertweb.bluebird.browser.model.JsDialogType
import io.github.norbertweb.bluebird.browser.model.SslDialogState
import io.github.norbertweb.bluebird.browser.model.StoredPermissionDecision

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
                    FluentIcon(
                        when (state.type) {
                            JsDialogType.ALERT   -> FluentIcons.Info
                            JsDialogType.CONFIRM -> FluentIcons.Warning
                            JsDialogType.PROMPT  -> FluentIcons.Edit
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
                    FluentIcon(FluentIcons.Warning, null, tint = red, modifier = Modifier.size(24.dp))
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
    request: io.github.norbertweb.bluebird.browser.model.PermissionRequest,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent    = Color(0xFF1A73E8)

    var remember by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = request.deny) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FluentIcon(FluentIcons.Security, null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("Permission Request", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }

                Text(
                    "${request.origin} wants to access:",
                    fontSize = 13.sp, color = textColor.copy(0.8f)
                )

                request.resources.forEach { res ->
                    val (icon, label) = when (res) {
                        android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> FluentIcons.Settings to "Camera"
                        android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> FluentIcons.Settings to "Microphone"
                        else -> FluentIcons.Settings to res
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FluentIcon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                        Text(label, fontSize = 12.sp, color = textColor)
                    }
                }

                if (request.remember != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = remember, onCheckedChange = { remember = it }, colors = CheckboxDefaults.colors(checkedColor = accent))
                        Text("Remember this choice for this site", fontSize = 11.sp, color = textColor.copy(.75f))
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = {
                        if (remember) request.remember?.invoke(StoredPermissionDecision.DENY)
                        request.deny()
                    }) {
                        Text("Deny", color = textColor.copy(0.5f), fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            if (remember) request.remember?.invoke(StoredPermissionDecision.ALLOW)
                            request.grant()
                        },
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
    onRemember: ((Boolean) -> Unit)? = null,
    isDark: Boolean
) {
    val bg        = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent    = Color(0xFF1A73E8)

    var remember by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDeny) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FluentIcon(FluentIcons.LocationOn, null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("Location Access", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                }
                Text("$origin wants to access your location.", fontSize = 13.sp, color = textColor.copy(0.8f))
                if (onRemember != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = remember, onCheckedChange = { remember = it }, colors = CheckboxDefaults.colors(checkedColor = accent))
                        Text("Remember this choice for this site", fontSize = 11.sp, color = textColor.copy(.75f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = { onRemember?.invoke(false); onDeny() }) { Text("Deny", color = textColor.copy(0.5f), fontSize = 13.sp) }
                    Button(onClick = { onRemember?.invoke(remember); onAllow() }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Allow", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClearBrowsingDataDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onClear: (Set<io.github.norbertweb.bluebird.browser.model.ClearDataOption>) -> Unit
) {
    val bg = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val accent = Color(0xFF1A73E8)
    var selected by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            setOf(
                io.github.norbertweb.bluebird.browser.model.ClearDataOption.HISTORY,
                io.github.norbertweb.bluebird.browser.model.ClearDataOption.CACHE
            )
        )
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bg,
        title = { Text("Clear browsing data", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Choose what Bluebird should remove.", color = textColor.copy(.65f), fontSize = 12.sp)
                val labels = listOf(
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.HISTORY to "Browsing history",
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.COOKIES to "Cookies",
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.CACHE to "Cached files and images",
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.SITE_STORAGE to "Site storage",
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.FORM_DATA to "Saved form data",
                    io.github.norbertweb.bluebird.browser.model.ClearDataOption.DOWNLOADS to "Download records"
                )
                labels.forEach { (option, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = if (option in selected) selected - option else selected + option
                        }.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = option in selected,
                            onCheckedChange = { checked -> selected = if (checked) selected + option else selected - option },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = accent)
                        )
                        Text(label, color = textColor, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClear(selected) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("Clear now", fontSize = 12.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = textColor.copy(.65f), fontSize = 12.sp) }
        }
    )
}


@Composable
fun SavePasswordDialog(
    origin: String,
    username: String?,
    isUpdate: Boolean,
    isDark: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val bg = if (isDark) Color(0xFF2C2C2C) else Color.White
    val text = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val muted = if (isDark) Color(0xFFB8B8B8) else Color(0xFF666666)
    val accent = Color(0xFF1A73E8)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FluentIcon(FluentIcons.LockClosed, null, tint = accent, modifier = Modifier.size(20.dp))
                Text(if (isUpdate) "Update saved password?" else "Save password?", color = text, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(origin, color = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (!username.isNullOrBlank()) Text(username, color = muted, fontSize = 12.sp)
                Text(
                    if (isUpdate) "Bluebird found a different password for this account." else "Save this sign-in securely in Bluebird Password Manager?",
                    color = muted, fontSize = 12.sp, lineHeight = 17.sp
                )
                Text("Passwords are encrypted with Android Keystore.", color = muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text(if (isUpdate) "Update" else "Save", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now", color = muted, fontSize = 13.sp) }
        }
    )
}

@Composable
fun CredentialPickerDialog(
    origin: String,
    credentials: List<io.github.norbertweb.bluebird.browser.security.StoredCredential>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelect: (io.github.norbertweb.bluebird.browser.security.StoredCredential) -> Unit
) {
    val bg = if (isDark) Color(0xFF2C2C2C) else Color.White
    val text = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val muted = text.copy(alpha = 0.58f)
    val accent = Color(0xFF1A73E8)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FluentIcon(FluentIcons.LockClosed, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Choose a saved password", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = text)
                        Text(origin, fontSize = 10.sp, color = muted, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (credentials.isEmpty()) {
                    Text("No saved credentials for this site.", fontSize = 12.sp, color = muted)
                } else {
                    credentials.forEach { credential ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelect(credential) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FluentIcon(FluentIcons.Lock, null, tint = text.copy(.65f), modifier = Modifier.size(17.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                                Text(credential.username, fontSize = 12.sp, color = text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(credential.nickname.ifBlank { origin }, fontSize = 9.sp, color = muted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            FluentIcon(FluentIcons.ArrowForward, null, tint = muted, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel", color = accent, fontSize = 11.sp) }
            }
        }
    }
}
