package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.data.FaziletPrayerService
import com.example.data.PreferencesManager
import com.example.model.AppLanguage
import com.example.receiver.PrayerAlarmScheduler
import com.example.service.PrayerForegroundService
import com.example.ui.HomeScreen
import com.example.ui.LibraryScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.HelfrexTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var prayerService: FaziletPrayerService

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Location permission granted, refresh GPS coordinates
            lifecycleScopeLaunchLocation()
        }

        // Reschedule alarms with exact time
        PrayerAlarmScheduler.scheduleAllAlarms(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PreferencesManager.getInstance(this)
        prayerService = FaziletPrayerService.getInstance(this)

        // Request all critical permissions on startup as required
        requestAppPermissions()

        // Start ongoing foreground service
        PrayerForegroundService.startService(this)

        // Kill switch: Stop any active azan or ringtone when entering the app
        com.example.service.AzanPlayerService.stopPlayback(this)

        setContent {
            val settings by prefsManager.settingsFlow.collectAsState()
            val prayerData by prayerService.prayerDataFlow.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            var selectedTab by remember { mutableIntStateOf(0) }

            HelfrexTheme(
                isDarkMode = settings.isDarkMode,
                themeStyle = settings.themeStyle
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        HelfrexBottomNav(
                            selectedTab = selectedTab,
                            language = settings.language,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> HomeScreen(
                            prayerData = prayerData,
                            settings = settings,
                            onRefreshLocation = {
                                coroutineScope.launch {
                                    prayerService.requestCurrentLocationAndRefresh()
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )

                        1 -> LibraryScreen(
                            settings = settings,
                            modifier = Modifier.padding(innerPadding)
                        )

                        2 -> SettingsScreen(
                            settings = settings,
                            onPrayerToggled = { id, checked ->
                                prefsManager.updateSelectedPrayer(id, checked)
                                PrayerAlarmScheduler.scheduleAllAlarms(this@MainActivity)
                            },
                            onSoundChanged = { sound ->
                                prefsManager.setAzanSound(sound)
                            },
                            onDurationChanged = { duration ->
                                prefsManager.setAzanDuration(duration)
                            },
                            onDarkModeChanged = { isDark ->
                                prefsManager.setDarkMode(isDark)
                            },
                            onThemeStyleChanged = { style ->
                                prefsManager.setThemeStyle(style)
                            },
                            onLanguageChanged = { lang ->
                                prefsManager.setLanguage(lang)
                            },
                            onManualLocationSaved = { country, city, lat, lng ->
                                coroutineScope.launch {
                                    prayerService.applyManualLocation(country, city, lat, lng)
                                }
                            },
                            onAutoGpsRequested = {
                                coroutineScope.launch {
                                    prayerService.applyAutomaticGpsLocation()
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Kill switch: instant audio stop when app is resumed
        com.example.service.AzanPlayerService.stopPlayback(this)
    }

    override fun onStop() {
        super.onStop()
        // Apply icon changes safely when the app goes into background
        com.example.util.IconManager.applyPendingIconUpdate(this)
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun lifecycleScopeLaunchLocation() {
        // Asynchronously request GPS position
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            prayerService.requestCurrentLocationAndRefresh()
        }
    }
}

@Composable
fun HelfrexBottomNav(
    selectedTab: Int,
    language: AppLanguage,
    onTabSelected: (Int) -> Unit
) {
    val menuLabel = when (language) {
        AppLanguage.TR -> "Menü"
        AppLanguage.RU -> "Главная"
        AppLanguage.AZ -> "Əsas"
        AppLanguage.EN -> "Menu"
    }
    val docsLabel = when (language) {
        AppLanguage.TR -> "Belge"
        AppLanguage.RU -> "Библиотека"
        AppLanguage.AZ -> "Sənədlər"
        AppLanguage.EN -> "Documents"
    }
    val settingsLabel = when (language) {
        AppLanguage.TR -> "Ayarlar"
        AppLanguage.RU -> "Настройки"
        AppLanguage.AZ -> "Tənzimləmələr"
        AppLanguage.EN -> "Settings"
    }

    val items = listOf(
        Triple(
            menuLabel,
            Icons.Filled.Home,
            Icons.Outlined.Home
        ),
        Triple(
            docsLabel,
            Icons.Filled.MenuBook,
            Icons.Outlined.MenuBook
        ),
        Triple(
            settingsLabel,
            Icons.Filled.Settings,
            Icons.Outlined.Settings
        )
    )

    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_$index")
            )
        }
    }
}
