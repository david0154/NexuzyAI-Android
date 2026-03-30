package ai.nexuzy.assistant.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QwenEngine: On-device LLM using Qwen 3B via MLC-LLM (mlc4j).
 *
 * HOW TO SET UP QWEN 3B:
 * ─────────────────────────────────────────────────────────────────────
 * 1. Install MLC-LLM Python package:
 *    pip install mlc-llm
 *
 * 2. Compile Qwen3B for Android:
 *    mlc_llm compile Qwen/Qwen2-1.5B-Instruct \
 *        --quantization q4f16_1 \
 *        --target android \
 *        --output dist/qwen3b-android/
 *
 *    OR download pre-built from HuggingFace:
 *    https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC
 *
 * 3. Place compiled weights in:  app/src/main/assets/qwen3b/
 * 4. Place mlc4j-release.aar in: app/libs/
 * 5. Uncomment the mlc4j import + engine lines below.
 * ─────────────────────────────────────────────────────────────────────
 *
 * Alternatively use llama.cpp android via JNI:
 *   https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
 *   Model: Qwen2-1.5B-Instruct-Q4_K_M.gguf
 */
class QwenEngine(private val context: Context) {

    private var isModelLoaded = false

    // Uncomment when mlc4j AAR is placed in app/libs/:
    // private var engine: ai.mlc.mlcllm.MLCEngine? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // engine = ai.mlc.mlcllm.MLCEngine()
            // engine?.load("qwen3b", context.filesDir.absolutePath + "/qwen3b")
            isModelLoaded = false // Set true after loading AAR
        } catch (e: Exception) {
            isModelLoaded = false
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        if (!isModelLoaded) {
            // Fallback: echo tool result from prompt when model not loaded
            return@withContext extractFallbackResponse(prompt)
        }
        try {
            // return@withContext engine!!.generate(prompt, maxTokens = 256)
            extractFallbackResponse(prompt)
        } catch (e: Exception) {
            "⚠️ Model error: ${e.message}"
        }
    }

    /**
     * Fallback: extracts tool result from prompt and formats it.
     * Works even before Qwen model weights are loaded.
     */
    private fun extractFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("TOOL RESULT") -> {
                val toolLine = prompt.lines()
                    .firstOrNull { it.startsWith("Current weather") || it.startsWith("Latest news") ||
                                  it.startsWith("Alarm") || it.startsWith("Flashlight") ||
                                  it.startsWith("Media") || it.startsWith("App launch") }
                    ?: "Here's what I found."
                toolLine
            }
            else -> "I'm NexuzyAI. Please load the Qwen 3B model weights to enable full responses. See QwenEngine.kt for setup instructions."
        }
    }
}
