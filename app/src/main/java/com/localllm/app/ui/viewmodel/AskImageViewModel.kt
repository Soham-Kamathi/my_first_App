package com.localllm.app.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Ask Image Screen - Vision-based image Q&A
 */
@HiltViewModel
class AskImageViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    companion object {
        private const val TAG = "AskImageViewModel"
    }

    private val _selectedImage = MutableStateFlow<Bitmap?>(null)
    val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()

    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()

    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isVisionSupported = MutableStateFlow(false)
    val isVisionSupported: StateFlow<Boolean> = _isVisionSupported.asStateFlow()

    private var generationJob: Job? = null

    init {
        observeModelState()
    }

    private fun observeModelState() {
        viewModelScope.launch {
            modelManager.loadingState.collect { state ->
                _isModelLoaded.value = state is ModelLoadingState.Loaded
                // Check if model supports vision (LLaVA, BakLLaVA, etc.)
                // For now, we'll assume vision is not supported until we add proper detection
                _isVisionSupported.value = false // Will be updated when vision models are supported
            }
        }
    }

    fun setImage(bitmap: Bitmap) {
        _selectedImage.value = bitmap
        Log.d(TAG, "Image set: ${bitmap.width}x${bitmap.height}")
    }

    fun clearImage() {
        _selectedImage.value = null
        _answer.value = ""
    }

    fun setQuestion(text: String) {
        _question.value = text
    }

    fun clearAnswer() {
        _answer.value = ""
    }

    fun clearAll() {
        _selectedImage.value = null
        _question.value = ""
        _answer.value = ""
    }

    fun askQuestion() {
        val image = _selectedImage.value ?: return
        val questionText = _question.value.takeIf { it.isNotBlank() } ?: return

        if (!_isVisionSupported.value) {
            _answer.value = "⚠️ Vision models are not yet supported.\n\n" +
                    "This feature requires a multimodal model like LLaVA or BakLLaVA " +
                    "that can process both images and text.\n\n" +
                    "Vision support will be added in a future update."
            return
        }

        generationJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _answer.value = ""

                Log.d(TAG, "Asking about image: $questionText")

                // TODO: Implement actual vision model inference
                // This will require:
                // 1. Loading a vision-capable model (LLaVA, BakLLaVA)
                // 2. Preprocessing the image (resizing, normalization)
                // 3. Encoding the image with a vision encoder (CLIP)
                // 4. Passing image embeddings + text to the LLM

                // For now, show a placeholder message
                _answer.value = "🔮 Vision inference is not yet implemented.\n\n" +
                        "To enable this feature, we need to:\n" +
                        "• Add vision encoder support (CLIP)\n" +
                        "• Integrate multimodal model loading\n" +
                        "• Process image embeddings\n\n" +
                        "Stay tuned for future updates!"

            } catch (e: Exception) {
                Log.e(TAG, "Error asking question", e)
                _answer.value = "Error: ${e.message}"
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
        // Release bitmap to free memory
        _selectedImage.value = null
    }
}
