package com.localllm.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web search service using DuckDuckGo Instant Answer API
 * Provides web search results to augment LLM responses
 */
@Singleton
class WebSearchService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Search result data class
     */
    data class SearchResult(
        val title: String,
        val snippet: String,
        val url: String
    )
    
    /**
     * Web search response containing results and context
     */
    data class WebSearchResponse(
        val query: String,
        val results: List<SearchResult>,
        val abstractText: String?,
        val abstractSource: String?,
        val relatedTopics: List<String>,
        val success: Boolean,
        val error: String? = null
    )
    
    /**
     * Perform a web search using DuckDuckGo Instant Answer API
     * Returns structured results for LLM context augmentation
     */
    suspend fun search(query: String, maxResults: Int = 5): WebSearchResponse {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LocalLLM Android App")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext WebSearchResponse(
                        query = query,
                        results = emptyList(),
                        abstractText = null,
                        abstractSource = null,
                        relatedTopics = emptyList(),
                        success = false,
                        error = "HTTP ${response.code}: ${response.message}"
                    )
                }
                
                val jsonString = response.body?.string() ?: ""
                val json = JSONObject(jsonString)
                
                // Extract abstract (main answer)
                val abstractText = json.optString("Abstract").takeIf { it.isNotBlank() }
                val abstractSource = json.optString("AbstractSource").takeIf { it.isNotBlank() }
                
                // Extract related topics
                val relatedTopics = mutableListOf<String>()
                val relatedArray = json.optJSONArray("RelatedTopics")
                relatedArray?.let { array ->
                    for (i in 0 until minOf(array.length(), maxResults)) {
                        val topic = array.optJSONObject(i)
                        topic?.optString("Text")?.takeIf { it.isNotBlank() }?.let { text ->
                            relatedTopics.add(text)
                        }
                    }
                }
                
                // Extract search results
                val results = mutableListOf<SearchResult>()
                
                // Add abstract as first result if available
                if (!abstractText.isNullOrBlank()) {
                    results.add(SearchResult(
                        title = json.optString("Heading", query),
                        snippet = abstractText,
                        url = json.optString("AbstractURL", "")
                    ))
                }
                
                // Add related topics as results
                relatedArray?.let { array ->
                    for (i in 0 until minOf(array.length(), maxResults - results.size)) {
                        val topic = array.optJSONObject(i)
                        if (topic != null && topic.has("Text")) {
                            val text = topic.optString("Text", "")
                            val firstUrl = topic.optString("FirstURL", "")
                            if (text.isNotBlank()) {
                                results.add(SearchResult(
                                    title = extractTitle(text),
                                    snippet = text,
                                    url = firstUrl
                                ))
                            }
                        }
                    }
                }
                
                // If no results from DuckDuckGo, try alternative approach
                if (results.isEmpty()) {
                    // Fallback: use Wikipedia search
                    val wikiResults = searchWikipedia(query, maxResults)
                    results.addAll(wikiResults)
                }
                
                WebSearchResponse(
                    query = query,
                    results = results.take(maxResults),
                    abstractText = abstractText,
                    abstractSource = abstractSource,
                    relatedTopics = relatedTopics,
                    success = true
                )
                
            } catch (e: Exception) {
                WebSearchResponse(
                    query = query,
                    results = emptyList(),
                    abstractText = null,
                    abstractSource = null,
                    relatedTopics = emptyList(),
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Search Wikipedia as fallback
     */
    private suspend fun searchWikipedia(query: String, maxResults: Int): List<SearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&srlimit=$maxResults"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LocalLLM Android App")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return emptyList()
            
            val jsonString = response.body?.string() ?: ""
            val json = JSONObject(jsonString)
            
            val results = mutableListOf<SearchResult>()
            val searchArray = json.optJSONObject("query")?.optJSONArray("search")
            
            searchArray?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        val title = item.optString("title", "")
                        val snippet = item.optString("snippet", "")
                            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
                        
                        if (title.isNotBlank()) {
                            results.add(SearchResult(
                                title = title,
                                snippet = snippet,
                                url = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                            ))
                        }
                    }
                }
            }
            
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Extract a title from text content
     */
    private fun extractTitle(text: String): String {
        // Try to find a title pattern or use first 50 chars
        val dashIndex = text.indexOf(" - ")
        return if (dashIndex > 0 && dashIndex < 100) {
            text.substring(0, dashIndex)
        } else {
            text.take(50).let { if (text.length > 50) "$it..." else it }
        }
    }
    
    /**
     * Format search results for LLM context injection
     */
    fun formatResultsForLLM(response: WebSearchResponse): String {
        if (!response.success || response.results.isEmpty()) {
            return ""
        }
        
        val builder = StringBuilder()
        builder.append("[Web Search Results for \"${response.query}\"]\n\n")
        
        response.abstractText?.let {
            builder.append("Summary: $it\n")
            response.abstractSource?.let { source ->
                builder.append("Source: $source\n")
            }
            builder.append("\n")
        }
        
        if (response.results.isNotEmpty()) {
            builder.append("Related Information:\n")
            response.results.forEachIndexed { index, result ->
                builder.append("${index + 1}. ${result.title}\n")
                builder.append("   ${result.snippet}\n")
                if (result.url.isNotBlank()) {
                    builder.append("   URL: ${result.url}\n")
                }
                builder.append("\n")
            }
        }
        
        builder.append("[End of Web Search Results]\n\n")
        builder.append("Based on the above search results, please provide a comprehensive answer:\n")
        
        return builder.toString()
    }
    
    /**
     * Determine if a query would benefit from web search
     */
    fun shouldSearchWeb(query: String): Boolean {
        val searchTriggers = listOf(
            "what is", "who is", "when did", "where is", "how to",
            "latest", "recent", "news", "current", "today",
            "define", "meaning of", "explain", "tell me about",
            "search", "look up", "find out", "information about"
        )
        
        val lowerQuery = query.lowercase()
        return searchTriggers.any { lowerQuery.contains(it) }
    }
}
