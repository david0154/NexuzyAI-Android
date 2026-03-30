package ai.nexuzy.assistant.llm

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SarvaamAIClient — integrates Sarvaam AI API for enhanced accuracy.
 *
 * Used when internet is available as a cloud AI fallback/enhancement layer.
 * Priority chain when internet is ON:
 *   1. Local MLC model (if loaded)
 *   2. Sarvaam AI API (this class)  ← better accuracy for complex questions
 *   3. DuckDuckGo search context    ← factual/current info
 *
 * Setup:
 *   1. Get your API key from https://sarvaam.ai
 *   2. Add to local.properties:  SARVAAM_API_KEY=your_key_here
 *   3. Key is injected via BuildConfig.SARVAAM_API_KEY
 *
 * API endpoint follows OpenAI-compatible chat completions format.
 */
class SarvaamAIClient(private val apiKey: String) {

    companion object {
        private const val TAG = "SarvaamAIClient"
        private const val BASE_URL = "https://api.sarvam.ai/v1/chat/completions"
        private const val MODEL = "sarvam-2b-v0.5"  // Update if Sarvaam releases newer models
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Send a prompt to Sarvaam AI and get a response.
     * @param prompt  Full prompt including system context
     * @param history List of (user, assistant) turn pairs for context
     * @return AI response string, or null if API call fails
     */
    fun generate(prompt: String, history: List<Pair<String, String>> = emptyList()): String? {
        if (apiKey.isBlank()) {
            Log.w(TAG, "Sarvaam API key not configured")
            return null
        }
        return try {
            val messages = JSONArray()

            // System message
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", "You are NexuzyAI, a helpful AI assistant made by David / Nexuzy Lab. " +
                    "Answer clearly and concisely. If you don't know something, say so honestly.")
            })

            // History
            history.takeLast(6).forEach { (user, assistant) ->
                messages.put(JSONObject().apply { put("role", "user"); put("content", user) })
                messages.put(JSONObject().apply { put("role", "assistant"); put("content", assistant) })
            }

            // Current prompt
            messages.put(JSONObject().apply { put("role", "user"); put("content", prompt) })

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", messages)
                put("max_tokens", 512)
                put("temperature", 0.7)
            }

            val req = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "API error ${resp.code}: ${resp.body?.string()}")
                    return null
                }
                resp.body?.string() ?: return null
            }

            val json = JSONObject(response)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

        } catch (e: Exception) {
            Log.e(TAG, "Sarvaam API error: ${e.message}")
            null
        }
    }
}
