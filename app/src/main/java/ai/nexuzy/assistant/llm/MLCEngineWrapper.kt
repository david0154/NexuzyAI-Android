package ai.nexuzy.assistant.llm

// ============================================================
//  MLCEngineWrapper
//  Adapted from: mlc-ai/mlc-llm/android/MLCChat/AppViewModel.kt
//  https://github.com/mlc-ai/mlc-llm/tree/main/android/MLCChat
//  License: Apache 2.0
// ============================================================
//
//  GENERATION STRATEGY (internet ON):
//
//    DuckDuckGo search  ──┐
//                          ├──► HybridAnswerEngine ──► Fused Answer
//    Sarvaam AI API     ──┘          ▲
//    Local MLC model ────────────────┘  (also fused if available)
//
//  GENERATION STRATEGY (internet OFF):
//    Local MLC model  ──► Offline fallback message
//
//  HOW mlc4j IS GENERATED:
//    pip install mlc-llm
//    python3 -m mlc_llm package
//    → dist/lib/mlc4j  (uncomment settings.gradle + build.gradle imports)
// ============================================================

import android.content.Context
import android.util.Log
import ai.nexuzy.assistant.BuildConfig
import ai.nexuzy.assistant.tools.NetworkUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.Executors

// Uncomment after `python3 -m mlc_llm package` + settings.gradle setup:
// import ai.mlc.mlcllm.MLCEngine
// import ai.mlc.mlcllm.OpenAIProtocol
// import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage

class MLCEngineWrapper(private val context: Context) {

    companion object {
        const val MLC_AVAILABLE = false  // Set true after mlc_llm package build
        const val TAG = "MLCEngineWrapper"
    }

    // Uncomment after setup:
    // private val engine = MLCEngine()

    private val executor = Executors.newSingleThreadExecutor()
    private val scope    = CoroutineScope(Dispatchers.Main + Job())
    private val appDirFile = context.getExternalFilesDir("") ?: context.filesDir

    var onStateChange: ((EngineState) -> Unit)? = null
    var onTokenStream: ((String) -> Unit)? = null
    private var currentState = EngineState.NOT_LOADED

    // Sarvaam AI client
    private val sarvaamClient: SarvaamAIClient? by lazy {
        val key = try { BuildConfig.SARVAAM_API_KEY } catch (_: Exception) { "" }
        if (key.isNotBlank()) SarvaamAIClient(key) else null
    }

    // HybridAnswerEngine: combines MLC + Sarvaam + DuckDuckGo
    private val hybridEngine: HybridAnswerEngine by lazy {
        HybridAnswerEngine(context, sarvaamClient)
    }

    enum class EngineState { NOT_LOADED, LOADING, READY, GENERATING, FAILED }

    fun loadModel(modelId: String, modelLib: String) {
        if (!MLC_AVAILABLE) { setState(EngineState.NOT_LOADED); return }
        setState(EngineState.LOADING)
        val modelPath = File(appDirFile, modelId).absolutePath
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

    /**
     * Generate a response.
     *
     * When internet is ON:
     *   → HybridAnswerEngine runs DuckDuckGo + Sarvaam AI in parallel,
     *     fuses the results with local MLC output (if available) for the
     *     most accurate possible answer.
     *
     * When internet is OFF:
     *   → Local MLC only (if loaded), else offline fallback.
     *
     * @param prompt      Full prompt string (from PromptBuilder, includes tool context)
     * @param userQuery   Raw user question (for DuckDuckGo search grounding)
     * @param toolContext Tool result already fetched (weather/news/location/link)
     * @param history     Conversation history
     */
    suspend fun generate(
        prompt: String,
        userQuery: String = "",
        toolContext: String = "",
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {

        // --- Try local MLC model first ---
        var mlcResult: String? = null
        if (MLC_AVAILABLE && currentState == EngineState.READY) {
            setState(EngineState.GENERATING)
            val sb = StringBuilder()
            try {
                // val messages = buildMessages(history, prompt)
                // val responses = engine.chat.completions.create(messages, stream_options=...)
                // for (res in responses) { res.choices.forEach { choice ->
                //     choice.delta.content?.asText()?.let { token ->
                //         sb.append(token)
                //         scope.launch { onTokenStream?.invoke(token) }
                //     }
                // }}
                mlcResult = sb.toString().takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                Log.e(TAG, "MLC error: ${e.message}")
            } finally {
                setState(EngineState.READY)
            }
        }

        // --- Hand off to HybridAnswerEngine when internet available,
        //     OR just return MLC result when offline ---
        val effectiveQuery = userQuery.ifBlank { extractUserQuery(prompt) }
        val finalAnswer = hybridEngine.generate(
            userQuery   = effectiveQuery,
            toolContext = toolContext,
            history     = history,
            mlcResult   = mlcResult
        )

        // Stream the final answer token by token for smooth UI
        if (finalAnswer.isNotBlank()) {
            val words = finalAnswer.split(" ")
            for (word in words) {
                scope.launch { onTokenStream?.invoke("$word ") }
                delay(18)
            }
        }

        finalAnswer
    }

    fun resetChat() {
        if (MLC_AVAILABLE) executor.submit { /* engine.reset() */ }
    }

    fun unload() {
        if (MLC_AVAILABLE) executor.submit {
            // engine.unload()
            scope.launch { setState(EngineState.NOT_LOADED) }
        }
    }

    fun downloadModel(
        modelId: String,
        modelUrl: String,
        onProgress: (Int, Int) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val modelDir = File(appDirFile, modelId).also { it.mkdirs() }
        executor.submit {
            try {
                val configFile = File(modelDir, "mlc-chat-config.json")
                if (!configFile.exists()) {
                    URL("$modelUrl/resolve/main/mlc-chat-config.json").openStream().use { inp ->
                        FileOutputStream(configFile).use { out ->
                            Channels.newChannel(inp).use { src ->
                                out.channel.transferFrom(src, 0, Long.MAX_VALUE)
                            }
                        }
                    }
                }
                scope.launch { onComplete() }
            } catch (e: Exception) {
                scope.launch { onError(e.message ?: "Download failed") }
            }
        }
    }

    private fun setState(s: EngineState) { currentState = s; onStateChange?.invoke(s) }
    fun getState() = currentState

    /** Extract the last user message from the prompt string */
    private fun extractUserQuery(prompt: String): String {
        val lines = prompt.lines()
        val userIdx = lines.indexOfLast { it.trim() == "<|im_start|>user" }
        return if (userIdx >= 0 && userIdx + 1 < lines.size)
            lines.drop(userIdx + 1)
                .takeWhile { it.trim() != "<|im_end|>" }
                .joinToString(" ")
                .trim()
        else prompt.take(200)
    }
}
