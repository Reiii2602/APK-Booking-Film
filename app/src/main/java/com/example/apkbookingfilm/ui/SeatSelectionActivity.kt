package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.FoodAdapter
import com.example.apkbookingfilm.adapter.FoodViewType
import com.example.apkbookingfilm.adapter.SeatAdapter
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivitySeatSelectionBinding
import com.example.apkbookingfilm.databinding.ItemSeatChipBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.FoodOrderItem
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.Seat
import com.example.apkbookingfilm.model.SeatStatus

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeatSelectionBinding
    private lateinit var repository: MovieRepository
    private lateinit var seatAdapter: SeatAdapter
    private val seatList = mutableListOf<Seat>()

    private var movie: Movie? = null
    private var cinema: Cinema? = null
    private var date: String = "Fri, 08 Nov"
    private var time: String = "19:30"

    private val selectedSeatNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeatSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        movie = intent.getSerializableExtra(EXTRA_MOVIE) as? Movie
        cinema = intent.getSerializableExtra(EXTRA_CINEMA) as? Cinema
        date = intent.getStringExtra(EXTRA_DATE) ?: "Fri, 08 Nov"
        time = intent.getStringExtra(EXTRA_TIME) ?: "19:30"

        setupHeader()
        setupRowLabels()
        setupSeatGrid()
        setupFoodAddons()
        setupListeners()
        updatePriceSummary()
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
        seatList.clear()

        // Awalnya TIDAK ada kursi yang dipilih (semua kursi kosong berstatus AVAILABLE kecuali TAKEN)
        val takenIds = setOf("A3", "A4", "B1", "B2", "C7", "C8", "D2", "D3", "D8", "F1", "F2", "G5", "G6", "G7", "G8", "H2")
        for (r in rows) {
            for (col in 1..8) {
                val seatId = "$r$col"
                val status = if (takenIds.contains(seatId)) SeatStatus.RESERVED else SeatStatus.AVAILABLE
                seatList.add(Seat(id = seatId, row = r, number = col, status = status))
            }
        }

        selectedSeatNames.clear()

        binding.rvSeats.layoutManager = GridLayoutManager(this, 8)
        seatAdapter = SeatAdapter(seatList) { _, selectedList ->
            selectedSeatNames.clear()
            selectedSeatNames.addAll(selectedList.map { it.id })
            updatePriceSummary()
        }
        binding.rvSeats.adapter = seatAdapter
    }

    private fun setupFoodAddons() {
        binding.rvFoodAddon.layoutManager = LinearLayoutManager(this)
        binding.rvFoodAddon.adapter = FoodAdapter(
            items = MovieRepository.foodMenu,
            viewType = FoodViewType.CART_ROW,
            repository = repository,
            onQuantityChanged = { updatePriceSummary() }
        )
    }

    private fun updatePriceSummary() {
        val count = selectedSeatNames.size
        val ticketPrice = cinema?.ticketPrice ?: 50000
        val seatTotal = count * ticketPrice
        val foodCount = repository.getFoodCartCount()
        val foodTotal = repository.getFoodCartTotal()
        val grandTotal = seatTotal + foodTotal

        binding.tvSeatTotalPrice.text = "${MovieRepository.formatRupiah(grandTotal)} total"
        binding.tvFoodAddonTotal.text = MovieRepository.formatRupiah(foodTotal)
        binding.tvSeatsCountDesc.text = "$count of 8 seats • Standard"

        val chipsContainer = binding.layoutSelectedChips
        chipsContainer.removeAllViews()

        if (selectedSeatNames.isNotEmpty()) {
            binding.tvNoSeatsHint.visibility = View.GONE
            binding.scrollSelectedChips.visibility = View.VISIBLE
            binding.tvBottomSeatsLabel.text = "Seats " + selectedSeatNames.joinToString(", ")
            val foodLabel = if (foodCount > 0) " + $foodCount snack" else ""
            binding.tvBottomSeatsDetails.text = "$count tiket$foodLabel • $time"
            binding.btnContinueToPayment.isEnabled = true

            val inflater = LayoutInflater.from(this)
            selectedSeatNames.forEach { seatId ->
                val chipBinding = ItemSeatChipBinding.inflate(inflater, chipsContainer, false)
                chipBinding.tvSeatChipLabel.text = seatId
                chipBinding.root.setOnClickListener {
                    unselectSeat(seatId)
                }
                chipsContainer.addView(chipBinding.root)
            }
        } else {
            binding.tvNoSeatsHint.visibility = View.VISIBLE
            binding.scrollSelectedChips.visibility = View.GONE
            binding.tvBottomSeatsLabel.text = "Belum ada kursi dipilih"
            binding.tvBottomSeatsDetails.text = if (foodCount > 0) "$foodCount camilan dipilih • pilih kursi dulu" else "Ketuk kursi tersedia di atas"
            binding.btnContinueToPayment.isEnabled = false
        }
    }

    private fun unselectSeat(seatId: String) {
        val idx = seatList.indexOfFirst { it.id == seatId }
        if (idx >= 0) {
            seatList[idx].status = SeatStatus.AVAILABLE
            seatAdapter.notifyItemChanged(idx)
            selectedSeatNames.remove(seatId)
            updatePriceSummary()
        }
    }

    private fun setupListeners() {
        binding.btnBackSeats.setOnClickListener {
            finish()
        }

        binding.btnBookmarkSeats.setOnClickListener {
            Toast.makeText(this, "Showtime saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinueToPayment.setOnClickListener {
            if (selectedSeatNames.isEmpty()) {
                Toast.makeText(this, "Silakan pilih minimal 1 kursi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentMovie = movie ?: return@setOnClickListener
            val currentCinema = cinema ?: return@setOnClickListener
            val foodOrders = ArrayList<FoodOrderItem>(repository.getFoodCartOrders())

            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_MOVIE, currentMovie)
                putExtra(PaymentActivity.EXTRA_CINEMA, currentCinema)
                putExtra(PaymentActivity.EXTRA_DATE, date)
                putExtra(PaymentActivity.EXTRA_TIME, time)
                putStringArrayListExtra(PaymentActivity.EXTRA_SEATS, ArrayList(selectedSeatNames))
                putExtra(PaymentActivity.EXTRA_FOOD_ITEMS, foodOrders)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_MOVIE = "extra_movie"
        const val EXTRA_CINEMA = "extra_cinema"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
    }
}
