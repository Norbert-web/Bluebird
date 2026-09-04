package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Add
import fluent.ui.system.icons.regular.ArrowReply
import fluent.ui.system.icons.regular.ArrowRotateClockwise
import fluent.ui.system.icons.regular.CalendarLtr
import fluent.ui.system.icons.regular.Checkmark
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.ChevronDown
import fluent.ui.system.icons.regular.ClipboardPaste
import fluent.ui.system.icons.regular.Copy
import fluent.ui.system.icons.regular.Cut
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Dismiss
import fluent.ui.system.icons.regular.Document
import fluent.ui.system.icons.regular.DocumentPdf
import fluent.ui.system.icons.regular.DocumentText
import fluent.ui.system.icons.regular.Edit
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.FullScreenMaximize
import fluent.ui.system.icons.regular.FullScreenMinimize
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.Link
import fluent.ui.system.icons.regular.List
import fluent.ui.system.icons.regular.LockClosed
import fluent.ui.system.icons.regular.MoreHorizontal
import fluent.ui.system.icons.regular.Pin
import fluent.ui.system.icons.regular.Print
import fluent.ui.system.icons.regular.Save
import fluent.ui.system.icons.regular.Search
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Share
import fluent.ui.system.icons.regular.Subtract
import fluent.ui.system.icons.regular.Table
import fluent.ui.system.icons.regular.TextFont
import fluent.ui.system.icons.regular.WindowConsole

/**
 * Word Processor icon gateway.
 *
 * Follows the same pattern as the main shell's FluentIcon.kt: a flat object
 * of vals exposing FluentIcons.Regular.X directly, plus a name->icon lookup
 * for WordImpress's toolbar/menu code. No Painter conversion — Icon() only
 * ever needs the ImageVector itself.
 */
private object WordImpressIcon {
    val Add             = FluentIcons.Regular.Add
    val ArrowRedo        = FluentIcons.Regular.ArrowReply
    val ArrowUndo        = FluentIcons.Regular.ArrowReply
    val ArrowRotateClockwise = FluentIcons.Regular.ArrowRotateClockwise
    val Calendar         = FluentIcons.Regular.CalendarLtr
    val Checkmark        = FluentIcons.Regular.Checkmark
    val CheckmarkCircle  = FluentIcons.Regular.CheckmarkCircle
    val ChevronDown      = FluentIcons.Regular.ChevronDown
    val ClipboardPaste   = FluentIcons.Regular.ClipboardPaste
    val Copy             = FluentIcons.Regular.Copy
    val Cut              = FluentIcons.Regular.Cut
    val Delete           = FluentIcons.Regular.Delete
    val Dismiss          = FluentIcons.Regular.Dismiss
    val Document         = FluentIcons.Regular.Document
    val DocumentPdf      = FluentIcons.Regular.DocumentPdf
    val DocumentText     = FluentIcons.Regular.DocumentText
    val Edit             = FluentIcons.Regular.Edit
    val FolderOpen       = FluentIcons.Regular.FolderOpen
    val FullScreenMaximize = FluentIcons.Regular.FullScreenMaximize
    val FullScreenMinimize = FluentIcons.Regular.FullScreenMinimize
    val Grid             = FluentIcons.Regular.Grid
    val Image            = FluentIcons.Regular.Image
    val Link             = FluentIcons.Regular.Link
    val List             = FluentIcons.Regular.List
    val LockClosed       = FluentIcons.Regular.LockClosed
    val MoreHorizontal   = FluentIcons.Regular.MoreHorizontal
    val Pin              = FluentIcons.Regular.Pin
    val Print            = FluentIcons.Regular.Print
    val Save             = FluentIcons.Regular.Save
    val Search           = FluentIcons.Regular.Search
    val Settings         = FluentIcons.Regular.Settings
    val Share            = FluentIcons.Regular.Share
    val Subtract         = FluentIcons.Regular.Subtract
    val Table            = FluentIcons.Regular.Table
    val TextFont         = FluentIcons.Regular.TextFont
    val WindowConsole    = FluentIcons.Regular.WindowConsole
}

@Composable
fun FluentIcon(
    name: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Int = 24
) {
    @Suppress("UNUSED_PARAMETER")
    val ignoredFilled = filled
    @Suppress("UNUSED_PARAMETER")
    val ignoredSize = size
    Icon(
        imageVector = fluentImageVector(name),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

private fun fluentImageVector(name: String): androidx.compose.ui.graphics.vector.ImageVector = when (name) {
    "add" -> WordImpressIcon.Add
    "arrow_clockwise", "arrow_rotate_clockwise" -> WordImpressIcon.ArrowRotateClockwise
    "arrow_redo" -> WordImpressIcon.ArrowRedo
    "arrow_undo" -> WordImpressIcon.ArrowUndo
    "checkmark" -> WordImpressIcon.Checkmark
    "chevron_down" -> WordImpressIcon.ChevronDown
    "clipboard_paste" -> WordImpressIcon.ClipboardPaste
    "circle" -> WordImpressIcon.CheckmarkCircle
    "copy", "content_copy" -> WordImpressIcon.Copy
    "cut", "content_cut" -> WordImpressIcon.Cut
    "delete" -> WordImpressIcon.Delete
    "dismiss" -> WordImpressIcon.Dismiss
    "subtract" -> WordImpressIcon.Subtract
    "save" -> WordImpressIcon.Save
    "search" -> WordImpressIcon.Search
    "settings" -> WordImpressIcon.Settings
    "lock_closed" -> WordImpressIcon.LockClosed
    "more_vertical" -> WordImpressIcon.MoreHorizontal
    "panel_left" -> WordImpressIcon.Grid
    "panel_bottom" -> WordImpressIcon.WindowConsole
    "full_screen_maximize" -> WordImpressIcon.FullScreenMaximize
    "full_screen_minimize" -> WordImpressIcon.FullScreenMinimize
    "zoom_in" -> WordImpressIcon.FullScreenMaximize
    "zoom_out" -> WordImpressIcon.FullScreenMinimize
    "document", "document_add" -> WordImpressIcon.Document
    "document_text", "document_page_number" -> WordImpressIcon.DocumentText
    "document_pdf" -> WordImpressIcon.DocumentPdf
    "image" -> WordImpressIcon.Image
    "link" -> WordImpressIcon.Link
    "calendar" -> WordImpressIcon.Calendar
    "table", "table_insert_row", "table_delete_row", "table_insert_column", "table_delete_column" -> WordImpressIcon.Table
    "print" -> WordImpressIcon.Print
    "share" -> WordImpressIcon.Share
    "text_bold", "text_italic", "text_underline", "text_strikethrough", "text_superscript", "text_subscript",
    "text_line_spacing", "text_color", "color_background", "text_clear_formatting", "text_insert", "text_add" -> WordImpressIcon.TextFont
    "text_bullet_list", "text_number_list_ltr", "text_number_list", "list" -> WordImpressIcon.List
    "text_align_left", "text_align_center", "text_align_right", "text_align_justify_low",
    "text_indent_decrease_ltr", "text_indent_increase_ltr" -> WordImpressIcon.TextFont
    "text_header_1", "text_footer", "book", "text_grammar_error", "number_symbol", "omega" -> WordImpressIcon.DocumentText
    "bookmark" -> WordImpressIcon.Pin
    "comment", "comment_add", "note_add" -> WordImpressIcon.DocumentText
    "ruler_horizontal", "border_all", "arrow_expand" -> WordImpressIcon.Grid
    "edit", "edit_off" -> WordImpressIcon.Edit
    "folder_open" -> WordImpressIcon.FolderOpen
    else -> WordImpressIcon.DocumentText
}
