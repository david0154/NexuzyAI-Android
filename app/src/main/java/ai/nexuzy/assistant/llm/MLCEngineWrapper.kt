package ai.nexuzy.assistant.llm

// ============================================================
//  MLCEngineWrapper
//  Adapted from: mlc-ai/mlc-llm/android/MLCChat/AppViewModel.kt
//  https://github.com/mlc-ai/mlc-llm/tree/main/android/MLCChat
//  License: Apache 2.0
// ============================================================
//
//  GENERATION PRIORITY CHAIN:
//
//    1. Local MLC model  (if MLC_AVAILABLE=true and model loaded)
//    2. Sarvaam AI API   (if internet on + SARVAAM_API_KEY set)
//    3. DuckDuckGo       (injected as tool context via ToolExecutor)
//    4. Offline fallback (shows tool result or guidance message)
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
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val appDirFile = context.getExternalFilesDir("") ?: context.filesDir

    var onStateChange: ((EngineState) -> Unit)? = null
    var onTokenStream: ((String) -> Unit)? = null
    private var currentState = EngineState.NOT_LOADED

    // Sarvaam AI client — activated when internet is available
    private val sarvaamClient: SarvaamAIClient? by lazy {
        val key = try { BuildConfig.SARVAAM_API_KEY } catch (e: Exception) { "" }
        if (key.isNotBlank()) SarvaamAIClient(key) else null
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
     * Generate a response using the priority chain:
     *   1. Local MLC model  (if MLC_AVAILABLE)
     *   2. Sarvaam AI API   (if internet + key configured)
     *   3. Offline fallback (tool context or guidance)
     */
    suspend fun generate(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {

        // --- 1. Local MLC model ---
        if (MLC_AVAILABLE && currentState == EngineState.READY) {
            setState(EngineState.GENERATING)
            val sb = StringBuilder()
            try {
                // val messages = mutableListOf<ChatCompletionMessage>()
                // history.forEach { (u, a) ->
                //     messages += ChatCompletionMessage(role=user, content=u)
                //     messages += ChatCompletionMessage(role=assistant, content=a)
                // }
                // messages += ChatCompletionMessage(role=user, content=prompt)
                // val responses = engine.chat.completions.create(messages=messages, stream_options=...)
                // for (res in responses) {
                //     for (choice in res.choices) {
                //         choice.delta.content?.let { token ->
                //             sb.append(token.asText())
                //             scope.launch { onTokenStream?.invoke(token.asText()) }
                //         }
                //     }
                // }
            } catch (e: Exception) {
                Log.e(TAG, "MLC generate error: ${e.message}")
                setState(EngineState.READY)
                return@withContext "Error: ${e.message}"
            }
            setState(EngineState.READY)
            return@withContext sb.toString()
        }

        // --- 2. Sarvaam AI API (when internet is on) ---
        if (NetworkUtils.isInternetAvailable(context) && sarvaamClient != null) {
            Log.d(TAG, "Using Sarvaam AI API for generation")
            val sarvaamResponse = sarvaamClient!!.generate(prompt, history)
            if (!sarvaamResponse.isNullOrBlank()) {
                // Simulate token streaming for UI consistency
                val words = sarvaamResponse.split(" ")
                for (word in words) {
                    scope.launch { onTokenStream?.invoke("$word ") }
                    delay(20)
                }
                return@withContext sarvaamResponse
            }
        }

        // --- 3. Offline fallback ---
        fallbackResponse(prompt)
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
                    URL("$modelUrl/resolve/main/mlc-chat-config.json").openStream().use { input ->
                        FileOutputStream(configFile).use { out ->
                            Channels.newChannel(input).use { src ->
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

    private fun fallbackResponse(prompt: String): String {
        // Check if any tool context was injected — show that
        val toolLine = prompt.lines().firstOrNull {
            it.startsWith("Current weather") || it.startsWith("Latest news") ||
            it.startsWith("Alarm") || it.startsWith("[DuckDuckGo") ||
            it.startsWith("[Link content") || it.startsWith("\u2022")
        }
        return toolLine?.trim()
            ?: if (NetworkUtils.isInternetAvailable(context))
                "\uD83D\uDCA1 MLC model not ready. Configure SARVAAM_API_KEY in local.properties for cloud AI, or run: python3 -m mlc_llm package"
               else
                "\uD83D\uDCA1 Offline mode. Connect to internet for enhanced responses, or run: python3 -m mlc_llm package to enable on-device AI."
    }
}
