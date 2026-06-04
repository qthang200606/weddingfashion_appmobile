package com.example.weddingapp.data.model

data class Appointment(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val phoneNumber: String = "",
    val date: String = "",        // Định dạng yyyy-MM-dd
    val timeSlot: String = "",    // Ví dụ: "09:00 - 10:00"
    val serviceType: String = "", // "Thử đồ" hoặc "Tư vấn"
    val status: String = "Chờ xác nhận", // Chờ xác nhận, Đã xác nhận, Đã xong, Đã hủy
    val timestamp: Long = System.currentTimeMillis()
)