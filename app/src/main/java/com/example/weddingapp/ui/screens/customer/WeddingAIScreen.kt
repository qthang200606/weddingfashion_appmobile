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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weddingapp.data.model.ChatMessage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.draw.clip
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingAIScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: WeddingAIViewModel = viewModel()
) {
    val messages = viewModel.messages
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.initWeddingChat(userId)
    }

    LaunchedEffect(messages.size, viewModel.isLoading) {
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
                actions = {
                    IconButton(onClick = { viewModel.initWeddingChat(userId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới hội thoại",
                            tint = Color(0xFF7A6010)
                        )
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

                if (viewModel.isLoading && messages.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(start = 8.dp, end = 60.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD4AF37)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Kim đang tra cứu dữ liệu tiệm cưới... ✍️",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

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
                        placeholder = { Text("Hỏi Kim về váy cưới, vest, dịch vụ của shop...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 3,
                        enabled = !viewModel.isLoading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (text.isNotBlank() && !viewModel.isLoading) {
                                viewModel.askAI(text)
                                text = ""
                            }
                        },
                        shape = RoundedCornerShape(50),
                        containerColor = if (viewModel.isLoading) Color.LightGray else Color(0xFFD4AF37),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(chat: ChatMessage) {
    val isModel = chat.role == "model"
    val regex = Regex("""!\[(.*?)]\((.*?)\)""")
    val matches = regex.findAll(chat.message).toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isModel) Alignment.Start else Alignment.End
    ) {
        Text(
            text = if (isModel) "Kim ✨" else "Bạn",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Surface(
            color = if (isModel) Color.White else Color(0xFFD4AF37),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Hiển thị text (loại bỏ markdown image)
                val textWithoutImages = chat.message.replace(regex, "")

                if (textWithoutImages.isNotBlank()) {
                    Text(
                        text = textWithoutImages,
                        color = if (isModel) Color.Black else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Hiển thị các product cards (nếu có ảnh)
                if (matches.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        matches.forEach { match ->
                            val alt = match.groupValues[1]
                            var url = match.groupValues[2]

                            // 🔑 FIX: Kiểm tra URL bị cắt ngắn
                            Log.d("ChatBubble", "Original URL: $url")

                            // Nếu URL không hợp lệ => tìm URL đầy đủ từ message
                            if (!url.startsWith("http") || url.length < 10) {
                                val fullMessage = chat.message
                                val fullUrlRegex = Regex("""https?://[^\s)]+""")
                                val fullMatch = fullUrlRegex.find(fullMessage)
                                if (fullMatch != null) {
                                    url = fullMatch.value
                                    Log.d("ChatBubble", "Recovered URL: $url")
                                }
                            }

                            // Product Card
                            if (url.isNotBlank() && url.startsWith("http")) {
                                ProductImageCard(
                                    productName = alt,
                                    imageUrl = url
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductImageCard(productName: String, imageUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ảnh sản phẩm
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 250.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color(0xFFD4AF37),
                            strokeWidth = 3.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚠️ Không tải được ảnh",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // Tên sản phẩm
            Text(
                text = productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                maxLines = 2
            )
        }
    }
}
