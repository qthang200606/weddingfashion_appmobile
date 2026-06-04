package com.example.weddingapp.data.repository

import com.example.weddingapp.data.model.PromoBanner
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object BannerRepository {
    private val db = FirebaseFirestore.getInstance()
    private val bannersCollection = db.collection("banners")

    fun getBanners(): Flow<List<PromoBanner>> = callbackFlow {
        val subscription = bannersCollection
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PromoBanner::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    fun addBanner(banner: PromoBanner, onComplete: (Boolean) -> Unit) {
        bannersCollection.add(banner).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteBanner(id: String, onComplete: (Boolean) -> Unit) {
        if (id.isEmpty()) return
        bannersCollection.document(id).delete().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // Trong BannerRepository.kt
    // Trong BannerRepository.kt
    fun updateBannerStatus(id: String, isActive: Boolean, onComplete: (Boolean) -> Unit) {
        db.collection("banners").document(id)
            .update("isActive", isActive)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}