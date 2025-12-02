package com.localllm.app.di

import android.content.Context
import com.localllm.app.data.local.dao.ConversationDao
import com.localllm.app.data.local.dao.ModelDao
import com.localllm.app.data.remote.HuggingFaceApi
import com.localllm.app.data.remote.ModelCatalogApi
import com.localllm.app.data.repository.ConversationRepository
import com.localllm.app.data.repository.ModelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideModelRepository(
        @ApplicationContext context: Context,
        modelDao: ModelDao,
        modelCatalogApi: ModelCatalogApi,
        huggingFaceApi: HuggingFaceApi
    ): ModelRepository {
        return ModelRepository(context, modelDao, modelCatalogApi, huggingFaceApi)
    }
    
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao
    ): ConversationRepository {
        return ConversationRepository(conversationDao)
    }
}
