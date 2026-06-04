package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.weddingapp.data.model.Chat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBoxScreen(
    chatId: String,
    currentUserId: String,
    productId: String? = null,
    onBack: () -> Unit,
    onNavigateToBooking: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val messages = remember { mutableStateListOf<Chat>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Danh sách gợi ý tin nhắn (Chỉ hiện cho Khách hàng)
    val quickReplies = listOf(
        "📅 Đặt lịch thử váy",
        "💰 Bảng giá trọn gói",
        "📍 Shop ở đâu ạ?",
        "Tư vấn cho mình nhé"
    )

    // Hàm gửi tin nhắn dùng chung (Xử lý cả Text và Booking Card)
    fun sendMessage(text: String, type: String = "TEXT") {
        if (text.isBlank()) return
        val msgData = hashMapOf(
            "text" to text,
            "senderId" to currentUserId,
            "timestamp" to FieldValue.serverTimestamp(),
            "type" to type
        )

        db.collection("chats").document(chatId).collection("messages").add(msgData)

        // Cập nhật trạng thái phòng chat ở bên ngoài danh sách
        db.collection("chats").document(chatId).set(
            hashMapOf(
                "lastMessage" to if (type == "BOOKING") "📅 Yêu cầu đặt lịch hẹn" else text,
                "lastTime" to FieldValue.serverTimestamp(),
                "userId" to chatId
            ), com.google.firebase.firestore.SetOptions.merge()
        )
    }

    // Lắng nghe tin nhắn Real-time
    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val newMessages = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                    messages.clear()
                    messages.addAll(newMessages)
                    if (messages.isNotEmpty()) {
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (currentUserId == "admin") "HỖ TRỢ KHÁCH HÀNG" else "TƯ VẤN CƯỚI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("Trực tuyến", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF0F2F5))) {

            // 1. DANH SÁCH TIN NHẮN
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == currentUserId
                    if (message.type == "BOOKING") {
                        BookingCard(isMe = isMe, onClick = onNavigateToBooking)
                    } else {
                        ChatBubble(message, isMe = isMe)
                    }
                }
            }

            // 2. GỢI Ý TIN NHẮN (Chỉ hiện cho khách hàng)
            if (currentUserId != "admin") {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickReplies) { reply ->
                        SuggestionChip(
                            onClick = {
                                if (reply.contains("Đặt lịch")) {
                                    sendMessage("Tôi muốn đặt lịch hẹn thử váy", "BOOKING")
                                } else {
                                    sendMessage(reply)
                                }
                            },
                            label = { Text(reply, fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
                        )
                    }
                }
            }

            // 3. Ô NHẬP TIN NHẮN
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nhập tin nhắn...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        containerColor = Color(0xFFE91E63),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// --- THÀNH PHẦN BONG BÓNG CHAT VĂN BẢN ---
@Composable
fun ChatBubble(message: Chat, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) Color(0xFFE91E63) else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isMe) Color.White else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

// --- THÀNH PHẦN THẺ ĐẶT LỊCH HẸN (BOOKING CARD) ---
@Composable
fun BookingCard(isMe: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.width(260.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📅 ĐẶT LỊCH HẸN", fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Shop sẽ hỗ trợ bạn thử váy và tư vấn trực tiếp tại cửa hàng.",
                    fontSize = 13.sp, color = Color.DarkGray
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("NHẤN ĐỂ ĐẶT LỊCH", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun AdminConfirmCard(message: Chat, isAdmin: Boolean, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE91E63))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(message.text, fontSize = 14.sp)

            if (isAdmin) { // CHỈ HIỆN NÚT VỚI ADMIN
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("XÁC NHẬN LỊCH NÀY")
                }
            }
        }
    }
}
