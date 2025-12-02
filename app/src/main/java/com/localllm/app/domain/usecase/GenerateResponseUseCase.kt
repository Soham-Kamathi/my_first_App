package com.localllm.app.domain.usecase

import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateResponseUseCase @Inject constructor(
    private val inferenceEngine: InferenceEngine
) {
    operator fun invoke(
        prompt: String,
        config: GenerationConfig = GenerationConfig(),
        onTokenGenerated: (String) -> Unit = {}
    ): Flow<GenerationResult> {
        return inferenceEngine.generateStream(prompt, config, onTokenGenerated)
    }
    
    fun stop() {
        inferenceEngine.cancelGeneration()
    }
}
