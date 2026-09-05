package io.github.norbertweb.bluebird.ui.components

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.delay

data class BpkInstallActions(
    val addToStart: Boolean = true,
    val pinToTaskbar: Boolean = false,
    val createDesktopShortcut: Boolean = false
)

/**
 * Single-window BPK installation wizard.
 *
 * Important: this is NOT an AlertDialog. It is rendered directly inside
 * WindowManager's existing floating window, preventing the old "two windows"
 * effect. The package owns the optional HTML/CSS/JS installer presentation.
 */
@Composable
fun BpkInstallerDialog(
    packageFile: File,
    isDark: Boolean,
    onInstalled: (InstalledBpkApp, BpkInstallActions) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staged by remember(packageFile.absolutePath) { mutableStateOf<StagedBpkPackage?>(null) }
    var error by remember(packageFile.absolutePath) { mutableStateOf<String?>(null) }
    var isInstalling by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var finished by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var progress by remember(packageFile.absolutePath) { mutableStateOf(0f) }
    var installStage by remember(packageFile.absolutePath) { mutableStateOf("Preparing installation…") }
    var showLocationPicker by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var installPath by remember(packageFile.absolutePath) { mutableStateOf("") }
    var addToStart by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var pinToTaskbar by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var createDesktopShortcut by remember(packageFile.absolutePath) { mutableStateOf(false) }
    var installedResult by remember(packageFile.absolutePath) { mutableStateOf<InstalledBpkApp?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 260),
        label = "bpk-install-progress"
    )

    LaunchedEffect(packageFile.absolutePath) {
        withContext(Dispatchers.IO) {
            runCatching { BpkPackageManager(context).stage(packageFile) }
                .onSuccess {
                    staged = it
                    val manager = BpkPackageManager(context)
                    installPath = File(manager.programFilesDir, sanitizePathPart(it.manifest.name)).absolutePath
                    progress = 0f
                }
                .onFailure { error = it.message ?: "Invalid Bluebird package" }
        }
    }

    DisposableEffect(packageFile.absolutePath) {
        onDispose { staged?.stageDir?.deleteRecursively() }
    }

    fun installNow() {
        val current = staged ?: return
        if (isInstalling || finished) return
        val selected = File(installPath.trim())
        isInstalling = true
        error = null
        progress = 0f
        installStage = "Preparing installation…"
        scope.launch {
            runCatching {
                val installed = withContext(Dispatchers.IO) {
                    val manager = BpkPackageManager(context)
                    manager.install(current, selected) { stage, value ->
                        val (label, base, span) = when (stage) {
                            BpkInstallStage.COPYING -> Triple("Installing application files…", 0.05f, 0.55f)
                            BpkInstallStage.OPTIMIZING -> Triple("Optimizing web assets…", 0.60f, 0.14f)
                            BpkInstallStage.CREATING_LAUNCHER -> Triple("Creating application launcher…", 0.76f, 0.10f)
                            BpkInstallStage.CACHING_PACKAGE -> Triple("Preparing reinstall package…", 0.87f, 0.06f)
                            BpkInstallStage.REGISTERING -> Triple("Registering application…", 0.94f, 0.06f)
                        }
                        installStage = label
                        progress = (base + span * value).coerceIn(0f, 1f)
                    }
                }
                installed
            }.onSuccess {
                installStage = "Installation completed"
                progress = 1f
                isInstalling = false
                installedResult = it
                finished = true
            }.onFailure {
                isInstalling = false
                error = it.message ?: "Installation failed"
                progress = 0f
                installStage = "Installation failed"
            }
        }
    }

    fun restartInstallation() {
        finished = false
        error = null
        progress = 0f
        installStage = "Preparing installation…"
        staged = null
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { BpkPackageManager(context).stage(packageFile) }
                    .onSuccess { staged = it }
                    .onFailure { error = it.message ?: "Invalid Bluebird package" }
            }
        }
    }

    val bg = if (isDark) DS.surfaceDark else DS.surfaceLight
    val text = if (isDark) Color.White else Color.Black
    val dim = text.copy(alpha = 0.62f)

    Box(Modifier.fillMaxSize().background(bg)) {
        when {
            error != null && staged == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Package cannot be installed", color = text, fontSize = 17.sp)
                    Text(error.orEmpty(), color = dim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                        Button(onClick = { restartInstallation() }) { Text("Retry") }
                    }
                }
            }

            staged == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularInstallerSpinner()
                    Text("Preparing installation…", color = text, fontSize = 14.sp)
                }
            }

            finished -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = BpkPackageIcon.decode(packageFile)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = staged?.manifest?.name,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Text("Installation finished", color = text, fontSize = 19.sp)
                    Text(staged?.manifest?.name.orEmpty(), color = dim, fontSize = 14.sp)
                    Text("The application has been installed and is ready to use.", color = dim, fontSize = 12.sp)

                    Text("Choose where to place it", color = text, fontSize = 13.sp)
                    InstallOptionRow("Add to Start menu", addToStart, { addToStart = it }, text)
                    InstallOptionRow("Pin to taskbar", pinToTaskbar, { pinToTaskbar = it }, text)
                    InstallOptionRow("Create desktop shortcut", createDesktopShortcut, { createDesktopShortcut = it }, text)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Later") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                installedResult?.let {
                                    onInstalled(
                                        it,
                                        BpkInstallActions(addToStart, pinToTaskbar, createDesktopShortcut)
                                    )
                                }
                                onDismiss()
                            }
                        ) { Text("Finish") }
                    }
                }
            }

            staged?.hasCustomInstaller == true -> {
                // Developer-owned installer UI. Bluebird keeps the surrounding
                // window chrome fixed; the WebView only controls its inner content.
                val current = staged!!
                Column(Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = false
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.blockNetworkLoads = true
                                settings.setSupportZoom(false)
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): Boolean {
                                        val requested = request?.url ?: return true
                                        val stageRoot = current.stageDir.canonicalFile
                                        val path = runCatching { File(requested.path ?: "").canonicalFile }.getOrNull()
                                        return requested.scheme != "file" ||
                                            path == null ||
                                            !(path.path == stageRoot.path || path.path.startsWith(stageRoot.path + File.separator))
                                    }
                                }
                                addJavascriptInterface(
                                    BpkInstallerJavascriptBridge(
                                        staged = current,
                                        packageManager = BpkPackageManager(ctx),
                                        scope = scope,
                                        onInstallingChanged = { installing -> isInstalling = installing },
                                        onProgress = { value -> progress = value.coerceIn(0f, 1f) },
                                        onOptionsChanged = { actions ->
                                            addToStart = actions.addToStart
                                            pinToTaskbar = actions.pinToTaskbar
                                            createDesktopShortcut = actions.createDesktopShortcut
                                        },
                                        onInstalled = {
                                            progress = 1f
                                            finished = true
                                            isInstalling = false
                                            installedResult = it
                                        },
                                        onCancel = { if (!isInstalling) onDismiss() }
                                    ),
                                    "BluebirdInstaller"
                                )
                                loadUrl(android.net.Uri.fromFile(File(current.stageDir, "installer/index.html")).toString())
                            }
                        }
                    )
                    if (isInstalling) {
                        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val bitmap = BpkPackageIcon.decode(packageFile)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(staged!!.manifest.name, color = text, fontSize = 18.sp)
                            Text("${staged!!.manifest.publisher} • ${staged!!.manifest.version}", color = dim, fontSize = 11.sp)
                        }
                    }

                    if (staged!!.manifest.description.isNotBlank()) {
                        Text(staged!!.manifest.description, color = dim, fontSize = 12.sp)
                    }

                    Text("Install location", color = text, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = installPath,
                            onValueChange = { installPath = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            label = { Text("Installation path") }
                        )
                        Button(
                            onClick = { showLocationPicker = !showLocationPicker },
                            enabled = !isInstalling
                        ) { Text("Browse…") }
                    }
                    if (showLocationPicker) {
                        val manager = BpkPackageManager(context)
                        val root = manager.bluebirdRoot
                        val folders = remember(root.absolutePath) {
                            buildList {
                                add(root)
                                root.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() }?.forEach { add(it) }
                                File(root, "Program Files").takeIf { it.isDirectory && it !in this }?.let { add(it) }
                            }.distinctBy { it.absolutePath }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) DS.surfaceDark else DS.surfaceLight).padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Choose a folder in Bluebird Storage", color = text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            folders.forEach { folder ->
                                TextButton(
                                    onClick = {
                                        val target = if (folder == root) File(root, "Program Files/${sanitizePathPart(staged!!.manifest.name)}") else File(folder, sanitizePathPart(staged!!.manifest.name))
                                        installPath = target.absolutePath
                                        showLocationPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(folder.name.ifBlank { "Bluebird Storage" }, color = text, fontSize = 12.sp)
                                        Text(folder.absolutePath, color = dim, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Choose an installation folder inside Bluebird Storage. Bluebird will create the application folder automatically.",
                        color = dim,
                        fontSize = 11.sp
                    )

                    Text("After installation", color = text, fontSize = 13.sp)
                    InstallOptionRow("Add to Start menu", addToStart, { addToStart = it }, text)
                    InstallOptionRow("Pin to taskbar", pinToTaskbar, { pinToTaskbar = it }, text)
                    InstallOptionRow("Create desktop shortcut", createDesktopShortcut, { createDesktopShortcut = it }, text)

                    if (isInstalling) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(installStage, color = dim, fontSize = 11.sp)
                        Text("${(animatedProgress * 100).toInt()}%", color = text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    } else if (error != null) {
                        Text(error.orEmpty(), color = DS.badgeRed, fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(enabled = !isInstalling, onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(enabled = !isInstalling && installPath.trim().isNotEmpty(), onClick = ::installNow) { Text("Install") }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallOptionRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, color = textColor, fontSize = 12.sp)
    }
}

@Composable
private fun CircularInstallerSpinner() {
    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(30.dp))
}

private fun sanitizePathPart(value: String): String =
    value.trim().replace(Regex("[\\/:*?\"<>|]"), "_").ifBlank { "BluebirdApp" }

private class BpkInstallerJavascriptBridge(
    private val staged: StagedBpkPackage,
    private val packageManager: BpkPackageManager,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onInstallingChanged: (Boolean) -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onOptionsChanged: (BpkInstallActions) -> Unit,
    private val onInstalled: (InstalledBpkApp) -> Unit,
    private val onCancel: () -> Unit
) {
    @Volatile private var currentPath: String =
        File(packageManager.programFilesDir, sanitizePathPart(staged.manifest.name)).absolutePath
    @Volatile private var addToStart: Boolean = false
    @Volatile private var pinToTaskbar: Boolean = false
    @Volatile private var createDesktopShortcut: Boolean = false

    @JavascriptInterface fun getInstallPath(): String = currentPath

    @JavascriptInterface fun setInstallPath(path: String) {
        val trimmed = path.trim()
        if (trimmed.isNotEmpty()) currentPath = trimmed
    }

    private fun currentActions() = BpkInstallActions(addToStart, pinToTaskbar, createDesktopShortcut)

    @JavascriptInterface
    fun getManifest(): String = org.json.JSONObject().apply {
        put("id", staged.manifest.id)
        put("name", staged.manifest.name)
        put("version", staged.manifest.version)
        put("publisher", staged.manifest.publisher)
        put("entry", staged.manifest.entry)
        put("icon", staged.manifest.icon)
        put("description", staged.manifest.description)
        put("homepage", staged.manifest.homepage)
        put("runtime", staged.manifest.runtime)
    }.toString()

    @JavascriptInterface
    fun install() {
        onInstallingChanged(true)
        onProgress(0.08f)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    packageManager.install(staged, File(currentPath))
                }
            }.onSuccess {
                onProgress(1f)
                onInstallingChanged(false)
                onInstalled(it)
            }.onFailure {
                onInstallingChanged(false)
            }
        }
    }

    @JavascriptInterface fun cancel() = onCancel()
}
