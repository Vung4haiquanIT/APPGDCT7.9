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

private val LightColorScheme =
  lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    secondary = GoldDark,
    onSecondary = Color.Black,
    tertiary = GoldPrimary,
    background = CreamBackground,
    onBackground = TextDark,
    surface = SurfaceCard,
    onSurface = TextDark,
    outline = BorderColor
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = RedLight,
    onPrimary = Color.Black,
    secondary = GoldPrimary,
    onSecondary = Color.Black,
    tertiary = GoldDark,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF333333)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
