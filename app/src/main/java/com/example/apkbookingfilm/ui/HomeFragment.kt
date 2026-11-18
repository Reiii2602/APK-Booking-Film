package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.adapter.MovieAdapter
import com.example.apkbookingfilm.adapter.MovieViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.data.TmdbApi.Category
import com.example.apkbookingfilm.databinding.FragmentHomeBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private val onMoviesUpdatedListener = {
        if (_binding != null) {
            refreshAll()
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

        setupGreeting()
        refreshAll()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
    }

    private fun refreshAll() {
        setupHighlight()
        setupRow(binding.rvNowShowing, Category.NOW_PLAYING)
        setupRow(binding.rvPopular, Category.POPULAR)
        setupRow(binding.rvTopRated, Category.TOP_RATED)
        setupRow(binding.rvComingSoon, Category.UPCOMING)
    }

    private fun setupGreeting() {
        val user = repository.getUser()
        if (user.isLoggedIn) {
            val firstName = user.name.split(" ").firstOrNull() ?: user.name
            binding.tvWelcomeUser.text = "Welcome back, $firstName"
        } else {
            binding.tvWelcomeUser.text = "Welcome back"
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

    private fun setupRow(recyclerView: RecyclerView, category: Category) {
        val movies = repository.getCategory(category).take(20)
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
            onMovieClick = { movie -> openMovieDetail(movie) }
        )
    }

    private fun openMovieDetail(movie: Movie) {
        val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
            putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
        }
        startActivity(intent)
    }

    private fun setupListeners() {
        binding.cardTheaterShortcut.setOnClickListener {
            (activity as? MainActivity)?.selectTab(2)
        }
        binding.tvSeeAllNowShowing.setOnClickListener {
            AllMoviesActivity.open(requireContext(), Category.NOW_PLAYING)
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

    override fun onDestroyView() {
        super.onDestroyView()
        MovieRepository.removeOnMoviesUpdatedListener(onMoviesUpdatedListener)
        _binding = null
    }
}
