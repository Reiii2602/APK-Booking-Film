package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.FoodAdapter
import com.example.apkbookingfilm.adapter.FoodViewType
import com.example.apkbookingfilm.adapter.MovieAdapter
import com.example.apkbookingfilm.adapter.MovieViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.data.TmdbApi.Category
import com.example.apkbookingfilm.databinding.FragmentHomeBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.AppToast
import com.example.apkbookingfilm.util.TmdbImageLoader
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private var nowShowingFilter: String = "ALL"
    private var nowSnapAttached = false
    private val snapHelper = PagerSnapHelper()

    private val onMoviesUpdatedListener = {
        if (_binding != null) {
            refreshAll()
        }
    }

    private val onCityChangedListener = {
        if (_binding != null) {
            updateCityDisplay()
            setupGreeting()
        }
    }

    private val onProfileChangedListener = {
        if (_binding != null) {
            setupGreeting()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())
        MovieRepository.addOnMoviesUpdatedListener(onMoviesUpdatedListener)
        MovieRepository.addOnCityChangedListener(onCityChangedListener)
        MovieRepository.addOnProfileChangedListener(onProfileChangedListener)

        setupGreeting()
        updateCityDisplay()
        setupFoodSection()
        refreshAll()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
        updateCityDisplay()
    }

    private fun updateCityDisplay() {
        val city = repository.getSelectedCity()
        val cinemas = repository.getCinemasForCity(city)
        binding.tvCityHome.text = city.uppercase()
        binding.tvCityCinemaCountHome.text = "${cinemas.size} Bioskop"
        binding.tvTheaterShortcutTitle.text = "Explore ${cinemas.size} Bioskop di $city"
        binding.tvTheaterShortcutSub.text = cinemas.take(3).joinToString(", ") { it.name }
    }

    private fun refreshAll() {
        setupHighlight()
        setupNowShowingCarousel()
        setupRow(binding.rvComingSoon, Category.UPCOMING)
        setupRow(binding.rvPopular, Category.POPULAR)
        setupRow(binding.rvTopRated, Category.TOP_RATED)
    }

    private fun setupGreeting() {
        val user = repository.getUser()
        val city = repository.getSelectedCity()
        if (user.isLoggedIn) {
            binding.btnLoginTopHome.visibility = View.GONE
            binding.cardUserAvatarHome.visibility = View.VISIBLE
            binding.tvUserAvatarHome.text = user.avatarInitials
            if (user.avatarUri.isNotEmpty() && File(user.avatarUri).exists()) {
                binding.ivUserAvatarHome.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(user.avatarUri))
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(binding.ivUserAvatarHome)
            } else {
                binding.ivUserAvatarHome.visibility = View.GONE
            }
            binding.tvWelcomeUser.text = "Welcome back, ${user.name}"
            binding.tvWelcomeSubtitle.text = "Book your favorite movies & snacks today"
        } else {
            binding.btnLoginTopHome.visibility = View.VISIBLE
            binding.cardUserAvatarHome.visibility = View.GONE
            binding.tvWelcomeUser.text = "Cinema Booking"
            binding.tvWelcomeSubtitle.text = "Masuk untuk simpan tiket & kumpulkan poin"
        }
    }

    private fun setupHighlight() {
        val highlightMovie = repository.getHighlightMovie()
        TmdbImageLoader.loadBackdrop(binding.ivHighlightPoster, highlightMovie)

        binding.tvHighlightTitle.text = highlightMovie.title
        binding.tvHighlightSubtitle.text = listOf(highlightMovie.genre, highlightMovie.duration, "★ ${highlightMovie.rating}")
            .filter { it.isNotEmpty() }.joinToString(" • ")

        binding.cardHighlight.setOnClickListener { openMovieDetail(highlightMovie) }
        binding.btnHighlightChooseSeats.setOnClickListener { openMovieDetail(highlightMovie) }
    }

    private fun setupNowShowingCarousel() {
        val baseList = repository.getCategory(Category.NOW_PLAYING).take(20)
        val movies = when (nowShowingFilter) {
            "WATCHLIST" -> repository.getSavedMovies().ifEmpty { baseList }
            "XXI" -> baseList.filterIndexed { i, _ -> i % 2 == 0 || i < 3 }.ifEmpty { baseList }
            "CGV" -> baseList.filterIndexed { i, _ -> i % 3 != 1 }.ifEmpty { baseList }
            "CINEPOLIS" -> baseList.filterIndexed { i, _ -> i % 2 == 1 || i < 3 }.ifEmpty { baseList }
            else -> baseList
        }

        val existing = binding.rvNowShowing.adapter as? MovieAdapter
        if (existing != null) {
            existing.updateList(movies)
        } else {
            val lm = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.rvNowShowing.layoutManager = lm
            binding.rvNowShowing.adapter = MovieAdapter(
                movies = movies,
                viewType = MovieViewType.NOW_SHOWING_CAROUSEL,
                onMovieClick = { movie -> openMovieDetail(movie) },
                onBookClick = { movie -> openMovieDetail(movie) },
                infiniteLoop = true
            )
            if (!nowSnapAttached) {
                nowSnapAttached = true
                snapHelper.attachToRecyclerView(binding.rvNowShowing)
                binding.rvNowShowing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            updateActiveCarouselTitle()
                        }
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        updateActiveCarouselTitle()
                    }
                })
            }
        }
        centerInfiniteCarousel(movies.size)
        updateActiveMovieText(movies.firstOrNull())
    }

    private fun centerInfiniteCarousel(size: Int) {
        if (size <= 1) return
        val mid = Int.MAX_VALUE / 2
        val startPos = mid - (mid % size)
        binding.rvNowShowing.scrollToPosition(startPos)
    }

    private fun updateActiveCarouselTitle() {
        if (_binding == null) return
        val lm = binding.rvNowShowing.layoutManager as? LinearLayoutManager ?: return
        val adapter = binding.rvNowShowing.adapter as? MovieAdapter ?: return
        val centerView = snapHelper.findSnapView(lm) ?: return
        val pos = lm.getPosition(centerView)
        val movie = adapter.getItemAt(pos)
        if (movie != null) {
            updateActiveMovieText(movie)
        }
    }

    private fun updateActiveMovieText(movie: Movie?) {
        if (_binding == null || movie == null) return
        binding.tvActiveNowTitle.text = movie.title
        binding.tvActiveNowDesc.text = "${movie.genre} • ★ ${movie.rating} • Ketuk BELI TIKET untuk pilih kursi!"
    }

    private fun setupFoodSection() {
        binding.rvCinemaFood.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCinemaFood.adapter = FoodAdapter(
            items = MovieRepository.foodMenu.take(5),
            viewType = FoodViewType.HOME_CARD,
            repository = repository,
            onBuyClick = { food -> openFoodMenu(food.id) },
            onSeeMoreClick = { openFoodMenu(null) }
        )
    }

    private fun openFoodMenu(preselectId: String?) {
        FoodOrderBottomSheet(preselectFoodId = preselectId).show(parentFragmentManager, FoodOrderBottomSheet.TAG)
    }

    private fun setupRow(recyclerView: RecyclerView, category: Category) {
        val movies = repository.getCategory(category).take(15)
        val existing = recyclerView.adapter as? MovieAdapter
        if (existing != null) {
            existing.updateList(movies)
            return
        }
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = MovieAdapter(
            movies = movies,
            viewType = MovieViewType.CARD_HORIZONTAL,
            onMovieClick = { movie -> openMovieDetail(movie) },
            onSeeMoreClick = { AllMoviesActivity.open(requireContext(), category) }
        )
    }

    private fun openMovieDetail(movie: Movie) {
        val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
            putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
        }
        startActivity(intent)
    }

    private fun setupListeners() {
        binding.btnLoginTopHome.setOnClickListener {
            val showRegister: () -> Unit = {
                CreateAccountBottomSheet(
                    onRegisterSuccess = { setupGreeting() },
                    onSwitchToLogin = {
                        LoginBottomSheet(onLoginSuccess = { setupGreeting() }).show(parentFragmentManager, LoginBottomSheet.TAG)
                    }
                ).show(parentFragmentManager, CreateAccountBottomSheet.TAG)
            }
            LoginBottomSheet(
                onLoginSuccess = { setupGreeting() },
                onSwitchToRegister = showRegister
            ).show(parentFragmentManager, LoginBottomSheet.TAG)
        }

        binding.cardUserAvatarHome.setOnClickListener {
            (activity as? MainActivity)?.selectTab(3)
        }

        binding.layoutCitySelectorHome.setOnClickListener {
            CityPickerDialog {
                updateCityDisplay()
                setupGreeting()
            }.show(parentFragmentManager, CityPickerDialog.TAG)
        }

        val chips = listOf(
            binding.chipNowAll to "ALL",
            binding.chipNowXxi to "XXI",
            binding.chipNowCgv to "CGV",
            binding.chipNowCinepolis to "CINEPOLIS",
            binding.chipNowWatchlist to "WATCHLIST"
        )
        chips.forEach { (chipView, filterKey) ->
            chipView.setOnClickListener {
                if (filterKey == "WATCHLIST" && repository.getSavedMovies().isEmpty()) {
                    AppToast.show(requireContext(), "Watchlist kamu masih kosong")
                    return@setOnClickListener
                }
                nowShowingFilter = filterKey
                updateNowShowingChips(chips.map { it.first }, chipView)
                setupNowShowingCarousel()
            }
        }

        binding.cardTheaterShortcut.setOnClickListener {
            (activity as? MainActivity)?.selectTab(2)
        }
        binding.tvSeeAllNowShowing.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.NOW_PLAYING)
        }
        binding.tvSeeAllFood.setOnClickListener {
            openFoodMenu(null)
        }
        binding.tvSeeAllPopular.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.POPULAR)
        }
        binding.tvSeeAllTopRated.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.TOP_RATED)
        }
        binding.tvSeeAllComingSoon.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.UPCOMING)
        }
        binding.btnSearchHome.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.TRENDING, focusSearch = true)
        }
    }

    private fun updateNowShowingChips(allChips: List<TextView>, selected: TextView) {
        allChips.forEach { chip ->
            val isSel = chip == selected
            chip.setBackgroundResource(if (isSel) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(requireContext().getColor(if (isSel) R.color.white else R.color.chip_text))
            chip.paint.isFakeBoldText = isSel
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MovieRepository.removeOnMoviesUpdatedListener(onMoviesUpdatedListener)
        MovieRepository.removeOnCityChangedListener(onCityChangedListener)
        MovieRepository.removeOnProfileChangedListener(onProfileChangedListener)
        _binding = null
    }
}
