package com.example.apkbookingfilm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.ActivityMainBinding
import com.example.apkbookingfilm.ui.HomeFragment
import com.example.apkbookingfilm.ui.LoginBottomSheet
import com.example.apkbookingfilm.ui.ProfileFragment
import com.example.apkbookingfilm.ui.SavedFragment
import com.example.apkbookingfilm.ui.TheatersFragment
import com.example.apkbookingfilm.update.UpdateManager
import com.example.apkbookingfilm.update.UpdateNotifier
import com.example.apkbookingfilm.update.UpdateWorker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: MovieRepository

    private var currentTabIndex = 0

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupBottomNav()
        selectTab(0)

        requestNotificationPermissionIfNeeded()
        UpdateNotifier.ensureChannel(this)
        UpdateWorker.schedule(this)

        val fromNotification = intent?.getBooleanExtra(EXTRA_SHOW_UPDATE, false) == true
        UpdateManager.checkForUpdate(this, forceDialog = fromNotification)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_UPDATE, false)) {
            UpdateManager.checkForUpdate(this, forceDialog = true)
        }
    }

    override fun onResume() {
        super.onResume()
        UpdateManager.onResume(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupBottomNav() {
        binding.tabTickets.setOnClickListener { selectTab(0) }
        binding.tabSaved.setOnClickListener { selectTab(1) }
        binding.tabTheaters.setOnClickListener { selectTab(2) }
        binding.tabProfile.setOnClickListener {
            selectTab(3)
            if (!repository.isLoggedIn()) {
                val existing = supportFragmentManager.findFragmentByTag(LoginBottomSheet.TAG)
                if (existing == null) {
                    val bottomSheet = LoginBottomSheet(
                        onLoginSuccess = {
                            val currentFrag = supportFragmentManager.findFragmentById(R.id.containerContent)
                            if (currentFrag is ProfileFragment) {
                                currentFrag.updateProfileUI()
                            }
                        },
                        onSwitchToRegister = {
                            val currentFrag = supportFragmentManager.findFragmentById(R.id.containerContent)
                            if (currentFrag is ProfileFragment) {
                                currentFrag.showCreateAccountBottomSheet()
                            }
                        }
                    )
                    bottomSheet.show(supportFragmentManager, LoginBottomSheet.TAG)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        currentTabIndex = index

        val fragment: Fragment = when (index) {
            0 -> HomeFragment()
            1 -> SavedFragment()
            2 -> TheatersFragment()
            3 -> ProfileFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.containerContent, fragment)
            .commit()

        updateTabIcons(index)
    }

    private fun updateTabIcons(selectedIndex: Int) {
        val tabIcons = listOf(binding.ivTabTickets, binding.ivTabSaved, binding.ivTabTheaters, binding.ivTabProfile)
        val tabTexts = listOf(binding.tvTabTickets, binding.tvTabSaved, binding.tvTabTheaters, binding.tvTabProfile)

        val activeColor = ContextCompat.getColor(this, R.color.maroon_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_muted)

        for (i in tabIcons.indices) {
            if (i == selectedIndex) {
                tabIcons[i].setColorFilter(activeColor)
                tabTexts[i].setTextColor(activeColor)
                tabTexts[i].typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                tabIcons[i].setColorFilter(inactiveColor)
                tabTexts[i].setTextColor(inactiveColor)
                tabTexts[i].typeface = android.graphics.Typeface.DEFAULT
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_UPDATE = "extra_show_update"
    }
}
