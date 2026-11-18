package com.example.apkbookingfilm.model

import java.io.Serializable

data class CastMember(
    val name: String,
    val character: String,
    val profilePath: String = ""
) : Serializable {
    val profileUrl: String
        get() = if (profilePath.isNotEmpty() && profilePath != "null") "https://image.tmdb.org/t/p/w185$profilePath" else ""
}

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
    val basePrice: Int = 50000,
    // ---- Data tambahan dari TMDB ----
    val tmdbId: Int = 0,
    val releaseDate: String = "",
    val voteCount: Int = 0,
    val tagline: String = "",
    val trailerKey: String = "",
    val originalLanguage: String = "",
    val cast: List<CastMember> = emptyList(),
    val hasFullDetail: Boolean = false
) : Serializable {
    val posterUrl: String
        get() = if (posterPath.isNotEmpty() && posterPath != "null") "https://image.tmdb.org/t/p/w500$posterPath" else ""

    val backdropUrl: String
        get() = if (backdropPath.isNotEmpty() && backdropPath != "null") "https://image.tmdb.org/t/p/w780$backdropPath"
        else posterUrl

    val releaseYear: String
        get() = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
}
