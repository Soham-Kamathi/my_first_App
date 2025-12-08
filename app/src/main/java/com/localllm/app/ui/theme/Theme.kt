package com.localllm.app.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.localllm.app.data.model.AppTheme

// Modern Dark Theme Colors - Inspired by premium AI interfaces
// Primary - Cyan/Teal accent (like the reference images)
private val PrimaryLight = Color(0xFF00BCD4) // Cyan 500
private val PrimaryDark = Color(0xFF00E5FF)  // Cyan A400 - Bright cyan for dark mode
private val OnPrimaryDark = Color(0xFF001F24) // Very dark cyan

// Secondary - Muted blue-grey
private val SecondaryLight = Color(0xFF546E7A) // Blue Grey 600
private val SecondaryDark = Color(0xFF78909C)  // Blue Grey 400

// Tertiary - Vibrant accent
private val TertiaryLight = Color(0xFF00ACC1) // Cyan 600
private val TertiaryDark = Color(0xFF00E5FF)  // Cyan A400

// Backgrounds - Deep, rich blacks and dark surfaces
private val BackgroundLight = Color(0xFFF5F5F5) // Very light grey
private val BackgroundDark = Color(0xFF000000)  // True black for OLED
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF121212)     // Dark surface (Material Design standard)
private val SurfaceVariantLight = Color(0xFFECEFF1) // Blue Grey 50
private val SurfaceVariantDark = Color(0xFF1E1E1E)  // Slightly lighter than surface

// Container colors for cards and elevated surfaces
private val SurfaceContainerDark = Color(0xFF1A1A1A)
private val SurfaceContainerHighDark = Color(0xFF242424)
private val SurfaceContainerHighestDark = Color(0xFF2C2C2C)

// Message Colors
val UserMessageBackground = Color(0xFF1976D2) // Blue 700
val UserMessageBackgroundDark = Color(0xFF1E3A5F) // Dark blue with subtle cyan tint
val AssistantMessageBackground = Color(0xFFF5F5F5)
val AssistantMessageBackgroundDark = Color(0xFF1E1E1E) // Dark grey

// Error colors
private val ErrorLight = Color(0xFFD32F2F) // Red 700
private val ErrorDark = Color(0xFFEF5350) // Red 400

private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF004D5C), // Dark cyan container
    onPrimaryContainer = Color(0xFF6FF7FF), // Light cyan
    
    // Secondary
    secondary = SecondaryDark,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF263238), // Blue Grey 900
    onSecondaryContainer = Color(0xFFB0BEC5), // Blue Grey 200
    
    // Tertiary
    tertiary = TertiaryDark,
    onTertiary = Color(0xFF001F24),
    tertiaryContainer = Color(0xFF004D5C),
    onTertiaryContainer = Color(0xFF6FF7FF),
    
    // Error
    error = ErrorDark,
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF5D1F1F),
    onErrorContainer = Color(0xFFFFB4AB),
    
    // Background & Surface
    background = BackgroundDark,
    onBackground = Color(0xFFE8E8E8), // Soft white, not pure white
    surface = SurfaceDark,
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFBDBDBD), // Grey 400 - softer than white
    
    // Surface containers (for elevated cards)

    
    // Outline & other
    outline = Color(0xFF424242), // Grey 800 - subtle outlines
    outlineVariant = Color(0xFF2C2C2C),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = PrimaryLight,
    surfaceTint = PrimaryDark
)

private val LightColorScheme = lightColorScheme(
    // Primary
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2), // Cyan 100
    onPrimaryContainer = Color(0xFF00363D),
    
    // Secondary
    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFD8DC), // Blue Grey 100
    onSecondaryContainer = Color(0xFF1C313A),
    
    // Tertiary
    tertiary = TertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2EBF2),
    onTertiaryContainer = Color(0xFF00363D),
    
    // Error
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background & Surface
    background = BackgroundLight,
    onBackground = Color(0xFF1A1A1A),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF455A64), // Blue Grey 700
    
    // Outline & other
    outline = Color(0xFF90A4AE), // Blue Grey 300
    outlineVariant = Color(0xFFCFD8DC),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C2C2C),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = PrimaryDark,
    surfaceTint = PrimaryLight
)

@Composable
fun LocalLLMTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false, // Disabled by default for consistent branding
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use true black status bar for immersive dark mode
            window.statusBarColor = if (darkTheme) {
                Color.Black.toArgb()
            } else {
                colorScheme.surface.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            
            // Optional: Make navigation bar transparent/black too
            window.navigationBarColor = if (darkTheme) {
                Color.Black.toArgb()
            } else {
                colorScheme.surface.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
