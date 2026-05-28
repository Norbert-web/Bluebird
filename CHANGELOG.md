# Changelog

All notable changes to **Bluebird** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Resizable floating windows with drag handles
- Remote Schools learning app (Uganda curriculum P1–S6)
- Taskbar auto-hide mode
- Multiple virtual desktops

---

## [1.5.0] - 2026-05-27

### Added
- Advanced window management system with smoother interactions
- Snap layout picker (Windows 11–style)
- Scrollable desktop when the screen becomes full

### Changed
- Replaced macOS-style traffic light window controls with a more native experience
- Modernized overall desktop interaction and appearance
- Default wallpaper mode now automatically appears on first app launch

### Fixed
- Desktop mode handling for large numbers of desktop items

### Better
- More stable desktop environment
- Smoother multitasking and dragging behavior
- Improved first-launch appearance

---

## [1.4.0] - 2026-05-27

### Fixed
- App update system HTTPS 404 errors
- Invisible desktop "dead zones" where icons could not be dragged
- Icon movement limitations across certain screen areas
- Icon overlapping when adding many icons to the desktop

### Improved
- Desktop stability and responsiveness
- Drag-and-drop experience and icon placement handling
- Update checking reliability and handling of GitHub-hosted update files
- Overall UI consistency

---

## [1.3.0] - 2026-05-27

### Added
- Complete in-app update system — Bluebird can now check for and download updates directly from GitHub
- Automatic update availability detection
- Post notifications for update availability, download completion, and update status feedback
- Expanded settings and app management options

### Improved
- Start menu polished for a more formal and professional appearance
- Settings layout and interaction feedback
- Background task handling and update checking process
- Overall app stability

---

## [1.2.0] - 2026-05-27

### Added
- Brand-new Start Menu with a cleaner, more modern design
- Syntax / code highlighting support in the text editor
- Enhanced text editing capabilities

### Improved
- Overall app appearance and visual consistency
- Navigation experience
- Editor responsiveness and rendering performance
- Smoother transitions and interactions

### Fixed
- Minor UI inconsistencies and stability issues

---

## [1.1.0] - 2026-05-27

### Added
- Multiple wallpaper modes (Default, 5 built-in wallpapers, Automatic cycling)

### Improved
- Stability and responsiveness across Android devices
- Overall user experience and visual consistency

### Fixed
- App icons going off-screen on some devices
- Minor UI rendering issues

---

## [1.0.0] - 2025-05-10

### 🎉 Initial Release

#### Added

**Core Desktop**
- Windows 11–style desktop with glassmorphism / acrylic UI
- Floating windowed app system — every built-in app opens in a draggable window
- Multi-window management — stack and focus multiple windows simultaneously
- Desktop icon grid: This PC, Recycle Bin, Settings, Network
- Desktop shortcut system — pin files, folders, and apps to desktop
- Right-click (long-press) desktop context menu: New Folder, New Text File, Personalize, Refresh, Display Settings
- 5 built-in gradient wallpapers: Blue Bloom, Sunset Purple, Forest Green, Deep Space, Aurora
- Custom wallpaper picker for home screen and lock screen independently
- Wallpaper persistence — custom images survive app restarts and device reboots
- Double-tap desktop to hide/show taskbar (immersive mode)
- Full dark and light theme support with live switching

**Taskbar**
- Windows 11–style centered taskbar with Start button, pinned apps, running window indicators, system tray
- Pin apps to taskbar via long-press in Start Menu
- Unpin from taskbar via long-press on taskbar icon
- Running window dot indicator
- Window focus/restore by tapping taskbar icon
- System tray: battery level, Wi-Fi status, Bluetooth, clock, date

**Start Menu**
- 6-column pinned app grid with all built-in apps visible
- All Apps view — scroll through every installed app
- Real-time app search
- Real username and avatar in bottom bar (sourced from OOBE)
- Long-press any app → Open / Add to Desktop / Pin to Taskbar
- Power menu: Sleep, Lock, Restart, Shut Down

**First-Launch Setup (OOBE)**
- 4-step animated wizard: Welcome → Permissions → Username → Profile Picture
- Per-permission grant flow: Storage, Contacts, SMS, Camera, Notification Access
- Username entry — shown in Start Menu, Lock Screen, Settings
- Profile picture picker from Gallery — copied to internal storage
- Skip support for every step

**Lock Screen**
- Separate lock screen wallpaper (independent from desktop wallpaper)
- Live clock (12-hour format) and date
- User avatar and name display
- Tap-to-unlock

**File Explorer (Real Filesystem)**
- Full `File` API integration — real files, sizes, dates
- Clickable breadcrumb navigation bar
- List view and grid view with toggle
- Sort by Name, Date, Size, Type — ascending/descending
- Hidden files toggle (dotfiles)
- File search within current directory
- File operations: Copy, Cut, Paste, Rename, Delete (to Recycle Bin)
- Long-press file context menu: Open / Copy / Cut / Rename / Create Desktop Shortcut / Delete
- New Folder creation
- Open files with correct system intent (images, video, audio, PDF, APK, etc.)
- Quick Access panel: Desktop, Downloads, Documents, Pictures, Music, Movies, DCIM, Storage
- Real storage usage progress bar in left panel
- Color-coded file type icons: images (green), video (purple), audio (orange), PDF (red), text (blue), folders (amber), archives (grey), APK (green)
- Create desktop shortcut from any file or folder

**Recycle Bin**
- All File Explorer deletions routed to Recycle Bin
- Desktop icon with badge count
- Restore items to original path
- Permanently delete individual items
- Empty entire bin with confirmation
- Multi-select with checkboxes for bulk operations
- Metadata display: original path, deletion date, file size

**Image Viewer**
- Full-screen immersive viewer
- Pinch-to-zoom, pan, double-tap 2.5× zoom
- Swipe left/right to navigate all images in folder
- Scrollable thumbnail strip at bottom
- Rotate 90° CW / CCW
- Share via system share sheet
- Set as home screen wallpaper
- Delete to Recycle Bin from inside viewer
- Image info panel (name, size, date, path)

**Media Player**
- Audio playback: MP3, WAV, OGG, FLAC, AAC, M4A via Android `MediaPlayer`
- Video support: MP4, MKV, AVI, MOV, WebM
- Auto-playlist from same folder
- Seek bar with real-time position
- Track metadata from file tags via `MediaMetadataRetriever`
- Play/Pause, Previous, Next, Shuffle, Repeat (Off / All / One)
- Playlist panel with now-playing indicator
- Track duration and current position display

**Phone App**
- Real contacts from `ContactsContract`
- Call log (last 100 entries) with type indicators (incoming/outgoing/missed)
- Full numeric keypad → `ACTION_CALL` intent
- Contact search by name or number
- One-tap call from any contact or call log entry

**Messages App**
- Real SMS threads from `Telephony.Sms.Conversations`
- Full conversation view with sent/received bubbles
- Compose and send SMS via `ACTION_SENDTO`
- Contact name resolution from phone number
- New conversation composer
- Thread search by name or content

**Notifications**
- Live system notifications via `NotificationListenerService`
- Action Center panel with grouped notifications
- App name, title, body, timestamp per notification
- Dismiss individual or clear all
- Badge count on Action Center button
- Callbacks re-registered on every `onResume()` for reliability

**Settings (12 categories)**
- Personalization: wallpaper picker, accent color (8 presets), dark/light mode
- Display: brightness slider → `Settings.System.SCREEN_BRIGHTNESS`
- Sound: volume slider → `AudioManager.STREAM_MUSIC`
- Network: Wi-Fi → opens system Wi-Fi Settings; Bluetooth → opens system Bluetooth Settings
- Battery: real level + charging status with progress bar
- Storage: real used/free with labeled progress bar
- Apps: installed apps list with tap-to-App-Info
- Accounts: user profile display
- About: device model, manufacturer, Android version, API level, RAM, app version

**Architecture**
- Single `LauncherUiState` data class — all UI state in one place
- `StateFlow` + Compose `collectAsStateWithLifecycle()`
- Persistent state in `SharedPreferences` with Gson serialization
- Wallpaper files copied to `filesDir` for restart persistence
- Pinned apps stored by package name list
- Boot receiver for auto-launch after device restart

---

## [0.9.0-beta] - 2025-04-15

### Added
- Initial beta with basic desktop, taskbar, and Start Menu
- File Explorer (simulated — replaced in 1.0.0 with real filesystem)
- Basic settings stubs

### Changed
- Replaced simulated file system with real Android `File` API
- Rewrote Start Menu with proper app pinning

### Fixed
- Animation spec errors from old Compose API (positional → named parameters)
- DrawablePainter import resolution
- Wallpaper content URI not surviving restarts

---

[Unreleased]: https://github.com/norbert-web/bluebird/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.5.0
[1.4.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.4.0
[1.3.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.3.0
[1.2.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.2.0
[1.1.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.1.0
[1.0.0]: https://github.com/norbert-web/bluebird/releases/tag/v1.0.0
[0.9.0-beta]: https://github.com/norbert-web/bluebird/releases/tag/v0.9.0-beta
