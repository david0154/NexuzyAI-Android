package ai.nexuzy.assistant

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ai.nexuzy.assistant.llm.ModelManager
import ai.nexuzy.assistant.llm.ModelDownloadManager
import ai.nexuzy.assistant.tools.NetworkUtils

/**
 * FirstLaunchActivity — shown ONCE on first install.
 *
 * WHY the MLC model cannot be bundled in the APK:
 *   Google Play APK limit = 100 MB. MLC model = 700 MB – 2.1 GB.
 *   It is physically impossible to ship model weights inside the APK.
 *   This is the same constraint faced by ALL on-device AI apps
 *   (Gemini Nano, ChatGPT mobile, etc.) — all require a one-time download.
 *
 * What this screen does:
 *   • Internet OFF  → Explains user will get basic NLP now; shows "Start" button.
 *                    When they connect internet later, they can download from Settings.
 *   • Internet ON   → AUTO-starts MLC model download immediately (no button tap needed).
 *                    Shows live progress. "Skip" button always visible to abort.
 *                    After download: full AI works OFFLINE FOREVER.
 *
 * After first launch (skip or finish download):
 *   • Saves first_launch_done flag → goes directly to ChatActivity forever.
 *
 * Offline capability tiers:
 *   Tier 1 (no internet, no MLC):  Basic NLP only (greetings/date/math/identity)
 *   Tier 2 (MLC downloaded):       Full real AI offline — zero internet needed
 *   Tier 3 (internet, no MLC):     Sarvaam AI + DuckDuckGo cloud AI
 */
class FirstLaunchActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "nexuzy_prefs"
        private const val KEY_FIRST  = "first_launch_done"
    }

    private lateinit var modelManager: ModelManager
    private lateinit var downloadManager: ModelDownloadManager
    private var isDownloading = false

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvInternetBadge: TextView
    private lateinit var tvModelInfo: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAction: Button   // changes label based on state
    private lateinit var btnSkip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_first_launch)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        modelManager    = ModelManager(this)
        downloadManager = ModelDownloadManager(this)

        bindViews()
        setupUI()
    }

    private fun bindViews() {
        tvTitle         = findViewById(R.id.tvFirstTitle)
        tvSubtitle      = findViewById(R.id.tvFirstSubtitle)
        tvStatus        = findViewById(R.id.tvFirstStatus)
        tvInternetBadge = findViewById(R.id.tvInternetBadge)
        tvModelInfo     = findViewById(R.id.tvModelInfo)
        tvProgress      = findViewById(R.id.tvDownloadProgress)
        progressBar     = findViewById(R.id.downloadProgressBar)
        btnAction       = findViewById(R.id.btnDownloadModel)
        btnSkip         = findViewById(R.id.btnSkipDownload)
    }

    private fun setupUI() {
        val hasInternet  = NetworkUtils.isInternetAvailable(this)
        val recommended  = modelManager.recommendedModel()
        val sizeStr      = modelManager.formatSize(recommended.estimatedBytes)

        tvTitle.text    = "🤖 Welcome to NexuzyAI"
        tvSubtitle.text = "Your private on-device AI assistant."

        if (hasInternet) {
            // ── INTERNET ON: auto-start download ─────────────────────────────
            tvInternetBadge.text = "🟢 Internet: Connected — downloading AI model automatically"
            tvInternetBadge.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark))

            tvStatus.text =
                "🚀 Setting up your AI assistant…\n\n" +
                "ℹ️ The AI model ($sizeStr) is downloading in the background.\n" +
                "After this ONE-TIME download, NexuzyAI works FULLY OFFLINE forever\n" +
                "with no internet connection needed.\n\n" +
                "⚠️ Note: The model cannot be bundled inside the APK — it is too large\n" +
                "(${sizeStr} vs 100 MB APK limit). This is the same for all on-device AI apps."

            tvModelInfo.text =
                "Downloading: ${recommended.displayName}\n" +
                "Size: $sizeStr · RAM on your device: ${modelManager.getRamLabel()}\n" +
                recommended.description

            // Progress visible immediately
            progressBar.visibility = View.VISIBLE
            tvProgress.visibility  = View.VISIBLE
            progressBar.progress   = 0
            tvProgress.text        = "Starting download…"

            // Action button = cancel while downloading
            btnAction.text      = "⏸️ Pause / Use Online Mode Instead"
            btnAction.isEnabled = true
            btnAction.setOnClickListener {
                downloadManager.cancelDownload()
                isDownloading = false
                Toast.makeText(this,
                    "⚠️ Download paused. Using Sarvaam AI online mode.",
                    Toast.LENGTH_LONG).show()
                finishSetup()
            }

            // Skip = same as cancel
            btnSkip.text = "▶️ Skip — Use Online AI (Sarvaam + DuckDuckGo)"
            btnSkip.setOnClickListener {
                downloadManager.cancelDownload()
                finishSetup()
            }

            // Auto-start download
            startDownload(recommended)

        } else {
            // ── NO INTERNET: explain clearly what works and what doesn't ─────────
            tvInternetBadge.text = "🔴 Internet: Offline"
            tvInternetBadge.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark))

            tvStatus.text =
                "⚠️ No internet detected.\n\n" +
                "You can start the app now with limited offline capability:\n" +
                "✅ Greetings, date/time, math, identity questions\n" +
                "❌ Real AI conversations (need MLC model — requires internet to download)\n" +
                "❌ Weather, news, web search\n\n" +
                "💡 Connect to Wi-Fi and reopen the app to auto-download the AI model\n" +
                "   for full offline AI that works forever with no internet."

            tvModelInfo.text =
                "AI model needed: ${recommended.displayName} ($sizeStr)\n" +
                "Requires internet to download once. After that: 100% offline."

            // No download possible — hide progress
            progressBar.visibility = View.GONE
            tvProgress.visibility  = View.GONE

            // Action button = disabled (no internet)
            btnAction.text      = "❌ No Internet — Cannot Download"
            btnAction.isEnabled = false

            // Skip = start with basic mode
            btnSkip.text = "▶️ Start with Basic Offline Mode"
            btnSkip.setOnClickListener { finishSetup() }
        }
    }

    private fun startDownload(model: ModelManager.ModelInfo) {
        if (isDownloading) return
        isDownloading = true

        lifecycleScope.launch {
            downloadManager.downloadModel(
                model = model,
                onProgress = { downloaded, total ->
                    runOnUiThread {
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        val dlMb    = downloaded / (1024 * 1024)
                        val totMb   = total / (1024 * 1024)
                        progressBar.progress = percent
                        tvProgress.text      = "⬇️ Downloading AI model: $dlMb MB / $totMb MB ($percent%)"
                    }
                },
                onComplete = {
                    runOnUiThread {
                        isDownloading  = false
                        tvProgress.text = "✅ AI model ready! NexuzyAI will now work fully offline."
                        progressBar.progress = 100
                        Toast.makeText(this@FirstLaunchActivity,
                            "✅ AI model downloaded! Full offline AI enabled.",
                            Toast.LENGTH_LONG).show()
                        // Auto-proceed to chat after 1.5s
                        progressBar.postDelayed({ finishSetup() }, 1500)
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        isDownloading = false
                        if (error == "Download cancelled") return@runOnUiThread
                        tvProgress.text = "⚠️ Download failed: $error"
                        Toast.makeText(this@FirstLaunchActivity,
                            "⚠️ Download failed. Using online AI mode (Sarvaam + DuckDuckGo).",
                            Toast.LENGTH_LONG).show()
                        finishSetup()
                    }
                }
            )
        }
    }

    private fun finishSetup() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST, true).apply()
        startActivity(Intent(this, ChatActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't cancel on destroy — if user presses home, download continues
        // Only cancel if user explicitly tapped cancel/skip
    }
}
