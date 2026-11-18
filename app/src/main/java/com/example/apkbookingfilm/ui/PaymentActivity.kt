package com.example.apkbookingfilm.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityPaymentBinding
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.FoodOrderItem
import com.example.apkbookingfilm.model.Movie
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var repository: MovieRepository

    private var movie: Movie? = null
    private var cinema: Cinema? = null
    private var date: String = "Fri, 06 Nov"
    private var time: String = "19:30"
    private var seats: ArrayList<String> = arrayListOf()
    private var foodItems: ArrayList<FoodOrderItem> = arrayListOf()
    private var isFoodOnly: Boolean = false

    private var seatSubtotal: Int = 0
    private var foodTotal: Int = 0
    private var grandTotal: Int = 0

    private var selectedProofUri: Uri? = null
    private var timer: CountDownTimer? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onProofImageSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        isFoodOnly = intent.getBooleanExtra(EXTRA_FOOD_ONLY, false)
        movie = intent.getSerializableExtra(EXTRA_MOVIE) as? Movie ?: repository.movies[0]
        cinema = intent.getSerializableExtra(EXTRA_CINEMA) as? Cinema ?: repository.cinemas[0]
        date = intent.getStringExtra(EXTRA_DATE) ?: "Fri, 06 Nov"
        time = intent.getStringExtra(EXTRA_TIME) ?: "19:30"

        val seatArgs = intent.getStringArrayListExtra(EXTRA_SEATS)
        if (!seatArgs.isNullOrEmpty()) {
            seats = seatArgs
        }

        @Suppress("UNCHECKED_CAST")
        val foodArgs = intent.getSerializableExtra(EXTRA_FOOD_ITEMS) as? ArrayList<FoodOrderItem>
        if (!foodArgs.isNullOrEmpty()) {
            foodItems = foodArgs
        }

        val ticketPrice = cinema?.ticketPrice ?: 50000
        seatSubtotal = if (isFoodOnly) 0 else seats.size * ticketPrice
        foodTotal = foodItems.sumOf { it.subtotal }
        grandTotal = seatSubtotal + foodTotal

        // Awalnya belum ada foto bukti transfer
        binding.cardUploadedFile.visibility = View.GONE
        binding.cardProofPreview.visibility = View.GONE

        setupOrderSummary()
        setupTimer()
        setupListeners()
    }

    private fun setupOrderSummary() {
        val currentMovie = movie ?: return
        val currentCinema = cinema ?: return

        if (isFoodOnly) {
            binding.tvOrderMovieTitle.text = "Cinema Food Order"
            binding.tvOrderDateTime.text = "Ambil di konter hari ini"
            binding.rowOrderSeats.visibility = View.GONE
        } else {
            binding.tvOrderMovieTitle.text = currentMovie.title
            binding.tvOrderDateTime.text = "$date • $time"
            binding.rowOrderSeats.visibility = View.VISIBLE
            binding.tvOrderSeats.text = "${seats.joinToString(", ")} • ${seats.size} tiket"
        }

        binding.tvOrderCinema.text = currentCinema.name

        if (foodItems.isNotEmpty()) {
            binding.rowOrderFood.visibility = View.VISIBLE
            binding.tvOrderFood.text = foodItems.joinToString("\n") { "${it.quantity}x ${it.item.name}" }
        } else {
            binding.rowOrderFood.visibility = View.GONE
        }

        val formattedTotal = MovieRepository.formatRupiah(grandTotal)
        binding.tvOrderTotal.text = formattedTotal
        binding.tvTransferAmount.text = formattedTotal
    }

    private fun setupTimer() {
        val totalMillis = (14 * 60 + 59) * 1000L
        timer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = (millisUntilFinished / 1000) / 60
                val secs = (millisUntilFinished / 1000) % 60
                binding.tvCountdown.text = String.format(Locale.US, "%02d:%02d", mins, secs)
            }

            override fun onFinish() {
                binding.tvCountdown.text = "00:00"
            }
        }.start()
    }

    private fun onProofImageSelected(uri: Uri) {
        selectedProofUri = uri

        var displayName = "bukti-transfer.jpg"
        var fileSizeBytes = -1L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    if (sizeIdx >= 0) {
                        fileSizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {}

        val sizeText = when {
            fileSizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
            fileSizeBytes > 0 -> String.format(Locale.US, "%d KB", fileSizeBytes / 1024)
            else -> "Gambar siap"
        }

        binding.tvUploadedFileName.text = displayName
        binding.tvUploadedFileSize.text = "✓ $sizeText • Terunggah"
        binding.cardUploadedFile.visibility = View.VISIBLE

        // Tampilkan preview gambar bukti SS persis di bawahnya
        binding.cardProofPreview.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .into(binding.ivProofPreview)
    }

    private fun setupListeners() {
        binding.btnBackPayment.setOnClickListener {
            finish()
        }

        binding.btnBookmarkPayment.setOnClickListener {
            Toast.makeText(this, "Order saved to pending", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyAccount.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Account Number", "0373 8812 4566")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied Bank BCA account: 0373 8812 4566", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyAmount.setOnClickListener {
            val amountStr = MovieRepository.formatRupiah(grandTotal)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Transfer Amount", amountStr)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Nominal disalin: $amountStr", Toast.LENGTH_SHORT).show()
        }

        // Buka galeri / file picker untuk pilih foto screenshot bukti transfer asli
        binding.cardUploadDashed.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRemoveUpload.setOnClickListener {
            selectedProofUri = null
            binding.ivProofPreview.setImageDrawable(null)
            binding.cardUploadedFile.visibility = View.GONE
            binding.cardProofPreview.visibility = View.GONE
        }

        binding.btnSubmitPaymentProof.setOnClickListener {
            val proof = selectedProofUri
            if (proof == null) {
                Toast.makeText(this, "Silakan upload screenshot bukti transfer terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentMovie = movie ?: return@setOnClickListener
            val currentCinema = cinema ?: return@setOnClickListener

            val randomBookingNum = (10000..99999).random()
            val booking = Booking(
                bookingId = "BK-$randomBookingNum",
                movie = currentMovie,
                cinema = currentCinema,
                date = date,
                time = if (isFoodOnly) "Counter Pickup" else "$time WIB",
                seats = if (isFoodOnly) listOf("FOOD ONLY") else seats,
                subtotal = seatSubtotal,
                serviceFee = 0,
                totalPrice = grandTotal,
                paymentMethod = "Bank Transfer BCA",
                paymentProofUploaded = true,
                isPaid = true,
                foodItems = foodItems,
                foodTotal = foodTotal,
                proofUri = proof.toString()
            )

            repository.saveBooking(booking)
            repository.clearFoodCart()

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
        const val EXTRA_FOOD_ITEMS = "extra_food_items"
        const val EXTRA_FOOD_ONLY = "extra_food_only"
    }
}
