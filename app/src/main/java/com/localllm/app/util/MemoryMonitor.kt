package com.localllm.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for tracking device memory usage.
 * Critical for ensuring models can be loaded safely.
 */
@Singleton
class MemoryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    /**
     * Get available memory in megabytes.
     */
    fun getAvailableMemoryMb(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    /**
     * Get total device memory in megabytes.
     */
    fun getTotalMemoryMb(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    /**
     * Get used memory in megabytes.
     */
    fun getUsedMemoryMb(): Long {
        return getTotalMemoryMb() - getAvailableMemoryMb()
    }

    /**
     * Get memory usage as a percentage.
     */
    fun getMemoryUsagePercent(): Float {
        val total = getTotalMemoryMb()
        return if (total > 0) {
            (getUsedMemoryMb().toFloat() / total) * 100f
        } else {
            0f
        }
    }

    /**
     * Check if the system is in low memory state.
     */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    /**
     * Get the low memory threshold in megabytes.
     */
    fun getLowMemoryThresholdMb(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.threshold / (1024 * 1024)
    }

    /**
     * Check if a model with the specified memory requirement can be loaded.
     * 
     * @param requiredMemoryMb Memory required by the model in MB
     * @param bufferMb Additional buffer to keep for system stability (default 200MB)
     * @return true if the model can likely be loaded safely
     */
    fun canLoadModel(requiredMemoryMb: Int, bufferMb: Int = 200): Boolean {
        val availableMb = getAvailableMemoryMb()
        // Be lenient - allow if we have at least 70% of required memory
        // The OS will use mmap which doesn't require all memory upfront
        val effectiveRequired = (requiredMemoryMb * 0.7).toInt()
        return (availableMb - bufferMb) >= effectiveRequired
    }

    /**
     * Get memory state as a human-readable string.
     */
    fun getMemoryStateString(): String {
        return buildString {
            append("Available: ${getAvailableMemoryMb()} MB / ${getTotalMemoryMb()} MB")
            append(" (${String.format("%.1f", getMemoryUsagePercent())}% used)")
            if (isLowMemory()) {
                append(" [LOW MEMORY]")
            }
        }
    }

    /**
     * Get the maximum recommended model size based on current memory.
     * Returns value in megabytes.
     */
    fun getMaxRecommendedModelSizeMb(): Long {
        val availableMb = getAvailableMemoryMb()
        // Use 60% of available memory for the model
        return (availableMb * 0.6).toLong()
    }

    /**
     * Get detailed memory info.
     */
    fun getDetailedMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            totalMb = memInfo.totalMem / (1024 * 1024),
            availableMb = memInfo.availMem / (1024 * 1024),
            thresholdMb = memInfo.threshold / (1024 * 1024),
            lowMemory = memInfo.lowMemory
        )
    }

    /**
     * Memory information data class.
     */
    data class MemoryInfo(
        val totalMb: Long,
        val availableMb: Long,
        val thresholdMb: Long,
        val lowMemory: Boolean
    ) {
        val usedMb: Long get() = totalMb - availableMb
        val usagePercent: Float get() = (usedMb.toFloat() / totalMb) * 100f
    }
}
