package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeStyle

private val ClassicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFE5E5E5),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF525252)
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF262626),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFFD4D4D4)
)

private val NeonBlueDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyanCard,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    background = Color(0xFF061014),
    onBackground = Color(0xFFE6F9FC),
    surface = Color(0xFF0A181E),
    onSurface = Color(0xFFE6F9FC),
    surfaceVariant = NeonCyanCard,
    onSurfaceVariant = Color(0xFF8ED2DE),
    outline = NeonCyan.copy(alpha = 0.7f)
)

private val NeonBlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color.White,
    background = Color(0xFFF0FDFE),
    onBackground = Color(0xFF00363A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF00363A),
    surfaceVariant = Color(0xFFE0F7FA),
    onSurfaceVariant = Color(0xFF006064),
    outline = Color(0xFF00E5FF)
)

private val NeonPurpleDarkColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = Color.White,
    primaryContainer = NeonPinkCard,
    onPrimaryContainer = Color(0xFFFF8EC4),
    secondary = NeonPink,
    onSecondary = Color.White,
    background = Color(0xFF140610),
    onBackground = Color(0xFFFCE6F2),
    surface = Color(0xFF1E0A18),
    onSurface = Color(0xFFFCE6F2),
    surfaceVariant = NeonPinkCard,
    onSurfaceVariant = Color(0xFFDE8EB8),
    outline = NeonPink.copy(alpha = 0.7f)
)

private val NeonPurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF831843),
    secondary = Color(0xFFE91E63),
    onSecondary = Color.White,
    background = Color(0xFFFDF2F8),
    onBackground = Color(0xFF500724),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF500724),
    surfaceVariant = Color(0xFFFCE7F3),
    onSurfaceVariant = Color(0xFF9D174D),
    outline = Color(0xFFFF2A85)
)

private val NeonEmeraldDarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = Color(0xFF002010),
    primaryContainer = NeonEmeraldCard,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = NeonEmerald,
    onSecondary = Color(0xFF002010),
    background = Color(0xFF06150E),
    onBackground = Color(0xFFE6F9F0),
    surface = Color(0xFF0A2016),
    onSurface = Color(0xFFE6F9F0),
    surfaceVariant = NeonEmeraldCard,
    onSurfaceVariant = Color(0xFF8ED2B0),
    outline = NeonEmerald.copy(alpha = 0.7f)
)

private val NeonEmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF065F46),
    outline = Color(0xFF50C878)
)

@Composable
fun HelfrexTheme(
    isDarkMode: Boolean = true,
    themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        ThemeStyle.CLASSIC -> if (isDarkMode) ClassicDarkColorScheme else ClassicLightColorScheme
        ThemeStyle.NEON_BLUE -> if (isDarkMode) NeonBlueDarkColorScheme else NeonBlueLightColorScheme
        ThemeStyle.NEON_PURPLE -> if (isDarkMode) NeonPurpleDarkColorScheme else NeonPurpleLightColorScheme
        ThemeStyle.NEON_EMERALD -> if (isDarkMode) NeonEmeraldDarkColorScheme else NeonEmeraldLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
