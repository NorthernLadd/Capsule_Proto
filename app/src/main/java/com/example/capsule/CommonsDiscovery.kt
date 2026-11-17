package com.example.capsule

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DiscoveryCapsule(
    val id: Long, val title: String, val lat: Double, val lon: Double,
    val image: String, val source: String, val artist: String, val credit: String,
    val license: String, val licenseUrl: String,
    val unlockAt: Long, var openedAt: Long = 0
) {
    fun json() = JSONObject().put("id", id).put("title", title).put("lat", lat).put("lon", lon)
        .put("image", image).put("source", source).put("artist", artist).put("credit", credit)
        .put("license", license).put("licenseUrl", licenseUrl).put("unlockAt", unlockAt).put("openedAt", openedAt)
    companion object {
        fun from(j: JSONObject) = DiscoveryCapsule(j.getLong("id"), j.getString("title"),
            j.getDouble("lat"), j.getDouble("lon"), j.getString("image"), j.getString("source"),
            j.getString("artist"), j.optString("credit"), j.getString("license"), j.getString("licenseUrl"),
            j.getLong("unlockAt"), j.optLong("openedAt"))
    }
}

class DiscoveryStore(context: Context) {
    private val prefs = context.getSharedPreferences("commons_discovery", Context.MODE_PRIVATE)
    fun load(): MutableList<DiscoveryCapsule> {
        val data = JSONArray(prefs.getString("capsules", "[]"))
        return (0 until data.length()).map { DiscoveryCapsule.from(data.getJSONObject(it)) }.toMutableList()
    }
    fun save(items: List<DiscoveryCapsule>) {
        prefs.edit().putString("capsules", JSONArray().apply { items.forEach { put(it.json()) } }.toString()).apply()
    }
}

object CommonsClient {
    private const val ENDPOINT = "https://commons.wikimedia.org/w/api.php"
    fun connection(url: String): HttpURLConnection {
        require(URL(url).protocol == "https")
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", "CapsulePrototype/1.0 (educational Android Commons discovery prototype)")
        }
    }
    private fun request(parameters: Map<String, String>): JSONObject {
        val uri = android.net.Uri.parse(ENDPOINT).buildUpon()
            .appendQueryParameter("action", "query").appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
        parameters.forEach { (k, v) -> uri.appendQueryParameter(k, v) }
        val connection = connection(uri.build().toString())
        try {
            if (connection.responseCode != 200) error("Commons returned HTTP ${connection.responseCode}. Try again later.")
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            if (json.has("error")) error(json.getJSONObject("error").optString("info", "Commons request failed"))
            return json
        } finally { connection.disconnect() }
    }
    // Blocking IO: caller runs on a single background executor.
    fun search(lat: Double, lon: Double): List<DiscoveryCapsule> {
        val nearby = request(mapOf("list" to "geosearch", "gscoord" to "$lat|$lon",
            "gsradius" to "2000", "gsnamespace" to "6", "gsprimary" to "all", "gslimit" to "10"))
            .optJSONObject("query")?.optJSONArray("geosearch") ?: return emptyList()
        val points = (0 until nearby.length()).map { nearby.getJSONObject(it) }
            .associateBy { it.getLong("pageid") }
        val result = mutableListOf<DiscoveryCapsule>()
        // Small sequential batches limit the cost of extended metadata requests.
        points.keys.toList().chunked(5).forEach { ids ->
            val pages = request(mapOf("pageids" to ids.joinToString("|"), "prop" to "imageinfo",
                "iiprop" to "url|mime|extmetadata", "iiurlwidth" to "800"))
                .optJSONObject("query")?.optJSONArray("pages") ?: JSONArray()
            for (i in 0 until pages.length()) {
                val page = pages.getJSONObject(i)
                val point = points[page.optLong("pageid")] ?: continue
                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val meta = info.optJSONObject("extmetadata") ?: continue
                fun field(name: String) = meta.optJSONObject(name)?.optString("value").orEmpty()
                val license = field("LicenseShortName")
                val licenseUrl = field("LicenseUrl").let { if (it.startsWith("//")) "https:$it" else it }
                val image = info.optString("thumburl")
                val source = info.optString("descriptionurl")
                val artist = field("Artist")
                val latitude = point.optDouble("lat", Double.NaN)
                val longitude = point.optDouble("lon", Double.NaN)
                if (info.optString("mime") !in listOf("image/jpeg", "image/png")) continue
                if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) continue
                if (!image.startsWith("https://") || !source.startsWith("https://") || artist.isBlank()) continue
                if (!licenseUrl.startsWith("https://creativecommons.org/")) continue
                if (!(license.startsWith("CC BY ") || license.startsWith("CC BY-SA ") || license == "CC0")) continue
                result += DiscoveryCapsule(page.getLong("pageid"),
                    page.getString("title").removePrefix("File:").substringBeforeLast('.').replace('_', ' '),
                    latitude, longitude, image, source, artist, field("Credit"), license, licenseUrl,
                    System.currentTimeMillis() + 60_000)
            }
        }
        return result
    }
}
