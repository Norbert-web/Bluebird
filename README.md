<div align="center">

# Bluebird

### A full Windows 11 desktop experience — on Android.

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?style=flat-square&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Min%20API-26%20(Android%208.0)-orange?style=flat-square)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v2.0-success?style=flat-square)](https://github.com/norbert-web/bluebird/releases)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/norbert-web/bluebird/actions)

<br/>

> **Bluebird** is an open-source Android home screen replacement that recreates the Windows 11 desktop environment pixel-perfectly — complete with a floating windowed app system, a real file explorer, a built-in app store, live wallpapers, a word processor, a terminal, and now full Fluent UI System Icons.

<br/>

**[Download APK](#download--releases) · [Screenshots](#screenshots) · [Features](#features) · [Architecture](#architecture) · [Getting Started](#getting-started)**

</div>

---

## Table of Contents

- [Overview](#overview)
- [What's New](#whats-new)
- [Release History](#release-history)
- [Screenshots](#screenshots)
- [Features](#features)
- [Built-in Apps](#built-in-apps)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Permissions](#permissions)
- [Getting Started](#getting-started)
- [Building from Source](#building-from-source)
- [Download & Releases](#download--releases)
- [Configuration](#configuration)
- [Known Limitations](#known-limitations)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Credits](#credits)
- [Contact](#contact)

---

## Overview

**Bluebird** transforms your Android device into a Windows 11 desktop. It is a fully functional **home screen launcher** — set it as your default launcher and your phone becomes a PC-style productivity device.

Bluebird isn't trying to convince you it's a real operating system running underneath Android — it doesn't pretend to be a dual-boot or a virtualized desktop. What it *is* is the most complete, carefully scaled desktop **simulation** available for Android: every window, icon, and menu is built from scratch in Jetpack Compose to feel and behave like the real thing, right down to floating windows you can drag and stack, a real file system underneath, and a taskbar that tracks what's actually running.

The detail Bluebird cares about most is one most "Windows launchers" get wrong: **density scaling**. Most desktop-style launchers are designed and screenshotted in landscape or on tablets, then look cramped, oversized, or broken the moment a real user opens them one-handed on a phone in portrait — which is how the overwhelming majority of Android users actually hold their device. Bluebird's icon grid, windows, and text scale to match the user's actual screen size and density, so the desktop looks intentional and correctly proportioned in both portrait and landscape — not too small, not oversized, just right for the screen it's on.

Bluebird is also built to run well on modest hardware. The release APK is around 27MB and stays smooth even on 2GB RAM devices, which matters because the people most drawn to a free, offline, desktop-style Android experience are often on budget phones with limited storage and data — not flagship devices with RAM to spare.

- A **floating windowed app system** where every app opens in a draggable, resizable window
- A **real Windows 11 taskbar** with clock, pinned apps, system tray, and Action Center
- A **Start Menu** with app grid, search, user profile, and power options
- A **real file explorer** powered by Android's `File` API — not simulated
- A **Bluebird App Store** for installing custom HTML/CSS/JS and web apps directly on the desktop
- **Live wallpapers** and **desktop particle animations** (Aurora, Nebula, Snow, Rain, Sakura, and more)
- **Word Impress**, a built-in rich-text word processor with pagination and PDF export
- A full **Terminal** app for on-device shell access
- **Live system notifications** via `NotificationListenerService`, plus native **toast notifications**
- **Real phone dialer** and **SMS messaging** using Android's native contact and telephony APIs
- A **media player** with full playback controls powered by Media3 (ExoPlayer)
- An **image viewer** with pinch-to-zoom, swipe navigation, and wallpaper-set support
- A **Recycle Bin** — deleted files go here, restorable or permanently removed
- A first-launch **OOBE wizard** (Out-of-Box Experience) for permissions, username, and avatar
- **Persistent wallpapers** (home + lock screen) that survive restarts
- **Desktop shortcuts** — pin any file, folder, or app directly to the desktop
- **Fluent UI System Icons** throughout the entire shell, matching Windows 11's real icon language

Built entirely with **Jetpack Compose** and **Kotlin**, targeting Android 8.0+ (API 26+).

---

## What's New

### v2.0 — "The Beginning of Beauty"

Bluebird v2.0 is the biggest visual milestone in the project's history. This release is dedicated entirely to closing the gap between Bluebird and real Windows 11 — starting with iconography.

- **Fluent UI System Icons** have been introduced across the desktop, taskbar, Start Menu, Settings, File Explorer, and every built-in app, replacing the previous Material icon set with Microsoft's own Fluent icon language for a far more authentic Windows 11 look and feel
- Refined icon weights and outlined/filled icon states that match Fluent UI's segoe-style rendering
- Visual polish pass across Acrylic surfaces, spacing, and iconography consistency in preparation for deeper Fluent Design adoption in v2.1
- This is the first release in the "v2.x — Fluent Design" arc; v2.1 is already in active development and will continue bringing Fluent Design System components (typography, motion, and depth) to the rest of the shell

### v1.9 — "Peak Performance"

The fastest and most highly optimized release of Bluebird to date.

- Major rendering and recomposition performance improvements across the Desktop, Taskbar, and Window Manager
- Reduced memory footprint for window switching and live wallpaper rendering
- Faster cold-start time from boot to desktop
- General stability hardening across the App Store, Word Impress, and Terminal introduced in v1.8

---

## Release History

| Version | Date | Codename | Highlights |
|---------|------|----------|------------|
| **v2.0** | Aug 30, 2026 | The Beginning of Beauty | Fluent UI System Icons across the entire OS, Windows 11 visual parity milestone |
| **v1.9** | Aug 28, 2026 | Peak Performance | Fastest, most optimized build to date; major performance and stability pass |
| **v1.8** | Aug 24, 2026 | The Major Expansion Update | Bluebird App Store, custom HTML/CSS/JS app installation, web app installation, live wallpapers, desktop animations, toast notifications, Word Impress, enhanced media controls |
| **v1.7** | May 29, 2026 | Widget & Desktop Stability | Fixed desktop icons, taskbar overlapping, added weather API |
| **v1.6** | May 28, 2026 | — | Notification system, media player improvements |
| **v1.5** | May 27, 2026 | Modern Window Management | Snap layout picker, scrollable desktop |
| **v1.4** | May 26, 2026 | — | Desktop stability and layout fixes |
| **v1.3** | May 24, 2026 | Smart Updates & Professional Start Menu | — |
| **v1.2** | May 24, 2026 | Editor & Start Menu Upgrade | New start menu, syntax highlighting |
| **v1.1** | May 24, 2026 | Wallpaper & Stability | 5 built-in wallpapers |
| **v1.0** | May 23, 2026 | Initial Release | — |

See [CHANGELOG.md](CHANGELOG.md) for the full, detailed changelog.

---

## Screenshots

<p align="center">
  <img src="screenshots/desktop.png" width="32%">
  <img src="screenshots/startmenu.png" width="32%">
  <img src="screenshots/explorer.png" width="32%">
</p>

<p align="center">
  <b>Desktop</b> • <b>Start Menu</b> • <b>File Explorer</b>
</p>

---

## Features

Everything below is grouped by the part of the shell it lives in. Where it's useful, each section notes *how* the feature is actually implemented (real Android APIs vs. simulated behavior), since one of Bluebird's core design goals is to be a real, functioning environment rather than a set of screens that only look like one.

### Desktop Environment

| Feature | Description |
|---------|-------------|
| **Floating Windows** | Every app opens in a draggable, focusable window with minimize/maximize/close |
| **Acrylic Glassmorphism UI** | Frosted-glass panels with blur, transparency, and layered depth |
| **Multi-window management** | Stack and switch between multiple open windows |
| **Snap layout picker** | Windows 11–style snap layout overlay for quick window positioning |
| **Desktop icons** | System icons (This PC, Recycle Bin, Settings, Network), file shortcuts, and app shortcuts, now rendered with Fluent UI System Icons |
| **Right-click context menu** | Long-press desktop → New Folder, New Text File, Personalize, Refresh, Display Settings |
| **Desktop shortcuts** | Drag any file from File Explorer → Create Shortcut; long-press app in Start Menu → Add to Desktop |
| **Scrollable desktop** | Desktop scrolls when icons exceed the screen area |
| **Live wallpapers** | Aurora, Nebula, Waves, and Bokeh animated wallpapers |
| **Desktop particle animations** | Snow, Bubbles, Stars, Rain, Hearts, Confetti, Fireflies, Leaves, Matrix, and Sakura |
| **5 built-in static wallpapers** | Blue Bloom, Sunset Purple, Forest Green, Deep Space, Aurora — gradient themes |
| **Custom wallpaper** | Pick any image from Gallery for home screen and lock screen separately |
| **Wallpaper persistence** | Custom images copied to internal storage — survive app restarts and reboots |
| **Wallpaper slideshow mode** | Automatically rotate through a selected set of wallpapers |
| **Double-tap taskbar toggle** | Double-tap the desktop to hide/show the taskbar (full immersive mode) |
| **Dark / Light / For You / Special themes** | Full theme switching, live and persistent, with an 8-color accent picker |

### Taskbar

| Feature | Description |
|---------|-------------|
| **Windows 11 taskbar** | Centered Start button, pinned apps, running window indicators, system tray |
| **Pin to taskbar** | Long-press any app icon in Start Menu → Pin to taskbar |
| **Unpin from taskbar** | Long-press any pinned taskbar icon → Unpin |
| **Running window badges** | Dot indicator below any app with an open window |
| **Window switcher** | Tap a running window's taskbar icon to focus or restore it |
| **System tray** | Battery level, Wi-Fi indicator, Bluetooth, clock, date — now with Fluent-style tray glyphs |
| **Notification badge** | Badge count on Action Center button when there are unread notifications |
| **Toast notifications** | Transient, Windows-style toast popups for incoming notifications and system events |

### Start Menu

| Feature | Description |
|---------|-------------|
| **App grid** | Pinned apps + all built-in apps in a 6-column icon grid, using Fluent UI System Icons |
| **All Apps view** | Scroll through every installed app on the device |
| **Real-time search** | Type to filter installed apps instantly |
| **User profile** | Real username + avatar (from OOBE setup) shown in bottom bar |
| **App context menu** | Long-press any app → Open / Add to Desktop / Pin to Taskbar |
| **Power menu** | Sleep, Lock, Restart, Shut Down options |

### File Explorer (Real Filesystem)

This is a genuine file manager, not a themed shortcut list. It's backed by `BluebirdFileSystem`, which sits directly on top of Android's `File` API, so every folder, file size, and modified date shown is real — the same data you'd see in any other file manager, just presented the Windows 11 way (breadcrumbs, Quick Access, sortable columns).

| Feature | Description |
|---------|-------------|
| **True filesystem browsing** | Uses `BluebirdFileSystem`, backed by the Android `File` API — real files, real sizes, real dates |
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
| **File type icons** | Distinct, Fluent-styled icons for images, video, audio, PDF, text, archives, APKs |
| **Advanced copy/move engine** | Unified copy engine shared between Desktop and File Explorer, with live progress, speed and time estimates, and cancel support |

### Bluebird App Store & Web Apps (New in v1.8)

Bluebird's installer mimics a real Windows-style software installation instead of just launching a bookmark. A web app's HTML/CSS/JS assets are packaged into a zip with the correct internal file structure, then renamed to Bluebird's own `.bpk` package extension. Bluebird recognizes `.bpk` files as installable programs — opening one triggers a real "install" flow (icon, name, permissions) rather than just opening a URL, so installed web apps behave like first-class desktop programs instead of glorified browser tabs.

| Feature | Description |
|---------|-------------|
| **Bluebird App Store** | Browse and install apps directly inside the launcher |
| **Custom HTML/CSS/JS app installation** | Package your own lightweight web-based apps as a `.bpk` installer and install them as first-class desktop apps |
| **Install from URL** | Install any web app directly from a URL |
| **Web App Manager** | Manage all installed web apps in one place — update, remove, relaunch |
| **Web App Viewer** | Dedicated runtime window for installed web apps, separate from the main browser |
| **Custom icon / favicon support** | Installed web apps display their own icon on the desktop and Start Menu |

### Word Impress — Word Processor (New in v1.8)

| Feature | Description |
|---------|-------------|
| **Rich text editing** | Full rich-text engine (`RichTextEngine`) with block-based document model |
| **Pagination** | Real page-based layout with automatic pagination as you type |
| **PDF export** | Export any document directly to PDF |
| **Native `.wdoc` format** | Documents are saved and loaded through a dedicated `WdocIO` / `WdocModel` pipeline |
| **Block-based content** | Text, headings, lists, and media handled as composable blocks (`BlockViews`) |

### Terminal (New in v1.8+)

A real command surface for the desktop, not a decorative black box — commands run through Bluebird's own shell bridge and behave like a standard floating window, so it can sit alongside File Explorer or Word Impress like any other app.

| Feature | Description |
|---------|-------------|
| **On-device shell access** | Run shell commands directly from the desktop |
| **Dedicated Terminal window** | Opens as a standard floating Bluebird window like any other app |

### Text Editor

A lightweight code/text editor for quick edits without leaving the desktop — think Notepad++ rather than a full IDE, aimed at fast edits to config files, notes, or code snippets on the go.

| Feature | Description |
|---------|-------------|
| **Syntax highlighting** | Powered by a custom `SyntaxEngine` for common languages |
| **Premium editor state** | Multi-tab, persistent editor preferences |
| **Line numbers & find/replace** | Standard code-editing conveniences |

### Image Viewer

| Feature | Description |
|---------|-------------|
| **Full-screen viewer** | Opens any image in immersive full-screen mode |
| **Pinch-to-zoom** | Multi-touch pinch zoom, pan, double-tap to zoom 2.5x |
| **Swipe navigation** | Swipe left/right to move between images in the same folder |
| **Thumbnail strip** | Scrollable thumbnail row at the bottom for quick jump |
| **Rotate** | Rotate image 90 degrees clockwise or counterclockwise |
| **Share** | Share image via system share sheet |
| **Set as wallpaper** | Set current image as home screen wallpaper instantly |
| **Delete to bin** | Delete to Recycle Bin from inside the viewer |
| **Image info panel** | File name, size, date modified, path |

### Media Player

| Feature | Description |
|---------|-------------|
| **Media3 (ExoPlayer) playback** | Plays MP3, WAV, OGG, FLAC, AAC, M4A, MP4, MKV, AVI, MOV, WebM |
| **Background playback service** | Dedicated `PlaybackService` for uninterrupted media playback |
| **Auto-playlist** | Loads all media files from the same folder into a playlist via `MediaLibraryRepository` |
| **Seek bar** | Real-time playback position with tap-to-seek |
| **Track metadata** | Title, artist, album from file tags |
| **Playback controls** | Play/Pause, Previous, Next, Shuffle, Repeat (Off / All / One) |
| **Enhanced media controls** | Expanded system-level media notification controls (New in v1.8) |
| **Playlist panel** | Left sidebar with all tracks, now-playing indicator, equalizer animation |

### Phone

| Feature | Description |
|---------|-------------|
| **Real contacts** | Reads contacts from `ContactsContract` — real names and numbers |
| **Call log** | Reads recent calls from `CallLog` with incoming/outgoing/missed indicators |
| **Numeric keypad** | Full dial pad that triggers `ACTION_CALL` intent |
| **Contact search** | Search by name or number |
| **One-tap call** | Tap any contact or call log entry to dial |

### Messages (SMS)

| Feature | Description |
|---------|-------------|
| **Real SMS threads** | Reads conversation threads from `Telephony.Sms.Conversations` |
| **Message view** | Full conversation with sent/received bubbles |
| **Send SMS** | Compose and send messages via `ACTION_SENDTO` intent |
| **Contact resolution** | Phone numbers resolved to contact names automatically |
| **New message** | Start a new conversation with any number |
| **Conversation search** | Filter threads by name or message content |

### Recycle Bin

| Feature | Description |
|---------|-------------|
| **System Recycle Bin** | All files deleted from File Explorer go here |
| **Badge count** | Desktop Recycle Bin icon shows item count badge |
| **Restore** | Restore items to their original paths |
| **Permanent delete** | Remove items permanently one by one |
| **Empty bin** | Delete all items at once with confirmation |
| **Multi-select** | Checkbox multi-select for bulk operations |
| **Item details** | Original path, deletion date, file size for every item |

### Settings

Over 50 individual settings across the following categories, all exposed through `LauncherUiState`:

| Section | Highlights |
|---------|-----------|
| **Appearance** | App theme (System, For You, Dark, Light, Special), text scale, icon size, taskbar position, start menu layout, transparency, animation speed, corner radius, grid size, app labels, dark mode schedule, wallpaper slideshow |
| **System** | Launch on boot, snap layouts, clipboard history, Do Not Disturb with scheduling, Focus Assist, screen timeout, game mode, battery saver, thermal protection |
| **Sound** | Media, ringtone, notification, and alarm volume; system sounds; haptic feedback; notification sound; volume-key media control |
| **Network** | Wi-Fi and Bluetooth toggles, data saver, hotspot, VPN, custom DNS |
| **Accessibility** | High contrast, larger text, bold text, reduce motion, mono audio, button shapes, color correction, touch-hold delay, magnification, captions |
| **Privacy & Security** | Screen lock, location access, camera access, microphone access, usage diagnostics, unknown sources, biometric unlock |
| **Time & Language** | 24-hour clock, auto set time, timezone, date format, first day of week |
| **Other** | Search bar, search engine, search suggestions, auto backup, auto update, update channel |

### Notifications

| Feature | Description |
|---------|-------------|
| **Live notifications** | `NotificationListenerService` reads real system notifications |
| **Toast notifications** | Windows-style transient toast popups (New in v1.8) |
| **Action Center** | Grouped notifications with app name, title, body, and timestamp |
| **Bluebird announcements** | Remote team announcements delivered via `notify.json` |
| **Dismiss** | Dismiss individual notifications or clear all |
| **Auto-reconnect** | Service callbacks re-registered on every `onResume()` |

### First-Launch Setup (OOBE)

| Step | Description |
|------|-------------|
| **Welcome** | Animated welcome screen with Bluebird branding |
| **Permissions** | Per-permission grant buttons for Storage, Contacts, SMS, Camera, Notifications |
| **Username** | Enter your name — shown in Start Menu, Lock Screen, and Settings |
| **Profile picture** | Pick from Gallery — stored persistently in internal storage |
| **Skip support** | Every step can be skipped and revisited in Settings later |

### Lock Screen

| Feature | Description |
|---------|-------------|
| **Custom wallpaper** | Shows lock screen wallpaper (separate from home wallpaper) |
| **Live clock** | Large clock with date |
| **User avatar** | Profile picture or initial letter shown on lock screen |
| **Tap to unlock** | Single tap unlocks back to the desktop |

### Gestures

Swipe up, swipe down, double tap, pinch, and standard navigation-bar gestures are supported across the desktop shell.

### Undo System

Reversible actions across the shell surface an undo affordance with a clear, human-readable label for the action just performed.

---

## Built-in Apps

Every app below runs as its own floating window inside Bluebird's window manager — none of them are separate installable APKs. That's what makes the multi-window experience possible: File Explorer, Word Impress, Terminal, and Settings can all be open and visible at once, each independently draggable, resizable, minimizable, and closable, exactly like real desktop applications.

| App | Description |
|-----|-------------|
| **File Explorer** | Real filesystem browser |
| **Settings** | Full settings panel across 8+ categories |
| **Phone** | Real dialer with contacts and call log |
| **Messages** | Real SMS reader and sender |
| **Media Player** | Audio/video player with playlist, built on Media3 |
| **Image Viewer** | Full-screen image viewer |
| **Photos** | Photo gallery |
| **Recycle Bin** | Deleted files manager |
| **Browser** | WebView-based browser with ad blocking |
| **Calculator** | Standard calculator |
| **Calendar** | Month calendar view |
| **Task Manager** | Running processes viewer |
| **Text Editor** | In-launcher editor with syntax highlighting |
| **Word Impress** | Rich-text word processor with pagination and PDF export |
| **Bluebird Store** | Built-in app store for custom HTML/CSS/JS and web apps |
| **Terminal** | On-device shell access |
| **Web App Manager** | Manage installed web apps |
| **Web App Viewer** | Runtime window for installed web apps |

---

## Architecture

Bluebird uses a single-ViewModel, unidirectional data flow architecture:

```
+-----------------------------------------------------------------+
|                          MainActivity                            |
|  +----------------------+     +------------------------------+  |
|  |   SetupScreen        |     |        DesktopScreen          |  |
|  |   (OOBE Wizard)      |     |                                |  |
|  +----------------------+     |  +----------+   +-----------+  |  |
|                                |  | Desktop  |   |  Windows  |  |  |
|                                |  | (Icons,  |   |  Manager  |  |  |
|                                |  | Wallpaper|   |           |  |  |
|                                |  | Context  |   |  Floating |  |  |
|                                |  | Menu)    |   |  Windows  |  |  |
|                                |  +----------+   +-----------+  |  |
|                                |  +--------------------------+  |  |
|                                |  |         Taskbar           |  |  |
|                                |  |   Start | Apps | Tray     |  |  |
|                                |  +--------------------------+  |  |
|                                |  +--------------------------+  |  |
|                                |  |    Overlays (animated)     |  |  |
|                                |  |  Start Menu | Search       |  |  |
|                                |  |  Action Center | Power     |  |  |
|                                |  +--------------------------+  |  |
|                                +--------------------------------+  |
+-----------------------------------------------------------------+
                               |
                    +----------v-----------+
                    |   LauncherViewModel   |
                    |                       |
                    |   LauncherUiState     |
                    |   +-----------------+ |
                    |   | wallpaper       | |
                    |   | userProfile     | |
                    |   | installedApps   | |
                    |   | pinnedApps      | |
                    |   | openWindows     | |
                    |   | notifications   | |
                    |   | desktopShortcuts| |
                    |   | recycleBinItems | |
                    |   | webApps         | |
                    |   | overlayStates   | |
                    |   +-----------------+ |
                    +----------+------------+
                               |
               +---------------+---------------+
               |               |               |
     +---------v------+  +-----v------+  +----v--------+
     |  SharedPrefs   |  |  Android   |  |   System    |
     |  (persistence) |  |   APIs     |  |  Services   |
     |                |  |  File, SMS |  |  Wallpaper  |
     |  - wallpaper   |  |  Contacts  |  |  AudioMgr   |
     |  - username    |  |  CallLog   |  |  Notif.     |
     |  - pinned apps |  |  PackageMgr|  |  Listener   |
     |  - shortcuts   |  |  Media3    |  |  BatteryMgr |
     |  - recycle bin |  +------------+  +-------------+
     +----------------+
```

### Data Flow

```
User Gesture -> Composable -> ViewModel.action() -> UiState update -> Recompose
```

All state lives in a single `LauncherUiState` data class, collected as `StateFlow` by Compose. No Room database — all persistence is through `SharedPreferences` and `DataStore`, with Gson serialization for complex objects.

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 2.0.0 |
| **UI Framework** | Jetpack Compose | BOM 2024.08.00 |
| **Architecture** | MVVM + StateFlow | — |
| **Build System** | Gradle (KTS) | 8.5.2 |
| **Navigation** | None (custom window manager) | — |
| **Media** | Media3 (ExoPlayer) | 1.4.1 |
| **Image Loading** | Coil | 2.7.0 |
| **JSON** | Gson | 2.11.0 |
| **Persistence** | SharedPreferences + DataStore | 1.1.1 |
| **Icons** | Fluent UI System Icons + Material Icons Extended | BOM |
| **Splash Screen** | AndroidX Core SplashScreen | 1.0.1 |
| **Min SDK** | Android 8.0 (Oreo) | API 26 |
| **Target SDK** | Android 15 | API 35 |
| **Compile SDK** | 36 | — |
| **Java Version** | 17 | — |

---

## Project Structure

```
Bluebird/
├── app/
│   ├── build.gradle.kts                  # versionCode=10, versionName=2.0
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/io/github/norbertweb/bluebird/
│           ├── BluebirdApplication.kt
│           ├── DesktopModeHelper.kt
│           ├── LauncherViewModel.kt          # Core logic with 70+ settings
│           ├── MainActivity.kt               # Entry point, OOBE routing, immersive mode
│           │
│           ├── browser/                      # WebView-based browser
│           │   ├── data/
│           │   │   └── BrowserRepository.kt
│           │   ├── ui/
│           │   │   ├── BrowserScreen.kt
│           │   │   └── NewTabPage.kt
│           │   ├── components/
│           │   ├── keyboard/
│           │   ├── model/
│           │   ├── panels/
│           │   ├── webview/
│           │   └── utils/
│           │       ├── AdBlocker.kt
│           │       ├── DownloadHelper.kt
│           │       └── UrlUtils.kt
│           │
│           ├── core/
│           │   └── filesystem/
│           │       └── BluebirdFileSystem.kt # Real filesystem support
│           │
│           ├── data/
│           │   ├── BootReceiver.kt           # Auto-launch on boot
│           │   └── NotificationListener.kt   # System notifications
│           │
│           ├── editor/                       # Premium text/code editor
│           │   ├── core/
│           │   │   ├── EditorModels.kt
│           │   │   └── PremiumEditorState.kt
│           │   ├── highlighting/
│           │   │   └── SyntaxEngine.kt
│           │   ├── ui/screens/
│           │   │   └── PremiumTextEditorScreen.kt
│           │   └── utils/
│           │       └── EditorPreferences.kt
│           │
│           ├── media/                        # Media player service
│           │   ├── MediaLibraryRepository.kt
│           │   └── PlaybackService.kt
│           │
│           ├── ui/
│           │   ├── components/
│           │   │   ├── ActionCenter.kt       # Notifications + quick tiles
│           │   │   ├── CommonComponents.kt   # Acrylic UI
│           │   │   ├── Desktop.kt            # Desktop with icons
│           │   │   ├── DesktopPreferences.kt
│           │   │   ├── DesktopWallpaperState.kt
│           │   │   ├── NotificationToast.kt  # Toast notifications (v1.8)
│           │   │   ├── SearchOverlay.kt
│           │   │   ├── StartMenu.kt
│           │   │   ├── Taskbar.kt
│           │   │   ├── TerminalScreen.kt     # Terminal (v1.8+)
│           │   │   ├── WebAppManager.kt      # Web app support (v1.8)
│           │   │   ├── WidgetsPanel.kt
│           │   │   └── WindowManager.kt
│           │   ├── screens/
│           │   │   ├── AppScreens.kt         # Calculator, Calendar, Photos, TaskManager
│           │   │   ├── DesktopScreen.kt
│           │   │   ├── FileExplorerScreen.kt
│           │   │   ├── ImageViewerScreen.kt
│           │   │   ├── MediaPlayerScreen.kt
│           │   │   ├── MessagesScreen.kt
│           │   │   ├── RecycleBinScreen.kt
│           │   │   ├── SettingsScreen.kt
│           │   │   ├── SetupScreen.kt        # OOBE wizard
│           │   │   ├── LockScreenActivity.kt
│           │   │   ├── BrowserScreen.kt
│           │   │   └── Launcherupdatesettings.kt
│           │   └── theme/
│           │       └── Theme.kt              # bluebirdColors, dark/light Material3 themes
│           │
│           ├── wordprocessor/                # Word Impress (v1.8)
│           │   ├── WordImpress.kt
│           │   ├── RichTextEngine.kt
│           │   ├── BlockViews.kt
│           │   ├── Pagination.kt
│           │   ├── PdfExport.kt
│           │   ├── WdocIO.kt
│           │   └── WdocModel.kt
│           │
│           └── update/
│               ├── UpdateManager.kt          # GitHub-based update checker & downloader
│               ├── UpdateNotificationHelper.kt
│               └── Updatemodels.kt
│
├── gradle/
│   ├── libs.versions.toml                    # Version catalog (all dependencies)
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                          # Project-level build config
├── settings.gradle.kts                       # Module settings
├── gradle.properties                         # Gradle JVM args
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
├── SECURITY.md
├── README.md
└── screenshots/
```

---

## Permissions

Bluebird requests the following permissions, each explained:

| Permission | Why it's needed | Required? |
|-----------|-----------------|-----------|
| `QUERY_ALL_PACKAGES` | List all installed apps in Start Menu | Yes |
| `RECEIVE_BOOT_COMPLETED` | Auto-start as home screen after reboot | Yes |
| `SET_WALLPAPER` / `SET_WALLPAPER_HINTS` | Apply custom wallpaper to system | Yes |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | Browse media files in File Explorer and Media Player | Yes |
| `READ_EXTERNAL_STORAGE` | Filesystem access (Android 12 and below) | Yes |
| `MANAGE_EXTERNAL_STORAGE` | Full filesystem access for File Explorer | Optional |
| `READ_CONTACTS` | Show real contacts in Phone app | Optional |
| `CALL_PHONE` | Dial calls from the Phone dialer | Optional |
| `READ_CALL_LOG` | Show recent calls in Phone app | Optional |
| `READ_SMS` / `SEND_SMS` / `RECEIVE_SMS` | Read and send messages | Optional |
| `CAMERA` | Take profile picture during OOBE | Optional |
| `POST_NOTIFICATIONS` | Show system and toast notifications | Optional |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read live notifications via system service | Optional |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Browser, App Store, web apps, network status, update checker | Optional |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Bluetooth status in Quick Settings | Optional |
| `WRITE_SETTINGS` | Adjust screen brightness | Optional |
| `BATTERY_STATS` | Real battery level in status bar | Optional |

> `MANAGE_EXTERNAL_STORAGE` requires manual grant in Android Settings on API 30+. The OOBE wizard guides the user through this.

> **Notification Listener** must be manually granted in **Settings → Notifications → Notification Access**. The OOBE wizard opens this screen directly.

---

## Getting Started

### Prerequisites

- Android Studio **Hedgehog (2023.1.1)** or newer
- JDK **17** or newer
- Android device or emulator running **Android 8.0+ (API 26+)**
- Gradle **8.5.2**

### Clone the Repository

```bash
git clone https://github.com/norbert-web/bluebird.git
cd bluebird
```

### Open in Android Studio

1. Open Android Studio
2. Click **File → Open** and select the `Bluebird/` folder
3. Wait for Gradle sync to complete
4. Connect your device or start an emulator

### Set as Default Launcher

After installing, Android will prompt you to select a default launcher. Choose **Bluebird** and tap **Always**. If not prompted:

1. Go to **Settings → Apps → Default Apps → Home App**
2. Select **Bluebird**

---

## Building from Source

### Debug Build (for development)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

1. Create a keystore (first time only):

```bash
keytool -genkey -v -keystore io.github.norbertweb.bluebird-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias io.github.norbertweb.bluebird
```

2. Configure signing in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../io.github.norbertweb.bluebird-release.jks")
            storePassword = System.getenv("KEYSTORE_PASS")
            keyAlias = "io.github.norbertweb.bluebird"
            keyPassword = System.getenv("KEY_PASS")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

3. Build:

```bash
KEYSTORE_PASS=yourpassword KEY_PASS=yourkeypass ./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Install Directly to Device

```bash
# Debug
./gradlew installDebug

# Or use ADB manually
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests

```bash
./gradlew test          # Unit tests
./gradlew connectedTest # Instrumented tests (requires device/emulator)
```

---

## Download & Releases

### Latest Release: v2.0

**[Download from GitHub Releases →](https://github.com/norbert-web/bluebird/releases/latest)**

### Installing the APK

1. Download the latest `bluebird-release.apk` from [GitHub Releases](https://github.com/norbert-web/bluebird/releases/latest)
2. On your Android device, go to **Settings → Security → Install unknown apps**
3. Enable **Allow from this source** for your browser or file manager
4. Open the downloaded APK and tap **Install**
5. When prompted to choose a launcher, select **Bluebird → Always**

### Sideload via ADB

```bash
adb install io.github.norbertweb.bluebird-release.apk
```

---

## Configuration

Bluebird stores all user preferences in `SharedPreferences` under the key `launcher_prefs_v2`. There is no config file — everything is configured through the in-app **Settings** and **OOBE wizard**.

### Resetting to defaults

To fully reset Bluebird:

```bash
adb shell pm clear io.github.norbertweb.bluebird
```

Or: **Android Settings → Apps → Bluebird → Storage → Clear Data**

### Changing the package name

If you want to publish your own fork, change the package name in:

1. `app/build.gradle.kts` → `namespace` and `applicationId`
2. `app/src/main/AndroidManifest.xml` → `android:authorities` in the `FileProvider`
3. Rename the Java package directory from `io/github/norbertweb/bluebird/` to your new package

---

## Known Limitations

Most of the limitations below aren't Bluebird-specific bugs — they're restrictions Android itself places on any third-party app, launcher or not. They're listed here in full because a transparent limitations list is more trustworthy than pretending a home-screen replacement can override the OS it's running on.

| Limitation | Reason | Workaround |
|-----------|--------|-----------|
| **Wi-Fi cannot be toggled programmatically** | Removed in Android 10 (API 29) by Google | Tapping Wi-Fi opens system Wi-Fi Settings |
| **Bluetooth toggle** | Deprecated for third-party apps in Android 13 | Tapping opens system Bluetooth Settings |
| **MANAGE_EXTERNAL_STORAGE** | Requires manual grant on Android 11+ | OOBE wizard provides a direct link |
| **Notification Listener** | Requires manual grant in system settings | OOBE wizard opens the settings screen |
| **File deletions are permanent** | Recycle Bin tracks metadata but `File.delete()` can't be reliably undone on all devices | Items appear in Recycle Bin with restore option |
| **Screen brightness control** | Requires `WRITE_SETTINGS` which must be granted manually | Settings → Display → System permissions |
| **Wallpaper on lock screen** | Android system lock screen is separate from the in-app lock screen | Use the in-app lock (Start → Power → Lock) for Bluebird's lock screen |
| **Web apps run in a sandboxed WebView** | Bluebird web apps are not native apps and are limited to WebView-supported APIs | Use native Android apps for functionality outside WebView's scope |

---

## Roadmap

### v2.1 — Next Release (In Development)

- [ ] Continued Fluent Design System rollout: typography, motion curves, and layered depth
- [ ] Fluent-style Mica and Acrylic material refinements
- [ ] Resizable floating windows (drag handles on edges/corners)
- [ ] Taskbar auto-hide mode (hover to reveal)
- [ ] Multiple virtual desktops

### Upcoming

- [ ] Remote Schools built-in learning app (Uganda curriculum P1–S6)
- [ ] Window transparency controls
- [ ] Widget support (third-party app widgets on desktop)
- [ ] Clipboard manager

### Future

- [ ] Screen recording
- [ ] Keyboard shortcuts (for physical keyboards)
- [ ] Full color-wheel accent picker
- [ ] Cloud sync for settings and shortcuts
- [ ] Full custom theming engine beyond dark/light/Fluent presets

---

## Contributing

Contributions are warmly welcome! Full guidelines — including dev environment setup, coding standards, and the PR process — live in [CONTRIBUTING.md](CONTRIBUTING.md). Quick summary below:

### Bug Reports

Use the [GitHub Issues](https://github.com/norbert-web/bluebird/issues) tracker. Please include:

- Android version and device model
- Steps to reproduce
- Expected vs actual behavior
- Logcat output (filter by `io.github.norbertweb.bluebird`)

### Pull Requests

1. **Fork** the repository
2. **Create a branch**: `git checkout -b feature/your-feature-name`
3. **Make your changes** following the existing code style
4. **Test** on a real device (emulators may not support all launcher features)
5. **Commit** with a clear message: `git commit -m "feat: add window resize handles"`
6. **Push** and open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Jetpack Compose best practices (hoisted state, stable parameters)
- Keep Composables small and focused
- Add `@Preview` annotations where useful
- State mutations must go through `LauncherViewModel` — never mutate state directly in a composable
- New icons should use Fluent UI System Icons to stay consistent with the v2.x visual direction

### Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, released code |
| `develop` | Integration branch for next release |
| `feature/*` | New features |
| `fix/*` | Bug fixes |
| `release/*` | Release preparation |

---

## License

```
MIT License

Copyright (c) 2025 Bluebird Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Credits

### Libraries Used

| Library | License | Purpose |
|---------|---------|---------|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | Apache 2.0 | UI framework |
| [Media3 (ExoPlayer)](https://developer.android.com/media/media3) | Apache 2.0 | Audio/video playback |
| [Coil](https://coil-kt.github.io/coil/) | Apache 2.0 | Image loading |
| [Gson](https://github.com/google/gson) | Apache 2.0 | JSON serialization |
| [Fluent UI System Icons](https://github.com/microsoft/fluentui-system-icons) | MIT | Windows 11–style icon set |
| [Material Icons Extended](https://fonts.google.com/icons) | Apache 2.0 | Supplementary icon set |
| [AndroidX DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) | Apache 2.0 | Preferences persistence |
| [AndroidX SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen) | Apache 2.0 | Splash screen API |

### Design Inspiration

- [Windows 11](https://www.microsoft.com/en-us/windows/windows-11) by Microsoft — for the UI design language, Fluent Design System, and Acrylic material

### Contributors

<a href="https://github.com/norbert-web/bluebird/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=norbert-web/bluebird" />
</a>

---

## Contact

- **GitHub Issues**: [github.com/norbert-web/bluebird/issues](https://github.com/norbert-web/bluebird/issues)
- **Discussions**: [github.com/norbert-web/bluebird/discussions](https://github.com/norbert-web/bluebird/discussions)
- **Email**: trebronwayne@gmail.com

---

<div align="center">

Made with Kotlin · [Back to top](#bluebird)

</div>
