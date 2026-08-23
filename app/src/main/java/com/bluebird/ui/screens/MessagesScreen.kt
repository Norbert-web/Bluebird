package com.bluebird.ui.screens

// ============================================================================================
// NOTE: The composable is kept named `MessagesScreen` on purpose so existing navigation/routing
// that points at this screen keeps working. Functionally this is no longer SMS — it is the
// **Bluebird Store**: a desktop-style app store for small HTML/CSS/JS apps built specifically
// for Bluebird. Apps are downloaded (or bundled), installed into a real per-app directory under
// this app's private storage, can be pinned to the desktop (queryable by other screens), and run
// in a sandboxed WebView with a native "Bluebird" JS bridge — a small on-device SDK that no other
// simulator ships. This is the differentiator: Bluebird isn't just skinned, it's extensible.
// ============================================================================================

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bluebird.ui.theme.Win11Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream

// ============================================================================================
// DATA MODEL
// ============================================================================================

/** Per-app classification used for browsing. Kept separate from nav sections (see NavSection). */
enum class AppCategory(val label: String) {
    PRODUCTIVITY("Productivity"),
    UTILITIES("Utilities"),
    GAMES("Games"),
    CREATIVITY("Creativity")
}

/** Left-rail navigation sections. INSTALLED and DEVELOPER are computed views, not app categories. */
private enum class NavSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FEATURED("Featured", Icons.Default.Star),
    PRODUCTIVITY("Productivity", Icons.Default.Work),
    UTILITIES("Utilities", Icons.Default.Build),
    GAMES("Games", Icons.Default.SportsEsports),
    CREATIVITY("Creativity", Icons.Default.Brush),
    INSTALLED("Installed", Icons.Default.CheckCircle),
    DEVELOPER("Developer", Icons.Default.Code)
}

data class BluebirdApp(
    val id: String,
    val name: String,
    val developer: String,
    val shortDescription: String,
    val description: String,
    val category: AppCategory,
    val accentColor: Color,
    val version: String = "1.0.0",
    val sizeKb: Int = 24,
    val rating: Float = 4.5f,
    val featured: Boolean = false,
    /** Null = bundled template written straight to disk. Non-null = downloaded & unzipped. */
    val downloadUrl: String? = null,
    val isSideloaded: Boolean = false
)

data class InstalledEntry(
    val appId: String,
    val installPath: String,
    val installedAt: Long,
    val pinnedToDesktop: Boolean,
    val version: String
)

// ============================================================================================
// BUILT-IN APP CATALOG + TEMPLATES
// Small but genuinely functional HTML/CSS/JS apps, written to disk on "install" and run for
// real inside a WebView. Each one calls the native Bluebird bridge to prove the SDK works.
// ============================================================================================

private object BuiltInApps {

    val catalog = listOf(
        BluebirdApp(
            id = "clock", name = "Bluebird Clock", developer = "Bluebird Labs",
            shortDescription = "A live desktop clock", category = AppCategory.UTILITIES,
            accentColor = Color(0xFF2B579A), sizeKb = 6, rating = 4.7f, featured = true,
            description = "A minimal live clock with date. Demonstrates a background-updating HTML/CSS/JS widget running natively on Bluebird."
        ),
        BluebirdApp(
            id = "calc", name = "Simple Calculator", developer = "Bluebird Labs",
            shortDescription = "A pocket calculator", category = AppCategory.UTILITIES,
            accentColor = Color(0xFF1E7A34), sizeKb = 9, rating = 4.4f,
            description = "A four-function calculator built with a plain HTML grid and vanilla JS — no frameworks."
        ),
        BluebirdApp(
            id = "notes", name = "Quick Notes", developer = "Bluebird Labs",
            shortDescription = "Jot things down, saved locally", category = AppCategory.PRODUCTIVITY,
            accentColor = Color(0xFFB8860B), sizeKb = 8, rating = 4.6f, featured = true,
            description = "A single-page notepad that autosaves to the browser's local storage inside its sandboxed app directory, and toasts through the native Bluebird bridge when you save."
        ),
        BluebirdApp(
            id = "paint", name = "Bluebird Sketch", developer = "Bluebird Labs",
            shortDescription = "Freehand canvas drawing", category = AppCategory.CREATIVITY,
            accentColor = Color(0xFF6A1B9A), sizeKb = 11, rating = 4.3f,
            description = "A finger/mouse-driven drawing canvas with a color palette and clear button, built on the HTML5 canvas element."
        )
    )

    fun templateFor(appId: String): Triple<String, String, String> = when (appId) {
        "clock" -> Triple(CLOCK_HTML, CLOCK_CSS, CLOCK_JS)
        "calc" -> Triple(CALC_HTML, CALC_CSS, CALC_JS)
        "notes" -> Triple(NOTES_HTML, NOTES_CSS, NOTES_JS)
        "paint" -> Triple(PAINT_HTML, PAINT_CSS, PAINT_JS)
        else -> Triple(FALLBACK_HTML, "", "")
    }

    private const val CLOCK_HTML = """<!DOCTYPE html><html><head><link rel="stylesheet" href="style.css"></head>
<body><div id="app"><div id="time">--:--:--</div><div id="date">--</div></div><script src="script.js"></script></body></html>"""
    private const val CLOCK_CSS = """body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;background:#0f1720;font-family:sans-serif;color:#fff}
#app{text-align:center}#time{font-size:56px;font-weight:600;letter-spacing:2px}#date{font-size:16px;opacity:.6;margin-top:8px}"""
    private const val CLOCK_JS = """function tick(){const n=new Date();document.getElementById('time').textContent=n.toLocaleTimeString();
document.getElementById('date').textContent=n.toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric'});}
tick();setInterval(tick,1000);if(window.Bluebird){Bluebird.toast('Clock started');}"""

    private const val CALC_HTML = """<!DOCTYPE html><html><head><link rel="stylesheet" href="style.css"></head>
<body><div id="calc"><input id="disp" readonly value="0"/><div id="keys"></div></div><script src="script.js"></script></body></html>"""
    private const val CALC_CSS = """body{margin:0;background:#1c1c1c;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif}
#calc{width:260px}#disp{width:100%;box-sizing:border-box;font-size:28px;padding:12px;margin-bottom:8px;text-align:right;border:none;border-radius:8px}
#keys{display:grid;grid-template-columns:repeat(4,1fr);gap:6px}
button{padding:16px 0;font-size:18px;border:none;border-radius:8px;background:#333;color:#fff}
button.op{background:#2B579A}button.eq{background:#1E7A34;grid-column:span 2}"""
    private const val CALC_JS = """const keys=['7','8','9','/','4','5','6','*','1','2','3','-','0','.','C','+','='];
const kdiv=document.getElementById('keys');const disp=document.getElementById('disp');let expr='';
keys.forEach(k=>{const b=document.createElement('button');b.textContent=k;
if('/*-+'.includes(k))b.className='op';if(k==='=')b.className='eq';
b.onclick=()=>{if(k==='C'){expr='';}else if(k==='='){try{expr=String(Function('"use strict";return('+expr+')')());}catch(e){expr='Error';}}else{expr+=k;}disp.value=expr||'0';};
kdiv.appendChild(b);});"""

    private const val NOTES_HTML = """<!DOCTYPE html><html><head><link rel="stylesheet" href="style.css"></head>
<body><div id="bar"><span>Quick Notes</span><button id="save">Save</button></div><textarea id="note" placeholder="Type a note..."></textarea><script src="script.js"></script></body></html>"""
    private const val NOTES_CSS = """body{margin:0;font-family:sans-serif;background:#fff;height:100vh;display:flex;flex-direction:column}
#bar{display:flex;justify-content:space-between;align-items:center;padding:10px 14px;background:#B8860B;color:#fff}
#save{border:none;background:#00000030;color:#fff;padding:6px 14px;border-radius:6px}
textarea{flex:1;border:none;outline:none;padding:16px;font-size:15px;resize:none}"""
    private const val NOTES_JS = """const ta=document.getElementById('note');ta.value=localStorage.getItem('bluebird_note')||'';
document.getElementById('save').onclick=()=>{localStorage.setItem('bluebird_note',ta.value);if(window.Bluebird){Bluebird.toast('Note saved');}};"""

    private const val PAINT_HTML = """<!DOCTYPE html><html><head><link rel="stylesheet" href="style.css"></head>
<body><div id="bar"><div id="palette"></div><button id="clear">Clear</button></div><canvas id="c"></canvas><script src="script.js"></script></body></html>"""
    private const val PAINT_CSS = """body{margin:0;font-family:sans-serif;height:100vh;display:flex;flex-direction:column;background:#fafafa}
#bar{display:flex;align-items:center;gap:8px;padding:8px;background:#eee}
#palette span{display:inline-block;width:22px;height:22px;border-radius:50%;margin-right:6px;cursor:pointer;border:2px solid #fff}
canvas{flex:1;touch-action:none}#clear{margin-left:auto;padding:6px 12px;border:none;border-radius:6px;background:#6A1B9A;color:#fff}"""
    private const val PAINT_JS = """const c=document.getElementById('c');const ctx=c.getContext('2d');
function resize(){c.width=c.clientWidth;c.height=c.clientHeight;}resize();window.onresize=resize;
let color='#000',drawing=false;const colors=['#000','#c00000','#2B579A','#1E7A34','#B8860B'];
const pal=document.getElementById('palette');colors.forEach(col=>{const s=document.createElement('span');s.style.background=col;s.onclick=()=>color=col;pal.appendChild(s);});
function pos(e){const r=c.getBoundingClientRect();const t=e.touches?e.touches[0]:e;return{x:t.clientX-r.left,y:t.clientY-r.top};}
function start(e){drawing=true;const p=pos(e);ctx.beginPath();ctx.moveTo(p.x,p.y);}
function move(e){if(!drawing)return;const p=pos(e);ctx.strokeStyle=color;ctx.lineWidth=3;ctx.lineCap='round';ctx.lineTo(p.x,p.y);ctx.stroke();e.preventDefault();}
function end(){drawing=false;}
c.addEventListener('mousedown',start);c.addEventListener('mousemove',move);window.addEventListener('mouseup',end);
c.addEventListener('touchstart',start);c.addEventListener('touchmove',move);c.addEventListener('touchend',end);
document.getElementById('clear').onclick=()=>ctx.clearRect(0,0,c.width,c.height);"""

    private const val FALLBACK_HTML = """<!DOCTYPE html><html><body style="font-family:sans-serif;padding:24px">
<h3>App content missing</h3><p>This app's files could not be found.</p></body></html>"""
}

// ============================================================================================
// BLUEBIRD JS BRIDGE — the tiny native SDK exposed to every installed web app as `window.Bluebird`
// ============================================================================================

private class BluebirdBridge(private val context: Context, private val onToast: (String) -> Unit) {
    @JavascriptInterface
    fun toast(message: String) {
        Handler(Looper.getMainLooper()).post { onToast(message) }
    }

    @JavascriptInterface
    fun platformInfo(): String = "Bluebird OS Simulator / Android"
}

// ============================================================================================
// REPOSITORY — install directory, manifest persistence, download+unzip, sideload
// ============================================================================================

private object BluebirdStore {
    private const val MANIFEST_FILE = "manifest.json"

    fun appsRootDir(context: Context): File {
        val dir = File(context.filesDir, "bluebird_apps")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun appDir(context: Context, appId: String): File = File(appsRootDir(context), appId)

    private fun manifestFile(context: Context) = File(appsRootDir(context), MANIFEST_FILE)

    fun loadManifest(context: Context): Pair<MutableList<InstalledEntry>, MutableList<BluebirdApp>> {
        val file = manifestFile(context)
        if (!file.exists()) return mutableListOf<InstalledEntry>() to mutableListOf()
        return try {
            val root = JSONObject(file.readText())
            val installed = mutableListOf<InstalledEntry>()
            val installedArr = root.optJSONArray("installed") ?: JSONArray()
            for (i in 0 until installedArr.length()) {
                val o = installedArr.getJSONObject(i)
                installed.add(
                    InstalledEntry(
                        appId = o.getString("appId"), installPath = o.getString("installPath"),
                        installedAt = o.getLong("installedAt"), pinnedToDesktop = o.optBoolean("pinnedToDesktop", false),
                        version = o.optString("version", "1.0.0")
                    )
                )
            }
            val custom = mutableListOf<BluebirdApp>()
            val customArr = root.optJSONArray("customApps") ?: JSONArray()
            for (i in 0 until customArr.length()) {
                val o = customArr.getJSONObject(i)
                custom.add(
                    BluebirdApp(
                        id = o.getString("id"), name = o.getString("name"), developer = o.optString("developer", "Sideloaded"),
                        shortDescription = o.optString("shortDescription", ""), description = o.optString("description", ""),
                        category = AppCategory.entries.firstOrNull { it.name == o.optString("category") } ?: AppCategory.UTILITIES,
                        accentColor = Color(0xFF546E7A), sizeKb = o.optInt("sizeKb", 16), rating = 0f, isSideloaded = true
                    )
                )
            }
            installed to custom
        } catch (e: Exception) {
            mutableListOf<InstalledEntry>() to mutableListOf()
        }
    }

    fun saveManifest(context: Context, installed: List<InstalledEntry>, custom: List<BluebirdApp>) {
        val root = JSONObject()
        val installedArr = JSONArray()
        installed.forEach { e ->
            installedArr.put(JSONObject().apply {
                put("appId", e.appId); put("installPath", e.installPath)
                put("installedAt", e.installedAt); put("pinnedToDesktop", e.pinnedToDesktop); put("version", e.version)
            })
        }
        val customArr = JSONArray()
        custom.forEach { a ->
            customArr.put(JSONObject().apply {
                put("id", a.id); put("name", a.name); put("developer", a.developer)
                put("shortDescription", a.shortDescription); put("description", a.description)
                put("category", a.category.name); put("sizeKb", a.sizeKb)
            })
        }
        root.put("installed", installedArr); root.put("customApps", customArr)
        manifestFile(context).writeText(root.toString())
    }

    /** Extracts a zip stream into [dir], guarding against zip-slip path traversal. */
    private fun extractZip(input: InputStream, dir: File, totalHint: Int, onProgress: (Float) -> Unit) {
        dir.mkdirs()
        val canonicalRoot = dir.canonicalPath + File.separator
        val zis = ZipInputStream(input)
        var downloaded = 0
        val buffer = ByteArray(8192)
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(dir, entry.name)
            if (!outFile.canonicalPath.startsWith(canonicalRoot)) {
                throw SecurityException("Blocked unsafe zip entry: ${entry.name}")
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                        downloaded += len
                        if (totalHint > 0) onProgress((downloaded.toFloat() / totalHint).coerceIn(0f, 1f))
                    }
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        onProgress(1f)
    }

    private fun writeBundledTemplate(dir: File, appId: String) {
        dir.mkdirs()
        val (html, css, js) = BuiltInApps.templateFor(appId)
        File(dir, "index.html").writeText(html)
        File(dir, "style.css").writeText(css)
        File(dir, "script.js").writeText(js)
    }

    /** Installs [app]: downloads+unzips if it has a URL, otherwise writes its bundled template. */
    suspend fun install(context: Context, app: BluebirdApp, onProgress: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val dir = appDir(context, app.id)
                if (app.downloadUrl != null) {
                    val conn = URL(app.downloadUrl).openConnection() as HttpURLConnection
                    conn.connect()
                    val total = conn.contentLength
                    conn.inputStream.use { extractZip(it, dir, total, onProgress) }
                } else {
                    onProgress(0.35f)
                    writeBundledTemplate(dir, app.id)
                    onProgress(1f)
                }
                val (installed, custom) = loadManifest(context)
                val updated = installed.filterNot { it.appId == app.id } +
                    InstalledEntry(app.id, dir.absolutePath, System.currentTimeMillis(), false, app.version)
                saveManifest(context, updated, custom)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Sideloads a developer's own zip (must contain at least index.html) as a new custom app. */
    suspend fun sideload(context: Context, name: String, input: InputStream): Result<BluebirdApp> =
        withContext(Dispatchers.IO) {
            try {
                val id = "custom_" + UUID.randomUUID().toString().take(8)
                val dir = appDir(context, id)
                extractZip(input, dir, -1) {}
                if (!File(dir, "index.html").exists()) {
                    dir.deleteRecursively()
                    return@withContext Result.failure(IllegalArgumentException("Zip must contain an index.html at its root"))
                }
                val app = BluebirdApp(
                    id = id, name = name, developer = "Sideloaded", shortDescription = "Custom app",
                    description = "A developer-installed app running from local files.",
                    category = AppCategory.UTILITIES, accentColor = Color(0xFF546E7A), isSideloaded = true
                )
                val (installed, custom) = loadManifest(context)
                val updatedInstalled = installed + InstalledEntry(id, dir.absolutePath, System.currentTimeMillis(), false, "1.0.0")
                val updatedCustom = custom + app
                saveManifest(context, updatedInstalled, updatedCustom)
                Result.success(app)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun uninstall(context: Context, appId: String) {
        appDir(context, appId).deleteRecursively()
        val (installed, custom) = loadManifest(context)
        saveManifest(context, installed.filterNot { it.appId == appId }, custom.filterNot { it.id == appId })
    }

    fun setPinned(context: Context, appId: String, pinned: Boolean) {
        val (installed, custom) = loadManifest(context)
        saveManifest(context, installed.map { if (it.appId == appId) it.copy(pinnedToDesktop = pinned) else it }, custom)
    }

    /** Public read hook — other screens (e.g. a Desktop/home screen) can call this to list
     *  apps the user has pinned, and build real desktop icons that launch them. */
    fun pinnedApps(context: Context): List<InstalledEntry> = loadManifest(context).first.filter { it.pinnedToDesktop }
}

// ============================================================================================
// MAIN COMPOSABLE
// ============================================================================================

@Composable
fun MessagesScreen(isDark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val railBg = if (isDark) Color(0xFF202020) else Color(0xFFEDEDED)
    val cardBg = if (isDark) Color(0xFF262626) else Color.White

    var section by remember { mutableStateOf(NavSection.FEATURED) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<BluebirdApp?>(null) }
    var runningApp by remember { mutableStateOf<BluebirdApp?>(null) }
    var showSideloadDialog by remember { mutableStateOf(false) }

    val customApps = remember { mutableStateListOf<BluebirdApp>() }
    val installed = remember { mutableStateListOf<InstalledEntry>() }
    val installProgress = remember { mutableStateMapOf<String, Float>() }

    fun refreshManifest() {
        val (inst, custom) = BluebirdStore.loadManifest(context)
        installed.clear(); installed.addAll(inst)
        customApps.clear(); customApps.addAll(custom)
    }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { } ; refreshManifest() }

    fun notify(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    fun isInstalled(appId: String) = installed.any { it.appId == appId }

    fun installApp(app: BluebirdApp) {
        installProgress[app.id] = 0f
        scope.launch {
            val result = BluebirdStore.install(context, app) { p -> installProgress[app.id] = p }
            installProgress.remove(app.id)
            if (result.isSuccess) {
                refreshManifest()
                notify("${app.name} installed")
            } else {
                notify("Install failed: ${result.exceptionOrNull()?.message ?: "unknown error"}")
            }
        }
    }

    fun uninstallApp(app: BluebirdApp) {
        BluebirdStore.uninstall(context, app.id)
        refreshManifest()
        if (selectedApp?.id == app.id) selectedApp = null
        notify("${app.name} uninstalled")
    }

    fun togglePin(app: BluebirdApp) {
        val current = installed.firstOrNull { it.appId == app.id }?.pinnedToDesktop ?: false
        BluebirdStore.setPinned(context, app.id, !current)
        refreshManifest()
        notify(if (!current) "Pinned to desktop" else "Removed from desktop")
    }

    val sideloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Custom App"
        scope.launch {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) { notify("Couldn't read file"); return@launch }
            val result = stream.use { BluebirdStore.sideload(context, name, it) }
            if (result.isSuccess) {
                refreshManifest()
                notify("${name} sideloaded")
            } else {
                notify("Sideload failed: ${result.exceptionOrNull()?.message ?: "unknown error"}")
            }
        }
    }

    val allApps = remember(customApps.toList()) { BuiltInApps.catalog + customApps }

    val visibleApps = remember(section, searchQuery, installed.toList(), customApps.toList()) {
        val base = when (section) {
            NavSection.FEATURED -> allApps.filter { it.featured }
            NavSection.PRODUCTIVITY -> allApps.filter { it.category == AppCategory.PRODUCTIVITY }
            NavSection.UTILITIES -> allApps.filter { it.category == AppCategory.UTILITIES }
            NavSection.GAMES -> allApps.filter { it.category == AppCategory.GAMES }
            NavSection.CREATIVITY -> allApps.filter { it.category == AppCategory.CREATIVITY }
            NavSection.INSTALLED -> allApps.filter { app -> installed.any { it.appId == app.id } }
            NavSection.DEVELOPER -> customApps.toList()
        }
        if (searchQuery.isBlank()) base
        else base.filter { it.name.contains(searchQuery, true) || it.shortDescription.contains(searchQuery, true) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = bgColor) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                runningApp != null -> AppRuntimeView(
                    app = runningApp!!, installDir = BluebirdStore.appDir(context, runningApp!!.id),
                    isDark = isDark, isPinned = installed.firstOrNull { it.appId == runningApp!!.id }?.pinnedToDesktop ?: false,
                    onToast = { notify(it) }, onTogglePin = { togglePin(runningApp!!) },
                    onClose = { runningApp = null }
                )

                selectedApp != null -> AppDetailPane(
                    app = selectedApp!!, isDark = isDark, textColor = textColor, cardBg = cardBg,
                    isInstalled = isInstalled(selectedApp!!.id),
                    isPinned = installed.firstOrNull { it.appId == selectedApp!!.id }?.pinnedToDesktop ?: false,
                    progress = installProgress[selectedApp!!.id],
                    onBack = { selectedApp = null },
                    onInstall = { installApp(selectedApp!!) },
                    onOpen = { runningApp = selectedApp },
                    onUninstall = { uninstallApp(selectedApp!!) },
                    onTogglePin = { togglePin(selectedApp!!) }
                )

                else -> Row(Modifier.fillMaxSize()) {
                    NavRail(
                        section = section, isDark = isDark, textColor = textColor, railBg = railBg,
                        installedCount = installed.size,
                        onSelect = { section = it },
                        onSideload = { showSideloadDialog = true }
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        StoreTopBar(
                            title = section.label, query = searchQuery, textColor = textColor,
                            onQueryChange = { searchQuery = it }
                        )
                        AppGrid(
                            apps = visibleApps, isDark = isDark, textColor = textColor, cardBg = cardBg,
                            isInstalled = { isInstalled(it) }, progressFor = { installProgress[it] },
                            onOpenDetail = { selectedApp = it },
                            onQuickInstall = { installApp(it) },
                            onQuickOpen = { runningApp = it },
                            emptyMessage = when (section) {
                                NavSection.DEVELOPER -> "No sideloaded apps yet. Tap \"Sideload app\" in the sidebar to add one."
                                NavSection.INSTALLED -> "Nothing installed yet — browse a category to find apps."
                                else -> "No apps match your search."
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSideloadDialog) {
        SideloadDialog(
            onDismiss = { showSideloadDialog = false },
            onPickZip = { showSideloadDialog = false; sideloadLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
        )
    }
}

// ============================================================================================
// NAV RAIL
// ============================================================================================

@Composable
private fun NavRail(
    section: NavSection, isDark: Boolean, textColor: Color, railBg: Color, installedCount: Int,
    onSelect: (NavSection) -> Unit, onSideload: () -> Unit
) {
    Column(modifier = Modifier.width(190.dp).fillMaxHeight().background(railBg).padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Icon(Icons.Default.Storefront, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Bluebird Store", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        NavSection.entries.forEach { s ->
            val selected = section == s
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (selected) Win11Colors.AccentBlue.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(s) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(s.icon, null, tint = if (selected) Win11Colors.AccentBlue else textColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(s.label, color = if (selected) Win11Colors.AccentBlue else textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (s == NavSection.INSTALLED && installedCount > 0) {
                    Text("$installedCount", color = textColor.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSideload() }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.UploadFile, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text("Sideload app", color = textColor.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

// ============================================================================================
// TOP BAR
// ============================================================================================

@Composable
private fun StoreTopBar(title: String, query: String, textColor: Color, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.width(240.dp).height(44.dp),
            placeholder = { Text("Search Bluebird Store", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Win11Colors.AccentBlue,
                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                focusedTextColor = textColor, unfocusedTextColor = textColor
            )
        )
    }
}

// ============================================================================================
// APP GRID + CARD
// ============================================================================================

@Composable
private fun AppGrid(
    apps: List<BluebirdApp>, isDark: Boolean, textColor: Color, cardBg: Color,
    isInstalled: (String) -> Boolean, progressFor: (String) -> Float?,
    onOpenDetail: (BluebirdApp) -> Unit, onQuickInstall: (BluebirdApp) -> Unit, onQuickOpen: (BluebirdApp) -> Unit,
    emptyMessage: String
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Apps, null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text(emptyMessage, color = textColor.copy(alpha = 0.4f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(220.dp))
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 190.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps, key = { it.id }) { app ->
            AppCard(
                app = app, textColor = textColor, cardBg = cardBg,
                installed = isInstalled(app.id), progress = progressFor(app.id),
                onClick = { onOpenDetail(app) },
                onQuickAction = { if (isInstalled(app.id)) onQuickOpen(app) else onQuickInstall(app) }
            )
        }
    }
}

@Composable
private fun AppCard(
    app: BluebirdApp, textColor: Color, cardBg: Color, installed: Boolean, progress: Float?,
    onClick: () -> Unit, onQuickAction: () -> Unit
) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(cardBg).clickable { onClick() }.padding(14.dp)
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(app.accentColor), contentAlignment = Alignment.Center) {
            Text(app.name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(app.name, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(app.developer, color = textColor.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(app.shortDescription, color = textColor.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
        Spacer(Modifier.height(10.dp))
        if (progress != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Win11Colors.AccentBlue)
        } else {
            Button(
                onClick = onQuickAction,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (installed) Win11Colors.SuccessGreen else Win11Colors.AccentBlue)
            ) {
                Text(if (installed) "Open" else "Install", fontSize = 12.sp)
            }
        }
    }
}

// ============================================================================================
// APP DETAIL PANE
// ============================================================================================

@Composable
private fun AppDetailPane(
    app: BluebirdApp, isDark: Boolean, textColor: Color, cardBg: Color,
    isInstalled: Boolean, isPinned: Boolean, progress: Float?,
    onBack: () -> Unit, onInstall: () -> Unit, onOpen: () -> Unit, onUninstall: () -> Unit, onTogglePin: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = textColor) }
            Text("Back to store", color = textColor.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(app.accentColor), contentAlignment = Alignment.Center) {
                Text(app.name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(app.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(app.developer, color = textColor.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("★ ${app.rating}", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("${app.sizeKb} KB", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("v${app.version}", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (progress != null) {
            Column {
                Text("Installing… ${(progress * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Win11Colors.AccentBlue)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isInstalled) {
                    Button(onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = Win11Colors.SuccessGreen)) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Open")
                    }
                    OutlinedButton(onClick = onTogglePin) {
                        Icon(
                            Icons.Default.PushPin, null, modifier = Modifier.size(16.dp),
                            tint = if (isPinned) Win11Colors.AccentBlue else LocalContentColor.current
                        )
                        Spacer(Modifier.width(6.dp)); Text(if (isPinned) "Pinned to desktop" else "Pin to desktop")
                    }
                    OutlinedButton(onClick = onUninstall, colors = ButtonDefaults.outlinedButtonColors(contentColor = Win11Colors.DangerRed)) {
                        Text("Uninstall")
                    }
                } else {
                    Button(onClick = onInstall, colors = ButtonDefaults.buttonColors(containerColor = Win11Colors.AccentBlue)) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Install")
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("About this app", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(app.description, color = textColor.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 19.sp)
        if (app.isSideloaded) {
            Spacer(Modifier.height(14.dp))
            Text("Sideloaded — runs from local files, not the Bluebird catalog.", color = textColor.copy(alpha = 0.45f), fontSize = 11.sp)
        }
    }
}

// ============================================================================================
// APP RUNTIME — sandboxed WebView with the native Bluebird bridge injected as window.Bluebird
// ============================================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AppRuntimeView(
    app: BluebirdApp, installDir: File, isDark: Boolean, isPinned: Boolean,
    onToast: (String) -> Unit, onTogglePin: () -> Unit, onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(if (isDark) Color(0xFF202020) else Color(0xFFEDEDED)).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(app.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.PushPin, null, modifier = Modifier.size(16.dp),
                    tint = if (isPinned) Win11Colors.AccentBlue else LocalContentColor.current
                )
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    webViewClient = WebViewClient()
                    addJavascriptInterface(BluebirdBridge(ctx, onToast), "Bluebird")
                    loadUrl("file://${installDir.absolutePath}/index.html")
                }
            }
        )
    }
}

// ============================================================================================
// SIDELOAD DIALOG
// ============================================================================================

@Composable
private fun SideloadDialog(onDismiss: () -> Unit, onPickZip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sideload an app") },
        text = {
            Text(
                "Pick a .zip containing an index.html at its root (plus any style.css / script.js / assets). " +
                    "It'll be installed into Bluebird's app directory and shows up under Developer.",
                fontSize = 13.sp
            )
        },
        confirmButton = { TextButton(onClick = onPickZip) { Text("Choose file") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
