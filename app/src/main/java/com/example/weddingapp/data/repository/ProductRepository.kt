package com.example.weddingapp.data.repository

import com.example.weddingapp.data.model.ProductItem
import com.example.weddingapp.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots // Bổ sung import này
import com.google.firebase.firestore.toObjects // Bổ sung import này
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map // Bổ sung import này

object ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    /**
     * Lấy danh sách sản phẩm, hỗ trợ Real-time
     */
    fun getProducts(categoryId: String? = null): Flow<List<ProductItem>> = callbackFlow {
        var query: Query = productsCollection.orderBy("createdAt", Query.Direction.DESCENDING)

        if (!categoryId.isNullOrBlank() && categoryId != "all" && categoryId != "Tất cả") {
            query = query.whereEqualTo("categoryId", categoryId)
        }

        val snapshotListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProductItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(items)
        }

        awaitClose { snapshotListener.remove() }
    }

    /**
     * Lấy chi tiết một sản phẩm theo ID (Real-time)
     */
    fun getProductById(productId: String): Flow<ProductItem?> = callbackFlow {
        val listener = productsCollection.document(productId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val item = snapshot?.toObject(ProductItem::class.java)?.copy(id = snapshot.id)
            trySend(item)
        }
        awaitClose { listener.remove() }
    }

    /**
     * THÊM MỚI SẢN PHẨM
     */
    fun addProduct(product: ProductItem, onComplete: (Boolean) -> Unit) {
        val productData = hashMapOf(
            "name" to product.name,
            "price" to product.price,
            "description" to product.description,
            "imageUrl" to product.imageUrl,
            "categoryId" to product.categoryId,
            "stock" to product.stock,
            "createdAt" to System.currentTimeMillis()
        )

        productsCollection.add(productData)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * CẬP NHẬT SẢN PHẨM
     */
    fun updateProduct(product: ProductItem, onComplete: (Boolean) -> Unit) {
        if (product.id.isEmpty()) return

        val updates = hashMapOf<String, Any>(
            "name" to product.name,
            "price" to product.price,
            "description" to product.description,
            "imageUrl" to product.imageUrl,
            "categoryId" to product.categoryId,
            "stock" to product.stock
        )

        productsCollection.document(product.id)
            .update(updates)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * CẬP NHẬT RIÊNG SỐ LƯỢNG TỒN KHO
     */
    fun updateStockOnly(productId: String, newStock: Int, onComplete: (Boolean) -> Unit = {}) {
        productsCollection.document(productId)
            .update("stock", newStock)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    /**
     * XÓA SẢN PHẨM
     */
    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        if (productId.isBlank()) return
        productsCollection.document(productId).delete()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * TÌM KIẾM SẢN PHẨM
     */
    fun searchProducts(query: String): Flow<List<ProductItem>> = callbackFlow {
        val subscription = productsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(ProductItem::class.java)?.copy(id = it.id)
                } ?: emptyList()

                val filtered = if (query.isEmpty()) emptyList()
                else products.filter { it.name.contains(query, ignoreCase = true) }

                trySend(filtered)
            }
        awaitClose { subscription.remove() }
    }

    fun updateStock(productId: String, newStock: Int) {
        productsCollection.document(productId)
            .update("stock", newStock)
    }

    /**
     * Lấy danh sách đánh giá của sản phẩm theo productId (Real-time)
     */
    fun getReviewsByProductId(productId: String): Flow<List<Review>> {
        return db.collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { query ->
                query.toObjects(Review::class.java)
            }
    }
}