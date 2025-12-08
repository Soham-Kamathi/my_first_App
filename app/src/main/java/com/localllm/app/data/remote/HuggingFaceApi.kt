package com.localllm.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Hugging Face API for searching and downloading GGUF models.
 */
interface HuggingFaceApi {
    
    /**
     * Search for models on Hugging Face.
     */
    @GET("models")
    suspend fun searchModels(
        @Query("search") search: String = "gguf",
        @Query("filter") filter: String = "gguf",
        @Query("sort") sort: String = "downloads",
        @Query("direction") direction: Int = -1,
        @Query("limit") limit: Int = 50
    ): List<HuggingFaceModel>
    
    /**
     * Get model details including files.
     */
    @GET("models/{modelId}")
    suspend fun getModelDetails(
        @Path("modelId", encoded = true) modelId: String,
        @Query("siblings") siblings: Int = 1
    ): HuggingFaceModelDetails
    
    /**
     * Get files in a model repository.
     */
    @GET("models/{modelId}/tree/main")
    suspend fun getModelFiles(
        @Path("modelId", encoded = true) modelId: String
    ): List<HuggingFaceFile>
}

/**
 * Basic model info from search results.
 */
data class HuggingFaceModel(
    val _id: String? = null,
    val id: String,
    val modelId: String? = null,
    val author: String? = null,
    val sha: String? = null,
    val lastModified: String? = null,
    val private: Boolean = false,
    val disabled: Boolean? = null,
    val gated: Boolean? = null,
    val pipeline_tag: String? = null,
    val tags: List<String>? = null,
    val downloads: Int = 0,
    val likes: Int = 0,
    val library_name: String? = null,
    val createdAt: String? = null
)

/**
 * Detailed model info.
 */
data class HuggingFaceModelDetails(
    val _id: String? = null,
    val id: String,
    val modelId: String? = null,
    val author: String? = null,
    val sha: String? = null,
    val lastModified: String? = null,
    val private: Boolean = false,
    val disabled: Boolean? = null,
    val gated: Boolean? = null,
    val pipeline_tag: String? = null,
    val tags: List<String>? = null,
    val downloads: Int = 0,
    val likes: Int = 0,
    val library_name: String? = null,
    val siblings: List<HuggingFaceSibling>? = null,
    val cardData: HuggingFaceCardData? = null,
    val createdAt: String? = null,
    val widgetData: List<Map<String, String>>? = null,
    val config: HuggingFaceConfig? = null,
    val transformersInfo: HuggingFaceTransformersInfo? = null
)

/**
 * Model configuration data.
 */
data class HuggingFaceConfig(
    val architectures: List<String>? = null,
    val model_type: String? = null,
    val quantization_config: Map<String, Any>? = null,
    val tokenizer_config: Map<String, Any>? = null
)

/**
 * Transformers library info.
 */
data class HuggingFaceTransformersInfo(
    val auto_model: String? = null,
    val pipeline_tag: String? = null
)

/**
 * File/sibling in a model repository.
 */
data class HuggingFaceSibling(
    val rfilename: String,
    val size: Long? = null,
    val blobId: String? = null,
    val lfs: HuggingFaceLfs? = null
)

/**
 * LFS info for large files.
 */
data class HuggingFaceLfs(
    val size: Long,
    val sha256: String? = null,
    val pointerSize: Int? = null
)

/**
 * Model card data.
 */
data class HuggingFaceCardData(
    val license: String? = null,
    val language: List<String>? = null,
    val datasets: List<String>? = null,
    val tags: List<String>? = null,
    val base_model: String? = null
)

/**
 * File from tree endpoint.
 */
data class HuggingFaceFile(
    val type: String,  // "file" or "directory"
    val oid: String? = null,
    val size: Long? = null,
    val path: String,
    val lfs: HuggingFaceLfs? = null
)
