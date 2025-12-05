package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.data.model.GenerationState
import com.localllm.app.data.model.MessageRole
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.model.UserPreferences
import com.localllm.app.data.local.PreferencesDataStore
import com.localllm.app.data.remote.WebSearchService
import com.localllm.app.data.repository.ConversationRepository
import com.localllm.app.domain.usecase.CreateConversationUseCase
import com.localllm.app.domain.usecase.GenerateConversationTitleUseCase
import com.localllm.app.domain.usecase.GetMessagesUseCase
import com.localllm.app.domain.usecase.SendMessageUseCase
import com.localllm.app.domain.usecase.UpdateMessageUseCase
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.ThinkingModeParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the main chat screen.
 * Handles conversation management, message sending, and text generation.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager,
    private val conversationRepository: ConversationRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val webSearchService: WebSearchService,
    private val createConversationUseCase: CreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val updateMessageUseCase: UpdateMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val generateConversationTitleUseCase: GenerateConversationTitleUseCase
) : ViewModel() {

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _currentGeneratingMessageId = MutableStateFlow<String?>(null)
    
    // Web search state
    private val _isSearchingWeb = MutableStateFlow(false)
    val isSearchingWeb: StateFlow<Boolean> = _isSearchingWeb.asStateFlow()
    
    private val _webSearchResults = MutableStateFlow<String?>(null)
    val webSearchResults: StateFlow<String?> = _webSearchResults.asStateFlow()
    
    private var generationJob: Job? = null

    val currentModel: StateFlow<ModelInfo?> = modelManager.currentModel

    val modelLoadingState: StateFlow<ModelLoadingState> = modelManager.loadingState

    val userPreferences: StateFlow<UserPreferences> = preferencesDataStore.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val isModelLoaded: Boolean
        get() = modelManager.isModelLoaded

    val isGenerating: Boolean
        get() = _generationState.value is GenerationState.Generating

    /**
     * Create a new conversation and set it as current.
     */
    fun createNewConversation() {
        viewModelScope.launch {
            val model = modelManager.currentModel.value
            val preferences = userPreferences.value
            
            val conversation = createConversationUseCase(
                title = "New Conversation",
                modelId = model?.id,
                systemPrompt = preferences.defaultSystemPrompt
            )
            
            setCurrentConversation(conversation.id)
            modelManager.clearKVCache()
        }
    }

    /**
     * Set the current conversation and load its messages.
     */
    fun setCurrentConversation(conversationId: String) {
        // Only clear KV cache when switching to a DIFFERENT conversation
        // This preserves context for follow-up queries within the same conversation
        val previousConversationId = _currentConversationId.value
        if (previousConversationId != null && previousConversationId != conversationId) {
            modelManager.clearKVCache()
        }
        
        _currentConversationId.value = conversationId
        
        viewModelScope.launch {
            getMessagesUseCase(conversationId)
                .catch { e -> 
                    // Handle error
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    /**
     * Send a user message and generate an AI response.
     */
    fun sendMessage(text: String) {
        val conversationId = _currentConversationId.value
        if (text.isBlank() || conversationId == null || isGenerating) return

        viewModelScope.launch {
            // Add user message to database (flow will update _messages automatically)
            sendMessageUseCase(
                conversationId = conversationId,
                content = text.trim(),
                role = MessageRole.USER
            )

            // Generate AI response
            generateResponse(conversationId)
        }
    }

    /**
     * Generate an AI response to the current conversation.
     */
    private fun generateResponse(conversationId: String) {
        if (!isModelLoaded) {
            // No model loaded
            return
        }

        generationJob = viewModelScope.launch {
            _generationState.value = GenerationState.Loading
            
            // Get completed messages for this conversation
            val completedMessages = _messages.value.filter { it.isComplete }
            val lastUserMessage = completedMessages.lastOrNull { it.role == MessageRole.USER }?.content ?: ""

            // Get current model info for prompt template
            val model = modelManager.currentModel.value
            val preferences = userPreferences.value
            
            // Perform web search if enabled
            var webSearchContext = ""
            if (preferences.webSearchEnabled && lastUserMessage.isNotBlank()) {
                _isSearchingWeb.value = true
                try {
                    val searchResponse = webSearchService.search(lastUserMessage)
                    if (searchResponse.success && searchResponse.results.isNotEmpty()) {
                        webSearchContext = webSearchService.formatResultsForLLM(searchResponse)
                        _webSearchResults.value = webSearchContext
                    }
                } catch (e: Exception) {
                    // Web search failed, continue without it
                }
                _isSearchingWeb.value = false
            }

            // Create placeholder assistant message
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                isComplete = false
            )
            
            _currentGeneratingMessageId.value = assistantMessage.id
            _messages.value = _messages.value + assistantMessage
            
            // Build system prompt with thinking mode if enabled
            var systemPrompt = preferences.defaultSystemPrompt
            if (preferences.thinkingModeEnabled) {
                systemPrompt = ThinkingModeParser.createThinkingModePrompt(systemPrompt)
            }
            
            // Add web search context to the prompt if available
            val messagesForPrompt = if (webSearchContext.isNotBlank()) {
                // Insert web search results before the last user message
                val mutableMessages = completedMessages.toMutableList()
                if (mutableMessages.isNotEmpty()) {
                    val lastMessage = mutableMessages.last()
                    if (lastMessage.role == MessageRole.USER) {
                        mutableMessages[mutableMessages.lastIndex] = lastMessage.copy(
                            content = webSearchContext + lastMessage.content
                        )
                    }
                }
                mutableMessages
            } else {
                completedMessages
            }
            
            // Build prompt from conversation history
            val prompt = inferenceEngine.buildPrompt(
                messages = messagesForPrompt,
                systemPrompt = systemPrompt,
                promptTemplate = model?.promptTemplate ?: "chatml"
            )

            val startTime = System.currentTimeMillis()
            var tokensGenerated = 0
            var currentContent = ""

            // Generate with streaming
            inferenceEngine.generateStream(
                prompt = prompt,
                config = preferences.defaultGenerationConfig,
                onTokenGenerated = { token ->
                    tokensGenerated++
                    currentContent += token
                    
                    // Update message content
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMessage.id) {
                            msg.copy(content = currentContent)
                        } else msg
                    }
                    
                    // Update generation state
                    val elapsed = System.currentTimeMillis() - startTime
                    val tokensPerSecond = if (elapsed > 0) {
                        tokensGenerated * 1000.0 / elapsed
                    } else 0.0
                    
                    _generationState.value = GenerationState.Generating(
                        tokensGenerated = tokensGenerated,
                        tokensPerSecond = tokensPerSecond
                    )
                }
            ).catch { error ->
                // Handle generation error
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessage.id) {
                        msg.copy(
                            content = "Error: ${error.message}",
                            isComplete = true
                        )
                    } else msg
                }
                _generationState.value = GenerationState.Complete(
                    GenerationResult.Error(error.message ?: "Unknown error")
                )
            }.collect { result ->
                // Generation complete
                val endTime = System.currentTimeMillis()
                val generationTime = endTime - startTime
                
                val finalMessage = assistantMessage.copy(
                    content = currentContent,
                    tokensGenerated = tokensGenerated,
                    generationTimeMs = generationTime,
                    isComplete = true
                )
                
                // Update in list and save to database
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMessage.id) finalMessage else msg
                }
                
                conversationRepository.addMessage(finalMessage)
                
                _generationState.value = GenerationState.Complete(result)
                _currentGeneratingMessageId.value = null
                
                // Auto-generate title if this is the first exchange
                if (_messages.value.size <= 2) {
                    generateConversationTitleUseCase(conversationId)
                }
            }
        }
    }

    /**
     * Stop the current generation.
     */
    fun stopGeneration() {
        inferenceEngine.cancelGeneration()
        generationJob?.cancel()
        generationJob = null
        
        // Mark current generating message as complete
        _currentGeneratingMessageId.value?.let { messageId ->
            _messages.value = _messages.value.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(isComplete = true)
                } else msg
            }
        }
        
        _generationState.value = GenerationState.Complete(GenerationResult.Cancelled)
        _currentGeneratingMessageId.value = null
    }

    /**
     * Regenerate the last assistant response.
     */
    fun regenerateLastResponse() {
        val conversationId = _currentConversationId.value ?: return
        if (isGenerating) return

        viewModelScope.launch {
            // Remove last assistant message if exists
            val lastMessage = _messages.value.lastOrNull()
            if (lastMessage?.role == MessageRole.ASSISTANT) {
                _messages.value = _messages.value.dropLast(1)
                conversationRepository.deleteMessage(lastMessage.id)
            }
            
            // Clear KV cache and regenerate
            modelManager.clearKVCache()
            generateResponse(conversationId)
        }
    }

    /**
     * Clear the current conversation's messages.
     */
    fun clearConversation() {
        viewModelScope.launch {
            _currentConversationId.value?.let { conversationId ->
                // Delete all messages but keep conversation
                _messages.value.forEach { message ->
                    conversationRepository.deleteMessage(message.id)
                }
                _messages.value = emptyList()
                modelManager.clearKVCache()
            }
        }
    }

    /**
     * Delete a specific message.
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            _messages.value = _messages.value.filter { it.id != messageId }
            conversationRepository.deleteMessage(messageId)
        }
    }

    /**
     * Copy message content to clipboard.
     */
    fun copyMessageContent(message: ChatMessage): String {
        return message.content
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
    }
}
