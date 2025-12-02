package com.localllm.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import com.localllm.app.data.model.CPUInfo
import com.localllm.app.data.model.DeviceInfo
import com.localllm.app.data.model.GPUInfo
import com.localllm.app.data.model.StorageOption
import com.localllm.app.data.model.StorageType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for detecting device hardware capabilities.
 * Used to recommend appropriate models and configure inference settings.
 */
@Singleton
class HardwareCapabilities @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryMonitor: MemoryMonitor
) {
    /**
     * Check if the device supports Android NNAPI.
     * NNAPI provides hardware-accelerated ML inference.
     */
    fun supportsNNAPI(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    }

    /**
     * Check if the device supports Vulkan compute.
     */
    fun supportsVulkan(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    /**
     * Get Vulkan version if supported.
     */
    fun getVulkanVersion(): String? {
        if (!supportsVulkan()) return null
        
        return try {
            val vulkanVersion = context.packageManager
                .getSystemAvailableFeatures()
                .find { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version ?: 0
            
            val major = vulkanVersion shr 22
            val minor = (vulkanVersion shr 12) and 0x3ff
            val patch = vulkanVersion and 0xfff
            "$major.$minor.$patch"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get CPU information.
     */
    fun getCPUInfo(): CPUInfo {
        return CPUInfo(
            cores = Runtime.getRuntime().availableProcessors(),
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        )
    }

    /**
     * Get GPU information using EGL.
     */
    fun getGPUInfo(): GPUInfo? {
        return try {
            val eglDisplay: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return null
            
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return null
            
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            
            if (numConfigs[0] == 0) {
                EGL14.eglTerminate(eglDisplay)
                return null
            }
            
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val eglContext = EGL14.eglCreateContext(
                eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0
            )
            
            val surfaceAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0)
            
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
            
            // Cleanup
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
            
            GPUInfo(
                vendor = vendor,
                renderer = renderer,
                version = glVersion,
                supportsVulkan = supportsVulkan(),
                vulkanVersion = getVulkanVersion()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get available storage options for saving models.
     */
    fun getStorageOptions(): List<StorageOption> {
        val options = mutableListOf<StorageOption>()
        
        // Internal storage
        val internalDir = context.getExternalFilesDir(null)
        if (internalDir != null) {
            options.add(StorageOption(
                path = internalDir.absolutePath,
                availableSpaceBytes = internalDir.usableSpace,
                totalSpaceBytes = internalDir.totalSpace,
                type = StorageType.INTERNAL,
                isRemovable = false
            ))
        }
        
        // External storage directories (SD card, etc.)
        context.getExternalFilesDirs(null).drop(1).forEachIndexed { index, dir ->
            if (dir != null && dir.canWrite()) {
                options.add(StorageOption(
                    path = dir.absolutePath,
                    availableSpaceBytes = dir.usableSpace,
                    totalSpaceBytes = dir.totalSpace,
                    type = if (index == 0) StorageType.EXTERNAL else StorageType.SD_CARD,
                    isRemovable = true
                ))
            }
        }
        
        return options
    }

    /**
     * Get models directory path.
     */
    fun getModelsDirectory(storageType: StorageType = StorageType.INTERNAL): File {
        val baseDir = when (storageType) {
            StorageType.INTERNAL -> context.getExternalFilesDir(null)
            StorageType.EXTERNAL, StorageType.SD_CARD -> {
                context.getExternalFilesDirs(null).drop(1).firstOrNull()
                    ?: context.getExternalFilesDir(null)
            }
        } ?: context.filesDir
        
        val modelsDir = File(baseDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    /**
     * Get complete device information.
     */
    fun getDeviceInfo(): DeviceInfo {
        val memoryInfo = memoryMonitor.getDetailedMemoryInfo()
        val cpuInfo = getCPUInfo()
        val gpuInfo = getGPUInfo()
        val storageOptions = getStorageOptions()
        
        return DeviceInfo(
            totalRamMb = memoryInfo.totalMb,
            availableRamMb = memoryInfo.availableMb,
            cpuCores = cpuInfo.cores,
            cpuArchitecture = cpuInfo.architecture,
            gpuInfo = gpuInfo,
            supportsNNAPI = supportsNNAPI(),
            supportsVulkan = supportsVulkan(),
            internalStorageAvailableMb = storageOptions
                .find { it.type == StorageType.INTERNAL }?.availableSpaceMb ?: 0,
            externalStorageAvailableMb = storageOptions
                .find { it.type != StorageType.INTERNAL }?.availableSpaceMb
        )
    }

    /**
     * Get optimal thread count for inference.
     */
    fun getOptimalThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        // Use all cores except one for system responsiveness
        return maxOf(1, cores - 1)
    }

    /**
     * Get device tier based on hardware capabilities.
     */
    fun getDeviceTier(): DeviceTier {
        val totalRam = memoryMonitor.getTotalMemoryMb()
        val cores = Runtime.getRuntime().availableProcessors()
        
        return when {
            totalRam >= 12000 && cores >= 8 -> DeviceTier.HIGH_END
            totalRam >= 6000 && cores >= 6 -> DeviceTier.MID_RANGE
            totalRam >= 4000 && cores >= 4 -> DeviceTier.LOW_END
            else -> DeviceTier.ENTRY_LEVEL
        }
    }

    /**
     * Get device summary for debugging.
     */
    fun getDeviceSummary(): String {
        val cpuInfo = getCPUInfo()
        val gpuInfo = getGPUInfo()
        val memoryInfo = memoryMonitor.getDetailedMemoryInfo()
        
        return buildString {
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("CPU: ${cpuInfo.cores} cores, ${cpuInfo.architecture}")
            appendLine("GPU: ${gpuInfo?.renderer ?: "Unknown"}")
            appendLine("RAM: ${memoryInfo.availableMb}MB / ${memoryInfo.totalMb}MB")
            appendLine("NNAPI: ${if (supportsNNAPI()) "Yes" else "No"}")
            appendLine("Vulkan: ${getVulkanVersion() ?: "Not supported"}")
            appendLine("Tier: ${getDeviceTier()}")
        }
    }
}

/**
 * Device performance tier.
 */
enum class DeviceTier {
    ENTRY_LEVEL,  // < 4GB RAM
    LOW_END,      // 4-6GB RAM
    MID_RANGE,    // 6-12GB RAM
    HIGH_END      // 12GB+ RAM
}
