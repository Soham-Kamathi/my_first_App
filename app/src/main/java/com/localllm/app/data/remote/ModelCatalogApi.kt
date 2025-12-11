package com.localllm.app.data.remote

import com.localllm.app.data.model.ModelInfo
import retrofit2.http.GET

/**
 * API interface for fetching the model catalog.
 */
interface ModelCatalogApi {
    
    @GET("catalog.json")
    suspend fun getModelCatalog(): ModelCatalogResponse
}

/**
 * Response wrapper for the model catalog.
 */
data class ModelCatalogResponse(
    val version: Int,
    val lastUpdated: String,
    val models: List<ModelInfo>
)

/**
 * Default model catalog for when remote fetch fails.
 * These are curated models known to work well on mobile devices.
 */
object DefaultModelCatalog {
    val models = listOf(
        ModelInfo(
            id = "qwen2.5-0.5b-instruct-q4",
            name = "Qwen 2.5 0.5B Instruct (Q4)",
            parameterCount = "0.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 400_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            minRamMb = 1024,
            recommendedRamMb = 2048,
            description = "Alibaba's smallest Qwen 2.5 model. Very fast on mobile.",
            license = "Apache-2.0",
            tags = listOf("qwen", "tiny", "fast", "instruct"),
            promptTemplate = "chatml",
            contextLength = 32768
        ),
        ModelInfo(
            id = "qwen2.5-1.5b-instruct-q4",
            name = "Qwen 2.5 1.5B Instruct (Q4)",
            parameterCount = "1.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_000_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            minRamMb = 2048,
            recommendedRamMb = 3072,
            description = "Alibaba's Qwen 2.5 1.5B model. Good quality for small size.",
            license = "Apache-2.0",
            tags = listOf("qwen", "small", "instruct"),
            promptTemplate = "chatml",
            contextLength = 32768
        ),
        ModelInfo(
            id = "llama-3.2-1b-instruct-q4",
            name = "Llama 3.2 1B Instruct (Q4)",
            parameterCount = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 750_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            minRamMb = 1536,
            recommendedRamMb = 2560,
            description = "Meta's latest Llama 3.2 1B model. Excellent for mobile.",
            license = "llama3.2",
            tags = listOf("llama", "meta", "small", "instruct"),
            promptTemplate = "llama3",
            contextLength = 131072
        ),
        ModelInfo(
            id = "llama-3.2-3b-instruct-q4",
            name = "Llama 3.2 3B Instruct (Q4)",
            parameterCount = "3B",
            quantization = "Q4_K_M",
            fileSizeBytes = 2_000_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            minRamMb = 3072,
            recommendedRamMb = 4096,
            description = "Meta's Llama 3.2 3B model. Great balance of quality and speed.",
            license = "llama3.2",
            tags = listOf("llama", "meta", "medium", "instruct"),
            promptTemplate = "llama3",
            contextLength = 131072
        ),
        ModelInfo(
            id = "smollm2-1.7b-instruct-q4",
            name = "SmolLM2 1.7B Instruct (Q4)",
            parameterCount = "1.7B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_100_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-1.7B-Instruct-GGUF/resolve/main/SmolLM2-1.7B-Instruct-Q4_K_M.gguf",
            minRamMb = 2048,
            recommendedRamMb = 3072,
            description = "HuggingFace's SmolLM2 - compact and capable.",
            license = "Apache-2.0",
            tags = listOf("smollm", "huggingface", "small", "instruct"),
            promptTemplate = "chatml",
            contextLength = 8192
        ),
        ModelInfo(
            id = "gemma-2-2b-it-q4",
            name = "Gemma 2 2B IT (Q4)",
            parameterCount = "2B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_600_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            minRamMb = 2560,
            recommendedRamMb = 4096,
            description = "Google's Gemma 2 2B instruction-tuned model.",
            license = "gemma",
            tags = listOf("gemma", "google", "small", "instruct"),
            promptTemplate = "gemma",
            contextLength = 8192
        ),
        ModelInfo(
            id = "phi-3-mini-4k-instruct-q4",
            name = "Phi-3 Mini 4K Instruct (Q4)",
            parameterCount = "3.8B",
            quantization = "Q4_K_M",
            fileSizeBytes = 2_300_000_000L,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
            minRamMb = 3584,
            recommendedRamMb = 5120,
            description = "Microsoft's Phi-3 Mini with excellent reasoning.",
            license = "MIT",
            tags = listOf("phi", "microsoft", "reasoning", "instruct"),
            promptTemplate = "phi3",
            contextLength = 4096
        ),
        ModelInfo(
            id = "tinyllama-1.1b-chat-q4",
            name = "TinyLlama 1.1B Chat (Q4)",
            parameterCount = "1.1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 670_000_000L,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            minRamMb = 1536,
            recommendedRamMb = 2560,
            description = "Compact and fast model suitable for basic conversations.",
            license = "Apache-2.0",
            tags = listOf("tiny", "fast", "chat"),
            promptTemplate = "chatml",
            contextLength = 2048
        ),
        
        // ========== VISION MODELS ==========
        ModelInfo(
            id = "moondream2-q4",
            name = "Moondream2 (Q4) 🖼️",
            parameterCount = "1.9B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_200_000_000L,
            downloadUrl = "https://huggingface.co/vikhyatk/moondream2-gguf/resolve/main/moondream2-text-model-q4_k_m.gguf",
            minRamMb = 2048,
            recommendedRamMb = 3072,
            description = "Tiny but capable vision model. Describe images, answer questions about photos.",
            license = "Apache-2.0",
            tags = listOf("vision", "multimodal", "tiny", "moondream"),
            promptTemplate = "moondream",
            contextLength = 2048,
            supportsVision = true,
            visionProjectorUrl = "https://huggingface.co/vikhyatk/moondream2-gguf/resolve/main/moondream2-mmproj-f16.gguf"
        ),
        ModelInfo(
            id = "llava-phi3-mini-q4",
            name = "LLaVA-Phi3 Mini (Q4) 🖼️",
            parameterCount = "4.2B",
            quantization = "Q4_K_M",
            fileSizeBytes = 2_500_000_000L,
            downloadUrl = "https://huggingface.co/xtuner/llava-phi-3-mini-gguf/resolve/main/llava-phi-3-mini-Q4_K_M.gguf",
            minRamMb = 4096,
            recommendedRamMb = 6144,
            description = "Vision model based on Phi-3. Understand images and answer visual questions.",
            license = "MIT",
            tags = listOf("vision", "multimodal", "llava", "phi"),
            promptTemplate = "llava",
            contextLength = 4096,
            supportsVision = true,
            visionProjectorUrl = "https://huggingface.co/xtuner/llava-phi-3-mini-gguf/resolve/main/llava-phi-3-mini-mmproj-f16.gguf"
        ),
        ModelInfo(
            id = "smolvlm-instruct-q4",
            name = "SmolVLM Instruct (Q4) 🖼️",
            parameterCount = "2B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_300_000_000L,
            downloadUrl = "https://huggingface.co/mradermacher/SmolVLM-Instruct-GGUF/resolve/main/SmolVLM-Instruct.Q4_K_M.gguf",
            minRamMb = 2560,
            recommendedRamMb = 4096,
            description = "HuggingFace's compact vision-language model. Fast and efficient for mobile.",
            license = "Apache-2.0",
            tags = listOf("vision", "multimodal", "smolvlm", "small"),
            promptTemplate = "smolvlm",
            contextLength = 4096,
            supportsVision = true,
            visionProjectorUrl = null // Integrated projector
        ),
        ModelInfo(
            id = "nanollava-q4",
            name = "NanoLLaVA (Q4) 🖼️",
            parameterCount = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 800_000_000L,
            downloadUrl = "https://huggingface.co/qnguyen3/nanoLLaVA-gguf/resolve/main/nanoLLaVA-Q4_K_M.gguf",
            minRamMb = 1536,
            recommendedRamMb = 2560,
            description = "Ultra-compact vision model. Basic image understanding in tiny footprint.",
            license = "Apache-2.0",
            tags = listOf("vision", "multimodal", "nano", "tiny", "llava"),
            promptTemplate = "llava",
            contextLength = 2048,
            supportsVision = true,
            visionProjectorUrl = "https://huggingface.co/qnguyen3/nanoLLaVA-gguf/resolve/main/nanoLLaVA-mmproj-f16.gguf"
        ),
        
        // ========== WHISPER MODELS (AUDIO TRANSCRIPTION) ==========
        ModelInfo(
            id = "whisper-tiny-q5",
            name = "Whisper Tiny (Q5) 🎙️",
            parameterCount = "39M",
            quantization = "Q5_K_M",
            fileSizeBytes = 45_000_000L,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_0.bin",
            minRamMb = 256,
            recommendedRamMb = 512,
            description = "Ultra-fast audio transcription. Supports 99 languages. Great for quick transcriptions.",
            license = "MIT",
            tags = listOf("whisper", "audio", "transcription", "tiny", "multilingual"),
            promptTemplate = "raw",
            contextLength = 0,
            modelType = "audio-transcription",
            isWhisper = true
        ),
        ModelInfo(
            id = "whisper-base-q5",
            name = "Whisper Base (Q5) 🎙️",
            parameterCount = "74M",
            quantization = "Q5_K_M",
            fileSizeBytes = 80_000_000L,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_0.bin",
            minRamMb = 512,
            recommendedRamMb = 768,
            description = "Balanced speed and accuracy. Supports 99 languages. Good for most use cases.",
            license = "MIT",
            tags = listOf("whisper", "audio", "transcription", "base", "multilingual"),
            promptTemplate = "raw",
            contextLength = 0,
            modelType = "audio-transcription",
            isWhisper = true
        ),
        ModelInfo(
            id = "whisper-small-q5",
            name = "Whisper Small (Q5) 🎙️",
            parameterCount = "244M",
            quantization = "Q5_K_M",
            fileSizeBytes = 260_000_000L,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_0.bin",
            minRamMb = 1024,
            recommendedRamMb = 1536,
            description = "Higher accuracy transcription. Supports 99 languages. Better for noisy audio.",
            license = "MIT",
            tags = listOf("whisper", "audio", "transcription", "small", "multilingual"),
            promptTemplate = "raw",
            contextLength = 0,
            modelType = "audio-transcription",
            isWhisper = true
        ),
        ModelInfo(
            id = "whisper-medium-q5",
            name = "Whisper Medium (Q5) 🎙️",
            parameterCount = "769M",
            quantization = "Q5_K_M",
            fileSizeBytes = 850_000_000L,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
            minRamMb = 2048,
            recommendedRamMb = 3072,
            description = "High-quality transcription. Supports 99 languages. Best for accurate results.",
            license = "MIT",
            tags = listOf("whisper", "audio", "transcription", "medium", "multilingual"),
            promptTemplate = "raw",
            contextLength = 0,
            modelType = "audio-transcription",
            isWhisper = true
        )
    )
}
