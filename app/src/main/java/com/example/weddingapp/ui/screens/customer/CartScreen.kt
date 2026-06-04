package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.CartItem
import com.example.weddingapp.data.repository.CartRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    // Lấy dữ liệu giỏ hàng thực tế từ Firebase qua Repository
    val cartItems by CartRepository.getCartItems().collectAsState(initial = emptyList())

    // Tính tổng tiền tự động
    val totalPrice = cartItems.sumOf { (it.price.replace(".", "").toLongOrNull() ?: 0L) * it.quantity }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Giỏ Hàng Của Bạn", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                CartBottomBar(totalPrice = totalPrice, onCheckout = onCheckout)
            }
        }
    ) { innerPadding ->
        if (cartItems.isEmpty()) {
            EmptyCartView()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFFBFBFB)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    CartProductRow(item)
                }
            }
        }
    }
}

@Composable
fun CartProductRow(item: CartItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text("${item.price}đ", color = Color(0xFFE91E63), fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(8.dp))

                // Bộ điều khiển số lượng
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityButton(Icons.Default.Remove) { 
                        CartRepository.updateQuantity(item.id, item.quantity - 1) 
                    }
                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                    QuantityButton(Icons.Default.Add) { 
                        CartRepository.updateQuantity(item.id, item.quantity + 1) 
                    }
                }
            }

            IconButton(onClick = { CartRepository.removeFromCart(item.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun QuantityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CartBottomBar(totalPrice: Long, onCheckout: () -> Unit) {
    Surface(
        shadowElevation = 20.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng cộng:", color = Color.Gray)
                Text(
                    String.format(Locale.getDefault(), "%,dđ", totalPrice),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("Thanh Toán Ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun EmptyCartView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Giỏ hàng của bạn đang trống", color = Color.Gray)
    }
}
