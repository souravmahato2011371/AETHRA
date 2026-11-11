package com.aethra.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Unauthenticated discovery of Internet Archive audio items that declare a
 * Creative Commons licence. Results are checked again against item metadata.
 */
object InternetArchiveCatalog {
    private const val SEARCH_ENDPOINT = "https://archive.org/advancedsearch.php"
    private const val METADATA_ENDPOINT = "https://archive.org/metadata/"

    suspend fun search(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tokens = query.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("[^\\p{L}\\p{N}_-]"), "") }
            .filter { it.isNotBlank() }
        val base = "mediatype:audio AND licenseurl:http*creativecommons.org/licenses/*"
        val terms = tokens.joinToString(" AND ") { token ->
            "(title:$token OR creator:$token OR subject:$token)"
        }
        // Do not force subject:music for the initial catalogue: many valid audio
        // uploads have no subject metadata and were therefore invisible.
        val searchQuery = if (terms.isBlank()) base else "$base AND ($terms)"
        val encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.name())
        val requestUrl = "$SEARCH_ENDPOINT?q=$encoded&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=licenseurl&rows=16&page=1&sort[]=downloads+desc&output=json"
        val root = getJson(URL(requestUrl))
        val docs = root.optJSONObject("response")?.optJSONArray("docs")
            ?: throw IllegalStateException("The online catalogue returned an unexpected response.")

        val seen = HashSet<String>()
        buildList {
            for (index in 0 until docs.length()) {
                val doc = docs.optJSONObject(index) ?: continue
                val identifier = doc.optString("identifier").takeIf { it.isNotBlank() } ?: continue
                if (!seen.add(identifier)) continue
                val metadata = runCatching { getJson(URL(METADATA_ENDPOINT + identifier)) }.getOrNull() ?: continue
                val item = metadata.optJSONObject("metadata") ?: JSONObject()
                val license = item.optString("licenseurl")
                    .ifBlank { doc.optString("licenseurl") }
                    .takeIf { it.startsWith("https://creativecommons.org/licenses/") || it.startsWith("http://creativecommons.org/licenses/") }
                    ?: continue
                val files = metadata.optJSONArray("files") ?: continue
                val audio = (0 until files.length()).mapNotNull { files.optJSONObject(it) }
                    .firstOrNull { file ->
                        val name = file.optString("name")
                        val format = file.optString("format")
                        !name.startsWith("__") && (
                            format.contains("MP3", true) || name.endsWith(".mp3", true) ||
                            format.contains("Ogg Vorbis", true) || name.endsWith(".ogg", true)
                        )
                    } ?: continue
                val fileName = audio.optString("name").takeIf { it.isNotBlank() } ?: continue
                val encodedName = fileName.split("/").joinToString("/") {
                    URLEncoder.encode(it, StandardCharsets.UTF_8.name()).replace("+", "%20")
                }
                val rawTitle = item.optString("title").ifBlank { doc.optString("title") }
                val title = rawTitle.takeIf { it.isNotBlank() && it != identifier && !it.matches(Regex("[0-9a-fA-F-]{20,}")) }
                    ?: fileName.substringAfterLast('/').substringBeforeLast('.')
                        .replace(Regex("[_-]+"), " ").trim().ifBlank { "Untitled audio" }
                val artist = item.optString("creator").ifBlank { doc.optString("creator") }
                    .ifBlank { "Independent creator" }
                val duration = parseDuration(item.optString("runtime").ifBlank { audio.optString("length") })
                add(
                    OnlineTrack(
                        id = "$identifier:$fileName",
                        title = title,
                        artist = artist,
                        album = item.optString("album").ifBlank { "Internet Archive" },
                        durationSeconds = duration,
                        streamUrl = "https://archive.org/download/$identifier/$encodedName",
                        artworkUrl = "https://archive.org/services/img/$identifier",
                        shareUrl = "https://archive.org/details/$identifier",
                        licenseUrl = license
                    )
                )
            }
        }
    }

    private fun parseDuration(value: String): Long {
        value.toDoubleOrNull()?.let { return it.toLong().coerceAtLeast(0L) }
        val parts = value.split(":").mapNotNull { it.toDoubleOrNull() }
        if (parts.isEmpty() || parts.size != value.count { it == ':' } + 1) return 0L
        return parts.fold(0.0) { total, part -> total * 60.0 + part }.toLong().coerceAtLeast(0L)
    }

    private fun getJson(url: URL): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 9_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AETHRA-Android/1.5")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("The open music catalogue returned HTTP $status.")
            }
            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
