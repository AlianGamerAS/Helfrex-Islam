package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.model.ThemeStyle

object IconManager {
    private const val TAG = "IconManager"

    private const val ALIAS_DEFAULT = "com.example.MainActivityDefault"
    private const val ALIAS_CLASSIC_LIGHT = "com.example.MainActivityClassicLight"
    private const val ALIAS_NEON_BLUE = "com.example.MainActivityNeonBlue"
    private const val ALIAS_NEON_PURPLE = "com.example.MainActivityNeonPurple"
    private const val ALIAS_NEON_EMERALD = "com.example.MainActivityNeonEmerald"

    private var pendingTargetAlias: String? = null

    /**
     * Returns the exact alias to enable for the current theme and dark mode setting.
     */
    fun getTargetAlias(themeStyle: ThemeStyle, isDarkMode: Boolean): String {
        return when (themeStyle) {
            ThemeStyle.CLASSIC -> if (isDarkMode) ALIAS_DEFAULT else ALIAS_CLASSIC_LIGHT
            ThemeStyle.NEON_BLUE -> ALIAS_NEON_BLUE
            ThemeStyle.NEON_PURPLE -> ALIAS_NEON_PURPLE
            ThemeStyle.NEON_EMERALD -> ALIAS_NEON_EMERALD
        }
    }

    fun scheduleIconUpdate(context: Context, themeStyle: ThemeStyle, isDarkMode: Boolean) {
        pendingTargetAlias = getTargetAlias(themeStyle, isDarkMode)
    }

    /**
     * Applies icon alias updates safely when the activity is stopped or in background.
     * Ensures EXACTLY ONE alias is enabled to prevent duplicate launcher icons.
     */
    fun applyPendingIconUpdate(context: Context) {
        val targetAlias = pendingTargetAlias ?: return
        pendingTargetAlias = null

        val allAliases = listOf(
            ALIAS_DEFAULT,
            ALIAS_CLASSIC_LIGHT,
            ALIAS_NEON_BLUE,
            ALIAS_NEON_PURPLE,
            ALIAS_NEON_EMERALD
        )
        val pm = context.packageManager

        for (alias in allAliases) {
            val componentName = ComponentName(context.packageName, alias)
            val desiredState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                if (pm.getComponentEnabledSetting(componentName) != desiredState) {
                    pm.setComponentEnabledSetting(
                        componentName,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not update alias setting for $alias: ${e.message}")
            }
        }
    }

    fun updateAppIcon(context: Context, themeStyle: ThemeStyle, isDarkMode: Boolean) {
        scheduleIconUpdate(context, themeStyle, isDarkMode)
    }
}
