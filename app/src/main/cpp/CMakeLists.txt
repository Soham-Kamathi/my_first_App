# CMakeLists.txt for Local LLM Android App
cmake_minimum_required(VERSION 3.22.1)
project(localllm LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Android-specific settings
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -fPIC")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG -fPIC")

# 16KB page size alignment for Android 15+ compatibility
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# llama.cpp configuration
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
set(GGML_DIR ${LLAMA_CPP_DIR}/ggml)

# Check if llama.cpp exists
if(NOT EXISTS "${LLAMA_CPP_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp not found. Please clone: git clone https://github.com/ggerganov/llama.cpp.git")
endif()

# GGML options for Android
set(GGML_STATIC ON CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_LTO OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)

# Disable GPU backends not available on Android (or optional)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_HIPBLAS OFF CACHE BOOL "" FORCE)
set(GGML_SYCL OFF CACHE BOOL "" FORCE)
set(GGML_KOMPUTE OFF CACHE BOOL "" FORCE)
set(GGML_RPC OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)

# Build llama.cpp as static library
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

# Add llama.cpp subdirectory
add_subdirectory(${LLAMA_CPP_DIR} llama.cpp.build)

# Find required Android libraries
find_library(log-lib log)
find_library(android-lib android)

# JNI bridge source (no external common files)
add_library(localllm SHARED
    llama_jni.cpp
)

# Include directories
target_include_directories(localllm PRIVATE
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/common
    ${LLAMA_CPP_DIR}/src
    ${GGML_DIR}/include
    ${GGML_DIR}/src
)

# Link libraries
target_link_libraries(localllm
    llama
    ggml
    ${log-lib}
    ${android-lib}
)

# Compiler definitions
target_compile_definitions(localllm PRIVATE
    GGML_USE_CPU
)

# 16KB page alignment for Android 15+ devices
set_target_properties(localllm PROPERTIES
    LINK_FLAGS "-Wl,-z,max-page-size=16384"
)

# ARM-specific optimizations
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_options(localllm PRIVATE
        -march=armv8-a+fp+simd+dotprod
    )
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_options(localllm PRIVATE
        -mfpu=neon-vfpv4
        -mfloat-abi=softfp
    )
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    target_compile_options(localllm PRIVATE
        -msse3
        -mssse3
    )
elseif(${ANDROID_ABI} STREQUAL "x86")
    target_compile_options(localllm PRIVATE
        -msse3
    )
endif()
