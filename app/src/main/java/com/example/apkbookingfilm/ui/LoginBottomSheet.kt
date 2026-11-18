package com.example.apkbookingfilm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.BottomSheetLoginBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LoginBottomSheet(
    private val onLoginSuccess: () -> Unit
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
            repository.login("Daffa Fadhul Rahman", "daffasigma@gmail.com", "Google")
            Toast.makeText(
                requireContext(),
                "Signed in successfully with Google!",
                Toast.LENGTH_SHORT
            ).show()
            onLoginSuccess()
            dismiss()
        }

        // Quick Demo 1-Tap Google Login
        binding.btnQuickDemoLogin.setOnClickListener {
            repository.login("Daffa Fadhul Rahman", "daffasigma@gmail.com", "Google")
            Toast.makeText(
                requireContext(),
                "Welcome back, Daffa Fadhul Rahman!",
                Toast.LENGTH_SHORT
            ).show()
            onLoginSuccess()
            dismiss()
        }

        // Continue with Apple
        binding.btnContinueWithApple.setOnClickListener {
            repository.login("Daffa Fadhul Rahman", "daffasigma@gmail.com", "Apple")
            Toast.makeText(
                requireContext(),
                "Signed in successfully with Apple!",
                Toast.LENGTH_SHORT
            ).show()
            onLoginSuccess()
            dismiss()
        }

        // Email Sign In
        binding.btnEmailSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim()
            if (email.isNullOrEmpty()) {
                binding.tilEmail.error = "Please enter an email"
                return@setOnClickListener
            }
            val name = if (email.contains("@")) {
                email.substringBefore("@").replace(".", " ").capitalizeWords()
            } else {
                "Daffa Fadhul Rahman"
            }
            repository.login(name, email, "Email")
            Toast.makeText(
                requireContext(),
                "Signed in successfully as $name",
                Toast.LENGTH_SHORT
            ).show()
            onLoginSuccess()
            dismiss()
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
