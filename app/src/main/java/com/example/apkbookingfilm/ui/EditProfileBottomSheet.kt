package com.example.apkbookingfilm.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.BottomSheetEditProfileBinding
import com.example.apkbookingfilm.util.AppToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class EditProfileBottomSheet(
    private val onProfileSaved: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private var savedAvatarPath: String? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            saveAvatarLocally(uri)
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        val user = repository.getUser()
        binding.etEditNickname.setText(user.name)
        binding.etEditEmail.setText(user.email)
        binding.etEditTier.setText(user.membershipTier)
        binding.tvEditAvatarInitials.text = user.avatarInitials

        if (user.avatarUri.isNotEmpty() && File(user.avatarUri).exists()) {
            binding.ivEditAvatar.visibility = View.VISIBLE
            Glide.with(this).load(File(user.avatarUri)).circleCrop().into(binding.ivEditAvatar)
        }

        binding.btnPickAvatar.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }
        binding.tvChangePhotoHint.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditNickname.text?.toString()?.trim().orEmpty()
            val newEmail = binding.etEditEmail.text?.toString()?.trim().orEmpty()
            val newTier = binding.etEditTier.text?.toString()?.trim().orEmpty()
            if (newName.isEmpty()) {
                binding.etEditNickname.error = "Nickname tidak boleh kosong"
                return@setOnClickListener
            }
            repository.updateProfile(newName, newEmail, newTier, savedAvatarPath)
            AppToast.show(requireContext(), "Profil berhasil diperbarui")
            onProfileSaved()
            dismiss()
        }
    }

    private fun saveAvatarLocally(uri: Uri) {
        try {
            val dest = File(requireContext().filesDir, "user_avatar.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            savedAvatarPath = dest.absolutePath
            binding.ivEditAvatar.visibility = View.VISIBLE
            Glide.with(this)
                .load(dest)
                .skipMemoryCache(true)
                .circleCrop()
                .into(binding.ivEditAvatar)
        } catch (_: Exception) {
            AppToast.show(requireContext(), "Gagal memuat foto profil")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditProfileBottomSheet"
    }
}
