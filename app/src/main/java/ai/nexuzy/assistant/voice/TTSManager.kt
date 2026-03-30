package ai.nexuzy.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * TTSManager: Text-to-Speech using Android's built-in TTS engine.
 * For better voice quality, integrate Coqui TTS via JNI or HTTP.
 * Coqui TTS Android: https://github.com/coqui-ai/TTS
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("en", "IN")
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexuzy_tts")
        }
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
