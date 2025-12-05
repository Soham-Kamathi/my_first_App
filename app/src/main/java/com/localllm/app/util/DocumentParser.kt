package com.localllm.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parsed document result
 */
data class ParsedDocument(
    val fileName: String,
    val content: String,
    val pageCount: Int = 1,
    val wordCount: Int,
    val charCount: Int,
    val fileType: DocumentType
)

/**
 * Supported document types
 */
enum class DocumentType(val extensions: List<String>, val mimeTypes: List<String>) {
    PDF(listOf("pdf"), listOf("application/pdf")),
    TXT(listOf("txt"), listOf("text/plain")),
    MARKDOWN(listOf("md", "markdown"), listOf("text/markdown", "text/x-markdown")),
    CODE(listOf("kt", "java", "py", "js", "ts", "cpp", "c", "h", "xml", "json", "yaml", "yml"), 
         listOf("text/x-kotlin", "text/x-java", "text/x-python", "application/javascript", "application/json")),
    UNKNOWN(emptyList(), emptyList())
}

/**
 * Document text chunk for RAG-style querying
 */
data class TextChunk(
    val content: String,
    val index: Int,
    val startChar: Int,
    val endChar: Int
)

/**
 * Document Parser utility
 * Parses PDF, TXT, MD, and code files to extract text content
 */
@Singleton
class DocumentParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DocumentParser"
        private const val DEFAULT_CHUNK_SIZE = 1000  // chars per chunk
        private const val CHUNK_OVERLAP = 200  // overlap between chunks
    }

    private var isPdfBoxInitialized = false

    /**
     * Initialize PDFBox (call once on app start or lazy init)
     */
    fun initializePdfBox() {
        if (!isPdfBoxInitialized) {
            PDFBoxResourceLoader.init(context)
            isPdfBoxInitialized = true
            Log.d(TAG, "PDFBox initialized")
        }
    }

    /**
     * Parse document from URI
     */
    suspend fun parseDocument(uri: Uri, fileName: String): Result<ParsedDocument> = withContext(Dispatchers.IO) {
        try {
            val fileType = detectFileType(fileName, uri)
            Log.d(TAG, "Parsing document: $fileName, type: $fileType")

            val content = when (fileType) {
                DocumentType.PDF -> parsePdf(uri)
                DocumentType.TXT, DocumentType.MARKDOWN, DocumentType.CODE -> parseTextFile(uri)
                DocumentType.UNKNOWN -> parseTextFile(uri) // Try as text
            }

            val pageCount = if (fileType == DocumentType.PDF) getPdfPageCount(uri) else 1
            val wordCount = content.split(Regex("\\s+")).size
            val charCount = content.length

            Result.success(ParsedDocument(
                fileName = fileName,
                content = content,
                pageCount = pageCount,
                wordCount = wordCount,
                charCount = charCount,
                fileType = fileType
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse document: $fileName", e)
            Result.failure(e)
        }
    }

    /**
     * Parse PDF file
     */
    private suspend fun parsePdf(uri: Uri): String = withContext(Dispatchers.IO) {
        initializePdfBox()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } ?: throw IllegalStateException("Could not open PDF file")
    }

    /**
     * Get PDF page count
     */
    private suspend fun getPdfPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        initializePdfBox()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                document.numberOfPages
            }
        } ?: 1
    }

    /**
     * Parse text-based file (TXT, MD, code)
     */
    private suspend fun parseTextFile(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalStateException("Could not open text file")
    }

    /**
     * Detect file type from extension and MIME type
     */
    private fun detectFileType(fileName: String, uri: Uri): DocumentType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = context.contentResolver.getType(uri)

        return DocumentType.entries.find { type ->
            type.extensions.contains(extension) || type.mimeTypes.any { it == mimeType }
        } ?: DocumentType.UNKNOWN
    }

    /**
     * Split document into chunks for RAG-style context injection
     */
    fun chunkDocument(
        content: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = CHUNK_OVERLAP
    ): List<TextChunk> {
        if (content.length <= chunkSize) {
            return listOf(TextChunk(content, 0, 0, content.length))
        }

        val chunks = mutableListOf<TextChunk>()
        var startIndex = 0
        var chunkIndex = 0

        while (startIndex < content.length) {
            val endIndex = minOf(startIndex + chunkSize, content.length)
            
            // Try to break at sentence or paragraph boundary
            val adjustedEnd = if (endIndex < content.length) {
                findBreakPoint(content, startIndex, endIndex)
            } else {
                endIndex
            }

            val chunkContent = content.substring(startIndex, adjustedEnd).trim()
            if (chunkContent.isNotEmpty()) {
                chunks.add(TextChunk(
                    content = chunkContent,
                    index = chunkIndex,
                    startChar = startIndex,
                    endChar = adjustedEnd
                ))
                chunkIndex++
            }

            startIndex = adjustedEnd - overlap
            if (startIndex < 0) startIndex = 0
            if (adjustedEnd >= content.length) break
        }

        Log.d(TAG, "Document chunked into ${chunks.size} pieces")
        return chunks
    }

    /**
     * Find a good break point (paragraph, sentence, or word boundary)
     */
    private fun findBreakPoint(content: String, start: Int, maxEnd: Int): Int {
        // Look for paragraph break
        val paragraphBreak = content.lastIndexOf("\n\n", maxEnd)
        if (paragraphBreak > start + 100) return paragraphBreak + 2

        // Look for sentence break
        val sentenceBreak = content.lastIndexOf(". ", maxEnd)
        if (sentenceBreak > start + 100) return sentenceBreak + 2

        // Look for line break
        val lineBreak = content.lastIndexOf("\n", maxEnd)
        if (lineBreak > start + 100) return lineBreak + 1

        // Look for word break
        val wordBreak = content.lastIndexOf(" ", maxEnd)
        if (wordBreak > start + 50) return wordBreak + 1

        return maxEnd
    }

    /**
     * Find relevant chunks for a query (simple keyword matching)
     * For better results, use embeddings-based similarity in the future
     */
    fun findRelevantChunks(
        chunks: List<TextChunk>,
        query: String,
        maxChunks: Int = 3
    ): List<TextChunk> {
        val queryWords = query.lowercase().split(Regex("\\s+"))
            .filter { it.length > 2 }

        return chunks
            .map { chunk ->
                val chunkLower = chunk.content.lowercase()
                val score = queryWords.count { word ->
                    chunkLower.contains(word)
                }
                chunk to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxChunks)
            .map { it.first }
    }

    /**
     * Build context string from relevant chunks
     */
    fun buildContextFromChunks(chunks: List<TextChunk>, maxLength: Int = 3000): String {
        val sb = StringBuilder()
        for (chunk in chunks) {
            if (sb.length + chunk.content.length > maxLength) {
                val remaining = maxLength - sb.length
                if (remaining > 100) {
                    sb.append(chunk.content.take(remaining))
                    sb.append("...")
                }
                break
            }
            if (sb.isNotEmpty()) sb.append("\n\n---\n\n")
            sb.append(chunk.content)
        }
        return sb.toString()
    }

    /**
     * Get supported file extensions
     */
    fun getSupportedExtensions(): List<String> {
        return DocumentType.entries
            .filter { it != DocumentType.UNKNOWN }
            .flatMap { it.extensions }
    }

    /**
     * Get supported MIME types for file picker
     */
    fun getSupportedMimeTypes(): Array<String> {
        return arrayOf(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/*"
        )
    }
}
