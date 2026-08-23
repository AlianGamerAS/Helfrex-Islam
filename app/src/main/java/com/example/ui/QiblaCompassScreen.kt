package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FaziletPrayerService
import com.example.model.AppLanguage
import com.example.model.ThemeStyle
import com.example.model.UserSettings
import com.example.ui.components.neonGlow
import com.example.ui.theme.SophisticatedCyan
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userLang = settings.language

    val prefsManager = remember { com.example.data.PreferencesManager.getInstance(context) }
    val prayerService = remember { FaziletPrayerService.getInstance(context) }
    val prayerData = prayerService.prayerDataFlow.value

    val (locLat, locLng) = prefsManager.getLastLocation()
    val (city, _) = prefsManager.getLastCityAndDistrict()
    val userLat = if (locLat != 0.0) locLat else 41.0082
    val userLon = if (locLng != 0.0) locLng else 28.9784
    val displayCity = if (prayerData.cityName.isNotBlank()) prayerData.cityName else city

    val qiblaBearing = remember(userLat, userLon) {
        calculateQiblaBearing(userLat, userLon)
    }

    var currentAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    // Setup Compass Sensors
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var lastAccelerometer = FloatArray(3)
        var lastMagnetometer = FloatArray(3)
        var lastAccelerometerSet = false
        var lastMagnetometerSet = false
        val rMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    SensorManager.getOrientation(rMatrix, orientation)
                    val azimuthRad = orientation[0]
                    var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    azimuthDeg = (azimuthDeg + 360f) % 360f
                    // Smooth low-pass filter
                    currentAzimuth = currentAzimuth + 0.15f * (azimuthDeg - currentAzimuth)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetometerSet = true
                }

                if (rotationSensor == null && lastAccelerometerSet && lastMagnetometerSet) {
                    if (SensorManager.getRotationMatrix(rMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(rMatrix, orientation)
                        val azimuthRad = orientation[0]
                        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                        azimuthDeg = (azimuthDeg + 360f) % 360f
                        currentAzimuth = currentAzimuth + 0.15f * (azimuthDeg - currentAzimuth)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorAvailable = false
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Relative angle between device heading and Qibla
    val relativeQiblaAngle = (qiblaBearing - currentAzimuth + 360f) % 360f
    val isAlignedWithQibla = abs(if (relativeQiblaAngle > 180) 360 - relativeQiblaAngle else relativeQiblaAngle) <= 4f

    val animatedHeading by animateFloatAsState(
        targetValue = -currentAzimuth,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "compass_rotation"
    )

    val targetStatusColor by animateColorAsState(
        targetValue = if (isAlignedWithQibla) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
        label = "aligned_color"
    )

    val topBarTitle = when (userLang) {
        AppLanguage.TR -> "Kıble Pusulası"
        AppLanguage.RU -> "Компас Киблы"
        AppLanguage.AZ -> "Qiblə Kompası"
        AppLanguage.EN -> "Qibla Compass"
    }

    val backDesc = when (userLang) {
        AppLanguage.TR -> "Geri"
        AppLanguage.RU -> "Назад"
        AppLanguage.AZ -> "Geri"
        AppLanguage.EN -> "Back"
    }

    val locDetectedFallback = when (userLang) {
        AppLanguage.TR -> "Konum Tespit Edildi"
        AppLanguage.RU -> "Местоположение определено"
        AppLanguage.AZ -> "Məkan Müəyyən Edildi"
        AppLanguage.EN -> "Location Detected"
    }

    val qiblaAngleLabel = when (userLang) {
        AppLanguage.TR -> "Kıble Açısı"
        AppLanguage.RU -> "Угол Киблы"
        AppLanguage.AZ -> "Qiblə Bucağı"
        AppLanguage.EN -> "Qibla Angle"
    }

    val alignedMessage = when (userLang) {
        AppLanguage.TR -> "✓ KIBLEYE YÖNELDİNİZ"
        AppLanguage.RU -> "✓ НАПРАВЛЕНО НА КИБЛУ"
        AppLanguage.AZ -> "✓ QİBLƏYƏ YÖNƏLDİNİZ"
        AppLanguage.EN -> "✓ ALIGNED WITH QIBLA"
    }

    val guideMessage = when (userLang) {
        AppLanguage.TR -> "Telefonu altın Kâbe işaretine doğru çevirin"
        AppLanguage.RU -> "Поверните телефон к золотому знаку Каабы"
        AppLanguage.AZ -> "Telefonu qızıl Kəbə işarəsinə doğru çevirin"
        AppLanguage.EN -> "Rotate phone towards gold Kaaba icon"
    }

    val statusSubtitle = when (userLang) {
        AppLanguage.TR -> "Cihaz Açısı: ${currentAzimuth.roundToInt()}° • Hedef Kıble: ${qiblaBearing.roundToInt()}°"
        AppLanguage.RU -> "Угол устройства: ${currentAzimuth.roundToInt()}° • Кибла: ${qiblaBearing.roundToInt()}°"
        AppLanguage.AZ -> "Cihaz Bucağı: ${currentAzimuth.roundToInt()}° • Hədəf Qiblə: ${qiblaBearing.roundToInt()}°"
        AppLanguage.EN -> "Heading: ${currentAzimuth.roundToInt()}° • Target Qibla: ${qiblaBearing.roundToInt()}°"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("compass_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDesc)
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
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Location and Bearing Information Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .neonGlow(settings.themeStyle, cornerRadius = 16.dp)
                    .testTag("compass_info_card"),
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayCity.ifEmpty { locDetectedFallback },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.4f°, %.4f°", userLat, userLon),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = qiblaAngleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${qiblaBearing.roundToInt()}°",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Compass Dial Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("compass_canvas")
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer glowing dial ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF151921),
                                Color(0xFF0F1218)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    drawCircle(
                        color = targetStatusColor.copy(alpha = if (isAlignedWithQibla) 0.8f else 0.3f),
                        radius = radius - 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = if (isAlignedWithQibla) 3.dp.toPx() else 1.5.dp.toPx())
                    )

                    drawCircle(
                        color = Color(0xFF263238),
                        radius = radius * 0.78f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Rotate the dial with current azimuth
                    rotate(animatedHeading, pivot = center) {
                        // Draw degree tick marks
                        for (deg in 0 until 360 step 15) {
                            val angleRad = Math.toRadians(deg.toDouble() - 90.0)
                            val isMajor = deg % 90 == 0
                            val isMedium = deg % 30 == 0

                            val tickLength = when {
                                isMajor -> 18.dp.toPx()
                                isMedium -> 12.dp.toPx()
                                else -> 6.dp.toPx()
                            }
                            val tickColor = when {
                                deg == 0 -> Color(0xFFEF4444) // North in Red
                                isMajor -> Color.White
                                else -> Color(0xFF64748B)
                            }
                            val tickStroke = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()

                            val startX = center.x + (radius - 12.dp.toPx()) * cos(angleRad).toFloat()
                            val startY = center.y + (radius - 12.dp.toPx()) * sin(angleRad).toFloat()
                            val endX = center.x + (radius - 12.dp.toPx() - tickLength) * cos(angleRad).toFloat()
                            val endY = center.y + (radius - 12.dp.toPx() - tickLength) * sin(angleRad).toFloat()

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = tickStroke
                            )
                        }

                        // Cardinal Directions Text
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 34f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // N (Red), E, S, W
                        paint.color = android.graphics.Color.parseColor("#EF4444")
                        drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius + 48.dp.toPx(), paint)

                        paint.color = android.graphics.Color.WHITE
                        drawContext.canvas.nativeCanvas.drawText("E", center.x + radius - 36.dp.toPx(), center.y + 12f, paint)
                        drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + radius - 24.dp.toPx(), paint)
                        drawContext.canvas.nativeCanvas.drawText("W", center.x - radius + 36.dp.toPx(), center.y + 12f, paint)

                        // QIBLA MARKER on the dial: Kaaba icon / Gold indicator at Qibla Bearing angle
                        rotate(qiblaBearing, pivot = center) {
                            val qiblaIndicatorRadius = radius - 30.dp.toPx()
                            val qiblaX = center.x
                            val qiblaY = center.y - qiblaIndicatorRadius

                            // Kaaba Emblem Box / Indicator
                            drawCircle(
                                color = Color(0xFFD4AF37),
                                radius = 14.dp.toPx(),
                                center = Offset(qiblaX, qiblaY)
                            )
                            drawCircle(
                                color = Color(0xFF0F172A),
                                radius = 11.dp.toPx(),
                                center = Offset(qiblaX, qiblaY)
                            )
                            drawCircle(
                                color = Color(0xFFD4AF37),
                                radius = 4.dp.toPx(),
                                center = Offset(qiblaX, qiblaY)
                            )

                            // Arrow line pointing to Kaaba from center
                            val qiblaArrowPath = Path().apply {
                                moveTo(center.x, center.y - qiblaIndicatorRadius + 18.dp.toPx())
                                lineTo(center.x - 6.dp.toPx(), center.y - 40.dp.toPx())
                                lineTo(center.x + 6.dp.toPx(), center.y - 40.dp.toPx())
                                close()
                            }
                            drawPath(qiblaArrowPath, brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0x33D4AF37))))
                        }
                    }

                    // Static Top Needle / Heading Pointer
                    val needlePath = Path().apply {
                        moveTo(center.x, center.y - radius + 6.dp.toPx())
                        lineTo(center.x - 10.dp.toPx(), center.y - radius + 24.dp.toPx())
                        lineTo(center.x + 10.dp.toPx(), center.y - radius + 24.dp.toPx())
                        close()
                    }
                    drawPath(
                        needlePath,
                        color = if (isAlignedWithQibla) Color(0xFF10B981) else Color(0xFF00F2FF)
                    )

                    // Center Hub
                    drawCircle(
                        color = Color(0xFF151921),
                        radius = 28.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = targetStatusColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                }

                // Center Icon / Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isAlignedWithQibla) Icons.Default.CheckCircle else Icons.Default.Mosque,
                        contentDescription = "Kıble",
                        tint = targetStatusColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom Status & Alignment Instruction Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .neonGlow(settings.themeStyle, cornerRadius = 16.dp)
                    .testTag("compass_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAlignedWithQibla) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAlignedWithQibla) alignedMessage else guideMessage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlignedWithQibla) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAlignedWithQibla) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Calculates initial great-circle bearing from user coordinates to Kaaba in Mecca
 */
private fun calculateQiblaBearing(userLat: Double, userLon: Double): Float {
    val kaabaLat = Math.toRadians(21.422487)
    val kaabaLon = Math.toRadians(39.826206)
    val uLat = Math.toRadians(userLat)
    val uLon = Math.toRadians(userLon)

    val deltaLon = kaabaLon - uLon
    val y = sin(deltaLon) * cos(kaabaLat)
    val x = cos(uLat) * sin(kaabaLat) - sin(uLat) * cos(kaabaLat) * cos(deltaLon)
    var bearing = Math.toDegrees(atan2(y, x))
    bearing = (bearing + 360.0) % 360.0
    return bearing.toFloat()
}
