package com.example.myrunapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = AppSecondaryText,
    tertiary = Accent,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color(0xFF06130E),
    onSecondary = AppPrimaryText,
    onTertiary = Color(0xFF06130E),
    onBackground = AppPrimaryText,
    onSurface = AppPrimaryText,
    error = AppError
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = AppSecondaryText,
    tertiary = Accent,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color(0xFF06130E),
    onSecondary = AppPrimaryText,
    onTertiary = Color(0xFF06130E),
    onBackground = AppPrimaryText,
    onSurface = AppPrimaryText,
    error = AppError
)

@Composable
fun MyRunAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
