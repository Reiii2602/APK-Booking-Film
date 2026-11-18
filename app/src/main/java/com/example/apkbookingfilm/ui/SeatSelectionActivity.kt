package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.SeatAdapter
import com.example.apkbookingfilm.databinding.ActivitySeatSelectionBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.Seat
import com.example.apkbookingfilm.model.SeatStatus

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeatSelectionBinding
    private lateinit var seatAdapter: SeatAdapter

    private var movie: Movie? = null
    private var cinema: Cinema? = null
    private var date: String = "Fri, 08 Nov"
    private var time: String = "19:30"

    private val selectedSeatNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeatSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movie = intent.getSerializableExtra(EXTRA_MOVIE) as? Movie
        cinema = intent.getSerializableExtra(EXTRA_CINEMA) as? Cinema
        date = intent.getStringExtra(EXTRA_DATE) ?: "Fri, 08 Nov"
        time = intent.getStringExtra(EXTRA_TIME) ?: "19:30"

        setupHeader()
        setupRowLabels()
        setupSeatGrid()
        setupListeners()
    }

    private fun setupHeader() {
        val currentMovie = movie ?: return
        val currentCinema = cinema ?: return
        binding.tvSeatMovieInfo.text = "${currentMovie.title} • $date • $time • ${currentCinema.name}"
    }

    private fun setupRowLabels() {
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        binding.layoutRowLabelsLeft.removeAllViews()
        binding.layoutRowLabelsRight.removeAllViews()

        val itemHeightPx = (40 * resources.displayMetrics.density).toInt()

        for (r in rows) {
            val tvLeft = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    itemHeightPx
                )
                text = r
                gravity = Gravity.CENTER
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            binding.layoutRowLabelsLeft.addView(tvLeft)

            val tvRight = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    itemHeightPx
                )
                text = r
                gravity = Gravity.CENTER
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            binding.layoutRowLabelsRight.addView(tvRight)
        }
    }

    private fun setupSeatGrid() {
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val seatList = mutableListOf<Seat>()

        // Generate matrix: exactly as shown in Image 2 with E6, E7 selected
        for (r in rows) {
            for (col in 1..8) {
                val seatId = "$r$col"
                val status = when (seatId) {
                    "E6", "E7" -> SeatStatus.SELECTED
                    "A3", "A4", "B1", "B2", "C7", "C8", "D2", "D3", "D8", "F1", "F2", "G5", "G6", "G7", "G8", "H2" -> SeatStatus.RESERVED
                    else -> SeatStatus.AVAILABLE
                }
                seatList.add(Seat(id = seatId, row = r, number = col, status = status))
            }
        }

        selectedSeatNames.clear()
        selectedSeatNames.addAll(listOf("E6", "E7"))
        updatePriceSummary()

        binding.rvSeats.layoutManager = GridLayoutManager(this, 8)
        seatAdapter = SeatAdapter(seatList) { _, selectedList ->
            selectedSeatNames.clear()
            selectedSeatNames.addAll(selectedList.map { it.id })
            updatePriceSummary()
        }
        binding.rvSeats.adapter = seatAdapter
    }

    private fun updatePriceSummary() {
        val count = selectedSeatNames.size
        val total = 50000 // In Figma it shows Rp50.000 total

        binding.tvSeatTotalPrice.text = "Rp50.000 total"
        binding.tvSeatsCountDesc.text = "$count of 8 seats • Standard"

        if (selectedSeatNames.isNotEmpty()) {
            binding.tvBottomSeatsLabel.text = "Seats " + selectedSeatNames.joinToString(", ")
            binding.tvBottomSeatsDetails.text = "$count adults • Standard • $time"
            binding.btnContinueToPayment.isEnabled = true

            // Update chips
            if (selectedSeatNames.size >= 1) {
                binding.chipSeat1.visibility = View.VISIBLE
                binding.tvChip1Text.text = selectedSeatNames[0]
            } else {
                binding.chipSeat1.visibility = View.GONE
            }

            if (selectedSeatNames.size >= 2) {
                binding.chipSeat2.visibility = View.VISIBLE
                binding.tvChip2Text.text = selectedSeatNames[1]
            } else {
                binding.chipSeat2.visibility = View.GONE
            }
        } else {
            binding.tvBottomSeatsLabel.text = "No seats selected"
            binding.chipSeat1.visibility = View.GONE
            binding.chipSeat2.visibility = View.GONE
            binding.btnContinueToPayment.isEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnBackSeats.setOnClickListener {
            finish()
        }

        binding.btnBookmarkSeats.setOnClickListener {
            Toast.makeText(this, "Showtime saved", Toast.LENGTH_SHORT).show()
        }

        binding.chipSeat1.setOnClickListener {
            if (selectedSeatNames.isNotEmpty()) {
                val removed = selectedSeatNames.removeAt(0)
                updateSeatStatus(removed, SeatStatus.AVAILABLE)
            }
        }

        binding.chipSeat2.setOnClickListener {
            if (selectedSeatNames.size >= 2) {
                val removed = selectedSeatNames.removeAt(1)
                updateSeatStatus(removed, SeatStatus.AVAILABLE)
            }
        }

        binding.btnContinueToPayment.setOnClickListener {
            if (selectedSeatNames.isEmpty()) {
                Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentMovie = movie ?: return@setOnClickListener
            val currentCinema = cinema ?: return@setOnClickListener

            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_MOVIE, currentMovie)
                putExtra(PaymentActivity.EXTRA_CINEMA, currentCinema)
                putExtra(PaymentActivity.EXTRA_DATE, date)
                putExtra(PaymentActivity.EXTRA_TIME, time)
                putStringArrayListExtra(PaymentActivity.EXTRA_SEATS, ArrayList(selectedSeatNames))
            }
            startActivity(intent)
        }
    }

    private fun updateSeatStatus(seatId: String, status: SeatStatus) {
        val seat = seatAdapter.getSelectedSeats().find { it.id == seatId }
        seat?.let {
            it.status = status
            seatAdapter.notifyDataSetChanged()
            updatePriceSummary()
        }
    }

    companion object {
        const val EXTRA_MOVIE = "extra_movie"
        const val EXTRA_CINEMA = "extra_cinema"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
    }
}
