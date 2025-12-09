package com.localllm.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val isSearchingWeb by viewModel.isSearchingWeb.collectAsState()
    
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
                webSearchEnabled = userPreferences.webSearchEnabled,
                onToggleWebSearch = { viewModel.toggleWebSearch() },
                onSendMessage = { text ->
                    if (viewModel.isModelLoaded) {
                        viewModel.sendMessage(text)
                    } else {
                        showModelWarning = true
                    }
                },
                onStopGeneration = { viewModel.stopGeneration() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Model loading indicator with enhanced colors
            if (modelLoadingState is ModelLoadingState.Loading) {
                LinearProgressIndicator(
                    progress = (modelLoadingState as ModelLoadingState.Loading).progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1E1E1E)
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { index, msg -> "${index}_${msg.id}" }
                    ) { _, message ->
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
                    
                    // Web search indicator
                    if (isSearchingWeb) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Searching the web...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = modelName ?: "LocalLLM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                if (modelLoadingState is ModelLoadingState.Loading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF)
                    )
                } else if (isModelLoaded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    Icons.Default.Menu, 
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onModelClick) {
                Icon(
                    Icons.Outlined.Memory, 
                    contentDescription = "Models",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings, 
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun NoModelBanner(
    onSelectModel: () -> Unit
) {
    // Enhanced gradient banner
    val bannerGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF1744).copy(alpha = 0.15f),
            Color(0xFFE91E63).copy(alpha = 0.1f)
        )
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.background(bannerGradient)) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1744).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No model loaded",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a model to start chatting",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0B0B0)
                    )
                }
                Button(
                    onClick = onSelectModel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF1744),
                        contentColor = Color.White
                    )
                ) {
                    Text("Select", fontWeight = FontWeight.SemiBold)
                }
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
            // Enhanced icon with gradient background
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF00E5FF)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Start a Conversation",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (modelName != null) {
                    "Chat with $modelName"
                } else {
                    "Load a model to begin"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0)
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingIndicator()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$tokensGenerated tokens • ${String.format("%.1f", tokensPerSecond)} t/s",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}
