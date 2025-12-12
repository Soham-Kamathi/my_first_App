package com.localllm.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nothing OS-inspired Design System
 * 
 * Key characteristics:
 * - Monochromatic palette: Pure black, white, grays
 * - Single accent color: Nothing Red (#D92027)
 * - Dot-matrix / retro-futuristic typography feel
 * - High contrast, minimal, functional
 * - Mix of circular and rounded rectangular shapes
 * - Bold, uppercase labels with generous letter spacing
 */

// ============================================================================
// NOTHING COLOR PALETTE
// ============================================================================

object NothingColors {
    // Primary palette - True black and white
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    
    // Nothing Red - The signature accent
    val NothingRed = Color(0xFFD92027)
    val NothingRedLight = Color(0xFFE85057)  // For light mode
    val NothingRedDark = Color(0xFFFF3B42)   // Brighter for dark mode
    
    // Grayscale palette (precise steps)
    val Gray50 = Color(0xFFFAFAFA)   // Lightest
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEEEEEE)
    val Gray300 = Color(0xFFE0E0E0)
    val Gray400 = Color(0xFFBDBDBD)
    val Gray500 = Color(0xFF9E9E9E)
    val Gray600 = Color(0xFF757575)
    val Gray700 = Color(0xFF616161)
    val Gray800 = Color(0xFF424242)
    val Gray850 = Color(0xFF303030)
    val Gray900 = Color(0xFF212121)
    val Gray950 = Color(0xFF121212)  // Darkest surface
    
    // Semantic colors
    val Success = Color(0xFF00C853)
    val Warning = Color(0xFFFFAB00)
    val Error = NothingRed
    val Info = Color(0xFF2979FF)
    
    // Widget/Card backgrounds
    val CardLightBg = Gray100
    val CardDarkBg = Gray900
    val CardLightBgAlt = Gray200
    val CardDarkBgAlt = Gray850
}

// ============================================================================
// NOTHING DARK COLOR SCHEME
// ============================================================================

val NothingDarkColorScheme = darkColorScheme(
    // Primary - Nothing Red accent
    primary = NothingColors.NothingRedDark,
    onPrimary = NothingColors.White,
    primaryContainer = Color(0xFF5C0A0D),
    onPrimaryContainer = Color(0xFFFFDAD6),
    
    // Secondary - Muted gray
    secondary = NothingColors.Gray400,
    onSecondary = NothingColors.Black,
    secondaryContainer = NothingColors.Gray800,
    onSecondaryContainer = NothingColors.Gray200,
    
    // Tertiary - White accent for contrast
    tertiary = NothingColors.White,
    onTertiary = NothingColors.Black,
    tertiaryContainer = NothingColors.Gray700,
    onTertiaryContainer = NothingColors.White,
    
    // Error
    error = NothingColors.NothingRedDark,
    onError = NothingColors.White,
    errorContainer = Color(0xFF5C0A0D),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background & Surface - True black for OLED
    background = NothingColors.Black,
    onBackground = NothingColors.White,
    surface = NothingColors.Black,
    onSurface = NothingColors.White,
    surfaceVariant = NothingColors.Gray950,
    onSurfaceVariant = NothingColors.Gray400,
    
    // Outline
    outline = NothingColors.Gray700,
    outlineVariant = NothingColors.Gray800,
    scrim = NothingColors.Black,
    
    // Inverse
    inverseSurface = NothingColors.Gray100,
    inverseOnSurface = NothingColors.Black,
    inversePrimary = NothingColors.NothingRed,
    
    surfaceTint = NothingColors.NothingRedDark
)

// ============================================================================
// NOTHING LIGHT COLOR SCHEME
// ============================================================================

val NothingLightColorScheme = lightColorScheme(
    // Primary - Nothing Red accent
    primary = NothingColors.NothingRed,
    onPrimary = NothingColors.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    
    // Secondary - Dark gray
    secondary = NothingColors.Gray700,
    onSecondary = NothingColors.White,
    secondaryContainer = NothingColors.Gray200,
    onSecondaryContainer = NothingColors.Gray900,
    
    // Tertiary - Black accent for contrast
    tertiary = NothingColors.Black,
    onTertiary = NothingColors.White,
    tertiaryContainer = NothingColors.Gray300,
    onTertiaryContainer = NothingColors.Black,
    
    // Error
    error = NothingColors.NothingRed,
    onError = NothingColors.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background & Surface - Clean white/off-white
    background = NothingColors.Gray50,
    onBackground = NothingColors.Black,
    surface = NothingColors.White,
    onSurface = NothingColors.Black,
    surfaceVariant = NothingColors.Gray100,
    onSurfaceVariant = NothingColors.Gray700,
    
    // Outline
    outline = NothingColors.Gray400,
    outlineVariant = NothingColors.Gray200,
    scrim = NothingColors.Black,
    
    // Inverse
    inverseSurface = NothingColors.Gray900,
    inverseOnSurface = NothingColors.White,
    inversePrimary = NothingColors.NothingRedDark,
    
    surfaceTint = NothingColors.NothingRed
)

// ============================================================================
// NOTHING TYPOGRAPHY
// Inspired by dot-matrix displays and retro-futuristic interfaces
// Using monospace for a distinctive, tech-forward aesthetic
// ============================================================================

// Nothing uses Ndot font family - we'll simulate with Monospace
private val NothingFontFamily = FontFamily.Monospace

val NothingTypography = Typography(
    // Display styles - Large, bold, tight, monospace feel
    displayLarge = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headlines - Medium weight, monospace, slightly spaced
    headlineLarge = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.25.sp
    ),
    
    // Titles - Bold with letter spacing for that dot-matrix look
    titleLarge = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.75.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.75.sp
    ),
    
    // Body - Monospace for reading, clean and technical
    bodyLarge = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Labels - Uppercase-friendly, wide tracking, dot-matrix inspired
    labelLarge = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.5.sp  // Wide tracking for labels
    ),
    labelMedium = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NothingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.0.sp
    )
)

// ============================================================================
// NOTHING SHAPES
// Mix of circular elements (icons, buttons) and soft rounded rectangles (cards)
// ============================================================================

val NothingShapes = Shapes(
    // Small - Chips, small buttons
    small = RoundedCornerShape(8.dp),
    
    // Medium - Cards, dialogs
    medium = RoundedCornerShape(16.dp),
    
    // Large - Sheets, large cards
    large = RoundedCornerShape(24.dp),
    
    // Extra large - Full-width panels
    extraLarge = RoundedCornerShape(32.dp)
)

// Specific shapes for Nothing-style widgets
object NothingWidgetShapes {
    val Circular = CircleShape
    val Pill = RoundedCornerShape(50)
    val WidgetSmall = RoundedCornerShape(12.dp)
    val WidgetMedium = RoundedCornerShape(20.dp)
    val WidgetLarge = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(24.dp)
}

// ============================================================================
// NOTHING DIMENSIONS
// Consistent spacing and sizing
// ============================================================================

object NothingDimens {
    // Padding
    val paddingXs = 4.dp
    val paddingSm = 8.dp
    val paddingMd = 16.dp
    val paddingLg = 24.dp
    val paddingXl = 32.dp
    
    // Card dimensions
    val cardMinHeight = 80.dp
    val cardMediumHeight = 120.dp
    val cardLargeHeight = 160.dp
    
    // Widget dimensions (Nothing-style grid widgets)
    val widgetSmall = 80.dp   // 1x1 grid unit
    val widgetMedium = 168.dp // 2x1 grid unit (with gap)
    val widgetLarge = 256.dp  // 3x1 or 2x2
    
    // Icon sizes
    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconXl = 48.dp
    
    // Corner radii
    val radiusSm = 8.dp
    val radiusMd = 16.dp
    val radiusLg = 24.dp
    val radiusXl = 32.dp
    val radiusFull = 50  // Percentage for pill shape
    
    // Border widths
    val borderThin = 1.dp
    val borderMedium = 2.dp
    val borderThick = 3.dp
    
    // Elevation
    val elevationNone = 0.dp
    val elevationLow = 2.dp
    val elevationMedium = 4.dp
    val elevationHigh = 8.dp
}

// ============================================================================
// NOTHING GRADIENTS (minimal, mostly solid)
// ============================================================================

object NothingGradients {
    // Subtle depth gradients for cards (dark mode)
    val cardDark = listOf(
        NothingColors.Gray900,
        NothingColors.Gray950
    )
    
    // Light mode card gradient
    val cardLight = listOf(
        NothingColors.White,
        NothingColors.Gray100
    )
    
    // Accent gradient (red)
    val accent = listOf(
        NothingColors.NothingRed,
        Color(0xFFB71C1C)
    )
    
    // Monochrome gradients
    val blackToGray = listOf(
        NothingColors.Black,
        NothingColors.Gray900
    )
    
    val whiteToGray = listOf(
        NothingColors.White,
        NothingColors.Gray100
    )
}
