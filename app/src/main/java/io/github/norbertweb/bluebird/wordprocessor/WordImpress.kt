package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// PhoneScreen.kt — main composable, state, and shell.
//
// NOTE: kept named `PhoneScreen` on purpose so existing navigation/routing that points at this
// screen keeps working. Functionally this is "Word Impress": a desktop-Word-inspired document
// editor with ribbon toolbar, real pagination, .wdoc zip packaging, PDF export/import, autosave
// with crash recovery, undo/redo, find & replace, bookmarks, a headings navigation panel, and
// more — see the file-level comments in WdocModel.kt, RichTextEngine.kt, Pagination.kt, and
// WdocIO.kt for the pieces this file wires together.
//
// UNDO/REDO SCOPE (documented, not hidden): snapshots are pushed before ribbon-triggered actions
// (formatting, paragraph style, lists/indent, inserting an image/table/page break/TOC/link) —
// not on every keystroke, and not for the small inline delete/resize buttons on an existing
// image or table. That keeps the history meaningful without needing a keystroke debounce timer;
// it's a good next increment if finer-grained undo is wanted later.
// ============================================================================================

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object WordTheme {
    val ribbonBlue = Color(0xFF2B579A)
    val ribbonBlueDark = Color(0xFF1C3B6B)
    val pageWhite = Color(0xFFFFFFFF)
    val darkCanvas = Color(0xFF121212)
    val darkSurface = Color(0xFF1E1E1E)
    val darkPage = Color(0xFF262626)
}

@Composable
fun PhoneScreen(isDark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val appBg = if (isDark) WordTheme.darkCanvas else Color(0xFFE8EAED)
    val pageColor = if (isDark) WordTheme.darkPage else WordTheme.pageWhite
    val ribbonBg = if (isDark) WordTheme.ribbonBlueDark else WordTheme.ribbonBlue
    val ribbonStripBg = if (isDark) WordTheme.darkSurface else Color(0xFFF3F2F1)

    val appSettings = remember { AppSettings() }
    val documents = remember { mutableStateListOf(WordDocument("Document1", appSettings.defaultPageSettings)) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val currentDoc = documents[currentIndex.coerceIn(0, documents.lastIndex)]

    var focusedParagraph by remember(currentDoc.id) {
        mutableStateOf(currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull())
    }
    var focusedTopIndex by remember(currentDoc.id) { mutableStateOf(0) }
    val jumpRequesters = remember(currentDoc.id) { mutableStateMapOf<String, BringIntoViewRequester>() }

    var ribbonTab by remember { mutableStateOf(RibbonTab.HOME) }
    var sidebarOpen by remember { mutableStateOf(true) }
    var navPanelOpen by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1f) }
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showPageSetupDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveredDraftFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }

    fun notify(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // ---- crash recovery: check once for leftover drafts from an unclean shutdown -------------
    LaunchedEffect(Unit) {
        val drafts = listDraftFiles(context)
        if (drafts.isNotEmpty()) {
            recoveredDraftFiles = drafts
            showRecoveryDialog = true
        }
    }

    // ---- autosave: writes a local draft every tick, and silently saves to the real file ------
    // (when one is already chosen) so the person never loses more than one interval of work.
    LaunchedEffect(currentDoc.id) {
        while (true) {
            delay(appSettings.autosaveIntervalSec.coerceAtLeast(5).toLong() * 1000L)
            if (!appSettings.autosaveEnabled) continue
            if (currentDoc.kind != DocKind.RICH_TEXT) continue
            saveDraft(context, currentDoc)
            val uri = currentDoc.savedUri
            if (uri != null && currentDoc.isDirty) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocumentZip(currentDoc)) }
                    currentDoc.isDirty = false
                } catch (_: Exception) { /* try again next tick */ }
            }
        }
    }

    fun loadPdfPages(uri: Uri) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) { notify("Couldn't open PDF"); return }
            val renderer = PdfRenderer(pfd)
            val pages = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages.add(bmp)
                }
            }
            renderer.close()
            pfd.close()
            pdfPages = pages
        } catch (e: Exception) {
            e.printStackTrace()
            notify("Failed to read PDF: ${e.message}")
        }
    }

    // ---- file pickers -------------------------------------------------------------------
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val mime = context.contentResolver.getType(uri) ?: ""
            if (mime.contains("pdf") || uri.toString().endsWith(".pdf", true)) {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                val doc = WordDocument(name, appSettings.defaultPageSettings).apply { kind = DocKind.PDF; pdfUri = uri }
                documents.add(doc)
                currentIndex = documents.lastIndex
                ribbonTab = RibbonTab.HOME
                loadPdfPages(uri)
            } else {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported document"
                val doc = openAnyDocument(bytes, name).apply { savedUri = uri }
                documents.add(doc)
                currentIndex = documents.lastIndex
                ribbonTab = RibbonTab.HOME
            }
            notify("Opened")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Couldn't open file")
        }
    }

    val openPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
        val doc = WordDocument(name, appSettings.defaultPageSettings).apply { kind = DocKind.PDF; pdfUri = uri }
        documents.add(doc)
        currentIndex = documents.lastIndex
        loadPdfPages(uri)
    }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocumentZip(currentDoc)) }
            currentDoc.savedUri = uri
            currentDoc.isDirty = false
            deleteDraft(context, currentDoc.id)
            notify("Saved")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Save failed")
        }
    }

    val createTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(plainTextOf(currentDoc).toByteArray()) }
            notify("Saved")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Save failed")
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val pdf = exportDocToPdf(currentDoc)
            context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            pdf.close()
            notify("Exported to PDF")
        } catch (e: Exception) {
            e.printStackTrace(); notify("PDF export failed")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                currentDoc.pushUndoSnapshot()
                val img = ImageBlock().apply {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                }
                val insertAt = (focusedTopIndex + 1).coerceIn(0, currentDoc.blocks.size)
                currentDoc.blocks.add(insertAt, img)
                focusedTopIndex = insertAt
                currentDoc.isDirty = true
            } else notify("Couldn't read image")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Couldn't insert image")
        }
    }

    fun saveCurrent() {
        val uri = currentDoc.savedUri
        if (uri == null) {
            createDocLauncher.launch("${currentDoc.title}.wdoc")
        } else {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocumentZip(currentDoc)) }
                currentDoc.isDirty = false
                deleteDraft(context, currentDoc.id)
                notify("Saved")
            } catch (e: Exception) {
                e.printStackTrace(); notify("Save failed")
            }
        }
    }

    fun newDocument() {
        documents.add(WordDocument("Document${documents.size + 1}", appSettings.defaultPageSettings))
        currentIndex = documents.lastIndex
        ribbonTab = RibbonTab.HOME
    }

    fun duplicateCurrent() {
        val clone = duplicateDocument(currentDoc)
        documents.add(clone)
        currentIndex = documents.lastIndex
        notify("Duplicated as \"${clone.title}\"")
    }

    fun deleteDocumentAt(i: Int) {
        if (documents.size <= 1) { notify("Can't delete the only open document"); return }
        deleteDraft(context, documents[i].id)
        documents.removeAt(i)
        if (currentIndex >= documents.size) currentIndex = documents.lastIndex
    }

    fun insertBlockAfterFocus(block: Block) {
        currentDoc.pushUndoSnapshot()
        val insertAt = (focusedTopIndex + 1).coerceIn(0, currentDoc.blocks.size)
        currentDoc.blocks.add(insertAt, block)
        focusedTopIndex = insertAt
        currentDoc.isDirty = true
    }

    // ---- undo / redo ----------------------------------------------------------------------
    fun undoAction() {
        if (currentDoc.undo()) {
            focusedParagraph = currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull()
            notify("Undo")
        } else notify("Nothing to undo")
    }

    fun redoAction() {
        if (currentDoc.redo()) {
            focusedParagraph = currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull()
            notify("Redo")
        } else notify("Nothing to redo")
    }

    // ---- formatting helpers (operate on the currently focused paragraph) -----------------
    fun toggleAttribute(pick: (StyleAttrs) -> Boolean, set: (StyleAttrs, Boolean) -> StyleAttrs) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = set(p.typingStyle, !pick(p.typingStyle))
        } else {
            currentDoc.pushUndoSnapshot()
            val newVal = !rangeHas(p.spans, sel.min, sel.max, pick)
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> set(s, newVal) }
        }
        currentDoc.isDirty = true
    }

    fun applyFontSize(size: Int) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(fontSize = size)
        } else {
            currentDoc.pushUndoSnapshot()
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(fontSize = size) }
        }
        currentDoc.isDirty = true
    }

    fun applyColor(color: Color) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(color = color)
        } else {
            currentDoc.pushUndoSnapshot()
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(color = color) }
        }
        currentDoc.isDirty = true
    }

    fun applyHighlight(color: Color?) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(highlight = color)
        } else {
            currentDoc.pushUndoSnapshot()
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(highlight = color) }
        }
        currentDoc.isDirty = true
    }

    fun applyFont(font: FontChoice) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(font = font)
        } else {
            currentDoc.pushUndoSnapshot()
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(font = font) }
        }
        currentDoc.isDirty = true
    }

    fun clearFormatting() {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = base
        } else {
            currentDoc.pushUndoSnapshot()
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { base }
        }
        currentDoc.isDirty = true
    }

    fun setAlign(a: TextAlign) {
        val p = focusedParagraph ?: return
        currentDoc.pushUndoSnapshot()
        p.alignmentOverride = a
        currentDoc.isDirty = true
    }

    fun applyParagraphStyle(styleId: String) {
        val p = focusedParagraph ?: return
        currentDoc.pushUndoSnapshot()
        p.styleId = styleId
        val base = BuiltInStyles.byId(styleId).baseAttrs()
        p.spans = normalizeAndMerge(emptyList(), p.field.text.length, base)
        p.typingStyle = base
        currentDoc.isDirty = true
    }

    fun toggleList(type: ListType) {
        val p = focusedParagraph ?: return
        currentDoc.pushUndoSnapshot()
        p.listType = if (p.listType == type) null else type
        currentDoc.isDirty = true
    }

    fun changeIndent(delta: Int) {
        val p = focusedParagraph ?: return
        currentDoc.pushUndoSnapshot()
        p.listLevel = (p.listLevel + delta).coerceIn(0, 4)
        currentDoc.isDirty = true
    }

    fun insertTextAtCursor(text: String) {
        val p = focusedParagraph ?: return
        currentDoc.pushUndoSnapshot()
        val sel = p.field.selection
        val old = p.field.text
        val newText = old.substring(0, sel.start) + text + old.substring(sel.end)
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val (field, spans) = editParagraphText(old, newText, sel.start + text.length, p.spans, p.typingStyle, base)
        p.spans = spans
        p.field = field
        currentDoc.isDirty = true
    }

    fun insertLinkOnSelection(url: String) {
        val p = focusedParagraph ?: return
        val sel = p.field.selection
        if (sel.collapsed) { notify("Select some text first, then insert the link"); return }
        currentDoc.pushUndoSnapshot()
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s ->
            s.copy(link = url, color = bluebirdColors.AccentBlue, underline = true)
        }
        currentDoc.isDirty = true
    }

    fun addBookmarkAtFocus(name: String) {
        val p = focusedParagraph ?: return
        currentDoc.bookmarks.add(Bookmark(name = name, blockId = p.id))
        currentDoc.isDirty = true
        notify("Bookmark added")
    }

    fun jumpToBlockId(id: String) {
        scope.launch {
            val requester = jumpRequesters[id]
            if (requester != null) requester.bringIntoView() else notify("Scroll to that item, then try again")
        }
    }

    fun regenerateToc(toc: TocBlock) {
        toc.entries.clear()
        toc.entries.addAll(buildTocEntries(currentDoc.blocks))
        currentDoc.isDirty = true
    }

    fun insertToc() {
        val toc = TocBlock().apply { entries.addAll(buildTocEntries(currentDoc.blocks)) }
        insertBlockAfterFocus(toc)
    }

    // ---- find & replace ---------------------------------------------------------------------
    val matches = if (searchQuery.isNotEmpty()) findAllMatches(currentDoc, searchQuery) else emptyList()
    val activeMatch = if (matches.isNotEmpty()) matches[currentMatchIndex.coerceIn(0, matches.lastIndex)] else null

    fun findNext() { if (matches.isNotEmpty()) currentMatchIndex = (currentMatchIndex + 1) % matches.size }
    fun findPrev() { if (matches.isNotEmpty()) currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size }

    fun replaceCurrentMatch() {
        val m = activeMatch ?: return
        val block = currentDoc.blocks.getOrNull(m.blockIndex) as? ParagraphBlock ?: return
        currentDoc.pushUndoSnapshot()
        val old = block.field.text
        val newText = old.substring(0, m.start) + replaceText + old.substring(m.end)
        val base = BuiltInStyles.byId(block.styleId).baseAttrs()
        val (field, spans) = editParagraphText(old, newText, m.start + replaceText.length, block.spans, block.typingStyle, base)
        block.spans = spans
        block.field = field
        currentDoc.isDirty = true
    }

    fun replaceAllMatches() {
        if (searchQuery.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        currentDoc.blocks.filterIsInstance<ParagraphBlock>().forEach { p ->
            if (p.field.text.contains(searchQuery, ignoreCase = true)) {
                val old = p.field.text
                val newText = old.replace(searchQuery, replaceText, ignoreCase = true)
                val base = BuiltInStyles.byId(p.styleId).baseAttrs()
                val (field, spans) = editParagraphText(old, newText, newText.length, p.spans, p.typingStyle, base)
                p.spans = spans
                p.field = field
            }
        }
        currentDoc.isDirty = true
        notify("Replaced all matches")
    }

    val fp = focusedParagraph
    val toolbarStyle: StyleAttrs = if (fp == null) StyleAttrs() else {
        val base = BuiltInStyles.byId(fp.styleId).baseAttrs()
        val sel = fp.field.selection
        if (sel.collapsed) fp.typingStyle
        else StyleAttrs(
            bold = rangeHas(fp.spans, sel.min, sel.max) { it.bold },
            italic = rangeHas(fp.spans, sel.min, sel.max) { it.italic },
            underline = rangeHas(fp.spans, sel.min, sel.max) { it.underline },
            strikethrough = rangeHas(fp.spans, sel.min, sel.max) { it.strikethrough },
            superscript = rangeHas(fp.spans, sel.min, sel.max) { it.superscript },
            subscript = rangeHas(fp.spans, sel.min, sel.max) { it.subscript },
            fontSize = styleAt(fp.spans, sel.min, base).fontSize,
            color = styleAt(fp.spans, sel.min, base).color,
            font = styleAt(fp.spans, sel.min, base).font,
            highlight = styleAt(fp.spans, sel.min, base).highlight
        )
    }
    val toolbarAlign = fp?.let { it.alignmentOverride ?: BuiltInStyles.byId(it.styleId).alignment } ?: TextAlign.Start
    val toolbarStyleId = fp?.styleId ?: "normal"
    val toolbarListType = fp?.listType
    val jumpTargetIds = remember(currentDoc.blocks.size, currentDoc.bookmarks.size) {
        (currentDoc.blocks.filterIsInstance<ParagraphBlock>().filter { it.styleId in BuiltInStyles.HEADING_IDS }.map { it.id } +
            currentDoc.bookmarks.map { it.blockId }).toSet()
    }
    val homeEnabled = currentDoc.kind == DocKind.RICH_TEXT && !currentDoc.readOnly && fp != null

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && currentDoc.kind == DocKind.RICH_TEXT) {
                when (event.key) {
                    Key.B -> { toggleAttribute({ s -> s.bold }, { s, v -> s.copy(bold = v) }); true }
                    Key.I -> { toggleAttribute({ s -> s.italic }, { s, v -> s.copy(italic = v) }); true }
                    Key.U -> { toggleAttribute({ s -> s.underline }, { s, v -> s.copy(underline = v) }); true }
                    Key.S -> { saveCurrent(); true }
                    Key.F -> { showFindReplaceDialog = true; true }
                    Key.Z -> { if (event.isShiftPressed) redoAction() else undoAction(); true }
                    Key.Y -> { redoAction(); true }
                    else -> false
                }
            } else false
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = appBg
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(appBg)) {

            if (!readingMode) {
                // ==================== TITLE BAR ====================
                Row(
                    modifier = Modifier.fillMaxWidth().background(ribbonBg).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { sidebarOpen = !sidebarOpen })
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = currentDoc.title,
                        onValueChange = { t -> currentDoc.title = t; currentDoc.isDirty = true },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    if (currentDoc.isDirty) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF41A5EE)))
                        Spacer(Modifier.width(8.dp))
                    }
                    if (currentDoc.readOnly) {
                        Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp).clickable {
                        if (currentDoc.kind == DocKind.RICH_TEXT && !currentDoc.readOnly) saveCurrent()
                    })
                }

                // ==================== RIBBON TABS ====================
                Row(modifier = Modifier.fillMaxWidth().background(ribbonBg)) {
                    listOf(
                        RibbonTab.FILE to "File", RibbonTab.HOME to "Home", RibbonTab.INSERT to "Insert",
                        RibbonTab.REFERENCES to "References", RibbonTab.VIEW to "View"
                    ).forEach { (tab, label) ->
                        val selected = ribbonTab == tab
                        Text(
                            label, color = Color.White, fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { ribbonTab = tab }
                                .background(if (selected) ribbonStripBg.copy(alpha = 0.25f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // ==================== BACKSTAGE (File tab) or RIBBON STRIP ====================
                if (ribbonTab == RibbonTab.FILE) {
                    FileBackstage(
                        isDark = isDark, textColor = textColor, bg = ribbonStripBg, documents = documents,
                        currentIndex = currentIndex,
                        onSelectDoc = { i -> currentIndex = i; ribbonTab = RibbonTab.HOME },
                        onNew = { newDocument() },
                        onOpen = { openDocLauncher.launch(arrayOf("text/*", "application/pdf", "*/*")) },
                        onOpenPdf = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                        onSave = { saveCurrent() },
                        onSaveAs = { showSaveAsDialog = true },
                        onSaveCopy = { duplicateCurrent() },
                        onExportPdf = { exportPdfLauncher.launch("${currentDoc.title}.pdf") },
                        onProperties = { showPropertiesDialog = true },
                        onPageSetup = { showPageSetupDialog = true },
                        onSettings = { showSettingsDialog = true },
                        onClose = { ribbonTab = RibbonTab.HOME }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth().background(ribbonStripBg)) {
                        when (ribbonTab) {
                            RibbonTab.HOME -> HomeRibbon(
                                enabled = homeEnabled,
                                typingOrSelectionStyle = toolbarStyle, alignment = toolbarAlign,
                                currentStyleId = toolbarStyleId, listType = toolbarListType,
                                onBold = { toggleAttribute({ s -> s.bold }, { s, v -> s.copy(bold = v) }) },
                                onItalic = { toggleAttribute({ s -> s.italic }, { s, v -> s.copy(italic = v) }) },
                                onUnderline = { toggleAttribute({ s -> s.underline }, { s, v -> s.copy(underline = v) }) },
                                onStrikethrough = { toggleAttribute({ s -> s.strikethrough }, { s, v -> s.copy(strikethrough = v) }) },
                                onSuperscript = { toggleAttribute({ s -> s.superscript }, { s, v -> s.copy(superscript = v, subscript = if (v) false else s.subscript) }) },
                                onSubscript = { toggleAttribute({ s -> s.subscript }, { s, v -> s.copy(subscript = v, superscript = if (v) false else s.superscript) }) },
                                onFontSizeChange = { applyFontSize(it) },
                                onColorChange = { applyColor(it) },
                                onHighlightChange = { applyHighlight(it) },
                                onFontChange = { applyFont(it) },
                                onAlignChange = { setAlign(it) },
                                onBullet = { toggleList(ListType.BULLET) },
                                onNumbered = { toggleList(ListType.NUMBER) },
                                onIndentIncrease = { changeIndent(1) },
                                onIndentDecrease = { changeIndent(-1) },
                                onStyleChange = { applyParagraphStyle(it) },
                                onClearFormatting = { clearFormatting() }
                            )
                            RibbonTab.INSERT -> InsertRibbon(
                                onInsertDate = { insertTextAtCursor(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())) },
                                onInsertDivider = { insertTextAtCursor("\n" + "─".repeat(32) + "\n") },
                                onInsertTable = { showTableDialog = true },
                                onInsertImage = { imagePickerLauncher.launch("image/*") },
                                onInsertPageBreak = { insertBlockAfterFocus(PageBreakBlock()) },
                                onInsertLink = { showLinkDialog = true },
                                onInsertBookmark = { showAddBookmarkDialog = true },
                                onInsertPageNumberField = { insertTextAtCursor("{page}") }
                            )
                            RibbonTab.REFERENCES -> ReferencesRibbon(
                                navPanelOpen = navPanelOpen,
                                bookmarks = currentDoc.bookmarks,
                                onInsertToc = { insertToc() },
                                onJumpToBookmark = { jumpToBlockId(it) },
                                onDeleteBookmark = { id -> currentDoc.bookmarks.removeAll { it.id == id } },
                                onToggleNavPanel = { navPanelOpen = !navPanelOpen }
                            )
                            RibbonTab.VIEW -> ViewRibbon(
                                sidebarOpen = sidebarOpen, zoom = zoom,
                                readOnly = currentDoc.readOnly, readingMode = readingMode,
                                onToggleSidebar = { sidebarOpen = !sidebarOpen },
                                onZoomChange = { zoom = it },
                                onToggleReadOnly = { currentDoc.readOnly = !currentDoc.readOnly },
                                onToggleReadingMode = { readingMode = true },
                                onPageSetup = { showPageSetupDialog = true }
                            )
                            else -> {}
                        }
                    }
                }
            }

            // ==================== MAIN CONTENT ====================
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!readingMode) {
                    AnimatedVisibility(visible = sidebarOpen) {
                        DocumentSidebar(
                            documents = documents, currentIndex = currentIndex, isDark = isDark, textColor = textColor,
                            onSelect = { currentIndex = it },
                            onNew = { newDocument() },
                            onDelete = { deleteDocumentAt(it) },
                            onRename = { i -> currentIndex = i; showRenameDialog = true },
                            onDuplicate = { i -> currentIndex = i; duplicateCurrent() },
                            onProperties = { i -> currentIndex = i; showPropertiesDialog = true }
                        )
                    }
                    AnimatedVisibility(visible = navPanelOpen) {
                        NavigationPanel(doc = currentDoc, textColor = textColor, onJump = { jumpToBlockId(it) })
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (currentDoc.kind == DocKind.PDF) {
                        PdfViewerPane(pages = pdfPages, zoom = zoom, isDark = isDark)
                    } else {
                        PagedDocumentView(
                            doc = currentDoc, zoom = zoom, pageColor = pageColor, textColor = textColor,
                            readOnly = currentDoc.readOnly,
                            matches = matches, activeMatch = activeMatch,
                            jumpRequesters = jumpRequesters, jumpTargetIds = jumpTargetIds,
                            onParagraphFocus = { p -> focusedParagraph = p },
                            onTopIndexFocus = { i -> focusedTopIndex = i },
                            onRegenerateToc = { regenerateToc(it) },
                            onJumpToBlock = { jumpToBlockId(it) }
                        )
                    }
                    if (readingMode) {
                        IconButton(
                            onClick = { readingMode = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                        ) { Icon(Icons.Default.FullscreenExit, "Exit reading mode", tint = Color.White) }
                    }
                }
            }

            if (!readingMode) {
                StatusBar(doc = currentDoc, zoom = zoom, textColor = textColor, ribbonStripBg = ribbonStripBg, onZoomChange = { zoom = it })
            }
        }
    }

    // ==================== DIALOGS ====================

    if (showSaveAsDialog) {
        SaveAsDialog(
            currentTitle = currentDoc.title,
            onDismiss = { showSaveAsDialog = false },
            onConfirm = { newTitle, format ->
                currentDoc.title = newTitle
                showSaveAsDialog = false
                when (format) {
                    SaveFormat.WDOC -> createDocLauncher.launch("$newTitle.wdoc")
                    SaveFormat.TXT -> createTxtLauncher.launch("$newTitle.txt")
                    SaveFormat.PDF -> exportPdfLauncher.launch("$newTitle.pdf")
                }
            }
        )
    }

    if (showTableDialog) {
        InsertTableDialog(
            onDismiss = { showTableDialog = false },
            onConfirm = { rows, cols -> insertBlockAfterFocus(newTable(rows, cols)); showTableDialog = false }
        )
    }

    if (showPageSetupDialog) {
        PageSetupDialog(
            current = currentDoc.pageSettings,
            onDismiss = { showPageSetupDialog = false },
            onConfirm = { settings -> currentDoc.pageSettings = settings; currentDoc.isDirty = true; showPageSetupDialog = false }
        )
    }

    if (showPropertiesDialog) {
        DocumentPropertiesDialog(
            doc = currentDoc,
            onDismiss = { showPropertiesDialog = false },
            onSaveAuthor = { author -> currentDoc.author = author; currentDoc.isDirty = true; showPropertiesDialog = false }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            current = currentDoc.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name -> currentDoc.title = name; currentDoc.isDirty = true; showRenameDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(settings = appSettings, onDismiss = { showSettingsDialog = false })
    }

    if (showFindReplaceDialog) {
        FindReplaceDialog(
            query = searchQuery, replacement = replaceText,
            matchCount = matches.size, currentMatch = currentMatchIndex.coerceIn(0, (matches.size - 1).coerceAtLeast(0)),
            onQueryChange = { searchQuery = it; currentMatchIndex = 0 },
            onReplacementChange = { replaceText = it },
            onNext = { findNext() }, onPrev = { findPrev() },
            onReplaceOne = { replaceCurrentMatch() }, onReplaceAll = { replaceAllMatches() },
            onDismiss = { showFindReplaceDialog = false }
        )
    }

    if (showLinkDialog) {
        LinkDialog(onDismiss = { showLinkDialog = false }, onConfirm = { url -> insertLinkOnSelection(url); showLinkDialog = false })
    }

    if (showAddBookmarkDialog) {
        AddBookmarkDialog(onDismiss = { showAddBookmarkDialog = false }, onConfirm = { name -> addBookmarkAtFocus(name); showAddBookmarkDialog = false })
    }

    if (showRecoveryDialog) {
        RecoveryDialog(
            count = recoveredDraftFiles.size,
            onRestore = {
                recoveredDraftFiles.forEach { f -> loadDraftAsDocument(f)?.let { documents.add(it) }; f.delete() }
                currentIndex = documents.lastIndex
                showRecoveryDialog = false
            },
            onDiscard = {
                recoveredDraftFiles.forEach { it.delete() }
                showRecoveryDialog = false
            }
        )
    }
}

// ============================================================================================
// SHELL PIECES — status bar, document sidebar, file backstage, PDF viewer
// ============================================================================================

@Composable
private fun PdfViewerPane(pages: List<Bitmap>, zoom: Float, isDark: Boolean) {
    if (pages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Loading PDF…", fontSize = 12.sp)
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(if (isDark) WordTheme.darkCanvas else Color(0xFFE8EAED)),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        itemsIndexed(pages) { index, bmp ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bmp.asImageBitmap(), contentDescription = "Page ${index + 1}",
                    modifier = Modifier.width((360 * zoom).dp).shadow(4.dp).background(Color.White)
                )
                Text("Page ${index + 1} of ${pages.size}", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun StatusBar(doc: WordDocument, zoom: Float, textColor: Color, ribbonStripBg: Color, onZoomChange: (Float) -> Unit) {
    val paragraphs = doc.blocks.filterIsInstance<ParagraphBlock>()
    val wordCount = paragraphs.sumOf { p -> p.field.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val charCount = paragraphs.sumOf { it.field.text.length }
    Row(
        modifier = Modifier.fillMaxWidth().background(ribbonStripBg).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when {
                doc.kind == DocKind.PDF -> "PDF document · read-only"
                doc.readOnly -> "Read-only · $wordCount words · $charCount characters"
                else -> "$wordCount words · $charCount characters · ${paragraphs.size} paragraphs"
            },
            color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ZoomOut, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).clickable { onZoomChange((zoom - 0.1f).coerceAtLeast(0.6f)) })
        Slider(value = zoom, onValueChange = onZoomChange, valueRange = 0.6f..2f, modifier = Modifier.width(90.dp).padding(horizontal = 4.dp))
        Icon(Icons.Default.ZoomIn, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).clickable { onZoomChange((zoom + 0.1f).coerceAtMost(2f)) })
        Spacer(Modifier.width(6.dp))
        Text("${(zoom * 100).toInt()}%", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun FileBackstage(
    isDark: Boolean, textColor: Color, bg: Color, documents: List<WordDocument>, currentIndex: Int,
    onSelectDoc: (Int) -> Unit, onNew: () -> Unit, onOpen: () -> Unit, onOpenPdf: () -> Unit,
    onSave: () -> Unit, onSaveAs: () -> Unit, onSaveCopy: () -> Unit, onExportPdf: () -> Unit,
    onProperties: () -> Unit, onPageSetup: () -> Unit, onSettings: () -> Unit, onClose: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 340.dp).background(bg)) {
        Column(
            modifier = Modifier.width(200.dp).fillMaxHeight().verticalScroll(rememberScrollState())
                .background(if (isDark) WordTheme.ribbonBlueDark else WordTheme.ribbonBlue)
                .padding(vertical = 8.dp)
        ) {
            BackstageAction(Icons.Default.NoteAdd, "New", onNew)
            BackstageAction(Icons.Default.FolderOpen, "Open…", onOpen)
            BackstageAction(Icons.Default.PictureAsPdf, "Open PDF…", onOpenPdf)
            BackstageAction(Icons.Default.Save, "Save", onSave)
            BackstageAction(Icons.Default.SaveAs, "Save As…", onSaveAs)
            BackstageAction(Icons.Default.ContentCopy, "Save a Copy", onSaveCopy)
            BackstageAction(Icons.Default.PictureAsPdf, "Export as PDF…", onExportPdf)
            BackstageAction(Icons.Default.Info, "Properties", onProperties)
            BackstageAction(Icons.Default.Description, "Page Setup", onPageSetup)
            BackstageAction(Icons.Default.Tune, "Settings", onSettings)
            Spacer(Modifier.weight(1f).heightIn(min = 8.dp))
            BackstageAction(Icons.Default.Close, "Close", onClose)
        }
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Recent", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            documents.forEachIndexed { i, doc ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectDoc(i) }
                        .background(if (i == currentIndex) bluebirdColors.AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (doc.kind == DocKind.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(doc.title, color = textColor, fontSize = 13.sp)
                        Text(
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(doc.lastModified)),
                            color = textColor.copy(alpha = 0.5f), fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackstageAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun DocumentSidebar(
    documents: List<WordDocument>, currentIndex: Int, isDark: Boolean, textColor: Color,
    onSelect: (Int) -> Unit, onNew: () -> Unit, onDelete: (Int) -> Unit,
    onRename: (Int) -> Unit, onDuplicate: (Int) -> Unit, onProperties: (Int) -> Unit
) {
    Column(
        modifier = Modifier.width(220.dp).fillMaxHeight()
            .background(if (isDark) WordTheme.darkSurface else Color(0xFFF3F2F1))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Documents", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onNew, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(documents.size) { i ->
                var showMenu by remember { mutableStateOf(false) }
                val doc = documents[i]
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(if (i == currentIndex) bluebirdColors.AccentBlue.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (doc.kind == DocKind.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        doc.title, color = textColor, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 6.dp)
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { onRename(i); showMenu = false })
                            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { onDuplicate(i); showMenu = false })
                            DropdownMenuItem(text = { Text("Properties") }, onClick = { onProperties(i); showMenu = false })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(i); showMenu = false })
                        }
                    }
                }
            }
        }
    }
}
