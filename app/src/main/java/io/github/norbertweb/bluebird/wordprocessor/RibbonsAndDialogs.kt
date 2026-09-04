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

@Composable
private fun wordRibbonBackground(): Color = rememberWordFluentPalette().ribbonBackground

@Composable
private fun wordRibbonSurface(): Color = rememberWordFluentPalette().ribbonSurface

@Composable
fun wordRibbonBorder(): Color = rememberWordFluentPalette().border

@Composable
fun wordRibbonAccent(): Color = rememberWordFluentPalette().accent

@Composable
private fun wordRibbonText(): Color = rememberWordFluentPalette().text

@Composable
private fun wordRibbonSecondaryText(): Color = rememberWordFluentPalette().secondaryText

@Composable
private fun wordRibbonDisabledText(): Color = rememberWordFluentPalette().secondaryText.copy(alpha = 0.55f)

@Composable
private fun wordRibbonMenuSurface(): Color = rememberWordFluentPalette().ribbonSurface


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
            .background(wordRibbonBackground())
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Clipboard: Word uses one visually dominant Paste command and smaller Cut/Copy.
        RibbonGroup("Clipboard") {
            Box {
                Column(
                    modifier = Modifier.width(44.dp).heightIn(min = 40.dp).clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = enabled) { onPaste() }
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FluentIcon("clipboard_paste", modifier = Modifier.size(18.dp), tint = wordRibbonAccent())
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
                    modifier = Modifier.width(38.dp).height(28.dp).clip(RoundedCornerShape(4.dp))
                        .background(wordRibbonSurface()).border(1.dp, wordRibbonBorder())
                ) {
                    BasicTextField(
                        value = fontSizeText,
                        onValueChange = { fontSizeText = it.filter(Char::isDigit).take(2) },
                        enabled = enabled,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = wordRibbonText(), textAlign = TextAlign.Center),
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
                        Box(Modifier.size(22.dp).clip(CircleShape).background(wordRibbonSurface()).border(1.dp, wordRibbonBorder()).clickable { onHighlightChange(null); showHighlightPicker = false }) {
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
                val alignmentIcon = when (alignment) {
                    TextAlign.Center -> "text_align_center"
                    TextAlign.End -> "text_align_right"
                    TextAlign.Justify -> "text_align_justify_low"
                    else -> "text_align_left"
                }
                RibbonToggleIcon(
                    alignmentIcon,
                    selected = enabled && alignment != null,
                    enabled = enabled,
                    onClick = { showAlignMenu = true }
                )
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
                            text = {
                                Text(
                                    st.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (st.bold) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (st.id == currentStyleId) {
                                { FluentIcon("checkmark", modifier = Modifier.size(15.dp), tint = wordRibbonAccent()) }
                            } else null,
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
            .background(wordRibbonSurface()).border(1.dp, wordRibbonBorder())
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 9.sp, color = wordRibbonText(), maxLines = 1, modifier = Modifier.weight(1f))
        FluentIcon("chevron_down", modifier = Modifier.size(11.dp), tint = wordRibbonSecondaryText())
    }
}

@Composable
private fun RibbonStyleGallery(currentStyleId: String, enabled: Boolean, onStyleChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (currentStyleId == "__mixed__") {
            Box(Modifier.width(58.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(wordRibbonSurface()).border(1.dp, wordRibbonBorder())) {
                Text("Multiple", fontSize = 8.sp, color = wordRibbonSecondaryText(), modifier = Modifier.align(Alignment.Center))
            }
        }
        BuiltInStyles.ALL.take(4).forEach { st ->
            val selected = st.id == currentStyleId
            Text(
                st.name, fontSize = 7.sp, maxLines = 1,
                modifier = Modifier.width(48.dp).height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) wordRibbonAccent().copy(alpha = 0.14f) else wordRibbonSurface())
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
            .background(wordRibbonBackground().copy(alpha = 0.9f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon("chevron_down", modifier = Modifier.size(9.dp), tint = wordRibbonSecondaryText())
    }
}

// ============================================================================================
// INSERT RIBBON
// ============================================================================================

@Composable
fun InsertRibbon(
    enabled: Boolean = true,
    onInsertDate: () -> Unit,
    onInsertDivider: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertBlankPage: () -> Unit,
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
            RibbonLabeledAction("document_add", "Blank Page", onInsertBlankPage, enabled = enabled)
            RibbonLabeledAction("document", "Page Break", onInsertPageBreak, enabled = enabled)
        }
        RibbonGroup("Tables") {
            RibbonLabeledAction("table", "Table", onInsertTable, enabled = enabled)
        }
        RibbonGroup("Illustrations") {
            RibbonLabeledAction("image", "Pictures", onInsertImage, enabled = enabled)
        }
        RibbonGroup("Links") {
            RibbonLabeledAction("link", "Link", onInsertLink, enabled = enabled)
            RibbonLabeledAction("bookmark", "Bookmark", onInsertBookmark, enabled = enabled)
        }
        RibbonGroup("Header & Footer") {
            RibbonLabeledAction("text_header_1", "Header", onToggleHeader, enabled = enabled)
            RibbonLabeledAction("text_footer", "Footer", onToggleFooter, enabled = enabled)
            RibbonLabeledAction("number_symbol", "Page #", onInsertPageNumberField, enabled = enabled)
        }
        RibbonGroup("Text") {
            RibbonLabeledAction("calendar", "Date", onInsertDate, enabled = enabled)
            RibbonLabeledAction("text_insert", "Divider", onInsertDivider, enabled = enabled)
        }
        RibbonGroup("Symbols") {
            RibbonLabeledAction("omega", "Symbol", onInsertSymbol, enabled = enabled)
        }
    }
}

// ============================================================================================
// LAYOUT RIBBON
// ============================================================================================

@Composable
fun LayoutRibbon(
    onPageSetup: () -> Unit,
    onMargins: () -> Unit,
    onMarginsPreset: (Float) -> Unit,
    onOrientationChange: (String) -> Unit,
    onIndentChange: (Float) -> Unit,
    onSpacingChange: (Int, Int) -> Unit,
    currentOrientation: String,
    currentParagraph: ParagraphBlock?,
    enabled: Boolean
) {
    var showOrientationMenu by remember { mutableStateOf(false) }
    var showMarginsMenu by remember { mutableStateOf(false) }
    var showSpacingMenu by remember { mutableStateOf(false) }
    CompactRibbonSurface {
        RibbonGroup("Page Setup") {
            RibbonLabeledAction("document_text", "Size", onPageSetup)
            Box {
                RibbonLabeledAction("settings", if (currentOrientation == "landscape") "Landscape" else "Portrait", onClick = { showOrientationMenu = true }, enabled = enabled)
                DropdownMenu(expanded = showOrientationMenu, onDismissRequest = { showOrientationMenu = false }) {
                    DropdownMenuItem(text = { Text("Portrait") }, onClick = { onOrientationChange("portrait"); showOrientationMenu = false })
                    DropdownMenuItem(text = { Text("Landscape") }, onClick = { onOrientationChange("landscape"); showOrientationMenu = false })
                }
            }
            Box {
                RibbonLabeledAction("text_align_justify_low", "Margins", onClick = { showMarginsMenu = true }, enabled = enabled)
                DropdownMenu(expanded = showMarginsMenu, onDismissRequest = { showMarginsMenu = false }) {
                    listOf(36f to "Narrow", 54f to "Moderate", 72f to "Normal", 108f to "Wide").forEach { (margin, label) ->
                        DropdownMenuItem(
                            text = { Text("$label (${margin.toInt()} pt)") },
                            onClick = { onMarginsPreset(margin); showMarginsMenu = false }
                        )
                    }
                }
            }
        }
        RibbonGroup("Paragraph") {
            RibbonLabeledAction("text_indent_decrease_ltr", "Indent ←", onClick = { onIndentChange(-18f) }, enabled = enabled)
            RibbonLabeledAction("text_indent_increase_ltr", "Indent →", onClick = { onIndentChange(18f) }, enabled = enabled)
            Box {
                RibbonLabeledAction("text_line_spacing", "Spacing", onClick = { showSpacingMenu = true }, enabled = enabled)
                DropdownMenu(expanded = showSpacingMenu, onDismissRequest = { showSpacingMenu = false }) {
                    listOf(0 to "Tight", 6 to "Compact", 8 to "Normal", 14 to "Relaxed").forEach { (after, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onSpacingChange(0, after); showSpacingMenu = false }
                        )
                    }
                }
            }
        }
        RibbonGroup("Page Background") {
            RibbonLabeledAction("document_text", "Page", onPageSetup)
            RibbonLabeledAction("border_all", "Borders", onPageSetup)
            RibbonLabeledAction("image", "Watermark", onPageSetup)
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
    onInsertFootnote: () -> Unit, onInsertEndnote: () -> Unit, onToggleNavPanel: () -> Unit,
    onCrossReference: () -> Unit = {}
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
            RibbonLabeledAction("book", "Sources", onClick = { })
            RibbonLabeledAction("document_text", "Bibliography", onClick = { })
        }
        RibbonGroup("Captions") {
            RibbonLabeledAction("text_add", "Caption", onClick = { })
            RibbonLabeledAction("list", "Cross-ref", onCrossReference)
        }
        RibbonGroup("Navigate") {
            Box {
                RibbonLabeledAction("bookmark", "Bookmark", onClick = { showBookmarkMenu = true })
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
    commentCount: Int,
    unresolvedCount: Int,
    noteCount: Int,
    onFind: () -> Unit,
    onAddComment: () -> Unit,
    onShowComments: () -> Unit,
    onReviewCenter: () -> Unit,
    onToggleReadOnly: () -> Unit
) {
    CompactRibbonSurface {
        RibbonGroup("Proofing") {
            RibbonLabeledAction("text_grammar_error", "Spelling", onClick = { })
            RibbonLabeledAction("search", "Find", onFind)
            RibbonLabeledAction("document_text", "Word Count", onClick = { })
        }
        RibbonGroup("Comments") {
            RibbonLabeledAction("comment_add", "New Comment", onAddComment, enabled = enabled)
            RibbonLabeledAction("comment", if (unresolvedCount > 0) "Comments $unresolvedCount" else "Comments", onShowComments, enabled = enabled || commentCount > 0)
            RibbonLabeledAction("panel_left", "Review Pane", onReviewCenter, enabled = enabled || commentCount > 0 || noteCount > 0)
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
            .background(wordRibbonBackground())
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
            modifier = Modifier.height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            content = content
        )
        Text(label, fontSize = 6.8.sp, color = wordRibbonSecondaryText(), maxLines = 1)
    }
    RibbonDivider()
}

@Composable
private fun RibbonToggleLabeled(icon: String, selected: Boolean, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(50.dp).height(42.dp).clip(RoundedCornerShape(4.dp))
            .background(if (selected) wordRibbonAccent().copy(alpha = 0.13f) else Color.Transparent)
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = if (selected) wordRibbonAccent() else wordRibbonText(), modifier = Modifier.size(16.dp), filled = selected)
        Text(label, fontSize = 6.8.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        containerColor = wordRibbonMenuSurface(),
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
                FluentIcon("checkmark", "Selected", modifier = Modifier.size(14.dp), tint = wordRibbonAccent())
            } else if (icon != null) {
                FluentIcon(icon, label, modifier = Modifier.size(15.dp), tint = if (enabled) wordRibbonText() else wordRibbonDisabledText())
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label, fontSize = 11.sp, color = if (enabled) wordRibbonText() else wordRibbonDisabledText(),
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (shortcut != null) {
            Text(shortcut, fontSize = 9.sp, color = wordRibbonSecondaryText())
        }
    }
}

@Composable
fun WordMenuDivider() {
    HorizontalDivider(Modifier.padding(vertical = 3.dp), color = wordRibbonBorder())
}

// ============================================================================================
// SMALL RIBBON HELPERS
// ============================================================================================

@Composable fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(37.dp).padding(vertical = 3.dp).background(wordRibbonBorder()))
}

@Composable
fun RibbonChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 7.5.sp, color = wordRibbonText(), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(2.dp))
            .background(wordRibbonSurface())
            .border(1.dp, wordRibbonBorder(), RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp)
    )
}

@Composable
fun RibbonIconButton(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) Color.Transparent else wordRibbonBackground().copy(alpha = 0.35f))
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
        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
            .background(if (selected) wordRibbonAccent().copy(alpha = 0.15f) else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, if (selected) wordRibbonAccent().copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = icon.replace('_', ' ') },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(icon, null, tint = if (selected) wordRibbonAccent() else wordRibbonText(), modifier = Modifier.size(16.dp), filled = selected)
    }
}

@Composable
fun RibbonLabeledAction(
    icon: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = if (enabled) wordRibbonText() else wordRibbonDisabledText()
    Column(
        modifier = Modifier.width(50.dp).height(42.dp).clip(RoundedCornerShape(4.dp))
            .background(if (enabled) Color.Transparent else wordRibbonBackground().copy(alpha = 0.25f))
            .clickable(enabled = enabled) { onClick() }.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 6.8.sp, textAlign = TextAlign.Center, lineHeight = 7.4.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
fun ReviewCenterDialog(
    comments: List<DocumentComment>,
    notes: List<DocumentNote>,
    onJump: (String) -> Unit,
    onResolve: (String, Boolean) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showResolved by remember { mutableStateOf(true) }
    val visibleComments = comments.filter { showResolved || !it.resolved }
    val unresolved = comments.count { !it.resolved }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Review center")
                Spacer(Modifier.height(4.dp))
                Text(
                    "${comments.size} comments • $unresolved open • ${notes.size} notes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(Modifier.heightIn(max = 500.dp)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Comments") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Notes") })
                }
                Spacer(Modifier.height(8.dp))
                if (tab == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showResolved, onCheckedChange = { showResolved = it })
                        Text("Show resolved", fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    if (visibleComments.isEmpty()) {
                        Text("No comments match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            visibleComments.forEach { c ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(7.dp).clip(CircleShape).background(
                                                if (c.resolved) MaterialTheme.colorScheme.outline else bluebirdColors.AccentBlue
                                            ))
                                            Spacer(Modifier.width(7.dp))
                                            Text(c.author.ifBlank { "Author" }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(if (c.resolved) "Resolved" else "Open", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (c.quotedText.isNotBlank()) {
                                            Text(
                                                "“${c.quotedText}”", fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 3, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 5.dp)
                                            )
                                        }
                                        Text(c.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { onJump(c.blockId); onDismiss() }, enabled = c.blockId.isNotBlank()) { Text("Go to") }
                                            TextButton(onClick = { onResolve(c.id, !c.resolved) }) { Text(if (c.resolved) "Reopen" else "Resolve") }
                                            TextButton(onClick = { onDeleteComment(c.id) }) { Text("Delete") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (notes.isEmpty()) {
                        Text("No footnotes or endnotes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            notes.forEach { n ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${n.marker}.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (n.isEndnote) "Endnote" else "Footnote", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(n.text, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { onJump(n.blockId); onDismiss() }, enabled = n.blockId.isNotBlank()) { Text("Go to") }
                                        }
                                    }
                                }
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
fun CrossReferenceDialog(
    targets: List<ReferenceTarget>,
    onDismiss: () -> Unit,
    onInsert: (ReferenceTarget, Boolean) -> Unit
) {
    var selectedId by remember(targets) { mutableStateOf(targets.firstOrNull()?.id) }
    var includePage by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cross-reference") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                Text("Reference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (targets.isEmpty()) {
                    Text("Add a heading or bookmark first.", fontSize = 12.sp)
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        targets.forEach { target ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .background(if (target.id == selectedId) bluebirdColors.AccentBlue.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable { selectedId = target.id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = target.id == selectedId, onClick = { selectedId = target.id })
                                Text(target.label, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includePage, onCheckedChange = { includePage = it })
                    Text("Include page number", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selectedId != null, onClick = { targets.firstOrNull { it.id == selectedId }?.let { onInsert(it, includePage) } }) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
            .background(wordRibbonBackground()).padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content
    )
}

@Composable
private fun RibbonCompactAction(icon: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(48.dp).heightIn(min = 42.dp).clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }.padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FluentIcon(icon, null, tint = wordRibbonAccent(), modifier = Modifier.size(18.dp))
        Text(label, color = wordRibbonText(), fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onColor: () -> Unit,
    onCopy: () -> Unit = {},
    onCut: () -> Unit = {},
    onClearFormatting: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Surface(
        modifier = modifier.wrapContentWidth().heightIn(min = 38.dp, max = 44.dp),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = wordRibbonMenuSurface()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MiniFormatButton("text_bold", style.bold, enabled, onBold)
            MiniFormatButton("text_italic", style.italic, enabled, onItalic)
            MiniFormatButton("text_underline", style.underline, enabled, onUnderline)
            RibbonMiniDivider()
            MiniFormatButton("subtract", false, enabled) { onFontSizeChange((style.fontSize - 1).coerceAtLeast(8)) }
            Text(style.fontSize.toString(), fontSize = 9.sp, color = wordRibbonText(), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            MiniFormatButton("add", false, enabled) { onFontSizeChange((style.fontSize + 1).coerceAtMost(96)) }
            RibbonMiniDivider()
            MiniFormatButton("color_background", false, enabled, onHighlight)
            MiniFormatButton("text_color", false, enabled, onColor)
            RibbonMiniDivider()
            MiniFormatButton("content_copy", false, enabled, onCopy)
            MiniFormatButton("content_cut", false, enabled, onCut)
            MiniFormatButton("text_clear_formatting", false, enabled, onClearFormatting)
            RibbonMiniDivider()
            MiniFormatButton("dismiss", false, enabled, onDismiss)
        }
    }
}

@Composable
private fun RibbonMiniDivider() {
    Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(20.dp).background(wordRibbonBorder()))
}

@Composable
private fun MiniFormatButton(icon: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(3.dp))
            .background(if (selected) wordRibbonAccent().copy(alpha = 0.13f) else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, if (selected) wordRibbonAccent().copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = icon.replace('_', ' ') },
        contentAlignment = Alignment.Center
    ) {
        FluentIcon(icon, modifier = Modifier.size(15.dp), tint = if (selected) wordRibbonAccent() else wordRibbonText(), filled = selected)
    }
}
