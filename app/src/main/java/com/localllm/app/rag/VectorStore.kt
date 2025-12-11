package com.localllm.app.rag

import android.util.Log
import com.localllm.app.util.DocumentParser
import com.localllm.app.util.ParsedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG indexing progress state
 */
sealed class IndexingState {
    object Idle : IndexingState()
    data class Indexing(val progress: Float, val currentChunk: Int, val totalChunks: Int) : IndexingState()
    data class Complete(val chunksIndexed: Int) : IndexingState()
    data class Error(val message: String) : IndexingState()
}

/**
 * Vector store for RAG document indexing and retrieval
 * Manages embedding storage and semantic search
 */
@Singleton
class VectorStore @Inject constructor(
    private val documentChunkDao: DocumentChunkDao,
    private val embeddingGenerator: EmbeddingGenerator,
    private val documentParser: DocumentParser
) {
    
    companion object {
        private const val TAG = "VectorStore"
        private const val DEFAULT_CHUNK_SIZE = 800
        private const val CHUNK_OVERLAP = 200
        private const val TOP_K_RESULTS = 5
    }
    
    private val _indexingState = MutableStateFlow<IndexingState>(IndexingState.Idle)
    val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()
    
    /**
     * Index a parsed document with embeddings
     */
    suspend fun indexDocument(
        document: ParsedDocument,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = CHUNK_OVERLAP
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Indexing document: ${document.fileName}")
            _indexingState.value = IndexingState.Indexing(0f, 0, 0)
            
            // Generate document ID
            val documentId = UUID.randomUUID().toString()
            
            // Chunk the document
            val textChunks = documentParser.chunkDocument(
                content = document.content,
                chunkSize = chunkSize,
                overlap = overlap
            )
            
            Log.d(TAG, "Document split into ${textChunks.size} chunks")
            
            // Build vocabulary from all chunks if not already built
            if (!embeddingGenerator.isVocabularyBuilt()) {
                val allTexts = textChunks.map { it.content }
                embeddingGenerator.buildVocabulary(allTexts)
            }
            
            // Generate embeddings for each chunk
            val documentChunkEntities = mutableListOf<DocumentChunkEntity>()
            
            for ((index, textChunk) in textChunks.withIndex()) {
                val embedding = embeddingGenerator.generateEmbedding(textChunk.content)
                val embeddingString = embedding.joinToString(",")
                
                val entity = DocumentChunkEntity(
                    documentId = documentId,
                    documentName = document.fileName,
                    chunkIndex = index,
                    content = textChunk.content,
                    embedding = embeddingString,
                    startChar = textChunk.startChar,
                    endChar = textChunk.endChar
                )
                
                documentChunkEntities.add(entity)
                
                // Update progress
                val progress = (index + 1).toFloat() / textChunks.size
                _indexingState.value = IndexingState.Indexing(progress, index + 1, textChunks.size)
            }
            
            // Insert all chunks into database
            documentChunkDao.insertChunks(documentChunkEntities)
            
            Log.d(TAG, "Successfully indexed ${documentChunkEntities.size} chunks")
            _indexingState.value = IndexingState.Complete(documentChunkEntities.size)
            
            Result.success(documentChunkEntities.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to index document: ${e.message}", e)
            _indexingState.value = IndexingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Search for relevant chunks using semantic similarity
     */
    suspend fun search(
        query: String,
        topK: Int = TOP_K_RESULTS,
        similarityThreshold: Float = 0.3f
    ): List<ChunkSearchResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching for: $query")
            
            // Generate query embedding
            val queryEmbedding = embeddingGenerator.generateEmbedding(query)
            
            // Get all chunks from database
            val allChunks = documentChunkDao.getAllChunks()
            
            if (allChunks.isEmpty()) {
                Log.w(TAG, "No chunks in vector store")
                return@withContext emptyList()
            }
            
            // Calculate similarity scores
            val results = allChunks.map { chunk ->
                val chunkEmbedding = parseEmbedding(chunk.embedding)
                val similarity = embeddingGenerator.cosineSimilarity(queryEmbedding, chunkEmbedding)
                ChunkSearchResult(chunk, similarity)
            }
            
            // Filter and sort by similarity
            val topResults = results
                .filter { it.similarity >= similarityThreshold }
                .sortedByDescending { it.similarity }
                .take(topK)
            
            Log.d(TAG, "Found ${topResults.size} relevant chunks (threshold: $similarityThreshold)")
            topResults
            
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Search within specific document
     */
    suspend fun searchInDocument(
        documentId: String,
        query: String,
        topK: Int = TOP_K_RESULTS
    ): List<ChunkSearchResult> = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = embeddingGenerator.generateEmbedding(query)
            val chunks = documentChunkDao.getChunksByDocument(documentId)
            
            chunks.map { chunk ->
                val chunkEmbedding = parseEmbedding(chunk.embedding)
                val similarity = embeddingGenerator.cosineSimilarity(queryEmbedding, chunkEmbedding)
                ChunkSearchResult(chunk, similarity)
            }
            .sortedByDescending { it.similarity }
            .take(topK)
            
        } catch (e: Exception) {
            Log.e(TAG, "Document search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get all indexed documents
     */
    suspend fun getIndexedDocuments(): List<DocumentInfo> {
        return documentChunkDao.getIndexedDocuments()
    }
    
    /**
     * Get chunks for a specific document
     */
    suspend fun getDocumentChunks(documentId: String): List<DocumentChunkEntity> {
        return documentChunkDao.getChunksByDocument(documentId)
    }
    
    /**
     * Delete document from index
     */
    suspend fun deleteDocument(documentId: String) {
        documentChunkDao.deleteChunksByDocument(documentId)
        Log.d(TAG, "Deleted document: $documentId")
    }
    
    /**
     * Clear entire vector store
     */
    suspend fun clearAll() {
        documentChunkDao.deleteAllChunks()
        embeddingGenerator.clearVocabulary()
        _indexingState.value = IndexingState.Idle
        Log.d(TAG, "Vector store cleared")
    }
    
    /**
     * Get total number of indexed chunks
     */
    suspend fun getTotalChunkCount(): Int {
        return documentChunkDao.getAllChunks().size
    }
    
    /**
     * Build context string from search results
     */
    fun buildContextFromResults(
        results: List<ChunkSearchResult>,
        maxLength: Int = 3000
    ): String {
        val sb = StringBuilder()
        
        for ((index, result) in results.withIndex()) {
            val chunkText = """
                [Document: ${result.chunk.documentName}, Chunk ${result.chunk.chunkIndex + 1}, Relevance: ${(result.similarity * 100).toInt()}%]
                ${result.chunk.content}
            """.trimIndent()
            
            if (sb.length + chunkText.length > maxLength) {
                if (sb.isEmpty() && index == 0) {
                    // Include at least first chunk even if over limit
                    sb.append(chunkText.take(maxLength))
                }
                break
            }
            
            if (sb.isNotEmpty()) sb.append("\n\n---\n\n")
            sb.append(chunkText)
        }
        
        return sb.toString()
    }
    
    /**
     * Parse embedding string back to float array
     */
    private fun parseEmbedding(embeddingString: String): FloatArray {
        return embeddingString.split(",").map { it.toFloat() }.toFloatArray()
    }
    
    /**
     * Get all chunks as Flow for reactive updates
     */
    fun getAllChunksFlow(): Flow<List<DocumentChunkEntity>> {
        return documentChunkDao.getAllChunksFlow()
    }
}
