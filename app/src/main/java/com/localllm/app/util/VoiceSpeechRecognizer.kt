package com.localllm.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Voice input state
 */
sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Listening : VoiceInputState()
    object Processing : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

/**
 * Wrapper class for Android SpeechRecognizer API
 * Provides speech-to-text functionality for voice input in chat.
 */
class VoiceSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()
    
    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()
    
    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Start listening for voice input
     */
    fun startListening(languageCode: String = "en-US") {
        if (!isAvailable()) {
            onError("Speech recognition not available on this device")
            _state.value = VoiceInputState.Error("Speech recognition not available")
            return
        }
        
        // Clean up existing recognizer
        stopListening()
        
        // Create new recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
        
        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        // Start listening
        _state.value = VoiceInputState.Listening
        _partialResults.value = ""
        speechRecognizer?.startListening(intent)
    }
    
    /**
     * Stop listening and clean up resources
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceInputState.Idle
        _partialResults.value = ""
    }
    
    /**
     * Cancel listening without processing results
     */
    fun cancel() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceInputState.Idle
        _partialResults.value = ""
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopListening()
    }
    
    /**
     * Create recognition listener with callbacks
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceInputState.Listening
            }
            
            override fun onBeginningOfSpeech() {
                // User started speaking
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could be used for visualization
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                _state.value = VoiceInputState.Processing
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }
                
                _state.value = VoiceInputState.Error(errorMessage)
                onError(errorMessage)
                
                // Auto-reset to idle after error
                _state.value = VoiceInputState.Idle
                _partialResults.value = ""
            }
            
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val recognizedText = matches[0]
                        onResult(recognizedText)
                    }
                }
                _state.value = VoiceInputState.Idle
                _partialResults.value = ""
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        _partialResults.value = matches[0]
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Optional: handle custom events
            }
        }
    }
}
