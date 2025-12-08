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
import java.util.UUID
import javax.inject.Inject

/**
 * Study mode enumeration
 */
enum class StudyMode {
    BROWSE,
    STUDY,
    QUIZ
}

/**
 * Individual flashcard data class
 */
data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val deckId: String,
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewed: Long? = null
)

/**
 * Flashcard deck data class
 */
data class FlashcardDeck(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cards: List<Flashcard> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * UI State for flashcard screen
 */
data class FlashcardUiState(
    val decks: List<FlashcardDeck> = listOf(FlashcardDeck(name = "Default")),
    val selectedDeck: FlashcardDeck? = null,
    val studyMode: StudyMode = StudyMode.BROWSE,
    val studyCards: List<Flashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val quizScore: Int = 0
)

/**
 * Generation state for AI-generated flashcards
 */
data class FlashcardGenerationState(
    val isGenerating: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

/**
 * ViewModel for Flashcard functionality
 * Handles flashcard CRUD, study sessions, and AI generation
 */
@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private val _generationState = MutableStateFlow(FlashcardGenerationState())
    val generationState: StateFlow<FlashcardGenerationState> = _generationState.asStateFlow()

    /**
     * Create a new deck
     */
    fun createDeck(name: String) {
        val newDeck = FlashcardDeck(name = name)
        _uiState.value = _uiState.value.copy(
            decks = _uiState.value.decks + newDeck
        )
    }

    /**
     * Select a deck to view
     */
    fun selectDeck(deck: FlashcardDeck?) {
        _uiState.value = _uiState.value.copy(selectedDeck = deck)
    }

    /**
     * Add a new flashcard
     */
    fun addCard(question: String, answer: String, deckName: String) {
        val decks = _uiState.value.decks.toMutableList()
        val deckIndex = decks.indexOfFirst { it.name == deckName }
        
        if (deckIndex >= 0) {
            val deck = decks[deckIndex]
            val newCard = Flashcard(
                question = question,
                answer = answer,
                deckId = deck.id
            )
            decks[deckIndex] = deck.copy(cards = deck.cards + newCard)
        } else {
            // Create new deck with the card
            val newDeck = FlashcardDeck(name = deckName)
            val newCard = Flashcard(
                question = question,
                answer = answer,
                deckId = newDeck.id
            )
            decks.add(newDeck.copy(cards = listOf(newCard)))
        }
        
        _uiState.value = _uiState.value.copy(decks = decks)
    }

    /**
     * Delete a flashcard
     */
    fun deleteCard(card: Flashcard) {
        val decks = _uiState.value.decks.map { deck ->
            deck.copy(cards = deck.cards.filter { it.id != card.id })
        }
        _uiState.value = _uiState.value.copy(decks = decks)
    }

    /**
     * Start a study session
     */
    fun startStudySession() {
        val cards = getActiveCards().shuffled()
        _uiState.value = _uiState.value.copy(
            studyMode = StudyMode.STUDY,
            studyCards = cards,
            currentCardIndex = 0,
            isCardFlipped = false
        )
    }

    /**
     * Start a quiz session
     */
    fun startQuizSession() {
        val cards = getActiveCards().shuffled()
        _uiState.value = _uiState.value.copy(
            studyMode = StudyMode.QUIZ,
            studyCards = cards,
            currentCardIndex = 0,
            quizScore = 0
        )
    }

    /**
     * Set study mode
     */
    fun setStudyMode(mode: StudyMode) {
        _uiState.value = _uiState.value.copy(
            studyMode = mode,
            currentCardIndex = 0,
            isCardFlipped = false,
            quizScore = 0
        )
    }

    /**
     * Flip the current card
     */
    fun flipCard() {
        _uiState.value = _uiState.value.copy(
            isCardFlipped = !_uiState.value.isCardFlipped
        )
    }

    /**
     * Go to next card
     */
    fun nextCard() {
        val newIndex = minOf(
            _uiState.value.currentCardIndex + 1,
            _uiState.value.studyCards.size - 1
        )
        _uiState.value = _uiState.value.copy(
            currentCardIndex = newIndex,
            isCardFlipped = false
        )
    }

    /**
     * Go to previous card
     */
    fun previousCard() {
        val newIndex = maxOf(_uiState.value.currentCardIndex - 1, 0)
        _uiState.value = _uiState.value.copy(
            currentCardIndex = newIndex,
            isCardFlipped = false
        )
    }

    /**
     * Mark current card as known
     */
    fun markCardKnown() {
        updateCardStats(correct = true)
        nextCard()
    }

    /**
     * Mark current card as unknown
     */
    fun markCardUnknown() {
        updateCardStats(correct = false)
        nextCard()
    }

    /**
     * Submit quiz answer
     */
    fun submitQuizAnswer(correct: Boolean) {
        var newScore = _uiState.value.quizScore
        if (correct) newScore++
        
        _uiState.value = _uiState.value.copy(
            quizScore = newScore,
            currentCardIndex = _uiState.value.currentCardIndex + 1
        )
    }

    /**
     * Generate flashcards using AI
     */
    fun generateFlashcards(topic: String, count: Int, deckName: String) {
        if (!modelManager.isModelLoaded) {
            _generationState.value = FlashcardGenerationState(
                error = "No model loaded. Please load a model first."
            )
            return
        }

        viewModelScope.launch {
            _generationState.value = FlashcardGenerationState(isGenerating = true)
            
            try {
                val preferences = preferencesDataStore.userPreferencesFlow.first()
                val model = modelManager.currentModel.value
                
                val prompt = """Generate exactly $count flashcards about: $topic

Format each flashcard EXACTLY like this:
Q: [question or term]
A: [answer or definition]

Generate educational, clear, and accurate flashcards. Each question should test understanding of a key concept.

Flashcards:"""

                val systemPrompt = "You are an expert educator creating study flashcards. Generate clear, concise flashcards in the exact format requested. Each Q: should be followed by A: on the next line."
                
                val fullPrompt = inferenceEngine.buildPrompt(
                    messages = emptyList(),
                    systemPrompt = systemPrompt,
                    promptTemplate = model?.promptTemplate ?: "chatml"
                ) + "\n\nUser: $prompt\n\nAssistant:"

                var response = ""
                inferenceEngine.generateStream(
                    prompt = fullPrompt,
                    config = preferences.defaultGenerationConfig.copy(
                        maxTokens = 1024,
                        temperature = 0.7f
                    ),
                    onTokenGenerated = { token ->
                        response += token
                    }
                ).collect { /* wait for completion */ }

                // Parse generated flashcards
                val cards = parseGeneratedFlashcards(response, deckName)
                
                if (cards.isNotEmpty()) {
                    // Find or create deck
                    val decks = _uiState.value.decks.toMutableList()
                    val existingDeckIndex = decks.indexOfFirst { it.name == deckName }
                    
                    if (existingDeckIndex >= 0) {
                        decks[existingDeckIndex] = decks[existingDeckIndex].copy(
                            cards = decks[existingDeckIndex].cards + cards
                        )
                    } else {
                        val newDeck = FlashcardDeck(name = deckName, cards = cards)
                        decks.add(newDeck)
                    }
                    
                    _uiState.value = _uiState.value.copy(decks = decks)
                }
                
                _generationState.value = FlashcardGenerationState(isGenerating = false)
                
            } catch (e: Exception) {
                _generationState.value = FlashcardGenerationState(
                    isGenerating = false,
                    error = e.message ?: "Generation failed"
                )
            }
        }
    }

    /**
     * Parse AI-generated flashcard text
     */
    private fun parseGeneratedFlashcards(text: String, deckName: String): List<Flashcard> {
        val cards = mutableListOf<Flashcard>()
        
        // Find or create deck ID
        val deckId = _uiState.value.decks.find { it.name == deckName }?.id 
            ?: UUID.randomUUID().toString()
        
        // Parse Q: and A: patterns
        val lines = text.lines()
        var currentQuestion: String? = null
        
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Q:", ignoreCase = true) -> {
                    currentQuestion = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("A:", ignoreCase = true) && currentQuestion != null -> {
                    val answer = trimmed.substringAfter(":").trim()
                    if (currentQuestion.isNotBlank() && answer.isNotBlank()) {
                        cards.add(Flashcard(
                            question = currentQuestion,
                            answer = answer,
                            deckId = deckId
                        ))
                    }
                    currentQuestion = null
                }
            }
        }
        
        return cards
    }

    /**
     * Get cards from selected deck or all cards
     */
    private fun getActiveCards(): List<Flashcard> {
        return _uiState.value.selectedDeck?.cards 
            ?: _uiState.value.decks.flatMap { it.cards }
    }

    /**
     * Update card statistics
     */
    private fun updateCardStats(correct: Boolean) {
        val currentCard = _uiState.value.studyCards.getOrNull(_uiState.value.currentCardIndex)
            ?: return
        
        val updatedCard = currentCard.copy(
            timesReviewed = currentCard.timesReviewed + 1,
            timesCorrect = if (correct) currentCard.timesCorrect + 1 else currentCard.timesCorrect,
            lastReviewed = System.currentTimeMillis()
        )
        
        // Update card in decks
        val decks = _uiState.value.decks.map { deck ->
            deck.copy(cards = deck.cards.map { card ->
                if (card.id == currentCard.id) updatedCard else card
            })
        }
        
        // Update study cards list
        val studyCards = _uiState.value.studyCards.map { card ->
            if (card.id == currentCard.id) updatedCard else card
        }
        
        _uiState.value = _uiState.value.copy(
            decks = decks,
            studyCards = studyCards
        )
    }
}
