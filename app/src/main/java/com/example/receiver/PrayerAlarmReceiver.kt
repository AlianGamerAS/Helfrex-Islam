package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.PreferencesManager
import com.example.service.AzanPlayerService

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: return
        val prayerNameTr = intent.getStringExtra(EXTRA_PRAYER_NAME_TR) ?: "Namaz"
        val prayerNameEn = intent.getStringExtra(EXTRA_PRAYER_NAME_EN) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        val prefsManager = PreferencesManager.getInstance(context)
        val settings = prefsManager.loadSettings()

        // Verify that this prayer is currently selected
        if (!settings.selectedPrayers.contains(prayerId)) {
            Log.d("PrayerAlarmReceiver", "Prayer $prayerId is disabled in settings. Skipping alarm.")
            return
        }

        Log.d("PrayerAlarmReceiver", "Triggering Azan for $prayerNameTr at $prayerTime")

        // Start Azan Player Service
        val serviceIntent = Intent(context, AzanPlayerService::class.java).apply {
            putExtra(AzanPlayerService.EXTRA_PRAYER_NAME_TR, prayerNameTr)
            putExtra(AzanPlayerService.EXTRA_PRAYER_NAME_EN, prayerNameEn)
            putExtra(AzanPlayerService.EXTRA_PRAYER_TIME, prayerTime)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("PrayerAlarmReceiver", "Failed to start AzanPlayerService", e)
        }

        // Reschedule future alarms
        PrayerAlarmScheduler.scheduleAllAlarms(context)
    }

    companion object {
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_PRAYER_NAME_TR = "extra_prayer_name_tr"
        const val EXTRA_PRAYER_NAME_EN = "extra_prayer_name_en"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
