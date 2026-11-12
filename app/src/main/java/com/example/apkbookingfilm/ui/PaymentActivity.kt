package com.example.apkbookingfilm.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityPaymentBinding
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.Movie

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var repository: MovieRepository

    private var movie: Movie? = null
    private var cinema: Cinema? = null
    private var date: String = "Fri, 06 Nov • 19:30"
    private var time: String = "19:30"
    private var seats: ArrayList<String> = arrayListOf("E6", "E7")

    private var hasUploadedProof = true
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        movie = intent.getSerializableExtra(EXTRA_MOVIE) as? Movie ?: repository.movies[0]
        cinema = intent.getSerializableExtra(EXTRA_CINEMA) as? Cinema ?: repository.cinemas[0]
        date = intent.getStringExtra(EXTRA_DATE) ?: "Fri, 06 Nov • 19:30"
        time = intent.getStringExtra(EXTRA_TIME) ?: "19:30"
        val seatArgs = intent.getStringArrayListExtra(EXTRA_SEATS)
        if (!seatArgs.isNullOrEmpty()) {
            seats = seatArgs
        }

        setupOrderSummary()
        setupTimer()
        setupListeners()
    }

    private fun setupOrderSummary() {
        val currentMovie = movie ?: return
        val currentCinema = cinema ?: return

        binding.tvOrderMovieTitle.text = currentMovie.title
        binding.tvOrderDateTime.text = "$date • $time"
        binding.tvOrderCinema.text = currentCinema.name
        binding.tvOrderSeats.text = "${seats.joinToString(", ")} • ${seats.size} adults"
        binding.tvOrderTotal.text = "Rp50.000"
    }

    private fun setupTimer() {
        val totalMillis = (14 * 60 + 59) * 1000L // 14:59
        timer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = (millisUntilFinished / 1000) / 60
                val secs = (millisUntilFinished / 1000) % 60
                binding.tvCountdown.text = String.format("%02d:%02d", mins, secs)
            }

            override fun onFinish() {
                binding.tvCountdown.text = "00:00"
            }
        }.start()
    }

    private fun setupListeners() {
        binding.btnBackPayment.setOnClickListener {
            finish()
        }

        binding.btnBookmarkPayment.setOnClickListener {
            Toast.makeText(this, "Order saved to pending", Toast.LENGTH_SHORT).show()
        }

        // Copy buttons
        binding.btnCopyAccount.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Account Number", "0373 8812 4566")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied Bank BCA account: 0373 8812 4566", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyAmount.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Transfer Amount", "Rp50.000")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied transfer amount: Rp50.000", Toast.LENGTH_SHORT).show()
        }

        // Upload receipt toggle
        binding.cardUploadDashed.setOnClickListener {
            hasUploadedProof = true
            binding.cardUploadedFile.visibility = View.VISIBLE
            Toast.makeText(this, "Proof attached: transfer-proof-0611.jpg", Toast.LENGTH_SHORT).show()
        }

        binding.btnRemoveUpload.setOnClickListener {
            hasUploadedProof = false
            binding.cardUploadedFile.visibility = View.GONE
        }

        binding.btnSubmitPaymentProof.setOnClickListener {
            val currentMovie = movie ?: return@setOnClickListener
            val currentCinema = cinema ?: return@setOnClickListener

            val randomBookingNum = (10000..99999).random()
            val booking = Booking(
                bookingId = "BK-$randomBookingNum",
                movie = currentMovie,
                cinema = currentCinema,
                date = date,
                time = "$time WIB",
                seats = seats,
                subtotal = 50000,
                serviceFee = 0,
                totalPrice = 50000,
                paymentMethod = "Metro Bank Transfer",
                paymentProofUploaded = true,
                isPaid = true
            )

            repository.saveBooking(booking)

            val intent = Intent(this, TicketSuccessActivity::class.java).apply {
                putExtra(TicketSuccessActivity.EXTRA_BOOKING, booking)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    companion object {
        const val EXTRA_MOVIE = "extra_movie"
        const val EXTRA_CINEMA = "extra_cinema"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_SEATS = "extra_seats"
    }
}
