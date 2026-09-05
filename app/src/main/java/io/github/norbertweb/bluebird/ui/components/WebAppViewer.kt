package io.github.norbertweb.bluebird.ui.components

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

/**
 * Runtime for an installed Bluebird web application.
 *
 * This is intentionally NOT a browser. There is no URL/address bar and no
 * back/forward browser chrome. The application owns the entire client area.
 * Internal links inside the packaged app continue to work normally.
 */
@Composable
fun WebAppViewerScreen(
    isDark: Boolean,
    app: InstalledBpkApp
) {
    val background = if (isDark) Color(0xFF111111) else Color(0xFFF7F7F7)
    val text = if (isDark) Color(0xFFEDEDED) else Color(0xFF1A1A1A)
    var isLoading by remember(app.id) { mutableStateOf(true) }
    var webViewRef by remember(app.id) { mutableStateOf<WebView?>(null) }

    val entryFile = remember(app.installDir, app.entry) {
        File(app.installDir, app.entry).canonicalFile
    }
    val root = remember(app.installDir) { File(app.installDir).canonicalFile }
    val validEntry = remember(entryFile, root) {
        entryFile.isFile &&
            (entryFile.path == root.path || entryFile.path.startsWith(root.path + File.separator))
    }

    Box(Modifier.fillMaxSize().background(background)) {
        if (validEntry) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            setSupportZoom(false)
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }
                        webViewRef = this
                        loadUrl(entryFile.toURI().toString())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopStart),
                    color = Color.Unspecified
                )
            }
        } else {
            Text(
                text = "Application entry point is invalid.",
                color = text,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
