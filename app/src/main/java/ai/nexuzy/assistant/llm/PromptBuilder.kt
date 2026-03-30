package ai.nexuzy.assistant.llm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PromptBuilder — builds Qwen3 chat-template prompts.
 *
 * build()        → standard prompt with optional tool context
 * buildHybrid()  → enriched prompt that injects web facts + tool context
 *                  side by side so local MLC can use both for grounding
 */
object PromptBuilder {

    fun build(
        userInput: String,
        toolContext: String = "",
        locationHint: String = "",
        activeModelName: String = "NexuzyAI"
    ): String {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH).format(Date())

        val systemPrompt = buildString {
            appendLine("You are $activeModelName, a smart AI assistant.")
            appendLine("Developed by David. Managed by Nexuzy Lab.")
            appendLine("Support: nexuzylab@gmail.com | davidk76011@gmail.com")
            appendLine("You run on-device. Zero data collected.")
            appendLine("Current date/time: $date")
            if (locationHint.isNotBlank()) appendLine(locationHint)
            appendLine()
            appendLine("Be helpful, concise, and accurate.")
            appendLine("If real-time data is provided below, use it in your answer.")
            appendLine("If you are not confident, say so honestly.")
        }

        return buildString {
            appendLine("<|im_start|>system")
            appendLine(systemPrompt.trim())
            appendLine("<|im_end|>")
            if (toolContext.isNotBlank()) {
                appendLine("<|im_start|>tool_result")
                appendLine(toolContext.trim())
                appendLine("<|im_end|>")
            }
            appendLine("<|im_start|>user")
            appendLine(userInput)
            appendLine("<|im_end|>")
            append("<|im_start|>assistant")
        }
    }

    /**
     * buildHybrid — used by HybridAnswerEngine to create a rich
     * combined prompt with BOTH web facts and tool data injected,
     * sent to Sarvaam AI or local MLC for maximum accuracy.
     */
    fun buildHybrid(
        userInput: String,
        toolContext: String = "",
        webFacts: String = "",
        locationHint: String = "",
        activeModelName: String = "NexuzyAI"
    ): String {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH).format(Date())

        return buildString {
            appendLine("<|im_start|>system")
            appendLine("You are $activeModelName, a smart AI assistant by Nexuzy Lab (David).")
            appendLine("Current date/time: $date")
            if (locationHint.isNotBlank()) appendLine(locationHint)
            appendLine("You have access to real-time tool data AND live web search results.")
            appendLine("Always synthesize ALL provided context for the most accurate answer.")
            appendLine("If data conflicts, prefer real-time tool data over web facts.")
            appendLine("Be concise, factual, and friendly. Admit uncertainty when needed.")
            appendLine("<|im_end|>")

            // Tool context (weather, news, location, link content)
            if (toolContext.isNotBlank()) {
                appendLine("<|im_start|>tool_result")
                appendLine("[Real-time tool data]:")
                appendLine(toolContext.trim())
                appendLine("<|im_end|>")
            }

            // DuckDuckGo web facts
            if (webFacts.isNotBlank()) {
                appendLine("<|im_start|>tool_result")
                appendLine("[Live web facts — DuckDuckGo]:")
                appendLine(webFacts.trim())
                appendLine("<|im_end|>")
            }

            appendLine("<|im_start|>user")
            appendLine(userInput)
            appendLine("<|im_end|>")
            append("<|im_start|>assistant")
        }
    }
}
