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
