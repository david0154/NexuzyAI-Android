package ai.nexuzy.assistant.voice

import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle

/**
 * VoiceInputManager: Wraps Android's built-in SpeechRecognizer.
 * For offline support, replace with Vosk or Whisper.cpp.
 * 
 * Vosk integration: https://alphacephei.com/vosk/android
 * Whisper.cpp Android: https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android
 */
class VoiceInputManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) onResult(text)
            }

            override fun onError(error: Int) {
                onError("Speech recognition error: $error")
            }

            // Required overrides (no-op)
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
