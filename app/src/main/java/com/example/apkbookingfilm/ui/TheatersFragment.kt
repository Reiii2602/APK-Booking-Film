package com.example.apkbookingfilm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkbookingfilm.MainActivity
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.CinemaAdapter
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.databinding.FragmentTheatersBinding

class TheatersFragment : Fragment() {

    private var _binding: FragmentTheatersBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MovieRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTheatersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MovieRepository(requireContext())

        setupTheatersList()
        setupListeners()
    }

    private fun setupTheatersList() {
        binding.rvTheaters.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CinemaAdapter(repository.cinemas) { cinema, time ->
            val firstMovie = repository.movies[0]
            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, firstMovie.id)
            }
            startActivity(intent)
        }
        binding.rvTheaters.adapter = adapter

        binding.etSearchTheater.addTextChangedListener { text ->
            val query = text?.toString()?.trim() ?: ""
            val filtered = if (query.isEmpty()) {
                repository.cinemas
            } else {
                repository.cinemas.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.location.contains(query, ignoreCase = true)
                }
            }
            binding.rvTheaters.adapter = CinemaAdapter(filtered) { _, _ ->
                val firstMovie = repository.movies[0]
                val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                    putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, firstMovie.id)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupListeners() {
        binding.btnTheatersBack.setOnClickListener {
            (activity as? MainActivity)?.selectTab(0)
        }

        binding.btnTheatersBookmark.setOnClickListener {
            (activity as? MainActivity)?.selectTab(1)
        }

        binding.chipFilterNearby.setOnClickListener {
            resetFilterChips()
            binding.chipFilterNearby.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipFilterNearby.setTextColor(requireContext().getColor(R.color.white))
        }

        binding.chipFilterImax.setOnClickListener {
            resetFilterChips()
            binding.chipFilterImax.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipFilterImax.setTextColor(requireContext().getColor(R.color.white))
        }

        binding.chipFilterDolby.setOnClickListener {
            resetFilterChips()
            binding.chipFilterDolby.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipFilterDolby.setTextColor(requireContext().getColor(R.color.white))
        }

        binding.chipFilterOpenNow.setOnClickListener {
            resetFilterChips()
            binding.chipFilterOpenNow.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipFilterOpenNow.setTextColor(requireContext().getColor(R.color.white))
        }
    }

    private fun resetFilterChips() {
        val chips = listOf(
            binding.chipFilterNearby,
            binding.chipFilterImax,
            binding.chipFilterDolby,
            binding.chipFilterOpenNow
        )
        chips.forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
            it.setTextColor(requireContext().getColor(R.color.chip_text))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
