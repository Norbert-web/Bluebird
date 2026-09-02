package io.github.norbertweb.bluebird.editor.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.norbertweb.bluebird.editor.core.TabData
import io.github.norbertweb.bluebird.editor.ui.theme.EditorColors
import io.github.norbertweb.bluebird.ui.components.FluentIcon

fun isWebPreviewSupported(fileName: String): Boolean = when (fileName.substringAfterLast('.', "").lowercase()) {
    "html", "htm", "css", "js", "jsx", "mjs" -> true
    else -> false
}

@Composable
fun LivePreviewPane(tab: TabData, colors: EditorColors, modifier: Modifier = Modifier) {
    val ext = tab.fileName.substringAfterLast('.', "").lowercase()
    var previewHtml by remember(tab.id, ext) { mutableStateOf(buildPreviewDocument(tab.content.text, ext)) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    LaunchedEffect(tab.id, tab.content.text, ext) {
        delay(250)
        previewHtml = buildPreviewDocument(tab.content.text, ext)
    }
    LaunchedEffect(previewHtml) {
        webView?.loadDataWithBaseURL("https://bluebird.local/", previewHtml, "text/html", "UTF-8", null)
    }
    Column(modifier.background(colors.surface).border(1.dp, colors.border)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(FluentIcon.Globe, null, tint = colors.accent, modifier = Modifier.size(15.dp))
            Text("Live Preview", color = colors.text, fontSize = 11.sp)
            Text(ext.uppercase(), color = colors.textMuted, fontSize = 9.sp)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    setBackgroundColor(Color.TRANSPARENT.hashCode())
                    webView = this
                    loadDataWithBaseURL("https://bluebird.local/", previewHtml, "text/html", "UTF-8", null)
                }
            },
            update = { view ->
                webView = view
            }
        )
    }
}

private fun buildPreviewDocument(source: String, ext: String): String = when (ext) {
    "html", "htm" -> source
    "css" -> """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>${source.replace("</style>", "<\/style>")}</style></head>
        <body><main class="preview-card"><h1>Bluebird CSS Preview</h1><p>Style the preview to see your CSS changes live.</p><button>Button</button><a href="#">Link</a><input placeholder="Input"><div class="sample">Sample container</div></main></body></html>
    """.trimIndent()
    "js", "jsx", "mjs" -> """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body><main id="app"><h1>Bluebird JavaScript Preview</h1><p id="output">Run your script against this page.</p><button id="action">Interact</button></main>
        <script>${source.replace("</script>", "<\/script>")}</script></body></html>
    """.trimIndent()
    else -> "<html><body></body></html>"
}
