package com.example.weddingapp.data.model

data class ProductItem(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val categoryId: String = "",    // "Váy" hoặc "Vest"
    val subCategory: String = "",
    val stock: Int = 0,
    val description: String = ""
)