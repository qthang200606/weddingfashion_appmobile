package com.example.weddingapp.ai



data class GroqRequest(
    val model: String = "llama-3.1-8b-instant", // Tên model phải chính xác
    val messages: List<ChatEntry>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048
)

data class ChatEntry(
    val role: String, // "system", "user", hoặc "assistant"
    val content: String
)

// Đảm bảo Response cũng chuẩn để không bị lỗi Gson
data class GroqResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: ChatEntry
)