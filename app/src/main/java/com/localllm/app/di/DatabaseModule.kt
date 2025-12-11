package com.localllm.app.di

import android.content.Context
import androidx.room.Room
import com.localllm.app.data.local.LocalLLMDatabase
import com.localllm.app.data.local.PreferencesDataStore
import com.localllm.app.data.local.dao.ConversationDao
import com.localllm.app.data.local.dao.ModelDao
import com.localllm.app.rag.DocumentChunkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LocalLLMDatabase {
        return Room.databaseBuilder(
            context,
            LocalLLMDatabase::class.java,
            "local_llm_database"
        )
            .addMigrations(
                LocalLLMDatabase.MIGRATION_1_2,
                LocalLLMDatabase.MIGRATION_2_3,
                LocalLLMDatabase.MIGRATION_3_4,
                LocalLLMDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideModelDao(database: LocalLLMDatabase): ModelDao {
        return database.modelDao()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: LocalLLMDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    @Singleton
    fun provideDocumentChunkDao(database: LocalLLMDatabase): DocumentChunkDao {
        return database.documentChunkDao()
    }
    
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): PreferencesDataStore {
        return PreferencesDataStore(context)
    }
}
