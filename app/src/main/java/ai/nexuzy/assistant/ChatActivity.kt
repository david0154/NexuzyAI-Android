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
import android.view.animation.AnimationUtils
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
import ai.nexuzy.assistant.middleware.IntentClassifier
import ai.nexuzy.assistant.middleware.ToolExecutor
import ai.nexuzy.assistant.llm.QwenEngine
import ai.nexuzy.assistant.llm.PromptBuilder
import ai.nexuzy.assistant.model.ChatMessage
import ai.nexuzy.assistant.tools.LocationTool
import java.util.Locale

class ChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ai.nexuzy.assistant.databinding.ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var toolExecutor: ToolExecutor
    private lateinit var locationTool: LocationTool
    private lateinit var qwenEngine: QwenEngine

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // STT
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // AdMob banner
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ai.nexuzy.assistant.databinding.ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupAdMob()
        setupVoice()

        toolExecutor = ToolExecutor(this)
        locationTool = LocationTool(this)
        qwenEngine = QwenEngine(this)

        requestPermissions()
        addWelcomeMessage()

        binding.sendBtn.setOnClickListener {
            val text = binding.messageInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                binding.messageInput.setText("")
                processUserInput(text)
            }
        }

        binding.voiceBtn.setOnClickListener { toggleVoice() }
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun addWelcomeMessage() {
        addAIMessage("👋 Hi David! I'm NexuzyAI powered by Qwen 3B.\nAsk me about weather, news, or say 'open YouTube', 'set alarm 7:00'!")
    }

    // ─── AdMob ────────────────────────────────────────────────────────────────
    private fun setupAdMob() {
        adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }
        binding.adContainer.addView(adView)
        adView?.loadAd(AdRequest.Builder().build())
    }

    // ─── Main pipeline ────────────────────────────────────────────────────────
    fun processUserInput(userInput: String) {
        addUserMessage(userInput)
        showTypingIndicator(true)

        lifecycleScope.launch {
            try {
                val intent = IntentClassifier.classify(userInput)
                val toolResult = toolExecutor.execute(intent, userInput)
                val locationHint = locationTool.getLocationSystemPrompt()
                val prompt = PromptBuilder.build(
                    userMessage = userInput,
                    toolResult = toolResult,
                    locationHint = locationHint
                )
                val response = qwenEngine.generate(prompt)
                showTypingIndicator(false)
                addAIMessage(response)
                if (ttsReady) tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "ai_reply")
            } catch (e: Exception) {
                showTypingIndicator(false)
                addAIMessage("⚠️ Error: ${e.message}")
            }
        }
    }

    // ─── Chat UI helpers ──────────────────────────────────────────────────────
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

    private fun showTypingIndicator(show: Boolean) {
        binding.typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ─── Voice STT ────────────────────────────────────────────────────────────
    private fun setupVoice() {
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    isListening = false
                    updateVoiceBtnState(false)
                    processUserInput(text)
                }
            }
            override fun onError(error: Int) {
                isListening = false
                updateVoiceBtnState(false)
                Toast.makeText(this@ChatActivity, "Voice error: $error", Toast.LENGTH_SHORT).show()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                // Animate pulse based on volume
                binding.voiceOrb.scaleX = 1f + (rmsdB / 80f)
                binding.voiceOrb.scaleY = 1f + (rmsdB / 80f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                binding.messageInput.setText(partial)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun toggleVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 102)
            return
        }
        if (!isListening) {
            isListening = true
            updateVoiceBtnState(true)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } else {
            isListening = false
            updateVoiceBtnState(false)
            speechRecognizer?.stopListening()
        }
    }

    private fun updateVoiceBtnState(listening: Boolean) {
        binding.voiceBtn.setIconResource(
            if (listening) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )
        binding.voiceOrb.visibility = if (listening) View.VISIBLE else View.GONE
        if (!listening) {
            binding.voiceOrb.scaleX = 1f
            binding.voiceOrb.scaleY = 1f
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "IN")
            ttsReady = true
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION),
            101
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        speechRecognizer?.destroy()
        adView?.destroy()
    }

    override fun onPause() { super.onPause(); adView?.pause() }
    override fun onResume() { super.onResume(); adView?.resume() }
}
