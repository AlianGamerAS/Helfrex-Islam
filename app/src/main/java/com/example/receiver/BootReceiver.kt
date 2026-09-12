package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.service.PrayerForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Boot broadcast received with action=$action")

        try {
            // 1. Reschedule all exact alarms
            PrayerAlarmScheduler.scheduleAllAlarms(context)

            // 2. Start Foreground Ongoing Notification Service immediately
            PrayerForegroundService.startService(context)
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error starting foreground service on boot", e)
        }
    }
}
