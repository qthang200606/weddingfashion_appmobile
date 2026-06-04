package com.example.weddingapp.ui.screens.customer

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weddingapp.data.model.ChatMessage
import com.example.weddingapp.ai.GroqApiService
import com.example.weddingapp.ai.GroqRequest
import com.example.weddingapp.ai.ChatEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.gson.GsonConverterFactory

// Chuyển sang AndroidViewModel để sử dụng SharedPreferences
class WeddingAIViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val sharedPreferences = context.getSharedPreferences("wedding_ai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val apiKey = ""

    private val apiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GroqApiService::class.java)

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val chatHistory = mutableListOf<ChatEntry>()

    var isLoading by mutableStateOf(false)
        private set

    init {
        // Khởi tạo System Instruction
        chatHistory.add(ChatEntry("system", """
            Bạn là Kim - Chuyên gia tư vấn đám cưới cao cấp. 
            NHIỆM VỤ:
            1. Ghi nhớ tên và năm sinh người dùng để xưng hô.
            2. TRÌNH BÀY: Dùng Bảng Markdown khi so sánh, gạch đầu dòng rõ ràng, in đậm (**chữ đậm**) ý quan trọng.
            3. PHONG CÁCH: Sang trọng, tận tâm.
        """.trimIndent()))

        // Tải dữ liệu cũ đã lưu
        loadMessages()
    }

    private fun saveMessages() {
        val json = gson.toJson(_messages.toList())
        sharedPreferences.edit().putString("saved_chat", json).apply()
    }

    private fun loadMessages() {
        val json = sharedPreferences.getString("saved_chat", null)
        if (json != null) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val savedList: List<ChatMessage> = gson.fromJson(json, type)
            _messages.addAll(savedList)

            // Cập nhật lại chatHistory cho AI từ dữ liệu đã lưu để AI không bị "mất trí nhớ"
            savedList.forEach {
                val role = if (it.role == "user") "user" else "assistant"
                chatHistory.add(ChatEntry(role, it.message))
            }
        } else {
            // Nếu chưa có dữ liệu lưu, hiển thị câu chào mặc định
            val welcomeMsg = ChatMessage("model", "Chào bạn, Kim rất vui được đồng hành cùng bạn! ✨\n\nĐể Kim dễ dàng tư vấn và xưng hô thân mật hơn, bạn có thể chia sẻ tên và năm sinh của mình được không ạ?")
            _messages.add(welcomeMsg)
            saveMessages()
        }
    }

    fun askAI(userPrompt: String) {
        if (userPrompt.isBlank() || isLoading) return

        val userMsg = ChatMessage("user", userPrompt)
        _messages.add(userMsg)
        chatHistory.add(ChatEntry("user", userPrompt))
        saveMessages() // Lưu ngay khi người dùng gửi

        isLoading = true

        viewModelScope.launch {
            try {
                val request = GroqRequest(
                    model = "llama-3.1-8b-instant",
                    messages = chatHistory,
                    temperature = 0.6
                )

                val response = apiService.getChatCompletion(
                    token = "Bearer ${apiKey.trim()}",
                    request = request
                )

                val aiText = response.choices.firstOrNull()?.message?.content
                if (aiText != null) {
                    val modelMsg = ChatMessage("model", aiText)
                    _messages.add(modelMsg)
                    chatHistory.add(ChatEntry("assistant", aiText))
                    saveMessages() // Lưu sau khi AI trả lời xong
                }
            } catch (e: Exception) {
                _messages.add(ChatMessage("model", "Lỗi kết nối: ${e.localizedMessage}"))
            } finally {
                isLoading = false
            }
        }
    }

    fun clearChat() {
        _messages.clear()
        chatHistory.removeAll { it.role != "system" }
        sharedPreferences.edit().remove("saved_chat").apply()
        _messages.add(ChatMessage("model", "Kim đã sẵn sàng tư vấn lại từ đầu đây!"))
    }
}