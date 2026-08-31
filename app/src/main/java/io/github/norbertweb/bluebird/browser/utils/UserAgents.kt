package io.github.norbertweb.bluebird.browser.utils

// ═══════════════════════════════════════════════════════════════════════
// User-agent strings
// Chrome 124 baseline — modern enough to pass UA sniffing on major sites
// ═══════════════════════════════════════════════════════════════════════

object UserAgents {

    const val MOBILE =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    const val DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun get(desktopMode: Boolean) = if (desktopMode) DESKTOP else MOBILE
}
