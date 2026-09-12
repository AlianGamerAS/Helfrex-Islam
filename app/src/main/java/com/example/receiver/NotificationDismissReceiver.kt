package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.PrayerForegroundService

/**
 * Receiver that triggers when the user or system dismisses the ongoing prayer bar notification.
 * Ensures the notification is immediately recreated and remains persistent.
 */
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationDismissReceiver", "Ongoing notification was dismissed. Recreating immediately...")
        try {
            PrayerForegroundService.startService(context)
        } catch (e: Exception) {
            Log.e("NotificationDismissReceiver", "Failed to restart PrayerForegroundService", e)
        }
    }
}
