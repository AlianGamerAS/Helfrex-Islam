package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.FaziletPrayerService
import com.example.service.PrayerForegroundService

/**
 * Watchdog and Daily Midnight Rollover Receiver.
 * Ensures the date rollover recalculates prayer times accurately for the new day,
 * and maintains the persistent ongoing foreground service.
 */
class PrayerWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PrayerWatchdogReceiver", "Watchdog triggered: action=${intent.action}")

        try {
            // 1. Refresh today's and tomorrow's prayer times
            FaziletPrayerService.getInstance(context).refreshDailyTimes()

            // 2. Reschedule exact alarms for the current/next day
            PrayerAlarmScheduler.scheduleAllAlarms(context)

            // 3. Ensure persistent foreground notification is active
            PrayerForegroundService.startService(context)
        } catch (e: Exception) {
            Log.e("PrayerWatchdogReceiver", "Error running watchdog", e)
        }
    }
}
