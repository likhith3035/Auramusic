package com.music.bitchord.ui.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.music.bitchord.data.settings.AppIcon
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppIconManager {

    @Volatile
    private var pendingDisableOthers: Boolean = false
    @Volatile
    private var runningActivities: Int = 0

    /**
     * Initializes the lifecycle observer on application startup so inactive aliases
     * are safely disabled only when the app is backgrounded (preventing Android OS
     * ActivityManager from abruptly finishing the foreground task).
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                runningActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                runningActivities--
                if (runningActivities <= 0) {
                    runningActivities = 0
                    if (pendingDisableOthers) {
                        applyPendingIconDisables(activity.applicationContext)
                    }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // On cold start, ensure only the user-selected icon is enabled
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cleanLingeringAliases(application)
            } catch (_: Exception) {}
        }
    }

    /**
     * Switches the app launcher icon to [targetIcon] safely:
     * 1. Enables the target alias immediately so the launcher recognizes it.
     * 2. Defers disabling the previously active alias until the app transitions
     *    to the background (onActivityStopped), preventing Android OS from killing
     *    the foreground activity while the user is inside Settings.
     * 3. Updates [AppSettings.appIcon] immediately for responsive UI feedback.
     */
    fun setAppIcon(context: Context, targetIcon: AppIcon) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val packageName = appContext.packageName

        try {
            // 1. Enable the target alias immediately
            val targetComponent = ComponentName(packageName, targetIcon.aliasName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            // 2. Mark pending disable so old aliases are disabled safely when backgrounded
            pendingDisableOthers = true

            // 3. If no activity is currently in foreground, apply immediately
            if (runningActivities <= 0) {
                applyPendingIconDisables(appContext)
            }

            // 4. Update reactive state
            AppSettings.setAppIcon(targetIcon)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Disables all non-selected aliases while the app is in the background.
     */
    fun applyPendingIconDisables(context: Context) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val packageName = appContext.packageName
        val currentIcon = AppSettings.appIcon.value

        try {
            for (icon in AppIcon.entries) {
                if (icon != currentIcon) {
                    val otherComponent = ComponentName(packageName, icon.aliasName)
                    val state = pm.getComponentEnabledSetting(otherComponent)
                    if (state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            otherComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP,
                        )
                    }
                }
            }
            pendingDisableOthers = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reconciles all aliases on cold start.
     */
    private fun cleanLingeringAliases(context: Context) {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val packageName = appContext.packageName
        val currentIcon = AppSettings.appIcon.value

        for (icon in AppIcon.entries) {
            val component = ComponentName(packageName, icon.aliasName)
            val desiredState = if (icon == currentIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                val currentState = pm.getComponentEnabledSetting(component)
                if (currentState != desiredState) {
                    pm.setComponentEnabledSetting(component, desiredState, PackageManager.DONT_KILL_APP)
                }
            } catch (_: Exception) {}
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
