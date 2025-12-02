package com.localllm.app.domain.usecase

import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all models.
 */
class GetAllModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(): Flow<List<ModelInfo>> = modelRepository.getAllModels()
}

/**
 * Use case for getting downloaded models only.
 */
class GetDownloadedModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(): Flow<List<ModelInfo>> = modelRepository.getDownloadedModels()
}

/**
 * Use case for getting available (not downloaded) models.
 */
class GetAvailableModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(): Flow<List<ModelInfo>> = modelRepository.getAvailableModels()
}

/**
 * Use case for getting models compatible with available RAM.
 */
class GetCompatibleModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(availableRamMb: Int): Flow<List<ModelInfo>> = 
        modelRepository.getCompatibleModels(availableRamMb)
}

/**
 * Use case for getting a specific model by ID.
 */
class GetModelByIdUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(modelId: String): ModelInfo? = 
        modelRepository.getModelById(modelId)
}

/**
 * Use case for refreshing the model catalog from remote.
 */
class RefreshModelCatalogUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(): Result<Unit> = modelRepository.refreshModelCatalog()
}

/**
 * Use case for deleting a downloaded model.
 */
class DeleteDownloadedModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(modelId: String) {
        modelRepository.deleteDownloadedModel(modelId)
    }
}

/**
 * Use case for getting storage statistics.
 */
class GetStorageStatsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(): StorageStats {
        return StorageStats(
            downloadedModelsCount = modelRepository.getDownloadedModelsCount(),
            totalDownloadedSizeBytes = modelRepository.getTotalDownloadedSize()
        )
    }
}

data class StorageStats(
    val downloadedModelsCount: Int,
    val totalDownloadedSizeBytes: Long
) {
    fun formattedSize(): String {
        return when {
            totalDownloadedSizeBytes >= 1_073_741_824 -> 
                String.format("%.2f GB", totalDownloadedSizeBytes / 1_073_741_824.0)
            totalDownloadedSizeBytes >= 1_048_576 -> 
                String.format("%.1f MB", totalDownloadedSizeBytes / 1_048_576.0)
            else -> 
                String.format("%.1f KB", totalDownloadedSizeBytes / 1024.0)
        }
    }
}
