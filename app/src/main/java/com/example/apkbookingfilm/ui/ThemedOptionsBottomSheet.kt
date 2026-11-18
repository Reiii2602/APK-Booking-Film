package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.BottomSheetOptionsBinding
import com.example.apkbookingfilm.databinding.ItemCityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemedOptionsBottomSheet(
    private val title: String,
    private val subtitle: String = "",
    private val options: List<Pair<String, String>>,
    private val selectedIndex: Int = -1,
    private val onSelect: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOptionsBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvOptionsTitle.text = title
        if (subtitle.isNotEmpty()) {
            binding.tvOptionsSubtitle.visibility = View.VISIBLE
            binding.tvOptionsSubtitle.text = subtitle
        }

        val inflater = LayoutInflater.from(requireContext())
        binding.layoutOptionsList.removeAllViews()
        options.forEachIndexed { index, (label, meta) ->
            val item = ItemCityBinding.inflate(inflater, binding.layoutOptionsList, false)
            item.tvCityName.text = label
            item.tvCityCinemaCount.text = meta
            item.tvCityCinemaCount.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
            item.ivCityPin.visibility = View.GONE
            val isSelected = index == selectedIndex
            item.ivCitySelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            item.root.setBackgroundResource(if (isSelected) R.drawable.bg_peach_card else R.drawable.bg_chip_unselected)
            item.root.setOnClickListener {
                dismiss()
                onSelect(index)
            }
            binding.layoutOptionsList.addView(item.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ThemedOptionsBottomSheet"
    }
}
