package com.example

import android.app.Application
import com.example.data.FaziletPrayerService
import com.example.receiver.PrayerAlarmScheduler
import com.example.service.PrayerForegroundService

open class HelfrexApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Prayer Service
        val prayerService = FaziletPrayerService.getInstance(this)
        
        // Start Ongoing Foreground Notification Service
        PrayerForegroundService.startService(this)

        // Preload offline audio and PDF resources on startup
        com.example.data.OfflineResourcePreloader.preloadInitialResources(this)

        // Schedule exact alarms for enabled prayer times
        PrayerAlarmScheduler.scheduleAllAlarms(this)

        // Ensure active launcher icon matches saved preferences
        try {
            val prefs = com.example.data.PreferencesManager.getInstance(this).loadSettings()
            com.example.util.IconManager.updateAppIcon(this, prefs.themeStyle, prefs.isDarkMode)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class PrayerApplication : HelfrexApp()

