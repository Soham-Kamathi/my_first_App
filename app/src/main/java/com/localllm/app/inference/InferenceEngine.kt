package com.localllm.app.inference

import android.util.Log
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.GenerationResult
import com.localllm.app.data.model.MessageRole
import com.localllm.app.data.model.PromptTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Inference engine that handles text generation using loaded LLM models.
 */
@Singleton
class InferenceEngine @Inject constructor(
    private val llamaAndroid: LlamaAndroid,
    private val modelManager: ModelManager
) {
    companion object {
        private const val TAG = "InferenceEngine"
    }

    /**
     * Generate a response with streaming token output.
     *
     * @param prompt The input prompt
     * @param config Generation configuration
     * @param onTokenGenerated Callback for each generated token
     * @return Flow emitting the generation result
     */
    fun generateStream(
        prompt: String,
        config: GenerationConfig = GenerationConfig(),
        onTokenGenerated: (String) -> Unit = {}
    ): Flow<GenerationResult> = flow {
        val contextPtr = modelManager.getContextPtr()
            ?: throw IllegalStateException("No model loaded")

        val startTime = System.currentTimeMillis()
        var tokensGenerated = 0

        try {
            Log.d(TAG, "Starting generation with ${config.maxTokens} max tokens")

            val callback = object : LlamaAndroid.TokenCallback {
                override fun onToken(token: String) {
                    tokensGenerated++
                    onTokenGenerated(token)
                }
            }

            val result = withContext(Dispatchers.Default) {
                llamaAndroid.generateTokens(
                    ctxPtr = contextPtr,
                    prompt = prompt,
                    maxTokens = config.maxTokens,
                    temperature = config.temperature,
                    topP = config.topP,
                    topK = config.topK,
                    repeatPenalty = config.repeatPenalty,
                    callback = callback
                )
            }

            val generationTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Generation completed: $tokensGenerated tokens in ${generationTime}ms")

            emit(GenerationResult.Success(
                text = result,
                tokensGenerated = tokensGenerated,
                generationTimeMs = generationTime
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            emit(GenerationResult.Error(e.message ?: "Unknown error", e))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Generate a response without streaming (simpler API).
     *
     * @param prompt The input prompt
     * @param config Generation configuration
     * @return The generation result
     */
    suspend fun generate(
        prompt: String,
        config: GenerationConfig = GenerationConfig()
    ): GenerationResult = withContext(Dispatchers.Default) {
        val contextPtr = modelManager.getContextPtr()
            ?: return@withContext GenerationResult.Error("No model loaded")

        val startTime = System.currentTimeMillis()
        var tokensGenerated = 0

        try {
            val callback = object : LlamaAndroid.TokenCallback {
                override fun onToken(token: String) {
                    tokensGenerated++
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

            val generationTime = System.currentTimeMillis() - startTime

            GenerationResult.Success(
                text = result,
                tokensGenerated = tokensGenerated,
                generationTimeMs = generationTime
            )
        } catch (e: Exception) {
            GenerationResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Cancel ongoing generation.
     */
    fun cancelGeneration() {
        llamaAndroid.cancelGeneration()
        Log.d(TAG, "Generation cancelled")
    }

    /**
     * Check if generation is currently in progress.
     */
    fun isGenerating(): Boolean = llamaAndroid.isGenerating()

    /**
     * Build a prompt from conversation history.
     *
     * @param messages List of chat messages
     * @param systemPrompt Optional system prompt
     * @param promptTemplate The prompt template format to use
     * @return Formatted prompt string
     */
    fun buildPrompt(
        messages: List<ChatMessage>,
        systemPrompt: String? = null,
        promptTemplate: String = PromptTemplate.CHATML
    ): String {
        return when (promptTemplate) {
            PromptTemplate.CHATML -> buildChatMLPrompt(messages, systemPrompt)
            PromptTemplate.ALPACA -> buildAlpacaPrompt(messages, systemPrompt)
            PromptTemplate.LLAMA2 -> buildLlama2Prompt(messages, systemPrompt)
            PromptTemplate.MISTRAL -> buildMistralPrompt(messages, systemPrompt)
            PromptTemplate.VICUNA -> buildVicunaPrompt(messages, systemPrompt)
            PromptTemplate.ZEPHYR -> buildZephyrPrompt(messages, systemPrompt)
            PromptTemplate.PHI -> buildPhiPrompt(messages, systemPrompt)
            else -> buildRawPrompt(messages, systemPrompt)
        }
    }

    private fun buildChatMLPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<|im_start|>system\n")
            sb.append(systemPrompt)
            sb.append("<|im_end|>\n")
        }
        
        for (message in messages) {
            val role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            sb.append("<|im_start|>$role\n")
            sb.append(message.content)
            sb.append("<|im_end|>\n")
        }
        
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun buildAlpacaPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("Below is an instruction that describes a task. Write a response that appropriately completes the request.\n\n")
            sb.append("### System:\n$systemPrompt\n\n")
        }
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("### Instruction:\n${message.content}\n\n")
                MessageRole.ASSISTANT -> sb.append("### Response:\n${message.content}\n\n")
                MessageRole.SYSTEM -> {} // Handled above
            }
        }
        
        sb.append("### Response:\n")
        return sb.toString()
    }

    private fun buildLlama2Prompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        val system = systemPrompt ?: "You are a helpful assistant."
        sb.append("<s>[INST] <<SYS>>\n$system\n<</SYS>>\n\n")
        
        var isFirst = true
        for (i in messages.indices) {
            val message = messages[i]
            when (message.role) {
                MessageRole.USER -> {
                    if (isFirst) {
                        sb.append("${message.content} [/INST] ")
                        isFirst = false
                    } else {
                        sb.append("<s>[INST] ${message.content} [/INST] ")
                    }
                }
                MessageRole.ASSISTANT -> {
                    sb.append("${message.content} </s>")
                }
                MessageRole.SYSTEM -> {}
            }
        }
        
        return sb.toString()
    }

    private fun buildMistralPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        sb.append("<s>")
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("[INST] ${message.content} [/INST]")
                MessageRole.ASSISTANT -> sb.append("${message.content}</s>")
                MessageRole.SYSTEM -> {}
            }
        }
        
        return sb.toString()
    }

    private fun buildVicunaPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("$systemPrompt\n\n")
        }
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("USER: ${message.content}\n")
                MessageRole.ASSISTANT -> sb.append("ASSISTANT: ${message.content}\n")
                MessageRole.SYSTEM -> {}
            }
        }
        
        sb.append("ASSISTANT:")
        return sb.toString()
    }

    private fun buildZephyrPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<|system|>\n$systemPrompt</s>\n")
        }
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("<|user|>\n${message.content}</s>\n")
                MessageRole.ASSISTANT -> sb.append("<|assistant|>\n${message.content}</s>\n")
                MessageRole.SYSTEM -> {}
            }
        }
        
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    private fun buildPhiPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("Instruct: ${message.content}\n")
                MessageRole.ASSISTANT -> sb.append("Output: ${message.content}\n")
                MessageRole.SYSTEM -> sb.append("${message.content}\n")
            }
        }
        
        sb.append("Output:")
        return sb.toString()
    }

    private fun buildRawPrompt(messages: List<ChatMessage>, systemPrompt: String?): String {
        val sb = StringBuilder()
        
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("$systemPrompt\n\n")
        }
        
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> sb.append("User: ${message.content}\n")
                MessageRole.ASSISTANT -> sb.append("Assistant: ${message.content}\n")
                MessageRole.SYSTEM -> sb.append("${message.content}\n")
            }
        }
        
        sb.append("Assistant: ")
        return sb.toString()
    }
}
