package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// Pagination.kt — compatibility API around the shared DocumentLayoutEngine.
// ============================================================================================

data class IndexedBlock(val index: Int, val block: Block)
data class DocPage(val entries: List<IndexedBlock>)

/**
 * The one public pagination entry point used throughout the app. All layout decisions are made
 * by DocumentLayoutEngine so editor status, navigation, printing metadata and TOC references see
 * the same page model.
 */
fun paginate(doc: WordDocument): List<DocPage> = DocumentLayoutEngine.layout(doc).pages

fun layoutDocument(doc: WordDocument): DocumentLayoutResult = DocumentLayoutEngine.layout(doc)
