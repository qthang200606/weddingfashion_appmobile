package com.example.weddingapp.ui.screens.customer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weddingapp.data.model.CartItem
import com.example.weddingapp.data.model.Review
import com.example.weddingapp.data.repository.CartRepository
import com.example.weddingapp.data.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots // QUAN TRỌNG: Sửa lỗi unresolved snapshots
import com.google.firebase.firestore.toObjects // QUAN TRỌNG: Sửa lỗi unresolved toObjects
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Lấy dữ liệu sản phẩm
    val productState = ProductRepository.getProductById(productId).collectAsState(initial = null)
    val product = productState.value

    // Lấy danh sách Review - Đã sửa lỗi infer type bằng cách chỉ định rõ kiểu dữ liệu
    val reviewsState = remember(productId) {
        FirebaseFirestore.getInstance()
            .collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                // Chỉ định rõ kiểu Review để tránh lỗi Cannot infer type
                snapshot.toObjects(Review::class.java)
            }
    }.collectAsState(initial = emptyList())

    val reviews = reviewsState.value

    // Tính rating trung bình - Sửa lỗi unresolved 'rating' và 'average'
    val avgRating = if (reviews.isNotEmpty()) {
        reviews.map { it.rating.toDouble() }.average().toFloat()
    } else 0f

    var selectedSize by remember { mutableStateOf("M") }
    var quantity by remember { mutableIntStateOf(1) }
    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (product == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            val isOutOfStock = product.stock <= 0
            val maxStock = product.stock

            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
                Column(
                    modifier = Modifier.verticalScroll(scrollState).padding(bottom = 100.dp)
                ) {
                    // --- 1. Ảnh sản phẩm ---
                    Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isOutOfStock) 0.6f else 1f
                        )

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .statusBarsPadding()
                                .background(Color.White.copy(0.7f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }

                        if (isOutOfStock) {
                            Surface(
                                color = Color.Black.copy(0.7f),
                                modifier = Modifier.align(Alignment.Center),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("HẾT HÀNG", color = Color.White, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- 2. Thông tin chính ---
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(product.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                        // Header Ranking
                        RatingSummarySection(avgRating, reviews.size)

                        Text(
                            text = "${formatPriceDetail(product.price)} VNĐ",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = if (isOutOfStock) Color.Gray else Color(0xFFB8860B),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFF1F1F1))

                        // --- 3. Chọn Size & Số lượng ---
                        Text("Kích cỡ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            listOf("S", "M", "L", "XL").forEach { size ->
                                SizeChip(size, selectedSize == size) { selectedSize = size }
                            }
                        }

                        Text("Số lượng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        QuantitySelector(
                            quantity = quantity,
                            onIncrement = { if (quantity < maxStock) quantity++ },
                            onDecrement = { if (quantity > 1) quantity-- },
                            isEnabled = !isOutOfStock
                        )

                        // --- 4. Mô tả ---
                        Text("Mô tả sản phẩm", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
                        Text(product.description, color = Color.DarkGray, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 8.dp, color = Color(0xFFF8F8F8))

                        // --- 5. Danh sách nhận xét ---
                        Text("Nhận xét từ khách hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (reviews.isEmpty()) {
                            Text("Chưa có đánh giá nào.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            reviews.forEach { review ->
                                RealReviewItem(review)
                            }
                        }
                    }
                }

                // --- 6. Thanh nút bấm phía dưới ---
                Surface(modifier = Modifier.align(Alignment.BottomCenter), shadowElevation = 15.dp, color = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding()) {
                        OutlinedButton(
                            onClick = { onNavigateToChat(productId) },
                            modifier = Modifier.height(54.dp).weight(0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFB8860B))
                        ) {
                            Text("Chat", color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    // SỬA LỖI: Chuyển product.id thành String bằng .toString()
                                    val item = CartItem(
                                        productId = product.id.toString(),
                                        name = product.name,
                                        price = product.price,
                                        imageUrl = product.imageUrl,
                                        quantity = quantity
                                    )
                                    CartRepository.addToCart(item)
                                    snackbarHostState.showSnackbar("Đã thêm vào giỏ hàng")
                                }
                            },
                            modifier = Modifier.height(54.dp).weight(0.7f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isOutOfStock,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            val grad = if (isOutOfStock) Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                            else Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFB8860B)))
                            Box(modifier = Modifier.fillMaxSize().background(grad), contentAlignment = Alignment.Center) {
                                Text(if (isOutOfStock) "HẾT HÀNG" else "THÊM VÀO GIỎ", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSummarySection(avgRating: Float, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { i ->
            Icon(
                Icons.Default.Star, null,
                tint = if (i < avgRating.toInt()) Color(0xFFD4AF37) else Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = String.format(" %.1f/5 (%d đánh giá)", avgRating, count),
            fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun RealReviewItem(review: Review) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFFF1F1F1), CircleShape), contentAlignment = Alignment.Center) {
                Text(review.userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row {
                    repeat(5) { i ->
                        Icon(Icons.Default.Star, null, tint = if (i < review.rating) Color(0xFFD4AF37) else Color.LightGray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Text(review.comment, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp, start = 48.dp))
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = Color(0xFFF1F1F1))
    }
}

@Composable
fun SizeChip(size: String, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.padding(end = 10.dp).clickable { onSelect() },
        color = if (isSelected) Color(0xFFB8860B) else Color(0xFFF8F8F8),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Text(size, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuantitySelector(quantity: Int, onIncrement: () -> Unit, onDecrement: () -> Unit, isEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp)).padding(4.dp)) {
        IconButton(onClick = onDecrement, enabled = isEnabled) { Icon(Icons.Default.Remove, null) }
        Text(quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        IconButton(onClick = onIncrement, enabled = isEnabled) { Icon(Icons.Default.Add, null) }
    }
}

fun formatPriceDetail(price: String): String {
    return try {
        val amount = price.replace(Regex("[^0-9]"), "").toLong()
        String.format(Locale.getDefault(), "%,d", amount).replace(",", ".")
    } catch (e: Exception) { price }
}