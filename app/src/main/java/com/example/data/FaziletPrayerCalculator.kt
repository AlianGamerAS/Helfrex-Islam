package com.example.data

import com.example.model.PrayerTimeItem
import com.example.model.PrayerTimesData
import com.example.model.PrayerType
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical Prayer Time Calculation Engine calibrated to Fazilet Takvimi's
 * classical Temkin (prudential buffer) and astronomical standards.
 */
object FaziletPrayerCalculator {

    // Temkin parameters calibrated strictly to Fazilet Takvimi
    private const val TEMKIN_IMSAK_MIN = -7  // Temkin for Imsak (subtracted to ensure safety before dawn)
    private const val TEMKIN_GUNES_MIN = -4  // Temkin for sunrise
    private const val TEMKIN_OGLE_MIN = 7    // Temkin for Dhuhr (added after true noon)
    private const val TEMKIN_IKINDI_MIN = 5  // Temkin for Asr
    private const val TEMKIN_AKSAM_MIN = 7   // Temkin for Maghrib
    private const val TEMKIN_YATSI_MIN = 5   // Temkin for Isha

    // Solar depression angles for Fazilet
    private const val FAZR_ANGLE = 19.0      // 19.0 degrees for Imsak (Fazilet standard)
    private const val ISHA_ANGLE = 17.0      // 17.0 degrees for Yatsi (Fazilet standard)

    fun calculateDailyTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        cityName: String,
        districtName: String = ""
    ): PrayerTimesData {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Check if the selected location is in Turkey
        val isTurkey = (latitude in 35.5..42.5 && longitude in 25.5..45.0) ||
                CityDatabase.turkeyCities.any { it.name.equals(cityName, ignoreCase = true) } ||
                districtName.contains("Türkiye", ignoreCase = true) ||
                districtName.contains("Turkey", ignoreCase = true)

        // Check if the location is in Azerbaijan
        val isAzerbaijan = (latitude in 38.0..42.2 && longitude in 44.5..51.0) ||
                CityDatabase.azerbaijanCities.any { it.name.equals(cityName, ignoreCase = true) } ||
                districtName.contains("Azerbaycan", ignoreCase = true) ||
                districtName.contains("Azerbaijan", ignoreCase = true)

        // Target timezone offset in hours (Turkey: UTC+3, Azerbaijan: UTC+4)
        val timeZone: Double = when {
            isTurkey -> 3.0
            isAzerbaijan -> 4.0
            // Saudi Arabia (Mecca, Medina)
            (latitude in 16.0..32.0 && longitude in 34.0..55.0) -> 3.0
            // General accurate geographical timezone from longitude
            else -> Math.round(longitude / 15.0).toDouble()
        }

        val targetTimeZone = when {
            isTurkey -> TimeZone.getTimeZone("Europe/Istanbul")
            isAzerbaijan -> TimeZone.getTimeZone("Asia/Baku")
            else -> {
                val offsetHours = timeZone.toInt()
                val sign = if (offsetHours >= 0) "+" else "-"
                TimeZone.getTimeZone(String.format(java.util.Locale.US, "GMT%s%02d:00", sign, Math.abs(offsetHours)))
            }
        }

        // --- High-Precision NOAA Solar Calculations ---
        val a = floor((14 - month) / 12.0)
        val y1 = year + 4800 - a
        val m1 = month + 12 * a - 3
        val jd = day + floor((153 * m1 + 2) / 5.0) + 365 * y1 + floor(y1 / 4.0) - floor(y1 / 100.0) + floor(y1 / 400.0) - 32045.0 - 0.5 + (12.0 - timeZone) / 24.0

        val t = (jd - 2451545.0) / 36525.0

        // Geometric Mean Longitude of Sun (deg)
        var l0 = 280.46646 + t * (36000.76983 + 0.0003032 * t)
        l0 = (l0 % 360.0 + 360.0) % 360.0

        // Mean Anomaly of Sun (deg)
        var mAnomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t)
        mAnomaly = (mAnomaly % 360.0 + 360.0) % 360.0
        val mRad = Math.toRadians(mAnomaly)

        // Eccentricity of Earth orbit
        val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

        // Sun Equation of Center
        val c = sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2.0 * mRad) * (0.019993 - 0.000101 * t) +
                sin(3.0 * mRad) * 0.000289

        // Sun True Longitude
        val sunTrueLong = l0 + c

        // Sun Apparent Longitude
        val omega = 125.04 - 1934.136 * t
        val lambda = sunTrueLong - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
        val lambdaRad = Math.toRadians(lambda)

        // Obliquity of Ecliptic
        val epsilon0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val epsilon = epsilon0 + 0.00256 * cos(Math.toRadians(omega))
        val epsRad = Math.toRadians(epsilon)

        // Sun Declination
        val sinDecl = sin(epsRad) * sin(lambdaRad)
        val decl = asin(sinDecl)

        // Equation of Time (in minutes)
        val yTan = tan(epsRad / 2.0) * tan(epsRad / 2.0)
        val l0Rad = Math.toRadians(l0)
        val eqTime = 4.0 * Math.toDegrees(
            yTan * sin(2.0 * l0Rad) -
            2.0 * eccentricity * sin(mRad) +
            4.0 * eccentricity * yTan * sin(mRad) * cos(2.0 * l0Rad) -
            0.5 * yTan * yTan * sin(4.0 * l0Rad) -
            1.25 * eccentricity * eccentricity * sin(2.0 * mRad)
        )

        val latRad = Math.toRadians(latitude)

        // Solar noon time in hours (Local Time)
        val solarNoon = (720.0 - 4.0 * longitude - eqTime) / 60.0 + timeZone

        // Hour angles
        fun hourAngle(angle: Double): Double {
            val cosHA = (sin(Math.toRadians(angle)) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
            if (cosHA > 1.0) return 0.0
            if (cosHA < -1.0) return 180.0
            return Math.toDegrees(acos(cosHA))
        }

        // Sunrise & Sunset angle (standard refraction + semi-diameter: -0.833 degrees)
        val haGunes = hourAngle(-0.833)
        // Imsak angle (19.0 degrees below horizon -> -19.0)
        val haImsak = hourAngle(-FAZR_ANGLE)
        // Yatsi angle (17.0 degrees below horizon -> -17.0)
        val haYatsi = hourAngle(-ISHA_ANGLE)

        // Asr hour angle (Standard / Shafi'i shadow ratio = 1)
        val asrAltRad = atan2(1.0, 1.0 + tan(Math.abs(latRad - decl)))
        val cosHaAsr = (sin(asrAltRad) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
        val haAsr = if (cosHaAsr in -1.0..1.0) Math.toDegrees(acos(cosHaAsr)) else 45.0

        // Base times in hours
        val rawImsak = solarNoon - (haImsak / 15.0)
        val rawGunes = solarNoon - (haGunes / 15.0)
        val rawOgle = solarNoon
        val rawIkindi = solarNoon + (haAsr / 15.0)
        val rawAksam = solarNoon + (haGunes / 15.0)
        val rawYatsi = solarNoon + (haYatsi / 15.0)

        // Apply Fazilet Temkin adjustments (in minutes)
        val imsakMinutes = Math.round(rawImsak * 60.0).toInt() + TEMKIN_IMSAK_MIN
        val gunesMinutes = Math.round(rawGunes * 60.0).toInt() + TEMKIN_GUNES_MIN
        // In Fazilet Takvimi calendar, Sabah prayer is 20 minutes after Imsak
        val sabahMinutes = imsakMinutes + 20
        val ogleMinutes = Math.round(rawOgle * 60.0).toInt() + TEMKIN_OGLE_MIN
        val ikindiMinutes = Math.round(rawIkindi * 60.0).toInt() + TEMKIN_IKINDI_MIN
        val aksamMinutes = Math.round(rawAksam * 60.0).toInt() + TEMKIN_AKSAM_MIN
        val yatsiMinutes = Math.round(rawYatsi * 60.0).toInt() + TEMKIN_YATSI_MIN

        fun minutesToTimeStr(totalMinutes: Int): String {
            val normalized = (totalMinutes % 1440 + 1440) % 1440
            val h = normalized / 60
            val m = normalized % 60
            return String.format("%02d:%02d", h, m)
        }

        fun timeStrToMillis(totalMinutes: Int): Long {
            val cal = Calendar.getInstance(targetTimeZone).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                val normalized = (totalMinutes % 1440 + 1440) % 1440
                set(Calendar.HOUR_OF_DAY, normalized / 60)
                set(Calendar.MINUTE, normalized % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        val items = listOf(
            PrayerTimeItem(PrayerType.IMSAK, minutesToTimeStr(imsakMinutes), timeStrToMillis(imsakMinutes)),
            PrayerTimeItem(PrayerType.SABAH, minutesToTimeStr(sabahMinutes), timeStrToMillis(sabahMinutes)),
            PrayerTimeItem(PrayerType.GUNES, minutesToTimeStr(gunesMinutes), timeStrToMillis(gunesMinutes)),
            PrayerTimeItem(PrayerType.OGLE, minutesToTimeStr(ogleMinutes), timeStrToMillis(ogleMinutes)),
            PrayerTimeItem(PrayerType.IKINDI, minutesToTimeStr(ikindiMinutes), timeStrToMillis(ikindiMinutes)),
            PrayerTimeItem(PrayerType.AKSAM, minutesToTimeStr(aksamMinutes), timeStrToMillis(aksamMinutes)),
            PrayerTimeItem(PrayerType.YATSI, minutesToTimeStr(yatsiMinutes), timeStrToMillis(yatsiMinutes))
        )

        val gregorianDateStr = String.format("%02d.%02d.%04d", day, month, year)
        val hijriDateStr = calculateHijriDate(calendar)

        return PrayerTimesData(
            dateGregorian = gregorianDateStr,
            dateHijri = hijriDateStr,
            cityName = cityName,
            districtName = districtName,
            items = items
        )
    }

    /**
     * Approximate Islamic Hijri Calendar calculator based on Umm al-Qura / Tabular method
     */
    fun calculateHijriDate(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Day Calculation
        val a = floor((14 - m) / 12.0).toInt()
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        val jd = d + floor((153 * mm + 2) / 5.0).toInt() + 365 * yy + floor(yy / 4.0).toInt() -
                floor(yy / 100.0).toInt() + floor(yy / 400.0).toInt() - 32045

        // Hijri algorithm from JD
        val l = jd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0).toInt()
        val l1 = l - 10631 * n + 354
        val j = (floor((10985 - l1) / 5316.0) * floor((50 * l1) / 17719.0) +
                floor(l1 / 5670.0) * floor((43 * l1) / 15238.0)).toInt()
        val l2 = l1 - floor((30 - j) / 15.0).toInt() * floor((17719 * j) / 50.0).toInt() -
                floor(j / 16.0).toInt() * floor((15238 * j) / 43.0).toInt() + 29
        val hijriMonth = floor((24 * l2) / 709.0).toInt()
        val hijriDay = l2 - floor((709 * hijriMonth) / 24.0).toInt()
        val hijriYear = 30 * n + j - 30

        val hijriMonthsTr = listOf(
            "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
            "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
            "Ramazan", "Şevval", "Zilkade", "Zilhicce"
        )

        val monthName = if (hijriMonth in 1..12) hijriMonthsTr[hijriMonth - 1] else "Ramazan"
        return "$hijriDay $monthName $hijriYear"
    }
}
