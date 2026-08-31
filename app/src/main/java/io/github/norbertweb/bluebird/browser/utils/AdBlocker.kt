package io.github.norbertweb.bluebird.browser.utils

// ═══════════════════════════════════════════════════════════════════════
// AdBlocker — real URL-pattern request filter
// Implements shouldInterceptRequest in WebViewClient.
// Uses a curated domain blocklist (EasyList-inspired patterns).
// ═══════════════════════════════════════════════════════════════════════

object AdBlocker {

    // ── Blocked ad/tracker domains ────────────────────────────────────
    // A representative subset of EasyList + EasyPrivacy domains.
    // In production, load this from a bundled assets file and update periodically.

    private val BLOCKED_DOMAINS = setOf(
        // Ad networks
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", "adsafeprotected.com", "moatads.com", "advertising.com",
        "2mdn.net", "ad.gt", "adcolony.com", "adform.net", "adhigh.net",
        "adition.com", "adjuggler.net", "adlightning.com", "admanager.com",
        "admob.com", "adnxs.com", "adready.com", "adroll.com",
        "ads-twitter.com", "adscale.de", "adsense.com", "adskeeper.co.uk",
        "adsonar.com", "adtechus.com", "adtech.de", "adtile.me",
        "adtrafficq.com", "adtrue.com", "adultadworld.com", "adv.li",
        "advancedads.net", "advertising.com", "affilimatch.de",
        "amazon-adsystem.com", "adblade.com", "adhese.com",
        "appnexus.com", "atdmt.com", "bidswitch.net", "btrll.com",
        "buysellads.com", "carbonads.com", "casalemedia.com",
        "contextweb.com", "convertmedia.com", "criteo.com",
        "crwdcntrl.net", "daphnee.io", "deloton.com",
        "digilant.com", "districtm.io", "domdex.com",
        "ebayadvertising.com", "emxdgt.com", "eyeota.net",
        "ezboard.com", "flashtalking.com", "fwmrm.net",
        "gemini.yahoo.com", "gumgum.com", "httpool.com",
        "improve-digital.com", "indexexchange.com", "iponweb.net",
        "jetpackdigital.com", "lijit.com", "liveintent.com",
        "liverain.com", "loopme.me", "lotame.com",
        "media.net", "mediaplex.com", "mediavoice.com",
        "mgid.com", "moatpixel.com", "monetate.net",
        "mopub.com", "mybestmatch.com", "nativo.com",
        "netpilot.com", "netrics.ch", "openx.net", "openx.com",
        "outbrain.com", "owl.li", "permutive.com",
        "playbuzz.com", "plista.com", "powells.com",
        "pubmatic.com", "pulsepoint.com", "quantcast.com",
        "revcontent.com", "rfihub.com", "rfihub.net",
        "richaudience.com", "rtbhouse.com", "rubiconproject.com",
        "s3.amazon.com", "safeframe.googlesyndication.com",
        "sas.com", "scorecardresearch.com", "sizmek.com",
        "smartadserver.com", "smartclip.net", "socdm.com",
        "sonobi.com", "sovrn.com", "spotxchange.com",
        "springserve.com", "stickyadstv.com", "stickyads.tv",
        "sumome.com", "supersonicads.com", "svc.mt.gov",
        "swiftype.com", "synacor.com", "taboola.com",
        "tapad.com", "telaria.com", "theadex.com",
        "33across.com", "tradedoubler.com", "tradetracker.com",
        "trafficjunky.net", "tribalfusion.com", "turn.com",
        "tvpixel.com", "undertone.com", "unrulymedia.com",
        "valueclick.com", "verizonmedia.com", "vibrantmedia.com",
        "video.unrulymedia.com", "w55c.net", "xaxis.com",
        "yahoo-dmps.com", "yieldmo.com", "yieldoptimizer.com",
        "yimg.com", "zanox.com", "zemanta.com",
        // Trackers
        "analytics.google.com", "google-analytics.com", "googletagmanager.com",
        "googletagservices.com", "hotjar.com", "hubspot.com",
        "kissmetrics.com", "marketo.com", "mixpanel.com",
        "mouseflow.com", "newrelic.com", "omniture.com",
        "optimizely.com", "pardot.com", "pingdom.com",
        "segment.com", "segment.io", "sumologic.com",
        "tealiumiq.com", "tynt.com", "woopra.com",
        "matomo.org", "connect.facebook.net", "facebook.com/tr",
        "bat.bing.com", "linkedin.com/px", "twitter.com/i/adsct",
        "snap.licdn.com", "analytics.tiktok.com",
        "addthis.com", "addtoany.com", "sharethis.com",
        // Fingerprinting / device tracking
        "fingerprintjs.com", "fpjscdn.net", "iovation.com",
        "threatmetrix.com", "kochava.com", "appsflyer.com",
        "adjust.com", "branch.io", "singular.net"
    )

    // URL path segments that are always ads regardless of domain
    private val BLOCKED_PATH_PATTERNS = listOf(
        "/ads/", "/ad/", "/adserver/", "/adserve/", "/advert/",
        "/adsystem/", "/banner/", "/banners/", "/popup/",
        "/sponsored/", "/tracking/", "/tracker/", "/pixel/",
        "/beacon/", "/impression/", "/analytics/collect",
        "/collect?v=", "/tr?id=", "/fbevents.js"
    )

    /**
     * Returns true if the given URL should be blocked.
     * Called from WebViewClient.shouldInterceptRequest().
     */
    fun shouldBlock(url: String, adBlockEnabled: Boolean, trackingEnabled: Boolean): Boolean {
        if (!adBlockEnabled && !trackingEnabled) return false

        val lower = url.lowercase()

        // Check domain blocklist
        val domain = extractDomain(lower)
        if (domain != null) {
            // Exact match
            if (BLOCKED_DOMAINS.contains(domain)) return true
            // Subdomain match  (e.g. "ads.example.com" → check "example.com")
            val parts = domain.split(".")
            for (i in 1 until parts.size - 1) {
                val parent = parts.drop(i).joinToString(".")
                if (BLOCKED_DOMAINS.contains(parent)) return true
            }
        }

        // Check path patterns
        if (adBlockEnabled) {
            BLOCKED_PATH_PATTERNS.forEach { pat ->
                if (lower.contains(pat)) return true
            }
        }

        return false
    }

    private fun extractDomain(url: String): String? {
        return try {
            val start = url.indexOf("://").let { if (it < 0) return null else it + 3 }
            val end   = url.indexOf("/", start).let { if (it < 0) url.length else it }
            val hostPort = url.substring(start, end)
            hostPort.substringBefore(":")  // strip port
        } catch (e: Exception) { null }
    }

    /** Returns an empty WebResourceResponse (blocked) */
    fun emptyResponse(): android.webkit.WebResourceResponse =
        android.webkit.WebResourceResponse(
            "text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0))
        )
}
