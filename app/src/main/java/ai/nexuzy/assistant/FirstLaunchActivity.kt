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
 * Key design decisions:
 *   1. App is FULLY USABLE immediately — no internet or download required.
 *      LocalOfflineEngine handles all offline queries out of the box.
 *   2. MLC model download is OPTIONAL — only shown if internet is available.
 *      User can always skip and use the app right away.
 *   3. After this screen completes (skip or download), the flag is saved
 *      so this screen never shows again.
 *
 * Flow:
 *   ┌────────────────────────────────────────┐
 *   │  First Install                          │
 *   │   ↓                                    │
 *   │  Show welcome + internet status         │
 *   │   ├─ Internet OFF → Skip only           │
 *   │   └─ Internet ON  → Download or Skip    │
 *   │        ↓                 ↓              │
 *   │   Download MLC      Skip (use offline)  │
 *   │        └─────────────┘              │
 *   │              ↓                         │
 *   │        ChatActivity                     │
 *   └────────────────────────────────────────┘
 */
class FirstLaunchActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "nexuzy_prefs"
        private const val KEY_FIRST  = "first_launch_done"
    }

    private lateinit var modelManager: ModelManager
    private lateinit var downloadManager: ModelDownloadManager
    private var isDownloading = false

    // Views (using findViewById for simplicity — no new layout file needed)
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvInternetBadge: TextView
    private lateinit var tvModelInfo: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnDownload: Button
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

        modelManager   = ModelManager(this)
        downloadManager = ModelDownloadManager(this)

        bindViews()
        setupUI()
    }

    private fun bindViews() {
        tvTitle        = findViewById(R.id.tvFirstTitle)
        tvSubtitle     = findViewById(R.id.tvFirstSubtitle)
        tvStatus       = findViewById(R.id.tvFirstStatus)
        tvInternetBadge = findViewById(R.id.tvInternetBadge)
        tvModelInfo    = findViewById(R.id.tvModelInfo)
        tvProgress     = findViewById(R.id.tvDownloadProgress)
        progressBar    = findViewById(R.id.downloadProgressBar)
        btnDownload    = findViewById(R.id.btnDownloadModel)
        btnSkip        = findViewById(R.id.btnSkipDownload)
    }

    private fun setupUI() {
        val hasInternet   = NetworkUtils.isInternetAvailable(this)
        val recommended   = modelManager.recommendedModel()
        val modelSizeStr  = modelManager.formatSize(recommended.estimatedBytes)

        tvTitle.text    = "🤖 Welcome to NexuzyAI"
        tvSubtitle.text = "Your private on-device AI assistant.\nWorks completely offline — no account needed."

        // Internet status badge
        if (hasInternet) {
            tvInternetBadge.text = "🟢 Internet: Connected"
            tvInternetBadge.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvInternetBadge.text = "🔴 Internet: Offline"
            tvInternetBadge.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        // Status message
        tvStatus.text = "✅ App is ready to use RIGHT NOW — no download needed!\n" +
            "NexuzyAI works fully offline using its built-in AI engine.\n" +
            "Tap \"Start Now\" to begin, or optionally download the on-device\n" +
            "AI model for richer offline responses."

        // Model info
        tvModelInfo.text = "Optional: ${recommended.displayName}\n" +
            "Size: $modelSizeStr · RAM needed: ${modelManager.getRamLabel()}\n" +
            recommended.description

        // Download button — only enabled if internet is available
        if (hasInternet) {
            btnDownload.text      = "⬇️ Download AI Model ($modelSizeStr)"
            btnDownload.isEnabled = true
            btnDownload.setOnClickListener { startDownload(recommended) }
        } else {
            btnDownload.text      = "❌ No Internet — Download Unavailable"
            btnDownload.isEnabled = false
        }

        // Skip button — ALWAYS visible and enabled
        btnSkip.text = "▶️ Start Now (Use Offline AI)"
        btnSkip.setOnClickListener { finishSetup() }

        // Progress — hidden initially
        progressBar.visibility = View.GONE
        tvProgress.visibility  = View.GONE
    }

    private fun startDownload(model: ModelManager.ModelInfo) {
        if (isDownloading) return
        isDownloading = true

        btnDownload.isEnabled = false
        btnSkip.text          = "⏸️ Cancel Download"
        btnSkip.setOnClickListener {
            downloadManager.cancelDownload()
            isDownloading = false
            setupUI()  // reset UI
        }

        progressBar.visibility = View.VISIBLE
        tvProgress.visibility  = View.VISIBLE
        progressBar.progress   = 0
        tvProgress.text        = "Preparing download…"

        lifecycleScope.launch {
            downloadManager.downloadModel(
                model = model,
                onProgress = { downloaded, total ->
                    runOnUiThread {
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        val dlMb    = downloaded / (1024 * 1024)
                        val totMb   = total / (1024 * 1024)
                        progressBar.progress = percent
                        tvProgress.text      = "Downloading… $dlMb MB / $totMb MB ($percent%)"
                    }
                },
                onComplete = {
                    runOnUiThread {
                        tvProgress.text = "✅ Download complete!"
                        Toast.makeText(this@FirstLaunchActivity,
                            "✅ AI model downloaded successfully!", Toast.LENGTH_SHORT).show()
                        finishSetup()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        isDownloading = false
                        tvProgress.text = "⚠️ Download failed: $error"
                        Toast.makeText(this@FirstLaunchActivity,
                            "⚠️ Download failed. You can still use offline mode.",
                            Toast.LENGTH_LONG).show()
                        setupUI()  // reset to allow retry
                    }
                }
            )
        }
    }

    /** Save first-launch flag and go to ChatActivity */
    private fun finishSetup() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST, true).apply()
        startActivity(Intent(this, ChatActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isDownloading) downloadManager.cancelDownload()
    }
}
