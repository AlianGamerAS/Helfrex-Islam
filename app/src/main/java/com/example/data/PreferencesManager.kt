package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage
import com.example.model.AzanDuration
import com.example.model.AzanSound
import com.example.model.PrayerType
import com.example.model.ThemeStyle
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    fun loadSettings(): UserSettings {
        val defaultPrayers = PrayerType.values().map { it.id }.toSet()
        val prayers = prefs.getStringSet(KEY_SELECTED_PRAYERS, defaultPrayers) ?: defaultPrayers
        val soundId = prefs.getString(KEY_AZAN_SOUND, AzanSound.AZAN.id) ?: AzanSound.AZAN.id
        val durationId = prefs.getString(KEY_AZAN_DURATION, AzanDuration.LONG.id) ?: AzanDuration.LONG.id
        val isDark = prefs.getBoolean(KEY_IS_DARK, true)
        val styleId = prefs.getString(KEY_THEME_STYLE, ThemeStyle.CLASSIC.id) ?: ThemeStyle.CLASSIC.id
        val langCode = prefs.getString(KEY_LANG, AppLanguage.TR.code) ?: AppLanguage.TR.code

        return UserSettings(
            selectedPrayers = prayers,
            azanSound = AzanSound.values().firstOrNull { it.id == soundId } ?: AzanSound.AZAN,
            azanDuration = AzanDuration.values().firstOrNull { it.id == durationId } ?: AzanDuration.LONG,
            isDarkMode = isDark,
            themeStyle = ThemeStyle.values().firstOrNull { it.id == styleId } ?: ThemeStyle.CLASSIC,
            language = AppLanguage.values().firstOrNull { it.code == langCode } ?: AppLanguage.TR
        )
    }

    fun updateSelectedPrayer(prayerId: String, isSelected: Boolean) {
        val current = _settingsFlow.value.selectedPrayers.toMutableSet()
        if (isSelected) {
            current.add(prayerId)
        } else {
            // Keep at least one prayer
            if (current.size > 1) {
                current.remove(prayerId)
            }
        }
        prefs.edit().putStringSet(KEY_SELECTED_PRAYERS, current).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedPrayers = current)
    }

    fun setAzanSound(sound: AzanSound) {
        prefs.edit().putString(KEY_AZAN_SOUND, sound.id).apply()
        _settingsFlow.value = _settingsFlow.value.copy(azanSound = sound)
    }

    fun setAzanDuration(duration: AzanDuration) {
        prefs.edit().putString(KEY_AZAN_DURATION, duration.id).apply()
        _settingsFlow.value = _settingsFlow.value.copy(azanDuration = duration)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
        val currentStyle = _settingsFlow.value.themeStyle
        _settingsFlow.value = _settingsFlow.value.copy(isDarkMode = isDark)
        try {
            com.example.util.IconManager.scheduleIconUpdate(context, currentStyle, isDark)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setThemeStyle(style: ThemeStyle) {
        prefs.edit().putString(KEY_THEME_STYLE, style.id).apply()
        val isDark = _settingsFlow.value.isDarkMode
        _settingsFlow.value = _settingsFlow.value.copy(themeStyle = style)
        try {
            com.example.util.IconManager.scheduleIconUpdate(context, style, isDark)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANG, lang.code).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = lang)
    }

    fun saveLastLocation(city: String, district: String, lat: Double, lng: Double) {
        prefs.edit()
            .putString(KEY_LAST_CITY, city)
            .putString(KEY_LAST_DISTRICT, district)
            .putFloat(KEY_LAST_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LNG, lng.toFloat())
            .apply()
    }

    fun saveManualLocation(country: String, city: String, lat: Double, lng: Double) {
        prefs.edit()
            .putBoolean(KEY_IS_MANUAL_LOCATION, true)
            .putString(KEY_MANUAL_COUNTRY, country)
            .putString(KEY_LAST_CITY, city)
            .putString(KEY_LAST_DISTRICT, country)
            .putFloat(KEY_LAST_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LNG, lng.toFloat())
            .apply()
    }

    fun clearManualLocation() {
        prefs.edit()
            .putBoolean(KEY_IS_MANUAL_LOCATION, false)
            .remove(KEY_MANUAL_COUNTRY)
            .apply()
    }

    fun isManualLocation(): Boolean {
        return prefs.getBoolean(KEY_IS_MANUAL_LOCATION, false)
    }

    fun getManualCountry(): String {
        return prefs.getString(KEY_MANUAL_COUNTRY, "Türkiye") ?: "Türkiye"
    }

    fun getLastLocation(): Pair<Double, Double> {
        val lat = prefs.getFloat(KEY_LAST_LAT, 41.0082f).toDouble() // Istanbul default
        val lng = prefs.getFloat(KEY_LAST_LNG, 28.9784f).toDouble()
        return Pair(lat, lng)
    }

    fun getLastCityAndDistrict(): Pair<String, String> {
        val city = prefs.getString(KEY_LAST_CITY, "İstanbul") ?: "İstanbul"
        val district = prefs.getString(KEY_LAST_DISTRICT, "Fatih") ?: "Fatih"
        return Pair(city, district)
    }

    companion object {
        private const val PREF_NAME = "helfrex_islam_prefs"
        private const val KEY_SELECTED_PRAYERS = "pref_selected_prayers"
        private const val KEY_AZAN_SOUND = "pref_azan_sound"
        private const val KEY_AZAN_DURATION = "pref_azan_duration"
        private const val KEY_IS_DARK = "pref_is_dark"
        private const val KEY_THEME_STYLE = "pref_theme_style"
        private const val KEY_LANG = "pref_lang"
        private const val KEY_IS_MANUAL_LOCATION = "pref_is_manual_location"
        private const val KEY_MANUAL_COUNTRY = "pref_manual_country"
        private const val KEY_LAST_CITY = "pref_last_city"
        private const val KEY_LAST_DISTRICT = "pref_last_district"
        private const val KEY_LAST_LAT = "pref_last_lat"
        private const val KEY_LAST_LNG = "pref_last_lng"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
