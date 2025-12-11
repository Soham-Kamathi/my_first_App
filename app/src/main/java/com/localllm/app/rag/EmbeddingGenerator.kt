package com.localllm.app.rag

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * BGE-Small embedding generator using ONNX Runtime
 * High-quality embeddings optimized for mobile devices
 * Model: BAAI/bge-small-en-v1.5 (33M parameters, quantized to Q4)
 */
@Singleton
class EmbeddingGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "EmbeddingGenerator"
        private const val EMBEDDING_DIM = 384 // BGE-Small output dimension
        private const val MAX_SEQ_LENGTH = 512 // Maximum token sequence length
        private const val MODEL_FILENAME = "bge-small-en-v1.5.onnx"
    }
    
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isModelLoaded = false
    
    // Simple tokenizer (word-based for fallback, real model uses WordPiece)
    private val vocabulary = mutableMapOf<String, Long>()
    private var vocabBuilt = false
    
    init {
        try {
            initializeONNX()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Runtime", e)
        }
    }
    
    /**
     * Initialize ONNX Runtime environment
     */
    private fun initializeONNX() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            // Try to load model from assets
            val modelPath = copyModelFromAssets()
            if (modelPath != null) {
                val sessionOptions = OrtSession.SessionOptions().apply {
                    // Optimize for mobile
                    setIntraOpNumThreads(4)
                    setInterOpNumThreads(2)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }
                
                ortSession = ortEnvironment?.createSession(modelPath, sessionOptions)
                isModelLoaded = true
                Log.d(TAG, "BGE-Small ONNX model loaded successfully")
            } else {
                Log.w(TAG, "BGE model not found in assets, using fallback TF-IDF")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ONNX model: ${e.message}", e)
            isModelLoaded = false
        }
    }
    
    /**
     * Copy ONNX model from assets to internal storage
     */
    private fun copyModelFromAssets(): String? {
        return try {
            val modelFile = context.filesDir.resolve(MODEL_FILENAME)
            
            if (!modelFile.exists()) {
                context.assets.open(MODEL_FILENAME).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copied ONNX model to: ${modelFile.absolutePath}")
            }
            
            modelFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Model file not found in assets: $MODEL_FILENAME", e)
            null
        }
    }
    
    /**
     * Build vocabulary from corpus (for fallback tokenizer)
     */
    suspend fun buildVocabulary(documents: List<String>) = withContext(Dispatchers.Default) {
        if (isModelLoaded) {
            Log.d(TAG, "Using BGE model, vocabulary building not needed")
            return@withContext
        }
        
        // Fallback: Build simple vocabulary for TF-IDF
        Log.d(TAG, "Building fallback vocabulary from ${documents.size} documents")
        
        val wordCounts = mutableMapOf<String, Int>()
        
        for (doc in documents) {
            val words = tokenizeSimple(doc)
            val uniqueWords = words.toSet()
            
            for (word in uniqueWords) {
                wordCounts[word] = wordCounts.getOrDefault(word, 0) + 1
            }
        }
        
        // Select top 10000 words
        vocabulary.clear()
        vocabulary.putAll(
            wordCounts.entries
                .sortedByDescending { it.value }
                .take(10000)
                .mapIndexed { index, entry -> entry.key to index.toLong() }
                .toMap()
        )
        
        vocabBuilt = true
        Log.d(TAG, "Fallback vocabulary built: ${vocabulary.size} terms")
    }
    
    /**
     * Generate embedding vector for text using BGE-Small model
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (isModelLoaded && ortSession != null && ortEnvironment != null) {
            try {
                return@withContext generateBGEEmbedding(text)
            } catch (e: Exception) {
                Log.e(TAG, "BGE embedding failed, falling back to TF-IDF: ${e.message}", e)
            }
        }
        
        // Fallback to TF-IDF if ONNX model not available
        return@withContext generateTFIDFEmbedding(text)
    }
    
    /**
     * Generate embedding using BGE-Small ONNX model
     */
    private fun generateBGEEmbedding(text: String): FloatArray {
        val env = ortEnvironment ?: throw IllegalStateException("ORT environment not initialized")
        val session = ortSession ?: throw IllegalStateException("ORT session not initialized")
        
        // Tokenize text (simplified - real implementation would use WordPiece tokenizer)
        val tokens = tokenizeForBGE(text)
        val inputIds = tokens.map { it.toLong() }.toLongArray()
        val attentionMask = LongArray(inputIds.size) { 1L }
        
        // Pad or truncate to MAX_SEQ_LENGTH
        val paddedInputIds = inputIds.copyOf(MAX_SEQ_LENGTH).apply {
            if (inputIds.size < MAX_SEQ_LENGTH) {
                for (i in inputIds.size until MAX_SEQ_LENGTH) {
                    this[i] = 0L // PAD token
                }
            }
        }
        
        val paddedAttentionMask = attentionMask.copyOf(MAX_SEQ_LENGTH).apply {
            if (attentionMask.size < MAX_SEQ_LENGTH) {
                for (i in attentionMask.size until MAX_SEQ_LENGTH) {
                    this[i] = 0L
                }
            }
        }
        
        // Create ONNX tensors
        val inputIdsTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(paddedInputIds),
            longArrayOf(1, MAX_SEQ_LENGTH.toLong())
        )
        
        val attentionMaskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(paddedAttentionMask),
            longArrayOf(1, MAX_SEQ_LENGTH.toLong())
        )
        
        // Run inference
        val inputs = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        )
        
        val outputs = session.run(inputs)
        
        // Extract embedding (last hidden state, mean pooling)
        val output = outputs[0].value as Array<Array<FloatArray>>
        val embedding = output[0][0] // First token (CLS) or mean pool
        
        // Clean up
        inputIdsTensor.close()
        attentionMaskTensor.close()
        outputs.close()
        
        // Normalize
        return normalizeVector(embedding)
    }
    
    /**
     * Fallback TF-IDF embedding generation
     */
    private fun generateTFIDFEmbedding(text: String): FloatArray {
        val words = tokenizeSimple(text)
        val embedding = FloatArray(EMBEDDING_DIM) { 0f }
        
        val termFrequency = mutableMapOf<String, Int>()
        for (word in words) {
            if (word in vocabulary) {
                termFrequency[word] = termFrequency.getOrDefault(word, 0) + 1
            }
        }
        
        val maxTf = termFrequency.values.maxOrNull() ?: 1
        
        for ((word, tf) in termFrequency) {
            val index = vocabulary[word]?.toInt() ?: continue
            if (index < EMBEDDING_DIM) {
                embedding[index] = (tf.toFloat() / maxTf)
            }
        }
        
        return normalizeVector(embedding)
    }
    
    /**
     * Simple tokenization for BGE (placeholder - real model uses WordPiece)
     */
    private fun tokenizeForBGE(text: String): List<Int> {
        // Simplified tokenization - in production, use proper WordPiece tokenizer
        // This is a placeholder that works with basic vocabulary
        val words = tokenizeSimple(text)
        return words.mapNotNull { vocabulary[it]?.toInt() }.take(MAX_SEQ_LENGTH - 2)
    }
    
    /**
     * Simple word tokenization
     */
    private fun tokenizeSimple(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
    }
    
    /**
     * Normalize vector to unit length (L2 norm)
     */
    private fun normalizeVector(vector: FloatArray): FloatArray {
        val magnitude = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (magnitude > 0f) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        return vector
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) { 
            "Embeddings must have same dimension" 
        }
        
        var dotProduct = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        
        return dotProduct.coerceIn(-1f, 1f)
    }
    
    /**
     * Batch generate embeddings
     */
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        texts.map { generateEmbedding(it) }
    }
    
    /**
     * Get embedding dimension
     */
    fun getEmbeddingDim(): Int = EMBEDDING_DIM
    
    /**
     * Check if model is ready
     */
    fun isModelLoaded(): Boolean = isModelLoaded
    
    /**
     * Check if vocabulary is built (for fallback)
     */
    fun isVocabularyBuilt(): Boolean = vocabBuilt || isModelLoaded
    
    /**
     * Clear vocabulary (for resetting)
     */
    fun clearVocabulary() {
        vocabulary.clear()
        vocabBuilt = false
        Log.d(TAG, "Vocabulary cleared")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
            ortSession = null
            ortEnvironment = null
            isModelLoaded = false
            Log.d(TAG, "ONNX resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up ONNX resources", e)
        }
    }
}
