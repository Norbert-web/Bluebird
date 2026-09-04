package io.github.norbertweb.bluebird.ui.components

import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Add
import fluent.ui.system.icons.regular.Airplane
import fluent.ui.system.icons.regular.Alert
import fluent.ui.system.icons.regular.AlertOff
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.ArrowAutofitWidth
import fluent.ui.system.icons.regular.ArrowDownload
import fluent.ui.system.icons.regular.ArrowLeft
import fluent.ui.system.icons.regular.ArrowMove
import fluent.ui.system.icons.regular.ArrowReply
import fluent.ui.system.icons.regular.ArrowRight
import fluent.ui.system.icons.regular.ArrowRotateClockwise
import fluent.ui.system.icons.regular.ArrowSort
import fluent.ui.system.icons.regular.ArrowSync
import fluent.ui.system.icons.regular.ArrowTrending
import fluent.ui.system.icons.regular.ArrowUpload
import fluent.ui.system.icons.regular.Accessibility
import fluent.ui.system.icons.regular.Battery0
import fluent.ui.system.icons.regular.Battery10
import fluent.ui.system.icons.regular.Battery3
import fluent.ui.system.icons.regular.Battery6
import fluent.ui.system.icons.regular.Battery9
import fluent.ui.system.icons.regular.BatteryCharge
import fluent.ui.system.icons.regular.Bluetooth
import fluent.ui.system.icons.regular.Archive
import fluent.ui.system.icons.regular.BrightnessHigh
import fluent.ui.system.icons.regular.Briefcase
import fluent.ui.system.icons.regular.Bug
import fluent.ui.system.icons.regular.Beaker
import fluent.ui.system.icons.regular.BuildingShop
import fluent.ui.system.icons.regular.Calculator
import fluent.ui.system.icons.regular.CalendarLtr
import fluent.ui.system.icons.regular.Calendar3Day
import fluent.ui.system.icons.regular.CalendarClock
import fluent.ui.system.icons.regular.CalendarDay
import fluent.ui.system.icons.regular.CalendarMonth
import fluent.ui.system.icons.regular.CalendarWeekNumbers
import fluent.ui.system.icons.regular.Chat
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ChevronLeft
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.ChevronUp
import fluent.ui.system.icons.regular.ClipboardPaste
import fluent.ui.system.icons.regular.ClockAlarm
import fluent.ui.system.icons.regular.CloudCheckmark
import fluent.ui.system.icons.regular.CloudOff
import fluent.ui.system.icons.regular.CloudArrowDown
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
import fluent.ui.system.icons.regular.LeafOne
import fluent.ui.system.icons.regular.Keyboard
import fluent.ui.system.icons.regular.LockClosedKey
import fluent.ui.system.icons.regular.Cast
import fluent.ui.system.icons.regular.CellularData4
import fluent.ui.system.icons.regular.PhoneLaptop
import fluent.ui.system.icons.regular.Crop
import fluent.ui.system.icons.regular.Games
import fluent.ui.system.icons.regular.Mail
import fluent.ui.system.icons.regular.PaintBrush
import fluent.ui.system.icons.regular.Person
import fluent.ui.system.icons.regular.ShieldCheckmark
import fluent.ui.system.icons.regular.TapSingle
import fluent.ui.system.icons.regular.Wallpaper
import fluent.ui.system.icons.regular.WeatherMoonOff
import fluent.ui.system.icons.regular.Wrench
import fluent.ui.system.icons.regular.WeatherMoon as WeatherMoonNightLight
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.FolderProhibited
import fluent.ui.system.icons.regular.FolderZip
import fluent.ui.system.icons.regular.FullScreenMaximize
import fluent.ui.system.icons.regular.FullScreenMinimize
import fluent.ui.system.icons.regular.Globe
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.GridDots
import fluent.ui.system.icons.regular.Heart
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.ImageMultiple
import fluent.ui.system.icons.regular.ImageOff
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Link
import fluent.ui.system.icons.regular.List
import fluent.ui.system.icons.regular.Location
import fluent.ui.system.icons.regular.LocationOff
import fluent.ui.system.icons.regular.LockClosed
import fluent.ui.system.icons.regular.Mic
import fluent.ui.system.icons.regular.MoreHorizontal
import fluent.ui.system.icons.regular.MoreVertical
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
import fluent.ui.system.icons.regular.RotateLeft
import fluent.ui.system.icons.regular.Search
import fluent.ui.system.icons.regular.Send
import fluent.ui.system.icons.regular.Server
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Share
import fluent.ui.system.icons.regular.Shield
import fluent.ui.system.icons.regular.SlideText
import fluent.ui.system.icons.regular.Sparkle
import fluent.ui.system.icons.regular.Speaker1
import fluent.ui.system.icons.regular.Speaker2
import fluent.ui.system.icons.regular.SpeakerMute
import fluent.ui.system.icons.regular.Stack
import fluent.ui.system.icons.regular.Star
import fluent.ui.system.icons.regular.Subtract
import fluent.ui.system.icons.regular.Square
import fluent.ui.system.icons.regular.SquareMultiple
import fluent.ui.system.icons.regular.SwipeRight
import fluent.ui.system.icons.regular.TabDesktopMultiple
import fluent.ui.system.icons.regular.Table
import fluent.ui.system.icons.regular.TaskListSquareLtr
import fluent.ui.system.icons.regular.TextFont
import fluent.ui.system.icons.regular.TextSortAscending
import fluent.ui.system.icons.regular.TopSpeed
import fluent.ui.system.icons.regular.Trophy
import fluent.ui.system.icons.regular.VehicleCar
import fluent.ui.system.icons.regular.Video
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
    // Editor aliases used by the IDE shell. Keep these mapped to real icons from 1.0.1.
    val Code = FluentIcons.Regular.WindowConsole
    val Analytics = FluentIcons.Regular.DataUsage
    val ArrowDownward = FluentIcons.Regular.ChevronDown
    val ArrowUpward = FluentIcons.Regular.ChevronUp
    val Bookmark = FluentIcons.Regular.Pin
    val BookmarkBorder = FluentIcons.Regular.Pin
    val CheckBox = FluentIcons.Regular.CheckmarkCircle
    val CheckBoxOutlineBlank = FluentIcons.Regular.CheckmarkCircle
    val Comment = FluentIcons.Regular.MoreHorizontal
    val ContentCopy = FluentIcons.Regular.Copy
    val ContentCut = FluentIcons.Regular.Cut
    val ContentPaste = FluentIcons.Regular.ClipboardPaste
    val DeleteForever = FluentIcons.Regular.Delete
    val FilterList = FluentIcons.Regular.List
    val FindReplace = FluentIcons.Regular.Search
    val FormatIndentIncrease = FluentIcons.Regular.ArrowAutofitWidth
    val FormatListBulleted = FluentIcons.Regular.List
    val FormatListNumbered = FluentIcons.Regular.TaskListSquareLtr
    val History = FluentIcons.Regular.ArrowRotateClockwise
    val KeyboardArrowDown = FluentIcons.Regular.ChevronDown
    val KeyboardArrowUp = FluentIcons.Regular.ChevronUp
    val Lock = FluentIcons.Regular.LockClosed
    val LockOpen = FluentIcons.Regular.LockClosedKey
    val NavigateBefore = FluentIcons.Regular.Previous
    val NavigateNext = FluentIcons.Regular.Next
    val Notes = FluentIcons.Regular.DocumentText
    val PushPin = FluentIcons.Regular.Pin
    val Redo = FluentIcons.Regular.ArrowRotateClockwise
    val Restore = FluentIcons.Regular.ArrowRotateClockwise
    val Schedule = FluentIcons.Regular.ClockAlarm
    val SelectAll = FluentIcons.Regular.CheckmarkCircle
    val Sort = FluentIcons.Regular.ArrowSort
    val SpaceBar = FluentIcons.Regular.Keyboard
    val Subject = FluentIcons.Regular.TextFont
    val TextDecrease = FluentIcons.Regular.TextFont
    val TextFields = FluentIcons.Regular.TextFont
    val TextIncrease = FluentIcons.Regular.TextFont
    val Undo = FluentIcons.Regular.ArrowReply
    val WrapText = FluentIcons.Regular.TextFont
    val ZoomIn = FluentIcons.Regular.FullScreenMaximize
    val ZoomOut = FluentIcons.Regular.FullScreenMinimize
    val ZoomOutMap = FluentIcons.Regular.FullScreenMinimize
    val Save = FluentIcons.Regular.Checkmark
    val SaveAs = FluentIcons.Regular.Open
    val Close = FluentIcons.Regular.Dismiss
    val Extension = FluentIcons.Regular.Apps
    val VerticalAlignCenter = FluentIcons.Regular.Resize
    val Error = FluentIcons.Regular.Alert
    val CheckCircle = FluentIcons.Regular.CheckmarkCircle
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

    // ── Windows 11 Quick Settings extras ──────────────────────────────────
    // These back the additional Action Center tiles (touch keyboard, night
    // light, mobile hotspot, cast, rotation lock, accessibility, energy
    // saver, snip). As noted above, confirm exact names against whichever
    // fluentui-system-icons version is actually resolved — Cast and
    // LockClosedKey resolved fine in this build; Nfc did not (see below).
    val Keyboard        = FluentIcons.Regular.Keyboard
    val NightLight      = FluentIcons.Regular.WeatherMoonNightLight
    val Hotspot         = FluentIcons.Regular.PhoneLaptop
    val Cast            = FluentIcons.Regular.Cast
    // NFC has no confirmed glyph in this pack's resolved version (build
    // failed on Regular.Nfc — "Unresolved reference"). Falling back to the
    // Phone icon, which is already proven to resolve elsewhere in this file.
    // Swap for a real contactless/NFC glyph once you confirm its exact name
    // against the actual artifact version Gradle pulled — check the catalog
    // at https://niyajali.github.io/fluentui-system-icons or Android
    // Studio's autocomplete on FluentIcons.Regular.
    val Nfc             = FluentIcons.Regular.Phone
    val RotationLock    = FluentIcons.Regular.LockClosedKey
    val Accessibility   = FluentIcons.Regular.Accessibility
    val EnergySaver     = FluentIcons.Regular.LeafOne
    val Snip            = FluentIcons.Regular.Crop
    val AutoRotate      = FluentIcons.Regular.ArrowRotateClockwise
    val AutofitWidth    = FluentIcons.Regular.ArrowAutofitWidth

    // ── Lock screen / desktop power & personalization ─────────────────────
    val BedtimeOff      = FluentIcons.Regular.WeatherMoonOff
    val RestartAlt      = FluentIcons.Regular.ArrowSync
    val WallpaperIcon   = FluentIcons.Regular.Wallpaper
    val Autorenew       = FluentIcons.Regular.ArrowSync
    val NetworkCell     = FluentIcons.Regular.CellularData4
    val Person          = FluentIcons.Regular.Person

    // ── Update settings / diagnostics ──────────────────────────────────────
    val BugReport       = FluentIcons.Regular.Bug
    val CloudDownload   = FluentIcons.Regular.CloudArrowDown
    val OpenInBrowser   = FluentIcons.Regular.Open
    val Science         = FluentIcons.Regular.Beaker
    val SystemUpdateAlt = FluentIcons.Regular.ArrowDownload
    val TouchApp        = FluentIcons.Regular.TapSingle
    val VerifiedUser    = FluentIcons.Regular.ShieldCheckmark

    // ── Messages / app categories ──────────────────────────────────────────
    val ArrowBack       = FluentIcons.Regular.ArrowLeft
    val Brush           = FluentIcons.Regular.PaintBrush
    val Build           = FluentIcons.Regular.Wrench
    val SportsEsports   = FluentIcons.Regular.Games
    val Star            = FluentIcons.Regular.Star
    val Storefront      = FluentIcons.Regular.BuildingShop
    val UploadFile      = FluentIcons.Regular.ArrowUpload
    val Work            = FluentIcons.Regular.Briefcase
    val Email           = FluentIcons.Regular.Mail
    val Message         = FluentIcons.Regular.Chat

    // ── Update-check frequency options — each needs a visually distinct
    // glyph so the selected cadence (launch/day/week/2-week/7-week/month/
    // manual) reads at a glance, mirroring the old 7-way Material icon set.
    val CalendarDay         = FluentIcons.Regular.CalendarDay
    val CalendarWeek        = FluentIcons.Regular.Calendar3Day
    val CalendarBiweekly    = FluentIcons.Regular.CalendarWeekNumbers
    val CalendarSevenWeeks  = FluentIcons.Regular.CalendarMonth
    val CalendarClockIcon   = FluentIcons.Regular.CalendarClock

    // ── Media gallery / file-type / image-viewer icons ─────────────────────
    // Used by ImageViewerScreen.kt, RecycleBinScreen.kt, and AppScreens.kt.
    val Archive         = FluentIcons.Regular.Archive
    val Article         = FluentIcons.Regular.DocumentText
    val AudioFile       = FluentIcons.Regular.MusicNote2
    val BrokenImage     = FluentIcons.Regular.ImageOff
    val Description     = FluentIcons.Regular.DocumentText
    val Favorite        = FluentIcons.Regular.Heart
    val FavoriteBorder  = FluentIcons.Regular.Heart
    val MoreVert        = FluentIcons.Regular.MoreVertical
    val OpenWith        = FluentIcons.Regular.ArrowMove
    val Refresh         = FluentIcons.Regular.ArrowSync
    val RotateLeft      = FluentIcons.Regular.RotateLeft
    val RotateRight     = FluentIcons.Regular.ArrowRotateClockwise
    val Slideshow       = FluentIcons.Regular.SlideText
    val SortByAlpha     = FluentIcons.Regular.TextSortAscending
    val Storage         = FluentIcons.Regular.Server
    val Video           = FluentIcons.Regular.Video

    // Chevrons / directional (ImageViewerScreen prev/next controls)
    val ChevronLeft     = FluentIcons.Regular.ChevronLeft
    val ArrowForward    = FluentIcons.Regular.ArrowRight

    // Grid density toggle (small/medium/large thumbnail size) — three
    // visually distinct glyphs so the state change is legible, mirroring
    // the old GridOn/GridView/ViewModule three-way Material toggle.
    val GridSmall       = FluentIcons.Regular.GridDots
    val GridMedium      = FluentIcons.Regular.Grid
    val GridLarge       = FluentIcons.Regular.SquareMultiple

    // ── Window title-bar controls (minimize / maximize / close) ────────────
    val Minimize        = FluentIcons.Regular.Subtract
    val CropSquare      = FluentIcons.Regular.Square

    // ── Web App Manager ─────────────────────────────────────────────────────
    val Language        = FluentIcons.Regular.Globe
    val OpenInNew       = FluentIcons.Regular.Open
    val Send            = FluentIcons.Regular.Send
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
