package com.music.bitchord.ui.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.music.bitchord.data.settings.AppIcon
import com.music.bitchord.data.settings.AppSettings

object AppIconManager {

    /**
     * Safely switches the app launcher icon to [targetIcon].
     *
     * In Android:
     * 1. The target alias is enabled FIRST so the launcher always sees at least one valid
     *    launcher entry point, preventing the app shortcut from disappearing from homescreens.
     * 2. All other icon aliases are then disabled.
     * 3. [PackageManager.DONT_KILL_APP] prevents the system from terminating the active process.
     * 4. Updates [AppSettings.appIcon].
     */
    fun setAppIcon(context: Context, targetIcon: AppIcon) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val packageName = appContext.packageName

        try {
            // 1. Enable the target alias FIRST
            val targetComponent = ComponentName(packageName, targetIcon.aliasName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            // 2. Disable all other aliases
            for (icon in AppIcon.entries) {
                if (icon != targetIcon) {
                    val otherComponent = ComponentName(packageName, icon.aliasName)
                    pm.setComponentEnabledSetting(
                        otherComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }

            // 3. Update reactive state
            AppSettings.setAppIcon(targetIcon)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Inspects which launcher alias is currently marked enabled in the system PackageManager.
     */
    fun getActiveAppIcon(context: Context): AppIcon {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val packageName = appContext.packageName

        for (icon in AppIcon.entries) {
            try {
                val state = pm.getComponentEnabledSetting(ComponentName(packageName, icon.aliasName))
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            } catch (_: Exception) {}
        }
        return AppSettings.appIcon.value
    }
}
