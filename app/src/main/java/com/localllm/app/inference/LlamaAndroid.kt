package com.localllm.app.inference

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin JNI wrapper for llama.cpp native library.
 * Provides the interface between Kotlin code and native C++ implementation.
 */
@Singleton
class LlamaAndroid @Inject constructor() {

    private var stubMode = false
    private var isGeneratingFlag = false
    private var cancelFlag = false
    
    // Store model and context pointers
    private var modelPtr: Long = 0
    private var contextPtr: Long = 0

    /**
     * Callback interface for streaming token generation.
     */
    interface TokenCallback {
        fun onToken(token: String)
    }

    companion object {
        private const val TAG = "LlamaAndroid"
        private var nativeLoaded = false
        
        init {
            try {
                System.loadLibrary("localllm")
                nativeLoaded = true
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native library not loaded - running in stub mode: ${e.message}")
                nativeLoaded = false
            }
        }
    }
    
    init {
        stubMode = !nativeLoaded
    }

    /**
     * Initialize the llama backend. Call once on app startup.
     */
    fun initBackend() {
        if (stubMode) {
            Log.d(TAG, "[STUB] initBackend called")
            return
        }
        try {
            Log.i(TAG, "Calling initBackendNative...")
            initBackendNative()
            Log.i(TAG, "initBackendNative completed successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "initBackendNative not found - switching to stub mode", e)
            stubMode = true
        } catch (e: Exception) {
            Log.e(TAG, "Exception in initBackendNative", e)
        }
    }
    
    private external fun initBackendNative(): Unit

    /**
     * Free the llama backend. Call on app shutdown.
     */
    fun freeBackend() {
        if (stubMode) {
            Log.d(TAG, "[STUB] freeBackend called")
            return
        }
        freeBackendNative()
    }
    
    private external fun freeBackendNative(): Unit

    /**
     * Load a GGUF model from the specified path.
     * Returns context pointer on success, 0 on failure.
     * 
     * @param modelPath Path to the GGUF model file
     * @param threads Number of CPU threads to use
     * @param contextSize Maximum context size in tokens
     * @param useMmap Use memory-mapped file loading (recommended)
     * @param useNNAPI Legacy parameter (unused, use gpuLayers instead)
     * @param gpuLayers Number of model layers to offload to GPU (0 = CPU only)
     */
    fun loadModel(
        modelPath: String,
        threads: Int = 4,
        contextSize: Int = 2048,
        useMmap: Boolean = true,
        useNNAPI: Boolean = false,
        gpuLayers: Int = 0
    ): Long {
        if (stubMode) {
            Log.d(TAG, "[STUB] loadModel called with path: $modelPath, gpuLayers: $gpuLayers")
            modelPtr = 12345L
            contextPtr = 67890L
            return contextPtr
        }
        
        return try {
            Log.i(TAG, "Loading model from: $modelPath")
            Log.i(TAG, "Parameters: threads=$threads, contextSize=$contextSize, useMmap=$useMmap, gpuLayers=$gpuLayers")
            
            // Load model with GPU layer configuration
            modelPtr = loadModelNative(modelPath, contextSize, threads, useMmap, false, gpuLayers)
            Log.i(TAG, "loadModelNative returned: $modelPtr")
            
            if (modelPtr == 0L) {
                Log.e(TAG, "Failed to load model from: $modelPath")
                return 0
            }
            
            // Create context
            Log.i(TAG, "Creating context...")
            contextPtr = createContextNative(modelPtr, contextSize, 512, threads)
            Log.i(TAG, "createContextNative returned: $contextPtr")
            
            if (contextPtr == 0L) {
                Log.e(TAG, "Failed to create context")
                freeModelNative(modelPtr)
                modelPtr = 0
                return 0
            }
            
            Log.i(TAG, "Model loaded successfully, contextPtr: $contextPtr")
            contextPtr
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found - JNI binding error", e)
            0L
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading model", e)
            0L
        }
    }
    
    private external fun loadModelNative(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        useMmap: Boolean,
        useMlock: Boolean,
        nGpuLayers: Int
    ): Long
    
    private external fun createContextNative(
        modelPtr: Long,
        nCtx: Int,
        nBatch: Int,
        nThreads: Int
    ): Long

    /**
     * Free a loaded model and its context.
     */
    fun freeModel(ctxPtr: Long) {
        if (stubMode) {
            Log.d(TAG, "[STUB] freeModel called")
            modelPtr = 0
            contextPtr = 0
            return
        }
        
        if (contextPtr != 0L) {
            freeContextNative(contextPtr)
            contextPtr = 0
        }
        if (modelPtr != 0L) {
            freeModelNative(modelPtr)
            modelPtr = 0
        }
    }
    
    private external fun freeModelNative(modelPtr: Long): Unit
    private external fun freeContextNative(ctxPtr: Long): Unit

    /**
     * Generate tokens from a prompt with streaming callback support.
     */
    fun generateTokens(
        ctxPtr: Long,
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        repeatPenalty: Float = 1.1f,
        callback: TokenCallback? = null
    ): String {
        if (stubMode) {
            Log.d(TAG, "[STUB] generateTokens called with prompt: ${prompt.take(50)}...")
            isGeneratingFlag = true
            cancelFlag = false
            
            // Simulate streaming response
            val stubResponse = "This is a **stub response** for testing purposes. " +
                "The actual LLM inference requires the native llama.cpp library to be compiled and loaded. " +
                "Once you set up the NDK and compile the native code, you'll get real AI responses here!\n\n" +
                "Your prompt was: \"${prompt.take(100)}${if (prompt.length > 100) "..." else ""}\""
            
            val words = stubResponse.split(" ")
            val result = StringBuilder()
            
            for (word in words) {
                if (cancelFlag) break
                val token = if (result.isEmpty()) word else " $word"
                result.append(token)
                callback?.onToken(token)
                Thread.sleep(50) // Simulate token generation delay
            }
            
            isGeneratingFlag = false
            return result.toString()
        }
        
        return generateNative(
            contextPtr, modelPtr, prompt, maxTokens,
            temperature, topP, topK, repeatPenalty, callback
        )
    }
    
    private external fun generateNative(
        ctxPtr: Long,
        modelPtr: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
        callback: TokenCallback?
    ): String

    /**
     * Cancel ongoing token generation.
     */
    fun cancelGeneration() {
        if (stubMode) {
            Log.d(TAG, "[STUB] cancelGeneration called")
            cancelFlag = true
            return
        }
        cancelGenerationNative()
    }
    
    private external fun cancelGenerationNative(): Unit

    /**
     * Check if generation is currently in progress.
     */
    fun isGenerating(): Boolean {
        if (stubMode) {
            return isGeneratingFlag
        }
        return isGeneratingNative()
    }
    
    private external fun isGeneratingNative(): Boolean

    /**
     * Get the context size of the loaded model.
     */
    fun getContextSize(ctxPtr: Long): Int {
        if (stubMode) {
            return 2048
        }
        return getContextSizeNative(ctxPtr)
    }
    
    private external fun getContextSizeNative(ctxPtr: Long): Int

    /**
     * Get the vocabulary size of the loaded model.
     */
    fun getVocabSize(ctxPtr: Long): Int {
        if (stubMode) {
            return 32000
        }
        // Not implemented in native, return default
        return 32000
    }

    /**
     * Clear the KV cache.
     */
    fun clearKVCache(ctxPtr: Long) {
        if (stubMode) {
            Log.d(TAG, "[STUB] clearKVCache called")
            return
        }
        clearKVCacheNative(ctxPtr)
    }
    
    private external fun clearKVCacheNative(ctxPtr: Long): Unit

    /**
     * Get system information for debugging.
     */
    fun getSystemInfo(): String {
        if (stubMode) {
            return "STUB MODE - Native library not loaded"
        }
        return getSystemInfoNative()
    }
    
    private external fun getSystemInfoNative(): String
    
    /**
     * Get model info as JSON string.
     */
    fun getModelInfo(): String {
        if (stubMode) {
            return "{\"description\":\"Stub Model\",\"n_params\":0,\"size\":0}"
        }
        if (modelPtr == 0L) return "{}"
        return getModelInfoNative(modelPtr)
    }
    
    private external fun getModelInfoNative(modelPtr: Long): String
    
    /**
     * Tokenize a string into token IDs.
     */
    fun tokenize(text: String, addSpecial: Boolean = true): IntArray? {
        if (stubMode) {
            // Return dummy tokens
            return text.split(" ").indices.map { it }.toIntArray()
        }
        if (modelPtr == 0L) return null
        return tokenizeNative(modelPtr, text, addSpecial, true)
    }
    
    private external fun tokenizeNative(
        modelPtr: Long,
        text: String,
        addSpecial: Boolean,
        parseSpecial: Boolean
    ): IntArray?
    
    /**
     * Detokenize token IDs back to string.
     */
    fun detokenize(tokens: IntArray): String {
        if (stubMode) {
            return "Stub detokenization"
        }
        if (modelPtr == 0L) return ""
        return detokenizeNative(modelPtr, tokens) ?: ""
    }
    
    private external fun detokenizeNative(modelPtr: Long, tokens: IntArray): String?
    
    /**
     * Get optimal thread count for this device.
     */
    fun getThreadCount(): Int {
        if (stubMode) {
            return Runtime.getRuntime().availableProcessors()
        }
        return getThreadCountNative()
    }
    
    private external fun getThreadCountNative(): Int
}
