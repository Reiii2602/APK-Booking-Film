package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.apkbookingfilm.BuildConfig
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentProfileBinding
import com.example.apkbookingfilm.update.UpdateManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private var hasAutoShownLoginPrompt = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        updateProfileUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
    }

    fun showLoginBottomSheet() {
        val existing = parentFragmentManager.findFragmentByTag(LoginBottomSheet.TAG)
        if (existing == null) {
            val bottomSheet = LoginBottomSheet {
                updateProfileUI()
            }
            bottomSheet.show(parentFragmentManager, LoginBottomSheet.TAG)
        }
    }

    fun updateProfileUI() {
        val user = repository.getUser()
        if (user.isLoggedIn) {
            binding.layoutNotLoggedIn.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE
            binding.tvProfileSubtitle.text = "Manage your account, tickets and preferences."

            binding.tvAvatarInitials.text = user.avatarInitials
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email

            binding.tvTicketsBookedCount.text = user.moviesCount.toString()
            binding.tvSavedFilmsCount.text = user.savedCount.toString()
            binding.tvPointsValue.text = String.format("%,d", user.points)
        } else {
            binding.layoutNotLoggedIn.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            binding.tvProfileSubtitle.text = "Sign in to see your tickets, saved films and rewards."
        }
    }

    private fun setupListeners() {
        binding.btnProfileBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(0)
        }

        binding.btnProfileBookmark.setOnClickListener {
            (activity as? MainActivity)?.selectTab(1)
        }

        // Not Logged In State buttons
        binding.btnSignInWhite.setOnClickListener {
            showLoginBottomSheet()
        }

        binding.btnCreateAccount.setOnClickListener {
            showLoginBottomSheet()
        }

        binding.btnGoogleQuickLogin.setOnClickListener {
            repository.login("Daffa Fadhul Rahman", "daffasigma@gmail.com", "Google")
            Toast.makeText(requireContext(), "Signed in with Google as Daffa Fadhul Rahman", Toast.LENGTH_SHORT).show()
            updateProfileUI()
        }

        binding.btnAppleQuickLogin.setOnClickListener {
            repository.login("Daffa Fadhul Rahman", "daffasigma@gmail.com", "Apple")
            Toast.makeText(requireContext(), "Signed in with Apple", Toast.LENGTH_SHORT).show()
            updateProfileUI()
        }

        // Next Screening View E-Ticket
        binding.btnViewETicket.setOnClickListener {
            val booking = repository.getLatestBooking()
            val intent = Intent(requireContext(), TicketSuccessActivity::class.java).apply {
                putExtra(TicketSuccessActivity.EXTRA_BOOKING, booking)
            }
            startActivity(intent)
        }

        // Account rows
        binding.btnRowMyTickets.setOnClickListener {
            val booking = repository.getLatestBooking()
            val intent = Intent(requireContext(), TicketSuccessActivity::class.java).apply {
                putExtra(TicketSuccessActivity.EXTRA_BOOKING, booking)
            }
            startActivity(intent)
        }

        binding.btnRowPaymentMethods.setOnClickListener {
            Toast.makeText(requireContext(), "Payment method: Bank BCA (Active)", Toast.LENGTH_SHORT).show()
        }

        binding.btnRowNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications: ON", Toast.LENGTH_SHORT).show()
        }

        binding.tvPreferredCity.text = repository.getSelectedCity()
        binding.btnRowPreferredCity.setOnClickListener {
            CityPickerDialog { chosen ->
                binding.tvPreferredCity.text = chosen
                Toast.makeText(requireContext(), "Kota diubah ke $chosen", Toast.LENGTH_SHORT).show()
            }.show(parentFragmentManager, CityPickerDialog.TAG)
        }

        binding.btnRowHelpSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Cinema Support • support@cinemabooking.com", Toast.LENGTH_SHORT).show()
        }

        binding.tvAppVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.btnRowCheckUpdate.setOnClickListener {
            Toast.makeText(requireContext(), "Memeriksa pembaruan...", Toast.LENGTH_SHORT).show()
            UpdateManager.checkForUpdate(requireActivity(), forceDialog = true, showUpToDateToast = true)
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your account?")
                .setPositiveButton("Log Out") { _, _ ->
                    repository.logout()
                    hasAutoShownLoginPrompt = false
                    updateProfileUI()
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
