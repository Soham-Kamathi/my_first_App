/**
 * llama_jni.cpp - JNI bridge for llama.cpp on Android
 * 
 * This file provides the native interface between Kotlin/Java and llama.cpp
 * 
 * Based on the official llama.cpp Android example:
 * https://github.com/ggml-org/llama.cpp/tree/main/examples/llama.android
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstdlib>
#include <cstring>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>

#include "llama.h"

#ifdef GGML_USE_VULKAN
#include "ggml-vulkan.h"
#endif

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Batch helper functions - matching official llama.cpp common.h implementation
// ============================================================================

// Clear a batch for reuse (just reset n_tokens counter)
static void batch_clear(struct llama_batch & batch) {
    batch.n_tokens = 0;
}

// Add a token to the batch - matches common_batch_add from common.h
static void batch_add(
        struct llama_batch & batch,
        llama_token id,
        llama_pos pos,
        llama_seq_id seq_id,
        bool logits) {
    batch.token[batch.n_tokens] = id;
    batch.pos[batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = 1;
    batch.seq_id[batch.n_tokens][0] = seq_id;
    batch.logits[batch.n_tokens] = logits ? 1 : 0;
    batch.n_tokens++;
}

// Global state
static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_cancel_requested{false};
static std::mutex g_mutex;

// Helper to convert jstring to std::string
static std::string jstring_to_string(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) return "";
    const char *chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// Helper to create jstring from std::string
static jstring string_to_jstring(JNIEnv *env, const std::string &str) {
    return env->NewStringUTF(str.c_str());
}

extern "C" {

// Initialize the llama backend
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_initBackendNative(JNIEnv *env, jobject thiz) {
    LOGI("Initializing llama backend");
    try {
        llama_backend_init();
        LOGI("llama backend initialized successfully");
    } catch (const std::exception& e) {
        LOGE("Exception initializing backend: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception initializing backend");
    }
}

// Free the llama backend
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeBackendNative(JNIEnv *env, jobject thiz) {
    LOGI("Freeing llama backend");
    try {
        llama_backend_free();
        LOGI("llama backend freed");
    } catch (...) {
        LOGE("Exception freeing backend");
    }
}

// Load a model from file with GPU acceleration support
JNIEXPORT jlong JNICALL
Java_com_localllm_app_inference_LlamaAndroid_loadModelNative(
        JNIEnv *env,
        jobject thiz,
        jstring model_path,
        jint n_ctx,
        jint n_threads,
        jboolean use_mmap,
        jboolean use_mlock,
        jint n_gpu_layers) {
    
    std::string path = jstring_to_string(env, model_path);
    LOGI("Loading model from: %s", path.c_str());
    LOGI("GPU layers requested: %d", n_gpu_layers);
    
    try {
        // Initialize model parameters
        llama_model_params model_params = llama_model_default_params();
        model_params.use_mmap = use_mmap;
        model_params.use_mlock = use_mlock;
        
        // GPU acceleration: set number of layers to offload to GPU
        // This requires Vulkan backend to be compiled in
        model_params.n_gpu_layers = n_gpu_layers;
        
        LOGI("Model params: mmap=%d, mlock=%d, gpu_layers=%d", 
             model_params.use_mmap, model_params.use_mlock, model_params.n_gpu_layers);
        
        // Load the model
        llama_model *model = llama_model_load_from_file(path.c_str(), model_params);
        if (model == nullptr) {
            LOGE("Failed to load model from: %s", path.c_str());
            return 0;
        }
        
        LOGI("Model loaded successfully with %d GPU layers, ptr: %p", n_gpu_layers, model);
        return reinterpret_cast<jlong>(model);
    } catch (const std::exception& e) {
        LOGE("Exception loading model: %s", e.what());
        return 0;
    } catch (...) {
        LOGE("Unknown exception loading model");
        return 0;
    }
}

// Free a loaded model
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeModelNative(JNIEnv *env, jobject thiz, jlong model_ptr) {
    if (model_ptr == 0) return;
    
    try {
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        LOGI("Freeing model: %p", model);
        llama_model_free(model);
        LOGI("Model freed");
    } catch (...) {
        LOGE("Exception freeing model");
    }
}

// Create a context for inference
JNIEXPORT jlong JNICALL
Java_com_localllm_app_inference_LlamaAndroid_createContextNative(
        JNIEnv *env,
        jobject thiz,
        jlong model_ptr,
        jint n_ctx,
        jint n_batch,
        jint n_threads) {
    
    if (model_ptr == 0) {
        LOGE("Cannot create context: model is null");
        return 0;
    }
    
    try {
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        
        // Initialize context parameters
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = n_ctx > 0 ? n_ctx : 2048;
        ctx_params.n_batch = n_batch > 0 ? n_batch : 512;
        ctx_params.n_threads = n_threads > 0 ? n_threads : std::thread::hardware_concurrency();
        ctx_params.n_threads_batch = ctx_params.n_threads;
        
        LOGI("Creating context with n_ctx=%d, n_batch=%d, n_threads=%d",
             ctx_params.n_ctx, ctx_params.n_batch, ctx_params.n_threads);
        
        llama_context *ctx = llama_init_from_model(model, ctx_params);
        if (ctx == nullptr) {
            LOGE("Failed to create context");
            return 0;
        }
        
        LOGI("Context created successfully, ptr: %p", ctx);
        return reinterpret_cast<jlong>(ctx);
    } catch (const std::exception& e) {
        LOGE("Exception creating context: %s", e.what());
        return 0;
    } catch (...) {
        LOGE("Unknown exception creating context");
        return 0;
    }
}

// Free a context
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeContextNative(JNIEnv *env, jobject thiz, jlong ctx_ptr) {
    if (ctx_ptr == 0) return;
    
    try {
        llama_context *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
        LOGI("Freeing context: %p", ctx);
        llama_free(ctx);
        LOGI("Context freed");
    } catch (...) {
        LOGE("Exception freeing context");
    }
}

// Clear the KV cache
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_clearKVCacheNative(JNIEnv *env, jobject thiz, jlong ctx_ptr) {
    if (ctx_ptr == 0) return;
    
    llama_context *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    llama_memory_t mem = llama_get_memory(ctx);
    if (mem) {
        llama_memory_clear(mem, true);
    }
    LOGD("KV cache cleared");
}

// Tokenize a string
JNIEXPORT jintArray JNICALL
Java_com_localllm_app_inference_LlamaAndroid_tokenizeNative(
        JNIEnv *env,
        jobject thiz,
        jlong model_ptr,
        jstring text,
        jboolean add_special,
        jboolean parse_special) {
    
    if (model_ptr == 0) {
        LOGE("Cannot tokenize: model is null");
        return nullptr;
    }
    
    try {
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        std::string str = jstring_to_string(env, text);
        
        // Get vocab for tokenization
        const llama_vocab *vocab = llama_model_get_vocab(model);
        
        // Estimate token count (usually ~4 chars per token)
        int max_tokens = str.length() / 2 + 128;
        std::vector<llama_token> tokens(max_tokens);
        
        int n_tokens = llama_tokenize(vocab, str.c_str(), str.length(),
                                       tokens.data(), max_tokens,
                                       add_special, parse_special);
        
        if (n_tokens < 0) {
            // Need more space
            tokens.resize(-n_tokens);
            n_tokens = llama_tokenize(vocab, str.c_str(), str.length(),
                                       tokens.data(), -n_tokens,
                                       add_special, parse_special);
        }
        
        if (n_tokens < 0) {
            LOGE("Failed to tokenize string");
            return nullptr;
        }
        
        tokens.resize(n_tokens);
        
        // Convert to Java array
        jintArray result = env->NewIntArray(n_tokens);
        env->SetIntArrayRegion(result, 0, n_tokens, reinterpret_cast<jint *>(tokens.data()));
        
        LOGD("Tokenized %zu chars into %d tokens", str.length(), n_tokens);
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception tokenizing: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Unknown exception tokenizing");
        return nullptr;
    }
}

// Detokenize tokens to string
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_detokenizeNative(
        JNIEnv *env,
        jobject thiz,
        jlong model_ptr,
        jintArray tokens) {
    
    if (model_ptr == 0 || tokens == nullptr) {
        return string_to_jstring(env, "");
    }
    
    try {
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        const llama_vocab *vocab = llama_model_get_vocab(model);
        
        jsize n_tokens = env->GetArrayLength(tokens);
        jint *token_data = env->GetIntArrayElements(tokens, nullptr);
        
        std::string result;
        char buf[256];
        
        for (int i = 0; i < n_tokens; i++) {
            int len = llama_token_to_piece(vocab, token_data[i], buf, sizeof(buf), 0, false);
            if (len > 0) {
                result.append(buf, len);
            }
        }
        
        env->ReleaseIntArrayElements(tokens, token_data, 0);
        return string_to_jstring(env, result);
    } catch (const std::exception& e) {
        LOGE("Exception detokenizing: %s", e.what());
        return string_to_jstring(env, "");
    } catch (...) {
        LOGE("Unknown exception detokenizing");
        return string_to_jstring(env, "");
    }
}

// Generate tokens with streaming callback
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_generateNative(
        JNIEnv *env,
        jobject thiz,
        jlong ctx_ptr,
        jlong model_ptr,
        jstring prompt,
        jint max_tokens,
        jfloat temperature,
        jfloat top_p,
        jint top_k,
        jfloat repeat_penalty,
        jobject callback) {
    
    LOGI("generateNative called: ctx_ptr=%ld, model_ptr=%ld", (long)ctx_ptr, (long)model_ptr);
    
    if (ctx_ptr == 0 || model_ptr == 0) {
        LOGE("Cannot generate: context or model is null (ctx=%ld, model=%ld)", 
             (long)ctx_ptr, (long)model_ptr);
        return string_to_jstring(env, "Error: Model not loaded properly");
    }
    
    // Check if already generating
    bool expected = false;
    if (!g_is_generating.compare_exchange_strong(expected, true)) {
        LOGW("Generation already in progress");
        return string_to_jstring(env, "Error: Generation already in progress");
    }
    
    g_cancel_requested.store(false);
    std::string result;
    llama_sampler *sampler = nullptr;
    llama_batch batch = {0, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    bool batch_allocated = false;
    
    try {
        llama_context *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        
        if (ctx == nullptr || model == nullptr) {
            LOGE("Context or model pointer is invalid");
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Invalid context or model");
        }
        
        const llama_vocab *vocab = llama_model_get_vocab(model);
        if (vocab == nullptr) {
            LOGE("Failed to get vocab from model");
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Failed to get vocabulary");
        }
        
        std::string prompt_str = jstring_to_string(env, prompt);
        LOGI("Starting generation, prompt length: %zu chars", prompt_str.length());
        
        if (prompt_str.empty()) {
            LOGE("Empty prompt provided");
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Empty prompt");
        }
        
        // Get callback method if provided
        jmethodID callback_method = nullptr;
        jclass callback_class = nullptr;
        if (callback != nullptr) {
            callback_class = env->GetObjectClass(callback);
            if (callback_class != nullptr) {
                callback_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
                if (callback_method == nullptr) {
                    LOGW("Could not find onToken method, proceeding without callback");
                }
            }
        }
        
        // Tokenize the prompt
        int max_prompt_tokens = prompt_str.length() + 256;
        std::vector<llama_token> prompt_tokens(max_prompt_tokens);
        
        LOGI("Tokenizing prompt...");
        int n_prompt_tokens = llama_tokenize(vocab, prompt_str.c_str(), prompt_str.length(),
                                              prompt_tokens.data(), max_prompt_tokens,
                                              true, true);
        
        if (n_prompt_tokens < 0) {
            LOGI("Need more space for tokens: %d", -n_prompt_tokens);
            prompt_tokens.resize(-n_prompt_tokens + 100);
            n_prompt_tokens = llama_tokenize(vocab, prompt_str.c_str(), prompt_str.length(),
                                              prompt_tokens.data(), prompt_tokens.size(),
                                              true, true);
        }
        
        if (n_prompt_tokens < 0) {
            LOGE("Failed to tokenize prompt, error code: %d", n_prompt_tokens);
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Failed to tokenize prompt");
        }
        
        if (n_prompt_tokens == 0) {
            LOGE("Tokenization returned 0 tokens");
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Prompt tokenized to zero tokens");
        }
        
        prompt_tokens.resize(n_prompt_tokens);
        LOGI("Prompt tokenized to %d tokens", n_prompt_tokens);
        
        // Clear KV cache completely for fresh generation
        LOGI("Clearing KV cache...");
        llama_memory_t mem = llama_get_memory(ctx);
        if (mem) {
            llama_memory_clear(mem, false);  // false = keep structure, just clear data
            LOGI("KV cache cleared");
        } else {
            LOGW("Could not get memory handle - proceeding without cache clear");
        }
        
        // Get context size and batch size
        int n_ctx = llama_n_ctx(ctx);
        int n_batch = llama_n_batch(ctx);
        if (n_batch <= 0) n_batch = 512;
        
        LOGI("Context size: %d, Batch size: %d", n_ctx, n_batch);

        if (n_prompt_tokens >= n_ctx) {
            LOGE("Prompt (%d tokens) exceeds context size (%d)", n_prompt_tokens, n_ctx);
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Prompt too long for context");
        }

        // Allocate batch using official llama_batch_init API
        // Allocate enough for at least the batch size
        int batch_size = std::max(n_batch, n_prompt_tokens);
        LOGI("Allocating batch with size %d using llama_batch_init", batch_size);
        batch = llama_batch_init(batch_size, 0, 1);  // n_tokens, embd=0, n_seq_max=1
        batch_allocated = true;
        
        if (batch.token == nullptr) {
            LOGE("Failed to allocate batch");
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Failed to allocate batch");
        }
        LOGI("Batch allocated successfully");

        // Process prompt in chunks
        LOGI("Processing prompt in batches...");
        int n_cur = 0;  // Current position in KV cache
        
        for (int i = 0; i < n_prompt_tokens; i += n_batch) {
            int n_eval = std::min(n_batch, n_prompt_tokens - i);
            
            // Clear and fill batch
            batch_clear(batch);
            for (int j = 0; j < n_eval; j++) {
                // logits = true only for last token of the prompt
                bool is_last = (i + j == n_prompt_tokens - 1);
                batch_add(batch, prompt_tokens[i + j], n_cur + j, 0, is_last);
            }
            
            LOGI("Processing prompt tokens %d to %d (batch of %d)", i, i + n_eval - 1, n_eval);
            
            int ret = llama_decode(ctx, batch);
            if (ret != 0) {
                LOGE("llama_decode failed during prompt processing at pos %d, error: %d", i, ret);
                llama_batch_free(batch);
                g_is_generating.store(false);
                return string_to_jstring(env, "Error: Failed to process prompt");
            }
            
            n_cur += n_eval;
        }
        
        LOGI("Prompt processing complete, n_cur=%d", n_cur);
        
        // Initialize sampler
        LOGI("Initializing sampler with temp=%.2f, top_p=%.2f, top_k=%d", 
             temperature, top_p, top_k);
        sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        if (sampler == nullptr) {
            LOGE("Failed to create sampler");
            llama_batch_free(batch);
            g_is_generating.store(false);
            return string_to_jstring(env, "Error: Failed to create sampler");
        }
        
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(top_k > 0 ? top_k : 40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(top_p > 0 ? top_p : 0.95f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature > 0 ? temperature : 0.8f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        
        // Generate tokens
        char token_buf[256];
        
        LOGI("Starting token generation, max_tokens=%d", max_tokens);
        
        for (int i = 0; i < max_tokens; i++) {
            // Check for cancellation
            if (g_cancel_requested.load()) {
                LOGI("Generation cancelled by user at token %d", i);
                break;
            }
            
            // Sample next token
            llama_token new_token = llama_sampler_sample(sampler, ctx, -1);
            
            // Check for end of generation
            if (llama_vocab_is_eog(vocab, new_token)) {
                LOGI("End of generation token received at token %d", i);
                break;
            }
            
            // Convert token to string
            int token_len = llama_token_to_piece(vocab, new_token, token_buf, sizeof(token_buf) - 1, 0, true);
            if (token_len > 0) {
                token_buf[token_len] = '\0';
                std::string token_str(token_buf, token_len);
                result += token_str;
                
                // Call callback if provided
                if (callback_method != nullptr && callback != nullptr) {
                    jstring jtoken = string_to_jstring(env, token_str);
                    if (jtoken != nullptr) {
                        env->CallVoidMethod(callback, callback_method, jtoken);
                        if (env->ExceptionCheck()) {
                            LOGW("Exception in callback, clearing and continuing");
                            env->ExceptionClear();
                        }
                        env->DeleteLocalRef(jtoken);
                    }
                }
                
                if (i % 50 == 0) {
                    LOGD("Generated %d tokens so far", i + 1);
                }
            }
            
            // Check context limit
            if (n_cur >= n_ctx - 1) {
                LOGW("Reached context limit at token %d", i);
                break;
            }
            
            // Prepare batch for next token decode
            batch_clear(batch);
            batch_add(batch, new_token, n_cur, 0, true);
            n_cur++;
            
            int decode_result = llama_decode(ctx, batch);
            if (decode_result != 0) {
                LOGE("Failed to decode token %d, error: %d", i, decode_result);
                break;
            }
        }
        
        LOGI("Generation complete, generated %zu chars", result.length());
        
        if (batch_allocated) llama_batch_free(batch);
        if (sampler) llama_sampler_free(sampler);
        g_is_generating.store(false);
        
        return string_to_jstring(env, result);
        
    } catch (const std::exception& e) {
        LOGE("Exception during generation: %s", e.what());
        if (batch_allocated) llama_batch_free(batch);
        if (sampler) llama_sampler_free(sampler);
        g_is_generating.store(false);
        return string_to_jstring(env, std::string("Error: ") + e.what());
    } catch (...) {
        LOGE("Unknown exception during generation");
        if (batch_allocated) llama_batch_free(batch);
        if (sampler) llama_sampler_free(sampler);
        g_is_generating.store(false);
        return string_to_jstring(env, "Error: Unknown native error");
    }
}

// Cancel ongoing generation
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_cancelGenerationNative(JNIEnv *env, jobject thiz) {
    LOGI("Cancel generation requested");
    g_cancel_requested.store(true);
}

// Check if generation is in progress
JNIEXPORT jboolean JNICALL
Java_com_localllm_app_inference_LlamaAndroid_isGeneratingNative(JNIEnv *env, jobject thiz) {
    return g_is_generating.load();
}

// Get model info
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getModelInfoNative(JNIEnv *env, jobject thiz, jlong model_ptr) {
    if (model_ptr == 0) {
        return string_to_jstring(env, "{}");
    }
    
    try {
        llama_model *model = reinterpret_cast<llama_model *>(model_ptr);
        
        char desc[256];
        llama_model_desc(model, desc, sizeof(desc));
        
        // Build JSON info string
        std::string info = "{";
        info += "\"description\":\"" + std::string(desc) + "\",";
        info += "\"n_params\":" + std::to_string(llama_model_n_params(model)) + ",";
        info += "\"size\":" + std::to_string(llama_model_size(model));
        info += "}";
        
        return string_to_jstring(env, info);
    } catch (...) {
        return string_to_jstring(env, "{}");
    }
}

// Get context size
JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getContextSizeNative(JNIEnv *env, jobject thiz, jlong ctx_ptr) {
    if (ctx_ptr == 0) return 0;
    try {
        llama_context *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
        return llama_n_ctx(ctx);
    } catch (...) {
        return 0;
    }
}

// Get number of threads
JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getThreadCountNative(JNIEnv *env, jobject thiz) {
    return std::thread::hardware_concurrency();
}

// Check if Vulkan is available
JNIEXPORT jboolean JNICALL
Java_com_localllm_app_inference_LlamaAndroid_isVulkanAvailableNative(JNIEnv *env, jobject thiz) {
#ifdef GGML_USE_VULKAN
    try {
        // Check if Vulkan backend is available
        int device_count = ggml_backend_vk_get_device_count();
        return device_count > 0 ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
#else
    return JNI_FALSE;
#endif
}

// Get Vulkan device count
JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getVulkanDeviceCountNative(JNIEnv *env, jobject thiz) {
#ifdef GGML_USE_VULKAN
    try {
        return ggml_backend_vk_get_device_count();
    } catch (...) {
        return 0;
    }
#else
    return 0;
#endif
}

// Get Vulkan device name
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getVulkanDeviceNameNative(JNIEnv *env, jobject thiz, jint device_index) {
#ifdef GGML_USE_VULKAN
    try {
        char description[256];
        ggml_backend_vk_get_device_description(device_index, description, sizeof(description));
        return string_to_jstring(env, description);
    } catch (...) {
        return string_to_jstring(env, "Unknown Device");
    }
#else
    return string_to_jstring(env, "Vulkan not available");
#endif
}

// System info
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getSystemInfoNative(JNIEnv *env, jobject thiz) {
    try {
        return string_to_jstring(env, llama_print_system_info());
    } catch (...) {
        return string_to_jstring(env, "Error getting system info");
    }
}

} // extern "C"

