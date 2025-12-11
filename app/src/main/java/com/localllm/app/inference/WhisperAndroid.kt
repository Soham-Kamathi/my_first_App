package com.localllm.app.inference

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WhisperAndroid - JNI wrapper for whisper.cpp audio transcription
 * 
 * This class provides a Kotlin interface to the native whisper.cpp library
 * for automatic speech recognition (ASR) / audio transcription.
 * 
 * Usage:
 * 1. Call loadModel() with path to a Whisper GGUF model
 * 2. Call transcribe() with audio file path to get transcription
 * 3. Call unloadModel() when done to free resources
 */
@Singleton
class WhisperAndroid @Inject constructor() {
    
    companion object {
        private const val TAG = "WhisperAndroid"
        
        // Load native library
        init {
            try {
                System.loadLibrary("llama-android")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    // Native Whisper context pointer (0 = not loaded)
    private var whisperContextPtr: Long = 0
    
    // Currently loaded model path
    private var currentModelPath: String? = null
    
    // Progress callback for transcription
    var progressCallback: ((Float) -> Unit)? = null
    
    /**
     * Check if a Whisper model is currently loaded
     */
    fun isModelLoaded(): Boolean = whisperContextPtr != 0L
    
    /**
     * Get the path of the currently loaded model
     */
    fun getCurrentModelPath(): String? = currentModelPath
    
    /**
     * Load a Whisper model from the given path
     * 
     * @param modelPath Absolute path to the Whisper GGUF model file
     * @return true if model loaded successfully, false otherwise
     */
    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if file exists
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file does not exist: $modelPath")
                return@withContext false
            }
            
            // Unload any existing model first
            if (whisperContextPtr != 0L) {
                Log.d(TAG, "Unloading existing model before loading new one")
                unloadModel()
            }
            
            Log.d(TAG, "Loading Whisper model from: $modelPath")
            
            // Initialize Whisper with the model
            val contextPtr = whisperInit(modelPath)
            
            if (contextPtr == 0L) {
                Log.e(TAG, "Failed to initialize Whisper model")
                return@withContext false
            }
            
            whisperContextPtr = contextPtr
            currentModelPath = modelPath
            
            Log.d(TAG, "Whisper model loaded successfully (context: $contextPtr)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading Whisper model: ${e.message}", e)
            false
        }
    }
    
    /**
     * Transcribe an audio file to text
     * 
     * @param audioPath Path to the audio file (WAV, MP3, M4A, etc.)
     * @param language Language code (e.g., "en", "auto" for auto-detect)
     * @param translate If true, translate to English
     * @return Transcription result or null on failure
     */
    suspend fun transcribe(
        audioPath: String,
        language: String = "auto",
        translate: Boolean = false
    ): TranscriptionResult? = withContext(Dispatchers.IO) {
        try {
            if (whisperContextPtr == 0L) {
                Log.e(TAG, "No model loaded. Call loadModel() first.")
                return@withContext null
            }
            
            // Check if audio file exists
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file does not exist: $audioPath")
                return@withContext null
            }
            
            Log.d(TAG, "Starting transcription of: $audioPath")
            Log.d(TAG, "Language: $language, Translate: $translate")
            
            val startTime = System.currentTimeMillis()
            
            // Load audio samples from file
            progressCallback?.invoke(0.1f)
            val samples = loadAudioSamples(audioPath)
            
            if (samples == null || samples.isEmpty()) {
                Log.e(TAG, "Failed to load audio samples from: $audioPath")
                return@withContext null
            }
            
            Log.d(TAG, "Loaded ${samples.size} audio samples (${samples.size / 16000.0}s @ 16kHz)")
            progressCallback?.invoke(0.2f)
            
            // Perform transcription
            val text = whisperTranscribe(
                contextPtr = whisperContextPtr,
                samples = samples,
                language = language,
                translate = translate
            )
            
            progressCallback?.invoke(1.0f)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            val audioDuration = samples.size / 16000.0 // Assuming 16kHz sample rate
            
            if (text == null) {
                Log.e(TAG, "Transcription returned null")
                return@withContext null
            }
            
            Log.d(TAG, "Transcription completed in ${elapsedTime}ms")
            Log.d(TAG, "Real-time factor: ${elapsedTime / 1000.0 / audioDuration}")
            
            TranscriptionResult(
                text = text.trim(),
                language = if (language == "auto") detectLanguage(text) else language,
                durationMs = elapsedTime,
                audioDurationSec = audioDuration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during transcription: ${e.message}", e)
            null
        }
    }
    
    /**
     * Transcribe audio from raw samples (already loaded)
     * 
     * @param samples Audio samples as float array (16kHz, mono)
     * @param language Language code
     * @param translate Whether to translate to English
     * @return Transcription result or null on failure
     */
    suspend fun transcribeFromSamples(
        samples: FloatArray,
        language: String = "auto",
        translate: Boolean = false
    ): TranscriptionResult? = withContext(Dispatchers.IO) {
        try {
            if (whisperContextPtr == 0L) {
                Log.e(TAG, "No model loaded. Call loadModel() first.")
                return@withContext null
            }
            
            if (samples.isEmpty()) {
                Log.e(TAG, "Empty audio samples provided")
                return@withContext null
            }
            
            Log.d(TAG, "Transcribing ${samples.size} samples")
            val startTime = System.currentTimeMillis()
            
            progressCallback?.invoke(0.2f)
            
            val text = whisperTranscribe(
                contextPtr = whisperContextPtr,
                samples = samples,
                language = language,
                translate = translate
            )
            
            progressCallback?.invoke(1.0f)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            val audioDuration = samples.size / 16000.0
            
            if (text == null) {
                Log.e(TAG, "Transcription returned null")
                return@withContext null
            }
            
            TranscriptionResult(
                text = text.trim(),
                language = if (language == "auto") detectLanguage(text) else language,
                durationMs = elapsedTime,
                audioDurationSec = audioDuration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during transcription: ${e.message}", e)
            null
        }
    }
    
    /**
     * Unload the current model and free resources
     */
    fun unloadModel() {
        if (whisperContextPtr != 0L) {
            Log.d(TAG, "Unloading Whisper model")
            try {
                whisperFree(whisperContextPtr)
            } catch (e: Exception) {
                Log.e(TAG, "Error freeing Whisper context: ${e.message}")
            }
            whisperContextPtr = 0
            currentModelPath = null
        }
    }
    
    /**
     * Get information about the loaded model
     */
    fun getModelInfo(): ModelInfo? {
        if (whisperContextPtr == 0L) return null
        
        return currentModelPath?.let { path ->
            val file = File(path)
            ModelInfo(
                path = path,
                name = file.nameWithoutExtension,
                sizeBytes = file.length()
            )
        }
    }
    
    /**
     * Simple language detection heuristic based on character patterns
     */
    private fun detectLanguage(text: String): String {
        return when {
            text.any { it in '\u4e00'..'\u9fff' } -> "zh" // Chinese
            text.any { it in '\u3040'..'\u30ff' } -> "ja" // Japanese
            text.any { it in '\uac00'..'\ud7af' } -> "ko" // Korean
            text.any { it in '\u0400'..'\u04ff' } -> "ru" // Russian/Cyrillic
            text.any { it in '\u0600'..'\u06ff' } -> "ar" // Arabic
            text.any { it in '\u0900'..'\u097f' } -> "hi" // Hindi
            else -> "en" // Default to English
        }
    }
    
    // ==================== Native Methods ====================
    
    /**
     * Initialize Whisper context with a model file
     * 
     * @param modelPath Path to the Whisper GGUF model
     * @return Pointer to the whisper context (0 on failure)
     */
    private external fun whisperInit(modelPath: String): Long
    
    /**
     * Run transcription on audio samples
     * 
     * @param contextPtr Pointer to the initialized whisper context
     * @param samples Audio samples as float array (16kHz, mono, normalized to [-1, 1])
     * @param language Language code ("auto", "en", "es", etc.)
     * @param translate Whether to translate to English
     * @return Transcribed text or null on failure
     */
    private external fun whisperTranscribe(
        contextPtr: Long,
        samples: FloatArray,
        language: String,
        translate: Boolean
    ): String?
    
    /**
     * Free the whisper context and release resources
     * 
     * @param contextPtr Pointer to the whisper context to free
     */
    private external fun whisperFree(contextPtr: Long)
    
    /**
     * Load audio file and convert to samples
     * Uses native code for efficient audio decoding
     * 
     * @param audioPath Path to the audio file
     * @return Float array of audio samples (16kHz, mono) or null on failure
     */
    private external fun loadAudioSamples(audioPath: String): FloatArray?
    
    // ==================== Data Classes ====================
    
    /**
     * Result of a transcription operation
     */
    data class TranscriptionResult(
        val text: String,
        val language: String,
        val durationMs: Long,
        val audioDurationSec: Double
    ) {
        /**
         * Real-time factor (RTF) - how fast transcription was relative to audio duration
         * RTF < 1.0 means faster than real-time
         */
        val realTimeFactor: Double
            get() = (durationMs / 1000.0) / audioDurationSec
        
        /**
         * Speed in audio seconds processed per second
         */
        val speed: Double
            get() = audioDurationSec / (durationMs / 1000.0)
    }
    
    /**
     * Information about a loaded model
     */
    data class ModelInfo(
        val path: String,
        val name: String,
        val sizeBytes: Long
    ) {
        val sizeMB: Double
            get() = sizeBytes / (1024.0 * 1024.0)
    }
}
