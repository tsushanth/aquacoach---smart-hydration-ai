package com.factory.aquacoachsmarthydrationai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = AquaSurfaceLight,
    primaryContainer = AquaBlue80,
    onPrimaryContainer = AquaPrimaryDark,
    secondary = AquaAccent,
    onSecondary = AquaSurfaceLight,
    background = AquaBackgroundLight,
    onBackground = AquaPrimaryDark,
    surface = AquaSurfaceLight,
    onSurface = AquaPrimaryDark,
    surfaceVariant = AquaProgressTrack,
    error = AquaWarning
)

private val DarkColors = darkColorScheme(
    primary = AquaSecondary,
    onPrimary = AquaPrimaryDark,
    primaryContainer = AquaBlueGrey40,
    onPrimaryContainer = AquaBlue80,
    secondary = AquaAccent,
    onSecondary = AquaPrimaryDark,
    background = AquaBackgroundDark,
    onBackground = AquaBlue80,
    surface = AquaSurfaceDark,
    onSurface = AquaBlue80,
    surfaceVariant = AquaBlueGrey40,
    error = AquaWarning
)

@Composable
fun AquaCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AquaTypography,
        content = content
    )
}
