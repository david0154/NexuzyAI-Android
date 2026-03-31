package ai.nexuzy.assistant.middleware

import ai.nexuzy.assistant.tools.LinkReaderTool

/**
 * IntentClassifier — keyword-based intent detection.
 * Works 100% offline, no model needed.
 */
object IntentClassifier {

    enum class Intent {
        // ─ Online tools ──────────────────────────────────────────────
        WEATHER, NEWS, LOCATION, LINK_READ, WEB_SEARCH,

        // ─ Device control (always offline) ─────────────────────────
        ALARM, SET_TIMER,
        FLASHLIGHT_ON, FLASHLIGHT_OFF,
        MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
        VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN,
        OPEN_APP,
        BATTERY_STATUS,
        WIFI_INFO,
        CALL,
        SEND_SMS,
        SHARE,
        TRANSLATE,

        // ─ App info ───────────────────────────────────────────────
        DEVELOPER_INFO, ABOUT_APP, MODEL_INFO,

        GENERAL
    }

    private val patterns = mapOf(
        // ─ Online ───────────────────────────────────────────────────
        Intent.WEATHER to listOf(
            "weather", "temperature", "rain", "forecast", "humid", "wind", "sunny",
            "cloudy", "hot outside", "cold outside", "mausam", "barish", "garmi", "sardi"
        ),
        Intent.NEWS to listOf(
            "news", "headline", "latest news", "today's news", "breaking",
            "what's happening", "current events", "khabar"
        ),
        Intent.LOCATION to listOf(
            "where am i", "my location", "current location", "where are we",
            "which city", "which place", "mera location", "gps"
        ),
        Intent.WEB_SEARCH to listOf(
            "search for", "search about", "look up", "google", "find info about",
            "search online", "web search", "internet search", "search the web"
        ),

        // ─ Alarms & Timers ──────────────────────────────────────────
        Intent.ALARM to listOf(
            "set alarm", "wake me", "alarm at", "alarm for", "set a reminder",
            "remind me at", "wake me up"
        ),
        Intent.SET_TIMER to listOf(
            "set timer", "start timer", "timer for", "countdown", "count down",
            "set a timer", "timer of"
        ),

        // ─ Flashlight ───────────────────────────────────────────────
        Intent.FLASHLIGHT_ON to listOf(
            "turn on flashlight", "flashlight on", "torch on", "enable torch",
            "turn on torch", "light on"
        ),
        Intent.FLASHLIGHT_OFF to listOf(
            "turn off flashlight", "flashlight off", "torch off", "disable torch",
            "turn off torch", "light off"
        ),

        // ─ Media ───────────────────────────────────────────────────
        Intent.MEDIA_PLAY to listOf(
            "play music", "play song", "resume music", "start music", "play"
        ),
        Intent.MEDIA_PAUSE to listOf(
            "pause music", "stop music", "pause song", "pause"
        ),
        Intent.MEDIA_NEXT to listOf(
            "next song", "skip song", "next track", "skip track"
        ),
        Intent.MEDIA_PREVIOUS to listOf(
            "previous song", "prev song", "previous track", "go back song"
        ),

        // ─ Volume ──────────────────────────────────────────────────
        Intent.VOLUME_UP to listOf(
            "volume up", "increase volume", "louder", "turn up volume", "raise volume"
        ),
        Intent.VOLUME_DOWN to listOf(
            "volume down", "decrease volume", "quieter", "turn down volume", "lower volume"
        ),
        Intent.VOLUME_MUTE to listOf(
            "mute", "silent mode", "silence", "mute volume", "turn off sound", "no sound"
        ),

        // ─ Brightness ──────────────────────────────────────────────
        Intent.BRIGHTNESS_UP to listOf(
            "brightness up", "increase brightness", "brighter", "more brightness",
            "turn up brightness", "screen brighter"
        ),
        Intent.BRIGHTNESS_DOWN to listOf(
            "brightness down", "decrease brightness", "dimmer", "less brightness",
            "turn down brightness", "screen dimmer"
        ),

        // ─ Battery ─────────────────────────────────────────────────
        Intent.BATTERY_STATUS to listOf(
            "battery", "battery level", "how much battery", "charge level",
            "is charging", "battery percentage", "battery status"
        ),

        // ─ WiFi ────────────────────────────────────────────────────
        Intent.WIFI_INFO to listOf(
            "wifi", "wi-fi", "internet connection", "connected to wifi",
            "wifi name", "which wifi", "wifi status", "network name"
        ),

        // ─ Call & SMS ──────────────────────────────────────────────
        Intent.CALL to listOf(
            "call ", "phone ", "dial ", "ring ", "call number", "make a call"
        ),
        Intent.SEND_SMS to listOf(
            "send sms", "send message", "text message", "send text", "sms to",
            "message to", "send a message to"
        ),

        // ─ Share ────────────────────────────────────────────────────
        Intent.SHARE to listOf(
            "share this", "share link", "share text", "share with", "send to friend",
            "share via"
        ),

        // ─ Translate ───────────────────────────────────────────────
        Intent.TRANSLATE to listOf(
            "translate", "translate this", "translate to", "convert language",
            "in hindi", "in english", "in spanish", "in french", "in bengali",
            "say in", "how to say"
        ),

        // ─ Open App ───────────────────────────────────────────────
        Intent.OPEN_APP to listOf(
            "open ", "launch ", "start app", "go to "
        ),

        // ─ App info ───────────────────────────────────────────────
        Intent.DEVELOPER_INFO to listOf(
            "who made you", "who created you", "who built you", "who developed you",
            "your developer", "your creator", "who is your developer",
            "who made david ai", "developer details", "about developer",
            "who is david", "nexuzy lab", "your owner", "who owns you"
        ),
        Intent.ABOUT_APP to listOf(
            "what are you", "about you", "tell me about yourself",
            "what is david ai", "what can you do", "your features",
            "what is your name", "introduce yourself"
        ),
        Intent.MODEL_INFO to listOf(
            "which model", "what model are you", "your model", "ai model",
            "which version", "model version", "what version"
        )
    )

    fun classify(input: String): Intent {
        val lower = input.lowercase().trim()
        if (LinkReaderTool.extractUrl(input) != null) return Intent.LINK_READ
        for ((intent, keywords) in patterns) {
            if (keywords.any { lower.contains(it) }) return intent
        }
        return Intent.GENERAL
    }
}
