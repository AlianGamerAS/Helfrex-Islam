package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.PrayerTimeItem
import com.example.model.PrayerTimesData
import com.example.model.PrayerType
import com.example.model.ThemeStyle
import com.example.model.UserSettings
import com.example.ui.components.neonGlow
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    prayerData: PrayerTimesData,
    settings: UserSettings,
    onRefreshLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userLang = settings.language
    val isTr = userLang == AppLanguage.TR
    val themeStyle = settings.themeStyle

    // Live real-time seconds ticker
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Filter items based on settings checkboxes, ordered chronologically
    val filteredItems = remember(prayerData.items, settings.selectedPrayers) {
        prayerData.items
            .filter { settings.selectedPrayers.contains(it.type.id) }
            .sortedBy { it.type.order }
    }

    // Determine the next upcoming prayer from the filtered list
    val nextItem = remember(filteredItems, currentTimeMillis) {
        filteredItems.firstOrNull { it.targetTimeMillis > currentTimeMillis }
            ?: filteredItems.firstOrNull()?.let {
                it.copy(targetTimeMillis = it.targetTimeMillis + 24 * 60 * 60 * 1000L)
            }
    }

    val remainingMillis = remember(nextItem, currentTimeMillis) {
        if (nextItem != null) {
            (nextItem.targetTimeMillis - currentTimeMillis).coerceAtLeast(0L)
        } else {
            0L
        }
    }

    val remainingSeconds = (remainingMillis / 1000) % 60
    val remainingMinutes = (remainingMillis / (1000 * 60)) % 60
    val remainingHours = remainingMillis / (1000 * 60 * 60)
    val formattedCountdown = String.format("%02d:%02d:%02d", remainingHours, remainingMinutes, remainingSeconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Location & Dates
            item {
                HeaderCard(
                    cityName = prayerData.cityName,
                    districtName = prayerData.districtName,
                    gregorianDate = prayerData.dateGregorian,
                    hijriDate = prayerData.dateHijri,
                    themeStyle = themeStyle,
                    language = userLang
                )
            }

            // Countdown Hero Card (Prominently displayed)
            item {
                val prayerFallback = when (userLang) {
                    AppLanguage.TR -> "Namaz"
                    AppLanguage.RU -> "Намаз"
                    AppLanguage.AZ -> "Namaz"
                    AppLanguage.EN -> "Prayer"
                }
                CountdownBarCard(
                    nextPrayerName = nextItem?.type?.getName(userLang) ?: prayerFallback,
                    countdownStr = formattedCountdown,
                    remainingMillis = remainingMillis,
                    themeStyle = themeStyle,
                    language = userLang
                )
            }

            // Section Title
            item {
                val prayerTimesTitle = when (userLang) {
                    AppLanguage.TR -> "Namaz Vakitleri"
                    AppLanguage.RU -> "Время намаза"
                    AppLanguage.AZ -> "Namaz Vaxtları"
                    AppLanguage.EN -> "Prayer Times"
                }
                val activeCountSubtitle = when (userLang) {
                    AppLanguage.TR -> "${filteredItems.size} Vakit Seçili"
                    AppLanguage.RU -> "${filteredItems.size} выбрано"
                    AppLanguage.AZ -> "${filteredItems.size} Vaxt Seçilib"
                    AppLanguage.EN -> "${filteredItems.size} Active"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prayerTimesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeCountSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List of selected prayer times
            items(filteredItems, key = { it.type.id }) { item ->
                val isCurrentNext = nextItem?.type == item.type
                PrayerTimeCard(
                    item = item,
                    isNext = isCurrentNext,
                    themeStyle = themeStyle,
                    language = userLang
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(
    cityName: String,
    districtName: String,
    gregorianDate: String,
    hijriDate: String,
    themeStyle: ThemeStyle,
    language: AppLanguage
) {
    val hijriLabel = when (language) {
        AppLanguage.TR -> "Hicri Tarih"
        AppLanguage.RU -> "Хиджри дата"
        AppLanguage.AZ -> "Hicri Tarix"
        AppLanguage.EN -> "Hijri Date"
    }
    val gregorianLabel = when (language) {
        AppLanguage.TR -> "Miladi Tarih"
        AppLanguage.RU -> "Григорианская дата"
        AppLanguage.AZ -> "Miladi Tarix"
        AppLanguage.EN -> "Gregorian Date"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow(themeStyle, cornerRadius = 20.dp)
            .testTag("header_location_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val locationTitle = if (districtName.isNotBlank()) "$cityName / $districtName" else cityName
                    Text(
                        text = locationTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dates Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = hijriLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hijriDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = gregorianLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeCard(
    item: PrayerTimeItem,
    isNext: Boolean,
    themeStyle: ThemeStyle,
    language: AppLanguage
) {
    val prayerIcon = getPrayerIcon(item.type)
    val prayerName = item.type.getName(language)

    val cardColor = if (isNext) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isNext) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconColor = if (isNext) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) Modifier.neonGlow(themeStyle, cornerRadius = 14.dp)
                else Modifier
            )
            .testTag("prayer_card_${item.type.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = if (isNext) 0.2f else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = prayerIcon,
                        contentDescription = prayerName,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = prayerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    if (isNext) {
                        val upcomingLabel = when (language) {
                            AppLanguage.TR -> "Sıradaki Vakit"
                            AppLanguage.RU -> "Следующий намаз"
                            AppLanguage.AZ -> "Növbəti Vaxt"
                            AppLanguage.EN -> "Upcoming Prayer"
                        }
                        Text(
                            text = upcomingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = item.timeStr,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CountdownBarCard(
    nextPrayerName: String,
    countdownStr: String,
    remainingMillis: Long,
    themeStyle: ThemeStyle,
    language: AppLanguage
) {
    val countdownHeader = when (language) {
        AppLanguage.TR -> "$nextPrayerName Namazına Kalan Vakit"
        AppLanguage.RU -> "До намаза $nextPrayerName осталось"
        AppLanguage.AZ -> "$nextPrayerName Namazına Qalan Vaxt"
        AppLanguage.EN -> "Time Remaining until $nextPrayerName"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow(themeStyle, cornerRadius = 18.dp, borderWidth = 2.dp)
            .testTag("countdown_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Countdown",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = countdownHeader,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Countdown Display
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = countdownStr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth progress indicator
            val maxSpanMillis = 6 * 60 * 60 * 1000L // approximate prayer interval
            val progress = (1f - (remainingMillis.toFloat() / maxSpanMillis)).coerceIn(0.05f, 0.95f)

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

private fun getPrayerIcon(type: PrayerType): ImageVector {
    return when (type) {
        PrayerType.IMSAK -> Icons.Default.NightsStay
        PrayerType.SABAH -> Icons.Default.Brightness6
        PrayerType.GUNES -> Icons.Default.Brightness7
        PrayerType.OGLE -> Icons.Default.WbSunny
        PrayerType.IKINDI -> Icons.Default.Brightness5
        PrayerType.AKSAM -> Icons.Default.Brightness6
        PrayerType.YATSI -> Icons.Default.Nightlight
    }
}
