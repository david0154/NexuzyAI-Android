package ai.david.ai.middleware

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.david.ai.tools.LocationTool
import ai.david.ai.tools.NewsTool
import ai.david.ai.tools.WeatherTool

class ToolExecutor(private val context: Context) {
    private val weatherTool  = WeatherTool(context)
    private val newsTool     = NewsTool()
    private val locationTool = LocationTool(context)

    private val devInfo = """
        |👨‍💻 Developer    : David
        |🏞️ Organization  : Nexuzy Lab
        |📧 Support       : nexuzylab@gmail.com
        |📧 Developer     : davidk76011@gmail.com
        |🐙 Open Source   : https://github.com/david0154/NexuzyAI-Android
        |🔒 Privacy       : Zero data collected. 100% on-device.
        |📄 License       : MIT © 2025–2026
    """.trimMargin()

    suspend fun execute(intent: IntentClassifier.Intent, raw: String): ToolResult = withContext(Dispatchers.IO) {
        try {
            when (intent) {
                IntentClassifier.Intent.WEATHER -> {
                    val loc = locationTool.getLastLocation()
                    ToolResult(ToolResult.Type.WEATHER, weatherTool.fetchWeather(loc?.first, loc?.second), false)
                }
                IntentClassifier.Intent.NEWS -> ToolResult(ToolResult.Type.NEWS, newsTool.fetchHeadlines(), false)
                IntentClassifier.Intent.LOCATION -> {
                    val loc  = locationTool.getLastLocation()
                    val city = locationTool.getCityName(loc?.first, loc?.second)
                    ToolResult(ToolResult.Type.LOCATION, if (city!=null) "You are in $city." else "Location unavailable.", true)
                }
                IntentClassifier.Intent.ALARM -> {
                    val hour = extractHour(raw)
                    if (hour != null) {
                        context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour.first)
                            putExtra(AlarmClock.EXTRA_MINUTES, hour.second)
                            putExtra(AlarmClock.EXTRA_MESSAGE, "David AI")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        ToolResult(ToolResult.Type.ACTION, "⏰ Alarm set for ${fmt(hour.first,hour.second)}", true)
                    } else ToolResult(ToolResult.Type.ACTION, "Say a time, e.g. \"Set alarm at 7 AM\"", true)
                }
                IntentClassifier.Intent.FLASHLIGHT_ON  -> ToolResult(ToolResult.Type.ACTION, "FLASHLIGHT_ON", true)
                IntentClassifier.Intent.FLASHLIGHT_OFF -> ToolResult(ToolResult.Type.ACTION, "FLASHLIGHT_OFF", true)
                IntentClassifier.Intent.MEDIA_PLAY  -> { dispatch(KeyEvent.KEYCODE_MEDIA_PLAY);  ToolResult(ToolResult.Type.ACTION, "▶️ Playing", true) }
                IntentClassifier.Intent.MEDIA_PAUSE -> { dispatch(KeyEvent.KEYCODE_MEDIA_PAUSE); ToolResult(ToolResult.Type.ACTION, "⏸️ Paused", true) }
                IntentClassifier.Intent.MEDIA_NEXT  -> { dispatch(KeyEvent.KEYCODE_MEDIA_NEXT);  ToolResult(ToolResult.Type.ACTION, "⏭️ Next track", true) }
                IntentClassifier.Intent.OPEN_APP -> {
                    val name = raw.lowercase().replace("open","").replace("launch","").replace("start app","").trim()
                    val pkg  = apps[name]
                    val i    = if (pkg!=null) context.packageManager.getLaunchIntentForPackage(pkg) else null
                    if (i!=null) { i.flags=Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(i); ToolResult(ToolResult.Type.ACTION, "🚀 Opening $name", true) }
                    else ToolResult(ToolResult.Type.ACTION, "App not found: $name", true)
                }
                IntentClassifier.Intent.DEVELOPER_INFO -> ToolResult(ToolResult.Type.INFO, devInfo, true)
                IntentClassifier.Intent.ABOUT_APP -> ToolResult(ToolResult.Type.INFO,
                    "I'm David AI — a private on-device AI assistant.\n🔒 Zero data collected. Runs offline.\n🎙️ Voice + text input.\n🌦️ Weather, news, location.\n⏰ Alarms, flashlight, media, apps.\n👨‍💻 Made by David · Nexuzy Lab", true)
                IntentClassifier.Intent.MODEL_INFO -> ToolResult(ToolResult.Type.INFO, "MODEL_INFO_PLACEHOLDER", true)
                IntentClassifier.Intent.GENERAL    -> ToolResult(ToolResult.Type.NONE, "", false)
            }
        } catch(e: Exception) { Log.e("ToolExecutor",e.message?:""); ToolResult(ToolResult.Type.NONE,"",false) }
    }

    private fun dispatch(keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }
    private fun extractHour(input: String): Pair<Int,Int>? {
        val m = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(input.lowercase()) ?: return null
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: 0
        val ap = m.groupValues[3].lowercase()
        if (ap=="pm" && h<12) h+=12; if (ap=="am" && h==12) h=0
        return Pair(h,min)
    }
    private fun fmt(h: Int, m: Int): String { val ap=if(h<12)"AM" else "PM"; val h12=if(h%12==0)12 else h%12; return String.format("%d:%02d %s",h12,m,ap) }
    private val apps = mapOf("youtube" to "com.google.android.youtube","whatsapp" to "com.whatsapp","instagram" to "com.instagram.android","chrome" to "com.android.chrome","camera" to "com.android.camera2","maps" to "com.google.android.apps.maps","spotify" to "com.spotify.music","twitter" to "com.twitter.android","facebook" to "com.facebook.katana","gmail" to "com.google.android.gm","settings" to "com.android.settings")
}

data class ToolResult(val type: Type, val content: String, val directReply: Boolean) {
    enum class Type { WEATHER, NEWS, LOCATION, ACTION, INFO, NONE }
}
