package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.adapter.MovieAdapter
import com.example.apkbookingfilm.adapter.MovieViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentSavedBinding
import com.example.apkbookingfilm.model.Movie

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository
    private var movieAdapter: MovieAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        setupList()
        binding.btnSavedBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(0)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupList() {
        binding.rvSavedFilms.layoutManager = LinearLayoutManager(requireContext())
        refreshList()
    }

    private fun refreshList() {
        val savedMovies = repository.getSavedMovies()
        if (savedMovies.isEmpty()) {
            binding.layoutEmptySaved.visibility = View.VISIBLE
            binding.rvSavedFilms.visibility = View.GONE
        } else {
            binding.layoutEmptySaved.visibility = View.GONE
            binding.rvSavedFilms.visibility = View.VISIBLE

            movieAdapter = MovieAdapter(
                movies = savedMovies,
                viewType = MovieViewType.SAVED_VERTICAL,
                onMovieClick = { movie -> openMovieDetail(movie) },
                onBookClick = { movie -> openMovieDetail(movie) },
                onRemoveSavedClick = { movie ->
                    repository.toggleSaved(movie.id)
                    Toast.makeText(requireContext(), "Removed ${movie.title}", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            )
            binding.rvSavedFilms.adapter = movieAdapter
        }
    }

    private fun openMovieDetail(movie: Movie) {
        val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
            putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
