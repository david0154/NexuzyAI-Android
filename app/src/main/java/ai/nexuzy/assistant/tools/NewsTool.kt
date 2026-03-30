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
 * NewsTool — accurate news headlines from NewsAPI + Google News RSS.
 *
 * Improvements:
 *  - Strips " - Source Name" suffixes Google News adds to titles
 *  - Deduplicates similar headlines before returning
 *  - Fetches top 10 items, returns best 5
 *  - Adds publication time next to each headline when available
 *  - Topic search: broader query cleaning for better results
 *  - Includes article description if available (NewsAPI)
 *  - Timeout extended to 12s for slow connections
 *
 * Google News RSS — FREE, no API key:
 *   Top India:  https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en
 *   Tech:       https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdv...
 *   Search:     https://news.google.com/rss/search?q=QUERY&hl=en-IN
 *
 * NewsAPI — optional (100 req/day free):
 *   Register: https://newsapi.org/register
 *   Add to local.properties: NEWS_API_KEY=your_key
 */
class NewsTool {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val apiKey = try { BuildConfig.NEWS_API_KEY } catch (_: Exception) { "" }

    // ── Public API ──────────────────────────────────────────────

    /** Top general headlines (India-focused) */
    suspend fun fetchTopHeadlines(country: String = "in", pageSize: Int = 5): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isNotEmpty()) fetchFromNewsAPI(country, pageSize)
            else fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
        }

    /** Alias used by ToolExecutor */
    suspend fun fetchHeadlines(): String = fetchTopHeadlines()

    /** Search headlines for a specific topic */
    suspend fun fetchTopicNews(topic: String): String = withContext(Dispatchers.IO) {
        val cleanTopic = topic
            .replace(Regex("(latest|news|about|on|for|tell me|show me|what|is|are|the)",
                RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim()
        val query = if (cleanTopic.length > 3) cleanTopic else topic
        val rssUrl = "https://news.google.com/rss/search?q=${query.replace(" ", "+")}&hl=en-IN&gl=IN&ceid=IN:en"
        fetchFromGoogleRSS(rssUrl)
    }

    // ── Private helpers ─────────────────────────────────────────

    private fun fetchFromNewsAPI(country: String, pageSize: Int): String {
        return try {
            val url = "https://newsapi.org/v2/top-headlines" +
                "?country=$country&pageSize=${pageSize + 3}&apiKey=$apiKey"
            val body = get(url)
                ?: return fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
            val articles = JSONObject(body).getJSONArray("articles")
            val results = mutableListOf<String>()
            for (i in 0 until articles.length()) {
                if (results.size >= pageSize) break
                val a = articles.getJSONObject(i)
                val title       = a.optString("title", "").cleanNewsTitle()
                val description = a.optString("description", "")
                val source      = a.optJSONObject("source")?.optString("name", "") ?: ""
                val publishedAt = a.optString("publishedAt", "").formatNewsDate()
                if (title.isBlank() || title == "[Removed]") continue
                val line = buildString {
                    append("• $title")
                    if (source.isNotBlank()) append(" [$source]")
                    if (publishedAt.isNotBlank()) append(" · $publishedAt")
                    if (description.isNotBlank() && description.length < 120)
                        append("\n  $description")
                }
                if (!isDuplicate(results, title)) results.add(line)
            }
            if (results.isNotEmpty()) results.joinToString("\n")
            else fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
        } catch (_: Exception) {
            fetchFromGoogleRSS("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en")
        }
    }

    private fun fetchFromGoogleRSS(url: String): String {
        return try {
            val body = get(url) ?: return "No news available right now."
            parseRSS(body)
        } catch (e: Exception) {
            "Could not fetch news: ${e.message}"
        }
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url)
            .addHeader("User-Agent", "NexuzyAI/2.0 Android")
            .addHeader("Accept", "application/rss+xml, application/json")
            .build()
        val resp = client.newCall(req).execute()
        return if (resp.isSuccessful) resp.body?.string() else null
    }

    /**
     * Improved RSS parser:
     * - Reads up to 10 items, filters to best 5
     * - Strips " - Source Name" Google News appends
     * - Captures <pubDate> for recency
     * - Deduplicates by title similarity
     */
    private fun parseRSS(xml: String): String {
        data class RssItem(val title: String, val pubDate: String)
        val items = mutableListOf<RssItem>()

        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false
            var inTitle = false; var inPubDate = false
            var curTitle = ""; var curDate = ""
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT && items.size < 10) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "item"    -> { inItem = true; curTitle = ""; curDate = "" }
                        "title"   -> if (inItem) inTitle = true
                        "pubDate" -> if (inItem) inPubDate = true
                    }
                    XmlPullParser.TEXT -> {
                        if (inTitle   && inItem) { curTitle += parser.text; inTitle   = false }
                        if (inPubDate && inItem) { curDate  += parser.text; inPubDate = false }
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "item" -> {
                            val clean = curTitle.cleanNewsTitle()
                            if (clean.isNotBlank() && !isDuplicate(items.map { it.title }, clean)) {
                                items.add(RssItem(clean, curDate.formatNewsDate()))
                            }
                            inItem = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}

        if (items.isEmpty()) return "No headlines found."

        return items.take(5).mapIndexed { i, item ->
            buildString {
                append("${i + 1}. ${item.title}")
                if (item.pubDate.isNotBlank()) append(" · ${item.pubDate}")
            }
        }.joinToString("\n")
    }

    // ── Extension helpers ────────────────────────────────────────

    /**
     * Removes the " - Source Name" suffix Google News appends,
     * strips HTML entities and extra whitespace.
     */
    private fun String.cleanNewsTitle(): String = this
        .replace(Regex(" - [A-Z][\\w .]+$"), "")       // strip " - The Hindu" etc.
        .replace(Regex("\\s*\\|.*$"), "")               // strip " | Extra"
        .replace("&amp;", "&").replace("&quot;", "\"")
        .replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&#39;", "'").replace("&nbsp;", " ")
        .trim()

    /**
     * Format ISO date string or RSS pubDate into readable "DD MMM" format.
     * e.g. "Mon, 30 Mar 2026 12:00:00 +0000" → "30 Mar"
     *      "2026-03-30T12:00:00Z"              → "30 Mar"
     */
    private fun String.formatNewsDate(): String {
        if (this.isBlank()) return ""
        return try {
            val formats = listOf(
                java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH),
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ENGLISH),
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", java.util.Locale.ENGLISH)
            )
            val date = formats.firstNotNullOfOrNull { fmt ->
                try { fmt.parse(this.trim()) } catch (_: Exception) { null }
            }
            if (date != null)
                java.text.SimpleDateFormat("dd MMM, h:mm a", java.util.Locale.ENGLISH).format(date)
            else ""
        } catch (_: Exception) { "" }
    }

    /** Check if a title is too similar to any existing result (basic dedup) */
    private fun isDuplicate(existing: List<String>, newTitle: String): Boolean {
        val n = newTitle.lowercase().take(40)
        return existing.any { it.lowercase().take(40) == n }
    }
}
