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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    @ApplicationContext private val context: Context
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

    private val _isWhisperSupported = MutableStateFlow(false)
    val isWhisperSupported: StateFlow<Boolean> = _isWhisperSupported.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var durationJob: Job? = null

    init {
        // Check if whisper is supported (placeholder - will be true when implemented)
        checkWhisperSupport()
    }

    private fun checkWhisperSupport() {
        // TODO: Check if whisper.cpp is available and model is loaded
        _isWhisperSupported.value = false
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

    fun transcribe() {
        if (_audioState.value != AudioState.HAS_AUDIO || audioFile == null) return

        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _audioState.value = AudioState.PROCESSING

                Log.d(TAG, "Starting transcription for: ${audioFile?.absolutePath}")

                // TODO: Implement actual whisper.cpp transcription
                // This will require:
                // 1. Loading whisper.cpp native library
                // 2. Loading Whisper GGML model
                // 3. Processing audio (resampling to 16kHz if needed)
                // 4. Running whisper inference

                // For now, show placeholder
                delay(1000) // Simulate processing

                _transcription.value = "🎙️ Whisper transcription is not yet implemented.\n\n" +
                        "To enable this feature, we need to:\n" +
                        "• Integrate whisper.cpp native library\n" +
                        "• Add Whisper GGML model support\n" +
                        "• Implement audio preprocessing\n\n" +
                        "Audio file: ${audioFile?.name}\n" +
                        "Language: ${_selectedLanguage.value}\n" +
                        "Translate: ${_translateToEnglish.value}\n\n" +
                        "Stay tuned for future updates!"

                _audioState.value = AudioState.HAS_AUDIO

            } catch (e: Exception) {
                Log.e(TAG, "Transcription error", e)
                _transcription.value = "Error: ${e.message}"
                _audioState.value = AudioState.HAS_AUDIO
            } finally {
                _isProcessing.value = false
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
