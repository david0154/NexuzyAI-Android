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
 * Decision tree (checked in this exact order):
 *
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │ 1. MLC already downloaded?                                      │
 *  │      YES → "✅ Full offline AI ready!" → auto-go to chat       │
 *  │            (internet status does NOT matter at all)             │
 *  │      NO  → check internet ↓                                     │
 *  ├─────────────────────────────────────────────────────────────────┤
 *  │ 2. Internet ON?                                                  │
 *  │      YES → auto-download MLC in background                      │
 *  │            skip button → use Sarvaam AI online instead          │
 *  │      NO  → show honest limited-mode warning                     │
 *  │            "connect Wi-Fi to auto-download"                     │
 *  └─────────────────────────────────────────────────────────────────┘
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
    private lateinit var btnAction: Button
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
        val recommended    = modelManager.recommendedModel()
        val sizeStr        = modelManager.formatSize(recommended.estimatedBytes)
        val mlcDownloaded  = downloadManager.isModelDownloaded(recommended)  // ← KEY CHECK
        val hasInternet    = NetworkUtils.isInternetAvailable(this)

        tvTitle.text    = "🤖 Welcome to NexuzyAI"
        tvSubtitle.text = "Your private on-device AI assistant."

        // ══════════════════════════════════════════════════════════════
        // CASE 1: MLC model already on disk → FULL OFFLINE AI READY
        //         Internet status is completely irrelevant here.
        // ══════════════════════════════════════════════════════════════
        if (mlcDownloaded) {
            tvInternetBadge.text = "🟢 AI Model: Already Downloaded"
            tvInternetBadge.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark))

            tvStatus.text =
                "✅ Full offline AI is ready!\n\n" +
                "NexuzyAI will work 100% offline with NO internet needed:\n" +
                "✅ Full AI conversations — any question, any topic\n" +
                "✅ Date, time, math, identity\n" +
                "✅ Device control (alarms, flashlight, media)\n" +
                "✅ GPS location\n\n" +
                "🌐 Connect internet anytime for live weather, news & web search."

            tvModelInfo.text =
                "Model: ${recommended.displayName}\n" +
                "Size: $sizeStr · RAM: ${modelManager.getRamLabel()}\n" +
                "Status: ✅ Downloaded and ready"

            progressBar.visibility  = View.GONE
            tvProgress.visibility   = View.GONE
            btnAction.visibility    = View.GONE

            btnSkip.text = "▶️ Start NexuzyAI"
            btnSkip.setOnClickListener { finishSetup() }

            // Auto-proceed after 2s — no need to make user tap anything
            btnSkip.postDelayed({ finishSetup() }, 2000)
            return
        }

        // ══════════════════════════════════════════════════════════════
        // CASE 2: MLC NOT downloaded + Internet ON → auto-download
        // ══════════════════════════════════════════════════════════════
        if (hasInternet) {
            tvInternetBadge.text = "🟢 Internet: Connected — downloading AI model automatically"
            tvInternetBadge.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark))

            tvStatus.text =
                "🚀 Setting up your AI assistant…\n\n" +
                "⬇️ Downloading AI model ($sizeStr) — ONE TIME only.\n" +
                "After this download, NexuzyAI works FULLY OFFLINE forever.\n" +
                "No internet needed ever again for AI chat.\n\n" +
                "ℹ️ Model cannot be bundled in APK ($sizeStr > 100 MB limit).\n" +
                "This is standard for all on-device AI apps."

            tvModelInfo.text =
                "Model: ${recommended.displayName}\n" +
                "Size: $sizeStr · RAM: ${modelManager.getRamLabel()}\n" +
                recommended.description

            progressBar.visibility = View.VISIBLE
            tvProgress.visibility  = View.VISIBLE
            progressBar.progress   = 0
            tvProgress.text        = "Starting download…"

            btnAction.text      = "⏸️ Cancel — Use Online AI Instead"
            btnAction.isEnabled = true
            btnAction.setOnClickListener {
                downloadManager.cancelDownload()
                isDownloading = false
                Toast.makeText(this,
                    "⚠️ Download cancelled. Using Sarvaam AI online mode.",
                    Toast.LENGTH_LONG).show()
                finishSetup()
            }

            btnSkip.text = "▶️ Skip — Use Online AI (Sarvaam + DuckDuckGo)"
            btnSkip.setOnClickListener {
                downloadManager.cancelDownload()
                finishSetup()
            }

            startDownload(recommended)
            return
        }

        // ══════════════════════════════════════════════════════════════
        // CASE 3: MLC NOT downloaded + NO Internet → honest limited warning
        //         (This is the ONLY case where internet warning is shown)
        // ══════════════════════════════════════════════════════════════
        tvInternetBadge.text = "🔴 Internet: Offline — AI model not yet downloaded"
        tvInternetBadge.setTextColor(
            ContextCompat.getColor(this, android.R.color.holo_red_dark))

        tvStatus.text =
            "⚠️ No internet detected AND AI model not downloaded yet.\n\n" +
            "Right now you can use:\n" +
            "✅ Greetings, date/time, math, identity questions\n" +
            "✅ Device control (alarms, flashlight, media)\n" +
            "✅ GPS location (coordinates)\n\n" +
            "❌ Real AI conversations — need MLC model\n" +
            "   (requires internet ONCE to download, then works offline forever)\n\n" +
            "💡 Connect to Wi-Fi then reopen the app.\n" +
            "   Download happens automatically — no setup needed."

        tvModelInfo.text =
            "Model needed: ${recommended.displayName} ($sizeStr)\n" +
            "Download once → works offline forever.\n" +
            "Your device RAM: ${modelManager.getRamLabel()}"

        progressBar.visibility = View.GONE
        tvProgress.visibility  = View.GONE

        btnAction.text      = "❌ No Internet — Cannot Download Now"
        btnAction.isEnabled = false

        btnSkip.text = "▶️ Start with Basic Mode (Connect Wi-Fi Later)"
        btnSkip.setOnClickListener { finishSetup() }
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
                        tvProgress.text      = "⬇️ $dlMb MB / $totMb MB ($percent%)"
                    }
                },
                onComplete = {
                    runOnUiThread {
                        isDownloading        = false
                        progressBar.progress = 100
                        tvProgress.text      = "✅ AI model ready! Full offline AI enabled."
                        Toast.makeText(this@FirstLaunchActivity,
                            "✅ Download complete! NexuzyAI now works 100% offline.",
                            Toast.LENGTH_LONG).show()
                        progressBar.postDelayed({ finishSetup() }, 1500)
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        isDownloading = false
                        if (error == "Download cancelled") return@runOnUiThread
                        tvProgress.text = "⚠️ Download failed: $error"
                        Toast.makeText(this@FirstLaunchActivity,
                            "⚠️ Download failed. Starting with online AI mode.",
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
        // Don't auto-cancel on destroy — background download continues if user presses Home
    }
}
