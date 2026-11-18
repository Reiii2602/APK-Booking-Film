package com.example.apkbookingfilm.model

import java.io.Serializable

data class Booking(
    val bookingId: String,
    val movie: Movie,
    val cinema: Cinema,
    val date: String,
    val time: String,
    val seats: List<String>,
    val subtotal: Int,
    val serviceFee: Int = 0,
    val totalPrice: Int,
    val paymentMethod: String = "Bank Transfer BCA",
    val paymentProofUploaded: Boolean = false,
    val isPaid: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val foodItems: List<FoodOrderItem> = emptyList(),
    val foodTotal: Int = 0,
    val proofUri: String = ""
) : Serializable
