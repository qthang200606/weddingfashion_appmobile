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

    // Bộ nhớ đệm (Cache map) dùng để lưu: Key (userId) -> Value (Tên thật đăng ký từ bảng users)
    // Giúp app chỉ đọc DB đúng 1 lần cho mỗi khách hàng, tiết kiệm tối đa dung lượng mạng
    var userNamesCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Lắng nghe danh sách phòng chat thời gian thực từ Firestore
    DisposableEffect(Unit) {
        val registration = db.collection("chats")
            .orderBy("lastTime", Query.Direction.DESCENDING) // Sắp xếp theo tin nhắn mới nhất
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val incomingChats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatSummary::class.java)?.copy(chatId = doc.id)
                    }
                    chatList = incomingChats

                    // ------------------------------------------------------------------------
                    // LOGIC TỰ ĐỘNG TRA CỨU TÊN THẬT TỪ COLLECTION "users"
                    // ------------------------------------------------------------------------
                    incomingChats.forEach { chat ->
                        val targetUserId = chat.userId.ifEmpty { chat.chatId }

                        // Nếu chưa có tên trong bộ nhớ đệm thì tiến hành gọi lên Firebase để tìm
                        if (targetUserId.isNotEmpty() && !userNamesCache.containsKey(targetUserId)) {
                            // Truy vấn chính xác vào collection "users" dựa trên ID người dùng
                            db.collection("users").document(targetUserId).get()
                                .addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        // KHỚP CHÍNH XÁC: Lấy giá trị từ trường "name" trong tài liệu thực tế của bạn
                                        val realName = userDoc.getString("name")?.trim()

                                        if (!realName.isNullOrEmpty()) {
                                            // Lưu tên thật tìm được vào cache, lập tức giao diện sẽ tự cập nhật đổi tên
                                            userNamesCache = userNamesCache + (targetUserId to realName)
                                        }
                                    }
                                }
                        }
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
                        val targetUserId = chat.userId.ifEmpty { chat.chatId }

                        // Kiểm tra chéo: Có tên thật trong cache thì gán vào, chưa có hoặc không tìm thấy thì dùng "Khách hàng" mặc định
                        val displayName = userNamesCache[targetUserId] ?: chat.userName

                        ChatListItem(
                            chat = chat,
                            displayTitle = displayName, // Đẩy tên thật (ví dụ: Trần Thị Diệu Thiện) ra giao diện hiển thị
                            onClick = { onNavigateToDetail(chat.chatId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatSummary,
    displayTitle: String, // Nhận tên tài khoản thực tế để render UI
    onClick: () -> Unit
) {
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
            // Avatar tự động lấy chữ cái đầu của TÊN THẬT (Ví dụ: Trần Thị Diệu Thiện -> Hiện chữ T)
            Box {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = Color(0xFFE91E63).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayTitle.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                        )
                    }
                }
                // Chấm xanh trạng thái hoạt động online giả lập
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
                        text = displayTitle, // Hiển thị chuẩn tên đăng ký thật từ Firestore
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

            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.LightGray
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp),
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