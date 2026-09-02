package io.github.norbertweb.bluebird.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.editor.editor.core.PremiumEditorState
import io.github.norbertweb.bluebird.editor.core.ShellActivity
import io.github.norbertweb.bluebird.ui.components.FluentIcon
import java.io.File

@Composable
fun WorkspaceSymbolsDialog(s: PremiumEditorState) {
    val c = s.colors
    var query by remember { mutableStateOf("") }
    val symbols = remember(s.workspaceIndex.indexedSymbolCount, query) {
        s.workspaceIndex.indexedSymbols.filter { query.isBlank() || it.name.contains(query, true) || it.fileName.contains(query, true) }
            .sortedWith(compareBy({ it.fileName.lowercase() }, { it.line }, { it.name.lowercase() }))
    }
    AlertDialog(
        onDismissRequest = { s.showWorkspaceSymbols = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(FluentIcon.Code, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Text("Workspace Symbols", color = c.text)
        } },
        text = { Column {
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp), cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().background(c.surfaceHover, RoundedCornerShape(7.dp))
                    .border(1.dp, c.border, RoundedCornerShape(7.dp)).padding(10.dp),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search symbols across the workspace…", color = c.textMuted, fontSize = 13.sp); inner() } }
            )
            Spacer(Modifier.height(8.dp))
            Text("${symbols.size} symbol(s) · ${s.workspaceIndex.indexedFilePaths.size} files", color = c.textMuted, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(symbols) { symbol ->
                    Row(Modifier.fillMaxWidth().clickable {
                        val open = s.tabs.indexOfFirst { it.filePath == symbol.filePath }
                        if (open >= 0) {
                            s.selectTabIdInGroup(s.activeEditorGroup, s.tabs[open].id)
                            s.updateTabById(s.tabs[open].id) { copy(content = content.copy(selection = androidx.compose.ui.text.TextRange(symbol.offset))) }
                            s.showWorkspaceSymbols = false
                        } else {
                            runCatching { s.loadFile(LocalContext.current, symbol.filePath) }
                            s.showWorkspaceSymbols = false
                        }
                    }.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(FluentIcon.Code, null, tint = c.accent, modifier = Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(symbol.name, color = c.text, fontSize = 12.sp)
                            Text("${symbol.kind} · ${symbol.fileName}:${symbol.line}:${symbol.column}", color = c.textMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { s.showWorkspaceSymbols = false }) { Text("Close", color = c.accent) } }
    )
}

@Composable
fun RenameSymbolDialog(s: PremiumEditorState) {
    val c = s.colors
    var value by remember(s.renameTarget) { mutableStateOf(s.renameTarget) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = { s.showRenameSymbol = false }, containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { Text("Rename Symbol", color = c.text) },
        text = { Column {
            Text("Rename all workspace references to \"${s.renameTarget}\".", color = c.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            BasicTextField(value = value, onValueChange = { value = it }, singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp), cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().background(c.surfaceHover, RoundedCornerShape(7.dp))
                    .border(1.dp, c.border, RoundedCornerShape(7.dp)).padding(10.dp).focusRequester(focus))
        } },
        confirmButton = { TextButton(enabled = value.isNotBlank() && value != s.renameTarget, onClick = { s.renameWorkspaceSymbol(value) }) { Text("Rename", color = c.accent) } },
        dismissButton = { TextButton(onClick = { s.showRenameSymbol = false }) { Text("Cancel", color = c.textMuted) } }
    )
}

@Composable
fun WorkspaceOutlineDialog(s: PremiumEditorState) {
    val c = s.colors
    val files = s.workspaceIndex.indexedFiles
    val symbols = s.workspaceIndex.indexedSymbols
    AlertDialog(
        onDismissRequest = { s.showWorkspaceOutline = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.List, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Workspace Outline", color = c.text)
            }
        },
        text = {
            Column {
                Text("${files.size} files · ${symbols.size} symbols · ${s.workspaceIndex.indexedImportCount} imports · ${s.workspaceIndex.indexedExportCount} exports", color = c.textMuted, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    files.forEach { file ->
                        item(key = "file:${file.path}") {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(FluentIcon.FolderOpen, null, tint = c.textMuted, modifier = Modifier.size(14.dp))
                                Text(file.fileName, color = c.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("${file.symbolCount}", color = c.textMuted, fontSize = 10.sp)
                            }
                        }
                        symbols.filter { it.filePath == file.path }.sortedBy { it.offset }.forEach { symbol ->
                            item(key = "symbol:${file.path}:${symbol.offset}") {
                                Row(Modifier.fillMaxWidth().clickable {
                                    val index = s.tabs.indexOfFirst { runCatching { File(it.filePath).canonicalPath == File(symbol.filePath).canonicalPath }.getOrDefault(it.filePath == symbol.filePath) }
                                    if (index >= 0) {
                                        s.selectTabIdInGroup(s.activeEditorGroup, s.tabs[index].id)
                                        s.updateTabById(s.tabs[index].id) { copy(content = content.copy(selection = androidx.compose.ui.text.TextRange(symbol.offset))) }
                                    } else runCatching { s.loadFile(LocalContext.current, symbol.filePath) }
                                    s.showWorkspaceOutline = false
                                }.padding(start = 24.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(FluentIcon.Code, null, tint = c.accent, modifier = Modifier.size(12.dp))
                                    Text(symbol.name, color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                    Text("${symbol.line}", color = c.textMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { s.showWorkspaceOutline = false }) { Text("Close", color = c.accent) } }
    )
}


@Composable
fun WorkspaceHomeDialog(s: PremiumEditorState) {
    val c = s.colors
    val root = s.workspaceRoot
    val files = s.workspaceIndex.indexedFiles
    val openFiles = s.tabs.filter { it.filePath.isNotBlank() }
    val modified = openFiles.count { it.isModified }
    val extensions = files.groupingBy { it.fileName.substringAfterLast('.', "file").lowercase() }.eachCount()
        .entries.sortedByDescending { it.value }.take(5)

    AlertDialog(
        onDismissRequest = { s.showWorkspaceHome = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(FluentIcon.FolderOpen, null, tint = c.accent, modifier = Modifier.size(20.dp))
                Column {
                    Text("Workspace", color = c.text, fontSize = 16.sp)
                    Text(root?.name ?: "Current project", color = c.textMuted, fontSize = 10.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    WorkspaceMetric("FILES", "${files.size}", c, Modifier.weight(1f))
                    WorkspaceMetric("SYMBOLS", "${s.workspaceIndex.indexedSymbolCount}", c, Modifier.weight(1f))
                    WorkspaceMetric("OPEN", "${openFiles.size}", c, Modifier.weight(1f))
                    WorkspaceMetric("CHANGED", "$modified", c, Modifier.weight(1f))
                }
                Text("QUICK ACTIONS", color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    WorkspaceHomeAction("Explorer", FluentIcon.FolderOpen, c) { s.showWorkspaceHome = false; s.shellActivity = ShellActivity.EXPLORER }
                    WorkspaceHomeAction("Outline", FluentIcon.List, c) { s.showWorkspaceHome = false; s.showWorkspaceOutline() }
                    WorkspaceHomeAction("Symbols", FluentIcon.Code, c) { s.showWorkspaceHome = false; s.showWorkspaceSymbolTree() }
                }
                Text("OPEN EDITORS", color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (openFiles.isEmpty()) {
                    Text("No files are open yet.", color = c.textMuted, fontSize = 11.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 170.dp)) {
                        items(openFiles, key = { it.id }) { tab ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                                    .clickable {
                                        s.selectTabIdInGroup(s.activeEditorGroup, tab.id)
                                        s.showWorkspaceHome = false
                                    }.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(FluentIcon.Document, null, tint = if (tab.isModified) c.warning else c.accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tab.fileName.ifBlank { "Untitled" }, color = c.text, fontSize = 11.sp, maxLines = 1)
                                    Text(tab.filePath, color = c.textMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (tab.isModified) Text("●", color = c.warning, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (extensions.isNotEmpty()) {
                    Text("PROJECT PROFILE", color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(extensions.joinToString("  ·  ") { ".${it.key} ${it.value}" }, color = c.textSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(s.workspaceIndexStatus, color = c.textMuted, fontSize = 9.sp)
            }
        },
        confirmButton = { TextButton(onClick = { s.showWorkspaceHome = false }) { Text("Back to Editor", color = c.accent) } }
    )
}

@Composable
private fun WorkspaceMetric(label: String, value: String, c: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors, modifier: Modifier) {
    Column(modifier.background(c.surfaceHover, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 7.dp)) {
        Text(value, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = c.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkspaceHomeAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, c: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(c.surfaceHover).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(14.dp))
        Text(label, color = c.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
