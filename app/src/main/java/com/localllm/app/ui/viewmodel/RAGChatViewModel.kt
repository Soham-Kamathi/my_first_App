package com.localllm.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.MessageRole
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import com.localllm.app.rag.ChunkSearchResult
import com.localllm.app.rag.DocumentInfo
import com.localllm.app.rag.IndexingState
import com.localllm.app.rag.VectorStore
import com.localllm.app.util.DocumentParser
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
 * UI State for RAG Chat
 */
data class RAGChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isGenerating: Boolean = false,
    val ragEnabled: Boolean = true,
    val indexedDocuments: List<DocumentInfo> = emptyList(),
    val currentContext: String? = null,
    val contextSources: List<ChunkSearchResult> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel for RAG-enhanced chat
 * Uses vector embeddings for semantic document retrieval
 */
@HiltViewModel
class RAGChatViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager,
    private val vectorStore: VectorStore,
    private val documentParser: DocumentParser
) : ViewModel() {

    companion object {
        private const val TAG = "RAGChatViewModel"
        private const val MAX_CONTEXT_LENGTH = 2000
        private const val TOP_K_CHUNKS = 3
    }

    private val _uiState = MutableStateFlow(RAGChatUiState())
    val uiState: StateFlow<RAGChatUiState> = _uiState.asStateFlow()

    val loadingState: StateFlow<ModelLoadingState> = modelManager.loadingState
    val indexingState: StateFlow<IndexingState> = vectorStore.indexingState

    val isModelLoaded: Boolean
        get() = modelManager.isModelLoaded

    private var generationJob: Job? = null

    init {
        documentParser.initializePdfBox()
        loadIndexedDocuments()
    }

    private fun loadIndexedDocuments() {
        viewModelScope.launch {
            val docs = vectorStore.getIndexedDocuments()
            _uiState.value = _uiState.value.copy(indexedDocuments = docs)
        }
    }

    fun indexDocument(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                val result = documentParser.parseDocument(uri, fileName)
                result.fold(
                    onSuccess = { document ->
                        vectorStore.indexDocument(document).fold(
                            onSuccess = { chunkCount ->
                                Log.d(TAG, "Indexed $chunkCount chunks from $fileName")
                                loadIndexedDocuments()
                                val message = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = "rag_chat",
                                    role = MessageRole.ASSISTANT,
                                    content = "✅ Successfully indexed **$fileName** with $chunkCount chunks.\n\nYou can now ask questions about this document!"
                                )
                                _uiState.value = _uiState.value.copy(
                                    messages = _uiState.value.messages + message
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Failed to index document: ${error.message}"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to parse document: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Document indexing failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Indexing error: ${e.message}"
                )
            }
        }
    }

    fun toggleRAG() {
        _uiState.value = _uiState.value.copy(ragEnabled = !_uiState.value.ragEnabled)
    }

    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = "rag_chat",
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
                val relevantContext = if (_uiState.value.ragEnabled) {
                    retrieveContext(input)
                } else {
                    null
                }

                val systemPrompt = buildRAGPrompt(relevantContext, input)

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = "rag_chat",
                    role = MessageRole.ASSISTANT,
                    content = ""
                )

                val finalMessages = updatedMessages + assistantMessage
                _uiState.value = _uiState.value.copy(messages = finalMessages)

                val responseBuilder = StringBuilder()

                inferenceEngine.generateStream(
                    prompt = systemPrompt,
                    onTokenGenerated = { token ->
                        responseBuilder.append(token)
                        val lastIndex = finalMessages.lastIndex
                        if (lastIndex >= 0) {
                            val updated = finalMessages.toMutableList()
                            updated[lastIndex] = assistantMessage.copy(content = responseBuilder.toString())
                            _uiState.value = _uiState.value.copy(messages = updated)
                        }
                    }
                )

                val lastIndex = finalMessages.lastIndex
                if (lastIndex >= 0) {
                    val updated = finalMessages.toMutableList()
                    updated[lastIndex] = assistantMessage.copy(content = responseBuilder.toString())
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        isGenerating = false,
                        currentContext = null
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Generation failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun retrieveContext(query: String): String? {
        val results = vectorStore.search(query, topK = TOP_K_CHUNKS, similarityThreshold = 0.3f)

        if (results.isEmpty()) {
            Log.d(TAG, "No relevant context found for query")
            return null
        }

        _uiState.value = _uiState.value.copy(contextSources = results)

        val context = vectorStore.buildContextFromResults(results, maxLength = MAX_CONTEXT_LENGTH)
        Log.d(TAG, "Retrieved context: ${context.length} chars from ${results.size} chunks")

        return context
    }

    private fun buildRAGPrompt(context: String?, query: String): String {
        return if (context != null) {
            "You are a helpful AI assistant with access to relevant document context.\n\nRELEVANT CONTEXT:\n$context\n\nUSER QUESTION: $query\n\nINSTRUCTIONS:\n- Answer the question based on the context provided above\n- If the context contains the answer, cite the relevant parts\n- If the context doesn't contain enough information, say so and provide a general answer\n- Be concise but thorough\n- Use markdown formatting for readability\n\nAssistant: $query"
        } else {
            query
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        inferenceEngine.cancelGeneration()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            vectorStore.deleteDocument(documentId)
            loadIndexedDocuments()
        }
    }

    fun clearVectorStore() {
        viewModelScope.launch {
            vectorStore.clearAll()
            loadIndexedDocuments()
            _uiState.value = _uiState.value.copy(messages = emptyList())
        }
    }
}
