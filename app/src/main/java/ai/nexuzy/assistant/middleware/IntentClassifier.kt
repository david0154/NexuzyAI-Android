package ai.nexuzy.assistant.middleware

/**
 * Middleware: Classifies user input into actionable intents.
 * Routes to the correct tool before sending to LLM.
 */
object IntentClassifier {

    enum class Intent {
        WEATHER,
        NEWS,
        DEVICE_OPEN_APP,
        DEVICE_SET_ALARM,
        DEVICE_FLASHLIGHT,
        DEVICE_MEDIA_CONTROL,
        GENERAL
    }

    private val weatherKeywords = listOf(
        "weather", "temperature", "rain", "sunny", "forecast", "humid", "wind", "storm", "cold", "hot"
    )
    private val newsKeywords = listOf(
        "news", "headline", "latest", "today's news", "breaking", "current events"
    )
    private val openAppKeywords = listOf("open", "launch", "start app", "run")
    private val alarmKeywords = listOf("set alarm", "wake me", "alarm at", "remind me at")
    private val flashlightKeywords = listOf("flashlight", "torch", "turn on light", "turn off light")
    private val mediaKeywords = listOf("play", "pause", "stop music", "next song", "previous")

    fun classify(input: String): Intent {
        val lower = input.lowercase()
        return when {
            weatherKeywords.any { lower.contains(it) } -> Intent.WEATHER
            newsKeywords.any { lower.contains(it) } -> Intent.NEWS
            alarmKeywords.any { lower.contains(it) } -> Intent.DEVICE_SET_ALARM
            flashlightKeywords.any { lower.contains(it) } -> Intent.DEVICE_FLASHLIGHT
            mediaKeywords.any { lower.contains(it) } -> Intent.DEVICE_MEDIA_CONTROL
            openAppKeywords.any { lower.contains(it) } -> Intent.DEVICE_OPEN_APP
            else -> Intent.GENERAL
        }
    }
}
