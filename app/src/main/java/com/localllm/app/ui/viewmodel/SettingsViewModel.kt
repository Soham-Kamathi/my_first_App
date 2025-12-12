package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.local.PreferencesDataStore
import com.localllm.app.data.model.AppTheme
import com.localllm.app.data.model.AppearanceStyle
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.StorageType
import com.localllm.app.data.model.UserPreferences
import com.localllm.app.data.model.WebSearchProvider
import com.localllm.app.domain.usecase.GetStorageStatsUseCase
import com.localllm.app.domain.usecase.StorageStats
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.HardwareCapabilities
import com.localllm.app.util.HardwareCapabilityDetector
import com.localllm.app.util.MemoryMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen.
 * Handles user preferences and app configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val modelManager: ModelManager,
    private val memoryMonitor: MemoryMonitor,
    private val hardwareCapabilities: HardwareCapabilities,
    private val getStorageStatsUseCase: GetStorageStatsUseCase,
    private val hardwareCapabilityDetector: HardwareCapabilityDetector
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesDataStore.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    private val _deviceSummary = MutableStateFlow("")
    val deviceSummary: StateFlow<String> = _deviceSummary.asStateFlow()

    private val _systemInfo = MutableStateFlow("")
    val systemInfo: StateFlow<String> = _systemInfo.asStateFlow()

    private val _hardwareProfile = MutableStateFlow<HardwareCapabilityDetector.HardwareProfile?>(null)
    val hardwareProfile: StateFlow<HardwareCapabilityDetector.HardwareProfile?> = _hardwareProfile.asStateFlow()

    init {
        loadDeviceInfo()
        loadStorageStats()
        loadHardwareProfile()
    }

    /**
     * Update the app theme.
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesDataStore.updateTheme(theme)
        }
    }

    /**
     * Update the appearance style (Default or Nothing OS inspired).
     */
    fun updateAppearanceStyle(style: AppearanceStyle) {
        viewModelScope.launch {
            preferencesDataStore.updateAppearanceStyle(style)
        }
    }

    /**
     * Update the default system prompt.
     */
    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            preferencesDataStore.updateSystemPrompt(prompt)
        }
    }

    /**
     * Update generation configuration.
     */
    fun updateGenerationConfig(config: GenerationConfig) {
        viewModelScope.launch {
            preferencesDataStore.updateGenerationConfig(config)
        }
    }

    /**
     * Update temperature setting.
     */
    fun updateTemperature(temperature: Float) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(temperature = temperature)
            )
        }
    }

    /**
     * Update max tokens setting.
     */
    fun updateMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(maxTokens = maxTokens)
            )
        }
    }

    /**
     * Update context size setting.
     */
    fun updateContextSize(contextSize: Int) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(contextSize = contextSize)
            )
        }
    }

    /**
     * Update top-p setting.
     */
    fun updateTopP(topP: Float) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(topP = topP)
            )
        }
    }

    /**
     * Update top-k setting.
     */
    fun updateTopK(topK: Int) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(topK = topK)
            )
        }
    }

    /**
     * Update repeat penalty setting.
     */
    fun updateRepeatPenalty(repeatPenalty: Float) {
        viewModelScope.launch {
            val current = userPreferences.value.defaultGenerationConfig
            preferencesDataStore.updateGenerationConfig(
                current.copy(repeatPenalty = repeatPenalty)
            )
        }
    }

    /**
     * Update thread count setting.
     */
    fun updateThreadCount(count: Int) {
        viewModelScope.launch {
            preferencesDataStore.updateThreadCount(count)
        }
    }

    /**
     * Update NNAPI enabled setting.
     */
    fun updateNNAPIEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateNNAPIEnabled(enabled)
        }
    }

    /**
     * Update mmap enabled setting.
     */
    fun updateMmapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateMmapEnabled(enabled)
        }
    }

    /**
     * Update preferred storage type.
     */
    fun updatePreferredStorage(storageType: StorageType) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferredStorage(storageType)
        }
    }

    /**
     * Update auto-load last model setting.
     */
    fun updateAutoLoadLastModel(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateAutoLoadLastModel(enabled)
        }
    }

    /**
     * Update show tokens per second setting.
     */
    fun updateShowTokensPerSecond(show: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateShowTokensPerSecond(show)
        }
    }

    /**
     * Update keep screen on setting.
     */
    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateKeepScreenOn(keepOn)
        }
    }

    /**
     * Update download on WiFi only setting.
     */
    fun updateDownloadOnWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateDownloadOnWifiOnly(wifiOnly)
        }
    }

    /**
     * Update thinking mode setting.
     */
    fun updateThinkingMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateThinkingMode(enabled)
        }
    }

    /**
     * Update web search setting.
     */
    fun updateWebSearch(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateWebSearch(enabled)
        }
    }

    /**
     * Update GPU acceleration enabled.
     */
    fun updateGpuAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateGpuAcceleration(enabled)
            // Set recommended GPU layers if enabling
            if (enabled && userPreferences.value.gpuLayers == 0) {
                _hardwareProfile.value?.let { profile ->
                    preferencesDataStore.updateGpuLayers(profile.recommendedGpuLayers)
                }
            }
        }
    }

    /**
     * Update GPU layers count.
     */
    fun updateGpuLayers(layers: Int) {
        viewModelScope.launch {
            preferencesDataStore.updateGpuLayers(layers)
        }
    }

    /**
     * Update Tavily API key for web search.
     */
    fun updateTavilyApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesDataStore.updateTavilyApiKey(apiKey)
        }
    }

    /**
     * Update web search provider preference.
     */
    fun updateWebSearchProvider(provider: WebSearchProvider) {
        viewModelScope.launch {
            preferencesDataStore.updateWebSearchProvider(provider)
        }
    }

    /**
     * Reset generation config to defaults.
     */
    fun resetGenerationConfig() {
        viewModelScope.launch {
            preferencesDataStore.updateGenerationConfig(GenerationConfig.BALANCED)
        }
    }

    /**
     * Apply creative preset.
     */
    fun applyCreativePreset() {
        viewModelScope.launch {
            preferencesDataStore.updateGenerationConfig(GenerationConfig.CREATIVE)
        }
    }

    /**
     * Apply precise preset.
     */
    fun applyPrecisePreset() {
        viewModelScope.launch {
            preferencesDataStore.updateGenerationConfig(GenerationConfig.PRECISE)
        }
    }

    /**
     * Apply code preset.
     */
    fun applyCodePreset() {
        viewModelScope.launch {
            preferencesDataStore.updateGenerationConfig(GenerationConfig.CODE)
        }
    }

    /**
     * Get available thread count options.
     */
    fun getThreadCountOptions(): List<Int> {
        val maxThreads = Runtime.getRuntime().availableProcessors()
        return (1..maxThreads).toList()
    }

    /**
     * Get optimal thread count recommendation.
     */
    fun getOptimalThreadCount(): Int {
        return hardwareCapabilities.getOptimalThreadCount()
    }

    /**
     * Check if NNAPI is supported.
     */
    fun isNNAPISupported(): Boolean {
        return hardwareCapabilities.supportsNNAPI()
    }

    /**
     * Get storage options.
     */
    fun getStorageOptions() = hardwareCapabilities.getStorageOptions()

    /**
     * Get memory info.
     */
    fun getMemoryInfo() = memoryMonitor.getDetailedMemoryInfo()

    private fun loadDeviceInfo() {
        _deviceSummary.value = hardwareCapabilities.getDeviceSummary()
        _systemInfo.value = modelManager.getSystemInfo()
    }

    private fun loadStorageStats() {
        viewModelScope.launch {
            _storageStats.value = getStorageStatsUseCase()
        }
    }

    private fun loadHardwareProfile() {
        viewModelScope.launch {
            _hardwareProfile.value = hardwareCapabilityDetector.detectHardwareProfile()
        }
    }
}
