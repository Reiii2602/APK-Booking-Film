package com.example.apkbookingfilm.model

import java.io.Serializable

data class FoodItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: String, // "Combo", "Popcorn", "Minuman", "Snack"
    val badge: String = ""
) : Serializable

data class FoodOrderItem(
    val item: FoodItem,
    var quantity: Int
) : Serializable {
    val subtotal: Int get() = item.price * quantity
}
