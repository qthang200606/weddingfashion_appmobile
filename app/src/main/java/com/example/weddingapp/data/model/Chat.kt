package com.example.weddingapp.data.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val isBot: Boolean = false,
    val type: String = "TEXT"
)
