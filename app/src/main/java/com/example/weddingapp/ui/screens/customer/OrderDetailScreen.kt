package com.example.weddingapp.ui.screens.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.CartItem
import com.example.weddingapp.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(orderId) {
        val registration = db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                snapshot?.let {
                    try {
                        // Sửa lỗi mapping Firestore
                        order = it.toObject(Order::class.java)?.copy(id = it.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                isLoading = false
            }
        onDispose { registration.remove() }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Xác nhận hủy đơn", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn hủy đơn hàng này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("orders").document(orderId).update("status", "Đã hủy")
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hủy đơn", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Quay lại") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CHI TIẾT ĐƠN HÀNG", style = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, null, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                }
            }
            order == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy thông tin đơn hàng.", color = Color.Gray)
                }
            }
            else -> {
                val data = order!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        OrderDetailStepper(data.status)
                        Spacer(Modifier.height(32.dp))
                    }

                    if (data.status == "Đang giao" && !data.trackingNumber.isNullOrBlank()) {
                        item {
                            OrderDetailSectionTitle("Theo dõi đơn hàng")
                            OrderDetailInfoCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalShipping, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Đơn vị: ${data.shippingProvider ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Mã vận đơn: ${data.trackingNumber}", color = Color.Gray, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val url = when (data.shippingProvider) {
                                                "GHTK" -> "https://web.giaohangtietkiem.vn/vanchuyen/${data.trackingNumber}"
                                                "GHN" -> "https://ghn.vn/blogs/trang-thai-don-hang?v=${data.trackingNumber}"
                                                else -> "https://www.google.com/search?q=${data.shippingProvider}+${data.trackingNumber}"
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                                    ) {
                                        Text("Tra cứu", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    item {
                        OrderDetailSectionTitle("Thông tin nhận hàng")
                        OrderDetailInfoCard {
                            Text(data.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(data.phoneNumber, color = Color.Gray, fontSize = 14.sp)
                            Text(data.address, color = Color.Gray, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    item { OrderDetailSectionTitle("Sản phẩm đã đặt") }
                    items(data.items ?: emptyList()) { item ->
                        OrderDetailItemRow(item)
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tổng thanh toán", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                // Sửa lỗi Argument type mismatch bằng cách gọi .toString() nếuTotalPrice là Int
                                text = formatPrice(data.totalPrice.toString()),
                                style = TextStyle(color = Color(0xFFD4AF37), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            )
                        }
                    }

                    if (data.status == "Chờ xử lý") {
                        item {
                            Spacer(Modifier.height(32.dp))
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                            ) {
                                Text("HỦY ĐƠN HÀNG", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    } else {
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailStepper(currentStatus: String) {
    val steps = listOf("Chờ xử lý", "Đã xác nhận", "Đang giao", "Hoàn thành")

    if (currentStatus == "Đã hủy") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Red.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "ĐƠN HÀNG ĐÃ BỊ HỦY",
                modifier = Modifier.padding(16.dp),
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
        return
    }

    val currentIndex = steps.indexOf(currentStatus).coerceAtLeast(0)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, status ->
            val isActive = index <= currentIndex
            val color = if (isActive) Color(0xFFD4AF37) else Color.LightGray

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(24.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                    if (index < currentIndex) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    else Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(status, fontSize = 9.sp, color = if (isActive) Color.Black else Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }

            if (index < steps.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.4f).padding(bottom = 12.dp),
                    thickness = 2.dp,
                    color = if (index < currentIndex) Color(0xFFD4AF37) else Color.LightGray
                )
            }
        }
    }
}

@Composable
fun OrderDetailItemRow(item: CartItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(64.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            // Đã sửa để hiển thị ảnh thật từ URL
            AsyncImage(
                model = item.imageUrl.ifEmpty { null },
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Số lượng: x${item.quantity}", color = Color.Gray, fontSize = 12.sp)
        }
        // Sửa lỗi ép kiểu tương tự nhưTotalPrice
        Text(formatPrice(item.price.toString()), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun OrderDetailSectionTitle(title: String) {
    Text(title.uppercase(), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.Gray), modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun OrderDetailInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

fun formatPrice(price: String): String {
    return try {
        val clean = price.replace(Regex("[^\\d]"), "")
        val number = clean.toLong()
        String.format(Locale.getDefault(), "%,dđ", number)
    } catch (e: Exception) {
        "0đ"
    }
}