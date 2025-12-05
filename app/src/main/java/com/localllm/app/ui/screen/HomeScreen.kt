package com.localllm.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.ui.viewmodel.HomeViewModel

/**
 * Feature card data class
 */
data class FeatureCard(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val route: String
)

/**
 * Home Screen inspired by Google Edge Gallery
 * Provides access to all AI features: Chat, Prompt Lab, Ask Image, Audio Scribe
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String?) -> Unit,
    onNavigateToPromptLab: () -> Unit,
    onNavigateToAskImage: () -> Unit,
    onNavigateToAudioScribe: () -> Unit,
    onNavigateToDocumentChat: () -> Unit = {},
    onNavigateToCodeCompanion: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToModels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()
    val recentConversations by viewModel.recentConversations.collectAsState()
    
    val features = remember {
        listOf(
            FeatureCard(
                id = "chat",
                title = "AI Chat",
                description = "Multi-turn conversations with AI assistant",
                icon = Icons.Outlined.Chat,
                gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                route = "chat"
            ),
            FeatureCard(
                id = "prompt_lab",
                title = "Prompt Lab",
                description = "Experiment with prompts, templates & parameters",
                icon = Icons.Outlined.Science,
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                route = "prompt_lab"
            ),
            FeatureCard(
                id = "document_chat",
                title = "Document Chat",
                description = "Chat with PDFs, text files, and documents",
                icon = Icons.Outlined.Description,
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                route = "document_chat"
            ),
            FeatureCard(
                id = "code_companion",
                title = "Code Companion",
                description = "Explain, debug, optimize, and convert code",
                icon = Icons.Outlined.Code,
                gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                route = "code_companion"
            ),
            FeatureCard(
                id = "templates",
                title = "Templates",
                description = "Pre-built conversation starters",
                icon = Icons.Outlined.Dashboard,
                gradient = listOf(Color(0xFF14B8A6), Color(0xFF0D9488)),
                route = "templates"
            ),
            FeatureCard(
                id = "ask_image",
                title = "Ask Image",
                description = "Upload images and ask AI questions about them",
                icon = Icons.Outlined.Image,
                gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                route = "ask_image"
            ),
            FeatureCard(
                id = "audio_scribe",
                title = "Audio Scribe",
                description = "Transcribe speech to text locally",
                icon = Icons.Outlined.Mic,
                gradient = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                route = "audio_scribe"
            )
        )
    }
    
    Scaffold(
        topBar = {
            HomeTopBar(
                modelName = currentModel?.name,
                modelLoadingState = modelLoadingState,
                onModelsClick = onNavigateToModels,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Section
            item {
                HeroSection(
                    modelName = currentModel?.name,
                    modelLoadingState = modelLoadingState,
                    onLoadModel = onNavigateToModels
                )
            }
            
            // Features Grid
            item {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                FeaturesGrid(
                    features = features,
                    onFeatureClick = { feature ->
                        when (feature.id) {
                            "chat" -> onNavigateToChat(null)
                            "prompt_lab" -> onNavigateToPromptLab()
                            "document_chat" -> onNavigateToDocumentChat()
                            "code_companion" -> onNavigateToCodeCompanion()
                            "templates" -> onNavigateToTemplates()
                            "ask_image" -> onNavigateToAskImage()
                            "audio_scribe" -> onNavigateToAudioScribe()
                        }
                    }
                )
            }
            
            // Quick Actions
            item {
                QuickActionsSection(
                    onNewChat = { onNavigateToChat(null) },
                    onHistory = onNavigateToHistory,
                    onModels = onNavigateToModels
                )
            }
            
            // Recent Conversations
            if (recentConversations.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Conversations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    RecentConversationsRow(
                        conversations = recentConversations,
                        onConversationClick = { conversationId -> 
                            onNavigateToChat(conversationId) 
                        }
                    )
                }
            }
            
            // Tips Section
            item {
                TipsSection()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    modelName: String?,
    modelLoadingState: ModelLoadingState,
    onModelsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "LocalLLM",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (modelLoadingState) {
                        is ModelLoadingState.Loading -> "Loading model..."
                        is ModelLoadingState.Loaded -> modelName ?: "Ready"
                        is ModelLoadingState.Error -> "Error loading model"
                        else -> "No model loaded"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onModelsClick) {
                Icon(Icons.Default.Memory, contentDescription = "Models")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
private fun HeroSection(
    modelName: String?,
    modelLoadingState: ModelLoadingState,
    onLoadModel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "On-Device AI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Run powerful AI models locally with complete privacy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            if (modelName == null && modelLoadingState !is ModelLoadingState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onLoadModel,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load a Model")
                }
            } else if (modelLoadingState is ModelLoadingState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = (modelLoadingState as ModelLoadingState.Loading).progress,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        }
    }
}

@Composable
private fun FeaturesGrid(
    features: List<FeatureCard>,
    onFeatureClick: (FeatureCard) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCardItem(
                feature = features[0],
                onClick = { onFeatureClick(features[0]) },
                modifier = Modifier.weight(1f)
            )
            FeatureCardItem(
                feature = features[1],
                onClick = { onFeatureClick(features[1]) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCardItem(
                feature = features[2],
                onClick = { onFeatureClick(features[2]) },
                modifier = Modifier.weight(1f)
            )
            FeatureCardItem(
                feature = features[3],
                onClick = { onFeatureClick(features[3]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureCardItem(
    feature: FeatureCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(feature.gradient)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNewChat: () -> Unit,
    onHistory: () -> Unit,
    onModels: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Outlined.Add,
                label = "New Chat",
                onClick = onNewChat
            )
            QuickActionButton(
                icon = Icons.Outlined.History,
                label = "History",
                onClick = onHistory
            )
            QuickActionButton(
                icon = Icons.Outlined.Memory,
                label = "Models",
                onClick = onModels
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentConversationsRow(
    conversations: List<HomeViewModel.RecentConversation>,
    onConversationClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(conversations) { conversation ->
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .clickable { onConversationClick(conversation.id) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conversation.timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun TipsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Pro Tip",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Smaller models (0.5B-3B) run faster on most devices. Try Qwen 0.5B for quick responses!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
