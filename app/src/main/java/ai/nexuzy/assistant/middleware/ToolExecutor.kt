package ai.nexuzy.assistant.middleware

import android.content.Context
import ai.nexuzy.assistant.tools.WeatherTool
import ai.nexuzy.assistant.tools.NewsTool
import ai.nexuzy.assistant.tools.DeviceControlTool
import ai.nexuzy.assistant.tools.LocationTool

/**
 * Middleware: Executes the correct tool based on classified intent.
 * Returns a context string to inject into the LLM prompt.
 */
class ToolExecutor(private val context: Context) {

    private val weatherTool = WeatherTool()
    private val newsTool = NewsTool()
    private val deviceTool = DeviceControlTool(context)
    private val locationTool = LocationTool(context)

    suspend fun execute(
        intent: IntentClassifier.Intent,
        userInput: String
    ): ToolResult {
        return when (intent) {
            IntentClassifier.Intent.WEATHER -> {
                val location = locationTool.getLastKnownLocation()
                val weather = weatherTool.fetchWeather(
                    lat = location?.latitude ?: 22.5726,  // Default: Kolkata
                    lon = location?.longitude ?: 88.3639
                )
                ToolResult(
                    contextString = "Current weather data: $weather",
                    systemHint = "User location = ${location?.let { "${it.latitude},${it.longitude}" } ?: "Kolkata, India"}"
                )
            }
            IntentClassifier.Intent.NEWS -> {
                val headlines = newsTool.fetchTopHeadlines()
                ToolResult(
                    contextString = "Latest news headlines: $headlines",
                    systemHint = null
                )
            }
            IntentClassifier.Intent.DEVICE_SET_ALARM -> {
                deviceTool.setAlarm(userInput)
                ToolResult(contextString = "Alarm has been set via Android system.", systemHint = null)
            }
            IntentClassifier.Intent.DEVICE_FLASHLIGHT -> {
                val on = userInput.contains("on", ignoreCase = true)
                deviceTool.toggleFlashlight(on)
                ToolResult(contextString = "Flashlight turned ${if (on) "ON" else "OFF"}.", systemHint = null)
            }
            IntentClassifier.Intent.DEVICE_MEDIA_CONTROL -> {
                deviceTool.controlMedia(userInput)
                ToolResult(contextString = "Media command executed.", systemHint = null)
            }
            IntentClassifier.Intent.DEVICE_OPEN_APP -> {
                deviceTool.openApp(userInput)
                ToolResult(contextString = "App launch attempted.", systemHint = null)
            }
            IntentClassifier.Intent.GENERAL -> {
                ToolResult(contextString = null, systemHint = null)
            }
        }
    }

    data class ToolResult(
        val contextString: String?,
        val systemHint: String?
    )
}
