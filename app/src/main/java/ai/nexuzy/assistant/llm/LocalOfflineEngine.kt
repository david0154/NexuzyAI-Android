package ai.nexuzy.assistant.llm

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LocalOfflineEngine — provides real answers when internet is OFF.
 *
 * Priority:
 *   1. MLC on-device model  (when MLC_AVAILABLE = true after mlc_llm package build)
 *   2. Smart rule-based NLP engine (handles identity, time, math, general Q&A)
 *      so the app is NEVER silent offline — always gives a useful response.
 *
 * The rule-based engine covers:
 *   - Identity questions (who are you, who made you)
 *   - Date / time queries
 *   - Basic math (add, subtract, multiply, divide)
 *   - Greetings
 *   - What can you do
 *   - Offline status message for truly unknown questions
 */
class LocalOfflineEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalOfflineEngine"
    }

    /**
     * Generate a response using local resources only (no internet).
     * Called by HybridAnswerEngine when internet is unavailable.
     *
     * @param userQuery   Raw user message
     * @param toolContext Any tool result already computed offline (e.g. GPS location)
     * @param prompt      Full built prompt (for MLC injection when available)
     * @return            Answer string
     */
    fun generate(
        userQuery: String,
        toolContext: String = "",
        prompt: String = ""
    ): String {

        // 1. If MLC is available and loaded, it will have already run in MLCEngineWrapper
        //    and its result passed in as mlcResult — this function is the fallback.

        // 2. If tool context exists (e.g. GPS coords), return it
        if (toolContext.isNotBlank()) {
            Log.d(TAG, "Returning tool context offline")
            return toolContext.trim()
        }

        // 3. Rule-based NLP for common queries
        val q = userQuery.lowercase().trim()
        return when {
            // ─ Greetings ─────────────────────────────────────────
            q.matchesAny("hello", "hi", "hey", "good morning", "good evening",
                "good afternoon", "good night", "namaste") ->
                "👋 Hey! I'm NexuzyAI. How can I help you today? (Offline mode)"

            // ─ Identity ─────────────────────────────────────────
            q.matchesAny("who are you", "what are you", "your name", "who made you",
                "who created you", "who built you", "who is your developer",
                "who developed you", "about you", "introduce yourself") ->
                "🤖 I'm NexuzyAI — a smart AI assistant developed by David, managed by Nexuzy Lab.\n" +
                "📧 nexuzylab@gmail.com | davidk76011@gmail.com\n" +
                "🐙 github.com/david0154/NexuzyAI-Android\n" +
                "🔒 I run on-device with zero data collection."

            // ─ Date / Time ───────────────────────────────────────
            q.matchesAny("what time", "current time", "what's the time",
                "what is the time") -> {
                val time = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
                "⏰ Current time: $time"
            }
            q.matchesAny("what date", "today's date", "what day is it",
                "what is today", "current date") -> {
                val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date())
                "📅 Today is $date"
            }
            q.matchesAny("what year", "which year") -> {
                val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
                "📅 The current year is $year"
            }

            // ─ What can you do ─────────────────────────────────
            q.matchesAny("what can you do", "your features", "help", "capabilities",
                "what do you do", "how can you help") ->
                "🤖 Here's what I can do:\n" +
                "�\uDF26\uFE0F Weather (internet needed)\n" +
                "📰 News headlines (internet needed)\n" +
                "📍 Your location (GPS works offline)\n" +
                "🔗 Read & summarize links (internet needed)\n" +
                "🔍 Web search via DuckDuckGo (internet needed)\n" +
                "⏰ Set alarms\n" +
                "💡 Turn flashlight on/off\n" +
                "🎵 Control media playback\n" +
                "🚀 Open any app\n" +
                "💬 Chat with me anytime, online or offline!"

            // ─ Basic Math ────────────────────────────────────────
            q.contains("what is") && (q.contains("+") || q.contains("-") ||
                q.contains("*") || q.contains("/") || q.contains("x") ||
                q.contains("plus") || q.contains("minus") || q.contains("times") ||
                q.contains("divided by") || q.contains("multiply")) ->
                solveMath(q)

            q.matches(Regex(".*\\d+\\s*[+\\-*/x]\\s*\\d+.*")) -> solveMath(q)

            // ─ Offline status for unknown ──────────────────────────
            else ->
                "📵 I'm currently in offline mode. For this question I need internet access.\n" +
                "Connect to Wi-Fi or mobile data for:\n" +
                "• Accurate AI answers (Sarvaam AI + DuckDuckGo)\n" +
                "• Latest weather and news\n" +
                "• Link reading and web search"
        }
    }

    /** Basic math solver for simple arithmetic questions */
    private fun solveMath(q: String): String {
        return try {
            // Normalize text-based operators
            val expr = q
                .replace("plus", "+").replace("minus", "-")
                .replace("times", "*").replace("multiplied by", "*")
                .replace("divided by", "/").replace(" x ", "*")
                .replace("what is", "").replace("calculate", "").replace("=", "")
                .trim()
            // Extract the math expression
            val mathRegex = Regex("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)")
            val match = mathRegex.find(expr) ?: return "🧮 I couldn't parse that math expression."
            val a = match.groupValues[1].toDouble()
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDouble()
            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else return "🚫 Cannot divide by zero!"
                else -> return "🧮 Unknown operator."
            }
            // Show as integer if result is whole
            val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
            "🧮 $a $op $b = $formatted"
        } catch (e: Exception) {
            "🧮 I couldn't calculate that. Try: \"What is 5 + 3?\""
        }
    }

    /** Check if query contains any of the given keyword phrases */
    private fun String.matchesAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
