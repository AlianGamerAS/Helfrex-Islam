package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.util.SoundPlayerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

object OfflineResourcePreloader {
    private const val TAG = "ResourcePreloader"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Preloads and caches audio and PDF resources into local storage
     * so that the application works smoothly.
     */
    fun preloadInitialResources(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                // 1. Download and extract local MP3 files
                SoundPlayerHelper.prepareAudioFile(
                    context = appContext,
                    fileName = "ezan.mp3",
                    remoteUrl = FileDownloader.URL_EZAN,
                    rawFallbackRes = R.raw.ezan1
                )

                SoundPlayerHelper.prepareAudioFile(
                    context = appContext,
                    fileName = "ringtone.mp3",
                    remoteUrl = FileDownloader.URL_RINGTONE,
                    rawFallbackRes = R.raw.ezan2
                )

                Log.d(TAG, "Audio resource preloading completed.")
            } catch (e: Exception) {
                Log.w(TAG, "Audio preloading note: ${e.message}")
            }
        }
    }
}
