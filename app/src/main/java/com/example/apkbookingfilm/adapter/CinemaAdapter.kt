package com.example.apkbookingfilm.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemTheaterBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast
import java.util.Calendar

class CinemaAdapter(
    private val cinemas: List<Cinema>,
    private val onCinemaClick: (Cinema, String) -> Unit
) : RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val binding = ItemTheaterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CinemaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        holder.bind(cinemas[position])
        AppLanguage.applyToView(holder.itemView)
    }

    override fun getItemCount(): Int = cinemas.size

    inner class CinemaViewHolder(private val binding: ItemTheaterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private fun isSlotPassedToday(timeStr: String): Boolean {
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return false
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val now = Calendar.getInstance()
            val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            return (h * 60 + m) <= nowMins
        }

        fun bind(cinema: Cinema) {
            binding.tvTheaterName.text = cinema.name
            binding.tvLocation.text = cinema.location
            binding.tvDistance.text = cinema.distance

            if (cinema.formats.isNotEmpty()) {
                binding.tvFormat1.text = cinema.formats.getOrNull(0) ?: ""
                binding.tvFormat2.text = cinema.formats.getOrNull(1) ?: ""
                binding.tvFormat3.text = cinema.formats.getOrNull(2) ?: ""
            }

            val times = cinema.showtimes
            if (times.isNotEmpty()) {
                val timeViews = listOf(binding.tvTime1, binding.tvTime2, binding.tvTime3)
                timeViews.forEachIndexed { index, tv ->
                    if (index < times.size) {
                        val slotTime = times[index]
                        tv.text = slotTime
                        val passed = isSlotPassedToday(slotTime)
                        if (passed) {
                            tv.alpha = 0.42f
                            tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                            tv.setOnClickListener {
                                AppToast.show(
                                    tv.context,
                                    AppLanguage.tr(
                                        "Showtime $slotTime has already passed today",
                                        "Jadwal jam $slotTime hari ini sudah lewat"
                                    )
                                )
                            }
                        } else {
                            tv.alpha = 1.0f
                            tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                            tv.setOnClickListener {
                                onCinemaClick(cinema, slotTime)
                            }
                        }
                    }
                }
            }

            binding.root.setOnClickListener {
                val defaultTime = cinema.showtimes.firstOrNull { !isSlotPassedToday(it) }
                    ?: cinema.showtimes.firstOrNull() ?: "19:30"
                onCinemaClick(cinema, defaultTime)
            }
        }
    }
}
