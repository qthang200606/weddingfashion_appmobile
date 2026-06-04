package com.example.weddingapp.data.model



data class Review(
    val id: String = "",           // ID của document review
    val productId: String = "",    // Để lọc review theo sản phẩm
    val userId: String = "",       // ID người đánh giá
    val userName: String = "",     // Tên người đánh giá (Lấy từ profile user)
    val rating: Int = 5,           // Số sao (1-5)
    val comment: String = "",      // Nội dung đánh giá
    val timestamp: Long = 0L,      // Dùng Long để lưu System.currentTimeMillis() cho đơn giản
    val orderId: String = ""       // (Tùy chọn) Đánh giá này thuộc đơn hàng nào
)