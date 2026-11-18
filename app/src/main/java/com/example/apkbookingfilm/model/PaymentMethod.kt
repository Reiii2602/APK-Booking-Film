package com.example.apkbookingfilm.model

import java.io.Serializable

data class PaymentMethod(
    val id: String,
    val name: String,
    val type: String, // "E-Wallet" atau "Transfer Bank"
    val accountNumber: String,
    val accountHolder: String,
    val logoResId: Int
) : Serializable
