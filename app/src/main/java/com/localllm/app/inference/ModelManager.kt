package com.localllm.app.inference

import android.util.Log
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.data.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of LLM models including loading, unloading, and state tracking.
 */
@Singleton
class ModelManager @Inject constructor(
    private val llamaAndroid: LlamaAndroid
) {
    companion object {
        private const val TAG = "ModelManager"
    }

    private val mutex = Mutex()
    
    private var currentContextPtr: Long? = null
    private var _currentModelId: String? = null
    private var backendInitialized = false
    
    private val _loadingState = MutableStateFlow<ModelLoadingState>(ModelLoadingState.NotLoaded)
    val loadingState: StateFlow<ModelLoadingState> = _loadingState.asStateFlow()
    
    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel.asStateFlow()

    val isModelLoaded: Boolean
        get() = currentContextPtr != null && currentContextPtr != 0L

    val currentModelId: String?
        get() = _currentModelId

    /**
     * Initialize the llama backend. Should be called on app startup.
     */
    fun initBackend() {
        if (backendInitialized) return
        try {
            llamaAndroid.initBackend()
            backendInitialized = true
            Log.i(TAG, "Backend initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backend", e)
        }
    }

    /**
     * Free the llama backend. Should be called on app shutdown.
     */
    fun freeBackend() {
        if (!backendInitialized) return
        try {
            llamaAndroid.freeBackend()
            backendInitialized = false
            Log.i(TAG, "Backend freed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to free backend", e)
        }
    }

    /**
     * Load a model from the specified path.
     *
     * @param model The model info object
     * @param threads Number of CPU threads to use
     * @param contextSize Context window size
     * @param useMmap Whether to use memory-mapped files
     * @param useNNAPI Whether to use NNAPI acceleration
     * @param gpuLayers Number of layers to offload to GPU (0 = CPU only)
     * @return Result indicating success or failure
     */
    suspend fun loadModel(
        model: ModelInfo,
        threads: Int = Runtime.getRuntime().availableProcessors() - 1,
        contextSize: Int = 2048,
        useMmap: Boolean = true,
        useNNAPI: Boolean = false,
        gpuLayers: Int = 0
    ): Result<Unit> = mutex.withLock {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure backend is initialized
                if (!backendInitialized) {
                    Log.i(TAG, "Initializing backend before model load")
                    initBackend()
                }
                
                _loadingState.value = ModelLoadingState.Loading(0f)
                Log.i(TAG, "Loading model: ${model.name} from ${model.localPath}")
                Log.i(TAG, "GPU layers requested: $gpuLayers")
                
                // Unload existing model first
                unloadModelInternal()
                
                val modelPath = model.localPath
                    ?: return@withContext Result.failure(IllegalStateException("Model not downloaded"))
                
                // Verify the file exists
                val modelFile = java.io.File(modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file does not exist: $modelPath")
                    _loadingState.value = ModelLoadingState.Error("Model file not found")
                    return@withContext Result.failure(IllegalStateException("Model file not found: $modelPath"))
                }
                
                Log.i(TAG, "Model file exists, size: ${modelFile.length()} bytes")
                
                val adjustedThreads = threads.coerceIn(1, Runtime.getRuntime().availableProcessors())
                val adjustedContextSize = minOf(contextSize, model.contextLength).coerceAtLeast(512)
                
                Log.i(TAG, "Loading with threads=$adjustedThreads, contextSize=$adjustedContextSize, useMmap=$useMmap, gpuLayers=$gpuLayers")
                
                _loadingState.value = ModelLoadingState.Loading(0.3f)
                
                val contextPtr = llamaAndroid.loadModel(
                    modelPath = modelPath,
                    threads = adjustedThreads,
                    contextSize = adjustedContextSize,
                    useMmap = useMmap,
                    useNNAPI = useNNAPI,
                    gpuLayers = gpuLayers
                )
                
                Log.i(TAG, "loadModel returned contextPtr: $contextPtr")
                
                if (contextPtr == 0L) {
                    _loadingState.value = ModelLoadingState.Error("Failed to load model - native library returned 0")
                    Log.e(TAG, "Failed to load model: returned null context")
                    return@withContext Result.failure(RuntimeException("Failed to load model - check logs for details"))
                }
                
                currentContextPtr = contextPtr
                _currentModelId = model.id
                _currentModel.value = model
                _loadingState.value = ModelLoadingState.Loaded(model)
                
                Log.i(TAG, "Model loaded successfully: ${model.name}, contextPtr: $contextPtr")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                _loadingState.value = ModelLoadingState.Error(e.message ?: "Unknown error: ${e.javaClass.simpleName}")
                Result.failure(e)
            }
        }
    }

    /**
     * Unload the currently loaded model.
     */
    suspend fun unloadModel() = mutex.withLock {
        unloadModelInternal()
    }

    private fun unloadModelInternal() {
        currentContextPtr?.let { ptr ->
            if (ptr != 0L) {
                llamaAndroid.freeModel(ptr)
                Log.i(TAG, "Model unloaded")
            }
        }
        currentContextPtr = null
        _currentModelId = null
        _currentModel.value = null
        _loadingState.value = ModelLoadingState.NotLoaded
    }

    /**
     * Get the native context pointer for the loaded model.
     * Used by InferenceEngine for token generation.
     */
    fun getContextPtr(): Long? = currentContextPtr

    /**
     * Clear the KV cache for starting a new conversation.
     */
    fun clearKVCache() {
        currentContextPtr?.let { ptr ->
            if (ptr != 0L) {
                llamaAndroid.clearKVCache(ptr)
                Log.d(TAG, "KV cache cleared")
            }
        }
    }

    /**
     * Get context size of the loaded model.
     */
    fun getContextSize(): Int {
        return currentContextPtr?.let { ptr ->
            if (ptr != 0L) llamaAndroid.getContextSize(ptr) else 0
        } ?: 0
    }

    /**
     * Get system info for debugging.
     */
    fun getSystemInfo(): String = llamaAndroid.getSystemInfo()
}

/**
 * Represents the state of model loading.
 */
sealed class ModelLoadingState {
    data object NotLoaded : ModelLoadingState()
    data class Loading(val progress: Float) : ModelLoadingState()
    data class Loaded(val model: ModelInfo) : ModelLoadingState()
    data class Error(val message: String) : ModelLoadingState()
}
