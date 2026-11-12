package com.example.apkbookingfilm

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: MovieRepository

    private var currentTabIndex = 0

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
                    val bottomSheet = LoginBottomSheet {
                        val currentFrag = supportFragmentManager.findFragmentById(R.id.containerContent)
                        if (currentFrag is ProfileFragment) {
                            currentFrag.updateProfileUI()
                        }
                    }
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
}
