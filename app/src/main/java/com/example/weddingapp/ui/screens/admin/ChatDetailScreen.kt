package com.example.weddingapp.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 🌟 LẮNG NGHE TIN NHẮN THỜI GIAN THỰC TỪ FIRESTORE
    DisposableEffect(chatId) {
        val registration = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Tin cũ trên, tin mới dưới
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.getString("message") ?: ""
                        val role = doc.getString("role") ?: "user"
                        ChatMessage(role = role, message = msg)
                    }
                    // Tự động cuộn xuống tin nhắn mới nhất
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                } else {
                    Log.e("ChatDetail", "Lỗi tải tin nhắn: ${error?.message}")
                }
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết hỗ trợ 💬", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            // 1. DANH SÁCH TIN NHẮN
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    AdminChatBubbleItem(message = message)
                }
            }

            // 2. Ô NHẬP TIN NHẮN CỦA ADMIN
            Surface(
                tonalElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Nhập phản hồi của chủ shop...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val currentText = textInput.trim()
                                textInput = ""

                                // 🌟 ĐẨY DATA LÊN FIRESTORE VỚI ROLE LÀ "admin" KHÔNG LỆCH ĐI ĐÂU ĐƯỢC
                                val newMessage = hashMapOf(
                                    "message" to currentText,
                                    "role" to "admin",
                                    "timestamp" to com.google.firebase.Timestamp.now()
                                )

                                db.collection("chats").document(chatId)
                                    .collection("messages").add(newMessage)
                                    .addOnSuccessListener {
                                        // Cập nhật tin nhắn hiển thị nhanh ngoài màn hình danh sách
                                        db.collection("chats").document(chatId).update(
                                            mapOf(
                                                "lastMessage" to currentText,
                                                "lastTime" to com.google.firebase.Timestamp.now()
                                            )
                                        )
                                    }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        containerColor = Color(0xFFE91E63),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// LOGIC PHÂN CHIA TRÁI / PHẢI TUYỆT ĐỐI CHUẨN ĐẠT
// ==========================================
@Composable
fun AdminChatBubbleItem(message: ChatMessage) {
    // Nếu role là "admin" -> Chủ shop gửi (Bên Phải). Ngược lại là khách hoặc AI (Bên Trái).
    val isFromAdmin = message.role == "admin"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromAdmin) Alignment.End else Alignment.Start
    ) {
        // Tên định danh nhỏ trên đầu bong bóng
        Text(
            text = when (message.role) {
                "admin" -> "Bạn (Chủ shop)"
                "model" -> "Kim AI ✨"
                else -> "Khách hàng"
            },
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        // Khung nền Bong bóng Chat
        Surface(
            color = if (isFromAdmin) Color(0xFFE91E63) else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromAdmin) 16.dp else 4.dp, // Khách nhọn góc trái dưới
                bottomEnd = if (isFromAdmin) 4.dp else 16.dp   // Admin nhọn góc phải dưới
            ),
            shadowElevation = 1.dp
        ) {
            Box(modifier = Modifier.padding(12.dp).widthIn(max = 270.dp)) {
                Text(
                    text = message.message,
                    fontSize = 15.sp,
                    color = if (isFromAdmin) Color.White else Color.Black
                )
            }
        }
    }
}