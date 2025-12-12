package com.localllm.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localllm.app.data.model.AppearanceStyle
import com.localllm.app.ui.theme.*

/**
 * Nothing OS-inspired UI Components
 * 
 * Design principles:
 * - High contrast black/white with red accent
 * - Dot-matrix inspired elements
 * - Mix of circular and rounded rectangular shapes
 * - Minimal, functional aesthetic
 * - Bold typography with generous spacing
 */

// ============================================================================
// NOTHING WIDGET CARD
// Grid-style widget cards like Nothing OS home screen
// ============================================================================

@Composable
fun NothingWidgetCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: NothingWidgetSize = NothingWidgetSize.MEDIUM,
    accentColor: Color = NothingColors.NothingRed,
    showArrow: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "widget_scale"
    )
    
    val dimensions = when (size) {
        NothingWidgetSize.SMALL -> 100.dp to 100.dp
        NothingWidgetSize.MEDIUM -> 160.dp to 120.dp
        NothingWidgetSize.LARGE -> 200.dp to 160.dp
        NothingWidgetSize.WIDE -> 320.dp to 120.dp
    }
    
    Card(
        modifier = modifier
            .width(dimensions.first)
            .height(dimensions.second)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = NothingWidgetShapes.WidgetMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(NothingDimens.paddingMd)
        ) {
            // Icon in top-left
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Arrow in top-right (optional)
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                )
            }
            
            // Title and subtitle at bottom
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

enum class NothingWidgetSize {
    SMALL,   // 1x1 grid
    MEDIUM,  // 2x1 grid
    LARGE,   // 2x2 grid
    WIDE     // 4x1 grid
}

// ============================================================================
// NOTHING FEATURE CARD
// Feature cards with Nothing OS styling (alternative to default FeatureCard)
// ============================================================================

@Composable
fun NothingFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    stats: String? = null,
    useAccentBg: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "feature_scale"
    )
    
    val bgColor = if (useAccentBg) {
        NothingColors.NothingRed
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (useAccentBg) {
        NothingColors.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = modifier
            .width(280.dp)
            .height(160.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = NothingWidgetShapes.CardLarge,
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (useAccentBg) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(NothingDimens.paddingLg)
        ) {
            // Top row: Icon and badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (useAccentBg) NothingColors.White.copy(alpha = 0.2f)
                            else NothingColors.NothingRed.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (useAccentBg) NothingColors.White else NothingColors.NothingRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Badge
                if (badge != null) {
                    Surface(
                        shape = NothingWidgetShapes.Pill,
                        color = if (useAccentBg) NothingColors.White else NothingColors.NothingRed
                    ) {
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (useAccentBg) NothingColors.NothingRed else NothingColors.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Bottom: Title, subtitle, and stats
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (stats != null) {
                        // Stats badge (dot-matrix inspired)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (useAccentBg) NothingColors.White.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = stats,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// NOTHING CIRCULAR BUTTON
// Round buttons inspired by Nothing OS control center
// ============================================================================

@Composable
fun NothingCircularButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isActive: Boolean = false,
    size: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "btn_scale"
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (isActive) NothingColors.NothingRed else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "btn_bg"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isActive) NothingColors.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "btn_icon"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(size * 0.45f)
            )
        }
        
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ============================================================================
// NOTHING CHIP / TAG
// Minimalist chips for categories, filters, badges
// ============================================================================

@Composable
fun NothingChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val bgColor by animateColorAsState(
        targetValue = if (selected) NothingColors.NothingRed else Color.Transparent,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) NothingColors.NothingRed else MaterialTheme.colorScheme.outline,
        animationSpec = tween(150),
        label = "chip_border"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) NothingColors.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(150),
        label = "chip_content"
    )
    
    Surface(
        modifier = modifier
            .clip(NothingWidgetShapes.Pill)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = NothingWidgetShapes.Pill
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = bgColor,
        shape = NothingWidgetShapes.Pill
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// ============================================================================
// NOTHING TOGGLE SWITCH
// Custom switch with Nothing OS styling
// ============================================================================

@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding * 2 else 0.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "thumb_offset"
    )
    
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            checked -> NothingColors.NothingRed
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "track_color"
    )
    
    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else -> NothingColors.White
        },
        animationSpec = tween(200),
        label = "thumb_color"
    )
    
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(NothingWidgetShapes.Pill)
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(thumbPadding)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NothingColors.NothingRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ============================================================================
// NOTHING SECTION HEADER
// Section divider with Nothing OS styling
// ============================================================================

@Composable
fun NothingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NothingDimens.paddingMd, vertical = NothingDimens.paddingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        
        action?.invoke()
    }
}

// ============================================================================
// NOTHING STAT CARD
// Small info cards showing stats/metrics
// ============================================================================

@Composable
fun NothingStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = NothingWidgetShapes.WidgetSmall,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .padding(NothingDimens.paddingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NothingColors.NothingRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ============================================================================
// NOTHING DOT INDICATOR
// Dot-matrix inspired loading/status indicator
// ============================================================================

@Composable
fun NothingDotIndicator(
    count: Int = 3,
    activeIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(count) { index ->
            val isActive = index == activeIndex
            val dotSize by animateDpAsState(
                targetValue = if (isActive) 10.dp else 6.dp,
                animationSpec = tween(200),
                label = "dot_size_$index"
            )
            val dotColor by animateColorAsState(
                targetValue = if (isActive) NothingColors.NothingRed else MaterialTheme.colorScheme.outline,
                animationSpec = tween(200),
                label = "dot_color_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ============================================================================
// NOTHING OUTLINED CARD
// Card with prominent border instead of background
// ============================================================================

@Composable
fun NothingOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isPressed) NothingColors.NothingRed else borderColor,
        animationSpec = tween(150),
        label = "border_color"
    )
    
    Surface(
        modifier = modifier
            .clip(NothingWidgetShapes.Card)
            .border(
                width = 2.dp,
                color = animatedBorderColor,
                shape = NothingWidgetShapes.Card
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = Color.Transparent,
        shape = NothingWidgetShapes.Card
    ) {
        Column(
            modifier = Modifier.padding(NothingDimens.paddingMd),
            content = content
        )
    }
}
