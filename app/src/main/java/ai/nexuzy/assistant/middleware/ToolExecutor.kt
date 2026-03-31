package ai.nexuzy.assistant.middleware

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.nexuzy.assistant.tools.DeviceControlTool
import ai.nexuzy.assistant.tools.InternetSearchTool
import ai.nexuzy.assistant.tools.LinkReaderTool
import ai.nexuzy.assistant.tools.LocationTool
import ai.nexuzy.assistant.tools.NetworkUtils
import ai.nexuzy.assistant.tools.NewsTool
import ai.nexuzy.assistant.tools.WeatherTool

/**
 * ToolExecutor — executes tools based on classified intent.
 * Returns a ToolResult with context string injected into AI prompt.
 *
 * ──────────────────────────────────────────────────────────────
 * Internet rules:
 *   WEATHER, NEWS, WEB_SEARCH, LINK_READ → require internet
 *   LOCATION → GPS offline; city name needs internet
 *   TRANSLATE → opens Google Translate app (needs internet if app not cached)
 *   Everything else → works 100% offline
 * ──────────────────────────────────────────────────────────────
 */
class ToolExecutor(private val context: Context) {

    private val weatherTool    = WeatherTool(context)
    private val newsTool       = NewsTool()
    private val locationTool   = LocationTool(context)
    private val internetSearch = InternetSearchTool()
    private val linkReader     = LinkReaderTool()
    private val deviceControl  = DeviceControlTool(context)

    private val developerInfo = """
        |I was created by David, managed by Nexuzy Lab.
        |
        |👨‍💻 Developer    : David
        |🏢 Organization : Nexuzy Lab
        |📧 Support       : nexuzylab@gmail.com
        |📧 Developer     : davidk76011@gmail.com
        |🐙 Open Source   : https://github.com/david0154/NexuzyAI-Android
        |🔒 Privacy       : Zero data collected. Everything runs on your device.
        |📄 License       : MIT License © 2025–2026
    """.trimMargin()

    suspend fun execute(
        intent: IntentClassifier.Intent,
        rawInput: String
    ): ToolResult = withContext(Dispatchers.IO) {
        val hasInternet = NetworkUtils.isInternetAvailable(context)
        try {
            when (intent) {

                // ──────── ONLINE TOOLS ───────────────────────────────────────────

                IntentClassifier.Intent.WEATHER -> {
                    if (!hasInternet) return@withContext offline("⛅ Weather requires internet.")
                    val loc     = locationTool.getLastLocation()
                    val weather = weatherTool.fetchWeather(loc?.first, loc?.second)
                    ToolResult(ToolResult.Type.WEATHER, weather, false)
                }

                IntentClassifier.Intent.NEWS -> {
                    if (!hasInternet) return@withContext offline("📵 News requires internet.")
                    ToolResult(ToolResult.Type.NEWS, newsTool.fetchHeadlines(), false)
                }

                IntentClassifier.Intent.LOCATION -> {
                    val loc  = locationTool.getLastLocation()
                    val city = if (hasInternet) locationTool.getCityName(loc?.first, loc?.second)
                               else null
                    val text = when {
                        city != null -> "You are currently in $city."
                        loc  != null -> "📍 GPS: ${loc.first}, ${loc.second} (city name needs internet)"
                        else         -> "⚠️ Location unavailable. Please grant location permission."
                    }
                    ToolResult(ToolResult.Type.LOCATION, text, true)
                }

                IntentClassifier.Intent.LINK_READ -> {
                    if (!hasInternet) return@withContext offline("🔗 Link reading requires internet.")
                    val url = LinkReaderTool.extractUrl(rawInput)
                        ?: return@withContext ToolResult(ToolResult.Type.NONE, "", false)
                    ToolResult(ToolResult.Type.INFO, linkReader.readLink(url), false)
                }

                IntentClassifier.Intent.WEB_SEARCH -> {
                    if (!hasInternet) return@withContext offline("🔍 Web search requires internet.")
                    val query = rawInput.lowercase()
                        .replace(Regex("(search for|search about|look up|google|find info about|search online|web search|internet search|search the web)"), "")
                        .trim()
                    ToolResult(ToolResult.Type.INFO, internetSearch.search(query.ifBlank { rawInput }), false)
                }

                // ──────── ALARM & TIMER (offline) ─────────────────────────────────

                IntentClassifier.Intent.ALARM ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.setAlarm(rawInput), true)

                IntentClassifier.Intent.SET_TIMER ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.setTimer(rawInput), true)

                // ──────── FLASHLIGHT (offline) ───────────────────────────────────

                IntentClassifier.Intent.FLASHLIGHT_ON  ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.toggleFlashlight(true), true)

                IntentClassifier.Intent.FLASHLIGHT_OFF ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.toggleFlashlight(false), true)

                // ──────── MEDIA (offline) ────────────────────────────────────────

                IntentClassifier.Intent.MEDIA_PLAY ->
                    ToolResult(ToolResult.Type.ACTION,
                        deviceControl.controlMedia(KeyEvent.KEYCODE_MEDIA_PLAY), true)

                IntentClassifier.Intent.MEDIA_PAUSE ->
                    ToolResult(ToolResult.Type.ACTION,
                        deviceControl.controlMedia(KeyEvent.KEYCODE_MEDIA_PAUSE), true)

                IntentClassifier.Intent.MEDIA_NEXT ->
                    ToolResult(ToolResult.Type.ACTION,
                        deviceControl.controlMedia(KeyEvent.KEYCODE_MEDIA_NEXT), true)

                IntentClassifier.Intent.MEDIA_PREVIOUS ->
                    ToolResult(ToolResult.Type.ACTION,
                        deviceControl.controlMedia(KeyEvent.KEYCODE_MEDIA_PREVIOUS), true)

                // ──────── VOLUME (offline) ────────────────────────────────────────

                IntentClassifier.Intent.VOLUME_UP ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.volumeUp(), true)

                IntentClassifier.Intent.VOLUME_DOWN ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.volumeDown(), true)

                IntentClassifier.Intent.VOLUME_MUTE ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.muteVolume(), true)

                // ──────── BRIGHTNESS (offline) ───────────────────────────────────

                IntentClassifier.Intent.BRIGHTNESS_UP ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.brightnessUp(), true)

                IntentClassifier.Intent.BRIGHTNESS_DOWN ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.brightnessDown(), true)

                // ──────── BATTERY (offline) ───────────────────────────────────────

                IntentClassifier.Intent.BATTERY_STATUS ->
                    ToolResult(ToolResult.Type.INFO, deviceControl.getBatteryInfo(), true)

                // ──────── WIFI INFO (offline) ─────────────────────────────────────

                IntentClassifier.Intent.WIFI_INFO ->
                    ToolResult(ToolResult.Type.INFO, deviceControl.getWifiInfo(), true)

                // ──────── CALL & SMS (offline intent) ────────────────────────────

                IntentClassifier.Intent.CALL ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.makeCall(rawInput), true)

                IntentClassifier.Intent.SEND_SMS ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.sendSms(rawInput), true)

                // ──────── SHARE ───────────────────────────────────────────────────

                IntentClassifier.Intent.SHARE ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.shareText(rawInput), true)

                // ──────── TRANSLATE ───────────────────────────────────────────────

                IntentClassifier.Intent.TRANSLATE ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.openTranslate(rawInput), true)

                // ──────── OPEN APP (offline) ──────────────────────────────────────

                IntentClassifier.Intent.OPEN_APP ->
                    ToolResult(ToolResult.Type.ACTION, deviceControl.openApp(rawInput), true)

                // ──────── APP INFO ─────────────────────────────────────────────────

                IntentClassifier.Intent.DEVELOPER_INFO ->
                    ToolResult(ToolResult.Type.INFO, developerInfo, true)

                IntentClassifier.Intent.ABOUT_APP -> {
                    val info = """🤖 I'm NexuzyAI — a smart AI assistant by Nexuzy Lab.
🔒 On-device AI (MLC-LLM) — zero data collected.
🌐 Online: Sarvaam AI + DuckDuckGo for accurate answers.
🎙️ Voice & text input supported.
🌦️ Weather, news, location, link reading & web search.
⏰ Alarms, timers, flashlight, volume, brightness control.
📞 Calls & SMS · 📤 Share · 🌐 Translate · 🚀 Open apps.
🔋 Battery & WiFi status.
👨‍💻 Made by David · Nexuzy Lab
🐙 https://github.com/david0154/NexuzyAI-Android"""
                    ToolResult(ToolResult.Type.INFO, info, true)
                }

                IntentClassifier.Intent.MODEL_INFO ->
                    ToolResult(ToolResult.Type.INFO, "MODEL_INFO_PLACEHOLDER", true)

                // ──────── GENERAL ──────────────────────────────────────────────────

                IntentClassifier.Intent.GENERAL -> {
                    if (hasInternet) {
                        val result = internetSearch.search(rawInput)
                        if (!result.contains("No direct answer") && !result.contains("unavailable"))
                            return@withContext ToolResult(ToolResult.Type.INFO, result, false)
                    }
                    ToolResult(ToolResult.Type.NONE, "", false)
                }
            }
        } catch (e: Exception) {
            Log.e("ToolExecutor", "Error executing intent $intent: ${e.message}")
            ToolResult(ToolResult.Type.NONE, "", false)
        }
    }

    private fun offline(msg: String) =
        ToolResult(ToolResult.Type.INFO, msg, true)
}

data class ToolResult(
    val type: Type,
    val content: String,
    val directReply: Boolean
) {
    enum class Type { WEATHER, NEWS, LOCATION, ACTION, INFO, NONE }
}
