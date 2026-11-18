package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.ItemDateChipBinding
import com.example.apkbookingfilm.util.AppLanguage
import com.example.apkbookingfilm.util.AppToast

data class DateItem(
    val dayName: String,
    val dayNumber: String,
    val fullDate: String,
    val dayLabel: String = "NOV",
    val isPast: Boolean = false,
    val isToday: Boolean = false
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

            if (item.isPast) {
                binding.dateChipRoot.setBackgroundResource(R.drawable.bg_chip_unselected)
                binding.dateChipRoot.alpha = 0.38f
                binding.tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                binding.tvDayLabel.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                binding.root.setOnClickListener {
                    AppToast.show(
                        context,
                        AppLanguage.tr(
                            "Cannot select a past date (${item.fullDate})",
                            "Tidak dapat memilih hari yang sudah lewat (${item.fullDate})"
                        )
                    )
                }
                return
            }

            binding.dateChipRoot.alpha = 1.0f
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
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val previousIndex = selectedIndex
                selectedIndex = pos
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
                onDateSelected(item)
            }
        }
    }
}
