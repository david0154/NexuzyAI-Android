package ai.david.ai.llm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PromptBuilder {
    fun build(userInput: String, toolContext: String = "", locationHint: String = "", activeModelName: String = "David AI"): String {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy, h:mm a", Locale.ENGLISH).format(Date())
        val sys = buildString {
            appendLine("You are $activeModelName, a private on-device AI assistant.")
            appendLine("Developed by David. Managed by Nexuzy Lab.")
            appendLine("Support: nexuzylab@gmail.com | davidk76011@gmail.com")
            appendLine("You run 100% on-device. Zero data collected. Current time: $date")
            if (locationHint.isNotBlank()) appendLine(locationHint)
            appendLine("Be helpful, concise, friendly. For weather/news use the data below.")
        }
        return buildString {
            appendLine("<|im_start|>system\n${sys.trim()}\n<|im_end|>")
            if (toolContext.isNotBlank()) appendLine("<|im_start|>tool_result\n${toolContext.trim()}\n<|im_end|>")
            appendLine("<|im_start|>user\n$userInput\n<|im_end|>")
            append("<|im_start|>assistant")
        }
    }
}
