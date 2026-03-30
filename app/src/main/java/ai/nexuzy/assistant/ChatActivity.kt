package ai.nexuzy.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import ai.nexuzy.assistant.adapter.ChatAdapter
import ai.nexuzy.assistant.databinding.ActivityChatBinding
import ai.nexuzy.assistant.llm.MLCEngineWrapper
import ai.nexuzy.assistant.llm.ModelManager
import ai.nexuzy.assistant.llm.PromptBuilder
import ai.nexuzy.assistant.llm.QwenEngine
import ai.nexuzy.assistant.middleware.IntentClassifier
import ai.nexuzy.assistant.middleware.ToolExecutor
import ai.nexuzy.assistant.middleware.ToolResult
import ai.nexuzy.assistant.model.ChatMessage
import ai.nexuzy.assistant.tools.LocationTool
import ai.nexuzy.assistant.ui.ModelSelectorFragment
import java.util.Locale

class ChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var toolExecutor: ToolExecutor
    private lateinit var locationTool: LocationTool
    private lateinit var qwenEngine: QwenEngine

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Android 15+ Edge-to-Edge ─────────────────────────────────
        // When targetSdk >= 35, the system enforces edge-to-edge by default.
        // setDecorFitsSystemWindows(false) tells the window NOT to fit system
        // bars automatically — we handle insets manually below.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Apply Window Insets for Edge-to-Edge padding ───────────────
        // This pads the root view so content never hides behind:
        //   • Status bar (top)
        //   • Navigation bar (bottom)
        //   • Display cutout / notch (top on most phones)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        toolExecutor = ToolExecutor(this)
        locationTool = LocationTool(this)
        qwenEngine   = QwenEngine(this)

        setupRecyclerView()
        setupAdMob()
        setupSTT()
        setupTTS()
        setupEngineObserver()
        setupBackHandler()
        requestPermissions()
        updateModelBadge()

        // Greeting
        addAIMessage("👋 Hi! I'm ${qwenEngine.displayName} — your private on-device AI.\n" +
            "Try: \"What's the weather?\", \"Latest news\", \"Who made you?\", or \"Open YouTube\"")

        binding.sendBtn.setOnClickListener {
            val text = binding.messageInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
        }
        binding.voiceBtn.setOnClickListener { toggleVoice() }
        binding.clearBtn.setOnClickListener {
            qwenEngine.resetHistory()
            messages.clear()
            chatAdapter.notifyDataSetChanged()
            addAIMessage("👋 Chat cleared! I'm ${qwenEngine.displayName}. Ask me anything.")
        }
        binding.aboutBtn.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.modelBadge.setOnClickListener { showModelSelector() }
    }

    // ── Android 16 Predictive Back Gesture ─────────────────────────
    // OnBackPressedCallback replaces the deprecated onBackPressed().
    // This enables the predictive back swipe animation on Android 16.
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If keyboard is open, close it first; else finish activity
                val rootView = binding.root
                val insets = ViewCompat.getRootWindowInsets(rootView)
                val imeVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
                if (imeVisible) {
                    rootView.clearFocus()
                } else {
                    finish()
                }
            }
        })
    }

    private fun updateModelBadge() {
        binding.modelBadge.text = qwenEngine.displayName
    }

    private fun setupEngineObserver() {
        qwenEngine.onStateChange = { state ->
            val label = when (state) {
                MLCEngineWrapper.EngineState.LOADING    -> "⏳ Loading…"
                MLCEngineWrapper.EngineState.READY      -> "${qwenEngine.displayName} • Ready"
                MLCEngineWrapper.EngineState.GENERATING -> "⚙️ Thinking…"
                MLCEngineWrapper.EngineState.FAILED     -> "❌ Failed"
                MLCEngineWrapper.EngineState.NOT_LOADED -> qwenEngine.displayName
            }
            runOnUiThread { binding.modelBadge.text = label }
        }
        qwenEngine.onTokenStream = { token ->
            runOnUiThread {
                if (messages.isNotEmpty() && !messages.last().isUser) {
                    val last = messages.last()
                    messages[messages.size - 1] = last.copy(text = last.text + token)
                    chatAdapter.notifyItemChanged(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun showModelSelector() {
        ModelSelectorFragment(
            modelManager = qwenEngine.modelManager,
            onModelSelected = { model ->
                addAIMessage("🔄 Switching to ${model.displayName}… (requires MLC build)")
                qwenEngine.loadModel(model)
                updateModelBadge()
            }
        ).show(supportFragmentManager, "model_selector")
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupAdMob() {
        adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }
        binding.adContainer.addView(adView)
        adView?.loadAd(AdRequest.Builder().build())
    }

    fun processInput(userInput: String) {
        addUserMessage(userInput)
        showTyping(true)
        lifecycleScope.launch {
            try {
                val intent = IntentClassifier.classify(userInput)

                // MODEL_INFO intent — build response from real model data
                val toolResult = if (intent == IntentClassifier.Intent.MODEL_INFO) {
                    val m = qwenEngine.activeModel
                    val info = buildString {
                        appendLine("🧠 I am ${qwenEngine.displayName}")
                        if (m != null) {
                            appendLine("🔧 Backend model : ${m.modelId}")
                            appendLine("💾 Model size     : ~${qwenEngine.modelManager.formatSize(m.estimatedBytes)}")
                            appendLine("📱 Your device RAM: ${qwenEngine.modelManager.getRamLabel()}")
                            appendLine("🔒 Runs on-device, zero data collected.")
                        }
                    }
                    ai.nexuzy.assistant.middleware.ToolResult(
                        type = ToolResult.Type.INFO, content = info, directReply = true
                    )
                } else {
                    toolExecutor.execute(intent, userInput)
                }

                // Direct replies (location, actions, developer info) — no AI needed
                if (toolResult.directReply && toolResult.content.isNotBlank()) {
                    showTyping(false)
                    addAIMessage(toolResult.content)
                    speakIfReady(toolResult.content)
                    return@launch
                }

                // Build prompt with tool context
                val locationHint = locationTool.getLocationSystemPrompt()
                val prompt = PromptBuilder.build(
                    userInput       = userInput,
                    toolContext     = toolResult.content,
                    locationHint    = locationHint,
                    activeModelName = qwenEngine.displayName
                )

                // ── Generate — passes userQuery + toolContext for full hybrid routing ─
                // • Internet ON  → HybridAnswerEngine: DuckDuckGo + Sarvaam AI fused
                // • Internet OFF → LocalOfflineEngine: MLC or smart rule-based NLP
                addAIMessage("")
                val response = qwenEngine.generate(
                    prompt      = prompt,
                    userQuery   = userInput,
                    toolContext = toolResult.content
                )
                if (messages.isNotEmpty() && !messages.last().isUser) {
                    messages[messages.size - 1] = messages.last().copy(text = response)
                    chatAdapter.notifyItemChanged(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
                showTyping(false)
                speakIfReady(response)

            } catch (e: Exception) {
                showTyping(false)
                addAIMessage("⚠️ ${e.message}")
            }
        }
    }

    private fun speakIfReady(text: String) {
        if (ttsReady && text.isNotBlank()) {
            val plain = text.replace(Regex("[*_`#>\\[\\]()]"), "").take(250)
            tts?.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "ai")
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun addAIMessage(text: String) {
        messages.add(ChatMessage(text, isUser = false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun showTyping(show: Boolean) {
        runOnUiThread {
            binding.typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun setupSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                isListening = false; updateVoiceBtn(false)
                binding.voiceOrb.visibility = View.GONE
                if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
            }
            override fun onError(error: Int)        { isListening = false; updateVoiceBtn(false); binding.voiceOrb.visibility = View.GONE }
            override fun onReadyForSpeech(p: Bundle?) { binding.voiceOrb.visibility = View.VISIBLE }
            override fun onBeginningOfSpeech()       {}
            override fun onRmsChanged(rms: Float) {
                val s = 1f + (rms.coerceIn(0f, 10f) / 20f)
                binding.voiceOrb.scaleX = s; binding.voiceOrb.scaleY = s
            }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech()             { binding.voiceOrb.visibility = View.GONE }
            override fun onPartialResults(partial: Bundle?) {
                val p = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (p.isNotEmpty()) binding.messageInput.setText(p)
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
    }

    private fun setupTTS() { tts = TextToSpeech(this, this) }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { tts?.language = Locale("en", "IN"); ttsReady = true }
    }

    private fun toggleVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 102); return
        }
        if (!isListening) {
            isListening = true; updateVoiceBtn(true)
            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        } else {
            isListening = false; updateVoiceBtn(false); speechRecognizer?.stopListening()
        }
    }

    private fun updateVoiceBtn(listening: Boolean) {
        binding.voiceBtn.setIconResource(
            if (listening) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION), 101)
    }

    override fun onDestroy() { super.onDestroy(); tts?.shutdown(); speechRecognizer?.destroy(); adView?.destroy(); qwenEngine.unload() }
    override fun onPause()   { super.onPause();  adView?.pause() }
    override fun onResume()  { super.onResume(); adView?.resume() }
}
