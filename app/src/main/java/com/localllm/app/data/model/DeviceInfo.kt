package com.localllm.app.data.model

/**
 * Information about device hardware capabilities.
 */
data class DeviceInfo(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuCores: Int,
    val cpuArchitecture: String,
    val gpuInfo: GPUInfo?,
    val supportsNNAPI: Boolean,
    val supportsVulkan: Boolean,
    val internalStorageAvailableMb: Long,
    val externalStorageAvailableMb: Long?
) {
    /**
     * Returns a list of model parameter counts that are recommended for this device.
     */
    fun recommendedModelSizes(): List<String> {
        return when {
            totalRamMb >= 12_000 -> listOf("7B", "3B", "2.7B", "1.5B", "1B")
            totalRamMb >= 8_000 -> listOf("3B", "2.7B", "1.5B", "1B")
            totalRamMb >= 6_000 -> listOf("2.7B", "1.5B", "1B")
            totalRamMb >= 4_000 -> listOf("1.5B", "1B")
            else -> listOf("1B")
        }
    }

    /**
     * Returns the maximum recommended model size in bytes based on available RAM.
     */
    fun maxRecommendedModelSizeBytes(): Long {
        // Rule of thumb: model should be at most 60% of available RAM for Q4 quantization
        return (availableRamMb * 1024 * 1024 * 0.6).toLong()
    }

    /**
     * Checks if a model can run on this device.
     */
    fun canRunModel(model: ModelInfo): Boolean {
        return availableRamMb >= model.minRamMb
    }
}

/**
 * Information about the device's GPU.
 */
data class GPUInfo(
    val vendor: String,
    val renderer: String,
    val version: String,
    val supportsVulkan: Boolean,
    val vulkanVersion: String? = null
) {
    val isAdreno: Boolean
        get() = renderer.contains("Adreno", ignoreCase = true)

    val isMali: Boolean
        get() = renderer.contains("Mali", ignoreCase = true)

    val isPowerVR: Boolean
        get() = renderer.contains("PowerVR", ignoreCase = true)
}

/**
 * Represents a storage option for saving models.
 */
data class StorageOption(
    val path: String,
    val availableSpaceBytes: Long,
    val totalSpaceBytes: Long,
    val type: StorageType,
    val isRemovable: Boolean = false
) {
    val availableSpaceMb: Long
        get() = availableSpaceBytes / (1024 * 1024)

    val availableSpaceGb: Double
        get() = availableSpaceBytes / (1024.0 * 1024.0 * 1024.0)

    fun formattedAvailableSpace(): String {
        return if (availableSpaceGb >= 1.0) {
            String.format("%.1f GB available", availableSpaceGb)
        } else {
            String.format("%d MB available", availableSpaceMb)
        }
    }
}

/**
 * Type of storage location.
 */
enum class StorageType {
    INTERNAL,
    EXTERNAL,
    SD_CARD
}

/**
 * CPU information for the device.
 */
data class CPUInfo(
    val cores: Int,
    val architecture: String,
    val supportedAbis: List<String>,
    val maxFrequencyMhz: Long? = null
)
