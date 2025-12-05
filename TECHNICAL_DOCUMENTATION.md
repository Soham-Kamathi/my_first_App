# LocalLLM - Comprehensive Technical Documentation

> **A Complete Guide to Understanding How LocalLLM Works**

This document provides an in-depth technical explanation of the LocalLLM Android application. It covers everything from the high-level architecture to the low-level native code, explaining how each component works and why specific technologies were chosen. After reading this document, anyone should be able to answer technical questions about the app.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [What is LocalLLM?](#2-what-is-localllm)
3. [Technology Stack Overview](#3-technology-stack-overview)
4. [Architecture Deep Dive](#4-architecture-deep-dive)
5. [Native Code & llama.cpp Integration](#5-native-code--llamacpp-integration)
6. [Data Layer](#6-data-layer)
7. [Inference Engine](#7-inference-engine)
8. [User Interface Layer](#8-user-interface-layer)
9. [Feature Breakdown](#9-feature-breakdown)
10. [Model Management](#10-model-management)
11. [Memory & Performance](#11-memory--performance)
12. [Build System](#12-build-system)
13. [Glossary](#13-glossary)
14. [FAQ](#14-faq)

---

## 1. Executive Summary

**LocalLLM** is an Android application that runs Large Language Models (LLMs) directly on your phone without any internet connection. It uses a C++ library called **llama.cpp** to perform the actual AI computations, wrapped in a modern Android app built with **Kotlin** and **Jetpack Compose**.

### Key Technical Facts:
- **Language**: Kotlin (Android), C++ (native inference)
- **UI Framework**: Jetpack Compose with Material 3
- **AI Engine**: llama.cpp (compiled for Android ARM64 and x86_64)
- **Model Format**: GGUF (quantized models)
- **Database**: Room (SQLite)
- **Architecture**: Clean Architecture with MVVM pattern
- **Dependency Injection**: Hilt (Dagger)
- **Min Android Version**: API 26 (Android 8.0)

---

## 2. What is LocalLLM?

### 2.1 The Problem It Solves

Traditional AI chatbots like ChatGPT require:
- Internet connection
- Sending your data to remote servers
- Subscription fees
- Privacy concerns

LocalLLM solves these by running AI models entirely on your device.

### 2.2 How It Works (Simplified)

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER DEVICE                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐ │
│  │   User UI   │ --> │  ViewModel  │ --> │  Inference Engine   │ │
│  │  (Compose)  │     │   (Kotlin)  │     │      (Kotlin)       │ │
│  └─────────────┘     └─────────────┘     └──────────┬──────────┘ │
│                                                      │           │
│                                          ┌───────────▼─────────┐ │
│                                          │   JNI Bridge        │ │
│                                          │   (Kotlin/C++)      │ │
│                                          └───────────┬─────────┘ │
│                                                      │           │
│                                          ┌───────────▼─────────┐ │
│                                          │   llama.cpp         │ │
│                                          │   (Native C++)      │ │
│                                          └───────────┬─────────┘ │
│                                                      │           │
│                                          ┌───────────▼─────────┐ │
│                                          │   GGUF Model        │ │
│                                          │   (On Device)       │ │
│                                          └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Key Features

| Feature | Description |
|---------|-------------|
| AI Chat | ChatGPT-like conversations |
| Prompt Lab | Experiment with different prompts |
| Ask Image | Analyze images with AI (vision models) |
| Audio Scribe | Transcribe and summarize audio |
| Document Chat | Upload PDFs/docs and ask questions |
| Code Companion | Code explanation, debugging, generation |
| Conversation Templates | Pre-made conversation starters |
| Text-to-Speech | Read AI responses aloud |
| Export & Share | Save conversations as text/JSON |

---

## 3. Technology Stack Overview

### 3.1 Languages Used

| Language | Usage | Why |
|----------|-------|-----|
| **Kotlin** | Main Android code (95%) | Modern, safe, concise Android development |
| **C++** | Native inference code (5%) | Performance-critical AI computations |
| **CMake** | Build configuration | Industry standard for C++ builds |

### 3.2 Key Libraries & Frameworks

#### Android/Kotlin Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose | BOM 2023.10.01 | Modern declarative UI framework |
| Material 3 | Latest | Google's design system |
| Hilt | 2.48.1 | Dependency injection (manages object creation) |
| Room | 2.6.1 | SQLite database wrapper |
| DataStore | 1.0.0 | Key-value preferences storage |
| Navigation Compose | 2.7.5 | Screen navigation |
| Retrofit | 2.9.0 | HTTP client for API calls |
| Coroutines | 1.7.3 | Asynchronous programming |
| WorkManager | 2.9.0 | Background task scheduling |
| Coil | 2.5.0 | Image loading |

#### Native Libraries

| Library | Purpose |
|---------|---------|
| llama.cpp | Core LLM inference engine |
| GGML | Tensor math library (part of llama.cpp) |

### 3.3 Build Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Gradle | 8.9 | Build automation |
| Android Gradle Plugin | 8.2.0 | Android build tools |
| NDK | 26.1.10909125 | Native Development Kit for C++ |
| CMake | 3.22.1 | C++ build system |
| KSP | 1.9.20-1.0.14 | Kotlin Symbol Processing (compile-time code gen) |

---

## 4. Architecture Deep Dive

### 4.1 Clean Architecture

The app follows **Clean Architecture** principles, organizing code into layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                          │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  UI (Screens)  <-->  ViewModels  <-->  UI State             │ │
│  │  - ChatScreen        - ChatViewModel    - StateFlow         │ │
│  │  - HomeScreen        - HomeViewModel    - LiveData          │ │
│  │  - SettingsScreen    - etc.                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                       DOMAIN LAYER                               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Use Cases (Business Logic)                                 │ │
│  │  - SendMessageUseCase                                       │ │
│  │  - CreateConversationUseCase                                │ │
│  │  - GenerateResponseUseCase                                  │ │
│  │  - DownloadModelUseCase                                     │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                        DATA LAYER                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Repositories  <-->  Data Sources                           │ │
│  │  - ConversationRepository    - Room Database (Local)        │ │
│  │  - ModelRepository           - Retrofit API (Remote)        │ │
│  │                              - DataStore (Preferences)      │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      INFERENCE LAYER                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  InferenceEngine  <-->  ModelManager  <-->  LlamaAndroid    │ │
│  │  (Prompt building)       (Model lifecycle)   (JNI Bridge)   │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                       NATIVE LAYER                               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  llama_jni.cpp  <-->  llama.cpp  <-->  GGML                 │ │
│  │  (JNI interface)      (LLM core)        (Math operations)   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 MVVM Pattern

Each screen follows **Model-View-ViewModel (MVVM)**:

```kotlin
// VIEW (UI) - ChatScreen.kt
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    // Displays messages, sends input to ViewModel
}

// VIEWMODEL - ChatViewModel.kt
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val modelManager: ModelManager
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    fun sendMessage(text: String) {
        // Business logic here
    }
}

// MODEL - ChatMessage.kt
data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    // ...
)
```

### 4.3 Dependency Injection with Hilt

Hilt automatically creates and provides objects where needed:

```kotlin
// Module - defines HOW to create objects
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocalLLMDatabase {
        return Room.databaseBuilder(...)
            .build()
    }
}

// Usage - just add @Inject and Hilt provides it
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val modelManager: ModelManager,  // Hilt provides this
    private val repository: ConversationRepository  // And this
) : ViewModel()
```

**Hilt Modules in the App:**
| Module | Purpose |
|--------|---------|
| `DatabaseModule` | Provides Room database, DAOs, DataStore |
| `NetworkModule` | Provides Retrofit, OkHttp client |
| `RepositoryModule` | Provides repositories |
| `UseCaseModule` | Provides use cases |
| `InferenceModule` | Provides LlamaAndroid, ModelManager, InferenceEngine |

### 4.4 Package Structure

```
com.localllm.app/
├── LocalLLMApplication.kt    # App entry point, Hilt setup
├── MainActivity.kt           # Single activity (hosts all screens)
├── data/
│   ├── local/                # Local data storage
│   │   ├── dao/              # Room DAOs (database queries)
│   │   ├── LocalLLMDatabase.kt
│   │   └── PreferencesDataStore.kt
│   ├── model/                # Data classes
│   │   ├── ChatMessage.kt
│   │   ├── Conversation.kt
│   │   ├── ModelInfo.kt
│   │   └── ...
│   ├── remote/               # API services
│   └── repository/           # Data access logic
├── di/                       # Hilt dependency modules
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── ...
├── domain/
│   └── usecase/              # Business logic
│       ├── ConversationUseCases.kt
│       ├── ModelUseCases.kt
│       └── GenerateResponseUseCase.kt
├── inference/                # LLM inference code
│   ├── InferenceEngine.kt    # Prompt building, generation
│   ├── LlamaAndroid.kt       # JNI bridge to C++
│   └── ModelManager.kt       # Model loading/unloading
├── ui/
│   ├── components/           # Reusable UI components
│   ├── navigation/           # Navigation setup
│   ├── screen/               # All screens
│   │   ├── ChatScreen.kt
│   │   ├── HomeScreen.kt
│   │   └── ...
│   ├── theme/                # Colors, typography
│   └── viewmodel/            # ViewModels for each screen
├── util/                     # Utility classes
│   ├── TtsManager.kt         # Text-to-Speech
│   ├── DocumentParser.kt     # PDF/text parsing
│   └── ConversationExporter.kt
└── worker/                   # Background tasks
```

---

## 5. Native Code & llama.cpp Integration

### 5.1 What is llama.cpp?

**llama.cpp** is an open-source C++ library that runs Large Language Models efficiently on CPUs. It was created by Georgi Gerganov and is the most popular way to run LLMs on consumer hardware.

**Why llama.cpp?**
- Pure C/C++ - runs on any platform
- Highly optimized for CPU inference
- Supports quantization (smaller, faster models)
- Memory-mapped file loading (efficient RAM usage)
- Active development community

### 5.2 JNI Bridge Architecture

**JNI (Java Native Interface)** allows Kotlin/Java code to call C++ code:

```
┌──────────────────┐     JNI Call      ┌──────────────────┐
│   Kotlin Code    │ ───────────────> │    C++ Code      │
│                  │                   │                  │
│  LlamaAndroid.kt │                   │  llama_jni.cpp   │
│                  │ <─────────────── │                  │
└──────────────────┘     Return       └──────────────────┘
```

### 5.3 How Native Code is Loaded

```kotlin
// LlamaAndroid.kt
companion object {
    init {
        // Loads liblocallm.so at app startup
        System.loadLibrary("localllm")
    }
}
```

When `System.loadLibrary("localllm")` is called, Android looks for:
- `lib/arm64-v8a/liblocalllm.so` (for 64-bit ARM devices)
- `lib/x86_64/liblocalllm.so` (for x86_64 emulators)

### 5.4 Native Functions Explained

The C++ file `llama_jni.cpp` contains these key functions:

#### 5.4.1 Backend Initialization

```cpp
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_initBackendNative(JNIEnv *env, jobject thiz) {
    llama_backend_init();  // Initialize llama.cpp
}
```

**What it does:** Initializes the llama.cpp library. Called once when the app starts.

#### 5.4.2 Model Loading

```cpp
JNIEXPORT jlong JNICALL
Java_com_localllm_app_inference_LlamaAndroid_loadModelNative(
    JNIEnv *env, jobject thiz,
    jstring model_path,    // Path to .gguf file
    jint threads,          // Number of CPU threads
    jint context_size,     // Context window size
    jboolean use_mmap      // Use memory mapping
) {
    // Load model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;  // CPU only on Android
    model_params.use_mmap = use_mmap;
    
    // Load the model
    llama_model* model = llama_load_model_from_file(path, model_params);
    
    // Create context for inference
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_threads = threads;
    
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    
    return (jlong)ctx;  // Return pointer to Kotlin
}
```

**Key parameters:**
- `n_ctx`: Context size (how many tokens the model can "see")
- `n_threads`: CPU threads for parallel computation
- `use_mmap`: Memory-map the file (loads faster, uses less RAM)

#### 5.4.3 Token Generation

```cpp
JNIEXPORT jstring JNICALL
Java_com_localllm_app_inference_LlamaAndroid_generateTokensNative(
    JNIEnv *env, jobject thiz,
    jlong ctx_ptr,
    jstring prompt,
    jint max_tokens,
    jfloat temperature,
    jfloat top_p,
    jint top_k,
    jfloat repeat_penalty,
    jobject callback
) {
    // 1. Tokenize the prompt
    std::vector<llama_token> tokens = tokenize(ctx, prompt_str);
    
    // 2. Process prompt tokens (fills KV cache)
    llama_decode(ctx, batch);
    
    // 3. Generate tokens one by one
    for (int i = 0; i < max_tokens; i++) {
        // Sample next token based on probabilities
        llama_token new_token = llama_sample_token(ctx, ...);
        
        // Convert token to text
        std::string piece = llama_token_to_piece(ctx, new_token);
        
        // Call back to Kotlin with the token
        env->CallVoidMethod(callback, onTokenMethod, token_jstr);
        
        // Check for stop token
        if (new_token == llama_token_eos(model)) break;
    }
}
```

**The generation loop:**
1. Convert prompt text → token IDs
2. Feed tokens through model (fills KV cache)
3. For each new token:
   - Get probability distribution over vocabulary
   - Sample using temperature, top-p, top-k
   - Convert token ID → text
   - Send to UI via callback
   - Repeat until max tokens or stop token

#### 5.4.4 KV Cache Management

```cpp
JNIEXPORT void JNICALL
Java_com_localllm_app_inference_LlamaAndroid_clearKVCacheNative(
    JNIEnv *env, jobject thiz, jlong ctx_ptr
) {
    llama_kv_cache_clear((llama_context*)ctx_ptr);
}
```

**What is KV Cache?**
- Stores attention key/value pairs from processed tokens
- Allows the model to "remember" conversation context
- Must be cleared when switching conversations

### 5.5 CMake Build Configuration

The `CMakeLists.txt` file configures how native code is built:

```cmake
# Project setup
cmake_minimum_required(VERSION 3.22.1)
project(localllm LANGUAGES C CXX)

# C++ standard
set(CMAKE_CXX_STANDARD 17)

# Optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -fPIC")

# 16KB page alignment for Android 15+
set(CMAKE_SHARED_LINKER_FLAGS "-Wl,-z,max-page-size=16384")

# Disable GPU backends (CPU only)
set(GGML_CUDA OFF)
set(GGML_VULKAN OFF)
set(GGML_METAL OFF)

# Add llama.cpp as subdirectory
add_subdirectory(llama.cpp)

# Create our JNI library
add_library(localllm SHARED llama_jni.cpp)

# Link with llama.cpp
target_link_libraries(localllm llama ggml log android)
```

### 5.6 Supported Architectures

| Architecture | Description | Usage |
|--------------|-------------|-------|
| `arm64-v8a` | 64-bit ARM | Most modern Android phones |
| `x86_64` | 64-bit x86 | Android emulators |

---

## 6. Data Layer

### 6.1 Room Database

**Room** is Android's SQLite abstraction layer. It provides:
- Compile-time SQL verification
- Type-safe queries
- Automatic mapping to Kotlin classes

#### Database Schema

```
┌─────────────────────────────────────────────────────────────────┐
│                     LocalLLMDatabase                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌───────────────────┐     ┌───────────────────────────────┐   │
│  │      models       │     │        conversations          │   │
│  ├───────────────────┤     ├───────────────────────────────┤   │
│  │ id (PK)           │     │ id (PK)                       │   │
│  │ name              │     │ title                         │   │
│  │ parameterCount    │     │ modelId (FK → models)         │   │
│  │ quantization      │     │ systemPrompt                  │   │
│  │ fileSizeBytes     │     │ createdAt                     │   │
│  │ downloadUrl       │     │ updatedAt                     │   │
│  │ minRamMb          │     │ messageCount                  │   │
│  │ description       │     └───────────────────────────────┘   │
│  │ promptTemplate    │                    │                    │
│  │ contextLength     │                    │ 1:N                │
│  │ isDownloaded      │                    ▼                    │
│  │ localPath         │     ┌───────────────────────────────┐   │
│  └───────────────────┘     │        chat_messages          │   │
│                            ├───────────────────────────────┤   │
│                            │ id (PK)                       │   │
│                            │ conversationId (FK)           │   │
│                            │ role (USER/ASSISTANT/SYSTEM)  │   │
│                            │ content                       │   │
│                            │ timestamp                     │   │
│                            │ isComplete                    │   │
│                            │ tokensGenerated               │   │
│                            │ generationTimeMs              │   │
│                            └───────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

#### Entity Classes

```kotlin
// Conversation entity
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String?,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0
)

// ChatMessage entity
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val isComplete: Boolean = true,
    val tokensGenerated: Int = 0,
    val generationTimeMs: Long = 0
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }
```

#### DAO (Data Access Object)

```kotlin
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM chat_messages WHERE conversationId = :id ORDER BY timestamp")
    fun getMessages(id: String): Flow<List<ChatMessage>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
    
    @Delete
    suspend fun deleteConversation(conversation: Conversation)
}
```

### 6.2 DataStore Preferences

**DataStore** stores user preferences as key-value pairs:

```kotlin
class PreferencesDataStore(context: Context) {
    private val dataStore = context.dataStore
    
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            defaultSystemPrompt = prefs[DEFAULT_SYSTEM_PROMPT] ?: "You are a helpful assistant.",
            maxTokens = prefs[MAX_TOKENS] ?: 512,
            temperature = prefs[TEMPERATURE] ?: 0.7f,
            topP = prefs[TOP_P] ?: 0.9f,
            topK = prefs[TOP_K] ?: 40,
            repeatPenalty = prefs[REPEAT_PENALTY] ?: 1.1f,
            threadCount = prefs[THREAD_COUNT] ?: 4,
            contextSize = prefs[CONTEXT_SIZE] ?: 2048,
            theme = prefs[THEME] ?: "system"
        )
    }
}
```

### 6.3 Repository Pattern

Repositories abstract data sources from the rest of the app:

```kotlin
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    fun getAllConversations(): Flow<List<Conversation>> = 
        conversationDao.getAllConversations()
    
    fun getMessages(conversationId: String): Flow<List<ChatMessage>> =
        conversationDao.getMessages(conversationId)
    
    suspend fun createConversation(title: String, modelId: String?): Conversation {
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(conversation)
        return conversation
    }
}
```

---

## 7. Inference Engine

### 7.1 Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    INFERENCE LAYER                               │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │  InferenceEngine │  │   ModelManager   │  │  LlamaAndroid  │ │
│  │                  │  │                  │  │                │ │
│  │ • buildPrompt()  │  │ • loadModel()    │  │ • loadModel()  │ │
│  │ • generateStream │  │ • unloadModel()  │  │ • generate()   │ │
│  │ • cancelGenerate │  │ • clearKVCache() │  │ • tokenize()   │ │
│  │                  │  │ • getContextPtr()│  │ • JNI bridge   │ │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬───────┘ │
│           │                     │                     │         │
│           └─────────────────────┼─────────────────────┘         │
│                                 │                               │
│                                 ▼                               │
│                    ┌────────────────────────┐                   │
│                    │    Native llama.cpp    │                   │
│                    │    (liblocalllm.so)    │                   │
│                    └────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Model Manager

The `ModelManager` handles model lifecycle:

```kotlin
@Singleton
class ModelManager @Inject constructor(
    private val llamaAndroid: LlamaAndroid
) {
    private var currentContextPtr: Long? = null
    private val _loadingState = MutableStateFlow<ModelLoadingState>(ModelLoadingState.NotLoaded)
    
    val isModelLoaded: Boolean
        get() = currentContextPtr != null && currentContextPtr != 0L
    
    suspend fun loadModel(model: ModelInfo, threads: Int, contextSize: Int): Result<Unit> {
        // 1. Unload any existing model
        unloadModel()
        
        // 2. Load new model via JNI
        val contextPtr = llamaAndroid.loadModel(
            modelPath = model.localPath!!,
            threads = threads,
            contextSize = contextSize,
            useMmap = true
        )
        
        // 3. Store context pointer
        currentContextPtr = contextPtr
        _loadingState.value = ModelLoadingState.Loaded(model)
        
        return Result.success(Unit)
    }
    
    fun clearKVCache() {
        currentContextPtr?.let { ptr ->
            llamaAndroid.clearKVCache(ptr)
        }
    }
}
```

### 7.3 Prompt Templates

Different models expect different prompt formats. The `InferenceEngine` handles this:

#### ChatML (Used by Qwen, SmolLM, TinyLlama)
```
<|im_start|>system
You are a helpful assistant.
<|im_end|>
<|im_start|>user
Hello!
<|im_end|>
<|im_start|>assistant
Hi there! How can I help you today?
<|im_end|>
<|im_start|>user
What is 2+2?
<|im_end|>
<|im_start|>assistant
```

#### Llama 2 Format
```
<s>[INST] <<SYS>>
You are a helpful assistant.
<</SYS>>

Hello! [/INST] Hi there! How can I help you today? </s><s>[INST] What is 2+2? [/INST]
```

#### Llama 3 Format
```
<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are a helpful assistant.<|eot_id|>
<|start_header_id|>user<|end_header_id|>
Hello!<|eot_id|>
<|start_header_id|>assistant<|end_header_id|>
```

#### Mistral Format
```
<s>[INST] Hello! [/INST]Hi there! How can I help you today?</s>[INST] What is 2+2? [/INST]
```

#### All Supported Templates

| Template | Models |
|----------|--------|
| `chatml` | Qwen, SmolLM, TinyLlama, OpenHermes |
| `llama2` | Llama 2 family |
| `llama3` | Llama 3, 3.1, 3.2 family |
| `mistral` | Mistral, Mixtral |
| `alpaca` | Alpaca-style instruction models |
| `vicuna` | Vicuna models |
| `zephyr` | Zephyr, Notus |
| `phi` | Microsoft Phi-1, Phi-2 |
| `phi3` | Microsoft Phi-3, Phi-4 |
| `gemma` | Google Gemma |
| `deepseek` | DeepSeek models |
| `cohere` | Cohere Aya |
| `starcoder` | StarCoder code models |

### 7.4 Generation Configuration

```kotlin
data class GenerationConfig(
    val maxTokens: Int = 512,        // Max tokens to generate
    val temperature: Float = 0.7f,   // Randomness (0-2)
    val topP: Float = 0.9f,          // Nucleus sampling
    val topK: Int = 40,              // Top-K sampling
    val repeatPenalty: Float = 1.1f  // Penalty for repetition
)
```

**What these parameters do:**

| Parameter | Effect | Typical Range |
|-----------|--------|---------------|
| `temperature` | Higher = more random, creative. Lower = more deterministic | 0.1 - 1.5 |
| `topP` | Only consider tokens with cumulative probability ≤ topP | 0.5 - 0.95 |
| `topK` | Only consider top K most likely tokens | 10 - 100 |
| `repeatPenalty` | Discourages repeating tokens | 1.0 - 1.3 |

### 7.5 Streaming Generation

```kotlin
fun generateStream(
    prompt: String,
    config: GenerationConfig,
    onTokenGenerated: (String) -> Unit
): Flow<GenerationResult> = flow {
    val callback = object : LlamaAndroid.TokenCallback {
        override fun onToken(token: String) {
            tokensGenerated++
            onTokenGenerated(token)  // Update UI immediately
        }
    }
    
    val result = llamaAndroid.generateTokens(
        ctxPtr = contextPtr,
        prompt = prompt,
        maxTokens = config.maxTokens,
        temperature = config.temperature,
        topP = config.topP,
        topK = config.topK,
        repeatPenalty = config.repeatPenalty,
        callback = callback
    )
    
    emit(GenerationResult.Success(result, tokensGenerated, generationTime))
}
```

---

## 8. User Interface Layer

### 8.1 Jetpack Compose

**Jetpack Compose** is Android's modern UI toolkit. Instead of XML layouts, UI is written in Kotlin:

```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat") }) },
        bottomBar = { MessageInput(onSend = viewModel::sendMessage) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
    }
}
```

**Key Compose concepts:**

| Concept | Description |
|---------|-------------|
| `@Composable` | Functions that describe UI |
| `State` | Data that, when changed, triggers recomposition |
| `remember` | Remembers values across recompositions |
| `collectAsState()` | Converts Flow to Compose State |
| `LazyColumn` | Efficient scrollable list (like RecyclerView) |
| `Modifier` | Adds decoration, padding, click handlers |

### 8.2 Navigation

The app uses **Navigation Compose** for screen navigation:

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat?conversationId={conversationId}")
    object PromptLab : Screen("prompt_lab")
    object AskImage : Screen("ask_image")
    object AudioScribe : Screen("audio_scribe")
    object DocumentChat : Screen("document_chat")
    object CodeCompanion : Screen("code_companion")
    object ConversationTemplates : Screen("conversation_templates")
    object ModelLibrary : Screen("model_library")
    object Settings : Screen("settings")
    object ConversationHistory : Screen("conversation_history")
}

@Composable
fun LocalLLMNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(...) }
        composable(Screen.Chat.route) { ChatScreen(...) }
        // ... more screens
    }
}
```

### 8.3 Material 3 Design

The app follows **Material 3** design guidelines:

```kotlin
@Composable
fun LocalLLMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### 8.4 Screens Overview

| Screen | Purpose | Key Features |
|--------|---------|--------------|
| `HomeScreen` | Feature hub | Grid of feature cards |
| `ChatScreen` | Main chat interface | Message list, input, streaming |
| `PromptLabScreen` | Prompt experimentation | Template selection, parameter tuning |
| `AskImageScreen` | Image analysis | Image picker, vision model |
| `AudioScribeScreen` | Audio transcription | Recording, playback, summarization |
| `DocumentChatScreen` | Document Q&A | PDF/text upload, chunking |
| `CodeCompanionScreen` | Code assistance | Explain, debug, review actions |
| `ConversationTemplatesScreen` | Template starters | Pre-defined conversation prompts |
| `ModelLibraryScreen` | Model management | Browse, download, delete models |
| `SettingsScreen` | App settings | Theme, parameters, model config |
| `ConversationHistoryScreen` | Chat history | List, search, delete conversations |

---

## 9. Feature Breakdown

### 9.1 AI Chat

**Flow:**
1. User types message → `ChatViewModel.sendMessage()`
2. Message saved to Room database
3. `generateResponse()` called
4. Prompt built from conversation history
5. Native generation via JNI
6. Tokens streamed to UI via callback
7. Complete response saved to database

**Key classes:**
- `ChatScreen.kt` - UI
- `ChatViewModel.kt` - Logic
- `InferenceEngine.kt` - Generation
- `ConversationUseCases.kt` - Database operations

### 9.2 Prompt Lab

**Purpose:** Experiment with different prompts and parameters without full conversation context.

**Features:**
- Direct prompt input
- Template selection
- Real-time parameter adjustment
- One-shot generation

### 9.3 Document Chat

**Flow:**
1. User uploads PDF/TXT/MD file
2. `DocumentParser` extracts text
3. Text chunked for context fitting
4. User asks question
5. Relevant chunks + question sent to model
6. Response generated

**Chunking strategy:**
```kotlin
fun chunkText(text: String, chunkSize: Int = 1000, overlap: Int = 200): List<TextChunk> {
    // Split into overlapping chunks for better context
}
```

### 9.4 Code Companion

**Actions:**
- **Explain Code**: Describe what code does
- **Debug Code**: Find and fix bugs
- **Review Code**: Suggest improvements
- **Generate Code**: Create code from description
- **Convert Code**: Translate between languages

### 9.5 Text-to-Speech

**Implementation:**
```kotlin
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }
    
    fun stop() {
        tts?.stop()
    }
}
```

### 9.6 Export & Sharing

**Formats:**
- Plain text
- JSON (for reimport)
- Markdown

```kotlin
object ConversationExporter {
    fun exportToText(conversation: Conversation, messages: List<ChatMessage>): String
    fun exportToJson(conversation: Conversation, messages: List<ChatMessage>): String
    fun exportToMarkdown(conversation: Conversation, messages: List<ChatMessage>): String
}
```

---

## 10. Model Management

### 10.1 GGUF Format

**GGUF (GPT-Generated Unified Format)** is the model format used by llama.cpp:

- Self-contained (model weights + metadata)
- Supports quantization
- Optimized for CPU inference
- Single file format

### 10.2 Quantization Levels

| Quantization | Size Reduction | Quality | Speed |
|--------------|----------------|---------|-------|
| Q8_0 | ~50% | Excellent | Slower |
| Q6_K | ~60% | Very Good | Medium |
| Q5_K_M | ~65% | Good | Fast |
| Q4_K_M | ~75% | Decent | Faster |
| Q4_0 | ~75% | Lower | Fastest |
| Q3_K | ~80% | Lower | Fastest |
| Q2_K | ~85% | Lowest | Fastest |

**Recommendation:** Q4_K_M offers the best balance for mobile devices.

### 10.3 Model Catalog

The app includes 25+ models from Hugging Face:

| Model Family | Sizes | Best For |
|--------------|-------|----------|
| Qwen 2.5 | 0.5B-3B | General, coding |
| Llama 3.2 | 1B-3B | General purpose |
| SmolLM2 | 135M-1.7B | Low-end devices |
| Phi-4 Mini | 3.8B | Reasoning |
| Gemma 2 | 2B | General purpose |
| DeepSeek | 1.3B | General purpose |
| StarCoder2 | 3B | Code generation |
| TinyLlama | 1.1B | Fast inference |

### 10.4 Model Download Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  ModelLibrary   │ -> │  ModelDownloader │ -> │  Hugging Face   │
│     Screen      │    │    (OkHttp)      │    │      API        │
└────────┬────────┘    └────────┬─────────┘    └────────┬────────┘
         │                      │                       │
         │    Start Download    │                       │
         │ ──────────────────> │    GET request        │
         │                      │ ───────────────────> │
         │                      │                       │
         │                      │    Stream response    │
         │                      │ <─────────────────── │
         │                      │                       │
         │   Progress updates   │                       │
         │ <────────────────── │                       │
         │                      │                       │
         │                      │    Write to file     │
         │                      │ ──────────────────>  │
         │                      │    (app storage)     │
         │   Download complete  │                       │
         │ <────────────────── │                       │
         │                      │                       │
         │   Update database    │                       │
         │ ──────────────────> │                       │
└─────────────────────────────────────────────────────────────────┘
```

### 10.5 Device Capability Detection

```kotlin
data class DeviceInfo(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val cpuCores: Int,
    val isLowRamDevice: Boolean
) {
    fun recommendedModelSize(): String = when {
        totalRamMb >= 8192 -> "7B Q4_K_M or smaller"
        totalRamMb >= 6144 -> "3B Q4_K_M or smaller"
        totalRamMb >= 4096 -> "1.5B Q4_K_M or smaller"
        else -> "0.5B - 1B models only"
    }
}
```

---

## 11. Memory & Performance

### 11.1 Memory-Mapped Loading (mmap)

Instead of loading the entire model into RAM:

```
Traditional Loading:
[Disk] ---(read all)---> [RAM: Full Model]
Memory usage: Model size + overhead

Memory-Mapped Loading:
[Disk] <---(page fault)---> [RAM: Active Pages Only]
Memory usage: Only pages being accessed
```

**Benefits:**
- Faster initial load time
- Lower peak memory usage
- OS manages page swapping

### 11.2 Thread Configuration

```kotlin
val recommendedThreads = Runtime.getRuntime().availableProcessors() - 1
// Leave one core for system/UI
```

| Device Cores | Inference Threads | UI Thread |
|--------------|-------------------|-----------|
| 8 | 7 | 1 |
| 4 | 3 | 1 |
| 2 | 1 | 1 |

### 11.3 Context Window Management

The **context window** is how much text the model can "see" at once:

- Larger context = more conversation history
- Larger context = more memory usage
- Trade-off between context and RAM

| Context Size | Memory Overhead | Use Case |
|--------------|-----------------|----------|
| 512 | ~100MB | Very limited devices |
| 2048 | ~400MB | Standard mobile |
| 4096 | ~800MB | High-end devices |

### 11.4 KV Cache Management

The KV (Key-Value) cache stores attention states:

```kotlin
// Clear when switching conversations
fun setCurrentConversation(conversationId: String) {
    if (previousConversationId != conversationId) {
        modelManager.clearKVCache()  // Prevent context leakage
    }
    _currentConversationId.value = conversationId
}
```

**Why clear KV cache?**
- Prevents "hallucinations" from previous conversations
- Frees memory
- Ensures clean context for new conversation

---

## 12. Build System

### 12.1 Gradle Configuration

**Root `build.gradle.kts`:**
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

**App `build.gradle.kts`:**
```kotlin
android {
    compileSdk = 34
    minSdk = 26
    targetSdk = 34
    
    // NDK for native code
    ndkVersion = "26.1.10909125"
    
    // CMake for building llama.cpp
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    
    // Target architectures
    ndk {
        abiFilters += listOf("arm64-v8a", "x86_64")
    }
}
```

### 12.2 Build Variants

| Variant | Debuggable | Minified | App ID Suffix |
|---------|------------|----------|---------------|
| debug | Yes | No | `.debug` |
| release | No | Yes (ProGuard) | None |

### 12.3 Building the App

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

### 12.4 ProGuard Configuration

For release builds, ProGuard minifies and obfuscates code:

```proguard
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Hilt-generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.localllm.app.data.model.** { *; }
```

---

## 13. Glossary

| Term | Definition |
|------|------------|
| **LLM** | Large Language Model - AI model trained on text to generate responses |
| **GGUF** | File format for LLM models used by llama.cpp |
| **Quantization** | Reducing model precision (float32 → int4) for smaller size |
| **Token** | Smallest unit of text (word, subword, or character) |
| **Context Window** | Maximum number of tokens the model can process at once |
| **KV Cache** | Stored attention values for faster generation |
| **JNI** | Java Native Interface - bridge between Kotlin and C++ |
| **NDK** | Native Development Kit - tools for C++ on Android |
| **Inference** | Running a model to generate predictions |
| **Streaming** | Sending output token-by-token as generated |
| **Prompt Template** | Format for structuring input to specific models |
| **mmap** | Memory-mapped files - efficient file access |
| **Temperature** | Controls randomness in generation (higher = more random) |
| **Top-P** | Nucleus sampling - only consider tokens with cumulative prob ≤ P |
| **Top-K** | Only consider the K most likely next tokens |
| **Room** | Android database library (wrapper for SQLite) |
| **Hilt** | Dependency injection library for Android |
| **Compose** | Modern Android UI toolkit |
| **ViewModel** | Holds UI state, survives configuration changes |
| **Flow** | Kotlin's reactive stream for asynchronous data |
| **Coroutines** | Kotlin's concurrency framework |

---

## 14. FAQ

### Q1: How does the AI run without internet?

The model file (GGUF) is downloaded once and stored on the device. After that, all inference happens locally using the device's CPU. The llama.cpp library performs all calculations without any network requests.

### Q2: Why are models so large (1-4 GB)?

LLMs have billions of parameters (weights). Even with quantization (Q4 = 4-bit), a 3B parameter model requires ~3B × 4 bits = ~1.5 GB. This is the trade-off for having AI on-device.

### Q3: Why is generation slow on my phone?

LLM inference is computationally intensive. Speed depends on:
- CPU cores and speed
- Model size (smaller = faster)
- Quantization level (lower bits = faster)
- Context length (shorter = faster)

### Q4: What's the difference between Q4, Q5, Q8 quantization?

These represent bits per weight:
- Q8: 8 bits - best quality, largest size
- Q5: 5 bits - good quality, medium size
- Q4: 4 bits - decent quality, smallest practical size
- Q2-Q3: Lower quality, smallest size

### Q5: Why does the app need so much RAM?

The model must be loaded into memory for inference. A 2GB model needs at least 2GB RAM, plus ~500MB-1GB for context/KV cache, plus Android system overhead.

### Q6: Can I use GPU acceleration?

Currently, the app uses CPU only. GPU acceleration via Vulkan is possible with llama.cpp but requires additional implementation and testing for Android compatibility.

### Q7: Why use llama.cpp instead of TensorFlow Lite?

llama.cpp is:
- Purpose-built for LLMs
- Highly optimized for CPU inference
- Supports GGUF quantized models
- Active community with frequent updates
- Easier to integrate new models

### Q8: How is privacy ensured?

All inference happens on-device. The app:
- Doesn't send conversation data anywhere
- Stores all data locally (Room database)
- Model downloads are direct from Hugging Face (no middleman)
- No analytics or tracking

### Q9: Why does my conversation sometimes include irrelevant context?

If KV cache isn't cleared between conversations, the model can "remember" previous context. The app clears KV cache when:
- Switching conversations
- Creating new conversations
- The first message in a new conversation

### Q10: How do I add new models?

New models can be added to the model catalog by:
1. Finding a GGUF model on Hugging Face
2. Adding its metadata to `ModelRepository.kt`
3. Specifying the correct prompt template
4. Rebuilding the app

---

## Appendix A: File Reference

| File | Purpose |
|------|---------|
| `LlamaAndroid.kt` | JNI bridge to C++ |
| `llama_jni.cpp` | Native C++ implementation |
| `ModelManager.kt` | Model lifecycle management |
| `InferenceEngine.kt` | Prompt building, generation |
| `ChatViewModel.kt` | Chat screen logic |
| `ChatScreen.kt` | Chat UI |
| `LocalLLMDatabase.kt` | Room database definition |
| `ConversationDao.kt` | Database queries |
| `ModelRepository.kt` | Model data + catalog |
| `Navigation.kt` | Screen navigation setup |
| `TtsManager.kt` | Text-to-speech |
| `DocumentParser.kt` | PDF/text parsing |
| `CMakeLists.txt` | Native build config |
| `build.gradle.kts` | Gradle build config |

---

## Appendix B: Key Dependencies Explained

| Dependency | What It Does | Why We Need It |
|------------|--------------|----------------|
| `androidx.compose.*` | UI framework | Building the user interface |
| `hilt-android` | Dependency injection | Managing object creation |
| `room-*` | Database | Storing conversations/models |
| `datastore-preferences` | Key-value storage | User settings |
| `navigation-compose` | Navigation | Moving between screens |
| `retrofit2` | HTTP client | Downloading models |
| `kotlinx-coroutines` | Async programming | Background tasks |
| `coil-compose` | Image loading | Loading images |
| `pdfbox-android` | PDF parsing | Document chat feature |

---

*Last updated: December 2024*
*Version: 1.0.0*
