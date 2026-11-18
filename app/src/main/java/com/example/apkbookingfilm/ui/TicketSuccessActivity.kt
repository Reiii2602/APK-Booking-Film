package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityTicketSuccessBinding
import com.example.apkbookingfilm.model.Booking
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast
import com.example.apkbookingfilm.util.TmdbImageLoader

class TicketSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketSuccessBinding
    private lateinit var repository: MovieRepository
    private var booking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        booking = intent.getSerializableExtra(EXTRA_BOOKING) as? Booking ?: repository.getLatestBooking()

        setupTicketView()
        setupListeners()
    }

    private fun setupTicketView() {
        val currentBooking = booking ?: return
        val isFoodOrder = currentBooking.seats.contains("FOOD ONLY") || currentBooking.subtotal == 0

        binding.tvTicketCinema.text = currentBooking.cinema.name
        binding.tvTicketDate.text = currentBooking.date
        binding.tvTicketTime.text = currentBooking.time
        binding.tvTicketBookingId.text = "#${currentBooking.bookingId}"

        if (isFoodOrder) {
            val allDrinks = currentBooking.foodItems.isNotEmpty() &&
                currentBooking.foodItems.all { it.item.category.equals("Minuman", true) }
            val padPx = (12 * resources.displayMetrics.density).toInt()
            binding.ivTicketPoster.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.ivTicketPoster.setPadding(padPx, padPx, padPx, padPx)
            binding.ivTicketPoster.setBackgroundResource(R.drawable.bg_peach_card)
            binding.ivTicketPoster.setImageResource(if (allDrinks) R.drawable.ic_drink else R.drawable.ic_popcorn)

            binding.tvTicketMovieTitle.text = currentBooking.foodItems.joinToString(", ") {
                "${it.quantity}x ${it.item.name}"
            }.ifEmpty { AppLanguage.tr("Cinema Food & Drinks", "Makanan & Minuman Bioskop") }
            binding.tvTicketStudio.text = AppLanguage.tr("F&B Counter Pickup", "Konter Makanan & Minuman")
            binding.tvTicketSubtitle.text = AppLanguage.tr(
                "Your digital F&B order voucher is ready",
                "Voucher pesanan makanan & minuman kamu sudah siap"
            )
            binding.tvLabelTicketSeats.text = AppLanguage.tr("Order Type", "Jenis Pesanan")
            binding.tvTicketSeats.text = if (allDrinks) {
                AppLanguage.tr("Drinks", "Minuman")
            } else {
                AppLanguage.tr("Food & Drinks", "Makanan & Minuman")
            }
            binding.tvTicketQrHint.text = AppLanguage.tr("SCAN AT CINEMA F&B COUNTER", "SCAN DI KONTER F&B BIOSKOP")
        } else {
            TmdbImageLoader.loadPoster(binding.ivTicketPoster, currentBooking.movie)
            binding.tvTicketMovieTitle.text = currentBooking.movie.title
            binding.tvTicketStudio.text = currentBooking.cinema.formats.firstOrNull() ?: "Studio 1"
            binding.tvTicketSeats.text = currentBooking.seats.joinToString(", ")
        }

        if (currentBooking.foodItems.isNotEmpty()) {
            binding.layoutTicketFood.visibility = android.view.View.VISIBLE
            binding.tvTicketFood.text = currentBooking.foodItems.joinToString(", ") {
                "${it.quantity}x ${it.item.name}"
            }
        }
    }

    private fun setupListeners() {
        binding.btnBackTicket.setOnClickListener {
            finish()
        }

        binding.btnSaveTicket.setOnClickListener {
            AppToast.show(this, AppLanguage.tr("E-Ticket saved to profile!", "E-Ticket tersimpan di profil!"))
        }

        binding.btnBackToHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_BOOKING = "extra_booking"
    }
}
