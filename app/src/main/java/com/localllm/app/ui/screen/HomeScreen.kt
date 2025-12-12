package com.localllm.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.R
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.ui.components.FeatureCard
import com.localllm.app.ui.viewmodel.HomeViewModel
import com.localllm.app.ui.theme.LocalAppearanceStyle
import com.localllm.app.data.model.AppearanceStyle
import org.bouncycastle.math.raw.Mod

/**
 * Core feature card data class for primary features
 */
data class CoreFeatureCard(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val stats: String? = null,
    val badge: String? = null
)

/**
 * Secondary feature chip data class
 */
data class SecondaryFeatureChip(
    val id: String,
    val label: String,
    val icon: ImageVector
)



/**
 * Home Screen - High-Efficiency Dashboard for LocalLLM
 * Design prioritizes on-device status, fast feature access, and Material 3 Dark Mode aesthetic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String?) -> Unit,
    onNavigateToPromptLab: () -> Unit,
    onNavigateToAskImage: () -> Unit,
    onNavigateToAudioScribe: () -> Unit,
    onNavigateToCodeCompanion: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToQuiz: () -> Unit = {},
    onNavigateToRAGChat: () -> Unit = {},
    onNavigateToModels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()
    val recentConversations by viewModel.recentConversations.collectAsState()

    // Search state
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Get current appearance style
    val appearanceStyle = LocalAppearanceStyle.current
    val isNothingTheme = appearanceStyle == AppearanceStyle.NOTHING

    // Core Features (horizontal scrolling cards with gradients)
    val coreFeatures = remember(isNothingTheme) {
        if (isNothingTheme) {
            // Nothing theme: Black/Grey/White/Red color palette
            listOf(
                CoreFeatureCard(
                    id = "chat",
                    title = "AI Chat",
                    description = "Start intelligent conversations",
                    icon = Icons.Outlined.Chat,
                    gradient = listOf(Color(0xFFD92027), Color(0xFFB71C1C)),
                    badge = "Popular",
                    stats = "24/7"
                ),
                CoreFeatureCard(
                    id = "rag_chat",
                    title = "Document Chat",
                    description = "AI-powered semantic search",
                    icon = Icons.Outlined.Description,
                    gradient = listOf(Color(0xFF424242), Color(0xFF212121)),
                    badge = "AI",
                    stats = "RAG"
                ),
                CoreFeatureCard(
                    id = "prompt_lab",
                    title = "Prompt Lab",
                    description = "Experiment & fine-tune",
                    icon = Icons.Outlined.Science,
                    gradient = listOf(Color(0xFF757575), Color(0xFF424242)),
                    badge = "Pro",
                    stats = "Test"
                ),
                CoreFeatureCard(
                    id = "code_companion",
                    title = "Code Companion",
                    description = "Debug & optimize code",
                    icon = Icons.Outlined.Code,
                    gradient = listOf(Color(0xFF212121), Color(0xFF000000)),
                    badge = "Beta",
                    stats = "Dev"
                ),
                CoreFeatureCard(
                    id = "ask_image",
                    title = "Ask Image",
                    description = "Visual AI analysis",
                    icon = Icons.Outlined.Image,
                    gradient = listOf(Color(0xFFE85057), Color(0xFFD92027)),
                    stats = "Vision"
                ),
                CoreFeatureCard(
                    id = "audio_scribe",
                    title = "Audio Scribe",
                    description = "Speech to text locally",
                    icon = Icons.Outlined.Mic,
                    gradient = listOf(Color(0xFF616161), Color(0xFF303030)),
                    stats = "STT"
                ),
                CoreFeatureCard(
                    id = "flashcards",
                    title = "Flashcards",
                    description = "AI-powered learning",
                    icon = Icons.Outlined.Style,
                    gradient = listOf(Color(0xFFEEEEEE), Color(0xFFBDBDBD)),
                    stats = "Study"
                ),
                CoreFeatureCard(
                    id = "quiz",
                    title = "Quiz Mode",
                    description = "Test your knowledge",
                    icon = Icons.Outlined.Quiz,
                    gradient = listOf(Color(0xFF9E9E9E), Color(0xFF757575)),
                    stats = "Test"
                )
            )
        } else {
            // Default theme: Original cyan/blue gradient colors
            listOf(
                CoreFeatureCard(
                    id = "chat",
                    title = "AI Chat",
                    description = "Start intelligent conversations",
                    icon = Icons.Outlined.Chat,
                    gradient = listOf(Color(0xFF0288D1), Color(0xFF0277BD)),
                    badge = "Popular",
                    stats = "24/7"
                ),
                CoreFeatureCard(
                    id = "rag_chat",
                    title = "Document Chat",
                    description = "AI-powered semantic search",
                    icon = Icons.Outlined.Description,
                    gradient = listOf(Color(0xFF00796B), Color(0xFF00695C)),
                    badge = "AI",
                    stats = "RAG"
                ),
                CoreFeatureCard(
                    id = "prompt_lab",
                    title = "Prompt Lab",
                    description = "Experiment & fine-tune",
                    icon = Icons.Outlined.Science,
                    gradient = listOf(Color(0xFF5E35B1), Color(0xFF512DA8)),
                    badge = "Pro",
                    stats = "Test"
                ),
                CoreFeatureCard(
                    id = "code_companion",
                    title = "Code Companion",
                    description = "Debug & optimize code",
                    icon = Icons.Outlined.Code,
                    gradient = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                    badge = "Beta",
                    stats = "Dev"
                ),
                CoreFeatureCard(
                    id = "ask_image",
                    title = "Ask Image",
                    description = "Visual AI analysis",
                    icon = Icons.Outlined.Image,
                    gradient = listOf(Color(0xFF00897B), Color(0xFF00796B)),
                    stats = "Vision"
                ),
                CoreFeatureCard(
                    id = "audio_scribe",
                    title = "Audio Scribe",
                    description = "Speech to text locally",
                    icon = Icons.Outlined.Mic,
                    gradient = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)),
                    stats = "STT"
                ),
                CoreFeatureCard(
                    id = "flashcards",
                    title = "Flashcards",
                    description = "AI-powered learning",
                    icon = Icons.Outlined.Style,
                    gradient = listOf(Color(0xFF00ACC1), Color(0xFF0097A7)),
                    stats = "Study"
                ),
                CoreFeatureCard(
                    id = "quiz",
                    title = "Quiz Mode",
                    description = "Test your knowledge",
                    icon = Icons.Outlined.Quiz,
                    gradient = listOf(Color(0xFF00838F), Color(0xFF006064)),
                    stats = "Test"
                )
            )
        }
    }

    // Secondary Features (horizontal chips)
    val quickActions = remember {
        listOf(
            SecondaryFeatureChip(
                id = "models",
                label = "Models",
                icon = Icons.Filled.CloudDownload
            ),
            SecondaryFeatureChip(
                id = "templates",
                label = "Templates",
                icon = Icons.Outlined.Dashboard
            ),
            SecondaryFeatureChip(
                id = "settings",
                label = "Settings",
                icon = Icons.Default.Settings
            ),
            SecondaryFeatureChip(
                id = "history",
                label = "Full History",
                icon = Icons.Filled.History
            )
        )
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearchClick = { viewModel.toggleSearch(true) },
                onCloseSearch = { viewModel.toggleSearch(false) },
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        if (isSearchActive) {
            SearchResultsList(
                results = searchResults,
                onResultClick = onNavigateToChat,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // I. Model Status Banner
            item {
                ModelStatusBanner(
                    modelName = currentModel?.name,
                    modelLoadingState = modelLoadingState,
                    onLoadModel = onNavigateToModels,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            
            // II. Core Features - Horizontal Scroll
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        letterSpacing = 0.3.sp
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(coreFeatures) { feature ->
                            FeatureCard(
                                title = feature.title,
                                subtitle = feature.description,
                                icon = feature.icon,
                                gradient = feature.gradient,
                                stats = feature.stats,
                                badge = feature.badge,
                                onCardClick = {
                                    when (feature.id) {
                                        "chat" -> onNavigateToChat(null)
                                        "rag_chat" -> onNavigateToRAGChat()
                                        "prompt_lab" -> onNavigateToPromptLab()
                                        "code_companion" -> onNavigateToCodeCompanion()
                                        "ask_image" -> onNavigateToAskImage()
                                        "audio_scribe" -> onNavigateToAudioScribe()
                                        "flashcards" -> onNavigateToFlashcards()
                                        "quiz" -> onNavigateToQuiz()
                                    }
                                },
                                onActionClick = {
                                    when (feature.id) {
                                        "chat" -> onNavigateToChat(null)
                                        "rag_chat" -> onNavigateToRAGChat()
                                        "prompt_lab" -> onNavigateToPromptLab()
                                        "code_companion" -> onNavigateToCodeCompanion()
                                        "ask_image" -> onNavigateToAskImage()
                                        "audio_scribe" -> onNavigateToAudioScribe()
                                        "flashcards" -> onNavigateToFlashcards()
                                        "quiz" -> onNavigateToQuiz()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // III. Quick Actions - Horizontal Chips
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        letterSpacing = 0.3.sp
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(quickActions) { action ->
                            QuickActionChip(
                                action = action,
                                onClick = {
                                    when (action.id) {
                                        "models" -> onNavigateToModels()
                                        "templates" -> onNavigateToTemplates()
                                        "settings" -> onNavigateToSettings()
                                        "history" -> onNavigateToHistory()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // IV. Recent Conversations Preview
            if (recentConversations.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Chats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                            
                            TextButton(onClick = onNavigateToHistory) {
                                Text(
                                    "View All",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentConversations.take(5)) { conversation ->
                                RecentConversationPreviewCard(
                                    conversation = conversation,
                                    onClick = { onNavigateToChat(conversation.id) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        }
    }
}

// ============================================================================
// I. HEADER & BRANDING
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    onSettingsClick: () -> Unit
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search conversations...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.ArrowBack, "Close search")
                }
            },
            actions = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, "Clear search")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    } else {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small icon before LocalLLM
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LocalLLM",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search models and chats",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

// ============================================================================
// II. MODEL STATUS & SUGGESTIONS CARD (THE DYNAMIC BANNER)
// ============================================================================

@Composable
private fun ModelStatusBanner(
    modelName: String?,
    modelLoadingState: ModelLoadingState,
    onLoadModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            // State A: No Model Loaded (New User Flow)
            modelName == null && modelLoadingState !is ModelLoadingState.Loading -> {
                NoModelLoadedBanner(onLoadModel = onLoadModel)
            }
            // State B: Model Loading
            modelLoadingState is ModelLoadingState.Loading -> {
                ModelLoadingBanner(
                    progress = modelLoadingState.progress,
                    modelName = modelName
                )
            }
            // State C: Model Loaded (Existing User Flow)
            modelLoadingState is ModelLoadingState.Loaded -> {
                ModelLoadedBanner(modelName = modelName ?: "Unknown Model")
            }
            // State D: Error
            modelLoadingState is ModelLoadingState.Error -> {
                ModelErrorBanner(
                    error = modelLoadingState.message,
                    onLoadModel = onLoadModel
                )
            }
        }
    }
}

@Composable
private fun NoModelLoadedBanner(onLoadModel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEF4444).copy(alpha = 0.15f),
                            Color(0xFFDC2626).copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Model Active",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Load a model to unlock AI features",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onLoadModel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Browse Model Library",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelLoadingBanner(
    progress: Float,
    modelName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = "Loading Model...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.3.sp
                        )
                        if (modelName != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelLoadedBanner(modelName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inference Ready",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy Secured • Local Inference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelErrorBanner(
    error: String,
    onLoadModel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Model Loading Failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLoadModel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Another Model")
            }
        }
    }
}

// ============================================================================
// III. QUICK ACTIONS (HORIZONTAL CHIPS)
// ============================================================================

@Composable
private fun QuickActionChip(
    action: SecondaryFeatureChip,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============================================================================
// V. RECENT CONVERSATIONS PREVIEW (HORIZONTAL CARDS)
// ============================================================================

@Composable
private fun RecentConversationPreviewCard(
    conversation: HomeViewModel.RecentConversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.2.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = conversation.timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsList(
    results: List<HomeViewModel.RecentConversation>,
    onResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { conversation ->
                Card(
                    onClick = { onResultClick(conversation.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conversation.lastMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = conversation.timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

