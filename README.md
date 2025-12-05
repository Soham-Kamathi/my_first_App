# LocalLLM - Android LLM Inference App

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120" alt="LocalLLM Logo"/>
</p>

<p align="center">
  <strong>Run Large Language Models locally on your Android device</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#installation">Installation</a> •
  <a href="#building-from-source">Building</a> •
  <a href="#supported-models">Models</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## Overview

LocalLLM is an Android application that enables on-device inference of Large Language Models (LLMs) using the [llama.cpp](https://github.com/ggerganov/llama.cpp) library. It provides a ChatGPT-like interface for interacting with GGUF-format models, all running locally on your device with complete privacy.

**No internet connection required for inference. Your conversations never leave your device.**

---

## Features

### 🚀 Core Features
- **Local Inference** - Run LLMs entirely on-device using llama.cpp
- **Model Library** - Browse, download, and manage 25+ GGUF models from Hugging Face
- **Streaming Responses** - Real-time token generation with live updates
- **Conversation History** - Persistent chat history stored locally
- **Multiple Conversations** - Create and manage multiple chat sessions

### 🏠 Feature Hub (Home Screen)
The app features a modern home screen with quick access to all capabilities:

| Feature | Description |
|---------|-------------|
| **💬 AI Chat** | Full-featured ChatGPT-like conversations |
| **🧪 Prompt Lab** | Experiment with prompts and parameters |
| **🖼️ Ask Image** | Analyze images with vision-capable models |
| **🎙️ Audio Scribe** | Record, transcribe, and summarize audio |
| **📄 Document Chat** | Upload PDFs/docs and ask questions |
| **💻 Code Companion** | Code explanation, debugging, and generation |
| **📝 Templates** | Pre-made conversation starters |

### 🎯 AI Chat Features
- **Smart Context Management** - KV cache automatically managed per conversation
- **Follow-up Queries** - Maintain context within the same conversation
- **System Prompts** - Customize AI behavior per conversation
- **Generation Stats** - See tokens/second and generation time

### 🧪 Prompt Lab
- Direct prompt input without conversation overhead
- Real-time parameter adjustment (temperature, top-p, top-k)
- Template selection for different models
- Quick experimentation mode

### 📄 Document Chat
- **Supported Formats**: PDF, TXT, Markdown, Code files
- **Smart Chunking**: Documents split intelligently for context fitting
- **Q&A Interface**: Ask questions about uploaded documents
- Powered by PDFBox for PDF text extraction

### 💻 Code Companion
- **Explain Code**: Get detailed explanations of code snippets
- **Debug Code**: Find and fix bugs in your code
- **Review Code**: Get suggestions for improvements
- **Generate Code**: Create code from descriptions
- **Convert Code**: Translate between programming languages

### 🗣️ Text-to-Speech (TTS)
- **Read Aloud**: Listen to AI responses
- **Adjustable Speed**: Control speech rate (0.5x - 2.0x)
- **Pitch Control**: Customize voice pitch
- **Multiple Languages**: Support for various locales

### 📤 Export & Sharing
- **Plain Text Export**: Simple text format
- **JSON Export**: Structured format for reimport
- **Markdown Export**: Formatted with headers
- **Share**: Send conversations to other apps

### 📱 User Experience
- **Modern Material 3 UI** - Clean, intuitive interface following Material Design guidelines
- **Dark/Light Theme** - Automatic theme switching based on system settings
- **Device Capability Detection** - Automatically recommends models based on your device's RAM
- **Download Management** - Background downloads with progress tracking and resume support

### ⚙️ Technical Features
- **Memory-Mapped Loading** - Efficient model loading using mmap
- **Multi-threaded Inference** - Utilizes all available CPU cores
- **Customizable Generation** - Adjustable temperature, top-p, top-k, and repeat penalty
- **14 Prompt Templates** - Support for ChatML, Llama 2/3, Mistral, Phi, Gemma, and more

---

## Screenshots

| Home Screen | AI Chat | Model Library |
|:-----------:|:-------:|:-------------:|
| Feature hub with quick access | ChatGPT-like interface | Browse & download models |

| Document Chat | Code Companion | Settings |
|:-------------:|:--------------:|:--------:|
| PDF/text Q&A | Code assistance | Generation parameters |

---

## Requirements

### Minimum Requirements
- Android 8.0 (API 26) or higher
- ARM64 (arm64-v8a) or x86_64 processor
- 2 GB RAM minimum
- 1 GB free storage (plus space for models)

### Recommended
- Android 11.0 or higher
- 6+ GB RAM for larger models
- 10+ GB free storage

---

## Installation

### Pre-built APK

1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from unknown sources" in your device settings
3. Open the APK file to install
4. Launch LocalLLM from your app drawer

### Using ADB

```bash
# Install the APK (replace existing if already installed)
adb install -r app-debug.apk

# Launch the app
adb shell am start -n com.localllm.app/.MainActivity
```

---

## Building from Source

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or later
- **Android SDK** with API 34
- **Android NDK** 26.1.10909125
- **CMake** 3.22.1
- **Git** for cloning llama.cpp

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Soham-Kamathi/my_first_App.git
   cd my_first_App
   ```

2. **Clone llama.cpp into the cpp directory**
   ```bash
   cd app/src/main/cpp
   git clone https://github.com/ggerganov/llama.cpp.git
   cd ../../../..
   ```

3. **Build using Gradle**
   ```bash
   # Debug build
   ./gradlew assembleDebug
   
   # Release build
   ./gradlew assembleRelease
   ```

4. **Find the APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   app/build/outputs/apk/release/app-release.apk
   ```

### Build Configuration

The native build is configured in:
- `app/src/main/cpp/CMakeLists.txt` - CMake configuration
- `app/src/main/cpp/llama_jni.cpp` - JNI bridge implementation
- `app/build.gradle.kts` - NDK and CMake settings

**NDK Settings:**
```kotlin
android {
    ndkVersion = "26.1.10909125"
    
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

---

## Supported Models

LocalLLM supports any GGUF-format model compatible with llama.cpp. The app includes a curated catalog of **25+ mobile-optimized models**:

### Model Catalog

#### Qwen 2.5 Family (ChatML)
| Model | Size | Min RAM | Best For |
|-------|------|---------|----------|
| Qwen 2.5 0.5B | ~400 MB | 1 GB | Ultra-fast, low-end devices |
| Qwen 2.5 1.5B | ~1 GB | 2 GB | Balanced quality/speed |
| Qwen 2.5 3B | ~2 GB | 3 GB | Better quality |
| Qwen 2.5 Coder 1.5B | ~1 GB | 2 GB | Code generation |
| Qwen 2.5 Coder 3B | ~2 GB | 3 GB | Advanced coding |

#### Llama 3.2 Family (Llama3)
| Model | Size | Min RAM | Best For |
|-------|------|---------|----------|
| Llama 3.2 1B | ~750 MB | 1.5 GB | Meta's compact model |
| Llama 3.2 3B | ~2 GB | 3 GB | Quality conversations |

#### SmolLM2 Family (ChatML)
| Model | Size | Min RAM | Best For |
|-------|------|---------|----------|
| SmolLM2 135M | ~100 MB | 512 MB | Extremely fast |
| SmolLM2 360M | ~250 MB | 768 MB | Very fast |
| SmolLM2 1.7B | ~1.1 GB | 2 GB | Good balance |

#### Microsoft Phi Family
| Model | Size | Min RAM | Best For |
|-------|------|---------|----------|
| Phi-3 Mini 3.8B | ~2.3 GB | 3.5 GB | Reasoning |
| Phi-4 Mini 3.8B | ~2.4 GB | 3.5 GB | Latest reasoning |

#### Other Notable Models
| Model | Size | Min RAM | Best For |
|-------|------|---------|----------|
| Gemma 2 2B | ~1.6 GB | 2.5 GB | Google's model |
| DeepSeek 1.3B | ~900 MB | 1.5 GB | General purpose |
| TinyLlama 1.1B | ~670 MB | 1.5 GB | Classic small model |
| StarCoder2 3B | ~2 GB | 3 GB | Code completion |
| Aya Expanse 8B | ~5 GB | 6 GB | Multilingual |
| OpenHermes 2.5 Mistral | ~4.4 GB | 5 GB | Instruction following |

### Quantization Formats

| Format | Quality | Size | Recommended |
|--------|---------|------|-------------|
| Q8_0 | Excellent | Large | High-end devices |
| Q6_K | Very Good | Medium-Large | Good devices |
| Q5_K_M | Good | Medium | Balanced |
| **Q4_K_M** | Decent | **Small** | **Recommended** |
| Q4_K_S | Decent | Smaller | Memory constrained |
| Q3_K_M | Lower | Very Small | Very limited RAM |
| Q2_K | Lowest | Smallest | Ultra-constrained |

### Prompt Templates Supported

| Template | Models |
|----------|--------|
| `chatml` | Qwen, SmolLM, TinyLlama, OpenHermes |
| `llama2` | Llama 2 family |
| `llama3` | Llama 3, 3.1, 3.2 |
| `mistral` | Mistral, Mixtral |
| `phi3` | Phi-3, Phi-4 |
| `gemma` | Gemma 1, 2 |
| `alpaca` | Alpaca-style |
| `vicuna` | Vicuna models |
| `zephyr` | Zephyr, Notus |
| `deepseek` | DeepSeek |
| `cohere` | Cohere Aya |
| `starcoder` | StarCoder |

---

## Architecture

### Project Structure

```
app/
├── src/main/
│   ├── java/com/localllm/app/
│   │   ├── data/                    # Data layer
│   │   │   ├── local/               # Room database, DAOs
│   │   │   ├── model/               # Data classes
│   │   │   ├── remote/              # Hugging Face API
│   │   │   └── repository/          # Repositories
│   │   ├── di/                      # Hilt DI modules
│   │   │   ├── DatabaseModule.kt
│   │   │   ├── NetworkModule.kt
│   │   │   ├── RepositoryModule.kt
│   │   │   └── InferenceModule.kt
│   │   ├── domain/usecase/          # Business logic
│   │   ├── inference/               # LLM inference
│   │   │   ├── InferenceEngine.kt   # Prompt building, generation
│   │   │   ├── LlamaAndroid.kt      # JNI bridge
│   │   │   └── ModelManager.kt      # Model lifecycle
│   │   ├── ui/
│   │   │   ├── components/          # Reusable UI components
│   │   │   ├── navigation/          # Navigation setup
│   │   │   ├── screen/              # All screens
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   ├── ChatScreen.kt
│   │   │   │   ├── PromptLabScreen.kt
│   │   │   │   ├── AskImageScreen.kt
│   │   │   │   ├── AudioScribeScreen.kt
│   │   │   │   ├── DocumentChatScreen.kt
│   │   │   │   ├── CodeCompanionScreen.kt
│   │   │   │   ├── ConversationTemplatesScreen.kt
│   │   │   │   ├── ModelLibraryScreen.kt
│   │   │   │   └── SettingsScreen.kt
│   │   │   ├── theme/               # Material theme
│   │   │   └── viewmodel/           # ViewModels
│   │   ├── util/                    # Utilities
│   │   │   ├── TtsManager.kt        # Text-to-Speech
│   │   │   ├── DocumentParser.kt    # PDF/text parsing
│   │   │   └── ConversationExporter.kt
│   │   └── worker/                  # Background tasks
│   ├── cpp/                         # Native code
│   │   ├── CMakeLists.txt
│   │   ├── llama_jni.cpp            # JNI implementation
│   │   └── llama.cpp/               # llama.cpp library
│   └── res/                         # Resources
└── build.gradle.kts
```

### Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 1.9.20 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt 2.48.1 |
| **Database** | Room 2.6.1 |
| **Preferences** | DataStore |
| **Networking** | Retrofit 2 + OkHttp |
| **Native** | llama.cpp via JNI |
| **Build** | Gradle 8.9 + KSP |
| **PDF Parsing** | PDFBox Android |

### Key Components

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────────────┐  │
│  │  Home   │ │  Chat   │ │ PromptLab│ │ DocumentChat    │  │
│  │ Screen  │ │ Screen  │ │  Screen  │ │    Screen       │  │
│  └────┬────┘ └────┬────┘ └────┬─────┘ └────────┬────────┘  │
│       │          │           │                 │           │
│  ┌────▼────┐ ┌───▼────┐ ┌────▼─────┐ ┌────────▼────────┐  │
│  │  Home   │ │  Chat  │ │PromptLab │ │  DocumentChat   │  │
│  │ViewModel│ │ViewModel│ │ ViewModel│ │   ViewModel     │  │
│  └─────────┘ └────┬───┘ └──────────┘ └─────────────────┘  │
└───────────────────┼─────────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                     Inference Layer                          │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐  │
│  │ InferenceEngine │ │  ModelManager   │ │ LlamaAndroid  │  │
│  │ • buildPrompt() │ │ • loadModel()   │ │ • JNI Bridge  │  │
│  │ • generateStream│ │ • clearKVCache()│ │ • Native calls│  │
│  └─────────────────┘ └─────────────────┘ └───────┬───────┘  │
└──────────────────────────────────────────────────┼──────────┘
                                                   │
┌──────────────────────────────────────────────────▼──────────┐
│                      Native Layer (C++)                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    llama.cpp                         │    │
│  │  • Model loading    • Token generation               │    │
│  │  • KV cache mgmt    • Sampling (temp, top-p, top-k)  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Configuration

### Generation Parameters

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| `temperature` | 0.7 | 0.0 - 2.0 | Controls randomness. Lower = more focused |
| `topP` | 0.9 | 0.0 - 1.0 | Nucleus sampling threshold |
| `topK` | 40 | 1 - 100 | Limits vocabulary to top K tokens |
| `repeatPenalty` | 1.1 | 1.0 - 2.0 | Penalizes repetition |
| `maxTokens` | 512 | 1 - 4096 | Maximum response length |
| `contextSize` | 2048 | 512 - 32768 | Context window (model dependent) |

### System Prompts

Each conversation can have a custom system prompt. Default templates:
- **Assistant** - General helpful assistant
- **Coder** - Programming assistance
- **Writer** - Creative writing help
- **Tutor** - Educational explanations

---

## Troubleshooting

### App Crashes on Model Load

1. **Check available RAM** - Close other apps to free memory
2. **Try a smaller model** - Start with Qwen 0.5B or SmolLM2 135M
3. **Check logs**: 
   ```bash
   adb logcat | grep -E "LlamaJNI|ModelManager|llama"
   ```

### Model Download Fails

1. **Check internet connection**
2. **Verify storage space** - Models need GB of free space
3. **Try manual download** - Download GGUF from Hugging Face directly

### Slow Generation

1. **Reduce context size** - Lower values use less memory
2. **Use smaller quantization** - Q4_K_S instead of Q4_K_M
3. **Reduce thread count** - Sometimes fewer threads = faster
4. **Use smaller model** - SmolLM2 is very fast

### Context/Hallucination Issues

The app automatically manages KV cache:
- **Same conversation**: Context preserved for follow-ups ✓
- **Different conversation**: Cache cleared automatically ✓
- **New conversation**: Fresh start with no previous context ✓

### Native Library Not Loading

```bash
# Check if library is in APK
unzip -l app-debug.apk | grep ".so"

# Should show:
# lib/arm64-v8a/liblocalllm.so
# lib/x86_64/liblocalllm.so
```

---

## API Reference

### LlamaAndroid

```kotlin
// Initialize backend (call once on app start)
llamaAndroid.initBackend()

// Load a model
val contextPtr = llamaAndroid.loadModel(
    modelPath = "/path/to/model.gguf",
    threads = 4,
    contextSize = 2048,
    useMmap = true
)

// Generate text with streaming
llamaAndroid.generateTokens(
    ctxPtr = contextPtr,
    prompt = "Hello, how are you?",
    maxTokens = 512,
    temperature = 0.7f,
    callback = object : LlamaAndroid.TokenCallback {
        override fun onToken(token: String) {
            // Handle each generated token
        }
    }
)

// Clear KV cache (when switching conversations)
llamaAndroid.clearKVCache(contextPtr)

// Cleanup
llamaAndroid.freeModel(contextPtr)
llamaAndroid.freeBackend()
```

### InferenceEngine

```kotlin
// Generate with Flow (recommended)
inferenceEngine.generateStream(
    prompt = buildPrompt(messages),
    config = GenerationConfig(
        maxTokens = 512,
        temperature = 0.7f
    )
) { token ->
    // Handle streaming token
}.collect { result ->
    when (result) {
        is GenerationResult.Success -> // Handle success
        is GenerationResult.Error -> // Handle error
    }
}
```

### TtsManager

```kotlin
// Speak text
ttsManager.speak("Hello, I am your AI assistant!")

// Adjust settings
ttsManager.updateConfig(TtsConfig(
    speechRate = 1.2f,
    pitch = 1.0f,
    language = Locale.US
))

// Stop speaking
ttsManager.stop()
```

### DocumentParser

```kotlin
// Parse a document
val result = documentParser.parseDocument(uri, fileName)
result.onSuccess { doc ->
    println("Content: ${doc.content}")
    println("Pages: ${doc.pageCount}")
    println("Words: ${doc.wordCount}")
}

// Chunk for RAG-style querying
val chunks = documentParser.chunkText(doc.content, chunkSize = 1000)
```

---

## Performance Tips

1. **Use mmap loading** - Enabled by default, reduces memory pressure
2. **Optimize thread count** - Usually `cores - 1` is best
3. **Reduce context size** - Only use what you need
4. **Quantization matters** - Q4_K_M offers best speed/quality ratio
5. **Smart model selection** - Match model size to your device RAM
6. **Clear KV cache** - Handled automatically per conversation

### Device Recommendations

| Device RAM | Recommended Models |
|------------|-------------------|
| 2-3 GB | SmolLM2 135M/360M, Qwen 0.5B |
| 4 GB | Qwen 1.5B, SmolLM2 1.7B, Llama 3.2 1B |
| 6 GB | Qwen 3B, Llama 3.2 3B, Phi-3 Mini |
| 8+ GB | Any model in catalog |

---

## Privacy

LocalLLM is designed with privacy as a core principle:

- ✅ **All inference runs locally** - No data sent to external servers
- ✅ **Conversations stored locally** - In Room database on device
- ✅ **No analytics or tracking** - Your usage is completely private
- ✅ **No account required** - Use the app without signing up
- ✅ **Export your data** - Full control over your conversations
- ⚠️ **Model downloads** - Fetched from Hugging Face (one-time)

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

1. Clone and open in Android Studio
2. Sync Gradle files
3. Clone llama.cpp into `app/src/main/cpp/`
4. Run on emulator or device

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Write unit tests for new features

---

## Documentation

For detailed technical documentation, see:
- **[TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md)** - Comprehensive 1400+ line technical guide covering architecture, native code, data layer, inference engine, and more.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **llama.cpp** - MIT License
- **Jetpack Compose** - Apache 2.0 License
- **Material Icons** - Apache 2.0 License
- **OkHttp/Retrofit** - Apache 2.0 License
- **PDFBox** - Apache 2.0 License

---

## Acknowledgments

- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Amazing C++ inference library
- [Hugging Face](https://huggingface.co) - Model hosting and API
- [Google](https://developer.android.com) - Android SDK and Jetpack
- All the open-source model creators
- [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) - Design inspiration

---

## Roadmap

### Completed ✅
- [x] Home screen feature hub
- [x] Document Chat with PDF support
- [x] Code Companion
- [x] Conversation Templates
- [x] Text-to-Speech
- [x] Export & Sharing
- [x] Smart KV cache management
- [x] 25+ model catalog

### Planned 📋
- [ ] GPU acceleration (Vulkan)
- [ ] Voice input (Speech-to-Text)
- [ ] Image understanding (multimodal models)
- [ ] RAG with vector embeddings
- [ ] Model fine-tuning on device
- [ ] Widgets and quick actions
- [ ] Wear OS companion app

---

<p align="center">
  Made with ❤️ for the Android and AI community
</p>

<p align="center">
  <a href="#localllm---android-llm-inference-app">Back to top</a>
</p>
