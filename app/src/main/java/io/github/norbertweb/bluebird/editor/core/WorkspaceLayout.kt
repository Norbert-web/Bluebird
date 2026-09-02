package io.github.norbertweb.bluebird.editor.core

/** Persistent IDE workspace layout. Kept independent from document state so the editor engine stays reusable. */
data class WorkspaceLayout(
    val orientation: SplitOrientation = SplitOrientation.NONE,
    val secondGroupVisible: Boolean = false,
    val secondGroupRatio: Float = 0.5f,
    val primaryTabId: String? = null,
    val secondaryTabId: String? = null,
    val primaryTabIds: List<String> = emptyList(),
    val secondaryTabIds: List<String> = emptyList(),
)

enum class SplitOrientation { NONE, VERTICAL, HORIZONTAL }
