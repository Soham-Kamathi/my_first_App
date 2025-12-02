/**
 * Stub implementation of llama_android for development without llama.cpp
 * 
 * This file provides a minimal implementation that allows the app to compile
 * and run without the actual llama.cpp library, useful for UI development.
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <chrono>
#include <atomic>

#define LOG_TAG "LlamaAndroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_should_cancel{false};

struct StubContext {
    int n_ctx = 2048;
    int n_threads = 4;
};

extern "C" {

JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_initBackend(JNIEnv* env, jobject thiz) {
    LOGI("Stub: Initializing llama backend");
}

JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeBackend(JNIEnv* env, jobject thiz) {
    LOGI("Stub: Freeing llama backend");
}

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
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Stub: Loading model from: %s", path);
    env->ReleaseStringUTFChars(modelPath, path);
    
    // Simulate loading delay
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    
    auto* ctx = new StubContext();
    ctx->n_ctx = contextSize;
    ctx->n_threads = threads;
    
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_freeModel(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("Stub: Freeing model context");
    if (contextPtr != 0) {
        delete reinterpret_cast<StubContext*>(contextPtr);
    }
}

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
    g_is_generating = true;
    g_should_cancel = false;
    
    // Stub response
    std::string response = "I am a stub response from the LocalLLM app. "
        "The actual llama.cpp library is not linked. "
        "This response simulates what a real LLM response would look like. "
        "To enable real inference, please build llama.cpp for Android and link it to the project.";
    
    // Simulate streaming by calling callback with tokens
    if (callback != nullptr) {
        jclass callbackClass = env->GetObjectClass(callback);
        jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
        
        if (onTokenMethod != nullptr) {
            // Split response into words and stream them
            std::string word;
            for (size_t i = 0; i < response.size() && !g_should_cancel; ++i) {
                word += response[i];
                if (response[i] == ' ' || i == response.size() - 1) {
                    jstring tokenStr = env->NewStringUTF(word.c_str());
                    env->CallVoidMethod(callback, onTokenMethod, tokenStr);
                    env->DeleteLocalRef(tokenStr);
                    word.clear();
                    
                    // Simulate generation delay
                    std::this_thread::sleep_for(std::chrono::milliseconds(50));
                }
            }
        }
    }
    
    g_is_generating = false;
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_cancelGeneration(
    JNIEnv* env,
    jobject thiz
) {
    LOGI("Stub: Cancelling generation");
    g_should_cancel = true;
}

JNIEXPORT jboolean JNICALL
Java_com_localllm_app_inference_LlamaAndroid_isGenerating(
    JNIEnv* env,
    jobject thiz
) {
    return g_is_generating ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getContextSize(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    if (contextPtr == 0) return 2048;
    return reinterpret_cast<StubContext*>(contextPtr)->n_ctx;
}

JNIEXPORT jint JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getVocabSize(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    return 32000;
}

JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_clearKVCache(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("Stub: Clearing KV cache");
}

JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_getSystemInfo(
    JNIEnv* env,
    jobject thiz
) {
    return env->NewStringUTF("llama.cpp: Stub mode - Native library not linked");
}

} // extern "C"
