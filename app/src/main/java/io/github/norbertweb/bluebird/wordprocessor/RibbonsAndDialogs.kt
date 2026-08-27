package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// RibbonsAndDialogs.kt — the ribbon tabs and every modal dialog the app uses.
// ============================================================================================

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PRESET_COLORS = listOf(
    Color(0xFF1A1A1A), Color(0xFFC00000), Color(0xFF2B579A), Color(0xFF1E7A34),
    Color(0xFFB8860B), Color(0xFF6A1B9A), Color(0xFFE07B00), Color(0xFF616161)
)
private val PRESET_HIGHLIGHTS = listOf(
    Color(0xFFFFF59D), Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFF48FB1), Color(0xFFFFCC80)
)

enum class RibbonTab { FILE, HOME, INSERT, REFERENCES, VIEW }

// ============================================================================================
// HOME RIBBON
// ============================================================================================

@Composable
fun HomeRibbon(
    enabled: Boolean, typingOrSelectionStyle: StyleAttrs, alignment: TextAlign,
    currentStyleId: String, listType: ListType?,
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit, onStrikethrough: () -> Unit,
    onSuperscript: () -> Unit, onSubscript: () -> Unit,
    onFontSizeChange: (Int) -> Unit, onColorChange: (Color) -> Unit, onHighlightChange: (Color?) -> Unit,
    onFontChange: (FontChoice) -> Unit,
    onAlignChange: (TextAlign) -> Unit, onBullet: () -> Unit, onNumbered: () -> Unit,
    onIndentIncrease: () -> Unit, onIndentDecrease: () -> Unit, onStyleChange: (String) -> Unit,
    onClearFormatting: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            RibbonChip(BuiltInStyles.byId(currentStyleId).name, enabled) { showStyleMenu = true }
            DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                BuiltInStyles.ALL.forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st.name, fontWeight = if (st.bold) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onStyleChange(st.id); showStyleMenu = false }
                    )
                }
            }
        }

        RibbonDivider()

        Box {
            RibbonChip(typingOrSelectionStyle.font.label, enabled) { showFontMenu = true }
            DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                FontChoice.entries.forEach { f ->
                    DropdownMenuItem(text = { Text(f.label, fontFamily = f.family) }, onClick = { onFontChange(f); showFontMenu = false })
                }
            }
        }

        RibbonIconButton(Icons.Default.Remove, enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize - 1).coerceAtLeast(8)) }
        RibbonChip("${typingOrSelectionStyle.fontSize}", enabled) {}
        RibbonIconButton(Icons.Default.Add, enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize + 1).coerceAtMost(96)) }

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatBold, typingOrSelectionStyle.bold, enabled, onBold)
        RibbonToggleIcon(Icons.Default.FormatItalic, typingOrSelectionStyle.italic, enabled, onItalic)
        RibbonToggleIcon(Icons.Default.FormatUnderlined, typingOrSelectionStyle.underline, enabled, onUnderline)
        RibbonToggleIcon(Icons.Default.StrikethroughS, typingOrSelectionStyle.strikethrough, enabled, onStrikethrough)
        RibbonToggleIcon(Icons.Default.Superscript, typingOrSelectionStyle.superscript, enabled, onSuperscript)
        RibbonToggleIcon(Icons.Default.Subscript, typingOrSelectionStyle.subscript, enabled, onSubscript)

        Box {
            Box(
                modifier = Modifier.size(32.dp).padding(4.dp).clip(RoundedCornerShape(4.dp))
                    .background(typingOrSelectionStyle.color)
                    .clickable(enabled = enabled) { showColorPicker = true }
            )
            DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }) {
                Row(Modifier.padding(8.dp)) {
                    PRESET_COLORS.forEach { c ->
                        Box(
                            modifier = Modifier.size(24.dp).padding(2.dp).clip(CircleShape).background(c)
                                .clickable { onColorChange(c); showColorPicker = false }
                        )
                    }
                }
            }
        }

        Box {
            Box(
                modifier = Modifier.size(32.dp).padding(4.dp).clip(RoundedCornerShape(4.dp))
                    .background(typingOrSelectionStyle.highlight ?: Color.Transparent)
                    .then(if (typingOrSelectionStyle.highlight == null) Modifier.background(Color.Gray.copy(alpha = 0.12f)) else Modifier)
                    .clickable(enabled = enabled) { showHighlightPicker = true }
            ) {
                Icon(Icons.Default.FormatColorFill, null, modifier = Modifier.size(14.dp).align(Alignment.Center))
            }
            DropdownMenu(expanded = showHighlightPicker, onDismissRequest = { showHighlightPicker = false }) {
                Row(Modifier.padding(8.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).padding(2.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.3f))
                            .clickable { onHighlightChange(null); showHighlightPicker = false }
                    ) { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                    PRESET_HIGHLIGHTS.forEach { c ->
                        Box(
                            modifier = Modifier.size(24.dp).padding(2.dp).clip(CircleShape).background(c)
                                .clickable { onHighlightChange(c); showHighlightPicker = false }
                        )
                    }
                }
            }
        }

        RibbonIconButton(Icons.Default.FormatClear, enabled, onClearFormatting)

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatAlignLeft, alignment == TextAlign.Start, enabled) { onAlignChange(TextAlign.Start) }
        RibbonToggleIcon(Icons.Default.FormatAlignCenter, alignment == TextAlign.Center, enabled) { onAlignChange(TextAlign.Center) }
        RibbonToggleIcon(Icons.Default.FormatAlignRight, alignment == TextAlign.End, enabled) { onAlignChange(TextAlign.End) }
        RibbonToggleIcon(Icons.Default.FormatAlignJustify, alignment == TextAlign.Justify, enabled) { onAlignChange(TextAlign.Justify) }

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatListBulleted, listType == ListType.BULLET, enabled, onBullet)
        RibbonToggleIcon(Icons.Default.FormatListNumbered, listType == ListType.NUMBER, enabled, onNumbered)
        RibbonIconButton(Icons.Default.FormatIndentDecrease, enabled) { onIndentDecrease() }
        RibbonIconButton(Icons.Default.FormatIndentIncrease, enabled) { onIndentIncrease() }
    }
}

// ============================================================================================
// INSERT RIBBON
// ============================================================================================

@Composable
fun InsertRibbon(
    onInsertDate: () -> Unit, onInsertDivider: () -> Unit, onInsertTable: () -> Unit,
    onInsertImage: () -> Unit, onInsertPageBreak: () -> Unit, onInsertLink: () -> Unit,
    onInsertBookmark: () -> Unit, onInsertPageNumberField: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RibbonLabeledAction(Icons.Default.Image, "Picture", onInsertImage)
        RibbonLabeledAction(Icons.Default.TableChart, "Table", onInsertTable)
        RibbonLabeledAction(Icons.Default.InsertPageBreak, "Page Break", onInsertPageBreak)
        RibbonLabeledAction(Icons.Default.Link, "Link", onInsertLink)
        RibbonLabeledAction(Icons.Default.Bookmark, "Bookmark", onInsertBookmark)
        RibbonLabeledAction(Icons.Default.Numbers, "Page #", onInsertPageNumberField)
        RibbonLabeledAction(Icons.Default.CalendarToday, "Date", onInsertDate)
        RibbonLabeledAction(Icons.Default.HorizontalRule, "Divider", onInsertDivider)
    }
}

// ============================================================================================
// REFERENCES RIBBON
// ============================================================================================

@Composable
fun ReferencesRibbon(
    navPanelOpen: Boolean, bookmarks: List<Bookmark>,
    onInsertToc: () -> Unit, onJumpToBookmark: (String) -> Unit, onDeleteBookmark: (String) -> Unit,
    onToggleNavPanel: () -> Unit
) {
    var showBookmarkMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RibbonLabeledAction(Icons.Default.FormatListNumbered, "Table of\nContents", onInsertToc)
        Box {
            RibbonLabeledAction(Icons.Default.Bookmark, "Go to\nBookmark") { showBookmarkMenu = true }
            BookmarkMenu(
                bookmarks = bookmarks, expanded = showBookmarkMenu,
                onDismiss = { showBookmarkMenu = false }, onJump = onJumpToBookmark, onDelete = onDeleteBookmark
            )
        }
        RibbonLabeledAction(if (navPanelOpen) Icons.Default.ViewSidebar else Icons.Default.List, "Navigation\nPanel", onToggleNavPanel)
    }
}

// ============================================================================================
// VIEW RIBBON
// ============================================================================================

@Composable
fun ViewRibbon(
    sidebarOpen: Boolean, zoom: Float, readOnly: Boolean, readingMode: Boolean,
    onToggleSidebar: () -> Unit, onZoomChange: (Float) -> Unit,
    onToggleReadOnly: () -> Unit, onToggleReadingMode: () -> Unit,
    onPageSetup: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RibbonToggleIcon(Icons.Default.ViewSidebar, sidebarOpen, true) { onToggleSidebar() }
        RibbonToggleIcon(Icons.Default.EditOff, readOnly, true) { onToggleReadOnly() }
        RibbonToggleIcon(Icons.Default.Fullscreen, readingMode, true) { onToggleReadingMode() }
        RibbonIconButton(Icons.Default.SettingsApplications, true) { onPageSetup() }
        Text("Zoom", fontSize = 12.sp)
        Slider(value = zoom, onValueChange = onZoomChange, valueRange = 0.6f..2f, modifier = Modifier.width(160.dp))
        Text("${(zoom * 100).toInt()}%", fontSize = 12.sp)
    }
}

// ============================================================================================
// SMALL RIBBON HELPERS
// ============================================================================================

@Composable fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
fun RibbonChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.12f))
            .clickable(enabled = enabled) { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
fun RibbonIconButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RibbonToggleIcon(icon: ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(if (selected) bluebirdColors.AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (selected) bluebirdColors.AccentBlue else LocalContentColor.current, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RibbonLabeledAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onClick() }.padding(8.dp).width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
    }
}

// ============================================================================================
// DIALOGS
// ============================================================================================

enum class SaveFormat { WDOC, TXT, PDF }

@Composable
fun SaveAsDialog(currentTitle: String, onDismiss: () -> Unit, onConfirm: (String, SaveFormat) -> Unit) {
    var name by remember { mutableStateOf(currentTitle) }
    var format by remember { mutableStateOf(SaveFormat.WDOC) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save As") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("File name") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text("Format", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Column {
                    listOf(
                        SaveFormat.WDOC to "Word Impress Document (.wdoc)",
                        SaveFormat.TXT to "Plain Text (.txt)",
                        SaveFormat.PDF to "PDF Document (.pdf)"
                    ).forEach { (f, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { format = f }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = format == f, onClick = { format = f })
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, format) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun InsertTableDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var rows by remember { mutableStateOf("3") }
    var cols by remember { mutableStateOf("3") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Table") },
        text = {
            Column {
                OutlinedTextField(value = rows, onValueChange = { rows = it.filter(Char::isDigit) }, label = { Text("Rows") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = cols, onValueChange = { cols = it.filter(Char::isDigit) }, label = { Text("Columns") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm((rows.toIntOrNull() ?: 1).coerceIn(1, 20), (cols.toIntOrNull() ?: 1).coerceIn(1, 10)) }) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PageSetupDialog(current: PageSettings, onDismiss: () -> Unit, onConfirm: (PageSettings) -> Unit) {
    var sizeId by remember { mutableStateOf(current.sizeId) }
    var orientation by remember { mutableStateOf(current.orientation) }
    var marginTop by remember { mutableStateOf(current.marginTopPt.toInt().toString()) }
    var marginBottom by remember { mutableStateOf(current.marginBottomPt.toInt().toString()) }
    var marginLeft by remember { mutableStateOf(current.marginLeftPt.toInt().toString()) }
    var marginRight by remember { mutableStateOf(current.marginRightPt.toInt().toString()) }
    var customW by remember { mutableStateOf(current.customWidthPt.toInt().toString()) }
    var customH by remember { mutableStateOf(current.customHeightPt.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page Setup") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Paper size", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row {
                    listOf("A4", "LETTER", "LEGAL", "CUSTOM").forEach { sid ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { sizeId = sid }.padding(end = 6.dp)) {
                            RadioButton(selected = sizeId == sid, onClick = { sizeId = sid })
                            Text(sid, fontSize = 11.sp)
                        }
                    }
                }
                if (sizeId == "CUSTOM") {
                    Row {
                        OutlinedTextField(value = customW, onValueChange = { customW = it.filter(Char::isDigit) }, label = { Text("Width pt") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = customH, onValueChange = { customH = it.filter(Char::isDigit) }, label = { Text("Height pt") }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Orientation", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row {
                    listOf("portrait", "landscape").forEach { o ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { orientation = o }.padding(end = 8.dp)) {
                            RadioButton(selected = orientation == o, onClick = { orientation = o })
                            Text(o, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Margins (pt)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row {
                    OutlinedTextField(value = marginTop, onValueChange = { marginTop = it.filter(Char::isDigit) }, label = { Text("Top") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(value = marginBottom, onValueChange = { marginBottom = it.filter(Char::isDigit) }, label = { Text("Bottom") }, modifier = Modifier.weight(1f))
                }
                Row {
                    OutlinedTextField(value = marginLeft, onValueChange = { marginLeft = it.filter(Char::isDigit) }, label = { Text("Left") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(value = marginRight, onValueChange = { marginRight = it.filter(Char::isDigit) }, label = { Text("Right") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    PageSettings(
                        sizeId = sizeId, orientation = orientation,
                        customWidthPt = customW.toFloatOrNull() ?: 595f, customHeightPt = customH.toFloatOrNull() ?: 842f,
                        marginTopPt = marginTop.toFloatOrNull() ?: 72f, marginBottomPt = marginBottom.toFloatOrNull() ?: 72f,
                        marginLeftPt = marginLeft.toFloatOrNull() ?: 72f, marginRightPt = marginRight.toFloatOrNull() ?: 72f
                    )
                )
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DocumentPropertiesDialog(doc: WordDocument, onDismiss: () -> Unit, onSaveAuthor: (String) -> Unit) {
    var author by remember { mutableStateOf(doc.author) }
    val paragraphs = doc.blocks.filterIsInstance<ParagraphBlock>()
    val words = paragraphs.sumOf { p -> p.field.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val chars = paragraphs.sumOf { it.field.text.length }
    val dateFmt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Properties") },
        text = {
            Column {
                Text("Title: ${doc.title}", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text("Created: ${dateFmt.format(Date(doc.created))}", fontSize = 12.sp)
                Text("Modified: ${dateFmt.format(Date(doc.lastModified))}", fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("$words words · $chars characters · ${paragraphs.size} paragraphs", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSaveAuthor(author) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Document") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SettingsDialog(settings: AppSettings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Autosave", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Switch(checked = settings.autosaveEnabled, onCheckedChange = { settings.autosaveEnabled = it })
                }
                if (settings.autosaveEnabled) {
                    Text("Every ${settings.autosaveIntervalSec}s", fontSize = 11.sp)
                    Slider(
                        value = settings.autosaveIntervalSec.toFloat(),
                        onValueChange = { settings.autosaveIntervalSec = it.toInt() },
                        valueRange = 10f..120f
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Default font", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row {
                    FontChoice.entries.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings.defaultFont = f }.padding(end = 8.dp)) {
                            RadioButton(selected = settings.defaultFont == f, onClick = { settings.defaultFont = f })
                            Text(f.label, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Default size: ${settings.defaultFontSize}", fontSize = 12.sp)
                Slider(
                    value = settings.defaultFontSize.toFloat(),
                    onValueChange = { settings.defaultFontSize = it.toInt() },
                    valueRange = 8f..36f
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun FindReplaceDialog(
    query: String, replacement: String, matchCount: Int, currentMatch: Int,
    onQueryChange: (String) -> Unit, onReplacementChange: (String) -> Unit,
    onNext: () -> Unit, onPrev: () -> Unit, onReplaceOne: () -> Unit, onReplaceAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find & Replace") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Find") }, singleLine = true)
                Spacer(Modifier.height(6.dp))
                Text(if (matchCount == 0) "No matches" else "${currentMatch + 1} of $matchCount", fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = replacement, onValueChange = onReplacementChange, label = { Text("Replace with") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Row {
                    TextButton(onClick = onPrev) { Text("Prev") }
                    TextButton(onClick = onNext) { Text("Next") }
                    TextButton(onClick = onReplaceOne) { Text("Replace") }
                    TextButton(onClick = onReplaceAll) { Text("Replace All") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun LinkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Link") },
        text = { OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(url) }) { Text("Insert") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddBookmarkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Bookmark name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BookmarkMenu(bookmarks: List<Bookmark>, expanded: Boolean, onDismiss: () -> Unit, onJump: (String) -> Unit, onDelete: (String) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (bookmarks.isEmpty()) {
            DropdownMenuItem(text = { Text("No bookmarks yet") }, onClick = {}, enabled = false)
        }
        bookmarks.forEach { bm ->
            DropdownMenuItem(
                text = { Text(bm.name) },
                onClick = { onJump(bm.blockId); onDismiss() },
                trailingIcon = {
                    IconButton(onClick = { onDelete(bm.id) }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                    }
                }
            )
        }
    }
}

@Composable
fun RecoveryDialog(count: Int, onRestore: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text("Recover documents?") },
        text = { Text("Found $count document(s) that weren't saved before the app last closed.") },
        confirmButton = { TextButton(onClick = onRestore) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Discard") } }
    )
}
