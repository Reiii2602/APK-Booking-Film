package com.example.apkbookingfilm.model

import java.io.Serializable

data class Cinema(
    val id: String,
    val name: String,
    val location: String,
    val distance: String,
    val formats: List<String>,
    val showtimes: List<String>,
    val ticketPrice: Int = 50000
) : Serializable
