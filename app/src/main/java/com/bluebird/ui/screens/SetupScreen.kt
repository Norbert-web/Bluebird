package com.bluebird.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.bluebird.LauncherViewModel

// ─────────────────────────────────────────────────────────
// Design System
// ─────────────────────────────────────────────────────────
private object EdgeDS {
    // Core palette — Edge-inspired deep navy + electric blue
    val bgDeep       = Color(0xFF0A0E1A)
    val bgPanel      = Color(0xFF0F1420)
    val bgCard       = Color(0xFF141928)
    val bgElevated   = Color(0xFF1A2035)
    val bgInput      = Color(0xFF111827)

    val accentBlue   = Color(0xFF0078D4)   // Edge signature blue
    val accentCyan   = Color(0xFF00B4D8)
    val accentGlow   = Color(0xFF4FC3F7)

    val borderSubtle = Color(0x18FFFFFF)
    val borderMid    = Color(0x28FFFFFF)
    val borderAccent = Color(0x60FFFFFF)

    val textPrimary  = Color(0xFFE8EAF0)
    val textSecond   = Color(0xFF8892A4)
    val textHint     = Color(0xFF4D5668)

    val successGreen = Color(0xFF22C55E)
    val warningAmber = Color(0xFFF59E0B)

    fun accentGradient() = Brush.linearGradient(
        colors = listOf(accentBlue, accentCyan),
        start = Offset(0f, 0f), end = Offset(400f, 200f)
    )

    fun panelGradient() = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1829), Color(0xFF080D18))
    )

    fun glowBrush() = Brush.radialGradient(
        colors = listOf(accentBlue.copy(alpha = 0.25f), Color.Transparent),
        radius = 500f
    )
}

// ─────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────
private data class PermissionGroup(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>
)

private fun areAllPermissionsGranted(
    context: android.content.Context,
    permissions: List<String>
): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(context, it) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

// Step metadata
private data class StepMeta(val icon: ImageVector, val label: String, val subtitle: String)

private val stepMeta = listOf(
    StepMeta(Icons.Default.Carpenter,    "Welcome",     "Let's get started"),
    StepMeta(Icons.Default.Security,       "Permissions", "Access & Privacy"),
    StepMeta(Icons.Default.Person,         "Your Profile","Name & Identity"),
    StepMeta(Icons.Default.PhotoCamera,    "Your Photo",  "Profile Picture"),
)

// ─────────────────────────────────────────────────────────
// ROOT SETUP SCREEN — Landscape split-panel layout
// ─────────────────────────────────────────────────────────
@Composable
fun SetupScreen(
    viewModel: LauncherViewModel,
    onRequestNotificationAccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.setupStep
    val context = LocalContext.current

    // Show "Set as default launcher" dialog after setup completes
    var showDefaultLauncherPrompt by remember { mutableStateOf(false) }

    val permissionGroups = remember {
        listOf(
            PermissionGroup(
                "Storage", "Browse and manage your files",
                Icons.Default.FolderOpen,
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            ),
            PermissionGroup(
                "Phone & Contacts", "Make calls and view contacts",
                Icons.Default.Phone,
                listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
            ),
            PermissionGroup(
                "SMS", "Read and send messages",
                Icons.Default.Message,
                listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
            ),
            PermissionGroup(
                "Camera", "Take profile photos",
                Icons.Default.PhotoCamera,
                listOf(Manifest.permission.CAMERA)
            )
        )
    }

    val grantedStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionGroups.forEach { group ->
                put(group.title, areAllPermissionsGranted(context, group.permissions))
            }
        }
    }

    var requestedGroup by remember { mutableStateOf<PermissionGroup?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        requestedGroup?.let { group ->
            grantedStates[group.title] = group.permissions.all { results[it] == true }
        }
        requestedGroup = null
    }

    // Full-screen background
    Box(modifier = Modifier.fillMaxSize().background(EdgeDS.bgDeep)) {

        // Ambient glow top-left
        Box(
            modifier = Modifier
                .size(600.dp)
                .offset((-150).dp, (-150).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EdgeDS.accentBlue.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        // Ambient glow bottom-right
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(100.dp, 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EdgeDS.accentCyan.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        // ── Landscape split: Left branding panel + Right content ──
        Row(modifier = Modifier.fillMaxSize()) {

            // ── LEFT PANEL (branding + step nav) ──
            LeftBrandPanel(currentStep = step, modifier = Modifier.width(280.dp).fillMaxHeight())

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                EdgeDS.borderMid,
                                EdgeDS.borderSubtle,
                                Color.Transparent
                            )
                        )
                    )
            )

            // ── RIGHT PANEL (step content) ──
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it / 2 } + fadeIn(tween(320)) togetherWith
                                    slideOutHorizontally { -it / 2 } + fadeOut(tween(200))
                        } else {
                            slideInHorizontally { -it / 2 } + fadeIn(tween(320)) togetherWith
                                    slideOutHorizontally { it / 2 } + fadeOut(tween(200))
                        }
                    },
                    label = "step_content"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep(onNext = { viewModel.advanceSetupStep() })
                        1 -> PermissionsStep(
                            permissionGroups = permissionGroups,
                            grantedStates = grantedStates,
                            onRequestPermission = { group ->
                                requestedGroup = group
                                permLauncher.launch(group.permissions.toTypedArray())
                            },
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onNext = { viewModel.advanceSetupStep() }
                        )
                        2 -> UsernameStep(
                            currentName = uiState.userProfile.userName,
                            onNameChange = { viewModel.setUserName(it) },
                            onNext = { viewModel.advanceSetupStep() }
                        )
                        3 -> AvatarStep(
                            onAvatarPicked = { viewModel.setProfilePicture(context, Uri.parse(it)) },
                            onNext = {
                                viewModel.advanceSetupStep()
                                showDefaultLauncherPrompt = true
                            }
                        )
                        else -> {
                            LaunchedEffect(Unit) { viewModel.completeSetup() }
                        }
                    }
                }
            }
        }

        // ── Bottom step dots ──
        StepDots(
            total = 4,
            current = step,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
        )
    }

    // ── Default Launcher Dialog ──
    if (showDefaultLauncherPrompt) {
        DefaultLauncherDialog(
            onSetDefault = {
                showDefaultLauncherPrompt = false
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                context.startActivity(intent)
                viewModel.completeSetup()
            },
            onSkip = {
                showDefaultLauncherPrompt = false
                viewModel.completeSetup()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// Left Branding Panel
// ─────────────────────────────────────────────────────────
@Composable
private fun LeftBrandPanel(currentStep: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(EdgeDS.panelGradient())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo + brand
        Column {
            // Edge-style logo mark
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EdgeDS.accentGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Window,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Win11\nLauncher",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = EdgeDS.textPrimary,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Setup Assistant",
                fontSize = 12.sp,
                color = EdgeDS.textSecond,
                letterSpacing = 0.5.sp
            )
        }

        // Step navigator
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "SETUP STEPS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EdgeDS.textHint,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            stepMeta.forEachIndexed { index, meta ->
                StepNavItem(
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

        // Footer
        Column {
            HorizontalDivider(color = EdgeDS.borderSubtle, modifier = Modifier.padding(bottom = 14.dp))
            Text(
                "Win11 Launcher",
                fontSize = 11.sp,
                color = EdgeDS.textHint
            )
            Text(
                "v2.0 • Landscape Edition",
                fontSize = 10.sp,
                color = EdgeDS.textHint.copy(alpha = 0.6f)
            )
        }
    }
}

private enum class StepState { DONE, ACTIVE, UPCOMING }

@Composable
private fun StepNavItem(index: Int, meta: StepMeta, state: StepState) {
    val bgAlpha by animateFloatAsState(if (state == StepState.ACTIVE) 1f else 0f, label = "stepBg")
    val iconTint = when (state) {
        StepState.DONE     -> EdgeDS.successGreen
        StepState.ACTIVE   -> Color.White
        StepState.UPCOMING -> EdgeDS.textHint
    }
    val textColor = when (state) {
        StepState.DONE     -> EdgeDS.textSecond
        StepState.ACTIVE   -> EdgeDS.textPrimary
        StepState.UPCOMING -> EdgeDS.textHint
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (state == StepState.ACTIVE)
                    Brush.horizontalGradient(listOf(EdgeDS.accentBlue.copy(alpha = 0.2f), Color.Transparent))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    when (state) {
                        StepState.DONE   -> SolidColor(EdgeDS.successGreen.copy(alpha = 0.15f))
                        StepState.ACTIVE -> EdgeDS.accentGradient()
                        StepState.UPCOMING -> Brush.linearGradient(
                            listOf(EdgeDS.bgElevated, EdgeDS.bgElevated)
                        )
                    },
                    CircleShape
                )
                .border(
                    1.dp,
                    when (state) {
                        StepState.DONE -> EdgeDS.successGreen.copy(alpha = 0.4f)
                        StepState.ACTIVE -> Color.Transparent
                        StepState.UPCOMING -> EdgeDS.borderSubtle
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state == StepState.DONE) {
                Icon(Icons.Default.Check, null, tint = EdgeDS.successGreen, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }

        Column {
            Text(meta.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(meta.subtitle, fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
        }

        if (state == StepState.ACTIVE) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(EdgeDS.accentBlue, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step Dots (bottom indicator)
// ─────────────────────────────────────────────────────────
@Composable
private fun StepDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val width by animateDpAsState(if (isActive) 22.dp else 6.dp, spring(0.6f, 500f), label = "dot")
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) EdgeDS.accentGradient()
                        else Brush.linearGradient(listOf(EdgeDS.borderMid, EdgeDS.borderMid))
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step content wrapper
// ─────────────────────────────────────────────────────────
@Composable
private fun StepPane(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(EdgeDS.accentGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EdgeDS.textPrimary)
            Text(subtitle, fontSize = 13.sp, color = EdgeDS.textSecond)
        }

        HorizontalDivider(color = EdgeDS.borderSubtle)

        content()
    }
}

// ─────────────────────────────────────────────────────────
// STEP 0 — Welcome
// ─────────────────────────────────────────────────────────
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepPane(
        title = "Welcome to Win11 Launcher",
        subtitle = "The Windows 11 experience, on Android.",
        icon = Icons.Default.Laptop
    ) {
        // Feature chips
        val features = listOf(
            Triple(Icons.Default.Dashboard, "Start Menu", "Full Win11 Start Menu with pinned apps"),
            Triple(Icons.Default.ViewModule, "Task Manager", "Monitor and manage running apps"),
            Triple(Icons.Default.Notifications, "Notification Center", "Action center & quick settings"),
            Triple(Icons.Default.Search, "Search", "System-wide search across apps & files"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            features.forEach { (icon, title, desc) ->
                FeatureRow(icon = icon, title = title, desc = desc)
            }
        }

        Spacer(Modifier.height(4.dp))
        EdgeButton(text = "Get Started", icon = Icons.Default.ArrowForward, onClick = onNext)
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EdgeDS.bgElevated)
            .border(1.dp, EdgeDS.borderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(EdgeDS.accentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EdgeDS.accentCyan, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EdgeDS.textPrimary)
            Text(desc, fontSize = 11.sp, color = EdgeDS.textSecond)
        }
    }
}

// ─────────────────────────────────────────────────────────
// STEP 1 — Permissions
// ─────────────────────────────────────────────────────────
@Composable
private fun PermissionsStep(
    permissionGroups: List<PermissionGroup>,
    grantedStates: Map<String, Boolean>,
    onRequestPermission: (PermissionGroup) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onNext: () -> Unit
) {
    StepPane(
        title = "Permissions",
        subtitle = "Required for full functionality. You can change these later.",
        icon = Icons.Default.Security
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            permissionGroups.forEach { group ->
                EdgePermissionRow(
                    icon = group.icon,
                    title = group.title,
                    desc = group.description,
                    isGranted = grantedStates[group.title] ?: false,
                    onGrant = { onRequestPermission(group) }
                )
            }
            EdgePermissionRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                desc = "Show live system notifications",
                isGranted = false,
                onGrant = onRequestNotificationAccess,
                grantLabel = "Open Settings"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EdgeButton("Continue", Icons.Default.ArrowForward, onClick = onNext)
            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = EdgeDS.textSecond, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EdgePermissionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    grantLabel: String = "Grant"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EdgeDS.bgElevated)
            .border(
                1.dp,
                if (isGranted) EdgeDS.successGreen.copy(alpha = 0.3f) else EdgeDS.borderSubtle,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (isGranted) EdgeDS.successGreen.copy(alpha = 0.12f)
                    else EdgeDS.accentBlue.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (isGranted) EdgeDS.successGreen else EdgeDS.accentCyan,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EdgeDS.textPrimary)
            Text(desc, fontSize = 10.sp, color = EdgeDS.textSecond)
        }
        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EdgeDS.successGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = EdgeDS.successGreen, modifier = Modifier.size(14.dp))
                Text("Granted", fontSize = 11.sp, color = EdgeDS.successGreen, fontWeight = FontWeight.Medium)
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(EdgeDS.accentBlue.copy(alpha = 0.15f))
                    .border(1.dp, EdgeDS.accentBlue.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(grantLabel, fontSize = 11.sp, color = EdgeDS.accentGlow, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// STEP 2 — Username
// ─────────────────────────────────────────────────────────
@Composable
private fun UsernameStep(
    currentName: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var isFocused by remember { mutableStateOf(false) }

    StepPane(
        title = "What's your name?",
        subtitle = "Shown on the Start Menu, lock screen, and user profile.",
        icon = Icons.Default.Person
    ) {
        // Name input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Display Name", fontSize = 12.sp, color = EdgeDS.textSecond, fontWeight = FontWeight.Medium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EdgeDS.bgInput)
                    .border(
                        1.dp,
                        if (isFocused) EdgeDS.accentBlue else EdgeDS.borderMid,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = EdgeDS.textPrimary,
                        unfocusedTextColor = EdgeDS.textPrimary,
                        focusedLabelColor = EdgeDS.accentCyan,
                        unfocusedLabelColor = EdgeDS.textSecond,
                        cursorColor = EdgeDS.accentBlue,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    placeholder = { Text("e.g. Alex Johnson", color = EdgeDS.textHint, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, null, tint = EdgeDS.accentCyan, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint = EdgeDS.successGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            Text(
                "This can be changed later in Settings.",
                fontSize = 11.sp,
                color = EdgeDS.textHint
            )
        }

        Spacer(Modifier.height(4.dp))

        EdgeButton(
            text = "Continue",
            icon = Icons.Default.ArrowForward,
            enabled = name.isNotBlank(),
            onClick = {
                onNameChange(name.ifBlank { "User" })
                onNext()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// STEP 3 — Avatar
// ─────────────────────────────────────────────────────────
@Composable
private fun AvatarStep(
    onAvatarPicked: (String) -> Unit,
    onNext: () -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri = it; onAvatarPicked(it.toString()) }
    }

    StepPane(
        title = "Add a profile photo",
        subtitle = "Personalize your Start Menu and lock screen.",
        icon = Icons.Default.AccountCircle
    ) {
        // Avatar preview + picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedUri == null) EdgeDS.accentBlue.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (selectedUri != null) EdgeDS.accentGradient()
                        else Brush.linearGradient(listOf(EdgeDS.borderMid, EdgeDS.borderMid)),
                        CircleShape
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto, null,
                            tint = EdgeDS.accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Add photo", fontSize = 10.sp, color = EdgeDS.textSecond)
                    }
                }
            }

            // Right side actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (selectedUri != null) "Photo selected!" else "No photo selected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedUri != null) EdgeDS.successGreen else EdgeDS.textPrimary
                )
                Text(
                    "Tap the circle or use the button below to choose from your gallery.",
                    fontSize = 12.sp,
                    color = EdgeDS.textSecond
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EdgeDS.bgElevated)
                        .border(1.dp, EdgeDS.borderMid, RoundedCornerShape(8.dp))
                        .clickable { imagePicker.launch("image/*") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Image, null, tint = EdgeDS.accentCyan, modifier = Modifier.size(16.dp))
                        Text("Choose from Gallery", fontSize = 12.sp, color = EdgeDS.textPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        EdgeButton(
            text = if (selectedUri != null) "All Done!" else "Skip",
            icon = if (selectedUri != null) Icons.Default.Check else Icons.Default.SkipNext,
            onClick = onNext
        )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(EdgeDS.bgCard)
                .border(1.dp, EdgeDS.borderMid, RoundedCornerShape(18.dp))
                .padding(28.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(EdgeDS.accentGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Text(
                    "Set as Default Launcher?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeDS.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "To use Win11 Launcher as your home screen, set it as your default launcher in Android settings.",
                    fontSize = 13.sp,
                    color = EdgeDS.textSecond,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Info row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EdgeDS.accentBlue.copy(alpha = 0.1f))
                        .border(1.dp, EdgeDS.accentBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = EdgeDS.accentCyan, modifier = Modifier.size(16.dp))
                    Text(
                        "You'll be taken to Android's Home App settings. Select \"Win11 Launcher\" from the list.",
                        fontSize = 11.sp,
                        color = EdgeDS.textSecond,
                        lineHeight = 16.sp
                    )
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EdgeButton(
                        text = "Set as Default",
                        icon = Icons.Default.OpenInNew,
                        onClick = onSetDefault
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, EdgeDS.borderMid, RoundedCornerShape(10.dp))
                            .clickable(onClick = onSkip)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Maybe Later",
                            fontSize = 14.sp,
                            color = EdgeDS.textSecond,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Shared: Edge-style primary button
// ─────────────────────────────────────────────────────────
@Composable
private fun EdgeButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) EdgeDS.accentGradient()
                else Brush.linearGradient(listOf(EdgeDS.bgElevated, EdgeDS.bgElevated))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else EdgeDS.textHint
            )
            Icon(icon, null, tint = if (enabled) Color.White else EdgeDS.textHint, modifier = Modifier.size(16.dp))
        }
    }
}
