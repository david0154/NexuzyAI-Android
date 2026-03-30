package ai.nexuzy.assistant.llm

import android.content.Context
import android.util.Log
import ai.nexuzy.assistant.tools.InternetSearchTool
import ai.nexuzy.assistant.tools.NetworkUtils
import kotlinx.coroutines.*

/**
 * HybridAnswerEngine — smart routing engine:
 *
 *  INTERNET ON  →  DuckDuckGo + Sarvaam AI run IN PARALLEL → fused answer
 *                   (MLC result also merged in if available)
 *
 *  INTERNET OFF →  LocalOfflineEngine (MLC if built, else smart rule-based NLP)
 *                   → NEVER returns an empty or useless message
 */
class HybridAnswerEngine(
    private val context: Context,
    private val sarvaamClient: SarvaamAIClient?
) {
    companion object {
        private const val TAG = "HybridAnswerEngine"
    }

    private val searchTool    = InternetSearchTool()
    private val offlineEngine = LocalOfflineEngine(context)

    suspend fun generate(
        userQuery: String,
        toolContext: String = "",
        history: List<Pair<String, String>> = emptyList(),
        mlcResult: String? = null,
        fullPrompt: String = ""
    ): String = withContext(Dispatchers.IO) {

        val hasInternet = NetworkUtils.isInternetAvailable(context)

        // ══ OFFLINE PATH ══════════════════════════════════════════
        if (!hasInternet) {
            Log.d(TAG, "Offline mode — using LocalOfflineEngine")
            // If MLC already generated a result, prefer it
            return@withContext if (!mlcResult.isNullOrBlank()) {
                Log.d(TAG, "Offline: returning MLC result")
                mlcResult
            } else {
                // Use smart offline engine (rule-based NLP + tool context)
                Log.d(TAG, "Offline: using LocalOfflineEngine rule-based NLP")
                offlineEngine.generate(
                    userQuery   = userQuery,
                    toolContext = toolContext,
                    prompt      = fullPrompt
                )
            }
        }

        // ══ ONLINE PATH ══════════════════════════════════════════
        // Fire DuckDuckGo search async (non-blocking)
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

        // Wait for web facts, then inject into Sarvaam prompt for grounding
        val webFacts = webFactsDeferred.await()

        // Build grounded combined prompt
        val combinedPrompt = buildCombinedPrompt(
            userQuery   = userQuery,
            toolContext = toolContext,
            webFacts    = webFacts
        )

        // Call Sarvaam AI with grounded prompt
        val sarvaamAnswer: String? = sarvaamClient?.let {
            try {
                it.generate(combinedPrompt, history)
            } catch (e: Exception) {
                Log.w(TAG, "Sarvaam failed: ${e.message}")
                null
            }
        }

        // Fusion: best available answer
        when {
            !sarvaamAnswer.isNullOrBlank() -> {
                Log.d(TAG, "Online: Sarvaam AI answer (grounded with DuckDuckGo)")
                sarvaamAnswer
            }
            !webFacts.isNullOrBlank() -> {
                Log.d(TAG, "Online: DuckDuckGo web facts synthesis")
                synthesizeFromWebFacts(userQuery, webFacts)
            }
            !mlcResult.isNullOrBlank() -> {
                Log.d(TAG, "Online: using MLC result (Sarvaam + DDG unavailable)")
                mlcResult
            }
            else -> {
                // Internet on but all sources failed — still use offline engine
                Log.w(TAG, "All online sources failed, falling back to LocalOfflineEngine")
                offlineEngine.generate(userQuery, toolContext, fullPrompt)
            }
        }
    }

    private fun buildCombinedPrompt(
        userQuery: String,
        toolContext: String,
        webFacts: String?
    ): String = buildString {
        if (toolContext.isNotBlank()) {
            appendLine("[Real-time data from tools]:")
            appendLine(toolContext.trim())
            appendLine()
        }
        if (!webFacts.isNullOrBlank()) {
            appendLine("[Web facts from DuckDuckGo — use to improve accuracy]:")
            appendLine(webFacts.trim())
            appendLine()
        }
        appendLine("[User question]: $userQuery")
        appendLine()
        append("Answer clearly and accurately using all the above context. " +
               "If you are not confident, say so. Keep the response concise and helpful.")
    }

    private fun synthesizeFromWebFacts(query: String, facts: String): String = buildString {
        appendLine("🌐 Based on web search results:")
        appendLine()
        facts.lines().take(6).forEach { line ->
            if (line.isNotBlank()) appendLine(line.trim())
        }
    }.trim()
}
