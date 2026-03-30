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
import java.util.concurrent.TimeUnit

/**
 * NewsTool: Fetches headlines from NewsAPI + Google News RSS.
 *
 * GOOGLE NEWS RSS — NO API KEY NEEDED:
 * ─────────────────────────────────────────────────────────────────────
 * Google News RSS URLs (free, no auth):
 *   Top India news:  https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en
 *   Tech news:       https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGRqTVhZU0FtVnVHZ0pKVGlnQVAB
 *   Search topic:    https://news.google.com/rss/search?q=weather+Kolkata&hl=en-IN
 *
 * NEWSAPI KEY SETUP (optional, better headlines):
 * ─────────────────────────────────────────────────────────────────────
 * 1. Register free at: https://newsapi.org/register
 * 2. Add to local.properties:
 *    NEWS_API_KEY=your_key_here
 * 3. Free tier: 100 requests/day, top-headlines endpoint
 * ─────────────────────────────────────────────────────────────────────
 */
class NewsTool {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.NEWS_API_KEY

    // Primary: NewsAPI — Fallback: Google RSS
    suspend fun fetchTopHeadlines(country: String = "in", pageSize: Int = 5): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isNotEmpty()) fetchFromNewsAPI(country, pageSize)
            else fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
        }

    suspend fun fetchTopicNews(topic: String): String = withContext(Dispatchers.IO) {
        // Google News RSS topic search — NO KEY NEEDED
        val rssUrl = "https://news.google.com/rss/search?q=${topic.replace(" ", "+")}&hl=en-IN&gl=IN&ceid=IN:en"
        fetchFromGoogleRSS(rssUrl)
    }

    private fun fetchFromNewsAPI(country: String, pageSize: Int): String {
        return try {
            val url = "https://newsapi.org/v2/top-headlines?country=$country&pageSize=$pageSize&apiKey=$apiKey"
            val body = get(url) ?: return fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
            val root = JSONObject(body)
            val articles = root.getJSONArray("articles")
            (0 until minOf(articles.length(), 5)).joinToString("\n• ") { i ->
                "• " + articles.getJSONObject(i).getString("title")
            }
        } catch (_: Exception) {
            fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
        }
    }

    private fun fetchFromGoogleRSS(url: String): String {
        return try {
            val body = get(url) ?: return "No news available"
            parseRSS(body)
        } catch (e: Exception) {
            "Could not fetch news: ${e.message}"
        }
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url)
            .addHeader("User-Agent", "NexuzyAI/2.0")
            .build()
        return client.newCall(req).execute().body?.string()
    }

    private fun parseRSS(xml: String): String {
        val headlines = mutableListOf<String>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false; var inTitle = false
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < 5) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "item") inItem = true
                        if (inItem && parser.name == "title") inTitle = true
                    }
                    XmlPullParser.TEXT -> {
                        if (inTitle && inItem) { headlines.add("• " + parser.text.trim()); inTitle = false }
                    }
                    XmlPullParser.END_TAG -> { if (parser.name == "item") inItem = false }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return if (headlines.isNotEmpty()) headlines.joinToString("\n") else "No headlines found"
    }
}
