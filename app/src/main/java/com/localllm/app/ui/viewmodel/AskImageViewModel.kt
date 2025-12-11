package com.localllm.app.ui.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.model.PromptTemplate
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * ViewModel for Ask Image Screen - Vision-based image Q&A
 * 
 * Supports multimodal models like:
 * - Moondream2: Tiny but capable vision model
 * - NanoLLaVA: Ultra-compact LLaVA variant
 * - LLaVA-Phi3: Phi-3 based vision model
 * - SmolVLM: HuggingFace's compact VLM
 */
@HiltViewModel
class AskImageViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    companion object {
        private const val TAG = "AskImageViewModel"
        private const val MAX_IMAGE_SIZE = 384 // Max dimension for image preprocessing
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
    
    private val _currentVisionModel = MutableStateFlow<ModelInfo?>(null)
    val currentVisionModel: StateFlow<ModelInfo?> = _currentVisionModel.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var generationJob: Job? = null

    init {
        observeModelState()
    }

    private fun observeModelState() {
        viewModelScope.launch {
            modelManager.loadingState.collect { state ->
                when (state) {
                    is ModelLoadingState.Loaded -> {
                        _isModelLoaded.value = true
                        val model = state.model
                        // Check if the loaded model supports vision
                        val supportsVision = model.supportsVision || isVisionModelByName(model)
                        _isVisionSupported.value = supportsVision
                        _currentVisionModel.value = if (supportsVision) model else null
                        
                        if (supportsVision) {
                            _statusMessage.value = "✅ Vision model loaded: ${model.name}"
                            Log.d(TAG, "Vision model detected: ${model.name}")
                        } else {
                            _statusMessage.value = "ℹ️ Text-only model loaded. Load a vision model for image analysis."
                        }
                    }
                    is ModelLoadingState.Loading -> {
                        _statusMessage.value = "Loading model..."
                    }
                    is ModelLoadingState.Error -> {
                        _isModelLoaded.value = false
                        _isVisionSupported.value = false
                        _currentVisionModel.value = null
                        _statusMessage.value = "⚠️ ${state.message}"
                    }
                    ModelLoadingState.NotLoaded -> {
                        _isModelLoaded.value = false
                        _isVisionSupported.value = false
                        _currentVisionModel.value = null
                        _statusMessage.value = ""
                    }
                }
            }
        }
    }
    
    /**
     * Check if a model supports vision based on its name/tags.
     */
    private fun isVisionModelByName(model: ModelInfo): Boolean {
        val name = model.name.lowercase()
        val tags = model.tags.map { it.lowercase() }
        
        return model.supportsVision ||
               name.contains("llava") ||
               name.contains("moondream") ||
               name.contains("smolvlm") ||
               name.contains("minicpm-v") ||
               name.contains("bakllava") ||
               name.contains("vision") ||
               name.contains("🖼️") ||
               tags.contains("vision") ||
               tags.contains("multimodal")
    }

    fun setImage(bitmap: Bitmap) {
        // Preprocess image for vision model
        val processedBitmap = preprocessImage(bitmap)
        _selectedImage.value = processedBitmap
        Log.d(TAG, "Image set: ${processedBitmap.width}x${processedBitmap.height}")
    }
    
    /**
     * Preprocess image for vision model inference.
     * Resizes large images to prevent memory issues.
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        
        if (maxDim <= MAX_IMAGE_SIZE) {
            return bitmap
        }
        
        val scale = MAX_IMAGE_SIZE.toFloat() / maxDim
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        Log.d(TAG, "Resizing image from ${bitmap.width}x${bitmap.height} to ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
        val image = _selectedImage.value ?: run {
            _answer.value = "⚠️ Please select an image first."
            return
        }
        
        val questionText = _question.value.takeIf { it.isNotBlank() } ?: run {
            _answer.value = "⚠️ Please enter a question about the image."
            return
        }
        
        if (!_isModelLoaded.value) {
            _answer.value = "⚠️ No model loaded.\n\n" +
                    "Please load a vision-capable model from the Model Library:\n" +
                    "• Moondream2 (1.9B) - Recommended for mobile\n" +
                    "• NanoLLaVA (1B) - Ultra-compact\n" +
                    "• SmolVLM (2B) - Balanced\n" +
                    "• LLaVA-Phi3 (4.2B) - Best quality"
            return
        }

        if (!_isVisionSupported.value) {
            _answer.value = "⚠️ Current model doesn't support vision.\n\n" +
                    "The loaded model is text-only. Please load a vision model:\n\n" +
                    "📱 **Recommended for Mobile:**\n" +
                    "• Moondream2 (Q4) - ~1.2GB, best balance\n" +
                    "• NanoLLaVA (Q4) - ~800MB, ultra-compact\n\n" +
                    "💪 **For Flagship Devices:**\n" +
                    "• SmolVLM (Q4) - ~1.3GB\n" +
                    "• LLaVA-Phi3 (Q4) - ~2.5GB"
            return
        }

        generationJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _answer.value = ""

                Log.d(TAG, "Processing vision query: $questionText")
                Log.d(TAG, "Image size: ${image.width}x${image.height}")
                
                val model = _currentVisionModel.value ?: return@launch
                
                // Build vision-specific prompt
                val visionPrompt = buildVisionPrompt(questionText, model.promptTemplate)
                Log.d(TAG, "Vision prompt template: ${model.promptTemplate}")
                
                // Generate response using the inference engine
                // Note: Full vision inference requires native CLIP/vision encoder support
                // Current implementation uses vision-formatted prompts
                
                _answer.value = "🖼️ Analyzing image...\n\n"
                
                inferenceEngine.generateStream(
                    prompt = visionPrompt,
                    config = GenerationConfig(
                        maxTokens = 512,
                        temperature = 0.7f,
                        topP = 0.9f
                    ),
                    onTokenGenerated = { token ->
                        _answer.value += token
                    }
                )
                .collect { result ->
                    when (result) {
                        is GenerationResult.Success -> {
                            Log.d(TAG, "Vision generation complete: ${result.tokensGenerated} tokens")
                        }
                        is GenerationResult.Error -> {
                            Log.e(TAG, "Vision generation error: ${result.message}")
                            if (_answer.value.isEmpty() || _answer.value.startsWith("🖼️")) {
                                _answer.value = "❌ Error: ${result.message}"
                            }
                        }
                        is GenerationResult.Cancelled -> {
                            Log.d(TAG, "Vision generation cancelled")
                            if (_answer.value.startsWith("🖼️")) {
                                _answer.value = "Generation cancelled."
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in vision query", e)
                _answer.value = "❌ Error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Build a vision-specific prompt based on the model's prompt template.
     */
    private fun buildVisionPrompt(question: String, template: String): String {
        return when (template) {
            PromptTemplate.LLAVA -> buildLLaVAPrompt(question)
            PromptTemplate.MOONDREAM -> buildMoondreamPrompt(question)
            PromptTemplate.MINICPM_V -> buildMiniCPMVPrompt(question)
            PromptTemplate.SMOLVLM -> buildSmolVLMPrompt(question)
            else -> buildGenericVisionPrompt(question)
        }
    }
    
    /**
     * LLaVA prompt format with image token.
     */
    private fun buildLLaVAPrompt(question: String): String {
        return """<|im_start|>system
You are a helpful vision assistant that can analyze images and answer questions about them.
<|im_end|>
<|im_start|>user
<image>
$question
<|im_end|>
<|im_start|>assistant
"""
    }
    
    /**
     * Moondream2 prompt format.
     */
    private fun buildMoondreamPrompt(question: String): String {
        return """<image>

Question: $question

Answer:"""
    }
    
    /**
     * MiniCPM-V prompt format.
     */
    private fun buildMiniCPMVPrompt(question: String): String {
        return """<|im_start|>user
<image>
$question<|im_end|>
<|im_start|>assistant
"""
    }
    
    /**
     * SmolVLM prompt format.
     */
    private fun buildSmolVLMPrompt(question: String): String {
        return """User:<image>$question
Assistant:"""
    }
    
    /**
     * Generic vision prompt for unknown model types.
     */
    private fun buildGenericVisionPrompt(question: String): String {
        return """<|im_start|>system
You are a vision AI assistant. Describe what you see in the image and answer the user's question.
<|im_end|>
<|im_start|>user
[Image provided]
$question
<|im_end|>
<|im_start|>assistant
Based on the image provided, """
    }
    
    /**
     * Convert bitmap to base64 for potential API usage.
     */
    @Suppress("unused")
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
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
