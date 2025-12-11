#!/bin/bash
# Script to generate Vulkan shaders for Android build
# Run this on your development machine before building the Android app

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHADER_DIR="$SCRIPT_DIR/llama.cpp/ggml/src/ggml-vulkan/vulkan-shaders"
BUILD_DIR="$SHADER_DIR/build"

echo "=========================================="
echo "Vulkan Shader Generator for Android"
echo "=========================================="
echo ""

# Check if llama.cpp exists
if [ ! -d "$SCRIPT_DIR/llama.cpp" ]; then
    echo "❌ Error: llama.cpp directory not found!"
    echo "   Please clone llama.cpp first:"
    echo "   git clone https://github.com/ggerganov/llama.cpp.git"
    exit 1
fi

# Check for required tools
command -v cmake >/dev/null 2>&1 || { echo "❌ Error: cmake not found. Please install CMake."; exit 1; }

# Check for Vulkan SDK
if ! command -v glslc >/dev/null 2>&1; then
    echo "⚠️  Warning: glslc not found in PATH"
    echo "   You may need to install Vulkan SDK:"
    echo "   - Linux: https://vulkan.lunarg.com/sdk/home"
    echo "   - macOS: brew install vulkan-tools"
    echo "   - Windows: https://vulkan.lunarg.com/sdk/home"
    echo ""
fi

echo "📂 Building vulkan-shaders-gen..."
echo "   Working directory: $SHADER_DIR"

# Create build directory
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Configure CMake
echo ""
echo "🔧 Configuring CMake..."
cmake .. -DCMAKE_BUILD_TYPE=Release

# Build shader generator
echo ""
echo "🔨 Building shader generator..."
cmake --build . --config Release

# Run shader generator
echo ""
echo "⚡ Generating Vulkan shader headers..."
if [ -f "./vulkan-shaders-gen" ] || [ -f "./Release/vulkan-shaders-gen.exe" ]; then
    if [ -f "./vulkan-shaders-gen" ]; then
        ./vulkan-shaders-gen
    else
        ./Release/vulkan-shaders-gen.exe
    fi
    
    # Check if output was generated
    if [ -f "../vulkan-shaders-hpp.hpp" ]; then
        echo ""
        echo "✅ Success! Vulkan shaders generated:"
        echo "   → $SHADER_DIR/vulkan-shaders-hpp.hpp"
        echo ""
        echo "📱 You can now build your Android app with Vulkan support:"
        echo "   1. Open build.gradle.kts"
        echo "   2. Add: arguments += \"-DLOCALLLM_ENABLE_VULKAN=ON\""
        echo "   3. Rebuild the app"
        echo ""
    else
        echo "❌ Error: Shader header file not generated!"
        exit 1
    fi
else
    echo "❌ Error: vulkan-shaders-gen executable not found!"
    exit 1
fi

echo "=========================================="
