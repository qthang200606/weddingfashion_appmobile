package com.example.weddingapp.data.repository

import com.example.weddingapp.data.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Lấy danh sách đơn hàng của User hiện tại theo thời gian thực
     */
    fun getOrders(): Flow<List<Order>> = callbackFlow {
        val userId = auth.currentUser?.uid

        // Nếu chưa đăng nhập, trả về list rỗng
        if (userId == null) {
            trySend(emptyList())
            return@callbackFlow
        }

        // Truy vấn: Lọc theo userId và sắp xếp thời gian mới nhất lên đầu
        val query = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Bạn có thể log lỗi ở đây
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.toObjects(Order::class.java)
                trySend(orders)
            }
        }

        // Quan trọng: Đóng listener khi Flow không còn được thu thập (collect)
        awaitClose { subscription.remove() }
    }

    /**
     * Lấy chi tiết một đơn hàng cụ thể
     */
    fun getOrderDetail(orderId: String): Flow<Order?> = callbackFlow {
        val docRef = firestore.collection("orders").document(orderId)

        val subscription = docRef.addSnapshotListener { snapshot, _ ->
            val order = snapshot?.toObject(Order::class.java)
            trySend(order)
        }

        awaitClose { subscription.remove() }
    }

    // Xác định thứ tự các bước để vẽ Stepper
    fun getStatusStep(status: String): Int {
        return when (status) {
            "Chờ xử lý" -> 0
            "Đã xác nhận" -> 1
            "Đang giao" -> 2
            "Hoàn thành" -> 3
            else -> 0
        }
    }
}