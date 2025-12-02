package com.localllm.app.data.model

/**
 * Configuration for text generation.
 */
data class GenerationConfig(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val contextSize: Int = 2048,
    val stopSequences: List<String> = emptyList(),
    val seed: Int = -1 // -1 means random seed
) {
    companion object {
        /**
         * Default configuration for creative writing.
         */
        val CREATIVE = GenerationConfig(
            temperature = 0.9f,
            topP = 0.95f,
            repeatPenalty = 1.05f
        )

        /**
         * Default configuration for precise/factual responses.
         */
        val PRECISE = GenerationConfig(
            temperature = 0.3f,
            topP = 0.8f,
            repeatPenalty = 1.2f
        )

        /**
         * Balanced configuration (default).
         */
        val BALANCED = GenerationConfig()

        /**
         * Configuration for code generation.
         */
        val CODE = GenerationConfig(
            temperature = 0.4f,
            topP = 0.85f,
            repeatPenalty = 1.15f,
            maxTokens = 1024
        )
    }
}

/**
 * Represents the result of a generation operation.
 */
sealed class GenerationResult {
    data class Success(
        val text: String,
        val tokensGenerated: Int,
        val generationTimeMs: Long
    ) : GenerationResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : GenerationResult()

    data object Cancelled : GenerationResult()
}

/**
 * Represents the current state of the generation process.
 */
sealed class GenerationState {
    data object Idle : GenerationState()
    data object Loading : GenerationState()
    data class Generating(
        val tokensGenerated: Int,
        val tokensPerSecond: Double
    ) : GenerationState()
    data class Complete(val result: GenerationResult) : GenerationState()
}
