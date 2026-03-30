package ai.nexuzy.assistant.llm

import ai.nexuzy.assistant.middleware.ToolExecutor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PromptBuilder {

    fun build(
        userMessage: String,
        toolResult: ToolExecutor.ToolResult?,
        locationHint: String = "Kolkata, West Bengal, India"
    ): String {
        val date = LocalDate.now().toString()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        return buildString {
            appendLine("<|im_start|>system")
            appendLine("You are NexuzyAI, a helpful, concise Android voice assistant.")
            appendLine("User location: $locationHint")
            appendLine("Current date: $date | Time: $time")
            appendLine("Always reply in 2-3 sentences max. Be conversational and friendly.")
            toolResult?.systemHint?.let { appendLine(it) }
            appendLine("<|im_end|>")

            toolResult?.contextString?.let { ctx ->
                appendLine("<|im_start|>tool")
                appendLine(ctx)
                appendLine("<|im_end|>")
            }

            appendLine("<|im_start|>user")
            appendLine(userMessage)
            appendLine("<|im_end|>")
            appendLine("<|im_start|>assistant")
        }
    }
}
