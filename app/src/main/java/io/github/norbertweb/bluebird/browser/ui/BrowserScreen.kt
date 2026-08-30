package com.io.github.norbertweb.bluebird.browser.ui

// Use model alias to avoid clashing with android.webkit.PermissionRequest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.print.PrintManager
import android.webkit.GeolocationPermissions
import android.webkit.WebView
import android.app.KeyguardManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.io.github.norbertweb.bluebird.browser.data.BrowserRepository
import com.io.github.norbertweb.bluebird.browser.security.CredentialVault
import com.io.github.norbertweb.bluebird.browser.security.StoredCredential
import com.io.github.norbertweb.bluebird.browser.model.Bookmark
import com.io.github.norbertweb.bluebird.browser.model.BookmarkFolder
import com.io.github.norbertweb.bluebird.browser.model.BrowserPanel
import com.io.github.norbertweb.bluebird.browser.model.BrowserSettings
import com.io.github.norbertweb.bluebird.browser.model.BrowserTab
import com.io.github.norbertweb.bluebird.browser.model.DownloadItem
import com.io.github.norbertweb.bluebird.browser.model.HistoryEntry
import com.io.github.norbertweb.bluebird.browser.model.JsDialogState
import com.io.github.norbertweb.bluebird.browser.model.MAX_HISTORY_ENTRIES
import com.io.github.norbertweb.bluebird.browser.model.MAX_TABS
import com.io.github.norbertweb.bluebird.browser.model.NEWTAB_URL
import com.io.github.norbertweb.bluebird.browser.model.ClearDataOption
import com.io.github.norbertweb.bluebird.browser.model.SslDialogState
import com.io.github.norbertweb.bluebird.browser.model.TabGroup
import com.io.github.norbertweb.bluebird.browser.model.StoredPermissionDecision
import com.io.github.norbertweb.bluebird.browser.ui.components.BookmarksBar
import com.io.github.norbertweb.bluebird.browser.ui.components.EdgeNavigationBar
import com.io.github.norbertweb.bluebird.browser.ui.components.EdgeTabBar
import com.io.github.norbertweb.bluebird.browser.ui.components.FindInPageBar
import com.io.github.norbertweb.bluebird.browser.ui.components.GeolocationDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.JsDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.ClearBrowsingDataDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.PermissionRequestDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.SavePasswordDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.SslWarningDialog
import com.io.github.norbertweb.bluebird.browser.ui.newtab.NewTabPage
import com.io.github.norbertweb.bluebird.browser.ui.panels.AddressSuggestionsDropdown
import com.io.github.norbertweb.bluebird.browser.ui.panels.EdgeContextMenu
import com.io.github.norbertweb.bluebird.browser.ui.panels.SidePanel
import com.io.github.norbertweb.bluebird.browser.ui.panels.TabOverviewGrid
import com.io.github.norbertweb.bluebird.browser.ui.webview.BrowserWebView
import com.io.github.norbertweb.bluebird.browser.ui.webview.captureCredentialFromCurrentForm
import com.io.github.norbertweb.bluebird.browser.ui.webview.fillCredentialIntoCurrentForm
import com.io.github.norbertweb.bluebird.browser.utils.DownloadHelper
import com.io.github.norbertweb.bluebird.browser.utils.UrlUtils
import com.io.github.norbertweb.bluebird.browser.utils.onMain
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.io.github.norbertweb.bluebird.browser.model.PermissionRequest as BrowserPermissionRequest

// ═══════════════════════════════════════════════════════════════════════
// BrowserScreen — root composable
// All state is initialized from the repository (persisted) and saved
// back on every meaningful change via a debounced coroutine.
// ═══════════════════════════════════════════════════════════════════════

private data class BrowserPersistedState(
    val tabs: Pair<List<BrowserTab>, String>,
    val settings: BrowserSettings,
    val bookmarks: List<Bookmark>,
    val history: List<HistoryEntry>,
    val downloads: List<DownloadItem>,
    val credentials: List<StoredCredential>,
    val tabGroups: List<TabGroup>
)

@Composable
fun BrowserScreen() {
    val context    = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val repo       = remember { BrowserRepository.get(context) }
    val homeContentRepo = remember { com.io.github.norbertweb.bluebird.browser.data.HomeContentRepository.get(context) }
    val credentialVault = remember { CredentialVault(context) }
    val scope      = rememberCoroutineScope()
    val dlHelper   = remember { DownloadHelper(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Persistence: lazy, off-main initialization ────────────────────
    // JSON parsing of history/tabs is deferred until after the first frame.
    var browserInitialized by remember { mutableStateOf(false) }
    val tabs = remember {
        androidx.compose.runtime.snapshots.SnapshotStateList<BrowserTab>().also { it.add(BrowserTab()) }
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var settings by remember { mutableStateOf(BrowserSettings()) }
    val bookmarks = remember { androidx.compose.runtime.snapshots.SnapshotStateList<Bookmark>() }
    val bookmarkFolders = remember { androidx.compose.runtime.snapshots.SnapshotStateList<BookmarkFolder>() }
    val history = remember { androidx.compose.runtime.snapshots.SnapshotStateList<HistoryEntry>() }

    val downloads = remember { androidx.compose.runtime.snapshots.SnapshotStateList<DownloadItem>() }
    val credentials = remember { androidx.compose.runtime.snapshots.SnapshotStateList<StoredCredential>() }
    val tabGroups = remember { androidx.compose.runtime.snapshots.SnapshotStateList<TabGroup>() }

    LaunchedEffect(repo) {
        val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) {
            BrowserPersistedState(
                tabs = repo.loadTabs(),
                settings = repo.loadSettings(),
                bookmarks = repo.loadBookmarks(),
                history = repo.loadHistory(),
                downloads = repo.loadDownloads(),
                credentials = credentialVault.load(),
                tabGroups = repo.loadTabGroups()
            )
        }
        if (!isActive) return@LaunchedEffect
        val (savedTabs, savedActiveId) = loaded.tabs
        tabs.clear()
        val restoredTabs = if (savedTabs.isEmpty()) listOf(BrowserTab()) else savedTabs
        tabs.addAll(restoredTabs.sortedWith(compareByDescending<BrowserTab> { it.isPinned }))
        activeTabId = if (savedActiveId.isNotEmpty() && tabs.any { it.id == savedActiveId }) savedActiveId else tabs.first().id
        settings = loaded.settings
        bookmarks.clear(); bookmarks.addAll(loaded.bookmarks)
        bookmarkFolders.clear(); bookmarkFolders.addAll(repo.loadBookmarkFolders())
        history.clear(); history.addAll(loaded.history)
        downloads.clear(); downloads.addAll(dlHelper.reconcile(loaded.downloads))
        credentials.clear(); credentials.addAll(loaded.credentials)
        tabGroups.clear(); tabGroups.addAll(loaded.tabGroups)
        browserInitialized = true
    }

    // ── WebView instance map (tab id → WebView) ───────────────────────
    val webViews = remember { mutableMapOf<String, WebView>() }
    // Lightweight in-memory tab visuals. They are deliberately not persisted:
    // thumbnails are disposable UI cache, not browser data. Keep the cache bounded
    // so tab previews never become a hidden memory sink.
    val tabThumbnails = remember { androidx.compose.runtime.mutableStateMapOf<String, Bitmap>() }
    val tabFavicons = remember { androidx.compose.runtime.mutableStateMapOf<String, Bitmap>() }

    // ── UI state ──────────────────────────────────────────────────────
    var addressText       by remember { mutableStateOf("") }
    var addressBarFocused by remember { mutableStateOf(false) }
    var addressFocusRequestToken by remember { mutableStateOf(0) }
    var showMenu          by remember { mutableStateOf(false) }
    var showTabOverview   by remember { mutableStateOf(false) }
    var tabContextId      by remember { mutableStateOf<String?>(null) }
    var lastClosedTab     by remember { mutableStateOf<BrowserTab?>(null) }
    var activePanel       by remember { mutableStateOf(BrowserPanel.NONE) }
    var findQuery         by remember { mutableStateOf("") }
    var isFindActive      by remember { mutableStateOf(false) }
    var findActiveMatch   by remember { mutableStateOf(0) }
    var findTotalMatches  by remember { mutableStateOf(0) }
    var currentZoom       by remember { mutableStateOf(settings.defaultZoom) }
    var jsDialog          by remember { mutableStateOf<JsDialogState?>(null) }
    var sslDialog         by remember { mutableStateOf<SslDialogState?>(null) }
    var permDialog        by remember { mutableStateOf<BrowserPermissionRequest?>(null) }
    var geoDialog         by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var sitePermissions by remember { mutableStateOf(repo.loadSitePermissions()) }
    val pageErrors = remember { androidx.compose.runtime.mutableStateMapOf<String, Pair<String, String>>() }
    var passwordAuthGranted by remember { mutableStateOf(false) }
    var credentialOffer by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var credentialPickerOrigin by remember { mutableStateOf<String?>(null) }
    var selectedText by remember { mutableStateOf("") }
    var preparedChatPrompt by remember { mutableStateOf("") }
    var showChatContextConsent by remember { mutableStateOf(false) }
    var pendingChatContextMode by remember { mutableStateOf("ask") }
    var pendingCredentialFill by remember { mutableStateOf<StoredCredential?>(null) }
    val deviceAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        passwordAuthGranted = result.resultCode == android.app.Activity.RESULT_OK
        if (passwordAuthGranted && pendingCredentialFill != null) {
            credentialPickerOrigin = pendingCredentialFill!!.origin
            pendingCredentialFill = null
        }
    }
    fun onPasswordAuthenticationRequest() {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!settings.requireDeviceAuthForPasswords || passwordAuthGranted) {
            passwordAuthGranted = true
            return
        }
        if (!km.isDeviceSecure) {
            passwordAuthGranted = true
            return
        }
        val intent = km.createConfirmDeviceCredentialIntent("Unlock saved passwords", "Confirm your device credentials to view or copy a saved password.")
        deviceAuthLauncher.launch(intent)
    }

    // ── Derived ───────────────────────────────────────────────────────
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val isDark    = settings.darkMode

    // ── Colors ────────────────────────────────────────────────────────
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F3F3)
    val navBarBg     = if (isDark) Color(0xFF252525) else Color(0xFFF3F3F3)
    val borderColor  = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD)
    val textColor    = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val mainBg       = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)

    // ═════════════════════════════════════════════════════════════════
    // Debounced persistence save
    // ═════════════════════════════════════════════════════════════════

    var saveJob by remember { mutableStateOf<Job?>(null) }
    fun scheduleSave() {
        if (!browserInitialized) return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(800)
            repo.saveTabs(tabs, activeTabId)
            repo.saveBookmarks(bookmarks)
            repo.saveBookmarkFolders(bookmarkFolders)
            repo.saveTabGroups(tabGroups)
            repo.saveHistory(history)
            repo.saveDownloads(downloads)
            credentialVault.save(credentials)
            repo.saveSettings(settings)
        }
    }

    fun checkpointSession() {
        if (!browserInitialized) return
        tabs.forEach { tab -> if (!tab.isPrivate) tab.checkpointAt = System.currentTimeMillis() }
        repo.checkpointTabs(tabs, activeTabId)
    }

    LaunchedEffect(activeTabId)    { scheduleSave() }
    LaunchedEffect(tabs.size)      { scheduleSave() }
    LaunchedEffect(settings)       { scheduleSave() }
    LaunchedEffect(bookmarks.size) { scheduleSave() }
    LaunchedEffect(history.size)   { scheduleSave() }
    LaunchedEffect(downloads.size)  { scheduleSave() }
    LaunchedEffect(credentials.size) { if (browserInitialized) credentialVault.save(credentials) }
    LaunchedEffect(tabGroups.size) { if (browserInitialized) repo.saveTabGroups(tabGroups) }

    // Crash-safe lifecycle checkpoint. Normal edits stay debounced for performance;
    // leaving the foreground forces the latest lightweight session model to disk.
    DisposableEffect(lifecycleOwner, browserInitialized) {
        if (!browserInitialized) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                passwordAuthGranted = false
                tabs.forEach { tab ->
                    if (!tab.isPrivate) tab.checkpointAt = System.currentTimeMillis()
                }
                repo.checkpointTabs(tabs, activeTabId)
                repo.saveBookmarks(bookmarks)
                repo.saveBookmarkFolders(bookmarkFolders)
                repo.saveTabGroups(tabGroups)
                repo.saveHistory(history)
                repo.saveDownloads(downloads)
                repo.saveSettings(settings)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            tabThumbnails.clear()
            tabFavicons.clear()
            if (!browserInitialized) { dlHelper.destroy(); return@onDispose }
            tabs.forEach { tab -> if (!tab.isPrivate) tab.checkpointAt = System.currentTimeMillis() }
            repo.checkpointTabs(tabs, activeTabId)
            repo.saveBookmarks(bookmarks)
            repo.saveBookmarkFolders(bookmarkFolders)
            repo.saveTabGroups(tabGroups)
            repo.saveHistory(history)
            repo.saveDownloads(downloads)
            credentialVault.save(credentials)
            repo.saveSettings(settings)
            dlHelper.destroy()
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Helper functions
    // ═════════════════════════════════════════════════════════════════

    fun activeWebView() = webViews[activeTabId]

    fun extractSelectedText(onResult: (String) -> Unit) {
        activeWebView()?.evaluateJavascript("(window.getSelection ? window.getSelection().toString() : '')") { raw ->
            val text = runCatching { org.json.JSONTokener(raw ?: "\"\"").nextValue() as? String }.getOrNull().orEmpty().trim()
            selectedText = text.take(8000)
            onResult(selectedText)
        }
    }

    fun buildChatPrompt(mode: String) {
        val wv = activeWebView()
        if (activeTab.url == NEWTAB_URL || wv == null) return
        val selectionMode = mode == "selection"
        val js = if (selectionMode) "(window.getSelection ? window.getSelection().toString() : '')" else "(document.body ? document.body.innerText : '')"
        wv.evaluateJavascript(js) { raw ->
            val extracted = runCatching { org.json.JSONTokener(raw ?: "\"\"").nextValue() as? String }.getOrNull().orEmpty().trim()
            val body = extracted.take(if (selectionMode) 8000 else 12000)
            selectedText = if (selectionMode) body else selectedText
            val label = if (selectionMode) "selected text" else "page content"
            val instruction = when (mode) {
                "summarize" -> "Summarize the $label in clear, concise bullet points."
                "explain" -> "Explain the $label in simple language and highlight the key ideas."
                "translate" -> "Translate the $label and preserve the meaning and structure."
                else -> "Help me with the $label."
            }
            preparedChatPrompt = if (body.isBlank()) {
                "$instruction\nTitle: ${activeTab.title.ifBlank { "Current page" }}\nURL: ${activeTab.url}"
            } else {
                "$instruction\nTitle: ${activeTab.title.ifBlank { "Current page" }}\nURL: ${activeTab.url}\n\nContext:\n$body"
            }
        }
    }

    fun copyChatPromptToClipboard(prompt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Bluebird ChatGPT prompt", prompt))
        android.widget.Toast.makeText(context, "Prompt copied. Paste it into ChatGPT when you're ready.", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun captureTabThumbnail(tabId: String, webView: WebView) {
        if (webView.width <= 0 || webView.height <= 0) return
        val targetWidth = 240
        val targetHeight = 135
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val scale = minOf(targetWidth.toFloat() / webView.width, targetHeight.toFloat() / webView.height)
        canvas.save()
        canvas.scale(scale, scale)
        webView.draw(canvas)
        canvas.restore()
        tabThumbnails.remove(tabId)
        tabThumbnails[tabId] = bitmap
        while (tabThumbnails.size > 12) {
            val oldest = tabThumbnails.entries.firstOrNull() ?: break
            tabThumbnails.remove(oldest.key)
        }
    }

    fun cacheFavicon(tabId: String, bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        val size = 32
        val icon = Bitmap.createScaledBitmap(bitmap, size, size, true)
        tabFavicons.remove(tabId)
        tabFavicons[tabId] = icon
        while (tabFavicons.size > 24) {
            val key = tabFavicons.keys.firstOrNull() ?: break
            tabFavicons.remove(key)
        }
    }

    fun normalizeTabOrder() {
        val ordered = tabs.sortedWith(
            compareByDescending<BrowserTab> { it.isPinned }
                .thenBy { tabs.indexOf(it) }
        )
        if (ordered.map { it.id } != tabs.map { it.id }) {
            tabs.clear()
            tabs.addAll(ordered)
        }
    }

    fun moveTab(fromId: String, toId: String) {
        val fromIndex = tabs.indexOfFirst { it.id == fromId }
        val toIndex = tabs.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return
        if (tabs[fromIndex].isPinned != tabs[toIndex].isPinned) return
        val moved = tabs.removeAt(fromIndex)
        val adjustedTarget = tabs.indexOfFirst { it.id == toId }
        tabs.add(adjustedTarget.coerceAtLeast(0), moved)
        normalizeTabOrder()
        checkpointSession()
        scheduleSave()
    }

    fun createGroupForTab(tabId: String) {
        val color = listOf(0xFF1A73E8, 0xFF9C6DCA, 0xFF107C10, 0xFFD13438, 0xFFEAA300)[tabGroups.size % 5]
        val group = TabGroup(name = "Tab group ${tabGroups.size + 1}", color = color)
        tabGroups.add(group)
        updateTab(tabId) { groupId = group.id }
        scheduleSave()
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        updateTab(tabId) { this.groupId = groupId }
        scheduleSave()
    }

    fun removeEmptyGroups() {
        val used = tabs.mapNotNull { it.groupId }.toSet()
        tabGroups.removeAll { it.id !in used }
    }

    fun updateTab(id: String, block: BrowserTab.() -> Unit) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy().also(block)
        }
    }

    fun navigate(rawUrl: String, tabId: String = activeTabId) {
        val url    = UrlUtils.resolveUrl(rawUrl, settings.searchEngine)
        val tabIdx = tabs.indexOfFirst { it.id == tabId }
        if (tabIdx < 0) return
        val tab = tabs[tabIdx]
        if (tab.url.isNotEmpty() && tab.url != NEWTAB_URL) {
            tab.backStack.add(tab.url)
            tab.forwardStack.clear()
        }
        tabs[tabIdx] = tab.copy(url = url, displayUrl = UrlUtils.displayUrl(url))
        addressText       = UrlUtils.displayUrl(url)
        addressBarFocused = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (url != NEWTAB_URL) webViews[tabId]?.loadUrl(url)
        updateTab(tabId) { checkpointAt = System.currentTimeMillis(); rendererDiscarded = false; isLoading = url != NEWTAB_URL }
        scheduleSave()
        repo.checkpointTabs(tabs, activeTabId)
    }

    fun addTab(url: String = NEWTAB_URL, isPrivate: Boolean = false): String {
        if (tabs.size >= MAX_TABS) return activeTabId
        val tab = BrowserTab(url = url, isPrivate = isPrivate,
            faviconColor = UrlUtils.colorForUrl(url))
        tabs.add(tab)
        activeTabId = tab.id
        addressText = UrlUtils.displayUrl(url)
        checkpointSession()
        scheduleSave()
        return tab.id
    }

    fun closeTab(tab: BrowserTab) {
        // Private tabs must never enter the reopen-closed-tab stack.
        // Their URLs/history are session-only and should disappear when closed.
        lastClosedTab = if (tab.isPrivate) null else tab.copy(
            backStack = tab.backStack.toMutableList(),
            forwardStack = tab.forwardStack.toMutableList()
        )
        // Active WebView cleanup is owned by BrowserWebView.onDispose.
        // Inactive tabs have no renderer instance to destroy.
        webViews[tab.id]?.let { captureTabThumbnail(tab.id, it) }
        val idx = tabs.indexOf(tab)
        tabs.remove(tab)
        tabThumbnails.remove(tab.id)
        removeEmptyGroups()
        tabFavicons.remove(tab.id)
        if (tabs.isEmpty()) {
            addTab()
        } else if (tab.id == activeTabId) {
            activeTabId = tabs.getOrNull(idx)?.id ?: tabs.last().id
        }
        checkpointSession()
        scheduleSave()
    }

    fun reopenClosedTab() {
        val closed = lastClosedTab?.takeUnless { it.isPrivate } ?: return
        val restored = closed.copy(
            id = java.util.UUID.randomUUID().toString(),
            backStack = closed.backStack.toMutableList(),
            forwardStack = closed.forwardStack.toMutableList()
        )
        if (tabs.size >= MAX_TABS) return
        tabs.add(restored)
        activeTabId = restored.id
        lastClosedTab = null
        addressText = UrlUtils.displayUrl(restored.url)
        checkpointSession()
        scheduleSave()
    }

    fun goBack() {
        val tab = activeTab
        val wv  = activeWebView()
        if (wv?.canGoBack() == true) {
            wv.goBack()
        } else if (tab.backStack.isNotEmpty()) {
            val prev = tab.backStack.removeLast()
            tab.forwardStack.add(tab.url)
            updateTab(tab.id) { this.url = prev; this.displayUrl = UrlUtils.displayUrl(prev) }
            wv?.loadUrl(prev)
        }
    }

    fun goForward() {
        val tab = activeTab
        val wv  = activeWebView()
        if (wv?.canGoForward() == true) {
            wv.goForward()
        } else if (tab.forwardStack.isNotEmpty()) {
            val next = tab.forwardStack.removeLast()
            tab.backStack.add(tab.url)
            updateTab(tab.id) { this.url = next; this.displayUrl = UrlUtils.displayUrl(next) }
            wv?.loadUrl(next)
        }
    }

    fun toggleBookmark() {
        val url = activeTab.url
        if (url == NEWTAB_URL || url.isBlank()) return
        val existing = bookmarks.indexOfFirst { it.url == url }
        if (existing >= 0) {
            bookmarks.removeAt(existing)
        } else {
            bookmarks.add(0, Bookmark(
                title        = activeTab.title.ifBlank { url },
                url          = url,
                faviconColor = activeTab.faviconColor,
                folder       = "Bookmarks Bar"
            ))
        }
        scheduleSave()
    }

    fun addHistoryEntry(url: String, title: String, color: Long) {
        if (url == NEWTAB_URL || url.isBlank() || activeTab.isPrivate) return
        history.removeIf { it.url == url }
        history.add(0, HistoryEntry(
            title        = title.ifBlank { url },
            url          = url,
            faviconColor = color
        ))
        while (history.size > MAX_HISTORY_ENTRIES) history.removeLast()
        scheduleSave()
    }

    fun setZoom(zoom: Int) {
        currentZoom = zoom.coerceIn(25, 500)
        activeWebView()?.setInitialScale(currentZoom)
        settings = settings.copy(defaultZoom = currentZoom)
        scheduleSave()
    }

    fun sharePage() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, activeTab.url)
            putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    fun printPage() {
        val wv = activeWebView() ?: return
        val pm = context.getSystemService(PrintManager::class.java)
        val jobName = activeTab.title.ifBlank { "Bluebird Page" }
        val adapter = wv.createPrintDocumentAdapter(jobName)
        pm.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
    }

    // ── Back handler ──────────────────────────────────────────────────
    BackHandler {
        when {
            showMenu          -> showMenu = false
            showTabOverview   -> showTabOverview = false
            activePanel != BrowserPanel.NONE -> activePanel = BrowserPanel.NONE
            isFindActive      -> {
                isFindActive = false; findQuery = ""
                activeWebView()?.clearMatches()
            }
            addressBarFocused -> addressBarFocused = false
            else              -> goBack()
        }
    }

    // ── Sync address bar when tab changes ─────────────────────────────
    LaunchedEffect(activeTabId) {
        val tab = tabs.firstOrNull { it.id == activeTabId }
        if (tab != null) {
            addressText = UrlUtils.displayUrl(tab.url)
            currentZoom = settings.defaultZoom
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Layout
    // ═════════════════════════════════════════════════════════════════

    val isBookmarked = bookmarks.any { it.url == activeTab.url }
    val canGoBack    = (activeWebView()?.canGoBack() == true) || activeTab.backStack.isNotEmpty()
    val canGoForward = (activeWebView()?.canGoForward() == true) || activeTab.forwardStack.isNotEmpty()

    if (showChatContextConsent) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showChatContextConsent = false },
            title = { Text("Share page context with ChatGPT?", color = textColor, fontSize = 16.sp) },
            text = { Text(if (pendingChatContextMode == "selection") "Bluebird will read the selected text from this page and prepare a prompt. Nothing is sent automatically." else "Bluebird will read visible page text and prepare a ${pendingChatContextMode} prompt. Nothing is sent automatically and you can review it before copying it to ChatGPT.", color = textColor.copy(.75f), fontSize = 12.sp) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showChatContextConsent = false
                    buildChatPrompt(pendingChatContextMode)
                }) { Text("Continue", color = Color(0xFF1A73E8)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showChatContextConsent = false }) { Text("Cancel", color = textColor.copy(.7f)) } }
        )
    }

    ProvideTextStyle(TextStyle(fontFamily = FontFamily.SansSerif)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.Escape && addressBarFocused) {
                    addressBarFocused = false
                    addressText = UrlUtils.displayUrl(activeTab.url)
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    return@onPreviewKeyEvent true
                }
                // Desktop-style browser navigation shortcuts.
                if (event.isAltPressed && event.key == Key.DirectionLeft) {
                    goBack()
                    return@onPreviewKeyEvent true
                }
                if (event.isAltPressed && event.key == Key.DirectionRight) {
                    goForward()
                    return@onPreviewKeyEvent true
                }
                if (event.key == Key.F5) {
                    activeWebView()?.reload()
                    return@onPreviewKeyEvent true
                }
                if (!event.isCtrlPressed) return@onPreviewKeyEvent false
                when {
                    event.key == Key.L -> {
                        addressBarFocused = true
                        addressFocusRequestToken++
                        true
                    }
                    event.isShiftPressed && event.key == Key.T -> { reopenClosedTab(); true }
                    event.key == Key.T -> { addTab(); true }
                    event.key == Key.W -> { closeTab(activeTab); true }
                    event.key == Key.R -> { activeWebView()?.reload(); true }
                    event.key == Key.Tab -> {
                        if (tabs.size > 1) {
                            val index = tabs.indexOfFirst { it.id == activeTabId }
                            val next = if (event.isShiftPressed) (index - 1 + tabs.size) % tabs.size else (index + 1) % tabs.size
                            activeTabId = tabs[next].id
                            addressText = UrlUtils.displayUrl(tabs[next].url)
                        }
                        true
                    }
                    else -> false
                }
            }
            .background(mainBg)
    ) {
        // ── Tab Bar ────────────────────────────────────────────────────
        EdgeTabBar(
            tabs            = tabs,
            activeTabId     = activeTabId,
            isDark          = isDark,
            tabBarBg        = navBarBg,
            borderColor     = borderColor,
            onTabSelected   = { tab ->
                activeTabId     = tab.id
                addressText     = UrlUtils.displayUrl(tab.url)
                currentZoom     = settings.defaultZoom
                showTabOverview = false
                checkpointSession()
            },
            onTabClosed     = { closeTab(it) },
            onTabLongPressed = { tabContextId = it.id },
            onNewTab        = { addTab(); showTabOverview = false },
            onNewPrivateTab = { addTab(isPrivate = true); showTabOverview = false },
            onTabOverview   = { showTabOverview = !showTabOverview },
            onTabMoved      = { from, to -> moveTab(from, to) },
            onTabMiddleClicked = { closeTab(it) }
        )

        // ── Navigation Bar ─────────────────────────────────────────────
        EdgeNavigationBar(
            isDark              = isDark,
            navBarBg            = navBarBg,
            borderColor         = borderColor,
            isLoading           = activeTab.isLoading,
            loadingProgress     = activeTab.loadProgress,
            canGoBack           = canGoBack,
            canGoForward        = canGoForward,
            addressText         = addressText,
            addressBarFocused   = addressBarFocused,
            isBookmarked        = isBookmarked,
            isPrivateTab        = activeTab.isPrivate,
            activeUrl           = activeTab.url,
            zoomLevel           = currentZoom,
            onBack              = { goBack() },
            onForward           = { goForward() },
            onRefresh           = {
                if (activeTab.isLoading) activeWebView()?.stopLoading()
                else activeWebView()?.reload()
            },
            onHome              = { navigate(settings.searchEngine.homeUrl) },
            onAddressChange     = { text ->
                addressText = text
                if (!addressBarFocused) addressBarFocused = true
            },
            onAddressFocus      = {
                addressBarFocused = true
            },
            onAddressGo         = {
                navigate(addressText)
            },
            onAddressClear      = {
                addressText = ""
                addressBarFocused = true
                addressFocusRequestToken++
            },
            addressFocusRequestToken = addressFocusRequestToken,
            onBookmarkToggle    = { toggleBookmark() },
            onMenuOpen          = { showMenu = true },
            onBookmarksPanel    = {
                activePanel = if (activePanel == BrowserPanel.BOOKMARKS) BrowserPanel.NONE
                else BrowserPanel.BOOKMARKS
            },
            onHistoryPanel      = {
                activePanel = if (activePanel == BrowserPanel.HISTORY) BrowserPanel.NONE
                else BrowserPanel.HISTORY
            },
            onDownloadsPanel    = {
                activePanel = if (activePanel == BrowserPanel.DOWNLOADS) BrowserPanel.NONE
                else BrowserPanel.DOWNLOADS
            },
            onSettingsPanel     = {
                activePanel = if (activePanel == BrowserPanel.SETTINGS) BrowserPanel.NONE
                else BrowserPanel.SETTINGS
            },
            onSiteSettings      = { activePanel = BrowserPanel.SITE_SETTINGS },
            onFindInPage        = {
                isFindActive = true
            },
            onChatGptPanel      = { activePanel = if (activePanel == BrowserPanel.CHATGPT) BrowserPanel.NONE else BrowserPanel.CHATGPT },
            onDesktopModeToggle = {
                settings = settings.copy(desktopMode = !settings.desktopMode)
                activeWebView()?.reload()
            }
        )

        // ── Bookmarks Bar ──────────────────────────────────────────────
        AnimatedVisibility(settings.showBookmarksBar && !activeTab.isPrivate) {
            BookmarksBar(
                bookmarks       = bookmarks,
                isDark          = isDark,
                navBarBg        = navBarBg,
                borderColor     = borderColor,
                onBookmarkClick = { bm -> navigate(bm.url) }
            )
        }

        // ── Address suggestions ────────────────────────────────────────
        AnimatedVisibility(
            visible = addressBarFocused && addressText.isNotBlank()
        ) {
            AddressSuggestionsDropdown(
                query             = addressText,
                history           = history,
                bookmarks         = bookmarks,
                searchEngine      = settings.searchEngine,
                isDark            = isDark,
                onSuggestionClick = { url ->
                    navigate(url)
                    addressBarFocused = false
                },
                onDismiss         = { addressBarFocused = false }
            )
        }

        // ── Find in Page bar ───────────────────────────────────────────
        AnimatedVisibility(isFindActive) {
            FindInPageBar(
                query         = findQuery,
                activeMatch   = findActiveMatch,
                totalMatches  = findTotalMatches,
                isDark        = isDark,
                    onQueryChange = { q ->
                    findQuery = q
                    if (q.isEmpty()) {
                        activeWebView()?.clearMatches()
                        findActiveMatch  = 0
                        findTotalMatches = 0
                    } else {
                        activeWebView()?.findAllAsync(q)
                    }
                },
                onFocused     = { addressBarFocused = true },
                onFindNext    = { activeWebView()?.findNext(true) },
                onFindPrev    = { activeWebView()?.findNext(false) },
                onClose       = {
                    isFindActive     = false
                    findQuery        = ""
                    findActiveMatch  = 0
                    findTotalMatches = 0
                    activeWebView()?.clearMatches()
                }
            )
        }

        // ── Main content area ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ── Memory-aware tab lifecycle ─────────────────────────────
            // Only the active tab owns a WebView. Inactive tabs retain their
            // URL/history/scroll snapshot but not a renderer process. This
            // keeps large tab counts practical on lower-memory devices.
            val tab = activeTab
            val isNewTab = tab.url == NEWTAB_URL

            Box(Modifier.fillMaxSize().zIndex(1f)) {
                if (isNewTab) {
                    NewTabPage(
                        isDark     = isDark,
                        isPrivate  = tab.isPrivate,
                        settings   = settings,
                        bookmarks  = bookmarks,
                        history    = history,
                        onSettingsChange = { updated ->
                            settings = updated
                            scheduleSave()
                        },
                        onNavigate = { url -> navigate(url, tab.id) }
                    )
                } else {
                    key(tab.id) {
                    BrowserWebView(
                            url                  = tab.url,
                            tab                  = tab,
                            settings             = settings,
                            findQuery            = findQuery,
                            isFindActive         = isFindActive,
                            modifier             = Modifier.fillMaxSize(),
                            onWebViewReady       = { wv ->
                                webViews[tab.id] = wv
                                updateTab(tab.id) { rendererDiscarded = false; isLoading = false; loadProgress = 0f }
                            },
                            onWebViewDisposed    = { doomed, scrollY ->
                                updateTab(tab.id) {
                                    this.scrollY = scrollY.coerceAtLeast(0)
                                    this.rendererDiscarded = true
                                    this.checkpointAt = System.currentTimeMillis()
                                }
                                webViews.remove(tab.id)
                                if (tab.isPrivate) {
                                    // Private renderer data is cleared immediately when
                                    // its renderer leaves the composition.
                                    doomed?.clearHistory()
                                    doomed?.clearFormData()
                                    doomed?.clearSslPreferences()
                                    doomed?.clearCache(true)
                                }
                                scheduleSave()
                            },
                            restoreScrollY       = tab.scrollY,
                            onPageStarted        = { url ->
                                pageErrors.remove(tab.id)
                                updateTab(tab.id) {
                                    this.url          = url
                                    this.displayUrl   = UrlUtils.displayUrl(url)
                                    this.isLoading    = true
                                    this.loadProgress = 0f
                                }
                                addressText = UrlUtils.displayUrl(url)
                            },
                            onProgressChanged    = { p ->
                                updateTab(tab.id) { loadProgress = p / 100f }
                            },
                            onPageError          = { failedUrl, message ->
                                pageErrors[tab.id] = failedUrl to message.ifBlank { "The page could not be loaded." }
                                updateTab(tab.id) {
                                    this.isLoading = false
                                    this.loadProgress = 0f
                                }
                            },
                            onPageFinished       = { url, pageTitle ->
                                pageErrors.remove(tab.id)
                                val resolvedTitle = pageTitle?.takeIf { it.isNotBlank() } ?: url
                                val color         = UrlUtils.colorForUrl(url)
                                updateTab(tab.id) {
                                    this.url          = url
                                    this.displayUrl   = UrlUtils.displayUrl(url)
                                    this.isLoading    = false
                                    this.loadProgress = 1f
                                    this.lastVisited  = System.currentTimeMillis()
                                    this.faviconColor = color
                                }
                                addHistoryEntry(url, resolvedTitle, color)
                                addressText = UrlUtils.displayUrl(url)
                                scheduleSave()
                            },
                            onTitleChanged       = { title ->
                                updateTab(tab.id) { this.title = title }
                                scheduleSave()
                            },
                            onUrlChanged         = { url ->
                                addressText = UrlUtils.displayUrl(url)
                            },
                            onFaviconChanged     = { bitmap -> cacheFavicon(tab.id, bitmap) },
                            onCredentialFormDetected = { username ->
                                if (!tab.isPrivate && settings.offerToSavePasswords && activeTabId == tab.id && !tab.isLoading) {
                                    val origin = runCatching {
                                        val uri = android.net.Uri.parse(tab.url)
                                        if (uri.scheme == "https" && !uri.host.isNullOrBlank()) "https://${uri.host}" else ""
                                    }.getOrDefault("")
                                    if (origin.isNotBlank() && credentials.none { it.origin == origin && it.username == (username ?: "") }) {
                                        // Detection only; password is not read until the user accepts.
                                        credentialOffer = origin to username
                                    }
                                }
                            },
                            onFindResultsChanged = { active, total ->
                                findActiveMatch  = active
                                findTotalMatches = total
                            },
                            onDownloadStart      = { dlUrl, ua, cd, mime, len ->
                                val item = dlHelper.enqueue(
                                    url                = dlUrl,
                                    userAgent          = ua,
                                    contentDisposition = cd,
                                    mimeType           = mime,
                                    contentLength      = len,
                                    onProgress         = { progress, bytes ->
                                        val idx = downloads.indexOfFirst { it.url == dlUrl }
                                        if (idx >= 0) {
                                            downloads[idx] = downloads[idx].copy(
                                                progress        = progress,
                                                bytesDownloaded = bytes
                                            )
                                            scheduleSave()
                                        }
                                    },
                                    onComplete = { status ->
                                        val idx = downloads.indexOfFirst { it.url == dlUrl }
                                        if (idx >= 0) {
                                            downloads[idx] = downloads[idx].copy(status = status)
                                            scheduleSave()
                                        }
                                    }
                                )
                                downloads.add(0, item)
                                scheduleSave()
                                activePanel = BrowserPanel.DOWNLOADS
                            },
                            onJsDialog           = { d -> jsDialog = d },
                            onSslError           = { d -> sslDialog = d },
                            onPermissionRequest  = { r -> permDialog = r },
                            onRememberPermission = { origin, resource, decision ->
                                repo.saveSitePermission(com.io.github.norbertweb.bluebird.browser.model.SitePermission(origin, resource, decision))
                sitePermissions = repo.loadSitePermissions()
                            },
                            getStoredPermission  = { origin, resource -> repo.getSitePermission(origin, resource) },
                            onGeolocationRequest = { origin, cb -> geoDialog = Pair(origin, cb) },
                            onNewTabRequested    = { url -> addTab(url) },
                            isDark               = isDark,
                            isActive             = true
                        )
                    }
                    }
                }

                // Renderer recreation is intentionally lightweight. Show a calm
                // browser-style loading surface instead of exposing a blank frame
                // while an evicted tab's WebView is being rebuilt.
                if (!isNewTab && (tab.isLoading || tab.rendererDiscarded && webViews[tab.id] == null)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (tab.rendererDiscarded) {
                            tabThumbnails[tab.id]?.let { preview ->
                                if (!preview.isRecycled) {
                                    androidx.compose.foundation.Image(
                                        bitmap = preview.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        alpha = 0.72f
                                    )
                                }
                            }
                            Box(Modifier.fillMaxSize().background(mainBg.copy(alpha = 0.38f)))
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                if (tab.rendererDiscarded) "Restoring tab…" else "Loading…",
                                color = textColor.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                val pageError = pageErrors[tab.id]
                if (!isNewTab && pageError != null && !tab.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(mainBg.copy(alpha = 0.96f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                        ) {
                            FluentIcon(FluentIcons.Error, null, tint = if (isDark) Color(0xFFFFB4AB) else Color(0xFFB3261E), modifier = Modifier.size(34.dp))
                            Text("This page could not be loaded", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = textColor)
                            Text(pageError.second, fontSize = 12.sp, color = textColor.copy(alpha = 0.65f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.TextButton(onClick = { pageErrors.remove(tab.id); activeWebView()?.loadUrl(pageError.first) }) {
                                    FluentIcon(FluentIcons.Refresh, null, tint = textColor, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Try again", color = textColor)
                                }
                                androidx.compose.material3.TextButton(onClick = { pageErrors.remove(tab.id); navigate(settings.searchEngine.homeUrl) }) {
                                    FluentIcon(FluentIcons.Home, null, tint = textColor, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Home", color = textColor)
                                }
                            }
                        }
                    }
                }
            }

            // ── Side Panel — anchored TopEnd inside Box ────────────────
            // AnimatedVisibility is NOT used here because its ColumnScope
            // extension overload cannot be resolved unambiguously inside Box.
            // Visibility is controlled by the outer if-guard instead.
            if (activePanel != BrowserPanel.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .zIndex(10f)
                ) {
                    SidePanel(
                        panel            = activePanel,
                        isDark           = isDark,
                        surfaceColor     = surfaceColor,
                        textColor        = textColor,
                        borderColor      = borderColor,
                        bookmarks        = bookmarks,
                        bookmarkFolders = bookmarkFolders,
                        history          = history,
                        downloads        = downloads,
                        settings         = settings,
                        onSettingsChange = { s ->
                            settings = s
                            val wv = activeWebView()
                            wv?.settings?.javaScriptEnabled        = s.javaScriptEnabled
                            wv?.settings?.loadsImagesAutomatically = s.showImages
                            scheduleSave()
                        },
                        onNavigate       = { url -> navigate(url); activePanel = BrowserPanel.NONE },
                        onBookmarksChanged = { scheduleSave() },
                        onHistoryChanged = { scheduleSave() },
                        onDownloadOpen  = { dl -> dlHelper.open(dl.downloadManagerId) },
                        onDownloadRemove = { dl ->
                            dlHelper.remove(dl.downloadManagerId)
                            downloads.removeAll { it.id == dl.id }
                            scheduleSave()
                        },
                        onDownloadCancel = { dl ->
                            dlHelper.cancel(dl.downloadManagerId)
                            val idx = downloads.indexOfFirst { it.id == dl.id }
                            if (idx >= 0) downloads[idx] = downloads[idx].copy(status = com.io.github.norbertweb.bluebird.browser.model.DownloadStatus.CANCELLED)
                            scheduleSave()
                        },
                        onDownloadRetry = { dl ->
                            dlHelper.remove(dl.downloadManagerId)
                            val idx = downloads.indexOfFirst { it.id == dl.id }
                            val replacement = dlHelper.retry(
                                dl,
                                onProgress = { progress, bytes ->
                                    val i = downloads.indexOfFirst { it.id == dl.id }
                                    if (i >= 0) downloads[i] = downloads[i].copy(progress = progress, bytesDownloaded = bytes, status = com.io.github.norbertweb.bluebird.browser.model.DownloadStatus.DOWNLOADING)
                                    scheduleSave()
                                },
                                onComplete = { status ->
                                    val i = downloads.indexOfFirst { it.id == dl.id }
                                    if (i >= 0) downloads[i] = downloads[i].copy(status = status)
                                    scheduleSave()
                                }
                            )
                            if (idx >= 0) downloads[idx] = replacement else downloads.add(0, replacement)
                            scheduleSave()
                        },
                        onShowDownloads = { dlHelper.openDownloadsFolder() },
                        onClearCompletedDownloads = {
                            downloads.filter { it.status == com.io.github.norbertweb.bluebird.browser.model.DownloadStatus.COMPLETED }
                                .forEach { dlHelper.remove(it.downloadManagerId); downloads.remove(it) }
                            scheduleSave()
                        },
                        currentPageTitle = activeTab.title,
                        currentPageUrl   = activeTab.url,
                        credentials = credentials,
                        onSaveCredential = { credential ->
                            val idx = credentials.indexOfFirst { it.id == credential.id }
                            if (idx >= 0) credentials[idx] = credential else credentials.add(credential)
                            credentialVault.save(credentials)
                        },
                        onDeleteCredential = { credential ->
                            credentials.removeAll { it.id == credential.id }
                            credentialVault.save(credentials)
                        },
                        onOpenCredentialSite = { url -> navigate(url); activePanel = BrowserPanel.NONE },
                        onClearSiteData = { origin ->
                            android.webkit.WebStorage.getInstance().deleteOrigin(origin)
                            activeWebView()?.reload()
                        },
                        onRequestPasswordAuth = { onPasswordAuthenticationRequest() },
                        passwordAuthGranted = passwordAuthGranted,
                        sitePermissions = sitePermissions,
                        onClearSitePermissions = { origin ->
                            sitePermissions.filter { it.origin == origin }.forEach { repo.removeSitePermission(it.origin, it.resource) }
                            sitePermissions = repo.loadSitePermissions()
                        },
                        onSetSitePermission = { origin, resource, decision ->
                            repo.saveSitePermission(SitePermission(origin = origin, resource = resource, decision = decision))
                            sitePermissions = repo.loadSitePermissions()
                        },
                        onResetSitePermission = { origin, resource ->
                            repo.removeSitePermission(origin, resource)
                            sitePermissions = repo.loadSitePermissions()
                        },
                        selectedText = selectedText,
                        preparedPrompt = preparedChatPrompt,
                        onAskAboutPage = { mode -> pendingChatContextMode = mode; showChatContextConsent = true },
                        onAskAboutSelection = { pendingChatContextMode = "selection"; showChatContextConsent = true },
                        onCopyPrompt = { copyChatPromptToClipboard(it) },
                        onOpenChatGpt = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://chatgpt.com/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        onClose          = { activePanel = BrowserPanel.NONE }
                    )
                }
            }

            // ── Tab Overview ───────────────────────────────────────────
            if (showTabOverview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                ) {
                    TabOverviewGrid(
                        tabs            = tabs,
                        activeTabId     = activeTabId,
                        isDark          = isDark,
                        tabThumbnails   = tabThumbnails,
                        tabFavicons     = tabFavicons,
                        tabGroups       = tabGroups,
                        onSelectTab     = { tab ->
                            activeTabId     = tab.id
                            addressText     = UrlUtils.displayUrl(tab.url)
                            showTabOverview = false
                        },
                        onCloseTab      = { closeTab(it) },
                        onNewTab        = { addTab(); showTabOverview = false },
                        onNewPrivateTab = { addTab(isPrivate = true); showTabOverview = false },
                        onAssignTabToGroup = { tabId, groupId -> assignTabToGroup(tabId, groupId) },
                        onCreateGroupForTab = { tabId -> createGroupForTab(tabId) }
                    )
                }
            }

            if (tabContextId != null) {
                val contextTab = tabs.firstOrNull { it.id == tabContextId }
                if (contextTab != null) {
                    EdgeTabContextMenu(
                        tab = contextTab,
                        isDark = isDark,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        canCloseOthers = tabs.size > 1,
                        canReopenClosed = lastClosedTab != null && tabs.size < MAX_TABS,
                        tabGroups = tabGroups,
                        onSelect = { activeTabId = contextTab.id; addressText = UrlUtils.displayUrl(contextTab.url); tabContextId = null },
                        onTogglePin = { updateTab(contextTab.id) { isPinned = !isPinned }; normalizeTabOrder(); tabContextId = null; checkpointSession(); scheduleSave() },
                        onAssignGroup = { groupId -> assignTabToGroup(contextTab.id, groupId); tabContextId = null },
                        onCreateGroup = { createGroupForTab(contextTab.id); tabContextId = null },
                        onRemoveFromGroup = { assignTabToGroup(contextTab.id, null); removeEmptyGroups(); tabContextId = null },
                        onDuplicate = { if (tabs.size < MAX_TABS) { addTab(contextTab.url); tabContextId = null } },
                        onCloseOthers = {
                            val keep = contextTab.id
                            tabs.filter { it.id != keep && !it.isPinned }.toList().forEach { closeTab(it) }
                            activeTabId = keep
                            tabContextId = null
                            scheduleSave()
                        },
                        onReopenClosed = { reopenClosedTab(); tabContextId = null },
                        onClose = { closeTab(contextTab); tabContextId = null }
                    )
                } else tabContextId = null
            }

            // ── Context Menu ───────────────────────────────────────────
            if (showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(30f)
                ) {
                    EdgeContextMenu(
                        isDark          = isDark,
                        surfaceColor    = surfaceColor,
                        textColor       = textColor,
                        isBookmarked    = isBookmarked,
                        isPrivateTab    = activeTab.isPrivate,
                        currentZoom     = currentZoom,
                        onNewTab        = { addTab() },
                        onNewPrivateTab = { addTab(isPrivate = true) },
                        onBookmarks     = { activePanel = BrowserPanel.BOOKMARKS },
                        onHistory       = { activePanel = BrowserPanel.HISTORY },
                        onDownloads     = { activePanel = BrowserPanel.DOWNLOADS },
                        onSettings      = { activePanel = BrowserPanel.SETTINGS },
                        onChatGpt       = { activePanel = BrowserPanel.CHATGPT },
                        onPasswords     = { activePanel = BrowserPanel.PASSWORDS },
                        onFillPassword  = {
                            val origin = runCatching { android.net.Uri.parse(activeTab.url).let { if (it.scheme == "https" && !it.host.isNullOrBlank()) "https://${it.host}" else "" } }.getOrDefault("")
                            if (!activeTab.isPrivate && settings.autofillPasswords && origin.isNotBlank()) {
                                if (settings.requireDeviceAuthForPasswords && !passwordAuthGranted) {
                                    pendingCredentialFill = StoredCredential(origin = origin, username = "", password = "")
                                    onPasswordAuthenticationRequest()
                                } else credentialPickerOrigin = origin
                            }
                        },
                        onSiteSettings  = { activePanel = BrowserPanel.SITE_SETTINGS },
                        onFindInPage    = { isFindActive = true },
                        onPrint         = { printPage() },
                        onZoomIn        = { setZoom(currentZoom + 10) },
                        onZoomOut       = { setZoom(currentZoom - 10) },
                        onZoomReset     = { setZoom(100) },
                        onAddBookmark   = { toggleBookmark() },
                        onShare         = { sharePage() },
                        onClearData     = { showClearDataDialog = true },
                        onDismiss       = { showMenu = false }
                    )
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Dialogs (float above everything)
    // ═════════════════════════════════════════════════════════════════

    credentialPickerOrigin?.let { origin ->
        val matches = credentials.filter { it.origin == origin }
        CredentialPickerDialog(
            origin = origin,
            credentials = matches,
            isDark = isDark,
            onDismiss = { credentialPickerOrigin = null },
            onSelect = { credential ->
                credentialPickerOrigin = null
                if (settings.requireDeviceAuthForPasswords && !passwordAuthGranted) {
                    pendingCredentialFill = credential
                    onPasswordAuthenticationRequest()
                } else {
                    activeWebView()?.fillCredentialIntoCurrentForm(credential.username, credential.password)
                }
            }
        )
    }

    credentialOffer?.let { (origin, username) ->
        val existing = credentials.firstOrNull { it.origin == origin && (username.isNullOrBlank() || it.username == username) }
        SavePasswordDialog(
            origin = origin,
            username = username,
            isUpdate = existing != null,
            isDark = isDark,
            onSave = {
                activeWebView()?.captureCredentialFromCurrentForm { capturedUser, capturedPassword ->
                    onMain {
                        if (!capturedUser.isNullOrBlank() && !capturedPassword.isNullOrBlank()) {
                            val old = credentials.firstOrNull { it.origin == origin && it.username == capturedUser }
                            val item = StoredCredential(
                                id = old?.id ?: java.util.UUID.randomUUID().toString(),
                                origin = origin,
                                username = capturedUser,
                                password = capturedPassword,
                                nickname = old?.nickname ?: "",
                                createdAt = old?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            val index = credentials.indexOfFirst { it.id == item.id }
                            if (index >= 0) credentials[index] = item else credentials.add(item)
                            credentialVault.save(credentials)
                        }
                        credentialOffer = null
                    }
                }
            },
            onDismiss = { credentialOffer = null }
        )
    }

    jsDialog?.let { dialog ->
        JsDialog(state = dialog, isDark = isDark)
    }

    sslDialog?.let { dialog ->
        SslWarningDialog(
            state  = dialog.copy(
                onProceed = { dialog.onProceed(); sslDialog = null },
                onCancel  = { dialog.onCancel();  sslDialog = null }
            ),
            isDark = isDark
        )
    }

    permDialog?.let { req ->
        PermissionRequestDialog(
            request = BrowserPermissionRequest(
                origin    = req.origin,
                resources = req.resources,
                grant     = { req.grant(); permDialog = null },
                deny      = { req.deny();  permDialog = null },
                remember  = req.remember
            ),
            isDark = isDark
        )
    }

    geoDialog?.let { (origin, cb) ->
        GeolocationDialog(
            origin  = origin,
            onAllow = { cb.invoke(origin, true, false);  geoDialog = null },
            onDeny  = { cb.invoke(origin, false, false); geoDialog = null },
            onRemember = { remember -> repo.saveSitePermission(com.io.github.norbertweb.bluebird.browser.model.SitePermission(origin, "geolocation", if (remember) StoredPermissionDecision.ALLOW else StoredPermissionDecision.DENY)) },
            isDark  = isDark
        )
    }

    if (showClearDataDialog) {
        ClearBrowsingDataDialog(
            isDark = isDark,
            onDismiss = { showClearDataDialog = false },
            onClear = { options ->
                if (ClearDataOption.HISTORY in options) { history.clear(); repo.clearHistory() }
                if (ClearDataOption.COOKIES in options) repo.clearCookies(context)
                if (ClearDataOption.CACHE in options) { repo.clearCache(context); homeContentRepo.clearCache() }
                if (ClearDataOption.SITE_STORAGE in options) repo.clearSiteStorage()
                if (ClearDataOption.FORM_DATA in options) repo.clearFormData(context)
                if (ClearDataOption.DOWNLOADS in options) {
                    downloads.forEach { dl -> dlHelper.remove(dl.downloadManagerId) }
                    downloads.clear(); repo.clearDownloads()
                }
                if (ClearDataOption.HISTORY in options || ClearDataOption.COOKIES in options || ClearDataOption.CACHE in options || ClearDataOption.SITE_STORAGE in options || ClearDataOption.FORM_DATA in options) {
                    repo.clearSitePermissions()
                }
                showClearDataDialog = false
                scheduleSave()
            }
        )
    }
}
