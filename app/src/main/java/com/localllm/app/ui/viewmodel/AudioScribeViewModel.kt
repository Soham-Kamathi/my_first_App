package com.localllm.app.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.repository.ModelRepository
import com.localllm.app.inference.WhisperManager
import com.localllm.app.inference.WhisperLoadingState
import com.localllm.app.inference.TranscriptionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

enum class AudioState {
    IDLE,
    RECORDING,
    HAS_AUDIO,
    PROCESSING
}

/**
 * ViewModel for Audio Scribe Screen - Audio transcription
 */
@HiltViewModel
class AudioScribeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperManager: WhisperManager,
    private val modelRepository: ModelRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AudioScribeViewModel"
    }

    private val _audioState = MutableStateFlow(AudioState.IDLE)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _translateToEnglish = MutableStateFlow(false)
    val translateToEnglish: StateFlow<Boolean> = _translateToEnglish.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()
    
    private val _transcriptionProgress = MutableStateFlow(0f)
    val transcriptionProgress: StateFlow<Float> = _transcriptionProgress.asStateFlow()

    // Get downloaded Whisper models
    val downloadedWhisperModels: StateFlow<List<ModelInfo>> = modelRepository.getDownloadedModels()
        .map { models -> models.filter { it.isWhisper } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val currentWhisperModel: StateFlow<ModelInfo?> = whisperManager.currentModel
    val whisperLoadingState: StateFlow<WhisperLoadingState> = whisperManager.loadingState
    
    val isWhisperSupported: StateFlow<Boolean> = downloadedWhisperModels.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var durationJob: Job? = null

    init {
        // Auto-load first available Whisper model if not already loaded
        viewModelScope.launch {
            downloadedWhisperModels.collect { models ->
                if (models.isNotEmpty() && whisperManager.currentModel.value == null) {
                    // Auto-load the first (usually smallest) model
                    loadWhisperModel(models.first())
                }
            }
        }
    }

    fun setAudioPermission(granted: Boolean) {
        _hasAudioPermission.value = granted
        Log.d(TAG, "Audio permission: $granted")
    }

    fun setLanguage(code: String) {
        _selectedLanguage.value = code
    }

    fun setTranslateToEnglish(translate: Boolean) {
        _translateToEnglish.value = translate
    }

    fun startRecording() {
        if (_audioState.value != AudioState.IDLE) return

        try {
            audioFile = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            _audioState.value = AudioState.RECORDING
            _recordingDuration.value = 0

            // Start duration counter
            durationJob = viewModelScope.launch {
                while (isActive && _audioState.value == AudioState.RECORDING) {
                    delay(1000)
                    _recordingDuration.value++
                }
            }

            Log.d(TAG, "Recording started: ${audioFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _audioState.value = AudioState.IDLE
            releaseRecorder()
        }
    }

    fun stopRecording() {
        if (_audioState.value != AudioState.RECORDING) return

        try {
            durationJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _audioState.value = AudioState.HAS_AUDIO
            Log.d(TAG, "Recording stopped, duration: ${_recordingDuration.value}s")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _audioState.value = AudioState.IDLE
            releaseRecorder()
        }
    }

    fun loadAudioFile(uri: Uri) {
        viewModelScope.launch {
            try {
                // Copy audio file to cache
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "audio_import_${System.currentTimeMillis()}.wav")

                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                audioFile = tempFile
                _audioState.value = AudioState.HAS_AUDIO
                Log.d(TAG, "Audio file loaded: ${tempFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load audio file", e)
            }
        }
    }

    fun clearAudio() {
        audioFile?.delete()
        audioFile = null
        _audioState.value = AudioState.IDLE
        _recordingDuration.value = 0
    }

    fun clearTranscription() {
        _transcription.value = ""
    }

    fun clearAll() {
        clearAudio()
        clearTranscription()
    }

    /**
     * Load a Whisper model for transcription.
     */
    fun loadWhisperModel(model: ModelInfo) {
        if (!model.isWhisper) {
            Log.w(TAG, "Attempted to load non-Whisper model: ${model.id}")
            return
        }
        
        viewModelScope.launch {
            whisperManager.loadModel(model).fold(
                onSuccess = {
                    Log.d(TAG, "Whisper model loaded: ${model.name}")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load Whisper model", error)
                    _transcription.value = "Error loading model: ${error.message}"
                }
            )
        }
    }
    
    /**
     * Transcribe audio using Whisper.
     */
    fun transcribe() {
        if (_audioState.value != AudioState.HAS_AUDIO || audioFile == null) return

        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _audioState.value = AudioState.PROCESSING
                _transcriptionProgress.value = 0f

                Log.d(TAG, "Starting transcription for: ${audioFile?.absolutePath}")

                // Check if Whisper model is loaded
                if (whisperManager.currentModel.value == null) {
                    // Try to load first available model
                    val models = downloadedWhisperModels.value
                    if (models.isEmpty()) {
                        _transcription.value = "⚠️ No Whisper model downloaded.\n\n" +
                                "Please download a Whisper model from the Model Library first."
                        _audioState.value = AudioState.HAS_AUDIO
                        _isProcessing.value = false
                        return@launch
                    }
                    
                    // Load first model
                    Log.d(TAG, "Auto-loading Whisper model: ${models.first().name}")
                    whisperManager.loadModel(models.first()).fold(
                        onSuccess = { /* Continue */ },
                        onFailure = { error ->
                            _transcription.value = "Error loading Whisper model: ${error.message}"
                            _audioState.value = AudioState.HAS_AUDIO
                            _isProcessing.value = false
                            return@launch
                        }
                    )
                }

                // Perform transcription
                val result = whisperManager.transcribe(
                    audioPath = audioFile!!.absolutePath,
                    language = _selectedLanguage.value,
                    translate = _translateToEnglish.value
                ) { progress ->
                    _transcriptionProgress.value = progress
                }

                when (result) {
                    is TranscriptionResult.Success -> {
                        _transcription.value = result.text
                        Log.d(TAG, "Transcription completed in ${result.duration}s")
                    }
                    is TranscriptionResult.Error -> {
                        _transcription.value = "❌ Transcription Error\n\n${result.message}"
                        Log.e(TAG, "Transcription error: ${result.message}")
                    }
                    is TranscriptionResult.Progress -> {
                        // Progress updates handled via callback
                    }
                }

                _audioState.value = AudioState.HAS_AUDIO

            } catch (e: Exception) {
                Log.e(TAG, "Transcription error", e)
                _transcription.value = "❌ Error: ${e.message}"
                _audioState.value = AudioState.HAS_AUDIO
            } finally {
                _isProcessing.value = false
                _transcriptionProgress.value = 0f
            }
        }
    }

    fun copyTranscription() {
        if (_transcription.value.isEmpty()) return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Transcription", _transcription.value)
        clipboard.setPrimaryClip(clip)
        Log.d(TAG, "Transcription copied to clipboard")
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }
        mediaRecorder = null
    }

    override fun onCleared() {
        super.onCleared()
        durationJob?.cancel()
        releaseRecorder()
        // Clean up temp files
        audioFile?.delete()
    }
}
