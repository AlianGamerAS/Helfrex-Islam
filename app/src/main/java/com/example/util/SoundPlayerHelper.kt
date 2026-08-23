package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import com.example.R
import com.example.data.FileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object SoundPlayerHelper {
    private const val TAG = "SoundPlayerHelper"

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Generates a serene, melodious chime WAV file for Ringtone (Zil Sesi) so that
     * it sounds like a real pleasant notification chime/ringtone and NOT the Ezan audio.
     */
    fun generateMelodicRingtoneFile(context: Context): File {
        val targetFile = File(context.filesDir, "ringtone.mp3")
        try {
            val sampleRate = 44100
            val durationSec = 3.0
            val totalSamples = (sampleRate * durationSec).toInt()
            val audioData = ShortArray(totalSamples)

            // Harmonious 4-note peaceful chime (E5 -> G#5 -> B5 -> E6)
            val notes = listOf(
                Pair(0.0, 659.25),
                Pair(0.55, 830.61),
                Pair(1.10, 987.77),
                Pair(1.65, 1318.51)
            )

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                var sample = 0.0

                for ((startTime, freq) in notes) {
                    if (t >= startTime) {
                        val dt = t - startTime
                        val decay = Math.exp(-dt * 3.0)
                        val fundamental = Math.sin(2.0 * Math.PI * freq * dt)
                        val overtone1 = 0.4 * Math.sin(2.0 * Math.PI * (freq * 2.0) * dt)
                        val overtone2 = 0.2 * Math.sin(2.0 * Math.PI * (freq * 3.0) * dt)
                        val overtone3 = 0.1 * Math.sin(2.0 * Math.PI * (freq * 4.0) * dt)

                        sample += (fundamental + overtone1 + overtone2 + overtone3) * decay
                    }
                }

                val clamped = (sample * 16000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                audioData[i] = clamped
            }

            val byteDataSize = totalSamples * 2
            val totalFileSize = 44 + byteDataSize

            FileOutputStream(targetFile).use { fos ->
                fos.write("RIFF".toByteArray(Charsets.US_ASCII))
                fos.write(intToByteArray(totalFileSize - 8))
                fos.write("WAVE".toByteArray(Charsets.US_ASCII))
                fos.write("fmt ".toByteArray(Charsets.US_ASCII))
                fos.write(intToByteArray(16)) // PCM subchunk size
                fos.write(shortToByteArray(1)) // AudioFormat = 1 (PCM)
                fos.write(shortToByteArray(1)) // Channels = 1 (mono)
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(sampleRate * 2))
                fos.write(shortToByteArray(2)) // BlockAlign
                fos.write(shortToByteArray(16)) // BitsPerSample
                fos.write("data".toByteArray(Charsets.US_ASCII))
                fos.write(intToByteArray(byteDataSize))

                val buffer = ByteArray(2)
                for (s in audioData) {
                    buffer[0] = (s.toInt() and 0xFF).toByte()
                    buffer[1] = ((s.toInt() shr 8) and 0xFF).toByte()
                    fos.write(buffer)
                }
                fos.flush()
            }
            Log.d(TAG, "Successfully generated melodious ringtone chime at ${targetFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating ringtone: ${e.message}", e)
        }
        return targetFile
    }

    /**
     * Extracts raw resource to internal storage if valid and needed.
     */
    fun extractRawAudioFile(context: Context, rawResId: Int, fileName: String): File? {
        val file = File(context.filesDir, fileName)
        try {
            if (!file.exists() || !FileDownloader.isValidAudio(file)) {
                context.resources.openRawResource(rawResId).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (file.exists() && FileDownloader.isValidAudio(file)) {
                return file
            }
        } catch (e: Exception) {
            Log.w(TAG, "Raw extract failed for $fileName: ${e.message}")
        }
        return null
    }

    /**
     * Ensures an audio file exists in context.filesDir.
     * For Ringtone (Zil Sesi), ensures a melodic chime is generated and used.
     * For Azan (Ezan Sesi), downloads/prepares the real Ezan audio.
     */
    suspend fun prepareAudioFile(
        context: Context,
        fileName: String,
        remoteUrl: String,
        rawFallbackRes: Int? = null
    ): File? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext

        // If preparing ringtone, generate custom melodious chime
        if (fileName == "ringtone.mp3") {
            val ringtoneFile = File(appContext.filesDir, fileName)
            // If ringtone file does not exist or was old duplicate ezan, recreate it
            if (!ringtoneFile.exists() || ringtoneFile.length() > 500000L) { // Ezan is ~1.5MB+, Chime is ~260KB
                generateMelodicRingtoneFile(appContext)
            }
            return@withContext ringtoneFile
        }

        val targetFile = File(appContext.filesDir, fileName)
        if (targetFile.exists() && FileDownloader.isValidAudio(targetFile) && targetFile.length() > 5000) {
            return@withContext targetFile
        }

        // Try downloading from real URL
        val result = FileDownloader.downloadOrGetFile(
            context = appContext,
            url = remoteUrl,
            fileName = fileName,
            rawResFallback = rawFallbackRes
        )

        if (result.isSuccess) {
            val f = result.getOrNull()
            if (f != null && FileDownloader.isValidAudio(f)) {
                return@withContext f
            }
        }

        // Try raw resource fallback
        if (rawFallbackRes != null) {
            val extracted = extractRawAudioFile(appContext, rawFallbackRes, fileName)
            if (extracted != null && extracted.exists() && FileDownloader.isValidAudio(extracted)) {
                return@withContext extracted
            }
        }

        if (targetFile.exists() && FileDownloader.isValidAudio(targetFile)) {
            return@withContext targetFile
        }

        return@withContext null
    }

    /**
     * Creates a high-reliability MediaPlayer instance.
     */
    fun createMediaPlayer(
        context: Context,
        rawResId: Int,
        downloadedFileName: String? = null,
        remoteUrl: String? = null,
        usage: Int = AudioAttributes.USAGE_MEDIA,
        contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC
    ): MediaPlayer? {
        val appContext = context.applicationContext

        // Boost device audio stream safely
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val streamType = if (usage == AudioAttributes.USAGE_ALARM) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
                val maxVol = audioManager.getStreamMaxVolume(streamType)
                val currentVol = audioManager.getStreamVolume(streamType)
                if (currentVol < (maxVol * 0.7f).toInt()) {
                    audioManager.setStreamVolume(streamType, maxVol, 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio volume adjustment warning: ${e.message}")
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()

        // 1. If downloadedFileName is ringtone.mp3, make sure it's the chime
        if (downloadedFileName == "ringtone.mp3") {
            val ringtoneFile = File(appContext.filesDir, "ringtone.mp3")
            if (!ringtoneFile.exists() || ringtoneFile.length() > 500000L) {
                generateMelodicRingtoneFile(appContext)
            }
            if (ringtoneFile.exists() && ringtoneFile.length() > 1024) {
                try {
                    val player = MediaPlayer()
                    player.setAudioAttributes(audioAttributes)
                    try {
                        player.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (e: Exception) {}

                    val fis = FileInputStream(ringtoneFile)
                    player.setDataSource(fis.fd)
                    player.prepare()
                    fis.close()

                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Ringtone MediaPlayer error: what=$what, extra=$extra")
                        true
                    }
                    Log.d(TAG, "Successfully created MediaPlayer from ringtone chime: ${ringtoneFile.name}")
                    return player
                } catch (e: Exception) {
                    Log.w(TAG, "Ringtone play failed: ${e.message}")
                }
            }
        }

        // 2. Try local downloaded file if valid
        if (downloadedFileName != null && downloadedFileName != "ringtone.mp3") {
            val localFile = File(appContext.filesDir, downloadedFileName)
            if (localFile.exists() && FileDownloader.isValidAudio(localFile) && localFile.length() > 1024) {
                try {
                    val player = MediaPlayer()
                    player.setAudioAttributes(audioAttributes)
                    try {
                        player.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (e: Exception) { /* safe */ }

                    val fis = FileInputStream(localFile)
                    player.setDataSource(fis.fd)
                    player.prepare()
                    fis.close()

                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Local file MediaPlayer error: what=$what, extra=$extra")
                        true
                    }
                    Log.d(TAG, "Successfully created MediaPlayer from local file: ${localFile.name}")
                    return player
                } catch (e: Exception) {
                    Log.w(TAG, "Local file play failed for $downloadedFileName: ${e.message}")
                    try { localFile.delete() } catch (ex: Exception) {}
                }
            }
        }

        // 3. Try raw resource via MediaPlayer.create
        try {
            val player = MediaPlayer.create(appContext, rawResId, audioAttributes, 0)
            if (player != null) {
                try {
                    player.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {}
                player.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Raw res MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                return player
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer.create with attrs failed: ${e.message}")
        }

        // 4. Try standard MediaPlayer.create
        try {
            val player = MediaPlayer.create(appContext, rawResId)
            if (player != null) {
                player.setOnErrorListener { _, _, _ -> true }
                return player
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer.create standard failed: ${e.message}")
        }

        return null
    }
}
