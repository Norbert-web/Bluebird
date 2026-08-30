package io.github.norbertweb.bluebird.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Microsoft Fluent System UI icons (https://github.com/niyajali/fluentui-system-icons) —
// same icon set used across Bluebird OS (see MediaPlayerScreen.kt). FluentIcons.Filled.*
// is used for emphasis/active states, FluentIcons.Regular.* everywhere else.

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.Checkmark
import fluent.ui.system.icons.filled.CheckmarkCircle
import fluent.ui.system.icons.filled.Home
import fluent.ui.system.icons.regular.Alert
import fluent.ui.system.icons.regular.Apps
import fluent.ui.system.icons.regular.AppsList
import fluent.ui.system.icons.regular.ArrowRight
import fluent.ui.system.icons.regular.Camera
import fluent.ui.system.icons.regular.CameraAdd
import fluent.ui.system.icons.regular.ChevronRight
import fluent.ui.system.icons.regular.CloudArrowUp
import fluent.ui.system.icons.regular.DocumentText
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.Grid
import fluent.ui.system.icons.regular.HandLeft
import fluent.ui.system.icons.regular.HardDrive
import fluent.ui.system.icons.regular.Image
import fluent.ui.system.icons.regular.ImageMultiple
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.LockClosedKey
import fluent.ui.system.icons.regular.Open
import fluent.ui.system.icons.regular.PaintBrush
import fluent.ui.system.icons.regular.Person
import fluent.ui.system.icons.regular.Settings
import fluent.ui.system.icons.regular.Shapes
import fluent.ui.system.icons.regular.ShieldCheckmark
import fluent.ui.system.icons.regular.TopSpeed
import fluent.ui.system.icons.regular.WeatherMoon
import io.github.norbertweb.bluebird.LauncherViewModel
import java.io.File

// ─────────────────────────────────────────────────────────
// Fluent icon set — every icon this screen uses, centralized in one place
// (mirrors the same pattern used in MediaPlayerScreen.kt) so a name that
// doesn't resolve in your installed library version is a one-line fix here
// rather than a hunt through the whole file.
// ─────────────────────────────────────────────────────────
private object SetupFI {
    val Home         = FluentIcons.Filled.Home
    val Lock         = FluentIcons.Regular.LockClosedKey
    val Person       = FluentIcons.Regular.Person
    val Image        = FluentIcons.Regular.Image
    val Description  = FluentIcons.Regular.DocumentText
    val FolderOpen   = FluentIcons.Regular.FolderOpen
    val Storage      = FluentIcons.Regular.HardDrive
    val PhotoCamera  = FluentIcons.Regular.Camera
    val Notifications = FluentIcons.Regular.Alert
    val ArrowForward = FluentIcons.Regular.ArrowRight
    val Check        = FluentIcons.Filled.Checkmark
    val CheckCircle  = FluentIcons.Filled.CheckmarkCircle
    val ChevronRight = FluentIcons.Regular.ChevronRight
    val Dashboard    = FluentIcons.Regular.AppsList
    val Security     = FluentIcons.Regular.ShieldCheckmark
    val Settings     = FluentIcons.Regular.Settings
    val Speed        = FluentIcons.Regular.TopSpeed
    val AddAPhoto    = FluentIcons.Regular.CameraAdd
    val PhotoLibrary = FluentIcons.Regular.ImageMultiple
    val SkipNext     = FluentIcons.Regular.ArrowRight
    val Info         = FluentIcons.Regular.Info
    val OpenInNew    = FluentIcons.Regular.Open
    // Feature-tile icons for the new "Customize your setup" step
    val Palette      = FluentIcons.Regular.PaintBrush

    val DarkMode     = FluentIcons.Regular.WeatherMoon
    val Widgets      = FluentIcons.Regular.Grid
    val AppSuggest   = FluentIcons.Regular.Apps
    val Backup       = FluentIcons.Regular.CloudArrowUp
    val Gestures     = FluentIcons.Regular.HandLeft
    val IconPack     = FluentIcons.Regular.Shapes
}

// ─────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────

private data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>,
    val optional: Boolean = false,
    val requiresSettingsIntent: Boolean = false,
    val settingsIntentAction: String? = null,
    val isManageAllFiles: Boolean = false
)

private data class StepMeta(val icon: ImageVector, val label: String, val subtitle: String)

private val stepMeta = listOf(
    StepMeta(SetupFI.Home,        "Welcome",     "Get started"),
    StepMeta(SetupFI.Lock,        "Permissions", "Access & privacy"),
    StepMeta(SetupFI.Person,      "Profile",     "Name & identity"),
    StepMeta(SetupFI.Image,       "Photo",       "Profile picture"),
    StepMeta(SetupFI.Palette,     "Customize",   "Make it yours"),
    StepMeta(SetupFI.Description, "Legal",       "Terms & privacy"),
)

private enum class StepState { DONE, ACTIVE, UPCOMING }

/**
 * A single "Customize your setup" tile — modelled on Windows 11's post-install
 * OOBE customization page: a short list of feature toggles that ship with a
 * sensible recommended default, can be switched off individually, and can be
 * skipped as a group without blocking setup completion.
 *
 * `settingKey` is a stable identifier meant to be wired to wherever your app
 * persists preferences (DataStore/SharedPreferences/ViewModel) — this screen
 * only tracks the in-memory toggle state during setup; wiring each key to an
 * actual feature flag is left for you to fill in at the `onApply` callsite.
 */
private data class CustomizeTile(
    val settingKey: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val recommendedDefault: Boolean
)

private val customizeTiles = listOf(
    CustomizeTile(
        settingKey = "dark_mode_follow_system",
        icon = SetupFI.DarkMode,
        title = "Match system theme",
        description = "Automatically switch between light and dark based on your device setting.",
        recommendedDefault = true
    ),
    CustomizeTile(
        settingKey = "home_screen_widgets",
        icon = SetupFI.Widgets,
        title = "Add starter widgets",
        description = "Place a clock and a favorites widget on your home screen to begin with.",
        recommendedDefault = true
    ),
    CustomizeTile(
        settingKey = "app_suggestions",
        icon = SetupFI.AppSuggest,
        title = "Smart app suggestions",
        description = "Surface a row of apps you're likely to open next, based on time and habit.",
        recommendedDefault = true
    ),
    CustomizeTile(
        settingKey = "icon_pack_rounded",
        icon = SetupFI.IconPack,
        title = "Rounded icon style",
        description = "Use a softer, rounded icon shape across the launcher instead of square.",
        recommendedDefault = false
    ),
    CustomizeTile(
        settingKey = "gesture_navigation",
        icon = SetupFI.Gestures,
        title = "Swipe gestures",
        description = "Swipe up from the home screen for search, and swipe down for notifications.",
        recommendedDefault = true
    ),
    CustomizeTile(
        settingKey = "backup_settings",
        icon = SetupFI.Backup,
        title = "Back up settings",
        description = "Keep a local backup of your layout and preferences so you can restore them later.",
        recommendedDefault = false
    ),
)

private fun areAllPermissionsGranted(
    context: android.content.Context,
    permissions: List<String>
): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(context, it) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

// ─────────────────────────────────────────────────────────
// Root Setup Screen
// ─────────────────────────────────────────────────────────

@Composable
fun SetupScreen(
    viewModel: LauncherViewModel,
    onRequestNotificationAccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.setupStep
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    var setupCompleted by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf<PermissionItem?>(null) }
    var showManageStorageDialog by remember { mutableStateOf(false) }

    // Hoisted here (not inside CustomizeStep) so toggle choices survive the
    // user tapping Back into this step from Legal and returning — the same
    // reason UsernameStep's text is threaded through uiState rather than
    // living in a `remember` local to that one step.
    val customizeSelections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            customizeTiles.forEach { put(it.settingKey, it.recommendedDefault) }
        }
    }

    // Build permission list based on API level
    val permissionItems = remember {
        val storagePerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        buildList {
            add(
                PermissionItem(
                    "Media & Files",
                    "Read photos, videos, and audio from your device",
                    SetupFI.FolderOpen,
                    storagePerms,
                    optional = false
                )
            )
            // All-files / MANAGE_EXTERNAL_STORAGE (API 30+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    PermissionItem(
                        "Manage All Files",
                        "Required for file manager features and full storage access",
                        SetupFI.Storage,
                        emptyList(),
                        optional = true,
                        requiresSettingsIntent = true,
                        settingsIntentAction = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        isManageAllFiles = true
                    )
                )
            }
            add(
                PermissionItem(
                    "Camera",
                    "Take photos for your profile picture",
                    SetupFI.PhotoCamera,
                    listOf(Manifest.permission.CAMERA),
                    optional = false
                )
            )
            add(
                PermissionItem(
                    "Notifications",
                    "Show app notifications on your home screen",
                    SetupFI.Notifications,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        listOf(Manifest.permission.POST_NOTIFICATIONS)
                    else emptyList(),
                    optional = true,
                    requiresSettingsIntent = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                    settingsIntentAction = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS else null
                )
            )
        }
    }

    val grantedStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionItems.forEach { item ->
                put(item.title, when {
                    item.isManageAllFiles && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                        Environment.isExternalStorageManager()
                    item.permissions.isEmpty() -> false
                    else -> areAllPermissionsGranted(context, item.permissions)
                })
            }
        }
    }

    var requestedItem by remember { mutableStateOf<PermissionItem?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        requestedItem?.let { item ->
            val granted = item.permissions.all { results[it] == true }
            grantedStates[item.title] = granted
            if (!granted && item.permissions.isNotEmpty()) {
                showPermissionRationale = item
            }
        }
        requestedItem = null
    }

    val colorScheme = MaterialTheme.colorScheme
    val bg = colorScheme.background
    val surface = colorScheme.surface
    val outline = colorScheme.outlineVariant
    val primary = colorScheme.primary
    val onBackground = colorScheme.onBackground
    val surfaceVariant = colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        if (setupCompleted) {
            DefaultLauncherDialog(
                onSetDefault = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                    viewModel.completeSetup()
                },
                onSkip = { viewModel.completeSetup() }
            )
        } else {
            if (isTablet) {
                // ── Tablet: sidebar + content ──
                Row(modifier = Modifier.fillMaxSize()) {
                    LeftSidebar(
                        currentStep = step,
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = outline
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        StepContent(
                            step = step,
                            viewModel = viewModel,
                            uiState = uiState,
                            permissionItems = permissionItems,
                            grantedStates = grantedStates,
                            customizeSelections = customizeSelections,
                            onRequestPermission = { item ->
                                if (item.isManageAllFiles && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    showManageStorageDialog = true
                                } else if (item.requiresSettingsIntent && item.settingsIntentAction != null) {
                                    context.startActivity(Intent(item.settingsIntentAction).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    })
                                } else {
                                    requestedItem = item
                                    permLauncher.launch(item.permissions.toTypedArray())
                                }
                            },
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onBack = { viewModel.decrementSetupStep() },
                            onSetupCompleted = { setupCompleted = true }
                        )
                    }
                }
            } else {
                // ── Phone: top stepper + content ──
                Column(modifier = Modifier.fillMaxSize()) {
                    TopStepBar(currentStep = step, total = stepMeta.size)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        StepContent(
                            step = step,
                            viewModel = viewModel,
                            uiState = uiState,
                            permissionItems = permissionItems,
                            grantedStates = grantedStates,
                            customizeSelections = customizeSelections,
                            onRequestPermission = { item ->
                                if (item.isManageAllFiles && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    showManageStorageDialog = true
                                } else if (item.requiresSettingsIntent && item.settingsIntentAction != null) {
                                    context.startActivity(Intent(item.settingsIntentAction).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    })
                                } else {
                                    requestedItem = item
                                    permLauncher.launch(item.permissions.toTypedArray())
                                }
                            },
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onBack = { viewModel.decrementSetupStep() },
                            onSetupCompleted = { setupCompleted = true }
                        )
                    }
                }
            }
        }

        // Permission rationale dialog
        showPermissionRationale?.let { item ->
            AlertDialog(
                onDismissRequest = { showPermissionRationale = null },
                icon = { Icon(item.icon, contentDescription = null, tint = primary) },
                title = { Text("${item.title} permission needed") },
                text = {
                    Text(
                        if (item.optional)
                            "This permission is optional. Without it, some features related to ${item.title.lowercase()} will not be available. You can grant it later in Android Settings."
                        else
                            "${item.title} access is required for core features. Please grant it in Android Settings to continue.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionRationale = null
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }) { Text("Open Settings") }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationale = null }) { Text("Dismiss") }
                }
            )
        }

        // Manage All Files dialog
        if (showManageStorageDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog(
                onDismissRequest = { showManageStorageDialog = false },
                icon = { Icon(SetupFI.Storage, contentDescription = null, tint = primary) },
                title = { Text("Manage all files access") },
                text = {
                    Text(
                        "Bluebird OS needs access to manage all files on your device. You'll be redirected to Android Settings where you can enable this. This is optional — you can skip it and enable it later.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showManageStorageDialog = false
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }) { Text("Open Settings") }
                },
                dismissButton = {
                    TextButton(onClick = { showManageStorageDialog = false }) { Text("Skip") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step Content Router
// ─────────────────────────────────────────────────────────

@Composable
private fun StepContent(
    step: Int,
    viewModel: LauncherViewModel,
    uiState: io.github.norbertweb.bluebird.LauncherUiState,
    permissionItems: List<PermissionItem>,
    grantedStates: Map<String, Boolean>,
    customizeSelections: SnapshotStateMap<String, Boolean>,
    onRequestPermission: (PermissionItem) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onBack: () -> Unit,
    onSetupCompleted: () -> Unit
) {
    // Capture context outside AnimatedContent so it's safe in non-composable lambdas
    val context = LocalContext.current

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState > initialState
            val enter = slideInHorizontally(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetX = { if (forward) it / 3 else -it / 3 }
            ) + fadeIn(tween(320))
            val exit = slideOutHorizontally(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                targetOffsetX = { if (forward) -it / 3 else it / 3 }
            ) + fadeOut(tween(320))
            enter togetherWith exit
        },
        label = "setup_step"
    ) { currentStep ->
        when (currentStep) {
            0 -> WelcomeStep(onNext = { viewModel.advanceSetupStep() })
            1 -> PermissionsStep(
                permissionItems = permissionItems,
                grantedStates = grantedStates,
                onRequestPermission = onRequestPermission,
                onRequestNotificationAccess = onRequestNotificationAccess,
                onNext = { viewModel.advanceSetupStep() },
                onBack = onBack
            )
            2 -> UsernameStep(
                currentName = uiState.userProfile.userName,
                onNameChange = { viewModel.setUserName(it) },
                onNext = { viewModel.advanceSetupStep() },
                onBack = onBack
            )
            3 -> AvatarStep(
                context = context,
                onAvatarPicked = { viewModel.setProfilePicture(context, it) },
                onNext = { viewModel.advanceSetupStep() },
                onBack = onBack
            )
            4 -> CustomizeStep(
                selections = customizeSelections,
                onApply = { selections ->
                    // TODO: persist `selections` (settingKey -> Boolean) via your
                    // ViewModel/DataStore of choice, e.g.:
                    //   selections.forEach { (key, enabled) -> viewModel.setFeatureFlag(key, enabled) }
                    // Left as a no-op here since Bluebird OS's preference storage
                    // for these specific flags isn't defined in this file.
                },
                onNext = { viewModel.advanceSetupStep() },
                onBack = onBack
            )
            5 -> LegalStep(
                onNext = { onSetupCompleted() },
                onBack = onBack
            )
            else -> LaunchedEffect(Unit) { onSetupCompleted() }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Phone Top Step Bar
// ─────────────────────────────────────────────────────────

@Composable
private fun TopStepBar(currentStep: Int, total: Int) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bluebird OS Setup",
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface
            )
            Text(
                "Step ${currentStep + 1} of $total",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        // Segmented progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(total) { index ->
                val fraction = when {
                    index < currentStep -> 1f
                    index == currentStep -> 1f
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= currentStep) colorScheme.primary
                            else colorScheme.outlineVariant
                        )
                )
            }
        }
    }
    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
}

// ─────────────────────────────────────────────────────────
// Tablet Left Sidebar
// ─────────────────────────────────────────────────────────

@Composable
private fun LeftSidebar(currentStep: Int, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colorScheme.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App logo mark
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    SetupFI.Home,
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Bluebird OS",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )
            Text(
                "Setup wizard",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(24.dp))

            Text(
                "STEPS",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stepMeta.forEachIndexed { index, meta ->
                    SidebarStepItem(
                        index = index,
                        meta = meta,
                        state = when {
                            index < currentStep -> StepState.DONE
                            index == currentStep -> StepState.ACTIVE
                            else -> StepState.UPCOMING
                        }
                    )
                }
            }
        }

        // Footer
        Column {
            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))
            Text(
                "Bluebird OS · v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                "Icons: Fluent UI System Icons © Microsoft",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SidebarStepItem(index: Int, meta: StepMeta, state: StepState) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = when (state) {
        StepState.DONE, StepState.ACTIVE -> colorScheme.primaryContainer.copy(alpha = if (state == StepState.ACTIVE) 1f else 0.5f)
        StepState.UPCOMING -> Color.Transparent
    }
    val iconTint = when (state) {
        StepState.DONE -> colorScheme.primary
        StepState.ACTIVE -> colorScheme.primary
        StepState.UPCOMING -> colorScheme.onSurfaceVariant
    }
    val textColor = when (state) {
        StepState.DONE, StepState.ACTIVE -> colorScheme.onSurface
        StepState.UPCOMING -> colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (state == StepState.UPCOMING) colorScheme.surfaceVariant
                    else colorScheme.primary.copy(alpha = 0.12f)
                )
                .border(
                    1.dp,
                    when (state) {
                        StepState.DONE, StepState.ACTIVE -> colorScheme.primary
                        StepState.UPCOMING -> colorScheme.outline
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state == StepState.DONE) {
                Icon(SetupFI.Check, null, tint = colorScheme.primary, modifier = Modifier.size(13.dp))
            } else {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(meta.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(meta.subtitle, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
        }

        if (state == StepState.ACTIVE) {
            Icon(SetupFI.ChevronRight, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step Container
// ─────────────────────────────────────────────────────────

@Composable
private fun StepContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }

        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

        content()
    }
}

// ─────────────────────────────────────────────────────────
// Step 0 — Welcome
// ─────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepContainer(
        title = "Welcome to Bluebird OS",
        subtitle = "A clean, modern home screen launcher. Let's take a moment to set everything up.",
        icon = SetupFI.Home
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val features = listOf(
            Triple(SetupFI.Dashboard, "Clean interface", "A minimal design that keeps your most-used apps front and centre."),
            Triple(SetupFI.Settings,  "Fully customisable", "Organise apps, widgets, and layouts exactly how you want them."),
            Triple(SetupFI.Security,  "Privacy-focused", "Your data stays on your device. No telemetry, no tracking."),
            Triple(SetupFI.Speed,     "Lightweight", "Fast and smooth — built to stay out of your way."),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { (icon, title, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(19.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = "Get started", icon = SetupFI.ArrowForward, onClick = onNext)
    }
}

// ─────────────────────────────────────────────────────────
// Step 1 — Permissions
// ─────────────────────────────────────────────────────────

@Composable
private fun PermissionsStep(
    permissionItems: List<PermissionItem>,
    grantedStates: Map<String, Boolean>,
    onRequestPermission: (PermissionItem) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    StepContainer(
        title = "Permissions",
        subtitle = "Grant access so Bluebird OS can work fully. Optional permissions can be skipped and changed any time in Android Settings.",
        icon = SetupFI.Lock
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            permissionItems.forEach { item ->
                val isGranted = if (item.isManageAllFiles && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    grantedStates[item.title] ?: false
                }

                val onGrant: () -> Unit = when {
                    item.title == "Notifications" && item.permissions.isEmpty() -> onRequestNotificationAccess
                    else -> { -> onRequestPermission(item) }
                }

                PermissionCard(
                    icon = item.icon,
                    title = item.title,
                    desc = item.description,
                    isGranted = isGranted,
                    isOptional = item.optional,
                    grantLabel = if (item.requiresSettingsIntent) "Open Settings" else "Grant",
                    onGrant = onGrant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Continue", SetupFI.ArrowForward, onClick = onNext)
            SecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    isOptional: Boolean,
    grantLabel: String = "Grant",
    onGrant: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                0.5.dp,
                if (isGranted) colorScheme.primary.copy(alpha = 0.4f) else colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isGranted) colorScheme.primary.copy(alpha = 0.12f)
                    else colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isGranted) colorScheme.primary else colorScheme.onPrimaryContainer,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                if (isOptional) {
                    Text(
                        "Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.surfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        if (isGranted) {
            Icon(
                SetupFI.CheckCircle,
                contentDescription = "$title granted",
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorScheme.primaryContainer)
                    .semantics { role = Role.Button; contentDescription = "Grant $title permission" }
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    grantLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step 2 — Username
// ─────────────────────────────────────────────────────────

@Composable
private fun UsernameStep(
    currentName: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    // Keep in sync if ViewModel updates externally
    LaunchedEffect(currentName) { if (name.isBlank()) name = currentName }
    var isFocused by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    StepContainer(
        title = "What's your name?",
        subtitle = "This appears on your lock screen and home screen greeting. You can change it later in Settings.",
        icon = SetupFI.Person
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Display name",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        if (isFocused) colorScheme.primary else colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        cursorColor = colorScheme.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    placeholder = {
                        Text("e.g. Mirembe Comfort", color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(SetupFI.Person, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            Icon(SetupFI.CheckCircle, contentDescription = "Name entered", tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }

            Text(
                "Can be changed later in Settings",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Continue",
                icon = SetupFI.ArrowForward,
                enabled = name.isNotBlank(),
                onClick = {
                    onNameChange(name.trim().ifBlank { "User" })
                    onNext()
                }
            )
            SecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step 3 — Avatar
// ─────────────────────────────────────────────────────────

@Composable
private fun AvatarStep(
    context: android.content.Context,
    onAvatarPicked: (Uri) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // Camera temp file URI
    val cameraImageUri = remember {
        val file = File(context.cacheDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            onAvatarPicked(it)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedUri = cameraImageUri
            onAvatarPicked(cameraImageUri)
        }
    }

    // Check if camera permission is granted
    val hasCameraPermission = remember(Unit) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    StepContainer(
        title = "Add a profile photo",
        subtitle = "This will appear on your home screen and lock screen. You can skip this and add one later.",
        icon = SetupFI.Image
    ) {
        // Avatar preview
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedUri == null) colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (selectedUri != null) colorScheme.primary else colorScheme.outlineVariant,
                        CircleShape
                    )
                    .semantics { role = Role.Button; contentDescription = "Tap to choose profile photo" }
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            SetupFI.AddAPhoto,
                            null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Add photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (selectedUri != null) {
            Text(
                "Photo selected",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Source buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gallery
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .semantics { role = Role.Button; contentDescription = "Choose photo from gallery" }
                    .clickable { galleryLauncher.launch("image/*") }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(SetupFI.PhotoLibrary, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurface)
                }
            }

            // Camera
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .semantics { role = Role.Button; contentDescription = "Take a photo with camera" }
                    .clickable {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(cameraImageUri)
                        } else {
                            showCameraPermissionDialog = true
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(SetupFI.PhotoCamera, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Camera", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = if (selectedUri != null) "Continue" else "Skip for now",
                icon = if (selectedUri != null) SetupFI.Check else SetupFI.SkipNext,
                onClick = onNext
            )
            SecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }

    // Camera permission dialog
    if (showCameraPermissionDialog) {
        val ctx = LocalContext.current
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            icon = { Icon(SetupFI.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Camera access needed") },
            text = { Text("Camera permission was not granted during setup. Please enable it in Android Settings to take a photo.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showCameraPermissionDialog = false
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                    })
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// Step 4 — Customize (Windows-11-OOBE-style feature toggles)
// ─────────────────────────────────────────────────────────

@Composable
private fun CustomizeStep(
    selections: SnapshotStateMap<String, Boolean>,
    onApply: (Map<String, Boolean>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val enabledCount = customizeTiles.count { selections[it.settingKey] == true }
    val allRecommendedOn = customizeTiles.all { selections[it.settingKey] == it.recommendedDefault }

    StepContainer(
        title = "Customize your setup",
        subtitle = "We've picked sensible defaults below. Turn anything off now, or leave it — every one of these can be changed later in Settings.",
        icon = SetupFI.Palette
    ) {
        // Quick "reset to recommended" affordance — mirrors the OOBE pattern
        // where the whole page has one clear default state to fall back to.
        if (!allRecommendedOn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .clickable { customizeTiles.forEach { selections[it.settingKey] = it.recommendedDefault } }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(SetupFI.CheckCircle, null, tint = colorScheme.primary, modifier = Modifier.size(15.dp))
                Text(
                    "Reset to recommended",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            customizeTiles.forEach { tile ->
                val isOn = selections[tile.settingKey] ?: tile.recommendedDefault
                CustomizeTileRow(
                    tile = tile,
                    isOn = isOn,
                    onToggle = { selections[tile.settingKey] = it }
                )
            }
        }

        Text(
            "$enabledCount of ${customizeTiles.size} enabled",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Continue",
                icon = SetupFI.ArrowForward,
                onClick = { onApply(selections.toMap()); onNext() }
            )
            SecondaryButton(
                text = "Skip for now",
                onClick = {
                    // Skipping applies nothing — every tile stays at whatever the
                    // app's normal first-run default is, untouched by this screen.
                    onNext()
                },
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CustomizeTileRow(
    tile: CustomizeTile,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                0.5.dp,
                if (isOn) colorScheme.primary.copy(alpha = 0.3f) else colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .semantics { role = Role.Switch; contentDescription = "${tile.title}, ${if (isOn) "on" else "off"}" }
            .clickable { onToggle(!isOn) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isOn) colorScheme.primary.copy(alpha = 0.12f) else colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tile.icon,
                null,
                tint = if (isOn) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                if (tile.recommendedDefault) {
                    Text(
                        "Recommended",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(tile.description, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
        Switch(
            checked = isOn,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.onPrimary,
                checkedTrackColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.outline,
                uncheckedTrackColor = colorScheme.surfaceVariant
            )
        )
    }
}

// ─────────────────────────────────────────────────────────
// Step 5 — Legal & Privacy
// ─────────────────────────────────────────────────────────

@Composable
private fun LegalStep(onNext: () -> Unit, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var agreedToPrivacy by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    StepContainer(
        title = "Legal information",
        subtitle = "Please review our policies before completing setup.",
        icon = SetupFI.Description
    ) {
        // Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Privacy Policy", "About", "Open Source").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (index == selectedTab) colorScheme.primary else Color.Transparent)
                        .semantics { role = Role.Tab; contentDescription = label }
                        .clickable { selectedTab = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (index == selectedTab) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Legal content panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
            ) {
                when (selectedTab) {
                    0 -> item { PrivacyPolicyContent() }
                    1 -> item { AboutContent() }
                    2 -> item { OpenSourceContent() }
                }
            }
        }

        // Consent checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (agreedToPrivacy) colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                .border(
                    0.5.dp,
                    if (agreedToPrivacy) colorScheme.primary.copy(alpha = 0.4f) else colorScheme.outlineVariant,
                    RoundedCornerShape(8.dp)
                )
                .clickable { agreedToPrivacy = !agreedToPrivacy }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = agreedToPrivacy,
                onCheckedChange = { agreedToPrivacy = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    checkmarkColor = colorScheme.onPrimary,
                    uncheckedColor = colorScheme.outline
                )
            )
            Text(
                "I have read and agree to the Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Complete setup",
                icon = SetupFI.Check,
                enabled = agreedToPrivacy,
                onClick = onNext
            )
            SecondaryButton("Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegalSection("Data collection", "Bluebird OS stores your preferences and profile information locally on your device only. No data is sent to external servers.")
        LegalSection("Permissions", "Permissions are used solely for the features you enable. You can revoke any permission at any time through Android Settings.")
        LegalSection("Third-party services", "We do not share your personal data with third parties.")
        LegalSection("Updates", "This privacy policy may be updated. You will be notified of significant changes within the app.")
    }
}

@Composable
private fun AboutContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegalSection("About Bluebird OS", "Bluebird OS is a modern Android launcher designed to provide a clean, fast, and customisable home screen experience.")
        LegalSection("Our values", "Privacy first — your data belongs to you. Clean, minimal interface. Fully customisable. Transparent about all practices.")
        LegalSection("Support", "For issues, feature requests, or feedback, visit our GitHub repository or use the feedback option in Settings.")
    }
}

@Composable
private fun OpenSourceContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegalSection("Open source licence", "Bluebird OS is released under the Apache License 2.0. You are free to use, modify, and distribute this software under its terms.")
        LegalSection("Source code", "The complete source code is available on GitHub. Contributions, bug reports, and pull requests are welcome.")
        LegalSection("Key dependencies", "Bluebird OS uses Jetpack Compose, Material 3, and Coil. We are grateful to those projects and their maintainers.")
        LegalSection("Iconography", "Icons throughout Bluebird OS are from Fluent UI System Icons, © Microsoft Corporation, made available under the MIT License.")
        LegalSection("Licence text", "Licensed under the Apache License, Version 2.0. A copy is available at http://www.apache.org/licenses/LICENSE-2.0")
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
        Text(content, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}

// ─────────────────────────────────────────────────────────
// Default Launcher Dialog
// ─────────────────────────────────────────────────────────

@Composable
private fun DefaultLauncherDialog(
    onSetDefault: () -> Unit,
    onSkip: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surface)
                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(28.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(SetupFI.Home, null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set as default launcher?",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "To use Bluebird OS as your home screen, set it as the default launcher. You can change this any time in Android Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Info callout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .border(0.5.dp, colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(SetupFI.Info, null, tint = colorScheme.primary, modifier = Modifier.size(15.dp).padding(top = 1.dp))
                    Text(
                        "You will be redirected to Android Settings to select Bluebird OS as your home app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryButton(
                        text = "Set as default",
                        icon = SetupFI.OpenInNew,
                        onClick = onSetDefault,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        text = "Maybe later",
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Button Components
// ─────────────────────────────────────────────────────────

@Composable
private fun PrimaryButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.12f))
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Icon(
                icon,
                null,
                tint = if (enabled) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, colorScheme.outline, RoundedCornerShape(10.dp))
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurfaceVariant
        )
    }
}
