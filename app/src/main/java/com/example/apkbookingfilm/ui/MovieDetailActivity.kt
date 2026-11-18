package com.example.apkbookingfilm.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.CastAdapter
import com.example.apkbookingfilm.adapter.DateAdapter
import com.example.apkbookingfilm.adapter.DateItem
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityMovieDetailBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader
import java.util.Locale

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var repository: MovieRepository

    private var currentMovie: Movie? = null
    private var selectedCinema: Cinema? = null
    private var selectedDate: String = "Fri, 06 Nov"
    private var selectedTime: String = "19:30"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: "dune-2"
        currentMovie = repository.getMovieById(movieId) ?: repository.movies[0]
        selectedCinema = repository.cinemas[0]

        binding.rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCast.adapter = castAdapter

        setupViews()
        setupDates()
        setupCinemaShowtimes()
        setupListeners()
        loadFullDetail(movieId)
    }

    private val castAdapter = CastAdapter(emptyList())

    private fun loadFullDetail(movieId: String) {
        binding.progressDetail.visibility = View.VISIBLE
        repository.fetchMovieDetail(currentMovie?.id ?: movieId) { detail ->
            if (isFinishing || isDestroyed) return@fetchMovieDetail
            binding.progressDetail.visibility = View.GONE
            if (detail != null) {
                currentMovie = detail
                setupViews()
            }
            if (currentMovie?.cast.isNullOrEmpty()) {
                binding.tvCastEmpty.visibility = View.VISIBLE
                binding.tvCastEmpty.text = if (detail?.hasFullDetail == true)
                    "Data pemeran tidak tersedia." else "Gagal memuat pemeran. Periksa koneksi internet."
            }
        }
    }

    private fun setupViews() {
        val movie = currentMovie ?: return
        TmdbImageLoader.loadBackdrop(binding.ivDetailPoster, movie)

        binding.tvMovieTitle.text = movie.title
        val meta = listOf(movie.ageRating, movie.releaseYear, movie.genre, movie.duration)
            .filter { it.isNotEmpty() && it != "—" }
            .distinct()
        binding.tvGenreDuration.text = meta.joinToString(" • ")
        val votes = if (movie.voteCount > 0) "  (${formatVotes(movie.voteCount)})" else ""
        binding.tvRatingScore.text = "${movie.rating} / 10$votes"
        binding.tvSynopsis.text = movie.synopsis
        binding.tvDirector.text = "DIRECTOR: ${movie.director.ifEmpty { "-" }}"
        binding.tvStatusLabel.text = if (movie.isComingSoon) "COMING SOON" else "NOW SHOWING"
        binding.tvTagline.visibility = if (movie.tagline.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvTagline.text = "\u201C${movie.tagline}\u201D"
        binding.tvTrailerLabel.text = if (movie.trailerKey.isNotEmpty()) "TONTON TRAILER" else "TRAILER"
        binding.tvBottomPrice.text = "Rp50.000/adult"
        binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
        binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name ?: "Ambarrukmo XXI"} • IMAX"

        if (movie.cast.isNotEmpty()) {
            binding.tvCastEmpty.visibility = View.GONE
            binding.rvCast.visibility = View.VISIBLE
            castAdapter.updateList(movie.cast)
        }

        updateBookmarkIcon()
    }

    private fun formatVotes(count: Int): String = when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    } + " votes"

    private fun updateBookmarkIcon() {
        val movie = currentMovie ?: return
        val isSaved = repository.isMovieSaved(movie.id)
        if (isSaved) {
            binding.btnBookmark.setColorFilter(getColor(R.color.maroon_primary))
        } else {
            binding.btnBookmark.setColorFilter(getColor(R.color.white))
        }
    }

    private fun setupDates() {
        // As shown in Image 3: FRI 06 TODAY, SAT 07 NOV, SUN 08 NOV, MON 09 NOV, TUE 10 NOV
        val dateItems = listOf(
            DateItem("FRI", "06", "Fri, 06 Nov", "TODAY"),
            DateItem("SAT", "07", "Sat, 07 Nov", "NOV"),
            DateItem("SUN", "08", "Sun, 08 Nov", "NOV"),
            DateItem("MON", "09", "Mon, 09 Nov", "NOV"),
            DateItem("TUE", "10", "Tue, 10 Nov", "NOV")
        )
        selectedDate = dateItems[0].fullDate

        binding.rvDateSelector.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDateSelector.adapter = DateAdapter(dateItems, selectedIndex = 0) { item ->
            selectedDate = item.fullDate
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
        }
    }

    private fun setupCinemaShowtimes() {
        binding.time11.setOnClickListener {
            selectedCinema = repository.cinemas[0]
            selectedTime = "16:00"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • IMAX"
        }

        binding.time12.setOnClickListener {
            selectedCinema = repository.cinemas[0]
            selectedTime = "19:30"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • IMAX"
        }

        binding.time13.setOnClickListener {
            selectedCinema = repository.cinemas[0]
            selectedTime = "22:45"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • IMAX"
        }

        binding.time21.setOnClickListener {
            selectedCinema = repository.cinemas[1]
            selectedTime = "15:15"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • RPX"
        }

        binding.time22.setOnClickListener {
            selectedCinema = repository.cinemas[1]
            selectedTime = "18:45"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • RPX"
        }

        binding.time23.setOnClickListener {
            selectedCinema = repository.cinemas[1]
            selectedTime = "21:50"
            binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
            binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name} • RPX"
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnTrailer.setOnClickListener {
            val key = currentMovie?.trailerKey.orEmpty()
            if (key.isEmpty()) {
                Toast.makeText(this, "Trailer belum tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$key")))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key")))
            }
        }

        binding.tvFullDetails.setOnClickListener {
            val tmdbId = currentMovie?.tmdbId ?: 0
            if (tmdbId > 0) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/movie/$tmdbId")))
                } catch (_: ActivityNotFoundException) {
                }
            }
        }

        binding.btnBookmark.setOnClickListener {
            val movie = currentMovie ?: return@setOnClickListener
            val isNowSaved = repository.toggleSaved(movie.id)
            updateBookmarkIcon()
            val msg = if (isNowSaved) "Added to saved films" else "Removed from saved films"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnChooseSeats.setOnClickListener {
            val movie = currentMovie ?: return@setOnClickListener
            val cinema = selectedCinema ?: repository.cinemas[0]
            val intent = Intent(this, SeatSelectionActivity::class.java).apply {
                putExtra(SeatSelectionActivity.EXTRA_MOVIE, movie)
                putExtra(SeatSelectionActivity.EXTRA_CINEMA, cinema)
                putExtra(SeatSelectionActivity.EXTRA_DATE, selectedDate)
                putExtra(SeatSelectionActivity.EXTRA_TIME, selectedTime)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_MOVIE_ID = "extra_movie_id"
    }
}
