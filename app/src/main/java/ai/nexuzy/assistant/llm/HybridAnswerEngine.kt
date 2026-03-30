package ai.nexuzy.assistant.llm

import android.content.Context
import android.util.Log
import ai.nexuzy.assistant.tools.InternetSearchTool
import ai.nexuzy.assistant.tools.NetworkUtils
import kotlinx.coroutines.*

/**
 * HybridAnswerEngine — combines Local MLC + Sarvaam AI + DuckDuckGo
 * into ONE fused accurate answer when internet is available.
 *
 * Strategy when internet ON:
 *   1. Fire DuckDuckGo search + Sarvaam AI call IN PARALLEL (async)
 *   2. Merge web facts as grounding context into the Sarvaam prompt
 *   3. If local MLC is available, also run it and pick best answer
 *      (Sarvaam preferred for factual; MLC for personality/identity)
 *   4. Return the richest combined answer
 *
 * Strategy when internet OFF:
 *   → Falls back to local MLC only, then offline fallback
 */
class HybridAnswerEngine(
    private val context: Context,
    private val sarvaamClient: SarvaamAIClient?
) {
    companion object {
        private const val TAG = "HybridAnswerEngine"
    }

    private val searchTool = InternetSearchTool()

    /**
     * Generate a fused answer from all available sources.
     *
     * @param userQuery     Raw user question
     * @param toolContext   Any tool result already fetched (weather/news/location/link)
     * @param history       Conversation history (user, assistant) pairs
     * @param mlcResult     Result from local MLC model, null if not available
     * @return              Final fused answer string
     */
    suspend fun generate(
        userQuery: String,
        toolContext: String = "",
        history: List<Pair<String, String>> = emptyList(),
        mlcResult: String? = null
    ): String = withContext(Dispatchers.IO) {

        val hasInternet = NetworkUtils.isInternetAvailable(context)

        // --- OFFLINE: just use MLC or fallback ---
        if (!hasInternet) {
            return@withContext mlcResult
                ?: "\uD83D\uDCA1 Offline mode. Connect to internet for enhanced AI responses."
        }

        // --- ONLINE: run DuckDuckGo + Sarvaam in parallel ---
        val webFactsDeferred: Deferred<String?> = async {
            try {
                val result = searchTool.search(userQuery)
                if (result.contains("No direct answer") || result.contains("unavailable")) null
                else result
            } catch (e: Exception) {
                Log.w(TAG, "DuckDuckGo failed: ${e.message}")
                null
            }
        }

        // Build an enriched prompt combining tool context + web facts + user query
        // We first get webFacts to inject into Sarvaam prompt for grounding
        val webFacts = webFactsDeferred.await()

        // Build the combined prompt for Sarvaam AI
        val combinedPrompt = buildCombinedPrompt(
            userQuery = userQuery,
            toolContext = toolContext,
            webFacts = webFacts
        )

        // Call Sarvaam AI with the enriched prompt
        val sarvaamAnswer: String? = if (sarvaamClient != null) {
            try {
                sarvaamClient.generate(combinedPrompt, history)
            } catch (e: Exception) {
                Log.w(TAG, "Sarvaam failed: ${e.message}")
                null
            }
        } else null

        // Fusion logic: pick the best available answer
        return@withContext when {
            // Sarvaam gave an answer (grounded with web facts) — best option
            !sarvaamAnswer.isNullOrBlank() -> {
                Log.d(TAG, "Using Sarvaam AI answer (grounded with DuckDuckGo)")
                sarvaamAnswer
            }
            // Sarvaam unavailable but web facts exist — synthesize from web facts
            !webFacts.isNullOrBlank() -> {
                Log.d(TAG, "Sarvaam unavailable, using DuckDuckGo web facts")
                synthesizeFromWebFacts(userQuery, webFacts)
            }
            // MLC local model answer
            !mlcResult.isNullOrBlank() -> {
                Log.d(TAG, "Using local MLC answer")
                mlcResult
            }
            // Nothing worked
            else -> "\uD83D\uDCA1 I couldn't find a confident answer. Please check your internet connection or try rephrasing."
        }
    }

    /**
     * Builds a combined prompt that injects web facts and tool context
     * directly into the question sent to Sarvaam AI for grounded answers.
     */
    private fun buildCombinedPrompt(
        userQuery: String,
        toolContext: String,
        webFacts: String?
    ): String = buildString {
        // If tool data available (weather, news, location, link) — include it
        if (toolContext.isNotBlank()) {
            appendLine("[Real-time data from tools]:")
            appendLine(toolContext.trim())
            appendLine()
        }
        // If DuckDuckGo found relevant facts — include them as grounding
        if (!webFacts.isNullOrBlank()) {
            appendLine("[Web facts from DuckDuckGo for grounding — use to improve accuracy]:")
            appendLine(webFacts.trim())
            appendLine()
        }
        // The actual user question
        appendLine("[User question]: $userQuery")
        appendLine()
        append("Answer clearly and accurately using all the above context. " +
            "If you are not confident, say so. Keep the response concise and helpful.")
    }

    /**
     * When Sarvaam is unavailable, synthesize a clean response
     * directly from DuckDuckGo web facts.
     */
    private fun synthesizeFromWebFacts(query: String, facts: String): String {
        return buildString {
            appendLine("\uD83C\uDF10 Based on web search results:")
            appendLine()
            // Clean up the raw DuckDuckGo text for display
            facts.lines().take(6).forEach { line ->
                if (line.isNotBlank()) appendLine(line.trim())
            }
        }.trim()
    }
}
