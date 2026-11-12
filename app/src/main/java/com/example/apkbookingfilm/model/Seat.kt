package com.example.apkbookingfilm.model

enum class SeatStatus {
    AVAILABLE,
    RESERVED,
    SELECTED
}

data class Seat(
    val id: String, // e.g. "C4"
    val row: String, // "C"
    val number: Int, // 4
    var status: SeatStatus = SeatStatus.AVAILABLE,
    val price: Int = 60000
)
