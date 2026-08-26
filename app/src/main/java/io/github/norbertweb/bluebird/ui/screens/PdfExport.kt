package io.github.norbertweb.bluebird.ui.screens

// ============================================================================================
// PdfExport.kt — walks the block tree onto pages sized per doc.pageSettings, using
// android.graphics.pdf.PdfDocument. Renders paragraphs, images, a basic bordered table grid,
// a simple TOC listing, and (if enabled) a repeating header/footer with a {page} number field.
// ============================================================================================

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign

private const val PAGE_NUMBER_TOKEN = "{page}"

fun exportDocToPdf(doc: WordDocument): PdfDocument {
    val pdf = PdfDocument()
    val (pageWidthF, pageHeightF) = doc.pageSettings.dimensionsPt()
    val pageWidth = pageWidthF.toInt()
    val pageHeight = pageHeightF.toInt()
    val marginTop = doc.pageSettings.marginTopPt
    val marginBottom = doc.pageSettings.marginBottomPt
    val marginLeft = doc.pageSettings.marginLeftPt
    val marginRight = doc.pageSettings.marginRightPt
    val maxWidth = pageWidth - marginLeft - marginRight
    val headerFooterReserve = (if (doc.showHeader) 24f else 0f) + (if (doc.showFooter) 24f else 0f)

    var pageNum = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas = page.canvas
    var y = marginTop + (if (doc.showHeader) 24f else 0f)
    val bottomLimit = pageHeight - marginBottom - (if (doc.showFooter) 24f else 0f)

    fun drawHeaderFooter(onPage: Int) {
        val hfPaint = Paint().apply { textSize = 10f; isAntiAlias = true; color = android.graphics.Color.DKGRAY }
        if (doc.showHeader && doc.headerParagraph.field.text.isNotBlank()) {
            val text = doc.headerParagraph.field.text.replace(PAGE_NUMBER_TOKEN, onPage.toString())
            canvas.drawText(text, marginLeft, marginTop, hfPaint)
        }
        if (doc.showFooter && doc.footerParagraph.field.text.isNotBlank()) {
            val text = doc.footerParagraph.field.text.replace(PAGE_NUMBER_TOKEN, onPage.toString())
            canvas.drawText(text, marginLeft, pageHeight - marginBottom + 14f, hfPaint)
        }
    }

    fun newPage() {
        drawHeaderFooter(pageNum)
        pdf.finishPage(page)
        pageNum++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = marginTop + (if (doc.showHeader) 24f else 0f)
    }

    for (block in doc.blocks) {
        when (block) {
            is PageBreakBlock -> newPage()

            is ParagraphBlock -> {
                val style = BuiltInStyles.byId(block.styleId)
                val paint = Paint().apply {
                    textSize = style.fontSize.toFloat() * 1.1f
                    isAntiAlias = true
                    isFakeBoldText = style.bold
                    textSkewX = if (style.italic) -0.2f else 0f
                    color = style.color.toArgb()
                }
                val lineHeight = paint.fontSpacing
                y += style.spacingBefore.toFloat() * 0.5f
                val text = block.field.text
                val rawLines = if (text.isEmpty()) listOf("") else text.split("\n")
                for (raw in rawLines) {
                    var remaining = raw
                    if (remaining.isEmpty()) {
                        if (y + lineHeight > bottomLimit) newPage()
                        y += lineHeight
                        continue
                    }
                    while (remaining.isNotEmpty()) {
                        if (y + lineHeight > bottomLimit) newPage()
                        val count = paint.breakText(remaining, true, maxWidth, null)
                        var breakAt = count
                        if (breakAt < remaining.length) {
                            val lastSpace = remaining.lastIndexOf(' ', breakAt - 1)
                            if (lastSpace > 0) breakAt = lastSpace
                        }
                        val line = remaining.substring(0, breakAt).trimEnd()
                        val align = block.alignmentOverride ?: style.alignment
                        val x = when (align) {
                            TextAlign.Center -> marginLeft + (maxWidth - paint.measureText(line)) / 2
                            TextAlign.End -> marginLeft + maxWidth - paint.measureText(line)
                            else -> marginLeft
                        }
                        canvas.drawText(line, x, y + lineHeight * 0.8f, paint)
                        y += lineHeight
                        remaining = remaining.substring(breakAt).trimStart()
                    }
                }
                y += style.spacingAfter.toFloat() * 0.5f
            }

            is ImageBlock -> {
                block.bitmap?.let { bmp ->
                    val w = minOf(maxWidth, block.widthDp.toFloat())
                    val h = w * bmp.height / bmp.width.toFloat()
                    if (y + h > bottomLimit) newPage()
                    val rotated = if (block.rotationDeg != 0) {
                        val matrix = android.graphics.Matrix().apply { postRotate(block.rotationDeg.toFloat()) }
                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    } else bmp
                    val scaled = Bitmap.createScaledBitmap(rotated, w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), true)
                    val x = when (block.alignment) {
                        TextAlign.Center -> marginLeft + (maxWidth - w) / 2
                        TextAlign.End -> marginLeft + maxWidth - w
                        else -> marginLeft
                    }
                    canvas.drawBitmap(scaled, x, y, null)
                    y += h + 10f
                }
            }

            is TableBlock -> {
                val textPaint = Paint().apply { textSize = 11f; isAntiAlias = true; color = android.graphics.Color.BLACK }
                val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = android.graphics.Color.GRAY }
                val fillPaint = Paint().apply { style = Paint.Style.FILL }
                val cols = block.rows.firstOrNull()?.cells?.size ?: 0
                if (cols > 0) {
                    val colWidth = maxWidth / cols
                    val rowHeight = 24f
                    for (row in block.rows) {
                        if (y + rowHeight > bottomLimit) newPage()
                        row.cells.forEachIndexed { c, cell ->
                            val cx = marginLeft + c * colWidth
                            cell.backgroundColor?.let { bg ->
                                fillPaint.color = bg.toArgb()
                                canvas.drawRect(cx, y, cx + colWidth, y + rowHeight, fillPaint)
                            }
                            canvas.drawRect(cx, y, cx + colWidth, y + rowHeight, borderPaint)
                            val cellText = cell.blocks.joinToString(" ") { it.field.text }
                            canvas.drawText(cellText.take(40), cx + 4, y + rowHeight * 0.65f, textPaint)
                        }
                        y += rowHeight
                    }
                    y += 10f
                }
            }

            is TocBlock -> {
                val headingPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; isAntiAlias = true; color = android.graphics.Color.BLACK }
                val entryPaint = Paint().apply { textSize = 12f; isAntiAlias = true; color = android.graphics.Color.DKGRAY }
                if (y + 24f > bottomLimit) newPage()
                canvas.drawText("Table of Contents", marginLeft, y + 16f, headingPaint)
                y += 28f
                block.entries.forEach { entry ->
                    if (y + 18f > bottomLimit) newPage()
                    canvas.drawText(entry.text, marginLeft + entry.level * 16f, y + 12f, entryPaint)
                    y += 18f
                }
                y += 10f
            }
        }
    }
    drawHeaderFooter(pageNum)
    pdf.finishPage(page)
    return pdf
}
