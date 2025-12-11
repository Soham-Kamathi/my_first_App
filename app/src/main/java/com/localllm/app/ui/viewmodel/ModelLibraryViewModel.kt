package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.DeviceInfo
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.model.StorageType
import com.localllm.app.data.repository.ModelRepository
import com.localllm.app.domain.usecase.DeleteDownloadedModelUseCase
import com.localllm.app.domain.usecase.GetAvailableModelsUseCase
import com.localllm.app.domain.usecase.GetDownloadedModelsUseCase
import com.localllm.app.domain.usecase.GetStorageStatsUseCase
import com.localllm.app.domain.usecase.RefreshModelCatalogUseCase
import com.localllm.app.domain.usecase.StorageStats
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.HardwareCapabilities
import com.localllm.app.util.MemoryMonitor
import com.localllm.app.util.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the model library screen.
 * Handles model discovery, downloading, and loading.
 */
@HiltViewModel
class ModelLibraryViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val modelDownloader: ModelDownloader,
    private val modelManager: ModelManager,
    private val memoryMonitor: MemoryMonitor,
    private val hardwareCapabilities: HardwareCapabilities,
    private val getDownloadedModelsUseCase: GetDownloadedModelsUseCase,
    private val getAvailableModelsUseCase: GetAvailableModelsUseCase,
    private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
    private val deleteDownloadedModelUseCase: DeleteDownloadedModelUseCase,
    private val getStorageStatsUseCase: GetStorageStatsUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedTab = MutableStateFlow(ModelLibraryTab.DOWNLOADED)
    val selectedTab: StateFlow<ModelLibraryTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    private var downloadJobs = mutableMapOf<String, Job>()

    val downloadedModels: StateFlow<List<ModelInfo>> = getDownloadedModelsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableModels: StateFlow<List<ModelInfo>> = getAvailableModelsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredDownloadedModels: StateFlow<List<ModelInfo>> = combine(
        downloadedModels,
        searchQuery,
        sortOption,
        filterOption
    ) { models, query, sort, filter ->
        applyFiltersAndSort(models, query, sort, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredAvailableModels: StateFlow<List<ModelInfo>> = combine(
        availableModels,
        searchQuery,
        sortOption,
        filterOption
    ) { models, query, sort, filter ->
        applyFiltersAndSort(models, query, sort, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadStates: StateFlow<Map<String, DownloadState>> = modelDownloader.downloadStates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val modelLoadingState: StateFlow<ModelLoadingState> = modelManager.loadingState

    val currentModel: StateFlow<ModelInfo?> = modelManager.currentModel

    val deviceInfo: StateFlow<DeviceInfo> = MutableStateFlow(hardwareCapabilities.getDeviceInfo())
        .asStateFlow()

    init {
        refreshCatalog()
        loadStorageStats()
    }

    /**
     * Set the selected tab.
     */
    fun setSelectedTab(tab: ModelLibraryTab) {
        _selectedTab.value = tab
    }

    /**
     * Update search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Set sort option.
     */
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    /**
     * Set filter option.
     */
    fun setFilterOption(option: FilterOption) {
        _filterOption.value = option
    }

    /**
     * Refresh the model catalog from remote.
     */
    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            
            refreshModelCatalogUseCase().fold(
                onSuccess = {
                    // Catalog refreshed successfully
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to refresh catalog: ${error.message}"
                }
            )
            
            _isRefreshing.value = false
        }
    }

    /**
     * Download a model.
     */
    fun downloadModel(model: ModelInfo, storageType: StorageType = StorageType.INTERNAL) {
        // Cancel any existing download for this model
        downloadJobs[model.id]?.cancel()
        
        val job = viewModelScope.launch {
            modelDownloader.downloadModel(
                modelInfo = model,
                storageType = storageType
            ) { state ->
                // Progress updates handled via downloadStates flow
            }.fold(
                onSuccess = { localPath ->
                    // Update model in database with download info
                    modelRepository.updateDownloadStatus(
                        modelId = model.id,
                        isDownloaded = true,
                        localPath = localPath,
                        downloadedDate = System.currentTimeMillis()
                    )
                    loadStorageStats()
                },
                onFailure = { error ->
                    _errorMessage.value = "Download failed: ${error.message}"
                }
            )
        }
        
        downloadJobs[model.id] = job
    }

    /**
     * Cancel an ongoing download.
     */
    fun cancelDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        modelDownloader.cancelDownload(modelId)
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            // Unload if currently loaded
            if (modelManager.currentModelId == model.id) {
                modelManager.unloadModel()
            }
            
            // Delete file
            modelDownloader.deleteModel(model.id)
            
            // Update database
            deleteDownloadedModelUseCase(model.id)
            
            loadStorageStats()
        }
    }

    /**
     * Load a model for inference.
     */
    fun loadModel(model: ModelInfo) {
        if (model.localPath == null) {
            _errorMessage.value = "Model not downloaded"
            return
        }
        
        viewModelScope.launch {
            val memoryInfo = memoryMonitor.getDetailedMemoryInfo()
            
            // Warn if memory is low but don't block - mmap handles memory efficiently
            if (memoryInfo.availableMb < model.minRamMb) {
                android.util.Log.w("ModelLibraryViewModel", 
                    "Low memory warning: Need ${model.minRamMb}MB, have ${memoryInfo.availableMb}MB. Attempting anyway with mmap.")
            }
            
            modelManager.loadModel(
                model = model,
                threads = hardwareCapabilities.getOptimalThreadCount(),
                contextSize = model.contextLength,
                useMmap = true,
                useNNAPI = hardwareCapabilities.supportsNNAPI()
            ).fold(
                onSuccess = {
                    // Model loaded successfully
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to load model: ${error.message}"
                }
            )
        }
    }

    /**
     * Unload the currently loaded model.
     */
    fun unloadModel() {
        viewModelScope.launch {
            modelManager.unloadModel()
        }
    }

    /**
     * Check if a model can run on this device based on RAM.
     * For downloaded models, we're more lenient since mmap doesn't require all memory upfront.
     */
    fun canRunModel(model: ModelInfo): Boolean {
        // If model is downloaded, always allow attempting to load
        // mmap will handle memory efficiently
        if (model.isDownloaded) {
            return true
        }
        return memoryMonitor.canLoadModel(model.minRamMb)
    }

    /**
     * Get recommended models for this device.
     */
    fun getRecommendedModels(): List<ModelInfo> {
        val deviceInfo = hardwareCapabilities.getDeviceInfo()
        val recommendedSizes = deviceInfo.recommendedModelSizes()
        
        return availableModels.value.filter { model ->
            recommendedSizes.any { size -> model.parameterCount.contains(size) } &&
                deviceInfo.availableRamMb >= model.minRamMb
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Apply search, filter, and sort to a list of models.
     */
    private fun applyFiltersAndSort(
        models: List<ModelInfo>,
        query: String,
        sort: SortOption,
        filter: FilterOption
    ): List<ModelInfo> {
        var filtered = models

        // Apply search filter
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { model ->
                model.name.lowercase().contains(lowerQuery) ||
                model.id.lowercase().contains(lowerQuery) ||
                model.author?.lowercase()?.contains(lowerQuery) == true ||
                model.description.lowercase().contains(lowerQuery)
            }
        }

        // Apply capability filter
        filtered = when (filter) {
            FilterOption.ALL -> filtered
            FilterOption.TEXT_ONLY -> filtered.filter { !it.supportsVision }
            FilterOption.VISION -> filtered.filter { it.supportsVision }
        }

        // Apply sort
        filtered = when (sort) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.fileSizeBytes ?: Long.MAX_VALUE }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes ?: 0L }
            SortOption.DOWNLOADS -> filtered.sortedByDescending { it.downloads ?: 0 }
            SortOption.LIKES -> filtered.sortedByDescending { it.likes ?: 0 }
        }

        return filtered
    }

    /**
     * Load storage statistics.
     */
    private fun loadStorageStats() {
        viewModelScope.launch {
            _storageStats.value = getStorageStatsUseCase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadJobs.values.forEach { it.cancel() }
    }
}

/**
 * Tabs for the model library screen.
 */
enum class ModelLibraryTab {
    DOWNLOADED,
    AVAILABLE
}

/**
 * Sort options for model list.
 */
enum class SortOption(val displayName: String) {
    NAME("Name (A-Z)"),
    SIZE_ASC("Size (Small to Large)"),
    SIZE_DESC("Size (Large to Small)"),
    DOWNLOADS("Most Downloads"),
    LIKES("Most Likes")
}

/**
 * Filter options for model list.
 */
enum class FilterOption(val displayName: String) {
    ALL("All Models"),
    TEXT_ONLY("Text-Only Models"),
    VISION("Vision Models")
}
