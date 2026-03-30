package ai.nexuzy.assistant.llm

import android.content.Context
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * QwenEngine: High-level interface over MLCEngineWrapper.
 * Uses Qwen2-1.5B-Instruct (Qwen 3B class) via MLC-LLM.
 *
 * Model download (HuggingFace, pre-compiled for Android):
 *   https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC
 *
 * Steps:
 *   1. Download mlc4j-release.aar from https://github.com/mlc-ai/mlc-llm/releases
 *   2. Place in app/libs/
 *   3. Uncomment in app/build.gradle:  implementation files('libs/mlc4j-release.aar')
 *   4. Uncomment imports in MLCEngineWrapper.kt
 *   5. Set MLCEngineWrapper.MLC_AVAILABLE = true
 *   6. Call engine.loadModel("Qwen2-1.5B-Instruct-q4f16_1-MLC", "Qwen2-1.5B-Instruct-q4f16_1-MLC")
 */
class QwenEngine(context: Context) {

    private val wrapper = MLCEngineWrapper(context)
    private val conversationHistory = mutableListOf<Pair<String, String>>() // (user, ai)

    val state get() = wrapper.getState()
    var onStateChange: ((MLCEngineWrapper.EngineState) -> Unit)?
        get() = wrapper.onStateChange
        set(value) { wrapper.onStateChange = value }
    var onTokenStream: ((String) -> Unit)?
        get() = wrapper.onTokenStream
        set(value) { wrapper.onTokenStream = value }

    init {
        // Auto-load Qwen2-1.5B if MLC available
        if (MLCEngineWrapper.MLC_AVAILABLE) {
            wrapper.loadModel(
                modelId = "Qwen2-1.5B-Instruct-q4f16_1-MLC",
                modelLib = "Qwen2-1.5B-Instruct-q4f16_1-MLC"
            )
        }
    }

    suspend fun generate(prompt: String): String {
        val response = wrapper.generate(prompt, conversationHistory.toList())
        // Store history for multi-turn conversation
        val userMsg = prompt.lines()
            .dropWhile { !it.startsWith("<|im_start|>user") }
            .drop(1).firstOrNull() ?: ""
        if (userMsg.isNotEmpty() && response.isNotEmpty()) {
            conversationHistory.add(Pair(userMsg, response))
            if (conversationHistory.size > 10) conversationHistory.removeAt(0)
        }
        return response
    }

    fun resetHistory() = wrapper.resetChat().also { conversationHistory.clear() }
    fun unload() = wrapper.unload()

    fun downloadModel(
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        wrapper.downloadModel(
            modelId = "Qwen2-1.5B-Instruct-q4f16_1-MLC",
            modelUrl = "https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC",
            onProgress = onProgress,
            onComplete = onComplete,
            onError = onError
        )
    }
}
