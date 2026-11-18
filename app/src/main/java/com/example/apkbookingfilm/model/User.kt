package com.example.apkbookingfilm.model

import java.io.Serializable

data class User(
    val name: String = "Cinema Lover",
    val email: String = "",
    val avatarInitials: String = "CL",
    val avatarUri: String = "",
    val moviesCount: Int = 0,
    val savedCount: Int = 0,
    val reviewsCount: Int = 0,
    val points: Int = 0,
    val membershipTier: String = "MEMBER BARU",
    val authProvider: String = "Google",
    val isLoggedIn: Boolean = false
) : Serializable
