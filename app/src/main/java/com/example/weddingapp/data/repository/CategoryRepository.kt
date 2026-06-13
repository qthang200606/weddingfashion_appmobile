package com.example.weddingapp.data.repository

import com.example.weddingapp.data.model.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object CategoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val categoriesCollection = db.collection("categories")

    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val subscription = categoriesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun addCategory(name: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        val category = Category(name = name, imageUrl = imageUrl)
        categoriesCollection.add(category).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateCategory(id: String, newName: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        categoriesCollection.document(id).update(
            "name", newName,
            "imageUrl", imageUrl
        ).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteCategory(id: String, onComplete: (Boolean) -> Unit) {
        if (id.isEmpty()) return
        categoriesCollection.document(id).delete().addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}
