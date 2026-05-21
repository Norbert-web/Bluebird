package com.bluebird.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.bluebird.LauncherViewModel

// ─────────────────────────────────────────────────────────
// Professional Design System - Inspired by Microsoft Fluent
// ─────────────────────────────────────────────────────────
private object ProfessionalDS {
    // Modern, clean color palette
    val bgPrimary       = Color(0xFFFAFAFA)
    val bgSecondary     = Color(0xFFF3F3F3)
    val bgCard          = Color(0xFFFFFFFF)
    val bgInverse       = Color(0xFF1F1F1F)
    val bgInputField    = Color(0xFFF0F0F0)

    val accentPrimary   = Color(0xFF0078D4)   // Microsoft Blue
    val accentHover     = Color(0xFF106EBE)
    val accentPress     = Color(0xFF004B8D)
    val accentLight     = Color(0xFFEBF4F8)

    val successGreen    = Color(0xFF107C10)
    val warningAmber    = Color(0xFFFFB900)
    val errorRed        = Color(0xFFE81123)

    val borderSubtle    = Color(0xFFE1E1E1)
    val borderMid       = Color(0xFFCACACB)

    val textPrimary     = Color(0xFF242424)
    val textSecondary   = Color(0xFF616161)
    val textTertiary    = Color(0xFF757575)
    val textDisabled    = Color(0xFFA19F9D)

    val divider         = Color(0xFFE1E1E1)

    fun accentGradient() = Brush.linearGradient(
        colors = listOf(accentPrimary, Color(0xFF0063B1)),
        start = Offset(0f, 0f),
        end = Offset(400f, 200f)
    )

    fun shadowElevation(elevation: Dp) = when (elevation) {
        2.dp -> Color.Black.copy(alpha = 0.04f)
        4.dp -> Color.Black.copy(alpha = 0.06f)
        8.dp -> Color.Black.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
}

// ─────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────
private data class PermissionGroup(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>,
    val optional: Boolean = false
)

private fun areAllPermissionsGranted(
    context: android.content.Context,
    permissions: List<String>
): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(context, it) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

private data class StepMeta(val icon: ImageVector, val label: String, val subtitle: String)

private val stepMeta = listOf(
    StepMeta(Icons.Default.Home,            "Welcome",     "Get started"),
    StepMeta(Icons.Default.Lock,            "Permissions", "Access & Privacy"),
    StepMeta(Icons.Default.Person,          "Profile",     "Name & Identity"),
    StepMeta(Icons.Default.Image,           "Photo",       "Profile Picture"),
    StepMeta(Icons.Default.Description,     "Legal",       "Terms & Privacy"),
)

private enum class StepState { DONE, ACTIVE, UPCOMING }

// ─────────────────────────────────────────────────────────
// ROOT SETUP SCREEN
// ─────────────────────────────────────────────────────────
@Composable
fun SetupScreen(
    viewModel: LauncherViewModel,
    onRequestNotificationAccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.setupStep
    val context = LocalContext.current

    // Dialog states - Use NonCancellable to prevent dismissal
    var showDefaultLauncherDialog by remember { mutableStateOf(false) }
    var setupCompleted by remember { mutableStateOf(false) }

    val permissionGroups = remember {
        listOf(
            PermissionGroup(
                "Storage", "Access photos, documents, and files",
                Icons.Default.FolderOpen,
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                ),
                optional = false
            ),
            PermissionGroup(
                "Contacts & Calls", "Make calls and view your contacts",
                Icons.Default.Contacts,
                listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE),
                optional = true
            ),
            PermissionGroup(
                "Messages", "Read and send text messages",
                Icons.Default.Message,
                listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS),
                optional = true
            ),
            PermissionGroup(
                "Camera", "Capture profile photos and video",
                Icons.Default.PhotoCamera,
                listOf(Manifest.permission.CAMERA),
                optional = false
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

    // Main background
    Box(modifier = Modifier.fillMaxSize().background(ProfessionalDS.bgPrimary)) {

        // Subtle corner accents
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-80).dp, (-80).dp)
                .background(
                    ProfessionalDS.accentLight.copy(alpha = 0.4f),
                    RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .background(
                    ProfessionalDS.accentLight.copy(alpha = 0.3f),
                    RoundedCornerShape(50)
                )
        )

        // Main content
        if (setupCompleted) {
            // Show default launcher dialog instead of progressing
            DefaultLauncherDialog(
                onSetDefault = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                    showDefaultLauncherDialog = false
                    viewModel.completeSetup()
                },
                onSkip = {
                    showDefaultLauncherDialog = false
                    viewModel.completeSetup()
                }
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left sidebar
                LeftSidebar(currentStep = step, modifier = Modifier.width(280.dp).fillMaxHeight())

                // Vertical divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = ProfessionalDS.borderSubtle
                )

                // Right content
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    initialOffsetX = { it / 3 }
                                ) + fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(
                                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                                            targetOffsetX = { -it / 3 }
                                        ) + fadeOut(tween(300))
                            } else {
                                slideInHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    initialOffsetX = { -it / 3 }
                                ) + fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(
                                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                                            targetOffsetX = { it / 3 }
                                        ) + fadeOut(tween(300))
                            }
                        },
                        label = "setup_steps"
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
                                onNext = { viewModel.advanceSetupStep() }
                            )
                            4 -> LegalStep(
                                onNext = {
                                    setupCompleted = true
                                }
                            )
                            else -> {
                                LaunchedEffect(Unit) {
                                    setupCompleted = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom progress indicator
        if (!setupCompleted) {
            StepProgressIndicator(
                total = 5,
                current = step,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Left Sidebar Navigation
// ─────────────────────────────────────────────────────────
@Composable
private fun LeftSidebar(currentStep: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(ProfessionalDS.bgCard)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo and branding
        Column {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProfessionalDS.accentGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Bluebird OS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ProfessionalDS.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Setup Wizard",
                fontSize = 12.sp,
                color = ProfessionalDS.textSecondary,
                letterSpacing = 0.5.sp
            )
        }

        // Step navigation
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "SETUP STEPS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfessionalDS.textTertiary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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

        // Footer
        Column {
            Divider(color = ProfessionalDS.borderSubtle, modifier = Modifier.padding(bottom = 14.dp))
            Text(
                "Bluebird OS Setup",
                fontSize = 11.sp,
                color = ProfessionalDS.textTertiary
            )
            Text(
                "Version 1.0",
                fontSize = 10.sp,
                color = ProfessionalDS.textDisabled
            )
        }
    }
}

@Composable
private fun SidebarStepItem(index: Int, meta: StepMeta, state: StepState) {
    val bgColor = when (state) {
        StepState.DONE -> ProfessionalDS.accentLight
        StepState.ACTIVE -> ProfessionalDS.accentLight
        StepState.UPCOMING -> Color.Transparent
    }

    val iconTint = when (state) {
        StepState.DONE -> ProfessionalDS.successGreen
        StepState.ACTIVE -> ProfessionalDS.accentPrimary
        StepState.UPCOMING -> ProfessionalDS.textTertiary
    }

    val textColor = when (state) {
        StepState.DONE -> ProfessionalDS.textSecondary
        StepState.ACTIVE -> ProfessionalDS.textPrimary
        StepState.UPCOMING -> ProfessionalDS.textTertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        StepState.DONE -> ProfessionalDS.successGreen.copy(alpha = 0.12f)
                        StepState.ACTIVE -> ProfessionalDS.accentPrimary.copy(alpha = 0.12f)
                        StepState.UPCOMING -> ProfessionalDS.bgInputField
                    }
                )
                .border(
                    1.5.dp,
                    when (state) {
                        StepState.DONE -> ProfessionalDS.successGreen
                        StepState.ACTIVE -> ProfessionalDS.accentPrimary
                        StepState.UPCOMING -> ProfessionalDS.borderMid
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state == StepState.DONE) {
                Icon(Icons.Default.Check, null, tint = ProfessionalDS.successGreen, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                meta.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                meta.subtitle,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        if (state == StepState.ACTIVE) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = ProfessionalDS.accentPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Progress Indicator
// ─────────────────────────────────────────────────────────
@Composable
private fun StepProgressIndicator(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 4.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "progress_dot"
            )
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(width)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) ProfessionalDS.accentPrimary
                        else ProfessionalDS.borderMid
                    )
            )
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
    Column(
        modifier = Modifier
            .widthIn(max = 500.dp)
            .padding(horizontal = 40.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProfessionalDS.accentLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = ProfessionalDS.accentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ProfessionalDS.textPrimary
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = ProfessionalDS.textSecondary,
                lineHeight = 20.sp
            )
        }

        Divider(color = ProfessionalDS.borderSubtle)

        content()
    }
}

// ─────────────────────────────────────────────────────────
// STEP 0 — Welcome
// ─────────────────────────────────────────────────────────
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepContainer(
        title = "LAMN-NOBERT Welcomes you to Bluebird OS",
        subtitle = "Your modern, intuitive mobile launcher experience,a way forward to a mini OS environmont for you to play with and learn more!. Let's set things up.",
        icon = Icons.Default.Home
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val features = listOf(
                Triple(Icons.Default.Dashboard, "Clean Interface", "Beautiful, minimal design inspired by modern systems"),
                Triple(Icons.Default.Settings, "Customizable", "Organize your apps and widgets exactly how you want and DON'T FORGET, BLUEBIRD IS OPEN SOURCE,CUSTOMIZE TO YOUR FEEL AND CONTRIBUTE MORE!"),
                Triple(Icons.Default.Security, "Privacy-Focused", "Your data stays on your device"),
                Triple(Icons.Default.Speed, "Lightweight", "Fast, smooth, and responsive performance,optimized right just for your phone,call it a mini PC now!"),
            )

            features.forEach { (icon, title, desc) ->
                FeatureCard(icon = icon, title = title, desc = desc)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(text = "Get Started", icon = Icons.Default.ArrowForward, onClick = onNext)
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ProfessionalDS.bgSecondary)
            .border(1.dp, ProfessionalDS.borderSubtle, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ProfessionalDS.accentLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = ProfessionalDS.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfessionalDS.textPrimary
            )
            Text(
                desc,
                fontSize = 12.sp,
                color = ProfessionalDS.textSecondary,
                lineHeight = 16.sp
            )
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
    StepContainer(
        title = "Permissions",
        subtitle = "We need a few permissions to provide the full experience. Optional permissions can be skipped,but note that some features cause app crashes if their specific permissions are not granted,so please if you encounter crashes,check that all permissions are granted or the feature your using meets that condition.",
        icon = Icons.Default.Lock
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            permissionGroups.forEach { group ->
                PermissionCard(
                    icon = group.icon,
                    title = group.title,
                    desc = group.description,
                    isGranted = grantedStates[group.title] ?: false,
                    isOptional = group.optional,
                    onGrant = { onRequestPermission(group) }
                )
            }
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                desc = "Show timely notifications and alerts",
                isGranted = false,
                isOptional = true,
                onGrant = onRequestNotificationAccess,
                grantLabel = "Open Settings"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Continue", Icons.Default.ArrowForward, onClick = onNext)
            SecondaryButton("Skip", onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    isOptional: Boolean = false,
    onGrant: () -> Unit,
    grantLabel: String = "Grant"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ProfessionalDS.bgSecondary)
            .border(
                1.dp,
                if (isGranted) ProfessionalDS.successGreen.copy(alpha = 0.3f) else ProfessionalDS.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isGranted) ProfessionalDS.successGreen.copy(alpha = 0.12f)
                    else ProfessionalDS.accentLight
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isGranted) ProfessionalDS.successGreen else ProfessionalDS.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfessionalDS.textPrimary
                )
                if (isOptional) {
                    Text(
                        "Optional",
                        fontSize = 10.sp,
                        color = ProfessionalDS.textTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ProfessionalDS.bgInputField)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(desc, fontSize = 12.sp, color = ProfessionalDS.textSecondary)
        }
        if (isGranted) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = ProfessionalDS.successGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ProfessionalDS.accentLight)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    grantLabel,
                    fontSize = 12.sp,
                    color = ProfessionalDS.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
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

    StepContainer(
        title = "What's your name?",
        subtitle = "This will be displayed on your lock screen and Start Menu.",
        icon = Icons.Default.Person
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Display Name",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfessionalDS.textPrimary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProfessionalDS.bgInputField)
                    .border(
                        1.5.dp,
                        if (isFocused) ProfessionalDS.accentPrimary else ProfessionalDS.borderSubtle,
                        RoundedCornerShape(8.dp)
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
                        focusedTextColor = ProfessionalDS.textPrimary,
                        unfocusedTextColor = ProfessionalDS.textPrimary,
                        cursorColor = ProfessionalDS.accentPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    placeholder = {
                        Text(
                            "e.g. Mirembe Comfort",
                            color = ProfessionalDS.textTertiary,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = ProfessionalDS.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = ProfessionalDS.successGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            Text(
                "Can be changed later in settings",
                fontSize = 12.sp,
                color = ProfessionalDS.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
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
        uri?.let {
            selectedUri = it
            onAvatarPicked(it.toString())
        }
    }

    StepContainer(
        title = "Add a profile photo",
        subtitle = "This will appear on your Start Menu and lock screen.",
        icon = Icons.Default.Image
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedUri == null) ProfessionalDS.accentLight else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (selectedUri != null) ProfessionalDS.accentPrimary else ProfessionalDS.borderMid,
                        CircleShape
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            tint = ProfessionalDS.accentPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Add Photo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfessionalDS.textSecondary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (selectedUri != null) "Photo selected!" else "No photo selected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedUri != null) ProfessionalDS.successGreen else ProfessionalDS.textPrimary
                )
                Text(
                    "Tap the circle or use the button to choose a photo from your device.",
                    fontSize = 12.sp,
                    color = ProfessionalDS.textSecondary,
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProfessionalDS.bgSecondary)
                        .border(1.dp, ProfessionalDS.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { imagePicker.launch("image/*") }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            null,
                            tint = ProfessionalDS.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Choose from Gallery",
                            fontSize = 12.sp,
                            color = ProfessionalDS.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = if (selectedUri != null) "All Set!" else "Skip",
            icon = if (selectedUri != null) Icons.Default.Check else Icons.Default.SkipNext,
            onClick = onNext
        )
    }
}

// ─────────────────────────────────────────────────────────
// STEP 4 — Legal & Privacy
// ─────────────────────────────────────────────────────────
@Composable
private fun LegalStep(onNext: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    StepContainer(
        title = "Legal Information",
        subtitle = "Please review our policies before completing setup.",
        icon = Icons.Default.Description
    ) {
        // Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ProfessionalDS.bgSecondary)
                .border(1.dp, ProfessionalDS.borderSubtle, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Privacy Policy", "About", "Open Source") .forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (index == selectedTab) ProfessionalDS.accentPrimary
                            else Color.Transparent
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (index == selectedTab) Color.White else ProfessionalDS.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ProfessionalDS.bgSecondary)
                .border(1.dp, ProfessionalDS.borderSubtle, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .heightIn(max = 300.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> item { PrivacyPolicyContent() }
                    1 -> item { AboutContent() }
                    2 -> item { OpenSourceContent() }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Complete Setup",
            icon = Icons.Default.Check,
            onClick = onNext
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LegalSection(
            "Data Collection",
            "Bluebird OS collects minimal data. We only store your preferences, profile information, and usage statistics locally on your device. No data is sent to external servers without your explicit consent."
        )
        LegalSection(
            "Permissions",
            "When you grant permissions, they are used only for the specific features you enable. For example, storage permission is only used to access your files when needed. You can revoke permissions at any time through Android settings."
        )
        LegalSection(
            "Third-party Services",
            "We do not share your personal data with third parties."
        )
        LegalSection(
            "Updates",
            "This privacy policy may be updated. We will notify you of significant changes within the app."
        )
    }
}

@Composable
private fun AboutContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LegalSection(
            "About Bluebird OS",
            "Bluebird OS is a modern mobile launcher designed to provide a clean, intuitive, and performant experience. Our mission is to give users full control over their device interface."
        )
        LegalSection(
            "Our Values",
            "Privacy First: Your data belongs to you.\nMinimal: Clean, uncluttered interface.\nCustomizable: Adapt to your workflow.\nOpen: Transparent about our practices."
        )
        LegalSection(
            "Support",
            "For issues, feature requests, or feedback, please visit our GitHub repository or contact us through the app settings."
        )
        LegalSection(
            "Credits",
            "Bluebird OS is maintained by the community. Special thanks to all contributors and users who help make this project better.Whereas the community is just basically me Lamn Nobert and my fami... I hope for more to join and support this project. ~trebronwayne@gmail.com/ +256790014428"
        )
    }
}

@Composable
private fun OpenSourceContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LegalSection(
            "Open Source License",
            "Bluebird OS is released under the Apache License 2.0. You are free to use, modify, and distribute this software under the terms of this license."
        )
        LegalSection(
            "GitHub Repository",
            "The complete source code is available on GitHub. We welcome contributions from the community. Visit our repository to view the code, report issues, or submit pull requests."
        )
        LegalSection(
            "Contributing",
            "We encourage contributions! Whether it's bug reports, feature requests, or code contributions, your input helps make Bluebird OS better for everyone."
        )
        LegalSection(
            "Dependencies",
            "Bluebird OS uses several open-source libraries including Jetpack Compose, Material 3, and Coil. We are grateful to these projects and their maintainers."
        )
        LegalSection(
            "License Text",
            "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0"
        )
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfessionalDS.textPrimary
        )
        Text(
            content,
            fontSize = 11.sp,
            color = ProfessionalDS.textSecondary,
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────
// Default Launcher Dialog (Non-dismissible)
// ─────────────────────────────────────────────────────────
@Composable
private fun DefaultLauncherDialog(
    onSetDefault: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProfessionalDS.bgCard)
                .border(1.dp, ProfessionalDS.borderSubtle, RoundedCornerShape(12.dp))
                .padding(32.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProfessionalDS.accentGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Content
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set as Default Launcher?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfessionalDS.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "To fully experience Bluebird OS, set it as your default home screen. You can change this anytime in Android settings.",
                        fontSize = 13.sp,
                        color = ProfessionalDS.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Info box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProfessionalDS.accentLight)
                        .border(1.dp, ProfessionalDS.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = ProfessionalDS.accentPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        "You'll be redirected to Android settings where you can select Bluebird OS as your default launcher.",
                        fontSize = 11.sp,
                        color = ProfessionalDS.textSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = "Set as Default",
                        icon = Icons.Default.OpenInNew,
                        onClick = onSetDefault,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ProfessionalDS.borderMid, RoundedCornerShape(8.dp))
                            .clickable(onClick = onSkip)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Maybe Later",
                            fontSize = 13.sp,
                            color = ProfessionalDS.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                 brush = if (enabled) {ProfessionalDS.accentGradient()}
                else {
                    Brush.linearGradient(
                        listOf(
                        ProfessionalDS.bgInputField,
                        ProfessionalDS.bgInputField
                    )
                     )
                 }
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else ProfessionalDS.textDisabled
            )
            Icon(
                icon,
                null,
                tint = if (enabled) Color.White else ProfessionalDS.textDisabled,
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
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ProfessionalDS.borderMid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfessionalDS.textSecondary
        )
    }
}
