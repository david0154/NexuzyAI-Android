package ai.nexuzy.assistant.llm

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * ModelManager — detects device RAM and picks the best David AI model tier.
 *
 * Model Tier Mapping:
 * ┌──────────────────┬─────────────────────────────────┬───────────┬────────────────┐
 * │ Display Name     │ Backend Model ID                │ RAM Req   │ Params         │
 * ├──────────────────┼─────────────────────────────────┼───────────┼────────────────┤
 * │ David AI Lite    │ Qwen3-0.6B-q0f16-MLC            │ < 3 GB    │ 0.6 B          │
 * │ David AI 1B      │ Qwen3-1.7B-q4f16_1-MLC          │ 3–5 GB    │ 1.7 B (q4)     │
 * │ David AI 2B      │ gemma-2-2b-it-q4f16_1-MLC       │ > 5 GB    │ 2 B            │
 * └──────────────────┴─────────────────────────────────┴───────────┴────────────────┘
 *
 * RAM detection via ActivityManager.MemoryInfo.totalMem
 */
class ModelManager(private val context: Context) {

    companion object {
        const val TAG = "ModelManager"

        // MLC model IDs (backend, used by MLCEngineWrapper)
        const val MODEL_ID_LITE = "Qwen3-0.6B-q0f16-MLC"
        const val MODEL_ID_1B   = "Qwen3-1.7B-q4f16_1-MLC"
        const val MODEL_ID_2B   = "gemma-2-2b-it-q4f16_1-MLC"

        // HuggingFace URLs
        const val HF_LITE = "https://huggingface.co/mlc-ai/Qwen3-0.6B-q0f16-MLC"
        const val HF_1B   = "https://huggingface.co/mlc-ai/Qwen3-1.7B-q4f16_1-MLC"
        const val HF_2B   = "https://huggingface.co/mlc-ai/gemma-2-2b-it-q4f16_1-MLC"

        // David AI display names shown in UI
        const val DISPLAY_LITE = "David AI Lite"
        const val DISPLAY_1B   = "David AI 1B"
        const val DISPLAY_2B   = "David AI 2B"
    }

    data class ModelInfo(
        val modelId: String,
        val displayName: String,
        val hfUrl: String,
        val estimatedBytes: Long,
        val minRamBytes: Long,
        val description: String
    )

    val allModels = listOf(
        ModelInfo(
            modelId      = MODEL_ID_LITE,
            displayName  = DISPLAY_LITE,
            hfUrl        = HF_LITE,
            estimatedBytes = 700L * 1024 * 1024,    // ~700 MB
            minRamBytes  = 2L * 1024 * 1024 * 1024, // 2 GB
            description  = "Ultra-light. Best for low-RAM devices (\u2264 3GB RAM). Fast responses."
        ),
        ModelInfo(
            modelId      = MODEL_ID_1B,
            displayName  = DISPLAY_1B,
            hfUrl        = HF_1B,
            estimatedBytes = 1_400L * 1024 * 1024,  // ~1.4 GB
            minRamBytes  = 3L * 1024 * 1024 * 1024, // 3 GB
            description  = "Balanced. Best for most phones (3\u20136 GB RAM). Good quality."
        ),
        ModelInfo(
            modelId      = MODEL_ID_2B,
            displayName  = DISPLAY_2B,
            hfUrl        = HF_2B,
            estimatedBytes = 2_100L * 1024 * 1024,  // ~2.1 GB
            minRamBytes  = 5L * 1024 * 1024 * 1024, // 5 GB
            description  = "Full power. Best for flagship phones (6+ GB RAM). Best quality."
        )
    )

    /** Total physical RAM in bytes */
    fun getTotalRamBytes(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }

    /** Available RAM in bytes right now */
    fun getAvailableRamBytes(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem
    }

    /** RAM label for UI: "4.0 GB" */
    fun getRamLabel(): String {
        val gb = getTotalRamBytes().toDouble() / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }

    /**
     * Auto-select best model for this device.
     * Logic: pick the highest-tier model whose minRamBytes <= totalRam * 0.55
     * (leave 45% headroom for OS + app overhead)
     */
    fun recommendedModel(): ModelInfo {
        val totalRam = getTotalRamBytes()
        val usableRam = (totalRam * 0.55).toLong()
        Log.d(TAG, "Total RAM: ${totalRam / 1024 / 1024} MB | Usable: ${usableRam / 1024 / 1024} MB")
        return allModels.filter { it.minRamBytes <= usableRam }.maxByOrNull { it.minRamBytes }
            ?: allModels.first() // fallback to Lite
    }

    /** Models that can run on this device */
    fun compatibleModels(): List<ModelInfo> {
        val usableRam = (getTotalRamBytes() * 0.55).toLong()
        return allModels.filter { it.minRamBytes <= usableRam }
    }

    /** Format bytes to human-readable MB/GB */
    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        else -> String.format("%d MB", bytes / (1024 * 1024))
    }
}
