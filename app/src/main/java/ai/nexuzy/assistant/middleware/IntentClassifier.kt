package ai.nexuzy.assistant.middleware

/**
 * IntentClassifier — pure keyword-based intent detection.
 * Works 100% offline, no model needed.
 */
object IntentClassifier {

    enum class Intent {
        WEATHER, NEWS, LOCATION, ALARM, FLASHLIGHT_ON, FLASHLIGHT_OFF,
        MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, OPEN_APP,
        DEVELOPER_INFO, ABOUT_APP, MODEL_INFO,
        GENERAL
    }

    private val patterns = mapOf(
        Intent.WEATHER to listOf(
            "weather", "temperature", "rain", "forecast", "humid", "wind", "sunny",
            "cloudy", "hot", "cold", "mausam", "barish", "garmi", "sardi"
        ),
        Intent.NEWS to listOf(
            "news", "headline", "latest", "today\'s news", "breaking",
            "what\'s happening", "current events", "khabar"
        ),
        Intent.LOCATION to listOf(
            "where am i", "my location", "current location", "where are we",
            "which city", "which place", "mera location"
        ),
        Intent.ALARM to listOf(
            "set alarm", "wake me", "alarm at", "remind me", "set a reminder"
        ),
        Intent.FLASHLIGHT_ON to listOf(
            "turn on flashlight", "flashlight on", "torch on", "enable torch"
        ),
        Intent.FLASHLIGHT_OFF to listOf(
            "turn off flashlight", "flashlight off", "torch off", "disable torch"
        ),
        Intent.MEDIA_PLAY to listOf(
            "play music", "play song", "resume music", "start music"
        ),
        Intent.MEDIA_PAUSE to listOf(
            "pause music", "stop music", "pause song"
        ),
        Intent.MEDIA_NEXT to listOf(
            "next song", "skip song", "next track"
        ),
        Intent.OPEN_APP to listOf(
            "open ", "launch ", "start app"
        ),
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
        for ((intent, keywords) in patterns) {
            if (keywords.any { lower.contains(it) }) return intent
        }
        return Intent.GENERAL
    }
}
