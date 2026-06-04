package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weddingapp.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var totalRevenue by remember { mutableLongStateOf(0L) }
    var totalOrders by remember { mutableIntStateOf(0) }
    var completedOrders by remember { mutableIntStateOf(0) }
    var pendingOrders by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("orders").get().addOnSuccessListener { snapshot ->
            val orders = snapshot.toObjects(Order::class.java)
            totalOrders = orders.size
            totalRevenue = orders.filter { it.status == "Hoàn thành" }.sumOf { it.totalPrice }
            completedOrders = orders.count { it.status == "Hoàn thành" }
            pendingOrders = orders.count { it.status == "Chờ xử lý" || it.status == "Đã xác nhận" || it.status == "Đang giao" }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("THỐNG KÊ KINH DOANH", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatCard(
                        title = "Tổng doanh thu",
                        value = String.format(Locale.getDefault(), "%,dđ", totalRevenue),
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF4CAF50)
                    )
                }
                item {
                    StatCard(
                        title = "Tổng số đơn hàng",
                        value = totalOrders.toString(),
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF2196F3)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) {
                            StatCard(
                                title = "Đã hoàn thành",
                                value = completedOrders.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50),
                                small = true
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            StatCard(
                                title = "Đang xử lý",
                                value = pendingOrders.toString(),
                                icon = Icons.Default.Pending,
                                color = Color(0xFFFF9800),
                                small = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    small: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (small) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(if (small) 40.dp else 56.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = color,
                        modifier = Modifier.size(if (small) 20.dp else 28.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = if (small) 12.sp else 14.sp, color = Color.Gray)
                Text(
                    value,
                    fontSize = if (small) 18.sp else 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
