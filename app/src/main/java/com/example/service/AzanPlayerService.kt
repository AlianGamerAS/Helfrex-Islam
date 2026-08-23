package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FileDownloader
import com.example.data.PreferencesManager
import com.example.model.AppLanguage
import com.example.model.AzanDuration
import com.example.model.AzanSound
import com.example.receiver.StopAzanReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AzanPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoStopJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_AZAN) {
            stopPlaybackInternal()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerNameTr = intent?.getStringExtra(EXTRA_PRAYER_NAME_TR) ?: "Namaz"
        val prayerNameEn = intent?.getStringExtra(EXTRA_PRAYER_NAME_EN) ?: "Prayer"
        val prayerTime = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        val prefsManager = PreferencesManager.getInstance(this)
        val settings = prefsManager.loadSettings()
        val isTr = settings.language == AppLanguage.TR

        createNotificationChannel()

        val notification = buildAzanNotification(prayerNameTr, prayerNameEn, prayerTime, isTr)
        startForeground(NOTIFICATION_ID, notification)

        // Acquire WakeLock so device stays awake during alarm playback
        acquireWakeLock()

        playAzanSound(settings.azanSound, settings.azanDuration)

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HelfrexIslam:AzanWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(15 * 60 * 1000L) // 15 minutes max safeguard
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }

    private fun playAzanSound(sound: AzanSound, duration: AzanDuration) {
        stopPlaybackInternal()

        when (sound) {
            AzanSound.SILENT -> {
                Log.d(TAG, "Silent mode selected. No audio played.")
                // Auto dismiss notification after 20 seconds
                autoStopJob = serviceScope.launch {
                    delay(20000L)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            AzanSound.RINGTONE -> {
                val targetLoops = if (duration == AzanDuration.SHORT) 5 else 10
                val rawResId = R.raw.ezan2

                val mp = com.example.util.SoundPlayerHelper.createMediaPlayer(
                    context = applicationContext,
                    rawResId = rawResId,
                    downloadedFileName = "ringtone.mp3",
                    usage = AudioAttributes.USAGE_ALARM,
                    contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION
                )

                if (mp != null) {
                    setupRingtonePlayerAndLoop(mp, targetLoops)
                } else {
                    Log.e(TAG, "Could not initialize ringtone player.")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            AzanSound.AZAN -> {
                val rawResId = R.raw.ezan1

                val mp = com.example.util.SoundPlayerHelper.createMediaPlayer(
                    context = applicationContext,
                    rawResId = rawResId,
                    downloadedFileName = "ezan.mp3",
                    usage = AudioAttributes.USAGE_ALARM,
                    contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION
                )

                if (mp != null) {
                    setupAzanPlayer(mp, duration == AzanDuration.SHORT)
                } else {
                    Log.e(TAG, "Could not initialize azan player.")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun setupAzanPlayer(mp: MediaPlayer, isShort11Seconds: Boolean) {
        try {
            mediaPlayer = mp
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set max alarm volume: ${e.message}")
            }

            mp.setVolume(1.0f, 1.0f)
            mp.start()

            if (isShort11Seconds) {
                Log.d(TAG, "Azan Short mode: playing 11 seconds then stopping.")
                autoStopJob = serviceScope.launch {
                    delay(11000L)
                    Log.d(TAG, "11 seconds elapsed for Short Azan. Stopping.")
                    stopPlaybackInternal()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } else {
                Log.d(TAG, "Azan Long mode: playing full audio.")
                mp.setOnCompletionListener {
                    Log.d(TAG, "Full Azan completed.")
                    stopPlaybackInternal()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting azan playback", e)
            stopPlaybackInternal()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setupRingtonePlayerAndLoop(mp: MediaPlayer, targetLoops: Int) {
        try {
            mediaPlayer = mp
            // Max volume on Alarm stream
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set max alarm volume: ${e.message}")
            }

            mp.setVolume(1.0f, 1.0f)
            mp.start()

            var currentLoop = 1
            Log.d(TAG, "Starting ringtone playback (Loop $currentLoop of $targetLoops)")

            mp.setOnCompletionListener { player ->
                if (currentLoop < targetLoops) {
                    currentLoop++
                    Log.d(TAG, "Ringtone loop completed. Starting repetition $currentLoop of $targetLoops")
                    try {
                        player.seekTo(0)
                        player.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restarting ringtone loop: ${e.message}")
                        stopPlaybackInternal()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    Log.d(TAG, "All $targetLoops ringtone loops finished.")
                    stopPlaybackInternal()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ringtone playback", e)
            stopPlaybackInternal()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopPlaybackInternal() {
        autoStopJob?.cancel()
        autoStopJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing mediaPlayer", e)
        }
        releaseWakeLock()
    }

    private fun buildAzanNotification(
        prayerNameTr: String,
        prayerNameEn: String,
        prayerTime: String,
        isTr: Boolean
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StopAzanReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTr) "$prayerNameTr Vakti Girdi ($prayerTime)" else "$prayerNameEn Prayer Time ($prayerTime)"
        val message = if (isTr) "Helfrex İslam: Vakit Bildirimi" else "Helfrex Islam: Prayer Call"
        val stopLabel = if (isTr) "Durdur" else "Stop"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, stopLabel, stopPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Helfrex Ezan Alarmı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaz vakti girdiğinde çalan ezan bildirimleri"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopPlaybackInternal()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AzanPlayerService"
        const val CHANNEL_ID = "helfrex_azan_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_STOP_AZAN = "com.example.ACTION_STOP_AZAN"
        const val EXTRA_PRAYER_NAME_TR = "extra_prayer_name_tr"
        const val EXTRA_PRAYER_NAME_EN = "extra_prayer_name_en"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"

        fun stopPlayback(context: Context) {
            try {
                val intent = Intent(context, AzanPlayerService::class.java).apply {
                    action = ACTION_STOP_AZAN
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
