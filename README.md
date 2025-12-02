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
- **Model Library** - Browse, download, and manage GGUF models from Hugging Face
- **Streaming Responses** - Real-time token generation with live updates
- **Conversation History** - Persistent chat history stored locally
- **Multiple Conversations** - Create and manage multiple chat sessions

### 📱 User Experience
- **Modern Material 3 UI** - Clean, intuitive interface following Material Design guidelines
- **Dark/Light Theme** - Automatic theme switching based on system settings
- **Device Capability Detection** - Automatically recommends models based on your device's RAM
- **Download Management** - Background downloads with progress tracking and resume support

### ⚙️ Technical Features
- **Memory-Mapped Loading** - Efficient model loading using mmap
- **Multi-threaded Inference** - Utilizes all available CPU cores
- **Customizable Generation** - Adjustable temperature, top-p, top-k, and repeat penalty
- **Multiple Prompt Templates** - Support for various model formats (ChatML, Llama, Mistral, etc.)

---

## Screenshots

| Model Library | Chat Interface | Settings |
|:-------------:|:--------------:|:--------:|
| Browse and download models | Interact with loaded model | Customize generation parameters |

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
   git clone https://github.com/yourusername/localllm-android.git
   cd localllm-android
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

LocalLLM supports any GGUF-format model compatible with llama.cpp. The app includes a curated list of mobile-optimized models:

### Recommended for Mobile

| Model | Parameters | Size (Q4) | Min RAM | Description |
|-------|------------|-----------|---------|-------------|
| **Qwen 2.5 0.5B** | 0.5B | ~400 MB | 1 GB | Fastest, for low-end devices |
| **Llama 3.2 1B** | 1B | ~750 MB | 1.5 GB | Meta's latest small model |
| **Qwen 2.5 1.5B** | 1.5B | ~1 GB | 2 GB | Good quality/size balance |
| **SmolLM2 1.7B** | 1.7B | ~1.1 GB | 2 GB | HuggingFace's compact model |
| **TinyLlama 1.1B** | 1.1B | ~670 MB | 1.5 GB | Classic small model |
| **Gemma 2 2B** | 2B | ~1.6 GB | 2.5 GB | Google's model |
| **Llama 3.2 3B** | 3B | ~2 GB | 3 GB | Better quality |
| **Phi-3 Mini** | 3.8B | ~2.3 GB | 3.5 GB | Microsoft's reasoning model |

### Quantization Formats

The app prioritizes mobile-friendly quantizations:
- **Q4_K_M** - Best balance of size and quality (recommended)
- **Q4_K_S** - Slightly smaller, minor quality loss
- **Q3_K_M** - Smaller but noticeable quality loss
- **Q5_K_M** - Better quality, larger size

### Using Custom Models

1. Download any GGUF model from Hugging Face
2. Place it in your device's storage
3. The app will detect compatible models

---

## Architecture

### Project Structure

```
app/
├── src/main/
│   ├── java/com/localllm/app/
│   │   ├── data/                    # Data layer
│   │   │   ├── local/               # Room database
│   │   │   ├── model/               # Data models
│   │   │   ├── remote/              # Hugging Face API
│   │   │   └── repository/          # Data repositories
│   │   ├── di/                      # Hilt dependency injection
│   │   ├── domain/                  # Use cases
│   │   ├── inference/               # LLM inference engine
│   │   │   ├── InferenceEngine.kt   # Text generation
│   │   │   ├── LlamaAndroid.kt      # JNI wrapper
│   │   │   └── ModelManager.kt      # Model lifecycle
│   │   ├── ui/                      # Compose UI
│   │   │   ├── components/          # Reusable components
│   │   │   ├── screen/              # Screen composables
│   │   │   ├── theme/               # Material theme
│   │   │   └── viewmodel/           # ViewModels
│   │   └── util/                    # Utilities
│   ├── cpp/                         # Native code
│   │   ├── CMakeLists.txt           # CMake configuration
│   │   ├── llama_jni.cpp            # JNI bridge
│   │   └── llama.cpp/               # llama.cpp library
│   └── res/                         # Resources
└── build.gradle.kts                 # Build configuration
```

### Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 1.9.20 |
| **UI Framework** | Jetpack Compose with Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt 2.48.1 |
| **Database** | Room 2.6.1 |
| **Networking** | Retrofit 2 + OkHttp |
| **Native** | llama.cpp via JNI |
| **Build** | Gradle 8.9 with KSP |

### Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ ChatScreen  │  │ModelLibrary │  │  SettingsScreen     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │             │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────────▼──────────┐  │
│  │ChatViewModel│  │ModelLibrary │  │  SettingsViewModel  │  │
│  │             │  │  ViewModel  │  │                     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼────────────────────┼─────────────┘
          │                │                    │
┌─────────▼────────────────▼────────────────────▼─────────────┐
│                       Domain Layer                           │
│  ┌────────────────┐  ┌─────────────────┐  ┌──────────────┐  │
│  │ GenerateText   │  │DownloadModel    │  │ GetSettings  │  │
│  │   UseCase      │  │   UseCase       │  │   UseCase    │  │
│  └────────┬───────┘  └────────┬────────┘  └──────┬───────┘  │
└───────────┼───────────────────┼──────────────────┼──────────┘
            │                   │                  │
┌───────────▼───────────────────▼──────────────────▼──────────┐
│                        Data Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ InferenceEngine │  │ ModelRepository │  │SettingsRepo │  │
│  └────────┬────────┘  └────────┬────────┘  └──────┬──────┘  │
│           │                    │                  │          │
│  ┌────────▼────────┐  ┌────────▼────────┐  ┌─────▼──────┐  │
│  │  LlamaAndroid   │  │  Room Database  │  │ DataStore  │  │
│  │    (JNI)        │  │  + HuggingFace  │  │            │  │
│  └────────┬────────┘  └─────────────────┘  └────────────┘  │
└───────────┼─────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────┐
│                       Native Layer                           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    llama.cpp                         │    │
│  │  ┌───────────┐  ┌───────────┐  ┌─────────────────┐  │    │
│  │  │   Model   │  │  Context  │  │    Sampler      │  │    │
│  │  │  Loading  │  │  Manager  │  │    Pipeline     │  │    │
│  │  └───────────┘  └───────────┘  └─────────────────┘  │    │
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
2. **Try a smaller model** - Start with Qwen 0.5B
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

---

## Performance Tips

1. **Use mmap loading** - Enabled by default, reduces memory pressure
2. **Optimize thread count** - Usually `cores - 1` is best
3. **Reduce context size** - Only use what you need
4. **Quantization matters** - Q4_K_M offers best speed/quality ratio
5. **Clear KV cache** - Start new conversations to free memory

---

## Privacy

LocalLLM is designed with privacy as a core principle:

- ✅ **All inference runs locally** - No data sent to external servers
- ✅ **Conversations stored locally** - In encrypted Room database
- ✅ **No analytics or tracking** - Your usage is completely private
- ✅ **No account required** - Use the app without signing up
- ⚠️ **Model downloads** - Fetched from Hugging Face (metadata only)

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
3. Run on emulator or device

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Write unit tests for new features

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **llama.cpp** - MIT License
- **Jetpack Compose** - Apache 2.0 License
- **Material Icons** - Apache 2.0 License
- **OkHttp/Retrofit** - Apache 2.0 License

---

## Acknowledgments

- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Amazing C++ inference library
- [Hugging Face](https://huggingface.co) - Model hosting and API
- [Google](https://developer.android.com) - Android SDK and Jetpack
- All the open-source model creators

---

## Roadmap

- [ ] GPU acceleration (Vulkan/OpenCL)
- [ ] Voice input/output
- [ ] Image understanding (multimodal models)
- [ ] Model fine-tuning on device
- [ ] Conversation export/import
- [ ] Widgets and quick actions
- [ ] Wear OS companion app

---

<p align="center">
  Made with ❤️ for the Android and AI community
</p>

<p align="center">
  <a href="#localllm---android-llm-inference-app">Back to top</a>
</p>
