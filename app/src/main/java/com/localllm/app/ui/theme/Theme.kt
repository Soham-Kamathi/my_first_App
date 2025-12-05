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

// Primary colors - Professional Indigo/Slate
private val PrimaryLight = Color(0xFF4F46E5) // Indigo 600
private val PrimaryDark = Color(0xFF818CF8)  // Indigo 400
private val SecondaryLight = Color(0xFF64748B) // Slate 500
private val SecondaryDark = Color(0xFF94A3B8)  // Slate 400
private val TertiaryLight = Color(0xFF0EA5E9) // Sky 500
private val TertiaryDark = Color(0xFF38BDF8)  // Sky 400

// Backgrounds
private val BackgroundLight = Color(0xFFF8FAFC) // Slate 50
private val BackgroundDark = Color(0xFF0F172A)  // Slate 900
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF1E293B)     // Slate 800
private val SurfaceVariantLight = Color(0xFFF1F5F9) // Slate 100
private val SurfaceVariantDark = Color(0xFF334155)  // Slate 700

// Message Colors
val UserMessageBackground = PrimaryLight
val UserMessageBackgroundDark = PrimaryDark
val AssistantMessageBackground = SurfaceVariantLight
val AssistantMessageBackgroundDark = SurfaceVariantDark

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF1E1B4B), // Indigo 950
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    secondary = SecondaryDark,
    onSecondary = Color(0xFF0F172A), // Slate 900
    tertiary = TertiaryDark,
    background = BackgroundDark,
    onBackground = Color(0xFFF8FAFC), // Slate 50
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1), // Slate 300
    outline = Color(0xFF64748B) // Slate 500
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo 100
    onPrimaryContainer = Color(0xFF312E81), // Indigo 900
    secondary = SecondaryLight,
    onSecondary = Color.White,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569), // Slate 600
    outline = Color(0xFF94A3B8) // Slate 400
)

@Composable
fun LocalLLMTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
