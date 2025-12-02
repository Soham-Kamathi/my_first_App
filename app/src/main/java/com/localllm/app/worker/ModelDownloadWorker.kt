package com.localllm.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.localllm.app.R
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.model.StorageType
import com.localllm.app.data.repository.ModelRepository
import com.localllm.app.util.ModelDownloader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val modelDownloader: ModelDownloader,
    private val modelRepository: ModelRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_PROGRESS = "progress"
        
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun createWorkRequest(
            modelId: String,
            downloadUrl: String,
            fileName: String,
            fileSizeBytes: Long = 0L
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_MODEL_ID to modelId,
                KEY_DOWNLOAD_URL to downloadUrl,
                KEY_FILE_NAME to fileName,
                KEY_FILE_SIZE to fileSizeBytes
            )
            
            return OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .addTag("model_download")
                .addTag(modelId)
                .build()
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return@withContext Result.failure()
        val fileSizeBytes = inputData.getLong(KEY_FILE_SIZE, 0L)
        
        createNotificationChannel()
        
        try {
            // Update model state to downloading
            modelRepository.updateDownloadState(modelId, DownloadState.Downloading(0f))
            
            // Create a minimal ModelInfo for the downloader
            val modelInfo = ModelInfo(
                id = modelId,
                name = fileName.removeSuffix(".gguf"),
                description = "",
                parameterCount = "",
                quantization = "",
                fileSizeBytes = fileSizeBytes,
                downloadUrl = downloadUrl,
                contextLength = 2048,
                promptTemplate = "chatml",
                isDownloaded = false,
                minRamMb = 0,
                recommendedRamMb = 0,
                license = "",
                tags = emptyList()
            )
            
            val result = modelDownloader.downloadModel(
                modelInfo = modelInfo,
                storageType = StorageType.INTERNAL
            ) { state ->
                when (state) {
                    is DownloadState.Downloading -> {
                        // Update progress (non-suspend in callback)
                    }
                    else -> {}
                }
            }
            
            result.fold(
                onSuccess = { localPath ->
                    modelRepository.updateDownloadState(modelId, DownloadState.Completed(localPath))
                    modelRepository.updateModelLocalPath(modelId, localPath)
                    showCompletedNotification(fileName)
                    Result.success()
                },
                onFailure = { error ->
                    modelRepository.updateDownloadState(modelId, DownloadState.Error(error.message ?: "Unknown error"))
                    showFailedNotification(fileName, error.message ?: "Unknown error")
                    Result.failure()
                }
            )
        } catch (e: Exception) {
            modelRepository.updateDownloadState(modelId, DownloadState.Error(e.message ?: "Unknown error"))
            showFailedNotification(fileName, e.message ?: "Unknown error")
            Result.failure()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for model download progress"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showProgressNotification(fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading Model")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        
        setForegroundAsync(ForegroundInfo(NOTIFICATION_ID, notification))
    }
    
    private fun showCompletedNotification(fileName: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    private fun showFailedNotification(fileName: String, error: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("$fileName: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
