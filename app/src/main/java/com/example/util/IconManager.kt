package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.model.ThemeStyle

object IconManager {
    private const val TAG = "IconManager"

    const val ALIAS_DEFAULT = "com.example.MainActivityDefault"
    const val ALIAS_CLASSIC_LIGHT = "com.example.MainActivityClassicLight"
    const val ALIAS_NEON_BLUE = "com.example.MainActivityNeonBlue"
    const val ALIAS_NEON_PURPLE = "com.example.MainActivityNeonPurple"
    const val ALIAS_NEON_EMERALD = "com.example.MainActivityNeonEmerald"

    private val allAliases = listOf(
        ALIAS_DEFAULT,
        ALIAS_CLASSIC_LIGHT,
        ALIAS_NEON_BLUE,
        ALIAS_NEON_PURPLE,
        ALIAS_NEON_EMERALD
    )

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

    /**
     * Immediately applies icon alias updates.
     * Enables target alias first, then disables other aliases using DONT_KILL_APP.
     */
    fun updateAppIcon(context: Context, themeStyle: ThemeStyle, isDarkMode: Boolean) {
        val targetAlias = getTargetAlias(themeStyle, isDarkMode)
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkg = appContext.packageName

        try {
            // 1. Enable target alias FIRST so the launcher always has a valid component
            val targetComponent = ComponentName(pkg, targetAlias)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other aliases
            for (alias in allAliases) {
                if (alias != targetAlias) {
                    val otherComponent = ComponentName(pkg, alias)
                    pm.setComponentEnabledSetting(
                        otherComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            Log.d(TAG, "Successfully applied launcher icon alias: $targetAlias")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying icon alias: ${e.message}", e)
        }
    }

    fun applyPendingIconUpdate(context: Context) {
        // Kept for backward compatibility
    }
}

