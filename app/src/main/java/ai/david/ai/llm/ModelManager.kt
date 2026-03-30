package ai.david.ai.llm

import android.app.ActivityManager
import android.content.Context
import android.util.Log

class ModelManager(private val context: Context) {
    companion object {
        const val TAG = "ModelManager"
        const val MODEL_ID_LITE = "Qwen3-0.6B-q0f16-MLC"
        const val MODEL_ID_1B   = "Qwen3-1.7B-q4f16_1-MLC"
        const val MODEL_ID_2B   = "gemma-2-2b-it-q4f16_1-MLC"
        const val HF_LITE = "https://huggingface.co/mlc-ai/Qwen3-0.6B-q0f16-MLC"
        const val HF_1B   = "https://huggingface.co/mlc-ai/Qwen3-1.7B-q4f16_1-MLC"
        const val HF_2B   = "https://huggingface.co/mlc-ai/gemma-2-2b-it-q4f16_1-MLC"
        const val DISPLAY_LITE = "David AI Lite"
        const val DISPLAY_1B   = "David AI 1B"
        const val DISPLAY_2B   = "David AI 2B"
    }
    data class ModelInfo(val modelId: String, val displayName: String, val hfUrl: String, val estimatedBytes: Long, val minRamBytes: Long, val description: String)
    val allModels = listOf(
        ModelInfo(MODEL_ID_LITE, DISPLAY_LITE, HF_LITE, 700L*1024*1024,  2L*1024*1024*1024, "Ultra-light. For low-RAM phones (≤3 GB). Fast."),
        ModelInfo(MODEL_ID_1B,   DISPLAY_1B,   HF_1B,   1_400L*1024*1024, 3L*1024*1024*1024, "Balanced. For most phones (3–6 GB RAM)."),
        ModelInfo(MODEL_ID_2B,   DISPLAY_2B,   HF_2B,   2_100L*1024*1024, 5L*1024*1024*1024, "Full power. For flagship phones (6+ GB RAM).")
    )
    fun getTotalRamBytes(): Long { val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager; val i = ActivityManager.MemoryInfo(); am.getMemoryInfo(i); return i.totalMem }
    fun getAvailableRamBytes(): Long { val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager; val i = ActivityManager.MemoryInfo(); am.getMemoryInfo(i); return i.availMem }
    fun getRamLabel(): String = String.format("%.1f GB", getTotalRamBytes() / (1024.0*1024*1024))
    fun recommendedModel(): ModelInfo {
        val usable = (getTotalRamBytes() * 0.55).toLong()
        Log.d(TAG, "RAM usable: ${usable/1024/1024} MB")
        return allModels.filter { it.minRamBytes <= usable }.maxByOrNull { it.minRamBytes } ?: allModels.first()
    }
    fun compatibleModels(): List<ModelInfo> = allModels.filter { it.minRamBytes <= (getTotalRamBytes()*0.55).toLong() }
    fun formatSize(bytes: Long): String = if (bytes >= 1024L*1024*1024) String.format("%.1f GB", bytes/(1024.0*1024*1024)) else String.format("%d MB", bytes/(1024*1024))
}
