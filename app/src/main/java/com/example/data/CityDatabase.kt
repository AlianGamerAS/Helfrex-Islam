package com.example.data

data class OfflineCity(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

data class OfflineCountry(
    val nameTr: String,
    val nameEn: String,
    val code: String,
    val cities: List<OfflineCity>
)

object CityDatabase {

    val turkeyCities = listOf(
        OfflineCity("Adana", "Türkiye", 37.0000, 35.3213),
        OfflineCity("Adıyaman", "Türkiye", 37.7648, 38.2786),
        OfflineCity("Afyonkarahisar", "Türkiye", 38.7507, 30.5567),
        OfflineCity("Ağrı", "Türkiye", 39.7191, 43.0503),
        OfflineCity("Aksaray", "Türkiye", 38.3687, 34.0370),
        OfflineCity("Amasya", "Türkiye", 40.6534, 35.8331),
        OfflineCity("Ankara", "Türkiye", 39.9334, 32.8597),
        OfflineCity("Antalya", "Türkiye", 36.8969, 30.7133),
        OfflineCity("Ardahan", "Türkiye", 41.1105, 42.7022),
        OfflineCity("Artvin", "Türkiye", 41.1828, 41.8183),
        OfflineCity("Aydın", "Türkiye", 37.8560, 27.8416),
        OfflineCity("Balıkesir", "Türkiye", 39.6484, 27.8826),
        OfflineCity("Bartın", "Türkiye", 41.6344, 32.3375),
        OfflineCity("Batman", "Türkiye", 37.8812, 41.1293),
        OfflineCity("Bayburt", "Türkiye", 40.2552, 40.2249),
        OfflineCity("Bilecik", "Türkiye", 40.1451, 29.9799),
        OfflineCity("Bingöl", "Türkiye", 38.8854, 40.4983),
        OfflineCity("Bitlis", "Türkiye", 38.4006, 42.1095),
        OfflineCity("Bolu", "Türkiye", 40.7358, 31.6061),
        OfflineCity("Burdur", "Türkiye", 37.7203, 30.2908),
        OfflineCity("Bursa", "Türkiye", 40.1885, 29.0610),
        OfflineCity("Çanakkale", "Türkiye", 40.1553, 26.4142),
        OfflineCity("Çankırı", "Türkiye", 40.6013, 33.6134),
        OfflineCity("Çorum", "Türkiye", 40.5506, 34.9556),
        OfflineCity("Denizli", "Türkiye", 37.7765, 29.0864),
        OfflineCity("Diyarbakır", "Türkiye", 37.9144, 40.2306),
        OfflineCity("Düzce", "Türkiye", 40.8438, 31.1565),
        OfflineCity("Edirne", "Türkiye", 41.6772, 26.5557),
        OfflineCity("Elazığ", "Türkiye", 38.6810, 39.2264),
        OfflineCity("Erzincan", "Türkiye", 39.7500, 39.5000),
        OfflineCity("Erzurum", "Türkiye", 39.9055, 41.2658),
        OfflineCity("Eskişehir", "Türkiye", 39.7767, 30.5206),
        OfflineCity("Gaziantep", "Türkiye", 37.0662, 37.3833),
        OfflineCity("Giresun", "Türkiye", 40.9128, 38.3895),
        OfflineCity("Gümüşhane", "Türkiye", 40.4600, 39.4814),
        OfflineCity("Hakkari", "Türkiye", 37.5833, 43.7333),
        OfflineCity("Hatay (Antakya)", "Türkiye", 36.2023, 36.1603),
        OfflineCity("Iğdır", "Türkiye", 39.9196, 44.0454),
        OfflineCity("Isparta", "Türkiye", 37.7648, 30.5566),
        OfflineCity("İstanbul", "Türkiye", 41.0082, 28.9784),
        OfflineCity("İzmir", "Türkiye", 38.4192, 27.1287),
        OfflineCity("Kahramanmaraş", "Türkiye", 37.5858, 36.9371),
        OfflineCity("Karabük", "Türkiye", 41.2061, 32.6204),
        OfflineCity("Karaman", "Türkiye", 37.1759, 33.2287),
        OfflineCity("Kars", "Türkiye", 40.6167, 43.1000),
        OfflineCity("Kastamonu", "Türkiye", 41.3887, 33.7827),
        OfflineCity("Kayseri", "Türkiye", 38.7312, 35.4787),
        OfflineCity("Kırıkkale", "Türkiye", 39.8468, 33.5153),
        OfflineCity("Kırklareli", "Türkiye", 41.7333, 27.2167),
        OfflineCity("Kırşehir", "Türkiye", 39.1425, 34.1709),
        OfflineCity("Kilis", "Türkiye", 36.7184, 37.1212),
        OfflineCity("Kocaeli (İzmit)", "Türkiye", 40.8533, 29.8815),
        OfflineCity("Konya", "Türkiye", 37.8667, 32.4833),
        OfflineCity("Kütahya", "Türkiye", 39.4167, 29.9833),
        OfflineCity("Malatya", "Türkiye", 38.3552, 38.3095),
        OfflineCity("Manisa", "Türkiye", 38.6191, 27.4289),
        OfflineCity("Mardin", "Türkiye", 37.3212, 40.7245),
        OfflineCity("Mersin", "Türkiye", 36.8000, 34.6333),
        OfflineCity("Muğla", "Türkiye", 37.2153, 28.3636),
        OfflineCity("Muş", "Türkiye", 38.7432, 41.5064),
        OfflineCity("Nevşehir", "Türkiye", 38.6244, 34.7144),
        OfflineCity("Niğde", "Türkiye", 37.9667, 34.6833),
        OfflineCity("Ordu", "Türkiye", 40.9839, 37.8764),
        OfflineCity("Osmaniye", "Türkiye", 37.0742, 36.2478),
        OfflineCity("Rize", "Türkiye", 41.0201, 40.5234),
        OfflineCity("Sakarya (Adapazarı)", "Türkiye", 40.7731, 30.4033),
        OfflineCity("Samsun", "Türkiye", 41.2928, 36.3313),
        OfflineCity("Siirt", "Türkiye", 37.9333, 41.9500),
        OfflineCity("Sinop", "Türkiye", 42.0231, 35.1531),
        OfflineCity("Sivas", "Türkiye", 39.7477, 37.0179),
        OfflineCity("Şanlıurfa", "Türkiye", 37.1591, 38.7969),
        OfflineCity("Şırnak", "Türkiye", 37.5164, 42.4594),
        OfflineCity("Tekirdağ", "Türkiye", 40.9833, 27.5167),
        OfflineCity("Tokat", "Türkiye", 40.3167, 36.5500),
        OfflineCity("Trabzon", "Türkiye", 41.0015, 39.7178),
        OfflineCity("Tunceli", "Türkiye", 39.1079, 39.5401),
        OfflineCity("Uşak", "Türkiye", 38.6823, 29.4082),
        OfflineCity("Van", "Türkiye", 38.4891, 43.4089),
        OfflineCity("Yalova", "Türkiye", 40.6500, 29.2667),
        OfflineCity("Yozgat", "Türkiye", 39.8181, 34.8147),
        OfflineCity("Zonguldak", "Türkiye", 41.4564, 31.7987)
    )

    val germanyCities = listOf(
        OfflineCity("Berlin", "Almanya", 52.5200, 13.4050),
        OfflineCity("Köln (Cologne)", "Almanya", 50.9375, 6.9603),
        OfflineCity("Frankfurt", "Almanya", 50.1109, 8.6821),
        OfflineCity("Münih (Munich)", "Almanya", 48.1351, 11.5820),
        OfflineCity("Hamburg", "Almanya", 53.5511, 9.9937),
        OfflineCity("Stuttgart", "Almanya", 48.7758, 9.1829),
        OfflineCity("Düsseldorf", "Almanya", 51.2277, 6.7735),
        OfflineCity("Dortmund", "Almanya", 51.5136, 7.4653),
        OfflineCity("Essen", "Almanya", 51.4556, 7.0116),
        OfflineCity("Bremen", "Almanya", 53.0793, 8.8017),
        OfflineCity("Hannover", "Almanya", 52.3759, 9.7320),
        OfflineCity("Nürnberg", "Almanya", 49.4521, 11.0767)
    )

    val azerbaijanCities = listOf(
        OfflineCity("Bakü (Baku)", "Azerbaycan", 40.4093, 49.8671),
        OfflineCity("Gence", "Azerbaycan", 40.6828, 46.3606),
        OfflineCity("Göygöl", "Azerbaycan", 40.5878, 46.3189),
        OfflineCity("Sumgayıt", "Azerbaycan", 40.5855, 49.6317),
        OfflineCity("Nahçıvan", "Azerbaycan", 39.2089, 45.4122),
        OfflineCity("Şeki", "Azerbaycan", 41.1919, 47.1706),
        OfflineCity("Şuşa", "Azerbaycan", 39.7537, 46.7465),
        OfflineCity("Mingeçevir", "Azerbaycan", 40.7640, 47.0595),
        OfflineCity("Lenkeran", "Azerbaycan", 38.7543, 48.8506),
        OfflineCity("Şirvan", "Azerbaycan", 39.9378, 48.9290),
        OfflineCity("Hırdalan", "Azerbaycan", 40.4481, 49.7550),
        OfflineCity("Yevlah", "Azerbaycan", 40.6172, 47.1500),
        OfflineCity("Haçmaz", "Azerbaycan", 41.4636, 48.8061),
        OfflineCity("Kuba", "Azerbaycan", 41.3611, 48.5131),
        OfflineCity("Zakatala", "Azerbaycan", 41.6336, 46.6433),
        OfflineCity("Gabala (Gebele)", "Azerbaycan", 40.9982, 47.8700)
    )

    val saudiCities = listOf(
        OfflineCity("Mekke-i Mükerreme", "Suudi Arabistan", 21.3891, 39.8579),
        OfflineCity("Medine-i Münevvere", "Suudi Arabistan", 24.5247, 39.5692),
        OfflineCity("Riyad", "Suudi Arabistan", 24.7136, 46.6753),
        OfflineCity("Cidde", "Suudi Arabistan", 21.4858, 39.1925),
        OfflineCity("Dammam", "Suudi Arabistan", 26.4207, 50.0888)
    )

    val franceCities = listOf(
        OfflineCity("Paris", "Fransa", 48.8566, 2.3522),
        OfflineCity("Marsilya", "Fransa", 43.2965, 5.3698),
        OfflineCity("Lyon", "Fransa", 45.7640, 4.8357),
        OfflineCity("Strasbourg", "Fransa", 48.5734, 7.7521),
        OfflineCity("Toulouse", "Fransa", 43.6047, 1.4442),
        OfflineCity("Nice", "Fransa", 43.7102, 7.2620)
    )

    val ukCities = listOf(
        OfflineCity("Londra", "Birleşik Krallık", 51.5074, -0.1278),
        OfflineCity("Birmingham", "Birleşik Krallık", 52.4862, -1.8904),
        OfflineCity("Manchester", "Birleşik Krallık", 53.4808, -2.2426),
        OfflineCity("Leeds", "Birleşik Krallık", 53.8008, -1.5491),
        OfflineCity("Glasgow", "Birleşik Krallık", 55.8642, -4.2518)
    )

    val netherlandsCities = listOf(
        OfflineCity("Amsterdam", "Hollanda", 52.3676, 4.9041),
        OfflineCity("Rotterdam", "Hollanda", 51.9244, 4.4777),
        OfflineCity("Lahey (Den Haag)", "Hollanda", 52.0705, 4.3007),
        OfflineCity("Utrecht", "Hollanda", 52.0907, 5.1214),
        OfflineCity("Eindhoven", "Hollanda", 51.4416, 5.4697)
    )

    val austriaCities = listOf(
        OfflineCity("Viyana (Wien)", "Avusturya", 48.2082, 16.3738),
        OfflineCity("Graz", "Avusturya", 47.0707, 15.4395),
        OfflineCity("Linz", "Avusturya", 48.3069, 14.2858),
        OfflineCity("Salzburg", "Avusturya", 47.8095, 13.0550),
        OfflineCity("Innsbruck", "Avusturya", 47.2692, 11.4041)
    )

    val belgiumCities = listOf(
        OfflineCity("Brüksel", "Belçika", 50.8503, 4.3517),
        OfflineCity("Anvers (Antwerpen)", "Belçika", 51.2194, 4.4025),
        OfflineCity("Gent", "Belçika", 51.0543, 3.7174),
        OfflineCity("Liège", "Belçika", 50.6326, 5.5797)
    )

    val switzerlandCities = listOf(
        OfflineCity("Zürih", "İsviçre", 47.3769, 8.5417),
        OfflineCity("Cenevre", "İsviçre", 46.2044, 6.1432),
        OfflineCity("Basel", "İsviçre", 47.5596, 7.5886),
        OfflineCity("Bern", "İsviçre", 46.9480, 7.4474)
    )

    val usaCities = listOf(
        OfflineCity("New York", "ABD", 40.7128, -74.0060),
        OfflineCity("Los Angeles", "ABD", 34.0522, -118.2437),
        OfflineCity("Chicago", "ABD", 41.8781, -87.6298),
        OfflineCity("Houston", "ABD", 29.7604, -95.3698),
        OfflineCity("Dallas", "ABD", 32.7767, -96.7970),
        OfflineCity("Washington D.C.", "ABD", 38.9072, -77.0369),
        OfflineCity("San Francisco", "ABD", 37.7749, -122.4194),
        OfflineCity("Boston", "ABD", 42.3601, -71.0589),
        OfflineCity("Miami", "ABD", 25.7617, -80.1918)
    )

    val otherCities = listOf(
        OfflineCity("Saraybosna (Sarajevo)", "Bosna-Hersek", 43.8563, 18.4131),
        OfflineCity("Mostar", "Bosna-Hersek", 43.3438, 17.8078),
        OfflineCity("Üsküp (Skopje)", "Kuzey Makedonya", 41.9981, 21.4254),
        OfflineCity("Prizren", "Kosova", 42.2153, 20.7415),
        OfflineCity("Tiran (Tirana)", "Arnavutluk", 41.3275, 19.8187),
        OfflineCity("Gümülcine (Komotini)", "Yunanistan", 41.1192, 25.4057),
        OfflineCity("İskeçe (Xanthi)", "Yunanistan", 41.1349, 24.8880),
        OfflineCity("Lefkoşa", "KKTC", 35.1856, 33.3823),
        OfflineCity("Gazimağusa", "KKTC", 35.1250, 33.9417),
        OfflineCity("Girne", "KKTC", 35.3382, 33.3199),
        OfflineCity("Astana", "Kazakistan", 51.1694, 71.4491),
        OfflineCity("Almatı", "Kazakistan", 43.2220, 76.8512),
        OfflineCity("Taşkent", "Özbekistan", 41.2995, 69.2401),
        OfflineCity("Semerkand", "Özbekistan", 39.6270, 66.9750),
        OfflineCity("Buhara", "Özbekistan", 39.7747, 64.4286),
        OfflineCity("Bişkek", "Kırgızistan", 42.8746, 74.5698),
        OfflineCity("Aşkabat", "Türkmenistan", 37.9601, 58.3261),
        OfflineCity("Kudüs (Jerusalem)", "Filistin", 31.7683, 35.2137),
        OfflineCity("Gazze", "Filistin", 31.5017, 34.4668),
        OfflineCity("Kahire (Cairo)", "Mısır", 30.0444, 31.2357),
        OfflineCity("Doha", "Katar", 25.2854, 51.5310),
        OfflineCity("Dubai", "BAE", 25.2048, 55.2708),
        OfflineCity("Ebu Dabi", "BAE", 24.4539, 54.3773),
        OfflineCity("Kuveyt", "Kuveyt", 29.3759, 47.9774),
        OfflineCity("Muskat", "Umman", 23.5880, 58.3829),
        OfflineCity("Amman", "Ürdün", 31.9454, 35.9284),
        OfflineCity("Beyrut", "Lübnan", 33.8938, 35.5018),
        OfflineCity("İslamabad", "Pakistan", 33.6844, 73.0479),
        OfflineCity("Karaçi", "Pakistan", 24.8607, 67.0011),
        OfflineCity("Lahor", "Pakistan", 31.5204, 74.3587),
        OfflineCity("Cakarta", "Endonezya", -6.2088, 106.8456),
        OfflineCity("Kuala Lumpur", "Malezya", 3.1390, 101.6869),
        OfflineCity("Tokyo", "Japonya", 35.6762, 139.6503),
        OfflineCity("Seul", "Güney Kore", 37.5665, 126.9780),
        OfflineCity("Moskova", "Rusya", 55.7558, 37.6173),
        OfflineCity("Kazan", "Rusya / Tataristan", 55.8304, 49.0661),
        OfflineCity("Grozni", "Rusya / Çeçenistan", 43.3169, 45.6985),
        OfflineCity("Toronto", "Kanada", 43.6532, -79.3832),
        OfflineCity("Sidney", "Avustralya", -33.8688, 151.2093),
        OfflineCity("Melbourne", "Avustralya", -37.8136, 144.9631)
    )

    val countries: List<OfflineCountry> = listOf(
        OfflineCountry("Türkiye", "Turkey", "TR", turkeyCities),
        OfflineCountry("Almanya", "Germany", "DE", germanyCities),
        OfflineCountry("Azerbaycan", "Azerbaijan", "AZ", azerbaijanCities),
        OfflineCountry("Suudi Arabistan", "Saudi Arabia", "SA", saudiCities),
        OfflineCountry("Fransa", "France", "FR", franceCities),
        OfflineCountry("Birleşik Krallık", "United Kingdom", "UK", ukCities),
        OfflineCountry("Hollanda", "Netherlands", "NL", netherlandsCities),
        OfflineCountry("Avusturya", "Austria", "AT", austriaCities),
        OfflineCountry("Belçika", "Belgium", "BE", belgiumCities),
        OfflineCountry("İsviçre", "Switzerland", "CH", switzerlandCities),
        OfflineCountry("Amerika Birleşik Devletleri", "United States", "US", usaCities),
        OfflineCountry("Diğer Ülkeler", "Other Countries", "OTHER", otherCities)
    )
}
