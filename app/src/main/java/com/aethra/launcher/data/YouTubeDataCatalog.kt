package com.aethra.launcher.data

import com.aethra.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Long,
    val streamUrl: String,
    val artworkUrl: String?,
    val shareUrl: String?,
    val licenseUrl: String?
)

/**
 * Supported YouTube Data API v3 search for music-video discovery.
 * Results open on YouTube; this API does not provide audio stream URLs.
 */
object YouTubeDataCatalog {
    private const val ENDPOINT = "https://www.googleapis.com/youtube/v3/search"

    suspend fun search(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val key = BuildConfig.YOUTUBE_API_KEY
        require(key.isNotBlank()) {
            "YouTube search needs a build key. Add YOUTUBE_API_KEY to GitHub Actions secrets."
        }
        val parameters = buildList {
            add("part=snippet")
            add("type=video")
            add("maxResults=25")
            add("videoCategoryId=10")
            if (query.isNotBlank()) add("q=" + encode(query.trim()))
            add("key=" + encode(key))
        }.joinToString("&")
        val connection = (URL("$ENDPOINT?$parameters").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AETHRA-Android/1.6")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                throw IllegalStateException(message.ifBlank { "YouTube search failed (HTTP $status)." })
            }
            val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
                    if (videoId.isBlank()) continue
                    val snippet = item.optJSONObject("snippet") ?: continue
                    val thumbnailSet = snippet.optJSONObject("thumbnails")
                    val artwork = thumbnailSet?.optJSONObject("high")?.optString("url")
                        ?.takeIf { it.startsWith("https://") }
                        ?: thumbnailSet?.optJSONObject("medium")?.optString("url")
                            ?.takeIf { it.startsWith("https://") }
                    val watchUrl = "https://www.youtube.com/watch?v=$videoId"
                    add(
                        OnlineTrack(
                            id = videoId,
                            title = snippet.optString("title").ifBlank { "Untitled video" },
                            artist = snippet.optString("channelTitle").ifBlank { "YouTube creator" },
                            album = "YouTube",
                            durationSeconds = 0L,
                            streamUrl = watchUrl,
                            artworkUrl = artwork,
                            shareUrl = watchUrl,
                            licenseUrl = null
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
