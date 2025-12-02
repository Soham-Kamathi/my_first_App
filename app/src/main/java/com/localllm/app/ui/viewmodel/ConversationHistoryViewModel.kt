package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.repository.ConversationRepository
import com.localllm.app.domain.usecase.DeleteConversationUseCase
import com.localllm.app.domain.usecase.GetAllConversationsUseCase
import com.localllm.app.domain.usecase.SearchConversationsUseCase
import com.localllm.app.domain.usecase.TogglePinConversationUseCase
import com.localllm.app.domain.usecase.UpdateConversationTitleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the conversation history screen.
 * Handles listing, searching, and managing conversations.
 */
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val getAllConversationsUseCase: GetAllConversationsUseCase,
    private val searchConversationsUseCase: SearchConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val togglePinConversationUseCase: TogglePinConversationUseCase,
    private val updateConversationTitleUseCase: UpdateConversationTitleUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<Conversation>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                getAllConversationsUseCase()
            } else {
                searchConversationsUseCase(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Update the search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggle search mode.
     */
    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
        }
    }

    /**
     * Close search mode.
     */
    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    /**
     * Delete a conversation.
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationId)
        }
    }

    /**
     * Toggle pin status of a conversation.
     */
    fun togglePinConversation(conversationId: String) {
        viewModelScope.launch {
            togglePinConversationUseCase(conversationId)
        }
    }

    /**
     * Rename a conversation.
     */
    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            updateConversationTitleUseCase(conversationId, newTitle)
        }
    }

    /**
     * Get grouped conversations (pinned first, then by date).
     */
    fun getGroupedConversations(): Map<ConversationGroup, List<Conversation>> {
        val allConversations = conversations.value
        val grouped = mutableMapOf<ConversationGroup, List<Conversation>>()
        
        val pinned = allConversations.filter { it.isPinned }
        if (pinned.isNotEmpty()) {
            grouped[ConversationGroup.PINNED] = pinned
        }
        
        val unpinned = allConversations.filter { !it.isPinned }
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val weekMs = 7 * dayMs
        val monthMs = 30 * dayMs
        
        val today = unpinned.filter { now - it.updatedAt < dayMs }
        val thisWeek = unpinned.filter { (now - it.updatedAt) in dayMs until weekMs }
        val thisMonth = unpinned.filter { (now - it.updatedAt) in weekMs until monthMs }
        val older = unpinned.filter { now - it.updatedAt >= monthMs }
        
        if (today.isNotEmpty()) grouped[ConversationGroup.TODAY] = today
        if (thisWeek.isNotEmpty()) grouped[ConversationGroup.THIS_WEEK] = thisWeek
        if (thisMonth.isNotEmpty()) grouped[ConversationGroup.THIS_MONTH] = thisMonth
        if (older.isNotEmpty()) grouped[ConversationGroup.OLDER] = older
        
        return grouped
    }
}

/**
 * Grouping categories for conversations.
 */
enum class ConversationGroup(val displayName: String) {
    PINNED("Pinned"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    OLDER("Older")
}
