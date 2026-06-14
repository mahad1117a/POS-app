package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF1C1917),
    primaryContainer = Color(0xFF44403C),
    onPrimaryContainer = Color(0xFFF5F5F4),
    secondary = NaturalPrimary,
    onSecondary = Color.White,
    tertiary = NaturalGreenAccent,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = Color(0xFFF5F5F4),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF44403C),
    onSurfaceVariant = Color(0xFFFAFAF9)
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = Color.White,
    primaryContainer = NaturalTertiary,
    onPrimaryContainer = NaturalTextDark,
    secondary = NaturalSecondary,
    onSecondary = NaturalTextDark,
    tertiary = NaturalGreenAccent,
    background = NaturalBg,
    surface = Color.White,
    onBackground = NaturalTextDark,
    onSurface = NaturalTextDark,
    surfaceVariant = NaturalSecondary,
    onSurfaceVariant = NaturalTextDark,
    onTertiary = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to ensure our gorgeous emerald design is consistently branding the app
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
