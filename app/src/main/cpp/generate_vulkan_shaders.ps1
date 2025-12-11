# PowerShell script to generate Vulkan shaders for Android build
# Run this on your development machine before building the Android app

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ShaderDir = Join-Path $ScriptDir "llama.cpp\ggml\src\ggml-vulkan\vulkan-shaders"
$BuildDir = Join-Path $ShaderDir "build"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Vulkan Shader Generator for Android" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if llama.cpp exists
if (-not (Test-Path (Join-Path $ScriptDir "llama.cpp"))) {
    Write-Host "Error: llama.cpp directory not found!" -ForegroundColor Red
    Write-Host "   Please clone llama.cpp first:" -ForegroundColor Yellow
    Write-Host "   git clone https://github.com/ggerganov/llama.cpp.git" -ForegroundColor Yellow
    exit 1
}

# Check for CMake
$CMakeCmd = Get-Command cmake -ErrorAction SilentlyContinue
if (-not $CMakeCmd) {
    # Try to find CMake in Android Studio SDK
    $AndroidSdk = $env:ANDROID_SDK_ROOT
    if (-not $AndroidSdk) {
        $AndroidSdk = $env:ANDROID_HOME
    }
    if ($AndroidSdk) {
        $AndroidCMake = Get-ChildItem -Path (Join-Path $AndroidSdk "cmake") -Filter "cmake.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($AndroidCMake) {
            $env:PATH = "$($AndroidCMake.DirectoryName);$env:PATH"
            Write-Host "Found CMake in Android SDK: $($AndroidCMake.FullName)" -ForegroundColor Green
        }
    }
    
    # Check again
    $CMakeCmd = Get-Command cmake -ErrorAction SilentlyContinue
    if (-not $CMakeCmd) {
        Write-Host "Error: cmake not found. Please install CMake." -ForegroundColor Red
        Write-Host "   Option 1: Install standalone CMake from https://cmake.org/download/" -ForegroundColor Yellow
        Write-Host "   Option 2: Install via Android Studio SDK Manager" -ForegroundColor Yellow
        Write-Host "   Option 3: Add CMake to PATH manually" -ForegroundColor Yellow
        exit 1
    }
}

# Check for Vulkan SDK
if (-not (Get-Command glslc -ErrorAction SilentlyContinue)) {
    Write-Host "Warning: glslc not found in PATH" -ForegroundColor Yellow
    Write-Host "   You may need to install Vulkan SDK:" -ForegroundColor Yellow
    Write-Host "   https://vulkan.lunarg.com/sdk/home" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Building vulkan-shaders-gen..." -ForegroundColor Green
Write-Host "   Working directory: $ShaderDir"

# Create build directory
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
Set-Location $BuildDir

# Configure CMake
Write-Host ""
Write-Host "Configuring CMake..." -ForegroundColor Cyan
cmake .. -DCMAKE_BUILD_TYPE=Release

if ($LASTEXITCODE -ne 0) {
    Write-Host "CMake configuration failed!" -ForegroundColor Red
    Set-Location $ScriptDir
    exit 1
}

# Build shader generator
Write-Host ""
Write-Host "Building shader generator..." -ForegroundColor Cyan
cmake --build . --config Release

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Set-Location $ScriptDir
    exit 1
}

# Run shader generator
Write-Host ""
Write-Host "Generating Vulkan shader headers..." -ForegroundColor Cyan

# Find the shader generator executable
$ShaderGenExe = $null
$PossiblePaths = @(
    (Join-Path $BuildDir "Release\vulkan-shaders-gen.exe"),
    (Join-Path $BuildDir "vulkan-shaders-gen.exe"),
    (Join-Path $BuildDir "vulkan-shaders-gen")
)

foreach ($path in $PossiblePaths) {
    if (Test-Path $path) {
        $ShaderGenExe = $path
        break
    }
}

if ($null -eq $ShaderGenExe) {
    Write-Host "Error: vulkan-shaders-gen executable not found!" -ForegroundColor Red
    Write-Host "   Looked in: $BuildDir" -ForegroundColor Red
    Set-Location $ScriptDir
    exit 1
}

Write-Host "   Found: $ShaderGenExe" -ForegroundColor Gray
& $ShaderGenExe

if ($LASTEXITCODE -ne 0) {
    Write-Host "Shader generation failed!" -ForegroundColor Red
    Set-Location $ScriptDir
    exit 1
}

# Check if output was generated
$OutputFile = Join-Path $ShaderDir "vulkan-shaders-hpp.hpp"
if (Test-Path $OutputFile) {
    Write-Host ""
    Write-Host "Success! Vulkan shaders generated:" -ForegroundColor Green
    Write-Host "   -> $OutputFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now build your Android app with Vulkan support:" -ForegroundColor Yellow
    Write-Host "   1. Open app/build.gradle.kts" -ForegroundColor Yellow
    Write-Host "   2. In the cmake block, add:" -ForegroundColor Yellow
    Write-Host '      arguments += "-DLOCALLLM_ENABLE_VULKAN=ON"' -ForegroundColor Cyan
    Write-Host "   3. Rebuild the app with: .\gradlew assembleDebug" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "Error: Shader header file not generated!" -ForegroundColor Red
    Set-Location $ScriptDir
    exit 1
}

Write-Host "==========================================" -ForegroundColor Cyan

# Return to original directory
Set-Location $ScriptDir
