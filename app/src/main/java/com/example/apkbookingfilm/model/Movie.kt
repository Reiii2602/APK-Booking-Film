package com.example.apkbookingfilm.model

import java.io.Serializable

data class Movie(
    val id: String,
    val title: String,
    val genre: String,
    val duration: String,
    val ageRating: String,
    val rating: String,
    val synopsis: String,
    val posterResId: Int,
    val backdropResId: Int = posterResId,
    val posterPath: String = "",
    val backdropPath: String = "",
    val director: String = "Denis Villeneuve",
    val trailerDuration: String = "2:24",
    val isHighlight: Boolean = false,
    val isNowShowing: Boolean = true,
    val isComingSoon: Boolean = false,
    val basePrice: Int = 50000
) : Serializable {
    val posterUrl: String
        get() = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w500$posterPath" else ""

    val backdropUrl: String
        get() = if (backdropPath.isNotEmpty()) "https://image.tmdb.org/t/p/w780$backdropPath" else ""
}
