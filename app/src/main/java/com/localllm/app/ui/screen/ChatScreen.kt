package com.localllm.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.GenerationState
import com.localllm.app.data.model.MessageRole
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.ui.components.ChatInput
import com.localllm.app.ui.components.MessageBubble
import com.localllm.app.ui.components.TypingIndicator
import com.localllm.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/**
 * Main chat screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToModels: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var showModelWarning by remember { mutableStateOf(false) }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Create new conversation if none exists
    LaunchedEffect(Unit) {
        if (viewModel.currentConversationId.value == null) {
            viewModel.createNewConversation()
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                modelName = currentModel?.name,
                isModelLoaded = viewModel.isModelLoaded,
                modelLoadingState = modelLoadingState,
                onModelClick = onNavigateToModels,
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings,
                onNewChatClick = { viewModel.createNewConversation() }
            )
        },
        bottomBar = {
            ChatInput(
                enabled = viewModel.isModelLoaded && generationState !is GenerationState.Generating,
                isGenerating = generationState is GenerationState.Generating,
                onSendMessage = { text ->
                    if (viewModel.isModelLoaded) {
                        viewModel.sendMessage(text)
                    } else {
                        showModelWarning = true
                    }
                },
                onStopGeneration = { viewModel.stopGeneration() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Model loading indicator
            if (modelLoadingState is ModelLoadingState.Loading) {
                LinearProgressIndicator(
                    progress = (modelLoadingState as ModelLoadingState.Loading).progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // No model warning
            if (!viewModel.isModelLoaded && modelLoadingState !is ModelLoadingState.Loading) {
                NoModelBanner(onSelectModel = onNavigateToModels)
            }
            
            // Messages list
            if (messages.isEmpty()) {
                EmptyConversationPlaceholder(
                    modifier = Modifier.weight(1f),
                    modelName = currentModel?.name
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            showTokensPerSecond = userPreferences.showTokensPerSecond,
                            onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onRegenerate = if (message.role == MessageRole.ASSISTANT && 
                                              message == messages.lastOrNull()) {
                                { viewModel.regenerateLastResponse() }
                            } else null
                        )
                    }
                    
                    // Typing indicator when generating
                    if (generationState is GenerationState.Generating) {
                        item {
                            val state = generationState as GenerationState.Generating
                            GenerationStats(
                                tokensGenerated = state.tokensGenerated,
                                tokensPerSecond = state.tokensPerSecond
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Model warning dialog
    if (showModelWarning) {
        AlertDialog(
            onDismissRequest = { showModelWarning = false },
            title = { Text("No Model Loaded") },
            text = { Text("Please load a model before starting a conversation.") },
            confirmButton = {
                TextButton(onClick = {
                    showModelWarning = false
                    onNavigateToModels()
                }) {
                    Text("Select Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    modelName: String?,
    isModelLoaded: Boolean,
    modelLoadingState: ModelLoadingState,
    onModelClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = modelName ?: "No Model",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when (modelLoadingState) {
                    is ModelLoadingState.Loading -> {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ModelLoadingState.Loaded -> {
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is ModelLoadingState.Error -> {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.Menu, contentDescription = "History")
            }
        },
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
            IconButton(onClick = onModelClick) {
                Icon(Icons.Default.Memory, contentDescription = "Models")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
private fun NoModelBanner(
    onSelectModel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No model loaded",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Select a model to start chatting",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onSelectModel) {
                Text("Select")
            }
        }
    }
}

@Composable
private fun EmptyConversationPlaceholder(
    modifier: Modifier = Modifier,
    modelName: String?
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start a Conversation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (modelName != null) {
                    "Chat with $modelName"
                } else {
                    "Load a model to begin"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GenerationStats(
    tokensGenerated: Int,
    tokensPerSecond: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$tokensGenerated tokens • ${String.format("%.1f", tokensPerSecond)} t/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
