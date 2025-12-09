package com.localllm.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Chat input component with send button and generation controls.
 */
@Composable
fun ChatInput(
    enabled: Boolean,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    modifier: Modifier = Modifier,
    webSearchEnabled: Boolean = false,
    onToggleWebSearch: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    
    // Enhanced background with gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF121212).copy(alpha = 0.98f),
            Color(0xFF1A1A1A).copy(alpha = 0.98f)
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundGradient)
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        // Web Search Toggle Row
        if (onToggleWebSearch != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Web search toggle chip
                val chipColor by animateColorAsState(
                    targetValue = if (webSearchEnabled) Color(0xFF00E5FF).copy(alpha = 0.15f) 
                                  else Color.Transparent,
                    label = "chipColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (webSearchEnabled) Color(0xFF00E5FF) 
                                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    label = "borderColor"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (webSearchEnabled) Color(0xFF00E5FF) 
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    label = "iconTint"
                )
                
                Surface(
                    onClick = onToggleWebSearch,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = chipColor,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(borderColor, borderColor))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint
                        )
                        Text(
                            text = "Web Search",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (webSearchEnabled) Color(0xFF00E5FF) 
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (webSearchEnabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status text when enabled
                if (webSearchEnabled) {
                    Text(
                        text = "Search enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF).copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Enhanced text input
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled && !isGenerating,
                placeholder = { 
                    Text(
                        if (!enabled) "Load a model to chat" 
                        else if (isGenerating) "Generating..."
                        else "Type a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ) 
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    cursorColor = Color(0xFF00E5FF)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && enabled && !isGenerating) {
                            onSendMessage(text)
                            text = ""
                        }
                    }
                ),
                maxLines = 5,
                trailingIcon = {
                    if (text.isNotBlank() && !isGenerating) {
                        IconButton(
                            onClick = { text = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Enhanced send/stop button with gradient
            val containerColor = if (isGenerating) {
                Color(0xFFFF1744) // Bright red for stop
            } else {
                Color(0xFF00E5FF) // Cyan for send
            }
            
            val contentColor = if (isGenerating) {
                Color.White
            } else {
                Color.Black
            }
            
            FilledIconButton(
                onClick = {
                    if (isGenerating) {
                        onStopGeneration()
                    } else if (text.isNotBlank() && enabled) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = isGenerating || (text.isNotBlank() && enabled),
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                    disabledContainerColor = Color(0xFF1E1E1E),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isGenerating) "Stop" else "Send",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Typing indicator with animated dots.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val delay = index * 200
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.3f at 0
                        1f at 300
                        0.3f at 600
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = alpha))
            )
        }
    }
}
