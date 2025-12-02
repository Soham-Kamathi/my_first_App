package com.localllm.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.localllm.app.data.model.AppTheme
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.StorageType
import com.localllm.app.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * DataStore-based storage for user preferences.
 */
@Singleton
class PreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val DEFAULT_SYSTEM_PROMPT = stringPreferencesKey("default_system_prompt")
        val PREFERRED_STORAGE_TYPE = stringPreferencesKey("preferred_storage_type")
        val AUTO_LOAD_LAST_MODEL = booleanPreferencesKey("auto_load_last_model")
        val SHOW_TOKENS_PER_SECOND = booleanPreferencesKey("show_tokens_per_second")
        val ENABLE_HAPTIC_FEEDBACK = booleanPreferencesKey("enable_haptic_feedback")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_ON_WIFI_ONLY = booleanPreferencesKey("download_on_wifi_only")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val USE_NNAPI = booleanPreferencesKey("use_nnapi")
        val USE_MMAP = booleanPreferencesKey("use_mmap")
        val CONTEXT_CACHE_ENABLED = booleanPreferencesKey("context_cache_enabled")
        
        // Generation config
        val GEN_MAX_TOKENS = intPreferencesKey("gen_max_tokens")
        val GEN_TEMPERATURE = floatPreferencesKey("gen_temperature")
        val GEN_TOP_P = floatPreferencesKey("gen_top_p")
        val GEN_TOP_K = intPreferencesKey("gen_top_k")
        val GEN_REPEAT_PENALTY = floatPreferencesKey("gen_repeat_penalty")
        val GEN_CONTEXT_SIZE = intPreferencesKey("gen_context_size")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToUserPreferences(preferences)
        }

    private fun mapPreferencesToUserPreferences(preferences: Preferences): UserPreferences {
        val theme = preferences[PreferencesKeys.THEME]?.let { 
            try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.SYSTEM }
        } ?: AppTheme.SYSTEM

        val storageType = preferences[PreferencesKeys.PREFERRED_STORAGE_TYPE]?.let {
            try { StorageType.valueOf(it) } catch (e: Exception) { StorageType.INTERNAL }
        } ?: StorageType.INTERNAL

        val generationConfig = GenerationConfig(
            maxTokens = preferences[PreferencesKeys.GEN_MAX_TOKENS] ?: 512,
            temperature = preferences[PreferencesKeys.GEN_TEMPERATURE] ?: 0.7f,
            topP = preferences[PreferencesKeys.GEN_TOP_P] ?: 0.9f,
            topK = preferences[PreferencesKeys.GEN_TOP_K] ?: 40,
            repeatPenalty = preferences[PreferencesKeys.GEN_REPEAT_PENALTY] ?: 1.1f,
            contextSize = preferences[PreferencesKeys.GEN_CONTEXT_SIZE] ?: 2048
        )

        return UserPreferences(
            theme = theme,
            defaultModelId = preferences[PreferencesKeys.DEFAULT_MODEL_ID],
            defaultSystemPrompt = preferences[PreferencesKeys.DEFAULT_SYSTEM_PROMPT] 
                ?: UserPreferences.DEFAULT_SYSTEM_PROMPT,
            defaultGenerationConfig = generationConfig,
            preferredStorageType = storageType,
            autoLoadLastModel = preferences[PreferencesKeys.AUTO_LOAD_LAST_MODEL] ?: true,
            showTokensPerSecond = preferences[PreferencesKeys.SHOW_TOKENS_PER_SECOND] ?: true,
            enableHapticFeedback = preferences[PreferencesKeys.ENABLE_HAPTIC_FEEDBACK] ?: true,
            keepScreenOnDuringGeneration = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
            maxConcurrentDownloads = preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] ?: 1,
            downloadOnWifiOnly = preferences[PreferencesKeys.DOWNLOAD_ON_WIFI_ONLY] ?: true,
            threadCount = preferences[PreferencesKeys.THREAD_COUNT] ?: 0,
            useNNAPI = preferences[PreferencesKeys.USE_NNAPI] ?: true,
            useMmap = preferences[PreferencesKeys.USE_MMAP] ?: true,
            contextCacheEnabled = preferences[PreferencesKeys.CONTEXT_CACHE_ENABLED] ?: true
        )
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateDefaultModel(modelId: String?) {
        context.dataStore.edit { preferences ->
            if (modelId != null) {
                preferences[PreferencesKeys.DEFAULT_MODEL_ID] = modelId
            } else {
                preferences.remove(PreferencesKeys.DEFAULT_MODEL_ID)
            }
        }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun updateGenerationConfig(config: GenerationConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEN_MAX_TOKENS] = config.maxTokens
            preferences[PreferencesKeys.GEN_TEMPERATURE] = config.temperature
            preferences[PreferencesKeys.GEN_TOP_P] = config.topP
            preferences[PreferencesKeys.GEN_TOP_K] = config.topK
            preferences[PreferencesKeys.GEN_REPEAT_PENALTY] = config.repeatPenalty
            preferences[PreferencesKeys.GEN_CONTEXT_SIZE] = config.contextSize
        }
    }

    suspend fun updateThreadCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THREAD_COUNT] = count
        }
    }

    suspend fun updateNNAPIEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_NNAPI] = enabled
        }
    }

    suspend fun updateMmapEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MMAP] = enabled
        }
    }

    suspend fun updatePreferredStorage(storageType: StorageType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_STORAGE_TYPE] = storageType.name
        }
    }

    suspend fun updateAutoLoadLastModel(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOAD_LAST_MODEL] = enabled
        }
    }

    suspend fun updateShowTokensPerSecond(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TOKENS_PER_SECOND] = show
        }
    }

    suspend fun updateKeepScreenOn(keepOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = keepOn
        }
    }

    suspend fun updateDownloadOnWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_ON_WIFI_ONLY] = wifiOnly
        }
    }
}
