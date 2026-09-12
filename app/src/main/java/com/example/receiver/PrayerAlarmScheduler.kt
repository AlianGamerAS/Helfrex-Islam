package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.FaziletPrayerCalculator
import com.example.data.PreferencesManager
import java.util.Calendar

object PrayerAlarmScheduler {

    private const val TAG = "PrayerAlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefsManager = PreferencesManager.getInstance(context)

        val settings = prefsManager.loadSettings()
        val (lat, lng) = prefsManager.getLastLocation()
        val (city, district) = prefsManager.getLastCityAndDistrict()
        val targetTz = FaziletPrayerCalculator.getTimeZoneForLocation(lat, lng, city, district)

        val now = System.currentTimeMillis()

        // 1. Calculate TODAY's exact times
        val todayCal = Calendar.getInstance(targetTz)
        val todayData = FaziletPrayerCalculator.calculateDailyTimes(todayCal, lat, lng, city, district)

        // 2. Calculate TOMORROW's exact times (accounts for solar declination and daily minute shifts)
        val tomorrowCal = (todayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowData = FaziletPrayerCalculator.calculateDailyTimes(tomorrowCal, lat, lng, city, district)

        todayData.items.forEach { todayItem ->
            if (settings.selectedPrayers.contains(todayItem.type.id)) {
                // If the prayer has not occurred yet today, schedule for today.
                // If it already passed, schedule for TOMORROW'S exact newly calculated time!
                val (targetMillis, timeStr) = if (todayItem.targetTimeMillis > now + 2000L) {
                    Pair(todayItem.targetTimeMillis, todayItem.timeStr)
                } else {
                    val tomorrowItem = tomorrowData.items.find { it.type == todayItem.type }
                    if (tomorrowItem != null) {
                        Pair(tomorrowItem.targetTimeMillis, tomorrowItem.timeStr)
                    } else {
                        Pair(todayItem.targetTimeMillis + 24 * 60 * 60 * 1000L, todayItem.timeStr)
                    }
                }

                val requestCode = todayItem.type.order * 100
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_ID, todayItem.type.id)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME_TR, todayItem.type.nameTr)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME_EN, todayItem.type.nameEn)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, timeStr)
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
                    Log.d(TAG, "Scheduled alarm for ${todayItem.type.nameTr} at $timeStr (millis=$targetMillis)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule exact alarm for ${todayItem.type.nameTr}", e)
                }
            }
        }

        // 3. Schedule Daily Midnight Rollover at 00:00:15
        try {
            val midnightCal = (todayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 15)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val midnightIntent = Intent(context, PrayerWatchdogReceiver::class.java).apply {
                action = "com.example.ACTION_MIDNIGHT_ROLLOVER"
            }
            val midnightPendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                midnightIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    midnightCal.timeInMillis,
                    midnightPendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    midnightCal.timeInMillis,
                    midnightPendingIntent
                )
            }
            Log.d(TAG, "Scheduled midnight rollover for ${midnightCal.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule midnight rollover", e)
        }

        // 4. Schedule Periodic Watchdog Keep-Alive (every 45 minutes) to ensure ongoing service stays alive
        try {
            val watchdogIntent = Intent(context, PrayerWatchdogReceiver::class.java).apply {
                action = "com.example.ACTION_WATCHDOG_CHECK"
            }
            val watchdogPendingIntent = PendingIntent.getBroadcast(
                context,
                8888,
                watchdogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val watchdogTriggerAt = now + 45 * 60 * 1000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    watchdogTriggerAt,
                    watchdogPendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    watchdogTriggerAt,
                    watchdogPendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule watchdog keep-alive", e)
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

