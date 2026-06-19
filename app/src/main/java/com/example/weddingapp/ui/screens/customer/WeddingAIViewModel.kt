package com.example.weddingapp.ui.screens.customer

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weddingapp.data.model.ChatMessage
import com.example.weddingapp.ai.GroqApiService
import com.example.weddingapp.ai.GroqRequest
import com.example.weddingapp.ai.ChatEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.reflect.TypeToken

class WeddingAIViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val sharedPreferences = context.getSharedPreferences("wedding_ai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()

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

    // ==========================================
    // SYSTEM PROMPT
    // ==========================================
    private fun buildSystemPrompt(userName: String): String {
        return """
            Bạn là "Kim" – Trợ lý AI Planner thông minh & tận tâm của L'Amour Wedding Studio.
            Xưng hô lịch sự, ngọt ngào là "Bạn $userName". Hãy thân thiện, nhiệt tình, sáng tạo.
            
            ════════════════════════════════════════════════════════════
            🎀 CHUYÊN MÔN CỦA KIM - TOÀN DIỆN CƯỜ
            ════════════════════════════════════════════════════════════
            
            1️⃣ 👗 TƯ VẤN SẢN PHẨM & TRANG PHỤC CƯỚI
            2️⃣ 📏 TƯ VẤN SIZE & VÓC DÁNG
            3️⃣ ✨ TƯ VẤN PHONG CÁCH THỜI TRANG CƯỜ
            4️⃣ 📅 XÂY DỰNG PLAN CƯỜ - TOÀN DIỆN
            5️⃣ 📍 GỢI Ý ĐỊA ĐIỂM TỔ CHỨC & KHÔNG GIAN CƯỜ
            6️⃣ 💄 CHI TIẾT & TƯ VẤN CHUYÊN SÂU
            7️⃣ 💕 TƯ VẤN TÂM LÝ VÀ LỰA CHỌN
        """.trimIndent()
    }

    // ==========================================
    // DATABASE FETCH - LOAD TỪ FIREBASE
    // ==========================================
    private suspend fun getCompactProductsFromDB(): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WeddingAI", "🔄 Bắt đầu lấy dữ liệu từ Firebase...")

                val snapshot = firestore.collection("products").get().await()
                Log.d("WeddingAI", "✅ Firebase response: isEmpty = ${snapshot.isEmpty}, size = ${snapshot.documents.size}")

                if (snapshot.isEmpty) {
                    Log.w("WeddingAI", "⚠️ Collection 'products' không có dữ liệu")
                    return@withContext "Không có sản phẩm nào."
                }

                val stringBuilder = StringBuilder()

                snapshot.documents.forEachIndexed { index, doc ->
                    val name = doc.getString("name") ?: "Mẫu cưới"
                    val category = doc.getString("category") ?: "Trang phục"
                    val priceRaw = doc.get("price")?.toString() ?: "0"
                    val priceLong = priceRaw.toLongOrNull() ?: 0L
                    val imageUrl = doc.getString("imageUrl") ?: ""

                    Log.d("WeddingAI", "📦 Product $index: $name, URL: $imageUrl")

                    stringBuilder.append("**🎀 ${index + 1}. $name**\n")
                    stringBuilder.append("Phân loại: $category | Giá: ${priceLong}đ\n")

                    if (imageUrl.isNotBlank()) {
                        stringBuilder.append("![$name]($imageUrl)\n")
                    }
                    stringBuilder.append("\n")
                }

                val result = stringBuilder.toString().trim()
                Log.d("WeddingAI", "✅ Dữ liệu được tạo thành công: ${result.length} ký tự")
                result

            } catch (e: Exception) {
                Log.e("WeddingAI", "❌ Lỗi lấy dữ liệu Firestore: ${e.message}", e)
                e.printStackTrace()
                "Không thể truy cập kho hàng."
            }
        }
    }

    // ==========================================
    // API CALL VỚI RETRY LOGIC
    // ==========================================
    private suspend fun callApiWithRetry(
        request: GroqRequest,
        maxRetries: Int = 3
    ): String? {
        repeat(maxRetries) { attempt ->
            try {
                Log.d("WeddingAI", "📞 API Call - Lần thứ ${attempt + 1}...")
                val response = apiService.getChatCompletion(
                    token = "Bearer $apiKey",
                    request = request
                )
                Log.d("WeddingAI", "✅ API Success")
                return response.choices.firstOrNull()?.message?.content
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                Log.e("WeddingAI", "❌ API Error (Attempt ${attempt + 1}): $errorMsg", e)

                // Nếu là HTTP 429 (Rate Limit), đợi rồi retry
                if (errorMsg.contains("429") && attempt < maxRetries - 1) {
                    val waitTime = (2.0.pow(attempt.toDouble()) * 1000).toLong() // Exponential backoff
                    Log.w("WeddingAI", "⏳ Rate limit hit, đợi ${waitTime}ms rồi retry...")
                    delay(waitTime)
                } else if (attempt == maxRetries - 1) {
                    // Lần cuối cùng, trả về null
                    return null
                } else if (!errorMsg.contains("429")) {
                    // Lỗi khác, đừng retry
                    return null
                }
            }
        }
        return null
    }

    // ==========================================
    // XỬ LÝ HỘI THOẠI
    // ==========================================
    fun askAI(userPrompt: String) {
        if (userPrompt.isBlank() || isLoading) return

        _messages.add(ChatMessage("user", userPrompt))
        chatHistory.add(ChatEntry("user", userPrompt))

        if (chatHistory.size > 10) {
            val system = chatHistory.first()
            val recent = chatHistory.takeLast(8)
            chatHistory.clear()
            chatHistory.add(system)
            chatHistory.addAll(recent)
        }
        saveChat()

        isLoading = true

        viewModelScope.launch {
            try {
                val needsProducts = userPrompt.contains(Regex(
                    "váy|vest|áo dài|phụ kiện|giá|thuê|mẫu|sản phẩm|hình ảnh|hình",
                    RegexOption.IGNORE_CASE
                ))

                Log.d("WeddingAI", "🔍 Câu hỏi: '$userPrompt', needsProducts: $needsProducts")

                // Gọi API lần 1 với retry
                val aiText = withContext(Dispatchers.IO) {
                    callApiWithRetry(
                        GroqRequest(
                            model = "llama-3.1-8b-instant",
                            messages = chatHistory,
                            temperature = if (needsProducts) 0.0 else 0.8
                        )
                    )
                }

                if (aiText == null) {
                    Log.e("WeddingAI", "❌ API thất bại sau tất cả retry")
                    _messages.add(ChatMessage("model", "⚠️ Hệ thống quá tải. Vui lòng chờ vài giây và thử lại nhé! 💛"))
                    isLoading = false
                    saveChat()
                    return@launch
                }

                Log.d("WeddingAI", "✅ AI Response: ${aiText.take(100)}...")

                // Kiểm tra nếu cần fetch sản phẩm
                if (aiText.contains("TOOL: fetch_all_products") || needsProducts) {
                    Log.d("WeddingAI", "🛠️ Phát hiện cần TOOL: fetch_all_products")
                    val compactProducts = getCompactProductsFromDB()
                    Log.d("WeddingAI", "📊 Dữ liệu sản phẩm: ${compactProducts.take(100)}...")

                    if (chatHistory.isNotEmpty()) chatHistory.removeAt(chatHistory.size - 1)

                    chatHistory.add(
                        ChatEntry("user", """
                        DANH SÁCH SẢN PHẨM THỰC TẾ TẠI L'AMOUR WEDDING STUDIO:
                        $compactProducts

                        ⚠️ HÃY TƯ VẤN CHI TIẾT:
                        1. Gợi ý sản phẩm phù hợp với yêu cầu: "$userPrompt"
                        2. Dùng Markdown: ![Tên](URL) cho ảnh
                        3. Trình bày dễ hiểu, thân thiện
                    """.trimIndent())
                    )

                    val finalText = withContext(Dispatchers.IO) {
                        callApiWithRetry(
                            GroqRequest(
                                model = "llama-3.1-8b-instant",
                                messages = chatHistory,
                                temperature = 0.7
                            )
                        )
                    }

                    if (finalText != null) {
                        Log.d("WeddingAI", "✅ Final response nhận được")
                        _messages.add(ChatMessage("model", finalText))
                        chatHistory.add(ChatEntry("assistant", finalText))
                    } else {
                        Log.e("WeddingAI", "❌ Final response null")
                        _messages.add(ChatMessage("model", "⚠️ Không thể lấy dữ liệu. Vui lòng thử lại."))
                    }
                } else {
                    Log.d("WeddingAI", "💬 Trả lời câu hỏi tư vấn bình thường")
                    _messages.add(ChatMessage("model", aiText))
                    chatHistory.add(ChatEntry("assistant", aiText))
                }
                saveChat()

            } catch (e: Exception) {
                Log.e("WeddingAI", "❌ Lỗi: ${e.message}", e)
                e.printStackTrace()
                _messages.add(ChatMessage("model", "❌ Có lỗi xảy ra, vui lòng thử lại"))
            } finally {
                isLoading = false
            }
        }
    }

    fun initWeddingChat(userId: String, userName: String = "khách") {
        if (_messages.isNotEmpty()) return
        isLoading = true
        viewModelScope.launch {
            val system = buildSystemPrompt(userName)
            chatHistory.clear()
            chatHistory.add(ChatEntry("system", system))
            _messages.clear()

            val saved = sharedPreferences.getString("chat", null)
            if (saved != null) {
                try {
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    val list: List<ChatMessage> = gson.fromJson(saved, type)
                    _messages.addAll(list)
                    list.forEach {
                        val role = if (it.role == "model") "assistant" else "user"
                        chatHistory.add(ChatEntry(role, it.message))
                    }
                } catch (e: Exception) {
                    sharedPreferences.edit().remove("chat").apply()
                }
            }

            if (_messages.isEmpty()) {
                val welcome = ChatMessage(
                    role = "model",
                    message = "Chào bạn **$userName** 💐! Kim sẵn sàng giúp bạn! ✨"
                )
                _messages.add(welcome)
                chatHistory.add(ChatEntry("assistant", welcome.message))
                saveChat()
            }
            isLoading = false
        }
    }

    fun clearChat(userId: String, userName: String = "khách") {
        _messages.clear()
        chatHistory.clear()
        sharedPreferences.edit().remove("chat").apply()
        initWeddingChat(userId, userName)
    }

    private fun saveChat() {
        val json = gson.toJson(_messages.toList())
        sharedPreferences.edit().putString("chat", json).apply()
    }
}

// Extension function cho pow
private fun Double.pow(exponent: Double): Double {
    return Math.pow(this, exponent)
}
