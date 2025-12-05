package com.localllm.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.localllm.app.data.model.ChatMessage
import com.localllm.app.data.model.Conversation
import com.localllm.app.data.model.MessageRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export format options
 */
enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    MARKDOWN("md", "text/markdown", "Markdown"),
    PLAIN_TEXT("txt", "text/plain", "Plain Text"),
    JSON("json", "application/json", "JSON")
}

/**
 * Conversation Exporter utility
 * Exports conversations to various formats for sharing
 */
@Singleton
class ConversationExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConversationExporter"
        private const val EXPORT_FOLDER = "exports"
    }

    /**
     * Export conversation to specified format
     */
    suspend fun export(
        conversation: Conversation,
        messages: List<ChatMessage>,
        format: ExportFormat
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val content = when (format) {
                ExportFormat.MARKDOWN -> formatAsMarkdown(conversation, messages)
                ExportFormat.PLAIN_TEXT -> formatAsPlainText(conversation, messages)
                ExportFormat.JSON -> formatAsJson(conversation, messages)
            }

            val fileName = generateFileName(conversation, format)
            val file = saveToFile(fileName, content)

            Log.d(TAG, "Exported conversation to: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Format conversation as Markdown
     */
    private fun formatAsMarkdown(conversation: Conversation, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

        // Header
        sb.appendLine("# ${conversation.title}")
        sb.appendLine()
        sb.appendLine("**Date:** ${dateFormat.format(Date(conversation.createdAt))}")
        sb.appendLine("**Messages:** ${messages.size}")
        if (conversation.modelId != null) {
            sb.appendLine("**Model:** ${conversation.modelId}")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        // System prompt if present
        conversation.systemPrompt?.let { prompt ->
            sb.appendLine("## System Prompt")
            sb.appendLine()
            sb.appendLine("> $prompt")
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }

        // Messages
        sb.appendLine("## Conversation")
        sb.appendLine()

        for (message in messages) {
            val roleEmoji = when (message.role) {
                MessageRole.USER -> "👤"
                MessageRole.ASSISTANT -> "🤖"
                MessageRole.SYSTEM -> "⚙️"
            }
            val roleName = when (message.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }

            sb.appendLine("### $roleEmoji $roleName")
            sb.appendLine()
            sb.appendLine(message.content)
            sb.appendLine()

            // Add generation stats for assistant messages
            if (message.role == MessageRole.ASSISTANT && message.tokensGenerated > 0) {
                val tokensPerSec = if (message.generationTimeMs > 0) {
                    message.tokensGenerated * 1000.0 / message.generationTimeMs
                } else 0.0
                sb.appendLine("*${message.tokensGenerated} tokens, ${String.format("%.1f", tokensPerSec)} tok/s*")
                sb.appendLine()
            }
        }

        // Footer
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("*Exported from LocalLLM on ${dateFormat.format(Date())}*")

        return sb.toString()
    }

    /**
     * Format conversation as plain text
     */
    private fun formatAsPlainText(conversation: Conversation, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

        // Header
        sb.appendLine("═".repeat(60))
        sb.appendLine(conversation.title.uppercase())
        sb.appendLine("═".repeat(60))
        sb.appendLine()
        sb.appendLine("Date: ${dateFormat.format(Date(conversation.createdAt))}")
        sb.appendLine("Messages: ${messages.size}")
        conversation.modelId?.let { sb.appendLine("Model: $it") }
        sb.appendLine()

        // System prompt
        conversation.systemPrompt?.let { prompt ->
            sb.appendLine("─".repeat(40))
            sb.appendLine("SYSTEM PROMPT:")
            sb.appendLine(prompt)
            sb.appendLine()
        }

        sb.appendLine("─".repeat(40))
        sb.appendLine("CONVERSATION:")
        sb.appendLine()

        // Messages
        for (message in messages) {
            val roleName = when (message.role) {
                MessageRole.USER -> "USER"
                MessageRole.ASSISTANT -> "ASSISTANT"
                MessageRole.SYSTEM -> "SYSTEM"
            }

            sb.appendLine("[$roleName]")
            sb.appendLine(message.content)
            sb.appendLine()
        }

        // Footer
        sb.appendLine("─".repeat(40))
        sb.appendLine("Exported from LocalLLM on ${dateFormat.format(Date())}")

        return sb.toString()
    }

    /**
     * Format conversation as JSON
     */
    private fun formatAsJson(conversation: Conversation, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"conversation\": {")
        sb.appendLine("    \"id\": \"${conversation.id}\",")
        sb.appendLine("    \"title\": \"${escapeJson(conversation.title)}\",")
        sb.appendLine("    \"createdAt\": ${conversation.createdAt},")
        sb.appendLine("    \"updatedAt\": ${conversation.updatedAt},")
        conversation.modelId?.let {
            sb.appendLine("    \"modelId\": \"$it\",")
        }
        conversation.systemPrompt?.let {
            sb.appendLine("    \"systemPrompt\": \"${escapeJson(it)}\",")
        }
        sb.appendLine("    \"messageCount\": ${messages.size}")
        sb.appendLine("  },")
        sb.appendLine("  \"messages\": [")

        messages.forEachIndexed { index, message ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${message.id}\",")
            sb.appendLine("      \"role\": \"${message.role.name}\",")
            sb.appendLine("      \"content\": \"${escapeJson(message.content)}\",")
            sb.appendLine("      \"timestamp\": ${message.timestamp},")
            sb.appendLine("      \"tokensGenerated\": ${message.tokensGenerated},")
            sb.appendLine("      \"generationTimeMs\": ${message.generationTimeMs}")
            sb.append("    }")
            if (index < messages.size - 1) sb.append(",")
            sb.appendLine()
        }

        sb.appendLine("  ],")
        sb.appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
        sb.appendLine("  \"exportedBy\": \"LocalLLM\"")
        sb.appendLine("}")

        return sb.toString()
    }

    /**
     * Escape special characters for JSON
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Generate filename for export
     */
    private fun generateFileName(conversation: Conversation, format: ExportFormat): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val safeTitle = conversation.title
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(30)
        return "${safeTitle}_$timestamp.${format.extension}"
    }

    /**
     * Save content to file in exports folder
     */
    private suspend fun saveToFile(fileName: String, content: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, EXPORT_FOLDER)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)
        file.writeText(content)
        file
    }

    /**
     * Get shareable URI for file using FileProvider
     */
    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Create share intent for exported file
     */
    fun createShareIntent(file: File, format: ExportFormat): Intent {
        val uri = getShareableUri(file)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Conversation Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Copy conversation to clipboard as text
     */
    fun formatForClipboard(conversation: Conversation, messages: List<ChatMessage>): String {
        return formatAsPlainText(conversation, messages)
    }

    /**
     * Clean up old export files
     */
    suspend fun cleanupOldExports(maxAgeMs: Long = 24 * 60 * 60 * 1000) = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, EXPORT_FOLDER)
        if (exportDir.exists()) {
            val cutoff = System.currentTimeMillis() - maxAgeMs
            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.delete()
                    Log.d(TAG, "Deleted old export: ${file.name}")
                }
            }
        }
    }
}
