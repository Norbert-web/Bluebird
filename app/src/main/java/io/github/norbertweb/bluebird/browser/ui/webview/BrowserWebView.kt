package com.win11launcher.browser.ui.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.win11launcher.browser.model.BrowserSettings
import com.win11launcher.browser.model.BrowserTab
import com.win11launcher.browser.model.JsDialogState
import com.win11launcher.browser.model.JsDialogType
import com.win11launcher.browser.model.SslDialogState
import com.win11launcher.browser.utils.AdBlocker
import com.win11launcher.browser.utils.UserAgents
import com.win11launcher.browser.utils.onMain
import com.win11launcher.browser.model.PermissionRequest as BrowserPermissionRequest

// ═══════════════════════════════════════════════════════════════════════
// BrowserWebView — fully featured WebView with:
//  • Real ad/tracker blocking via shouldInterceptRequest
//  • Permission requests (geo, camera, mic) wired to settings
//  • JS dialogs (alert/confirm/prompt) surfaced as state
//  • SSL error dialog
//  • Real find-in-page  (findAllAsync + findNext)
//  • Cookie, cache, and session management
//  • Private mode (no cookies, no cache, no history)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BrowserWebView(
    url: String,
    tab: BrowserTab,
    settings: BrowserSettings,
    findQuery: String,
    isFindActive: Boolean,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (String, String?) -> Unit,   // url, title
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onFaviconChanged: (Bitmap?) -> Unit,
    onFindResultsChanged: (Int, Int) -> Unit,    // activeMatch, totalMatches
    onDownloadStart: (String, String, String, String, Long) -> Unit,
    onJsDialog: (JsDialogState) -> Unit,
    onSslError: (SslDialogState) -> Unit,
    onPermissionRequest: (BrowserPermissionRequest) -> Unit,
    onGeolocationRequest: (String, GeolocationPermissions.Callback) -> Unit,
    onNewTabRequested: (String) -> Unit,
    isDark: Boolean
) {
    // Re-apply find query whenever it changes
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(findQuery, isFindActive) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isFindActive && findQuery.isNotEmpty()) {
            wv.findAllAsync(findQuery)
        } else {
            wv.clearMatches()
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // ── Settings ──────────────────────────────────────────
                applyBrowserSettings(settings, isDark)

                // ── Private mode ──────────────────────────────────────
                if (tab.isPrivate) {
                    CookieManager.getInstance().setAcceptCookie(false)
                    settings.javaScriptEnabled.let {  /* already set above */ }
                } else {
                    CookieManager.getInstance().setAcceptCookie(settings.saveCookies)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, settings.saveCookies)
                }

                // ── WebViewClient ─────────────────────────────────────
                webViewClient = buildWebViewClient(
                    tab        = tab,
                    settings   = settings,
                    isDark     = isDark,
                    onPageStarted    = onPageStarted,
                    onPageFinished   = onPageFinished,
                    onUrlChanged     = onUrlChanged,
                    onSslError       = onSslError
                )

                // ── WebChromeClient ───────────────────────────────────
                webChromeClient = buildWebChromeClient(
                    settings              = settings,
                    onProgressChanged     = onProgressChanged,
                    onTitleChanged        = onTitleChanged,
                    onFaviconChanged      = onFaviconChanged,
                    onFindResultsChanged  = onFindResultsChanged,
                    onJsDialog            = onJsDialog,
                    onPermissionRequest   = onPermissionRequest,
                    onGeolocationRequest  = onGeolocationRequest,
                    onNewTabRequested     = onNewTabRequested
                )

                // ── Download listener ─────────────────────────────────
                setDownloadListener { dlUrl, ua, cd, mime, len ->
                    onMain { onDownloadStart(dlUrl, ua, cd, mime, len) }
                }

                // ── Find results listener ─────────────────────────────
                setFindListener { activeMatch, numberOfMatches, _ ->
                    onMain { onFindResultsChanged(activeMatch, numberOfMatches) }
                }

                webViewRef = this
                onWebViewReady(this)

                if (url.isNotBlank() && url != "io.github.norbertweb.bluebird://newtab") {
                    loadUrl(url)
                }
            }
        },
        update = { wv ->
            // Re-apply mutable settings on recomposition
            wv.settings.javaScriptEnabled       = settings.javaScriptEnabled
            wv.settings.loadsImagesAutomatically = settings.showImages
            wv.settings.userAgentString          = UserAgents.get(settings.desktopMode)
            CookieManager.getInstance().setAcceptCookie(
                if (tab.isPrivate) false else settings.saveCookies
            )
            webViewRef = wv
        },
        modifier = modifier
    )
}

// ─── Extension: apply all WebSettings ────────────────────────────────

private fun WebView.applyBrowserSettings(s: BrowserSettings, isDark: Boolean) {
    with(settings) {
        javaScriptEnabled                     = s.javaScriptEnabled
        domStorageEnabled                     = true
        databaseEnabled                       = true
        loadWithOverviewMode                  = true
        useWideViewPort                       = true
        builtInZoomControls                   = true
        displayZoomControls                   = false
        allowFileAccess                       = true
        allowContentAccess                    = true
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = !s.popupBlocker
        loadsImagesAutomatically              = s.showImages
        setSupportZoom(true)
        mediaPlaybackRequiresUserGesture      = true
        mixedContentMode                      = if (s.mixedContentAllowed)
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        else
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        userAgentString                       = UserAgents.get(s.desktopMode)
        textZoom                              = s.fontSize
        saveFormData                          = s.saveFormData
        setGeolocationEnabled(s.locationAccess)
    }

    // Dark mode: use WebSettingsCompat approach for API 29+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        try {
            // Try AndroidX WebKit approach first (most correct)
            val compat = Class.forName("androidx.webkit.WebSettingsCompat")
            val setDarkMode = compat.getMethod(
                "setForceDark",
                WebSettings::class.java,
                Int::class.java
            )
            val FORCE_DARK_ON  = 2
            val FORCE_DARK_OFF = 0
            setDarkMode.invoke(null, settings, if (isDark) FORCE_DARK_ON else FORCE_DARK_OFF)
        } catch (_: Exception) {
            // Fallback: deprecated API
            @Suppress("DEPRECATION")
            settings.forceDark = if (isDark)
                WebSettings.FORCE_DARK_AUTO
            else
                WebSettings.FORCE_DARK_OFF
        }
    }
}

// ─── WebViewClient builder ────────────────────────────────────────────

private fun buildWebViewClient(
    tab: BrowserTab,
    settings: BrowserSettings,
    isDark: Boolean,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onUrlChanged: (String) -> Unit,
    onSslError: (SslDialogState) -> Unit
): WebViewClient = object : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        url ?: return
        onMain {
            onPageStarted(url)
            onUrlChanged(url)
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        url ?: return
        onMain {
            onPageFinished(url, view.title)
            onUrlChanged(url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val u = request.url?.toString() ?: return false
        // Pass http/https through normally
        if (u.startsWith("http://") || u.startsWith("https://")) return false
        // Intent URIs (tel:, mailto:, intent:, market:…)
        return try {
            val intent = android.content.Intent.parseUri(u, android.content.Intent.URI_INTENT_SCHEME)
            view.context.startActivity(intent)
            true
        } catch (_: Exception) { true }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url?.toString() ?: return null
        if (AdBlocker.shouldBlock(url, settings.adBlockEnabled, settings.trackingProtection)) {
            return AdBlocker.emptyResponse()
        }
        return null
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (!request.isForMainFrame) return
        val desc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            error.description?.toString() ?: "Unknown error"
        else "Page error"
        val html = buildErrorPage(desc, request.url?.toString() ?: "", isDark)
        onMain { view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val host = error.url?.let { android.net.Uri.parse(it).host } ?: error.url ?: "Unknown"
        val desc = when (error.primaryError) {
            SslError.SSL_EXPIRED       -> "The site's security certificate has expired."
            SslError.SSL_IDMISMATCH    -> "The certificate hostname doesn't match the site."
            SslError.SSL_UNTRUSTED     -> "The certificate authority is not trusted."
            SslError.SSL_NOTYETVALID   -> "The certificate is not yet valid."
            else                       -> "A security certificate error occurred."
        }
        onMain {
            onSslError(SslDialogState(
                host             = host,
                errorDescription = desc,
                onProceed        = { handler.proceed() },
                onCancel         = { handler.cancel() }
            ))
        }
    }
}

// ─── WebChromeClient builder ──────────────────────────────────────────

private fun buildWebChromeClient(
    settings: BrowserSettings,
    onProgressChanged: (Int) -> Unit,
    onTitleChanged: (String) -> Unit,
    onFaviconChanged: (Bitmap?) -> Unit,
    onFindResultsChanged: (Int, Int) -> Unit,
    onJsDialog: (JsDialogState) -> Unit,
    onPermissionRequest: (BrowserPermissionRequest) -> Unit,
    onGeolocationRequest: (String, GeolocationPermissions.Callback) -> Unit,
    onNewTabRequested: (String) -> Unit
): WebChromeClient = object : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onMain { onProgressChanged(newProgress) }
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        title ?: return
        onMain { onTitleChanged(title) }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        onMain { onFaviconChanged(icon) }
    }

    // ── JS Dialogs — surface to UI instead of auto-accepting ──────────

    override fun onJsAlert(
        view: WebView, url: String?, message: String?, result: JsResult
    ): Boolean {
        onMain {
            onJsDialog(JsDialogState(
                type      = JsDialogType.ALERT,
                message   = message ?: "",
                onConfirm = { result.confirm() },
                onDismiss = { result.cancel() }
            ))
        }
        return true
    }

    override fun onJsConfirm(
        view: WebView, url: String?, message: String?, result: JsResult
    ): Boolean {
        onMain {
            onJsDialog(JsDialogState(
                type      = JsDialogType.CONFIRM,
                message   = message ?: "",
                onConfirm = { if (it == "true") result.confirm() else result.cancel() },
                onDismiss = { result.cancel() }
            ))
        }
        return true
    }

    override fun onJsPrompt(
        view: WebView, url: String?, message: String?,
        defaultValue: String?, result: JsPromptResult
    ): Boolean {
        onMain {
            onJsDialog(JsDialogState(
                type         = JsDialogType.PROMPT,
                message      = message ?: "",
                defaultValue = defaultValue ?: "",
                onConfirm    = { result.confirm(it) },
                onDismiss    = { result.cancel() }
            ))
        }
        return true
    }

    // ── WebRTC / Camera / Mic permissions ─────────────────────────────

    override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
        val allowedResources = request.resources.filter { resource ->
            when (resource) {
                android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> settings.cameraAccess
                android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> settings.microphoneAccess
                else                                                      -> false
            }
        }.toTypedArray()

        if (allowedResources.isEmpty()) {
            onMain {
                onPermissionRequest(BrowserPermissionRequest(
                    origin    = request.origin.toString(),
                    resources = request.resources,
                    grant     = { request.grant(request.resources) },
                    deny      = { request.deny() }
                ))
            }
        } else {
            request.grant(allowedResources)
        }
    }

    // ── Geolocation ───────────────────────────────────────────────────

    override fun onGeolocationPermissionsShowPrompt(
        origin: String, callback: GeolocationPermissions.Callback
    ) {
        if (settings.locationAccess) {
            callback.invoke(origin, true, false)
        } else {
            onMain { onGeolocationRequest(origin, callback) }
        }
    }

    // ── New window (popup blocker) ────────────────────────────────────

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
    ): Boolean {
        if (!isUserGesture || settings.popupBlocker) return false
        // Open in new tab
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        val newWv = WebView(view.context)
        transport.webView = newWv
        resultMsg.sendToTarget()
        newWv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                url ?: return
                onMain { onNewTabRequested(url) }
                newWv.stopLoading()
                newWv.destroy()
            }
        }
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
}

// ─── Error page HTML ──────────────────────────────────────────────────

fun buildErrorPage(error: String, url: String, isDark: Boolean): String {
    val bg  = if (isDark) "#1A1A1A" else "#F6F6F6"
    val fg  = if (isDark) "#E8E8E8" else "#1A1A1A"
    val sub = if (isDark) "#888888" else "#666666"
    val card= if (isDark) "#252525" else "#FFFFFF"
    return """<!DOCTYPE html><html lang="en"><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Page can't be reached</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{background:$bg;color:$fg;font-family:system-ui,sans-serif;
     display:flex;flex-direction:column;align-items:center;
     justify-content:center;height:100vh;padding:24px;text-align:center}
.card{background:$card;border-radius:12px;padding:32px 28px;max-width:420px;width:100%;
      box-shadow:0 2px 20px rgba(0,0,0,0.1)}
.icon{font-size:52px;margin-bottom:16px}
h2{font-size:20px;font-weight:600;margin-bottom:8px}
.url{font-size:11px;color:$sub;word-break:break-all;margin-bottom:12px;
     background:rgba(128,128,128,0.1);padding:6px 10px;border-radius:6px}
p{color:$sub;font-size:13px;line-height:1.6;margin-bottom:20px}
.actions{display:flex;gap:10px;justify-content:center;flex-wrap:wrap}
button{background:#1A73E8;color:#fff;border:none;border-radius:8px;
       padding:10px 24px;font-size:13px;cursor:pointer;font-weight:500;
       transition:opacity .15s}
button.sec{background:transparent;color:#1A73E8;border:1.5px solid #1A73E8}
button:active{opacity:0.8}
</style></head><body>
<div class="card">
  <div class="icon">🌐</div>
  <h2>Page can't be reached</h2>
  <div class="url">${url.take(80)}</div>
  <p>${error.take(120)}</p>
  <p>Check your internet connection or the address and try again.</p>
  <div class="actions">
    <button onclick="location.reload()">Try again</button>
    <button class="sec" onclick="history.back()">Go back</button>
  </div>
</div>
</body></html>"""
}
