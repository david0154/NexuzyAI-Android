package ai.nexuzy.assistant.tools

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * LinkReaderTool — fetches and extracts readable text from any URL.
 * Uses OkHttp to download page HTML, then strips tags to extract content.
 * Result is injected into AI prompt so the model can answer questions about the link.
 *
 * Usage: User says "summarize https://example.com" or pastes a URL.
 */
class LinkReaderTool {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        // Regex to detect URLs in user input
        val URL_PATTERN: Pattern = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"
        )

        fun extractUrl(input: String): String? {
            val matcher = URL_PATTERN.matcher(input)
            return if (matcher.find()) matcher.group(1) else null
        }
    }

    /**
     * Fetches the URL and returns a clean text summary (max 2000 chars).
     * Returns an error string if fetch fails.
     */
    fun readLink(url: String): String {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; NexuzyAI) AppleWebKit/537.36")
                .build()
            val html = client.newCall(req).execute().use { it.body?.string() ?: "" }
            val text = extractText(html)
            if (text.isBlank()) "[Could not extract readable content from: $url]"
            else "[Link content from $url]\n$text"
        } catch (e: Exception) {
            Log.e("LinkReaderTool", "Read error: ${e.message}")
            "[Could not access link: ${e.message}]"
        }
    }

    /** Strip HTML tags and collapse whitespace, return first 2000 chars */
    private fun extractText(html: String): String {
        // Remove script/style blocks
        var text = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")            // strip all tags
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("\\s+"), " ")               // collapse whitespace
            .trim()
        return text.take(2000)
    }
}
