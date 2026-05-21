package com.win11launcher.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.win11launcher.ui.theme.Win11Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Tab",
    var url: String = "edge://newtab",
    val backStack: MutableList<String> = mutableListOf(),
    val forwardStack: MutableList<String> = mutableListOf(),
    var faviconColor: Color = Color(0xFF0078D4),
    var isMuted: Boolean = false,
    var isPinned: Boolean = false,
    var isLoading: Boolean = false,
    var loadProgress: Float = 0f,
    var lastVisited: Long = System.currentTimeMillis()
)

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconColor: Color = Color(0xFF0078D4),
    val folder: String = "Bookmarks Bar",
    val addedAt: Long = System.currentTimeMillis()
)

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconColor: Color = Color(0xFF0078D4),
    val visitedAt: Long = System.currentTimeMillis()
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val mimeType: String,
    val fileSize: Long = 0L,
    var status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val startedAt: Long = System.currentTimeMillis(),
    var progress: Float = 0f
)

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED, PAUSED }

data class BrowserSettings(
    var useBuiltInKeyboard: Boolean = true,
    var searchEngine: SearchEngine = SearchEngine.GOOGLE,
    var darkMode: Boolean = false,
    var adBlockEnabled: Boolean = true,
    var trackingProtection: Boolean = true,
    var javaScriptEnabled: Boolean = true,
    var saveCookies: Boolean = true,
    var showBookmarksBar: Boolean = true,
    var startPage: StartPage = StartPage.NEW_TAB,
    var fontSize: Int = 100,
    var defaultZoom: Int = 100,
    var showImages: Boolean = true,
    var popupBlocker: Boolean = true,
    var locationAccess: Boolean = false,
    var cameraAccess: Boolean = false,
    var microphoneAccess: Boolean = false
)

enum class SearchEngine(val label: String, val url: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q="),
    BRAVE("Brave", "https://search.brave.com/search?q=")
}

enum class StartPage { NEW_TAB, BLANK, CONTINUE_WHERE_LEFT_OFF }

enum class BrowserPanel { NONE, BOOKMARKS, HISTORY, DOWNLOADS, SETTINGS, EXTENSIONS, COLLECTIONS }

// ═══════════════════════════════════════════════════════════════════════
// STATIC DATA
// ═══════════════════════════════════════════════════════════════════════

private val quickLinks = listOf(
    Triple("Google", "https://www.google.com", Color(0xFFEA4335)),
    Triple("YouTube", "https://www.youtube.com", Color(0xFFFF0000)),
    Triple("Microsoft", "https://www.microsoft.com", Color(0xFF0078D4)),
    Triple("GitHub", "https://github.com", Color(0xFF181717)),
    Triple("Reddit", "https://www.reddit.com", Color(0xFFFF4500)),
    Triple("Twitter", "https://twitter.com", Color(0xFF1DA1F2)),
    Triple("Wikipedia", "https://www.wikipedia.org", Color(0xFF636363)),
    Triple("Amazon", "https://www.amazon.com", Color(0xFFFF9900)),
    Triple("Netflix", "https://www.netflix.com", Color(0xFFE50914)),
    Triple("Discord", "https://discord.com", Color(0xFF5865F2)),
)

private val sampleNews = listOf(
    Triple("AI reshapes the future of work and creativity in 2026", "TechCrunch", Color(0xFF0078D4)),
    Triple("Markets surge as quantum computing breakthrough announced", "Bloomberg", Color(0xFF00897B)),
    Triple("Scientists develop new battery lasting 10x longer", "Nature", Color(0xFF7B1FA2)),
    Triple("SpaceX Starship completes first orbital mission", "Space.com", Color(0xFF1565C0)),
    Triple("New privacy laws reshape the internet globally", "Wired", Color(0xFFC62828)),
)

// ═══════════════════════════════════════════════════════════════════════
// BUILT-IN KEYBOARD
// ═══════════════════════════════════════════════════════════════════════

private val keyboardRows = listOf(
    listOf("q","w","e","r","t","y","u","i","o","p"),
    listOf("a","s","d","f","g","h","j","k","l"),
    listOf("⇧","z","x","c","v","b","n","m","⌫"),
    listOf("123","@","_","-","space",".",".com","↵")
)

private val numericRows = listOf(
    listOf("1","2","3","4","5","6","7","8","9","0"),
    listOf("!","@","#","$","%","^","&","*","(",")"),
    listOf("-","_","=","+","[","]","{","}","\\","|"),
    listOf("ABC","/",":",";","\"","'",",",".","↵")
)

@Composable
fun FloatingKeyboard(
    currentText: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    var isUppercase by remember { mutableStateOf(false) }
    var showNumeric by remember { mutableStateOf(false) }
    val currentRows = if (showNumeric) numericRows else keyboardRows

    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD1D5DB)
    val keyBg = if (isDark) Color(0xFF3A3A3A) else Color.White
    val specialKeyBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFB0B5BC)
    val textColor = if (isDark) Color.White else Color.Black
    val accentColor = Color(0xFF0078D4)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Suggestion bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    if (currentText.endsWith("w")) "www" else "",
                    if (currentText.isNotEmpty()) currentText.replaceFirstChar { it.uppercase() } else "",
                    ".com"
                ).filter { it.isNotEmpty() }.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = accentColor,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { onTextChange(currentText + suggestion) }
                    )
                    Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x30888888)))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardHide, null, tint = textColor.copy(0.6f), modifier = Modifier.size(12.dp))
                }
            }

            // Keyboard rows
            currentRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        val isSpecial = key in listOf("⇧", "⌫", "123", "ABC", "space", "↵", "@", "_", "-", ".", ".com", "/", ":", ";")
                        val displayKey = when {
                            key == "space" -> "space"
                            key == ".com" -> ".com"
                            !showNumeric && isUppercase && key.length == 1 -> key.uppercase()
                            else -> key
                        }
                        val keyWeight = when (key) {
                            "space" -> 4f
                            "⇧", "⌫", "123", "ABC" -> 1.6f
                            "↵" -> 1.8f
                            else -> 1f
                        }

                        Box(
                            modifier = Modifier
                                .weight(keyWeight)
                                .height(26.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when (key) {
                                        "↵" -> accentColor
                                        "⇧" -> if (isUppercase) accentColor.copy(0.3f) else specialKeyBg
                                        "space" -> keyBg
                                        in listOf("⌫", "123", "ABC") -> specialKeyBg
                                        else -> keyBg
                                    }
                                )
                                .clickable {
                                    when (key) {
                                        "⇧" -> isUppercase = !isUppercase
                                        "⌫" -> if (currentText.isNotEmpty()) onTextChange(currentText.dropLast(1))
                                        "space" -> onTextChange(currentText + " ")
                                        "↵" -> onDismiss()
                                        "123" -> showNumeric = true
                                        "ABC" -> showNumeric = false
                                        ".com" -> onTextChange(currentText + ".com")
                                        else -> {
                                            val charToAdd = if (!showNumeric && isUppercase) key.uppercase() else key
                                            onTextChange(currentText + charToAdd)
                                            if (isUppercase && !showNumeric) isUppercase = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayKey,
                                color = when (key) {
                                    "↵" -> Color.White
                                    else -> textColor
                                },
                                fontSize = when (key) {
                                    "space", ".com" -> 8.sp
                                    else -> 10.sp
                                },
                                fontWeight = if (key == "↵") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MAIN BROWSER SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(isDark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Colors ──
    val edgeBg = if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
    val tabBarBg = if (isDark) Color(0xFF292929) else Color(0xFFE3E3E3)
    val navBarBg = if (isDark) Color(0xFF272727) else Color(0xFFEFEFEF)
    val surfaceColor = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF202020)
    val accentBlue = Color(0xFF0078D4)
    val borderColor = if (isDark) Color(0x20FFFFFF) else Color(0x20000000)

    // ── Settings ──
    var settings by remember { mutableStateOf(BrowserSettings(darkMode = isDark)) }

    // ── Tabs ──
    val tabs = remember { mutableStateListOf<BrowserTab>() }
    var activeTabId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val t = BrowserTab()
            tabs.add(t)
            activeTabId = t.id
        }
    }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: return

    // ── Address bar state ──
    var addressText by remember { mutableStateOf(activeTab.url) }
    var addressBarFocused by remember { mutableStateOf(false) }
    var showAddressSuggestions by remember { mutableStateOf(false) }

    // ── Navigation state ──
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }

    // ── WebView ref ──
    var webView by remember { mutableStateOf<WebView?>(null) }

    // ── Panels ──
    var openPanel by remember { mutableStateOf(BrowserPanel.NONE) }
    var showTabOverview by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    // ── Keyboard ──
    var showBuiltInKeyboard by remember { mutableStateOf(false) }
    var keyboardTarget by remember { mutableStateOf("address") } // "address" | "find" | "newtab"

    // ── Sidebar state ──
    val sidebarCollapsed = true // always slim in landscape

    // ── Data ──
    val bookmarks = remember { mutableStateListOf<Bookmark>() }
    val history = remember { mutableStateListOf<HistoryEntry>() }
    val downloads = remember { mutableStateListOf<DownloadItem>() }
    val isBookmarked = bookmarks.any { it.url == activeTab.url }

    // ── Bookmark bar ──
    val bookmarkBarItems = remember(bookmarks) { bookmarks.filter { it.folder == "Bookmarks Bar" } }

    // ── Tab sync ──
    LaunchedEffect(activeTabId) {
        val tab = tabs.find { it.id == activeTabId } ?: return@LaunchedEffect
        addressText = tab.url
        canGoBack = tab.backStack.isNotEmpty()
        canGoForward = tab.forwardStack.isNotEmpty()
        if (tab.url != "edge://newtab") webView?.loadUrl(tab.url)
    }

    // ── Navigation helpers ──
    fun navigate(url: String) {
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http") && !finalUrl.startsWith("edge://")) {
            finalUrl = if (finalUrl.contains(".") && !finalUrl.contains(" ")) {
                "https://$finalUrl"
            } else {
                "${settings.searchEngine.url}${Uri.encode(finalUrl)}"
            }
        }
        if (activeTab.url != finalUrl) {
            activeTab.backStack.add(activeTab.url)
            activeTab.forwardStack.clear()
        }
        activeTab.url = finalUrl
        addressText = finalUrl
        canGoBack = activeTab.backStack.isNotEmpty()
        canGoForward = false
        webView?.loadUrl(finalUrl)
        showAddressSuggestions = false
        showBuiltInKeyboard = false
        addressBarFocused = false
    }

    fun goBack() {
        if (activeTab.backStack.isNotEmpty()) {
            activeTab.forwardStack.add(0, activeTab.url)
            val prev = activeTab.backStack.removeAt(activeTab.backStack.lastIndex)
            activeTab.url = prev; addressText = prev
            canGoBack = activeTab.backStack.isNotEmpty()
            canGoForward = true
            webView?.loadUrl(prev)
        }
    }

    fun goForward() {
        if (activeTab.forwardStack.isNotEmpty()) {
            activeTab.backStack.add(activeTab.url)
            val next = activeTab.forwardStack.removeAt(0)
            activeTab.url = next; addressText = next
            canGoBack = true
            canGoForward = activeTab.forwardStack.isNotEmpty()
            webView?.loadUrl(next)
        }
    }

    fun addTab(url: String = "edge://newtab") {
        val t = BrowserTab(url = url)
        tabs.add(t)
        activeTabId = t.id
        addressText = url
    }

    fun closeTab(tab: BrowserTab) {
        if (tabs.size <= 1) return
        val idx = tabs.indexOf(tab)
        tabs.remove(tab)
        if (activeTabId == tab.id) {
            activeTabId = tabs.getOrElse(idx.coerceAtMost(tabs.lastIndex)) { tabs.last() }.id
        }
    }

    // ── Keyboard handler ──
    fun handleKeyboardInput(text: String) {
        when (keyboardTarget) {
            "address" -> addressText = text
            "find" -> findQuery = text
        }
    }

    fun getCurrentKeyboardText(): String = when (keyboardTarget) {
        "address" -> addressText
        "find" -> findQuery
        else -> addressText
    }

    // ═══════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═══════════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize().background(edgeBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Tab Bar ──
            EdgeTabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                isDark = isDark,
                tabBarBg = tabBarBg,
                borderColor = borderColor,
                onTabSelected = { tab ->
                    activeTabId = tab.id
                    showTabOverview = false
                },
                onTabClosed = { closeTab(it) },
                onNewTab = { addTab() },
                onTabOverview = { showTabOverview = !showTabOverview }
            )

            // ── Navigation Bar ──
            EdgeNavigationBar(
                isDark = isDark,
                navBarBg = navBarBg,
                borderColor = borderColor,
                isLoading = isLoading,
                loadingProgress = loadingProgress,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                addressText = addressText,
                addressBarFocused = addressBarFocused,
                isBookmarked = isBookmarked,
                onBack = { goBack() },
                onForward = { goForward() },
                onRefresh = { if (isLoading) webView?.stopLoading() else webView?.reload() },
                onHome = { navigate("edge://newtab") },
                onAddressChange = { addressText = it; showAddressSuggestions = it.length > 1 },
                onAddressFocus = {
                    addressBarFocused = true
                    if (settings.useBuiltInKeyboard) {
                        keyboardTarget = "address"
                        showBuiltInKeyboard = true
                    }
                },
                onAddressGo = { navigate(addressText) },
                onBookmarkToggle = {
                    val existing = bookmarks.indexOfFirst { it.url == activeTab.url }
                    if (existing >= 0) bookmarks.removeAt(existing)
                    else bookmarks.add(Bookmark(title = activeTab.title, url = activeTab.url, faviconColor = activeTab.faviconColor))
                },
                onMenuOpen = { showMenu = true },
                onBookmarksPanel = { openPanel = if (openPanel == BrowserPanel.BOOKMARKS) BrowserPanel.NONE else BrowserPanel.BOOKMARKS },
                onHistoryPanel = { openPanel = if (openPanel == BrowserPanel.HISTORY) BrowserPanel.NONE else BrowserPanel.HISTORY },
                onDownloadsPanel = { openPanel = if (openPanel == BrowserPanel.DOWNLOADS) BrowserPanel.NONE else BrowserPanel.DOWNLOADS },
                onSettingsPanel = { openPanel = if (openPanel == BrowserPanel.SETTINGS) BrowserPanel.NONE else BrowserPanel.SETTINGS },
                onFindInPage = {
                    showFindBar = !showFindBar
                    if (showFindBar && settings.useBuiltInKeyboard) {
                        keyboardTarget = "find"
                        showBuiltInKeyboard = true
                    }
                }
            )

            // ── Bookmark Bar ──
            if (settings.showBookmarksBar && bookmarkBarItems.isNotEmpty()) {
                BookmarksBar(
                    bookmarks = bookmarkBarItems,
                    isDark = isDark,
                    navBarBg = navBarBg,
                    borderColor = borderColor,
                    onBookmarkClick = { navigate(it.url) }
                )
            }

            // ── Find in Page Bar ──
            AnimatedVisibility(
                visible = showFindBar,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                FindInPageBar(
                    query = findQuery,
                    isDark = isDark,
                    onQueryChange = { findQuery = it },
                    onFocused = {
                        if (settings.useBuiltInKeyboard) {
                            keyboardTarget = "find"
                            showBuiltInKeyboard = true
                        }
                    },
                    onFindNext = { webView?.findNext(true) },
                    onFindPrev = { webView?.findNext(false) },
                    onClose = { showFindBar = false; findQuery = ""; showBuiltInKeyboard = false }
                )
            }

            // ── Main content row ──
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // ── Side panel ──
                AnimatedVisibility(
                    visible = openPanel != BrowserPanel.NONE,
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                ) {
                    SidePanel(
                        panel = openPanel,
                        isDark = isDark,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        bookmarks = bookmarks,
                        history = history,
                        downloads = downloads,
                        settings = settings,
                        onSettingsChange = { settings = it },
                        onNavigate = { url -> navigate(url); openPanel = BrowserPanel.NONE },
                        onClose = { openPanel = BrowserPanel.NONE }
                    )
                }

                // ── Content area ──
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (showTabOverview) {
                        TabOverviewGrid(
                            tabs = tabs,
                            activeTabId = activeTabId,
                            isDark = isDark,
                            onSelectTab = { tab ->
                                activeTabId = tab.id
                                showTabOverview = false
                            },
                            onCloseTab = { closeTab(it) },
                            onNewTab = { addTab(); showTabOverview = false }
                        )
                    } else if (activeTab.url == "edge://newtab" || activeTab.url.isBlank()) {
                        EdgeNewTabPage(
                            isDark = isDark,
                            settings = settings,
                            bookmarks = bookmarks,
                            history = history,
                            onLinkClicked = { navigate(it) },
                            onSearchFocused = {
                                if (settings.useBuiltInKeyboard) {
                                    keyboardTarget = "address"
                                    showBuiltInKeyboard = true
                                }
                            }
                        )
                    } else {
                        EdgeWebView(
                            url = activeTab.url,
                            isDark = isDark,
                            settings = settings,
                            onWebViewCreated = { wv ->
                                webView = wv
                                if (activeTab.url != "edge://newtab") wv.loadUrl(activeTab.url)
                            },
                            onPageStarted = { url ->
                                isLoading = true; loadingProgress = 0f
                                activeTab.isLoading = true
                            },
                            onProgressChanged = { p ->
                                loadingProgress = p / 100f
                                activeTab.loadProgress = p / 100f
                            },
                            onPageFinished = { url ->
                                isLoading = false
                                activeTab.isLoading = false
                                canGoBack = webView?.canGoBack() == true || activeTab.backStack.isNotEmpty()
                                canGoForward = webView?.canGoForward() == true || activeTab.forwardStack.isNotEmpty()
                                if (url != null && url != "about:blank") {
                                    history.add(0, HistoryEntry(title = activeTab.title, url = url, faviconColor = activeTab.faviconColor))
                                    if (history.size > 500) history.removeAt(history.lastIndex)
                                }
                            },
                            onTitleChanged = { title -> activeTab.title = title },
                            onUrlChanged = { url ->
                                if (url != "about:blank") {
                                    activeTab.url = url
                                    addressText = url
                                }
                            },
                            onDownloadStart = { url, userAgent, contentDisposition, mimetype, contentLength ->
                                val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                val dlItem = DownloadItem(fileName = fileName, url = url, mimeType = mimetype, fileSize = contentLength)
                                downloads.add(0, dlItem)
                                try {
                                    val req = DownloadManager.Request(Uri.parse(url))
                                        .setMimeType(mimetype)
                                        .addRequestHeader("User-Agent", userAgent)
                                        .setTitle(fileName)
                                        .setDescription("Downloading...")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.enqueue(req)
                                    dlItem.status = DownloadStatus.COMPLETED
                                } catch (e: Exception) {
                                    dlItem.status = DownloadStatus.FAILED
                                }
                                openPanel = BrowserPanel.DOWNLOADS
                            }
                        )
                    }

                    // ── Address suggestions overlay ──
                    if (showAddressSuggestions && addressBarFocused && addressText.length > 1) {
                        AddressSuggestionsDropdown(
                            query = addressText,
                            history = history,
                            bookmarks = bookmarks,
                            isDark = isDark,
                            onSuggestionClick = { navigate(it) },
                            onDismiss = { showAddressSuggestions = false }
                        )
                    }
                }
            }

            // ── Built-in Floating Keyboard ──
            AnimatedVisibility(
                visible = showBuiltInKeyboard && settings.useBuiltInKeyboard,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FloatingKeyboard(
                    currentText = getCurrentKeyboardText(),
                    onTextChange = { text -> handleKeyboardInput(text) },
                    onDismiss = {
                        showBuiltInKeyboard = false
                        if (keyboardTarget == "address") {
                            navigate(addressText)
                        }
                    },
                    isDark = isDark
                )
            }
        }

        // ── Three-dot menu ──
        if (showMenu) {
            EdgeContextMenu(
                isDark = isDark,
                surfaceColor = surfaceColor,
                textColor = textColor,
                isBookmarked = isBookmarked,
                onNewTab = { addTab(); showMenu = false },
                onNewPrivateTab = { addTab("edge://newtab"); showMenu = false },
                onBookmarks = { openPanel = BrowserPanel.BOOKMARKS; showMenu = false },
                onHistory = { openPanel = BrowserPanel.HISTORY; showMenu = false },
                onDownloads = { openPanel = BrowserPanel.DOWNLOADS; showMenu = false },
                onSettings = { openPanel = BrowserPanel.SETTINGS; showMenu = false },
                onFindInPage = { showFindBar = true; showMenu = false },
                onPrint = { showMenu = false },
                onZoomIn = { webView?.setInitialScale((settings.defaultZoom + 10).coerceAtMost(200)); showMenu = false },
                onZoomOut = { webView?.setInitialScale((settings.defaultZoom - 10).coerceAtLeast(50)); showMenu = false },
                onAddBookmark = {
                    if (!isBookmarked) bookmarks.add(Bookmark(title = activeTab.title, url = activeTab.url))
                    showMenu = false
                },
                onShare = { showMenu = false },
                onDismiss = { showMenu = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TAB BAR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EdgeTabBar(
    tabs: List<BrowserTab>,
    activeTabId: String,
    isDark: Boolean,
    tabBarBg: Color,
    borderColor: Color,
    onTabSelected: (BrowserTab) -> Unit,
    onTabClosed: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onTabOverview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(tabBarBg)
            .border(BorderStroke(0.5.dp, borderColor), shape = RectangleShape),
        verticalAlignment = Alignment.Bottom
    ) {
        // Tabs scrollable
        LazyRow(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                EdgeTabItem(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    isDark = isDark,
                    onSelect = { onTabSelected(tab) },
                    onClose = { onTabClosed(tab) }
                )
            }
        }

        // New tab button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { onNewTab() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null,
                tint = if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444),
                modifier = Modifier.size(14.dp))
        }

        // Tab count/overview button
        Box(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onTabOverview() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.5.dp, if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${tabs.size}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
private fun EdgeTabItem(
    tab: BrowserTab,
    isActive: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val activeBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
    val bg = if (isActive) activeBg else Color.Transparent
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF0078D4)

    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .background(bg)
            .then(
                if (isActive) Modifier.drawBehind {
                    drawRect(accentBlue, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
                } else Modifier
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Favicon
            if (tab.isLoading) {
                val infiniteTransition = rememberInfiniteTransition(label = "tab_loading")
                val rotation by infiniteTransition.animateFloat(0f, 360f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "rot")
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = rotation },
                    strokeWidth = 1.5.dp, color = accentBlue
                )
            } else {
                Box(modifier = Modifier.size(12.dp).background(tab.faviconColor, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(tab.title.firstOrNull()?.uppercase() ?: "N",
                        fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(tab.title, color = textColor, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (tab.isPinned) {
                Icon(Icons.Default.PushPin, null, tint = accentBlue, modifier = Modifier.size(8.dp))
            }
            Box(modifier = Modifier.size(16.dp).clip(CircleShape)
                .clickable { onClose() }.background(Color.Transparent),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, null,
                    tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(9.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// NAVIGATION BAR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EdgeNavigationBar(
    isDark: Boolean,
    navBarBg: Color,
    borderColor: Color,
    isLoading: Boolean,
    loadingProgress: Float,
    canGoBack: Boolean,
    canGoForward: Boolean,
    addressText: String,
    addressBarFocused: Boolean,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onAddressChange: (String) -> Unit,
    onAddressFocus: () -> Unit,
    onAddressGo: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onMenuOpen: () -> Unit,
    onBookmarksPanel: () -> Unit,
    onHistoryPanel: () -> Unit,
    onDownloadsPanel: () -> Unit,
    onSettingsPanel: () -> Unit,
    onFindInPage: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF202020)
    val iconColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555)
    val addrBg = if (isDark) Color(0xFF383838) else Color.White
    val accentBlue = Color(0xFF0078D4)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(navBarBg)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Back
            NavBtn(Icons.Default.ArrowBack, enabled = canGoBack, tint = iconColor, onClick = onBack)
            // Forward
            NavBtn(Icons.Default.ArrowForward, enabled = canGoForward, tint = iconColor, onClick = onForward)
            // Refresh/Stop
            NavBtn(
                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                tint = iconColor, onClick = onRefresh
            )
            // Home
            NavBtn(Icons.Default.Home, tint = iconColor, onClick = onHome)

            Spacer(Modifier.width(4.dp))

            // Address bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(addrBg)
                    .border(
                        1.dp,
                        if (addressBarFocused) accentBlue else borderColor,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onAddressFocus() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lock/site icon
                    Icon(
                        if (addressText.startsWith("https")) Icons.Default.Lock else Icons.Default.LockOpen,
                        null, tint = if (addressText.startsWith("https")) Color(0xFF107C10) else Color(0xFF888888),
                        modifier = Modifier.size(10.dp)
                    )
                    BasicTextField(
                        value = addressText,
                        onValueChange = onAddressChange,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = textColor, fontSize = 12.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(accentBlue),
                        decorationBox = { inner ->
                            if (addressText.isEmpty()) Text("Search or enter address", color = Color(0xFF888888), fontSize = 12.sp)
                            inner()
                        }
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = accentBlue)
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // Reading view
            NavBtn(Icons.Default.Article, tint = iconColor, onClick = {})
            // Bookmark star
            NavBtn(
                if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                tint = if (isBookmarked) Color(0xFFFFD700) else iconColor,
                onClick = onBookmarkToggle
            )
            // Bookmarks panel
            NavBtn(Icons.Default.BookmarkBorder, tint = iconColor, onClick = onBookmarksPanel)
            // History
            NavBtn(Icons.Default.History, tint = iconColor, onClick = onHistoryPanel)
            // Downloads
            NavBtn(Icons.Default.Download, tint = iconColor, onClick = onDownloadsPanel)
            // Settings
            NavBtn(Icons.Default.Settings, tint = iconColor, onClick = onSettingsPanel)
            // More
            NavBtn(Icons.Default.MoreVert, tint = iconColor, onClick = onMenuOpen)
        }

        // Loading bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accentBlue,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun NavBtn(
    icon: ImageVector,
    enabled: Boolean = true,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null,
            tint = if (enabled) tint else tint.copy(alpha = 0.25f),
            modifier = Modifier.size(14.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BOOKMARKS BAR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BookmarksBar(
    bookmarks: List<Bookmark>,
    isDark: Boolean,
    navBarBg: Color,
    borderColor: Color,
    onBookmarkClick: (Bookmark) -> Unit
) {
    val textColor = if (isDark) Color(0xFFCCCCCC) else Color(0xFF333333)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(navBarBg)
            .border(BorderStroke(0.5.dp, borderColor), RectangleShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        bookmarks.take(12).forEach { bm ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onBookmarkClick(bm) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).background(bm.faviconColor, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(bm.title.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 6.sp, color = Color.White)
                }
                Text(bm.title, fontSize = 10.sp, color = textColor, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// FIND IN PAGE BAR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun FindInPageBar(
    query: String,
    isDark: Boolean,
    onQueryChange: (String) -> Unit,
    onFocused: () -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onClose: () -> Unit
) {
    val bg = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0)
    val textColor = if (isDark) Color.White else Color.Black
    val accentBlue = Color(0xFF0078D4)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).clickable { onFocused() },
            textStyle = TextStyle(color = textColor, fontSize = 12.sp),
            singleLine = true,
            cursorBrush = SolidColor(accentBlue),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Find in page...", color = Color(0xFF888888), fontSize = 12.sp)
                inner()
            }
        )
        NavBtn(Icons.Default.KeyboardArrowUp, tint = Color(0xFF888888), onClick = onFindPrev)
        NavBtn(Icons.Default.KeyboardArrowDown, tint = Color(0xFF888888), onClick = onFindNext)
        NavBtn(Icons.Default.Close, tint = Color(0xFF888888), onClick = onClose)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ADDRESS SUGGESTIONS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AddressSuggestionsDropdown(
    query: String,
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    isDark: Boolean,
    onSuggestionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bg = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val accentBlue = Color(0xFF0078D4)

    val historyMatches = history.filter { it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }.take(4)
    val bookmarkMatches = bookmarks.filter { it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }.take(3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Search suggestion
                SuggestionRow(
                    icon = Icons.Default.Search,
                    iconTint = Color(0xFF888888),
                    text = "Search for \"$query\"",
                    textColor = textColor,
                    onClick = { onSuggestionClick("https://www.google.com/search?q=${Uri.encode(query)}") }
                )

                if (bookmarkMatches.isNotEmpty()) {
                    Text("Bookmarks", fontSize = 9.sp, color = accentBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                    bookmarkMatches.forEach { bm ->
                        SuggestionRow(
                            icon = Icons.Default.Star, iconTint = Color(0xFFFFD700),
                            text = bm.title, subText = bm.url, textColor = textColor,
                            onClick = { onSuggestionClick(bm.url) }
                        )
                    }
                }
                if (historyMatches.isNotEmpty()) {
                    Text("History", fontSize = 9.sp, color = accentBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                    historyMatches.forEach { h ->
                        SuggestionRow(
                            icon = Icons.Default.History, iconTint = Color(0xFF888888),
                            text = h.title, subText = h.url, textColor = textColor,
                            onClick = { onSuggestionClick(h.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    subText: String? = null,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontSize = 12.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subText != null) {
                Text(subText, fontSize = 10.sp, color = textColor.copy(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SIDE PANEL
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SidePanel(
    panel: BrowserPanel,
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    borderColor: Color,
    bookmarks: MutableList<Bookmark>,
    history: MutableList<HistoryEntry>,
    downloads: MutableList<DownloadItem>,
    settings: BrowserSettings,
    onSettingsChange: (BrowserSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val accentBlue = Color(0xFF0078D4)
    val panelTitle = when (panel) {
        BrowserPanel.BOOKMARKS -> "Bookmarks"
        BrowserPanel.HISTORY -> "History"
        BrowserPanel.DOWNLOADS -> "Downloads"
        BrowserPanel.SETTINGS -> "Settings"
        BrowserPanel.EXTENSIONS -> "Extensions"
        BrowserPanel.COLLECTIONS -> "Collections"
        else -> ""
    }

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(surfaceColor)
            .border(BorderStroke(0.5.dp, borderColor), RectangleShape)
    ) {
        // Panel header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(if (isDark) Color(0xFF272727) else Color(0xFFE8E8E8))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(panelTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = textColor, modifier = Modifier.weight(1f))
            NavBtn(Icons.Default.Close, tint = textColor.copy(0.6f), onClick = onClose)
        }

        Divider(color = borderColor, thickness = 0.5.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            when (panel) {
                BrowserPanel.BOOKMARKS -> BookmarksPanel(bookmarks, isDark, textColor, accentBlue, onNavigate)
                BrowserPanel.HISTORY -> HistoryPanel(history, isDark, textColor, accentBlue,
                    onNavigate = onNavigate, onClear = { history.clear() })
                BrowserPanel.DOWNLOADS -> DownloadsPanel(downloads, isDark, textColor, accentBlue)
                BrowserPanel.SETTINGS -> SettingsPanel(settings, isDark, textColor, accentBlue, onSettingsChange)
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Coming soon", color = textColor.copy(0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BookmarksPanel(
    bookmarks: MutableList<Bookmark>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onNavigate: (String) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BookmarkBorder, null, tint = textColor.copy(0.3f), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("No bookmarks yet", color = textColor.copy(0.5f), fontSize = 12.sp)
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(bookmarks) { bm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigate(bm.url) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(20.dp).background(bm.faviconColor, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(bm.title.firstOrNull()?.uppercase() ?: "?", fontSize = 10.sp, color = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(bm.title, fontSize = 11.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(bm.url, fontSize = 9.sp, color = textColor.copy(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { bookmarks.remove(bm) }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = textColor.copy(0.4f), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryPanel(
    history: List<HistoryEntry>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onNavigate: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClear, contentPadding = PaddingValues(4.dp)) {
                    Text("Clear all", fontSize = 10.sp, color = accentBlue)
                }
            }
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history", color = textColor.copy(0.5f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                items(history) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigate(entry.url) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(18.dp).background(entry.faviconColor, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(entry.title.firstOrNull()?.uppercase() ?: "?", fontSize = 9.sp, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, fontSize = 10.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sdf.format(Date(entry.visitedAt)), fontSize = 8.sp, color = textColor.copy(0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsPanel(
    downloads: MutableList<DownloadItem>,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color
) {
    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, null, tint = textColor.copy(0.3f), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("No downloads", color = textColor.copy(0.5f), fontSize = 12.sp)
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(downloads) { dl ->
            val bg = if (isDark) Color(0xFF333333) else Color(0xFFF5F5F5)
            Card(shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bg)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            when (dl.status) {
                                DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                                DownloadStatus.FAILED -> Icons.Default.Error
                                DownloadStatus.PAUSED -> Icons.Default.Pause
                                else -> Icons.Default.Downloading
                            },
                            null,
                            tint = when (dl.status) {
                                DownloadStatus.COMPLETED -> Color(0xFF107C10)
                                DownloadStatus.FAILED -> Color(0xFFD13438)
                                else -> accentBlue
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(dl.fileName, fontSize = 11.sp, color = textColor,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                    }
                    if (dl.status == DownloadStatus.DOWNLOADING) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { dl.progress },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = accentBlue,
                            trackColor = if (isDark) Color(0xFF555555) else Color(0xFFDDDDDD)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            dl.fileSize > 0 -> "${dl.fileSize / 1024 / 1024} MB · ${dl.status.name.lowercase().replaceFirstChar { it.uppercase() }}"
                            else -> dl.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        },
                        fontSize = 9.sp, color = textColor.copy(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: BrowserSettings,
    isDark: Boolean,
    textColor: Color,
    accentBlue: Color,
    onChange: (BrowserSettings) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

        item {
            SettingsSectionHeader("Keyboard", textColor)
        }
        item {
            SettingsToggleRow(
                title = "Built-in floating keyboard",
                subtitle = "Tiny keyboard stays visible, avoids Android keyboard covering address bar",
                checked = settings.useBuiltInKeyboard,
                textColor = textColor,
                accentBlue = accentBlue,
                onToggle = { onChange(settings.copy(useBuiltInKeyboard = it)) }
            )
        }

        item { SettingsSectionHeader("General", textColor) }
        item {
            SettingsToggleRow("Show bookmarks bar", null, settings.showBookmarksBar, textColor, accentBlue) {
                onChange(settings.copy(showBookmarksBar = it))
            }
        }
        item {
            SettingsToggleRow("JavaScript", null, settings.javaScriptEnabled, textColor, accentBlue) {
                onChange(settings.copy(javaScriptEnabled = it))
            }
        }
        item {
            SettingsToggleRow("Show images", null, settings.showImages, textColor, accentBlue) {
                onChange(settings.copy(showImages = it))
            }
        }
        item {
            SettingsToggleRow("Save cookies", null, settings.saveCookies, textColor, accentBlue) {
                onChange(settings.copy(saveCookies = it))
            }
        }

        item { SettingsSectionHeader("Privacy & Security", textColor) }
        item {
            SettingsToggleRow("Ad blocker", "Block intrusive advertisements", settings.adBlockEnabled, textColor, accentBlue) {
                onChange(settings.copy(adBlockEnabled = it))
            }
        }
        item {
            SettingsToggleRow("Tracking protection", "Prevent cross-site tracking", settings.trackingProtection, textColor, accentBlue) {
                onChange(settings.copy(trackingProtection = it))
            }
        }
        item {
            SettingsToggleRow("Block pop-ups", null, settings.popupBlocker, textColor, accentBlue) {
                onChange(settings.copy(popupBlocker = it))
            }
        }

        item { SettingsSectionHeader("Search Engine", textColor) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                SearchEngine.values().forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onChange(settings.copy(searchEngine = engine)) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.searchEngine == engine,
                            onClick = { onChange(settings.copy(searchEngine = engine)) },
                            modifier = Modifier.size(16.dp),
                            colors = RadioButtonDefaults.colors(selectedColor = accentBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, fontSize = 12.sp, color = textColor)
                    }
                }
            }
        }

        item { SettingsSectionHeader("Permissions", textColor) }
        item {
            SettingsToggleRow("Location access", null, settings.locationAccess, textColor, accentBlue) {
                onChange(settings.copy(locationAccess = it))
            }
        }
        item {
            SettingsToggleRow("Camera access", null, settings.cameraAccess, textColor, accentBlue) {
                onChange(settings.copy(cameraAccess = it))
            }
        }
        item {
            SettingsToggleRow("Microphone access", null, settings.microphoneAccess, textColor, accentBlue) {
                onChange(settings.copy(microphoneAccess = it))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, textColor: Color) {
    Text(
        title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0078D4),
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsToggleRow(
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
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 9.sp, color = textColor.copy(0.5f), lineHeight = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.65f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentBlue,
                uncheckedTrackColor = Color(0xFF888888)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CONTEXT MENU
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EdgeContextMenu(
    isDark: Boolean,
    surfaceColor: Color,
    textColor: Color,
    isBookmarked: Boolean,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onFindInPage: () -> Unit,
    onPrint: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onAddBookmark: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(top = 72.dp, end = 8.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MenuRow(Icons.Default.Add, "New tab", textColor) { onNewTab(); onDismiss() }
                MenuRow(Icons.Default.Security, "New private tab", textColor) { onNewPrivateTab(); onDismiss() }
                Divider(color = if (isDark) Color(0x20FFFFFF) else Color(0x20000000), thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                MenuRow(
                    if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    if (isBookmarked) "Bookmarked" else "Add bookmark",
                    textColor
                ) { onAddBookmark(); onDismiss() }
                MenuRow(Icons.Default.Share, "Share", textColor) { onShare(); onDismiss() }
                MenuRow(Icons.Default.FindInPage, "Find in page", textColor) { onFindInPage(); onDismiss() }
                Divider(color = if (isDark) Color(0x20FFFFFF) else Color(0x20000000), thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                // Zoom row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ZoomIn, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    Text("Zoom", fontSize = 12.sp, color = textColor, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                }
                Divider(color = if (isDark) Color(0x20FFFFFF) else Color(0x20000000), thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                MenuRow(Icons.Default.BookmarkBorder, "Bookmarks", textColor) { onBookmarks(); onDismiss() }
                MenuRow(Icons.Default.History, "History", textColor) { onHistory(); onDismiss() }
                MenuRow(Icons.Default.Download, "Downloads", textColor) { onDownloads(); onDismiss() }
                MenuRow(Icons.Default.Print, "Print", textColor) { onPrint(); onDismiss() }
                Divider(color = if (isDark) Color(0x20FFFFFF) else Color(0x20000000), thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                MenuRow(Icons.Default.Settings, "Settings", textColor) { onSettings(); onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = textColor.copy(0.7f), modifier = Modifier.size(14.dp))
        Text(label, fontSize = 12.sp, color = textColor)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TAB OVERVIEW GRID
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun TabOverviewGrid(
    tabs: List<BrowserTab>,
    activeTabId: String,
    isDark: Boolean,
    onSelectTab: (BrowserTab) -> Unit,
    onCloseTab: (BrowserTab) -> Unit,
    onNewTab: () -> Unit
) {
    val bg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val cardBg = if (isDark) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF202020)
    val accentBlue = Color(0xFF0078D4)

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${tabs.size} tabs", fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 14.sp)
                TextButton(onClick = onNewTab) {
                    Icon(Icons.Default.Add, null, tint = accentBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New tab", fontSize = 12.sp, color = accentBlue)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable { onSelectTab(tab) }
                            .border(
                                width = if (tab.id == activeTabId) 2.dp else 0.dp,
                                color = if (tab.id == activeTabId) accentBlue else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Tab preview (colored placeholder)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(tab.faviconColor.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Language, null,
                                    tint = tab.faviconColor.copy(0.5f),
                                    modifier = Modifier.size(28.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(tab.faviconColor, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(tab.title, fontSize = 9.sp, color = textColor,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseTab(tab) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null,
                                        tint = textColor.copy(0.5f), modifier = Modifier.size(10.dp))
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
// NEW TAB PAGE
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EdgeNewTabPage(
    isDark: Boolean,
    settings: BrowserSettings,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onLinkClicked: (String) -> Unit,
    onSearchFocused: () -> Unit
) {
    val bg = if (isDark) Color(0xFF202020) else Color(0xFFF5F5F7)
    val textColor = if (isDark) Color(0xFFE8E8E8) else Color(0xFF202020)
    val surfaceColor = if (isDark) Color(0xFF2D2D2D) else Color.White
    val accentBlue = Color(0xFF0078D4)

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Edge logo
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF0078D4), Color(0xFF50E6FF))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("e", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
                Text("Microsoft Edge", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = textColor)
            }

            Spacer(Modifier.height(16.dp))

            // Search box
            var searchQuery by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(surfaceColor)
                    .border(1.dp, Color(0x20888888), RoundedCornerShape(19.dp))
                    .clickable { onSearchFocused() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).clickable { onSearchFocused() },
                    textStyle = TextStyle(color = textColor, fontSize = 13.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(accentBlue),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Search or enter web address", color = Color(0xFF888888), fontSize = 13.sp)
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                            .background(accentBlue)
                            .clickable {
                                onLinkClicked("${settings.searchEngine.url}${Uri.encode(searchQuery)}")
                                searchQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Quick links
            Text("Quick links", fontSize = 11.sp, color = textColor.copy(0.6f),
                modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp))
            Spacer(Modifier.height(8.dp))

            val visibleLinks = quickLinks.take(if (isDark) 8 else 10)
            LazyRow(
                modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleLinks) { (name, url, color) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(52.dp)
                            .clickable { onLinkClicked(url) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(color, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.first().uppercase(),
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(name, fontSize = 9.sp, color = textColor.copy(0.7f), maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }

            // Recent bookmarks
            if (bookmarks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Bookmarks", fontSize = 11.sp, color = textColor.copy(0.6f),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp))
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    bookmarks.take(5).forEach { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(surfaceColor.copy(0.5f))
                                .clickable { onLinkClicked(bm.url) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(18.dp).background(bm.faviconColor, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(bm.title.firstOrNull()?.uppercase() ?: "?", fontSize = 9.sp, color = Color.White)
                            }
                            Text(bm.title, fontSize = 11.sp, color = textColor, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(0.3f), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // News
            Text("News & Interests", fontSize = 11.sp, color = textColor.copy(0.6f),
                modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp))
            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sampleNews) { (title, source, color) ->
                    Card(
                        modifier = Modifier.width(150.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(color.copy(0.2f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Article, null, tint = color.copy(0.6f), modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(title, fontSize = 9.sp, color = textColor, maxLines = 3,
                                overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(source, fontSize = 8.sp, color = textColor.copy(0.4f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Weather / info card
            Card(
                modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0078D4).copy(if (isDark) 0.15f else 0.08f)
                )
            ) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    Column {
                        Text("Good to browse!", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Your browsing is protected · Tracking blocked",
                            fontSize = 9.sp, color = textColor.copy(0.6f))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// WEBVIEW
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EdgeWebView(
    url: String,
    isDark: Boolean,
    settings: BrowserSettings,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDownloadStart: (String, String, String, String, Long) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                with(this.settings) {
                    javaScriptEnabled = settings.javaScriptEnabled
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowFileAccess = true
                    allowContentAccess = true
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = !settings.popupBlocker
                    blockNetworkImage = !settings.showImages
                    loadsImagesAutomatically = settings.showImages
                    setSupportZoom(true)
                    textZoom = settings.fontSize
                    mediaPlaybackRequiresUserGesture = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 EdgA/120.0.0.0"
                }

                if (isDark) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        this.settings.forceDark = WebSettings.FORCE_DARK_AUTO
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted(url)
                        url?.let { onUrlChanged(it) }
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(url)
                        url?.let { onUrlChanged(it) }
                        title?.let { onTitleChanged(it) }
                        // Dark mode injection
                        if (isDark) {
                            view?.evaluateJavascript("""
                                (function() {
                                  var style = document.createElement('style');
                                  style.type = 'text/css';
                                  document.head.appendChild(style);
                                })();
                            """.trimIndent(), null)
                        }
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString() ?: return false
                        if (reqUrl.startsWith("http") || reqUrl.startsWith("https")) return false
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(reqUrl))
                            ctx.startActivity(intent)
                        } catch (e: Exception) { /* ignore */ }
                        return true
                    }
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            view?.loadDataWithBaseURL(null, buildErrorPage(error?.description?.toString() ?: "Unknown error", isDark), "text/html", "UTF-8", null)
                        }
                    }
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                        handler?.cancel()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        title?.let { onTitleChanged(it) }
                    }
                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        result?.confirm()
                        return true
                    }
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean = true
                }

                setDownloadListener { dlUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadStart(dlUrl, userAgent, contentDisposition, mimetype, contentLength)
                }

                CookieManager.getInstance().apply {
                    setAcceptCookie(settings.saveCookies)
                    setAcceptThirdPartyCookies(this@apply as WebView?, false)
                }

                onWebViewCreated(this)
            }
        },
        update = { wv ->
            wv.settings.javaScriptEnabled = settings.javaScriptEnabled
            wv.settings.loadsImagesAutomatically = settings.showImages
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildErrorPage(error: String, isDark: Boolean): String {
    val bg = if (isDark) "#1A1A1A" else "#F5F5F5"
    val text = if (isDark) "#E8E8E8" else "#202020"
    val sub = if (isDark) "#888888" else "#666666"
    return """
        <!DOCTYPE html>
        <html>
        <head><meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { background: $bg; color: $text; font-family: -apple-system, sans-serif;
                 display: flex; flex-direction: column; align-items: center; justify-content: center;
                 height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }
          .icon { font-size: 48px; margin-bottom: 16px; }
          h2 { margin: 0 0 8px 0; font-size: 20px; }
          p { color: $sub; text-align: center; font-size: 13px; max-width: 400px; }
        </style></head>
        <body>
          <div class="icon">⚠️</div>
          <h2>This page can't be reached</h2>
          <p>$error</p>
          <p>Check your internet connection or the URL and try again.</p>
        </body></html>
    """.trimIndent()
}
