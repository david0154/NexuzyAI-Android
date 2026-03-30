package ai.nexuzy.assistant.llm

import ai.nexuzy.assistant.middleware.ToolExecutor

/**
 * PromptBuilder: Constructs the final prompt sent to the on-device LLM.
 * Injects tool context + location as system prompt.
 */
object PromptBuilder {

    fun build(
        userMessage: String,
        toolResult: ToolExecutor.ToolResult?,
        locationHint: String = "Kolkata, India"
    ): String {
        val sb = StringBuilder()

        sb.appendLine("[SYSTEM]")
        sb.appendLine("You are NexuzyAI, a helpful Android assistant.")
        sb.appendLine("User location: $locationHint")
        sb.appendLine("Today: ${java.time.LocalDate.now()}")

        toolResult?.systemHint?.let { hint ->
            sb.appendLine(hint)
        }

        toolResult?.contextString?.let { ctx ->
            sb.appendLine()
            sb.appendLine("[TOOL RESULT]")
            sb.appendLine(ctx)
        }

        sb.appendLine()
        sb.appendLine("[USER]")
        sb.appendLine(userMessage)
        sb.appendLine()
        sb.appendLine("[ASSISTANT]")

        return sb.toString()
    }
}
