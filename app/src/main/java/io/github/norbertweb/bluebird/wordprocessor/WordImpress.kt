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
import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.FileProvider
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ClipboardParagraph(
    val text: String,
    val spans: List<FormatRange>,
    val styleId: String = "normal",
    val alignment: TextAlign? = null,
    val listType: ListType? = null,
    val listLevel: Int = 0
)

private data class RichClipboardPayload(
    val text: String,
    val spans: List<FormatRange>,
    val sourceBase: StyleAttrs,
    val paragraphs: List<ClipboardParagraph> = emptyList()
)

private var richClipboardPayload: RichClipboardPayload? = null


private object WordTheme {
    // Compact Microsoft Word desktop-inspired chrome: blue title/status bars,
    // neutral ribbon, and a light gray document workspace.
    val wordBlue = Color(0xFF185ABD)
    val wordBlueDark = Color(0xFF103F82)
    val ribbonWhite = Color(0xFFFFFFFF)
    val ribbonNeutral = Color(0xFFF3F2F1)
    val pageWhite = Color(0xFFFFFFFF)
    val darkCanvas = Color(0xFF121212)
    val darkSurface = Color(0xFF1E1E1E)
    val darkPage = Color(0xFF262626)
}

enum class DocumentViewMode { MULTIPLE_PAGES, SINGLE_PAGE, PAGE_WIDTH }

enum class ContextualRibbon { PICTURE, TABLE }

@Composable
fun PhoneScreen(isDark: Boolean, initialPath: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val textColor = if (isDark) bluebirdColors.TextPrimary else bluebirdColors.TextPrimaryLight
    val appBg = if (isDark) WordTheme.darkCanvas else Color(0xFFE8EAED)
    val pageColor = if (isDark) WordTheme.darkPage else WordTheme.pageWhite
    val ribbonBg = if (isDark) WordTheme.wordBlueDark else WordTheme.wordBlue
    val ribbonStripBg = if (isDark) WordTheme.darkSurface else WordTheme.ribbonNeutral

    val appSettings = remember { AppSettings() }
    val documents = remember { mutableStateListOf(WordDocument("Document1", appSettings.defaultPageSettings)) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val currentDoc = documents[currentIndex.coerceIn(0, documents.lastIndex)]

    var focusedParagraph by remember(currentDoc.id) {
        mutableStateOf(currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull())
    }
    var focusedTopIndex by remember(currentDoc.id) { mutableStateOf(0) }
    val jumpRequesters = remember(currentDoc.id) { mutableStateMapOf<String, BringIntoViewRequester>() }
    val jumpTargetIds: Set<String> = jumpRequesters.keys
    var focusTargetParagraphId by remember(currentDoc.id) { mutableStateOf<String?>(null) }
    var documentSelection by remember(currentDoc.id) { mutableStateOf<DocumentSelection?>(null) }
    // Touch selection affordance: native BasicTextField owns the handles; this state only
    // controls the compact Word mini-toolbar so it appears after a real selection exists.
    var showSelectionToolbar by remember(currentDoc.id) { mutableStateOf(false) }

    var ribbonTab by remember { mutableStateOf(RibbonTab.HOME) }
    var contextualRibbon by remember { mutableStateOf<ContextualRibbon?>(null) }
    var selectedBlockId by remember(currentDoc.id) { mutableStateOf<String?>(null) }
    var showRuler by rememberSaveable { mutableStateOf(true) }
    var sidebarOpen by remember { mutableStateOf(false) }
    var navPanelOpen by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(false) }
    var showPageThumbnails by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DocumentViewMode.PAGE_WIDTH) }
    var zoom by remember { mutableStateOf(1f) }
    var pdfPages by remember { mutableStateOf<List<PdfPageInfo>>(emptyList()) }

    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showPageSetupDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showSymbolDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var pendingNoteIsEndnote by remember { mutableStateOf(false) }
    var recoveredDraftFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }

    fun notify(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // ---- crash recovery: check once for leftover drafts from an unclean shutdown -------------
    LaunchedEffect(Unit) {
        val drafts = withContext(kotlinx.coroutines.Dispatchers.IO) { listDraftFiles(context) }
        if (drafts.isNotEmpty()) {
            recoveredDraftFiles = drafts
            showRecoveryDialog = true
        }
    }

    // ---- autosave: writes a local draft every tick, and silently saves to the real file ------
    // (when one is already chosen) so the person never loses more than one interval of work.
    LaunchedEffect(currentDoc.id) {
        while (isActive) {
            delay(appSettings.autosaveIntervalSec.coerceAtLeast(5).toLong() * 1000L)
            if (!appSettings.autosaveEnabled || currentDoc.kind != DocKind.RICH_TEXT) continue

            val doc = currentDoc
            val uri = doc.savedUri
            val wasDirty = doc.isDirty
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                saveDraft(context, doc)
                if (uri != null && wasDirty) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocumentZip(doc)) }
                    }
                }
            }
            if (wasDirty && uri != null && doc === currentDoc) {
                currentDoc.isDirty = false
            }
        }
    }

    fun loadPdfPages(uri: Uri) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val pages = ArrayList<PdfPageInfo>(renderer.pageCount)
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                pages.add(PdfPageInfo(i, page.width, page.height))
                            }
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) { pdfPages = pages }
                    }
                } ?: withContext(kotlinx.coroutines.Dispatchers.Main) { notify("Couldn't open PDF") }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) { notify("Failed to read PDF: ${e.message}") }
            }
        }
    }


    // Files launched from Bluebird File Explorer open directly in Word Impress.
    // Use the app's FileProvider URI so PDF/document loading follows the same
    // content-URI path as the built-in picker.
    LaunchedEffect(initialPath) {
        if (initialPath.isBlank()) return@LaunchedEffect
        val file = File(initialPath)
        if (!file.isFile) return@LaunchedEffect
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = context.contentResolver.getType(uri) ?: ""
            if (file.extension.equals("pdf", true) || mime.contains("pdf")) {
                val doc = WordDocument(file.name, appSettings.defaultPageSettings).apply {
                    kind = DocKind.PDF
                    pdfUri = uri
                }
                documents.add(doc)
                currentIndex = documents.lastIndex
                loadPdfPages(uri)
            } else {
                val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) { file.readBytes() }
                val doc = openAnyDocument(bytes, file.name).apply { savedUri = uri }
                documents.add(doc)
                currentIndex = documents.lastIndex
            }
            notify("Opened ${file.name}")
        } catch (e: Exception) {
            notify("Couldn't open ${file.name}")
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

    fun shareCurrentDocument() {
        if (currentDoc.kind != DocKind.RICH_TEXT) {
            notify("PDF documents are read-only here")
            return
        }
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "${currentDoc.title.ifBlank { "Document" }}.wdoc")
                file.writeBytes(serializeDocumentZip(currentDoc))
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, currentDoc.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Share document"))
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) { notify("Couldn't share document") }
            }
        }
    }

    fun printCurrentDocument() {
        if (currentDoc.kind != DocKind.RICH_TEXT) {
            notify("PDF printing is not available from this view")
            return
        }
        try {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                notify("Printing isn't available on this device")
                return
            }
            val docSnapshot = currentDoc
            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?, extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) { callback?.onLayoutCancelled(); return }
                    callback?.onLayoutFinished(
                        PrintDocumentInfo.Builder(docSnapshot.title.ifBlank { "Document" })
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build(),
                        true
                    )
                }

                override fun onWrite(
                    pages: Array<android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        if (cancellationSignal?.isCanceled == true) { callback?.onWriteCancelled(); return }
                        val dest = destination ?: run { callback?.onWriteFailed("No print destination"); return }
                        val pdf = exportDocToPdf(docSnapshot)
                        ParcelFileDescriptor.AutoCloseOutputStream(dest).use { out -> pdf.writeTo(out) }
                        pdf.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(docSnapshot.title.ifBlank { "Document" }, adapter, PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build())
        } catch (e: Exception) {
            notify("Couldn't start printing")
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
    fun restoreEditorFocus(previousId: String?, previousSelection: TextRange = TextRange.Zero) {
        val target = previousId?.let { id -> currentDoc.blocks.firstOrNull { it.id == id } as? ParagraphBlock }
            ?: currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull()
        focusedParagraph = target
        target?.let {
            val max = it.field.text.length
            val start = previousSelection.start.coerceIn(0, max)
            val end = previousSelection.end.coerceIn(0, max)
            it.field = it.field.copy(selection = TextRange(start, end))
            focusTargetParagraphId = it.id
        }
    }

    fun undoAction() {
        val previousId = focusedParagraph?.id
        val previousSelection = focusedParagraph?.field?.selection ?: TextRange.Zero
        if (currentDoc.undo()) {
            restoreEditorFocus(previousId, previousSelection)
            notify("Undo")
        } else notify("Nothing to undo")
    }

    fun redoAction() {
        val previousId = focusedParagraph?.id
        val previousSelection = focusedParagraph?.field?.selection ?: TextRange.Zero
        if (currentDoc.redo()) {
            restoreEditorFocus(previousId, previousSelection)
            notify("Redo")
        } else notify("Nothing to redo")
    }

    // Text edits are grouped into short undo transactions so Ctrl+Z behaves like a desktop
    // editor without creating one history entry for every keystroke.
    fun beginTypingUndo(doc: WordDocument) {
        val now = System.currentTimeMillis()
        if (now - doc.lastEditSnapshotAt > 700L) doc.pushUndoSnapshot()
        doc.lastEditSnapshotAt = now
    }

    // ---- document selection helpers ---------------------------------------------------------
    fun paragraphIndex(id: String): Int = currentDoc.blocks.indexOfFirst { it.id == id }

    fun normalizeDocumentSelection(sel: DocumentSelection): DocumentSelection? {
        val a = paragraphIndex(sel.startBlockId)
        val b = paragraphIndex(sel.endBlockId)
        if (a < 0 || b < 0) return null
        return if (a < b || (a == b && sel.startOffset <= sel.endOffset)) sel
        else DocumentSelection(sel.endBlockId, sel.endOffset, sel.startBlockId, sel.startOffset)
    }

    // ---- formatting helpers ------------------------------------------------------------------
    fun selectedParagraphsForFormatting(): List<Pair<ParagraphBlock, TextRange>> {
        val sel = documentSelection?.let(::normalizeDocumentSelection)
        if (sel != null && !sel.collapsed) {
            val a = paragraphIndex(sel.startBlockId)
            val b = paragraphIndex(sel.endBlockId)
            if (a >= 0 && b >= 0) {
                return currentDoc.blocks.subList(a, b + 1).mapNotNull { block ->
                    val p = block as? ParagraphBlock ?: return@mapNotNull null
                    val start = if (p.id == sel.startBlockId) sel.startOffset else 0
                    val end = if (p.id == sel.endBlockId) sel.endOffset else p.field.text.length
                    if (end > start) p to TextRange(start.coerceIn(0, p.field.text.length), end.coerceIn(0, p.field.text.length)) else null
                }
            }
        }
        val p = focusedParagraph ?: return emptyList()
        val fallbackSel = p.field.selection
        return if (!fallbackSel.collapsed) listOf(p to TextRange(fallbackSel.min, fallbackSel.max)) else emptyList()
    }

    fun selectedParagraphsIncludingEmpty(): List<ParagraphBlock> {
        val sel = documentSelection?.let(::normalizeDocumentSelection)
        if (sel != null && !sel.collapsed) {
            val a = paragraphIndex(sel.startBlockId)
            val b = paragraphIndex(sel.endBlockId)
            if (a >= 0 && b >= 0) return currentDoc.blocks.subList(a, b + 1).filterIsInstance<ParagraphBlock>()
        }
        return focusedParagraph?.let(::listOf) ?: emptyList()
    }

    fun toggleAttribute(pick: (StyleAttrs) -> Boolean, set: (StyleAttrs, Boolean) -> StyleAttrs) {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) {
            focusedParagraph?.let { p -> p.typingStyle = set(p.typingStyle, !pick(p.typingStyle)); currentDoc.isDirty = true }
            return
        }
        currentDoc.pushUndoSnapshot()
        val newVal = !targets.all { (p, r) -> rangeHas(p.spans, r.min, r.max, pick) }
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { s -> set(s, newVal) }
            p.typingStyle = set(p.typingStyle, newVal)
        }
        currentDoc.isDirty = true
    }

    fun applyFontSize(size: Int) {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) { focusedParagraph?.let { it.typingStyle = it.typingStyle.copy(fontSize = size); currentDoc.isDirty = true }; return }
        currentDoc.pushUndoSnapshot()
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { s -> s.copy(fontSize = size) }
            p.typingStyle = p.typingStyle.copy(fontSize = size)
        }
        currentDoc.isDirty = true
    }

    fun applyColor(color: Color) {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) { focusedParagraph?.let { it.typingStyle = it.typingStyle.copy(color = color); currentDoc.isDirty = true }; return }
        currentDoc.pushUndoSnapshot()
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { s -> s.copy(color = color) }
            p.typingStyle = p.typingStyle.copy(color = color)
        }
        currentDoc.isDirty = true
    }

    fun applyHighlight(color: Color?) {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) { focusedParagraph?.let { it.typingStyle = it.typingStyle.copy(highlight = color); currentDoc.isDirty = true }; return }
        currentDoc.pushUndoSnapshot()
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { s -> s.copy(highlight = color) }
            p.typingStyle = p.typingStyle.copy(highlight = color)
        }
        currentDoc.isDirty = true
    }

    fun applyFont(font: FontChoice) {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) { focusedParagraph?.let { it.typingStyle = it.typingStyle.copy(font = font); currentDoc.isDirty = true }; return }
        currentDoc.pushUndoSnapshot()
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { s -> s.copy(font = font) }
            p.typingStyle = p.typingStyle.copy(font = font)
        }
        currentDoc.isDirty = true
    }

    fun clearFormatting() {
        val targets = selectedParagraphsForFormatting()
        if (targets.isEmpty()) { focusedParagraph?.let { p -> p.typingStyle = BuiltInStyles.byId(p.styleId).baseAttrs(); currentDoc.isDirty = true }; return }
        currentDoc.pushUndoSnapshot()
        targets.forEach { (p, r) ->
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            p.spans = applyStyle(p.spans, r.min, r.max, p.field.text.length, base) { base }
            p.typingStyle = base
        }
        currentDoc.isDirty = true
    }

    fun setAlign(a: TextAlign) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        targets.forEach { it.alignmentOverride = a }
        currentDoc.isDirty = true
    }

    fun applyParagraphStyle(styleId: String) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        targets.forEach { p ->
            p.styleId = styleId
            val base = BuiltInStyles.byId(styleId).baseAttrs()
            p.spans = normalizeAndMerge(p.spans, p.field.text.length, base)
            p.typingStyle = base
        }
        currentDoc.isDirty = true
    }

    fun toggleList(type: ListType) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        val turnOn = targets.any { it.listType != type }
        targets.forEach { it.listType = if (turnOn) type else null }
        currentDoc.isDirty = true
    }

    fun changeIndent(delta: Int) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        targets.forEach { p ->
            if (p.listType != null) p.listLevel = (p.listLevel + delta).coerceIn(0, 4)
            else p.leftIndentPt = (p.leftIndentPt + delta * 18f).coerceIn(0f, 288f)
        }
        currentDoc.isDirty = true
    }

    fun setParagraphSpacing(before: Int, after: Int) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        targets.forEach { p ->
            p.spacingBeforeOverride = before.coerceIn(0, 144)
            p.spacingAfterOverride = after.coerceIn(0, 144)
        }
        currentDoc.isDirty = true
    }

    fun setLineSpacing(value: Float) {
        val targets = selectedParagraphsIncludingEmpty()
        if (targets.isEmpty()) return
        currentDoc.pushUndoSnapshot()
        targets.forEach { it.lineSpacing = value.coerceIn(0.8f, 3f) }
        currentDoc.isDirty = true
    }

    fun syncVisualSelection(sel: DocumentSelection?) {
        val normalized = sel?.let(::normalizeDocumentSelection)
        documentSelection = normalized
        if (normalized == null) return
        val a = paragraphIndex(normalized.startBlockId)
        val b = paragraphIndex(normalized.endBlockId)
        currentDoc.blocks.forEachIndexed { i, block ->
            if (block is ParagraphBlock) {
                val range = when {
                    i < a || i > b -> TextRange.Zero
                    a == b && i == a -> TextRange(normalized.startOffset.coerceIn(0, block.field.text.length), normalized.endOffset.coerceIn(0, block.field.text.length))
                    i == a -> TextRange(normalized.startOffset.coerceIn(0, block.field.text.length), block.field.text.length)
                    i == b -> TextRange(0, normalized.endOffset.coerceIn(0, block.field.text.length))
                    else -> TextRange(0, block.field.text.length)
                }
                block.field = block.field.copy(selection = range)
            }
        }
    }

    fun beginDocumentSelectionFromFocus() {
        val p = focusedParagraph ?: return
        val r = p.field.selection
        documentSelection = DocumentSelection(p.id, r.start, p.id, r.end)
    }

    fun moveCaretAcrossParagraph(forward: Boolean, extend: Boolean, byWord: Boolean = false): Boolean {
        val p = focusedParagraph ?: return false
        val index = paragraphIndex(p.id)
        val sel = p.field.selection
        val current = if (extend) sel.end else if (forward) sel.max else sel.min
        val targetIndex = if (forward) index + 1 else index - 1
        if (targetIndex !in currentDoc.blocks.indices) return false
        val target = currentDoc.blocks[targetIndex] as? ParagraphBlock ?: return false
        if (byWord && extend && current != if (forward) p.field.text.length else 0) return false
        val targetOffset = if (forward) 0 else target.field.text.length
        val anchor = if (extend) sel.start else targetOffset
        if (extend) {
            val existing = documentSelection
            val baseSel = existing ?: DocumentSelection(p.id, sel.start, p.id, sel.end)
            val start = if (forward) baseSel.startBlockId else target.id
            val startOffset = if (forward) baseSel.startOffset else targetOffset
            val end = if (forward) target.id else baseSel.endBlockId
            val endOffset = if (forward) targetOffset else baseSel.endOffset
            syncVisualSelection(DocumentSelection(start, startOffset, end, endOffset))
        } else {
            target.field = target.field.copy(selection = TextRange(targetOffset))
            documentSelection = null
        }
        focusedParagraph = target
        focusedTopIndex = targetIndex
        focusTargetParagraphId = target.id
        return true
    }

    fun selectedDocumentParagraphs(): Pair<Int, Int>? {
        val sel = documentSelection?.let(::normalizeDocumentSelection) ?: return null
        val a = paragraphIndex(sel.startBlockId); val b = paragraphIndex(sel.endBlockId)
        if (a < 0 || b < 0) return null
        return a to b
    }

    fun copyDocumentSelection(): Boolean {
        val sel = documentSelection?.let(::normalizeDocumentSelection) ?: return false
        val a = paragraphIndex(sel.startBlockId); val b = paragraphIndex(sel.endBlockId)
        if (a < 0 || b < 0) return false
        val selected = currentDoc.blocks.subList(a, b + 1).filterIsInstance<ParagraphBlock>()
        if (selected.isEmpty()) return false
        val pieces = selected.mapIndexed { i, para ->
            val start = if (i == 0) sel.startOffset else 0
            val end = if (i == selected.lastIndex) sel.endOffset else para.field.text.length
            para.field.text.substring(start.coerceIn(0, para.field.text.length), end.coerceIn(0, para.field.text.length))
        }
        val text = pieces.joinToString("\n")
        if (text.isEmpty()) return false
        val paragraphs = selected.mapIndexed { i, para ->
            val start = if (i == 0) sel.startOffset else 0
            val end = if (i == selected.lastIndex) sel.endOffset else para.field.text.length
            val len = (end - start).coerceAtLeast(0)
            val spans = para.spans.filter { it.end > start && it.start < end }.map { it.copy(start = (it.start - start).coerceAtLeast(0), end = (it.end - start).coerceAtMost(len)) }.filter { it.end > it.start }
            ClipboardParagraph(para.field.text.substring(start, end), spans, para.styleId, para.alignmentOverride, para.listType, para.listLevel)
        }
        richClipboardPayload = RichClipboardPayload(text, paragraphs.firstOrNull()?.spans ?: emptyList(), BuiltInStyles.byId(selected.first().styleId).baseAttrs(), paragraphs)
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Bluebird Word", text))
        notify("Copied")
        return true
    }

    fun cutDocumentSelection(): Boolean {
        val sel = documentSelection?.let(::normalizeDocumentSelection) ?: return false
        if (currentDoc.readOnly || !copyDocumentSelection()) return false
        val a = paragraphIndex(sel.startBlockId); val b = paragraphIndex(sel.endBlockId)
        if (a < 0 || b < 0) return false
        currentDoc.pushUndoSnapshot()
        val first = currentDoc.blocks[a] as? ParagraphBlock ?: return false
        val last = currentDoc.blocks[b] as? ParagraphBlock ?: return false
        val prefix = first.field.text.substring(0, sel.startOffset.coerceIn(0, first.field.text.length))
        val suffix = last.field.text.substring(sel.endOffset.coerceIn(0, last.field.text.length))
        val merged = prefix + suffix
        val join = prefix.length
        val spans = first.spans.filter { it.end <= sel.startOffset }.map { it.copy() }.toMutableList()
        last.spans.filter { it.start >= sel.endOffset }.forEach { spans.add(it.copy(start = it.start - sel.endOffset + join, end = it.end - sel.endOffset + join)) }
        first.field = TextFieldValue(merged, TextRange(join))
        first.spans = normalizeAndMerge(spans, merged.length, BuiltInStyles.byId(first.styleId).baseAttrs())
        for (i in b downTo a + 1) currentDoc.blocks.removeAt(i)
        focusedParagraph = first; focusedTopIndex = a; focusTargetParagraphId = first.id; documentSelection = null
        currentDoc.isDirty = true; currentDoc.lastModified = System.currentTimeMillis(); notify("Cut")
        return true
    }

    fun copySelection() {
        if (documentSelection != null && !documentSelection!!.collapsed) { if (copyDocumentSelection()) return }
        val p = focusedParagraph ?: return
        val sel = p.field.selection
        if (sel.collapsed) { notify("Select text first"); return }
        val start = sel.min
        val end = sel.max
        val text = p.field.text.substring(start, end)
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val copiedSpans = p.spans
            .filter { it.end > start && it.start < end }
            .map { it.copy(start = (it.start - start).coerceAtLeast(0), end = (it.end - start).coerceAtMost(end - start)) }
            .filter { it.end > it.start }
        richClipboardPayload = RichClipboardPayload(text, copiedSpans, base)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Bluebird Word", text))
        notify("Copied")
    }

    fun cutSelection() {
        if (documentSelection != null && !documentSelection!!.collapsed) { if (cutDocumentSelection()) return }
        val p = focusedParagraph ?: return
        val sel = p.field.selection
        if (sel.collapsed) { notify("Select text first"); return }
        copySelection()
        currentDoc.pushUndoSnapshot()
        val old = p.field.text
        val newText = old.removeRange(sel.min, sel.max)
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val (field, spans) = editParagraphText(old, newText, sel.min, p.spans, p.typingStyle, base)
        p.field = field
        p.spans = spans
        currentDoc.isDirty = true
        notify("Cut")
    }

    /** Deletes the current logical document selection, including selections spanning paragraphs.
     *  This is the key bridge between the continuous document model and the paragraph text fields.
     */
    fun deleteDocumentSelection(backspace: Boolean): Boolean {
        val sel = documentSelection?.let(::normalizeDocumentSelection) ?: return false
        if (sel.collapsed || currentDoc.readOnly) return false
        val surface = DocumentTextSurface(currentDoc)
        val range = surface.paragraphRange(sel) ?: return false
        val (a, b) = range
        if (a !in currentDoc.blocks.indices || b !in currentDoc.blocks.indices) return false
        // A selection can only be represented safely as a continuous text range when all
        // blocks between its endpoints are paragraphs. Never silently delete images/tables.
        if (currentDoc.blocks.subList(a, b + 1).any { it !is ParagraphBlock }) return false

        currentDoc.pushUndoSnapshot()
        val first = currentDoc.blocks[a] as ParagraphBlock
        val last = currentDoc.blocks[b] as ParagraphBlock
        val start = sel.startOffset.coerceIn(0, first.field.text.length)
        val end = sel.endOffset.coerceIn(0, last.field.text.length)

        if (a == b) {
            val old = first.field.text
            val newText = old.removeRange(start, end)
            val base = BuiltInStyles.byId(first.styleId).baseAttrs()
            val (field, spans) = editParagraphText(old, newText, start, first.spans, first.typingStyle, base)
            first.field = field
            first.spans = spans
            first.typingStyle = typingStyleAtCursor(first, start)
        } else {
            val prefix = first.field.text.substring(0, start)
            val suffix = last.field.text.substring(end)
            val merged = prefix + suffix
            val join = prefix.length
            val kept = first.spans.filter { it.end <= start }.map { it.copy() }.toMutableList()
            last.spans.filter { it.start >= end }.forEach {
                kept.add(it.copy(start = it.start - end + join, end = it.end - end + join))
            }
            first.field = TextFieldValue(merged, TextRange(join))
            first.spans = normalizeAndMerge(kept, merged.length, BuiltInStyles.byId(first.styleId).baseAttrs())
            first.typingStyle = typingStyleAtCursor(first, join)
            for (i in b downTo a + 1) currentDoc.blocks.removeAt(i)
        }

        focusedParagraph = first
        focusedTopIndex = a
        focusTargetParagraphId = first.id
        documentSelection = null
        showSelectionToolbar = false
        currentDoc.isDirty = true
        currentDoc.lastModified = System.currentTimeMillis()
        notify(if (backspace) "Selection deleted" else "Selection deleted")
        return true
    }

    fun selectAllText() {
        val firstIndex = currentDoc.blocks.indexOfFirst { it is ParagraphBlock }
        val lastIndex = currentDoc.blocks.indexOfLast { it is ParagraphBlock }
        if (firstIndex < 0 || lastIndex < 0) return
        val first = currentDoc.blocks[firstIndex] as ParagraphBlock
        val last = currentDoc.blocks[lastIndex] as ParagraphBlock
        syncVisualSelection(DocumentSelection(first.id, 0, last.id, last.field.text.length))
        focusedParagraph = last
        focusedTopIndex = lastIndex
        showSelectionToolbar = !currentDoc.readOnly
    }

    fun insertTextAtCursor(text: String) {
        val p = focusedParagraph ?: return
        if (text.isEmpty() || currentDoc.readOnly) return
        currentDoc.pushUndoSnapshot()
        val sel = p.field.selection
        val old = p.field.text
        val newText = old.substring(0, sel.start) + text + old.substring(sel.end)
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val field = TextFieldValue(newText, TextRange((sel.start + text.length).coerceAtMost(newText.length)))
        if (text.contains('\n')) {
            val parts = splitParagraphFromEditedValue(p, field)
            if (parts.size > 1) {
                val index = currentDoc.blocks.indexOfFirst { it.id == p.id }
                if (index >= 0) {
                    currentDoc.blocks.addAll(index + 1, parts.drop(1))
                    focusTargetParagraphId = parts.last().id
                }
            }
        } else {
            val (updatedField, spans) = editParagraphText(old, newText, field.selection.end, p.spans, p.typingStyle, base)
            p.spans = spans
            p.field = updatedField
            p.typingStyle = typingStyleAtCursor(p, updatedField.selection.end)
        }
        currentDoc.isDirty = true
        currentDoc.lastModified = System.currentTimeMillis()
    }

    fun pasteClipboard() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip
        val text = clip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text.isNullOrEmpty()) { notify("Clipboard is empty"); return }
        var p = focusedParagraph
        val rich = richClipboardPayload
        if (p != null && documentSelection != null && !documentSelection!!.collapsed) {
            cutDocumentSelection()
            p = focusedParagraph
        }
        if (p != null && rich != null && rich.text == text && rich.paragraphs.size > 1) {
            if (currentDoc.readOnly) return
            currentDoc.pushUndoSnapshot()
            val start = p.field.selection.min
            val end = p.field.selection.max
            val prefix = p.field.text.substring(0, start)
            val suffix = p.field.text.substring(end)
            val pieces = rich.paragraphs
            val created = pieces.mapIndexed { i, cp ->
                ParagraphBlock().apply {
                    styleId = cp.styleId
                    alignmentOverride = cp.alignment
                    listType = cp.listType
                    listLevel = cp.listLevel
                    field = TextFieldValue(if (i == 0) prefix + cp.text else if (i == pieces.lastIndex) cp.text + suffix else cp.text)
                    val shift = if (i == 0) prefix.length else 0
                    spans = normalizeAndMerge(cp.spans.map { it.copy(start = it.start + shift, end = it.end + shift) }, field.text.length, BuiltInStyles.byId(styleId).baseAttrs())
                    typingStyle = typingStyleAtCursor(this, field.text.length)
                }
            }
            val idx = currentDoc.blocks.indexOfFirst { it.id == p.id }
            if (idx >= 0) { currentDoc.blocks.removeAt(idx); currentDoc.blocks.addAll(idx, created); focusedParagraph = created.last(); focusedTopIndex = idx + created.lastIndex; focusTargetParagraphId = created.last().id }
            currentDoc.isDirty = true; currentDoc.lastModified = System.currentTimeMillis(); documentSelection = null; notify("Pasted formatting")
        } else if (p != null && rich != null && rich.text == text && !text.contains('\n')) {
            val sel = p.field.selection
            if (currentDoc.readOnly) return
            currentDoc.pushUndoSnapshot()
            val old = p.field.text
            val start = sel.min
            val end = sel.max
            val newText = old.substring(0, start) + text + old.substring(end)
            val base = BuiltInStyles.byId(p.styleId).baseAttrs()
            val delta = text.length
            val shifted = p.spans.filter { it.end <= start || it.start >= end }
                .map { if (it.start >= end) it.copy(start = it.start + delta - (end - start), end = it.end + delta - (end - start)) else it }
                .toMutableList()
            val inserted = rich.spans.map { it.copy(start = it.start + start, end = it.end + start) }
            shifted.addAll(inserted)
            p.spans = normalizeAndMerge(shifted, newText.length, base)
            p.field = TextFieldValue(newText, TextRange(start + text.length))
            p.typingStyle = typingStyleAtCursor(p, p.field.selection.end)
            currentDoc.isDirty = true
            currentDoc.lastModified = System.currentTimeMillis()
            notify("Pasted formatting")
        } else {
            insertTextAtCursor(text)
            notify("Pasted")
        }
    }

    fun toggleHeader() {
        if (currentDoc.readOnly) return
        currentDoc.pushUndoSnapshot()
        currentDoc.showHeader = !currentDoc.showHeader
        currentDoc.isDirty = true
    }

    fun toggleFooter() {
        if (currentDoc.readOnly) return
        currentDoc.pushUndoSnapshot()
        currentDoc.showFooter = !currentDoc.showFooter
        currentDoc.isDirty = true
    }

    fun insertPageNumberFieldIntoFooter() {
        if (currentDoc.readOnly) return
        currentDoc.pushUndoSnapshot()
        currentDoc.showFooter = true
        val existing = currentDoc.footerParagraph.field.text
        if (!existing.contains("{page}")) {
            val separator = if (existing.isBlank()) "" else " "
            currentDoc.footerParagraph.field = TextFieldValue(existing + separator + "{page}")
        }
        currentDoc.isDirty = true
        notify("Page number added to footer")
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

    fun addComment(text: String) {
        val p = focusedParagraph ?: return
        val sel = p.field.selection
        val quoted = if (!sel.collapsed) p.field.text.substring(sel.min, sel.max) else ""
        currentDoc.comments.add(DocumentComment(text = text, blockId = p.id, quotedText = quoted.take(240)))
        currentDoc.isDirty = true
        notify("Comment added")
    }

    fun insertNote(text: String, endnote: Boolean) {
        val p = focusedParagraph ?: return
        val marker = currentDoc.notes.count { it.isEndnote == endnote } + 1
        val label = if (endnote) "[$marker]" else "[$marker]"
        currentDoc.notes.add(DocumentNote(text = text, isEndnote = endnote, blockId = p.id, marker = marker))
        insertTextAtCursor(label)
        notify(if (endnote) "Endnote inserted" else "Footnote inserted")
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
    val selectionTargets = selectedParagraphsForFormatting()
    val toolbarStyle: StyleAttrs = if (selectionTargets.isNotEmpty()) {
        val first = selectionTargets.first().first
        val base = BuiltInStyles.byId(first.styleId).baseAttrs()
        val attrs = selectionTargets.flatMap { (p, r) ->
            p.spans.filter { it.end > r.min && it.start < r.max }.map { it.style }
        }
        val probe = attrs.firstOrNull() ?: base
        StyleAttrs(
            bold = attrs.isNotEmpty() && attrs.all { it.bold },
            italic = attrs.isNotEmpty() && attrs.all { it.italic },
            underline = attrs.isNotEmpty() && attrs.all { it.underline },
            strikethrough = attrs.isNotEmpty() && attrs.all { it.strikethrough },
            superscript = attrs.isNotEmpty() && attrs.all { it.superscript },
            subscript = attrs.isNotEmpty() && attrs.all { it.subscript },
            fontSize = if (attrs.all { it.fontSize == probe.fontSize }) probe.fontSize else probe.fontSize,
            color = if (attrs.all { it.color == probe.color }) probe.color else probe.color,
            font = if (attrs.all { it.font == probe.font }) probe.font else probe.font,
            highlight = if (attrs.all { it.highlight == probe.highlight }) probe.highlight else probe.highlight
        )
    } else if (fp == null) StyleAttrs() else {
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
    val selectedForToolbar = selectedParagraphsIncludingEmpty()
    val alignmentValues = selectedForToolbar.map { it.alignmentOverride ?: BuiltInStyles.byId(it.styleId).alignment }.distinct()
    val toolbarAlign: TextAlign? = if (documentSelection?.let { !it.collapsed } == true) {
        alignmentValues.singleOrNull()
    } else if (selectionTargets.isNotEmpty()) {
        alignmentValues.singleOrNull() ?: fp?.let { it.alignmentOverride ?: BuiltInStyles.byId(it.styleId).alignment }
    } else fp?.let { it.alignmentOverride ?: BuiltInStyles.byId(it.styleId).alignment } ?: TextAlign.Start
    val styleValues = selectedForToolbar.map { it.styleId }.distinct()
    val toolbarStyleId = if (styleValues.size == 1) styleValues.firstOrNull() ?: "normal" else "__mixed__"
    val toolbarListType = if (selectedParagraphsIncludingEmpty().map { it.listType }.distinct().size == 1) selectedParagraphsIncludingEmpty().firstOrNull()?.listType else null
    val homeEnabled = currentDoc.kind == DocKind.RICH_TEXT && !currentDoc.readOnly && fp != null

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && currentDoc.kind == DocKind.RICH_TEXT) {
                when (event.key) {
                    Key.N -> { newDocument(); true }
                    Key.O -> { openDocLauncher.launch(arrayOf("text/*", "application/pdf", "*/*")); true }
                    Key.C -> { copySelection(); true }
                    Key.X -> { if (!currentDoc.readOnly) cutSelection(); true }
                    Key.V -> { if (!currentDoc.readOnly) pasteClipboard(); true }
                    Key.B -> { toggleAttribute({ s -> s.bold }, { s, v -> s.copy(bold = v) }); true }
                    Key.I -> { toggleAttribute({ s -> s.italic }, { s, v -> s.copy(italic = v) }); true }
                    Key.U -> { toggleAttribute({ s -> s.underline }, { s, v -> s.copy(underline = v) }); true }
                    Key.L -> { setAlign(TextAlign.Start); true }
                    Key.E -> { setAlign(TextAlign.Center); true }
                    Key.R -> { setAlign(TextAlign.End); true }
                    Key.J -> { setAlign(TextAlign.Justify); true }
                    Key.K -> { showLinkDialog = true; true }
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
                // ==================== WORD-STYLE TITLE / QUICK ACCESS BAR ====================
                Row(
                    modifier = Modifier.fillMaxWidth().height(30.dp).background(ribbonBg).padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FluentIcon("panel_left", "Toggle navigation", tint = Color.White, modifier = Modifier.size(17.dp).clickable { sidebarOpen = !sidebarOpen })
                    Spacer(Modifier.width(6.dp))
                    FluentIcon("arrow_undo", "Undo", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(17.dp).clickable { undoAction() })
                    Spacer(Modifier.width(5.dp))
                    FluentIcon("arrow_redo", "Redo", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(17.dp).clickable { redoAction() })
                    Spacer(Modifier.width(8.dp))
                    FluentIcon("document_text", "Document", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = currentDoc.title,
                        onValueChange = { t -> currentDoc.title = t; currentDoc.isDirty = true },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    if (currentDoc.isDirty) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF41A5EE)))
                        Spacer(Modifier.width(8.dp))
                    }
                    if (currentDoc.readOnly) {
                        FluentIcon("lock_closed", "Read only", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    FluentIcon("search", "Find", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp).clickable { showFindReplaceDialog = true })
                    Spacer(Modifier.width(10.dp))
                    FluentIcon("save", "Save", tint = Color.White, modifier = Modifier.size(17.dp).clickable {
                        if (currentDoc.kind == DocKind.RICH_TEXT && !currentDoc.readOnly) saveCurrent()
                    })
                }

                // ==================== RIBBON TABS ====================
                Row(
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                        .background(if (isDark) WordTheme.darkSurface else WordTheme.ribbonWhite),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        RibbonTab.FILE to "File", RibbonTab.HOME to "Home", RibbonTab.INSERT to "Insert",
                        RibbonTab.LAYOUT to "Layout", RibbonTab.REFERENCES to "References", RibbonTab.REVIEW to "Review",
                        RibbonTab.VIEW to "View"
                    ).forEach { (tab, label) ->
                        val selected = ribbonTab == tab
                        Box(
                            modifier = Modifier.fillMaxHeight()
                                .clickable { ribbonTab = tab }
                                .background(if (tab == RibbonTab.FILE && selected) WordTheme.wordBlue else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (tab == RibbonTab.FILE && selected) Color.White else textColor,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            if (selected && tab != RibbonTab.FILE) {
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.62f).height(2.dp).background(WordTheme.wordBlue))
                            }
                        }
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
                        onPrint = { printCurrentDocument() },
                        onShare = { shareCurrentDocument() },
                        onProperties = { showPropertiesDialog = true },
                        onPageSetup = { showPageSetupDialog = true },
                        onSettings = { showSettingsDialog = true },
                        onClose = { ribbonTab = RibbonTab.HOME }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth().background(ribbonStripBg)) {
                        when {
                            contextualRibbon == ContextualRibbon.PICTURE -> PictureContextRibbon(
                                onDelete = {
                                    if (!currentDoc.readOnly && focusedTopIndex in currentDoc.blocks.indices && currentDoc.blocks[focusedTopIndex] is ImageBlock) {
                                        currentDoc.pushUndoSnapshot(); currentDoc.blocks.removeAt(focusedTopIndex); contextualRibbon = null; currentDoc.isDirty = true
                                    }
                                },
                                onScale = { scale ->
                                    val image = currentDoc.blocks.getOrNull(focusedTopIndex) as? ImageBlock
                                    if (image != null && !currentDoc.readOnly) { currentDoc.pushUndoSnapshot(); image.widthDp = (image.widthDp * scale).toInt().coerceIn(80, 900); currentDoc.isDirty = true }
                                },
                                onSetScale = { percent ->
                                    val image = currentDoc.blocks.getOrNull(focusedTopIndex) as? ImageBlock
                                    if (image != null && !currentDoc.readOnly) { currentDoc.pushUndoSnapshot(); image.widthDp = (300f * percent / 100f).toInt().coerceIn(80, 900); currentDoc.isDirty = true }
                                },
                                onAlign = { align ->
                                    val image = currentDoc.blocks.getOrNull(focusedTopIndex) as? ImageBlock
                                    if (image != null && !currentDoc.readOnly) { image.alignment = align; currentDoc.isDirty = true }
                                },
                                onClose = { contextualRibbon = null }
                            )
                            contextualRibbon == ContextualRibbon.TABLE -> TableContextRibbon(
                                onAddRow = {
                                    val table = currentDoc.blocks.getOrNull(focusedTopIndex) as? TableBlock
                                    if (table != null && !currentDoc.readOnly) {
                                        currentDoc.pushUndoSnapshot()
                                        val cols = table.rows.firstOrNull()?.cells?.size ?: 1
                                        table.rows.add(TableRow().apply { repeat(cols) { cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } })
                                        currentDoc.isDirty = true
                                    }
                                },
                                onAddColumn = {
                                    val table = currentDoc.blocks.getOrNull(focusedTopIndex) as? TableBlock
                                    if (table != null && !currentDoc.readOnly) {
                                        currentDoc.pushUndoSnapshot()
                                        table.rows.forEach { it.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) }
                                        currentDoc.isDirty = true
                                    }
                                },
                                onDelete = {
                                    if (!currentDoc.readOnly && focusedTopIndex in currentDoc.blocks.indices && currentDoc.blocks[focusedTopIndex] is TableBlock) {
                                        currentDoc.pushUndoSnapshot(); currentDoc.blocks.removeAt(focusedTopIndex); contextualRibbon = null; currentDoc.isDirty = true
                                    }
                                },
                                onClose = { contextualRibbon = null }
                            )
                            ribbonTab == RibbonTab.HOME -> HomeRibbon(
                                enabled = homeEnabled,
                                typingOrSelectionStyle = toolbarStyle, alignment = toolbarAlign,
                                mixedFormatting = selectionTargets.size > 1 || selectionTargets.any { (p, r) ->
                                    p.spans.filter { it.end > r.min && it.start < r.max }.map { it.style }.distinct().size > 1
                                },
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
                                onClearFormatting = { clearFormatting() },
                                onCopy = { copySelection() },
                                onCut = { cutSelection() },
                                onPaste = { pasteClipboard() },
                                onSpacingChange = { before, after -> setParagraphSpacing(before, after) },
                                onLineSpacingChange = { setLineSpacing(it) }
                            )
                            ribbonTab == RibbonTab.INSERT -> InsertRibbon(
                                onInsertDate = { insertTextAtCursor(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())) },
                                onInsertDivider = { insertTextAtCursor("\n" + "─".repeat(32) + "\n") },
                                onInsertTable = { showTableDialog = true },
                                onInsertImage = { imagePickerLauncher.launch("image/*") },
                                onInsertPageBreak = { insertBlockAfterFocus(PageBreakBlock()) },
                                onInsertLink = { showLinkDialog = true },
                                onInsertBookmark = { showAddBookmarkDialog = true },
                                onInsertPageNumberField = { insertPageNumberFieldIntoFooter() },
                                onToggleHeader = { toggleHeader() },
                                onToggleFooter = { toggleFooter() },
                                onInsertSymbol = { showSymbolDialog = true }
                            )
                            ribbonTab == RibbonTab.LAYOUT -> LayoutRibbon(
                                onPageSetup = { showPageSetupDialog = true },
                                onMargins = { showPageSetupDialog = true }
                            )
                            ribbonTab == RibbonTab.REFERENCES -> ReferencesRibbon(
                                navPanelOpen = navPanelOpen,
                                bookmarks = currentDoc.bookmarks,
                                onInsertToc = { insertToc() },
                                onJumpToBookmark = { jumpToBlockId(it) },
                                onDeleteBookmark = { id -> currentDoc.bookmarks.removeAll { it.id == id } },
                                onInsertFootnote = { pendingNoteIsEndnote = false; showNoteDialog = true },
                                onInsertEndnote = { pendingNoteIsEndnote = true; showNoteDialog = true },
                                onToggleNavPanel = { navPanelOpen = !navPanelOpen }
                            )
                            ribbonTab == RibbonTab.REVIEW -> ReviewRibbon(
                                enabled = currentDoc.kind == DocKind.RICH_TEXT,
                                onFind = { showFindReplaceDialog = true },
                                onAddComment = { if (!currentDoc.readOnly) showCommentDialog = true },
                                onShowComments = { showCommentsDialog = true },
                                onToggleReadOnly = { currentDoc.readOnly = !currentDoc.readOnly }
                            )
                            ribbonTab == RibbonTab.VIEW -> ViewRibbon(
                                sidebarOpen = sidebarOpen, thumbnailsOpen = showPageThumbnails, zoom = zoom,
                                readOnly = currentDoc.readOnly, readingMode = readingMode, viewMode = viewMode,
                                showRuler = showRuler,
                                onToggleSidebar = { sidebarOpen = !sidebarOpen },
                                onToggleThumbnails = { showPageThumbnails = !showPageThumbnails },
                                onZoomChange = { zoom = it },
                                onViewModeChange = { viewMode = it },
                                onToggleReadOnly = { currentDoc.readOnly = !currentDoc.readOnly },
                                onToggleReadingMode = { readingMode = true },
                                onToggleRuler = { showRuler = !showRuler },
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
                    AnimatedVisibility(visible = showPageThumbnails) {
                        PageThumbnailPanel(doc = currentDoc, textColor = textColor, onJump = { jumpToBlockId(it) })
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (currentDoc.kind == DocKind.PDF) {
                        PdfViewerPane(pages = pdfPages, pdfUri = currentDoc.pdfUri, zoom = zoom, isDark = isDark)
                    } else {
                        PagedDocumentView(
                            doc = currentDoc, zoom = zoom, pageColor = pageColor, textColor = textColor,
                            readOnly = currentDoc.readOnly, showRuler = showRuler, focusedParagraph = focusedParagraph,
                            focusTargetParagraphId = focusTargetParagraphId,
                            onFocusTargetChange = { focusTargetParagraphId = it },
                            onFocusTargetConsumed = { focusTargetParagraphId = null },
                            matches = matches, activeMatch = activeMatch,
                            jumpRequesters = jumpRequesters, jumpTargetIds = jumpTargetIds,
                            onParagraphFocus = { p -> focusedParagraph = p; selectedBlockId = p.id; contextualRibbon = null; showSelectionToolbar = false },
                            onTopIndexFocus = { i -> focusedTopIndex = i },
                            onImageSelect = { selectedBlockId = currentDoc.blocks.getOrNull(focusedTopIndex)?.id; contextualRibbon = ContextualRibbon.PICTURE },
                            onTableSelect = { selectedBlockId = currentDoc.blocks.getOrNull(focusedTopIndex)?.id; contextualRibbon = ContextualRibbon.TABLE },
                            selectedBlockId = selectedBlockId,
                            onTextEdit = { beginTypingUndo(currentDoc) },
                            onCopy = { copySelection() },
                            onCut = { cutSelection() },
                            onPaste = { pasteClipboard() },
                            onSelectAll = { selectAllText() },
                            onSelectionChange = { block, range ->
                                if (focusedParagraph?.id == block.id) {
                                    if (range.collapsed) {
                                        documentSelection = null
                                        showSelectionToolbar = false
                                    } else {
                                        syncVisualSelection(DocumentSelection(block.id, range.start, block.id, range.end))
                                        showSelectionToolbar = true
                                    }
                                }
                            },
                            onMoveAcrossParagraph = { block, index, forward, extend, byWord ->
                                if (focusedParagraph?.id == block.id) moveCaretAcrossParagraph(forward, extend, byWord) else false
                            },
                            onDocumentSelectionDelete = { isDelete -> deleteDocumentSelection(!isDelete) },
                            onLink = { showLinkDialog = true },
                            onComment = { showCommentDialog = true },
                            onRegenerateToc = { regenerateToc(it) },
                            onJumpToBlock = { jumpToBlockId(it) }
                        )
                    }
                    val selectedText = (documentSelection?.let { !it.collapsed } == true) ||
                        (focusedParagraph?.let { !it.field.selection.collapsed && it.field.selection.min != it.field.selection.max } == true)
                    if (selectedText && showSelectionToolbar && !currentDoc.readOnly) {
                        TextSelectionMiniToolbar(
                            style = toolbarStyle,
                            enabled = homeEnabled,
                            onBold = { toggleAttribute({ a -> a.bold }, { a, v -> a.copy(bold = v) }) },
                            onItalic = { toggleAttribute({ a -> a.italic }, { a, v -> a.copy(italic = v) }) },
                            onUnderline = { toggleAttribute({ a -> a.underline }, { a, v -> a.copy(underline = v) }) },
                            onFontSizeChange = { applyFontSize(it) },
                            onHighlight = { applyHighlight(Color(0xFFFFF59D)) },
                            onColor = { applyColor(Color(0xFF1A1A1A)) },
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                        )
                    }
                    if (readingMode) {
                        IconButton(
                            onClick = { readingMode = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                        ) { FluentIcon("full_screen_minimize", "Exit reading mode", tint = Color.White) }
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

    if (showSymbolDialog) {
        SymbolDialog(
            onDismiss = { showSymbolDialog = false },
            onInsert = { symbol -> insertTextAtCursor(symbol); showSymbolDialog = false }
        )
    }

    if (showLinkDialog) {
        LinkDialog(onDismiss = { showLinkDialog = false }, onConfirm = { url -> insertLinkOnSelection(url); showLinkDialog = false })
    }

    if (showAddBookmarkDialog) {
        AddBookmarkDialog(onDismiss = { showAddBookmarkDialog = false }, onConfirm = { name -> addBookmarkAtFocus(name); showAddBookmarkDialog = false })
    }

    if (showCommentDialog) {
        val quoted = focusedParagraph?.let { p ->
            val r = p.field.selection
            if (!r.collapsed) p.field.text.substring(r.min, r.max) else ""
        } ?: ""
        CommentDialog(
            quotedText = quoted,
            onDismiss = { showCommentDialog = false },
            onConfirm = { addComment(it); showCommentDialog = false }
        )
    }

    if (showCommentsDialog) {
        CommentsDialog(
            comments = currentDoc.comments,
            onResolve = { id -> currentDoc.comments.firstOrNull { it.id == id }?.let { it.resolved = true; currentDoc.isDirty = true } },
            onDelete = { id -> currentDoc.comments.removeAll { it.id == id }; currentDoc.isDirty = true },
            onDismiss = { showCommentsDialog = false }
        )
    }

    if (showNoteDialog) {
        NoteDialog(
            title = if (pendingNoteIsEndnote) "Insert Endnote" else "Insert Footnote",
            onDismiss = { showNoteDialog = false },
            onConfirm = { insertNote(it, pendingNoteIsEndnote); showNoteDialog = false }
        )
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

private data class PdfPageInfo(val index: Int, val width: Int, val height: Int)

@Composable
private fun PdfViewerPane(pages: List<PdfPageInfo>, pdfUri: Uri?, zoom: Float, isDark: Boolean) {
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
        if (pdfUri != null) {
            itemsIndexed(pages, key = { _, page -> page.index }) { _, page ->
                PdfPagePreview(page, pages.size, pdfUri, zoom)
            }
        }
    }
}

@Composable
private fun PdfPagePreview(page: PdfPageInfo, total: Int, pdfUri: Uri, zoom: Float) {
    val context = LocalContext.current
    var bitmap by remember(page.index, pdfUri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(page.index, pdfUri) { mutableStateOf(false) }

    LaunchedEffect(page.index, pdfUri) {
        val rendered = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(page.index).use { pdfPage ->
                            val targetWidth = 900
                            val targetHeight = (targetWidth * pdfPage.height.toFloat() / pdfPage.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { bmp ->
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }.getOrNull()
        }
        if (rendered != null) bitmap = rendered else failed = true
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(), contentDescription = "Page ${page.index + 1}",
                modifier = Modifier.width((360 * zoom).dp).shadow(4.dp).background(Color.White)
            )
            failed -> Box(
                Modifier.width((360 * zoom).dp).aspectRatio(page.width.toFloat() / page.height.coerceAtLeast(1)).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Text("Unable to render page", fontSize = 12.sp) }
            else -> Box(
                Modifier.width((360 * zoom).dp).aspectRatio(page.width.toFloat() / page.height.coerceAtLeast(1)).background(Color.White),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(modifier = Modifier.size(22.dp)) }
        }
        Text("Page ${page.index + 1} of $total", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
    }
}
@Composable
private fun StatusBar(doc: WordDocument, zoom: Float, textColor: Color, ribbonStripBg: Color, onZoomChange: (Float) -> Unit) {
    val paragraphs = doc.blocks.filterIsInstance<ParagraphBlock>()
    val wordCount = paragraphs.sumOf { p -> p.field.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val charCount = paragraphs.sumOf { it.field.text.length }
    val pageCount = if (doc.kind == DocKind.PDF) 0 else paginate(doc).size.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).background(WordTheme.wordBlue).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when {
                doc.kind == DocKind.PDF -> "PDF document · Read-only"
                doc.readOnly -> "Page 1 of $pageCount · Read-only · $wordCount words"
                else -> "Page 1 of $pageCount · $wordCount words · $charCount characters"
            },
            color = Color.White.copy(alpha = 0.95f), fontSize = 9.sp, modifier = Modifier.weight(1f)
        )
        FluentIcon("zoom_out", null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp).clickable { onZoomChange((zoom - 0.1f).coerceAtLeast(0.6f)) })
        Slider(value = zoom, onValueChange = onZoomChange, valueRange = 0.6f..2f, modifier = Modifier.width(90.dp).padding(horizontal = 4.dp))
        FluentIcon("zoom_in", null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp).clickable { onZoomChange((zoom + 0.1f).coerceAtMost(2f)) })
        Spacer(Modifier.width(6.dp))
        Text("${(zoom * 100).toInt()}%", color = Color.White.copy(alpha = 0.95f), fontSize = 11.sp)
    }
}

@Composable
private fun FileBackstage(
    isDark: Boolean, textColor: Color, bg: Color, documents: List<WordDocument>, currentIndex: Int,
    onSelectDoc: (Int) -> Unit, onNew: () -> Unit, onOpen: () -> Unit, onOpenPdf: () -> Unit,
    onSave: () -> Unit, onSaveAs: () -> Unit, onSaveCopy: () -> Unit, onExportPdf: () -> Unit,
    onPrint: () -> Unit, onShare: () -> Unit, onProperties: () -> Unit, onPageSetup: () -> Unit,
    onSettings: () -> Unit, onClose: () -> Unit
) {
    var selectedSection by remember { mutableStateOf("Home") }
    val leftBg = if (isDark) WordTheme.wordBlueDark else WordTheme.wordBlue
    val surface = if (isDark) WordTheme.darkSurface else Color.White
    val subtle = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F2F1)
    val secondary = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF666666)

    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight().background(surface)) {
        Column(
            modifier = Modifier.width(142.dp).fillMaxHeight().background(leftBg).padding(vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            BackstageAction("document_add", "New", onNew, compact = true)
            BackstageAction("folder_open", "Open", onOpen, compact = true)
            BackstageAction("save", "Save", onSave, compact = true, enabled = true)
            BackstageAction("save_edit", "Save As", onSaveAs, compact = true)
            BackstageAction("document_pdf", "Export", onExportPdf, compact = true)
            BackstageAction("print", "Print", onPrint, compact = true)
            BackstageAction("share", "Share", onShare, compact = true)
            Spacer(Modifier.height(4.dp))
            BackstageSectionButton("Home", selectedSection == "Home") { selectedSection = "Home" }
            BackstageSectionButton("Info", selectedSection == "Info") { selectedSection = "Info" }
            BackstageSectionButton("Options", selectedSection == "Options") { selectedSection = "Options" }
            Spacer(Modifier.weight(1f))
            BackstageAction("settings", "Settings", onSettings, compact = true)
            BackstageAction("dismiss", "Close", onClose, compact = true)
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp)) {
            when (selectedSection) {
                "Home" -> {
                    Text("Document", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Create, open and manage your documents.", color = secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        BackstageTile("document_add", "New", "Blank document", onNew)
                        BackstageTile("folder_open", "Open", "Browse files", onOpen)
                        BackstageTile("document_pdf", "Open PDF", "Read a PDF", onOpenPdf)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Recent", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    documents.forEachIndexed { i, doc ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                .background(if (i == currentIndex) WordRibbonAccent.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable { onSelectDoc(i) }.padding(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FluentIcon(if (doc.kind == DocKind.PDF) "document_pdf" else "document_text", null,
                                tint = WordRibbonAccent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, color = textColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(doc.lastModified)), color = secondary, fontSize = 9.sp)
                            }
                            if (doc.isDirty) FluentIcon("circle", null, tint = WordRibbonAccent, modifier = Modifier.size(6.dp))
                        }
                    }
                }
                "Info" -> {
                    Text("Info", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    BackstageInfoRow("Name", currentDocumentTitle(documents, currentIndex))
                    BackstageInfoRow("Format", if (documents.getOrNull(currentIndex)?.kind == DocKind.PDF) "PDF" else "Bluebird Word Document")
                    BackstageInfoRow("Status", if (documents.getOrNull(currentIndex)?.isDirty == true) "Unsaved changes" else "Saved")
                    BackstageInfoRow("Pages", paginate(documents.getOrNull(currentIndex) ?: WordDocument("Document")).size.coerceAtLeast(1).toString())
                    Spacer(Modifier.height(8.dp))
                    BackstageAction("info", "Document Properties", onProperties, darkText = textColor, surface = subtle)
                    BackstageAction("document_text", "Page Setup", onPageSetup, darkText = textColor, surface = subtle)
                }
                "Options" -> {
                    Text("Word Impress Options", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Application and document preferences.", color = secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                    BackstageAction("settings", "Settings", onSettings, darkText = textColor, surface = subtle)
                    BackstageAction("document_text", "Page Setup", onPageSetup, darkText = textColor, surface = subtle)
                }
            }
        }
    }
}

@Composable
private fun BackstageSectionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 11.dp, vertical = 6.dp))
}

@Composable
private fun BackstageTile(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier.width(112.dp).height(78.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, WordRibbonBorder)
        .clickable { onClick() }.padding(8.dp), verticalArrangement = Arrangement.Center) {
        FluentIcon(icon, null, tint = WordRibbonAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 10.sp, color = Color(0xFF202020), fontWeight = FontWeight.SemiBold)
        Text(subtitle, fontSize = 8.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BackstageInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, fontSize = 10.sp, color = Color(0xFF666666), modifier = Modifier.width(75.dp))
        Text(value, fontSize = 10.sp, color = Color(0xFF202020), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun currentDocumentTitle(documents: List<WordDocument>, index: Int): String =
    documents.getOrNull(index)?.title ?: "Document"

@Composable
private fun BackstageAction(
    icon: String, label: String, onClick: () -> Unit,
    compact: Boolean = false, enabled: Boolean = true, darkText: Color? = null, surface: Color = Color.Transparent
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
            .background(surface).clickable(enabled = enabled) { onClick() }
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FluentIcon(icon, null, tint = darkText ?: Color.White.copy(alpha = if (enabled) 1f else 0.45f), modifier = Modifier.size(if (compact) 15.dp else 16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = darkText ?: Color.White.copy(alpha = if (enabled) 1f else 0.45f), fontSize = if (compact) 10.sp else 11.sp)
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
                FluentIcon("add", null, tint = bluebirdColors.AccentBlue, modifier = Modifier.size(14.dp))
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
                    FluentIcon(
                        if (doc.kind == DocKind.PDF) "document_pdf" else "document_text",
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
                            FluentIcon("more_vertical", null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
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
