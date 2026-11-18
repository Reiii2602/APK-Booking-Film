package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.ItemSeatBinding
import com.example.apkbookingfilm.model.Seat
import com.example.apkbookingfilm.model.SeatStatus

class SeatAdapter(
    private val seats: List<Seat>,
    private val onSeatToggled: (Seat, List<Seat>) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    fun getSelectedSeats(): List<Seat> {
        return seats.filter { it.status == SeatStatus.SELECTED }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seats[position])
    }

    override fun getItemCount(): Int = seats.size

    inner class SeatViewHolder(private val binding: ItemSeatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(seat: Seat) {
            val context = binding.root.context
            binding.tvSeatNumber.text = seat.id

            when (seat.status) {
                SeatStatus.AVAILABLE -> {
                    binding.tvSeatNumber.setBackgroundResource(R.drawable.bg_seat_available)
                    binding.tvSeatNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    binding.root.isClickable = true
                }
                SeatStatus.RESERVED -> {
                    binding.tvSeatNumber.setBackgroundResource(R.drawable.bg_seat_reserved)
                    binding.tvSeatNumber.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    binding.root.isClickable = false
                }
                SeatStatus.SELECTED -> {
                    binding.tvSeatNumber.setBackgroundResource(R.drawable.bg_seat_selected)
                    binding.tvSeatNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.root.isClickable = true
                }
            }

            binding.root.setOnClickListener {
                if (seat.status == SeatStatus.RESERVED) return@setOnClickListener

                seat.status = if (seat.status == SeatStatus.SELECTED) {
                    SeatStatus.AVAILABLE
                } else {
                    SeatStatus.SELECTED
                }
                notifyItemChanged(bindingAdapterPosition)
                onSeatToggled(seat, getSelectedSeats())
            }
        }
    }
}
