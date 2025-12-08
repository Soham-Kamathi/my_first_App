package com.localllm.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.ui.viewmodel.FlashcardViewModel
import com.localllm.app.ui.viewmodel.Flashcard
import com.localllm.app.ui.viewmodel.FlashcardDeck
import com.localllm.app.ui.viewmodel.StudyMode

/**
 * Interactive Flashcard Learning Screen
 * Supports AI-generated flashcards and spaced repetition study
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (uiState.studyMode) {
                            StudyMode.BROWSE -> "Flashcards"
                            StudyMode.STUDY -> "Study Mode"
                            StudyMode.QUIZ -> "Quiz Mode"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.studyMode != StudyMode.BROWSE) {
                            viewModel.setStudyMode(StudyMode.BROWSE)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.studyMode == StudyMode.BROWSE) {
                        IconButton(onClick = { showGenerateDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Card")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.studyMode) {
                StudyMode.BROWSE -> {
                    FlashcardBrowseContent(
                        decks = uiState.decks,
                        selectedDeck = uiState.selectedDeck,
                        onDeckSelected = { viewModel.selectDeck(it) },
                        onStartStudy = { viewModel.startStudySession() },
                        onStartQuiz = { viewModel.startQuizSession() },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onCreateDeck = { viewModel.createDeck(it) }
                    )
                }
                StudyMode.STUDY -> {
                    FlashcardStudyContent(
                        cards = uiState.studyCards,
                        currentIndex = uiState.currentCardIndex,
                        onCardFlip = { viewModel.flipCard() },
                        onNextCard = { viewModel.nextCard() },
                        onPreviousCard = { viewModel.previousCard() },
                        onMarkKnown = { viewModel.markCardKnown() },
                        onMarkUnknown = { viewModel.markCardUnknown() },
                        isFlipped = uiState.isCardFlipped
                    )
                }
                StudyMode.QUIZ -> {
                    FlashcardQuizContent(
                        cards = uiState.studyCards,
                        currentIndex = uiState.currentCardIndex,
                        score = uiState.quizScore,
                        onAnswer = { correct -> viewModel.submitQuizAnswer(correct) },
                        onComplete = { viewModel.setStudyMode(StudyMode.BROWSE) }
                    )
                }
            }
        }
    }
    
    // Create Card Dialog
    if (showCreateDialog) {
        CreateFlashcardDialog(
            deckNames = uiState.decks.map { it.name },
            onDismiss = { showCreateDialog = false },
            onCreate = { question, answer, deckName ->
                viewModel.addCard(question, answer, deckName)
                showCreateDialog = false
            }
        )
    }
    
    // AI Generate Dialog
    if (showGenerateDialog) {
        GenerateFlashcardsDialog(
            isGenerating = generationState.isGenerating,
            onDismiss = { showGenerateDialog = false },
            onGenerate = { topic, count, deckName ->
                viewModel.generateFlashcards(topic, count, deckName)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardBrowseContent(
    decks: List<FlashcardDeck>,
    selectedDeck: FlashcardDeck?,
    onDeckSelected: (FlashcardDeck?) -> Unit,
    onStartStudy: () -> Unit,
    onStartQuiz: () -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onCreateDeck: (String) -> Unit
) {
    var showNewDeckDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Deck selector
        Text(
            text = "Your Decks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedDeck == null,
                    onClick = { onDeckSelected(null) },
                    label = { Text("All Cards") },
                    leadingIcon = if (selectedDeck == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            items(decks) { deck ->
                FilterChip(
                    selected = selectedDeck?.id == deck.id,
                    onClick = { onDeckSelected(deck) },
                    label = { Text("${deck.name} (${deck.cards.size})") },
                    leadingIcon = if (selectedDeck?.id == deck.id) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            item {
                AssistChip(
                    onClick = { showNewDeckDialog = true },
                    label = { Text("New Deck") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Study buttons
        val currentCards = selectedDeck?.cards ?: decks.flatMap { it.cards }
        if (currentCards.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartStudy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.School, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Study")
                }
                
                OutlinedButton(
                    onClick = onStartQuiz,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Quiz, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Quiz")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Cards list
        Text(
            text = "Cards (${currentCards.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        if (currentCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Style,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No flashcards yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add or ✨ to generate with AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(currentCards) { index, card ->
                    FlashcardListItem(
                        card = card,
                        index = index + 1,
                        onDelete = { onDeleteCard(card) }
                    )
                }
            }
        }
    }
    
    if (showNewDeckDialog) {
        NewDeckDialog(
            onDismiss = { showNewDeckDialog = false },
            onCreate = { name ->
                onCreateDeck(name)
                showNewDeckDialog = false
            }
        )
    }
}

@Composable
private fun FlashcardListItem(
    card: Flashcard,
    index: Int,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Text(
                text = card.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FlashcardStudyContent(
    cards: List<Flashcard>,
    currentIndex: Int,
    onCardFlip: () -> Unit,
    onNextCard: () -> Unit,
    onPreviousCard: () -> Unit,
    onMarkKnown: () -> Unit,
    onMarkUnknown: () -> Unit,
    isFlipped: Boolean
) {
    if (cards.isEmpty()) return
    
    val currentCard = cards[currentIndex]
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / cards.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "${currentIndex + 1} / ${cards.size}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Flashcard
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { onCardFlip() },
            colors = CardDefaults.cardColors(
                containerColor = if (isFlipped) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (rotation <= 90f) currentCard.question else currentCard.answer,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        rotationY = if (rotation > 90f) 180f else 0f
                    }
                )
            }
        }
        
        Text(
            text = "Tap card to flip",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onPreviousCard,
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, null)
                Text("Previous")
            }
            
            OutlinedButton(
                onClick = onNextCard,
                enabled = currentIndex < cards.size - 1
            ) {
                Text("Next")
                Icon(Icons.Default.ChevronRight, null)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Know / Don't Know buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onMarkUnknown,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Still Learning")
            }
            
            Button(
                onClick = onMarkKnown,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Got It!")
            }
        }
    }
}

@Composable
private fun FlashcardQuizContent(
    cards: List<Flashcard>,
    currentIndex: Int,
    score: Int,
    onAnswer: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    if (currentIndex >= cards.size) {
        // Quiz complete
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Quiz Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Score: $score / ${cards.size}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            val percentage = if (cards.isNotEmpty()) (score * 100) / cards.size else 0
            Text(
                text = "${percentage}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    percentage >= 80 -> Color(0xFF4CAF50)
                    percentage >= 60 -> Color(0xFFFFA000)
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onComplete) {
                Text("Done")
            }
        }
        return
    }
    
    val currentCard = cards[currentIndex]
    var showAnswer by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        LinearProgressIndicator(
            progress = currentIndex.toFloat() / cards.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentIndex + 1} of ${cards.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = currentCard.question,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!showAnswer) {
            Button(
                onClick = { showAnswer = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Answer")
            }
        } else {
            // Answer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = currentCard.answer,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Did you get it right?",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        onAnswer(false)
                        showAnswer = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text("No")
                }
                
                Button(
                    onClick = { 
                        onAnswer(true)
                        showAnswer = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Yes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFlashcardDialog(
    deckNames: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var selectedDeck by remember { mutableStateOf(deckNames.firstOrNull() ?: "Default") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Term") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer / Definition") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                // Deck selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDeck,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Deck") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        deckNames.forEach { deck ->
                            DropdownMenuItem(
                                text = { Text(deck) },
                                onClick = {
                                    selectedDeck = deck
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(question, answer, selectedDeck) },
                enabled = question.isNotBlank() && answer.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GenerateFlashcardsDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (String, Int, String) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(5) }
    var deckName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = { Text("Generate Flashcards with AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating flashcards...")
                    }
                } else {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topic") },
                        placeholder = { Text("e.g., French vocabulary, Photosynthesis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = deckName.ifBlank { topic },
                        onValueChange = { deckName = it },
                        label = { Text("Deck Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Number of cards: $count")
                    Slider(
                        value = count.toFloat(),
                        onValueChange = { count = it.toInt() },
                        valueRange = 3f..15f,
                        steps = 11
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(topic, count, deckName.ifBlank { topic }) },
                enabled = topic.isNotBlank() && !isGenerating
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NewDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Deck") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Deck Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
