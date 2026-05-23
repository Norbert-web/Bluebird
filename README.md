<div align="center">

# 🐦 Bluebird

### A full Windows 11 desktop experience — on Android.

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?style=flat-square&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Min%20API-26%20(Android%208.0)-orange?style=flat-square)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-success?style=flat-square)](https://github.com/norbert-web/bluebird/releases)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/norbert-web/bluebird/actions)

<br/>

> **Bluebird** is an open-source Android home screen replacement that recreates the Windows 11 desktop environment pixel-perfectly — complete with a floating windowed app system, real file explorer, live notifications, media player, phone dialer, SMS messaging, wallpaper engine, and a full first-launch setup experience.

<br/>

**[📦 Download APK](#-download--releases) · [📸 Screenshots](#-screenshots) · [✨ Features](#-features) · [🏗️ Architecture](#-architecture) · [🚀 Getting Started](#-getting-started) · [🤝 Contributing](#-contributing)**

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Screenshots](#-screenshots)
- [Features](#-features)
- [Built-in Apps](#-built-in-apps)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Permissions](#-permissions)
- [Getting Started](#-getting-started)
- [Building from Source](#-building-from-source)
- [Download & Releases](#-download--releases)
- [Configuration](#-configuration)
- [Known Limitations](#-known-limitations)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)
- [Credits](#-credits)

---

## 🔭 Overview

**Bluebird** transforms your Android device into a Windows 11 desktop. It is a fully functional **home screen launcher** — set it as your default launcher and your phone becomes a PC-style productivity environment, complete with:

- A **floating windowed app system** where every app opens in a draggable, resizable window
- A **real Windows 11 taskbar** with clock, pinned apps, system tray, and Action Center
- A **Start Menu** with app grid, search, user profile, and power options
- A **real file explorer** powered by Android's `File` API — not simulated
- **Live system notifications** via `NotificationListenerService`
- **Real phone dialer** and **SMS messaging** using Android's native contact & telephony APIs
- A **media player** with full playback controls powered by `MediaPlayer`
- An **image viewer** with pinch-to-zoom, swipe navigation, and wallpaper-set support
- A **Recycle Bin** — deleted files go here, restorable or permanently removed
- A first-launch **OOBE wizard** (Out-of-Box Experience) for permissions, username, and avatar
- **Persistent wallpapers** (home + lock screen) that survive restarts
- **Desktop shortcuts** — pin any file, folder, or app directly to the desktop

Built entirely with **Jetpack Compose** and **Kotlin**, targeting Android 8.0+ (API 26+).

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots/desktop.png" width="32%">
  <img src="screenshots/startmenu.png" width="32%">
  <img src="screenshots/explorer.png" width="32%">
</p>

<p align="center">
  <b>Desktop</b> • <b>Start Menu</b> • <b>File Explorer</b>
</p>

---

## ✨ Features

### 🖥️ Desktop Environment

| Feature | Description |
|---------|-------------|
| **Floating Windows** | Every app opens in a draggable, focusable window with minimize/maximize/close |
| **Acrylic Glassmorphism UI** | Frosted-glass panels with blur, transparency, and layered depth |
| **Multi-window management** | Stack and switch between multiple open windows |
| **Desktop icons** | System icons (This PC, Recycle Bin, Settings, Network), file shortcuts, and app shortcuts |
| **Right-click context menu** | Long-press desktop → New Folder, New Text File, Personalize, Refresh, Display Settings |
| **Desktop shortcuts** | Drag any file from File Explorer → Create Shortcut; long-press app in Start Menu → Add to Desktop |
| **5 built-in wallpapers** | Blue Bloom, Sunset Purple, Forest Green, Deep Space, Aurora — gradient themes |
| **Custom wallpaper** | Pick any image from Gallery for home screen and lock screen separately |
| **Wallpaper persistence** | Custom images copied to internal storage — survive app restarts and reboots |
| **Double-tap taskbar toggle** | Double-tap the desktop to hide/show the taskbar (full immersive mode) |
| **Dark / Light theme** | Full dark and light mode switching, live and persistent |

### 📌 Taskbar

| Feature | Description |
|---------|-------------|
| **Windows 11 taskbar** | Centered Start button, pinned apps, running window indicators, system tray |
| **Pin to taskbar** | Long-press any app icon in Start Menu → Pin to taskbar |
| **Unpin from taskbar** | Long-press any pinned taskbar icon → Unpin |
| **Running window badges** | Dot indicator below any app with an open window |
| **Window switcher** | Tap a running window's taskbar icon to focus or restore it |
| **System tray** | Battery level, Wi-Fi indicator, Bluetooth, clock, date |
| **Notification badge** | Badge count on Action Center button when there are unread notifications |

### 🪟 Start Menu

| Feature | Description |
|---------|-------------|
| **App grid** | Pinned apps + all built-in apps in a 6-column icon grid |
| **All Apps view** | Scroll through every installed app on the device |
| **Real-time search** | Type to filter installed apps instantly |
| **User profile** | Real username + avatar (from OOBE setup) shown in bottom bar |
| **App context menu** | Long-press any app → Open / Add to Desktop / Pin to Taskbar |
| **Power menu** | Sleep, Lock, Restart, Shut Down options |

### 🗂️ File Explorer (Real Filesystem)

| Feature | Description |
|---------|-------------|
| **True filesystem browsing** | Uses Android `File` API — real files, real sizes, real dates |
| **Breadcrumb navigation** | Clickable path segments — tap any segment to jump there |
| **List & Grid views** | Toggle between detail list and thumbnail grid |
| **Sort options** | Sort by Name, Date, Size, or Type — ascending or descending |
| **Hidden files toggle** | Show/hide dotfiles |
| **File search** | Filter files in the current directory by name |
| **File operations** | Copy, Cut, Paste, Rename, Delete (to Recycle Bin) |
| **File context menu** | Long-press → Open / Copy / Cut / Rename / Create Shortcut / Delete |
| **Create desktop shortcut** | Right-click any file/folder → Create Desktop Shortcut |
| **New folder** | Create folders directly in current directory |
| **Open with intent** | Tapping a file opens it with the correct system app |
| **Quick Access panel** | Desktop, Downloads, Documents, Pictures, Music, Movies, DCIM, Storage |
| **Storage progress bar** | Shows used/free internal storage in left panel |
| **File type icons** | Distinct color-coded icons for images, video, audio, PDF, text, archives, APKs |

### 🖼️ Image Viewer

| Feature | Description |
|---------|-------------|
| **Full-screen viewer** | Opens any image in immersive full-screen mode |
| **Pinch-to-zoom** | Multi-touch pinch zoom, pan, double-tap to zoom 2.5× |
| **Swipe navigation** | Swipe left/right to move between images in the same folder |
| **Thumbnail strip** | Scrollable thumbnail row at the bottom for quick jump |
| **Rotate** | Rotate image 90° CW or CCW |
| **Share** | Share image via system share sheet |
| **Set as wallpaper** | Set current image as home screen wallpaper instantly |
| **Delete to bin** | Delete to Recycle Bin from inside the viewer |
| **Image info panel** | File name, size, date modified, path |

### 🎵 Media Player

| Feature | Description |
|---------|-------------|
| **Audio playback** | Plays MP3, WAV, OGG, FLAC, AAC, M4A via Android `MediaPlayer` |
| **Video support** | MP4, MKV, AVI, MOV, WebM |
| **Auto-playlist** | Loads all media files from the same folder into a playlist |
| **Seek bar** | Real-time playback position with tap-to-seek |
| **Track metadata** | Title, artist, album from file tags via `MediaMetadataRetriever` |
| **Playback controls** | Play/Pause, Previous, Next, Shuffle, Repeat (Off / All / One) |
| **Playlist panel** | Left sidebar with all tracks, now-playing indicator, equalizer animation |
| **Duration display** | Track duration and current position always visible |

### 📱 Phone

| Feature | Description |
|---------|-------------|
| **Real contacts** | Reads contacts from `ContactsContract` — real names and numbers |
| **Call log** | Reads last 100 calls from `CallLog` with incoming/outgoing/missed indicators |
| **Numeric keypad** | Full dial pad that triggers `ACTION_CALL` intent |
| **Contact search** | Search by name or number |
| **One-tap call** | Tap any contact or call log entry to dial |

### 💬 Messages (SMS)

| Feature | Description |
|---------|-------------|
| **Real SMS threads** | Reads conversation threads from `Telephony.Sms.Conversations` |
| **Message view** | Full conversation with sent/received bubbles |
| **Send SMS** | Compose and send messages via `ACTION_SENDTO` intent |
| **Contact resolution** | Phone numbers resolved to contact names automatically |
| **New message** | Start a new conversation with any number |
| **Conversation search** | Filter threads by name or message content |

### 🗑️ Recycle Bin

| Feature | Description |
|---------|-------------|
| **System Recycle Bin** | All files deleted from File Explorer go here |
| **Badge count** | Desktop Recycle Bin icon shows item count badge |
| **Restore** | Restore items to their original paths |
| **Permanent delete** | Remove items permanently one by one |
| **Empty bin** | Delete all items at once with confirmation |
| **Multi-select** | Checkbox multi-select for bulk operations |
| **Item details** | Original path, deletion date, file size for every item |

### ⚙️ Settings

| Section | Features |
|---------|---------|
| **Personalization** | Wallpaper picker (home + lock screen), gradient presets, accent color (8 colors), dark/light mode |
| **Display** | Brightness slider → real `Settings.System.SCREEN_BRIGHTNESS` |
| **Sound** | Volume slider → real `AudioManager.STREAM_MUSIC` |
| **Network** | Wi-Fi toggle → opens system Wi-Fi settings; Bluetooth → opens system Bluetooth settings |
| **Battery** | Real battery level + charging status with progress bar |
| **Storage** | Real used/free storage with progress bar |
| **Apps** | List of all installed apps; tap to open App Info |
| **Accounts** | User profile with avatar and name from OOBE |
| **About** | Device model, manufacturer, Android version, API level, RAM, app version |

### 🔔 Notifications

| Feature | Description |
|---------|-------------|
| **Live notifications** | `NotificationListenerService` reads real system notifications |
| **Action Center** | Grouped notifications with app name, title, body, and timestamp |
| **Dismiss** | Dismiss individual notifications or clear all |
| **Auto-reconnect** | Service callbacks re-registered on every `onResume()` |

### 🔐 First-Launch Setup (OOBE)

| Step | Description |
|------|-------------|
| **Welcome** | Animated welcome screen with Bluebird branding |
| **Permissions** | Per-permission grant buttons for Storage, Contacts, SMS, Camera, Notifications |
| **Username** | Enter your name — shown in Start Menu, Lock Screen, and Settings |
| **Profile picture** | Pick from Gallery — stored persistently in internal storage |
| **Skip support** | Every step can be skipped and revisited in Settings later |

### 🔒 Lock Screen

| Feature | Description |
|---------|-------------|
| **Custom wallpaper** | Shows lock screen wallpaper (separate from home wallpaper) |
| **Live clock** | Large 12-hour clock with date |
| **User avatar** | Profile picture or initial letter shown on lock screen |
| **Tap to unlock** | Single tap unlocks back to the desktop |

---

## 📱 Built-in Apps

| App | Icon | Description |
|-----|------|-------------|
| **File Explorer** | 📁 | Real filesystem browser |
| **Settings** | ⚙️ | Full settings panel |
| **Phone** | 📞 | Real dialer with contacts & call log |
| **Messages** | 💬 | Real SMS reader and sender |
| **Media Player** | 🎵 | Audio/video player with playlist |
| **Image Viewer** | 🖼️ | Full-screen image viewer |
| **Recycle Bin** | 🗑️ | Deleted files manager |
| **Browser** | 🌐 | WebView-based browser |
| **Calculator** | 🔢 | Standard calculator |
| **Calendar** | 📅 | Month calendar view |
| **Photos** | 🖼️ | Photo gallery |
| **Task Manager** | 📊 | Running processes viewer |

---

## 🏗️ Architecture

Bluebird uses a single-ViewModel, unidirectional data flow architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                         │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │   SetupScreen       │    │      DesktopScreen          │ │
│  │   (OOBE Wizard)     │    │                             │ │
│  └─────────────────────┘    │  ┌──────────┐ ┌─────────┐  │ │
│                             │  │ Desktop  │ │ Windows │  │ │
│                             │  │ (Icons,  │ │ Manager │  │ │
│                             │  │ Wallpaper│ │         │  │ │
│                             │  │ Context  │ │ Floating│  │ │
│                             │  │ Menu)    │ │ Windows │  │ │
│                             │  └──────────┘ └─────────┘  │ │
│                             │  ┌──────────────────────┐   │ │
│                             │  │      Taskbar          │   │ │
│                             │  │  Start │ Apps │ Tray  │   │ │
│                             │  └──────────────────────┘   │ │
│                             │  ┌──────────────────────┐   │ │
│                             │  │  Overlays (animated)  │   │ │
│                             │  │  Start Menu │ Search  │   │ │
│                             │  │  Action Center│ Power  │   │ │
│                             │  └──────────────────────┘   │ │
│                             └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                   ┌──────────▼──────────┐
                   │  LauncherViewModel   │
                   │                      │
                   │  LauncherUiState     │
                   │  ┌─────────────────┐ │
                   │  │ wallpaper       │ │
                   │  │ userProfile     │ │
                   │  │ installedApps   │ │
                   │  │ pinnedApps      │ │
                   │  │ openWindows     │ │
                   │  │ notifications   │ │
                   │  │ desktopShortcuts│ │
                   │  │ recycleBinItems │ │
                   │  │ overlayStates   │ │
                   │  └─────────────────┘ │
                   └──────────┬──────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼──────┐  ┌─────▼──────┐  ┌────▼────────┐
    │  SharedPrefs   │  │ Android    │  │ System      │
    │  (persistence) │  │ APIs       │  │ Services    │
    │                │  │ File, SMS  │  │ Wallpaper   │
    │  - wallpaper   │  │ Contacts   │  │ AudioManager│
    │  - username    │  │ CallLog    │  │ Notification│
    │  - pinned apps │  │ PackageMgr │  │ Listener    │
    │  - shortcuts   │  │ MediaPlayer│  │ BatteryMgr  │
    │  - recycle bin │  └────────────┘  └─────────────┘
    └────────────────┘
```

### Data Flow

```
User Gesture → Composable → ViewModel.action() → UiState update → Recompose
```

All state lives in a single `LauncherUiState` data class, collected as `StateFlow` by Compose. No Room database — all persistence is through `SharedPreferences` with Gson serialization for complex objects.

---

## 🛠️ Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 2.0.0 |
| **UI Framework** | Jetpack Compose | BOM 2024.08.00 |
| **Architecture** | MVVM + StateFlow | — |
| **Build System** | Gradle (KTS) | 8.5.2 |
| **Navigation** | None (custom window manager) | — |
| **Image Loading** | Coil | 2.7.0 |
| **JSON** | Gson | 2.11.0 |
| **Persistence** | SharedPreferences + DataStore | 1.1.1 |
| **Icons** | Material Icons Extended | BOM |
| **System UI** | Accompanist SystemUI | 0.34.0 |
| **Permissions** | Accompanist Permissions | 0.34.0 |
| **Drawable Rendering** | Accompanist DrawablePainter | 0.34.0 |
| **Splash Screen** | AndroidX Core SplashScreen | 1.0.1 |
| **Min SDK** | Android 8.0 (Oreo) | API 26 |
| **Target SDK** | Android 15 | API 35 |
| **Compile SDK** | 35 | — |
| **Java Version** | 17 | — |

---

## 📁 Project Structure

```
bluebird/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/bluebird/
│   │       │   │
│   │       │   ├── MainActivity.kt              # Entry point, OOBE routing, immersive mode
│   │       │   ├── LauncherViewModel.kt         # Single ViewModel, all state & business logic
│   │       │   │
│   │       │   ├── data/
│   │       │   │   ├── NotificationListener.kt  # NotificationListenerService
│   │       │   │   └── BootReceiver.kt          # Auto-launch on device boot
│   │       │   │
│   │       │   └── ui/
│   │       │       ├── theme/
│   │       │       │   └── Theme.kt             # Win11Colors, dark/light Material3 themes
│   │       │       │
│   │       │       ├── components/
│   │       │       │   ├── ActionCenter.kt      # Notification panel + quick tiles
│   │       │       │   ├── CommonComponents.kt  # AcrylicSurface, AppIconSmall, shared UI
│   │       │       │   ├── Desktop.kt           # Wallpaper, icons, context menu
│   │       │       │   ├── SearchOverlay.kt     # Global search overlay
│   │       │       │   ├── StartMenu.kt         # Start menu, app grid, power menu
│   │       │       │   ├── Taskbar.kt           # Bluebird taskbar, tray, clock
│   │       │       │   ├── WidgetsPanel.kt      # Widgets slide-in panel
│   │       │       │   └── WindowManager.kt     # Floating window container & routing
│   │       │       │
│   │       │       └── screens/
│   │       │           ├── AppScreens.kt        # Calculator, Calendar, Photos, TaskManager
│   │       │           ├── BrowserScreen.kt     # WebView browser
│   │       │           ├── DesktopScreen.kt     # Root desktop compositor
│   │       │           ├── FileExplorerScreen.kt # Real file manager
│   │       │           ├── ImageViewerScreen.kt # Full-screen image viewer
│   │       │           ├── LockScreenActivity.kt # System lock screen activity
│   │       │           ├── MediaPlayerScreen.kt # Audio/video player
│   │       │           ├── MessagesScreen.kt    # SMS reader/sender
│   │       │           ├── PhoneScreen.kt       # Dialer + contacts + call log
│   │       │           ├── RecycleBinScreen.kt  # Recycle bin manager
│   │       │           ├── SettingsScreen.kt    # Full settings (12 categories)
│   │       │           └── SetupScreen.kt       # First-launch OOBE wizard
│   │       │
│   │       └── res/
│   │           ├── drawable/                    # Icons, launcher foreground/background
│   │           ├── mipmap-anydpi-v26/           # Adaptive icon definitions
│   │           ├── values/                      # strings.xml, colors.xml, themes.xml
│   │           └── xml/
│   │               └── file_pa
