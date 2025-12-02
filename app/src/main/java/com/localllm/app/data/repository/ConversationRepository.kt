package com.localllm.app.data.repository

import com.localllm.app.data.local.dao.ConversationDao
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.model.ConversationWithMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing conversations and messages.
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {
    /**
     * Get all conversations sorted by last updated.
     */
    fun getAllConversations(): Flow<List<Conversation>> = 
        conversationDao.getAllConversations()

    /**
     * Get pinned conversations.
     */
    fun getPinnedConversations(): Flow<List<Conversation>> = 
        conversationDao.getPinnedConversations()

    /**
     * Get a specific conversation by ID.
     */
    suspend fun getConversationById(conversationId: String): Conversation? = 
        conversationDao.getConversationById(conversationId)

    /**
     * Get a conversation with all its messages.
     */
    fun getConversationWithMessages(conversationId: String): Flow<ConversationWithMessages?> {
        return combine(
            conversationDao.getConversationByIdFlow(conversationId),
            conversationDao.getMessagesForConversation(conversationId)
        ) { conversation, messages ->
            conversation?.let {
                ConversationWithMessages(it, messages)
            }
        }
    }

    /**
     * Get messages for a conversation.
     */
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> = 
        conversationDao.getMessagesForConversation(conversationId)

    /**
     * Get messages for a conversation synchronously.
     */
    suspend fun getMessagesForConversationSync(conversationId: String): List<ChatMessage> =
        conversationDao.getMessagesForConversationSync(conversationId)

    /**
     * Create a new conversation.
     */
    suspend fun createConversation(
        title: String = "New Conversation",
        modelId: String? = null,
        systemPrompt: String? = null
    ): Conversation {
        val conversation = Conversation(
            title = title,
            modelId = modelId,
            systemPrompt = systemPrompt
        )
        conversationDao.insertConversation(conversation)
        return conversation
    }

    /**
     * Update a conversation.
     */
    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation)
    }

    /**
     * Update conversation title.
     */
    suspend fun updateConversationTitle(conversationId: String, title: String) {
        conversationDao.updateConversationTitle(conversationId, title)
    }

    /**
     * Toggle pinned status.
     */
    suspend fun togglePinned(conversationId: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updatePinnedStatus(conversationId, !it.isPinned)
        }
    }

    /**
     * Delete a conversation and all its messages.
     */
    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversationWithMessages(conversationId)
    }

    /**
     * Add a message to a conversation.
     */
    suspend fun addMessage(message: ChatMessage) {
        conversationDao.insertMessage(message)
        conversationDao.updateConversationTimestamp(message.conversationId)
    }

    /**
     * Update a message.
     */
    suspend fun updateMessage(message: ChatMessage) {
        conversationDao.updateMessage(message)
    }

    /**
     * Delete a message.
     */
    suspend fun deleteMessage(messageId: String) {
        conversationDao.deleteMessageById(messageId)
    }

    /**
     * Get the last message of a conversation.
     */
    suspend fun getLastMessage(conversationId: String): ChatMessage? = 
        conversationDao.getLastMessageForConversation(conversationId)

    /**
     * Get conversations for a specific model.
     */
    fun getConversationsForModel(modelId: String): Flow<List<Conversation>> = 
        conversationDao.getConversationsForModel(modelId)

    /**
     * Search conversations by content.
     */
    fun searchConversations(query: String): Flow<List<Conversation>> = 
        conversationDao.searchConversations(query)

    /**
     * Get message count for a conversation.
     */
    suspend fun getMessageCount(conversationId: String): Int = 
        conversationDao.getMessageCountForConversation(conversationId)

    /**
     * Generate a title for a conversation based on first user message.
     */
    suspend fun generateTitle(conversationId: String): String {
        val messages = conversationDao.getMessagesForConversationSync(conversationId)
        val firstUserMessage = messages.find { it.role == com.localllm.app.data.model.MessageRole.USER }
        
        return firstUserMessage?.content?.let { content ->
            if (content.length > 50) {
                content.take(47) + "..."
            } else {
                content
            }
        } ?: "New Conversation"
    }
}
