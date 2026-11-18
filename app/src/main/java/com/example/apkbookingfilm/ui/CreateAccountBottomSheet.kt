package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.BottomSheetCreateAccountBinding
import com.example.apkbookingfilm.util.AppToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateAccountBottomSheet(
    private val onRegisterSuccess: () -> Unit,
    private val onSwitchToLogin: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateAccountBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = MovieRepository(requireContext())

        // Register with Google
        binding.btnRegisterWithGoogle.setOnClickListener {
            repository.login("Cinema Member", "member@gmail.com", "Google")
            AppToast.show(requireContext(), "Akun Google berhasil didaftarkan!")
            onRegisterSuccess()
            dismiss()
        }

        // Register with Apple
        binding.btnRegisterWithApple.setOnClickListener {
            repository.login("Cinema Member", "member@icloud.com", "Apple")
            AppToast.show(requireContext(), "Akun Apple berhasil didaftarkan!")
            onRegisterSuccess()
            dismiss()
        }

        // Manual registration with Name, Email & Password
        binding.btnDoRegister.setOnClickListener {
            val name = binding.etRegisterName.text?.toString()?.trim().orEmpty()
            val email = binding.etRegisterEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etRegisterPassword.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.tilRegisterName.error = "Nama lengkap wajib diisi"
                return@setOnClickListener
            } else {
                binding.tilRegisterName.error = null
            }

            if (email.isEmpty() || !email.contains("@")) {
                binding.tilRegisterEmail.error = "Masukkan alamat email yang valid"
                return@setOnClickListener
            } else {
                binding.tilRegisterEmail.error = null
            }

            if (password.length < 6) {
                binding.tilRegisterPassword.error = "Kata sandi minimal 6 karakter"
                return@setOnClickListener
            } else {
                binding.tilRegisterPassword.error = null
            }

            repository.login(name, email, "Manual")
            AppToast.show(requireContext(), "Selamat datang di Cinema Plus, $name!")
            onRegisterSuccess()
            dismiss()
        }

        // Switch to Sign In
        binding.tvSwitchToSignIn.setOnClickListener {
            dismiss()
            onSwitchToLogin?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CreateAccountBottomSheet"
    }
}
