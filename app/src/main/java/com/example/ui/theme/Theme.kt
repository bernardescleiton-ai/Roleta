package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LuckyPurpleLight,
    onPrimary = Color.White,
    primaryContainer = LuckyPurple,
    onPrimaryContainer = Color.White,
    secondary = LuckyGold,
    onSecondary = Color.Black,
    secondaryContainer = LuckyAmber,
    tertiary = LuckyEmerald,
    background = LuckyDarkBackground,
    onBackground = Color.White,
    surface = LuckyDarkSurface,
    onSurface = Color.White,
    surfaceVariant = LuckyDarkCard,
    onSurfaceVariant = Color(0xFFE5E7EB),
    outline = LuckyDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LuckyPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = LuckyIndigo,
    secondary = LuckyAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    tertiary = LuckyEmerald,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek vibrant dark theme for high-contrast roulette excitement
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
