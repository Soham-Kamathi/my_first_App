package com.localllm.app.util

import android.content.Context
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Handles downloading of LLM models with resume capability and progress tracking.
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareCapabilities: HardwareCapabilities
) {
    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 8192
        private const val CONNECTION_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: Flow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    /**
     * Download a model with progress tracking and resume capability.
     *
     * @param modelInfo The model to download
     * @param storageType Where to store the model
     * @param onProgress Callback for progress updates
     * @return Result containing the local path on success
     */
    suspend fun downloadModel(
        modelInfo: ModelInfo,
        storageType: com.localllm.app.data.model.StorageType = com.localllm.app.data.model.StorageType.INTERNAL,
        onProgress: (DownloadState) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val modelsDir = hardwareCapabilities.getModelsDirectory(storageType)
            val destFile = File(modelsDir, "${modelInfo.id}.gguf")
            val tempFile = File(modelsDir, "${modelInfo.id}.gguf.tmp")
            
            // Check available space
            val availableSpace = modelsDir.usableSpace
            val requiredSpace = modelInfo.fileSizeBytes
            if (availableSpace < requiredSpace) {
                val error = DownloadState.Error(
                    "Insufficient storage space. Need ${formatBytes(requiredSpace)}, " +
                    "but only ${formatBytes(availableSpace)} available.",
                    isResumable = false
                )
                updateState(modelInfo.id, error)
                onProgress(error)
                return@withContext Result.failure(Exception(error.message))
            }
            
            // Check if already downloaded
            if (destFile.exists() && destFile.length() == modelInfo.fileSizeBytes) {
                val completed = DownloadState.Completed(destFile.absolutePath)
                updateState(modelInfo.id, completed)
                onProgress(completed)
                return@withContext Result.success(destFile.absolutePath)
            }
            
            // Support resume: check if partial file exists
            val existingLength = if (tempFile.exists()) tempFile.length() else 0L
            
            val url = URL(modelInfo.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            // Request resume from existing position
            if (existingLength > 0) {
                connection.setRequestProperty("Range", "bytes=$existingLength-")
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            val contentLength = when (responseCode) {
                HttpURLConnection.HTTP_OK -> connection.contentLengthLong
                HttpURLConnection.HTTP_PARTIAL -> existingLength + connection.contentLengthLong
                else -> {
                    val error = DownloadState.Error(
                        "Server returned error: $responseCode",
                        isResumable = responseCode >= 500
                    )
                    updateState(modelInfo.id, error)
                    onProgress(error)
                    return@withContext Result.failure(Exception(error.message))
                }
            }
            
            val totalBytes = if (contentLength > 0) contentLength else modelInfo.fileSizeBytes
            
            var totalRead = existingLength
            var lastProgressUpdate = System.currentTimeMillis()
            var bytesInLastSecond = 0L
            var speed = 0L
            
            connection.inputStream.use { input ->
                RandomAccessFile(tempFile, "rw").use { output ->
                    output.seek(existingLength)
                    
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check for cancellation
                        if (!coroutineContext.isActive) {
                            val paused = DownloadState.Paused
                            updateState(modelInfo.id, paused)
                            onProgress(paused)
                            return@withContext Result.failure(Exception("Download cancelled"))
                        }
                        
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        bytesInLastSecond += bytesRead
                        
                        // Update progress at most once per 100ms
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate >= 100) {
                            val elapsed = (now - lastProgressUpdate) / 1000.0
                            speed = if (elapsed > 0) (bytesInLastSecond / elapsed).toLong() else 0
                            bytesInLastSecond = 0
                            lastProgressUpdate = now
                            
                            val progress = DownloadState.Downloading(
                                bytesDownloaded = totalRead,
                                totalBytes = totalBytes,
                                speedBytesPerSecond = speed
                            )
                            updateState(modelInfo.id, progress)
                            onProgress(progress)
                        }
                    }
                }
            }
            
            // Verify download completed
            if (tempFile.length() < modelInfo.fileSizeBytes * 0.99) {
                // Allow 1% tolerance for file size differences
                val error = DownloadState.Error(
                    "Download incomplete: ${tempFile.length()} / ${modelInfo.fileSizeBytes}",
                    isResumable = true
                )
                updateState(modelInfo.id, error)
                onProgress(error)
                return@withContext Result.failure(Exception(error.message))
            }
            
            // Verify checksum if available
            if (!modelInfo.sha256Checksum.isNullOrBlank()) {
                updateState(modelInfo.id, DownloadState.Verifying)
                onProgress(DownloadState.Verifying)
                
                val actualChecksum = calculateSha256(tempFile)
                if (!actualChecksum.equals(modelInfo.sha256Checksum, ignoreCase = true)) {
                    tempFile.delete()
                    val error = DownloadState.Error(
                        "Checksum verification failed",
                        isResumable = false
                    )
                    updateState(modelInfo.id, error)
                    onProgress(error)
                    return@withContext Result.failure(Exception(error.message))
                }
            }
            
            // Rename temp file to final destination
            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                val error = DownloadState.Error(
                    "Failed to move downloaded file",
                    isResumable = false
                )
                updateState(modelInfo.id, error)
                onProgress(error)
                return@withContext Result.failure(Exception(error.message))
            }
            
            val completed = DownloadState.Completed(destFile.absolutePath)
            updateState(modelInfo.id, completed)
            onProgress(completed)
            
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            val error = DownloadState.Error(
                e.message ?: "Unknown error",
                isResumable = true
            )
            updateState(modelInfo.id, error)
            onProgress(error)
            Result.failure(e)
        }
    }

    /**
     * Cancel an ongoing download.
     */
    fun cancelDownload(modelId: String) {
        updateState(modelId, DownloadState.Paused)
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(modelId: String): Boolean {
        val modelsDir = hardwareCapabilities.getModelsDirectory()
        val modelFile = File(modelsDir, "$modelId.gguf")
        val tempFile = File(modelsDir, "$modelId.gguf.tmp")
        
        var deleted = true
        if (modelFile.exists()) {
            deleted = deleted && modelFile.delete()
        }
        if (tempFile.exists()) {
            deleted = deleted && tempFile.delete()
        }
        
        updateState(modelId, DownloadState.NotStarted)
        return deleted
    }

    /**
     * Get the download state for a specific model.
     */
    fun getDownloadState(modelId: String): DownloadState {
        return _downloadStates.value[modelId] ?: DownloadState.NotStarted
    }

    /**
     * Check if a model file exists locally.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val modelsDir = hardwareCapabilities.getModelsDirectory()
        val modelFile = File(modelsDir, "$modelId.gguf")
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Get the local path for a model if it exists.
     */
    fun getModelPath(modelId: String): String? {
        val modelsDir = hardwareCapabilities.getModelsDirectory()
        val modelFile = File(modelsDir, "$modelId.gguf")
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    private fun updateState(modelId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(modelId, state)
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
