package com.example.apkbookingfilm.update

import com.example.apkbookingfilm.BuildConfig
import org.json.JSONObject
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long
) : Serializable

/**
 * Cek versi terbaru dari GitHub Releases (dibuat otomatis oleh GitHub Actions setiap push).
 * Tag rilis berformat v1.0.<versionCode>, contoh: v1.0.12
 */
object UpdateChecker {

    private val LATEST_URL = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

    /** Blocking — panggil dari background thread. */
    fun fetchLatest(): UpdateInfo? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(LATEST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "CinemaBooking-Android")
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parse(JSONObject(body))
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(root: JSONObject): UpdateInfo? {
        val tag = root.optString("tag_name", "")
        val versionCode = Regex("(\\d+)$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: return null

        val assets = root.optJSONArray("assets") ?: return null
        var apkUrl = ""
        var size = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = a.optString("browser_download_url")
                size = a.optLong("size", 0L)
                break
            }
        }
        if (apkUrl.isEmpty()) return null

        val name = root.optString("name", "").ifEmpty { tag }
        val changelog = root.optString("body", "").let { if (it == "null") "" else it }.trim()

        return UpdateInfo(
            versionCode = versionCode,
            versionName = tag.removePrefix("v").ifEmpty { name },
            changelog = changelog,
            apkUrl = apkUrl,
            apkSize = size
        )
    }

    fun isNewer(info: UpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE
}
