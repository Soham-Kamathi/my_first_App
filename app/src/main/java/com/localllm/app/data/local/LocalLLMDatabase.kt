package com.localllm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localllm.app.data.local.dao.ConversationDao
import com.localllm.app.data.local.dao.ModelDao
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.model.ModelTypeConverters
import com.localllm.app.rag.DocumentChunkDao
import com.localllm.app.rag.DocumentChunkEntity

/**
 * Main Room database for the LocalLLM app.
 */
@Database(
    entities = [
        ModelInfo::class,
        Conversation::class,
        ChatMessage::class,
        DocumentChunkEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(ModelTypeConverters::class, MessageRoleConverter::class)
abstract class LocalLLMDatabase : RoomDatabase() {
    
    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao
    abstract fun documentChunkDao(): DocumentChunkDao

    companion object {
        const val DATABASE_NAME = "localllm_database"
        
        /**
         * Migration from version 1 to 2: Add metadata columns to models table.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE models ADD COLUMN author TEXT")
                database.execSQL("ALTER TABLE models ADD COLUMN modelFamily TEXT")
                database.execSQL("ALTER TABLE models ADD COLUMN downloads INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE models ADD COLUMN likes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE models ADD COLUMN pipelineTag TEXT")
                database.execSQL("ALTER TABLE models ADD COLUMN lastModified TEXT")
            }
        }
        
        /**
         * Migration from version 2 to 3: Add vision support columns to models table.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE models ADD COLUMN supportsVision INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE models ADD COLUMN visionProjectorUrl TEXT")
            }
        }
        
        /**
         * Migration from version 3 to 4: Add Whisper/audio model support columns.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE models ADD COLUMN modelType TEXT NOT NULL DEFAULT 'text-generation'")
                database.execSQL("ALTER TABLE models ADD COLUMN isWhisper INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        /**
         * Migration from version 4 to 5: Add RAG vector store table for document chunks.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS document_chunks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId TEXT NOT NULL,
                        documentName TEXT NOT NULL,
                        chunkIndex INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        embedding TEXT NOT NULL,
                        startChar INTEGER NOT NULL,
                        endChar INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_documentId ON document_chunks(documentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_timestamp ON document_chunks(timestamp)")
            }
        }
    }
}

/**
 * TypeConverter for MessageRole enum.
 */
class MessageRoleConverter {
    @androidx.room.TypeConverter
    fun fromMessageRole(role: com.localllm.app.data.model.MessageRole): String {
        return role.name
    }

    @androidx.room.TypeConverter
    fun toMessageRole(value: String): com.localllm.app.data.model.MessageRole {
        return com.localllm.app.data.model.MessageRole.valueOf(value)
    }
}
