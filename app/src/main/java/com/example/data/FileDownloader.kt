package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object FileDownloader {

    private const val TAG = "FileDownloader"

    // Exact URLs specified by user
    const val URL_KURAN_PDF = "https://dijital.diyanet.gov.tr/File/Download?path=kurani_kerim_bilgisayar_hatli.pdf&id=428"
    const val URL_ILMIHAL_PDF = "https://dn721803.ca.archive.org/0/items/muhtasar-ilmihal-fazilet/Muhtasar_ilmihal_fazilet.pdf"
    const val URL_SIYER_PDF = "https://webdosyasp.diyanet.gov.tr/muftuluk/UserFiles/sakarya/Ilceler/adapazari/UserFiles/Files/ORTAOKULLAR%20%C4%B0%C3%87%C4%B0N%20YARI%C5%9EMA%20K%C4%B0TABINI%20%C4%B0ND%C4%B0R_9a08d198-3723-4cea-8f62-49516eb5180f.pdf"

    const val URL_EZAN = "https://github.com/AlianGamerAS/Islam-Storage/raw/c36e9dbb445bb6d2462cf430a6427776e0dda417/Ezan%20Sesi.mp3"
    const val URL_RINGTONE = "https://github.com/AlianGamerAS/Islam-Storage/raw/refs/heads/main/Ezan%20Sesi.mp3"

    // Raw GitHub direct CDNs (fallback if github.com/raw redirects to HTML)
    const val URL_EZAN_RAW_CDN = "https://raw.githubusercontent.com/AlianGamerAS/Islam-Storage/c36e9dbb445bb6d2462cf430a6427776e0dda417/Ezan%20Sesi.mp3"
    const val URL_RINGTONE_RAW_CDN = "https://raw.githubusercontent.com/AlianGamerAS/Islam-Storage/main/Ezan%20Sesi.mp3"

    // Backwards compatibility aliases
    const val URL_EZAN_1 = URL_EZAN
    const val URL_EZAN_2 = URL_RINGTONE
    const val URL_RINGTONE_GITHUB = URL_RINGTONE

    val okHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()
        }
    }

    /**
     * Checks if a file is a valid PDF by inspecting its magic bytes (%PDF)
     */
    fun isValidPdf(file: File): Boolean {
        if (!file.exists() || file.length() < 500) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(5)
                val read = fis.read(header)
                if (read >= 4) {
                    val str = String(header, 0, 4, Charsets.US_ASCII)
                    str.startsWith("%PDF")
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a file is a valid audio file (not an HTML error page or empty).
     */
    fun isValidAudio(file: File): Boolean {
        if (!file.exists() || file.length() < 1000) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(16)
                val read = fis.read(header)
                if (read >= 4) {
                    val str = String(header, 0, minOf(read, 15), Charsets.US_ASCII).lowercase()
                    // Reject HTML / XML / text error responses
                    if (str.startsWith("<!doc") || str.startsWith("<html") || str.startsWith("{") || str.startsWith("error") || str.startsWith("404")) {
                        return false
                    }
                    true
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a file exists locally and is valid in app internal files storage.
     */
    fun isFileCached(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || file.length() <= 1000) return false
        if (fileName.endsWith(".pdf", ignoreCase = true)) {
            if (!isValidPdf(file)) {
                file.delete()
                return false
            }
        } else if (fileName.endsWith(".mp3", ignoreCase = true)) {
            if (!isValidAudio(file)) {
                file.delete()
                return false
            }
        }
        return true
    }

    /**
     * Gets the local File handle if cached and valid, or null.
     */
    fun getCachedFile(context: Context, fileName: String): File? {
        val file = File(context.filesDir, fileName)
        if (file.exists() && file.length() > 1000) {
            if (fileName.endsWith(".pdf", ignoreCase = true)) {
                if (isValidPdf(file)) return file else {
                    file.delete()
                    return null
                }
            } else if (fileName.endsWith(".mp3", ignoreCase = true)) {
                if (isValidAudio(file)) return file else {
                    file.delete()
                    return null
                }
            }
            return file
        }
        return null
    }

    private fun attemptDownload(url: String, targetFile: File, tempFile: File, fileName: String, onProgress: (Float) -> Unit): Boolean {
        try {
            Log.d(TAG, "Attempting binary download: $url -> ${targetFile.name}")
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "*/*")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val body = response.body!!
                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists() && tempFile.length() > 500) {
                    if (fileName.endsWith(".pdf", ignoreCase = true) && !isValidPdf(tempFile)) {
                        Log.w(TAG, "Downloaded file $fileName is not a valid PDF header (%PDF), size=${tempFile.length()}")
                        tempFile.delete()
                        return false
                    }
                    if (fileName.endsWith(".mp3", ignoreCase = true) && !isValidAudio(tempFile)) {
                        Log.w(TAG, "Downloaded file $fileName is not a valid audio, size=${tempFile.length()}")
                        tempFile.delete()
                        return false
                    }
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = tempFile.renameTo(targetFile)
                    if (renamed || targetFile.exists()) {
                        onProgress(1f)
                        Log.d(TAG, "Download succeeded: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download attempt failed for $url: ${e.message}")
        }
        return false
    }

    /**
     * Downloads file from URL into context.filesDir with progress reporting.
     */
    suspend fun downloadOrGetFile(
        context: Context,
        url: String,
        fileName: String,
        assetFallback: String? = null,
        rawResFallback: Int? = null,
        forceDownload: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, fileName)

        // Check if already valid in cache
        if (!forceDownload && isFileCached(context, fileName)) {
            onProgress(1f)
            return@withContext Result.success(targetFile)
        }

        val tempFile = File(context.cacheDir, "${fileName}_${System.currentTimeMillis()}.tmp")

        try {
            // Attempt 1: primary URL
            if (attemptDownload(url, targetFile, tempFile, fileName, onProgress)) {
                return@withContext Result.success(targetFile)
            }

            // Attempt 2: fallback raw CDN for GitHub audio URLs if primary failed
            if (fileName == "ezan.mp3" && attemptDownload(URL_EZAN_RAW_CDN, targetFile, tempFile, fileName, onProgress)) {
                return@withContext Result.success(targetFile)
            }
            if (fileName == "ringtone.mp3" && attemptDownload(URL_RINGTONE_RAW_CDN, targetFile, tempFile, fileName, onProgress)) {
                return@withContext Result.success(targetFile)
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        // Fallback: check raw resource for audio
        if (rawResFallback != null) {
            try {
                context.resources.openRawResource(rawResFallback).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 1000) {
                    onProgress(1f)
                    return@withContext Result.success(targetFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback raw resource failed for $fileName: ${e.message}")
            }
        }

        Result.failure(Exception("Dosya indirilemedi. Lütfen internet bağlantınızı kontrol edip 'Tekrar Dene' butonuna dokunun."))
    }
}
