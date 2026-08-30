package com.io.github.norbertweb.bluebird.browser.ui.panels
import com.io.github.norbertweb.bluebird.browser.ui.components.FluentIcon
import com.io.github.norbertweb.bluebird.browser.ui.components.FluentIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import com.io.github.norbertweb.bluebird.browser.model.Bookmark
import com.io.github.norbertweb.bluebird.browser.model.BookmarkFolder
import com.io.github.norbertweb.bluebird.browser.model.BrowserPanel
import com.io.github.norbertweb.bluebird.browser.model.BrowserSettings
import com.io.github.norbertweb.bluebird.browser.model.BrowserTab
import com.io.github.norbertweb.bluebird.browser.model.DownloadItem
import com.io.github.norbertweb.bluebird.browser.model.DownloadStatus
import com.io.github.norbertweb.bluebird.browser.model.HistoryEntry
import com.io.github.norbertweb.bluebird.browser.model.SearchEngine
import com.io.github.norbertweb.bluebird.browser.model.StartPage
import com.io.github.norbertweb.bluebird.browser.model.SitePermission
import com.io.github.norbertweb.bluebird.browser.model.StoredPermissionDecision
import com.io.github.norbertweb.bluebird.browser.model.TabGroup
import com.io.github.norbertweb.bluebird.browser.security.StoredCredential
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.view.ViewGroup
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.webkit.WebView
import android.webkit.WebViewClient

// ═══════════════════════════════════════════════════════════════════════
// SidePanel — container that routes to the right panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SidePanel(
    panel: BrowserPanel,
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    borderColor: Color,
    bookmarks: MutableList<Bookmark>,
    bookmarkFolders: MutableList<BookmarkFolder>,
    history: MutableList<HistoryEntry>,
    downloads: MutableList<DownloadItem>,
    settings: BrowserSettings,
    onSettingsChange: (BrowserSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onBookmarksChanged: () -> Unit = {},
    onHistoryChanged: () -> Unit = {},
    onDownloadOpen: (DownloadItem) -> Unit,
    onDownloadRemove: (DownloadItem) -> Unit,
    onDownloadCancel: (DownloadItem) -> Unit = {},
    onDownloadRetry: (DownloadItem) -> Unit = {},
    onShowDownloads: () -> Unit = {},
    onClearCompletedDownloads: () -> Unit = {},
    credentials: MutableList<StoredCredential> = mutableListOf(),
    onSaveCredential: (StoredCredential) -> Unit = {},
    onDeleteCredential: (StoredCredential) -> Unit = {},
    onOpenCredentialSite: (String) -> Unit = {},
    onRequestPasswordAuth: () -> Unit = {},
    passwordAuthGranted: Boolean = false,
    currentPageTitle: String,
    currentPageUrl: String,
    sitePermissions: List<SitePermission> = emptyList(),
    onClearSitePermissions: (String) -> Unit = {},
    onSetSitePermission: (String, String, StoredPermissionDecision) -> Unit = { _, _, _ -> },
    onResetSitePermission: (String, String) -> Unit = { _, _ -> },
    onClearSiteData: (String) -> Unit = {},
    selectedText: String = "",
    preparedPrompt: String = "",
    onAskAboutPage: (String) -> Unit = {},
    onAskAboutSelection: () -> Unit = {},
    onCopyPrompt: (String) -> Unit = {},
    onOpenChatGpt: () -> Unit = {},
    onClose: () -> Unit
) {
    val accentBlue = Color(0xFF1A73E8)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(surfaceColor)
            .border(
                BorderStroke(0.5.dp, borderColor),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Panel header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    when (panel) {
                        BrowserPanel.BOOKMARKS   -> "Bookmarks"
                        BrowserPanel.HISTORY     -> "History"
                        BrowserPanel.DOWNLOADS   -> "Downloads"
                        BrowserPanel.SETTINGS    -> "Settings"
                        BrowserPanel.EXTENSIONS  -> "Extensions"
                        BrowserPanel.COLLECTIONS -> "Collections"
                        BrowserPanel.CHATGPT      -> "ChatGPT"
                        BrowserPanel.SITE_SETTINGS -> "Site settings"
                        BrowserPanel.PASSWORDS -> "Passwords"
                        else                     -> ""
                    },
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = textColor
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    FluentIcon(FluentIcons.Close, null, tint = textColor.copy(0.5f), modifier = Modifier.size(14.dp))
                }
            }

            Divider(color = borderColor, thickness = 0.5.dp)

            when (panel) {
                BrowserPanel.CHATGPT    -> ChatGptPanel(
                    isDark = isDark,
                    pageTitle = currentPageTitle,
                    pageUrl = currentPageUrl,
                    selectedText = selectedText,
                    preparedPrompt = preparedPrompt,
                    onAskAboutPage = onAskAboutPage,
                    onAskAboutSelection = onAskAboutSelection,
                    onCopyPrompt = onCopyPrompt,
                    onOpenChatGpt = onOpenChatGpt
                )
                BrowserPanel.SITE_SETTINGS -> SiteSettingsPanel(isDark, textColor, currentPageUrl, sitePermissions, onClearSitePermissions, onSetSitePermission, onResetSitePermission, onClearSiteData)
                BrowserPanel.PASSWORDS -> PasswordManagerPanel(isDark, textColor, accentBlue, credentials, onSaveCredential, onDeleteCredential, onOpenCredentialSite, onRequestPasswordAuth, passwordAuthGranted)
                BrowserPanel.BOOKMARKS  -> BookmarksPanel(bookmarks, bookmarkFolders, isDark, textColor, accentBlue, onNavigate, onBookmarksChanged)
                BrowserPanel.HISTORY    -> HistoryPanel(history, isDark, textColor, accentBlue, onNavigate, { history.clear() }, onHistoryChanged)
                BrowserPanel.DOWNLOADS -> DownloadsPanel(
                    downloads, isDark, textColor, accentBlue,
                    onDownloadOpen, onDownloadRemove,
                    onDownloadCancel, onDownloadRetry,
                    onShowDownloads, onClearCompletedDownloads
                )
                BrowserPanel.SETTINGS   -> SettingsPanel(settings, isDark, textColor, accentBlue, onSettingsChange)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Coming soon", color = textColor.copy(0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Site Settings Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SiteSettingsPanel(
    isDark: Boolean,
    textColor: Color,
    currentUrl: String,
    permissions: List<SitePermission>,
    onClearSitePermissions: (String) -> Unit,
    onSetSitePermission: (String, String, StoredPermissionDecision) -> Unit,
    onResetSitePermission: (String, String) -> Unit,
    onClearSiteData: (String) -> Unit
) {
    val muted = textColor.copy(0.58f)
    val origin = runCatching { Uri.parse(currentUrl).let { if (it.scheme.isNullOrBlank() || it.host.isNullOrBlank()) "" else "${it.scheme}://${it.host}${if (it.port > 0) ":${it.port}" else ""}" } }.getOrDefault("")
    val siteEntries = permissions.filter { it.origin == origin }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FluentIcon(FluentIcons.Security, null, tint = textColor.copy(.7f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Connection & permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Text(if (origin.isBlank()) "This page has no site origin" else origin, fontSize = 9.sp, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Saved permissions", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
        Spacer(Modifier.height(6.dp))
        val resources = listOf(
            "geolocation" to "Location",
            android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE to "Camera",
            android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE to "Microphone"
        )
        resources.forEach { (resource, label) ->
            val current = siteEntries.firstOrNull { it.resource == resource }?.decision
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                FluentIcon(when (current) {
                    StoredPermissionDecision.ALLOW -> FluentIcons.CheckCircle
                    StoredPermissionDecision.DENY -> FluentIcons.Blocked
                    null -> FluentIcons.Info
                }, null, tint = when (current) {
                    StoredPermissionDecision.ALLOW -> Color(0xFF107C10)
                    StoredPermissionDecision.DENY -> Color(0xFFD13438)
                    null -> muted
                }, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                    Text(label, fontSize = 11.sp, color = textColor)
                    Text(when (current) {
                        StoredPermissionDecision.ALLOW -> "Allowed"
                        StoredPermissionDecision.DENY -> "Blocked"
                        null -> "Ask (default)"
                    }, fontSize = 9.sp, color = muted)
                }
                TextButton(enabled = origin.isNotBlank() && current != StoredPermissionDecision.ALLOW, onClick = { onSetSitePermission(origin, resource, StoredPermissionDecision.ALLOW) }) {
                    Text("Allow", fontSize = 9.sp)
                }
                TextButton(enabled = origin.isNotBlank() && current != StoredPermissionDecision.DENY, onClick = { onSetSitePermission(origin, resource, StoredPermissionDecision.DENY) }) {
                    Text("Block", fontSize = 9.sp)
                }
                TextButton(enabled = origin.isNotBlank() && current != null, onClick = { onResetSitePermission(origin, resource) }) {
                    Text("Ask", fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Ask lets Bluebird prompt you again the next time the site requests access.", fontSize = 9.sp, color = muted, lineHeight = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text("Site data", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
        Spacer(Modifier.height(4.dp))
        Text("Clear this site's WebView storage. Cookies are controlled by the browser's global cookie setting.", fontSize = 9.sp, color = muted, lineHeight = 13.sp)
        Spacer(Modifier.height(4.dp))
        TextButton(enabled = origin.isNotBlank(), onClick = { if (origin.isNotBlank()) onClearSiteData(origin) }) {
            Text("Clear site data", fontSize = 10.sp, color = Color(0xFF1A73E8))
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD), thickness = .5.dp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FluentIcon(FluentIcons.LockClosed, null, tint = Color(0xFF107C10), modifier = Modifier.size(16.dp))
            Text("Connection security", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                currentUrl.startsWith("https://", true) -> "HTTPS connection"
                currentUrl.startsWith("http://", true) -> "Not secure — HTTP connection"
                else -> "Browser or local page"
            },
            fontSize = 9.sp, color = muted
        )
        Spacer(Modifier.height(8.dp))
        Text("Bluebird asks before granting sensitive permissions unless you have explicitly allowed them in browser settings or remembered a site decision.", fontSize = 9.sp, color = muted, lineHeight = 13.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Password manager
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PasswordManagerPanel(
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    credentials: MutableList<StoredCredential>,
    onSave: (StoredCredential) -> Unit,
    onDelete: (StoredCredential) -> Unit,
    onOpenSite: (String) -> Unit,
    onRequestAuth: () -> Unit,
    authGranted: Boolean
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<StoredCredential?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var revealedIds by remember { mutableStateOf(emptySet<String>()) }
    val filtered = credentials.filter { query.isBlank() || it.origin.contains(query, true) || it.username.contains(query, true) || it.nickname.contains(query, true) }

    Column(Modifier.fillMaxSize()) {
        PanelSearchField(query, { query = it }, "Search passwords…", textColor, accentBlue)
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showAdd = true }) {
                FluentIcon(FluentIcons.Add, null, tint = accentBlue, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add password", fontSize = 10.sp, color = accentBlue)
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(FluentIcons.LockClosed, if (credentials.isEmpty()) "No saved passwords" else "No matching passwords", textColor)
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.id }) { credential ->
                    val revealed = credential.id in revealedIds && authGranted
                    Card(colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF8F8F8)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FluentIcon(FluentIcons.LockClosed, null, tint = accentBlue, modifier = Modifier.size(16.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Text(credential.nickname.ifBlank { credential.origin }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(credential.username, fontSize = 9.sp, color = textColor.copy(.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { onOpenSite(credential.origin) }) { Text("Open", fontSize = 9.sp, color = accentBlue) }
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (revealed) credential.password else "••••••••••••", fontSize = 10.sp, color = textColor, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    if (!authGranted) onRequestAuth() else revealedIds = if (revealed) revealedIds - credential.id else revealedIds + credential.id
                                }) { Text(if (revealed) "Hide" else "Reveal", fontSize = 9.sp, color = accentBlue) }
                                TextButton(onClick = {
                                    if (!authGranted) { onRequestAuth() } else {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Bluebird password", credential.password))
                                    }
                                }) { Text("Copy", fontSize = 9.sp, color = accentBlue) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = { editing = credential }) { Text("Edit", fontSize = 9.sp, color = textColor.copy(.65f)) }
                                TextButton(onClick = { onDelete(credential) }) { Text("Delete", fontSize = 9.sp, color = Color(0xFFD13438)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CredentialEditorDialog(isDark, textColor, accentBlue, null, onDismiss = { showAdd = false }, onSave = { onSave(it); showAdd = false })
    }
    editing?.let { current ->
        CredentialEditorDialog(isDark, textColor, accentBlue, current, onDismiss = { editing = null }, onSave = { onSave(it); editing = null })
    }
}

@Composable
private fun CredentialEditorDialog(
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    initial: StoredCredential?,
    onDismiss: () -> Unit,
    onSave: (StoredCredential) -> Unit
) {
    val bg = if (isDark) Color(0xFF2C2C2C) else Color.White
    var origin by remember { mutableStateOf(initial?.origin ?: "https://") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var nickname by remember { mutableStateOf(initial?.nickname ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bg,
        title = { Text(if (initial == null) "Add password" else "Edit password", color = textColor, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(origin, { origin = it }, singleLine = true, label = { Text("Website") }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor))
                OutlinedTextField(nickname, { nickname = it }, singleLine = true, label = { Text("Name (optional)") }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor))
                OutlinedTextField(username, { username = it }, singleLine = true, label = { Text("Username") }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor))
                OutlinedTextField(password, { password = it }, singleLine = true, label = { Text("Password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor))
            }
        },
        confirmButton = {
            TextButton(enabled = origin.startsWith("http") && username.isNotBlank() && password.isNotBlank(), onClick = {
                val now = System.currentTimeMillis()
                onSave(StoredCredential(initial?.id ?: UUID.randomUUID().toString(), origin.trimEnd('/'), username, password, nickname, initial?.createdAt ?: now, now))
            }) { Text("Save", color = accentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textColor.copy(.6f)) } }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Bookmarks Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ChatGptPanel(
    isDark: Boolean,
    pageTitle: String,
    pageUrl: String,
    selectedText: String,
    preparedPrompt: String,
    onAskAboutPage: (String) -> Unit,
    onAskAboutSelection: () -> Unit,
    onCopyPrompt: (String) -> Unit,
    onOpenChatGpt: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val muted = textColor.copy(.6f)
    val accent = Color(0xFF1A73E8)
    val bg = if (isDark) Color(0xFF202020) else Color(0xFFF7F7F7)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(bg).padding(10.dp)) {
            Text("Ask ChatGPT", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Spacer(Modifier.height(3.dp))
            Text("Bluebird shares page context only after you explicitly request it.", fontSize = 9.sp, color = muted)
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF303030) else Color.White)) {
                Column(Modifier.padding(9.dp)) {
                    Text(pageTitle.ifBlank { "Current page" }, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(pageUrl, fontSize = 8.sp, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (selectedText.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text("Selected text ready", fontSize = 9.sp, color = accent, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onAskAboutPage("ask") }) { Text("Ask", fontSize = 10.sp, color = accent) }
                TextButton(onClick = { onAskAboutPage("summarize") }) { Text("Summarize", fontSize = 10.sp, color = accent) }
                TextButton(onClick = { onAskAboutPage("explain") }) { Text("Explain", fontSize = 10.sp, color = accent) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onAskAboutPage("translate") }) { Text("Translate", fontSize = 10.sp, color = accent) }
                if (selectedText.isNotBlank()) {
                    TextButton(onClick = onAskAboutSelection) { Text("Use selection", fontSize = 10.sp, color = accent) }
                }
            }
            if (preparedPrompt.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Context prepared", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(Modifier.height(3.dp))
                Text(preparedPrompt, fontSize = 8.sp, color = muted, maxLines = 6, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onCopyPrompt(preparedPrompt) }) { Text("Copy prompt", fontSize = 10.sp, color = accent) }
                    TextButton(onClick = onOpenChatGpt) { Text("Open ChatGPT", fontSize = 10.sp, color = accent) }
                }
            }
            Text("ChatGPT cannot be given the page silently. You choose what context to copy and use.", fontSize = 8.sp, color = muted)
        }
        Divider(color = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD), thickness = 0.5.dp)
        AndroidView(
            factory = { ctx -> WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                webViewClient = WebViewClient()
                loadUrl("https://chatgpt.com/")
            }},
            update = { it.onResume() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BookmarksPanel(
    bookmarks: MutableList<Bookmark>,
    folders: MutableList<BookmarkFolder>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onNavigate: (String) -> Unit,
    onBookmarksChanged: () -> Unit
) {
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedFolder by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Bookmarks Bar") }
    var showNewFolder by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newFolderName by androidx.compose.runtime.mutableStateOf("")
    val folderNames = listOf("Bookmarks Bar") + folders.map { it.name }.distinct()
    val filtered = bookmarks.filter { bm ->
        bm.folder == selectedFolder && (query.isBlank() || bm.title.contains(query, true) || bm.url.contains(query, true))
    }
    Column(Modifier.fillMaxSize()) {
        PanelSearchField(query, { query = it }, "Search bookmarks…", textColor, accentBlue)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(folderNames, key = { it }) { name ->
                    TextButton(onClick = { selectedFolder = name }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                        Text(name, fontSize = 9.sp, fontWeight = if (selectedFolder == name) FontWeight.Bold else FontWeight.Normal, color = if (selectedFolder == name) accentBlue else textColor.copy(.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            IconButton(onClick = { showNewFolder = true }, modifier = Modifier.size(28.dp)) { FluentIcon(FluentIcons.Add, null, tint = accentBlue, modifier = Modifier.size(15.dp)) }
        }
        if (showNewFolder) {
            Card(Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, singleLine = true, placeholder = { Text("Folder name", fontSize = 10.sp) }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textColor), modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (newFolderName.trim().isNotEmpty() && folderNames.none { it.equals(newFolderName.trim(), true) }) { folders.add(BookmarkFolder(name = newFolderName.trim())); selectedFolder = newFolderName.trim(); newFolderName = ""; showNewFolder = false; onBookmarksChanged() } }) { Text("Create", fontSize = 10.sp, color = accentBlue) }
                }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(FluentIcons.BookmarkBorder, if (bookmarks.isEmpty()) "No bookmarks yet" else "No bookmarks in $selectedFolder", textColor)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(filtered, key = { it.id }) { bm ->
                BookmarkRow(bm, textColor, { onNavigate(bm.url) }, { bookmarks.remove(bm); onBookmarksChanged() })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// History Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun HistoryPanel(
    history: MutableList<HistoryEntry>, isDark: Boolean, textColor: Color, accentBlue: Color,
    onNavigate: (String) -> Unit, onClear: () -> Unit, onHistoryChanged: () -> Unit = {}
) {
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val filtered = history.filter { query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        PanelSearchField(query, { query = it }, "Search history…", textColor, accentBlue)
        if (history.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("${filtered.size} of ${history.size} entries", fontSize = 10.sp, color = textColor.copy(.4f))
                TextButton(onClick = { onClear(); onHistoryChanged() }, contentPadding = PaddingValues(4.dp)) { Text("Clear all", fontSize = 10.sp, color = Color(0xFFD32F2F)) }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(FluentIcons.History, if (history.isEmpty()) "No history" else "No matching history", textColor)
            return@Column
        }
        val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
        LazyColumn(Modifier.fillMaxSize(), PaddingValues(8.dp), Arrangement.spacedBy(2.dp)) {
            items(filtered, key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onNavigate(entry.url) }.padding(horizontal = 8.dp, vertical = 5.dp), Alignment.CenterVertically, Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(18.dp).background(Color(entry.faviconColor), CircleShape), Alignment.Center) { Text(entry.title.firstOrNull()?.toString()?.uppercase() ?: "?", fontSize = 8.sp, color = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, fontSize = 10.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.url, fontSize = 8.sp, color = textColor.copy(.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sdf.format(Date(entry.visitedAt)), fontSize = 8.sp, color = textColor.copy(.35f))
                    }
                    IconButton(onClick = { history.remove(entry); onHistoryChanged() }, modifier = Modifier.size(24.dp)) { FluentIcon(FluentIcons.Close, null, tint = textColor.copy(.25f), modifier = Modifier.size(10.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Downloads Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DownloadsPanel(
    downloads: MutableList<DownloadItem>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onOpen: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    onRetry: (DownloadItem) -> Unit,
    onShowDownloads: () -> Unit,
    onClearCompleted: () -> Unit
) {
    if (downloads.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            EmptyState(FluentIcons.Download, "No downloads", textColor)
            TextButton(onClick = onShowDownloads) { Text("Open Downloads folder", color = accentBlue, fontSize = 11.sp) }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClearCompleted) { Text("Clear completed", fontSize = 10.sp, color = accentBlue) }
            TextButton(onClick = onShowDownloads) { Text("Open folder", fontSize = 10.sp, color = accentBlue) }
        }
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(downloads, key = { it.id }) { dl ->
            val cardBg = if (isDark) Color(0xFF333333) else Color(0xFFF5F5F5)
            Card(
                shape  = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FluentIcon(
                            when (dl.status) {
                                DownloadStatus.COMPLETED  -> FluentIcons.CheckCircle
                                DownloadStatus.FAILED     -> FluentIcons.Error
                                DownloadStatus.PAUSED     -> FluentIcons.Pause
                                DownloadStatus.CANCELLED  -> FluentIcons.Close
                                DownloadStatus.DOWNLOADING-> FluentIcons.Downloading
                            },
                            null,
                            tint = when (dl.status) {
                                DownloadStatus.COMPLETED  -> Color(0xFF107C10)
                                DownloadStatus.FAILED     -> Color(0xFFD13438)
                                else                      -> accentBlue
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            dl.fileName,
                            fontSize = 11.sp,
                            color    = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).clickable(enabled = dl.status == DownloadStatus.COMPLETED) { onOpen(dl) }
                        )
                        IconButton(onClick = { onRemove(dl) }, modifier = Modifier.size(24.dp)) {
                            FluentIcon(FluentIcons.Delete, null, tint = textColor.copy(.35f), modifier = Modifier.size(13.dp))
                        }
                    }

                    if (dl.status == DownloadStatus.DOWNLOADING) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress    = { dl.progress },
                            modifier    = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color       = accentBlue,
                            trackColor  = if (isDark) Color(0xFF555555) else Color(0xFFDDDDDD)
                        )
                    }

                    Spacer(Modifier.height(3.dp))
                    val sizeLabel = if (dl.fileSize > 0)
                        "${dl.fileSize / 1024 / 1024} MB · "
                    else ""
                    Text(
                        "$sizeLabel${dl.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 9.sp,
                        color    = textColor.copy(0.5f)
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (dl.status) {
                            DownloadStatus.COMPLETED -> TextButton(onClick = { onOpen(dl) }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Open", fontSize = 9.sp, color = accentBlue) }
                            DownloadStatus.FAILED, DownloadStatus.PAUSED, DownloadStatus.CANCELLED -> TextButton(onClick = { onRetry(dl) }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Retry", fontSize = 9.sp, color = accentBlue) }
                            DownloadStatus.DOWNLOADING -> TextButton(onClick = { onCancel(dl) }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Cancel", fontSize = 9.sp, color = Color(0xFFD13438)) }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Settings Panel
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SettingsPanel(
    settings: BrowserSettings,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onChange: (BrowserSettings) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

        item { SectionHeader("General", accentBlue) }
        item { ToggleRow("Show bookmarks bar",    null, settings.showBookmarksBar, textColor, accentBlue) { onChange(settings.copy(showBookmarksBar = it)) } }
        item { ToggleRow("Desktop mode",          "Request desktop versions of websites", settings.desktopMode, textColor, accentBlue) { onChange(settings.copy(desktopMode = it)) } }
        item { ToggleRow("JavaScript",            null, settings.javaScriptEnabled, textColor, accentBlue) { onChange(settings.copy(javaScriptEnabled = it)) } }
        item { ToggleRow("Load images",           null, settings.showImages, textColor, accentBlue) { onChange(settings.copy(showImages = it)) } }
        item { ToggleRow("Save cookies",          null, settings.saveCookies, textColor, accentBlue) { onChange(settings.copy(saveCookies = it)) } }
        item { ToggleRow("Save form data",        null, settings.saveFormData, textColor, accentBlue) { onChange(settings.copy(saveFormData = it)) } }
        item { SectionHeader("Passwords & autofill", accentBlue) }
        item { ToggleRow("Offer to save passwords", "Ask before storing website credentials", settings.offerToSavePasswords, textColor, accentBlue) { onChange(settings.copy(offerToSavePasswords = it)) } }
        item { ToggleRow("Autofill passwords", "Allow saved credentials to be used for forms", settings.autofillPasswords, textColor, accentBlue) { onChange(settings.copy(autofillPasswords = it)) } }
        item { ToggleRow("Require device authentication", "Protect password reveal/copy with your device lock", settings.requireDeviceAuthForPasswords, textColor, accentBlue) { onChange(settings.copy(requireDeviceAuthForPasswords = it)) } }
        item { ToggleRow("Allow mixed content",   "Load HTTP on HTTPS pages (insecure)", settings.mixedContentAllowed, textColor, accentBlue) { onChange(settings.copy(mixedContentAllowed = it)) } }

        item { SectionHeader("Privacy & Security", accentBlue) }
        item { ToggleRow("Ad blocker",           "Block intrusive advertisements", settings.adBlockEnabled, textColor, accentBlue) { onChange(settings.copy(adBlockEnabled = it)) } }
        item { ToggleRow("Tracking protection",  "Block cross-site trackers", settings.trackingProtection, textColor, accentBlue) { onChange(settings.copy(trackingProtection = it)) } }
        item { ToggleRow("Block pop-ups",        null, settings.popupBlocker, textColor, accentBlue) { onChange(settings.copy(popupBlocker = it)) } }

        item { SectionHeader("Search Engine", accentBlue) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                SearchEngine.values().forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onChange(settings.copy(searchEngine = engine)) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.searchEngine == engine,
                            onClick  = { onChange(settings.copy(searchEngine = engine)) },
                            modifier = Modifier.size(16.dp),
                            colors   = RadioButtonDefaults.colors(selectedColor = accentBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, fontSize = 12.sp, color = textColor)
                    }
                }
            }
        }

        item { SectionHeader("Keyboard", accentBlue) }

        item { SectionHeader("Permissions", accentBlue) }
        item { ToggleRow("Location access",   "Allow sites to request location",   settings.locationAccess,   textColor, accentBlue) { onChange(settings.copy(locationAccess = it)) } }
        item { ToggleRow("Camera access",     "Allow sites to request camera",     settings.cameraAccess,     textColor, accentBlue) { onChange(settings.copy(cameraAccess = it)) } }
        item { ToggleRow("Microphone access", "Allow sites to request microphone", settings.microphoneAccess, textColor, accentBlue) { onChange(settings.copy(microphoneAccess = it)) } }

        item { SectionHeader("New tab", accentBlue) }
        item { ToggleRow("Quick links", "Show your favorite shortcuts on the New Tab page", settings.homeShowQuickLinks, textColor, accentBlue) { onChange(settings.copy(homeShowQuickLinks = it)) } }
        item { ToggleRow("Recent sites", "Show recently visited sites", settings.homeShowRecentSites, textColor, accentBlue) { onChange(settings.copy(homeShowRecentSites = it)) } }
        item { ToggleRow("Bookmarks", "Show your bookmarks on the New Tab page", settings.homeShowBookmarks, textColor, accentBlue) { onChange(settings.copy(homeShowBookmarks = it)) } }
        item { ToggleRow("Subscribed news", "Show cached articles from your feeds", settings.homeShowNews, textColor, accentBlue) { onChange(settings.copy(homeShowNews = it)) } }
        item { ToggleRow("Wallpaper", "Show the cached wallpaper; rotates every 5 hours", settings.homeShowWallpaper, textColor, accentBlue) { onChange(settings.copy(homeShowWallpaper = it)) } }

        item { SectionHeader("Start Page", accentBlue) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                StartPage.values().forEach { sp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onChange(settings.copy(startPage = sp)) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.startPage == sp,
                            onClick  = { onChange(settings.copy(startPage = sp)) },
                            modifier = Modifier.size(16.dp),
                            colors   = RadioButtonDefaults.colors(selectedColor = accentBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (sp) {
                                StartPage.NEW_TAB                -> "New tab page"
                                StartPage.BLANK                  -> "Blank page"
                                StartPage.CONTINUE_WHERE_LEFT_OFF-> "Continue where left off"
                            },
                            fontSize = 12.sp,
                            color    = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentBlue: Color) {
    Text(
        title,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Bold,
        color      = accentBlue,
        modifier   = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 3.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    textColor: Color,
    accentBlue: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 9.sp, color = textColor.copy(0.45f), lineHeight = 12.sp)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            modifier        = Modifier.scale(0.65f),
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = accentBlue,
                uncheckedTrackColor = Color(0xFF888888)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Context Menu (three-dot)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EdgeContextMenu(
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    isBookmarked: Boolean,
    isPrivateTab: Boolean,
    currentZoom: Int,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onChatGpt: () -> Unit,
    onPasswords: () -> Unit,
    onFillPassword: () -> Unit,
    onSiteSettings: () -> Unit,
    onFindInPage: () -> Unit,
    onPrint: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onAddBookmark: () -> Unit,
    onShare: () -> Unit,
    onClearData: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.3f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            modifier  = Modifier.width(220.dp).padding(top = 80.dp, end = 8.dp)
                .clickable(enabled = false) {},
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MenuRow(FluentIcons.Add,       "New tab",          textColor) { onNewTab(); onDismiss() }
                MenuRow(FluentIcons.Security,  "New private tab",  textColor) { onNewPrivateTab(); onDismiss() }

                MenuDivider(isDark)

                MenuRow(
                    if (isBookmarked) FluentIcons.Star else FluentIcons.StarBorder,
                    if (isBookmarked) "Bookmarked ✓" else "Add bookmark",
                    textColor
                ) { onAddBookmark(); onDismiss() }
                MenuRow(FluentIcons.Share,       "Share page",     textColor) { onShare(); onDismiss() }
                MenuRow(FluentIcons.FindInPage,  "Find in page",   textColor) { onFindInPage(); onDismiss() }
                MenuRow(FluentIcons.Print,       "Print…",         textColor) { onPrint(); onDismiss() }

                MenuDivider(isDark)

                // Zoom row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FluentIcon(FluentIcons.ZoomIn, null, tint = textColor.copy(0.6f), modifier = Modifier.size(14.dp))
                    Text("Zoom", fontSize = 12.sp, color = textColor, modifier = Modifier.padding(start = 8.dp))
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) {
                        FluentIcon(FluentIcons.Remove, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "$currentZoom%",
                        fontSize = 11.sp,
                        color = textColor.copy(0.8f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onZoomReset() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) {
                        FluentIcon(FluentIcons.Add, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                }

                MenuDivider(isDark)

                MenuRow(FluentIcons.BookmarkBorder, "Bookmarks", textColor) { onBookmarks(); onDismiss() }
                MenuRow(FluentIcons.History,        "History",   textColor) { onHistory(); onDismiss() }
                MenuRow(FluentIcons.Download,       "Downloads", textColor) { onDownloads(); onDismiss() }
                MenuRow(FluentIcons.Delete,         "Clear browsing data", textColor) { onClearData(); onDismiss() }

                MenuDivider(isDark)

                MenuRow(FluentIcons.Sparkle, "ChatGPT assistant", textColor) { onChatGpt(); onDismiss() }
                MenuRow(FluentIcons.LockClosed, "Fill saved password", textColor) { onFillPassword(); onDismiss() }
                MenuRow(FluentIcons.LockClosed, "Passwords", textColor) { onPasswords(); onDismiss() }
                MenuRow(FluentIcons.Security, "Site settings", textColor) { onSiteSettings(); onDismiss() }
                MenuRow(FluentIcons.Settings, "Settings", textColor) { onSettings(); onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: String, label: String, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FluentIcon(icon, null, tint = textColor.copy(0.65f), modifier = Modifier.size(15.dp))
        Text(label, fontSize = 12.sp, color = textColor)
    }
}

@Composable
private fun MenuDivider(isDark: Boolean) {
    Divider(
        color     = if (isDark) Color(0x20FFFFFF) else Color(0x20000000),
        thickness = 0.5.dp,
        modifier  = Modifier.padding(vertical = 3.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Tab Overview Grid
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun TabOverviewGrid(
    tabs: List<BrowserTab>,
    activeTabId: String,
    tabThumbnails: Map<String, android.graphics.Bitmap> = emptyMap(),
    tabFavicons: Map<String, android.graphics.Bitmap> = emptyMap(),
    tabGroups: List<TabGroup> = emptyList(),
    isDark: Boolean,
    onSelectTab: (BrowserTab) -> Unit,
    onCloseTab: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onAssignTabToGroup: (tabId: String, groupId: String?) -> Unit = { _, _ -> },
    onCreateGroupForTab: (tabId: String) -> Unit = {}
) {
    val bg        = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val cardBg    = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF1A73E8)
    val privateColor = Color(0xFF9C6DCA)
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    val filteredTabs = tabs.filter { tab ->
        val matchesText = searchQuery.isBlank() ||
            tab.title.contains(searchQuery, true) || tab.url.contains(searchQuery, true)
        val matchesGroup = selectedGroupId == null || tab.groupId == selectedGroupId
        matchesText && matchesGroup
    }

    // Separate normal and private tabs
    val normalTabs  = tabs.filter { !it.isPrivate }
    val privateTabs = tabs.filter { it.isPrivate }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${filteredTabs.size} of ${tabs.size} tabs", fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 14.sp)
                    androidx.compose.material3.TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Search tabs", fontSize = 10.sp, color = textColor.copy(0.4f)) },
                        leadingIcon = { FluentIcon(FluentIcons.Search, null, tint = textColor.copy(0.5f), modifier = Modifier.size(13.dp)) },
                        modifier = Modifier.fillMaxWidth().height(34.dp).padding(top = 3.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textColor),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = cardBg, unfocusedContainerColor = cardBg,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onNewPrivateTab) {
                        FluentIcon(FluentIcons.Security, null, tint = privateColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Private", fontSize = 11.sp, color = privateColor)
                    }
                    TextButton(onClick = onNewTab) {
                        FluentIcon(FluentIcons.Add, null, tint = accentBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New tab", fontSize = 11.sp, color = accentBlue)
                    }
                }
            }

            if (tabGroups.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = selectedGroupId == null,
                            onClick = { selectedGroupId = null },
                            label = { Text("All", fontSize = 10.sp) }
                        )
                    }
                    items(tabGroups, key = { it.id }) { group ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = { selectedGroupId = if (selectedGroupId == group.id) null else group.id },
                            label = { Text(group.name, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns               = GridCells.Adaptive(140.dp),
                contentPadding        = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.weight(1f)
            ) {
                gridItems(filteredTabs, key = { tab -> tab.id }) { tab ->
                    // Capture all tab properties before entering nested composables
                    val tabId         = tab.id
                    val tabIsPrivate  = tab.isPrivate
                    val tabTitle      = tab.title
                    val tabFavColor   = tab.faviconColor
                    val isActive      = tabId == activeTabId
                    val accent        = if (tabIsPrivate) privateColor else accentBlue

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clickable { onSelectTab(tab) }
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = if (isActive) accent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape     = RoundedCornerShape(8.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = if (tabIsPrivate) Color(0xFF1E1430) else cardBg
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        if (tabIsPrivate) Color(0xFF2D1F3D)
                                        else Color(tabFavColor).copy(0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val preview = tabThumbnails[tabId]
                                if (preview != null && !preview.isRecycled && !tabIsPrivate) {
                                    androidx.compose.foundation.Image(
                                        bitmap = preview.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (tabIsPrivate) {
                                    FluentIcon(
                                        FluentIcons.Security,
                                        contentDescription = null,
                                        tint     = privateColor.copy(0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    val favicon = tabFavicons[tabId]
                                    if (favicon != null && !favicon.isRecycled) {
                                        androidx.compose.foundation.Image(
                                            bitmap = favicon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    } else {
                                        FluentIcon(
                                        FluentIcons.Language,
                                        contentDescription = null,
                                        tint     = Color(tabFavColor).copy(0.5f),
                                        modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val footerFavicon = tabFavicons[tabId]
                                if (!tabIsPrivate && footerFavicon != null && !footerFavicon.isRecycled) {
                                    androidx.compose.foundation.Image(
                                        bitmap = footerFavicon.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (tabIsPrivate) privateColor else Color(tabFavColor),
                                                CircleShape
                                            )
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text     = tabTitle,
                                    fontSize = 9.sp,
                                    color    = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseTab(tab) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    FluentIcon(
                                        FluentIcons.Close,
                                        contentDescription = null,
                                        tint     = textColor.copy(0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Address Suggestions
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AddressSuggestionsDropdown(
    query: String,
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    searchEngine: SearchEngine,
    isDark: Boolean,
    onSuggestionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bg = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val muted = textColor.copy(alpha = 0.52f)
    val accentBlue = Color(0xFF1A73E8)
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return

    val urlLike = com.io.github.norbertweb.bluebird.browser.utils.UrlUtils.looksLikeUrl(cleanQuery) ||
        cleanQuery.startsWith("http://", ignoreCase = true) ||
        cleanQuery.startsWith("https://", ignoreCase = true)

    // Local-only suggestions keep the omnibox useful without sending every
    // keystroke to a remote suggestion service. This is intentionally data-saving.
    val bookmarkMatches = bookmarks
        .asSequence()
        .filter { it.url.contains(cleanQuery, true) || it.title.contains(cleanQuery, true) }
        .distinctBy { it.url }
        .take(4)
        .toList()

    val historyMatches = history
        .asSequence()
        .filter { it.url.contains(cleanQuery, true) || it.title.contains(cleanQuery, true) }
        .filterNot { entry -> bookmarkMatches.any { it.url == entry.url } }
        .distinctBy { it.url }
        .take(5)
        .toList()

    Box(modifier = Modifier.fillMaxWidth().clickable { onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                if (!urlLike) {
                    SuggestionRow(
                        icon = FluentIcons.Search,
                        title = "Search with ${searchEngine.label}",
                        url = cleanQuery,
                        color = accentBlue,
                        textColor = textColor,
                        onClick = { onSuggestionClick(cleanQuery) }
                    )
                } else {
                    SuggestionRow(
                        icon = FluentIcons.Language,
                        title = "Go to ${cleanQuery}",
                        url = "https://${cleanQuery.removePrefix("https://").removePrefix("http://")}",
                        color = accentBlue,
                        textColor = textColor,
                        onClick = { onSuggestionClick(cleanQuery) }
                    )
                }

                if (bookmarkMatches.isNotEmpty()) {
                    Text("Bookmarks", fontSize = 9.sp, color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 7.dp, bottom = 2.dp))
                    bookmarkMatches.forEach { bm ->
                        SuggestionRow(
                            icon = FluentIcons.BookmarkBorder,
                            title = bm.title,
                            url = bm.url,
                            color = accentBlue,
                            textColor = textColor,
                            onClick = { onSuggestionClick(bm.url) }
                        )
                    }
                }

                if (historyMatches.isNotEmpty()) {
                    Text("History", fontSize = 9.sp, color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 7.dp, bottom = 2.dp))
                    historyMatches.forEach { entry ->
                        SuggestionRow(
                            icon = FluentIcons.History,
                            title = entry.title,
                            url = entry.url,
                            color = muted,
                            textColor = textColor,
                            onClick = { onSuggestionClick(entry.url) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SuggestionRow(
    icon: Int,
    title: String,
    url: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FluentIcon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url, fontSize = 9.sp, color = textColor.copy(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Empty state helper
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(icon: String, label: String, textColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FluentIcon(icon, null, tint = textColor.copy(0.25f), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, color = textColor.copy(0.4f), fontSize = 12.sp)
        }
    }
}



// ═══════════════════════════════════════════════════════════════════════
// Tab context menu — desktop-browser actions on long press / right-click
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EdgeTabContextMenu(
    tab: BrowserTab,
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    canCloseOthers: Boolean,
    canReopenClosed: Boolean,
    tabGroups: List<TabGroup> = emptyList(),
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDuplicate: () -> Unit,
    onCloseOthers: () -> Unit,
    onAssignGroup: (String) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onRemoveFromGroup: () -> Unit = {},
    onReopenClosed: () -> Unit,
    onClose: () -> Unit
) {
    val secondary = textColor.copy(alpha = 0.68f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable { onSelect() },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .width(240.dp)
                .padding(top = 44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = tab.title.ifBlank { "New Tab" },
                    color = secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
                MenuDivider(isDark)
                MenuRow(FluentIcons.Article, "Switch to tab", textColor) { onSelect() }
                MenuRow(
                    if (tab.isPinned) FluentIcons.PushPin else FluentIcons.PushPin,
                    if (tab.isPinned) "Unpin tab" else "Pin tab",
                    textColor
                ) { onTogglePin() }
                MenuRow(FluentIcons.Article, "Duplicate tab", textColor) { onDuplicate() }
                if (canCloseOthers) {
                    MenuRow(FluentIcons.Close, "Close other tabs", textColor) { onCloseOthers() }
                }
                if (canReopenClosed) {
                    MenuRow(FluentIcons.History, "Reopen closed tab", textColor) { onReopenClosed() }
                }
                if (tabGroups.isNotEmpty()) {
                    MenuDivider(isDark)
                    Text(
                        "Tab groups",
                        color = secondary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                    tabGroups.take(6).forEach { group ->
                        MenuRow(FluentIcons.BookmarkBorder, group.name, textColor) { onAssignGroup(group.id) }
                    }
                }
                if (tab.groupId != null) {
                    MenuRow(FluentIcons.BookmarkBorder, "Remove from group", textColor) { onRemoveFromGroup() }
                }
                MenuRow(FluentIcons.Add, "Create new tab group", textColor) { onCreateGroup() }
                MenuDivider(isDark)
                MenuRow(FluentIcons.Close, "Close tab", textColor) { onClose() }
            }
        }
    }}}
