package com.example.weddingapp.data.model

data class CartItem(
    val id: String = "",         // ID của document trong collection 'carts'
    val productId: String = "",  // ID của sản phẩm gốc
    val userId: String = "",     // ID của người mua
    val name: String = "",
    val price: String = "",      // Định dạng "12.500.000"
    val imageUrl: String = "",
    val quantity: Int = 1
)