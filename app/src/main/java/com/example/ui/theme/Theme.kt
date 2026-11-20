package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val OvertimeLightColorScheme = lightColorScheme(
    primary = EmeraldTeal,
    onPrimary = Color.White,
    primaryContainer = EmeraldTealSoft,
    onPrimaryContainer = EmeraldTealDark,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = AmberAccentDark,
    tertiary = EmeraldTealLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFF1F5F9),
    error = StatusRejected,
    onError = Color.White
)

private val OvertimeDarkColorScheme = darkColorScheme(
    primary = EmeraldTealLight,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = EmeraldTealContainerDark,
    onPrimaryContainer = EmeraldTealSoft,
    secondary = AmberAccent,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberSoft,
    tertiary = EmeraldTeal,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val OvertimeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun OvertimeTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OvertimeDarkColorScheme else OvertimeLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    val currentDensity = LocalDensity.current
    val lockedDensity = Density(density = currentDensity.density, fontScale = 1.0f)

    CompositionLocalProvider(LocalDensity provides lockedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = OvertimeShapes,
            content = content
        )
    }
}
