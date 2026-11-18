package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemMovieCardBinding
import com.example.apkbookingfilm.databinding.ItemMovieGridBinding
import com.example.apkbookingfilm.databinding.ItemMovieSavedBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

enum class MovieViewType {
    CARD_HORIZONTAL,
    SAVED_VERTICAL,
    GRID
}

class MovieAdapter(
    private var movies: List<Movie>,
    private val viewType: MovieViewType = MovieViewType.CARD_HORIZONTAL,
    private val onMovieClick: (Movie) -> Unit,
    private val onBookClick: ((Movie) -> Unit)? = null,
    private val onRemoveSavedClick: ((Movie) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateList(newList: List<Movie>) {
        movies = newList
        notifyDataSetChanged()
    }

    fun appendList(more: List<Movie>) {
        val existing = movies.map { it.id }.toHashSet()
        val toAdd = more.filter { !existing.contains(it.id) }
        if (toAdd.isEmpty()) return
        val start = movies.size
        movies = movies + toAdd
        notifyItemRangeInserted(start, toAdd.size)
    }

    override fun getItemCount(): Int = movies.size

    override fun getItemViewType(position: Int): Int = viewType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeOrdinal: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewTypeOrdinal) {
            MovieViewType.CARD_HORIZONTAL.ordinal ->
                MovieCardViewHolder(ItemMovieCardBinding.inflate(inflater, parent, false))
            MovieViewType.GRID.ordinal ->
                MovieGridViewHolder(ItemMovieGridBinding.inflate(inflater, parent, false))
            else ->
                MovieSavedViewHolder(ItemMovieSavedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val movie = movies[position]
        when (holder) {
            is MovieCardViewHolder -> holder.bind(movie)
            is MovieSavedViewHolder -> holder.bind(movie)
            is MovieGridViewHolder -> holder.bind(movie)
        }
    }

    inner class MovieCardViewHolder(private val binding: ItemMovieCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPoster, movie)
            binding.tvTitle.text = movie.title
            binding.tvGenre.text = movie.genre
            binding.tvDuration.text = if (movie.isComingSoon && movie.releaseDate.isNotEmpty())
                "Rilis ${movie.releaseDate}" else movie.duration
            binding.tvRating.text = movie.rating
            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }

    inner class MovieGridViewHolder(private val binding: ItemMovieGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPoster, movie)
            binding.tvTitle.text = movie.title
            val first = movie.genre.split(",").firstOrNull()?.trim().orEmpty()
            binding.tvGenre.text = listOf(first, movie.releaseYear).filter { it.isNotEmpty() }.joinToString(" • ")
            binding.tvRating.text = movie.rating
            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }

    inner class MovieSavedViewHolder(private val binding: ItemMovieSavedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPoster, movie)
            binding.tvTitle.text = movie.title
            binding.tvGenre.text = "${movie.genre} • ${movie.duration}"
            binding.tvRating.text = "${movie.rating} / 10"

            binding.btnBook.setOnClickListener {
                onBookClick?.invoke(movie) ?: onMovieClick(movie)
            }
            binding.ivRemoveSaved.setOnClickListener {
                onRemoveSavedClick?.invoke(movie)
            }
            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }
}
