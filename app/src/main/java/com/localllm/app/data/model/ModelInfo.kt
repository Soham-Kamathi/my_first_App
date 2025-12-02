package com.localllm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Represents information about an LLM model available for download or already downloaded.
 */
@Entity(tableName = "models")
data class ModelInfo(
    @PrimaryKey
    val id: String,
    val name: String,
    val parameterCount: String, // "7B", "3B", "2.7B", etc.
    val quantization: String, // "Q4_K_M", "Q5_K_S", "Q8_0", etc.
    val fileSizeBytes: Long,
    val downloadUrl: String,
    val minRamMb: Int,
    val recommendedRamMb: Int,
    val description: String,
    val license: String,
    val tags: List<String>,
    val promptTemplate: String = PromptTemplate.CHATML,
    val contextLength: Int = 2048,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val downloadedDate: Long? = null,
    val sha256Checksum: String? = null
) {
    /**
     * Returns the file size formatted as a human-readable string.
     */
    fun formattedFileSize(): String {
        return when {
            fileSizeBytes >= 1_073_741_824 -> String.format("%.2f GB", fileSizeBytes / 1_073_741_824.0)
            fileSizeBytes >= 1_048_576 -> String.format("%.1f MB", fileSizeBytes / 1_048_576.0)
            fileSizeBytes >= 1024 -> String.format("%.1f KB", fileSizeBytes / 1024.0)
            else -> "$fileSizeBytes B"
        }
    }

    /**
     * Returns the minimum RAM requirement formatted as a human-readable string.
     */
    fun formattedRamRequirement(): String {
        return if (minRamMb >= 1024) {
            String.format("%.1f GB RAM", minRamMb / 1024.0)
        } else {
            "$minRamMb MB RAM"
        }
    }
}

/**
 * Prompt template formats for different model types.
 */
object PromptTemplate {
    const val CHATML = "chatml"
    const val ALPACA = "alpaca"
    const val VICUNA = "vicuna"
    const val LLAMA2 = "llama2"
    const val MISTRAL = "mistral"
    const val PHI = "phi"
    const val ZEPHYR = "zephyr"
    const val RAW = "raw"
}

/**
 * Room TypeConverters for complex types.
 */
class ModelTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
