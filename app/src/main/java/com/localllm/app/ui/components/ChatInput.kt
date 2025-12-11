package com.localllm.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.localllm.app.util.VoiceInputState
import com.localllm.app.util.VoiceSpeechRecognizer

/**
 * Chat input component with send button, voice input, and generation controls.
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
    val context = LocalContext.current
    
    // Voice input state
    var hasAudioPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Voice recognizer
    val voiceRecognizer = remember {
        VoiceSpeechRecognizer(
            context = context,
            onResult = { recognizedText ->
                // Append recognized text to input field
                text = if (text.isBlank()) recognizedText else "$text $recognizedText"
            },
            onError = { errorMessage ->
                // Could show snackbar with error
            }
        )
    }
    
    val voiceState by voiceRecognizer.state.collectAsState()
    val partialResults by voiceRecognizer.partialResults.collectAsState()
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            voiceRecognizer.startListening()
        } else {
            showPermissionDialog = true
        }
    }
    
    // Clean up voice recognizer
    DisposableEffect(Unit) {
        onDispose {
            voiceRecognizer.release()
        }
    }
    
    // Enhanced background with gradient - theme-aware
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
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
            // Voice input button
            if (!isGenerating) {
                val isListening = voiceState is VoiceInputState.Listening
                val micColor by animateColorAsState(
                    targetValue = if (isListening) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary,
                    label = "micColor"
                )
                
                // Pulsing animation when listening
                val scale by animateFloatAsState(
                    targetValue = if (isListening) 1.1f else 1f,
                    animationSpec = if (isListening) {
                        infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        )
                    } else {
                        spring()
                    },
                    label = "micScale"
                )
                
                IconButton(
                    onClick = {
                        when (voiceState) {
                            is VoiceInputState.Listening -> {
                                voiceRecognizer.stopListening()
                            }
                            else -> {
                                if (hasAudioPermission) {
                                    voiceRecognizer.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    },
                    enabled = enabled && voiceRecognizer.isAvailable(),
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Voice input",
                        tint = if (enabled && voiceRecognizer.isAvailable()) micColor 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Enhanced text input
            OutlinedTextField(
                value = if (voiceState is VoiceInputState.Listening && partialResults.isNotBlank()) {
                    partialResults
                } else {
                    text
                },
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled && !isGenerating && voiceState !is VoiceInputState.Listening,
                placeholder = { 
                    Text(
                        when {
                            !enabled -> "Load a model to chat"
                            isGenerating -> "Generating..."
                            voiceState is VoiceInputState.Listening -> "Listening..."
                            voiceState is VoiceInputState.Processing -> "Processing..."
                            else -> "Type a message or tap mic..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ) 
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
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
                    if (text.isNotBlank() && !isGenerating && voiceState !is VoiceInputState.Listening) {
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
                MaterialTheme.colorScheme.error // Error color for stop
            } else {
                MaterialTheme.colorScheme.primary // Primary for send
            }
            
            val contentColor = if (isGenerating) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onPrimary
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
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
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
        
        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Microphone Permission Required") },
                text = { Text("Voice input requires microphone permission. Please grant permission in app settings.") },
                confirmButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("OK")
                    }
                }
            )
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}
