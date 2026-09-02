package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// RibbonsAndDialogs.kt — the ribbon tabs and every modal dialog the app uses.
// ============================================================================================

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WordRibbonBackground = Color(0xFFFFFFFF)
private val WordRibbonSurface = Color(0xFFF3F2F1)
val WordRibbonBorder = Color(0xFFD9D9D9)
val WordRibbonAccent = Color(0xFF185ABD)

private val PRESET_COLORS = listOf(
    Color(0xFF1A1A1A), Color(0xFFC00000), Color(0xFF2B579A), Color(0xFF1E7A34),
    Color(0xFFB8860B), Color(0xFF6A1B9A), Color(0xFFE07B00), Color(0xFF616161)
)
private val PRESET_HIGHLIGHTS = listOf(
    Color(0xFFFFF59D), Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFF48FB1), Color(0xFFFFCC80)
)

enum class RibbonTab { FILE, HOME, INSERT, LAYOUT, REFERENCES, REVIEW, VIEW }

// ============================================================================================
// HOME RIBBON
// ============================================================================================

@Composable
fun HomeRibbon(
    enabled: Boolean, typingOrSelectionStyle: StyleAttrs, alignment: TextAlign?,
    mixedFormatting: Boolean = false,
    currentStyleId: String, listType: ListType?,
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit, onStrikethrough: () -> Unit,
    onSuperscript: () -> Unit, onSubscript: () -> Unit,
    onFontSizeChange: (Int) -> Unit, onColorChange: (Color) -> Unit, onHighlightChange: (Color?) -> Unit,
    onFontChange: (FontChoice) -> Unit,
    onAlignChange: (TextAlign) -> Unit, onBullet: () -> Unit, onNumbered: () -> Unit,
    onIndentIncrease: () -> Unit, onIndentDecrease: () -> Unit, onStyleChange: (String) -> Unit,
    onClearFormatting: () -> Unit, onCopy: () -> Unit, onCut: () -> Unit, onPaste: () -> Unit,
    onSpacingChange: (Int, Int) -> Unit, onLineSpacingChange: (Float) -> Unit
) {
    var showFontMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showSpacingMenu by remember { mutableStateOf(false) }
    var showPasteMenu by remember { mutableStateOf(false) }
    var fontSizeText by remember(typingOrSelectionStyle.fontSize, mixedFormatting) { mutableStateOf(if (mixedFormatting) "" else typingOrSelectionStyle.fontSize.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(WordRibbonBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Clipboard: Word uses one visually dominant Paste command and smaller Cut/Copy.
        RibbonGroup("Clipboard") {
            Box {
                Column(
                    modifier = Modifier.width(40.dp).clip(RoundedCornerShape(2.dp))
                        .clickable(enabled = enabled) { onPaste() }
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FluentIcon("clipboard_paste", modifier = Modifier.size(18.dp), tint = WordRibbonAccent)
                    Text("Paste", fontSize = 6.5.sp, lineHeight = 7.sp)
                }
                RibbonDropArrow(onClick = { showPasteMenu = true }, enabled = enabled,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-1).dp))
                DropdownMenu(expanded = showPasteMenu, onDismissRequest = { showPasteMenu = false }) {
                    DropdownMenuItem(text = { Text("Keep source formatting", fontSize = 12.sp) }, onClick = { onPaste(); showPasteMenu = false })
                    DropdownMenuItem(text = { Text("Merge formatting", fontSize = 12.sp) }, onClick = { onPaste(); showPasteMenu = false })
                    DropdownMenuItem(text = { Text("Keep text only", fontSize = 12.sp) }, onClick = { onPaste(); showPasteMenu = false })
                }
            }
            RibbonIconButton("cut", enabled, onCut)
            RibbonIconButton("copy", enabled, onCopy)
        }

        RibbonGroup("Font") {
            // Font family box — compact but visually closer to Word's editable combo box.
            Box {
                RibbonComboBox(if (mixedFormatting) "Multiple" else typingOrSelectionStyle.font.label, enabled) { showFontMenu = true }
                DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                    FontChoice.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.label, fontFamily = f.family, fontSize = 12.sp) },
                            onClick = { onFontChange(f); showFontMenu = false }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RibbonIconButton("subtract", enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize - 1).coerceAtLeast(8)) }
                Box(
                    modifier = Modifier.width(34.dp).height(24.dp).clip(RoundedCornerShape(2.dp))
                        .background(Color.White).border(1.dp, WordRibbonBorder)
                ) {
                    BasicTextField(
                        value = fontSizeText,
                        onValueChange = { fontSizeText = it.filter(Char::isDigit).take(2) },
                        enabled = enabled,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = Color(0xFF202020), textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 4.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                            fontSizeText.toIntOrNull()?.let { onFontSizeChange(it.coerceIn(8, 96)) }
                        })
                    )
                }
                RibbonIconButton("add", enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize + 1).coerceAtMost(96)) }
            }
            RibbonToggleIcon("text_bold", typingOrSelectionStyle.bold, enabled, onBold)
            RibbonToggleIcon("text_italic", typingOrSelectionStyle.italic, enabled, onItalic)
            RibbonToggleIcon("text_underline", typingOrSelectionStyle.underline, enabled, onUnderline)
            RibbonToggleIcon("text_strikethrough", typingOrSelectionStyle.strikethrough, enabled, onStrikethrough)
            RibbonToggleIcon("text_superscript", typingOrSelectionStyle.superscript, enabled, onSuperscript)
            RibbonToggleIcon("text_subscript", typingOrSelectionStyle.subscript, enabled, onSubscript)
            Box {
                RibbonIconButton("text_color", enabled) { showColorPicker = true }
                RibbonDropArrow(onClick = { showColorPicker = true }, enabled = enabled, modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-1).dp))
                DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }) {
                    Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        PRESET_COLORS.forEach { c ->
                            Box(Modifier.size(22.dp).clip(CircleShape).background(c).clickable { onColorChange(c); showColorPicker = false })
                        }
                    }
                }
            }
            Box {
                RibbonIconButton("color_background", enabled) { showHighlightPicker = true }
                RibbonDropArrow(onClick = { showHighlightPicker = true }, enabled = enabled, modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-1).dp))
                DropdownMenu(expanded = showHighlightPicker, onDismissRequest = { showHighlightPicker = false }) {
                    Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White).border(1.dp, WordRibbonBorder).clickable { onHighlightChange(null); showHighlightPicker = false }) {
                            FluentIcon("dismiss", modifier = Modifier.size(11.dp), tint = Color.Gray)
                        }
                        PRESET_HIGHLIGHTS.forEach { c ->
                            Box(Modifier.size(22.dp).clip(CircleShape).background(c).clickable { onHighlightChange(c); showHighlightPicker = false })
                        }
                    }
                }
            }
            RibbonIconButton("text_clear_formatting", enabled, onClearFormatting)
        }

        RibbonGroup("Paragraph") {
            RibbonToggleIcon("text_bullet_list", listType == ListType.BULLET, enabled, onBullet)
            RibbonToggleIcon("text_number_list_ltr", listType == ListType.NUMBER, enabled, onNumbered)
            RibbonIconButton("text_indent_decrease_ltr", enabled, onIndentDecrease)
            RibbonIconButton("text_indent_increase_ltr", enabled, onIndentIncrease)
            Box {
                RibbonToggleIcon(
                    when (alignment) {
                        TextAlign.Center -> "text_align_center"
                        TextAlign.End -> "text_align_right"
                        TextAlign.Justify -> "text_align_justify_low"
                        else -> "text_align_left"
                    }, alignment != null, enabled
                ) { showAlignMenu = true }
                RibbonDropArrow(onClick = { showAlignMenu = true }, enabled = enabled, modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-1).dp))
                DropdownMenu(expanded = showAlignMenu, onDismissRequest = { showAlignMenu = false }) {
                    listOf(
                        TextAlign.Start to "Align Left", TextAlign.Center to "Center",
                        TextAlign.End to "Align Right", TextAlign.Justify to "Justify"
                    ).forEach { (a, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 12.sp) },
                            leadingIcon = { FluentIcon(
                                when (a) { TextAlign.Center -> "text_align_center"; TextAlign.End -> "text_align_right"; TextAlign.Justify -> "text_align_justify_low"; else -> "text_align_left" },
                                modifier = Modifier.size(16.dp)
                            ) },
                            onClick = { onAlignChange(a); showAlignMenu = false }
                        )
                    }
                }
            }
            Box {
                RibbonIconButton("text_line_spacing", enabled) { showSpacingMenu = true }
                RibbonDropArrow(onClick = { showSpacingMenu = true }, enabled = enabled, modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-1).dp))
                DropdownMenu(expanded = showSpacingMenu, onDismissRequest = { showSpacingMenu = false }) {
                    Text("Line spacing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                    listOf(1.0f to "1.0", 1.15f to "1.15", 1.5f to "1.5", 2.0f to "2.0").forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label, fontSize = 12.sp) }, onClick = { onLineSpacingChange(value); showSpacingMenu = false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Add space after paragraph", fontSize = 12.sp) }, onClick = { onSpacingChange(0, 8); showSpacingMenu = false })
                    DropdownMenuItem(text = { Text("Remove space after paragraph", fontSize = 12.sp) }, onClick = { onSpacingChange(0, 0); showSpacingMenu = false })
                }
            }
        }

        RibbonGroup("Styles") {
            Box {
                RibbonStyleGallery(currentStyleId, enabled, onStyleChange)
                RibbonDropArrow(onClick = { showStyleMenu = true }, enabled = enabled, modifier = Modifier.align(Alignment.CenterEnd))
                DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                    BuiltInStyles.ALL.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st.name, fontSize = 12.sp, fontWeight = if (st.bold) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onStyleChange(st.id); showStyleMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RibbonComboBox(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.width(92.dp).height(24.dp).clip(RoundedCornerShape(2.dp))
            .background(Color.White).border(1.dp, WordRibbonBorder)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 9.sp, color = Color(0xFF202020), maxLines = 1, modifier = Modifier.weight(1f))
        FluentIcon("chevron_down", modifier = Modifier.size(11.dp), tint = Color(0xFF555555))
    }
}

@Composable
private fun RibbonStyleGallery(currentStyleId: String, enabled: Boolean, onStyleChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (currentStyleId == "__mixed__") {
            Box(Modifier.width(58.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(Color.White).border(1.dp, WordRibbonBorder)) {
                Text("Multiple", fontSize = 8.sp, color = Color(0xFF555555), modifier = Modifier.align(Alignment.Center))
            }
        }
        BuiltInStyles.ALL.take(4).forEach { st ->
            val selected = st.id == currentStyleId
            Text(
                st.name, fontSize = 7.sp, maxLines = 1,
                modifier = Modifier.width(48.dp).height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) WordRibbonAccent.copy(alpha = 0.14f) else WordRibbonSurface)
                    .clickable(enabled = enabled) { onStyleChange(st.id) }
                    .padding(horizontal = 3.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun RibbonDropArrow(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(11.dp).clip(RoundedCornerShape(2.dp))
            .background(WordRibbonBackground.copy(alpha = 0.9f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon("chevron_down", modifier = Modifier.size(9.dp), tint = Color(0xFF555555))
    }
}

// ============================================================================================
// INSERT RIBBON
// ============================================================================================

@Composable
fun InsertRibbon(
    onInsertDate: () -> Unit,
    onInsertDivider: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onInsertLink: () -> Unit,
    onInsertBookmark: () -> Unit,
    onInsertPageNumberField: () -> Unit,
    onToggleHeader: () -> Unit,
    onToggleFooter: () -> Unit,
    onInsertSymbol: () -> Unit
) {
    CompactRibbonSurface {
        RibbonGroup("Pages") {
            RibbonLabeledAction("document_add", "Blank Page", onInsertPageBreak)
            RibbonLabeledAction("document", "Page Break", onInsertPageBreak)
        }
        RibbonGroup("Tables") {
            RibbonLabeledAction("table", "Table", onInsertTable)
        }
        RibbonGroup("Illustrations") {
            RibbonLabeledAction("image", "Pictures", onInsertImage)
        }
        RibbonGroup("Links") {
            RibbonLabeledAction("link", "Link", onInsertLink)
            RibbonLabeledAction("bookmark", "Bookmark", onInsertBookmark)
        }
        RibbonGroup("Header & Footer") {
            RibbonLabeledAction("text_header_1", "Header", onToggleHeader)
            RibbonLabeledAction("text_footer", "Footer", onToggleFooter)
            RibbonLabeledAction("number_symbol", "Page #", onInsertPageNumberField)
        }
        RibbonGroup("Text") {
            RibbonLabeledAction("calendar", "Date", onInsertDate)
            RibbonLabeledAction("text_insert", "Divider", onInsertDivider)
        }
        RibbonGroup("Symbols") {
            RibbonLabeledAction("omega", "Symbol", onInsertSymbol)
        }
    }
}

// ============================================================================================
// LAYOUT RIBBON
// ============================================================================================

@Composable
fun LayoutRibbon(
    onPageSetup: () -> Unit,
    onMargins: () -> Unit
) {
    CompactRibbonSurface {
        RibbonGroup("Page Setup") {
            RibbonLabeledAction("document_text", "Size", onPageSetup)
            RibbonLabeledAction("settings", "Orientation", onPageSetup)
            RibbonLabeledAction("text_align_justify_low", "Margins", onMargins)
        }
        RibbonGroup("Page Background") {
            RibbonLabeledAction("text_color", "Color") { }
            RibbonLabeledAction("image", "Watermark") { }
            RibbonLabeledAction("border_all", "Borders") { }
        }
        RibbonGroup("Paragraph") {
            RibbonLabeledAction("text_indent_decrease_ltr", "Indent ↓") { }
            RibbonLabeledAction("text_indent_increase_ltr", "Indent ↑") { }
            RibbonLabeledAction("text_line_spacing", "Spacing") { }
        }
    }
}

// ============================================================================================
// REFERENCES RIBBON
// ============================================================================================

@Composable
fun ReferencesRibbon(
    navPanelOpen: Boolean, bookmarks: List<Bookmark>,
    onInsertToc: () -> Unit, onJumpToBookmark: (String) -> Unit, onDeleteBookmark: (String) -> Unit,
    onInsertFootnote: () -> Unit, onInsertEndnote: () -> Unit, onToggleNavPanel: () -> Unit
) {
    var showBookmarkMenu by remember { mutableStateOf(false) }
    CompactRibbonSurface {
        RibbonGroup("Table of Contents") {
            RibbonLabeledAction("text_number_list_ltr", "Contents", onInsertToc)
        }
        RibbonGroup("Footnotes") {
            RibbonLabeledAction("note_add", "Footnote", onInsertFootnote)
            RibbonLabeledAction("note_add", "Endnote", onInsertEndnote)
        }
        RibbonGroup("Citations") {
            RibbonLabeledAction("book", "Sources") { }
            RibbonLabeledAction("document_text", "Bibliography") { }
        }
        RibbonGroup("Captions") {
            RibbonLabeledAction("text_add", "Caption") { }
            RibbonLabeledAction("list", "Cross-ref") { }
        }
        RibbonGroup("Navigate") {
            Box {
                RibbonLabeledAction("bookmark", "Bookmark") { showBookmarkMenu = true }
                BookmarkMenu(
                    bookmarks = bookmarks, expanded = showBookmarkMenu,
                    onDismiss = { showBookmarkMenu = false }, onJump = onJumpToBookmark, onDelete = onDeleteBookmark
                )
            }
            RibbonLabeledAction(if (navPanelOpen) "panel_left" else "list", "Navigation", onToggleNavPanel)
        }
    }
}

// ============================================================================================
// REVIEW RIBBON
// ============================================================================================

@Composable
fun ReviewRibbon(
    enabled: Boolean,
    onFind: () -> Unit,
    onAddComment: () -> Unit,
    onShowComments: () -> Unit,
    onToggleReadOnly: () -> Unit
) {
    CompactRibbonSurface {
        RibbonGroup("Proofing") {
            RibbonLabeledAction("text_grammar_error", "Spelling") { }
            RibbonLabeledAction("search", "Find", onFind)
            RibbonLabeledAction("document_text", "Word Count") { }
        }
        RibbonGroup("Comments") {
            RibbonLabeledAction("comment_add", "New Comment", onAddComment)
            RibbonLabeledAction("comment", "Comments", onShowComments)
        }
        RibbonGroup("Protect") {
            RibbonLabeledAction(if (enabled) "edit" else "edit_off", if (enabled) "Editing" else "Read Only", onToggleReadOnly)
        }
    }
}

// ============================================================================================
// VIEW RIBBON
// ============================================================================================

@Composable
fun ViewRibbon(
    sidebarOpen: Boolean, thumbnailsOpen: Boolean, zoom: Float, readOnly: Boolean,
    readingMode: Boolean, viewMode: DocumentViewMode, showRuler: Boolean,
    onToggleSidebar: () -> Unit, onToggleThumbnails: () -> Unit,
    onZoomChange: (Float) -> Unit, onViewModeChange: (DocumentViewMode) -> Unit,
    onToggleReadOnly: () -> Unit, onToggleReadingMode: () -> Unit, onToggleRuler: () -> Unit,
    onPageSetup: () -> Unit
) {
    CompactRibbonSurface {
        RibbonGroup("Show") {
            RibbonToggleLabeled("panel_left", sidebarOpen, "Navigation", onToggleSidebar)
            RibbonToggleLabeled("panel_bottom", thumbnailsOpen, "Pages", onToggleThumbnails)
            RibbonToggleLabeled("ruler_horizontal", showRuler, "Ruler", onToggleRuler)
        }
        RibbonGroup("Immersive") {
            RibbonToggleLabeled("full_screen_maximize", readingMode, "Reading", onToggleReadingMode)
            RibbonToggleLabeled("edit_off", readOnly, if (readOnly) "Read Only" else "Editing", onToggleReadOnly)
        }
        RibbonGroup("Page Movement") {
            RibbonChip("Multiple", viewMode == DocumentViewMode.MULTIPLE_PAGES) { onViewModeChange(DocumentViewMode.MULTIPLE_PAGES) }
            RibbonChip("Single", viewMode == DocumentViewMode.SINGLE_PAGE) { onViewModeChange(DocumentViewMode.SINGLE_PAGE) }
            RibbonChip("Page Width", viewMode == DocumentViewMode.PAGE_WIDTH) { onViewModeChange(DocumentViewMode.PAGE_WIDTH) }
        }
        RibbonGroup("Zoom") {
            RibbonIconButton("zoom_out", true) { onZoomChange((zoom - 0.1f).coerceAtLeast(0.6f)) }
            RibbonChip("${(zoom * 100).toInt()}%", true) { onZoomChange(1f) }
            RibbonIconButton("zoom_in", true) { onZoomChange((zoom + 0.1f).coerceAtMost(2f)) }
            RibbonIconButton("settings", true, onPageSetup)
        }
    }
}

// ============================================================================================
// WORD RIBBON PRIMITIVES
// ============================================================================================

@Composable
private fun CompactRibbonSurface(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(WordRibbonBackground)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        content = content
    )
}

@Composable
private fun RibbonGroup(label: String, content: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            content = content
        )
        Text(label, fontSize = 6.2.sp, color = Color(0xFF666666), maxLines = 1)
    }
    RibbonDivider()
}

@Composable
private fun RibbonToggleLabeled(icon: String, selected: Boolean, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(45.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
            .background(if (selected) WordRibbonAccent.copy(alpha = 0.13f) else Color.Transparent)
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = if (selected) WordRibbonAccent else Color(0xFF333333), modifier = Modifier.size(16.dp), filled = selected)
        Text(label, fontSize = 6.5.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ============================================================================================
// WORD-STYLE COMMAND MENUS
// Compact desktop-style menus used consistently across ribbon and document surfaces.
// ============================================================================================

@Composable
fun WordDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(3.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(min = 190.dp, max = 270.dp)
    ) {
        Column(Modifier.padding(vertical = 3.dp), content = content)
    }
}

@Composable
fun WordMenuItem(
    icon: String? = null,
    label: String,
    shortcut: String? = null,
    enabled: Boolean = true,
    checked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(31.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (checked) {
                FluentIcon("checkmark", "Selected", modifier = Modifier.size(14.dp), tint = WordRibbonAccent)
            } else if (icon != null) {
                FluentIcon(icon, label, modifier = Modifier.size(15.dp), tint = if (enabled) Color(0xFF333333) else Color(0xFFAAAAAA))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label, fontSize = 11.sp, color = if (enabled) Color(0xFF222222) else Color(0xFF999999),
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (shortcut != null) {
            Text(shortcut, fontSize = 9.sp, color = Color(0xFF777777))
        }
    }
}

@Composable
fun WordMenuDivider() {
    HorizontalDivider(Modifier.padding(vertical = 3.dp), color = Color(0xFFE4E4E4))
}

// ============================================================================================
// SMALL RIBBON HELPERS
// ============================================================================================

@Composable fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(37.dp).padding(vertical = 3.dp).background(WordRibbonBorder))
}

@Composable
fun RibbonChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 7.5.sp, color = Color(0xFF202020), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(2.dp))
            .background(WordRibbonSurface)
            .border(1.dp, WordRibbonBorder, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp)
    )
}

@Composable
fun RibbonIconButton(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(24.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = icon.replace('_', ' ') },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(icon, null, modifier = Modifier.size(15.dp))
    }
}

@Composable
fun RibbonToggleIcon(icon: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(2.dp))
            .background(if (selected) WordRibbonAccent.copy(alpha = 0.15f) else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, if (selected) WordRibbonAccent.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = icon.replace('_', ' ') },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(icon, null, tint = if (selected) WordRibbonAccent else Color(0xFF333333), modifier = Modifier.size(16.dp), filled = selected)
    }
}

@Composable
fun RibbonLabeledAction(icon: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(44.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
            .clickable { onClick() }.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = Color(0xFF333333), modifier = Modifier.size(16.dp))
        Text(label, fontSize = 6.5.sp, textAlign = TextAlign.Center, lineHeight = 7.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
fun CommentDialog(
    quotedText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Comment") },
        text = {
            Column {
                if (quotedText.isNotBlank()) {
                    Text("Selected text", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(quotedText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                }
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Comment") }, minLines = 3)
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text("Post") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CommentsDialog(
    comments: List<DocumentComment>,
    onResolve: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments") },
        text = {
            if (comments.isEmpty()) Text("No comments yet.") else Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                comments.forEach { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(c.author, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(if (c.resolved) "Resolved" else "Open", fontSize = 11.sp)
                            }
                            if (c.quotedText.isNotBlank()) Text("“${c.quotedText}”", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            Text(c.text, modifier = Modifier.padding(top = 6.dp))
                            Row {
                                if (!c.resolved) TextButton(onClick = { onResolve(c.id) }) { Text("Resolve") }
                                TextButton(onClick = { onDelete(c.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun NoteDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Note text") }, minLines = 4)
    }, confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text("Insert") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
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
fun SymbolDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    val symbols = listOf(
        "©", "®", "™", "§", "¶", "†", "‡", "•", "…", "—", "–",
        "“", "”", "‘", "’", "«", "»", "°", "±", "×", "÷", "≤", "≥",
        "≠", "≈", "∞", "√", "π", "µ", "←", "→", "↑", "↓", "↔", "✓"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Symbol") },
        text = {
            Column {
                Text("Choose a symbol to insert at the cursor.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(symbols.size) { index ->
                        OutlinedButton(
                            onClick = { onInsert(symbols[index]) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(44.dp)
                        ) { Text(symbols[index], fontSize = 20.sp) }
                    }
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
                        FluentIcon("dismiss", null, modifier = Modifier.size(12.dp))
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

// ============================================================================================
// COMPACT CONTEXTUAL RIBBONS — shown only when an object is selected.
// These intentionally stay small so the desktop-style UI remains usable on phones.
// ============================================================================================

@Composable
fun PictureContextRibbon(
    onDelete: () -> Unit,
    onScale: (Float) -> Unit,
    onSetScale: (Int) -> Unit,
    onAlign: (TextAlign) -> Unit,
    onClose: () -> Unit
) {
    CompactRibbonRow {
        RibbonGroup(label = "Picture") {
            RibbonLabeledAction("image", "Picture", onClose)
        }
        RibbonGroup(label = "Size") {
            RibbonCompactAction("arrow_expand", "50%") { onSetScale(50) }
            RibbonCompactAction("arrow_expand", "100%") { onSetScale(100) }
            RibbonCompactAction("arrow_expand", "150%") { onSetScale(150) }
        }
        RibbonGroup(label = "Align") {
            RibbonCompactAction("text_align_left", "Left") { onAlign(TextAlign.Start) }
            RibbonCompactAction("text_align_center", "Center") { onAlign(TextAlign.Center) }
            RibbonCompactAction("text_align_right", "Right") { onAlign(TextAlign.End) }
        }
        RibbonGroup(label = "Arrange") {
            RibbonCompactAction("delete", "Delete", onDelete)
        }
    }
}

@Composable
fun TableContextRibbon(
    onAddRow: () -> Unit, onAddColumn: () -> Unit,
    onDelete: () -> Unit, onClose: () -> Unit
) {
    CompactRibbonRow {
        RibbonGroup(label = "Table") {
            RibbonLabeledAction("table", "Table", onClose)
        }
        RibbonGroup(label = "Layout") {
            RibbonCompactAction("table_insert_row", "Row", onAddRow)
            RibbonCompactAction("table_insert_column", "Column", onAddColumn)
        }
        RibbonGroup(label = "Arrange") {
            RibbonCompactAction("delete", "Delete", onDelete)
        }
    }
}

@Composable
private fun CompactRibbonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .background(WordRibbonBackground).padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content
    )
}

@Composable
private fun RibbonCompactAction(icon: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(42.dp).clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }.padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


// Compact floating formatting bar shown while text is selected. It mirrors Word's
// mini-toolbar concept without consuming permanent ribbon space on a phone.
@Composable
fun TextSelectionMiniToolbar(
    style: StyleAttrs,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onHighlight: () -> Unit,
    onColor: () -> Unit
) {
    Surface(
        modifier = modifier.wrapContentWidth().height(34.dp),
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 5.dp,
        tonalElevation = 2.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            MiniFormatButton("text_bold", style.bold, enabled, onBold)
            MiniFormatButton("text_italic", style.italic, enabled, onItalic)
            MiniFormatButton("text_underline", style.underline, enabled, onUnderline)
            MiniFormatButton("subtract", false, enabled) { onFontSizeChange((style.fontSize - 1).coerceAtLeast(8)) }
            Text(style.fontSize.toString(), fontSize = 9.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
            MiniFormatButton("add", false, enabled) { onFontSizeChange((style.fontSize + 1).coerceAtMost(96)) }
            MiniFormatButton("color_background", false, enabled, onHighlight)
            MiniFormatButton("text_color", false, enabled, onColor)
        }
    }
}

@Composable
private fun MiniFormatButton(icon: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(3.dp))
            .background(if (selected) WordRibbonAccent.copy(alpha = 0.13f) else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, if (selected) WordRibbonAccent.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = icon.replace('_', ' ') },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(icon, modifier = Modifier.size(15.dp), tint = if (selected) WordRibbonAccent else Color(0xFF303030), filled = selected)
    }
}
