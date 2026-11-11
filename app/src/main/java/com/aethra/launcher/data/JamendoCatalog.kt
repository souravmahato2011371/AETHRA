package com.aethra.launcher.data

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

object JamendoCatalog {
    private const val ENDPOINT = "https://api.jamendo.com/v3.0/tracks/"

    suspend fun search(clientId: String, query: String): List<OnlineTrack> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            require(clientId.isNotBlank()) { "Add your Jamendo developer client ID in AETHRA Settings." }

            val safeClientId = URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8.name())
            val safeQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
            val searchPart = if (query.isBlank()) {
                "order=popularity_week&fuzzytags=pop"
            } else {
                "search=$safeQuery&order=relevance"
            }
            val requestUrl = URL(
                "$ENDPOINT?client_id=$safeClientId&format=json&limit=30&audioformat=mp32&include=licenses&type=albumtrack%20single&$searchPart"
            )

            val connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AETHRA-Android/1.2")
            }

            try {
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(StandardCharsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

                if (status !in 200..299) {
                    throw IllegalStateException("Jamendo returned HTTP $status. Check the client ID or try again.")
                }

                val root = JSONObject(response)
                val headers = root.optJSONObject("headers")
                if (headers?.optString("status") == "error") {
                    val detail = headers.optString("error_message").ifBlank { "The catalogue rejected this request." }
                    throw IllegalStateException(detail)
                }

                val results = root.optJSONArray("results") ?: return@withContext emptyList()
                buildList {
                    for (index in 0 until results.length()) {
                        val item = results.optJSONObject(index) ?: continue
                        val stream = item.optString("audio").takeIf {
                            it.startsWith("https://")
                        } ?: continue
                        val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                        val title = item.optString("name").ifBlank { "Untitled track" }
                        val artist = item.optString("artist_name").ifBlank { "Independent artist" }
                        val artwork = item.optString("album_image")
                            .ifBlank { item.optString("image") }
                            .takeIf { it.startsWith("https://") }

                        add(
                            OnlineTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                album = item.optString("album_name").ifBlank { "Single" },
                                durationSeconds = item.optLong("duration", 0L).coerceAtLeast(0L),
                                streamUrl = stream,
                                artworkUrl = artwork,
                                shareUrl = item.optString("shareurl").takeIf { it.startsWith("https://") },
                                licenseUrl = item.optString("license_ccurl").takeIf { it.startsWith("https://") }
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
}
