package com.bluebird.ui.components

import com.bluebird.R

// ─────────────────────────────────────────────────────────────────
// Wallpaper mode
// APPARENT  – animated gradient colours (5 presets, cycles automatically)
// DEFAULT   – real bitmap wallpapers from res/drawable (wp1..wp5), cycles automatically
// CUSTOM    – user-chosen image from gallery / file picker (URI stored in prefs)
// ─────────────────────────────────────────────────────────────────
enum class DesktopWallpaperMode { APPARENT, DEFAULT, CUSTOM }

// ─────────────────────────────────────────────────────────────────
// Default wallpaper drawable resource IDs.
// It will be put in use after all the five wallpapers are got,I think I need local wallpapers
//   val DEFAULT_WALLPAPERS = listOf(
//       R.drawable.desktop_wp_1,
//       R.drawable.desktop_wp_2,
//       R.drawable.desktop_wp_3,
//       R.drawable.desktop_wp_4,
//       R.drawable.desktop_wp_5
//   )


val DEFAULT_WALLPAPERS: List<Int> = listOf(
    R.drawable.desktop_wp_1,
       R.drawable.desktop_wp_2,
       R.drawable.desktop_wp_3,
       R.drawable.desktop_wp_4,
       R.drawable.desktop_wp_5
)

