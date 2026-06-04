package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.ProductItem
import com.example.weddingapp.data.repository.ProductRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    categoryId: String,
    categoryName: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val products by ProductRepository.getProducts(categoryId).collectAsState(initial = emptyList())

    // Gradient nền nhẹ nhàng, sang trọng
    val mainGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFDFCFB))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // Căn giữa tiêu đề cho sang trọng
                title = {
                    Text(
                        categoryName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(mainGradient)) {
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37), strokeWidth = 2.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        CustomerProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun CustomerProductCard(product: ProductItem, onClick: () -> Unit) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..3 // Cảnh báo khi sắp hết hàng

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat design cho sang trọng
    ) {
        Column {
            Box(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                // Ảnh sản phẩm
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (isOutOfStock) 0.5f else 1f
                )

                // Nhãn HẾT HÀNG (Thiết kế tối giản)
                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HẾT HÀNG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Nhãn CÒN ÍT (Tạo cảm giác khan hiếm để khách mua nhanh)
                if (isLowStock) {
                    Text(
                        text = "CHỈ CÒN ${product.stock}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp, bottom = 8.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = if (isOutOfStock) Color.Gray else Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatPriceDisplayList(product.price)}đ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOutOfStock) Color.LightGray else Color(0xFFB8860B) // Màu Gold tối sang trọng
                        )
                    )

                    // Nút thêm nhanh vào giỏ
                    if (!isOutOfStock) {
                        IconButton(
                            onClick = { /* Xử lý thêm nhanh */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.Black.copy(0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
fun formatPriceDisplayList(price: String): String {
    return try {
        val amount = price.toLong()
        java.text.DecimalFormat("#,###").format(amount).replace(",", ".")
    } catch (e: Exception) {
        price
    }
}
