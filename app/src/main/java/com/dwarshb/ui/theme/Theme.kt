package com.dwarshb.ui.theme

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
    primary = PayPalBlue,
    onPrimary = Color.White,
    primaryContainer = PayPalNavy,
    onPrimaryContainer = Color.White,
    secondary = PayPalLightBlue,
    onSecondary = Slate900,
    tertiary = PayPalGold,
    onTertiary = Slate900,
    background = BackgroundDark,
    surface = SurfaceCardDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = PayPalNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EBFB),
    onPrimaryContainer = PayPalNavy,
    secondary = PayPalBlue,
    onSecondary = Color.White,
    tertiary = PayPalGold,
    onTertiary = Slate900,
    background = BackgroundLight,
    surface = SurfaceCardLight,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200
)

@Composable
fun AgentCartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded PayPal colors consistent by default
    content: @Composable () -> Unit
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
