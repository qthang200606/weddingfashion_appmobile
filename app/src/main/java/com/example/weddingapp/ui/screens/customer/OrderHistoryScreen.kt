package com.example.weddingapp.ui.screens.customer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.map
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onOrderClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Trạng thái cho Dialog Đánh giá
    var selectedOrderForReview by remember { mutableStateOf<Order?>(null) }

    // 1. Lấy danh sách đơn hàng Real-time
    val orders by firestore.collection("orders")
        .whereEqualTo("userId", userId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .snapshots()
        .map { query ->
            query.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
        }
        .collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đang xử lý", "Lịch sử")

    // 2. Lọc đơn hàng theo Tab
    val filteredOrders = remember(selectedTab, orders) {
        if (selectedTab == 0) {
            orders.filter { it.status in listOf("Chờ xử lý", "Đã xác nhận", "Đang giao") }
        } else {
            orders.filter { it.status in listOf("Hoàn thành", "Đã hủy") }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ĐƠN HÀNG CỦA BẠN",
                        style = TextStyle(letterSpacing = 2.sp, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, null, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFFAFAFA))) {
            Column {
                // Tab điều hướng
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFFD4AF37),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFFD4AF37)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                if (filteredOrders.isEmpty()) {
                    EmptyOrderState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderHistoryCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                onReviewClick = { selectedOrderForReview = order }
                            )
                        }
                    }
                }
            }

            // --- Dialog Đánh giá sản phẩm ---
            // ... các phần khác giữ nguyên ...

// --- Dialog Đánh giá sản phẩm ---
            if (selectedOrderForReview != null) {
                ReviewDialog(
                    onDismiss = { selectedOrderForReview = null },
                    onSubmit = { rating, comment ->
                        // Bước 1: Lấy thông tin User từ Firestore trước khi lưu Review
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                // Lấy tên từ document users, nếu không có thì lấy email, cuối cùng mới dùng "Khách hàng"
                                val realUserName = document.getString("fullName")
                                    ?: document.getString("name")
                                    ?: FirebaseAuth.getInstance().currentUser?.displayName
                                    ?: "Khách hàng"

                                // Bước 2: Tạo dữ liệu review với tên thật
                                val reviewData = hashMapOf(
                                    "userId" to userId,
                                    "userName" to realUserName, // Đã lấy tên thật ở đây
                                    "rating" to rating,
                                    "comment" to comment,
                                    "timestamp" to System.currentTimeMillis(),
                                    "orderId" to selectedOrderForReview!!.id,
                                    // Thêm productId để trang chi tiết sản phẩm có thể lọc được
                                    "productId" to (selectedOrderForReview!!.items.firstOrNull()?.productId ?: "")
                                )

                                // Bước 3: Lưu vào collection reviews
                                firestore.collection("reviews").add(reviewData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Cảm ơn $realUserName đã đánh giá!", Toast.LENGTH_SHORT).show()
                                        selectedOrderForReview = null
                                    }
                            }
                            .addOnFailureListener {
                                // Nếu lỗi không lấy được user profile, vẫn cho gửi với tên mặc định
                                Toast.makeText(context, "Lỗi kết nối, đang gửi với tên mặc định", Toast.LENGTH_SHORT).show()
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: Order, onClick: () -> Unit, onReviewClick: () -> Unit) {
    val statusColor = when (order.status) {
        "Chờ xử lý" -> Color(0xFFE91E63)
        "Đã xác nhận" -> Color(0xFF2196F3)
        "Đang giao" -> Color(0xFFD4AF37)
        "Hoàn thành" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Phần Header Mã đơn & Trạng thái (Giữ nguyên)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã: ${order.id.takeLast(8).uppercase()}", fontWeight = FontWeight.Bold)
                Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = order.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = TextStyle(color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // --- THAY ĐỔI Ở ĐÂY: Hiển thị ảnh thật từ Firebase ---
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(60.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    val firstItemImageUrl = order.items.firstOrNull()?.imageUrl ?: ""
                    if (firstItemImageUrl.isNotEmpty()) {
                        androidx.compose.ui.layout.ContentScale
                        coil.compose.AsyncImage(
                            model = firstItemImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        // Ảnh mặc định nếu URL trống
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, null, tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Hiển thị tên sản phẩm đầu tiên kèm số lượng
                    val firstItemName = order.items.firstOrNull()?.name ?: "Sản phẩm"
                    Text(
                        text = if (order.items.size > 1) "$firstItemName (+${order.items.size - 1})" else firstItemName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,dđ", order.totalPrice.toLong()),
                        style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFD4AF37))
                    )
                }

                if (order.status == "Hoàn thành") {
                    Button(
                        onClick = onReviewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Đánh giá", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đánh giá của bạn", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                Icons.Default.Star, null,
                                tint = if (index < rating) Color(0xFFD4AF37) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Bạn thấy sản phẩm như thế nào?") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }, colors = ButtonDefaults.buttonColors(Color(0xFFD4AF37))) {
                Text("Gửi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        }
    )
}

@Composable
fun EmptyOrderState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chưa có đơn hàng nào", color = Color.Gray)
    }
}
