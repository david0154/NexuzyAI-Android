package ai.nexuzy.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ai.nexuzy.assistant.middleware.IntentClassifier
import ai.nexuzy.assistant.middleware.ToolExecutor
import ai.nexuzy.assistant.llm.PromptBuilder
import ai.nexuzy.assistant.tools.LocationTool
import ai.nexuzy.assistant.voice.VoiceInputManager
import ai.nexuzy.assistant.voice.TTSManager

/**
 * ChatActivity: Main chat screen.
 * Flow:
 *   User input (text/voice)
 *     → IntentClassifier.classify()
 *     → ToolExecutor.execute()
 *     → PromptBuilder.build()  ← injects tool result + location
 *     → MLCEngine.generate()   ← on-device LLM
 *     → Display + TTS speak
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var toolExecutor: ToolExecutor
    private lateinit var locationTool: LocationTool
    private lateinit var voiceInput: VoiceInputManager
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        toolExecutor = ToolExecutor(this)
        locationTool = LocationTool(this)
        ttsManager = TTSManager(this)

        voiceInput = VoiceInputManager(
            context = this,
            onResult = { spokenText -> processUserInput(spokenText) },
            onError = { error -> /* show error snackbar */ }
        )

        requestPermissions()
    }

    fun processUserInput(userInput: String) {
        lifecycleScope.launch {
            // Step 1: Classify intent
            val intent = IntentClassifier.classify(userInput)

            // Step 2: Execute tool
            val toolResult = toolExecutor.execute(intent, userInput)

            // Step 3: Get location context
            val locationHint = locationTool.getLocationSystemPrompt()

            // Step 4: Build full prompt
            val prompt = PromptBuilder.build(
                userMessage = userInput,
                toolResult = toolResult,
                locationHint = locationHint
            )

            // Step 5: Send to MLC-LLM engine (wire up your MLCEngine here)
            // val response = MLCEngine.generate(prompt)
            // displayMessage(response)
            // ttsManager.speak(response)

            // Placeholder until MLC model weights are loaded:
            val placeholder = "[LLM response will appear here. Connect MLCEngine with model weights.]"
            // displayMessage(placeholder)
        }
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(this, perms, 101)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        voiceInput.stopListening()
    }
}
