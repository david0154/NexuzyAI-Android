package ai.david.ai.middleware

object IntentClassifier {
    enum class Intent { WEATHER, NEWS, LOCATION, ALARM, FLASHLIGHT_ON, FLASHLIGHT_OFF, MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, OPEN_APP, DEVELOPER_INFO, ABOUT_APP, MODEL_INFO, GENERAL }
    private val patterns = mapOf(
        Intent.WEATHER        to listOf("weather","temperature","rain","forecast","humid","wind","sunny","cloudy","hot outside","cold outside","mausam","barish","garmi","sardi"),
        Intent.NEWS           to listOf("news","headline","latest","breaking","what's happening","current events","khabar"),
        Intent.LOCATION       to listOf("where am i","my location","current location","which city","which place","mera location"),
        Intent.ALARM          to listOf("set alarm","wake me","alarm at","remind me at","set a reminder"),
        Intent.FLASHLIGHT_ON  to listOf("flashlight on","torch on","turn on flashlight","enable torch"),
        Intent.FLASHLIGHT_OFF to listOf("flashlight off","torch off","turn off flashlight","disable torch"),
        Intent.MEDIA_PLAY     to listOf("play music","play song","resume music"),
        Intent.MEDIA_PAUSE    to listOf("pause music","stop music","pause song"),
        Intent.MEDIA_NEXT     to listOf("next song","skip song","next track"),
        Intent.OPEN_APP       to listOf("open ","launch ","start app"),
        Intent.DEVELOPER_INFO to listOf("who made you","who created you","who built you","who developed you","your developer","your creator","who is your developer","who made david ai","developer details","about developer","who is david","nexuzy lab","your owner","who owns you"),
        Intent.ABOUT_APP      to listOf("what are you","about you","tell me about yourself","what is david ai","what can you do","your features","introduce yourself"),
        Intent.MODEL_INFO     to listOf("which model","what model","your model","which version","model version","what version","ai model")
    )
    fun classify(input: String): Intent {
        val lower = input.lowercase().trim()
        for ((intent, kws) in patterns) if (kws.any { lower.contains(it) }) return intent
        return Intent.GENERAL
    }
}
