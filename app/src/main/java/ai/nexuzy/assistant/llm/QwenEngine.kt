package ai.nexuzy.assistant.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QwenEngine — High-level interface for David AI.
 * Wraps MLCEngineWrapper and ModelManager.
 * Exposes the active model's David AI display name (e.g. "David AI 1B").
 */
class QwenEngine(private val context: Context) {

    private val mlcWrapper = MLCEngineWrapper(context)
    val modelManager = ModelManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Currently loaded model info
    var activeModel: ModelManager.ModelInfo? = null
        private set

    /** Display name shown in UI badge: "David AI 1B", "David AI Lite" etc */
    val displayName: String get() = activeModel?.displayName ?: "David AI"

    var onStateChange: ((MLCEngineWrapper.EngineState) -> Unit)?
        get() = mlcWrapper.onStateChange
        set(v) { mlcWrapper.onStateChange = v }

    var onTokenStream: ((String) -> Unit)?
        get() = mlcWrapper.onTokenStream
        set(v) { mlcWrapper.onTokenStream = v }

    private val chatHistory = mutableListOf<Pair<String, String>>()

    init {
        // Auto-select best model for this device on init
        val recommended = modelManager.recommendedModel()
        activeModel = recommended
        Log.d("QwenEngine", "Auto-selected: ${recommended.displayName} for ${modelManager.getRamLabel()} RAM")
    }

    /** Load the auto-recommended model */
    fun loadRecommendedModel() {
        val model = modelManager.recommendedModel()
        activeModel = model
        mlcWrapper.loadModel(model.modelId, model.modelId)
    }

    /** Manually load a specific model by ModelInfo */
    fun loadModel(model: ModelManager.ModelInfo) {
        activeModel = model
        mlcWrapper.loadModel(model.modelId, model.modelId)
    }

    suspend fun generate(prompt: String): String {
        val result = mlcWrapper.generate(prompt, chatHistory)
        if (result.isNotBlank() && !result.startsWith("Error") && !result.startsWith("💡")) {
            val userPrompt = prompt.lines().lastOrNull { it.isNotBlank() } ?: prompt
            chatHistory.add(Pair(userPrompt, result))
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
