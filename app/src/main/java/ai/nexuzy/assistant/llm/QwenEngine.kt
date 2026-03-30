package ai.nexuzy.assistant.llm

import android.content.Context

/**
 * QwenEngine: High-level interface over MLCEngineWrapper.
 *
 * Default model: Qwen3-1.7B-q4f16_1-MLC
 * (Official MLC-LLM Android config — mlc-ai/mlc-llm/android/MLCChat/mlc-package-config.json)
 *
 * HuggingFace: https://huggingface.co/mlc-ai/Qwen3-1.7B-q4f16_1-MLC
 * VRAM needed: ~3GB  (works on most mid/high-end Android phones)
 *
 * Lighter fallback: Qwen3-0.6B-q0f16-MLC (~1.5GB VRAM, lower quality)
 * HuggingFace: https://huggingface.co/mlc-ai/Qwen3-0.6B-q0f16-MLC
 *
 * To switch to the lighter model, change DEFAULT_MODEL_ID and DEFAULT_MODEL_LIB below.
 */
class QwenEngine(context: Context) {

    companion object {
        // ✓ Synced with official mlc-package-config.json (mlc-ai/mlc-llm/android/MLCChat)
        // Change to "Qwen3-0.6B-q0f16-MLC" for low-RAM devices
        const val DEFAULT_MODEL_ID  = "Qwen3-1.7B-q4f16_1-MLC"
        const val DEFAULT_MODEL_LIB = "Qwen3-1.7B-q4f16_1-MLC"
        const val DEFAULT_MODEL_HF  = "https://huggingface.co/mlc-ai/Qwen3-1.7B-q4f16_1-MLC"
        const val LIGHT_MODEL_ID    = "Qwen3-0.6B-q0f16-MLC"
        const val LIGHT_MODEL_LIB   = "Qwen3-0.6B-q0f16-MLC"
        const val LIGHT_MODEL_HF    = "https://huggingface.co/mlc-ai/Qwen3-0.6B-q0f16-MLC"
    }

    private val wrapper = MLCEngineWrapper(context)
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    val state get() = wrapper.getState()
    var onStateChange: ((MLCEngineWrapper.EngineState) -> Unit)?
        get() = wrapper.onStateChange
        set(value) { wrapper.onStateChange = value }
    var onTokenStream: ((String) -> Unit)?
        get() = wrapper.onTokenStream
        set(value) { wrapper.onTokenStream = value }

    init {
        if (MLCEngineWrapper.MLC_AVAILABLE) {
            wrapper.loadModel(DEFAULT_MODEL_ID, DEFAULT_MODEL_LIB)
        }
    }

    suspend fun generate(prompt: String): String {
        val response = wrapper.generate(prompt, conversationHistory.toList())
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

    /** Download default model (Qwen3-1.7B) from HuggingFace */
    fun downloadDefaultModel(
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        wrapper.downloadModel(
            modelId  = DEFAULT_MODEL_ID,
            modelUrl = DEFAULT_MODEL_HF,
            onProgress = onProgress,
            onComplete = onComplete,
            onError = onError
        )
    }

    /** Download lighter Qwen3-0.6B for low-RAM phones */
    fun downloadLightModel(
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        wrapper.downloadModel(
            modelId  = LIGHT_MODEL_ID,
            modelUrl = LIGHT_MODEL_HF,
            onProgress = onProgress,
            onComplete = onComplete,
            onError = onError
        )
    }
}
