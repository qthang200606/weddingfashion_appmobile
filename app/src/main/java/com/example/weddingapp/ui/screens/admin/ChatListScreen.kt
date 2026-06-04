package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.ChatSummary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var chatList by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Lắng nghe danh sách phòng chat thời gian thực
    DisposableEffect(Unit) {
        val registration = db.collection("chats")
            .orderBy("lastTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    chatList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatSummary::class.java)?.copy(chatId = doc.id)
                    }
                }
                isLoading = false
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HỘP THƯ HỖ TRỢ", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFE91E63))
            } else if (chatList.isEmpty()) {
                EmptyInboxView()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "Tin nhắn gần đây",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                    items(chatList) { chat ->
                        ChatListItem(chat, onClick = { onNavigateToDetail(chat.chatId) })
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: ChatSummary, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    val timeDisplay = chat.lastTime?.toDate()?.let {
        val now = Calendar.getInstance()
        val chatTime = Calendar.getInstance().apply { time = it }
        if (now.get(Calendar.DATE) == chatTime.get(Calendar.DATE)) timeFormat.format(it)
        else dateFormat.format(it)
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar với viền trạng thái
            Box {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = Color(0xFFE91E63).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chat.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                        )
                    }
                }
                // Chấm xanh online giả lập
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF4CAF50)))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = timeDisplay, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = if (chat.lastMessage.contains("?")) Color.Black else Color.Gray,
                    fontWeight = if (chat.lastMessage.contains("?")) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Icon chỉ báo mũi tên vào chat
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.LightGray
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp), // Thụt đầu dòng để thẳng với Text
            thickness = 0.5.dp,
            color = Color(0xFFEEEEEE)
        )
    }
}

@Composable
fun EmptyInboxView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(Modifier.height(16.dp))
        Text("Chưa có tin nhắn nào từ khách", color = Color.Gray, fontSize = 16.sp)
    }
}