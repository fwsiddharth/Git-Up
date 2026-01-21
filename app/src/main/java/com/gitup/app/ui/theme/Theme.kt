package com.gitup.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = DarkColors.background,
    surface = DarkColors.surface,
    surfaceVariant = DarkColors.surfaceVariant,
    surfaceContainerLow = DarkColors.surfaceContainerLow,
    surfaceContainerHigh = DarkColors.surfaceContainerHigh,
    surfaceBright = DarkColors.surfaceBright,
    
    outline = DarkColors.outline,
    outlineVariant = DarkColors.outlineVariant,
    
    onSurface = DarkColors.onSurface,
    onSurfaceVariant = DarkColors.onSurfaceVariant,
    onBackground = DarkColors.onBackground,
    
    primary = DarkColors.primary,
    onPrimary = DarkColors.onPrimary,
    primaryContainer = DarkColors.primaryContainer,
    onPrimaryContainer = DarkColors.onPrimaryContainer,
    
    scrim = DarkColors.scrim,
    inverseSurface = DarkColors.inverseSurface,
    inverseOnSurface = DarkColors.inverseOnSurface,
    
    // Set all other colors to monochrome equivalents
    secondary = DarkColors.primary,
    onSecondary = DarkColors.onPrimary,
    secondaryContainer = DarkColors.primaryContainer,
    onSecondaryContainer = DarkColors.onPrimaryContainer,
    
    tertiary = DarkColors.primary,
    onTertiary = DarkColors.onPrimary,
    tertiaryContainer = DarkColors.primaryContainer,
    onTertiaryContainer = DarkColors.onPrimaryContainer,
    
    error = DarkColors.onSurface,
    onError = DarkColors.surface,
    errorContainer = DarkColors.surfaceContainerHigh,
    onErrorContainer = DarkColors.onSurface
)

private val LightColorScheme = lightColorScheme(
    background = LightColors.background,
    surface = LightColors.surface,
    surfaceVariant = LightColors.surfaceVariant,
    surfaceContainerLow = LightColors.surfaceContainerLow,
    surfaceContainerHigh = LightColors.surfaceContainerHigh,
    surfaceBright = LightColors.surfaceBright,
    
    outline = LightColors.outline,
    outlineVariant = LightColors.outlineVariant,
    
    onSurface = LightColors.onSurface,
    onSurfaceVariant = LightColors.onSurfaceVariant,
    onBackground = LightColors.onBackground,
    
    primary = LightColors.primary,
    onPrimary = LightColors.onPrimary,
    primaryContainer = LightColors.primaryContainer,
    onPrimaryContainer = LightColors.onPrimaryContainer,
    
    scrim = LightColors.scrim,
    inverseSurface = LightColors.inverseSurface,
    inverseOnSurface = LightColors.inverseOnSurface,
    
    // Set all other colors to monochrome equivalents
    secondary = LightColors.primary,
    onSecondary = LightColors.onPrimary,
    secondaryContainer = LightColors.primaryContainer,
    onSecondaryContainer = LightColors.onPrimaryContainer,
    
    tertiary = LightColors.primary,
    onTertiary = LightColors.onPrimary,
    tertiaryContainer = LightColors.primaryContainer,
    onTertiaryContainer = LightColors.onPrimaryContainer,
    
    error = LightColors.onSurface,
    onError = LightColors.surface,
    errorContainer = LightColors.surfaceContainerHigh,
    onErrorContainer = LightColors.onSurface
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GitUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
