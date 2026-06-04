package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.map
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderManagementScreen(onBack: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    // 1. Lấy danh sách đơn hàng real-time kèm ID thực tế
    val orders by remember {
        firestore.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { query ->
                query.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
            }
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QUẢN LÝ ĐƠN HÀNG", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có đơn hàng nào", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    AdminOrderCard(order)
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(order: Order) {
    val firestore = FirebaseFirestore.getInstance()
    var showShippingDialog by remember { mutableStateOf(false) }

    if (showShippingDialog) {
        ShippingInfoDialog(
            onDismiss = { showShippingDialog = false },
            onConfirm = { provider, code ->
                firestore.collection("orders").document(order.id)
                    .update(
                        "status", "Đang giao",
                        "shippingProvider", provider,
                        "trackingNumber", code
                    )
                showShippingDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mã đơn: #${order.id.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold)
                    Text(order.paymentMethod, fontSize = 12.sp, color = Color.Gray)
                }

                Surface(
                    color = if (order.paymentStatus == "PAID") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (order.paymentStatus == "PAID") "ĐÃ THANH TOÁN" else "CHƯA THANH TOÁN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (order.paymentStatus == "PAID") Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Text("👤 ${order.customerName}", fontWeight = FontWeight.SemiBold)
            Text("📞 ${order.phoneNumber}", fontSize = 14.sp)
            Text("📍 ${order.address}", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            order.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• ${item.name} x${item.quantity}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("${item.price}đ", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Trạng thái: ", fontSize = 13.sp)
                Text(
                    text = order.status,
                    color = when(order.status) {
                        "Hoàn thành" -> Color(0xFF4CAF50)
                        "Đã hủy" -> Color.Gray
                        else -> Color(0xFFE91E63)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng cộng:", fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.getDefault(), "%,dđ", order.totalPrice),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFFE91E63)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            val canConfirm = order.paymentMethod == "COD" || order.paymentStatus == "PAID"

            when (order.status) {
                "Chờ xử lý" -> {
                    Button(
                        onClick = {
                            val batch = firestore.batch()
                            val orderRef = firestore.collection("orders").document(order.id)

                            // 1. Cập nhật trạng thái đơn hàng
                            batch.update(orderRef, "status", "Đã xác nhận")

                            // 2. Duyệt qua danh sách items trong đơn để trừ kho
                            order.items.forEach { item ->
                                val productRef = firestore.collection("products").document(item.productId)
                                // Sử dụng FieldValue.increment(-quantity) để trừ số lượng an toàn
                                batch.update(
                                    productRef,
                                    "stock",
                                    com.google.firebase.firestore.FieldValue.increment(-item.quantity.toLong())
                                )
                            }

                            // Thực thi toàn bộ thay đổi
                            batch.commit()
                                .addOnSuccessListener {
                                    // Có thể thêm thông báo Toast thành công ở đây
                                }
                                .addOnFailureListener { e ->
                                    // Xử lý lỗi nếu cần
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canConfirm) Color(0xFFE91E63) else Color.LightGray
                        )
                    ) {
                        Text(if (canConfirm) "XÁC NHẬN ĐƠN HÀNG" else "CHỜ THANH TOÁN...")
                    }
                }





                "Đã xác nhận" -> {
                    Button(
                        onClick = { showShippingDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("GIAO CHO VẬN CHUYỂN")
                    }
                }

                "Đang giao" -> {
                    Button(
                        onClick = {
                            firestore.collection("orders").document(order.id)
                                .update("status", "Hoàn thành")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("XÁC NHẬN HOÀN THÀNH")
                    }
                }
            }
        }
    }
}

@Composable
fun ShippingInfoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val providers = listOf("GHTK", "GHN", "Viettel Post", "Khác")
    var selectedProvider by remember { mutableStateOf(providers[0]) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thông tin vận chuyển", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Chọn đơn vị và nhập mã vận đơn.")
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedProvider)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        providers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    selectedProvider = p
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Mã vận đơn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.isNotBlank()) onConfirm(selectedProvider, code) },
                enabled = code.isNotBlank()
            ) { Text("Bắt đầu giao") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
