package io.github.norbertweb.bluebird.ui.components

import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Add
import fluent.ui.system.icons.regular.Airplane
import fluent.ui.system.icons.regular.Alert
import fluent.ui.system.icons.regular.AlertOff
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.ArrowDownload
import fluent.ui.system.icons.regular.ArrowReply
import fluent.ui.system.icons.regular.ArrowSort
import fluent.ui.system.icons.regular.ArrowSync
import fluent.ui.system.icons.regular.ArrowTrending
import fluent.ui.system.icons.regular.ArrowUpload
import fluent.ui.system.icons.regular.Battery0
import fluent.ui.system.icons.regular.Battery10
import fluent.ui.system.icons.regular.Battery3
import fluent.ui.system.icons.regular.Battery6
import fluent.ui.system.icons.regular.Battery9
import fluent.ui.system.icons.regular.BatteryCharge
import fluent.ui.system.icons.regular.Bluetooth
import fluent.ui.system.icons.regular.BrightnessHigh
import fluent.ui.system.icons.regular.Calculator
import fluent.ui.system.icons.regular.CalendarLtr
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.ChevronUp
import fluent.ui.system.icons.regular.ClipboardPaste
import fluent.ui.system.icons.regular.ClockAlarm
import fluent.ui.system.icons.regular.CloudCheckmark
import fluent.ui.system.icons.regular.CloudOff
import fluent.ui.system.icons.regular.Color
import fluent.ui.system.icons.regular.Copy
import fluent.ui.system.icons.regular.Cut
import fluent.ui.system.icons.regular.DataUsage
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Desktop
import fluent.ui.system.icons.regular.Dismiss
import fluent.ui.system.icons.regular.Document
import fluent.ui.system.icons.regular.DocumentPdf
import fluent.ui.system.icons.regular.DocumentText
import fluent.ui.system.icons.regular.Edit
import fluent.ui.system.icons.regular.EyeOff
import fluent.ui.system.icons.regular.Flash
import fluent.ui.system.icons.regular.Folder
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.FolderProhibited
import fluent.ui.system.icons.regular.FolderZip
import fluent.ui.system.icons.regular.FullScreenMaximize
import fluent.ui.system.icons.regular.FullScreenMinimize
import fluent.ui.system.icons.regular.Globe
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.ImageMultiple
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Link
import fluent.ui.system.icons.regular.List
import fluent.ui.system.icons.regular.Location
import fluent.ui.system.icons.regular.LocationOff
import fluent.ui.system.icons.regular.LockClosed
import fluent.ui.system.icons.regular.Mic
import fluent.ui.system.icons.regular.MoreHorizontal
import fluent.ui.system.icons.regular.MusicNote2
import fluent.ui.system.icons.regular.Next
import fluent.ui.system.icons.regular.Open
import fluent.ui.system.icons.regular.Options
import fluent.ui.system.icons.regular.Pause
import fluent.ui.system.icons.regular.PersonWalking
import fluent.ui.system.icons.regular.Phone
import fluent.ui.system.icons.regular.Pin
import fluent.ui.system.icons.regular.Play
import fluent.ui.system.icons.regular.PlayCircle
import fluent.ui.system.icons.regular.Power
import fluent.ui.system.icons.regular.Previous
import fluent.ui.system.icons.regular.Print
import fluent.ui.system.icons.regular.Prohibited
import fluent.ui.system.icons.regular.Rename
import fluent.ui.system.icons.regular.Resize
import fluent.ui.system.icons.regular.Search
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Share
import fluent.ui.system.icons.regular.Shield
import fluent.ui.system.icons.regular.Sparkle
import fluent.ui.system.icons.regular.Speaker1
import fluent.ui.system.icons.regular.Speaker2
import fluent.ui.system.icons.regular.SpeakerMute
import fluent.ui.system.icons.regular.Stack
import fluent.ui.system.icons.regular.Subtract
import fluent.ui.system.icons.regular.SwipeRight
import fluent.ui.system.icons.regular.TabDesktopMultiple
import fluent.ui.system.icons.regular.Table
import fluent.ui.system.icons.regular.TaskListSquareLtr
import fluent.ui.system.icons.regular.TextFont
import fluent.ui.system.icons.regular.TopSpeed
import fluent.ui.system.icons.regular.Trophy
import fluent.ui.system.icons.regular.VehicleCar
import fluent.ui.system.icons.regular.Warning
import fluent.ui.system.icons.regular.WeatherCloudy
import fluent.ui.system.icons.regular.WeatherMoon
import fluent.ui.system.icons.regular.WeatherRainShowersDay
import fluent.ui.system.icons.regular.WeatherSunny
import fluent.ui.system.icons.regular.WiFi1
import fluent.ui.system.icons.regular.WiFiOff
import fluent.ui.system.icons.regular.Window
import fluent.ui.system.icons.regular.WindowAd
import fluent.ui.system.icons.regular.WindowConsole

// Adjust this import if your IDE resolves FluentIcons to a different package —
// this fork doesn't publish one canonical import path in its docs, so let
// Android Studio's autocomplete confirm the exact package for the version pulled.


// ─────────────────────────────────────────────────────────
// Fluent System Icons — single source of truth for every icon
// drawn anywhere in the Bluebird shell (Start Menu, Desktop,
// TaskBar, Settings, …). Re-skinning the whole shell, or swapping
// in a different icon pack, only ever means editing this file.
//
// Dependency (module build.gradle.kts):
//     implementation("io.github.niyajali:fluentui-system-icons:1.0.1")
// Root build.gradle: mavenCentral() must be declared.
//
// Icons are ImageVectors — FluentIcons.Regular.X (outline, used throughout
// this shell to match Windows 11) or FluentIcons.Filled.X (solid, for
// selected/active states where called out). Browse the full catalog at
// https://niyajali.github.io/fluentui-system-icons if a name below doesn't
// match your pulled version — Android Studio's autocomplete on
// FluentIcons.Regular.… will also show the closest real name.
// ─────────────────────────────────────────────────────────
object FluentIcon {
    // Apps / navigation
    val Settings        = FluentIcons.Regular.Settings
    val Calculator      = FluentIcons.Regular.Calculator
    val Calendar        = FluentIcons.Regular.CalendarLtr
    val Moon            = FluentIcons.Regular.WeatherMoon
    val Folder          = FluentIcons.Regular.Folder
    val FolderOpen      = FluentIcons.Regular.FolderOpen
    val FolderZip       = FluentIcons.Regular.FolderZip
    val FolderProhibited = FluentIcons.Regular.FolderProhibited
    val Globe           = FluentIcons.Regular.Globe
    val TaskList        = FluentIcons.Regular.TaskListSquareLtr
    val PlayCircle      = FluentIcons.Regular.PlayCircle
    val Play            = FluentIcons.Regular.Play
    val Console         = FluentIcons.Regular.WindowConsole
    val Desktop         = FluentIcons.Regular.Desktop
    val Apps            = FluentIcons.Regular.Apps

    // Documents / files
    val Document        = FluentIcons.Regular.Document
    val DocumentText    = FluentIcons.Regular.DocumentText
    val DocumentPdf     = FluentIcons.Regular.DocumentPdf
    val Table           = FluentIcons.Regular.Table
    val TextFont        = FluentIcons.Regular.TextFont
    val MusicNote2      = FluentIcons.Regular.MusicNote2
    val Android         = FluentIcons.Regular.Phone

    // Images
    val ImageMultiple   = FluentIcons.Regular.ImageMultiple
    val Image           = FluentIcons.Regular.Image
    val Color           = FluentIcons.Regular.Color

    // Layout
    val Grid            = FluentIcons.Regular.Grid
    val List            = FluentIcons.Regular.List
    val FullScreenMax   = FluentIcons.Regular.FullScreenMaximize
    val Resize          = FluentIcons.Regular.Resize
    val FullScreenMin   = FluentIcons.Regular.FullScreenMinimize

    // Actions
    val Add             = FluentIcons.Regular.Add
    val Edit             = FluentIcons.Regular.Edit
    val Delete          = FluentIcons.Regular.Delete
    val Checkmark       = FluentIcons.Regular.Checkmark
    val Dismiss         = FluentIcons.Regular.Dismiss
    val Subtract        = FluentIcons.Regular.Subtract
    val Copy            = FluentIcons.Regular.Copy
    val Cut             = FluentIcons.Regular.Cut
    val ClipboardPaste  = FluentIcons.Regular.ClipboardPaste
    val Rename          = FluentIcons.Regular.Rename
    val Open            = FluentIcons.Regular.Open
    val Share           = FluentIcons.Regular.Share
    val Search          = FluentIcons.Regular.Search
    val ArrowSync       = FluentIcons.Regular.ArrowSync
    val ArrowSort       = FluentIcons.Regular.ArrowSort
    val ArrowReply      = FluentIcons.Regular.ArrowReply
    val Link            = FluentIcons.Regular.Link
    val Info            = FluentIcons.Regular.Info

    // Chevrons
    val ChevronUp       = FluentIcons.Regular.ChevronUp
    val ChevronDown     = FluentIcons.Regular.ChevronDown
    val ChevronRight    = FluentIcons.Regular.ChevronRight

    // System tray / quick actions
    val PhoneAndroid    = FluentIcons.Regular.Phone
    val Wifi            = FluentIcons.Regular.WiFi1
    val WifiOff         = FluentIcons.Regular.WiFiOff
    val Bluetooth       = FluentIcons.Regular.Bluetooth
    val Airplane        = FluentIcons.Regular.Airplane
    val Prohibited      = FluentIcons.Regular.Prohibited
    val BrightnessHigh  = FluentIcons.Regular.BrightnessHigh
    val Speaker2        = FluentIcons.Regular.Speaker2
    val Power           = FluentIcons.Regular.Power
    val SleepArrow      = FluentIcons.Regular.WeatherMoon
    val LockClosed      = FluentIcons.Regular.LockClosed

    // Taskbar / system tray / window chrome
    val Sparkle         = FluentIcons.Regular.Sparkle
    val Battery10       = FluentIcons.Regular.Battery10
    val CheckmarkCircle = FluentIcons.Regular.CheckmarkCircle
    val Widget          = FluentIcons.Regular.WindowAd
    val DesktopMultiple = FluentIcons.Regular.TabDesktopMultiple
    val Stack           = FluentIcons.Regular.Stack
    val MoreHorizontal  = FluentIcons.Regular.MoreHorizontal
    val Print           = FluentIcons.Regular.Print
    val Pin             = FluentIcons.Regular.Pin
    val SwipeRight      = FluentIcons.Regular.SwipeRight
    val Options         = FluentIcons.Regular.Options
    val EyeOff          = FluentIcons.Regular.EyeOff
    val Speaker1        = FluentIcons.Regular.Speaker1
    val SpeakerMute     = FluentIcons.Regular.SpeakerMute
    val Location        = FluentIcons.Regular.Location
    val Mic             = FluentIcons.Regular.Mic
    val Alert           = FluentIcons.Regular.Alert
    val Shield          = FluentIcons.Regular.Shield
    val CloudCheckmark  = FluentIcons.Regular.CloudCheckmark
    val Window          = FluentIcons.Regular.Window

    // Widgets panel / Action Center / notifications
    val Alarm           = FluentIcons.Regular.ClockAlarm
    val AlertOff        = FluentIcons.Regular.AlertOff
    val ArrowDownload   = FluentIcons.Regular.ArrowDownload
    val ArrowUpload     = FluentIcons.Regular.ArrowUpload
    val ArrowTrendingUp = FluentIcons.Regular.ArrowTrending
    val Battery0        = FluentIcons.Regular.Battery0
    val Battery3        = FluentIcons.Regular.Battery3
    val Battery6        = FluentIcons.Regular.Battery6
    val Battery9        = FluentIcons.Regular.Battery9
    val BatteryCharge   = FluentIcons.Regular.BatteryCharge
    val CloudOff        = FluentIcons.Regular.CloudOff
    val DataUsage       = FluentIcons.Regular.DataUsage
    val Flash           = FluentIcons.Regular.Flash
    val LocationOff     = FluentIcons.Regular.LocationOff
    val Pause           = FluentIcons.Regular.Pause
    val SkipBack        = FluentIcons.Regular.Previous
    val SkipForward     = FluentIcons.Regular.Next
    val TopSpeed        = FluentIcons.Regular.TopSpeed
    val Trophy          = FluentIcons.Regular.Trophy
    val VehicleCar      = FluentIcons.Regular.VehicleCar
    val WalkingIcon     = FluentIcons.Regular.PersonWalking
    val Warning         = FluentIcons.Regular.Warning
    val WeatherCloudy   = FluentIcons.Regular.WeatherCloudy
    val WeatherRain     = FluentIcons.Regular.WeatherRainShowersDay
    val WeatherSunny    = FluentIcons.Regular.WeatherSunny
}

/**
 * Resolves the Fluent icon representing a window/app "kind" — used for the
 * taskbar's running-window icons, the window titlebar icon, and the PiP
 * thumbnail. This used to be duplicated (with a drifted MEDIA_PLAYER mapping
 * — Taskbar.kt used a play icon, WindowManager.kt used a music note) in two
 * separate files; consolidated here so every surface agrees.
 */
fun iconForKey(key: String): androidx.compose.ui.graphics.vector.ImageVector = when (key) {
    io.github.norbertweb.bluebird.WindowIconKey.PremiumTextEditorScreen -> FluentIcon.TextFont
    io.github.norbertweb.bluebird.WindowIconKey.SETTINGS         -> FluentIcon.Settings
    io.github.norbertweb.bluebird.WindowIconKey.FILE_EXPLORER    -> FluentIcon.Folder
    io.github.norbertweb.bluebird.WindowIconKey.BROWSER          -> FluentIcon.Globe
    io.github.norbertweb.bluebird.WindowIconKey.CALCULATOR       -> FluentIcon.Calculator
    io.github.norbertweb.bluebird.WindowIconKey.CALENDAR         -> FluentIcon.Calendar
    io.github.norbertweb.bluebird.WindowIconKey.PHOTOS           -> FluentIcon.ImageMultiple
    io.github.norbertweb.bluebird.WindowIconKey.TASK_MANAGER     -> FluentIcon.TaskList
    io.github.norbertweb.bluebird.WindowIconKey.MEDIA_PLAYER     -> FluentIcon.PlayCircle
    io.github.norbertweb.bluebird.WindowIconKey.IMAGE_VIEWER     -> FluentIcon.Image
    io.github.norbertweb.bluebird.WindowIconKey.WORD_IMPRESS     -> FluentIcon.DocumentText
    io.github.norbertweb.bluebird.WindowIconKey.BLUEBIRD_STORE   -> FluentIcon.Moon
    io.github.norbertweb.bluebird.WindowIconKey.RECYCLE_BIN      -> FluentIcon.Delete
    io.github.norbertweb.bluebird.WindowIconKey.WEB_APP_MANAGER  -> FluentIcon.Globe
    io.github.norbertweb.bluebird.WindowIconKey.WEB_APP          -> FluentIcon.Globe
    io.github.norbertweb.bluebird.WindowIconKey.COPY_PROGRESS    -> FluentIcon.Copy
    else                                                          -> FluentIcon.Window
}
