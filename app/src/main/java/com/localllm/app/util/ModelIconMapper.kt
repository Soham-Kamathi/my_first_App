package com.localllm.app.util

import com.localllm.app.R

/**
 * Utility object for mapping model names to their corresponding icon resources.
 */
object ModelIconMapper {
    
    /**
     * Map of model keywords to drawable resource IDs.
     */
    private val iconMap = mapOf(
        // Specific models
        "llama" to R.drawable.llama,
        "mistral" to R.drawable.mistralai,
        "deepseek" to R.drawable.deepseek,
        "qwen" to R.drawable.qwen2,
        "gemma" to R.drawable.gemma,
        "gemini" to R.drawable.gemini,
        "phi" to R.drawable.phi_2,
        "claude" to R.drawable.claude,
        "gpt" to R.drawable.gpt,
        "grok" to R.drawable.grok,
        "perplexity" to R.drawable.perplexityai,
        
        // Use cases/types
        "code" to R.drawable.code_completion,
        "starcoder" to R.drawable.code_completion,
        "codellama" to R.drawable.code_completion,
        "document" to R.drawable.document_chat,
        "chat" to R.drawable.ai_chat,
        "prompt" to R.drawable.prompt_lab
    )
    
    /**
     * Get the icon resource ID for a given model name or ID.
     * Returns a default chat icon if no specific match is found.
     * 
     * @param modelNameOrId The model name, ID, or description
     * @return The drawable resource ID for the model icon
     */
    fun getIconForModel(modelNameOrId: String): Int {
        val normalizedName = modelNameOrId.lowercase()
        
        // Try to find exact match or partial match
        for ((keyword, iconRes) in iconMap) {
            if (normalizedName.contains(keyword)) {
                return iconRes
            }
        }
        
        // Default icon
        return R.drawable.ai_chat
    }
    
    /**
     * Get icon based on model family/author.
     * 
     * @param author The model author/organization
     * @param modelName The model name
     * @return The drawable resource ID for the model icon
     */
    fun getIconForModelByAuthor(author: String?, modelName: String): Int {
        author?.lowercase()?.let { authorLower ->
            return when {
                authorLower.contains("meta") || authorLower.contains("facebook") -> R.drawable.llama
                authorLower.contains("mistral") -> R.drawable.mistralai
                authorLower.contains("deepseek") -> R.drawable.deepseek
                authorLower.contains("qwen") || authorLower.contains("alibaba") -> R.drawable.qwen2
                authorLower.contains("google") -> {
                    if (modelName.lowercase().contains("gemma")) R.drawable.gemma
                    else R.drawable.gemini
                }
                authorLower.contains("microsoft") -> R.drawable.phi_2
                authorLower.contains("anthropic") -> R.drawable.claude
                authorLower.contains("openai") -> R.drawable.gpt
                authorLower.contains("xai") -> R.drawable.grok
                authorLower.contains("perplexity") -> R.drawable.perplexityai
                else -> getIconForModel(modelName)
            }
        }
        
        return getIconForModel(modelName)
    }
    
    /**
     * Get a descriptive label for the model based on its characteristics.
     * 
     * @param tags List of model tags
     * @param pipelineTag The pipeline tag
     * @return A user-friendly description
     */
    fun getModelTypeDescription(tags: List<String>, pipelineTag: String?): String {
        return when {
            tags.any { it.contains("code", ignoreCase = true) } -> "Code Assistant"
            tags.any { it.contains("chat", ignoreCase = true) || it.contains("conversational", ignoreCase = true) } -> "Chat Model"
            tags.any { it.contains("instruct", ignoreCase = true) } -> "Instruction Following"
            pipelineTag == "text-generation" -> "Text Generation"
            pipelineTag == "conversational" -> "Conversational AI"
            else -> "Language Model"
        }
    }
    
    /**
     * Extract model family from name or tags.
     * 
     * @param modelName The model name
     * @param tags List of model tags
     * @return The model family name
     */
    fun getModelFamily(modelName: String, tags: List<String>): String {
        val normalized = modelName.lowercase()
        
        return when {
            normalized.contains("llama") -> "Llama"
            normalized.contains("mistral") -> "Mistral"
            normalized.contains("deepseek") -> "DeepSeek"
            normalized.contains("qwen") -> "Qwen"
            normalized.contains("gemma") -> "Gemma"
            normalized.contains("phi") -> "Phi"
            normalized.contains("tinyllama") -> "TinyLlama"
            normalized.contains("smollm") -> "SmolLM"
            normalized.contains("starcoder") -> "StarCoder"
            tags.any { it.contains("llama", ignoreCase = true) } -> "Llama"
            tags.any { it.contains("mistral", ignoreCase = true) } -> "Mistral"
            else -> "Other"
        }
    }
}
