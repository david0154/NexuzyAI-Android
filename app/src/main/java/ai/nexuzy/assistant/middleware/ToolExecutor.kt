package ai.nexuzy.assistant.middleware

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.nexuzy.assistant.tools.InternetSearchTool
import ai.nexuzy.assistant.tools.LinkReaderTool
import ai.nexuzy.assistant.tools.LocationTool
import ai.nexuzy.assistant.tools.NetworkUtils
import ai.nexuzy.assistant.tools.NewsTool
import ai.nexuzy.assistant.tools.WeatherTool

/**
 * ToolExecutor — executes tools based on classified intent.
 * Returns a context string injected into the AI prompt.
 *
 * Internet-gated rules:
 *   • WEATHER, NEWS → require internet; return offline error if no connection
 *   • LOCATION      → GPS works offline; reverse geocode needs internet (Geocoder)
 *   • LINK_READ     → requires internet
 *   • WEB_SEARCH    → requires internet (DuckDuckGo)
 *   • GENERAL       → offline AI; if unknown + internet on → DuckDuckGo fallback
 */
class ToolExecutor(private val context: Context) {

    private val weatherTool      = WeatherTool(context)
    private val newsTool         = NewsTool()
    private val locationTool     = LocationTool(context)
    private val internetSearch   = InternetSearchTool()
    private val linkReader       = LinkReaderTool()

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
        val hasInternet = NetworkUtils.isInternetAvailable(context)
        try {
            when (intent) {

                IntentClassifier.Intent.WEATHER -> {
                    if (!hasInternet)
                        return@withContext ToolResult(
                            ToolResult.Type.WEATHER,
                            "\u26a1 No internet connection. Weather data requires internet access.",
                            directReply = true
                        )
                    val loc = locationTool.getLastLocation()
                    val weather = weatherTool.fetchWeather(loc?.first, loc?.second)
                    ToolResult(type = ToolResult.Type.WEATHER, content = weather, directReply = false)
                }

                IntentClassifier.Intent.NEWS -> {
                    if (!hasInternet)
                        return@withContext ToolResult(
                            ToolResult.Type.NEWS,
                            "\uD83D\uDCF5 No internet connection. News requires internet access.",
                            directReply = true
                        )
                    val headlines = newsTool.fetchHeadlines()
                    ToolResult(type = ToolResult.Type.NEWS, content = headlines, directReply = false)
                }

                IntentClassifier.Intent.LOCATION -> {
                    // GPS works offline; city name from Geocoder needs internet
                    val loc  = locationTool.getLastLocation()
                    val city = if (hasInternet) locationTool.getCityName(loc?.first, loc?.second)
                               else null
                    val text = when {
                        city != null -> "You are currently in $city."
                        loc != null  -> "GPS location: ${loc.first}, ${loc.second} (city name needs internet)."
                        else         -> "Unable to determine your location. Please grant location permission."
                    }
                    ToolResult(type = ToolResult.Type.LOCATION, content = text, directReply = true)
                }

                IntentClassifier.Intent.LINK_READ -> {
                    if (!hasInternet)
                        return@withContext ToolResult(
                            ToolResult.Type.INFO,
                            "\uD83D\uDD17 No internet connection. Cannot read the link without internet.",
                            directReply = true
                        )
                    val url = LinkReaderTool.extractUrl(rawInput)
                        ?: return@withContext ToolResult(ToolResult.Type.NONE, "", false)
                    val content = linkReader.readLink(url)
                    ToolResult(type = ToolResult.Type.INFO, content = content, directReply = false)
                }

                IntentClassifier.Intent.WEB_SEARCH -> {
                    if (!hasInternet)
                        return@withContext ToolResult(
                            ToolResult.Type.INFO,
                            "\uD83D\uDD0D No internet connection. Web search requires internet access.",
                            directReply = true
                        )
                    val query = rawInput
                        .lowercase()
                        .replace(Regex("(search for|search about|look up|google|find info about|search online|web search|internet search)"), "")
                        .trim()
                    val result = internetSearch.search(query.ifBlank { rawInput })
                    ToolResult(type = ToolResult.Type.INFO, content = result, directReply = false)
                }

                IntentClassifier.Intent.ALARM -> {
                    val hour = extractHour(rawInput)
                    if (hour != null) {
                        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_HOUR, hour.first)
                            putExtra(AlarmClock.EXTRA_MINUTES, hour.second)
                            putExtra(AlarmClock.EXTRA_MESSAGE, "NexuzyAI Alarm")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(alarmIntent)
                        ToolResult(ToolResult.Type.ACTION, "\u23f0 Alarm set for ${formatTime(hour.first, hour.second)}", true)
                    } else {
                        ToolResult(ToolResult.Type.ACTION, "Please say a time, e.g. \"Set alarm at 7 AM\"", true)
                    }
                }

                IntentClassifier.Intent.FLASHLIGHT_ON  ->
                    ToolResult(ToolResult.Type.ACTION, "FLASHLIGHT_ON",  true)

                IntentClassifier.Intent.FLASHLIGHT_OFF ->
                    ToolResult(ToolResult.Type.ACTION, "FLASHLIGHT_OFF", true)

                IntentClassifier.Intent.MEDIA_PLAY -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY))
                    ToolResult(ToolResult.Type.ACTION, "\u25b6\ufe0f Playing music", true)
                }

                IntentClassifier.Intent.MEDIA_PAUSE -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
                    ToolResult(ToolResult.Type.ACTION, "\u23f8\ufe0f Music paused", true)
                }

                IntentClassifier.Intent.MEDIA_NEXT -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                    ToolResult(ToolResult.Type.ACTION, "\u23ed\ufe0f Next track", true)
                }

                IntentClassifier.Intent.OPEN_APP -> {
                    val appName = rawInput.lowercase()
                        .replace("open", "").replace("launch", "").replace("start app", "").trim()
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(resolvePackageName(appName) ?: "")
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        ToolResult(ToolResult.Type.ACTION, "\uD83D\uDE80 Opening $appName", true)
                    } else {
                        ToolResult(ToolResult.Type.ACTION, "Could not find app: $appName", true)
                    }
                }

                IntentClassifier.Intent.DEVELOPER_INFO ->
                    ToolResult(ToolResult.Type.INFO, developerInfo, true)

                IntentClassifier.Intent.ABOUT_APP -> {
                    val info = """I'm NexuzyAI \u2014 a smart AI assistant by Nexuzy Lab.
\uD83D\uDD12 On-device AI (MLC-LLM) — zero data collected.
\uD83C\uDF10 When internet is on: uses Sarvaam AI + DuckDuckGo for accuracy.
\uD83C\uDF99\ufe0f Voice & text input supported.
\uD83C\uDF26\ufe0f Weather, news, location, link reading & web search.
\u23F0 Set alarms and control your device.
\uD83D\uDD17 Share any link — I'll read and summarize it for you.
\uD83D\uDC68\u200D\uD83D\uDCBB Made by David \u00b7 Nexuzy Lab
\uD83D\uDC19 https://github.com/david0154/NexuzyAI-Android"""
                    ToolResult(ToolResult.Type.INFO, info, true)
                }

                IntentClassifier.Intent.MODEL_INFO ->
                    ToolResult(ToolResult.Type.INFO, "MODEL_INFO_PLACEHOLDER", true)

                IntentClassifier.Intent.GENERAL -> {
                    // If internet is on, provide a DuckDuckGo search context for GENERAL queries
                    // so the AI can give accurate answers even if it doesn't know something
                    if (hasInternet) {
                        val searchResult = internetSearch.search(rawInput)
                        // Only inject if meaningful result found
                        if (!searchResult.contains("No direct answer") && !searchResult.contains("unavailable")) {
                            return@withContext ToolResult(
                                ToolResult.Type.INFO,
                                searchResult,
                                directReply = false  // inject into AI prompt for better answer
                            )
                        }
                    }
                    ToolResult(type = ToolResult.Type.NONE, content = "", directReply = false)
                }
            }
        } catch (e: Exception) {
            Log.e("ToolExecutor", "Error: ${e.message}")
            ToolResult(type = ToolResult.Type.NONE, content = "", directReply = false)
        }
    }

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
    val directReply: Boolean
) {
    enum class Type { WEATHER, NEWS, LOCATION, ACTION, INFO, NONE }
}
