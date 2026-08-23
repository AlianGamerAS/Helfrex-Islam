package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.FaziletPrayerService
import com.example.data.PreferencesManager
import com.example.model.PrayerTimeItem

object PrayerAlarmScheduler {

    private const val TAG = "PrayerAlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager.getInstance(context)
        val prayerService = FaziletPrayerService.getInstance(context)

        val settings = prefsManager.loadSettings()
        val data = prayerService.prayerDataFlow.value

        val now = System.currentTimeMillis()

        data.items.forEach { item ->
            // Only schedule if user selected this prayer in settings
            if (settings.selectedPrayers.contains(item.type.id)) {
                var targetMillis = item.targetTimeMillis
                if (targetMillis <= now) {
                    // Already passed today, schedule for tomorrow (+24h)
                    targetMillis += 24 * 60 * 60 * 1000L
                }

                val requestCode = item.type.order * 100
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_ID, item.type.id)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME_TR, item.type.nameTr)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME_EN, item.type.nameEn)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, item.timeStr)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetMillis,
                            pendingIntent
                        )
                    }
                    Log.d(TAG, "Scheduled alarm for ${item.type.nameTr} at $targetMillis")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule exact alarm", e)
                }
            }
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 1..7) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i * 100,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
