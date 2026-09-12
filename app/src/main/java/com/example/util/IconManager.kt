package com.example.util

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.example.model.ThemeStyle

object IconManager {
    private const val TAG = "IconManager"
    private const val PREFS_NAME = "icon_manager_prefs"
    private const val KEY_PENDING_ALIAS = "pending_icon_alias"

    const val ALIAS_CLASSIC_DARK = "com.example.MainActivityDefault"
    const val ALIAS_CLASSIC_LIGHT = "com.example.MainActivityClassicLight"
    const val ALIAS_NEON_BLUE_DARK = "com.example.MainActivityNeonBlue"
    const val ALIAS_NEON_BLUE_LIGHT = "com.example.MainActivityNeonBlueLight"
    const val ALIAS_NEON_PURPLE_DARK = "com.example.MainActivityNeonPurple"
    const val ALIAS_NEON_PURPLE_LIGHT = "com.example.MainActivityNeonPurpleLight"
    const val ALIAS_NEON_EMERALD_DARK = "com.example.MainActivityNeonEmerald"
    const val ALIAS_NEON_EMERALD_LIGHT = "com.example.MainActivityNeonEmeraldLight"

    val allAliases = listOf(
        ALIAS_CLASSIC_DARK,
        ALIAS_CLASSIC_LIGHT,
        ALIAS_NEON_BLUE_DARK,
        ALIAS_NEON_BLUE_LIGHT,
        ALIAS_NEON_PURPLE_DARK,
        ALIAS_NEON_PURPLE_LIGHT,
        ALIAS_NEON_EMERALD_DARK,
        ALIAS_NEON_EMERALD_LIGHT
    )

    private var activeActivityCount = 0

    fun initLifecycleObserver(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activeActivityCount++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activeActivityCount = maxOf(0, activeActivityCount - 1)
                // When app goes to background (no visible activities), safely apply pending icon alias
                if (activeActivityCount == 0) {
                    applyPendingIconUpdate(activity.applicationContext)
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Returns the exact alias to enable for the current theme and dark/light mode setting.
     */
    fun getTargetAlias(themeStyle: ThemeStyle, isDarkMode: Boolean): String {
        return when (themeStyle) {
            ThemeStyle.CLASSIC -> if (isDarkMode) ALIAS_CLASSIC_DARK else ALIAS_CLASSIC_LIGHT
            ThemeStyle.NEON_BLUE -> if (isDarkMode) ALIAS_NEON_BLUE_DARK else ALIAS_NEON_BLUE_LIGHT
            ThemeStyle.NEON_PURPLE -> if (isDarkMode) ALIAS_NEON_PURPLE_DARK else ALIAS_NEON_PURPLE_LIGHT
            ThemeStyle.NEON_EMERALD -> if (isDarkMode) ALIAS_NEON_EMERALD_DARK else ALIAS_NEON_EMERALD_LIGHT
        }
    }

    /**
     * Schedules an icon update.
     * If the app is currently in foreground, we DO NOT disable the active activity-alias immediately
     * because Android kills the process if the running component is disabled.
     * It will be safely applied once the user moves the app to background or exits.
     */
    fun updateAppIcon(context: Context, themeStyle: ThemeStyle, isDarkMode: Boolean) {
        val targetAlias = getTargetAlias(themeStyle, isDarkMode)
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_ALIAS, targetAlias).apply()

        // Only apply immediately if no activity is visible on screen
        if (activeActivityCount <= 0) {
            applyPendingIconUpdate(appContext)
        } else {
            Log.d(TAG, "Icon update queued for background: $targetAlias (preventing app kill)")
        }
    }

    /**
     * Safely applies the pending icon alias update when the app is in background.
     */
    fun applyPendingIconUpdate(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val targetAlias = prefs.getString(KEY_PENDING_ALIAS, null) ?: return
        val pm = appContext.packageManager
        val pkg = appContext.packageName

        try {
            val targetComponent = ComponentName(pkg, targetAlias)
            val currentTargetState = pm.getComponentEnabledSetting(targetComponent)

            // If target is already enabled, just make sure others are disabled
            if (currentTargetState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                // 1. Enable target alias first
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // 2. Disable other aliases
            for (alias in allAliases) {
                if (alias != targetAlias) {
                    val otherComponent = ComponentName(pkg, alias)
                    val otherState = pm.getComponentEnabledSetting(otherComponent)
                    if (otherState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            otherComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
            Log.d(TAG, "Successfully applied pending launcher icon alias: $targetAlias")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying icon alias: ${e.message}", e)
        }
    }
}
