package com.localllm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.MessageRole
import com.localllm.app.ui.theme.AssistantMessageBackground
import com.localllm.app.ui.theme.AssistantMessageBackgroundDark
import com.localllm.app.ui.theme.UserMessageBackground
import com.localllm.app.ui.theme.UserMessageBackgroundDark

/**
 * Message bubble component for displaying chat messages.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    showTokensPerSecond: Boolean = true,
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    var showActions by remember { mutableStateOf(false) }
    
    // Enhanced gradient backgrounds
    val userGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF00B8D4)
        )
    )
    
    val assistantGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1E1E),
            Color(0xFF2A2A2A)
        )
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Enhanced message bubble
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = if (isUser) 24.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 24.dp
                    )
                ),
            color = if (isUser) {
                Color.Transparent
            } else {
                Color(0xFF1E1E1E)
            },
            onClick = { showActions = !showActions }
        ) {
            Box(
                modifier = if (isUser) {
                    Modifier.background(userGradient)
                } else {
                    Modifier
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Role label for assistant
                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF00E5FF))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Assistant",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    
                    // Message content
                    Text(
                        text = message.content.ifBlank { "..." },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                        ),
                        color = if (isUser) {
                            Color.Black
                        } else {
                            Color.White
                        }
                    )
                    
                    // Enhanced generation stats for assistant messages
                    if (!isUser && showTokensPerSecond && message.isComplete && message.tokensGenerated > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.tokensGenerated} tokens • ${String.format("%.1f", message.tokensPerSecond())} t/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }
        }
        
        // Enhanced action buttons
        if (showActions && message.isComplete) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy button
                Surface(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }
                
                // Regenerate button (only for last assistant message)
                if (onRegenerate != null && !isUser) {
                    Surface(
                        onClick = onRegenerate,
                        modifier = Modifier.size(36.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0xFF1E1E1E),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
                
                // Delete button
                Surface(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFF1744)
                        )
                    }
                }
            }
        }
    }
}

/**
 * System message display for displaying system prompts or notices.
 */
@Composable
fun SystemMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
