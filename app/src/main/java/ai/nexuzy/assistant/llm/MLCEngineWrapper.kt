package ai.nexuzy.assistant.llm

// ============================================================
//  MLCEngineWrapper — adapted from:
//  mlc-ai/mlc-llm/android/MLCChat/app/src/main/java/ai/mlc/mlcchat/AppViewModel.kt
//  Source: https://github.com/mlc-ai/mlc-llm/tree/main/android
//  License: Apache 2.0
// ============================================================
//
//  mlc4j IS A LOCAL MODULE, NOT A DOWNLOADABLE AAR.
//
//  To enable Qwen3 on-device:
//  1. git clone --recursive https://github.com/mlc-ai/mlc-llm.git
//  2. cp -r mlc-llm/android/mlc4j  NexuzyAI-Android/mlc4j
//  3. cd NexuzyAI-Android/mlc4j && python3 prepare_libs.py
//  4. settings.gradle:  uncomment  include ':mlc4j'
//  5. app/build.gradle: uncomment  implementation project(':mlc4j')
//  6. Below:            set  MLC_AVAILABLE = true
//                       uncomment the 3 import lines
// ============================================================

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.Executors

// Step 6a: Uncomment these 3 lines after adding mlc4j module:
// import ai.mlc.mlcllm.MLCEngine
// import ai.mlc.mlcllm.OpenAIProtocol
// import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage

class MLCEngineWrapper(private val context: Context) {

    companion object {
        // Step 6b: Change to true after completing setup steps 1-5 above
        const val MLC_AVAILABLE = false
        const val TAG = "MLCEngineWrapper"
    }

    // Step 6c: Uncomment after adding mlc4j:
    // private val engine = MLCEngine()

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val appDirFile = context.getExternalFilesDir("") ?: context.filesDir

    var onStateChange: ((EngineState) -> Unit)? = null
    var onTokenStream: ((String) -> Unit)? = null
    private var currentState = EngineState.NOT_LOADED

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

    suspend fun generate(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (!MLC_AVAILABLE || currentState != EngineState.READY) return@withContext fallbackResponse(prompt)
        setState(EngineState.GENERATING)
        val sb = StringBuilder()
        try {
            // Mirrors AppViewModel.ChatState.requestGenerate() exactly:
            //
            // val messages = mutableListOf<ChatCompletionMessage>()
            // for ((u, a) in history) {
            //     messages.add(ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.user,
            //         content=OpenAIProtocol.ChatCompletionMessageContent(text=u)))
            //     messages.add(ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.assistant,
            //         content=a))
            // }
            // messages.add(ChatCompletionMessage(role=OpenAIProtocol.ChatCompletionRole.user,
            //     content=OpenAIProtocol.ChatCompletionMessageContent(text=prompt)))
            //
            // val responses = engine.chat.completions.create(
            //     messages = messages,
            //     stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
            // )
            // for (res in responses) {
            //     for (choice in res.choices) {
            //         choice.delta.content?.let { c ->
            //             val token = c.asText()
            //             sb.append(token)
            //             scope.launch { onTokenStream?.invoke(token) }
            //         }
            //     }
            // }
        } catch (e: Exception) {
            Log.e(TAG, "Generate error: ${e.message}")
            setState(EngineState.READY)
            return@withContext "Error: ${e.message}"
        }
        setState(EngineState.READY)
        sb.toString()
    }

    fun resetChat() { if (MLC_AVAILABLE) executor.submit { /* engine.reset() */ } }

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
        val lines = prompt.lines()
        val toolResult = lines.firstOrNull {
            it.startsWith("Current weather") || it.startsWith("Latest news") ||
            it.startsWith("Alarm") || it.startsWith("Flashlight") ||
            it.startsWith("Media") || it.startsWith("•")
        }
        return toolResult?.trim()
            ?: "💡 Qwen3 not loaded. See mlc4j/README_PLACEHOLDER.md for setup."
    }
}
