package com.localllm.app.inference

import android.content.Context
import android.util.Log
import com.localllm.app.data.model.ModelInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State of Whisper model loading.
 */
sealed class WhisperLoadingState {
    object Idle : WhisperLoadingState()
    data class Loading(val progress: Float = 0f) : WhisperLoadingState()
    object Loaded : WhisperLoadingState()
    data class Error(val message: String) : WhisperLoadingState()
}

/**
 * Result of audio transcription.
 */
sealed class TranscriptionResult {
    data class Success(val text: String, val duration: Float) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
    data class Progress(val progress: Float) : TranscriptionResult()
}

/**
 * Manages Whisper model loading and audio transcription.
 * Singleton manager that handles model lifecycle and transcription requests.
 */
@Singleton
class WhisperManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperAndroidProvider: WhisperAndroid
) {
    companion object {
        private const val TAG = "WhisperManager"
    }

    private val _loadingState = MutableStateFlow<WhisperLoadingState>(WhisperLoadingState.Idle)
    val loadingState: StateFlow<WhisperLoadingState> = _loadingState.asStateFlow()

    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel.asStateFlow()

    private var whisperAndroid: WhisperAndroid? = null
    private val loadMutex = Mutex()

    val currentModelId: String?
        get() = _currentModel.value?.id

    /**
     * Load a Whisper model for transcription.
     * 
     * @param model Model information
     * @return Result indicating success or failure
     */
    suspend fun loadModel(model: ModelInfo): Result<Unit> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            try {
                if (!model.isWhisper) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Model is not a Whisper model: ${model.id}")
                    )
                }

                if (model.localPath == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Model not downloaded: ${model.id}")
                    )
                }

                // Unload current model if different
                if (_currentModel.value?.id != model.id) {
                    unloadModel()
                }

                _loadingState.value = WhisperLoadingState.Loading(0.1f)
                Log.d(TAG, "Loading Whisper model: ${model.name}")

                // Initialize Whisper instance
                if (whisperAndroid == null) {
                    whisperAndroid = whisperAndroidProvider
                }

                _loadingState.value = WhisperLoadingState.Loading(0.3f)

                // Load model
                val success = whisperAndroid?.loadModel(model.localPath) ?: false
                
                if (!success) {
                    _loadingState.value = WhisperLoadingState.Error("Failed to load model")
                    return@withContext Result.failure(
                        Exception("Failed to load Whisper model")
                    )
                }

                _loadingState.value = WhisperLoadingState.Loading(1.0f)
                _currentModel.value = model
                _loadingState.value = WhisperLoadingState.Loaded

                Log.d(TAG, "Whisper model loaded: ${model.name}")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading Whisper model", e)
                _loadingState.value = WhisperLoadingState.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    /**
     * Transcribe audio file to text.
     * 
     * @param audioPath Path to audio file
     * @param language Language code or "auto"
     * @param translate Whether to translate to English
     * @param onProgress Progress callback
     * @return Transcription result
     */
    suspend fun transcribe(
        audioPath: String,
        language: String = "auto",
        translate: Boolean = false,
        onProgress: (suspend (Float) -> Unit)? = null
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            if (_loadingState.value !is WhisperLoadingState.Loaded) {
                return@withContext TranscriptionResult.Error("No Whisper model loaded")
            }

            if (whisperAndroid == null) {
                return@withContext TranscriptionResult.Error("Whisper not initialized")
            }

            Log.d(TAG, "Starting transcription: $audioPath")
            val startTime = System.currentTimeMillis()

            onProgress?.invoke(0.1f)

            // Set up progress callback
            whisperAndroid?.progressCallback = { progress ->
                kotlinx.coroutines.runBlocking {
                    onProgress?.invoke(progress)
                }
            }

            val result = whisperAndroid?.transcribe(
                audioPath = audioPath,
                language = language,
                translate = translate
            )

            if (result == null) {
                return@withContext TranscriptionResult.Error("Transcription failed")
            }

            val duration = (System.currentTimeMillis() - startTime) / 1000f
            Log.d(TAG, "Transcription completed in ${"%.1f".format(duration)}s")

            TranscriptionResult.Success(result.text, duration)

        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            TranscriptionResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Unload the current Whisper model.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            try {
                whisperAndroid?.unloadModel()
                whisperAndroid = null
                _currentModel.value = null
                _loadingState.value = WhisperLoadingState.Idle
                Log.d(TAG, "Whisper model unloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model", e)
            }
        }
    }

    /**
     * Check if a model is currently loaded.
     */
    fun isModelLoaded(): Boolean {
        return _loadingState.value is WhisperLoadingState.Loaded
    }

    /**
     * Get information about the Whisper instance.
     */
    fun getWhisperInfo(): WhisperAndroid.ModelInfo? {
        return whisperAndroid?.getModelInfo()
    }
}
