package io.github.norbertweb.bluebird

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.norbertweb.bluebird.data.NotificationListener
import io.github.norbertweb.bluebird.ui.screens.DesktopScreen
import io.github.norbertweb.bluebird.ui.screens.SetupScreen
import io.github.norbertweb.bluebird.ui.theme.LocalTextScale
import io.github.norbertweb.bluebird.ui.theme.bluebirdTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled */ }

    // Context wrapper — handles Views and XML inflation outside Compose
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(DesktopModeHelper.buildDesktopContext(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        // NOTE: FLAG_KEEP_SCREEN_ON is intentionally NOT set here anymore.
        // It used to be added unconditionally on create AND re-added on every
        // brightness change below, with nothing ever clearing it — so the
        // screen could never time out and turn off, even when the launcher
        // was just sitting idle. The flag is now applied/cleared reactively
        // in setContent() based on uiState.activeMediaPlaybackIds, so it's
        // only held while a video is actually playing (browser or the
        // built-in Media Player), matching normal Android/desktop behavior.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        NotificationListener.onNotificationPosted = { notification ->
            viewModel.addNotification(notification)
        }
        NotificationListener.onNotificationRemoved = { key ->
            viewModel.dismissNotification(key)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            val systemDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(uiState.brightness) {
                window?.attributes = window?.attributes?.apply {
                    screenBrightness = uiState.brightness
                }
            }

            // Screen stays awake only while something is actually playing media —
            // e.g. a browser video or the built-in Media Player — not just because
            // the launcher app is open. Toggled reactively: FLAG_KEEP_SCREEN_ON is
            // added the moment any player id is added to activeMediaPlaybackIds,
            // and removed the moment the set goes empty, so normal screen-timeout
            // and lock behavior work correctly the rest of the time.
            LaunchedEffect(uiState.activeMediaPlaybackIds.isNotEmpty()) {
                if (uiState.activeMediaPlaybackIds.isNotEmpty()) {
                    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // ── THIS IS THE ACTUAL FIX ────────────────────────────────────────
            // DesktopDensityOverride overrides LocalDensity for the entire
            // Compose tree. Every dp value — padding, font size, component size —
            // is now measured against the io.github.norbertweb.io.github.norbertweb.bluebird density, not the phone's native
            // density. This is equivalent to changing "Smallest width" in Dev Options
            // but works entirely within the app, no root or ADB needed.
            // ─────────────────────────────────────────────────────────────────────
            DesktopDensityOverride(context = context) {
                CompositionLocalProvider(LocalTextScale provides uiState.textScale) {
                    // Always follows the Android system's own light/dark setting now —
                    // see Theme.kt. uiState.isDarkTheme is kept in LauncherUiState only
                    // for other screens that still read it as an approximation (e.g. to
                    // pick icon tint before this composable is reached), not as a
                    // user-facing override anymore.
                    bluebirdTheme(darkTheme = systemDarkTheme) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            if (!uiState.hasCompletedSetup) {
                                SetupScreen(
                                    viewModel = viewModel,
                                    onRequestNotificationAccess = {
                                        startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        )
                                    }
                                )
                            } else {
                                DesktopScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            BackHandler {
                val state = viewModel.uiState.value
                when {
                    state.isWidgetsOpen -> viewModel.toggleWidgets()
                    state.isStartMenuOpen || state.isActionCenterOpen ||
                            state.isSearchOpen || state.isPowerMenuOpen ||
                            state.isDesktopContextMenuOpen -> viewModel.dismissAllOverlays()
                    state.openWindows.isNotEmpty() -> state.activeWindowId?.let {
                        viewModel.closeWindow(it)
                    }
                    else -> viewModel.dismissAllOverlays()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Compose automatically recomposes and re-reads LocalDensity on config
        // change, so DesktopDensityOverride will recompute the target density
        // from the new DisplayMetrics automatically. Nothing extra needed.
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()

        window?.attributes = window?.attributes?.apply {
            screenBrightness = viewModel.uiState.value.brightness
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
