package com.localllm.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LocalLLMApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any app-wide configurations here
        initializeNativeLibrary()
    }
    
    private fun initializeNativeLibrary() {
        try {
            System.loadLibrary("llama-android")
        } catch (e: UnsatisfiedLinkError) {
            // Library not available - running in stub mode
            android.util.Log.w("LocalLLMApplication", "Native library not available: ${e.message}")
        }
    }
}
