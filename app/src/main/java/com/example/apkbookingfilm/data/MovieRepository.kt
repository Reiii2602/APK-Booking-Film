package com.example.apkbookingfilm.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.TmdbApi.Category
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

class MovieRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_cinema_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_POINTS = "user_points"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_SAVED_IDS = "saved_movie_ids_v2"
        private const val KEY_TICKET_COUNT = "ticket_count"
        private const val KEY_LATEST_BOOKING_ID = "latest_booking_id"
        private const val KEY_LATEST_MOVIE_ID = "latest_movie_id"
        private const val KEY_LATEST_CINEMA_NAME = "latest_cinema_name"
        private const val KEY_LATEST_SEATS = "latest_seats"
        private const val KEY_LATEST_DATE = "latest_date"
        private const val KEY_LATEST_TIME = "latest_time"
        private const val KEY_LATEST_TOTAL = "latest_total"

        private const val CACHE_FILE = "tmdb_cache.json"
        private const val PAGES_PER_CATEGORY = 2

        /** Data cadangan kalau HP offline & belum pernah ambil data dari TMDB. */
        private val offlineMovies = listOf(
            Movie(
                id = "dune-2", title = "Dune: Part Two", genre = "Sci-Fi, Adventure",
                duration = "2h 46m", ageRating = "13+", rating = "8.2",
                synopsis = "Paul Atreides unites with Chani and the Fremen on a path of revenge, facing a choice between love and the fate of the universe.",
                posterResId = R.drawable.img_poster_dune, backdropResId = R.drawable.img_backdrop_dune,
                posterPath = "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg", backdropPath = "/uhUO7vQQKvCTfQWubOt5MAKokbL.jpg",
                director = "Denis Villeneuve", isHighlight = true, tmdbId = 693134, releaseDate = "2024-02-27"
            ),
            Movie(
                id = "killers-flower-moon", title = "Killers of the Flower Moon", genre = "Crime, Drama, History",
                duration = "3h 26m", ageRating = "17+", rating = "7.5",
                synopsis = "When oil is discovered in 1920s Oklahoma under Osage Nation land, the Osage people are murdered one by one until the FBI steps in to unravel the conspiracy.",
                posterResId = R.drawable.img_poster_killers, backdropResId = R.drawable.img_backdrop_killers,
                posterPath = "/dB6Krk806zeqd0YNp2ngQ9zXteH.jpg", backdropPath = "/acvE3RWjDLgvbL2RtcyzkrsAyNV.jpg",
                director = "Martin Scorsese", tmdbId = 466420, releaseDate = "2023-10-18"
            ),
            Movie(
                id = "oppenheimer", title = "Oppenheimer", genre = "Drama, History",
                duration = "3h 01m", ageRating = "17+", rating = "8.1",
                synopsis = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
                posterResId = R.drawable.img_poster_oppenheimer, backdropResId = R.drawable.img_backdrop_oppenheimer,
                posterPath = "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", backdropPath = "/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                director = "Christopher Nolan", tmdbId = 872585, releaseDate = "2023-07-19"
            ),
            Movie(
                id = "poor-things", title = "Poor Things", genre = "Comedy, Drama, Romance",
                duration = "2h 21m", ageRating = "21+", rating = "7.7",
                synopsis = "The incredible tale about the fantastical evolution of Bella Baxter, a young woman brought back to life by the brilliant and unorthodox scientist Dr. Godwin Baxter.",
                posterResId = R.drawable.img_poster_poor_things, backdropResId = R.drawable.img_backdrop_poor_things,
                posterPath = "/kCGlIMHnOm8JPXq3rXM6c5wMxcT.jpg", backdropPath = "/zh6IdheEYinU4TPtorWsjx6qPQE.jpg",
                director = "Yorgos Lanthimos", tmdbId = 792307, releaseDate = "2023-12-07"
            ),
            Movie(
                id = "maestro", title = "Maestro", genre = "Drama, Music",
                duration = "2h 09m", ageRating = "17+", rating = "6.6",
                synopsis = "A towering and fearless love story chronicling the lifelong relationship between Leonard Bernstein and Felicia Montealegre Cohn Bernstein.",
                posterResId = R.drawable.img_poster_maestro, backdropResId = R.drawable.img_backdrop_maestro,
                posterPath = "/kxj7rMco6RNYsVcNwuGAIlfWu64.jpg", backdropPath = "/et0G74BxoBgNQEZBkUcVhsgeRFF.jpg",
                director = "Bradley Cooper", tmdbId = 523607, releaseDate = "2023-12-20"
            ),
            Movie(
                id = "the-fall-guy", title = "The Fall Guy", genre = "Action, Comedy",
                duration = "2h 06m", ageRating = "13+", rating = "7.1",
                synopsis = "A down-and-out stuntman must track down a missing movie star, solve a conspiracy and try to win back the love of his life.",
                posterResId = R.drawable.img_poster_fall_guy, backdropResId = R.drawable.img_poster_fall_guy,
                posterPath = "/e7olqFmzcIX5c23kX4zSmLPJi8c.jpg", backdropPath = "/e7olqFmzcIX5c23kX4zSmLPJi8c.jpg",
                director = "David Leitch", isComingSoon = true, isNowShowing = false, tmdbId = 746036, releaseDate = "2024-04-24"
            ),
            Movie(
                id = "kingdom-planet-apes", title = "Kingdom of the Planet of the Apes", genre = "Sci-Fi, Action, Adventure",
                duration = "2h 25m", ageRating = "13+", rating = "7.1",
                synopsis = "Several generations in the future following Caesar's reign, apes are now the dominant species and live harmoniously while humans have been reduced to living in the shadows.",
                posterResId = R.drawable.img_poster_kingdom_apes, backdropResId = R.drawable.img_poster_kingdom_apes,
                posterPath = "/gKkl37BQuKTanygYQG1pyYgLVgf.jpg", backdropPath = "/z121auT5q9Smum2KP05vm0am92P.jpg",
                director = "Wes Ball", isComingSoon = true, isNowShowing = false, tmdbId = 653346, releaseDate = "2024-05-08"
            )
        )

        // ---- State global (hanya diubah di main thread) ----
        val allMovies: MutableList<Movie> = ArrayList(offlineMovies)
        private val categoryIds: MutableMap<Category, List<String>> = mutableMapOf()
        private var cacheLoaded = false
        private var lastFetchMs = 0L
        @Volatile var isLoading = false
            private set
        private val listeners = mutableListOf<() -> Unit>()
        private val executor = Executors.newFixedThreadPool(3)
        private val mainHandler = Handler(Looper.getMainLooper())

        fun addOnMoviesUpdatedListener(listener: () -> Unit) { listeners.add(listener) }
        fun removeOnMoviesUpdatedListener(listener: () -> Unit) { listeners.remove(listener) }

        private fun notifyListeners() {
            listeners.toList().forEach { it.invoke() }
        }

        /** Tambah / perbarui film (mis. hasil search atau halaman berikutnya). Main thread. */
        fun upsertMovies(list: List<Movie>) {
            for (m in list) {
                val idx = allMovies.indexOfFirst { it.id == m.id }
                if (idx >= 0) {
                    // jangan timpa data detail lengkap dengan data list yang lebih sedikit
                    if (!allMovies[idx].hasFullDetail || m.hasFullDetail) allMovies[idx] = m
                } else {
                    allMovies.add(m)
                }
            }
        }
    }

    private val appContext = context.applicationContext

    val movies: List<Movie> get() = allMovies

    val cinemas: List<Cinema> = listOf(
        Cinema(
            id = "ambarrukmo-xxi",
            name = "Ambarrukmo XXI",
            location = "Plaza Ambarrukmo Lt. 3, Jl. Laksda Adisucipto, Yogyakarta",
            distance = "1.2 km",
            formats = listOf("PREMIERE", "IMAX WITH LASER", "DOLBY ATMOS"),
            showtimes = listOf("12:30", "15:15", "18:00", "20:45"),
            ticketPrice = 50000
        ),
        Cinema(
            id = "empire-xxi",
            name = "Empire XXI Jogja",
            location = "Jl. Urip Sumoharjo No. 104, Gondokusuman, Yogyakarta",
            distance = "2.4 km",
            formats = listOf("PREMIERE", "DOLBY ATMOS", "2D REGULAR"),
            showtimes = listOf("13:00", "15:45", "18:30", "21:15"),
            ticketPrice = 50000
        ),
        Cinema(
            id = "jogja-city-xxi",
            name = "Jogja City XXI",
            location = "Jogja City Mall Lt. 2, Jl. Magelang Km. 6, Yogyakarta",
            distance = "3.5 km",
            formats = listOf("PREMIERE", "IMAX", "DOLBY ATMOS"),
            showtimes = listOf("12:45", "15:30", "18:15", "21:00"),
            ticketPrice = 50000
        ),
        Cinema(
            id = "cgv-pakuwon",
            name = "CGV Pakuwon Mall Jogja",
            location = "Pakuwon Mall Lt. 2, Jl. Ring Road Utara, Sleman, Yogyakarta",
            distance = "4.2 km",
            formats = listOf("4DX", "SPHERE X", "VELVET CLASS"),
            showtimes = listOf("13:15", "16:00", "18:45", "21:30"),
            ticketPrice = 50000
        ),
        Cinema(
            id = "cinepolis-lippo",
            name = "Cinepolis Lippo Plaza Jogja",
            location = "Lippo Plaza Lt. 1, Jl. Laksda Adisucipto No. 32, Yogyakarta",
            distance = "2.8 km",
            formats = listOf("VIP CINEMA", "MACRO XE", "REGULAR"),
            showtimes = listOf("13:30", "16:15", "19:00", "21:45"),
            ticketPrice = 50000
        )
    )

    init {
        loadCacheIfNeeded()
        fetchMoviesFromTmdb()
    }

    // ================================================================ TMDB

    /** Ambil semua kategori (Trending, Now Playing, Popular, Top Rated, Upcoming) dari TMDB. */
    fun fetchMoviesFromTmdb(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isLoading) return
        if (!force && lastFetchMs != 0L && now - lastFetchMs < 10 * 60 * 1000) return
        isLoading = true

        executor.execute {
            val fetched = LinkedHashMap<String, Movie>()
            val ids = mutableMapOf<Category, List<String>>()
            for (cat in Category.values()) {
                val catIds = mutableListOf<String>()
                for (page in 1..PAGES_PER_CATEGORY) {
                    val result = TmdbApi.fetchCategory(cat, page) ?: break
                    result.movies.forEach { m ->
                        if (!fetched.containsKey(m.id)) fetched[m.id] = m
                        if (!catIds.contains(m.id)) catIds.add(m.id)
                    }
                    if (page >= result.totalPages) break
                }
                if (catIds.isNotEmpty()) ids[cat] = catIds
            }

            mainHandler.post {
                isLoading = false
                if (fetched.isNotEmpty()) {
                    lastFetchMs = System.currentTimeMillis()
                    applyFetched(fetched.values.toList(), ids)
                    saveCache()
                }
                notifyListeners()
            }
        }
    }

    private fun applyFetched(fetched: List<Movie>, ids: Map<Category, List<String>>) {
        val keepIds = getSavedIds() + setOfNotNull(prefs.getString(KEY_LATEST_MOVIE_ID, null))
        val previous = allMovies.associateBy { it.id }
        val merged = LinkedHashMap<String, Movie>()
        fetched.forEach { m ->
            val old = previous[m.id]
            // pertahankan detail lengkap yang sudah pernah diambil
            merged[m.id] = if (old != null && old.hasFullDetail) old.copy(
                rating = m.rating, voteCount = m.voteCount,
                isNowShowing = m.isNowShowing, isComingSoon = m.isComingSoon
            ) else m
        }
        // film yang disimpan user / tiket terakhir tetap disimpan walau tidak ada di daftar baru
        previous.values.filter { keepIds.contains(it.id) && !merged.containsKey(it.id) }
            .forEach { merged[it.id] = it }

        allMovies.clear()
        allMovies.addAll(merged.values)
        categoryIds.clear()
        categoryIds.putAll(ids)
    }

    /** Ambil detail lengkap (durasi, cast, sutradara, trailer) lalu update cache. */
    fun fetchMovieDetail(movieId: String, callback: (Movie?) -> Unit) {
        val base = getMovieById(movieId)
        val tmdbId = base?.tmdbId?.takeIf { it > 0 }
            ?: movieId.removePrefix("tmdb-").toIntOrNull() ?: 0
        if (tmdbId <= 0) { callback(base); return }

        executor.execute {
            val detail = TmdbApi.fetchDetail(tmdbId, base)
            mainHandler.post {
                if (detail != null) upsertMovies(listOf(detail))
                callback(detail ?: base)
            }
        }
    }

    /** Daftar film untuk satu kategori (fallback ke data lokal bila offline). */
    fun getCategory(category: Category): List<Movie> {
        val ids = categoryIds[category]
        if (!ids.isNullOrEmpty()) {
            val byId = allMovies.associateBy { it.id }
            val list = ids.mapNotNull { byId[it] }
            if (list.isNotEmpty()) return list
        }
        return when (category) {
            Category.UPCOMING -> allMovies.filter { it.isComingSoon }
            Category.NOW_PLAYING -> allMovies.filter { it.isNowShowing }
            Category.TOP_RATED -> allMovies.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            else -> allMovies
        }
    }

    fun getHighlightMovie(): Movie {
        return getCategory(Category.TRENDING).firstOrNull { it.backdropPath.isNotEmpty() && it.isNowShowing }
            ?: allMovies.firstOrNull { it.isHighlight }
            ?: allMovies.first()
    }

    // ---------------------------------------------------------------- cache file

    private fun loadCacheIfNeeded() {
        if (cacheLoaded) return
        cacheLoaded = true
        try {
            val file = File(appContext.filesDir, CACHE_FILE)
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("movies") ?: return
            val list = mutableListOf<Movie>()
            for (i in 0 until arr.length()) {
                TmdbApi.movieFromJson(arr.getJSONObject(i))?.let { list.add(it) }
            }
            if (list.isEmpty()) return
            val cats = root.optJSONObject("categories")
            val ids = mutableMapOf<Category, List<String>>()
            if (cats != null) {
                for (cat in Category.values()) {
                    val a = cats.optJSONArray(cat.name) ?: continue
                    ids[cat] = (0 until a.length()).map { a.getString(it) }
                }
            }
            allMovies.clear()
            allMovies.addAll(list)
            categoryIds.clear()
            categoryIds.putAll(ids)
        } catch (_: Exception) {
            // cache rusak -> abaikan
        }
    }

    private fun saveCache() {
        val snapshotMovies = allMovies.toList()
        val snapshotCats = categoryIds.toMap()
        executor.execute {
            try {
                val arr = JSONArray()
                snapshotMovies.filter { it.tmdbId > 0 }.forEach { arr.put(TmdbApi.movieToJson(it)) }
                val cats = JSONObject()
                snapshotCats.forEach { (cat, ids) -> cats.put(cat.name, JSONArray(ids)) }
                val root = JSONObject().put("movies", arr).put("categories", cats)
                File(appContext.filesDir, CACHE_FILE).writeText(root.toString())
            } catch (_: Exception) {
            }
        }
    }

    fun getMovieById(id: String): Movie? {
        return allMovies.find { it.id == id }
    }

    fun getCinemaById(id: String): Cinema? {
        return cinemas.find { it.id == id }
    }

    // ================================================================ Authentication
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUser(): User {
        val isLoggedIn = isLoggedIn()
        return if (isLoggedIn) {
            val name = prefs.getString(KEY_USER_NAME, "Daffa Fadhul Rahman") ?: "Daffa Fadhul Rahman"
            val email = prefs.getString(KEY_USER_EMAIL, "daffasigma@gmail.com") ?: "daffasigma@gmail.com"
            val authProvider = prefs.getString(KEY_AUTH_PROVIDER, "Google") ?: "Google"
            val points = prefs.getInt(KEY_USER_POINTS, 1240)
            val initials = name.split(" ").filter { it.isNotEmpty() }
                .map { it.first().uppercase() }
                .take(2).joinToString("")
            User(
                name = name,
                email = email,
                avatarInitials = if (initials.isNotEmpty()) initials else "DF",
                moviesCount = prefs.getInt(KEY_TICKET_COUNT, 12),
                savedCount = getSavedIds().size,
                reviewsCount = 8,
                points = points,
                membershipTier = "CINEMA PLUS • SINCE 2024",
                authProvider = authProvider,
                isLoggedIn = true
            )
        } else {
            User(
                name = "Guest User",
                email = "",
                avatarInitials = "GU",
                moviesCount = 0,
                savedCount = 0,
                reviewsCount = 0,
                points = 0,
                membershipTier = "Guest",
                authProvider = "None",
                isLoggedIn = false
            )
        }
    }

    fun login(name: String = "Daffa Fadhul Rahman", email: String = "daffasigma@gmail.com", provider: String = "Google") {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_AUTH_PROVIDER, provider)
            putInt(KEY_USER_POINTS, prefs.getInt(KEY_USER_POINTS, 1240))
            apply()
        }
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    // ================================================================ Saved Films
    private fun getSavedIds(): Set<String> = prefs.getStringSet(KEY_SAVED_IDS, null) ?: emptySet()

    fun isMovieSaved(movieId: String): Boolean = getSavedIds().contains(movieId)

    fun toggleSaved(movieId: String): Boolean {
        val currentSet = getSavedIds().toMutableSet()
        val isNowSaved = if (currentSet.contains(movieId)) {
            currentSet.remove(movieId)
            false
        } else {
            currentSet.add(movieId)
            true
        }
        prefs.edit().putStringSet(KEY_SAVED_IDS, currentSet).apply()
        saveCache()
        return isNowSaved
    }

    fun getSavedMovies(): List<Movie> {
        val set = getSavedIds()
        return allMovies.filter { set.contains(it.id) }
    }

    // ================================================================ Booking
    fun saveBooking(booking: Booking) {
        val ticketsCount = prefs.getInt(KEY_TICKET_COUNT, 12) + booking.seats.size
        val currentPoints = prefs.getInt(KEY_USER_POINTS, 1240) + (booking.totalPrice / 1000)
        prefs.edit().apply {
            putString(KEY_LATEST_BOOKING_ID, booking.bookingId)
            putString(KEY_LATEST_MOVIE_ID, booking.movie.id)
            putString(KEY_LATEST_CINEMA_NAME, booking.cinema.name)
            putString(KEY_LATEST_SEATS, booking.seats.joinToString(", "))
            putString(KEY_LATEST_DATE, booking.date)
            putString(KEY_LATEST_TIME, booking.time)
            putInt(KEY_LATEST_TOTAL, booking.totalPrice)
            putInt(KEY_TICKET_COUNT, ticketsCount)
            putInt(KEY_USER_POINTS, currentPoints)
            apply()
        }
        upsertMovies(listOf(booking.movie))
        saveCache()
    }

    fun getLatestBooking(): Booking? {
        val bookingId = prefs.getString(KEY_LATEST_BOOKING_ID, null) ?: "BK-98421"
        val movieId = prefs.getString(KEY_LATEST_MOVIE_ID, "dune-2") ?: "dune-2"
        val movie = getMovieById(movieId) ?: allMovies[0]
        val cinemaName = prefs.getString(KEY_LATEST_CINEMA_NAME, "Ambarrukmo XXI") ?: "Ambarrukmo XXI"
        val cinema = cinemas.find { it.name == cinemaName } ?: cinemas[0]
        val seatsStr = prefs.getString(KEY_LATEST_SEATS, "E6, E7") ?: "E6, E7"
        val seats = seatsStr.split(", ")
        val date = prefs.getString(KEY_LATEST_DATE, "Fri, 08 Nov • 19:30") ?: "Fri, 08 Nov • 19:30"
        val time = prefs.getString(KEY_LATEST_TIME, "19:30") ?: "19:30"
        val total = prefs.getInt(KEY_LATEST_TOTAL, 50000)

        return Booking(
            bookingId = bookingId,
            movie = movie,
            cinema = cinema,
            date = date,
            time = time,
            seats = seats,
            subtotal = total,
            serviceFee = 0,
            totalPrice = total,
            isPaid = true
        )
    }
}
