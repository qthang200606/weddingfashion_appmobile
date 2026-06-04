package com.example.weddingapp.ui.screens.customer

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weddingapp.data.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingAIScreen(
    onBack: () -> Unit,
    viewModel: WeddingAIViewModel = viewModel()
) {
    val messages = viewModel.messages
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Tự động cuộn xuống khi có tin nhắn mới
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trợ lý ảo Kim 💐", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            // Danh sách tin nhắn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }

                if (viewModel.isLoading) {
                    item {
                        Text(
                            "Kim đang soạn câu trả lời... ✍️",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Thanh nhập tin nhắn
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
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Hỏi Kim về ngày cưới...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.askAI(text)
                                text = ""
                            }
                        },
                        shape = RoundedCornerShape(50),
                        containerColor = Color(0xFFD4AF37),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(chat: ChatMessage) {
    val isModel = chat.role == "model"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isModel) Alignment.Start else Alignment.End
    ) {
        // Tên người gửi
        Text(
            text = if (isModel) "Kim ✨" else "Bạn",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Surface(
            color = if (isModel) Color.White else Color(0xFFD4AF37),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isModel) 4.dp else 16.dp,
                bottomEnd = if (isModel) 16.dp else 4.dp
            ),
            shadowElevation = 1.dp
        ) {
            Box(modifier = Modifier.padding(12.dp).widthIn(max = 280.dp)) {
                // Sử dụng Text thông thường để đảm bảo không lỗi build do thư viện Markdown
                Text(
                    text = chat.message,
                    color = if (isModel) Color.Black else Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}
