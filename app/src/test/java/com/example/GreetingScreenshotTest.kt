package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.AppLanguage
import com.example.model.PrayerTimeItem
import com.example.model.PrayerTimesData
import com.example.model.PrayerType
import com.example.model.UserSettings
import com.example.ui.HomeScreen
import com.example.ui.theme.HelfrexTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleData = PrayerTimesData(
        dateGregorian = "22.08.2026",
        dateHijri = "9 Rebiülevvel 1448",
        cityName = "İstanbul",
        districtName = "Fatih",
        items = listOf(
            PrayerTimeItem(PrayerType.IMSAK, "04:52", System.currentTimeMillis() - 3600000),
            PrayerTimeItem(PrayerType.SABAH, "06:12", System.currentTimeMillis() - 1800000),
            PrayerTimeItem(PrayerType.GUNES, "06:35", System.currentTimeMillis() - 900000),
            PrayerTimeItem(PrayerType.OGLE, "13:16", System.currentTimeMillis() + 3600000, isNext = true),
            PrayerTimeItem(PrayerType.IKINDI, "16:58", System.currentTimeMillis() + 7200000),
            PrayerTimeItem(PrayerType.AKSAM, "19:54", System.currentTimeMillis() + 10800000),
            PrayerTimeItem(PrayerType.YATSI, "21:20", System.currentTimeMillis() + 14400000)
        )
    )

    val sampleSettings = UserSettings(
        language = AppLanguage.TR
    )

    composeTestRule.setContent {
      HelfrexTheme {
        HomeScreen(
            prayerData = sampleData,
            settings = sampleSettings,
            onRefreshLocation = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
