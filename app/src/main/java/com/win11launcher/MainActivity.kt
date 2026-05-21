package com.win11launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.win11launcher.data.NotificationListener
import com.win11launcher.ui.screens.DesktopScreen
import com.win11launcher.ui.screens.SetupScreen
import com.win11launcher.ui.theme.Win11Theme

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        NotificationListener.onNotificationPosted = { notification ->
            viewModel.addNotification(notification)
        }
        NotificationListener.onNotificationRemoved = { key ->
            viewModel.dismissNotification(key)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(uiState.brightness) {
                window?.attributes = window?.attributes?.apply {
                    screenBrightness = uiState.brightness
                }
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            // ── THIS IS THE ACTUAL FIX ────────────────────────────────────────
            // DesktopDensityOverride overrides LocalDensity for the entire
            // Compose tree. Every dp value — padding, font size, component size —
            // is now measured against the desktop density, not the phone's native
            // density. This is equivalent to changing "Smallest width" in Dev Options
            // but works entirely within the app, no root or ADB needed.
            // ─────────────────────────────────────────────────────────────────────
            DesktopDensityOverride(context = context) {
                Win11Theme(darkTheme = uiState.isDarkTheme) {
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

        NotificationListener.onNotificationPosted = { notification ->
            viewModel.addNotification(notification)
        }
        NotificationListener.onNotificationRemoved = { key ->
            viewModel.dismissNotification(key)
        }
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
