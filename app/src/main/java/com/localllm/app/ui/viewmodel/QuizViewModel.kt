package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.local.PreferencesDataStore
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quiz state enumeration
 */
enum class QuizState {
    SETUP,
    IN_PROGRESS,
    REVIEW,
    COMPLETE
}

/**
 * Quiz question data class
 */
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String = ""
)

/**
 * UI State for quiz screen
 */
data class QuizUiState(
    val quizState: QuizState = QuizState.SETUP,
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val score: Int = 0,
    val topic: String = "",
    val difficulty: String = "Medium"
)

/**
 * Generation state for AI-generated quizzes
 */
data class QuizGenerationState(
    val isGenerating: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

/**
 * ViewModel for Quiz functionality
 * Handles quiz generation, scoring, and review
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _generationState = MutableStateFlow(QuizGenerationState())
    val generationState: StateFlow<QuizGenerationState> = _generationState.asStateFlow()

    /**
     * Generate a quiz using AI
     */
    fun generateQuiz(topic: String, questionCount: Int, difficulty: String) {
        if (!modelManager.isModelLoaded) {
            _generationState.value = QuizGenerationState(
                error = "No model loaded. Please load a model first."
            )
            return
        }

        viewModelScope.launch {
            _generationState.value = QuizGenerationState(isGenerating = true)
            _uiState.value = _uiState.value.copy(
                topic = topic,
                difficulty = difficulty
            )
            
            try {
                val preferences = preferencesDataStore.userPreferencesFlow.first()
                val model = modelManager.currentModel.value
                
                val prompt = """Generate exactly $questionCount multiple choice quiz questions about: $topic
Difficulty level: $difficulty

Format EACH question EXACTLY like this:
QUESTION: [The question text]
A) [First option]
B) [Second option]
C) [Third option]
D) [Fourth option]
CORRECT: [Letter of correct answer: A, B, C, or D]
EXPLANATION: [Brief explanation of why this is correct]

Make sure questions are educational, accurate, and appropriately challenging for $difficulty difficulty.

Generate the quiz now:"""

                val systemPrompt = """You are an expert quiz creator. Generate clear, educational multiple choice questions. 
Each question must have exactly 4 options (A, B, C, D), one correct answer, and a brief explanation.
Follow the exact format requested. Questions should be factually accurate and test real knowledge."""
                
                val fullPrompt = inferenceEngine.buildPrompt(
                    messages = emptyList(),
                    systemPrompt = systemPrompt,
                    promptTemplate = model?.promptTemplate ?: "chatml"
                ) + "\n\nUser: $prompt\n\nAssistant:"

                var response = ""
                inferenceEngine.generateStream(
                    prompt = fullPrompt,
                    config = preferences.defaultGenerationConfig.copy(
                        maxTokens = 2048,
                        temperature = 0.7f
                    ),
                    onTokenGenerated = { token ->
                        response += token
                    }
                ).collect { /* wait for completion */ }

                // Parse generated quiz
                val questions = parseGeneratedQuiz(response)
                
                if (questions.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        quizState = QuizState.IN_PROGRESS,
                        questions = questions,
                        currentQuestionIndex = 0,
                        selectedAnswers = emptyMap(),
                        score = 0
                    )
                } else {
                    _generationState.value = QuizGenerationState(
                        error = "Failed to generate quiz questions. Please try again."
                    )
                }
                
                _generationState.value = QuizGenerationState(isGenerating = false)
                
            } catch (e: Exception) {
                _generationState.value = QuizGenerationState(
                    isGenerating = false,
                    error = e.message ?: "Quiz generation failed"
                )
            }
        }
    }

    /**
     * Parse AI-generated quiz text into questions
     */
    private fun parseGeneratedQuiz(text: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        
        // Split by QUESTION: pattern
        val questionBlocks = text.split(Regex("QUESTION:\\s*", RegexOption.IGNORE_CASE))
            .filter { it.isNotBlank() }
        
        for (block in questionBlocks) {
            try {
                val lines = block.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) continue
                
                // First line is the question
                val questionText = lines.first().trim()
                if (questionText.isBlank()) continue
                
                // Parse options
                val options = mutableListOf<String>()
                var correctIndex = -1
                var explanation = ""
                
                for (line in lines.drop(1)) {
                    val trimmed = line.trim()
                    when {
                        trimmed.matches(Regex("^[Aa]\\)\\s*.+")) -> {
                            options.add(trimmed.substringAfter(")").trim())
                        }
                        trimmed.matches(Regex("^[Bb]\\)\\s*.+")) -> {
                            options.add(trimmed.substringAfter(")").trim())
                        }
                        trimmed.matches(Regex("^[Cc]\\)\\s*.+")) -> {
                            options.add(trimmed.substringAfter(")").trim())
                        }
                        trimmed.matches(Regex("^[Dd]\\)\\s*.+")) -> {
                            options.add(trimmed.substringAfter(")").trim())
                        }
                        trimmed.startsWith("CORRECT:", ignoreCase = true) -> {
                            val answer = trimmed.substringAfter(":").trim().uppercase()
                            correctIndex = when {
                                answer.startsWith("A") -> 0
                                answer.startsWith("B") -> 1
                                answer.startsWith("C") -> 2
                                answer.startsWith("D") -> 3
                                else -> -1
                            }
                        }
                        trimmed.startsWith("EXPLANATION:", ignoreCase = true) -> {
                            explanation = trimmed.substringAfter(":").trim()
                        }
                    }
                }
                
                // Validate and add question
                if (options.size >= 4 && correctIndex in 0..3) {
                    questions.add(QuizQuestion(
                        question = questionText,
                        options = options.take(4),
                        correctAnswerIndex = correctIndex,
                        explanation = explanation
                    ))
                }
            } catch (e: Exception) {
                // Skip malformed questions
                continue
            }
        }
        
        return questions
    }

    /**
     * Select an answer for the current question
     */
    fun selectAnswer(answerIndex: Int) {
        val currentIndex = _uiState.value.currentQuestionIndex
        _uiState.value = _uiState.value.copy(
            selectedAnswers = _uiState.value.selectedAnswers + (currentIndex to answerIndex)
        )
    }

    /**
     * Go to next question
     */
    fun nextQuestion() {
        val newIndex = minOf(
            _uiState.value.currentQuestionIndex + 1,
            _uiState.value.questions.size - 1
        )
        _uiState.value = _uiState.value.copy(currentQuestionIndex = newIndex)
    }

    /**
     * Go to previous question
     */
    fun previousQuestion() {
        val newIndex = maxOf(_uiState.value.currentQuestionIndex - 1, 0)
        _uiState.value = _uiState.value.copy(currentQuestionIndex = newIndex)
    }

    /**
     * Submit the quiz and calculate score
     */
    fun submitQuiz() {
        val questions = _uiState.value.questions
        val answers = _uiState.value.selectedAnswers
        
        var score = 0
        questions.forEachIndexed { index, question ->
            if (answers[index] == question.correctAnswerIndex) {
                score++
            }
        }
        
        _uiState.value = _uiState.value.copy(
            quizState = QuizState.COMPLETE,
            score = score
        )
    }

    /**
     * Go to review mode
     */
    fun reviewAnswers() {
        _uiState.value = _uiState.value.copy(quizState = QuizState.REVIEW)
    }

    /**
     * Reset quiz to setup state
     */
    fun resetQuiz() {
        _uiState.value = QuizUiState()
        _generationState.value = QuizGenerationState()
    }
}
