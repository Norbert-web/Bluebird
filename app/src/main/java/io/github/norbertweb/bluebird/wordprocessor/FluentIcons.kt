package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.*
import fluent.ui.system.icons.regular.*

/**
 * Single icon gateway for Word Impress.
 *
 * Artwork is now supplied by io.github.niyajali:fluentui-system-icons:1.0.1.
 * The API exposes Microsoft Fluent System Icons as type-safe Compose ImageVectors,
 * so the word processor no longer depends on Android drawable/resource-name lookup.
 *
 * The string names are intentionally kept stable so the rest of the existing UI does
 * not need to change while the underlying icon provider is migrated.
 */
@Composable
fun FluentIcon(
    name: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Int = 24
) {
    // `size` is retained for source compatibility with the existing UI. Fluent's
    // Compose vectors are resolution-independent, so their rendered size is controlled
    // by the caller's Modifier.size(...).
    @Suppress("UNUSED_VARIABLE")
    val requestedSize = size
    Icon(
        imageVector = fluentImageVector(name, filled),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Convenience painter for places that need a Painter instead of an Icon composable.
 */
@Composable
fun fluentIconPainter(name: String, filled: Boolean = false, size: Int = 24): Painter {
    @Suppress("UNUSED_VARIABLE")
    val requestedSize = size
    return rememberVectorPainter(fluentImageVector(name, filled))
}

private fun fluentImageVector(name: String, filled: Boolean): ImageVector = when (name) {
    "add" -> if (filled) FluentIcons.Filled.Add else FluentIcons.Regular.Add
    "arrow_clockwise" -> if (filled) FluentIcons.Filled.ArrowClockwise else FluentIcons.Regular.ArrowClockwise
    "arrow_redo" -> if (filled) FluentIcons.Filled.ArrowRedo else FluentIcons.Regular.ArrowRedo
    "arrow_rotate_clockwise" -> if (filled) FluentIcons.Filled.ArrowRotateClockwise else FluentIcons.Regular.ArrowRotateClockwise
    "arrow_undo" -> if (filled) FluentIcons.Filled.ArrowUndo else FluentIcons.Regular.ArrowUndo
    "checkmark" -> if (filled) FluentIcons.Filled.Checkmark else FluentIcons.Regular.Checkmark
    "chevron_down" -> if (filled) FluentIcons.Filled.ChevronDown else FluentIcons.Regular.ChevronDown
    "clipboard_paste" -> if (filled) FluentIcons.Filled.ClipboardPaste else FluentIcons.Regular.ClipboardPaste
    "circle" -> if (filled) FluentIcons.Filled.Circle else FluentIcons.Regular.Circle
    "copy" -> if (filled) FluentIcons.Filled.Copy else FluentIcons.Regular.Copy
    "cut" -> if (filled) FluentIcons.Filled.Cut else FluentIcons.Regular.Cut
    "delete" -> if (filled) FluentIcons.Filled.Delete else FluentIcons.Regular.Delete
    "dismiss" -> if (filled) FluentIcons.Filled.Dismiss else FluentIcons.Regular.Dismiss
    "document_page_number" -> if (filled) FluentIcons.Filled.DocumentPageNumber else FluentIcons.Regular.DocumentPageNumber
    "document_pdf" -> if (filled) FluentIcons.Filled.DocumentPdf else FluentIcons.Regular.DocumentPdf
    "document_text" -> if (filled) FluentIcons.Filled.DocumentText else FluentIcons.Regular.DocumentText
    "text_color" -> if (filled) FluentIcons.Filled.TextColor else FluentIcons.Regular.TextColor
    "color_background" -> if (filled) FluentIcons.Filled.ColorBackground else FluentIcons.Regular.ColorBackground
    "text_clear_formatting" -> if (filled) FluentIcons.Filled.TextClearFormatting else FluentIcons.Regular.TextClearFormatting
    "text_indent_decrease_ltr" -> FluentIcons.Regular.TextLineSpacing
    "text_indent_increase_ltr" -> FluentIcons.Regular.TextLineSpacing
    "text_line_spacing" -> if (filled) FluentIcons.Filled.TextLineSpacing else FluentIcons.Regular.TextLineSpacing
    "full_screen_minimize" -> if (filled) FluentIcons.Filled.FullScreenMinimize else FluentIcons.Regular.FullScreenMinimize
    "lock_closed" -> if (filled) FluentIcons.Filled.LockClosed else FluentIcons.Regular.LockClosed
    "more_vertical" -> if (filled) FluentIcons.Filled.MoreVertical else FluentIcons.Regular.MoreVertical
    "panel_left" -> if (filled) FluentIcons.Filled.PanelLeft else FluentIcons.Regular.PanelLeft
    "save" -> if (filled) FluentIcons.Filled.Save else FluentIcons.Regular.Save
    "search" -> if (filled) FluentIcons.Filled.Search else FluentIcons.Regular.Search
    "settings" -> if (filled) FluentIcons.Filled.Settings else FluentIcons.Regular.Settings
    "subtract" -> if (filled) FluentIcons.Filled.Subtract else FluentIcons.Regular.Subtract
    "table_delete_column" -> if (filled) FluentIcons.Filled.TableDeleteColumn else FluentIcons.Regular.TableDeleteColumn
    "table_delete_row" -> if (filled) FluentIcons.Filled.TableDeleteRow else FluentIcons.Regular.TableDeleteRow
    "table_insert_column" -> if (filled) FluentIcons.Filled.TableInsertColumn else FluentIcons.Regular.TableInsertColumn
    "table_insert_row" -> if (filled) FluentIcons.Filled.TableInsertRow else FluentIcons.Regular.TableInsertRow
    "zoom_in" -> if (filled) FluentIcons.Filled.ZoomIn else FluentIcons.Regular.ZoomIn
    "zoom_out" -> if (filled) FluentIcons.Filled.ZoomOut else FluentIcons.Regular.ZoomOut
    else -> if (filled) FluentIcons.Filled.Dismiss else FluentIcons.Regular.Dismiss
}
