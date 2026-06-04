package com.example.weddingapp.ai



import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {
    /**
     * Gửi yêu cầu chat đến Groq API theo chuẩn OpenAI
     * @param token: Định dạng "Bearer <API_KEY>"
     * @param request: Đối tượng chứa model, messages và cấu hình AI
     */
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): GroqResponse
}