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

/**
 * Main Room database for the LocalLLM app.
 */
@Database(
    entities = [
        ModelInfo::class,
        Conversation::class,
        ChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ModelTypeConverters::class, MessageRoleConverter::class)
abstract class LocalLLMDatabase : RoomDatabase() {
    
    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao

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
