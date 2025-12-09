package com.localllm.app.data.remote

import android.util.Log
import com.localllm.app.data.model.WebSearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web search service using Tavily API as primary and multiple fallbacks
 * 
 * Tavily API provides high-quality search results optimized for LLM applications.
 * Get your API key at: https://tavily.com/
 * 
 * Search provider priority (when set to AUTO):
 * 1. Tavily API (if API key is configured) - Best for LLM context
 * 2. DuckDuckGo HTML scraping - Good general fallback
 * 3. DuckDuckGo Instant API - Good for factual queries
 * 4. Wikipedia - Final fallback for encyclopedic content
 */
@Singleton
class WebSearchService @Inject constructor() {
    
    companion object {
        private const val TAG = "WebSearchService"
        
        // Tavily API configuration
        private const val TAVILY_API_URL = "https://api.tavily.com/search"
        private const val TAVILY_DEFAULT_MAX_RESULTS = 5
        private const val TAVILY_SEARCH_DEPTH_BASIC = "basic"
        private const val TAVILY_SEARCH_DEPTH_ADVANCED = "advanced"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * Search result data class
     */
    data class SearchResult(
        val title: String,
        val snippet: String,
        val url: String,
        val score: Float = 0f,  // Relevancy score from Tavily
        val publishedDate: String? = null  // Publication date if available
    )
    
    /**
     * Web search response containing results and context
     */
    data class WebSearchResponse(
        val query: String,
        val results: List<SearchResult>,
        val abstractText: String?,      // LLM-generated answer from Tavily
        val abstractSource: String?,
        val relatedTopics: List<String>,
        val success: Boolean,
        val error: String? = null,
        val provider: String = "Unknown"  // Which provider returned results
    )
    
    /**
     * Perform a web search using the configured provider
     * 
     * @param query The search query
     * @param maxResults Maximum number of results to return
     * @param tavilyApiKey Tavily API key (optional, enables Tavily search)
     * @param provider Preferred search provider
     * @param includeAnswer Whether to include LLM-generated answer from Tavily
     * @param searchDepth Search depth for Tavily ("basic" or "advanced")
     */
    suspend fun search(
        query: String,
        maxResults: Int = 5,
        tavilyApiKey: String = "",
        provider: WebSearchProvider = WebSearchProvider.AUTO,
        includeAnswer: Boolean = true,
        searchDepth: String = TAVILY_SEARCH_DEPTH_BASIC
    ): WebSearchResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting web search for: \"$query\" with provider: $provider")
                
                when (provider) {
                    WebSearchProvider.TAVILY -> {
                        if (tavilyApiKey.isNotBlank()) {
                            return@withContext searchTavily(query, maxResults, tavilyApiKey, includeAnswer, searchDepth)
                        } else {
                            Log.w(TAG, "Tavily selected but no API key provided")
                            return@withContext createErrorResponse(query, "Tavily API key not configured")
                        }
                    }
                    WebSearchProvider.DUCKDUCKGO -> {
                        return@withContext searchDuckDuckGoWithFallback(query, maxResults)
                    }
                    WebSearchProvider.WIKIPEDIA -> {
                        val results = searchWikipedia(query, maxResults)
                        return@withContext WebSearchResponse(
                            query = query,
                            results = results,
                            abstractText = null,
                            abstractSource = "Wikipedia",
                            relatedTopics = emptyList(),
                            success = results.isNotEmpty(),
                            provider = "Wikipedia"
                        )
                    }
                    WebSearchProvider.AUTO -> {
                        // Try Tavily first if API key is available
                        if (tavilyApiKey.isNotBlank()) {
                            Log.d(TAG, "AUTO mode: Trying Tavily API first")
                            val tavilyResult = searchTavily(query, maxResults, tavilyApiKey, includeAnswer, searchDepth)
                            if (tavilyResult.success) {
                                return@withContext tavilyResult
                            }
                            Log.w(TAG, "Tavily failed, falling back to DuckDuckGo")
                        }
                        
                        // Fallback to DuckDuckGo
                        return@withContext searchDuckDuckGoWithFallback(query, maxResults)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Web search failed: ${e.message}", e)
                createErrorResponse(query, e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Search using Tavily API
     * 
     * Tavily provides:
     * - High-quality, LLM-optimized search results
     * - Optional AI-generated answers
     * - Relevancy scores for each result
     * - Clean, structured content snippets
     */
    private fun searchTavily(
        query: String,
        maxResults: Int,
        apiKey: String,
        includeAnswer: Boolean,
        searchDepth: String
    ): WebSearchResponse {
        return try {
            Log.d(TAG, "Searching Tavily API...")
            
            // Build request body
            val requestBody = JSONObject().apply {
                put("query", query)
                put("search_depth", searchDepth)
                put("max_results", maxResults)
                put("include_answer", if (includeAnswer) "basic" else false)
                put("include_raw_content", false)
                put("include_images", false)
            }
            
            Log.d(TAG, "Tavily request: ${requestBody.toString(2)}")
            
            val request = Request.Builder()
                .url(TAVILY_API_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "Tavily response code: ${response.code}")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Tavily API error: ${response.code} - $responseBody")
                return when (response.code) {
                    401 -> createErrorResponse(query, "Invalid Tavily API key")
                    429 -> createErrorResponse(query, "Tavily rate limit exceeded")
                    else -> createErrorResponse(query, "Tavily API error: ${response.code}")
                }
            }
            
            val json = JSONObject(responseBody)
            
            // Extract answer if available
            val answer = json.optString("answer").takeIf { it.isNotBlank() }
            
            // Extract results
            val results = mutableListOf<SearchResult>()
            val resultsArray = json.optJSONArray("results") ?: JSONArray()
            
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                results.add(SearchResult(
                    title = item.optString("title", ""),
                    snippet = item.optString("content", ""),
                    url = item.optString("url", ""),
                    score = item.optDouble("score", 0.0).toFloat(),
                    publishedDate = item.optString("published_date").takeIf { it.isNotBlank() }
                ))
            }
            
            Log.d(TAG, "Tavily returned ${results.size} results" + if (answer != null) " with answer" else "")
            
            WebSearchResponse(
                query = json.optString("query", query),
                results = results,
                abstractText = answer,
                abstractSource = "Tavily AI",
                relatedTopics = emptyList(),
                success = results.isNotEmpty() || answer != null,
                provider = "Tavily"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Tavily search failed: ${e.message}", e)
            createErrorResponse(query, "Tavily error: ${e.message}")
        }
    }
    
    /**
     * Search DuckDuckGo with multiple fallback strategies
     */
    private fun searchDuckDuckGoWithFallback(query: String, maxResults: Int): WebSearchResponse {
        // Try HTML scraping first (most reliable for general queries)
        val htmlResults = searchDuckDuckGoHtml(query, maxResults)
        if (htmlResults.isNotEmpty()) {
            Log.d(TAG, "Found ${htmlResults.size} results from DuckDuckGo HTML")
            return WebSearchResponse(
                query = query,
                results = htmlResults,
                abstractText = null,
                abstractSource = "DuckDuckGo",
                relatedTopics = emptyList(),
                success = true,
                provider = "DuckDuckGo"
            )
        }
        
        // Fallback to Instant Answer API (good for factual queries)
        Log.d(TAG, "Trying DuckDuckGo Instant Answer API")
        val instantResult = searchDuckDuckGoInstant(query, maxResults)
        if (instantResult.results.isNotEmpty()) {
            return instantResult.copy(provider = "DuckDuckGo Instant")
        }
        
        // Final fallback: Wikipedia
        Log.d(TAG, "Trying Wikipedia fallback")
        val wikiResults = searchWikipedia(query, maxResults)
        if (wikiResults.isNotEmpty()) {
            return WebSearchResponse(
                query = query,
                results = wikiResults,
                abstractText = null,
                abstractSource = "Wikipedia",
                relatedTopics = emptyList(),
                success = true,
                provider = "Wikipedia"
            )
        }
        
        Log.w(TAG, "No results found from any source")
        return createErrorResponse(query, "No results found from any search provider")
    }
    
    /**
     * Search DuckDuckGo by scraping HTML results (lite version)
     */
    private fun searchDuckDuckGoHtml(query: String, maxResults: Int): List<SearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            Log.d(TAG, "DuckDuckGo HTML URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "DuckDuckGo HTML failed: ${response.code}")
                return emptyList()
            }
            
            val html = response.body?.string() ?: ""
            Log.d(TAG, "Got HTML response, length: ${html.length}")
            
            val results = mutableListOf<SearchResult>()
            
            // Parse result blocks using regex
            val titlePattern = Regex(
                """<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]*)"[^>]*>([^<]+)</a>""",
                RegexOption.IGNORE_CASE
            )
            
            val snippetPattern = Regex(
                """<a[^>]*class="[^"]*result__snippet[^"]*"[^>]*>([^<]+)</a>""",
                RegexOption.IGNORE_CASE
            )
            
            val allTitles = titlePattern.findAll(html).toList()
            val allSnippets = snippetPattern.findAll(html).toList()
            
            Log.d(TAG, "Found ${allTitles.size} titles, ${allSnippets.size} snippets")
            
            for (i in 0 until minOf(allTitles.size, maxResults)) {
                try {
                    val titleMatch = allTitles[i]
                    var resultUrl = titleMatch.groupValues[1]
                    val title = titleMatch.groupValues[2].trim()
                    
                    // Decode DuckDuckGo redirect URL if needed
                    if (resultUrl.contains("duckduckgo.com/l/") || resultUrl.contains("uddg=")) {
                        val uddgMatch = Regex("uddg=([^&\"]+)").find(resultUrl)
                        uddgMatch?.let {
                            try {
                                resultUrl = URLDecoder.decode(it.groupValues[1], "UTF-8")
                            } catch (e: Exception) {
                                Log.w(TAG, "URL decode failed: ${e.message}")
                            }
                        }
                    }
                    
                    val snippet = if (i < allSnippets.size) {
                        allSnippets[i].groupValues[1].trim()
                    } else ""
                    
                    if (title.isNotBlank() && !title.startsWith("Ad")) {
                        results.add(SearchResult(
                            title = title,
                            snippet = snippet,
                            url = resultUrl
                        ))
                        Log.d(TAG, "Added result: $title")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse result $i: ${e.message}")
                }
            }
            
            Log.d(TAG, "Extracted ${results.size} results from HTML")
            results
        } catch (e: Exception) {
            Log.e(TAG, "DuckDuckGo HTML search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Search using DuckDuckGo Instant Answer API (good for factual queries)
     */
    private fun searchDuckDuckGoInstant(query: String, maxResults: Int): WebSearchResponse {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            
            Log.d(TAG, "DuckDuckGo Instant URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LocalLLM Android App/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            Log.d(TAG, "Instant API response code: ${response.code}")
            
            if (!response.isSuccessful) {
                return createErrorResponse(query, "DuckDuckGo HTTP ${response.code}")
            }
            
            val jsonString = response.body?.string() ?: ""
            val json = JSONObject(jsonString)
            
            val abstractText = json.optString("Abstract").takeIf { it.isNotBlank() }
            val abstractSource = json.optString("AbstractSource").takeIf { it.isNotBlank() }
            
            val results = mutableListOf<SearchResult>()
            
            // Add abstract as first result if available
            if (!abstractText.isNullOrBlank()) {
                results.add(SearchResult(
                    title = json.optString("Heading", query),
                    snippet = abstractText,
                    url = json.optString("AbstractURL", "")
                ))
            }
            
            // Add related topics
            val relatedTopics = mutableListOf<String>()
            val relatedArray = json.optJSONArray("RelatedTopics")
            relatedArray?.let { array ->
                for (i in 0 until minOf(array.length(), maxResults - results.size)) {
                    val topic = array.optJSONObject(i)
                    if (topic != null && topic.has("Text")) {
                        val text = topic.optString("Text", "")
                        val firstUrl = topic.optString("FirstURL", "")
                        if (text.isNotBlank()) {
                            relatedTopics.add(text)
                            results.add(SearchResult(
                                title = extractTitle(text),
                                snippet = text,
                                url = firstUrl
                            ))
                        }
                    }
                }
            }
            
            WebSearchResponse(
                query = query,
                results = results.take(maxResults),
                abstractText = abstractText,
                abstractSource = abstractSource,
                relatedTopics = relatedTopics,
                success = results.isNotEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "DuckDuckGo Instant failed: ${e.message}", e)
            createErrorResponse(query, "DuckDuckGo error: ${e.message}")
        }
    }
    
    /**
     * Search Wikipedia as fallback
     */
    private fun searchWikipedia(query: String, maxResults: Int): List<SearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&srlimit=$maxResults"
            
            Log.d(TAG, "Wikipedia URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LocalLLM Android App/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Wikipedia failed: ${response.code}")
                return emptyList()
            }
            
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
            
            Log.d(TAG, "Wikipedia returned ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Wikipedia search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Extract title from text (first sentence or phrase)
     */
    private fun extractTitle(text: String): String {
        val dashIndex = text.indexOf(" - ")
        return if (dashIndex > 0 && dashIndex < 100) {
            text.substring(0, dashIndex).trim()
        } else {
            text.take(60).let { 
                if (text.length > 60) "$it..." else it 
            }
        }
    }
    
    /**
     * Create an error response
     */
    private fun createErrorResponse(query: String, error: String): WebSearchResponse {
        return WebSearchResponse(
            query = query,
            results = emptyList(),
            abstractText = null,
            abstractSource = null,
            relatedTopics = emptyList(),
            success = false,
            error = error
        )
    }
    
    /**
     * Format search results for LLM context injection
     */
    fun formatResultsForLLM(response: WebSearchResponse): String {
        if (!response.success || (response.results.isEmpty() && response.abstractText == null)) {
            return ""
        }
        
        val builder = StringBuilder()
        builder.append("[Web Search Results for \"${response.query}\"]\n")
        builder.append("[Source: ${response.provider}]\n\n")
        
        // Include Tavily AI answer if available
        response.abstractText?.let {
            builder.append("AI Summary: $it\n")
            response.abstractSource?.let { source ->
                builder.append("(Source: $source)\n")
            }
            builder.append("\n")
        }
        
        if (response.results.isNotEmpty()) {
            builder.append("Search Results:\n")
            response.results.forEachIndexed { index, result ->
                builder.append("${index + 1}. ${result.title}")
                if (result.score > 0) {
                    builder.append(" (relevance: ${String.format("%.0f%%", result.score * 100)})")
                }
                builder.append("\n")
                if (result.snippet.isNotBlank()) {
                    builder.append("   ${result.snippet}\n")
                }
                if (result.url.isNotBlank()) {
                    builder.append("   URL: ${result.url}\n")
                }
                result.publishedDate?.let {
                    builder.append("   Published: $it\n")
                }
                builder.append("\n")
            }
        }
        
        builder.append("[End of Web Search Results]\n\n")
        builder.append("Based on the above search results, please provide an accurate and helpful answer to the user's question:\n\n")
        
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
            "search", "look up", "find out", "information about",
            "price", "weather", "stock", "score", "update",
            "2024", "2025", "this year", "last year"
        )
        
        val lowerQuery = query.lowercase()
        return searchTriggers.any { lowerQuery.contains(it) }
    }
}
