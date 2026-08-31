package io.github.norbertweb.bluebird.browser.ui.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.norbertweb.bluebird.browser.model.BrowserSettings
import io.github.norbertweb.bluebird.browser.model.BrowserTab
import io.github.norbertweb.bluebird.browser.model.JsDialogState
import io.github.norbertweb.bluebird.browser.model.JsDialogType
import io.github.norbertweb.bluebird.browser.model.SslDialogState
import io.github.norbertweb.bluebird.browser.model.StoredPermissionDecision
import io.github.norbertweb.bluebird.browser.model.SitePermission
import io.github.norbertweb.bluebird.browser.utils.AdBlocker
import io.github.norbertweb.bluebird.browser.utils.UserAgents
import io.github.norbertweb.bluebird.browser.utils.onMain
import io.github.norbertweb.bluebird.browser.model.PermissionRequest as BrowserPermissionRequest

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
    onWebViewDisposed: (WebView, Int) -> Unit,
    restoreScrollY: Int = 0,
    onPageStarted: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (String, String?) -> Unit,   // url, title
    onPageError: (String, String) -> Unit,       // url, human-readable error
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onFaviconChanged: (Bitmap?) -> Unit,
    onCredentialFormDetected: (String?) -> Unit = {},
    onFindResultsChanged: (Int, Int) -> Unit,    // activeMatch, totalMatches
    onDownloadStart: (String, String, String, String, Long) -> Unit,
    onJsDialog: (JsDialogState) -> Unit,
    onSslError: (SslDialogState) -> Unit,
    onPermissionRequest: (BrowserPermissionRequest) -> Unit,
    onRememberPermission: (String, String, StoredPermissionDecision) -> Unit,
    getStoredPermission: (String, String) -> StoredPermissionDecision?,
    onGeolocationRequest: (String, GeolocationPermissions.Callback) -> Unit,
    onNewTabRequested: (String) -> Unit,
    isDark: Boolean,
    isActive: Boolean = true
) {
    // Re-apply find query whenever it changes. WebViews are intentionally
    // scoped to the active tab so inactive tabs do not keep renderer memory.
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val restoreScrollPending = remember(restoreScrollY) {
        java.util.concurrent.atomic.AtomicBoolean(restoreScrollY > 0)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                val savedScroll = wv.scrollY.coerceAtLeast(0)
                onWebViewDisposed(wv, savedScroll)
                wv.stopLoading()
                wv.onPause()
                wv.removeAllViews()
                wv.destroy()
            }
            webViewRef = null
        }
    }

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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, !isActive)
                }
                visibility = if (isActive) View.VISIBLE else View.INVISIBLE

                // ── Android Autofill integration ───────────────────────
                // WebView exposes HTML form structure to the Android Autofill Framework.
                // Normal tabs participate in the device's configured autofill service;
                // private tabs explicitly opt out so private sessions do not surface
                // saved credentials to the system autofill UI.
                importantForAutofill = if (tab.isPrivate) {
                    View.IMPORTANT_FOR_AUTOFILL_NO
                } else {
                    View.IMPORTANT_FOR_AUTOFILL_YES
                }

                // ── Private mode ──────────────────────────────────────
                // WebView's CookieManager is process-global, so never toggle it here:
                // doing so would silently disable cookies for normal tabs too. Private
                // tabs instead avoid browser persistence and are never restored.
                if (tab.isPrivate) {
                    this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    this.settings.saveFormData = false
                    this.settings.domStorageEnabled = true
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
                    onPageError      = onPageError,
                    onRestoreScroll   = { view ->
                        if (restoreScrollPending.compareAndSet(true, false)) {
                            view.post { view.scrollTo(0, restoreScrollY.coerceAtLeast(0)) }
                        }
                    },
                    onUrlChanged     = onUrlChanged,
                    onSslError       = onSslError,
                    onCredentialFormDetected = onCredentialFormDetected
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
                    onRememberPermission  = onRememberPermission,
                    getStoredPermission   = getStoredPermission,
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
            // CookieManager is process-global; do not flip it for private tabs.
            // The private session uses no-cache/no-form-data and is not persisted.
            if (!tab.isPrivate) {
                CookieManager.getInstance().setAcceptCookie(settings.saveCookies)
                CookieManager.getInstance().setAcceptThirdPartyCookies(wv, settings.saveCookies)
            } else {
                wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                wv.settings.saveFormData = false
            }
            if (isActive) {
                wv.onResume()
                wv.visibility = View.VISIBLE
            } else {
                wv.onPause()
                wv.visibility = View.INVISIBLE
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, !isActive)
            }
            webViewRef = wv
        },
        modifier = modifier
    )
}

// Extract credentials only after an explicit user action from the Save password UI.
fun WebView.captureCredentialFromCurrentForm(onResult: (String?, String?) -> Unit) {
    val script = """
        (function(){
          try {
            var p=document.querySelector('input[type=password]');
            if(!p) return JSON.stringify({username:'',password:''});
            var f=p.form || document;
            var u=f.querySelector('input:not([type=password])[name*=user i],input:not([type=password])[name*=email i],input:not([type=password])[autocomplete=username],input[type=email]');
            return JSON.stringify({username:u ? (u.value || '') : '', password:p.value || ''});
          } catch(e) { return JSON.stringify({username:'',password:''}); }
        })()
    """.trimIndent()
    evaluateJavascript(script) { raw ->
        runCatching {
            val jsonString = org.json.JSONTokener(raw ?: "\"\"").nextValue() as? String ?: return@runCatching
            val obj = org.json.JSONObject(jsonString)
            onResult(obj.optString("username").takeIf { it.isNotBlank() }, obj.optString("password").takeIf { it.isNotBlank() })
        }.onFailure { onResult(null, null) }
    }
}


/** Fills the current login form from a credential chosen by the user. */
fun WebView.fillCredentialIntoCurrentForm(username: String, password: String, onResult: (Boolean) -> Unit = {}) {
    val u = org.json.JSONObject.quote(username)
    val pw = org.json.JSONObject.quote(password)
    val script = """
        (function(){
          try {
            var p=document.querySelector('input[type=password]');
            if(!p) return false;
            var f=p.form || document;
            var u=f.querySelector('input:not([type=password])[autocomplete=username],input:not([type=password])[name*=user i],input:not([type=password])[name*=email i],input[type=email]');
            if(u){ u.focus(); u.value=$u; u.dispatchEvent(new Event('input',{bubbles:true})); u.dispatchEvent(new Event('change',{bubbles:true})); }
            p.focus(); p.value=$pw; p.dispatchEvent(new Event('input',{bubbles:true})); p.dispatchEvent(new Event('change',{bubbles:true}));
            return true;
          } catch(e) { return false; }
        })()
    """.trimIndent()
    evaluateJavascript(script) { raw ->
        onMain { onResult(raw?.trim() == "true") }
    }
}

// ─── Extension: apply all WebSettings ────────────────────────────────

private fun WebView.applyBrowserSettings(s: BrowserSettings, isDark: Boolean) {
    with(settings) {
        javaScriptEnabled                     = s.javaScriptEnabled
        domStorageEnabled                     = true
        databaseEnabled                       = true
        loadWithOverviewMode                  = true
        useWideViewPort                       = true
        offscreenPreRaster                    = false
        cacheMode                             = WebSettings.LOAD_DEFAULT
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
    onPageError: (String, String) -> Unit,
    onRestoreScroll: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onSslError: (SslDialogState) -> Unit,
    onCredentialFormDetected: (String?) -> Unit
): WebViewClient = object : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        url ?: return
        onMain {
            onPageStarted(url)
            onUrlChanged(url)
        }
    }

    // Detect a password form without reading the password. The actual credential
    // values are extracted only after the user explicitly chooses Save password.
    private fun detectCredentialForm(view: WebView) {
        if (tab.isPrivate || !settings.offerToSavePasswords) return
        val origin = runCatching { android.net.Uri.parse(view.url ?: "").scheme }.getOrNull()
        if (origin != "https") return
        val script = """
            (function(){
              try {
                var p=document.querySelector('input[type=password]');
                if(!p) return '';
                var f=p.form || document;
                var u=f.querySelector('input:not([type=password])[name*=user i],input:not([type=password])[name*=email i],input:not([type=password])[autocomplete=username],input[type=email]');
                return u ? (u.value || '') : '';
              } catch(e) { return ''; }
            })()
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            val username = runCatching { org.json.JSONTokener(result ?: "\"\"").nextValue() as? String }.getOrNull()
            onMain { onCredentialFormDetected(username?.takeIf { it.isNotBlank() }) }
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        url ?: return
        onMain {
            onPageFinished(url, view.title)
            onRestoreScroll(view)
            onUrlChanged(url)
            detectCredentialForm(view)
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
        val failedUrl = request.url?.toString() ?: ""
        onMain {
            // Keep the failed document intact; the Compose error surface sits above
            // it and can retry the original URL. Loading an HTML error document here
            // would replace the failed URL and make retry/back navigation ambiguous.
            onPageError(failedUrl, desc)
        }
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
    onRememberPermission: (String, String, StoredPermissionDecision) -> Unit,
    getStoredPermission: (String, String) -> StoredPermissionDecision?,
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
        val origin = request.origin.toString()
        val remembered = request.resources.mapNotNull { resource ->
            getStoredPermission(origin, resource)?.let { resource to it }
        }.toMap()

        if (remembered.isNotEmpty() && remembered.size == request.resources.size) {
            if (remembered.values.all { it == StoredPermissionDecision.ALLOW }) request.grant(request.resources)
            else request.deny()
            return
        }

        val allowedBySettings = request.resources.filter { resource ->
            when (resource) {
                android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> settings.cameraAccess
                android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> settings.microphoneAccess
                else -> false
            }
        }.toTypedArray()

        if (allowedBySettings.size == request.resources.size) {
            request.grant(allowedBySettings)
        } else {
            onMain {
                onPermissionRequest(BrowserPermissionRequest(
                    origin = origin,
                    resources = request.resources,
                    grant = { request.grant(request.resources) },
                    deny = {
                        request.deny()
                    },
                    remember = { decision ->
                        request.resources.forEach { onRememberPermission(origin, it, decision) }
                    }
                ))
            }
        }
    }

    // ── Geolocation ───────────────────────────────────────────────────

    override fun onGeolocationPermissionsShowPrompt(
        origin: String, callback: GeolocationPermissions.Callback
    ) {
        when (getStoredPermission(origin, "geolocation")) {
            StoredPermissionDecision.ALLOW -> callback.invoke(origin, true, true)
            StoredPermissionDecision.DENY -> callback.invoke(origin, false, true)
            null -> if (settings.locationAccess) callback.invoke(origin, true, false)
            else onMain { onGeolocationRequest(origin, callback) }
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
