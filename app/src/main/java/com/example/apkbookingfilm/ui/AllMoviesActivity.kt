package com.example.apkbookingfilm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.adapter.MovieAdapter
import com.example.apkbookingfilm.adapter.MovieViewType
import com.example.apkbookingfilm.data.MovieRepository
import com.example.apkbookingfilm.data.TmdbApi
import com.example.apkbookingfilm.data.TmdbApi.Category
import com.example.apkbookingfilm.databinding.ActivityAllMoviesBinding
import java.util.concurrent.Executors

/**
 * Jelajahi seluruh katalog film TMDB: per kategori (infinite scroll) + pencarian.
 */
class AllMoviesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllMoviesBinding
    private lateinit var repository: MovieRepository
    private lateinit var adapter: MovieAdapter

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentCategory: Category = Category.NOW_PLAYING
    private var currentQuery: String = ""
    private var page = 0
    private var totalPages = 1
    private var isLoadingPage = false
    private var requestToken = 0
    private val chipViews = mutableMapOf<Category, TextView>()
    private var pendingSearch: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = MovieRepository(this)

        currentCategory = intent.getStringExtra(EXTRA_CATEGORY)
            ?.let { name -> Category.values().firstOrNull { it.name == name } }
            ?: Category.NOW_PLAYING

        adapter = MovieAdapter(
            movies = emptyList(),
            viewType = MovieViewType.GRID,
            onMovieClick = { movie ->
                startActivity(Intent(this, MovieDetailActivity::class.java).apply {
                    putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
                })
            }
        )
        val layoutManager = GridLayoutManager(this, 3)
        binding.rvAllMovies.layoutManager = layoutManager
        binding.rvAllMovies.adapter = adapter
        binding.rvAllMovies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (!isLoadingPage && page < totalPages && lastVisible >= adapter.itemCount - 9) {
                    loadNextPage()
                }
            }
        })

        binding.btnBackAll.setOnClickListener { finish() }
        setupChips()
        setupSearch()

        // Tampilkan dulu data yang sudah ada (cache), lalu muat dari TMDB
        adapter.updateList(repository.getCategory(currentCategory))
        resetAndLoad()

        if (intent.getBooleanExtra(EXTRA_FOCUS_SEARCH, false)) {
            binding.etSearchMovie.requestFocus()
            binding.etSearchMovie.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etSearchMovie, InputMethodManager.SHOW_IMPLICIT)
            }, 250)
        }
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()

    private fun setupChips() {
        val container = binding.layoutCategoryChips
        container.removeAllViews()
        Category.values().forEach { cat ->
            val chip = TextView(this).apply {
                text = cat.label
                textSize = 13f
                setPadding(dp(16), dp(8), dp(16), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener {
                    if (currentQuery.isNotEmpty()) {
                        binding.etSearchMovie.setText("")
                    }
                    if (currentCategory != cat || currentQuery.isNotEmpty()) {
                        currentCategory = cat
                        currentQuery = ""
                        updateChips()
                        adapter.updateList(repository.getCategory(cat))
                        binding.rvAllMovies.scrollToPosition(0)
                        resetAndLoad()
                    }
                }
            }
            chipViews[cat] = chip
            container.addView(chip)
        }
        updateChips()
    }

    private fun updateChips() {
        chipViews.forEach { (cat, chip) ->
            val selected = currentQuery.isEmpty() && cat == currentCategory
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(getColor(if (selected) R.color.white else R.color.chip_text))
            chip.paint.isFakeBoldText = selected
        }
        binding.tvAllTitle.text = if (currentQuery.isNotEmpty()) "Hasil: \"$currentQuery\"" else currentCategory.label
    }

    private fun setupSearch() {
        binding.etSearchMovie.addTextChangedListener { text ->
            val q = text?.toString()?.trim().orEmpty()
            binding.btnClearSearch.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
            pendingSearch?.let { mainHandler.removeCallbacks(it) }
            val r = Runnable { applySearch(q) }
            pendingSearch = r
            mainHandler.postDelayed(r, 500)
        }
        binding.etSearchMovie.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                pendingSearch?.let { mainHandler.removeCallbacks(it) }
                applySearch(v.text?.toString()?.trim().orEmpty())
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }
        binding.btnClearSearch.setOnClickListener { binding.etSearchMovie.setText("") }
    }

    private fun applySearch(q: String) {
        if (q == currentQuery) return
        currentQuery = q
        updateChips()
        if (q.isEmpty()) {
            adapter.updateList(repository.getCategory(currentCategory))
        } else {
            adapter.updateList(emptyList())
        }
        binding.rvAllMovies.scrollToPosition(0)
        resetAndLoad()
    }

    private fun resetAndLoad() {
        page = 0
        totalPages = 1
        isLoadingPage = false
        requestToken++
        loadNextPage(replace = true)
    }

    private fun loadNextPage(replace: Boolean = false) {
        if (isLoadingPage) return
        isLoadingPage = true
        binding.progressAll.visibility = View.VISIBLE
        binding.tvAllEmpty.visibility = View.GONE

        val token = requestToken
        val nextPage = page + 1
        val query = currentQuery
        val category = currentCategory

        executor.execute {
            val result = if (query.isNotEmpty()) TmdbApi.search(query, nextPage)
            else TmdbApi.fetchCategory(category, nextPage)

            mainHandler.post {
                if (isFinishing || isDestroyed || token != requestToken) return@post
                isLoadingPage = false
                binding.progressAll.visibility = View.GONE
                if (result == null) {
                    if (adapter.itemCount == 0) {
                        binding.tvAllEmpty.text = "Gagal memuat film.\nPeriksa koneksi internet kamu."
                        binding.tvAllEmpty.visibility = View.VISIBLE
                    }
                    return@post
                }
                page = result.page
                totalPages = minOf(result.totalPages, 500) // batas maksimum TMDB
                MovieRepository.upsertMovies(result.movies)
                // pakai versi dari repository (bisa jadi sudah punya detail lengkap)
                val list = result.movies.map { repository.getMovieById(it.id) ?: it }
                if (replace) adapter.updateList(list) else adapter.appendList(list)

                if (adapter.itemCount == 0) {
                    binding.tvAllEmpty.text = "Film tidak ditemukan"
                    binding.tvAllEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingSearch?.let { mainHandler.removeCallbacks(it) }
        executor.shutdownNow()
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_FOCUS_SEARCH = "extra_focus_search"

        fun open(context: Context, category: Category, focusSearch: Boolean = false) {
            context.startActivity(Intent(context, AllMoviesActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY, category.name)
                putExtra(EXTRA_FOCUS_SEARCH, focusSearch)
            })
        }
    }
}
