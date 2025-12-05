package com.localllm.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.data.model.MessageRole
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import com.localllm.app.ui.screen.PromptTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Prompt Lab Screen
 */
@HiltViewModel
class PromptLabViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    companion object {
        private const val TAG = "PromptLabViewModel"
    }

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(512)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<PromptTemplate?>(null)
    val selectedTemplate: StateFlow<PromptTemplate?> = _selectedTemplate.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private var generationJob: Job? = null

    init {
        observeModelState()
    }

    private fun observeModelState() {
        viewModelScope.launch {
            modelManager.loadingState.collect { state ->
                _isModelLoaded.value = state is ModelLoadingState.Loaded
            }
        }
    }

    fun setPrompt(text: String) {
        _prompt.value = text
    }

    fun clearPrompt() {
        _prompt.value = ""
    }

    fun setTemperature(value: Float) {
        _temperature.value = value
    }

    fun setMaxTokens(value: Int) {
        _maxTokens.value = value
    }

    fun selectTemplate(template: PromptTemplate) {
        _selectedTemplate.value = if (template.id == "free") null else template
        if (template.template.isNotEmpty()) {
            _prompt.value = template.template
        }
    }

    fun clearAll() {
        _prompt.value = ""
        _output.value = ""
        _selectedTemplate.value = null
    }

    fun generate() {
        if (_prompt.value.isBlank() || _isGenerating.value) return

        generationJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _output.value = ""

                Log.d(TAG, "Starting generation with prompt: ${_prompt.value.take(50)}...")

                // Create a single-turn message for generation
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = "prompt_lab",
                    role = MessageRole.USER,
                    content = _prompt.value,
                    timestamp = System.currentTimeMillis()
                )

                // Build prompt with appropriate template (use chatml for prompt lab)
                val fullPrompt = inferenceEngine.buildPrompt(
                    messages = listOf(message),
                    systemPrompt = "You are a helpful AI assistant. Respond directly to the user's request.",
                    promptTemplate = "chatml"
                )

                // Generate response using streaming
                val config = GenerationConfig(
                    maxTokens = _maxTokens.value,
                    temperature = _temperature.value,
                    topP = 0.9f,
                    topK = 40,
                    repeatPenalty = 1.1f
                )

                inferenceEngine.generateStream(
                    prompt = fullPrompt,
                    config = config,
                    onTokenGenerated = { token ->
                        _output.value += token
                    }
                ).collect { result ->
                    when (result) {
                        is GenerationResult.Success -> {
                            Log.d(TAG, "Generation complete, output length: ${_output.value.length}")
                        }
                        is GenerationResult.Error -> {
                            Log.e(TAG, "Generation error: ${result.message}")
                            if (_output.value.isEmpty()) {
                                _output.value = "Error: ${result.message}"
                            }
                        }
                        is GenerationResult.Cancelled -> {
                            Log.d(TAG, "Generation cancelled")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Generation error", e)
                _output.value = "Error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        inferenceEngine.cancelGeneration()
        _isGenerating.value = false
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
    }
}
