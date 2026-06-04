package com.example.weddingapp.data.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Long = 0,
    val status: String = "Chờ xử lý",
    val timestamp: Long = System.currentTimeMillis(),
    val trackingNumber: String? = null,
    val shippingProvider: String? = null,
    val paymentMethod: String = "COD", // "COD" hoặc "QR"
    val paymentStatus: String = "UNPAID" // "UNPAID", "PENDING", "PAID"
)


