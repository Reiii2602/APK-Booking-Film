package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.BottomSheetConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemedConfirmBottomSheet(
    private val badgeText: String = "KONFIRMASI",
    private val title: String,
    private val message: String,
    private val positiveText: String = "YA",
    private val negativeText: String = "BATAL",
    private val onConfirm: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetConfirmBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvConfirmBadge.text = badgeText
        binding.tvConfirmTitle.text = title
        binding.tvConfirmMessage.text = message
        binding.btnConfirmPositive.text = positiveText
        binding.btnConfirmNegative.text = negativeText

        binding.btnConfirmPositive.setOnClickListener {
            dismiss()
            onConfirm()
        }
        binding.btnConfirmNegative.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ThemedConfirmBottomSheet"
    }
}
