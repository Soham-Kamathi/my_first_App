package com.localllm.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.ui.viewmodel.QuizViewModel
import com.localllm.app.ui.viewmodel.QuizQuestion
import com.localllm.app.ui.viewmodel.QuizState

/**
 * Interactive Quiz Screen
 * Generates and administers AI-powered quizzes with scoring
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (uiState.quizState) {
                            QuizState.SETUP -> "Create Quiz"
                            QuizState.IN_PROGRESS -> "Quiz"
                            QuizState.REVIEW -> "Review Answers"
                            QuizState.COMPLETE -> "Results"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState.quizState) {
                            QuizState.SETUP -> onNavigateBack()
                            else -> viewModel.resetQuiz()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            when (uiState.quizState) {
                QuizState.SETUP -> {
                    QuizSetupContent(
                        isGenerating = generationState.isGenerating,
                        onGenerate = { topic, count, difficulty ->
                            viewModel.generateQuiz(topic, count, difficulty)
                        }
                    )
                }
                QuizState.IN_PROGRESS -> {
                    QuizInProgressContent(
                        questions = uiState.questions,
                        currentIndex = uiState.currentQuestionIndex,
                        selectedAnswers = uiState.selectedAnswers,
                        onSelectAnswer = { viewModel.selectAnswer(it) },
                        onNextQuestion = { viewModel.nextQuestion() },
                        onPreviousQuestion = { viewModel.previousQuestion() },
                        onSubmit = { viewModel.submitQuiz() }
                    )
                }
                QuizState.REVIEW -> {
                    QuizReviewContent(
                        questions = uiState.questions,
                        selectedAnswers = uiState.selectedAnswers,
                        score = uiState.score,
                        onFinish = { viewModel.resetQuiz() }
                    )
                }
                QuizState.COMPLETE -> {
                    QuizCompleteContent(
                        score = uiState.score,
                        totalQuestions = uiState.questions.size,
                        onReview = { viewModel.reviewAnswers() },
                        onNewQuiz = { viewModel.resetQuiz() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizSetupContent(
    isGenerating: Boolean,
    onGenerate: (String, Int, String) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var questionCount by remember { mutableStateOf(5) }
    var difficulty by remember { mutableStateOf("Medium") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AI Quiz Generator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Test your knowledge on any topic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        if (isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating quiz questions...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "This may take a moment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Topic input
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Quiz Topic") },
                placeholder = { Text("e.g., World History, Chemistry, Programming") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Topic, null) },
                singleLine = true
            )
            
            // Question count
            Text(
                text = "Number of Questions: $questionCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = questionCount.toFloat(),
                onValueChange = { questionCount = it.toInt() },
                valueRange = 3f..15f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Difficulty selector
            Text(
                text = "Difficulty Level",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Easy", "Medium", "Hard").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { difficulty = level },
                        label = { Text(level) },
                        leadingIcon = if (difficulty == level) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate button
            Button(
                onClick = { onGenerate(topic, questionCount, difficulty) },
                modifier = Modifier.fillMaxWidth(),
                enabled = topic.isNotBlank()
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Quiz")
            }
            
            // Quick topics
            Text(
                text = "Quick Topics",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Science", "History", "Geography", "Math", 
                    "Literature", "Technology", "Sports", "Music"
                ).forEach { quickTopic ->
                    SuggestionChip(
                        onClick = { topic = quickTopic },
                        label = { Text(quickTopic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizInProgressContent(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedAnswers: Map<Int, Int>,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSubmit: () -> Unit
) {
    if (questions.isEmpty()) return
    
    val currentQuestion = questions[currentIndex]
    val selectedAnswer = selectedAnswers[currentIndex]
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / questions.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${selectedAnswers.size} answered",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Question dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            questions.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (index == currentIndex) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == currentIndex -> MaterialTheme.colorScheme.primary
                                selectedAnswers.containsKey(index) -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = currentQuestion.question,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Answer options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            currentQuestion.options.forEachIndexed { index, option ->
                val isSelected = selectedAnswer == index
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAnswer(index) }
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ('A' + index).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreviousQuestion,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ChevronLeft, null)
                Text("Previous")
            }
            
            if (currentIndex < questions.size - 1) {
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                    Icon(Icons.Default.ChevronRight, null)
                }
            } else {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = selectedAnswers.size == questions.size
                ) {
                    Icon(Icons.Default.Done, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Submit")
                }
            }
        }
        
        if (selectedAnswers.size < questions.size && currentIndex == questions.size - 1) {
            Text(
                text = "Answer all questions to submit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuizCompleteContent(
    score: Int,
    totalQuestions: Int,
    onReview: () -> Unit,
    onNewQuiz: () -> Unit
) {
    val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when {
                percentage >= 80 -> Icons.Default.EmojiEvents
                percentage >= 60 -> Icons.Default.ThumbUp
                else -> Icons.Default.Refresh
            },
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = when {
                percentage >= 80 -> Color(0xFFFFD700)
                percentage >= 60 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when {
                percentage >= 80 -> "Excellent!"
                percentage >= 60 -> "Good Job!"
                percentage >= 40 -> "Nice Try!"
                else -> "Keep Learning!"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$score / $totalQuestions correct",
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = "${percentage}%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                percentage >= 80 -> Color(0xFF4CAF50)
                percentage >= 60 -> Color(0xFFFFA000)
                percentage >= 40 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.error
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onReview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Visibility, null)
            Spacer(Modifier.width(8.dp))
            Text("Review Answers")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onNewQuiz,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("New Quiz")
        }
    }
}

@Composable
private fun QuizReviewContent(
    questions: List<QuizQuestion>,
    selectedAnswers: Map<Int, Int>,
    score: Int,
    onFinish: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Final Score",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$score / ${questions.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val percentage = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
                    Text(
                        text = "${percentage}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage >= 80 -> Color(0xFF4CAF50)
                            percentage >= 60 -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
        
        itemsIndexed(questions) { index, question ->
            val selectedAnswer = selectedAnswers[index]
            val isCorrect = selectedAnswer == question.correctAnswerIndex
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) 
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    question.options.forEachIndexed { optIndex, option ->
                        val isSelectedAnswer = selectedAnswer == optIndex
                        val isCorrectAnswer = question.correctAnswerIndex == optIndex
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isCorrectAnswer -> Icons.Default.CheckCircle
                                    isSelectedAnswer -> Icons.Default.Cancel
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = when {
                                    isCorrectAnswer -> Color(0xFF4CAF50)
                                    isSelectedAnswer -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCorrectAnswer) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isCorrectAnswer -> Color(0xFF4CAF50)
                                    isSelectedAnswer -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    
                    if (question.explanation.isNotBlank()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "💡 ${question.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
