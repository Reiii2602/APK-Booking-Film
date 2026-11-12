package com.example.apkbookingfilm.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apkbookingfilm.model.Movie

object TmdbImageLoader {
    fun loadPoster(imageView: ImageView, movie: Movie) {
        val url = movie.posterUrl
        if (url.isNotEmpty()) {
            Glide.with(imageView.context)
                .load(url)
                .placeholder(movie.posterResId)
                .error(movie.posterResId)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            imageView.setImageResource(movie.posterResId)
        }
    }

    fun loadBackdrop(imageView: ImageView, movie: Movie) {
        val url = movie.backdropUrl
        if (url.isNotEmpty()) {
            Glide.with(imageView.context)
                .load(url)
                .placeholder(movie.backdropResId)
                .error(movie.backdropResId)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            imageView.setImageResource(movie.backdropResId)
        }
    }
}
