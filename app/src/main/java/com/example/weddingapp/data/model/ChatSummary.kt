package com.example.weddingapp.data.model

data class ChatSummary(
    val chatId: String = "",
    val userId: String = "",
    val lastMessage: String = "",
    val lastTime: com.google.firebase.Timestamp? = null,
    val userName: String = "Khách hàng" // Có thể lấy thêm từ collection Users
)