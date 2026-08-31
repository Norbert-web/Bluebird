package io.github.norbertweb.bluebird.browser.utils

import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.Locale
import io.github.norbertweb.bluebird.browser.model.NEWTAB_URL
import io.github.norbertweb.bluebird.browser.model.SearchEngine

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
        if (trimmed.isBlank()) return NEWTAB_URL
        if (trimmed == NEWTAB_URL) return NEWTAB_URL
        if (trimmed.equals("about:blank", ignoreCase = true)) return "about:blank"

        // Preserve explicit schemes. WebView understands more than HTTP(S),
        // so do not accidentally turn mailto:, tel:, file:, intent:, etc. into
        // search queries. Only schemes followed by `//` or a well-known scheme
        // delimiter are accepted here to avoid treating normal prose such as
        // "hello: world" as a URL.
        val scheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").find(trimmed)?.value
        if (scheme != null) {
            val normalized = scheme.dropLast(1).lowercase(Locale.ROOT) + ":" + trimmed.drop(scheme.length)
            return normalized
        }

        return if (looksLikeUrl(trimmed)) {
            "https://$trimmed"
        } else {
            "${searchEngine.queryUrl}${Uri.encode(trimmed)}"
        }
    }

    /**
     * Desktop-omnibox style URL detection. Prefer URL navigation for hostnames,
     * localhost and IP literals; otherwise treat the input as a search query.
     */
    fun looksLikeUrl(raw: String): Boolean {
        val value = raw.trim()
        if (value.isBlank() || value.contains(Regex("\\s"))) return false
        if (value.startsWith("?") || value.startsWith("/")) return false
        if (value.equals("localhost", ignoreCase = true)) return true
        if (value.matches(Regex("^[0-9]{1,3}(\\.[0-9]{1,3}){3}(:[0-9]{1,5})?$"))) return true
        if (value.contains(":")) return value.substringBefore(":").contains(".")
        return value.contains(".") && !value.endsWith(".")
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
