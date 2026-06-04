package com.example.weddingapp.data.repository


import com.example.weddingapp.data.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Tên collection trong Firestore
    private const val CART_COLLECTION = "carts"

    /**
     * Lấy danh sách sản phẩm trong giỏ hàng của người dùng hiện tại (Real-time)
     */
    fun getCartItems(): Flow<List<CartItem>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }

        // Lọc sản phẩm theo userId của người đang đăng nhập
        val query = db.collection(CART_COLLECTION)
            .whereEqualTo("userId", userId)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(CartItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(items)
        }

        // Đóng listener khi không còn sử dụng để tiết kiệm tài nguyên
        awaitClose { subscription.remove() }
    }

    /**
     * Cập nhật số lượng sản phẩm
     */
    fun updateQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }

        db.collection(CART_COLLECTION).document(cartItemId)
            .update("quantity", newQuantity)
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    fun removeFromCart(cartItemId: String) {
        db.collection("carts").document(cartItemId)
            .delete()
            .addOnSuccessListener { /* Xử lý thành công nếu cần */ }
    }

    /**
     * Thêm sản phẩm mới vào giỏ hàng (Dùng cho nút "Thêm vào giỏ" ở trang Chi tiết)
     */
    suspend fun addToCart(product: CartItem) {
        val userId = auth.currentUser?.uid ?: return

        // Kiểm tra xem sản phẩm này đã có trong giỏ hàng chưa
        val existingItems = db.collection(CART_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", product.productId)
            .get()
            .await()

        if (existingItems.isEmpty) {
            // Nếu chưa có, thêm mới
            db.collection(CART_COLLECTION).add(product.copy(userId = userId))
        } else {
            // Nếu đã có, tăng số lượng lên 1
            val docId = existingItems.documents.first().id
            val currentQty = existingItems.documents.first().getLong("quantity") ?: 1
            db.collection(CART_COLLECTION).document(docId).update("quantity", currentQty + 1)
        }
    }
    fun clearCart() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("carts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    document.reference.delete()
                }
            }
    }
}