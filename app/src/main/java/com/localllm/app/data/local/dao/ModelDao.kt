package com.localllm.app.data.local.dao

import androidx.room.*
import com.localllm.app.data.model.ModelInfo
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for model-related database operations.
 */
@Dao
interface ModelDao {

    @Query("SELECT * FROM models ORDER BY name ASC")
    fun getAllModels(): Flow<List<ModelInfo>>

    @Query("SELECT * FROM models WHERE isDownloaded = 1 ORDER BY downloadedDate DESC")
    fun getDownloadedModels(): Flow<List<ModelInfo>>

    @Query("SELECT * FROM models WHERE isDownloaded = 0 ORDER BY name ASC")
    fun getAvailableModels(): Flow<List<ModelInfo>>

    @Query("SELECT * FROM models WHERE id = :modelId")
    suspend fun getModelById(modelId: String): ModelInfo?

    @Query("SELECT * FROM models WHERE id = :modelId")
    fun getModelByIdFlow(modelId: String): Flow<ModelInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelInfo>)

    @Update
    suspend fun updateModel(model: ModelInfo)

    @Delete
    suspend fun deleteModel(model: ModelInfo)

    @Query("DELETE FROM models WHERE id = :modelId")
    suspend fun deleteModelById(modelId: String)

    @Query("UPDATE models SET isDownloaded = :isDownloaded, localPath = :localPath, downloadedDate = :downloadedDate WHERE id = :modelId")
    suspend fun updateDownloadStatus(
        modelId: String,
        isDownloaded: Boolean,
        localPath: String?,
        downloadedDate: Long?
    )

    @Query("SELECT * FROM models WHERE :tag IN (SELECT value FROM json_each(tags))")
    fun getModelsByTag(tag: String): Flow<List<ModelInfo>>

    @Query("SELECT * FROM models WHERE minRamMb <= :availableRamMb AND isDownloaded = 0 ORDER BY parameterCount DESC")
    fun getCompatibleModels(availableRamMb: Int): Flow<List<ModelInfo>>

    @Query("SELECT COUNT(*) FROM models WHERE isDownloaded = 1")
    suspend fun getDownloadedModelsCount(): Int

    @Query("SELECT SUM(fileSizeBytes) FROM models WHERE isDownloaded = 1")
    suspend fun getTotalDownloadedSize(): Long?
}
