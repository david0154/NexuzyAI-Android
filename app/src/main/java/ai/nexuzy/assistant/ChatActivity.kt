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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import ai.nexuzy.assistant.adapter.ChatAdapter
import ai.nexuzy.assistant.databinding.ActivityChatBinding
import ai.nexuzy.assistant.llm.MLCEngineWrapper
import ai.nexuzy.assistant.llm.QwenEngine
import ai.nexuzy.assistant.llm.PromptBuilder
import ai.nexuzy.assistant.middleware.IntentClassifier
import ai.nexuzy.assistant.middleware.ToolExecutor
import ai.nexuzy.assistant.model.ChatMessage
import ai.nexuzy.assistant.tools.LocationTool
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
        qwenEngine = QwenEngine(this)

        setupRecyclerView()
        setupAdMob()
        setupSTT()
        setupTTS()
        setupEngineObserver()
        requestPermissions()

        addAIMessage("👋 Hi David! I'm NexuzyAI — Qwen 3B on-device.\nTry: \"What's the weather?\" or \"Open YouTube\"")

        binding.sendBtn.setOnClickListener {
            val text = binding.messageInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
        }
        binding.voiceBtn.setOnClickListener { toggleVoice() }
        binding.clearBtn.setOnClickListener { qwenEngine.resetHistory(); messages.clear(); chatAdapter.notifyDataSetChanged() }
    }

    private fun setupEngineObserver() {
        qwenEngine.onStateChange = { state ->
            val label = when (state) {
                MLCEngineWrapper.EngineState.LOADING -> "Loading Qwen 3B..."
                MLCEngineWrapper.EngineState.READY -> "Qwen 3B • Ready"
                MLCEngineWrapper.EngineState.GENERATING -> "Generating..."
                MLCEngineWrapper.EngineState.FAILED -> "Model Failed"
                MLCEngineWrapper.EngineState.NOT_LOADED -> "Model Not Loaded"
            }
            binding.modelBadge.text = label
        }
        qwenEngine.onTokenStream = { token ->
            // Stream tokens into last message as they arrive
            if (messages.isNotEmpty() && !messages.last().isUser) {
                val last = messages.last()
                messages[messages.size - 1] = last.copy(text = last.text + token)
                chatAdapter.notifyItemChanged(messages.size - 1)
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
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
                val toolResult = toolExecutor.execute(intent, userInput)
                val locationHint = locationTool.getLocationSystemPrompt()
                val prompt = PromptBuilder.build(userInput, toolResult, locationHint)
                // Add empty AI bubble for streaming
                addAIMessage("")
                val response = qwenEngine.generate(prompt)
                // Update final response (also handles non-streaming fallback)
                if (messages.isNotEmpty() && !messages.last().isUser) {
                    messages[messages.size - 1] = messages.last().copy(text = response)
                    chatAdapter.notifyItemChanged(messages.size - 1)
                }
                showTyping(false)
                if (ttsReady && response.isNotBlank()) {
                    tts?.speak(response.take(200), TextToSpeech.QUEUE_FLUSH, null, "ai")
                }
            } catch (e: Exception) {
                showTyping(false)
                addAIMessage("⚠️ ${e.message}")
            }
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
        binding.typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ─── STT ───────────────────────────────────────────────────────────────
    private fun setupSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                isListening = false; updateVoiceBtn(false)
                if (text.isNotEmpty()) { binding.messageInput.setText(""); processInput(text) }
            }
            override fun onError(error: Int) {
                isListening = false; updateVoiceBtn(false)
                Toast.makeText(this@ChatActivity, "Voice error $error. Check mic permission.", Toast.LENGTH_SHORT).show()
            }
            override fun onReadyForSpeech(p: Bundle?) { binding.voiceOrb.visibility = View.VISIBLE }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {
                val scale = 1f + (rms.coerceIn(0f, 10f) / 20f)
                binding.voiceOrb.scaleX = scale; binding.voiceOrb.scaleY = scale
            }
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
            isListening = false; updateVoiceBtn(false)
            speechRecognizer?.stopListening()
        }
    }

    private fun updateVoiceBtn(listening: Boolean) {
        binding.voiceBtn.setIconResource(
            if (listening) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now
        )
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION), 101)
    }

    override fun onDestroy() { super.onDestroy(); tts?.shutdown(); speechRecognizer?.destroy(); adView?.destroy(); qwenEngine.unload() }
    override fun onPause()  { super.onPause();  adView?.pause() }
    override fun onResume() { super.onResume(); adView?.resume() }
}
