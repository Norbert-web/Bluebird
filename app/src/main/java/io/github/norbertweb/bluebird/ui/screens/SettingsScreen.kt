package io.github.norbertweb.bluebird.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.norbertweb.bluebird.AppTheme
import io.github.norbertweb.bluebird.LauncherUiState
import io.github.norbertweb.bluebird.LauncherViewModel
import io.github.norbertweb.bluebird.WallpaperTarget
import io.github.norbertweb.bluebird.ui.components.wallpaperGradients
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import com.google.accompanist.drawablepainter.DrawablePainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────────────────
// CATEGORY ENUM — replaces raw strings to prevent silent routing bugs
// ─────────────────────────────────────────────────────────────────────────────

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    SYSTEM          ("System",              Icons.Default.Settings),
    SOUND           ("Sound",               Icons.Default.VolumeUp),
    BLUETOOTH       ("Bluetooth & devices", Icons.Default.Bluetooth),
    NETWORK         ("Network & internet",  Icons.Default.Wifi),
    APPEARANCE      ("Appearance",          Icons.Default.Palette),
    GESTURES        ("Gestures",            Icons.Default.SwipeRight),
    APPS            ("Apps",                Icons.Default.Apps),
    ACCOUNTS        ("Accounts",            Icons.Default.AccountCircle),
    TIME_LANGUAGE   ("Time & language",     Icons.Default.Language),
    GAMING          ("Gaming",              Icons.Default.SportsEsports),
    ACCESSIBILITY   ("Accessibility",       Icons.Default.Accessible),
    PRIVACY         ("Privacy & security",  Icons.Default.Lock),
    BACKUP          ("Backup & restore",    Icons.Default.Backup),
    SEARCH          ("Search",              Icons.Default.Search),
    UPDATE          ("Launcher Update",     Icons.Default.Update),
    ABOUT           ("About",               Icons.Default.Info)
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(isDark: Boolean, viewModel: LauncherViewModel? = null) {
    val context  = LocalContext.current
    val uiState  = viewModel?.uiState?.collectAsState()?.value

    val effectiveDark  = uiState?.isDarkTheme ?: isDark
    val textColor      = if (effectiveDark) bluebirdColors.TextPrimary      else bluebirdColors.TextPrimaryLight
    val bgColor        = if (effectiveDark) Color(0xFF1C1C1C)            else Color(0xFFFAFAFA)
    val surfaceBg      = if (effectiveDark) Color(0xFF2C2C2C)            else Color(0xFFEEEEEE)
    val navBg          = if (effectiveDark) Color(0xFF1A1A1A)            else Color(0xFFF0F0F0)

    val (resolvedBg, resolvedNav, resolvedSurface, resolvedText) = when (uiState?.appTheme) {
        AppTheme.SPECIAL -> listOf(Color(0xFF0E0820), Color(0xFF130A2E), Color(0xFF1C1040), Color(0xFFE8DEFF))
        else             -> listOf(bgColor, navBg, surfaceBg, textColor)
    }
    val specialAccent  = if (uiState?.appTheme == AppTheme.SPECIAL) Color(0xFF9C6BF7) else bluebirdColors.AccentBlue

    var selectedCategory by remember { mutableStateOf(SettingsCategory.SYSTEM) }

    Row(modifier = Modifier.fillMaxSize()) {

        // ── Left nav pane ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(resolvedNav)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (uiState != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedCategory = SettingsCategory.ACCOUNTS }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(44.dp)) {
                        if (uiState.userProfile.profilePicturePath.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(uiState.userProfile.profilePicturePath),
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(specialAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.userProfile.userName,
                            style      = MaterialTheme.typography.titleSmall,
                            color      = resolvedText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = (13 * uiState.textScale).sp,
                            maxLines   = 1, overflow = TextOverflow.Ellipsis)
                        Text("Local Account",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = resolvedText.copy(alpha = 0.5f))
                    }
                    Icon(Icons.Default.ChevronRight, null,
                        tint = resolvedText.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp))
                }
                Divider(color = resolvedText.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 8.dp))
            }

            Spacer(Modifier.height(4.dp))

            SettingsCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) specialAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(cat.icon, null,
                        tint     = if (isSelected) specialAccent else resolvedText.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp))
                    Text(cat.label,
                        color      = if (isSelected) specialAccent else resolvedText,
                        fontSize   = (13 * (uiState?.textScale ?: 1f)).sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines   = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ── Right content area ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(resolvedBg)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(selectedCategory.label,
                style      = MaterialTheme.typography.headlineSmall,
                color      = resolvedText,
                fontWeight = FontWeight.Light,
                fontSize   = (22 * (uiState?.textScale ?: 1f)).sp)
            Spacer(Modifier.height(4.dp))

            val args = ScreenArgs(effectiveDark, resolvedText, resolvedSurface, specialAccent, uiState, viewModel, context)

            when (selectedCategory) {
                SettingsCategory.SYSTEM       -> SystemSettings(args)
                SettingsCategory.SOUND        -> SoundSettings(args)
                SettingsCategory.BLUETOOTH    -> BluetoothSettings(args)
                SettingsCategory.NETWORK      -> NetworkSettings(args)
                SettingsCategory.APPEARANCE   -> AppearanceSettings(args)
                SettingsCategory.GESTURES     -> GestureSettings(args)
                SettingsCategory.APPS         -> AppsSettings(args)
                SettingsCategory.ACCOUNTS     -> AccountsSettings(args)
                SettingsCategory.TIME_LANGUAGE-> TimeLanguageSettings(args)
                SettingsCategory.GAMING       -> GamingSettings(args)
                SettingsCategory.ACCESSIBILITY-> AccessibilitySettings(args)
                SettingsCategory.PRIVACY      -> PrivacySecuritySettings(args)
                SettingsCategory.BACKUP       -> BackupRestoreSettings(args)
                SettingsCategory.SEARCH       -> SearchSettings(args)
                SettingsCategory.UPDATE       -> LauncherUpdateSettings(args)
                SettingsCategory.ABOUT        -> AboutSettings(args)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED ARGS
// ─────────────────────────────────────────────────────────────────────────────

internal data class ScreenArgs(
    val isDark    : Boolean,
    val textColor : Color,
    val surfaceBg : Color,
    val accent    : Color,
    val uiState   : LauncherUiState?,
    val vm        : LauncherViewModel?,
    val ctx       : Context
)

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SettingsGroup(title: String, a: ScreenArgs, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title,
            color      = a.accent,
            fontSize   = (12 * (a.uiState?.textScale ?: 1f)).sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = a.surfaceBg),
            shape  = RoundedCornerShape(8.dp)
        ) { Column(modifier = Modifier.padding(4.dp), content = content) }
    }
}

// BUG FIX: entire row is now clickable, not just the Switch thumb
@Composable
internal fun SToggle(
    icon     : ImageVector,
    label    : String,
    sub      : String = "",
    a        : ScreenArgs,
    checked  : Boolean,
    onToggle : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() }           // ← FIX: whole row tappable
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = a.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = a.accent)
        )
    }
}

@Composable
internal fun SNav(
    icon    : ImageVector,
    label   : String,
    sub     : String = "",
    value   : String = "",
    a       : ScreenArgs,
    onClick : () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = a.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
        }
        if (value.isNotEmpty()) Text(value, color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp)
        Icon(Icons.Default.ChevronRight, null, tint = a.textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}

@Composable
internal fun SSlider(
    icon          : ImageVector,
    label         : String,
    sub           : String = "",
    value         : Float,
    range         : ClosedFloatingPointRange<Float> = 0f..1f,
    valueLabel    : String = "",
    a             : ScreenArgs,
    onValueChange : (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = a.accent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp)
                if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
            }
            Text(valueLabel, color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp)
        }
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = range,
            colors        = SliderDefaults.colors(thumbColor = a.accent, activeTrackColor = a.accent),
            modifier      = Modifier.padding(start = 32.dp)
        )
    }
}

@Composable
internal fun SDropdown(
    icon     : ImageVector,
    label    : String,
    sub      : String = "",
    options  : List<String>,
    selected : String,
    a        : ScreenArgs,
    onSelect : (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = a.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
        }
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(a.textColor.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(selected, color = a.textColor, fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = a.textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text        = { Text(option, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp) },
                        onClick     = { onSelect(option); expanded = false },
                        leadingIcon = if (option == selected) ({
                            Icon(Icons.Default.Check, null, tint = a.accent, modifier = Modifier.size(14.dp))
                        }) else null
                    )
                }
            }
        }
    }
}

internal fun divColor(a: ScreenArgs) = a.textColor.copy(alpha = 0.06f)

// ─────────────────────────────────────────────────────────────────────────────
// SYSTEM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SystemSettings(a: ScreenArgs) {
    StorageInfo(a)
    RamInfo(a)
    Spacer(Modifier.height(4.dp))

    SettingsGroup("Startup & Boot", a) {
        SToggle(Icons.Default.PlayCircle, "Launch on boot",
            "Start launcher automatically when device starts",
            a, a.uiState?.launchOnBoot ?: false) { a.vm?.setLaunchOnBoot(!(a.uiState?.launchOnBoot ?: false)) }
    }

    SettingsGroup("Multitasking", a) {
        SToggle(Icons.Default.GridView, "Snap layouts",
            "Snap windows to screen zones",
            a, a.uiState?.snapLayouts ?: true) { a.vm?.setSnapLayouts(!(a.uiState?.snapLayouts ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.ContentPaste, "Clipboard history",
            "Keep a history of copied items",
            a, a.uiState?.clipboardHistory ?: true) { a.vm?.setClipboardHistory(!(a.uiState?.clipboardHistory ?: true)) }
    }

    SettingsGroup("Recent apps", a) {
        SDropdown(Icons.Default.History, "Recent app limit",
            "How many apps appear in the recents tray",
            listOf("5", "10", "15", "20", "Unlimited"),
            a.uiState?.recentAppsLimit ?: "10", a) { a.vm?.setRecentAppsLimit(it) }
    }

    SettingsGroup("Notifications", a) {
        SToggle(Icons.Default.Notifications, "Notification banners",
            "Show pop-up banners for new notifications",
            a, a.uiState?.notificationBanners ?: true) { a.vm?.setNotificationBanners(!(a.uiState?.notificationBanners ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Badge, "Notification badges",
            "Show red dot on app icons",
            a, a.uiState?.showNotificationBadges ?: true) { a.vm?.setShowNotificationBadges(!(a.uiState?.showNotificationBadges ?: true)) }
    }

    SettingsGroup("Do Not Disturb", a) {
        SToggle(Icons.Default.DoNotDisturb, "Do Not Disturb",
            "Silence all notifications",
            a, a.uiState?.dndEnabled ?: false) { a.vm?.setDndEnabled(!(a.uiState?.dndEnabled ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Schedule, "Scheduled DND",
            "Auto-enable between set hours",
            a, a.uiState?.dndScheduled ?: false) { a.vm?.setDndScheduled(!(a.uiState?.dndScheduled ?: false)) }
        if (a.uiState?.dndScheduled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, null, tint = a.accent, modifier = Modifier.size(16.dp))
                Text("From ${a.uiState.dndStartHour}:00 to ${a.uiState.dndEndHour}:00",
                    color    = a.textColor.copy(alpha = 0.7f),
                    fontSize = (12 * a.uiState.textScale).sp)
            }
        }
    }

    SettingsGroup("Focus Assist", a) {
        SToggle(Icons.Default.CenterFocusStrong, "Focus assist",
            "Allow only priority notifications",
            a, a.uiState?.focusAssist ?: false) { a.vm?.setFocusAssist(!(a.uiState?.focusAssist ?: false)) }
    }

    SettingsGroup("Battery & Power", a) {
        SToggle(Icons.Default.BatterySaver, "Battery saver",
            "Limit background activity to extend battery life",
            a, a.uiState?.batterySaver ?: false) { a.vm?.setBatterySaver(!(a.uiState?.batterySaver ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.PowerSettingsNew, "Screen timeout",
            "Turn off screen after inactivity",
            listOf("30 seconds", "1 minute", "2 minutes", "5 minutes", "10 minutes", "Never"),
            when (a.uiState?.screenTimeoutMinutes) {
                0 -> "30 seconds"; 1 -> "1 minute"; 2 -> "2 minutes"
                5 -> "5 minutes";  10 -> "10 minutes"; else -> "Never"
            }, a
        ) { v -> a.vm?.setScreenTimeout(when (v) {
            "30 seconds" -> 0; "1 minute" -> 1; "2 minutes" -> 2
            "5 minutes"  -> 5; "10 minutes" -> 10; else -> -1
        }) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOUND
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SoundSettings(a: ScreenArgs) {
    SettingsGroup("Volume", a) {
        SSlider(Icons.Default.MusicNote, "Media volume", "Music, videos, games",
            a.uiState?.mediaVolume ?: 0.7f, 0f..1f,
            "${((a.uiState?.mediaVolume ?: 0.7f) * 100).toInt()}%", a) { a.vm?.setMediaVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(Icons.Default.NotificationsActive, "Notification volume", "",
            a.uiState?.notifVolume ?: 0.6f, 0f..1f,
            "${((a.uiState?.notifVolume ?: 0.6f) * 100).toInt()}%", a) { a.vm?.setNotifVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(Icons.Default.Phone, "Ring volume", "",
            a.uiState?.ringtoneVolume ?: 0.5f, 0f..1f,
            "${((a.uiState?.ringtoneVolume ?: 0.5f) * 100).toInt()}%", a) { a.vm?.setRingtoneVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(Icons.Default.Alarm, "Alarm volume", "",
            a.uiState?.alarmVolume ?: 0.8f, 0f..1f,
            "${((a.uiState?.alarmVolume ?: 0.8f) * 100).toInt()}%", a) { a.vm?.setAlarmVolume(it, a.ctx) }
    }

    SettingsGroup("Sound preferences", a) {
        SToggle(Icons.Default.VolumeUp, "System sounds", "UI click and action sounds",
            a, a.uiState?.systemSounds ?: true) { a.vm?.setSystemSounds(!(a.uiState?.systemSounds ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Vibration, "Haptic feedback", "Vibrate on touch",
            a, a.uiState?.hapticFeedback ?: true) { a.vm?.setHapticFeedback(!(a.uiState?.hapticFeedback ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.VolumeDown, "Media volume keys",
            "Volume keys control media by default",
            a, a.uiState?.volumeKeysMedia ?: true) { a.vm?.setVolumeKeysMedia(!(a.uiState?.volumeKeysMedia ?: true)) }
    }

    SettingsGroup("Notification sound", a) {
        SDropdown(Icons.Default.NotificationsActive, "Default notification sound", "",
            listOf("Default", "Chime", "Ping", "Ripple", "Skyline", "None"),
            a.uiState?.notifSound ?: "Default", a) { a.vm?.setNotifSound(it) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BLUETOOTH & DEVICES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BluetoothSettings(a: ScreenArgs) {
    SettingsGroup("Bluetooth", a) {
        SToggle(Icons.Default.Bluetooth, "Bluetooth", "Enable Bluetooth radio",
            a, a.uiState?.isBluetoothOn ?: false) {
            a.vm?.openBluetoothSettings(a.ctx)
            a.vm?.toggleBluetooth()
        }
    }

    SettingsGroup("Connected devices", a) {
        if (a.uiState?.isBluetoothOn == true) {
            SNav(Icons.Default.BluetoothSearching, "Pair new device", "Scan for nearby Bluetooth devices", a = a) {
                a.vm?.openBluetoothSettings(a.ctx)
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                if (a.uiState?.isBluetoothOn == true) "No devices paired yet"
                else "Enable Bluetooth to see devices",
                color    = a.textColor.copy(alpha = 0.4f),
                fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp
            )
        }
    }

    SettingsGroup("Other devices", a) {
        SNav(Icons.Default.Mouse, "Mouse & touchpad", "Pointer speed, buttons", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Keyboard, "Keyboard", "Layout, language, shortcuts", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Print, "Printers & scanners", "Manage connected printers", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Usb, "USB", "USB preferences and connected drives", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Cast, "Wireless displays", "Connect to a screen or TV wirelessly", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NETWORK & INTERNET
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NetworkSettings(a: ScreenArgs) {
    SettingsGroup("Wi-Fi", a) {
        SToggle(Icons.Default.Wifi, "Wi-Fi",
            if (a.uiState?.isWifiOn == true) "Connected" else "Off",
            a, a.uiState?.isWifiOn ?: false) {
            a.vm?.openWifiSettings(a.ctx)
            a.vm?.toggleWifi()
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.WifiFind, "Manage networks", "Saved, available Wi-Fi networks", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("Mobile & data", a) {
        SToggle(Icons.Default.DataSaverOn, "Data saver",
            "Restrict background mobile data",
            a, a.uiState?.dataSaver ?: false) { a.vm?.setDataSaver(!(a.uiState?.dataSaver ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Router, "Mobile hotspot",
            "Share internet with other devices",
            a, a.uiState?.hotspotEnabled ?: false) { a.vm?.setHotspot(!(a.uiState?.hotspotEnabled ?: false)) }
        if (a.uiState?.hotspotEnabled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PhoneAndroid, null, tint = a.accent, modifier = Modifier.size(14.dp))
                Text("0 devices connected", color = a.textColor.copy(alpha = 0.6f),
                    fontSize = (11 * (a.uiState.textScale)).sp)
            }
        }
    }

    SettingsGroup("Airplane mode", a) {
        SToggle(Icons.Default.AirplanemodeActive, "Airplane mode",
            "Disable all wireless communications",
            a, a.uiState?.isAirplaneMode ?: false) { a.vm?.toggleAirplaneMode() }
    }

    SettingsGroup("VPN", a) {
        SToggle(Icons.Default.VpnKey, "VPN",
            if (a.uiState?.vpnEnabled == true) "Connected" else "Not connected",
            a, a.uiState?.vpnEnabled ?: false) { a.vm?.setVpn(!(a.uiState?.vpnEnabled ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Add, "Add a VPN", "Configure a new VPN connection", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("DNS", a) {
        SToggle(Icons.Default.Dns, "Custom DNS", "Use a custom DNS server address",
            a, a.uiState?.customDns ?: false) { a.vm?.setCustomDns(!(a.uiState?.customDns ?: false)) }
        if (a.uiState?.customDns == true) {
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SDropdown(Icons.Default.Language, "DNS server", "Choose a preset or enter custom",
                listOf("8.8.8.8 (Google)", "1.1.1.1 (Cloudflare)", "9.9.9.9 (Quad9)", "8.8.4.4 (Google alt)", "3.1.1.1 (EAT)"),
                a.uiState.dnsAddress, a) { a.vm?.setDnsAddress(it) }
        }
    }

    SettingsGroup("Proxy", a) {
        SNav(Icons.Default.SettingsEthernet, "Proxy settings",
            "Manual proxy configuration for this network", a = a)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APPEARANCE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSettings(a: ScreenArgs) {

    // Theme picker
    SettingsGroup("Theme", a) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Choose a theme", color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp, fontWeight = FontWeight.Medium)
            Text("Controls how the entire launcher looks", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
            Spacer(Modifier.height(4.dp))

            data class ThemeOption(val theme: AppTheme, val icon: ImageVector, val name: String, val desc: String, val bg: Color, val accent: Color)
            val themes = listOf(
                ThemeOption(AppTheme.SYSTEM,  Icons.Default.SettingsBrightness, "System",    "Follows Android system dark/light",       Color(0xFF1F1F1F), Color(0xFF0078D4)),
                ThemeOption(AppTheme.FOR_YOU, Icons.Default.AutoAwesome,        "For You",   "Adapts accent colors from your wallpaper", Color(0xFF2A2016), Color(0xFFD4A017)),
                ThemeOption(AppTheme.DARK,    Icons.Default.DarkMode,           "Dark",      "Always dark",                             Color(0xFF121212), Color(0xFF4FC3F7)),
                ThemeOption(AppTheme.LIGHT,   Icons.Default.LightMode,          "Light",     "Always light and crisp",                  Color(0xFFF5F5F5), Color(0xFF1565C0)),
                ThemeOption(AppTheme.SPECIAL, Icons.Default.Star,               "Special ✦", "Deep indigo — By LAMN-NOBERT",            Color(0xFF12092A), Color(0xFF9C6BF7))
            )
            val currentTheme = a.uiState?.appTheme ?: AppTheme.SYSTEM

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { t ->
                    val isSelected = currentTheme == t.theme
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(if (isSelected) 2.dp else 1.dp,
                                if (isSelected) a.accent else a.textColor.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp))
                            .clickable { a.vm?.setAppTheme(t.theme) }
                            .padding(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(4.dp)).background(t.bg), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(14.dp).background(t.accent, CircleShape))
                        }
                        Spacer(Modifier.height(6.dp))
                        Icon(t.icon, null, tint = if (isSelected) a.accent else a.textColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(t.name, color = if (isSelected) a.accent else a.textColor,
                            fontSize   = (10 * (a.uiState?.textScale ?: 1f)).sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            AnimatedContent(targetState = currentTheme, label = "theme_desc") { t ->
                Text(themes.find { it.theme == t }?.desc ?: "", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.uiState?.textScale ?: 1f)).sp)
            }
        }
    }

    // Dark mode schedule
    SettingsGroup("Dark mode schedule", a) {
        SDropdown(Icons.Default.Bedtime, "Auto dark mode",
            "Automatically switch theme by time or system",
            listOf("Disabled", "Sunset to sunrise", "Custom hours", "Follow system"),
            a.uiState?.darkModeSchedule ?: "Follow system", a) { a.vm?.setDarkModeSchedule(it) }
    }

    // Wallpaper
    SettingsGroup("Background", a) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Desktop wallpaper", color = a.textColor, fontSize = (13 * (a.uiState?.textScale ?: 1f)).sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                wallpaperGradients.forEachIndexed { index, gradient ->
                    val isSelected = a.uiState?.wallpaper?.homeWallpaperIndex == index && a.uiState.wallpaper.homeWallpaperUri.isEmpty()
                    Box(
                        modifier = Modifier.size(56.dp, 36.dp).clip(RoundedCornerShape(4.dp))
                            .background(Brush.linearGradient(gradient))
                            .border(if (isSelected) 2.dp else 0.dp, a.accent, RoundedCornerShape(4.dp))
                            .clickable { a.vm?.setBuiltInWallpaper(index, WallpaperTarget.HOME) },
                        contentAlignment = Alignment.Center
                    ) { if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
            SToggle(Icons.Default.Slideshow, "Wallpaper slideshow",
                "Rotate wallpaper automatically",
                a, a.uiState?.wallpaperSlideshow ?: false) { a.vm?.setWallpaperSlideshow(!(a.uiState?.wallpaperSlideshow ?: false)) }
            if (a.uiState?.wallpaperSlideshow == true) {
                SDropdown(Icons.Default.Timer, "Change every", "",
                    listOf("15 minutes", "30 minutes", "1 hour", "6 hours", "Daily"),
                    a.uiState.wallpaperSlideshowInterval ?: "1 hour", a) { a.vm?.setWallpaperSlideshowInterval(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { a.vm?.openWallpaperPicker(WallpaperTarget.HOME) },
                    border  = BorderStroke(1.dp, a.accent),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = a.accent)
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Home screen", fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp)
                }
                OutlinedButton(
                    onClick = { a.vm?.openWallpaperPicker(WallpaperTarget.LOCK_SCREEN) },
                    border  = BorderStroke(1.dp, a.accent),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = a.accent)
                ) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock screen", fontSize = (12 * (a.uiState?.textScale ?: 1f)).sp)
                }
            }
        }
    }

    // Accent color
    SettingsGroup("Accent color", a) {
        val accentColors = listOf(0xFF0078D4L, 0xFF107C10L, 0xFFE81123L, 0xFFFF8C00L, 0xFF744DA9L,
            0xFF00B7C3L, 0xFFE74856L, 0xFF7A7574L, 0xFF9C6BF7L, 0xFF10A0E3L, 0xFF00CC6AL, 0xFFFF6B35L)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                accentColors.forEach { colorValue ->
                    val color      = Color(colorValue)
                    val isSelected = a.uiState?.accentColor == colorValue
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(color)
                            .border(if (isSelected) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { a.vm?.setAccentColor(colorValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    // Text size
    SettingsGroup("Text size", a) {
        val scale = a.uiState?.textScale ?: 1f
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.TextFields, null, tint = a.accent, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Text size", color = a.textColor, fontSize = (13 * scale).sp)
                    Text("Drag to resize all text in the launcher", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * scale).sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(a.accent.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(when { scale < 0.85f -> "XS"; scale < 0.95f -> "Small"; scale < 1.05f -> "Default"; scale < 1.15f -> "Large"; scale < 1.25f -> "XL"; else -> "XXL" },
                        color = a.accent, fontSize = (11 * scale).sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 32.dp)) {
                Text("A", color = a.textColor.copy(alpha = 0.4f), fontSize = 10.sp)
                Slider(value = scale, onValueChange = { a.vm?.setTextScale(it) }, valueRange = 0.8f..1.4f, steps = 5,
                    colors   = SliderDefaults.colors(thumbColor = a.accent, activeTrackColor = a.accent),
                    modifier = Modifier.weight(1f))
                Text("A", color = a.textColor.copy(alpha = 0.8f), fontSize = 18.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(a.textColor.copy(alpha = 0.05f)).padding(10.dp)) {
                Text("Preview: The quick brown fox jumps over the lazy dog", color = a.textColor.copy(alpha = 0.7f), fontSize = (13 * scale).sp)
            }
        }
    }

    // Font
    SettingsGroup("Font", a) {
        SDropdown(Icons.Default.FontDownload, "System font",
            "Choose the font used across the launcher",
            listOf("Default (Roboto)", "Sans-serif", "Serif", "Monospace", "Cursive"),
            a.uiState?.launcherFont ?: "Default (Roboto)", a) { a.vm?.setLauncherFont(it) }
    }

    // Icon size
    SettingsGroup("Icon size", a) {
        SDropdown(Icons.Default.Apps, "Desktop icon size",
            "Size of app icons on the home screen",
            listOf("Small", "Medium", "Large", "Extra Large"),
            a.uiState?.iconSize ?: "Medium", a) { a.vm?.setIconSize(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.BorderAll, "Grid size",
            "Columns × rows on the home screen",
            listOf("4 × 5", "4 × 6", "5 × 5", "5 × 6", "6 × 6"),
            a.uiState?.gridSize ?: "4 × 5", a) { a.vm?.setGridSize(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Label, "Show app labels",
            "Display app names below icons",
            a, a.uiState?.showAppLabels ?: true) { a.vm?.setShowAppLabels(!(a.uiState?.showAppLabels ?: true)) }
    }

    // Corner radius
    SettingsGroup("Shape & corners", a) {
        SSlider(Icons.Default.RoundedCorner, "Corner radius",
            "Roundness of cards and menus",
            a.uiState?.cornerRadius ?: 0.5f, 0f..1f,
            when { (a.uiState?.cornerRadius ?: 0.5f) < 0.25f -> "Sharp"; (a.uiState?.cornerRadius ?: 0.5f) < 0.6f -> "Rounded"; else -> "Pill" },
            a) { a.vm?.setCornerRadius(it) }
    }

    // Visual effects
    SettingsGroup("Visual effects", a) {
        SToggle(Icons.Default.BlurOn, "Transparency & blur",
            "Acrylic/blur effect on panels",
            a, a.uiState?.transparencyEffects ?: true) { a.vm?.setTransparencyEffects(!(a.uiState?.transparencyEffects ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(Icons.Default.Speed, "Animation speed",
            "Controls launcher transition speed",
            a.uiState?.animationSpeed ?: 1f, 0f..2f,
            when { (a.uiState?.animationSpeed ?: 1f) < 0.1f -> "Off"; (a.uiState?.animationSpeed ?: 1f) < 0.75f -> "Slow"; (a.uiState?.animationSpeed ?: 1f) < 1.25f -> "Normal"; else -> "Fast" },
            a) { a.vm?.setAnimationSpeed(it) }
    }

    // Layout
    SettingsGroup("Layout", a) {
        SDropdown(Icons.Default.ViewQuilt, "Taskbar position",
            "Where the taskbar appears on screen",
            listOf("Bottom", "Left", "Right"),
            a.uiState?.taskbarPosition ?: "Bottom", a) { a.vm?.setTaskbarPosition(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.GridView, "Start menu layout",
            "Balance of pinned apps vs. recommendations",
            listOf("More pins", "Balanced", "More recommendations"),
            a.uiState?.startMenuLayout ?: "Balanced", a) { a.vm?.setStartMenuLayout(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.Sort, "Status bar clock position",
            "Where the clock appears in the status bar",
            listOf("Left", "Center", "Right"),
            a.uiState?.statusBarClockPosition ?: "Right", a) { a.vm?.setStatusBarClockPosition(it) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GESTURES  (new section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GestureSettings(a: ScreenArgs) {
    SettingsGroup("Home screen gestures", a) {
        SDropdown(Icons.Default.SwipeUp, "Swipe up",
            "Action when swiping up on the home screen",
            listOf("App drawer", "Notification shade", "Search", "Nothing"),
            a.uiState?.gestureSwipeUp ?: "App drawer", a) { a.vm?.setGestureSwipeUp(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.SwipeDown, "Swipe down",
            "Action when swiping down on the home screen",
            listOf("Notification shade", "Quick settings", "Search", "Nothing"),
            a.uiState?.gestureSwipeDown ?: "Notification shade", a) { a.vm?.setGestureSwipeDown(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.TouchApp, "Double-tap home",
            "Action when double-tapping on empty space",
            listOf("Lock screen", "Search", "Nothing"),
            a.uiState?.gestureDoubleTap ?: "Lock screen", a) { a.vm?.setGestureDoubleTap(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.ZoomIn, "Pinch to open",
            "Pinch gesture on the home screen",
            listOf("App drawer", "Overview", "Nothing"),
            a.uiState?.gesturePinch ?: "Overview", a) { a.vm?.setGesturePinch(it) }
    }

    SettingsGroup("App icon gestures", a) {
        SToggle(Icons.Default.Swipe, "Swipe-up shortcut on icons",
            "Assign an action to swiping up on any app icon",
            a, a.uiState?.iconSwipeUpEnabled ?: false) { a.vm?.setIconSwipeUp(!(a.uiState?.iconSwipeUpEnabled ?: false)) }
    }

    SettingsGroup("Navigation gestures", a) {
        SToggle(Icons.Default.NavigateNext, "Navigation bar gestures",
            "Use swipe gestures instead of buttons",
            a, a.uiState?.navBarGestures ?: true) { a.vm?.setNavBarGestures(!(a.uiState?.navBarGestures ?: true)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APPS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppsSettings(a: ScreenArgs) {
    val pm    = a.ctx.packageManager
    val scale = a.uiState?.textScale ?: 1f

    // Load apps once; not inside LazyColumn to avoid repeated recomposition
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(pm).toString() }
    }
    var searchQuery  by remember { mutableStateOf("") }
    var sortOrder    by remember { mutableStateOf("A–Z") }
    val filtered = apps
        .filter { it.loadLabel(pm).toString().contains(searchQuery, ignoreCase = true) }
        .let { list -> if (sortOrder == "Z–A") list.reversed() else list }

    SettingsGroup("App drawer", a) {
        SDropdown(Icons.Default.Sort, "Sort order",
            "How apps appear in the drawer",
            listOf("A–Z", "Z–A", "Most used", "Recently installed"),
            sortOrder, a) { sortOrder = it }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.HideSource, "Hide apps",
            "Choose apps to hide from the drawer",
            a, a.uiState?.hideAppsEnabled ?: false) { a.vm?.setHideApps(!(a.uiState?.hideAppsEnabled ?: false)) }
    }

    SettingsGroup("Search apps (${filtered.size} / ${apps.size})", a) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, null, tint = a.textColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            BasicTextField(
                value        = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine   = true,
                textStyle    = androidx.compose.ui.text.TextStyle(color = a.textColor, fontSize = (13 * scale).sp),
                modifier     = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text("Search installed apps…", color = a.textColor.copy(alpha = 0.3f), fontSize = (13 * scale).sp)
                    inner()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Icon(Icons.Default.Close, null,
                    tint     = a.textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
            }
        }
    }

    // FIX: proper scrollable container — no nested scroll conflict
    SettingsGroup("Installed apps", a) {
        // Use a fixed-height scrollable Column (not LazyColumn inside verticalScroll)
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(scrollState)
        ) {
            filtered.forEachIndexed { i, info ->
                if (i > 0) Divider(color = divColor(a).copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            try {
                                a.ctx.startActivity(
                                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:${info.activityInfo.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter     = DrawablePainter(info.loadIcon(pm)),
                        contentDescription = null,
                        modifier    = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(info.loadLabel(pm).toString(), color = a.textColor, fontSize = (13 * scale).sp)
                        Text(info.activityInfo.packageName, color = a.textColor.copy(alpha = 0.4f),
                            fontSize = (10 * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = a.textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    SettingsGroup("App defaults", a) {
        SNav(Icons.Default.OpenInBrowser, "Default browser",  "Choose which browser opens links",       a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Email,          "Default email app", "Choose which app handles mailto links", a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Map,            "Default map app",   "Choose map application",                a = a)
    }

    SettingsGroup("Install sources", a) {
        SNav(Icons.Default.Security, "Install unknown apps", "Allow sideloading from other sources", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACCOUNTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RenameAccountDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Change username") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Display name") },
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AccountsSettings(a: ScreenArgs) {
    val scale = a.uiState?.textScale ?: 1f
    var showRenameDialog by remember { mutableStateOf(false) }

    SettingsGroup("Your account", a) {
        if (a.uiState != null && a.vm != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(56.dp)) {
                    if (a.uiState.userProfile.profilePicturePath.isNotEmpty()) {
                        AsyncImage(model = Uri.parse(a.uiState.userProfile.profilePicturePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(a.accent, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(a.uiState.userProfile.userName, color = a.textColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Medium)
                    Text("Local Account", color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * scale).sp)
                }
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SNav(Icons.Default.Edit,      "Change username",        "Update your display name",         a = a) {
                showRenameDialog = true
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SNav(Icons.Default.AddAPhoto, "Change profile picture", "Pick a photo from your gallery",   a = a) {
                a.vm.openProfilePicturePicker()
            }

            if (showRenameDialog) {
                RenameAccountDialog(
                    currentName = a.uiState.userProfile.userName,
                    onConfirm = { newName ->
                        a.vm.setUserName(newName)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false }
                )
            }
        }
    }

    SettingsGroup("Sign-in options", a) {
        SNav(Icons.Default.Pin,         "PIN",            "Set up a numeric PIN for quick sign-in",      a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Pattern,     "Screen pattern", "Draw a pattern to unlock",                    a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Password,    "Password",       "Use a full text password",                    a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Fingerprint, "Fingerprint",    "Register fingerprint for biometric unlock",   a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_FINGERPRINT_ENROLL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Face,        "Face unlock",    "Use facial recognition",                      a = a)
    }

    SettingsGroup("Linked accounts", a) {
        SNav(Icons.Default.AccountBox,  "Google account",    "Sync, apps and services",    value = "Not linked", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Cloud,       "Microsoft account", "OneDrive, Outlook and more", value = "Not linked", a = a)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TIME & LANGUAGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimeLanguageSettings(a: ScreenArgs) {
    val scale       = a.uiState?.textScale ?: 1f
    val use24h      = a.uiState?.use24HourClock ?: false
    val timeFmt     = if (use24h) "HH:mm" else "hh:mm a"
    val currentTime = remember(use24h) { SimpleDateFormat(timeFmt, Locale.getDefault()).format(Date()) }
    val currentDate = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }

    SettingsGroup("Date & time", a) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(currentTime, color = a.textColor, fontSize = (22 * scale).sp, fontWeight = FontWeight.Light)
                Text(currentDate, color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * scale).sp)
            }
            Icon(Icons.Default.AccessTime, null, tint = a.accent, modifier = Modifier.size(32.dp))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Sync,     "Set time automatically",  "Sync with internet time servers",       a, a.uiState?.autoSetTime ?: true)    { a.vm?.setAutoSetTime(!(a.uiState?.autoSetTime ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Schedule, "24-hour clock",           "Use 24-hour instead of 12-hour format", a, use24h)                             { a.vm?.setUse24HourClock(!use24h) }
    }

    SettingsGroup("Timezone", a) {
        val timezones = remember {
            TimeZone.getAvailableIDs()
                .map { id -> val tz = TimeZone.getTimeZone(id); "$id (${tz.displayName})" }
                .take(80) // Reasonable limit for the dropdown
        }
        SDropdown(Icons.Default.Public, "Timezone", "Set your local timezone",
            timezones, a.uiState?.timeZone ?: "Africa/Kampala", a) { a.vm?.setTimeZone(it) }
    }

    SettingsGroup("Date format", a) {
        SDropdown(Icons.Default.CalendarToday, "Date format", "How dates are displayed",
            listOf("MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD", "D MMM YYYY"),
            a.uiState?.dateFormat ?: "DD/MM/YYYY", a) { a.vm?.setDateFormat(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.ViewWeek, "First day of week", "",
            listOf("Sunday", "Monday", "Saturday"),
            a.uiState?.firstDayOfWeek ?: "Sunday", a) { a.vm?.setFirstDayOfWeek(it) }
    }

    SettingsGroup("Language & region", a) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("System language", color = a.textColor, fontSize = (13 * scale).sp)
            Text(Locale.getDefault().displayLanguage, color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * scale).sp)
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Translate, "Add a language", "Install additional language packs", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GAMING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GamingSettings(a: ScreenArgs) {
    SettingsGroup("Game Mode", a) {
        SToggle(Icons.Default.SportsEsports, "Game Mode",
            "Prioritize CPU/GPU for games, reduce background activity",
            a, a.uiState?.gameModeEnabled ?: false) { a.vm?.setGameMode(!(a.uiState?.gameModeEnabled ?: false)) }
        if (a.uiState?.gameModeEnabled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Text("Game Mode is active — background processes reduced",
                    color = a.textColor.copy(alpha = 0.6f), fontSize = (11 * (a.uiState.textScale)).sp)
            }
        }
    }

    SettingsGroup("Performance", a) {
        SDropdown(Icons.Default.Speed, "Frame rate cap",
            "Maximum frames rendered per second",
            listOf("30 fps", "60 fps", "90 fps", "120 fps", "Unlimited"),
            a.uiState?.frameRateCap ?: "Unlimited", a) { a.vm?.setFrameRateCap(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.BarChart, "Performance overlay",
            "Show FPS, CPU, and RAM while in a game",
            a, a.uiState?.performanceOverlay ?: false) { a.vm?.setPerformanceOverlay(!(a.uiState?.performanceOverlay ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Thermostat, "Thermal throttle protection",
            "Reduce performance to prevent overheating",
            a, a.uiState?.thermalProtection ?: true) { a.vm?.setThermalProtection(!(a.uiState?.thermalProtection ?: true)) }
    }

    SettingsGroup("During gameplay", a) {
        SToggle(Icons.Default.DoNotDisturb, "Do Not Disturb",
            "Mute notifications while a game is fullscreen",
            a, a.uiState?.dndWhileGaming ?: true) { a.vm?.setDndWhileGaming(!(a.uiState?.dndWhileGaming ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Vibration, "Haptic feedback",
            "Vibration effects in supported games",
            a, a.uiState?.hapticInGames ?: true) { a.vm?.setHapticInGames(!(a.uiState?.hapticInGames ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Screenshot, "Block screenshots",
            "Prevent other apps from capturing your screen during a game",
            a, a.uiState?.blockGameScreenshots ?: false) { a.vm?.setBlockGameScreenshots(!(a.uiState?.blockGameScreenshots ?: false)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACCESSIBILITY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccessibilitySettings(a: ScreenArgs) {
    SettingsGroup("Vision", a) {
        SToggle(Icons.Default.Contrast, "High contrast",
            "Increase contrast for better readability",
            a, a.uiState?.highContrast ?: false) { a.vm?.setHighContrast(!(a.uiState?.highContrast ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.FontDownload, "Larger text",
            "Increase base text size across the launcher",
            a, a.uiState?.largerText ?: false) { a.vm?.setLargerText(!(a.uiState?.largerText ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.FormatBold, "Bold text",
            "Make all text bold for easier reading",
            a, a.uiState?.boldText ?: false) { a.vm?.setBoldText(!(a.uiState?.boldText ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.ColorLens, "Color correction",
            "Adjust colors for color vision deficiencies",
            listOf("None", "Deuteranopia (red-green)", "Protanopia (red-green alt)", "Tritanopia (blue-yellow)", "Grayscale"),
            a.uiState?.colorCorrectionMode ?: "None", a) { a.vm?.setColorCorrectionMode(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.ZoomIn, "Magnification",
            "Triple-tap to zoom anywhere on screen",
            a, a.uiState?.magnificationEnabled ?: false) { a.vm?.setMagnification(!(a.uiState?.magnificationEnabled ?: false)) }
    }

    SettingsGroup("Motion & interaction", a) {
        SToggle(Icons.Default.Animation, "Reduce motion",
            "Minimize animations and transitions",
            a, a.uiState?.reduceMotion ?: false) { a.vm?.setReduceMotion(!(a.uiState?.reduceMotion ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(Icons.Default.TouchApp, "Touch & hold delay",
            "How long before a long-press registers",
            a.uiState?.touchHoldDelay ?: 0.5f, 0f..1f,
            when { (a.uiState?.touchHoldDelay ?: 0.5f) < 0.33f -> "Short"; (a.uiState?.touchHoldDelay ?: 0.5f) < 0.66f -> "Medium"; else -> "Long" },
            a) { a.vm?.setTouchHoldDelay(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.RadioButtonChecked, "Button shapes",
            "Show outlines around tappable buttons",
            a, a.uiState?.buttonShapes ?: false) { a.vm?.setButtonShapes(!(a.uiState?.buttonShapes ?: false)) }
    }

    SettingsGroup("Audio", a) {
        SToggle(Icons.Default.HearingDisabled, "Mono audio",
            "Combine stereo channels into mono",
            a, a.uiState?.monoAudio ?: false) { a.vm?.setMonoAudio(!(a.uiState?.monoAudio ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.ClosedCaption, "Captions",
            "Show captions for media when available",
            a, a.uiState?.captionsEnabled ?: false) { a.vm?.setCaptionsEnabled(!(a.uiState?.captionsEnabled ?: false)) }
    }

    SettingsGroup("System", a) {
        SNav(Icons.Default.Accessibility, "More accessibility options",
            "Open Android's full accessibility settings", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIVACY & SECURITY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacySecuritySettings(a: ScreenArgs) {
    SettingsGroup("Device security", a) {
        SToggle(Icons.Default.Lock, "Screen lock",
            "Require PIN, pattern, or password on wake",
            a, a.uiState?.screenLock ?: false) { a.vm?.setScreenLock(!(a.uiState?.screenLock ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Fingerprint, "Biometric unlock",
            "Use fingerprint or face to unlock",
            a, a.uiState?.biometricUnlock ?: false) { a.vm?.setBiometricUnlock(!(a.uiState?.biometricUnlock ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.InstallMobile, "Install unknown apps",
            "Allow apps from outside the Play Store",
            a, a.uiState?.unknownSources ?: false) { a.vm?.setUnknownSources(!(a.uiState?.unknownSources ?: false)) }
    }

    SettingsGroup("App permissions", a) {
        SToggle(Icons.Default.LocationOn, "Location access",
            "Allow apps to request location",
            a, a.uiState?.locationAccess ?: true) { a.vm?.setLocationAccess(!(a.uiState?.locationAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Camera, "Camera access",
            "Allow apps to use the camera",
            a, a.uiState?.cameraAccess ?: true) { a.vm?.setCameraAccess(!(a.uiState?.cameraAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Mic, "Microphone access",
            "Allow apps to use the microphone",
            a, a.uiState?.micAccess ?: true) { a.vm?.setMicAccess(!(a.uiState?.micAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.ManageAccounts, "Manage per-app permissions",
            "Fine-grained control per application", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("Data & diagnostics", a) {
        SToggle(Icons.Default.Analytics, "Usage & diagnostics",
            "Send anonymous crash reports and usage data",
            a, a.uiState?.usageDiagnostics ?: false) { a.vm?.setUsageDiagnostics(!(a.uiState?.usageDiagnostics ?: false)) }
    }

    SettingsGroup("Privacy dashboard", a) {
        SNav(Icons.Default.Policy, "View permission usage",
            "See which apps used each permission recently", a = a) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                a.ctx.startActivity(Intent(android.provider.Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.DeleteSweep, "Clear app data",
            "Wipe stored data for specific apps", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BACKUP & RESTORE  (new section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BackupRestoreSettings(a: ScreenArgs) {
    val scale = a.uiState?.textScale ?: 1f

    SettingsGroup("Backup", a) {
        SToggle(Icons.Default.Cloud, "Auto backup",
            "Automatically back up launcher settings to device storage",
            a, a.uiState?.autoBackup ?: false) { a.vm?.setAutoBackup(!(a.uiState?.autoBackup ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.SaveAlt, "Export settings",
            "Save a backup file to your Downloads folder", a = a) {
            a.vm?.exportSettings(a.ctx)
        }
    }

    SettingsGroup("Restore", a) {
        SNav(Icons.Default.FolderOpen, "Import settings",
            "Restore from a previously exported backup", a = a) {
            a.vm?.importSettings(a.ctx)
        }
    }

    SettingsGroup("Reset", a) {
        var showConfirm by remember { mutableStateOf(false) }
        SNav(Icons.Default.RestartAlt, "Reset all settings",
            "Restore launcher defaults — this cannot be undone", a = a) {
            showConfirm = true
        }
        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("Reset all settings?") },
                text  = { Text("This will restore all launcher settings to their defaults. Your apps and data will not be affected.") },
                confirmButton = {
                    TextButton(onClick = {
                        a.vm?.resetAllSettings()
                        showConfirm = false
                    }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH  (new section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchSettings(a: ScreenArgs) {
    SettingsGroup("Search bar", a) {
        SToggle(Icons.Default.Search, "Show search bar",
            "Display the search bar on the home screen",
            a, a.uiState?.showSearchBar ?: true) { a.vm?.setShowSearchBar(!(a.uiState?.showSearchBar ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(Icons.Default.Language, "Default search engine",
            "Used for web searches from the home screen",
            listOf("Google", "Bing", "DuckDuckGo", "Yahoo", "Brave Search"),
            a.uiState?.searchEngine ?: "Google", a) { a.vm?.setSearchEngine(it) }
    }

    SettingsGroup("Search results include", a) {
        SToggle(Icons.Default.Apps, "Apps",
            "Show installed apps in search results",
            a, a.uiState?.searchIncludeApps ?: true) { a.vm?.setSearchIncludeApps(!(a.uiState?.searchIncludeApps ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Contacts, "Contacts",
            "Show contacts in search results",
            a, a.uiState?.searchIncludeContacts ?: true) { a.vm?.setSearchIncludeContacts(!(a.uiState?.searchIncludeContacts ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Settings, "Settings",
            "Show settings pages in search results",
            a, a.uiState?.searchIncludeSettings ?: true) { a.vm?.setSearchIncludeSettings(!(a.uiState?.searchIncludeSettings ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(Icons.Default.Web, "Web results",
            "Show web suggestions while typing",
            a, a.uiState?.searchWebSuggestions ?: true) { a.vm?.setSearchWebSuggestions(!(a.uiState?.searchWebSuggestions ?: true)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STORAGE HELPER  (improved — no deprecated API)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StorageInfo(a: ScreenArgs) {
    val scale = a.uiState?.textScale ?: 1f

    val (total, used) = remember {
        try {
            val dir  = a.ctx.filesDir
            val tot  = dir.totalSpace
            val free = dir.usableSpace
            Pair(tot, tot - free)
        } catch (_: Exception) { Pair(0L, 0L) }
    }

    SettingsGroup("Storage", a) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text("Internal Storage", color = a.textColor, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = (13 * scale).sp)
                Text("${fmtBytes(used)} / ${fmtBytes(total)}", color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * scale).sp)
            }
            LinearProgressIndicator(
                progress    = { if (total > 0) (used.toFloat() / total) else 0f },
                modifier    = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color       = when {
                    total > 0 && (used.toFloat() / total) > 0.9f -> Color(0xFFE53935)
                    total > 0 && (used.toFloat() / total) > 0.75f-> Color(0xFFFF9800)
                    else -> a.accent
                },
                trackColor  = a.textColor.copy(alpha = 0.1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StorageLegend("Used", a.accent, fmtBytes(used))
                StorageLegend("Free", a.textColor.copy(alpha = 0.3f), fmtBytes(total - used))
            }
        }
    }
}

@Composable
private fun RamInfo(a: ScreenArgs) {
    val scale = a.uiState?.textScale ?: 1f
    val (total, avail) = remember {
        val am  = a.ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi  = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        Pair(mi.totalMem, mi.availMem)
    }

    SettingsGroup("Memory (RAM)", a) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text("RAM usage", color = a.textColor, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = (13 * scale).sp)
                Text("${fmtBytes(total - avail)} / ${fmtBytes(total)}", color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * scale).sp)
            }
            LinearProgressIndicator(
                progress   = { if (total > 0) ((total - avail).toFloat() / total) else 0f },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = Color(0xFF9C6BF7),
                trackColor = a.textColor.copy(alpha = 0.1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StorageLegend("In use", Color(0xFF9C6BF7), fmtBytes(total - avail))
                StorageLegend("Free",   a.textColor.copy(alpha = 0.3f), fmtBytes(avail))
            }
        }
    }
}

@Composable
private fun StorageLegend(label: String, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text("$label: $value", fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ABOUT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSettings(a: ScreenArgs) {
    val scale   = a.uiState?.textScale ?: 1f
    val pm      = a.ctx.packageManager
    val pkgInfo = try { pm.getPackageInfo(a.ctx.packageName, 0) } catch (_: Exception) { null }
    val am      = a.ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

    SettingsGroup("Device", a) {
        listOf(
            "Device name"     to Build.MODEL,
            "Manufacturer"    to Build.MANUFACTURER,
            "Android version" to Build.VERSION.RELEASE,
            "API level"       to Build.VERSION.SDK_INT.toString(),
            "Processor"       to Build.HARDWARE,
            "Total RAM"       to fmtBytes(memInfo.totalMem),
            "Available RAM"   to fmtBytes(memInfo.availMem)
        ).forEachIndexed { i, (label, value) ->
            if (i > 0) Divider(color = a.textColor.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(label, color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * scale).sp, modifier = Modifier.width(140.dp))
                Text(value, color = a.textColor, fontSize = (12 * scale).sp)
            }
        }
    }

    SettingsGroup("Bluebird Launcher", a) {
        listOf(
            "Version" to (pkgInfo?.versionName ?: "1.0"),
            "Build"   to (pkgInfo?.longVersionCode?.toString() ?: "1"),
            "Package" to a.ctx.packageName
        ).forEachIndexed { i, (label, value) ->
            if (i > 0) Divider(color = a.textColor.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(label, color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * scale).sp, modifier = Modifier.width(140.dp))
                Text(value, color = a.textColor, fontSize = (12 * scale).sp)
            }
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        // Contact info moved to structured nav items, not in subtitle strings
        SNav(Icons.Default.Star,      "Rate this launcher",   "Leave a review or send feedback",         a = a) {
            a.ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:trebronwayne@gmail.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.BugReport, "Report a bug",         "Send a bug report to the developer",      a = a) {
            a.ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:nlamn.dev@outlook.com?subject=Bug%20Report")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Policy,    "Privacy policy",       "All data is stored locally on your device", a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(Icons.Default.Gavel,     "Open source licenses", "Third-party library attributions",         a = a)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UTILITY
// ─────────────────────────────────────────────────────────────────────────────

private fun fmtBytes(bytes: Long): String = when {
    bytes <= 0L              -> "0 B"
    bytes >= 1_073_741_824L  -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L      -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L          -> "%.1f KB".format(bytes / 1_024.0)
    else                     -> "$bytes B"
}
