package com.example.weddingapp.data.model

data class PromoBanner(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val discountTag: String = "", // Ví dụ: "Giảm 30%", "Đồng giá 999k"
    val isActive: Boolean = true
)