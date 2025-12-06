package com.localllm.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects hardware capabilities for optimal LLM inference.
 * Supports GPU (Vulkan), CPU SIMD optimizations, and memory detection.
 */
@Singleton
class HardwareCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HardwareCapability"
        
        // RAM thresholds for model recommendations
        const val RAM_LOW = 2L * 1024 * 1024 * 1024       // 2 GB
        const val RAM_MEDIUM = 4L * 1024 * 1024 * 1024    // 4 GB
        const val RAM_HIGH = 6L * 1024 * 1024 * 1024      // 6 GB
        const val RAM_ULTRA = 8L * 1024 * 1024 * 1024     // 8 GB
        
        // Known high-end chipsets with good GPU/NPU support
        private val HIGH_END_CHIPSETS = listOf(
            // Qualcomm Snapdragon (8 series)
            "SM8650", "SM8550", "SM8475", "SM8450", "SM8350", "SM8250", // 8 Gen 3, 8 Gen 2, 8+ Gen 1, 8 Gen 1, 888, 865
            // MediaTek Dimensity (9000 series)
            "MT6989", "MT6985", "MT6983", "MT6980", // Dimensity 9300, 9200, 9000
            // Samsung Exynos (2xxx series)
            "s5e9945", "s5e9935", "s5e9925", "s5e9920", // Exynos 2400, 2200, 2100
            // Google Tensor
            "Tensor", "TensorG2", "TensorG3", "TensorG4"
        )
        
        // Chipsets with dedicated NPU
        private val NPU_CHIPSETS = listOf(
            // Qualcomm with Hexagon NPU
            "SM8650", "SM8550", "SM8475", "SM8450", "SM8350",
            // MediaTek with APU
            "MT6989", "MT6985", "MT6983",
            // Samsung with NPU
            "s5e9945", "s5e9935", "s5e9925",
            // Google Tensor with TPU
            "Tensor", "TensorG2", "TensorG3", "TensorG4"
        )
    }
    
    /**
     * Hardware capability levels
     */
    enum class CapabilityLevel {
        LOW,      // Basic CPU only, 2-4GB RAM
        MEDIUM,   // Good CPU, 4-6GB RAM
        HIGH,     // Fast CPU + GPU support, 6-8GB RAM
        ULTRA     // Flagship CPU + GPU + NPU, 8GB+ RAM
    }
    
    /**
     * Acceleration type available
     */
    enum class AccelerationType {
        CPU_ONLY,
        CPU_SIMD,      // ARM NEON / x86 SSE/AVX
        GPU_VULKAN,    // Vulkan compute
        NPU            // Neural Processing Unit (future)
    }
    
    /**
     * Hardware profile with all detected capabilities
     */
    data class HardwareProfile(
        val deviceModel: String,
        val chipset: String,
        val cpuCores: Int,
        val cpuArchitecture: String,
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val capabilityLevel: CapabilityLevel,
        val supportedAcceleration: List<AccelerationType>,
        val recommendedGpuLayers: Int,
        val recommendedThreads: Int,
        val recommendedContextSize: Int,
        val hasVulkanSupport: Boolean,
        val hasNpuSupport: Boolean,
        val isHighEndDevice: Boolean
    ) {
        val totalRamGB: Double get() = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        val availableRamGB: Double get() = availableRamBytes / (1024.0 * 1024.0 * 1024.0)
    }
    
    /**
     * Detect and return the full hardware profile
     */
    fun detectHardwareProfile(): HardwareProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRam = memoryInfo.totalMem
        val availableRam = memoryInfo.availMem
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val chipset = detectChipset()
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        
        val isHighEnd = isHighEndChipset(chipset)
        val hasVulkan = checkVulkanSupport()
        val hasNpu = hasNpuCapability(chipset)
        
        val capabilityLevel = determineCapabilityLevel(totalRam, isHighEnd)
        val accelerationTypes = detectAccelerationTypes(cpuArch, hasVulkan, hasNpu)
        
        val profile = HardwareProfile(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            chipset = chipset,
            cpuCores = cpuCores,
            cpuArchitecture = cpuArch,
            totalRamBytes = totalRam,
            availableRamBytes = availableRam,
            capabilityLevel = capabilityLevel,
            supportedAcceleration = accelerationTypes,
            recommendedGpuLayers = calculateRecommendedGpuLayers(capabilityLevel, hasVulkan),
            recommendedThreads = calculateRecommendedThreads(cpuCores, capabilityLevel),
            recommendedContextSize = calculateRecommendedContextSize(totalRam),
            hasVulkanSupport = hasVulkan,
            hasNpuSupport = hasNpu,
            isHighEndDevice = isHighEnd
        )
        
        Log.i(TAG, "Hardware Profile: $profile")
        return profile
    }
    
    /**
     * Detect the SoC/chipset name
     */
    private fun detectChipset(): String {
        return try {
            // Try to read from Build properties
            val hardware = Build.HARDWARE
            val board = Build.BOARD
            val soc = Build.SOC_MODEL.takeIf { Build.VERSION.SDK_INT >= 31 } ?: ""
            
            when {
                soc.isNotBlank() -> soc
                hardware.contains("qcom", ignoreCase = true) -> "Qualcomm $hardware"
                hardware.contains("exynos", ignoreCase = true) -> "Samsung $hardware"
                hardware.contains("mt", ignoreCase = true) -> "MediaTek $hardware"
                hardware.contains("tensor", ignoreCase = true) -> "Google $hardware"
                else -> "$hardware ($board)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect chipset", e)
            "Unknown"
        }
    }
    
    /**
     * Check if this is a high-end chipset
     */
    private fun isHighEndChipset(chipset: String): Boolean {
        return HIGH_END_CHIPSETS.any { chipset.contains(it, ignoreCase = true) }
    }
    
    /**
     * Check if chipset has NPU capability
     */
    private fun hasNpuCapability(chipset: String): Boolean {
        return NPU_CHIPSETS.any { chipset.contains(it, ignoreCase = true) }
    }
    
    /**
     * Check Vulkan support on the device
     */
    private fun checkVulkanSupport(): Boolean {
        return try {
            // Check Android API level (Vulkan requires API 24+)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return false
            }
            
            // Check for Vulkan feature
            val pm = context.packageManager
            pm.hasSystemFeature("android.hardware.vulkan.compute", 0) ||
            pm.hasSystemFeature("android.hardware.vulkan.level", 0) ||
            pm.hasSystemFeature("android.hardware.vulkan.version", 0x401000) // Vulkan 1.1+
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Vulkan support", e)
            false
        }
    }
    
    /**
     * Determine overall capability level
     */
    private fun determineCapabilityLevel(totalRam: Long, isHighEnd: Boolean): CapabilityLevel {
        return when {
            totalRam >= RAM_ULTRA && isHighEnd -> CapabilityLevel.ULTRA
            totalRam >= RAM_HIGH && isHighEnd -> CapabilityLevel.HIGH
            totalRam >= RAM_MEDIUM -> CapabilityLevel.MEDIUM
            else -> CapabilityLevel.LOW
        }
    }
    
    /**
     * Detect available acceleration types
     */
    private fun detectAccelerationTypes(
        cpuArch: String,
        hasVulkan: Boolean,
        hasNpu: Boolean
    ): List<AccelerationType> {
        val types = mutableListOf<AccelerationType>()
        
        // All devices support CPU
        types.add(AccelerationType.CPU_ONLY)
        
        // SIMD support (NEON for ARM, SSE for x86)
        if (cpuArch.contains("arm64") || cpuArch.contains("x86_64")) {
            types.add(AccelerationType.CPU_SIMD)
        }
        
        // Vulkan GPU compute
        if (hasVulkan) {
            types.add(AccelerationType.GPU_VULKAN)
        }
        
        // NPU (future support)
        if (hasNpu) {
            types.add(AccelerationType.NPU)
        }
        
        return types
    }
    
    /**
     * Calculate recommended GPU layers based on capability
     * More layers = more computation offloaded to GPU
     */
    private fun calculateRecommendedGpuLayers(level: CapabilityLevel, hasVulkan: Boolean): Int {
        if (!hasVulkan) return 0
        
        return when (level) {
            CapabilityLevel.ULTRA -> 99  // All layers on GPU
            CapabilityLevel.HIGH -> 35   // Most layers on GPU
            CapabilityLevel.MEDIUM -> 20 // Some layers on GPU
            CapabilityLevel.LOW -> 0     // CPU only
        }
    }
    
    /**
     * Calculate recommended thread count
     */
    private fun calculateRecommendedThreads(cpuCores: Int, level: CapabilityLevel): Int {
        // Leave some cores for the system
        return when (level) {
            CapabilityLevel.ULTRA -> maxOf(cpuCores - 1, 4)
            CapabilityLevel.HIGH -> maxOf(cpuCores - 2, 4)
            CapabilityLevel.MEDIUM -> maxOf(cpuCores / 2, 2)
            CapabilityLevel.LOW -> maxOf(cpuCores / 2, 2)
        }.coerceIn(2, 8)
    }
    
    /**
     * Calculate recommended context size based on available RAM
     */
    private fun calculateRecommendedContextSize(totalRam: Long): Int {
        return when {
            totalRam >= RAM_ULTRA -> 8192
            totalRam >= RAM_HIGH -> 4096
            totalRam >= RAM_MEDIUM -> 2048
            else -> 1024
        }
    }
    
    /**
     * Get model size recommendations based on hardware
     */
    fun getModelRecommendations(): ModelRecommendations {
        val profile = detectHardwareProfile()
        
        return ModelRecommendations(
            maxModelSizeGB = when (profile.capabilityLevel) {
                CapabilityLevel.ULTRA -> 8.0
                CapabilityLevel.HIGH -> 4.0
                CapabilityLevel.MEDIUM -> 2.5
                CapabilityLevel.LOW -> 1.5
            },
            recommendedQuantization = when (profile.capabilityLevel) {
                CapabilityLevel.ULTRA -> "Q6_K or Q8_0"
                CapabilityLevel.HIGH -> "Q5_K_M or Q6_K"
                CapabilityLevel.MEDIUM -> "Q4_K_M"
                CapabilityLevel.LOW -> "Q3_K or Q4_0"
            },
            canRunLargeModels = profile.capabilityLevel >= CapabilityLevel.HIGH,
            useGpuAcceleration = profile.hasVulkanSupport && profile.capabilityLevel >= CapabilityLevel.MEDIUM
        )
    }
    
    data class ModelRecommendations(
        val maxModelSizeGB: Double,
        val recommendedQuantization: String,
        val canRunLargeModels: Boolean,
        val useGpuAcceleration: Boolean
    )
}
