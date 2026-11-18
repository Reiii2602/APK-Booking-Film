package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.apkbookingfilm.BuildConfig
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentProfileBinding
import com.example.apkbookingfilm.update.UpdateManager
import com.example.apkbookingfilm.util.AppToast
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private val onCityChangedListener = {
        if (_binding != null) updateCityViews()
    }

    private val onProfileChangedListener = {
        if (_binding != null) updateProfileUI()
    }

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
        MovieRepository.addOnCityChangedListener(onCityChangedListener)
        MovieRepository.addOnProfileChangedListener(onProfileChangedListener)

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

    private fun updateCityViews() {
        val city = repository.getSelectedCity()
        binding.tvPreferredCity.text = city
        binding.tvPreferredCityGuest.text = city
    }

    fun updateProfileUI() {
        updateCityViews()

        val user = repository.getUser()
        if (user.isLoggedIn) {
            binding.layoutNotLoggedIn.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE
            binding.tvProfileSubtitle.text = "Manage your account, tickets and preferences."

            binding.tvAvatarInitials.text = user.avatarInitials
            if (user.avatarUri.isNotEmpty() && File(user.avatarUri).exists()) {
                binding.ivAvatarPhoto.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(user.avatarUri))
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(binding.ivAvatarPhoto)
            } else {
                binding.ivAvatarPhoto.visibility = View.GONE
            }

            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email.ifEmpty { "Ketuk ikon pensil untuk lengkapi email" }
            binding.tvMembershipTier.text = user.membershipTier

            binding.tvTicketsBookedCount.text = user.moviesCount.toString()
            binding.tvMyTicketsCountRow.text = user.moviesCount.toString()
            binding.tvSavedFilmsCount.text = user.savedCount.toString()
            binding.tvPointsValue.text = NumberFormat.getIntegerInstance(Locale("in", "ID")).format(user.points)

            val latest = repository.getLatestBooking()
            if (latest != null) {
                binding.cardNextScreening.visibility = View.VISIBLE
                binding.tvNextMovieTitle.text = latest.movie.title
                val seatStr = if (latest.seats.isNotEmpty()) " • Seats ${latest.seats.joinToString(", ")}" else ""
                binding.tvNextMovieSchedule.text = "${latest.date} • ${latest.time} • ${latest.cinema.name}$seatStr"
            } else {
                binding.cardNextScreening.visibility = View.GONE
            }

            val payMethod = repository.getSelectedPaymentMethod()
            binding.ivPaymentMethodRow.setImageResource(payMethod.logoResId)
            binding.tvPaymentMethodValue.text = payMethod.name

            updateNotificationRowUI()
        } else {
            binding.layoutNotLoggedIn.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            binding.tvProfileSubtitle.text = "Sign in to see your tickets, saved films and rewards."
        }
    }

    private fun updateNotificationRowUI() {
        val enabled = repository.areNotificationsEnabled()
        binding.tvNotificationsStatus.text = if (enabled) "On" else "Off"
        binding.tvNotificationsStatus.setTextColor(
            ContextCompat.getColor(requireContext(), if (enabled) R.color.green_success else R.color.text_muted)
        )
    }

    private fun setupListeners() {
        binding.btnProfileBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(0)
        }

        binding.btnProfileBookmark.setOnClickListener {
            (activity as? MainActivity)?.selectTab(1)
        }

        // Edit Profile (Nickname, Email, Photo)
        val openEditProfile = {
            EditProfileBottomSheet { updateProfileUI() }.show(parentFragmentManager, EditProfileBottomSheet.TAG)
        }
        binding.btnEditProfile.setOnClickListener { openEditProfile() }
        binding.layoutAvatarProfile.setOnClickListener { openEditProfile() }

        // Not Logged In State buttons
        binding.btnSignInWhite.setOnClickListener { showLoginBottomSheet() }
        binding.btnCreateAccount.setOnClickListener { showLoginBottomSheet() }

        binding.btnGoogleQuickLogin.setOnClickListener {
            repository.login("Cinema User", "user@gmail.com", "Google")
            AppToast.show(requireContext(), "Berhasil masuk dengan Google")
            updateProfileUI()
        }

        binding.btnAppleQuickLogin.setOnClickListener {
            repository.login("Cinema User", "user@icloud.com", "Apple")
            AppToast.show(requireContext(), "Berhasil masuk dengan Apple")
            updateProfileUI()
        }

        // Next Screening View E-Ticket
        binding.btnViewETicket.setOnClickListener {
            val booking = repository.getLatestBooking()
            if (booking == null) {
                AppToast.show(requireContext(), "Belum ada tiket aktif")
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), TicketSuccessActivity::class.java).apply {
                putExtra(TicketSuccessActivity.EXTRA_BOOKING, booking)
            })
        }

        // Account rows
        binding.btnRowMyTickets.setOnClickListener {
            val booking = repository.getLatestBooking()
            if (booking == null) {
                AppToast.show(requireContext(), "Belum ada tiket aktif. Yuk pesan film dulu!")
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), TicketSuccessActivity::class.java).apply {
                putExtra(TicketSuccessActivity.EXTRA_BOOKING, booking)
            })
        }

        binding.btnRowPaymentMethods.setOnClickListener {
            PaymentMethodBottomSheet { method ->
                binding.ivPaymentMethodRow.setImageResource(method.logoResId)
                binding.tvPaymentMethodValue.text = method.name
                AppToast.show(requireContext(), "Metode pembayaran: ${method.name}")
            }.show(parentFragmentManager, PaymentMethodBottomSheet.TAG)
        }

        binding.btnRowNotifications.setOnClickListener {
            val next = !repository.areNotificationsEnabled()
            repository.setNotificationsEnabled(next)
            updateNotificationRowUI()
            AppToast.show(requireContext(), if (next) "Notifikasi: ON" else "Notifikasi: OFF")
        }

        val openCityPicker = {
            CityPickerDialog { chosen ->
                updateCityViews()
                AppToast.show(requireContext(), "Kota diubah ke $chosen")
            }.show(parentFragmentManager, CityPickerDialog.TAG)
        }
        binding.btnRowPreferredCity.setOnClickListener { openCityPicker() }
        binding.btnRowPreferredCityGuest.setOnClickListener { openCityPicker() }

        binding.btnRowHelpSupport.setOnClickListener {
            AppToast.show(requireContext(), "Cinema Support • support@cinemabooking.com")
        }

        binding.tvAppVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.btnRowCheckUpdate.setOnClickListener {
            AppToast.show(requireContext(), "Memeriksa pembaruan...")
            UpdateManager.checkForUpdate(requireActivity(), forceDialog = true, showUpToDateToast = true)
        }

        binding.btnLogout.setOnClickListener {
            ThemedConfirmBottomSheet(
                badgeText = "KELUAR AKUN",
                title = "Log Out",
                message = "Yakin ingin keluar dari akun kamu?",
                positiveText = "LOG OUT",
                negativeText = "BATAL"
            ) {
                repository.logout()
                updateProfileUI()
                AppToast.show(requireContext(), "Berhasil keluar akun")
            }.show(parentFragmentManager, ThemedConfirmBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MovieRepository.removeOnCityChangedListener(onCityChangedListener)
        MovieRepository.removeOnProfileChangedListener(onProfileChangedListener)
        _binding = null
    }
}
