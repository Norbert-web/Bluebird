package io.github.norbertweb.bluebird.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.editor.core.BottomPanel
import io.github.norbertweb.bluebird.editor.core.SplitOrientation
import io.github.norbertweb.bluebird.editor.core.ShellActivity
import io.github.norbertweb.bluebird.editor.editor.core.PremiumEditorState
import io.github.norbertweb.bluebird.ui.components.FluentIcon
import java.io.File
import io.github.norbertweb.bluebird.editor.ui.components.isWebPreviewSupported

private const val SIDEBAR_MIN = 190f
private const val SIDEBAR_MAX = 360f
private const val BOTTOM_MIN = 110f
private const val BOTTOM_MAX = 360f

@Composable
fun IdeShell(
    s: PremiumEditorState,
    onOpenFile: () -> Unit,
    onOpenWorkspacePath: (String) -> Unit = {},
    onSave: () -> Unit,
    onNewTab: () -> Unit,
    content: @Composable (Int) -> Unit,
) {
    val c = s.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    var sidebarWidth by remember { mutableFloatStateOf(250f) }
    var bottomHeight by remember { mutableFloatStateOf(170f) }

    LaunchedEffect(Unit) {
        val restored = io.github.norbertweb.bluebird.editor.utils.EditorPreferences.loadWorkspaceLayout(context)
        s.restoreWorkspaceLayout(restored)
    }
    LaunchedEffect(s.workspaceLayout) {
        io.github.norbertweb.bluebird.editor.utils.EditorPreferences.saveWorkspaceLayout(context, s.workspaceLayout)
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        IdeTitleBar(s, onSave = onSave, onNewTab = onNewTab)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            ActivityRail(s)
            ShellSidebar(
                s = s,
                width = sidebarWidth,
                onOpenFile = onOpenFile,
                onOpenWorkspacePath = onOpenWorkspacePath,
                onWidthChange = { sidebarWidth = it.coerceIn(SIDEBAR_MIN, SIDEBAR_MAX) },
            )
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .border(1.dp, c.border)
            ) {
                EditorGroupWorkspace(
                    s = s,
                    content = content,
                )
            }
        }
        if (s.showBottomPanel) {
            BottomToolPanel(
                s = s,
                height = bottomHeight,
                onHeightChange = { bottomHeight = it.coerceIn(BOTTOM_MIN, BOTTOM_MAX) },
            )
        }
    }
}

@Composable
private fun EditorGroupWorkspace(
    s: PremiumEditorState,
    content: @Composable (Int) -> Unit,
) {
    val c = s.colors
    val layout = s.workspaceLayout

    Column(Modifier.fillMaxSize().background(c.bg)) {
        if (layout.secondGroupVisible) {
            when (layout.orientation) {
                SplitOrientation.VERTICAL -> {
                    Row(Modifier.fillMaxSize()) {
                        EditorGroupPane(s, 0, Modifier.weight(layout.secondGroupRatio), content)
                        SplitterVertical(c) { s.setWorkspaceSplitRatio(layout.secondGroupRatio + it) }
                        EditorGroupPane(s, 1, Modifier.weight(1f - layout.secondGroupRatio), content)
                    }
                }
                SplitOrientation.HORIZONTAL -> {
                    Column(Modifier.fillMaxSize()) {
                        EditorGroupPane(s, 0, Modifier.weight(layout.secondGroupRatio), content)
                        SplitterHorizontal(c) { s.setWorkspaceSplitRatio(layout.secondGroupRatio + it) }
                        EditorGroupPane(s, 1, Modifier.weight(1f - layout.secondGroupRatio), content)
                    }
                }
                SplitOrientation.NONE -> EditorGroupPane(s, 0, Modifier.fillMaxSize(), content)
            }
        } else {
            EditorGroupPane(s, 0, Modifier.fillMaxSize(), content)
        }
    }
}

@Composable
private fun EditorGroupPane(
    s: PremiumEditorState,
    group: Int,
    modifier: Modifier,
    content: @Composable (Int) -> Unit,
) {
    val c = s.colors
    Column(modifier.border(1.dp, c.border)) {
        val activeGroup = s.activeEditorGroup == group
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(if (activeGroup) c.surfaceHover else c.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(2.dp).height(14.dp).background(if (activeGroup) c.accent else Color.Transparent, RoundedCornerShape(50)))
            Text(
                if (group == 0) "EDITOR GROUP 1" else "EDITOR GROUP 2",
                color = if (activeGroup) c.text else c.textMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 7.dp)
            )
            if (activeGroup) Text("ACTIVE", color = c.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (group == 1) ShellIconButton(FluentIcon.Close, "Close Group", c) { s.closeEditorGroup() }
        }
        Box(Modifier.fillMaxSize().clickable { s.activateEditorGroup(group) }) { content(group) }
    }
}

@Composable
private fun SplitterVertical(c: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors, onDrag: (Float) -> Unit) {
    Box(
        Modifier.width(6.dp).fillMaxHeight().pointerInput(Unit) {
            detectDragGestures { _, drag -> onDrag(drag.x / 1000f) }
        },
        contentAlignment = Alignment.Center
    ) { Box(Modifier.width(1.dp).fillMaxHeight().background(c.border)) }
}

@Composable
private fun SplitterHorizontal(c: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors, onDrag: (Float) -> Unit) {
    Box(
        Modifier.height(6.dp).fillMaxWidth().pointerInput(Unit) {
            detectDragGestures { _, drag -> onDrag(drag.y / 1000f) }
        },
        contentAlignment = Alignment.Center
    ) { Box(Modifier.fillMaxWidth().height(1.dp).background(c.border)) }
}

@Composable
private fun IdeTitleBar(s: PremiumEditorState, onSave: () -> Unit, onNewTab: () -> Unit) {
    val c = s.colors
    Row(
        Modifier.fillMaxWidth().height(40.dp).background(c.surface)
            .border(1.dp, c.border),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(start = 10.dp).size(20.dp).clip(RoundedCornerShape(5.dp)).background(c.accent), contentAlignment = Alignment.Center) {
            Icon(FluentIcon.Code, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(1.dp).height(20.dp).background(c.border))
        Spacer(Modifier.width(10.dp))
        Text("Bluebird VS Code", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("  •  ${s.fileName}", color = c.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        ShellIconButton(FluentIcon.Add, "New", c) { onNewTab() }
        ShellIconButton(FluentIcon.Save, "Save", c, enabled = s.isModified) { onSave() }
        ShellIconButton(FluentIcon.FolderOpen, "Workspace Overview", c) { s.showWorkspaceHome = true }
        ShellIconButton(FluentIcon.Search, "Command Palette", c) { s.showCommandPalette = true }
        if (isWebPreviewSupported(s.fileName)) {
            ShellIconButton(FluentIcon.Globe, if (s.showLivePreview) "Hide Live Preview" else "Live Preview", c) { s.showLivePreview = !s.showLivePreview }
        }
        ShellIconButton(FluentIcon.Window, "Split Right", c) { s.splitEditor(SplitOrientation.VERTICAL) }
        ShellIconButton(FluentIcon.Window, "Split Down", c) { s.splitEditor(SplitOrientation.HORIZONTAL) }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun ActivityRail(s: PremiumEditorState) {
    val c = s.colors
    val entries = listOf(
        ShellActivity.EXPLORER to (FluentIcon.FolderOpen to "Explorer"),
        ShellActivity.SEARCH to (FluentIcon.Search to "Search"),
        ShellActivity.SOURCE_CONTROL to (FluentIcon.ArrowSync to "Source Control"),
        ShellActivity.RUN_DEBUG to (FluentIcon.Play to "Run and Debug"),
        ShellActivity.EXTENSIONS to (FluentIcon.Extension to "Extensions"),
    )
    Column(
        Modifier.width(48.dp).fillMaxHeight().background(c.surface)
            .border(1.dp, c.border),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))
        entries.forEach { (activity, pair) ->
            val selected = s.shellActivity == activity
            Box(
                Modifier.padding(vertical = 2.dp).size(40.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (selected) c.surfaceHover else Color.Transparent)
                    .clickable {
                        s.shellActivity = activity
                        when (activity) {
                            ShellActivity.SEARCH -> s.showFindBar = true
                            ShellActivity.SOURCE_CONTROL, ShellActivity.RUN_DEBUG -> {
                                s.bottomPanel = if (activity == ShellActivity.SOURCE_CONTROL) BottomPanel.OUTPUT else BottomPanel.PROBLEMS
                                s.showBottomPanel = true
                            }
                            else -> Unit
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.align(Alignment.CenterStart).width(2.dp).height(24.dp).background(c.accent))
                Icon(pair.first, pair.second, tint = if (selected) c.accent else c.textMuted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.padding(vertical = 6.dp).size(40.dp).clip(RoundedCornerShape(7.dp))
                .clickable { s.showSettingsPanel = true },
            contentAlignment = Alignment.Center
        ) { Icon(FluentIcon.Settings, "Settings", tint = c.textMuted, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun ShellSidebar(
    s: PremiumEditorState,
    width: Float,
    onOpenFile: () -> Unit,
    onOpenWorkspacePath: (String) -> Unit,
    onWidthChange: (Float) -> Unit,
) {
    val c = s.colors
    Box(Modifier.width(width.dp).fillMaxHeight()) {
        if (s.shellActivity == ShellActivity.EXPLORER) {
            ExplorerSidebar(s, onOpenFile, onOpenWorkspacePath)
        } else {
            UtilitySidebar(s)
        }
        Box(
            Modifier.align(Alignment.CenterEnd).width(6.dp).fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures { _, drag -> onWidthChange(width + drag.x) }
                }
        )
        Box(Modifier.align(Alignment.CenterEnd).width(1.dp).fillMaxHeight().background(c.border))
    }
}

@Composable
private fun ExplorerSidebar(
    s: PremiumEditorState,
    onOpenFile: () -> Unit,
    onOpenWorkspacePath: (String) -> Unit,
) {
    val c = s.colors
    val parent = s.filePath.takeIf { it.isNotEmpty() }?.let { File(it).parentFile }
    val root = parent?.name?.ifBlank { parent.path } ?: "WORKSPACE"
    Column(
        Modifier.fillMaxSize().background(c.surface).padding(vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("EXPLORER", color = c.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            ShellIconButton(FluentIcon.ArrowSync, "Refresh", c) { s.explorerRefreshKey++ }
            ShellIconButton(FluentIcon.Add, "Open File", c) { onOpenFile() }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(FluentIcon.FolderOpen, null, tint = c.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(root.uppercase(), color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        if (parent == null || !parent.isDirectory) {
            Text("Open a file to populate the workspace.", color = c.textMuted, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
            Text("OPEN FILE", color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clickable { onOpenFile() })
        } else {
            WorkspaceTree(
                directory = parent,
                activePath = s.filePath,
                refreshKey = s.explorerRefreshKey,
                onOpen = onOpenWorkspacePath,
                colors = c,
            )
        }
    }
}

@Composable
private fun WorkspaceTree(
    directory: File,
    activePath: String,
    refreshKey: Int,
    onOpen: (String) -> Unit,
    colors: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors,
) {
    val entries = remember(directory.absolutePath, refreshKey) {
        runCatching {
            directory.listFiles()
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?.toList()
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(entries, key = { it.absolutePath }) { file ->
            WorkspaceTreeRow(file, file.absolutePath == activePath, onOpen, colors)
        }
    }
}

@Composable
private fun WorkspaceTreeRow(
    file: File,
    active: Boolean,
    onOpen: (String) -> Unit,
    colors: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) colors.surfaceHover else Color.Transparent)
            .clickable(enabled = file.isFile) { onOpen(file.absolutePath) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (file.isDirectory) FluentIcon.FolderOpen else FluentIcon.Document,
            null,
            tint = if (file.isDirectory) colors.textMuted else colors.accent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(file.name, color = if (active) colors.text else colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UtilitySidebar(s: PremiumEditorState) {
    val c = s.colors
    Column(Modifier.fillMaxSize().background(c.surface).padding(12.dp)) {
        val (title, icon) = when (s.shellActivity) {
            ShellActivity.SEARCH -> "SEARCH" to FluentIcon.Search
            ShellActivity.SOURCE_CONTROL -> "SOURCE CONTROL" to FluentIcon.ArrowSync
            ShellActivity.RUN_DEBUG -> "RUN AND DEBUG" to FluentIcon.Play
            ShellActivity.EXTENSIONS -> "EXTENSIONS" to FluentIcon.Extension
            ShellActivity.EXPLORER -> "EXPLORER" to FluentIcon.FolderOpen
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = c.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        when (s.shellActivity) {
            ShellActivity.SEARCH -> Text("Search is ready in the editor. Use Ctrl+Shift+F for workspace search in the next search-engine phase.", color = c.textMuted, fontSize = 11.sp)
            ShellActivity.SOURCE_CONTROL -> Text("Source Control panel is connected to the Fluent shell. Git operations will land in Phase 7.", color = c.textMuted, fontSize = 11.sp)
            ShellActivity.RUN_DEBUG -> Text("Run and Debug is part of the workspace shell. Debug adapters arrive in Phase 8.", color = c.textMuted, fontSize = 11.sp)
            ShellActivity.EXTENSIONS -> Text("Extensions are reserved for the isolated extension host planned for Phase 9.", color = c.textMuted, fontSize = 11.sp)
            ShellActivity.EXPLORER -> Unit
        }
    }
}

@Composable
private fun BottomToolPanel(s: PremiumEditorState, height: Float, onHeightChange: (Float) -> Unit) {
    val c = s.colors
    Column(Modifier.fillMaxWidth().height(height.dp).background(c.surface)) {
        Box(
            Modifier.fillMaxWidth().height(5.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag -> onHeightChange(height - drag.y) }
                }
        )
        Row(Modifier.fillMaxWidth().height(32.dp).horizontalScroll(rememberScrollState()).border(1.dp, c.border), verticalAlignment = Alignment.CenterVertically) {
            BottomTab(s, BottomPanel.PROBLEMS, "PROBLEMS")
            BottomTab(s, BottomPanel.OUTPUT, "OUTPUT")
            BottomTab(s, BottomPanel.TERMINAL, "TERMINAL")
            Spacer(Modifier.weight(1f))
            ShellIconButton(FluentIcon.Close, "Close Panel", c) { s.showBottomPanel = false }
        }
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            when (s.bottomPanel) {
                BottomPanel.PROBLEMS -> {
                    val diagnostics = s.diagnostics
                    if (diagnostics.isEmpty()) {
                        Text("No problems detected.", color = c.textMuted, fontSize = 11.sp)
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(diagnostics) { diagnostic ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp))
                                        .clickable { s.goToLine(diagnostic.line) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (diagnostic.severity == io.github.norbertweb.bluebird.editor.core.DiagnosticSeverity.ERROR) FluentIcon.Error
                                        else if (diagnostic.severity == io.github.norbertweb.bluebird.editor.core.DiagnosticSeverity.WARNING) FluentIcon.Warning
                                        else FluentIcon.Info,
                                        null, tint = c.textMuted, modifier = Modifier.size(14.dp)
                                    )
                                    Text("${diagnostic.line}:${diagnostic.column}", color = c.textMuted, fontSize = 10.sp)
                                    Text(diagnostic.message, color = c.text, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                BottomPanel.OUTPUT -> Text("Bluebird output will appear here.", color = c.textMuted, fontSize = 11.sp)
                BottomPanel.TERMINAL -> Text("Terminal integration is planned for Phase 6.", color = c.textMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun BottomTab(s: PremiumEditorState, tab: BottomPanel, label: String) {
    val c = s.colors
    Box(
        Modifier.height(32.dp).clip(RoundedCornerShape(5.dp))
            .background(if (s.bottomPanel == tab) c.surfaceHover else Color.Transparent)
            .clickable { s.bottomPanel = tab; s.showBottomPanel = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (s.bottomPanel == tab) c.text else c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShellIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    c: io.github.norbertweb.bluebird.editor.ui.theme.EditorColors,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            .background(if (enabled) Color.Transparent else c.surfaceHover.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = if (enabled) c.textMuted else c.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}
