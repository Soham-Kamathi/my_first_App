package com.localllm.app.data.model

/**
 * Represents the download state of a model.
 */
sealed class DownloadState {
    data object NotStarted : DownloadState()
    
    data class Downloading(
        val progress: Float = 0f,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSecond: Long = 0L
    ) : DownloadState() {
        constructor(progress: Float) : this(progress, 0L, 0L, 0L)
        
        val progressPercent: Int
            get() = (progress * 100).toInt()
        
        val calculatedProgress: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else progress

        fun formattedProgress(): String {
            val downloadedMb = bytesDownloaded / (1024.0 * 1024.0)
            val totalMb = totalBytes / (1024.0 * 1024.0)
            return String.format("%.1f / %.1f MB", downloadedMb, totalMb)
        }

        fun formattedSpeed(): String {
            return when {
                speedBytesPerSecond >= 1_048_576 -> 
                    String.format("%.1f MB/s", speedBytesPerSecond / 1_048_576.0)
                speedBytesPerSecond >= 1024 -> 
                    String.format("%.1f KB/s", speedBytesPerSecond / 1024.0)
                else -> 
                    "$speedBytesPerSecond B/s"
            }
        }

        fun estimatedTimeRemaining(): String {
            if (speedBytesPerSecond <= 0) return "Calculating..."
            val remainingBytes = totalBytes - bytesDownloaded
            val secondsRemaining = remainingBytes / speedBytesPerSecond
            return when {
                secondsRemaining >= 3600 -> "${secondsRemaining / 3600}h ${(secondsRemaining % 3600) / 60}m"
                secondsRemaining >= 60 -> "${secondsRemaining / 60}m ${secondsRemaining % 60}s"
                else -> "${secondsRemaining}s"
            }
        }
    }
    
    data object Paused : DownloadState()
    
    data class Completed(val localPath: String) : DownloadState()
    
    data class Error(val message: String, val isResumable: Boolean = true) : DownloadState()
    
    data object Verifying : DownloadState()
}

/**
 * Represents a download task for tracking purposes.
 */
data class DownloadTask(
    val modelId: String,
    val modelName: String,
    val state: DownloadState,
    val startTime: Long = System.currentTimeMillis()
)
