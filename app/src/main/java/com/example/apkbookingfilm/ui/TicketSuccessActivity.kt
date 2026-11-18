package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityTicketSuccessBinding
import com.example.apkbookingfilm.model.Booking
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
        TmdbImageLoader.loadPoster(binding.ivTicketPoster, currentBooking.movie)

        binding.tvTicketMovieTitle.text = currentBooking.movie.title
        binding.tvTicketCinema.text = currentBooking.cinema.name
        binding.tvTicketStudio.text = "${currentBooking.cinema.formats.firstOrNull() ?: "Studio 1"}"
        binding.tvTicketDate.text = currentBooking.date
        binding.tvTicketTime.text = currentBooking.time
        binding.tvTicketSeats.text = currentBooking.seats.joinToString(", ")
        binding.tvTicketBookingId.text = "#${currentBooking.bookingId}"

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
            Toast.makeText(this, "Ticket saved to gallery & profile!", Toast.LENGTH_SHORT).show()
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
