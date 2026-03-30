package ai.nexuzy.assistant.middleware

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.nexuzy.assistant.tools.LocationTool
import ai.nexuzy.assistant.tools.NewsTool
import ai.nexuzy.assistant.tools.WeatherTool

/**
 * ToolExecutor — executes tools based on classified intent.
 * Returns a context string injected into the AI prompt.
 * Works for both text and voice input (IntentClassifier handles both).
 */
class ToolExecutor(private val context: Context) {

    private val weatherTool  = WeatherTool(context)
    private val newsTool     = NewsTool()
    private val locationTool = LocationTool(context)

    // Developer/app info — returned directly, no AI needed
    private val developerInfo = """
        |I was created by David, managed by Nexuzy Lab.
        |
        |\uD83D\uDC68\u200D\uD83D\uDCBB Developer : David
        |\uD83C\uDFE2 Organization : Nexuzy Lab
        |\uD83D\uDCE7 Support      : nexuzylab@gmail.com
        |\uD83D\uDCE7 Developer    : davidk76011@gmail.com
        |\uD83D\uDC19 Open Source  : https://github.com/david0154/NexuzyAI-Android
        |\uD83D\uDD12 Privacy      : I collect zero data. Everything runs on your device.
        |\uD83D\uDCC4 License      : MIT License \u00a9 2025\u20132026
    """.trimMargin()

    suspend fun execute(
        intent: IntentClassifier.Intent,
        rawInput: String
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            when (intent) {

                IntentClassifier.Intent.WEATHER -> {
                    val loc = locationTool.getLastLocation()
                    val weather = weatherTool.fetchWeather(loc?.first, loc?.second)
                    ToolResult(type = ToolResult.Type.WEATHER, content = weather, directReply = false)
                }

                IntentClassifier.Intent.NEWS -> {
                    val headlines = newsTool.fetchHeadlines()
                    ToolResult(type = ToolResult.Type.NEWS, content = headlines, directReply = false)
                }

                IntentClassifier.Intent.LOCATION -> {
                    val loc  = locationTool.getLastLocation()
                    val city = locationTool.getCityName(loc?.first, loc?.second)
                    val text = if (city != null) "You are currently in $city." else "Unable to determine your location."
                    ToolResult(type = ToolResult.Type.LOCATION, content = text, directReply = true)
                }

                IntentClassifier.Intent.ALARM -> {
                    val hour = extractHour(rawInput)
                    if (hour != null) {
                        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour.first)
                            putExtra(AlarmClock.EXTRA_MINUTES, hour.second)
                            putExtra(AlarmClock.EXTRA_MESSAGE, "David AI Alarm")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(alarmIntent)
                        ToolResult(type = ToolResult.Type.ACTION, content = "\u23f0 Alarm set for ${formatTime(hour.first, hour.second)}", directReply = true)
                    } else {
                        ToolResult(type = ToolResult.Type.ACTION, content = "Please say a time, e.g. \"Set alarm at 7 AM\"", directReply = true)
                    }
                }

                IntentClassifier.Intent.FLASHLIGHT_ON -> {
                    ToolResult(type = ToolResult.Type.ACTION, content = "FLASHLIGHT_ON", directReply = true)
                }

                IntentClassifier.Intent.FLASHLIGHT_OFF -> {
                    ToolResult(type = ToolResult.Type.ACTION, content = "FLASHLIGHT_OFF", directReply = true)
                }

                IntentClassifier.Intent.MEDIA_PLAY -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY))
                    ToolResult(type = ToolResult.Type.ACTION, content = "\u25b6\ufe0f Playing music", directReply = true)
                }

                IntentClassifier.Intent.MEDIA_PAUSE -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
                    ToolResult(type = ToolResult.Type.ACTION, content = "\u23f8\ufe0f Music paused", directReply = true)
                }

                IntentClassifier.Intent.MEDIA_NEXT -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                    ToolResult(type = ToolResult.Type.ACTION, content = "\u23ed\ufe0f Next track", directReply = true)
                }

                IntentClassifier.Intent.OPEN_APP -> {
                    val appName = rawInput.lowercase()
                        .replace("open", "").replace("launch", "").replace("start app", "").trim()
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(
                        resolvePackageName(appName) ?: ""
                    )
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        ToolResult(type = ToolResult.Type.ACTION, content = "\uD83D\uDE80 Opening $appName", directReply = true)
                    } else {
                        ToolResult(type = ToolResult.Type.ACTION, content = "Could not find app: $appName", directReply = true)
                    }
                }

                IntentClassifier.Intent.DEVELOPER_INFO -> {
                    ToolResult(type = ToolResult.Type.INFO, content = developerInfo, directReply = true)
                }

                IntentClassifier.Intent.ABOUT_APP -> {
                    val info = """I'm David AI \u2014 a private, on-device AI assistant.
\uD83D\uDD12 I run entirely on your device. Zero data collected.
\uD83C\uDF99\ufe0f I understand both voice and text.
\uD83C\uDF26\ufe0f I can tell you the weather, latest news, your location.
\u23F0 I can set alarms and control your device.
\uD83D\uDC68\u200D\uD83D\uDCBB Made by David \u00b7 Nexuzy Lab
\uD83D\uDC19 https://github.com/david0154/NexuzyAI-Android"""
                    ToolResult(type = ToolResult.Type.INFO, content = info, directReply = true)
                }

                IntentClassifier.Intent.MODEL_INFO -> {
                    // Filled by ChatActivity with actual model info
                    ToolResult(type = ToolResult.Type.INFO, content = "MODEL_INFO_PLACEHOLDER", directReply = true)
                }

                IntentClassifier.Intent.GENERAL -> {
                    ToolResult(type = ToolResult.Type.NONE, content = "", directReply = false)
                }
            }
        } catch (e: Exception) {
            Log.e("ToolExecutor", "Error: ${e.message}")
            ToolResult(type = ToolResult.Type.NONE, content = "", directReply = false)
        }
    }

    // --- helpers ---

    private fun extractHour(input: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = regex.find(input.lowercase()) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val min  = match.groupValues[2].toIntOrNull() ?: 0
        val ampm = match.groupValues[3].lowercase()
        if (ampm == "pm" && hour < 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        return Pair(hour, min)
    }

    private fun formatTime(h: Int, m: Int): String {
        val ampm = if (h < 12) "AM" else "PM"
        val h12  = if (h % 12 == 0) 12 else h % 12
        return String.format("%d:%02d %s", h12, m, ampm)
    }

    private fun resolvePackageName(appName: String): String? = mapOf(
        "youtube"   to "com.google.android.youtube",
        "whatsapp"  to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome"    to "com.android.chrome",
        "camera"    to "com.android.camera2",
        "maps"      to "com.google.android.apps.maps",
        "spotify"   to "com.spotify.music",
        "twitter"   to "com.twitter.android",
        "facebook"  to "com.facebook.katana",
        "gmail"     to "com.google.android.gm",
        "settings"  to "com.android.settings"
    )[appName]
}

data class ToolResult(
    val type: Type,
    val content: String,
    val directReply: Boolean  // true = show directly without AI, false = inject into AI prompt
) {
    enum class Type { WEATHER, NEWS, LOCATION, ACTION, INFO, NONE }
}
