package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.BottomSheetLoginBinding
import com.example.apkbookingfilm.util.AppToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LoginBottomSheet(
    private val onLoginSuccess: () -> Unit,
    private val onSwitchToRegister: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLoginBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = MovieRepository(requireContext())

        // Continue with Google
        binding.btnContinueWithGoogle.setOnClickListener {
            repository.login("Cinema User", "user@gmail.com", "Google")
            AppToast.show(requireContext(), "Berhasil masuk dengan Google")
            onLoginSuccess()
            dismiss()
        }

        // Continue with Apple
        binding.btnContinueWithApple.setOnClickListener {
            repository.login("Cinema User", "user@icloud.com", "Apple")
            AppToast.show(requireContext(), "Berhasil masuk dengan Apple")
            onLoginSuccess()
            dismiss()
        }

        // Email Sign In
        binding.btnEmailSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim()
            if (email.isNullOrEmpty()) {
                binding.tilEmail.error = "Masukkan email terlebih dahulu"
                return@setOnClickListener
            }
            val name = if (email.contains("@")) {
                email.substringBefore("@").replace(".", " ").capitalizeWords()
            } else {
                "Cinema User"
            }
            repository.login(name, email, "Email")
            AppToast.show(requireContext(), "Selamat datang, $name!")
            onLoginSuccess()
            dismiss()
        }

        // Switch to Register
        binding.tvSwitchToRegister.setOnClickListener {
            dismiss()
            onSwitchToRegister?.invoke()
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LoginBottomSheet"
    }
}
