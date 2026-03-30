package ai.nexuzy.assistant.llm

import ai.nexuzy.assistant.middleware.ToolExecutor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Builds Qwen chat-template prompt.
 * Format: <|im_start|>role\ncontent<|im_end|>
 * This is the official Qwen2 prompt format.
 */
object PromptBuilder {

    fun build(
        userMessage: String,
        toolResult: ToolExecutor.ToolResult?,
        locationHint: String = "Kolkata, West Bengal, India"
    ): String {
        val date = LocalDate.now().toString()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        return buildString {
            // System prompt
            appendLine("<|im_start|>system")
            appendLine("You are NexuzyAI, a helpful, concise Android voice assistant.")
            appendLine("User location: $locationHint")
            appendLine("Date: $date | Time: $time IST")
            appendLine("Rules: Reply in 2-3 sentences. Be conversational. Use data from TOOL RESULT if provided.")
            toolResult?.systemHint?.let { appendLine(it) }
            append("<|im_end|>\n")

            // Tool result injected as a tool turn
            toolResult?.contextString?.let { ctx ->
                appendLine("<|im_start|>tool")
                appendLine(ctx)
                append("<|im_end|>\n")
            }

            // User message
            appendLine("<|im_start|>user")
            appendLine(userMessage)
            append("<|im_end|>\n")

            // Assistant prefix (model completes from here)
            append("<|im_start|>assistant\n")
        }
    }
}
