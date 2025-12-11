package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.data.repository.ConversationRepository
import com.localllm.app.data.repository.ModelRepository
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for the Home Screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val modelRepository: ModelRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    data class RecentConversation(
        val id: String,
        val title: String,
        val lastMessage: String,
        val timeAgo: String,
        val timestamp: Long
    )

    val currentModel: StateFlow<ModelInfo?> = modelManager.currentModel

    val modelLoadingState: StateFlow<ModelLoadingState> = modelManager.loadingState

    private val _recentConversations = MutableStateFlow<List<RecentConversation>>(emptyList())
    val recentConversations: StateFlow<List<RecentConversation>> = _recentConversations.asStateFlow()

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<RecentConversation>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                conversationRepository.searchConversations(query).map { conversations ->
                    conversations.map { conversation ->
                        // Get last message for preview
                        val messages = conversationRepository.getMessagesForConversationSync(conversation.id)
                        val lastMessage = messages.lastOrNull()?.content ?: "No messages"
                        
                        RecentConversation(
                            id = conversation.id,
                            title = conversation.title,
                            lastMessage = lastMessage.take(100),
                            timeAgo = getTimeAgo(conversation.updatedAt),
                            timestamp = conversation.updatedAt
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecentConversations()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    private fun loadRecentConversations() {
        viewModelScope.launch {
            try {
                val conversations = conversationRepository.getAllConversations().first()
                    .sortedByDescending { it.updatedAt }
                    .take(5)
                    .map { conversation ->
                        // Get last message for preview
                        val messages = conversationRepository.getMessagesForConversation(conversation.id).first()
                        val lastMessage = messages.lastOrNull()?.content ?: "No messages"
                        
                        RecentConversation(
                            id = conversation.id,
                            title = conversation.title,
                            lastMessage = lastMessage.take(100),
                            timeAgo = getTimeAgo(conversation.updatedAt),
                            timestamp = conversation.updatedAt
                        )
                    }
                _recentConversations.value = conversations
            } catch (e: Exception) {
                // Silently handle - recent conversations are optional
            }
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hr ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days days ago"
            }
            else -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
