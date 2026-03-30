package ai.david.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import ai.david.ai.adapter.ChatAdapter
import ai.david.ai.databinding.ActivityChatBinding
import ai.david.ai.llm.MLCEngineWrapper
import ai.david.ai.llm.ModelManager
import ai.david.ai.llm.PromptBuilder
import ai.david.ai.llm.QwenEngine
import ai.david.ai.middleware.IntentClassifier
import ai.david.ai.middleware.ToolExecutor
import ai.david.ai.middleware.ToolResult
import ai.david.ai.model.ChatMessage
import ai.david.ai.tools.LocationTool
import ai.david.ai.ui.ModelSelectorFragment
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
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolExecutor = ToolExecutor(this)
        locationTool = LocationTool(this)
        qwenEngine   = QwenEngine(this)
        setupRecyclerView()
        setupAdMob()
        setupSTT()
        setupTTS()
        setupEngineObserver()
        requestPermissions()
        updateModelBadge()
        addAIMessage("👋 Hi! I'm ${qwenEngine.displayName} — your private on-device AI.\n" +
            "Try: \"What's the weather?\", \"Latest news\", \"Who made you?\", \"Open YouTube\"")
        binding.sendBtn.setOnClickListener {
            val text = binding.messageInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
        }
        binding.voiceBtn.setOnClickListener { toggleVoice() }
        binding.clearBtn.setOnClickListener {
            qwenEngine.resetHistory(); messages.clear(); chatAdapter.notifyDataSetChanged()
            addAIMessage("👋 Chat cleared! I'm ${qwenEngine.displayName}. Ask me anything.")
        }
        binding.aboutBtn.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        binding.modelBadge.setOnClickListener { showModelSelector() }
    }

    private fun updateModelBadge() { binding.modelBadge.text = qwenEngine.displayName }

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
        ModelSelectorFragment(qwenEngine.modelManager) { model ->
            addAIMessage("🔄 Switching to ${model.displayName}…")
            qwenEngine.loadModel(model)
            updateModelBadge()
        }.show(supportFragmentManager, "model_selector")
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupAdMob() {
        adView = AdView(this).apply { setAdSize(AdSize.BANNER); adUnitId = BuildConfig.ADMOB_BANNER_ID }
        binding.adContainer.addView(adView)
        adView?.loadAd(AdRequest.Builder().build())
    }

    fun processInput(userInput: String) {
        addUserMessage(userInput)
        showTyping(true)
        lifecycleScope.launch {
            try {
                val intent = IntentClassifier.classify(userInput)
                val toolResult = if (intent == IntentClassifier.Intent.MODEL_INFO) {
                    val m = qwenEngine.activeModel
                    val info = buildString {
                        appendLine("🧠 I am ${qwenEngine.displayName}")
                        if (m != null) {
                            appendLine("🔧 Backend : ${m.modelId}")
                            appendLine("💾 Size    : ~${qwenEngine.modelManager.formatSize(m.estimatedBytes)}")
                            appendLine("📱 Your device RAM : ${qwenEngine.modelManager.getRamLabel()}")
                            appendLine("🔒 Runs on-device, zero data collected.")
                        }
                    }
                    ToolResult(type = ToolResult.Type.INFO, content = info, directReply = true)
                } else {
                    toolExecutor.execute(intent, userInput)
                }
                if (toolResult.directReply && toolResult.content.isNotBlank()) {
                    showTyping(false); addAIMessage(toolResult.content); speakIfReady(toolResult.content); return@launch
                }
                val locationHint = locationTool.getLocationSystemPrompt()
                val prompt = PromptBuilder.build(userInput, toolResult.content, locationHint, qwenEngine.displayName)
                addAIMessage("")
                val response = qwenEngine.generate(prompt)
                if (messages.isNotEmpty() && !messages.last().isUser) {
                    messages[messages.size - 1] = messages.last().copy(text = response)
                    chatAdapter.notifyItemChanged(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
                showTyping(false); speakIfReady(response)
            } catch (e: Exception) {
                showTyping(false); addAIMessage("⚠️ ${e.message}")
            }
        }
    }

    private fun speakIfReady(text: String) {
        if (ttsReady && text.isNotBlank())
            tts?.speak(text.replace(Regex("[*_`#>\\[\\]()]"), "").take(250), TextToSpeech.QUEUE_FLUSH, null, "ai")
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

    private fun showTyping(show: Boolean) = runOnUiThread {
        binding.typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                isListening = false; updateVoiceBtn(false); binding.voiceOrb.visibility = View.GONE
                if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
            }
            override fun onError(error: Int) { isListening = false; updateVoiceBtn(false); binding.voiceOrb.visibility = View.GONE }
            override fun onReadyForSpeech(p: Bundle?) { binding.voiceOrb.visibility = View.VISIBLE }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) { val s = 1f + rms.coerceIn(0f,10f)/20f; binding.voiceOrb.scaleX=s; binding.voiceOrb.scaleY=s }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { binding.voiceOrb.visibility = View.GONE }
            override fun onPartialResults(partial: Bundle?) {
                val p = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (p.isNotEmpty()) binding.messageInput.setText(p)
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
    }

    private fun setupTTS() { tts = TextToSpeech(this, this) }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts?.language = Locale("en","IN"); ttsReady = true } }

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
        } else { isListening = false; updateVoiceBtn(false); speechRecognizer?.stopListening() }
    }

    private fun updateVoiceBtn(listening: Boolean) {
        binding.voiceBtn.setIconResource(if (listening) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION), 101)
    }

    override fun onDestroy() { super.onDestroy(); tts?.shutdown(); speechRecognizer?.destroy(); adView?.destroy(); qwenEngine.unload() }
    override fun onPause()   { super.onPause();  adView?.pause() }
    override fun onResume()  { super.onResume(); adView?.resume() }
}
