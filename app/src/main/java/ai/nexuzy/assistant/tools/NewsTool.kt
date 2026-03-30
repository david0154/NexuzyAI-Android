package ai.nexuzy.assistant.tools

import ai.nexuzy.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.io.StringReader

/**
 * NewsTool: Fetches news from NewsAPI and Google News RSS.
 * Primary: NewsAPI (requires API key in local.properties NEWS_API_KEY)
 * Fallback: Google News RSS (no key needed)
 */
class NewsTool {

    private val client = OkHttpClient()
    private val apiKey = BuildConfig.NEWS_API_KEY

    suspend fun fetchTopHeadlines(country: String = "in", pageSize: Int = 5): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isNotEmpty()) {
                fetchFromNewsAPI(country, pageSize)
            } else {
                fetchFromGoogleRSS()
            }
        }

    private fun fetchFromNewsAPI(country: String, pageSize: Int): String {
        return try {
            val url = "https://newsapi.org/v2/top-headlines?country=$country&pageSize=$pageSize&apiKey=$apiKey"
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().body?.string() ?: return "No news available"
            parseNewsAPI(body)
        } catch (e: Exception) {
            fetchFromGoogleRSS()
        }
    }

    private fun parseNewsAPI(json: String): String {
        val root = JSONObject(json)
        val articles = root.getJSONArray("articles")
        val headlines = (0 until minOf(articles.length(), 5)).map { i ->
            articles.getJSONObject(i).getString("title")
        }
        return headlines.joinToString(" | ")
    }

    private fun fetchFromGoogleRSS(): String {
        return try {
            val url = "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().body?.string() ?: return "No news available"
            parseRSS(body)
        } catch (e: Exception) {
            "Could not fetch news: ${e.message}"
        }
    }

    private fun parseRSS(xml: String): String {
        val headlines = mutableListOf<String>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false
            var inTitle = false
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < 5) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "item") inItem = true
                        if (inItem && parser.name == "title") inTitle = true
                    }
                    XmlPullParser.TEXT -> {
                        if (inTitle && inItem) {
                            headlines.add(parser.text.trim())
                            inTitle = false
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") inItem = false
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return if (headlines.isNotEmpty()) headlines.joinToString(" | ") else "No headlines found"
    }
}
