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
 * Decision tree:
 *
 *  1a. MLC downloaded + Internet ON
 *      → BEST MODE: MLC + Sarvaam AI + DuckDuckGo all active
 *      → Device control always available
 *      → Auto-go to chat in 2s
 *
 *  1b. MLC downloaded + NO Internet
 *      → Full offline AI ready
 *      → Device control always available
 *      → Auto-go to chat in 2s
 *
 *  2.  No MLC + Internet ON
 *      → Auto-download MLC in background
 *      → Device control available while downloading
 *      → Skip → use Sarvaam AI online
 *
 *  3.  No MLC + NO Internet
 *      → Limited mode: basic NLP + device control
 *      → Device control STILL works — always offline
 *
 * Device control features (ALWAYS available, no internet, no MLC needed):
 *   ⏰ Set alarms     🔦 Flashlight on/off    🎵 Media play/pause/next
 *   🔊 Volume control  🚀 Open any app         📍 GPS location
 *   🎙️ Voice input     ⏱️ Stopwatch/timer
 */
class FirstLaunchActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "nexuzy_prefs"
        private const val KEY_FIRST  = "first_launch_done"

        // These features work in EVERY case — no internet, no MLC needed
        private const val ALWAYS_AVAILABLE =
            "

📱 Always available (no internet, no AI needed):\n" +
            "⏰ Set alarms · 🔦 Flashlight · 🎵 Media control\n" +
            "🔊 Volume · 🚀 Open any app · 📍 GPS location\n" +
            "🎙️ Voice input · ⏱️ Timers · 🕒 Date & time"
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
        val recommended   = modelManager.recommendedModel()
        val sizeStr       = modelManager.formatSize(recommended.estimatedBytes)
        val mlcDownloaded = downloadManager.isModelDownloaded(recommended)
        val hasInternet   = NetworkUtils.isInternetAvailable(this)

        tvTitle.text    = "🤖 Welcome to NexuzyAI"
        tvSubtitle.text = "Your private on-device AI assistant."

        progressBar.visibility = View.GONE
        tvProgress.visibility  = View.GONE
        btnAction.visibility   = View.VISIBLE

        when {

            // ══ CASE 1a: MLC downloaded + Internet ON → BEST MODE ══════════════
            mlcDownloaded && hasInternet -> {
                tvInternetBadge.text = "🌟 Best Mode: MLC + Sarvaam AI + DuckDuckGo"
                tvInternetBadge.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark))

                tvStatus.text =
                    "🌟 You have the BEST possible setup!\n\n" +
                    "✅ MLC on-device model — private, fast\n" +
                    "✅ Sarvaam AI — cloud intelligence\n" +
                    "✅ DuckDuckGo — real-time web grounding\n" +
                    "✅ Live weather, news, web search\n" +
                    "✅ Works offline too (MLC handles it)" +
                    ALWAYS_AVAILABLE

                tvModelInfo.text =
                    "Model: ${recommended.displayName} ✅ Ready\n" +
                    "Size: $sizeStr · RAM: ${modelManager.getRamLabel()}\n" +
                    "Internet: 🟢 Connected"

                btnAction.visibility = View.GONE
                btnSkip.text         = "🚀 Start NexuzyAI (Best Mode)"
                btnSkip.setOnClickListener { finishSetup() }
                btnSkip.postDelayed({ finishSetup() }, 2000)
            }

            // ══ CASE 1b: MLC downloaded + NO Internet → FULL OFFLINE AI ══════
            mlcDownloaded && !hasInternet -> {
                tvInternetBadge.text = "🟢 AI Model Ready — Full offline mode"
                tvInternetBadge.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark))

                tvStatus.text =
                    "✅ Full offline AI ready!\n\n" +
                    "✅ Full AI conversations — any question, any topic\n" +
                    "✅ 100% private — zero data sent anywhere\n" +
                    "❌ Weather / news / web search (need internet)" +
                    ALWAYS_AVAILABLE

                tvModelInfo.text =
                    "Model: ${recommended.displayName} ✅ Ready\n" +
                    "Size: $sizeStr · RAM: ${modelManager.getRamLabel()}\n" +
                    "Internet: 🔴 Offline (not needed for AI chat)"

                btnAction.visibility = View.GONE
                btnSkip.text         = "▶️ Start NexuzyAI"
                btnSkip.setOnClickListener { finishSetup() }
                btnSkip.postDelayed({ finishSetup() }, 2000)
            }

            // ══ CASE 2: No MLC + Internet ON → AUTO-DOWNLOAD ══════════════
            !mlcDownloaded && hasInternet -> {
                tvInternetBadge.text = "🟢 Internet: Connected — downloading AI model…"
                tvInternetBadge.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark))

                tvStatus.text =
                    "🚀 Setting up your AI assistant…\n\n" +
                    "⬇️ Downloading AI model ($sizeStr) — ONE TIME only.\n" +
                    "After this, NexuzyAI works FULLY OFFLINE forever.\n\n" +
                    "ℹ️ Cannot bundle in APK ($sizeStr > 100 MB Play limit)." +
                    ALWAYS_AVAILABLE

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
                        "⚠️ Cancelled. Using Sarvaam AI online mode.",
                        Toast.LENGTH_LONG).show()
                    finishSetup()
                }

                btnSkip.text = "▶️ Skip — Use Online AI (Sarvaam + DuckDuckGo)"
                btnSkip.setOnClickListener {
                    downloadManager.cancelDownload()
                    finishSetup()
                }

                startDownload(recommended)
            }

            // ══ CASE 3: No MLC + NO Internet → LIMITED (but device control works!) ═
            else -> {
                tvInternetBadge.text = "🔴 No Internet — AI model not yet downloaded"
                tvInternetBadge.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark))

                tvStatus.text =
                    "⚠️ No internet AND AI model not downloaded yet.\n\n" +
                    "❌ Real AI conversations (need MLC model)\n" +
                    "   Connect Wi-Fi → reopen app → auto-downloads\n" +
                    "   Download once → full offline AI forever.\n\n" +
                    "✅ Basic NLP: greetings, date/time, math, identity" +
                    ALWAYS_AVAILABLE

                tvModelInfo.text =
                    "Model needed: ${recommended.displayName} ($sizeStr)\n" +
                    "Download once → full offline AI forever.\n" +
                    "Your RAM: ${modelManager.getRamLabel()}"

                btnAction.text      = "❌ Cannot Download — No Internet"
                btnAction.isEnabled = false

                btnSkip.text = "▶️ Start (Device Control + Basic Mode)"
                btnSkip.setOnClickListener { finishSetup() }
            }
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
                        tvProgress.text      = "⬇️ $dlMb MB / $totMb MB ($percent%)"
                    }
                },
                onComplete = {
                    runOnUiThread {
                        isDownloading        = false
                        progressBar.progress = 100
                        tvProgress.text      = "✅ Done! Full offline AI + device control enabled."
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
                            "⚠️ Download failed. Starting with online AI + device control.",
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
    }
}
