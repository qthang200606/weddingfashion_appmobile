package com.example.weddingapp.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit

) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ADMIN DASHBOARD",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- PHẦN 1: THỐNG KÊ ---
            Text("Tổng quan hệ thống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("chat", "124", Icons.Default.Inventory, Color(0xFF4CAF50), Modifier.weight(1f,),onClick = { onNavigate("admin_chat_list") })
                StatCard("Lịch hẹn", "12", Icons.Default.CalendarMonth, Color(0xFF2196F3), Modifier.weight(1f),onClick = { onNavigate("appointment_management") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- PHẦN 2: CHỨC NĂNG QUẢN TRỊ ---
            Text("Chức năng quản lý", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item { AdminActionCard("Quản lý sản phẩm", Icons.Default.AddBusiness) { onNavigate("product_management") } }
                item { AdminActionCard("Quản lý danh mục", Icons.AutoMirrored.Filled.FormatListBulleted) { onNavigate("promotion_manager") } }
                item { AdminActionCard("Quản lý đơn hàng", Icons.Default.CheckCircle) { onNavigate("order_management") } }
                item { AdminActionCard("Quản lý tồn kho", Icons.Default.Event) { onNavigate("inventory_management") } }
                item { AdminActionCard("Quản lý banner", Icons.Default.CardGiftcard) { onNavigate("banner_management") } }
                item { AdminActionCard("Thống kê", Icons.Default.Assessment) { onNavigate("statistics") } }
                item {
                    AdminActionCard("Tin nhắn khách", Icons.Default.Chat) {
                        onNavigate("admin_chat_list")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier,onClick: () -> Unit = {} ) {
    Card(
        modifier = modifier.clickable {onClick()},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, fontSize = 12.sp, color = Color.Gray)
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminActionCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFFD4AF37)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
