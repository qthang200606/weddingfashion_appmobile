package com.example.weddingapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    fun sendMessage(chatId: String, text: String, senderId: String, isBot: Boolean = false) {
        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "isBot" to isBot,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                // Cập nhật tin nhắn cuối cùng ở document cha để Admin dễ theo dõi
                db.collection("chats").document(chatId).update(
                    "lastMessage", text,
                    "lastTimestamp", FieldValue.serverTimestamp()
                )
            }
    }
}
