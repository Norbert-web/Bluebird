package com.bluebird.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.UUID

// ─────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────
data class InstalledWebApp(
    val id: String         = UUID.randomUUID().toString(),
    val name: String,
    val url: String,            // https://… OR "file://custom" for custom apps
    val iconEmoji: String  = "🌐",
    val iconPath: String   = "",       // real fetched favicon, relative to context.filesDir; "" = use iconEmoji
    val accentColor: Long  = 0xFF0078D4,
    val isCustom: Boolean  = false,   // written in HTML/CSS/JS
    val htmlContent: String = "",      // only used when isCustom = true
    val installedAt: Long  = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────
// WebAppPreferences — persists installed web apps
// ─────────────────────────────────────────────────────────────────
class WebAppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("web_apps", Context.MODE_PRIVATE)

    fun save(apps: List<InstalledWebApp>) {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(JSONObject().apply {
                put("id",           app.id)
                put("name",         app.name)
                put("url",          app.url)
                put("iconEmoji",    app.iconEmoji)
                put("iconPath",     app.iconPath)
                put("accentColor",  app.accentColor)
                put("isCustom",     app.isCustom)
                put("htmlContent",  app.htmlContent)
                put("installedAt",  app.installedAt)
            })
        }
        prefs.edit().putString("apps", arr.toString()).apply()
    }

    fun load(): List<InstalledWebApp> {
        val json = prefs.getString("apps", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                InstalledWebApp(
                    id          = o.getString("id"),
                    name        = o.getString("name"),
                    url         = o.getString("url"),
                    iconEmoji   = o.optString("iconEmoji", "🌐"),
                    iconPath    = o.optString("iconPath", ""),
                    accentColor = o.optLong("accentColor", 0xFF0078D4L),
                    isCustom    = o.optBoolean("isCustom", false),
                    htmlContent = o.optString("htmlContent", ""),
                    installedAt = o.optLong("installedAt", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}

// ─────────────────────────────────────────────────────────────────
// Emoji options for icon picker
// ─────────────────────────────────────────────────────────────────
private val EMOJI_OPTIONS = listOf(
    "🌐","📱","💻","🎮","📊","🎵","🎨","📝","🔧","⚙️",
    "📰","🛒","💬","📧","📅","🗂️","🔒","🌍","🏠","🎯",
    "🚀","🤖","🧩","💡","📡","🔍","🎬","📸","🗺️","🏦"
)

private val ACCENT_OPTIONS = listOf(
    0xFF0078D4L, 0xFF107C10L, 0xFFD83B01L, 0xFF8764B8L,
    0xFFE3008CL, 0xFF00B7C3L, 0xFFFFB900L, 0xFFFF8C00L
)

// Default HTML template for new custom apps
private const val CUSTOM_APP_TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>My App</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      background: #1a1a2e;
      color: #cccccc;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px;
    }
    h1 { color: #4ec9b0; margin-bottom: 16px; }
    p  { opacity: 0.7; text-align: center; }
  </style>
</head>
<body>
  <h1>Hello from Bluebird!</h1>
  <p>Edit this HTML to build your custom app.<br>You have full access to HTML, CSS, and JavaScript.</p>
</body>
</html>"""

// ─────────────────────────────────────────────────────────────────
// Real favicon fetch — pulls the site's actual icon (via a favicon
// resolver service, since parsing arbitrary HTML for <link rel=icon>
// is unreliable) rather than ever fabricating one. Returns null on
// any failure so callers fall back to the user-picked emoji.
// ─────────────────────────────────────────────────────────────────
suspend fun fetchRealFavicon(siteUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val host = Uri.parse(siteUrl).host ?: return@withContext null
        val faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=$host"
        (URL(faviconUrl).openConnection()).run {
            connectTimeout = 6000
            readTimeout    = 6000
            getInputStream().use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Exception) { null }
}

// ─────────────────────────────────────────────────────────────────
// WebAppManagerScreen — the "app store" / install interface
// ─────────────────────────────────────────────────────────────────
@Composable
fun WebAppManagerScreen(
    isDark: Boolean,
    viewModel: com.bluebird.LauncherViewModel,
    onLaunchApp: (InstalledWebApp) -> Unit
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()

    val bg       = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF5F5F5)
    val surface  = if (isDark) Color(0xFF1A1A1A) else Color.White
    val tc       = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val tcDim    = if (isDark) Color(0xFF888888) else Color(0xFF666666)
    val accent   = Color(0xFF0078D4)

    // Single source of truth is the ViewModel (backed by WebAppPreferences +
    // the real .webapp files on Desktop) — no separate local copy to drift.
    val apps          = uiState.installedWebApps
    var showInstall   by remember { mutableStateOf(false) }
    var showCustom    by remember { mutableStateOf(false) }
    var isInstalling  by remember { mutableStateOf(false) }
    var deleteTarget  by remember { mutableStateOf<InstalledWebApp?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // ── Header ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Language, null, tint = accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Web Apps", color = tc, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${apps.size} installed", color = tcDim, fontSize = 11.sp)
            }
            // Install from URL
            OutlinedButton(
                onClick = { showInstall = true },
                shape   = RoundedCornerShape(6.dp),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("From URL", fontSize = 12.sp)
            }
            // Create custom
            Button(
                onClick = { showCustom = true },
                shape   = RoundedCornerShape(6.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Custom App", fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0))

        if (apps.isEmpty()) {
            // Empty state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🌐", fontSize = 48.sp)
                    Text("No web apps installed", color = tc, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Install a web app from a URL,\nor create your own with HTML/CSS/JS.",
                        color = tcDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showInstall = true }) { Text("Install from URL") }
                        Button(onClick = { showCustom = true }) { Text("Create Custom App") }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps, key = { it.id }) { app ->
                    WebAppTile(
                        app     = app,
                        isDark  = isDark,
                        onClick = { onLaunchApp(app) },
                        onDelete = { deleteTarget = app }
                    )
                }
            }
        }
    }

    // ── Install from URL dialog ──────────────────────────────────
    if (showInstall) {
        InstallFromUrlDialog(
            isDark       = isDark,
            isInstalling = isInstalling,
            onInstall    = { newApp ->
                isInstalling = true
                scope.launch {
                    val favicon = fetchRealFavicon(newApp.url)
                    viewModel.installWebApp(
                        name        = newApp.name,
                        url         = newApp.url,
                        favicon     = favicon,
                        accentColor = newApp.accentColor,
                        isCustom    = false
                    )
                    isInstalling = false
                    showInstall  = false
                }
            },
            onDismiss = { if (!isInstalling) showInstall = false }
        )
    }

    // ── Custom HTML/CSS/JS editor dialog ────────────────────────
    if (showCustom) {
        CustomAppEditorDialog(
            isDark    = isDark,
            onSave    = { newApp ->
                viewModel.installWebApp(
                    name        = newApp.name,
                    url         = newApp.url,
                    favicon     = null,
                    accentColor = newApp.accentColor,
                    isCustom    = true,
                    htmlContent = newApp.htmlContent
                )
                showCustom = false
            },
            onDismiss = { showCustom = false }
        )
    }

    // ── Delete confirmation ──────────────────────────────────────
    deleteTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = if (isDark) Color(0xFF1E1E1E) else Color.White,
            title = { Text("Uninstall \"${app.name}\"?", color = if (isDark) Color.White else Color.Black) },
            text  = { Text("This will remove the app and all its data.", color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.uninstallWebApp(app.id); deleteTarget = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE81123))
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// App tile in the grid
// ─────────────────────────────────────────────────────────────────
@Composable
private fun WebAppTile(
    app: InstalledWebApp,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = if (isDark) Color(0xFF1C1C1C) else Color.White
    val tc      = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val tcDim   = if (isDark) Color(0xFF888888) else Color(0xFF666666)
    val accent  = Color(app.accentColor)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icon circle with accent colour — real favicon when we have one, emoji as fallback
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(accent.copy(0.15f), CircleShape)
                        .border(1.dp, accent.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val favicon by produceState<Bitmap?>(initialValue = null, app.iconPath) {
                        value = if (app.iconPath.isNotBlank()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val f = java.io.File(context.filesDir, app.iconPath)
                                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                } catch (_: Exception) { null }
                            }
                        } else null
                    }
                    if (favicon != null) {
                        Image(
                            bitmap = favicon!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Text(app.iconEmoji, fontSize = 22.sp)
                    }
                }

                Text(
                    text       = app.name,
                    color      = tc,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    textAlign  = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (app.isCustom) Icons.Default.Code else Icons.Default.Language,
                        null, tint = tcDim, modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text     = if (app.isCustom) "Custom" else app.url.take(24),
                        color    = tcDim,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Overflow menu button
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick  = { showMenu = true },
                    modifier = Modifier.size(28.dp).padding(top = 4.dp, end = 4.dp)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = tcDim, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(if (isDark) Color(0xFF252525) else Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open", fontSize = 13.sp) },
                        onClick = { showMenu = false; onClick() },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Uninstall", fontSize = 13.sp, color = Color(0xFFE81123)) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFE81123), modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Install from URL dialog
// ─────────────────────────────────────────────────────────────────
@Composable
private fun InstallFromUrlDialog(
    isDark: Boolean,
    isInstalling: Boolean = false,
    onInstall: (InstalledWebApp) -> Unit,
    onDismiss: () -> Unit
) {
    val bg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val tc  = if (isDark) Color.White else Color(0xFF1A1A1A)

    var name       by remember { mutableStateOf("") }
    var url        by remember { mutableStateOf("https://") }
    var emoji      by remember { mutableStateOf("🌐") }
    var accentIdx  by remember { mutableIntStateOf(0) }
    var showEmoji  by remember { mutableStateOf(false) }
    var urlError   by remember { mutableStateOf("") }

    fun validate(): Boolean {
        urlError = ""
        if (name.isBlank()) { urlError = "App name is required"; return false }
        // Auto-prepend https:// for user convenience
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.isBlank() || url == "https://") {
                urlError = "Please enter a valid URL"
                return false
            }
            url = "https://$url"
        }
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Language, null, tint = Color(0xFF0078D4), modifier = Modifier.size(20.dp))
                Text("Install Web App", color = tc, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // App name
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("App Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(8.dp)
                )

                // URL
                OutlinedTextField(
                    value         = url,
                    onValueChange = { url = it; urlError = "" },
                    label         = { Text("URL") },
                    singleLine    = true,
                    isError       = urlError.isNotEmpty(),
                    supportingText = if (urlError.isNotEmpty()) ({ Text(urlError, color = Color(0xFFE81123)) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(8.dp),
                    leadingIcon   = { Icon(Icons.Default.Link, null) }
                )

                // Emoji icon picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Icon", color = tc.copy(0.6f), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(Color(ACCENT_OPTIONS[accentIdx]).copy(0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(ACCENT_OPTIONS[accentIdx]).copy(0.4f), RoundedCornerShape(8.dp))
                                .clickable { showEmoji = !showEmoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 20.sp) }
                        Text("Tap to change icon", color = tc.copy(0.5f), fontSize = 11.sp)
                    }
                    if (showEmoji) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement   = Arrangement.spacedBy(4.dp)
                        ) {
                            items(EMOJI_OPTIONS.size) { i ->
                                Box(
                                    Modifier
                                        .aspectRatio(1f)
                                        .background(
                                            if (EMOJI_OPTIONS[i] == emoji) Color(0xFF0078D4).copy(0.2f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { emoji = EMOJI_OPTIONS[i]; showEmoji = false },
                                    contentAlignment = Alignment.Center
                                ) { Text(EMOJI_OPTIONS[i], fontSize = 18.sp) }
                            }
                        }
                    }
                }

                // Accent colour
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Colour", color = tc.copy(0.6f), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ACCENT_OPTIONS.forEachIndexed { i, col ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(Color(col), CircleShape)
                                    .then(if (i == accentIdx)
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier)
                                    .clickable { accentIdx = i }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isInstalling,
                onClick = {
                    if (validate()) {
                        onInstall(InstalledWebApp(
                            name        = name.trim(),
                            url         = url.trim(),
                            iconEmoji   = emoji,
                            accentColor = ACCENT_OPTIONS[accentIdx],
                            isCustom    = false
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Fetching icon…", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Install", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = { TextButton(enabled = !isInstalling, onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────
// Custom HTML/CSS/JS editor dialog
// ─────────────────────────────────────────────────────────────────
@Composable
private fun CustomAppEditorDialog(
    isDark: Boolean,
    onSave: (InstalledWebApp) -> Unit,
    onDismiss: () -> Unit
) {
    val bg  = if (isDark) Color(0xFF1A1A1A) else Color.White
    val tc  = if (isDark) Color.White else Color(0xFF1A1A1A)
    val codeBg = Color(0xFF0D1117)

    var name       by remember { mutableStateOf("") }
    var html       by remember { mutableStateOf(CUSTOM_APP_TEMPLATE) }
    var emoji      by remember { mutableStateOf("🎨") }
    var accentIdx  by remember { mutableIntStateOf(1) }
    var activeTab  by remember { mutableStateOf(0) }   // 0=Info, 1=Code, 2=Preview
    var nameError  by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = bg,
        shape            = RoundedCornerShape(12.dp),
        modifier         = Modifier.fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Code, null, tint = Color(0xFF4EC9B0), modifier = Modifier.size(20.dp))
                Text("Create Custom App", color = tc, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Tab bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF111111) else Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Info", "Code", "Preview").forEachIndexed { i, label ->
                        val sel = activeTab == i
                        Box(
                            Modifier
                                .weight(1f)
                                .background(
                                    if (sel) if (isDark) Color(0xFF2A2A2A) else Color.White else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { activeTab = i }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) tc else tc.copy(0.5f), fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (activeTab) {
                    // ── Info tab ──────────────────────────────────────
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value         = name,
                                onValueChange = { name = it; nameError = "" },
                                label         = { Text("App Name") },
                                singleLine    = true,
                                isError       = nameError.isNotEmpty(),
                                supportingText = if (nameError.isNotEmpty()) ({ Text(nameError, color = Color(0xFFE81123)) }) else null,
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Icon", color = tc.copy(0.6f), fontSize = 11.sp)
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        modifier = Modifier.width(160.dp).height(100.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(EMOJI_OPTIONS.size) { i ->
                                            Box(
                                                Modifier
                                                    .aspectRatio(1f)
                                                    .background(
                                                        if (EMOJI_OPTIONS[i] == emoji) Color(0xFF0078D4).copy(0.25f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { emoji = EMOJI_OPTIONS[i] },
                                                contentAlignment = Alignment.Center
                                            ) { Text(EMOJI_OPTIONS[i], fontSize = 16.sp) }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Colour", color = tc.copy(0.6f), fontSize = 11.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ACCENT_OPTIONS.chunked(2).forEachIndexed { ri, row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                row.forEachIndexed { ci, col ->
                                                    val idx = ri * 2 + ci
                                                    Box(
                                                        Modifier
                                                            .size(28.dp)
                                                            .background(Color(col), CircleShape)
                                                            .then(if (idx == accentIdx) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                                            .clickable { accentIdx = idx }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Code tab ──────────────────────────────────────
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("HTML / CSS / JavaScript", color = tc.copy(0.7f), fontSize = 11.sp)
                                TextButton(
                                    onClick = { html = CUSTOM_APP_TEMPLATE },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("Reset", fontSize = 11.sp) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .heightIn(min = 240.dp)
                                    .background(codeBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                OutlinedTextField(
                                    value         = html,
                                    onValueChange = { html = it },
                                    modifier      = Modifier.fillMaxSize(),
                                    textStyle     = androidx.compose.ui.text.TextStyle(
                                        color      = Color(0xFFCCCCCC),
                                        fontSize   = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor   = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    minLines = 12
                                )
                            }
                        }
                    }

                    // ── Preview tab ───────────────────────────────────
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        ) {
                            WebAppViewer(
                                url         = null,
                                htmlContent = html,
                                isCustom    = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = "App name is required"; activeTab = 0; return@Button }
                    onSave(InstalledWebApp(
                        name        = name.trim(),
                        url         = "file://custom",
                        iconEmoji   = emoji,
                        accentColor = ACCENT_OPTIONS[accentIdx],
                        isCustom    = true,
                        htmlContent = html
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
            ) { Text("Save App", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────
// WebAppViewerScreen — full window for running a web app
// ─────────────────────────────────────────────────────────────────
@Composable
fun WebAppViewerScreen(
    isDark: Boolean,
    app: InstalledWebApp
) {
    val tc     = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val accent = Color(app.accentColor)

    var pageTitle   by remember { mutableStateOf(app.name) }
    var isLoading   by remember { mutableStateOf(true) }
    var canGoBack   by remember { mutableStateOf(false) }
    var canGoFwd    by remember { mutableStateOf(false) }
    var webViewRef  by remember { mutableStateOf<WebView?>(null) }
    var currentUrl  by remember { mutableStateOf(if (!app.isCustom) app.url else "") }
    var showUrlBar  by remember { mutableStateOf(false) }
    var urlInput    by remember { mutableStateOf(app.url) }

    Column(modifier = Modifier.fillMaxSize().background(if (isDark) Color(0xFF111111) else Color(0xFFF5F5F5))) {

        // ── Mini browser toolbar (only for URL apps) ──────────────
        if (!app.isCustom) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFEEEEEE))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick  = { webViewRef?.goBack() },
                    enabled  = canGoBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back",
                        tint = if (canGoBack) tc else tc.copy(0.3f),
                        modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick  = { webViewRef?.goForward() },
                    enabled  = canGoFwd,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, "Forward",
                        tint = if (canGoFwd) tc else tc.copy(0.3f),
                        modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick  = { webViewRef?.reload() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        "Reload", tint = tc, modifier = Modifier.size(16.dp)
                    )
                }

                // URL bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .background(if (isDark) Color(0xFF0C0C0C) else Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, if (isDark) Color(0xFF2A2A2A) else Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
                        .clickable { showUrlBar = true; urlInput = currentUrl }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF4EC9B0), modifier = Modifier.size(10.dp))
                        Text(
                            text     = currentUrl.removePrefix("https://").removePrefix("http://").take(50),
                            color    = tc.copy(0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Loading progress
                if (isLoading) {
                    CircularProgressIndicator(
                        color     = accent,
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Editable URL bar (shown when tapped)
            if (showUrlBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF0C0C0C) else Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value         = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f).height(46.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            currentUrl = urlInput
                            webViewRef?.loadUrl(urlInput)
                            showUrlBar = false
                        }),
                        shape         = RoundedCornerShape(6.dp),
                        textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    IconButton(onClick = { currentUrl = urlInput; webViewRef?.loadUrl(urlInput); showUrlBar = false }) {
                        Icon(Icons.Default.Send, "Go", tint = accent)
                    }
                    TextButton(onClick = { showUrlBar = false }) { Text("Cancel") }
                }
            }

            // Loading bar
            AnimatedVisibility(isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color    = accent
                )
            }
        }

        // ── WebView ───────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            WebAppViewer(
                url         = if (!app.isCustom) currentUrl else null,
                htmlContent = if (app.isCustom) app.htmlContent else null,
                isCustom    = app.isCustom,
                onPageStarted = { url, _ ->
                    isLoading = true
                    currentUrl = url ?: currentUrl
                    urlInput   = currentUrl
                },
                onPageFinished = { wv, url ->
                    isLoading = false
                    canGoBack = wv?.canGoBack() == true
                    canGoFwd  = wv?.canGoForward() == true
                    pageTitle = wv?.title?.takeIf { it.isNotBlank() } ?: app.name
                    currentUrl = url ?: currentUrl
                },
                onWebViewCreated = { wv -> webViewRef = wv }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// WebAppViewer — raw WebView composable
// ─────────────────────────────────────────────────────────────────
@Composable
fun WebAppViewer(
    url: String?,
    htmlContent: String?,
    isCustom: Boolean,
    onPageStarted:   ((String?, Bitmap?) -> Unit)? = null,
    onPageFinished:  ((WebView?, String?) -> Unit)? = null,
    onWebViewCreated: ((WebView) -> Unit)? = null
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled       = true
                    domStorageEnabled       = true
                    allowFileAccess         = isCustom
                    mixedContentMode        = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode               = WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode    = true
                    useWideViewPort         = true
                    builtInZoomControls     = false
                    displayZoomControls     = false
                    setSupportZoom(true)
                }
                webChromeClient = WebChromeClient()
                webViewClient   = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted?.invoke(url, favicon)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished?.invoke(view, url)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // Allow all navigation within the WebView
                        return false
                    }
                }
                onWebViewCreated?.invoke(this)
                if (isCustom && htmlContent != null) {
                    loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                } else if (url != null) {
                    loadUrl(url)
                }
            }
        },
        update = { wv ->
            // Only reload custom HTML when content actually changes; avoids re-rendering on every recomposition
            if (isCustom && htmlContent != null) {
                val lastHash = wv.tag as? Int
                val newHash  = htmlContent.hashCode()
                if (lastHash != newHash) {
                    wv.tag = newHash
                    wv.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
