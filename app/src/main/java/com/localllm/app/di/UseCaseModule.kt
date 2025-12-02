package com.localllm.app.di

import com.localllm.app.data.repository.ConversationRepository
import com.localllm.app.data.repository.ModelRepository
import com.localllm.app.domain.usecase.*
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.HardwareCapabilities
import com.localllm.app.util.ModelDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Model Use Cases
    
    @Provides
    @Singleton
    fun provideGetAllModelsUseCase(
        modelRepository: ModelRepository
    ): GetAllModelsUseCase {
        return GetAllModelsUseCase(modelRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetDownloadedModelsUseCase(
        modelRepository: ModelRepository
    ): GetDownloadedModelsUseCase {
        return GetDownloadedModelsUseCase(modelRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetAvailableModelsUseCase(
        modelRepository: ModelRepository
    ): GetAvailableModelsUseCase {
        return GetAvailableModelsUseCase(modelRepository)
    }
    
    @Provides
    @Singleton
    fun provideDeleteDownloadedModelUseCase(
        modelRepository: ModelRepository
    ): DeleteDownloadedModelUseCase {
        return DeleteDownloadedModelUseCase(modelRepository)
    }
    
    @Provides
    @Singleton
    fun provideRefreshModelCatalogUseCase(
        modelRepository: ModelRepository
    ): RefreshModelCatalogUseCase {
        return RefreshModelCatalogUseCase(modelRepository)
    }
    
    // Conversation Use Cases
    
    @Provides
    @Singleton
    fun provideGetAllConversationsUseCase(
        conversationRepository: ConversationRepository
    ): GetAllConversationsUseCase {
        return GetAllConversationsUseCase(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideCreateConversationUseCase(
        conversationRepository: ConversationRepository
    ): CreateConversationUseCase {
        return CreateConversationUseCase(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideDeleteConversationUseCase(
        conversationRepository: ConversationRepository
    ): DeleteConversationUseCase {
        return DeleteConversationUseCase(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetMessagesUseCase(
        conversationRepository: ConversationRepository
    ): GetMessagesUseCase {
        return GetMessagesUseCase(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        conversationRepository: ConversationRepository
    ): SendMessageUseCase {
        return SendMessageUseCase(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideGenerateResponseUseCase(
        inferenceEngine: InferenceEngine
    ): GenerateResponseUseCase {
        return GenerateResponseUseCase(inferenceEngine)
    }
}
