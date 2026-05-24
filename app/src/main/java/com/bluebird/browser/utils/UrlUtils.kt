package com.win11launcher.browser.utils

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.win11launcher.browser.model.NEWTAB_URL
import com.win11launcher.browser.model.SearchEngine

// ═══════════════════════════════════════════════════════════════════════
// Main-thread dispatcher
// ═══════════════════════════════════════════════════════════════════════

private val mainHandler = Handler(Looper.getMainLooper())

fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block()
    else mainHandler.post(block)
}

// ═══════════════════════════════════════════════════════════════════════
// URL utilities
// ═══════════════════════════════════════════════════════════════════════

object UrlUtils {

    /**
     * Resolves raw address-bar text into a navigable URL.
     */
    fun resolveUrl(raw: String, searchEngine: SearchEngine): String {
        val trimmed = raw.trim()
        return when {
            trimmed.isBlank()                                             -> NEWTAB_URL
            trimmed == NEWTAB_URL                                         -> NEWTAB_URL
            trimmed == "about:blank"                                      -> "about:blank"
            trimmed.startsWith("http://")
                    || trimmed.startsWith("https://")
                    || trimmed.startsWith("file://")                      -> trimmed
            // Looks like a domain  (contains dot, no spaces)
            trimmed.contains(".") && !trimmed.contains(" ")
                    && !trimmed.startsWith("?")                           -> "https://$trimmed"
            else -> "${searchEngine.queryUrl}${Uri.encode(trimmed)}"
        }
    }

    /**
     * Returns a short display form of a URL for the address bar.
     */
    fun displayUrl(url: String): String {
        if (url == NEWTAB_URL || url.isBlank()) return ""
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }

    /**
     * Returns the host of a URL for security badge display.
     */
    fun host(url: String): String? {
        return try { Uri.parse(url).host } catch (_: Exception) { null }
    }

    /**
     * True if the URL is a real web URL (not newtab / blank).
     */
    fun isWebUrl(url: String) =
        url.startsWith("http://") || url.startsWith("https://")

    /**
     * Returns true if the URL is secure (HTTPS).
     */
    fun isSecure(url: String) = url.startsWith("https://")

    /**
     * Strips protocol and trailing slash for a clean favicon base URL.
     */
    fun faviconUrl(pageUrl: String): String? {
        return try {
            val uri = Uri.parse(pageUrl)
            "${uri.scheme}://${uri.host}/favicon.ico"
        } catch (_: Exception) { null }
    }

    /**
     * Guess a display color from a URL's host (deterministic, no collision guarantee).
     */
    fun colorForUrl(url: String): Long {
        val colors = longArrayOf(
            0xFF1A73E8, 0xFFE53935, 0xFF43A047, 0xFFFB8C00,
            0xFF8E24AA, 0xFF00ACC1, 0xFFD81B60, 0xFF6D4C41,
            0xFF039BE5, 0xFF7CB342
        )
        val host = host(url) ?: url
        return colors[Math.abs(host.hashCode()) % colors.size]
    }
}
