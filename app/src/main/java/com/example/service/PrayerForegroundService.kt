package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FaziletPrayerService
import com.example.data.PreferencesManager
import com.example.model.AppLanguage
import com.example.model.PrayerTimeItem
import com.example.model.PrayerType
import com.example.model.ThemeStyle
import com.example.receiver.NotificationDismissReceiver
import com.example.receiver.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class PrayerForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var notificationManager: NotificationManager
    private var powerManager: PowerManager? = null
    private var isScreenInteractive = true
    private var lastDayOfYear = -1
    private var lastTriggeredPrayerKey = ""

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenInteractive = true
                    updateNotificationImmediately()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenInteractive = powerManager?.isInteractive ?: true

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildInitialNotification())
        startLiveCountdownLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotificationImmediately()
        PrayerAlarmScheduler.scheduleAllAlarms(this)
        return START_STICKY
    }

    private fun updateNotificationImmediately() {
        serviceScope.launch {
            try {
                performNotificationUpdate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performNotificationUpdate() {
        val prayerService = FaziletPrayerService.getInstance(applicationContext)
        val prefsManager = PreferencesManager.getInstance(applicationContext)

        // Midnight rollover check
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (lastDayOfYear != -1 && lastDayOfYear != currentDay) {
            prayerService.refreshDailyTimes()
            PrayerAlarmScheduler.scheduleAllAlarms(applicationContext)
        }
        lastDayOfYear = currentDay

        val settings = prefsManager.loadSettings()
        val data = prayerService.prayerDataFlow.value
        val enriched = prayerService.enrichNextPrayer(data, settings.selectedPrayers)
        val isTr = settings.language == AppLanguage.TR

        val nextPrayer = enriched.nextPrayer
        val remainingMillis = enriched.remainingMillisToNext

        val seconds = (remainingMillis / 1000) % 60
        val minutes = (remainingMillis / (1000 * 60)) % 60
        val hours = (remainingMillis / (1000 * 60 * 60))

        val formattedCountdown = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val nextName = if (nextPrayer != null) {
            nextPrayer.type.getName(settings.language)
        } else {
            when (settings.language) {
                AppLanguage.TR -> "Namaz"
                AppLanguage.AZ -> "Namaz"
                AppLanguage.RU -> "Намаз"
                AppLanguage.EN -> "Prayer"
            }
        }

        val enabledItems = enriched.items.filter { settings.selectedPrayers.contains(it.type.id) }.sortedBy { it.type.order }
        val activePrayerType = determineActivePrayerType(enabledItems, nextPrayer?.type)

        // Live check: when device system clock matches the exact displayed prayer time, play Azan immediately!
        val nowCal = Calendar.getInstance()
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val nowMin = nowCal.get(Calendar.MINUTE)
        val currentClockTimeStr = String.format(java.util.Locale.US, "%02d:%02d", nowHour, nowMin)

        for (item in enabledItems) {
            if (item.timeStr == currentClockTimeStr) {
                val triggerKey = "${nowCal.get(Calendar.DAY_OF_YEAR)}_${item.type.id}_$currentClockTimeStr"
                if (lastTriggeredPrayerKey != triggerKey) {
                    lastTriggeredPrayerKey = triggerKey
                    Log.d("PrayerForegroundService", "Device clock ($currentClockTimeStr) matches ${item.type.nameTr}. Playing Azan!")
                    AzanPlayerService.startPlayback(
                        context = applicationContext,
                        prayerNameTr = item.type.nameTr,
                        prayerNameEn = item.type.nameEn,
                        prayerTime = item.timeStr
                    )
                }
            }
        }

        val remoteViews = RemoteViews(packageName, R.layout.notification_prayer_bar)

        applyThemeToRemoteViews(
            remoteViews = remoteViews,
            settings = settings,
            nextName = nextName,
            countdownStr = formattedCountdown,
            items = enriched.items,
            activeType = activePrayerType,
            isTr = isTr
        )

        val notification = buildOngoingNotification(remoteViews)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startLiveCountdownLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    performNotificationUpdate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // If screen is on, update every second for live timer.
                // If screen is off, delay 20s to conserve battery and prevent OS kills.
                val delayTime = if (powerManager?.isInteractive != false) 1000L else 20000L
                delay(delayTime)
            }
        }
    }

    private fun determineActivePrayerType(items: List<PrayerTimeItem>, nextType: PrayerType?): PrayerType? {
        if (items.isEmpty()) return null
        if (nextType == null) return items.lastOrNull()?.type

        val nextIndex = items.indexOfFirst { it.type == nextType }
        return if (nextIndex > 0) {
            items[nextIndex - 1].type
        } else if (nextIndex == 0) {
            items.lastOrNull()?.type
        } else {
            null
        }
    }

    private fun applyThemeToRemoteViews(
        remoteViews: RemoteViews,
        settings: com.example.model.UserSettings,
        nextName: String,
        countdownStr: String,
        items: List<PrayerTimeItem>,
        activeType: PrayerType?,
        isTr: Boolean
    ) {
        val isDark = settings.isDarkMode
        val themeStyle = settings.themeStyle

        // 1. Root & Inner container backgrounds
        remoteViews.setInt(
            R.id.notif_root_layout,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_notif_panel_dark else R.drawable.bg_notif_panel_light
        )
        remoteViews.setInt(
            R.id.notif_inner_strip,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_notif_inner_dark else R.drawable.bg_notif_inner_light
        )
        remoteViews.setInt(
            R.id.notif_countdown_container,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_countdown_pill_dark else R.drawable.bg_countdown_pill_light
        )

        // 2. Title without location, formatting as "[Prayer] Vaktine:"
        val titleText = when (settings.language) {
            AppLanguage.TR -> "$nextName Vaktine:"
            AppLanguage.AZ -> "$nextName Vaxtına:"
            AppLanguage.RU -> "До времени $nextName:"
            AppLanguage.EN -> "$nextName Time:"
        }
        remoteViews.setTextViewText(R.id.notif_title_text, titleText)
        remoteViews.setTextColor(
            R.id.notif_title_text,
            if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
        )

        remoteViews.setTextViewText(R.id.notif_countdown_text, countdownStr)

        val countdownColor = when (themeStyle) {
            ThemeStyle.CLASSIC -> if (isDark) Color.WHITE else Color.BLACK
            ThemeStyle.NEON_BLUE -> Color.parseColor("#00E5FF")
            ThemeStyle.NEON_PURPLE -> Color.parseColor("#FF2E93")
            ThemeStyle.NEON_EMERALD -> Color.parseColor("#50C878")
        }
        remoteViews.setTextColor(R.id.notif_countdown_text, countdownColor)

        // 3. Active box drawable
        val activeBoxDrawable = when (themeStyle) {
            ThemeStyle.CLASSIC -> if (isDark) R.drawable.bg_prayer_box_active_white else R.drawable.bg_prayer_box_active_black
            ThemeStyle.NEON_BLUE -> R.drawable.bg_prayer_box_active_blue
            ThemeStyle.NEON_PURPLE -> R.drawable.bg_prayer_box_active_pink
            ThemeStyle.NEON_EMERALD -> R.drawable.bg_prayer_box_active_emerald
        }

        val activeTextColor = when (themeStyle) {
            ThemeStyle.CLASSIC -> if (isDark) Color.BLACK else Color.WHITE
            ThemeStyle.NEON_BLUE -> Color.parseColor("#0A0C10")
            ThemeStyle.NEON_EMERALD -> Color.parseColor("#002010")
            else -> Color.WHITE
        }

        val inactiveBoxDrawable = if (isDark) R.drawable.bg_prayer_box_inactive_dark else R.drawable.bg_prayer_box_inactive_light
        val inactiveNameColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        val inactiveTimeColor = if (isDark) Color.WHITE else Color.parseColor("#0F172A")

        // Map Prayer items to Box View IDs
        val prayerViewMappings = listOf(
            Triple(PrayerType.IMSAK, R.id.notif_box_imsak, Pair(R.id.notif_name_imsak, R.id.notif_time_imsak)),
            Triple(PrayerType.SABAH, R.id.notif_box_sabah, Pair(R.id.notif_name_sabah, R.id.notif_time_sabah)),
            Triple(PrayerType.GUNES, R.id.notif_box_gunes, Pair(R.id.notif_name_gunes, R.id.notif_time_gunes)),
            Triple(PrayerType.OGLE, R.id.notif_box_ogle, Pair(R.id.notif_name_ogle, R.id.notif_time_ogle)),
            Triple(PrayerType.IKINDI, R.id.notif_box_ikindi, Pair(R.id.notif_name_ikindi, R.id.notif_time_ikindi)),
            Triple(PrayerType.AKSAM, R.id.notif_box_aksam, Pair(R.id.notif_name_aksam, R.id.notif_time_aksam)),
            Triple(PrayerType.YATSI, R.id.notif_box_yatsi, Pair(R.id.notif_name_yatsi, R.id.notif_time_yatsi))
        )

        for ((type, boxId, textIds) in prayerViewMappings) {
            val isSelected = settings.selectedPrayers.contains(type.id)
            if (!isSelected) {
                remoteViews.setViewVisibility(boxId, View.GONE)
            } else {
                remoteViews.setViewVisibility(boxId, View.VISIBLE)
                val item = items.find { it.type == type }
                val timeStr = item?.timeStr ?: "--:--"
                val nameStr = if (isTr) type.nameTr else type.nameEn

                remoteViews.setTextViewText(textIds.first, nameStr)
                remoteViews.setTextViewText(textIds.second, timeStr)

                val isActive = (type == activeType)
                if (isActive) {
                    remoteViews.setInt(boxId, "setBackgroundResource", activeBoxDrawable)
                    remoteViews.setTextColor(textIds.first, activeTextColor)
                    remoteViews.setTextColor(textIds.second, activeTextColor)
                } else {
                    remoteViews.setInt(boxId, "setBackgroundResource", inactiveBoxDrawable)
                    remoteViews.setTextColor(textIds.first, inactiveNameColor)
                    remoteViews.setTextColor(textIds.second, inactiveTimeColor)
                }
            }
        }
    }

    private fun buildOngoingNotification(remoteViews: RemoteViews): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(this, NotificationDismissReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            this,
            1005,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine system & app dark mode: icon must be WHITE in dark mode, BLACK in light mode - NEVER yellow!
        val isSystemNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val prefs = PreferencesManager.getInstance(this).loadSettings()
        val isDark = isSystemNight || prefs.isDarkMode
        val iconColor = if (isDark) Color.WHITE else Color.BLACK

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(iconColor)
            .setColorized(false)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setCustomHeadsUpContentView(remoteViews)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun buildInitialNotification(): Notification {
        val initialRemoteViews = RemoteViews(packageName, R.layout.notification_prayer_bar)
        initialRemoteViews.setTextViewText(R.id.notif_title_text, "Vakitler yükleniyor...")
        initialRemoteViews.setTextViewText(R.id.notif_countdown_text, "--:--:--")
        return buildOngoingNotification(initialRemoteViews)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sabit Namaz Vakitleri Bildirimi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Seçili namaz vakitleri ve bir sonraki ezana kalan süre bildirimi"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // When app is swiped away from recent tasks, ensure foreground service stays persistent
        try {
            startService(applicationContext)
            val restartIntent = Intent(applicationContext, PrayerForegroundService::class.java).also {
                it.setPackage(packageName)
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1002,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am?.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 300L, restartPendingIntent)
            } else {
                am?.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 300L, restartPendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        // Auto-restart if killed by OS
        try {
            val restartIntent = Intent(applicationContext, PrayerForegroundService::class.java).also {
                it.setPackage(packageName)
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1003,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am?.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500L, restartPendingIntent)
            } else {
                am?.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500L, restartPendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "helfrex_ongoing_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, PrayerForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
