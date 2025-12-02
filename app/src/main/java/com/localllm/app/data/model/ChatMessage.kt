package com.localllm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single message in a conversation.
 */
@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensGenerated: Int = 0,
    val generationTimeMs: Long = 0,
    val isComplete: Boolean = true
) {
    /**
     * Returns tokens per second rate for this message (if it's an assistant message).
     */
    fun tokensPerSecond(): Double {
        return if (generationTimeMs > 0) {
            tokensGenerated * 1000.0 / generationTimeMs
        } else {
            0.0
        }
    }
}

/**
 * Enum representing the role of a message sender.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Data class combining a conversation with its messages.
 */
data class ConversationWithMessages(
    val conversation: Conversation,
    val messages: List<ChatMessage>
)
