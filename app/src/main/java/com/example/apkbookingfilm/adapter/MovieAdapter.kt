package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemMovieCardBinding
import com.example.apkbookingfilm.databinding.ItemMovieSavedBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

enum class MovieViewType {
    CARD_HORIZONTAL,
    SAVED_VERTICAL
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

    override fun getItemCount(): Int = movies.size

    override fun getItemViewType(position: Int): Int = viewType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeOrdinal: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewTypeOrdinal == MovieViewType.CARD_HORIZONTAL.ordinal) {
            val binding = ItemMovieCardBinding.inflate(inflater, parent, false)
            MovieCardViewHolder(binding)
        } else {
            val binding = ItemMovieSavedBinding.inflate(inflater, parent, false)
            MovieSavedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val movie = movies[position]
        if (holder is MovieCardViewHolder) {
            holder.bind(movie)
        } else if (holder is MovieSavedViewHolder) {
            holder.bind(movie)
        }
    }

    inner class MovieCardViewHolder(private val binding: ItemMovieCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPoster, movie)
            binding.tvTitle.text = movie.title
            binding.tvGenre.text = movie.genre
            binding.tvDuration.text = movie.duration
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
