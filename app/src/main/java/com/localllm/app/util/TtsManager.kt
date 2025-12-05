package com.localllm.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text-to-Speech states
 */
enum class TtsState {
    IDLE,
    SPEAKING,
    PAUSED,
    ERROR
}

/**
 * TTS configuration
 */
data class TtsConfig(
    val speechRate: Float = 1.0f,  // 0.5 to 2.0
    val pitch: Float = 1.0f,       // 0.5 to 2.0
    val language: Locale = Locale.US
)

/**
 * Text-to-Speech Manager
 * Singleton wrapper around Android TTS for reading responses aloud
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    private val _state = MutableStateFlow(TtsState.IDLE)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    private val _config = MutableStateFlow(TtsConfig())
    val config: StateFlow<TtsConfig> = _config.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<Locale>>(emptyList())
    val availableLanguages: StateFlow<List<Locale>> = _availableLanguages.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                _isAvailable.value = true
                
                // Set default language
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Default language not supported")
                }
                
                // Get available languages
                _availableLanguages.value = tts?.availableLanguages?.toList() ?: emptyList()
                
                // Set up progress listener
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = TtsState.SPEAKING
                        _currentUtteranceId.value = utteranceId
                        Log.d(TAG, "TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = TtsState.IDLE
                        _currentUtteranceId.value = null
                        Log.d(TAG, "TTS done: $utteranceId")
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = TtsState.ERROR
                        _currentUtteranceId.value = null
                        Log.e(TAG, "TTS error: $utteranceId")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _state.value = TtsState.ERROR
                        _currentUtteranceId.value = null
                        Log.e(TAG, "TTS error: $utteranceId, code: $errorCode")
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        _state.value = TtsState.IDLE
                        _currentUtteranceId.value = null
                        Log.d(TAG, "TTS stopped: $utteranceId, interrupted: $interrupted")
                    }
                })

                // Apply initial config
                applyConfig(_config.value)
                
                // Speak pending text if any
                pendingText?.let { speak(it) }
                pendingText = null
                
                Log.d(TAG, "TTS initialized successfully")
            } else {
                isInitialized = false
                _isAvailable.value = false
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    /**
     * Speak text aloud
     * @param text Text to speak
     * @param queueMode QUEUE_FLUSH (replace current) or QUEUE_ADD (add to queue)
     * @return Utterance ID for tracking
     */
    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ): String? {
        if (!isInitialized) {
            pendingText = text
            Log.w(TAG, "TTS not initialized, queuing text")
            return null
        }

        if (text.isBlank()) return null

        val utteranceId = UUID.randomUUID().toString()
        
        // Clean text for better speech
        val cleanedText = cleanTextForSpeech(text)
        
        val result = tts?.speak(cleanedText, queueMode, null, utteranceId)
        
        if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "Speaking: ${cleanedText.take(50)}...")
            return utteranceId
        } else {
            Log.e(TAG, "Failed to speak, result: $result")
            _state.value = TtsState.ERROR
            return null
        }
    }

    /**
     * Stop speaking
     */
    fun stop() {
        tts?.stop()
        _state.value = TtsState.IDLE
        _currentUtteranceId.value = null
        Log.d(TAG, "TTS stopped")
    }

    /**
     * Pause speaking (stops current utterance)
     */
    fun pause() {
        tts?.stop()
        _state.value = TtsState.PAUSED
        Log.d(TAG, "TTS paused")
    }

    /**
     * Set speech rate
     * @param rate 0.5 (slow) to 2.0 (fast), default 1.0
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        _config.value = _config.value.copy(speechRate = clampedRate)
        tts?.setSpeechRate(clampedRate)
        Log.d(TAG, "Speech rate set to: $clampedRate")
    }

    /**
     * Set pitch
     * @param pitch 0.5 (low) to 2.0 (high), default 1.0
     */
    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        _config.value = _config.value.copy(pitch = clampedPitch)
        tts?.setPitch(clampedPitch)
        Log.d(TAG, "Pitch set to: $clampedPitch")
    }

    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        return if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            _config.value = _config.value.copy(language = locale)
            Log.d(TAG, "Language set to: ${locale.displayName}")
            true
        } else {
            Log.w(TAG, "Language not supported: ${locale.displayName}")
            false
        }
    }

    /**
     * Update TTS configuration
     */
    fun updateConfig(config: TtsConfig) {
        _config.value = config
        applyConfig(config)
    }

    private fun applyConfig(config: TtsConfig) {
        tts?.setSpeechRate(config.speechRate)
        tts?.setPitch(config.pitch)
        tts?.setLanguage(config.language)
    }

    /**
     * Clean text for better TTS pronunciation
     */
    private fun cleanTextForSpeech(text: String): String {
        return text
            // Remove markdown code blocks
            .replace(Regex("```[\\s\\S]*?```"), "code block omitted")
            // Remove inline code
            .replace(Regex("`[^`]+`"), "code")
            // Remove markdown headers
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            // Remove markdown bold/italic
            .replace(Regex("[*_]{1,2}([^*_]+)[*_]{1,2}"), "$1")
            // Remove URLs
            .replace(Regex("https?://\\S+"), "link")
            // Replace bullet points
            .replace(Regex("^[•\\-*]\\s*", RegexOption.MULTILINE), "")
            // Replace numbered lists
            .replace(Regex("^\\d+\\.\\s*", RegexOption.MULTILINE), "")
            // Clean up extra whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    /**
     * Release TTS resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isAvailable.value = false
        Log.d(TAG, "TTS shutdown")
    }
}
