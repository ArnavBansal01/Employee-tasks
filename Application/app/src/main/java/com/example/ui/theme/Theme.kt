package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Blue500,
    secondary = Blue600,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onPrimary = White,
    onBackground = Slate50,
    onSurface = Slate50,
    onSurfaceVariant = Slate200
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Blue600,
    secondary = Blue500,
    background = Slate50,
    surface = White,
    surfaceVariant = Slate200,
    onPrimary = White,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate700
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to match requested aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
