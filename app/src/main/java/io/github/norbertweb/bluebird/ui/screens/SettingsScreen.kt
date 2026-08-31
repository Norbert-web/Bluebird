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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
// FluentIcons.Regular.X is written in code (see usage throughout this file), but — same
// pattern as androidx.compose.material.icons.extended — each icon is a separate
// tree-shakeable extension property that needs its OWN import; a single blanket
// `import ...FluentIcons` is not enough. Package root and per-icon import path below
// per confirmed working example from another file in this project.
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Accessibility
import fluent.ui.system.icons.regular.Add
import fluent.ui.system.icons.regular.Airplane
import fluent.ui.system.icons.regular.Alert
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.ArrowClockwise
import fluent.ui.system.icons.regular.ArrowCounterclockwise
import fluent.ui.system.icons.regular.ArrowDownload
import fluent.ui.system.icons.regular.ArrowRight
import fluent.ui.system.icons.regular.ArrowSort
import fluent.ui.system.icons.regular.ArrowSync
import fluent.ui.system.icons.regular.BatterySaver
import fluent.ui.system.icons.regular.Bluetooth
import fluent.ui.system.icons.regular.Blur
import fluent.ui.system.icons.regular.BorderAll
import fluent.ui.system.icons.regular.Bug
import fluent.ui.system.icons.regular.CalendarLtr
import fluent.ui.system.icons.regular.Call
import fluent.ui.system.icons.regular.Camera
import fluent.ui.system.icons.regular.CameraAdd
import fluent.ui.system.icons.regular.Cast
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.CircleHalfFill
import fluent.ui.system.icons.regular.ClipboardPaste
import fluent.ui.system.icons.regular.Clock
import fluent.ui.system.icons.regular.ClosedCaption
import fluent.ui.system.icons.regular.Cloud
import fluent.ui.system.icons.regular.CloudArrowUp
import fluent.ui.system.icons.regular.Cursor
import fluent.ui.system.icons.regular.DarkTheme
import fluent.ui.system.icons.regular.DataBarHorizontal
import fluent.ui.system.icons.regular.DataTrending
import fluent.ui.system.icons.regular.DataUsage
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Desktop
import fluent.ui.system.icons.regular.Dismiss
import fluent.ui.system.icons.regular.DocumentText
import fluent.ui.system.icons.regular.Edit
import fluent.ui.system.icons.regular.EyeOff
import fluent.ui.system.icons.regular.Fingerprint
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.Games
import fluent.ui.system.icons.regular.Globe
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.History
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.ImageMultiple
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Key
import fluent.ui.system.icons.regular.Keyboard
import fluent.ui.system.icons.regular.LocalLanguage
import fluent.ui.system.icons.regular.Location
import fluent.ui.system.icons.regular.LockClosedKey
import fluent.ui.system.icons.regular.Mail
import fluent.ui.system.icons.regular.Map
import fluent.ui.system.icons.regular.Mic
import fluent.ui.system.icons.regular.MusicNote1
import fluent.ui.system.icons.regular.PaintBrush
import fluent.ui.system.icons.regular.People
import fluent.ui.system.icons.regular.Person
import fluent.ui.system.icons.regular.PersonCircle
import fluent.ui.system.icons.regular.PersonSettings
import fluent.ui.system.icons.regular.Phone
import fluent.ui.system.icons.regular.PhoneVibrate
import fluent.ui.system.icons.regular.PlayCircle
import fluent.ui.system.icons.regular.Power
import fluent.ui.system.icons.regular.Print
import fluent.ui.system.icons.regular.Prohibited
import fluent.ui.system.icons.regular.RadioButton
import fluent.ui.system.icons.regular.Resize
import fluent.ui.system.icons.regular.RibbonStar
import fluent.ui.system.icons.regular.Router
import fluent.ui.system.icons.regular.Scan
import fluent.ui.system.icons.regular.Screenshot
import fluent.ui.system.icons.regular.Search
import fluent.ui.system.icons.regular.Server
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Shield
import fluent.ui.system.icons.regular.Sparkle
import fluent.ui.system.icons.regular.Speaker1
import fluent.ui.system.icons.regular.Speaker2
import fluent.ui.system.icons.regular.Star
import fluent.ui.system.icons.regular.SwipeDown
import fluent.ui.system.icons.regular.SwipeRight
import fluent.ui.system.icons.regular.SwipeUp
import fluent.ui.system.icons.regular.Tag
import fluent.ui.system.icons.regular.TapDouble
import fluent.ui.system.icons.regular.Target
import fluent.ui.system.icons.regular.TextBold
import fluent.ui.system.icons.regular.TextFont
import fluent.ui.system.icons.regular.Timer
import fluent.ui.system.icons.regular.TopSpeed
import fluent.ui.system.icons.regular.Translate
import fluent.ui.system.icons.regular.UsbStick
import fluent.ui.system.icons.regular.WeatherMoon
import fluent.ui.system.icons.regular.WeatherSunny

import fluent.ui.system.icons.regular.ZoomIn
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
    SYSTEM          ("System",              FluentIcons.Regular.Settings),
    SOUND           ("Sound",               FluentIcons.Regular.Speaker2),
    BLUETOOTH       ("Bluetooth & devices", FluentIcons.Regular.Bluetooth),
    NETWORK         ("Network & internet",  FluentIcons.Regular.Bluetooth),
    APPEARANCE      ("Appearance",          FluentIcons.Regular.PaintBrush),
    GESTURES        ("Gestures",            FluentIcons.Regular.SwipeRight),
    APPS            ("Apps",                FluentIcons.Regular.Apps),
    ACCOUNTS        ("Accounts",            FluentIcons.Regular.PersonCircle),
    TIME_LANGUAGE   ("Time & language",     FluentIcons.Regular.LocalLanguage),
    GAMING          ("Gaming",              FluentIcons.Regular.Games),
    ACCESSIBILITY   ("Accessibility",       FluentIcons.Regular.Accessibility),
    PRIVACY         ("Privacy & security",  FluentIcons.Regular.LockClosedKey),
    BACKUP          ("Backup & restore",    FluentIcons.Regular.CloudArrowUp),
    SEARCH          ("Search",              FluentIcons.Regular.Search),
    UPDATE          ("Launcher Update",     FluentIcons.Regular.ArrowClockwise),
    ABOUT           ("About",               FluentIcons.Regular.Info)
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT SCREEN
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolved theme palette for the current appTheme/dark-mode combination.
 * Wrapped in `remember` (keyed on the two inputs that actually change it) so the
 * palette isn't recomputed — and a fresh List<Color> destructured — on every
 * recomposition of the screen, only when the theme actually changes.
 */
private data class SettingsPalette(val bg: Color, val nav: Color, val surface: Color, val text: Color, val accent: Color)

@Composable
fun SettingsScreen(isDark: Boolean, viewModel: LauncherViewModel? = null) {
    val context  = LocalContext.current
    val uiState  = viewModel?.uiState?.collectAsState()?.value

    val effectiveDark  = uiState?.isDarkTheme ?: isDark
    // Windows 11 "Mica" palette — softer neutrals than pure black/white, with the
    // nav rail one shade off the content pane like the real Settings app.
    val textColor      = if (effectiveDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val bgColor        = if (effectiveDark) Color(0xFF202020)         else Color(0xFFF3F3F3)
    val surfaceBg      = if (effectiveDark) Color(0xFF2C2C2C)         else Color(0xFFFBFBFB)
    val navBg          = if (effectiveDark) Color(0xFF272727)         else Color(0xFFF9F9F9)

    val palette = remember(uiState?.appTheme, effectiveDark) {
        when (uiState?.appTheme) {
            AppTheme.SPECIAL -> SettingsPalette(
                bg = Color(0xFF0E0820), nav = Color(0xFF130A2E), surface = Color(0xFF1C1040),
                text = Color(0xFFE8DEFF), accent = Color(0xFF9C6BF7)
            )
            else -> SettingsPalette(
                bg = bgColor, nav = navBg, surface = surfaceBg, text = textColor,
                accent = bluebirdColors.AccentBlue
            )
        }
    }
    val resolvedBg      = palette.bg
    val resolvedNav     = palette.nav
    val resolvedSurface = palette.surface
    val resolvedText    = palette.text
    val specialAccent   = palette.accent

    var selectedCategory by remember { mutableStateOf(SettingsCategory.SYSTEM) }
    var navSearch by remember { mutableStateOf("") }
    val visibleCategories = remember(navSearch) {
        if (navSearch.isBlank()) SettingsCategory.entries.toList()
        else SettingsCategory.entries.filter { it.label.contains(navSearch, ignoreCase = true) }
    }

    Row(modifier = Modifier.fillMaxSize().background(resolvedBg)) {

        // ── Left nav pane ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(256.dp)
                .fillMaxHeight()
                .background(resolvedNav)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (uiState != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { selectedCategory = SettingsCategory.ACCOUNTS }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(40.dp)) {
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
                                Icon(FluentIcons.Regular.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                }
                Spacer(Modifier.height(8.dp))

                // Windows 11 style "Find a setting" search box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(resolvedBg)
                        .border(1.dp, resolvedText.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(FluentIcons.Regular.Search, null, tint = resolvedText.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = navSearch,
                        onValueChange = { navSearch = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = resolvedText, fontSize = (13 * (uiState?.textScale ?: 1f)).sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (navSearch.isEmpty()) Text("Find a setting", color = resolvedText.copy(alpha = 0.4f), fontSize = (13 * (uiState?.textScale ?: 1f)).sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(visibleCategories, key = { it.name }) { cat ->
                val isSelected = selectedCategory == cat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) specialAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selected-item accent bar, matching the real Settings app's nav rail
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (isSelected) 16.dp else 0.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) specialAccent else Color.Transparent)
                    )
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
        }

        // ── Right content area ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(resolvedBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(selectedCategory.icon, null, tint = resolvedText, modifier = Modifier.size(24.dp))
                Text(selectedCategory.label,
                    style      = MaterialTheme.typography.headlineSmall,
                    color      = resolvedText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = (24 * (uiState?.textScale ?: 1f)).sp)
            }
            Spacer(Modifier.height(2.dp))

            val args = ScreenArgs(effectiveDark, resolvedText, resolvedSurface, specialAccent, uiState, viewModel, context,
                scale = uiState?.textScale ?: 1f)

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

/**
 * `FluentIcons.Regular.X` / `FluentIcons.Filled.X` (from io.github.niyajali:fluentui-system-icons)
 * are plain top-level ImageVector vals — same shape as the old `Icons.Outlined.X` —
 * so every existing `icon: ImageVector` parameter throughout this file keeps working
 * unchanged, and there's no @Composable/resource-loading indirection needed (unlike
 * the drawable-resource approach this replaced).
 *
 * NOTE: the exact names below were derived by hand from Fluent's icon-slug naming
 * convention and were not verified against a compiler — if Android Studio underlines
 * one in red, autocomplete on `FluentIcons.Regular.` to find the closest real name;
 * it's a one-line fix wherever it appears. The import path is also a best guess —
 * adjust it to whatever your IDE resolves the library's actual package to.
 */

private data class ThemeOption(val theme: AppTheme, val icon: ImageVector, val name: String, val desc: String, val bg: Color, val accent: Color)

internal data class ScreenArgs(
    val isDark    : Boolean,
    val textColor : Color,
    val surfaceBg : Color,
    val accent    : Color,
    val uiState   : LauncherUiState?,
    val vm        : LauncherViewModel?,
    val ctx       : Context,
    // Computed once per screen instead of re-deriving `uiState?.textScale ?: 1f`
    // (a null-check + boxing) at every one of the ~150 call sites that need it.
    val scale     : Float = uiState?.textScale ?: 1f
)

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SettingsGroup(title: String, a: ScreenArgs, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title,
            color      = a.textColor.copy(alpha = 0.65f),
            fontSize   = (12 * (a.scale)).sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(start = 2.dp, bottom = 2.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = a.surfaceBg),
            shape  = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, a.textColor.copy(alpha = 0.08f))
        ) { Column(modifier = Modifier.padding(2.dp), content = content) }
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
            .clip(RoundedCornerShape(4.dp))
            .clickable { onToggle() }           // ← FIX: whole row tappable
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = a.textColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (14 * (a.scale)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.55f), fontSize = (12 * (a.scale)).sp)
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
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = a.textColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (14 * (a.scale)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.55f), fontSize = (12 * (a.scale)).sp)
        }
        if (value.isNotEmpty()) Text(value, color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * (a.scale)).sp)
        Icon(FluentIcons.Regular.ChevronRight, null, tint = a.textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
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
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = a.textColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = a.textColor, fontSize = (14 * (a.scale)).sp)
                if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.55f), fontSize = (12 * (a.scale)).sp)
            }
            Text(valueLabel, color = a.textColor.copy(alpha = 0.6f), fontSize = (12 * (a.scale)).sp)
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
            .clip(RoundedCornerShape(4.dp))
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = a.textColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = a.textColor, fontSize = (14 * (a.scale)).sp)
            if (sub.isNotEmpty()) Text(sub, color = a.textColor.copy(alpha = 0.55f), fontSize = (12 * (a.scale)).sp)
        }
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, a.textColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .background(a.textColor.copy(alpha = 0.04f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(selected, color = a.textColor, fontSize = (12 * (a.scale)).sp)
                Spacer(Modifier.width(4.dp))
                Icon(FluentIcons.Regular.ChevronDown, null, tint = a.textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text        = { Text(option, fontSize = (13 * (a.scale)).sp) },
                        onClick     = { onSelect(option); expanded = false },
                        leadingIcon = if (option == selected) ({
                            Icon(FluentIcons.Regular.Checkmark, null, tint = a.accent, modifier = Modifier.size(14.dp))
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
        SToggle(FluentIcons.Regular.PlayCircle, "Launch on boot",
            "Start launcher automatically when device starts",
            a, a.uiState?.launchOnBoot ?: false) { a.vm?.setLaunchOnBoot(!(a.uiState?.launchOnBoot ?: false)) }
    }

    SettingsGroup("Multitasking", a) {
        SToggle(FluentIcons.Regular.Grid, "Snap layouts",
            "Snap windows to screen zones",
            a, a.uiState?.snapLayouts ?: true) { a.vm?.setSnapLayouts(!(a.uiState?.snapLayouts ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.ClipboardPaste, "Clipboard history",
            "Keep a history of copied items",
            a, a.uiState?.clipboardHistory ?: true) { a.vm?.setClipboardHistory(!(a.uiState?.clipboardHistory ?: true)) }
    }

    SettingsGroup("Recent apps", a) {
        SDropdown(FluentIcons.Regular.History, "Recent app limit",
            "How many apps appear in the recents tray",
            listOf("5", "10", "15", "20", "Unlimited"),
            a.uiState?.recentAppsLimit ?: "10", a) { a.vm?.setRecentAppsLimit(it) }
    }

    SettingsGroup("Notifications", a) {
        SToggle(FluentIcons.Regular.Alert, "Notification banners",
            "Show pop-up banners for new notifications",
            a, a.uiState?.notificationBanners ?: true) { a.vm?.setNotificationBanners(!(a.uiState?.notificationBanners ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.RibbonStar, "Notification badges",
            "Show red dot on app icons",
            a, a.uiState?.showNotificationBadges ?: true) { a.vm?.setShowNotificationBadges(!(a.uiState?.showNotificationBadges ?: true)) }
    }

    SettingsGroup("Do Not Disturb", a) {
        SToggle(FluentIcons.Regular.Prohibited, "Do Not Disturb",
            "Silence all notifications",
            a, a.uiState?.dndEnabled ?: false) { a.vm?.setDndEnabled(!(a.uiState?.dndEnabled ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Clock, "Scheduled DND",
            "Auto-enable between set hours",
            a, a.uiState?.dndScheduled ?: false) { a.vm?.setDndScheduled(!(a.uiState?.dndScheduled ?: false)) }
        if (a.uiState?.dndScheduled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(FluentIcons.Regular.WeatherMoon, null, tint = a.accent, modifier = Modifier.size(16.dp))
                Text("From ${a.uiState.dndStartHour}:00 to ${a.uiState.dndEndHour}:00",
                    color    = a.textColor.copy(alpha = 0.7f),
                    fontSize = (12 * a.uiState.textScale).sp)
            }
        }
    }

    SettingsGroup("Focus Assist", a) {
        SToggle(FluentIcons.Regular.Target, "Focus assist",
            "Allow only priority notifications",
            a, a.uiState?.focusAssist ?: false) { a.vm?.setFocusAssist(!(a.uiState?.focusAssist ?: false)) }
    }

    SettingsGroup("Battery & Power", a) {
        SToggle(FluentIcons.Regular.BatterySaver, "Battery saver",
            "Limit background activity to extend battery life",
            a, a.uiState?.batterySaver ?: false) { a.vm?.setBatterySaver(!(a.uiState?.batterySaver ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.Power, "Screen timeout",
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
        SSlider(FluentIcons.Regular.MusicNote1, "Media volume", "Music, videos, games",
            a.uiState?.mediaVolume ?: 0.7f, 0f..1f,
            "${((a.uiState?.mediaVolume ?: 0.7f) * 100).toInt()}%", a) { a.vm?.setMediaVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(FluentIcons.Regular.Alert, "Notification volume", "",
            a.uiState?.notifVolume ?: 0.6f, 0f..1f,
            "${((a.uiState?.notifVolume ?: 0.6f) * 100).toInt()}%", a) { a.vm?.setNotifVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(FluentIcons.Regular.Call, "Ring volume", "",
            a.uiState?.ringtoneVolume ?: 0.5f, 0f..1f,
            "${((a.uiState?.ringtoneVolume ?: 0.5f) * 100).toInt()}%", a) { a.vm?.setRingtoneVolume(it, a.ctx) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(FluentIcons.Regular.Clock, "Alarm volume", "",
            a.uiState?.alarmVolume ?: 0.8f, 0f..1f,
            "${((a.uiState?.alarmVolume ?: 0.8f) * 100).toInt()}%", a) { a.vm?.setAlarmVolume(it, a.ctx) }
    }

    SettingsGroup("Sound preferences", a) {
        SToggle(FluentIcons.Regular.Speaker2, "System sounds", "UI click and action sounds",
            a, a.uiState?.systemSounds ?: true) { a.vm?.setSystemSounds(!(a.uiState?.systemSounds ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.PhoneVibrate, "Haptic feedback", "Vibrate on touch",
            a, a.uiState?.hapticFeedback ?: true) { a.vm?.setHapticFeedback(!(a.uiState?.hapticFeedback ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Speaker1, "Media volume keys",
            "Volume keys control media by default",
            a, a.uiState?.volumeKeysMedia ?: true) { a.vm?.setVolumeKeysMedia(!(a.uiState?.volumeKeysMedia ?: true)) }
    }

    SettingsGroup("Notification sound", a) {
        SDropdown(FluentIcons.Regular.Alert, "Default notification sound", "",
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
        SToggle(FluentIcons.Regular.Bluetooth, "Bluetooth", "Enable Bluetooth radio",
            a, a.uiState?.isBluetoothOn ?: false) {
            a.vm?.openBluetoothSettings(a.ctx)
            a.vm?.toggleBluetooth()
        }
    }

    SettingsGroup("Connected devices", a) {
        if (a.uiState?.isBluetoothOn == true) {
            SNav(FluentIcons.Regular.Bluetooth, "Pair new device", "Scan for nearby Bluetooth devices", a = a) {
                a.vm?.openBluetoothSettings(a.ctx)
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                if (a.uiState?.isBluetoothOn == true) "No devices paired yet"
                else "Enable Bluetooth to see devices",
                color    = a.textColor.copy(alpha = 0.4f),
                fontSize = (12 * (a.scale)).sp
            )
        }
    }

    SettingsGroup("Other devices", a) {
        SNav(FluentIcons.Regular.Cursor, "Mouse & touchpad", "Pointer speed, buttons", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Keyboard, "Keyboard", "Layout, language, shortcuts", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Print, "Printers & scanners", "Manage connected printers", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.UsbStick, "USB", "USB preferences and connected drives", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Cast, "Wireless displays", "Connect to a screen or TV wirelessly", a = a) {
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
        SToggle(FluentIcons.Regular.Bluetooth, "Wi-Fi",
            if (a.uiState?.isWifiOn == true) "Connected" else "Off",
            a, a.uiState?.isWifiOn ?: false) {
            a.vm?.openWifiSettings(a.ctx)
            a.vm?.toggleWifi()
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Bluetooth, "Manage networks", "Saved, available Wi-Fi networks", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("Mobile & data", a) {
        SToggle(FluentIcons.Regular.DataUsage, "Data saver",
            "Restrict background mobile data",
            a, a.uiState?.dataSaver ?: false) { a.vm?.setDataSaver(!(a.uiState?.dataSaver ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Router, "Mobile hotspot",
            "Share internet with other devices",
            a, a.uiState?.hotspotEnabled ?: false) { a.vm?.setHotspot(!(a.uiState?.hotspotEnabled ?: false)) }
        if (a.uiState?.hotspotEnabled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcons.Regular.Phone, null, tint = a.accent, modifier = Modifier.size(14.dp))
                Text("0 devices connected", color = a.textColor.copy(alpha = 0.6f),
                    fontSize = (11 * (a.uiState.textScale)).sp)
            }
        }
    }

    SettingsGroup("Airplane mode", a) {
        SToggle(FluentIcons.Regular.Airplane, "Airplane mode",
            "Disable all wireless communications",
            a, a.uiState?.isAirplaneMode ?: false) { a.vm?.toggleAirplaneMode() }
    }

    SettingsGroup("VPN", a) {
        SToggle(FluentIcons.Regular.Key, "VPN",
            if (a.uiState?.vpnEnabled == true) "Connected" else "Not connected",
            a, a.uiState?.vpnEnabled ?: false) { a.vm?.setVpn(!(a.uiState?.vpnEnabled ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Add, "Add a VPN", "Configure a new VPN connection", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("DNS", a) {
        SToggle(FluentIcons.Regular.Server, "Custom DNS", "Use a custom DNS server address",
            a, a.uiState?.customDns ?: false) { a.vm?.setCustomDns(!(a.uiState?.customDns ?: false)) }
        if (a.uiState?.customDns == true) {
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SDropdown(FluentIcons.Regular.LocalLanguage, "DNS server", "Choose a preset or enter custom",
                listOf("8.8.8.8 (Google)", "1.1.1.1 (Cloudflare)", "9.9.9.9 (Quad9)", "8.8.4.4 (Google alt)", "3.1.1.1 (EAT)"),
                a.uiState.dnsAddress, a) { a.vm?.setDnsAddress(it) }
        }
    }

    SettingsGroup("Proxy", a) {
        SNav(FluentIcons.Regular.Settings, "Proxy settings",
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
            Text("Choose a theme", color = a.textColor, fontSize = (13 * (a.scale)).sp, fontWeight = FontWeight.Medium)
            Text("Controls how the entire launcher looks", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.scale)).sp)
            Spacer(Modifier.height(4.dp))

            // PERF FIX: this list of 5 data-class instances was being reallocated on
            // every recomposition of the Appearance screen. It's static, so build it
            // once and reuse it. FluentIcons.Regular.X are plain vals (like the old
            // Icons.Outlined.X), so no @Composable-call restriction applies here.
            val themes = remember {
                listOf(
                    ThemeOption(AppTheme.SYSTEM,  FluentIcons.Regular.Desktop,      "System",    "Follows Android system dark/light",       Color(0xFF1F1F1F), Color(0xFF0078D4)),
                    ThemeOption(AppTheme.FOR_YOU, FluentIcons.Regular.Sparkle,      "For You",   "Adapts accent colors from your wallpaper", Color(0xFF2A2016), Color(0xFFD4A017)),
                    ThemeOption(AppTheme.DARK,    FluentIcons.Regular.DarkTheme,    "Dark",      "Always dark",                             Color(0xFF121212), Color(0xFF4FC3F7)),
                    ThemeOption(AppTheme.LIGHT,   FluentIcons.Regular.WeatherSunny, "Light",     "Always light and crisp",                  Color(0xFFF5F5F5), Color(0xFF1565C0)),
                    ThemeOption(AppTheme.SPECIAL, FluentIcons.Regular.Star,         "Special ✦", "Deep indigo — By LAMN-NOBERT",            Color(0xFF12092A), Color(0xFF9C6BF7))
                )
            }
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
                            fontSize   = (10 * (a.scale)).sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            AnimatedContent(targetState = currentTheme, label = "theme_desc") { t ->
                Text(themes.find { it.theme == t }?.desc ?: "", color = a.textColor.copy(alpha = 0.5f), fontSize = (11 * (a.scale)).sp)
            }
        }
    }

    // Dark mode schedule
    SettingsGroup("Dark mode schedule", a) {
        SDropdown(FluentIcons.Regular.WeatherMoon, "Auto dark mode",
            "Automatically switch theme by time or system",
            listOf("Disabled", "Sunset to sunrise", "Custom hours", "Follow system"),
            a.uiState?.darkModeSchedule ?: "Follow system", a) { a.vm?.setDarkModeSchedule(it) }
    }

    // Wallpaper
    SettingsGroup("Background", a) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Desktop wallpaper", color = a.textColor, fontSize = (13 * (a.scale)).sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                wallpaperGradients.forEachIndexed { index, gradient ->
                    val isSelected = a.uiState?.wallpaper?.homeWallpaperIndex == index && a.uiState.wallpaper.homeWallpaperUri.isEmpty()
                    Box(
                        modifier = Modifier.size(56.dp, 36.dp).clip(RoundedCornerShape(4.dp))
                            .background(Brush.linearGradient(gradient))
                            .border(if (isSelected) 2.dp else 0.dp, a.accent, RoundedCornerShape(4.dp))
                            .clickable { a.vm?.setBuiltInWallpaper(index, WallpaperTarget.HOME) },
                        contentAlignment = Alignment.Center
                    ) { if (isSelected) Icon(FluentIcons.Regular.Checkmark, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
            SToggle(FluentIcons.Regular.ImageMultiple, "Wallpaper slideshow",
                "Rotate wallpaper automatically",
                a, a.uiState?.wallpaperSlideshow ?: false) { a.vm?.setWallpaperSlideshow(!(a.uiState?.wallpaperSlideshow ?: false)) }
            if (a.uiState?.wallpaperSlideshow == true) {
                SDropdown(FluentIcons.Regular.Timer, "Change every", "",
                    listOf("15 minutes", "30 minutes", "1 hour", "6 hours", "Daily"),
                    a.uiState.wallpaperSlideshowInterval ?: "1 hour", a) { a.vm?.setWallpaperSlideshowInterval(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { a.vm?.openWallpaperPicker(WallpaperTarget.HOME) },
                    border  = BorderStroke(1.dp, a.accent),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = a.accent)
                ) {
                    Icon(FluentIcons.Regular.Image, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Home screen", fontSize = (12 * (a.scale)).sp)
                }
                OutlinedButton(
                    onClick = { a.vm?.openWallpaperPicker(WallpaperTarget.LOCK_SCREEN) },
                    border  = BorderStroke(1.dp, a.accent),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = a.accent)
                ) {
                    Icon(FluentIcons.Regular.LockClosedKey, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock screen", fontSize = (12 * (a.scale)).sp)
                }
            }
        }
    }

    // Accent color
    SettingsGroup("Accent color", a) {
        val accentColors = remember {
            listOf(0xFF0078D4L, 0xFF107C10L, 0xFFE81123L, 0xFFFF8C00L, 0xFF744DA9L,
                0xFF00B7C3L, 0xFFE74856L, 0xFF7A7574L, 0xFF9C6BF7L, 0xFF10A0E3L, 0xFF00CC6AL, 0xFFFF6B35L)
        }
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
                        if (isSelected) Icon(FluentIcons.Regular.Checkmark, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    // Text size
    SettingsGroup("Text size", a) {
        val scale = a.scale
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(FluentIcons.Regular.TextFont, null, tint = a.accent, modifier = Modifier.size(20.dp))
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
        SDropdown(FluentIcons.Regular.TextFont, "System font",
            "Choose the font used across the launcher",
            listOf("Default (Roboto)", "Sans-serif", "Serif", "Monospace", "Cursive"),
            a.uiState?.launcherFont ?: "Default (Roboto)", a) { a.vm?.setLauncherFont(it) }
    }

    // Icon size
    SettingsGroup("Icon size", a) {
        SDropdown(FluentIcons.Regular.Apps, "Desktop icon size",
            "Size of app icons on the home screen",
            listOf("Small", "Medium", "Large", "Extra Large"),
            a.uiState?.iconSize ?: "Medium", a) { a.vm?.setIconSize(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.BorderAll, "Grid size",
            "Columns × rows on the home screen",
            listOf("4 × 5", "4 × 6", "5 × 5", "5 × 6", "6 × 6"),
            a.uiState?.gridSize ?: "4 × 5", a) { a.vm?.setGridSize(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Tag, "Show app labels",
            "Display app names below icons",
            a, a.uiState?.showAppLabels ?: true) { a.vm?.setShowAppLabels(!(a.uiState?.showAppLabels ?: true)) }
    }

    // Corner radius
    SettingsGroup("Shape & corners", a) {
        SSlider(FluentIcons.Regular.Resize, "Corner radius",
            "Roundness of cards and menus",
            a.uiState?.cornerRadius ?: 0.5f, 0f..1f,
            when { (a.uiState?.cornerRadius ?: 0.5f) < 0.25f -> "Sharp"; (a.uiState?.cornerRadius ?: 0.5f) < 0.6f -> "Rounded"; else -> "Pill" },
            a) { a.vm?.setCornerRadius(it) }
    }

    // Visual effects
    SettingsGroup("Visual effects", a) {
        SToggle(FluentIcons.Regular.Blur, "Transparency & blur",
            "Acrylic/blur effect on panels",
            a, a.uiState?.transparencyEffects ?: true) { a.vm?.setTransparencyEffects(!(a.uiState?.transparencyEffects ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(FluentIcons.Regular.TopSpeed, "Animation speed",
            "Controls launcher transition speed",
            a.uiState?.animationSpeed ?: 1f, 0f..2f,
            when { (a.uiState?.animationSpeed ?: 1f) < 0.1f -> "Off"; (a.uiState?.animationSpeed ?: 1f) < 0.75f -> "Slow"; (a.uiState?.animationSpeed ?: 1f) < 1.25f -> "Normal"; else -> "Fast" },
            a) { a.vm?.setAnimationSpeed(it) }
    }

    // Layout
    SettingsGroup("Layout", a) {
        SDropdown(FluentIcons.Regular.Grid, "Taskbar position",
            "Where the taskbar appears on screen",
            listOf("Bottom", "Left", "Right"),
            a.uiState?.taskbarPosition ?: "Bottom", a) { a.vm?.setTaskbarPosition(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.Grid, "Start menu layout",
            "Balance of pinned apps vs. recommendations",
            listOf("More pins", "Balanced", "More recommendations"),
            a.uiState?.startMenuLayout ?: "Balanced", a) { a.vm?.setStartMenuLayout(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.ArrowSort, "Status bar clock position",
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
        SDropdown(FluentIcons.Regular.SwipeUp, "Swipe up",
            "Action when swiping up on the home screen",
            listOf("App drawer", "Notification shade", "Search", "Nothing"),
            a.uiState?.gestureSwipeUp ?: "App drawer", a) { a.vm?.setGestureSwipeUp(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.SwipeDown, "Swipe down",
            "Action when swiping down on the home screen",
            listOf("Notification shade", "Quick settings", "Search", "Nothing"),
            a.uiState?.gestureSwipeDown ?: "Notification shade", a) { a.vm?.setGestureSwipeDown(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.TapDouble, "Double-tap home",
            "Action when double-tapping on empty space",
            listOf("Lock screen", "Search", "Nothing"),
            a.uiState?.gestureDoubleTap ?: "Lock screen", a) { a.vm?.setGestureDoubleTap(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.ZoomIn, "Pinch to open",
            "Pinch gesture on the home screen",
            listOf("App drawer", "Overview", "Nothing"),
            a.uiState?.gesturePinch ?: "Overview", a) { a.vm?.setGesturePinch(it) }
    }

    SettingsGroup("App icon gestures", a) {
        SToggle(FluentIcons.Regular.SwipeRight, "Swipe-up shortcut on icons",
            "Assign an action to swiping up on any app icon",
            a, a.uiState?.iconSwipeUpEnabled ?: false) { a.vm?.setIconSwipeUp(!(a.uiState?.iconSwipeUpEnabled ?: false)) }
    }

    SettingsGroup("Navigation gestures", a) {
        SToggle(FluentIcons.Regular.ArrowRight, "Navigation bar gestures",
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
    val scale = a.scale

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
        SDropdown(FluentIcons.Regular.ArrowSort, "Sort order",
            "How apps appear in the drawer",
            listOf("A–Z", "Z–A", "Most used", "Recently installed"),
            sortOrder, a) { sortOrder = it }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.EyeOff, "Hide apps",
            "Choose apps to hide from the drawer",
            a, a.uiState?.hideAppsEnabled ?: false) { a.vm?.setHideApps(!(a.uiState?.hideAppsEnabled ?: false)) }
    }

    SettingsGroup("Search apps (${filtered.size} / ${apps.size})", a) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(FluentIcons.Regular.Search, null, tint = a.textColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
                Icon(FluentIcons.Regular.Dismiss, null,
                    tint     = a.textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
            }
        }
    }

    // PERF FIX: this used to be a plain Column + verticalScroll, which composes and
    // measures a row for *every* installed app up front even though only ~10 are
    // ever visible at once (on a phone with 150+ apps that's 150+ Image/Text/Icon
    // trees built for nothing). A LazyColumn only composes what's on screen, and a
    // stable `key` means re-filtering/re-sorting doesn't have to rebuild rows that
    // didn't change position. It still works nested inside the outer verticalScroll
    // Column because it's height-bounded (heightIn(max = 360.dp)) instead of
    // fillMaxHeight — a bounded LazyColumn inside a scrollable parent is safe;
    // an unbounded one is what causes the classic "infinite constraints" crash.
    SettingsGroup("Installed apps", a) {
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(filtered, key = { it.activityInfo.packageName }) { info ->
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
                    Icon(FluentIcons.Regular.ChevronRight, null, tint = a.textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                }
                Divider(color = divColor(a).copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }

    SettingsGroup("App defaults", a) {
        SNav(FluentIcons.Regular.Globe, "Default browser",  "Choose which browser opens links",       a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Mail,          "Default email app", "Choose which app handles mailto links", a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Map,            "Default map app",   "Choose map application",                a = a)
    }

    SettingsGroup("Install sources", a) {
        SNav(FluentIcons.Regular.Shield, "Install unknown apps", "Allow sideloading from other sources", a = a) {
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
        icon = { Icon(FluentIcons.Regular.Edit, contentDescription = null) },
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
    val scale = a.scale
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
                            Icon(FluentIcons.Regular.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(a.uiState.userProfile.userName, color = a.textColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Medium)
                    Text("Local Account", color = a.textColor.copy(alpha = 0.5f), fontSize = (12 * scale).sp)
                }
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SNav(FluentIcons.Regular.Edit,      "Change username",        "Update your display name",         a = a) {
                showRenameDialog = true
            }
            Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
            SNav(FluentIcons.Regular.CameraAdd, "Change profile picture", "Pick a photo from your gallery",   a = a) {
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
        SNav(FluentIcons.Regular.Key,         "PIN",            "Set up a numeric PIN for quick sign-in",      a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Grid,     "Screen pattern", "Draw a pattern to unlock",                    a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Key,    "Password",       "Use a full text password",                    a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Fingerprint, "Fingerprint",    "Register fingerprint for biometric unlock",   a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_FINGERPRINT_ENROLL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Scan,        "Face unlock",    "Use facial recognition",                      a = a)
    }

    SettingsGroup("Linked accounts", a) {
        SNav(FluentIcons.Regular.Person,  "Google account",    "Sync, apps and services",    value = "Not linked", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Cloud,       "Microsoft account", "OneDrive, Outlook and more", value = "Not linked", a = a)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TIME & LANGUAGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimeLanguageSettings(a: ScreenArgs) {
    val scale       = a.scale
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
            Icon(FluentIcons.Regular.Clock, null, tint = a.accent, modifier = Modifier.size(32.dp))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.ArrowSync,     "Set time automatically",  "Sync with internet time servers",       a, a.uiState?.autoSetTime ?: true)    { a.vm?.setAutoSetTime(!(a.uiState?.autoSetTime ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Clock, "24-hour clock",           "Use 24-hour instead of 12-hour format", a, use24h)                             { a.vm?.setUse24HourClock(!use24h) }
    }

    SettingsGroup("Timezone", a) {
        val timezones = remember {
            TimeZone.getAvailableIDs()
                .map { id -> val tz = TimeZone.getTimeZone(id); "$id (${tz.displayName})" }
                .take(80) // Reasonable limit for the dropdown
        }
        SDropdown(FluentIcons.Regular.Globe, "Timezone", "Set your local timezone",
            timezones, a.uiState?.timeZone ?: "Africa/Kampala", a) { a.vm?.setTimeZone(it) }
    }

    SettingsGroup("Date format", a) {
        SDropdown(FluentIcons.Regular.CalendarLtr, "Date format", "How dates are displayed",
            listOf("MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD", "D MMM YYYY"),
            a.uiState?.dateFormat ?: "DD/MM/YYYY", a) { a.vm?.setDateFormat(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.CalendarLtr, "First day of week", "",
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
        SNav(FluentIcons.Regular.Translate, "Add a language", "Install additional language packs", a = a) {
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
        SToggle(FluentIcons.Regular.Games, "Game Mode",
            "Prioritize CPU/GPU for games, reduce background activity",
            a, a.uiState?.gameModeEnabled ?: false) { a.vm?.setGameMode(!(a.uiState?.gameModeEnabled ?: false)) }
        if (a.uiState?.gameModeEnabled == true) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(FluentIcons.Regular.CheckmarkCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Text("Game Mode is active — background processes reduced",
                    color = a.textColor.copy(alpha = 0.6f), fontSize = (11 * (a.uiState.textScale)).sp)
            }
        }
    }

    SettingsGroup("Performance", a) {
        SDropdown(FluentIcons.Regular.TopSpeed, "Frame rate cap",
            "Maximum frames rendered per second",
            listOf("30 fps", "60 fps", "90 fps", "120 fps", "Unlimited"),
            a.uiState?.frameRateCap ?: "Unlimited", a) { a.vm?.setFrameRateCap(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.DataBarHorizontal, "Performance overlay",
            "Show FPS, CPU, and RAM while in a game",
            a, a.uiState?.performanceOverlay ?: false) { a.vm?.setPerformanceOverlay(!(a.uiState?.performanceOverlay ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.TopSpeed, "Thermal throttle protection",
            "Reduce performance to prevent overheating",
            a, a.uiState?.thermalProtection ?: true) { a.vm?.setThermalProtection(!(a.uiState?.thermalProtection ?: true)) }
    }

    SettingsGroup("During gameplay", a) {
        SToggle(FluentIcons.Regular.Prohibited, "Do Not Disturb",
            "Mute notifications while a game is fullscreen",
            a, a.uiState?.dndWhileGaming ?: true) { a.vm?.setDndWhileGaming(!(a.uiState?.dndWhileGaming ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.PhoneVibrate, "Haptic feedback",
            "Vibration effects in supported games",
            a, a.uiState?.hapticInGames ?: true) { a.vm?.setHapticInGames(!(a.uiState?.hapticInGames ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Screenshot, "Block screenshots",
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
        SToggle(FluentIcons.Regular.CircleHalfFill, "High contrast",
            "Increase contrast for better readability",
            a, a.uiState?.highContrast ?: false) { a.vm?.setHighContrast(!(a.uiState?.highContrast ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.TextFont, "Larger text",
            "Increase base text size across the launcher",
            a, a.uiState?.largerText ?: false) { a.vm?.setLargerText(!(a.uiState?.largerText ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.TextBold, "Bold text",
            "Make all text bold for easier reading",
            a, a.uiState?.boldText ?: false) { a.vm?.setBoldText(!(a.uiState?.boldText ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.PaintBrush, "Color correction",
            "Adjust colors for color vision deficiencies",
            listOf("None", "Deuteranopia (red-green)", "Protanopia (red-green alt)", "Tritanopia (blue-yellow)", "Grayscale"),
            a.uiState?.colorCorrectionMode ?: "None", a) { a.vm?.setColorCorrectionMode(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.ZoomIn, "Magnification",
            "Triple-tap to zoom anywhere on screen",
            a, a.uiState?.magnificationEnabled ?: false) { a.vm?.setMagnification(!(a.uiState?.magnificationEnabled ?: false)) }
    }

    SettingsGroup("Motion & interaction", a) {
        SToggle(FluentIcons.Regular.Sparkle, "Reduce motion",
            "Minimize animations and transitions",
            a, a.uiState?.reduceMotion ?: false) { a.vm?.setReduceMotion(!(a.uiState?.reduceMotion ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SSlider(FluentIcons.Regular.TapDouble, "Touch & hold delay",
            "How long before a long-press registers",
            a.uiState?.touchHoldDelay ?: 0.5f, 0f..1f,
            when { (a.uiState?.touchHoldDelay ?: 0.5f) < 0.33f -> "Short"; (a.uiState?.touchHoldDelay ?: 0.5f) < 0.66f -> "Medium"; else -> "Long" },
            a) { a.vm?.setTouchHoldDelay(it) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.RadioButton, "Button shapes",
            "Show outlines around tappable buttons",
            a, a.uiState?.buttonShapes ?: false) { a.vm?.setButtonShapes(!(a.uiState?.buttonShapes ?: false)) }
    }

    SettingsGroup("Audio", a) {
        SToggle(FluentIcons.Regular.Speaker1, "Mono audio",
            "Combine stereo channels into mono",
            a, a.uiState?.monoAudio ?: false) { a.vm?.setMonoAudio(!(a.uiState?.monoAudio ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.ClosedCaption, "Captions",
            "Show captions for media when available",
            a, a.uiState?.captionsEnabled ?: false) { a.vm?.setCaptionsEnabled(!(a.uiState?.captionsEnabled ?: false)) }
    }

    SettingsGroup("System", a) {
        SNav(FluentIcons.Regular.Accessibility, "More accessibility options",
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
        SToggle(FluentIcons.Regular.LockClosedKey, "Screen lock",
            "Require PIN, pattern, or password on wake",
            a, a.uiState?.screenLock ?: false) { a.vm?.setScreenLock(!(a.uiState?.screenLock ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Fingerprint, "Biometric unlock",
            "Use fingerprint or face to unlock",
            a, a.uiState?.biometricUnlock ?: false) { a.vm?.setBiometricUnlock(!(a.uiState?.biometricUnlock ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Phone, "Install unknown apps",
            "Allow apps from outside the Play Store",
            a, a.uiState?.unknownSources ?: false) { a.vm?.setUnknownSources(!(a.uiState?.unknownSources ?: false)) }
    }

    SettingsGroup("App permissions", a) {
        SToggle(FluentIcons.Regular.Location, "Location access",
            "Allow apps to request location",
            a, a.uiState?.locationAccess ?: true) { a.vm?.setLocationAccess(!(a.uiState?.locationAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Camera, "Camera access",
            "Allow apps to use the camera",
            a, a.uiState?.cameraAccess ?: true) { a.vm?.setCameraAccess(!(a.uiState?.cameraAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Mic, "Microphone access",
            "Allow apps to use the microphone",
            a, a.uiState?.micAccess ?: true) { a.vm?.setMicAccess(!(a.uiState?.micAccess ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.PersonSettings, "Manage per-app permissions",
            "Fine-grained control per application", a = a) {
            a.ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsGroup("Data & diagnostics", a) {
        SToggle(FluentIcons.Regular.DataTrending, "Usage & diagnostics",
            "Send anonymous crash reports and usage data",
            a, a.uiState?.usageDiagnostics ?: false) { a.vm?.setUsageDiagnostics(!(a.uiState?.usageDiagnostics ?: false)) }
    }

    SettingsGroup("Privacy dashboard", a) {
        SNav(FluentIcons.Regular.Shield, "View permission usage",
            "See which apps used each permission recently", a = a) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                a.ctx.startActivity(Intent(android.provider.Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Delete, "Clear app data",
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
    val scale = a.scale

    SettingsGroup("Backup", a) {
        SToggle(FluentIcons.Regular.Cloud, "Auto backup",
            "Automatically back up launcher settings to device storage",
            a, a.uiState?.autoBackup ?: false) { a.vm?.setAutoBackup(!(a.uiState?.autoBackup ?: false)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.ArrowDownload, "Export settings",
            "Save a backup file to your Downloads folder", a = a) {
            a.vm?.exportSettings(a.ctx)
        }
    }

    SettingsGroup("Restore", a) {
        SNav(FluentIcons.Regular.FolderOpen, "Import settings",
            "Restore from a previously exported backup", a = a) {
            a.vm?.importSettings(a.ctx)
        }
    }

    SettingsGroup("Reset", a) {
        var showConfirm by remember { mutableStateOf(false) }
        SNav(FluentIcons.Regular.ArrowCounterclockwise, "Reset all settings",
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
        SToggle(FluentIcons.Regular.Search, "Show search bar",
            "Display the search bar on the home screen",
            a, a.uiState?.showSearchBar ?: true) { a.vm?.setShowSearchBar(!(a.uiState?.showSearchBar ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SDropdown(FluentIcons.Regular.LocalLanguage, "Default search engine",
            "Used for web searches from the home screen",
            listOf("Google", "Bing", "DuckDuckGo", "Yahoo", "Brave Search"),
            a.uiState?.searchEngine ?: "Google", a) { a.vm?.setSearchEngine(it) }
    }

    SettingsGroup("Search results include", a) {
        SToggle(FluentIcons.Regular.Apps, "Apps",
            "Show installed apps in search results",
            a, a.uiState?.searchIncludeApps ?: true) { a.vm?.setSearchIncludeApps(!(a.uiState?.searchIncludeApps ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.People, "Contacts",
            "Show contacts in search results",
            a, a.uiState?.searchIncludeContacts ?: true) { a.vm?.setSearchIncludeContacts(!(a.uiState?.searchIncludeContacts ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Settings, "Settings",
            "Show settings pages in search results",
            a, a.uiState?.searchIncludeSettings ?: true) { a.vm?.setSearchIncludeSettings(!(a.uiState?.searchIncludeSettings ?: true)) }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SToggle(FluentIcons.Regular.Globe, "Web results",
            "Show web suggestions while typing",
            a, a.uiState?.searchWebSuggestions ?: true) { a.vm?.setSearchWebSuggestions(!(a.uiState?.searchWebSuggestions ?: true)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LAUNCHER UPDATE
// BUG FIX: SettingsCategory.UPDATE routed to this composable, but it didn't exist
// anywhere in the file — selecting "Launcher Update" would have failed to compile.
// ─────────────────────────────────────────────────────────────────────────────



// ─────────────────────────────────────────────────────────────────────────────
// STORAGE HELPER  (improved — no deprecated API)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StorageInfo(a: ScreenArgs) {
    val scale = a.scale

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
    val scale = a.scale
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
    val scale   = a.scale
    val pm      = a.ctx.packageManager
    // PERF FIX: getPackageInfo() and getMemoryInfo() are Binder IPC calls to system
    // services. They were previously run directly in the composable body, so every
    // single recomposition of this screen (e.g. from an unrelated state read) re-hit
    // both system services. `remember` runs them once and reuses the result.
    val pkgInfo = remember(a.ctx) { try { pm.getPackageInfo(a.ctx.packageName, 0) } catch (_: Exception) { null } }
    val memInfo = remember(a.ctx) {
        val am = a.ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }

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
        SNav(FluentIcons.Regular.Star,      "Rate this launcher",   "Leave a review or send feedback",         a = a) {
            a.ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:trebronwayne@gmail.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Bug, "Report a bug",         "Send a bug report to the developer",      a = a) {
            a.ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:nlamn.dev@outlook.com?subject=Bug%20Report")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.Shield,    "Privacy policy",       "All data is stored locally on your device", a = a)
        Divider(color = divColor(a), modifier = Modifier.padding(horizontal = 12.dp))
        SNav(FluentIcons.Regular.DocumentText,     "Open source licenses", "Third-party library attributions",         a = a)
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
