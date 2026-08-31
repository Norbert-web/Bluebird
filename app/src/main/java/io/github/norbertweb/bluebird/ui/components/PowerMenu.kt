package io.github.norbertweb.bluebird.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

// ─────────────────────────────────────────────────────────
// POWER MENU — extracted out of StartMenu.kt's BottomUserBar so it can be
// reused (e.g. from Mobile Home or the lock screen) without duplicating
// the sleep/restart/shutdown reflection calls.
// ─────────────────────────────────────────────────────────
@Composable
fun PowerMenuButton(isDark: Boolean, textPrimary: androidx.compose.ui.graphics.Color) {
    val context = LocalContext.current
    var showPowerMenu by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(DS.sectionCorner))
                .background(if (showPowerMenu) DS.accentStart.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                .clickable { showPowerMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FluentIcon.Power,
                contentDescription = "Power options",
                tint = if (showPowerMenu) DS.accentStart else textPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = showPowerMenu,
            onDismissRequest = { showPowerMenu = false },
            modifier = Modifier
                .background(if (isDark) DS.surfaceDark else DS.glassLight, RoundedCornerShape(DS.sectionCorner))
                .border(1.dp, if (isDark) DS.borderDark else DS.borderLight, RoundedCornerShape(DS.sectionCorner))
        ) {
            PowerMenuItem(label = "Sleep", icon = FluentIcon.SleepArrow, isDark = isDark) {
                showPowerMenu = false
                // Locking the screen is the closest a normal app can get to "sleep" —
                // PowerManager.goToSleep() is a hidden/system-only API and isn't in the
                // public SDK. This requires the app to be a device/profile owner; it's
                // a safe no-op (caught below) otherwise.
                runCatching {
                    (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager)
                        ?.lockNow()
                }
            }
            PowerMenuItem(label = "Restart", icon = FluentIcon.ArrowSync, isDark = isDark) {
                showPowerMenu = false
                // PowerManager.reboot() is public API but needs the signature-level
                // android.permission.REBOOT, normally granted only to system apps.
                runCatching {
                    (context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.reboot(null)
                }
            }
            PowerMenuItem(label = "Shut Down", icon = FluentIcon.Power, isDark = isDark) {
                showPowerMenu = false
                // There is no public SDK call to power off the device from an app —
                // Intent.ACTION_REQUEST_SHUTDOWN is a hidden/system-only constant, not
                // part of the public Intent API. This reaches PowerManager's hidden
                // shutdown(...) via reflection instead, which only succeeds on a
                // system-signed / privileged build; it's a safe no-op elsewhere.
                runCatching {
                    val pm = context.getSystemService(Context.POWER_SERVICE)
                    pm?.javaClass
                        ?.getMethod(
                            "shutdown",
                            Boolean::class.javaPrimitiveType,
                            String::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                        ?.invoke(pm, false, "userrequested", false)
                }
            }
        }
    }
}

@Composable
private fun PowerMenuItem(label: String, icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val iconTint = if (label == "Shut Down") DS.badgeRed else DS.accentStart

    Row(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(DS.chipCorner))
            .background(if (pressed) (if (isDark) DS.pressedDark else DS.pressedLight) else androidx.compose.ui.graphics.Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 12.sp, color = textColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal)
    }
}
