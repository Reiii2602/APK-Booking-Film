package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemTheaterBinding
import com.example.apkbookingfilm.model.Cinema
import com.example.apkbookingfilm.util.AppLanguage

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
                        tv.text = times[index]
                        tv.setOnClickListener {
                            onCinemaClick(cinema, times[index])
                        }
                    }
                }
            }

            binding.root.setOnClickListener {
                val defaultTime = cinema.showtimes.firstOrNull() ?: "19:30"
                onCinemaClick(cinema, defaultTime)
            }
        }
    }
}
