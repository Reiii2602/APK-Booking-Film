package com.example.apkbookingfilm.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
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
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast
import com.example.apkbookingfilm.util.TmdbImageLoader
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var repository: MovieRepository

    private var currentMovie: Movie? = null
    private var selectedCinema: Cinema? = null
    private var selectedDate: String = ""
    private var isSelectedDateToday: Boolean = true
    private var selectedTime: String = ""
    private var selectedFormatFilter: String = "All formats"

    private var preloadedTrailerKey: String = ""
    private var isTrailerWebLoaded = false
    private var wantPlayWhenLoaded = false
    private var isTrailerPlaying = false
    private var isTrailerMuted = false

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
                    AppLanguage.tr("Cast data not available.", "Data pemeran tidak tersedia.")
                else
                    AppLanguage.tr("Failed to load cast. Check your internet connection.", "Gagal memuat pemeran. Periksa koneksi internet.")
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
        binding.tvStatusLabel.text = if (movie.isComingSoon) {
            AppLanguage.tr("COMING SOON", "SEGERA TAYANG")
        } else {
            AppLanguage.tr("NOW SHOWING", "SEDANG TAYANG")
        }
        binding.tvTagline.visibility = if (movie.tagline.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvTagline.text = "\u201C${movie.tagline}\u201D"
        binding.tvTrailerLabel.text = if (movie.trailerKey.isNotEmpty()) {
            AppLanguage.tr("PLAY TRAILER", "PUTAR CUPLIKAN")
        } else {
            AppLanguage.tr("TRAILER", "CUPLIKAN")
        }

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
        val now = Calendar.getInstance()
        val todayYear = now.get(Calendar.YEAR)
        val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)

        val monthYearFmt = SimpleDateFormat("MMM yyyy", Locale.US)
        binding.tvShowtimeMonthYear.text = monthYearFmt.format(now.time).uppercase(Locale.US)

        // Hitung hari Senin s/d Minggu pada minggu berjalan secara realtime
        val weekCal = (now.clone() as Calendar).apply {
            val dow = get(Calendar.DAY_OF_WEEK)
            val offsetFromMonday = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
            add(Calendar.DAY_OF_YEAR, offsetFromMonday)
        }

        val monthShortFmt = SimpleDateFormat("MMM", Locale.US)
        val dateItems = mutableListOf<DateItem>()
        var todayIndex = 0

        for (i in 0 until 7) {
            val calYear = weekCal.get(Calendar.YEAR)
            val calDayOfYear = weekCal.get(Calendar.DAY_OF_YEAR)
            val isPast = calYear < todayYear || (calYear == todayYear && calDayOfYear < todayDayOfYear)
            val isToday = calYear == todayYear && calDayOfYear == todayDayOfYear
            if (isToday) todayIndex = i

            val dow = weekCal.get(Calendar.DAY_OF_WEEK)
            val (dayUpper, dayTitle) = when (dow) {
                Calendar.MONDAY -> AppLanguage.tr("MON", "SEN") to AppLanguage.tr("Mon", "Sen")
                Calendar.TUESDAY -> AppLanguage.tr("TUE", "SEL") to AppLanguage.tr("Tue", "Sel")
                Calendar.WEDNESDAY -> AppLanguage.tr("WED", "RAB") to AppLanguage.tr("Wed", "Rab")
                Calendar.THURSDAY -> AppLanguage.tr("THU", "KAM") to AppLanguage.tr("Thu", "Kam")
                Calendar.FRIDAY -> AppLanguage.tr("FRI", "JUM") to AppLanguage.tr("Fri", "Jum")
                Calendar.SATURDAY -> AppLanguage.tr("SAT", "SAB") to AppLanguage.tr("Sat", "Sab")
                else -> AppLanguage.tr("SUN", "MIN") to AppLanguage.tr("Sun", "Min")
            }

            val dayNumInt = weekCal.get(Calendar.DAY_OF_MONTH)
            val dayNumStr = String.format(Locale.US, "%02d", dayNumInt)
            val monthStr = monthShortFmt.format(weekCal.time)
            val fullDate = "$dayTitle, $dayNumStr $monthStr"
            val dayLabel = if (isToday) {
                AppLanguage.tr("TODAY", "HARI INI")
            } else {
                monthStr.uppercase(Locale.US)
            }

            dateItems.add(
                DateItem(
                    dayName = dayUpper,
                    dayNumber = dayNumStr,
                    fullDate = fullDate,
                    dayLabel = dayLabel,
                    isPast = isPast,
                    isToday = isToday
                )
            )
            weekCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        selectedDate = dateItems[todayIndex].fullDate
        isSelectedDateToday = true

        binding.rvDateSelector.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDateSelector.adapter = DateAdapter(dateItems, selectedIndex = todayIndex) { item ->
            selectedDate = item.fullDate
            isSelectedDateToday = item.isToday
            val cinema = selectedCinema
            if (cinema != null) {
                if (isSelectedDateToday && (selectedTime.isEmpty() || isShowtimePassed(selectedTime))) {
                    selectedTime = findFirstActiveShowtime(cinema)
                } else if (!isSelectedDateToday && selectedTime.isEmpty()) {
                    selectedTime = cinema.showtimes.firstOrNull().orEmpty()
                }
            }
            refreshShowtimeSelectionVisuals()
            updateBottomBar()
        }
        binding.rvDateSelector.scrollToPosition((todayIndex - 1).coerceAtLeast(0))
    }

    private fun isShowtimePassed(timeStr: String): Boolean {
        if (!isSelectedDateToday || timeStr.isBlank()) return false
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val now = Calendar.getInstance()
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return (h * 60 + m) <= nowMins
    }

    private fun findFirstActiveShowtime(cinema: Cinema): String {
        return cinema.showtimes.take(4).firstOrNull { !isShowtimePassed(it) }.orEmpty()
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

        // Pastikan cinema & jam default terpilih valid dan belum lewat jam sekarang
        if (selectedCinema == null || filtered.none { it.id == selectedCinema?.id }) {
            selectedCinema = filtered.firstOrNull()
        }
        val activeCinema = selectedCinema
        if (activeCinema != null && (selectedTime.isEmpty() || !activeCinema.showtimes.contains(selectedTime) || isShowtimePassed(selectedTime))) {
            selectedTime = findFirstActiveShowtime(activeCinema)
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
                    if (isShowtimePassed(timeStr)) {
                        AppToast.show(
                            this,
                            AppLanguage.tr(
                                "Showtime $timeStr has already passed",
                                "Jadwal jam $timeStr sudah lewat"
                            )
                        )
                        return@setOnClickListener
                    }
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
            val isPassed = isShowtimePassed(ref.time)
            val isSelected = !isPassed && ref.cinema.id == selectedCinema?.id && ref.time == selectedTime
            val isCinemaSelected = ref.cinema.id == selectedCinema?.id
            val priceStr = MovieRepository.formatRupiah(ref.cinema.ticketPrice)

            if (isPassed) {
                ref.binding.root.setBackgroundResource(R.drawable.bg_chip_unselected)
                ref.binding.root.alpha = 0.42f
                ref.binding.tvSlotTime.setTextColor(muted)
                ref.binding.tvSlotTime.paintFlags =
                    ref.binding.tvSlotTime.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                ref.binding.tvSlotSub.text = AppLanguage.tr("PASSED", "LEWAT")
                ref.binding.tvSlotSub.setTextColor(muted)
                ref.binding.tvSlotSub.paint.isFakeBoldText = false
            } else if (isSelected) {
                ref.binding.root.alpha = 1.0f
                ref.binding.tvSlotTime.paintFlags =
                    ref.binding.tvSlotTime.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                ref.binding.root.setBackgroundResource(R.drawable.bg_chip_selected)
                ref.binding.tvSlotTime.setTextColor(white)
                ref.binding.tvSlotSub.text = AppLanguage.tr("✓ SELECTED", "✓ DIPILIH")
                ref.binding.tvSlotSub.setTextColor(peach)
                ref.binding.tvSlotSub.paint.isFakeBoldText = true
            } else {
                ref.binding.root.alpha = 1.0f
                ref.binding.tvSlotTime.paintFlags =
                    ref.binding.tvSlotTime.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
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
        val timeDisplay = if (selectedTime.isEmpty() || isShowtimePassed(selectedTime)) {
            AppLanguage.tr("No active slot", "Jadwal lewat")
        } else {
            selectedTime
        }
        binding.tvBottomShowtimeDate.text = "$selectedDate • $timeDisplay"
        binding.tvBottomShowtimeVenue.text = "${cinema.name} • $fmt"
        binding.tvBottomPrice.text = "${MovieRepository.formatRupiah(cinema.ticketPrice)}/${AppLanguage.tr("adult", "dewasa")}"
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
            val formats = listOf(
                "All formats" to AppLanguage.tr("All studio types", "Semua studio bioskop"),
                "IMAX" to AppLanguage.tr("Extra wide screen & immersive sound", "Layar ekstra lebar & suara imersif"),
                "PREMIERE" to AppLanguage.tr("VIP recliner seats & blanket", "Kursi recliner VIP & selimut"),
                "DOLBY ATMOS" to AppLanguage.tr("360-degree spatial audio", "Audio spasial 360 derajat"),
                "4DX" to AppLanguage.tr("Motion seats & environmental effects", "Kursi gerak & efek lingkungan"),
                "REGULAR" to AppLanguage.tr("Standard 2D studio", "Studio 2D standar")
            )
            val checked = formats.indexOfFirst { it.first == selectedFormatFilter }.coerceAtLeast(0)
            ThemedOptionsBottomSheet(
                title = AppLanguage.tr("Choose Studio Format", "Pilih Format Studio"),
                subtitle = AppLanguage.tr("Filter showtimes by studio type", "Filter jadwal tayang berdasarkan tipe studio"),
                options = formats,
                selectedIndex = checked
            ) { which ->
                selectedFormatFilter = formats[which].first
                renderCinemasForSelectedCity()
            }.show(supportFragmentManager, ThemedOptionsBottomSheet.TAG)
        }

        binding.btnTrailer.setOnClickListener {
            val key = currentMovie?.trailerKey.orEmpty()
            if (key.isEmpty()) {
                AppToast.show(this, AppLanguage.tr("Trailer is not available for this movie yet", "Cuplikan trailer belum tersedia untuk film ini"))
                return@setOnClickListener
            }
            showAndPlayInlineTrailer(key)
        }

        binding.viewTrailerTouchShield.setOnClickListener {
            toggleTrailerPlayPause()
        }

        binding.btnPlayPauseTrailer.setOnClickListener {
            toggleTrailerPlayPause()
        }

        binding.btnMuteTrailer.setOnClickListener {
            toggleTrailerMute()
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
            val msg = if (isNowSaved) {
                AppLanguage.tr("Added to saved films", "Ditambahkan ke film tersimpan")
            } else {
                AppLanguage.tr("Removed from saved films", "Dihapus dari film tersimpan")
            }
            AppToast.show(this, msg)
        }

        binding.btnChooseSeats.setOnClickListener {
            if (selectedTime.isEmpty() || isShowtimePassed(selectedTime)) {
                AppToast.show(
                    this,
                    AppLanguage.tr(
                        "Selected showtime has passed. Please pick an active time or tomorrow's date.",
                        "Jadwal hari ini sudah lewat. Silakan pilih jam yang aktif atau hari besok."
                    )
                )
                return@setOnClickListener
            }
            val movie = currentMovie ?: return@setOnClickListener
            val cinema = selectedCinema ?: repository.cinemas[0]
            val launchSeatSelection = {
                val intent = Intent(this, SeatSelectionActivity::class.java).apply {
                    putExtra(SeatSelectionActivity.EXTRA_MOVIE, movie)
                    putExtra(SeatSelectionActivity.EXTRA_CINEMA, cinema)
                    putExtra(SeatSelectionActivity.EXTRA_DATE, selectedDate)
                    putExtra(SeatSelectionActivity.EXTRA_TIME, selectedTime)
                }
                startActivity(intent)
            }
            if (!repository.isLoggedIn()) {
                AppToast.show(
                    this,
                    AppLanguage.tr("Please sign in first to choose seats", "Silakan login terlebih dahulu untuk memilih kursi")
                )
                LoginBottomSheet(
                    onLoginSuccess = { launchSeatSelection() },
                    onSwitchToRegister = {
                        CreateAccountBottomSheet(
                            onRegisterSuccess = { launchSeatSelection() },
                            onSwitchToLogin = {
                                LoginBottomSheet(onLoginSuccess = { launchSeatSelection() })
                                    .show(supportFragmentManager, LoginBottomSheet.TAG)
                            }
                        ).show(supportFragmentManager, CreateAccountBottomSheet.TAG)
                    }
                ).show(supportFragmentManager, LoginBottomSheet.TAG)
                return@setOnClickListener
            }
            launchSeatSelection()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showAndPlayInlineTrailer(youtubeKey: String) {
        binding.layoutTrailerPlayer.visibility = View.VISIBLE
        binding.webTrailer.onResume()

        if (youtubeKey == preloadedTrailerKey && isTrailerWebLoaded) {
            binding.progressTrailer.visibility = View.GONE
            isTrailerPlaying = true
            binding.btnPlayPauseTrailer.setImageResource(R.drawable.ic_pause)
            sendYtCommand("playVideo")
            return
        }

        preloadedTrailerKey = youtubeKey
        isTrailerWebLoaded = false
        isTrailerPlaying = true
        isTrailerMuted = false
        binding.btnPlayPauseTrailer.setImageResource(R.drawable.ic_pause)
        binding.btnMuteTrailer.setImageResource(R.drawable.ic_volume_up)
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
                isTrailerWebLoaded = true
                binding.progressTrailer.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Watch-only: blokir semua klik keluar / pencarian video lain di YouTube
                return true
            }
        }

        // Langsung autoplay=1 saat user ketuk PUTAR CUPLIKAN; crop vertikal 136% + pointer-events:none
        // menghilangkan logo YouTube, judul link, dan mencegah pencarian ke YouTube.
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                #wrap { position: fixed; left: 0; top: -18%; width: 100%; height: 136%; pointer-events: none; }
                iframe { width: 100%; height: 100%; border: 0; pointer-events: none; }
              </style>
            </head>
            <body>
              <div id="wrap">
                <iframe id="yt"
                  src="https://www.youtube-nocookie.com/embed/$youtubeKey?autoplay=1&playsinline=1&controls=0&modestbranding=1&rel=0&iv_load_policy=3&fs=0&disablekb=1&loop=1&playlist=$youtubeKey&enablejsapi=1"
                  allow="autoplay; encrypted-media; picture-in-picture"
                  allowfullscreen>
                </iframe>
              </div>
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

    private fun sendYtCommand(func: String) {
        val js = "var f=document.getElementById('yt'); if(f&&f.contentWindow){f.contentWindow.postMessage(JSON.stringify({event:'command',func:'$func',args:''}),'*');}"
        binding.webTrailer.evaluateJavascript(js, null)
    }

    private fun toggleTrailerPlayPause() {
        if (isTrailerPlaying) {
            isTrailerPlaying = false
            binding.btnPlayPauseTrailer.setImageResource(R.drawable.ic_play_arrow)
            sendYtCommand("pauseVideo")
        } else {
            isTrailerPlaying = true
            binding.btnPlayPauseTrailer.setImageResource(R.drawable.ic_pause)
            sendYtCommand("playVideo")
        }
    }

    private fun toggleTrailerMute() {
        isTrailerMuted = !isTrailerMuted
        binding.btnMuteTrailer.setImageResource(
            if (isTrailerMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
        )
        sendYtCommand(if (isTrailerMuted) "mute" else "unMute")
    }

    private fun closeInlineTrailer() {
        isTrailerPlaying = false
        sendYtCommand("pauseVideo")
        binding.layoutTrailerPlayer.visibility = View.INVISIBLE
    }

    override fun onPause() {
        super.onPause()
        if (binding.layoutTrailerPlayer.visibility == View.VISIBLE) {
            isTrailerPlaying = false
            sendYtCommand("pauseVideo")
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
