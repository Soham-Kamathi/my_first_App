package com.localllm.app.di

import android.content.Context
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.LlamaAndroid
import com.localllm.app.inference.ModelManager
import com.localllm.app.util.BatteryMonitor
import com.localllm.app.util.HardwareCapabilities
import com.localllm.app.util.MemoryMonitor
import com.localllm.app.util.ModelDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    @Provides
    @Singleton
    fun provideLlamaAndroid(): LlamaAndroid {
        return LlamaAndroid()
    }
    
    @Provides
    @Singleton
    fun provideModelManager(
        llamaAndroid: LlamaAndroid
    ): ModelManager {
        return ModelManager(llamaAndroid)
    }
    
    @Provides
    @Singleton
    fun provideInferenceEngine(
        llamaAndroid: LlamaAndroid,
        modelManager: ModelManager
    ): InferenceEngine {
        return InferenceEngine(llamaAndroid, modelManager)
    }
    
    @Provides
    @Singleton
    fun provideMemoryMonitor(
        @ApplicationContext context: Context
    ): MemoryMonitor {
        return MemoryMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideBatteryMonitor(
        @ApplicationContext context: Context
    ): BatteryMonitor {
        return BatteryMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideHardwareCapabilities(
        @ApplicationContext context: Context,
        memoryMonitor: MemoryMonitor
    ): HardwareCapabilities {
        return HardwareCapabilities(context, memoryMonitor)
    }
    
    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        hardwareCapabilities: HardwareCapabilities
    ): ModelDownloader {
        return ModelDownloader(context, hardwareCapabilities)
    }
}
