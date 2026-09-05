package io.github.norbertweb.bluebird.system

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Bluebird's bridge to Android global/system actions.
 *
 * Keep this service deliberately action-oriented: Bluebird asks for a concrete
 * user-triggered action, and the service performs only that action. We do not
 * use Home, Back, or Recents here.
 */
class BluebirdAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInstance = this

        // This service is an on-demand system-action bridge. It does not
        // inspect window contents and does not consume accessibility events.
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (serviceInstance === this) serviceInstance = null
        super.onDestroy()
    }

    private fun perform(action: Int): Boolean = performGlobalAction(action)

    fun lockScreen(): Boolean = perform(GLOBAL_ACTION_LOCK_SCREEN)

    fun screenshot(): Boolean = perform(GLOBAL_ACTION_TAKE_SCREENSHOT)

    fun openNotifications(): Boolean = perform(GLOBAL_ACTION_NOTIFICATIONS)

    fun openQuickSettings(): Boolean = perform(GLOBAL_ACTION_QUICK_SETTINGS)

    /**
     * Toggles Android's soft-keyboard show policy between hidden and automatic.
     * This is a policy toggle, not a fake visual state in Bluebird.
     */
    fun toggleTouchKeyboard(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val controller = softKeyboardController
        val current = controller.showMode
        val target = if (current == SHOW_MODE_HIDDEN) {
            SHOW_MODE_AUTO
        } else {
            SHOW_MODE_HIDDEN
        }
        return controller.setShowMode(target)
    }

    companion object {
        @Volatile
        private var serviceInstance: BluebirdAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val enabled = runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            }.getOrNull() ?: return false

            val expected = ComponentName(context, BluebirdAccessibilityService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun lockScreen(): Boolean = serviceInstance?.lockScreen() == true

        fun screenshot(): Boolean = serviceInstance?.screenshot() == true

        fun openNotifications(): Boolean = serviceInstance?.openNotifications() == true

        fun openQuickSettings(): Boolean = serviceInstance?.openQuickSettings() == true

        fun toggleTouchKeyboard(): Boolean = serviceInstance?.toggleTouchKeyboard() == true

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

}
