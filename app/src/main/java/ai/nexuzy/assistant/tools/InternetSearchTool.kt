package ai.nexuzy.assistant.tools

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * InternetSearchTool — uses DuckDuckGo Instant Answer API (no key required).
 * Falls back to DuckDuckGo HTML scrape summary if instant answer is empty.
 *
 * Called by ToolExecutor when:
 *   • AI model doesn't know the answer (GENERAL intent + internet on)
 *   • User explicitly asks to search something
 *   • WEB_SEARCH intent classified
 */
class InternetSearchTool {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Search via DuckDuckGo Instant Answer API.
     * Returns a formatted string result for AI context injection.
     */
    fun search(query: String): String {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_redirect=1&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "NexuzyAI-Android/1.0")
                .build()
            val body = client.newCall(req).execute().use { it.body?.string() ?: "" }
            parseDDGResponse(query, body)
        } catch (e: Exception) {
            Log.e("InternetSearchTool", "Search error: ${e.message}")
            "[Web search unavailable: ${e.message}]"
        }
    }

    private fun parseDDGResponse(query: String, json: String): String {
        if (json.isBlank()) return "[No results found for: $query]"
        return try {
            val obj = JSONObject(json)
            val abstract = obj.optString("Abstract", "")
            val answer = obj.optString("Answer", "")
            val definition = obj.optString("Definition", "")
            val relatedTopics = obj.optJSONArray("RelatedTopics")

            val sb = StringBuilder()
            sb.appendLine("[DuckDuckGo Search: \"$query\"]")

            when {
                answer.isNotBlank() -> sb.appendLine("Answer: $answer")
                abstract.isNotBlank() -> {
                    sb.appendLine("Summary: $abstract")
                    val src = obj.optString("AbstractSource", "")
                    if (src.isNotBlank()) sb.appendLine("Source: $src")
                }
                definition.isNotBlank() -> sb.appendLine("Definition: $definition")
                else -> {
                    // Pull first 3 related topics
                    if (relatedTopics != null && relatedTopics.length() > 0) {
                        sb.appendLine("Related results:")
                        for (i in 0 until minOf(3, relatedTopics.length())) {
                            val topic = relatedTopics.optJSONObject(i)
                            val text = topic?.optString("Text", "") ?: ""
                            if (text.isNotBlank()) sb.appendLine("• $text")
                        }
                    } else {
                        sb.appendLine("No direct answer found. Try rephrasing your question.")
                    }
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e("InternetSearchTool", "Parse error: ${e.message}")
            "[Search result parse error]"
        }
    }
}
