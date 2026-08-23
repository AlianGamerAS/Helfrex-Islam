package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CityDatabase
import com.example.data.FileDownloader
import com.example.data.OfflineCity
import com.example.data.PreferencesManager
import com.example.model.AppLanguage
import com.example.model.AzanDuration
import com.example.model.AzanSound
import com.example.model.PrayerType
import com.example.model.ThemeStyle
import com.example.model.UserSettings
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsSubScreen {
    MAIN,
    PRAYER_TIMES,
    LOCATION_MANAGEMENT,
    AZAN_SOUND,
    CUSTOMIZE,
    QIBLA_COMPASS,
    LANGUAGE
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onPrayerToggled: (String, Boolean) -> Unit,
    onSoundChanged: (AzanSound) -> Unit,
    onDurationChanged: (AzanDuration) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onThemeStyleChanged: (ThemeStyle) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onManualLocationSaved: (country: String, city: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
    onAutoGpsRequested: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    BackHandler(enabled = activeSubScreen != SettingsSubScreen.MAIN) {
        activeSubScreen = SettingsSubScreen.MAIN
    }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "settings_subscreen_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            SettingsSubScreen.MAIN -> {
                SettingsMainMenu(
                    settings = settings,
                    onNavigate = { activeSubScreen = it },
                    onClose = onCloseSettings
                )
            }

            SettingsSubScreen.PRAYER_TIMES -> {
                PrayerTimesSubScreen(
                    settings = settings,
                    onPrayerToggled = onPrayerToggled,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }

            SettingsSubScreen.LOCATION_MANAGEMENT -> {
                LocationManagementSubScreen(
                    settings = settings,
                    onSaveManualLocation = onManualLocationSaved,
                    onAutoGps = onAutoGpsRequested,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }

            SettingsSubScreen.AZAN_SOUND -> {
                AzanSoundSubScreen(
                    settings = settings,
                    onSoundChanged = onSoundChanged,
                    onDurationChanged = onDurationChanged,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }

            SettingsSubScreen.CUSTOMIZE -> {
                CustomizeThemeSubScreen(
                    settings = settings,
                    onDarkModeChanged = onDarkModeChanged,
                    onThemeStyleChanged = onThemeStyleChanged,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }

            SettingsSubScreen.QIBLA_COMPASS -> {
                QiblaCompassScreen(
                    settings = settings,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }

            SettingsSubScreen.LANGUAGE -> {
                LanguageSubScreen(
                    settings = settings,
                    onLanguageChanged = onLanguageChanged,
                    onBack = { activeSubScreen = SettingsSubScreen.MAIN }
                )
            }
        }
    }
}

@Composable
private fun SettingsMainMenu(
    settings: UserSettings,
    onNavigate: (SettingsSubScreen) -> Unit,
    onClose: () -> Unit
) {
    val userLang = settings.language
    val themeStyle = settings.themeStyle

    val titleSettings = when (userLang) {
        AppLanguage.TR -> "Ayarlar"
        AppLanguage.RU -> "Настройки"
        AppLanguage.AZ -> "Tənzimləmələr"
        AppLanguage.EN -> "Settings"
    }
    val subtitleSettings = when (userLang) {
        AppLanguage.TR -> "Helfrex İslam Tercihleri ve Özellikleri"
        AppLanguage.RU -> "Настройки и функции приложения"
        AppLanguage.AZ -> "Tətbiq Tənzimləmələri və Xüsusiyyətləri"
        AppLanguage.EN -> "Helfrex Islam Preferences & Tools"
    }

    val catPrayerTitle = when (userLang) {
        AppLanguage.TR -> "Namaz Vakitleri"
        AppLanguage.RU -> "Время намаза"
        AppLanguage.AZ -> "Namaz Vaxtları"
        AppLanguage.EN -> "Prayer Times"
    }
    val catPrayerSub = when (userLang) {
        AppLanguage.TR -> "Ana ekranda ve bildirimde görünen vakitler"
        AppLanguage.RU -> "Отображение на главном экране и в уведомлениях"
        AppLanguage.AZ -> "Əsas ekranda və bildirişdə görünən vaxtlar"
        AppLanguage.EN -> "Select visible prayers on home & notification"
    }

    val catLocTitle = when (userLang) {
        AppLanguage.TR -> "Konum ve Şehir Yönetimi"
        AppLanguage.RU -> "Управление местоположением"
        AppLanguage.AZ -> "Məkan və Şəhər İdarəetməsi"
        AppLanguage.EN -> "Location & City Management"
    }
    val catLocSub = when (userLang) {
        AppLanguage.TR -> "İnternetsiz ülke/şehir seçimi veya otomatik GPS"
        AppLanguage.RU -> "Выбор страны/города офлайн или авто-GPS"
        AppLanguage.AZ -> "İnternetsiz ölkə/şəhər seçimi və ya avtomatik GPS"
        AppLanguage.EN -> "Offline country/city selection or automatic GPS"
    }

    val catAzanTitle = when (userLang) {
        AppLanguage.TR -> "Ezan Sesi ve Alarmlar"
        AppLanguage.RU -> "Звук азана и будильники"
        AppLanguage.AZ -> "Azan Səsi və Siqnallar"
        AppLanguage.EN -> "Azan Sound & Alarms"
    }
    val catAzanSub = when (userLang) {
        AppLanguage.TR -> "Ezan / Zil sesi, çalma döngüsü ve ses testi"
        AppLanguage.RU -> "Выбор мелодии, повторы и проверка звука"
        AppLanguage.AZ -> "Azan / Zəng səsi, təkrarlanma və səs yoxlaması"
        AppLanguage.EN -> "Azan tone, repeat loop & sound test"
    }

    val catThemeTitle = when (userLang) {
        AppLanguage.TR -> "Özelleştir (Tema)"
        AppLanguage.RU -> "Персонализация (Тема)"
        AppLanguage.AZ -> "Fərdiləşdirmə (Mövzu)"
        AppLanguage.EN -> "Customize (Theme)"
    }
    val catThemeSub = when (userLang) {
        AppLanguage.TR -> "Açık / Koyu mod ve görsel temalar"
        AppLanguage.RU -> "Светлая/темная тема и неоновые стили"
        AppLanguage.AZ -> "Açıq / Qaranlıq rejim və vizual mövzular"
        AppLanguage.EN -> "Dark / Light mode & visual themes"
    }

    val catQiblaTitle = when (userLang) {
        AppLanguage.TR -> "Kıble Pusulası"
        AppLanguage.RU -> "Компас Киблы"
        AppLanguage.AZ -> "Qiblə Kompası"
        AppLanguage.EN -> "Qibla Compass"
    }
    val catQiblaSub = when (userLang) {
        AppLanguage.TR -> "Canlı sensörler ve Kâbe yön göstergesi"
        AppLanguage.RU -> "Сенсорный компас и указатель Каабы"
        AppLanguage.AZ -> "Canlı sensorlar və Kəbə istiqaməti"
        AppLanguage.EN -> "Live sensor compass & Kaaba direction needle"
    }

    val catLangTitle = when (userLang) {
        AppLanguage.TR -> "Dil Seçimi"
        AppLanguage.RU -> "Выбор языка"
        AppLanguage.AZ -> "Dil Seçimi"
        AppLanguage.EN -> "Language"
    }
    val catLangSub = when (userLang) {
        AppLanguage.TR -> "Uygulama dili: Türkçe / English / Русский / Azərbaycan"
        AppLanguage.RU -> "Язык приложения: Türkçe / English / Русский / Azərbaycan"
        AppLanguage.AZ -> "Tətbiq dili: Türkçe / English / Русский / Azərbaycan"
        AppLanguage.EN -> "Application language: Türkçe / English / Русский / Azərbaycan"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = titleSettings,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitleSettings,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SettingsCategoryCard(
                    title = catPrayerTitle,
                    subtitle = catPrayerSub,
                    icon = Icons.Default.Schedule,
                    themeStyle = themeStyle,
                    tag = "cat_prayer_times",
                    onClick = { onNavigate(SettingsSubScreen.PRAYER_TIMES) }
                )
            }

            item {
                SettingsCategoryCard(
                    title = catLocTitle,
                    subtitle = catLocSub,
                    icon = Icons.Default.LocationCity,
                    themeStyle = themeStyle,
                    tag = "cat_location_management",
                    onClick = { onNavigate(SettingsSubScreen.LOCATION_MANAGEMENT) }
                )
            }

            item {
                SettingsCategoryCard(
                    title = catAzanTitle,
                    subtitle = catAzanSub,
                    icon = Icons.Default.VolumeUp,
                    themeStyle = themeStyle,
                    tag = "cat_azan_sound",
                    onClick = { onNavigate(SettingsSubScreen.AZAN_SOUND) }
                )
            }

            item {
                SettingsCategoryCard(
                    title = catThemeTitle,
                    subtitle = catThemeSub,
                    icon = Icons.Default.ColorLens,
                    themeStyle = themeStyle,
                    tag = "cat_customize",
                    onClick = { onNavigate(SettingsSubScreen.CUSTOMIZE) }
                )
            }

            item {
                SettingsCategoryCard(
                    title = catQiblaTitle,
                    subtitle = catQiblaSub,
                    icon = Icons.Default.Explore,
                    themeStyle = themeStyle,
                    tag = "cat_qibla_compass",
                    onClick = { onNavigate(SettingsSubScreen.QIBLA_COMPASS) }
                )
            }

            item {
                SettingsCategoryCard(
                    title = catLangTitle,
                    subtitle = catLangSub,
                    icon = Icons.Default.Language,
                    themeStyle = themeStyle,
                    tag = "cat_language",
                    onClick = { onNavigate(SettingsSubScreen.LANGUAGE) }
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    themeStyle: ThemeStyle,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow(themeStyle, cornerRadius = 16.dp)
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 1. Alt Sayfa: Namaz Vakitleri
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerTimesSubScreen(
    settings: UserSettings,
    onPrayerToggled: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val isTr = settings.language == AppLanguage.TR

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTr) "Namaz Vakitleri" else "Prayer Times",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_prayer_times")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = if (isTr) "Ana ekranda ve bildirim şeridinde görünmesini istediğiniz vakitleri seçin:" else "Select which prayer times are shown on home screen and notification bar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(PrayerType.values().size) { index ->
                val prayerType = PrayerType.values()[index]
                val isChecked = settings.selectedPrayers.contains(prayerType.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPrayerToggled(prayerType.id, !isChecked) }
                        .testTag("checkbox_row_${prayerType.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayerType.getName(settings.language),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onPrayerToggled(prayerType.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("checkbox_${prayerType.id}")
                        )
                    }
                }
            }
        }
    }
}

// 2. Alt Sayfa: Konum ve Şehir Yönetimi (Manuel / Otomatik)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationManagementSubScreen(
    settings: UserSettings,
    onSaveManualLocation: (country: String, city: String, lat: Double, lng: Double) -> Unit,
    onAutoGps: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val isTr = settings.language == AppLanguage.TR

    val isManual = prefsManager.isManualLocation()
    val currentCountry = prefsManager.getManualCountry()
    val (currentCity, currentDistrict) = prefsManager.getLastCityAndDistrict()

    val countries = CityDatabase.countries
    var selectedCountryIndex by remember {
        mutableIntStateOf(
            countries.indexOfFirst { it.nameTr == currentCountry }.coerceAtLeast(0)
        )
    }

    val currentCountryObj = countries[selectedCountryIndex]
    var selectedCityIndex by remember {
        mutableIntStateOf(
            currentCountryObj.cities.indexOfFirst { it.name == currentCity }.coerceAtLeast(0)
        )
    }

    var countryExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTr) "Konum ve Şehir Yönetimi" else "Location & City Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_location")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active status indicator card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isManual) Icons.Default.LocationCity else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTr) "Mevcut Konum Modu:" else "Current Location Mode:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isManual) {
                                    if (isTr) "Manuel Seçim ($currentCity, $currentCountry)" else "Manual Selection ($currentCity, $currentCountry)"
                                } else {
                                    if (isTr) "Otomatik GPS ($currentCity)" else "Automatic GPS ($currentCity)"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Text(
                    text = if (isTr) "Manuel Ülke ve Şehir Seçimi (İnternetsiz)" else "Manual Country & City Selection (Offline)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isTr) "İnternet bağlantısına ihtiyaç duymadan bulunduğunuz şehri seçip kaydedebilirsiniz." else "Select and save your city without needing an internet connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Country Selector Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = !countryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (isTr) currentCountryObj.nameTr else currentCountryObj.nameEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isTr) "Ülke" else "Country") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_country")
                    )

                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        countries.forEachIndexed { index, country ->
                            DropdownMenuItem(
                                text = { Text(if (isTr) country.nameTr else country.nameEn) },
                                onClick = {
                                    selectedCountryIndex = index
                                    selectedCityIndex = 0
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // City Selector Dropdown
            item {
                val availableCities = currentCountryObj.cities
                val safeCityIndex = selectedCityIndex.coerceIn(0, (availableCities.size - 1).coerceAtLeast(0))
                val selectedCityObj = availableCities.getOrNull(safeCityIndex) ?: availableCities[0]

                ExposedDropdownMenuBox(
                    expanded = cityExpanded,
                    onExpandedChange = { cityExpanded = !cityExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCityObj.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isTr) "Şehir / Bölge" else "City / Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_city")
                    )

                    ExposedDropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false }
                    ) {
                        availableCities.forEachIndexed { index, city ->
                            DropdownMenuItem(
                                text = { Text(city.name) },
                                onClick = {
                                    selectedCityIndex = index
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                val availableCities = currentCountryObj.cities
                val safeCityIndex = selectedCityIndex.coerceIn(0, (availableCities.size - 1).coerceAtLeast(0))
                val chosenCity = availableCities.getOrNull(safeCityIndex) ?: availableCities[0]

                Button(
                    onClick = {
                        onSaveManualLocation(
                            currentCountryObj.nameTr,
                            chosenCity.name,
                            chosenCity.latitude,
                            chosenCity.longitude
                        )
                        Toast.makeText(
                            context,
                            if (isTr) "${chosenCity.name} konumu kaydedildi." else "Location ${chosenCity.name} saved.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_manual_location_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTr) "Kaydet" else "Save",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Auto-set Location Button (Directly underneath Save button)
            item {
                Button(
                    onClick = {
                        onAutoGps()
                        Toast.makeText(
                            context,
                            if (isTr) "Otomatik GPS konumuna geçildi." else "Switched to automatic GPS location.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auto_gps_location_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTr) "Konumu Otomatik Ayarla" else "Set Location Automatically",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// 3. Alt Sayfa: Ezan Sesi ve Alarmlar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AzanSoundSubScreen(
    settings: UserSettings,
    onSoundChanged: (AzanSound) -> Unit,
    onDurationChanged: (AzanDuration) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isTr = settings.language == AppLanguage.TR
    val coroutineScope = rememberCoroutineScope()

    var isTestPlaying by remember { mutableStateOf(false) }
    var testMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var testLoopCount by remember { mutableIntStateOf(1) }
    var testJob by remember { mutableStateOf<Job?>(null) }

    fun stopTestSound() {
        testJob?.cancel()
        testJob = null
        try {
            if (testMediaPlayer?.isPlaying == true) {
                testMediaPlayer?.stop()
            }
            testMediaPlayer?.release()
            testMediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isTestPlaying = false
        testLoopCount = 1
    }

    fun playTestSound() {
        stopTestSound()
        val isShort = settings.azanDuration == AzanDuration.SHORT

        when (settings.azanSound) {
            AzanSound.SILENT -> {
                Toast.makeText(
                    context,
                    if (isTr) "Sessiz mod seçili. Ses çalınmaz." else "Silent mode active. No sound played.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            AzanSound.RINGTONE -> {
                val targetLoops = if (isShort) 5 else 10
                val rawResId = R.raw.ezan2

                coroutineScope.launch {
                    try {
                        com.example.util.SoundPlayerHelper.prepareAudioFile(
                            context = context,
                            fileName = "ringtone.mp3",
                            remoteUrl = com.example.data.FileDownloader.URL_RINGTONE,
                            rawFallbackRes = rawResId
                        )

                        val mp = com.example.util.SoundPlayerHelper.createMediaPlayer(
                            context = context,
                            rawResId = rawResId,
                            downloadedFileName = "ringtone.mp3",
                            usage = AudioAttributes.USAGE_MEDIA,
                            contentType = AudioAttributes.CONTENT_TYPE_MUSIC
                        )

                        if (mp != null) {
                            mp.setVolume(1.0f, 1.0f)
                            mp.start()
                            testMediaPlayer = mp
                            isTestPlaying = true
                            testLoopCount = 1

                            mp.setOnCompletionListener { player ->
                                if (testLoopCount < targetLoops) {
                                    testLoopCount++
                                    try {
                                        player.seekTo(0)
                                        player.start()
                                    } catch (e: Exception) {
                                        stopTestSound()
                                    }
                                } else {
                                    stopTestSound()
                                }
                            }
                        } else {
                            Toast.makeText(context, if (isTr) "Ses çalınamadı." else "Audio could not play.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, if (isTr) "Hata: ${e.message}" else "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            AzanSound.AZAN -> {
                val rawResId = R.raw.ezan1

                coroutineScope.launch {
                    try {
                        com.example.util.SoundPlayerHelper.prepareAudioFile(
                            context = context,
                            fileName = "ezan.mp3",
                            remoteUrl = com.example.data.FileDownloader.URL_EZAN,
                            rawFallbackRes = rawResId
                        )

                        val mp = com.example.util.SoundPlayerHelper.createMediaPlayer(
                            context = context,
                            rawResId = rawResId,
                            downloadedFileName = "ezan.mp3",
                            usage = AudioAttributes.USAGE_MEDIA,
                            contentType = AudioAttributes.CONTENT_TYPE_MUSIC
                        )

                        if (mp != null) {
                            mp.setVolume(1.0f, 1.0f)
                            mp.start()
                            testMediaPlayer = mp
                            isTestPlaying = true

                            if (isShort) {
                                testJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(11000L)
                                    stopTestSound()
                                }
                            } else {
                                mp.setOnCompletionListener {
                                    stopTestSound()
                                }
                            }
                        } else {
                            Toast.makeText(context, if (isTr) "Ses çalınamadı." else "Audio could not play.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, if (isTr) "Hata: ${e.message}" else "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopTestSound() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTr) "Ezan Sesi ve Alarmlar" else "Azan Sound & Alarms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_azan_sound")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = if (isTr) "Vakit girdiğinde çalacak ses:" else "Sound played on prayer time:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(AzanSound.values().size) { index ->
                val sound = AzanSound.values()[index]
                val isSelected = settings.azanSound == sound
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSoundChanged(sound) }
                        .testTag("sound_radio_row_${sound.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sound.getName(settings.language),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val soundDesc = when (sound) {
                                AzanSound.SILENT -> if (isTr) "Sadece sessiz bildirim" else "Silent notification only"
                                AzanSound.RINGTONE -> if (isTr) "GitHub Zil Sesi (Ezan Sesi.mp3)" else "GitHub Ringtone (Ezan Sesi.mp3)"
                                AzanSound.AZAN -> if (isTr) "Orijinal Ezan Sesi" else "Original Azan Audio"
                            }
                            Text(
                                text = soundDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSoundChanged(sound) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("sound_radio_${sound.id}")
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AzanDuration.values().forEach { duration ->
                        val isSelected = settings.azanDuration == duration
                        val label = if (duration == AzanDuration.SHORT) {
                            if (isTr) "Kısa" else "Short"
                        } else {
                            if (isTr) "Uzun" else "Long"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDurationChanged(duration) }
                                .testTag("duration_mode_${duration.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (isTestPlaying) stopTestSound() else playTestSound()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_sound_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isTestPlaying) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isTestPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Test Sound"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTestPlaying) {
                            if (isTr) "Sesi Durdur" else "Stop Sound"
                        } else {
                            if (isTr) "Seçili Sesi Test Et" else "Test Selected Sound"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 4. Alt Sayfa: Özelleştir (Tema)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeThemeSubScreen(
    settings: UserSettings,
    onDarkModeChanged: (Boolean) -> Unit,
    onThemeStyleChanged: (ThemeStyle) -> Unit,
    onBack: () -> Unit
) {
    val isTr = settings.language == AppLanguage.TR

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTr) "Özelleştir (Tema)" else "Customize (Theme)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_customize")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Dark / Light Mode Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isTr) "Karanlık Mod" else "Dark Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.isDarkMode) {
                                        if (isTr) "Koyu arka plan aktif" else "Dark background enabled"
                                    } else {
                                        if (isTr) "Açık tema (Beyaz arkaplan) aktif" else "Light theme (White background) enabled"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = onDarkModeChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (isTr) "Görsel Temalar" else "Visual Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(ThemeStyle.values().size) { index ->
                val style = ThemeStyle.values()[index]
                val isSelected = settings.themeStyle == style
                val themeDescription = when (style) {
                    ThemeStyle.CLASSIC -> if (isTr) "Klasik — Aydınlıkta Beyaz simge, Karanlıkta Koyu tema" else "Classic — White icon in Light, Dark in Dark mode"
                    ThemeStyle.NEON_BLUE -> if (isTr) "Neon Mavi — Buton, saat ve kartlarda Mavi Neon parlama" else "Neon Blue — Electric Cyan Neon glow on cards & clocks"
                    ThemeStyle.NEON_PURPLE -> if (isTr) "Neon Mor — Buton, saat ve kartlarda Mor Neon parlama" else "Neon Purple — Pinkish Purple Neon glow on cards & clocks"
                    ThemeStyle.NEON_EMERALD -> if (isTr) "Neon Zümrüt — Parlak zümrüt yeşili neon vurgular ve parlamalı kartlar" else "Neon Emerald — Vibrant emerald green neon accents & glow"
                }

                val accentColor = when (style) {
                    ThemeStyle.CLASSIC -> if (settings.isDarkMode) Color.White else Color.Black
                    ThemeStyle.NEON_BLUE -> NeonCyan
                    ThemeStyle.NEON_PURPLE -> NeonPink
                    ThemeStyle.NEON_EMERALD -> NeonEmerald
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onThemeStyleChanged(style) }
                        .testTag("theme_radio_row_${style.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = style.getName(settings.language),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onThemeStyleChanged(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("theme_radio_${style.id}")
                        )
                    }
                }
            }
        }
    }
}

// 5. Alt Sayfa: Dil Seçimi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSubScreen(
    settings: UserSettings,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val isTr = settings.language == AppLanguage.TR

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTr) "Dil Seçimi" else "Language",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_language")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isTr) "Uygulama arayüz dilini seçin:" else "Select application interface language:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AppLanguage.values().forEach { lang ->
                val isSelected = settings.language == lang
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLanguageChanged(lang) }
                        .testTag("lang_toggle_${lang.code}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seçili",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
