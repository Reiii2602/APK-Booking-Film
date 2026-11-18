package com.example.apkbookingfilm.data

import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.model.CastMember
import com.example.apkbookingfilm.model.Movie
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Client sederhana untuk The Movie Database (TMDB) API v3.
 * Semua fungsi di sini bersifat blocking -> panggil dari background thread.
 */
object TmdbApi {

    const val API_KEY = "2dca580c2a14b55200e784d157207b4d"
    private const val BASE = "https://api.themoviedb.org/3"
    private const val LANG = "en-US"

    enum class Category(val path: String, val label: String) {
        TRENDING("trending/movie/week", "Trending"),
        NOW_PLAYING("movie/now_playing", "Now Showing"),
        POPULAR("movie/popular", "Popular"),
        TOP_RATED("movie/top_rated", "Top Rated"),
        UPCOMING("movie/upcoming", "Coming Soon")
    }

    data class MoviePage(val movies: List<Movie>, val page: Int, val totalPages: Int)

    val genreMap = mapOf(
        28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
        80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
        14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
        9648 to "Mystery", 10749 to "Romance", 878 to "Sci-Fi", 10770 to "TV Movie",
        53 to "Thriller", 10752 to "War", 37 to "Western"
    )

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ---------------------------------------------------------------- network

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun fetchCategory(category: Category, page: Int = 1): MoviePage? {
        val url = "$BASE/${category.path}?api_key=$API_KEY&language=$LANG&page=$page"
        val body = httpGet(url) ?: return null
        return parsePage(body, forceComingSoon = category == Category.UPCOMING)
    }

    fun search(query: String, page: Int = 1): MoviePage? {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE/search/movie?api_key=$API_KEY&language=$LANG&include_adult=false&query=$q&page=$page"
        val body = httpGet(url) ?: return null
        return parsePage(body, forceComingSoon = false)
    }

    /** Detail lengkap: durasi, genre, sutradara, pemeran, trailer, rating usia. */
    fun fetchDetail(tmdbId: Int, base: Movie?): Movie? {
        if (tmdbId <= 0) return null
        val url = "$BASE/movie/$tmdbId?api_key=$API_KEY&language=$LANG&append_to_response=credits,videos,release_dates&include_video_language=en,id,null"
        val body = httpGet(url) ?: return null
        return try {
            parseDetail(JSONObject(body), base)
        } catch (_: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------- parsing

    private fun parsePage(body: String, forceComingSoon: Boolean): MoviePage? {
        return try {
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: JSONArray()
            val list = mutableListOf<Movie>()
            for (i in 0 until results.length()) {
                parseListItem(results.getJSONObject(i), forceComingSoon)?.let { list.add(it) }
            }
            MoviePage(list, root.optInt("page", 1), root.optInt("total_pages", 1))
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.str(key: String): String {
        val v = optString(key, "")
        return if (v == "null") "" else v
    }

    private fun parseListItem(obj: JSONObject, forceComingSoon: Boolean): Movie? {
        val title = obj.str("title")
        val tmdbId = obj.optInt("id", 0)
        if (title.isEmpty() || tmdbId == 0) return null

        val releaseDate = obj.str("release_date")
        val genreIds = obj.optJSONArray("genre_ids")
        val genres = mutableListOf<String>()
        if (genreIds != null) {
            for (i in 0 until genreIds.length()) {
                genreMap[genreIds.optInt(i)]?.let { genres.add(it) }
                if (genres.size >= 2) break
            }
        }
        val isComingSoon = forceComingSoon || (releaseDate.isNotEmpty() && releaseDate > today)
        val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "—"
        val isAdult = obj.optBoolean("adult", false)
        val ageRating = when {
            isAdult -> "21+"
            genres.contains("Animation") || genres.contains("Family") -> "SU"
            genres.contains("Horror") -> "17+"
            else -> "13+"
        }

        return Movie(
            id = "tmdb-$tmdbId",
            title = title,
            genre = if (genres.isNotEmpty()) genres.joinToString(", ") else "Drama",
            duration = year,
            ageRating = ageRating,
            rating = String.format(Locale.US, "%.1f", obj.optDouble("vote_average", 0.0)),
            synopsis = obj.str("overview").ifEmpty { "Sinopsis belum tersedia." },
            posterResId = R.drawable.bg_poster_placeholder,
            backdropResId = R.drawable.bg_poster_placeholder,
            posterPath = obj.str("poster_path"),
            backdropPath = obj.str("backdrop_path"),
            director = "",
            trailerDuration = "",
            isNowShowing = !isComingSoon,
            isComingSoon = isComingSoon,
            tmdbId = tmdbId,
            releaseDate = releaseDate,
            voteCount = obj.optInt("vote_count", 0),
            originalLanguage = obj.str("original_language")
        )
    }

    private fun parseDetail(obj: JSONObject, base: Movie?): Movie {
        val tmdbId = obj.optInt("id")
        val listVersion = parseListItem(obj, forceComingSoon = false)

        val runtime = obj.optInt("runtime", 0)
        val durationStr = if (runtime > 0) "${runtime / 60}h ${String.format(Locale.US, "%02d", runtime % 60)}m"
        else (base?.duration ?: listVersion?.duration ?: "—")

        val genresArr = obj.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genresArr != null) {
            for (i in 0 until minOf(genresArr.length(), 3)) {
                val name = genresArr.getJSONObject(i).str("name")
                if (name.isNotEmpty()) genres.add(if (name == "Science Fiction") "Sci-Fi" else name)
            }
        }

        // Credits
        val cast = mutableListOf<CastMember>()
        val directors = mutableListOf<String>()
        obj.optJSONObject("credits")?.let { credits ->
            credits.optJSONArray("cast")?.let { arr ->
                for (i in 0 until minOf(arr.length(), 20)) {
                    val c = arr.getJSONObject(i)
                    cast.add(CastMember(c.str("name"), c.str("character"), c.str("profile_path")))
                }
            }
            credits.optJSONArray("crew")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    if (c.str("job") == "Director") directors.add(c.str("name"))
                }
            }
        }

        // Trailer (YouTube)
        var trailerKey = ""
        obj.optJSONObject("videos")?.optJSONArray("results")?.let { arr ->
            var fallback = ""
            for (i in 0 until arr.length()) {
                val v = arr.getJSONObject(i)
                if (v.str("site") != "YouTube") continue
                if (v.str("type") == "Trailer") {
                    if (v.optBoolean("official", false)) { trailerKey = v.str("key"); break }
                    if (fallback.isEmpty()) fallback = v.str("key")
                } else if (fallback.isEmpty()) {
                    fallback = v.str("key")
                }
            }
            if (trailerKey.isEmpty()) trailerKey = fallback
        }

        // Rating usia (Indonesia > US)
        var ageRating = base?.ageRating ?: listVersion?.ageRating ?: "13+"
        obj.optJSONObject("release_dates")?.optJSONArray("results")?.let { arr ->
            val certs = mutableMapOf<String, String>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val country = r.str("iso_3166_1")
                val dates = r.optJSONArray("release_dates") ?: continue
                for (j in 0 until dates.length()) {
                    val cert = dates.getJSONObject(j).str("certification").trim()
                    if (cert.isNotEmpty()) { certs[country] = cert; break }
                }
            }
            val id = certs["ID"]
            val us = certs["US"]
            ageRating = when {
                id != null -> id
                us != null -> when (us) {
                    "G", "PG" -> "SU"
                    "PG-13" -> "13+"
                    "R" -> "17+"
                    "NC-17" -> "21+"
                    else -> ageRating
                }
                else -> ageRating
            }
        }

        val releaseDate = obj.str("release_date")
        val isComingSoon = base?.isComingSoon ?: (releaseDate.isNotEmpty() && releaseDate > today)

        return Movie(
            id = base?.id ?: "tmdb-$tmdbId",
            title = obj.str("title").ifEmpty { base?.title ?: "" },
            genre = if (genres.isNotEmpty()) genres.joinToString(", ") else (base?.genre ?: "Drama"),
            duration = durationStr,
            ageRating = ageRating,
            rating = String.format(Locale.US, "%.1f", obj.optDouble("vote_average", 0.0)),
            synopsis = obj.str("overview").ifEmpty { base?.synopsis ?: "Sinopsis belum tersedia." },
            posterResId = base?.posterResId ?: R.drawable.bg_poster_placeholder,
            backdropResId = base?.backdropResId ?: R.drawable.bg_poster_placeholder,
            posterPath = obj.str("poster_path").ifEmpty { base?.posterPath ?: "" },
            backdropPath = obj.str("backdrop_path").ifEmpty { base?.backdropPath ?: "" },
            director = if (directors.isNotEmpty()) directors.joinToString(", ") else "-",
            trailerDuration = "",
            isHighlight = base?.isHighlight ?: false,
            isNowShowing = !isComingSoon,
            isComingSoon = isComingSoon,
            basePrice = base?.basePrice ?: 50000,
            tmdbId = tmdbId,
            releaseDate = releaseDate,
            voteCount = obj.optInt("vote_count", 0),
            tagline = obj.str("tagline"),
            trailerKey = trailerKey,
            originalLanguage = obj.str("original_language"),
            cast = cast,
            hasFullDetail = true
        )
    }

    // ---------------------------------------------------------------- cache (JSON)

    fun movieToJson(m: Movie): JSONObject = JSONObject().apply {
        put("id", m.id); put("title", m.title); put("genre", m.genre)
        put("duration", m.duration); put("ageRating", m.ageRating); put("rating", m.rating)
        put("synopsis", m.synopsis); put("posterPath", m.posterPath); put("backdropPath", m.backdropPath)
        put("director", m.director); put("isNowShowing", m.isNowShowing); put("isComingSoon", m.isComingSoon)
        put("tmdbId", m.tmdbId); put("releaseDate", m.releaseDate); put("voteCount", m.voteCount)
        put("originalLanguage", m.originalLanguage)
    }

    fun movieFromJson(o: JSONObject): Movie? {
        val id = o.str("id")
        val title = o.str("title")
        if (id.isEmpty() || title.isEmpty()) return null
        return Movie(
            id = id,
            title = title,
            genre = o.str("genre"),
            duration = o.str("duration"),
            ageRating = o.str("ageRating"),
            rating = o.str("rating"),
            synopsis = o.str("synopsis"),
            posterResId = R.drawable.bg_poster_placeholder,
            backdropResId = R.drawable.bg_poster_placeholder,
            posterPath = o.str("posterPath"),
            backdropPath = o.str("backdropPath"),
            director = o.str("director"),
            trailerDuration = "",
            isNowShowing = o.optBoolean("isNowShowing", true),
            isComingSoon = o.optBoolean("isComingSoon", false),
            tmdbId = o.optInt("tmdbId", 0),
            releaseDate = o.str("releaseDate"),
            voteCount = o.optInt("voteCount", 0),
            originalLanguage = o.str("originalLanguage")
        )
    }
}
