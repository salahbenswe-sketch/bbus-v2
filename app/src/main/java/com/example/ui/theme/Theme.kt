package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BusIndigoPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = BusIndigoContainerDark,
    onPrimaryContainer = Color(0xFFFFDADD),
    secondary = Color(0xFFCEBFC0),
    onSecondary = Color(0xFF352F30),
    tertiary = Color(0xFF7CD1A9),
    onTertiary = Color(0xFF073824),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5B1718),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BusBackgroundDark,
    surface = BusSurfaceDark,
    onBackground = Color(0xFFE8E1E2),
    onSurface = Color(0xFFE8E1E2),
    surfaceVariant = Color(0xFF302D30),
    onSurfaceVariant = Color(0xFFCBC4C5),
    outline = Color(0xFF958F90),
    outlineVariant = Color(0xFF494548),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BusIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE6E8),
    onPrimaryContainer = Color(0xFF410009),
    secondary = Color(0xFF595457),
    onSecondary = Color.White,
    tertiary = BusAccentTeal,
    onTertiary = Color.White,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = BusBackgroundLight,
    surface = BusSurfaceLight,
    onBackground = Color(0xFF21191A),
    onSurface = Color(0xFF21191A),
    surfaceVariant = Color(0xFFF0EAEB),
    onSurfaceVariant = Color(0xFF554F51),
    outline = Color(0xFF81797B),
    outlineVariant = Color(0xFFE1DADB),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
