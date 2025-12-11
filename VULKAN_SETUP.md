# Vulkan GPU Acceleration for LocalLLM Android

This document explains how to enable Vulkan GPU acceleration for faster inference on your Android device.

## Overview

Vulkan GPU acceleration can provide **2-10x faster** inference speeds compared to CPU-only execution, depending on your device's GPU capabilities.

### Why Vulkan?

- ✅ **Cross-platform**: Supported on most Android 7.0+ devices
- ✅ **Efficient**: Direct GPU memory access
- ✅ **Modern**: Latest graphics API standard
- ✅ **Flexible**: Works with integrated and discrete GPUs

## Prerequisites

### System Requirements

- **Android Device**: Android 7.0 (API 24) or higher with Vulkan support
- **Development Machine**: Windows, macOS, or Linux
- **Tools**:
  - CMake 3.19+
  - Vulkan SDK (includes `glslc` shader compiler)
  - Android NDK (installed via Android Studio)

### Check Device Vulkan Support

To check if your Android device supports Vulkan:

1. Install [Vulkan Hardware Capability Viewer](https://play.google.com/store/apps/details?id=de.saschawillems.vulkancapsviewer)
2. Open the app to see your device's Vulkan capabilities

Most devices from 2017+ support Vulkan 1.1 or higher.

## Setup Instructions

### Step 1: Install Vulkan SDK

#### Windows
```powershell
# Download from https://vulkan.lunarg.com/sdk/home
# Run the installer and add to PATH
```

#### macOS
```bash
brew install vulkan-tools molten-vk
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install vulkan-tools libvulkan-dev
```

Verify installation:
```bash
glslc --version
```

### Step 2: Generate Vulkan Shaders

The Android NDK cannot compile Vulkan shaders directly. You must pre-compile them on your development machine:

#### Using PowerShell (Windows)
```powershell
cd app/src/main/cpp
.\generate_vulkan_shaders.ps1
```

#### Using Bash (Linux/macOS)
```bash
cd app/src/main/cpp
chmod +x generate_vulkan_shaders.sh
./generate_vulkan_shaders.sh
```

This will:
1. Build the `vulkan-shaders-gen` tool
2. Generate `vulkan-shaders-hpp.hpp` with pre-compiled shaders
3. Place the file in `llama.cpp/ggml/src/ggml-vulkan/vulkan-shaders/`

**Note**: You only need to regenerate shaders when updating llama.cpp.

### Step 3: Enable Vulkan in Build Configuration

Open `app/build.gradle.kts` and modify the CMake configuration:

```kotlin
android {
    // ... other config ...
    
    defaultConfig {
        // ... other config ...
        
        externalNativeBuild {
            cmake {
                // Add this line to enable Vulkan
                arguments += "-DLOCALLLM_ENABLE_VULKAN=ON"
                
                // Existing arguments
                arguments += "-DANDROID_STL=c++_shared"
                cppFlags += "-std=c++17"
                cppFlags += "-fexceptions"
                cppFlags += "-frtti"
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }
}
```

### Step 4: Build the App

```bash
# Clean build
./gradlew clean

# Build with Vulkan support
./gradlew assembleDebug
```

Watch for build messages:
- ✅ `Vulkan support enabled - using pre-compiled shaders`
- ✅ `Linking Vulkan library for GPU acceleration`

If you see warnings about missing shaders, go back to Step 2.

## Usage

### In Code

The app will automatically detect and use Vulkan if available:

```kotlin
// Check Vulkan availability
val llamaAndroid = LlamaAndroid()
val deviceInfo = llamaAndroid.getDeviceInfo()

if (deviceInfo.hasVulkan) {
    Log.i("GPU", "Vulkan available: ${deviceInfo.vulkanDevices}")
    
    // Load model with GPU acceleration
    llamaAndroid.loadModel(
        modelPath = "/path/to/model.gguf",
        gpuLayers = 32  // Number of layers to offload to GPU
    )
} else {
    Log.i("GPU", "Vulkan not available, using CPU")
}
```

### GPU Layers Parameter

The `gpuLayers` parameter controls how much of the model runs on the GPU:

- `0`: CPU-only (no GPU acceleration)
- `1-10`: Partial offloading (good for testing)
- `20-40`: Recommended for most models
- `99`: Maximum offloading (all layers on GPU)

**Recommendations by model size**:
- **Small models (< 3B)**: 32 layers
- **Medium models (3B-7B)**: 24-32 layers
- **Large models (7B-13B)**: 16-24 layers
- **Very large models (13B+)**: 8-16 layers

Higher values = faster but more VRAM usage.

### Settings UI

Users can adjust GPU settings in the app's Settings screen:

- **Enable GPU Acceleration**: Toggle Vulkan on/off
- **GPU Layers**: Slider to adjust layer offloading
- **GPU Device**: Select which GPU to use (if multiple available)

## Performance Tips

### Optimization Strategies

1. **Start Conservative**: Begin with 16-24 GPU layers and increase gradually
2. **Monitor Memory**: Watch for out-of-memory errors
3. **Test on Target Device**: Performance varies widely by device
4. **Balance Temperature**: High GPU usage can cause thermal throttling

### Expected Performance

Performance improvements depend on your device:

| Device GPU | Speed Increase | Notes |
|------------|---------------|-------|
| Adreno 600+ | 3-5x | Modern Qualcomm devices |
| Mali G7x+ | 2-4x | Samsung/Mediatek devices |
| Adreno 500 | 1.5-3x | Older Qualcomm devices |
| Mali G5x | 1.5-2x | Budget devices |

### Benchmarking

To measure performance:

```kotlin
val startTime = System.currentTimeMillis()
val result = llamaAndroid.generateTokens(/* ... */)
val duration = System.currentTimeMillis() - startTime
val tokensPerSecond = result.tokenCount / (duration / 1000.0)
Log.i("Benchmark", "Speed: $tokensPerSecond tokens/sec")
```

## Troubleshooting

### Build Errors

**Error**: `Vulkan support enabled but pre-compiled shaders not found`
```
Solution: Run generate_vulkan_shaders.ps1 or .sh script
```

**Error**: `glslc: command not found`
```
Solution: Install Vulkan SDK and add to PATH
```

**Error**: `ggml-vulkan.h: No such file or directory`
```
Solution: Make sure llama.cpp submodule is up to date:
  git submodule update --init --recursive
```

### Runtime Errors

**App crashes on model load with GPU enabled**
```
Solution: Reduce gpuLayers parameter or disable GPU
```

**No performance improvement**
```
Solution: 
1. Verify Vulkan is actually enabled (check logs)
2. Increase gpuLayers parameter
3. Ensure device has good Vulkan support
```

**Out of memory errors**
```
Solution: Reduce gpuLayers or use smaller model
```

### Debugging

Enable verbose logging:

```kotlin
// In LlamaAndroid.kt
Log.d(TAG, "Device info: $deviceInfo")
Log.d(TAG, "Loading with ${gpuLayers} GPU layers")
```

Check logcat for Vulkan messages:
```bash
adb logcat | grep -E "Vulkan|GGML|GPU"
```

## Advanced Configuration

### Multiple GPU Devices

For devices with multiple GPUs:

```kotlin
val devices = llamaAndroid.getVulkanDevices()
devices.forEachIndexed { index, name ->
    Log.i("GPU", "Device $index: $name")
}

// Select specific device (implementation may vary)
llamaAndroid.setVulkanDevice(0)  // Use first GPU
```

### Custom Shader Compilation

If you need specific Vulkan extensions:

```bash
cd app/src/main/cpp/llama.cpp/ggml/src/ggml-vulkan/vulkan-shaders

# Build with custom flags
cmake -B build \
  -DGGML_VULKAN_SHADER_DEBUG_INFO=ON \
  -DGGML_VULKAN_VALIDATE=ON

cmake --build build
./build/vulkan-shaders-gen
```

### Disabling Vulkan at Runtime

Even if built with Vulkan support, you can disable it:

```kotlin
// Always use CPU
llamaAndroid.loadModel(
    modelPath = modelPath,
    gpuLayers = 0  // Disable GPU
)
```

## FAQ

**Q: Do I need to regenerate shaders for every build?**  
A: No, only when updating the llama.cpp submodule.

**Q: Can I use Vulkan and CPU acceleration together?**  
A: Yes! The `gpuLayers` parameter controls the split.

**Q: Does Vulkan work on emulators?**  
A: Generally no. Emulator Vulkan support is limited.

**Q: Why is my first inference slow with GPU?**  
A: The first run initializes GPU resources. Subsequent runs are faster.

**Q: Can I use OpenCL instead of Vulkan?**  
A: Vulkan is recommended for Android. OpenCL support is deprecated.

## Resources

- [Vulkan SDK](https://vulkan.lunarg.com/sdk/home)
- [llama.cpp Vulkan Backend](https://github.com/ggerganov/llama.cpp/tree/master/ggml/src/ggml-vulkan)
- [Android Vulkan Guide](https://developer.android.com/ndk/guides/graphics/getting-started)
- [Vulkan Hardware Database](https://vulkan.gpuinfo.org/)

## Contributing

Found a bug or have suggestions? Please open an issue on GitHub.

## License

This implementation follows the llama.cpp MIT license.
