package com.example.apkbookingfilm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.databinding.ItemMovieCardBinding
import com.example.apkbookingfilm.databinding.ItemMovieGridBinding
import com.example.apkbookingfilm.databinding.ItemMovieNowShowingBinding
import com.example.apkbookingfilm.databinding.ItemMovieSavedBinding
import com.example.apkbookingfilm.databinding.ItemSeeMoreArrowBinding
import com.example.apkbookingfilm.model.Movie
import com.example.apkbookingfilm.util.TmdbImageLoader

enum class MovieViewType {
    CARD_HORIZONTAL,
    SAVED_VERTICAL,
    GRID,
    NOW_SHOWING_CAROUSEL
}

class MovieAdapter(
    private var movies: List<Movie>,
    private val viewType: MovieViewType = MovieViewType.CARD_HORIZONTAL,
    private val onMovieClick: (Movie) -> Unit,
    private val onBookClick: ((Movie) -> Unit)? = null,
    private val onRemoveSavedClick: ((Movie) -> Unit)? = null,
    private val onSeeMoreClick: (() -> Unit)? = null,
    private val infiniteLoop: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SEE_MORE = 99
    }

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

    fun getItemAt(position: Int): Movie? {
        if (movies.isEmpty()) return null
        return if (infiniteLoop && movies.size > 1) {
            movies[position.mod(movies.size)]
        } else {
            movies.getOrNull(position)
        }
    }

    fun getRealCount(): Int = movies.size

    private fun isInfinite(): Boolean = infiniteLoop && movies.size > 1

    private fun hasSeeMore(): Boolean = !isInfinite() && onSeeMoreClick != null && movies.isNotEmpty()

    override fun getItemCount(): Int {
        if (isInfinite()) return Int.MAX_VALUE
        return if (hasSeeMore()) movies.size + 1 else movies.size
    }

    override fun getItemViewType(position: Int): Int {
        if (hasSeeMore() && position == movies.size) return VIEW_TYPE_SEE_MORE
        return viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeOrdinal: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewTypeOrdinal == VIEW_TYPE_SEE_MORE) {
            return SeeMoreViewHolder(ItemSeeMoreArrowBinding.inflate(inflater, parent, false))
        }
        return when (viewTypeOrdinal) {
            MovieViewType.CARD_HORIZONTAL.ordinal ->
                MovieCardViewHolder(ItemMovieCardBinding.inflate(inflater, parent, false))
            MovieViewType.NOW_SHOWING_CAROUSEL.ordinal ->
                NowShowingViewHolder(ItemMovieNowShowingBinding.inflate(inflater, parent, false))
            MovieViewType.GRID.ordinal ->
                MovieGridViewHolder(ItemMovieGridBinding.inflate(inflater, parent, false))
            else ->
                MovieSavedViewHolder(ItemMovieSavedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SeeMoreViewHolder) {
            holder.bind()
            return
        }
        val movie = getItemAt(position) ?: return
        when (holder) {
            is MovieCardViewHolder -> holder.bind(movie)
            is NowShowingViewHolder -> holder.bind(movie)
            is MovieSavedViewHolder -> holder.bind(movie)
            is MovieGridViewHolder -> holder.bind(movie)
        }
    }

    inner class SeeMoreViewHolder(private val binding: ItemSeeMoreArrowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener { onSeeMoreClick?.invoke() }
        }
    }

    inner class NowShowingViewHolder(private val binding: ItemMovieNowShowingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPosterNow, movie)
            binding.tvRatingNow.text = movie.rating
            binding.tvBadgeNow.text = if (movie.isComingSoon) "PRE-SALE" else "SEDANG TAYANG"
            binding.root.setOnClickListener { onMovieClick(movie) }
            binding.btnBuyTicketNow.setOnClickListener {
                onBookClick?.invoke(movie) ?: onMovieClick(movie)
            }
        }
    }

    inner class MovieCardViewHolder(private val binding: ItemMovieCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            TmdbImageLoader.loadPoster(binding.ivPoster, movie)
            binding.tvTitle.text = movie.title
            binding.tvGenre.text = movie.genre
            binding.tvDuration.text = if (movie.isComingSoon && movie.releaseDate.isNotEmpty())
                "Rilis: ${movie.releaseDate}" else movie.duration
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
