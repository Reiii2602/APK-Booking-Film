package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.DateAdapter
import com.example.apkbookingfilm.adapter.DateItem
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityMovieDetailBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

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

        setupViews()
        setupDates()
        setupCinemaShowtimes()
        setupListeners()
    }

    private fun setupViews() {
        val movie = currentMovie ?: return
        TmdbImageLoader.loadBackdrop(binding.ivDetailPoster, movie)

        binding.tvMovieTitle.text = movie.title
        binding.tvGenreDuration.text = "${movie.ageRating} • 2024 • ${movie.genre} • ${movie.duration}"
        binding.tvRatingScore.text = "${movie.rating} / 10"
        binding.tvSynopsis.text = movie.synopsis
        binding.tvDirector.text = "DIRECTOR: ${movie.director}"
        binding.tvBottomPrice.text = "Rp50.000/adult"
        binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
        binding.tvBottomShowtimeVenue.text = "${selectedCinema?.name ?: "Ambarrukmo XXI"} • IMAX"

        updateBookmarkIcon()
    }

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
            Toast.makeText(this, "Playing Trailer (2:24)", Toast.LENGTH_SHORT).show()
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
