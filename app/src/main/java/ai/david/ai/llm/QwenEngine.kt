package ai.david.ai.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class QwenEngine(private val context: Context) {
    private val mlcWrapper = MLCEngineWrapper(context)
    val modelManager = ModelManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    var activeModel: ModelManager.ModelInfo? = null
        private set
    val displayName: String get() = activeModel?.displayName ?: "David AI"
    var onStateChange: ((MLCEngineWrapper.EngineState) -> Unit)?
        get() = mlcWrapper.onStateChange
        set(v) { mlcWrapper.onStateChange = v }
    var onTokenStream: ((String) -> Unit)?
        get() = mlcWrapper.onTokenStream
        set(v) { mlcWrapper.onTokenStream = v }
    private val chatHistory = mutableListOf<Pair<String,String>>()
    init {
        activeModel = modelManager.recommendedModel()
        Log.d("QwenEngine", "Selected: ${activeModel?.displayName} for ${modelManager.getRamLabel()} RAM")
    }
    fun loadRecommendedModel() { val m = modelManager.recommendedModel(); activeModel = m; mlcWrapper.loadModel(m.modelId, m.modelId) }
    fun loadModel(model: ModelManager.ModelInfo) { activeModel = model; mlcWrapper.loadModel(model.modelId, model.modelId) }
    suspend fun generate(prompt: String): String {
        val result = mlcWrapper.generate(prompt, chatHistory)
        if (result.isNotBlank() && !result.startsWith("Error") && !result.startsWith("💡")) {
            val userLine = prompt.lines().lastOrNull { it.isNotBlank() } ?: prompt
            chatHistory.add(Pair(userLine, result))
            if (chatHistory.size > 10) chatHistory.removeAt(0)
        }
        return result
    }
    fun resetHistory() { chatHistory.clear(); mlcWrapper.resetChat() }
    fun unload() = mlcWrapper.unload()
    fun getState() = mlcWrapper.getState()
}
