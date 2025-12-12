package com.localllm.app.data.model

/**
 * User preferences for the app.
 */
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val appearanceStyle: AppearanceStyle = AppearanceStyle.DEFAULT,
    val defaultModelId: String? = null,
    val defaultSystemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val defaultGenerationConfig: GenerationConfig = GenerationConfig.BALANCED,
    val preferredStorageType: StorageType = StorageType.INTERNAL,
    val autoLoadLastModel: Boolean = true,
    val showTokensPerSecond: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val keepScreenOnDuringGeneration: Boolean = true,
    val maxConcurrentDownloads: Int = 1,
    val downloadOnWifiOnly: Boolean = true,
    val threadCount: Int = 0, // 0 means auto-detect
    val useNNAPI: Boolean = true,
    val useMmap: Boolean = true,
    val contextCacheEnabled: Boolean = true,
    // GPU/Hardware Acceleration
    val gpuAccelerationEnabled: Boolean = false, // Enable GPU offloading (Vulkan)
    val gpuLayers: Int = 0,                      // Number of layers on GPU (0=auto)
    // New AI features
    val thinkingModeEnabled: Boolean = false,  // Chain-of-thought visualization
    val webSearchEnabled: Boolean = false,      // Web search before LLM response
    // Web Search API Configuration
    val tavilyApiKey: String = "",              // Tavily API key for web search
    val webSearchProvider: WebSearchProvider = WebSearchProvider.AUTO  // Preferred search provider
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant. " +
                "You provide accurate, helpful, and friendly responses to user queries. " +
                "If you don't know something, admit it rather than making up information."
    }
}

/**
 * App theme options.
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Appearance style options - affects overall visual design language.
 * DEFAULT: Modern cyan/teal premium AI interface
 * NOTHING: Minimalist black/white/red inspired by Nothing OS design language
 */
enum class AppearanceStyle {
    DEFAULT,
    NOTHING
}

/**
 * Inference settings that can be adjusted per-session.
 */
data class InferenceSettings(
    val threadCount: Int,
    val useNNAPI: Boolean,
    val useMmap: Boolean,
    val batchSize: Int = 512,
    val gpuLayers: Int = 0
)

/**
 * Web search provider options
 */
enum class WebSearchProvider {
    AUTO,     // Automatically choose best available (Tavily if key present, else fallbacks)
    TAVILY,   // Tavily API (requires API key)
    DUCKDUCKGO, // DuckDuckGo (no API key needed)
    WIKIPEDIA  // Wikipedia only
}
