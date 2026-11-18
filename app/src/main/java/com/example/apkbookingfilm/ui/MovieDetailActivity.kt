package com.example.apkbookingfilm.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.CastAdapter
import com.example.apkbookingfilm.adapter.DateAdapter
import com.example.apkbookingfilm.adapter.DateItem
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityMovieDetailBinding
import com.example.apkbookingfilm.databinding.ItemShowtimeCinemaBinding
import com.example.apkbookingfilm.databinding.ItemShowtimeSlotBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var repository: MovieRepository

    private var currentMovie: Movie? = null
    private var selectedCinema: Cinema? = null
    private var selectedDate: String = "Fri, 06 Nov"
    private var selectedTime: String = ""
    private var selectedFormatFilter: String = "All formats"

    private val castAdapter = CastAdapter(emptyList())

    private data class SlotViewRef(
        val cinema: Cinema,
        val time: String,
        val binding: ItemShowtimeSlotBinding,
        val card: MaterialCardView
    )
    private val slotViewRefs = mutableListOf<SlotViewRef>()

    private val onCityChangedListener = {
        if (!isFinishing && !isDestroyed) {
            renderCinemasForSelectedCity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        MovieRepository.addOnCityChangedListener(onCityChangedListener)

        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: "dune-2"
        currentMovie = repository.getMovieById(movieId) ?: repository.movies[0]

        binding.rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCast.adapter = castAdapter

        setupDates()
        renderCinemasForSelectedCity()
        setupViews()
        setupListeners()
        loadFullDetail(movieId)
    }

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
        binding.tvTrailerLabel.text = if (movie.trailerKey.isNotEmpty()) "PUTAR CUPLIKAN" else "CUPLIKAN"

        if (movie.cast.isNotEmpty()) {
            binding.tvCastEmpty.visibility = View.GONE
            binding.rvCast.visibility = View.VISIBLE
            castAdapter.updateList(movie.cast)
        }

        updateBottomBar()
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
            updateBottomBar()
        }
    }

    private fun renderCinemasForSelectedCity() {
        val city = repository.getSelectedCity()
        binding.tvCityDetail.text = city
        binding.tvFormatFilterDetail.text = "$selectedFormatFilter ⌄"

        val allCityCinemas = repository.getCinemasForCity(city)
        val filtered = if (selectedFormatFilter == "All formats") {
            allCityCinemas
        } else {
            allCityCinemas.filter { c ->
                c.formats.any { it.contains(selectedFormatFilter, ignoreCase = true) }
            }.ifEmpty { allCityCinemas }
        }

        // Pastikan cinema & jam default terpilih valid
        if (selectedCinema == null || filtered.none { it.id == selectedCinema?.id }) {
            selectedCinema = filtered.firstOrNull()
        }
        val activeCinema = selectedCinema
        if (activeCinema != null && (selectedTime.isEmpty() || !activeCinema.showtimes.contains(selectedTime))) {
            selectedTime = activeCinema.showtimes.getOrNull(1) ?: activeCinema.showtimes.firstOrNull() ?: "19:30"
        }

        val container = binding.layoutCinemaShowtimes
        container.removeAllViews()
        slotViewRefs.clear()

        val inflater = LayoutInflater.from(this)
        filtered.forEach { cinema ->
            val cardBinding = ItemShowtimeCinemaBinding.inflate(inflater, container, false)
            cardBinding.tvCinemaName.text = cinema.name
            cardBinding.tvCinemaDistance.text = cinema.distance
            cardBinding.tvCinemaAddress.text = cinema.location
            cardBinding.tvCinemaFormatPrimary.text = cinema.formats.getOrNull(0) ?: "REGULAR"
            val secFormat = cinema.formats.getOrNull(1)
            if (secFormat != null) {
                cardBinding.tvCinemaFormatSecondary.visibility = View.VISIBLE
                cardBinding.tvCinemaFormatSecondary.text = secFormat
            } else {
                cardBinding.tvCinemaFormatSecondary.visibility = View.GONE
            }

            val slotsRow = cardBinding.layoutShowtimeSlots
            slotsRow.removeAllViews()
            val timesToShow = cinema.showtimes.take(4)
            timesToShow.forEachIndexed { idx, timeStr ->
                val slotBinding = ItemShowtimeSlotBinding.inflate(inflater, slotsRow, false)
                val lp = slotBinding.root.layoutParams as LinearLayout.LayoutParams
                if (idx == timesToShow.lastIndex) lp.marginEnd = 0
                slotBinding.root.layoutParams = lp

                slotBinding.tvSlotTime.text = timeStr
                val ref = SlotViewRef(cinema, timeStr, slotBinding, cardBinding.cardCinemaItem)
                slotViewRefs.add(ref)

                slotBinding.root.setOnClickListener {
                    selectedCinema = cinema
                    selectedTime = timeStr
                    refreshShowtimeSelectionVisuals()
                    updateBottomBar()
                }
                slotsRow.addView(slotBinding.root)
            }

            container.addView(cardBinding.root)
        }

        refreshShowtimeSelectionVisuals()
        updateBottomBar()
    }

    private fun refreshShowtimeSelectionVisuals() {
        val white = ContextCompat.getColor(this, R.color.white)
        val peach = ContextCompat.getColor(this, R.color.peach_bg)
        val chipText = ContextCompat.getColor(this, R.color.chip_text)
        val muted = ContextCompat.getColor(this, R.color.text_muted)
        val maroon = ContextCompat.getColor(this, R.color.maroon_primary)
        val divider = ContextCompat.getColor(this, R.color.divider_line)
        val density = resources.displayMetrics.density

        slotViewRefs.forEach { ref ->
            val isSelected = ref.cinema.id == selectedCinema?.id && ref.time == selectedTime
            val isCinemaSelected = ref.cinema.id == selectedCinema?.id
            val priceStr = MovieRepository.formatRupiah(ref.cinema.ticketPrice)

            if (isSelected) {
                ref.binding.root.setBackgroundResource(R.drawable.bg_chip_selected)
                ref.binding.tvSlotTime.setTextColor(white)
                ref.binding.tvSlotSub.text = "✓ SELECTED"
                ref.binding.tvSlotSub.setTextColor(peach)
                ref.binding.tvSlotSub.paint.isFakeBoldText = true
            } else {
                ref.binding.root.setBackgroundResource(R.drawable.bg_chip_unselected)
                ref.binding.tvSlotTime.setTextColor(chipText)
                ref.binding.tvSlotSub.text = priceStr
                ref.binding.tvSlotSub.setTextColor(muted)
                ref.binding.tvSlotSub.paint.isFakeBoldText = false
            }

            ref.card.strokeColor = if (isCinemaSelected) maroon else divider
            ref.card.strokeWidth = ((if (isCinemaSelected) 1.5f else 1f) * density).toInt()
        }
    }

    private fun updateBottomBar() {
        val cinema = selectedCinema ?: return
        val fmt = cinema.formats.firstOrNull() ?: "2D"
        binding.tvBottomShowtimeDate.text = "$selectedDate • $selectedTime"
        binding.tvBottomShowtimeVenue.text = "${cinema.name} • $fmt"
        binding.tvBottomPrice.text = "${MovieRepository.formatRupiah(cinema.ticketPrice)}/adult"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutCityDetail.setOnClickListener {
            CityPickerDialog {
                renderCinemasForSelectedCity()
            }.show(supportFragmentManager, CityPickerDialog.TAG)
        }

        binding.tvFormatFilterDetail.setOnClickListener {
            val formats = arrayOf("All formats", "IMAX", "PREMIERE", "DOLBY ATMOS", "4DX", "REGULAR")
            val checked = formats.indexOf(selectedFormatFilter).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Pilih Format Studio")
                .setSingleChoiceItems(formats, checked) { dialog, which ->
                    selectedFormatFilter = formats[which]
                    renderCinemasForSelectedCity()
                    dialog.dismiss()
                }
                .show()
        }

        binding.btnTrailer.setOnClickListener {
            val key = currentMovie?.trailerKey.orEmpty()
            if (key.isEmpty()) {
                Toast.makeText(this, "Cuplikan trailer belum tersedia untuk film ini", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playInlineTrailer(key)
        }

        binding.btnCloseTrailer.setOnClickListener {
            closeInlineTrailer()
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
                putExtra(SeatSelectionActivity.EXTRA_TIME, selectedTime.ifEmpty { "19:30" })
            }
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun playInlineTrailer(youtubeKey: String) {
        binding.layoutTrailerPlayer.visibility = View.VISIBLE
        binding.progressTrailer.visibility = View.VISIBLE

        val webView = binding.webTrailer
        webView.setBackgroundColor(Color.BLACK)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressTrailer.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Tetap tahan navigasi di dalam player in-app
                return false
            }
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
              <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: 0; }
              </style>
            </head>
            <body>
              <iframe
                src="https://www.youtube-nocookie.com/embed/$youtubeKey?autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1"
                allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                allowfullscreen>
              </iframe>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "https://www.themoviedb.org",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun closeInlineTrailer() {
        binding.webTrailer.stopLoading()
        binding.webTrailer.loadUrl("about:blank")
        binding.layoutTrailerPlayer.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        if (binding.layoutTrailerPlayer.visibility == View.VISIBLE) {
            binding.webTrailer.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MovieRepository.removeOnCityChangedListener(onCityChangedListener)
        binding.webTrailer.destroy()
    }

    companion object {
        const val EXTRA_MOVIE_ID = "extra_movie_id"
    }
}
