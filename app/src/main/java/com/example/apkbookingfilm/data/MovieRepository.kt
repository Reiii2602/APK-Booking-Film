package com.example.apkbookingfilm.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.TmdbApi.Category
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.FoodItem
import com.example.apkbookingfilm.model.FoodOrderItem
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.PaymentMethod
import com.example.apkbookingfilm.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MovieRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_cinema_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_TIER = "user_tier"
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
        private const val KEY_USER_POINTS = "user_points_v3"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_SAVED_IDS = "saved_movie_ids_v2"
        private const val KEY_TICKET_COUNT = "ticket_count_v3"
        private const val KEY_LATEST_BOOKING_ID = "latest_booking_id_v3"
        private const val KEY_LATEST_MOVIE_ID = "latest_movie_id_v3"
        private const val KEY_LATEST_CINEMA_NAME = "latest_cinema_name_v3"
        private const val KEY_LATEST_SEATS = "latest_seats_v3"
        private const val KEY_LATEST_DATE = "latest_date_v3"
        private const val KEY_LATEST_TIME = "latest_time_v3"
        private const val KEY_LATEST_TOTAL = "latest_total_v3"
        private const val KEY_LATEST_PAYMENT_METHOD = "latest_payment_method_v3"
        private const val KEY_SELECTED_CITY = "selected_city"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SELECTED_PAYMENT_ID = "selected_payment_id"

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

        /** Katalog bioskop seluruh kota besar di Indonesia dengan koordinat real. */
        val allCinemas: List<Cinema> = listOf(
            // YOGYAKARTA
            Cinema("ambarrukmo-xxi", "Ambarrukmo XXI", "Plaza Ambarrukmo Lt. 3, Jl. Laksda Adisucipto, Yogyakarta", "1.2 km",
                listOf("PREMIERE", "IMAX WITH LASER", "DOLBY ATMOS"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Yogyakarta", "XXI", -7.7828, 110.4012),
            Cinema("empire-xxi", "Empire XXI Jogja", "Jl. Urip Sumoharjo No. 104, Gondokusuman, Yogyakarta", "2.4 km",
                listOf("PREMIERE", "DOLBY ATMOS", "2D REGULAR"), listOf("13:00", "15:45", "18:30", "21:15"),
                45000, "Yogyakarta", "XXI", -7.7832, 110.3886),
            Cinema("jogja-city-xxi", "Jogja City XXI", "Jogja City Mall Lt. 2, Jl. Magelang Km. 6, Yogyakarta", "3.5 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("12:45", "15:30", "18:15", "21:00"),
                50000, "Yogyakarta", "XXI", -7.7524, 110.3607),
            Cinema("cgv-pakuwon-jogja", "CGV Pakuwon Mall Jogja", "Pakuwon Mall Lt. 2, Jl. Ring Road Utara, Sleman, Yogyakarta", "4.2 km",
                listOf("4DX", "SPHERE X", "VELVET CLASS"), listOf("13:15", "16:00", "18:45", "21:30"),
                50000, "Yogyakarta", "CGV", -7.7586, 110.3991),
            Cinema("cinepolis-lippo-jogja", "Cinépolis Lippo Plaza Jogja", "Lippo Plaza Lt. 1, Jl. Laksda Adisucipto No. 32, Yogyakarta", "2.8 km",
                listOf("VIP CINEMA", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Yogyakarta", "Cinépolis", -7.7834, 110.3910),

            // SOLO (SURAKARTA)
            Cinema("solo-paragon-xxi", "Solo Paragon XXI", "Solo Paragon Mall Lt. 2, Jl. Yosodipuro No. 133, Solo", "1.1 km",
                listOf("PREMIERE", "DOLBY ATMOS", "2D REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Solo", "XXI", -7.5608, 110.8124),
            Cinema("solo-square-xxi", "Solo Square XXI", "Solo Square Lt. 3, Jl. Slamet Riyadi No. 451, Solo", "2.6 km",
                listOf("DOLBY ATMOS", "2D REGULAR"), listOf("13:00", "15:45", "18:30", "21:15"),
                45000, "Solo", "XXI", -7.5601, 110.7885),
            Cinema("the-park-solo-xxi", "The Park XXI Solo", "The Park Mall Lt. 2, Jl. Ir. Soekarno, Solo Baru", "4.5 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("12:45", "15:30", "18:15", "21:00"),
                50000, "Solo", "XXI", -7.6010, 110.8165),
            Cinema("cgv-transmart-solo", "CGV Transmart Solo", "Transmart Pabelan Lt. 3, Jl. A. Yani No. 234, Solo", "5.3 km",
                listOf("4DX", "GOLD CLASS", "REGULAR 2D"), listOf("13:15", "16:00", "18:45", "21:30"),
                45000, "Solo", "CGV", -7.5540, 110.7689),
            Cinema("cinepolis-solo", "Cinépolis Solo Grand Mall", "Solo Grand Mall Lt. 3, Jl. Slamet Riyadi No. 273, Solo", "1.8 km",
                listOf("MACRO XE", "VIP", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Solo", "Cinépolis", -7.5656, 110.8087),

            // JAKARTA
            Cinema("grand-indo-cgv", "CGV Grand Indonesia", "Grand Indonesia West Mall Lt. 8, Jl. MH Thamrin No. 1, Jakarta", "1.4 km",
                listOf("4DX", "SPHERE X", "GOLD CLASS"), listOf("12:00", "15:00", "18:15", "21:15"),
                65000, "Jakarta", "CGV", -6.1950, 106.8208),
            Cinema("plaza-senayan-xxi", "Plaza Senayan XXI", "Plaza Senayan Lt. 5, Jl. Asia Afrika No. 8, Jakarta", "3.1 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("12:30", "15:30", "18:30", "21:30"),
                60000, "Jakarta", "XXI", -6.2254, 106.7993),
            Cinema("gandaria-city-xxi", "Gandaria City XXI", "Gandaria City Lt. 2, Jl. Sultan Iskandar Muda, Jakarta", "4.8 km",
                listOf("PREMIERE", "IMAX WITH LASER", "DOLBY ATMOS"), listOf("13:00", "16:00", "19:00", "21:45"),
                60000, "Jakarta", "XXI", -6.2442, 106.7834),
            Cinema("kokas-xxi", "Kota Kasablanka XXI", "Kota Kasablanka Lt. 2, Jl. Casablanca Raya Kav. 88, Jakarta", "3.7 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:45", "15:45", "18:45", "21:30"),
                55000, "Jakarta", "XXI", -6.2241, 106.8424),
            Cinema("cinepolis-senayan-park", "Cinépolis Senayan Park", "Senayan Park Lt. 1, Jl. Gerbang Pemuda No. 3, Jakarta", "2.9 km",
                listOf("VIP LUXE", "MACRO XE", "REGULAR"), listOf("13:15", "16:15", "19:15", "22:00"),
                55000, "Jakarta", "Cinépolis", -6.2121, 106.8037),

            // BANDUNG
            Cinema("ciwalk-xxi", "Ciwalk XXI Bandung", "Cihampelas Walk Lt. 2, Jl. Cihampelas No. 160, Bandung", "1.5 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Bandung", "XXI", -6.8938, 107.6054),
            Cinema("tsm-bandung-xxi", "TSM XXI Bandung", "Trans Studio Mall Lt. 2, Jl. Gatot Subroto No. 289, Bandung", "3.4 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("13:00", "15:45", "18:30", "21:15"),
                55000, "Bandung", "XXI", -6.9255, 107.6365),
            Cinema("cgv-pvj", "CGV Paris Van Java", "Paris Van Java Lt. 1, Jl. Sukajadi No. 131-139, Bandung", "2.3 km",
                listOf("4DX", "VELVET CLASS", "STARIUM"), listOf("12:45", "15:30", "18:15", "21:00"),
                55000, "Bandung", "CGV", -6.8891, 107.5959),
            Cinema("cgv-23-paskal", "CGV 23 Paskal Bandung", "23 Paskal Shopping Center Lt. 3, Jl. Pasir Kaliki, Bandung", "2.1 km",
                listOf("4DX", "GOLD CLASS", "SPHERE X"), listOf("13:15", "16:00", "18:45", "21:30"),
                55000, "Bandung", "CGV", -6.9158, 107.5936),
            Cinema("cinepolis-istana-plaza", "Cinépolis Istana Plaza", "Istana Plaza Lt. 3, Jl. Pasirkaliki No. 121, Bandung", "1.9 km",
                listOf("MACRO XE", "VIP", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Bandung", "Cinépolis", -6.9062, 107.5967),

            // SURABAYA
            Cinema("tunjungan-5-xxi", "Tunjungan 5 XXI", "Tunjungan Plaza 5 Lt. 5, Jl. Basuki Rahmat, Surabaya", "1.3 km",
                listOf("PREMIERE", "IMAX WITH LASER", "DOLBY ATMOS"), listOf("12:30", "15:15", "18:00", "20:45"),
                55000, "Surabaya", "XXI", -7.2624, 112.7388),
            Cinema("pakuwon-sby-xxi", "Pakuwon Mall XXI Surabaya", "Pakuwon Mall Lt. 2, Jl. Puncak Indah Lontar No. 2, Surabaya", "5.8 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("13:00", "15:45", "18:30", "21:15"),
                55000, "Surabaya", "XXI", -7.2892, 112.6754),
            Cinema("ciputra-world-xxi", "Ciputra World XXI", "Ciputra World Lt. 4, Jl. Mayjen Sungkono No. 89, Surabaya", "3.9 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:45", "15:30", "18:15", "21:00"),
                50000, "Surabaya", "XXI", -7.2925, 112.7212),
            Cinema("cgv-marvell-sby", "CGV Marvell City Surabaya", "Marvell City Lt. 3, Jl. Ngagel No. 123, Surabaya", "2.7 km",
                listOf("4DX", "SPHERE X", "SWEETBOX"), listOf("13:15", "16:00", "18:45", "21:30"),
                50000, "Surabaya", "CGV", -7.2903, 112.7453),
            Cinema("cinepolis-cito", "Cinépolis City of Tomorrow", "CITO Mall Lt. 2, Jl. Ahmad Yani No. 288, Surabaya", "6.2 km",
                listOf("MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Surabaya", "Cinépolis", -7.3422, 112.7289),

            // SEMARANG
            Cinema("paragon-smg-xxi", "Paragon City XXI Semarang", "Paragon City Mall Lt. 3, Jl. Pemuda No. 118, Semarang", "1.2 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Semarang", "XXI", -6.9791, 110.4159),
            Cinema("dp-mall-xxi", "DP Mall XXI Semarang", "DP Mall Lt. 2, Jl. Pemuda No. 150, Semarang", "1.6 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("13:00", "15:45", "18:30", "21:15"),
                50000, "Semarang", "XXI", -6.9825, 110.4126),
            Cinema("citra-smg-xxi", "Citra XXI Semarang", "Mall Ciputra Lt. 3, Jl. Simpang Lima No. 1, Semarang", "2.4 km",
                listOf("DOLBY ATMOS", "REGULAR"), listOf("12:45", "15:30", "18:15", "21:00"),
                45000, "Semarang", "XXI", -6.9895, 110.4227),
            Cinema("cinepolis-java-smg", "Cinépolis Java Supermall", "Java Supermall Lt. 3, Jl. MT Haryono No. 992, Semarang", "3.8 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Semarang", "Cinépolis", -7.0017, 110.4337),

            // MALANG
            Cinema("araya-mlg-xxi", "Araya XXI Malang", "Plaza Araya Lt. 2, Jl. Blimbing Indah Megah, Malang", "3.4 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                45000, "Malang", "XXI", -7.9349, 112.6584),
            Cinema("transmart-mx-xxi", "Transmart MX Mall XXI", "Transmart MX Mall Lt. 3, Jl. Veteran No. 8, Malang", "1.8 km",
                listOf("DOLBY ATMOS", "REGULAR"), listOf("13:00", "15:45", "18:30", "21:15"),
                45000, "Malang", "XXI", -7.9571, 112.6177),
            Cinema("cgv-blimbing-mlg", "CGV Malang City Point", "Malang City Point Lt. 3, Jl. Terusan Dieng No. 32, Malang", "2.6 km",
                listOf("GOLD CLASS", "STARIUM", "REGULAR"), listOf("13:15", "16:00", "18:45", "21:30"),
                45000, "Malang", "CGV", -7.9718, 112.6104),
            Cinema("cinepolis-matos", "Cinépolis MATOS Malang", "Malang Town Square Lt. 3, Jl. Veteran No. 2, Malang", "1.5 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Malang", "Cinépolis", -7.9565, 112.6186),

            // MEDAN
            Cinema("centre-point-xxi", "Centre Point XXI Medan", "Mall Centre Point Lt. 3A, Jl. Jawa No. 8, Medan", "1.1 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Medan", "XXI", 3.5919, 98.6806),
            Cinema("delipark-xxi", "Delipark Podomoro XXI", "Delipark Mall Lt. 3, Jl. Guru Patimpus No. 1, Medan", "1.8 km",
                listOf("PREMIERE", "IMAX WITH LASER", "DOLBY ATMOS"), listOf("13:00", "15:45", "18:30", "21:15"),
                55000, "Medan", "XXI", 3.5966, 98.6746),
            Cinema("cgv-focal-medan", "CGV Focal Point Medan", "Focal Point Lt. 3, Jl. Ring Road Gagak Hitam, Medan", "4.5 km",
                listOf("4DX", "SPHERE X", "REGULAR"), listOf("13:15", "16:00", "18:45", "21:30"),
                45000, "Medan", "CGV", 3.5734, 98.6251),
            Cinema("cinepolis-sun-plaza", "Cinépolis Sun Plaza Medan", "Sun Plaza Lt. 4, Jl. KH Zainul Arifin No. 7, Medan", "2.1 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                50000, "Medan", "Cinépolis", 3.5835, 98.6716),

            // MAKASSAR
            Cinema("tsm-makassar-xxi", "TSM XXI Makassar", "Trans Studio Mall Lt. 2, Jl. HM Dg Patompo, Makassar", "2.9 km",
                listOf("PREMIERE", "IMAX", "DOLBY ATMOS"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Makassar", "XXI", -5.1636, 119.3948),
            Cinema("panakkukang-xxi", "Panakkukang XXI", "Mall Panakkukang Lt. 3, Jl. Boulevard, Makassar", "2.1 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("13:00", "15:45", "18:30", "21:15"),
                45000, "Makassar", "XXI", -5.1563, 119.4468),
            Cinema("cgv-panakkukang", "CGV Panakkukang Square", "Panakkukang Square Lt. 3, Jl. Adiyaksa, Makassar", "2.4 km",
                listOf("GOLD CLASS", "STARIUM", "REGULAR"), listOf("13:15", "16:00", "18:45", "21:30"),
                45000, "Makassar", "CGV", -5.1586, 119.4424),

            // DENPASAR / BALI
            Cinema("beachwalk-xxi", "Beachwalk XXI Kuta", "Beachwalk Shopping Center Lt. 2, Jl. Pantai Kuta, Bali", "2.5 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                55000, "Denpasar", "XXI", -8.7153, 115.1695),
            Cinema("level21-xxi", "Level 21 XXI Denpasar", "Level 21 Mall Lt. 3, Jl. Teuku Umar No. 1, Denpasar", "1.3 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("13:00", "15:45", "18:30", "21:15"),
                50000, "Denpasar", "XXI", -8.6762, 115.2094),
            Cinema("cinepolis-lippo-bali", "Cinépolis Lippo Mall Kuta", "Lippo Mall Kuta Lt. UG, Jl. Kartika Plaza, Bali", "3.4 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                50000, "Denpasar", "Cinépolis", -8.7327, 115.1669),

            // PALEMBANG
            Cinema("pim-xxi", "PIM XXI Palembang", "Palembang Indah Mall Lt. 3, Jl. Letkol Iskandar, Palembang", "1.4 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Palembang", "XXI", -2.9787, 104.7453),
            Cinema("cgv-soma-plg", "CGV Social Market Palembang", "Social Market Lt. 2, Jl. Veteran No. 999, Palembang", "2.0 km",
                listOf("4DX", "SWEETBOX", "REGULAR"), listOf("13:15", "16:00", "18:45", "21:30"),
                45000, "Palembang", "CGV", -2.9765, 104.7615),
            Cinema("cinepolis-palembang-icon", "Cinépolis Palembang Icon", "Palembang Icon Lt. 3, Jl. POM IX, Palembang", "1.9 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                50000, "Palembang", "Cinépolis", -2.9776, 104.7393),

            // BALIKPAPAN
            Cinema("ewalk-xxi", "E-Walk XXI Balikpapan", "E-Walk BSB Lt. 1, Jl. Jend. Sudirman, Balikpapan", "1.5 km",
                listOf("PREMIERE", "DOLBY ATMOS", "REGULAR"), listOf("12:30", "15:15", "18:00", "20:45"),
                50000, "Balikpapan", "XXI", -1.2747, 116.8559),
            Cinema("cgv-plaza-bpn", "CGV Plaza Balikpapan", "Plaza Balikpapan Lt. 2, Jl. Jend. Sudirman, Balikpapan", "2.3 km",
                listOf("4DX", "GOLD CLASS", "REGULAR"), listOf("13:15", "16:00", "18:45", "21:30"),
                50000, "Balikpapan", "CGV", -1.2757, 116.8427),
            Cinema("cinepolis-living-bpn", "Cinépolis Living Plaza Balikpapan", "Living Plaza Lt. 2, Jl. MT Haryono, Balikpapan", "3.1 km",
                listOf("VIP", "MACRO XE", "REGULAR"), listOf("13:30", "16:15", "19:00", "21:45"),
                45000, "Balikpapan", "Cinépolis", -1.2571, 116.8673)
        )

        /** Menu Makanan & Minuman Cinema Food. */
        val foodMenu: List<FoodItem> = listOf(
            FoodItem("combo-a", "COMBO A", "1 Large Salted Popcorn + 1 Coca-Cola Regular", 45000, "Combo", "BEST SELLER"),
            FoodItem("serbu-a", "SERBU A", "1 Large Caramel Popcorn + 1 XXI Java Tea", 50000, "Combo", "HEMAT"),
            FoodItem("combo-couple", "COMBO COUPLE", "1 Jumbo Mix Popcorn + 2 Soft Drinks", 75000, "Combo", "FAVORIT"),
            FoodItem("nachos-cheese", "NACHOS CHEESE", "Tortilla Chips renyah + Saus Keju Hangat", 40000, "Snack", "CRISPY"),
            FoodItem("beef-hotdog", "BEEF HOTDOG", "Sosis Sapi Panggang + Roti & Saus Mustard", 38000, "Snack", ""),
            FoodItem("french-fries", "FRENCH FRIES", "Kentang Goreng Renyah + Saus Sambal", 30000, "Snack", ""),
            FoodItem("iced-java-tea", "XXI JAVA TEA", "Iced Lemon / Lychee Tea Spesial 22oz", 25000, "Minuman", "SEGAR"),
            FoodItem("coca-cola", "COCA-COLA LARGE", "Minuman Bersoda Dingin 22oz", 22000, "Minuman", "")
        )

        /** Metode pembayaran E-Wallet & Transfer Bank lengkap dengan logo. */
        val paymentMethods: List<PaymentMethod> = listOf(
            PaymentMethod("ovo", "OVO", "E-Wallet", "0812 8899 4566", "PT Cinema Booking ID", R.drawable.ic_pay_ovo),
            PaymentMethod("gopay", "GoPay", "E-Wallet", "0812 8899 4566", "PT Cinema Booking ID", R.drawable.ic_pay_gopay),
            PaymentMethod("dana", "DANA", "E-Wallet", "0812 8899 4566", "PT Cinema Booking ID", R.drawable.ic_pay_dana),
            PaymentMethod("shopeepay", "ShopeePay", "E-Wallet", "0812 8899 4566", "PT Cinema Booking ID", R.drawable.ic_pay_shopeepay),
            PaymentMethod("qris", "QRIS", "Instant QR", "ID102030405060", "Cinema Booking QRIS", R.drawable.ic_pay_qris),
            PaymentMethod("bca", "Bank BCA", "Transfer Bank", "0373 8812 4566", "PT Cinema Booking Indonesia", R.drawable.ic_pay_bca),
            PaymentMethod("mandiri", "Bank Mandiri", "Transfer Bank", "1370 0192 8844", "PT Cinema Booking Indonesia", R.drawable.ic_pay_mandiri),
            PaymentMethod("bri", "Bank BRI", "Transfer Bank", "0029 0100 4455 501", "PT Cinema Booking Indonesia", R.drawable.ic_pay_bri),
            PaymentMethod("bni", "Bank BNI", "Transfer Bank", "0884 7711 2299", "PT Cinema Booking Indonesia", R.drawable.ic_pay_bni)
        )

        // Keranjang makanan global antar halaman (Home -> SeatSelection -> Payment)
        private val foodCart: MutableMap<String, Int> = linkedMapOf()
        private val foodCartListeners = mutableListOf<() -> Unit>()
        private val profileListeners = mutableListOf<() -> Unit>()

        // Koordinat user hasil GPS scan (opsional)
        @Volatile private var userLat: Double? = null
        @Volatile private var userLng: Double? = null

        // ---- State global (hanya diubah di main thread) ----
        val allMovies: MutableList<Movie> = ArrayList(offlineMovies)
        private val categoryIds: MutableMap<Category, List<String>> = mutableMapOf()
        private var cacheLoaded = false
        private var lastFetchMs = 0L
        @Volatile var isLoading = false
            private set
        private val listeners = mutableListOf<() -> Unit>()
        private val cityListeners = mutableListOf<() -> Unit>()
        private val executor = Executors.newFixedThreadPool(3)
        private val mainHandler = Handler(Looper.getMainLooper())

        fun addOnMoviesUpdatedListener(listener: () -> Unit) { listeners.add(listener) }
        fun removeOnMoviesUpdatedListener(listener: () -> Unit) { listeners.remove(listener) }

        fun addOnCityChangedListener(listener: () -> Unit) { cityListeners.add(listener) }
        fun removeOnCityChangedListener(listener: () -> Unit) { cityListeners.remove(listener) }

        fun addOnFoodCartChangedListener(listener: () -> Unit) { foodCartListeners.add(listener) }
        fun removeOnFoodCartChangedListener(listener: () -> Unit) { foodCartListeners.remove(listener) }

        fun addOnProfileChangedListener(listener: () -> Unit) { profileListeners.add(listener) }
        fun removeOnProfileChangedListener(listener: () -> Unit) { profileListeners.remove(listener) }

        private fun notifyListeners() {
            listeners.toList().forEach { it.invoke() }
        }

        private fun notifyCityListeners() {
            cityListeners.toList().forEach { it.invoke() }
        }

        private fun notifyFoodCartListeners() {
            foodCartListeners.toList().forEach { it.invoke() }
        }

        private fun notifyProfileListeners() {
            profileListeners.toList().forEach { it.invoke() }
        }

        fun formatRupiah(amount: Int): String {
            val nf = NumberFormat.getIntegerInstance(Locale("in", "ID"))
            return "Rp${nf.format(amount)}"
        }

        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        /** Tambah / perbarui film (mis. hasil search atau halaman berikutnya). Main thread. */
        fun upsertMovies(list: List<Movie>) {
            for (m in list) {
                val idx = allMovies.indexOfFirst { it.id == m.id }
                if (idx >= 0) {
                    if (!allMovies[idx].hasFullDetail || m.hasFullDetail) allMovies[idx] = m
                } else {
                    allMovies.add(m)
                }
            }
        }
    }

    private val appContext = context.applicationContext

    val movies: List<Movie> get() = allMovies

    val cinemas: List<Cinema> get() = getCinemasForCity(getSelectedCity())

    init {
        loadCacheIfNeeded()
        fetchMoviesFromTmdb()
    }

    // ================================================================ Kota & Bioskop Indonesia

    fun getAvailableCities(): List<String> = allCinemas.map { it.city }.distinct()

    fun getSelectedCity(): String = prefs.getString(KEY_SELECTED_CITY, "Yogyakarta") ?: "Yogyakarta"

    fun setSelectedCity(city: String) {
        val valid = getAvailableCities().firstOrNull { it.equals(city, ignoreCase = true) } ?: "Yogyakarta"
        prefs.edit().putString(KEY_SELECTED_CITY, valid).apply()
        notifyCityListeners()
    }

    fun getCinemasForCity(city: String = getSelectedCity()): List<Cinema> {
        val list = allCinemas.filter { it.city.equals(city, ignoreCase = true) }
            .ifEmpty { allCinemas.filter { it.city == "Yogyakarta" } }
        val lat = userLat
        val lng = userLng
        return if (lat != null && lng != null) {
            list.map { c ->
                val km = haversineKm(lat, lng, c.lat, c.lng)
                c.copy(distance = String.format(Locale.US, "%.1f km", km))
            }.sortedBy {
                it.distance.removeSuffix(" km").toDoubleOrNull() ?: 999.0
            }
        } else {
            list
        }
    }

    /** Deteksi kota terdekat dari koordinat GPS user. */
    fun updateUserLocation(lat: Double, lng: Double): String {
        userLat = lat
        userLng = lng
        val nearest = allCinemas.minByOrNull { haversineKm(lat, lng, it.lat, it.lng) }
        val detectedCity = nearest?.city ?: "Yogyakarta"
        setSelectedCity(detectedCity)
        return detectedCity
    }

    fun getCinemaById(id: String): Cinema? {
        return allCinemas.find { it.id == id }
    }

    // ================================================================ Food & Drinks Cart

    fun getFoodQuantity(foodId: String): Int = foodCart[foodId] ?: 0

    fun setFoodQuantity(foodId: String, qty: Int) {
        if (qty <= 0) foodCart.remove(foodId) else foodCart[foodId] = qty
        notifyFoodCartListeners()
    }

    fun addFoodItem(foodId: String) {
        setFoodQuantity(foodId, getFoodQuantity(foodId) + 1)
    }

    fun getFoodCartOrders(): List<FoodOrderItem> {
        return foodMenu.mapNotNull { item ->
            val q = foodCart[item.id] ?: 0
            if (q > 0) FoodOrderItem(item, q) else null
        }
    }

    fun getFoodCartCount(): Int = foodCart.values.sum()

    fun getFoodCartTotal(): Int = getFoodCartOrders().sumOf { it.subtotal }

    fun clearFoodCart() {
        foodCart.clear()
        notifyFoodCartListeners()
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
            merged[m.id] = if (old != null && old.hasFullDetail) old.copy(
                rating = m.rating, voteCount = m.voteCount,
                isNowShowing = m.isNowShowing, isComingSoon = m.isComingSoon
            ) else m
        }
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

    // ================================================================ Authentication & Profile (Starts from 0)
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUser(): User {
        val isLoggedIn = isLoggedIn()
        return if (isLoggedIn) {
            val name = prefs.getString(KEY_USER_NAME, "Cinema User") ?: "Cinema User"
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val authProvider = prefs.getString(KEY_AUTH_PROVIDER, "Google") ?: "Google"
            val tier = prefs.getString(KEY_USER_TIER, "MEMBER BARU • SINCE 2026") ?: "MEMBER BARU • SINCE 2026"
            val avatarUri = prefs.getString(KEY_USER_AVATAR_URI, "") ?: ""
            val points = prefs.getInt(KEY_USER_POINTS, 0)
            val initials = name.split(" ").filter { it.isNotEmpty() }
                .map { it.first().uppercase() }
                .take(2).joinToString("")
            User(
                name = name,
                email = email,
                avatarInitials = if (initials.isNotEmpty()) initials else "CU",
                avatarUri = avatarUri,
                moviesCount = prefs.getInt(KEY_TICKET_COUNT, 0),
                savedCount = getSavedIds().size,
                reviewsCount = 0,
                points = points,
                membershipTier = tier,
                authProvider = authProvider,
                isLoggedIn = true
            )
        } else {
            User(
                name = "Guest User",
                email = "",
                avatarInitials = "GU",
                avatarUri = "",
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

    fun login(name: String = "Cinema User", email: String = "user@cinemabooking.id", provider: String = "Google") {
        val existingName = prefs.getString(KEY_USER_NAME, null)
        val existingEmail = prefs.getString(KEY_USER_EMAIL, null)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            if (existingName.isNullOrEmpty() || name != "Daffa Fadhul Rahman") {
                putString(KEY_USER_NAME, name)
            }
            if (existingEmail.isNullOrEmpty() || email != "daffasigma@gmail.com") {
                putString(KEY_USER_EMAIL, email)
            }
            putString(KEY_AUTH_PROVIDER, provider)
            apply()
        }
        notifyProfileListeners()
    }

    fun updateProfile(name: String, email: String, tier: String, avatarUri: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name.trim().ifEmpty { "Cinema User" })
            putString(KEY_USER_EMAIL, email.trim())
            putString(KEY_USER_TIER, tier.trim().ifEmpty { "MEMBER BARU • SINCE 2026" })
            if (avatarUri != null) {
                putString(KEY_USER_AVATAR_URI, avatarUri)
            }
            apply()
        }
        notifyProfileListeners()
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        notifyProfileListeners()
    }

    // ================================================================ Notifications & Payment Method

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getSelectedPaymentMethod(): PaymentMethod {
        val id = prefs.getString(KEY_SELECTED_PAYMENT_ID, "bca") ?: "bca"
        return paymentMethods.find { it.id == id } ?: paymentMethods.first()
    }

    fun setSelectedPaymentMethod(id: String) {
        prefs.edit().putString(KEY_SELECTED_PAYMENT_ID, id).apply()
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
        notifyProfileListeners()
        return isNowSaved
    }

    fun getSavedMovies(): List<Movie> {
        val set = getSavedIds()
        return allMovies.filter { set.contains(it.id) }
    }

    // ================================================================ Booking
    fun saveBooking(booking: Booking) {
        val ticketsCount = prefs.getInt(KEY_TICKET_COUNT, 0) + booking.seats.size
        val currentPoints = prefs.getInt(KEY_USER_POINTS, 0) + (booking.totalPrice / 1000)
        prefs.edit().apply {
            putString(KEY_LATEST_BOOKING_ID, booking.bookingId)
            putString(KEY_LATEST_MOVIE_ID, booking.movie.id)
            putString(KEY_LATEST_CINEMA_NAME, booking.cinema.name)
            putString(KEY_LATEST_SEATS, booking.seats.joinToString(", "))
            putString(KEY_LATEST_DATE, booking.date)
            putString(KEY_LATEST_TIME, booking.time)
            putInt(KEY_LATEST_TOTAL, booking.totalPrice)
            putString(KEY_LATEST_PAYMENT_METHOD, booking.paymentMethod)
            putInt(KEY_TICKET_COUNT, ticketsCount)
            putInt(KEY_USER_POINTS, currentPoints)
            apply()
        }
        upsertMovies(listOf(booking.movie))
        saveCache()
        notifyProfileListeners()
    }

    fun getLatestBooking(): Booking? {
        val bookingId = prefs.getString(KEY_LATEST_BOOKING_ID, null) ?: return null
        val movieId = prefs.getString(KEY_LATEST_MOVIE_ID, null) ?: return null
        val movie = getMovieById(movieId) ?: allMovies.firstOrNull() ?: return null
        val cinemaName = prefs.getString(KEY_LATEST_CINEMA_NAME, "Ambarrukmo XXI") ?: "Ambarrukmo XXI"
        val cinema = allCinemas.find { it.name == cinemaName } ?: cinemas.first()
        val seatsStr = prefs.getString(KEY_LATEST_SEATS, "") ?: ""
        val seats = if (seatsStr.isNotEmpty()) seatsStr.split(", ") else emptyList()
        val date = prefs.getString(KEY_LATEST_DATE, "Fri, 06 Nov") ?: "Fri, 06 Nov"
        val time = prefs.getString(KEY_LATEST_TIME, "19:30") ?: "19:30"
        val total = prefs.getInt(KEY_LATEST_TOTAL, 0)
        val payMethod = prefs.getString(KEY_LATEST_PAYMENT_METHOD, "Bank BCA") ?: "Bank BCA"

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
            paymentMethod = payMethod,
            isPaid = true
        )
    }
}
