package ai.nexuzy.assistant.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * QwenEngine — High-level interface for NexuzyAI.
 * Wraps MLCEngineWrapper and ModelManager.
 *
 * generate() now accepts optional userQuery + toolContext so the
 * HybridAnswerEngine can use them for DuckDuckGo grounding and
 * offline LocalOfflineEngine NLP.
 */
class QwenEngine(private val context: Context) {

    private val mlcWrapper   = MLCEngineWrapper(context)
    val modelManager         = ModelManager(context)
    private val scope        = CoroutineScope(Dispatchers.Main + Job())

    var activeModel: ModelManager.ModelInfo? = null
        private set

    val displayName: String get() = activeModel?.displayName ?: "NexuzyAI"

    var onStateChange: ((MLCEngineWrapper.EngineState) -> Unit)?
        get() = mlcWrapper.onStateChange
        set(v) { mlcWrapper.onStateChange = v }

    var onTokenStream: ((String) -> Unit)?
        get() = mlcWrapper.onTokenStream
        set(v) { mlcWrapper.onTokenStream = v }

    private val chatHistory = mutableListOf<Pair<String, String>>()

    init {
        val recommended = modelManager.recommendedModel()
        activeModel = recommended
        Log.d("QwenEngine", "Auto-selected: ${recommended.displayName}")
    }

    fun loadRecommendedModel() {
        val model = modelManager.recommendedModel()
        activeModel = model
        mlcWrapper.loadModel(model.modelId, model.modelId)
    }

    fun loadModel(model: ModelManager.ModelInfo) {
        activeModel = model
        mlcWrapper.loadModel(model.modelId, model.modelId)
    }

    /**
     * Generate a response.
     *
     * @param prompt      Full built prompt (from PromptBuilder)
     * @param userQuery   Raw user message (for DuckDuckGo grounding + offline NLP)
     * @param toolContext Tool result already fetched (weather/news/location/link)
     */
    suspend fun generate(
        prompt: String,
        userQuery: String = "",
        toolContext: String = ""
    ): String {
        val result = mlcWrapper.generate(
            prompt      = prompt,
            userQuery   = userQuery,
            toolContext = toolContext,
            history     = chatHistory
        )
        // Save to history only if meaningful answer
        if (result.isNotBlank() && !result.startsWith("📵")) {
            val key = userQuery.ifBlank { prompt.take(80) }
            chatHistory.add(Pair(key, result))
            if (chatHistory.size > 10) chatHistory.removeAt(0)
        }
        return result
    }

    fun resetHistory() {
        chatHistory.clear()
        mlcWrapper.resetChat()
    }

    fun unload() = mlcWrapper.unload()
    fun getState() = mlcWrapper.getState()
}
