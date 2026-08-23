package com.example.model

enum class PrayerType(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val nameRu: String,
    val nameAz: String,
    val order: Int
) {
    IMSAK("imsak", "İmsak", "Imsak / Fajr", "Имсак", "İmsak", 1),
    SABAH("sabah", "Sabah", "Dawn Prayer", "Фаджр", "Sübh", 2),
    GUNES("gunes", "Güneş", "Sunrise", "Восход", "Günəş", 3),
    OGLE("ogle", "Öğle", "Dhuhr", "Зухр", "Zöhr", 4),
    IKINDI("ikindi", "İkindi", "Asr", "Аср", "Əsr", 5),
    AKSAM("aksam", "Akşam", "Maghrib", "Магриб", "Məğrib", 6),
    YATSI("yatsi", "Yatsı", "Isha", "Иша", "İşa", 7);

    fun getName(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> nameTr
        AppLanguage.EN -> nameEn
        AppLanguage.RU -> nameRu
        AppLanguage.AZ -> nameAz
    }
}

data class PrayerTimeItem(
    val type: PrayerType,
    val timeStr: String, // HH:mm format
    val targetTimeMillis: Long,
    val isNext: Boolean = false,
    val isPast: Boolean = false
)

data class PrayerTimesData(
    val dateGregorian: String,
    val dateHijri: String,
    val cityName: String,
    val districtName: String = "",
    val items: List<PrayerTimeItem> = emptyList(),
    val nextPrayer: PrayerTimeItem? = null,
    val remainingMillisToNext: Long = 0L
)

enum class AzanSound(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val nameRu: String,
    val nameAz: String
) {
    SILENT("silent", "Yok", "None / Silent", "Без звука", "Səssiz"),
    RINGTONE("ringtone", "Zil Sesi", "Phone Ringtone", "Мелодия звонка", "Zəng Səsi"),
    AZAN("azan", "Ezan Sesi", "Azan Sound", "Звук Азана", "Azan Səsi");

    fun getName(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> nameTr
        AppLanguage.EN -> nameEn
        AppLanguage.RU -> nameRu
        AppLanguage.AZ -> nameAz
    }
}

enum class AzanDuration(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val nameRu: String,
    val nameAz: String
) {
    SHORT("short", "Kısa", "Short", "Короткий", "Qısa"),
    LONG("long", "Uzun", "Long", "Полный", "Uzun");

    fun getName(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> nameTr
        AppLanguage.EN -> nameEn
        AppLanguage.RU -> nameRu
        AppLanguage.AZ -> nameAz
    }
}

enum class ThemeStyle(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val nameRu: String,
    val nameAz: String
) {
    CLASSIC("classic", "Klasik (Önerilen)", "Classic (Recommended)", "Классический", "Klassik"),
    NEON_BLUE("neon_blue", "Neon (Mavi)", "Neon (Blue)", "Неон (Синий)", "Neon (Mavi)"),
    NEON_PURPLE("neon_purple", "Neon (Mor)", "Neon (Purple)", "Неон (Фиолетовый)", "Neon (Bənövşəyi)"),
    NEON_EMERALD("neon_emerald", "Neon (Zümrüt)", "Neon (Emerald)", "Неон (Изумрудный)", "Neon (Zümrüd)");

    fun getName(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> nameTr
        AppLanguage.EN -> nameEn
        AppLanguage.RU -> nameRu
        AppLanguage.AZ -> nameAz
    }
}

enum class AppLanguage(
    val code: String,
    val displayName: String
) {
    TR("tr", "Türkçe"),
    EN("en", "English"),
    RU("ru", "Русский"),
    AZ("az", "Azərbaycan")
}

data class UserSettings(
    val selectedPrayers: Set<String> = PrayerType.values().map { it.id }.toSet(),
    val azanSound: AzanSound = AzanSound.AZAN,
    val azanDuration: AzanDuration = AzanDuration.LONG,
    val isDarkMode: Boolean = true,
    val themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    val language: AppLanguage = AppLanguage.TR
)
