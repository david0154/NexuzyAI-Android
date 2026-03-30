package ai.nexuzy.assistant.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * WeatherTool: Fetches real-time weather from Open-Meteo (free, no API key needed).
 * https://open-meteo.com/
 */
class WeatherTool {

    private val client = OkHttpClient()

    suspend fun fetchWeather(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m," +
                "precipitation,weather_code,apparent_temperature" +
                "&timezone=Asia%2FKolkata"

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "Weather data unavailable"
            parseWeather(body)
        } catch (e: Exception) {
            "Could not fetch weather: ${e.message}"
        }
    }

    private fun parseWeather(json: String): String {
        return try {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val feelsLike = current.getDouble("apparent_temperature")
            val humidity = current.getInt("relative_humidity_2m")
            val wind = current.getDouble("wind_speed_10m")
            val rain = current.getDouble("precipitation")
            val code = current.getInt("weather_code")
            val condition = weatherCodeToDescription(code)

            "Temperature: ${temp}°C (feels like ${feelsLike}°C), " +
                    "Condition: $condition, Humidity: ${humidity}%, " +
                    "Wind: ${wind} km/h, Precipitation: ${rain}mm"
        } catch (e: Exception) {
            "Weather parse error: ${e.message}"
        }
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Rain showers"
        95 -> "Thunderstorm"
        else -> "Unknown ($code)"
    }
}
