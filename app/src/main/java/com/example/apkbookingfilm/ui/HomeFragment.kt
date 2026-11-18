package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.adapter.MovieAdapter
import com.example.apkbookingfilm.adapter.MovieViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentHomeBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    private val onMoviesUpdatedListener = {
        if (_binding != null) {
            setupNowShowing()
            setupComingSoon()
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
        setupHighlight()
        setupNowShowing()
        setupComingSoon()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
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
        val highlightMovie = repository.movies.firstOrNull { it.isHighlight } ?: repository.movies[0]
        TmdbImageLoader.loadBackdrop(binding.ivHighlightPoster, highlightMovie)

        binding.tvHighlightTitle.text = highlightMovie.title
        binding.tvHighlightSubtitle.text = "${highlightMovie.genre} • ${highlightMovie.duration} • ${highlightMovie.ageRating}"

        val openDetail = {
            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, highlightMovie.id)
            }
            startActivity(intent)
        }

        binding.cardHighlight.setOnClickListener { openDetail() }
        binding.btnHighlightChooseSeats.setOnClickListener { openDetail() }
    }

    private fun setupNowShowing() {
        val nowShowingMovies = repository.movies.filter { it.isNowShowing }
        binding.rvNowShowing.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNowShowing.adapter = MovieAdapter(
            movies = nowShowingMovies,
            viewType = MovieViewType.CARD_HORIZONTAL,
            onMovieClick = { movie -> openMovieDetail(movie) }
        )
    }

    private fun setupComingSoon() {
        val comingSoonMovies = repository.movies.filter { it.isComingSoon }
        binding.rvComingSoon.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvComingSoon.adapter = MovieAdapter(
            movies = comingSoonMovies,
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
            val firstMovie = repository.movies[0]
            openMovieDetail(firstMovie)
        }
        binding.btnSearchHome.setOnClickListener {
            (activity as? MainActivity)?.selectTab(2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MovieRepository.removeOnMoviesUpdatedListener(onMoviesUpdatedListener)
        _binding = null
    }
}
