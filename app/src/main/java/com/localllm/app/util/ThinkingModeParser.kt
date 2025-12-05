package com.localllm.app.util

/**
 * Parser for chain-of-thought (thinking mode) content in LLM responses.
 * Supports <think>...</think> tags for internal reasoning display.
 */
object ThinkingModeParser {
    
    /**
     * Parsed response containing thinking content and final response
     */
    data class ParsedResponse(
        val thinkingContent: String?,
        val responseContent: String,
        val hasThinking: Boolean
    )
    
    // Tags for thinking mode
    private val THINK_OPEN_TAG = "<think>"
    private val THINK_CLOSE_TAG = "</think>"
    private val ALT_THINK_OPEN_TAG = "<thinking>"
    private val ALT_THINK_CLOSE_TAG = "</thinking>"
    
    /**
     * Parse LLM response to extract thinking and response content
     */
    fun parse(content: String): ParsedResponse {
        var thinkingContent: String? = null
        var responseContent = content
        
        // Try standard <think> tags
        val thinkStart = content.indexOf(THINK_OPEN_TAG, ignoreCase = true)
        val thinkEnd = content.indexOf(THINK_CLOSE_TAG, ignoreCase = true)
        
        if (thinkStart != -1 && thinkEnd != -1 && thinkEnd > thinkStart) {
            thinkingContent = content.substring(thinkStart + THINK_OPEN_TAG.length, thinkEnd).trim()
            responseContent = (content.substring(0, thinkStart) + content.substring(thinkEnd + THINK_CLOSE_TAG.length)).trim()
        } else {
            // Try alternative <thinking> tags
            val altThinkStart = content.indexOf(ALT_THINK_OPEN_TAG, ignoreCase = true)
            val altThinkEnd = content.indexOf(ALT_THINK_CLOSE_TAG, ignoreCase = true)
            
            if (altThinkStart != -1 && altThinkEnd != -1 && altThinkEnd > altThinkStart) {
                thinkingContent = content.substring(altThinkStart + ALT_THINK_OPEN_TAG.length, altThinkEnd).trim()
                responseContent = (content.substring(0, altThinkStart) + content.substring(altThinkEnd + ALT_THINK_CLOSE_TAG.length)).trim()
            }
        }
        
        return ParsedResponse(
            thinkingContent = thinkingContent,
            responseContent = responseContent,
            hasThinking = thinkingContent != null
        )
    }
    
    /**
     * Extract multiple thinking blocks from content
     */
    fun parseMultiple(content: String): List<ParsedResponse> {
        val results = mutableListOf<ParsedResponse>()
        var remaining = content
        
        while (true) {
            val thinkStart = remaining.indexOf(THINK_OPEN_TAG, ignoreCase = true)
                .takeIf { it != -1 }
                ?: remaining.indexOf(ALT_THINK_OPEN_TAG, ignoreCase = true).takeIf { it != -1 }
                ?: break
            
            val isAlt = remaining.lowercase().indexOf(ALT_THINK_OPEN_TAG) == thinkStart
            val openTag = if (isAlt) ALT_THINK_OPEN_TAG else THINK_OPEN_TAG
            val closeTag = if (isAlt) ALT_THINK_CLOSE_TAG else THINK_CLOSE_TAG
            
            val thinkEnd = remaining.indexOf(closeTag, thinkStart, ignoreCase = true)
            if (thinkEnd == -1) break
            
            val thinkingContent = remaining.substring(thinkStart + openTag.length, thinkEnd).trim()
            val beforeThink = remaining.substring(0, thinkStart).trim()
            
            if (beforeThink.isNotEmpty()) {
                results.add(ParsedResponse(null, beforeThink, false))
            }
            
            results.add(ParsedResponse(thinkingContent, "", true))
            remaining = remaining.substring(thinkEnd + closeTag.length)
        }
        
        if (remaining.trim().isNotEmpty()) {
            results.add(ParsedResponse(null, remaining.trim(), false))
        }
        
        return results
    }
    
    /**
     * Check if content is currently in a thinking block (for streaming)
     */
    fun isInThinkingBlock(content: String): Boolean {
        val lastOpenThink = maxOf(
            content.lastIndexOf(THINK_OPEN_TAG, ignoreCase = true),
            content.lastIndexOf(ALT_THINK_OPEN_TAG, ignoreCase = true)
        )
        
        val lastCloseThink = maxOf(
            content.lastIndexOf(THINK_CLOSE_TAG, ignoreCase = true),
            content.lastIndexOf(ALT_THINK_CLOSE_TAG, ignoreCase = true)
        )
        
        return lastOpenThink > lastCloseThink
    }
    
    /**
     * Create a system prompt that enables thinking mode
     */
    fun createThinkingModePrompt(basePrompt: String): String {
        return """$basePrompt

When solving complex problems, you should think step by step. Use <think>...</think> tags to show your reasoning process before providing your final answer.

Example format:
<think>
Let me break this down...
First, I'll consider...
Then I'll analyze...
</think>

[Your final answer here]

This helps demonstrate your reasoning process clearly."""
    }
    
    /**
     * Strip thinking tags from content for clean display
     */
    fun stripThinkingTags(content: String): String {
        var result = content
        
        // Remove standard tags
        result = result.replace(Regex("<think>.*?</think>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        
        // Remove alternative tags
        result = result.replace(Regex("<thinking>.*?</thinking>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        
        return result.trim()
    }
}
