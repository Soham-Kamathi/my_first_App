package com.localllm.app.data.repository

import android.content.Context
import android.util.Log
import com.localllm.app.data.local.dao.ModelDao
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.remote.DefaultModelCatalog
import com.localllm.app.data.remote.HuggingFaceApi
import com.localllm.app.data.remote.ModelCatalogApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing model data from both local database and remote catalog.
 */
@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDao: ModelDao,
    private val modelCatalogApi: ModelCatalogApi,
    private val huggingFaceApi: HuggingFaceApi
) {
    companion object {
        private const val TAG = "ModelRepository"
        
        // Popular GGUF model repositories on HuggingFace
        private val POPULAR_GGUF_REPOS = listOf(
            "bartowski/Llama-3.2-1B-Instruct-GGUF",
            "bartowski/Llama-3.2-3B-Instruct-GGUF",
            "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "microsoft/Phi-3-mini-4k-instruct-gguf",
            "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            "TheBloke/Mistral-7B-Instruct-v0.2-GGUF",
            "bartowski/gemma-2-2b-it-GGUF",
            "bartowski/SmolLM2-1.7B-Instruct-GGUF"
        )
    }
    /**
     * Get all models (both downloaded and available for download).
     */
    fun getAllModels(): Flow<List<ModelInfo>> = modelDao.getAllModels()

    /**
     * Get only downloaded models.
     */
    fun getDownloadedModels(): Flow<List<ModelInfo>> = modelDao.getDownloadedModels()

    /**
     * Get models available for download (not yet downloaded).
     */
    fun getAvailableModels(): Flow<List<ModelInfo>> = modelDao.getAvailableModels()

    /**
     * Get a specific model by ID.
     */
    suspend fun getModelById(modelId: String): ModelInfo? = modelDao.getModelById(modelId)

    /**
     * Get a specific model by ID as Flow.
     */
    fun getModelByIdFlow(modelId: String): Flow<ModelInfo?> = modelDao.getModelByIdFlow(modelId)

    /**
     * Get models that can run on a device with the specified available RAM.
     */
    fun getCompatibleModels(availableRamMb: Int): Flow<List<ModelInfo>> = 
        modelDao.getCompatibleModels(availableRamMb)

    /**
     * Refresh the model catalog from Hugging Face.
     */
    suspend fun refreshModelCatalog(): Result<Unit> {
        return try {
            Log.d(TAG, "Refreshing model catalog from Hugging Face...")
            val existingModels = modelDao.getAllModels().first()
            val models = mutableListOf<ModelInfo>()
            
            // Fetch models from popular GGUF repositories
            for (repoId in POPULAR_GGUF_REPOS) {
                try {
                    Log.d(TAG, "Fetching repo: $repoId")
                    val details = huggingFaceApi.getModelDetails(repoId)
                    
                    // Try to get file sizes from tree API
                    val fileTree = try {
                        huggingFaceApi.getModelFiles(repoId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not get file tree for $repoId: ${e.message}")
                        emptyList()
                    }
                    
                    // Find GGUF files - prefer tree API for sizes, fall back to siblings
                    val ggufFiles = if (fileTree.isNotEmpty()) {
                        fileTree.filter { it.path.endsWith(".gguf") }
                            .map { file -> 
                                GgufFileInfo(
                                    filename = file.path,
                                    size = file.lfs?.size ?: file.size ?: estimateFileSizeFromName(repoId, file.path)
                                )
                            }
                    } else {
                        details.siblings?.filter { it.rfilename.endsWith(".gguf") }
                            ?.map { file ->
                                GgufFileInfo(
                                    filename = file.rfilename,
                                    size = file.lfs?.size ?: file.size ?: estimateFileSizeFromName(repoId, file.rfilename)
                                )
                            } ?: emptyList()
                    }
                    
                    // Filter for mobile-friendly quantizations
                    val mobileFiles = ggufFiles.filter { file ->
                        val name = file.filename.lowercase()
                        name.contains("q4_k_m") || name.contains("q4_k_s") || 
                        name.contains("q5_k_m") || name.contains("q3_k_m") ||
                        (ggufFiles.size == 1) // If only one file, include it
                    }.take(2) // Take up to 2 variants per repo
                    
                    for (file in mobileFiles) {
                        val fileSize = file.size
                        
                        // Only include models under 5GB for mobile
                        if (fileSize > 5_000_000_000L) continue
                        
                        val modelId = "${repoId.replace("/", "-")}-${file.filename.removeSuffix(".gguf")}"
                        val downloadUrl = "https://huggingface.co/$repoId/resolve/main/${file.filename}"
                        
                        val modelInfo = ModelInfo(
                            id = modelId,
                            name = formatModelName(repoId, file.filename),
                            parameterCount = extractParamCount(repoId, file.filename),
                            quantization = extractQuantization(file.filename),
                            fileSizeBytes = fileSize,
                            downloadUrl = downloadUrl,
                            minRamMb = estimateMinRam(fileSize),
                            recommendedRamMb = estimateRecommendedRam(fileSize),
                            description = "From ${details.author ?: repoId.split("/")[0]} on Hugging Face. ${details.downloads} downloads.",
                            license = details.cardData?.license ?: "unknown",
                            tags = buildTags(repoId, file.filename, details.tags),
                            promptTemplate = detectPromptTemplate(repoId),
                            contextLength = detectContextLength(repoId),
                            isDownloaded = false,
                            localPath = null,
                            downloadedDate = null
                        )
                        
                        // Check if already downloaded
                        val existing = existingModels.find { it.id == modelId || it.downloadUrl == downloadUrl }
                        if (existing != null && existing.isDownloaded) {
                            models.add(modelInfo.copy(
                                isDownloaded = true,
                                localPath = existing.localPath,
                                downloadedDate = existing.downloadedDate
                            ))
                        } else {
                            models.add(modelInfo)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch repo $repoId: ${e.message}")
                    // Continue with other repos
                }
            }
            
            // If we got some models, save them
            if (models.isNotEmpty()) {
                Log.d(TAG, "Found ${models.size} models from Hugging Face")
                modelDao.insertModels(models)
                Result.success(Unit)
            } else {
                // Fall back to default catalog
                Log.d(TAG, "No models from HuggingFace, using default catalog")
                useDefaultCatalog(existingModels)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh catalog: ${e.message}", e)
            // Fall back to default catalog
            try {
                val existingModels = modelDao.getAllModels().first()
                useDefaultCatalog(existingModels)
            } catch (e2: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun useDefaultCatalog(existingModels: List<ModelInfo>): Result<Unit> {
        val defaultModels = DefaultModelCatalog.models.map { model ->
            val existing = existingModels.find { it.id == model.id }
            if (existing != null && existing.isDownloaded) {
                model.copy(
                    isDownloaded = true,
                    localPath = existing.localPath,
                    downloadedDate = existing.downloadedDate
                )
            } else {
                model
            }
        }
        modelDao.insertModels(defaultModels)
        return Result.success(Unit)
    }
    
    private fun formatModelName(repoId: String, filename: String): String {
        val repoName = repoId.split("/").last()
            .replace("-GGUF", "")
            .replace("-gguf", "")
            .replace("Instruct", "")
            .replace("-", " ")
            .trim()
        
        val quant = extractQuantization(filename)
        return "$repoName ($quant)"
    }
    
    private fun extractParamCount(repoId: String, filename: String): String {
        val combined = "$repoId $filename".lowercase()
        return when {
            combined.contains("0.5b") -> "0.5B"
            combined.contains("1.1b") -> "1.1B"
            combined.contains("1.5b") -> "1.5B"
            combined.contains("1.7b") -> "1.7B"
            combined.contains("1b") -> "1B"
            combined.contains("2b") -> "2B"
            combined.contains("3b") -> "3B"
            combined.contains("7b") -> "7B"
            combined.contains("8b") -> "8B"
            combined.contains("13b") -> "13B"
            else -> "?"
        }
    }
    
    private fun extractQuantization(filename: String): String {
        val name = filename.uppercase()
        return when {
            name.contains("Q4_K_M") -> "Q4_K_M"
            name.contains("Q4_K_S") -> "Q4_K_S"
            name.contains("Q5_K_M") -> "Q5_K_M"
            name.contains("Q5_K_S") -> "Q5_K_S"
            name.contains("Q3_K_M") -> "Q3_K_M"
            name.contains("Q3_K_S") -> "Q3_K_S"
            name.contains("Q8_0") -> "Q8_0"
            name.contains("Q6_K") -> "Q6_K"
            name.contains("Q4_0") -> "Q4_0"
            name.contains("Q4_1") -> "Q4_1"
            name.contains("F16") -> "F16"
            else -> "GGUF"
        }
    }
    
    private fun estimateMinRam(fileSizeBytes: Long): Int {
        // Rough estimate: model file size + ~500MB overhead
        // If file size is 0, return a reasonable default
        if (fileSizeBytes <= 0) return 1024
        return ((fileSizeBytes / (1024 * 1024)) + 500).toInt().coerceAtLeast(512)
    }
    
    private fun estimateRecommendedRam(fileSizeBytes: Long): Int {
        // Recommended: model file size + ~1GB overhead
        if (fileSizeBytes <= 0) return 2048
        return ((fileSizeBytes / (1024 * 1024)) + 1024).toInt().coerceAtLeast(1024)
    }
    
    /**
     * Estimate file size based on model name when API doesn't provide it.
     * Uses typical sizes for known model architectures and quantizations.
     */
    private fun estimateFileSizeFromName(repoId: String, filename: String): Long {
        val combined = "$repoId $filename".lowercase()
        
        // Extract parameter count
        val params = when {
            combined.contains("0.5b") -> 0.5
            combined.contains("1.1b") -> 1.1
            combined.contains("1.5b") -> 1.5
            combined.contains("1.7b") -> 1.7
            combined.contains("1b") -> 1.0
            combined.contains("2b") -> 2.0
            combined.contains("3b") -> 3.0
            combined.contains("3.8b") -> 3.8
            combined.contains("7b") -> 7.0
            combined.contains("8b") -> 8.0
            combined.contains("13b") -> 13.0
            else -> 2.0 // Default assumption
        }
        
        // Estimate bytes per parameter based on quantization
        val bytesPerParam = when {
            combined.contains("q4_k_m") || combined.contains("q4_k_s") -> 0.5  // ~4 bits
            combined.contains("q3_k") -> 0.4  // ~3 bits
            combined.contains("q5_k") -> 0.6  // ~5 bits
            combined.contains("q6_k") -> 0.75 // ~6 bits
            combined.contains("q8_0") -> 1.0  // ~8 bits
            combined.contains("f16") -> 2.0   // 16 bits
            else -> 0.5 // Default to Q4
        }
        
        // Calculate: params (in billions) * bytes per param * 1 billion + overhead
        val estimatedSize = (params * bytesPerParam * 1_000_000_000L + 100_000_000L).toLong()
        return estimatedSize
    }
    
    /**
     * Helper class to hold GGUF file info.
     */
    private data class GgufFileInfo(
        val filename: String,
        val size: Long
    )
    
    private fun buildTags(repoId: String, filename: String, repoTags: List<String>?): List<String> {
        val tags = mutableListOf<String>()
        val combined = "$repoId $filename".lowercase()
        
        if (combined.contains("llama")) tags.add("llama")
        if (combined.contains("qwen")) tags.add("qwen")
        if (combined.contains("phi")) tags.add("phi")
        if (combined.contains("gemma")) tags.add("gemma")
        if (combined.contains("mistral")) tags.add("mistral")
        if (combined.contains("smol")) tags.add("smollm")
        if (combined.contains("tiny")) tags.add("tiny")
        if (combined.contains("instruct")) tags.add("instruct")
        if (combined.contains("chat")) tags.add("chat")
        
        return tags
    }
    
    private fun detectPromptTemplate(repoId: String): String {
        val name = repoId.lowercase()
        return when {
            name.contains("llama-3") -> "llama3"
            name.contains("llama-2") || name.contains("llama2") -> "llama2"
            name.contains("qwen") -> "chatml"
            name.contains("phi-3") || name.contains("phi3") -> "phi3"
            name.contains("gemma") -> "gemma"
            name.contains("mistral") -> "mistral"
            name.contains("smollm") -> "chatml"
            name.contains("tinyllama") -> "chatml"
            else -> "chatml"
        }
    }
    
    private fun detectContextLength(repoId: String): Int {
        val name = repoId.lowercase()
        return when {
            name.contains("128k") -> 131072
            name.contains("32k") -> 32768
            name.contains("16k") -> 16384
            name.contains("8k") -> 8192
            name.contains("4k") -> 4096
            name.contains("qwen") -> 32768
            name.contains("llama-3") -> 8192
            name.contains("phi-3") -> 4096
            name.contains("gemma") -> 8192
            else -> 4096
        }
    }

    /**
     * Update a model's download status.
     */
    suspend fun updateDownloadStatus(
        modelId: String,
        isDownloaded: Boolean,
        localPath: String?,
        downloadedDate: Long?
    ) {
        modelDao.updateDownloadStatus(modelId, isDownloaded, localPath, downloadedDate)
    }

    /**
     * Update model download state.
     */
    suspend fun updateDownloadState(modelId: String, state: DownloadState) {
        // For now, we just track in memory - could add a downloadState column to ModelInfo
        when (state) {
            is DownloadState.Completed -> {
                // Mark as downloaded when complete
            }
            is DownloadState.Error -> {
                // Reset download status on failure
            }
            else -> {
                // Downloading, Paused, NotStarted - no DB update needed
            }
        }
    }

    /**
     * Update model's local path after download.
     */
    suspend fun updateModelLocalPath(modelId: String, localPath: String) {
        modelDao.updateDownloadStatus(modelId, true, localPath, System.currentTimeMillis())
    }

    /**
     * Delete a downloaded model (mark as not downloaded and remove local path).
     */
    suspend fun deleteDownloadedModel(modelId: String) {
        val model = modelDao.getModelById(modelId)
        model?.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        modelDao.updateDownloadStatus(modelId, false, null, null)
    }

    /**
     * Insert or update a model.
     */
    suspend fun insertModel(model: ModelInfo) {
        modelDao.insertModel(model)
    }

    /**
     * Get the total size of all downloaded models.
     */
    suspend fun getTotalDownloadedSize(): Long {
        return modelDao.getTotalDownloadedSize() ?: 0L
    }

    /**
     * Get the count of downloaded models.
     */
    suspend fun getDownloadedModelsCount(): Int {
        return modelDao.getDownloadedModelsCount()
    }

    /**
     * Search models by tag.
     */
    fun getModelsByTag(tag: String): Flow<List<ModelInfo>> = modelDao.getModelsByTag(tag)
    
    /**
     * Get the models directory path.
     */
    fun getModelsDirectory(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }
}
