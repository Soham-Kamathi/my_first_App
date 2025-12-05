package com.localllm.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.MessageRole
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.DocumentParser
import com.localllm.app.util.ParsedDocument
import com.localllm.app.util.TextChunk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI State for Document Chat screen
 */
data class DocumentChatUiState(
    val documentLoaded: Boolean = false,
    val documentName: String = "",
    val documentPreview: String = "",
    val documentWordCount: Int = 0,
    val documentPageCount: Int = 1,
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isGenerating: Boolean = false,
    val isLoadingDocument: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for Document Chat feature
 * Allows users to chat with documents (PDF, TXT, MD) using RAG-style context
 */
@HiltViewModel
class DocumentChatViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager,
    private val documentParser: DocumentParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentChatUiState())
    val uiState: StateFlow<DocumentChatUiState> = _uiState.asStateFlow()

    val loadingState: StateFlow<ModelLoadingState> = modelManager.loadingState

    val isModelLoaded: Boolean
        get() = modelManager.isModelLoaded

    private var parsedDocument: ParsedDocument? = null
    private var documentChunks: List<TextChunk> = emptyList()
    private var generationJob: Job? = null

    init {
        documentParser.initializePdfBox()
    }

    /**
     * Load document from URI
     */
    fun loadDocument(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoadingDocument = true, errorMessage = null)

            val result = documentParser.parseDocument(uri, fileName)
            result.fold(
                onSuccess = { document ->
                    parsedDocument = document
                    documentChunks = documentParser.chunkDocument(document.content)

                    val preview = if (document.content.length > 500) {
                        document.content.take(500) + "..."
                    } else {
                        document.content
                    }

                    val welcomeMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = "document_chat",
                        role = MessageRole.ASSISTANT,
                        content = "📄 Document loaded: **${document.fileName}**\n\n" +
                                "• Pages: ${document.pageCount}\n" +
                                "• Words: ${document.wordCount}\n" +
                                "• Characters: ${document.charCount}\n" +
                                "• Chunks: ${documentChunks.size}\n\n" +
                                "Ask me anything about this document!"
                    )

                    _uiState.value = _uiState.value.copy(
                        documentLoaded = true,
                        documentName = document.fileName,
                        documentPreview = preview,
                        documentWordCount = document.wordCount,
                        documentPageCount = document.pageCount,
                        isLoadingDocument = false,
                        messages = listOf(welcomeMessage)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDocument = false,
                        errorMessage = "Failed to load document: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Update current input text
     */
    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    /**
     * Send message and generate response
     */
    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = "document_chat",
            role = MessageRole.USER,
            content = input
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            currentInput = "",
            isGenerating = true,
            errorMessage = null
        )

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Find relevant chunks using keyword matching
                val relevantContext = findRelevantContext(input)

                // Build the prompt with document context
                val systemPrompt = buildDocumentSystemPrompt(relevantContext)
                val fullPrompt = buildConversationPrompt(systemPrompt, updatedMessages)

                val responseBuilder = StringBuilder()
                val assistantMessageId = UUID.randomUUID().toString()
                val assistantMessage = ChatMessage(
                    id = assistantMessageId,
                    conversationId = "document_chat",
                    role = MessageRole.ASSISTANT,
                    content = ""
                )

                _uiState.value = _uiState.value.copy(
                    messages = updatedMessages + assistantMessage
                )

                inferenceEngine.generateStream(
                    prompt = fullPrompt,
                    onTokenGenerated = { token ->
                        responseBuilder.append(token)
                        val currentMessages = _uiState.value.messages.toMutableList()
                        val lastIndex = currentMessages.lastIndex
                        if (lastIndex >= 0) {
                            currentMessages[lastIndex] = assistantMessage.copy(
                                content = responseBuilder.toString()
                            )
                            _uiState.value = _uiState.value.copy(messages = currentMessages)
                        }
                    }
                )

                val finalMessages = _uiState.value.messages.toMutableList()
                val lastIndex = finalMessages.lastIndex
                if (lastIndex >= 0) {
                    finalMessages[lastIndex] = assistantMessage.copy(
                        content = responseBuilder.toString()
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = finalMessages,
                        isGenerating = false
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Generation failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Find relevant document chunks for the query
     */
    private fun findRelevantContext(query: String): String {
        if (documentChunks.isEmpty()) {
            return parsedDocument?.content?.take(3000) ?: ""
        }

        val queryWords = query.lowercase().split(Regex("[\\s,\\.\\?!]+"))
            .filter { it.length > 3 }
            .toSet()

        // Score each chunk based on keyword overlap
        val scoredChunks = documentChunks.map { chunk ->
            val chunkWords = chunk.content.lowercase().split(Regex("[\\s,\\.\\n]+"))
                .filter { it.length > 3 }
                .toSet()
            val score = queryWords.intersect(chunkWords).size
            chunk to score
        }.sortedByDescending { it.second }

        // Take top 3 most relevant chunks
        val relevantChunks = scoredChunks.take(3)
            .filter { it.second > 0 }
            .map { it.first }

        return if (relevantChunks.isNotEmpty()) {
            relevantChunks.joinToString("\n\n---\n\n") { it.content }
        } else {
            // If no keyword matches, return beginning of document
            parsedDocument?.content?.take(3000) ?: ""
        }
    }

    private fun buildDocumentSystemPrompt(context: String): String {
        return """You are a helpful AI assistant that answers questions about documents.
            
DOCUMENT CONTEXT:
$context

INSTRUCTIONS:
- Answer questions based ONLY on the document content provided above
- If the information is not in the document, say so clearly
- Quote relevant parts of the document when helpful
- Be concise but thorough in your answers"""
    }

    private fun buildConversationPrompt(systemPrompt: String, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append(systemPrompt)
        sb.append("\n\n")

        // Add conversation history (limited to last 10 messages)
        val recentMessages = messages.takeLast(10)
        for (message in recentMessages) {
            when (message.role) {
                MessageRole.USER -> sb.append("User: ${message.content}\n")
                MessageRole.ASSISTANT -> sb.append("Assistant: ${message.content}\n")
                MessageRole.SYSTEM -> {} // Skip system messages in conversation
            }
        }

        sb.append("Assistant:")
        return sb.toString()
    }

    /**
     * Stop ongoing generation
     */
    fun stopGeneration() {
        generationJob?.cancel()
        inferenceEngine.cancelGeneration()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    /**
     * Clear loaded document
     */
    fun clearDocument() {
        parsedDocument = null
        documentChunks = emptyList()
        _uiState.value = DocumentChatUiState()
    }

    /**
     * Dismiss error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
