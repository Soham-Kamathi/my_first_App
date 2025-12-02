/**
 * JNI Bridge for llama.cpp integration with Android
 * 
 * This file provides the native interface between Kotlin/Java and llama.cpp.
 * It handles model loading, token generation, and memory management.
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <atomic>

// Conditionally include llama.cpp headers
#ifdef LLAMA_CPP_AVAILABLE
#include "llama.h"
#include "common.h"
#else
// Forward declarations for stub mode
struct llama_model;
struct llama_context;
#endif

#define LOG_TAG "LlamaAndroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global state management
static std::mutex g_mutex;
static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_should_cancel{false};

/**
 * Context wrapper that holds llama model and context together
 */
struct LlamaContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    int n_ctx = 2048;
    int n_threads = 4;
    bool use_mmap = true;
    
    ~LlamaContext() {
#ifdef LLAMA_CPP_AVAILABLE
        if (ctx) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model) {
            llama_free_model(model);
            model = nullptr;
        }
#endif
    }
};

extern "C" {

/**
 * Initialize the llama backend. Should be called once on app startup.
 */
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_initBackend(JNIEnv* env, jobject thiz) {
    LOGI("Initializing llama backend");
#ifdef LLAMA_CPP_AVAILABLE
    llama_backend_init();
#endif
}

/**
 * Free the llama backend. Should be called on app shutdown.
 */
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeBackend(JNIEnv* env, jobject thiz) {
    LOGI("Freeing llama backend");
#ifdef LLAMA_CPP_AVAILABLE
    llama_backend_free();
#endif
}

/**
 * Load a GGUF model from the specified path.
 * 
 * @param modelPath Path to the .gguf model file
 * @param threads Number of CPU threads to use
 * @param contextSize Size of the context window
 * @param useMmap Whether to use memory-mapped files
 * @param useNNAPI Whether to use Android NNAPI acceleration
 * @return Pointer to the loaded context, or 0 on failure
 */
JNIEXPORT jlong JNICALL
Java_com_localllm_app_inference_LlamaAndroid_loadModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath,
    jint threads,
    jint contextSize,
    jboolean useMmap,
    jboolean useNNAPI
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s with %d threads, context size: %d", path, threads, contextSize);
    
#ifdef LLAMA_CPP_AVAILABLE
    auto* wrapper = new LlamaContext();
    wrapper->n_ctx = contextSize;
    wrapper->n_threads = threads;
    wrapper->use_mmap = useMmap;
    
    // Model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = useMmap;
    model_params.use_mlock = false;
    
    // Load the model
    wrapper->model = llama_load_model_from_file(path, model_params);
    
    if (!wrapper->model) {
        LOGE("Failed to load model from: %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    
    // Context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;
    
    // Create context
    wrapper->ctx = llama_new_context_with_model(wrapper->model, ctx_params);
    
    if (!wrapper->ctx) {
        LOGE("Failed to create context for model");
        llama_free_model(wrapper->model);
        env->ReleaseStringUTFChars(modelPath, path);
        delete wrapper;
        return 0;
    }
    
    LOGI("Model loaded successfully");
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(wrapper);
#else
    // Stub implementation
    LOGI("Stub mode: Model loading simulated");
    env->ReleaseStringUTFChars(modelPath, path);
    auto* wrapper = new LlamaContext();
    wrapper->n_ctx = contextSize;
    wrapper->n_threads = threads;
    return reinterpret_cast<jlong>(wrapper);
#endif
}

/**
 * Free a loaded model and its context.
 * 
 * @param contextPtr Pointer to the LlamaContext
 */
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeModel(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    if (contextPtr == 0) {
        LOGE("Invalid context pointer");
        return;
    }
    
    LOGI("Freeing model context");
    auto* wrapper = reinterpret_cast<LlamaContext*>(contextPtr);
    delete wrapper;
}

/**
 * Generate tokens from a prompt.
 * 
 * @param contextPtr Pointer to the LlamaContext
 * @param prompt The input prompt string
 * @param maxTokens Maximum number of tokens to generate
 * @param temperature Sampling temperature (0.0-2.0)
 * @param topP Top-p sampling parameter
 * @param topK Top-k sampling parameter
 * @param repeatPenalty Repeat penalty parameter
 * @param callback Callback object for streaming tokens
 * @return The generated text
 */
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_generateTokens(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jfloat topP,
    jint topK,
    jfloat repeatPenalty,
    jobject callback
) {
    if (contextPtr == 0) {
        LOGE("Invalid context pointer");
        return env->NewStringUTF("");
    }
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(contextPtr);
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    
    LOGI("Generating tokens for prompt (length: %zu)", strlen(promptStr));
    
    g_is_generating = true;
    g_should_cancel = false;
    
    std::string result;
    
#ifdef LLAMA_CPP_AVAILABLE
    // Tokenize the prompt
    std::vector<llama_token> tokens = llama_tokenize(wrapper->ctx, promptStr, true);
    
    if (tokens.empty()) {
        LOGE("Failed to tokenize prompt");
        env->ReleaseStringUTFChars(prompt, promptStr);
        g_is_generating = false;
        return env->NewStringUTF("");
    }
    
    // Evaluate the prompt tokens
    if (llama_eval(wrapper->ctx, tokens.data(), tokens.size(), 0, wrapper->n_threads) != 0) {
        LOGE("Failed to evaluate prompt tokens");
        env->ReleaseStringUTFChars(prompt, promptStr);
        g_is_generating = false;
        return env->NewStringUTF("");
    }
    
    // Get callback method if provided
    jmethodID onTokenMethod = nullptr;
    if (callback != nullptr) {
        jclass callbackClass = env->GetObjectClass(callback);
        onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    }
    
    // Generate tokens
    int n_generated = 0;
    while (n_generated < maxTokens && !g_should_cancel) {
        // Sample the next token
        llama_token new_token = llama_sample_token(wrapper->ctx, nullptr, nullptr, temperature, topP, topK);
        
        // Check for end of generation
        if (new_token == llama_token_eos(wrapper->model)) {
            break;
        }
        
        // Convert token to text
        char buf[256];
        int n = llama_token_to_piece(wrapper->model, new_token, buf, sizeof(buf));
        if (n > 0) {
            std::string piece(buf, n);
            result += piece;
            
            // Call the callback if provided
            if (callback != nullptr && onTokenMethod != nullptr) {
                jstring tokenStr = env->NewStringUTF(piece.c_str());
                env->CallVoidMethod(callback, onTokenMethod, tokenStr);
                env->DeleteLocalRef(tokenStr);
            }
        }
        
        // Evaluate the new token
        if (llama_eval(wrapper->ctx, &new_token, 1, tokens.size() + n_generated, wrapper->n_threads) != 0) {
            LOGE("Failed to evaluate generated token");
            break;
        }
        
        n_generated++;
    }
    
    LOGI("Generated %d tokens", n_generated);
#else
    // Stub implementation for development
    result = "[Stub Mode] This is a simulated response. llama.cpp is not linked.";
    
    // Simulate token-by-token generation
    if (callback != nullptr) {
        jclass callbackClass = env->GetObjectClass(callback);
        jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
        
        std::string words[] = {"[Stub", " Mode]", " This", " is", " a", " simulated", " response."};
        for (const auto& word : words) {
            if (g_should_cancel) break;
            jstring tokenStr = env->NewStringUTF(word.c_str());
            env->CallVoidMethod(callback, onTokenMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }
    }
#endif
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    g_is_generating = false;
    
    return env->NewStringUTF(result.c_str());
}

/**
 * Cancel ongoing generation.
 */
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_cancelGeneration(
    JNIEnv* env,
    jobject thiz
) {
    LOGI("Cancelling generation");
    g_should_cancel = true;
}

/**
 * Check if generation is currently in progress.
 */
JNIEXPORT jboolean JNICALL
Java_com_localllm_app_inference_LlamaAndroid_isGenerating(
    JNIEnv* env,
    jobject thiz
) {
    return g_is_generating ? JNI_TRUE : JNI_FALSE;
}

/**
 * Get the context size of the loaded model.
 */
JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getContextSize(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    if (contextPtr == 0) {
        return 0;
    }
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(contextPtr);
    return wrapper->n_ctx;
}

/**
 * Get the vocabulary size of the loaded model.
 */
JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getVocabSize(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
#ifdef LLAMA_CPP_AVAILABLE
    if (contextPtr == 0) {
        return 0;
    }
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(contextPtr);
    return llama_n_vocab(wrapper->model);
#else
    return 32000; // Default vocab size for stub
#endif
}

/**
 * Clear the KV cache (for starting a new conversation).
 */
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_clearKVCache(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
#ifdef LLAMA_CPP_AVAILABLE
    if (contextPtr == 0) {
        return;
    }
    
    auto* wrapper = reinterpret_cast<LlamaContext*>(contextPtr);
    llama_kv_cache_clear(wrapper->ctx);
    LOGI("KV cache cleared");
#endif
}

/**
 * Get system information for debugging.
 */
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getSystemInfo(
    JNIEnv* env,
    jobject thiz
) {
#ifdef LLAMA_CPP_AVAILABLE
    std::string info = llama_print_system_info();
    return env->NewStringUTF(info.c_str());
#else
    return env->NewStringUTF("llama.cpp: Stub mode (native library not linked)");
#endif
}

} // extern "C"
