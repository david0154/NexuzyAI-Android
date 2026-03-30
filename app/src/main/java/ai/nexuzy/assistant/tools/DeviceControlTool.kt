package ai.nexuzy.assistant.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.AlarmClock
import android.view.KeyEvent
import java.util.Calendar

/**
 * DeviceControlTool: Handles device actions via Android Intent system.
 * Uses standard Android APIs — no root required.
 */
class DeviceControlTool(private val context: Context) {

    // ── Open App ──────────────────────────────────────────────────────────────
    fun openApp(userInput: String) {
        val appName = extractAppName(userInput)
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(resolvePackageName(appName))
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun extractAppName(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("youtube") -> "youtube"
            lower.contains("whatsapp") -> "whatsapp"
            lower.contains("chrome") -> "chrome"
            lower.contains("maps") -> "maps"
            lower.contains("camera") -> "camera"
            lower.contains("settings") -> "settings"
            else -> ""
        }
    }

    private fun resolvePackageName(app: String): String = when (app) {
        "youtube" -> "com.google.android.youtube"
        "whatsapp" -> "com.whatsapp"
        "chrome" -> "com.android.chrome"
        "maps" -> "com.google.android.apps.maps"
        "settings" -> "com.android.settings"
        else -> app
    }

    // ── Set Alarm ─────────────────────────────────────────────────────────────
    fun setAlarm(userInput: String) {
        val hour = extractHour(userInput)
        val minute = extractMinute(userInput)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "NexuzyAI Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun extractHour(input: String): Int {
        val regex = Regex("(\\d{1,2}):(\\d{2})")
        return regex.find(input)?.groupValues?.get(1)?.toIntOrNull() ?: 7
    }

    private fun extractMinute(input: String): Int {
        val regex = Regex("(\\d{1,2}):(\\d{2})")
        return regex.find(input)?.groupValues?.get(2)?.toIntOrNull() ?: 0
    }

    // ── Flashlight ────────────────────────────────────────────────────────────
    fun toggleFlashlight(on: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
        cameraManager.setTorchMode(cameraId, on)
    }

    // ── Media Control ─────────────────────────────────────────────────────────
    fun controlMedia(userInput: String) {
        val lower = userInput.lowercase()
        val keyCode = when {
            lower.contains("pause") || lower.contains("stop") -> KeyEvent.KEYCODE_MEDIA_PAUSE
            lower.contains("play") -> KeyEvent.KEYCODE_MEDIA_PLAY
            lower.contains("next") -> KeyEvent.KEYCODE_MEDIA_NEXT
            lower.contains("previous") || lower.contains("prev") -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
