package com.example.apkbookingfilm.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apkbookingfilm.R
import com.example.apkbookingfilm.model.CastMember
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
            Glide.with(imageView.context).clear(imageView)
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
            Glide.with(imageView.context).clear(imageView)
            imageView.setImageResource(movie.backdropResId)
        }
    }

    fun loadCast(imageView: ImageView, cast: CastMember) {
        val url = cast.profileUrl
        if (url.isNotEmpty()) {
            Glide.with(imageView.context)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.bg_cast_placeholder)
                .error(R.drawable.bg_cast_placeholder)
                .into(imageView)
        } else {
            Glide.with(imageView.context).clear(imageView)
            imageView.setImageResource(R.drawable.bg_cast_placeholder)
        }
    }
}
