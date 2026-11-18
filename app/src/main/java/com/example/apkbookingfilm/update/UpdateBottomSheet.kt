package com.example.apkbookingfilm.update

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.BuildConfig
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.databinding.BottomSheetUpdateBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class UpdateBottomSheet(
    private val info: UpdateInfo,
    private val onUpdateClick: () -> Unit,
    private val onDismissClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUpdateBinding? = null
    private val binding get() = _binding!!
    private var actionTaken = false

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sizeText = if (info.apkSize > 0) {
            String.format(Locale.US, " (%.1f MB)", info.apkSize / 1024.0 / 1024.0)
        } else ""

        binding.tvCurrentVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.tvNewVersion.text = "v${info.versionName}$sizeText"
        binding.tvUpdateSubtitle.text = "Versi v${info.versionName}$sizeText sudah tersedia. Perbarui sekarang untuk fitur terbaru."

        val cleanedChangelog = info.changelog
            .replace(Regex("Update otomatis dari push ke `main` \\([a-f0-9]+\\)\\.?"), "")
            .replace("Perubahan terbaru:", "")
            .trim()
            .ifEmpty { "Perbaikan bug & peningkatan performa." }
        binding.tvUpdateChangelog.text = cleanedChangelog

        binding.btnUpdateNow.setOnClickListener {
            actionTaken = true
            dismiss()
            onUpdateClick()
        }

        binding.btnUpdateLater.setOnClickListener {
            actionTaken = true
            dismiss()
            onDismissClick()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (!actionTaken) onDismissClick()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UpdateBottomSheet"
    }
}
