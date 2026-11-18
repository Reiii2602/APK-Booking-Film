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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityPaymentBinding
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.model.FoodOrderItem
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.model.PaymentMethod
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast
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
    private lateinit var selectedPaymentMethod: PaymentMethod

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
        selectedPaymentMethod = repository.getSelectedPaymentMethod()

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
        updatePaymentMethodUI()
        setupTimer()
        setupListeners()
    }

    private fun updatePaymentMethodUI() {
        binding.ivSelectedPayLogo.setImageResource(selectedPaymentMethod.logoResId)
        binding.tvPaymentBankName.text = selectedPaymentMethod.name
        binding.tvPaymentBankType.text = selectedPaymentMethod.type
        binding.tvPaymentAccountNumber.text = selectedPaymentMethod.accountNumber
        binding.tvPaymentAccountHolder.text = selectedPaymentMethod.accountHolder
        binding.tvPaymentSectionTitle.text = selectedPaymentMethod.type.uppercase(Locale.US)
    }

    private fun setupOrderSummary() {
        val currentMovie = movie ?: return
        val currentCinema = cinema ?: return

        if (isFoodOnly) {
            val allDrinks = foodItems.isNotEmpty() && foodItems.all { it.item.category.equals("Minuman", true) }
            val allFood = foodItems.isNotEmpty() && foodItems.none { it.item.category.equals("Minuman", true) }
            val orderCategoryLabel = when {
                allDrinks -> AppLanguage.tr("Drinks", "Minuman")
                allFood -> AppLanguage.tr("Food", "Makanan")
                else -> AppLanguage.tr("Food & Drinks", "Makanan & Minuman")
            }

            binding.tvLabelOrderMovie.text = orderCategoryLabel
            binding.tvOrderMovieTitle.gravity = android.view.Gravity.END
            binding.tvOrderMovieTitle.text = foodItems.joinToString("\n") { "${it.quantity}x ${it.item.name}" }
                .ifEmpty { AppLanguage.tr("Cinema Food & Drinks", "Makanan & Minuman Bioskop") }

            binding.tvLabelOrderShowtime.text = AppLanguage.tr("Pickup", "Pengambilan")
            binding.tvOrderDateTime.text = AppLanguage.tr("Today • F&B Counter", "Hari Ini • Konter F&B")
            binding.tvLabelOrderCinema.text = AppLanguage.tr("Cinema", "Bioskop")
            binding.rowOrderSeats.visibility = View.GONE

            binding.rowOrderFood.visibility = View.VISIBLE
            binding.tvLabelOrderFood.text = AppLanguage.tr("Total Items", "Jumlah Item")
            binding.tvOrderFood.text = "${foodItems.sumOf { it.quantity }} ${AppLanguage.tr("items", "item")}"
        } else {
            binding.tvLabelOrderMovie.text = AppLanguage.tr("Movie", "Film")
            binding.tvOrderMovieTitle.text = currentMovie.title
            binding.tvLabelOrderShowtime.text = AppLanguage.tr("Showtime", "Jadwal Tayang")
            binding.tvOrderDateTime.text = "$date • $time"
            binding.tvLabelOrderCinema.text = AppLanguage.tr("Cinema", "Bioskop")
            binding.rowOrderSeats.visibility = View.VISIBLE
            binding.tvLabelOrderSeats.text = AppLanguage.tr("Seats", "Kursi")
            binding.tvOrderSeats.text = "${seats.joinToString(", ")} • ${seats.size} ${AppLanguage.tr("tickets", "tiket")}"

            if (foodItems.isNotEmpty()) {
                binding.rowOrderFood.visibility = View.VISIBLE
                binding.tvLabelOrderFood.text = AppLanguage.tr("Food & Drinks", "Makanan & Minuman")
                binding.tvOrderFood.text = foodItems.joinToString("\n") { "${it.quantity}x ${it.item.name}" }
            } else {
                binding.rowOrderFood.visibility = View.GONE
            }
        }

        binding.tvOrderCinema.text = currentCinema.name

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
            else -> AppLanguage.tr("Image ready", "Gambar siap")
        }

        binding.tvUploadedFileName.text = displayName
        binding.tvUploadedFileSize.text = "✓ $sizeText • ${AppLanguage.tr("Uploaded", "Terunggah")}"
        binding.cardUploadedFile.visibility = View.VISIBLE

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
            AppToast.show(this, AppLanguage.tr("Order temporarily saved", "Order disimpan sementara"))
        }

        val openPaymentPicker = {
            PaymentMethodBottomSheet { method ->
                selectedPaymentMethod = method
                updatePaymentMethodUI()
                AppToast.show(this, "${AppLanguage.tr("Method selected", "Metode dipilih")}: ${method.name}")
            }.show(supportFragmentManager, PaymentMethodBottomSheet.TAG)
        }
        binding.btnChangePaymentMethod.setOnClickListener { openPaymentPicker() }
        binding.rowSelectedPaymentHeader.setOnClickListener { openPaymentPicker() }

        binding.btnCopyAccount.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Account Number", selectedPaymentMethod.accountNumber)
            clipboard.setPrimaryClip(clip)
            AppToast.show(this, "${AppLanguage.tr("Copied", "Disalin")}: ${selectedPaymentMethod.accountNumber}")
        }

        binding.btnCopyAmount.setOnClickListener {
            val amountStr = MovieRepository.formatRupiah(grandTotal)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Transfer Amount", amountStr)
            clipboard.setPrimaryClip(clip)
            AppToast.show(this, "${AppLanguage.tr("Amount copied", "Nominal disalin")}: $amountStr")
        }

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
                AppToast.show(
                    this,
                    AppLanguage.tr("Please upload a screenshot of transfer proof first", "Silakan upload screenshot bukti transfer terlebih dahulu")
                )
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
                paymentMethod = selectedPaymentMethod.name,
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
