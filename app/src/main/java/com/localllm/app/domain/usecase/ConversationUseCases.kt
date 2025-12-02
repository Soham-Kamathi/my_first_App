package com.localllm.app.domain.usecase

import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.model.ConversationWithMessages
import com.localllm.app.data.model.MessageRole
import com.localllm.app.data.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all conversations.
 */
class GetAllConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(): Flow<List<Conversation>> = 
        conversationRepository.getAllConversations()
}

/**
 * Use case for getting a conversation with its messages.
 */
class GetConversationWithMessagesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(conversationId: String): Flow<ConversationWithMessages?> = 
        conversationRepository.getConversationWithMessages(conversationId)
}

/**
 * Use case for creating a new conversation.
 */
class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        title: String = "New Conversation",
        modelId: String? = null,
        systemPrompt: String? = null
    ): Conversation = conversationRepository.createConversation(title, modelId, systemPrompt)
}

/**
 * Use case for deleting a conversation.
 */
class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }
}

/**
 * Use case for adding a user message to a conversation.
 */
class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        role: MessageRole = MessageRole.USER
    ): ChatMessage {
        val message = ChatMessage(
            conversationId = conversationId,
            role = role,
            content = content
        )
        conversationRepository.addMessage(message)
        return message
    }
}

/**
 * Use case for updating a message (e.g., during streaming generation).
 */
class UpdateMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(message: ChatMessage) {
        conversationRepository.updateMessage(message)
    }
}

/**
 * Use case for searching conversations.
 */
class SearchConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(query: String): Flow<List<Conversation>> = 
        conversationRepository.searchConversations(query)
}

/**
 * Use case for toggling a conversation's pinned status.
 */
class TogglePinConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String) {
        conversationRepository.togglePinned(conversationId)
    }
}

/**
 * Use case for updating a conversation's title.
 */
class UpdateConversationTitleUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String, title: String) {
        conversationRepository.updateConversationTitle(conversationId, title)
    }
}

/**
 * Use case for auto-generating a title based on first message.
 */
class GenerateConversationTitleUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String): String {
        val title = conversationRepository.generateTitle(conversationId)
        conversationRepository.updateConversationTitle(conversationId, title)
        return title
    }
}

/**
 * Use case for getting messages for a conversation.
 */
class GetMessagesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> = 
        conversationRepository.getMessagesForConversation(conversationId)
}
