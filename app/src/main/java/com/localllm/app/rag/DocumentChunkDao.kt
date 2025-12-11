package com.localllm.app.rag

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Document chunk with vector embedding
 */
@Entity(
    tableName = "document_chunks",
    indices = [Index(value = ["documentId"]), Index(value = ["timestamp"])]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "documentId")
    val documentId: String,
    
    @ColumnInfo(name = "documentName")
    val documentName: String,
    
    @ColumnInfo(name = "chunkIndex")
    val chunkIndex: Int,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "embedding")
    val embedding: String, // Stored as comma-separated floats for Room compatibility
    
    @ColumnInfo(name = "startChar")
    val startChar: Int,
    
    @ColumnInfo(name = "endChar")
    val endChar: Int,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Search result with similarity score
 */
data class ChunkSearchResult(
    val chunk: DocumentChunkEntity,
    val similarity: Float
)

/**
 * DAO for document chunks and vector search
 */
@Dao
interface DocumentChunkDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: DocumentChunkEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)
    
    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksByDocument(documentId: String): List<DocumentChunkEntity>
    
    @Query("SELECT * FROM document_chunks ORDER BY timestamp DESC")
    fun getAllChunksFlow(): Flow<List<DocumentChunkEntity>>
    
    @Query("SELECT * FROM document_chunks ORDER BY timestamp DESC")
    suspend fun getAllChunks(): List<DocumentChunkEntity>
    
    @Query("SELECT DISTINCT documentId, documentName FROM document_chunks ORDER BY timestamp DESC")
    suspend fun getIndexedDocuments(): List<DocumentInfo>
    
    @Query("SELECT COUNT(*) FROM document_chunks WHERE documentId = :documentId")
    suspend fun getChunkCount(documentId: String): Int
    
    @Delete
    suspend fun deleteChunk(chunk: DocumentChunkEntity)
    
    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocument(documentId: String)
    
    @Query("DELETE FROM document_chunks")
    suspend fun deleteAllChunks()
    
    @Query("SELECT * FROM document_chunks WHERE content LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchByKeyword(query: String, limit: Int = 10): List<DocumentChunkEntity>
}

/**
 * Document info for listing indexed documents
 */
data class DocumentInfo(
    val documentId: String,
    val documentName: String
)
