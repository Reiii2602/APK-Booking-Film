package com.example.apkbookingfilm.model

import java.io.Serializable

data class User(
    val name: String = "Daffa Fadhul Rahman",
    val email: String = "daffasigma@gmail.com",
    val avatarInitials: String = "DF",
    val moviesCount: Int = 12,
    val savedCount: Int = 6,
    val reviewsCount: Int = 8,
    val points: Int = 1240,
    val membershipTier: String = "PLUS MEMBER • SINCE 2024",
    val authProvider: String = "Google",
    val isLoggedIn: Boolean = false
) : Serializable
