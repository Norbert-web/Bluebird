package io.github.norbertweb.bluebird.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.io.File
import io.github.norbertweb.bluebird.LauncherViewModel

/**
 * Bluebird's installed-programs manager.
 * It deliberately manages installed applications, not a separate "web app" category.
 */
@Composable
fun ProgramManagerScreen(
    isDark: Boolean,
    viewModel: LauncherViewModel,
    onLaunchApp: (InstalledBpkApp) -> Unit = { viewModel.launchBpkApp(it.id) }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf(BpkPackageManager(context).apps()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var uninstallTarget by remember { mutableStateOf<InstalledBpkApp?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val bg = if (isDark) Color(0xFF111111) else Color(0xFFF7F7F7)
    val surface = if (isDark) Color(0xFF1C1C1C) else Color.White
    val text = if (isDark) Color(0xFFF2F2F2) else Color(0xFF202020)
    val sub = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)

    fun refresh() {
        apps = BpkPackageManager(context).apps()
        viewModel.ensureInstalledAppsLoaded()
    }

    Column(Modifier.fillMaxSize().background(bg)) {
        Row(
            Modifier.fillMaxWidth().background(surface).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Program Manager", color = text, fontSize = 20.sp)
                Text("Installed applications", color = sub, fontSize = 12.sp)
            }
            Text("${apps.size} installed", color = sub, fontSize = 12.sp)
        }
        HorizontalDivider()

        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FluentIcon.Apps, null, tint = sub, modifier = Modifier.size(42.dp))
                    Text("No packaged applications installed", color = text)
                    Text("Install a .bpk package to see it here.", color = sub, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(apps, key = { it.id }) { app ->
                    val icon = remember(app.iconPath) {
                        runCatching { BitmapFactory.decodeFile(app.iconPath) }.getOrNull()
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (icon != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = icon.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Icon(FluentIcon.Apps, null, tint = sub, modifier = Modifier.size(48.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = text, fontSize = 15.sp)
                                Text("${app.version} • ${app.publisher}", color = sub, fontSize = 11.sp)
                                Text(app.installDir, color = sub, fontSize = 10.sp, maxLines = 1)
                            }
                            if (busyId == app.id) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { onLaunchApp(app) }) { Icon(FluentIcon.Play, null, tint = text) }
                                IconButton(onClick = {
                                    if (app.canReinstall) {
                                        busyId = app.id
                                        scope.launch {
                                            runCatching { viewModel.reinstallBpkApplicationAndWait(app.id) }
                                                .onSuccess { refresh() }
                                                .onFailure { message = it.message ?: "Reinstall failed" }
                                            busyId = null
                                        }
                                    } else message = "${app.name} does not have a cached package for reinstall."
                                }) { Icon(FluentIcon.Refresh, null, tint = text) }
                                IconButton(onClick = { uninstallTarget = app }) { Icon(FluentIcon.Delete, null, tint = text) }
                            }
                        }
                    }
                }
            }
        }
    }

    uninstallTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { uninstallTarget = null },
            title = { Text("Uninstall ${app.name}?") },
            text = { Text("This removes the installed application files from Bluebird Storage.") },
            confirmButton = {
                Button(onClick = {
                    uninstallTarget = null
                    busyId = app.id
                    scope.launch {
                        runCatching { viewModel.uninstallBpkApplicationAndWait(app.id) }
                            .onSuccess { refresh() }
                            .onFailure { message = it.message ?: "Uninstall failed" }
                        busyId = null
                    }
                }) { Text("Uninstall") }
            },
            dismissButton = { TextButton(onClick = { uninstallTarget = null }) { Text("Cancel") } }
        )
    }

    message?.let { msg ->
        AlertDialog(onDismissRequest = { message = null }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}
