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

/**
 * Public, unauthenticated discovery for audio items that declare a Creative Commons licence.
 * This catalogue is not a substitute for commercial music services.
 */
object InternetArchiveCatalog {
    private const val SEARCH_ENDPOINT = "https://archive.org/advancedsearch.php"
    private const val METADATA_ENDPOINT = "https://archive.org/metadata/"

    suspend fun search(query: String): List<OnlineTrack> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val tokens = query.trim().split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .map { it.replace(Regex("[^A-Za-z0-9_-]"), "") }
                .filter { it.isNotBlank() }
            val base = "mediatype:audio AND licenseurl:http*creativecommons.org/licenses/*"
            val terms = tokens.joinToString(" AND ") { token ->
                "(title:$token OR creator:$token OR subject:$token)"
            }
            val searchQuery = if (terms.isBlank()) "$base AND subject:music" else "$base AND ($terms)"
            val encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.name())
            val root = getJson(URL("$SEARCH_ENDPOINT?q=$encoded&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=licenseurl&rows=24&page=1&output=json"))
            val docs = root.optJSONObject("response")?.optJSONArray("docs")
                ?: return@withContext emptyList()

            buildList {
                for (index in 0 until docs.length()) {
                    val doc = docs.optJSONObject(index) ?: continue
                    val identifier = doc.optString("identifier").takeIf { it.isNotBlank() } ?: continue
                    val license = doc.optString("licenseurl").takeIf {
                        it.startsWith("https://creativecommons.org/") ||
                            it.startsWith("http://creativecommons.org/")
                    } ?: continue
                    val metadata = runCatching { getJson(URL(METADATA_ENDPOINT + identifier)) }.getOrNull() ?: continue
                    val item = metadata.optJSONObject("metadata") ?: JSONObject()
                    val files = metadata.optJSONArray("files") ?: continue
                    val audio = (0 until files.length()).mapNotNull { files.optJSONObject(it) }
                        .firstOrNull { file ->
                            val name = file.optString("name")
                            val format = file.optString("format")
                            !name.startsWith("__") &&
                                (format.contains("MP3", true) || name.endsWith(".mp3", true) ||
                                    format.contains("Ogg Vorbis", true) || name.endsWith(".ogg", true))
                        } ?: continue
                    val fileName = audio.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val encodedName = fileName.split("/").joinToString("/") {
                        URLEncoder.encode(it, StandardCharsets.UTF_8.name()).replace("+", "%20")
                    }
                    val title = item.optString("title").ifBlank { doc.optString("title") }.ifBlank { "Untitled audio" }
                    val artist = item.optString("creator").ifBlank { doc.optString("creator") }.ifBlank { "Independent creator" }
                    val duration = item.optString("runtime").toDoubleOrNull()?.toLong() ?: 0L
                    add(
                        OnlineTrack(
                            id = "$identifier:$fileName".hashCode().toString(),
                            title = title,
                            artist = artist,
                            album = "Internet Archive",
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

    private fun getJson(url: URL): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 18_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AETHRA-Android/1.3")
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
