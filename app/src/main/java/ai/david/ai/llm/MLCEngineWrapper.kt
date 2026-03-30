package ai.david.ai.llm

// ─────────────────────────────────────────────────────────────
// WHY mlc4j IS NEEDED — plain English explanation
// ─────────────────────────────────────────────────────────────
//
// David AI uses on-device LLMs (Qwen3, Gemma) for AI chat.
// These models are written in C++ (TVM/CUDA/OpenCL runtime).
// Android can't call C++ directly from Kotlin — it needs a
// JNI (Java Native Interface) bridge.
//
// mlc4j IS that bridge. It provides:
//   • MLCEngine class  →  Kotlin-callable AI engine
//   • engine.reload(path, lib)  →  load a model
//   • engine.chat.completions.create(messages)  →  generate tokens
//   • libmlc_llm.so + libmlc4j.so  →  compiled for arm64-v8a
//
// WHY it can't be downloaded from Maven:
//   The .so files are compiled specifically for:
//     1. YOUR target Android ABI (arm64-v8a)
//     2. YOUR model (Qwen3 vs Gemma = different kernels)
//     3. YOUR quantization (q4f16, q0f16)
//   No single pre-built AAR works for all models.
//   So MLC-LLM compiles it fresh from mlc-package-config.json.
//
// HOW TO GENERATE mlc4j (one-time setup):
//   pip install mlc-llm
//   python3 -m mlc_llm package
//   → Creates dist/lib/mlc4j  (the Android module)
//   → Creates dist/bundle/    (compiled model weights)
//
// WITHOUT mlc4j: MLC_AVAILABLE = false
//   App still works: weather, news, voice, device control, tools.
//   AI fallback returns helpful direct answers.
//
// WITH mlc4j: MLC_AVAILABLE = true
//   Full on-device Qwen3 / Gemma AI inference.
// ─────────────────────────────────────────────────────────────

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

// Uncomment after `python3 -m mlc_llm package` and settings.gradle setup:
// import ai.mlc.mlcllm.MLCEngine
// import ai.mlc.mlcllm.OpenAIProtocol
// import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage

class MLCEngineWrapper(private val context: Context) {

    companion object {
        const val MLC_AVAILABLE = false  // set true after mlc_llm package
        const val TAG = "MLCEngineWrapper"
    }

    // private val engine = MLCEngine()  // uncomment after setup

    private val executor = Executors.newSingleThreadExecutor()
    private val scope    = CoroutineScope(Dispatchers.Main + Job())
    private val appDir   = context.getExternalFilesDir("") ?: context.filesDir

    var onStateChange: ((EngineState) -> Unit)? = null
    var onTokenStream: ((String) -> Unit)? = null
    private var currentState = EngineState.NOT_LOADED

    enum class EngineState { NOT_LOADED, LOADING, READY, GENERATING, FAILED }

    fun loadModel(modelId: String, modelLib: String) {
        if (!MLC_AVAILABLE) { setState(EngineState.NOT_LOADED); return }
        setState(EngineState.LOADING)
        val modelPath = File(appDir, modelId).absolutePath
        executor.submit {
            try {
                // engine.unload()
                // engine.reload(modelPath, modelLib)
                scope.launch { setState(EngineState.READY) }
            } catch (e: Exception) {
                Log.e(TAG, "Load failed: ${e.message}")
                scope.launch { setState(EngineState.FAILED) }
            }
        }
    }

    suspend fun generate(prompt: String, history: List<Pair<String,String>> = emptyList()): String =
        withContext(Dispatchers.IO) {
            if (!MLC_AVAILABLE || currentState != EngineState.READY)
                return@withContext fallbackResponse(prompt)
            setState(EngineState.GENERATING)
            val sb = StringBuilder()
            try {
                // val messages = mutableListOf<ChatCompletionMessage>()
                // history.forEach { (u,a) ->
                //     messages += ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.user, content=OpenAIProtocol.ChatCompletionMessageContent(text=u))
                //     messages += ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.assistant, content=a)
                // }
                // messages += ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.user, content=OpenAIProtocol.ChatCompletionMessageContent(text=prompt))
                // val responses = engine.chat.completions.create(messages=messages, stream_options=OpenAIProtocol.StreamOptions(include_usage=true))
                // for (res in responses) {
                //     for (choice in res.choices) {
                //         choice.delta.content?.let { sb.append(it.asText()); scope.launch { onTokenStream?.invoke(it.asText()) } }
                //         if (choice.finish_reason == "length") sb.append(" [truncated]")
                //     }
                // }
            } catch (e: Exception) {
                Log.e(TAG, "Generate: ${e.message}")
                setState(EngineState.READY)
                return@withContext "Error: ${e.message}"
            }
            setState(EngineState.READY)
            sb.toString()
        }

    fun resetChat() { if (MLC_AVAILABLE) executor.submit { /* engine.reset() */ } }
    fun unload() { if (MLC_AVAILABLE) executor.submit { /* engine.unload() */; scope.launch { setState(EngineState.NOT_LOADED) } } }
    private fun setState(s: EngineState) { currentState = s; onStateChange?.invoke(s) }
    fun getState() = currentState

    private fun fallbackResponse(prompt: String): String {
        // Smart fallback when MLC not available — parse tool context from prompt
        val lines = prompt.lines()
        val toolBlock = lines.dropWhile { !it.startsWith("<|im_start|>tool_result") }
            .drop(1).takeWhile { !it.startsWith("<|im_end|>") }
        return if (toolBlock.isNotEmpty()) toolBlock.joinToString("\n").trim()
        else "💡 David AI model not loaded yet. Run: python3 -m mlc_llm package (see SETUP_GUIDE.md)"
    }
}
