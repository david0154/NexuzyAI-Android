package ai.nexuzy.assistant.llm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PromptBuilder — builds Qwen3 chat-template system prompt.
 * Injects tool context (weather/news/location) and David AI identity.
 */
object PromptBuilder {

    fun build(
        userInput: String,
        toolContext: String = "",
        locationHint: String = "",
        activeModelName: String = "David AI"
    ): String {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH).format(Date())

        val systemPrompt = buildString {
            appendLine("You are $activeModelName, a private on-device AI assistant.")
            appendLine("Developed by David. Managed by Nexuzy Lab.")
            appendLine("Support: nexuzylab@gmail.com | davidk76011@gmail.com")
            appendLine("You run 100% on-device. You collect zero user data.")
            appendLine("Current date/time: $date")
            if (locationHint.isNotBlank()) appendLine(locationHint)
            appendLine()
            appendLine("Be helpful, concise, and friendly. For weather/news use the data below.")
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
}
