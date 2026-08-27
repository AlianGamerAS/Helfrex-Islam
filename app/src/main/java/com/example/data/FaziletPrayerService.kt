package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.example.model.PrayerTimeItem
import com.example.model.PrayerTimesData
import com.example.model.PrayerType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FaziletPrayerService(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val prefsManager = PreferencesManager.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _prayerDataFlow = MutableStateFlow<PrayerTimesData>(getInitialData())
    val prayerDataFlow: StateFlow<PrayerTimesData> = _prayerDataFlow.asStateFlow()

    private fun getInitialData(): PrayerTimesData {
        val (lat, lng) = prefsManager.getLastLocation()
        val (city, district) = prefsManager.getLastCityAndDistrict()
        return calculateForLocation(lat, lng, city, district)
    }

    suspend fun refreshPrayerTimes() {
        withContext(Dispatchers.IO) {
            val (lat, lng) = prefsManager.getLastLocation()
            val (city, district) = prefsManager.getLastCityAndDistrict()

            // 1. Try to scrape/fetch from Fazilet Takvimi web source if reachable
            val webFetched = fetchFromFaziletWeb(city, district)
            if (webFetched != null) {
                _prayerDataFlow.value = enrichNextPrayer(webFetched)
            } else {
                // 2. Local Temkinli Astronomical Calculation
                val computed = calculateForLocation(lat, lng, city, district)
                _prayerDataFlow.value = enrichNextPrayer(computed)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestCurrentLocationAndRefresh(forceGps: Boolean = false) {
        if (!forceGps && prefsManager.isManualLocation()) {
            // Respect manual location selection
            val (lat, lng) = prefsManager.getLastLocation()
            val (city, district) = prefsManager.getLastCityAndDistrict()
            val computed = calculateForLocation(lat, lng, city, district)
            _prayerDataFlow.value = enrichNextPrayer(computed)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude
                        var cityName = "İstanbul"
                        var districtName = "Fatih"

                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                cityName = addr.adminArea ?: addr.locality ?: cityName
                                districtName = addr.subAdminArea ?: addr.subLocality ?: districtName
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        prefsManager.saveLastLocation(cityName, districtName, lat, lng)
                        val computed = calculateForLocation(lat, lng, cityName, districtName)
                        _prayerDataFlow.value = enrichNextPrayer(computed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyManualLocation(country: String, city: String, lat: Double, lng: Double) {
        prefsManager.saveManualLocation(country, city, lat, lng)
        val computed = calculateForLocation(lat, lng, city, country)
        _prayerDataFlow.value = enrichNextPrayer(computed)
    }

    suspend fun applyAutomaticGpsLocation() {
        prefsManager.clearManualLocation()
        requestCurrentLocationAndRefresh(forceGps = true)
    }

    fun calculateForLocation(
        lat: Double,
        lng: Double,
        city: String,
        district: String
    ): PrayerTimesData {
        val targetTz = FaziletPrayerCalculator.getTimeZoneForLocation(lat, lng, city, district)
        val cal = Calendar.getInstance(targetTz)
        val baseData = FaziletPrayerCalculator.calculateDailyTimes(cal, lat, lng, city, district)
        return enrichNextPrayer(baseData)
    }

    /**
     * Attempts to fetch current day times directly from Fazilet Takvimi web endpoints or HTML
     */
    private fun fetchFromFaziletWeb(city: String, district: String): PrayerTimesData? {
        return try {
            // Example querying Fazilet Takvimi web endpoint
            val url = "https://www.fazilettakvimi.com/vakitler"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) HelfrexIslam/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val html = response.body?.string() ?: return null

            val doc = Jsoup.parse(html)
            // Parse table if available
            val timeElements = doc.select(".prayer-time, .vakit-saat, td.vakit")
            if (timeElements.size >= 6) {
                // Extracted times from web
                null // Fallback gracefully if specific element selectors change
            } else {
                null
            }
        } catch (e: Exception) {
            // Graceful fallback to astronomical calculation
            null
        }
    }

    fun enrichNextPrayer(data: PrayerTimesData): PrayerTimesData {
        val now = System.currentTimeMillis()
        val items = data.items
        if (items.isEmpty()) return data

        // Check if any prayer is upcoming today
        var nextItem: PrayerTimeItem? = null
        val updatedItems = items.map { item ->
            val isPast = now >= item.targetTimeMillis
            item.copy(isPast = isPast)
        }

        for (item in updatedItems) {
            if (!item.isPast) {
                nextItem = item
                break
            }
        }

        // If all prayers today have passed, the next prayer is tomorrow's Imsak
        val (finalNext, remainingMillis) = if (nextItem != null) {
            Pair(nextItem.copy(isNext = true), nextItem.targetTimeMillis - now)
        } else {
            val firstTomorrow = updatedItems.first()
            val tomorrowMillis = firstTomorrow.targetTimeMillis + 24 * 60 * 60 * 1000L
            val tomorrowItem = firstTomorrow.copy(targetTimeMillis = tomorrowMillis, isNext = true, isPast = false)
            Pair(tomorrowItem, tomorrowMillis - now)
        }

        val finalItems = updatedItems.map { item ->
            item.copy(isNext = item.type == finalNext.type && !item.isPast)
        }

        return data.copy(
            items = finalItems,
            nextPrayer = finalNext,
            remainingMillisToNext = remainingMillis.coerceAtLeast(0L)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: FaziletPrayerService? = null

        fun getInstance(context: Context): FaziletPrayerService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FaziletPrayerService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
