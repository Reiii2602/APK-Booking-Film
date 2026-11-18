package com.example.apkbookingfilm.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.User
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MovieRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_cinema_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TMDB_API_KEY = "2dca580c2a14b55200e784d157207b4d"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_POINTS = "user_points"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_SAVED_IDS = "saved_movie_ids"
        private const val KEY_TICKET_COUNT = "ticket_count"
        private const val KEY_LATEST_BOOKING_ID = "latest_booking_id"
        private const val KEY_LATEST_MOVIE_ID = "latest_movie_id"
        private const val KEY_LATEST_CINEMA_NAME = "latest_cinema_name"
        private const val KEY_LATEST_SEATS = "latest_seats"
        private const val KEY_LATEST_DATE = "latest_date"
        private const val KEY_LATEST_TIME = "latest_time"
        private const val KEY_LATEST_TOTAL = "latest_total"

        private val genreMap = mapOf(
            28 to "Action",
            12 to "Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            14 to "Fantasy",
            36 to "History",
            27 to "Horror",
            10402 to "Music",
            9648 to "Mystery",
            10749 to "Romance",
            878 to "Sci-Fi",
            10770 to "TV Movie",
            53 to "Thriller",
            10752 to "War",
            37 to "Western"
        )

        private val initialMovies = mutableListOf(
            Movie(
                id = "dune-2",
                title = "Dune: Part Two",
                genre = "Sci-Fi, Adventure",
                duration = "2h 46m",
                ageRating = "13+",
                rating = "8.5",
                synopsis = "Paul Atreides unites with Chani and the Fremen on a path of revenge, facing a choice between love and the fate of the universe.",
                posterResId = R.drawable.img_poster_dune,
                backdropResId = R.drawable.img_backdrop_dune,
                posterPath = "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
                backdropPath = "/uhUO7vQQKvCTfQWubOt5MAKokbL.jpg",
                director = "Denis Villeneuve",
                trailerDuration = "2:24",
                isHighlight = true,
                isNowShowing = true,
                basePrice = 50000
            ),
            Movie(
                id = "killers-flower-moon",
                title = "Killers of the Flower Moon",
                genre = "Crime, Drama, History",
                duration = "3h 26m",
                ageRating = "17+",
                rating = "8.5",
                synopsis = "When oil is discovered in 1920s Oklahoma under Osage Nation land, the Osage people are murdered one by one until the FBI steps in to unravel the conspiracy.",
                posterResId = R.drawable.img_poster_killers,
                backdropResId = R.drawable.img_backdrop_killers,
                posterPath = "/dB6Krk806zeqd0YNp2ngQ9zXteH.jpg",
                backdropPath = "/acvE3RWjDLgvbL2RtcyzkrsAyNV.jpg",
                director = "Martin Scorsese",
                trailerDuration = "2:24",
                isNowShowing = true,
                basePrice = 50000
            ),
            Movie(
                id = "oppenheimer",
                title = "Openheimer",
                genre = "Biography, Drama, History",
                duration = "3h 00m",
                ageRating = "17+",
                rating = "8.5",
                synopsis = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
                posterResId = R.drawable.img_poster_oppenheimer,
                backdropResId = R.drawable.img_backdrop_oppenheimer,
                posterPath = "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                backdropPath = "/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                director = "Christopher Nolan",
                trailerDuration = "2:24",
                isNowShowing = true,
                basePrice = 50000
            ),
            Movie(
                id = "poor-things",
                title = "Poor Things",
                genre = "Comedy, Drama, Romance",
                duration = "2h 21m",
                ageRating = "21+",
                rating = "8.5",
                synopsis = "The incredible tale about the fantastical evolution of Bella Baxter, a young woman brought back to life by the brilliant and unorthodox scientist Dr. Godwin Baxter.",
                posterResId = R.drawable.img_poster_poor_things,
                backdropResId = R.drawable.img_backdrop_poor_things,
                posterPath = "/kCGlIMHnOm8JPXq3rXM6c5wMxcT.jpg",
                backdropPath = "/zh6IdheEYinU4TPtorWsjx6qPQE.jpg",
                director = "Yorgos Lanthimos",
                trailerDuration = "2:24",
                isNowShowing = true,
                basePrice = 50000
            ),
            Movie(
                id = "maestro",
                title = "Maestro",
                genre = "Biography, Drama, Music",
                duration = "2h 09m",
                ageRating = "17+",
                rating = "8.5",
                synopsis = "A towering and fearless love story chronicling the lifelong relationship between Leonard Bernstein and Felicia Montealegre Cohn Bernstein.",
                posterResId = R.drawable.img_poster_maestro,
                backdropResId = R.drawable.img_backdrop_maestro,
                posterPath = "/kxj7rMco6RNYsVcNwuGAIlfWu64.jpg",
                backdropPath = "/et0G74BxoBgNQEZBkUcVhsgeRFF.jpg",
                director = "Bradley Cooper",
                trailerDuration = "2:24",
                isNowShowing = true,
                basePrice = 50000
            ),
            Movie(
                id = "the-fall-guy",
                title = "The Fall Guy",
                genre = "Action, Comedy",
                duration = "2h 06m",
                ageRating = "13+",
                rating = "7.1",
                synopsis = "A down-and-out stuntman must track down a missing movie star, solve a conspiracy and try to win back the love of his life.",
                posterResId = R.drawable.img_poster_fall_guy,
                backdropResId = R.drawable.img_poster_fall_guy,
                posterPath = "/e7olqFmzcIX5c23kX4zSmLPJi8c.jpg",
                backdropPath = "/e7olqFmzcIX5c23kX4zSmLPJi8c.jpg",
                director = "David Leitch",
                isComingSoon = true,
                isNowShowing = false,
                basePrice = 50000
            ),
            Movie(
                id = "kingdom-planet-apes",
                title = "Kingdom of the Planet of the Apes",
                genre = "Sci-Fi, Action, Adventure",
                duration = "2h 25m",
                ageRating = "13+",
                rating = "7.8",
                synopsis = "Several generations in the future following Caesar's reign, apes are now the dominant species and live harmoniously while humans have been reduced to living in the shadows.",
                posterResId = R.drawable.img_poster_kingdom_apes,
                backdropResId = R.drawable.img_poster_kingdom_apes,
                posterPath = "/gKkl37BQuKTanygYQG1pyYgLVgf.jpg",
                backdropPath = "/z121auT5q9Smum2KP05vm0am92P.jpg",
                director = "Wes Ball",
                isComingSoon = true,
                isNowShowing = false,
                basePrice = 50000
            )
        )

        // Cached list across repository instances
        val allMovies: MutableList<Movie> = ArrayList(initialMovies)
        private var hasFetchedFromTmdb = false
        private val listeners = mutableListOf<() -> Unit>()

        fun addOnMoviesUpdatedListener(listener: () -> Unit) {
            listeners.add(listener)
        }

        fun removeOnMoviesUpdatedListener(listener: () -> Unit) {
            listeners.remove(listener)
        }
    }

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
        fetchMoviesFromTmdb()
    }

    fun fetchMoviesFromTmdb() {
        if (hasFetchedFromTmdb) return
        hasFetchedFromTmdb = true

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val nowPlaying = fetchEndpoint("https://api.themoviedb.org/3/movie/now_playing?api_key=$TMDB_API_KEY&language=en-US&page=1", isNowShowing = true, isComingSoon = false)
                val upcoming = fetchEndpoint("https://api.themoviedb.org/3/movie/upcoming?api_key=$TMDB_API_KEY&language=en-US&page=1", isNowShowing = false, isComingSoon = true)
                val popular = fetchEndpoint("https://api.themoviedb.org/3/movie/popular?api_key=$TMDB_API_KEY&language=en-US&page=1", isNowShowing = true, isComingSoon = false)

                val newMovies = (nowPlaying + upcoming + popular)

                val existingIds = allMovies.map { it.id }.toSet()
                val existingTitles = allMovies.map { it.title.lowercase(Locale.ROOT) }.toSet()

                val toAdd = newMovies.filter {
                    !existingIds.contains(it.id) && !existingTitles.contains(it.title.lowercase(Locale.ROOT))
                }

                if (toAdd.isNotEmpty()) {
                    allMovies.addAll(toAdd)
                    Handler(Looper.getMainLooper()).post {
                        listeners.forEach { it.invoke() }
                    }
                }
            } catch (_: Exception) {
                // If offline, fallback to initialMovies cleanly
            }
        }
    }

    private fun fetchEndpoint(urlString: String, isNowShowing: Boolean, isComingSoon: Boolean): List<Movie> {
        val resultList = mutableListOf<Movie>()
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.requestMethod = "GET"

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()

            val root = JSONObject(sb.toString())
            val results = root.optJSONArray("results")
            if (results != null) {
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val movie = parseMovieJson(obj, isNowShowing, isComingSoon)
                    if (movie != null) {
                        resultList.add(movie)
                    }
                }
            }
        }
        conn.disconnect()
        return resultList
    }

    private fun parseMovieJson(obj: JSONObject, isNowShowing: Boolean, isComingSoon: Boolean): Movie? {
        val title = obj.optString("title", "")
        if (title.isEmpty()) return null

        val id = "tmdb-" + obj.optInt("id", (1000..9999).random())
        val posterPath = obj.optString("poster_path", "")
        val backdropPath = obj.optString("backdrop_path", "")
        val overview = obj.optString("overview", "A compelling cinematic experience.")
        val voteAvg = obj.optDouble("vote_average", 7.8)
        val ratingStr = String.format(Locale.US, "%.1f", voteAvg)

        val genreIds = obj.optJSONArray("genre_ids")
        val genres = mutableListOf<String>()
        if (genreIds != null) {
            for (i in 0 until minOf(genreIds.length(), 2)) {
                val gName = genreMap[genreIds.optInt(i)]
                if (gName != null) genres.add(gName)
            }
        }
        val genreString = if (genres.isNotEmpty()) genres.joinToString(", ") else "Drama, Action"

        val isAdult = obj.optBoolean("adult", false)
        val ageRating = if (isAdult) "21+" else if (genres.contains("Animation") || genres.contains("Family")) "SU" else "13+"

        return Movie(
            id = id,
            title = title,
            genre = genreString,
            duration = "2h 10m",
            ageRating = ageRating,
            rating = ratingStr,
            synopsis = overview,
            posterResId = R.drawable.img_poster_dune,
            backdropResId = R.drawable.img_backdrop_dune,
            posterPath = posterPath,
            backdropPath = backdropPath,
            director = "Hollywood & International",
            trailerDuration = "2:20",
            isHighlight = false,
            isNowShowing = isNowShowing,
            isComingSoon = isComingSoon,
            basePrice = 50000
        )
    }

    fun getMovieById(id: String): Movie? {
        return allMovies.find { it.id == id }
    }

    fun getCinemaById(id: String): Cinema? {
        return cinemas.find { it.id == id }
    }

    // Authentication
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
                savedCount = 6,
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

    // Saved Films
    fun isMovieSaved(movieId: String): Boolean {
        val set = prefs.getStringSet(KEY_SAVED_IDS, null) ?: setOf("dune-2", "oppenheimer", "killers-flower-moon")
        return set.contains(movieId)
    }

    fun toggleSaved(movieId: String): Boolean {
        val currentSet = prefs.getStringSet(KEY_SAVED_IDS, null)?.toMutableSet()
            ?: mutableSetOf("dune-2", "oppenheimer", "killers-flower-moon")
        val isNowSaved = if (currentSet.contains(movieId)) {
            currentSet.remove(movieId)
            false
        } else {
            currentSet.add(movieId)
            true
        }
        prefs.edit().putStringSet(KEY_SAVED_IDS, currentSet).apply()
        return isNowSaved
    }

    fun getSavedMovies(): List<Movie> {
        val set = prefs.getStringSet(KEY_SAVED_IDS, null) ?: setOf("dune-2", "oppenheimer", "killers-flower-moon")
        return allMovies.filter { set.contains(it.id) }
    }

    // Save latest booking
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
