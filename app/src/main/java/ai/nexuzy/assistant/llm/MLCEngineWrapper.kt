package ai.nexuzy.assistant.llm

// ============================================================
//  MLCEngineWrapper
//  Adapted from: mlc-ai/mlc-llm/android/MLCChat/AppViewModel.kt
//  https://github.com/mlc-ai/mlc-llm/tree/main/android/MLCChat
//  License: Apache 2.0
// ============================================================
//
//  ROUTING LOGIC:
//
//  ┌─────────────────────────────────────────────────────────────────────┐
//  │  INTERNET ON                                               │
//  │  DuckDuckGo search  ──┬─► HybridAnswerEngine ► fused ans  │
//  │  Sarvaam AI API     ──┤          ▲                        │
//  │  Local MLC result  ──┘          └────────────────────     │
//  ├─────────────────────────────────────────────────────────────────────┤
//  │  INTERNET OFF                                              │
//  │  Local MLC model (if built) ► LocalOfflineEngine (NLP)     │
//  │  → ALWAYS gives a real answer, never silent               │
//  └─────────────────────────────────────────────────────────────────────┘
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

    // private val engine = MLCEngine()

    private val executor    = Executors.newSingleThreadExecutor()
    private val scope       = CoroutineScope(Dispatchers.Main + Job())
    private val appDirFile  = context.getExternalFilesDir("") ?: context.filesDir

    var onStateChange: ((EngineState) -> Unit)? = null
    var onTokenStream: ((String) -> Unit)? = null
    private var currentState = EngineState.NOT_LOADED

    private val sarvaamClient: SarvaamAIClient? by lazy {
        val key = try { BuildConfig.SARVAAM_API_KEY } catch (_: Exception) { "" }
        if (key.isNotBlank()) SarvaamAIClient(key) else null
    }

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
     * Main generate function.
     *
     * @param prompt      Full Qwen3 chat-template prompt (from PromptBuilder)
     * @param userQuery   Raw user question (extracted if blank)
     * @param toolContext Tool result fetched before this call
     * @param history     Conversation history pairs
     */
    suspend fun generate(
        prompt: String,
        userQuery: String = "",
        toolContext: String = "",
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {

        // Extract raw user query from prompt if not provided separately
        val effectiveQuery = userQuery.ifBlank { extractUserQuery(prompt) }

        // ─ Step 1: Try local MLC model (when built) ────────────────────
        var mlcResult: String? = null
        if (MLC_AVAILABLE && currentState == EngineState.READY) {
            setState(EngineState.GENERATING)
            val sb = StringBuilder()
            try {
                // val messages = buildMessages(history, prompt)
                // engine.chat.completions.create(messages, ...).forEach { res ->
                //     res.choices.forEach { choice ->
                //         choice.delta.content?.asText()?.let { token ->
                //             sb.append(token)
                //             scope.launch { onTokenStream?.invoke(token) }
                //         }
                //     }
                // }
                mlcResult = sb.toString().takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                Log.e(TAG, "MLC error: ${e.message}")
            } finally {
                setState(EngineState.READY)
            }
        }

        // ─ Step 2: Route to HybridAnswerEngine ────────────────────────
        //   • If internet ON  → DuckDuckGo + Sarvaam fused answer
        //   • If internet OFF → MLC result OR LocalOfflineEngine NLP
        val finalAnswer = hybridEngine.generate(
            userQuery   = effectiveQuery,
            toolContext = toolContext,
            history     = history,
            mlcResult   = mlcResult,
            fullPrompt  = prompt
        )

        // ─ Step 3: Stream final answer token by token for smooth UI ─
        if (finalAnswer.isNotBlank()) {
            finalAnswer.split(" ").forEach { word ->
                scope.launch { onTokenStream?.invoke("$word ") }
                delay(16)
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

    /** Extract user message from Qwen3 chat template prompt */
    private fun extractUserQuery(prompt: String): String {
        val lines = prompt.lines()
        val idx   = lines.indexOfLast { it.trim() == "<|im_start|>user" }
        return if (idx >= 0 && idx + 1 < lines.size)
            lines.drop(idx + 1)
                .takeWhile { it.trim() != "<|im_end|>" }
                .joinToString(" ")
                .trim()
        else prompt.take(200)
    }
}
