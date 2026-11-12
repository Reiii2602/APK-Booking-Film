package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.ItemDateChipBinding

data class DateItem(
    val dayName: String,
    val dayNumber: String,
    val fullDate: String,
    val dayLabel: String = "NOV"
)

class DateAdapter(
    private val dates: List<DateItem>,
    private var selectedIndex: Int = 0,
    private val onDateSelected: (DateItem) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = dates.size

    inner class DateViewHolder(private val binding: ItemDateChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DateItem, isSelected: Boolean) {
            val context = binding.root.context
            binding.tvDayName.text = item.dayName
            binding.tvDayNumber.text = item.dayNumber
            binding.tvDayLabel.text = item.dayLabel

            if (isSelected) {
                binding.dateChipRoot.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.peach_card))
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.tvDayLabel.setTextColor(ContextCompat.getColor(context, R.color.peach_bg))
            } else {
                binding.dateChipRoot.setBackgroundResource(R.drawable.bg_chip_unselected)
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.tvDayLabel.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            }

            binding.root.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
                onDateSelected(item)
            }
        }
    }
}
